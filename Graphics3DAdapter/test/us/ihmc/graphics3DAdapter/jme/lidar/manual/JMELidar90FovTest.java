package us.ihmc.graphics3DAdapter.jme.lidar.manual;

import us.ihmc.graphics3DAdapter.jme.lidar.LidarTestListener;
import us.ihmc.graphics3DAdapter.jme.lidar.LidarTestParameters;
import us.ihmc.graphics3DAdapter.jme.lidar.JMEGPULidarTestEnviroment;
import us.ihmc.utilities.lidar.polarLidar.LidarScan;

public class JMELidar90FovTest extends LidarTestParameters implements LidarTestListener
{
   public static void main(String[] args)
   {
      JMELidar90FovTest test = new JMELidar90FovTest();
      new JMEGPULidarTestEnviroment().testManually(test, test);
   }

   public JMELidar90FovTest()
   {
      setShowGpuPoints(false);
      setPlaceLidar(false);
      setShowScanRays(false);
      setRotationSpeed(5.0);
      setScansPerSweep(720);
      setLidarTestRotationAmount((2 * Math.PI));
      setRotationLimitEnabled(true);
      setPrintDebug(false);

      setViewWidth(1000);
      setViewHeight(700);
      setShowSky(true);
      setWallDistance(-5.0);
      setPlaceJmeSphere(true);
      setPlaceIhmcSphere(false);
      setPlaceLidar(true);
      setPlaceWall(false);
      setRotateWall(false);
      setShowGpuPoints(true);
      setShowTracePoints(true);
      setShowScanRays(false);
      setGpuVsTraceTolerance(5.0);

      setLidarSweepStartAngle(-Math.PI / 4);
      setLidarSweepEndAngle(Math.PI / 4);
   }

   public void notify(LidarScan gpuScan, LidarScan traceScan)
   {
   }

   public void stop()
   {
   }
}
