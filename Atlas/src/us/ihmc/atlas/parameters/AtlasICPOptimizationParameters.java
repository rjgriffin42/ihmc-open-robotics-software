package us.ihmc.atlas.parameters;

import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;

/** {@inheritDoc} */
public class AtlasICPOptimizationParameters extends ICPOptimizationParameters
{
   private final boolean runningOnRealRobot;

   public AtlasICPOptimizationParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;
   }

   /** {@inheritDoc} */
   @Override
   public int numberOfFootstepsToConsider()
   {
      return 1;
   }

   /** {@inheritDoc} */
   @Override
   public double getFootstepWeight()
   {
      return runningOnRealRobot ? 20.0 : 15.0;
   }

   /** {@inheritDoc} */
   @Override
   public double getFootstepRegularizationWeight()
   {
      return runningOnRealRobot ? 0.001 : 0.001;
   }

   /** {@inheritDoc} */
   @Override
   public double getFeedbackWeight()
   {
      return 0.05;
   }

   /** {@inheritDoc} */
   @Override
   public double getFeedbackRegularizationWeight()
   {
      return runningOnRealRobot ? 0.0001 : 0.001;
   }

   /** {@inheritDoc} */
   @Override
   public double getFeedbackParallelGain()
   {
      return runningOnRealRobot ? 3.0 : 5.0;
   }

   /** {@inheritDoc} */
   @Override
   public double getFeedbackOrthogonalGain()
   {
      return runningOnRealRobot ? 2.5 : 5.0;
   }

   /** {@inheritDoc} */
   @Override
   public double getDynamicRelaxationWeight()
   {
      return runningOnRealRobot ? 500.0 : 5000.0;
   }

   /** {@inheritDoc} */
   @Override
   public double getDynamicRelaxationDoubleSupportWeightModifier()
   {
      return runningOnRealRobot ? 1.0 : 5.0;
   }

   /** {@inheritDoc} */
   @Override
   public boolean scaleStepRegularizationWeightWithTime()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean scaleFeedbackWeightWithGain()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean scaleUpcomingStepWeights()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useFeedback()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useFeedbackRegularization()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useStepAdjustment()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useFootstepRegularization()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useFeedbackWeightHardening()
   {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean useICPFromBeginningOfState()
   {
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public double getMinimumFootstepWeight()
   {
      return 0.0001;
   }

   /** {@inheritDoc} */
   @Override
   public double getMinimumFeedbackWeight()
   {
      return 0.0001;
   }

   /** {@inheritDoc} */
   @Override
   public double getMinimumTimeRemaining()
   {
      return 0.001;
   }

   /** {@inheritDoc} */
   @Override
   public double getFeedbackWeightHardeningMultiplier()
   {
      return 20.0;
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxCMPForwardExit()
   {
      return 0.01;
   }

   /** {@inheritDoc} */
   @Override
   public double getMaxCMPLateralExit()
   {
      return 0.005;
   }

   /** {@inheritDoc} */
   @Override
   public double getForwardAdjustmentDeadband()
   {
      return 0.03;
   }

   /** {@inheritDoc} */
   @Override
   public double getLateralAdjustmentDeadband()
   {
      return 0.03;
   }

   /** {@inheritDoc} */
   @Override
   public double getRemainingTimeToStopAdjusting()
   {
      return 0.05;
   }
   
   /** {@inheritDoc} */
   @Override
   public boolean useDiscontinuousDeadband()
   {
      return false;
   }
}
