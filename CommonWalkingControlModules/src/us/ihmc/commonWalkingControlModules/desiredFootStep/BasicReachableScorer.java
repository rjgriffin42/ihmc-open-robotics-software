package us.ihmc.commonWalkingControlModules.desiredFootStep;

import us.ihmc.commonWalkingControlModules.captureRegion.CaptureRegionCalculator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;


public class BasicReachableScorer implements StepLocationScorer
{
   protected final CaptureRegionCalculator captureRegionCalculator;

   public BasicReachableScorer(CaptureRegionCalculator captureRegionCalculator)
   {
      this.captureRegionCalculator = captureRegionCalculator;
   }

   public double getStepLocationScore(RobotSide supportLeg, FramePose desiredFootPose)
   {
      FrameConvexPolygon2d reachableRegion = captureRegionCalculator.getReachableRegion(supportLeg);
      if (reachableRegion.isPointInside(new FramePoint2d(desiredFootPose.getReferenceFrame(), desiredFootPose.getX(), desiredFootPose.getY())))
         return 1.0;
      else
         return 0.0;
   }
}
