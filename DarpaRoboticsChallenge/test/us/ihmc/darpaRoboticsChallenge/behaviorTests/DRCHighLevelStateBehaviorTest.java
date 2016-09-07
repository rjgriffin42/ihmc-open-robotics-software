package us.ihmc.darpaRoboticsChallenge.behaviorTests;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCBehaviorTestHelper;
import us.ihmc.humanoidBehaviors.behaviors.primitives.HighLevelStateBehavior;
import us.ihmc.humanoidRobotics.communication.packets.HighLevelStateMessage;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelState;
import us.ihmc.robotics.time.GlobalTimer;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.thread.ThreadTools;

public abstract class DRCHighLevelStateBehaviorTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());

      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcBehaviorTestHelper != null)
      {
         drcBehaviorTestHelper.closeAndDispose();
         drcBehaviorTestHelper = null;
      }

      GlobalTimer.clearTimers();

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   @AfterClass
   public static void printMemoryUsageAfterClass()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(DRCHighLevelStateBehaviorTest.class + " after class.");
   }

   private DRCBehaviorTestHelper drcBehaviorTestHelper;

   @Before
   public void setUp()
   {
      DRCDemo01NavigationEnvironment testEnvironment = new DRCDemo01NavigationEnvironment();

      drcBehaviorTestHelper = new DRCBehaviorTestHelper(testEnvironment, getSimpleRobotName(),
            DRCObstacleCourseStartingLocation.DEFAULT, simulationTestingParameters, getRobotModel());
   }

   @DeployableTestMethod(estimatedDuration = 21.5)
   @Test(timeout = 64580)
   public void testWalkingState() throws SimulationExceededMaximumTimeException
   {
      testState(HighLevelState.WALKING);
   }

   @DeployableTestMethod(estimatedDuration = 21.5)
   @Test(timeout = 64580)
   public void testDoNothingBahviourState() throws SimulationExceededMaximumTimeException
   {
      testState(HighLevelState.DO_NOTHING_BEHAVIOR);

      OneDegreeOfFreedomJoint[] oneDofJoints = drcBehaviorTestHelper.getRobot().getOneDegreeOfFreedomJoints();

      for (OneDegreeOfFreedomJoint joint : oneDofJoints)
      {
         String jointName = joint.getName();
         double tau = joint.getTau().getDoubleValue();

         if (!jointName.contains("hokuyo"))
         {
            assertTrue(joint.getName() + " tau : " + tau, tau == 0.0);
         }
      }
   }

   @DeployableTestMethod(estimatedDuration = 21.5)
   @Test(timeout = 64580)
   public void testDiagnosticsState() throws SimulationExceededMaximumTimeException
   {
      testState(HighLevelState.DIAGNOSTICS);
   }

   private void testState(HighLevelState desiredState) throws SimulationExceededMaximumTimeException
   {
      boolean success = drcBehaviorTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      assertTrue(success);

      double trajectoryTime = 2.0;

      final HighLevelStateBehavior highLevelStateBehavior = new HighLevelStateBehavior(drcBehaviorTestHelper.getBehaviorCommunicationBridge());

      highLevelStateBehavior.initialize();
      highLevelStateBehavior.setInput(new HighLevelStateMessage(desiredState));
      assertTrue(highLevelStateBehavior.hasInputBeenSet());

      success = drcBehaviorTestHelper.executeBehaviorSimulateAndBlockAndCatchExceptions(highLevelStateBehavior, trajectoryTime);
      assertTrue(success);

      HighLevelState actualState = getCurrentHighLevelState();

      assertTrue(highLevelStateBehavior.isDone());
      assertTrue("Actual high level state: " + actualState + ", does not match desired high level state: " + desiredState + ".",
            desiredState.equals(actualState));
   }

   private HighLevelState getCurrentHighLevelState()
   {
      return drcBehaviorTestHelper.getDRCSimulationFactory().getControllerFactory().getCurrentHighLevelState();
   }
}
