package us.ihmc.commonWalkingControlModules.captureRegion;

import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.commonWalkingControlModules.capturePoint.CapturePointTools;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.pushRecoveryController.PushRecoveryControllerParameters;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.euclid.geometry.interfaces.ConvexPolygon2DReadOnly;
import us.ihmc.euclid.geometry.tools.EuclidGeometryPolygonTools;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.referenceFrame.*;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.StepConstraintRegion;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.robotics.geometry.ConvexPolygonTools;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.yoVariables.providers.DoubleProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class MultiStepRecoveryStepCalculator
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final SideDependentList<? extends ReferenceFrame> soleZUpFrames;

   private final ReachableFootholdsCalculator reachableFootholdsCalculator;
   private final AchievableCaptureRegionCalculatorWithDelay captureRegionCalculator;

   private final ConvexPolygonTools polygonTools = new ConvexPolygonTools();

   private final FrameConvexPolygon2D reachableRegion = new FrameConvexPolygon2D();

   private final RecyclingArrayList<FramePoint2DBasics> capturePointsAtTouchdown = new RecyclingArrayList<>(FramePoint2D::new);
   private final RecyclingArrayList<FramePoint2DBasics> recoveryStepLocations = new RecyclingArrayList<>(FramePoint2D::new);
   private final TDoubleArrayList recoverySwingDurations = new TDoubleArrayList();

   private final RecyclingArrayList<FrameConvexPolygon2DBasics> captureRegionsAtTouchdown = new RecyclingArrayList<>(FrameConvexPolygon2D::new);
   private final RecyclingArrayList<FrameConvexPolygon2DBasics> reachableRegions = new RecyclingArrayList<>(FrameConvexPolygon2D::new);
   private final RecyclingArrayList<FrameConvexPolygon2DBasics> reachableCaptureRegions = new RecyclingArrayList<>(FrameConvexPolygon2D::new);
   private final RecyclingArrayList<FrameConvexPolygon2DBasics> reachableConstrainedCaptureRegions = new RecyclingArrayList<>(FrameConvexPolygon2D::new);
   private final RecyclingArrayList<RecyclingArrayList<ConstrainedReachableRegion>> constrainedReachableRegions = new RecyclingArrayList<>(() -> new RecyclingArrayList(ConstrainedReachableRegion::new));
   private final List<ConstrainedReachableRegion> constraintRegions = new ArrayList<>();

   private final PoseReferenceFrame stanceFrame = new PoseReferenceFrame("StanceFrame", worldFrame);
   private final FramePose3D stancePose = new FramePose3D();
   private final FramePoint2D stancePosition = new FramePoint2D();

   private final FrameLine2D forwardLineAtNominalWidth = new FrameLine2D();
   private final FramePoint2D forwardLineSegmentEnd = new FramePoint2D();

   private final FramePoint3D stepPosition = new FramePoint3D();

   private final ConvexPolygon2DReadOnly defaultFootPolygon;
   private final FramePoint2D icpAtStart = new FramePoint2D();
   private final FrameConvexPolygon2DBasics stancePolygon = new FrameConvexPolygon2D();

   private final RecyclingArrayList<Footstep> recoveryFootsteps = new RecyclingArrayList<>(Footstep::new);
   private final RecyclingArrayList<FootstepTiming> recoveryFootstepTimings = new RecyclingArrayList<>(FootstepTiming::new);

   private final PushRecoveryControllerParameters pushRecoveryParameters;
   private IntFunction<List<StepConstraintRegion>> constraintRegionProvider;

   private boolean isStateCapturable = false;

   private int depth = 3;

   public MultiStepRecoveryStepCalculator(DoubleProvider kinematicsStepRange,
                                          DoubleProvider footWidth,
                                          PushRecoveryControllerParameters pushRecoveryParameters,
                                          SideDependentList<? extends ReferenceFrame> soleZUpFrames,
                                          ConvexPolygon2DReadOnly defaultFootPolygon)
   {
      this(kinematicsStepRange, kinematicsStepRange, footWidth, kinematicsStepRange, pushRecoveryParameters, soleZUpFrames, defaultFootPolygon);
   }

   public MultiStepRecoveryStepCalculator(DoubleProvider maxStepLength,
                                          DoubleProvider maxBackwardsStepLength,
                                          DoubleProvider minStepWidth,
                                          DoubleProvider maxStepWidth,
                                          PushRecoveryControllerParameters pushRecoveryParameters,
                                          SideDependentList<? extends ReferenceFrame> soleZUpFrames,
                                          ConvexPolygon2DReadOnly defaultFootPolygon)
   {
      this.soleZUpFrames = soleZUpFrames;
      this.defaultFootPolygon = defaultFootPolygon;
      this.pushRecoveryParameters = pushRecoveryParameters;

      reachableFootholdsCalculator = new ReachableFootholdsCalculator(maxStepLength, maxBackwardsStepLength, minStepWidth, maxStepWidth);
      captureRegionCalculator = new AchievableCaptureRegionCalculatorWithDelay();
   }

   public void setConstraintRegionProvider(IntFunction<List<StepConstraintRegion>> constraintRegionProvider)
   {
      this.constraintRegionProvider = constraintRegionProvider;
   }

   public void setMaxStepsToGenerateForRecovery(int depth)
   {
      this.depth = depth;
   }

   private final TDoubleArrayList candidateSwingTimes = new TDoubleArrayList();

   public boolean computePreferredRecoverySteps(RobotSide swingSide,
                                                double nextTransferDuration,
                                                double swingTimeRemaining,
                                                FramePoint2DReadOnly currentICP,
                                                double omega0,
                                                FrameConvexPolygon2DReadOnly footPolygon)
   {
      candidateSwingTimes.reset();
      candidateSwingTimes.add(swingTimeRemaining);

      return computeRecoverySteps(swingSide, nextTransferDuration, swingTimeRemaining, candidateSwingTimes, currentICP, omega0, footPolygon);
   }

   public boolean computeRecoverySteps(RobotSide swingSide,
                                       double nextTransferDuration,
                                       double minSwingTimeRemaining,
                                       double maxSwingTimeRemaining,
                                       FramePoint2DReadOnly currentICP,
                                       double omega0,
                                       FrameConvexPolygon2DReadOnly footPolygon)
   {
      candidateSwingTimes.reset();
      candidateSwingTimes.add(minSwingTimeRemaining);
      candidateSwingTimes.add(maxSwingTimeRemaining);

      return computeRecoverySteps(swingSide, nextTransferDuration, minSwingTimeRemaining, candidateSwingTimes, currentICP, omega0, footPolygon);
   }

   public boolean computeRecoverySteps(RobotSide swingSide,
                                       double nextTransferDuration,
                                       double swingTimeToSet,
                                       TDoubleArrayList candidateSwingTimes,
                                       FramePoint2DReadOnly currentICP,
                                       double omega0,
                                       FrameConvexPolygon2DReadOnly footPolygon)
   {
      recoverySwingDurations.reset();

      int numberOfRecoverySteps = calculateRecoveryStepLocations(swingSide, nextTransferDuration, candidateSwingTimes, currentICP, omega0, footPolygon);

      recoveryFootsteps.clear();
      recoveryFootstepTimings.clear();

      for (int i = 0; i < numberOfRecoverySteps; i++)
      {
         stepPosition.set(recoveryStepLocations.get(i), stancePose.getZ());
         Footstep recoveryFootstep = recoveryFootsteps.add();
         recoveryFootstep.setPose(stepPosition, stancePose.getOrientation());
         recoveryFootstep.setRobotSide(swingSide);

         recoveryFootstepTimings.add().setTimings(recoverySwingDurations.get(i), nextTransferDuration);

         swingSide = swingSide.getOppositeSide();
      }

      return isStateCapturable;
   }

   public int getNumberOfRecoverySteps()
   {
      return recoveryFootsteps.size();
   }

   public Footstep getRecoveryStep(int stepIdx)
   {
      return recoveryFootsteps.get(stepIdx);
   }

   public FootstepTiming getRecoveryStepTiming(int stepIdx)
   {
      return recoveryFootstepTimings.get(stepIdx);
   }

   private final FramePoint2D pointToThrowAway = new FramePoint2D();

   private int calculateRecoveryStepLocations(RobotSide swingSide,
                                              double nextTransferDuration,
                                              TDoubleArrayList candidateSwingTimeRange,
                                              FramePoint2DReadOnly currentICP,
                                              double omega0,
                                              FrameConvexPolygon2DReadOnly footPolygon)
   {
      icpAtStart.set(currentICP);
      stancePose.setToZero(soleZUpFrames.get(swingSide.getOppositeSide()));
      stancePose.changeFrame(worldFrame);
      stanceFrame.setPoseAndUpdate(stancePose);

      int depthIdx = 0;
      stancePolygon.setIncludingFrame(footPolygon);

      int numberOfRecoverySteps = 0;
      isStateCapturable = false;

      recoveryStepLocations.clear();
      capturePointsAtTouchdown.clear();
      reachableRegions.clear();
      captureRegionsAtTouchdown.clear();
      reachableCaptureRegions.clear();
      reachableConstrainedCaptureRegions.clear();

      constraintRegions.clear();
      constrainedReachableRegions.clear();

      for (; depthIdx < depth; depthIdx++)
      {
         reachableFootholdsCalculator.calculateReachableRegion(swingSide, stancePose.getPosition(), stancePose.getOrientation(), reachableRegion);

         if (captureRegionCalculator.calculateCaptureRegion(nextTransferDuration, candidateSwingTimeRange, icpAtStart, omega0, stancePose, stancePolygon))
         {
            isStateCapturable = true;
            break;
         }

         FrameConvexPolygon2DBasics captureRegionAtTouchdown = captureRegionsAtTouchdown.add();

         captureRegionAtTouchdown.setMatchingFrame(captureRegionCalculator.getCaptureRegion(), false);
         reachableRegions.add().set(reachableRegion);

         stancePosition.set(stancePose.getPosition());
         numberOfRecoverySteps++;

         FrameConvexPolygon2DBasics reachableCaptureRegion = reachableCaptureRegions.add();
         polygonTools.computeIntersectionOfPolygons(captureRegionAtTouchdown, reachableRegion, reachableCaptureRegion);

         FramePoint2DBasics recoveryStepLocation = recoveryStepLocations.add();
         FramePoint2DBasics capturePointAtTouchdown = capturePointsAtTouchdown.add();

         computeConstrainedReachableRegions(depthIdx, reachableRegion);

         FrameConvexPolygon2DBasics constrainedCaptureRegion = reachableConstrainedCaptureRegions.add();
         computeConstrainedCaptureRegion(depthIdx, reachableCaptureRegion, constrainedCaptureRegion);

         if (!constrainedCaptureRegion.isEmpty())
         { // they do intersect
            FramePoint2DReadOnly centerOfIntersection = constrainedCaptureRegion.getCentroid();

            computeRecoveryStepAtNominalWidth(swingSide, stancePosition, centerOfIntersection, capturePointAtTouchdown);

            if (!constrainedCaptureRegion.isPointInside(capturePointAtTouchdown))
            {
               EuclidGeometryPolygonTools.intersectionBetweenLineSegment2DAndConvexPolygon2D(stancePosition,
                                                                                             centerOfIntersection,
                                                                                             constrainedCaptureRegion.getPolygonVerticesView(),
                                                                                             constrainedCaptureRegion.getNumberOfVertices(),
                                                                                             true,
                                                                                             capturePointAtTouchdown,
                                                                                             pointToThrowAway);
            }

            double swingDuration = computeSwingDuration(nextTransferDuration, candidateSwingTimeRange.get(0), icpAtStart, capturePointAtTouchdown, omega0, stancePolygon);
            recoverySwingDurations.add(swingDuration);

            recoveryStepLocation.set(capturePointAtTouchdown);
            isStateCapturable = true;
            break;
         }
         else
         {
            boolean definitelyNotCapturable;
            if (constraintRegionProvider == null || constraintRegionProvider.apply(depthIdx) == null)
            {
               definitelyNotCapturable = computeUnconstrainedBestEffortStep(captureRegionAtTouchdown, reachableRegion, capturePointAtTouchdown, recoveryStepLocation);
            }
            else if (constrainedReachableRegions.get(depthIdx).size() > 0)
            {
               definitelyNotCapturable = computeBestEffortStepWithEnvironmentalConstraints(depthIdx,
                                                                                           captureRegionAtTouchdown,
                                                                                           capturePointAtTouchdown,
                                                                                           recoveryStepLocation);
            }
            else
            {
               definitelyNotCapturable = true;
            }

            double swingDuration = computeSwingDuration(nextTransferDuration, candidateSwingTimeRange.get(0), icpAtStart, capturePointAtTouchdown, omega0, stancePolygon);
            recoverySwingDurations.add(swingDuration);

            if (definitelyNotCapturable)
               break;
         }

         swingSide = swingSide.getOppositeSide();

         stancePose.getPosition().set(recoveryStepLocation);
         icpAtStart.set(capturePointAtTouchdown);

         stanceFrame.setPoseAndUpdate(stancePose);
         stancePolygon.clear(stanceFrame);
         stancePolygon.set(defaultFootPolygon);
         stancePolygon.update();
         stancePolygon.scale(stancePolygon.getCentroid(), 0.5);
         stancePolygon.changeFrameAndProjectToXYPlane(worldFrame);
      }

      return numberOfRecoverySteps;
   }

   private void computeRecoveryStepAtNominalWidth(RobotSide swingSide,
                                                  FramePoint2DReadOnly stancePosition,
                                                  FramePoint2DReadOnly pointInDirectionOfStep,
                                                  FramePoint2DBasics recoveryStepLocationToPack)
   {
      forwardLineAtNominalWidth.setToZero(stanceFrame);
      forwardLineAtNominalWidth.set(stanceFrame, 0.0, swingSide.negateIfRightSide(pushRecoveryParameters.getPreferredStepWidth()), 1.0, 0.0);
      forwardLineAtNominalWidth.changeFrame(worldFrame);

      forwardLineSegmentEnd.set(forwardLineAtNominalWidth.getPoint());
      forwardLineSegmentEnd.add(forwardLineAtNominalWidth.getDirection());
      EuclidGeometryTools.intersectionBetweenTwoLine2Ds(stancePosition,
                                                        pointInDirectionOfStep,
                                                        forwardLineAtNominalWidth.getPoint(),
                                                        forwardLineSegmentEnd,
                                                        recoveryStepLocationToPack);
   }

   private final FrameVector2D icpDynamicsDirection = new FrameVector2D();
   private final FramePoint2D icpBeforeTransfer = new FramePoint2D();
   private final FramePoint2D nominalCMP = new FramePoint2D();

   private double computeSwingDuration(double nextTransferDuration,
                                       double minSwingDuration,
                                       FramePoint2DReadOnly icpAtStart,
                                       FramePoint2DReadOnly icpAtEnd,
                                       double omega0,
                                       FrameConvexPolygon2DReadOnly stancePolygon)
   {
      int lineOfSightStartIndex = stancePolygon.lineOfSightStartIndex(icpAtStart);
      int lineOfSightEndIndex = stancePolygon.lineOfSightEndIndex(icpAtStart);
      if (lineOfSightStartIndex == -1 || lineOfSightEndIndex == -1)
         return minSwingDuration;

      icpDynamicsDirection.sub(icpAtEnd, icpAtStart);
      EuclidGeometryTools.intersectionBetweenLine2DAndLineSegment2D(icpAtStart,
                                                                    icpDynamicsDirection,
                                                                    stancePolygon.getVertex(lineOfSightStartIndex),
                                                                    stancePolygon.getVertex(lineOfSightEndIndex),
                                                                    nominalCMP);
      AchievableCaptureRegionCalculatorWithDelay.computeCapturePointBeforeTransfer(icpAtEnd, nominalCMP, omega0, nextTransferDuration, icpBeforeTransfer);
      double idealSwingDuration = CapturePointTools.computeTimeToReachCapturePointUsingConstantCMP(omega0, icpBeforeTransfer, icpAtStart, nominalCMP);

      return Math.max(idealSwingDuration, minSwingDuration);
   }

   FramePoint2DReadOnly getCapturePointAtTouchdown(int i)
   {
      return capturePointsAtTouchdown.get(i);
   }

   FramePoint2DReadOnly getRecoveryStepLocation(int i)
   {
      return recoveryStepLocations.get(i);
   }

   public FrameConvexPolygon2DReadOnly getReachableRegion(int i)
   {
      return reachableRegions.get(i);
   }

   public FrameConvexPolygon2DReadOnly getCaptureRegionAtTouchdown(int i)
   {
      return captureRegionsAtTouchdown.get(i);
   }

   public FrameConvexPolygon2DReadOnly getIntersectingRegion(int i)
   {
      return reachableCaptureRegions.get(i);
   }

   public int getNumberOfConstraintRegionsForStep(int stepNumber)
   {
      return constrainedReachableRegions.get(stepNumber).size();
   }

   public FrameConvexPolygon2DReadOnly getConstrainedReachableRegion(int stepNumber, int regionNumber)
   {
      return constrainedReachableRegions.get(stepNumber).get(regionNumber);
   }

   public boolean hasConstraintRegions()
   {
      return !constraintRegions.isEmpty();
   }

   public StepConstraintRegion getConstraintRegionInWorld(int i)
   {
      if (constraintRegions.get(i) != null)
         return constraintRegions.get(i).getStepConstraintRegion();
      return null;
   }

   private void computeConstrainedCaptureRegion(int stepNumber,
                                                FrameConvexPolygon2DReadOnly reachableCaptureRegion,
                                                FrameConvexPolygon2DBasics constrainedCaptureRegionToPack)
   {
      // FIXME this logic is bad.
      if (constrainedReachableRegions.get(stepNumber).size() == 0 && (constraintRegionProvider == null || constraintRegionProvider.apply(stepNumber) == null))
      {
         constrainedCaptureRegionToPack.set(reachableCaptureRegion);
         return;
      }

      ConstrainedReachableRegion constraintRegion = null;
      if (constrainedReachableRegions.get(stepNumber).size() == 1)
      {
         constraintRegion = constrainedReachableRegions.get(stepNumber).get(0);
      }
      else
      {
         double minArea = 0.0;
         for (int i = 0; i < constrainedReachableRegions.get(stepNumber).size(); i++)
         {
            ConstrainedReachableRegion region = constrainedReachableRegions.get(stepNumber).get(i);
            double area = polygonTools.computeIntersectionAreaOfPolygons(reachableCaptureRegion, region);
            if (area > minArea)
            {
               minArea = area;
               constraintRegion = region;
            }
         }
      }

      constraintRegions.add(constraintRegion);

      if (constraintRegion != null)
      {
         polygonTools.computeIntersectionOfPolygons(reachableCaptureRegion, constraintRegion, constrainedCaptureRegionToPack);
      }
      else
      {
         constrainedCaptureRegionToPack.clearAndUpdate();
      }
   }

   private boolean computeUnconstrainedBestEffortStep(FrameConvexPolygon2DReadOnly captureRegion,
                                                      FrameConvexPolygon2DReadOnly reachableRegion,
                                                      FramePoint2DBasics capturePointAtTouchdownToPack,
                                                      FramePoint2DBasics recoveryStepLocationToPack)
   {
      if (captureRegion.getNumberOfVertices() == 0)
      {
         return true;
      }
      else if (captureRegion.getNumberOfVertices() == 1)
      {
         capturePointAtTouchdownToPack.set(captureRegion.getVertex(0));
         reachableRegion.orthogonalProjection(capturePointAtTouchdownToPack, recoveryStepLocationToPack);
      }
      else if (captureRegion.getNumberOfVertices() == 2)
      {
         throw new RuntimeException("The event of the capture region having two points still needs to be implemented.");
      }
      else
      {
         polygonTools.computeMinimumDistancePoints(reachableRegion, captureRegion, recoveryStepLocationToPack, capturePointAtTouchdownToPack);
      }

      return false;
   }

   private final FramePoint2D point1ToThrowAway = new FramePoint2D();
   private final FramePoint2D point2ToThrowAway = new FramePoint2D();

   private boolean computeBestEffortStepWithEnvironmentalConstraints(int stepNumber,
                                                                     FrameConvexPolygon2DReadOnly captureRegion,
                                                                     FramePoint2DBasics capturePointAtTouchdownToPack,
                                                                     FramePoint2DBasics recoveryStepLocationToPack)
   {
      double closestDistance = Double.POSITIVE_INFINITY;
      boolean isStateDefinitelyNotCapturable = true;

      for (int i = 0; i < constrainedReachableRegions.get(stepNumber).size(); i++)
      {
         FrameConvexPolygon2DReadOnly constrainedReachableRegion = constrainedReachableRegions.get(stepNumber).get(i);

         if (constrainedReachableRegion.isEmpty())
            continue;

         boolean regionIsNotCapturable = computeUnconstrainedBestEffortStep(captureRegion, constrainedReachableRegion, point1ToThrowAway, point2ToThrowAway);
         if (!regionIsNotCapturable)
            isStateDefinitelyNotCapturable = false;

         double distanceSquared = point1ToThrowAway.distanceSquared(point2ToThrowAway);
         if (isStateDefinitelyNotCapturable || (!regionIsNotCapturable && distanceSquared < closestDistance))
         {
            closestDistance = distanceSquared;
            capturePointAtTouchdownToPack.set(point1ToThrowAway);
            recoveryStepLocationToPack.set(point2ToThrowAway);
         }
      }

      return isStateDefinitelyNotCapturable;
   }

   private final FrameConvexPolygon2D constraintRegionHull = new FrameConvexPolygon2D();
   private final FrameConvexPolygon2D intersectingHull = new FrameConvexPolygon2D();

   private void computeConstrainedReachableRegions(int stepNumber, FrameConvexPolygon2DReadOnly reachableRegion)
   {
      constrainedReachableRegions.add().clear();
      if (constraintRegionProvider == null || constraintRegionProvider.apply(stepNumber) == null)
         return;

      List<StepConstraintRegion> constraintRegions = constraintRegionProvider.apply(stepNumber);
      for (int i = 0; i < constraintRegions.size(); i++)
      {
         constraintRegionHull.set(constraintRegions.get(i).getConvexHull());
         constraintRegionHull.applyTransform(constraintRegions.get(i).getTransformToWorld(), false);

         polygonTools.computeIntersectionOfPolygons(reachableRegion, constraintRegionHull, intersectingHull);
         if (intersectingHull.getArea() > 0.0)
         {
            ConstrainedReachableRegion constrainedReachableRegion = constrainedReachableRegions.get(stepNumber).add();
            constrainedReachableRegion.set(intersectingHull);
            constrainedReachableRegion.setStepConstraintRegion(constraintRegions.get(i));
         }
      }
   }

   private static class ConstrainedReachableRegion extends FrameConvexPolygon2D
   {
      private StepConstraintRegion stepConstraintRegion;

      public void setStepConstraintRegion(StepConstraintRegion stepConstraintRegion)
      {
         this.stepConstraintRegion = stepConstraintRegion;
      }

      public StepConstraintRegion getStepConstraintRegion()
      {
         return stepConstraintRegion;
      }
   }
}
