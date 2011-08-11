package us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP.capturePointCenterOfPressure;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.CapturePointCenterOfPressureControlModule;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class DoNothingCapturePointCenterOfPressureControlModule implements CapturePointCenterOfPressureControlModule
{
   private ReferenceFrame midFeetZUpFrame;

   public DoNothingCapturePointCenterOfPressureControlModule(ReferenceFrame midFeetZUpFrame)
   {
      this.midFeetZUpFrame = midFeetZUpFrame;
   }
   
   public void controlDoubleSupport(BipedSupportPolygons bipedSupportPolygons, FramePoint currentCapturePoint, FramePoint desiredCapturePoint,
                                    FramePoint centerOfMassPositionInWorldFrame, FrameVector2d desiredVelocity, FrameVector2d currentVelocity)
   {
   }

   public void controlSingleSupport(RobotSide supportLeg, BipedSupportPolygons supportPolygons, FramePoint currentCapturePoint, FrameVector2d desiredVelocity,
                                    FrameLineSegment2d guideLine, FramePoint desiredCapturePoint, FramePoint centerOfMassPositionInZUpFrame,
                                    FrameVector2d currentVelocity)
   {
   }

   public void packDesiredCenterOfPressure(FramePoint desiredCenterOfPressureToPack)
   {
      desiredCenterOfPressureToPack.setToZero(midFeetZUpFrame);
   }

}
