package us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointTrajectoryUtils;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.Direction;
import us.ihmc.utilities.math.geometry.FrameBox3d;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryWaypointGenerationMethod;

/**
 * Created with IntelliJ IDEA.
 * User: pneuhaus
 * Date: 5/11/13
 * Time: 3:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class FootstepDataTansformerTest
{
   private static Random random = new Random(100L);

   @Test
   public void test()
   {
      Transform3D transform3D;
      FootstepData originalFootstepData;
      FootstepData transformedFootstepData;

      int numberOfTests = 1;

      for (int i = 0; i < numberOfTests; i++)
      {
         originalFootstepData = getTestFootstepData();
         transform3D = new Transform3D();
         transform3D.set(new Vector3d(1.0, 0.0, 0.0));
//         transform3D = RandomTools.generateRandomTransform(random);
         
//         Vector3d v1 = new Vector3d();
//         transform3D.get(v1);
//         System.out.println(v1 + "  T");
         
         transformedFootstepData = FootstepDataTransformer.transformFootstepData(originalFootstepData, transform3D);

         performEqualsTestsWithTransform(originalFootstepData, transform3D, transformedFootstepData);
      }
   }

   private static FootstepData getTestFootstepData()
   {
      FootstepData ret = new FootstepData();
      ret.robotSide = RobotSide.LEFT;
      ret.location = RandomTools.generateRandomPoint(random, 10.0, 10.0, 10.0);
      AxisAngle4d axisAngle = RandomTools.generateRandomRotation(random);
      ret.orientation = new Quat4d();
      ret.orientation.set(axisAngle);

      List<Point3d> listOfPoints = new ArrayList<Point3d>();
      {
         for (int i = 0; i < 30; i++)
         {
            listOfPoints.add(RandomTools.generateRandomPoint(random, 10.0, 10.0, 10.0));
         }
      }

      Point3d boxDimensions = RandomTools.generateRandomPoint(random, 10.0, 10.0, 10.0);
      Transform3D randomBoxToWorldTransform = new Transform3D(); // RandomTools.generateRandomTransform(random);
      randomBoxToWorldTransform.set(new Vector3d(0.0, 1.0, 0.0));
      
      ReferenceFrame randomBoxFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("randomFrame", ReferenceFrame.getWorldFrame(),
            randomBoxToWorldTransform);
      
      ret.trajectoryBoxData = TwoWaypointTrajectoryUtils.getTopFaceOfBox(new FrameBox3d(randomBoxFrame, Math.abs(boxDimensions.getX()), Math.abs(boxDimensions.getY()), Math.abs(boxDimensions.getZ())));
      
//      System.out.println((TwoWaypointTrajectoryUtils.getTopFaceOfBox(new FrameBox3d(randomBoxFrame, Math.abs(boxDimensions.getX()), Math.abs(boxDimensions.getY()), Math.abs(boxDimensions.getZ()))).location) + "   <- dis");
      
      int index = (int) Math.floor(random.nextDouble() * TrajectoryWaypointGenerationMethod.values().length);
      ret.trajectoryWaypointGenerationMethod = TrajectoryWaypointGenerationMethod.values()[index];

      return ret;
   }

   private static void performEqualsTestsWithTransform(FootstepData footstepData, Transform3D transform3D, FootstepData transformedFootstepData)
   {
      double distance;

      // public String rigidBodyName;
      assertTrue(footstepData.getRobotSide() == transformedFootstepData.getRobotSide());

      // public Point3d location;
      distance = getDistanceBetweenPoints(footstepData.getLocation(), transform3D, transformedFootstepData.getLocation());
      assertEquals("not equal", 0.0, distance, 1e-6);

      // public Quat4d orientation;
      Quat4d startQuat = footstepData.getOrientation();
      Quat4d endQuat = transformedFootstepData.getOrientation();
      assertTrue(areOrientationsEqualWithTransform(startQuat, transform3D, endQuat));

      assertTrue(areBoxesEqual(footstepData.getTrajectoryBox(), transform3D, transformedFootstepData.getTrajectoryBox()));

      // public TrajectoryWaypointGenerationMethod trajectoryWaypointGenerationMethod;
      assertTrue("", footstepData.getTrajectoryWaypointGenerationMethod().equals(transformedFootstepData.getTrajectoryWaypointGenerationMethod()));
   }

   @Test
   public void testDistance()
   {
      Point3d startPoint = new Point3d(2.0, 6.0, 5.0);
      Transform3D transform3D = new Transform3D();
      transform3D.set(new Vector3d(1.0, 2.0, 3.0));

      Point3d endPoint = new Point3d();
      transform3D.transform(startPoint, endPoint);

      double distance = getDistanceBetweenPoints(startPoint, transform3D, endPoint);
      assertEquals("not equal", 0.0, distance, 1e-6);
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

   private static double getDistanceBetweenPoints(Point3d startingPoint, Transform3D transform3D, Point3d endPoint)
   {
      ReferenceFrame ending = ReferenceFrame.constructARootFrame("ending", false, true, true);
      ReferenceFrame starting = ReferenceFrame.constructFrameWithUnchangingTransformToParent("starting", ending, transform3D, false, true, true);

      FramePoint start = new FramePoint(starting, startingPoint);
      FramePoint end = new FramePoint(ending, endPoint);

      end.changeFrame(starting);

      return end.distance(start);
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

   private static boolean areBoxesEqual(FrameBox3d boxStarting, Transform3D expectedTransform, FrameBox3d boxEnding)
   {
      Transform3D actualTransform = boxEnding.getReferenceFrame().getTransformToDesiredFrame(boxStarting.getReferenceFrame());
      
      if (!expectedTransform.epsilonEquals(actualTransform, 1e-6))
         return false;
      
      for (Direction direction : Direction.values())
      {
         double startDimension = boxStarting.getDimension(direction);
         double endDimension = boxEnding.getDimension(direction);

         if (Math.abs(startDimension - endDimension) > 1e-6)
            return false;
      }

      return true;
   }
}
