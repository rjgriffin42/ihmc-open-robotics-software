package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.NewStateMultiplierCalculator;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.FootstepRecursionMultiplierCalculator;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.dataStructures.variable.IntegerYoVariable;
import us.ihmc.robotics.geometry.*;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFrameVector2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.tools.exceptions.NoConvergenceException;
import us.ihmc.tools.io.printing.PrintTools;

import java.util.ArrayList;

public class ICPOptimizationController
{
   private static final boolean VISUALIZE = true;
   private static final boolean COMPUTE_COST_TO_GO = true;
   private static final boolean USE_NEW_MULTIPLIER_CALCULATOR = true;

   private static final String yoNamePrefix = "controller";
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final IntegerYoVariable numberOfFootstepsToConsider = new IntegerYoVariable("numberOfFootstepsToConsider", registry);


   private final boolean useTwoCMPs;
   private final boolean useInitialICP;
   private final boolean useFootstepRegularization;
   private final boolean useFeedbackRegularization;

   private final BooleanYoVariable useStepAdjustment = new BooleanYoVariable("useStepAdjustment", registry);

   private final BooleanYoVariable scaleStepRegularizationWeightWithTime = new BooleanYoVariable("scaleStepRegularizationWeightWithTime", registry);
   private final BooleanYoVariable scaleFeedbackWeightWithGain = new BooleanYoVariable("scaleFeedbackWeightWithGain", registry);
   private final BooleanYoVariable scaleUpcomingStepWeights = new BooleanYoVariable("scaleUpcomingStepWeights", registry);

   private final BooleanYoVariable swingSpeedUpEnabled = new BooleanYoVariable(yoNamePrefix + "SwingSpeedUpEnabled", registry);

   private final BooleanYoVariable isStanding = new BooleanYoVariable(yoNamePrefix + "IsStanding", registry);
   private final BooleanYoVariable isInTransfer = new BooleanYoVariable(yoNamePrefix + "IsInTransfer", registry);
   private final BooleanYoVariable isInitialTransfer = new BooleanYoVariable(yoNamePrefix + "IsInitialTransfer", registry);

   private final DoubleYoVariable doubleSupportDuration = new DoubleYoVariable(yoNamePrefix + "DoubleSupportDuration", registry);
   private final DoubleYoVariable singleSupportDuration = new DoubleYoVariable(yoNamePrefix + "SingleSupportDuration", registry);
   private final DoubleYoVariable initialDoubleSupportDuration = new DoubleYoVariable(yoNamePrefix + "InitialTransferDuration", registry);

   private final EnumYoVariable<RobotSide> transferToSide = new EnumYoVariable<>(yoNamePrefix + "TransferToSide", registry, RobotSide.class, true);
   private final EnumYoVariable<RobotSide> supportSide = new EnumYoVariable<>(yoNamePrefix + "SupportSide", registry, RobotSide.class, true);

   private final DoubleYoVariable initialTime = new DoubleYoVariable(yoNamePrefix + "InitialTime", registry);
   private final DoubleYoVariable timeInCurrentState = new DoubleYoVariable(yoNamePrefix + "TimeInCurrentState", registry);
   private final DoubleYoVariable timeRemainingInState = new DoubleYoVariable(yoNamePrefix + "TimeRemainingInState", registry);
   private final DoubleYoVariable speedUpTime = new DoubleYoVariable(yoNamePrefix + "SpeedUpTime", registry);
   private final DoubleYoVariable minimumTimeRemaining = new DoubleYoVariable(yoNamePrefix + "MinimumTimeRemaining", registry);

   private final FramePoint2d currentICP = new FramePoint2d();
   private final FramePoint2d desiredICP = new FramePoint2d();
   private final FrameVector2d desiredICPVelocity = new FrameVector2d();
   private final FramePoint2d referenceCMP = new FramePoint2d();

   private final YoFramePoint2d controllerFeedbackCMP = new YoFramePoint2d(yoNamePrefix + "FeedbackCMP", worldFrame, registry);
   private final YoFrameVector2d controllerFeedbackCMPDelta = new YoFrameVector2d(yoNamePrefix + "FeedbackCMPDelta", worldFrame, registry);
   private final YoFrameVector2d icpError = new YoFrameVector2d("icpError", "", worldFrame, registry);

   private final YoFramePoint2d yoReconstructedICP = new YoFramePoint2d("reconstructedICP", "", worldFrame, registry);
   private final YoFrameVector2d reconstructedICPError = new YoFrameVector2d("reconstructedICPError", "", worldFrame, registry);
   private final YoFrameVector2d reconstructedCMPDelta = new YoFrameVector2d("reconstructedCMPDelta", "", worldFrame, registry);
   private final YoFrameVector2d modifiedReconstructedICPError = new YoFrameVector2d("modifiedReconstructedICPError", "", worldFrame, registry);
   private final YoFramePoint2d dynamicRelaxation = new YoFramePoint2d("dynamicRelaxation", "", worldFrame, registry);

