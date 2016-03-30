package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.SolverWeightLevels;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.tools.FormattingTools;

import javax.vecmath.Vector3d;

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

   protected boolean trajectoryWasReplanned;

   protected final LegSingularityAndKneeCollapseAvoidanceControlModule legSingularityAndKneeCollapseAvoidanceControlModule;
   private final LegJointLimitAvoidanceControlModule legJointLimitAvoidanceControlModule;

   private final YoFramePoint yoDesiredPosition;
   private final YoFrameVector yoDesiredLinearVelocity;
   private final BooleanYoVariable yoSetDesiredAccelerationToZero;
   private final BooleanYoVariable yoSetDesiredVelocityToZero;

   public AbstractUnconstrainedState(ConstraintType constraintType, FootControlHelper footControlHelper, YoSE3PIDGainsInterface gains,
         YoVariableRegistry registry)
   {
      super(constraintType, footControlHelper, registry);

      this.legSingularityAndKneeCollapseAvoidanceControlModule = footControlHelper.getLegSingularityAndKneeCollapseAvoidanceControlModule();

      RigidBody foot = contactableFoot.getRigidBody();
      String namePrefix = foot.getName() + FormattingTools.underscoredToCamelCase(constraintType.toString().toLowerCase(), true);
      yoDesiredLinearVelocity = new YoFrameVector(namePrefix + "DesiredLinearVelocity", worldFrame, registry);
      yoDesiredLinearVelocity.setToNaN();
      yoDesiredPosition = new YoFramePoint(namePrefix + "DesiredPosition", worldFrame, registry);
      yoDesiredPosition.setToNaN();
      yoSetDesiredAccelerationToZero = new BooleanYoVariable(namePrefix + "SetDesiredAccelerationToZero", registry);
      yoSetDesiredVelocityToZero = new BooleanYoVariable(namePrefix + "SetDesiredVelocityToZero", registry);

      if (USE_ALL_LEG_JOINT_SWING_CORRECTOR)
         legJointLimitAvoidanceControlModule = new LegJointLimitAvoidanceControlModule(namePrefix, registry, momentumBasedController, robotSide);
      else
         legJointLimitAvoidanceControlModule = null;

      spatialFeedbackControlCommand.setWeightForSolver(SolverWeightLevels.FOOT_SWING_WEIGHT);
      spatialFeedbackControlCommand.set(rootBody, foot);
      spatialFeedbackControlCommand.setPrimaryBase(footControlHelper.getMomentumBasedController().getFullRobotModel().getPelvis());
      spatialFeedbackControlCommand.setGains(gains);
      FramePose anklePoseInFoot = new FramePose(contactableFoot.getFrameAfterParentJoint());
      anklePoseInFoot.changeFrame(contactableFoot.getRigidBody().getBodyFixedFrame());
      spatialFeedbackControlCommand.setControlFrameFixedInEndEffector(anklePoseInFoot);
   }

   public void setWeight(double weight)
   {
      spatialFeedbackControlCommand.setWeightForSolver(weight);
   }

   public void setWeights(Vector3d angularWeight, Vector3d linearWeight)
   {
      spatialFeedbackControlCommand.setWeightsForSolver(angularWeight, linearWeight);
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

      initializeTrajectory();
   }

   @Override
   public void doSpecificAction()
   {
      computeAndPackTrajectory();

      if (USE_ALL_LEG_JOINT_SWING_CORRECTOR)
      {
         legJointLimitAvoidanceControlModule.correctSwingFootTrajectory(desiredPosition, desiredOrientation, desiredLinearVelocity, desiredAngularVelocity,
               desiredLinearAcceleration, desiredAngularAcceleration);
      }

      legSingularityAndKneeCollapseAvoidanceControlModule.correctSwingFootTrajectory(desiredPosition, desiredLinearVelocity, desiredLinearAcceleration);

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

      yoDesiredPosition.setAndMatchFrame(desiredPosition);
      yoDesiredLinearVelocity.setAndMatchFrame(desiredLinearVelocity);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      super.doTransitionOutOfAction();
      yoDesiredPosition.setToNaN();
      yoDesiredLinearVelocity.setToNaN();
      trajectoryWasReplanned = false;
   }

   @Override
   public SpatialAccelerationCommand getInverseDynamicsCommand()
   {
      return null;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return spatialFeedbackControlCommand;
   }
}
