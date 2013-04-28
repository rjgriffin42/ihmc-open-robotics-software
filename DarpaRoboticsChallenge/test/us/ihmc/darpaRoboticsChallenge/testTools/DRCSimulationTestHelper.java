package us.ihmc.darpaRoboticsChallenge.testTools;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.bambooTools.BambooTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataList;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.referenceFrames.ReferenceFrames;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01;
import us.ihmc.darpaRoboticsChallenge.DRCDemo01StartingLocation;
import us.ihmc.darpaRoboticsChallenge.DRCEnvironmentModel;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseSimulation;
import us.ihmc.darpaRoboticsChallenge.HumanoidRobotSimulation;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.ThreadTools;

import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;

public class DRCSimulationTestHelper
{
   private final DRCObstacleCourseSimulation drcSimulation;
   private final ScriptedFootstepDataListObjectCommunicator teamObjectCommunicator;
   
   private final boolean checkNothingChanged;
   private final NothingChangedVerifier nothingChangedVerifier;
   private BlockingSimulationRunner blockingSimulationRunner;
   
   private final boolean createMovie;

   public DRCSimulationTestHelper(DRCDemo01StartingLocation selectedLocation, DRCEnvironmentModel selectedEnvironment, boolean checkNothingChanged, boolean createMovie)
   {
      teamObjectCommunicator = new ScriptedFootstepDataListObjectCommunicator("Team");
      ScriptedFootstepDataListObjectCommunicator drcNetworkObjectCommunicator = new ScriptedFootstepDataListObjectCommunicator("DRCNetwork");

      this.checkNothingChanged = checkNothingChanged;
      this.createMovie = createMovie;
      
      boolean startOutsidePen = false;
      boolean useGazebo = false;
      boolean automaticallyStartSimulation = false;
      boolean startDRCNetworkProcessor = false;

      drcSimulation = DRCDemo01.startDRCSim(teamObjectCommunicator, drcNetworkObjectCommunicator, selectedLocation, selectedEnvironment, startOutsidePen,
              useGazebo, automaticallyStartSimulation, startDRCNetworkProcessor);

      blockingSimulationRunner = new BlockingSimulationRunner(drcSimulation.getSimulationConstructionSet(), 60.0 * 10.0);

      if (checkNothingChanged)
      {
         nothingChangedVerifier = new NothingChangedVerifier("DRCWalkingUpToRampTest", drcSimulation.getSimulationConstructionSet());
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
      HumanoidRobotSimulation<SDFRobot> simulationDRCSimulation = drcSimulation.getSimulationDRCSimulation();

      ReferenceFrames referenceFrames = simulationDRCSimulation.getController().getControllerReferenceFrames();
      FullRobotModel fullRobotModel = simulationDRCSimulation.getController().getControllerModel();

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

         boolean writeNewBaseFile = nothingChangedVerifier.getWriteNewBaseFile();

         double maxPercentDifference = 0.001;
         nothingChangedVerifier.verifySameResultsAsPreviously(maxPercentDifference, stringsToIgnore);
         assertFalse("Had to write new base file. On next run nothing should change", writeNewBaseFile);
      }
   }

   public void sendFootstepListToListeners(FootstepDataList footstepDataList)
   {
      teamObjectCommunicator.sendFootstepListToListeners(footstepDataList);
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
      cameraConfiguration.setCameraFix(cameraFix);
      cameraConfiguration.setCameraPosition(cameraPosition);
      cameraConfiguration.setCameraTracking(false, true, true, false);
      cameraConfiguration.setCameraDolly(false, true, true, false);
      scs.setupCamera(cameraConfiguration);
      scs.selectCamera("testCamera");
   }

}
