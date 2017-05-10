package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.tools.exceptions.NoConvergenceException;

import java.util.ArrayList;
import java.util.List;

public class ICPTimingOptimizationController extends ICPOptimizationController
{
   /** gradient descent variables */
   private final DoubleYoVariable timingAdjustmentWeight = new DoubleYoVariable(yoNamePrefix + "TimingAdjustmentWeight", registry);
   private final DoubleYoVariable gradientThresholdForAdjustment = new DoubleYoVariable(yoNamePrefix + "GradientThresholdForAdjustment", registry);
   private final DoubleYoVariable gradientDescentGain = new DoubleYoVariable(yoNamePrefix + "GradientDescentGain", registry);

   private final DoubleYoVariable timingAdjustmentAttenuation = new DoubleYoVariable(yoNamePrefix + "TimingAdjustmentAttenuation", registry);
   private final DoubleYoVariable timingSolutionLowerBound = new DoubleYoVariable(yoNamePrefix + "TimingSolutionLowerBound", registry);
   private final DoubleYoVariable timingSolutionUpperBound = new DoubleYoVariable(yoNamePrefix + "TimingSolutionUpperBound", registry);

   private final DoubleYoVariable timingDeadline = new DoubleYoVariable(yoNamePrefix + "TimingDeadline", registry);
   private final BooleanYoVariable finishedOnTime = new BooleanYoVariable(yoNamePrefix + "FinishedOnTime", registry);

   private final double variationSizeToComputeTimingGradient;
   private final int maxNumberOfGradientIterations;
   private final int numberOfGradientReductions;
   private final double minimumSwingDuration;
   private static final double percentCostRequiredDecrease = 0.05;

   private final DoubleYoVariable referenceSwingDuration = new DoubleYoVariable(yoNamePrefix + "ReferenceSwingDuration", registry);

   private final List<DoubleYoVariable> swingTimings = new ArrayList<>();
   private final List<DoubleYoVariable> timingAdjustments = new ArrayList<>();
   private final List<DoubleYoVariable> costToGos = new ArrayList<>();
   private final List<DoubleYoVariable> costToGoGradients = new ArrayList<>();

   private final IntegerYoVariable numberOfGradientIterations = new IntegerYoVariable("numberOfGradientIterations", registry);
   private final IntegerYoVariable numberOfGradientReductionIterations = new IntegerYoVariable("numberOfGradientReductionIterations", registry);
   private final DoubleYoVariable estimatedMinimumCostSwingTime = new DoubleYoVariable("estimatedMinimumCostSwingTime", registry);

   private final ICPTimingCostFunctionEstimator costFunctionEstimator = new ICPTimingCostFunctionEstimator();

   public ICPTimingOptimizationController(CapturePointPlannerParameters icpPlannerParameters, ICPOptimizationParameters icpOptimizationParameters,
         WalkingControllerParameters walkingControllerParameters, BipedSupportPolygons bipedSupportPolygons,
         SideDependentList<? extends ContactablePlaneBody> contactableFeet, double controlDT, YoVariableRegistry parentRegistry,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      super(icpPlannerParameters, icpOptimizationParameters, bipedSupportPolygons, contactableFeet, controlDT, false, yoGraphicsListRegistry);

      numberOfFootstepsToConsider.set(icpOptimizationParameters.numberOfFootstepsToConsider());

      defaultSwingSplitFraction.set(icpPlannerParameters.getSwingDurationAlpha());
      defaultTransferSplitFraction.set(icpPlannerParameters.getTransferDurationAlpha());

      transferSplitFractionUnderDisturbance.set(icpOptimizationParameters.getDoubleSupportSplitFractionForBigAdjustment());
      magnitudeForBigAdjustment.set(icpOptimizationParameters.getMagnitudeForBigAdjustment());

      timingAdjustmentWeight.set(icpOptimizationParameters.getTimingAdjustmentGradientDescentWeight());
      gradientThresholdForAdjustment.set(icpOptimizationParameters.getGradientThresholdForTimingAdjustment());
      gradientDescentGain.set(icpOptimizationParameters.getGradientDescentGain());
      timingAdjustmentAttenuation.set(icpOptimizationParameters.getTimingAdjustmentAttenuation());
      timingDeadline.set(icpOptimizationParameters.getMaximumDurationForOptimization());

      variationSizeToComputeTimingGradient = icpOptimizationParameters.getVariationSizeToComputeTimingGradient();
      maxNumberOfGradientIterations = icpOptimizationParameters.getMaximumNumberOfGradientIterations();
      numberOfGradientReductions = icpOptimizationParameters.getMaximumNumberOfGradientReductions();
      minimumSwingDuration = walkingControllerParameters.getMinimumSwingTimeForDisturbanceRecovery();

      for (int i = 0; i < maxNumberOfGradientIterations; i++)
      {
         DoubleYoVariable swingTiming = new DoubleYoVariable(yoNamePrefix + "SwingTiming" + i, registry);
         DoubleYoVariable timingAdjustment = new DoubleYoVariable(yoNamePrefix + "TimingAdjustment" + i, registry);
         DoubleYoVariable costToGo = new DoubleYoVariable(yoNamePrefix + "CostToGo" + i, registry);
         DoubleYoVariable costToGoGradient = new DoubleYoVariable(yoNamePrefix + "CostToGoGradient" + i, registry);

         swingTimings.add(swingTiming);
         timingAdjustments.add(timingAdjustment);
         costToGos.add(costToGo);
         costToGoGradients.add(costToGoGradient);
      }

      parentRegistry.addChild(registry);
   }


