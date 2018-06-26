package us.ihmc.atlas.joystickBasedStepping;

import controller_msgs.msg.dds.BDIBehaviorCommandPacket;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.joystickBasedJavaFXController.JoystickBasedSteppingMainUI;
import us.ihmc.commonWalkingControlModules.configurations.SteppingParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.Ros2Node;

public class AtlasJoystickBasedSteppingApplication extends Application
{
   private JoystickBasedSteppingMainUI ui;
   private final Ros2Node ros2Node = ROS2Tools.createRos2Node(PubSubImplementation.FAST_RTPS, "ihmc_atlas_xbox_joystick_control");
   private IHMCROS2Publisher<BDIBehaviorCommandPacket> bdiBehaviorcommandPublisher;

   @Override
   public void start(Stage primaryStage) throws Exception
   {
      String robotTargetString = getParameters().getNamed().getOrDefault("robotTarget", "REAL_ROBOT");
      RobotTarget robotTarget = RobotTarget.valueOf(robotTargetString);
      PrintTools.info("-------------------------------------------------------------------");
      PrintTools.info("  -------- Loading parameters for RobotTarget: " + robotTarget + "  -------");
      PrintTools.info("-------------------------------------------------------------------");
      AtlasRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ, robotTarget, false);
      String robotName = robotModel.getSimpleRobotName();
      bdiBehaviorcommandPublisher = ROS2Tools.createPublisher(ros2Node, BDIBehaviorCommandPacket.class,
                                                              ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName));
      AtlasKickAndPunchMessenger atlasKickAndPunchMessenger = new AtlasKickAndPunchMessenger(ros2Node, robotName);

      WalkingControllerParameters walkingControllerParameters = robotModel.getWalkingControllerParameters();
      SteppingParameters steppingParameters = walkingControllerParameters.getSteppingParameters();
      double footLength = steppingParameters.getFootLength();
      double toeWidth = steppingParameters.getToeWidth();
      double footWidth = steppingParameters.getFootWidth();
      ConvexPolygon2D footPolygon = new ConvexPolygon2D();
      footPolygon.addVertex(footLength / 2.0, toeWidth / 2.0);
      footPolygon.addVertex(footLength / 2.0, -toeWidth / 2.0);
      footPolygon.addVertex(-footLength / 2.0, -footWidth / 2.0);
      footPolygon.addVertex(-footLength / 2.0, footWidth / 2.0);
      footPolygon.update();

      ui = new JoystickBasedSteppingMainUI(robotName, primaryStage, ros2Node, robotModel, robotModel.getWalkingControllerParameters(),
                                           atlasKickAndPunchMessenger, atlasKickAndPunchMessenger, atlasKickAndPunchMessenger,
                                           new SideDependentList<>(footPolygon, footPolygon));
   }

   @Override
   public void stop() throws Exception
   {
      super.stop();
      ui.stop();

      if (bdiBehaviorcommandPublisher != null)
         bdiBehaviorcommandPublisher.publish(HumanoidMessageTools.createBDIBehaviorCommandPacket(true));

      Platform.exit();
   }

   /**
    * 
    * @param args should either be {@code --robotTarget=SCS} or {@code --robotTarget=REAL_ROBOT}.
    */
   public static void main(String[] args)
   {
      launch(args);
   }
}
