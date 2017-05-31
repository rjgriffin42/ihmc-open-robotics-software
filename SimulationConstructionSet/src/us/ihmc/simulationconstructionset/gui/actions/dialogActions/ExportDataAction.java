package us.ihmc.simulationconstructionset.gui.actions.dialogActions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;

import us.ihmc.simulationconstructionset.gui.SCSAction;
import us.ihmc.simulationconstructionset.gui.dialogConstructors.ExportDataDialogConstructor;

public class ExportDataAction extends SCSAction
{
   private static final long serialVersionUID = -5481556236530603500L;

   private ExportDataDialogConstructor constructor;

   public ExportDataAction(ExportDataDialogConstructor constructor)
   {
      super("Export Data...",
              "icons/Export24.gif",
              KeyEvent.VK_E,
              "Export Data",
              "Export simulation data to a file."
      );

      this.constructor = constructor;
   }

   public void doAction()
   {
      constructor.constructDialog();
   }

   public void closeAndDispose()
   {
      if (constructor != null)
      {
         constructor.closeAndDispose();
         constructor = null;
      }
   }

   public void setCurrentDirectory(String directory)
   {
      constructor.setCurrentDirectory(directory);
   }

   public void setCurrentDirectory(File directory)
   {
      constructor.setCurrentDirectory(directory);
   }

}
