package us.ihmc.quadrupedRobotics.controller.force;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

import us.ihmc.quadrupedRobotics.QuadrupedForceTestYoVariables;
import us.ihmc.quadrupedRobotics.QuadrupedMultiRobotTestInterface;
import us.ihmc.quadrupedRobotics.QuadrupedTestBehaviors;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.QuadrupedTestGoals;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.input.managers.QuadrupedStepTeleopManager;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.robotics.testing.YoVariableTestGoal;
import us.ihmc.simulationConstructionSetTools.util.simulationrunner.GoalOrientedTestConductor;
import us.ihmc.tools.MemoryTools;

public abstract class QuadrupedXGaitFlatGroundWalkingTest implements QuadrupedMultiRobotTestInterface
{
   private GoalOrientedTestConductor conductor;
   private QuadrupedForceTestYoVariables variables;
   private QuadrupedStepTeleopManager stepTeleopManager;

   @Before
   public void setup()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");

      try
      {
         QuadrupedTestFactory quadrupedTestFactory = createQuadrupedTestFactory();
         quadrupedTestFactory.setControlMode(QuadrupedControlMode.FORCE);
         quadrupedTestFactory.setGroundContactModelType(QuadrupedGroundContactModelType.FLAT);
         quadrupedTestFactory.setUseNetworking(true);
         conductor = quadrupedTestFactory.createTestConductor();
         variables = new QuadrupedForceTestYoVariables(conductor.getScs());
         stepTeleopManager = quadrupedTestFactory.getStepTeleopManager();
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error loading simulation: " + e.getMessage());
      }
   }
   
   @After
   public void tearDown()
   {
      conductor.concludeTesting();
      conductor = null;
      variables = null;
      
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }
   
   public void testFlatGroundWalking(double walkingSpeed)
   {
      QuadrupedTestBehaviors.readyXGait(conductor, variables, stepTeleopManager);

      double walkTime = 5.0;
      stepTeleopManager.requestXGait();
      stepTeleopManager.getXGaitSettings().setEndPhaseShift(90.0);
      stepTeleopManager.setDesiredVelocity(walkingSpeed, 0.0, 0.0);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addTerminalGoal(YoVariableTestGoal.timeInFuture(variables.getYoTime(), walkTime));

      double finalPositionX = walkTime * walkingSpeed * 0.7;
      if(walkingSpeed > 0.0)
      {
         conductor.addTerminalGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), finalPositionX));
      }
      else
      {
         conductor.addTerminalGoal(YoVariableTestGoal.doubleLessThan(variables.getRobotBodyX(), finalPositionX));
      }

      conductor.simulate();
   }

   public void testWalkingInAForwardLeftCircle()
   {
      QuadrupedTestBehaviors.readyXGait(conductor, variables, stepTeleopManager);

      stepTeleopManager.requestXGait();
      stepTeleopManager.getXGaitSettings().setEndPhaseShift(90.0);
      stepTeleopManager.setDesiredVelocity(0.6, 0.0, 0.3);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), 15.0));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), 1.0));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI, 0.1));
      conductor.simulate();
   }
   
   public void testWalkingInAForwardRightCircle()
   {
      QuadrupedTestBehaviors.readyXGait(conductor, variables, stepTeleopManager);

      stepTeleopManager.requestXGait();
      stepTeleopManager.getXGaitSettings().setEndPhaseShift(90.0);
      stepTeleopManager.setDesiredVelocity(0.6, 0.0, -0.3);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), 15.0));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), 1.0));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI, 0.1));
      conductor.simulate();
   }
   
   public void testWalkingInABackwardLeftCircle()
   {
      QuadrupedTestBehaviors.readyXGait(conductor, variables, stepTeleopManager);

      stepTeleopManager.requestXGait();
      stepTeleopManager.getXGaitSettings().setEndPhaseShift(90.0);
      stepTeleopManager.setDesiredVelocity(-0.6, 0.0, -0.3);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), 15.0));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), -1.5));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), -Math.PI, 0.1));
      conductor.simulate();
   }
   
   public void testWalkingInABackwardRightCircle()
   {
      QuadrupedTestBehaviors.readyXGait(conductor, variables, stepTeleopManager);

      stepTeleopManager.requestXGait();
      stepTeleopManager.getXGaitSettings().setEndPhaseShift(90.0);
      stepTeleopManager.setDesiredVelocity(-0.6, 0.0, 0.3);
      conductor.addSustainGoal(QuadrupedTestGoals.notFallen(variables));
      conductor.addSustainGoal(YoVariableTestGoal.doubleLessThan(variables.getYoTime(), 15.0));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleGreaterThan(variables.getRobotBodyX(), -1.5));
      conductor.addWaypointGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI / 2, 0.1));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyX(), 0.0, 0.3));
      conductor.addTerminalGoal(YoVariableTestGoal.doubleWithinEpsilon(variables.getRobotBodyYaw(), Math.PI, 0.1));
      conductor.simulate();
   }
}
