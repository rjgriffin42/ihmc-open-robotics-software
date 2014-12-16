package us.ihmc.steppr.hardware.state.slowSensors;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;

public class RawPhaseCurrentADTicks implements StepprSlowSensor
{
   private final IntegerYoVariable rawPhaseCurrentADTicks;
   private final DoubleYoVariable phaseCurrent;

   public RawPhaseCurrentADTicks(String name, String phase, YoVariableRegistry registry)
   {
      rawPhaseCurrentADTicks = new IntegerYoVariable(name + "RawPhase" + phase + "CurrentADTicks", registry);
      phaseCurrent = new DoubleYoVariable(name + "Phase" + phase + "Current", registry);
   }

   @Override
   public void update(int value)
   {
      rawPhaseCurrentADTicks.set(value);
      phaseCurrent.set(((double) value) * -3.3 / 4096.0 / 40.0 / 0.0015);
   }

}
