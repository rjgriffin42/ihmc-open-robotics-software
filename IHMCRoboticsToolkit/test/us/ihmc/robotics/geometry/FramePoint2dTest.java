package us.ihmc.robotics.geometry;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Test;

import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.referenceFrame.FrameTuple2DReadOnly;
import us.ihmc.euclid.referenceFrame.FrameTuple3DReadOnlyTest;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.exceptions.ReferenceFrameMismatchException;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.robotics.random.RandomGeometry;

public class FramePoint2dTest extends FrameTuple2DTest<FramePoint2d>
{

   @Override
   public FramePoint2d createTuple(ReferenceFrame referenceFrame, double x, double y)
   {
      return createFrameTuple(referenceFrame, x, y);
   }

   @Override
   public FramePoint2d createFrameTuple(ReferenceFrame referenceFrame, double x, double y)
   {
      return new FramePoint2d(referenceFrame, x, y);
   }

   @After
   public void tearDown() throws Exception
   {
   }

   @Override
   public double getEpsilon()
   {
      return 1.0e-15;
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testOne()
   {
      FramePoint2d point = new FramePoint2d(worldFrame, 1.0, 2.0);

      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_double_double_String()
   {
      FramePoint2d point = new FramePoint2d(worldFrame, 1.0, 2.0);

      point.checkReferenceFrameMatch(worldFrame);

      try
      {
         point.checkReferenceFrameMatch(theFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good
      }
      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_FrameTuple2d()
   {
      FrameTuple2D<?, ?> frameTuple = createFrameTuple(theFrame, 1.0, 2.0);
      FramePoint2d point = new FramePoint2d(frameTuple);

      point.checkReferenceFrameMatch(theFrame);

      try
      {
         point.checkReferenceFrameMatch(aFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good
      }

      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame()
   {
      FramePoint2d point = new FramePoint2d(theFrame);
      point.checkReferenceFrameMatch(theFrame);

      try
      {
         point.checkReferenceFrameMatch(aFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good
      }
      assertEquals(0.0, point.getX(), 1e-7);
      assertEquals(0.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d()
   {
      FramePoint2d point = new FramePoint2d();

      point.checkReferenceFrameMatch(worldFrame);

      try
      {
         point.checkReferenceFrameMatch(theFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good
      }
      assertEquals(0.0, point.getX(), 1e-7);
      assertEquals(0.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_double_double()
   {
      FramePoint2d point = new FramePoint2d(worldFrame, 1.0, 2.0);
      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_double()
   {
      double[] position = { 1.0, 2.0 };
      FramePoint2d point = new FramePoint2d(worldFrame, position);
      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_double_String()
   {
      double[] position = { 1.0, 2.0 };
      FramePoint2d point = new FramePoint2d(worldFrame, position);
      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_String()
   {
      FramePoint2d point = new FramePoint2d(worldFrame);
      assertEquals(0.0, point.getX(), 1e-7);
      assertEquals(0.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_Tuple2d()
   {
      Point2D position = new Point2D(1.0, 2.0);
      FramePoint2d point = new FramePoint2d(worldFrame, position);
      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFramePoint2d_ReferenceFrame_Tuple2d_String()
   {
      Point2D position = new Point2D(1.0, 2.0);
      FramePoint2d point = new FramePoint2d(worldFrame, position);
      assertEquals(1.0, point.getX(), 1e-7);
      assertEquals(2.0, point.getY(), 1e-7);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistance_FramePoint2d()
   {
      FramePoint2d point1 = new FramePoint2d(theFrame, 1.0, 2.0);
      FramePoint2d point2 = new FramePoint2d(theFrame, 3.0, 4.0);
      double num = sumOfSquares(point1, point2);
      assertEquals("Should be equal", Math.sqrt(num), point1.distance(point2), epsilon);

      FramePoint2d point3 = new FramePoint2d(theFrame, 1.0, 2.0);
      num = sumOfSquares(point1, point3);
      assertEquals("Should be equal", 0.0, point1.distance(point3), epsilon);

      try
      {
         FramePoint2d point4 = new FramePoint2d(aFrame, 1.0, 2.0);
         point1.distance(point4);
         fail();
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceSquared_FramePoint2d()
   {
      FramePoint2d point1 = new FramePoint2d(theFrame, 1.0, 2.0);
      FramePoint2d point2 = new FramePoint2d(theFrame, 3.0, 4.0);
      double num = sumOfSquares(point1, point2);
      assertEquals("Should be equal", num, point1.distanceSquared(point2), epsilon);

      FramePoint2d point3 = new FramePoint2d(theFrame, 1.0, 2.0);
      num = sumOfSquares(point1, point3);
      assertEquals("Should be equal", num, point1.distanceSquared(point3), epsilon);

      try
      {
         FramePoint2d point4 = new FramePoint2d(aFrame, 1.0, 2.0);
         point1.distanceSquared(point4);
         fail();
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetPoint()
   {
      FramePoint2d point1 = new FramePoint2d(theFrame, 1.0, 2.0);
      Point2DReadOnly point2d = point1.getPoint();
      assertEquals("Should be equal", point1.getX(), point2d.getX(), epsilon);
      assertEquals("Should be equal", point1.getY(), point2d.getY(), epsilon);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransform_Transform3D_boolean()
   {
      boolean requireTransformInPlane = false;
      Random random = new Random(398742498237598750L);
      RigidBodyTransform transform = EuclidCoreRandomTools.generateRandomRigidBodyTransform(random);

      Point3D pointToTransform = RandomGeometry.nextPoint3D(random, 100.0, 100.0, 0.0);
      FramePoint2d pointToTest = new FramePoint2d(null, new Point2D(pointToTransform.getX(), pointToTransform.getY()));

      transform.transform(pointToTransform);
      pointToTest.applyTransform(transform, requireTransformInPlane);

      //transform on Point2d gives same result as FramePoint2d, transform not required in plane
      assertEquals("Should be equal", pointToTransform.getX(), pointToTest.getX(), epsilon);
      assertEquals("Should be equal", pointToTransform.getY(), pointToTest.getY(), epsilon);
      try
      {
         pointToTest.applyTransform(transform, true);
         fail("Should have thrown RuntimeException");
      }
      catch (RuntimeException re)
      {
         //Good
      }

      double[] matrix = { 6.0, 7.0, 0.0 };
      RigidBodyTransform transform2 = EuclidCoreRandomTools.generateRandomRigidBodyTransform2D(random);

      Point3D pointToTransform2 = new Point3D(matrix);
      FramePoint2d pointToTest2 = new FramePoint2d(null, matrix);

      transform2.transform(pointToTransform2);
      pointToTest2.applyTransform(transform2, true);

      //transform on Point2d gives same result as FramePoint2d, transform required in plane
      assertEquals("Should be equal", pointToTransform2.getX(), pointToTest2.getX(), epsilon);
      assertEquals("Should be equal", pointToTransform2.getY(), pointToTest2.getY(), epsilon);
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransform_Transform3D()
   {
      Random random = new Random(398742498237598750L);
      RigidBodyTransform transform = EuclidCoreRandomTools.generateRandomRigidBodyTransform(random);

      Point3D pointToTransform = RandomGeometry.nextPoint3D(random, 100.0, 100.0, 0.0);
      FramePoint2d pointToTest = new FramePoint2d(null, new Point2D(pointToTransform.getX(), pointToTransform.getY()));

      try
      {
         pointToTest.applyTransform(transform);
         fail("Should have thrown RuntimeException");
      }
      catch (RuntimeException re)
      {
         //Good
      }

      double[] matrix = { 6.0, 7.0 };
      RigidBodyTransform transform2 = EuclidCoreRandomTools.generateRandomRigidBodyTransform2D(random);
      FramePoint2d pointToTransform2 = new FramePoint2d(null, matrix);
      FramePoint2d pointToTest2 = new FramePoint2d(null, matrix);

      pointToTransform2.applyTransform(transform2);
      pointToTest2.applyTransform(transform2, true);

      assertEquals("Should be equal", pointToTransform2.getX(), pointToTest2.getX(), epsilon);
      assertEquals("Should be equal", pointToTransform2.getY(), pointToTest2.getY(), epsilon);
   }

//	@DeployableTestMethod(estimatedDuration = 0.0)
//	@Test(timeout = 30000)
//   public void testApplyTransformCopy_Transform3D()
//   {
//      double[] matrix = { 6.0, 7.0, 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0 };
//      RigidBodyTransform transform = new RigidBodyTransform(matrix);
//      FramePoint2d pointToTransform2 = new FramePoint2d(null, matrix);
//      FramePoint2d pointToTest2 = new FramePoint2d(null, matrix);
//
//      pointToTransform2.applyTransform(transform);
//      FramePoint2d copy = pointToTest2.applyTransformCopy(transform);
//
//      assertEquals("Should be equal", pointToTransform2.getX(), copy.getX(), epsilon);
//      assertEquals("Should be equal", pointToTransform2.getY(), copy.getY(), epsilon);
//   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testChangeFrame_ReferenceFrame()
   {
      FramePoint2d frame = new FramePoint2d(theFrame);
      frame.changeFrame(theFrame);

      frame.changeFrame(childFrame);
      frame.checkReferenceFrameMatch(childFrame);
      try
      {
         frame.checkReferenceFrameMatch(theFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good 
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testChangeFrameAndProjectToXYPlane_ReferenceFrame()
   {
      FramePoint2d frame = new FramePoint2d(theFrame);
      frame.changeFrameAndProjectToXYPlane(theFrame);

      frame.changeFrameAndProjectToXYPlane(childFrame);
      frame.checkReferenceFrameMatch(childFrame);
      try
      {
         frame.checkReferenceFrameMatch(theFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good 
      }
   }

	@ContinuousIntegrationTest(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testChangeFrameAndProjectToXYPlaneCopy_ReferenceFrame()
   {
      FramePoint2d frame = new FramePoint2d(theFrame);
      FramePoint2d copy = frame.changeFrameAndProjectToXYPlaneCopy(theFrame);

      copy = frame.changeFrameAndProjectToXYPlaneCopy(childFrame);
      copy.checkReferenceFrameMatch(childFrame);
      try
      {
         copy.checkReferenceFrameMatch(theFrame);
         fail("Should have thrown ReferenceFrameMismatchException");
      }
      catch (ReferenceFrameMismatchException rfme)
      {
         //Good 
      }
   }

   double sumOfSquares(FramePoint2d framePoint1, FramePoint2d framePoint2)
   {
      double ret = (framePoint2.getX() - framePoint1.getX()) * (framePoint2.getX() - framePoint1.getX()) + (framePoint2.getY() - framePoint1.getY())
            * (framePoint2.getY() - framePoint1.getY());
      return ret;
   }

   @Override
   public void testOverloading() throws Exception
   {
      super.testOverloading();
      FrameTuple3DReadOnlyTest.assertSuperMethodsAreOverloaded(FrameTuple2DReadOnly.class, Tuple2DReadOnly.class, FramePoint2d.class, Point2DBasics.class);
   }
}
