package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ReferenceCentroidalMomentumPivotLocationsCalculator;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFramePointInMultipleFrames;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

import java.util.ArrayList;

public class ICPOptimizationInputHandler
{
   private static final String namePrefix = "icpOptimizationController";
   private static final String yoNamePrefix = "controller";
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoFramePoint finalICP;
   private final YoFramePoint2d stanceEntryCMP;
   private final YoFramePoint2d stanceExitCMP;
   private final YoFramePoint2d previousStanceExitCMP;

   private final ReferenceCentroidalMomentumPivotLocationsCalculator referenceCMPsCalculator;
   private final FootstepRecursionMultiplierCalculator footstepRecursionMultiplierCalculator;

   private final DoubleYoVariable doubleSupportDuration;
   private final DoubleYoVariable singleSupportDuration;

   private final DoubleYoVariable exitCMPDurationInPercentOfStepTime;
   private final DoubleYoVariable doubleSupportSplitFraction;

   private final ArrayList<YoFramePointInMultipleFrames> entryCornerPoints = new ArrayList<>();
   private final ArrayList<YoFramePointInMultipleFrames> exitCornerPoints = new ArrayList<>();

   private final ArrayList<FrameVector2d> entryOffsets = new ArrayList<>();
   private final ArrayList<FrameVector2d> exitOffsets = new ArrayList<>();

   public ICPOptimizationInputHandler(CapturePointPlannerParameters icpPlannerParameters, BipedSupportPolygons bipedSupportPolygons,
         SideDependentList<? extends ContactablePlaneBody> contactableFeet, int maximumNumberOfFootstepsToConsider,
         FootstepRecursionMultiplierCalculator footstepRecursionMultiplierCalculator, DoubleYoVariable doubleSupportDuration,
         DoubleYoVariable singleSupportDuration, DoubleYoVariable exitCMPDurationInPercentOfStepTime, DoubleYoVariable doubleSupportSplitFraction,
         YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.footstepRecursionMultiplierCalculator = footstepRecursionMultiplierCalculator;

      this.doubleSupportDuration = doubleSupportDuration;
      this.singleSupportDuration = singleSupportDuration;
      this.exitCMPDurationInPercentOfStepTime = exitCMPDurationInPercentOfStepTime;
      this.doubleSupportSplitFraction = doubleSupportSplitFraction;

      exitCMPDurationInPercentOfStepTime.set(icpPlannerParameters.getTimeSpentOnExitCMPInPercentOfStepTime());
      doubleSupportSplitFraction.set(icpPlannerParameters.getDoubleSupportSplitFraction());

      referenceCMPsCalculator = new ReferenceCentroidalMomentumPivotLocationsCalculator(namePrefix, bipedSupportPolygons, contactableFeet,
            maximumNumberOfFootstepsToConsider, registry);
      referenceCMPsCalculator.initializeParameters(icpPlannerParameters);

      ReferenceFrame[] framesToRegister = new ReferenceFrame[] {worldFrame, bipedSupportPolygons.getMidFeetZUpFrame(),
            bipedSupportPolygons.getSoleZUpFrames().get(RobotSide.LEFT), bipedSupportPolygons.getSoleZUpFrames().get(RobotSide.RIGHT)};
      for (int i = 0; i < maximumNumberOfFootstepsToConsider - 1; i++)
      {
         YoFramePointInMultipleFrames earlyCornerPoint = new YoFramePointInMultipleFrames(yoNamePrefix + "EntryCornerPoints" + i, registry, framesToRegister);
         entryCornerPoints.add(earlyCornerPoint);

         YoFramePointInMultipleFrames lateCornerPoint = new YoFramePointInMultipleFrames(yoNamePrefix + "ExitCornerPoints" + i, registry, framesToRegister);
         exitCornerPoints.add(lateCornerPoint);
      }

      finalICP = new YoFramePoint(yoNamePrefix + "FinalICP", worldFrame, registry);
      stanceEntryCMP = new YoFramePoint2d(yoNamePrefix + "StanceEntryCMP", worldFrame, registry);
      stanceExitCMP = new YoFramePoint2d(yoNamePrefix + "StanceExitCMP", worldFrame, registry);
      previousStanceExitCMP = new YoFramePoint2d(yoNamePrefix + "PreviousStanceExitCMP", worldFrame, registry);

      for (int i = 0; i < maximumNumberOfFootstepsToConsider; i++)
      {
         entryOffsets.add(new FrameVector2d(worldFrame));
         exitOffsets.add(new FrameVector2d(worldFrame));
      }

      if (yoGraphicsListRegistry != null)
      {
         String name = "stanceCMPPoints";
         YoGraphicPosition previousExitCMP = new YoGraphicPosition("previousExitCMP", previousStanceExitCMP, 0.01, YoAppearance.Red(), GraphicType.SQUARE);
         YoGraphicPosition entryCMP = new YoGraphicPosition("entryCMP", stanceEntryCMP, 0.01, YoAppearance.Red(), GraphicType.SQUARE);
         YoGraphicPosition exitCMP = new YoGraphicPosition("exitCMP", stanceExitCMP, 0.01, YoAppearance.Red(), GraphicType.SQUARE);

         YoGraphicPosition finalICP = new YoGraphicPosition("finalICP", this.finalICP, 0.005, YoAppearance.Black(), GraphicType.SOLID_BALL);

         yoGraphicsListRegistry.registerArtifact(name, previousExitCMP.createArtifact());
         yoGraphicsListRegistry.registerArtifact(name, entryCMP.createArtifact());
         yoGraphicsListRegistry.registerArtifact(name, exitCMP.createArtifact());

         yoGraphicsListRegistry.registerArtifact(name, finalICP.createArtifact());
      }
   }

