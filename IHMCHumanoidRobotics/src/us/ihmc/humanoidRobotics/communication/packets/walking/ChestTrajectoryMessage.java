package us.ihmc.humanoidRobotics.communication.packets.walking;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiMessage;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.humanoidRobotics.communication.packets.SO3TrajectoryPointMessage;
import us.ihmc.robotics.geometry.RigidBodyTransform;

@ClassDocumentation("This message commands the controller to move in taskspace the chest to the desired orientation while going through the specified trajectory points."
      + " A hermite based curve (third order) is used to interpolate the orientations."
      + " To excute a simple trajectory to reach a desired chest orientation, set only one trajectory point with zero velocity and its time to be equal to the desired trajectory time."
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule does not apply to the fields of this message.")
public class ChestTrajectoryMessage extends IHMCRosApiMessage<ChestTrajectoryMessage>
      implements TransformableDataObject<ChestTrajectoryMessage>, VisualizablePacket
{
   @FieldDocumentation("List of trajectory points (in taskpsace) to go through while executing the trajectory. All the information contained in these trajectory points needs to be expressed in world frame.")
   public SO3TrajectoryPointMessage[] taskspaceTrajectoryPoints;

   /**
    * Empty constructor for serialization.
    */
   public ChestTrajectoryMessage()
   {
      setUniqueId(1L);
   }

   public ChestTrajectoryMessage(ChestTrajectoryMessage chestTrajectoryMessage)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      setDestination(chestTrajectoryMessage.getDestination());
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[chestTrajectoryMessage.getNumberOfTrajectoryPoints()];
      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         taskspaceTrajectoryPoints[i] = new SO3TrajectoryPointMessage(chestTrajectoryMessage.taskspaceTrajectoryPoints[i]);
   }

   /**
    * Use this constructor to execute a simple interpolation in taskspace to the desired orientation.
    * @param trajectoryTime how long it takes to reach the desired orientation.
    * @param desiredOrientation desired chest orientation expressed in world frame.
    */
   public ChestTrajectoryMessage(double trajectoryTime, Quat4d desiredOrientation)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      Vector3d zeroAngularVelocity = new Vector3d();
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[] {new SO3TrajectoryPointMessage(trajectoryTime, desiredOrientation, zeroAngularVelocity)};
   }

   /**
    * Use this constructor to build a message with more than one trajectory point.
    * This constructor only allocates memory for the trajectory points, you need to call {@link #setTrajectoryPoint(int, double, Quat4d, Vector3d)} for each trajectory point afterwards.
    * @param numberOfTrajectoryPoints number of trajectory points that will be sent to the controller.
    */
   public ChestTrajectoryMessage(int numberOfTrajectoryPoints)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      taskspaceTrajectoryPoints = new SO3TrajectoryPointMessage[numberOfTrajectoryPoints];
   }

   /**
    * Create a trajectory point.
    * @param trajectoryPointIndex index of the trajectory point to create.
    * @param time time at which the trajectory point has to be reached. The time is relative to when the trajectory starts.
    * @param orientation define the desired 3D orientation to be reached at this trajectory point. It is expressed in world frame.
    * @param angularVelocity define the desired 3D angular velocity to be reached at this trajectory point. It is expressed in world frame.
    */
   public void setTrajectoryPoint(int trajectoryPointIndex, double time, Quat4d orientation, Vector3d angularVelocity)
   {
      rangeCheck(trajectoryPointIndex);
      taskspaceTrajectoryPoints[trajectoryPointIndex] = new SO3TrajectoryPointMessage(time, orientation, angularVelocity);
   }

   public int getNumberOfTrajectoryPoints()
   {
      return taskspaceTrajectoryPoints.length;
   }

   public SO3TrajectoryPointMessage getTrajectoryPoint(int trajectoryPointIndex)
   {
      rangeCheck(trajectoryPointIndex);
      return taskspaceTrajectoryPoints[trajectoryPointIndex];
   }

   public SO3TrajectoryPointMessage[] getTrajectoryPoints()
   {
      return taskspaceTrajectoryPoints;
   }

   private void rangeCheck(int trajectoryPointIndex)
   {
      if (trajectoryPointIndex >= getNumberOfTrajectoryPoints() || trajectoryPointIndex < 0)
         throw new IndexOutOfBoundsException("Trajectory point index: " + trajectoryPointIndex + ", number of trajectory points: " + getNumberOfTrajectoryPoints());
   }

   @Override
   public boolean epsilonEquals(ChestTrajectoryMessage other, double epsilon)
   {
      if (getNumberOfTrajectoryPoints() != other.getNumberOfTrajectoryPoints())
         return false;

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
      {
         if (!taskspaceTrajectoryPoints[i].epsilonEquals(other.taskspaceTrajectoryPoints[i], epsilon))
            return false;
      }

      return true;
   }

   @Override
   public ChestTrajectoryMessage transform(RigidBodyTransform transform)
   {
      ChestTrajectoryMessage transformedChestTrajectoryMessage = new ChestTrajectoryMessage(getNumberOfTrajectoryPoints());

      for (int i = 0; i < getNumberOfTrajectoryPoints(); i++)
         transformedChestTrajectoryMessage.taskspaceTrajectoryPoints[i] = taskspaceTrajectoryPoints[i].transform(transform);

      return transformedChestTrajectoryMessage;
   }

   @Override
   public String toString()
   {
      if (taskspaceTrajectoryPoints != null)
         return "Chest SO3 trajectory: number of SO3 trajectory points = " + getNumberOfTrajectoryPoints();
      else
         return "Chest SO3 trajectory: no SO3 trajectory points";
   }
}
