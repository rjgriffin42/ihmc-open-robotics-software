package us.ihmc.atlas;

import java.io.IOException;
import java.net.URI;

import us.ihmc.communication.configuration.NetworkParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkProcessor;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

public class AtlasMultisenseMocapNetworkProcessor
{
   private static String defaultRosNameSpace = "/ihmc_ros/atlas";
   private static String defaultRobotModel = "ATLAS_UNPLUGGED_V5_NO_HANDS";

   public AtlasMultisenseMocapNetworkProcessor(DRCRobotModel robotModel, String nameSpace) throws IOException
   {
      URI rosUri = NetworkParameters.getROSURI();
      
      DRCNetworkModuleParameters params = new DRCNetworkModuleParameters();
      params.setRosUri(rosUri);
      params.setUseRosModule(true);
      params.setUseMocapModule(true);
      params.setUseSensorModule(true);
      params.setUseUiModule(true);
      
      new DRCNetworkProcessor(robotModel, params );
   }
   
   public static void main(String[] args) throws JSAPException, IOException
   {
      JSAP jsap = new JSAP();
      
      FlaggedOption rosNameSpace = new FlaggedOption("namespace").setLongFlag("namespace").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false)
            .setStringParser(JSAP.STRING_PARSER);
      rosNameSpace.setDefault(defaultRosNameSpace);

      FlaggedOption model = new FlaggedOption("robotModel").setLongFlag("model").setShortFlag('m').setRequired(false).setStringParser(JSAP.STRING_PARSER);
      model.setHelp("Robot models: " + AtlasRobotModelFactory.robotModelsToString());
      model.setDefault(defaultRobotModel);
      
      jsap.registerParameter(model);
      jsap.registerParameter(rosNameSpace);
      JSAPResult config = jsap.parse(args);

      DRCRobotModel robotModel;

      try
      {
         robotModel = AtlasRobotModelFactory.createDRCRobotModel(config.getString("robotModel"), AtlasRobotModel.AtlasTarget.SIM, false);
      }
      catch (IllegalArgumentException e)
      {
         System.err.println("Incorrect robot model " + config.getString("robotModel"));
         System.out.println(jsap.getHelp());
         return;
      }

      new AtlasMultisenseMocapNetworkProcessor(robotModel, config.getString("namespace"));
   }
}
