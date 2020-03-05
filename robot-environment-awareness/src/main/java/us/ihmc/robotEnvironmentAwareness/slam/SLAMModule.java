package us.ihmc.robotEnvironmentAwareness.slam;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import javafx.scene.paint.Color;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.jOctoMap.ocTree.NormalOcTree;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.robotEnvironmentAwareness.communication.REAModuleAPI;
import us.ihmc.robotEnvironmentAwareness.communication.converters.OcTreeMessageConverter;
import us.ihmc.robotEnvironmentAwareness.communication.packets.NormalOcTreeMessage;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools.ExceptionHandling;
import us.ihmc.robotEnvironmentAwareness.ui.graphicsBuilders.StereoVisionPointCloudViewer;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.Ros2Node;

public class SLAMModule
{
   private final Messager reaMessager;

   private static final double DEFAULT_OCTREE_RESOLUTION = 0.02;

   private static final Color LATEST_ORIGINAL_POINT_CLOUD_COLOR = Color.BEIGE;
   private static final Color SOURCE_POINT_CLOUD_COLOR = Color.BLACK;
   private static final Color LATEST_POINT_CLOUD_COLOR = Color.LIME;

   private final AtomicReference<Boolean> enable;

   private final Topic<PlanarRegionsListMessage> planarRegionsStateTopicToSubmit;
   private final AtomicReference<StereoVisionPointCloudMessage> newPointCloud = new AtomicReference<>(null);
   private final LinkedList<StereoVisionPointCloudMessage> pointCloudQueue = new LinkedList<StereoVisionPointCloudMessage>();

   private final RandomICPSLAM slam = new RandomICPSLAM(DEFAULT_OCTREE_RESOLUTION);

   private ScheduledExecutorService executorService = ExecutorServiceTools.newSingleThreadScheduledExecutor(getClass(), ExceptionHandling.CATCH_AND_REPORT);
   private static final int THREAD_PERIOD_MILLISECONDS = 1;
   private ScheduledFuture<?> scheduledMain;
   private ScheduledFuture<?> scheduledSLAM;

   private final AtomicReference<RandomICPSLAMParameters> ihmcSLAMParameters;

   private static final String PLANAR_REGIONS_LIST_TOPIC_SURFIX = "_slam";
   private final IHMCROS2Publisher<PlanarRegionsListMessage> planarRegionPublisher;

   public SLAMModule(Ros2Node ros2Node, Messager reaMessager, Topic<Boolean> slamEnableTopic, Topic<PlanarRegionsListMessage> slamPlanarRegionsStateTopic)
   {
      this.reaMessager = reaMessager;
      enable = reaMessager.createInput(slamEnableTopic, true);
      planarRegionsStateTopicToSubmit = slamPlanarRegionsStateTopic;

      ihmcSLAMParameters = reaMessager.createInput(REAModuleAPI.SLAMParameters, new RandomICPSLAMParameters());

      reaMessager.registerTopicListener(REAModuleAPI.SLAMClear, (content) -> clearSLAM());

      MessageTopicNameGenerator publisherTopicNameGenerator;
      publisherTopicNameGenerator = (Class<?> T) -> ROS2Tools.appendTypeToTopicName(ROS2Tools.IHMC_ROS_TOPIC_PREFIX, T) + PLANAR_REGIONS_LIST_TOPIC_SURFIX;
      planarRegionPublisher = ROS2Tools.createPublisher(ros2Node, PlanarRegionsListMessage.class, publisherTopicNameGenerator);
   }

