package us.ihmc.valkyrie;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.networkProcessor.kinemtaticsStreamingToolboxModule.KinematicsStreamingToolboxModule;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotDataLogger.logger.DataServerSettings;
import us.ihmc.valkyrieRosControl.ValkyrieRosControlController;

public class ValkyrieKinematicsStreamingToolboxModule extends KinematicsStreamingToolboxModule
{
   public ValkyrieKinematicsStreamingToolboxModule(DRCRobotModel robotModel, boolean startYoVariableServer, PubSubImplementation pubSubImplementation)
   {
      super(robotModel, startYoVariableServer, pubSubImplementation);
   }

   @Override
   public DataServerSettings getYoVariableServerSettings()
   {
      return new DataServerSettings(true);
   }

   public static void main(String[] args)
   {
      ValkyrieRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.REAL_ROBOT, ValkyrieRosControlController.VERSION);
      boolean startYoVariableServer = true;
      PubSubImplementation pubSubImplementation = PubSubImplementation.FAST_RTPS;
      ValkyrieKinematicsStreamingToolboxModule module = new ValkyrieKinematicsStreamingToolboxModule(robotModel, startYoVariableServer, pubSubImplementation);

      Runtime.getRuntime().addShutdownHook(new Thread(() ->
      {
         System.out.println("Shutting down " + ValkyrieKinematicsStreamingToolboxModule.class.getSimpleName());
         module.closeAndDispose();
      }));
   }
}
