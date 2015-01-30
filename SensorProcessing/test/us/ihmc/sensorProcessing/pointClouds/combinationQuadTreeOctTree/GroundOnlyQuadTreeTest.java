package us.ihmc.sensorProcessing.pointClouds.combinationQuadTreeOctTree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.ground.RotatableBoxTerrainObject;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.unitTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.dataStructures.AbstractHeightMapTest;
import us.ihmc.utilities.math.dataStructures.HeightMap;
import us.ihmc.utilities.math.geometry.BoundingBox2d;
import us.ihmc.utilities.math.geometry.Box3d;
import us.ihmc.utilities.math.geometry.Plane3d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.graphics.BagOfBalls;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;

public class GroundOnlyQuadTreeTest extends AbstractHeightMapTest
{
   private static final float ALTERNATE_HEIGHT_THRESHOLD = 0.01f;
   private double epsilon = 1e-6;
   private static final float HEIGHT_THRESHOLD = 0.05f;

   private static final boolean DO_ASSERTS = true;

	@AverageDuration
	@Test(timeout = 300000)
   public void testSimpleThings()
   {
      float xMin = 0.0f;
      float yMin = 0.0f;
      float xMax = 1.0f;
      float yMax = 1.0f;

      GroundOnlyQuadTree quadTree1 = new GroundOnlyQuadTree(xMin, yMin, xMax, yMax, 1.0, 0.1, 100000);
      GroundOnlyQuadTree quadTree = quadTree1;
      assertNull(quadTree.get(0.2f, 0.2f));
      quadTree.addToQuadtree(0.5f, 0.5f, 20.0f);
      assertEquals(20.0f, quadTree.get(0.5f, 0.5f), epsilon);
      quadTree.addToQuadtree(0.5f, 0.5f, 1.0f);
      assertEquals(1.0f, quadTree.get(0.5f, 0.5f), epsilon);
      assertEquals(true, quadTree.getLeafNodeAtLocation(0.5f, 0.5f).getMetaData().getIsStuffAboveMe());
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testGettingAreas()
   {
      super.testGettingAreas();
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testSomethingAbove() throws Exception
   {
      float xMin = 0.0f;
      float yMin = 0.0f;
      float xMax = 2.0f;
      float yMax = 2.0f;

      GroundOnlyQuadTree quadTree1 = new GroundOnlyQuadTree(xMin, yMin, xMax, yMax, 1.0, 0.1, 100000);
      GroundOnlyQuadTree quadTree = quadTree1;

      float ground = 0.1f;
      float higher = 1.0f;
      float unseen = Float.NaN;
      float bottom = -5.0f;
      float tiptop = 5.0f;

      float[][] testLocations = new float[][]
      {
         {0.5f, 0.5f},    //
         {0.5f, 1.5f},    //
         {1.5f, 0.5f},    //
         {1.5f, 1.5f}
      };

      int[] indexesToSet = new int[]
      {
         0, 1, 2, 3,    //
         0, 1, 2, 3,    //
         0,    //
         0
      };

      float[] valuesToSet = new float[]
      {
         ground, higher, ground, higher,    //
         higher, ground, higher, ground,    //
         tiptop,    //
         bottom
      };

      int[] expectedLeafNumbers = new int[]
      {
         1, 4, 4, 4,    //
         4, 4, 4, 1,    //
         1,    //
         4
      };

      float[][] expectedAtAllLocations = new float[][]
      {
         {ground, ground, ground, ground}, {ground, higher, unseen, unseen}, {ground, higher, ground, unseen}, {ground, higher, ground, higher},    //
         {ground, higher, ground, higher}, {ground, ground, ground, higher}, {ground, ground, ground, higher}, {ground, ground, ground, ground},    //
         {ground, ground, ground, ground},    //
         {bottom, ground, ground, ground}
      };

      boolean[][] expectedStuffAboveMeFlagAtAllLocations = new boolean[][]
      {
         {false, false, false, false}, {false, false, false, false}, {false, false, false, false}, {false, false, false, false},    //
         {true, false, false, false}, {true, true, false, false}, {true, true, true, false}, {true, true, true, true},    //
         {true, true, true, true},    //
         {true, true, true, true}
      };

      int numTests = expectedLeafNumbers.length;
      for (int i = 0; i < numTests; i++)
      {
         int index = indexesToSet[i];
         float[] point = testLocations[index];
         float x = point[0];
         float y = point[1];
         float value = valuesToSet[i];
         System.out.println("Running test " + i + " putting a point at (" + x + "," + y + ") which is " + value);
         quadTree.addToQuadtree(x, y, value);
         String wrongQuantityString = "test index " + i;
         for (int j = 0; j < testLocations.length; j++)
         {
            float expectedAtLocation = expectedAtAllLocations[i][j];
            float xToTest = testLocations[j][0];
            float yToTest = testLocations[j][1];
            String testString = "test " + i + " location " + j + " with (x,y)=(" + xToTest + "," + yToTest + ") and value = " + expectedAtLocation + "";
            Float float1 = quadTree.get(xToTest, yToTest);
            if (null == float1)
               if (Float.isNaN(expectedAtLocation))
                  continue;
               else
                  fail(testString + "value is unexpectedly null!");
            else
               assertEquals(testString, expectedAtLocation, float1, epsilon);

            assertEquals(expectedStuffAboveMeFlagAtAllLocations[i][j], quadTree.getLeafNodeAtLocation(xToTest, yToTest).getMetaData().getIsStuffAboveMe());
         }

         assertEquals(wrongQuantityString, expectedLeafNumbers[i], quadTree.listAllLeafNodes().size());
      }
   }

/*   @Test(timeout=300000)
   public void testUnhandledPoints()
   {
      super.testUnhandledPoints();
   }*/

   @Override
   public HeightMap getHeightMap(double minX, double minY, double maxX, double maxY, double resolution)
   {
      return new GroundOnlyQuadTree(minX, minY, maxX, maxY, resolution, HEIGHT_THRESHOLD, 100000);
   }

   protected GroundOnlyQuadTree createDefaultQuadTree(float quadTreeResolution)
   {
      float xMin = 0.0f;
      float yMin = 0.0f;
      float xMax = 4.0f;
      float yMax = 4.0f;

      float heightThreshold = ALTERNATE_HEIGHT_THRESHOLD;

      return new GroundOnlyQuadTree(xMin, yMin, xMax, yMax, quadTreeResolution, heightThreshold, 100000);
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testPuttingDifferentHeightPointsAtDifferentLocations() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);
      testEmptyTree(quadTree);

      // first point
      // should remain a single quad and should all have the same value
      double expected = 1.0;
      quadTree.addToQuadtree(1.0f, 1.0f, (float) expected);
      verifyLevelOneAtHeight(quadTree, expected);
      assertTrue(quadTree.listAllLeafNodes().size() == 1);

      // second point
      // should become four quads and each has a different value
      expected = 0.5;
      quadTree.addToQuadtree(1.0f, 3.0f, (float) expected);
      assertTrue(quadTree.listAllLeafNodes().size() == 4);
      double actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testPuttingSimilarHeightPoints() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      // no points
      testEmptyTree(quadTree);

      // first point
      // should remain a single quad and should all have the same value
      double expected = 1.0;
      quadTree.addToQuadtree(1.0f, 1.0f, (float) expected);
      assertTrue(quadTree.listAllLeafNodes().size() == 1);
      verifyLevelOneAtHeight(quadTree, expected);

      // more points at height within threshold
      // should remain one quad
      int numberOfPoints = 50;
      Random random = new Random(777);
      for (int i = 0; i < numberOfPoints; i++)
      {
         quadTree.addToQuadtree(1.0f, 3.0f, (float) expected + (ALTERNATE_HEIGHT_THRESHOLD * random.nextFloat()));
         assertTrue(quadTree.listAllLeafNodes().size() == 1);
         verifyLevelOneAtHeight(quadTree, expected);
      }
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testAll() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      // no points
      testEmptyTree(quadTree);

      // first point
      double expected = 1.0;
      quadTree.addToQuadtree(1.0f, 1.0f, (float) expected);
      assertTrue(quadTree.listAllLeafNodes().size() == 1);
      double actual = quadTree.get(1.0, 1.0);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      assertEquals(expected, actual, epsilon);

      // second point in different quad
      quadTree.addToQuadtree(1.0f, 3.0f, (float) 0.5);
      assertTrue(quadTree.listAllLeafNodes().size() == 4);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(3.0, 1.0));
      assertNull(quadTree.get(3.0, 3.0));

      // third point in different quad
      quadTree.addToQuadtree(3.0f, 3.0f, (float) 1.5);
      assertTrue(quadTree.listAllLeafNodes().size() == 4);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(3.0, 1.0));

