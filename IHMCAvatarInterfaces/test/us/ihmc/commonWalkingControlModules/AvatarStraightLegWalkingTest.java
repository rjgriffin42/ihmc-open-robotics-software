package us.ihmc.commonWalkingControlModules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.MultiRobotTestInterface;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.util.environments.FlatGroundEnvironment;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.tools.MemoryTools;
import us.ihmc.tools.thread.ThreadTools;

import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public abstract class AvatarStraightLegWalkingTest implements MultiRobotTestInterface
{
   private static final SimulationTestingParameters simulationTestingParameters = SimulationTestingParameters.createFromEnvironmentVariables();

   private DRCSimulationTestHelper drcSimulationTestHelper;

   private static final String forwardFastScript = "scripts/ExerciseAndJUnitScripts/stepAdjustment_forwardWalkingFast.xml";
   private static final String yawScript = "scripts/ExerciseAndJUnitScripts/icpOptimizationPushTestScript.xml";
   private static final String slowStepScript = "scripts/ExerciseAndJUnitScripts/icpOptimizationPushTestScriptSlow.xml";

   private static double simulationTime = 10.0;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());
   }

   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (simulationTestingParameters.getKeepSCSUp())
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
      BambooTools.reportTestFinishedMessage(simulationTestingParameters.getShowWindows());
   }

   @ContinuousIntegrationTest(estimatedDuration =  20.0)
   @Test(timeout = 120000)
   public void testForwardWalking() throws SimulationExceededMaximumTimeException
   {
      setupTest(getScript());

      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(simulationTime);

      assertTrue(success);
   }

   private void setupTest(String scriptName) throws SimulationExceededMaximumTimeException
   {
      this.setupTest(scriptName, ReferenceFrame.getWorldFrame());
   }

   private void setupTest(String scriptName, ReferenceFrame yawReferenceFrame) throws SimulationExceededMaximumTimeException
   {
      FlatGroundEnvironment flatGround = new FlatGroundEnvironment();
      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;
      drcSimulationTestHelper = new DRCSimulationTestHelper(flatGround, "DRCSimpleFlatGroundScriptTest", selectedLocation, simulationTestingParameters, getRobotModel());

      if (scriptName != null && !scriptName.isEmpty())
      {
         drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.001);
         InputStream scriptInputStream = getClass().getClassLoader().getResourceAsStream(scriptName);
         if (yawReferenceFrame != null)
         {
            drcSimulationTestHelper.loadScriptFile(scriptInputStream, yawReferenceFrame);
         }
         else
         {
            drcSimulationTestHelper.loadScriptFile(scriptInputStream, ReferenceFrame.getWorldFrame());
         }
      }

      setupCamera();
      ThreadTools.sleep(1000);
   }

   private void setupCamera()
   {
      Point3D cameraFix = new Point3D(0.0, 0.0, 0.89);
      Point3D cameraPosition = new Point3D(10.0, 2.0, 1.37);
      drcSimulationTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
   }

   public String getScript()
   {
      return forwardFastScript;
   }

   public String getYawscript()
   {
      return yawScript;
   }

   public String getSlowstepScript()
   {
      return slowStepScript;
   }
}
