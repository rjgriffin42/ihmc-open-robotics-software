package us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.taskspace;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.NormOps;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MotionQPInput;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MotionQPInputCalculator;
import us.ihmc.commons.RandomNumbers;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.tools.EuclidCoreRandomTools;
import us.ihmc.robotics.controllers.OrientationPIDGains;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.RevoluteJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTestTools;
import us.ihmc.robotics.screwTheory.ScrewTestTools.RandomFloatingChain;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.TwistCalculator;

public class OrientationFeedbackControllerTest
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @ContinuousIntegrationTest(estimatedDuration = 0.5)
   @Test(timeout = 30000)
   public void testCompareAgainstSpatialController() throws Exception
   {
      Random random = new Random(5641654L);

      YoVariableRegistry registry = new YoVariableRegistry("Dummy");
      int numberOfRevoluteJoints = 10;
      RandomFloatingChain randomFloatingChain = new RandomFloatingChain(random, numberOfRevoluteJoints);
      List<RevoluteJoint> joints = randomFloatingChain.getRevoluteJoints();
      RigidBody elevator = randomFloatingChain.getElevator();
      RigidBody endEffector = joints.get(joints.size() - 1).getSuccessor();

      ReferenceFrame centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMassFrame", worldFrame, elevator);
      TwistCalculator twistCalculator = new TwistCalculator(worldFrame, elevator);
      InverseDynamicsJoint[] jointsToOptimizeFor = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      double controlDT = 0.004;

      WholeBodyControlCoreToolbox toolbox = new WholeBodyControlCoreToolbox(controlDT, 0.0, null, jointsToOptimizeFor, centerOfMassFrame, twistCalculator, null,
                                                                            null, registry);
      toolbox.setupForInverseDynamicsSolver(null);
      FeedbackControllerToolbox feedbackControllerToolbox = new FeedbackControllerToolbox(registry);
      OrientationFeedbackController orientationFeedbackController = new OrientationFeedbackController(endEffector, toolbox, feedbackControllerToolbox, registry);
      orientationFeedbackController.setEnabled(true);

      OrientationFeedbackControlCommand orientationFeedbackControlCommand = new OrientationFeedbackControlCommand();
      orientationFeedbackControlCommand.set(elevator, endEffector);
      OrientationPIDGains orientationGains = new OrientationPIDGains();

      SpatialFeedbackController spatialFeedbackController = new SpatialFeedbackController(endEffector, toolbox, feedbackControllerToolbox, registry);
      spatialFeedbackController.setEnabled(true);

      SpatialFeedbackControlCommand spatialFeedbackControlCommand = new SpatialFeedbackControlCommand();
      spatialFeedbackControlCommand.set(elevator, endEffector);
      spatialFeedbackControlCommand.getSpatialAccelerationCommand().setSelectionMatrixForAngularControl();

      MotionQPInputCalculator motionQPInputCalculator = new MotionQPInputCalculator(centerOfMassFrame, twistCalculator, toolbox.getJointIndexHandler(), null,
                                                                                    registry);
      MotionQPInput orientationMotionQPInput = new MotionQPInput(toolbox.getJointIndexHandler().getNumberOfDoFs());
      MotionQPInput spatialMotionQPInput = new MotionQPInput(toolbox.getJointIndexHandler().getNumberOfDoFs());

      SpatialAccelerationCommand orientationControllerOutput = orientationFeedbackController.getInverseDynamicsOutput();
      SpatialAccelerationCommand spatialControllerOutput = spatialFeedbackController.getInverseDynamicsOutput();

      for (int i = 0; i < 300; i++)
      {
         ScrewTestTools.setRandomPositions(joints, random);
         ScrewTestTools.setRandomVelocities(joints, random);
         joints.get(0).getPredecessor().updateFramesRecursively();
         centerOfMassFrame.update();
         twistCalculator.compute();

         double proportionalGain = RandomNumbers.nextDouble(random, 10.0, 200.0);
         double derivativeGain = RandomNumbers.nextDouble(random, 0.0, 100.0);
         double integralGain = 0.0;// RandomNumbers.nextDouble(random, 0.0, 100.0); //TODO difference in the implementation of the error accumulation.
         double maxIntegralError = 0.0; //RandomNumbers.nextDouble(random, 0.0, 10.0);
         orientationGains.setGains(proportionalGain, derivativeGain, integralGain, maxIntegralError);
         orientationGains.setMaximumProportionalError(RandomNumbers.nextDouble(random, 0.0, 10.0));
         orientationGains.setMaximumDerivativeError(RandomNumbers.nextDouble(random, 0.0, 10.0));
         orientationGains.setMaximumFeedbackAndFeedbackRate(RandomNumbers.nextDouble(random, 0.1, 10.0), Double.POSITIVE_INFINITY); // TODO the rate limitation is not applied in the same frame. Need to determine which frame is better. 
         orientationFeedbackControlCommand.setGains(orientationGains);
         spatialFeedbackControlCommand.setGains(orientationGains);

         FrameOrientation desiredOrientation = new FrameOrientation(worldFrame, EuclidCoreRandomTools.generateRandomQuaternion(random));
         FrameVector desiredAngularVelocity = new FrameVector(worldFrame, EuclidCoreRandomTools.generateRandomVector3D(random, -10.0, 10.0));
         FrameVector feedForwardAngularAcceleration = new FrameVector(worldFrame, EuclidCoreRandomTools.generateRandomVector3D(random, -10.0, 10.0));

         orientationFeedbackControlCommand.set(desiredOrientation, desiredAngularVelocity, feedForwardAngularAcceleration);
         spatialFeedbackControlCommand.set(desiredOrientation, desiredAngularVelocity, feedForwardAngularAcceleration);

         spatialFeedbackController.submitFeedbackControlCommand(spatialFeedbackControlCommand);
         orientationFeedbackController.submitFeedbackControlCommand(orientationFeedbackControlCommand);

         spatialFeedbackController.computeInverseDynamics();
         orientationFeedbackController.computeInverseDynamics();

         motionQPInputCalculator.convertSpatialAccelerationCommand(orientationControllerOutput, orientationMotionQPInput);
         motionQPInputCalculator.convertSpatialAccelerationCommand(spatialControllerOutput, spatialMotionQPInput);

         assertEquals(spatialMotionQPInput.taskJacobian, orientationMotionQPInput.taskJacobian, 1.0e-12);
         assertEquals(spatialMotionQPInput.taskObjective, orientationMotionQPInput.taskObjective, 1.0e-12);
         assertEquals(spatialMotionQPInput.taskWeightMatrix, orientationMotionQPInput.taskWeightMatrix, 1.0e-12);
      }
   }

   private static void assertEquals(DenseMatrix64F expected, DenseMatrix64F actual, double epsilon)
   {
      assertTrue(assertErrorMessage(expected, actual), MatrixFeatures.isEquals(expected, actual, epsilon));
   }

   private static String assertErrorMessage(DenseMatrix64F expected, DenseMatrix64F actual)
   {
      DenseMatrix64F diff = new DenseMatrix64F(expected.getNumRows(), expected.getNumCols());
      CommonOps.subtract(expected, actual, diff);
      return "Expected:\n" + expected + "\nActual:\n" + actual + ", difference: " + NormOps.normP2(diff);
   }
}
