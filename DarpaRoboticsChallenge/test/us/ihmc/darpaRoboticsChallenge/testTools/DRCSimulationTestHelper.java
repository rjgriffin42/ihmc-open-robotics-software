package us.ihmc.darpaRoboticsChallenge.testTools;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.BlindWalkingPacket;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.packets.ComHeightPacket;
import us.ihmc.commonWalkingControlModules.referenceFrames.ReferenceFrames;
import us.ihmc.darpaRoboticsChallenge.DRCController;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01StartingLocation;
import us.ihmc.darpaRoboticsChallenge.DRCEnvironmentModel;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseSimulation;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.time.GlobalTimer;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;

public class DRCSimulationTestHelper
{
   private final DRCObstacleCourseSimulation drcSimulation;
   private final ScriptedFootstepDataListObjectCommunicator networkObjectCommunicator;
   
   private final boolean checkNothingChanged;
   private final NothingChangedVerifier nothingChangedVerifier;
   private BlockingSimulationRunner blockingSimulationRunner;
   
   private final boolean createMovie;

   public DRCSimulationTestHelper(String name, String scriptFilename, DRCDemo01StartingLocation selectedLocation, DRCEnvironmentModel selectedEnvironment, boolean checkNothingChanged, boolean createMovie)
   {
      this(name, scriptFilename, selectedLocation, selectedEnvironment, checkNothingChanged, createMovie, false);
   }
   
   public DRCSimulationTestHelper(String name, String scriptFilename, DRCDemo01StartingLocation selectedLocation, DRCEnvironmentModel selectedEnvironment, boolean checkNothingChanged, boolean createMovie, boolean createLoadOfContactPointForTheFeet)
   {
      networkObjectCommunicator = new ScriptedFootstepDataListObjectCommunicator("Team");

      this.checkNothingChanged = checkNothingChanged;
      this.createMovie = createMovie;
      
      boolean startOutsidePen = false;
      boolean automaticallyStartSimulation = false;
      boolean startDRCNetworkProcessor = false;

      boolean initializeEstimatorToActual = true;
      
      drcSimulation = DRCDemo01.startDRCSim(scriptFilename, networkObjectCommunicator, selectedLocation, selectedEnvironment, initializeEstimatorToActual,
            startOutsidePen, automaticallyStartSimulation, startDRCNetworkProcessor, createLoadOfContactPointForTheFeet);
      
      blockingSimulationRunner = new BlockingSimulationRunner(drcSimulation.getSimulationConstructionSet(), 60.0 * 10.0);

      if (checkNothingChanged)
      {
         nothingChangedVerifier = new NothingChangedVerifier(name, drcSimulation.getSimulationConstructionSet());
      }
      else
      {
         nothingChangedVerifier = null;
      }
   }

   public BlockingSimulationRunner getBlockingSimulationRunner()
   {
      return blockingSimulationRunner;
   }
   
   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return drcSimulation.getSimulationConstructionSet();
   }

   public ScriptedFootstepGenerator createScriptedFootstepGenerator()
   {
      DRCController controller = drcSimulation.getController();

      ReferenceFrames referenceFrames = controller.getControllerReferenceFrames();
      FullRobotModel fullRobotModel = controller.getControllerModel();

      ScriptedFootstepGenerator scriptedFootstepGenerator = new ScriptedFootstepGenerator(referenceFrames, fullRobotModel);

      return scriptedFootstepGenerator;
   }

   public void checkNothingChanged()
   {
      if (checkNothingChanged)
      {
         ThreadTools.sleep(1000);

         ArrayList<String> stringsToIgnore = new ArrayList<String>();
         stringsToIgnore.add("nano");
         stringsToIgnore.add("milli");
         stringsToIgnore.add("Timer");
         
         boolean writeNewBaseFile = nothingChangedVerifier.getWriteNewBaseFile();

         double maxPercentDifference = 0.001;
         nothingChangedVerifier.verifySameResultsAsPreviously(maxPercentDifference, stringsToIgnore);
         assertFalse("Had to write new base file. On next run nothing should change", writeNewBaseFile);
      }
   }

   public void sendFootstepListToListeners(FootstepDataList footstepDataList)
   {
      networkObjectCommunicator.sendFootstepListToListeners(footstepDataList);
   }

   public void sendFootstepListToListeners(BlindWalkingPacket blindWalkingPacket)
   {
      networkObjectCommunicator.sendBlindWalkingPacketToListeners(blindWalkingPacket);
   }

   public void sendComHeightPacketToListeners(ComHeightPacket comHeightPacket)
   {
      networkObjectCommunicator.sendComHeightPacketToListeners(comHeightPacket);
   }

   public SDFRobot getRobot()
   {
      return drcSimulation.getRobot();
   }
   
   public DRCObstacleCourseSimulation getDRCSimulation()
   {
      return drcSimulation;
   }

   public void simulateAndBlock(double simulationTime) throws SimulationExceededMaximumTimeException
   {
      blockingSimulationRunner.simulateAndBlock(simulationTime);
   }

   public void destroySimulation()
   {
      blockingSimulationRunner.destroySimulation();
      blockingSimulationRunner = null;
      if (drcSimulation != null && drcSimulation.getController() != null)
      {
         drcSimulation.getController().dispose();
      }
      GlobalTimer.clearTimers();
      TimerTaskScheduler.cancelAndReset();
      AsyncContinuousExecutor.cancelAndReset();
   }

   public boolean simulateAndBlockAndCatchExceptions(double simulationTime)
   {
      try
      {
         simulateAndBlock(simulationTime);
         return true;
      }
      catch(Exception e)
      {
         System.err.println("Caught exception in SimulationTestHelper.simulateAndBlockAndCatchExceptions. Exception = /n" + e);
         return false;
      }
   }
   
   public void createMovie(SimulationConstructionSet scs, int callStackHeight)
   {
      if (createMovie)
      {
         BambooTools.createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(scs, callStackHeight);
      }
   }
   
   public RobotSide[] createRobotSidesStartingFrom(RobotSide robotSide, int length)
   {
      RobotSide[] ret = new RobotSide[length];

      for (int i = 0; i < length; i++)
      {
         ret[i] = robotSide;
         robotSide = robotSide.getOppositeSide();
      }

      return ret;
   }
   
   public void setupCameraForUnitTest(SimulationConstructionSet scs, Point3d cameraFix, Point3d cameraPosition)
   {
      CameraConfiguration cameraConfiguration = new CameraConfiguration("testCamera");
      
      Random randomForSlightlyMovingCameraSoThatYouTubeVideosAreDifferent = new Random();
      Vector3d randomCameraOffset = RandomTools.generateRandomVector(randomForSlightlyMovingCameraSoThatYouTubeVideosAreDifferent, 0.05);
      cameraFix.add(randomCameraOffset);
      
      cameraConfiguration.setCameraFix(cameraFix);
      cameraConfiguration.setCameraPosition(cameraPosition);
      cameraConfiguration.setCameraTracking(false, true, true, false);
      cameraConfiguration.setCameraDolly(false, true, true, false);
      scs.setupCamera(cameraConfiguration);
      scs.selectCamera("testCamera");
   }

}
