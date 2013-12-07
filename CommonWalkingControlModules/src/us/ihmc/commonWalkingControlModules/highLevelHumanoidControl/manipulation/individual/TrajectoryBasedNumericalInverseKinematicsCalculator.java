package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.trajectories.OrientationTrajectoryGenerator;
import us.ihmc.utilities.math.DampedLeastSquaresSolver;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RevoluteJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePose;
import com.yobotics.simulationconstructionset.util.trajectory.PositionTrajectoryGenerator;

public class TrajectoryBasedNumericalInverseKinematicsCalculator
{

   // Persistent variables
   private final double dt;
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final RigidBody base;
   private final GeometricJacobian ikJacobian;
   private final DampedLeastSquaresSolver solver;
   private final TwistCalculator twistCalculator;
   private final FramePoint endEffectorPositionInFrameToControlPoseOf;
   private final FrameOrientation endEffectorOrientationInFrameToControlPoseOf;
   

   // TODO: YoVariableize desired variables
   private final FramePoint desiredPosition = new FramePoint(worldFrame);
   private final FrameVector desiredVelocity = new FrameVector(worldFrame);
   private final FrameVector desiredAcceleration = new FrameVector(worldFrame);
   
   private final FrameOrientation desiredOrientation = new FrameOrientation(worldFrame);
   private final FrameVector desiredAngularVelocity = new FrameVector(worldFrame);
   private final FrameVector desiredAngularAcceleration = new FrameVector(worldFrame);
   
   private ReferenceFrame frameToControlPoseOf;
   private PositionTrajectoryGenerator positionTrajectoryGenerator;
   private OrientationTrajectoryGenerator orientationTrajectoryGenerator;
   
   
   private final ReferenceFrame baseFrame;
   private final ReferenceFrame baseFrameForIK;
   private final ReferenceFrame endEffectorFrame;
   private final ReferenceFrame frameToControlPoseOfForIk;
   
   private final RevoluteJoint[] revoluteJoints;
   private final RevoluteJoint[] revoluteJointsCopyForIK;
   
   private final DoubleYoVariable ikPositionErrorGain = new DoubleYoVariable("ikPositionErrorGain", registry);
   private final DoubleYoVariable ikVelocityErrorGain = new DoubleYoVariable("ikVelocityErrorGain", registry);
   private final DoubleYoVariable ikAlpha = new DoubleYoVariable("ikAlpha", registry);
   
   private ReferenceFrame trajectoryFrame;
   
   // External variables
   private final DenseMatrix64F desiredAngles;
   private final DenseMatrix64F desiredAngularVelocities;
   
   
   // Temporary variables
   private final DenseMatrix64F inverseKinematicsStep;
   private final DenseMatrix64F twistMatrix = new DenseMatrix64F(6, 1);

   private final FramePoint currentPosition = new FramePoint();
   private final FrameOrientation currentOrientation = new FrameOrientation();

   private final Vector3d linearError = new Vector3d();
   private final Vector3d angularError = new Vector3d();

   private final Point3d currentPoint = new Point3d();
   private final Matrix3d currentOrientationMatrix = new Matrix3d();
   private final Matrix3d desiredOrientationMatrix = new Matrix3d();

   private final Twist baseTwist = new Twist();
   private final Twist desiredTwist;
   

   private final Vector3d currentOrientationColumn = new Vector3d();
   private final Vector3d desiredOrientationColumn = new Vector3d();
   private final Vector3d columnCrossProduct = new Vector3d();
   private final Point3d desiredPositionPoint = new Point3d();

   private final Vector3d desiredVelocityVector = new Vector3d();
   private final Vector3d desiredAngularVelocityVector = new Vector3d();
   
   
   // Visualization
   
   private final FramePose desiredTrajectoryPose = new FramePose();
   private final YoFramePose yoDesiredTrajectoryPose = new YoFramePose("desiredTrajectoryPose", "", worldFrame, registry);

