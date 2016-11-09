package us.ihmc.footstepPlanning.flatGroundPlanning;

import org.junit.Test;

import us.ihmc.footstepPlanning.FootstepPlanner;
import us.ihmc.footstepPlanning.simplePlanners.TurnWalkTurnPlanner;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;

@ContinuousIntegrationAnnotations.ContinuousIntegrationPlan(categories = IntegrationCategory.IN_DEVELOPMENT)
public class TurnWalkTurnPlannerTest extends FootstepPlannerOnFlatGroundTest
{
   private static final boolean visualize = true;
   private final TurnWalkTurnPlanner planner = new TurnWalkTurnPlanner();

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testJustStraightLine()
   {
      super.testJustStraightLine();
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testStraightLineWithInitialTurn()
   {
      super.testStraightLineWithInitialTurn();
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testJustTurnInPlace()
   {
      super.testJustTurnInPlace();
   }

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 0.1)
   @Test(timeout = 300000)
   public void testRandomPoses()
   {
      super.testRandomPoses();
   }

   @Override
   public FootstepPlanner getPlanner()
   {
      return planner;
   }

   @Override
   public boolean visualize()
   {
      return visualize;
   }
}
