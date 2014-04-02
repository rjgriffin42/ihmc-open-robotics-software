package us.ihmc.darpaRoboticsChallenge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.atlas.initialSetup.SquaredUpDRCRobotInitialSetup;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.visualizer.RobotVisualizer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.time.GlobalTimer;
import com.yobotics.simulationconstructionset.util.FlatGroundProfile;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;

public class DRCFlatGroundWalkingTest
{
   private static final boolean ALWAYS_SHOW_GUI = false;
   private static final boolean KEEP_SCS_UP = false;

   private static final boolean CREATE_MOVIE = BambooTools.doMovieCreation();
   private static final boolean checkNothingChanged = BambooTools.getCheckNothingChanged();

   private static final boolean SHOW_GUI = ALWAYS_SHOW_GUI || checkNothingChanged || CREATE_MOVIE;

   private BlockingSimulationRunner blockingSimulationRunner;
   private DRCController drcController;
   private RobotVisualizer robotVisualizer;
   

   private static final double standingTimeDuration = 1.0;
   private static final double defaultWalkingTimeDuration = BambooTools.isEveryCommitBuild() ? 45.0 : 90.0;
   private static final boolean useVelocityAndHeadingScript = true;
   private static final boolean cheatWithGroundHeightAtForFootstep = false;
   private static final boolean useLoadOfContactPointsForTheFeet = false;
   private static final boolean drawGroundProfile = false;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }
   
   @After
   public void destroySimulationAndRecycleMemory()
   {
      if (KEEP_SCS_UP)
      {
         ThreadTools.sleepForever();
      }

      // Do this here in case a test fails. That way the memory will be recycled.
      if (blockingSimulationRunner != null)
      {
         blockingSimulationRunner.destroySimulation();
         blockingSimulationRunner = null;
      }

      if (drcController != null)
      {
         drcController.dispose();
         drcController = null;
      }

      if (robotVisualizer != null)
      {
         robotVisualizer.close();
         robotVisualizer = null;
      }

      GlobalTimer.clearTimers();
      TimerTaskScheduler.cancelAndReset();
      AsyncContinuousExecutor.cancelAndReset();
      
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }


   @Test
   public void testAtlasFlatGroundWalking() throws SimulationExceededMaximumTimeException
   {
	  final AtlasRobotVersion ATLAS_ROBOT_VERSION = AtlasRobotVersion.DRC_NO_HANDS;
	  final boolean RUNNING_ON_REAL_ROBOT = DRCLocalConfigParameters.RUNNING_ON_REAL_ROBOT;
	  
      BambooTools.reportTestStartedMessage();
      

      DRCRobotModel robotModel = new AtlasRobotModel(ATLAS_ROBOT_VERSION, RUNNING_ON_REAL_ROBOT);
      DRCRobotInterface robotInterface = new PlainDRCRobot(robotModel);
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = new SquaredUpDRCRobotInitialSetup(0.0);

      WalkingControllerParameters walkingControllerParameters = robotModel.getWalkingControlParamaters();
      ArmControllerParameters armControllerParameters = robotModel.getArmControllerParameters();
      DRCFlatGroundWalkingTrack track = setupFlatGroundSimulationTrack(walkingControllerParameters, armControllerParameters, robotInterface, robotInitialSetup, robotModel);

      drcController = track.getDrcController();
      SimulationConstructionSet scs = track.getSimulationConstructionSet();

      NothingChangedVerifier nothingChangedVerifier = null;
      double walkingTimeDuration;
      if (checkNothingChanged)
      {
         nothingChangedVerifier = new NothingChangedVerifier("AtlasFlatGroundWalkingTest", scs);
         walkingTimeDuration = 7.0;
      }
      else
         walkingTimeDuration = defaultWalkingTimeDuration;

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);

//    DoubleYoVariable desiredHeading = (DoubleYoVariable) scs.getVariable("desiredHeading");
//    DoubleYoVariable pelvisYaw = (DoubleYoVariable) scs.getVariable("q_yaw");
//    DoubleYoVariable centerOfMassHeight = (DoubleYoVariable) scs.getVariable("ProcessedSensors.comPositionz");
      DoubleYoVariable comError = (DoubleYoVariable) scs.getVariable("positionError_comHeight");

      initiateMotion(scs, standingTimeDuration, blockingSimulationRunner);

