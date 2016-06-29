package us.ihmc.darpaRoboticsChallenge.testTools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullHumanoidRobotModel;
import us.ihmc.SdfLoader.SDFHumanoidRobot;
import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelBehaviorFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.MomentumBasedControllerFactory;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.communication.net.LocalObjectCommunicator;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.darpaRoboticsChallenge.DRCGuiInitialSetup;
import us.ihmc.darpaRoboticsChallenge.DRCObstacleCourseStartingLocation;
import us.ihmc.darpaRoboticsChallenge.DRCSimulationFactory;
import us.ihmc.darpaRoboticsChallenge.DRCSimulationStarter;
import us.ihmc.darpaRoboticsChallenge.DRCStartingLocation;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.environment.CommonAvatarEnvironmentInterface;
import us.ihmc.darpaRoboticsChallenge.environment.DRCDemo01NavigationEnvironment;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkModuleParameters;
import us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.ForceSensorHysteresisCreator;
import us.ihmc.darpaRoboticsChallenge.scriptEngine.ScriptBasedControllerCommandGenerator;
import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.humanoidRobotics.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.robotics.dataStructures.variable.YoVariable;
import us.ihmc.robotics.geometry.BoundingBox3d;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.InverseDynamicsCalculatorListener;
import us.ihmc.robotics.time.GlobalTimer;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.bambooTools.SimulationTestingParameters;
import us.ihmc.simulationconstructionset.robotController.RobotController;
import us.ihmc.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerFailureException;
import us.ihmc.simulationconstructionset.util.simulationTesting.NothingChangedVerifier;
import us.ihmc.tools.thread.ThreadTools;

public class DRCSimulationTestHelper
{
   private final SimulationConstructionSet scs;
   private final SDFHumanoidRobot sdfRobot;
   private final DRCSimulationFactory drcSimulationFactory;
   protected final PacketCommunicator controllerCommunicator;
   private final CommonAvatarEnvironmentInterface testEnvironment;

   private final SimulationTestingParameters simulationTestingParameters;

   private final NothingChangedVerifier nothingChangedVerifier;
   private BlockingSimulationRunner blockingSimulationRunner;
   private final WalkingControllerParameters walkingControlParameters;

   private final FullHumanoidRobotModel fullRobotModel;
   private final ScriptedFootstepGenerator scriptedFootstepGenerator;
   private final ScriptedHandstepGenerator scriptedHandstepGenerator;

   private final DRCNetworkModuleParameters networkProcessorParameters;
   private DRCSimulationStarter simulationStarter;
   private Exception caughtException;

   public DRCSimulationTestHelper(String name, DRCObstacleCourseStartingLocation selectedLocation,
         SimulationTestingParameters simulationconstructionsetparameters, DRCRobotModel robotModel)
   {
      this(new DRCDemo01NavigationEnvironment(), name, selectedLocation, simulationconstructionsetparameters, robotModel);
   }

   public DRCSimulationTestHelper(CommonAvatarEnvironmentInterface commonAvatarEnvironmentInterface, String name,
         DRCStartingLocation selectedLocation, SimulationTestingParameters simulationTestingParameters, DRCRobotModel robotModel)
   {
      this(commonAvatarEnvironmentInterface, name, selectedLocation, simulationTestingParameters, robotModel, null, null, null);
   }

   public DRCSimulationTestHelper(CommonAvatarEnvironmentInterface commonAvatarEnvironmentInterface, String name,
         DRCStartingLocation selectedLocation, SimulationTestingParameters simulationTestingParameters, DRCRobotModel robotModel,
         DRCNetworkModuleParameters drcNetworkModuleParameters)
   {
      this(commonAvatarEnvironmentInterface, name, selectedLocation, simulationTestingParameters, robotModel, drcNetworkModuleParameters, null, null);
   }

