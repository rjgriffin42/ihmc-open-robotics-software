package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.robotics.controllers.YoPIDSE3Gains;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class YoPlanarFootSE3Gains implements YoPIDSE3Gains
{
   private final YoPlanarFootPositionGains positionGains;
   private final YoPlanarFootOrientationGains orientationGains;

   public YoPlanarFootSE3Gains(String prefix, YoVariableRegistry registry)
   {
      positionGains = new YoPlanarFootPositionGains(prefix, registry);
      orientationGains = new YoPlanarFootOrientationGains(prefix, registry);
   }

   @Override
   public YoPlanarFootPositionGains getPositionGains()
   {
      return positionGains;
   }

   @Override
   public YoPlanarFootOrientationGains getOrientationGains()
   {
      return orientationGains;
   }

   public void setPositionProportionalGains(double proportionalGains)
   {
      setPositionProportionalGains(proportionalGains, proportionalGains);
   }

   public void setPositionProportionalGains(double proportionalGainX, double proportionalGainY)
   {
      positionGains.setProportionalGains(proportionalGainX, proportionalGainY);
   }

   public void setPositionDerivativeGains(double derivativeGains)
   {
      setPositionDerivativeGains(derivativeGains, derivativeGains);
   }

   public void setPositionDerivativeGains(double derivativeGainX, double derivativeGainZ)
   {
      positionGains.setDerivativeGains(derivativeGainX, derivativeGainZ);
   }

   public void setPositionDampingRatio(double dampingRatio)
   {
      positionGains.setDampingRatio(dampingRatio);
   }

   public void setPositionMaxFeedbackAndFeedbackRate(double maxFeedback, double maxFeedbackRate)
   {
      positionGains.setMaxFeedbackAndFeedbackRate(maxFeedback, maxFeedbackRate);
   }

   public void setPositionMaxDerivativeError(double maxDerivativeError)
   {
      positionGains.setMaxDerivativeError(maxDerivativeError);
   }

   public void setPositionMaxProportionalError(double maxProportionalError)
   {
      positionGains.setMaxProportionalError(maxProportionalError);
   }

   public void setOrientationProportionalGains(double proportionalGainX)
   {
      orientationGains.setProportionalGains(proportionalGainX);
   }

   public void setOrientationDerivativeGains(double derivativeGainX)
   {
      orientationGains.setDerivativeGains(derivativeGainX);
   }

   public void setOrientationDampingRatio(double dampingRatio)
   {
      orientationGains.setDampingRatio(dampingRatio);
   }

   public void setOrientationMaxFeedbackAndFeedbackRate(double maxFeedback, double maxFeedbackRate)
   {
      orientationGains.setMaxFeedbackAndFeedbackRate(maxFeedback, maxFeedbackRate);
   }

   public void setOrientationMaxDerivativeError(double maxDerivativeError)
   {
      orientationGains.setMaxDerivativeError(maxDerivativeError);
   }

   public void setOrientationMaxProportionalError(double maxProportionalError)
   {
      orientationGains.setMaxProportionalError(maxProportionalError);
   }

   public void createDerivativeGainUpdater(boolean updateNow)
   {
      positionGains.createDerivativeGainUpdater(updateNow);
      orientationGains.createDerivativeGainUpdater(updateNow);
   }
}