   private final FramePoint2d finalICPRecursion = new FramePoint2d();
   private final FramePoint2d cmpConstantEffects = new FramePoint2d();

   private final YoFramePoint2d beginningOfStateICP = new YoFramePoint2d(yoNamePrefix + "BeginningOfStateICP", worldFrame, registry);
   private final YoFrameVector2d beginningOfStateICPVelocity = new YoFrameVector2d(yoNamePrefix + "BeginningOfStateICPVelocity", worldFrame, registry);

   private final ArrayList<Footstep> upcomingFootsteps = new ArrayList<>();
   private final ArrayList<YoFramePoint2d> upcomingFootstepLocations = new ArrayList<>();
   private final ArrayList<YoFramePoint2d> footstepSolutions = new ArrayList<>();

   private final DoubleYoVariable forwardFootstepWeight = new DoubleYoVariable(yoNamePrefix + "ForwardFootstepWeight", registry);
   private final DoubleYoVariable lateralFootstepWeight = new DoubleYoVariable(yoNamePrefix + "LateralFootstepWeight", registry);
   private final YoFramePoint2d scaledFootstepWeights = new YoFramePoint2d(yoNamePrefix + "ScaledFootstepWeights", worldFrame, registry);

   private final DoubleYoVariable swingFeedbackForwardWeight = new DoubleYoVariable(yoNamePrefix + "SwingFeedbackForwardWeight", registry);
   private final DoubleYoVariable swingFeedbackLateralWeight = new DoubleYoVariable(yoNamePrefix + "SwingFeedbackLateralWeight", registry);
   private final DoubleYoVariable transferFeedbackForwardWeight = new DoubleYoVariable(yoNamePrefix + "TransferFeedbackForwardWeight", registry);
   private final DoubleYoVariable transferFeedbackLateralWeight = new DoubleYoVariable(yoNamePrefix + "TransferFeedbackLateralWeight", registry);
   private final YoFramePoint2d scaledFeedbackWeight = new YoFramePoint2d(yoNamePrefix + "ScaledFeedbackWeight", worldFrame, registry);

   private final DoubleYoVariable footstepRegularizationWeight = new DoubleYoVariable(yoNamePrefix + "FootstepRegularizationWeight", registry);
   private final DoubleYoVariable feedbackRegularizationWeight = new DoubleYoVariable(yoNamePrefix + "FeedbackRegularizationWeight", registry);
   private final DoubleYoVariable scaledFootstepRegularizationWeight = new DoubleYoVariable(yoNamePrefix + "ScaledFootstepRegularizationWeight", registry);
   private final DoubleYoVariable dynamicRelaxationWeight = new DoubleYoVariable(yoNamePrefix + "DynamicRelaxationWeight", registry);

   private final DoubleYoVariable feedbackOrthogonalGain = new DoubleYoVariable(yoNamePrefix + "FeedbackOrthogonalGain", registry);
   private final DoubleYoVariable feedbackParallelGain = new DoubleYoVariable(yoNamePrefix + "FeedbackParallelGain", registry);

   private final DoubleYoVariable remainingTimeToStopAdjusting = new DoubleYoVariable(yoNamePrefix + "RemainingTimeToStopAdjusting", registry);

   private final IntegerYoVariable numberOfIterations = new IntegerYoVariable(yoNamePrefix + "NumberOfIterations", registry);
   private final BooleanYoVariable hasNotConvergedInPast = new BooleanYoVariable(yoNamePrefix + "HasNotConvergedInPast", registry);
   private final IntegerYoVariable hasNotConvergedCounts = new IntegerYoVariable(yoNamePrefix + "HasNotConvergedCounts", registry);

   private final ICPOptimizationSolver solver;

   private final FootstepRecursionMultiplierCalculator footstepRecursionMultiplierCalculator;
   private final NewStateMultiplierCalculator stateMultiplierCalculator;

   private final ICPOptimizationCMPConstraintHandler cmpConstraintHandler;
   private final ICPOptimizationReachabilityConstraintHandler reachabilityConstraintHandler;
   private final ICPOptimizationSolutionHandler solutionHandler;
   private final ICPOptimizationInputHandler inputHandler;

   private final SideDependentList<? extends ContactablePlaneBody> contactableFeet;

   private final double controlDT;
   private final double dynamicRelaxationDoubleSupportWeightModifier;
   private final int maximumNumberOfFootstepsToConsider;

   private boolean localUseStepAdjustment;
   private boolean localScaleUpcomingStepWeights;

