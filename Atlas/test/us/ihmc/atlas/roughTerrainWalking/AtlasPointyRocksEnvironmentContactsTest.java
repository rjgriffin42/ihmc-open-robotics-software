package us.ihmc.atlas.roughTerrainWalking;

import org.junit.Test;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.roughTerrainWalking.HumanoidPointyRocksEnvironmentContactsTest;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;

@DeployableTestClass(targets = {TestPlanTarget.Slow})
public class AtlasPointyRocksEnvironmentContactsTest extends HumanoidPointyRocksEnvironmentContactsTest
{
   private final AtlasRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, DRCRobotModel.RobotTarget.SCS, false);

   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }

   @Override
   protected DRCRobotModel getRobotModel(int xContactPoints, int yContactPoints, boolean createOnlyEdgePoints)
   {
      robotModel.addMoreFootContactPointsSimOnly(16, 8, createOnlyEdgePoints);
      return robotModel;
   }
   
   @Override
   @DeployableTestMethod(estimatedDuration = 137.5, targets = {TestPlanTarget.Slow, TestPlanTarget.Video})
   @Test(timeout = 690000)
   public void testWalkingOnLinesInEnvironment() throws SimulationExceededMaximumTimeException
   {
      super.testWalkingOnLinesInEnvironment();
   }
   
   @Override
   @DeployableTestMethod(estimatedDuration = 69.7, targets = {TestPlanTarget.Slow, TestPlanTarget.Video})
   @Test(timeout = 350000)
   public void testWalkingOnPointInEnvironment() throws SimulationExceededMaximumTimeException
   {
      super.testWalkingOnPointInEnvironment();
   }
}
