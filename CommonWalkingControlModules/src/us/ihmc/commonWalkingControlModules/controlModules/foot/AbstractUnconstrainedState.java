package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.FormattingTools;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.yoUtilities.controllers.YoSE3PIDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFrameVector;


/**
 * The unconstrained state is used if the foot is moved free in space without constrains. Depending on the type of trajectory
 * this can either be a movement along a straight line or (in case of walking) a swing motion.
 * 
 * E.g. the MoveStraightState and the SwingState extend this class and implement the trajectory related methods.
 */
public abstract class AbstractUnconstrainedState extends AbstractFootControlState
{
   private static final boolean CORRECT_SWING_CONSIDERING_JOINT_LIMITS = true;

   protected boolean trajectoryWasReplanned;

   protected final YoSE3PIDGains gains;

   private final LegSingularityAndKneeCollapseAvoidanceControlModule legSingularityAndKneeCollapseAvoidanceControlModule;
   
   private final OneDoFJoint hipYawJoint, anklePitchJoint, ankleRollJoint;
   
   private final YoFramePoint yoDesiredPosition;
   private final YoFrameVector yoDesiredLinearVelocity;

   public AbstractUnconstrainedState(ConstraintType constraintType, RigidBodySpatialAccelerationControlModule accelerationControlModule,
         MomentumBasedController momentumBasedController, ContactablePlaneBody contactableBody, int jacobianId, DoubleYoVariable nullspaceMultiplier,
         BooleanYoVariable jacobianDeterminantInRange, BooleanYoVariable doSingularityEscape,
         LegSingularityAndKneeCollapseAvoidanceControlModule legSingularityAndKneeCollapseAvoidanceControlModule, YoSE3PIDGains gains, RobotSide robotSide,
         YoVariableRegistry registry)
   {
      super(constraintType, accelerationControlModule, momentumBasedController,
            contactableBody, jacobianId, nullspaceMultiplier, jacobianDeterminantInRange, doSingularityEscape,
            robotSide, registry);
      
      this.legSingularityAndKneeCollapseAvoidanceControlModule = legSingularityAndKneeCollapseAvoidanceControlModule;
      this.gains = gains;

      this.hipYawJoint = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.HIP_YAW);
      this.ankleRollJoint = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.ANKLE_ROLL);
      this.anklePitchJoint = momentumBasedController.getFullRobotModel().getLegJoint(robotSide, LegJointName.ANKLE_PITCH);
   
      RigidBody foot = contactableBody.getRigidBody();
      String namePrefix = foot.getName() + FormattingTools.underscoredToCamelCase(constraintType.toString().toLowerCase(), true);
      yoDesiredLinearVelocity = new YoFrameVector(namePrefix + "DesiredLinearVelocity", worldFrame, registry);
      yoDesiredLinearVelocity.setToNaN();
      yoDesiredPosition = new YoFramePoint(namePrefix + "DesiredPosition", worldFrame, registry);
      yoDesiredPosition.setToNaN();
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

   public void doTransitionIntoAction()
   {
      legSingularityAndKneeCollapseAvoidanceControlModule.setCheckVelocityForSwingSingularityAvoidance(true);

      accelerationControlModule.reset();

      isCoPOnEdge = false;
      initializeTrajectory();

      accelerationControlModule.setGains(gains);
   }

   public void doSpecificAction()
   {
      accelerationControlModule.setGains(gains);

      if (doSingularityEscape.getBooleanValue())
      {
         initializeTrajectory();
      }

      computeAndPackTrajectory();

      desiredOrientation.setIncludingFrame(trajectoryOrientation);

      if (CORRECT_SWING_CONSIDERING_JOINT_LIMITS)
         correctInputsAccordingToJointLimits();

      legSingularityAndKneeCollapseAvoidanceControlModule.correctSwingFootTrajectoryForSingularityAvoidance(desiredPosition, desiredLinearVelocity,
            desiredLinearAcceleration);

      accelerationControlModule.doPositionControl(desiredPosition, desiredOrientation, desiredLinearVelocity, desiredAngularVelocity,
            desiredLinearAcceleration, desiredAngularAcceleration, rootBody);
      accelerationControlModule.packAcceleration(footAcceleration);

      setTaskspaceConstraint(footAcceleration);
      
      desiredPosition.changeFrame(worldFrame);
      yoDesiredPosition.set(desiredPosition);
      desiredLinearVelocity.changeFrame(worldFrame);
      yoDesiredLinearVelocity.set(desiredLinearVelocity);
   }

   private final double[] desiredYawPitchRoll = new double[3];
   private final double epsilon = 1e-3;

   // TODO Pretty much hackish... 
   private void correctInputsAccordingToJointLimits()
   {
      ReferenceFrame frameBeforeHipYawJoint = hipYawJoint.getFrameBeforeJoint();
      desiredOrientation.changeFrame(frameBeforeHipYawJoint);
      desiredOrientation.getYawPitchRoll(desiredYawPitchRoll);
      if (desiredYawPitchRoll[0] > hipYawJoint.getJointLimitUpper() - epsilon)
      {
         desiredYawPitchRoll[0] = hipYawJoint.getJointLimitUpper();
         desiredAngularVelocity.changeFrame(frameBeforeHipYawJoint);
         desiredAngularVelocity.setZ(Math.min(0.0, desiredAngularVelocity.getZ()));
         desiredAngularAcceleration.changeFrame(frameBeforeHipYawJoint);
         desiredAngularAcceleration.setZ(Math.min(0.0, desiredAngularVelocity.getZ()));
      }
      else if (desiredYawPitchRoll[0] < hipYawJoint.getJointLimitLower() + epsilon)
      {
         desiredYawPitchRoll[0] = hipYawJoint.getJointLimitLower();
         desiredAngularVelocity.changeFrame(frameBeforeHipYawJoint);
         desiredAngularVelocity.setZ(Math.max(0.0, desiredAngularVelocity.getZ()));
         desiredAngularAcceleration.changeFrame(frameBeforeHipYawJoint);
         desiredAngularAcceleration.setZ(Math.max(0.0, desiredAngularVelocity.getZ()));
      }
      desiredOrientation.setYawPitchRoll(desiredYawPitchRoll);

      ReferenceFrame frameBeforeAnklePitchJoint = anklePitchJoint.getFrameBeforeJoint();
      desiredOrientation.changeFrame(frameBeforeAnklePitchJoint);
      desiredOrientation.getYawPitchRoll(desiredYawPitchRoll);
      if (desiredYawPitchRoll[1] > anklePitchJoint.getJointLimitUpper() - epsilon)
      {
         desiredYawPitchRoll[1] = anklePitchJoint.getJointLimitUpper();
         desiredAngularVelocity.changeFrame(frameBeforeAnklePitchJoint);
         desiredAngularVelocity.setY(Math.min(0.0, desiredAngularVelocity.getY()));
         desiredAngularAcceleration.changeFrame(frameBeforeAnklePitchJoint);
         desiredAngularAcceleration.setY(Math.min(0.0, desiredAngularVelocity.getY()));
      }
      else if (desiredYawPitchRoll[1] < anklePitchJoint.getJointLimitLower() + epsilon)
      {
         desiredYawPitchRoll[1] = anklePitchJoint.getJointLimitLower();
         desiredAngularVelocity.changeFrame(frameBeforeAnklePitchJoint);
         desiredAngularVelocity.setY(Math.max(0.0, desiredAngularVelocity.getY()));
         desiredAngularAcceleration.changeFrame(frameBeforeAnklePitchJoint);
         desiredAngularAcceleration.setY(Math.max(0.0, desiredAngularVelocity.getY()));
      }
      desiredOrientation.setYawPitchRoll(desiredYawPitchRoll);

      ReferenceFrame frameBeforeAnkleRollJoint = ankleRollJoint.getFrameBeforeJoint();
      desiredOrientation.changeFrame(frameBeforeAnkleRollJoint);
      desiredOrientation.getYawPitchRoll(desiredYawPitchRoll);
      if (desiredYawPitchRoll[2] > ankleRollJoint.getJointLimitUpper() - epsilon)
      {
         desiredYawPitchRoll[2] = ankleRollJoint.getJointLimitUpper();
         desiredAngularVelocity.changeFrame(frameBeforeAnkleRollJoint);
         desiredAngularVelocity.setX(Math.min(0.0, desiredAngularVelocity.getX()));
         desiredAngularAcceleration.changeFrame(frameBeforeAnkleRollJoint);
         desiredAngularAcceleration.setX(Math.min(0.0, desiredAngularVelocity.getX()));
      }
      else if (desiredYawPitchRoll[2] < ankleRollJoint.getJointLimitLower() + epsilon)
      {
         desiredYawPitchRoll[2] = ankleRollJoint.getJointLimitLower();
         desiredAngularVelocity.changeFrame(frameBeforeAnkleRollJoint);
         desiredAngularVelocity.setX(Math.max(0.0, desiredAngularVelocity.getX()));
         desiredAngularAcceleration.changeFrame(frameBeforeAnkleRollJoint);
         desiredAngularAcceleration.setX(Math.max(0.0, desiredAngularVelocity.getX()));
      }
      desiredOrientation.setYawPitchRoll(desiredYawPitchRoll);

      desiredOrientation.changeFrame(worldFrame);
      desiredAngularVelocity.changeFrame(worldFrame);
      desiredAngularAcceleration.changeFrame(worldFrame);
   }

   public void doTransitionOutOfAction()
   {
      yoDesiredPosition.setToNaN();
      yoDesiredLinearVelocity.setToNaN();
      trajectoryWasReplanned = false;

      accelerationControlModule.reset();
   }
}
