package us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.uiConnector;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import org.ros.internal.message.Message;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;

import us.ihmc.communication.PacketRouter;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.interfaces.PacketCommunicator;
import us.ihmc.communication.packetCommunicator.interfaces.PacketServer;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.msgToPacket.IHMCRosApiMessageMap;
import us.ihmc.utilities.ros.publisher.IHMCPacketToMsgPublisher;
import us.ihmc.utilities.ros.publisher.RosTopicPublisher;

public class UiPacketToRosMsgRedirector implements PacketConsumer<Packet>
{
   private static final Map<String, Class> PACKETS_TO_REDIRECT_TO_ROS = IHMCRosApiMessageMap.INPUT_PACKET_MESSAGE_NAME_MAP;
   private static final String ROS_NAMESPACE = "/ihmc_msgs/atlas";
   
   private final RosMainNode rosMainNode;
   private final NodeConfiguration nodeConfiguration;
   private final MessageFactory messageFactory;
   private final ArrayList<RosTopicPublisher<?>> publishers;
   private final PacketServer packetCommunicator;


   public UiPacketToRosMsgRedirector(DRCRobotModel robotModel, URI rosCoreURI, PacketCommunicator packetCommunicator, PacketRouter packetRouter)
   {
      rosMainNode = new RosMainNode(rosCoreURI, ROS_NAMESPACE, true);
      this.packetCommunicator = packetCommunicator;
      this.nodeConfiguration = NodeConfiguration.newPrivate();
      this.messageFactory = nodeConfiguration.getTopicMessageFactory();
      this.publishers = new ArrayList<RosTopicPublisher<?>>();
      setupMsgTopics(packetCommunicator);
      rosMainNode.execute();
      packetCommunicator.attachListener(Packet.class, this);
      packetRouter.setPacketRedirects(PacketDestination.CONTROLLER.ordinal(), packetCommunicator.getId());
   }

   @Override
   public void receivedPacket(Packet packet)
   {
      if (!PACKETS_TO_REDIRECT_TO_ROS.containsKey(packet.getClass()))
      {
         packet.setDestination(PacketDestination.CONTROLLER.ordinal());
         packetCommunicator.send(packet);
      }
   }

   private void setupMsgTopics(PacketCommunicator packetCommunicator)
   {
      Map<String, Class> outputPacketList = PACKETS_TO_REDIRECT_TO_ROS;

      for (Map.Entry<String, Class> e : outputPacketList.entrySet())
      {
         Message message = messageFactory.newFromType(e.getKey());

         IHMCPacketToMsgPublisher<Message, Packet> publisher = IHMCPacketToMsgPublisher.createIHMCPacketToMsgPublisher(message, false, packetCommunicator,
               e.getValue());
         publishers.add(publisher);
         rosMainNode.attachPublisher(ROS_NAMESPACE + IHMCRosApiMessageMap.PACKET_TO_TOPIC_MAP.get(e.getValue()), publisher);
      }
   }
}
