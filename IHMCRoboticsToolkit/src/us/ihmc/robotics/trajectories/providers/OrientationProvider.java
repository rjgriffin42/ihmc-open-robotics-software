package us.ihmc.robotics.trajectories.providers;

import us.ihmc.robotics.geometry.FrameOrientation;

public interface OrientationProvider
{
   public abstract void getOrientation(FrameOrientation orientationToPack);
}