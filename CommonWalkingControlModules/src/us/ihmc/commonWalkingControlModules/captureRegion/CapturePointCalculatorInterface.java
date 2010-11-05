package us.ihmc.commonWalkingControlModules.captureRegion;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public interface CapturePointCalculatorInterface
{
   public abstract void computeCapturePoint(RobotSide supportSide);

   public abstract FramePoint2d getCapturePoint2dInFrame(ReferenceFrame referenceFrame);

   public FramePoint computePredictedCapturePoint(RobotSide supportLeg, double captureTime, FramePoint centerOfPressure);

   public abstract FramePoint getPredictedCapturePointInFrame(ReferenceFrame referenceFrame);
   public abstract FramePoint getCapturePointInFrame(ReferenceFrame referenceFrame);


}
