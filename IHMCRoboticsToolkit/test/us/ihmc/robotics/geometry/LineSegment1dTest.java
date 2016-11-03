package us.ihmc.robotics.geometry;

import us.ihmc.robotics.geometry.LineSegment1d;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.testing.MutationTestingTools;

import org.junit.Test;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import static org.junit.Assert.*;

public class LineSegment1dTest
{
   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void lineBoundariesTest()
   {
      double firstPoint = 16.7;
      double secondPoint = -2.5;
      double[] pointsArray = {firstPoint, secondPoint};
      double p1 = 2.5;
      double p2 = 18.0;
      double p3 = 0.0;
      double p4 = 7.1;
      double p5 = -5.2;
      double p6 = 16.2;

      LineSegment1d line = new LineSegment1d(pointsArray);

      assertEquals(line.getMinPoint(), secondPoint, 0.001);
      assertEquals(line.getMaxPoint(), firstPoint, 0.001);
      assertEquals(line.getSecondEndpoint(), secondPoint, 0.001);
      assertEquals(line.getFirstEndpoint(), firstPoint, 0.001);
      assertEquals(line.getMidPoint(), p4, 0.001);

      assertFalse(line.isBetweenEndpoints(p2, 0.001));
      assertTrue(line.isBetweenEndpoints(p3, 0.001));
      assertFalse(line.isBetweenEndpoints(p5, 0.001));
      assertFalse(line.isBetweenEndpointsExclusive(firstPoint));
      assertTrue(line.isBetweenEndpointsInclusive(secondPoint));

      assertTrue(line.isBetweenEndpoints(p1, 5.0));
      assertFalse(line.isBetweenEndpoints(p3, 20));
      assertTrue(line.isBetweenEndpoints(p6, 0.5));

      LineSegment1d pointLine = new LineSegment1d(firstPoint, firstPoint);

      assertTrue(pointLine.isBetweenEndpoints(firstPoint, 0.0));
      assertFalse(pointLine.isBetweenEndpoints(firstPoint, 0.01));
      assertFalse(pointLine.isBetweenEndpoints(secondPoint, 0.0));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void lineOverlapsTest()
   {
      double firstPoint = -10;
      double secondPoint = 10;

      LineSegment1d emptyLine = new LineSegment1d();
      LineSegment1d mainLine = new LineSegment1d(firstPoint, secondPoint);
      LineSegment1d separateLine = new LineSegment1d(firstPoint + 100, secondPoint + 100);
      LineSegment1d otherLine1 = new LineSegment1d(firstPoint - 3, secondPoint - 3);
      LineSegment1d intersectionLine1 = new LineSegment1d(firstPoint, secondPoint - 3);
      LineSegment1d otherLine2 = new LineSegment1d(secondPoint, secondPoint + 10);

      assertTrue(mainLine.computeOverlap(otherLine1, emptyLine));
      assertEquals(intersectionLine1.getMaxPoint(), emptyLine.getMaxPoint(), 0.001);
      assertEquals(intersectionLine1.getMinPoint(), emptyLine.getMinPoint(), 0.001);
      assertTrue(intersectionLine1.computeOverlap(emptyLine, emptyLine));
      assertFalse(mainLine.computeOverlap(separateLine, emptyLine));

      assertTrue(mainLine.isOverlappingInclusive(otherLine2));
      assertFalse(mainLine.isOverlappingExclusive(otherLine2));
      assertTrue(mainLine.isOverlappingExclusive(new LineSegment1d(-9.9, 9.9)));

      LineSegment1d intersectionLine2 = mainLine.computeOverlap(otherLine1);
      LineSegment1d intersectionLine3 = mainLine.computeOverlap(separateLine);

      assertEquals(intersectionLine1.getMaxPoint(), intersectionLine2.getMaxPoint(), 0.001);
      assertEquals(intersectionLine1.getMinPoint(), intersectionLine2.getMinPoint(), 0.001);
      assertEquals(null, intersectionLine3);

      assertFalse(mainLine.isBetweenEndpointsExclusive(mainLine));
      assertTrue(mainLine.isBetweenEndpointsInclusive(mainLine));
      assertFalse(mainLine.isBetweenEndpointsInclusive(otherLine2));
      assertTrue(mainLine.isBetweenEndpointsExclusive(new LineSegment1d(-5, 5)));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testDistance()
   {
      double firstPoint = -10;
      double secondPoint = 10;
      double p1 = 15;
      double p2 = -15;
      double p3 = 2.0;

      LineSegment1d mainLine = new LineSegment1d(firstPoint, secondPoint);
      LineSegment1d secondLine = new LineSegment1d(secondPoint, firstPoint);

      assertEquals(mainLine.distance(p1), 5, 0.001);
      assertEquals(mainLine.distance(p2), 5, 0.001);
      assertEquals(mainLine.distance(p3), -8, 0.001);
      assertEquals(mainLine.distance(firstPoint), 0, 0.001);

      assertTrue(mainLine.isBefore(p2));
      assertTrue(mainLine.isAfter(p1));
      assertFalse(mainLine.isBefore(p3));
      assertFalse(mainLine.isAfter(p3));
      assertFalse(mainLine.isBefore(firstPoint));
      assertFalse(mainLine.isAfter(secondPoint));

      assertTrue(secondLine.isBefore(p1));
      assertTrue(secondLine.isAfter(p2));
      assertFalse(secondLine.isBefore(p3));
      assertFalse(secondLine.isAfter(p3));
      assertFalse(secondLine.isBefore(secondPoint));
      assertFalse(secondLine.isAfter(firstPoint));

      assertEquals(mainLine.length(), 20, 0.001);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testExtension()
   {
      double firstPoint = -10;
      double secondPoint = 10;
      double p1 = 15;
      double p2 = -15;
      double p3 = 0.0;

      LineSegment1d mainLine = new LineSegment1d(firstPoint, secondPoint);

      mainLine.extendSegmentToPoint(p1);
      assertEquals(mainLine.getMaxPoint(), p1, 0.001);
      mainLine.extendSegmentToPoint(p2);
      assertEquals(mainLine.getMinPoint(), p2, 0.001);
      mainLine.extendSegmentToPoint(p3);
      assertEquals(mainLine.getMaxPoint(), p1, 0.001);
      assertEquals(mainLine.getMinPoint(), p2, 0.001);

   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testSetters()
   {
      double p1 = 15;
      double p2 = -15;
      boolean fail = false;
      LineSegment1d line1 = new LineSegment1d();
      LineSegment1d line2 = new LineSegment1d(-10,10);

      line1.setFirstEndpoint(p1);
      line1.setSecondEndpoint(p2);
      
      assertEquals(line1.getFirstEndpoint(), p1, 0.001);
      assertEquals(line1.getSecondEndpoint(), p2, 0.001);

      line1.setMaxPoint(17);
      line1.setMinPoint(-17);
      assertEquals(line1.length(), 34, 0.001);
      
      line2.setMaxPoint(17);
      assertEquals(line2.length(), 27, 0.001);

      try
      {
         line1.setMaxPoint(-17);
      }
      catch (RuntimeException e)
      {
         fail = true;
      }

      assertTrue(fail);
      fail = false;

      try
      {
         line1.setMinPoint(17);
      }
      catch (RuntimeException e)
      {
         fail = true;
      }

      assertTrue(fail);
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void toUpperDImensionsTest()
   {
      Point2d point2d = new Point2d(1,1);
      Vector2d direction2d = new Vector2d(1,2);
      LineSegment1d firstLine = new LineSegment1d(0, 10);
      LineSegment2d line2d = firstLine.toLineSegment2d(point2d, direction2d);
      
      assertEquals(line2d.getFirstEndpoint(), new Point2d(1,1));
      assertEquals(line2d.getSecondEndpoint(), new Point2d(11,21));
      
      Point3d point3d = new Point3d(1,1,1);
      Vector3d direction3d = new Vector3d(1,2,3);
      LineSegment3d line3d = firstLine.toLineSegment3d(point3d, direction3d);
      
      assertEquals(line3d.getPointA(), new Point3d(1,1,1));
      assertEquals(line3d.getPointB(), new Point3d(11,21,31));
   }

   public static void main(String[] args)
   {
      String targetTests = "us.ihmc.robotics.geometry.LineSegment1dTest";
      String targetClasses = "us.ihmc.robotics.geometry.LineSegment1d";
      MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClasses);
   }

}
