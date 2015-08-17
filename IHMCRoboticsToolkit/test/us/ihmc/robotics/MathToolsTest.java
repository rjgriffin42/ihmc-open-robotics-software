package us.ihmc.robotics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.GeometryTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.tools.random.RandomTools;
import us.ihmc.tools.test.JUnitTools;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MathToolsTest
{
   private Random random;

   @Before
   public void setUp() throws Exception
   {
      random = new Random(100L);
   }

   @After
   public void tearDown() throws Exception
   {
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testConstructor()
           throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
   {
      // Screw you clover, I can test the private constructor(!)
      assertEquals(1, MathTools.class.getDeclaredConstructors().length);
      Constructor<MathTools> constructor = MathTools.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()));
      constructor.setAccessible(true);
      constructor.newInstance();
   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testClipToMinMaxWrongBounds()
   {
      double min = 1.0;
      double max = 0.9;
      MathTools.clipToMinMax(5.0, min, max);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testClipToMinMax_2()
   {
      Random rand = new Random();
      for (int i = 0; i < 1000; i++)
      {
         double max = rand.nextDouble() * 1000.0;
         double clippedVal = MathTools.clipToMinMax(max * 2.0, max);
         assertEquals(clippedVal, max, 1e-7);

         max = rand.nextDouble() * 1000.0;
         clippedVal = MathTools.clipToMinMax(max * -2.0, max);
         assertEquals(clippedVal, -max, 1e-7);

         max = rand.nextDouble() * 1000.0;
         clippedVal = MathTools.clipToMinMax((float) (max * 2.0), (float) max);
         assertEquals(clippedVal, max, 1e-4);

         max = rand.nextDouble() * 1000.0;
         clippedVal = MathTools.clipToMinMax((float) (max * -2.0), (float) max);
         assertEquals(clippedVal, -max, 1e-4);
      }
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsNumber()
   {
      Random rand = new Random();
      for (int i = 0; i < 1000; i++)
      {
         assertTrue(MathTools.isNumber(rand.nextDouble() * 1000.0));
         assertFalse(MathTools.containsNaN(new Vector3d(rand.nextDouble() * 1000.0, rand.nextDouble() * 1000.0, rand.nextDouble() * 1000.0)));
      }

      assertFalse(MathTools.isNumber(Double.NaN));
      assertTrue(MathTools.containsNaN(new Vector3d(Double.NaN, Double.NaN, Double.NaN)));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testClipToMinMaxNaN()
   {
      assertTrue(Double.isNaN(MathTools.clipToMinMax(Double.NaN, 0.0, 1.0)));
   }



   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testCheckIfInRange()
   {
      assertTrue(MathTools.isInsideBoundsInclusive(5, 0, 6));
      assertTrue(MathTools.isInsideBoundsInclusive(6, 0, 6));
      assertTrue(MathTools.isInsideBoundsInclusive(0, 0, 6));
      assertFalse(MathTools.isInsideBoundsInclusive(7, 0, 6));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testCheckIfInRange_2()
   {
      MathTools.checkIfInRange(-5, -1, 1);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testClipToMinMax()
   {
      for (int i = 0; i < 10; i++)
      {
         double min = random.nextDouble();
         double max = min + random.nextDouble();
         double val = 3.0 * (random.nextDouble() - 0.25);
         double result = MathTools.clipToMinMax(val, min, max);

         boolean tooSmall = result < min;
         boolean tooBig = result > max;
         if (tooSmall || tooBig)
         {
            fail();
         }

         boolean withinBounds = (val > min) && (val < max);
         if (withinBounds)
         {
            assertEquals(val, result, 1e-10);
         }
      }
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void squareTest()
   {
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, 25, 10.0);
      for (double randomValue : randomValues)
      {
         assertEquals(MathTools.square(randomValue), Math.pow(randomValue, 2), 1e-12);
      }
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void cubeTest()
   {
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, 25, 10.0);
      for (double randomValue : randomValues)
      {
         assertEquals(MathTools.cube(randomValue), Math.pow(randomValue, 3), 1e-12);
      }
   }

   @EstimatedDuration(duration = 0.04)
   @Test(timeout = 30000)
   public void powWithIntegerTest()
   {
      int numberOfTrials = 10000;
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, numberOfTrials, 0.1, 10.0);
      int[] randomExponents = RandomTools.generateRandomIntArray(random, numberOfTrials, 10);
      for (int i = 0; i < numberOfTrials; i++)
      {
         double x = randomValues[i];
         int exp = randomExponents[i];
         double xPowedToTest = MathTools.powWithInteger(x, exp);
         double xPowedExpected = Math.pow(x, (double) exp);
         double errorRatio = (xPowedToTest - xPowedExpected) / xPowedExpected;
         boolean isRelativeErrorLowEnough = MathTools.epsilonEquals(errorRatio, 0.0, 1.0e-15);
         assertTrue(isRelativeErrorLowEnough);
      }
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void signTest()
   {
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, 25, 10.0);
      for (double randomValue : randomValues)
      {
         if (randomValue == 0.0)
            continue;
         assertEquals(Math.signum(randomValue), MathTools.sign(randomValue), 1e-12);
      }

      assertEquals(1.0, MathTools.sign(0.0), 1e-12);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void sumDoublesTest()
   {
      double[] posVals = new double[25];
      double[] negVals = new double[25];
      ArrayList<Double> posValsList = new ArrayList<Double>();
      ArrayList<Double> negValsList = new ArrayList<Double>();
      for (int i = 0; i < 25; i++)
      {
         posVals[i] = 1.5;
         negVals[i] = -1.5;
         posValsList.add(1.5);
         negValsList.add(-1.5);
      }

      assertEquals(MathTools.sumDoubles(posVals), 37.5, 1e-12);
      assertEquals(MathTools.sumDoubles(negVals), -37.5, 1e-12);

      assertEquals(MathTools.sumDoubles(posValsList), 37.5, 1e-12);
      assertEquals(MathTools.sumDoubles(negValsList), -37.5, 1e-12);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void sumIntegerTest()
   {
      int[] posVals = new int[25];
      int[] negVals = new int[25];
      ArrayList<Integer> posValsList = new ArrayList<Integer>();
      ArrayList<Integer> negValsList = new ArrayList<Integer>();
      for (int i = 0; i < 25; i++)
      {
         posVals[i] = 1;
         negVals[i] = -1;
         posValsList.add(1);
         negValsList.add(-1);
      }

      assertEquals(MathTools.sumIntegers(posVals), 25);
      assertEquals(MathTools.sumIntegers(negVals), -25);

      assertEquals(MathTools.sumIntegers(posValsList), 25);
      assertEquals(MathTools.sumIntegers(negValsList), -25);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void dotPlusTest()
   {
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, 25, 10.0);
      int[] randomInts = RandomTools.generateRandomIntArray(random, 25, 10);
      double sumOfRandomValues = MathTools.sumDoubles(randomValues);
      long sumOfInts = MathTools.sumIntegers(randomInts);

      randomValues = MathTools.dotPlus(randomValues, 7.3);
      assertEquals(sumOfRandomValues + 25 * 7.3, MathTools.sumDoubles(randomValues), 1e-12);

      randomInts = MathTools.dotPlus(randomInts, 7);
      assertEquals(sumOfInts + 25 * 7, MathTools.sumIntegers(randomInts));

   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsInsideBounds()
   {
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, 25, 12.5);
      for (double randomValue : randomValues)
      {
         assertTrue(MathTools.isInsideBoundsExclusive(randomValue, -12.5, 12.5));

         if (randomValue < 0)
            randomValue -= 12.6;
         else
            randomValue += 12.6;

         assertFalse(MathTools.isInsideBoundsExclusive(randomValue, -12.5, 12.5));

      }

      assertFalse(MathTools.isInsideBoundsExclusive(Double.NaN, -10.0, 10.0));

      assertFalse(MathTools.isInsideBoundsExclusive(10.0, -10.0, 10.0));
      assertFalse(MathTools.isInsideBoundsExclusive(-10.0, -10.0, 10.0));

   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testIsInsideBoundsWrongBounds()
   {
      double min = 1.0;
      double max = 0.9;
      MathTools.isInsideBoundsExclusive(5.0, min, max);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsInsideBoundsInclusive()
   {
      double[] randomValues = RandomTools.generateRandomDoubleArray(random, 25, 12.5);
      for (double randomValue : randomValues)
      {
         assertTrue(MathTools.isInsideBoundsInclusive(randomValue, -12.5, 12.5));

         if (randomValue < 0)
            randomValue -= 12.6;
         else
            randomValue += 12.6;

         assertFalse(MathTools.isInsideBoundsInclusive(randomValue, -12.5, 12.5));

      }

      assertFalse(MathTools.isInsideBoundsInclusive(Double.NaN, -10.0, 10.0));

      assertTrue(MathTools.isInsideBoundsInclusive(10.0, -10.0, 10.0));
      assertTrue(MathTools.isInsideBoundsInclusive(-10.0, -10.0, 10.0));


      assertTrue(MathTools.isInsideBoundsInclusive(5.0, 5.0, 5.0));

   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testIsInsideBoundsWrongBoundsInclusive()
   {
      double min = 1.0;
      double max = 0.9;
      MathTools.isInsideBoundsInclusive(5.0, min, max);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testMin()
   {
      double[] numbers =
      {
         -1.0, -4.0, 4.0, 3.0, 0.0, 1.0, -2.0, -5.0, -3.0, 2.0, 2.0, 3.0, 5.0, 5.0
      };

      assertEquals(MathTools.min(numbers), -5.0, 1e-34);

      numbers[4] = Double.POSITIVE_INFINITY;
      assertFalse(Double.isInfinite(MathTools.min(numbers)));

      numbers[4] = Double.NEGATIVE_INFINITY;
      assertTrue(Double.isInfinite(MathTools.min(numbers)));

      numbers[4] = Double.NaN;
      assertTrue(Double.isNaN(MathTools.min(numbers)));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testMax()
   {
      double[] numbers =
      {
         -1.0, -4.0, 4.0, 3.0, 0.0, 1.0, -2.0, -5.0, -3.0, 2.0, 2.0, 3.0, 5.0, 5.0
      };

      assertEquals(MathTools.max(numbers), 5.0, 1e-34);

      numbers[4] = Double.POSITIVE_INFINITY;
      assertTrue(Double.isInfinite(MathTools.max(numbers)));

      numbers[4] = Double.NEGATIVE_INFINITY;
      assertFalse(Double.isInfinite(MathTools.max(numbers)));

      numbers[4] = Double.NaN;
      assertTrue(Double.isNaN(MathTools.max(numbers)));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testMeanArray()
   {
      double numbers[] =
      {
         -1.0, -4.0, 4.0, 3.0, 0.0, 1.0, -2.0, -5.0, -3.0, 2.0, 2.0, 3.0, 5.0, 5.0
      };
      assertEquals(0.7143, MathTools.mean(numbers), 1e-4);

      assertEquals(5.0, MathTools.mean(new double[] {5.0}), 1e-34);

      numbers[4] = Double.POSITIVE_INFINITY;
      assertTrue(Double.isInfinite(MathTools.mean(numbers)));

      numbers[4] = Double.NEGATIVE_INFINITY;
      assertTrue(Double.isInfinite(MathTools.mean(numbers)));

      numbers[4] = Double.NaN;
      assertTrue(Double.isNaN(MathTools.mean(numbers)));

   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testMeanArrayList()
   {
      Double numbersArray[] =
      {
         -1.0, -4.0, 4.0, 3.0, 0.0, 1.0, -2.0, -5.0, -3.0, 2.0, 2.0, 3.0, 5.0, 5.0
      };
      ArrayList<Double> numbers = new ArrayList<Double>(Arrays.asList(numbersArray));
      assertEquals(0.7143, MathTools.mean(numbers), 1e-4);

      assertEquals(5.0, MathTools.mean(new double[] {5.0}), 1e-34);

      numbers.set(4, Double.POSITIVE_INFINITY);
      assertTrue(Double.isInfinite(MathTools.mean(numbers)));

      numbers.set(4, Double.NEGATIVE_INFINITY);
      assertTrue(Double.isInfinite(MathTools.mean(numbers)));

      numbers.set(4, Double.NaN);
      assertTrue(Double.isNaN(MathTools.mean(numbers)));

   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testCheckIfInRangeFalse()
   {
      MathTools.checkIfInRange(5.0, -3.0, 2.0);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testCheckIfInRangeTrue()
   {
      MathTools.checkIfInRange(1.0, -3.0, 2.0);
      MathTools.checkIfInRange(5.0, 5.0, 5.0);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testDiff()
   {
      double[] array = {45, -11, 7};
      double[] expectedReturn = {-56, 18};
      double[] actualReturn = MathTools.diff(array);
      assertEquals(expectedReturn[0], actualReturn[0], 1e-12);
      assertEquals(expectedReturn[1], actualReturn[1], 1e-12);

      double[] array2 = {-20, 1, -2.9};
      double[] expectedReturn2 = {21, -3.9};
      double[] actualReturn2 = MathTools.diff(array2);

      assertEquals(expectedReturn2[0], actualReturn2[0], 1e-12);
      assertEquals(expectedReturn2[1], actualReturn2[1], 1e-12);

   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testEpsilonEquals()
   {
      double v1 = 2.0;
      double v2 = 1.0;
      double epsilon = 3.0;
      boolean expectedReturn = true;
      boolean actualReturn = MathTools.epsilonEquals(v1, v2, epsilon);
      assertEquals(expectedReturn, actualReturn);

      v1 = Double.NaN;
      v2 = Double.NaN;
      epsilon = 3.0;
      expectedReturn = true;
      actualReturn = MathTools.epsilonEquals(v1, v2, epsilon);
      assertTrue(actualReturn);

      /** @todo fill in the test code */

      double v3 = 1.0;
      double v4 = 0.0;
      double epsi = 0.0;
      boolean expectedReturn2 = false;
      boolean actualReturn2 = MathTools.epsilonEquals(v3, v4, epsi);
      assertEquals(expectedReturn2, actualReturn2);

   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testPercentEquals()
   {
      double v1 = 1.0;
      double v2 = 1.099;
      double percent = 0.1;
      boolean expectedReturn = true;
      boolean actualReturn = MathTools.withinPercentEquals(v1, v2, percent);
      assertEquals(expectedReturn, actualReturn);

      v1 = 1.0;
      v2 = -1.0;
      percent = 0.01;
      expectedReturn = false;
      actualReturn = MathTools.withinPercentEquals(v1, v2, percent);
      assertEquals(expectedReturn, actualReturn);

      v1 = 1.0;
      v2 = 1.009999;
      percent = 0.01;
      expectedReturn = true;
      actualReturn = MathTools.withinPercentEquals(v1, v2, percent);
      assertEquals(expectedReturn, actualReturn);

      v1 = 1.0;
      v2 = 1.099;
      percent = 0.01;
      expectedReturn = false;
      actualReturn = MathTools.withinPercentEquals(v1, v2, percent);
      assertEquals(expectedReturn, actualReturn);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testDiffFrameVector()
   {
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      ArrayList<FrameVector> vectors = new ArrayList<FrameVector>();
      vectors.add(new FrameVector(worldFrame, 1.0, 2.0, 3.0));
      vectors.add(new FrameVector(worldFrame, 4.0, -2.0, 0.0));
      vectors.add(new FrameVector(worldFrame, 6.0, 2.0, -4.0));

      ArrayList<FrameVector> expectedReturn = new ArrayList<FrameVector>();
      expectedReturn.add(new FrameVector(worldFrame, 3.0, -4.0, -3.0));
      expectedReturn.add(new FrameVector(worldFrame, 2.0, 4.0, -4.0));

      ArrayList<FrameVector> actualReturn = MathTools.diff(vectors);

      for (int i = 0; i < 2; i++)
      {
         assertTrue(expectedReturn.get(i).epsilonEquals(actualReturn.get(i), 1e-12));
      }

   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testDiffFrameVectorDifferentFrames()
   {
      ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
      ReferenceFrame anotherFrame = ReferenceFrame.constructARootFrame("anotherFrame");
      ArrayList<FrameVector> vectors = new ArrayList<FrameVector>();
      vectors.add(new FrameVector(worldFrame, 1.0, 2.0, 3.0));
      vectors.add(new FrameVector(anotherFrame, 4.0, -2.0, 0.0));

      MathTools.diff(vectors);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testCheckIsEqual()
   {
      MathTools.checkIfEqual(1, 1);
      MathTools.checkIfEqual(-2, -2);

      MathTools.checkIfEqual(2.0, 2.001, 0.1);
      MathTools.checkIfEqual(-2.0, -2.001, 0.1);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testCheckIsEqualNaN()
   {
      MathTools.checkIfEqual(Double.NaN, Double.NaN, 1e-12);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testCheckIsEqualInt()
   {
      MathTools.checkIfEqual(2, 4);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsGreaterThan()
   {
      assertTrue(MathTools.isGreaterThan(2.00011000, 2.00010000, 8));
      assertFalse(MathTools.isGreaterThan(2.00011000, 2.00010000, 4));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsGreaterThanOrEqualTo()
   {
      assertTrue(MathTools.isGreaterThanOrEqualTo(2.00011000, 2.00010000, 8));
      assertTrue(MathTools.isGreaterThanOrEqualTo(2.00011000, 2.00010000, 4));
      assertTrue(MathTools.isGreaterThanOrEqualTo(2.00019000, 2.00020000, 4));
      assertFalse(MathTools.isGreaterThanOrEqualTo(2.00019000, 2.00020000, 5));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsLessThan()
   {
      assertFalse(MathTools.isLessThan(2.00011000, 2.00010000, 8));
      assertFalse(MathTools.isLessThan(2.00011000, 2.00010000, 4));
      assertFalse(MathTools.isLessThan(2.00019000, 2.00020000, 4));
      assertTrue(MathTools.isLessThan(2.00019000, 2.00020000, 5));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsLessThanOrEqualTo()
   {
      assertFalse(MathTools.isLessThanOrEqualTo(2.00011000, 2.00010000, 8));
      assertTrue(MathTools.isLessThanOrEqualTo(2.00011000, 2.00010000, 4));
      assertTrue(MathTools.isLessThanOrEqualTo(2.00019000, 2.00020000, 4));
      assertTrue(MathTools.isLessThanOrEqualTo(2.00019000, 2.00020000, 5));
   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testCheckIsEqualDouble()
   {
      MathTools.checkIfEqual(2.0, 2.001, 0.0001);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testGcd()
   {
      Random random = new Random(12890471L);
      for (int i = 0; i < 1000; i++)
      {
         long a = random.nextInt(Integer.MAX_VALUE);
         long b = random.nextInt(Integer.MAX_VALUE);

         long c = MathTools.gcd(a, b);

         assertTrue((a % c == 0) && (b % c == 0));
      }
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testLcm()
   {
      Random random = new Random(1240898L);
      for (int i = 0; i < 1000; i++)
      {
         long a = random.nextInt(Integer.MAX_VALUE);
         long b = random.nextInt(Integer.MAX_VALUE);

         long c = MathTools.lcm(a, b);

         assertTrue((c % a == 0) && (c % b == 0));
      }

      long c = MathTools.lcm(12, 18, 6, 3, 4);
      assertEquals(36, c);

   }

   @EstimatedDuration(duration = 0.0)
   @Test(
      timeout = 30000,
      expected = RuntimeException.class
   )
   public void testLcm_2()
   {
      Random rand = new Random();
      long c = MathTools.lcm(rand.nextLong());
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testArePointsInOrderColinear()
   {
      Point3d startPoint = new Point3d(0.0, 0.0, 0.0);
      Point3d middlePoint = new Point3d(1.0, 1.0, 1.0);
      Point3d endPoint = new Point3d(2.0, 2.0, 2.0);
      boolean expectedReturn = true;
      double epsilon = 1e-10;
      boolean actualReturn = GeometryTools.arePointsInOrderAndColinear(startPoint, middlePoint, endPoint, epsilon);
      assertEquals("return value", expectedReturn, actualReturn);

      /** @todo fill in the test code */
   }

   @EstimatedDuration(duration = 0.1)
   @Test(timeout = 30000)
   public void testArePointsInOrderColinear2()
   {
      Random random = new Random(100L);
      double scale = 10.0;

      // these points should pass
      int numberOfTests = 1000;
      for (int i = 0; i < numberOfTests; i++)
      {
         double x, y, z;
         x = scale * (random.nextDouble() - 0.5);
         y = scale * (random.nextDouble() - 0.5);
         z = scale * (random.nextDouble() - 0.5);
         Point3d start = new Point3d(x, y, z);

         x = scale * (random.nextDouble() - 0.5);
         y = scale * (random.nextDouble() - 0.5);
         z = scale * (random.nextDouble() - 0.5);
         Point3d end = new Point3d(x, y, z);

         Vector3d startToEnd = new Vector3d(end);
         startToEnd.sub(start);

         double epsilon = 1e-10;
         if (startToEnd.length() < epsilon)
            continue;
         else
         {
            for (int j = 0; j < numberOfTests; j++)
            {
               double percentAlong = 0.99 * random.nextDouble();
               Vector3d adder = new Vector3d(startToEnd);
               adder.scale(percentAlong);

               Point3d middle = new Point3d(start);
               middle.add(adder);

               boolean inOrder = GeometryTools.arePointsInOrderAndColinear(start, middle, end, epsilon);
               if (!inOrder)
               {
                  fail("FAILED: start=" + start + ", middle=" + middle + ", end=" + end);
               }
            }
         }
      }
   }

// @EstimatedDuration
// @Test(timeout = 300000)
// public void testDiff1()
// {
//   ArrayList array = null;
//   MathTools.diff(array);
//
//   ArrayList expectedReturn = array;
//   ArrayList actualReturn = mathTools.diff(array);
//   assertTrue("Test Failed", actualReturn[0] == expectedReturn[0]
//              && actualReturn[1] == expectedReturn[1]);
// }

// @EstimatedDuration
// @Test(timeout = 300000)
// public void testDiffWithAlphaFilter()
// {
//    ArrayList array = null;
//    double alpha = 0.0;
//    double dt = 0.0;
//    ArrayList expectedReturn = null;
//    ArrayList actualReturn = mathTools.diffWithAlphaFilter(array, alpha, dt);
//    assertEquals("return value", expectedReturn, actualReturn);
//
// }

// @EstimatedDuration
// @Test(timeout = 300000)
// public void testGetQuaternionFromTransform3D()
// {
//    Transform3D transform3D = null;
//    Quat4d q1 = null;
//    mathTools.getQuaternionFromTransform3D(transform3D, q1);
//
// }

// @EstimatedDuration
// @Test(timeout = 300000)
// public void testLoadTransform() throws IOException
// {
//    BufferedReader bufferedReader = null;
//    Transform3D expectedReturn = null;
//    Transform3D actualReturn = mathTools.loadTransform(bufferedReader);
//    assertEquals("return value", expectedReturn, actualReturn);
//
// }

// @EstimatedDuration
// @Test(timeout = 300000)
// public void testSaveTransform()
// {
//    Transform3D transform3D = null;
//    PrintWriter printWriter = null;
//    mathTools.saveTransform(transform3D, printWriter);
//
// }

// @EstimatedDuration
// @Test(timeout = 300000)
// public void testSplitArrayIntoEqualishParts()
// {
//    ArrayList array = null;
//    int numberOfParts = 0;
//    ArrayList expectedReturn = null;
//    ArrayList actualReturn = mathTools.splitArrayIntoEqualishParts(array, numberOfParts);
//    assertEquals("return value", expectedReturn, actualReturn);
// }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testProjectionOntoPlane()
   {
      // test by projecting on plane spanning x,y through z=0.1

      Vector3d p1 = new Vector3d(Math.random(), Math.random(), 0.1);
      Vector3d p2 = new Vector3d(Math.random(), Math.random(), 0.1);
      Vector3d p3 = new Vector3d(Math.random(), Math.random(), 0.1);

      Vector3d p = new Vector3d(Math.random(), Math.random(), Math.random());

      Vector3d proj = GeometryTools.getProjectionOntoPlane(p1, p2, p3, p);

      assertEquals(p.getX(), proj.getX(), Double.MIN_VALUE);
      assertEquals(p.getY(), proj.getY(), Double.MIN_VALUE);
      assertEquals(0.1, proj.getZ(), 10e-10);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testRoundToGivenPrecision()
   {
      double longDouble = 0.12345678910111213;

      double roundedNumber = MathTools.roundToGivenPrecision(longDouble, 1e-7);
      assertEquals(roundedNumber, 0.1234567, 1e-14);

      roundedNumber = MathTools.roundToGivenPrecision(longDouble, 1e-3);
      assertEquals(roundedNumber, 0.123, 1e-14);

      Vector3d preciseVector = new Vector3d(0.12345678910111213, 100.12345678910111213, 1000.12345678910111213);
      Vector3d roundedVector = new Vector3d(preciseVector);

      MathTools.roundToGivenPrecision(roundedVector, 1e-7);
      JUnitTools.assertTuple3dEquals(new Vector3d(0.1234567, 100.1234567, 1000.1234567), roundedVector, 1e-12);

      MathTools.roundToGivenPrecision(roundedVector, 1e-3);
      JUnitTools.assertTuple3dEquals(new Vector3d(0.123, 100.123, 1000.123), roundedVector, 1e-14);
   }

   @EstimatedDuration(duration = 0.0)
   @Test(timeout = 30000)
   public void testIsFinite()
   {
      Random rand = new Random();
      for (int i = 0; i < 1000; i++)
      {
         assertTrue(MathTools.isFinite(rand.nextFloat() * 1000));
         assertFalse(MathTools.isFinite(rand.nextFloat() / 0.0));

         assertTrue(MathTools.isFinite(new Vector3d(rand.nextDouble() * 1000, rand.nextDouble() * 1000, rand.nextDouble() * 1000)));
         assertFalse(MathTools.isFinite(new Vector3d(rand.nextDouble() / 0.0, rand.nextDouble() / 0.0, rand.nextDouble() / 0.0)));

      }

   }


}
