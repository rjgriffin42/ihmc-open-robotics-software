package us.ihmc.robotics.trajectories.providers;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;

public class ConstantConfigurationProvider implements SE3ConfigurationProvider
{
   private final FramePose configuration;

   public ConstantConfigurationProvider(ReferenceFrame referenceFrame)
   {
      this.configuration = new FramePose(referenceFrame);
   }

   public ConstantConfigurationProvider(FramePoint framePoint)
   {
      configuration = new FramePose(framePoint, new FrameOrientation(framePoint.getReferenceFrame()));
   }

   public ConstantConfigurationProvider(FramePose framePose)
   {
      this.configuration = new FramePose(framePose);
   }

   public void getPosition(FramePoint positionToPack)
   {
      configuration.getPositionIncludingFrame(positionToPack);
   }

   public void getOrientation(FrameOrientation orientationToPack)
   {
      configuration.getOrientationIncludingFrame(orientationToPack);
   }
}
