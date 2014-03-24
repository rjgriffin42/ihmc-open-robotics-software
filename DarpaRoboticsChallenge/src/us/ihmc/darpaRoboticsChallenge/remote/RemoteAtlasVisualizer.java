package us.ihmc.darpaRoboticsChallenge.remote;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.atlas.visualization.SliderBoardControllerListener;
import us.ihmc.atlas.visualization.SliderBoardFactory;
import us.ihmc.atlas.visualization.WalkControllerSliderBoard;
import us.ihmc.darpaRoboticsChallenge.DRCLocalConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModelFactory;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.robotDataCommunication.YoVariableClient;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

public class RemoteAtlasVisualizer
{
   public static final String defaultHost = DRCLocalConfigParameters.ROBOT_CONTROLLER_IP_ADDRESS;
   public static final int defaultPort = 5555;
   private final boolean showOverheadView = DRCLocalConfigParameters.SHOW_OVERHEAD_VIEW;
   
   public RemoteAtlasVisualizer(String host, int port, int bufferSize, DRCRobotModel robotModel)
   {
      System.out.println("Connecting to host " + host);
      
      DRCRobotJointMap jointMap = robotModel.getJointMap(false, false);
      JaxbSDFLoader robotLoader = DRCRobotSDFLoader.loadDRCRobot(jointMap);
//      SDFRobot robot = robotLoader.createRobot(jointMap, false);
//      SliderBoardFactory sliderBoardFactory = GainControllerSliderBoard.getFactory();
      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
//      SliderBoardFactory sliderBoardFactory = PositionControllerSliderBoard.getFactory();
//      SliderBoardFactory sliderBoardFactory = JointAngleOffsetSliderBoard.getFactory();

      SliderBoardControllerListener scsYoVariablesUpdatedListener = new SliderBoardControllerListener(robotLoader, jointMap, bufferSize, sliderBoardFactory);
      scsYoVariablesUpdatedListener.addButton("requestStop", 1.0);
      
      
      YoVariableClient client = new YoVariableClient(host, port, scsYoVariablesUpdatedListener, "remote", showOverheadView);
      client.start();
   }

   public static void main(String[] args) throws JSAPException
   {
      int bufferSize = 16384;
      JSAP jsap = new JSAP();
      
      FlaggedOption hostOption = new FlaggedOption("host").setStringParser(JSAP.STRING_PARSER).setRequired(false).setLongFlag("host").setShortFlag('L').setDefault(
            defaultHost);
      FlaggedOption portOption = new FlaggedOption("port").setStringParser(JSAP.INTEGER_PARSER).setRequired(false).setLongFlag("port").setShortFlag('p')
            .setDefault(String.valueOf(defaultPort));
      FlaggedOption robotModel = new FlaggedOption("robotModel").setLongFlag("model").setShortFlag('m').setRequired(true).setStringParser(JSAP.STRING_PARSER);
      robotModel.setHelp("Robot models: " + DRCRobotModelFactory.robotModelsToString());
      
      jsap.registerParameter(hostOption);
      jsap.registerParameter(portOption);
      jsap.registerParameter(robotModel);
      
      JSAPResult config = jsap.parse(args);
      
      if (config.success())
      {
         String host = config.getString("host");
         int port = config.getInt("port");
         DRCRobotModel model = DRCRobotModelFactory.CreateDRCRobotModel(config.getString("robotModel"));
         
         new RemoteAtlasVisualizer(host, port, bufferSize, model);         
      }
      else
      {
         System.err.println();
         System.err.println("Usage: java " + RemoteAtlasVisualizer.class.getName());
         System.err.println("                " + jsap.getUsage());
         System.err.println();
         System.exit(1);
      }
      
      
      
   }
}
