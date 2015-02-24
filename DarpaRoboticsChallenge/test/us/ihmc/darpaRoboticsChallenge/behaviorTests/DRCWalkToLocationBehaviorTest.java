package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Vector2d;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packetCommunicator.KryoPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.behaviors.HumanoidBehaviorControlModePacket.HumanoidBehaviorControlModeEnum;
import us.ihmc.communication.util.NetworkConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.WalkToLocationBehavior;
import us.ihmc.humanoidBehaviors.communication.BehaviorCommunicationBridge;
import us.ihmc.humanoidBehaviors.utilities.TrajectoryBasedStopThreadUpdatable;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;

import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.io.printing.SysoutTool;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FramePose2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.time.GlobalTimer;

public abstract class DRCWalkToLocationBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

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

      GlobalTimer.clearTimers();
      
      

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCWalkToLocationBehaviorTest.class + " after class.");
   }

   private static final boolean DEBUG = false;

   private final double POSITION_THRESHOLD = 0.06;   // Atlas typically achieves between 0.02-0.03 position threshold
   private final double ORIENTATION_THRESHOLD = 0.2;  // Atlas typically achieves between .005-0.1 orientation threshold (more accurate when turning in place at final target)

   private DRCBehaviorTestHelper drcBehaviorTestHelper;

   @Before
   public void setUp()
   {
      if (NetworkConfigParameters.USE_BEHAVIORS_MODULE)
      {
         throw new RuntimeException("Must set NetworkConfigParameters.USE_BEHAVIORS_MODULE = false in order to perform this test!");
      }

      DRCDemo01NavigationEnvironment testEnvironment = new DRCDemo01NavigationEnvironment();

      KryoPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(), PacketDestination.CONTROLLER.ordinal(), "DRCControllerCommunicator");
      KryoPacketCommunicator networkObjectCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(), PacketDestination.NETWORK_PROCESSOR.ordinal(), "MockNetworkProcessorCommunicator");

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(testEnvironment, networkObjectCommunicator, getSimpleRobotName(), null,
            DRCObstacleCourseStartingLocation.DEFAULT, simulationTestingParameters, getRobotModel(), controllerCommunicator);
   }

   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testTurn361DegreesInPlace() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      FramePose2d desiredMidFeetPose2d = getCurrentMidFeetPose2dCopy();
      double currentYaw = desiredMidFeetPose2d.getYaw();
      desiredMidFeetPose2d.setYaw(currentYaw + Math.toRadians(361.0));
      
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(walkToLocationBehavior);
      assertTrue(success);
      SysoutTool.println("Behavior Should be done", DEBUG);

      assertCurrentMidFeetPoseIsWithinThreshold(desiredMidFeetPose2d);
      assertTrue(walkToLocationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }
   
   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkForwardsX() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = RandomTools.generateRandomDouble(new Random(), 1.0, 2.0);
      Vector2d walkDirection = new Vector2d(1, 0);
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(walkToLocationBehavior);
      assertTrue(success);
      SysoutTool.println("Behavior Should be done", DEBUG);

      assertCurrentMidFeetPoseIsWithinThreshold(desiredMidFeetPose2d);
      assertTrue(walkToLocationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }
   
   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkBackwardsASmallAmountWithoutTurningInPlace() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = 2.0 * getRobotModel().getWalkingControllerParameters().getMinStepLengthForToeOff();
      Vector2d walkDirection = new Vector2d(-1, 0);
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);

      int randomZeroOrOne = RandomTools.generateRandomInt(new Random(), 0, 1);
      double walkingOrientationRelativeToPathDirection;
      if (randomZeroOrOne == 0)
      {
         walkingOrientationRelativeToPathDirection = Math.PI;
      }
      else
      {
         walkingOrientationRelativeToPathDirection = -Math.PI;
      }
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d, walkingOrientationRelativeToPathDirection);
      int numberOfFootsteps = walkToLocationBehavior.getNumberOfFootSteps();
      if (DEBUG)
         SysoutTool.println("Number of Footsteps: " + numberOfFootsteps);
      assertTrue(numberOfFootsteps <= 4.0);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(walkToLocationBehavior);
      assertTrue(success);
      SysoutTool.println("Behavior Should be done", DEBUG);

      assertPosesAreWithinThresholds(desiredMidFeetPose2d, getCurrentMidFeetPose2dCopy(), 10.0 * POSITION_THRESHOLD);  //TODO: Determine why position error is so large when walking backwards
      assertTrue(walkToLocationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }
   
   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkAtAngleAndFinishAlignedWithWalkingPath() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = RandomTools.generateRandomDouble(new Random(), 1.0, 2.0);  
      double walkAngleDegrees = RandomTools.generateRandomDouble(new Random(), 45.0);
      
      Vector2d walkDirection = new Vector2d(Math.cos(Math.toRadians(walkAngleDegrees)), Math.sin(Math.toRadians(walkAngleDegrees)));
      FramePose2d desiredMidFeetPose2d = copyOffsetAndYawCurrentMidfeetPose2d(walkDistance, walkDirection, Math.toRadians(walkAngleDegrees));
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(walkToLocationBehavior);
      assertTrue(success);
      SysoutTool.println("Behavior Should be done", DEBUG);

      assertCurrentMidFeetPoseIsWithinThreshold(desiredMidFeetPose2d);
      assertTrue(walkToLocationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }
   
   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkAtAngleAndFinishAlignedWithInitialOrientation() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = RandomTools.generateRandomDouble(new Random(), 1.0, 2.0);  
      double walkAngleDegrees = RandomTools.generateRandomDouble(new Random(), 45.0);
      
      Vector2d walkDirection = new Vector2d(Math.cos(Math.toRadians(walkAngleDegrees)), Math.sin(Math.toRadians(walkAngleDegrees)));
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(walkToLocationBehavior);
      assertTrue(success);
      SysoutTool.println("Behavior Should be done", DEBUG);

      assertCurrentMidFeetPoseIsWithinThreshold(desiredMidFeetPose2d);
      assertTrue(walkToLocationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkAndStopBehavior() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = 4.0;
      Vector2d walkDirection = new Vector2d(1, 0);
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      double pausePercent = Double.POSITIVE_INFINITY;
      double pauseDuration = Double.POSITIVE_INFINITY;
      double stopPercent = 20.0;

      ReferenceFrame frameToKeepTrackOf = drcBehaviorTestHelper.getReferenceFrames().getMidFeetZUpFrame();

      TrajectoryBasedStopThreadUpdatable stopThreadUpdatable = new TrajectoryBasedStopThreadUpdatable(drcBehaviorTestHelper.getRobotDataReceiver(),
            walkToLocationBehavior, pausePercent, pauseDuration, stopPercent, desiredMidFeetPose2d, frameToKeepTrackOf);

      success = drcBehaviorTestHelper.executeBehaviorPauseAndResumeOrStop(walkToLocationBehavior, stopThreadUpdatable);
      assertTrue(success);
      SysoutTool.println("Stop Simulating Behavior", DEBUG);

      FramePose2d midFeetPose2dAtStop = stopThreadUpdatable.getTestFramePose2dAtTransition(HumanoidBehaviorControlModeEnum.STOP);
      FramePose2d midFeetPose2dFinal = stopThreadUpdatable.getCurrentTestFramePose2dCopy();

      // Position and orientation may change after stop command if the robot is currently in single support, 
      // since the robot will complete the current step (to get back into double support) before actually stopping
      double positionThreshold = getRobotModel().getWalkingControllerParameters().getMaxStepLength();
      double orientationThreshold = Math.PI;
      assertPosesAreWithinThresholds(midFeetPose2dAtStop, midFeetPose2dFinal, positionThreshold, orientationThreshold);
      assertTrue(!walkToLocationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkPauseAndResumeBehavior() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = 3.0;
      Vector2d walkDirection = new Vector2d(1, 0);
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      double pausePercent = 20.0;
      double pauseDuration = 2.0;
      double stopPercent = Double.POSITIVE_INFINITY;

      ReferenceFrame frameToKeepTrackOf = drcBehaviorTestHelper.getReferenceFrames().getMidFeetZUpFrame();

      TrajectoryBasedStopThreadUpdatable stopThreadUpdatable = new TrajectoryBasedStopThreadUpdatable(drcBehaviorTestHelper.getRobotDataReceiver(),
            walkToLocationBehavior, pausePercent, pauseDuration, stopPercent, desiredMidFeetPose2d, frameToKeepTrackOf);

      success = drcBehaviorTestHelper.executeBehaviorPauseAndResumeOrStop(walkToLocationBehavior, stopThreadUpdatable);
      assertTrue(success);
      SysoutTool.println("Stop Simulating Behavior", DEBUG);

      FramePose2d midFeetPoseAtPause = stopThreadUpdatable.getTestFramePose2dAtTransition(HumanoidBehaviorControlModeEnum.PAUSE);
      FramePose2d midFeetPoseAtResume = stopThreadUpdatable.getTestFramePose2dAtTransition(HumanoidBehaviorControlModeEnum.RESUME);
      FramePose2d midFeetPoseFinal = stopThreadUpdatable.getCurrentTestFramePose2dCopy();

      // Position and orientation may change after pause command if the robot is currently in single support, 
      // since the robot will complete the current step (to get back into double support) before actually pausing
      double positionThreshold = getRobotModel().getWalkingControllerParameters().getMaxStepLength();
      double orientationThreshold = Math.PI;
      assertPosesAreWithinThresholds(midFeetPoseAtPause, midFeetPoseAtResume, positionThreshold, orientationThreshold);
      assertTrue(walkToLocationBehavior.isDone());
      assertPosesAreWithinThresholds(desiredMidFeetPose2d, midFeetPoseFinal, POSITION_THRESHOLD, ORIENTATION_THRESHOLD);
      assertPosesAreWithinThresholds(desiredMidFeetPose2d, midFeetPoseFinal, 0.9*POSITION_THRESHOLD, ORIENTATION_THRESHOLD);
      assertPosesAreWithinThresholds(desiredMidFeetPose2d, midFeetPoseFinal, POSITION_THRESHOLD, 0.9*ORIENTATION_THRESHOLD);


      BambooTools.reportTestFinishedMessage();
   }

   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkPauseAndResumeOnLastStepBehavior() throws SimulationExceededMaximumTimeException
   {
      //This test makes sure that walking behavior doesn't declare isDone() when *starting/resuming* walking
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = 3.0;
      Vector2d walkDirection = new Vector2d(1, 0);
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      double pausePercent = 80.0;
      double pauseDuration = 2.0;
      double stopPercent = Double.POSITIVE_INFINITY;

      ReferenceFrame frameToKeepTrackOf = drcBehaviorTestHelper.getReferenceFrames().getMidFeetZUpFrame();

      TrajectoryBasedStopThreadUpdatable stopThreadUpdatable = new TrajectoryBasedStopThreadUpdatable(drcBehaviorTestHelper.getRobotDataReceiver(),
            walkToLocationBehavior, pausePercent, pauseDuration, stopPercent, desiredMidFeetPose2d, frameToKeepTrackOf);

      success = drcBehaviorTestHelper.executeBehaviorPauseAndResumeOrStop(walkToLocationBehavior, stopThreadUpdatable);
      assertTrue(success);
      SysoutTool.println("Stop Simulating Behavior", DEBUG);

      FramePose2d midFeetPoseAtPause = stopThreadUpdatable.getTestFramePose2dAtTransition(HumanoidBehaviorControlModeEnum.PAUSE);
      FramePose2d midFeetPoseAtResume = stopThreadUpdatable.getTestFramePose2dAtTransition(HumanoidBehaviorControlModeEnum.RESUME);
      FramePose2d midFeetPoseFinal = stopThreadUpdatable.getCurrentTestFramePose2dCopy();

      // Position and orientation may change after pause command if the robot is currently in single support, 
      // since the robot will complete the current step (to get back into double support) before actually pausing
      double positionThreshold = getRobotModel().getWalkingControllerParameters().getMaxStepLength();
      double orientationThreshold = Math.PI;
      assertPosesAreWithinThresholds(midFeetPoseAtPause, midFeetPoseAtResume, positionThreshold, orientationThreshold);
      assertTrue(walkToLocationBehavior.isDone());
      assertPosesAreWithinThresholds(desiredMidFeetPose2d, midFeetPoseFinal);

      BambooTools.reportTestFinishedMessage();
   }
   
   @AverageDuration(duration = 50.0)
   @Test(timeout = 300000)
   public void testWalkStopAndWalkToDifferentLocation() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      SysoutTool.println("Initializing Sim", DEBUG);
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      SysoutTool.println("Initializing Behavior", DEBUG);
      double walkDistance = 4.0;
      Vector2d walkDirection = new Vector2d(1, 0);
      FramePose2d desiredMidFeetPose2d = copyAndOffsetCurrentMidfeetPose2d(walkDistance, walkDirection);
      WalkToLocationBehavior walkToLocationBehavior = createAndSetupWalkToLocationBehavior(desiredMidFeetPose2d);

      double pausePercent = Double.POSITIVE_INFINITY;
      double pauseDuration = Double.POSITIVE_INFINITY;
      double stopPercent = 20.0;

      ReferenceFrame frameToKeepTrackOf = drcBehaviorTestHelper.getReferenceFrames().getMidFeetZUpFrame();

      TrajectoryBasedStopThreadUpdatable stopThreadUpdatable = new TrajectoryBasedStopThreadUpdatable(drcBehaviorTestHelper.getRobotDataReceiver(),
            walkToLocationBehavior, pausePercent, pauseDuration, stopPercent, desiredMidFeetPose2d, frameToKeepTrackOf);

      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorPauseAndResumeOrStop(walkToLocationBehavior, stopThreadUpdatable);
      assertTrue(success);
      SysoutTool.println("Stop Simulating Behavior", DEBUG);

      FramePose2d midFeetPose2dAtStop = stopThreadUpdatable.getTestFramePose2dAtTransition(HumanoidBehaviorControlModeEnum.STOP);
      FramePose2d midFeetPose2dFinal = stopThreadUpdatable.getCurrentTestFramePose2dCopy();

      // Position and orientation may change after stop command if the robot is currently in single support, 
      // since the robot will complete the current step (to get back into double support) before actually stopping
      double positionThreshold = getRobotModel().getWalkingControllerParameters().getMaxStepLength();
      double orientationThreshold = Math.PI;
      assertPosesAreWithinThresholds(midFeetPose2dAtStop, midFeetPose2dFinal, positionThreshold, orientationThreshold);
      assertTrue(!walkToLocationBehavior.isDone());
      
      SysoutTool.println("Setting New Behavior Inputs", DEBUG);
      walkDistance = 1.0;
      walkDirection.set(0, 1);
      double desiredYawAngle = Math.atan2(walkDirection.y, walkDirection.x);
      FramePose2d newDesiredMidFeetPose2d = copyOffsetAndYawCurrentMidfeetPose2d(walkDistance, walkDirection, desiredYawAngle);
      walkToLocationBehavior.setTarget(newDesiredMidFeetPose2d);
      walkToLocationBehavior.resume();
      assertTrue(walkToLocationBehavior.hasInputBeenSet());
      
      SysoutTool.println("Starting to Execute Behavior", DEBUG);
      success = drcBehaviorTestHelper.executeBehaviorUntilDone(walkToLocationBehavior);
      assertTrue(success);
      SysoutTool.println("Stop Simulating Behavior", DEBUG);

      assertCurrentMidFeetPoseIsWithinThreshold(newDesiredMidFeetPose2d);
      assertTrue(walkToLocationBehavior.isDone());
      
      BambooTools.reportTestFinishedMessage();
   }

   private FramePose2d copyAndOffsetCurrentMidfeetPose2d(double walkDistance, Vector2d walkDirection)
   {
      FramePose2d desiredMidFeetPose = getCurrentMidFeetPose2dCopy();

      walkDirection.normalize();

      desiredMidFeetPose.setX( desiredMidFeetPose.getX() + walkDistance * walkDirection.getX() );
      desiredMidFeetPose.setY( desiredMidFeetPose.getY() + walkDistance * walkDirection.getY() );
      
      return desiredMidFeetPose;
   }
   
   private FramePose2d copyOffsetAndYawCurrentMidfeetPose2d(double walkDistance, Vector2d walkDirection, double desiredYawAngle)
   {
      FramePose2d desiredMidFeetPose = getCurrentMidFeetPose2dCopy();

      walkDirection.normalize();

      double xDesired = desiredMidFeetPose.getX() + walkDistance * walkDirection.getX();
      double yDesired = desiredMidFeetPose.getY() + walkDistance * walkDirection.getY();
      
      desiredMidFeetPose.setPoseIncludingFrame(ReferenceFrame.getWorldFrame(), xDesired, yDesired, desiredYawAngle);
      return desiredMidFeetPose;
   }

   private WalkToLocationBehavior createAndSetupWalkToLocationBehavior(FramePose2d desiredMidFeetPose)
   {
      return createAndSetupWalkToLocationBehavior(desiredMidFeetPose, 0.0);
   }

   
   private WalkToLocationBehavior createAndSetupWalkToLocationBehavior(FramePose2d desiredMidFeetPose, double walkingOrientationRelativeToPathDirection)
   {
      BehaviorCommunicationBridge communicationBridge = drcBehaviorTestHelper.getBehaviorCommunicationBridge();
      FullRobotModel fullRobotModel = drcBehaviorTestHelper.getSDFFullRobotModel();
      ReferenceFrames referenceFrames = drcBehaviorTestHelper.getReferenceFrames();
      WalkingControllerParameters walkingControllerParams = getRobotModel().getWalkingControllerParameters();

      final WalkToLocationBehavior walkToLocationBehavior = new WalkToLocationBehavior(communicationBridge, fullRobotModel, referenceFrames,
            walkingControllerParams);

      walkToLocationBehavior.initialize();
      walkToLocationBehavior.setWalkingOrientationRelativeToPathDirection(walkingOrientationRelativeToPathDirection);
      walkToLocationBehavior.setTarget(desiredMidFeetPose);
      assertTrue(walkToLocationBehavior.hasInputBeenSet());

      return walkToLocationBehavior;
   }

   private FramePose2d getCurrentMidFeetPose2dCopy()
   {
      drcBehaviorTestHelper.updateRobotModel();
      ReferenceFrame midFeetFrame = drcBehaviorTestHelper.getReferenceFrames().getMidFeetZUpFrame();

      FramePose midFeetPose = new FramePose();
      midFeetPose.setToZero(midFeetFrame);
      midFeetPose.changeFrame(ReferenceFrame.getWorldFrame());

      FramePose2d ret = new FramePose2d();
      ret.setPoseIncludingFrame(midFeetPose.getReferenceFrame(), midFeetPose.getX(), midFeetPose.getY(), midFeetPose.getYaw());

      return ret;
   }

   private void assertCurrentMidFeetPoseIsWithinThreshold(FramePose2d desiredMidFeetPose)
   {
      FramePose2d currentMidFeetPose = getCurrentMidFeetPose2dCopy();
      assertPosesAreWithinThresholds(desiredMidFeetPose, currentMidFeetPose);
   }

   private void assertPosesAreWithinThresholds(FramePose2d desiredPose, FramePose2d actualPose)
   {
      assertPosesAreWithinThresholds(desiredPose, actualPose, POSITION_THRESHOLD);
   }

   private void assertPosesAreWithinThresholds(FramePose2d desiredPose, FramePose2d actualPose, double positionThreshold)
   {
      assertPosesAreWithinThresholds(desiredPose, actualPose, positionThreshold, ORIENTATION_THRESHOLD);
   }

   private void assertPosesAreWithinThresholds(FramePose2d desiredPose, FramePose2d actualPose, double positionThreshold, double orientationThreshold)
   {
      double positionDistance = desiredPose.getPositionDistance(actualPose);
      double orientationDistance = desiredPose.getOrientationDistance(actualPose);

      if (DEBUG)
      {
         SysoutTool.println(" desired Midfeet Pose :\n" + desiredPose + "\n");
         SysoutTool.println(" actual Midfeet Pose :\n" + actualPose + "\n");

         SysoutTool.println(" positionDistance = " + positionDistance);
         SysoutTool.println(" orientationDistance = " + orientationDistance);
      }
      
      assertEquals("Pose position error :" + positionDistance + " exceeds threshold: " + positionThreshold, 0.0, positionDistance, positionThreshold);
      assertEquals("Pose orientation error :" + orientationDistance + " exceeds threshold: " + orientationThreshold, 0.0, orientationDistance, orientationThreshold);
   }
}
