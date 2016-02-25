package us.ihmc.humanoidRobotics.communication.packets;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiMessage;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.TransformTools;
import us.ihmc.robotics.math.trajectories.waypoints.SO3WaypointInterface;

@ClassDocumentation("This class is used to build trajectory messages in taskspace. It holds the only the rotational information for one waypoint (orientation & angular velocity). "
      + "Feel free to look at EuclideanWaypoint (translational) and SE3Waypoint (rotational AND translational)")
public class SO3WaypointMessage extends IHMCRosApiMessage<SO3WaypointMessage>
      implements SO3WaypointInterface<SO3WaypointMessage>, TransformableDataObject<SO3WaypointMessage>
{
   @FieldDocumentation("Time at which the waypoint has to be reached. The time is relative to when the trajectory starts.")
   public double time;
   @FieldDocumentation("Define the desired 3D orientation to be reached at this waypoint. It is expressed in world frame.")
   public Quat4d orientation;
   @FieldDocumentation("Define the desired 3D angular velocity to be reached at this waypoint. It is expressed in world frame.")
   public Vector3d angularVelocity;

   /**
    * Empty constructor for serialization.
    */
   public SO3WaypointMessage()
   {
   }

   public SO3WaypointMessage(SO3WaypointMessage so3Waypoint)
   {
      time = so3Waypoint.time;
      if (so3Waypoint.orientation != null)
         orientation = new Quat4d(so3Waypoint.orientation);
      if (so3Waypoint.angularVelocity != null)
         angularVelocity = new Vector3d(so3Waypoint.angularVelocity);
   }

   public SO3WaypointMessage(double time, Quat4d orientation, Vector3d angularVelocity)
   {
      this.time = time;
      this.orientation = orientation;
      this.angularVelocity = angularVelocity;
   }

   @Override
   public void set(SO3WaypointMessage waypoint)
   {
      time = waypoint.time;
      if (waypoint.orientation != null)
         orientation.set(waypoint.orientation);
      else
         orientation.set(0.0, 0.0, 0.0, 1.0);
      if (waypoint.angularVelocity != null)
         angularVelocity.set(waypoint.angularVelocity);
      else
         angularVelocity.set(0.0, 0.0, 0.0);
   }

   @Override
   public double getTime()
   {
      return time;
   }

   @Override
   public void addTimeOffset(double timeOffsetToAdd)
   {
      time += timeOffsetToAdd;
   }

   @Override
   public void subtractTimeOffset(double timeOffsetToSubtract)
   {
      time -= timeOffsetToSubtract;
   }

   @Override
   public void setTime(double time)
   {
      this.time = time;
   }

   @Override
   public void getOrientation(Quat4d orientationToPack)
   {
      orientationToPack.set(orientation);
   }

   public void setOrientation(Quat4d orientation)
   {
      this.orientation = orientation;
   }

   @Override
   public void getAngularVelocity(Vector3d angularVelocityToPack)
   {
      angularVelocityToPack.set(angularVelocity);
   }

   public void setAngularVelocity(Vector3d angularVelocity)
   {
      this.angularVelocity = angularVelocity;
   }

   @Override
   public SO3WaypointMessage transform(RigidBodyTransform transform)
   {
      SO3WaypointMessage transformedWaypointMessage = new SO3WaypointMessage();

      transformedWaypointMessage.time = time;

      if (orientation != null)
         transformedWaypointMessage.orientation = TransformTools.getTransformedQuat(orientation, transform);
      else
         transformedWaypointMessage.orientation = null;

      if (angularVelocity != null)
         transformedWaypointMessage.angularVelocity = TransformTools.getTransformedVector(angularVelocity, transform);
      else
         transformedWaypointMessage.angularVelocity = null;

      return transformedWaypointMessage;
   }

   @Override
   public boolean epsilonEquals(SO3WaypointMessage other, double epsilon)
   {
      if (orientation == null && other.orientation != null)
         return false;
      if (orientation != null && other.orientation == null)
         return false;

      if (angularVelocity == null && other.angularVelocity != null)
         return false;
      if (angularVelocity != null && other.angularVelocity == null)
         return false;

      if (!MathTools.epsilonEquals(time, other.time, epsilon))
         return false;
      if (!orientation.epsilonEquals(other.orientation, epsilon))
         return false;
      if (!angularVelocity.epsilonEquals(other.angularVelocity, epsilon))
         return false;

      return true;
   }

   @Override
   public String toString()
   {
      NumberFormat doubleFormat = new DecimalFormat(" 0.00;-0.00");
      String qxToString = doubleFormat.format(orientation.getX());
      String qyToString = doubleFormat.format(orientation.getY());
      String qzToString = doubleFormat.format(orientation.getZ());
      String qsToString = doubleFormat.format(orientation.getW());
      String wxToString = doubleFormat.format(angularVelocity.getX());
      String wyToString = doubleFormat.format(angularVelocity.getY());
      String wzToString = doubleFormat.format(angularVelocity.getZ());

      String timeToString = "time = " + doubleFormat.format(time);
      String orientationToString = "orientation = (" + qxToString + ", " + qyToString + ", " + qzToString + ", " + qsToString + ")";
      String angularVelocityToString = "angular velocity = (" + wxToString + ", " + wyToString + ", " + wzToString + ")";

      return "SO3 waypoint: (" + timeToString + ", " + orientationToString + ", " + angularVelocityToString + ")";
   }
}
