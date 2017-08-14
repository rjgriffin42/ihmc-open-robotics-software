package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.robotics.controllers.pidGains.GainCalculator;
import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoVariable;

public class YoPlanarFootOrientationGains implements YoPID3DGains
{
   private final YoDouble proportionalYGain;
   private final YoDouble derivativeYGain;
   private final YoDouble dampingRatioY;

   private final YoDouble maxDerivativeError;
   private final YoDouble maxProportionalError;

   private final YoDouble maximumFeedback;
   private final YoDouble maximumFeedbackRate;

   public YoPlanarFootOrientationGains(String suffix, YoVariableRegistry registry)
   {
      proportionalYGain = new YoDouble("kpYAngular" + suffix, registry);
      derivativeYGain = new YoDouble("kdYAngular" + suffix, registry);
      dampingRatioY = new YoDouble("zetaYAngular" + suffix, registry);

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
      proportionalYGain.set(0.0);
      derivativeYGain.set(0.0);
      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);
      maxDerivativeError.set(Double.POSITIVE_INFINITY);
      maxProportionalError.set(Double.POSITIVE_INFINITY);
   }

   public void createDerivativeGainUpdater(boolean updateNow)
   {
      VariableChangedListener kdYUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeYGain.set(GainCalculator.computeDerivativeGain(proportionalYGain.getDoubleValue(), dampingRatioY.getDoubleValue()));
         }
      };

      proportionalYGain.addVariableChangedListener(kdYUpdater);
      dampingRatioY.addVariableChangedListener(kdYUpdater);

      if (updateNow)
         kdYUpdater.variableChanged(null);
   }

   @Override
   public void setProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      proportionalYGain.set(proportionalGainY);
   }

   public void setProportionalGains(double proportionalGainY)
   {
      setProportionalGains(0.0, proportionalGainY, 0.0);
   }

   @Override
   public void setDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      derivativeYGain.set(derivativeGainY);
   }

   public void setDerivativeGains(double derivativeGainY)
   {
      setDerivativeGains(0.0, derivativeGainY, 0.0);
   }

   public void setDampingRatio(double dampingRatio)
   {
      dampingRatioY.set(dampingRatio);
   }

   @Override
   public void setIntegralGains(double integralGainX, double integralGainY, double integralGainZ, double maxIntegralError)
   {
   }

   @Override
   public void setProportionalGains(double[] proportionalGains)
   {
      setProportionalGains(proportionalGains[0], 0.0, 0.0);
   }

   @Override
   public void setDerivativeGains(double[] derivativeGains)
   {
      setDerivativeGains(derivativeGains[0], 0.0, 0.0);
   }

   @Override
   public void setIntegralGains(double[] integralGains, double maxIntegralError)
   {
      setIntegralGains(integralGains[0], 0.0, 0.0, maxIntegralError);
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

   private double[] tempPropotionalGains = new double[1];

   @Override
   public double[] getProportionalGains()
   {
      tempPropotionalGains[0] = proportionalYGain.getDoubleValue();

      return tempPropotionalGains;
   }

   private double[] tempDerivativeGains = new double[1];

   @Override
   public double[] getDerivativeGains()
   {
      tempDerivativeGains[0] = derivativeYGain.getDoubleValue();

      return tempDerivativeGains;
   }

   private double[] tempIntegralGains = new double[1];

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
