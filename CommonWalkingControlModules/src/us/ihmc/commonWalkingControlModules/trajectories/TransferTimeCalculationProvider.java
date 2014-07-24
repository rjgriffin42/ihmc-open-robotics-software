package us.ihmc.commonWalkingControlModules.trajectories;

import us.ihmc.utilities.math.trajectories.providers.DoubleProvider;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class TransferTimeCalculationProvider implements DoubleProvider
{
   private final ConstantTransferTimeCalculator transferTimeCalculator;
   private final DoubleYoVariable transferTime;

   public TransferTimeCalculationProvider(String name, YoVariableRegistry registry, ConstantTransferTimeCalculator transferTimeCalculator, double defaultTransferTime)
   {
      this.transferTime = new DoubleYoVariable(name, registry);
      this.transferTimeCalculator = transferTimeCalculator;
      this.transferTime.set(defaultTransferTime);
   }
   
   public double getValue()
   {
      return transferTime.getDoubleValue();
   }
   
   public void updateTransferTime()
   {
      transferTime.set(transferTimeCalculator.getTransferTime());
   }

}
