package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFrameVector2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

import java.util.ArrayList;

public class ICPOptimizationSolutionHandler
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoFramePoint2d actualEndOfStateICP;
   private final YoFramePoint2d controllerReferenceICP;
   private final YoFrameVector2d controllerReferenceICPVelocity;
   private final YoFramePoint2d controllerReferenceCMP;

   private final YoFramePoint2d nominalEndOfStateICP;
   private final YoFramePoint2d nominalReferenceICP;
   private final YoFrameVector2d nominalReferenceICPVelocity;
   private final YoFramePoint2d nominalReferenceCMP;

   private final DoubleYoVariable footstepForwardDeadband;
   private final DoubleYoVariable footstepLateralDeadband;
   private final BooleanYoVariable footstepWasAdjusted;

   private final DoubleYoVariable controllerCostToGo;
   private final DoubleYoVariable controllerFootstepCostToGo;
   private final DoubleYoVariable controllerFootstepRegularizationCostToGo;
   private final DoubleYoVariable controllerFeedbackCostToGo;
   private final DoubleYoVariable controllerFeedbackRegularizationCostToGo;
   private final DoubleYoVariable controllerDynamicRelaxationCostToGo;

   private final FootstepRecursionMultiplierCalculator footstepRecursionMultiplierCalculator;

   public ICPOptimizationSolutionHandler(ICPOptimizationParameters icpOptimizationParameters, FootstepRecursionMultiplierCalculator footstepRecursionMultiplierCalculator,
         YoVariableRegistry registry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.footstepRecursionMultiplierCalculator = footstepRecursionMultiplierCalculator;

      actualEndOfStateICP = new YoFramePoint2d("actualEndOfStateICP", worldFrame, registry);

      controllerReferenceICP = new YoFramePoint2d("controllerReferenceICP", worldFrame, registry);
      controllerReferenceICPVelocity = new YoFrameVector2d("controllerReferenceICPVelocity", worldFrame, registry);
      controllerReferenceCMP = new YoFramePoint2d("controllerReferenceCMP", worldFrame, registry);

      nominalEndOfStateICP = new YoFramePoint2d("nominalEndOfStateICP", worldFrame, registry);
      nominalReferenceICP = new YoFramePoint2d("nominalReferenceICP", worldFrame, registry);
      nominalReferenceICPVelocity = new YoFrameVector2d("nominalReferenceICPVelocity", worldFrame, registry);
      nominalReferenceCMP = new YoFramePoint2d("nominalReferenceCMP", worldFrame, registry);

      controllerCostToGo = new DoubleYoVariable("costToGo", registry);
      controllerFootstepCostToGo = new DoubleYoVariable("footstepCostToGo", registry);
      controllerFootstepRegularizationCostToGo = new DoubleYoVariable("footstepRegularizationCostToGo", registry);
      controllerFeedbackCostToGo = new DoubleYoVariable("feedbackCostToGo", registry);
      controllerFeedbackRegularizationCostToGo = new DoubleYoVariable("feedbackRegularizationCostToGo", registry);
      controllerDynamicRelaxationCostToGo = new DoubleYoVariable("dynamicRelaxationCostToGo", registry);

      footstepForwardDeadband = new DoubleYoVariable("footstepForwardDeadband", registry);
      footstepLateralDeadband = new DoubleYoVariable("footstepLateralDeadband", registry);
      footstepWasAdjusted = new BooleanYoVariable("footstepWasAdjusted", registry);

      footstepForwardDeadband.set(icpOptimizationParameters.getForwardAdjustmentDeadband());
      footstepLateralDeadband.set(icpOptimizationParameters.getLateralAdjustmentDeadband());

      if (yoGraphicsListRegistry != null)
      {
         String name = "ICPOptimization";

         YoGraphicPosition actualEndOfStateICP = new YoGraphicPosition("actualEndOfStateICP", this.actualEndOfStateICP, 0.005, YoAppearance.Aquamarine(), GraphicType.SOLID_BALL);

         YoGraphicPosition referenceICP = new YoGraphicPosition("controllerReferenceICP", controllerReferenceICP, 0.01, YoAppearance.Yellow(), GraphicType.SOLID_BALL);
         YoGraphicPosition referenceCMP = new YoGraphicPosition("controllerReferenceCMP", controllerReferenceCMP, 0.01, YoAppearance.Beige(), GraphicType.BALL_WITH_CROSS);

         YoGraphicPosition nominalReferenceICP = new YoGraphicPosition("nominalReferenceICP", this.nominalReferenceICP, 0.01, YoAppearance.LightYellow(), GraphicType.BALL);
         YoGraphicPosition nominalEndOfStateICP = new YoGraphicPosition("nominalEndOfStateICP", this.nominalEndOfStateICP, 0.01, YoAppearance.Green(), GraphicType.SOLID_BALL);

         yoGraphicsListRegistry.registerArtifact(name, actualEndOfStateICP.createArtifact());
         yoGraphicsListRegistry.registerArtifact(name, referenceICP.createArtifact());
         yoGraphicsListRegistry.registerArtifact(name, referenceCMP.createArtifact());
         yoGraphicsListRegistry.registerArtifact(name, nominalReferenceICP.createArtifact());
         yoGraphicsListRegistry.registerArtifact(name, nominalEndOfStateICP.createArtifact());
      }
   }

   public void updateCostsToGo(ICPOptimizationSolver solver)
   {
      controllerCostToGo.set(solver.getCostToGo());
      controllerFootstepCostToGo.set(solver.getFootstepCostToGo());
      controllerFootstepRegularizationCostToGo.set(solver.getFootstepRegularizationCostToGo());
      controllerFeedbackCostToGo.set(solver.getFeedbackCostToGo());
      controllerFeedbackRegularizationCostToGo.set(solver.getFeedbackRegularizationCostToGo());
      controllerDynamicRelaxationCostToGo.set(solver.getDynamicRelaxationCostToGo());
   }


   private final FramePoint2d locationSolution = new FramePoint2d();
   private final FramePoint2d upcomingFootstepLocation = new FramePoint2d();

   public void extractFootstepSolutions(ArrayList<YoFramePoint2d> footstepSolutionsToPack, ArrayList<Footstep> upcomingFootsteps, int numberOfFootstepsToConsider,
         ICPOptimizationSolver solver)
   {
      boolean firstStepAdjusted = false;
      for (int i = 0; i < numberOfFootstepsToConsider; i++)
      {
         solver.getFootstepSolutionLocation(i, locationSolution);

         upcomingFootsteps.get(i).getPosition2d(upcomingFootstepLocation);
         ReferenceFrame deadbandFrame = upcomingFootsteps.get(i).getSoleReferenceFrame();

         boolean footstepWasAdjusted = applyLocationDeadband(locationSolution, upcomingFootstepLocation, deadbandFrame, footstepForwardDeadband.getDoubleValue(),
               footstepLateralDeadband.getDoubleValue());

         if (i == 0)
            firstStepAdjusted = footstepWasAdjusted;

         footstepSolutionsToPack.get(i).set(locationSolution);
      }

      this.footstepWasAdjusted.set(firstStepAdjusted);

   }

   private final FramePoint solutionLocation = new FramePoint();
   private final FramePoint referenceLocation = new FramePoint();
   private final FrameVector solutionAdjustment = new FrameVector();

   private boolean applyLocationDeadband(FramePoint2d solutionLocationToPack, FramePoint2d referenceLocation2d, ReferenceFrame deadbandFrame, double forwardDeadband,
         double lateralDeadband)
   {
      solutionLocation.setXYIncludingFrame(solutionLocationToPack);
      referenceLocation.setXYIncludingFrame(referenceLocation2d);

      solutionLocation.changeFrame(deadbandFrame);
      referenceLocation.changeFrame(deadbandFrame);
      solutionAdjustment.setToZero(deadbandFrame);

      solutionAdjustment.set(solutionLocation);
      solutionAdjustment.sub(referenceLocation);

      boolean wasAdjusted = false;

      if (Math.abs(solutionAdjustment.getX()) < forwardDeadband)
      {
         solutionLocation.setX(referenceLocation.getX());
      }
      else
      {
         if (solutionAdjustment.getX() > 0.0)
            solutionLocation.setX(solutionLocation.getX() - forwardDeadband);
         else
            solutionLocation.setX(solutionLocation.getX() + forwardDeadband);

         wasAdjusted = true;
      }

      if (Math.abs(solutionAdjustment.getY()) < lateralDeadband)
      {
         solutionLocation.setY(referenceLocation.getY());
      }
      else
      {
         if (solutionAdjustment.getY() > 0.0)
            solutionLocation.setY(solutionLocation.getY() - lateralDeadband);
         else
            solutionLocation.setY(solutionLocation.getY() + lateralDeadband);

         wasAdjusted = true;
      }

      solutionLocation.changeFrame(solutionLocationToPack.getReferenceFrame());
      solutionLocationToPack.setByProjectionOntoXYPlane(solutionLocation);

      return wasAdjusted;
   }

   private final FramePoint2d tmpEndPoint = new FramePoint2d();
   private final FramePoint2d tmpReferencePoint = new FramePoint2d();
   private final FramePoint2d tmpCMP = new FramePoint2d();
   private final FrameVector2d tmpReferenceVelocity = new FrameVector2d();
   private final FramePoint2d finalICP2d = new FramePoint2d();

   public void computeReferenceFromSolutions(ArrayList<YoFramePoint2d> footstepSolutions, ICPOptimizationInputHandler inputHandler,
         YoFramePoint2d beginningOfStateICP, double omega0, int numberOfFootstepsToConsider)
   {
      computeReferenceFromSolutions(footstepSolutions, inputHandler.getEntryOffsets(), inputHandler.getExitOffsets(), inputHandler.getPreviousStanceExitCMP(),
            inputHandler.getStanceEntryCMP(), inputHandler.getStanceExitCMP(), inputHandler.getFinalICP(), beginningOfStateICP, omega0, numberOfFootstepsToConsider);
   }

   public void computeReferenceFromSolutions(ArrayList<YoFramePoint2d> footstepSolutions, ArrayList<FrameVector2d> entryOffsets, ArrayList<FrameVector2d> exitOffsets,
         FramePoint2d previousStanceExitCMP, FramePoint2d stanceEntryCMP, FramePoint2d stanceExitCMP, FramePoint finalICP, YoFramePoint2d beginningOfStateICP,
         double omega0, int numberOfFootstepsToConsider)
   {
      finalICP.getFrameTuple2d(finalICP2d);
      footstepRecursionMultiplierCalculator.computeICPPoints(finalICP2d, footstepSolutions, entryOffsets, exitOffsets, previousStanceExitCMP, stanceEntryCMP,
            stanceExitCMP, beginningOfStateICP.getFrameTuple2d(), numberOfFootstepsToConsider, tmpEndPoint, tmpReferencePoint, tmpReferenceVelocity);

      CapturePointTools.computeDesiredCentroidalMomentumPivot(tmpReferencePoint, tmpReferenceVelocity, omega0, tmpCMP);

      actualEndOfStateICP.set(tmpEndPoint);
      controllerReferenceICP.set(tmpReferencePoint);
      controllerReferenceICPVelocity.set(tmpReferenceVelocity);
      controllerReferenceCMP.set(tmpCMP);
   }

   public void computeNominalValues(ArrayList<YoFramePoint2d> upcomingFootstepLocations, ICPOptimizationInputHandler inputHandler,
         YoFramePoint2d beginningOfStateICP, double omega0, int numberOfFootstepsToConsider)
   {
      computeNominalValues(upcomingFootstepLocations, inputHandler.getEntryOffsets(), inputHandler.getExitOffsets(), inputHandler.getPreviousStanceExitCMP(),
            inputHandler.getStanceEntryCMP(), inputHandler.getStanceExitCMP(), inputHandler.getFinalICP(), beginningOfStateICP, omega0, numberOfFootstepsToConsider);
   }

   public void computeNominalValues(ArrayList<YoFramePoint2d> upcomingFootstepLocations, ArrayList<FrameVector2d> entryOffsets, ArrayList<FrameVector2d> exitOffsets,
         FramePoint2d previousStanceExitCMP, FramePoint2d stanceEntryCMP, FramePoint2d stanceExitCMP, FramePoint finalICP, YoFramePoint2d beginningOfStateICP,
         double omega0, int numberOfFootstepsToConsider)
   {
      finalICP.getFrameTuple2d(finalICP2d);
      footstepRecursionMultiplierCalculator.computeICPPoints(finalICP2d, upcomingFootstepLocations, entryOffsets, exitOffsets,
            previousStanceExitCMP, stanceEntryCMP, stanceExitCMP, beginningOfStateICP.getFrameTuple2d(), numberOfFootstepsToConsider, tmpEndPoint,
            tmpReferencePoint, tmpReferenceVelocity);

      CapturePointTools.computeDesiredCentroidalMomentumPivot(tmpReferencePoint, tmpReferenceVelocity, omega0, tmpCMP);

      nominalEndOfStateICP.set(tmpEndPoint);
      nominalReferenceICP.set(tmpReferencePoint);
      nominalReferenceICPVelocity.set(tmpReferenceVelocity);
      nominalReferenceCMP.set(tmpCMP);
   }

   public void setValuesForFeedbackOnly(FramePoint2d desiredICP, FrameVector2d desiredICPVelocity, double omega0)
   {
      CapturePointTools.computeDesiredCentroidalMomentumPivot(desiredICP, desiredICPVelocity, omega0, tmpCMP);

      controllerReferenceICP.set(desiredICP);
      controllerReferenceICPVelocity.set(desiredICPVelocity);
      controllerReferenceCMP.set(tmpCMP);
      nominalReferenceICP.set(desiredICP);
      nominalReferenceICPVelocity.set(desiredICPVelocity);
      nominalReferenceCMP.set(tmpCMP);
   }

   public FramePoint2d getControllerReferenceICP()
   {
      return controllerReferenceICP.getFrameTuple2d();
   }

   public void getControllerReferenceCMP(FramePoint2d framePointToPack)
   {
      controllerReferenceCMP.getFrameTuple2d(framePointToPack);
   }

   public boolean wasFootstepAdjusted()
   {
      return footstepWasAdjusted.getBooleanValue();
   }
}

