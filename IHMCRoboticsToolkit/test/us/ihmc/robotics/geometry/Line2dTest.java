package us.ihmc.robotics.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.robotics.MathTools;
import us.ihmc.tools.testing.MutationTestingTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;

public class Line2dTest
{
   private static final int ITERATIONS = 1000;
   private static final double maxRandomValue = 1.0e5;

   private double randomDouble(Random random, double maxRandomValue)
   {
      return random.nextDouble() * maxRandomValue * 2.0 - maxRandomValue;
   }
   
   private double randomDouble(Random random)
   {
      return randomDouble(random, maxRandomValue);
   }

   private Point2d randomPoint(Random random)
   {
      return new Point2d(randomDouble(random, 1.0e5), randomDouble(random, 1.0e5));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testConstructors()
   {
      double delta = 1.0e-5;

      Random random = new Random(1000L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Vector2d vector = new Vector2d(secondPointOnLine.getX() - firstPointOnLine.getX(), secondPointOnLine.getY() - firstPointOnLine.getY());
         Line2d line2dByPointVector = new Line2d(firstPointOnLine, vector);
         Line2d line2dByPointPoint = new Line2d(firstPointOnLine, secondPointOnLine);

         assertEquals(line2dByPointPoint.getPoint(), line2dByPointVector.getPoint());
         assertEquals(line2dByPointPoint.getNormalizedVector().getX(), line2dByPointVector.getNormalizedVector().getX(), delta);
         assertEquals(line2dByPointPoint.getNormalizedVector().getY(), line2dByPointVector.getNormalizedVector().getY(), delta);

         Line2d line2dByCopy = new Line2d(line2dByPointVector);
         assertFalse(line2dByPointVector == line2dByCopy);
         assertEquals(line2dByPointVector.getPoint(), line2dByCopy.getPoint());
         assertEquals(line2dByPointVector.getNormalizedVector().getX(), line2dByCopy.getNormalizedVector().getX(), delta);
         assertEquals(line2dByPointVector.getNormalizedVector().getY(), line2dByCopy.getNormalizedVector().getY(), delta);

      }

   }
   
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testIsPointInFrontOfLine2d()
   {
      Line2d line2d = new Line2d();
      Point2d point1 = new Point2d();
      Point2d point2 = new Point2d();
      Point2d point3 = new Point2d();
      Vector2d frontDirection = new Vector2d();
      
      point1.set(0.0, 0.0);
      point2.set(1.0, 1.0);
      line2d.set(point1, point2);
      frontDirection.set(0.0, 1.0);
      
      point3.set(0.0, 1.0);
      assertEquals("not equal", true, line2d.isPointInFrontOfLine(frontDirection, point3));
      
      point3.set(0.0, -1.0);
      assertEquals("not equal", false, line2d.isPointInFrontOfLine(frontDirection, point3));
      
      point1.set(0.0, 0.0);
      point2.set(-1.0, 1.0);
      line2d.set(point1, point2);
      
      point3.set(0.0, 1.0);
      assertEquals("not equal", true, line2d.isPointInFrontOfLine(frontDirection, point3));
      
      point3.set(0.0, -1.0);
      assertEquals("not equal", false, line2d.isPointInFrontOfLine(frontDirection, point3));
      
      frontDirection.set(0.0, -1.0);
      
      point3.set(0.0, 1.0);
      assertEquals("not equal", false, line2d.isPointInFrontOfLine(frontDirection, point3));
      
      point3.set(0.0, -1.0);
      assertEquals("not equal", true, line2d.isPointInFrontOfLine(frontDirection, point3));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testPointVectorConstructorForException()
   {
      // TODO: Test this at various random points, or is this sufficient?
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Vector2d vector = new Vector2d(0.0, 0.0);
      new Line2d(firstPointOnLine, vector);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testPointPointConstructorForException()
   {
      // TODO: Test this at various random points, or is this sufficient?
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      new Line2d(firstPointOnLine, firstPointOnLine);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetPoint()
   {
      // TODO: Test this at various random points, or is this sufficient?
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      assertEquals(firstPointOnLine, line2d.getPoint());
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetNormalizedVector()
   {
      double delta = 1.0e-5;

      Random random = new Random(789L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         double xdiff = secondPointOnLine.getX() - firstPointOnLine.getX();
         double ydiff = secondPointOnLine.getY() - firstPointOnLine.getY();
         double length = Math.sqrt(MathTools.square(xdiff) + MathTools.square(ydiff));
         assertEquals(xdiff / length, line2d.getNormalizedVector().getX(), delta);
         assertEquals(ydiff / length, line2d.getNormalizedVector().getY(), delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetNormalizedVectorCopy()
   {
      double delta = 1.0e-5;

      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

      Vector2d normalizedVector = new Vector2d(Math.sqrt(2.0) / 2.0, Math.sqrt(2.0) / 2.0);
      assertEquals(normalizedVector.getX(), line2d.getNormalizedVector().getX(), delta);
      assertEquals(normalizedVector.getY(), line2d.getNormalizedVector().getY(), delta);
      Vector2d normalizedVector2 = new Vector2d();
      line2d.getNormalizedVector(normalizedVector2);
      assertFalse(line2d.getNormalizedVector() == normalizedVector2);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetSlope()
   {
      Random random = new Random(2048L);
      double delta = 1.0e-5;

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         double slope = (secondPointOnLine.getY() - firstPointOnLine.getY()) / (secondPointOnLine.getX() - firstPointOnLine.getX());
         assertEquals(slope, line2d.getSlope(), delta);
      }

      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(0.0, 5.0);
      Line2d verticalLine = new Line2d(firstPointOnLine, secondPointOnLine);
      assertEquals(Double.POSITIVE_INFINITY, verticalLine.getSlope(), delta);

      secondPointOnLine = new Point2d(0.0, -5.0);
      Line2d horizontalLine = new Line2d(firstPointOnLine, secondPointOnLine);
      assertEquals(Double.NEGATIVE_INFINITY, horizontalLine.getSlope(), delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetXIntercept()
   {
      double delta = 1.0e-5;
      Random random = new Random(1886L);
      for (int i = 0; i < 1000; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         double slope = (secondPointOnLine.getY() - firstPointOnLine.getY()) / (secondPointOnLine.getX() - firstPointOnLine.getX());
         double additive = firstPointOnLine.getY() - slope * firstPointOnLine.getX();
         assertEquals(-additive / slope, line2d.getXIntercept(), delta);
      }

      // Edge cases: on top of x-axis and parallel to x-axis
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(5.0, 0.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      assertEquals(Double.NaN, line2d.getXIntercept(), delta);

      firstPointOnLine = new Point2d(1.0, 1.0);
      secondPointOnLine = new Point2d(2.0, 1.0);
      line2d.set(firstPointOnLine, secondPointOnLine);
      assertEquals(Double.NEGATIVE_INFINITY, line2d.getXIntercept(), delta);

      line2d.set(secondPointOnLine, firstPointOnLine);
      assertEquals(Double.POSITIVE_INFINITY, line2d.getXIntercept(), delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetYIntercept()
   {
      double delta = 1.0e-5;
      Random random = new Random(1972L);
      for (int i = 0; i < 1000; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         double slope = (secondPointOnLine.getY() - firstPointOnLine.getY()) / (secondPointOnLine.getX() - firstPointOnLine.getX());
         double additive = firstPointOnLine.getY() - slope * firstPointOnLine.getX();
         assertEquals(additive, line2d.getYIntercept(), delta);
      }

      // Edge cases: on top of x-axis and parallel to x-axis
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(0.0, 5.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

      assertEquals(Double.NaN, line2d.getYIntercept(), delta);

      firstPointOnLine = new Point2d(1.0, 1.0);
      secondPointOnLine = new Point2d(1.0, 2.0);
      line2d.set(firstPointOnLine, secondPointOnLine);
      assertEquals(Double.NEGATIVE_INFINITY, line2d.getYIntercept(), delta);

      line2d.set(secondPointOnLine, firstPointOnLine);
      assertEquals(Double.POSITIVE_INFINITY, line2d.getYIntercept(), delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testNegateDirection()
   {
      double delta = 1.0e-5;

      Random random = new Random(2036L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d normalizedVectorCopy = new Vector2d();
         line2d.getNormalizedVector(normalizedVectorCopy);
         line2d.negateDirection();
         assertEquals(-normalizedVectorCopy.getX(), line2d.getNormalizedVector().getX(), delta);
         assertEquals(-normalizedVectorCopy.getY(), line2d.getNormalizedVector().getY(), delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testNegateDirectionCopy()
   {
      double delta = 1.0e-5;

      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      Line2d line2dNegated = line2d.negateDirectionCopy();
      line2d.negateDirection();
      assertEquals(line2d.getNormalizedVector().getX(), line2dNegated.getNormalizedVector().getX(), delta);
      assertEquals(line2d.getNormalizedVector().getY(), line2dNegated.getNormalizedVector().getY(), delta);
      assertFalse(line2d == line2dNegated);
      assertFalse(line2d.getNormalizedVector() == line2dNegated.getNormalizedVector());
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetPointPoint()
   {
      double delta = 1.0e-5;

      Random random = new Random(9999L);
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      for (int i = 0; i < ITERATIONS; i++)
      {
         firstPointOnLine = randomPoint(random);
         secondPointOnLine = randomPoint(random);
         line2d.set(firstPointOnLine, secondPointOnLine);
         assertFalse(firstPointOnLine == line2d.getPoint());
         assertEquals(firstPointOnLine, line2d.getPoint());
         double xdiff = secondPointOnLine.getX() - firstPointOnLine.getX();
         double ydiff = secondPointOnLine.getY() - firstPointOnLine.getY();
         double length = Math.sqrt(MathTools.square(xdiff) + MathTools.square(ydiff));

         assertEquals(xdiff / length, line2d.getNormalizedVector().getX(), delta);
         assertEquals(ydiff / length, line2d.getNormalizedVector().getY(), delta);

      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSetPointPointException()
   {
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      line2d.set(firstPointOnLine, firstPointOnLine);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetPointArray()
   {
      double delta = 1.0e-5;

      Random random = new Random(1444L);
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      for (int i = 0; i < ITERATIONS; i++)
      {
         firstPointOnLine = randomPoint(random);
         secondPointOnLine = randomPoint(random);
         Point2d[] points = {firstPointOnLine, secondPointOnLine};
         line2d.set(points);
         assertFalse(firstPointOnLine == line2d.getPoint());
         assertEquals(firstPointOnLine, line2d.getPoint());
         double xdiff = secondPointOnLine.getX() - firstPointOnLine.getX();
         double ydiff = secondPointOnLine.getY() - firstPointOnLine.getY();
         double length = Math.sqrt(MathTools.square(xdiff) + MathTools.square(ydiff));

         assertEquals(xdiff / length, line2d.getNormalizedVector().getX(), delta);
         assertEquals(ydiff / length, line2d.getNormalizedVector().getY(), delta);

      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetPointArrayExceptions()
   {
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      Point2d[] points = {firstPointOnLine};
      try
      {
         line2d.set(points);
         fail("Failed to throw exception for single point.");
      }
      catch (RuntimeException exception)
      {
      }

      points = new Point2d[2];
      points[0] = firstPointOnLine;
      points[1] = firstPointOnLine;

      try
      {
         line2d.set(points);
         fail("Failed to throw exception for two equal points.");
      }
      catch (RuntimeException exception)
      {
      }

      points = new Point2d[3];
      points[0] = firstPointOnLine;
      points[1] = secondPointOnLine;
      points[2] = randomPoint(new Random(8282L));

      try
      {
         line2d.set(points);
         fail("Failed to throw exception for too many points.");
      }
      catch (RuntimeException exception)
      {
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetLine()
   {
      // TODO: Is it necessary to do random tests here?
      
      double delta = 1.0e-5;

      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

      Point2d newFirstPointOnLine = new Point2d(10.0, 15.0);
      Point2d newSecondPointOnLine = new Point2d(15.0, 8.0);
      Line2d secondLine2d = new Line2d(newFirstPointOnLine, newSecondPointOnLine);

      line2d.set(secondLine2d);
      assertFalse(secondLine2d == line2d);
      assertEquals(secondLine2d.getPoint().getX(), line2d.getPoint().getX(), delta);
      assertEquals(secondLine2d.getPoint().getY(), line2d.getPoint().getY(), delta);
      assertFalse(secondLine2d.getPoint() == line2d.getPoint());

      assertEquals(secondLine2d.getNormalizedVector().getX(), line2d.getNormalizedVector().getX(), delta);
      assertEquals(secondLine2d.getNormalizedVector().getY(), line2d.getNormalizedVector().getY(), delta);
      assertFalse(secondLine2d.getNormalizedVector() == line2d.getNormalizedVector());
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSetDoubleException()
   {
      // TODO: Failing test case ignored. Should throw an exception like the Point2d, Vector2d constructor
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

      line2d.set(5.0, 6.0, 0.0, 0.0);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetPoint2d()
   {
      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      Point2d newPoint = new Point2d(11.0, 9.0);
      line2d.setPoint2d(newPoint);

      assertEquals(newPoint, line2d.getPoint());
      assertFalse(newPoint == line2d.getPoint());
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testRotate()
   {
      double delta = 1.0e-5;
      Random random = new Random(777L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = new Point2d(0.0, 0.0);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         double angle = Math.atan2(line2d.getNormalizedVector().getY(), line2d.getNormalizedVector().getX());
         double rotation = randomDouble(random, 2.0 * Math.PI);
         double newAngle = angle + rotation;
         line2d.rotate(rotation);

         assertEquals(Math.cos(newAngle), line2d.getNormalizedVector().getX(), delta);
         assertEquals(Math.sin(newAngle), line2d.getNormalizedVector().getY(), delta);
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testShiftToLeftAndRight()
   {
      double distanceToShift = 0.2;
      double epsilon = 1e-7;

      // Pointing straight up:
      Line2d line = new Line2d(0.0, 0.0, 0.0, 1.0);
      Line2d shiftedLine = new Line2d(line);
      shiftedLine.shiftToRight(distanceToShift);

      Point2d shiftedLineOrigin = shiftedLine.getPoint();
      Vector2d lineVector = line.getNormalizedVector();
      Vector2d shiftedLineVector = shiftedLine.getNormalizedVector();

      assertEquals(distanceToShift, shiftedLineOrigin.getX(), epsilon);
      assertEquals(0.0, shiftedLineOrigin.getY(), epsilon);
      assertEquals(lineVector.getX(), shiftedLineVector.getX(), epsilon);
      assertEquals(lineVector.getY(), shiftedLineVector.getY(), epsilon);

      shiftedLine.set(line);
      shiftedLine.shiftToLeft(distanceToShift);

      shiftedLineOrigin = shiftedLine.getPoint();
      lineVector = line.getNormalizedVector();
      shiftedLineVector = shiftedLine.getNormalizedVector();

      assertEquals(-distanceToShift, shiftedLineOrigin.getX(), epsilon);
      assertEquals(0.0, shiftedLineOrigin.getY(), epsilon);
      assertEquals(lineVector.getX(), shiftedLineVector.getX(), epsilon);
      assertEquals(lineVector.getY(), shiftedLineVector.getY(), epsilon);

      // Pointing straight along x:
      line = new Line2d(0.0, 0.0, 1.0, 0.0);
      shiftedLine.set(line);
      shiftedLine.shiftToRight(distanceToShift);

      shiftedLineOrigin = shiftedLine.getPoint();
      lineVector = line.getNormalizedVector();
      shiftedLineVector = shiftedLine.getNormalizedVector();

      assertEquals(0.0, shiftedLineOrigin.getX(), epsilon);
      assertEquals(-distanceToShift, shiftedLineOrigin.getY(), epsilon);
      assertEquals(lineVector.getX(), shiftedLineVector.getX(), epsilon);
      assertEquals(lineVector.getY(), shiftedLineVector.getY(), epsilon);

      shiftedLine.set(line);
      shiftedLine.shiftToLeft(distanceToShift);

      shiftedLineOrigin = shiftedLine.getPoint();
      lineVector = line.getNormalizedVector();
      shiftedLineVector = shiftedLine.getNormalizedVector();

      assertEquals(0.0, shiftedLineOrigin.getX(), epsilon);
      assertEquals(distanceToShift, shiftedLineOrigin.getY(), epsilon);
      assertEquals(lineVector.getX(), shiftedLineVector.getX(), epsilon);
      assertEquals(lineVector.getY(), shiftedLineVector.getY(), epsilon);

      // Pointing at (1,1)
      line = new Line2d(0.0, 0.0, 1.0, 1.0);
      shiftedLine.set(line);
      shiftedLine.shiftToRight(distanceToShift);

      double distanceAtFortyFiveDegrees = distanceToShift * Math.sqrt(2.0) / 2.0;

      shiftedLineOrigin = shiftedLine.getPoint();
      lineVector = line.getNormalizedVector();
      shiftedLineVector = shiftedLine.getNormalizedVector();

      assertEquals(distanceAtFortyFiveDegrees, shiftedLineOrigin.getX(), epsilon);
      assertEquals(-distanceAtFortyFiveDegrees, shiftedLineOrigin.getY(), epsilon);
      assertEquals(lineVector.getX(), shiftedLineVector.getX(), epsilon);
      assertEquals(lineVector.getY(), shiftedLineVector.getY(), epsilon);

      shiftedLine.set(line);
      shiftedLine.shiftToLeft(distanceToShift);

      shiftedLineOrigin = shiftedLine.getPoint();
      lineVector = line.getNormalizedVector();
      shiftedLineVector = shiftedLine.getNormalizedVector();

      assertEquals(-distanceAtFortyFiveDegrees, shiftedLineOrigin.getX(), epsilon);
      assertEquals(distanceAtFortyFiveDegrees, shiftedLineOrigin.getY(), epsilon);
      assertEquals(lineVector.getX(), shiftedLineVector.getX(), epsilon);
      assertEquals(lineVector.getY(), shiftedLineVector.getY(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testInteriorBisector()
   {
      Random random = new Random(1982);
      double delta = 1.0e-5;

      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 0.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

      Line2d secondLine2d = new Line2d(line2d);

      Line2d interiorBisector = line2d.interiorBisector(secondLine2d);

      assertEquals(line2d.getPoint(), interiorBisector.getPoint());
      assertEquals(line2d.getNormalizedVector().getX(), interiorBisector.getNormalizedVector().getX(), delta);
      assertEquals(line2d.getNormalizedVector().getY(), interiorBisector.getNormalizedVector().getY(), delta);

      Line2d parallelLine2d = new Line2d(line2d);
      parallelLine2d.setPoint2d(new Point2d(5.5, 18));
      assertNull(line2d.interiorBisector(parallelLine2d));

      for (int i = 0; i < ITERATIONS; i++)
      {
         line2d.set(randomDouble(random, 10.0), randomDouble(random, 10.0), randomDouble(random, 10.0), randomDouble(random, 10.0));
         secondLine2d.set(randomDouble(random, 10.0), randomDouble(random, 10.0), randomDouble(random, 10.0), randomDouble(random, 10.0));
         interiorBisector = line2d.interiorBisector(secondLine2d);

         double tangent1 = line2d.getSlope();
         double additive1 = line2d.getPoint().getY() - tangent1 * line2d.getPoint().getX();
         double tangent2 = secondLine2d.getSlope();
         double additive2 = secondLine2d.getPoint().getY() - tangent2 * secondLine2d.getPoint().getX();

         double intersectX = (additive2 - additive1) / (tangent1 - tangent2);
         double intersectY = tangent1 * intersectX + additive1;

         assertEquals(intersectX, interiorBisector.getPoint().getX(), delta);
         assertEquals(intersectY, interiorBisector.getPoint().getY(), delta);

         Vector2d interiorNormalizedVector = line2d.getNormalizedVector();
         Vector2d vector = new Vector2d();
         secondLine2d.getNormalizedVector(vector);
         interiorNormalizedVector.add(vector);
         double length = Math.sqrt(MathTools.square(interiorNormalizedVector.getX()) + MathTools.square(interiorNormalizedVector.getY()));
         assertEquals(interiorNormalizedVector.getX() / length, interiorBisector.getNormalizedVector().getX(), delta);
         assertEquals(interiorNormalizedVector.getY() / length, interiorBisector.getNormalizedVector().getY(), delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testPerpendicularVector()
   {
      double delta = 1.0e-5;

      Random random = new Random(1984L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d perpendicular = line2d.perpendicularVector();

         assertEquals(0.0, perpendicular.getX() * line2d.getNormalizedVector().getX() + perpendicular.getY() * line2d.getNormalizedVector().getY(), delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.1, targets = TestPlanTarget.Exclude)
	@Test(timeout=300000)
   public void testContainsEpsilon()
   {
      double epsilon = 1e-1;
      Point2d firstPointOnLine = new Point2d(3.0, 4.0);
      Point2d secondPointOnLine = new Point2d(8.0, 6.0);
      System.out.println(secondPointOnLine);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
      Vector2d perpendicular = line2d.perpendicularVector();
      System.out.println(perpendicular);

      assertTrue(line2d.containsEpsilon(secondPointOnLine, epsilon));
      assertTrue(line2d.containsEpsilon(firstPointOnLine, epsilon));

      Point2d pointNearLine = (Point2d) secondPointOnLine.clone();
      pointNearLine.scaleAdd(epsilon / 2.0, perpendicular, secondPointOnLine);
      System.out.println(pointNearLine);
      assertTrue(line2d.containsEpsilon(pointNearLine, epsilon));

      pointNearLine.scaleAdd(epsilon, perpendicular, secondPointOnLine);
      System.out.println(pointNearLine);
      assertTrue(line2d.containsEpsilon(pointNearLine, epsilon));

      // TODO: This test fails; seems a Math.sqrt is missing, or epsilon should be renamed epsilonSquared
      pointNearLine.scaleAdd(epsilon * 2.0, perpendicular, secondPointOnLine);
      System.out.println(pointNearLine);
      System.out.println(pointNearLine.distance(secondPointOnLine));
      assertFalse(line2d.containsEpsilon(pointNearLine, epsilon));

      pointNearLine.scaleAdd(-epsilon / 2.0, perpendicular, secondPointOnLine);
      System.out.println(pointNearLine);
      assertTrue(line2d.containsEpsilon(pointNearLine, epsilon));

      pointNearLine.scaleAdd(-epsilon, perpendicular, secondPointOnLine);
      System.out.println(pointNearLine);
      assertTrue(line2d.containsEpsilon(pointNearLine, epsilon));

      pointNearLine.scaleAdd(-epsilon * 2.0, perpendicular, secondPointOnLine);
      System.out.println(pointNearLine);
      assertFalse(line2d.containsEpsilon(pointNearLine, epsilon));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testPerpendicularLineThroughPoint()
   {
      double delta = 1.0e-5;
      Random random = new Random(8888L);

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Point2d pointOnPerpendicularLine = randomPoint(random);
         Line2d perpendicularLine = line2d.perpendicularLineThroughPoint(pointOnPerpendicularLine);

         assertTrue(perpendicularLine.containsEpsilon(pointOnPerpendicularLine, delta));
         assertEquals(0.0,
                      perpendicularLine.getNormalizedVector().getX() * line2d.getNormalizedVector().getX()
                      + perpendicularLine.getNormalizedVector().getY() * line2d.getNormalizedVector().getY(), delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testOrthogonalProjection()
   {
      double delta = 1.0e-5;
      Random random = new Random(2000L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d normalizedVector = line2d.getNormalizedVector();
         Vector2d perpendicular = line2d.perpendicularVector();

         Point2d pointOnLine = new Point2d(firstPointOnLine);
         double distance = randomDouble(random, 10.0);
         double perpendicularDistance = randomDouble(random, 10.0);
         pointOnLine.x = pointOnLine.x + distance * normalizedVector.x;
         pointOnLine.y = pointOnLine.y + distance * normalizedVector.y;
         Point2d pointOffLine = new Point2d(pointOnLine);
         pointOffLine.x = pointOffLine.x + perpendicularDistance * perpendicular.x;
         pointOffLine.y = pointOffLine.y + perpendicularDistance * perpendicular.y;

         line2d.orthogonalProjection(pointOffLine);

         assertEquals(pointOnLine.x, pointOffLine.x, delta);
         assertEquals(pointOnLine.y, pointOffLine.y, delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testOrthogonalProjectionCopy()
   {
      double delta = 1.0e-5;
      Random random = new Random(1111L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d normalizedVector = line2d.getNormalizedVector();
         Vector2d perpendicular = line2d.perpendicularVector();

         Point2d pointOnLine = new Point2d(firstPointOnLine);
         double distance = randomDouble(random, 10.0);
         double perpendicularDistance = randomDouble(random, 10.0);
         pointOnLine.x = pointOnLine.x + distance * normalizedVector.x;
         pointOnLine.y = pointOnLine.y + distance * normalizedVector.y;
         Point2d pointOffLine = new Point2d(pointOnLine);
         pointOffLine.x = pointOffLine.x + perpendicularDistance * perpendicular.x;
         pointOffLine.y = pointOffLine.y + perpendicularDistance * perpendicular.y;

         Point2d orthogonalCopy = line2d.orthogonalProjectionCopy(pointOffLine);

         assertEquals(pointOnLine.x, orthogonalCopy.x, delta);
         assertEquals(pointOnLine.y, orthogonalCopy.y, delta);
         assertNotSame(pointOnLine, orthogonalCopy);
         assertNotSame(pointOffLine, orthogonalCopy);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIntersectionWithLineSegment2d()
   {
      Random random = new Random(3333L);
      double delta = 1.0e-5;

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPoint = randomPoint(random);
         Point2d secondPoint = randomPoint(random);
         LineSegment2d lineSegment2d = new LineSegment2d(firstPoint, secondPoint);

         Line2d colinearLine2d = new Line2d(firstPoint, secondPoint);

         // TODO: Sometimes fails.
//       assertNull(colinearLine2d.intersectionWith(lineSegment2d));

         Line2d parallelLine2d = new Line2d(colinearLine2d);
         double distance = randomDouble(random, 10.0);
         parallelLine2d.getPoint().scaleAdd(distance, parallelLine2d.perpendicularVector(), parallelLine2d.getPoint());
         assertNull(parallelLine2d.intersectionWith(lineSegment2d));

         Vector2d direction = new Vector2d(randomDouble(random, 10.0), randomDouble(random, 10.0));
         Line2d lineThroughEndPoint = new Line2d(firstPoint, direction);
         Point2d intersection = lineThroughEndPoint.intersectionWith(lineSegment2d);
         assertEquals(firstPoint.x, intersection.x, delta);
         assertEquals(firstPoint.y, intersection.y, delta);
         lineThroughEndPoint.setPoint2d(secondPoint);
         intersection = lineThroughEndPoint.intersectionWith(lineSegment2d);

         // TODO intersection is null, which is unexpected.
//       assertEquals(secondPoint.x, intersection.x, delta);
//       assertEquals(secondPoint.y, intersection.y, delta);

         Point2d midPoint = new Point2d();
         midPoint.add(firstPoint, secondPoint);
         midPoint.scale(0.5);

         Line2d intersectingLine = new Line2d(midPoint, direction);
         intersection = intersectingLine.intersectionWith(lineSegment2d);
         assertEquals(midPoint.x, intersection.x, delta);
         assertEquals(midPoint.y, intersection.y, delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testInterSectionWithLine2d()
   {
      Random random = new Random(767L);
      double delta = 1.0e-3;

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnFirstLine = randomPoint(random);
         Point2d secondPointOnFirstLine = randomPoint(random);
         Vector2d directionSecondLine = new Vector2d(randomDouble(random, 10.0), randomDouble(random, 10.0));

         Line2d firstLine = new Line2d(firstPointOnFirstLine, secondPointOnFirstLine);
         Line2d secondLine = new Line2d(secondPointOnFirstLine, directionSecondLine);

         Point2d intersectionFirstWithSecond = firstLine.intersectionWith(secondLine);
         Point2d intersectionSecondWithFirst = secondLine.intersectionWith(firstLine);

         assertEquals(secondPointOnFirstLine.x, intersectionFirstWithSecond.x, delta);
         assertEquals(secondPointOnFirstLine.y, intersectionFirstWithSecond.y, delta);

         assertEquals(secondPointOnFirstLine.x, intersectionSecondWithFirst.x, delta);
         assertEquals(secondPointOnFirstLine.y, intersectionSecondWithFirst.y, delta);

         Line2d firstLineCopy = new Line2d(firstLine);
         Point2d intersectionWithParallel = firstLine.intersectionWith(firstLineCopy);
         assertNull(intersectionWithParallel);

         double distance = randomDouble(random, 10.0);
         Vector2d perpendicular = firstLine.perpendicularVector();
         Point2d newPoint = new Point2d(firstLine.getPoint());
         newPoint.scaleAdd(distance, perpendicular, newPoint);
         firstLineCopy.setPoint2d(newPoint);

         intersectionWithParallel = firstLine.intersectionWith(firstLineCopy);
         assertNull(intersectionWithParallel);
         intersectionWithParallel = firstLineCopy.intersectionWith(firstLine);
         assertNull(intersectionWithParallel);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistancePoint2d()
   {
      Random random = new Random(743L);
      double delta = 1.0e-3;

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Point2d distantPoint = randomPoint(random);

         double calculatedDistance = line2d.distance(distantPoint);

         Line2d orthogonalLine = line2d.perpendicularLineThroughPoint(firstPointOnLine);
         Point2d orthogonalProjection = orthogonalLine.orthogonalProjectionCopy(distantPoint);
         double xdiff = orthogonalProjection.x - firstPointOnLine.x;
         double ydiff = orthogonalProjection.y - firstPointOnLine.y;
         double distance = Math.sqrt(xdiff * xdiff + ydiff * ydiff);

         assertEquals(distance, calculatedDistance, delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.1, targets = TestPlanTarget.Exclude)
	@Test(timeout=300000)
   public void testDistanceLine2d()
   {
      // TODO Write actual test cases, then write the code. Requires definitions.
      fail("Has not been implemented yet.");
   }

	@DeployableTestMethod(estimatedDuration = 0.1, targets = TestPlanTarget.Exclude)
	@Test(timeout=300000)
   public void testDistanceLineSegment2d()
   {
      // TODO Write actual test cases, then write the code. Requires definitions.
      fail("Has not been implemented yet.");
   }

	@DeployableTestMethod(estimatedDuration = 0.1, targets = TestPlanTarget.Exclude)
	@Test(timeout=300000)
   public void testDistanceConvexPolygon2d()
   {
      // TODO Write actual test cases, then write the code. Requires definitions.
      fail("Has not been implemented yet.");
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void isPointOnLineTest()
   {
      Point2d point1 = new Point2d(1.0, 1.0);
      Point2d point2 = new Point2d(5.0, 5.0);
      
      Point2d point3 = new Point2d(2.2,3.3);
      Point2d point4 = new Point2d(10,10);
      
      Line2d line = new Line2d(point1, point2);
      
      assertFalse(line.isPointOnLine(point3));
      assertTrue(line.isPointOnLine(point4));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void areLinesPerpendicularTest()
   {
      Point2d point1 = new Point2d(1.0, 1.0);
      Point2d point2 = new Point2d(5.0, 5.0);
      
      Point2d point3 = new Point2d(2.2,3.3);
      Point2d point4 = new Point2d(10,10);
      
      Point2d point5 = new Point2d(-2,4);
      Point2d point5UnProjected = new Point2d(point5);
      
      Line2d line1 = new Line2d(point1,point2);
      Line2d line2 = new Line2d(point3,point4);
      
      line1.orthogonalProjection(point5); //project point5 onto line1, this ensures we have a point for creating a perpendicular line
      
      Line2d line3 = new Line2d(point5, point5UnProjected);
      
      assertFalse(line1.areLinesPerpendicular(line2));
      assertTrue(line1.areLinesPerpendicular(line3));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointOnLeftSideOfLine()
   {
      Random random = new Random(8989L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d normalizedVector = new Vector2d();
         line2d.getNormalizedVector(normalizedVector);
         Vector2d perpendicularVector = line2d.perpendicularVector();
         Point2d checkPoint = new Point2d();

         double longitude = randomDouble(random);
         double latitude = (randomDouble(random) + maxRandomValue) / 2.0 + 1.0;    // Makes sure the point ends up on the right side.
         checkPoint.scaleAdd(longitude, normalizedVector, firstPointOnLine);
         checkPoint.scaleAdd(latitude, perpendicularVector, checkPoint);

         assertFalse(line2d.isPointOnLeftSideOfLine(checkPoint));

         // TODO: Do we need a check for a point on the line? Floating point errors might put this at the wrong side.
//       checkPoint.scaleAdd(longitude, normalizedVector, firstPointOnLine);
//       assertFalse(line2d.isPointOnLeftSideOfLine(checkPoint));

         checkPoint.scaleAdd(-2.0 * latitude, perpendicularVector, checkPoint);
         assertTrue(line2d.isPointOnLeftSideOfLine(checkPoint));
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointOnRightSideOfLine()
   {
      // TODO: check strictness on this method, currently inconsistent with isPointOnLeftSideOfLine
      Random random = new Random(9999L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d normalizedVector = new Vector2d();
         line2d.getNormalizedVector(normalizedVector);
         Vector2d perpendicularVector = line2d.perpendicularVector();
         Point2d checkPoint = new Point2d();

         double longitude = randomDouble(random);
         double latitude = (randomDouble(random) + maxRandomValue) / 2.0 + 1.0;    // Makes sure the point ends up on the right side.
         checkPoint.scaleAdd(longitude, normalizedVector, firstPointOnLine);
         checkPoint.scaleAdd(latitude, perpendicularVector, checkPoint);

         assertTrue(line2d.isPointOnRightSideOfLine(checkPoint));

         // TODO: Do we need a check for a point on the line? Floating point errors might put this at the wrong side.
//       checkPoint.scaleAdd(longitude, normalizedVector, firstPointOnLine);
//       assertTrue(line2d.isPointOnRightSideOfLine(checkPoint));

         checkPoint.scaleAdd(-2.0 * latitude, perpendicularVector, checkPoint);
         assertFalse(line2d.isPointOnRightSideOfLine(checkPoint));
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSideConsistency()
   {
      Random random = new Random(1234L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Vector2d normalizedVector = new Vector2d();
         line2d.getNormalizedVector(normalizedVector);
         Vector2d perpendicularVector = line2d.perpendicularVector();
         Point2d checkPoint = new Point2d();

         double longitude = randomDouble(random);
         double latitude = randomDouble(random);
         checkPoint.scaleAdd(longitude, normalizedVector, firstPointOnLine);
         checkPoint.scaleAdd(latitude, perpendicularVector, checkPoint);

         assertFalse(line2d.isPointOnRightSideOfLine(checkPoint) == line2d.isPointOnLeftSideOfLine(checkPoint));
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointInFrontOfLine()
   {
      Random random = new Random(7777L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Point2d checkPoint = new Point2d(secondPointOnLine);
         double shift = (randomDouble(random) + maxRandomValue) / 2.0 + 1.0;    // Makes sure the shift is strictly positive.
         checkPoint.x = checkPoint.x + shift;
         assertTrue(line2d.isPointInFrontOfLine(checkPoint));
         checkPoint.x = checkPoint.x - 2.0 * shift;
         assertFalse(line2d.isPointInFrontOfLine(checkPoint));

         // TODO: is a test necessary where the point is exactly on the line? These kind of tests might fail due to floating point errors.
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointInFrontOfLineException()
   {
      Random random = new Random(1776L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Vector2d direction = new Vector2d(1.0, 0.0);
         Line2d line2d = new Line2d(firstPointOnLine, direction);
         Point2d checkPoint = randomPoint(random);
         try
         {
            line2d.isPointInFrontOfLine(checkPoint);
            fail("Failed to throw exception.");
         }
         catch (RuntimeException exception)
         {
         }
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointBehindLine()
   {
      Random random = new Random(1776L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Point2d checkPoint = new Point2d(secondPointOnLine);
         double shift = (randomDouble(random) - maxRandomValue) / 2.0 - 1.0;    // Makes sure the shift is strictly negative.
         checkPoint.x = checkPoint.x + shift;
         assertTrue(line2d.isPointBehindLine(checkPoint));
         checkPoint.x = checkPoint.x - 2.0 * shift;
         assertFalse(line2d.isPointBehindLine(checkPoint));

         // TODO: is a test necessary where the point is exactly on the line? These kind of tests might fail due to floating point errors.
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIsPointBehindLineException()
   {
      Random random = new Random(1776L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Vector2d direction = new Vector2d(1.0, 0.0);
         Line2d line2d = new Line2d(firstPointOnLine, direction);
         Point2d checkPoint = randomPoint(random);
         try
         {
            line2d.isPointBehindLine(checkPoint);
            fail("Failed to throw exception.");
         }
         catch (RuntimeException exception)
         {
         }
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrontBehindConsistency()
   {
      Random random = new Random(1776L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);
         Point2d checkPoint = new Point2d(secondPointOnLine);
         double shift = randomDouble(random);
         checkPoint.x = checkPoint.x + shift;
         assertFalse(line2d.isPointBehindLine(checkPoint) == line2d.isPointInFrontOfLine(checkPoint));
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetParameterGivenPointEpsilon()
   {
      Random random = new Random(1776L);
      double epsilon = 1e-5;
      double delta = 1.0e-5;

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firsPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firsPointOnLine, secondPointOnLine);
         double parameter = randomDouble(random);
         Point2d checkPoint = new Point2d(firsPointOnLine);
         Vector2d normalizedVector = new Vector2d();
         line2d.getNormalizedVector(normalizedVector);
         checkPoint.scaleAdd(parameter, normalizedVector, firsPointOnLine);

         double calculatedParameter = line2d.getParameterGivenPointEpsilon(checkPoint, epsilon);

         assertEquals(parameter, calculatedParameter, delta);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetParameterGivenPointEpsilonException()
   {
      Random random = new Random(1776L);
      double epsilon = 1e-5;
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firsPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firsPointOnLine, secondPointOnLine);
         double parameter = randomDouble(random);
         double perpendicularDistance = randomDouble(random);
         perpendicularDistance = perpendicularDistance + Math.signum(perpendicularDistance);    // Ensures that the point cannot be on the line
         Point2d checkPoint = new Point2d(firsPointOnLine);
         Vector2d normalizedVector = new Vector2d();
         line2d.getNormalizedVector(normalizedVector);
         checkPoint.scaleAdd(parameter, normalizedVector, firsPointOnLine);
         checkPoint.scaleAdd(perpendicularDistance, line2d.perpendicularVector(), checkPoint);

         try
         {
            line2d.getParameterGivenPointEpsilon(checkPoint, epsilon);
            fail("Failed to throw an exception");
         }
         catch (RuntimeException exception)
         {
         }
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIntersectionWithConvexPolygon()
   {
      // TODO: Failing test case ignored
      Random random = new Random(1776L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

         ArrayList<Point2d> pointList = new ArrayList<Point2d>();
         for (int j = 0; j < 25; j++)
         {
            pointList.add(randomPoint(random));
         }

         ConvexPolygon2d convexPolygon = new ConvexPolygon2d(pointList);
         Point2d[] intersectionList = line2d.intersectionWith(convexPolygon);

         assertTrue((intersectionList == null) || (intersectionList.length % 2 == 0));
         assertTrue((intersectionList == null) || (intersectionList.length <= 2));
      }

      Point2d firstPointOnLine = new Point2d(0.0, 0.0);
      Point2d secondPointOnLine = new Point2d(1.0, 1.0);
      Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

      Point2d firstPolygonPoint = new Point2d(0.0, 0.0);
      Point2d secondPolygonPoint = new Point2d(0.0, 1.0);
      Point2d thirdPolygonPoint = new Point2d(-1.0, 0.0);
      ArrayList<Point2d> polygonPoints = new ArrayList<Point2d>();
      polygonPoints.add(firstPolygonPoint);
      polygonPoints.add(secondPolygonPoint);
      polygonPoints.add(thirdPolygonPoint);
      ConvexPolygon2d triangle = new ConvexPolygon2d(polygonPoints);

      Point2d[] intersections = line2d.intersectionWith(triangle);
      assertEquals(1, intersections.length);

      line2d.setPoint2d(new Point2d(-0.5, 0));
      intersections = line2d.intersectionWith(triangle);
      assertEquals(2, intersections.length);

      line2d.setPoint2d(new Point2d(0.5, 0));
      intersections = line2d.intersectionWith(triangle);
      assertNull(intersections);

      line2d.set(0.0, 0.0, 0.0, 1.0);
      intersections = line2d.intersectionWith(triangle);

      // TODO: Define how many intersections there should be.
      // JvE: personal opinion is 2, current result is 1.
      // assertEquals(2, intersections.length);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testcontainsNaN()
   {
      Random random = new Random(1776L);
      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

         assertFalse(line2d.containsNaN());

         Point2d point = line2d.getPoint();
         point.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         point.y = Double.NaN;
         assertTrue(line2d.containsNaN());

         Vector2d vector = line2d.getNormalizedVector();
         Vector2d vectorCopy = new Vector2d();
         line2d.getNormalizedVector(vectorCopy);
         vector.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = vectorCopy.x;
         vector.y = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = vectorCopy.x;
         point.y = Double.NaN;
         assertTrue(line2d.containsNaN());

         point.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.y = vectorCopy.y;
         vector.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         point.y = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = vectorCopy.x;
         point.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = vectorCopy.x;
         vector.y = Double.NaN;
         assertTrue(line2d.containsNaN());

         vector.x = Double.NaN;
         assertTrue(line2d.containsNaN());

         point.y = Double.NaN;
         assertTrue(line2d.containsNaN());

         point.x = Double.NaN;
         assertTrue(line2d.containsNaN());
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetParallelThroughPoint()
   {
      Random random = new Random(1776L);
      double delta = 1.0e-5;

      for (int i = 0; i < ITERATIONS; i++)
      {
         Point2d firstPointOnLine = randomPoint(random);
         Point2d secondPointOnLine = randomPoint(random);
         Line2d line2d = new Line2d(firstPointOnLine, secondPointOnLine);

         Line2d parallelLine = new Line2d(line2d);
         Point2d checkPoint = randomPoint(random);
         parallelLine.setParallelLineThroughPoint(checkPoint);

         assertEquals(line2d.getNormalizedVector().x, parallelLine.getNormalizedVector().x, delta);
         assertEquals(line2d.getNormalizedVector().y, parallelLine.getNormalizedVector().y, delta);

         assertTrue(parallelLine.containsEpsilon(checkPoint, delta));
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransformTranslation()
   {
      Random random = new Random(1776L);
      double delta = 1.0e-5;

      RigidBodyTransform transform = new RigidBodyTransform();

      // pure translation:
      Vector3d translation = new Vector3d(random.nextDouble(), random.nextDouble(), 0.0);
      Vector3d eulerAngles = new Vector3d(0.0, 0.0, 0.0);

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);

      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);
      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);
      Point2d point = new Point2d();
      line.getPoint(point);
      Vector2d vector = new Vector2d();
      line.getNormalizedVector(vector);

      line.applyTransform(transform);
      assertEquals("pure translation failed", point.x + translation.x, line.point.x, delta);
      assertEquals("pure translation failed", point.y + translation.y, line.point.y, delta);
      assertEquals("pure translation failed", vector.x, line.normalizedVector.x, delta);
      assertEquals("pure translation failed", vector.y, line.normalizedVector.y, delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.1, targets = TestPlanTarget.Exclude)
	@Test(timeout=300000,expected = RuntimeException.class)
   public void testApplyTransformTranslationException()
   {
      Random random = new Random(1776L);
      RigidBodyTransform transform = new RigidBodyTransform();

      // pure translation:
      Vector3d translation = new Vector3d(random.nextDouble(), random.nextDouble(), 1.0);
      Vector3d eulerAngles = new Vector3d(0.0, 0.0, 0.0);

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);


      System.out.println(transform);

      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);
      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);

      // TODO Check out why this doesn't fail. Should it fail? Is out of plane translation ignored?
      line.applyTransform(transform);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransformRotation()
   {
      Random random = new Random(1776L);
      double delta = 1.0e-5;

      RigidBodyTransform transform = new RigidBodyTransform();
      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);

      // pure translation:
      Vector3d translation = new Vector3d(0.0, 0.0, 0.0);
      Vector3d eulerAngles = new Vector3d(0.0, 0.0, randomDouble(random, 2.0 * Math.PI));

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);

      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);
      Point2d point = new Point2d();
      line.getPoint(point);
      Vector2d vector = new Vector2d();
      line.getNormalizedVector(vector);

      line.applyTransform(transform);

      double alpha = eulerAngles.z;
      double sina = Math.sin(alpha);
      double cosa = Math.cos(alpha);

      assertEquals("pure rotation failed", point.x * cosa - point.y * sina, line.point.x, delta);
      assertEquals("pure rotation failed", point.x * sina + point.y * cosa, line.point.y, delta);
      assertEquals("pure rotation failed", vector.x * cosa - vector.y * sina, line.normalizedVector.x, delta);
      assertEquals("pure rotation failed", vector.x * sina + vector.y * cosa, line.normalizedVector.y, delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testApplyTransformRotationXaxisException()
   {
      Random random = new Random(1776L);
      RigidBodyTransform transform = new RigidBodyTransform();
      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);

      // pure translation:
      Vector3d translation = new Vector3d(0.0, 0.0, 0.0);
      Vector3d eulerAngles = new Vector3d(randomDouble(random, 2.0 * Math.PI), 0.0, 0.0);

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);

      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);

      line.applyTransform(transform);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testApplyTransformRotationYaxisException()
   {
      Random random = new Random(1776L);
      RigidBodyTransform transform = new RigidBodyTransform();
      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);

      // pure translation:
      Vector3d translation = new Vector3d(0.0, 0.0, 0.0);
      Vector3d eulerAngles = new Vector3d(0.0, randomDouble(random, 2.0 * Math.PI), 0.0);

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);

      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);

      line.applyTransform(transform);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransformCombination()
   {
      Random random = new Random(1776L);
      double delta = 1.0e-5;

      RigidBodyTransform transform = new RigidBodyTransform();
      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);

      // pure translation:
      Vector3d translation = new Vector3d(randomDouble(random), randomDouble(random), 0.0);
      Vector3d eulerAngles = new Vector3d(0.0, 0.0, randomDouble(random, 2.0 * Math.PI));

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);

      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);
      Point2d point = new Point2d();
      line.getPoint(point);
      Vector2d vector = new Vector2d();
      line.getNormalizedVector(vector);

      line.applyTransform(transform);

      double alpha = eulerAngles.z;
      double sina = Math.sin(alpha);
      double cosa = Math.cos(alpha);

      assertEquals("pure rotation failed", point.x * cosa - point.y * sina + translation.x, line.point.x, delta);
      assertEquals("pure rotation failed", point.x * sina + point.y * cosa + translation.y, line.point.y, delta);
      assertEquals("pure rotation failed", vector.x * cosa - vector.y * sina, line.normalizedVector.x, delta);
      assertEquals("pure rotation failed", vector.x * sina + vector.y * cosa, line.normalizedVector.y, delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransformCopy()
   {
      Random random = new Random(1776L);
      double delta = 1.0e-5;

      RigidBodyTransform transform = new RigidBodyTransform();
      Point2d firstPointOnLine = randomPoint(random);
      Point2d secondPointOnLine = randomPoint(random);

      // pure translation:
      Vector3d translation = new Vector3d(randomDouble(random), randomDouble(random), 0.0);
      Vector3d eulerAngles = new Vector3d(0.0, 0.0, randomDouble(random, 2.0 * Math.PI));

      transform.setRotationEulerAndZeroTranslation(eulerAngles);
      transform.setTranslation(translation);

      Line2d line = new Line2d(firstPointOnLine, secondPointOnLine);
      Point2d point = new Point2d();
      line.getPoint(point);
      Vector2d vector = new Vector2d();
      line.getNormalizedVector(vector);

      Line2d transformedCopy = line.applyTransformCopy(transform);
      line.applyTransform(transform);
      assertNotSame(transformedCopy, line);
      assertNotSame(transformedCopy.point, line.point);
      assertNotSame(transformedCopy.normalizedVector, line.normalizedVector);
      assertEquals(line.point.x, transformedCopy.point.x, delta);
      assertEquals(line.point.y, transformedCopy.point.y, delta);
      assertEquals(line.normalizedVector.x, transformedCopy.normalizedVector.x, delta);
      assertEquals(line.normalizedVector.y, transformedCopy.normalizedVector.y, delta);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testOrthogonalProjectionCopyPoint2dLine2d()
   {
      Point2d startPoint = new Point2d(-10.0, 0.0);
      Point2d endPoint = new Point2d(10.0, 0.0);
      Line2d line1 = new Line2d(startPoint, endPoint);

      Point2d origionalPoint = new Point2d(-20.0, 10.0);
      Point2d projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(-20.0, 0.0), projectedPoint);

      origionalPoint = new Point2d(-20.0, -10.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(-20, 0.0), projectedPoint);

      origionalPoint = new Point2d(-20.0, 0.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(-20.0, 0.0), projectedPoint);

      origionalPoint = new Point2d(20.0, 10.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(20.0, 0.0), projectedPoint);

      origionalPoint = new Point2d(20.0, -10.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(20.0, 0.0), projectedPoint);
      origionalPoint = new Point2d(20.0, 0.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(20.0, 0.0), projectedPoint);
      origionalPoint = new Point2d(0.0, 10.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(0.0, 0.0), projectedPoint);
      origionalPoint = new Point2d(0.0, -10.0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(0.0, 0.0), projectedPoint);


      origionalPoint = new Point2d(5.0, 0);
      projectedPoint = line1.orthogonalProjectionCopy(origionalPoint);
      assertEquals(new Point2d(5.0, 0.0), projectedPoint);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testIntersectionLine2dLine2d()
   {
      Line2d line1 = new Line2d(new Point2d(-10.0, 0.0), new Point2d(10.0, 0.0));
      Line2d line2 = new Line2d(new Point2d(-10.0, 10.0), new Point2d(10.0, 0.0));
      Line2d line3 = new Line2d(new Point2d(0.0, 10.0), new Point2d(0.0, -10.0));
      Line2d line4 = new Line2d(new Point2d(0.0, -10.0), new Point2d(0.0, 10.0));
      Line2d line5 = new Line2d(new Point2d(-10.0, 0.0), new Point2d(10.0, 0.0));
      Line2d line6 = new Line2d(new Point2d(10.0, 0.0), new Point2d(-10.0, 0.0));
      Line2d line7 = new Line2d(new Point2d(10.0, 0.0), new Point2d(20.0, 0.0));
      Line2d line8 = new Line2d(new Point2d(10.0, 0.0), new Point2d(-20.0, 0.0));
      Line2d line9 = new Line2d(new Point2d(10.1, 0.0), new Point2d(20.0, 0.0));
      Line2d line10 = new Line2d(new Point2d(10.0, 0.0), new Point2d(20.0, 1.0));
      Line2d line11 = new Line2d(new Point2d(-10.0, 1.0), new Point2d(10.0, 1.0));

      assertEquals(null, line1.intersectionWith(line11));


      assertEquals(null, line1.intersectionWith(line5));
      assertEquals(null, line1.intersectionWith(line6));
      assertEquals(new Point2d(10.0, 0.0), line1.intersectionWith(line2));
      assertEquals(new Point2d(10.0, 0.0), line1.intersectionWith(line10));

      assertEquals(new Point2d(0.0, 0.0), line1.intersectionWith(line3));
      assertEquals(new Point2d(0.0, 0.0), line1.intersectionWith(line4));


      assertEquals(null, line1.intersectionWith(line7));
      assertEquals(null, line1.intersectionWith(line8));
      assertEquals(null, line1.intersectionWith(line9));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDistancePointLineTwo()
   {
      Point2d pointOnLine = new Point2d(0.0, 1.0);
      Vector2d directionVector = new Vector2d(1.0, 0.0);
      Line2d line = new Line2d(pointOnLine, directionVector);

      Point2d point = new Point2d(0.0, 2.0);
      double distance = line.distance(point);
      double delta = 1e-12;
      assertEquals("Distance to a horizontal line not calculated correctly", 1.0, distance, delta);

      pointOnLine = new Point2d(-1.0, 0.0);
      directionVector = new Vector2d(0.0, 1.0);
      line = new Line2d(pointOnLine, directionVector);

      point = new Point2d(2.0, 3.0);
      distance = line.distance(point);
      delta = 1e-12;
      assertEquals("Distance to a horizontal line not calculated correctly", 3.0, distance, delta);
   }
	
	public static void main(String[] args)
   {
	   String targetTests = "us.ihmc.robotics.geometry.Line2dTest";
	   String targetClasses = "us.ihmc.robotics.geometry.FrameTuple,us.ihmc.robotics.geometry.Line2d";
	   MutationTestingTools.doPITMutationTestAndOpenResult(targetTests, targetClasses);
   }

}
