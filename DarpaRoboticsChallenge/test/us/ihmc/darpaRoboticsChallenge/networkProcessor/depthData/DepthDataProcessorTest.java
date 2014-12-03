package us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.vecmath.Point3d;

import org.junit.Before;
import org.junit.Test;

import us.ihmc.bambooTools.BambooTools;
import us.ihmc.communication.packets.sensing.DepthDataStateCommand;
import us.ihmc.communication.packets.sensing.DepthDataStateCommand.LidarState;
import us.ihmc.communication.packets.sensing.SparseLidarScanPacket;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCWallAtDistanceEnvironment;
import us.ihmc.darpaRoboticsChallenge.networking.DRCUserInterfaceNetworkingManager;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationNetworkTestHelper;
import us.ihmc.graphics3DAdapter.jme.util.JMELidarScanVisualizer;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.net.NetStateListener;
import us.ihmc.utilities.net.ObjectConsumer;

public abstract class DepthDataProcessorTest implements MultiRobotTestInterface, NetStateListener
{
   private static final int MINIMUM_SCANS_TO_RECIEVE = 90;
   private static final float SCAN_TOLERANCE = 0.001f;
   private static final double WALL_DISTANCE = 1.0;
   private static final boolean ALLOW_PERCENTAGE_OUT_OF_RANGE = true;
   private static final double PERCENT_ALLOWABLE_OUT_OF_RANGE = .05;

   private int numberOfLidarScansConsumed = 0;
   private long numberOfLidarPointsConsumed = 0;
   private ConcurrentLinkedQueue<AssertionError> errorQueue = new ConcurrentLinkedQueue<>();
   private JMELidarScanVisualizer jmeLidarScanVisualizer;
   
   @Before
   public void setUp()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before: ");
   }

   @Test
   public void testIsReceivingScansAnd95PercentOfPointsAreCorrect()
   {
      BambooTools.reportTestStartedMessage();
      
      jmeLidarScanVisualizer = new JMELidarScanVisualizer();
      
      DRCSimulationNetworkTestHelper drcSimulationTestHelper = new DRCSimulationNetworkTestHelper(getRobotModel(),
            new DRCWallAtDistanceEnvironment(WALL_DISTANCE),"",true,true);
      drcSimulationTestHelper.setupCamera(new Point3d(1.8375, -0.16, 0.89), new Point3d(1.10, 8.30, 1.37));
      drcSimulationTestHelper.addNetStateListener(this);
      drcSimulationTestHelper.addConsumer(SparseLidarScanPacket.class, new LidarConsumer());
      
      drcSimulationTestHelper.connect();
      
      drcSimulationTestHelper.sendCommand(new DepthDataStateCommand(LidarState.ENABLE));
      
      boolean success = drcSimulationTestHelper.simulate(5);
      
      assertTrue(success);
      
      System.out.println("Scans consumed: " + numberOfLidarScansConsumed);
      assertTrue("Lidar scans are not being received; numberOfLidarScansConsumed = " + numberOfLidarScansConsumed, numberOfLidarScansConsumed > MINIMUM_SCANS_TO_RECIEVE);
      
      System.out.println("Number of points consumed: " + numberOfLidarPointsConsumed + " Points out of range: " + errorQueue.size() + " Percentage: "
            + ((double) errorQueue.size() / numberOfLidarPointsConsumed) + " less than .05");
      
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

   private class LidarConsumer implements ObjectConsumer<SparseLidarScanPacket>
   {
      @Override
      public void consumeObject(SparseLidarScanPacket sparseLidarScan)
      {
         numberOfLidarScansConsumed++;

         jmeLidarScanVisualizer.updateLidarNodeTransform(sparseLidarScan.getStartTransform());
         jmeLidarScanVisualizer.addPointCloud(sparseLidarScan.getAllPoints3f());

         try
         {
            List<Point3d> lidarWorldPoints = sparseLidarScan.getAllPoints();
            numberOfLidarPointsConsumed += lidarWorldPoints.size();
            
            for (Point3d lidarWorldPoint : lidarWorldPoints)
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

   public void connected()
   {
      System.out.println(DRCUserInterfaceNetworkingManager.class.getSimpleName() + ": Connected!");
   }

   public void disconnected()
   {
      System.out.println(DRCUserInterfaceNetworkingManager.class.getSimpleName() + ": Disconnected.");
   }
}
