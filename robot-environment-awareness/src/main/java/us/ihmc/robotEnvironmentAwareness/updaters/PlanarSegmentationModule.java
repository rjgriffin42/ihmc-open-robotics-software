package us.ihmc.robotEnvironmentAwareness.updaters;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.AtomicDouble;

import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.REAStateRequestMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.bytedeco.librealsense.timestamp_data;
import us.ihmc.commons.Conversions;
import us.ihmc.commons.time.Stopwatch;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.euclid.geometry.Pose3D;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.jOctoMap.boundingBox.OcTreeBoundingBoxWithCenterAndYaw;
import us.ihmc.jOctoMap.node.NormalOcTreeNode;
import us.ihmc.jOctoMap.ocTree.NormalOcTree;
import us.ihmc.jOctoMap.tools.JOctoMapTools;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.pubsub.subscriber.Subscriber;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.robotEnvironmentAwareness.communication.REAModuleAPI;
import us.ihmc.robotEnvironmentAwareness.communication.SegmentationModuleAPI;
import us.ihmc.robotEnvironmentAwareness.communication.converters.BoundingBoxMessageConverter;
import us.ihmc.robotEnvironmentAwareness.communication.converters.OcTreeMessageConverter;
import us.ihmc.robotEnvironmentAwareness.communication.packets.BoundingBoxParametersMessage;
import us.ihmc.robotEnvironmentAwareness.io.FilePropertyHelper;
import us.ihmc.robotEnvironmentAwareness.perceptionSuite.PerceptionModule;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PlanarRegionSegmentationParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.PolygonizerParameters;
import us.ihmc.robotEnvironmentAwareness.planarRegion.SurfaceNormalFilterParameters;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools;
import us.ihmc.robotEnvironmentAwareness.tools.ExecutorServiceTools.ExceptionHandling;
import us.ihmc.robotEnvironmentAwareness.ui.graphicsBuilders.OcTreeMeshBuilder;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.ros2.Ros2Node;

public class PlanarSegmentationModule implements OcTreeConsumer, PerceptionModule
{
   private static final String planarRegionsTimeReport = "OcTreePlanarRegion update took: ";
   private static final String reportPlanarRegionsStateTimeReport = "Reporting Planar Regions state took: ";

   private final TimeReporter timeReporter = new TimeReporter();
   Stopwatch stopwatch = new Stopwatch();

   private static final int THREAD_PERIOD_MILLISECONDS = 200;

   protected static final boolean DEBUG = true;

   private final ROS2Topic<?> outputTopic;
   private boolean manageRosNode;
   private final Ros2Node ros2Node;

   private final REAPlanarRegionFeatureUpdater planarRegionFeatureUpdater;

   private final SegmentationModuleStateReporter moduleStateReporter;
   private final IHMCROS2Publisher<PlanarRegionsListMessage> planarRegionPublisher;

   private final AtomicReference<Boolean> clearOcTree;
   private final AtomicReference<Pair<NormalOcTree, Long>> ocTree = new AtomicReference<>(null);
   private final AtomicReference<NormalOcTree> currentOcTree = new AtomicReference<>(null);
   private final AtomicReference<Pose3DReadOnly> sensorPose;

   private final AtomicReference<Boolean> runAsynchronously;
   private final AtomicReference<Boolean> isOcTreeBoundingBoxRequested;
   private final AtomicReference<BoundingBoxParametersMessage> atomicBoundingBoxParameters;
   private final AtomicReference<Boolean> useBoundingBox;

   private ScheduledExecutorService executorService = ExecutorServiceTools.newSingleThreadScheduledExecutor(getClass(), ExceptionHandling.CATCH_AND_REPORT);
   private ScheduledFuture<?> scheduled;
   private final Messager reaMessager;

   private PlanarSegmentationModule(Messager reaMessager, String configurationFilePath) throws Exception
   {
      this(ROS2Tools.createRos2Node(PubSubImplementation.FAST_RTPS, ROS2Tools.REA_NODE_NAME), reaMessager, configurationFilePath, true);
   }

