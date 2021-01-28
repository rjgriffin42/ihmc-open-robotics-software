package us.ihmc.valkyrie.roughTerrainWalking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.roughTerrainWalking.HumanoidEndToEndStairsTest;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.configuration.ValkyrieRobotVersion;

public class ValkyrieEndToEndStairsTest extends HumanoidEndToEndStairsTest
{
   @Override
   public DRCRobotModel getRobotModel()
   {
      return new ValkyrieRobotModel(RobotTarget.SCS, ValkyrieRobotVersion.FINGERLESS);
   }

   @Test
   public void testUpStairsSlow(TestInfo testInfo) throws Exception
   {
      testStairs(testInfo, true, true, 0.6, 0.25, 0.0);
   }

   @Test
   public void testDownStairsSlow(TestInfo testInfo) throws Exception
   {
      testStairs(testInfo, true, false, 0.9, 0.25, 0.0);
   }

   @Test
   public void testUpStairs(TestInfo testInfo) throws Exception
   {
      testStairs(testInfo, false, true, 0.9, 0.25, 0.0);
   }

   @Test
   public void testDownStairs(TestInfo testInfo) throws Exception
   {
      testStairs(testInfo, false, false, 1.0, 0.35, 0.0);
   }
}
