package us.ihmc.humanoidRobotics.communication.packets.walking;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiMessage;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.humanoidRobotics.communication.packets.Waypoint1DMessage;

@ClassDocumentation("This mesage commands the controller to move the pelvis to a new height in world while going through the specified waypoints."
      + " Sending this command will not affect the pelvis horizontal position. To control the pelvis 3D position use the PelvisTrajectoryMessage instead."
      + " A third order polynomial is used to interpolate between waypoints."
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule does not apply to the fields of this message.")
public class PelvisHeightTrajectoryMessage extends IHMCRosApiMessage<PelvisHeightTrajectoryMessage> implements VisualizablePacket
{
   @FieldDocumentation("List of waypoints to go through while executing the trajectory.")
   public Waypoint1DMessage[] waypoints;

   /**
    * Empty constructor for serialization.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    */
   public PelvisHeightTrajectoryMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   /**
    * Clone contructor.
    * @param pelvisHeightTrajectoryMessage message to clone.
    */
   public PelvisHeightTrajectoryMessage(PelvisHeightTrajectoryMessage pelvisHeightTrajectoryMessage)
   {
      setUniqueId(pelvisHeightTrajectoryMessage.getUniqueId());
      setDestination(pelvisHeightTrajectoryMessage.getDestination());
      waypoints = new Waypoint1DMessage[pelvisHeightTrajectoryMessage.getNumberOfWaypoints()];
      for (int i = 0; i < getNumberOfWaypoints(); i++)
         waypoints[i] = new Waypoint1DMessage(pelvisHeightTrajectoryMessage.waypoints[i]);
   }

   /**
    * Use this constructor to go straight to the given end point.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    * @param trajectoryTime how long it takes to reach the desired height.
    * @param desiredHeight desired pelvis height expressed in world frame.
    */
   public PelvisHeightTrajectoryMessage(double trajectoryTime, double desiredHeight)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      waypoints = new Waypoint1DMessage[] {new Waypoint1DMessage(trajectoryTime, desiredHeight, 0.0)};
   }

   /**
    * Use this constructor to build a message with more than one waypoint.
    * This constructor only allocates memory for the waypoints, you need to call {@link #setWaypoint(int, double, double, double)} for each waypoint afterwards.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    * @param numberOfWaypoints number of waypoints that will be sent to the controller.
    */
   public PelvisHeightTrajectoryMessage(int numberOfWaypoints)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      waypoints = new Waypoint1DMessage[numberOfWaypoints];
   }

   /**
    * Create a waypoint.
    * @param waypointIndex index of the waypoint to create.
    * @param time time at which the waypoint has to be reached. The time is relative to when the trajectory starts.
    * @param height define the desired height position to be reached at this waypoint. It is expressed in world frame.
    * @param heightVelocity define the desired height velocity to be reached at this waypoint. It is expressed in world frame.
    */
   public void setWaypoint(int waypointIndex, double time, double height, double heightVelocity)
   {
      rangeCheck(waypointIndex);
      waypoints[waypointIndex] = new Waypoint1DMessage(time, height, heightVelocity);
   }

   public int getNumberOfWaypoints()
   {
      return waypoints.length;
   }

   public Waypoint1DMessage getWaypoint(int waypointIndex)
   {
      return waypoints[waypointIndex];
   }

   public Waypoint1DMessage[] getWaypoints()
   {
      return waypoints;
   }

   public Waypoint1DMessage getLastWaypoint()
   {
      return waypoints[getNumberOfWaypoints() - 1];
   }

   public double getTrajectoryTime()
   {
      return getLastWaypoint().getTime();
   }

   private void rangeCheck(int waypointIndex)
   {
      if (waypointIndex >= getNumberOfWaypoints() || waypointIndex < 0)
         throw new IndexOutOfBoundsException("Waypoint index: " + waypointIndex + ", number of waypoints: " + getNumberOfWaypoints());
   }

   @Override
   public boolean epsilonEquals(PelvisHeightTrajectoryMessage other, double epsilon)
   {
      if (getNumberOfWaypoints() != other.getNumberOfWaypoints())
         return false;
      
      for (int i = 0; i < getNumberOfWaypoints(); i++)
      {
         if (!waypoints[i].epsilonEquals(other.waypoints[i], epsilon))
            return false;
      }

      return true;
   }

   @Override
   public String toString()
   {
      if (waypoints != null)
         return "Pelvis height 1D trajectory: number of 1D waypoints = " + getNumberOfWaypoints();
      else
         return "Pelvis height 1D trajectory: no 1D waypoints";
   }
}
