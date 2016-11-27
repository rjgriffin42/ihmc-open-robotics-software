package us.ihmc.avatar.networkProcessor.lidarScanPublisher;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;

import gnu.trove.list.array.TFloatArrayList;
import sensor_msgs.PointCloud2;
import us.ihmc.communication.net.ObjectCommunicator;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.SimulatedLidarScanPacket;
import us.ihmc.humanoidRobotics.communication.packets.sensing.LidarScanMessage;
import us.ihmc.humanoidRobotics.communication.packets.sensing.RequestLidarScanMessage;
import us.ihmc.humanoidRobotics.kryo.PPSTimestampOffsetProvider;
import us.ihmc.ihmcPerception.depthData.CollisionBoxProvider;
import us.ihmc.ihmcPerception.depthData.CollisionShapeTester;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.lidar.LidarScan;
import us.ihmc.robotics.lidar.LidarScanParameters;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.sensorProcessing.communication.producers.RobotConfigurationDataBuffer;
import us.ihmc.tools.thread.ThreadTools;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.subscriber.RosPointCloudSubscriber;

public class LidarScanPublisher
{
   private static final int MAX_NUMBER_OF_LISTENERS = 10;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String name = getClass().getSimpleName();
   private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(ThreadTools.getNamedThreadFactory(name));
   
   private final AtomicReference<ScanData> scanDataToPublish = new AtomicReference<>(null);

   private final PacketCommunicator packetCommunicator;
   private final ConcurrentLinkedDeque<PacketDestination> listeners = new ConcurrentLinkedDeque<>();

   private final String robotName;
   private final FullHumanoidRobotModel fullRobotModel;
   private ReferenceFrame lidarFrame = null;
   private ReferenceFrame scanPointsFrame = worldFrame;
   private final RobotConfigurationDataBuffer robotConfigurationDataBuffer = new RobotConfigurationDataBuffer();

   private CollisionShapeTester collisionBoxNode = null;
   private PPSTimestampOffsetProvider ppsTimestampOffsetProvider = null;

   public LidarScanPublisher(FullHumanoidRobotModelFactory modelFactory, PacketCommunicator packetCommunicator)
   {
      this.packetCommunicator = packetCommunicator;
      robotName = modelFactory.getRobotDescription().getName();
      fullRobotModel = modelFactory.createFullRobotModel();

      packetCommunicator.attachListener(RobotConfigurationData.class, robotConfigurationDataBuffer);
      packetCommunicator.attachListener(RequestLidarScanMessage.class, createRequestLidarScanMessageConsumer());
   }

   public void start()
   {
      executorService.scheduleAtFixedRate(createPublisherTask(), 0L, 1L, TimeUnit.MILLISECONDS);
   }

   public void shutdown()
   {
      executorService.shutdownNow();
   }

   public void receiveLidarFromROS(String lidarScanROSTopic, URI rosCoreURI)
   {
      String graphName = robotName + "/" + name;
      RosMainNode rosMainNode = new RosMainNode(rosCoreURI, graphName, true);
      receiveLidarFromROS(lidarScanROSTopic, rosMainNode);
   }

   public void receiveLidarFromROS(String lidarScanROSTopic, RosMainNode rosMainNode)
   {
      rosMainNode.attachSubscriber(lidarScanROSTopic, createROSTopicSubscriber());
   }

   public void receiveLidarFromSCS(ObjectCommunicator scsSensorsCommunicator)
   {
      scsSensorsCommunicator.attachListener(SimulatedLidarScanPacket.class, createSimulatedLidarScanPacketConsumer());
   }

   private String lidarName;

   public void setLidarBaseFrame(String lidarName)
   {
      this.lidarName = lidarName;
      lidarFrame = fullRobotModel.getLidarBaseFrame(lidarName);
   }

   public void setScanFrameToWorldFrame()
   {
      scanPointsFrame = worldFrame;
   }

