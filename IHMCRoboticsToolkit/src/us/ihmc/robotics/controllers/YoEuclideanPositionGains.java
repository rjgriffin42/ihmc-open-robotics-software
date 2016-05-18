package us.ihmc.robotics.controllers;

import javax.vecmath.Matrix3d;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

public class YoEuclideanPositionGains implements YoPositionPIDGainsInterface
{
   private static final String[] directionNames = new String[] {"x", "y", "z"};

   private final DoubleYoVariable[] proportionalGains = new DoubleYoVariable[3];
   private final DoubleYoVariable[] derivativeGains = new DoubleYoVariable[3];
   private final DoubleYoVariable[] integralGains = new DoubleYoVariable[3];
   private final DoubleYoVariable maxIntegralError;

   private final DoubleYoVariable maxAcceleration;
   private final DoubleYoVariable maxJerk;
   private final DoubleYoVariable maxVelocityError;
   private final DoubleYoVariable maxError;

   public YoEuclideanPositionGains(String prefix, YoVariableRegistry registry)
   {
      String baseProportionalGainName = prefix + "PositionProportionalGain";
      String baseDerivativeGainName = prefix + "PositionDerivativeGain";
      String baseIntegralGainName = prefix + "PositionIntegralGain";

      for (int i = 0; i < 3; i++)
      {
         proportionalGains[i] = new DoubleYoVariable(baseProportionalGainName + directionNames[i], registry);
         derivativeGains[i] = new DoubleYoVariable(baseDerivativeGainName + directionNames[i], registry);
         integralGains[i] = new DoubleYoVariable(baseIntegralGainName + directionNames[i], registry);
      }

      maxIntegralError = new DoubleYoVariable(prefix + "PositionMaxIntegralError", registry);
      maxAcceleration = new DoubleYoVariable(prefix + "PositionMaxAcceleration", registry);
      maxJerk = new DoubleYoVariable(prefix + "PositionMaxJerk", registry);
      maxVelocityError = new DoubleYoVariable(prefix + "PositionMaxVelocityError", registry);
      maxError = new DoubleYoVariable(prefix + "PositionMaxError", registry);

      maxAcceleration.set(Double.POSITIVE_INFINITY);
      maxJerk.set(Double.POSITIVE_INFINITY);
      maxVelocityError.set(Double.POSITIVE_INFINITY);
      maxError.set(Double.POSITIVE_INFINITY);
   }

   @Override
   public void reset()
   {
      for (int i = 0; i < proportionalGains.length; i++)
      {
         proportionalGains[i].set(0.0);
         derivativeGains[i].set(0.0);
         integralGains[i].set(0.0);
      }

      maxIntegralError.set(0.0);
      maxAcceleration.set(Double.POSITIVE_INFINITY);
      maxJerk.set(Double.POSITIVE_INFINITY);
      maxVelocityError.set(Double.POSITIVE_INFINITY);
      maxError.set(Double.POSITIVE_INFINITY);
   }

   @Override
   public Matrix3d createProportionalGainMatrix()
   {
      Matrix3d proportionalGainMatrix = new Matrix3d();

      for (int i = 0; i < 3; i++)
      {
         proportionalGains[i].addVariableChangedListener(new MatrixUpdater(i, i, proportionalGainMatrix));
         proportionalGains[i].notifyVariableChangedListeners();
      }

      return proportionalGainMatrix;
   }

   @Override
   public Matrix3d createDerivativeGainMatrix()
   {
      Matrix3d derivativeGainMatrix = new Matrix3d();

      for (int i = 0; i < 3; i++)
      {
         derivativeGains[i].addVariableChangedListener(new MatrixUpdater(i, i, derivativeGainMatrix));
         derivativeGains[i].notifyVariableChangedListeners();
      }

      return derivativeGainMatrix;
   }

   @Override
   public Matrix3d createIntegralGainMatrix()
   {
      Matrix3d integralGainMatrix = new Matrix3d();

      for (int i = 0; i < 3; i++)
      {
         integralGains[i].addVariableChangedListener(new MatrixUpdater(i, i, integralGainMatrix));
         integralGains[i].notifyVariableChangedListeners();
      }

      return integralGainMatrix;
   }

