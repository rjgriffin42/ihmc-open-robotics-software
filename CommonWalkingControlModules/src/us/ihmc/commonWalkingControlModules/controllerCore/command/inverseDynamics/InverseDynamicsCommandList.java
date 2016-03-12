package us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommandType;

public class InverseDynamicsCommandList implements InverseDynamicsCommand<InverseDynamicsCommandList>
{
   private final List<InverseDynamicsCommand<?>> commandList = new ArrayList<>();

   public InverseDynamicsCommandList()
   {
   }

   public void addCommand(InverseDynamicsCommand<?> command)
   {
      commandList.add(command);
   }

   public void clear()
   {
      commandList.clear();
   }

   public InverseDynamicsCommand<?> getCommand(int commandIndex)
   {
      return commandList.get(commandIndex);
   }

   public int getNumberOfCommands()
   {
      return commandList.size();
   }

   @Override
   public void set(InverseDynamicsCommandList other)
   {
      clear();
      for (int i = 0; i < other.getNumberOfCommands(); i++)
         addCommand(other.getCommand(i));
   }

   @Override
   public ControllerCoreCommandType getCommandType()
   {
      return ControllerCoreCommandType.COMMAND_LIST;
   }
}
