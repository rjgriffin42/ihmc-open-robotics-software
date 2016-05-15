package us.ihmc.commonWalkingControlModules.controllerCore;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.After;
import org.junit.Test;
import us.ihmc.commonWalkingControlModules.controllerCore.VirtualModelControllerTestHelper.RobotLegs;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualWrenchCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.GeometricJacobianHolder;
import us.ihmc.commonWalkingControlModules.virtualModelControl.VirtualModelControlSolution;
import us.ihmc.commonWalkingControlModules.virtualModelControl.VirtualModelController;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.*;
import us.ihmc.simulationconstructionset.ExternalForcePoint;
import us.ihmc.simulationconstructionset.RobotTools.SCSRobotFromInverseDynamicsRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanTarget;
import us.ihmc.tools.thread.ThreadTools;

import javax.vecmath.Vector3d;
import java.util.*;

@DeployableTestClass(targets = TestPlanTarget.Fast)
public class VirtualModelControllerTest
{
   private final Random bigRandom = new Random(1000L);
   private final Random random = new Random();
   private final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private boolean hasSCSSimulation = false;

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testJacobianCalculation()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench wrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      InverseDynamicsJoint[] controlledJoints = ScrewTools.createJointPath(pelvis, foot);
      GeometricJacobian jacobian = new GeometricJacobian(controlledJoints, pelvis.getBodyFixedFrame());
      jacobian.compute();

      DenseMatrix64F jacobianMatrix = jacobian.getJacobianMatrix();
      DenseMatrix64F transposeJacobianMatrix = new DenseMatrix64F(jacobianMatrix.numCols, Wrench.SIZE);
      CommonOps.transpose(jacobianMatrix, transposeJacobianMatrix);

      wrench.changeFrame(pelvis.getBodyFixedFrame());
      DenseMatrix64F wrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
      wrenchMatrix.set(0, 0, wrench.getAngularPartX());
      wrenchMatrix.set(1, 0, wrench.getAngularPartY());
      wrenchMatrix.set(2, 0, wrench.getAngularPartZ());
      wrenchMatrix.set(3, 0, wrench.getLinearPartX());
      wrenchMatrix.set(4, 0, wrench.getLinearPartY());
      wrenchMatrix.set(5, 0, wrench.getLinearPartZ());

      DenseMatrix64F jointEffort = new DenseMatrix64F(controlledJoints.length, 1);
      CommonOps.multTransA(jacobianMatrix, wrenchMatrix, jointEffort);

      desiredForce.changeFrame(foot.getBodyFixedFrame());
      wrench.changeFrame(foot.getBodyFixedFrame());

      DenseMatrix64F appliedWrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
      CommonOps.invert(transposeJacobianMatrix);
      CommonOps.mult(transposeJacobianMatrix, jointEffort, appliedWrenchMatrix);
      Wrench appliedWrench = new Wrench(foot.getBodyFixedFrame(), jacobian.getJacobianFrame(), appliedWrenchMatrix);
      appliedWrench.changeFrame(foot.getBodyFixedFrame());

