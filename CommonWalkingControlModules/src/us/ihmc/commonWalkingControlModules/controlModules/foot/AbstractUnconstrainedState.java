package us.ihmc.commonWalkingControlModules.controlModules.foot;

import static us.ihmc.robotics.weightMatrices.SolverWeightLevels.FOOT_SWING_WEIGHT;

import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.tools.FormattingTools;

/**
 * The unconstrained state is used if the foot is moved free in space without constrains. Depending on the type of trajectory
 * this can either be a movement along a straight line or (in case of walking) a swing motion.
 *
 * E.g. the MoveStraightState and the SwingState extend this class and implement the trajectory related methods.
 */
public abstract class AbstractUnconstrainedState extends AbstractFootControlState
{
   private static final boolean USE_ALL_LEG_JOINT_SWING_CORRECTOR = false;

   private final SpatialFeedbackControlCommand spatialFeedbackControlCommand = new SpatialFeedbackControlCommand();

   protected final LegSingularityAndKneeCollapseAvoidanceControlModule legSingularityAndKneeCollapseAvoidanceControlModule;
   private final LegJointLimitAvoidanceControlModule legJointLimitAvoidanceControlModule;

   private final YoFramePoint yoDesiredPosition;
   private final YoFrameVector yoDesiredLinearVelocity;
   private final YoBoolean yoSetDesiredAccelerationToZero;
   private final YoBoolean yoSetDesiredVelocityToZero;

   protected final YoBoolean scaleSecondaryJointWeights;
   protected final YoDouble secondaryJointWeightScale;

   private final YoFrameVector angularWeight;
   protected final YoFrameVector linearWeight;

   private final ReferenceFrame ankleFrame;
   private final PoseReferenceFrame controlFrame;

   private final YoSE3PIDGainsInterface gains;

   public AbstractUnconstrainedState(ConstraintType constraintType, FootControlHelper footControlHelper, YoSE3PIDGainsInterface gains,
         YoVariableRegistry registry)
   {
      super(constraintType, footControlHelper);
      this.gains = gains;

      this.legSingularityAndKneeCollapseAvoidanceControlModule = footControlHelper.getLegSingularityAndKneeCollapseAvoidanceControlModule();

      RigidBody foot = contactableFoot.getRigidBody();
      String namePrefix = foot.getName() + FormattingTools.underscoredToCamelCase(constraintType.toString().toLowerCase(), true);
      yoDesiredLinearVelocity = new YoFrameVector(namePrefix + "DesiredLinearVelocity", worldFrame, registry);
      yoDesiredLinearVelocity.setToNaN();
      yoDesiredPosition = new YoFramePoint(namePrefix + "DesiredPosition", worldFrame, registry);
      yoDesiredPosition.setToNaN();
      yoSetDesiredAccelerationToZero = new YoBoolean(namePrefix + "SetDesiredAccelerationToZero", registry);
      yoSetDesiredVelocityToZero = new YoBoolean(namePrefix + "SetDesiredVelocityToZero", registry);

      scaleSecondaryJointWeights = new YoBoolean(namePrefix + "ScaleSecondaryJointWeights", registry);
      secondaryJointWeightScale = new YoDouble(namePrefix + "SecondaryJointWeightScale", registry);
      secondaryJointWeightScale.set(1.0);

      angularWeight = new YoFrameVector(namePrefix + "AngularWeight", worldFrame, registry);
      linearWeight = new YoFrameVector(namePrefix + "LinearWeight", worldFrame, registry);

      angularWeight.set(FOOT_SWING_WEIGHT, FOOT_SWING_WEIGHT, FOOT_SWING_WEIGHT);
      linearWeight.set(FOOT_SWING_WEIGHT, FOOT_SWING_WEIGHT, FOOT_SWING_WEIGHT);

      if (USE_ALL_LEG_JOINT_SWING_CORRECTOR)
         legJointLimitAvoidanceControlModule = new LegJointLimitAvoidanceControlModule(namePrefix, registry, controllerToolbox, robotSide);
      else
         legJointLimitAvoidanceControlModule = null;

      ankleFrame = contactableFoot.getFrameAfterParentJoint();
      controlFrame = new PoseReferenceFrame("controlFrame", contactableFoot.getRigidBody().getBodyFixedFrame());

      spatialFeedbackControlCommand.set(rootBody, foot);
      spatialFeedbackControlCommand.setPrimaryBase(pelvis);
      spatialFeedbackControlCommand.setGains(gains);
      ReferenceFrame linearGainsFrame = footControlHelper.getHighLevelHumanoidControllerToolbox().getPelvisZUpFrame();
      spatialFeedbackControlCommand.setGainsFrames(null, linearGainsFrame);
      FramePose anklePoseInFoot = new FramePose(ankleFrame);
      anklePoseInFoot.changeFrame(contactableFoot.getRigidBody().getBodyFixedFrame());
      changeControlFrame(anklePoseInFoot);
   }

