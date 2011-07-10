package us.ihmc.sensorProcessing.encoder;


import com.yobotics.simulationconstructionset.IntegerYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.SensorProcessor;

public class EncoderResolutionReducer implements SensorProcessor
{
   private final IntegerYoVariable rawTicksIn;
   private final IntegerYoVariable rawTicksOut;
   private final int reductionFactor;
   
   public EncoderResolutionReducer(String name, IntegerYoVariable rawTicksIn, int reductionFactor, YoVariableRegistry registry)
   {
      this.rawTicksIn = rawTicksIn;
      this.rawTicksOut = new IntegerYoVariable(name + "rawTicksOut", registry);
      this.reductionFactor = reductionFactor;
   }

   public void initialize()
   {
      update();
   }

   public void update()
   {
      rawTicksOut.set(rawTicksIn.getIntegerValue() / reductionFactor);
   }
   
   public IntegerYoVariable getRawTicksOut()
   {
      return rawTicksOut;
   }
}
