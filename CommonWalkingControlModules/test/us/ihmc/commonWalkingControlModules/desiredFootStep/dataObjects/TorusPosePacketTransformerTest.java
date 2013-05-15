package us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects;

import org.junit.Test;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.HandPosePacket;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulationStateMachine.TorusPosePacket;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: pneuhaus
 * Date: 5/14/13
 * Time: 10:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class TorusPosePacketTransformerTest
{
   @Test
   public void testTransformTorusPosePacket()
   {
      int numberOfTests = 10;
      double radius = 1.0;
      Random random = new Random(100L);
      Transform3D transform3D;

      for (int i = 0; i < numberOfTests; i++)
      {
         Point3d point3d = RandomTools.generateRandomPoint(random, 10.0, 10.0, 10.0);
         Vector3d normal = RandomTools.generateRandomVector(random, 1.0);

         TorusPosePacket starting = new TorusPosePacket(point3d, normal, radius);

         transform3D = RandomTools.generateRandomTransform(random);

         TorusPosePacket ending = TorusPosePacketTransformer.transformTorusPosePacket(starting, transform3D);

         performEqualsTest(starting, transform3D, ending);
      }
   }

   private static void performEqualsTest(TorusPosePacket starting, Transform3D transform3D, TorusPosePacket ending)
   {
      // Point3d position;
      double distance = getDistanceBetweenPoints(starting.getPosition(), transform3D, ending.getPosition());
      assertEquals("not equal", 0.0, distance, 1e-6);

      // Quat4d orientation;
      Vector3d startingNormalCopy = new Vector3d(starting.getNormal());
      transform3D.transform(startingNormalCopy);

      boolean normalsEqual = startingNormalCopy.epsilonEquals(ending.normal, 1e-6);
      assertTrue(normalsEqual);
   }

   private static double getDistanceBetweenPoints(Point3d startingPoint, Transform3D transform3D, Point3d endPoint)
   {
      ReferenceFrame ending = ReferenceFrame.constructARootFrame("ending", false, true, true);
      ReferenceFrame starting = ReferenceFrame.constructFrameWithUnchangingTransformToParent("starting", ending, transform3D, false, true, true);

      FramePoint start = new FramePoint(starting, startingPoint);
      FramePoint end = new FramePoint(ending, endPoint);

      end.changeFrame(starting);

      return end.distance(start);
   }

   private static boolean areOrientationsEqualWithTransform(Quat4d orientationStart, Transform3D transform3D, Quat4d orientationEnd)
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
