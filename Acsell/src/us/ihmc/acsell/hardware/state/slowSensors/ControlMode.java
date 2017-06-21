package us.ihmc.acsell.hardware.state.slowSensors;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.BooleanYoVariable;
import us.ihmc.yoVariables.variable.EnumYoVariable;

public class ControlMode implements AcsellSlowSensor
{
   enum ControlModeEnum
   {
      IDLE,
      IDLE_SOME_MORE,
      VOLTAGE,
      CURRENT;
      
      public static final ControlModeEnum[] values = values();
   }
   
   private final EnumYoVariable<ControlModeEnum> controlMode;
   private final BooleanYoVariable thermalShutdown;
   
   
   public ControlMode(String name, YoVariableRegistry registry)
   {
      controlMode = new EnumYoVariable<>(name + "ControlMode", registry, ControlModeEnum.class, true);
      thermalShutdown = new BooleanYoVariable(name + "ThermalShutdown", registry);
   }

   @Override
   public void update(int value)
   {
      int controlModeByte = value & 0xFF;
      int thermal = (value >>> 8); 
      
      if(controlModeByte > 0 && controlModeByte < ControlModeEnum.values.length)
      {
         controlMode.set(ControlModeEnum.values[controlModeByte]);
      }
      else
      {
         controlMode.set(null);
      }
      
      thermalShutdown.set(thermal != 0);
   }
   
}
