package us.ihmc.commonWalkingControlModules.controlModules.legConfiguration;

import us.ihmc.commonWalkingControlModules.configurations.StraightLegWalkingParameters;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.InterpolationTools;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.FinishableState;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.GenericStateMachine;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateMachineTools;

import java.util.ArrayList;
import java.util.List;

public class LegConfigurationControlModule
{
   public enum LegConfigurationType
   {
      STRAIGHTEN, STRAIGHT, COLLAPSE, BENT
   }

   private static final double minimumDampingScale = 0.2;
   private static final boolean scaleDamping = true;

   private static final boolean ONLY_MOVE_PRIV_POS_IF_NOT_BENDING = false;

   private final YoVariableRegistry registry;

   private final PrivilegedAccelerationCommand privilegedAccelerationCommand = new PrivilegedAccelerationCommand();

   private final YoEnum<LegConfigurationType> requestedState;
   private final GenericStateMachine<LegConfigurationType, FinishableState<LegConfigurationType>> stateMachine;

   private final YoDouble legPitchPrivilegedWeight;

   private final YoDouble kneeStraightPrivilegedWeight;
   private final YoDouble straightJointSpacePositionGain;
   private final YoDouble straightJointSpaceVelocityGain;
   private final YoDouble straightActuatorSpacePositionGain;
   private final YoDouble straightActuatorSpaceVelocityGain;
   private final YoDouble straightMaxPositionBlendingFactor;
   private final YoDouble straightMaxVelocityBlendingFactor;

   private final YoDouble kneeBentPrivilegedWeight;
   private final YoDouble bentJointSpacePositionGain;
   private final YoDouble bentJointSpaceVelocityGain;
   private final YoDouble bentActuatorSpaceVelocityGain;
   private final YoDouble bentActuatorSpacePositionGain;
   private final YoDouble bentMaxPositionBlendingFactor;
   private final YoDouble bentMaxVelocityBlendingFactor;

   private final YoDouble kneePitchPrivilegedConfiguration;
   private final YoDouble kneePitchPrivilegedError;

   private final YoDouble kneePrivilegedPAction;
   private final YoDouble kneePrivilegedDAction;
   private final YoDouble privilegedMaxAcceleration;

   private final YoDouble effectiveKneeStiffness;
   private final YoDouble effectiveKneeDamping;

   private final YoBoolean useFullyExtendedLeg;
   private final YoBoolean useBracingLeg;
   private final YoDouble desiredAngle;
   private final YoDouble desiredAngleWhenStraight;
   private final YoDouble desiredAngleWhenExtended;
   private final YoDouble desiredAngleWhenBracing;

   private final YoDouble straighteningSpeed;
   private final YoDouble collapsingDuration;

   private final YoDouble desiredVirtualActuatorLength;
   private final YoDouble currentVirtualActuatorLength;
   private final YoDouble currentVirtualActuatorVelocity;

   private final OneDoFJoint kneePitchJoint;

   private final YoDouble positionBlendingFactor;
   private final YoDouble velocityBlendingFactor;
   private final YoDouble dampingActionScaleFactor;

   private static final int hipPitchJointIndex = 0;
   private static final int kneePitchJointIndex = 1;
   private static final int anklePitchJointIndex = 2;

   private final double kneeRangeOfMotion;
   private final double kneeSquareRangeOfMotion;
   private final double kneeMidRangeOfMotion;

   private final double thighLength;
   private final double shinLength;

   private final LegConfigurationGains straightLegGains;
   private final LegConfigurationGains bentLegGains;

   private boolean blendPositionError;
   private boolean blendVelocityError;
   private double jointSpaceConfigurationGain;
   private double jointSpaceVelocityGain;
   private double actuatorSpaceConfigurationGain;
   private double actuatorSpaceVelocityGain;
   private double maxPositionBlendingFactor;
   private double maxVelocityBlendingFactor;

   private double kneePitchPrivilegedConfigurationWeight;

