package us.ihmc.darpaRoboticsChallenge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.automaticSimulationRunner.AutomaticSimulationRunner;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.visualizer.RobotVisualizer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.PlainDRCRobot;
import us.ihmc.darpaRoboticsChallenge.initialSetup.SquaredUpDRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.projectM.R2Sim02.initialSetup.RobotInitialSetup;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.time.GlobalTimer;
import com.yobotics.simulationconstructionset.util.ground.BumpyGroundProfile;
import com.yobotics.simulationconstructionset.util.ground.CombinedTerrainObject;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;

public class DRCBumpyAndShallowRampsWalkingTest
{
   private static final boolean ALWAYS_SHOW_GUI = false;
   private static final boolean KEEP_SCS_UP = false;
   private static final DRCRobotModel robotModel = DRCRobotModel.DRC_NO_HANDS;

   private static final boolean CREATE_MOVIE = BambooTools.doMovieCreation();
   private static final boolean checkNothingChanged = BambooTools.getCheckNothingChanged();

   private static final boolean SHOW_GUI = ALWAYS_SHOW_GUI || checkNothingChanged || CREATE_MOVIE;

   private BlockingSimulationRunner blockingSimulationRunner;
   private DRCController drcController;
   private RobotVisualizer robotVisualizer;

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
   public void testDRCOverShallowRamp() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double standingTimeDuration = 1.0;
      double maximumWalkTime = 30.0;
      double desiredVelocityValue = 1.0;
      double desiredHeadingValue = 0.0;

      boolean useVelocityAndHeadingScript = false;
      boolean cheatWithGroundHeightAtForFootstep = true;
      boolean useLoadOfContactPointsForTheFeet = false;

      if (checkNothingChanged) maximumWalkTime = 3.0;
      
      WalkingControllerParameters drcControlParameters = DRCLocalConfigParameters.defaultModel.getWalkingControlParamaters();
      ArmControllerParameters armControllerParameters = DRCLocalConfigParameters.defaultModel.getArmControllerParameters();
      
//      drcControlParameters.setNominalHeightAboveAnkle(drcControlParameters.nominalHeightAboveAnkle() - 0.03);    // Need to do this or the leg goes straight and the robot falls.

      Pair<CombinedTerrainObject, Double> combinedTerrainObjectAndRampEndX = createRamp();
      CombinedTerrainObject combinedTerrainObject = combinedTerrainObjectAndRampEndX.first();
      boolean drawGroundProfile = false;

      double rampEndX = combinedTerrainObjectAndRampEndX.second();
      
      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, combinedTerrainObject, drawGroundProfile, useVelocityAndHeadingScript,
            cheatWithGroundHeightAtForFootstep, useLoadOfContactPointsForTheFeet);

      drcController = track.getDrcController();
      robotVisualizer = track.getRobotVisualizer();

      SimulationConstructionSet scs = track.getSimulationConstructionSet();
      scs.setGroundVisible(false);
      scs.addStaticLinkGraphics(combinedTerrainObject.getLinkGraphics());

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);

      NothingChangedVerifier nothingChangedVerifier = null;
      if (checkNothingChanged)
      {
         nothingChangedVerifier = new NothingChangedVerifier("DRCOverShallowRampTest", scs);
      }

      BooleanYoVariable walk = (BooleanYoVariable) scs.getVariable("walk");
      DoubleYoVariable q_x = (DoubleYoVariable) scs.getVariable("q_x");
      DoubleYoVariable desiredSpeed = (DoubleYoVariable) scs.getVariable("desiredVelocityX");
      DoubleYoVariable desiredHeading = (DoubleYoVariable) scs.getVariable("desiredHeading");

