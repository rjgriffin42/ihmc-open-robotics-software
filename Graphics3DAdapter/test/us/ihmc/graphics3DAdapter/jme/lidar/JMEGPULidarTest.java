package us.ihmc.graphics3DAdapter.jme.lidar;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import us.ihmc.graphics3DAdapter.jme.lidar.manual.JMELidar120FovTest;
import us.ihmc.graphics3DAdapter.jme.lidar.manual.JMELidar360FovTest;
import us.ihmc.graphics3DAdapter.jme.lidar.manual.JMELidar60FovTest;
import us.ihmc.graphics3DAdapter.jme.lidar.manual.JMELidarSphere270FovTest;
import us.ihmc.utilities.code.agileTesting.BambooPlanType;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.BambooPlan;
import us.ihmc.utilities.lidar.polarLidar.LidarScan;
import us.ihmc.utilities.test.JUnitTools;

@BambooPlan(planType={BambooPlanType.UI})
public class JMEGPULidarTest implements LidarTestListener
{
   private static final boolean TEST_MANUALLY = false;
   private JMEGPULidarTestEnviroment lidarTest;
   private LidarTestParameters parameters;
   private boolean stop = false;
   private final LinkedBlockingQueue<ScanPair> scanPairs = new LinkedBlockingQueue<JMEGPULidarTest.ScanPair>();
   private double averageDifference = 0.0;
   private long numScans = 0;

   public static void main(String[] args)
   {
      JUnitCore junit = new JUnitCore();

      for (int i = 0; i < 5; i++)
      {
         System.out.println("Test Number " + i + "...");

         junit.run(JMEGPULidarTest.class);
      }
   }

   @After
   public void tearDown()
   {
      System.out.println("Average difference: " + averageDifference / numScans + " Number of Scans: " + numScans);
      
      assertTrue("Number of scans incorrect: ", numScans == 7920);
      
      numScans = 0;
      averageDifference = 0.0;
      
      lidarTest.getWorld().stop();
   }

	@AverageDuration
	@Test(timeout=300000)
   public void test60DegreeFieldOfView()
   {
      parameters = new JMELidar60FovTest();
      doATest(parameters);
   }

	@AverageDuration
	@Test(timeout=300000)
   public void test120DegreeFieldOfView()
   {
      parameters = new JMELidar120FovTest();
      doATest(parameters);
   }

	@AverageDuration
	@Test(timeout=300000)
   public void test360DegreeFieldOfView()
   {
      parameters = new JMELidar360FovTest();
      doATest(parameters);
   }

	@AverageDuration
	@Test(timeout=300000)
   public void test270DegreeFieldOfView()
   {
      parameters = new JMELidarSphere270FovTest();
      doATest(parameters);
   }

   private void doATest(LidarTestParameters parameters)
   {
      lidarTest = new JMEGPULidarTestEnviroment();

      if (TEST_MANUALLY)
         lidarTest.testManually(parameters, this);
      else
         lidarTest.testAutomatically(parameters, this);

      beginAssertingLidarScans();
   }

   private void beginAssertingLidarScans()
   {
      while (!stop)
      {
         if (!scanPairs.isEmpty())
         {
            ScanPair pair = scanPairs.poll();
            
            JUnitTools.assertLidarScanEquals(pair.gpuScan, pair.traceScan, 1e-7, (float) parameters.getGpuVsTraceTolerance()); 
            
            recordStatistics(pair.gpuScan, pair.traceScan);
         }
      }
   }

   private void recordStatistics(LidarScan gpuScan, LidarScan traceScan)
   {
      for (int i = 0; i < parameters.getScansPerSweep(); i++)
      {
         averageDifference += traceScan.getRange(i) - gpuScan.getRange(i);
         numScans++;
      }
   }

   public void notify(LidarScan gpuScan, LidarScan traceScan)
   {
      scanPairs.add(new ScanPair(gpuScan, traceScan));
   }

   public void stop()
   {
      stop = true;
   }

   private class ScanPair
   {
      private final LidarScan gpuScan;
      private final LidarScan traceScan;

      public ScanPair(LidarScan gpuScan, LidarScan traceScan)
      {
         this.gpuScan = gpuScan;
         this.traceScan = traceScan;
      }
   }
}