   public void clearPlan()
   {
      referenceCMPsCalculator.clear();
   }

   public void addFootstepToPlan(Footstep footstep)
   {
      referenceCMPsCalculator.addUpcomingFootstep(footstep);
   }

   public void initializeForDoubleSupport(boolean isStanding, boolean useTwoCMPs, RobotSide transferToSide, double omega0)
   {
      referenceCMPsCalculator.setUseTwoCMPsPerSupport(useTwoCMPs);
      referenceCMPsCalculator.computeReferenceCMPsStartingFromDoubleSupport(isStanding, transferToSide);
      referenceCMPsCalculator.update();

      updateCornerPoints(useTwoCMPs, omega0);
   }

   public void initializeForSingleSupport(boolean useTwoCMPs, RobotSide supportSide, double omega0)
   {
      referenceCMPsCalculator.setUseTwoCMPsPerSupport(useTwoCMPs);
      referenceCMPsCalculator.computeReferenceCMPsStartingFromSingleSupport(supportSide);
      referenceCMPsCalculator.update();

      updateCornerPoints(useTwoCMPs, omega0);
   }

   private void updateCornerPoints(boolean useTwoCMPs, double omega0)
   {
      double steppingDuration = doubleSupportDuration.getDoubleValue() + singleSupportDuration.getDoubleValue();
      if (useTwoCMPs)
         CapturePointTools.computeDesiredCornerPoints(entryCornerPoints, exitCornerPoints, referenceCMPsCalculator.getEntryCMPs(), referenceCMPsCalculator.getExitCMPs(),
               steppingDuration, exitCMPDurationInPercentOfStepTime.getDoubleValue(), omega0);
      else
         CapturePointTools.computeDesiredCornerPoints(entryCornerPoints, referenceCMPsCalculator.getEntryCMPs(), false, steppingDuration, omega0);

   }

   public void computeFinalICPRecursion(FramePoint2d finalICPRecursionToPack, int numberOfFootstepsToConsider, boolean useTwoCMPs, boolean isInTransfer, double omega0)
   {
      computeFinalICP(numberOfFootstepsToConsider, useTwoCMPs, isInTransfer, omega0);

      finalICPRecursionToPack.setByProjectionOntoXYPlane(finalICP.getFrameTuple());
      finalICPRecursionToPack.scale(footstepRecursionMultiplierCalculator.getFinalICPRecursionMultiplier());
      finalICPRecursionToPack.scale(footstepRecursionMultiplierCalculator.getCurrentStateProjectionMultiplier());
   }

