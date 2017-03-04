package us.ihmc.robotics.geometry;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commons.Epsilons;
import us.ihmc.commons.MutationTestFacilitator;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.robotics.random.RandomGeometry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

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
public class GeometryToolsTest
{
   private static final int ITERATIONS = 1000;

   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }

   private static final double EPSILON = 1e-6;

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetDistanceBetweenPointAndPlane1()
   {
      FramePoint pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      double actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      double expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -3);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -3);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -3);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 6.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 1, 0, 0);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 0, 0);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 1, 1, 1);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      actual = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
      expected = 2.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetDistanceBetweenPointAndPlane2()
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point3D pointOnPlane = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         Vector3D planeNormal = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));

         Vector3D parallelToPlane = RandomGeometry.nextOrthogonalVector3D(random, planeNormal, true);
         Point3D secondPointOnPlane = new Point3D();
         secondPointOnPlane.scaleAdd(RandomNumbers.nextDouble(random, 10.0), parallelToPlane, pointOnPlane);

         double expectedDistance = RandomNumbers.nextDouble(random, 0.0, 10.0);
         Point3D point = new Point3D();
         point.scaleAdd(expectedDistance / planeNormal.length(), planeNormal, secondPointOnPlane);

         double actualDistance = GeometryTools.distanceFromPointToPlane(point, pointOnPlane, planeNormal);
         assertEquals(expectedDistance, actualDistance, Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testIsLineSegmentIntersectingPlane1()
   {
      FramePoint pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -1);
      FramePoint lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      assertTrue(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 1, 0, 0);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), -6, 3, -3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertTrue(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 6, -3, -3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertTrue(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 6, -3, 3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertFalse(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 6, -3, -3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, -1);
      assertFalse(GeometryTools.isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testDoLineSegmentsIntersect1()
   {
      boolean intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(0.0, -1.0), new Point2D(0.0, 1.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(0.0, 1.0), new Point2D(0.0, -1.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, 1.0), new Point2D(1.0, -1.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, -1.0), new Point2D(1.0, 1.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, -1.0), new Point2D(1.0, -1.0));
      assertFalse(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, 1.0), new Point2D(1.0, 1.0));
      assertFalse(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.1, 1.0), new Point2D(-1.1, -1.0));
      assertFalse(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, 1.0), new Point2D(-1.0, -1.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0));
      assertTrue(intersect);

      intersect = GeometryTools.doLineSegmentsIntersect(new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0), new Point2D(-1.0, 0.0), new Point2D(1.0, 0.0));
      assertTrue(intersect);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testDoLineSegmentsIntersect2()
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D lineSegmentStart1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D lineSegmentEnd1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);

         Point2D pointOnLineSegment1 = new Point2D();
         pointOnLineSegment1.interpolate(lineSegmentStart1, lineSegmentEnd1, RandomNumbers.nextDouble(random, 0.0, 1.0));

         Vector2D lineDirection2 = RandomGeometry.nextVector2D(random, 1.0);

         Point2D lineSegmentStart2 = new Point2D();
         Point2D lineSegmentEnd2 = new Point2D();

         // Expecting intersection
         lineSegmentStart2.scaleAdd(RandomNumbers.nextDouble(random, 0.0, 10.0), lineDirection2, pointOnLineSegment1);
         lineSegmentEnd2.scaleAdd(RandomNumbers.nextDouble(random, -10.0, 0.0), lineDirection2, pointOnLineSegment1);
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2));
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentStart2, lineSegmentEnd2));
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentEnd2, lineSegmentStart2));
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentEnd2, lineSegmentStart2));

         // Not expecting intersection
         lineSegmentStart2.scaleAdd(RandomNumbers.nextDouble(random, 0.0, 10.0), lineDirection2, pointOnLineSegment1);
         lineSegmentEnd2.scaleAdd(RandomNumbers.nextDouble(random, 0.0, 10.0), lineDirection2, pointOnLineSegment1);
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2));
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentStart2, lineSegmentEnd2));
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentEnd2, lineSegmentStart2));
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentEnd2, lineSegmentStart2));
      }

      // Test intersection at one of the end points
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D lineSegmentStart1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D lineSegmentEnd1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);

         Point2D pointOnLineSegment1 = new Point2D(lineSegmentStart1);

         Vector2D lineDirection2 = RandomGeometry.nextVector2D(random, 1.0);

         Point2D lineSegmentStart2 = new Point2D();
         Point2D lineSegmentEnd2 = new Point2D();

         // Expecting intersection
         lineSegmentStart2.scaleAdd(RandomNumbers.nextDouble(random, 0.0, 10.0), lineDirection2, pointOnLineSegment1);
         lineSegmentEnd2.scaleAdd(RandomNumbers.nextDouble(random, -10.0, 0.0), lineDirection2, pointOnLineSegment1);
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2));
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentStart2, lineSegmentEnd2));
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentEnd2, lineSegmentStart2));
         assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentEnd2, lineSegmentStart2));
      }

      // Test with parallel/collinear line segments
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D lineSegmentStart1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D lineSegmentEnd1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);

         Point2D lineSegmentStart2 = new Point2D();
         Point2D lineSegmentEnd2 = new Point2D();

         double alpha1 = RandomNumbers.nextDouble(random, 2.0);
         double alpha2 = RandomNumbers.nextDouble(random, 2.0);

         // Make the second line segment collinear to the first one
         lineSegmentStart2.interpolate(lineSegmentStart1, lineSegmentEnd1, alpha1);
         lineSegmentEnd2.interpolate(lineSegmentStart1, lineSegmentEnd1, alpha2);

         if ((0.0 < alpha1 && alpha1 < 1.0) || (0.0 < alpha2 && alpha2 < 1.0) || alpha1 * alpha2 < 0.0)
         {
            assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2));
            assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentStart2, lineSegmentEnd2));
            assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentEnd2, lineSegmentStart2));
            assertTrue(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentEnd2, lineSegmentStart2));
         }
         else
         {
            assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2));
            assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentStart2, lineSegmentEnd2));
            assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentEnd2, lineSegmentStart2));
            assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentEnd2, lineSegmentStart2));
         }

         // Shift the second line segment such that it becomes only parallel to the first.
         Vector2D orthogonal = new Vector2D();
         orthogonal.sub(lineSegmentEnd1, lineSegmentStart1);
         orthogonal.set(-orthogonal.getY(), orthogonal.getX());
         orthogonal.normalize();

         double distance = RandomNumbers.nextDouble(random, 1.0e-10, 10.0);
         lineSegmentStart2.scaleAdd(distance, orthogonal, lineSegmentStart2);
         lineSegmentEnd2.scaleAdd(distance, orthogonal, lineSegmentEnd2);
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2));
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentStart2, lineSegmentEnd2));
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentEnd1, lineSegmentStart1, lineSegmentEnd2, lineSegmentStart2));
         assertFalse(GeometryTools.doLineSegmentsIntersect(lineSegmentStart1, lineSegmentEnd1, lineSegmentEnd2, lineSegmentStart2));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPerpendicularBisector1()
   {
      Point2D lineStart = new Point2D(1, 1);
      Point2D lineEnd = new Point2D(5, 5);
      Point2D bisectorStart = new Point2D(2, 1);
      Vector2D bisectorDirection = new Vector2D();
      GeometryTools.getPerpendicularBisector(lineStart, lineEnd, bisectorStart, bisectorDirection);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPerpendicularBisector2()
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D lineSegmentStart = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D lineSegmentEnd = RandomGeometry.nextPoint2D(random, 10.0, 10.0);

         Point2D expectedBisectorStart = new Point2D();
         expectedBisectorStart.interpolate(lineSegmentStart, lineSegmentEnd, 0.5);
         Vector2D expectedBisectorDirection = new Vector2D();
         expectedBisectorDirection.sub(lineSegmentEnd, lineSegmentStart);
         GeometryTools.getPerpendicularVector(expectedBisectorDirection, expectedBisectorDirection);
         expectedBisectorDirection.normalize();

         Point2D actualBisectorStart = new Point2D();
         Vector2D actualBisectorDirection = new Vector2D();
         GeometryTools.getPerpendicularBisector(lineSegmentStart, lineSegmentEnd, actualBisectorStart, actualBisectorDirection);
         EuclidCoreTestTools.assertTuple2DEquals(expectedBisectorStart, actualBisectorStart, Epsilons.ONE_TRILLIONTH);
         EuclidCoreTestTools.assertTuple2DEquals(expectedBisectorDirection, actualBisectorDirection, Epsilons.ONE_TRILLIONTH);

         Point2D pointOnBisector = new Point2D();
         pointOnBisector.scaleAdd(1.0, actualBisectorDirection, actualBisectorStart);
         assertTrue(EuclidGeometryTools.isPoint2DOnLeftSideOfLine2D(pointOnBisector, lineSegmentStart, lineSegmentEnd));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPerpendicularVector()
   {
      Vector2D vector = new Vector2D(15.0, 10.0);
      Vector2D expectedReturn = new Vector2D(-10.0, 15.0);
      Vector2D actualReturn = GeometryTools.getPerpendicularVector(vector);
      assertEquals("return value", expectedReturn, actualReturn);
      Random random = new Random(1176L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         vector = RandomGeometry.nextVector2D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         Vector2D perpendicularVector = GeometryTools.getPerpendicularVector(vector);
         assertEquals(vector.length(), perpendicularVector.length(), Epsilons.ONE_TRILLIONTH);
         assertEquals(vector.length() * vector.length(), vector.cross(perpendicularVector), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, vector.dot(perpendicularVector), Epsilons.ONE_TRILLIONTH);
         assertEquals(Math.PI / 2.0, vector.angle(perpendicularVector), Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPerpendicularVectorFromLineToPoint1()
   {
      FramePoint point0 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint lineStart0 = new FramePoint(ReferenceFrame.getWorldFrame(), -10, 10, 0);
      FramePoint lineEnd0 = new FramePoint(ReferenceFrame.getWorldFrame(), 10, 10, 0);
      FramePoint intersectionPoint0 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 10, 0);
      FrameVector x0 = new FrameVector(point0.getReferenceFrame());
      x0.sub(point0, intersectionPoint0);
      FrameVector expectedReturn0 = x0;
      FrameVector actualReturn0 = GeometryTools.getPerpendicularVectorFromLineToPoint(point0, lineStart0, lineEnd0, intersectionPoint0);

      assertTrue("Test Failed", expectedReturn0.epsilonEquals(actualReturn0, EPSILON));

      FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), 4, 2, 0);
      FramePoint lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 10, 10, 0);
      FramePoint intersectionPoint = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 0);
      FrameVector x = new FrameVector(point.getReferenceFrame());
      x.sub(point, intersectionPoint);
      FrameVector expectedReturn = x;
      FrameVector actualReturn = GeometryTools.getPerpendicularVectorFromLineToPoint(point, lineStart, lineEnd, intersectionPoint);
      assertTrue("Test Failed", expectedReturn.epsilonEquals(actualReturn, EPSILON));

      FramePoint point1 = new FramePoint(ReferenceFrame.getWorldFrame(), -2.5, 1.5, 0);
      FramePoint lineStart1 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint lineEnd1 = new FramePoint(ReferenceFrame.getWorldFrame(), -4, 4, 0);
      FramePoint intersectionPoint1 = new FramePoint(ReferenceFrame.getWorldFrame(), -2, 2, 0);

      EuclidGeometryTools.orthogonalProjectionOnLineSegment2D(new Point2D(-2.5, 1.5), new Point2D(0, 0), new Point2D(-4, 4));
      FrameVector x1 = new FrameVector(point1.getReferenceFrame());
      x1.sub(point1, intersectionPoint1);
      FrameVector expectedReturn1 = x1;
      FrameVector actualReturn1 = GeometryTools.getPerpendicularVectorFromLineToPoint(point1, lineStart1, lineEnd1, intersectionPoint1);

      assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPerpendicularVectorFromLineToPoint2()
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D expectedPerpendicularVector = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         Point3D expectedIntersection = RandomGeometry.nextPoint3D(random, -10.0, 10.0);

         Vector3D lineDirection = RandomGeometry.nextOrthogonalVector3D(random, expectedPerpendicularVector, true);
         Point3D firstPointOnLine = new Point3D();
         firstPointOnLine.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, expectedIntersection);
         Point3D secondPointOnLine = new Point3D();
         secondPointOnLine.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, expectedIntersection);

         Point3D point = new Point3D();
         point.add(expectedIntersection, expectedPerpendicularVector);

         Point3D actualIntersection = new Point3D();
         double epsilon = Epsilons.ONE_TRILLIONTH;

         if (firstPointOnLine.distance(secondPointOnLine) < 5.0e-4)
            epsilon = Epsilons.ONE_TEN_BILLIONTH; // Loss of precision when the given points defining the line are getting close.

         Vector3D actualPerpendicularVector = GeometryTools.getPerpendicularVectorFromLineToPoint(point, firstPointOnLine, secondPointOnLine,
                                                                                                  actualIntersection);
         EuclidCoreTestTools.assertTuple3DEquals(expectedIntersection, actualIntersection, epsilon);
         EuclidCoreTestTools.assertTuple3DEquals(expectedPerpendicularVector, actualPerpendicularVector, epsilon);

         actualPerpendicularVector = GeometryTools.getPerpendicularVectorFromLineToPoint(point, firstPointOnLine, secondPointOnLine, null);
         EuclidCoreTestTools.assertTuple3DEquals(expectedPerpendicularVector, actualPerpendicularVector, epsilon);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPlaneNormalGivenThreePoints()
   {
      FramePoint point1 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FramePoint point2 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FramePoint point3 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FrameVector expectedReturn = null;
      FrameVector actualReturn = GeometryTools.getPlaneNormalGivenThreePoints(point1, point2, point3);
      assertEquals("test failed", expectedReturn, actualReturn);

      FramePoint point91 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      FramePoint point92 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 5, 0);
      FramePoint point93 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 1, 0);
      FrameVector expectedReturn9 = null;
      FrameVector actualReturn9 = GeometryTools.getPlaneNormalGivenThreePoints(point91, point92, point93);
      assertEquals("test failed", expectedReturn9, actualReturn9);

      FramePoint point81 = new FramePoint(ReferenceFrame.getWorldFrame(), 9, 0, 0);
      FramePoint point82 = new FramePoint(ReferenceFrame.getWorldFrame(), 7, 0, 0);
      FramePoint point83 = new FramePoint(ReferenceFrame.getWorldFrame(), 4, 0, 0);
      FrameVector expectedReturn8 = null;
      FrameVector actualReturn8 = GeometryTools.getPlaneNormalGivenThreePoints(point81, point82, point83);
      assertEquals("test failed", expectedReturn8, actualReturn8);

      FramePoint point71 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 4);
      FramePoint point72 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 6);
      FramePoint point73 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 7);
      FrameVector expectedReturn7 = null;
      FrameVector actualReturn7 = GeometryTools.getPlaneNormalGivenThreePoints(point71, point72, point73);
      assertEquals("test failed", expectedReturn7, actualReturn7);

      FramePoint point11 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 5, 46);
      FramePoint point12 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 587, 3);
      FramePoint point13 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 18, 8);
      FramePoint p1 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 5, 5);
      FramePoint v1 = new FramePoint(ReferenceFrame.getWorldFrame(), 1, 5, 5);
      FrameVector expectedReturn1 = new FrameVector(p1.getReferenceFrame());
      expectedReturn1.sub(p1, v1);
      FrameVector actualReturn1 = GeometryTools.getPlaneNormalGivenThreePoints(point11, point12, point13);
      assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));

      FramePoint point21 = new FramePoint(ReferenceFrame.getWorldFrame(), 65, 0, 46);
      FramePoint point22 = new FramePoint(ReferenceFrame.getWorldFrame(), 43, 0, 3);
      FramePoint point23 = new FramePoint(ReferenceFrame.getWorldFrame(), 13, 0, 8);
      FramePoint p2 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 1, 5);
      FramePoint v2 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 5);
      FrameVector expectedReturn2 = new FrameVector(p2.getReferenceFrame());
      expectedReturn2.sub(p2, v2);
      FrameVector actualReturn2 = GeometryTools.getPlaneNormalGivenThreePoints(point21, point22, point23);
      assertTrue("Test Failed", expectedReturn2.epsilonEquals(actualReturn2, EPSILON));

      FramePoint point31 = new FramePoint(ReferenceFrame.getWorldFrame(), 65, 56, 0);
      FramePoint point32 = new FramePoint(ReferenceFrame.getWorldFrame(), 43, 3, 0);
      FramePoint point33 = new FramePoint(ReferenceFrame.getWorldFrame(), 13, 87, 0);
      FramePoint p3 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 55, 0);
      FramePoint v3 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 55, 1);
      FrameVector expectedReturn3 = new FrameVector(p3.getReferenceFrame());
      expectedReturn3.sub(p3, v3);
      FrameVector actualReturn3 = GeometryTools.getPlaneNormalGivenThreePoints(point31, point32, point33);
      assertTrue("Test Failed", expectedReturn3.epsilonEquals(actualReturn3, EPSILON));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPlaneNormalGivenThreePoints1()
   {
      Point3D point1 = new Point3D(0, 0, 0);
      Point3D point2 = new Point3D(7, 0, 0);
      Point3D point3 = new Point3D(2, 0, 0);
      Vector3D expectedReturn = null;
      Vector3D actualReturn = GeometryTools.getPlaneNormalGivenThreePoints(point1, point2, point3);
      assertEquals("return value", expectedReturn, actualReturn);

      Point3D point01 = new Point3D(15, 0, 0);
      Point3D point02 = new Point3D(15, 0, 0);
      Point3D point03 = new Point3D(15, 0, 0);
      Vector3D expectedReturn1 = null;
      Vector3D actualReturn1 = GeometryTools.getPlaneNormalGivenThreePoints(point01, point02, point03);
      assertEquals("return value", expectedReturn1, actualReturn1);

      Point3D point11 = new Point3D(0, 4, 0);
      Point3D point12 = new Point3D(0, 2, 0);
      Point3D point13 = new Point3D(0, 67, 0);
      Vector3D expectedReturn2 = null;
      Vector3D actualReturn2 = GeometryTools.getPlaneNormalGivenThreePoints(point11, point12, point13);
      assertEquals("return value", expectedReturn2, actualReturn2);

      Point3D point21 = new Point3D(0, 0, 4);
      Point3D point22 = new Point3D(0, 0, 7);
      Point3D point23 = new Point3D(0, 0, 5);
      Vector3D expectedReturn3 = null;
      Vector3D actualReturn3 = GeometryTools.getPlaneNormalGivenThreePoints(point21, point22, point23);
      assertEquals("return value", expectedReturn3, actualReturn3);

      Point3D point31 = new Point3D(0, 67, 5);
      Point3D point32 = new Point3D(0, 3, 7);
      Point3D point33 = new Point3D(0, 90, 7.24264068712);
      Vector3D expectedReturn4 = new Vector3D(-1, 0, 0);
      Vector3D actualReturn4 = GeometryTools.getPlaneNormalGivenThreePoints(point31, point32, point33);
      assertEquals("return value", expectedReturn4, actualReturn4);

      Point3D point41 = new Point3D(45, 0, 5);
      Point3D point42 = new Point3D(35, 0, 7);
      Point3D point43 = new Point3D(132, 0, 7.24264068712);
      Vector3D expectedReturn5 = new Vector3D(0, 1, 0);
      Vector3D actualReturn5 = GeometryTools.getPlaneNormalGivenThreePoints(point41, point42, point43);
      assertTrue("Test Failed", expectedReturn5.epsilonEquals(actualReturn5, EPSILON));

      Point3D point51 = new Point3D(45, 67, 0);
      Point3D point52 = new Point3D(35, 56, 0);
      Point3D point53 = new Point3D(132, -4, 0);
      Vector3D expectedReturn6 = new Vector3D(0, 0, 1);
      Vector3D actualReturn6 = GeometryTools.getPlaneNormalGivenThreePoints(point51, point52, point53);
      assertTrue("Test Failed", expectedReturn6.epsilonEquals(actualReturn6, EPSILON));

      Point3D point61 = new Point3D(1, 5, 7);
      Point3D point62 = new Point3D(1, 5, 7);
      Point3D point63 = new Point3D(5, 12, 4325);
      Vector3D expectedReturn7 = null;
      Vector3D actualReturn7 = GeometryTools.getPlaneNormalGivenThreePoints(point61, point62, point63);
      assertEquals("return value", expectedReturn7, actualReturn7);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetPlaneNormalGivenThreePoints2()
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D expectedPlaneNormal = RandomGeometry.nextVector3D(random, 1.0);

         Point3D firstPointOnPlane = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         Point3D secondPointOnPlane = new Point3D();
         Point3D thirdPointOnPlane = new Point3D();

         Vector3D secondOrthogonalToNormal = RandomGeometry.nextOrthogonalVector3D(random, expectedPlaneNormal, true);
         Vector3D thirdOrthogonalToNormal = RandomGeometry.nextOrthogonalVector3D(random, expectedPlaneNormal, true);

         secondPointOnPlane.scaleAdd(RandomNumbers.nextDouble(random, 1.0, 10.0), secondOrthogonalToNormal, firstPointOnPlane);
         thirdPointOnPlane.scaleAdd(RandomNumbers.nextDouble(random, 1.0, 10.0), thirdOrthogonalToNormal, firstPointOnPlane);

         Vector3D actualPlaneNormal = GeometryTools.getPlaneNormalGivenThreePoints(firstPointOnPlane, secondPointOnPlane, thirdPointOnPlane);

         if (expectedPlaneNormal.dot(actualPlaneNormal) < 0.0)
            actualPlaneNormal.negate();

         EuclidCoreTestTools.assertTuple3DEquals(expectedPlaneNormal, actualPlaneNormal, Epsilons.ONE_TRILLIONTH);
      }
   }

   /*
    * public void testGetTransform() { FramePoint point = null; FrameVector
    * normal = null; Orientation expectedReturn = null; Orientation actualReturn
    * = geometryTools.getTransform(point, normal); assertEquals("return value",
    * expectedReturn, actualReturn); } public void
    * testGetVerticalSpansOfPoints() { double xMin = 0.0; double yMin = 0.0;
    * double zMin = 0.0; double xMax = 0.0; double yMax = 0.0; double zMax =
    * 0.0; double xResolution = 0.0; double yResolution = 0.0; double
    * zResolution = 0.0; ArrayList expectedReturn = null; ArrayList actualReturn
    * = geometryTools.getVerticalSpansOfPoints(xMin, yMin, zMin, xMax, yMax,
    * zMax, xResolution, yResolution, zResolution); assertEquals("return value",
    * expectedReturn, actualReturn); }
    */

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetTopVertexOfIsoscelesTriangle1()
   {
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();

      FramePoint baseVertexA = new FramePoint(frame);
      FramePoint baseVertexC = new FramePoint(frame);
      FramePoint topVertexB = new FramePoint(frame);
      FrameVector trianglePlaneNormal = new FrameVector(frame);

      FramePoint topVertexBComputed = new FramePoint(frame);

      double legLength = 1.5;
      double topVertexAngle = Math.toRadians(1.0);

      while (Math.toDegrees(topVertexAngle) < 179.0)
      {
         trianglePlaneNormal.setIncludingFrame(frame, 0.0, 0.0, 1.0);

         topVertexB.setIncludingFrame(frame, 0.0, 0.0, 0.0);
         baseVertexA.setIncludingFrame(frame, legLength * Math.cos(-0.5 * topVertexAngle), legLength * Math.sin(-0.5 * topVertexAngle), 0.0);
         baseVertexC.setIncludingFrame(frame, legLength * Math.cos(0.5 * topVertexAngle), legLength * Math.sin(0.5 * topVertexAngle), 0.0);

         assertTrue("TopVertexAngle = " + Math.toDegrees(topVertexAngle) + " degrees",
                    GeometryTools.isFormingTriangle(baseVertexA.distance(baseVertexC), baseVertexA.distance(topVertexB), topVertexB.distance(baseVertexC)));

         GeometryTools.getTopVertexOfIsoscelesTriangle(baseVertexA, baseVertexC, trianglePlaneNormal, topVertexAngle, topVertexBComputed);

         String errorMsg = "Computed vertex: " + topVertexBComputed + "\n does not match actual vertex: " + topVertexB + "\n when topVertex Angle = "
               + Math.toDegrees(topVertexAngle) + " degrees \n";
         assertEquals(errorMsg, 0.0, topVertexB.distance(topVertexBComputed), 1e-9);

         topVertexAngle += Math.toRadians(1.0);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testGetTopVertexOfIsoscelesTriangle2()
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point3D expectedB = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         Point3D a = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         Vector3D ba = new Vector3D();
         ba.sub(a, expectedB);

         double abcAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);

         Vector3D triangleNormal = RandomGeometry.nextOrthogonalVector3D(random, ba, true);
         triangleNormal.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));
         AxisAngle abcAxisAngle = new AxisAngle(triangleNormal, abcAngle);
         RotationMatrix abcRotationMatrix = new RotationMatrix();
         abcRotationMatrix.set(abcAxisAngle);
         Vector3D bc = new Vector3D();
         abcRotationMatrix.transform(ba, bc);

         Point3D c = new Point3D();
         c.add(bc, expectedB);

         double epsilon = Epsilons.ONE_TEN_BILLIONTH;

         Point3D actualB = new Point3D();
         GeometryTools.getTopVertexOfIsoscelesTriangle(a, c, triangleNormal, abcAngle, actualB);
         EuclidCoreTestTools.assertTuple3DEquals(expectedB, actualB, epsilon);
         assertEquals(abcAngle, ba.angle(bc), epsilon);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testIsPointOnLeftSideOfLine()
   {
      FramePoint lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 5.0, 0.0, 0.0);
      FramePoint lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 5.0, 10.0, 0.0);

      FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), 10, 5, 0.0);

      boolean expectedReturn = false;
      boolean actualReturn = GeometryTools.isPointOnLeftSideOfLine(point, lineStart, lineEnd);
      assertEquals("return value", expectedReturn, actualReturn);

      /** @todo fill in the test code */
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void testAngleByLawOfCosineWithNegativeSideLengthA()
   {
      double a = 1.0;
      GeometryTools.getUnknownTriangleAngleByLawOfCosine(-a, a, a);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void testAngleByLawOfCosineWithNegativeSideLengthB()
   {
      double a = 1.0;
      GeometryTools.getUnknownTriangleAngleByLawOfCosine(a, -a, a);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void testAngleByLawOfCosineWithNegativeSideLengthC()
   {
      double a = 1.0;
      GeometryTools.getUnknownTriangleAngleByLawOfCosine(a, a, -a);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testAngleByLawOfCosineWithEqualLengthTriangle()
   {
      double a = 1.0;
      double alpha = GeometryTools.getUnknownTriangleAngleByLawOfCosine(a, a, a);
      double expected_alpha = Math.PI / 3.0;
      assertEquals(expected_alpha, alpha, 1e-10);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void testSideLengthByLawOfCosineNegativeSideLengthA()
   {
      double a = 1.0;
      double gamma = Math.PI / 3.0;
      GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(-a, a, gamma);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void testSideLengthByLawOfCosineNegativeSideLengthB()
   {
      double a = 1.0;
      double gamma = Math.PI / 3.0;
      GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, -a, gamma);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void testSideLengthByLawOfCosineGreaterThanPiAngle()
   {
      double a = 1.0;
      double gamma = Math.PI / 2.0 * 3.0;
      GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, a, gamma);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSideLengthByLawOfCosineWithEqualLengthTriangle()
   {
      double a = 1.0;
      double gamma = Math.PI / 3.0;
      double c = GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, a, gamma);
      assertEquals(a, c, 1e-10);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSideLengthByLawOfCosineWithEqualLengthTriangleNegativeAngle()
   {
      double a = 1.0;
      double gamma = -Math.PI / 3.0;
      double c = GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, a, gamma);
      assertEquals(a, c, 1e-10);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testLawOfCosineRadnom()
   {
      Random random = new Random(34534L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D a = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D b = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D c = RandomGeometry.nextPoint2D(random, 10.0, 10.0);

         Vector2D ab = new Vector2D();
         ab.sub(b, a);
         Vector2D ba = new Vector2D();
         ba.sub(a, b);
         Vector2D ac = new Vector2D();
         ac.sub(c, a);
         Vector2D ca = new Vector2D();
         ca.sub(a, c);
         Vector2D bc = new Vector2D();
         bc.sub(c, b);
         Vector2D cb = new Vector2D();
         cb.sub(b, c);

         // The three edge lengths
         double abLength = ab.length();
         double acLength = ac.length();
         double bcLength = bc.length();

         // The three angles
         double abc = Math.abs(ba.angle(bc));
         double bca = Math.abs(cb.angle(ca));
         double cab = Math.abs(ac.angle(ab));

         assertEquals(bcLength, GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(abLength, acLength, cab), Epsilons.ONE_TRILLIONTH);
         assertEquals(abLength, GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(acLength, bcLength, bca), Epsilons.ONE_TRILLIONTH);
         assertEquals(acLength, GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(abLength, bcLength, abc), Epsilons.ONE_TRILLIONTH);

         assertEquals(cab, GeometryTools.getUnknownTriangleAngleByLawOfCosine(abLength, acLength, bcLength), Epsilons.ONE_TRILLIONTH);
         assertEquals(bca, GeometryTools.getUnknownTriangleAngleByLawOfCosine(acLength, bcLength, abLength), Epsilons.ONE_TRILLIONTH);
         assertEquals(abc, GeometryTools.getUnknownTriangleAngleByLawOfCosine(abLength, bcLength, acLength), Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void isFormingTriangleFailTest()
   {
      double a = 1.0;
      double b = 10.0;
      boolean actual = GeometryTools.isFormingTriangle(b, a, a);
      assertEquals(false, actual);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void isFormingTriangleSameSidedTest()
   {
      double a = 1.0;
      boolean actual = GeometryTools.isFormingTriangle(a, a, a);
      assertEquals(true, actual);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000, expected = RuntimeException.class)
   public void illegalPythagorasGetCathetus()
   {
      GeometryTools.pythagorasGetCathetus(1.0, 2.0);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testClipToBoundingBox()
   {
      Tuple3DBasics tuple3d = new Point3D(1.0, -1.0, 0.0);
      GeometryTools.clipToBoundingBox(tuple3d, -0.5, 0.5, 0.5, -0.5, 0.0, 0.0);
      EuclidCoreTestTools.assertTuple3DEquals("not equal", new Point3D(0.5, -0.5, 0.0), tuple3d, 0.0);
      tuple3d.set(1.0, -1.0, 0.0);
      GeometryTools.clipToBoundingBox(tuple3d, 0.5, -0.5, -0.5, 0.5, -0.1, 0.1);
      EuclidCoreTestTools.assertTuple3DEquals("not equal", new Point3D(0.5, -0.5, 0.0), tuple3d, 0.0);
      tuple3d.set(1.0, -1.0, 2.0);
      GeometryTools.clipToBoundingBox(tuple3d, 0.5, -0.5, -0.5, 0.5, -0.1, 1.0);
      EuclidCoreTestTools.assertTuple3DEquals("not equal", new Point3D(0.5, -0.5, 1.0), tuple3d, 0.0);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testCombine()
   {
      Random random = new Random(1176L);
      ArrayList<Point2D> firstList = new ArrayList<Point2D>();
      for (int i = 0; i < 100; i++)
      {
         firstList.add(new Point2D(random.nextDouble(), random.nextDouble()));
      }

      ConvexPolygon2d firstPolygon = new ConvexPolygon2d(firstList);

      ArrayList<Point2D> secondList = new ArrayList<Point2D>();
      for (int i = 0; i < 200; i++)
      {
         secondList.add(new Point2D(random.nextDouble(), random.nextDouble()));
      }

      ConvexPolygon2d secondPolygon = new ConvexPolygon2d(secondList);

      ConvexPolygon2d result = new ConvexPolygon2d(firstPolygon, secondPolygon);

      // convexity of the result is already checked in another test
      for (Point2D point : firstList)
      {
         if (!result.isPointInside(point))
         {
            double distance = result.distance(point);

            if (distance > 1e-7)
               throw new RuntimeException("Not each point is inside the result. distance = " + distance);
         }

         //       assertTrue("Not each point isinside the result. distance = " , result.isPointInside(point));
      }

      for (Point2D point : secondList)
      {
         if (!result.isPointInside(point))
         {
            double distance = result.distance(point);

            if (distance > 1e-7)
               throw new RuntimeException("Not each point is inside the result. distance = " + distance);
         }

         //       assertTrue("Not each point is inside the result", result.isPointInside(point));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetAngleFromFirstToSecondVector() throws Exception
   {
      Random random = new Random(51651L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         double firstVectorLength = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double secondVectorLength = RandomNumbers.nextDouble(random, 0.0, 10.0);
         Vector2D firstVector = RandomGeometry.nextVector2D(random, firstVectorLength);
         Vector2D secondVector = new Vector2D();

         for (double yaw = -Math.PI; yaw <= Math.PI; yaw += Math.PI / 100.0)
         {
            double c = Math.cos(yaw);
            double s = Math.sin(yaw);
            secondVector.setX(firstVector.getX() * c - firstVector.getY() * s);
            secondVector.setY(firstVector.getX() * s + firstVector.getY() * c);
            secondVector.scale(secondVectorLength / firstVectorLength);
            double computedYaw = GeometryTools.getAngleFromFirstToSecondVector(firstVector, secondVector);
            double yawDifference = AngleTools.computeAngleDifferenceMinusPiToPi(yaw, computedYaw);
            assertEquals(0.0, yawDifference, Epsilons.ONE_TRILLIONTH);
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testNormalizeSafeZUp() throws Exception
   {
      Vector3D actualVector;
      Vector3D expectedVector = new Vector3D();
      Random random = new Random(1176L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         actualVector = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, Epsilons.ONE_TRILLIONTH, 10.0));

         expectedVector.setAndNormalize(actualVector);
         GeometryTools.normalizeSafelyZUp(actualVector);
         EuclidCoreTestTools.assertTuple3DEquals(expectedVector, actualVector, Epsilons.ONE_TRILLIONTH);

         actualVector = RandomGeometry.nextVector3D(random, 0.999 * Epsilons.ONE_TRILLIONTH);
         expectedVector.set(0.0, 0.0, 1.0);
         GeometryTools.normalizeSafelyZUp(actualVector);
         EuclidCoreTestTools.assertTuple3DEquals(expectedVector, actualVector, Epsilons.ONE_TRILLIONTH);

         actualVector = new Vector3D();
         expectedVector.set(0.0, 0.0, 1.0);
         GeometryTools.normalizeSafelyZUp(actualVector);
         EuclidCoreTestTools.assertTuple3DEquals(expectedVector, actualVector, Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetRadiusOfArc() throws Exception
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         double expectedArcRadius = RandomNumbers.nextDouble(random, 0.1, 100.0);
         double chordAngle = RandomNumbers.nextDouble(random, -3.0 * Math.PI, 3.0 * Math.PI);
         double chordLength = 2.0 * expectedArcRadius * Math.sin(0.5 * chordAngle);
         double actualArcRadius = GeometryTools.getRadiusOfArc(chordLength, chordAngle);
         assertEquals(expectedArcRadius, actualArcRadius, Epsilons.ONE_TRILLIONTH);
      }

      assertTrue(Double.isNaN(GeometryTools.getRadiusOfArc(1.0, 0.0)));
      assertTrue(Double.isNaN(GeometryTools.getRadiusOfArc(1.0, Math.PI)));
      assertTrue(Double.isNaN(GeometryTools.getRadiusOfArc(1.0, -Math.PI)));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetAxisAngleFromFirstToSecondVector1() throws Exception
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D firstVector = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         double expectedAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI);
         Vector3D expectedAxis = RandomGeometry.nextOrthogonalVector3D(random, firstVector, true);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(expectedAxisAngle);

         Vector3D secondVector = new Vector3D();
         rotationMatrix.transform(firstVector, secondVector);
         secondVector.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         AxisAngle actualAxisAngle = new AxisAngle();
         GeometryTools.getAxisAngleFromFirstToSecondVector(firstVector, secondVector, actualAxisAngle);

         Vector3D actualAxis = new Vector3D(actualAxisAngle.getX(), actualAxisAngle.getY(), actualAxisAngle.getZ());

         assertEquals(1.0, actualAxis.length(), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, actualAxis.dot(firstVector), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, actualAxis.dot(secondVector), Epsilons.ONE_TRILLIONTH);

         assertEquals(0.0, expectedAxis.dot(firstVector), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, expectedAxis.dot(secondVector), Epsilons.ONE_TRILLIONTH);

         if (actualAxisAngle.getAngle() * expectedAxisAngle.getAngle() < 0.0)
         {
            expectedAxis.negate();
            expectedAngle = -expectedAngle;
            expectedAxisAngle.set(expectedAxis, expectedAngle);
         }

         try
         {
            assertTrue(expectedAxisAngle.epsilonEquals(actualAxisAngle, Epsilons.ONE_TRILLIONTH));
         }
         catch (AssertionError e)
         {
            throw new AssertionError("expected:\n<" + expectedAxisAngle + ">\n but was:\n<" + actualAxisAngle + ">");
         }
      }

      // Test close to 0.0
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D firstVector = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         double expectedAngle = RandomNumbers.nextDouble(random, 0.0001, 0.001);
         if (random.nextBoolean())
            expectedAngle = -expectedAngle;
         Vector3D expectedAxis = RandomGeometry.nextOrthogonalVector3D(random, firstVector, true);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(expectedAxisAngle);

         Vector3D secondVector = new Vector3D();
         rotationMatrix.transform(firstVector, secondVector);
         secondVector.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         AxisAngle actualAxisAngle = new AxisAngle();
         GeometryTools.getAxisAngleFromFirstToSecondVector(firstVector, secondVector, actualAxisAngle);

         Vector3D actualAxis = new Vector3D(actualAxisAngle.getX(), actualAxisAngle.getY(), actualAxisAngle.getZ());

         assertEquals(1.0, actualAxis.length(), Epsilons.ONE_TRILLIONTH);
         // Can not be as accurate as we get closer to 0.0
         assertEquals(0.0, actualAxis.dot(firstVector), Epsilons.ONE_TEN_BILLIONTH);
         assertEquals(0.0, actualAxis.dot(secondVector), Epsilons.ONE_TEN_BILLIONTH);

         assertEquals(0.0, expectedAxis.dot(firstVector), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, expectedAxis.dot(secondVector), Epsilons.ONE_TRILLIONTH);

         if (actualAxisAngle.getAngle() * expectedAxisAngle.getAngle() < 0.0)
         {
            expectedAxis.negate();
            expectedAngle = -expectedAngle;
            expectedAxisAngle.set(expectedAxis, expectedAngle);
         }

         try
         {
            // Can not be as accurate as we get closer to 0.0
            assertTrue(expectedAxisAngle.epsilonEquals(actualAxisAngle, Epsilons.ONE_TEN_BILLIONTH));
         }
         catch (AssertionError e)
         {
            throw new AssertionError("expected:\n<" + expectedAxisAngle + ">\n but was:\n<" + actualAxisAngle + ">");
         }
      }

      // Test close to Math.PI
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D referenceNormal = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         double expectedAngle = RandomNumbers.nextDouble(random, 0.00001, 0.001);
         if (random.nextBoolean())
            expectedAngle = -expectedAngle;
         expectedAngle += Math.PI;
         Vector3D expectedAxis = RandomGeometry.nextOrthogonalVector3D(random, referenceNormal, true);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(expectedAxisAngle);

         Vector3D rotatedNormal = new Vector3D();
         rotationMatrix.transform(referenceNormal, rotatedNormal);
         rotatedNormal.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         AxisAngle actualAxisAngle = new AxisAngle();
         GeometryTools.getAxisAngleFromFirstToSecondVector(referenceNormal, rotatedNormal, actualAxisAngle);

         Vector3D actualAxis = new Vector3D(actualAxisAngle.getX(), actualAxisAngle.getY(), actualAxisAngle.getZ());

         assertEquals(1.0, actualAxis.length(), Epsilons.ONE_TRILLIONTH);
         // Can not be as accurate as we get closer to Math.PI
         assertEquals(0.0, actualAxis.dot(referenceNormal), Epsilons.ONE_TEN_BILLIONTH);
         assertEquals(0.0, actualAxis.dot(rotatedNormal), Epsilons.ONE_TEN_BILLIONTH);

         assertEquals(0.0, expectedAxis.dot(referenceNormal), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, expectedAxis.dot(rotatedNormal), Epsilons.ONE_TRILLIONTH);
         if (actualAxisAngle.getAngle() * expectedAxisAngle.getAngle() < 0.0)
         {
            expectedAxis.negate();
            expectedAngle = -expectedAngle;
            expectedAxisAngle.set(expectedAxis, expectedAngle);
         }

         if (Math.abs(expectedAxisAngle.getAngle() + actualAxisAngle.getAngle()) > 2.0 * Math.PI - 0.1)
         {
            // Here the sign of the axis does not matter.
            if (expectedAxis.dot(actualAxis) < 0.0)
               expectedAxis.negate();
            // Can not be as accurate as we get closer to Math.PI
            EuclidCoreTestTools.assertTuple3DEquals(expectedAxis, actualAxis, Epsilons.ONE_TEN_BILLIONTH);
         }
         else
         {
            try
            {
               assertTrue(expectedAxisAngle.epsilonEquals(actualAxisAngle, Epsilons.ONE_TRILLIONTH));
            }
            catch (AssertionError e)
            {
               throw new AssertionError("expected:\n<" + expectedAxisAngle + ">\n but was:\n<" + actualAxisAngle + ">");
            }
         }
      }

      // Test exactly at 0.0
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D referenceNormal = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         Vector3D rotatedNormal = new Vector3D(referenceNormal);
         rotatedNormal.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));
         double expectedAngle = 0.0;
         Vector3D expectedAxis = new Vector3D(1.0, 0.0, 0.0);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);

         AxisAngle actualAxisAngle = new AxisAngle();
         GeometryTools.getAxisAngleFromFirstToSecondVector(referenceNormal, rotatedNormal, actualAxisAngle);

         Vector3D actualAxis = new Vector3D(actualAxisAngle.getX(), actualAxisAngle.getY(), actualAxisAngle.getZ());

         assertEquals(1.0, actualAxis.length(), Epsilons.ONE_TRILLIONTH);

         if (actualAxisAngle.getAngle() * expectedAxisAngle.getAngle() < 0.0)
         {
            expectedAxis.negate();
            expectedAngle = -expectedAngle;
            expectedAxisAngle.set(expectedAxis, expectedAngle);
         }

         try
         {
            assertTrue(expectedAxisAngle.epsilonEquals(actualAxisAngle, Epsilons.ONE_TRILLIONTH));
         }
         catch (AssertionError e)
         {
            throw new AssertionError("expected:\n<" + expectedAxisAngle + ">\n but was:\n<" + actualAxisAngle + ">");
         }
      }

      // Test exactly at Math.PI
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D referenceNormal = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         Vector3D rotatedNormal = new Vector3D();
         rotatedNormal.setAndNegate(referenceNormal);
         rotatedNormal.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));
         double expectedAngle = Math.PI;
         Vector3D expectedAxis = new Vector3D(1.0, 0.0, 0.0);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);

         AxisAngle actualAxisAngle = new AxisAngle();
         GeometryTools.getAxisAngleFromFirstToSecondVector(referenceNormal, rotatedNormal, actualAxisAngle);

         Vector3D actualAxis = new Vector3D(actualAxisAngle.getX(), actualAxisAngle.getY(), actualAxisAngle.getZ());

         assertEquals(1.0, actualAxis.length(), Epsilons.ONE_TRILLIONTH);

         if (actualAxisAngle.getAngle() * expectedAxisAngle.getAngle() < 0.0)
         {
            expectedAxis.negate();
            expectedAngle = -expectedAngle;
            expectedAxisAngle.set(expectedAxis, expectedAngle);
         }

         try
         {
            assertTrue(expectedAxisAngle.epsilonEquals(actualAxisAngle, Epsilons.ONE_TRILLIONTH));
         }
         catch (AssertionError e)
         {
            throw new AssertionError("expected:\n<" + expectedAxisAngle + ">\n but was:\n<" + actualAxisAngle + ">");
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetAxisAngleFromFirstToSecondVector2() throws Exception
   {
      Random random = new Random(1176L);
      // Test getRotationBasedOnNormal(AxisAngle4d rotationToPack, Vector3d normalVector3d)
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D referenceNormal = new Vector3D(0.0, 0.0, 1.0);
         double expectedAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI);
         Vector3D expectedAxis = RandomGeometry.nextOrthogonalVector3D(random, referenceNormal, true);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(expectedAxisAngle);

         Vector3D rotatedNormal = new Vector3D();
         rotationMatrix.transform(referenceNormal, rotatedNormal);
         rotatedNormal.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         AxisAngle actualAxisAngle = new AxisAngle();
         GeometryTools.getAxisAngleFromFirstToSecondVector(referenceNormal, rotatedNormal, actualAxisAngle);

         Vector3D actualAxis = new Vector3D(actualAxisAngle.getX(), actualAxisAngle.getY(), actualAxisAngle.getZ());

         assertEquals(1.0, actualAxis.length(), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, actualAxis.dot(referenceNormal), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, actualAxis.dot(rotatedNormal), Epsilons.ONE_TRILLIONTH);

         assertEquals(0.0, expectedAxis.dot(referenceNormal), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, expectedAxis.dot(rotatedNormal), Epsilons.ONE_TRILLIONTH);

         if (actualAxisAngle.getAngle() * expectedAxisAngle.getAngle() < 0.0)
         {
            expectedAxis.negate();
            expectedAngle = -expectedAngle;
            expectedAxisAngle.set(expectedAxis, expectedAngle);
         }

         try
         {
            assertTrue(expectedAxisAngle.epsilonEquals(actualAxisAngle, Epsilons.ONE_TRILLIONTH));
         }
         catch (AssertionError e)
         {
            throw new AssertionError("expected:\n<" + expectedAxisAngle + ">\n but was:\n<" + actualAxisAngle + ">");
         }
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetPerpendicularBisectorSegment1() throws Exception
   {
      Point2D firstLinePoint = new Point2D(1.0, 1.0);
      Point2D secondLinePoint = new Point2D(0.0, 1.0);
      double lengthOffset = 2.0;
      List<Point2D> normalPointsFromLine = GeometryTools.getPerpendicularBisectorSegment(firstLinePoint, secondLinePoint, lengthOffset);
      EuclidCoreTestTools.assertTuple2DEquals(new Point2D(0.5, -1.0), normalPointsFromLine.get(0), Epsilons.ONE_TRILLIONTH);
      EuclidCoreTestTools.assertTuple2DEquals(new Point2D(0.5, 3.0), normalPointsFromLine.get(1), Epsilons.ONE_TRILLIONTH);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetPerpendicularBisectorSegment2() throws Exception
   {
      Random random = new Random(1176L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D firstPointOnLine = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D secondPointOnLine = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Vector2D lineDirection = new Vector2D();
         lineDirection.sub(secondPointOnLine, firstPointOnLine);
         double lengthOffset = RandomNumbers.nextDouble(random, 0.0, 10.0);
         List<Point2D> normalPointsFromLine = GeometryTools.getPerpendicularBisectorSegment(firstPointOnLine, secondPointOnLine, lengthOffset);

         Point2D normalPoint0 = normalPointsFromLine.get(0);
         Point2D normalPoint1 = normalPointsFromLine.get(1);
         Vector2D normalDirection = new Vector2D();
         normalDirection.sub(normalPoint1, normalPoint0);

         assertEquals(2.0 * lengthOffset, normalDirection.length(), Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, lineDirection.dot(normalDirection), Epsilons.ONE_TRILLIONTH);
         assertEquals(lengthOffset, EuclidGeometryTools.distanceFromPoint2DToLine2D(normalPoint0, firstPointOnLine, secondPointOnLine), Epsilons.ONE_TRILLIONTH);
         assertEquals(lengthOffset, EuclidGeometryTools.distanceFromPoint2DToLine2D(normalPoint1, firstPointOnLine, secondPointOnLine), Epsilons.ONE_TRILLIONTH);
         assertTrue(EuclidGeometryTools.isPoint2DOnLeftSideOfLine2D(normalPoint0, firstPointOnLine, secondPointOnLine));
         assertTrue(EuclidGeometryTools.isPoint2DOnRightSideOfLine2D(normalPoint1, firstPointOnLine, secondPointOnLine));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetTriangleBisector() throws Exception
   {
      Random random = new Random(1176L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2D a = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D b = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D c = RandomGeometry.nextPoint2D(random, 10.0, 10.0);

         Vector2D ba = new Vector2D();
         ba.sub(a, b);
         Vector2D bc = new Vector2D();
         bc.sub(c, b);

         double abcAngle = ba.angle(bc);

         Point2D x = new Point2D();
         GeometryTools.getTriangleBisector(a, b, c, x);

         Vector2D bx = new Vector2D();
         bx.sub(x, b);

         double abxAngle = ba.angle(bx);

         assertEquals(0.5 * abcAngle, abxAngle, Epsilons.ONE_TRILLIONTH);
         assertEquals(0.0, EuclidGeometryTools.distanceFromPoint2DToLine2D(x, a, c), Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetXYDistance() throws Exception
   {
      Random random = new Random(232L);

      for (int i = 0; i < 1000; i++)
      {
         Point3D firstPoint3d = RandomGeometry.nextPoint3D(random, 10.0, 10.0, 10.0);
         Point3D secondPoint3d = RandomGeometry.nextPoint3D(random, 10.0, 10.0, 10.0);
         Point2D firstPoint2d = new Point2D(firstPoint3d.getX(), firstPoint3d.getY());
         Point2D secondPoint2d = new Point2D(secondPoint3d.getX(), secondPoint3d.getY());
         double expectedDistance = firstPoint2d.distance(secondPoint2d);
         double actualDistance = GeometryTools.getXYDistance(firstPoint3d, secondPoint3d);
         assertEquals(expectedDistance, actualDistance, Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testGetAngleFromFirstToSecondVector3D() throws Exception
   {
      Random random = new Random(1176L);
      // Test getRotationBasedOnNormal(AxisAngle4d rotationToPack, Vector3d normalVector3d)
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D firstVector = new Vector3D(0.0, 0.0, 1.0);
         double expectedAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI);
         Vector3D expectedAxis = RandomGeometry.nextOrthogonalVector3D(random, firstVector, true);
         AxisAngle expectedAxisAngle = new AxisAngle(expectedAxis, expectedAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(expectedAxisAngle);

         Vector3D secondVector = new Vector3D();
         rotationMatrix.transform(firstVector, secondVector);
         secondVector.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         double actualAngle = GeometryTools.getAngleFromFirstToSecondVector(firstVector, secondVector);

         assertEquals(expectedAngle, actualAngle, Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAreVectorsCollinear3D() throws Exception
   {
      Random random = new Random(232L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D firstVector = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));

         Vector3D rotationAxis = RandomGeometry.nextOrthogonalVector3D(random, firstVector, true);
         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);

         AxisAngle rotationAxisAngle = new AxisAngle(rotationAxis, rotationAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(rotationAxisAngle);

         Vector3D secondVector = new Vector3D();
         rotationMatrix.transform(firstVector, secondVector);
         secondVector.normalize();
         secondVector.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         assertEquals(rotationAngle < angleEpsilon, GeometryTools.areVectorsCollinear(firstVector, secondVector, angleEpsilon));
      }

      // Try again with small values
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D firstVector = RandomGeometry.nextVector3D(random, 1.0);

         Vector3D rotationAxis = RandomGeometry.nextOrthogonalVector3D(random, firstVector, true);
         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Epsilons.ONE_MILLIONTH * Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Epsilons.ONE_MILLIONTH * Math.PI / 2.0);
         if (Math.abs(rotationAngle - angleEpsilon) < 1.0e-7)
            continue; // This is the limit of accuracy.

         AxisAngle rotationAxisAngle = new AxisAngle(rotationAxis, rotationAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(rotationAxisAngle);

         Vector3D secondVector = new Vector3D();
         rotationMatrix.transform(firstVector, secondVector);

         assertEquals(rotationAngle < angleEpsilon, GeometryTools.areVectorsCollinear(firstVector, secondVector, angleEpsilon));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAreVectorsCollinear2D() throws Exception
   {
      Random random = new Random(232L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector2D firstVector = RandomGeometry.nextVector2D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));

         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);

         Vector2D secondVector = new Vector2D();
         GeometryTools.rotateTuple2d(rotationAngle, firstVector, secondVector);
         secondVector.normalize();
         secondVector.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         assertEquals(rotationAngle < angleEpsilon, GeometryTools.areVectorsCollinear(firstVector, secondVector, angleEpsilon));
      }

      // Try again with small values
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector2D firstVector = RandomGeometry.nextVector2D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));

         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Epsilons.ONE_MILLIONTH * Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Epsilons.ONE_MILLIONTH * Math.PI / 2.0);
         if (Math.abs(rotationAngle - angleEpsilon) < 1.0e-7)
            continue; // This is the limit of accuracy.

         Vector2D secondVector = new Vector2D();
         GeometryTools.rotateTuple2d(rotationAngle, firstVector, secondVector);

         assertEquals(rotationAngle < angleEpsilon, GeometryTools.areVectorsCollinear(firstVector, secondVector, angleEpsilon));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAreLinesCollinear2D() throws Exception
   {
      Random random = new Random(232L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector2D lineDirection1 = RandomGeometry.nextVector2D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));

         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);

         Vector2D lineDirection2 = new Vector2D();
         GeometryTools.rotateTuple2d(rotationAngle, lineDirection1, lineDirection2);
         lineDirection2.normalize();
         lineDirection2.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         Point2D firstPointOnLine1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D secondPointOnLine1 = new Point2D();
         secondPointOnLine1.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection1, firstPointOnLine1);

         Vector2D orthogonal = GeometryTools.getPerpendicularVector(lineDirection1);
         orthogonal.normalize();
         double distance = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double distanceEspilon = RandomNumbers.nextDouble(random, 0.0, 10.0);

         Point2D firstPointOnLine2 = new Point2D();
         firstPointOnLine2.scaleAdd(distance, orthogonal, firstPointOnLine1);
         Point2D secondPointOnLine2 = new Point2D();
         secondPointOnLine2.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection2, firstPointOnLine2);

         boolean expectedCollinear = rotationAngle < angleEpsilon && distance < distanceEspilon;
         boolean actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, secondPointOnLine1, firstPointOnLine2, secondPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
      }

      // Test only the distance with parallel line segments.
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector2D lineDirection = RandomGeometry.nextVector2D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         Vector2D orthogonal = GeometryTools.getPerpendicularVector(lineDirection);
         orthogonal.normalize();

         double angleEpsilon = Epsilons.ONE_MILLIONTH;

         Point2D firstPointOnLine1 = RandomGeometry.nextPoint2D(random, 10.0, 10.0);
         Point2D secondPointOnLine1 = new Point2D();
         secondPointOnLine1.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, firstPointOnLine1);

         double distance = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double distanceEspilon = RandomNumbers.nextDouble(random, 0.0, 10.0);

         Point2D firstPointOnLine2 = new Point2D();
         firstPointOnLine2.scaleAdd(distance, orthogonal, firstPointOnLine1);
         Point2D secondPointOnLine2 = new Point2D();
         secondPointOnLine2.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, firstPointOnLine2);
         firstPointOnLine2.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, firstPointOnLine2);

         boolean expectedCollinear = distance < distanceEspilon;
         boolean actualCollinear;
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, secondPointOnLine1, firstPointOnLine2, secondPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, secondPointOnLine1, secondPointOnLine2, firstPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine1, firstPointOnLine1, secondPointOnLine2, firstPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine1, firstPointOnLine1, firstPointOnLine2, secondPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);

         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine2, secondPointOnLine2, firstPointOnLine1, secondPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);                                                                                
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine2, firstPointOnLine2, firstPointOnLine1, secondPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);                                                                                
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine2, firstPointOnLine2, secondPointOnLine1, firstPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);                                                                                
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine2, secondPointOnLine2, secondPointOnLine1, firstPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAreLinesCollinear3D() throws Exception
   {
      Random random = new Random(2312L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D lineDirection1 = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));

         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);
         Vector3D orthogonal = RandomGeometry.nextOrthogonalVector3D(random, lineDirection1, true);
         AxisAngle axisAngle = new AxisAngle(orthogonal, rotationAngle);

         Vector3D lineDirection2 = new Vector3D();
         axisAngle.transform(lineDirection1, lineDirection2);
         lineDirection2.normalize();
         lineDirection2.scale(RandomNumbers.nextDouble(random, 0.0, 10.0));

         Point3D firstPointOnLine1 = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         Point3D secondPointOnLine1 = new Point3D();
         secondPointOnLine1.scaleAdd(RandomNumbers.nextDouble(random, 0.0, 10.0), lineDirection1, firstPointOnLine1);

         double distance = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double distanceEspilon = RandomNumbers.nextDouble(random, 0.0, 10.0);

         Point3D firstPointOnLine2 = new Point3D();
         firstPointOnLine2.scaleAdd(distance, orthogonal, firstPointOnLine1);
         Point3D secondPointOnLine2 = new Point3D();
         secondPointOnLine2.scaleAdd(RandomNumbers.nextDouble(random, 0.0, 10.0), lineDirection2, firstPointOnLine2);

         boolean expectedCollinear = rotationAngle < angleEpsilon && distance < distanceEspilon;
         boolean actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, secondPointOnLine1, firstPointOnLine2, secondPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, lineDirection1, firstPointOnLine2, lineDirection2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
      }

      // Test only the distance with parallel line segments.
      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector3D lineDirection = RandomGeometry.nextVector3D(random, RandomNumbers.nextDouble(random, 0.0, 10.0));
         Vector3D orthogonal = RandomGeometry.nextOrthogonalVector3D(random, lineDirection, true);

         double angleEpsilon = Epsilons.ONE_MILLIONTH;

         Point3D firstPointOnLine1 = RandomGeometry.nextPoint3D(random, -10.0, 10.0);
         Point3D secondPointOnLine1 = new Point3D();
         secondPointOnLine1.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, firstPointOnLine1);

         double distance = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double distanceEspilon = RandomNumbers.nextDouble(random, 0.0, 10.0);

         Point3D firstPointOnLine2 = new Point3D();
         firstPointOnLine2.scaleAdd(distance, orthogonal, firstPointOnLine1);
         Point3D secondPointOnLine2 = new Point3D();
         secondPointOnLine2.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, firstPointOnLine2);
         firstPointOnLine2.scaleAdd(RandomNumbers.nextDouble(random, 10.0), lineDirection, firstPointOnLine2);

         boolean expectedCollinear = distance < distanceEspilon;
         boolean actualCollinear;
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, secondPointOnLine1, firstPointOnLine2, secondPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine1, secondPointOnLine1, secondPointOnLine2, firstPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine1, firstPointOnLine1, secondPointOnLine2, firstPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine1, firstPointOnLine1, firstPointOnLine2, secondPointOnLine2, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);

         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine2, secondPointOnLine2, firstPointOnLine1, secondPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);                                                                                
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine2, firstPointOnLine2, firstPointOnLine1, secondPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);                                                                                
         actualCollinear = GeometryTools.areLinesCollinear(secondPointOnLine2, firstPointOnLine2, secondPointOnLine1, firstPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);                                                                                
         actualCollinear = GeometryTools.areLinesCollinear(firstPointOnLine2, secondPointOnLine2, secondPointOnLine1, firstPointOnLine1, angleEpsilon, distanceEspilon);
         assertEquals(expectedCollinear, actualCollinear);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testRotateTuple2d() throws Exception
   {
      Random random = new Random(232L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Vector2D original2d = RandomGeometry.nextVector2D(random, RandomNumbers.nextDouble(random, 10.0));
         Vector3D original3d = new Vector3D(original2d.getX(), original2d.getY(), 0.0);

         double yaw = RandomNumbers.nextDouble(random, 3.0 * Math.PI);

         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.setToYawMatrix(yaw);

         Vector2D expectedTransformed2d = new Vector2D();
         Vector3D expectedTransformed3d = new Vector3D();
         rotationMatrix.transform(original3d, expectedTransformed3d);
         expectedTransformed2d.set(expectedTransformed3d.getX(), expectedTransformed3d.getY());

         Vector2D actualTransformed2d = new Vector2D();
         GeometryTools.rotateTuple2d(yaw, original2d, actualTransformed2d);

         EuclidCoreTestTools.assertTuple2DEquals(expectedTransformed2d, actualTransformed2d, Epsilons.ONE_TRILLIONTH);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testArePlanesCoincident() throws Exception
   {
      Random random = new Random();//232L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point3D pointOnPlane1 = RandomGeometry.nextPoint3D(random, 10.0, 10.0, 10.0);
         Vector3D planeNormal1 = RandomGeometry.nextVector3D(random, 1.0);

         Point3D pointOnPlane2 = new Point3D();
         Vector3D planeNormal2 = new Vector3D();

         double distanceEpsilon = RandomNumbers.nextDouble(random, 1.0);
         double distanceBetweenPlanes = RandomNumbers.nextDouble(random, 1.0);

         pointOnPlane2.scaleAdd(distanceBetweenPlanes, planeNormal1, pointOnPlane1);

         Vector3D rotationAxis = RandomGeometry.nextOrthogonalVector3D(random, planeNormal1, true);
         double angleEpsilon = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);
         double rotationAngle = RandomNumbers.nextDouble(random, 0.0, Math.PI / 2.0);

         AxisAngle rotationAxisAngle = new AxisAngle(rotationAxis, rotationAngle);
         RotationMatrix rotationMatrix = new RotationMatrix();
         rotationMatrix.set(rotationAxisAngle);

         rotationMatrix.transform(planeNormal1, planeNormal2);

         boolean expectedCoincidentResult = Math.abs(distanceBetweenPlanes) < distanceEpsilon && rotationAngle < angleEpsilon;
         boolean actualCoincidentResult = GeometryTools.arePlanesCoincident(pointOnPlane1, planeNormal1, pointOnPlane2, planeNormal2, angleEpsilon,
                                                                        distanceEpsilon);
         assertEquals(expectedCoincidentResult, actualCoincidentResult);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testIsZero() throws Exception
   {
      Random random = new Random(23423L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         double x = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double y = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double z = RandomNumbers.nextDouble(random, 0.0, 10.0);
         double epsilon = RandomNumbers.nextDouble(random, 0.0, 10.0);

         boolean isTuple2dZero = x < epsilon && y < epsilon;
         boolean isTuple3dZero = x < epsilon && y < epsilon && z < epsilon;

         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, -y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, -y), epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, y), -epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, y), -epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(-x, -y), -epsilon));
         assertEquals(isTuple2dZero, GeometryTools.isZero(new Point2D(x, -y), -epsilon));

         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, -z), epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, y, -z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(x, -y, -z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, y, -z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, z), -epsilon));
         assertEquals(isTuple3dZero, GeometryTools.isZero(new Point3D(-x, -y, -z), -epsilon));
      }
   }

   public static void main(String[] args)
   {
      MutationTestFacilitator.facilitateMutationTestForClass(GeometryTools.class, GeometryToolsTest.class);
   }
}
