package us.ihmc.steppr.hardware.state.slowSensors;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class ControllerTemperature1 implements StepprSlowSensor
{
   private final DoubleYoVariable mcbTemperature1;
   private final double THERM_B = 3730;
   private final double THERM_R_25C = 22000;
   private final double SERIES_RESISTANCE = 100.0e3;
   private final double V_S = 12.0;
   private final double MAX_ADC_COUNT = 4096.0;
   private final double MAX_ADC_VOLTAGE = 3.3;
   
   public ControllerTemperature1(String name, YoVariableRegistry parentRegistry)
   {
      mcbTemperature1 = new DoubleYoVariable(name + "MCBTemperature1", parentRegistry);
   }

   @Override
   public void update(int value)
   {
	  double motor_temp_c = 0;
	  if (value > 0)
	  {
		  double therm_r_meas = SERIES_RESISTANCE * value / (MAX_ADC_COUNT * V_S / MAX_ADC_VOLTAGE - value);
    	  double therm_k = THERM_R_25C*Math.exp(-THERM_B/298.15);
          motor_temp_c = THERM_B / Math.log(therm_r_meas / therm_k) - 273.15;
      }
      mcbTemperature1.set(motor_temp_c);
   }
   
}
