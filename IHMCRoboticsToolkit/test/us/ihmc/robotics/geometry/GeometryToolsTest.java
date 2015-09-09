package us.ihmc.robotics.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

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
   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }

   private static double EPSILON = 1e-6;

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAveragePoints()
   {
      Point3d a = new Point3d(5.8, 9.9, 4.5);
      Point3d b = new Point3d(5.6, 8.1, 5.5);
      double expectedReturn1 = 5.7;
      double expectedReturn2 = 9.0;
      double expectedReturn3 = 5;
      Point3d actualReturn = GeometryTools.averagePoints(a, b);
      double actualReturn1 = actualReturn.x;
      double actualReturn2 = actualReturn.y;
      double actualReturn3 = actualReturn.z;
      assertEquals("return value", expectedReturn1, actualReturn1, EPSILON);
      assertEquals("return value", expectedReturn2, actualReturn2, EPSILON);
      assertEquals("return value", expectedReturn3, actualReturn3, EPSILON);

      Point3d a1 = new Point3d(-5, -5, -5);
      Point3d b1 = new Point3d(-5, -5, -5);
      double expectedReturn11 = -5;
      double expectedReturn12 = -5;
      double expectedReturn13 = -5;
      Point3d actualReturn01 = GeometryTools.averagePoints(a1, b1);
      double actualReturn11 = actualReturn01.x;
      double actualReturn12 = actualReturn01.y;
      double actualReturn13 = actualReturn01.z;
      assertEquals("return value", expectedReturn11, actualReturn11, EPSILON);
      assertEquals("return value", expectedReturn12, actualReturn12, EPSILON);
      assertEquals("return value", expectedReturn13, actualReturn13, EPSILON);

      Point3d a2 = new Point3d(0, 0, 0);
      Point3d b2 = new Point3d(0, 0, 0);
      double expectedReturn21 = 0;
      double expectedReturn22 = 0;
      double expectedReturn23 = 0;
      Point3d actualReturn02 = GeometryTools.averagePoints(a2, b2);
      double actualReturn21 = actualReturn02.x;
      double actualReturn22 = actualReturn02.y;
      double actualReturn23 = actualReturn02.z;
      assertEquals("return value", expectedReturn21, actualReturn21, EPSILON);
      assertEquals("return value", expectedReturn22, actualReturn22, EPSILON);
      assertEquals("return value", expectedReturn23, actualReturn23, EPSILON);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAveragePoints1()
   {
      ArrayList<Point2d> points = new ArrayList<Point2d>();
      Point2d a = new Point2d(1.0, 4.6);
      Point2d b = new Point2d(5.2, 6.0);
      Point2d c = new Point2d(3.7, 2.0);
      points.add(a);
      points.add(b);
      points.add(c);
      double expectedReturn1 = 3.3;
      double expectedReturn2 = 4.2;
      Point2d actualReturn = GeometryTools.averagePoint2ds(points);
      double actualReturn1 = actualReturn.x;
      double actualReturn2 = actualReturn.y;
      assertEquals("return value", expectedReturn1, actualReturn1, EPSILON);
      assertEquals("return value", expectedReturn2, actualReturn2, EPSILON);

      ArrayList<Point2d> points1 = new ArrayList<Point2d>();
      Point2d a1 = new Point2d(0.0, 0.0);
      Point2d b1 = new Point2d(0.0, 0.0);
      Point2d c1 = new Point2d(0.0, 0.0);
      points1.add(a1);
      points1.add(b1);
      points1.add(c1);
      double expectedReturn11 = 0.0;
      double expectedReturn12 = 0.0;
      Point2d actualReturn01 = GeometryTools.averagePoint2ds(points1);
      double actualReturn11 = actualReturn01.x;
      double actualReturn12 = actualReturn01.y;
      assertEquals("return value", expectedReturn11, actualReturn11, EPSILON);
      assertEquals("return value", expectedReturn12, actualReturn12, EPSILON);

      ArrayList<Point2d> points2 = new ArrayList<Point2d>();
      Point2d a2 = new Point2d(-1.0, -4.6);
      Point2d b2 = new Point2d(-5.2, -6.0);
      Point2d c2 = new Point2d(-3.7, -2.0);
      points2.add(a2);
      points2.add(b2);
      points2.add(c2);
      double expectedReturn21 = -3.3;
      double expectedReturn22 = -4.2;
      Point2d actualReturn02 = GeometryTools.averagePoint2ds(points2);
      double actualReturn21 = actualReturn02.x;
      double actualReturn22 = actualReturn02.y;
      assertEquals("return value", expectedReturn21, actualReturn21, EPSILON);
      assertEquals("return value", expectedReturn22, actualReturn22, EPSILON);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAveragePoints2()
   {
      ArrayList<Point3d> points = new ArrayList<Point3d>();
      Point3d a = new Point3d(4.3, 5.6, 3.6);
      Point3d b = new Point3d(8.1, 8.4, 0.0);
      Point3d c = new Point3d(5.6, 1.0, 4.5);
      points.add(a);
      points.add(b);
      points.add(c);
      double expectedReturn1 = 6.0;
      double expectedReturn2 = 5.0;
      double expectedReturn3 = 2.7;
      Point3d actualReturn = GeometryTools.averagePoint3ds(points);
      double actualReturn1 = actualReturn.x;
      double actualReturn2 = actualReturn.y;
      double actualReturn3 = actualReturn.z;
      assertEquals("return value", expectedReturn1, actualReturn1, EPSILON);
      assertEquals("return value", expectedReturn2, actualReturn2, EPSILON);
      assertEquals("return value", expectedReturn3, actualReturn3, EPSILON);

      ArrayList<Point3d> points1 = new ArrayList<Point3d>();
      Point3d a1 = new Point3d(0.0, 0.0, 0.0);
      Point3d b1 = new Point3d(0.0, 0.0, 0.0);
      Point3d c1 = new Point3d(0.0, 0.0, 0.0);
      points1.add(a1);
      points1.add(b1);
      points1.add(c1);
      double expectedReturn11 = 0.0;
      double expectedReturn12 = 0.0;
      double expectedReturn13 = 0.0;
      Point3d actualReturn01 = GeometryTools.averagePoint3ds(points1);
      double actualReturn11 = actualReturn01.x;
      double actualReturn12 = actualReturn01.y;
      double actualReturn13 = actualReturn01.z;
      assertEquals("return value", expectedReturn11, actualReturn11, EPSILON);
      assertEquals("return value", expectedReturn12, actualReturn12, EPSILON);
      assertEquals("return value", expectedReturn13, actualReturn13, EPSILON);

   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPoints()
   {
      double[] a = {2.0, 2.0};
      double[] b = {6.0, -1.0};
      double expectedReturn = 5.0;
      double actualReturn = GeometryTools.distanceBetweenPoints(a, b);
      assertEquals("return value", expectedReturn, actualReturn, Double.MIN_VALUE);

      double[] a1 = {2.5, 5.1};
      double[] b1 = {9.3, 10.7};
      double expectedReturn1 = 8.80908621822;
      double actualReturn1 = GeometryTools.distanceBetweenPoints(a1, b1);
      assertEquals("return value", expectedReturn1, actualReturn1, EPSILON);

      double[] a2 = {5.0, 2.0};
      double[] b2 = {5.0, 2.0};
      double expectedReturn2 = 0.0;
      double actualReturn2 = GeometryTools.distanceBetweenPoints(a2, b2);
      assertEquals("return value", expectedReturn2, actualReturn2, Double.MIN_VALUE);

   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceFromPointToLine()
   {
      Point2d point = new Point2d(10, 2);
      Point2d lineStart = new Point2d(4, 2);
      Point2d lineEnd = new Point2d(10, 10);
      double expectedReturn = 4.8;
      double actualReturn = GeometryTools.distanceFromPointToLine(point, lineStart, lineEnd);
      assertEquals("return value", expectedReturn, actualReturn, Double.MIN_VALUE);

      Point2d point1 = new Point2d(10, 2);
      Point2d lineStart1 = new Point2d(10, 1);
      Point2d lineEnd1 = new Point2d(10, 10);
      double expectedReturn1 = 0.0;
      double actualReturn1 = GeometryTools.distanceFromPointToLine(point1, lineStart1, lineEnd1);
      assertEquals("return value", expectedReturn1, actualReturn1, Double.MIN_VALUE);

      Point2d point2 = new Point2d(1, 2);
      Point2d lineStart2 = new Point2d(4, 2);
      Point2d lineEnd2 = new Point2d(10, 10);
      double expectedReturn2 = 2.4;
      double actualReturn2 = GeometryTools.distanceFromPointToLine(point2, lineStart2, lineEnd2);
      assertEquals("return value", expectedReturn2, actualReturn2, EPSILON);

      Point2d point3 = new Point2d(10, 10);
      Point2d lineStart3 = new Point2d(4, 2);
      Point2d lineEnd3 = new Point2d(4, 2);
      double expectedReturn3 = 10;
      double actualReturn3 = GeometryTools.distanceFromPointToLine(point3, lineStart3, lineEnd3);
      assertEquals("return value", expectedReturn3, actualReturn3, Double.MIN_VALUE);



   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceFromPointToLine1()
   {
      Point3d point = new Point3d(10, 2, 0);
      Point3d lineStart = new Point3d(4, 2, 0);
      Point3d lineEnd = new Point3d(10, 10, 0);
      double expectedReturn = 4.8;
      double actualReturn = GeometryTools.distanceFromPointToLine(point, lineStart, lineEnd);
      assertEquals("return value", expectedReturn, actualReturn, Double.MIN_VALUE);

      Point3d point2 = new Point3d(3, 3, 0);
      Point3d lineStart2 = new Point3d(0, 0, 0);
      Point3d lineEnd2 = new Point3d(3, 3, 3);
      double expectedReturn2 = 2.44948974278;
      double actualReturn2 = GeometryTools.distanceFromPointToLine(point2, lineStart2, lineEnd2);
      assertEquals("return value", expectedReturn2, actualReturn2, EPSILON);

      Point3d point1 = new Point3d(10, 10, 0);
      Point3d lineStart1 = new Point3d(4, 2, 0);
      Point3d lineEnd1 = new Point3d(4, 2, 0);
      double expectedReturn1 = 10.0;
      double actualReturn1 = GeometryTools.distanceFromPointToLine(point1, lineStart1, lineEnd1);
      assertEquals("return value", expectedReturn1, actualReturn1, Double.MIN_VALUE);

   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceFromPointToLineSegment()
   {
      Point2d point = new Point2d(10, 2);
      Point2d lineStart = new Point2d(4, 2);
      Point2d lineEnd = new Point2d(10, 10);
      double expectedReturn = 4.8;
      double actualReturn = GeometryTools.distanceFromPointToLineSegment(point, lineStart, lineEnd);
      assertEquals("return value", expectedReturn, actualReturn, Double.MIN_VALUE);

      Point2d point1 = new Point2d(10, 10);
      Point2d lineStart1 = new Point2d(4, 2);
      Point2d lineEnd1 = new Point2d(4, 2);
      double expectedReturn1 = 10.0;
      double actualReturn1 = GeometryTools.distanceFromPointToLineSegment(point1, lineStart1, lineEnd1);
      assertEquals("return value", expectedReturn1, actualReturn1, Double.MIN_VALUE);

      Point2d point2 = new Point2d(1, 1);
      Point2d lineStart2 = new Point2d(4, 2);
      Point2d lineEnd2 = new Point2d(5, 5);
      double expectedReturn2 = 3.16227766017;
      double actualReturn2 = GeometryTools.distanceFromPointToLineSegment(point2, lineStart2, lineEnd2);
      assertEquals("return value", expectedReturn2, actualReturn2, EPSILON);

      /** @todo fill in the test code */
   }

/*
   public void testGetClosestPointsForTwoLines()
   {
      Point3d p1 = new Point3d(5, 5, 0);
      FramePoint point1 = new FramePoint(ReferenceFrame.getWorldFrame(), p1.x, p1.y, p1.z);

      FrameVector vector1 = null;
      FramePoint point2 = null;
      FrameVector vector2 = null;
      FramePoint pointOnLine1 = null;
      FramePoint pointOnLine2 = null;
      geometryTools.getClosestPointsForTwoLines(point1, vector1, point2, vector2, pointOnLine1, pointOnLine2);
   }
*/

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetDistanceBetweenPointAndPlane()
   {
      FramePoint pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      double actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      double expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -3);
      actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -3);
      actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -3);
      actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      expected = 6.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 1, 0, 0);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 0, 0);
      actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      expected = 3.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 1, 1, 1);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      point = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 3, 0);
      actual = GeometryTools.distanceFromPointToPlane(pointOnPlane, planeNormal, point);
      expected = 2.0;
      assertEquals("FAILED: Distance from point to plane", expected, actual, EPSILON);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsLineIntersectingPlane()
   {
      FramePoint pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -1);
      FramePoint lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);
      assertTrue(GeometryTools.isLineIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 1, 0, 0);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), -6, 3, -3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertTrue(GeometryTools.isLineIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 1, 0);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 6, -3, -3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertTrue(GeometryTools.isLineIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 6, -3, 3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, 6);
      assertFalse(GeometryTools.isLineIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));

      pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 6, -3, -3);
      lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 6, 3, -1);
      assertFalse(GeometryTools.isLineIntersectingPlane(pointOnPlane, planeNormal, lineStart, lineEnd));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetIntersectionBetweenLineAndPlane()
   {
      FramePoint pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FrameVector planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
      FramePoint lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 1);
      FramePoint lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 3);

