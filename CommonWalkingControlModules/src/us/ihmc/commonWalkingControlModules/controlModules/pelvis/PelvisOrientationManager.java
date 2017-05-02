package us.ihmc.commonWalkingControlModules.controlModules.pelvis;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisOrientationTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.StopAllTrajectoryCommand;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.GenericStateMachine;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.StateMachineTools;

public class PelvisOrientationManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final GenericStateMachine<PelvisOrientationControlMode, PelvisOrientationControlState> stateMachine;
   private final EnumYoVariable<PelvisOrientationControlMode> requestedState;

   private final ControllerPelvisOrientationManager walkingManager;
   private final UserPelvisOrientationManager userManager;

   private final FrameOrientation tempOrientation = new FrameOrientation();

   public PelvisOrientationManager(YoOrientationPIDGainsInterface gains, HighLevelHumanoidControllerToolbox controllerToolbox,
                                   YoVariableRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
      DoubleYoVariable yoTime = controllerToolbox.getYoTime();
      String namePrefix = getClass().getSimpleName();
      stateMachine = new GenericStateMachine<>(namePrefix + "State", namePrefix + "SwitchTime", PelvisOrientationControlMode.class, yoTime, registry);
      requestedState = new EnumYoVariable<>(namePrefix + "RequestedControlMode", registry, PelvisOrientationControlMode.class, true);

      walkingManager = new ControllerPelvisOrientationManager(gains, controllerToolbox, registry);
      userManager = new UserPelvisOrientationManager(gains, controllerToolbox, registry);
      setupStateMachine();
   }

   private void setupStateMachine()
   {
      List<PelvisOrientationControlState> states = new ArrayList<>();
      states.add(walkingManager);
      states.add(userManager);

      for (PelvisOrientationControlState fromState : states)
      {
         for (PelvisOrientationControlState toState : states)
         {
            StateMachineTools.addRequestedStateTransition(requestedState, false, fromState, toState);
         }
         stateMachine.addState(fromState);
      }
   }

   public void setWeights(Vector3D weight)
   {
      walkingManager.setWeights(weight);
      userManager.setWeights(weight);
   }

   public void compute()
   {
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();
   }

   public void initialize()
   {
      requestState(walkingManager.getStateEnum());
      walkingManager.setToHoldCurrentInWorldFrame();

//      tempOrientation.setToZero(userManager.getControlFrame());
//      userManager.goHome(1.0, tempOrientation);
//      requestState(userManager.getStateEnum());
   }

   public void handleStopAllTrajectoryCommand(StopAllTrajectoryCommand command)
   {
      updateOffsetInWalkingManager();
      requestState(walkingManager.getStateEnum());
   }

   public void prepareForLocomotion()
   {
      updateOffsetInWalkingManager();
      requestState(walkingManager.getStateEnum());
   }

   private void updateOffsetInWalkingManager()
   {
      if (stateMachine.getCurrentStateEnum() == walkingManager.getStateEnum())
      {
         return;
      }

      userManager.getCurrentDesiredOrientation(tempOrientation);
      walkingManager.setOffset(tempOrientation);
   }

   public OrientationFeedbackControlCommand getFeedbackControlCommand()
   {
      return stateMachine.getCurrentState().getFeedbackControlCommand();
   }

   public void goToHomeFromCurrentDesired(double trajectoryTime)
   {
      walkingManager.goToHomeFromCurrentDesired(trajectoryTime);
   }

   public void setTrajectoryTime(double transferTime)
   {
      walkingManager.setTrajectoryTime(transferTime);
   }

   public void moveToAverageInSupportFoot(RobotSide supportSide)
   {
      walkingManager.moveToAverageInSupportFoot(supportSide);
   }

   public void resetOrientationOffset()
   {
      walkingManager.resetOrientationOffset();
   }

   public void setToHoldCurrentDesiredInMidFeetZUpFrame()
   {
      walkingManager.setToHoldCurrentDesiredInMidFeetZUpFrame();
   }

   public void centerInMidFeetZUpFrame(double trajectoryTime)
   {
      walkingManager.centerInMidFeetZUpFrame(trajectoryTime);
   }

   public void setToHoldCurrentDesiredInSupportFoot(RobotSide supportSide)
   {
      walkingManager.setToHoldCurrentDesiredInSupportFoot(supportSide);
   }

   public void setToHoldCurrentInWorldFrame()
   {
      walkingManager.setToHoldCurrentInWorldFrame();
   }

   public void setToZeroInMidFeetZUpFrame()
   {
      walkingManager.setToZeroInMidFeetZUpFrame();
   }

   public void setWithUpcomingFootstep(Footstep upcomingFootstep)
   {
      walkingManager.setWithUpcomingFootstep(upcomingFootstep);
   }

   public void handlePelvisOrientationTrajectoryCommands(PelvisOrientationTrajectoryCommand command)
   {
      stateMachine.getCurrentState().getCurrentDesiredOrientation(tempOrientation);
      userManager.handlePelvisOrientationTrajectoryCommands(command, tempOrientation);
      requestState(userManager.getStateEnum());
   }

   private final PelvisOrientationTrajectoryCommand tempPelvisOrientationTrajectoryCommand = new PelvisOrientationTrajectoryCommand();
   public void handlePelvisTrajectoryCommand(PelvisTrajectoryCommand command)
   {
      tempPelvisOrientationTrajectoryCommand.set(command);
      handlePelvisOrientationTrajectoryCommands(tempPelvisOrientationTrajectoryCommand);
   }

   private void requestState(PelvisOrientationControlMode state)
   {
      if (stateMachine.getCurrentStateEnum() != state)
      {
         requestedState.set(state);
      }
   }

   public FeedbackControlCommandList createFeedbackControlTemplate()
   {
      FeedbackControlCommandList ret = new FeedbackControlCommandList();
      for (PelvisOrientationControlMode mode : PelvisOrientationControlMode.values())
      {
         PelvisOrientationControlState state = stateMachine.getState(mode);
         if (state != null && state.getFeedbackControlCommand() != null)
            ret.addCommand(state.getFeedbackControlCommand());
      }
      return ret;
   }
}
