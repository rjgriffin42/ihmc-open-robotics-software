package us.ihmc.darpaRoboticsChallenge.testTools;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.referenceFrames.ReferenceFrames;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.communication.packets.manipulation.HandstepPacket;
import us.ihmc.communication.packets.walking.BlindWalkingPacket;
import us.ihmc.communication.packets.walking.ComHeightPacket;
import us.ihmc.communication.packets.walking.FootstepDataList;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseDemo;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseSimulation;
import us.ihmc.darpaRoboticsChallenge.DRCSimulationFactory;
import us.ihmc.darpaRoboticsChallenge.DRCStartingLocation;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.visualization.SliderBoardFactory;
import us.ihmc.darpaRoboticsChallenge.visualization.WalkControllerSliderBoard;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.AsyncContinuousExecutor;
import us.ihmc.utilities.RandomTools;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.TimerTaskScheduler;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.yoUtilities.time.GlobalTimer;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.ground.TerrainObject3D;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;

public class DRCSimulationTestHelper
{
   private final SimulationConstructionSet scs;
   private final SDFRobot sdfRobot;
   private final DRCSimulationFactory drcSimulationFactory;
   private final ObjectCommunicator networkObjectCommunicator;

   private final boolean checkNothingChanged;
   private final NothingChangedVerifier nothingChangedVerifier;
   private BlockingSimulationRunner blockingSimulationRunner;
   private final WalkingControllerParameters walkingControlParameters;

   private final boolean createMovie;

   private final DRCRobotModel robotModel;
   private final FullRobotModel fullRobotModel;
   private final ReferenceFrames referenceFrames;
   private final ScriptedFootstepGenerator scriptedFootstepGenerator;
   private final ScriptedHandstepGenerator scriptedHandstepGenerator;

   
   public DRCSimulationTestHelper(String name, String scriptFileName, DRCStartingLocation selectedLocation, boolean checkNothingChanged, boolean showGUI,
         boolean createMovie, DRCRobotModel robotModel)
   {
      this(new DRCDemo01NavigationEnvironment().getTerrainObject3D(), new ScriptedFootstepDataListObjectCommunicator("Team"), name, scriptFileName, selectedLocation, checkNothingChanged, showGUI,
            createMovie, false, robotModel);
   }
   
   public DRCSimulationTestHelper(TerrainObject3D terrainObject, String name, String scriptFileName, DRCStartingLocation selectedLocation,
         boolean checkNothingChanged, boolean showGUI, boolean createMovie, DRCRobotModel robotModel)
   {
      this(terrainObject, new ScriptedFootstepDataListObjectCommunicator("Team"), name, scriptFileName, selectedLocation, checkNothingChanged, showGUI,
            createMovie, false, robotModel);
   }
         
   public DRCSimulationTestHelper(TerrainObject3D terrainObject, ObjectCommunicator networkObjectCommunicator, String name, 
         String scriptFileName, DRCStartingLocation selectedLocation, boolean checkNothingChanged, boolean showGUI,
         boolean createMovie, boolean startNetworkProcessor, DRCRobotModel robotModel)
   {
      this.networkObjectCommunicator = networkObjectCommunicator;
      this.walkingControlParameters = robotModel.getWalkingControllerParameters();
      this.checkNothingChanged = checkNothingChanged;
      this.createMovie = createMovie;
      this.robotModel = robotModel;
      if (createMovie)
         showGUI = true;

      fullRobotModel = robotModel.createFullRobotModel();
      referenceFrames = new ReferenceFrames(fullRobotModel);
      scriptedFootstepGenerator = new ScriptedFootstepGenerator(referenceFrames, fullRobotModel, walkingControlParameters);
      scriptedHandstepGenerator = new ScriptedHandstepGenerator(fullRobotModel);

      boolean automaticallyStartSimulation = false;
      boolean initializeEstimatorToActual = true;

      SliderBoardFactory sliderBoardFactory = WalkControllerSliderBoard.getFactory();
      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(false, false, sliderBoardFactory, showGUI);

      DRCObstacleCourseSimulation drcSimulation = DRCObstacleCourseDemo.startDRCSim(terrainObject, scriptFileName, selectedLocation, guiInitialSetup,
            initializeEstimatorToActual, automaticallyStartSimulation, startNetworkProcessor, robotModel, networkObjectCommunicator);

      scs = drcSimulation.getSimulationConstructionSet();
      sdfRobot = drcSimulation.getRobot();
      drcSimulationFactory = drcSimulation.getSimulation();
      blockingSimulationRunner = new BlockingSimulationRunner(scs, 60.0 * 10.0);

      if (checkNothingChanged)
      {
         nothingChangedVerifier = new NothingChangedVerifier(name, scs);
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
      return scs;
   }

   public ScriptedFootstepGenerator createScriptedFootstepGenerator()
   {
      return scriptedFootstepGenerator;
   }
   
   public ScriptedHandstepGenerator createScriptedHandstepGenerator()
   {
      return scriptedHandstepGenerator;
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
         stringsToIgnore.add("startTime");
         stringsToIgnore.add("actualEstimatorDT");
         stringsToIgnore.add("nextExecutionTime");
         stringsToIgnore.add("totalDelay");
         stringsToIgnore.add("lastEstimatorClockStartTime");
         stringsToIgnore.add("lastControllerClockTime");
         stringsToIgnore.add("controllerStartTime");
         stringsToIgnore.add("actualControlDT");

         boolean writeNewBaseFile = nothingChangedVerifier.getWriteNewBaseFile();

         double maxPercentDifference = 0.001;
         nothingChangedVerifier.verifySameResultsAsPreviously(maxPercentDifference, stringsToIgnore);
         assertFalse("Had to write new base file. On next run nothing should change", writeNewBaseFile);
      }
   }