   @Override
   public void setProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      proportionalGains[0].set(proportionalGainX);
      proportionalGains[1].set(proportionalGainY);
      proportionalGains[2].set(proportionalGainZ);
   }

   @Override
   public void setDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      derivativeGains[0].set(derivativeGainX);
      derivativeGains[1].set(derivativeGainY);
      derivativeGains[2].set(derivativeGainZ);
   }

   @Override
   public void setIntegralGains(double integralGainX, double integralGainY, double integralGainZ, double maxIntegralError)
   {
      integralGains[0].set(integralGainX);
      integralGains[1].set(integralGainY);
      integralGains[2].set(integralGainZ);

      this.maxIntegralError.set(maxIntegralError);
   }

   @Override
   public void setProportionalGains(double[] proportionalGains)
   {
      for (int i = 0; i < proportionalGains.length; i++)
      {
         this.proportionalGains[i].set(proportionalGains[i]);
      }
   }

   @Override
   public void setDerivativeGains(double[] derivativeGains)
   {
      for (int i = 0; i < derivativeGains.length; i++)
      {
         this.derivativeGains[i].set(derivativeGains[i]);
      }
   }

   @Override
   public void setIntegralGains(double[] integralGains, double maxIntegralError)
   {
      for (int i = 0; i < integralGains.length; i++)
      {
         this.integralGains[i].set(integralGains[i]);
      }

      this.maxIntegralError.set(maxIntegralError);
   }

   @Override
   public void setMaxAccelerationAndJerk(double maxAcceleration, double maxJerk)
   {
      this.maxAcceleration.set(maxAcceleration);
      this.maxJerk.set(maxJerk);
   }

   @Override
   public void setMaxVelocityError(double maxVelocityError)
   {
      this.maxVelocityError.set(maxVelocityError);
   }

   @Override
   public void setMaxError(double maxError)
   {
      this.maxError.set(maxError);
   }

   @Override
   public void set(PositionPIDGainsInterface gains)
   {
      setProportionalGains(gains.getProportionalGains());
      setDerivativeGains(gains.getDerivativeGains());
      setIntegralGains(gains.getIntegralGains(), gains.getMaximumIntegralError());
      setMaxAccelerationAndJerk(gains.getMaximumAcceleration(), gains.getMaximumJerk());
      setMaxVelocityError(gains.getMaximumVelocityError());
      setMaxError(gains.getMaximumError());
   }

   @Override
   public DoubleYoVariable getYoMaximumAcceleration()
   {
      return maxAcceleration;
   }

   @Override
   public DoubleYoVariable getYoMaximumJerk()
   {
      return maxJerk;
   }

   @Override
   public DoubleYoVariable getYoMaximumVelocityError()
   {
      return maxVelocityError;
   }

   @Override
   public DoubleYoVariable getYoMaximumError()
   {
      return maxError;
   }

   private double[] tempPropotionalGains = new double[3];

   @Override
   public double[] getProportionalGains()
   {
      for (int i = 0; i < 3; i++)
         tempPropotionalGains[i] = proportionalGains[i].getDoubleValue();
      return tempPropotionalGains;
   }

   private double[] tempDerivativeGains = new double[3];

   @Override
   public double[] getDerivativeGains()
   {
      for (int i = 0; i < 3; i++)
         tempDerivativeGains[i] = derivativeGains[i].getDoubleValue();
      return tempDerivativeGains;
   }

   private double[] tempIntegralGains = new double[3];

   @Override
   public double[] getIntegralGains()
   {
      for (int i = 0; i < 3; i++)
         tempIntegralGains[i] = integralGains[i].getDoubleValue();
      return tempIntegralGains;
   }

   @Override
   public double getMaximumIntegralError()
   {
      return maxIntegralError.getDoubleValue();
   }

   @Override
   public double getMaximumAcceleration()
   {
      return maxAcceleration.getDoubleValue();
   }

   @Override
   public double getMaximumJerk()
   {
      return maxJerk.getDoubleValue();
   }

   @Override
   public double getMaximumVelocityError()
   {
      return maxVelocityError.getDoubleValue();
   }

   @Override
   public double getMaximumError()
   {
      return maxError.getDoubleValue();
   }

}