   public DRCSimulationTestHelper(CommonAvatarEnvironmentInterface commonAvatarEnvironmentInterface, String name,
         DRCStartingLocation selectedLocation, SimulationTestingParameters simulationTestingParameters, DRCRobotModel robotModel,
         DRCNetworkModuleParameters drcNetworkModuleParameters, HighLevelBehaviorFactory highLevelBehaviorFactoryToAdd, DRCRobotInitialSetup<SDFHumanoidRobot> initialSetup)
   {
      this.controllerCommunicator = PacketCommunicator.createIntraprocessPacketCommunicator(NetworkPorts.CONTROLLER_PORT, new IHMCCommunicationKryoNetClassList());
      this.testEnvironment = commonAvatarEnvironmentInterface;

      this.walkingControlParameters = robotModel.getWalkingControllerParameters();

      this.simulationTestingParameters = simulationTestingParameters;

      try
      {
         controllerCommunicator.connect();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }

      fullRobotModel = robotModel.createFullRobotModel();
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      scriptedFootstepGenerator = new ScriptedFootstepGenerator(referenceFrames, fullRobotModel, walkingControlParameters);
      scriptedHandstepGenerator = new ScriptedHandstepGenerator(fullRobotModel);

      DRCGuiInitialSetup guiInitialSetup = new DRCGuiInitialSetup(false, false, simulationTestingParameters);

      simulationStarter = new DRCSimulationStarter(robotModel, commonAvatarEnvironmentInterface);
      simulationStarter.setRunMultiThreaded(simulationTestingParameters.getRunMultiThreaded());
      simulationStarter.setUsePerfectSensors(simulationTestingParameters.getUsePefectSensors());
      if (highLevelBehaviorFactoryToAdd != null)
         simulationStarter.registerHighLevelController(highLevelBehaviorFactoryToAdd);
      if (initialSetup != null)
         simulationStarter.setRobotInitialSetup(initialSetup);
      simulationStarter.setStartingLocation(selectedLocation);
      simulationStarter.setGuiInitialSetup(guiInitialSetup);
      simulationStarter.setInitializeEstimatorToActual(true);

      if (drcNetworkModuleParameters == null)
      {
         networkProcessorParameters = new DRCNetworkModuleParameters();
         networkProcessorParameters.enableNetworkProcessor(false);
      }
      else
      {
         networkProcessorParameters = drcNetworkModuleParameters;
      }

      simulationStarter.startSimulation(networkProcessorParameters, false);

      scs = simulationStarter.getSimulationConstructionSet();
      sdfRobot = simulationStarter.getSDFRobot();
      drcSimulationFactory = simulationStarter.getDRCSimulationFactory();
      blockingSimulationRunner = new BlockingSimulationRunner(scs, 60.0 * 10.0);
      simulationStarter.attachControllerFailureListener(blockingSimulationRunner.createControllerFailureListener());



      if (simulationTestingParameters.getCheckNothingChangedInSimulation())
      {
         nothingChangedVerifier = new NothingChangedVerifier(name, scs);
      }
      else
      {
         nothingChangedVerifier = null;
      }
   }

   public YoVariable<?> getYoVariable(String name)
   {
      return scs.getVariable(name);
   }

   public YoVariable<?> getYoVariable(String nameSpace, String name)
   {
      return scs.getVariable(nameSpace, name);
   }

   public void loadScriptFile(String scriptFilename, ReferenceFrame referenceFrame)
   {
      ScriptBasedControllerCommandGenerator scriptBasedControllerCommandGenerator = simulationStarter.getScriptBasedControllerCommandGenerator();
      scriptBasedControllerCommandGenerator.loadScriptFile(scriptFilename, referenceFrame);
   }

   public void loadScriptFile(InputStream scriptInputStream, ReferenceFrame referenceFrame)
   {
      ScriptBasedControllerCommandGenerator scriptBasedControllerCommandGenerator = simulationStarter.getScriptBasedControllerCommandGenerator();
      scriptBasedControllerCommandGenerator.loadScriptFile(scriptInputStream, referenceFrame);
   }

   public ConcurrentLinkedQueue<Command<?, ?>> getQueuedControllerCommands()
   {
      return simulationStarter.getQueuedControllerCommands();
   }

   public BlockingSimulationRunner getBlockingSimulationRunner()
   {
      return blockingSimulationRunner;
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return scs;
   }

   public DRCSimulationFactory getDRCSimulationFactory()
   {
      return drcSimulationFactory;
   }

   public void setInverseDynamicsCalculatorListener(InverseDynamicsCalculatorListener inverseDynamicsCalculatorListener)
   {
      MomentumBasedControllerFactory controllerFactory = drcSimulationFactory.getControllerFactory();
      controllerFactory.setInverseDynamicsCalculatorListener(inverseDynamicsCalculatorListener);
   }

   public SDFFullHumanoidRobotModel getControllerFullRobotModel()
   {
      return drcSimulationFactory.getControllerFullRobotModel();
   }

   public SDFFullHumanoidRobotModel getSDFFullRobotModel()
   {
      return (SDFFullHumanoidRobotModel) fullRobotModel;
   }

   /**
    * For unit testing only
    *
    * @param controller
    */
   public void addRobotControllerOnControllerThread(RobotController controller)
   {
      drcSimulationFactory.addRobotControllerOnControllerThread(controller);
   }

   public CommonAvatarEnvironmentInterface getTestEnviroment()
   {
      return testEnvironment;
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
      if (simulationTestingParameters.getCheckNothingChangedInSimulation())
      {
         ThreadTools.sleep(1000);

         ArrayList<String> stringsToIgnore = createVariableNamesStringsToIgnore();

         boolean writeNewBaseFile = nothingChangedVerifier.getWriteNewBaseFile();

         double maxPercentDifference = 0.001;
         nothingChangedVerifier.verifySameResultsAsPreviously(maxPercentDifference, stringsToIgnore);
         assertFalse("Had to write new base file. On next run nothing should change", writeNewBaseFile);
      }
   }