   public ICPOptimizationController(CapturePointPlannerParameters icpPlannerParameters, ICPOptimizationParameters icpOptimizationParameters,
         WalkingControllerParameters walkingControllerParameters, BipedSupportPolygons bipedSupportPolygons,
         SideDependentList<? extends ContactablePlaneBody> contactableFeet, double controlDT, YoVariableRegistry parentRegistry,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.contactableFeet = contactableFeet;
      this.controlDT = controlDT;

      maximumNumberOfFootstepsToConsider = icpOptimizationParameters.getMaximumNumberOfFootstepsToConsider();
      numberOfFootstepsToConsider.set(icpOptimizationParameters.numberOfFootstepsToConsider());

      initialDoubleSupportDuration.set(icpPlannerParameters.getDoubleSupportInitialTransferDuration());

      int totalVertices = 0;
      for (RobotSide robotSide : RobotSide.values)
         totalVertices += contactableFeet.get(robotSide).getTotalNumberOfContactPoints();

      solver = new ICPOptimizationSolver(icpOptimizationParameters, totalVertices, COMPUTE_COST_TO_GO);


      useStepAdjustment.set(icpOptimizationParameters.useStepAdjustment());

      useTwoCMPs = icpPlannerParameters.useTwoCMPsPerSupport();
      useInitialICP = icpOptimizationParameters.useICPFromBeginningOfState();
      useFootstepRegularization = icpOptimizationParameters.useFootstepRegularization();
      useFeedbackRegularization = icpOptimizationParameters.useFeedbackRegularization();

      scaleStepRegularizationWeightWithTime.set(icpOptimizationParameters.scaleStepRegularizationWeightWithTime());
      scaleFeedbackWeightWithGain.set(icpOptimizationParameters.scaleFeedbackWeightWithGain());
      scaleUpcomingStepWeights.set(icpOptimizationParameters.scaleUpcomingStepWeights());

      forwardFootstepWeight.set(icpOptimizationParameters.getForwardFootstepWeight());
      lateralFootstepWeight.set(icpOptimizationParameters.getLateralFootstepWeight());
      footstepRegularizationWeight.set(icpOptimizationParameters.getFootstepRegularizationWeight());
      swingFeedbackForwardWeight.set(icpOptimizationParameters.getSingleSupportFeedbackForwardWeight());
      swingFeedbackLateralWeight.set(icpOptimizationParameters.getSingleSupportFeedbackLateralWeight());
      transferFeedbackForwardWeight.set(icpOptimizationParameters.getDoubleSupportFeedbackForwardWeight());
      transferFeedbackLateralWeight.set(icpOptimizationParameters.getDoubleSupportFeedbackLateralWeight());
      feedbackRegularizationWeight.set(icpOptimizationParameters.getFeedbackRegularizationWeight());
      feedbackOrthogonalGain.set(icpOptimizationParameters.getFeedbackOrthogonalGain());
      feedbackParallelGain.set(icpOptimizationParameters.getFeedbackParallelGain());
      dynamicRelaxationWeight.set(icpOptimizationParameters.getDynamicRelaxationWeight());

      minimumTimeRemaining.set(icpOptimizationParameters.getMinimumTimeRemaining());

      remainingTimeToStopAdjusting.set(icpOptimizationParameters.getRemainingTimeToStopAdjusting());
      if (walkingControllerParameters != null)
         swingSpeedUpEnabled.set(walkingControllerParameters.allowDisturbanceRecoveryBySpeedingUpSwing());
      else
         swingSpeedUpEnabled.set(false);

      dynamicRelaxationDoubleSupportWeightModifier = icpOptimizationParameters.getDynamicRelaxationDoubleSupportWeightModifier();

      DoubleYoVariable exitCMPDurationInPercentOfStepTime = new DoubleYoVariable(yoNamePrefix + "TimeSpentOnExitCMPInPercentOfStepTime", registry);
      DoubleYoVariable doubleSupportSplitFraction = new DoubleYoVariable(yoNamePrefix + "DoubleSupportSplitFraction", registry);

      exitCMPDurationInPercentOfStepTime.set(icpPlannerParameters.getTimeSpentOnExitCMPInPercentOfStepTime());
      doubleSupportSplitFraction.set(icpPlannerParameters.getDoubleSupportSplitFraction());

      if (USE_NEW_MULTIPLIER_CALCULATOR)
      {
         stateMultiplierCalculator = new NewStateMultiplierCalculator(icpPlannerParameters, exitCMPDurationInPercentOfStepTime, doubleSupportSplitFraction,
               doubleSupportSplitFraction, maximumNumberOfFootstepsToConsider, registry);
         footstepRecursionMultiplierCalculator = null;
      }
      else
      {
         footstepRecursionMultiplierCalculator = new FootstepRecursionMultiplierCalculator(icpPlannerParameters, exitCMPDurationInPercentOfStepTime,
               doubleSupportSplitFraction, maximumNumberOfFootstepsToConsider, registry);
         stateMultiplierCalculator = null;
      }

      cmpConstraintHandler = new ICPOptimizationCMPConstraintHandler(bipedSupportPolygons, icpOptimizationParameters, registry);
      reachabilityConstraintHandler = new ICPOptimizationReachabilityConstraintHandler(bipedSupportPolygons, icpOptimizationParameters, registry);
      solutionHandler = new ICPOptimizationSolutionHandler(icpOptimizationParameters, footstepRecursionMultiplierCalculator, stateMultiplierCalculator, VISUALIZE,
            USE_NEW_MULTIPLIER_CALCULATOR, registry, yoGraphicsListRegistry);
      inputHandler = new ICPOptimizationInputHandler(icpPlannerParameters, bipedSupportPolygons, contactableFeet, maximumNumberOfFootstepsToConsider,
            footstepRecursionMultiplierCalculator, stateMultiplierCalculator, doubleSupportDuration, singleSupportDuration, exitCMPDurationInPercentOfStepTime,
            doubleSupportSplitFraction, VISUALIZE, USE_NEW_MULTIPLIER_CALCULATOR, registry, yoGraphicsListRegistry);

      for (int i = 0; i < maximumNumberOfFootstepsToConsider; i++)
      {
         upcomingFootstepLocations.add(new YoFramePoint2d("upcomingFootstepLocation" + i, worldFrame, registry));
         footstepSolutions.add(new YoFramePoint2d("footstepSolutionLocation" + i, worldFrame, registry));
      }

      parentRegistry.addChild(registry);

      if (VISUALIZE)
      {
         String name = "ICPOptimization";
         YoGraphicPosition referenceICP = new YoGraphicPosition("controllerReconstructedICP", yoReconstructedICP, 0.02, YoAppearance.Red(), GraphicType.BALL_WITH_ROTATED_CROSS);
         yoGraphicsListRegistry.registerArtifact(name, referenceICP.createArtifact());
         yoGraphicsListRegistry.registerYoGraphic(name, referenceICP);
      }
   }