   /** {@inheritDoc} */
   @Override
   public void submitRemainingTimeInSwingUnderDisturbance(double remainingTimeForSwing)
   {
      // do nothing
   }


   /** {@inheritDoc} */
   @Override
   public void initializeForTransfer(double initialTime, RobotSide transferToSide, double omega0)
   {
      super.initializeForTransfer(initialTime, transferToSide, omega0);

      referenceSwingDuration.set(swingDurations.get(0).getDoubleValue());
   }

   /** {@inheritDoc} */
   @Override
   public void initializeForSingleSupport(double initialTime, RobotSide supportSide, double omega0)
   {
      super.initializeForSingleSupport(initialTime, supportSide, omega0);

      referenceSwingDuration.set(swingDurations.get(0).getDoubleValue());
   }













   /** {@inheritDoc} */
   @Override
   public void compute(double currentTime, FramePoint2d desiredICP, FrameVector2d desiredICPVelocity, FramePoint2d currentICP, double omega0)
   {
      controllerTimer.startMeasurement();

      desiredICP.changeFrame(worldFrame);
      desiredICPVelocity.changeFrame(worldFrame);
      currentICP.changeFrame(worldFrame);

      this.currentICP.set(currentICP);
      this.desiredICP.set(desiredICP);
      this.desiredICPVelocity.set(desiredICPVelocity);

      solutionHandler.getControllerReferenceCMP(referenceCMP);

      computeTimeInCurrentState(currentTime);
      computeTimeRemainingInState();

      int numberOfFootstepsToConsider = clipNumberOfFootstepsToConsiderToProblem(this.numberOfFootstepsToConsider.getIntegerValue());

      scaleStepRegularizationWeightWithTime();
      scaleFeedbackWeightWithGain();

      NoConvergenceException noConvergenceException;
      qpSolverTimer.startMeasurement();
      if (isStanding.getBooleanValue())
      {
         submitSolverTaskConditionsForFeedbackOnlyControl();
         noConvergenceException = solveQP();
         solver.setPreviousFootstepSolutionFromCurrent();
         solver.setPreviousFeedbackDeltaSolutionFromCurrent();
         numberOfGradientIterations.set(0);
         numberOfGradientReductionIterations.set(0);
         finishedOnTime.set(true);
         costToGos.get(0).set(solver.getCostToGo());
      }
      else if (isInTransfer.getBooleanValue())
      {
         submitSolverTaskConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);
         noConvergenceException = solveQP();
         solver.setPreviousFootstepSolutionFromCurrent();
         solver.setPreviousFeedbackDeltaSolutionFromCurrent();
         numberOfGradientIterations.set(0);
         numberOfGradientReductionIterations.set(0);
         finishedOnTime.set(true);
         costToGos.get(0).set(solver.getCostToGo());
      }
      else
      {
         noConvergenceException = solveGradientDescent(numberOfFootstepsToConsider, omega0);
      }
      qpSolverTimer.stopMeasurement();

