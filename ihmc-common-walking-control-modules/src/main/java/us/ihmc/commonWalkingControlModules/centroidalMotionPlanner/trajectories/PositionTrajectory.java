package us.ihmc.commonWalkingControlModules.centroidalMotionPlanner.trajectories;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.robotics.math.trajectories.FrameTrajectory3D;
import us.ihmc.robotics.math.trajectories.SegmentedFrameTrajectory3D;

public class PositionTrajectory extends SegmentedFrameTrajectory3D
{
   public PositionTrajectory(int maxNumberOfSegments, int maxNumberOfCoefficients)
   {
      super(maxNumberOfSegments, maxNumberOfCoefficients);
   }

   public void set(FrameTrajectory3D trajectory)
   {
      FrameTrajectory3D newSegment = segments.add();
      newSegment.set(trajectory);
   }

   public void update(double time, FramePoint3D position)
   {
      update(time);
      position.setIncludingFrame(currentSegment.getFramePosition());
   }
   
   public void update(double time, FramePoint3D position, FrameVector3D velocity)
   {
      update(time);
      position.setIncludingFrame(currentSegment.getFramePosition());
      velocity.setIncludingFrame(currentSegment.getFrameVelocity());
   }
}