   public void setStepDurations(double doubleSupportDuration, double singleSupportDuration)
   {
      setDoubleSupportDuration(doubleSupportDuration);
      setSingleSupportDuration(singleSupportDuration);
   }

   public void setDoubleSupportDuration(double doubleSupportDuration)
   {
      this.doubleSupportDuration.set(doubleSupportDuration);
   }

   public void setSingleSupportDuration(double singleSupportDuration)
   {
      this.singleSupportDuration.set(singleSupportDuration);
   }

   public void clearPlan()
   {
      upcomingFootsteps.clear();

      if (!USE_NEW_MULTIPLIER_CALCULATOR)
         footstepRecursionMultiplierCalculator.reset();

      inputHandler.clearPlan();
      for (int i = 0; i < maximumNumberOfFootstepsToConsider; i++)
         upcomingFootstepLocations.get(i).setToZero();
   }

   private final FramePoint2d tmpFramePoint2d = new FramePoint2d();
   public void addFootstepToPlan(Footstep footstep)
   {
      if (footstep != null)
      {
         upcomingFootsteps.add(footstep);
         footstep.getPosition2d(tmpFramePoint2d);
         upcomingFootstepLocations.get(upcomingFootsteps.size() - 1).set(tmpFramePoint2d);
         inputHandler.addFootstepToPlan(footstep);

         footstepSolutions.get(upcomingFootsteps.size() - 1).set(tmpFramePoint2d);
      }
   }

   public void submitRemainingTimeInSwingUnderDisturbance(double remainingTimeForSwing)
   {
      if (swingSpeedUpEnabled.getBooleanValue() && remainingTimeForSwing < timeRemainingInState.getDoubleValue())
      {
         double speedUpTime = timeRemainingInState.getDoubleValue() - remainingTimeForSwing;
         this.speedUpTime.add(speedUpTime);
      }
   }


   public void initializeForStanding(double initialTime)
   {
      this.initialTime.set(initialTime);
      isStanding.set(true);
      isInTransfer.set(false);
      isInitialTransfer.set(true);

      setProblemBooleans();

      if (USE_NEW_MULTIPLIER_CALCULATOR)
         stateMultiplierCalculator.resetTimes();
      else
         footstepRecursionMultiplierCalculator.resetTimes();

      cmpConstraintHandler.updateCMPConstraintForDoubleSupport(solver);
      reachabilityConstraintHandler.updateReachabilityConstraintForDoubleSupport(solver);

      speedUpTime.set(0.0);
   }

