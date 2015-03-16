package us.ihmc.atlas;

import us.ihmc.atlas.AtlasRobotModel.AtlasTarget;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.wholeBodyInverseKinematicsSimulationController.WholeBodyVisualizier;

public class AtlasWholeBodyVisualizer
{
   public static void main(String[] args)
   {
      DRCRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ, AtlasTarget.SIM, false);
      WholeBodyVisualizier wholeBodyVisualizier = new WholeBodyVisualizier(robotModel);
   }
}
