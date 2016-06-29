package us.ihmc.valkyrieRosControl;

import java.io.IOException;
import java.util.HashMap;

import us.ihmc.affinity.Affinity;
import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.MomentumBasedControllerFactory;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.WalkingProvider;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.darpaRoboticsChallenge.DRCEstimatorThread;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.humanoidRobotics.communication.packets.StampedPosePacket;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelState;
import us.ihmc.humanoidRobotics.communication.streamingData.HumanoidGlobalDataProducer;
import us.ihmc.humanoidRobotics.communication.subscribers.PelvisPoseCorrectionCommunicator;
import us.ihmc.humanoidRobotics.communication.subscribers.PelvisPoseCorrectionCommunicatorInterface;
import us.ihmc.humanoidRobotics.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.robotDataCommunication.YoVariableServer;
import us.ihmc.robotDataCommunication.logger.LogSettings;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.rosControl.EffortJointHandle;
import us.ihmc.rosControl.wholeRobot.ForceTorqueSensorHandle;
import us.ihmc.rosControl.wholeRobot.IHMCWholeRobotControlJavaBridge;
import us.ihmc.rosControl.wholeRobot.IMUHandle;
import us.ihmc.rosControl.wholeRobot.PositionJointHandle;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.tools.SettableTimestampProvider;
import us.ihmc.util.PeriodicRealtimeThreadScheduler;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.configuration.ValkyrieConfigurationRoot;
import us.ihmc.valkyrie.parameters.ValkyrieSensorInformation;
import us.ihmc.wholeBodyController.DRCControllerThread;
import us.ihmc.wholeBodyController.DRCOutputWriter;
import us.ihmc.wholeBodyController.DRCOutputWriterWithTorqueOffsets;
import us.ihmc.wholeBodyController.RobotContactPointParameters;
import us.ihmc.wholeBodyController.concurrent.MultiThreadedRealTimeRobotController;
import us.ihmc.wholeBodyController.concurrent.ThreadDataSynchronizer;
import us.ihmc.wholeBodyController.diagnostics.DiagnosticsWhenHangingControllerFactory;
import us.ihmc.wholeBodyController.diagnostics.HumanoidJointPoseList;

public class ValkyrieRosControlController extends IHMCWholeRobotControlJavaBridge
{   
//   private static final String[] torqueControlledJoints = { "leftHipYaw", "leftHipRoll", "leftHipPitch", "leftKneePitch", "leftAnklePitch", "leftAnkleRoll",
//         "rightHipYaw", "rightHipRoll", "rightHipPitch", "rightKneePitch", "rightAnklePitch", "rightAnkleRoll", "torsoYaw", "torsoPitch", "torsoRoll",
//         "leftShoulderPitch", "leftShoulderRoll", "leftShoulderYaw", "leftElbowPitch", "leftForearmYaw", "leftWristRoll", "leftWristPitch", "lowerNeckPitch",
//         "neckYaw", "upperNeckPitch", "rightShoulderPitch", "rightShoulderRoll", "rightShoulderYaw", "rightElbowPitch", "rightForearmYaw", "rightWristRoll",
//         "rightWristPitch" };
	private static final String[] torqueControlledJoints = {
	      "leftHipYaw", "leftHipRoll", "leftHipPitch", "leftKneePitch", "leftAnklePitch", "leftAnkleRoll",
	      "rightHipYaw", "rightHipRoll", "rightHipPitch", "rightKneePitch", "rightAnklePitch", "rightAnkleRoll",
	      "torsoYaw", "torsoPitch", "torsoRoll",
	      "leftShoulderPitch", "leftShoulderRoll", "leftShoulderYaw", "leftElbowPitch",
//	      "lowerNeckPitch", "neckYaw", "upperNeckPitch",
	      "rightShoulderPitch", "rightShoulderRoll", "rightShoulderYaw", "rightElbowPitch"
	      };

