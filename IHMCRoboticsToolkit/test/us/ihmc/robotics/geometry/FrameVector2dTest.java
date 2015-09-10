package us.ihmc.robotics.geometry;

import org.junit.After;
import org.junit.Test;

import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class FrameVector2dTest extends FrameTuple2dTest<FrameVector2d>
{
   @Override
   public FrameVector2d createFrameTuple(ReferenceFrame referenceFrame, double x, double y, String name)
   {
      return new FrameVector2d(referenceFrame, x, y, name);
   }

   @After
   public void tearDown()
   {
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_ReferenceFrame_double_double_String()
   {
      double x = 5.7, y = 56.3;
      String name = "vector";
      FrameVector2d frame = new FrameVector2d(theFrame, x, y, name);
      assertEquals("Should be equal", frame.getX(), x, epsilon);
      assertEquals("Should be equal", frame.getY(), y, epsilon);
      assertEquals("Should be equal", frame.getName(), name);
      assertEquals("Should be equal", frame.getReferenceFrame(), theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_ReferenceFrame_double_double()
   {
      double x = 5.7, y = 56.3;
      FrameVector2d frame = new FrameVector2d(theFrame, x, y);
      assertEquals("Should be equal", frame.getX(), x, epsilon);
      assertEquals("Should be equal", frame.getY(), y, epsilon);
      assertEquals("Should be equal", frame.getName(), null);
      assertEquals("Should be equal", frame.getReferenceFrame(), theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d()
   {
      FrameVector2d frame = new FrameVector2d();
      assertEquals("Should be equal", frame.getX(), 0.0, epsilon);
      assertEquals("Should be equal", frame.getY(), 0.0, epsilon);
      assertEquals("Should be equal", frame.getName(), null);
      assertEquals("Should be equal", frame.getReferenceFrame(), ReferenceFrame.getWorldFrame());
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_ReferenceFrame_Tuple2d()
   {
      double x = 5.7, y = 56.3;
      Point2d tuple = new Point2d(x, y);
      FrameVector2d frame = new FrameVector2d(theFrame, tuple);
      assertEquals("Should be equal", frame.getX(), x, epsilon);
      assertEquals("Should be equal", frame.getY(), y, epsilon);
      assertEquals("Should be equal", frame.getName(), null);
      assertEquals("Should be equal", frame.getReferenceFrame(), theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_ReferenceFrame_double()
   {
      double x = 5.7, y = 56.3;
      double[] vector = {x, y};
      FrameVector2d frame = new FrameVector2d(theFrame, vector);
      assertEquals("Should be equal", frame.getX(), x, epsilon);
      assertEquals("Should be equal", frame.getY(), y, epsilon);
      assertEquals("Should be equal", frame.getName(), null);
      assertEquals("Should be equal", frame.getReferenceFrame(), theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_ReferenceFrame()
   {
      double x = 0.0, y = 0.0;
      FrameVector2d frame = new FrameVector2d(theFrame);
      assertEquals("Should be equal", frame.getX(), x, epsilon);
      assertEquals("Should be equal", frame.getY(), y, epsilon);
      assertEquals("Should be equal", frame.getName(), null);
      assertEquals("Should be equal", frame.getReferenceFrame(), theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_FrameTuple2d()
   {
      double x = 5.7, y = 56.3;
      FrameTuple2d<?> frameTuple = createFrameTuple(theFrame, x, y);
      FrameVector2d frame = new FrameVector2d(frameTuple);
      assertEquals("Should be equal", frame.getX(), x, epsilon);
      assertEquals("Should be equal", frame.getY(), y, epsilon);
      assertEquals("Should be equal", frame.getName(), null);
      assertEquals("Should be equal", frame.getReferenceFrame(), frameTuple.getReferenceFrame());
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameVector2d_FramePoint2d_FramePoint2d()
   {
      double x = 5.7, y = 56.3;
      FramePoint2d startFramePoint = new FramePoint2d(theFrame, x, y, "start");
      FramePoint2d endFramePoint = new FramePoint2d(theFrame, y, x, "end");
      FrameVector2d frame = new FrameVector2d(startFramePoint, endFramePoint);
      assertEquals("Should be equal", frame.getX(), endFramePoint.getX() - startFramePoint.getX(), epsilon);
      assertEquals("Should be equal", frame.getY(), endFramePoint.getY() - startFramePoint.getY(), epsilon);
      assertEquals("Should be equal", frame.getName(), endFramePoint.getName());
      assertEquals("Should be equal", frame.getReferenceFrame(), startFramePoint.getReferenceFrame());
      try
      {
         FramePoint2d startFramePoint2 = new FramePoint2d(aFrame, x, y);
         new FrameVector2d(startFramePoint2, endFramePoint);
         fail();
      }
      catch(ReferenceFrameMismatchException rfme)
      {
         //Good
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetVector()
   {
      double x = 5.6, y = 45.67;
      String name = "woof";
      FrameVector2d frame = new FrameVector2d(aFrame, x, y, name);
      Vector2d vector2d = frame.getVector();
      assertEquals("Should be equal", frame.getX(), vector2d.getX(), epsilon);
      assertEquals("Should be equal", frame.getY(), vector2d.getY(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testRotate90()
   {
      Random random = new Random(45456L);
      double x = random.nextDouble(), y = random.nextDouble();
      String name = "woof";
      FrameVector2d frame = new FrameVector2d(aFrame, x, y, name);
      frame.rotate90();
      assertEquals("Should be equal", frame.getX(), -y, epsilon);
      assertEquals("Should be equal", frame.getY(), x, epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDot_FrameVector2d()
   {
      Random random = new Random(4556L);
      double x1 = random.nextDouble(), x2 = random.nextDouble(), x3 = random.nextDouble(), 
            y1 = random.nextDouble(), y2 = random.nextDouble(), y3 = random.nextDouble();
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);
      FrameVector2d frame2 = new FrameVector2d(theFrame, x2, y2);
      FrameVector2d frame3 = new FrameVector2d(aFrame, x3, y3);

      double result = frame1.dot(frame2);
      assertEquals("Should be equal", result, frame1.getX() * frame2.getX() + frame1.getY() * frame2.getY(), epsilon);
      try
      {
         frame1.dot(frame3);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch(ReferenceFrameMismatchException rfme)
      {
         //Good
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testCross_FrameVector2d()
   {
      Random random = new Random(4556L);
      double x1 = random.nextDouble(), x2 = random.nextDouble(), x3 = random.nextDouble(), 
            y1 = random.nextDouble(), y2 = random.nextDouble(), y3 = random.nextDouble();
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);
      FrameVector2d frame2 = new FrameVector2d(theFrame, x2, y2);
      FrameVector2d frame3 = new FrameVector2d(aFrame, x3, y3);

      double result = frame1.cross(frame2);
      assertEquals("Should be equal", result, frame1.getX() * frame2.getY() - frame1.getY() * frame2.getX(), epsilon);
      try
      {
         frame1.cross(frame3);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch(ReferenceFrameMismatchException rfme)
      {
         //Good
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAngle_FrameVector2d()
   {
      Random random = new Random(4556L);
      double x1 = random.nextDouble(), x2 = random.nextDouble(), x3 = random.nextDouble(), 
            y1 = random.nextDouble(), y2 = random.nextDouble(), y3 = random.nextDouble();
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);
      FrameVector2d frame2 = new FrameVector2d(theFrame, x2, y2);
      FrameVector2d frame3 = new FrameVector2d(aFrame, x3, y3);
      
      Vector2d vector1 = new Vector2d(x1, y1);
      Vector2d vector2 = new Vector2d(x2, y2);

      double result = frame1.angle(frame2);
      double resultVector = vector1.angle(vector2);
      assertEquals("Should be equal", result, resultVector, epsilon);
      try
      {
         frame1.angle(frame3);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch(ReferenceFrameMismatchException rfme)
      {
         //Good
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testNormalize()
   {
      double x1 = 1.0, y1 = 1.0;
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);

      frame1.normalize();
      double result = frame1.getX() * frame1.getX() + frame1.getY() * frame1.getY();
      assertEquals("Should be equal", 1.0, result, epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testLength()
   {
      double x1 = 1.0, y1 = 1.0;
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);
      double result = Math.sqrt(frame1.getX() * frame1.getX() + frame1.getY() * frame1.getY());
      assertEquals("Should be equal", result, frame1.length(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testLengthSquared()
   {
      double x1 = 1.0, y1 = 1.0;
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);
      double result = frame1.getX() * frame1.getX() + frame1.getY() * frame1.getY();
      assertEquals("Should be equal", result, frame1.lengthSquared(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testClipMaxLength_double()
   {
      double x1 = 0.5, y1 = -0.5;
      double maxLength = 1.0;
      FrameVector2d frame1 = new FrameVector2d(theFrame, x1, y1);
      frame1.clipMaxLength(maxLength);
      assertTrue(frame1.getX() <= maxLength);
      assertTrue(frame1.getY() <= maxLength);

      double maxLength2 = 1e-8;
      frame1.clipMaxLength(maxLength2);
      assertEquals("Should be 0.0", frame1.getX(), 0.0, epsilon);
      assertEquals("Should be 0.0", frame1.getY(), 0.0, epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testChangeFrame_ReferenceFrame()
   {
      FrameVector2d frameVector = new FrameVector2d(theFrame);
      frameVector.changeFrame(childFrame);
      frameVector.checkReferenceFrameMatch(childFrame);

      frameVector.changeFrame(childFrame);
      frameVector.checkReferenceFrameMatch(childFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransform_Transform3D()
   {  
      Random random = new Random(398742498237598750L);
      RigidBodyTransform transform = RigidBodyTransform.generateRandomTransform(random);

      Vector3d vectorToTransform = RandomTools.generateRandomVector(random, 0.0, 0.0, 0.0, 100.0, 100.0, 0.0);
      FrameVector2d vectorToTest = new FrameVector2d(null, new Vector2d(vectorToTransform.x, vectorToTransform.y)); 

      try
      {
         vectorToTest.applyTransform(transform);
         fail("Should have thrown RuntimeException");
      }
      catch(RuntimeException re)
      {
         //Good
      }

      double[] matrix = {6.0, 7.0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0};
      RigidBodyTransform transform2 = new RigidBodyTransform(matrix);

      Vector3d vectorToTransform2 = new Vector3d(matrix);
      FrameVector2d vectorToTest2 = new FrameVector2d(null, matrix);

      transform2.transform(vectorToTransform2);
      vectorToTest2.applyTransform(transform2);

      assertEquals("Should be equal", vectorToTransform2.getX(), vectorToTest2.getX(), epsilon);
      assertEquals("Should be equal", vectorToTransform2.getY(), vectorToTest2.getY(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransformCopy_Transform3D()
   {
      Random random = new Random(398742498237598750L);
      RigidBodyTransform transform = RigidBodyTransform.generateRandomTransform(random);

      Vector3d vectorToTransform = RandomTools.generateRandomVector(random, 0.0, 0.0, 0.0, 100.0, 100.0, 0.0);
      FrameVector2d vectorToTest = new FrameVector2d(null, new Vector2d(vectorToTransform.x, vectorToTransform.y)); 

      try
      {
         vectorToTest.applyTransformCopy(transform);
         fail("Should have thrown RuntimeException");
      }
      catch(RuntimeException re)
      {
         //Good
      }

      double[] matrix = {6.0, 7.0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0};
      RigidBodyTransform transform2 = new RigidBodyTransform(matrix);

      Vector3d vectorToTransform2 = new Vector3d(matrix);
      FrameVector2d vectorToTest2 = new FrameVector2d(null, matrix);

      transform2.transform(vectorToTransform2);
      vectorToTest2 = vectorToTest2.applyTransformCopy(transform2);

      assertEquals("Should be equal", vectorToTransform2.getX(), vectorToTest2.getX(), epsilon);
      assertEquals("Should be equal", vectorToTransform2.getY(), vectorToTest2.getY(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testChangeFrameUsingTransform_ReferenceFrame_Transform3D()
   { 
      double[] matrix = {6.0, 7.0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0};
      RigidBodyTransform transform = new RigidBodyTransform(matrix);

      Vector3d vectorToTransform = new Vector3d(matrix);
      FrameVector2d vectorToTest = new FrameVector2d(aFrame, matrix);

      transform.transform(vectorToTransform);
      vectorToTest.changeFrameUsingTransform(theFrame, transform);

      assertEquals("Should be equal", vectorToTransform.getX(), vectorToTest.getX(), epsilon);
      assertEquals("Should be equal", vectorToTransform.getY(), vectorToTest.getY(), epsilon);
      vectorToTest.checkReferenceFrameMatch(theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
      public void testChangeFrameUsingTransformCopy_ReferenceFrame_Transform3D()
      {
         double[] matrix = {6.0, 7.0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0};
         RigidBodyTransform transform = new RigidBodyTransform(matrix);

         Vector3d vectorToTransform = new Vector3d(matrix);
         FrameVector2d vectorToTest = new FrameVector2d(aFrame, matrix);
         FrameVector2d copy = new FrameVector2d();

         transform.transform(vectorToTransform);
         copy = vectorToTest.changeFrameUsingTransformCopy(theFrame, transform);

         assertEquals("Should be equal", vectorToTransform.getX(), copy.getX(), epsilon);
         assertEquals("Should be equal", vectorToTransform.getY(), copy.getY(), epsilon);
         copy.checkReferenceFrameMatch(theFrame);
      }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSets()
   {
      FrameVector alpha = new FrameVector(ReferenceFrame.getWorldFrame(), 1.0, 2.0, 3.0);
      FrameVector beta = new FrameVector(ReferenceFrame.getWorldFrame(), 8.0, -2.0, 0.0);
      alpha.set(0.0, 0.0, 0.0);
      alpha.setX(-7.0);
      alpha.setY(10.3);
      alpha.setZ(1.9);
      alpha.set(10, 20, 30);
      alpha.setX(0);
      alpha.set(beta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testadd()
   {
      FrameVector alpha = new FrameVector(ReferenceFrame.getWorldFrame(), 1.0, 2.0, 3.0);
      FrameVector beta = new FrameVector(ReferenceFrame.getWorldFrame(), 7.0, 0.0, 6.0);
      alpha.add(beta);

   }

   // NaN was found in beta, commented out for further testing

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testCheckForNaN()
   {
      FrameVector alpha = new FrameVector(ReferenceFrame.getWorldFrame(), 6.0, 50.0, 2.0);

      // FrameVector beta = new FrameVector(ReferenceFrame.getWorldFrame(), Math.sqrt(1.0), 2.0, 3.0);
      alpha.checkForNaN();

      // beta.checkForNaN();
   }

   /*
    * public void test() { ReferenceFrame worldFrame =
    * ReferenceFrame.getWorldFrame(); FrameVector vWorld = new
    * FrameVector(ReferenceFrame.getWorldFrame(), 1.0, 2.0, 3.0);
    * 
    * ArrayList<ReferenceFrame> frames = new ArrayList<ReferenceFrame>();
    * frames.add(worldFrame); frames.clear(); }
    */

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testInterpolate()
   {
      FrameVector alpha = new FrameVector(ReferenceFrame.getWorldFrame(), -1.0, 0.0, 17.0);
      FrameVector beta = new FrameVector(ReferenceFrame.getWorldFrame(), 3.3, 30.0, 9.0);
      FrameVector gamma = new FrameVector(ReferenceFrame.getWorldFrame(), 1.0, 2.8, 3.0);

      gamma.interpolate(alpha, beta, 3.0);
      gamma.interpolate(beta, alpha, 1);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetReferenceFrame()
   {
      FrameVector alpha = new FrameVector(ReferenceFrame.getWorldFrame(), 1.0, 2.0, 3.0);
      FrameVector beta = new FrameVector(ReferenceFrame.getWorldFrame(), 0.0, 20.0, 5.0);
      alpha.getReferenceFrame();
      beta.getReferenceFrame();
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGets()
   {
      FrameVector alpha = new FrameVector(ReferenceFrame.getWorldFrame(), 1.0, 2.0, 3.0);
      FrameVector beta = new FrameVector(ReferenceFrame.getWorldFrame(), 7.0, 0.0, -6.0);
      alpha.getX();
      beta.getY();
      beta.getZ();
   }
}