   public void initializeForTransfer(double initialTime, RobotSide transferToSide, double omega0)
   {
      this.transferToSide.set(transferToSide);
      if (transferToSide == null)
         transferToSide = RobotSide.LEFT;
      isInTransfer.set(true);

      int numberOfFootstepsToConsider = initializeOnContactChange(initialTime);

      if (USE_NEW_MULTIPLIER_CALCULATOR)
      {
         stateMultiplierCalculator.resetTimes();
         if (isInitialTransfer.getBooleanValue())
            stateMultiplierCalculator.submitTimes(0, initialDoubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());
         else
            stateMultiplierCalculator.submitTimes(0, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         for (int i = 1; i < numberOfFootstepsToConsider + 1; i++)
            stateMultiplierCalculator.submitTimes(i, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         stateMultiplierCalculator.computeRecursionMultipliers(numberOfFootstepsToConsider, isInTransfer.getBooleanValue(), useTwoCMPs, omega0);
      }
      else
      {
         footstepRecursionMultiplierCalculator.resetTimes();
         if (isInitialTransfer.getBooleanValue())
            footstepRecursionMultiplierCalculator.submitTimes(0, initialDoubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());
         else
            footstepRecursionMultiplierCalculator.submitTimes(0, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         for (int i = 1; i < numberOfFootstepsToConsider + 1; i++)
            footstepRecursionMultiplierCalculator.submitTimes(i, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         footstepRecursionMultiplierCalculator.computeRecursionMultipliers(numberOfFootstepsToConsider, isInTransfer.getBooleanValue(), useTwoCMPs, omega0);
      }

      inputHandler.initializeForDoubleSupport(isStanding.getBooleanValue(), useTwoCMPs, transferToSide, omega0);

      cmpConstraintHandler.updateCMPConstraintForDoubleSupport(solver);
      reachabilityConstraintHandler.updateReachabilityConstraintForDoubleSupport(solver);
   }

   /**
    * Notifies the icp optimization controller that teh robot is now in single support
    * @param initialTime controller time at the start of the current single support phase
    * @param supportSide sets the side of the support foot for the upcoming single support
    * @param omega0 current inverted pendulum natural frequency
    */
   public void initializeForSingleSupport(double initialTime, RobotSide supportSide, double omega0)
   {
      this.supportSide.set(supportSide);
      isStanding.set(false);
      isInTransfer.set(false);
      isInitialTransfer.set(false);

      int numberOfFootstepsToConsider = initializeOnContactChange(initialTime);

      if (USE_NEW_MULTIPLIER_CALCULATOR)
      {
         stateMultiplierCalculator.resetTimes();
         stateMultiplierCalculator.submitTimes(0, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         for (int i = 1; i < numberOfFootstepsToConsider + 1; i++)
            stateMultiplierCalculator.submitTimes(i, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());
         stateMultiplierCalculator.submitTimes(numberOfFootstepsToConsider + 1, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         stateMultiplierCalculator.computeRecursionMultipliers(numberOfFootstepsToConsider, isInTransfer.getBooleanValue(), useTwoCMPs, omega0);
      }
      else
      {
         footstepRecursionMultiplierCalculator.resetTimes();
         footstepRecursionMultiplierCalculator.submitTimes(0, 0.0, singleSupportDuration.getDoubleValue());

         for (int i = 1; i < numberOfFootstepsToConsider + 1; i++)
            footstepRecursionMultiplierCalculator.submitTimes(i, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());
         footstepRecursionMultiplierCalculator.submitTimes(numberOfFootstepsToConsider + 1, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

         footstepRecursionMultiplierCalculator.computeRecursionMultipliers(numberOfFootstepsToConsider, isInTransfer.getBooleanValue(), useTwoCMPs, omega0);
      }

      inputHandler.initializeForSingleSupport(useTwoCMPs, supportSide, omega0);

      cmpConstraintHandler.updateCMPConstraintForSingleSupport(supportSide, solver);
      reachabilityConstraintHandler.updateReachabilityConstraintForSingleSupport(supportSide, solver);
   }

   private int initializeOnContactChange(double initialTime)
   {
      int numberOfFootstepsToConsider = clipNumberOfFootstepsToConsiderToProblem(this.numberOfFootstepsToConsider.getIntegerValue());

      this.initialTime.set(initialTime);
      speedUpTime.set(0.0);

      setProblemBooleans();

      beginningOfStateICP.set(solutionHandler.getControllerReferenceICP());
      beginningOfStateICPVelocity.set(solutionHandler.getControllerReferenceICPVelocity());

      if (useFootstepRegularization)
         resetFootstepRegularizationTask();
      if (useFeedbackRegularization)
         solver.resetFeedbackRegularization();

      return numberOfFootstepsToConsider;
   }

   private void setProblemBooleans()
   {
      localUseStepAdjustment = useStepAdjustment.getBooleanValue();
      localScaleUpcomingStepWeights = scaleUpcomingStepWeights.getBooleanValue();
   }

   private final FramePoint2d dynRelax = new FramePoint2d();
   private final FramePoint2d reconstructedICP = new FramePoint2d();
   private final FramePoint2d desiredCMP = new FramePoint2d();
   private final FrameVector2d desiredCMPDelta = new FrameVector2d();

   private final boolean useReconstruction = false;

   public void compute(double currentTime, FramePoint2d desiredICP, FrameVector2d desiredICPVelocity, FramePoint2d currentICP, double omega0)
   {
      desiredICP.changeFrame(worldFrame);
      desiredICPVelocity.changeFrame(worldFrame);
      currentICP.changeFrame(worldFrame);

      this.currentICP.set(currentICP);
      this.desiredICP.set(desiredICP);
      this.desiredICPVelocity.set(desiredICPVelocity);

      solutionHandler.getControllerReferenceCMP(referenceCMP);

      computeTimeInCurrentState(currentTime);
      computeTimeRemainingInState();

      int numberOfFootstepsToConsider = checkForEndingOfAdjustment(omega0);

      scaleStepRegularizationWeightWithTime();
      scaleFeedbackWeightWithGain();

      if (isStanding.getBooleanValue())
         setConditionsForFeedbackOnlyControl();
      else
         setConditionsForSteppingControl(numberOfFootstepsToConsider, omega0);

      NoConvergenceException noConvergenceException = null;
      try
      {
         solver.compute(finalICPRecursion, cmpConstantEffects, currentICP, referenceCMP);
      }
      catch (NoConvergenceException e)
      {
         if (!hasNotConvergedInPast.getBooleanValue())
         {
            e.printStackTrace();
            PrintTools.warn(this, "Only showing the stack trace of the first " + e.getClass().getSimpleName() + ". This may be happening more than once.");
         }

         hasNotConvergedInPast.set(true);
         hasNotConvergedCounts.increment();

         noConvergenceException = e;
      }

      // don't pole the new solutions if there's a no convergence exception
      if (noConvergenceException == null)
      {
         numberOfIterations.set(solver.getNumberOfIterations());

         if (localUseStepAdjustment)
            solutionHandler.extractFootstepSolutions(footstepSolutions, upcomingFootstepLocations, upcomingFootsteps, numberOfFootstepsToConsider, solver);

         solver.getCMPFeedbackDifference(desiredCMPDelta);

         if (COMPUTE_COST_TO_GO)
            solutionHandler.updateCostsToGo(solver);
      }

      if (isStanding.getBooleanValue())
      {
         solutionHandler.setValuesForFeedbackOnly(desiredICP, desiredICPVelocity, omega0);
      }
      else
      {
         solutionHandler.computeReferenceFromSolutions(footstepSolutions, inputHandler, beginningOfStateICP, beginningOfStateICPVelocity, omega0, numberOfFootstepsToConsider);
         solutionHandler.computeNominalValues(upcomingFootstepLocations, inputHandler, beginningOfStateICP, beginningOfStateICPVelocity, omega0, numberOfFootstepsToConsider);
      }

      solutionHandler.getControllerReferenceCMP(desiredCMP);
      solver.getReconstructedReferenceICPPosition(reconstructedICP);
      solver.getDynamicRelaxation(dynRelax);

      yoReconstructedICP.set(reconstructedICP);

      icpError.set(currentICP);
      icpError.sub(solutionHandler.getControllerReferenceICP());

      reconstructedICPError.set(currentICP);
      reconstructedICPError.sub(reconstructedICP);

      reconstructedCMPDelta.setX(feedbackGains.getX() * reconstructedICPError.getX());
      reconstructedCMPDelta.setY(feedbackGains.getY() * reconstructedICPError.getY());


      modifiedReconstructedICPError.set(reconstructedICPError);
      modifiedReconstructedICPError.add(dynRelax);

      dynamicRelaxation.set(dynRelax);


      if (useReconstruction)
      {
         desiredCMP.add(reconstructedCMPDelta.getFrameTuple2d());
         controllerFeedbackCMPDelta.set(reconstructedCMPDelta);
      }
      else
      {
         desiredCMP.add(desiredCMPDelta);
         controllerFeedbackCMPDelta.set(desiredCMPDelta);
      }
      controllerFeedbackCMP.set(desiredCMP);
   }

   private void setConditionsForFeedbackOnlyControl()
   {
      solver.resetFootstepConditions();

      setFeedbackConditions();

      finalICPRecursion.set(desiredICP);
      cmpConstantEffects.setToZero();
   }

   private int setConditionsForSteppingControl(int numberOfFootstepsToConsider, double omega0)
   {
      if (isInTransfer.getBooleanValue())
      {
         cmpConstraintHandler.updateCMPConstraintForDoubleSupport(solver);
         reachabilityConstraintHandler.updateReachabilityConstraintForDoubleSupport(solver);
      }
      else
      {
         cmpConstraintHandler.updateCMPConstraintForSingleSupport(supportSide.getEnumValue(), solver);
         reachabilityConstraintHandler.updateReachabilityConstraintForSingleSupport(supportSide.getEnumValue(), solver);
      }

      solver.resetFootstepConditions();

      if (useFeedbackRegularization)
         solver.setFeedbackRegularizationWeight(feedbackRegularizationWeight.getDoubleValue() / controlDT);

      if (localUseStepAdjustment && !isInTransfer.getBooleanValue())
      {
         for (int footstepIndex = 0; footstepIndex < numberOfFootstepsToConsider; footstepIndex++)
            submitFootstepConditionsToSolver(footstepIndex);

         if (useFootstepRegularization)
            solver.setFootstepRegularizationWeight(scaledFootstepRegularizationWeight.getDoubleValue() / controlDT);
      }

      setFeedbackConditions();

      double clippedTimeRemaining = Math.max(minimumTimeRemaining.getDoubleValue(), timeRemainingInState.getDoubleValue());

      inputHandler.update(useTwoCMPs, omega0);
      inputHandler.computeFinalICPRecursion(finalICPRecursion, numberOfFootstepsToConsider, useTwoCMPs, isInTransfer.getBooleanValue(), omega0);
      inputHandler.computeCMPConstantEffects(cmpConstantEffects, beginningOfStateICP.getFrameTuple2d(), beginningOfStateICPVelocity.getFrameTuple2d(),
            upcomingFootstepLocations, clippedTimeRemaining, timeInCurrentState.getDoubleValue(), omega0, numberOfFootstepsToConsider, useTwoCMPs, isInTransfer.getBooleanValue(), useInitialICP);

      return numberOfFootstepsToConsider;
   }

   private final FrameVector2d feedbackGains = new FrameVector2d();
   private void setFeedbackConditions()
   {
      ICPOptimizationControllerHelper.transformFeedbackGains(feedbackGains, desiredICPVelocity, feedbackParallelGain, feedbackOrthogonalGain);

      double dynamicRelaxationWeight = this.dynamicRelaxationWeight.getDoubleValue();
      if (!localUseStepAdjustment)
         dynamicRelaxationWeight = dynamicRelaxationWeight / dynamicRelaxationDoubleSupportWeightModifier;

      solver.resetFeedbackConditions();
      solver.setFeedbackConditions(scaledFeedbackWeight.getX(), scaledFeedbackWeight.getY(), feedbackGains.getX(), feedbackGains.getY(), dynamicRelaxationWeight);
   }

   private final FramePose footstepPose = new FramePose();
   private int checkForEndingOfAdjustment(double omega0)
   {
      int numberOfFootstepsToConsider = clipNumberOfFootstepsToConsiderToProblem(this.numberOfFootstepsToConsider.getIntegerValue());

      if (timeRemainingInState.getDoubleValue() < remainingTimeToStopAdjusting.getDoubleValue() && localUseStepAdjustment && !isStanding.getBooleanValue() && !isInTransfer.getBooleanValue())
      {
         //record current locations
         for (int i = 0; i < numberOfFootstepsToConsider; i++)
         {
            upcomingFootstepLocations.get(i).set(footstepSolutions.get(i));

            Footstep footstep = upcomingFootsteps.get(i);
            footstep.getPose(footstepPose);
            footstepPose.setXYFromPosition2d(footstepSolutions.get(i).getFrameTuple2d());
            footstep.setPose(footstepPose);

            inputHandler.addFootstepToPlan(footstep);
         }

         localUseStepAdjustment = false;

         numberOfFootstepsToConsider = clipNumberOfFootstepsToConsiderToProblem(this.numberOfFootstepsToConsider.getIntegerValue());

         if (USE_NEW_MULTIPLIER_CALCULATOR)
         {
            stateMultiplierCalculator.resetTimes();
            stateMultiplierCalculator.submitTimes(0, 0.0, singleSupportDuration.getDoubleValue());

            for (int i = 1; i < numberOfFootstepsToConsider + 1; i++)
               stateMultiplierCalculator.submitTimes(i, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());
            stateMultiplierCalculator.submitTimes(numberOfFootstepsToConsider + 1, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

            stateMultiplierCalculator.computeRecursionMultipliers(numberOfFootstepsToConsider, isInTransfer.getBooleanValue(), useTwoCMPs, omega0);
         }
         else
         {
            footstepRecursionMultiplierCalculator.resetTimes();
            footstepRecursionMultiplierCalculator.submitTimes(0, 0.0, singleSupportDuration.getDoubleValue());

            for (int i = 1; i < numberOfFootstepsToConsider + 1; i++)
               footstepRecursionMultiplierCalculator.submitTimes(i, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());
            footstepRecursionMultiplierCalculator.submitTimes(numberOfFootstepsToConsider + 1, doubleSupportDuration.getDoubleValue(), singleSupportDuration.getDoubleValue());

            footstepRecursionMultiplierCalculator.computeRecursionMultipliers(numberOfFootstepsToConsider, isInTransfer.getBooleanValue(), useTwoCMPs, omega0);
         }
      }

      return numberOfFootstepsToConsider;
   }

   private void resetFootstepRegularizationTask()
   {
      int numberOfFootstepsToConsider = clipNumberOfFootstepsToConsiderToProblem(this.numberOfFootstepsToConsider.getIntegerValue());

      for (int i = 0; i < numberOfFootstepsToConsider; i++)
         solver.resetFootstepRegularization(i, upcomingFootstepLocations.get(i).getFrameTuple2d());
   }

   private int clipNumberOfFootstepsToConsiderToProblem(int numberOfFootstepsToConsider)
   {
      numberOfFootstepsToConsider = Math.min(numberOfFootstepsToConsider, upcomingFootsteps.size());
      numberOfFootstepsToConsider = Math.min(numberOfFootstepsToConsider, maximumNumberOfFootstepsToConsider);

      if (!localUseStepAdjustment || isInTransfer.getBooleanValue() || isStanding.getBooleanValue())
         numberOfFootstepsToConsider = 0;

      return numberOfFootstepsToConsider;
   }

   private final FrameVector2d footstepWeights = new FrameVector2d();
   private void submitFootstepConditionsToSolver(int footstepIndex)
   {
      ReferenceFrame soleFrame = contactableFeet.get(supportSide.getEnumValue()).getSoleFrame();
      ICPOptimizationControllerHelper.transformWeightsToWorldFrame(footstepWeights, forwardFootstepWeight, lateralFootstepWeight, soleFrame);
      scaledFootstepWeights.set(footstepWeights);

      if (localScaleUpcomingStepWeights)
         scaledFootstepWeights.scale(1.0 / (footstepIndex + 1));

      double footstepRecursionMultiplier;
      if (USE_NEW_MULTIPLIER_CALCULATOR)
      {
         if (useTwoCMPs)
         {
            double entryMutliplier = stateMultiplierCalculator.getEntryCMPRecursionMultiplier(footstepIndex);
            double exitMutliplier = stateMultiplierCalculator.getExitCMPRecursionMultiplier(footstepIndex);

            footstepRecursionMultiplier = entryMutliplier + exitMutliplier;
         }
         else
         {
            footstepRecursionMultiplier = stateMultiplierCalculator.getExitCMPRecursionMultiplier(footstepIndex);
         }
         footstepRecursionMultiplier *= stateMultiplierCalculator.getStateEndCurrentMultiplier();
      }
      else
      {
         if (useTwoCMPs)
         {
            double entryMutliplier = footstepRecursionMultiplierCalculator.getCMPRecursionEntryMultiplier(footstepIndex);
            double exitMutliplier = footstepRecursionMultiplierCalculator.getCMPRecursionExitMultiplier(footstepIndex);

            footstepRecursionMultiplier = entryMutliplier + exitMutliplier;
         }
         else
         {
            footstepRecursionMultiplier = footstepRecursionMultiplierCalculator.getCMPRecursionExitMultiplier(footstepIndex);
         }
         footstepRecursionMultiplier *= footstepRecursionMultiplierCalculator.getCurrentStateProjectionMultiplier();
      }

      solver.setFootstepAdjustmentConditions(footstepIndex, footstepRecursionMultiplier, scaledFootstepWeights.getX(), scaledFootstepWeights.getY(),
            upcomingFootstepLocations.get(footstepIndex).getFrameTuple2d());
   }


   private void computeTimeInCurrentState(double currentTime)
   {
      timeInCurrentState.set(currentTime - initialTime.getDoubleValue() + speedUpTime.getDoubleValue());
   }

   private void computeTimeRemainingInState()
   {
      if (isStanding.getBooleanValue())
      {
         timeRemainingInState.set(0.0);
      }
      else
      {
         double remainingTime;
         if (isInTransfer.getBooleanValue())
            remainingTime = doubleSupportDuration.getDoubleValue() - timeInCurrentState.getDoubleValue();
         else
            remainingTime = singleSupportDuration.getDoubleValue() - timeInCurrentState.getDoubleValue();

         timeRemainingInState.set(remainingTime);
      }
   }

   private void scaleStepRegularizationWeightWithTime()
   {
      if (scaleStepRegularizationWeightWithTime.getBooleanValue())
      {
         double alpha = Math.max(timeRemainingInState.getDoubleValue(), minimumTimeRemaining.getDoubleValue()) / singleSupportDuration.getDoubleValue();
         scaledFootstepRegularizationWeight.set(footstepRegularizationWeight.getDoubleValue() / alpha);
      }
      else
      {
         scaledFootstepRegularizationWeight.set(footstepRegularizationWeight.getDoubleValue());
      }
   }

   private final FrameVector2d feedbackWeights = new FrameVector2d();
   private void scaleFeedbackWeightWithGain()
   {
      ReferenceFrame soleFrame = contactableFeet.get(supportSide.getEnumValue()).getSoleFrame();

      if (isInTransfer.getBooleanValue())
         ICPOptimizationControllerHelper.transformWeightsToWorldFrame(feedbackWeights, transferFeedbackForwardWeight, transferFeedbackLateralWeight, soleFrame);
      else
         ICPOptimizationControllerHelper.transformWeightsToWorldFrame(feedbackWeights, swingFeedbackForwardWeight, swingFeedbackLateralWeight, soleFrame);

      scaledFeedbackWeight.set(feedbackWeights);

      if (scaleFeedbackWeightWithGain.getBooleanValue())
      {
         ICPOptimizationControllerHelper.transformFeedbackGains(feedbackGains, desiredICPVelocity, feedbackParallelGain, feedbackOrthogonalGain);

         double alpha = Math.sqrt(Math.pow(feedbackGains.getX(), 2) + Math.pow(feedbackGains.getY(), 2));
         scaledFeedbackWeight.scale(1.0 / alpha);
      }
   }

   public int getNumberOfFootstepsToConsider()
   {
      return numberOfFootstepsToConsider.getIntegerValue();
   }

   public void getDesiredCMP(FramePoint2d desiredCMPToPack)
   {
      controllerFeedbackCMP.getFrameTuple2d(desiredCMPToPack);
   }

   public void getFootstepSolution(int footstepIndex, FramePoint2d footstepSolutionToPack)
   {
      footstepSolutions.get(footstepIndex).getFrameTuple2d(footstepSolutionToPack);
   }

   public boolean wasFootstepAdjusted()
   {
      return solutionHandler.wasFootstepAdjusted();
   }
}
