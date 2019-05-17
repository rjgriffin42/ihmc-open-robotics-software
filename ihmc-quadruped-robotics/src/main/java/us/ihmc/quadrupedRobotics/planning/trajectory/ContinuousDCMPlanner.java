package us.ihmc.quadrupedRobotics.planning.trajectory;

import us.ihmc.commonWalkingControlModules.capturePoint.CapturePointTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsList;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.ArtifactList;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.quadrupedBasics.gait.QuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.ContactState;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedContactSequence;
import us.ihmc.robotics.math.trajectories.YoFrameTrajectory3D;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.*;

import java.util.ArrayList;
import java.util.List;

public class ContinuousDCMPlanner implements DCMPlannerInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private static final boolean VISUALIZE = true;
   private static final double POINT_SIZE = 0.005;

   private static final int STEP_SEQUENCE_CAPACITY = 50;

   private final QuadrupedPiecewiseConstantCopTrajectory piecewiseConstantCopTrajectory;
   private final PiecewiseReverseDcmTrajectory dcmTrajectory;
   private final YoFrameTrajectory3D dcmFirstSpline;
   private final YoFrameTrajectory3D dcmSecondSpline;

   private final DoubleParameter initialTransitionDuration = new DoubleParameter("initialTransitionDuration", registry, 0.5);
   private final DoubleParameter minimumSplineDuration = new DoubleParameter("minimumSplineDuration", registry, 0.1);
   private final DoubleParameter splineSplitFraction = new DoubleParameter("splineSplitFraction", registry, 0.25);

   private final QuadrupedTimedContactSequence timedContactSequence = new QuadrupedTimedContactSequence(2 * STEP_SEQUENCE_CAPACITY);
   private final List<QuadrupedTimedStep> stepSequence = new ArrayList<>();

   private final QuadrantDependentList<MovingReferenceFrame> soleFrames;

   private final YoDouble controllerTime;
   private final YoDouble comHeight = new YoDouble("comHeightForPlanning", registry);
   private final YoInteger numberOfStepsInPlanner = new YoInteger("numberOfStepsInPlanner", registry);
   private final YoFramePoint3D perfectCMPPosition = new YoFramePoint3D("perfectCMPPosition", worldFrame, registry);

   private final YoBoolean isStanding = new YoBoolean("isStanding", registry);
   private final YoBoolean isUsingSpline = new YoBoolean("isUsingSpline", registry);
   private final YoBoolean isInitialTransfer = new YoBoolean("isInitialTransfer", registry);
   private final YoDouble splineStartTime = new YoDouble("splineStartTime", registry);
   private final YoDouble splineEndTime = new YoDouble("splineEndTime", registry);

   private final ReferenceFrame supportFrame;
   private final YoFramePoint3D dcmPositionAtStartOfState = new YoFramePoint3D("dcmPositionAtStartOfState", worldFrame, registry);
   private final YoFrameVector3D dcmVelocityAtStartOfState = new YoFrameVector3D("dcmVelocityAtStartOfState", worldFrame, registry);
   private final YoFramePoint3D dcmPositionAtEndOfTransition = new YoFramePoint3D("dcmPositionAtEndOfTransition", worldFrame, registry);
   private final YoFrameVector3D dcmVelocityAtEndOfTransition = new YoFrameVector3D("dcmVelocityAtEndOfTransition", worldFrame, registry);
   private final YoFramePoint3D dcmPositionAtStartOfSpline = new YoFramePoint3D("dcmPositionAtStartOfSpline", worldFrame, registry);
   private final YoFrameVector3D dcmVelocityAtStartOfSpline = new YoFrameVector3D("dcmVelocityAtStartOfSpline", worldFrame, registry);
   private final YoFramePoint3D dcmPositionAtEndOfSpline = new YoFramePoint3D("dcmPositionAtEndOfSpline", worldFrame, registry);
   private final YoFrameVector3D dcmVelocityAtEndOfSpline = new YoFrameVector3D("dcmVelocityAtEndOfSpline", worldFrame, registry);
   private final YoDouble timeAtStartOfState = new YoDouble("timeAtStartOfState", registry);
   private final YoDouble timeInState = new YoDouble("timeInState", registry);

   private final FramePoint3D initialTransitionDCMPosition = new FramePoint3D();
   private final FrameVector3D initialTransitionDCMVelocity = new FrameVector3D();
   private final FramePoint3D finalTransitionDCMPosition = new FramePoint3D();
   private final FrameVector3D finalTransitionDCMVelocity = new FrameVector3D();

   private final FramePoint3D initialSplineDCMPosition = new FramePoint3D();
   private final FrameVector3D initialSplineDCMVelocity = new FrameVector3D();
   private final FramePoint3D finalSplineDCMPosition = new FramePoint3D();
   private final FrameVector3D finalSplineDCMVelocity = new FrameVector3D();

   private final FramePoint3D finalDCM = new FramePoint3D();

   private final FramePoint3D tempPoint = new FramePoint3D();

   private final boolean debug;

   public ContinuousDCMPlanner(DCMPlannerParameters dcmPlannerParameters, double gravity, double nominalHeight, YoDouble robotTimestamp, ReferenceFrame supportFrame,
                               QuadrantDependentList<MovingReferenceFrame> soleFrames, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this(dcmPlannerParameters, gravity, nominalHeight, robotTimestamp, supportFrame, soleFrames, parentRegistry, yoGraphicsListRegistry, false);
   }

   public ContinuousDCMPlanner(DCMPlannerParameters dcmPlannerParameters, double gravity, double nominalHeight, YoDouble robotTimestamp, ReferenceFrame supportFrame,
                               QuadrantDependentList<MovingReferenceFrame> soleFrames, YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry,
                               boolean debug)

   {
      this.controllerTime = robotTimestamp;
      this.supportFrame = supportFrame;
      this.soleFrames = soleFrames;
      this.debug = debug;
      this.dcmFirstSpline = new YoFrameTrajectory3D("dcmFirstSpline", 4, supportFrame, registry);
      this.dcmSecondSpline = new YoFrameTrajectory3D("dcmSecondSpline", 4, supportFrame, registry);

      DCMPlannerParameters yoDcmPlannerParameters = new YoDCMPlannerParameters(dcmPlannerParameters, registry);
      dcmTrajectory = new PiecewiseReverseDcmTrajectory(STEP_SEQUENCE_CAPACITY, gravity, nominalHeight, registry);
      piecewiseConstantCopTrajectory = new QuadrupedPiecewiseConstantCopTrajectory(2 * STEP_SEQUENCE_CAPACITY, yoDcmPlannerParameters, registry);

      parentRegistry.addChild(registry);

      if (yoGraphicsListRegistry != null)
         setupVisualizers(yoGraphicsListRegistry);
   }

   private void setupVisualizers(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      YoGraphicsList yoGraphicsList = new YoGraphicsList(getClass().getSimpleName());
      ArtifactList artifactList = new ArtifactList(getClass().getSimpleName());

      piecewiseConstantCopTrajectory.setupVisualizers(yoGraphicsList, artifactList, POINT_SIZE);
      dcmTrajectory.setupVisualizers(yoGraphicsListRegistry, POINT_SIZE);

      YoGraphicPosition perfectCMPPositionViz = new YoGraphicPosition("Perfect CMP Position", perfectCMPPosition, 0.002, YoAppearance.BlueViolet());

      artifactList.setVisible(VISUALIZE);
      yoGraphicsList.setVisible(VISUALIZE);

      yoGraphicsListRegistry.registerYoGraphic("dcmPlanner", perfectCMPPositionViz);
      yoGraphicsListRegistry.registerArtifact("dcmPlanner", perfectCMPPositionViz.createArtifact());

      yoGraphicsListRegistry.registerYoGraphicsList(yoGraphicsList);
      yoGraphicsListRegistry.registerArtifactList(artifactList);
   }

   public void clearStepSequence()
   {
      stepSequence.clear();
      numberOfStepsInPlanner.set(0);
   }

   public void setNominalCoMHeight(double comHeight)
   {
      this.comHeight.set(comHeight);
   }

   public void addStepToSequence(QuadrupedTimedStep step)
   {
      stepSequence.add(step);
      numberOfStepsInPlanner.increment();
   }

   public void initializeForStanding()
   {
      isStanding.set(true);
      timedContactSequence.clear();
      piecewiseConstantCopTrajectory.resetVariables();
      dcmTrajectory.resetVariables();
   }

   public void initializeForStepping(QuadrantDependentList<YoEnum<ContactState>> currentContactStates, FramePoint3DReadOnly currentDCMPosition,
                                     FrameVector3DReadOnly currentDCMVelocity)
   {
      isStanding.set(false);
      isInitialTransfer.set(true);

      double currentTime = controllerTime.getDoubleValue();
      boolean isCurrentPlanValid = stepSequence.get(numberOfStepsInPlanner.getIntegerValue() - 1).getTimeInterval().getEndTime() > currentTime;

      if (isCurrentPlanValid)
      {
         // compute dcm trajectory
         computeDcmTrajectory(currentContactStates);

         dcmPositionAtStartOfState.setMatchingFrame(currentDCMPosition);
         dcmVelocityAtStartOfState.setMatchingFrame(currentDCMVelocity);
         timeAtStartOfState.set(controllerTime.getDoubleValue());
         computeInitialTransitionTrajectory();
      }
   }

   public void beganStep()
   {
      dcmPositionAtStartOfState.setMatchingFrame(desiredDCMPosition);
      dcmVelocityAtStartOfState.setMatchingFrame(desiredDCMVelocity);
      timeAtStartOfState.set(controllerTime.getDoubleValue());
   }

   public void completedStep()
   {
      dcmPositionAtStartOfState.setMatchingFrame(desiredDCMPosition);
      dcmVelocityAtStartOfState.setMatchingFrame(desiredDCMVelocity);
      timeAtStartOfState.set(controllerTime.getDoubleValue());
      isInitialTransfer.set(false);
   }

   private void computeDcmTrajectory(QuadrantDependentList<YoEnum<ContactState>> currentContactStates)
   {
      // compute piecewise constant center of pressure plan
      double currentTime = controllerTime.getDoubleValue();
      timedContactSequence.update(stepSequence, soleFrames, currentContactStates, currentTime);
      piecewiseConstantCopTrajectory.initializeTrajectory(currentTime, timedContactSequence, stepSequence);

      // compute dcm trajectory with final boundary constraint
      int numberOfIntervals = piecewiseConstantCopTrajectory.getNumberOfIntervals();
      tempPoint.setIncludingFrame(piecewiseConstantCopTrajectory.getCopPositionAtStartOfInterval(numberOfIntervals - 1));
      tempPoint.changeFrame(ReferenceFrame.getWorldFrame());
      tempPoint.add(0, 0, comHeight.getDoubleValue());

      dcmTrajectory.setComHeight(comHeight.getDoubleValue());
      dcmTrajectory.initializeTrajectory(numberOfIntervals, piecewiseConstantCopTrajectory.getTimeAtStartOfInterval(),
                                         piecewiseConstantCopTrajectory.getCopPositionsAtStartOfInterval(),
                                         piecewiseConstantCopTrajectory.getTimeAtStartOfInterval(numberOfIntervals - 1), tempPoint);
   }

   private void computeInitialTransitionTrajectory()
   {
      double currentIntervalDuration = piecewiseConstantCopTrajectory.getTimeAtEndOfInterval(0) - timeAtStartOfState.getValue();
      double nextIntervalDuration = piecewiseConstantCopTrajectory.getIntervalDuration(1);

      double nextSplineTimeSpentOnEntryCMP = splineSplitFraction.getValue() * currentIntervalDuration;
      double nextSplineTimeSpentOnExitCMP = splineSplitFraction.getValue() * nextIntervalDuration;

      boolean initialPhaseIsLongEnough = currentIntervalDuration > initialTransitionDuration.getValue();

      double transitionStartTime = timeAtStartOfState.getDoubleValue();
      double transitionEndTime;
      if (initialPhaseIsLongEnough)
      {
         transitionEndTime = transitionStartTime + initialTransitionDuration.getValue();
      }
      else
      {
         double desiredTimeInNextPhase = initialTransitionDuration.getValue() - currentIntervalDuration;
         desiredTimeInNextPhase = Math.min(desiredTimeInNextPhase, minimumSplineDuration.getValue());
         desiredTimeInNextPhase = Math.max(desiredTimeInNextPhase, piecewiseConstantCopTrajectory.getIntervalDuration(1));
         transitionEndTime = desiredTimeInNextPhase + piecewiseConstantCopTrajectory.getTimeAtStartOfInterval(1);
      }

      splineStartTime.set(transitionStartTime);
      splineEndTime.set(transitionEndTime);

      dcmTrajectory.computeTrajectory(transitionEndTime);
      dcmTrajectory.getPosition(finalTransitionDCMPosition);
      dcmTrajectory.getVelocity(finalTransitionDCMVelocity);

      initialTransitionDCMPosition.setIncludingFrame(dcmPositionAtStartOfState);
      initialTransitionDCMVelocity.setIncludingFrame(dcmVelocityAtStartOfState);
      initialTransitionDCMPosition.changeFrame(dcmFirstSpline.getReferenceFrame());
      initialTransitionDCMVelocity.changeFrame(dcmFirstSpline.getReferenceFrame());
      finalTransitionDCMPosition.changeFrame(dcmFirstSpline.getReferenceFrame());
      finalTransitionDCMVelocity.changeFrame(dcmFirstSpline.getReferenceFrame());

      dcmFirstSpline
            .setCubic(transitionStartTime, transitionEndTime, initialTransitionDCMPosition, initialTransitionDCMVelocity, finalTransitionDCMPosition,
                      finalTransitionDCMVelocity);

      dcmPositionAtEndOfTransition.setMatchingFrame(finalTransitionDCMPosition);
      dcmVelocityAtEndOfTransition.setMatchingFrame(finalTransitionDCMVelocity);

      if (debug)
         runTransitionDebugChecks(transitionStartTime, transitionEndTime);
   }

   private void computeTransitionTrajectory()
   {
      double currentIntervalDuration = piecewiseConstantCopTrajectory.getTimeAtEndOfInterval(0) - timeAtStartOfState.getValue();
      double nextIntervalDuration = piecewiseConstantCopTrajectory.getIntervalDuration(1);

      double splineStartTime = timeAtStartOfState.getDoubleValue();
      dcmPositionAtStartOfSpline.set(dcmPositionAtStartOfState);
      dcmVelocityAtStartOfSpline.set(dcmVelocityAtStartOfState);

      double splineEndTime;
      if (nextIntervalDuration < minimumSplineDuration.getValue())
      {
         splineEndTime = piecewiseConstantCopTrajectory.getTimeAtEndOfInterval(1);
      }
      else
      {
         double desiredSplineDuration = Math.max(splineSplitFraction.getValue() * nextIntervalDuration, minimumSplineDuration.getValue());
         splineEndTime = piecewiseConstantCopTrajectory.getTimeAtStartOfInterval(1) + desiredSplineDuration;
      }

      this.splineStartTime.set(splineStartTime);
      this.splineEndTime.set(splineEndTime);

      dcmTrajectory.computeTrajectory(splineEndTime);
      dcmTrajectory.getPosition(finalSplineDCMPosition);
      dcmTrajectory.getVelocity(finalSplineDCMVelocity);

      dcmPositionAtEndOfSpline.setMatchingFrame(finalSplineDCMPosition);
      dcmVelocityAtEndOfSpline.setMatchingFrame(finalSplineDCMVelocity);
      initialSplineDCMPosition.setIncludingFrame(dcmPositionAtStartOfSpline);
      initialSplineDCMVelocity.setIncludingFrame(dcmVelocityAtStartOfSpline);

      initialSplineDCMPosition.changeFrame(dcmFirstSpline.getReferenceFrame());
      initialSplineDCMVelocity.changeFrame(dcmFirstSpline.getReferenceFrame());
      finalSplineDCMPosition.changeFrame(dcmFirstSpline.getReferenceFrame());
      finalSplineDCMVelocity.changeFrame(dcmFirstSpline.getReferenceFrame());

      dcmFirstSpline.setCubic(splineStartTime, splineEndTime, initialSplineDCMPosition, initialSplineDCMVelocity, finalSplineDCMPosition,
                              finalSplineDCMVelocity);

      if (debug)
         runTransitionDebugChecks(splineStartTime, splineEndTime);
   }

   private void runTransitionDebugChecks(double transitionStartTime, double transitionEndTime)
   {
      if (finalTransitionDCMPosition.containsNaN())
         throw new IllegalArgumentException("Final DCM position at end of transition contains NaN.");
      if (finalTransitionDCMVelocity.containsNaN())
         throw new IllegalArgumentException("Final DCM velocity at end of transition contains NaN.");
      if (dcmPositionAtStartOfState.containsNaN())
         throw new IllegalArgumentException("DCM position at start of state contains NaN.");
      if (dcmVelocityAtStartOfState.containsNaN())
         throw new IllegalArgumentException("DCM velocity at start of state contains NaN.");
      if (!Double.isFinite(transitionStartTime))
         throw new IllegalArgumentException("Transition start time is not valid.");
      if (!Double.isFinite(transitionEndTime))
         throw new IllegalArgumentException("Transition end time is not valid.");
      if (transitionStartTime > transitionEndTime)
         throw new IllegalArgumentException("Transition start time " + transitionStartTime + " is after the transition end time " + transitionEndTime + ".");
      if (!dcmFirstSpline.isValidTrajectory())
         throw new IllegalArgumentException("Transition trajectory is invalid.");
   }

   private final FramePoint3D desiredDCMPosition = new FramePoint3D();
   private final FrameVector3D desiredDCMVelocity = new FrameVector3D();

   public void computeDcmSetpoints(QuadrantDependentList<YoEnum<ContactState>> currentContactStates, FixedFramePoint3DBasics desiredDCMPositionToPack,
                                   FixedFrameVector3DBasics desiredDCMVelocityToPack)
   {
      timeInState.set(controllerTime.getDoubleValue() - timeAtStartOfState.getValue());

      if (isStanding.getBooleanValue())
      {
         isUsingSpline.set(false);

         // update desired dcm position
         desiredDCMPosition.setToZero(supportFrame);
         desiredDCMPosition.setZ(comHeight.getDoubleValue());
         desiredDCMVelocity.setToZero(supportFrame);
      }
      else
      {
         computeDcmTrajectory(currentContactStates);

         if (isInitialTransfer.getBooleanValue())
            computeInitialTransitionTrajectory();
         else
            computeTransitionTrajectory();

         double currentTime = controllerTime.getDoubleValue();
         dcmTrajectory.computeTrajectory(currentTime);
         if (dcmFirstSpline.timeIntervalContains(currentTime))
         {
            isUsingSpline.set(true);
            dcmFirstSpline.compute(currentTime);
            dcmFirstSpline.getFramePosition(desiredDCMPosition);
            dcmFirstSpline.getFrameVelocity(desiredDCMVelocity);
         }
         else
         {
            isUsingSpline.set(false);
            dcmTrajectory.getPosition(desiredDCMPosition);
            dcmTrajectory.getVelocity(desiredDCMVelocity);
         }

         dcmTrajectory.getPositionAtEndOfSwing(finalDCM);
      }

      desiredDCMPosition.changeFrame(desiredDCMPositionToPack.getReferenceFrame());
      desiredDCMVelocity.changeFrame(desiredDCMVelocityToPack.getReferenceFrame());

      if (debug)
         runOutputDebugChecks();

      desiredDCMPositionToPack.set(desiredDCMPosition);
      desiredDCMVelocityToPack.set(desiredDCMVelocity);

      CapturePointTools.computeDesiredCentroidalMomentumPivot(desiredDCMPosition, desiredDCMVelocity, dcmTrajectory.getNaturalFrequency(), perfectCMPPosition);
   }

   private void runOutputDebugChecks()
   {
      if (desiredDCMPosition.containsNaN())
         throw new IllegalArgumentException("Desired DCM Position contains NaN.");
      if (desiredDCMVelocity.containsNaN())
         throw new IllegalArgumentException("Desired DCM Velocity contains NaN.");
   }

   public void getFinalDCMPosition(FixedFramePoint3DBasics finalDesiredDCMToPack)
   {
      finalDesiredDCMToPack.setMatchingFrame(finalDCM);
   }

   @Override
   public void getDesiredECMPPosition(FramePoint3DBasics desiredECMPPositionToPack)
   {
      desiredECMPPositionToPack.setIncludingFrame(perfectCMPPosition);
   }
}