   protected void changeControlFrame(FramePose controlFramePoseInEndEffector)
   {
      controlFramePoseInEndEffector.checkReferenceFrameMatch(contactableFoot.getRigidBody().getBodyFixedFrame());
      spatialFeedbackControlCommand.setControlFrameFixedInEndEffector(controlFramePoseInEndEffector);
      controlFrame.setPoseAndUpdate(controlFramePoseInEndEffector);
   }

   public void setWeight(double weight)
   {
      angularWeight.set(1.0, 1.0, 1.0);
      angularWeight.scale(weight);
      linearWeight.set(1.0, 1.0, 1.0);
      linearWeight.scale(weight);
   }

   public void setWeights(Vector3D angularWeight, Vector3D linearWeight)
   {
      this.angularWeight.set(angularWeight);
      this.linearWeight.set(linearWeight);
   }

   /**
    * initializes all the trajectories
    */
   protected abstract void initializeTrajectory();

   /**
    * compute the and pack the following variables:
    * desiredPosition, desiredLinearVelocity, desiredLinearAcceleration,
    * trajectoryOrientation, desiredAngularVelocity, desiredAngularAcceleration
    */
   protected abstract void computeAndPackTrajectory();

   @Override
   public void doTransitionIntoAction()
   {
      super.doTransitionIntoAction();
      legSingularityAndKneeCollapseAvoidanceControlModule.setCheckVelocityForSwingSingularityAvoidance(true);
      spatialFeedbackControlCommand.resetSecondaryTaskJointWeightScale();

      initializeTrajectory();
   }

   private final Vector3D tempAngularWeightVector = new Vector3D();
   private final Vector3D tempLinearWeightVector = new Vector3D();
   private final FramePoint desiredAnklePosition = new FramePoint();
   private final FramePose desiredPose = new FramePose();

   @Override
   public void doSpecificAction()
   {
      computeAndPackTrajectory();

      if (USE_ALL_LEG_JOINT_SWING_CORRECTOR)
      {
         legJointLimitAvoidanceControlModule.correctSwingFootTrajectory(desiredPosition, desiredOrientation, desiredLinearVelocity, desiredAngularVelocity,
               desiredLinearAcceleration, desiredAngularAcceleration);
      }

      desiredPose.setPoseIncludingFrame(desiredPosition, desiredOrientation);
      changeDesiredPoseBodyFrame(controlFrame, ankleFrame, desiredPose);
      desiredPose.getPositionIncludingFrame(desiredAnklePosition);

      legSingularityAndKneeCollapseAvoidanceControlModule.correctSwingFootTrajectory(desiredAnklePosition, desiredLinearVelocity, desiredLinearAcceleration);

      desiredPose.setPosition(desiredAnklePosition);
      changeDesiredPoseBodyFrame(ankleFrame, controlFrame, desiredPose);
      desiredPose.getPositionIncludingFrame(desiredPosition);

      if (yoSetDesiredVelocityToZero.getBooleanValue())
      {
         desiredLinearVelocity.setToZero();
      }

      if (yoSetDesiredAccelerationToZero.getBooleanValue())
      {
         desiredLinearAcceleration.setToZero();
      }

      spatialFeedbackControlCommand.set(desiredPosition, desiredLinearVelocity, desiredLinearAcceleration);
      spatialFeedbackControlCommand.set(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);
      angularWeight.get(tempAngularWeightVector);
      linearWeight.get(tempLinearWeightVector);
      spatialFeedbackControlCommand.setWeightsForSolver(tempAngularWeightVector, tempLinearWeightVector);
      spatialFeedbackControlCommand.setScaleSecondaryTaskJointWeight(scaleSecondaryJointWeights.getBooleanValue(), secondaryJointWeightScale.getDoubleValue());
      spatialFeedbackControlCommand.setGains(gains);

      yoDesiredPosition.setAndMatchFrame(desiredPosition);
      yoDesiredLinearVelocity.setAndMatchFrame(desiredLinearVelocity);
   }

   private final RigidBodyTransform oldBodyFrameDesiredTransform = new RigidBodyTransform();
   private final RigidBodyTransform newBodyFrameDesiredTransform = new RigidBodyTransform();
   private final RigidBodyTransform transformFromNewBodyFrameToOldBodyFrame = new RigidBodyTransform();

   private void changeDesiredPoseBodyFrame(ReferenceFrame oldBodyFrame, ReferenceFrame newBodyFrame, FramePose framePoseToModify)
   {
      if (oldBodyFrame == newBodyFrame)
         return;

      framePoseToModify.getPose(oldBodyFrameDesiredTransform);
      newBodyFrame.getTransformToDesiredFrame(transformFromNewBodyFrameToOldBodyFrame, oldBodyFrame);
      newBodyFrameDesiredTransform.set(oldBodyFrameDesiredTransform);
      newBodyFrameDesiredTransform.multiply(transformFromNewBodyFrameToOldBodyFrame);
      framePoseToModify.setPose(newBodyFrameDesiredTransform);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      super.doTransitionOutOfAction();
      yoDesiredPosition.setToNaN();
      yoDesiredLinearVelocity.setToNaN();
   }

   @Override
   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return null;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return spatialFeedbackControlCommand;
   }
}