//    FramePoint expectedReturn = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
      FramePoint actualReturn = GeometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineStart, lineEnd);
      assertNull(actualReturn);

      // assertTrue("FAILED: Plane intersection", expectedReturn.epsilonEquals(actualReturn, EPSILON));

//    pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 1);
//    planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
//    lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -3);
//    lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 6);
//    expectedReturn = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 1.0);
//    actualReturn = GeometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineStart, lineEnd);
//    assertTrue("FAILED: Plane intersection", expectedReturn.epsilonEquals(actualReturn, EPSILON));
//    
//    pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, -1);
//    planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
//    lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -3);
//    lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 6);
//    expectedReturn = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -1.0);
//    actualReturn = GeometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineStart, lineEnd);
//    assertTrue("FAILED: Plane intersection", expectedReturn.epsilonEquals(actualReturn, EPSILON));
//    
//    pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
//    planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
//    lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 3);
//    lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 6);
//    actualReturn = GeometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineStart, lineEnd);
//    System.out.println("Plane intersection: " + actualReturn);
//    assertNull(actualReturn);
//    
//    pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
//    planeNormal = new FrameVector(pointOnPlane.getReferenceFrame(), 0, 0, 1);
//    lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -3);
//    lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, -6);
//    actualReturn = GeometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineStart, lineEnd);
//    System.out.println("Plane intersection: " + actualReturn);
//    assertNull(actualReturn);

      // pointOnPlane = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
