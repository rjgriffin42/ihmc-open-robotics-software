package us.ihmc.commonWalkingControlModules.controlModules.foot;

import java.util.ArrayList;
import java.util.List;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.CurrentConfigurationProvider;
import us.ihmc.utilities.math.trajectories.providers.DoubleProvider;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialMotionVector;
import us.ihmc.yoUtilities.controllers.PDController;
import us.ihmc.yoUtilities.controllers.YoSE3PIDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.humanoidRobot.footstep.Footstep;
import us.ihmc.yoUtilities.math.trajectories.OrientationInterpolationTrajectoryGenerator;
import us.ihmc.yoUtilities.math.trajectories.StraightLinePositionTrajectoryGenerator;
import us.ihmc.yoUtilities.math.trajectories.YoPolynomial;
import us.ihmc.yoUtilities.math.trajectories.providers.YoSE3ConfigurationProvider;

public class HeuristicSwingState extends AbstractFootControlState implements SwingStateInterface
{

   private final OneDoFJoint knee;
   private final GeometricJacobian gimpedJacobian;
   protected final YoSE3PIDGains gains;
   private final FramePose footPose = new FramePose();
   protected final DenseMatrix64F selectionMatrix;

   private final CurrentConfigurationProvider initialConfigurationProvider;

   private final DoubleProvider swingTimeProvider;
   private final PDController kneeController;
   private final YoPolynomial kneeTrajectory;

   private final FramePoint desiredPositionInWorld = new FramePoint();
   private final FrameOrientation desiredOrientationInWorld = new FrameOrientation();

   private final DoubleYoVariable initialKneeAngle;
   private final DoubleYoVariable finalKneeVelocity;

   private StraightLinePositionTrajectoryGenerator positionTrajectoryGenerator;
   private OrientationInterpolationTrajectoryGenerator orientationTrajectoryGenerator;

   private final YoSE3ConfigurationProvider finalConfigurationProvider;

   private final FrameVector ankleVector;
   private final ReferenceFrame hipFrame;
   private final DoubleYoVariable estimatedFinalKneeAngle;
   private final double upperLegLength, lowerLegLength;

   public HeuristicSwingState(DoubleProvider swingTimeProvider, RigidBodySpatialAccelerationControlModule accelerationControlModule,
         MomentumBasedController momentumBasedController, ContactablePlaneBody contactableBody, int jacobianId, DoubleYoVariable nullspaceMultiplier,
         BooleanYoVariable jacobianDeterminantInRange, BooleanYoVariable doSingularityEscape, YoSE3PIDGains gains, RobotSide robotSide,
         YoVariableRegistry registry)
   {
      super(ConstraintType.SWING, accelerationControlModule, momentumBasedController, contactableBody, jacobianId, nullspaceMultiplier,
            jacobianDeterminantInRange, doSingularityEscape, robotSide, registry);
      this.gains = gains;
      this.swingTimeProvider = swingTimeProvider;
      List<InverseDynamicsJoint> joints = new ArrayList<>();
      for (InverseDynamicsJoint joint : jacobian.getJointsInOrder())
      {
         joints.add(joint);
      }

      ReferenceFrame footFrame = contactableBody.getFrameAfterParentJoint();
      initialConfigurationProvider = new CurrentConfigurationProvider(footFrame);

      knee = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.KNEE);
      joints.remove(knee);
      gimpedJacobian = new GeometricJacobian(joints.toArray(new InverseDynamicsJoint[joints.size()]), jacobian.getJacobianFrame());

      selectionMatrix = new DenseMatrix64F(SpatialMotionVector.SIZE, SpatialMotionVector.SIZE);
      CommonOps.setIdentity(selectionMatrix);
      selectionMatrix.set(5, 5, 0);

      kneeController = new PDController(robotSide.getCamelCaseNameForMiddleOfExpression() + "KneeController", registry);
      kneeTrajectory = new YoPolynomial(robotSide.getCamelCaseNameForStartOfExpression() + "KneeTrajectory", 7, registry);

      kneeController.setProportionalGain(10);

      initialKneeAngle = new DoubleYoVariable("initialKneeAngle", registry);
      finalKneeVelocity = new DoubleYoVariable("finalKneeVelocity", registry);
      finalKneeVelocity.set(-0.5);

      finalConfigurationProvider = new YoSE3ConfigurationProvider(robotSide.getCamelCaseNameForStartOfExpression() + "HeuristicSwingFinalPose", worldFrame,
            registry);

      positionTrajectoryGenerator = new StraightLinePositionTrajectoryGenerator(robotSide.getCamelCaseNameForStartOfExpression() + "FootPosition", worldFrame,
            swingTimeProvider, initialConfigurationProvider, finalConfigurationProvider, registry);

      orientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator(robotSide.getCamelCaseNameForStartOfExpression() + "Orientation",
            worldFrame, swingTimeProvider, initialConfigurationProvider, finalConfigurationProvider, registry);

      estimatedFinalKneeAngle = new DoubleYoVariable(robotSide.getCamelCaseNameForStartOfExpression() + "EstimatedFinalKneeAngle", registry);

