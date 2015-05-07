package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.utilities.humanoidRobot.footstep.Footstep;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FrameVector;

public interface SwingStateInterface
{

   public abstract void setFootstep(Footstep footstep);

   public abstract void replanTrajectory(Footstep footstep);

   public abstract void requestSwingSpeedUp(double speedUpFactor);

   public abstract void setInitialDesireds(FrameOrientation initialOrientation, FrameVector initialAngularVelocity);

}