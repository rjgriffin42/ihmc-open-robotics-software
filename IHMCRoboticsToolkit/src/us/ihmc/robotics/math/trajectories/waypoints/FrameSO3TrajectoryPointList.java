package us.ihmc.robotics.math.trajectories.waypoints;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.math.trajectories.waypoints.interfaces.SO3TrajectoryPointInterface;
import us.ihmc.robotics.math.trajectories.waypoints.interfaces.TrajectoryPointListInterface;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class FrameSO3TrajectoryPointList extends FrameTrajectoryPointList<SimpleSO3TrajectoryPoint, FrameSO3TrajectoryPoint, FrameSO3TrajectoryPointList>
{
   public FrameSO3TrajectoryPointList()
   {
      super(FrameSO3TrajectoryPoint.class);
   }

   public <T extends TrajectoryPointListInterface<? extends SO3TrajectoryPointInterface<?>, T>> void setIncludingFrame(ReferenceFrame referenceFrame, T trajectoryPointList)
   {
      clear(referenceFrame);
      for (int i = 0; i < trajectoryPointList.getNumberOfTrajectoryPoints(); i++)
         addTrajectoryPoint(trajectoryPointList.getLastTrajectoryPoint());
   }

   public void addTrajectoryPoint(double time, Quat4d orientation, Vector3d angularVelocity)
   {
      FrameSO3TrajectoryPoint newTrajectoryPoint = addAndInitializeTrajectoryPoint();
      newTrajectoryPoint.set(time, orientation, angularVelocity);
   }

   public void addTrajectoryPoint(SO3TrajectoryPointInterface<?> trajectoryPoint)
   {
      FrameSO3TrajectoryPoint newTrajectoryPoint = addAndInitializeTrajectoryPoint();
      newTrajectoryPoint.set(trajectoryPoint);
   }

   @Override
   public String toString()
   {
      if (trajectoryPoints != null)
         return "Frame SO3 trajectory: number of frame SO3 trajectory points = " + getNumberOfTrajectoryPoints() + ".";
      else
         return "Frame SO3 trajectory: no frame SO3 trajectory point.";
   }
}
