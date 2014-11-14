package us.ihmc.simulationconstructionset.gui.dialogConstructors;

import java.awt.Container;

import javax.swing.JFrame;

import us.ihmc.simulationconstructionset.DataBuffer;
import us.ihmc.simulationconstructionset.gui.DataBufferChangeListener;
import us.ihmc.simulationconstructionset.gui.dialogs.DataBufferPropertiesDialog;

public class DataBufferPropertiesDialogGenerator implements DataBufferPropertiesDialogConstructor
{
   private DataBuffer dataBuffer;
   private Container parentContainer;
   private JFrame frame;
   private DataBufferChangeListener listener;

   public DataBufferPropertiesDialogGenerator(DataBuffer dataBuffer, Container parentContainer, JFrame frame, DataBufferChangeListener listener)
   {
      this.dataBuffer = dataBuffer;
      this.parentContainer = parentContainer;
      this.frame = frame;
      this.listener = listener;
   }

   public void constructDataBufferPropertiesDialog()
   {
      new DataBufferPropertiesDialog(parentContainer, frame, dataBuffer, listener);
   }

   public void closeAndDispose()
   {
      dataBuffer = null;
      parentContainer = null;
      frame = null;
      listener = null;
   }
}
