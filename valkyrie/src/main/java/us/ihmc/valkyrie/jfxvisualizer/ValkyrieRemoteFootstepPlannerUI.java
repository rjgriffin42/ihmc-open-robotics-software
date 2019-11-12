package us.ihmc.valkyrie.jfxvisualizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.footstepPlanning.MultiStageFootstepPlanningModule;
import us.ihmc.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.footstepPlanning.ui.FootstepPlannerUI;
import us.ihmc.footstepPlanning.ui.RemoteUIMessageConverter;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.pubsub.DomainFactory;
import us.ihmc.valkyrie.ValkyrieNetworkProcessor;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.configuration.ValkyrieRobotVersion;
import us.ihmc.valkyrie.parameters.ValkyrieFootstepPostProcessorParameters;

/**
 * This class provides a visualizer for the remote footstep planner found in the footstep planner toolbox.
 * It allows users to view the resulting plans calculated by the toolbox. It also allows the user to tune
 * the planner parameters, and request a new plan from the planning toolbox.
 */
public class ValkyrieRemoteFootstepPlannerUI extends Application
{
   private SharedMemoryJavaFXMessager messager;
   private RemoteUIMessageConverter messageConverter;

   private FootstepPlannerUI ui;

   @Override
   public void start(Stage primaryStage) throws Exception
   {
      DRCRobotModel model = new ValkyrieRobotModel(RobotTarget.REAL_ROBOT, false);
      DRCRobotModel previewModel = new ValkyrieRobotModel(RobotTarget.REAL_ROBOT, ValkyrieRobotVersion.DEFAULT, null, null, false, true, 0.0);

      messager = new SharedMemoryJavaFXMessager(FootstepPlannerMessagerAPI.API);
      messageConverter = RemoteUIMessageConverter.createConverter(messager, model.getSimpleRobotName(), DomainFactory.PubSubImplementation.FAST_RTPS);

      messager.startMessager();

      ui = FootstepPlannerUI.createMessagerUI(primaryStage, messager, model.getFootstepPlannerParameters(), model.getVisibilityGraphsParameters(),
                                              new ValkyrieFootstepPostProcessorParameters(), model, previewModel, model.getContactPointParameters(),
                                              model.getWalkingControllerParameters());
      ui.show();

      if(!ValkyrieNetworkProcessor.launchFootstepPlannerModule)
      {
         new MultiStageFootstepPlanningModule(model, model.getLogModelProvider(), false);
      }
   }

   @Override
   public void stop() throws Exception
   {
      super.stop();

      messager.closeMessager();
      messageConverter.destroy();
      ui.stop();

      Platform.exit();
   }

   public static void main(String[] args)
   {
      launch(args);
   }
}
