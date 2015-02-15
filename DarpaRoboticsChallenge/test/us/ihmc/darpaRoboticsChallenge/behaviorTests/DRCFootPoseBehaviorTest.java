package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packetCommunicator.KryoPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.walking.FootPosePacket;
import us.ihmc.communication.util.NetworkConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.behaviors.primitives.FootPoseBehavior;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.time.GlobalTimer;

public abstract class DRCFootPoseBehaviorTest implements MultiRobotTestInterface
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
      TimerTaskScheduler.cancelAndReset();
      AsyncContinuousExecutor.cancelAndReset();

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCFootPoseBehaviorTest.class + " after class.");
   }

   private static final boolean DEBUG = false;

   private final double POSITION_THRESHOLD = 0.1;
   private final double ORIENTATION_THRESHOLD = 0.007;

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

	@AverageDuration(duration = 29.1)
   @Test(timeout = 87234)
   public void testSimpleFootPoseBehavior() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      double trajectoryTime = 3.0;
      RobotSide robotSide = RobotSide.LEFT;
      double deltaZ = 0.2;

      final FootPoseBehavior footPoseBehavior = new FootPoseBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(), drcBehaviorTestHelper.getYoTime(),
            drcBehaviorTestHelper.getCapturePointUpdatable().getYoDoubleSupport());

      FramePose desiredFootPose = getCurrentFootPose(robotSide);
      desiredFootPose.setZ(desiredFootPose.getZ() + deltaZ);

      FootPosePacket desiredFootPosePacket = createFootPosePacket(robotSide, desiredFootPose, trajectoryTime);
      footPoseBehavior.initialize();
      footPoseBehavior.setInput(desiredFootPosePacket);
      assertTrue(footPoseBehavior.hasInputBeenSet());

      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(footPoseBehavior, trajectoryTime + 1.0);
      assertTrue(success);

      FramePose finalFootPose = getCurrentFootPose(robotSide);
      assertTrue(footPoseBehavior.isDone());
      assertPosesAreWithinThresholds(desiredFootPose, finalFootPose);

      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 21.5)
   @Test(timeout = 64598)
   public void testSimulataneousLeftAndRightFootPoses() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      double trajectoryTime = 3.0;
      double deltaZ = 0.2;

      SideDependentList<FramePose> initialFootPoses = new SideDependentList<FramePose>();
      SideDependentList<BehaviorInterface> footPoseBehaviors = new SideDependentList<BehaviorInterface>();
      LinkedHashMap<BehaviorInterface, FramePose> desiredFootPoses = new LinkedHashMap<BehaviorInterface, FramePose>();

      RobotSide lastRobotSideSentToController = null;

      for (RobotSide robotSide : RobotSide.values)
      {
         final FootPoseBehavior footPoseBehavior = new FootPoseBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
               drcBehaviorTestHelper.getYoTime(), drcBehaviorTestHelper.getCapturePointUpdatable().getYoDoubleSupport());

         FramePose initialFootPose = getCurrentFootPose(robotSide);
         initialFootPoses.put(robotSide, initialFootPose);

         FramePose desiredFootPose = new FramePose(initialFootPose);
         desiredFootPose.setZ(desiredFootPose.getZ() + deltaZ);

         FootPosePacket desiredFootPosePacket = createFootPosePacket(robotSide, desiredFootPose, trajectoryTime);
         footPoseBehavior.initialize();
         footPoseBehavior.setInput(desiredFootPosePacket);
         assertTrue(footPoseBehavior.hasInputBeenSet());

         footPoseBehaviors.put(robotSide, footPoseBehavior);
         desiredFootPoses.put(footPoseBehavior, desiredFootPose);

         lastRobotSideSentToController = robotSide;
      }

      success = drcBehaviorTestHelper.executeBehaviorsSimulateAndBlockAndCatchExceptions(footPoseBehaviors, trajectoryTime + 1.0);
      assertTrue(success);

      assertPosesAreWithinThresholds(getCurrentFootPose(lastRobotSideSentToController),
            desiredFootPoses.get(footPoseBehaviors.get(lastRobotSideSentToController)));
      assertFootPoseDidNotChange(lastRobotSideSentToController.getOppositeSide(), initialFootPoses);

      for (RobotSide robotSide : RobotSide.values)
      {
         assertTrue(footPoseBehaviors.get(robotSide).isDone());
      }

      BambooTools.reportTestFinishedMessage();
   }

   private void assertFootPoseDidNotChange(RobotSide robotSide, SideDependentList<FramePose> initialFootPoses)
   {
      FramePose finalFootPose = getCurrentFootPose(robotSide);

      FramePose initialFootPose = initialFootPoses.get(robotSide);

      assertPosesAreWithinThresholds(finalFootPose, initialFootPose);
   }

   private FootPosePacket createFootPosePacket(RobotSide robotSide, FramePose desiredFootPose, double trajectoryTime)
   {
      Point3d desiredFootPosition = new Point3d();
      Quat4d desiredFootOrientation = new Quat4d();

      desiredFootPose.getPosition(desiredFootPosition);
      desiredFootPose.getOrientation(desiredFootOrientation);

      FootPosePacket ret = new FootPosePacket(robotSide, desiredFootPosition, desiredFootOrientation, trajectoryTime);

      return ret;
   }

   private FramePose getCurrentFootPose(RobotSide robotSide)
   {
      drcBehaviorTestHelper.updateRobotModel();
      ReferenceFrame footFrame = drcBehaviorTestHelper.getSDFFullRobotModel().getFoot(robotSide).getBodyFixedFrame();
      FramePose footPose = new FramePose();
      footPose.setToZero(footFrame);
      footPose.changeFrame(ReferenceFrame.getWorldFrame());
      return footPose;
   }

   private void assertPosesAreWithinThresholds(FramePose framePose1, FramePose framePose2)
   {
      double positionDistance = framePose1.getPositionDistance(framePose2);
      double orientationDistance = framePose1.getOrientationDistance(framePose2);

      if (DEBUG)
      {
         System.out.println("testSimpleHandPoseMove: positionDistance=" + positionDistance);
         System.out.println("testSimpleHandPoseMove: orientationDistance=" + orientationDistance);
      }

      assertEquals(0.0, positionDistance, POSITION_THRESHOLD);
      assertEquals(0.0, orientationDistance, ORIENTATION_THRESHOLD);
   }
}
