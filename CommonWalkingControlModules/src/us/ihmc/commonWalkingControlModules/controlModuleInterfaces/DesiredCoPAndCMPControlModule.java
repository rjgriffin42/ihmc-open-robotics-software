package us.ihmc.commonWalkingControlModules.controlModuleInterfaces;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;

public interface DesiredCoPAndCMPControlModule
{
   public abstract void compute(FramePoint2d capturePoint, RobotSide supportLeg, FramePoint2d desiredCapturePoint, FrameVector2d desiredCapturePointVelocity, double desiredPelvisRoll, double desiredPelvisPitch, double omega0);
   public abstract void packCoP(FramePoint2d cop);
   public abstract void packCMP(FramePoint2d cmp);
}
