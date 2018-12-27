package us.ihmc.quadrupedPlanning.networkProcessing;

import controller_msgs.msg.dds.*;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.communication.ROS2Tools.ROS2TopicQualifier;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.interfaces.Settable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static us.ihmc.communication.ROS2Tools.getTopicNameGenerator;

public class QuadrupedStepTeleopCommunicationProperties
{
   private static final List<Class<? extends Command<?, ?>>> toolboxSupportedCommands;
   private static final List<Class<? extends Settable<?>>> toolboxSupportedStatusMessages;

   static
   {
      List<Class<? extends Settable<?>>> statusMessages = new ArrayList<>();
      statusMessages.add(BodyPathPlanMessage.class);

      toolboxSupportedStatusMessages = Collections.unmodifiableList(statusMessages);

      List<Class<? extends Command<?, ?>>> commands = new ArrayList<>();

      toolboxSupportedCommands = Collections.unmodifiableList(commands);
   }

   public static List<Class<? extends Settable<?>>> getSupportedStatusMessages()
   {
      return toolboxSupportedStatusMessages;
   }

   public static List<Class<? extends Command<?, ?>>> getSupportedCommands()
   {
      return toolboxSupportedCommands;
   }

   public static MessageTopicNameGenerator publisherTopicNameGenerator(String robotName)
   {
      return getTopicNameGenerator(robotName, ROS2Tools.STEP_TELEOP_TOOLBOX, ROS2TopicQualifier.OUTPUT);
   }

   public static MessageTopicNameGenerator subscriberTopicNameGenerator(String robotName)
   {
      return getTopicNameGenerator(robotName, ROS2Tools.STEP_TELEOP_TOOLBOX, ROS2TopicQualifier.INPUT);
   }
}
