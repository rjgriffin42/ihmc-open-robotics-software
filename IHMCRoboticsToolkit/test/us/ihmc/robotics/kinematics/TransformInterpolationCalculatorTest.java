package us.ihmc.robotics.kinematics;

import org.junit.Test;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

;

/**
 * Created with IntelliJ IDEA.
 * User: pneuhaus
 * Date: 4/25/13
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransformInterpolationCalculatorTest
{
   public TransformInterpolationCalculator transformInterpolationCalculator = new TransformInterpolationCalculator();

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationOne() throws Exception
   {
      RigidBodyTransform t1 =  new RigidBodyTransform();
      t1.setIdentity();

      RigidBodyTransform t2 =  new RigidBodyTransform();
      t2.setIdentity();

      RigidBodyTransform t3 =  new RigidBodyTransform();
      
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, 0.0);
      assertTrue(t1.equals(t3));

      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, 1.0);
      assertTrue(t2.equals(t3));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationForTranslation() throws Exception
   {
      Matrix3d maxtrixIdentity = new Matrix3d();
      maxtrixIdentity.setIdentity();

      Vector3d vector1 = new Vector3d(0.0, 0.0, 0.0);
      Vector3d vector2 = new Vector3d(5.0, 8.0, 10.0);

      RigidBodyTransform t1 =  new RigidBodyTransform(maxtrixIdentity, vector1);
      RigidBodyTransform t2 =  new RigidBodyTransform(maxtrixIdentity, vector2);

      RigidBodyTransform t3 =  new RigidBodyTransform();
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, 0.0);
      Vector3d interpolatedVector = new Vector3d();
      t3.get(interpolatedVector);
      assertTrue(vector1.epsilonEquals(interpolatedVector, 1e-8));

      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, 1.0);
      interpolatedVector = new Vector3d();
      t3.get(interpolatedVector);
      assertTrue(vector2.epsilonEquals(interpolatedVector, 1e-8));

      double alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      interpolatedVector = new Vector3d();
      t3.get(interpolatedVector);

      Vector3d expectedVector = new Vector3d();
      expectedVector.scaleAdd((1- alpha), vector1, expectedVector);
      expectedVector.scaleAdd(alpha, vector2, expectedVector);

      assertTrue(expectedVector.epsilonEquals(interpolatedVector, 1e-8));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationForRotationYaw() throws Exception
   {
      double yaw1, pitch1, roll1;
      double yaw2, pitch2, roll2;
      RigidBodyTransform t1 = new RigidBodyTransform();
      RigidBodyTransform t2 = new RigidBodyTransform();
      RigidBodyTransform t3 = new RigidBodyTransform();
      double alpha;
      double[] yawPitchRoll = new double[3];

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 1.0;
      pitch2 = 0.0;
      roll2 = 0.0;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      alpha = 0.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t1.equals(t3));

      alpha = 1.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t2.epsilonEquals(t3, 1e-6));

      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      assertEquals(yawPitchRoll[0], (alpha-1)*yaw1 + alpha * yaw2, 1e-6);

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 1.0;
      pitch2 = 0.0;
      roll2 = 0.0;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      alpha = 0.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t1.equals(t3));

      alpha = 1.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t2.epsilonEquals(t3, 1e-6));

      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      assertEquals(yawPitchRoll[0], (alpha-1)*yaw1 + alpha * yaw2, 1e-6);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationForRotationRoll() throws Exception
   {
      double yaw1, pitch1, roll1;
      double yaw2, pitch2, roll2;
      RigidBodyTransform t1 = new RigidBodyTransform();
      RigidBodyTransform t2 = new RigidBodyTransform();
      RigidBodyTransform t3 = new RigidBodyTransform();
      double alpha;
      double[] yawPitchRoll = new double[3];

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 0.0;
      pitch2 = 0.0;
      roll2 = 1.0;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      alpha = 0.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t1.equals(t3));

      alpha = 1.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t2.epsilonEquals(t3, 1e-6));

      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      assertEquals(yawPitchRoll[2], (alpha-1)*roll1 + alpha * roll2, 1e-6);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationForRotationPitch() throws Exception
   {
      double yaw1, pitch1, roll1;
      double yaw2, pitch2, roll2;
      RigidBodyTransform t1 = new RigidBodyTransform();
      RigidBodyTransform t2 = new RigidBodyTransform();
      RigidBodyTransform t3 = new RigidBodyTransform();
      double alpha;
      double[] yawPitchRoll = new double[3];

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 0.0;
      pitch2 = 1.0;
      roll2 = 0.0;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      alpha = 0.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t1.equals(t3));

      alpha = 1.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t2.epsilonEquals(t3, 1e-6));

      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      assertEquals(yawPitchRoll[1], (alpha-1)*pitch1 + alpha * pitch2, 1e-6);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationForRotationYawEdgeCases() throws Exception
   {
      double yaw1, pitch1, roll1;
      double yaw2, pitch2, roll2;
      RigidBodyTransform t1 = new RigidBodyTransform();
      RigidBodyTransform t2 = new RigidBodyTransform();
      RigidBodyTransform t3 = new RigidBodyTransform();
      double alpha;
      double[] yawPitchRoll = new double[3];

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 10.0;

      //must convert to -pi to pi range
      yaw2 = AngleTools.shiftAngleToStartOfRange(yaw2, -Math.PI);

      pitch2 = 0.0;
      roll2 = 0.0;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      alpha = 0.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t1.equals(t3));

      alpha = 1.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      assertTrue(t2.epsilonEquals(t3, 1e-6));

      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      assertEquals(yawPitchRoll[0], (alpha-1)*yaw1 + alpha * yaw2, 1e-6);

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 100.0;



      pitch2 = 0.0;
      roll2 = 0.0;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      alpha = 0.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      assertTrue(t1.equals(t3));

      alpha = 1.0;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);


      assertTrue(t2.epsilonEquals(t3, 1e-6));

      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      //must convert to -pi to pi range
      yaw2 = AngleTools.shiftAngleToStartOfRange(yaw2, -Math.PI);
      assertEquals(yawPitchRoll[0], (alpha-1)*yaw1 + alpha * yaw2, 1e-6);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testComputeInterpolationForRotationCombined() throws Exception
   {
      double yaw1, pitch1, roll1;
      double yaw2, pitch2, roll2;
      RigidBodyTransform t1 = new RigidBodyTransform();
      RigidBodyTransform t2 = new RigidBodyTransform();
      RigidBodyTransform t3 = new RigidBodyTransform();
      double alpha;
      double[] yawPitchRoll = new double[3];

      yaw1 = 0.0;
      pitch1 = 0.0;
      roll1 = 0.0;

      yaw2 = 1.0;
      pitch2 = -1.0;
      roll2 = 1.6;

      t1.setEuler(new Vector3d(roll1, pitch1, yaw1));
      t2.setEuler(new Vector3d(roll2, pitch2, yaw2));

      AxisAngle4d axist1 = new AxisAngle4d();
      Matrix3d maxtrixt1 = new Matrix3d();
      t1.getRotation(maxtrixt1);
//      axist1.set(maxtrixt1);
      RotationTools.convertMatrixToAxisAngle(maxtrixt1, axist1);

      AxisAngle4d axist2 = new AxisAngle4d();
      Matrix3d maxtrixt2 = new Matrix3d();
      t2.getRotation(maxtrixt2);
//      axist2.set(maxtrixt2);
      RotationTools.convertMatrixToAxisAngle(maxtrixt2, axist2);
      



      alpha = 0.25;
      transformInterpolationCalculator.computeInterpolation(t1, t2, t3, alpha);
      getYawPitchRoll(yawPitchRoll, t3);

      AxisAngle4d axist3 = new AxisAngle4d();
      Matrix3d maxtrixt3 = new Matrix3d();
      t3.getRotation(maxtrixt3);
      axist3.set(maxtrixt3);

      //Since t1 has no rotation, t3 rotation should be in the same direction as t2 with the angle controlled by alpha
      double[] t2xyztheta = new double[4];
      axist2.get(t2xyztheta);

      double[] t3xyztheta = new double[4];
      axist3.get(t3xyztheta);

      //compare vectors
      Vector3d t2vector = new Vector3d(t2xyztheta[0], t2xyztheta[1], t2xyztheta[2]);
      Vector3d t3vector = new Vector3d(t3xyztheta[0], t3xyztheta[1], t3xyztheta[2]);

      double anlgeBetweenVectors = t2vector.angle(t3vector);
      assertEquals(0.0, anlgeBetweenVectors, 1e-6);

      double expectedAngleRotation = alpha * t2xyztheta[3];
      assertEquals(expectedAngleRotation, t3xyztheta[3], 1e-6);
   }


   private void getYawPitchRoll(double[] yawPitchRoll, RigidBodyTransform transform3D)
   {
      // This seems to work much better than going to quaternions first, especially when yaw is large...
      Matrix3d rotationMatrix = new Matrix3d();
      transform3D.getRotation(rotationMatrix);
      yawPitchRoll[0] = Math.atan2(rotationMatrix.m10, rotationMatrix.m00);
      yawPitchRoll[1] = Math.asin(-rotationMatrix.m20);
      yawPitchRoll[2] = Math.atan2(rotationMatrix.m21, rotationMatrix.m22);

      if (Double.isNaN(yawPitchRoll[0]) || Double.isNaN(yawPitchRoll[1]) || Double.isNaN(yawPitchRoll[2]))
      {
         throw new RuntimeException("yaw, pitch, or roll are NaN! transform3D = " + transform3D);
      }
   }


   @DeployableTestMethod(estimatedDuration = 0.05)
   @Test(timeout = 30000)
   public void testInterpolationWithFramePoses()
   {
      Random random = new Random(52156165L);

      RigidBodyTransform transform1 = new RigidBodyTransform();
      RigidBodyTransform transform2 = new RigidBodyTransform();
      RigidBodyTransform toTestTransform = new RigidBodyTransform();
      RigidBodyTransform expectedTransform = new RigidBodyTransform();

      FramePose framePose1 = new FramePose();
      FramePose framePose2 = new FramePose();
      FramePose expectedFramePose = new FramePose();

      PoseReferenceFrame frame1 = new PoseReferenceFrame("frame1", framePose1);
      PoseReferenceFrame frame2 = new PoseReferenceFrame("frame2", framePose2);

      TransformInterpolationCalculator transformInterpolationCalculator = new TransformInterpolationCalculator();
      
      for (int i = 0; i < 1000; i++)
      {
         transform1.set(RigidBodyTransform.generateRandomTransform(random));
         transform2.set(RigidBodyTransform.generateRandomTransform(random));

         framePose1.setPoseIncludingFrame(ReferenceFrame.getWorldFrame(), transform1);
         framePose2.setPoseIncludingFrame(ReferenceFrame.getWorldFrame(), transform2);

         double alpha = RandomTools.generateRandomDouble(random, 0.0, 1.0);
         transformInterpolationCalculator.computeInterpolation(transform1, transform2, toTestTransform, alpha);

         frame1.setPoseAndUpdate(framePose1);
         frame2.setPoseAndUpdate(framePose2);

         // Change to frame1 just for convenience when debugging since the framePose1 is zero in that frame.
         framePose1.changeFrame(frame1);
         framePose2.changeFrame(frame1);
         expectedFramePose.changeFrame(frame1);

         expectedFramePose.interpolate(framePose1, framePose2, alpha);
         expectedFramePose.changeFrame(ReferenceFrame.getWorldFrame());

         expectedFramePose.getPose(expectedTransform);
         
         assertTrue(expectedTransform.epsilonEquals(toTestTransform, 1.0e-10));
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.001)
   @Test(timeout = 30000)
   public void testInterpolationForTimeStampedTransform()
   {
      Random random = new Random(52156165L);
      TimeStampedTransform3D firstTimeStampedTransform = new TimeStampedTransform3D();
      TimeStampedTransform3D secondTimeStampedTransform = new TimeStampedTransform3D();
      TimeStampedTransform3D toTestTimeStampedTransform = new TimeStampedTransform3D();
      
      RigidBodyTransform expectedTransform = new RigidBodyTransform();
      
      TransformInterpolationCalculator transformInterpolationCalculator = new TransformInterpolationCalculator();
      
      firstTimeStampedTransform.setTimeStamp(RandomTools.generateRandomInt(random, 123, 45196516));
      secondTimeStampedTransform.setTimeStamp(firstTimeStampedTransform.getTimeStamp() - 1);
      
      try
      {
         transformInterpolationCalculator.interpolate(firstTimeStampedTransform, secondTimeStampedTransform, toTestTimeStampedTransform, 216515L);
         fail("Should have thrown a RuntimeException as firstTimestamp is smaller than secondTimestamp.");
      }
      catch (RuntimeException e)
      {
         // Good
      }

      firstTimeStampedTransform.setTimeStamp(RandomTools.generateRandomInt(random, 123, 45196516));
      secondTimeStampedTransform.setTimeStamp(firstTimeStampedTransform.getTimeStamp() + RandomTools.generateRandomInt(random, 1, 20));

      try
      {
         transformInterpolationCalculator.interpolate(firstTimeStampedTransform, secondTimeStampedTransform, toTestTimeStampedTransform, secondTimeStampedTransform.getTimeStamp() + 1);
         fail("Should have thrown a RuntimeException as the given timestamp is outside bounds.");
      }
      catch (RuntimeException e)
      {
         // Good
      }


      try
      {
         transformInterpolationCalculator.interpolate(firstTimeStampedTransform, secondTimeStampedTransform, toTestTimeStampedTransform, firstTimeStampedTransform.getTimeStamp() - 1);
         fail("Should have thrown a RuntimeException as the given timestamp is outside bounds.");
      }
      catch (RuntimeException e)
      {
         // Good
      }

      for (int i = 0; i < 100; i++)
      {
         firstTimeStampedTransform.setTransform3D(RigidBodyTransform.generateRandomTransform(random));
         secondTimeStampedTransform.setTransform3D(RigidBodyTransform.generateRandomTransform(random));

         long timestamp1 = RandomTools.generateRandomInt(random, 123, Integer.MAX_VALUE / 4);
         long timestamp2 = timestamp1 + RandomTools.generateRandomInt(random, 1, 200);
         firstTimeStampedTransform.setTimeStamp(timestamp1);
         secondTimeStampedTransform.setTimeStamp(timestamp2);

         long timeStampForInterpolation = RandomTools.generateRandomInt(random, (int) firstTimeStampedTransform.getTimeStamp(), (int) secondTimeStampedTransform.getTimeStamp());

         transformInterpolationCalculator.interpolate(firstTimeStampedTransform, secondTimeStampedTransform, toTestTimeStampedTransform, timeStampForInterpolation);

         double alpha = ((double) timeStampForInterpolation - (double) timestamp1) / ((double) timestamp2 - (double) timestamp1);
         transformInterpolationCalculator.computeInterpolation(firstTimeStampedTransform.getTransform3D(), secondTimeStampedTransform.getTransform3D(), expectedTransform, alpha);

         assertTrue(expectedTransform.epsilonEquals(toTestTimeStampedTransform.getTransform3D(), 1.0e-10));
      }
   }
}