      // fourth point in different quad
      quadTree.addToQuadtree(3.0f, 1.0f, (float) 0.0);
      assertTrue(quadTree.listAllLeafNodes().size() == 4);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.0;
      assertEquals(expected, actual, epsilon);

      // fifth point (second point in same quad)
      quadTree.addToQuadtree(0.5f, 0.5f, (float) 0.5);
      assertTrue(quadTree.listAllLeafNodes().size() == 7);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(0.5, 0.5);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.5, 1.5);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(0.5, 1.5));
      assertNull(quadTree.get(1.5, 0.5));

      // sixth point (second point in same sub-quad)
      quadTree.addToQuadtree(0.25f, 0.25f, (float) 0.25);
      assertTrue(quadTree.listAllLeafNodes().size() == 10);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(0.5, 0.5);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.5, 1.5);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(0.5, 1.5));
      assertNull(quadTree.get(1.5, 0.5));
      actual = quadTree.get(0.25, 0.25);
      expected = 0.25;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(0.5, 0.5);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(0.25, 0.75));
      assertNull(quadTree.get(0.75, 0.25));

      // seventh point (third point in same sub-sub-quad and below resolution)
      quadTree.addToQuadtree(0.1f, 0.1f, (float) -7.0);
      assertTrue(quadTree.listAllLeafNodes().size() == 10);
      actual = quadTree.get(0.25, 0.25);
      expected = -7.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(0.5, 0.5);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.5, 1.5);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(0.5, 1.5));
      assertNull(quadTree.get(1.5, 0.5));
      actual = quadTree.get(0.5, 0.5);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      assertNull(quadTree.get(0.25, 0.75));
      assertNull(quadTree.get(0.75, 0.25));

      // point close in height
      quadTree.addToQuadtree(1.1f, 3.1f, (float) 0.495);
      assertTrue(quadTree.listAllLeafNodes().size() == 10);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(0.5, 2.2);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);

      /*
       * Removal is not yet tested in GroundOnlyQuadTree, since it is not used.
       *
       *
       * // remove smallest point
       *   quadTree.remove(0.2f, 0.2f);
       *   assertEquals(7,quadTree.listAllLeafNodes().size());
       *   actual = quadTree.get(1.0, 1.0);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(1.0, 3.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 3.0);
       *   expected = 1.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 1.0);
       *   expected = 0.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.5, 0.5);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(1.5, 1.5);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   assertNull(quadTree.get(0.5, 1.5));
       *   assertNull(quadTree.get(1.5, 0.5));
       *   actual = quadTree.get(0.25, 0.25);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.25, 0.75);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.75, 0.25);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *
       *   // remove null leaf
       *   quadTree.remove(0.25f, 0.75f);
       *   assertTrue(quadTree.listAllLeafNodes().size() == 4);
       *   actual = quadTree.get(1.0, 1.0);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(1.0, 3.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 3.0);
       *   expected = 1.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 1.0);
       *   expected = 0.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.5, 0.5);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(1.5, 1.5);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.5, 1.5);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(1.5, 0.5);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.5, 0.5);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.25, 0.25);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.25, 0.75);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(0.75, 0.25);
       *   expected = 1.0;
       *   assertEquals(expected, actual, epsilon);
       *
       *   // remove another leaf
       *   quadTree.remove(0.5f, 0.5f);
       *   assertTrue(quadTree.listAllLeafNodes().size() == 4);
       *   assertNull(quadTree.get(1.0, 1.0));
       *   actual = quadTree.get(1.0, 3.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 3.0);
       *   expected = 1.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 1.0);
       *   expected = 0.0;
       *   assertEquals(expected, actual, epsilon);
       *   assertNull(quadTree.get(1.5, 1.5));
       *   assertNull(quadTree.get(0.5, 1.5));
       *   assertNull(quadTree.get(1.5, 0.5));
       *   assertNull(quadTree.get(1.5, 0.5));
       *   assertNull(quadTree.get(0.5, 0.5));
       *   assertNull(quadTree.get(0.25, 0.25));
       *
       *   // remove another leaf
       *   quadTree.remove(3.0, 1.0f);
       *   assertTrue(quadTree.listAllLeafNodes().size() == 4);
       *   actual = quadTree.get(1.0, 3.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 3.0);
       *   expected = 1.5;
       *   assertEquals(expected, actual, epsilon);
       *   assertNull(quadTree.get(3.0, 1.0));
       *   assertNull(quadTree.get(1.0, 1.0));
       *
       *   // remove another leaf
       *   quadTree.remove(3.0, 3.0f);
       *   assertTrue(quadTree.listAllLeafNodes().size() == 1);
       *   actual = quadTree.get(1.0, 3.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 3.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(3.0, 1.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *   actual = quadTree.get(1.0, 1.0);
       *   expected = 0.5;
       *   assertEquals(expected, actual, epsilon);
       *
       *   // remove another leaf
       *   quadTree.remove(1.0, 3.0f);
       *   assertTrue(quadTree.listAllLeafNodes().size() == 1);
       *   assertNull(quadTree.get(1.0, 3.0));
       *   assertNull(quadTree.get(3.0, 3.0));
       *   assertNull(quadTree.get(3.0, 1.0));
       *   assertNull(quadTree.get(1.0, 1.0));
       */
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testMerging() throws Exception
   {
      /*
       * Merging in GroundOnlyQuadTrees differs from merging in QuadTrees.
       */

      GroundOnlyQuadTree quadTree = createDefaultQuadTree(2.0f);
      int actualCount = quadTree.countNodes();
      int expectedCount = 1;
      assertEquals(expectedCount, actualCount);

      // add point
      quadTree.addToQuadtree(1.0f, 1.0f, (float) 1.0);
      actualCount = quadTree.countNodes();
      expectedCount = 1;
      assertEquals(expectedCount, actualCount);

      // add point
      quadTree.addToQuadtree(1.0f, 3.0f, (float) 0.5);
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // add point
      quadTree.addToQuadtree(3.0f, 3.0f, (float) 1.5);
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // add point
      quadTree.addToQuadtree(3.0f, 1.0f, (float) 0.75);
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // test values
      double expected = 1.0;
      double actual = quadTree.get(1.0, 1.0);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.75;
      assertEquals(expected, actual, epsilon);

      // change values
      quadTree.addToQuadtree(3.0f, 3.0f, (float) 0.5);
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // test values
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.75;
      assertEquals(expected, actual, epsilon);

      // change values
      quadTree.addToQuadtree(3.0f, 1.0f, (float) 0.5);
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // test values
      actual = quadTree.get(1.0, 1.0);
      expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);

      // change values
      quadTree.addToQuadtree(1.0f, 1.0f, (float) 0.5);

      // test values
      expected = 0.5;
      verifyLevelOneAtHeight(quadTree, expected);

      // won't merge because one has not seen stuff above
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // change values
      quadTree.addToQuadtree(1.0f, 3.0f, (float) 1.5);

      // shouldn'e change because it is higher
      assertEquals(expectedCount, actualCount);

      // should have merged because all have stuff above now
      actualCount = quadTree.countNodes();
      expectedCount = 1;
      assertEquals(expectedCount, actualCount);

      // change value to divide
      quadTree.addToQuadtree(1.0f, 1.0f, (float) 0.45);
      actualCount = quadTree.countNodes();
      expectedCount = 5;
      assertEquals(expectedCount, actualCount);

      // test values
      actual = quadTree.get(1.0, 1.0);
      expected = 0.45;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);

   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testClear() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      // no points
      double expected = 0.0;
      testEmptyTree(quadTree);

      // first point
      expected = 1.0;
      quadTree.addToQuadtree(0.9f, 0.90f, (float) expected);
      assertTrue(quadTree.listAllLeafNodes().size() == 1);
      verifyLevelOneAtHeight(quadTree, expected);

      // clear tree
      quadTree.clear();
      assertTrue(quadTree.listAllLeafNodes().size() == 1);
      assertNull(quadTree.get(1.0, 1.0));

      // first point again
      expected = 1.0;
      quadTree.addToQuadtree(0.9f, 0.90f, (float) expected);
      assertTrue(quadTree.listAllLeafNodes().size() == 1);
      verifyLevelOneAtHeight(quadTree, expected);
   }

   private void testEmptyTree(GroundOnlyQuadTree quadTree)
   {
      assertNull(quadTree.get(1.0f, 1.0f));
      assertTrue(quadTree.listAllLeafNodes().size() == 1);
   }

   private void verifyLevelOneAtHeight(GroundOnlyQuadTree quadTree, double expected)
   {
      double actual = quadTree.get(1.0f, 1.0f);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0f, 3.0f);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0f, 1.0f);
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0f, 3.0f);
      assertEquals(expected, actual, epsilon);
   }

