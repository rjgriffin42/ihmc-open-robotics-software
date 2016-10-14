package us.ihmc.llaQuadruped.controller.force;

import java.io.IOException;

import org.junit.Test;

import us.ihmc.llaQuadruped.LLAQuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedXGaitTurning720Test;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerFailureException;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;

@ContinuousIntegrationPlan(categories = IntegrationCategory.IN_DEVELOPMENT)
public class LLAQuadrupedTurning720Test extends QuadrupedXGaitTurning720Test
{
   @Override
   public QuadrupedTestFactory createQuadrupedTestFactory()
   {
      return new LLAQuadrupedTestFactory();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 80.0)
   @Test(timeout = 200000)
   public void rotate720InPlaceRight() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      super.rotate720InPlaceRight();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 80.0)
   @Test(timeout = 200000)
   public void rotate720InPlaceLeft() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      super.rotate720InPlaceLeft();
   }
}
