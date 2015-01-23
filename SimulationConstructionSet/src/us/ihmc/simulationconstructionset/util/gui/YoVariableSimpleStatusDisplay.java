package us.ihmc.simulationconstructionset.util.gui;



import java.awt.Color;

import javax.swing.JLabel;

import us.ihmc.simulationconstructionset.NewDataListener;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;

public class YoVariableSimpleStatusDisplay extends JLabel implements NewDataListener
{
   private final String trueString = "ON";
   private final String falseString = "OFF";
   private final Color trueColor = Color.GREEN;
   private final Color falseColor = Color.RED;

   private BooleanYoVariable currentStateVariable;

   public YoVariableSimpleStatusDisplay(BooleanYoVariable currentStateVariable)
   {
      this.currentStateVariable = currentStateVariable;

      handleStateChange();
   }

   private void handleStateChange()
   {
      if (currentStateVariable.getBooleanValue())
      {
         setTrue();
      }
      else
      {
         setFalse();
      }
   }

   private void setTrue()
   {
      this.setForeground(trueColor);
      this.setText(trueString);
   }

   private void setFalse()
   {
      this.setForeground(falseColor);
      this.setText(falseString);
   }

   public NewDataListener getDataListener()
   {
      return this;
   }

   public void newDataHasBeenSent()
   {
   }

   public void newDataHasBeenReceived()
   {
      handleStateChange();
   }
}
