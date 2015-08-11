package us.ihmc.robotics.geometry;

import org.junit.Ignore;
import org.junit.Test;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.tools.random.RandomTools;
import us.ihmc.tools.test.JUnitTools;

import javax.vecmath.Point2d;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConvexPolygonShrinkerTest
{

   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testSimpleSquareConvexPolygonShrinking()
   {
      ArrayList<Point2d> vertices = new ArrayList<Point2d>();
      
      vertices.add(new Point2d(0.0, 0.0));
      vertices.add(new Point2d(1.0, 0.0));
      vertices.add(new Point2d(1.0, 1.0));
      vertices.add(new Point2d(0.0, 1.0));
      
      ConvexPolygon2d polygon = new ConvexPolygon2d(vertices);
      ConvexPolygonShrinker shrinker = new ConvexPolygonShrinker();
      ConvexPolygon2d shrunkenPolygon = new ConvexPolygon2d();
      
      shrinker.shrinkConstantDistanceInto(polygon, 0.1, shrunkenPolygon);
      
      JUnitTools.assertTuple2dEquals(new Point2d(0.1, 0.1), shrunkenPolygon.getVertexCCW(0), 1e-7);
      JUnitTools.assertTuple2dEquals(new Point2d(0.9, 0.1), shrunkenPolygon.getVertexCCW(1), 1e-7);
      JUnitTools.assertTuple2dEquals(new Point2d(0.9, 0.9), shrunkenPolygon.getVertexCCW(2), 1e-7);
      JUnitTools.assertTuple2dEquals(new Point2d(0.1, 0.9), shrunkenPolygon.getVertexCCW(3), 1e-7);
      
      
      polygon = new ConvexPolygon2d(vertices);
      
      shrinker.shrinkConstantDistanceInto(polygon, 1.1, shrunkenPolygon);
      JUnitTools.assertTuple2dEquals(new Point2d(0.5, 0.5), shrunkenPolygon.getVertexCCW(0), 1e-7);
      assertEquals(1, shrunkenPolygon.getNumberOfVertices());
   }
   
   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testSimpleTriangleConvexPolygonShrinking()
   {
      ArrayList<Point2d> vertices = new ArrayList<Point2d>();

      vertices.add(new Point2d(0.0, 0.5));
      vertices.add(new Point2d(1.0, 0.0));
      vertices.add(new Point2d(2.2, 1.0));

      ConvexPolygon2d polygon = new ConvexPolygon2d(vertices);
      ConvexPolygonShrinker shrinker = new ConvexPolygonShrinker();
      ConvexPolygon2d shrunkenPolygon = new ConvexPolygon2d();

      shrinker.shrinkConstantDistanceInto(polygon, 0.1, shrunkenPolygon);


      JUnitTools.assertTuple2dEquals(new Point2d(0.986224428207409, 0.11869118477128504), shrunkenPolygon.getVertexCCW(0), 1e-7);
      JUnitTools.assertTuple2dEquals(new Point2d(1.8160104213223893, 0.8101795123671021), shrunkenPolygon.getVertexCCW(1), 1e-7);
      JUnitTools.assertTuple2dEquals(new Point2d(0.2947361006115917, 0.4644353485691937), shrunkenPolygon.getVertexCCW(2), 1e-7);

      polygon = new ConvexPolygon2d(vertices);

      shrinker.shrinkConstantDistanceInto(polygon, 1.1, shrunkenPolygon);
      JUnitTools.assertTuple2dEquals(new Point2d(1.0666666666, 0.5), shrunkenPolygon.getVertexCCW(0), 1e-7);
      assertEquals(1, shrunkenPolygon.getNumberOfVertices());
   }

   
   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testSimpleLineConvexPolygonShrinking()
   {
      ArrayList<Point2d> vertices = new ArrayList<Point2d>();
      
      vertices.add(new Point2d(-1.0, 3.0));
      vertices.add(new Point2d(1.0, 3.0));
      
      ConvexPolygon2d polygon = new ConvexPolygon2d(vertices);
      ConvexPolygonShrinker shrinker = new ConvexPolygonShrinker();
      ConvexPolygon2d shrunkenPolygon = new ConvexPolygon2d();

      shrinker.shrinkConstantDistanceInto(polygon, 0.1, shrunkenPolygon);
      
      JUnitTools.assertTuple2dEquals(new Point2d(0.9, 3.0), shrunkenPolygon.getVertexCCW(0), 1e-7);
      JUnitTools.assertTuple2dEquals(new Point2d(-0.9, 3.0), shrunkenPolygon.getVertexCCW(1), 1e-7);
      
      polygon = new ConvexPolygon2d(vertices);
      
      shrinker.shrinkConstantDistanceInto(polygon, 1.1, shrunkenPolygon);
      
      JUnitTools.assertTuple2dEquals(new Point2d(0.0, 3.0), shrunkenPolygon.getVertexCCW(0), 1e-7);
      assertEquals(1, shrunkenPolygon.getNumberOfVertices());
   }
   
   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testSimplePointConvexPolygonShrinking()
   {
      ArrayList<Point2d> vertices = new ArrayList<Point2d>();
      
      vertices.add(new Point2d(-1.0, 3.0));
      
      ConvexPolygon2d polygon = new ConvexPolygon2d(vertices);
      ConvexPolygonShrinker shrinker = new ConvexPolygonShrinker();
      ConvexPolygon2d shrunkenPolygon = new ConvexPolygon2d();

      shrinker.shrinkConstantDistanceInto(polygon, 0.1, shrunkenPolygon);
      
      JUnitTools.assertTuple2dEquals(new Point2d(-1.0, 3.0), shrunkenPolygon.getVertexCCW(0), 1e-7);
   }
   
   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testShrinkingRandomPolygonsAreCompletelyInsideOriginalPolygons()
   {
      Random random = new Random(1984L);
      ReferenceFrame zUpFrame = ReferenceFrame.getWorldFrame();
      
      double xMin = -2.0;
      double xMax = 2.0;
      double yMin = -1.0;
      double yMax = 4.0;
      double widthMax = 2.2;
      double heightMax = 1.3;
      int numberOfPoints = random.nextInt(20);
      int numberOfPolygons = 100;
      
      ArrayList<FrameConvexPolygon2d> randomPolygons = ConvexPolygon2dTestHelpers.generateRandomPolygons(random, zUpFrame, xMin, xMax, yMin, yMax, widthMax, heightMax, numberOfPoints, numberOfPolygons);
      
      ConvexPolygonShrinker shrinker = new ConvexPolygonShrinker();
      FrameConvexPolygon2d shrunkenPolygon = new FrameConvexPolygon2d();
      
      for (FrameConvexPolygon2d randomPolygon : randomPolygons)
      {
         double distance = RandomTools.generateRandomDouble(random, 0.001, 5.0);
         shrinker.shrinkConstantDistanceInto(randomPolygon, distance, shrunkenPolygon);
        
         ConvexPolygon2d bigPolygon = randomPolygon.getConvexPolygon2dCopy();
         ConvexPolygon2d smallPolygon = shrunkenPolygon.getConvexPolygon2dCopy();
         
         boolean completelyInside = smallPolygon.isCompletelyInside(bigPolygon);
         assertTrue(completelyInside);
      }
   }
   
   @Ignore("Use manually when making sure no garbage is generated or doing timing tests.")
   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testMemoryGarbageGeneration()
   {
      ArrayList<Point2d> vertices = new ArrayList<Point2d>();
      
      vertices.add(new Point2d(0.0, 0.0));
      vertices.add(new Point2d(0.5, -0.2));
      vertices.add(new Point2d(1.0, 0.0));
      vertices.add(new Point2d(1.2, 0.5));
      vertices.add(new Point2d(1.0, 1.0));
      vertices.add(new Point2d(0.5, 1.2));
      vertices.add(new Point2d(0.0, 1.0));
      vertices.add(new Point2d(-0.2, 0.5));
      
      ConvexPolygon2d polygon = new ConvexPolygon2d(vertices);
      ConvexPolygonShrinker shrinker = new ConvexPolygonShrinker();
      ConvexPolygon2d shrunkenPolygon = new ConvexPolygon2d();
      
      int numberOfTests = 2000;
      
      long startTime = System.currentTimeMillis();
      for (int i=0; i<numberOfTests; i++)
      {
         shrinker.shrinkConstantDistanceInto(polygon, 0.1, shrunkenPolygon);
      }
      long endTime = System.currentTimeMillis();
      
      double millisPerTest = ((double) (endTime - startTime)) / ((double) numberOfTests);
      
      System.out.println("millisPerTest = " + millisPerTest);
   }
   
   

}