   public LegConfigurationControlModule(RobotSide robotSide, HighLevelHumanoidControllerToolbox controllerToolbox, StraightLegWalkingParameters straightLegWalkingParameters,
                                        YoVariableRegistry parentRegistry)
   {
      String sidePrefix = robotSide.getCamelCaseNameForStartOfExpression();
      String namePrefix = sidePrefix + "Leg";
      registry = new YoVariableRegistry(sidePrefix + getClass().getSimpleName());

      kneePitchJoint = controllerToolbox.getFullRobotModel().getLegJoint(robotSide, LegJointName.KNEE_PITCH);
      double kneeLimitUpper = kneePitchJoint.getJointLimitUpper();
      if (Double.isNaN(kneeLimitUpper) || Double.isInfinite(kneeLimitUpper))
         kneeLimitUpper = Math.PI;
      double kneeLimitLower = kneePitchJoint.getJointLimitLower();
      if (Double.isNaN(kneeLimitLower) || Double.isInfinite(kneeLimitLower))
         kneeLimitLower = -Math.PI;
      kneeSquareRangeOfMotion = MathTools.square(kneeLimitUpper - kneeLimitLower);
      kneeRangeOfMotion = kneeLimitUpper - kneeLimitLower;
      kneeMidRangeOfMotion = 0.5 * (kneeLimitUpper + kneeLimitLower);

      OneDoFJoint hipPitchJoint = controllerToolbox.getFullRobotModel().getLegJoint(robotSide, LegJointName.HIP_PITCH);
      OneDoFJoint anklePitchJoint = controllerToolbox.getFullRobotModel().getLegJoint(robotSide, LegJointName.ANKLE_PITCH);
      privilegedAccelerationCommand.addJoint(hipPitchJoint, Double.NaN);
      privilegedAccelerationCommand.addJoint(kneePitchJoint, Double.NaN);
      privilegedAccelerationCommand.addJoint(anklePitchJoint, Double.NaN);

      legPitchPrivilegedWeight = new YoDouble(sidePrefix + "LegPitchPrivilegedWeight", registry);

      kneeStraightPrivilegedWeight = new YoDouble(sidePrefix + "KneeStraightPrivilegedWeight", registry);
      straightJointSpacePositionGain = new YoDouble(sidePrefix + "StraightLegJointSpaceKp", registry);
      straightJointSpaceVelocityGain = new YoDouble(sidePrefix + "StraightLegJointSpaceKv", registry);
      straightActuatorSpacePositionGain = new YoDouble(sidePrefix + "StraightLegActuatorSpaceKp", registry);
      straightActuatorSpaceVelocityGain = new YoDouble(sidePrefix + "StraightLegActuatorSpaceKv", registry);
      straightMaxPositionBlendingFactor = new YoDouble(sidePrefix + "StraightMaxPositionBlendingFactor", registry);
      straightMaxVelocityBlendingFactor = new YoDouble(sidePrefix + "StraightMaxVelocityBlendingFactor", registry);

      kneeBentPrivilegedWeight = new YoDouble(sidePrefix + "KneeBentPrivilegedWeight", registry);
      bentJointSpacePositionGain = new YoDouble(sidePrefix + "BentLegJointSpaceKp", registry);
      bentJointSpaceVelocityGain = new YoDouble(sidePrefix + "BentLegJointSpaceKv", registry);
      bentActuatorSpacePositionGain = new YoDouble(sidePrefix + "BentLegActuatorSpaceKp", registry);
      bentActuatorSpaceVelocityGain = new YoDouble(sidePrefix + "BentLegActuatorSpaceKv", registry);
      bentMaxPositionBlendingFactor = new YoDouble(sidePrefix + "BentMaxPositionBlendingFactor", registry);
      bentMaxVelocityBlendingFactor = new YoDouble(sidePrefix + "BentMaxVelocityBlendingFactor", registry);

      kneePitchPrivilegedConfiguration = new YoDouble(sidePrefix + "KneePitchPrivilegedConfiguration", registry);
      privilegedMaxAcceleration = new YoDouble(sidePrefix + "LegPrivilegedMaxAcceleration", registry);

      kneePitchPrivilegedError = new YoDouble(sidePrefix + "KneePitchPrivilegedError", registry);
      kneePrivilegedPAction = new YoDouble(sidePrefix + "KneePrivilegedPAction", registry);
      kneePrivilegedDAction = new YoDouble(sidePrefix + "KneePrivilegedDAction", registry);

      effectiveKneeStiffness = new YoDouble(sidePrefix + "EffectiveKneeStiffness", registry);
      effectiveKneeDamping = new YoDouble(sidePrefix + "EffectiveKneeDamping", registry);

      legPitchPrivilegedWeight.set(straightLegWalkingParameters.getLegPitchPrivilegedWeight());

      straightLegGains = straightLegWalkingParameters.getStraightLegGains();
      bentLegGains = straightLegWalkingParameters.getBentLegGains();

      kneeStraightPrivilegedWeight.set(straightLegWalkingParameters.getKneeStraightLegPrivilegedWeight());
      straightJointSpacePositionGain.set(straightLegGains.getJointSpaceKp());
      straightJointSpaceVelocityGain.set(straightLegGains.getJointSpaceKd());
      straightActuatorSpacePositionGain.set(straightLegGains.getActuatorSpaceKp());
      straightActuatorSpaceVelocityGain.set(straightLegGains.getActuatorSpaceKd());
      straightMaxPositionBlendingFactor.set(straightLegGains.getMaxPositionBlendingFactor());
      straightMaxVelocityBlendingFactor.set(straightLegGains.getMaxVelocityBlendingFactor());

      kneeBentPrivilegedWeight.set(straightLegWalkingParameters.getKneeBentLegPrivilegedWeight());
      bentJointSpacePositionGain.set(bentLegGains.getJointSpaceKp());
      bentJointSpaceVelocityGain.set(bentLegGains.getJointSpaceKd());
      bentActuatorSpacePositionGain.set(bentLegGains.getActuatorSpaceKp());
      bentActuatorSpaceVelocityGain.set(bentLegGains.getActuatorSpaceKd());
      bentMaxPositionBlendingFactor.set(bentLegGains.getMaxPositionBlendingFactor());
      bentMaxVelocityBlendingFactor.set(bentLegGains.getMaxVelocityBlendingFactor());

      privilegedMaxAcceleration.set(straightLegWalkingParameters.getPrivilegedMaxAcceleration());

      positionBlendingFactor = new YoDouble(namePrefix + "PositionBlendingFactor", registry);
      velocityBlendingFactor = new YoDouble(namePrefix + "VelocityBlendingFactor", registry);
      dampingActionScaleFactor = new YoDouble(namePrefix + "DampingActionScaleFactor", registry);

      useFullyExtendedLeg = new YoBoolean(namePrefix + "UseFullyExtendedLeg", registry);
      useBracingLeg = new YoBoolean(namePrefix + "UseBracingLeg", registry);

      desiredAngle = new YoDouble(namePrefix + "DesiredAngle", registry);

      desiredAngleWhenStraight = new YoDouble(namePrefix + "DesiredAngleWhenStraight", registry);
      desiredAngleWhenStraight.set(straightLegWalkingParameters.getStraightKneeAngle());

      desiredAngleWhenExtended = new YoDouble(namePrefix + "DesiredAngleWhenExtended", registry);
      desiredAngleWhenExtended.set(0.0);

      desiredAngleWhenBracing = new YoDouble(namePrefix + "DesiredAngleWhenBracing", registry);
      desiredAngleWhenBracing.set(0.4);

      straighteningSpeed = new YoDouble(namePrefix + "SupportKneeStraighteningSpeed", registry);
      straighteningSpeed.set(straightLegWalkingParameters.getSpeedForSupportKneeStraightening());

      collapsingDuration = new YoDouble(namePrefix + "SupportKneeCollapsingDuration", registry);
      collapsingDuration.set(straightLegWalkingParameters.getSupportKneeCollapsingDuration());

      desiredVirtualActuatorLength = new YoDouble(namePrefix + "DesiredVirtualActuatorLength", registry);
      currentVirtualActuatorLength = new YoDouble(namePrefix + "CurrentVirtualActuatorLength", registry);
      currentVirtualActuatorVelocity = new YoDouble(namePrefix + "CurrentVirtualActuatorVelocity", registry);

      // set up states and state machine
      YoDouble time = controllerToolbox.getYoTime();
      stateMachine = new GenericStateMachine<>(namePrefix + "State", namePrefix + "SwitchTime", LegConfigurationType.class, time, registry);
      requestedState = YoEnum.create(namePrefix + "RequestedState", "", LegConfigurationType.class, registry, true);
      requestedState.set(null);

      // compute leg segment lengths
      FullHumanoidRobotModel fullRobotModel = controllerToolbox.getFullRobotModel();
      ReferenceFrame hipPitchFrame = fullRobotModel.getLegJoint(RobotSide.LEFT, LegJointName.HIP_PITCH).getFrameAfterJoint();
      FramePoint hipPoint = new FramePoint(hipPitchFrame);
      FramePoint kneePoint = new FramePoint(fullRobotModel.getLegJoint(RobotSide.LEFT, LegJointName.KNEE_PITCH).getFrameBeforeJoint());
      kneePoint.changeFrame(hipPitchFrame);

      thighLength = hipPoint.distance(kneePoint);

      ReferenceFrame kneePitchFrame = fullRobotModel.getLegJoint(RobotSide.LEFT, LegJointName.KNEE_PITCH).getFrameAfterJoint();
      kneePoint.setToZero(kneePitchFrame);
      FramePoint anklePoint = new FramePoint(fullRobotModel.getLegJoint(RobotSide.LEFT, LegJointName.ANKLE_PITCH).getFrameBeforeJoint());
      anklePoint.changeFrame(kneePitchFrame);

      shinLength = kneePoint.distance(anklePoint);

      setupStateMachine();

      if (straightLegWalkingParameters.attemptToStraightenLegs())
         stateMachine.setCurrentState(LegConfigurationType.STRAIGHT);
      else
         stateMachine.setCurrentState(LegConfigurationType.BENT);

      parentRegistry.addChild(registry);
   }

