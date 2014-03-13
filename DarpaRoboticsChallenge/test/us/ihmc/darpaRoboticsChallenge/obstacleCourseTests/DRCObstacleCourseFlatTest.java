package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.SimulationDoneCriterion;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.packets.ComHeightPacket;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01StartingLocation;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedFootstepGenerator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import static org.junit.Assert.assertTrue;

public class DRCObstacleCourseFlatTest
{
   private static final boolean KEEP_SCS_UP = false;
   private static final DRCRobotModel robotModel = DRCRobotModel.ATLAS_NO_HANDS_ADDED_MASS;

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

   @Ignore("Invoked manually to test memory & thread leaks")
   @Test
   public void testForMemoryLeaks() throws Exception
   {
      for (int i = 0; i < 10; i++)
      {
         showMemoryUsageBeforeTest();
         testStandingForACoupleSeconds();
         destroySimulationAndRecycleMemory();
      }
   }

   @Test
   public void testStandingForACoupleSeconds() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCStandingTest", "", selectedLocation, checkNothingChanged, showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.1);    // 2.0);

      ThreadTools.sleep(2000);


      // drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testSimpleFlatGroundScript() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      String scriptName = "scripts/ExerciseAndJUnitScripts/SimpleFlatGroundScript.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(this.getClass(), scriptName);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSimpleFlatGroundScriptTest", fileName, selectedLocation,
              checkNothingChanged, createMovie, false, robotModel);
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(20.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testSimpleFlatGroundScriptWithRandomFootSlip() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      String scriptName = "scripts/ExerciseAndJUnitScripts/SimpleFlatGroundScript.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(this.getClass(), scriptName);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSimpleFlatGroundScriptTest", fileName, selectedLocation, checkNothingChanged, createMovie, false, robotModel);
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      SDFRobot robot = drcSimulationTestHelper.getRobot();
      setupCameraForWalkingUpToRamp(simulationConstructionSet);
      SlipRandomOnNextStepPerturber slipRandomOnEachStepPerturber = new SlipRandomOnNextStepPerturber(robot, 1002L);
      slipRandomOnEachStepPerturber.setTranslationRangeToSlipNextStep(new double[]{0.01, 0.01, 0.0}, new double[]{0.07, 0.07, 0.005});
      slipRandomOnEachStepPerturber.setRotationRangeToSlipNextStep(new double[]{0.03, 0.0, 0.0}, new double[]{0.4, 0.0, 0.0});
      slipRandomOnEachStepPerturber.setSlipAfterStepTimeDeltaRange(0.005, 0.5);
      slipRandomOnEachStepPerturber.setSlipPercentSlipPerTickRange(0.005, 0.5);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.0);

      robot.setController(slipRandomOnEachStepPerturber, 10);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.5);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(20.0);


      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testLongStepsMaxHeightPauseAndResume() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      String scriptName = "scripts/ExerciseAndJUnitScripts/LongStepsMaxHeightPauseAndRestart_LeftFootTest.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(this.getClass(), scriptName);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCLongStepsMaxHeightPauseAndRestartTest", fileName, selectedLocation,
              checkNothingChanged, createMovie, false, robotModel);
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(20.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }


   @Test
   public void testSideStepsWithSlipping() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      String scriptName = "scripts/ExerciseAndJUnitScripts/LongSideStepsLeft.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(this.getClass(), scriptName);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSideStepsWithSlippingTest", fileName, selectedLocation,
              checkNothingChanged, createMovie, false, robotModel);

      SDFRobot robot = drcSimulationTestHelper.getRobot();

      SlipOnNextStepPerturber slipOnEachStepPerturber = new SlipOnNextStepPerturber(robot, RobotSide.LEFT);
      slipOnEachStepPerturber.setAmountToSlipNextStep(new Vector3d(0.06, -0.06, 0.0));
      slipOnEachStepPerturber.setRotationToSlipNextStep(-0.15, 0.0, 0.0);
      slipOnEachStepPerturber.setSlipAfterStepTimeDelta(0.05);
      slipOnEachStepPerturber.setPercentToSlipPerTick(0.1);
      robot.setController(slipOnEachStepPerturber, 10);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForSideStepSlipping(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);

      slipOnEachStepPerturber.setSlipNextStep(true);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(14.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testSideStepsWithRandomSlipping() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      String scriptName = "scripts/ExerciseAndJUnitScripts/LongSideStepsLeft.xml";
      String fileName = BambooTools.getFullFilenameUsingClassRelativeURL(this.getClass(), scriptName);
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCSideStepsWithSlippingTest", fileName, selectedLocation,
              checkNothingChanged, createMovie, false, robotModel);

      SDFRobot robot = drcSimulationTestHelper.getRobot();

      SlipRandomOnNextStepPerturber slipRandomOnEachStepPerturber = new SlipRandomOnNextStepPerturber(robot, 1000L);
      slipRandomOnEachStepPerturber.setTranslationRangeToSlipNextStep(new double[]{0.0, 0.0, 0.0}, new double[]{0.04, 0.04, 0.01});
      slipRandomOnEachStepPerturber.setRotationRangeToSlipNextStep(new double[]{0.0, 0.0, 0.0}, new double[]{0.2, 0.05, 0.02});
      slipRandomOnEachStepPerturber.setSlipAfterStepTimeDeltaRange(0.01, 1.0);
      slipRandomOnEachStepPerturber.setSlipPercentSlipPerTickRange(0.02, 1.0);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.0);
      
      robot.setController(slipRandomOnEachStepPerturber, 10);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      setupCameraForSideStepSlipping(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(0.5);
      slipRandomOnEachStepPerturber.setProbabilityOfSlip(0.5);
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(14.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }



   @Test
   public void testStandingOnUnevenTerrainForACoupleSeconds() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.TOP_OF_SLOPES;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCStandingTest", "", selectedLocation, checkNothingChanged, createMovie,
              true, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();

      Point3d cameraFix = new Point3d(3.25, 3.25, 1.02);
      Point3d cameraPosition = new Point3d(6.35, 0.18, 0.97);
      drcSimulationTestHelper.setupCameraForUnitTest(simulationConstructionSet, cameraFix, cameraPosition);

      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(10.0);


      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   // @Test
   // public void testMemoryStuff()
   // {
   // for (int i=0; i<3; i++)
   // {
   // System.gc();
   // System.runFinalization();
   // ThreadTools.sleep(1000);
   //
   // System.out.println("Sleeping Forever");
   // ThreadTools.sleepForever();
   // }
   // }

   
   @Ignore("Added for fixing DRC-866. Does not work for fast walking")
   @Test
   public void testRotatedStepInTheAir() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCRotatedStepsInTheAirTest", "", selectedLocation, checkNothingChanged,
              showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForRotatedStepInTheAir(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(8.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }
   
   @Test
   public void testWalkingUpToRampWithShortSteps() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingUpToRampShortStepsTest", "", selectedLocation, checkNothingChanged,
            showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForWalkingUpToRampShortSteps(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(15.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testWalkingUpToRampWithLongSteps() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingUpToRampLongStepsTest", "", selectedLocation, checkNothingChanged,
            showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForWalkingOnFlatLongSteps(scriptedFootstepGenerator);

      // FootstepDataList footstepDataList = createFootstepsForTwoLongFlatSteps(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(15.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testWalkingUpToRampWithLongStepsAndOccasionallyStraightKnees() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOccasionallyStraightKneesTest", "", selectedLocation, checkNothingChanged,
            showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingUpToRamp(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForWalkingOnFlatLongSteps(scriptedFootstepGenerator);

      // FootstepDataList footstepDataList = createFootstepsForTwoLongFlatSteps(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);
      drcSimulationTestHelper.sendComHeightPacketToListeners(new ComHeightPacket(0.08));


      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(15.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   @Test
   public void testTurningInPlaceAndPassingPI() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.DEFAULT_BUT_ALMOST_PI;

      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCTurningInPlaceAndPassingPITest", "", selectedLocation,
            checkNothingChanged, showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForTurningInPlaceAndPassingPI(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForTurningInPlaceAndPassingPI(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);


      final DoubleYoVariable pelvisOrientationError =
         (DoubleYoVariable) simulationConstructionSet.getVariable(
             "WalkingHighLevelHumanoidController.RootJointAngularAccelerationControlModule.pelvisAxisAngleOrientationController",
             "pelvisOrientationErrorMagnitude");

      SimulationDoneCriterion checkPelvisOrientationError = new SimulationDoneCriterion()
      {
         @Override
         public boolean isSimulationDone()
         {
            return (Math.abs(pelvisOrientationError.getDoubleValue()) > 0.3);
         }
      };

      simulationConstructionSet.setSimulateDoneCriterion(checkPelvisOrientationError);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(12.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }


   private void setupCameraForWalkingUpToRamp(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(1.8375, -0.16, 0.89);
      Point3d cameraPosition = new Point3d(1.10, 8.30, 1.37);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private void setupCameraForSideStepSlipping(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(2.0, 0.4, 0.75);
      Point3d cameraPosition = new Point3d(6.5, 0.4, 0.75);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private void setupCameraForTurningInPlaceAndPassingPI(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(0.036, 0.0, 0.89);
      Point3d cameraPosition = new Point3d(-7, -0.3575, 1.276);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }


   private FootstepDataList createFootstepsForRotatedStepInTheAir(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
            {
            {
               {0.4, 0.10, 0.28},
               {0.6, 0.0, 0.0, 0.8}
            },
            {
               {0.48, -0.10329823409587219, 0.08400000000000005},
               {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
            }
            };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);

      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }


   private FootstepDataList createFootstepsForWalkingUpToRampShortSteps(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {
         {
            {0.2148448504580547, -0.09930268518393547, 0.08399999999999999},
            {3.405174677589428E-21, -6.767715309751755E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {0.4481532647842352, 0.10329823409587219, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {0.6834821762408051, -0.09551979778612019, 0.08399999999999999},
            {3.405174677589428E-21, -6.767715309751755E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {0.9167977582017036, 0.10565710343022289, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {1.1521266696582735, -0.09316092845176947, 0.08399999999999999},
            {3.405174677589428E-21, -6.767715309751755E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {1.385442251619172, 0.1080159727645736, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {1.620771163075742, -0.09080205911741877, 0.08399999999999999},
            {3.405174677589428E-21, -6.767715309751755E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {1.8540867450366407, 0.11037484209892431, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {2.0894156564932107, -0.08844318978306806, 0.08399999999999999},
            {3.405174677589428E-21, -6.767715309751755E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {2.322731238454109, 0.11273371143327501, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {2.558060149910679, -0.08608432044871735, 0.08398952447640476},
            {-5.047008501650524E-21, 4.53358964226292E-22, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {2.7913757318715775, 0.11509258076762573, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {3.0267046433281477, -0.08372545111436663, 0.08398952447640476},
            {-6.38257081820882E-21, -2.5377866560433405E-20, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {3.260020225289046, 0.11745145010197644, 0.08400000000000005},
            {-1.705361817083927E-23, 6.776242118837171E-21, 0.0025166698394258787, 0.9999968331814453}
         },
         {
            {3.2610268900368817, -0.08254601644719128, 0.08398952447640476},
            {3.49577202412201E-21, 2.923107094657073E-20, 0.0025166698394258787, 0.9999968331814453}
         }
      };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.RIGHT, footstepLocationsAndOrientations.length);

      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }

   private FootstepDataList createFootstepsForWalkingOnFlatLongSteps(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {
         {
            {0.5909646234016005, 0.10243127081250579, 0.08400000000000002},
            {3.5805394102331502E-22, -1.0841962601668662E-19, 0.003302464707320093, 0.99999454684856}
         },
         {
            {1.212701966120992, -0.09394691394679651, 0.084}, {1.0806157207566333E-19, 1.0877767995770995E-19, 0.0033024647073200924, 0.99999454684856}
         },
         {
            {1.8317941784239657, 0.11014657591704705, 0.08619322927296164},
            {8.190550851520344E-19, 1.5693991726842814E-18, 0.003302464707320093, 0.99999454684856}
         },
         {
            {2.4535283480857237, -0.08575120920059497, 0.08069788195751608},
            {-2.202407644730947E-19, -8.117149793610565E-19, 0.0033024647073200924, 0.99999454684856}
         },
         {
            {3.073148474156348, 0.11833676240086898, 0.08590468550531082},
            {4.322378465953267E-5, 0.003142233766871708, 0.0033022799833692306, 0.9999896096688056}
         },
         {
            {3.0729346702590505, -0.0816428320664241, 0.0812390388356}, {-8.243740658642556E-5, -0.005993134849034999, 0.003301792738040525, 0.999976586577641}
         }
      };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);

      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }


   private FootstepDataList createFootstepsForTurningInPlaceAndPassingPI(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {
         {
            {0.053884346896697966, 0.19273164589134978, 0.08574185103923426},
            {-6.938862977443471E-11, -8.7126898825953E-11, 0.9990480941331229, 0.04362230632342559}
         },
         {
            {0.05388201845443364, -0.20574623329424319, 0.08574185073944539},
            {1.6604742582112774E-10, 1.4170466407843545E-10, 0.9990483490180827, -0.04361646849807009}
         },
         {
            {0.0017235494647287533, 0.19045456181341558, 0.08574185040535603},
            {-5.0383363690493444E-11, -1.0843741493223105E-10, 0.9961949527487116, -0.0871528319562377}
         },
         {
            {0.10485496441611886, -0.19444611557725083, 0.08574185102571344},
            {1.5201027889830733E-10, 1.4860298371617872E-10, 0.9848082603764649, -0.1736453002366632}
         },
         {
            {-0.04807055917333275, 0.17475485972777594, 0.08574185070322422},
            {-3.05160242173266E-11, -1.2789253687750615E-10, 0.976296639469044, -0.21643676157587363}
         },
         {
            {0.15116636401480588, -0.17033827066486662, 0.08574185038049925},
            {1.3537219389951473E-10, 1.5295866511692108E-10, 0.9537178292633579, -0.3007030131960579}
         },
         {
            {-0.09210459251806524, 0.14670244796138915, 0.08574185100111767},
            {-1.0126547178247246E-11, -1.4515938198837407E-10, 0.9396936200915386, -0.3420173977435346}
         },
         {
            {0.18966017152321202, -0.13506560904726644, 0.0857418506668508},
            {1.1641785319333712E-10, 1.5469718133894557E-10, 0.9063090217942931, -0.42261561378428936}
         },
         {
            {-0.12737770450507258, 0.10820905279560836, 0.08574185036731347},
            {-1.0436197890210116E-11, 1.599425098341044E-10, -0.8870121823838428, 0.46174602142590504}
         },
         {
            {0.21771309767509972, -0.09103190305599193, 0.08574185095383173},
            {-9.547157074708167E-11, -1.5378878590499154E-10, -0.8433930157263759, 0.5372971440683164}
         },
         {
            {-0.15148609051286105, 0.061897935068802395, 0.085741850664082},
            {-3.082037679075772E-11, 1.7198897704022203E-10, -0.8191537200820054, 0.573574043063153}
         },
         {
            {0.23341338156809216, -0.0412379781596809, 0.08574185031046283},
            {-7.289174317616828E-11, -1.5024902169235376E-10, -0.7660463210652019, 0.6427853716307409}
         },
         {
            {-0.16278680351003783, 0.010925120900156002, 0.08574185095977704},
            {-5.067721042808702E-11, 1.8109266512133938E-10, -0.7372793107685568, 0.6755880534117237}
         },
         {
            {0.23569107567555475, 0.010922792383292988, 0.08574185059966628},
            {-4.906471775149843E-11, -1.441384550795834E-10, -0.6755923617465286, 0.7372753629070672}
         },
         {
            {-0.1605097194824509, -0.0412356760683499, 0.08574185032282551},
            {-6.966694301257708E-11, 1.87097807520974E-10, -0.6427898477118872, 0.766042565187163}
         },
         {
            {0.20979454839765582, 0.013396779318557463, 0.08574185088931394},
            {-2.91671807071375E-11, -1.3694134194254838E-10, -0.5937694707170026, 0.804635206565342}
         },
         {
            {-0.0496373406094997, -0.06666317759167362, 0.08574185062507425},
            {-7.826318574113734E-11, 1.8865011916447275E-10, -0.5937694705296589, 0.8046352067035897}
         }
      };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.RIGHT, footstepLocationsAndOrientations.length);

      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }
}