   private void computeFinalICP(int numberOfFootstepsToConsider, boolean useTwoCMPs, boolean isInTransfer, double omega0)
   {
      double doubleSupportTimeSpentBeforeEntryCornerPoint = doubleSupportDuration.getDoubleValue() * doubleSupportSplitFraction.getDoubleValue();
      double steppingDuration = doubleSupportDuration.getDoubleValue() + singleSupportDuration.getDoubleValue();

      double totalTimeSpentOnExitCMP = steppingDuration * exitCMPDurationInPercentOfStepTime.getDoubleValue();
      double timeToSpendOnFinalCMPBeforeDoubleSupport = totalTimeSpentOnExitCMP - doubleSupportTimeSpentBeforeEntryCornerPoint;

      if (numberOfFootstepsToConsider == 0)
      {
         if (isInTransfer)
            CapturePointTools.computeDesiredCapturePointPosition(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint, entryCornerPoints.get(1),
                  referenceCMPsCalculator.getEntryCMPs().get(1), finalICP);
         else
            CapturePointTools.computeDesiredCapturePointPosition(omega0, timeToSpendOnFinalCMPBeforeDoubleSupport, exitCornerPoints.get(0),
                  referenceCMPsCalculator.getExitCMPs().get(0), finalICP);
      }
      else
      {
         int stepIndexToPoll;
         if (isInTransfer)
            stepIndexToPoll = numberOfFootstepsToConsider + 1;
         else
            stepIndexToPoll = numberOfFootstepsToConsider;

         if (useTwoCMPs)
            CapturePointTools.computeDesiredCapturePointPosition(omega0, timeToSpendOnFinalCMPBeforeDoubleSupport, exitCornerPoints.get(stepIndexToPoll),
                  referenceCMPsCalculator.getExitCMPs().get(stepIndexToPoll), finalICP);
         else
            CapturePointTools.computeDesiredCapturePointPosition(omega0, doubleSupportTimeSpentBeforeEntryCornerPoint, entryCornerPoints.get(stepIndexToPoll),
                  referenceCMPsCalculator.getEntryCMPs().get(stepIndexToPoll), finalICP);
      }
   }

   private final FramePoint2d previousStanceExitCMP2d = new FramePoint2d(worldFrame);
   private final FramePoint2d stanceEntryCMP2d = new FramePoint2d(worldFrame);
   private final FramePoint2d stanceExitCMP2d = new FramePoint2d(worldFrame);

   public void computeStanceCMPProjection(FramePoint2d stanceCMPProjectionToPack, double timeRemainingInState, boolean useTwoCMPs, boolean isInTransfer,
         boolean useInitialICP, double omega0)
   {
      footstepRecursionMultiplierCalculator.computeRemainingProjectionMultipliers(timeRemainingInState, useTwoCMPs, isInTransfer, omega0, useInitialICP);

      if (useTwoCMPs)
      {
         if (isInTransfer)
         {
            FramePoint previousStanceExitCMP = referenceCMPsCalculator.getExitCMPs().get(0).getFrameTuple();
            FramePoint stanceEntryCMP = referenceCMPsCalculator.getEntryCMPs().get(1).getFrameTuple();
            FramePoint stanceExitCMP = referenceCMPsCalculator.getExitCMPs().get(1).getFrameTuple();

            previousStanceExitCMP2d.setByProjectionOntoXYPlane(previousStanceExitCMP);
            stanceEntryCMP2d.setByProjectionOntoXYPlane(stanceEntryCMP);
            stanceExitCMP2d.setByProjectionOntoXYPlane(stanceExitCMP);

            this.previousStanceExitCMP.set(previousStanceExitCMP2d);
            this.stanceEntryCMP.set(stanceEntryCMP2d);
            this.stanceExitCMP.set(stanceExitCMP2d);
         }
         else
         {
            FramePoint stanceEntryCMP = referenceCMPsCalculator.getEntryCMPs().get(0).getFrameTuple();
            FramePoint stanceExitCMP = referenceCMPsCalculator.getExitCMPs().get(0).getFrameTuple();

            previousStanceExitCMP2d.setToZero();
            stanceEntryCMP2d.setByProjectionOntoXYPlane(stanceEntryCMP);
            stanceExitCMP2d.setByProjectionOntoXYPlane(stanceExitCMP);

            this.previousStanceExitCMP.setToNaN();
            this.stanceEntryCMP.set(stanceEntryCMP2d);
            this.stanceExitCMP.set(stanceExitCMP2d);
         }
      }
      else
      {
         if (isInTransfer)
         {
            FramePoint previousStanceExitCMP = referenceCMPsCalculator.getEntryCMPs().get(0).getFrameTuple();
            FramePoint stanceExitCMP = referenceCMPsCalculator.getEntryCMPs().get(1).getFrameTuple();

            previousStanceExitCMP2d.setByProjectionOntoXYPlane(previousStanceExitCMP);
            stanceEntryCMP2d.setToZero();
            stanceExitCMP2d.setByProjectionOntoXYPlane(stanceExitCMP);

            this.previousStanceExitCMP.set(previousStanceExitCMP2d);
            this.stanceEntryCMP.setToNaN();
            this.stanceExitCMP.set(stanceExitCMP2d);
         }
         else
         {
            FramePoint stanceExitCMP = referenceCMPsCalculator.getEntryCMPs().get(0).getFrameTuple();

            previousStanceExitCMP2d.setToZero();
            stanceEntryCMP2d.setToZero();
            stanceExitCMP2d.setByProjectionOntoXYPlane(stanceExitCMP);

            this.previousStanceExitCMP.setToNaN();
            this.stanceEntryCMP.setToNaN();
            this.stanceExitCMP.set(stanceExitCMP2d);
         }
      }

      double previousExitMultiplier = footstepRecursionMultiplierCalculator.getRemainingPreviousStanceExitCMPProjectionMultiplier();
      double entryMultiplier = footstepRecursionMultiplierCalculator.getRemainingStanceEntryCMPProjectionMultiplier();
      double exitMultiplier = footstepRecursionMultiplierCalculator.getRemainingStanceExitCMPProjectionMultiplier();

      double currentStateProjectionMultiplier = footstepRecursionMultiplierCalculator.getCurrentStateProjectionMultiplier();

      entryMultiplier += currentStateProjectionMultiplier * footstepRecursionMultiplierCalculator.getStanceEntryCMPProjectionMultiplier();
      exitMultiplier += currentStateProjectionMultiplier * footstepRecursionMultiplierCalculator.getStanceExitCMPProjectionMultiplier();

      previousStanceExitCMP2d.scale(previousExitMultiplier);
      stanceEntryCMP2d.scale(entryMultiplier);
      stanceExitCMP2d.scale(exitMultiplier);

      stanceCMPProjectionToPack.setToZero();
      stanceCMPProjectionToPack.add(previousStanceExitCMP2d);
      stanceCMPProjectionToPack.add(stanceEntryCMP2d);
      stanceCMPProjectionToPack.add(stanceExitCMP2d);
   }

