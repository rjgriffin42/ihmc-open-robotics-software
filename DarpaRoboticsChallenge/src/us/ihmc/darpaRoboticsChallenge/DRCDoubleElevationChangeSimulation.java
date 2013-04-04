package us.ihmc.darpaRoboticsChallenge;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;

import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.SquaredUpDRCRobotInitialSetup;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.util.DoubleStepGroundProfile;

public class DRCDoubleElevationChangeSimulation
{   
   public static void main(String[] args) throws JSAPException
   {
      final double stepHeight = 0.05;
      final boolean stepUp = false;
      
      AutomaticSimulationRunner automaticSimulationRunner = null;
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false);

      DRCRobotInterface robotInterface = new PlainDRCRobot(DRCRobotModel.getDefaultRobotModel());
      final double groundHeight = 0.0;
      double elevationChange = 0.0;
      if(stepUp) elevationChange = Math.abs(stepHeight);
      else elevationChange = - Math.abs(stepHeight);
      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(new DoubleStepGroundProfile(-2.0, 2.0, 0.53, 0.9, elevationChange, 0.0), robotInterface.getSimulateDT());    //(new FlatGroundProfile(groundHeight), robotInterface.getSimulateDT());
      RobotInitialSetup<SDFRobot> robotInitialSetup = new SquaredUpDRCRobotInitialSetup(groundHeight);

      boolean useVelocityAndHeadingScript = false;
      boolean cheatWithGroundHeightAtForFootstep = false;
      boolean doChestOrientationControl = false;

      DRCRobotWalkingControllerParameters drcControlParameters = new DRCRobotWalkingControllerParameters();
      
      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(drcControlParameters, robotInterface, 
            robotInitialSetup, guiInitialSetup, scsInitialSetup, 
            useVelocityAndHeadingScript, automaticSimulationRunner, 0.005, 16000, 
            doChestOrientationControl, cheatWithGroundHeightAtForFootstep);
      
      ((BooleanYoVariable) drcFlatGroundWalkingTrack.getSimulationConstructionSet().getVariable("walk")).set(true);
      ((DoubleYoVariable) drcFlatGroundWalkingTrack.getSimulationConstructionSet().getVariable("desiredVelocityX")).set(1.0);
   }

   
}
