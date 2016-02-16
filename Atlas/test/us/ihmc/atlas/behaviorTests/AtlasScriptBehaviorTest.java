package us.ihmc.atlas.behaviorTests;

import java.io.FileNotFoundException;
import org.junit.Test;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.behaviorTests.DRCScriptBehaviorTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;

@DeployableTestClass(targets = {TestPlanTarget.InDevelopment})
public class AtlasScriptBehaviorTest extends DRCScriptBehaviorTest
{
   private final AtlasRobotModel robotModel;

   public AtlasScriptBehaviorTest()
   {
      robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_DUAL_ROBOTIQ, DRCRobotModel.RobotTarget.SCS, false);
   }

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
   @DeployableTestMethod(estimatedDuration = 39.3, targets = TestPlanTarget.Flaky)
   @Test(timeout = 200000)
   public void testPauseAndResumeScript() throws FileNotFoundException, SimulationExceededMaximumTimeException
   {
      super.testPauseAndResumeScript();
   }

   @Override
   @DeployableTestMethod(estimatedDuration = 26.4, targets = TestPlanTarget.Flaky)
   @Test(timeout = 130000)
   public void testScriptWithOneHandPosePacket() throws FileNotFoundException, SimulationExceededMaximumTimeException
   {
      super.testScriptWithOneHandPosePacket();
   }

   @Override
   @DeployableTestMethod(estimatedDuration = 54.0)
   @Test(timeout = 270000)
   public void testScriptWithTwoComHeightScriptPackets() throws FileNotFoundException, SimulationExceededMaximumTimeException
   {
      super.testScriptWithTwoComHeightScriptPackets();
   }

   @Override
   @DeployableTestMethod(estimatedDuration = 36.1, targets = TestPlanTarget.Flaky)
   @Test(timeout = 180000)
   public void testScriptWithTwoHandPosePackets() throws FileNotFoundException, SimulationExceededMaximumTimeException
   {
      super.testScriptWithTwoHandPosePackets();
   }

   @Override
   @DeployableTestMethod(estimatedDuration = 36.4)
   @Test(timeout = 180000)
   public void testSimpleScript() throws FileNotFoundException, SimulationExceededMaximumTimeException
   {
      super.testSimpleScript();
   }

   @Override
   @DeployableTestMethod(estimatedDuration = 53.9)
   @Test(timeout = 270000)
   public void testStopScript() throws FileNotFoundException, SimulationExceededMaximumTimeException
   {
      super.testStopScript();
   }
}
