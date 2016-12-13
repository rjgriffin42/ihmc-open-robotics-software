package us.ihmc.humanoidRobotics.communication.packets.walking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.communication.ros.generators.RosEnumValueDocumentation;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.communication.ros.generators.RosExportedField;
import us.ihmc.communication.packets.Packet;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.humanoidRobotics.communication.packets.PacketValidityChecker;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.geometry.TransformTools;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.trajectories.TrajectoryType;

@RosMessagePacket(documentation = "This message specifies the position, orientation and side (left or right) of a desired footstep in world frame.",
                  rosPackage = RosMessagePacket.CORE_IHMC_PACKAGE)
public class FootstepDataMessage extends Packet<FootstepDataMessage> implements TransformableDataObject<FootstepDataMessage>
{
   public enum FootstepOrigin
   {
      @Deprecated
      @RosEnumValueDocumentation(documentation = "The location of the footstep refers to the location of the ankle frame."
            + " The ankle frame is fixed in the foot, centered at the last ankle joint."
            + " The orientation = [qx = 0.0, qy = 0.0, qz = 0.0, qs = 1.0] corresponds to: x-axis pointing forward, y-axis pointing left, z-axis pointing upward."
            + " This option is for backward compatibility only and will be gone in an upcoming release."
            + " This origin is deprecated as it directly depends on the robot structure and is not directly related to the actual foot sole.")
      AT_ANKLE_FRAME,
      @RosEnumValueDocumentation(documentation = "The location of the footstep refers to the location of the sole frame."
            + " The sole frame is fixed in the foot, centered at the center of the sole."
            + " The orientation = [qx = 0.0, qy = 0.0, qz = 0.0, qs = 1.0] corresponds to: x-axis pointing forward, y-axis pointing left, z-axis pointing upward."
            + " This origin is preferred as it directly depends on the actual foot sole and is less dependent on the robot structure.")
      AT_SOLE_FRAME
   }

   @RosExportedField(documentation = "Specifies whether the given location is the location of the ankle or the sole.")
   public FootstepOrigin origin;
   @RosExportedField(documentation = "Specifies which foot will swing to reach the foostep.")
   public RobotSide robotSide;
   @RosExportedField(documentation = "Specifies the position of the footstep. It is expressed in world frame.")
   public Point3d location;
   @RosExportedField(documentation = "Specifies the orientation of the footstep. It is expressed in world frame.")
   public Quat4d orientation;

   @RosExportedField(documentation = "predictedContactPoints specifies the vertices of the expected contact polygon between the foot and\n"
         + "the world. A value of null or an empty list will default to using the entire foot. Contact points  are expressed in sole frame. This ordering does not matter.\n"
         + "For example: to tell the controller to use the entire foot, the predicted contact points would be:\n" + "predicted_contact_points:\n"
         + "- {x: 0.5 * foot_length, y: -0.5 * toe_width}\n" + "- {x: 0.5 * foot_length, y: 0.5 * toe_width}\n"
         + "- {x: -0.5 * foot_length, y: -0.5 * heel_width}\n" + "- {x: -0.5 * foot_length, y: 0.5 * heel_width}\n")
   public ArrayList<Point2d> predictedContactPoints;

   @RosExportedField(documentation = "This contains information on what the swing trajectory should be for each step. Recomended is DEFAULT.\n")
   public TrajectoryType trajectoryType = TrajectoryType.DEFAULT;

   @RosExportedField(documentation = "Contains information on how high the robot should step. This affects only basic and obstacle clearance trajectories."
         + "Recommended values are between 0.1 (default) and 0.25.\n")
   public double swingHeight = 0;

   /**
    * Empty constructor for serialization.
    */
   public FootstepDataMessage()
   {
      origin = FootstepOrigin.AT_ANKLE_FRAME;
   }

