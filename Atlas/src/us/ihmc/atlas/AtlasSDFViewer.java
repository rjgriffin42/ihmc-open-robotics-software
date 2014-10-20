package us.ihmc.atlas;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;

public class AtlasSDFViewer
{
   public static void main(String[] args)
   {
      DRCRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, AtlasRobotModel.AtlasTarget.SIM, false);
      SimulationConstructionSet scs = new SimulationConstructionSet(robotModel.createSdfRobot(false));
      scs.startOnAThread();
   }
}
