package us.ihmc.valkyrie;

import java.io.IOException;
import java.net.URI;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

import us.ihmc.communication.configuration.NetworkParameterKeys;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel.RobotTarget;
import us.ihmc.darpaRoboticsChallenge.gfe.ThePeoplesGloriousNetworkProcessor;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkProcessor;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.uiConnector.UiPacketToRosMsgRedirector;
import us.ihmc.humanoidRobotics.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.utilities.ros.RosMainNode;

public class ValkyrieROSNetworkProcessor
{
   private static final String DEFAULT_TF_PREFIX = null;
   private static String defaultRosNameSpace = "/ihmc_ros/valkyrie";
   private static String nodeName = "/robot_data";

   private static final boolean ENABLE_UI_PACKET_TO_ROS_CONVERTER = true;

   public ValkyrieROSNetworkProcessor(String nameSpace, String tfPrefix) throws IOException
   {
      ValkyrieRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.REAL_ROBOT, true);
      PacketCommunicator gfeCommunicator = null;
      URI rosUri = NetworkParameters.getROSURI();

      if (ENABLE_UI_PACKET_TO_ROS_CONVERTER)
      {
         gfeCommunicator = PacketCommunicator.createIntraprocessPacketCommunicator(NetworkPorts.GFE_COMMUNICATOR, new IHMCCommunicationKryoNetClassList());

         DRCNetworkModuleParameters networkProcessorParameters = new DRCNetworkModuleParameters();
         networkProcessorParameters.enableLocalControllerCommunicator(false);
         networkProcessorParameters.enableUiModule(true);
         networkProcessorParameters.enableGFECommunicator(true);
         networkProcessorParameters.enableControllerCommunicator(true);
         DRCNetworkProcessor networkProcessor = new DRCNetworkProcessor(robotModel, networkProcessorParameters);
         new UiPacketToRosMsgRedirector(robotModel, rosUri, gfeCommunicator, networkProcessor.getPacketRouter(), defaultRosNameSpace);
      }
      else
      {
         String kryoIP = NetworkParameters.getHost(NetworkParameterKeys.robotController);
         gfeCommunicator = PacketCommunicator.createTCPPacketCommunicatorClient(kryoIP, NetworkPorts.CONTROLLER_PORT, new IHMCCommunicationKryoNetClassList());
      }

      RosMainNode rosMainNode = new RosMainNode(rosUri, nameSpace + nodeName);
      rosMainNode.execute();

      new ThePeoplesGloriousNetworkProcessor(rosUri, gfeCommunicator, robotModel, nameSpace, tfPrefix);
   }

   public static void main(String[] args) throws JSAPException, IOException
   {
      JSAP jsap = new JSAP();

      FlaggedOption rosNameSpace = new FlaggedOption("namespace").setLongFlag("namespace").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false)
            .setStringParser(JSAP.STRING_PARSER);
      rosNameSpace.setDefault(defaultRosNameSpace);

      FlaggedOption tfPrefix = new FlaggedOption("tfPrefix").setLongFlag("tfPrefix").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false)
            .setStringParser(JSAP.STRING_PARSER);
      tfPrefix.setDefault(DEFAULT_TF_PREFIX);

      jsap.registerParameter(tfPrefix);
      jsap.registerParameter(rosNameSpace);
      JSAPResult config = jsap.parse(args);

      String tfPrefixArg = config.getString("tfPrefix");
      String nodeNameSpacePrefix = config.getString("namespace");
      new ValkyrieROSNetworkProcessor(nodeNameSpacePrefix, tfPrefixArg);
   }
}