   private void setupStateMachine()
   {
      List<FinishableState<LegConfigurationType>> states = new ArrayList<>();

      FinishableState<LegConfigurationType> straighteningToStraightState = new StraighteningKneeControlState(straighteningSpeed);
      FinishableState<LegConfigurationType> straightState = new StraightKneeControlState();
      FinishableState<LegConfigurationType> bentState = new BentKneeControlState();
      FinishableState<LegConfigurationType> collapseState = new CollapseKneeControlState();
      states.add(straighteningToStraightState);
      states.add(straightState);
      states.add(bentState);
      states.add(collapseState);

      straighteningToStraightState.setDefaultNextState(LegConfigurationType.STRAIGHT);
      collapseState.setDefaultNextState(LegConfigurationType.BENT);

      for (FinishableState<LegConfigurationType> fromState : states)
      {
         for (FinishableState<LegConfigurationType> toState : states)
         {
            StateMachineTools.addRequestedStateTransition(requestedState, false, fromState, toState);
         }
      }

      for (FinishableState<LegConfigurationType> state : states)
      {
         stateMachine.addState(state);
      }
   }

   public void initialize()
   {
   }

   public void doControl()
   {
      if (useBracingLeg.getBooleanValue())
         desiredAngle.set(desiredAngleWhenBracing.getDoubleValue());
      else if (useFullyExtendedLeg.getBooleanValue())
         desiredAngle.set(desiredAngleWhenExtended.getDoubleValue());
      else
         desiredAngle.set(desiredAngleWhenStraight.getDoubleValue());

      stateMachine.checkTransitionConditions();
      stateMachine.getCurrentState().doAction();

      double privilegedKneeAcceleration = computeKneeAcceleration();
      double privilegedHipPitchAcceleration = -0.5 * privilegedKneeAcceleration;
      double privilegedAnklePitchAcceleration = -0.5 * privilegedKneeAcceleration;

      privilegedAccelerationCommand.setOneDoFJoint(hipPitchJointIndex, privilegedHipPitchAcceleration);
      privilegedAccelerationCommand.setOneDoFJoint(kneePitchJointIndex, privilegedKneeAcceleration);
      privilegedAccelerationCommand.setOneDoFJoint(anklePitchJointIndex, privilegedAnklePitchAcceleration);

      privilegedAccelerationCommand.setWeight(hipPitchJointIndex, legPitchPrivilegedWeight.getDoubleValue());
      privilegedAccelerationCommand.setWeight(kneePitchJointIndex, kneePitchPrivilegedConfigurationWeight);
      privilegedAccelerationCommand.setWeight(anklePitchJointIndex, legPitchPrivilegedWeight.getDoubleValue());
   }

