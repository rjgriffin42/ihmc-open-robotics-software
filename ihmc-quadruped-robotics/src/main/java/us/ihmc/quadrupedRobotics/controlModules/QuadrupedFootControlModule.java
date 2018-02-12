package us.ihmc.quadrupedRobotics.controlModules;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedSolePositionController;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedStepTransitionCallback;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.QuadrupedTaskSpaceEstimates;
import us.ihmc.quadrupedRobotics.planning.ContactState;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.YoQuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.trajectory.ThreeDoFSwingFootTrajectory;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachine;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachineBuilder;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachineState;
import us.ihmc.robotics.stateMachines.eventBasedStateMachine.FiniteStateMachineStateChangedListener;
import us.ihmc.quadrupedRobotics.util.TimeInterval;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class QuadrupedFootControlModule
{

   // control variables
   private final YoVariableRegistry registry;
   private final YoDouble timestamp;
   private final QuadrupedSolePositionController solePositionController;
   private final QuadrupedSolePositionController.Setpoints solePositionControllerSetpoints;
   private final FrameVector3D soleForceCommand;
   private final YoQuadrupedTimedStep stepCommand;
   private final YoBoolean stepCommandIsValid;
   private final QuadrupedTaskSpaceEstimates taskSpaceEstimates;
   private final QuadrupedFootControlModuleParameters parameters;

   // foot state machine
   public enum FootState
   {
      SUPPORT, SWING
   }

   public enum FootEvent
   {
      TIMEOUT
   }
   private final FiniteStateMachine<FootState, FootEvent, FiniteStateMachineState<FootEvent>> footStateMachine;
   private QuadrupedStepTransitionCallback stepTransitionCallback;

   public QuadrupedFootControlModule(QuadrupedFootControlModuleParameters parameters, RobotQuadrant robotQuadrant, QuadrupedSolePositionController solePositionController,
                                     YoDouble timestamp, YoVariableRegistry parentRegistry)
   {
      // control variables
      String prefix = robotQuadrant.getCamelCaseName();
      this.registry = new YoVariableRegistry(robotQuadrant.getPascalCaseName() + getClass().getSimpleName());
      this.timestamp = timestamp;
      this.solePositionController = solePositionController;
      this.solePositionControllerSetpoints = new QuadrupedSolePositionController.Setpoints(robotQuadrant);
      this.soleForceCommand = new FrameVector3D();
      this.stepCommand = new YoQuadrupedTimedStep(prefix + "StepCommand", registry);
      this.stepCommandIsValid = new YoBoolean(prefix + "StepCommandIsValid", registry);
      this.taskSpaceEstimates = new QuadrupedTaskSpaceEstimates();
      this.parameters = parameters;
      // state machine
      FiniteStateMachineBuilder<FootState, FootEvent, FiniteStateMachineState<FootEvent>> stateMachineBuilder = new FiniteStateMachineBuilder<>(FootState.class, FootEvent.class,
            prefix + "FootState", registry);
      stateMachineBuilder.addState(FootState.SUPPORT, new SupportState(robotQuadrant));
      stateMachineBuilder.addState(FootState.SWING, new SwingState(robotQuadrant));
      stateMachineBuilder.addTransition(FootEvent.TIMEOUT, FootState.SUPPORT, FootState.SWING);
      stateMachineBuilder.addTransition(FootEvent.TIMEOUT, FootState.SWING, FootState.SUPPORT);
      footStateMachine = stateMachineBuilder.build(FootState.SUPPORT);
      stepTransitionCallback = null;

      parentRegistry.addChild(registry);
   }

   public void registerStepTransitionCallback(QuadrupedStepTransitionCallback stepTransitionCallback)
   {
      this.stepTransitionCallback = stepTransitionCallback;
   }

   public void attachStateChangedListener(FiniteStateMachineStateChangedListener stateChangedListener)
   {
      footStateMachine.attachStateChangedListener(stateChangedListener);
   }

   public void reset()
   {
      stepCommandIsValid.set(false);
      footStateMachine.reset();
   }

   public void triggerStep(QuadrupedTimedStep stepCommand)
   {
      if (footStateMachine.getCurrentStateEnum() == FootState.SUPPORT)
      {
         this.stepCommand.set(stepCommand);
         this.stepCommandIsValid.set(true);
      }
   }

   public void adjustStep(FramePoint3D newGoalPosition)
   {
      this.stepCommand.setGoalPosition(newGoalPosition);
   }

   public ContactState getContactState()
   {
      if (footStateMachine.getCurrentStateEnum() == FootState.SUPPORT)
         return ContactState.IN_CONTACT;
      else
         return ContactState.NO_CONTACT;
   }

   public void compute(FrameVector3D soleForceCommand, QuadrupedTaskSpaceEstimates taskSpaceEstimates)
   {
      // Update estimates.
      this.taskSpaceEstimates.set(taskSpaceEstimates);

      // Update foot state machine.
      footStateMachine.process();

      // Pack sole force command result.
      soleForceCommand.set(this.soleForceCommand);
   }

   private class SupportState implements FiniteStateMachineState<FootEvent>
   {
      private RobotQuadrant robotQuadrant;

      public SupportState(RobotQuadrant robotQuadrant)
      {
         this.robotQuadrant = robotQuadrant;
      }

      @Override
      public void onEntry()
      {
      }

      @Override
      public FootEvent process()
      {
         if (stepCommandIsValid.getBooleanValue())
         {
            double currentTime = timestamp.getDoubleValue();
            double liftOffTime = stepCommand.getTimeInterval().getStartTime();
            double touchDownTime = stepCommand.getTimeInterval().getEndTime();

            // trigger swing phase
            if (currentTime >= liftOffTime && currentTime < touchDownTime)
            {
               if (stepTransitionCallback != null)
               {
                  stepTransitionCallback.onLiftOff(robotQuadrant);
               }
               return FootEvent.TIMEOUT;
            }
         }

         return null;
      }

      @Override
      public void onExit()
      {
      }
   }

   private class SwingState implements FiniteStateMachineState<FootEvent>
   {
      private RobotQuadrant robotQuadrant;
      private final ThreeDoFSwingFootTrajectory swingTrajectory;
      private final FramePoint3D goalPosition;
      private final GlitchFilteredYoBoolean touchdownTrigger;

      public SwingState(RobotQuadrant robotQuadrant)
      {
         this.robotQuadrant = robotQuadrant;
         this.goalPosition = new FramePoint3D();
         this.swingTrajectory = new ThreeDoFSwingFootTrajectory(this.robotQuadrant.getPascalCaseName(), registry);
         this.touchdownTrigger = new GlitchFilteredYoBoolean(this.robotQuadrant.getCamelCaseName() + "TouchdownTriggered", registry,
               parameters.getTouchdownTriggerWindowParameter());
      }

      @Override
      public void onEntry()
      {
         // initialize swing trajectory
         double groundClearance = stepCommand.getGroundClearance();
         TimeInterval timeInterval = stepCommand.getTimeInterval();
         stepCommand.getGoalPosition(goalPosition);
         goalPosition.changeFrame(ReferenceFrame.getWorldFrame());
         goalPosition.add(0.0, 0.0, parameters.getStepGoalOffsetZParameter());
         FramePoint3D solePosition = taskSpaceEstimates.getSolePosition(robotQuadrant);
         solePosition.changeFrame(goalPosition.getReferenceFrame());
         swingTrajectory.initializeTrajectory(solePosition, goalPosition, groundClearance, timeInterval);

         // initialize contact state and feedback gains
         solePositionController.reset();
         solePositionController.getGains().setProportionalGains(parameters.getSolePositionProportionalGainsParameter());
         solePositionController.getGains().setDerivativeGains(parameters.getSolePositionDerivativeGainsParameter());
         solePositionController.getGains()
               .setIntegralGains(parameters.getSolePositionIntegralGainsParameter(), parameters.getSolePositionMaxIntegralErrorParameter());
         solePositionControllerSetpoints.initialize(taskSpaceEstimates);

         touchdownTrigger.set(false);
      }

      @Override
      public FootEvent process()
      {
         double currentTime = timestamp.getDoubleValue();
         double touchDownTime = stepCommand.getTimeInterval().getEndTime();

         // Compute current goal position.
         stepCommand.getGoalPosition(goalPosition);
         goalPosition.changeFrame(ReferenceFrame.getWorldFrame());
         goalPosition.add(0.0, 0.0, parameters.getStepGoalOffsetZParameter());

         // Compute swing trajectory.
         if (touchDownTime - currentTime > parameters.getMinimumStepAdjustmentTimeParameter())
         {
            swingTrajectory.adjustTrajectory(goalPosition, currentTime);
         }
         swingTrajectory.computeTrajectory(currentTime);
         swingTrajectory.getPosition(solePositionControllerSetpoints.getSolePosition());

         // Detect early touch-down.
         FrameVector3D soleForceEstimate = taskSpaceEstimates.getSoleVirtualForce(robotQuadrant);
         soleForceEstimate.changeFrame(ReferenceFrame.getWorldFrame());
         double pressureEstimate = -soleForceEstimate.getZ();
         double relativeTimeInSwing = currentTime - stepCommand.getTimeInterval().getStartTime();
         double normalizedTimeInSwing = relativeTimeInSwing / stepCommand.getTimeInterval().getDuration();
         if (normalizedTimeInSwing > 0.5)
         {
            touchdownTrigger.update(pressureEstimate > parameters.getTouchdownPressureLimitParameter());
         }

         // Compute sole force.
         if (touchdownTrigger.getBooleanValue())
         {
            double pressureLimit = parameters.getTouchdownPressureLimitParameter();
            soleForceCommand.changeFrame(ReferenceFrame.getWorldFrame());
            soleForceCommand.set(0, 0, -pressureLimit);
         }
         else
         {
            solePositionController.compute(soleForceCommand, solePositionControllerSetpoints, taskSpaceEstimates);
            soleForceCommand.changeFrame(ReferenceFrame.getWorldFrame());
         }

         // Trigger support phase.
         if (currentTime >= touchDownTime)
         {
            if (stepTransitionCallback != null)
            {
               stepTransitionCallback.onTouchDown(robotQuadrant);
            }
            return FootEvent.TIMEOUT;
         }
         else
            return null;
      }

      @Override
      public void onExit()
      {
         soleForceCommand.setToZero();
         stepCommandIsValid.set(false);
      }
   }
}
