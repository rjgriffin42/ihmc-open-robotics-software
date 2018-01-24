package us.ihmc.robotics.parameterGui.tuning;

import us.ihmc.robotics.parameterGui.GuiParameter;

public class IntegerTuner extends NumericTuner<Integer>
{
   public IntegerTuner(GuiParameter parameter)
   {
      super(parameter);
   }

   @Override
   public NumericSpinner<Integer> createASpinner()
   {
      return new IntegerSpinner();
   }

   @Override
   public boolean areBoundsConsistent(Integer value, Integer min, Integer max)
   {
      return value >= min && value <= max;
   }

   @Override
   public Integer getSmallerNumber(Integer a, Integer b)
   {
      return Math.min(a, b);
   }

   @Override
   public Integer getLargerNumber(Integer a, Integer b)
   {
      return Math.max(a, b);
   }
}