   public void setFullyExtendLeg(boolean fullyExtendLeg)
   {
      useFullyExtendedLeg.set(fullyExtendLeg);
   }

   public void setLegBracing(boolean legBracing)
   {
      useBracingLeg.set(legBracing);
   }

   private double computeKneeAcceleration()
   {
      double currentPosition = kneePitchJoint.getQ();

      double desiredVirtualLength = computeVirtualActuatorLength(kneePitchPrivilegedConfiguration.getDoubleValue());
      double currentVirtualLength = computeVirtualActuatorLength(currentPosition);

      desiredVirtualActuatorLength.set(desiredVirtualLength);
      currentVirtualActuatorLength.set(currentVirtualLength);

      double error = kneePitchPrivilegedConfiguration.getDoubleValue() - currentPosition;
      double virtualError = desiredVirtualLength - currentVirtualLength;
      kneePitchPrivilegedError.set(error);

      double currentVirtualVelocity = computeVirtualActuatorVelocity(currentPosition, kneePitchJoint.getQd());
      currentVirtualActuatorVelocity.set(currentVirtualVelocity);

      double percentDistanceToMidRange = MathTools.clamp(Math.abs(currentPosition - kneeMidRangeOfMotion) / (0.5 * kneeRangeOfMotion), 0.0, 1.0);
      double positionBlendingFactor = Math.min(maxPositionBlendingFactor * percentDistanceToMidRange, 1.0);
      double velocityBlendingFactor = Math.min(maxVelocityBlendingFactor * percentDistanceToMidRange, 1.0);
      this.positionBlendingFactor.set(positionBlendingFactor);
      this.velocityBlendingFactor.set(velocityBlendingFactor);

      double jointSpaceKp = 2.0 * jointSpaceConfigurationGain / kneeSquareRangeOfMotion;

      double jointSpacePAction = jointSpaceKp * error;
      double actuatorSpacePAction = -actuatorSpaceConfigurationGain * virtualError;

      // modify gains based on error. If there's a big error, don't damp velocities
      double percentError = Math.abs(error) / (0.5 * kneeRangeOfMotion);
      double dampingActionScaleFactor;
      if (scaleDamping)
         dampingActionScaleFactor = MathTools.clamp(1.0 - (1.0 - minimumDampingScale) * percentError, 0.0, 1.0);
      else
         dampingActionScaleFactor = 1.0;
      this.dampingActionScaleFactor.set(dampingActionScaleFactor);

      double jointSpaceDAction = dampingActionScaleFactor * jointSpaceVelocityGain * -kneePitchJoint.getQd();
      double actuatorSpaceDAction = dampingActionScaleFactor * actuatorSpaceVelocityGain * currentVirtualVelocity;

      double pAction, dAction;

      if (blendPositionError)
         pAction = InterpolationTools.linearInterpolate(jointSpacePAction, actuatorSpacePAction, positionBlendingFactor);
      else
         pAction = jointSpacePAction;

      if (blendVelocityError)
         dAction = InterpolationTools.linearInterpolate(jointSpaceDAction, actuatorSpaceDAction, velocityBlendingFactor);
      else
         dAction = jointSpaceDAction;

      kneePrivilegedPAction.set(pAction);
      kneePrivilegedDAction.set(dAction);

      effectiveKneeStiffness.set(pAction / error);
      effectiveKneeDamping.set(-dAction / kneePitchJoint.getQd());

      return MathTools.clamp(kneePrivilegedPAction.getDoubleValue() + kneePrivilegedDAction.getDoubleValue(), privilegedMaxAcceleration.getDoubleValue());
   }

