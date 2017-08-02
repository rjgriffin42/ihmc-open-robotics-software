package us.ihmc.avatar.networkProcessor.rrtToolboxModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.avatar.networkProcessor.modules.ToolboxModule;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.StatusPacket;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ConstrainedWholeBodyPlanningRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ConstrainedWholeBodyPlanningToolboxOutputStatus;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotModels.FullHumanoidRobotModel;

public class ConstrainedWholeBodyPlanningToolboxModule extends ToolboxModule
{
   private static final PacketDestination PACKET_DESTINATION = PacketDestination.CONSTRAINED_WHOLE_BODY_PLANNING_TOOLBOX_MODULE;
   private static final NetworkPorts NETWORK_PORT = NetworkPorts.CONSTRAINED_WHOLE_BODY_PLANNING_TOOLBOX_MODULE_PORT;

   private final ConstrainedWholeBodyPlanningToolboxController constrainedWholeBodyPlanningToolboxController;

   public ConstrainedWholeBodyPlanningToolboxModule(DRCRobotModel drcRobotModel, FullHumanoidRobotModel fullHumanoidRobotModel, LogModelProvider modelProvider,
                                                    boolean startYoVariableServer)
         throws IOException
   {
      super(drcRobotModel.createFullRobotModel(), drcRobotModel.getLogModelProvider(), startYoVariableServer, PACKET_DESTINATION, NETWORK_PORT);

      setTimeWithoutInputsBeforeGoingToSleep(Double.POSITIVE_INFINITY);

      constrainedWholeBodyPlanningToolboxController = new ConstrainedWholeBodyPlanningToolboxController(fullRobotModel, statusOutputManager, registry, yoGraphicsListRegistry, startYoVariableServer);
      packetCommunicator.attachListener(ConstrainedWholeBodyPlanningRequestPacket.class, constrainedWholeBodyPlanningToolboxController.createRequestConsumer());
      startYoVariableServer();
   }

   @Override
   public ToolboxController getToolboxController()
   {
      return constrainedWholeBodyPlanningToolboxController;
   }

   @Override
   public List<Class<? extends Command<?, ?>>> createListOfSupportedCommands()
   {
      List<Class<? extends Command<?, ?>>> commands = new ArrayList<>();
      return commands;
   }

   @Override
   public List<Class<? extends StatusPacket<?>>> createListOfSupportedStatus()
   {
      List<Class<? extends StatusPacket<?>>> status = new ArrayList<>();
      status.add(ConstrainedWholeBodyPlanningToolboxOutputStatus.class);
      return status;
   }

}