      hipFrame = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.HIP_PITCH).getFrameAfterJoint();
      ReferenceFrame beforeAnkleFrame = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.ANKLE_PITCH).getFrameBeforeJoint();

      FramePoint hip = new FramePoint(hipFrame);
      hip.changeFrame(knee.getFrameBeforeJoint());
      upperLegLength = hip.getVectorCopy().length();

      FramePoint ankle = new FramePoint(beforeAnkleFrame);
      ankle.changeFrame(knee.getFrameAfterJoint());
      lowerLegLength = ankle.getVectorCopy().length();

      FramePoint footPoint = new FramePoint(footFrame);
      FramePoint anklePoint = new FramePoint(beforeAnkleFrame);
      footPoint.changeFrame(worldFrame);
      anklePoint.changeFrame(worldFrame);
      ankleVector = new FrameVector(worldFrame);
      ankleVector.sub(anklePoint, footPoint);
   }

   @Override
   public void setFootstep(Footstep footstep, TrajectoryParameters trajectoryParameters, boolean useLowHeightTrajectory)
   {
      footstep.getPose(footPose);
      footPose.changeFrame(worldFrame);

      desiredPositionInWorld.changeFrame(worldFrame);
      desiredOrientationInWorld.changeFrame(worldFrame);

      initialKneeAngle.set(knee.getQ());

      finalConfigurationProvider.setPose(footPose);

      positionTrajectoryGenerator.initialize();
      orientationTrajectoryGenerator.initialize();

   }

   @Override
   public void replanTrajectory(Footstep footstep, double swingTimeRemaining, boolean useLowHeightTrajectory)
   {
      // TODO Auto-generated method stub

   }

   private final FramePoint desiredAnklePosition = new FramePoint();
   private final FramePoint hipPosition = new FramePoint();
   private final FrameVector legVector = new FrameVector(worldFrame);

   @Override
   public void doSpecificAction()
   {
      accelerationControlModule.setGains(gains);
      gimpedJacobian.compute();

      if (getTimeInCurrentState() < swingTimeProvider.getValue())
      {
         footPose.getPositionIncludingFrame(desiredAnklePosition);
         desiredAnklePosition.add(ankleVector);
         desiredAnklePosition.changeFrame(worldFrame);

         hipPosition.setIncludingFrame(hipFrame, 0.0, 0.0, 0.0);
         hipPosition.changeFrame(worldFrame);

         legVector.sub(hipPosition, desiredAnklePosition);
         double legLength = legVector.length();

         if (legLength > (lowerLegLength + upperLegLength - 1e-6))
         {
            legLength = (lowerLegLength + upperLegLength - 1e-6);
         }
         estimatedFinalKneeAngle
               .set(Math.PI
                     - Math.acos((upperLegLength * upperLegLength + lowerLegLength * lowerLegLength - legLength * legLength)
                           / (2 * upperLegLength * lowerLegLength)));
      }
      
      estimatedFinalKneeAngle.set(initialKneeAngle.getDoubleValue());

      kneeTrajectory.setSexticUsingWaypoint(0, swingTimeProvider.getValue() / 2.0, swingTimeProvider.getValue(), initialKneeAngle.getDoubleValue(), 0, 0,
            initialKneeAngle.getDoubleValue() + 0.5, estimatedFinalKneeAngle.getDoubleValue(), finalKneeVelocity.getDoubleValue(), 0.0);

      positionTrajectoryGenerator.compute(getTimeInCurrentState());
      orientationTrajectoryGenerator.compute(getTimeInCurrentState());

      positionTrajectoryGenerator.packLinearData(desiredPosition, desiredLinearVelocity, desiredLinearAcceleration);
      orientationTrajectoryGenerator.packAngularData(trajectoryOrientation, desiredAngularVelocity, desiredAngularAcceleration);
      desiredOrientation.setIncludingFrame(trajectoryOrientation);

      accelerationControlModule.doPositionControl(desiredPosition, desiredOrientation, desiredLinearVelocity, desiredAngularVelocity,
            desiredLinearAcceleration, desiredAngularAcceleration, rootBody);
      accelerationControlModule.packAcceleration(footAcceleration);

      ReferenceFrame bodyFixedFrame = contactableBody.getRigidBody().getBodyFixedFrame();
      footAcceleration.changeBodyFrameNoRelativeAcceleration(bodyFixedFrame);
      footAcceleration.changeFrameNoRelativeMotion(bodyFixedFrame);

      taskspaceConstraintData.set(footAcceleration, nullspaceMultipliers, selectionMatrix);
      momentumBasedController.setDesiredSpatialAcceleration(gimpedJacobian, taskspaceConstraintData);
      double qDesired;
      double qdDesired;
      double qddDesired;
      if (getTimeInCurrentState() <= swingTimeProvider.getValue())
      {
         kneeTrajectory.compute(getTimeInCurrentState());
         qDesired = kneeTrajectory.getPosition();
         qdDesired = kneeTrajectory.getVelocity();
         qddDesired = kneeTrajectory.getAcceleration();
      }
      else
      {
         double dt = getTimeInCurrentState() - swingTimeProvider.getValue();
         qDesired = estimatedFinalKneeAngle.getDoubleValue() + dt * finalKneeVelocity.getDoubleValue();
         qdDesired = finalKneeVelocity.getDoubleValue();
         qddDesired = 0.0;
      }

      double qdd = kneeController.computeForAngles(knee.getQ(), qDesired, knee.getQd(), qdDesired) + qddDesired;

      momentumBasedController.setOneDoFJointAcceleration(knee, qdd);
   }

}
