package us.ihmc.robotics.screwTheory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.apache.commons.lang3.ArrayUtils;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.ejml.ops.RandomMatrices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.testing.JUnitTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class WrenchTest
{
   private ReferenceFrame frameA;
   private ReferenceFrame frameB;
   private ReferenceFrame frameC;

   @Before
   public void setUp() throws Exception
   {
      frameA = ReferenceFrame.constructAWorldFrame("A");
      frameB = new ReferenceFrame("B", frameA)
      {
         private static final long serialVersionUID = 1L;

         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
            transformToParent.setEuler(1.0, 2.0, 3.0);
            RigidBodyTransform translation = new RigidBodyTransform();
            translation.setTranslation(new Vector3d(3.0, 4.0, 5.0));
            transformToParent.multiply(translation);
         }
      };

      frameC = new ReferenceFrame("C", frameB)
      {
         private static final long serialVersionUID = 1L;

         @Override
         protected void updateTransformToParent(RigidBodyTransform transformToParent)
         {
            transformToParent.setEuler(1.0, 2.0, 3.0);
            RigidBodyTransform translation = new RigidBodyTransform();
            translation.setTranslation(new Vector3d(3.0, 4.0, 5.0));
            transformToParent.multiply(translation);
         }
      };

      frameB.update();
      frameC.update();
   }

   @After
   public void tearDown() throws Exception
   {
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testChangeExpressedInWhatReferenceFrame()
   {
      // create random twists and random wrenches, transform both to other frames,
      // and check that the instantaneous power returned by dot() remains the same
      double epsilon = 1e-10;
      Random random = new Random(102L);
      int nTests = 10;
      for (int i = 0; i < nTests; i++)
      {
         Vector3d angularVelocity = new Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
         Vector3d linearVelocity = new Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
         Twist twist = new Twist(frameC, frameA, frameA, linearVelocity, angularVelocity);

         Vector3d torque = new Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
         Vector3d force = new Vector3d(random.nextDouble(), random.nextDouble(), random.nextDouble());
         Wrench wrench = new Wrench(frameC, frameA, force, torque);    // baseFrame doesn't matter

         double power1 = twist.dot(wrench);

         twist.changeFrame(frameB);
         wrench.changeFrame(frameB);

         double power2 = twist.dot(wrench);

         assertEquals(power1, power2, epsilon);
      }
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDefaultConstructor()
   {
      Wrench wrench = new Wrench();
      assertNull(wrench.getBodyFrame());
      assertNull(wrench.getExpressedInFrame());
      JUnitTools.assertTuple3dEquals(new Vector3d(), wrench.getAngularPart(), 0.0);
      JUnitTools.assertTuple3dEquals(new Vector3d(), wrench.getLinearPart(), 0.0);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testConstructUsingMatrix()
   {
      Random random = new Random(167L);
      DenseMatrix64F matrix = RandomMatrices.createRandom(Wrench.SIZE, 1, random);
      Wrench wrench = new Wrench(frameA, frameB, matrix);
      DenseMatrix64F matrixBack = new DenseMatrix64F(Wrench.SIZE, 1);
      wrench.getMatrix(matrixBack);
      JUnitTools.assertMatrixEquals(matrix, matrixBack, 0.0);

      Vector3d torque = new Vector3d();
      Vector3d force = new Vector3d();
      MatrixTools.denseMatrixToVector3d(matrix, torque, 0, 0);
      MatrixTools.denseMatrixToVector3d(matrix, force, 3, 0);
      JUnitTools.assertTuple3dEquals(torque, wrench.getAngularPartCopy(), 0.0);
      JUnitTools.assertTuple3dEquals(force, wrench.getLinearPartCopy(), 0.0);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testConstructUsingMatrixTooSmall()
   {
      Random random = new Random(12342L);
      DenseMatrix64F matrix = RandomMatrices.createRandom(Wrench.SIZE - 1, 1, random);
      new Wrench(frameA, frameB, matrix);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testConstructUsingMatrixTooBig()
   {
      Random random = new Random(12342L);
      DenseMatrix64F matrix = RandomMatrices.createRandom(Wrench.SIZE + 1, 1, random);
      new Wrench(frameA, frameB, matrix);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testConstructUsingDoubleArray()
   {
      Random random = new Random(1234L);
      int vectorDimension = 3;
      double[] angularArray = new double[vectorDimension];
      double[] linearArray = new double[vectorDimension];

      for (int i = 0; i < vectorDimension; i++)
      {
         angularArray[i] = random.nextDouble();
         linearArray[i] = random.nextDouble();
      }

      double array[] = ArrayUtils.addAll(angularArray, linearArray);

      Wrench wrench = new Wrench(frameC, frameA, array);
      assertEquals(wrench.getBodyFrame(), frameC);
      assertEquals(wrench.getExpressedInFrame(), frameA);
      double epsilon = 0.0;
      JUnitTools.assertTuple3dEquals(new Vector3d(angularArray), wrench.getAngularPartCopy(), epsilon);
      JUnitTools.assertTuple3dEquals(new Vector3d(linearArray), wrench.getLinearPartCopy(), epsilon);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testConstructUsingDoubleArrayTooSmall()
   {
      new Wrench(frameA, frameB, new double[Wrench.SIZE + 1]);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testConstructUsingDoubleArrayTooBig()
   {
      new Wrench(frameA, frameB, new double[Wrench.SIZE - 1]);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testAddNotAllowed()
   {
      Wrench wrench1 = null, wrench2 = null;
      try
      {
         wrench1 = new Wrench(frameB, frameA);
         wrench2 = new Wrench(frameC, frameA);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench1.add(wrench2);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testAddNotAllowed2()
   {
      Wrench wrench1 = null, wrench2 = null;
      try
      {
         wrench1 = new Wrench(frameB, frameA);
         wrench2 = new Wrench(frameB, frameB);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench1.add(wrench2);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testAdd()
   {
      Random random = new Random(187L);
      Wrench wrench1 = new Wrench(frameA, frameB, RandomMatrices.createRandom(Wrench.SIZE, 1, random));
      Wrench wrench2 = new Wrench(frameA, frameB, RandomMatrices.createRandom(Wrench.SIZE, 1, random));
      Wrench wrench3 = new Wrench(wrench1);
      wrench3.add(wrench2);

      Vector3d linearPart = wrench1.getLinearPartCopy();
      linearPart.add(wrench2.getLinearPartCopy());

      Vector3d angularPart = wrench1.getAngularPartCopy();
      angularPart.add(wrench2.getAngularPartCopy());

      JUnitTools.assertTuple3dEquals(wrench3.getLinearPartCopy(), linearPart, 1e-24);
      JUnitTools.assertTuple3dEquals(wrench3.getAngularPartCopy(), angularPart, 1e-24);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSubNotAllowed()
   {
      Wrench wrench1 = null, wrench2 = null;
      try
      {
         wrench1 = new Wrench(frameB, frameA);
         wrench2 = new Wrench(frameC, frameA);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench1.sub(wrench2);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testSubNotAllowed2()
   {
      Wrench wrench1 = null, wrench2 = null;
      try
      {
         wrench1 = new Wrench(frameB, frameA);
         wrench2 = new Wrench(frameB, frameB);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench1.sub(wrench2);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSub()
   {
      Random random = new Random(187L);
      Wrench wrench1 = new Wrench(frameA, frameB, RandomMatrices.createRandom(Wrench.SIZE, 1, random));
      Wrench wrench2 = new Wrench(frameA, frameB, RandomMatrices.createRandom(Wrench.SIZE, 1, random));
      Wrench wrench3 = new Wrench(wrench1);
      wrench3.sub(wrench2);

      Vector3d linearPart = wrench1.getLinearPartCopy();
      linearPart.sub(wrench2.getLinearPartCopy());

      Vector3d angularPart = wrench1.getAngularPartCopy();
      angularPart.sub(wrench2.getAngularPartCopy());

      JUnitTools.assertTuple3dEquals(wrench3.getLinearPartCopy(), linearPart, 1e-24);
      JUnitTools.assertTuple3dEquals(wrench3.getAngularPartCopy(), angularPart, 1e-24);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testCheckAndSetNotAllowed1()
   {
      Wrench wrench1 = null, wrench2 = null;
      try
      {
         wrench1 = new Wrench(frameA, frameB);
         wrench2 = new Wrench(frameA, frameC);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench2.checkAndSet(wrench1);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testCheckAndSetNotAllowed2()
   {
      Wrench wrench1 = null, wrench2 = null;
      try
      {
         wrench1 = new Wrench(frameA, frameB);
         wrench2 = new Wrench(frameC, frameB);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench2.checkAndSet(wrench1);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testDotProduct()
   {
      testDotProduct(frameA, frameB, frameC);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testDotProductNotAllowed1()
   {
      testDotProductNotAllowed1(frameA, frameB, frameC);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000,expected = RuntimeException.class)
   public void testDotProductNotAllowed2()
   {
      testDotProductNotAllowed2(frameA, frameB, frameC);
   }

   public static void testDotProduct(ReferenceFrame frameA, ReferenceFrame frameB, ReferenceFrame frameC)
   {
      Random random = new Random(187L);
      DenseMatrix64F twistMatrix = RandomMatrices.createRandom(Twist.SIZE, 1, random);
      DenseMatrix64F wrenchMatrix = RandomMatrices.createRandom(Wrench.SIZE, 1, random);
      Twist twist = new Twist(frameA, frameB, frameC, twistMatrix);
      Wrench wrench = new Wrench(frameA, frameC, wrenchMatrix);
      DenseMatrix64F c = new DenseMatrix64F(1, 1);
      CommonOps.multTransA(twistMatrix, wrenchMatrix, c);
      assertEquals(c.get(0, 0), wrench.dot(twist), 1e-12);
   }

   public static void testDotProductNotAllowed1(ReferenceFrame frameA, ReferenceFrame frameB, ReferenceFrame frameC)
   {
      Wrench wrench = null;
      Twist twist = null;
      try
      {
         wrench = new Wrench(frameA, frameB);
         twist = new Twist(frameA, frameB, frameC);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench.dot(twist);
   }



   public static void testDotProductNotAllowed2(ReferenceFrame frameA, ReferenceFrame frameB, ReferenceFrame frameC)
   {
      Wrench wrench = null;
      Twist twist = null;
      try
      {
         wrench = new Wrench(frameB, frameB);
         twist = new Twist(frameA, frameB, frameB);
      }
      catch (RuntimeException e)
      {
         fail();
      }

      wrench.dot(twist);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSetToZero()
   {
      Random random = new Random(71243L);
      Wrench wrench = new Wrench(frameA, frameB, RandomMatrices.createRandom(Wrench.SIZE, 1, random));
      wrench.setToZero(frameC, frameA);
      assertEquals(frameC, wrench.getBodyFrame());
      assertEquals(frameA, wrench.getExpressedInFrame());
      DenseMatrix64F matrix = RandomMatrices.createRandom(Wrench.SIZE, 1, random);
      double epsilon = 1e-12;
      assertTrue(NormOps.normP2(matrix) > epsilon);
      wrench.getMatrix(matrix);
      assertTrue(NormOps.normP2(matrix) == 0.0);
   }
}
