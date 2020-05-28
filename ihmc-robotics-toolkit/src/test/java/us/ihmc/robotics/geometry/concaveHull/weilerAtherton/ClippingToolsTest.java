package us.ihmc.robotics.geometry.concaveHull.weilerAtherton;

import org.junit.jupiter.api.Test;
import us.ihmc.euclid.tools.EuclidCoreTestTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.robotics.geometry.concavePolygon2D.ConcavePolygon2D;
import us.ihmc.robotics.geometry.concavePolygon2D.weilerAtherton.ClippingTools;
import us.ihmc.robotics.geometry.concavePolygon2D.weilerAtherton.LinkedPoint;
import us.ihmc.robotics.geometry.concavePolygon2D.weilerAtherton.LinkedPointList;

import javax.sound.sampled.Clip;

import static us.ihmc.robotics.Assert.*;

public class ClippingToolsTest
{
   @Test
   public void testCreateLinkedPointList()
   {
      // check simple square
      ConcavePolygon2D polygon = new ConcavePolygon2D();
      polygon.addVertex(-1.0, 1.0);
      polygon.addVertex(1.0, 1.0);
      polygon.addVertex(1.0, -1.0);
      polygon.addVertex(-1.0, -1.0);
      polygon.update();

      LinkedPointList pointList = ClippingTools.createLinkedPointList(polygon);
      LinkedPoint point = pointList.getFirstPoint();

      // check the chain
      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
         point = point.getSuccessor();
      assertTrue(point == pointList.getFirstPoint());

      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
      {
         EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex " + i, polygon.getVertex(i), point.getPoint(), 1e-6);
         point = point.getSuccessor();
      }

      // Check triangle
      polygon.clear();
      polygon.addVertex(0.0, 1.0);
      polygon.addVertex(1.0, 0.0);
      polygon.addVertex(-1.0, 0.0);
      polygon.update();

      pointList = ClippingTools.createLinkedPointList(polygon);
      point = pointList.getFirstPoint();

      // check the chain
      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
      {
         point = point.getSuccessor();
      }
      assertTrue(point == pointList.getFirstPoint());

      for (int i = 0; i < polygon.getNumberOfVertices(); i++)
      {
         EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex " + i, polygon.getVertex(i), point.getPoint(), 1e-6);
         point = point.getSuccessor();
      }
   }