   public void setScanFrameToLidarSensorFrame()
   {
      if (lidarFrame == null)
         throw new RuntimeException("The lidar frame has to be set first.");
      RigidBodyTransform transformToLidarBaseFrame = fullRobotModel.getLidarBaseToSensorTransform(lidarName);
      scanPointsFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("lidarScanFrame", lidarFrame, transformToLidarBaseFrame);
   }

   public void setCollisionBoxProvider(CollisionBoxProvider collisionBoxProvider)
   {
      collisionBoxNode = new CollisionShapeTester(fullRobotModel, collisionBoxProvider);
   }

   public void setPPSTimestampOffsetProvider(PPSTimestampOffsetProvider ppsTimestampOffsetProvider)
   {
      this.ppsTimestampOffsetProvider = ppsTimestampOffsetProvider;
   }

   private RosPointCloudSubscriber createROSTopicSubscriber()
   {
      return new RosPointCloudSubscriber()
      {
         @Override
         public void onNewMessage(PointCloud2 pointCloud)
         {
            UnpackedPointCloud pointCloudData = unpackPointsAndIntensities(pointCloud);
            Point3d[] scanPoints = pointCloudData.getPoints();
            long timestamp = pointCloud.getHeader().getStamp().totalNsecs();

            scanDataToPublish.set(new ScanData(timestamp, scanPoints));
         }
      };
   }

   private ObjectConsumer<SimulatedLidarScanPacket> createSimulatedLidarScanPacketConsumer()
   {
      return new ObjectConsumer<SimulatedLidarScanPacket>()
      {
         private final RigidBodyTransform identityTransform = new RigidBodyTransform();

         @Override
         public void consumeObject(SimulatedLidarScanPacket packet)
         {
            LidarScanParameters lidarScanParameters = packet.getLidarScanParameters();
            float[] ranges = packet.getRanges();
            int sensorId = packet.getSensorId();
            LidarScan scan = new LidarScan(lidarScanParameters, ranges, sensorId);
            // Set the world transforms to nothing, so points are in lidar scan frame
            scan.setWorldTransforms(identityTransform, identityTransform);
            List<Point3d> scanPoints = scan.getAllPoints();
            long timestamp = packet.getScanStartTime();

            scanDataToPublish.set(new ScanData(timestamp, scanPoints));
         }
      };
   }

   private Runnable createPublisherTask()
   {
      return new Runnable()
      {
         private final RigidBodyTransform transformToWorld = new RigidBodyTransform();

         @Override
         public void run()
         {
            ScanData scanData = scanDataToPublish.getAndSet(null);
            if (scanData == null)
               return;

            if (listeners.isEmpty())
               return;

            int count = 0;
            Set<PacketDestination> listenerSet = EnumSet.noneOf(PacketDestination.class);

            while (!listeners.isEmpty() && count < MAX_NUMBER_OF_LISTENERS)
            {
               listenerSet.add(listeners.poll());
               count++;
            }
            listeners.clear();

            long robotTimestamp;

            if (ppsTimestampOffsetProvider == null)
            {
               robotTimestamp = scanData.getTimestamp();
               robotConfigurationDataBuffer.updateFullRobotModelWithNewestData(fullRobotModel, null);
            }
            else
            {
               long timestamp = scanData.getTimestamp();
               robotTimestamp = ppsTimestampOffsetProvider.adjustTimeStampToRobotClock(timestamp);
               boolean waitForTimestamp = true;
               boolean success = robotConfigurationDataBuffer.updateFullRobotModel(waitForTimestamp, robotTimestamp, fullRobotModel, null) != -1;
               if (!success)
                  return;
            }

            if (!scanPointsFrame.isWorldFrame())
            {
               scanPointsFrame.getTransformToDesiredFrame(transformToWorld, worldFrame);
               scanData.transform(transformToWorld);
            }

            float[] scanPointBuffer;

            if (collisionBoxNode != null)
            {
               collisionBoxNode.update();
               scanPointBuffer = scanData.getFilteredScanBuffer(collisionBoxNode);
            }
            else
            {
               scanPointBuffer = scanData.getScanBuffer();
            }

            Point3f lidarPosition;
            Quat4f lidarOrientation;

            if (lidarFrame != null)
            {
               lidarPosition = new Point3f();
               lidarOrientation = new Quat4f();
               lidarFrame.getTransformToDesiredFrame(transformToWorld, worldFrame);
               transformToWorld.get(lidarOrientation, lidarPosition);
            }
            else
            {
               lidarPosition = null;
               lidarOrientation = null;
            }

            sendLidarScanMessageToListeners(robotTimestamp, lidarPosition, lidarOrientation, scanPointBuffer, listenerSet);
            listeners.clear();
         }
      };
   }
   
