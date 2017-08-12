package us.ihmc.robotics.controllers;

import us.ihmc.robotics.controllers.pidGains.GainCalculator;
import us.ihmc.robotics.controllers.pidGains.PID3DGains;
import us.ihmc.robotics.controllers.pidGains.PID3DGainsReadOnly;
import us.ihmc.robotics.controllers.pidGains.PIDSE3Gains;

/**
 * @author twan
 *         Date: 6/4/13
 */
public class SE3PIDGains implements PIDSE3Gains
{
   private final PositionPIDGains positionGains = new PositionPIDGains();
   private final OrientationPIDGains orientationGains = new OrientationPIDGains();

   public void set(double kpPosition, double zetaPosition, double kPOrientation, double zetaOrientation)
   {
      set(kpPosition, zetaPosition, 0.0, 0.0, kPOrientation, zetaOrientation, 0.0, 0.0);
   }

   public void set(double kpPosition, double zetaPosition, double kiPosition, double maxIntegralPosError, double kpOrientation, double zetaOrientation, double kiOrientation, double maxIntegralOriError)
   {
      double kdPosition = GainCalculator.computeDerivativeGain(kpPosition, zetaPosition);
      setPositionGains(kpPosition, kdPosition, kiPosition, maxIntegralPosError);

      double kdOrientation = GainCalculator.computeDerivativeGain(kpOrientation, zetaOrientation);
      setOrientationGains(kpOrientation, kdOrientation, kiOrientation, maxIntegralOriError);
   }

   public void setPositionGains(double proportionalGain, double derivativeGain)
   {
      setPositionGains(proportionalGain, derivativeGain, 0.0, 0.0);
   }

   public void setPositionGains(double proportionalGain, double derivativeGain, double integralGain, double maxIntegralError)
   {
      positionGains.setGains(proportionalGain, derivativeGain, integralGain, maxIntegralError);
   }

   public void setPositionProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      positionGains.setProportionalGains(proportionalGainX, proportionalGainY, proportionalGainZ);
   }

   public void setPositionDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      positionGains.setDerivativeGains(derivativeGainX, derivativeGainY, derivativeGainZ);
   }

   public void setPositionIntegralGains(double integralGainX, double integralGainY, double integralGainZ, double maxIntegralError)
   {
      positionGains.setIntegralGains(integralGainX, integralGainY, integralGainZ, maxIntegralError);
   }

   public void setOrientationGains(double proportionalGain, double derivativeGain)
   {
      orientationGains.setGains(proportionalGain, derivativeGain);
   }

   public void setOrientationGains(double proportionalGain, double derivativeGain, double integralGain, double maxIntegralError)
   {
      orientationGains.setGains(proportionalGain, derivativeGain, integralGain, maxIntegralError);
   }

   public void setOrientationProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      orientationGains.setProportionalGains(proportionalGainX, proportionalGainY, proportionalGainZ);
   }

   public void setOrientationDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      orientationGains.setDerivativeGains(derivativeGainX, derivativeGainY, derivativeGainZ);
   }

   public void setOrientationIntegralGains(double integralGainX, double integralGainY, double integralGainZ, double maxIntegralError)
   {
      orientationGains.setIntegralGains(integralGainX, integralGainY, integralGainZ, maxIntegralError);
   }

   public void setPositionMaximumFeedbackAndFeedbackRate(double maxFeedback, double maxFeedbackRate)
   {
      positionGains.setMaxFeedbackAndFeedbackRate(maxFeedback, maxFeedbackRate);
   }

   public void setOrientationMaximumFeedbackAndFeedbackRate(double maxFeedback, double maxFeedbackRate)
   {
      orientationGains.setMaxFeedbackAndFeedbackRate(maxFeedback, maxFeedbackRate);
   }

   public void setOrientationMaximumDerivativeError(double maxDerivativeError)
   {
      orientationGains.setMaxDerivativeError(maxDerivativeError);
   }

   public void setOrientationMaximumProportionalError(double maxProportionalError)
   {
      orientationGains.setMaxProportionalError(maxProportionalError);
   }

   @Override
   public void set(PIDSE3Gains gains)
   {
      positionGains.set(gains.getPositionGains());
      orientationGains.set(gains.getOrientationGains());
   }

   @Override
   public void setPositionGains(PID3DGainsReadOnly positionGains)
   {
      this.positionGains.set(positionGains);
   }

   @Override
   public void setOrientationGains(PID3DGainsReadOnly orientationGains)
   {
      this.orientationGains.set(orientationGains);
   }

   @Override
   public PID3DGains getPositionGains()
   {
      return positionGains;
   }

   @Override
   public PID3DGains getOrientationGains()
   {
      return orientationGains;
   }
}
