package us.ihmc.valkyrie.obstacleCourse;

import org.junit.Test;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseEveryBuildTest;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;
import us.ihmc.valkyrie.ValkyrieRobotModel;

@DeployableTestClass(targets = TestPlanTarget.Fast)
public class ValkyrieObstacleCourseEveryBuildTest extends DRCObstacleCourseEveryBuildTest
{
   private final DRCRobotModel robotModel = new ValkyrieRobotModel(DRCRobotModel.RobotTarget.SCS, false);
   
   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.VALKYRIE);
   }
   
   @Override
   @DeployableTestMethod(estimatedDuration = 72.0)
   @Test(timeout = 360000)
   public void testSimpleFlatGroundScript() throws SimulationExceededMaximumTimeException
   {
      super.testSimpleFlatGroundScript();
   }
   
   @Override
   @DeployableTestMethod(estimatedDuration = 53.4)
   @Test(timeout = 270000)
   public void testWalkingUpToRampWithLongSteps() throws SimulationExceededMaximumTimeException
   {
      super.testWalkingUpToRampWithLongSteps();
   }
}
