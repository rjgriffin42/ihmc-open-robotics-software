package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationController;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.*;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class ICPOptimizationLinearMomentumRateOfChangeControlModule extends LinearMomentumRateOfChangeControlModule
{
   private final ICPOptimizationController icpOptimizationController;
   private final DoubleYoVariable yoTime;

   public ICPOptimizationLinearMomentumRateOfChangeControlModule(CommonHumanoidReferenceFrames referenceFrames, BipedSupportPolygons bipedSupportPolygons,
         SideDependentList<? extends ContactablePlaneBody> contactableFeet, CapturePointPlannerParameters icpPlannerParameters,
         ICPOptimizationParameters icpOptimizationParameters, DoubleYoVariable yoTime, double totalMass, double gravityZ, double controlDT,
         YoVariableRegistry parentRegistry, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      super("ICPOptimization", referenceFrames, bipedSupportPolygons, gravityZ, totalMass, parentRegistry);

      this.yoTime = yoTime;

      MathTools.checkIfInRange(gravityZ, 0.0, Double.POSITIVE_INFINITY);

      icpOptimizationController = new ICPOptimizationController(icpPlannerParameters, icpOptimizationParameters, bipedSupportPolygons, contactableFeet,
            controlDT, registry, yoGraphicsListRegistry);
   }

   public void setDoubleSupportDuration(double doubleSupportDuration)
   {
      icpOptimizationController.setDoubleSupportDuration(doubleSupportDuration);
   }

   public void setSingleSupportDuration(double singleSupportDuration)
   {
      icpOptimizationController.setSingleSupportDuration(singleSupportDuration);
   }

   public void clearPlan()
   {
      icpOptimizationController.clearPlan();
   }

   public void addFootstepToPlan(Footstep footstep)
   {
      icpOptimizationController.addFootstepToPlan(footstep);
   }

   public void initializeForStanding()
   {
      icpOptimizationController.initializeForStanding(yoTime.getDoubleValue());
   }

   public void initializeForSingleSupport()
   {
      icpOptimizationController.initializeForSingleSupport(yoTime.getDoubleValue(), supportSide, omega0);
   }

   public void initializeForTransfer()
   {
      icpOptimizationController.initializeForTransfer(yoTime.getDoubleValue(), transferToSide, omega0);
   }

   @Override
   public void computeCMPInternal(FramePoint2d desiredCMPPreviousValue)
   {
      icpOptimizationController.compute(yoTime.getDoubleValue(), desiredCapturePoint, desiredCapturePointVelocity, capturePoint, omega0);
      icpOptimizationController.getDesiredCMP(desiredCMP);
   }

   private final FramePose footstepPose = new FramePose();
   private final FramePoint2d footstepPositionSolution = new FramePoint2d();

   @Override public boolean getUpcomingFootstepSolution(Footstep footstepToPack)
   {
      if (icpOptimizationController.getNumberOfFootstepsToConsider() > 0)
      {
         footstepToPack.getPose(footstepPose);
         icpOptimizationController.getFootstepSolution(0, footstepPositionSolution);
         footstepPose.setXYFromPosition2d(footstepPositionSolution);
         footstepToPack.setPose(footstepPose);
      }

      return icpOptimizationController.wasFootstepAdjusted();
   }

   public void setCMPProjectionArea(FrameConvexPolygon2d areaToProjectInto, FrameConvexPolygon2d safeArea)
   {}

}