   public SDFHumanoidRobot getRobot()
   {
      return sdfRobot;
   }

   public void simulateAndBlock(double simulationTime) throws SimulationExceededMaximumTimeException, ControllerFailureException
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

      if (networkProcessorParameters != null)
      {
         LocalObjectCommunicator simulatedSensorCommunicator = networkProcessorParameters.getSimulatedSensorCommunicator();
         if (simulatedSensorCommunicator != null)
         {
            simulatedSensorCommunicator.close();
         }

      }
      if (controllerCommunicator != null)
      {
         controllerCommunicator.close();
      }

      simulationStarter.close();
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
         this.caughtException = e;
         System.err.println("Caught exception in " + getClass().getSimpleName() + ".simulateAndBlockAndCatchExceptions. Exception = /n" + e);
         return false;
      }
   }

   public void createVideo(String simplifiedRobotModelName, int callStackHeight)
   {
      if (simulationTestingParameters.getCreateSCSVideos())
      {
         BambooTools.createVideoAndDataWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(simplifiedRobotModelName, scs, callStackHeight);
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

   public void assertRobotsRootJointIsInBoundingBox(BoundingBox3d boundingBox)
   {
      assertRobotsRootJointIsInBoundingBox(boundingBox, getRobot());
   }

   public static void assertRobotsRootJointIsInBoundingBox(BoundingBox3d boundingBox, SDFHumanoidRobot robot)
   {
      Point3d position = new Point3d();
      robot.getRootJoint().getPosition(position);
      boolean inside = boundingBox.isInside(position);
      if (!inside)
      {
         fail("Joint was at " + position + ". Expecting it to be inside boundingBox " + boundingBox);
      }
   }

   public static ArrayList<String> createVariableNamesStringsToIgnore()
   {
      ArrayList<String> exceptions = new ArrayList<String>();
      exceptions.add("nano");
      exceptions.add("milli");
      exceptions.add("Timer");
      exceptions.add("startTime");
      exceptions.add("actualEstimatorDT");
      exceptions.add("nextExecutionTime");
      exceptions.add("totalDelay");
      exceptions.add("lastEstimatorClockStartTime");
      exceptions.add("lastControllerClockTime");
      exceptions.add("controllerStartTime");
      exceptions.add("actualControlDT");
      exceptions.add("timePassed");

      //    exceptions.add("gc_");
      //    exceptions.add("toolFrame");
      //    exceptions.add("ef_");
      //    exceptions.add("kp_");

      return exceptions;
   }

   public Exception getCaughtException()
   {
      return caughtException;
   }

   public void send(Packet<?> packet)
   {
      controllerCommunicator.send(packet);
   }

   public <T extends Packet<?>> void attachListener(Class<T> clazz, PacketConsumer<T> listener)
   {
      controllerCommunicator.attachListener(clazz, listener);
   }

   public PacketCommunicator getControllerCommunicator()
   {
      return controllerCommunicator;
   }

   public ArrayList<RobotController> getFootForceSensorHysteresisCreators()
   {
      SideDependentList<ArrayList<WrenchCalculatorInterface>> footForceSensors = new SideDependentList<ArrayList<WrenchCalculatorInterface>>();
      packFootForceSensors(footForceSensors);

      ArrayList<RobotController> footForceSensorSignalCorruptors = new ArrayList<RobotController>();

      for(RobotSide robotSide : RobotSide.values)
      {
         for(int i = 0; i<footForceSensors.get(robotSide).size(); i++)
         {
            ForceSensorHysteresisCreator forceSensorSignalCorruptor = new ForceSensorHysteresisCreator(sdfRobot.computeCenterOfMass(new Point3d()),
                  footForceSensors.get(robotSide).get(i).getName(), footForceSensors.get(robotSide).get(i));

            footForceSensorSignalCorruptors.add(forceSensorSignalCorruptor);
         }
      }

      return footForceSensorSignalCorruptors;
   }

   public void packFootForceSensors(SideDependentList<ArrayList<WrenchCalculatorInterface>> footForceSensors)
   {
      ArrayList<WrenchCalculatorInterface> forceSensors = new ArrayList<WrenchCalculatorInterface>();
      sdfRobot.getForceSensors(forceSensors);

      SideDependentList<String> jointNamesBeforeFeet = sdfRobot.getJointNamesBeforeFeet();

      for(RobotSide robotSide : RobotSide.values)
      {
         footForceSensors.put(robotSide,new ArrayList<WrenchCalculatorInterface>());
         for(int i = 0; i<forceSensors.size(); i++)
         {
            if(forceSensors.get(i).getJoint().getName().equals(jointNamesBeforeFeet.get(robotSide)))
            {
               footForceSensors.get(robotSide).add(forceSensors.get(i));
            }
         }
      }
   }


}
