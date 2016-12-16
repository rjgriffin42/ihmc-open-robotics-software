package us.ihmc.atlas.roughTerrainWalking;

import org.junit.Test;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.roughTerrainWalking.AvatarSwingWithWaypointsTest;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.FAST})
public class AtlasSwingWithWaypointsTest extends AvatarSwingWithWaypointsTest
{
   @Override
   @ContinuousIntegrationTest(estimatedDuration = 30.0)
   @Test(timeout = 500000)
   public void testRegularSwingWithWaypoints() throws SimulationExceededMaximumTimeException
   {
      super.testRegularSwingWithWaypoints();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 30.0)
   @Test(timeout = 500000)
   public void testSwingWithWaypointsRotated() throws SimulationExceededMaximumTimeException
   {
      super.testSwingWithWaypointsRotated();
   }

   @Override
   public DRCRobotModel getRobotModel()
   {
      return new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, DRCRobotModel.RobotTarget.SCS, false);
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }

}
