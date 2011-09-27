package us.ihmc.util.parameterOptimization;

import com.yobotics.simulationconstructionset.DoubleYoVariable;

public class DoubleYoVariableParameterToOptimize extends DoubleParameterToOptimize
{
   private final DoubleYoVariable yoVariable;
   
   public DoubleYoVariableParameterToOptimize(double min, double max, DoubleYoVariable yoVariable, ListOfParametersToOptimize listOfParametersToOptimize)
   {
      super(yoVariable.getName(), min, max, listOfParametersToOptimize);
      this.yoVariable = yoVariable;
   }
   
   public void setCurrentValueGivenZeroToOne(double zeroToOne)
   {
      super.setCurrentValueGivenZeroToOne(zeroToOne);
      yoVariable.set(this.getCurrentValue());
   }
   
   public void setCurrentValue(double newValue)
   {
      super.setCurrentValue(newValue);
      yoVariable.set(newValue);
   }

}
