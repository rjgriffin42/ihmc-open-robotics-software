package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.ControllerCoreOuput;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelState;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.stateMachines.State;

public abstract class HighLevelBehavior extends State<HighLevelState>
{

   public HighLevelBehavior(HighLevelState stateEnum)
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

   public abstract void setControllerCoreOuput(ControllerCoreOuput controllerCoreOuput);

   public abstract ControllerCoreCommand getControllerCoreCommand();
}
