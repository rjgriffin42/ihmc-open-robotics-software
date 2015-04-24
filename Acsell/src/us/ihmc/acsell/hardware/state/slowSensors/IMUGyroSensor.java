package us.ihmc.acsell.hardware.state.slowSensors;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class IMUGyroSensor implements AcsellSlowSensor
{
   private final DoubleYoVariable imuGyro;
   
   public IMUGyroSensor(String name, String axis, YoVariableRegistry registry)
   {
      imuGyro = new DoubleYoVariable(name + "IMUGyro" + axis, registry);
   }

   @Override
   public void update(int value)
   {
      imuGyro.set(((short)value));
   }

}
