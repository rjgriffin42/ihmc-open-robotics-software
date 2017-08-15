package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;


import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.AbstractICPOptimizationController;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;

public class ICPBasedLinearMomentumRateOfChangeControlModule extends LinearMomentumRateOfChangeControlModule
{
   private final ICPProportionalController icpProportionalController;
   private final BipedSupportPolygons bipedSupportPolygons;

   public ICPBasedLinearMomentumRateOfChangeControlModule(CommonHumanoidReferenceFrames referenceFrames, BipedSupportPolygons bipedSupportPolygons,
         double controlDT, double totalMass, double gravityZ, YoICPControlGains icpControlGains, YoVariableRegistry parentRegistry,
         YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this(referenceFrames, bipedSupportPolygons, controlDT, totalMass, gravityZ, icpControlGains, parentRegistry, yoGraphicsListRegistry, true);
   }

   public ICPBasedLinearMomentumRateOfChangeControlModule(CommonHumanoidReferenceFrames referenceFrames, BipedSupportPolygons bipedSupportPolygons,
         double controlDT, double totalMass, double gravityZ, YoICPControlGains icpControlGains, YoVariableRegistry parentRegistry,
         YoGraphicsListRegistry yoGraphicsListRegistry, boolean use2DProjection)
   {
      super("", referenceFrames, gravityZ, totalMass, parentRegistry, yoGraphicsListRegistry, use2DProjection);
      this.bipedSupportPolygons = bipedSupportPolygons;


      icpProportionalController = new ICPProportionalController(icpControlGains, controlDT, registry);


   }

   public void computeCMPInternal(FramePoint2D desiredCMPPreviousValue)
   {
      if (supportSide != supportLegPreviousTick.getEnumValue())
      {
         icpProportionalController.reset();
      }

      desiredCMP.set(icpProportionalController.doProportionalControl(desiredCMPPreviousValue, capturePoint, desiredCapturePoint,
            finalDesiredCapturePoint, desiredCapturePointVelocity, perfectCMP, omega0));

      yoUnprojectedDesiredCMP.set(desiredCMP);

      // do projection here:
      if (!areaToProjectInto.isEmpty())
      {
         desiredCMPinSafeArea.set(safeArea.isPointInside(desiredCMP));
         if (safeArea.isPointInside(desiredCMP))
         {
            supportPolygon.setIncludingFrameAndUpdate(bipedSupportPolygons.getSupportPolygonInMidFeetZUp());
            areaToProjectInto.setIncludingFrameAndUpdate(supportPolygon);
         }

         cmpProjector.projectCMPIntoSupportPolygonIfOutside(capturePoint, areaToProjectInto, finalDesiredCapturePoint, desiredCMP);
         if (cmpProjector.getWasCMPProjected())
            icpProportionalController.bleedOffIntegralTerm();
      }
   }

   @Override
   public void clearPlan(){}

   @Override
   public void addFootstepToPlan(Footstep footstep, FootstepTiming timing){}

   @Override
   public void setFinalTransferDuration(double finalTransferDuration){}

   @Override
   public void initializeForStanding(){}

   @Override
   public void initializeForSingleSupport(){}

   @Override
   public void initializeForTransfer(){}

   @Override
   public void submitRemainingTimeInSwingUnderDisturbance(double remainingTimeInSwing) {}

   @Override
   public boolean getUpcomingFootstepSolution(Footstep footstepToPack)
   {
      return false;
   }

   @Override
   public AbstractICPOptimizationController getICPOptimizationController()
   {
      return null;
   }

   @Override
   public double getOptimizedTimeRemaining()
   {
      throw new RuntimeException("This method is not implemented in planner based momentum control module.");
   }

   @Override
   public void setReferenceICPVelocity(FrameVector2D referenceICPVelocity) {}
}