   public TrajectoryBasedNumericalInverseKinematicsCalculator(RigidBody base, RigidBody endEffector, double controlDT, TwistCalculator twistCalculator, YoVariableRegistry parentRegistry, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.base = base;
      this.dt = controlDT;
      this.twistCalculator = twistCalculator;
      InverseDynamicsJoint[] inverseDynamicsJoints = ScrewTools.createJointPath(base, endEffector);
      revoluteJoints = new RevoluteJoint[inverseDynamicsJoints.length];
      revoluteJointsCopyForIK = new RevoluteJoint[inverseDynamicsJoints.length];

      createJointPathCopy(inverseDynamicsJoints);

      baseFrame = base.getBodyFixedFrame();
      endEffectorFrame = endEffector.getBodyFixedFrame();
      baseFrameForIK = baseFrame; // TODO: Check if correct
      RigidBody endEffectorForIK = revoluteJointsCopyForIK[revoluteJointsCopyForIK.length - 1].getSuccessor();
      ReferenceFrame endEffectorFrameForIK = endEffectorForIK.getBodyFixedFrame();
      frameToControlPoseOfForIk = new ReferenceFrame(base.getName() + "ControlFrame", endEffectorFrameForIK)
      {
         private static final long serialVersionUID = -2964609854840695124L;

         @Override
         public void updateTransformToParent(Transform3D transformToParent)
         {
            frameToControlPoseOf.getTransformToDesiredFrame(transformToParent, endEffectorFrame);
         }
      };

      ikJacobian = new GeometricJacobian(revoluteJointsCopyForIK[0].getPredecessor(), endEffectorForIK, baseFrameForIK);
      solver = new DampedLeastSquaresSolver(ikJacobian.getNumberOfColumns());

      desiredAngles = new DenseMatrix64F(ikJacobian.getNumberOfColumns(), 1);
      desiredAngularVelocities = new DenseMatrix64F(ikJacobian.getNumberOfColumns(), 1);
      inverseKinematicsStep = new DenseMatrix64F(ikJacobian.getNumberOfColumns(), 1);

      endEffectorPositionInFrameToControlPoseOf = new FramePoint(frameToControlPoseOfForIk);
      endEffectorOrientationInFrameToControlPoseOf = new FrameOrientation(frameToControlPoseOfForIk);

      desiredTwist = new Twist(endEffectorFrameForIK, baseFrame, baseFrame);

      ikPositionErrorGain.set(100.0);
      ikVelocityErrorGain.set(250.0);
      ikAlpha.set(0.05);

      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("", yoDesiredTrajectoryPose.createDynamicGraphicCoordinateSystem(endEffector.getName() + "DesiredTrajectoryPose", 0.2));
      
      parentRegistry.addChild(registry);
   }

   public void initialize()
   {
      for (int i = 0; i < revoluteJointsCopyForIK.length; i++)
      {
         RevoluteJoint measured = revoluteJoints[i];
         RevoluteJoint copy = revoluteJointsCopyForIK[i];

         copy.setJointPositionVelocityAndAcceleration(measured);
         copy.updateMotionSubspace();

         desiredAngles.set(i, 0, measured.getQ());
      }

      updateFrames();
      updateTrajectories(0.0);
   }

   private void updateFrames()
   {
      revoluteJointsCopyForIK[0].getPredecessor().updateFramesRecursively();
      ikJacobian.compute();
   }


   public void compute(double time)
   {
      updateError();

      updateTrajectories(time);

      desiredVelocity.getVector(desiredVelocityVector);
      desiredAngularVelocity.getVector(desiredAngularVelocityVector);

      linearError.scale(ikPositionErrorGain.getDoubleValue());
      angularError.scale(ikVelocityErrorGain.getDoubleValue());
      desiredVelocityVector.add(linearError);
      desiredAngularVelocityVector.add(angularError);

      desiredTwist.set(frameToControlPoseOf, trajectoryFrame, baseFrame, desiredVelocityVector, desiredAngularVelocityVector);

      transformTwistToEndEffectorFrame();

      // Calculate desired joint velocities
      initializeSolver();
      desiredTwist.packMatrix(twistMatrix, 0);
      solver.solve(twistMatrix, inverseKinematicsStep);

      CommonOps.addEquals(desiredAngles, dt, inverseKinematicsStep);

      for (int i = 0; i < revoluteJointsCopyForIK.length; i++)
      {
         revoluteJointsCopyForIK[i].setQ(desiredAngles.get(i, 0));
      }
      updateFrames();
      
      updateDesiredVelocities();
   }

   private void transformTwistToEndEffectorFrame()
   {   
      //TODO: Check correctness
//      desiredTwist.changeFrame(frameToControlPoseOf);
      desiredTwist.changeBodyFrameNoRelativeTwist(endEffectorFrame);
//      desiredTwist.changeFrame(baseFrame);
      
      if(trajectoryFrame == worldFrame)
      {
         twistCalculator.packTwistOfBody(baseTwist, base);
         desiredTwist.sub(baseTwist);
      }
      else
      {
         desiredTwist.changeBaseFrameNoRelativeTwist(baseFrame);
      }
      
   }

   private void initializeSolver()
   {
      solver.setAlpha(ikAlpha.getDoubleValue());
      solver.setA(ikJacobian.getJacobianMatrix()); 
   }

   private void updateDesiredVelocities()
   {
      desiredTwist.set(frameToControlPoseOf, trajectoryFrame, baseFrame, desiredVelocity.getVector(), desiredAngularVelocity.getVector());
      transformTwistToEndEffectorFrame();
      initializeSolver();
      desiredTwist.packMatrix(twistMatrix, 0);
      solver.solve(twistMatrix, desiredAngularVelocities);
   }

