package us.ihmc.valkyrieRosControl.dataHolders;

import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoVariable;
import us.ihmc.robotics.sensors.IMUDefinition;
import us.ihmc.valkyrie.imu.MicroStrainData.MicrostrainFilterType;

public class YoMicroStrainIMUHandleHolder extends YoIMUHandleHolder
{
   private final MicroStrainIMUHandle microStrainIMUHandle;

   private final YoEnum<MicrostrainFilterType> filterTypeToUse;

   public static YoMicroStrainIMUHandleHolder create(int sensorId, IMUDefinition imuDefinition, YoVariableRegistry parentRegistry)
   {
      return new YoMicroStrainIMUHandleHolder(new MicroStrainIMUHandle(imuDefinition.getName(), sensorId), imuDefinition, parentRegistry);
   }

   private YoMicroStrainIMUHandleHolder(MicroStrainIMUHandle handle, IMUDefinition imuDefinition, YoVariableRegistry parentRegistry)
   {
      super(handle, imuDefinition, parentRegistry);
      this.microStrainIMUHandle = handle;

      filterTypeToUse = new YoEnum<>(handle.getName() + "_filterTypeToUse", parentRegistry, MicrostrainFilterType.class);

      filterTypeToUse.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void notifyOfVariableChanged(YoVariable<?> v)
         {
            microStrainIMUHandle.setFilterTypeToReturn(filterTypeToUse.getEnumValue());
         }
      });
      filterTypeToUse.set(MicrostrainFilterType.COMPLIMENTARY_FILTER);

   }

   @Override
   public void update()
   {
      microStrainIMUHandle.update();
      super.update();
   }

}
