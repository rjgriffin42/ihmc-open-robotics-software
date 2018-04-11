package us.ihmc.quadrupedRobotics;

import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerRequestedEvent;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerEnum;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedSteppingRequestedEvent;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedSteppingStateEnum;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public class QuadrupedForceTestYoVariables extends QuadrupedTestYoVariables
{
   private final YoEnum<QuadrupedForceControllerRequestedEvent> userTrigger;
   private final YoEnum<QuadrupedSteppingRequestedEvent> stepTrigger;
   private final YoEnum<QuadrupedForceControllerEnum> forceControllerState;
   private final YoEnum<QuadrupedSteppingStateEnum> steppingState;

   private final YoDouble stanceHeight;
   private final YoDouble groundPlanePointZ;
   
   // XGait
   private final YoDouble xGaitEndPhaseShiftInput;
   private final YoDouble xGaitEndDoubleSupportDurationInput;
   private final YoDouble xGaitStanceWidthInput;
   private final YoDouble xGaitStanceLengthInput;
   private final YoDouble xGaitStepGroundClearanceInput;
   private final YoDouble xGaitStepDurationInput;
   
   // Step
   private final YoEnum<RobotQuadrant> timedStepQuadrant;
   private final YoDouble timedStepDuration;
   private final YoDouble timedStepGroundClearance;
   private final YoDouble timedStepGoalPositionX;
   private final YoDouble timedStepGoalPositionY;
   private final YoDouble timedStepGoalPositionZ;

   // CoM
   private final YoDouble comPositionEstimateX;
   private final YoDouble comPositionEstimateY;
   private final YoDouble currentHeightInWorld;
   private final YoDouble comPositionEstimateYaw;
   private final YoDouble comPositionEstimatePitch;
   private final YoDouble comPositionEstimateRoll;

   private final YoDouble comPositionSetpointX;
   private final YoDouble comPositionSetpointY;
   private final YoDouble desiredHeightInWorld;

   @SuppressWarnings("unchecked")
   public QuadrupedForceTestYoVariables(SimulationConstructionSet scs)
   {
      super(scs);
      
      userTrigger = (YoEnum<QuadrupedForceControllerRequestedEvent>) scs.getVariable("userTrigger");
      stepTrigger = (YoEnum<QuadrupedSteppingRequestedEvent>) scs.getVariable("stepTrigger");
      forceControllerState = (YoEnum<QuadrupedForceControllerEnum>) scs.getVariable("forceControllerCurrentState");
      steppingState = (YoEnum<QuadrupedSteppingStateEnum>) scs.getVariable("steppingCurrentState");

      stanceHeight = (YoDouble) scs.getVariable("stanceHeight");
      groundPlanePointZ = (YoDouble) scs.getVariable("groundPlanePointZ");
      
      xGaitEndPhaseShiftInput = (YoDouble) scs.getVariable("endPhaseShiftInput");
      xGaitEndDoubleSupportDurationInput = (YoDouble) scs.getVariable("endDoubleSupportDurationInput");
      xGaitStanceWidthInput = (YoDouble) scs.getVariable("stanceWidthInput");
      xGaitStanceLengthInput = (YoDouble) scs.getVariable("stanceLengthInput");
      xGaitStepGroundClearanceInput = (YoDouble) scs.getVariable("stepGroundClearanceInput");
      xGaitStepDurationInput = (YoDouble) scs.getVariable("stepDurationInput");
      
      comPositionEstimateX = (YoDouble) scs.getVariable("comPositionEstimateX");
      comPositionEstimateY = (YoDouble) scs.getVariable("comPositionEstimateY");
      currentHeightInWorld = (YoDouble) scs.getVariable("currentHeightInWorld");
      
      timedStepQuadrant = (YoEnum<RobotQuadrant>) scs.getVariable("timedStepQuadrant");
      timedStepDuration = (YoDouble) scs.getVariable("timedStepDuration");
      timedStepGroundClearance = (YoDouble) scs.getVariable("timedStepGroundClearance");
      timedStepGoalPositionX = (YoDouble) scs.getVariable("timedStepGoalPositionX");
      timedStepGoalPositionY = (YoDouble) scs.getVariable("timedStepGoalPositionY");
      timedStepGoalPositionZ = (YoDouble) scs.getVariable("timedStepGoalPositionZ");

      comPositionEstimateYaw = (YoDouble) scs.getVariable("comPositionEstimateYaw");
      comPositionEstimatePitch = (YoDouble) scs.getVariable("comPositionEstimatePitch");
      comPositionEstimateRoll = (YoDouble) scs.getVariable("comPositionEstimateRoll");

      comPositionSetpointX = (YoDouble) scs.getVariable("comPositionSetpointX");
      comPositionSetpointY = (YoDouble) scs.getVariable("comPositionSetpointY");
      desiredHeightInWorld = (YoDouble) scs.getVariable("desiredHeightInWorld");
   }

   public YoEnum<QuadrupedForceControllerRequestedEvent> getUserTrigger()
   {
      return userTrigger;
   }

   public YoEnum<QuadrupedSteppingRequestedEvent> getStepTrigger()
   {
      return stepTrigger;
   }

   public YoEnum<QuadrupedForceControllerEnum> getForceControllerState()
   {
      return forceControllerState;
   }

   public YoEnum<QuadrupedSteppingStateEnum> getSteppingState()
   {
      return steppingState;
   }

   public YoDouble getStanceHeight()
   {
      return stanceHeight;
   }

   public YoDouble getGroundPlanePointZ()
   {
      return groundPlanePointZ;
   }

   public YoDouble getComPositionEstimateX()
   {
      return comPositionEstimateX;
   }

   public YoDouble getComPositionEstimateY()
   {
      return comPositionEstimateY;
   }

   public YoDouble getCurrentHeightInWorld()
   {
      return currentHeightInWorld;
   }
   
   public YoEnum<RobotQuadrant> getTimedStepQuadrant()
   {
      return timedStepQuadrant;
   }

   public YoDouble getTimedStepDuration()
   {
      return timedStepDuration;
   }

   public YoDouble getTimedStepGroundClearance()
   {
      return timedStepGroundClearance;
   }

   public YoDouble getTimedStepGoalPositionX()
   {
      return timedStepGoalPositionX;
   }

   public YoDouble getTimedStepGoalPositionY()
   {
      return timedStepGoalPositionY;
   }

   public YoDouble getTimedStepGoalPositionZ()
   {
      return timedStepGoalPositionZ;
   }

   public YoDouble getComPositionEstimateYaw()
   {
      return comPositionEstimateYaw;
   }

   public YoDouble getComPositionEstimatePitch()
   {
      return comPositionEstimatePitch;
   }

   public YoDouble getComPositionEstimateRoll()
   {
      return comPositionEstimateRoll;
   }

   public YoDouble getComPositionSetpointX()
   {
      return comPositionSetpointX;
   }

   public YoDouble getComPositionSetpointY()
   {
      return comPositionSetpointY;
   }

   public YoDouble getHeightInWorldSetpoint()
   {
      return desiredHeightInWorld;
   }
}
