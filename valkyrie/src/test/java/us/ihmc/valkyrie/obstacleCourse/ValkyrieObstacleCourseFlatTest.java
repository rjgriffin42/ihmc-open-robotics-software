package us.ihmc.valkyrie.obstacleCourse;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.obstacleCourseTests.DRCObstacleCourseFlatTest;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.valkyrie.ValkyrieRobotModel;

public class ValkyrieObstacleCourseFlatTest extends DRCObstacleCourseFlatTest
{
   private final DRCRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.SCS, false);

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

   /**
    * Doesn't work with Valkyrie yet. Need to get it working some day.
    */
   @Override
   @Disabled
   @Test
   public void testWalkingUpToRampWithLongStepsAndOccasionallyStraightKnees() throws SimulationExceededMaximumTimeException
   {
      super.testWalkingUpToRampWithLongStepsAndOccasionallyStraightKnees();
   }

   @Override
   @Test
   public void testRotatedStepInTheAir() throws SimulationExceededMaximumTimeException
   {
      super.testRotatedStepInTheAir();
   }

   @Test
   public void testSimpleScripts() throws SimulationExceededMaximumTimeException, IOException
   {
      super.testSimpleScripts();
   }

   @Test
   public void testACoupleStepsUsingQueuedControllerCommands() throws SimulationExceededMaximumTimeException
   {
      super.testACoupleStepsUsingQueuedControllerCommands();
   }

   @Test
   public void testACoupleQueuedControllerCommands() throws SimulationExceededMaximumTimeException
   {
      super.testACoupleQueuedControllerCommands();
   }

   @Test
   public void testACoupleMoreQueuedControllerCommands() throws SimulationExceededMaximumTimeException
   {
      super.testACoupleMoreQueuedControllerCommands();
   }

   @Override
   @Test
   public void testWalkingUpToRampWithShortSteps() throws SimulationExceededMaximumTimeException
   {
      super.testWalkingUpToRampWithShortSteps();
   }

   @Override
   @Test
   public void testStandingTooHighToCheckIfSingularityStuffIsWorkingProperly() throws SimulationExceededMaximumTimeException
   {
      super.testStandingTooHighToCheckIfSingularityStuffIsWorkingProperly();
   }

   @Override
   @Test
   public void testStandingForACoupleSeconds() throws SimulationExceededMaximumTimeException
   {
      super.testStandingForACoupleSeconds();
   }

   @Override
   @Test
   public void testLongStepsMaxHeightPauseAndResume() throws SimulationExceededMaximumTimeException
   {
      super.testLongStepsMaxHeightPauseAndResume();
   }

   @Override
   @Test
   public void testTurningInPlaceAndPassingPI() throws SimulationExceededMaximumTimeException
   {
      super.testTurningInPlaceAndPassingPI();
   }

   @Override
   @Test
   public void testStandingOnUnevenTerrainForACoupleSeconds() throws SimulationExceededMaximumTimeException
   {
      super.testStandingOnUnevenTerrainForACoupleSeconds();
   }

   @Override
   @Disabled
   @Test
   public void testForMemoryLeaks() throws Exception
   {
      super.testForMemoryLeaks();
   }
}