      extractSolutionsFromSolver(numberOfFootstepsToConsider, omega0, noConvergenceException);

      controllerTimer.stopMeasurement();
   }





   private NoConvergenceException solveGradientDescent(int numberOfFootstepsToConsider, double omega0)
   {
      for (int i = 0; i < maxNumberOfGradientIterations; i++)
      {
         costToGos.get(i).setToNaN();
         costToGoGradients.get(i).setToNaN();
         timingAdjustments.get(i).setToNaN();
         swingTimings.get(i).setToNaN();
      }

      costFunctionEstimator.reset();
      double timingLowerBound = minimumSwingDuration;
      double timingUpperBound = Double.POSITIVE_INFINITY;
      boolean finishedOnTime = true;

      submitSolverTaskConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);

      NoConvergenceException noConvergenceException = solveQP();
      double costToGoUnvaried = computeTotalCostToGo();
      double variationSize = variationSizeToComputeTimingGradient;

      if (noConvergenceException != null)
         return noConvergenceException;

      DoubleYoVariable swingDuration = swingDurations.get(0);
      swingDuration.add(variationSize);

      submitSolverTaskConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);
      noConvergenceException = solveQP();

      if (noConvergenceException != null)
         return noConvergenceException;

      double costToGoWithVariation = computeTotalCostToGo();
      double averageCostToGo = 0.5 * (costToGoWithVariation + costToGoUnvaried);
      double costToGoGradient = (costToGoWithVariation - costToGoUnvaried) / variationSize;
      swingDuration.sub(variationSize);

      costToGos.get(0).set(averageCostToGo);
      costToGoGradients.get(0).set(costToGoGradient);
      swingTimings.get(0).set(swingDuration.getDoubleValue());

      costFunctionEstimator.addPoint(averageCostToGo, costToGoGradient, swingDuration.getDoubleValue());

      int iterationNumber = 0;
      while (Math.abs(costToGoGradient) > gradientThresholdForAdjustment.getDoubleValue())
      {
         // update bounds on the gradient descent
         if (costToGoGradient > 0) // the current gradient is positive, which means that the timing solution must be less
            timingUpperBound = Math.min(swingDuration.getDoubleValue(), timingUpperBound);
         else // the current gradient is negative, which means that the timing solution must be more
            timingLowerBound = Math.max(swingDuration.getDoubleValue(), timingLowerBound);

         // if the cost reduction adjustment requires moving outside the bounds, exit
         if ((MathTools.epsilonEquals(swingDuration.getDoubleValue(), timingLowerBound, 0.0001) && costToGoGradient > 0) ||
               (MathTools.epsilonEquals(swingDuration.getDoubleValue(), timingUpperBound, 0.0001) && costToGoGradient < 0))
            break;

         // estimate time adjustment using gradient based methods
         double timeAdjustment = -gradientDescentGain.getDoubleValue() * costToGoGradient;

         iterationNumber++;
         // exit loop if we've gone too many ticks
         if (iterationNumber >= maxNumberOfGradientIterations)
            break;

         if (controllerTimer.getCurrentTime().getDoubleValue() > timingDeadline.getDoubleValue())
         { // if the controller has taken too long, notify us and break the loop
            finishedOnTime = false;
            break;
         }

         // make sure it doesn't modify the duration outside the bounds
         timeAdjustment = MathTools.clamp(timeAdjustment, timingLowerBound - swingDuration.getDoubleValue(), timingUpperBound - swingDuration.getDoubleValue());
         timingAdjustments.get(iterationNumber).set(timeAdjustment);


         // modify current single support duration
         swingDuration.add(timeAdjustment);

         submitSolverTaskConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);

         noConvergenceException = solveQP();
         if (noConvergenceException != null)
            return noConvergenceException;

         costToGoUnvaried = computeTotalCostToGo();

         int reductionNumber = 0;
         while (costToGoUnvaried >= averageCostToGo + percentCostRequiredDecrease * Math.signum(averageCostToGo) * averageCostToGo)
         {
            // update the bounds
            if (costToGoGradient > 0) // we just decreased the duration, and it caused an increase in cost
               timingLowerBound = Math.max(swingDuration.getDoubleValue(), timingLowerBound);
            else // we just increased the duration, and it caused an increase in cost
               timingUpperBound = Math.min(swingDuration.getDoubleValue(), timingUpperBound);

            // exit loop if we've gone too many ticks
            if (reductionNumber >= numberOfGradientReductions || iterationNumber >= maxNumberOfGradientIterations)
               break;

            if (controllerTimer.getCurrentTime().getDoubleValue() > timingDeadline.getDoubleValue())
            { // if the controller has taken too long, notify us and break the loop
               finishedOnTime = false;
               break;
            }

            // add the current point to the estimator
            costFunctionEstimator.addPoint(costToGoUnvaried, swingDuration.getDoubleValue());

            // the current adjustment causes an increase in cost, so reduce the adjustment
            timeAdjustment = timingAdjustmentAttenuation.getDoubleValue() * timeAdjustment;

            // make sure it doesn't modify the duration outside the bounds
            timeAdjustment = MathTools.clamp(timeAdjustment, swingDuration.getDoubleValue() - timingUpperBound, swingDuration.getDoubleValue() - timingLowerBound);
            timingAdjustments.get(iterationNumber - 1).set(timeAdjustment);
            swingDuration.sub(timeAdjustment);

            // if the cost reduction adjustment requires moving outside the bounds, exit
            if ((MathTools.epsilonEquals(swingDuration.getDoubleValue(), timingLowerBound, 0.0001) && costToGoGradient > 0) ||
                  (MathTools.epsilonEquals(swingDuration.getDoubleValue(), timingUpperBound, 0.0001) && costToGoGradient < 0))
               break;

            // compute new cost at the current time
            submitSolverTaskConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);

            noConvergenceException = solveQP();
            if (noConvergenceException != null)
               return noConvergenceException;

            costToGoUnvaried = computeTotalCostToGo();

            swingTimings.get(iterationNumber).set(swingDuration.getDoubleValue());
            costToGos.get(iterationNumber).set(costToGoUnvaried);
            costToGoGradients.get(iterationNumber).setToNaN();

            iterationNumber++;
            reductionNumber++;
         }

         numberOfGradientReductionIterations.set(reductionNumber);

         // compute gradient at new point
         swingDuration.add(variationSize);

         submitSolverTaskConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);

         noConvergenceException = solveQP();
         if (noConvergenceException != null)
            return noConvergenceException;

         costToGoWithVariation = computeTotalCostToGo();

         averageCostToGo = 0.5 * (costToGoWithVariation + costToGoUnvaried);
         costToGoGradient = (costToGoWithVariation - costToGoUnvaried) / variationSize;
         swingDuration.sub(variationSize);

         swingTimings.get(iterationNumber).set(swingDuration.getDoubleValue());
         costToGos.get(iterationNumber).set(averageCostToGo);
         costToGoGradients.get(iterationNumber).set(costToGoGradient);

         costFunctionEstimator.addPoint(averageCostToGo, costToGoGradient, swingDuration.getDoubleValue());
      }

      timingSolutionLowerBound.set(timingLowerBound);
      timingSolutionUpperBound.set(timingUpperBound);
      this.finishedOnTime.set(finishedOnTime);

      solver.setPreviousFeedbackDeltaSolutionFromCurrent();
      solver.setPreviousFootstepSolutionFromCurrent();

      estimatedMinimumCostSwingTime.set(costFunctionEstimator.getEstimatedCostFunctionSolution());

      numberOfGradientIterations.set(iterationNumber + 1);

      return null;
   }

   public double computeTotalCostToGo()
   {
      double solverCostToGo = solver.getCostToGo();
      double timingCostToGo = timingAdjustmentWeight.getDoubleValue() * Math.pow(swingDurations.get(0).getDoubleValue() - referenceSwingDuration.getDoubleValue(), 2.0);

      return solverCostToGo + timingCostToGo;
   }



}
