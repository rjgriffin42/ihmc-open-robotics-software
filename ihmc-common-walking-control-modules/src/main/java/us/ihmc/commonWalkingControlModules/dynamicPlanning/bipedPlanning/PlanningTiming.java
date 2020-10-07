package us.ihmc.commonWalkingControlModules.dynamicPlanning.bipedPlanning;

import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.tools.saveableModule.SaveableModuleState;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class PlanningTiming extends SaveableModuleState
{
   /** The nominal swing duration of a footstep as specified in the FootstepData */
   private final YoDouble swingTime;

   /** The nominal transfer duration of a footstep as specified in the FootstepData */
   private final YoDouble transferTime;

   public PlanningTiming(String suffix, YoRegistry registry)
   {
      swingTime = new YoDouble("swingTime" + suffix, registry);
      transferTime = new YoDouble("transferTime" + suffix, registry);
      registerDoubleToSave(swingTime);
      registerDoubleToSave(transferTime);

      clear();
   }

   public double getSwingTime()
   {
      return swingTime.getDoubleValue();
   }

   public double getTransferTime()
   {
      return transferTime.getDoubleValue();
   }

   public void clear()
   {
      swingTime.setToNaN();
      transferTime.setToNaN();
   }

   public void set(FootstepTiming timing)
   {
      setSwingTime(timing.getSwingTime());
      setTransferTime(timing.getTransferTime());
   }

   public void setSwingTime(double swingTime)
   {
      this.swingTime.set(swingTime);
   }

   public void setTransferTime(double transferTime)
   {
      this.transferTime.set(transferTime);
   }
}
