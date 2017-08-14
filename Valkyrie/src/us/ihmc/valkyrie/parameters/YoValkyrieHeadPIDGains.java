package us.ihmc.valkyrie.parameters;

import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.pidGains.GainCalculator;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoVariable;

public class YoValkyrieHeadPIDGains implements YoOrientationPIDGainsInterface
{
   private final YoDouble proportionalXGain, proportionalYZGain;
   private final YoDouble derivativeXGain, derivativeYZGain;
   private final YoDouble dampingRatioX, dampingRatioYZ;

   private final YoDouble maxDerivativeError;
   private final YoDouble maxProportionalError;

   private final YoDouble maximumFeedback;
   private final YoDouble maximumFeedbackRate;

   public YoValkyrieHeadPIDGains(String suffix, YoVariableRegistry registry)
   {
      proportionalXGain = new YoDouble("kpXAngular" + suffix, registry);
      proportionalYZGain = new YoDouble("kpYZAngular" + suffix, registry);
      derivativeXGain = new YoDouble("kdXAngular" + suffix, registry);
      derivativeYZGain = new YoDouble("kdYZAngular" + suffix, registry);
      dampingRatioX = new YoDouble("zetaXAngular" + suffix, registry);
      dampingRatioYZ = new YoDouble("zetaYZAngular" + suffix, registry);

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
      proportionalXGain.set(0.0);
      proportionalYZGain.set(0.0);
      derivativeXGain.set(0.0);
      derivativeYZGain.set(0.0);
      dampingRatioX.set(0.0);
      dampingRatioYZ.set(0.0);
      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);
      maxDerivativeError.set(Double.POSITIVE_INFINITY);
      maxProportionalError.set(Double.POSITIVE_INFINITY);
   }

   public void createDerivativeGainUpdater(boolean updateNow)
   {
      VariableChangedListener kdXUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeXGain.set(GainCalculator.computeDerivativeGain(proportionalXGain.getDoubleValue(), dampingRatioX.getDoubleValue()));
         }
      };

      proportionalXGain.addVariableChangedListener(kdXUpdater);
      dampingRatioX.addVariableChangedListener(kdXUpdater);

      if (updateNow)
         kdXUpdater.variableChanged(null);

      VariableChangedListener kdYZUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeYZGain.set(GainCalculator.computeDerivativeGain(proportionalYZGain.getDoubleValue(), dampingRatioYZ.getDoubleValue()));
         }
      };

      proportionalYZGain.addVariableChangedListener(kdYZUpdater);
      dampingRatioYZ.addVariableChangedListener(kdYZUpdater);

      if (updateNow)
         kdYZUpdater.variableChanged(null);
   }

   @Override
   public void setProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      proportionalXGain.set(proportionalGainX);
      proportionalYZGain.set(proportionalGainY);
   }

   public void setProportionalGains(double proportionalGainX, double proportionalGainYZ)
   {
      proportionalXGain.set(proportionalGainX);
      proportionalYZGain.set(proportionalGainYZ);
   }

   @Override
   public void setDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      derivativeXGain.set(derivativeGainX);
      derivativeYZGain.set(derivativeGainY);
   }

   public void setDerivativeGains(double derivativeGainX, double derivativeGainYZ)
   {
      derivativeXGain.set(derivativeGainX);
      derivativeYZGain.set(derivativeGainYZ);
   }

   public void setDampingRatio(double dampingRatio)
   {
      dampingRatioX.set(dampingRatio);
      dampingRatioYZ.set(dampingRatio);
   }

   public void setDampingRatios(double dampingRatioX, double dampingRatioYZ)
   {
      this.dampingRatioX.set(dampingRatioX);
      this.dampingRatioYZ.set(dampingRatioYZ);
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
      tempPropotionalGains[0] = proportionalXGain.getDoubleValue();
      tempPropotionalGains[1] = proportionalYZGain.getDoubleValue();
      tempPropotionalGains[2] = proportionalYZGain.getDoubleValue();

      return tempPropotionalGains;
   }

   private double[] tempDerivativeGains = new double[3];

   @Override
   public double[] getDerivativeGains()
   {
      tempDerivativeGains[0] = derivativeXGain.getDoubleValue();
      tempDerivativeGains[1] = derivativeYZGain.getDoubleValue();
      tempDerivativeGains[2] = derivativeYZGain.getDoubleValue();

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