   public void sendFootstepListToListeners(FootstepDataList footstepDataList)
   {
      if (networkObjectCommunicator instanceof ScriptedFootstepDataListObjectCommunicator)
         ((ScriptedFootstepDataListObjectCommunicator) networkObjectCommunicator).sendFootstepListToListeners(footstepDataList);
   }
   
   public void sendHandstepPacketToListeners(HandstepPacket handstepPacket)
   {
      if (networkObjectCommunicator instanceof ScriptedFootstepDataListObjectCommunicator)
         ((ScriptedFootstepDataListObjectCommunicator) networkObjectCommunicator).sendHandstepPacketToListeners(handstepPacket);
   }
   
   public void sendHandPosePacketToListeners(HandPosePacket handPosePacket)
   {
      if (networkObjectCommunicator instanceof ScriptedFootstepDataListObjectCommunicator)
         ((ScriptedFootstepDataListObjectCommunicator) networkObjectCommunicator).sendHandPosePacketToListeners(handPosePacket);

   }

   public void sendBlindWalkingPacketToListeners(BlindWalkingPacket blindWalkingPacket)
   {
      if (networkObjectCommunicator instanceof ScriptedFootstepDataListObjectCommunicator)
         ((ScriptedFootstepDataListObjectCommunicator) networkObjectCommunicator).sendBlindWalkingPacketToListeners(blindWalkingPacket);
   }

   public void sendComHeightPacketToListeners(ComHeightPacket comHeightPacket)
   {
      if (networkObjectCommunicator instanceof ScriptedFootstepDataListObjectCommunicator)
         ((ScriptedFootstepDataListObjectCommunicator) networkObjectCommunicator).sendComHeightPacketToListeners(comHeightPacket);
   }

   public SDFRobot getRobot()
   {
      return sdfRobot;
   }

   public void simulateAndBlock(double simulationTime) throws SimulationExceededMaximumTimeException
   {
      blockingSimulationRunner.simulateAndBlock(simulationTime);
   }

   public void destroySimulation()
   {
      blockingSimulationRunner.destroySimulation();
      blockingSimulationRunner = null;
      if (drcSimulationFactory != null)
      {
         drcSimulationFactory.dispose();
      }
      GlobalTimer.clearTimers();
      TimerTaskScheduler.cancelAndReset();
      AsyncContinuousExecutor.cancelAndReset();
   }

   public boolean simulateAndBlockAndCatchExceptions(double simulationTime) throws SimulationExceededMaximumTimeException
   {
      try
      {
         simulateAndBlock(simulationTime);
         return true;
      }
      catch (Exception e)
      {
         System.err.println("Caught exception in SimulationTestHelper.simulateAndBlockAndCatchExceptions. Exception = /n" + e);
         throw e;
      }
   }

   public void createMovie(String simplifiedRobotModelName, int callStackHeight)
   {
      if (createMovie)
      {
         BambooTools.createMovieAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(simplifiedRobotModelName, scs, callStackHeight);
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

   public void setupCameraForUnitTest(Point3d cameraFix, Point3d cameraPosition)
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
