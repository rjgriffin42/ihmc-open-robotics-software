package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01StartingLocation;
import us.ihmc.darpaRoboticsChallenge.DRCEnvironmentModel;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedFootstepGenerator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public class DRCObstacleCoursePlatformTest
{
   private static final boolean KEEP_SCS_UP = false;

   private static final boolean createMovie = BambooTools.doMovieCreation();
   private static final boolean checkNothingChanged = BambooTools.getCheckNothingChanged();

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
   public void testWalkingOverSmallPlatform() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.SMALL_PLATFORM;
      DRCEnvironmentModel selectedEnvironment = DRCEnvironmentModel.OBSTACLE_COURSE;
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOverSmallPlatformTest", "", selectedLocation, selectedEnvironment, checkNothingChanged, createMovie);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingOverSmallPlatform(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForSteppingOntoSmallPlatform(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0);

      if (success)
      {
         footstepDataList = createFootstepsForSteppingOffOfSmallPlatform(scriptedFootstepGenerator);
         drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0);
      }
      
      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);
      
      BambooTools.reportTestFinishedMessage();
   }
   
   @Test
   public void testWalkingOntoMediumPlatformToesTouching() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.MEDIUM_PLATFORM;
      DRCEnvironmentModel selectedEnvironment = DRCEnvironmentModel.OBSTACLE_COURSE;
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOntoMediumPlatformToesTouchingTest", "", selectedLocation, selectedEnvironment, checkNothingChanged, createMovie);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingOverMediumPlatform(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForSteppingOntoMediumPlatform(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);
      
      BambooTools.reportTestFinishedMessage();
   }
   

   @Test
   public void testWalkingOffOfMediumPlatform() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();
      
      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.ON_MEDIUM_PLATFORM;
      DRCEnvironmentModel selectedEnvironment = DRCEnvironmentModel.OBSTACLE_COURSE;
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOntoMediumPlatformToesTouchingTest", "", selectedLocation, selectedEnvironment, checkNothingChanged, createMovie);
   
      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();
      
      setupCameraForWalkingOffOfMediumPlatform(simulationConstructionSet);
      
      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      
      FootstepDataList footstepDataList = createFootstepsForSteppingOffOfMediumPlatform(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);
      
      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(4.0);
      
      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();
      
      assertTrue(success);
      
      BambooTools.reportTestFinishedMessage();
   }

   private void setupCameraForWalkingOverSmallPlatform(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(-3.0, -4.6, 0.8);
      Point3d cameraPosition = new Point3d(-11.5, -5.8, 2.5);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }
   
   private void setupCameraForWalkingOverMediumPlatform(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(-3.9, -5.6, 0.55);
      Point3d cameraPosition = new Point3d(-7.5, -2.3, 0.58);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }
   
   private void setupCameraForWalkingOffOfMediumPlatform(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(-3.9, -5.6, 0.55);
      Point3d cameraPosition = new Point3d(-7.6, -2.4, 0.58);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }


   private FootstepDataList createFootstepsForSteppingOntoSmallPlatform(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
            {{{-3.3303508964136372, -5.093152916934431, 0.2361869051765919}, {-0.003380023644676521, 0.01519186055257256, 0.9239435001894032, -0.3822122332825927}},
            {{-3.4980005080184333, -4.927710662235891, 0.23514263035532196}, {-6.366244432153206E-4, -2.2280928201561157E-4, 0.9240709626189128, -0.3822203567445069}}
            };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);
      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }

   private FootstepDataList createFootstepsForSteppingOffOfSmallPlatform(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
            {{{-3.850667406347062, -5.249955436839419, 0.08402883817600326}, {-0.0036296745847858064, 0.003867481752280881, 0.9236352342329301, -0.38323598752046323}},
            {{-3.6725725349280296, -5.446807690769805, 0.08552806597763604}, {-6.456929194763128E-5, -0.01561897825296648, 0.9234986484659182, -0.3832835629540643}}
            };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.RIGHT, footstepLocationsAndOrientations.length);
      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }
   
   
   private FootstepDataList createFootstepsForSteppingOntoMediumPlatform(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
            {{{-4.144889177599215, -5.68009276450442, 0.2841471307289875}, {-0.012979910123161926, 0.017759854548746876, 0.9232071519598507, -0.3836726001029824}},
            {{-3.997325285359919, -5.8527640256176685, 0.2926905844610473}, {-0.022159348866436335, -0.014031420240348416, 0.9230263369316307, -0.3838417171627259}}
            };
      
      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.RIGHT, footstepLocationsAndOrientations.length);
      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }
   
   private FootstepDataList createFootstepsForSteppingOffOfMediumPlatform(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
            {{{-4.304392715667327, -6.084498586699763, 0.08716704456087025}, {-0.0042976203878775715, -0.010722204803598987, 0.9248070170408506, -0.38026115501738456}},
            {{-4.4394706079327255, -5.9465856725464565, 0.08586305720146342}, {-8.975861226689934E-4, 0.002016837110644428, 0.9248918980282926, -0.380223754740342}},
            };
      
      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.LEFT, footstepLocationsAndOrientations.length);
      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }
 

}
