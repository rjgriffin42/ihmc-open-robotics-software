package us.ihmc.robotics.math.trajectories.waypoints;

import us.ihmc.robotics.geometry.ReferenceFrameHolder;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class FrameTrajectoryPointList<T extends FrameTrajectoryPointList<T, P>, P extends FrameTrajectoryPoint<P>> extends ReferenceFrameHolder
      implements TrajectoryPointListInterface<T, P>
{
   protected ReferenceFrame referenceFrame = ReferenceFrame.getWorldFrame();
   protected final RecyclingArrayList<P> trajectoryPoints;

   public FrameTrajectoryPointList(Class<P> trajectoryPointClass)
   {
      trajectoryPoints = new RecyclingArrayList<>(trajectoryPointClass);
   }

   @Override
   public void clear()
   {
      trajectoryPoints.clear();
   }

   public void clear(ReferenceFrame referenceFrame)
   {
      this.referenceFrame = referenceFrame;
      trajectoryPoints.clear();
   }

   @Override
   public void addTrajectoryPoint(P trajectoryPoint)
   {
      P newTrajectoryPoint = addAndInitializeTrajectoryPoint();
      newTrajectoryPoint.set(trajectoryPoint);
   }

   public void addTrajectoryPointAndMatchFrame(P trajectoryPoint)
   {
      P newTrajectoryPoint = addAndInitializeTrajectoryPoint();
      newTrajectoryPoint.setIncludingFrame(trajectoryPoint);
      newTrajectoryPoint.changeFrame(referenceFrame);
   }

   @Override
   public void set(T other)
   {
      checkReferenceFrameMatch(other);
      clear();
      for (int i = 0; i < other.getNumberOfTrajectoryPoints(); i++)
      {
         P newTrajectoryPoint = addAndInitializeTrajectoryPoint();
         newTrajectoryPoint.set(other.trajectoryPoints.get(i));
      }
   }

   public void setIncludingFrame(T other)
   {
      clear(other.referenceFrame);
      for (int i = 0; i < other.getNumberOfTrajectoryPoints(); i++)
      {
         P newTrajectoryPoint = addAndInitializeTrajectoryPoint();
         // Here we don't want to do setIncludingFrame() in case there is inconsistency in other.
         newTrajectoryPoint.set(other.trajectoryPoints.get(i));
      }
   }

   protected P addAndInitializeTrajectoryPoint()
   {
      P newTrajectoryPoint = trajectoryPoints.add();
      newTrajectoryPoint.setToZero(referenceFrame);
      return newTrajectoryPoint;
   }

   public void changeFrame(ReferenceFrame referenceFrame)
   {
      if (this.referenceFrame == referenceFrame)
         return;

      for (int i = 0; i < trajectoryPoints.size(); i++)
         trajectoryPoints.get(i).changeFrame(referenceFrame);
      this.referenceFrame = referenceFrame;
   }

   @Override
   public int getNumberOfTrajectoryPoints()
   {
      return trajectoryPoints.size();
   }

   @Override
   public P getTrajectoryPoint(int trajectoryPointIndex)
   {
      return trajectoryPoints.get(trajectoryPointIndex);
   }

   @Override
   public P getLastTrajectoryPoint()
   {
      return trajectoryPoints.getLast();
   }

   @Override
   public double getTrajectoryTime()
   {
      return getLastTrajectoryPoint().getTime();
   }

   @Override
   public ReferenceFrame getReferenceFrame()
   {
      return referenceFrame;
   }

   @Override
   public boolean epsilonEquals(T other, double epsilon)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         return false;
      if (referenceFrame != other.referenceFrame)
         return false;
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         P thisTrajectoryPoint = trajectoryPoints.get(i);
         P otherTrajectoryPoint = other.trajectoryPoints.get(i);
         if (!thisTrajectoryPoint.epsilonEquals(otherTrajectoryPoint, epsilon))
            return false;
      }
      return true;
   }

   @Override
   public String toString()
   {
      if (trajectoryPoints != null)
         return "Frame trajectory: number of frame trajectory points = " + getNumberOfTrajectoryPoints() + ".";
      else
         return "Frame trajectory: no frame trajectory point.";
   }
}
