package us.ihmc.steppr.hardware.state;

import java.nio.ByteBuffer;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;

public class StepprPowerDistributionADCState
{
   private final YoVariableRegistry registry;
   private final IntegerYoVariable ADC[] = new IntegerYoVariable[8];

   private final DoubleYoVariable robotPower;
   private final DoubleYoVariable busVoltage;
   private final DoubleYoVariable leftLimbCurrent;
   private final DoubleYoVariable rightLimbCurrent;
   private final DoubleYoVariable torsoLimbCurrent;

   public StepprPowerDistributionADCState(String name, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      for (int i = 0; i < ADC.length; i++)
      {
         ADC[i] = new IntegerYoVariable("rawADC" + i, registry);
      }

      robotPower = new DoubleYoVariable("robotPower", registry);
      busVoltage = new DoubleYoVariable("busVoltage", registry);
      leftLimbCurrent = new DoubleYoVariable("leftLimbCurrent", registry);
      rightLimbCurrent = new DoubleYoVariable("rightLimbCurrent", registry);
      torsoLimbCurrent = new DoubleYoVariable("torsoLimbCurrent", registry);

      parentRegistry.addChild(registry);
   }

   public void update(ByteBuffer buffer)
   {
      for (int i = 0; i < ADC.length; i++)
      {
         ADC[i].set(buffer.getShort());
      }

      busVoltage.set(((double) (ADC[0].getIntegerValue() & 0xFFFF)) / 491.0 - 0.1);
      leftLimbCurrent.set((ADC[1].getValueAsDouble() + 16.0) * 0.0061);
      rightLimbCurrent.set((ADC[2].getValueAsDouble() + 14.0) * 0.0061);
      torsoLimbCurrent.set((ADC[3].getValueAsDouble() + 15.0) * 0.0061);

      robotPower.set(busVoltage.getDoubleValue() * (leftLimbCurrent.getDoubleValue() + rightLimbCurrent.getDoubleValue() + torsoLimbCurrent.getDoubleValue()));
   }
}