   private PlanarSegmentationModule(Ros2Node ros2Node, Messager reaMessager, String configurationFilePath) throws Exception
   {
      this(ros2Node, reaMessager, configurationFilePath, false);
   }

   private PlanarSegmentationModule(Ros2Node ros2Node, Messager reaMessager, String configurationFilePath, boolean manageRosNode) throws Exception
   {
      this(ros2Node,
           REACommunicationProperties.inputTopic,
           REACommunicationProperties.subscriberCustomRegionsTopicName,
           ROS2Tools.REALSENSE_SLAM_REGIONS,
           reaMessager,
           configurationFilePath,
           manageRosNode);
   }

   private PlanarSegmentationModule(Ros2Node ros2Node,
                                    ROS2Topic<?> inputTopic,
                                    ROS2Topic<?> customRegionTopic,
                                    ROS2Topic<?> outputTopic,
                                    Messager reaMessager,
                                    String configurationFilePath,
                                    boolean manageRosNode) throws Exception
   {
      this.outputTopic = outputTopic;
      this.manageRosNode = manageRosNode;
      this.reaMessager = reaMessager;
      this.ros2Node = ros2Node;

      File configurationFile = setupConfigurationFile(configurationFilePath);

      if (!reaMessager.isMessagerOpen())
         reaMessager.startMessager();

      moduleStateReporter = new SegmentationModuleStateReporter(reaMessager);

      planarRegionFeatureUpdater = new REAPlanarRegionFeatureUpdater(reaMessager, SegmentationModuleAPI.RequestEntireModuleState);
      planarRegionFeatureUpdater.setOcTreeEnableTopic(SegmentationModuleAPI.OcTreeEnable);
      planarRegionFeatureUpdater.setPlanarRegionSegmentationEnableTopic(SegmentationModuleAPI.PlanarRegionsSegmentationEnable);
      planarRegionFeatureUpdater.setPlanarRegionSegmentationClearTopic(SegmentationModuleAPI.PlanarRegionsSegmentationClear);
      planarRegionFeatureUpdater.setCustomRegionsMergingEnableTopic(SegmentationModuleAPI.CustomRegionsMergingEnable);
      planarRegionFeatureUpdater.setCustomRegionsClearTopic(SegmentationModuleAPI.CustomRegionsClear);
      planarRegionFeatureUpdater.setPlanarRegionsPolygonizerEnableTopic(SegmentationModuleAPI.PlanarRegionsPolygonizerEnable);
      planarRegionFeatureUpdater.setPlanarRegionsPolygonizerClearTopic(SegmentationModuleAPI.PlanarRegionsPolygonizerClear);
      planarRegionFeatureUpdater.setPlanarRegionsIntersectionEnableTopic(SegmentationModuleAPI.PlanarRegionsIntersectionEnable);
      planarRegionFeatureUpdater.setPlanarRegionSegmentationParametersTopic(SegmentationModuleAPI.PlanarRegionsSegmentationParameters);
      planarRegionFeatureUpdater.setCustomRegionMergeParametersTopic(SegmentationModuleAPI.CustomRegionsMergingParameters);
      planarRegionFeatureUpdater.setPlanarRegionsConcaveHullFactoryParametersTopic(SegmentationModuleAPI.PlanarRegionsConcaveHullParameters);
      planarRegionFeatureUpdater.setPlanarRegionsPolygonizerParametersTopic(SegmentationModuleAPI.PlanarRegionsPolygonizerParameters);
      planarRegionFeatureUpdater.setPlanarRegionsIntersectionParametersTopic(SegmentationModuleAPI.PlanarRegionsIntersectionParameters);
      planarRegionFeatureUpdater.setSurfaceNormalFilterParametersTopic(SegmentationModuleAPI.SurfaceNormalFilterParameters);
      planarRegionFeatureUpdater.bindControls();

      reaMessager.registerTopicListener(SegmentationModuleAPI.RequestEntireModuleState, messageContent -> sendCurrentState());

      runAsynchronously = reaMessager.createInput(SegmentationModuleAPI.RunAsynchronously, true);
      isOcTreeBoundingBoxRequested = reaMessager.createInput(SegmentationModuleAPI.RequestBoundingBox, false);

      useBoundingBox = reaMessager.createInput(SegmentationModuleAPI.OcTreeBoundingBoxEnable, true);
      atomicBoundingBoxParameters = reaMessager.createInput(SegmentationModuleAPI.OcTreeBoundingBoxParameters,
                                                            BoundingBoxMessageConverter.createBoundingBoxParametersMessage(-1.0f,
                                                                                                                           -2.0f,
                                                                                                                           -3.0f,
                                                                                                                           5.0f,
                                                                                                                           2.0f,
                                                                                                                           0.5f));

      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node,
                                                    PlanarRegionsListMessage.class,
                                                    customRegionTopic,
                                                    this::dispatchCustomPlanarRegion);
      // TODO what the heck is this used for?
      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node, REAStateRequestMessage.class, inputTopic, this::handleREAStateRequestMessage);

      planarRegionPublisher = ROS2Tools.createPublisherTypeNamed(ros2Node, PlanarRegionsListMessage.class, outputTopic);

      FilePropertyHelper filePropertyHelper = new FilePropertyHelper(configurationFile);
      loadConfigurationFile(filePropertyHelper);

      reaMessager.registerTopicListener(SegmentationModuleAPI.SaveUpdaterConfiguration, (content) -> saveConfigurationFIle(filePropertyHelper));

      clearOcTree = reaMessager.createInput(SegmentationModuleAPI.OcTreeClear, false);
      sensorPose = reaMessager.createInput(SegmentationModuleAPI.SensorPose, null);

      PlanarRegionSegmentationParameters planarRegionSegmentationParameters = new PlanarRegionSegmentationParameters();
      planarRegionSegmentationParameters.setMaxDistanceFromPlane(0.03);
      planarRegionSegmentationParameters.setMinRegionSize(150);
      planarRegionFeatureUpdater.setPlanarRegionSegmentationParameters(planarRegionSegmentationParameters);

      SurfaceNormalFilterParameters surfaceNormalFilterParameters = new SurfaceNormalFilterParameters();
      surfaceNormalFilterParameters.setUseSurfaceNormalFilter(true);
      surfaceNormalFilterParameters.setSurfaceNormalLowerBound(Math.toRadians(-40.0));
      surfaceNormalFilterParameters.setSurfaceNormalUpperBound(Math.toRadians(40.0));
      planarRegionFeatureUpdater.setSurfaceNormalFilterParameters(surfaceNormalFilterParameters);

      PolygonizerParameters polygonizerParameters = new PolygonizerParameters();
      polygonizerParameters.setConcaveHullThreshold(0.15);
      planarRegionFeatureUpdater.setPolygonizerParameters(polygonizerParameters);

      reaMessager.submitMessage(SegmentationModuleAPI.UIOcTreeDisplayType, OcTreeMeshBuilder.DisplayType.PLANE);
      reaMessager.submitMessage(SegmentationModuleAPI.UIOcTreeColoringMode, OcTreeMeshBuilder.ColoringType.REGION);

      // At the very end, we force the modules to submit their state so duplicate inputs have consistent values.
      reaMessager.submitMessage(SegmentationModuleAPI.RequestEntireModuleState, true);
   }

   @Override
   public void reportOcTree(NormalOcTree ocTree, Pose3DReadOnly sensorPose)
   {
      long latestTimestamp = -2L;
      for (NormalOcTreeNode node : ocTree)
      {
         if (node.getLastHitTimestamp() > latestTimestamp)
            latestTimestamp = node.getLastHitTimestamp();
      }
      LogTools.debug("Received ocTree. size: " + ocTree.size()
                    + " hash: " + ocTree.hashCode()
                    + " timestamp: " + latestTimestamp
                    + " sensorPosition: " + sensorPose
      );
      this.ocTree.set(Pair.of(ocTree, latestTimestamp));

      reaMessager.submitMessage(SegmentationModuleAPI.OcTreeState, OcTreeMessageConverter.convertToMessage(ocTree));
      reaMessager.submitMessage(SegmentationModuleAPI.SensorPose, sensorPose);

      if (!runAsynchronously.get())
      {
         if (clearOcTree.getAndSet(false))
         {
            planarRegionFeatureUpdater.clearOcTree();
         }

         compute(ocTree, sensorPose);
      }
   }

   private void dispatchCustomPlanarRegion(Subscriber<PlanarRegionsListMessage> subscriber)
   {
      PlanarRegionsListMessage message = subscriber.takeNextData();
      PlanarRegionsList customPlanarRegions = PlanarRegionMessageConverter.convertToPlanarRegionsList(message);
      customPlanarRegions.getPlanarRegionsAsList().forEach(planarRegionFeatureUpdater::registerCustomPlanarRegion);
   }

   private void handleREAStateRequestMessage(Subscriber<REAStateRequestMessage> subscriber)
   {
      REAStateRequestMessage newMessage = subscriber.takeNextData();

      if (newMessage.getRequestResume())
         reaMessager.submitMessage(SegmentationModuleAPI.OcTreeEnable, true);
      else if (newMessage.getRequestPause()) // We guarantee to resume if requested, regardless of the pause request.
         reaMessager.submitMessage(SegmentationModuleAPI.OcTreeEnable, false);
      if (newMessage.getRequestClear())
         clearOcTree.set(true);
   }

   private void loadConfigurationFile(FilePropertyHelper filePropertyHelper)
   {
      planarRegionFeatureUpdater.loadConfiguration(filePropertyHelper);

      Boolean useBoundingBoxFile = filePropertyHelper.loadBooleanProperty(SegmentationModuleAPI.OcTreeBoundingBoxEnable.getName());
      if (useBoundingBoxFile != null)
         useBoundingBox.set(useBoundingBoxFile);
      String boundingBoxParametersFile = filePropertyHelper.loadProperty(SegmentationModuleAPI.OcTreeBoundingBoxParameters.getName());
      if (boundingBoxParametersFile != null)
         atomicBoundingBoxParameters.set(BoundingBoxMessageConverter.parse(boundingBoxParametersFile));
   }

   private void saveConfigurationFIle(FilePropertyHelper filePropertyHelper)
   {
      planarRegionFeatureUpdater.saveConfiguration(filePropertyHelper);
      filePropertyHelper.saveProperty(SegmentationModuleAPI.RunAsynchronously.getName(), runAsynchronously.get());
      filePropertyHelper.saveProperty(SegmentationModuleAPI.OcTreeBoundingBoxParameters.getName(), atomicBoundingBoxParameters.get().toString());
      filePropertyHelper.saveProperty(SegmentationModuleAPI.OcTreeBoundingBoxEnable.getName(), useBoundingBox.get());
   }

   private void sendCurrentState()
   {
      reaMessager.submitMessage(SegmentationModuleAPI.RunAsynchronously, runAsynchronously.get());
      reaMessager.submitMessage(SegmentationModuleAPI.OcTreeBoundingBoxEnable, useBoundingBox.get());
      reaMessager.submitMessage(SegmentationModuleAPI.OcTreeBoundingBoxParameters, atomicBoundingBoxParameters.get());
   }

   private final AtomicDouble lastCompleteUpdate = new AtomicDouble(Double.NaN);

   private void mainUpdate()
   {
      if (isThreadInterrupted())
         return;

      try
      {
         Pair<NormalOcTree, Long> ocTreeTimestamp = ocTree.getAndSet(null);
         Pose3DReadOnly latestSensorPose = this.sensorPose.get();
         if (ocTreeTimestamp != null)
         {
            this.sensorPose.set(null); // only set it to null when the other is not and it's been "read"
         }

         if (runAsynchronously.get())
         {
            if (clearOcTree.getAndSet(false))
            {
               planarRegionFeatureUpdater.clearOcTree();
            }

            if (ocTreeTimestamp != null)
            {
               compute(ocTreeTimestamp.getLeft(), latestSensorPose);
            }
         }

         if (isOcTreeBoundingBoxRequested.getAndSet(false))
            reaMessager.submitMessage(SegmentationModuleAPI.OcTreeBoundingBoxState, BoundingBoxMessageConverter.convertToMessage(currentOcTree.get().getBoundingBox()));
      }
      catch (Exception e)
      {
         if (DEBUG)
         {
            e.printStackTrace();
         }
         else
         {
            LogTools.error(e.getClass().getSimpleName());
         }
      }

      double currentTime = JOctoMapTools.nanoSecondsToSeconds(System.nanoTime());
      lastCompleteUpdate.set(currentTime);
   }

   private void compute(NormalOcTree latestOcTree, Pose3DReadOnly latestSensorPose)
   {
      Pose3D sensorPose = new Pose3D(latestSensorPose);
      NormalOcTree mainOcTree = new NormalOcTree(latestOcTree);

      handleBoundingBox(mainOcTree, sensorPose);

      currentOcTree.set(mainOcTree);

      timeReporter.run(() -> planarRegionFeatureUpdater.update(mainOcTree, sensorPose.getPosition()), planarRegionsTimeReport);
      reaMessager.submitMessage(SegmentationModuleAPI.UISegmentationDuration, timeReporter.getStringToReport());

      timeReporter.run(() -> moduleStateReporter.reportPlanarRegionsState(planarRegionFeatureUpdater), reportPlanarRegionsStateTimeReport);

      planarRegionPublisher.publish(PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(planarRegionFeatureUpdater.getPlanarRegionsList()));
   }

   private boolean isThreadInterrupted()
   {
      return Thread.interrupted() || scheduled == null || scheduled.isCancelled();
   }

   public void start()
   {
      LogTools.info("Planar segmentation Module is starting.");

      if (scheduled == null)
      {
         scheduled = executorService.scheduleAtFixedRate(this::mainUpdate, 0, THREAD_PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
      }
   }

   public void stop()
   {
      LogTools.info("Planar segmentation Module is going down.");

      try
      {
         if (reaMessager.isMessagerOpen())
            reaMessager.closeMessager();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      if (manageRosNode)
         ros2Node.destroy();

      if (scheduled != null)
      {
         scheduled.cancel(true);
         scheduled = null;
      }

      if (executorService != null)
      {
         executorService.shutdownNow();
         executorService = null;
      }
   }

   private void handleBoundingBox(NormalOcTree referenceOctree, Pose3D sensorPose)
   {
      if (!useBoundingBox.get())
      {
         referenceOctree.disableBoundingBox();
         return;
      }

      OcTreeBoundingBoxWithCenterAndYaw boundingBox = new OcTreeBoundingBoxWithCenterAndYaw();

      Point3D min = atomicBoundingBoxParameters.get().getMin();
      Point3D max = atomicBoundingBoxParameters.get().getMax();
      boundingBox.setLocalMinMaxCoordinates(min, max);

      if (sensorPose != null)
      {
         boundingBox.setOffset(sensorPose.getPosition());
         boundingBox.setYawFromQuaternion(new Quaternion(sensorPose.getOrientation()));
      }

      boundingBox.update(referenceOctree.getResolution(), referenceOctree.getTreeDepth());
      referenceOctree.setBoundingBox(boundingBox);
   }


   public static PlanarSegmentationModule createIntraprocessModule(String configurationFilePath, Messager messager) throws Exception

      {
      return new PlanarSegmentationModule(messager, configurationFilePath);
   }

   public static PlanarSegmentationModule createIntraprocessModule(String configurationFilePath, Ros2Node ros2Node, Messager messager) throws Exception
   {
      return new PlanarSegmentationModule(ros2Node, messager, configurationFilePath);
   }

   private static File setupConfigurationFile(String configurationFilePath)
   {
      File configurationFile = new File(configurationFilePath);
      try
      {
         configurationFile.getParentFile().mkdirs();
         configurationFile.createNewFile();
      }
      catch (IOException e)
      {
         LogTools.info(configurationFile.getAbsolutePath());
         e.printStackTrace();
      }
      return configurationFile;
   }
}
