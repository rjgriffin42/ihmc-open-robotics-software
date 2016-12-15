package us.ihmc.avatar.behaviorTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.controllerAPI.EndToEndChestTrajectoryMessageTest;
import us.ihmc.avatar.controllerAPI.EndToEndHandTrajectoryMessageTest;
import us.ihmc.avatar.controllerAPI.EndToEndPelvisTrajectoryMessageTest;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.kinematicsToolboxModule.KinematicsToolboxModule;
import us.ihmc.avatar.testTools.DRCBehaviorTestHelper;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlModule;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.humanoidBehaviors.behaviors.primitives.WholeBodyInverseKinematicsBehavior;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.time.GlobalTimer;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.environments.CommonAvatarEnvironmentInterface;
import us.ihmc.simulationconstructionset.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.thread.ThreadTools;

public abstract class WholeBodyInverseKinematicsBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
   private boolean isKinematicsToolboxVisualizerEnabled = false;
   private DRCBehaviorTestHelper drcBehaviorTestHelper;
   private KinematicsToolboxModule kinematicsToolboxModule;
   private PacketCommunicator toolboxCommunicator;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }
      
      if (kinematicsToolboxModule != null)
      {
         kinematicsToolboxModule.destroy();
         kinematicsToolboxModule = null;
      }

      if (toolboxCommunicator != null)
      {
         toolboxCommunicator.close();
         toolboxCommunicator.closeConnection();
         toolboxCommunicator = null;
      }

      GlobalTimer.clearTimers();

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   @Before
   public void setUp() throws IOException
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      CommonAvatarEnvironmentInterface envrionment = new FlatGroundEnvironment();

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(envrionment, getSimpleRobotName(), null, simulationTestingParameters, getRobotModel());

      setupKinematicsToolboxModule();
   }
   
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests if pelvis and chest are not affected (change in position & orientation) by a hand movement of 20cm in the positive x direction
   @Test(timeout = 160000)
   public void testSolvingForAHandPose() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      RobotSide robotSide = RobotSide.RIGHT;

      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      String nameSpace = robotSide.getCamelCaseNameForStartOfExpression() + HandControlModule.class.getSimpleName();
      String varname = nameSpace + "SwitchTime";
      double initialSwitchTime = scs.getVariable(nameSpace, varname).getValueAsDouble();

      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());

      ReferenceFrame handControlFrame = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(robotSide);

      ReferenceFrame chestControlFrame = drcBehaviorTestHelper.getControllerFullRobotModel().getChest().getBodyFixedFrame();
      FrameOrientation initialChestOrientation = new FrameOrientation(chestControlFrame);
      initialChestOrientation.changeFrame(ReferenceFrame.getWorldFrame());

      ReferenceFrame pelvisControlFrame = drcBehaviorTestHelper.getControllerFullRobotModel().getPelvis().getBodyFixedFrame();
      FrameOrientation initialPelvisOrientation = new FrameOrientation(pelvisControlFrame);
      initialPelvisOrientation.changeFrame(ReferenceFrame.getWorldFrame());

      FramePose desiredHandPose = new FramePose(handControlFrame);
      desiredHandPose.changeFrame(ReferenceFrame.getWorldFrame());
      desiredHandPose.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setDesiredHandPose(robotSide, desiredHandPose);
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      double newSwitchTime = scs.getVariable(nameSpace, varname).getValueAsDouble();

      assertNotEquals(initialSwitchTime, newSwitchTime, 1.0e-3);

      Quat4d controllerDesiredChestOrientation = EndToEndChestTrajectoryMessageTest.findControllerDesiredOrientation(scs);
      Quat4d controllerDesiredPelvisOrientation = EndToEndPelvisTrajectoryMessageTest.findControllerDesiredOrientation(scs);

      double angleEpsilon = Math.toRadians(10);

      assertTrue(isOrientationEqual(initialChestOrientation.getQuaternion(), controllerDesiredChestOrientation, angleEpsilon));
      assertTrue(isOrientationEqual(initialPelvisOrientation.getQuaternion(), controllerDesiredPelvisOrientation, angleEpsilon));

      Point3d controllerDesiredHandPosition = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(robotSide, scs);

      Point3d handPosition = new Point3d();
      desiredHandPose.getPosition(handPosition);

      double positionEpsilon = 1.0e-4;
      double positionDifference = handPosition.distance(controllerDesiredHandPosition);
      
      assertTrue("Position difference: " + positionDifference, positionDifference <positionEpsilon);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests position and orientation of the two hands after a movement of 20cm in the positive x direction
   @Test(timeout = 160000)
   public void testSolvingForBothHandPoses() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);
      
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());

      ReferenceFrame handControlFrameR = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(RobotSide.RIGHT);
      ReferenceFrame handControlFrameL = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(RobotSide.LEFT);

      FramePose desiredHandPoseR = new FramePose(handControlFrameR);
      desiredHandPoseR.changeFrame(ReferenceFrame.getWorldFrame());
      desiredHandPoseR.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setDesiredHandPose(RobotSide.RIGHT, desiredHandPoseR);
      FramePose desiredHandPoseL = new FramePose(handControlFrameL);
      desiredHandPoseL.changeFrame(ReferenceFrame.getWorldFrame());
      desiredHandPoseL.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setDesiredHandPose(RobotSide.LEFT, desiredHandPoseL);
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      Quat4d controllerDesiredHandOrientationR = EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(RobotSide.RIGHT, scs);
      Quat4d desiredHandOrientationR = new Quat4d();
      desiredHandPoseR.getOrientation(desiredHandOrientationR);
      Quat4d controllerDesiredHandOrientationL = EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(RobotSide.LEFT, scs);
      Quat4d desiredHandOrientationL = new Quat4d();
      desiredHandPoseL.getOrientation(desiredHandOrientationL);

      double handAngleEpsilon = Math.toRadians(1);

      assertTrue(isOrientationEqual(desiredHandOrientationR, controllerDesiredHandOrientationR, handAngleEpsilon));
      assertTrue(isOrientationEqual(desiredHandOrientationL, controllerDesiredHandOrientationL, handAngleEpsilon));
      
      Point3d controllerDesiredHandPositionR = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(RobotSide.RIGHT, scs);
      Point3d controllerDesiredHandPositionL = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(RobotSide.LEFT, scs);
      Point3d rightPosition = new Point3d();
      desiredHandPoseR.getPosition(rightPosition);
      Point3d leftPosition = new Point3d();
      desiredHandPoseL.getPosition(leftPosition);
      double rightDifference = rightPosition.distance(controllerDesiredHandPositionR);
      double leftDifference = leftPosition.distance(controllerDesiredHandPositionL);

      double positionEpsilon = 1.0e-4;
      
      assertTrue("Position difference: " + rightDifference, rightDifference <positionEpsilon);
      assertTrue("Position difference: " + leftDifference, leftDifference <positionEpsilon);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests the selection matrix of a hand, sets a desired roll offset and makes sure it is reached
   @Test(timeout = 160000)
   public void testSolvingForHandSelectionMatrix() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      RobotSide robotSide = RobotSide.RIGHT;

      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      String nameSpace = robotSide.getCamelCaseNameForStartOfExpression() + HandControlModule.class.getSimpleName();
      String varname = nameSpace + "SwitchTime";
      double initialSwitchTime = scs.getVariable(nameSpace, varname).getValueAsDouble();

      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());

      ReferenceFrame handControlFrame = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(robotSide);

      ReferenceFrame chestControlFrame = drcBehaviorTestHelper.getControllerFullRobotModel().getChest().getBodyFixedFrame();
      FrameOrientation initialChestOrientation = new FrameOrientation(chestControlFrame);
      initialChestOrientation.changeFrame(ReferenceFrame.getWorldFrame());

      Quat4d offsetOrientation = new Quat4d();
      RotationTools.convertYawPitchRollToQuaternion(0.0, 0.0, 0.1, offsetOrientation);
      FramePose desiredHandPose = new FramePose(handControlFrame);
      desiredHandPose.setOrientation(offsetOrientation);
      desiredHandPose.changeFrame(ReferenceFrame.getWorldFrame());
      desiredHandPose.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setDesiredHandPose(robotSide, desiredHandPose);
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      double newSwitchTime = scs.getVariable(nameSpace, varname).getValueAsDouble();

      assertNotEquals(initialSwitchTime, newSwitchTime, 1.0e-3);

      Quat4d controllerDesiredHandOrientation = EndToEndHandTrajectoryMessageTest.findControllerDesiredOrientation(robotSide, scs);
      Quat4d desiredHandOrientation = new Quat4d();
      desiredHandPose.getOrientation(desiredHandOrientation);

      double handAngleEpsilon = Math.toRadians(1);

      assertTrue(isOrientationEqual(desiredHandOrientation, controllerDesiredHandOrientation, handAngleEpsilon));
      
      Quat4d controllerDesiredChestOrientation = EndToEndChestTrajectoryMessageTest.findControllerDesiredOrientation(scs);

      double chestAngleEpsilon = Math.toRadians(10);

      assertTrue(isOrientationEqual(initialChestOrientation.getQuaternion(), controllerDesiredChestOrientation, chestAngleEpsilon));
      
      Point3d controllerDesiredHandPosition = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(robotSide, scs);

      Point3d handPosition = new Point3d();
      desiredHandPose.getPosition(handPosition);

      double positionEpsilon = 1.0e-4;
      double positionDifference = handPosition.distance(controllerDesiredHandPosition);
      
      assertTrue("Position difference: " + positionDifference, positionDifference <positionEpsilon);

      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests the method setHandLinearControlOnly, sets desired angular offsets on both hands and makes sure they are not reached
   @Test(timeout = 160000)
   public void testSolvingForHandAngularLinearControl() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);
      
      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());

      ReferenceFrame handControlFrameR = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(RobotSide.RIGHT);
      ReferenceFrame handControlFrameL = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(RobotSide.LEFT);

      Quat4d offsetOrientationRight = new Quat4d();
      RotationTools.convertYawPitchRollToQuaternion(0.0, 0.0, 1.0, offsetOrientationRight);
      FramePose desiredHandPoseR = new FramePose(handControlFrameR);
      desiredHandPoseR.changeFrame(ReferenceFrame.getWorldFrame());
      
      Quat4d handQuatRight = new Quat4d();
      desiredHandPoseR.getOrientation(handQuatRight);
      handQuatRight.mul(handQuatRight, offsetOrientationRight);
      desiredHandPoseR.setOrientation(handQuatRight);
      desiredHandPoseR.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setHandLinearControlOnly(RobotSide.RIGHT);
      ik.setDesiredHandPose(RobotSide.RIGHT, desiredHandPoseR);

      
      Quat4d offsetOrientationLeft = new Quat4d();
      RotationTools.convertYawPitchRollToQuaternion(1.0, 1.0, 0.0, offsetOrientationLeft);
      FramePose desiredHandPoseL = new FramePose(handControlFrameL);
      desiredHandPoseL.changeFrame(ReferenceFrame.getWorldFrame());
      
      Quat4d handQuatLeft = new Quat4d();
      desiredHandPoseL.getOrientation(handQuatLeft);
      handQuatLeft.mul(handQuatLeft, offsetOrientationLeft);
      desiredHandPoseL.setOrientation(handQuatLeft);
      desiredHandPoseL.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setHandLinearControlOnly(RobotSide.LEFT);
      ik.setDesiredHandPose(RobotSide.LEFT, desiredHandPoseL);
      
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      FramePose currentHandPoseR = new FramePose(handControlFrameR);
      currentHandPoseR.changeFrame(ReferenceFrame.getWorldFrame());
      double currentRollR = currentHandPoseR.getRoll();
      FramePose currentHandPoseL = new FramePose(handControlFrameL);
      currentHandPoseL.changeFrame(ReferenceFrame.getWorldFrame());
      double currentYawL = currentHandPoseL.getYaw();
      double currentPitchL = currentHandPoseL.getPitch();
      
      double angleEpsilon = Math.toRadians(2); 

      assertNotEquals("Current roll: " + currentRollR, currentRollR, desiredHandPoseR.getRoll(), angleEpsilon);
      assertNotEquals("Current yaw: " + currentYawL, currentYawL, desiredHandPoseL.getYaw(), angleEpsilon); 
      assertNotEquals("Current pitch: " + currentPitchL, currentPitchL, desiredHandPoseL.getPitch(), angleEpsilon); 

      Point3d controllerDesiredHandPositionR = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(RobotSide.RIGHT, scs);
      Point3d controllerDesiredHandPositionL = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(RobotSide.LEFT, scs);
      Point3d rightPosition = new Point3d();
      desiredHandPoseR.getPosition(rightPosition);
      Point3d leftPosition = new Point3d();
      desiredHandPoseL.getPosition(leftPosition);
      double rightDifference = rightPosition.distance(controllerDesiredHandPositionR);
      double leftDifference = leftPosition.distance(controllerDesiredHandPositionL);

      double positionEpsilon = 1.0e-4;
      
      assertTrue("Position difference: " + rightDifference, rightDifference <positionEpsilon);
      assertTrue("Position difference: " + leftDifference, leftDifference <positionEpsilon);
      
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests the method setHandLinearControlAndYawPitchOnly, sets a desired roll offset on one hand and makes sure it is not reached
   @Test(timeout = 160000)
   public void testSolvingForHandRollConstraint() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());

      ReferenceFrame handControlFrame = drcBehaviorTestHelper.getReferenceFrames().getHandFrame(RobotSide.RIGHT);

      Quat4d offsetOrientation = new Quat4d();
      RotationTools.convertYawPitchRollToQuaternion(0.0, 0.0, 1.0, offsetOrientation);
      FramePose desiredHandPose = new FramePose(handControlFrame);
      desiredHandPose.changeFrame(ReferenceFrame.getWorldFrame());
      
      Quat4d handQuat = new Quat4d();
      desiredHandPose.getOrientation(handQuat);
      handQuat.mul(handQuat, offsetOrientation);
      desiredHandPose.setOrientation(handQuat);
      desiredHandPose.translate(0.20, 0.0, 0.0);
      ik.setTrajectoryTime(0.5);
      ik.setHandLinearControlAndYawPitchOnly(RobotSide.RIGHT);
      ik.setDesiredHandPose(RobotSide.RIGHT, desiredHandPose);
      
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SimulationConstructionSet scs = drcBehaviorTestHelper.getSimulationConstructionSet();
      Point3d controllerDesiredHandPosition = EndToEndHandTrajectoryMessageTest.findControllerDesiredPosition(RobotSide.RIGHT, scs);
      
      FramePose currentHandPose = new FramePose(handControlFrame);
      currentHandPose.changeFrame(ReferenceFrame.getWorldFrame());
      double currentRoll = currentHandPose.getRoll();
      
      double angleEpsilon = Math.toRadians(5); 

      assertNotEquals("Current roll " + currentRoll, currentRoll, desiredHandPose.getRoll(), angleEpsilon); 

      Point3d handPosition = new Point3d();
      desiredHandPose.getPosition(handPosition);

      double positionEpsilon = 1.0e-4;
      double positionDifference = handPosition.distance(controllerDesiredHandPosition);
      
      assertTrue("Position difference: " + positionDifference, positionDifference <positionEpsilon);
      
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests the selection matrix of the chest
   @Test(timeout = 160000)
   public void testSolvingForChestAngularControl() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());
      
      Quat4d offsetOrientationChest = new Quat4d();
      RotationTools.convertYawPitchRollToQuaternion(0.3, 0.0, 0.1, offsetOrientationChest);
      ReferenceFrame chestControlFrame = drcBehaviorTestHelper.getControllerFullRobotModel().getChest().getBodyFixedFrame();
      FrameOrientation desiredChestOrientation = new FrameOrientation(chestControlFrame);
      double initialChestPitch = desiredChestOrientation.getPitch();
      double initialChestYaw = desiredChestOrientation.getYaw();
      desiredChestOrientation.set(offsetOrientationChest);
      desiredChestOrientation.changeFrame(ReferenceFrame.getWorldFrame());
      ik.setTrajectoryTime(0.5);
      ik.setChestAngularControl(true, false, false);
      ik.setDesiredChestOrientation(desiredChestOrientation);   
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      FrameOrientation currentChestOrientation = new FrameOrientation(chestControlFrame);
      currentChestOrientation.changeFrame(ReferenceFrame.getWorldFrame());
      double currentChestRoll = currentChestOrientation.getRoll();
      double currentChestYaw = currentChestOrientation.getYaw();
      double currentChestPitch = currentChestOrientation.getPitch();
      
      double angleEpsilon = Math.toRadians(1);

      assertEquals("Expected: " + desiredChestOrientation.getRoll() + " Received: " + currentChestRoll, desiredChestOrientation.getRoll(), currentChestRoll, angleEpsilon); 
      assertEquals("Expected: " + initialChestYaw + " Received: " + currentChestYaw, initialChestYaw, currentChestYaw, angleEpsilon); 
      assertEquals("Expected: " + initialChestPitch + " Received: " + currentChestPitch, initialChestPitch, currentChestPitch, angleEpsilon); 
      
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   @Ignore
   @ContinuousIntegrationTest(estimatedDuration = 30.0) // Tests the selection matrix of the pelvis 
   @Test(timeout = 160000)
   public void testSolvingForPelvisAngularControl() throws SimulationExceededMaximumTimeException, IOException
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      drcBehaviorTestHelper.updateRobotModel();

      WholeBodyInverseKinematicsBehavior ik = new WholeBodyInverseKinematicsBehavior(getRobotModel(), drcBehaviorTestHelper.getYoTime(),
                                                                                     drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
                                                                                     getRobotModel().createFullRobotModel());
      
      Quat4d offsetOrientationPelvis = new Quat4d();
      RotationTools.convertYawPitchRollToQuaternion(0.3, 0.0, 0.1, offsetOrientationPelvis);
      ReferenceFrame pelvisControlFrame = drcBehaviorTestHelper.getControllerFullRobotModel().getPelvis().getBodyFixedFrame();
      FrameOrientation desiredPelvisOrientation = new FrameOrientation(pelvisControlFrame);
      double initialPelvisPitch = desiredPelvisOrientation.getPitch();
      double initialPelvisYaw = desiredPelvisOrientation.getYaw();
      desiredPelvisOrientation.set(offsetOrientationPelvis);
      desiredPelvisOrientation.changeFrame(ReferenceFrame.getWorldFrame());
      ik.setTrajectoryTime(0.5);
      ik.setPelvisAngularControl(true, false, false);
      ik.setDesiredPelvisOrientation(desiredPelvisOrientation);   
      drcBehaviorTestHelper.dispatchBehavior(ik);

      while (!ik.isDone())
      {
         ThreadTools.sleep(100);
      }

      assertFalse("Bad solution: " + ik.getSolutionQuality(), ik.hasSolverFailed());

      success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      FrameOrientation currentPelvisOrientation = new FrameOrientation(pelvisControlFrame);
      currentPelvisOrientation.changeFrame(ReferenceFrame.getWorldFrame());
      double currentPelvisRoll = currentPelvisOrientation.getRoll();
      double currentPelvisYaw = currentPelvisOrientation.getYaw();
      double currentPelvisPitch = currentPelvisOrientation.getPitch();
      
      double angleEpsilon = Math.toRadians(1);

      assertEquals("Expected: " + desiredPelvisOrientation.getRoll() + " Received: " + currentPelvisRoll, desiredPelvisOrientation.getRoll(), currentPelvisRoll, angleEpsilon); 
      assertEquals("Expected: " + initialPelvisYaw + " Received: " + currentPelvisYaw, initialPelvisYaw, currentPelvisYaw, angleEpsilon); 
      assertEquals("Expected: " + initialPelvisPitch + " Received: " + currentPelvisPitch, initialPelvisPitch, currentPelvisPitch, angleEpsilon); 
      
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }
   
   private boolean isOrientationEqual(Quat4d initialQuat, Quat4d finalQuat, double angleEpsilon)
   {
      Quat4d quatDifference = new Quat4d(initialQuat);
      quatDifference.mulInverse(finalQuat);

      AxisAngle4d angleDifference = new AxisAngle4d();
      angleDifference.set(quatDifference);
      AngleTools.trimAngleMinusPiToPi(angleDifference.getAngle());

      System.out.println(angleDifference.getAngle());
      return Math.abs(angleDifference.getAngle()) < angleEpsilon;
   }

   private void setupKinematicsToolboxModule() throws IOException
   {
      DRCRobotModel robotModel = getRobotModel();
      kinematicsToolboxModule = new KinematicsToolboxModule(robotModel.createFullRobotModel(), robotModel.getLogModelProvider(), isKinematicsToolboxVisualizerEnabled);
      toolboxCommunicator = drcBehaviorTestHelper.createAndStartPacketCommunicator(NetworkPorts.KINEMATICS_TOOLBOX_MODULE_PORT, PacketDestination.KINEMATICS_TOOLBOX_MODULE);
   }
}
