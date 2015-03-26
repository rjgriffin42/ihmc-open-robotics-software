package us.ihmc.atlas.packets.wholebody;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.packets.WholeBodyTrajectoryPacketTest;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;

public class AtlasWholeBodyTrajectoryPacketTest extends WholeBodyTrajectoryPacketTest
{
   private final AtlasRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ, AtlasRobotModel.AtlasTarget.SIM, false);

   @Override
   public DRCRobotModel getRobotModel() {
         return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }
}
