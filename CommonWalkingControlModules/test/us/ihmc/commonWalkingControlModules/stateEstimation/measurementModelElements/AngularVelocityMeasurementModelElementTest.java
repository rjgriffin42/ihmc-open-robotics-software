package us.ihmc.commonWalkingControlModules.stateEstimation.measurementModelElements;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.EjmlUnitTests;
import org.junit.Test;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.controlFlow.NullControlFlowElement;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.MatrixTools;
import us.ihmc.utilities.math.geometry.Direction;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTestTools.RandomFloatingChain;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class AngularVelocityMeasurementModelElementTest
{
   private static final Vector3d X = new Vector3d(1.0, 0.0, 0.0);
   private static final Vector3d Y = new Vector3d(0.0, 1.0, 0.0);
   private static final Vector3d Z = new Vector3d(0.0, 0.0, 1.0);

   @Test
   public void test()
   {
      Random random = new Random(1235L);
      Vector3d[] jointAxes = new Vector3d[] {X, Y, Z};
      RandomFloatingChain randomFloatingChain = new RandomFloatingChain(random, jointAxes);
      RigidBody elevator = randomFloatingChain.getElevator();

      RigidBody estimationLink = randomFloatingChain.getRootJoint().getSuccessor();
      ReferenceFrame estimationFrame = randomFloatingChain.getRootJoint().getFrameAfterJoint();
      RigidBody measurementLink = randomFloatingChain.getRevoluteJoints().get(jointAxes.length - 1).getSuccessor();
      ReferenceFrame measurementFrame = measurementLink.getBodyFixedFrame();

      ControlFlowElement controlFlowElement = new NullControlFlowElement();

      ControlFlowOutputPort<FrameVector> angularVelocityStatePort = new ControlFlowOutputPort<FrameVector>(controlFlowElement);
      ControlFlowOutputPort<FrameVector> biasStatePort = new ControlFlowOutputPort<FrameVector>(controlFlowElement);
      ControlFlowInputPort<Vector3d> angularVelocityMeasurementInputPort = new ControlFlowInputPort<Vector3d>(controlFlowElement);

      TwistCalculator twistCalculator = new TwistCalculator(elevator.getBodyFixedFrame(), randomFloatingChain.getElevator());
      String name = "test";
      YoVariableRegistry registry = new YoVariableRegistry(name);
      AngularVelocityMeasurementModelElement modelElement = new AngularVelocityMeasurementModelElement(angularVelocityStatePort, biasStatePort,
                                                               angularVelocityMeasurementInputPort, estimationLink, estimationFrame, measurementLink,
                                                               measurementFrame, twistCalculator, name, registry);

      randomFloatingChain.setRandomPositionsAndVelocities(random);
      twistCalculator.compute();

      FrameVector measuredAngularVelocity = getAngularVelocity(twistCalculator, measurementLink, measurementFrame);
      FrameVector bias = new FrameVector(measurementFrame, RandomTools.generateRandomVector(random));
      measuredAngularVelocity.add(bias);
      angularVelocityMeasurementInputPort.setData(measuredAngularVelocity.getVectorCopy());

      biasStatePort.setData(bias);
      FrameVector angularVelocityOfEstimationLink = getAngularVelocity(twistCalculator, estimationLink, estimationFrame);
      angularVelocityStatePort.setData(angularVelocityOfEstimationLink);

      modelElement.computeMatrixBlocks();

      DenseMatrix64F zeroResidual = modelElement.computeResidual();
      DenseMatrix64F zeroVector = new DenseMatrix64F(3, 1);
      EjmlUnitTests.assertEquals(zeroVector, zeroResidual, 1e-12);

      double perturbation = 1e-5;
      double tol = 1e-12;

      // bias perturbations
      DenseMatrix64F biasOutputMatrixBlock = modelElement.getOutputMatrixBlock(biasStatePort);
      for (Direction direction : Direction.values())
      {
         FrameVector perturbationVector = new FrameVector(measurementFrame);
         perturbationVector.set(direction, perturbation);

         DenseMatrix64F perturbationEjmlVector = new DenseMatrix64F(3, 1);
         MatrixTools.setDenseMatrixFromTuple3d(perturbationEjmlVector, perturbationVector.getVector(), 0, 0);

         FrameVector perturbedBias = new FrameVector(bias);
         perturbedBias.add(perturbationVector);
         biasStatePort.setData(perturbedBias);

         MeasurementModelTestTools.assertDeltaResidualCorrect(modelElement, biasOutputMatrixBlock, perturbationEjmlVector, tol);
      }

      biasStatePort.setData(bias);

      // angular velocity perturbations
      DenseMatrix64F angularVelocityOutputMatrixBlock = modelElement.getOutputMatrixBlock(angularVelocityStatePort);
      for (Direction direction : Direction.values())
      {
         FrameVector perturbationVector = new FrameVector(estimationFrame);
         perturbationVector.set(direction, perturbation);

         DenseMatrix64F perturbationEjmlVector = new DenseMatrix64F(3, 1);
         MatrixTools.setDenseMatrixFromTuple3d(perturbationEjmlVector, perturbationVector.getVector(), 0, 0);

         FrameVector perturbedAngularVelocity = new FrameVector(angularVelocityOfEstimationLink);
         perturbedAngularVelocity.add(perturbationVector);
         angularVelocityStatePort.setData(perturbedAngularVelocity);

         MeasurementModelTestTools.assertDeltaResidualCorrect(modelElement, angularVelocityOutputMatrixBlock, perturbationEjmlVector, tol);
      }
   }

   private FrameVector getAngularVelocity(TwistCalculator twistCalculator, RigidBody rigidBody, ReferenceFrame referenceFrame)
   {
      Twist twist = new Twist();
      twistCalculator.packTwistOfBody(twist, rigidBody);
      twist.changeFrame(referenceFrame);
      FrameVector ret = new FrameVector(referenceFrame);
      twist.packAngularPart(ret);

      return ret;
   }
}
