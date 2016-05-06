package us.ihmc.robotics.controllers;

import us.ihmc.robotics.dataStructures.listener.VariableChangedListener;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;

public class YoPDGains implements PDGainsInterface
{
   private final DoubleYoVariable kp;
   private final DoubleYoVariable zeta;
   private final DoubleYoVariable kd;
   private final DoubleYoVariable maximumAcceleration;
   private final DoubleYoVariable maximumJerk;
   private final DoubleYoVariable positionDeadband;

   public YoPDGains(String suffix, YoVariableRegistry registry)
   {
      kp = new DoubleYoVariable("kp" + suffix, registry);
      zeta = new DoubleYoVariable("zeta" + suffix, registry);
      kd = new DoubleYoVariable("kd" + suffix, registry);

      maximumAcceleration = new DoubleYoVariable("maximumAcceleration" + suffix, registry);
      maximumJerk = new DoubleYoVariable("maximumJerk" + suffix, registry);

      positionDeadband = new DoubleYoVariable("positionDeadband" + suffix, registry);

      maximumAcceleration.set(Double.POSITIVE_INFINITY);
      maximumJerk.set(Double.POSITIVE_INFINITY);
   }

   public void setPDGains(double kp, double zeta)
   {
      this.kp.set(kp);
      this.zeta.set(zeta);
   }

   public void setKp(double kp)
   {
      this.kp.set(kp);
   }

   public void setKd(double kd)
   {
      this.kd.set(kd);
   }

   public void setZeta(double zeta)
   {
      this.zeta.set(zeta);
   }

   public void setMaximumAcceleration(double maxAcceleration)
   {
      this.maximumAcceleration.set(maxAcceleration);
   }

   public void setMaximumJerk(double maxJerk)
   {
      this.maximumJerk.set(maxJerk);
   }

   public void setMaximumAccelerationAndMaximumJerk(double maxAcceleration, double maxJerk)
   {
      maximumAcceleration.set(maxAcceleration);
      maximumJerk.set(maxJerk);
   }

   public void setPositionDeadband(double deadband)
   {
      positionDeadband.set(deadband);
   }

   @Override
   public double getKp()
   {
      return kp.getDoubleValue();
   }

   public double getZeta()
   {
      return zeta.getDoubleValue();
   }

   @Override
   public double getKd()
   {
      return kd.getDoubleValue();
   }

   @Override
   public double getMaximumAcceleration()
   {
      return maximumAcceleration.getDoubleValue();
   }

   @Override
   public double getMaximumJerk()
   {
      return maximumJerk.getDoubleValue();
   }

   public DoubleYoVariable getYoKp()
   {
      return kp;
   }

   public DoubleYoVariable getYoZeta()
   {
      return zeta;
   }

   public DoubleYoVariable getYoKd()
   {
      return kd;
   }

   public DoubleYoVariable getYoMaximumAcceleration()
   {
      return maximumAcceleration;
   }

   public DoubleYoVariable getYoMaximumJerk()
   {
      return maximumJerk;
   }

   public DoubleYoVariable getPositionDeadband()
   {
      return positionDeadband;
   }

   public void createDerivativeGainUpdater(boolean updateNow)
   {
      VariableChangedListener kdUpdater = new VariableChangedListener()
      {
         public void variableChanged(YoVariable<?> v)
         {
            kd.set(GainCalculator.computeDerivativeGain(kp.getDoubleValue(), zeta.getDoubleValue()));
         }
      };
   
      kp.addVariableChangedListener(kdUpdater);
      zeta.addVariableChangedListener(kdUpdater);
      
      if (updateNow) kdUpdater.variableChanged(null);
   }

}