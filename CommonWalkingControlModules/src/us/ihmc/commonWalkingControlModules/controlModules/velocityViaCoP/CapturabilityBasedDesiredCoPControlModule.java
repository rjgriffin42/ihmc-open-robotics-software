package us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCapturePointCalculator;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCapturePointToDesiredCoPControlModule;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCenterOfPressureFilter;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCoPControlModule;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.GuideLineCalculator;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.GuideLineToDesiredCoPControlModule;
import us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait.SingleSupportCondition;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class CapturabilityBasedDesiredCoPControlModule implements DesiredCoPControlModule
{
   private final UseGuideLineDecider guideLineOrCapturePointSingleSupportDecider;
   private final DesiredCapturePointCalculator desiredCapturePointCalculator;
   private final DesiredCapturePointToDesiredCoPControlModule desiredCapturePointToDesiredCoPControlModule;
   private final GuideLineCalculator guideLineCalculator;
   private final GuideLineToDesiredCoPControlModule guideLineToDesiredCoPControlModule;
   private final DesiredCenterOfPressureFilter desiredCenterOfPressureFilter;
   private final CapturabilityBasedDesiredCoPVisualizer visualizer;

   private final CouplingRegistry couplingRegistry;
   private final CommonWalkingReferenceFrames referenceFrames;

   public CapturabilityBasedDesiredCoPControlModule(UseGuideLineDecider guideLineOrCapturePointSingleSupportDecider,
         DesiredCapturePointCalculator desiredCapturePointCalculator,
         DesiredCapturePointToDesiredCoPControlModule desiredCapturePointToDesiredCoPControlModule, GuideLineCalculator guideLineCalculator,
         GuideLineToDesiredCoPControlModule guideLineToDesiredCoPControlModule, DesiredCenterOfPressureFilter desiredCenterOfPressureFilter,
         CapturabilityBasedDesiredCoPVisualizer visualizer, CouplingRegistry couplingRegistry, CommonWalkingReferenceFrames referenceFrames)
   {
      this.guideLineOrCapturePointSingleSupportDecider = guideLineOrCapturePointSingleSupportDecider;
      this.desiredCapturePointCalculator = desiredCapturePointCalculator;
      this.desiredCapturePointToDesiredCoPControlModule = desiredCapturePointToDesiredCoPControlModule;
      this.guideLineCalculator = guideLineCalculator;
      this.guideLineToDesiredCoPControlModule = guideLineToDesiredCoPControlModule;
      this.desiredCenterOfPressureFilter = desiredCenterOfPressureFilter;
      this.visualizer = visualizer;
      this.couplingRegistry = couplingRegistry;
      this.referenceFrames = referenceFrames;
   }

   public FramePoint2d computeDesiredCoPSingleSupport(RobotSide supportLeg, FrameVector2d desiredVelocity, SingleSupportCondition singleSupportCondition,
         double timeInState)
   {
      BipedSupportPolygons bipedSupportPolygons = couplingRegistry.getBipedSupportPolygons();
      ReferenceFrame ankleZUpFrame = referenceFrames.getAnkleZUpFrame(supportLeg);
      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(ankleZUpFrame).toFramePoint2d();
      FramePoint finalDesiredSwingTarget = couplingRegistry.getDesiredFootstep().getPositionInFrame(ankleZUpFrame);
      FramePoint2d ret = new FramePoint2d(ankleZUpFrame);

      if (guideLineOrCapturePointSingleSupportDecider.useGuideLine(singleSupportCondition, timeInState, desiredVelocity))
      {
         guideLineCalculator.update(supportLeg, bipedSupportPolygons, capturePoint, finalDesiredSwingTarget, desiredVelocity);
         FrameLineSegment2d guideLine = guideLineCalculator.getGuideLine(supportLeg);
         visualizer.setGuideLine(guideLine);
         ret = guideLineToDesiredCoPControlModule.computeDesiredCoPSingleSupport(supportLeg, bipedSupportPolygons, capturePoint, desiredVelocity, guideLine);
      } else
      {
         FramePoint2d desiredCapturePoint = desiredCapturePointCalculator.computeDesiredCapturePointSingleSupport(supportLeg, bipedSupportPolygons,
               desiredVelocity, singleSupportCondition);
         visualizer.setDesiredCapturePoint(desiredCapturePoint);
         ret = desiredCapturePointToDesiredCoPControlModule.computeDesiredCoPSingleSupport(supportLeg, bipedSupportPolygons, capturePoint, desiredVelocity,
               desiredCapturePoint, new FrameVector2d(desiredCapturePoint.getReferenceFrame()));
      }

      ret = desiredCenterOfPressureFilter.filter(ret, supportLeg);
      visualizer.setDesiredCoP(ret);

      return ret;
   }

   public FramePoint2d computeDesiredCoPDoubleSupport(RobotSide loadingLeg, FrameVector2d desiredVelocity)
   {
      BipedSupportPolygons bipedSupportPolygons = couplingRegistry.getBipedSupportPolygons();
      ReferenceFrame midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();
      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(midFeetZUpFrame).toFramePoint2d();
      FramePoint2d ret = new FramePoint2d(midFeetZUpFrame);

      FramePoint2d desiredCapturePoint = desiredCapturePointCalculator.computeDesiredCapturePointDoubleSupport(loadingLeg, bipedSupportPolygons,
            desiredVelocity);
      visualizer.setDesiredCapturePoint(desiredCapturePoint);
      ret = desiredCapturePointToDesiredCoPControlModule.computeDesiredCoPDoubleSupport(bipedSupportPolygons, capturePoint, desiredVelocity,
            desiredCapturePoint, new FrameVector2d(desiredCapturePoint.getReferenceFrame()));
      ret = desiredCenterOfPressureFilter.filter(ret, null);
      visualizer.setDesiredCoP(ret);

      return ret;
   }
}