//    DoubleYoVariable centerOfMassHeight = (DoubleYoVariable) scs.getVariable("ProcessedSensors.comPositionz");
      DoubleYoVariable comError = (DoubleYoVariable) scs.getVariable("positionError_comHeight");
      DoubleYoVariable leftFootHeight = (DoubleYoVariable) scs.getVariable("p_leftFootPositionZ");
      DoubleYoVariable rightFootHeight = (DoubleYoVariable) scs.getVariable("p_rightFootPositionZ");

      initiateMotion(standingTimeDuration, blockingSimulationRunner, walk);
      desiredSpeed.set(desiredVelocityValue);
      desiredHeading.set(desiredHeadingValue);

//    ThreadTools.sleepForever();

      double timeIncrement = 1.0;
      boolean done = false;
      while (!done)
      {
         blockingSimulationRunner.simulateAndBlock(timeIncrement);

         if (Math.abs(comError.getDoubleValue()) > 0.09)
         {
            fail("comError = " + Math.abs(comError.getDoubleValue()));
         }

         if (scs.getTime() > standingTimeDuration + maximumWalkTime)
            done = true;
         if (q_x.getDoubleValue() > rampEndX)
            done = true;
      }

      if (checkNothingChanged)
         checkNothingChanged(nothingChangedVerifier);

      createMovie(scs);
   }
   
   
   @Test
   public void testDRCOverRandomBlocks() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double standingTimeDuration = 1.0;
      double maximumWalkTime = 10.0;
      double desiredVelocityValue = 0.5;
      double desiredHeadingValue = 0.0;

      boolean useVelocityAndHeadingScript = false;
      boolean cheatWithGroundHeightAtForFootstep = true;
      boolean useLoadOfContactPointsForTheFeet = true;
      
      WalkingControllerParameters drcControlParameters = DRCLocalConfigParameters.defaultModel.getWalkingControlParamaters();
      ArmControllerParameters armControllerParameters = DRCLocalConfigParameters.defaultModel.getArmControllerParameters();
      
//      drcControlParameters.setNominalHeightAboveAnkle(drcControlParameters.nominalHeightAboveAnkle() - 0.03);    // Need to do this or the leg goes straight and the robot falls.

      Pair<CombinedTerrainObject, Double> combinedTerrainObjectAndRampEndX = createRandomBlocks(); 
      CombinedTerrainObject combinedTerrainObject = combinedTerrainObjectAndRampEndX.first();
      boolean drawGroundProfile = false;

      double rampEndX = combinedTerrainObjectAndRampEndX.second();
      
      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, combinedTerrainObject, drawGroundProfile, useVelocityAndHeadingScript,
            cheatWithGroundHeightAtForFootstep, useLoadOfContactPointsForTheFeet);

      drcController = track.getDrcController();
      robotVisualizer = track.getRobotVisualizer();

      SimulationConstructionSet scs = track.getSimulationConstructionSet();
      scs.setGroundVisible(false);
      scs.addStaticLinkGraphics(combinedTerrainObject.getLinkGraphics());

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);

      BooleanYoVariable walk = (BooleanYoVariable) scs.getVariable("walk");
      DoubleYoVariable q_x = (DoubleYoVariable) scs.getVariable("q_x");
      DoubleYoVariable desiredSpeed = (DoubleYoVariable) scs.getVariable("desiredVelocityX");
      DoubleYoVariable desiredHeading = (DoubleYoVariable) scs.getVariable("desiredHeading");

//    DoubleYoVariable centerOfMassHeight = (DoubleYoVariable) scs.getVariable("ProcessedSensors.comPositionz");
      DoubleYoVariable comError = (DoubleYoVariable) scs.getVariable("positionError_comHeight");

      initiateMotion(standingTimeDuration, blockingSimulationRunner, walk);
      desiredSpeed.set(desiredVelocityValue);
      desiredHeading.set(desiredHeadingValue);

