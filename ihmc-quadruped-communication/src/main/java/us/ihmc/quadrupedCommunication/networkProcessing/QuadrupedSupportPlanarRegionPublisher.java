package us.ihmc.quadrupedCommunication.networkProcessing;

import controller_msgs.msg.dds.BipedalSupportPlanarRegionParametersMessage;
import controller_msgs.msg.dds.PlanarRegionsListMessage;
import controller_msgs.msg.dds.QuadrupedSupportPlanarRegionParametersMessage;
import controller_msgs.msg.dds.RobotConfigurationData;
import gnu.trove.list.array.TFloatArrayList;
import org.ejml.data.DenseMatrix64F;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.ROS2TopicQualifier;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.mecano.multiBodySystem.interfaces.FloatingJointBasics;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.quadrupedBasics.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.robotEnvironmentAwareness.communication.REACommunicationProperties;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.robotModels.FullQuadrupedRobotModelFactory;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.geometry.PlanarRegion;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.ros2.NewMessageListener;
import us.ihmc.ros2.RealtimeRos2Node;
import us.ihmc.sensorProcessing.frames.CommonQuadrupedReferenceFrames;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class QuadrupedSupportPlanarRegionPublisher
{
   private static final double defaultRegionSize = 0.3;

   private static final int FRONT_LEFT_FOOT_INDEX = 0;
   private static final int FRONT_RIGHT_FOOT_INDEX = 1;
   private static final int HIND_LEFT_FOOT_INDEX = 2;
   private static final int HIND_RIGHT_FOOT_INDEX = 3;
   private static final int CONVEX_HULL_INDEX = 4;

   private final RealtimeRos2Node ros2Node;
   private final IHMCRealtimeROS2Publisher<PlanarRegionsListMessage> regionPublisher;

   private final AtomicReference<RobotConfigurationData> latestRobotConfigurationData = new AtomicReference<>(null);
   private final AtomicReference<QuadrupedSupportPlanarRegionParametersMessage> latestParametersMessage = new AtomicReference<>(null);

   private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(ThreadTools.getNamedThreadFactory(getClass().getSimpleName()));
   private ScheduledFuture<?> task;

   private final FullQuadrupedRobotModel fullRobotModel;
   private final OneDoFJointBasics[] oneDoFJoints;
   private final CommonQuadrupedReferenceFrames referenceFrames;
   private final QuadrantDependentList<ContactablePlaneBody> contactableFeet;
   private final List<PlanarRegion> supportRegions = new ArrayList<>();

   public QuadrupedSupportPlanarRegionPublisher(FullQuadrupedRobotModelFactory robotModel,
                                                QuadrantDependentList<ArrayList<Point2D>> groundContactPoints, PubSubImplementation pubSubImplementation)
   {
      fullRobotModel = robotModel.createFullRobotModel();
      oneDoFJoints = fullRobotModel.getOneDoFJoints();
      referenceFrames = new QuadrupedReferenceFrames(fullRobotModel);
      String robotName = robotModel.getRobotDescription().getName();
      ContactableBodiesFactory<RobotQuadrant> contactableBodiesFactory = new ContactableBodiesFactory<>();
      contactableBodiesFactory.setFullRobotModel(fullRobotModel);
      contactableBodiesFactory.setReferenceFrames(referenceFrames);
      contactableBodiesFactory.setFootContactPoints(groundContactPoints);
      contactableFeet = new QuadrantDependentList<>(contactableBodiesFactory.createFootContactablePlaneBodies());

      ros2Node = ROS2Tools.createRealtimeRos2Node(pubSubImplementation, "supporting_planar_region_publisher");

      ROS2Tools.createCallbackSubscription(ros2Node, RobotConfigurationData.class, ControllerAPIDefinition.getPublisherTopicNameGenerator(robotName),
                                           (NewMessageListener<RobotConfigurationData>) subscriber -> latestRobotConfigurationData.set(subscriber.takeNextData()));
      regionPublisher = ROS2Tools.createPublisher(ros2Node, PlanarRegionsListMessage.class,
                                                  REACommunicationProperties.subscriberCustomRegionsTopicNameGenerator);
      ROS2Tools.createCallbackSubscription(ros2Node, QuadrupedSupportPlanarRegionParametersMessage.class,
                                           ROS2Tools.getTopicNameGenerator(robotName, ROS2Tools.QUADRUPED_SUPPORT_REGION_PUBLISHER, ROS2TopicQualifier.INPUT), s -> latestParametersMessage.set(s.takeNextData()));

      QuadrupedSupportPlanarRegionParametersMessage defaultParameters = new QuadrupedSupportPlanarRegionParametersMessage();
      defaultParameters.setEnable(true);
      defaultParameters.setSupportRegionSize(defaultRegionSize);
      latestParametersMessage.set(defaultParameters);

      for (int i = 0; i < 3; i++)
      {
         supportRegions.add(new PlanarRegion());
      }
   }

   public void start()
   {
      ros2Node.spin();
      task = executorService.scheduleWithFixedDelay(this::run, 0, 1, TimeUnit.SECONDS);
   }

   public void close()
   {
      ros2Node.destroy();
      task.cancel(true);
   }

   private void run()
   {
      QuadrupedSupportPlanarRegionParametersMessage parameters = latestParametersMessage.get();
      if (!parameters.getEnable() || parameters.getSupportRegionSize() <= 0.0)
      {
         supportRegions.set(FRONT_LEFT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(FRONT_RIGHT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(HIND_LEFT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(HIND_RIGHT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(CONVEX_HULL_INDEX, new PlanarRegion());

         publishRegions();
         return;
      }

      double supportRegionSize = parameters.getSupportRegionSize();

      RobotConfigurationData robotConfigurationData = latestRobotConfigurationData.get();
      if (robotConfigurationData == null)
         return;

      setRobotStateFromRobotConfigurationData(robotConfigurationData, fullRobotModel.getRootJoint(), oneDoFJoints);

      referenceFrames.updateFrames();

      QuadrantDependentList<Boolean> isInSupport = new QuadrantDependentList<>(true, true, true, true);
      if (feetAreInSamePlane(isInSupport))
      {
         ReferenceFrame leftSoleFrame = contactableFeet.get(RobotQuadrant.FRONT_LEFT).getSoleFrame();

         List<FramePoint2D> allContactPoints = new ArrayList<>();
         allContactPoints.addAll(createConvexPolygonPoints(contactableFeet.get(RobotQuadrant.FRONT_LEFT).getSoleFrame(), supportRegionSize));
         allContactPoints.addAll(createConvexPolygonPoints(contactableFeet.get(RobotQuadrant.FRONT_RIGHT).getSoleFrame(), supportRegionSize));
         allContactPoints.addAll(createConvexPolygonPoints(contactableFeet.get(RobotQuadrant.HIND_RIGHT).getSoleFrame(), supportRegionSize));
         allContactPoints.addAll(createConvexPolygonPoints(contactableFeet.get(RobotQuadrant.HIND_LEFT).getSoleFrame(), supportRegionSize));
         allContactPoints.forEach(p -> p.changeFrameAndProjectToXYPlane(leftSoleFrame));

         supportRegions.set(FRONT_LEFT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(FRONT_RIGHT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(HIND_LEFT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(HIND_RIGHT_FOOT_INDEX, new PlanarRegion());
         supportRegions.set(CONVEX_HULL_INDEX, new PlanarRegion(leftSoleFrame.getTransformToWorldFrame(),
                                                new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(allContactPoints))));
      }
      else
      {
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            if (isInSupport.get(robotQuadrant))
            {
               ContactablePlaneBody contactableFoot = contactableFeet.get(robotQuadrant);
               List<FramePoint2D> contactPoints = createConvexPolygonPoints(contactableFoot.getSoleFrame(), supportRegionSize);
               RigidBodyTransform transformToWorld = contactableFoot.getSoleFrame().getTransformToWorldFrame();
               supportRegions.set(robotQuadrant.ordinal(),
                                  new PlanarRegion(transformToWorld, new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(contactPoints))));
            }
            else
            {
               supportRegions.set(robotQuadrant.ordinal(), new PlanarRegion());
            }
         }

         supportRegions.set(CONVEX_HULL_INDEX, new PlanarRegion());
      }

      publishRegions();
   }

   private void publishRegions()
   {
      for (int i = 0; i < 3; i++)
      {
         supportRegions.get(i).setRegionId(i);
      }

      regionPublisher.publish(PlanarRegionMessageConverter.convertToPlanarRegionsListMessage(new PlanarRegionsList(supportRegions)));
   }

   private boolean feetAreInSamePlane(QuadrantDependentList<Boolean> isInSupport)
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         if (!isInSupport.get(robotQuadrant))
         {
            return false;
         }
      }
      ReferenceFrame frontLeftSoleFrame = contactableFeet.get(RobotQuadrant.FRONT_LEFT).getSoleFrame();
      ReferenceFrame frontRightSoleFrame = contactableFeet.get(RobotQuadrant.FRONT_RIGHT).getSoleFrame();
      ReferenceFrame hindLeftSoleFrame = contactableFeet.get(RobotQuadrant.HIND_LEFT).getSoleFrame();
      ReferenceFrame hindRightSoleFrame = contactableFeet.get(RobotQuadrant.HIND_RIGHT).getSoleFrame();

      RigidBodyTransform frontLeftToHindLeft = frontLeftSoleFrame.getTransformToDesiredFrame(hindLeftSoleFrame);
      RigidBodyTransform frontLeftToHindRight = frontLeftSoleFrame.getTransformToDesiredFrame(hindRightSoleFrame);
      RigidBodyTransform frontLeftToFrontRight = frontLeftSoleFrame.getTransformToDesiredFrame(frontRightSoleFrame);

      double translationEpsilon = 0.02;
      return Math.abs(frontLeftToHindLeft.getTranslationZ()) < translationEpsilon && Math.abs(frontLeftToHindRight.getTranslationZ()) < translationEpsilon &&
            Math.abs(frontLeftToFrontRight.getTranslationZ()) < translationEpsilon;
   }

   private static List<FramePoint2D> createConvexPolygonPoints(ReferenceFrame referenceFrame, double size)
   {
      List<FramePoint2D> points = new ArrayList<>();
      points.add(new FramePoint2D(referenceFrame, size, size));
      points.add(new FramePoint2D(referenceFrame, -size, size));
      points.add(new FramePoint2D(referenceFrame, size, -size));
      points.add(new FramePoint2D(referenceFrame, -size, -size));

      return points;
   }

   public static void setRobotStateFromRobotConfigurationData(RobotConfigurationData robotConfigurationData, FloatingJointBasics desiredRootJoint,
                                                              OneDoFJointBasics[] oneDoFJoints)
   {
      TFloatArrayList newJointAngles = robotConfigurationData.getJointAngles();

      for (int i = 0; i < newJointAngles.size(); i++)
      {
         oneDoFJoints[i].setQ(newJointAngles.get(i));
         oneDoFJoints[i].setQd(0.0);
      }

      if (desiredRootJoint != null)
      {
         Vector3D translation = robotConfigurationData.getRootTranslation();
         desiredRootJoint.getJointPose().setPosition(translation.getX(), translation.getY(), translation.getZ());
         Quaternion orientation = robotConfigurationData.getRootOrientation();
         desiredRootJoint.getJointPose().getOrientation().setQuaternion(orientation.getX(), orientation.getY(), orientation.getZ(), orientation.getS());
         desiredRootJoint.setJointVelocity(0, new DenseMatrix64F(6, 1));

         desiredRootJoint.getPredecessor().updateFramesRecursively();
      }
   }

   public void stop()
   {
      task.cancel(false);
   }

   public void destroy()
   {
      stop();
      executorService.shutdownNow();
      ros2Node.destroy();
   }
}
