package us.ihmc.simulationconstructionset.gui.actions.dialogActions;

import us.ihmc.simulationconstructionset.gui.SCSAction;
import us.ihmc.simulationconstructionset.gui.dialogConstructors.ExportGraphsToFileConstructor;

import java.awt.event.KeyEvent;

public class ExportGraphsToFileAction extends SCSAction
{
   private ExportGraphsToFileConstructor constructor;

   public ExportGraphsToFileAction(ExportGraphsToFileConstructor constructor)
   {
      super("Export Graphs To File",
              "icons/exportGraph.png",
              KeyEvent.VK_UNDEFINED,
              "Export Graphs To File",
              "Export Graphs To File"
      );

      this.constructor = constructor;
   }

   public void closeAndDispose()
   {
      constructor.closeAndDispose();
      constructor = null;
   }

   @Override
   public void doAction()
   {
      constructor.constructDialog();
   }

}
