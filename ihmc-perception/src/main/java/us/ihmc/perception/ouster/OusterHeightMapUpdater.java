package us.ihmc.perception.ouster;

import org.apache.commons.lang3.tuple.Triple;
import perception_msgs.msg.dds.HeightMapMessage;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.property.ROS2StoredPropertySetGroup;
import us.ihmc.communication.ros2.ROS2Helper;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.ihmcPerception.depthData.PointCloudData;
import us.ihmc.ihmcPerception.heightMap.HeightMapAPI;
import us.ihmc.ihmcPerception.heightMap.HeightMapUpdater;
import us.ihmc.ros2.RealtimeROS2Node;
import us.ihmc.tools.thread.ExecutorServiceTools;

import java.nio.FloatBuffer;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class OusterHeightMapUpdater
{
   private static final long updateDTMillis = 100;
   private static final int initialPublishFrequency = 5;

   private final AtomicBoolean updateThreadIsRunning = new AtomicBoolean(false);
   private final HeightMapUpdater heightMapUpdater;
   private final Supplier<ReferenceFrame> groundFrameProvider;

   private final ROS2StoredPropertySetGroup ros2PropertySetGroup;

   private final ScheduledExecutorService executorService = ExecutorServiceTools.newSingleThreadScheduledExecutor(ThreadTools.createNamedThreadFactory(getClass().getSimpleName()),
                                                                                                    ExecutorServiceTools.ExceptionHandling.CATCH_AND_REPORT);
   public OusterHeightMapUpdater(Supplier<ReferenceFrame> groundFrameProvider, RealtimeROS2Node ros2Node)
   {
      this.groundFrameProvider = groundFrameProvider;

      IHMCRealtimeROS2Publisher<HeightMapMessage > heightMapPublisher = ROS2Tools.createPublisher(ros2Node, ROS2Tools.HEIGHT_MAP_OUTPUT);

      heightMapUpdater = new HeightMapUpdater();
      heightMapUpdater.attachHeightMapConsumer(heightMapPublisher::publish);

      ros2PropertySetGroup = new ROS2StoredPropertySetGroup(new ROS2Helper(ros2Node));
      ros2PropertySetGroup.registerStoredPropertySet(HeightMapAPI.PARAMETERS, heightMapUpdater.getHeightMapParameters());
      ros2PropertySetGroup.registerStoredPropertySet(HeightMapAPI.FILTER_PARAMETERS, heightMapUpdater.getHeightMapFilterParameters());

      heightMapUpdater.setPublishFrequency(initialPublishFrequency);
      heightMapUpdater.setEnableUpdates(true);

      executorService.scheduleAtFixedRate(this::update, 0, updateDTMillis, TimeUnit.MILLISECONDS);
   }

   public void updateWithDataBuffer(ReferenceFrame sensorFrame, FloatBuffer pointCloudInSensorFrame, int numberOfPoints, Instant instant)
   {
      double groundHeight = groundFrameProvider.get().getTransformToRoot().getTranslationZ();

      FramePose3D sensorPose = new FramePose3D(sensorFrame);
      sensorPose.changeFrame(ReferenceFrame.getWorldFrame());
      Point3D gridCenter = new Point3D(sensorPose.getX(), sensorPose.getY(), groundHeight);
      PointCloudData pointCloudData = new PointCloudData(instant, numberOfPoints, pointCloudInSensorFrame);

      heightMapUpdater.addPointCloudToQueue(Triple.of(pointCloudData, sensorPose, gridCenter));
   }

   public void update()
   {
      ros2PropertySetGroup.update();

      if (heightMapUpdater.updatesAreEnabled())
      {
         if (!updateThreadIsRunning.getAndSet(true))
         {
            heightMapUpdater.runFullUpdate(updateDTMillis);
            updateThreadIsRunning.set(false);
         }
      }
   }

   public void stop()
   {
      executorService.shutdown();
   }
}
