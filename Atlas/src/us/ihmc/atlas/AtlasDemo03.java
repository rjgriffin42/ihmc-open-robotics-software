package us.ihmc.atlas;

import com.martiansoftware.jsap.JSAPException;

import us.ihmc.atlas.initialSetup.VRCTask1InVehicleHovering;
import us.ihmc.darpaRoboticsChallenge.DRCDemo03;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.simulationconstructionset.HumanoidFloatingRootJointRobot;

public class AtlasDemo03 extends DRCDemo03
{
   public AtlasDemo03(DRCGuiInitialSetup guiInitialSetup,
         DRCRobotModel robotModel, DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup)
   {
      super(guiInitialSetup, robotModel, robotInitialSetup);
   }

   public static void main(String[] args) throws JSAPException
   {
      DRCRobotModel defaultModelForGraphicSelector = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_INVISIBLE_CONTACTABLE_PLANE_HANDS, DRCRobotModel.RobotTarget.SCS, false);

      DRCRobotModel model = null;
      model = AtlasRobotModelFactory.selectSimulationModelFromFlag(args);
      
      if (model == null)
         model = AtlasRobotModelFactory.selectModelFromGraphicSelector(defaultModelForGraphicSelector);

      if (model == null)
          throw new RuntimeException("No robot model selected");


      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(false, false);

//      String ipAddress = null;
//      int portNumber = -1;
      
      DRCRobotInitialSetup<HumanoidFloatingRootJointRobot> robotInitialSetup = new VRCTask1InVehicleHovering(0.0); // new VRCTask1InVehicleInitialSetup(-0.03); // DrivingDRCRobotInitialSetup();
      new AtlasDemo03(guiInitialSetup, model, robotInitialSetup);
   }
}