/*   private void verifyLevelOneAtDefaultHeight(GroundOnlyQuadTree quadTree)
   {
      double actual = quadTree.get(1.0, 1.0);
      double expected = 1.0;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(1.0, 3.0);
      expected = 0.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 3.0);
      expected = 1.5;
      assertEquals(expected, actual, epsilon);
      actual = quadTree.get(3.0, 1.0);
      expected = 0.0;
      assertEquals(expected, actual, epsilon);
   }*/

	@AverageDuration
	@Test(timeout = 300000)
   public void testGetMinX() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      double expected = 0.0f;
      double actual = quadTree.getMinX();
      assertEquals(expected, actual, epsilon);
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testGetMaxX() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      double expected = 4.0f;
      double actual = quadTree.getMaxX();
      assertEquals(expected, actual, epsilon);
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testGetMinY() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      double expected = 0.0f;
      double actual = quadTree.getMinY();
      assertEquals(expected, actual, epsilon);
   }

	@AverageDuration
	@Test(timeout = 300000)
   public void testGetMaxY() throws Exception
   {
      GroundOnlyQuadTree quadTree = createDefaultQuadTree(0.5f);

      double expected = 4.0f;
      double actual = quadTree.getMaxY();
      assertEquals(expected, actual, epsilon);
   }

  

   @Ignore
	@AverageDuration
	@Test(timeout = 150000)   public void testOnALineOfPoints()
   {
      ArrayList<Point3d> points = new ArrayList<Point3d>();
      points.add(new Point3d(0.0, 0.0, 0.0));
      points.add(new Point3d(0.1, 0.0, 0.0));
      points.add(new Point3d(0.2, 0.0, 0.2));

//    points.add(new Point3d(0.3, 0.0, 0.2));

      double resolution = 0.02;

      BoundingBox2d boundingBox = new BoundingBox2d(-0.04, -0.04, 1.0, 0.06);
      testOnAListOfPoints(points, boundingBox, resolution);

      ThreadTools.sleepForever();
   }

   @Ignore
	@AverageDuration
	@Test(timeout = 150000)   public void testOnSomeSlopes()
   {
      double halfWidth = 0.5;
      double resolution = 0.1;

      Point3d center = new Point3d(0.0, 0.0, 0.3);
      Vector3d normal = new Vector3d(0.1, 0.2, 0.8);
      testOnASlope(center, normal, halfWidth, resolution);

      center = new Point3d(0.0, 0.0, 0.3);
      normal = new Vector3d(1.0, 1.0, 1.0);
      testOnASlope(center, normal, halfWidth, resolution);

      center = new Point3d(0.0, 0.0, 0.3);
      normal = new Vector3d(-1.0, 1.0, 1.0);
      testOnASlope(center, normal, halfWidth, resolution);

//    ThreadTools.sleepForever();
   }

   @Ignore
	@AverageDuration
	@Test(timeout = 150000)   public void testOnSomeStairCases()
   {
      double halfWidth = 0.6;
      double resolution = 0.02;

      Point3d center = new Point3d(0.0, 0.0, 0.3);
      double stairSeparation = 0.2;
      double oneStairLandingHeight = 0.0;

      Vector3d normal = new Vector3d(0.3, -0.3, 1.0);
      testOnAStaircase(center, normal, halfWidth, resolution, stairSeparation, oneStairLandingHeight);

//    normal = new Vector3d(0.3, 0.3, 1.0);
//    testOnAStaircase(center, normal, halfWidth, resolution, stairSeparation, oneStairLandingHeight);
//    
//    normal = new Vector3d(-0.3, 0.3, 1.0);
//    testOnAStaircase(center, normal, halfWidth, resolution, stairSeparation, oneStairLandingHeight);
//    
//    normal = new Vector3d(-0.3, -0.3, 1.0);
//    testOnAStaircase(center, normal, halfWidth, resolution, stairSeparation, oneStairLandingHeight);

      ThreadTools.sleepForever();
   }

   @Ignore
	@AverageDuration
	@Test(timeout = 150000)   public void testUsingStairGroundProfile()
   {
      CombinedTerrainObject3D groundProfile = createStepsGroundProfile();

      double centerX = -3.5;
      double centerY = 3.5;
      double halfWidth = 0.6;

      double minX = centerX - halfWidth;
      double minY = centerY - halfWidth;
      double maxX = centerX + halfWidth;
      double maxY = centerY + halfWidth;

      BoundingBox2d boundingBox = new BoundingBox2d(minX, minY, maxX, maxY);

      double resolution = 0.02;
      double heightThreshold = 0.002;
      int maxNodes = 1000000;

      QuadTreeTestHelper testHelper = new QuadTreeTestHelper(boundingBox);
      testHelper.setResolutionParameters(resolution, heightThreshold, maxNodes);

      ArrayList<Point3d> points = testHelper.createAListOfPointsFromAGroundProfile(groundProfile, minX, minY, maxX, maxY, resolution);
      testHelper.doATest(points);

      // TODO: Get this to pass!
      testHelper.assertPointsLieOnHeightMap(points);

      ThreadTools.sleepForever();
   }

   private void testOnAStaircase(Point3d center, Vector3d normal, double halfWidth, double resolution, double stairSeparation, double oneStairLandingHeight)
   {
      normal.normalize();

      BoundingBox2d boundingBox = new BoundingBox2d(center.getX() - halfWidth, center.getY() - halfWidth, center.getX() + halfWidth, center.getY() + halfWidth);
      Plane3d plane3d = new Plane3d(center, normal);
      ArrayList<Point3d> points = generatePointsForStairs(plane3d, halfWidth, resolution, stairSeparation, oneStairLandingHeight);

//    Collections.shuffle(points);

      testOnAListOfPoints(points, boundingBox, resolution);
   }

   private void testOnASlope(Point3d center, Vector3d normal, double halfWidth, double resolution)
   {
      normal.normalize();

      BoundingBox2d boundingBox = new BoundingBox2d(center.getX() - halfWidth, center.getY() - halfWidth, center.getX() + halfWidth, center.getY() + halfWidth);
      Plane3d plane3d = new Plane3d(center, normal);
      ArrayList<Point3d> points = generatePointsForSlope(plane3d, halfWidth, resolution);
      testOnAListOfPoints(points, boundingBox, resolution);
   }

   private void testOnAListOfPoints(ArrayList<Point3d> points, BoundingBox2d rangeOfPoints, double resolution)
   {
      double heightThreshold = 0.002;
      int maxNodes = 1000000;

      QuadTreeTestHelper testHelper = new QuadTreeTestHelper(rangeOfPoints);
      testHelper.setResolutionParameters(resolution, heightThreshold, maxNodes);

      testHelper.doATest(points);
      testHelper.assertPointsLieOnHeightMap(points);
   }


   private static ArrayList<Point3d> generatePointsForStairs(Plane3d plane3d, double halfWidth, double stepSize, double stairSeparation,
           double oneStairLandingHeight)
   {
      ArrayList<Point3d> ret = generatePointsForSlope(plane3d, halfWidth, stepSize);
      formStaircaseWithPointsOnAPlane(ret, stairSeparation, oneStairLandingHeight);

      return ret;
   }

   private static ArrayList<Point3d> generatePointsForSlope(Plane3d plane3d, double halfWidth, double stepSize)
   {
      Point3d centerPoint = plane3d.getPointCopy();

      double minX = centerPoint.getX() - halfWidth;
      double minY = centerPoint.getY() - halfWidth;
      double maxX = centerPoint.getX() + halfWidth;
      double maxY = centerPoint.getY() + halfWidth;

      ArrayList<Point3d> points = new ArrayList<Point3d>();

      for (double x = minX; x < maxX; x = x + stepSize)
      {
         for (double y = minY; y < maxY; y = y + stepSize)
         {
            double z = plane3d.getZOnPlane(x, y);
            points.add(new Point3d(x, y, z));
         }
      }

      return points;
   }

   private static void formStaircaseWithPointsOnAPlane(ArrayList<Point3d> pointsList, double stairSeparation, double oneStairLandingHeight)
   {
      for (Point3d point3d : pointsList)
      {
         double z = point3d.getZ();

         double newZ = Math.floor((z - oneStairLandingHeight) / stairSeparation) * stairSeparation;
         point3d.setZ(newZ);
      }
   }


   private static class QuadTreeTestHelper
   {
      private HeightMap heightMap;

      private final BoundingBox2d rangeOfPointsToTest;

      private double resolution = 0.1;
      private double heightThreshold = 0.002;
      private int maxNodes = 1000000;

      public QuadTreeTestHelper(BoundingBox2d rangeOfPointsToTest)
      {
         this.rangeOfPointsToTest = rangeOfPointsToTest;
      }

      public void setResolutionParameters(double resolution, double heightThreshold, int maxNodes)
      {
         this.resolution = resolution;
         this.heightThreshold = heightThreshold;
         this.maxNodes = maxNodes;
      }

      public HeightMap getHeightMap()
      {
         return heightMap;
      }

      public void doATest(ArrayList<Point3d> points)
      {
         Robot robot = new Robot("TestQuadTree");
         SimulationConstructionSet scs = new SimulationConstructionSet(robot);
         scs.setGroundVisible(true);
         YoVariableRegistry registry = robot.getRobotsYoVariableRegistry();

         YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();

         double ballSize = resolution * 0.35;
         BagOfBalls bagOfBalls = new BagOfBalls(points.size(), ballSize, registry, yoGraphicsListRegistry);
         ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
         YoFramePoint queryPoint = new YoFramePoint("queryPoint", worldFrame, registry);

         YoGraphicPosition queryViz = new YoGraphicPosition("Query", queryPoint, 1.1 * ballSize, YoAppearance.PaleGoldenRod());
         yoGraphicsListRegistry.registerYoGraphic("Query", queryViz);

         heightMap = createHeightMapFromAListOfPoints(queryPoint, bagOfBalls, scs, points, rangeOfPointsToTest, resolution, heightThreshold, maxNodes);


//       BoundingBox3d boundingBox = new BoundingBox3d(minX, minY, -1000.0, maxX, maxY, 1000.0);
//       GroundProfileFromHeightMap groundProfileFromHeightMap = GroundProfileFromHeightMap.createAGroundProfileFromAHeightMapWithPoints(heightMap, boundingBox);



         scs.addYoGraphicsListRegistry(yoGraphicsListRegistry);

         Point2d centerPoint = new Point2d();
         rangeOfPointsToTest.getCenterPointCopy(centerPoint);
         List<Point3d> allPointsWithinArea = heightMap.getAllPointsWithinArea(centerPoint.getX(), centerPoint.getY(), 10.0, 10.0);
         for (Point3d point3d : allPointsWithinArea)
         {
            Graphics3DObject staticLinkGraphics = new Graphics3DObject();
            staticLinkGraphics.translate(new Vector3d(point3d.getX(), point3d.getY(), point3d.getZ() + 0.001));

            double cubeSize = resolution * 0.5;
            staticLinkGraphics.translate(new Vector3d(0.0, 0.0, -cubeSize / 2.0));
            staticLinkGraphics.addCube(cubeSize, cubeSize, cubeSize, YoAppearance.Chartreuse());
            scs.addStaticLinkGraphics(staticLinkGraphics);
         }

         for (Point3d point : points)
         {
            double z = heightMap.getHeightAtPoint(point.getX(), point.getY());

            Graphics3DObject staticLinkGraphics = new Graphics3DObject();
            staticLinkGraphics.translate(new Vector3d(point.getX(), point.getY(), z + 0.001));

            double cubeSize = resolution * 0.35;
            staticLinkGraphics.addCube(cubeSize, cubeSize, cubeSize / 3.0, YoAppearance.Purple());
            scs.addStaticLinkGraphics(staticLinkGraphics);
         }


         scs.startOnAThread();
      }

      public void assertPointsLieOnHeightMap(ArrayList<Point3d> points)
      {
         if (DO_ASSERTS)
         {
            for (Point3d point : points)
            {
               double heightMapZ = heightMap.getHeightAtPoint(point.getX(), point.getY());
               assertEquals(point.getZ(), heightMapZ, 1e-7);
            }
         }
      }

      public ArrayList<Point3d> createAListOfPointsFromAGroundProfile(GroundProfile3D groundProfile, BoundingBox2d testingRange, double resolution)
      {
         double minX = testingRange.getMinPoint().getX();
         double maxX = testingRange.getMaxPoint().getX();
         double minY = testingRange.getMinPoint().getY();
         double maxY = testingRange.getMaxPoint().getY();

         return createAListOfPointsFromAGroundProfile(groundProfile, minX, minY, maxX, maxY, resolution);
      }

      public ArrayList<Point3d> createAListOfPointsFromAGroundProfile(GroundProfile3D groundProfile, double minX, double minY, double maxX, double maxY,
              double resolution)
      {
         ArrayList<Point3d> points = new ArrayList<Point3d>();
         for (double x = minX; x < maxX; x = x + resolution)
         {
            for (double y = minY; y < maxY; y = y + resolution)
            {
               double z = groundProfile.getHeightMapIfAvailable().heightAt(x, y, 0.0);
               points.add(new Point3d(x, y, z));
            }
         }

         return points;
      }


      public HeightMap createHeightMapFromAListOfPoints(YoFramePoint queryPoint, BagOfBalls bagOfBalls, SimulationConstructionSet scs,
              ArrayList<Point3d> points, BoundingBox2d testingRange, double resolution, double heightThreshold, int maxNodes)
      {
         double minX = testingRange.getMinPoint().getX();
         double maxX = testingRange.getMaxPoint().getX();
         double minY = testingRange.getMinPoint().getY();
         double maxY = testingRange.getMaxPoint().getY();

//       CleanQuadTreeHeightMap heightMap = new CleanQuadTreeHeightMap(minX, minY, maxX, maxY, resolution, heightThreshold);
//         CleanQuadTreeHeightMap heightMap = new CleanQuadTreeHeightMap(minX - resolution, minY - resolution, maxX + resolution, maxY + resolution, resolution,
//                                               heightThreshold);

//         heightMap.checkRepInvarients();
//       QuadTreeHeightMap heightMap = new QuadTreeHeightMap(minX, minY, maxX, maxY, resolution, heightThreshold);
         GroundOnlyQuadTree heightMap = new GroundOnlyQuadTree(testingRange, resolution, heightThreshold, maxNodes);


         for (Point3d point : points)
         {
            queryPoint.set(point);

            heightMap.addPoint(point.getX(), point.getY(), point.getZ());

            Graphics3DObject staticLinkGraphics = new Graphics3DObject();
            staticLinkGraphics.translate(new Vector3d(point.getX(), point.getY(), point.getZ() + 0.001));

            double cubeSize = resolution * 0.35;
            staticLinkGraphics.addCube(cubeSize, cubeSize, cubeSize / 3.0, YoAppearance.Blue());

            if (scs != null)
            {
               scs.addStaticLinkGraphics(staticLinkGraphics);

               bagOfBalls.reset();

               for (Point3d checkPoint : points)
               {
                  double z2 = heightMap.getHeightAtPoint(checkPoint.getX(), checkPoint.getY());
                  bagOfBalls.setBall(checkPoint.getX(), checkPoint.getY(), z2);
               }

               scs.tickAndUpdate();
            }
         }

         return heightMap;
      }
   }


   private CombinedTerrainObject3D createStepsGroundProfile()
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D("stairs");

      AppearanceDefinition color = YoAppearance.DarkGray();
      double courseAngle = 135;
      int numberOfSteps = 3;
      double rise = 0.2;
      double startDistance = 4.0;
      double run = 0.4;

      for (int i = 0; i < numberOfSteps; i++)
      {
         double[] newPoint = rotateAroundOrigin(new double[] {startDistance + (i * run), 0}, courseAngle);
         setUpWall(combinedTerrainObject, newPoint, 3.0, run, rise * (i + 1), courseAngle, color);
      }

      {
         double[] newPoint = rotateAroundOrigin(new double[] {startDistance + (numberOfSteps * run), 0}, courseAngle);
         setUpWall(combinedTerrainObject, newPoint, 3.0, run, rise * (numberOfSteps - 1 + 1), courseAngle, color);
      }

      for (int i = 1; i < numberOfSteps + 1; i++)
      {
         double offset = numberOfSteps * run;
         double[] newPoint = rotateAroundOrigin(new double[] {offset + startDistance + (i * run), 0}, courseAngle);
         setUpWall(combinedTerrainObject, newPoint, 3.0, run, rise * (-i + numberOfSteps + 1), courseAngle, color);
      }

      return combinedTerrainObject;
   }

   private static double[] rotateAroundOrigin(double[] xy, double angdeg)
   {
      double x = xy[0];
      double y = xy[1];
      double[] newPoint = new double[2];
      double angRad = Math.toRadians(angdeg);
      newPoint[0] = x * Math.cos(angRad) - y * Math.sin(angRad);
      newPoint[1] = y * Math.cos(angRad) + x * Math.sin(angRad);

      return newPoint;
   }

   private static void setUpWall(CombinedTerrainObject3D combinedTerrainObject, double[] xy, double width, double length, double height, double yawDegrees,
                                 AppearanceDefinition app)
   {
      double x = xy[0];
      double y = xy[1];
      RigidBodyTransform location = new RigidBodyTransform();
      location.rotZ(Math.toRadians(yawDegrees));

      location.setTranslation(new Vector3d(x, y, height / 2));
      RotatableBoxTerrainObject newBox = new RotatableBoxTerrainObject(new Box3d(location, length, width, height), app);
      combinedTerrainObject.addTerrainObject(newBox);
   }

}
