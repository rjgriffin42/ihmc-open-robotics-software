package us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.manipulation.HandPosePacket.Frame;
import us.ihmc.communication.packets.manipulation.HandPoseStatus;
import us.ihmc.communication.packets.sensing.DepthDataStateCommand;
import us.ihmc.communication.packets.sensing.DepthDataStateCommand.LidarState;
import us.ihmc.communication.packets.sensing.PointCloudWorldPacket;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCWallAtDistanceEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.graphics3DAdapter.jme.util.JMELidarScanVisualizer;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.robotSide.RobotSide;

public abstract class DepthDataProcessorTest implements MultiRobotTestInterface
{
   private static final int MINIMUM_SCANS_TO_RECIEVE = 60; // GPU Benchmark
   private static final float SCAN_TOLERANCE = 0.001f;
   private static final double WALL_DISTANCE = 1.0;
   private static final boolean ALLOW_PERCENTAGE_OUT_OF_RANGE = true;
   private static final double PERCENT_ALLOWABLE_OUT_OF_RANGE = .05;

   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private int numberOfLidarScansConsumed = 0;
   private long numberOfLidarPointsConsumed = 0;
   private final ConcurrentLinkedQueue<AssertionError> errorQueue = new ConcurrentLinkedQueue<>();
   private JMELidarScanVisualizer jmeLidarScanVisualizer;
   private DRCSimulationTestHelper testHelper;

   @Before
   public void setUp()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before: ");
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

   @EstimatedDuration
   @Test(timeout = 300000)
   public void testIsReceivingScansAnd95PercentOfPointsAreCorrect() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      jmeLidarScanVisualizer = new JMELidarScanVisualizer();
      
      DRCObstacleCourseStartingLocation startingLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      testHelper = new DRCSimulationTestHelper(new DRCWallAtDistanceEnvironment(WALL_DISTANCE), this.getClass().getSimpleName(), null, startingLocation, simulationTestingParameters, getRobotModel());
      testHelper.setupCameraForUnitTest(new Point3d(1.8375, -0.16, 0.89), new Point3d(1.10, 8.30, 1.37));

      testHelper.attachListener(PointCloudWorldPacket.class, new PointCloudWorldConsumer());

      testHelper.attachListener(HandPoseStatus.class, new PacketConsumer<HandPoseStatus>()
      {
         @Override
         public void receivedPacket(HandPoseStatus object)
         {
            PrintTools.debug(DepthDataProcessorTest.this, "Hand pose status recieved!");
         }
      });

      DepthDataStateCommand lidarEnablePacket = new DepthDataStateCommand(LidarState.ENABLE);
      lidarEnablePacket.setDestination(PacketDestination.SENSOR_MANAGER);

      testHelper.simulateAndBlockAndCatchExceptions(1.1);

      Point3d position = RandomTools.generateRandomPoint(new Random(), 0.1, -0.3, 0.7, 0.5, 0.3, 1.3);
      Quat4d orientation = RandomTools.generateRandomQuaternion(new Random(), Math.PI / 4);

      HandPosePacket handPosePacket = new HandPosePacket(RobotSide.LEFT, Frame.CHEST, position, orientation, 2.0);
      handPosePacket.setDestination(PacketDestination.CONTROLLER);

      testHelper.send(handPosePacket);
      
      testHelper.send(lidarEnablePacket);
      
      boolean success = testHelper.simulateAndBlockAndCatchExceptions(5.0);

      assertTrue(success);

      System.out.println("Scans consumed: " + numberOfLidarScansConsumed);
      assertTrue("Lidar scans are not being received; numberOfLidarScansConsumed = " + numberOfLidarScansConsumed, numberOfLidarScansConsumed > MINIMUM_SCANS_TO_RECIEVE);

      System.out.println("Number of points consumed: " + numberOfLidarPointsConsumed + " Points out of range: " + errorQueue.size() + " Percentage: " + ((double) errorQueue.size() / numberOfLidarPointsConsumed) + " less than .05");

      assertTrue("Too many points are out of range: ", (double) errorQueue.size() / numberOfLidarPointsConsumed < PERCENT_ALLOWABLE_OUT_OF_RANGE);
      if (!ALLOW_PERCENTAGE_OUT_OF_RANGE)
         throwAllAssertionErrors();

      BambooTools.reportTestFinishedMessage();
   }

   private void throwAllAssertionErrors()
   {
      while (!errorQueue.isEmpty())
      {
         throw errorQueue.poll();
      }
   }

   private class PointCloudWorldConsumer implements PacketConsumer<PointCloudWorldPacket>
   {
      @Override
      public void receivedPacket(PointCloudWorldPacket pointCloud)
      {
         PrintTools.debug(DepthDataProcessorTest.this, "Point cloud world received.");

         numberOfLidarScansConsumed++;
         // jmeLidarScanVisualizer.updateLidarNodeTransform(sparseLidarScan.getStartTransform());
         jmeLidarScanVisualizer.addPointCloud(Arrays.asList(pointCloud.getDecayingWorldScan()));

         try
         {
            List<Point3f> lidarWorldPoints = Arrays.asList(pointCloud.getDecayingWorldScan());
            numberOfLidarPointsConsumed += lidarWorldPoints.size();

            for (Point3f lidarWorldPoint : lidarWorldPoints)
            {
               if (lidarWorldPoint.getX() > 0.5)
               {
                  assertEquals(WALL_DISTANCE, lidarWorldPoint.getX(), SCAN_TOLERANCE);
               }
            }
         }
         catch (AssertionError assertionError)
         {
            errorQueue.add(assertionError);
         }
      }
   }
}
