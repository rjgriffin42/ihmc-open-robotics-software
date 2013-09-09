package us.ihmc.sensorProcessing.stateEstimation.measurementModelElements;



import java.util.Random;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.EjmlUnitTests;
import org.junit.Test;

import us.ihmc.controlFlow.ControlFlowElement;
import us.ihmc.controlFlow.ControlFlowInputPort;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.controlFlow.NullControlFlowElement;
import us.ihmc.sensorProcessing.stateEstimation.OrientationAndPositionFullRobotModelUpdater;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.RigidBodyToIndexMap;
import us.ihmc.sensorProcessing.stateEstimation.measurmentModelElements.PointVelocityMeasurementModelElement;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocityDataObject;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTestTools.RandomFloatingChain;
import us.ihmc.utilities.screwTheory.AfterJointReferenceFrameNameMap;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class PointVelocityMeasurementModelElementTest
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
      SixDoFJoint rootJoint = randomFloatingChain.getRootJoint();

      RigidBody estimationLink = randomFloatingChain.getRootJoint().getSuccessor();
      ReferenceFrame estimationFrame = randomFloatingChain.getRootJoint().getFrameAfterJoint();
      RigidBody measurementLink = randomFloatingChain.getRevoluteJoints().get(jointAxes.length - 1).getSuccessor();
      ReferenceFrame measurementFrame = measurementLink.getParentJoint().getFrameAfterJoint();

      ControlFlowElement controlFlowElement = new NullControlFlowElement();

      FullInverseDynamicsStructure inverseDynamicsStructure = new FullInverseDynamicsStructure(elevator, estimationLink, rootJoint);
      ControlFlowInputPort<FullInverseDynamicsStructure> inverseDynamicsStructureInputPort =
         new ControlFlowInputPort<FullInverseDynamicsStructure>("inverseDynamicsStructureInputPort", controlFlowElement);
      inverseDynamicsStructureInputPort.setData(inverseDynamicsStructure);

      TwistCalculator twistCalculator = inverseDynamicsStructure.getTwistCalculator();
      SpatialAccelerationCalculator spatialAccelerationCalculator = inverseDynamicsStructure.getSpatialAccelerationCalculator();

      String name = "test";
      YoVariableRegistry registry = new YoVariableRegistry(name);

      ControlFlowInputPort<PointVelocityDataObject> pointVelocityMeasurementInputPort = new ControlFlowInputPort<PointVelocityDataObject>("pointVelocityMeasurementInputPort", controlFlowElement);

      ControlFlowOutputPort<FramePoint> centerOfMassPositionPort = new ControlFlowOutputPort<FramePoint>("centerOfMassPositionPort", controlFlowElement);
      ControlFlowOutputPort<FrameVector> centerOfMassVelocityPort = new ControlFlowOutputPort<FrameVector>("centerOfMassVelocityPort", controlFlowElement);
      ControlFlowOutputPort<FrameVector> centerOfMassAccelerationPort = new ControlFlowOutputPort<FrameVector>("centerOfMassAccelerationPort", controlFlowElement);

      ControlFlowOutputPort<FrameOrientation> orientationPort = new ControlFlowOutputPort<FrameOrientation>("orientationPort", controlFlowElement);
      ControlFlowOutputPort<FrameVector> angularVelocityPort = new ControlFlowOutputPort<FrameVector>("angularVelocityPort", controlFlowElement);
      ControlFlowOutputPort<FrameVector> angularAccelerationPort = new ControlFlowOutputPort<FrameVector>("angularAccelerationPort", controlFlowElement);

      RigidBody stationaryPointLink = measurementLink;
      FramePoint stationaryPoint = new FramePoint(measurementFrame, RandomTools.generateRandomPoint(random, 1.0, 1.0, 1.0));
      AfterJointReferenceFrameNameMap referenceFrameMap = new AfterJointReferenceFrameNameMap(elevator);
      RigidBodyToIndexMap rigidBodyToIndexMap = new RigidBodyToIndexMap(elevator);
      PointVelocityMeasurementModelElement modelElement = new PointVelocityMeasurementModelElement(name, pointVelocityMeasurementInputPort,
                                                             centerOfMassPositionPort, centerOfMassVelocityPort, orientationPort, angularVelocityPort,
                                                             estimationFrame, inverseDynamicsStructureInputPort,
                                                             referenceFrameMap, rigidBodyToIndexMap, false, registry);

      randomFloatingChain.setRandomPositionsAndVelocities(random);
      twistCalculator.compute();
      spatialAccelerationCalculator.compute();

      Runnable updater = new OrientationAndPositionFullRobotModelUpdater(inverseDynamicsStructureInputPort, centerOfMassPositionPort, centerOfMassVelocityPort,
                            centerOfMassAccelerationPort, orientationPort, angularVelocityPort, angularAccelerationPort);

      centerOfMassPositionPort.setData(new FramePoint(ReferenceFrame.getWorldFrame(), RandomTools.generateRandomVector(random)));
      centerOfMassVelocityPort.setData(new FrameVector(ReferenceFrame.getWorldFrame(), RandomTools.generateRandomVector(random)));
      centerOfMassAccelerationPort.setData(new FrameVector(ReferenceFrame.getWorldFrame(), RandomTools.generateRandomVector(random)));
      Matrix3d orientation = new Matrix3d();
      orientation.set(RandomTools.generateRandomRotation(random));
      orientationPort.setData(new FrameOrientation(ReferenceFrame.getWorldFrame(), orientation));
      angularVelocityPort.setData(new FrameVector(estimationFrame, RandomTools.generateRandomVector(random)));
      angularAccelerationPort.setData(new FrameVector(estimationFrame, RandomTools.generateRandomVector(random)));

      updater.run();

      setMeasuredPointVelocityToActual(inverseDynamicsStructure, twistCalculator, stationaryPointLink, stationaryPoint, pointVelocityMeasurementInputPort);

      DenseMatrix64F zeroResidual = modelElement.computeResidual();
      DenseMatrix64F zeroVector = new DenseMatrix64F(3, 1);
      EjmlUnitTests.assertEquals(zeroVector, zeroResidual, 1e-12);

      double perturbation = 1e-6;
      double tol = 1e-12;
      modelElement.computeMatrixBlocks();

      // CoM velocity perturbations
      MeasurementModelTestTools.assertOutputMatrixCorrectUsingPerturbation(centerOfMassVelocityPort, modelElement,
              new FrameVector(centerOfMassVelocityPort.getData()), perturbation, tol, updater);

      // angular velocity perturbations
      MeasurementModelTestTools.assertOutputMatrixCorrectUsingPerturbation(angularVelocityPort, modelElement, new FrameVector(angularVelocityPort.getData()),
              perturbation, tol, updater);

      // orientation perturbations
      MeasurementModelTestTools.assertOutputMatrixCorrectUsingPerturbation(orientationPort, modelElement, new FrameOrientation(orientationPort.getData()),
              perturbation, tol, updater);
   }

   private void setMeasuredPointVelocityToActual(FullInverseDynamicsStructure inverseDynamicsStructure, TwistCalculator twistCalculator, RigidBody stationaryPointLink, FramePoint measurementPointInBodyFrame,
           ControlFlowInputPort<PointVelocityDataObject> pointVelocityMeasurementInputPort)
   {
      Twist twist = new Twist();
      twistCalculator.packTwistOfBody(twist, stationaryPointLink);
      twist.changeFrame(twist.getBaseFrame());
      FramePoint pointInTwistBaseFrame = measurementPointInBodyFrame.changeFrameCopy(twist.getBaseFrame());
      FrameVector velocityOfMeasurementPointInWorldFrame = new FrameVector(twist.getBaseFrame());
      twist.packVelocityOfPointFixedInBodyFrame(velocityOfMeasurementPointInWorldFrame, pointInTwistBaseFrame);
      velocityOfMeasurementPointInWorldFrame.changeFrame(ReferenceFrame.getWorldFrame());
      
      PointVelocityDataObject pointVelocityDataObject = new PointVelocityDataObject();
      pointVelocityDataObject.set(stationaryPointLink, measurementPointInBodyFrame, velocityOfMeasurementPointInWorldFrame);
   
      pointVelocityMeasurementInputPort.setData(pointVelocityDataObject);
   }
}
