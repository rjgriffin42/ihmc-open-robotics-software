package us.ihmc.darpaRoboticsChallenge.gfe;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.ros.internal.message.Message;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.communication.producers.RobotPoseBuffer;
import us.ihmc.communication.subscribers.RobotDataReceiver;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.ros.RosRobotJointStatePublisher;
import us.ihmc.darpaRoboticsChallenge.ros.RosRobotPosePublisher;
import us.ihmc.darpaRoboticsChallenge.ros.RosSCSCameraPublisher;
import us.ihmc.darpaRoboticsChallenge.ros.RosSCSLidarPublisher;
import us.ihmc.darpaRoboticsChallenge.ros.RosTfPublisher;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.utilities.humanoidRobot.frames.ReferenceFrames;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.net.AtomicSettableTimestampProvider;
import us.ihmc.utilities.net.ComparableDataObject;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.ros.PPSTimestampOffsetProvider;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.msgToPacket.IHMCMessageMap;
import us.ihmc.utilities.ros.publisher.IHMCPacketToMsgPublisher;
import us.ihmc.utilities.ros.publisher.RosTopicPublisher;
import us.ihmc.utilities.ros.subscriber.AbstractRosTopicSubscriber;
import us.ihmc.utilities.ros.subscriber.IHMCMsgToPacketSubscriber;
import us.ihmc.utilities.ros.subscriber.TimestampedPoseFootStepGenerator;

public class ThePeoplesGloriousNetworkProcessor
{
   private static final String nodeName = "/Controller";

   private final AtomicSettableTimestampProvider timestampProvider = new AtomicSettableTimestampProvider();
   private final PPSTimestampOffsetProvider ppsTimestampOffsetProvider;
   private final RosMainNode rosMainNode;
   private final DRCRobotModel robotModel;
   private final ObjectCommunicator controllerCommunicationBridge;
   private final ObjectCommunicator scsSensorCommunicationBridge;

   private final ArrayList<AbstractRosTopicSubscriber<?>> subscribers;
   private final ArrayList<RosTopicPublisher<?>> publishers;

   private final NodeConfiguration nodeConfiguration;
   private final MessageFactory messageFactory;
   private final FullRobotModel fullRobotModel;

   public ThePeoplesGloriousNetworkProcessor(URI rosUri, ObjectCommunicator controllerCommunicationBridge, ObjectCommunicator scsSensorCommunicationBridge, PPSTimestampOffsetProvider ppsOffsetProvider, 
                                             DRCRobotModel robotModel, String namespace) throws IOException
   {
      this.rosMainNode = new RosMainNode(rosUri, namespace + nodeName);
      this.robotModel = robotModel;
      this.controllerCommunicationBridge = controllerCommunicationBridge;
      this.scsSensorCommunicationBridge = scsSensorCommunicationBridge;
      this.ppsTimestampOffsetProvider = ppsOffsetProvider;
      this.ppsTimestampOffsetProvider.attachToRosMainNode(rosMainNode);
      this.subscribers = new ArrayList<AbstractRosTopicSubscriber<?>>();
      this.publishers = new ArrayList<RosTopicPublisher<?>>();

      this.nodeConfiguration = NodeConfiguration.newPrivate();
      this.messageFactory = nodeConfiguration.getTopicMessageFactory();
      this.fullRobotModel = robotModel.createFullRobotModel();
      RobotDataReceiver robotDataReceiver = new RobotDataReceiver(fullRobotModel, null, true);
      ReferenceFrames referenceFrames = robotDataReceiver.getReferenceFrames();
      controllerCommunicationBridge.attachListener(RobotConfigurationData.class, robotDataReceiver);

      setupInputs(namespace + "/inputs", referenceFrames, fullRobotModel);
      setupOutputs(namespace + "/outputs");
      rosMainNode.execute();

      controllerCommunicationBridge.connect();
   }

   public ThePeoplesGloriousNetworkProcessor(URI rosUri, ObjectCommunicator controllerCommunicationBridge, DRCRobotModel robotModel, String namespace) throws
         IOException
   {
      this(rosUri, controllerCommunicationBridge, null, robotModel.getPPSTimestampOffsetProvider(), robotModel, namespace);
   }

   private void setupOutputs(String namespace)
   {
      SDFFullRobotModel fullRobotModel = robotModel.createFullRobotModel();
      new RobotDataReceiver(fullRobotModel, null, true);
      DRCRobotSensorInformation sensorInformation = robotModel.getSensorInformation();
      RobotPoseBuffer robotPoseBuffer = new RobotPoseBuffer(controllerCommunicationBridge, 1000, timestampProvider);

      RosTfPublisher tfPublisher = new RosTfPublisher(rosMainNode);

      new RosRobotPosePublisher(controllerCommunicationBridge, rosMainNode, ppsTimestampOffsetProvider, robotPoseBuffer, sensorInformation, namespace,
            tfPublisher);
      new RosRobotJointStatePublisher(controllerCommunicationBridge, rosMainNode, ppsTimestampOffsetProvider, namespace);

      if(scsSensorCommunicationBridge != null)
      {
         publishSimulatedCameraAndLidar(fullRobotModel, sensorInformation, tfPublisher);
      }

      Map<String, Class> inputPacketList = IHMCMessageMap.OUTPUT_PACKET_MESSAGE_NAME_MAP;

      for (Map.Entry<String, Class> e : inputPacketList.entrySet())
      {
         Message message = messageFactory.newFromType(e.getKey());

         IHMCPacketToMsgPublisher<Message, ComparableDataObject> publisher = IHMCPacketToMsgPublisher.CreateIHMCPacketToMsgPublisher(message, false,
               controllerCommunicationBridge, e.getValue());
         publishers.add(publisher);
         rosMainNode.attachPublisher(namespace + "/" + e.getKey(), publisher);
      }
   }

   private void publishSimulatedCameraAndLidar(SDFFullRobotModel fullRobotModel, DRCRobotSensorInformation sensorInformation, RosTfPublisher tfPublisher)
   {
      if (sensorInformation.getCameraParameters().length > 0)
      {
         new RosSCSCameraPublisher(scsSensorCommunicationBridge, rosMainNode, ppsTimestampOffsetProvider, sensorInformation.getCameraParameters());
      }

      if (sensorInformation.getLidarParameters().length > 0)
      {
         new RosSCSLidarPublisher(scsSensorCommunicationBridge, rosMainNode, ppsTimestampOffsetProvider, fullRobotModel,
               sensorInformation.getLidarParameters(), tfPublisher);
      }
   }

   private void setupInputs(String namespace, ReferenceFrames referenceFrames, FullRobotModel fullRobotModel)
   {
      Map<String, Class> inputPacketList = IHMCMessageMap.INPUT_PACKET_MESSAGE_NAME_MAP;

      for (Map.Entry<String, Class> e : inputPacketList.entrySet())
      {
         Message message = messageFactory.newFromType(e.getKey());
         IHMCMsgToPacketSubscriber<Message> subscriber = IHMCMsgToPacketSubscriber.CreateIHMCMsgToPacketSubscriber(message,
               controllerCommunicationBridge);
         subscribers.add(subscriber);
         rosMainNode.attachSubscriber(namespace + "/" + e.getKey(), subscriber);
      }
      
      TimestampedPoseFootStepGenerator footPoseGenerator = new TimestampedPoseFootStepGenerator(referenceFrames, fullRobotModel, controllerCommunicationBridge);
      rosMainNode.attachSubscriber(namespace + "/TimestampedPoseFootStepGenerator", footPoseGenerator);
   }
}