   public FootstepDataMessage(RobotSide robotSide, Point3d location, Quat4d orientation)
   {
      this(robotSide, location, orientation, null);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3d location, Quat4d orientation, ArrayList<Point2d> predictedContactPoints)
   {
      this(robotSide, location, orientation, predictedContactPoints, TrajectoryType.DEFAULT, 0.0);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3d location, Quat4d orientation, TrajectoryType trajectoryType, double swingHeight)
   {
      this(robotSide, location, orientation, null, trajectoryType, swingHeight);
   }

   public FootstepDataMessage(RobotSide robotSide, Point3d location, Quat4d orientation, ArrayList<Point2d> predictedContactPoints,
         TrajectoryType trajectoryType, double swingHeight)
   {
      origin = FootstepOrigin.AT_ANKLE_FRAME;
      this.robotSide = robotSide;
      this.location = location;
      this.orientation = orientation;
      if (predictedContactPoints != null && predictedContactPoints.size() == 0)
         this.predictedContactPoints = null;
      else
         this.predictedContactPoints = predictedContactPoints;
      this.trajectoryType = trajectoryType;
      this.swingHeight = swingHeight;
   }

   public FootstepDataMessage(FootstepDataMessage footstepData)
   {
      this.origin = footstepData.origin;
      this.robotSide = footstepData.robotSide;
      this.location = new Point3d(footstepData.location);
      this.orientation = new Quat4d(footstepData.orientation);
      RotationTools.checkQuaternionNormalized(this.orientation);
      if (footstepData.predictedContactPoints == null || footstepData.predictedContactPoints.isEmpty())
      {
         this.predictedContactPoints = null;
      }
      else
      {
         this.predictedContactPoints = new ArrayList<>();
         for (Point2d contactPoint : footstepData.predictedContactPoints)
         {
            this.predictedContactPoints.add(new Point2d(contactPoint));
         }
      }
      this.trajectoryType = footstepData.trajectoryType;
      this.swingHeight = footstepData.swingHeight;
   }

   public FootstepDataMessage clone()
   {
      return new FootstepDataMessage(this);
   }

   public FootstepDataMessage(Footstep footstep)
   {
      origin = FootstepOrigin.AT_ANKLE_FRAME;
      robotSide = footstep.getRobotSide();
      location = new Point3d();
      orientation = new Quat4d();
      footstep.getPositionInWorldFrame(location);
      footstep.getOrientationInWorldFrame(orientation);

      List<Point2d> footstepContactPoints = footstep.getPredictedContactPoints();
      if (footstepContactPoints != null)
      {
         if (predictedContactPoints == null)
         {
            predictedContactPoints = new ArrayList<>();
         }
         else
         {
            predictedContactPoints.clear();
         }
         for (Point2d contactPoint : footstepContactPoints)
         {
            predictedContactPoints.add((Point2d) contactPoint.clone());
         }
      }
      else
      {
         predictedContactPoints = null;
      }
      trajectoryType = footstep.trajectoryType;
      swingHeight = footstep.swingHeight;
   }

   public FootstepOrigin getOrigin()
   {
      return origin;
   }

   public ArrayList<Point2d> getPredictedContactPoints()
   {
      return predictedContactPoints;
   }

   public Point3d getLocation()
   {
      return location;
   }

   public void getLocation(Point3d locationToPack)
   {
      locationToPack.set(location);
   }

   public Quat4d getOrientation()
   {
      return orientation;
   }