   private void sendLidarScanMessageToListeners(long timestamp, Point3f lidarPosition, Quat4f lidarOrientation, float[] scanPointBuffer, Set<PacketDestination> listeners)
   {
      LidarScanMessage message = new LidarScanMessage(timestamp, lidarPosition, lidarOrientation, scanPointBuffer);

      Iterator<PacketDestination> iterator = listeners.iterator();

      while (true)
      {
         PacketDestination destination = iterator.next();
         message.setDestination(destination);
         packetCommunicator.send(message);

         if (!iterator.hasNext())
            break;

         lidarPosition = lidarPosition == null ? null : new Point3f(lidarPosition);
         lidarOrientation = lidarOrientation == null ? null : new Quat4f(lidarOrientation);
         scanPointBuffer = Arrays.copyOf(scanPointBuffer, scanPointBuffer.length);
         message = new LidarScanMessage(timestamp, lidarPosition, lidarOrientation, scanPointBuffer);
      }
   }

   private PacketConsumer<RequestLidarScanMessage> createRequestLidarScanMessageConsumer()
   {
      return new PacketConsumer<RequestLidarScanMessage>()
      {
         @Override
         public void receivedPacket(RequestLidarScanMessage packet)
         {
            if (packet != null)
               listeners.add(PacketDestination.fromOrdinal(packet.getSource()));
         }
      };
   }

   private class ScanData
   {
      private final long timestamp;
      private final Point3d[] scanPoints;
      private final int numberOfScanPoints;

      public ScanData(long timestamp, Point3d[] scanPoints)
      {
         this.timestamp = timestamp;
         this.scanPoints = scanPoints;
         numberOfScanPoints = scanPoints.length;
      }

      public ScanData(long timestamp, List<Point3d> scanPoints)
      {
         this.timestamp = timestamp;
         this.scanPoints = scanPoints.toArray(new Point3d[0]);
         numberOfScanPoints = scanPoints.size();
      }

      public long getTimestamp()
      {
         return timestamp;
      }

      public void transform(RigidBodyTransform transform)
      {
         for (int i = 0; i < numberOfScanPoints; i++)
            transform.transform(scanPoints[i]);
      }

      public float[] getFilteredScanBuffer(CollisionShapeTester collisionShapeTester)
      {
         TFloatArrayList scanPointBuffer = new TFloatArrayList();

         for (int i = 0; i < numberOfScanPoints; i++)
         {
            Point3d scanPoint = scanPoints[i];

            if (collisionShapeTester.contains(scanPoint))
               continue;

            scanPointBuffer.add((float) scanPoint.getX());
            scanPointBuffer.add((float) scanPoint.getY());
            scanPointBuffer.add((float) scanPoint.getZ());
         }

         return scanPointBuffer.toArray();
      }

      public float[] getScanBuffer()
      {
         TFloatArrayList scanPointBuffer = new TFloatArrayList();

         for (int i = 0; i < numberOfScanPoints; i++)
         {
            Point3d scanPoint = scanPoints[i];

            scanPointBuffer.add((float) scanPoint.getX());
            scanPointBuffer.add((float) scanPoint.getY());
            scanPointBuffer.add((float) scanPoint.getZ());
         }

         return scanPointBuffer.toArray();
      }
   }
}
