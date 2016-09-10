package us.ihmc.robotics.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.junit.Test;

import us.ihmc.tools.testing.MutationTestingTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;

@DeployableTestClass(targets = {TestPlanTarget.Fast})
public class ConvexPolygon2dCalculatorTest
{
   private static final double epsilon = 1.0e-10;

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testConstruction()
   {
      new ConvexPolygon2dCalculator();
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetSignedDistance1()
   {
      // single point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.update();

      Point2d point = new Point2d(2.5, 1.0);
      double distance = ConvexPolygon2dCalculator.getSignedDistance(point, polygon);
      assertDistanceCorrect(-Math.sqrt(2.5 * 2.5 + 1.0 * 1.0), distance);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetSignedDistance2()
   {
      // line polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.update();

      Point2d point1 = new Point2d(2.5, 1.0);
      double distance1 = ConvexPolygon2dCalculator.getSignedDistance(point1, polygon);
      assertDistanceCorrect(-Math.sqrt(1.5 * 1.5 + 1.0 * 1.0), distance1);

      Point2d point2 = new Point2d(0.5, 1.0);
      double distance2 = ConvexPolygon2dCalculator.getSignedDistance(point2, polygon);
      assertDistanceCorrect(-1.0, distance2);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetSignedDistance3()
   {
      // triangle polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(10.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 10.0));
      polygon.update();

      Point2d point1 = new Point2d(10.0, 10.0);
      double distance1 = ConvexPolygon2dCalculator.getSignedDistance(point1, polygon);
      assertDistanceCorrect(-5.0 * Math.sqrt(2.0), distance1);

      Point2d point2 = new Point2d(1.2, 1.1);
      double distance2 = ConvexPolygon2dCalculator.getSignedDistance(point2, polygon);
      assertDistanceCorrect(1.1, distance2);

      Point2d point3 = new Point2d(0.05, 9.8);
      double distance3 = ConvexPolygon2dCalculator.getSignedDistance(point3, polygon);
      assertDistanceCorrect(0.05, distance3);

      Point2d point4 = new Point2d(9.8, 0.15);
      double distance4 = ConvexPolygon2dCalculator.getSignedDistance(point4, polygon);
      assertDistanceCorrect(0.5 * Math.sqrt(0.05 * 0.05 * 2.0), distance4);

      Point2d point5 = new Point2d(5.0, -0.15);
      double distance5 = ConvexPolygon2dCalculator.getSignedDistance(point5, polygon);
      assertDistanceCorrect(-0.15, distance5);

      Point2d point6 = new Point2d(15.0, -0.15);
      double distance6 = ConvexPolygon2dCalculator.getSignedDistance(point6, polygon);
      assertDistanceCorrect(-Math.sqrt(5.0 * 5.0 + 0.15 * 0.15), distance6);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetClosestVertexPoint1()
   {
      Point2d vertex1 = new Point2d(0.0, 0.0);
      Point2d vertex2 = new Point2d(10.0, 0.0);
      Point2d vertex3 = new Point2d(0.0, 10.0);

      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(vertex1);
      polygon.addVertex(vertex2);
      polygon.addVertex(vertex3);
      polygon.update();

      Point2d point1 = new Point2d(-1.0, -1.0);
      assertPointsEqual(vertex1, ConvexPolygon2dCalculator.getClosestVertexCopy(point1, polygon));

      Point2d point2 = new Point2d(1.0, 1.0);
      assertPointsEqual(vertex1, ConvexPolygon2dCalculator.getClosestVertexCopy(point2, polygon));

      Point2d point3 = new Point2d(10.0, 0.0);
      assertPointsEqual(vertex2, ConvexPolygon2dCalculator.getClosestVertexCopy(point3, polygon));

      Point2d point4 = new Point2d(9.8, 0.0);
      assertPointsEqual(vertex2, ConvexPolygon2dCalculator.getClosestVertexCopy(point4, polygon));

      Point2d point5 = new Point2d(10.0, 11.0);
      assertPointsEqual(vertex3, ConvexPolygon2dCalculator.getClosestVertexCopy(point5, polygon));

      Point2d point6 = new Point2d(-3.0, 8.0);
      assertPointsEqual(vertex3, ConvexPolygon2dCalculator.getClosestVertexCopy(point6, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetClosestVertexPoint2()
   {
      // make sure the method fails as expected with an empty polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      Point2d closestVertex = new Point2d();

      assertFalse(ConvexPolygon2dCalculator.getClosestVertex(new Point2d(), polygon, closestVertex));
      assertTrue(ConvexPolygon2dCalculator.getClosestVertexCopy(new Point2d(), polygon) == null);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetClosestVertexLine1()
   {
      Point2d vertex1 = new Point2d(0.0, 0.0);
      Point2d vertex2 = new Point2d(10.0, 0.0);
      Point2d vertex3 = new Point2d(0.0, 10.0);

      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(vertex1);
      polygon.addVertex(vertex2);
      polygon.addVertex(vertex3);
      polygon.update();

      Line2d line1 = new Line2d(new Point2d(-1.0, 1.0), new Point2d(1.0, -1.0));
      assertPointsEqual(vertex1, ConvexPolygon2dCalculator.getClosestVertexCopy(line1, polygon));

      Line2d line2 = new Line2d(new Point2d(9.0, 0.0), new Point2d(0.0, 1.0));
      assertPointsEqual(vertex2, ConvexPolygon2dCalculator.getClosestVertexCopy(line2, polygon));

      Line2d line3 = new Line2d(new Point2d(11.0, 0.0), new Point2d(0.0, 12.0));
      assertPointsEqual(vertex2, ConvexPolygon2dCalculator.getClosestVertexCopy(line3, polygon));

      Line2d line4 = new Line2d(new Point2d(12.0, 0.0), new Point2d(0.0, 11.0));
      assertPointsEqual(vertex3, ConvexPolygon2dCalculator.getClosestVertexCopy(line4, polygon));

      Line2d line5 = new Line2d(new Point2d(-1.0, 13.0), new Point2d(1.0, 14.0));
      assertPointsEqual(vertex3, ConvexPolygon2dCalculator.getClosestVertexCopy(line5, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetClosestVertexLine2()
   {
      // make sure the method fails as expected with an empty polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      Point2d closestVertex = new Point2d();

      assertFalse(ConvexPolygon2dCalculator.getClosestVertex(new Line2d(), polygon, closestVertex));
      assertTrue(ConvexPolygon2dCalculator.getClosestVertexCopy(new Line2d(), polygon) == null);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInside1()
   {
      // single point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d point1 = new Point2d(1.0, 1.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point1, epsilon, polygon));

      Point2d point2 = new Point2d(0.8, 0.9);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point2, polygon));

      Point2d point3 = new Point2d(0.8, 1.1);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point3, 0.3, polygon));

      Point2d point4 = new Point2d(1.0, 0.9);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point4, polygon));

      Point2d point5 = new Point2d(2.0, 1.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point5, polygon));
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point5, 1.0, polygon));

      Point2d point6 = new Point2d(1.0, 2.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point6, polygon));
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point6, 1.0, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInside2()
   {
      // line polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.update();

      Point2d point1 = new Point2d(0.1, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point1, epsilon, polygon));

      Point2d point2 = new Point2d(0.1, 0.1);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point2, epsilon, polygon));

      Point2d point3 = new Point2d(1.5, 0.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point3, epsilon, polygon));

