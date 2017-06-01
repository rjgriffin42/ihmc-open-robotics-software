package us.ihmc.simulationconstructionset.gui.actions;

import us.ihmc.simulationconstructionset.commands.CreateNewViewportWindowCommandExecutor;
import us.ihmc.simulationconstructionset.gui.SCSAction;

import java.awt.event.KeyEvent;

public class CreateNewViewportWindowAction extends SCSAction
{
   private CreateNewViewportWindowCommandExecutor executor;

   public CreateNewViewportWindowAction(CreateNewViewportWindowCommandExecutor executor)
   {
      super("New Viewport Window",
              "",
              KeyEvent.VK_V,
              "Creates a new Viewport Window.",
              "Creates a new Viewport Window for showing 3D Graphics in SCS."
      );

      this.executor = executor;
   }

   @Override
   public void doAction()
   {
      executor.createNewViewportWindow();
   }
}
