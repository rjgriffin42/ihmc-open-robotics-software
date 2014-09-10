package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.utilities.math.DampedLeastSquaresSolver;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.RevoluteJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.graphics.YoGraphicCoordinateSystem;
import us.ihmc.yoUtilities.math.frames.YoFramePose;

import com.yobotics.simulationconstructionset.util.trajectory.OrientationTrajectoryGenerator;
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
   private final DoubleYoVariable ikOrientationErrorGain = new DoubleYoVariable("ikOrientationErrorGain", registry);
   private final DoubleYoVariable ikAlpha = new DoubleYoVariable("ikAlpha", registry);
   private final DoubleYoVariable maximumIKError = new DoubleYoVariable("maximumIKError", registry);
   private final DoubleYoVariable maximumAngleOutsideLimits = new DoubleYoVariable("maximumAngleOutsideLimits", registry);

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
   private final YoFramePose yoDesiredTrajectoryPose = new YoFramePose("desiredTrajectoryPose", "", worldFrame, registry);

   public TrajectoryBasedNumericalInverseKinematicsCalculator(RigidBody base, RigidBody endEffector, double controlDT, TwistCalculator twistCalculator,
         YoVariableRegistry parentRegistry, YoGraphicsListRegistry dynamicGraphicObjectsListRegistry)
   {
      this.base = base;
      this.dt = controlDT;
      this.twistCalculator = twistCalculator;
      InverseDynamicsJoint[] inverseDynamicsJoints = ScrewTools.createJointPath(base, endEffector);
      revoluteJoints = ScrewTools.filterJoints(inverseDynamicsJoints, RevoluteJoint.class);
      revoluteJointsCopyForIK = ScrewTools.filterJoints(ScrewTools.cloneJointPath(inverseDynamicsJoints), RevoluteJoint.class);

      baseFrame = base.getBodyFixedFrame();
      endEffectorFrame = endEffector.getBodyFixedFrame();
      baseFrameForIK = baseFrame; // TODO: Check if correct
      RigidBody endEffectorForIK = revoluteJointsCopyForIK[revoluteJointsCopyForIK.length - 1].getSuccessor();
      ReferenceFrame endEffectorFrameForIK = endEffectorForIK.getBodyFixedFrame();
      frameToControlPoseOfForIk = new ReferenceFrame(base.getName() + "ControlFrame", endEffectorFrameForIK)
      {
         private static final long serialVersionUID = -2964609854840695124L;

         @Override
         protected void updateTransformToParent(Transform3D transformToParent)
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
      ikOrientationErrorGain.set(100.0);
      ikAlpha.set(0.05);
      maximumIKError.set(0.06);
      maximumAngleOutsideLimits.set(0.1);

      if (dynamicGraphicObjectsListRegistry != null)
      {
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("", new YoGraphicCoordinateSystem(endEffector.getName() + "DesiredTrajectoryPose", yoDesiredTrajectoryPose, 0.2));
      }

      if (parentRegistry != null)
      {
         parentRegistry.addChild(registry);
      }
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

      revoluteJointsCopyForIK[0].getPredecessor().updateFramesRecursively();
      ikJacobian.compute();
      updateTrajectories(0.0);
   }

   public boolean compute(double time)
   {
      boolean inverseKinematicsIsValid = updateError();

      updateTrajectories(time);

      desiredVelocity.get(desiredVelocityVector);
      desiredAngularVelocity.get(desiredAngularVelocityVector);

      linearError.scale(ikPositionErrorGain.getDoubleValue());
      angularError.scale(ikOrientationErrorGain.getDoubleValue());
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
         double q = desiredAngles.get(i, 0);
         if (q < (revoluteJoints[i].getJointLimitLower() - maximumAngleOutsideLimits.getDoubleValue())
               || q > (revoluteJoints[i].getJointLimitUpper() + maximumAngleOutsideLimits.getDoubleValue()))
         {
            inverseKinematicsIsValid = false;
         }

         revoluteJointsCopyForIK[i].setQ(q);
      }

      // Update frames
      revoluteJointsCopyForIK[0].getPredecessor().updateFramesRecursively();
      ikJacobian.compute();

      if (inverseKinematicsIsValid)
      {
         updateDesiredVelocities();
      }
      else
      {
         desiredAngularVelocities.zero();
      }

      return inverseKinematicsIsValid;
   }

   private void transformTwistToEndEffectorFrame()
   {
      //TODO: Check correctness
      //      desiredTwist.changeFrame(frameToControlPoseOf);
      desiredTwist.changeBodyFrameNoRelativeTwist(endEffectorFrame);
      //      desiredTwist.changeFrame(baseFrame);

      if (trajectoryFrame == worldFrame && twistCalculator != null)
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

      yoDesiredTrajectoryPose.setAndMatchFrame(desiredPosition, desiredOrientation);

      trajectoryFrame = desiredPosition.getReferenceFrame();

      desiredPosition.changeFrame(baseFrame);
      desiredVelocity.changeFrame(baseFrame);
      desiredOrientation.changeFrame(baseFrame);
      desiredAngularVelocity.changeFrame(baseFrame);

   }

   private boolean updateError()
   {
      frameToControlPoseOfForIk.update();

      currentPosition.setIncludingFrame(endEffectorPositionInFrameToControlPoseOf);
      currentPosition.changeFrame(baseFrameForIK);

      currentPosition.get(currentPoint);
      desiredPosition.get(desiredPositionPoint);

      linearError.sub(desiredPositionPoint, currentPoint);

      currentOrientation.setIncludingFrame(endEffectorOrientationInFrameToControlPoseOf);
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

      return linearError.length() <= maximumIKError.getDoubleValue() && angularError.length() <= maximumIKError.getDoubleValue();
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

   public DenseMatrix64F getDesiredJointAngles()
   {
      return desiredAngles;
   }

   public DenseMatrix64F getDesiredJointVelocities()
   {
      return desiredAngularVelocities;
   }
}
