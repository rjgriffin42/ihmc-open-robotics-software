package us.ihmc.perception.ouster;

import perception_msgs.msg.dds.ImageMessage;
import perception_msgs.msg.dds.LidarScanMessage;
import std_msgs.msg.dds.Bool;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCROS2Input;
import us.ihmc.communication.ros2.ROS2ControllerPublishSubscribeAPI;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.perception.BytedecoTools;
import us.ihmc.perception.OpenCLManager;
import us.ihmc.perception.netty.NettyOuster;
import us.ihmc.robotics.referenceFrames.ModifiableReferenceFrame;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.tools.thread.Activator;
import us.ihmc.tools.thread.MissingThreadTools;
import us.ihmc.tools.thread.ResettableExceptionHandlingExecutorService;

import java.util.function.Supplier;

/**
 * This class publishes a PNG compressed depth image from the Ouster as fast as the frames come in.
 */
public class OusterDriverAndDepthPublisher
{
   public static final ROS2Topic<Bool> PUBLISH_LIDAR_SCAN = new ROS2Topic<>();
   public static final ROS2Topic<Bool> PUBLISH_HEIGHT_MAP = new ROS2Topic<>();

   private final Activator nativesLoadedActivator;
   private final IHMCROS2Input<Bool> publishLidarScanInput;
   private final IHMCROS2Input<Bool> publishHeightMapInput;
   private final Supplier<HumanoidReferenceFrames> humanoidReferenceFramesSupplier;
   private final Runnable asynchronousCompressAndPublish = this::asynchronousCompressAndPublish;
   private final ResettableExceptionHandlingExecutorService extractCompressAndPublishThread;
   private final NettyOuster ouster;
   private final OusterDepthPublisher depthPublisher;
   private final OusterHeightMapUpdater heightMapUpdater;
   private OpenCLManager openCLManager;
   private OusterDepthExtractionKernel depthExtractionKernel;
   private volatile HumanoidReferenceFrames humanoidReferenceFrames;
   private final ModifiableReferenceFrame ousterSensorFrame = new ModifiableReferenceFrame(ReferenceFrame.getWorldFrame());

   public OusterDriverAndDepthPublisher(ROS2ControllerPublishSubscribeAPI ros2,
                                        Supplier<HumanoidReferenceFrames> humanoidReferenceFramesSupplier,
                                        ROS2Topic<ImageMessage> imageMessageTopic,
                                        ROS2Topic<LidarScanMessage> lidarScanTopic)
   {
      this.humanoidReferenceFramesSupplier = humanoidReferenceFramesSupplier;

      nativesLoadedActivator = BytedecoTools.loadOpenCVNativesOnAThread();

      publishLidarScanInput = ros2.subscribe(PUBLISH_LIDAR_SCAN);
      publishHeightMapInput = ros2.subscribe(PUBLISH_HEIGHT_MAP);

      ouster = new NettyOuster();
      ouster.bind();

      depthPublisher = new OusterDepthPublisher(imageMessageTopic, lidarScanTopic, () -> publishLidarScanInput.getLatest().getData());
      heightMapUpdater = new OusterHeightMapUpdater(ros2);
      publishHeightMapInput.addCallback(publish ->
      {
         if (publish.getData())
            heightMapUpdater.start();
         else
            heightMapUpdater.stop();
      });

      extractCompressAndPublishThread = MissingThreadTools.newSingleThreadExecutor("CopyAndPublish", true, 1);
      // Using incoming Ouster UDP Netty events as the thread scheduler. Only called on last datagram of frame.
      ouster.setOnFrameReceived(this::onFrameReceived);

      Runtime.getRuntime().addShutdownHook(new Thread(() ->
      {
         depthPublisher.destroy();
         heightMapUpdater.stop();
         ouster.setOnFrameReceived(null);
         ouster.destroy();
         ThreadTools.sleepSeconds(0.5);
         extractCompressAndPublishThread.destroy();
      }, getClass().getSimpleName() + "Shutdown"));
   }

   // If we aren't doing anything, copy the data and publish it.
   private synchronized void onFrameReceived()
   {
      if (nativesLoadedActivator.poll() && ouster.isInitialized())
      {
         if (openCLManager == null)
         {
            openCLManager = new OpenCLManager();
            depthExtractionKernel = new OusterDepthExtractionKernel(ouster, openCLManager, () -> publishLidarScanInput.getLatest().getData());
            depthPublisher.initialize(ouster.getImageWidth(), ouster.getImageHeight());
         }

         synchronized (this)
         {
            humanoidReferenceFrames = humanoidReferenceFramesSupplier.get();
         }

         // Fast memcopy while the ouster thread is blocked
         depthExtractionKernel.copyLidarFrameBuffer();
         extractCompressAndPublishThread.clearQueueAndExecute(asynchronousCompressAndPublish);

         if (publishHeightMapInput.getLatest().getData())
         {
            heightMapUpdater.updateWithDataBuffer(humanoidReferenceFrames.getOusterLidarFrame(),
                                                  humanoidReferenceFrames.getMidFeetZUpFrame(),
                                                  depthExtractionKernel.getPointCloudInSensorFrame(),
                                                  ouster.getImageHeight() * ouster.getImageWidth(),
                                                  ouster.getAquisitionInstant());
         }
      }
   }

   private void asynchronousCompressAndPublish()
   {
      synchronized (this) // Avoiding concurrent modification of transforms
      {
         humanoidReferenceFrames.getOusterLidarFrame().getTransformToDesiredFrame(ousterSensorFrame.getTransformToParent(), ReferenceFrame.getWorldFrame());
         ousterSensorFrame.getReferenceFrame().update();
      }
      depthPublisher.extractCompressAndPublish(ousterSensorFrame.getReferenceFrame(), depthExtractionKernel, ouster.getAquisitionInstant());
   }
}