      Point2d point4 = new Point2d(1.0, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point4.x, point4.y, polygon));

      Point2d point5 = new Point2d(1.0, epsilon * 0.1);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point5.x, point5.y, polygon));

      Point2d point6 = new Point2d(1.0, epsilon * 0.1);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point6, epsilon, polygon));

      Point2d point7 = new Point2d(1.5, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point7, 0.5, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInside3()
   {
      // triangle polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(5.0, 0.0));
      polygon.addVertex(new Point2d(3.0, 5.0));
      polygon.update();

      Point2d point1 = new Point2d(0.3, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point1, epsilon, polygon));

      Point2d point2 = new Point2d(0.0, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point2, epsilon, polygon));

      Point2d point3 = new Point2d(2.0, 2.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point3, polygon));

      Point2d point4 = new Point2d(1.0, 0.3);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point4, epsilon, polygon));

      Point2d point5 = new Point2d(-1.0, 4.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point5.x, point5.y, epsilon, polygon));

      Point2d point6 = new Point2d(6.0, 7.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point6, epsilon, polygon));

      Point2d point7 = new Point2d(10.0, 0.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point7, epsilon, polygon));

      Point2d point8 = new Point2d(0.1, 0.2);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point8, polygon));

      Point2d point9 = new Point2d(3.5, 4.9);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point9.x, point9.y, epsilon, polygon));

      Point2d point10 = new Point2d(3.5, -1.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point10, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInside4()
   {
      // empty polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();

      Point2d point1 = new Point2d(10.0, 0.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point1, epsilon, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInside5()
   {
      // foot polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(-0.06, -0.08));
      polygon.addVertex(new Point2d(0.14, -0.08));
      polygon.addVertex(new Point2d(0.14, -0.19));
      polygon.addVertex(new Point2d(-0.06, -0.19));
      polygon.update();

      Point2d point1 = new Point2d(0.03, 0.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInside(point1, 0.02, polygon));

      Point2d point2 = new Point2d(0.03, -0.09);
      assertTrue(ConvexPolygon2dCalculator.isPointInside(point2, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInBoundingBox1()
   {
      // single point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d point1 = new Point2d(1.0, 1.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point1, epsilon, polygon));

      Point2d point2 = new Point2d(0.8, 0.9);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point2, polygon));

      Point2d point3 = new Point2d(0.8, 1.1);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point3, 0.3, polygon));

      Point2d point4 = new Point2d(1.0, 0.9);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point4, polygon));

      Point2d point5 = new Point2d(2.0, 1.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point5, polygon));
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point5, 1.0, polygon));

      Point2d point6 = new Point2d(1.0, 2.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point6, polygon));
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point6, 1.0, polygon));

      Point2d point7 = new Point2d(1.0 + epsilon, 1.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point7, epsilon, polygon));

      Point2d point8 = new Point2d(1.0 - epsilon, 1.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point8, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInBoundingBox2()
   {
      // line polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d point1 = new Point2d(1.0, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point1, epsilon, polygon));

      Point2d point2 = new Point2d(0.0, 1.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point2, epsilon, polygon));

      Point2d point3 = new Point2d(0.0, 0.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point3, -epsilon, polygon));

      Point2d point4 = new Point2d(0.5, 0.5);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point4.x, point4.y, polygon));

      Point2d point5 = new Point2d(1.0, -epsilon * 0.1);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point5.x, point5.y, polygon));

      Point2d point6 = new Point2d(0.0, -epsilon * 0.1);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point6, epsilon, polygon));

      Point2d point7 = new Point2d(0.7, -epsilon * 2.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point7, 2.0 * epsilon, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPointInBoundingBox3()
   {
      // triangle polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(5.0, 0.0));
      polygon.addVertex(new Point2d(3.0, 5.0));
      polygon.update();

      Point2d point1 = new Point2d(0.3, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point1, epsilon, polygon));

      Point2d point2 = new Point2d(0.0, 0.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point2, epsilon, polygon));

      Point2d point3 = new Point2d(2.0, 2.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point3, polygon));

      Point2d point4 = new Point2d(1.0, 0.3);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point4, epsilon, polygon));

      Point2d point5 = new Point2d(-1.0, 4.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point5.x, point5.y, epsilon, polygon));

      Point2d point6 = new Point2d(6.0, 7.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point6, epsilon, polygon));

      Point2d point7 = new Point2d(10.0, 0.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point7, epsilon, polygon));

      Point2d point8 = new Point2d(0.1, 0.2);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point8, polygon));

      Point2d point9 = new Point2d(3.5, 4.9);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point9.x, point9.y, epsilon, polygon));

      Point2d point10 = new Point2d(3.5, -1.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point10, polygon));

      Point2d point11 = new Point2d(-0.1, 1.0);
      assertFalse(ConvexPolygon2dCalculator.isPointInBoundingBox(point11, polygon));

      Point2d point12 = new Point2d(0.0, 1.0);
      assertTrue(ConvexPolygon2dCalculator.isPointInBoundingBox(point12, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIsPolygonInside1()
   {
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(2.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 2.0));
      polygon.update();

      ConvexPolygon2d polygonToTest1 = new ConvexPolygon2d();
      polygonToTest1.addVertex(new Point2d(0.1, 0.1));
      polygonToTest1.addVertex(new Point2d(0.2, 0.2));
      polygonToTest1.update();
      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest1, polygon));

      ConvexPolygon2d polygonToTest2 = new ConvexPolygon2d(polygon);
      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest2, polygon));
      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest2, epsilon, polygon));
      assertFalse(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest2, -epsilon, polygon));

      ConvexPolygon2d polygonToTest3 = new ConvexPolygon2d();
      polygonToTest3.addVertex(new Point2d(0.3, 0.9));
      polygonToTest3.addVertex(new Point2d(0.1, 0.1));
      polygonToTest3.addVertex(new Point2d(1.0, 1.2));
      polygonToTest3.update();
      assertFalse(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest3, polygon));

      ConvexPolygon2d polygonToTest4 = new ConvexPolygon2d();
      assertTrue(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest4, polygon));

      ConvexPolygon2d polygonToTest5 = new ConvexPolygon2d();
      polygonToTest5.addVertex(new Point2d(-0.1, 0.1));
      polygonToTest5.update();
      assertFalse(ConvexPolygon2dCalculator.isPolygonInside(polygonToTest5, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testTranslatePolygon1()
   {
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(10.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 10.0));
      polygon.update();

      Vector2d translation1 = new Vector2d(0.0, 0.0);
      ConvexPolygon2d polygon1 = ConvexPolygon2dCalculator.translatePolygonCopy(translation1, polygon);
      assertTrue(polygon1.epsilonEquals(polygon, epsilon));

      Vector2d translation2 = new Vector2d(1.0, 0.5);
      ConvexPolygon2d polygon2 = ConvexPolygon2dCalculator.translatePolygonCopy(translation2, polygon);
      assertTrue(polygon2.getVertex(2).epsilonEquals(new Point2d(1.0, 0.5), epsilon));
      assertTrue(polygon2.getVertex(1).epsilonEquals(new Point2d(11.0, 0.5), epsilon));
      assertTrue(polygon2.getVertex(0).epsilonEquals(new Point2d(1.0, 10.5), epsilon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testTranslatePolygon2()
   {
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.update();

      Vector2d translation1 = new Vector2d(-0.1, 0.0);
      ConvexPolygon2dCalculator.translatePolygon(translation1, polygon);
      assertTrue(polygon.getVertex(0).epsilonEquals(translation1, epsilon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testCanObserverSeeEdge1()
   {
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      // observer inside polygon can not see any outside edges
      Point2d observer1 = new Point2d(0.5, 0.5);
      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
         assertFalse(ConvexPolygon2dCalculator.canObserverSeeEdge(i, observer1, polygon));

      // this observer should be able to see the edge starting at vertex (0.0, 0.0)
      Point2d observer2 = new Point2d(-0.5, 0.5);
      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
      {
         if (polygon.getVertex(i).epsilonEquals(new Point2d(0.0, 0.0), epsilon))
            assertTrue(ConvexPolygon2dCalculator.canObserverSeeEdge(i, observer2, polygon));
         else
            assertFalse(ConvexPolygon2dCalculator.canObserverSeeEdge(i, observer2, polygon));
      }

      // this observer should be able to see the edges starting at vertex (0.0, 1.0) and at (1.0, 1.0)
      Point2d observer3 = new Point2d(1.5, 1.5);
      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
      {
         if (polygon.getVertex(i).epsilonEquals(new Point2d(0.0, 1.0), epsilon))
            assertTrue(ConvexPolygon2dCalculator.canObserverSeeEdge(i, observer3, polygon));
         else if (polygon.getVertex(i).epsilonEquals(new Point2d(1.0, 1.0), epsilon))
            assertTrue(ConvexPolygon2dCalculator.canObserverSeeEdge(i, observer3, polygon));
         else
            assertFalse(ConvexPolygon2dCalculator.canObserverSeeEdge(i, observer3, polygon));
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testCanObserverSeeEdge2()
   {
      // line polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.update();

      // should be able to see one edge
      Point2d observer1 = new Point2d(0.0, 0.0);
      boolean seeEdge1 = ConvexPolygon2dCalculator.canObserverSeeEdge(0, observer1, polygon);
      boolean seeEdge2 = ConvexPolygon2dCalculator.canObserverSeeEdge(1, observer1, polygon);
      assertTrue((seeEdge1 || seeEdge2) && !(seeEdge1 && seeEdge2));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testCanObserverSeeEdge3()
   {
      // point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d observer1 = new Point2d(0.0, 0.0);
      assertFalse(ConvexPolygon2dCalculator.canObserverSeeEdge(0, observer1, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetVertexOnSide1()
   {
      // add vertices in clockwise order so updating the polygon does not change indices
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.update();

      Point2d observer1 = new Point2d(0.5, -0.5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 1, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 2, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 3, observer1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(1, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(1, 2, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(1, 3, observer1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(2, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(2, 3, observer1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(3, 3, observer1, polygon), 3);

      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(1, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(2, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(3, 0, observer1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(1, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(2, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(3, 1, observer1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(2, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(3, 2, observer1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(3, 3, observer1, polygon), 3);

      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(0, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(0, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(0, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(0, 3, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(1, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(1, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(1, 3, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(2, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(2, 3, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(3, 3, observer1, polygon), 3);

      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(0, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(1, 0, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(2, 0, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(3, 0, observer1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(1, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(2, 1, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(3, 1, observer1, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(2, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(3, 2, observer1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnRight(3, 3, observer1, polygon), 3);

      Point2d observer2 = new Point2d(0.5, 0.5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 0, observer2, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 1, observer2, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 2, observer2, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 3, observer2, polygon), 3);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetVertexOnSide2()
   {
      // add vertices in clockwise order so updating the polygon does not change indices
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d observer1 = new Point2d(0.0, 0.0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 1, observer1, polygon), 0);

      Point2d observer2 = new Point2d(0.0, 2.0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 1, observer2, polygon), 1);

      Point2d observer3 = new Point2d(10.0, 0.0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 1, observer3, polygon), 0);

      Point2d observer4 = new Point2d(2.0, 2.0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getVertexOnLeft(0, 1, observer4, polygon), 1);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetMiddleIndexCounterClockwise1()
   {
      // do not update polygon to keep number of vertices
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      for (int i = 0; i < 6; i++)
         polygon.addVertex(new Point2d());
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 0, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 1, polygon), 4);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 2, polygon), 5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(3, 3, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(4, 4, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(5, 5, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 0, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 0, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(3, 0, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(4, 0, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(5, 0, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(3, 1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(4, 1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(5, 1, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(3, 2, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(4, 2, polygon), 3);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(5, 2, polygon), 4);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(4, 3, polygon), 4);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(5, 3, polygon), 4);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(5, 4, polygon), 5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 1, polygon), 4);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 2, polygon), 4);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 3, polygon), 5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 4, polygon), 5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 5, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 2, polygon), 5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 3, polygon), 5);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 4, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 5, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 3, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 4, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 5, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(3, 4, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(3, 5, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(4, 5, polygon), 2);

      polygon.clear();
      for (int i = 0; i < 3; i++)
         polygon.addVertex(new Point2d());
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 0, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 1, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 2, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 1, polygon), 2);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 2, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 2, polygon), 0);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(1, 0, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 0, polygon), 1);
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(2, 1, polygon), 2);

      polygon.clear();
      polygon.addVertex(new Point2d());
      assertIndexCorrect(ConvexPolygon2dCalculator.getMiddleIndexCounterClockwise(0, 0, polygon), 0);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetLineOfSightVertices1()
   {
      Point2d vertex1 = new Point2d(0.0, 1.0);
      Point2d vertex2 = new Point2d(1.0, 1.0);
      Point2d vertex3 = new Point2d(1.5, 0.5);
      Point2d vertex4 = new Point2d(1.0, 0.0);
      Point2d vertex5 = new Point2d(0.0, 0.0);

      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(vertex1);
      polygon.addVertex(vertex2);
      polygon.addVertex(vertex3);
      polygon.addVertex(vertex4);
      polygon.addVertex(vertex5);
      polygon.update();

      Point2d observer1 = new Point2d(-0.5, 0.5);
      assertPointsEqual(new Point2d[] {vertex1, vertex5}, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer1, polygon), true);

      Point2d observer2 = new Point2d(1.0, -0.5);
      assertPointsEqual(new Point2d[] {vertex5, vertex3}, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer2, polygon), true);

      Point2d observer3 = new Point2d(-1.0, -2.0 + epsilon);
      assertPointsEqual(new Point2d[] {vertex1, vertex4}, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer3, polygon), true);

      Point2d observer4 = new Point2d(-1.0, -2.0 - epsilon);
      assertPointsEqual(new Point2d[] {vertex1, vertex3}, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer4, polygon), true);

      Point2d observer5 = new Point2d(1.5 + epsilon, 0.5);
      assertPointsEqual(new Point2d[] {vertex4, vertex2}, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer5, polygon), true);

      Point2d observer6 = vertex3;
      assertPointsEqual(null, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer6, polygon), true);

      Point2d observer7 = new Point2d(0.5, 0.5);
      assertPointsEqual(null, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer7, polygon), true);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetLineOfSightVertices2()
   {
      // empty polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();

      Point2d observer1 = new Point2d(0.5, 0.5);
      assertIndicesCorrect(null, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer1, polygon));
      assertPointsEqual(null, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer1, polygon), true);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetLineOfSightVertexIndices1()
   {
      Point2d vertex = new Point2d(-0.5, 0.5);

      // point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(vertex);
      polygon.update();

      Point2d observer1 = vertex;
      assertIndicesCorrect(null, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer1, polygon));

      Point2d observer2 = new Point2d(0.5, 0.5);
      assertIndicesCorrect(new int[] {0, 0}, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer2, polygon));
      assertPointsEqual(new Point2d[] {vertex}, ConvexPolygon2dCalculator.getLineOfSightVerticesCopy(observer2, polygon), true);

      int[] result = new int[] {-1, 7};
      ConvexPolygon2dCalculator.getLineOfSightVertexIndices(observer2, result, polygon);
      assertIndicesCorrect(new int[] {0, 0}, result);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetLineOfSightVertexIndices4()
   {
      // add vertices in clockwise order so updating the polygon does not change indices
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d observer1 = new Point2d(-1.0, 1.0);
      assertIndicesCorrect(new int[] {0, 1}, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer1, polygon));

      Point2d observer2 = new Point2d(0.5, 0.0);
      assertIndicesCorrect(new int[] {0, 1}, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer2, polygon));

      Point2d observer3 = new Point2d(0.5, 1.5);
      assertIndicesCorrect(new int[] {1, 0}, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer3, polygon));

      Point2d observer4 = new Point2d(0.5, 1.0);
      assertIndicesCorrect(null, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer4, polygon));

      Point2d observer5 = new Point2d(1.0, 1.0);
      assertIndicesCorrect(null, ConvexPolygon2dCalculator.getLineOfSightVertexIndicesCopy(observer5, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectionLambda1()
   {
      Random random = new Random(84587278988L);
      for (int i = 0; i < 1000; i++)
      {
         Point2d point1 = new Point2d(random.nextGaussian(), random.nextGaussian());
         Vector2d direction1 = new Vector2d(random.nextGaussian(), random.nextGaussian());
         Point2d point2 = new Point2d(random.nextGaussian(), random.nextGaussian());
         Vector2d direction2 = new Vector2d(random.nextGaussian(), random.nextGaussian());

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);
         Point2d intersection = new Point2d(point1);
         direction1.scale(lambda);
         intersection.add(direction1);

         Line2d line1 = new Line2d(point1, direction1);
         Line2d line2 = new Line2d(point2, direction2);
         assertPointsEqual(line1.intersectionWith(line2), intersection);
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectionLambda2()
   {
      Random random = new Random(8458475566478988L);
      for (int i = 0; i < 1000; i++)
      {
         Point2d point1 = new Point2d(random.nextGaussian(), random.nextGaussian());
         Vector2d direction1 = new Vector2d(random.nextGaussian(), random.nextGaussian());
         Point2d point2 = new Point2d(random.nextGaussian(), random.nextGaussian());
         Vector2d direction2 = new Vector2d(direction1);
         direction2.scale(random.nextGaussian());

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);

         assertTrue("Lines are parallel expected lambda to ne NaN.", Double.isNaN(lambda));
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectionLambda3()
   {
      // check directions aligned with axes
      {
         Point2d point1 = new Point2d(1.0, 1.0);
         Vector2d direction1 = new Vector2d(0.5, 0.5);
         Point2d point2 = new Point2d(point1);
         Vector2d direction2 = new Vector2d(0, 1.0);
         Point2d expected = new Point2d(point1);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);

         Point2d intersection = new Point2d(point1);
         direction1.scale(lambda);
         intersection.add(direction1);

         assertPointsEqual(expected, intersection);
      }

      {
         Point2d point1 = new Point2d(-1.0, -1.0);
         Vector2d direction1 = new Vector2d(0.5, 0.5);
         Point2d point2 = new Point2d(point1);
         Vector2d direction2 = new Vector2d(1.0, 0.0);
         Point2d expected = new Point2d(point1);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);

         Point2d intersection = new Point2d(point1);
         direction1.scale(lambda);
         intersection.add(direction1);

         assertPointsEqual(expected, intersection);
      }

      {
         Point2d point1 = new Point2d(0.0, 1.0);
         Vector2d direction1 = new Vector2d(0.0, 2.0);
         Point2d point2 = new Point2d(0.0, 0.0);
         Vector2d direction2 = new Vector2d(0.5, 0.0);
         Point2d expected = new Point2d(0.0, 0.0);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);

         Point2d intersection = new Point2d(point1);
         direction1.scale(lambda);
         intersection.add(direction1);

         assertPointsEqual(expected, intersection);
      }

      {
         Point2d point1 = new Point2d(1.0, 0.0);
         Vector2d direction1 = new Vector2d(2.0, 0.0);
         Point2d point2 = new Point2d(0.0, 0.0);
         Vector2d direction2 = new Vector2d(0.0, 0.5);
         Point2d expected = new Point2d(0.0, 0.0);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);

         Point2d intersection = new Point2d(point1);
         direction1.scale(lambda);
         intersection.add(direction1);

         assertPointsEqual(expected, intersection);
      }

      {
         Point2d point1 = new Point2d(0.0, 0.0);
         Vector2d direction1 = new Vector2d(0.0, 1.0);
         Point2d point2 = new Point2d(point1);
         Vector2d direction2 = new Vector2d(0.0, 1.0);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);
         assertTrue("Lines are parallel expected lambda to ne NaN.", Double.isNaN(lambda));
      }

      {
         Point2d point1 = new Point2d(1.0, 0.0);
         Vector2d direction1 = new Vector2d(0.0, 1.0);
         Point2d point2 = new Point2d(2.0, 0.0);
         Vector2d direction2 = new Vector2d(0.0, 1.0);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);
         assertTrue("Lines are parallel expected lambda to ne NaN.", Double.isNaN(lambda));
      }

      {
         Point2d point1 = new Point2d(1.0, 2.0);
         Vector2d direction1 = new Vector2d(1.0, 0.0);
         Point2d point2 = new Point2d(1.0, 1.0);
         Vector2d direction2 = new Vector2d(1.0, 0.0);

         double lambda = ConvexPolygon2dCalculator.getIntersectionLambda(point1.x, point1.y, direction1.x, direction1.y, point2.x, point2.y, direction2.x,
               direction2.y);
         assertTrue("Lines are parallel expected lambda to ne NaN.", Double.isNaN(lambda));
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testDoesLineIntersectEdge1()
   {
      // add in order so update does not change indices:
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.addVertex(new Point2d(1.5, 0.5));
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.update();

      Line2d line1 = new Line2d(new Point2d(0.0, 0.1), new Vector2d(1.0, 1.0));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 0, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 1, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 2, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 3, polygon));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 4, polygon));

      Line2d line2 = new Line2d(new Point2d(0.9, 1.0), new Vector2d(1.0, -1.0));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 0, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 1, polygon));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 2, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 3, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 4, polygon));

      Line2d line3 = new Line2d(new Point2d(0.2, 0.6), new Vector2d(1.0, 0.0));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 0, polygon));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 1, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 2, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 3, polygon));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 4, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testDoesLineIntersectEdge2()
   {
      // add in order so update does not change indices:
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Line2d line1 = new Line2d(new Point2d(0.0, 0.3), new Vector2d(1.0, 0.0));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 0, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line1, 1, polygon));

      Line2d line2 = new Line2d(new Point2d(0.0, 0.3), new Vector2d(0.0, 1.0));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 0, polygon));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line2, 1, polygon));

      Line2d line3 = new Line2d(new Point2d(0.0, 0.3), new Vector2d(0.0, -1.0));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 0, polygon));
      assertTrue(ConvexPolygon2dCalculator.doesLineIntersectEdge(line3, 1, polygon));

      Line2d line4 = new Line2d(new Point2d(2.0, 0.3), new Vector2d(0.0, -1.0));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line4, 0, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line4, 1, polygon));

      Line2d line5 = new Line2d(new Point2d(-epsilon, 0.3), new Vector2d(0.0, -1.0));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line5, 0, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line5, 1, polygon));

      Line2d line6 = new Line2d(new Point2d(0.0, 0.3), new Vector2d(1.0, 0.0));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line6, 0, polygon));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line6, 1, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testDoesLineIntersectEdge3()
   {
      ConvexPolygon2d polygon = new ConvexPolygon2d();

      Line2d line5 = new Line2d(new Point2d(0.0, 0.0), new Vector2d(1.0, 0.0));
      assertFalse(ConvexPolygon2dCalculator.doesLineIntersectEdge(line5, 0, polygon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectingEdges1()
   {
      Point2d vertex1 = new Point2d(0.0, 1.0);
      Point2d vertex2 = new Point2d(1.0, 1.0);
      Point2d vertex3 = new Point2d(1.0, 0.0);
      Point2d vertex4 = new Point2d(0.0, 0.0);

      // add in order so update does not change indices:
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(vertex1);
      polygon.addVertex(vertex2);
      polygon.addVertex(vertex3);
      polygon.addVertex(vertex4);
      polygon.update();

      LineSegment2d result1 = new LineSegment2d();
      LineSegment2d result2 = new LineSegment2d();

      Line2d line1 = new Line2d(new Point2d(0.5, 0.5), new Vector2d(-1.0, 0.0));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line1, result1, result2, polygon), 2);
      assertTrue(result1.epsilonEquals(new LineSegment2d(vertex2, vertex3), epsilon));
      assertTrue(result2.epsilonEquals(new LineSegment2d(vertex4, vertex1), epsilon));
      LineSegment2d[] edgesFound1 = ConvexPolygon2dCalculator.getIntersectingEdgesCopy(line1, polygon);
      assertTrue(edgesFound1.length == 2);
      assertTrue(edgesFound1[0].epsilonEquals(new LineSegment2d(vertex2, vertex3), epsilon));
      assertTrue(edgesFound1[1].epsilonEquals(new LineSegment2d(vertex4, vertex1), epsilon));

      Line2d line2 = new Line2d(new Point2d(0.5, 1.5), new Vector2d(1.0, 0.0));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line2, result1, result2, polygon), 0);
      LineSegment2d[] edgesFound2 = ConvexPolygon2dCalculator.getIntersectingEdgesCopy(line2, polygon);
      assertTrue(edgesFound2 == null);

      Line2d line3 = new Line2d(new Point2d(0.0, 2.0), new Vector2d(1.0, -1.0));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line3, result1, result2, polygon), 2);
      assertTrue(result1.epsilonEquals(new LineSegment2d(vertex1, vertex2), epsilon));
      assertTrue(result2.epsilonEquals(new LineSegment2d(vertex2, vertex3), epsilon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectingEdges2()
   {
      // line polygon
      Point2d vertex1 = new Point2d(1.0, 1.0);
      Point2d vertex2 = new Point2d(1.0, 0.0);

      // add in order so update does not change indices:
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(vertex1);
      polygon.addVertex(vertex2);
      polygon.update();

      LineSegment2d result1 = new LineSegment2d();
      LineSegment2d result2 = new LineSegment2d();

      Line2d line1 = new Line2d(new Point2d(0.5, 1.5), new Vector2d(0.0, 0.1));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line1, result1, result2, polygon), 0);

      Line2d line2 = new Line2d(new Point2d(-0.5, 0.0), new Vector2d(0.75, 0.25));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line2, result1, result2, polygon), 2);
      assertTrue(result1.epsilonEquals(new LineSegment2d(vertex1, vertex2), epsilon));
      assertTrue(result2.epsilonEquals(new LineSegment2d(vertex2, vertex1), epsilon));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectingEdges3()
   {
      // point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(-1.0, -0.5));
      polygon.update();

      LineSegment2d result1 = new LineSegment2d();
      LineSegment2d result2 = new LineSegment2d();

      Line2d line1 = new Line2d(new Point2d(0.0, 0.0), new Vector2d(-0.5, -0.25));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line1, result1, result2, polygon), 0);

      Line2d line2 = new Line2d(new Point2d(0.5, 1.5), new Vector2d(0.0, 0.1));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line2, result1, result2, polygon), 0);

      Line2d line3 = new Line2d(new Point2d(-1.0, -0.5), new Vector2d(1.0, 0.1));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line3, result1, result2, polygon), 0);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testGetIntersectingEdges4()
   {
      // empty polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();

      LineSegment2d result1 = new LineSegment2d();
      LineSegment2d result2 = new LineSegment2d();

      Line2d line1 = new Line2d(new Point2d(0.5, 1.5), new Vector2d(0.0, 0.1));
      assertEquals(ConvexPolygon2dCalculator.getIntersectingEdges(line1, result1, result2, polygon), 0);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIntersectionWithLine1()
   {
      // add in order so vertices do not get changed when update is called.
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(0.0, 0.0));
      polygon.addVertex(new Point2d(-1.0, 0.0));
      polygon.addVertex(new Point2d(0.0, 1.0));
      polygon.addVertex(new Point2d(1.0, 1.0));
      polygon.update();

      Point2d result1 = new Point2d(0.6, 0.4);
      Point2d result2 = new Point2d(0.1, 0.9);

      Line2d line1 = new Line2d(new Point2d(0.0, 0.5), new Vector2d(0.1, 0.0));
      Point2d[] expected1 = new Point2d[] {new Point2d(-0.5, 0.5), new Point2d(0.5, 0.5)};
      assertPointsEqual(expected1, ConvexPolygon2dCalculator.intersectionWithLineCopy(line1, polygon), false);

      Line2d line2 = new Line2d(new Point2d(1.0, 0.0), new Vector2d(0.0, -8.0));
      Point2d[] expected2 = new Point2d[] {new Point2d(1.0, 1.0)};
      assertPointsEqual(expected2, ConvexPolygon2dCalculator.intersectionWithLineCopy(line2, polygon), false);
      assertTrue(ConvexPolygon2dCalculator.intersectionWithLine(line2, result1, result2, polygon) == 1);
      assertPointsEqual(expected2[0], result1);

      Line2d line3 = new Line2d(new Point2d(0.0, 1.0), new Vector2d(0.5, 0.0));
      Point2d[] expected3 = new Point2d[] {new Point2d(0.0, 1.0), new Point2d(1.0, 1.0)};
      assertPointsEqual(expected3, ConvexPolygon2dCalculator.intersectionWithLineCopy(line3, polygon), false);
      assertTrue(ConvexPolygon2dCalculator.intersectionWithLine(line3, result1, result2, polygon) == 2);
      assertPointsEqual(expected3[0], result1);
      assertPointsEqual(expected3[1], result2);

      Line2d line4 = new Line2d(new Point2d(0.5, 10.0), new Vector2d(0.0, 0.1));
      Point2d[] expected4 = new Point2d[] {new Point2d(0.5, 1.0), new Point2d(0.5, 0.5)};
      assertPointsEqual(expected4, ConvexPolygon2dCalculator.intersectionWithLineCopy(line4, polygon), false);

      Line2d line5 = new Line2d(new Point2d(-1.0, -0.5), new Vector2d(1.0, 1.0));
      Point2d[] expected5 = new Point2d[] {new Point2d(-0.5, 0.0), new Point2d(0.5, 1.0)};
      assertPointsEqual(expected5, ConvexPolygon2dCalculator.intersectionWithLineCopy(line5, polygon), false);

      Line2d line6 = new Line2d(new Point2d(0.0, -1.5), new Vector2d(1.0, 1.0));
      Point2d[] expected6 = null;
      result1.set(0.0, 0.0);
      result2.set(0.0, 0.0);
      assertPointsEqual(expected6, ConvexPolygon2dCalculator.intersectionWithLineCopy(line6, polygon), false);
      assertTrue(ConvexPolygon2dCalculator.intersectionWithLine(line6, result1, result2, polygon) == 0);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIntersectionWithLine2()
   {
      // line polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.addVertex(new Point2d(-1.0, 0.0));
      polygon.update();

      Line2d line1 = new Line2d(new Point2d(-1.0, 1.0), new Vector2d(0.0, -0.8));
      Point2d[] expected1 = new Point2d[] {new Point2d(-1.0, 0.0)};
      assertPointsEqual(expected1, ConvexPolygon2dCalculator.intersectionWithLineCopy(line1, polygon), false);

      Line2d line2 = new Line2d(new Point2d(-0.5, 1.0), new Vector2d(0.0, -0.8));
      Point2d[] expected2 = new Point2d[] {new Point2d(-0.5, 0.0)};
      assertPointsEqual(expected2, ConvexPolygon2dCalculator.intersectionWithLineCopy(line2, polygon), false);

      Line2d line3 = new Line2d(new Point2d(1.5, 1.0), new Vector2d(0.0, -0.8));
      Point2d[] expected3 = null;
      assertPointsEqual(expected3, ConvexPolygon2dCalculator.intersectionWithLineCopy(line3, polygon), false);

      Line2d line4 = new Line2d(new Point2d(-0.8, 0.0), new Vector2d(0.1, 0.0));
      Point2d[] expected4 = new Point2d[] {new Point2d(-1.0, 0.0), new Point2d(1.0, 0.0)};
      assertPointsEqual(expected4, ConvexPolygon2dCalculator.intersectionWithLineCopy(line4, polygon), false);

      Line2d line5 = new Line2d(new Point2d(1.0, 0.0), new Vector2d(0.0, -0.1));
      Point2d[] expected5 = new Point2d[] {new Point2d(1.0, 0.0)};
      assertPointsEqual(expected5, ConvexPolygon2dCalculator.intersectionWithLineCopy(line5, polygon), false);

      Line2d line6 = new Line2d(new Point2d(-1.0, 0.0), new Vector2d(0.0, -0.1));
      Point2d[] expected6 = new Point2d[] {new Point2d(-1.0, 0.0)};
      assertPointsEqual(expected6, ConvexPolygon2dCalculator.intersectionWithLineCopy(line6, polygon), false);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIntersectionWithLine3()
   {
      // point polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();
      polygon.addVertex(new Point2d(1.0, 0.0));
      polygon.update();

      Line2d line1 = new Line2d(new Point2d(3.0, 1.0), new Vector2d(-2.0, -1.0));
      Point2d[] expected1 = new Point2d[] {new Point2d(1.0, 0.0)};
      assertPointsEqual(expected1, ConvexPolygon2dCalculator.intersectionWithLineCopy(line1, polygon), false);

      Line2d line2 = new Line2d(new Point2d(2.0, 1.0), new Vector2d(-1.3, -0.8));
      Point2d[] expected2 = null;
      assertPointsEqual(expected2, ConvexPolygon2dCalculator.intersectionWithLineCopy(line2, polygon), false);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 3000)
   public void testIntersectionWithLine4()
   {
      // empty polygon
      ConvexPolygon2d polygon = new ConvexPolygon2d();

      Line2d line1 = new Line2d(new Point2d(3.0, 1.0), new Vector2d(-1.6, -0.8));
      Point2d[] expected1 = null;
      assertPointsEqual(expected1, ConvexPolygon2dCalculator.intersectionWithLineCopy(line1, polygon), false);
   }

   private static void assertPointsEqual(Point2d[] expected, Point2d[] actual, boolean enforceOrder)
   {
      if (expected == null || actual == null)
      {
         assertTrue("Expected did not equal actual. One of them was null.", expected == actual);
         return;
      }

      assertEquals("Array lengths are not equal.", expected.length, actual.length);
      int points = expected.length;
      for (int i = 0; i < points; i++)
      {
         if (enforceOrder)
         {
            assertPointsEqual(expected[i], actual[i]);
            continue;
         }

         boolean foundPoint = false;
         for (int j = 0; j < points; j++)
         {
            if (expected[i].epsilonEquals(actual[j], epsilon))
               foundPoint = true;
         }
         assertTrue("Did not find point.", foundPoint);
      }
   }

   private static void assertIndicesCorrect(int[] expected, int[] actual)
   {
      if (expected == null || actual == null)
      {
         assertTrue("Expected did not equal actual. One of them was null.", expected == actual);
         return;
      }

      assertEquals("Array lengths are not equal.", expected.length, actual.length);
      for (int i = 0; i < expected.length; i++)
         assertIndexCorrect(expected[i], actual[i]);
   }

   private static void assertIndexCorrect(int expected, int actual)
   {
      assertEquals("Index does not equal expected.", expected, actual);
   }

   private static void assertDistanceCorrect(double expected, double actual)
   {
      assertEquals("Distance does not equal expected.", expected, actual, epsilon);
   }

   private static void assertPointsEqual(Point2d expected, Point2d actual)
   {
      if (expected == null && actual == null)
         return;

      double localEpsilon = epsilon * expected.distance(new Point2d());
      assertTrue("Point does not match expected.", expected.epsilonEquals(actual, localEpsilon));
   }

   public static void main(String[] args)
   {
      String targetTests = "us.ihmc.robotics.geometry.ConvexPolygon2dCalculatorTest";
      String targetClasses = "us.ihmc.robotics.geometry.ConvexPolygon2dCalculator";
      MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClasses);
   }
}
