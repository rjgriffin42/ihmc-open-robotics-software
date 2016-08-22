package us.ihmc.quadrupedRobotics.controller.position;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.quadrupedRobotics.QuadrupedMultiRobotTestInterface;
import us.ihmc.quadrupedRobotics.QuadrupedPositionTestYoVariables;
import us.ihmc.quadrupedRobotics.QuadrupedTestBehaviors;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.QuadrupedTestGoals;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.robotics.testing.YoVariableTestGoal;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerFailureException;
import us.ihmc.simulationconstructionset.util.simulationRunner.GoalOrientedTestConductor;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public abstract class QuadrupedPositionCrawlTurning360Test implements QuadrupedMultiRobotTestInterface
{
   private GoalOrientedTestConductor conductor;
   private QuadrupedPositionTestYoVariables variables;
   
   @Before
   public void setup()
   {
      try
      {
         MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

         QuadrupedTestFactory quadrupedTestFactory = createQuadrupedTestFactory();
         quadrupedTestFactory.setControlMode(QuadrupedControlMode.POSITION);
         quadrupedTestFactory.setGroundContactModelType(QuadrupedGroundContactModelType.FLAT);
         conductor = quadrupedTestFactory.createTestConductor();
         variables = new QuadrupedPositionTestYoVariables(conductor.getScs());
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error loading simulation: " + e.getMessage());
      }
   }
   
   @After
   public void tearDown()
   {
      conductor = null;
      variables = null;
      
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @DeployableTestMethod(estimatedDuration = 120.0)
   @Test(timeout = 800000)
   public void rotate360InPlaceRight() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      
      variables.getYoPlanarVelocityInputZ().set(-0.7);
      
      int numSpins = 1;
      for (int i = 0; i < numSpins; i++)
      {
         conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
         conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), variables.getYoTime().getDoubleValue() + 50.0));
//         conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 4.0, 1e-2));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 2.0, 1e-2));
         conductor.simulate();
//
         conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
         conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), variables.getYoTime().getDoubleValue() + 50.0));
//         conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 4.0, 1e-2));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), 0.0, 1e-2));
         conductor.simulate();
      }
      
      conductor.concludeTesting();
   }
   
   @DeployableTestMethod(estimatedDuration = 120.0)
   @Test(timeout = 800000)
   public void rotate360InPlaceLeft() throws SimulationExceededMaximumTimeException, ControllerFailureException, IOException
   {
      QuadrupedTestBehaviors.standUp(conductor, variables);
      
      variables.getYoPlanarVelocityInputZ().set(0.5);
      
      int numSpins = 1;
      for (int i = 0; i < numSpins; i++)
      {
         conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
         conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), variables.getYoTime().getDoubleValue() + 10.0));
//         conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 4.0, 1e-2));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 2.0, 1e-2));
         conductor.simulate();

         conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
         conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), variables.getYoTime().getDoubleValue() + 10.0));
//         conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 4.0, 1e-2));
         conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), 0.0, 1e-2));
         conductor.simulate();
      }
      
      conductor.concludeTesting();
   }
}
