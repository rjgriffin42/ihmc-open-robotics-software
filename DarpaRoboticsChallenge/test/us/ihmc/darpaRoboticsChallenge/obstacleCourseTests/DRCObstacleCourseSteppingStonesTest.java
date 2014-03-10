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
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedFootstepGenerator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public class DRCObstacleCourseSteppingStonesTest
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

   
   @Test
   public void testWalkingOverEasySteppingStones() throws SimulationExceededMaximumTimeException
   {
      try
      {
      BambooTools.reportTestStartedMessage();

      DRCDemo01StartingLocation selectedLocation = DRCDemo01StartingLocation.EASY_STEPPING_STONES;
      DRCEnvironmentModel selectedEnvironment = DRCEnvironmentModel.OBSTACLE_COURSE;
      drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingEasySteppingStonesTest", "", selectedLocation, selectedEnvironment, checkNothingChanged, showGUI, createMovie, robotModel);

      SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForWalkingOverEasySteppingStones(simulationConstructionSet);

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      FootstepDataList footstepDataList = createFootstepsForWalkingOverEasySteppingStones(scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(13.0);

      drcSimulationTestHelper.createMovie(simulationConstructionSet, 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);
      
      BambooTools.reportTestFinishedMessage();
      }
      catch (Throwable throwable)
      {
         System.err.println("Caught throwable in testWalkingOverEasySteppingStones: " + throwable);
         System.err.flush();
         throw throwable;
      }
   }
   
 
   private void setupCameraForWalkingOverEasySteppingStones(SimulationConstructionSet scs)
   {
      Point3d cameraFix = new Point3d(-8.6, -0.1, 0.94);
      Point3d cameraPosition = new Point3d(-14.0, -5.0, 2.7);

      drcSimulationTestHelper.setupCameraForUnitTest(scs, cameraFix, cameraPosition);
   }

   private FootstepDataList createFootstepsForWalkingOverEasySteppingStones(ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {{{-7.72847174992541, -0.5619736174919732, 0.3839138258635628}, {-0.002564106649548222, 9.218543591576633E-4, 0.9999871158757672, 0.004282945726398341}},
         {{-8.233931300168681, -0.952122284180518, 0.3841921077973934}, {-2.649132161393031E-6, -0.00302400231893713, 0.999986265693845, 0.004280633905867881}},
         {{-8.711157422190857, -0.5634436272430561, 0.38340964898482055}, {-6.333967334144636E-4, -0.002689012266100874, 0.9999870292977306, 0.004278931865605645}},
         {{-9.246614388340875, -0.9823725639340232, 0.3838760717826556}, {4.990380502353344E-4, 0.002867206806117212, 0.9999866091454905, 0.00427920738681889}},
         {{-9.694460236661355, -0.5363354293129117, 0.3828438933446154}, {0.0043663633816866795, 6.575433167622114E-4, 0.9999811020260976, 0.004277627645902338}},
         {{-10.204483462540168, -1.0007498263499959, 0.3841142603691748}, {3.379337850421112E-4, 0.0013510800402890615, 0.9999898702179759, 0.004280168795429233}},
         {{-10.20677294790819, -0.6741336761434962, 0.3829201197142793}, {0.004772284224629501, 0.005592011887113724, 0.9999639290557834, 0.004253856327364576}}
         };
      
      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(RobotSide.RIGHT, footstepLocationsAndOrientations.length);
      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }

}
