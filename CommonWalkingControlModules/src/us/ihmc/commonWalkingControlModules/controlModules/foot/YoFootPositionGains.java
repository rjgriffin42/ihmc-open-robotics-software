package us.ihmc.commonWalkingControlModules.controlModules.foot;

import javax.vecmath.Matrix3d;

import us.ihmc.robotics.controllers.GainCalculator;
import us.ihmc.robotics.controllers.MatrixUpdater;
import us.ihmc.robotics.controllers.PositionPIDGainsInterface;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.dataStructures.listener.VariableChangedListener;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.YoVariable;

public class YoFootPositionGains implements YoPositionPIDGainsInterface
{
   private final DoubleYoVariable proportionalXYGain, proportionalZGain;
   private final DoubleYoVariable derivativeXYGain, derivativeZGain;
   private final DoubleYoVariable derivativeCorrectionXYGain, derivativeCorrectionZGain;
   private final DoubleYoVariable dampingRatio;

   private final DoubleYoVariable maximumFeedback;
   private final DoubleYoVariable maximumFeedbackRate;
   private final DoubleYoVariable maxDerivativeError;
   private final DoubleYoVariable maxProportionalError;

   public YoFootPositionGains(String suffix, YoVariableRegistry registry)
   {
      proportionalXYGain = new DoubleYoVariable("kpXYLinear" + suffix, registry);
      proportionalZGain = new DoubleYoVariable("kpZLinear" + suffix, registry);
      derivativeXYGain = new DoubleYoVariable("kdXYLinear" + suffix, registry);
      derivativeZGain = new DoubleYoVariable("kdZLinear" + suffix, registry);
      derivativeCorrectionXYGain = new DoubleYoVariable("kvXYLinear" + suffix, registry);
      derivativeCorrectionZGain = new DoubleYoVariable("kvZLinear" + suffix, registry);
      dampingRatio = new DoubleYoVariable("zetaLinear" + suffix, registry);

      maximumFeedback = new DoubleYoVariable("maximumLinearFeedback" + suffix, registry);
      maximumFeedbackRate = new DoubleYoVariable("maximumLinearFeedbackRate" + suffix, registry);
      maxDerivativeError = new DoubleYoVariable("maximumLinearDerivativeError" + suffix, registry);
      maxProportionalError = new DoubleYoVariable("maximumLinearProportionalError" + suffix, registry);

      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);
      maxDerivativeError.set(Double.POSITIVE_INFINITY);
      maxProportionalError.set(Double.POSITIVE_INFINITY);
   }

   @Override
   public void reset()
   {
      proportionalXYGain.set(0.0);
      proportionalZGain.set(0.0);
      derivativeXYGain.set(0.0);
      derivativeZGain.set(0.0);
      derivativeCorrectionXYGain.set(0.0);
      derivativeCorrectionZGain.set(0.0);
      dampingRatio.set(0.0);
      maximumFeedback.set(Double.POSITIVE_INFINITY);
      maximumFeedbackRate.set(Double.POSITIVE_INFINITY);
      maxDerivativeError.set(Double.POSITIVE_INFINITY);
      maxProportionalError.set(Double.POSITIVE_INFINITY);
   }

   @Override
   public Matrix3d createProportionalGainMatrix()
   {
      Matrix3d proportionalGainMatrix = new Matrix3d();

      proportionalXYGain.addVariableChangedListener(new MatrixUpdater(0, 0, proportionalGainMatrix));
      proportionalXYGain.addVariableChangedListener(new MatrixUpdater(1, 1, proportionalGainMatrix));
      proportionalZGain.addVariableChangedListener(new MatrixUpdater(2, 2, proportionalGainMatrix));

      proportionalXYGain.notifyVariableChangedListeners();
      proportionalZGain.notifyVariableChangedListeners();

      return proportionalGainMatrix;
   }

   @Override
   public Matrix3d createDerivativeGainMatrix()
   {
      Matrix3d derivativeGainMatrix = new Matrix3d();

      derivativeXYGain.addVariableChangedListener(new MatrixUpdater(0, 0, derivativeGainMatrix));
      derivativeXYGain.addVariableChangedListener(new MatrixUpdater(1, 1, derivativeGainMatrix));
      derivativeZGain.addVariableChangedListener(new MatrixUpdater(2, 2, derivativeGainMatrix));

      derivativeXYGain.notifyVariableChangedListeners();
      derivativeZGain.notifyVariableChangedListeners();

      return derivativeGainMatrix;
   }

   @Override
   public Matrix3d createDerivativeCorrectionGainMatrix()
   {
      Matrix3d derivativeCorrectionGainMatrix = new Matrix3d();

      derivativeCorrectionXYGain.addVariableChangedListener(new MatrixUpdater(0, 0, derivativeCorrectionGainMatrix));
      derivativeCorrectionXYGain.addVariableChangedListener(new MatrixUpdater(1, 1, derivativeCorrectionGainMatrix));
      derivativeCorrectionZGain.addVariableChangedListener(new MatrixUpdater(2, 2, derivativeCorrectionGainMatrix));

      derivativeCorrectionXYGain.notifyVariableChangedListeners();
      derivativeCorrectionZGain.notifyVariableChangedListeners();

      return derivativeCorrectionGainMatrix;
   }

   @Override
   public Matrix3d createIntegralGainMatrix()
   {
      return new Matrix3d();
   }

   public void createDerivativeGainUpdater(boolean updateNow)
   {
      VariableChangedListener kdXYUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeXYGain.set(GainCalculator.computeDerivativeGain(proportionalXYGain.getDoubleValue(), dampingRatio.getDoubleValue()));
         }
      };

      proportionalXYGain.addVariableChangedListener(kdXYUpdater);
      dampingRatio.addVariableChangedListener(kdXYUpdater);

      if (updateNow)
         kdXYUpdater.variableChanged(null);

      VariableChangedListener kdZUpdater = new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            derivativeZGain.set(GainCalculator.computeDerivativeGain(proportionalZGain.getDoubleValue(), dampingRatio.getDoubleValue()));
         }
      };

      proportionalZGain.addVariableChangedListener(kdZUpdater);
      dampingRatio.addVariableChangedListener(kdZUpdater);

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

   @Override
   public void setDerivativeCorrectionGains(double derivativeCorrectionGainX, double derivativeCorrectionGainY, double derivativeCorrectionGainZ)
   {
      derivativeCorrectionXYGain.set(derivativeCorrectionGainX);
      derivativeCorrectionZGain.set(derivativeCorrectionGainZ);
   }

   public void setDerivativeCorrectionGains(double derivativeCorrectionGainXY, double derivativeCorrectionGainZ)
   {
      derivativeCorrectionXYGain.set(derivativeCorrectionGainXY);
      derivativeCorrectionZGain.set(derivativeCorrectionGainZ);
   }

   public void setDampingRatio(double dampingRatio)
   {
      this.dampingRatio.set(dampingRatio);
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
   public void setDerivativeCorrectionGains(double[] derivativeCorrectionGains)
   {
      setDerivativeCorrectionGains(derivativeCorrectionGains[0], derivativeCorrectionGains[1], derivativeCorrectionGains[2]);
   }

   @Override
   public void setIntegralGains(double[] integralGains, double maxIntegralError)
   {
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
   public void set(PositionPIDGainsInterface gains)
   {
      setProportionalGains(gains.getProportionalGains());
      setDerivativeGains(gains.getDerivativeGains());
      setDerivativeCorrectionGains(gains.getDerivativeCorrectionGains());
      setIntegralGains(gains.getIntegralGains(), gains.getMaximumIntegralError());
      setMaxFeedbackAndFeedbackRate(gains.getMaximumFeedback(), gains.getMaximumFeedbackRate());
      setMaxDerivativeError(gains.getMaximumDerivativeError());
      setMaxProportionalError(gains.getMaximumProportionalError());
   }

   @Override
   public DoubleYoVariable getYoMaximumFeedback()
   {
      return maximumFeedback;
   }

   @Override
   public DoubleYoVariable getYoMaximumFeedbackRate()
   {
      return maximumFeedbackRate;
   }

   @Override
   public DoubleYoVariable getYoMaximumDerivativeError()
   {
      return maxDerivativeError;
   }

   @Override
   public DoubleYoVariable getYoMaximumProportionalError()
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

   private double[] tempDerivativeCorrectionGains = new double[3];

   @Override
   public double[] getDerivativeCorrectionGains()
   {
      tempDerivativeCorrectionGains[0] = derivativeCorrectionXYGain.getDoubleValue();
      tempDerivativeCorrectionGains[1] = derivativeCorrectionXYGain.getDoubleValue();
      tempDerivativeCorrectionGains[2] = derivativeCorrectionZGain.getDoubleValue();

      return tempDerivativeCorrectionGains;
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