   public void start() throws IOException
   {
      if (scheduledMain == null)
      {
         scheduledMain = executorService.scheduleAtFixedRate(this::updateMain, 0, THREAD_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
      }

      if (scheduledSLAM == null)
      {
         scheduledSLAM = executorService.scheduleAtFixedRate(this::updateSLAM, 0, THREAD_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
      }
   }

   public void stop() throws Exception
   {
      LogTools.info("IhmcSLAMModule is going down.");

      reaMessager.closeMessager();

      if (scheduledMain != null)
      {
         scheduledMain.cancel(true);
         scheduledMain = null;
      }

      if (scheduledSLAM != null)
      {
         scheduledSLAM.cancel(true);
         scheduledSLAM = null;
      }

      if (executorService != null)
      {
         executorService.shutdownNow();
         executorService = null;
      }
   }

   private boolean isMainThreadInterrupted()
   {
      return Thread.interrupted() || scheduledMain == null || scheduledMain.isCancelled();
   }

   private boolean isSLAMThreadInterrupted()
   {
      return Thread.interrupted() || scheduledSLAM == null || scheduledSLAM.isCancelled();
   }

   public void updateSLAM()
   {
      if (isSLAMThreadInterrupted())
         return;

      if (pointCloudQueue.size() == 0)
         return;

      updateSLAMParameters();

      StereoVisionPointCloudMessage pointCloudToCompute = pointCloudQueue.getFirst();

      String stringToReport = "";
      boolean success;
      if (slam.isEmpty())
      {
         slam.addKeyFrame(pointCloudToCompute);
         success = true;
      }
      else
      {
         success = slam.addFrame(pointCloudToCompute);
      }
      pointCloudQueue.removeFirst();
      reaMessager.submitMessage(REAModuleAPI.QueuedBuffers, pointCloudQueue.size() + " [" + slam.getSensorPoses().size() + "]");
      stringToReport = stringToReport + success + " " + slam.getSensorPoses().size() + " " + slam.getComputationTimeForLatestFrame() + " (sec) ";
      reaMessager.submitMessage(REAModuleAPI.SLAMStatus, stringToReport);

      if (success)
      {
         NormalOcTree octreeMap = slam.getOctree();
         NormalOcTreeMessage octreeMessage = OcTreeMessageConverter.convertToMessage(octreeMap);
         reaMessager.submitMessage(REAModuleAPI.SLAMOctreeMapState, octreeMessage);

         slam.updatePlanarRegionsMap();
         PlanarRegionsList planarRegionsMap = slam.getPlanarRegionsMap();
         PlanarRegionsListMessage planarRegionsListMessage = PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegionsMap);
         reaMessager.submitMessage(planarRegionsStateTopicToSubmit, planarRegionsListMessage);
         planarRegionPublisher.publish(planarRegionsListMessage);

         SLAMFrame latestFrame = slam.getLatestFrame();
         Point3DReadOnly[] originalPointCloud = latestFrame.getOriginalPointCloud();
         Point3DReadOnly[] correctedPointCloud = latestFrame.getPointCloud();
         Point3DReadOnly[] sourcePointsToWorld = slam.getSourcePointsToWorldLatestFrame();
         if (originalPointCloud == null || sourcePointsToWorld == null || correctedPointCloud == null)
            return;
         StereoVisionPointCloudMessage latestStereoMessage = createLatestFrameStereoVisionPointCloudMessage(originalPointCloud,
                                                                                                            sourcePointsToWorld,
                                                                                                            correctedPointCloud);
         latestStereoMessage.getSensorPosition().set(latestFrame.getSensorPose().getTranslation());
         latestStereoMessage.getSensorOrientation().set(latestFrame.getSensorPose().getRotation());
         reaMessager.submitMessage(REAModuleAPI.IhmcSLAMFrameState, latestStereoMessage);
      }
   }

   private StereoVisionPointCloudMessage createLatestFrameStereoVisionPointCloudMessage(Point3DReadOnly[] originalPointCloud,
                                                                                        Point3DReadOnly[] sourcePointsToWorld,
                                                                                        Point3DReadOnly[] correctedPointCloud)
   {
      int numberOfPointsToPack = originalPointCloud.length + sourcePointsToWorld.length + correctedPointCloud.length;

      float[] pointCloudBuffer = new float[numberOfPointsToPack * 3];
      int[] colorBuffer = new int[numberOfPointsToPack * 3];
      for (int i = 0; i < originalPointCloud.length; i++)
      {
         pointCloudBuffer[3 * i + 0] = (float) originalPointCloud[i].getX();
         pointCloudBuffer[3 * i + 1] = (float) originalPointCloud[i].getY();
         pointCloudBuffer[3 * i + 2] = (float) originalPointCloud[i].getZ();
         colorBuffer[i] = StereoVisionPointCloudViewer.colorToInt(LATEST_ORIGINAL_POINT_CLOUD_COLOR);
      }
      for (int i = originalPointCloud.length; i < originalPointCloud.length + sourcePointsToWorld.length; i++)
      {
         pointCloudBuffer[3 * i + 0] = (float) sourcePointsToWorld[i - originalPointCloud.length].getX();
         pointCloudBuffer[3 * i + 1] = (float) sourcePointsToWorld[i - originalPointCloud.length].getY();
         pointCloudBuffer[3 * i + 2] = (float) sourcePointsToWorld[i - originalPointCloud.length].getZ();
         colorBuffer[i] = StereoVisionPointCloudViewer.colorToInt(SOURCE_POINT_CLOUD_COLOR);
      }
      for (int i = originalPointCloud.length + sourcePointsToWorld.length; i < numberOfPointsToPack; i++)
      {
         pointCloudBuffer[3 * i + 0] = (float) correctedPointCloud[i - originalPointCloud.length - sourcePointsToWorld.length].getX();
         pointCloudBuffer[3 * i + 1] = (float) correctedPointCloud[i - originalPointCloud.length - sourcePointsToWorld.length].getY();
         pointCloudBuffer[3 * i + 2] = (float) correctedPointCloud[i - originalPointCloud.length - sourcePointsToWorld.length].getZ();
         colorBuffer[i] = StereoVisionPointCloudViewer.colorToInt(LATEST_POINT_CLOUD_COLOR);
      }
      return MessageTools.createStereoVisionPointCloudMessage(19870612L, pointCloudBuffer, colorBuffer);
   }

   public void updateMain()
   {
      if (isMainThreadInterrupted())
         return;

      if (enable.get())
      {
         StereoVisionPointCloudMessage pointCloud = newPointCloud.getAndSet(null);
         if (pointCloud == null)
            return;

         pointCloudQueue.add(pointCloud);
      }
   }

   private void updateSLAMParameters()
   {
      RandomICPSLAMParameters parameters = ihmcSLAMParameters.get();
      slam.updateParameters(parameters);
   }

   public void clearSLAM()
   {
      pointCloudQueue.clear();
      slam.clear();
      newPointCloud.set(null);
   }

   public void handlePointCloud(StereoVisionPointCloudMessage message)
   {
      newPointCloud.set(message);
   }
}
