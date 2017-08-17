package us.ihmc.robotics.controllers;

import us.ihmc.robotics.controllers.pidGains.GainCalculator;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoVariable;

public class YoPDGains implements PDGainsInterface
{
   protected final YoDouble kp;
   private final YoDouble zeta;
   protected final YoDouble kd;
   private final YoDouble maximumOutput;
   private final YoDouble maximumFeedback;
   private final YoDouble maximumFeedbackRate;
   private final YoDouble positionDeadband;

   public YoPDGains(String suffix, YoVariableRegistry registry)
   {
      kp = new YoDouble("kp" + suffix, registry);
      zeta = new YoDouble("zeta" + suffix, registry);
      kd = new YoDouble("kd" + suffix, registry);

      maximumOutput = new YoDouble("maximumOutput" + suffix, registry);
      maximumFeedback = new YoDouble("maximumFeedback" + suffix, registry);
      maximumFeedbackRate = new YoDouble("maximumFeedbackRate" + suffix, registry);

      positionDeadband = new YoDouble("positionDeadband" + suffix, registry);

      maximumOutput.set(Double.POSITIVE_INFINITY);
      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);
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

   public void setMaximumOutput(double maximumOutput)
   {
      this.maximumOutput.set(maximumOutput);
   }

   public void setMaximumFeedback(double maxFeedback)
   {
      this.maximumFeedback.set(maxFeedback);
   }

   public void setMaximumFeedbackRate(double maxFeedbackRate)
   {
      this.maximumFeedbackRate.set(maxFeedbackRate);
   }

   public void setMaximumFeedbackAndMaximumFeedbackRate(double maxFeedback, double maxFeedbackRate)
   {
      maximumFeedback.set(maxFeedback);
      maximumFeedbackRate.set(maxFeedbackRate);
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

   public double getMaximumOutput()
   {
      return maximumOutput.getDoubleValue();
   }

   @Override
   public double getMaximumFeedback()
   {
      return maximumFeedback.getDoubleValue();
   }

   @Override
   public double getMaximumFeedbackRate()
   {
      return maximumFeedbackRate.getDoubleValue();
   }

   public YoDouble getYoKp()
   {
      return kp;
   }

   public YoDouble getYoZeta()
   {
      return zeta;
   }

   public YoDouble getYoKd()
   {
      return kd;
   }

   public YoDouble getYoMaximumOutput()
   {
      return maximumOutput;
   }

   public YoDouble getYoMaximumFeedback()
   {
      return maximumFeedback;
   }

   public YoDouble getYoMaximumFeedbackRate()
   {
      return maximumFeedbackRate;
   }

   public YoDouble getPositionDeadband()
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

   public void set(YoPDGains other)
   {
      this.kp.set(other.kp.getDoubleValue());
      this.zeta.set(other.zeta.getDoubleValue());
      this.kd.set(other.kd.getDoubleValue());
      this.maximumOutput.set(other.maximumOutput.getDoubleValue());
      this.maximumFeedback.set(other.maximumFeedback.getDoubleValue());
      this.maximumFeedbackRate.set(other.maximumFeedbackRate.getDoubleValue());
      this.positionDeadband.set(other.positionDeadband.getDoubleValue());
   }

}