   private void updateTrajectories(double time)
   {
      // Compute and pack desired hand positions and velocities
      positionTrajectoryGenerator.compute(time);
      orientationTrajectoryGenerator.compute(time);

      positionTrajectoryGenerator.packLinearData(desiredPosition, desiredVelocity, desiredAcceleration);
      orientationTrajectoryGenerator.packAngularData(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);

      desiredTrajectoryPose.set(desiredPosition.getReferenceFrame());
      desiredTrajectoryPose.setPosition(desiredPosition);
      desiredTrajectoryPose.setOrientation(desiredOrientation);
      desiredTrajectoryPose.changeFrame(worldFrame);
      yoDesiredTrajectoryPose.set(desiredTrajectoryPose);
      
      trajectoryFrame = desiredPosition.getReferenceFrame();
      
      desiredPosition.changeFrame(baseFrame);
      desiredVelocity.changeFrame(baseFrame);
      desiredOrientation.changeFrame(baseFrame);
      desiredAngularVelocity.changeFrame(baseFrame);
      
      
   }


   private void updateError()
   {

      frameToControlPoseOfForIk.update();

      currentPosition.setAndChangeFrame(endEffectorPositionInFrameToControlPoseOf);
      currentPosition.changeFrame(baseFrameForIK);

      currentPosition.getPoint(currentPoint);
      desiredPosition.getPoint(desiredPositionPoint);

      linearError.sub(desiredPositionPoint, currentPoint);

      currentOrientation.setAndChangeFrame(endEffectorOrientationInFrameToControlPoseOf);
      currentOrientation.changeFrame(baseFrameForIK);

      currentOrientation.getMatrix3d(currentOrientationMatrix);
      desiredOrientation.getMatrix3d(desiredOrientationMatrix);

      angularError.set(0.0, 0.0, 0.0);
      for (int i = 0; i < 3; i++)
      {
         currentOrientationMatrix.getColumn(i, currentOrientationColumn);
         desiredOrientationMatrix.getColumn(i, desiredOrientationColumn);
         columnCrossProduct.cross(currentOrientationColumn, desiredOrientationColumn);

         angularError.add(columnCrossProduct);
      }
      angularError.scale(0.5);

   }

   public RevoluteJoint[] getRevoluteJointsInOrder()
   {
      return revoluteJoints;
   }

   public void setTrajectory(PositionTrajectoryGenerator positionTrajectoryGenerator, OrientationTrajectoryGenerator orientationTrajectoryGenerator,
         ReferenceFrame frameToControlPoseOf)
   {
      this.positionTrajectoryGenerator = positionTrajectoryGenerator;
      this.orientationTrajectoryGenerator = orientationTrajectoryGenerator;
      this.frameToControlPoseOf = frameToControlPoseOf;
   }

   private void createJointPathCopy(InverseDynamicsJoint[] inverseDynamicsJoints)
   {
      for (int i = 0; i < inverseDynamicsJoints.length; i++)
      {

         FramePoint comOffset = new FramePoint();
         if (inverseDynamicsJoints[i] instanceof OneDoFJoint)
         {
            RevoluteJoint revoluteJoint = (RevoluteJoint) inverseDynamicsJoints[i];
            revoluteJoints[i] = revoluteJoint;

            RigidBody predecessorOriginal = revoluteJoint.getPredecessor();
            RigidBody successorOriginal = revoluteJoint.getSuccessor();

            RigidBody predecessorCopy;
            if (i > 0)
            {
               predecessorOriginal.packCoMOffset(comOffset);
               predecessorCopy = ScrewTools.addRigidBody(predecessorOriginal.getName() + "Copy", revoluteJointsCopyForIK[i - 1], predecessorOriginal
                     .getInertia().getMassMomentOfInertiaPartCopy(), predecessorOriginal.getInertia().getMass(), comOffset.getVectorCopy());
            }
            else
            {
               predecessorCopy = new RigidBody(predecessorOriginal.getName() + "Copy", predecessorOriginal.getParentJoint().getFrameAfterJoint());
            }

            RevoluteJoint jointCopy = ScrewTools.addRevoluteJoint(revoluteJoint.getName() + "Copy", predecessorCopy, revoluteJoint.getOffsetTransform3D(),
                  revoluteJoint.getJointAxis().getVectorCopy());
            revoluteJointsCopyForIK[i] = jointCopy;

            successorOriginal.packCoMOffset(comOffset);
            RigidBody successorCopy = ScrewTools.addRigidBody(successorOriginal.getName() + "Copy", jointCopy, successorOriginal.getInertia()
                  .getMassMomentOfInertiaPartCopy(), successorOriginal.getInertia().getMass(), comOffset.getVectorCopy());
            jointCopy.setSuccessor(successorCopy);
         }
         else
         {
            throw new RuntimeException("Non-revolute joints not supported for trajectory based numerical IK");
         }
      }
   }

   public DenseMatrix64F getDesiredJointAngles()
   {
      return desiredAngles;
   }

   public DenseMatrix64F getDesiredJointVelocities()
   {
      return desiredAngularVelocities;
   }
}
