package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedFootstepGenerator;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.BoundingBox3d;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

   public abstract class DRCObstacleCourseStandingYawedTest implements MultiRobotTestInterface
   {
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

     
      
      @Test(timeout=300000)
      public void testStandingYawed() throws SimulationExceededMaximumTimeException
      {
         BambooTools.reportTestStartedMessage();

         DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.ROCKS;
         
         drcSimulationTestHelper = new DRCSimulationTestHelper("DRCWalkingOntoRocksTest", "", selectedLocation, checkNothingChanged, showGUI, createMovie, getRobotModel());

         SimulationConstructionSet simulationConstructionSet = drcSimulationTestHelper.getSimulationConstructionSet();
         ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

         setupCameraForWalkingOntoRocks(simulationConstructionSet);

         ThreadTools.sleep(1000);
         boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);
         
         drcSimulationTestHelper.createMovie(getSimpleRobotName(), 1);
         drcSimulationTestHelper.checkNothingChanged();

         assertTrue(success);
         
         Point3d center = new Point3d(-2.179104505087052E-6, 2.050336483387291, 0.7874231497270643); 
         Vector3d plusMinusVector = new Vector3d(0.2, 0.2, 0.5);
         BoundingBox3d boundingBox = BoundingBox3d.createUsingCenterAndPlusMinusVector(center, plusMinusVector);
         drcSimulationTestHelper.assertRobotsRootJointIsInBoundingBox(boundingBox);

         
         BambooTools.reportTestFinishedMessage();
      }
      
      
      private void setupCameraForWalkingOntoRocks(SimulationConstructionSet scs)
      {
         Point3d cameraFix = new Point3d(0.1, 3.2, 0.5);
         Point3d cameraPosition = new Point3d(-2.8, 4.8, 1.5);

         drcSimulationTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
      }

   }
