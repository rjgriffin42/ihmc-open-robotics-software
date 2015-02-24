package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packetCommunicator.KryoPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.behaviors.DrillTaskPacket;
import us.ihmc.communication.util.NetworkConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDrillEnvironment;
import us.ihmc.darpaRoboticsChallenge.initialSetup.OffsetAndYawRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.DrillTaskBehavior;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.util.environments.ContactableCylinderRobot;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.time.GlobalTimer;

import com.jme3.math.Vector3f;

/**
 * @author unknownid
 *
 */
public abstract class DRCDrillTaskBehaviorTest implements MultiRobotTestInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   static
   {
      simulationTestingParameters.setKeepSCSUp(false);
   }

   // Testing parameters:
   private static final Vector3d initialRobotPosition = new Vector3d(0.0, 0.0, 0.0);
   private static final double initialRobotYaw = 0.0;
   private static final RobotSide grabSide = RobotSide.LEFT;

   private DRCBehaviorTestHelper drcBehaviorTestHelper;
   private ContactableCylinderRobot drillRobot;
   private DrillTaskBehavior drillTaskBehavior;

   @Before
   public void setUp()
   {
      if (NetworkConfigParameters.USE_BEHAVIORS_MODULE)
      {
         throw new RuntimeException("Must set NetworkConfigParameters.USE_BEHAVIORS_MODULE = false in order to perform this test!");
      }

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
         drcBehaviorTestHelper.destroySimulation();
         drcBehaviorTestHelper = null;
      }

      GlobalTimer.clearTimers();
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   /**
    * @throws SimulationExceededMaximumTimeException
    */
   @Test(timeout = 120000)
   public void testDrillPickUp() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      // set up the test
      DRCDrillEnvironment testEnvironment = new DRCDrillEnvironment();
      drillRobot = testEnvironment.getDrillRobot();

      KryoPacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),
                                                         PacketDestination.CONTROLLER.ordinal(), "DRCControllerCommunicator");
      KryoPacketCommunicator networkObjectCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(),
                                                            PacketDestination.NETWORK_PROCESSOR.ordinal(), "MockNetworkProcessorCommunicator");

      DRCStartingLocation startingLocation = new DRCStartingLocation()
      {
         @Override
         public OffsetAndYawRobotInitialSetup getStartingLocationOffset()
         {
            OffsetAndYawRobotInitialSetup offsetAndYawRobotInitialSetup = new OffsetAndYawRobotInitialSetup(initialRobotPosition, initialRobotYaw);

            return offsetAndYawRobotInitialSetup;
         }
      };

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(testEnvironment, networkObjectCommunicator, getSimpleRobotName(), null, startingLocation,
              simulationTestingParameters, getRobotModel(), controllerCommunicator);
      drillTaskBehavior = new DrillTaskBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge(), drcBehaviorTestHelper.getYoTime(),
              drcBehaviorTestHelper.getSDFFullRobotModel(), drcBehaviorTestHelper.getReferenceFrames(), getRobotModel());
      drillTaskBehavior.setGrabSide(grabSide);

      // add behavior trigger and run simulation
      drcBehaviorTestHelper.getDRCSimulationFactory().getRobot().setController(new DrillTaskUser());
      drcBehaviorTestHelper.executeBehaviorUntilDoneUsingBehaviorDispatcher(drillTaskBehavior);

      // check if drill is in hand:
      Vector3f wristHandOffset = getRobotModel().getJmeTransformWristToHand(grabSide).getTranslation();
      ReferenceFrame finalHandFrame = drcBehaviorTestHelper.getSDFFullRobotModel().getHandControlFrame(grabSide);
      FramePoint finalHandPosition = new FramePoint(finalHandFrame, wristHandOffset.x, wristHandOffset.y, wristHandOffset.z);
      finalHandPosition.changeFrame(worldFrame);
      assertTrue(drillRobot.isPointOnOrInside(finalHandPosition.getPoint()));

      BambooTools.reportTestFinishedMessage();
   }

   private class DrillTaskUser implements RobotController
   {
      private final double startBehaviorTime = 1.0;

      private final YoVariableRegistry registry = new YoVariableRegistry("DrillBehaviorTest");
      private final BooleanYoVariable sendDrillPacket = new BooleanYoVariable("sendDrillPacket", registry);
      private final BooleanYoVariable packetHasBeenSent = new BooleanYoVariable("drillPacketHasBeenSent", registry);

      @Override
      public void initialize()
      {
         sendDrillPacket.set(false);
         packetHasBeenSent.set(false);
      }

      @Override
      public YoVariableRegistry getYoVariableRegistry()
      {
         return registry;
      }

      @Override
      public String getName()
      {
         return registry.getName();
      }

      @Override
      public String getDescription()
      {
         return getName();
      }

      @Override
      public void doControl()
      {
         if (!packetHasBeenSent.getBooleanValue() && (drcBehaviorTestHelper.getYoTime().getDoubleValue() > startBehaviorTime))
         {
            sendDrillPacket.set(true);
         }

         if (sendDrillPacket.getBooleanValue())
         {
            RigidBodyTransform drillTransform = new RigidBodyTransform();
            drillRobot.getBodyTransformToWorld(drillTransform);

            DrillTaskPacket drillTaskPacket = new DrillTaskPacket(drillTransform);
            drcBehaviorTestHelper.sendPacketAsIfItWasFromUI(drillTaskPacket);

            packetHasBeenSent.set(true);
            sendDrillPacket.set(false);
         }
      }
   }
}