   public void getOrientation(Quat4d orientationToPack)
   {
      orientationToPack.set(this.orientation);
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public double getSwingHeight()
   {
      return swingHeight;
   }

   public void setRobotSide(RobotSide robotSide)
   {
      this.robotSide = robotSide;
   }

   public void setOrigin(FootstepOrigin origin)
   {
      this.origin = origin;
   }

   public void setLocation(Point3d location)
   {
      if (this.location == null) this.location = new Point3d();
      this.location.set(location);
   }

   public void setOrientation(Quat4d orientation)
   {
      if (this.orientation == null) this.orientation = new Quat4d();
      this.orientation.set(orientation);
   }

   public void setSwingHeight(double swingHeight)
   {
      this.swingHeight = swingHeight;
      if (trajectoryType == TrajectoryType.DEFAULT)
         trajectoryType = TrajectoryType.BASIC;
   }

   public void setPredictedContactPoints(ArrayList<Point2d> predictedContactPoints)
   {
      this.predictedContactPoints = predictedContactPoints;
   }

   public TrajectoryType getTrajectoryType()
   {
      return trajectoryType;
   }

   public void setTrajectoryType(TrajectoryType trajectoryType)
   {
      this.trajectoryType = trajectoryType;
   }

   public String toString()
   {
      String ret = "";

      FrameOrientation frameOrientation = new FrameOrientation(ReferenceFrame.getWorldFrame(), this.orientation);
      double[] ypr = frameOrientation.getYawPitchRoll();
      ret = location.toString();
      ret += ", YawPitchRoll = " + Arrays.toString(ypr) + "\n";
      ret += "Predicted Contact Points: ";
      if (predictedContactPoints != null)
      {
         ret += "size = " + predictedContactPoints.size() + "\n";
      }
      else
      {
         ret += "null";
      }

      return ret;
   }

   public boolean epsilonEquals(FootstepDataMessage footstepData, double epsilon)
   {
      boolean robotSideEquals = robotSide == footstepData.robotSide;
      boolean locationEquals = location.epsilonEquals(footstepData.location, epsilon);

      boolean orientationEquals = orientation.epsilonEquals(footstepData.orientation, epsilon);
      if (!orientationEquals)
      {
         Quat4d temp = new Quat4d();
         temp.negate(orientation);
         orientationEquals = temp.epsilonEquals(footstepData.orientation, epsilon);
      }

      boolean contactPointsEqual = true;

      if ((this.predictedContactPoints == null) && (footstepData.predictedContactPoints != null))
         contactPointsEqual = false;
      else if ((this.predictedContactPoints != null) && (footstepData.predictedContactPoints == null))
         contactPointsEqual = false;
      else if (this.predictedContactPoints != null)
      {
         int size = predictedContactPoints.size();
         if (size != footstepData.predictedContactPoints.size())
            contactPointsEqual = false;
         else
         {
            for (int i = 0; i < size; i++)
            {
               Point2d pointOne = predictedContactPoints.get(i);
               Point2d pointTwo = footstepData.predictedContactPoints.get(i);

               if (!(pointOne.distanceSquared(pointTwo) < 1e-7))
                  contactPointsEqual = false;
            }
         }
      }

      return robotSideEquals && locationEquals && orientationEquals && contactPointsEqual;
   }

   public FootstepDataMessage transform(RigidBodyTransform transform)
   {
      FootstepDataMessage ret = this.clone();

      // Point3d location;
      ret.location = TransformTools.getTransformedPoint(this.getLocation(), transform);

      // Quat4d orientation;
      ret.orientation = TransformTools.getTransformedQuat(this.getOrientation(), transform);
      return ret;
   }

   public FootstepDataMessage(Random random)
   {
      TrajectoryType[] trajectoryTypes = TrajectoryType.values();
      int randomOrdinal = random.nextInt(trajectoryTypes.length);

      origin = FootstepOrigin.AT_ANKLE_FRAME;
      this.robotSide = random.nextBoolean() ? RobotSide.LEFT : RobotSide.RIGHT;
      this.location = RandomTools.generateRandomPointWithEdgeCases(random, 0.05);
      this.orientation = RandomTools.generateRandomQuaternion(random);
      int numberOfPredictedContactPoints = random.nextInt(10);
      this.predictedContactPoints = new ArrayList<>();

      for (int i = 0; i < numberOfPredictedContactPoints; i++)
      {
         predictedContactPoints.add(new Point2d(random.nextDouble(), random.nextDouble()));
      }

      this.trajectoryType = trajectoryTypes[randomOrdinal];
      this.swingHeight = RandomTools.generateRandomDoubleWithEdgeCases(random, 0.05);
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      return PacketValidityChecker.validateFootstepDataMessage(this);
   }
}
