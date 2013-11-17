package us.ihmc.darpaRoboticsChallenge;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.atlas.visualization.SliderBoardFactory;
import us.ihmc.atlas.visualization.WalkControllerSliderBoard;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.controllers.ControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.FlatGroundWalkingHighLevelHumanoidControllerFactory;
import us.ihmc.darpaRoboticsChallenge.controllers.DRCRobotMomentumBasedControllerFactory;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.drcRobot.SimulatedModelCorruptorDRCRobotInterface;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCSimDRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.remote.RemoteAtlasVisualizer;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;
import us.ihmc.robotDataCommunication.YoVariableServer;
import us.ihmc.utilities.Pair;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.ground.BumpyGroundProfile;

public class DRCFlatGroundWalkingTrack
{
   private static final boolean USE_BUMPY_GROUND = false;
   
   private static final boolean START_YOVARIABLE_SERVER = true; 
   
   private final HumanoidRobotSimulation<SDFRobot> drcSimulation;
   private final DRCController drcController;
   private final YoVariableServer robotVisualizer;

   public DRCFlatGroundWalkingTrack(DRCRobotWalkingControllerParameters drcControlParameters, ArmControllerParameters armControllerParameters, 
         DRCRobotInterface robotInterface,
                                    RobotInitialSetup<SDFRobot> robotInitialSetup, DRCGuiInitialSetup guiInitialSetup, DRCSCSInitialSetup scsInitialSetup,
                                    boolean useVelocityAndHeadingScript, AutomaticSimulationRunner automaticSimulationRunner, double timePerRecordTick,
                                    int simulationDataBufferSize, boolean cheatWithGroundHeightAtForFootstep)
   {
//    scsInitialSetup = new DRCSCSInitialSetup(TerrainType.FLAT);
      scsInitialSetup.setSimulationDataBufferSize(simulationDataBufferSize);

      double dt = scsInitialSetup.getDT();
      int recordFrequency = (int) Math.round(timePerRecordTick / dt);
      if (recordFrequency < 1)
         recordFrequency = 1;
      scsInitialSetup.setRecordFrequency(recordFrequency);

      DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry;
      if (guiInitialSetup.isGuiShown())
         dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry(false);
      else
         dynamicGraphicObjectsListRegistry = null;
      YoVariableRegistry registry = new YoVariableRegistry("adjustableParabolicTrajectoryDemoSimRegistry");

      boolean useFastTouchdowns = false;

      FlatGroundWalkingHighLevelHumanoidControllerFactory highLevelHumanoidControllerFactory =
         new FlatGroundWalkingHighLevelHumanoidControllerFactory(drcControlParameters, armControllerParameters, useVelocityAndHeadingScript, useFastTouchdowns);

      if (cheatWithGroundHeightAtForFootstep)
      {
         highLevelHumanoidControllerFactory.setupForCheatingUsingGroundHeightAtForFootstepProvider(scsInitialSetup.getGroundProfile());
      }

      if (START_YOVARIABLE_SERVER)
      {
         robotVisualizer = new YoVariableServer(robotInterface.getRobot().getRobotsYoVariableRegistry(), RemoteAtlasVisualizer.defaultPort, DRCConfigParameters.ATLAS_INTERFACING_DT, dynamicGraphicObjectsListRegistry);
      }
      else
      {
         robotVisualizer = null;
      }
      
      ControllerFactory controllerFactory = new DRCRobotMomentumBasedControllerFactory(highLevelHumanoidControllerFactory);
      Pair<HumanoidRobotSimulation<SDFRobot>, DRCController> humanoidSimulation = DRCSimulationFactory.createSimulation(controllerFactory, null, robotInterface, robotInitialSetup, scsInitialSetup, guiInitialSetup, null, robotVisualizer, dynamicGraphicObjectsListRegistry);
      drcSimulation = humanoidSimulation.first();
      drcController = humanoidSimulation.second();

      // add other registries
      drcSimulation.addAdditionalYoVariableRegistriesToSCS(registry);

      
      
      if (automaticSimulationRunner != null)
      {
         drcSimulation.start(automaticSimulationRunner);
      }
      else
      {
         drcSimulation.start(null);
      }
      
      if (robotVisualizer != null)
      {
         robotVisualizer.start();
      }
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return drcSimulation.getSimulationConstructionSet();
   }

   public DRCController getDrcController()
   {
      return drcController;
   }

   public YoVariableServer getRobotVisualizer()
   {
      return robotVisualizer;
   }

   public static void main(String[] args) throws JSAPException
   {
      AutomaticSimulationRunner automaticSimulationRunner = null;

      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false, sliderBoardFactory);

//    DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(TerrainType.FLAT_Z_NEGATIVE_TWO);
//    RobotInitialSetup<SDFRobot> robotInitialSetup = new SquaredUpDRCRobotInitialSetup(-2.0);
      DRCRobotInterface robotInterface = new PlainDRCRobot(DRCRobotModel.getDefaultRobotModel(), false);
      
      if (DRCConfigParameters.CORRUPT_SIMULATION_MODEL)
      {
         robotInterface = new SimulatedModelCorruptorDRCRobotInterface(robotInterface);
      }
      
      final double groundHeight = 0.0;
      GroundProfile groundProfile;
      if (USE_BUMPY_GROUND)
      {
         groundProfile = createBumpyGroundProfile();
      }
      else
      {
         groundProfile = new FlatGroundProfile(groundHeight);
      }

      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT());
      scsInitialSetup.setDrawGroundProfile(true);
      scsInitialSetup.setInitializeEstimatorToActual(true);
      
      double initialYaw = 0.3;
      RobotInitialSetup<SDFRobot> robotInitialSetup = new DRCSimDRCRobotInitialSetup(groundHeight, initialYaw);

      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;

      DRCRobotWalkingControllerParameters drcControlParameters = new DRCRobotWalkingControllerParameters();
      DRCRobotArmControllerParameters armControlParameters = new DRCRobotArmControllerParameters();
      
      new DRCFlatGroundWalkingTrack(drcControlParameters, armControlParameters, robotInterface, robotInitialSetup, guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript,
                                    automaticSimulationRunner, DRCConfigParameters.CONTROL_DT, 16000, cheatWithGroundHeightAtForFootstep);
   }

   private static BumpyGroundProfile createBumpyGroundProfile()
   {
      double xAmp1 = 0.05, xFreq1 = 0.5, xAmp2 = 0.01, xFreq2 = 0.5;
      double yAmp1 = 0.01, yFreq1 = 0.07, yAmp2 = 0.05, yFreq2 = 0.37;
      BumpyGroundProfile groundProfile = new BumpyGroundProfile(xAmp1, xFreq1, xAmp2, xFreq2, yAmp1, yFreq1, yAmp2, yFreq2);
      return groundProfile;
   }


}
