package us.ihmc.quadrupedRobotics.parameters;

public class DefaultSwingTargetGeneratorParameters implements QuadrupedControllerParameters 
{
   private final double minimumVelocityForFullSkew = 0.1;
   private final double minimumDistanceFromSameSideFoot = 0.04;
   private final double stanceLength = 0.34;
   private final double stanceWidth = 0.24;
   private final double maxForwardSkew = 0.29;
   private final double maxLateralSkew = 0.1;
   private final double maxYawPerStep = 0.25;
   private final double initialDesiredFootCorrectionBreakFrequency = 1.0;
   private final double defaultDesiredFootCorrectionBreakFrequency = 0.15;
   
   @Override
   public double getMinimumVelocityForFullSkew()
   {
      return minimumVelocityForFullSkew;
   }

   @Override
   public double getMinimumDistanceFromSameSideFoot()
   {
      return minimumDistanceFromSameSideFoot;
   }

   @Override
   public double getStanceLength()
   {
      return stanceLength;
   }

   @Override
   public double getStanceWidth()
   {
      return stanceWidth;
   }

   @Override
   public double getMaxForwardSkew()
   {
      return maxForwardSkew;
   }
   
   @Override
   public double getMaxLateralSkew()
   {
      return maxLateralSkew;
   }

   @Override
   public double getMaxYawPerStep()
   {
      return maxYawPerStep;
   }

   @Override
   public double getInitalCoMHeight()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public double getDefaultSwingHeight()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public double getDefaultSwingDuration()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public double getDefaultSubCircleRadius()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public double getInitialDesiredFootCorrectionBreakFrequency()
   {
      return initialDesiredFootCorrectionBreakFrequency;
   }

   @Override
   public double getDefaultDesiredFootCorrectionBreakFrequency()
   {
      return defaultDesiredFootCorrectionBreakFrequency;
   }
}