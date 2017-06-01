package us.ihmc.simulationconstructionset.gui.actions.dialogActions;

import us.ihmc.simulationconstructionset.gui.SCSAction;
import us.ihmc.simulationconstructionset.gui.dialogConstructors.DataBufferPropertiesDialogConstructor;
import java.awt.event.KeyEvent;

@SuppressWarnings("serial")
public class DataBufferPropertiesAction extends SCSAction
{
   private DataBufferPropertiesDialogConstructor constructor;

   public DataBufferPropertiesAction(DataBufferPropertiesDialogConstructor constructor)
   {
      super("Data Buffer Properties...",
              "",
              KeyEvent.VK_B,
              "Short Description", // TODO
              "Long Description" // TODO
      );

      this.constructor = constructor;
   }

   @Override
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
}
