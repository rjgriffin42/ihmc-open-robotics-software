package us.ihmc.robotics.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Test;

import us.ihmc.robotics.geometry.transformables.TransformableVector3d;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.testing.JUnitTools;
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
public class FrameVectorTest extends FrameTupleTest<TransformableVector3d>
{

   @After
   public void tearDown()
   {
   }

   @Override
   public FrameVector createEmptyFrameTuple()
   {
      return new FrameVector();
   }

   @Override
   public FrameVector createFrameTuple(ReferenceFrame referenceFrame, double x, double y, double z)
   {
      return new FrameVector(referenceFrame, x, y, z);
   }

   @Override
   public FrameTuple<FrameVector, TransformableVector3d> createFrameTuple(ReferenceFrame referenceFrame, double x, double y, double z, String name)
   {
      return new FrameVector(referenceFrame, x, y, z, name);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void allConstructorsWork() //Brett
   {
      Tuple3d tuple = new Point3d(1.0, 1.0, 1.0);
      String name = "VectorName";
      double[] doubleArray = {10.0, -20.0, 35.0};
      Vector3d zeroVector = new Vector3d(0.0, 0.0, 0.0);

      FrameVector frameTuple = new FrameVector(theFrame, tuple);
      assertEquals("These should be equal", tuple, frameTuple.getVector());
      frameTuple.setName(name);
      assertEquals("These should be equal", name, frameTuple.getName());
      assertEquals("These should be equal", theFrame, frameTuple.getReferenceFrame());

      FrameVector frameTupleString = new FrameVector(theFrame, tuple, name);
      assertEquals("These should be equal", tuple, frameTupleString.getVector());
      frameTupleString.setName(name);
      assertEquals("These should be equal", name, frameTupleString.getName());
      assertEquals("These should be equal", theFrame, frameTupleString.getReferenceFrame());

      FrameVector frameDoubles = new FrameVector(theFrame, doubleArray);
      Tuple3d position = new Vector3d(doubleArray);
      assertEquals("These should be equal", position, frameDoubles.getVector());
      frameDoubles.setName(name);
      assertEquals("These should be equal", name, frameDoubles.getName());
      assertEquals("These should be equal", theFrame, frameDoubles.getReferenceFrame());

      FrameVector frameDoublesString = new FrameVector(theFrame, doubleArray, name);
      assertEquals("These should be equal", position, frameDoublesString.getVector());
      assertEquals("These should be equal", name, frameDoublesString.getName());
      assertEquals("These should be equal", theFrame, frameDoublesString.getReferenceFrame());

      FrameVector empty = new FrameVector();
      empty.setName(name);
      empty.setIncludingFrame(theFrame, 10.0, -20.0, 35.0);
      assertEquals("These should be equal", position, empty.getVector());
      assertEquals("These should be equal", name, empty.getName());
      assertEquals("These should be equal", theFrame, empty.getReferenceFrame());

      FrameVector frame = new FrameVector(theFrame);
      frame.setName(name);
      frame.set(10.0, -20.0, 35.0);
      assertEquals("These should be equal", position, frame.getVector());
      assertEquals("These should be equal", name, frame.getName());
      assertEquals("These should be equal", theFrame, frame.getReferenceFrame());

      FrameVector frameString = new FrameVector(theFrame, name);
      assertEquals("These should be equal", name, frameString.getName());
      assertEquals("These should be equal", theFrame, frameString.getReferenceFrame());
      assertEquals("These should be equal", zeroVector, frameString.getVector());

      //      FrameVector frametuple = new FrameVector(frameTuple);
      //      FrameVector frameXYZ = new FrameVector(referenceFrame, x, y, z);
      //      FrameVector frameXYZString = new FrameVector(referenceFrame, x, y, z name);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testFrameChanges()
   {
      FrameVector frameVector = new FrameVector(theFrame);
      RigidBodyTransform transform3d = new RigidBodyTransform();
      
      frameVector.changeFrameUsingTransform(childFrame, transform3d);
      frameVector.checkReferenceFrameMatch(childFrame);
      
      FrameVector result = new FrameVector(frameVector);

      result.changeFrameUsingTransform(theFrame, transform3d);
      result.checkReferenceFrameMatch(theFrame);
      
      result = new FrameVector(frameVector);
      result.changeFrame(childFrame);
      result.checkReferenceFrameMatch(childFrame);
      
      frameVector.changeFrame(theFrame); //cause of failure
      frameVector.checkReferenceFrameMatch(theFrame);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testGetVector()
   {
      FrameVector frameVector = new FrameVector(theFrame, 10.0, 20.0, 30.0);
      Vector3d expected = frameVector.getVector();
      assertEquals("These should be equal", 10.0, expected.getX(), epsilon);
      assertEquals("These should be equal", 20.0, expected.getY(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testToFrameVector2d() //Brett was here
   {
      FrameVector frameVector = new FrameVector(theFrame, 11.0, 22.0, 33.0);
      FrameVector2d expected = frameVector.toFrameVector2d();
      assertEquals("These should be equal", 11.0, expected.getX(), epsilon);
      assertEquals("These should be equal", 22.0, expected.getY(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = ReferenceFrameMismatchException.class)
   public void testDot() //Brett
   {
      FrameVector frameVector1 = new FrameVector(theFrame, 1.0, 2.0, 3.0);
      FrameVector frameVector2 = new FrameVector(theFrame, 10.0, 20.0, 30.0);
      double dotProduct = frameVector1.dot(frameVector2);
      double expected = 140.0;
      assertEquals("This should be equal", expected, dotProduct, epsilon);
      
      //test for mismatched reference frames
      FrameVector frameVector3 = new FrameVector(aFrame, 0.0, 1.0, 2.0);
      frameVector1.dot(frameVector3);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = ReferenceFrameMismatchException.class)
   public void testAngle() //Brett
   {
      FrameVector frameVector1 = new FrameVector(theFrame, 1.0, 2.0, 3.0);
      FrameVector frameVector2 = new FrameVector(theFrame, 0.0, 1.0, 2.0);

      double actual = frameVector1.angle(frameVector2);
      double l1 = frameVector1.length();
      double l2 = frameVector2.length();
      double denom = l1 * l2;
      double numer = frameVector1.dot(frameVector2);
      double arg = numer/denom;
      double expected = Math.acos(arg);
      assertEquals("These should be equal", expected, actual, epsilon);
      
      //test for mismatched reference frames
      FrameVector frameVector3 = new FrameVector(aFrame, 0.0, 1.0, 2.0);
      frameVector1.angle(frameVector3);
   }
	
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testIsParallel()
   {
      Random random = new Random(100L);

      FrameVector randomFrameVector = new FrameVector(theFrame, RandomTools.generateRandomVector(random));
      FrameVector parallelVector = new FrameVector(randomFrameVector);
      parallelVector.scale(RandomTools.generateRandomDouble(random, -1.0, 1.0));

      String errorMsg = "\n" + randomFrameVector + "\n should be parallel to: \n" + parallelVector + "\n Angle between vectors = " + randomFrameVector.angle(parallelVector);
      assertTrue(errorMsg, randomFrameVector.isEpsilonParallel(parallelVector, 1e-7));
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testLength() //Brett was here
   {
      for(int i = 0; i < 100; i++) //compare against Vector3d.length()
      {
         Random random = new Random(45456L);
         Vector3d vector3d = RandomTools.generateRandomVector(random);
         FrameVector frameVector= new FrameVector(theFrame, vector3d);
         double vector3dResult = vector3d.length();
         double frameVectorResult = frameVector.length();
         assertEquals("These should be equal", vector3dResult, frameVectorResult, epsilon);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testLengthSquared() //Brett was here
   {
      FrameVector frameVector = new FrameVector(theFrame, 0.0, 4.0, 3.0);
      assertEquals("These should be equal", 25.0, frameVector.lengthSquared(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testApplyTransform() //Brett was here
   {
      for(int i = 0; i < 100; i++)
      {
         Random random = new Random(45456L);
         Vector3d vector3d = RandomTools.generateRandomVector(random);
         FrameVector frameVector = new FrameVector(theFrame, vector3d);
         RigidBodyTransform transform = RigidBodyTransform.generateRandomTransform(random);

         frameVector.applyTransform(transform);  //Compare transform of Vector3d and FrameVector
         transform.transform(vector3d);
         assertTrue(frameVector.getVector().epsilonEquals(vector3d, epsilon));
      }      
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testCrosses() //Brett
   {
      FrameVector frameVector1 = createFrameTuple(theFrame, 1, 2, 3); 
      FrameVector frameVector2 = createFrameTuple(theFrame, 0, 1, 2);
      FrameVector v2other = createFrameTuple(aFrame, 0, 1, 2);
      FrameVector result = createFrameTuple(theFrame, 0, 0, 0);
      Vector3d expected = new Vector3d(1, -2, 1);

      result.cross(frameVector1, frameVector2);
      assertTrue(result.getVector().epsilonEquals(expected, epsilon)); //correctly calculates cross product
      assertEquals("These should be equal", 0.0, result.dot(frameVector1), epsilon); //result of cross product orthogonal to both its factors
      assertEquals("These should be equal", 0.0, result.dot(frameVector2), epsilon); //result of cross product orthogonal to both its factors

      try //fails on non-matching reference frames
      {
         result.cross(frameVector1, v2other);
         fail();
      }
      catch(ReferenceFrameMismatchException rfme)
      {
         //Good
      }

      try //fails on non-matching reference frames
      {
         result.cross(v2other, frameVector1);
         fail();
      }
      catch(ReferenceFrameMismatchException rfme)
      {
         //Good
      }

      for(int i = 0; i < 100; i++) //compare against Vector3d.cross()
      {
         Random random = new Random(45456L);
         Vector3d v1 = RandomTools.generateRandomVector(random);
         Vector3d v2 = RandomTools.generateRandomVector(random);
         Vector3d v3 = RandomTools.generateRandomVector(random);
         Vector3d staticResult = RandomTools.generateRandomVector(random);
         FrameVector fv1 = new FrameVector(theFrame, v1);
         FrameVector fv2 = new FrameVector(theFrame, v2);
         FrameVector fv3 = new FrameVector(theFrame, v3);

         v3.cross(v1, v2);  //Compare cross of Vector3d and FrameVector
         fv3.cross(fv1, fv2);
         assertTrue(fv3.getVector().epsilonEquals(v3, epsilon));

         FrameVector.cross(staticResult, v1, v2); //compare to static version of cross()
         assertTrue(fv3.getVector().epsilonEquals(staticResult, epsilon));
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testCrossWithVector3d()
   {
      Random random = new Random(45456L);
      FrameVector expectedFrameVector = new FrameVector(theFrame);
      FrameVector actualFrameVector = new FrameVector(theFrame);

      for(int i = 0; i < 100; i++) //compare against Vector3d.cross()
      {
         Vector3d v1 = RandomTools.generateRandomVector(random);
         Vector3d v2 = RandomTools.generateRandomVector(random);
         Vector3d v3 = RandomTools.generateRandomVector(random);
         

         v3.cross(v1, v2);  //Compare cross of Vector3d and FrameVector
         expectedFrameVector.set(v3);
         actualFrameVector.cross(v1, v2);
         assertTrue(actualFrameVector.epsilonEquals(expectedFrameVector, epsilon));
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testNormalize() //Brett
   {
      for(int i = 0; i < 100; i++) //sum of squares of normalized vector equals 1
      {
         Random random = new Random(45456L);
         Vector3d vector3d = RandomTools.generateRandomVector(random);
         FrameVector v1 = new FrameVector(theFrame, vector3d);
         v1.normalize();
         double sumOfSquares = v1.getX()*v1.getX() + v1.getY()*v1.getY() + v1.getZ()*v1.getZ(); 
         assertEquals("These should be equal", 1.0, sumOfSquares, epsilon);
      }
   }

   public static void assertFrameVectorEquals(FrameVector expected, FrameVector actual, double delta)
   {
      expected.checkReferenceFrameMatch(actual);
      JUnitTools.assertTuple3dEquals(expected.getVector(), actual.getVector(), delta);
   }
}