   private static final String[] positionControlledJoints = {"lowerNeckPitch", "neckYaw", "upperNeckPitch",};

   public static final boolean USE_USB_MICROSTRAIN_IMUS = false;
   public static final boolean USE_SWITCHABLE_FILTER_HOLDER_FOR_NON_USB_IMUS = false;
   public static final String[] readIMUs = USE_USB_MICROSTRAIN_IMUS ? new String[0] : new String[ValkyrieSensorInformation.imuSensorsToUse.length];
   static
   {
      if (!USE_USB_MICROSTRAIN_IMUS)
      {
         for (int i = 0; i < ValkyrieSensorInformation.imuSensorsToUse.length; i++)
         {
            readIMUs[i] = ValkyrieSensorInformation.imuSensorsToUse[i].replace("pelvis_", "").replace("torso_", "");
         }
      }
   }

   public static final String[] readForceTorqueSensors = {"leftFootSixAxis", "rightFootSixAxis"};
   public static final String[] forceTorqueSensorModelNames = {"leftAnkleRoll", "rightAnkleRoll"};

   public static final double gravity = 9.80665;

   private static final WalkingProvider walkingProvider = WalkingProvider.DATA_PRODUCER;

   public static final boolean INTEGRATE_ACCELERATIONS_AND_CONTROL_VELOCITIES = true;
   private static final boolean DO_SLOW_INTEGRATION_FOR_TORQUE_OFFSET = true;

   private MultiThreadedRealTimeRobotController robotController;

   private final SettableTimestampProvider timestampProvider = new SettableTimestampProvider();

   private boolean firstTick = true;

   private final ValkyrieAffinity valkyrieAffinity = new ValkyrieAffinity();

   public ValkyrieRosControlController()
   {

   }

   private DiagnosticsWhenHangingControllerFactory diagnosticControllerFactory = null;

   private MomentumBasedControllerFactory createDRCControllerFactory(ValkyrieRobotModel robotModel, PacketCommunicator packetCommunicator,
         DRCRobotSensorInformation sensorInformation)
   {
      ContactableBodiesFactory contactableBodiesFactory = robotModel.getContactPointParameters().getContactableBodiesFactory();

      final HighLevelState initialBehavior = HighLevelState.DO_NOTHING_BEHAVIOR; // HERE!!
      WalkingControllerParameters walkingControllerParamaters = robotModel.getWalkingControllerParameters();
      ArmControllerParameters armControllerParamaters = robotModel.getArmControllerParameters();
      CapturePointPlannerParameters capturePointPlannerParameters = robotModel.getCapturePointPlannerParameters();

      SideDependentList<String> feetContactSensorNames = sensorInformation.getFeetContactSensorNames();
      SideDependentList<String> feetForceSensorNames = sensorInformation.getFeetForceSensorNames();
      SideDependentList<String> wristForceSensorNames = sensorInformation.getWristForceSensorNames();
      MomentumBasedControllerFactory controllerFactory = new MomentumBasedControllerFactory(contactableBodiesFactory, feetForceSensorNames,
            feetContactSensorNames, wristForceSensorNames, walkingControllerParamaters, armControllerParamaters, capturePointPlannerParameters,
            initialBehavior);

      HumanoidJointPoseList humanoidJointPoseList = new HumanoidJointPoseList();
      humanoidJointPoseList.createPoseSetters();
      humanoidJointPoseList.createPoseSettersJustArms();
      humanoidJointPoseList.createPoseSettersJustLegs();
      humanoidJointPoseList.createPoseSettersTuneWaist();

      ValkyrieTorqueOffsetPrinter valkyrieTorqueOffsetPrinter = new ValkyrieTorqueOffsetPrinter();
      valkyrieTorqueOffsetPrinter.setRobotName(robotModel.getFullRobotName());
      diagnosticControllerFactory = new DiagnosticsWhenHangingControllerFactory(humanoidJointPoseList, true, true, valkyrieTorqueOffsetPrinter);
      diagnosticControllerFactory.setTransitionRequested(true);
      controllerFactory.addHighLevelBehaviorFactory(diagnosticControllerFactory);
      PeriodicRealtimeThreadScheduler networkSubscriberScheduler = new PeriodicRealtimeThreadScheduler(ValkyriePriorityParameters.POSECOMMUNICATOR_PRIORITY);
      controllerFactory.createControllerNetworkSubscriber(networkSubscriberScheduler, packetCommunicator);

      if (walkingProvider == WalkingProvider.VELOCITY_HEADING_COMPONENT)
         controllerFactory.createComponentBasedFootstepDataMessageGenerator();

      return controllerFactory;
   }

