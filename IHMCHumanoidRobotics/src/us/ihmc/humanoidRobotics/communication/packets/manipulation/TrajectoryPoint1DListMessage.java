package us.ihmc.humanoidRobotics.communication.packets.manipulation;

import java.util.Random;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiMessage;
import us.ihmc.humanoidRobotics.communication.packets.TrajectoryPoint1DMessage;
import us.ihmc.robotics.math.trajectories.waypoints.TrajectoryPointListInterface;
import us.ihmc.robotics.math.trajectories.waypoints.TrajectoryPoint1DInterface;
import us.ihmc.robotics.random.RandomTools;

@ClassDocumentation("This class is used to build trajectory messages in jointspace. It holds all the trajectory points to go through with a one-dimensional trajectory."
      + " A third order polynomial function is used to interpolate between trajectory points.")
public class TrajectoryPoint1DListMessage extends IHMCRosApiMessage<TrajectoryPoint1DListMessage>
      implements TrajectoryPointListInterface<TrajectoryPoint1DListMessage, TrajectoryPoint1DMessage>
{
   @FieldDocumentation("List of trajectory points to go through while executing the trajectory.")
   public TrajectoryPoint1DMessage[] trajectoryPoints;

   /**
    * Empty constructor for serialization.
    */
   public TrajectoryPoint1DListMessage()
   {
   }

   public TrajectoryPoint1DListMessage(TrajectoryPointListInterface<?, ? extends TrajectoryPoint1DInterface<?>> trajectory1dMessage)
   {
      trajectoryPoints = new TrajectoryPoint1DMessage[trajectory1dMessage.getNumberOfTrajectoryPoints()];
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         trajectoryPoints[i] = new TrajectoryPoint1DMessage(trajectory1dMessage.getTrajectoryPoint(i));
   }

   /**
    * Use this constructor to go straight to the given end point.
    * @param trajectoryTime how long it takes to reach the desired position.
    * @param desiredPosition desired end point position.
    */
   public TrajectoryPoint1DListMessage(double trajectoryTime, double desiredPosition)
   {
      trajectoryPoints = new TrajectoryPoint1DMessage[] {new TrajectoryPoint1DMessage(trajectoryTime, desiredPosition, 0.0)};
   }

   /**
    * Use this constructor to build a message with more than one trajectory points.
    * This constructor only allocates memory for the trajectory points, you need to call {@link #setTrajectoryPoint(int, double, double, double)} for each trajectory point afterwards.
    * @param numberOfTrajectoryPoints number of trajectory points that will be sent to the controller.
    */
   public TrajectoryPoint1DListMessage(int numberOfTrajectoryPoints)
   {
      trajectoryPoints = new TrajectoryPoint1DMessage[numberOfTrajectoryPoints];
   }

   @Override
   public void clear()
   {
      throw new RuntimeException("This cannot implement the method clear().");
   }

   @Override
   public void addTrajectoryPoint(TrajectoryPoint1DMessage trajectoryPoint)
   {
      throw new RuntimeException("This cannot implement the method addTrajectoryPoint().");
   }

   /**
    * Create a trajectory point.
    * @param trajectoryPointIndex index of the trajectory point to create.
    * @param time time at which the trajectory point has to be reached. The time is relative to when the trajectory starts.
    * @param position define the desired 1D position to be reached at this trajectory point.
    * @param velocity define the desired 1D velocity to be reached at this trajectory point.
    */
   public void setTrajectoryPoint(int trajectoryPointIndex, double time, double position, double velocity)
   {
      rangeCheck(trajectoryPointIndex);
      trajectoryPoints[trajectoryPointIndex] = new TrajectoryPoint1DMessage(time, position, velocity);
   }

   @Override
   public void set(TrajectoryPoint1DListMessage other)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         throw new RuntimeException("Trajectory messages do not have the same number of trajectory points.");

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         trajectoryPoints[i].set(other.trajectoryPoints[i]);
   }

   @Override
   public int getNumberOfTrajectoryPoints()
   {
      return trajectoryPoints.length;
   }

   @Override
   public TrajectoryPoint1DMessage getTrajectoryPoint(int trajectoryPointIndex)
   {
      return trajectoryPoints[trajectoryPointIndex];
   }

   public TrajectoryPoint1DMessage[] getTrajectoryPoints()
   {
      return trajectoryPoints;
   }

   @Override
   public TrajectoryPoint1DMessage getLastTrajectoryPoint()
   {
      return trajectoryPoints[getNumberOfTrajectoryPoints() - 1];
   }

   @Override
   public double getTrajectoryTime()
   {
      return getLastTrajectoryPoint().getTime();
   }

   private void rangeCheck(int trajectoryPointIndex)
   {
      if (trajectoryPointIndex >= getNumberOfTrajectoryPoints() || trajectoryPointIndex < 0)
         throw new IndexOutOfBoundsException("Trajectory point index: " + trajectoryPointIndex + ", number of trajectory points: " + getNumberOfTrajectoryPoints());
   }

   @Override
   public boolean epsilonEquals(TrajectoryPoint1DListMessage other, double epsilon)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         return false;

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         if (!trajectoryPoints[i].epsilonEquals(other.trajectoryPoints[i], epsilon))
            return false;
      }

      return true;
   }

   public TrajectoryPoint1DListMessage(Random random)
   {
      this(random.nextInt(16) + 1);

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         double time = RandomTools.generateRandomDoubleWithEdgeCases(random, 0.01);
         double position = RandomTools.generateRandomDoubleWithEdgeCases(random, 0.01);
         double velocity = RandomTools.generateRandomDoubleWithEdgeCases(random, 0.01);
         setTrajectoryPoint(i, time, position, velocity);
      }
   }

   @Override
   public String toString()
   {
      if (trajectoryPoints != null)
         return "Trajectory 1D: number of 1D trajectory points = " + getNumberOfTrajectoryPoints() + ".";
      else
         return "Trajectory 1D: no 1D trajectory point.";
   }
}
