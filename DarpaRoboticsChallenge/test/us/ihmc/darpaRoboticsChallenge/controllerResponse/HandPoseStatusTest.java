package us.ihmc.darpaRoboticsChallenge.controllerResponse;

import static org.junit.Assert.assertTrue;

import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.manipulation.HandPosePacket.Frame;
import us.ihmc.communication.packets.manipulation.HandPoseStatus;
import us.ihmc.communication.packets.manipulation.StopArmMotionPacket;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.drcRobot.FlatGroundEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.robotSide.RobotSide;

public abstract class HandPoseStatusTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();
   
   private DRCSimulationTestHelper testHelper;

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
      if (testHelper != null)
      {
         testHelper.destroySimulation();
         testHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   
   private boolean hasSimulationBeenInitialized;

   private int statusStartedCounter = 0;
   private int statusCompletedCounter = 0;

   private int leftStatusStartedCounter = 0;
   private int leftStatusCompletedCounter = 0;
   private int rightStatusStartedCounter = 0;
   private int rightStatusCompletedCounter = 0;


   @Before
   public void setUp()
   {
      showMemoryUsageBeforeTest();
   }


   private HandPosePacket createRandomHandPosePacket()
   {

      RobotSide robotSide = RandomTools.generateRandomRobotSide(new Random());
      Point3d position = RandomTools.generateRandomPoint(new Random(), 0.1, -0.2, -0.3, 0.5, 0.2, 0.3);
      Quat4d orientation = RandomTools.generateRandomQuaternion(new Random(), Math.PI / 4);

      HandPosePacket handPosePacket = new HandPosePacket(robotSide, Frame.CHEST, position, orientation, 0.4);
      handPosePacket.setDestination(PacketDestination.CONTROLLER);
      return handPosePacket;
   }

   private HandPosePacket createRandomHandPosePacketWithRobotSide(RobotSide robotSide)
   {

      Point3d position = RandomTools.generateRandomPoint(new Random(), 0.1, -0.2, -0.3, 0.5, 0.2, 0.3);
      Quat4d orientation = RandomTools.generateRandomQuaternion(new Random(), Math.PI / 4);

      HandPosePacket handPosePacket = new HandPosePacket(robotSide, Frame.CHEST, position, orientation, 0.4);
      handPosePacket.setDestination(PacketDestination.CONTROLLER);
      return handPosePacket;
   }

	@AverageDuration(duration = 11.8)
	@Test(timeout = 35449)
   public void testStartedAndCompletedStatusAreSentAndReceivedForOneHandPose() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      KryoLocalPacketCommunicator mockUiCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      KryoLocalPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      mockUiCommunicator.attacthGlobalSendListener(controllerCommunicator);
      controllerCommunicator.attacthGlobalSendListener(mockUiCommunicator);
      
      DRCObstacleCourseStartingLocation startingLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      testHelper = new DRCSimulationTestHelper(new FlatGroundEnvironment(), controllerCommunicator, this.getClass().getSimpleName(), null, startingLocation , simulationTestingParameters, false, getRobotModel());
      

      statusStartedCounter = 0;
      statusCompletedCounter = 0;

      hasSimulationBeenInitialized = false;

      mockUiCommunicator.attachListener(HandPoseStatus.class, new PacketConsumer<HandPoseStatus>()
      {
         @Override
         public void receivedPacket(HandPoseStatus object)
         {
            if (object.getStatus() == HandPoseStatus.Status.STARTED && hasSimulationBeenInitialized)
               statusStartedCounter++;

            if (object.getStatus() == HandPoseStatus.Status.COMPLETED && hasSimulationBeenInitialized)
               statusCompletedCounter++;
         }
      });

      HandPosePacket outgoingHandPosePacket = createRandomHandPosePacket();

      testHelper.simulateAndBlockAndCatchExceptions(1.1);
      hasSimulationBeenInitialized = true;

      mockUiCommunicator.send(outgoingHandPosePacket);
      testHelper.simulateAndBlockAndCatchExceptions(1.0);

      assertTrue((statusStartedCounter == 1) && (statusCompletedCounter == 1));
      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 21.5)
	@Test(timeout = 64372)
   public void testPauseDuringSingleSendAndReceivedForOneHandPose() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      KryoLocalPacketCommunicator mockUiCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      KryoLocalPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      mockUiCommunicator.attacthGlobalSendListener(controllerCommunicator);
      controllerCommunicator.attacthGlobalSendListener(mockUiCommunicator);
      
      DRCObstacleCourseStartingLocation startingLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      testHelper = new DRCSimulationTestHelper(new FlatGroundEnvironment(), controllerCommunicator, this.getClass().getSimpleName(), null, startingLocation , simulationTestingParameters, false, getRobotModel());

      statusStartedCounter = 0;
      statusCompletedCounter = 0;

      hasSimulationBeenInitialized = false;

      mockUiCommunicator.attachListener(HandPoseStatus.class, new PacketConsumer<HandPoseStatus>()
      {
         @Override
         public void receivedPacket(HandPoseStatus status)
         {
            if (status.getStatus() == HandPoseStatus.Status.STARTED && hasSimulationBeenInitialized)
               statusStartedCounter++;

            if (status.getStatus() == HandPoseStatus.Status.COMPLETED && hasSimulationBeenInitialized)
               statusCompletedCounter++;
         }
      });

      testHelper.simulateAndBlockAndCatchExceptions(1.1);
      hasSimulationBeenInitialized = true;
      
      Vector3d startTranslation = new Vector3d();
      testHelper.getRobot().getJoint("l_arm_wrx").getTranslationToWorld(startTranslation);

      Vector3d desiredTranslation = new Vector3d(startTranslation);
      desiredTranslation.add(new Vector3d(0.3,0.3,0.8));
      
      HandPosePacket outgoingHandPosePacket = createRandomHandPosePacket();
      outgoingHandPosePacket.position.x = desiredTranslation.getX();
      outgoingHandPosePacket.position.y = desiredTranslation.getY();
      outgoingHandPosePacket.position.z = desiredTranslation.getZ();
      int trajectoryTime = 3;
      outgoingHandPosePacket.trajectoryTime = trajectoryTime;
      outgoingHandPosePacket.robotSide = RobotSide.LEFT;
      mockUiCommunicator.send(outgoingHandPosePacket);
      double timeToSimulateHandMotion = 1.0;
      testHelper.simulateAndBlockAndCatchExceptions(timeToSimulateHandMotion);
      mockUiCommunicator.send(new StopArmMotionPacket(RobotSide.LEFT));
      testHelper.simulateAndBlockAndCatchExceptions(3.0);

      Vector3d endTranslation = new Vector3d();
      testHelper.getRobot().getJoint("l_arm_wrx").getTranslationToWorld(endTranslation);
      
      Vector3d expectedTranslation = new Vector3d();
      expectedTranslation.interpolate(startTranslation, desiredTranslation, timeToSimulateHandMotion / trajectoryTime);
      
      PrintTools.debug(this, "statusStartedCounter: " + statusStartedCounter + " statusCompletedCounter: " + statusCompletedCounter);
      
      assertTrue((statusStartedCounter == 2) && (statusCompletedCounter == 1));
      assertTrue(Math.abs(endTranslation.getX() - expectedTranslation.getX()) < 0.1);
      assertTrue(Math.abs(endTranslation.getY() - expectedTranslation.getY()) < 0.1);
      assertTrue(Math.abs(endTranslation.getZ() - expectedTranslation.getZ()) < 0.1);

      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 11.4)
	@Test(timeout = 34250)
   public void testWhenTwoHandPosesAreSentInARow() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      final KryoLocalPacketCommunicator mockUiCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      KryoLocalPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      mockUiCommunicator.attacthGlobalSendListener(controllerCommunicator);
      controllerCommunicator.attacthGlobalSendListener(mockUiCommunicator);
      
      DRCObstacleCourseStartingLocation startingLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      testHelper = new DRCSimulationTestHelper(new FlatGroundEnvironment(), controllerCommunicator, this.getClass().getSimpleName(), null, startingLocation , simulationTestingParameters, false, getRobotModel());

      statusStartedCounter = 0;
      statusCompletedCounter = 0;

      hasSimulationBeenInitialized = false;

      mockUiCommunicator.attachListener(HandPoseStatus.class, new PacketConsumer<HandPoseStatus>()
      {
         @Override
         public void receivedPacket(HandPoseStatus object)
         {
            if (object.getStatus() == HandPoseStatus.Status.STARTED && hasSimulationBeenInitialized)
               statusStartedCounter++;

            if (object.getStatus() == HandPoseStatus.Status.COMPLETED && hasSimulationBeenInitialized)
               statusCompletedCounter++;
         }
      });

      RobotSide robotSide = RandomTools.generateRandomRobotSide(new Random());

      final HandPosePacket outgoingHandPosePacket_1 = createRandomHandPosePacketWithRobotSide(robotSide);
      final HandPosePacket outgoingHandPosePacket_2 = createRandomHandPosePacketWithRobotSide(robotSide);

      testHelper.simulateAndBlockAndCatchExceptions(1.1);
      hasSimulationBeenInitialized = true;

      Runnable myRunnable = new Runnable()
      {
         @Override
         public void run()
         {
            mockUiCommunicator.send(outgoingHandPosePacket_1);
            ThreadTools.sleep(200);
            mockUiCommunicator.send(outgoingHandPosePacket_2);
         }
      };

      Thread handposeThread = new Thread(myRunnable);
      handposeThread.start();
      testHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue((statusStartedCounter == 2) && (statusCompletedCounter == 1));

      BambooTools.reportTestFinishedMessage();
   }

	@AverageDuration(duration = 11.7)
	@Test(timeout = 35190)
   public void testEachArmReceiveOneHandPoseAtTheSameTime() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      final KryoLocalPacketCommunicator mockUiCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      KryoLocalPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),PacketDestination.NETWORK_PROCESSOR.ordinal(), "HandPoseStatusTest"); 
      mockUiCommunicator.attacthGlobalSendListener(controllerCommunicator);
      controllerCommunicator.attacthGlobalSendListener(mockUiCommunicator);
      
      DRCObstacleCourseStartingLocation startingLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      testHelper = new DRCSimulationTestHelper(new FlatGroundEnvironment(), controllerCommunicator, this.getClass().getSimpleName(), null, startingLocation , simulationTestingParameters, false, getRobotModel());

      leftStatusStartedCounter = 0;
      leftStatusCompletedCounter = 0;
      rightStatusStartedCounter = 0;
      rightStatusCompletedCounter = 0;

      hasSimulationBeenInitialized = false;

      mockUiCommunicator.attachListener(HandPoseStatus.class, new PacketConsumer<HandPoseStatus>()
      {
         @Override
         public void receivedPacket(HandPoseStatus object)
         {
            if (object.getStatus() == HandPoseStatus.Status.STARTED && hasSimulationBeenInitialized)
            {
               if (object.getRobotSide() == RobotSide.LEFT)
                  leftStatusStartedCounter++;
               else
                  rightStatusStartedCounter++;
            }
            if (object.getStatus() == HandPoseStatus.Status.COMPLETED && hasSimulationBeenInitialized)
            {
               if (object.getRobotSide() == RobotSide.LEFT)
                  leftStatusCompletedCounter++;
               else
                  rightStatusCompletedCounter++;
            }
         }
      });

      final HandPosePacket outgoingHandPosePacket_1 = createRandomHandPosePacketWithRobotSide(RobotSide.LEFT);
      final HandPosePacket outgoingHandPosePacket_2 = createRandomHandPosePacketWithRobotSide(RobotSide.RIGHT);

      testHelper.simulateAndBlockAndCatchExceptions(1.1);
      hasSimulationBeenInitialized = true;

      Runnable myRunnable = new Runnable()
      {
         @Override
         public void run()
         {
            mockUiCommunicator.send(outgoingHandPosePacket_1);
            ThreadTools.sleep(200);
            mockUiCommunicator.send(outgoingHandPosePacket_2);
         }
      };

      Thread handposeThread = new Thread(myRunnable);
      handposeThread.start();
      testHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue((leftStatusStartedCounter == 1) && (rightStatusStartedCounter == 1) && (leftStatusCompletedCounter == 1) && (rightStatusCompletedCounter == 1));
      BambooTools.reportTestFinishedMessage();
   }
}
