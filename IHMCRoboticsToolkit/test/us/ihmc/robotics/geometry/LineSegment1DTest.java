package us.ihmc.robotics.geometry;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Test;

import us.ihmc.commons.MutationTestFacilitator;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.geometry.LineSegment1D;
import us.ihmc.euclid.geometry.LineSegment2D;
import us.ihmc.euclid.geometry.LineSegment3D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;

public class LineSegment1DTest
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

      LineSegment1D line = new LineSegment1D(pointsArray);

      assertEquals(line.getMinPoint(), secondPoint, 0.001 / line.length());
      assertEquals(line.getMaxPoint(), firstPoint, 0.001 / line.length());
      assertEquals(line.getSecondEndpoint(), secondPoint, 0.001 / line.length());
      assertEquals(line.getFirstEndpoint(), firstPoint, 0.001 / line.length());
      assertEquals(line.getMidPoint(), p4, 0.001 / line.length());

      assertFalse(line.isBetweenEndpoints(p2, 0.001 / line.length()));
      assertTrue(line.isBetweenEndpoints(p3, 0.001 / line.length()));
      assertFalse(line.isBetweenEndpoints(p5, 0.001 / line.length()));
      assertFalse(line.isBetweenEndpointsExclusive(firstPoint));
      assertTrue(line.isBetweenEndpointsInclusive(secondPoint));

      assertTrue(line.isBetweenEndpoints(p1, 4.999 / line.length()));
      assertFalse(line.isBetweenEndpoints(p3, 20 / line.length()));
      assertTrue(line.isBetweenEndpoints(p6, 0.5 / line.length()));

      LineSegment1D pointLine = new LineSegment1D(firstPoint, firstPoint);

      assertTrue(pointLine.isBetweenEndpoints(firstPoint, 0.0));
      assertTrue(pointLine.isBetweenEndpoints(firstPoint, 0.01));
      assertFalse(pointLine.isBetweenEndpoints(secondPoint, 0.0));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void lineOverlapsTest1()
   {
      double firstPoint = -10;
      double secondPoint = 10;

      LineSegment1D emptyLine = new LineSegment1D();
      LineSegment1D mainLine = new LineSegment1D(firstPoint, secondPoint);
      LineSegment1D separateLine = new LineSegment1D(firstPoint + 100, secondPoint + 100);
      LineSegment1D otherLine1 = new LineSegment1D(firstPoint - 3, secondPoint - 3);
      LineSegment1D intersectionLine1 = new LineSegment1D(firstPoint, secondPoint - 3);
      LineSegment1D otherLine2 = new LineSegment1D(secondPoint, secondPoint + 10);

      assertTrue(mainLine.computeOverlap(otherLine1, emptyLine));
      assertEquals(intersectionLine1.getMaxPoint(), emptyLine.getMaxPoint(), 0.001);
      assertEquals(intersectionLine1.getMinPoint(), emptyLine.getMinPoint(), 0.001);
      assertTrue(intersectionLine1.computeOverlap(emptyLine, emptyLine));
      assertFalse(mainLine.computeOverlap(separateLine, emptyLine));

      assertTrue(mainLine.isOverlappingInclusive(otherLine2));
      assertFalse(mainLine.isOverlappingExclusive(otherLine2));
      assertTrue(mainLine.isOverlappingExclusive(new LineSegment1D(-9.9, 9.9)));

      LineSegment1D intersectionLine2 = mainLine.computeOverlap(otherLine1);
      LineSegment1D intersectionLine3 = mainLine.computeOverlap(separateLine);

      assertEquals(intersectionLine1.getMaxPoint(), intersectionLine2.getMaxPoint(), 0.001);
      assertEquals(intersectionLine1.getMinPoint(), intersectionLine2.getMinPoint(), 0.001);
      assertEquals(null, intersectionLine3);

      assertFalse(mainLine.isBetweenEndpointsExclusive(mainLine));
      assertTrue(mainLine.isBetweenEndpointsInclusive(mainLine));
      assertFalse(mainLine.isBetweenEndpointsInclusive(otherLine2));
      assertTrue(mainLine.isBetweenEndpointsExclusive(new LineSegment1D(-5, 5)));
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void lineOverlapsTest2()
   {
      Random random = new Random(32423L);

      // lineSegment2 inside lineSegment1
      for (int i = 0; i < 1000; i++)
      {
         double lineSegmentStart1 = RandomNumbers.nextDouble(random, 10.0);
         double lineSegmentEnd1 = RandomNumbers.nextDouble(random, 10.0);
         LineSegment1D lineSegment1 = new LineSegment1D(lineSegmentStart1, lineSegmentEnd1);

         double boundaryOne = lineSegmentStart1 < lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double boundaryTwo = lineSegmentStart1 > lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double lineSegmentStart2 = RandomNumbers.nextDouble(random, boundaryOne, boundaryTwo);
         double lineSegmentEnd2 = RandomNumbers.nextDouble(random, boundaryOne, boundaryTwo);
         LineSegment1D lineSegment2 = new LineSegment1D(lineSegmentStart2, lineSegmentEnd2);

         double expectedOverlapStart = lineSegmentStart2;
         double expectedOverlapEnd = lineSegmentEnd2;

         LineSegment1D actualOverlap = new LineSegment1D();

         boolean areOverlaping = lineSegment1.computeOverlap(lineSegment2, actualOverlap);
         assertTrue(areOverlaping);

         if (actualOverlap.getFirstEndpoint() != expectedOverlapStart)
         {
            assertTrue(expectedOverlapStart == actualOverlap.getSecondEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getFirstEndpoint());
         }
         else
         {
            assertTrue(expectedOverlapStart == actualOverlap.getFirstEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getSecondEndpoint());
         }

         areOverlaping = lineSegment2.computeOverlap(lineSegment1, actualOverlap);
         assertTrue(areOverlaping);

         if (actualOverlap.getFirstEndpoint() != expectedOverlapStart)
         {
            assertTrue(expectedOverlapStart == actualOverlap.getSecondEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getFirstEndpoint());
         }
         else
         {
            assertTrue(expectedOverlapStart == actualOverlap.getFirstEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getSecondEndpoint());
         }
      }

      // lineSegment2 partially overlapping lineSegment1 case 1
      for (int i = 0; i < 1000; i++)
      {
         double lineSegmentStart1 = RandomNumbers.nextDouble(random, 10.0);
         double lineSegmentEnd1 = RandomNumbers.nextDouble(random, 10.0);
         LineSegment1D lineSegment1 = new LineSegment1D(lineSegmentStart1, lineSegmentEnd1);

         double boundaryOne = lineSegmentStart1 < lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double boundaryTwo = lineSegmentStart1 > lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double lineSegmentStart2 = RandomNumbers.nextDouble(random, boundaryOne, boundaryTwo);
         double lineSegmentEnd2 = RandomNumbers.nextDouble(random, boundaryOne - 10.0, boundaryOne);
         LineSegment1D lineSegment2 = new LineSegment1D(lineSegmentStart2, lineSegmentEnd2);

         double expectedOverlapStart = lineSegmentStart2;
         double expectedOverlapEnd = boundaryOne;

         LineSegment1D actualOverlap = new LineSegment1D();

         boolean areOverlaping = lineSegment1.computeOverlap(lineSegment2, actualOverlap);
         assertTrue(areOverlaping);

         if (actualOverlap.getFirstEndpoint() != expectedOverlapStart)
         {
            assertTrue(expectedOverlapStart == actualOverlap.getSecondEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getFirstEndpoint());
         }
         else
         {
            assertTrue(expectedOverlapStart == actualOverlap.getFirstEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getSecondEndpoint());
         }

         areOverlaping = lineSegment2.computeOverlap(lineSegment1, actualOverlap);
         assertTrue(areOverlaping);

         if (actualOverlap.getFirstEndpoint() != expectedOverlapStart)
         {
            assertTrue(expectedOverlapStart == actualOverlap.getSecondEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getFirstEndpoint());
         }
         else
         {
            assertTrue(expectedOverlapStart == actualOverlap.getFirstEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getSecondEndpoint());
         }
      }

      // lineSegment2 partially overlapping lineSegment1 case 2
      for (int i = 0; i < 1000; i++)
      {
         double lineSegmentStart1 = RandomNumbers.nextDouble(random, 10.0);
         double lineSegmentEnd1 = RandomNumbers.nextDouble(random, 10.0);
         LineSegment1D lineSegment1 = new LineSegment1D(lineSegmentStart1, lineSegmentEnd1);

         double boundaryOne = lineSegmentStart1 < lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double boundaryTwo = lineSegmentStart1 > lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double lineSegmentStart2 = RandomNumbers.nextDouble(random, boundaryOne, boundaryTwo);
         double lineSegmentEnd2 = RandomNumbers.nextDouble(random, boundaryTwo, boundaryTwo + 10.0);
         LineSegment1D lineSegment2 = new LineSegment1D(lineSegmentStart2, lineSegmentEnd2);

         double expectedOverlapStart = lineSegmentStart2;
         double expectedOverlapEnd = boundaryTwo;

         LineSegment1D actualOverlap = new LineSegment1D();

         boolean areOverlaping = lineSegment1.computeOverlap(lineSegment2, actualOverlap);
         assertTrue(areOverlaping);

         if (actualOverlap.getFirstEndpoint() != expectedOverlapStart)
         {
            assertTrue(expectedOverlapStart == actualOverlap.getSecondEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getFirstEndpoint());
         }
         else
         {
            assertTrue(expectedOverlapStart == actualOverlap.getFirstEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getSecondEndpoint());
         }

         areOverlaping = lineSegment2.computeOverlap(lineSegment1, actualOverlap);
         assertTrue(areOverlaping);

         if (actualOverlap.getFirstEndpoint() != expectedOverlapStart)
         {
            assertTrue(expectedOverlapStart == actualOverlap.getSecondEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getFirstEndpoint());
         }
         else
         {
            assertTrue(expectedOverlapStart == actualOverlap.getFirstEndpoint());
            assertTrue(expectedOverlapEnd == actualOverlap.getSecondEndpoint());
         }
      }

      // lineSegment2 not overlapping lineSegment1 case 1
      for (int i = 0; i < 1000; i++)
      {
         double lineSegmentStart1 = RandomNumbers.nextDouble(random, 10.0);
         double lineSegmentEnd1 = RandomNumbers.nextDouble(random, 10.0);
         LineSegment1D lineSegment1 = new LineSegment1D(lineSegmentStart1, lineSegmentEnd1);

         double max = lineSegmentStart1 > lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double lineSegmentStart2 = RandomNumbers.nextDouble(random, max, max + 10.0);
         double lineSegmentEnd2 = RandomNumbers.nextDouble(random, max, max + 10.0);
         LineSegment1D lineSegment2 = new LineSegment1D(lineSegmentStart2, lineSegmentEnd2);

         LineSegment1D actualOverlap = new LineSegment1D();

         boolean areOverlaping = lineSegment1.computeOverlap(lineSegment2, actualOverlap);
         assertFalse(areOverlaping);

         areOverlaping = lineSegment2.computeOverlap(lineSegment1, actualOverlap);
         assertFalse(areOverlaping);
      }

      // lineSegment2 not overlapping lineSegment1 case 2
      for (int i = 0; i < 1000; i++)
      {
         double lineSegmentStart1 = RandomNumbers.nextDouble(random, 10.0);
         double lineSegmentEnd1 = RandomNumbers.nextDouble(random, 10.0);
         LineSegment1D lineSegment1 = new LineSegment1D(lineSegmentStart1, lineSegmentEnd1);

         double min = lineSegmentStart1 < lineSegmentEnd1 ? lineSegmentStart1 : lineSegmentEnd1;
         double lineSegmentStart2 = RandomNumbers.nextDouble(random, min - 10.0, min);
         double lineSegmentEnd2 = RandomNumbers.nextDouble(random, min - 10.0, min);
         LineSegment1D lineSegment2 = new LineSegment1D(lineSegmentStart2, lineSegmentEnd2);

         LineSegment1D actualOverlap = new LineSegment1D();

         boolean areOverlaping = lineSegment1.computeOverlap(lineSegment2, actualOverlap);
         assertFalse(areOverlaping);

         areOverlaping = lineSegment2.computeOverlap(lineSegment1, actualOverlap);
         assertFalse(areOverlaping);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testSignedDistance()
   {
      double firstPoint = -10;
      double secondPoint = 10;
      double p1 = 15;
      double p2 = -15;
      double p3 = 2.0;

      LineSegment1D mainLine = new LineSegment1D(firstPoint, secondPoint);
      LineSegment1D secondLine = new LineSegment1D(secondPoint, firstPoint);

      assertEquals(mainLine.signedDistance(p1), 5, 0.001);
      assertEquals(mainLine.signedDistance(p2), 5, 0.001);
      assertEquals(mainLine.signedDistance(p3), -8, 0.001);
      assertEquals(mainLine.signedDistance(firstPoint), 0, 0.001);

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

      LineSegment1D mainLine = new LineSegment1D(firstPoint, secondPoint);

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
      LineSegment1D line1 = new LineSegment1D();
      LineSegment1D line2 = new LineSegment1D(-10,10);

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
      Point2D point2d = new Point2D(1,1);
      Vector2D direction2d = new Vector2D(1,2);
      LineSegment1D firstLine = new LineSegment1D(0, 10);
      LineSegment2D line2d = firstLine.toLineSegment2d(point2d, direction2d);
      
      assertEquals(line2d.getFirstEndpoint(), new Point2D(1,1));
      assertEquals(line2d.getSecondEndpoint(), new Point2D(11,21));
      
      Point3D point3d = new Point3D(1,1,1);
      Vector3D direction3d = new Vector3D(1,2,3);
      LineSegment3D line3d = firstLine.toLineSegment3d(point3d, direction3d);
      
      assertEquals(line3d.getFirstEndpoint(), new Point3D(1,1,1));
      assertEquals(line3d.getSecondEndpoint(), new Point3D(11,21,31));
   }

   public static void main(String[] args)
   {
      MutationTestFacilitator.facilitateMutationTestForClass(LineSegment1D.class, LineSegment1DTest.class);
   }

}
