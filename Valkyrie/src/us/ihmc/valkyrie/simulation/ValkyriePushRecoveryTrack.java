package us.ihmc.valkyrie.simulation;

import javax.vecmath.Vector3d;

import com.martiansoftware.jsap.JSAPException;

import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.humanoidRobotics.HumanoidFloatingRootJointRobot;
import us.ihmc.simulationconstructionset.FloatingRootJointRobot;
import us.ihmc.commonWalkingControlModules.pushRecovery.PushRobotController;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTrack;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCSCSInitialSetup;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.validation.YoVariableThreadAccessValidator;
import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.ground.FlatGroundProfile;
import us.ihmc.valkyrie.ValkyrieRobotModel;

public class ValkyriePushRecoveryTrack
{
   private final static boolean VISUALIZE_FORCE = true;
   
   public static void main(String[] args) throws JSAPException
   {
      DRCRobotModel model = new ValkyrieRobotModel(DRCRobotModel.RobotTarget.SCS, false);
      final double groundHeight = 0.0;

      GroundProfile3D groundProfile = new FlatGroundProfile(groundHeight);

      YoVariableThreadAccessValidator.registerAccessValidator();
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false);

      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, model.getSimulateDT());
      scsInitialSetup.setDrawGroundProfile(true);
      scsInitialSetup.setInitializeEstimatorToActual(true);

      double initialYaw = 0.3;
      DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup = model.getDefaultRobotInitialSetup(groundHeight, initialYaw);

      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;

      DRCFlatGroundWalkingTrack track = new DRCFlatGroundWalkingTrack(robotInitialSetup, guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript,
            cheatWithGroundHeightAtForFootstep, model);

      FloatingRootJointRobot robot = track.getDrcSimulation().getRobot();
      FullHumanoidRobotModel fullRobotModel = model.createFullRobotModel();
      PushRobotController pushRobotController = new PushRobotController(robot, fullRobotModel);

      pushRobotController.addPushButtonToSCS(track.getSimulationConstructionSet());
      
      double defaultForceDurationInSeconds = 0.15;
      double defaultForceMagnitude = 350.0;
      Vector3d defaultForceDirection = new Vector3d(1.0, 0.0, 0.0);
      
      SimulationConstructionSet scs = track.getSimulationConstructionSet();
      
      BooleanYoVariable enable = (BooleanYoVariable) scs.getVariable("enablePushRecovery");
      // enable push recovery
      enable.set(true);
      
      if(VISUALIZE_FORCE)
      {
         scs.addYoGraphic(pushRobotController.getForceVisualizer());
      }
      
      pushRobotController.setPushDuration(defaultForceDurationInSeconds);
      pushRobotController.setPushForceMagnitude(defaultForceMagnitude);
      pushRobotController.setPushForceDirection(defaultForceDirection);
   }
}