      VirtualModelControllerTestHelper.compareWrenches(wrench, appliedWrench);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMC()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame and no selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      submitAndCheckVMC(pelvis, foot, desiredWrench, null);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectAll()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      submitAndCheckVMC(pelvis, foot, desiredWrench, CommonOps.identity(Wrench.SIZE, Wrench.SIZE));
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForce()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only force
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 3, 1);
      selectionMatrix.set(1, 4, 1);
      selectionMatrix.set(2, 5, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectTorque()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 1, 1);
      selectionMatrix.set(2, 2, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForceX()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(1, 6);
      selectionMatrix.set(0, 3, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForceY()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(1, 6);
      selectionMatrix.set(0, 4, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForceZ()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(1, 6);
      selectionMatrix.set(0, 5, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectTorqueX()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(1, 6);
      selectionMatrix.set(0, 0, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectTorqueY()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(1, 6);
      selectionMatrix.set(0, 1, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectTorqueZ()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(1, 6);
      selectionMatrix.set(0, 2, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForceXTorqueY()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(2, 6);
      selectionMatrix.set(0, 1, 1);
      selectionMatrix.set(1, 4, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForceYZTorqueX()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 4, 1);
      selectionMatrix.set(2, 5, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCSelectForceXTorqueXZ()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCWrongExpressedInFrame()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), pelvis.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCWrongExpressedOnFrame()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(pelvis.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCWrongExpressedInAndOnFrame()
   {
      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(pelvis.getBodyFixedFrame(), pelvis.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      // select only torque
      DenseMatrix64F selectionMatrix = new DenseMatrix64F(3, 6);
      selectionMatrix.set(0, 0, 1);
      selectionMatrix.set(1, 2, 1);
      selectionMatrix.set(2, 3, 1);

      submitAndCheckVMC(pelvis, foot, desiredWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 1500)
   public void testVMCVirtualWrenchCommand()
   {
      simulationTestingParameters.setKeepSCSUp(false);
      hasSCSSimulation = false;

      double gravity = -9.81;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      VirtualModelControllerTestHelper.RobotLegs robotLeg = testHelper.createRobotLeg(gravity);
      RigidBody endEffector = robotLeg.getFoot(RobotSide.LEFT);
      RigidBody foot = endEffector.getParentJoint().getSuccessor();
      RigidBody pelvis = robotLeg.getRootJoint().getSuccessor();

      // send in the correct frame with identity selection matrix
      FrameVector desiredForce = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      FrameVector desiredTorque = new FrameVector(foot.getBodyFixedFrame(), new Vector3d(bigRandom.nextDouble(), bigRandom.nextDouble(), bigRandom.nextDouble()));
      Wrench desiredWrench = new Wrench(foot.getBodyFixedFrame(), foot.getBodyFixedFrame(), desiredForce.getVector(), desiredTorque.getVector());

      DenseMatrix64F selectionMatrix = CommonOps.identity(Wrench.SIZE, Wrench.SIZE);

      InverseDynamicsJoint[] controlledJoints = ScrewTools.createJointPath(pelvis, endEffector);
      GeometricJacobian jacobian = new GeometricJacobian(controlledJoints, pelvis.getBodyFixedFrame());
      jacobian.compute();

      DenseMatrix64F jacobianMatrix = jacobian.getJacobianMatrix();
      DenseMatrix64F transposeJacobianMatrix = new DenseMatrix64F(jacobianMatrix.numCols, Wrench.SIZE);
      CommonOps.transpose(jacobianMatrix, transposeJacobianMatrix);
      CommonOps.invert(transposeJacobianMatrix);

      VirtualModelController virtualModelController = new VirtualModelController(new GeometricJacobianHolder(), pelvis, null, null);
      virtualModelController.registerEndEffector(pelvis, endEffector);

      VirtualWrenchCommand virtualWrenchCommand = new VirtualWrenchCommand();
      virtualWrenchCommand.set(foot, desiredWrench, selectionMatrix);

      virtualModelController.submitEndEffectorVirtualWrench(virtualWrenchCommand);

      // find jacobian transpose solution
      VirtualModelControlSolution virtualModelControlSolution = new VirtualModelControlSolution();
      virtualModelController.compute(virtualModelControlSolution);

      desiredWrench.changeFrame(pelvis.getBodyFixedFrame());

      // compute end effector force from torques
      Map<InverseDynamicsJoint, Double> jointTorques = virtualModelControlSolution.getJointTorques();
      DenseMatrix64F jointEffortMatrix = new DenseMatrix64F(controlledJoints.length, 1);
      for (int i = 0; i < controlledJoints.length; i++)
      {
         jointEffortMatrix.set(i, 0, jointTorques.get(controlledJoints[i]));
      }

      DenseMatrix64F appliedWrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
      CommonOps.mult(transposeJacobianMatrix, jointEffortMatrix, appliedWrenchMatrix);
      Wrench appliedWrench = new Wrench(endEffector.getBodyFixedFrame(), jacobian.getJacobianFrame(), appliedWrenchMatrix);

      VirtualModelControllerTestHelper.compareWrenches(desiredWrench, appliedWrench, selectionMatrix);
   }

   @DeployableTestMethod
   @Test(timeout = 300000000)
   public void testVMCWithArm() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(true);
      hasSCSSimulation = true;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      VirtualModelControllerTestHelper.RobotArm robotArm = testHelper.createRobotArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBody> endEffectors = new ArrayList<>();
      RigidBody hand = robotArm.getHand();
      endEffectors.add(hand);

      double forceX = random.nextDouble() * 10.0;
      double forceY = random.nextDouble() * 10.0;
      double forceZ = random.nextDouble() * 10.0;
      double torqueX = random.nextDouble() * 10.0;
      double torqueY = random.nextDouble() * 10.0;
      double torqueZ = random.nextDouble() * 10.0;
      Vector3d desiredForce = new Vector3d(forceX, forceY, forceZ);
      Vector3d desiredTorque = new Vector3d(torqueX, torqueY, torqueZ);

      List<Vector3d> desiredForces = new ArrayList<>();
      List<Vector3d> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce);
      desiredTorques.add(desiredTorque);

      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(robotArm.getExternalForcePoint());

      DenseMatrix64F selectionMatrix = CommonOps.identity(Wrench.SIZE, Wrench.SIZE);
      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, endEffectors, desiredForces, desiredTorques, externalForcePoints, selectionMatrix);

      simulationTestingParameters.setKeepSCSUp(false);
   }

   @DeployableTestMethod
   @Test(timeout = 3000000)
   public void testVMCWithPlanarArm() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(true);
      hasSCSSimulation = true;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      VirtualModelControllerTestHelper.PlanarRobotArm robotArm = testHelper.createPlanarArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBody> endEffectors = new ArrayList<>();
      RigidBody hand = robotArm.getHand();
      endEffectors.add(hand);

      double forceX = random.nextDouble() * 10.0;
      double forceZ = random.nextDouble() * 10.0;
      double torqueY = random.nextDouble() * 10.0;
      Vector3d desiredForce = new Vector3d(forceX, 0.0, forceZ);
      Vector3d desiredTorque = new Vector3d(0.0, torqueY, 0.0);

      List<Vector3d> desiredForces = new ArrayList<>();
      List<Vector3d> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce);
      desiredTorques.add(desiredTorque);

      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(robotArm.getExternalForcePoint());

      DenseMatrix64F selectionMatrix = CommonOps.identity(Wrench.SIZE, Wrench.SIZE);
      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, endEffectors, desiredForces, desiredTorques, externalForcePoints, selectionMatrix);

      simulationTestingParameters.setKeepSCSUp(false);
   }

   @DeployableTestMethod
   @Test(timeout = 3000000)
   public void testPlanarHydra() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(true);
      hasSCSSimulation = true;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      VirtualModelControllerTestHelper.PlanarForkedRobotArm robotArm = testHelper.createPlanarForkedRobotArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBody> endEffectors = new ArrayList<>();
      RigidBody leftHand = robotArm.getHand(RobotSide.LEFT);
      RigidBody rightHand = robotArm.getHand(RobotSide.RIGHT);
      endEffectors.add(leftHand);
      endEffectors.add(rightHand);

      double forceX = random.nextDouble() * 10.0;
      double forceZ = random.nextDouble() * 10.0;
      double torqueY = random.nextDouble() * 10.0;
      Vector3d desiredForce1 = new Vector3d(forceX, 0.0, forceZ);
      Vector3d desiredForce2 = new Vector3d(forceX, 0.0, forceZ);
      Vector3d desiredTorque1 = new Vector3d(0.0, torqueY, 0.0);
      Vector3d desiredTorque2 = new Vector3d(0.0, torqueY, 0.0);

      List<Vector3d> desiredForces = new ArrayList<>();
      List<Vector3d> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce1);
      desiredForces.add(desiredForce2);
      desiredTorques.add(desiredTorque1);
      desiredTorques.add(desiredTorque2);

      SideDependentList<ExternalForcePoint> sideDependentExternalForcePoints = robotArm.getExternalForcePoints();
      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.LEFT));
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.RIGHT));

      DenseMatrix64F selectionMatrix = CommonOps.identity(Wrench.SIZE, Wrench.SIZE);

      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, endEffectors, desiredForces, desiredTorques, externalForcePoints, selectionMatrix);

      simulationTestingParameters.setKeepSCSUp(false);
   }

   @DeployableTestMethod
   @Test(timeout = 3000000)
   public void testHydra() throws Exception
   {
      simulationTestingParameters.setKeepSCSUp(true);
      hasSCSSimulation = true;

      VirtualModelControllerTestHelper testHelper = new VirtualModelControllerTestHelper();
      VirtualModelControllerTestHelper.ForkedRobotArm robotArm = testHelper.createForkedRobotArm();
      SCSRobotFromInverseDynamicsRobotModel scsRobotArm = robotArm.getSCSRobotArm();

      List<RigidBody> endEffectors = new ArrayList<>();
      RigidBody leftHand = robotArm.getHand(RobotSide.LEFT);
      RigidBody rightHand = robotArm.getHand(RobotSide.RIGHT);
      endEffectors.add(leftHand);
      endEffectors.add(rightHand);

      double forceZ = random.nextDouble() * 10.0;
      Vector3d desiredForce1 = new Vector3d(0.0, 0.0, forceZ);
      Vector3d desiredForce2 = new Vector3d(0.0, 0.0, forceZ);
      Vector3d desiredTorque1 = new Vector3d(0.0, 0.0, 0.0);
      Vector3d desiredTorque2 = new Vector3d(0.0, 0.0, 0.0);

      List<Vector3d> desiredForces = new ArrayList<>();
      List<Vector3d> desiredTorques = new ArrayList<>();
      desiredForces.add(desiredForce1);
      desiredForces.add(desiredForce2);
      desiredTorques.add(desiredTorque1);
      desiredTorques.add(desiredTorque2);

      SideDependentList<ExternalForcePoint> sideDependentExternalForcePoints = robotArm.getExternalForcePoints();
      List<ExternalForcePoint> externalForcePoints = new ArrayList<>();
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.LEFT));
      externalForcePoints.add(sideDependentExternalForcePoints.get(RobotSide.RIGHT));

      DenseMatrix64F selectionMatrix = CommonOps.identity(Wrench.SIZE, Wrench.SIZE);

      VirtualModelControllerTestHelper.createVirtualModelControlTest(scsRobotArm, robotArm, endEffectors, desiredForces, desiredTorques, externalForcePoints, selectionMatrix);

      simulationTestingParameters.setKeepSCSUp(false);
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp() && hasSCSSimulation)
      {
         ThreadTools.sleepForever();
      }
   }

   private void submitAndCheckVMC(RigidBody base, RigidBody endEffector, Wrench desiredWrench, DenseMatrix64F selectionMatrix)
   {
      simulationTestingParameters.setKeepSCSUp(false);

      OneDoFJoint[] controlledJoints = ScrewTools.createOneDoFJointPath(base, endEffector);
      GeometricJacobian jacobian = new GeometricJacobian(controlledJoints, base.getBodyFixedFrame());
      jacobian.compute();

      DenseMatrix64F jacobianMatrix = jacobian.getJacobianMatrix();
      DenseMatrix64F transposeJacobianMatrix = new DenseMatrix64F(jacobianMatrix.numCols, Wrench.SIZE);
      CommonOps.transpose(jacobianMatrix, transposeJacobianMatrix);
      CommonOps.invert(transposeJacobianMatrix);

      VirtualModelController virtualModelController = new VirtualModelController(new GeometricJacobianHolder(), base, null, null);
      virtualModelController.registerEndEffector(base, endEffector);

      desiredWrench.changeFrame(base.getBodyFixedFrame());

      if (selectionMatrix == null)
         virtualModelController.submitEndEffectorVirtualWrench(endEffector, desiredWrench);
      else
         virtualModelController.submitEndEffectorVirtualWrench(endEffector, desiredWrench, selectionMatrix);

      // find jacobian transpose solution
      VirtualModelControlSolution virtualModelControlSolution = new VirtualModelControlSolution();
      virtualModelController.compute(virtualModelControlSolution);

      // compute end effector force from torques
      Map<InverseDynamicsJoint, Double> jointTorques = virtualModelControlSolution.getJointTorques();
      DenseMatrix64F jointEffortMatrix = new DenseMatrix64F(controlledJoints.length, 1);
      for (int i = 0; i < controlledJoints.length; i++)
      {
         jointEffortMatrix.set(i, 0, jointTorques.get(controlledJoints[i]));
      }

      DenseMatrix64F appliedWrenchMatrix = new DenseMatrix64F(Wrench.SIZE, 1);
      CommonOps.mult(transposeJacobianMatrix, jointEffortMatrix, appliedWrenchMatrix);
      Wrench appliedWrench = new Wrench(endEffector.getBodyFixedFrame(), jacobian.getJacobianFrame(), appliedWrenchMatrix);

      if (selectionMatrix == null)
         VirtualModelControllerTestHelper.compareWrenches(desiredWrench, appliedWrench);
      else
         VirtualModelControllerTestHelper.compareWrenches(desiredWrench, appliedWrench, selectionMatrix);
   }
}
