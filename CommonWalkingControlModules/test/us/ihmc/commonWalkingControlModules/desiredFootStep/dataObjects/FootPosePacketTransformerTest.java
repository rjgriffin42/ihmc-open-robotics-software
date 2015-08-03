package us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import org.junit.Test;

import us.ihmc.communication.packets.walking.FootPosePacket;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.robotSide.RobotSide;

/**
 * Created with IntelliJ IDEA.
 * User: pneuhaus
 * Date: 5/20/13
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class FootPosePacketTransformerTest
{

	@EstimatedDuration(duration = 0.0)
	@Test(timeout = 30000)
   public void testTransformHandPosePacket() throws Exception
   {
      int numberOfTests = 10;
      Random random = new Random(100L);
      RigidBodyTransform transform3D;
      RobotSide robotSide;
      for (int i = 0; i < numberOfTests; i++)
      {
         AxisAngle4d axisAngle = RandomTools.generateRandomRotation(random);
         Quat4d quat = new Quat4d();
         quat.set(axisAngle);



         if (i % 2 == 0)
            robotSide = RobotSide.LEFT;
         else
            robotSide = RobotSide.RIGHT;

         Point3d point3d = RandomTools.generateRandomPoint(random, 10.0, 10.0, 10.0);
         
         double trajectoryTime = RandomTools.generateRandomDouble(random, 0.6, 5.0);

         FootPosePacket starting = new FootPosePacket(robotSide, point3d, quat, trajectoryTime);

         transform3D = RandomTools.generateRandomTransform(random);

         FootPosePacket ending = starting.transform(transform3D);

         performEqualsTest(starting, transform3D, ending);
      }
   }

   private static void performEqualsTest(FootPosePacket starting, RigidBodyTransform transform3D, FootPosePacket ending)
   {
      // RobotSide robotSide;
      assertTrue(starting.getRobotSide().equals(ending.getRobotSide()));

      // Point3d position;
      double distance = getDistanceBetweenPoints(starting.getPosition(), transform3D, ending.getPosition());
      assertEquals("not equal", 0.0, distance, 1e-6);

      // Quat4d orientation;
      Quat4d startQuat = starting.getOrientation();
      Quat4d endQuat = ending.getOrientation();
      assertTrue(areOrientationsEqualWithTransform(startQuat, transform3D, endQuat));
   }

   private static double getDistanceBetweenPoints(Point3d startingPoint, RigidBodyTransform transform3D, Point3d endPoint)
   {
      ReferenceFrame ending = ReferenceFrame.constructARootFrame("ending", false, true, true);
      ReferenceFrame starting = ReferenceFrame.constructFrameWithUnchangingTransformToParent("starting", ending, transform3D, false, true, true);

      FramePoint start = new FramePoint(starting, startingPoint);
      FramePoint end = new FramePoint(ending, endPoint);

      end.changeFrame(starting);

      return end.distance(start);
   }

   private static boolean areOrientationsEqualWithTransform(Quat4d orientationStart, RigidBodyTransform transform3D, Quat4d orientationEnd)
   {
      ReferenceFrame ending = ReferenceFrame.constructARootFrame("ending", false, true, true);
      ReferenceFrame starting = ReferenceFrame.constructFrameWithUnchangingTransformToParent("starting", ending, transform3D, false, true, true);

      FrameOrientation start = new FrameOrientation(starting, orientationStart);
      FrameOrientation end = new FrameOrientation(ending, orientationEnd);

      end.changeFrame(starting);

      return equalsFrameOrientation(start, end);
   }

   private static boolean equalsFrameOrientation(FrameOrientation frameOrientation1, FrameOrientation frameOrientation2)
   {
      // Check reference frame first
      if (frameOrientation1.getReferenceFrame() != frameOrientation2.getReferenceFrame())
         return false;

      double[] rpyThis = frameOrientation1.getYawPitchRoll();
      double[] rpyThat = frameOrientation2.getYawPitchRoll();

      for (int i = 0; i < rpyThat.length; i++)
      {
         if (Math.abs(rpyThis[i] - rpyThat[i]) > 1e-6)
            return false;
      }

      return true;
   }
}