   private double computeVirtualActuatorLength(double kneePitchAngle)
   {
      double length = Math.pow(thighLength, 2.0) + Math.pow(shinLength, 2.0) + 2.0 * thighLength * shinLength * Math.cos(kneePitchAngle);
      return Math.sqrt(length);
   }

   private double computeVirtualActuatorVelocity(double kneePitchAngle, double kneePitchVelocity)
   {
      double virtualLength = computeVirtualActuatorLength(kneePitchAngle);
      double velocity = -thighLength * shinLength / virtualLength * kneePitchVelocity * Math.sin(kneePitchAngle);
      return velocity;
   }

   public void setKneeAngleState(LegConfigurationType controlType)
   {
      requestedState.set(controlType);
   }

   public LegConfigurationType getCurrentKneeControlState()
   {
      return stateMachine.getCurrentStateEnum();
   }

   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return privilegedAccelerationCommand;
   }

   private class StraighteningKneeControlState extends FinishableState<LegConfigurationType>
   {
      private final YoDouble yoStraighteningSpeed;

      private double startingPosition;
      private double previousKneePitchAngle;

      private double timeUntilStraight;
      private double straighteningSpeed;

      private double dwellTime;
      private double desiredPrivilegedPosition;

      private double previousTime;

      public StraighteningKneeControlState(YoDouble straighteningSpeed)
      {
         super(LegConfigurationType.STRAIGHTEN);

         this.yoStraighteningSpeed = straighteningSpeed;
      }

      @Override
      public boolean isDone()
      {
         return getTimeInCurrentState() > (timeUntilStraight + dwellTime);
      }

      @Override
      public void doAction()
      {
         double estimatedDT = estimateDT();
         double currentPosition = kneePitchJoint.getQ();

         if (ONLY_MOVE_PRIV_POS_IF_NOT_BENDING)
         {
            if (currentPosition > previousKneePitchAngle && currentPosition > startingPosition) // the knee is bending
               dwellTime += estimatedDT;
            else
               desiredPrivilegedPosition -= estimatedDT * straighteningSpeed;
         }
         else
         {
            desiredPrivilegedPosition -= estimatedDT * straighteningSpeed;
         }

         kneePitchPrivilegedConfiguration.set(desiredPrivilegedPosition);

         jointSpaceConfigurationGain = straightJointSpacePositionGain.getDoubleValue();
         jointSpaceVelocityGain = straightJointSpaceVelocityGain.getDoubleValue();
         actuatorSpaceConfigurationGain = straightActuatorSpacePositionGain.getDoubleValue();
         actuatorSpaceVelocityGain = straightActuatorSpaceVelocityGain.getDoubleValue();
         maxPositionBlendingFactor = straightMaxPositionBlendingFactor.getDoubleValue();
         maxVelocityBlendingFactor = straightMaxVelocityBlendingFactor.getDoubleValue();

         blendPositionError = straightLegGains.getBlendPositionError();
         blendVelocityError = straightLegGains.getBlendVelocityError();

         kneePitchPrivilegedConfigurationWeight = kneeStraightPrivilegedWeight.getDoubleValue();

         previousKneePitchAngle = currentPosition;

         if (isDone())
            transitionToDefaultNextState();
      }



      @Override
      public void doTransitionIntoAction()
      {
         startingPosition = kneePitchJoint.getQ();
         previousKneePitchAngle = kneePitchJoint.getQ();

         straighteningSpeed = yoStraighteningSpeed.getDoubleValue();
         timeUntilStraight = (startingPosition - desiredAngle.getDoubleValue()) / straighteningSpeed;
         timeUntilStraight = Math.max(timeUntilStraight, 0.0);

         desiredPrivilegedPosition = startingPosition;

         previousTime = 0.0;
         dwellTime = 0.0;
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }

      private double estimateDT()
      {
         double currentTime = getTimeInCurrentState();

         double estimatedDT = currentTime - previousTime;
         previousTime = currentTime;

         return estimatedDT;
      }
   }

   private class StraightKneeControlState extends FinishableState<LegConfigurationType>
   {
      public StraightKneeControlState()
      {
         super(LegConfigurationType.STRAIGHT);
      }

      @Override
      public boolean isDone()
      {
         return false;
      }

      @Override
      public void doAction()
      {
         kneePitchPrivilegedConfiguration.set(desiredAngle.getDoubleValue());

         jointSpaceConfigurationGain = straightJointSpacePositionGain.getDoubleValue();
         jointSpaceVelocityGain = straightJointSpaceVelocityGain.getDoubleValue();
         actuatorSpaceConfigurationGain = straightActuatorSpacePositionGain.getDoubleValue();
         actuatorSpaceVelocityGain = straightActuatorSpaceVelocityGain.getDoubleValue();
         maxPositionBlendingFactor = straightMaxPositionBlendingFactor.getDoubleValue();
         maxVelocityBlendingFactor = straightMaxVelocityBlendingFactor.getDoubleValue();

         blendPositionError = straightLegGains.getBlendPositionError();
         blendVelocityError = straightLegGains.getBlendVelocityError();

         kneePitchPrivilegedConfigurationWeight = kneeStraightPrivilegedWeight.getDoubleValue();
      }

      @Override
      public void doTransitionIntoAction()
      {
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }

   private class BentKneeControlState extends FinishableState<LegConfigurationType>
   {
      public BentKneeControlState()
      {
         super(LegConfigurationType.BENT);
      }

      @Override
      public boolean isDone()
      {
         return false;
      }

      @Override
      public void doAction()
      {
         kneePitchPrivilegedConfiguration.set(kneeMidRangeOfMotion);

         jointSpaceConfigurationGain = bentJointSpacePositionGain.getDoubleValue();
         jointSpaceVelocityGain = bentJointSpaceVelocityGain.getDoubleValue();
         actuatorSpaceConfigurationGain = bentActuatorSpacePositionGain.getDoubleValue();
         actuatorSpaceVelocityGain = bentActuatorSpaceVelocityGain.getDoubleValue();
         maxPositionBlendingFactor = bentMaxPositionBlendingFactor.getDoubleValue();
         maxVelocityBlendingFactor = bentMaxVelocityBlendingFactor.getDoubleValue();

         blendPositionError = bentLegGains.getBlendPositionError();
         blendVelocityError = bentLegGains.getBlendVelocityError();

         kneePitchPrivilegedConfigurationWeight = kneeBentPrivilegedWeight.getDoubleValue();
      }

      @Override
      public void doTransitionIntoAction()
      {
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }

   private class CollapseKneeControlState extends FinishableState<LegConfigurationType>
   {
      public CollapseKneeControlState()
      {
         super(LegConfigurationType.COLLAPSE);
      }

      @Override
      public boolean isDone()
      {
         return getTimeInCurrentState() > collapsingDuration.getDoubleValue();
      }

      @Override
      public void doAction()
      {
         double desiredKneePosition = InterpolationTools.linearInterpolate(desiredAngle.getDoubleValue(), kneeMidRangeOfMotion,
               getTimeInCurrentState() / collapsingDuration.getDoubleValue());

         kneePitchPrivilegedConfiguration.set(desiredKneePosition);

         jointSpaceConfigurationGain = bentJointSpacePositionGain.getDoubleValue();
         jointSpaceVelocityGain = bentJointSpaceVelocityGain.getDoubleValue();
         actuatorSpaceConfigurationGain = bentActuatorSpacePositionGain.getDoubleValue();
         actuatorSpaceVelocityGain = bentActuatorSpaceVelocityGain.getDoubleValue();
         maxPositionBlendingFactor = bentMaxPositionBlendingFactor.getDoubleValue();
         maxVelocityBlendingFactor = bentMaxVelocityBlendingFactor.getDoubleValue();

         blendPositionError = bentLegGains.getBlendPositionError();
         blendVelocityError = bentLegGains.getBlendVelocityError();

         kneePitchPrivilegedConfigurationWeight = kneeBentPrivilegedWeight.getDoubleValue();

         if (isDone())
            transitionToDefaultNextState();
      }

      @Override
      public void doTransitionIntoAction()
      {
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }
}
