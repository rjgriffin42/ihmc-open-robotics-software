package us.ihmc.darpaRoboticsChallenge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotLidarParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPointCloudParameters;
import us.ihmc.graphics3DAdapter.GPULidar;
import us.ihmc.graphics3DAdapter.GPULidarCallback;
import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.utilities.lidar.PointCloudPacket;
import us.ihmc.utilities.lidar.polarLidar.LidarScan;
import us.ihmc.utilities.lidar.polarLidar.geometry.LidarScanParameters;
import us.ihmc.utilities.math.TimeTools;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.TimestampProvider;

import com.yobotics.simulationconstructionset.simulatedSensors.LidarMount;

public class DRCLidar
{
   private static LidarMount getSensor(SDFRobot robot, String sensorName)
   {
      ArrayList<LidarMount> lidarSensors = robot.getSensors(LidarMount.class);
      
      if(lidarSensors.size() == 0)
      {
         System.err.println("DRCLidar: No LIDAR units found on SDF Robot.");

         return null;
      }
      
      for(LidarMount lidarMount : lidarSensors)
      {
         if(lidarMount.getName() == sensorName)
         {
            return lidarMount;
         }
      }
      System.err.println("DRCLidar: Could Not find " + sensorName + "Using " + lidarSensors.get(0).getName() + "Instead! FIX THIS!");
      return lidarSensors.get(0);
   }

   public static void setupDRCRobotLidar(DRCSimulationFactory drcSimulation, ObjectCommunicator objectCommunicator, DRCRobotJointMap jointMap,
         DRCRobotLidarParameters lidarParams, TimestampProvider timestampProvider, boolean startLidar)
 {
    Graphics3DAdapter graphics3dAdapter = drcSimulation.getSimulationConstructionSet().getGraphics3dAdapter();
    if (graphics3dAdapter != null)
    {
       LidarMount lidarMount = getSensor(drcSimulation.getRobot(), lidarParams.getSensorNameInSdf());

       LidarScanParameters lidarScanParameters = lidarMount.getLidarScanParameters();
       int horizontalRays = lidarScanParameters.pointsPerSweep;
       float fov = lidarScanParameters.sweepYawMax - lidarScanParameters.sweepYawMin;
       float near = lidarScanParameters.minRange;
       float far = lidarScanParameters.maxRange;

       DRCLidarCallback callback = new DRCLidarCallback(objectCommunicator, lidarScanParameters, lidarParams.getSensorId());
       GPULidar lidar = graphics3dAdapter.createGPULidar(callback, horizontalRays, fov, near, far);
       lidarMount.setLidar(lidar);
    }
 }

   public static void setupDRCRobotPointCloud(DRCSimulationFactory drcSimulation, ObjectCommunicator objectCommunicator, DRCRobotJointMap jointMap,
         DRCRobotPointCloudParameters pointCloudParams, TimestampProvider timestampProvider, boolean startLidar)
 {
    Graphics3DAdapter graphics3dAdapter = drcSimulation.getSimulationConstructionSet().getGraphics3dAdapter();

    LidarMount lidarMount = getSensor(drcSimulation.getRobot(), pointCloudParams.getSensorNameInSdf());

    LidarScanParameters lidarScanParameters = lidarMount.getLidarScanParameters();
    int horizontalRays = lidarScanParameters.pointsPerSweep;
    float fov = lidarScanParameters.sweepYawMax - lidarScanParameters.sweepYawMin;
    float near = lidarScanParameters.minRange;
    float far = lidarScanParameters.maxRange;


    DRCLidarToPointCloudCallback callback = new DRCLidarToPointCloudCallback(objectCommunicator, lidarScanParameters, pointCloudParams.getSensorId());
    GPULidar lidar = graphics3dAdapter.createGPULidar(callback, horizontalRays, fov, near, far);
    lidarMount.setLidar(lidar);
 }


   public static class DRCLidarToPointCloudCallback implements GPULidarCallback
   {
      private final Executor pool = Executors.newSingleThreadExecutor();

      private final ObjectCommunicator objectCommunicator;
      private final LidarScanParameters lidarScanParameters;
      private int pointCloudSensorId;

      public DRCLidarToPointCloudCallback(ObjectCommunicator objectCommunicator, LidarScanParameters lidarScanParameters, int pointCloudSensorId)
      {
         this.objectCommunicator = objectCommunicator;
         this.lidarScanParameters = lidarScanParameters;
         this.pointCloudSensorId = pointCloudSensorId;
      }

      @Override
      public void scan(float[] scan, Transform3D lidarTransform, double time)
      {
         Transform3D transform = new Transform3D(lidarTransform);
         final LidarScan lidarScan = new LidarScan(new LidarScanParameters(lidarScanParameters, TimeTools.secondsToNanoSeconds(time)), transform, transform,
                                        Arrays.copyOf(scan, scan.length));

         ArrayList<Point3d> points = lidarScan.getAllPoints();
         Point3d[] pointsArray = new Point3d[points.size()];
         points.toArray(pointsArray);

         final PointCloudPacket pointCloud = new PointCloudPacket(pointsArray, TimeTools.secondsToNanoSeconds(time));

         pool.execute(new Runnable()
         {
            @Override
            public void run()
            {
               objectCommunicator.consumeObject(pointCloud);
            }
         });
      }

   }


   public static class DRCLidarCallback implements GPULidarCallback
   {
      private final Executor pool = Executors.newSingleThreadExecutor();

      private final ObjectCommunicator objectCommunicator;
      private final LidarScanParameters lidarScanParameters;
      private final int lidarSensorId;

      public DRCLidarCallback(ObjectCommunicator objectCommunicator, LidarScanParameters lidarScanParameters, int lidarSensorId)
      {
         this.objectCommunicator = objectCommunicator;
         this.lidarScanParameters = lidarScanParameters;
         this.lidarSensorId = lidarSensorId;
      }

      @Override
      public void scan(float[] scan, Transform3D lidarTransform, double time)
      {
         Transform3D transform = new Transform3D(lidarTransform);
         final LidarScan lidarScan = new LidarScan(new LidarScanParameters(lidarScanParameters, TimeTools.secondsToNanoSeconds(time)), transform, transform,
                                        Arrays.copyOf(scan, scan.length),lidarSensorId);

         pool.execute(new Runnable()
         {
            @Override
            public void run()
            {
               objectCommunicator.consumeObject(lidarScan);
            }
         });
      }
   }

}
