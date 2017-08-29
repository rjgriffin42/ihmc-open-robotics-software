package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.newHighLevelStates;

import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreOutputReadOnly;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelState;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.NewHighLevelStates;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.FinishableState;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public abstract class NewHighLevelControllerState extends FinishableState<NewHighLevelStates>
{

   public NewHighLevelControllerState(NewHighLevelStates stateEnum)
   {
      super(stateEnum);
   }

   @Override
   public abstract void doAction();

   @Override
   public abstract void doTransitionIntoAction();

   @Override
   public abstract void doTransitionOutOfAction();

   public abstract YoVariableRegistry getYoVariableRegistry();

   public abstract void setControllerCoreOutput(ControllerCoreOutputReadOnly controllerCoreOutput);

   public abstract ControllerCoreCommand getControllerCoreCommand();

   @Override
   public boolean isDone()
   {
      return true;
   }
}