//    ThreadTools.sleepForever();

      double timeIncrement = 1.0;

      while (scs.getTime() - standingTimeDuration < walkingTimeDuration)
      {
         blockingSimulationRunner.simulateAndBlock(timeIncrement);

         // TODO: Put test for heading back in here.
//       if (!MathTools.epsilonEquals(desiredHeading.getDoubleValue(), pelvisYaw.getDoubleValue(), epsilonHeading))
//       {
//          fail("Desired Heading too large of error: " + desiredHeading.getDoubleValue());
//       }

         //TODO: Reduce the error tolerance from 2.5 cm to under 1 cm after we change things so that we are truly 
         // controlling pelvis height, not CoM height.
         if (Math.abs(comError.getDoubleValue()) > 0.06)
         {
            fail("Math.abs(comError.getDoubleValue()) > 0.06: " + comError.getDoubleValue() + " at t = " + scs.getTime());
         }
      }

      if (checkNothingChanged)
         checkNothingChanged(nothingChangedVerifier);

      createMovie(scs);
      BambooTools.reportTestFinishedMessage();

   }
 
   private void initiateMotion(SimulationConstructionSet scs, double standingTimeDuration, BlockingSimulationRunner runner)
           throws SimulationExceededMaximumTimeException
   {
      BooleanYoVariable walk = (BooleanYoVariable) scs.getVariable("walk");
      walk.set(false);
      runner.simulateAndBlock(standingTimeDuration);
      walk.set(true);
   }

   private void createMovie(SimulationConstructionSet scs)
   {
      if (CREATE_MOVIE)
      {
         BambooTools.createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(scs, 1);
      }
   }

   boolean setupForCheatingUsingGroundHeightAtForFootstepProvider = false;

   private DRCFlatGroundWalkingTrack setupFlatGroundSimulationTrack(WalkingControllerParameters walkingControllerParameters,
         ArmControllerParameters armControllerParameters, DRCRobotInterface robotInterface, DRCRobotInitialSetup<SDFRobot> robotInitialSetup,
         DRCRobotModel robotModel)
   {
      AutomaticSimulationRunner automaticSimulationRunner = null;
      DRCGuiInitialSetup guiInitialSetup = createGUIInitialSetup();

      double timePerRecordTick = DRCConfigParameters.CONTROL_DT;
      int simulationDataBufferSize = 16000;
      
      GroundProfile groundProfile = new FlatGroundProfile();
      
      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT(), useLoadOfContactPointsForTheFeet);
      scsInitialSetup.setDrawGroundProfile(drawGroundProfile);
      
      if (cheatWithGroundHeightAtForFootstep)
         scsInitialSetup.setInitializeEstimatorToActual(true);

      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(walkingControllerParameters, armControllerParameters, robotInterface, robotInitialSetup, guiInitialSetup,
                                                               scsInitialSetup, useVelocityAndHeadingScript, automaticSimulationRunner, timePerRecordTick,
                                                               simulationDataBufferSize, cheatWithGroundHeightAtForFootstep, robotModel);

      SimulationConstructionSet scs = drcFlatGroundWalkingTrack.getSimulationConstructionSet();

      setupCameraForUnitTest(scs);

      return drcFlatGroundWalkingTrack;
   }

   private void checkNothingChanged(NothingChangedVerifier nothingChangedVerifier)
   {
      ArrayList<String> stringsToIgnore = new ArrayList<String>();
      stringsToIgnore.add("nano");
      stringsToIgnore.add("milli");
      stringsToIgnore.add("Timer");

      boolean writeNewBaseFile = nothingChangedVerifier.getWriteNewBaseFile();

      double maxPercentDifference = 0.001;
      nothingChangedVerifier.verifySameResultsAsPreviously(maxPercentDifference, stringsToIgnore);
      assertFalse("Had to write new base file. On next run nothing should change", writeNewBaseFile);
   }

   private DRCGuiInitialSetup createGUIInitialSetup()
   {
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(true, false);
      guiInitialSetup.setIsGuiShown(SHOW_GUI);

      return guiInitialSetup;
   }

   protected void setupCameraForUnitTest(SimulationConstructionSet scs)
   {
      CameraConfiguration cameraConfiguration = new CameraConfiguration("testCamera");
      cameraConfiguration.setCameraFix(0.6, 0.4, 1.1);
      cameraConfiguration.setCameraPosition(-0.15, 10.0, 3.0);
      cameraConfiguration.setCameraTracking(true, true, true, false);
      cameraConfiguration.setCameraDolly(true, true, true, false);
      scs.setupCamera(cameraConfiguration);
      scs.selectCamera("testCamera");
   }
}
