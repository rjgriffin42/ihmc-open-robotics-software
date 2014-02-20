package us.ihmc.darpaRoboticsChallenge;

import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.SquaredUpDRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;
import us.ihmc.utilities.MemoryTools;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.UnreasonableAccelerationException;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationRunner.SimulationRewindabilityVerifier;
import com.yobotics.simulationconstructionset.util.simulationRunner.VariableDifference;

public class DRCFlatGroundRewindabilityTest
{
   private static final boolean SHOW_GUI = false;
   private static final double totalTimeToTest = 10.0;
   private static final double timeToTickAhead = 1.5;
   private static final double timePerTick = 0.01;

   @Before
   public void setUp() throws Exception
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void showMemoryUsageAfterTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   
   @Ignore
   @Test
   public void testCanRewindAndGoForward() throws UnreasonableAccelerationException
   {
      BambooTools.reportTestStartedMessage();

      int numberOfSteps = 100;

      SimulationConstructionSet scs = setupScs();

      for (int i = 0; i < numberOfSteps; i++)
      {
         scs.simulateOneRecordStepNow();
         scs.simulateOneRecordStepNow();
         scs.stepBackwardNow();
      }

      scs.closeAndDispose();

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testRewindability() throws UnreasonableAccelerationException, SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      
      int numTicksToTest = (int) Math.round(totalTimeToTest / timePerTick);
      if (numTicksToTest < 1)
         numTicksToTest = 1;

      int numTicksToSimulateAhead = (int) Math.round(timeToTickAhead / timePerTick);
      if (numTicksToSimulateAhead < 1)
         numTicksToSimulateAhead = 1;

      SimulationConstructionSet scs1 = setupScs();    // createTheSimulation(ticksForDataBuffer);
      SimulationConstructionSet scs2 = setupScs();    // createTheSimulation(ticksForDataBuffer);
      
      BlockingSimulationRunner blockingSimulationRunner1 = new BlockingSimulationRunner(scs1, 1000.0);
      BlockingSimulationRunner blockingSimulationRunner2 = new BlockingSimulationRunner(scs2, 1000.0);
      
      BooleanYoVariable walk1 = (BooleanYoVariable) scs1.getVariable("walk");
      BooleanYoVariable walk2 = (BooleanYoVariable) scs2.getVariable("walk");
      
//      double standingTimeDuration = 1.0;
//      double walkingTimeDuration = 1.0;
//      initiateMotion(standingTimeDuration, walkingTimeDuration, blockingSimulationRunner1, walk1);      
//      initiateMotion(standingTimeDuration, walkingTimeDuration, blockingSimulationRunner2, walk2);

      walk1.set(true);
      walk2.set(true);
      
      ArrayList<String> exceptions = new ArrayList<String>();
      exceptions.add("gc_");
      exceptions.add("toolFrame");
      exceptions.add("ef_");
      exceptions.add("kp_");
      exceptions.add("TimeNano");
      exceptions.add("DurationMilli");
      SimulationRewindabilityVerifier checker = new SimulationRewindabilityVerifier(scs1, scs2, exceptions);

      // TODO velocityMagnitudeInHeading usually differs by 0.00125
      double maxDifferenceAllowed = 1e-7;
      ArrayList<VariableDifference> variableDifferences;getClass();
      variableDifferences = checker.checkRewindabilityWithSimpleMethod(numTicksToTest, maxDifferenceAllowed);
      if (!variableDifferences.isEmpty())
      {
         System.err.println("variableDifferences: \n" + VariableDifference.allVariableDifferencesToString(variableDifferences));
         if (SHOW_GUI)
            sleepForever();
         fail("Found Variable Differences!\n variableDifferences: \n" + VariableDifference.allVariableDifferencesToString(variableDifferences));
      }

      // sleepForever();

      scs1.closeAndDispose();
      scs2.closeAndDispose();

      BambooTools.reportTestFinishedMessage();
   }

   private SimulationConstructionSet setupScs()
   {
      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;

      GroundProfile groundProfile = new FlatGroundProfile();

      DRCRobotModel robotModel = DRCRobotModel.getDefaultRobotModel();
      WalkingControllerParameters drcControlParameters = robotModel.getWalkingControlParamaters();
      ArmControllerParameters armControllerParameters = robotModel.getArmControllerParameters();

      AutomaticSimulationRunner automaticSimulationRunner = null;
      DRCGuiInitialSetup guiInitialSetup = createGUIInitialSetup();

      double timePerRecordTick = DRCConfigParameters.CONTROL_DT;
      int simulationDataBufferSize = 16000;

      RobotInitialSetup<SDFRobot> robotInitialSetup = new SquaredUpDRCRobotInitialSetup(0.0);
      DRCRobotInterface robotInterface = new PlainDRCRobot(robotModel, false);
      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT());

      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(drcControlParameters, armControllerParameters, robotInterface, robotInitialSetup,
                                                               guiInitialSetup, scsInitialSetup, useVelocityAndHeadingScript, automaticSimulationRunner,
                                                               timePerRecordTick, simulationDataBufferSize,
            cheatWithGroundHeightAtForFootstep);

      SimulationConstructionSet scs = drcFlatGroundWalkingTrack.getSimulationConstructionSet();

      setupCameraForUnitTest(scs);
      
      return scs;
   }

   private void setupCameraForUnitTest(SimulationConstructionSet scs)
   {
      CameraConfiguration cameraConfiguration = new CameraConfiguration("testCamera");
      cameraConfiguration.setCameraFix(0.6, 0.4, 1.1);
      cameraConfiguration.setCameraPosition(-0.15, 10.0, 3.0);
      cameraConfiguration.setCameraTracking(true, true, true, false);
      cameraConfiguration.setCameraDolly(true, true, true, false);
      scs.setupCamera(cameraConfiguration);
      scs.selectCamera("testCamera");
   }

   private DRCGuiInitialSetup createGUIInitialSetup()
   {
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false);
      guiInitialSetup.setIsGuiShown(SHOW_GUI);

      return guiInitialSetup;
   }

   private void initiateMotion(double standingTimeDuration, double walkingTimeDuration, BlockingSimulationRunner runner, BooleanYoVariable walk)
           throws SimulationExceededMaximumTimeException
   {
      walk.set(false);
      runner.simulateAndBlock(standingTimeDuration);
      walk.set(true);
//      runner.simulateAndBlock(walkingTimeDuration);
   }

   private void sleepForever()
   {
      while (true)
      {
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e)
         {
         }

      }
   }
}