   @Override
   protected void init()
   {
      long maxMemory = Runtime.getRuntime().maxMemory();

      System.out.println("Partying hard with max memory of: " + maxMemory);
      /*
       * Create joints
       */

      HashMap<String, EffortJointHandle> effortJointHandles = new HashMap<>();
      for (String joint : torqueControlledJoints)
      {
         effortJointHandles.put(joint, createEffortJointHandle(joint));
      }

      HashMap<String, PositionJointHandle> positionJointHandles = new HashMap<>();
      for (String joint : positionControlledJoints)
      {
         positionJointHandles.put(joint, createPositionJointHandle(joint));
      }

      HashMap<String, IMUHandle> imuHandles = new HashMap<>();
      for (String imu : readIMUs)
      {
         if (USE_SWITCHABLE_FILTER_HOLDER_FOR_NON_USB_IMUS)
         {
            String complimentaryFilterHandleName = "CF" + imu;
            String kalmanFilterHandleName = "EF" + imu;
            imuHandles.put(complimentaryFilterHandleName, createIMUHandle(complimentaryFilterHandleName));
            imuHandles.put(kalmanFilterHandleName, createIMUHandle(kalmanFilterHandleName));
         }
         else
         {
            imuHandles.put(imu, createIMUHandle(imu));
         }
      }

      HashMap<String, ForceTorqueSensorHandle> forceTorqueSensorHandles = new HashMap<>();
      for (int i = 0; i < readForceTorqueSensors.length; i++)
      {
         String forceTorqueSensor = readForceTorqueSensors[i];
         String modelName = forceTorqueSensorModelNames[i];
         forceTorqueSensorHandles.put(modelName, createForceTorqueSensorHandle(forceTorqueSensor));
      }

      /*
       * Create registries
       */

      ValkyrieRobotModel robotModel = new ValkyrieRobotModel(DRCRobotModel.RobotTarget.REAL_ROBOT, true);
      ValkyrieSensorInformation sensorInformation = robotModel.getSensorInformation();

      /*
       * Create network servers/clients
       */
      PacketCommunicator controllerPacketCommunicator = PacketCommunicator.createTCPPacketCommunicatorServer(NetworkPorts.CONTROLLER_PORT,
            new IHMCCommunicationKryoNetClassList());
      PeriodicRealtimeThreadScheduler yoVariableServerScheduler = new PeriodicRealtimeThreadScheduler(ValkyriePriorityParameters.LOGGER_PRIORITY);
      LogModelProvider logModelProvider = robotModel.getLogModelProvider();
      LogSettings logSettings = robotModel.getLogSettings(ValkyrieConfigurationRoot.USE_CAMERAS_FOR_LOGGING);
      double estimatorDT = robotModel.getEstimatorDT();
      YoVariableServer yoVariableServer = new YoVariableServer(getClass(), yoVariableServerScheduler, logModelProvider, logSettings, estimatorDT);
      HumanoidGlobalDataProducer dataProducer = new HumanoidGlobalDataProducer(controllerPacketCommunicator);

      /*
       * Create sensors
       */

      StateEstimatorParameters stateEstimatorParameters = robotModel.getStateEstimatorParameters();

      ValkyrieRosControlSensorReaderFactory sensorReaderFactory = new ValkyrieRosControlSensorReaderFactory(timestampProvider, stateEstimatorParameters,
            effortJointHandles, positionJointHandles, imuHandles, forceTorqueSensorHandles, sensorInformation);

      /*
       * Create controllers
       */
      MomentumBasedControllerFactory controllerFactory = createDRCControllerFactory(robotModel, controllerPacketCommunicator, sensorInformation);
      controllerFactory.attachControllerFailureListener(sensorReaderFactory.createControllerFailureListener());

      /*
       * Create output writer
       */
      ValkyrieRosControlOutputWriter valkyrieOutputWriter = new ValkyrieRosControlOutputWriter(robotModel);
      DRCOutputWriter drcOutputWriter = valkyrieOutputWriter;

      if (DO_SLOW_INTEGRATION_FOR_TORQUE_OFFSET)
      {
         double controllerDT = robotModel.getControllerDT();
         DRCOutputWriterWithTorqueOffsets drcOutputWriterWithTorqueOffsets = new DRCOutputWriterWithTorqueOffsets(drcOutputWriter, controllerDT);
         drcOutputWriter = drcOutputWriterWithTorqueOffsets;
      }

      PelvisPoseCorrectionCommunicatorInterface externalPelvisPoseSubscriber = null;
      externalPelvisPoseSubscriber = new PelvisPoseCorrectionCommunicator(null);
      dataProducer.attachListener(StampedPosePacket.class, externalPelvisPoseSubscriber);

      /*
       * Build controller
       */
      ThreadDataSynchronizer threadDataSynchronizer = new ThreadDataSynchronizer(robotModel);
      RobotContactPointParameters contactPointParameters = robotModel.getContactPointParameters();
      PeriodicRealtimeThreadScheduler estimatorScheduler = new PeriodicRealtimeThreadScheduler(ValkyriePriorityParameters.POSECOMMUNICATOR_PRIORITY);
      DRCEstimatorThread estimatorThread = new DRCEstimatorThread(sensorInformation, contactPointParameters, stateEstimatorParameters, sensorReaderFactory,
            threadDataSynchronizer, estimatorScheduler, dataProducer, yoVariableServer, gravity);
      estimatorThread.setExternalPelvisCorrectorSubscriber(externalPelvisPoseSubscriber);
      DRCControllerThread controllerThread = new DRCControllerThread(robotModel, sensorInformation, controllerFactory, threadDataSynchronizer, drcOutputWriter,
            dataProducer, yoVariableServer, gravity, estimatorDT);

      if (diagnosticControllerFactory != null)
         diagnosticControllerFactory.attachJointTorqueOffsetProcessor(sensorReaderFactory.getSensorReader());

      /*
       * Connect all servers
       */
      try
      {
         controllerPacketCommunicator.connect();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      yoVariableServer.start();

      robotController = new MultiThreadedRealTimeRobotController(estimatorThread);
      if (valkyrieAffinity.setAffinity())
      {
         robotController.addController(controllerThread, ValkyriePriorityParameters.CONTROLLER_PRIORITY, valkyrieAffinity.getControlThreadProcessor());
      }
      else
      {
         robotController.addController(controllerThread, ValkyriePriorityParameters.CONTROLLER_PRIORITY, null);
      }

      robotController.start();
   }

   @Override
   protected void doControl(long time, long duration)
   {
      if (firstTick)
      {
         if (valkyrieAffinity.setAffinity())
         {
            System.out.println("Setting estimator thread affinity to processor " + valkyrieAffinity.getEstimatorThreadProcessor().getId());
            Affinity.setAffinity(valkyrieAffinity.getEstimatorThreadProcessor());
         }
         firstTick = false;
      }

      timestampProvider.setTimestamp(time);
      robotController.read();
   }
}