   public void computeBeginningOfStateICPProjection(FramePoint2d beginningOfStateICPProjectionToPack, FramePoint2d beginningOfStateICP)
   {
      beginningOfStateICPProjectionToPack.set(beginningOfStateICP);
      beginningOfStateICPProjectionToPack.scale(footstepRecursionMultiplierCalculator.getInitialICPProjectionMultiplier());
   }

   private final FramePoint2d totalOffsetEffect = new FramePoint2d();
   public void computeCMPOffsetRecursionEffect(FramePoint2d cmpOffsetRecursionEffectToPack, ArrayList<YoFramePoint2d> upcomingFootstepLocations,
         int numberOfFootstepsToConsider)
   {
      computeTwoCMPOffsets(upcomingFootstepLocations, numberOfFootstepsToConsider);

      cmpOffsetRecursionEffectToPack.setToZero();
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         totalOffsetEffect.set(exitOffsets.get(i));
         totalOffsetEffect.scale(footstepRecursionMultiplierCalculator.getCMPRecursionExitMultiplier(i));

         cmpOffsetRecursionEffectToPack.add(totalOffsetEffect);

         totalOffsetEffect.set(entryOffsets.get(i));
         totalOffsetEffect.scale(footstepRecursionMultiplierCalculator.getCMPRecursionEntryMultiplier(i));

         cmpOffsetRecursionEffectToPack.add(totalOffsetEffect);
      }
      cmpOffsetRecursionEffectToPack.scale(footstepRecursionMultiplierCalculator.getCurrentStateProjectionMultiplier());
   }

   private void computeTwoCMPOffsets(ArrayList<YoFramePoint2d> upcomingFootstepLocations, int numberOfFootstepsToConsider)
   {
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         FrameVector2d entryOffset = entryOffsets.get(i);
         FrameVector2d exitOffset = exitOffsets.get(i);

         entryOffset.setToZero(worldFrame);
         exitOffset.setToZero(worldFrame);

         entryOffset.setByProjectionOntoXYPlane(referenceCMPsCalculator.getEntryCMPs().get(i + 1).getFrameTuple());
         exitOffset.setByProjectionOntoXYPlane(referenceCMPsCalculator.getExitCMPs().get(i + 1).getFrameTuple());

         entryOffset.sub(upcomingFootstepLocations.get(i).getFrameTuple2d());
         exitOffset.sub(upcomingFootstepLocations.get(i).getFrameTuple2d());
      }
   }

   public ArrayList<FrameVector2d> getEntryOffsets()
   {
      return entryOffsets;
   }

   public ArrayList<FrameVector2d> getExitOffsets()
   {
      return exitOffsets;
   }

   public FramePoint2d getPreviousStanceExitCMP()
   {
      return previousStanceExitCMP.getFrameTuple2d();
   }

   public FramePoint2d getStanceEntryCMP()
   {
      return stanceEntryCMP.getFrameTuple2d();
   }

   public FramePoint2d getStanceExitCMP()
   {
      return stanceExitCMP.getFrameTuple2d();
   }

   public FramePoint getFinalICP()
   {
      return finalICP.getFrameTuple();
   }
}
