package us.ihmc.valkyrie.simulation;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.atlas.visualization.SliderBoardFactory;
import us.ihmc.atlas.visualization.WalkControllerSliderBoard;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTrack;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCRobotInterface;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCSCSInitialSetup;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.valkyrie.ValkyrieRobotInterface;
import us.ihmc.valkyrie.paramaters.ValkyrieRobotModel;

import com.yobotics.simulationconstructionset.util.FlatGroundProfile;

public class ValkyrieFlatGroundWalkingTrack
{

   public static void main(String[] args)
   {
      AutomaticSimulationRunner automaticSimulationRunner = null;

      DRCRobotModel robotModel = new ValkyrieRobotModel();
      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false, sliderBoardFactory);

      DRCRobotInterface robotInterface = new ValkyrieRobotInterface();
      
      
      final double groundHeight = 0.0;
      GroundProfile groundProfile = new FlatGroundProfile(groundHeight);

      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT());
      scsInitialSetup.setDrawGroundProfile(true);
      scsInitialSetup.setInitializeEstimatorToActual(true);
      
      double initialYaw = 0.0;
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = robotModel.getDefaultRobotInitialSetup(groundHeight, initialYaw);

      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;

      WalkingControllerParameters drcControlParameters = robotModel.getWalkingControlParamaters();
      ArmControllerParameters armControlParameters = robotModel.getArmControllerParameters();
      
      new DRCFlatGroundWalkingTrack(drcControlParameters, armControlParameters, robotInterface, robotInitialSetup, guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript,
                                    automaticSimulationRunner, DRCConfigParameters.CONTROL_DT, 16000, cheatWithGroundHeightAtForFootstep, robotModel);
   }

}
