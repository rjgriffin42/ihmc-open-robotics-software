package us.ihmc.avatar.networkProcessor.quadTreeHeightMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.avatar.networkProcessor.modules.ToolboxModule;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.StatusPacket;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.humanoidRobotics.communication.packets.heightQuadTree.HeightQuadTreeMessage;
import us.ihmc.humanoidRobotics.communication.packets.sensing.PointCloudWorldPacket;
import us.ihmc.humanoidRobotics.communication.toolbox.heightQuadTree.command.HeightQuadTreeToolboxRequestCommand;
import us.ihmc.humanoidRobotics.communication.toolbox.heightQuadTree.command.PointCloud3DCommand;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotModels.FullHumanoidRobotModel;

public class HeightQuadTreeToolboxModule extends ToolboxModule
{
   private static final PacketDestination PACKET_DESTINATION = PacketDestination.HEIGHT_QUADTREE_TOOLBOX_MODULE;
   private static final NetworkPorts NETWORK_PORT = NetworkPorts.HEIGHT_QUADTREE_TOOLBOX_MODULE_PORT;

   private final HeightQuadTreeToolboxController controller;

   public HeightQuadTreeToolboxModule(FullHumanoidRobotModel desiredFullRobotModel, LogModelProvider modelProvider) throws IOException
   {
      super(desiredFullRobotModel, modelProvider, false, PACKET_DESTINATION, NETWORK_PORT);

      controller = new HeightQuadTreeToolboxController(commandInputManager, statusOutputManager, registry);
      setTimeWithoutInputsBeforeGoingToSleep(3.0);
   }

   @Override
   public ToolboxController getToolboxController()
   {
      return controller;
   }

   @Override
   public List<Class<? extends Command<?, ?>>> createListOfSupportedCommands()
   {
      List<Class<? extends Command<?, ?>>> commands = new ArrayList<>();
      commands.add(HeightQuadTreeToolboxRequestCommand.class);
      commands.add(PointCloud3DCommand.class);
      return commands;
   }

   @Override
   public List<Class<? extends StatusPacket<?>>> createListOfSupportedStatus()
   {
      List<Class<? extends StatusPacket<?>>> status = new ArrayList<>();
      status.add(HeightQuadTreeMessage.class);
      return status;
   }

   @Override
   public Set<Class<? extends Packet<?>>> filterExceptions()
   {
      Set<Class<? extends Packet<?>>> filterExceptions = new HashSet<>();
      filterExceptions.add(PointCloudWorldPacket.class);
      return filterExceptions;
   }
}
