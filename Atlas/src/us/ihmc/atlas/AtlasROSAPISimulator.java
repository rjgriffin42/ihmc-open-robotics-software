package us.ihmc.atlas;

import java.io.IOException;
import java.net.URI;

import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.net.PacketCommunicator;
import us.ihmc.communication.packetCommunicator.KryoLocalPacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.darpaRoboticsChallenge.DRCSimulationStarter;
import us.ihmc.darpaRoboticsChallenge.DRCSimulationTools;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.gfe.ThePeoplesGloriousNetworkProcessor;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.time.SimulationRosClockPPSTimestampOffsetProvider;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

public class AtlasROSAPISimulator
{
   private static String defaultRosNameSpace = "atlas";
   private static String defaultRobotModel = "DRC_NO_HANDS";
   private final boolean startUI = true;
   
   public AtlasROSAPISimulator(DRCRobotModel robotModel, String nameSpace) throws IOException
   {
      PacketCommunicator controllerCommunicator = new KryoLocalPacketCommunicator(new IHMCCommunicationKryoNetClassList(), PacketDestination.BROADCAST.ordinal(), "AtlasROSAPISimulatorLocalCommunicator");

      DRCSimulationStarter simulationStarter = DRCSimulationTools.createObstacleCourseSimulationStarter(robotModel);
      simulationStarter.setControllerInputPacketCommunicator(controllerCommunicator);
      if (startUI)
         simulationStarter.setNetworkProcessorOutputPacketCommunicator(controllerCommunicator);
      boolean startNetworkProcessor = startUI;
      simulationStarter.startSimulation(startNetworkProcessor, true);

      URI rosUri = robotModel.getNetworkParameters().getRosURI();

      PacketCommunicator sensorCommunicator = simulationStarter.getSCSSensorOutputPacketCommunicator();
      SimulationRosClockPPSTimestampOffsetProvider ppsOffsetProvider = new SimulationRosClockPPSTimestampOffsetProvider();
      new ThePeoplesGloriousNetworkProcessor(rosUri, controllerCommunicator, sensorCommunicator, ppsOffsetProvider, robotModel, nameSpace);

      if (startUI)
         simulationStarter.startOpertorInterface();
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

      new AtlasROSAPISimulator(robotModel, config.getString("namespace"));
   }
}
