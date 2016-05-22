package us.ihmc.ihmcPerception.depthData;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.tools.time.Timer;

import javax.vecmath.Point3d;
import java.util.ArrayList;

public class AsyncPointCloudReceiver implements PointCloudDataReceiverInterface
{
   private final boolean DEBUG = false;
   private Timer timer = DEBUG ? new Timer().start() : null;

   private final SDFFullRobotModel fullRobotModel;

   private volatile ArrayList<Point3d> pointsInWorldFrame;
   private volatile double groundHeight;
   private volatile long timestamp;

   public AsyncPointCloudReceiver(SDFFullRobotModel fullRobotModel)
   {
      this.fullRobotModel = fullRobotModel;
   }

   @Override
   public void receivedPointCloudData(ReferenceFrame scanFrame, ReferenceFrame lidarFrame, long[] timestamps, ArrayList<Point3d> points,
         PointCloudSource... sources)
   {
      if(DEBUG)
      {
         System.out.println(getClass().getSimpleName() + ": Received point cloud in " + scanFrame.getName() + " frame @ "
               + 1.0 / timer.lap() + " FPS from " + Thread.currentThread().getName());
      }

      if(timestamps.length > 0)
      {
         lidarFrame.update();
         RigidBodyTransform lidarTransform = lidarFrame.getTransformToWorldFrame();


         double localGroundHeight = Double.MAX_VALUE;
         for(Point3d point : points)
         {
            lidarTransform.transform(point);
            if(point.getZ() < localGroundHeight) localGroundHeight = point.getZ();
         }

         synchronized(this)
         {
            pointsInWorldFrame = points;
            groundHeight = localGroundHeight;
            timestamp = timestamps[0];
         }
      }
   }

   @Override
   public ReferenceFrame getLidarFrame(String sensorNameInSdf)
   {
      return ReferenceFrame.constructFrameWithUnchangingTransformFromParent(sensorNameInSdf, fullRobotModel.getLidarBaseFrame(sensorNameInSdf), fullRobotModel.getLidarBaseToSensorTransform(sensorNameInSdf));
   }

   public synchronized ArrayList<Point3d> getPointsInWorldFrame()
   {
      return pointsInWorldFrame;
   }

   public synchronized double getGroundHeight()
   {
      return groundHeight;
   }

   public synchronized long getTimestamp()
   {
      return timestamp;
   }
}