//    v= new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 1);
//    planeNormal = new FrameVector(pointOnPlane.getReferenceFrame());
//    planeNormal.sub(pointOnPlane, v);
//    lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 3);
//    lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 6);
//    expectedReturn = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 0);
//    actualReturn = GeometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineStart, lineEnd);
//    System.out.println(actualReturn);
//    assertTrue("FAILED: Above plane", expectedReturn.epsilonEquals(actualReturn, EPSILON));

      /*
       * FramePoint pointOnPlane1 = new FramePoint(ReferenceFrame.getWorldFrame(), 5, 5, 0);
       * FramePoint v1 = new FramePoint(ReferenceFrame.getWorldFrame(), 5, 5, 1);
       * FrameVector planeNormal1 = new FrameVector(pointOnPlane1.getReferenceFrame());
       * planeNormal1.sub(pointOnPlane1, v1);
       * FramePoint lineStart1 = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 3);
       * FramePoint lineEnd1 = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 3);
       * FramePoint expectedReturn1 = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 0);
       * FramePoint actualReturn1 = geometryTools.getIntersectionBetweenLineAndPlane(pointOnPlane1, planeNormal1, lineStart1, lineEnd1);
       * assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));
       */

   }

// @Test(timeout=300000)
// public void testIntersectionLine2dLine2d()
// {
//    Line2d line1 = new Line2d(new Point2d(-10.0, 0.0), new Point2d(10.0, 0.0));
//    Line2d line2 = new Line2d(new Point2d(-10.0, 10.0), new Point2d(10.0, 0.0));
//    Line2d line3 = new Line2d(new Point2d(0.0, 10.0), new Point2d(0.0, -10.0));
//    Line2d line4 = new Line2d(new Point2d(0.0, -10.0), new Point2d(0.0, 10.0));
//    Line2d line5 = new Line2d(new Point2d(-10.0, 0.0), new Point2d(10.0, 0.0));
//    Line2d line6 = new Line2d(new Point2d(10.0, 0.0), new Point2d(-10.0, 0.0));
//    Line2d line7 = new Line2d(new Point2d(10.0, 0.0), new Point2d(20.0, 0.0));
//    Line2d line8 = new Line2d(new Point2d(10.0, 0.0), new Point2d(-20.0, 0.0));
//    Line2d line9 = new Line2d(new Point2d(10.1, 0.0), new Point2d(20.0, 0.0));
//    Line2d line10 = new Line2d(new Point2d(10.0, 0.0), new Point2d(20.0, 1.0));
//    Line2d line11 = new Line2d(new Point2d(-10.0, 1.0), new Point2d(10.0, 1.0));
//
//    assertEquals(null, GeometryTools.intersection(line1, line11));
//
//
//    assertEquals(null, GeometryTools.intersection(line5, line1));
//    assertEquals(null, GeometryTools.intersection(line6, line1));
//    assertEquals(new Point2d(10.0, 0.0), GeometryTools.intersection(line2, line1));
//    assertEquals(new Point2d(10.0, 0.0), GeometryTools.intersection(line10, line1));
//
//    assertEquals(new Point2d(0.0, 0.0), GeometryTools.intersection(line3, line1));
//    assertEquals(new Point2d(0.0, 0.0), GeometryTools.intersection(line4, line1));
//
//
//    assertEquals(null, GeometryTools.intersection(line7, line1));
//    assertEquals(null, GeometryTools.intersection(line8));
//    assertEquals(null, GeometryTools.intersection(line9));
// }

   // What happens if to lines are the same line??????
   // Parallel lines returns something.....but not the right something

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetIntersectionBetweenTwoLines()
   {
      Point2d point1 = new Point2d(5, 1.0);
      Vector2d vector1 = new Vector2d(8, 9);
      Point2d point2 = new Point2d(5, 1.0);
      Vector2d vector2 = new Vector2d(3, 9);
      Point2d expectedReturn = new Point2d(5.0, 1.0);
      Point2d actualReturn = GeometryTools.getIntersectionBetweenTwoLines(point1, vector1, point2, vector2);
      assertEquals("return value", expectedReturn, actualReturn);


//    Point2d point11 = new Point2d(5.0, 1.0);
//    Vector2d vector11 = new Vector2d(0.0, 1.0);
//    Point2d point22 = new Point2d(6.0, 1.0);
//    Vector2d vector22 = new Vector2d(0.0, 1.0);
//    Point2d expectedReturn11 = new Point2d(5.0, 1.0);
//    Point2d actualReturn11 = GeometryTools.getIntersectionBetweenTwoLines(point11, vector11, point22, vector22);
//    assertEquals("return value", expectedReturn11, actualReturn11);
//
//
//    Point2d point = new Point2d(1, 1);
//    Vector2d vector = new Vector2d(0, 1);
//    Point2d point3 = new Point2d(1, 10);
//    Vector2d vector3 = new Vector2d(0, -1);
//    Point2d expectedReturn1 = new Point2d(5.0, 1.0);
//    Point2d actualReturn1 = GeometryTools.getIntersectionBetweenTwoLines(point, vector, point3, vector3);
//    assertEquals("return value", expectedReturn1, actualReturn1);


   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetNormalToPlane()
   {
      Point3d a = new Point3d(0, 0, 0);
      Point3d b = new Point3d(1, 0, 0);
      Point3d c = new Point3d(0, 1, 0);
      Vector3d expectedReturn = new Vector3d(0, 0, 1);
      Vector3d actualReturn = GeometryTools.getNormalToPlane(a, b, c);
      assertEquals("return value", expectedReturn, actualReturn);

      Point3d a1 = new Point3d(5, 0, 6);
      Point3d b1 = new Point3d(1, 0, 4);
      Point3d c1 = new Point3d(2, 0, 65);
      Vector3d expectedReturn1 = new Vector3d(0, 1, 0);
      Vector3d actualReturn1 = GeometryTools.getNormalToPlane(a1, b1, c1);
      assertEquals("return value", expectedReturn1, actualReturn1);

      Point3d a2 = new Point3d(0, 6, 4);
      Point3d b2 = new Point3d(0, 32, 6);
      Point3d c2 = new Point3d(0, 1, 4);
      Vector3d expectedReturn2 = new Vector3d(1, 0, 0);
      Vector3d actualReturn2 = GeometryTools.getNormalToPlane(a2, b2, c2);
      assertEquals("return value", expectedReturn2, actualReturn2);

      /*
       *    Point3d a3 = new Point3d(7, 5, 9);
       *    Point3d b3 = new Point3d(0, 1, 0);
       *    Point3d c3 = new Point3d(0, 1, 0);
       *    Vector3d expectedReturn3 = new Vector3d(0, 0, 1);
       *    Vector3d actualReturn3 = geometryTools.getNormalToPlane(a3, b3, c3);
       *    assertEquals("return value", expectedReturn3, actualReturn3);
       */
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetPerpendicularBisector()
   {
      Point2d lineStart = new Point2d(1, 1);
      Point2d lineEnd = new Point2d(5, 5);
      Point2d bisectorStart = new Point2d(2, 1);
      Vector2d bisectorDirection = new Vector2d();
      GeometryTools.getPerpendicularBisector(lineStart, lineEnd, bisectorStart, bisectorDirection);

   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetPerpendicularVector()
   {
      Vector2d vector = new Vector2d(15.0, 10.0);
      Vector2d expectedReturn = new Vector2d(-10.0, 15.0);
      Vector2d actualReturn = GeometryTools.getPerpendicularVector(vector);
      assertEquals("return value", expectedReturn, actualReturn);

      /** @todo fill in the test code */
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetPerpendicularVectorFromLineToPoint()
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

      Point2d returnPoint = GeometryTools.getClosestPointToLineSegment(new Point2d(-2.5, 1.5), new Point2d(0, 0), new Point2d(-4, 4));
      FrameVector x1 = new FrameVector(point1.getReferenceFrame());
      x1.sub(point1, intersectionPoint1);
      FrameVector expectedReturn1 = x1;
      FrameVector actualReturn1 = GeometryTools.getPerpendicularVectorFromLineToPoint(point1, lineStart1, lineEnd1, intersectionPoint1);

      assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));


      /*
       *  FramePoint point1 = new FramePoint(ReferenceFrame.getWorldFrame(), 4, 2, 0);
       *  FramePoint lineStart1 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
       *  FramePoint lineEnd1 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
       *  FramePoint intersectionPoint1 = new FramePoint(ReferenceFrame.getWorldFrame(), 3, 3, 0);
       *  FrameVector x1 = new FrameVector(point1.getReferenceFrame());
       *  x1.sub(point1, intersectionPoint1);
       *  FrameVector expectedReturn1 = x1;
       *  FrameVector actualReturn1 = geometryTools.getPerpendicularVectorFromLineToPoint(point1, lineStart1, lineEnd1, intersectionPoint1);
       *  assertTrue("Test Failed", expectedReturn1.epsilonEquals(actualReturn1, EPSILON));
       *
       * /returns zeros if point is on line
       *  FramePoint point2 = new FramePoint(ReferenceFrame.getWorldFrame(), 5, 0, 0);
       *  FramePoint lineStart2 = new FramePoint(ReferenceFrame.getWorldFrame(), 0, 0, 0);
       *  FramePoint lineEnd2 = new FramePoint(ReferenceFrame.getWorldFrame(), 10, 0, 0);
       *  FramePoint intersectionPoint2 = new FramePoint(ReferenceFrame.getWorldFrame(), 5, 0, 0);
       *  FrameVector x2 = new FrameVector(point2.getReferenceFrame());
       *  x2.sub(point2, intersectionPoint2);
       *  FrameVector expectedReturn2 = x2;
       *  FrameVector actualReturn2 = geometryTools.getPerpendicularVectorFromLineToPoint(point2, lineStart2, lineEnd2, intersectionPoint2);
       *  assertEquals("return value", expectedReturn2, actualReturn2);
       */
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
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

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetPlaneNormalGivenThreePoints1()
   {
      Point3d point1 = new Point3d(0, 0, 0);
      Point3d point2 = new Point3d(7, 0, 0);
      Point3d point3 = new Point3d(2, 0, 0);
      Vector3d expectedReturn = null;
      Vector3d actualReturn = GeometryTools.getPlaneNormalGivenThreePoints(point1, point2, point3);
      assertEquals("return value", expectedReturn, actualReturn);

      Point3d point01 = new Point3d(15, 0, 0);
      Point3d point02 = new Point3d(15, 0, 0);
      Point3d point03 = new Point3d(15, 0, 0);
      Vector3d expectedReturn1 = null;
      Vector3d actualReturn1 = GeometryTools.getPlaneNormalGivenThreePoints(point01, point02, point03);
      assertEquals("return value", expectedReturn1, actualReturn1);

      Point3d point11 = new Point3d(0, 4, 0);
      Point3d point12 = new Point3d(0, 2, 0);
      Point3d point13 = new Point3d(0, 67, 0);
      Vector3d expectedReturn2 = null;
      Vector3d actualReturn2 = GeometryTools.getPlaneNormalGivenThreePoints(point11, point12, point13);
      assertEquals("return value", expectedReturn2, actualReturn2);

      Point3d point21 = new Point3d(0, 0, 4);
      Point3d point22 = new Point3d(0, 0, 7);
      Point3d point23 = new Point3d(0, 0, 5);
      Vector3d expectedReturn3 = null;
      Vector3d actualReturn3 = GeometryTools.getPlaneNormalGivenThreePoints(point21, point22, point23);
      assertEquals("return value", expectedReturn3, actualReturn3);

      Point3d point31 = new Point3d(0, 67, 5);
      Point3d point32 = new Point3d(0, 3, 7);
      Point3d point33 = new Point3d(0, 90, 7.24264068712);
      Vector3d expectedReturn4 = new Vector3d(-1, 0, 0);
      Vector3d actualReturn4 = GeometryTools.getPlaneNormalGivenThreePoints(point31, point32, point33);
      assertEquals("return value", expectedReturn4, actualReturn4);

      Point3d point41 = new Point3d(45, 0, 5);
      Point3d point42 = new Point3d(35, 0, 7);
      Point3d point43 = new Point3d(132, 0, 7.24264068712);
      Vector3d expectedReturn5 = new Vector3d(0, 1, 0);
      Vector3d actualReturn5 = GeometryTools.getPlaneNormalGivenThreePoints(point41, point42, point43);
      assertTrue("Test Failed", expectedReturn5.epsilonEquals(actualReturn5, EPSILON));

      Point3d point51 = new Point3d(45, 67, 0);
      Point3d point52 = new Point3d(35, 56, 0);
      Point3d point53 = new Point3d(132, -4, 0);
      Vector3d expectedReturn6 = new Vector3d(0, 0, 1);
      Vector3d actualReturn6 = GeometryTools.getPlaneNormalGivenThreePoints(point51, point52, point53);
      assertTrue("Test Failed", expectedReturn6.epsilonEquals(actualReturn6, EPSILON));

      Point3d point61 = new Point3d(1, 5, 7);
      Point3d point62 = new Point3d(1, 5, 7);
      Point3d point63 = new Point3d(5, 12, 4325);
      Vector3d expectedReturn7 = null;
      Vector3d actualReturn7 = GeometryTools.getPlaneNormalGivenThreePoints(point61, point62, point63);
      assertEquals("return value", expectedReturn7, actualReturn7);


   }

/*
   public void testGetTransform()
   {
      FramePoint point = null;
      FrameVector normal = null;
      Orientation expectedReturn = null;
      Orientation actualReturn = geometryTools.getTransform(point, normal);
      assertEquals("return value", expectedReturn, actualReturn);
   }

   public void testGetVerticalSpansOfPoints()
   {
      double xMin = 0.0;
      double yMin = 0.0;
      double zMin = 0.0;
      double xMax = 0.0;
      double yMax = 0.0;
      double zMax = 0.0;
      double xResolution = 0.0;
      double yResolution = 0.0;
      double zResolution = 0.0;
      ArrayList expectedReturn = null;
      ArrayList actualReturn = geometryTools.getVerticalSpansOfPoints(xMin, yMin, zMin, xMax, yMax, zMax, xResolution, yResolution, zResolution);
      assertEquals("return value", expectedReturn, actualReturn);
   }
*/
	
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test (timeout = 30000)
   public void testGetTopVertexOfIsoscelesTriangle()
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
         
         assertTrue("TopVertexAngle = " + Math.toDegrees(topVertexAngle) + " degrees", GeometryTools.isFormingTriangle(baseVertexA.distance(baseVertexC), baseVertexA.distance(topVertexB), topVertexB.distance(baseVertexC)));

         GeometryTools.getTopVertexOfIsoscelesTriangle(baseVertexA, baseVertexC, trianglePlaneNormal, topVertexAngle, topVertexBComputed);

         String errorMsg = "Computed vertex: " + topVertexBComputed + "\n does not match actual vertex: " + topVertexB + "\n when topVertex Angle = " + Math.toDegrees(topVertexAngle) + " degrees \n";
         assertEquals(errorMsg, 0.0 , topVertexB.distance(topVertexBComputed), 1e-9);

         topVertexAngle += Math.toRadians(1.0);
      }
   }
   
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test (timeout = 30000)
   public void testGetPerpendicularToLine()
   {
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();

      FramePoint lineStart = new FramePoint(frame);
      FramePoint lineEnd = new FramePoint(frame);
      
      FrameVector lineStartToEnd = new FrameVector(frame);
      FrameVector planeNormal = new FrameVector(frame);
      FramePoint bisectorEnd = new FramePoint(frame);

      FrameVector bisectorComputed = new FrameVector(frame);

      double bisectorLengthDesired = 1.5;

      planeNormal.setIncludingFrame(frame, 0.0, 0.0, 1.0);
      lineStart.setIncludingFrame(frame, 0.0, -1.0, 0.0);
      lineEnd.setIncludingFrame(frame, 0.0, 1.0, 0.0);
      lineStartToEnd.sub(lineEnd, lineStart);
      bisectorEnd.setIncludingFrame(frame, -bisectorLengthDesired, 0.0, 0.0);
      
      GeometryTools.getPerpendicularToLine(lineStartToEnd, planeNormal, bisectorLengthDesired, bisectorComputed);
      FramePoint bisectorEndComputed = FramePoint.getMidPoint(lineStart, lineEnd);
      bisectorEndComputed.add(bisectorComputed);
      
      String errorMsg = "Computed bisector endpoint: " + bisectorEndComputed + "\n does not match actual bisector endpoint: " + bisectorEnd;
      assertEquals(errorMsg, 0.0, bisectorEnd.distance(bisectorEndComputed), 1e-9);
   }
	
	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetZPlanePerpendicularBisector()
   {
      FramePoint lineStart = new FramePoint(ReferenceFrame.getWorldFrame(), 0.0, 0.0, 11.5);
      FramePoint lineEnd = new FramePoint(ReferenceFrame.getWorldFrame(), -3.0, 3.0, -89.6);

      FramePoint mid = new FramePoint(lineEnd.getReferenceFrame());
      FrameVector direction = new FrameVector(lineEnd.getReferenceFrame());

      GeometryTools.getZPlanePerpendicularBisector(lineStart, lineEnd, mid, direction);

      // assertEquals("return value", expectedReturn, actualReturn);

   }

	@DeployableTestMethod(estimatedDuration = 0.0)
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

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointOnLeftSideOfLine1()
   {
      Point2d point = new Point2d(3, 9);
      Point2d lineStart = new Point2d(-5, 8);
      Point2d lineEnd = new Point2d(10, 7);
      boolean expectedReturn = true;
      boolean actualReturn = GeometryTools.isPointOnLeftSideOfLine(point, lineStart, lineEnd);
      assertEquals("return value", expectedReturn, actualReturn);

      Point2d point2 = new Point2d(1, 5);
      Point2d lineStart2 = new Point2d(-5, 8);
      Point2d lineEnd2 = new Point2d(10, 7);
      boolean expectedReturn2 = false;
      boolean actualReturn2 = GeometryTools.isPointOnLeftSideOfLine(point2, lineStart2, lineEnd2);
      assertEquals("return value", expectedReturn2, actualReturn2);

      Point2d point3 = new Point2d(1, 1);
      Point2d lineStart3 = new Point2d(0, 0);
      Point2d lineEnd3 = new Point2d(10, 10);
      boolean expectedReturn3 = false;
      boolean actualReturn3 = GeometryTools.isPointOnLeftSideOfLine(point3, lineStart3, lineEnd3);
      assertEquals("return value", expectedReturn3, actualReturn3);

      Point2d point4 = new Point2d(3, 9);
      Point2d lineStart4 = new Point2d(10, 7);
      Point2d lineEnd4 = new Point2d(-5, 8);
      boolean expectedReturn4 = false;
      boolean actualReturn4 = GeometryTools.isPointOnLeftSideOfLine(point4, lineStart4, lineEnd4);
      assertEquals("return value", expectedReturn4, actualReturn4);


   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testAngleByLawOfCosineWithNegativeSideLengthA()
   {
      double a = 1.0;
      GeometryTools.getUnknownTriangleAngleByLawOfCosine(-a, a, a);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testAngleByLawOfCosineWithNegativeSideLengthB()
   {
      double a = 1.0;
      GeometryTools.getUnknownTriangleAngleByLawOfCosine(a, -a, a);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testAngleByLawOfCosineWithNegativeSideLengthC()
   {
      double a = 1.0;
      GeometryTools.getUnknownTriangleAngleByLawOfCosine(a, a, -a);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAngleByLawOfCosineWithEqualLengthTriangle()
   {
      double a = 1.0;
      double alpha = GeometryTools.getUnknownTriangleAngleByLawOfCosine(a, a, a);
      double expected_alpha = Math.PI / 3.0;
      assertEquals(expected_alpha, alpha, 1e-10);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSideLengthByLawOfCosineNegativeSideLengthA()
   {
      double a = 1.0;
      double gamma = Math.PI / 3.0;
      GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(-a, a, gamma);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSideLengthByLawOfCosineNegativeSideLengthB()
   {
      double a = 1.0;
      double gamma = Math.PI / 3.0;
      GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, -a, gamma);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSideLengthByLawOfCosineGreaterThanPiAngle()
   {
      double a = 1.0;
      double gamma = Math.PI / 2.0 * 3.0;
      GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, a, gamma);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSideLengthByLawOfCosineWithEqualLengthTriangle()
   {
      double a = 1.0;
      double gamma = Math.PI / 3.0;
      double c = GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, a, gamma);
      assertEquals(a, c, 1e-10);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSideLengthByLawOfCosineWithEqualLengthTriangleNegativeAngle()
   {
      double a = 1.0;
      double gamma = -Math.PI / 3.0;
      double c = GeometryTools.getUnknownTriangleSideLengthByLawOfCosine(a, a, gamma);
      assertEquals(a, c, 1e-10);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void isFormingTriangleFailTest()
   {
      double a = 1.0;
      double b = 10.0;
      boolean actual = GeometryTools.isFormingTriangle(b, a, a);
      assertEquals(false, actual);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void isFormingTriangleSameSidedTest()
   {
      double a = 1.0;
      boolean actual = GeometryTools.isFormingTriangle(a, a, a);
      assertEquals(true, actual);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void illegalPythagorasGetCathetus()
   {
      GeometryTools.pythagorasGetCathetus(1.0, 2.0);
   }

   private static final boolean VERBOSE = false;

   private Random random = new Random(1176L);

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceLineSegment2dLineSegment2d()
   {
      // not yet implemented
//    for (int i = 0; i < 1000; i++)
//    {
//       Point2d seg1p1 = new Point2d(random.nextDouble() * 1000 - 500, random.nextDouble() * 1000 - 500);
//       Point2d seg1p2 = new Point2d(random.nextDouble() * 1000 - 500, random.nextDouble() * 1000 - 500);
//       Point2d seg2p1 = new Point2d(random.nextDouble() * 1000 - 500, random.nextDouble() * 1000 - 500);
//       Point2d seg2p2 = new Point2d(random.nextDouble() * 1000 - 500, random.nextDouble() * 1000 - 500);
//
//       LineSegment2d seg1 = new LineSegment2d(seg1p1, seg1p2);
//       LineSegment2d seg2 = new LineSegment2d(seg2p1, seg2p2);
//
//       double returnedDistance = Geometry2dCalculator.distance(seg1, seg2);
//       if (seg1.intersectionWith(seg2) != null)
//          assertEquals("segmentes: " + seg1 + " " + seg2 + " intersect but distance is greater than 0.0: " + returnedDistance, 0.0, returnedDistance,
//                       0.000001);
//       else
//       {
//          double shortestDistance = Double.MAX_VALUE;
//          double distance = seg1.distance(seg2p1);
//          if (distance < shortestDistance)
//             shortestDistance = distance;
//
//
//
//       }
//    }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testCombine()
   {
      ArrayList<Point2d> firstList = new ArrayList<Point2d>();
      for (int i = 0; i < 100; i++)
      {
         firstList.add(new Point2d(random.nextDouble(), random.nextDouble()));
      }

      ConvexPolygon2d firstPolygon = new ConvexPolygon2d(firstList);

      ArrayList<Point2d> secondList = new ArrayList<Point2d>();
      for (int i = 0; i < 200; i++)
      {
         secondList.add(new Point2d(random.nextDouble(), random.nextDouble()));
      }

      ConvexPolygon2d secondPolygon = new ConvexPolygon2d(secondList);

      ConvexPolygon2d result = new ConvexPolygon2d(firstPolygon, secondPolygon);

      // convexity of the result is already checked in another test
      for (Point2d point : firstList)
      {
         if (!result.isPointInside(point))
         {
            double distance = result.distance(point);

            if (distance > 1e-7)
               throw new RuntimeException("Not each point is inside the result. distance = " + distance);
         }

//       assertTrue("Not each point isinside the result. distance = " , result.isPointInside(point));
      }

      for (Point2d point : secondList)
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

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testMovePointInsidePolygonAlongVector()
   {
      int nTests = 1000;
      int testNumber = 0;
      ReferenceFrame frame = ReferenceFrame.getWorldFrame();

      int minPoints = 3;
      int maxPoints = 10;
      while (testNumber < nTests)
      {
         ArrayList<FramePoint2d> points = new ArrayList<FramePoint2d>();
         int nPoints = minPoints + random.nextInt(maxPoints - minPoints);
         for (int i = 0; i < nPoints; i++)
         {
            points.add(new FramePoint2d(frame, random.nextDouble() - 0.5, random.nextDouble() - 0.5));
         }

         FrameConvexPolygon2d polygon = new FrameConvexPolygon2d(points);

         FramePoint2d pointToMove = new FramePoint2d(frame, 2.0 * (random.nextDouble() - 0.5), 2.0 * (random.nextDouble() - 0.5));

         FramePoint2d pointInside = new FramePoint2d(frame, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
         if (polygon.isPointInside(pointInside))
         {
            // do test
            FrameVector2d vector = new FrameVector2d(pointToMove, pointInside);
            FrameLine2d line = new FrameLine2d(pointInside, vector);
            FramePoint2d[] intersections = polygon.intersectionWith(line);
            double distanceBetweenIntersections = intersections[0].distance(intersections[1]);

            double scaling = 0.6;    // larger than 0.5 means that distanceToBeInside could be infeasible
            double distanceToBeInside = distanceBetweenIntersections * random.nextDouble() * scaling;
            boolean feasible = distanceToBeInside < distanceBetweenIntersections / 2.0;

            GeometryTools.movePointInsidePolygonAlongVector(pointToMove, vector, polygon, distanceToBeInside);

            if (feasible)
            {
               for (int i = 0; i < intersections.length; i++)
               {
                  double distanceToIntersection = pointToMove.distance(intersections[i]);
                  assertTrue(distanceToIntersection >= distanceToBeInside - 1e-12);
               }

               assertTrue(polygon.isPointInside(pointToMove, 1e-12));
               assertTrue(line.containsEpsilon(pointToMove, 1e-12));
            }
            else
            {
               assertTrue(polygon.isPointInside(pointToMove, 1e-12));
               assertTrue(line.containsEpsilon(pointToMove, 1e-12));
               FrameLineSegment2d intersectionsLineSegment = new FrameLineSegment2d(intersections[0], intersections[1]);
               assertEquals(0.5, intersectionsLineSegment.percentageAlongLineSegment(pointToMove), 1e-12);
            }


            testNumber++;
         }

      }

   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsNegativeAngle()
   {
      assertPolygons(new double[]
      {
         0, 5, 2, -2, 2, 0
      }, new double[]
      {
         2.5, 1, 2.8, 1, 3, .9, 4, 0, 3, -1
      }, new double[] {2, 0, 46.0 / 17, 6.0 / 34}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsThirdQuadrant()
   {
      assertPolygons(new double[]
      {
         -2, -1, -1, -1, -1, -2
      }, new double[]
      {
         -2, -2, -2, -3, -4, -4, -4, -2
      }, new double[] {-1.5, -1.5, -2, -2}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsNegativeAngleAndTwoVisibleVerticesOnPolygon1()
   {
      assertPolygons(new double[]
      {
         0, 0, 1, 2, 1, 0
      }, new double[]
      {
         2, 2, 0, 3, -1, 4
      }, new double[] {1, 2, 1.2, 2.4}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsParalellEdges()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         0, 3, 2, 3, -1, 4, 3, 4
      }, new double[] {1, 2, 1, 3}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsMultiplePossibleAnswers()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         3, 2, 2, 3, 2, 4, 4, 2
      }, new double[] {1, 2, 2, 3}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsTwoVisiblePoints()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         4, 1, 1, 4, 2, 4, 4, 2
      }, new double[] {2, 1, 3, 2}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsTwoVisiblePoints2()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         4, 1, 1.5, 4, 2, 4, 4, 2
      }, new double[] {2, 1, 194.0 / 61, 121.0 / 61}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsOneOfTheAnglesIsZero()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         0, 2, 0, 3, 1, 3, .8, 2
      }, new double[] {.9, 1.9, .8, 2}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsTriangles()
   {
      assertPolygons(new double[]
      {
         0, 1, 1, 0, 2, 0
      }, new double[]
      {
         0, 3, 4, 3, 1, 2
      }, new double[] {.4, .8, 1, 2}, .001);
   }

// @Test(timeout=300000)
   public void testDistanceBetweenPolygonsSharedPoint()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         0, 2, 0, 3, 1, 3, 1, 2
      }, new double[] {1, 2, 1, 2}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsPointOnEdge()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         0, 2, 0, 3, 1, 3, .5, 1.5
      }, new double[] {.5, 1.5, .5, 1.5}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsNegativeAngle2()
   {
      assertPolygons(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      }, new double[]
      {
         0, 2, 0, 3, 1, 3, .4, 1.5
      }, new double[] {.45, 1.45, .4, 1.5}, .001);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsSolutionIsTwoVertices()
   {
      assertPolygons(new double[]
      {
         0, 0, 2, 0, 2, 2
      }, new double[]
      {
         4, 3, 6, 3, 6, 7
      }, new double[] {2, 2, 4, 3}, 0);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistanceBetweenPolygonsIntersectingPolygons()
   {
      ConvexPolygon2d polygon1 = getPolygon(new double[]
      {
         0, 0, 0, 1, 1, 0, 2, 1, 1, 2
      });
      ConvexPolygon2d polygon2 = getPolygon(new double[]
      {
         1, 1, 0, 3, 2, 2, 3, 0
      });

      try
      {
         GeometryTools.computeMinimumDistancePoints(polygon1, polygon2);
         fail();
      }

      catch (RuntimeException re)
      {
         assertEquals(re.getMessage(), "Cannot compute minimum distance between intersecting polygons.");
      }

      try
      {
         GeometryTools.computeMinimumDistancePoints(polygon2, polygon1);
         fail();
      }

      catch (RuntimeException re)
      {
         assertEquals(re.getMessage(), "Cannot compute minimum distance between intersecting polygons.");
      }
   }

   private void assertPolygons(double[] p1, double[] p2, double[] expectedSolution, double epsilon)
   {
      if (expectedSolution.length != 4)
      {
         throw new RuntimeException("Invalid input.");
      }

      ConvexPolygon2d polygon1 = getPolygon(p1);
      ConvexPolygon2d polygon2 = getPolygon(p2);
      Point2d[] closestPoints = GeometryTools.computeMinimumDistancePoints(polygon1, polygon2);
      Point2d[] closestPointsReversed = GeometryTools.computeMinimumDistancePoints(polygon2, polygon1);
      assertEquals(closestPoints[0].distance(closestPoints[1]), closestPointsReversed[0].distance(closestPointsReversed[1]), epsilon);
      assertEquals(expectedSolution[0], closestPoints[0].x, epsilon);
      assertEquals(expectedSolution[1], closestPoints[0].y, epsilon);
      assertEquals(expectedSolution[2], closestPoints[1].x, epsilon);
      assertEquals(expectedSolution[3], closestPoints[1].y, epsilon);
   }

   private ConvexPolygon2d getPolygon(double[] polygon)
   {
      if (polygon.length % 2 != 0)
      {
         throw new RuntimeException("Invalid input.");
      }

      List<Point2d> list = new ArrayList<Point2d>();
      for (int i = 0; i < polygon.length; i += 2)
      {
         list.add(new Point2d(polygon[i], polygon[i + 1]));
      }

      return new ConvexPolygon2d(list);
   }

}
