package us.ihmc.robotics.math.trajectories.providers;

import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.trajectories.providers.PositionProvider;


public class YoPositionProvider implements PositionProvider
{
   private final YoFramePoint framePoint;

   public YoPositionProvider(YoFramePoint yoFramePoint)
   {
      this.framePoint = yoFramePoint;
   }

   public void getPosition(FramePoint3D positionToPack)
   {
      framePoint.getFrameTupleIncludingFrame(positionToPack);
   }

   public void set(FramePoint3D framePoint)
   {
      this.framePoint.set(framePoint);
   }
}
