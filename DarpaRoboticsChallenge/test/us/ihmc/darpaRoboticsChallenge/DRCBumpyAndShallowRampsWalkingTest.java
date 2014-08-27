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
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.time.GlobalTimer;
import com.yobotics.simulationconstructionset.util.ground.BumpyGroundProfile;
import com.yobotics.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;

@SuppressWarnings("deprecation")
public abstract class DRCBumpyAndShallowRampsWalkingTest implements MultiRobotTestInterface
{
   private static final boolean ALWAYS_SHOW_GUI = false;
   private static final boolean KEEP_SCS_UP = false;

   private static final boolean CREATE_MOVIE = BambooTools.doMovieCreation();
   private static final boolean checkNothingChanged = BambooTools.getCheckNothingChanged();
   private static final boolean SHOW_GUI = ALWAYS_SHOW_GUI || checkNothingChanged || CREATE_MOVIE;
   private DRCRobotModel robotModel;

   private BlockingSimulationRunner blockingSimulationRunner;
   private DRCSimulationFactory drcSimulation;

   @Before
   public void showMemoryUsageBeforeTest()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
      robotModel = getRobotModel();
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

      if (drcSimulation != null)
      {
         drcSimulation.dispose();
         drcSimulation = null;
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

      if (checkNothingChanged) maximumWalkTime = 3.0;
      
      WalkingControllerParameters drcControlParameters = robotModel.getWalkingControllerParameters();
      ArmControllerParameters armControllerParameters = robotModel.getArmControllerParameters();
      
//      drcControlParameters.setNominalHeightAboveAnkle(drcControlParameters.nominalHeightAboveAnkle() - 0.03);    // Need to do this or the leg goes straight and the robot falls.

      Pair<CombinedTerrainObject3D, Double> combinedTerrainObjectAndRampEndX = createRamp();
      CombinedTerrainObject3D combinedTerrainObject = combinedTerrainObjectAndRampEndX.first();
      boolean drawGroundProfile = false;

      double rampEndX = combinedTerrainObjectAndRampEndX.second();
      
      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, null, combinedTerrainObject, drawGroundProfile, useVelocityAndHeadingScript,
            cheatWithGroundHeightAtForFootstep);

      drcSimulation = track.getDrcSimulation();

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
//      DoubleYoVariable leftFootHeight = (DoubleYoVariable) scs.getVariable("p_leftFootPositionZ");
//      DoubleYoVariable rightFootHeight = (DoubleYoVariable) scs.getVariable("p_rightFootPositionZ");

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
      
      WalkingControllerParameters drcControlParameters = robotModel.getWalkingControllerParameters();
      ArmControllerParameters armControllerParameters = robotModel.getArmControllerParameters();
      
//      drcControlParameters.setNominalHeightAboveAnkle(drcControlParameters.nominalHeightAboveAnkle() - 0.03);    // Need to do this or the leg goes straight and the robot falls.

      Pair<CombinedTerrainObject3D, Double> combinedTerrainObjectAndRampEndX = createRandomBlocks(); 
      CombinedTerrainObject3D combinedTerrainObject = combinedTerrainObjectAndRampEndX.first();
      boolean drawGroundProfile = false;

      double rampEndX = combinedTerrainObjectAndRampEndX.second();
      
      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, null, combinedTerrainObject, drawGroundProfile, useVelocityAndHeadingScript,
            cheatWithGroundHeightAtForFootstep);

      drcSimulation = track.getDrcSimulation();

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
   
   private Pair<CombinedTerrainObject3D, Double> createRamp()
   {
      double rampSlopeUp = 0.1;
      double rampSlopeDown = 0.08;

      double rampXStart0 = 0.5;
      double rampXLength0 = 6.0; //2.0;
      double landingHeight = rampSlopeUp * rampXLength0;
      double landingLength = 1.0;
      double rampXLength1 = landingHeight / rampSlopeDown;

      double rampYStart = -2.0;
      double rampYEnd = 6.0;

      double landingStartX = rampXStart0 + rampXLength0;
      double landingEndX = landingStartX + landingLength;
      double rampEndX = landingEndX + rampXLength1;

      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D("JustARamp");

      AppearanceDefinition appearance = YoAppearance.Green();
      combinedTerrainObject.addRamp(rampXStart0, rampYStart, landingStartX, rampYEnd, landingHeight, appearance);
      combinedTerrainObject.addBox(landingStartX, rampYStart, landingEndX, rampYEnd, 0.0, landingHeight, YoAppearance.Gray());
      combinedTerrainObject.addRamp(rampEndX, rampYStart, landingEndX, rampYEnd, landingHeight, appearance);

      combinedTerrainObject.addBox(rampXStart0 - 2.0, rampYStart, rampEndX + 2.0, rampYEnd, -0.05, 0.0);
      
      return new Pair<CombinedTerrainObject3D, Double>(combinedTerrainObject, rampEndX);
   }
   
   private Pair<CombinedTerrainObject3D, Double> createRandomBlocks()
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D("RandomBlocks");

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
      
      return new Pair<CombinedTerrainObject3D, Double>(combinedTerrainObject, xMax);
   }
   
   @Test
   public void testDRCBumpyGroundWalking() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double standingTimeDuration = 1.0;
      double walkingTimeDuration = 40.0;

      boolean useVelocityAndHeadingScript = true;
      
      //TODO: This should work with cheatWithGroundHeightAtForFootstep = false also, but for some reason height gets messed up and robot gets stuck...
      boolean cheatWithGroundHeightAtForFootstep = true;

      GroundProfile3D groundProfile = createBumpyGroundProfile();
      boolean drawGroundProfile = true;
      
      WalkingControllerParameters drcControlParameters = robotModel.getWalkingControllerParameters();
      ArmControllerParameters armControllerParameters = robotModel.getArmControllerParameters();
      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, groundProfile, null, drawGroundProfile,
            useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep);

      drcSimulation = track.getDrcSimulation();
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
         BambooTools.createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(getSimpleRobotName(), scs, 1);
      }
   }

   boolean setupForCheatingUsingGroundHeightAtForFootstepProvider = false;

   private DRCFlatGroundWalkingTrack setupSimulationTrack(WalkingControllerParameters drcControlParameters, ArmControllerParameters
         armControllerParameters, GroundProfile3D groundProfile, GroundProfile3D groundProfile3D, boolean drawGroundProfile,
         boolean useVelocityAndHeadingScript, boolean cheatWithGroundHeightAtForFootstep)
   {
      DRCGuiInitialSetup guiInitialSetup = createGUIInitialSetup();
      
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = robotModel.getDefaultRobotInitialSetup(0, 0);
      
      DRCSCSInitialSetup scsInitialSetup;
      
      if (groundProfile != null)
      {
         scsInitialSetup = new DRCSCSInitialSetup(groundProfile, robotModel.getSimulateDT());
      }
      else
      {
         scsInitialSetup = new DRCSCSInitialSetup(groundProfile3D, robotModel.getSimulateDT());
      }
      scsInitialSetup.setDrawGroundProfile(drawGroundProfile);
      
      if (cheatWithGroundHeightAtForFootstep)
         scsInitialSetup.setInitializeEstimatorToActual(true);

      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(robotInitialSetup, guiInitialSetup,
                                                               scsInitialSetup, useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep, robotModel);

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

