package us.ihmc.humanoidRobotics.communication.packets;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiMessage;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.TransformTools;
import us.ihmc.robotics.math.trajectories.waypoints.SE3WaypointInterface;

@ClassDocumentation("This class is used to build trajectory messages in taskspace. It holds the necessary information for one waypoint. "
      + "Feel free to look at EuclideanWaypoint (translational) and SO3Waypoint (rotational)")
public class SE3WaypointMessage extends IHMCRosApiMessage<SE3WaypointMessage> implements SE3WaypointInterface<SE3WaypointMessage>, TransformableDataObject<SE3WaypointMessage>
{
   @FieldDocumentation("Time at which the waypoint has to be reached. The time is relative to when the trajectory starts.")
   public double time;
   @FieldDocumentation("Define the desired 3D position to be reached at this waypoint. It is expressed in world frame.")
   public Point3d position;
   @FieldDocumentation("Define the desired 3D orientation to be reached at this waypoint. It is expressed in world frame.")
   public Quat4d orientation;
   @FieldDocumentation("Define the desired 3D linear velocity to be reached at this waypoint. It is expressed in world frame.")
   public Vector3d linearVelocity;
   @FieldDocumentation("Define the desired 3D angular velocity to be reached at this waypoint. It is expressed in world frame.")
   public Vector3d angularVelocity;

   /**
    * Empty constructor for serialization.
    */
   public SE3WaypointMessage()
   {
   }

   public SE3WaypointMessage(SE3WaypointMessage se3WaypointMessage)
   {
      time = se3WaypointMessage.time;
      if (se3WaypointMessage.position != null)
         position = new Point3d(se3WaypointMessage.position);
      if (se3WaypointMessage.orientation != null)
         orientation = new Quat4d(se3WaypointMessage.orientation);
      if (se3WaypointMessage.linearVelocity != null)
         linearVelocity = new Vector3d(se3WaypointMessage.linearVelocity);
      if (se3WaypointMessage.angularVelocity != null)
         angularVelocity = new Vector3d(se3WaypointMessage.angularVelocity);
   }

   public SE3WaypointMessage(double time, Point3d position, Quat4d orientation, Vector3d linearVelocity, Vector3d angularVelocity)
   {
      this.time = time;
      this.position = position;
      this.orientation = orientation;
      this.linearVelocity = linearVelocity;
      this.angularVelocity = angularVelocity;
   }

   @Override
   public void set(SE3WaypointMessage waypoint)
   {
      time = waypoint.time;
      if (waypoint.position != null)
         position.set(waypoint.position);
      else
         position.set(0.0, 0.0, 0.0);
      if (waypoint.orientation != null)
         orientation.set(waypoint.orientation);
      else
         orientation.set(0.0, 0.0, 0.0, 1.0);
      if (waypoint.linearVelocity != null)
         linearVelocity.set(waypoint.linearVelocity);
      else
         linearVelocity.set(0.0, 0.0, 0.0);
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

   public void setTime(double time)
   {
      this.time = time;
   }

   @Override
   public void getPosition(Point3d positionToPack)
   {
      positionToPack.set(position);
   }

   public void setPosition(Point3d position)
   {
      this.position = position;
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
   public void getLinearVelocity(Vector3d linearVelocityToPack)
   {
      linearVelocityToPack.set(linearVelocity);
   }

   public void setLinearVelocity(Vector3d linearVelocity)
   {
      this.linearVelocity = linearVelocity;
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
   public boolean epsilonEquals(SE3WaypointMessage other, double epsilon)
   {
      if (position == null && other.position != null)
         return false;
      if (position != null && other.position == null)
         return false;

      if (orientation == null && other.orientation != null)
         return false;
      if (orientation != null && other.orientation == null)
         return false;

      if (linearVelocity == null && other.linearVelocity != null)
         return false;
      if (linearVelocity != null && other.linearVelocity == null)
         return false;

      if (angularVelocity == null && other.angularVelocity != null)
         return false;
      if (angularVelocity != null && other.angularVelocity == null)
         return false;

      if (!MathTools.epsilonEquals(time, other.time, epsilon))
         return false;
      if (!position.epsilonEquals(other.position, epsilon))
         return false;
      if (!orientation.epsilonEquals(other.orientation, epsilon))
         return false;
      if (!linearVelocity.epsilonEquals(other.linearVelocity, epsilon))
         return false;
      if (!angularVelocity.epsilonEquals(other.angularVelocity, epsilon))
         return false;

      return true;
   }

   @Override
   public SE3WaypointMessage transform(RigidBodyTransform transform)
   {
      SE3WaypointMessage transformedWaypointMessage = new SE3WaypointMessage();

      transformedWaypointMessage.time = time;

      if (position != null)
         transformedWaypointMessage.position = TransformTools.getTransformedPoint(position, transform);
      else
         transformedWaypointMessage.position = null;

      if (orientation != null)
         transformedWaypointMessage.orientation = TransformTools.getTransformedQuat(orientation, transform);
      else
         transformedWaypointMessage.orientation = null;

      if (linearVelocity != null)
         transformedWaypointMessage.linearVelocity = TransformTools.getTransformedVector(linearVelocity, transform);
      else
         transformedWaypointMessage.linearVelocity = null;

      if (angularVelocity != null)
         transformedWaypointMessage.angularVelocity = TransformTools.getTransformedVector(angularVelocity, transform);
      else
         transformedWaypointMessage.angularVelocity = null;

      return transformedWaypointMessage;
   }

   @Override
   public String toString()
   {
      NumberFormat doubleFormat = new DecimalFormat(" 0.00;-0.00");
      String xToString = doubleFormat.format(position.getX());
      String yToString = doubleFormat.format(position.getY());
      String zToString = doubleFormat.format(position.getZ());
      String xDotToString = doubleFormat.format(linearVelocity.getX());
      String yDotToString = doubleFormat.format(linearVelocity.getY());
      String zDotToString = doubleFormat.format(linearVelocity.getZ());
      String qxToString = doubleFormat.format(orientation.getX());
      String qyToString = doubleFormat.format(orientation.getY());
      String qzToString = doubleFormat.format(orientation.getZ());
      String qsToString = doubleFormat.format(orientation.getW());
      String wxToString = doubleFormat.format(angularVelocity.getX());
      String wyToString = doubleFormat.format(angularVelocity.getY());
      String wzToString = doubleFormat.format(angularVelocity.getZ());

      String timeToString = "time = " + doubleFormat.format(time);
      String positionToString = "position = (" + xToString + ", " + yToString + ", " + zToString + ")";
      String orientationToString = "orientation = (" + qxToString + ", " + qyToString + ", " + qzToString + ", " + qsToString + ")";
      String linearVelocityToString = "linear velocity = (" + xDotToString + ", " + yDotToString + ", " + zDotToString + ")";
      String angularVelocityToString = "angular velocity = (" + wxToString + ", " + wyToString + ", " + wzToString + ")";

      return "SE3 waypoint: (" + timeToString + ", " + positionToString + ", " + orientationToString + ", " + linearVelocityToString + ", " + angularVelocityToString + ")";
   }
}
