package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01StartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedFootstepGenerator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public abstract class DRCObstacleCourseTrialsTerrainTest implements MultiRobotTestInterface
{
private Class thisClass = DRCObstacleCourseTrialsTerrainTest.class;
   private static final boolean KEEP_SCS_UP = false;

   private static final boolean createMovie = BambooTools.doMovieCreation();
   private static final boolean checkNothingChanged = BambooTools.getCheckNothingChanged();
   private static final boolean showGUI = KEEP_SCS_UP || createMovie;

   private DRCSimulationTestHelper drcSimulationTestHelper;

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
      if (drcSimulationTestHelper != null)
      {
         drcSimulationTestHelper.destroySimulation();
         drcSimulationTestHelper = null;
      }

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }



   @Test
   public void testTrialsTerrainSlopeScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsSlopeLeftFootPose.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DRC_TRIALS_TRAINING_WALKING;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSlopeTest", fileName, selectedLocation, checkNothingChanged, createMovie,
              false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOntoSlopes(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(30.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTrialsTerrainSlopeScriptRandomFootSlip() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsSlopeLeftFootPose.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DRC_TRIALS_TRAINING_WALKING;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSlopeTest", fileName, selectedLocation, checkNothingChanged, createMovie,
              false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOntoSlopes(simulationConstructionSet);

      SDFRobot robot = drcSimulationTestHelper.getRobot();
      SlipRandomOnNextStepPerturber slipRandomOnEachStepPerturber = new SlipRandomOnNextStepPerturber(robot, 1201L);
      slipRandomOnEachStepPerturber.setTranslationRangeToSlipNextStep(new double[]{0.03, 0.03, 0.0}, new double[]{0.05, 0.05, 0.005});
      slipRandomOnEachStepPerturber.setRotationRangeToSlipNextStep(new double[]{0.02, 0.01, 0.0}, new double[]{0.2, 0.05, 0.01});
      slipRandomOnEachStepPerturber.setSlipAfterStepTimeDeltaRange(0.01, 0.5);
      slipRandomOnEachStepPerturber.setSlipPercentSlipPerTickRange(0.01, 0.03);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.0);
      robot.setController(slipRandomOnEachStepPerturber, 10);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.6);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(29.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }


   @Test
   public void testTrialsTerrainZigzagHurdlesScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsZigzagHurdlesLeftFootPose.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_ZIGZAG_BLOCKS;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCZigzagHurdlesTest", fileName, selectedLocation, checkNothingChanged,
              createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverHurdles(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(15.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }
   
   @Test
   public void testTrialsTerrainZigzagHurdlesScriptRandomFootSlip() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsZigzagHurdlesLeftFootPose.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_ZIGZAG_BLOCKS;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCZigzagHurdlesTest", fileName, selectedLocation, checkNothingChanged,
              createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverHurdles(simulationConstructionSet);
      
      
      SDFRobot robot = drcSimulationTestHelper.getRobot();
      SlipRandomOnNextStepPerturber slipRandomOnEachStepPerturber = new SlipRandomOnNextStepPerturber(robot, 1201L);
      slipRandomOnEachStepPerturber.setTranslationRangeToSlipNextStep(new double[]{0.04, 0.04, 0.0}, new double[]{0.06, 0.08, 0.005});
      slipRandomOnEachStepPerturber.setRotationRangeToSlipNextStep(new double[]{0.0, 0.0, 0.0}, new double[]{0.2, 0.05, 0.02});
      slipRandomOnEachStepPerturber.setSlipAfterStepTimeDeltaRange(0.01, 0.5);
      slipRandomOnEachStepPerturber.setSlipPercentSlipPerTickRange(0.01, 0.03);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.0);
      robot.setController(slipRandomOnEachStepPerturber, 10);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(1.0);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(14.0);
      
      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTrialsTerrainCinderblockFieldPartOneScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsCinderblockFieldPartOneLeftFootPose.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_CINDERBLOCK_FIELD;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCCinderblockFieldPartOneTest", fileName, selectedLocation,
              checkNothingChanged, createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverFlatCinderblockField(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(40.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTrialsTerrainCinderblockFieldPartTwoScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsCinderblockFieldPartTwoLeftFootPose.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_SLANTED_CINDERBLOCK_FIELD;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCCinderblockfieldPartTwoTest", fileName, selectedLocation,
              checkNothingChanged, createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverSlantedCinderblockField(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(40.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTrialsTerrainUpFlatCinderblocksScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsUpFlatCinderblocks.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_CINDERBLOCK_FIELD;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCFlatCinderblockTest", fileName, selectedLocation, checkNothingChanged,
              createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverFlatCinderblockField(simulationConstructionSet);

      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(25.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();
//ThreadTools.sleepForever();
      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTrialsTerrainUpSlantedCinderblocksScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsUpSlantedCinderblocks.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_SLANTED_CINDERBLOCK_FIELD;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSlantedCinderblockTest", fileName, selectedLocation, checkNothingChanged,
              createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverSlantedCinderblockField(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(25.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTrialsTerrainCinderblockEntireFieldScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String scriptName = "scripts/ExerciseAndJUnitScripts/DRCTrialsCinderblockFieldBoth.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(thisClass, scriptName);
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.IN_FRONT_OF_CINDERBLOCK_FIELD;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSlantedCinderblockTest", fileName, selectedLocation, checkNothingChanged,
              createMovie, false, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingOverCinderblockField(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(50.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }




/*   // Is now a scripted test
   @Test
   public void testWalkingOntoAndOverSlopes() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DRC_TRIALS_TRAINING_WALKING;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOntoSlopesTest", "", selectedLocation, checkNothingChanged,
              createMovie);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingOntoSlopes(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForWalkingToTheSlopesNormally(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(6.5);

      if (success)
      {
         footstepDataList = createFootstepsForSteppingOverTheSlopeEdge(scriptedFootstepGenerator);
         drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(6.0);
      }

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }
*/
   @Test
   public void testWalkingOntoAndOverSlopesSideways() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DRC_TRIALS_TRAINING_WALKING;
      
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOntoSlopesTest", "", selectedLocation, checkNothingChanged,
            showGUI, createMovie, getRobotModel());

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingOntoSlopes(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForWalkingToTheSlopesSideways(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(20.0);

      if (success)
      {
         footstepDataList = createFootstepsForSteppingOverTheSlopesEdgeSideways(scriptedFootstepGenerator);
         drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(8.0);
      }

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   private void setupCameraForWalkingOntoSlopes(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(3.6214, 2.5418, 0.5);
      Point3d cameraPosition = new Point3d(6.6816, -0.5441, 1.5);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private void setupCameraForWalkingOverHurdles(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(4.9246, 4.0338, 0.5);
      Point3d cameraPosition = new Point3d(8.1885, 1.1641, 1.5);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private void setupCameraForWalkingOverCinderblockField(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(7.8655, 6.8947, 0.5);
      Point3d cameraPosition = new Point3d(10.2989, 18.7661, 3.2746);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private void setupCameraForWalkingOverSlantedCinderblockField(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(9.7689, 9.0724, 0.5);
      Point3d cameraPosition = new Point3d(8.0254, 16.6036, 2.5378);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private void setupCameraForWalkingOverFlatCinderblockField(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(7.447, 7.0966, 0.5);
      Point3d cameraPosition = new Point3d(6.3809, 14.6839, 2.7821);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   /*
    *    // Is now a scripted test
    * private FootstepDataList createFootstepsForWalkingToTheSlopesNormally(ScriptedFootstepGenerator scriptedFootstepGenerator)
    * {
    *  double[][][] footstepLocationsAndOrientations = new double[][][]
    *  {
    *     {
    *        {2.1265936534673218, 2.3428927767215417, 0.08591277243384399}, {0.020704851858501554, -0.008524153124099326, 0.3678797475257915, 0.9296037539099088}
    *     },
    *     {
    *        {2.6166814909099094, 2.3996906331962657, 0.08226766576256324},
    *        {2.4508705508237736E-4, 7.204771499494282E-4, 0.35318402355492856, 0.9355535614546948}
    *     },
    *     {
    *        {2.739624824329704, 2.8475642035481017, 0.09205292111010316}, {0.03188071876413438, -0.038174623620904354, 0.3675020633742088, 0.928691849484092}
    *     },
    *     {
    *        {3.193896638477976, 2.9213552160981826, 0.17477524869701494}, {0.04675359881912196, -0.116811005963886, 0.34844159401254376, 0.9288475361678918}
    *     },
    *     {
    *        {3.017845920846812, 3.1060500699939313, 0.17155859233956158}, {0.03567318375247706, -0.13777549770429312, 0.36045274233562125, 0.9218563644820306}
    *     },
    *  };
    *
    *  RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);
    *
    *  return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
    * }
    *
    * private FootstepDataList createFootstepsForSteppingOverTheSlopeEdge(ScriptedFootstepGenerator scriptedFootstepGenerator)
    * {
    *  double[][][] footstepLocationsAndOrientations = new double[][][]
    *  {
    *     {
    *        {3.0196365393148823, 3.157527589669437, 0.18463968610481485}, {0.06878939191058485, -0.14859257123868208, 0.36996761202767287, 0.9145010844082091}
    *     },
    *     {
    *        {3.242221271901224, 2.9525762835818927, 0.19170633363319947}, {0.05074043948683175, -0.1388048392204765, 0.35405122373484255, 0.9234751514694487}
    *     },
    *     {
    *        {3.2884777064719466, 3.4423998984979947, 0.21600585958795815}, {-0.09042178480364092, 0.12404908940997492, 0.3587959323612516, 0.9207069040528045}
    *     },
    *     {
    *        {3.504460942426418, 3.2186466197482773, 0.21310781108121274}, {-0.10127177343643246, 0.10226561231768722, 0.3474545483212657, 0.9265857268991324}
    *     },
    *  };
    *
    *  RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);
    *
    *  return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
    * }
    */

   private FootstepDataList createFootstepsForWalkingToTheSlopesSideways(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {
         {
            {1.9823966635641284, 2.2652470283072947, 0.0837307038087527}, {0.020640132711869014, -0.008498293160241518, 0.3083105864353867, 0.951023841040206}
         },
         {
            {2.1498164650868086, 1.9068093446433945, 0.08347777099109424},
            {0.0017662883179677942, 0.006293715779592783, 0.10348136532245923, 0.9946099116730455}
         },
         {
            {2.0874687872550273, 2.298441155960018, 0.08427730698998047},
            {0.018633535386299895, -0.007684905338928583, 0.024333507423710935, 0.9995006823436388}
         },
         {
            {2.0314602664501606, 1.9146192377173497, 0.08341613830944179},
            {0.0034096687293227626, 0.011999890346433685, -0.18430794544823617, 0.9827893762325068}
         },
         {
            {2.2034072670888567, 2.281143757931813, 0.08394437770346314}, {0.015080397973109637, -0.006233688252807148, -0.26166319609415833, 0.96502129227159}
         },
         {
            {2.01915833306869, 2.0207468649842344, 0.08359962278744236}, {0.004579288707518639, 0.014818193944121557, -0.36640082496031345, 0.9303278382976451}
         },
         {
            {2.3514127370083746, 2.3597369501954506, 0.0860154559910794},
            {0.013290279881016632, -0.005689963592658761, -0.38133174097309863, 0.9243252112224485}
         },
         {
            {2.25188495618737, 2.254080704383737, 0.08394252424978803}, {-0.007521908318387368, -0.014514380020142312, -0.36682257924428485, 0.9301472727608521}
         },
         {
            {2.5571508061806436, 2.575341468058366, 0.08684300869106024}, {0.01329027988101649, -0.005689963592658824, -0.3813317409730987, 0.9243252112224485}
         },
         {
            {2.457577639970055, 2.4696407711863455, 0.0869664911152282}, {0.004579288707518356, 0.014818193944121443, -0.3664008249603138, 0.930327838297645}
         },
         {
            {2.768315526189699, 2.785782484898131, 0.08433104034516861}, {0.013290279881016347, -0.0056899635926588865, -0.3813317409730988, 0.9243252112224485}
         },
         {
            {2.5635712023164903, 2.5743143400835953, 0.0870408732178602}, {0.0045792887075182115, 0.014818193944121382, -0.36640082496031384, 0.930327838297645}
         },
         {
            {2.885879020571682, 2.856576938293644, 0.10386656274046036}, {0.12558777759117537, -0.06679059297300634, -0.3513269921130159, 0.92538428310775}
         },
         {
            {2.7488021282402655, 2.6966826406653803, 0.08370236609247309},
            {-0.002071973823236298, -0.003865630015040666, -0.34064306248913384, 0.9401824651667817}
         },
         {
            {2.986089110278983, 2.9635615047422394, 0.14058922355834996}, {0.10717475880427008, -0.026653519731933816, -0.35290983266496895, 0.9291166831832962}
         },
         {
            {2.844078254699551, 2.7987722839957434, 0.08895082990782543}, {0.05682759834686232, -0.026260994395297752, -0.34066061044879653, 0.9380998522162506}
         },
         {
            {3.079930510367088, 3.075738188913338, 0.18223629720937254}, {0.09637509142099876, -0.05639328437007134, -0.3547113886462364, 0.9282841536922889}
         },
         {
            {2.9344673839516484, 2.905450415197158, 0.12337655475135587}, {0.11580121216799653, -0.04880027856780968, -0.33742432635314157, 0.9329273476842961}
         },
         {
            {3.128623440850548, 3.133453453117145, 0.20285914961446738}, {0.12598064593469932, -0.06710112170905909, -0.35316275861517443, 0.9246093133008028}
         },
      };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);

      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }

   private FootstepDataList createFootstepsForSteppingOverTheSlopesEdgeSideways(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {
         {
            {3.0166188930803033, 3.0119111124382747, 0.15483943760187868}, {0.1089192633351566, -0.024058516458074313, -0.36480741929991994, 0.9243772653435911}
         },
         {
            {3.285102645187515, 3.361157755301027, 0.22549038617963604}, {-0.12112164390947337, 0.0472878892769468, -0.34692969244751093, 0.9288343185965263}
         },
         {
            {3.1055260564624887, 3.160607633126951, 0.20546113718754253}, {0.12642092274810612, -0.06414867390038669, -0.3595546575929555, 0.9222923322523894}
         },
         {
            {3.3695983984590763, 3.4737424555716165, 0.18833480541902758}, {-0.12112164390947357, 0.04728788927694677, -0.3469296924475111, 0.9288343185965262}
         },
         {
            {3.2811041904535196, 3.3537632182460775, 0.22458026669614373}, {-0.11259311979747, 0.025105614756717354, -0.36036496334456175, 0.9256509010829258}
         },
      };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.RIGHT, footstepLocationsAndOrientations.length);

      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }

}
