package us.ihmc.robotDataCommunication;

import us.ihmc.yoUtilities.YoVariable;

public class VariableChangedMessage
{
   private YoVariable variable;
   private double val = -1;

   public VariableChangedMessage()
   {
   }

   public YoVariable getVariable()
   {
      return variable;
   }

   public void setVariable(YoVariable variable)
   {
      this.variable = variable;
   }

   public void setVal(double val)
   {
      this.val = val;
   }

   public double getVal()
   {
      return val;
   }

   public static class Builder implements us.ihmc.concurrent.Builder<VariableChangedMessage>
   {

      public VariableChangedMessage newInstance()
      {
         return new VariableChangedMessage();
      }

   }

}