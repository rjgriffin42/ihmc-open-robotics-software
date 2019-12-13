package us.ihmc.valkyrie;

import java.net.URI;
import java.net.URISyntaxException;

import com.martiansoftware.jsap.JSAPException;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.avatar.networkProcessor.DRCNetworkProcessor;
import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.valkyrie.configuration.ValkyrieRobotVersion;
import us.ihmc.valkyrie.planner.ValkyrieAStarFootstepPlanner;
import us.ihmc.valkyrieRosControl.ValkyrieRosControlController;

public class ValkyrieNetworkProcessor
{
   private static final ValkyrieRobotModel model = new ValkyrieRobotModel(RobotTarget.REAL_ROBOT);
   public static final boolean launchFootstepPlannerModule = true;
   
   public static void main(String[] args) throws URISyntaxException, JSAPException
   {
      DRCNetworkModuleParameters networkModuleParams = new DRCNetworkModuleParameters();
      
      networkModuleParams.enableRobotEnvironmentAwerenessModule(false);
      networkModuleParams.enableKinematicsToolbox(true);
      networkModuleParams.enableKinematicsStreamingToolbox(true, ValkyrieKinematicsStreamingToolboxModule.class);
      networkModuleParams.enableKinematicsPlanningToolbox(true);
      networkModuleParams.enableFootstepPlanningToolbox(launchFootstepPlannerModule);
      networkModuleParams.enableFootstepPlanningToolboxVisualizer(false);
      networkModuleParams.enableBipedalSupportPlanarRegionPublisher(true);
      networkModuleParams.enableWalkingPreviewToolbox(true);
      networkModuleParams.enableAutoREAStateUpdater(true);

//      uncomment these for the sensors
      URI rosuri = NetworkParameters.getROSURI();

      if(rosuri != null)
      {
         networkModuleParams.enableRosModule(true);
         networkModuleParams.setRosUri(rosuri);
         networkModuleParams.enableSensorModule(true);
         System.out.println("ROS_MASTER_URI="+rosuri);
      }

      new ValkyrieAStarFootstepPlanner(model).setupWithRos();

      new DRCNetworkProcessor(model, networkModuleParams, PubSubImplementation.FAST_RTPS);
   }
}
