package us.ihmc.atlas;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.darpaRoboticsChallenge.DRCLocalConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;

public class AtlasSDFViewer
{
   public static void main(String[] args)
   {
     DRCRobotModel selectedModel = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT);
      DRCRobotJointMap jointMap = selectedModel.getJointMap();
      JaxbSDFLoader loader = DRCRobotSDFLoader.loadDRCRobot(jointMap, false);
      System.out.println(loader.createRobot(jointMap, true).getName());
      
      SimulationConstructionSet scs = new SimulationConstructionSet(loader.createRobot(jointMap, false));
      scs.startOnAThread();

   }
}
