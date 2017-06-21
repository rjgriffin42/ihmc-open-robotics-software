package us.ihmc.sensorProcessing.encoder.processors;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.IntegerYoVariable;


public class JerryEncoderProcessorNoYoVariablesWrapper extends JerryEncoderProcessorNoYoVariables implements EncoderProcessor
{
   private final IntegerYoVariable rawTicks;
   private final YoDouble time;
   
   public JerryEncoderProcessorNoYoVariablesWrapper(String name, IntegerYoVariable rawTicks, YoDouble time, double distancePerTick, double dt)
   {
      super(dt, distancePerTick);
      
      this.rawTicks = rawTicks;
      this.time = time;
   }
   
   public void update()
   {      
      super.update(rawTicks.getIntegerValue(), time.getDoubleValue());
   }

   public void initialize()
   {
      this.update();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return null;
   }

   public String getName()
   {
      return "JerryEncoderProcessorNoYoVariables";
   }

   public String getDescription()
   {
      return getName();
   }

}

