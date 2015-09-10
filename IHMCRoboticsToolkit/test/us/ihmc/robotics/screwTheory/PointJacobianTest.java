package us.ihmc.robotics.screwTheory;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.junit.Test;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVectorTest;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

import javax.vecmath.Vector3d;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author twan
 *         Date: 5/18/13
 */
public class PointJacobianTest
{
   private static final Vector3d X = new Vector3d(1.0, 0.0, 0.0);
   private static final Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
   private static final Vector3d Z = new Vector3d(0.0, 0.0, 1.0);

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testVersusNumericalDifferentiation()
   {
      Random random = new Random(1252523L);
      Vector3d[] jointAxes = new Vector3d[]
      {
         X, Y, Z, Y, Y, X
      };
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      randomFloatingChain.setRandomPositionsAndVelocities(random);

      RigidBody base = randomFloatingChain.getRootJoint().getSuccessor();
      RigidBody endEffector = randomFloatingChain.getLeafBody();
      GeometricJacobian geometricJacobian = new GeometricJacobian(base, endEffector, base.getBodyFixedFrame());
      geometricJacobian.compute();

      FramePoint point = new FramePoint(base.getBodyFixedFrame(), RandomTools.generateRandomVector(random));
      PointJacobian pointJacobian = new PointJacobian();
      pointJacobian.set(geometricJacobian, point);
      pointJacobian.compute();

      InverseDynamicsJoint[] joints = geometricJacobian.getJointsInOrder();

      DenseMatrix64F jointVelocities = new DenseMatrix64F(ScrewTools.computeDegreesOfFreedom(joints), 1);
      ScrewTools.packJointVelocitiesMatrix(joints, jointVelocities);

      DenseMatrix64F pointVelocityFromJacobianMatrix = new DenseMatrix64F(3, 1);
      CommonOps.mult(pointJacobian.getJacobianMatrix(), jointVelocities, pointVelocityFromJacobianMatrix);
      FrameVector pointVelocityFromJacobian = new FrameVector(pointJacobian.getFrame());
      MatrixTools.denseMatrixToVector3d(pointVelocityFromJacobianMatrix, pointVelocityFromJacobian.getVector(), 0, 0);

      FramePoint point2 = new FramePoint(point);
      point2.changeFrame(endEffector.getBodyFixedFrame());
      double dt = 1e-8;
      ScrewTestTools.integrateVelocities(randomFloatingChain.getRevoluteJoints(), dt);
      point2.changeFrame(base.getBodyFixedFrame());

      FrameVector pointVelocityFromNumericalDifferentiation = new FrameVector(point2);
      pointVelocityFromNumericalDifferentiation.sub(point);
      pointVelocityFromNumericalDifferentiation.scale(1.0 / dt);

      FrameVectorTest.assertFrameVectorEquals(pointVelocityFromNumericalDifferentiation, pointVelocityFromJacobian, 1e-6);
   }

	@DeployableTestMethod(estimatedDuration = 0.0)
	@Test(timeout = 30000)
   public void testSingularValuesOfTwoPointJacobians()
   {
      Random random = new Random(12351235L);
      Vector3d[] jointAxes = new Vector3d[]
      {
         X, Y, Z, Y, Y, X
      };
      ScrewTestTools.RandomFloatingChain randomFloatingChain = new ScrewTestTools.RandomFloatingChain(random, jointAxes);
      randomFloatingChain.setRandomPositionsAndVelocities(random);

      RigidBody base = randomFloatingChain.getRootJoint().getSuccessor();
      RigidBody endEffector = randomFloatingChain.getLeafBody();
      GeometricJacobian geometricJacobian = new GeometricJacobian(base, endEffector, base.getBodyFixedFrame());
      geometricJacobian.compute();

      FramePoint point1 = new FramePoint(base.getBodyFixedFrame(), RandomTools.generateRandomVector(random));
      FramePoint point2 = new FramePoint(base.getBodyFixedFrame(), RandomTools.generateRandomVector(random));

      PointJacobian pointJacobian1 = new PointJacobian();
      pointJacobian1.set(geometricJacobian, point1);
      pointJacobian1.compute();

      PointJacobian pointJacobian2 = new PointJacobian();
      pointJacobian2.set(geometricJacobian, point2);
      pointJacobian2.compute();

      DenseMatrix64F assembledJacobian = new DenseMatrix64F(SpatialMotionVector.SIZE, geometricJacobian.getNumberOfColumns());
      CommonOps.insert(pointJacobian1.getJacobianMatrix(), assembledJacobian, 0, 0);
      CommonOps.insert(pointJacobian2.getJacobianMatrix(), assembledJacobian, 3, 0);

      SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(assembledJacobian.getNumRows(), assembledJacobian.getNumCols(), true, true,
                                                          false);
      svd.decompose(assembledJacobian);

      double[] singularValues = svd.getSingularValues();
      double smallestSingularValue = Double.POSITIVE_INFINITY;
      for (double singularValue : singularValues)
      {
         if (singularValue < smallestSingularValue)
         {
            smallestSingularValue = singularValue;
         }
      }

      double epsilon = 1e-12;
      assertTrue(smallestSingularValue < epsilon);
   }
}