//    ThreadTools.sleepForever();

      double timeIncrement = 1.0;
      boolean done = false;
      while (!done)
      {
         blockingSimulationRunner.simulateAndBlock(timeIncrement);

         if (Math.abs(comError.getDoubleValue()) > 0.09)
         {
            fail("comError = " + Math.abs(comError.getDoubleValue()));
         }

         if (scs.getTime() > standingTimeDuration + maximumWalkTime)
            done = true;
         if (q_x.getDoubleValue() > rampEndX)
            done = true;
      }

      createMovie(scs);
   }
   
   private Pair<CombinedTerrainObject, Double> createRamp()
   {
      double rampSlopeUp = 0.1;
      double rampSlopeDown = 0.08;

      double rampXStart0 = 0.5;
      double rampXLength0 = 2.0;
      double landingHeight = rampSlopeUp * rampXLength0;
      double landingLength = 1.0;
      double rampXLength1 = landingHeight / rampSlopeDown;

      double rampYStart = -2.0;
      double rampYEnd = 6.0;

      double landingStartX = rampXStart0 + rampXLength0;
      double landingEndX = landingStartX + landingLength;
      double rampEndX = landingEndX + rampXLength1;

      CombinedTerrainObject combinedTerrainObject = new CombinedTerrainObject("JustARamp");

      AppearanceDefinition appearance = YoAppearance.Green();
      combinedTerrainObject.addRamp(rampXStart0, rampYStart, landingStartX, rampYEnd, landingHeight, appearance);
      combinedTerrainObject.addBox(landingStartX, rampYStart, landingEndX, rampYEnd, 0.0, landingHeight, YoAppearance.Gray());
      combinedTerrainObject.addRamp(rampEndX, rampYStart, landingEndX, rampYEnd, landingHeight, appearance);

      combinedTerrainObject.addBox(rampXStart0 - 2.0, rampYStart, rampEndX + 2.0, rampYEnd, -0.05, 0.0);
      
      return new Pair<CombinedTerrainObject, Double>(combinedTerrainObject, rampEndX);
   }
   
   private Pair<CombinedTerrainObject, Double> createRandomBlocks()
   {
      CombinedTerrainObject combinedTerrainObject = new CombinedTerrainObject("RandomBlocks");

      Random random = new Random(1776L);
      int numberOfBoxes = 200;
      
      double xMin = -0.2, xMax = 5.0;
      double yMin = -1.0, yMax = 1.0;
      double maxLength = 0.4;
      double maxHeight = 0.06;

      combinedTerrainObject.addBox(xMin - 2.0, yMin-maxLength, xMax + 2.0, yMax + maxLength, -0.01, 0.0, YoAppearance.Gold());

      for (int i=0; i<numberOfBoxes; i++)
      {
         double xStart = RandomTools.generateRandomDouble(random, xMin, xMax);
         double yStart = RandomTools.generateRandomDouble(random, yMin, yMax);
         double xEnd = xStart + RandomTools.generateRandomDouble(random, maxLength*0.1, maxLength);
         double yEnd = yStart + RandomTools.generateRandomDouble(random, maxLength*0.1, maxLength);
         double zStart = 0.0;
         double zEnd = zStart + RandomTools.generateRandomDouble(random, maxHeight*0.1, maxHeight);
         combinedTerrainObject.addBox(xStart, yStart, xEnd, yEnd, zStart, zEnd, YoAppearance.Green());
      }
      
      return new Pair<CombinedTerrainObject, Double>(combinedTerrainObject, xMax);
   }
   
   @Test
   public void testDRCBumpyGroundWalking() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double standingTimeDuration = 1.0;
      double walkingTimeDuration = 40.0;

      boolean useVelocityAndHeadingScript = true;
      boolean cheatWithGroundHeightAtForFootstep = false;
      boolean useLoadOfContactPointsForTheFeet = false;

      GroundProfile groundProfile = createBumpyGroundProfile();
      boolean drawGroundProfile = true;
      
      WalkingControllerParameters drcControlParameters = DRCLocalConfigParameters.defaultModel.getWalkingControlParamaters();
      ArmControllerParameters armControllerParameters = DRCLocalConfigParameters.defaultModel.getArmControllerParameters();
      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, groundProfile, drawGroundProfile,
            useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep, useLoadOfContactPointsForTheFeet);

      drcController = track.getDrcController();
      SimulationConstructionSet scs = track.getSimulationConstructionSet();

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);

      BooleanYoVariable walk = (BooleanYoVariable) scs.getVariable("walk");
      DoubleYoVariable stepLength = (DoubleYoVariable) scs.getVariable("maxStepLength");
      DoubleYoVariable offsetHeightAboveGround = (DoubleYoVariable) scs.getVariable("offsetHeightAboveGround");
      DoubleYoVariable comError = (DoubleYoVariable) scs.getVariable("positionError_comHeight");
      stepLength.set(0.4);
      offsetHeightAboveGround.set(-0.1);
      initiateMotion(standingTimeDuration, blockingSimulationRunner, walk);

      double timeIncrement = 1.0;

      while (scs.getTime() - standingTimeDuration < walkingTimeDuration)
      {
         blockingSimulationRunner.simulateAndBlock(timeIncrement);

         if (Math.abs(comError.getDoubleValue()) > 0.3)
         {
            fail("Math.abs(comError.getDoubleValue()) > 0.3: " + comError.getDoubleValue() + " at t = " + scs.getTime());
         }
      }

      createMovie(scs);
      BambooTools.reportTestFinishedMessage();
   }

   private void initiateMotion(double standingTimeDuration, BlockingSimulationRunner runner, BooleanYoVariable walk)
           throws SimulationExceededMaximumTimeException
   {
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

   private DRCFlatGroundWalkingTrack setupSimulationTrack(WalkingControllerParameters drcControlParameters, ArmControllerParameters
         armControllerParameters, GroundProfile groundProfile, boolean drawGroundProfile,
         boolean useVelocityAndHeadingScript, boolean cheatWithGroundHeightAtForFootstep, boolean useLoadOfContactPointsForTheFeet)
   {
      AutomaticSimulationRunner automaticSimulationRunner = null;
      DRCGuiInitialSetup guiInitialSetup = createGUIInitialSetup();
      
      double timePerRecordTick = DRCConfigParameters.CONTROL_DT;
      int simulationDataBufferSize = 16000;

      RobotInitialSetup<SDFRobot> robotInitialSetup = new SquaredUpDRCRobotInitialSetup(0.0);
      DRCRobotInterface robotInterface = new PlainDRCRobot(robotModel, false, useLoadOfContactPointsForTheFeet);
      DRCSCSInitialSetup scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotInterface.getSimulateDT(), useLoadOfContactPointsForTheFeet);
      scsInitialSetup.setDrawGroundProfile(drawGroundProfile);
      
      if (cheatWithGroundHeightAtForFootstep)
         scsInitialSetup.setInitializeEstimatorToActual(true);

      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(drcControlParameters, armControllerParameters, robotInterface, robotInitialSetup, guiInitialSetup,
                                                               scsInitialSetup, useVelocityAndHeadingScript, automaticSimulationRunner, timePerRecordTick,
                                                               simulationDataBufferSize, cheatWithGroundHeightAtForFootstep,robotModel);

      SimulationConstructionSet scs = drcFlatGroundWalkingTrack.getSimulationConstructionSet();
      scs.setGroundVisible(false);
      setupCameraForUnitTest(scs);

      return drcFlatGroundWalkingTrack;
   }
   
   private static BumpyGroundProfile createBumpyGroundProfile()
   {
      double xAmp1 = 0.05, xFreq1 = 0.5, xAmp2 = 0.01, xFreq2 = 0.5;
      double yAmp1 = 0.01, yFreq1 = 0.07, yAmp2 = 0.05, yFreq2 = 0.37;
      BumpyGroundProfile groundProfile = new BumpyGroundProfile(xAmp1, xFreq1, xAmp2, xFreq2, yAmp1, yFreq1, yAmp2, yFreq2);
      return groundProfile;
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

