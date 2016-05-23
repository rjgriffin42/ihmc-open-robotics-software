package us.ihmc.ihmcPerception.depthData;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point3d;

import us.ihmc.communication.net.ObjectCommunicator;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.communication.packets.SimulatedLidarScanPacket;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.lidar.LidarScan;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class SCSPointCloudLidarReceiver implements ObjectConsumer<SimulatedLidarScanPacket>
{

   private final PointCloudDataReceiverInterface pointCloudDataReceiver;
   private final ReferenceFrame lidarFrame;

   private final ReferenceFrame lidarScanFrame;
   private final RigidBodyTransform identityTransform = new RigidBodyTransform();

   public SCSPointCloudLidarReceiver(ObjectCommunicator scsSensorsCommunicator, ReferenceFrame lidarFrame, ReferenceFrame lidarScanFrame,
         PointCloudDataReceiver pointCloudDataReceiver)
   {
      this.pointCloudDataReceiver = pointCloudDataReceiver;
      this.lidarFrame = lidarFrame;
      this.lidarScanFrame = lidarScanFrame;

      scsSensorsCommunicator.attachListener(SimulatedLidarScanPacket.class, this);
   }

   @Override
   public void consumeObject(SimulatedLidarScanPacket packet)
   {
      LidarScan scan = new LidarScan(packet.getLidarScanParameters(), packet.getRanges(), packet.getSensorId());
      // Set the world transforms to nothing, so points are in lidar scan frame
      scan.setWorldTransforms(identityTransform, identityTransform);
      ArrayList<Point3d> points = scan.getAllPoints();

      long[] timestamps = new long[points.size()];
      Arrays.fill(timestamps, packet.getScanStartTime());

      pointCloudDataReceiver.receivedPointCloudData(lidarScanFrame, lidarFrame, timestamps, points, PointCloudSource.NEARSCAN,PointCloudSource.QUADTREE);
   }

   public void connect()
   {

   }

}
