package us.ihmc.atlas;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTrack;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCSCSInitialSetup;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.visualization.SliderBoardFactory;
import us.ihmc.darpaRoboticsChallenge.visualization.WalkControllerSliderBoard;
import us.ihmc.graphics3DAdapter.GroundProfile3D;

import com.martiansoftware.jsap.JSAPException;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.ground.BumpyGroundProfile;

public class AtlasFlatGroundWalkingTrack
{
   private static final DRCRobotModel defaultModelForGraphicSelector = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, false, false);
   
   private static final boolean USE_BUMPY_GROUND = false;
   
   public static void main(String[] args) throws JSAPException
   {
      
      DRCRobotModel model = null;
      model = AtlasRobotModelFactory.selectModelFromFlag(args, false, false);
      
      if (model == null)
         model = AtlasRobotModelFactory.selectModelFromGraphicSelector(defaultModelForGraphicSelector);

      if (model == null)
          throw new RuntimeException("No robot model selected");
      
      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false, sliderBoardFactory);
      
      
      final double groundHeight = 0.0;
      GroundProfile3D groundProfile;
      if (USE_BUMPY_GROUND)
      {
         groundProfile = createBumpyGroundProfile();
      }
      else
      {
         groundProfile = new FlatGroundProfile(groundHeight);
      }

      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, model.getSimulateDT());
      scsInitialSetup.setDrawGroundProfile(true);
      scsInitialSetup.setInitializeEstimatorToActual(true);
      
      double initialYaw = 0.3;
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = model.getDefaultRobotInitialSetup(groundHeight, initialYaw);

      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;

      new DRCFlatGroundWalkingTrack(robotInitialSetup, guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript,
                                    cheatWithGroundHeightAtForFootstep, model);
   }

   private static BumpyGroundProfile createBumpyGroundProfile()
   {
      double xAmp1 = 0.05, xFreq1 = 0.5, xAmp2 = 0.01, xFreq2 = 0.5;
      double yAmp1 = 0.01, yFreq1 = 0.07, yAmp2 = 0.05, yFreq2 = 0.37;
      BumpyGroundProfile groundProfile = new BumpyGroundProfile(xAmp1, xFreq1, xAmp2, xFreq2, yAmp1, yFreq1, yAmp2, yFreq2);
      return groundProfile;
   }
}
