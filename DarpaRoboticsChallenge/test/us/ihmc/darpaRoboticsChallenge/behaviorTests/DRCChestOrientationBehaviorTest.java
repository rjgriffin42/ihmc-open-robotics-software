package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Quat4d;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packetCommunicator.KryoPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.walking.ChestOrientationPacket;
import us.ihmc.communication.util.NetworkConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.primitives.ChestOrientationBehavior;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;
import us.ihmc.utilities.code.unitTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.io.printing.SysoutTool;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.yoUtilities.time.GlobalTimer;

public abstract class DRCChestOrientationBehaviorTest implements MultiRobotTestInterface
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
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCChestOrientationBehaviorTest.class + " after class.");
   }

   private static final boolean DEBUG = false;

   private final double MAX_ANGLE_TO_TEST_RAD = 30.0 * Math.PI / 180.0;
   private final double POSITION_THRESHOLD = Double.NaN;
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

	@AverageDuration(duration = 16.4)
   @Test(timeout = 49112)
   public void testSingleRandomChestOrientationMove() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      Quat4d desiredChestQuat = new Quat4d(RandomTools.generateRandomQuaternion(new Random(), MAX_ANGLE_TO_TEST_RAD));
      ChestOrientationPacket chestOrientationPacket = new ChestOrientationPacket(desiredChestQuat, false, 1.0);

      ChestOrientationBehavior chestOrientationBehavior = testChestOrientationBehavior(chestOrientationPacket);

      assertTrue(chestOrientationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 16.1)
   @Test(timeout = 48244)
   public void testSingleChestPitchOrientationMove() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double rotationAngle = MAX_ANGLE_TO_TEST_RAD * RandomTools.generateRandomDouble(new Random(), 0.3, 1.0);
      AxisAngle4d desiredAxisAngle = new AxisAngle4d(0.0, 1.0, 0.0, rotationAngle);
      Quat4d desiredChestQuat = new Quat4d();
      desiredChestQuat.set(desiredAxisAngle);

      ChestOrientationPacket chestOrientationPacket = new ChestOrientationPacket(desiredChestQuat, false, 1.0);

      ChestOrientationBehavior chestOrientationBehavior = testChestOrientationBehavior(chestOrientationPacket);

      assertTrue(chestOrientationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 16.6)
   @Test(timeout = 49935)
   public void testSingleChestRollOrientationMove() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double rotationAngle = MAX_ANGLE_TO_TEST_RAD * RandomTools.generateRandomDouble(new Random(), 0.3, 1.0);
      AxisAngle4d desiredAxisAngle = new AxisAngle4d(1.0, 0.0, 0.0, rotationAngle);
      Quat4d desiredChestQuat = new Quat4d();
      desiredChestQuat.set(desiredAxisAngle);

      ChestOrientationPacket chestOrientationPacket = new ChestOrientationPacket(desiredChestQuat, false, 1.0);

      ChestOrientationBehavior chestOrientationBehavior = testChestOrientationBehavior(chestOrientationPacket);

      assertTrue(chestOrientationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 16.9)
   @Test(timeout = 50793)
   public void testSingleChestYawOrientationMove() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double rotationAngle = MAX_ANGLE_TO_TEST_RAD * RandomTools.generateRandomDouble(new Random(), 0.3, 1.0);
      AxisAngle4d desiredAxisAngle = new AxisAngle4d(0.0, 0.0, 1.0, rotationAngle);
      Quat4d desiredChestQuat = new Quat4d();
      desiredChestQuat.set(desiredAxisAngle);

      ChestOrientationPacket chestOrientationPacket = new ChestOrientationPacket(desiredChestQuat, false, 1.0);

      ChestOrientationBehavior chestOrientationBehavior = testChestOrientationBehavior(chestOrientationPacket);

      assertTrue(chestOrientationBehavior.isDone());

      BambooTools.reportTestFinishedMessage();
   }
	
	@AverageDuration(duration = 15)
	@Test(timeout = 50000)
	public void testTranformOrientation() throws SimulationExceededMaximumTimeException
	{
	   final double quatDelta = 1e-6;
	   
      BambooTools.reportTestStartedMessage();

      double rotationAngle = MAX_ANGLE_TO_TEST_RAD * RandomTools.generateRandomDouble(new Random(), 0.3, 1.0);
      AxisAngle4d desiredAxisAngle = new AxisAngle4d(0.0, 0.0, 1.0, rotationAngle);
      Quat4d desiredChestQuat = new Quat4d();
      desiredChestQuat.set(desiredAxisAngle);

      ChestOrientationPacket chestOrientationPacket = new ChestOrientationPacket(desiredChestQuat, false, 1.0);
      
      RigidBodyTransform transform = new RigidBodyTransform();
      ChestOrientationPacket transformPacket = chestOrientationPacket.transform(transform);
      Quat4d packetQuat = transformPacket.getOrientation();
      assertEquals(packetQuat.w, desiredChestQuat.w, quatDelta);
      assertEquals(packetQuat.x, desiredChestQuat.x, quatDelta);
      assertEquals(packetQuat.y, desiredChestQuat.y, quatDelta);
      assertEquals(packetQuat.z, desiredChestQuat.z, quatDelta);
      
      BambooTools.reportTestFinishedMessage();
	}

   private ChestOrientationBehavior testChestOrientationBehavior(ChestOrientationPacket chestOrientationPacket) throws SimulationExceededMaximumTimeException
   {
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);

      final ChestOrientationBehavior chestOrientBehavior = new ChestOrientationBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(),
            drcBehaviorTestHelper.getYoTime());

      chestOrientBehavior.initialize();
      chestOrientBehavior.setInput(chestOrientationPacket);
      assertTrue(chestOrientBehavior.hasInputBeenSet());

      double totalSimTime = chestOrientationPacket.getTrajectoryTime();
      totalSimTime += 1.0;

      FramePose initialChestPose = getCurrentChestPose();
      success = success && drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(chestOrientBehavior, totalSimTime);
      FramePose finalChestPose = getCurrentChestPose();

      if (DEBUG)
      {
         SysoutTool.println(" initial Chest Pose :\n" + initialChestPose);
         SysoutTool.println(" final Chest Pose :\n" + finalChestPose);
      }
      FramePose desiredChestPose = new FramePose();
      desiredChestPose.setPose(initialChestPose.getFramePointCopy().getPoint(), chestOrientationPacket.orientation);
      assertPosesAreWithinThresholds(desiredChestPose, finalChestPose);

      assertTrue(success);

      return chestOrientBehavior;
   }

   private FramePose getCurrentChestPose()
   {
      drcBehaviorTestHelper.updateRobotModel();

      FramePose ret = new FramePose();
      ret.setToZero(drcBehaviorTestHelper.getSDFFullRobotModel().getChest().getBodyFixedFrame());
      ret.changeFrame(ReferenceFrame.getWorldFrame());

      return ret;
   }

   private void assertPosesAreWithinThresholds(FramePose framePose1, FramePose framePose2)
   {
      double positionDistance = framePose1.getPositionDistance(framePose2);
      double orientationDistance = framePose1.getOrientationDistance(framePose2);

      if (DEBUG)
      {
         SysoutTool.println(" desired Chest Pose :\n" + framePose1);
         SysoutTool.println(" actual Chest Pose :\n" + framePose2);

         SysoutTool.println(" positionDistance = " + positionDistance);
         SysoutTool.println(" orientationDistance = " + orientationDistance);
      }

      if (!Double.isNaN(POSITION_THRESHOLD))
      {
         assertEquals(0.0, positionDistance, POSITION_THRESHOLD);
      }
      assertEquals(0.0, orientationDistance, ORIENTATION_THRESHOLD);
   }
}
