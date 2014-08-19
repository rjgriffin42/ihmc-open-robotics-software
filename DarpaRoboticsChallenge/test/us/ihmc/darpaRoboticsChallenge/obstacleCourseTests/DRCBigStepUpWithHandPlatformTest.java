package us.ihmc.darpaRoboticsChallenge.obstacleCourseTests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Handstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.packets.HandstepPacket;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.MultiRobotTestInterface;
import us.ihmc.darpaRoboticsChallenge.environment.BigStepUpWithHandPlatformEnvironment;
import us.ihmc.darpaRoboticsChallenge.testTools.DRCSimulationTestHelper;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedFootstepGenerator;
import us.ihmc.darpaRoboticsChallenge.testTools.ScriptedHandstepGenerator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.MemoryTools;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public abstract class DRCBigStepUpWithHandPlatformTest implements MultiRobotTestInterface
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

   
   @Test
   public void testBigStepUpWithHandPlatform() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      double stepHeight = 0.2;

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      BigStepUpWithHandPlatformEnvironment environment = new BigStepUpWithHandPlatformEnvironment(stepHeight);
      drcSimulationTestHelper = new DRCSimulationTestHelper(environment.getTerrainObject3D(), "testBigStepUpWithHandPlatform", "", selectedLocation, checkNothingChanged,
            showGUI, createMovie, getRobotModel());

      ScriptedFootstepGenerator scriptedFootstepGenerator = drcSimulationTestHelper.createScriptedFootstepGenerator();

      setupCameraForBigStepUpWithHandPlatform();

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);

      ScriptedHandstepGenerator scriptedHandstepGenerator = drcSimulationTestHelper.createScriptedHandstepGenerator();

      double leftHandstepY = 0.4;
      double rightHandstepY = -0.4;
      ArrayList<Handstep> handsteps = createHandstepForTopsOfHandholds(leftHandstepY, rightHandstepY, scriptedHandstepGenerator);

      for (Handstep handstep : handsteps)
      {
         HandstepPacket handstepPacket = new HandstepPacket(handstep);
         drcSimulationTestHelper.sendHandstepPacketToListeners(handstepPacket);
         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.5);
      }

      FootstepDataList footstepDataList = createFootstepsForStepOntoPlatform(RobotSide.LEFT, 0.5, 0.1, stepHeight, scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0);

      //TODO: This only works if you step a little too high. Otherwise it catches its foot on the step
      double extraHeightToAvoidStubbing = 0.05;
      footstepDataList = createFootstepsForStepOntoPlatform(RobotSide.RIGHT, 0.5, -0.1, stepHeight + extraHeightToAvoidStubbing, scriptedFootstepGenerator);
      drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);

      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0);
      
//      HandPosePacket releaseLeftHandToHome = HandPosePacket.createGoToHomePacket(RobotSide.LEFT, 1.0);
//      drcSimulationTestHelper.sendHandPosePacketToListeners(releaseLeftHandToHome);
//      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
//
//      HandPosePacket releaseRightHandToHome = HandPosePacket.createGoToHomePacket(RobotSide.RIGHT, 1.0);
//      drcSimulationTestHelper.sendHandPosePacketToListeners(releaseRightHandToHome);
//      success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(1.0);
//
//      for (int i=0; i<2; i++)
//      {
//         bodyY = bodyY + 0.3;
//
//         FootstepDataList footstepDataList = createFootstepsForTwoSideSteps(bodyY, scriptedFootstepGenerator);
//         drcSimulationTestHelper.sendFootstepListToListeners(footstepDataList);
//
//         success = success && drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(3.0);
//      }
      
      drcSimulationTestHelper.createMovie(getSimpleRobotName(), 1);
      drcSimulationTestHelper.checkNothingChanged();

      assertTrue(success);

      BambooTools.reportTestFinishedMessage();
   }

   private void setupCameraForBigStepUpWithHandPlatform()
   {
      Point3d cameraFix = new Point3d(1.8375, -0.16, 0.89);
      Point3d cameraPosition = new Point3d(1.10, 8.30, 1.37);

      drcSimulationTestHelper.setupCameraForUnitTest(cameraFix, cameraPosition);
   }

   private ArrayList<Handstep> createHandstepForTopsOfHandholds(double leftHandstepY, double rightHandstepY, ScriptedHandstepGenerator scriptedHandstepGenerator)
   {
      ArrayList<Handstep> ret = new ArrayList<Handstep>();

      double height = 0.9;
      
      RobotSide robotSide = RobotSide.LEFT;
      Tuple3d position = new Point3d(0.5, leftHandstepY, height);
      Vector3d surfaceNormal = new Vector3d(0.0, 0.0, 1.0);
      double rotationAngleAboutNormal = -0.3;
      double swingTrajectoryTime = 1.0;

      Handstep handstep = scriptedHandstepGenerator.createHandstep(robotSide, position, surfaceNormal, rotationAngleAboutNormal, swingTrajectoryTime);
      ret.add(handstep);
      
      robotSide = RobotSide.RIGHT;
      position = new Point3d(0.5, rightHandstepY, height);
      surfaceNormal = new Vector3d(0.0, 0.0, 1.0);
      rotationAngleAboutNormal = -rotationAngleAboutNormal;

      handstep = scriptedHandstepGenerator.createHandstep(robotSide, position, surfaceNormal, rotationAngleAboutNormal, swingTrajectoryTime);
      ret.add(handstep);

      return ret;
   }
   
   private FootstepDataList createFootstepsForStepOntoPlatform(RobotSide robotSide, double stepX, double stepY, double stepZ, ScriptedFootstepGenerator scriptedFootstepGenerator)
   {
      double[][][] footstepLocationsAndOrientations = new double[][][]
      {
         {
            {stepX, stepY, stepZ},
            {0.0, 0.0, 0.0, 1.0}
         }
      };

      RobotSide[] robotSides = drcSimulationTestHelper.createRobotSidesStartingFrom(robotSide, footstepLocationsAndOrientations.length);
      return scriptedFootstepGenerator.generateFootstepsFromLocationsAndOrientations(robotSides, footstepLocationsAndOrientations);
   }

}

