package us.ihmc.darpaRoboticsChallenge.roughTerrainWalking;

import static org.junit.Assert.*;

import java.util.Random;

import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.OptimizationMomentumControlModule.QPSolverFlavor;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTrack;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCSCSInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCSimulationFactory;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.visualization.WalkControllerSliderBoard;
import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.time.GlobalTimer;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.util.ground.CombinedTerrainObject3D;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

@SuppressWarnings("deprecation")
public abstract class DRCFootExplorationTest implements MultiRobotTestInterface
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

   //   @Test(timeout=300000)
   public void testDRCOverRandomBlocks() throws SimulationExceededMaximumTimeException
   {
      testDRCOverBlocksField(0.4);
   }

   @Test(timeout=300000)
   public void testDRCOverRandomBars() throws SimulationExceededMaximumTimeException
   {
      testDRCOverBlocksField(-0.4);
   }

   private void testDRCOverBlocksField(double StartingYOffsetFromCenter) throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double standingTimeDuration = 2.0;
      double maximumWalkTime = 10.0;
      double desiredVelocityValue = 0.3;
      double desiredHeadingValue = 0.0;

      boolean useVelocityAndHeadingScript = false;
      boolean cheatWithGroundHeightAtForFootstep = true;

      WalkingControllerParameters drcControlParameters = robotModel.getWalkingControllerParameters();
      ArmControllerParameters armControllerParameters = robotModel.getArmControllerParameters();

      //         drcControlParameters.setNominalHeightAboveAnkle(drcControlParameters.nominalHeightAboveAnkle() - 0.03);    // Need to do this or the leg goes straight and the robot falls.

      Pair<CombinedTerrainObject3D, Double> combinedTerrainObjectAndRampEndX = createRandomBlocks();
      CombinedTerrainObject3D combinedTerrainObject = combinedTerrainObjectAndRampEndX.first();
      boolean drawGroundProfile = false;

      double rampEndX = combinedTerrainObjectAndRampEndX.second();
      DRCRobotInitialSetup<SDFRobot> robotInitialSetup = robotModel.getDefaultRobotInitialSetup(0.0001, 0); //slighly off ground
      robotInitialSetup.setOffset(new Vector3d(0, StartingYOffsetFromCenter, 0));

      DRCFlatGroundWalkingTrack track = setupSimulationTrack(drcControlParameters, armControllerParameters, null, combinedTerrainObject, drawGroundProfile,
            useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep, robotInitialSetup);

      drcSimulation = track.getDrcSimulation();

      SimulationConstructionSet scs = track.getSimulationConstructionSet();
      scs.setGroundVisible(false);
      scs.addStaticLinkGraphics(combinedTerrainObject.getLinkGraphics());

      blockingSimulationRunner = new BlockingSimulationRunner(scs, 1000.0);

      BooleanYoVariable walk = (BooleanYoVariable) scs.getVariable("walk");
      DoubleYoVariable q_x = (DoubleYoVariable) scs.getVariable("q_x");
      DoubleYoVariable desiredSpeed = (DoubleYoVariable) scs.getVariable("desiredVelocityX");
      DoubleYoVariable desiredHeading = (DoubleYoVariable) scs.getVariable("desiredHeading");
      BooleanYoVariable performCoPExploration = (BooleanYoVariable) scs.getVariable("FootExplorationControlModule", "performCoPExploration");
      EnumYoVariable<QPSolverFlavor> requestedQPSolver = (EnumYoVariable<QPSolverFlavor>) scs.getVariable("OptimizationMomentumControlModule",
            "requestedQPSolver");

      DoubleYoVariable centerOfMassHeight = (DoubleYoVariable) scs.getVariable("ProcessedSensors.comPositionz");
      DoubleYoVariable comError = (DoubleYoVariable) scs.getVariable("positionError_comHeight");

      requestedQPSolver.set(QPSolverFlavor.CQP_OASES_DIRECT);
      performCoPExploration.set(true);
      initiateMotion(standingTimeDuration, blockingSimulationRunner, walk);
      desiredSpeed.set(desiredVelocityValue);
      desiredHeading.set(desiredHeadingValue);
      scs.setMaxBufferSize(40000);
      scs.changeBufferSize(30000);

      blockingSimulationRunner.simulateAndBlock(200);

      if (Math.abs(comError.getDoubleValue()) > 0.2)
      {
         fail("comError = " + Math.abs(comError.getDoubleValue()));
      }

      // TODO: add boundingbox3d
      if (q_x.getDoubleValue() < 0.9)
      {
         fail("Robot didn't traverse the terrain. CoM_x = " + q_x.getDoubleValue());
      }

      createMovie(scs);
   }

   private Pair<CombinedTerrainObject3D, Double> createRandomBlocks()
   {
      CombinedTerrainObject3D combinedTerrainObject = new CombinedTerrainObject3D("RandomBlocks");

      Random random = new Random(1776L);
      int numberOfBoxes = 60;

      double xMin = 0.2, xMax = 1.0;
      double yMin = -1.0, yMax = 1.0;
      double maxLength = 0.4;
      double maxHeight = 0.06;

      combinedTerrainObject.addBox(xMin - 2.0, yMin - maxLength, xMax + 2.0, yMax + maxLength, -0.01, 0.0, YoAppearance.Gray());

      //random boxes
      for (int i = 0; i < numberOfBoxes; i++)
      {
         double xStart = RandomTools.generateRandomDouble(random, xMin, xMax);
         double yStart = RandomTools.generateRandomDouble(random, 0, yMax);
         double xEnd = xStart + RandomTools.generateRandomDouble(random, maxLength * 0.1, maxLength);
         double yEnd = yStart + RandomTools.generateRandomDouble(random, maxLength * 0.1, maxLength);
         double zStart = 0.0;
         double zEnd = zStart + RandomTools.generateRandomDouble(random, maxHeight * 0.1, maxHeight);
         combinedTerrainObject.addBox(xStart, yStart, xEnd, yEnd, zStart, zEnd, YoAppearance.Green());
      }

      //random horizontal bars
      double lastXEnd = 0;
      for (int i = 0; lastXEnd < xMax; i++)
      {
         double xStart = RandomTools.generateRandomDouble(random, lastXEnd, lastXEnd + 0.3);
         double xEnd = xStart + RandomTools.generateRandomDouble(random, 0.08, 0.2);
         lastXEnd = xEnd;
         double yStart = yMin;
         double yEnd = 0;
         double zStart = 0.0;
         //         double zEnd = zStart + RandomTools.generateRandomDouble(random, maxHeight * 0.1, maxHeight);
         double zEnd = zStart + 0.035;
         combinedTerrainObject.addBox(xStart, yStart, xEnd, yEnd, zStart, zEnd, YoAppearance.Green());
      }

      return new Pair<CombinedTerrainObject3D, Double>(combinedTerrainObject, xMax);
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

   private DRCFlatGroundWalkingTrack setupSimulationTrack(WalkingControllerParameters drcControlParameters, ArmControllerParameters armControllerParameters,
         GroundProfile3D groundProfile, GroundProfile3D groundProfile3D, boolean drawGroundProfile, boolean useVelocityAndHeadingScript,
         boolean cheatWithGroundHeightAtForFootstep, DRCRobotInitialSetup<SDFRobot> robotInitialSetup)
   {
      DRCGuiInitialSetup guiInitialSetup = createGUIInitialSetup();

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

      DRCFlatGroundWalkingTrack drcFlatGroundWalkingTrack = new DRCFlatGroundWalkingTrack(robotInitialSetup, guiInitialSetup, scsInitialSetup,
            useVelocityAndHeadingScript, cheatWithGroundHeightAtForFootstep, robotModel);

      SimulationConstructionSet scs = drcFlatGroundWalkingTrack.getSimulationConstructionSet();
      scs.setGroundVisible(false);
      setupCameraForUnitTest(scs);
      
      new WalkControllerSliderBoard(scs, scs.getRootRegistry());

      return drcFlatGroundWalkingTrack;
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
      cameraConfiguration.setCameraFix(0.08, -0.1, 0.035);
      cameraConfiguration.setCameraPosition(3, -1.25, 1.35);
      cameraConfiguration.setCameraTracking(true, true, true, false);
      //      cameraConfiguration.setCameraDolly(true, true, true, false);
      scs.setupCamera(cameraConfiguration);
      scs.selectCamera("testCamera");
   }
}
