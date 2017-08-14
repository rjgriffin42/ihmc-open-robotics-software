package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.robotics.controllers.pidGains.GainCalculator;
import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoVariable;

public class YoFootOrientationGains implements YoPID3DGains
{
   private final YoDouble proportionalXYGain, proportionalZGain;
   private final YoDouble derivativeXYGain, derivativeZGain;
   private final YoDouble dampingRatioXY, dampingRatioZ;

   private final YoDouble maxDerivativeError;
   private final YoDouble maxProportionalError;

   private final YoDouble maximumFeedback;
   private final YoDouble maximumFeedbackRate;

   public YoFootOrientationGains(String suffix, YoVariableRegistry registry)
   {
      proportionalXYGain = new YoDouble("kpXYAngular" + suffix, registry);
      proportionalZGain = new YoDouble("kpZAngular" + suffix, registry);
      derivativeXYGain = new YoDouble("kdXYAngular" + suffix, registry);
      derivativeZGain = new YoDouble("kdZAngular" + suffix, registry);
      dampingRatioXY = new YoDouble("zetaXYAngular" + suffix, registry);
      dampingRatioZ = new YoDouble("zetaZAngular" + suffix, registry);

      maximumFeedback = new YoDouble("maximumAngularFeedback" + suffix, registry);
      maximumFeedbackRate = new YoDouble("maximumAngularFeedbackRate" + suffix, registry);

      maxDerivativeError = new YoDouble("maximumAngularDerivativeError" + suffix, registry);
      maxProportionalError = new YoDouble("maximumAngularProportionalError" + suffix, registry);

      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);

      maxDerivativeError.set(Double.POSITIVE_INFINITY);
      maxProportionalError.set(Double.POSITIVE_INFINITY);
   }

   public void reset()
   {
      proportionalXYGain.set(0.0);
      proportionalZGain.set(0.0);
      derivativeXYGain.set(0.0);
      derivativeZGain.set(0.0);
      dampingRatioXY.set(0.0);
      dampingRatioZ.set(0.0);
      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);
      maxDerivativeError.set(Double.POSITIVE_INFINITY);
      maxProportionalError.set(Double.POSITIVE_INFINITY);
   }

   public void createDerivativeGainUpdater(boolean updateNow)
   {
      VariableChangedListener kdXYUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeXYGain.set(GainCalculator.computeDerivativeGain(proportionalXYGain.getDoubleValue(), dampingRatioXY.getDoubleValue()));
         }
      };

      proportionalXYGain.addVariableChangedListener(kdXYUpdater);
      dampingRatioXY.addVariableChangedListener(kdXYUpdater);

      if (updateNow)
         kdXYUpdater.variableChanged(null);

      VariableChangedListener kdZUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeZGain.set(GainCalculator.computeDerivativeGain(proportionalZGain.getDoubleValue(), dampingRatioZ.getDoubleValue()));
         }
      };

      proportionalZGain.addVariableChangedListener(kdZUpdater);
      dampingRatioZ.addVariableChangedListener(kdZUpdater);

      if (updateNow)
         kdZUpdater.variableChanged(null);
   }

   @Override
   public void setProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      proportionalXYGain.set(proportionalGainX);
      proportionalZGain.set(proportionalGainZ);
   }

   public void setProportionalGains(double proportionalGainXY, double proportionalGainZ)
   {
      proportionalXYGain.set(proportionalGainXY);
      proportionalZGain.set(proportionalGainZ);
   }

   @Override
   public void setDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      derivativeXYGain.set(derivativeGainX);
      derivativeZGain.set(derivativeGainZ);
   }

   public void setDerivativeGains(double derivativeGainXY, double derivativeGainZ)
   {
      derivativeXYGain.set(derivativeGainXY);
      derivativeZGain.set(derivativeGainZ);
   }

   public void setDampingRatio(double dampingRatio)
   {
      dampingRatioXY.set(dampingRatio);
      dampingRatioZ.set(dampingRatio);
   }

   public void setDampingRatios(double dampingRatioXY, double dampingRatioZ)
   {
      this.dampingRatioXY.set(dampingRatioXY);
      this.dampingRatioZ.set(dampingRatioZ);
   }

   @Override
   public void setIntegralGains(double integralGainX, double integralGainY, double integralGainZ, double maxIntegralError)
   {
   }

   @Override
   public void setProportionalGains(double[] proportionalGains)
   {
      setProportionalGains(proportionalGains[0], proportionalGains[1], proportionalGains[2]);
   }

   @Override
   public void setDerivativeGains(double[] derivativeGains)
   {
      setDerivativeGains(derivativeGains[0], derivativeGains[1], derivativeGains[2]);
   }

   @Override
   public void setIntegralGains(double[] integralGains, double maxIntegralError)
   {
   }

   public void setMaximumFeedback(double maxFeedback)
   {
      maximumFeedback.set(maxFeedback);
   }

   public void setMaximumFeedbackRate(double maxFeedbackRate)
   {
      maximumFeedbackRate.set(maxFeedbackRate);
   }

   @Override
   public void setMaxFeedbackAndFeedbackRate(double maxFeedback, double maxFeedbackRate)
   {
      maximumFeedback.set(maxFeedback);
      maximumFeedbackRate.set(maxFeedbackRate);
   }

   @Override
   public void setMaxDerivativeError(double maxDerivativeError)
   {
      this.maxDerivativeError.set(maxDerivativeError);
   }

   @Override
   public void setMaxProportionalError(double maxProportionalError)
   {
      this.maxProportionalError.set(maxProportionalError);
   }

   @Override
   public YoDouble getYoMaximumFeedback()
   {
      return maximumFeedback;
   }

   @Override
   public YoDouble getYoMaximumFeedbackRate()
   {
      return maximumFeedbackRate;
   }

   @Override
   public YoDouble getYoMaximumDerivativeError()
   {
      return maxDerivativeError;
   }

   @Override
   public YoDouble getYoMaximumProportionalError()
   {
      return maxProportionalError;
   }

   private double[] tempPropotionalGains = new double[3];

   @Override
   public double[] getProportionalGains()
   {
      tempPropotionalGains[0] = proportionalXYGain.getDoubleValue();
      tempPropotionalGains[1] = proportionalXYGain.getDoubleValue();
      tempPropotionalGains[2] = proportionalZGain.getDoubleValue();

      return tempPropotionalGains;
   }

   private double[] tempDerivativeGains = new double[3];

   @Override
   public double[] getDerivativeGains()
   {
      tempDerivativeGains[0] = derivativeXYGain.getDoubleValue();
      tempDerivativeGains[1] = derivativeXYGain.getDoubleValue();
      tempDerivativeGains[2] = derivativeZGain.getDoubleValue();

      return tempDerivativeGains;
   }

   private double[] tempIntegralGains = new double[3];

   @Override
   public double[] getIntegralGains()
   {
      return tempIntegralGains;
   }

   @Override
   public double getMaximumIntegralError()
   {
      return 0.0;
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

   @Override
   public double getMaximumDerivativeError()
   {
      return maxDerivativeError.getDoubleValue();
   }

   @Override
   public double getMaximumProportionalError()
   {
      return maxProportionalError.getDoubleValue();
   }

}