   @Test
   public void testAddingIntersections()
   {
      // Check two squares overlapping along edges
      ConcavePolygon2D polygonA = new ConcavePolygon2D();
      polygonA.addVertex(-1.0, 1.0);
      polygonA.addVertex(1.0, 1.0);
      polygonA.addVertex(1.0, -1.0);
      polygonA.addVertex(-1.0, -1.0);
      polygonA.update();

      ConcavePolygon2D polygonB = new ConcavePolygon2D();
      polygonB.addVertex(0.5, 0.5);
      polygonB.addVertex(1.5, 0.5);
      polygonB.addVertex(1.5, -0.5);
      polygonB.addVertex(0.5, -0.5);
      polygonB.update();

      LinkedPointList pointListA = ClippingTools.createLinkedPointList(polygonA);
      LinkedPointList pointListB = ClippingTools.createLinkedPointList(polygonB);

      ConcavePolygon2D polygonAWithIntersectionsExpected = new ConcavePolygon2D();
      polygonAWithIntersectionsExpected.addVertex(-1.0, 1.0);
      polygonAWithIntersectionsExpected.addVertex(1.0, 1.0);
      polygonAWithIntersectionsExpected.addVertex(1.0, 0.5);
      polygonAWithIntersectionsExpected.addVertex(1.0, -0.5);
      polygonAWithIntersectionsExpected.addVertex(1.0, -1.0);
      polygonAWithIntersectionsExpected.addVertex(-1.0, -1.0);
      polygonAWithIntersectionsExpected.update();

      ConcavePolygon2D polygonBWithIntersectionsExpected = new ConcavePolygon2D();
      polygonBWithIntersectionsExpected.addVertex(0.5, 0.5);
      polygonBWithIntersectionsExpected.addVertex(1.0, 0.5);
      polygonBWithIntersectionsExpected.addVertex(1.5, 0.5);
      polygonBWithIntersectionsExpected.addVertex(1.5, -0.5);
      polygonBWithIntersectionsExpected.addVertex(1.0, -0.5);
      polygonBWithIntersectionsExpected.addVertex(0.5, -0.5);
      polygonBWithIntersectionsExpected.update();

      ClippingTools.insertIntersectionsIntoList(pointListA, polygonB);

      LinkedPoint pointOnA = pointListA.getFirstPoint();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 0" ,
                                                           polygonAWithIntersectionsExpected.getVertex(0),
                                                           pointOnA.getPoint(),
                                                           1e-6);
      pointOnA = pointOnA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 1" ,
                                                           polygonAWithIntersectionsExpected.getVertex(1),
                                                           pointOnA.getPoint(),
                                                           1e-6);
      pointOnA = pointOnA.getSuccessor();// midpoint
      pointOnA = pointOnA.getSuccessor();// midpoint
      pointOnA = pointOnA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 2" ,
                                                           polygonAWithIntersectionsExpected.getVertex(2),
                                                           pointOnA.getPoint(),
                                                           1e-6);
      pointOnA = pointOnA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 3" ,
                                                           polygonAWithIntersectionsExpected.getVertex(3),
                                                           pointOnA.getPoint(),
                                                           1e-6);
      pointOnA = pointOnA.getSuccessor();
      assertEquals(4, polygonAWithIntersectionsExpected.getNumberOfVertices());
      assertTrue(pointOnA == pointListA.getFirstPoint());

      ClippingTools.insertIntersectionsIntoList(pointListB, polygonA);

      LinkedPoint pointOnB = pointListB.getFirstPoint();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 0" ,
                                                           polygonBWithIntersectionsExpected.getVertex(0),
                                                           pointOnB.getPoint(),
                                                           1e-6);
      pointOnB = pointOnB.getSuccessor();
      pointOnB = pointOnB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 1" ,
                                                           polygonBWithIntersectionsExpected.getVertex(1),
                                                           pointOnB.getPoint(),
                                                           1e-6);
      pointOnB = pointOnB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 2" ,
                                                           polygonBWithIntersectionsExpected.getVertex(2),
                                                           pointOnB.getPoint(),
                                                           1e-6);
      pointOnB = pointOnB.getSuccessor();
      pointOnB = pointOnB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals("Failed at vertex 3" ,
                                                           polygonBWithIntersectionsExpected.getVertex(3),
                                                           pointOnB.getPoint(),
                                                           1e-6);

      pointOnB = pointOnB.getSuccessor();
      assertTrue(pointOnB == pointListB.getFirstPoint());
   }

   @Test
   public void testSingleVertexIntersection()
   {
      ConcavePolygon2D polygonToClip = new ConcavePolygon2D();
      polygonToClip.addVertex(1.0, 1.0);
      polygonToClip.addVertex(1.0, -1.0);
      polygonToClip.addVertex(-1.0, -1.0);
      polygonToClip.addVertex(-1.0, 1.0);
      polygonToClip.update();

      ConcavePolygon2D clippingPolygon = new ConcavePolygon2D();
      clippingPolygon.addVertex(-0.5, 1.5);
      clippingPolygon.addVertex(0.5, 1.5);
      clippingPolygon.addVertex(0, 1.0);
      clippingPolygon.update();

      LinkedPointList pointList = ClippingTools.createLinkedPointList(polygonToClip);
      ClippingTools.insertIntersectionsIntoList(pointList, clippingPolygon);

      LinkedPoint vertex = pointList.getFirstPoint();
      do
      {
         assertFalse(vertex.getIsIntersectionPoint());
         vertex = vertex.getSuccessor();
      }
      while (vertex != pointList.getFirstPoint());
   }

   @Test
   public void testTrickyInsertIntersections()
   {
      ConcavePolygon2D polygonToClip = new ConcavePolygon2D();
      polygonToClip.addVertex(-1.0, 1.0);
      polygonToClip.addVertex(1.0, 1.0);
      polygonToClip.addVertex(1.0, -1.0);
      polygonToClip.addVertex(-1.0, -1.0);
      polygonToClip.update();

      ConcavePolygon2D clippingPolygon = new ConcavePolygon2D();
      clippingPolygon.addVertex(0.5, 1.5);
      clippingPolygon.addVertex(1.5, 1.5);
      clippingPolygon.addVertex(0.5, 0.5);
      clippingPolygon.update();

      LinkedPointList listA = ClippingTools.createLinkedPointList(polygonToClip);
      LinkedPointList listB = ClippingTools.createLinkedPointList(clippingPolygon);

      ClippingTools.insertIntersectionsIntoList(listA, clippingPolygon);
      ClippingTools.insertIntersectionsIntoList(listB, polygonToClip);

      LinkedPoint pointA = listA.getFirstPoint();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-1.0, 1.0), pointA.getPoint(), 1e-7);
      pointA = pointA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 1.0), pointA.getPoint(), 1e-7);
      pointA = pointA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, 1.0), pointA.getPoint(), 1e-7);
      pointA = pointA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, -1.0), pointA.getPoint(), 1e-7);
      pointA = pointA.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-1.0, -1.0), pointA.getPoint(), 1e-7);

      LinkedPoint pointB = listB.getFirstPoint();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 1.5), pointB.getPoint(), 1e-7);
      pointB = pointB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.5, 1.5), pointB.getPoint(), 1e-7);
      pointB = pointB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, 1.0), pointB.getPoint(), 1e-7);
      pointB = pointB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 0.5), pointB.getPoint(), 1e-7);
      pointB = pointB.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 1.0), pointB.getPoint(), 1e-7);
   }

   @Test
   public void testInsertIntersections()
   {
      ConcavePolygon2D polygon1 = new ConcavePolygon2D();
      polygon1.addVertex(-1.0, 1.0);
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.update();

      ConcavePolygon2D polygon2 = new ConcavePolygon2D();
      polygon2.addVertex(0.5, 0.5);
      polygon2.addVertex(1.5, 0.5);
      polygon2.addVertex(1.5, -0.5);
      polygon2.addVertex(0.5, -0.5);
      polygon2.update();

      LinkedPointList list = ClippingTools.createLinkedPointList(polygon1);
      ClippingTools.insertIntersectionsIntoList(list, polygon2);

      LinkedPoint point = list.getFirstPoint();
      point = point.getSuccessor();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, 0.5), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, -0.5), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());
   }

   @Test
   public void testInsertIntersections2()
   {
      ConcavePolygon2D polygon1 = new ConcavePolygon2D();
      polygon1.addVertex(-1.0, 1.0);
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.update();

      ConcavePolygon2D polygon2 = new ConcavePolygon2D();
      polygon2.addVertex(-1.0, 1.5);
      polygon2.addVertex(1.0, 1.5);
      polygon2.addVertex(0.0, 0.5);
      polygon2.update();

      LinkedPointList list = ClippingTools.createLinkedPointList(polygon1);
      ClippingTools.insertIntersectionsIntoList(list, polygon2);

      LinkedPoint point = list.getFirstPoint();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.5, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isOutgoingIntersection());
      assertFalse(point.isIncomingIntersection());

      list = ClippingTools.createLinkedPointList(polygon2);
      ClippingTools.insertIntersectionsIntoList(list, polygon1);
      point = list.getFirstPoint();
      point = point.getSuccessor();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.5, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());
   }

   @Test
   public void testInsertIntersections3()
   {
      ConcavePolygon2D polygon1 = new ConcavePolygon2D();
      polygon1.addVertex(-1.0, 1.0);
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.update();

      ConcavePolygon2D polygon2 = new ConcavePolygon2D();
      polygon2.addVertex(-0.5, 1.5);
      polygon2.addVertex(0.5, 1.5);
      polygon2.addVertex(0.0, 1.0);
      polygon2.update();

      LinkedPointList list = ClippingTools.createLinkedPointList(polygon1);
      ClippingTools.insertIntersectionsIntoList(list, polygon2);

      LinkedPoint point = list.getFirstPoint();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0, 1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());

      list = ClippingTools.createLinkedPointList(polygon2);
      ClippingTools.insertIntersectionsIntoList(list, polygon1);
      point = list.getFirstPoint();
      point = point.getSuccessor();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.0, 1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());
   }

   @Test
   public void testInsertIntersections4()
   {
      ConcavePolygon2D uPolygon = new ConcavePolygon2D();
      uPolygon.addVertex(-1.0, 1.0);
      uPolygon.addVertex(-0.9, 1.0);
      uPolygon.addVertex(-0.9, -0.9);
      uPolygon.addVertex(0.9, -0.9);
      uPolygon.addVertex(0.9, 1.0);
      uPolygon.addVertex(1.0, 1.0);
      uPolygon.addVertex(1.0, -1.0);
      uPolygon.addVertex(-1.0, -1.0);
      uPolygon.update();

      ConcavePolygon2D hat = new ConcavePolygon2D();
      hat.addVertex(-1.0, 1.0);
      hat.addVertex(1.0, 1.0);
      hat.addVertex(1.0, 0.9);
      hat.addVertex(-1.0, 0.9);
      hat.update();

      LinkedPointList list = ClippingTools.createLinkedPointList(uPolygon);
      ClippingTools.insertIntersectionsIntoList(list, hat);

      LinkedPoint point = list.getFirstPoint();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-1.0, 1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.9, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.9, 0.9), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.9, -0.9), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.9, -0.9), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());
   }

   @Test
   public void testInsertIntersections5()
   {
      ConcavePolygon2D polygon1 = new ConcavePolygon2D();
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.addVertex(-1.0, 1.0);
      polygon1.update();

      ConcavePolygon2D polygon2 = new ConcavePolygon2D();
      polygon2.addVertex(-0.1, 1.0);
      polygon2.addVertex(0.0, 1.1);
      polygon2.addVertex(0.1, 1.0);
      polygon2.addVertex(0.1, -1.0);
      polygon2.addVertex(0.0, -1.1);
      polygon2.addVertex(-0.1, -1.0);
      polygon2.update();

      LinkedPointList list = ClippingTools.createLinkedPointList(polygon1);
      ClippingTools.insertIntersectionsIntoList(list, polygon2);

      LinkedPoint point = list.getFirstPoint();
      point = point.getSuccessor();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.1, -1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.1, -1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());

      point = point.getSuccessor();
      point = point.getSuccessor();
      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-0.1, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.1, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());
   }

   @Test
   public void testInsertIntersections6()
   {
      ConcavePolygon2D polygon1 = new ConcavePolygon2D();
      polygon1.addVertex(-1.0, 1.0);
      polygon1.addVertex(1.0, 1.0);
      polygon1.addVertex(0.75, 0.5);
      polygon1.addVertex(1.0, -1.0);
      polygon1.addVertex(-1.0, -1.0);
      polygon1.update();

      ConcavePolygon2D polygon2 = new ConcavePolygon2D();
      polygon2.addVertex(0.5, 1.5);
      polygon2.addVertex(1.5, 1.5);
      polygon2.addVertex(1.5, 0.5);
      polygon2.addVertex(0.5, 0.5);
      polygon2.update();

      LinkedPointList list = ClippingTools.createLinkedPointList(polygon1);
      ClippingTools.insertIntersectionsIntoList(list, polygon2);

      LinkedPoint point = list.getFirstPoint();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-1.0, 1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.5, 1.0), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertTrue(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, 1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(0.75, 0.5), point.getPoint(), 1e-7);
      assertTrue(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertTrue(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(1.0, -1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      point = point.getSuccessor();
      EuclidCoreTestTools.assertPoint2DGeometricallyEquals(new Point2D(-1.0, -1.0), point.getPoint(), 1e-7);
      assertFalse(point.getIsIntersectionPoint());
      assertFalse(point.isIncomingIntersection());
      assertFalse(point.isOutgoingIntersection());

      assertEquals(list.getFirstPoint(), point.getSuccessor());
   }
}
