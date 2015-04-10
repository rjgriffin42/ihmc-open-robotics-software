package us.ihmc.darpaRoboticsChallenge;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFJointNameMap.JointRole;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonWalkingControlModules.corruptors.FullRobotModelCorruptor;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.MomentumBasedControllerFactory;
import us.ihmc.communication.packets.StampedPosePacket;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.communication.subscribers.PelvisPoseCorrectionCommunicator;
import us.ihmc.communication.subscribers.PelvisPoseCorrectionCommunicatorInterface;
import us.ihmc.darpaRoboticsChallenge.controllers.DRCSimulatedIMUPublisher;
import us.ihmc.darpaRoboticsChallenge.controllers.JointLowLevelPositionControlSimulator;
import us.ihmc.darpaRoboticsChallenge.controllers.PIDLidarTorqueController;
import us.ihmc.darpaRoboticsChallenge.controllers.PassiveJointController;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.SimulatedDRCRobotTimeProvider;
import us.ihmc.darpaRoboticsChallenge.environment.CommonAvatarEnvironmentInterface;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.robotDataCommunication.VisualizerUtils;
import us.ihmc.robotDataCommunication.YoVariableServer;
import us.ihmc.sensorProcessing.parameters.DRCRobotLidarParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.sensorProcessing.simulatedSensors.DRCPerfectSensorReaderFactory;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReaderFactory;
import us.ihmc.sensorProcessing.simulatedSensors.SimulatedSensorHolderAndReaderFromRobotFactory;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.SimulationConstructionSetParameters;
import us.ihmc.simulationconstructionset.UnreasonableAccelerationException;
import us.ihmc.simulationconstructionset.robotController.AbstractThreadedRobotController;
import us.ihmc.simulationconstructionset.robotController.MultiThreadedRobotControlElement;
import us.ihmc.simulationconstructionset.robotController.MultiThreadedRobotController;
import us.ihmc.simulationconstructionset.robotController.SingleThreadedRobotController;
import us.ihmc.util.PeriodicNonRealtimeThreadScheduler;
import us.ihmc.util.PeriodicThreadScheduler;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.TimestampProvider;
import us.ihmc.utilities.io.printing.PrintTools;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.wholeBodyController.DRCControllerThread;
import us.ihmc.wholeBodyController.DRCOutputWriter;
import us.ihmc.wholeBodyController.DRCOutputWriterWithStateChangeSmoother;
import us.ihmc.wholeBodyController.DRCOutputWriterWithTorqueOffsets;
import us.ihmc.wholeBodyController.DRCRobotJointMap;
import us.ihmc.wholeBodyController.DRCSimulationOutputWriter;
import us.ihmc.wholeBodyController.concurrent.SingleThreadedThreadDataSynchronizer;
import us.ihmc.wholeBodyController.concurrent.ThreadDataSynchronizer;
import us.ihmc.wholeBodyController.concurrent.ThreadDataSynchronizerInterface;
import us.ihmc.yoUtilities.controllers.YoPDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class DRCSimulationFactory
{
//   public static boolean RUN_MULTI_THREADED = !true;

   public static final boolean DO_SLOW_INTEGRATION_FOR_TORQUE_OFFSET = false;
   private static final boolean DO_SMOOTH_JOINT_TORQUES_AT_CONTROLLER_STATE_CHANGES = false;
   
   private static final double gravity = -9.81;

   private final SimulationConstructionSet scs;
   private final SDFRobot simulatedRobot;
   private final MomentumBasedControllerFactory controllerFactory;
   
   private DRCEstimatorThread drcEstimatorThread;
   private DRCControllerThread drcControllerThread;
   private AbstractThreadedRobotController multiThreadedRobotController;

   private final SimulatedDRCRobotTimeProvider simulatedDRCRobotTimeProvider;
   
   private final YoVariableServer yoVariableServer;

   private ThreadDataSynchronizerInterface threadDataSynchronizer;

   private GlobalDataProducer globalDataProducer;
   
   public DRCSimulationFactory(DRCRobotModel drcRobotModel, MomentumBasedControllerFactory controllerFactory, CommonAvatarEnvironmentInterface environment,
         DRCRobotInitialSetup<SDFRobot> robotInitialSetup, DRCSCSInitialSetup scsInitialSetup, DRCGuiInitialSetup guiInitialSetup,
         GlobalDataProducer globalDataProducer)
   {
      this.globalDataProducer = globalDataProducer;
      
      simulatedDRCRobotTimeProvider = new SimulatedDRCRobotTimeProvider(drcRobotModel.getSimulateDT());

      boolean createCollisionMeshes = false;
      simulatedRobot = drcRobotModel.createSdfRobot(createCollisionMeshes);

      this.controllerFactory = controllerFactory;

      if(drcRobotModel.getLogSettings().isLog())
      {
         PeriodicThreadScheduler scheduler = new PeriodicNonRealtimeThreadScheduler("DRCSimulationYoVariableServer");
         yoVariableServer = new YoVariableServer(getClass(), scheduler, drcRobotModel.getLogModelProvider(), drcRobotModel.getLogSettings(), drcRobotModel.getEstimatorDT());
      }
      else
      {
         yoVariableServer = null;
      }

      Robot[] allSimulatedRobots = setupEnvironmentAndListSimulatedRobots(simulatedRobot, environment);
      
      SimulationConstructionSetParameters simulationConstructionSetParameters = guiInitialSetup.getSimulationConstructionSetParameters();
      simulationConstructionSetParameters.setDataBufferSize(scsInitialSetup.getSimulationDataBufferSize());
      scs = new SimulationConstructionSet(allSimulatedRobots, guiInitialSetup.getGraphics3DAdapter(), simulationConstructionSetParameters);
      scs.setDT(drcRobotModel.getSimulateDT(), 1);
      
      createRobotController(drcRobotModel, controllerFactory, globalDataProducer, simulatedRobot, scs, scsInitialSetup, robotInitialSetup);

      SimulatedRobotCenterOfMassVisualizer simulatedRobotCenterOfMassVisualizer = new SimulatedRobotCenterOfMassVisualizer(simulatedRobot, drcRobotModel.getSimulateDT());
      simulatedRobot.setController(simulatedRobotCenterOfMassVisualizer);
      
      simulatedRobot.setDynamicIntegrationMethod(scsInitialSetup.getDynamicIntegrationMethod());

      scsInitialSetup.initializeSimulation(scs);

      if (guiInitialSetup.isGuiShown())
      {
         VisualizerUtils.createOverheadPlotter(scs, guiInitialSetup.isShowOverheadView(), drcControllerThread.getDynamicGraphicObjectsListRegistry(),
               drcEstimatorThread.getDynamicGraphicObjectsListRegistry());
      }

      guiInitialSetup.initializeGUI(scs, simulatedRobot, drcRobotModel);

      if (environment != null && environment.getTerrainObject3D() != null)
      {
         scs.addStaticLinkGraphics(environment.getTerrainObject3D().getLinkGraphics());
      }

      scsInitialSetup.initializeRobot(simulatedRobot, drcRobotModel, null);
      robotInitialSetup.initializeRobot(simulatedRobot, drcRobotModel.getJointMap());
      simulatedRobot.update();

//      setupJointDamping(simulatedRobot, drcRobotModel);
   }

   private Robot[] setupEnvironmentAndListSimulatedRobots(SDFRobot simulatedRobot, CommonAvatarEnvironmentInterface environment)
   {
      List<Robot> allSimulatedRobotList = new ArrayList<Robot>();
      allSimulatedRobotList.add(simulatedRobot);
      if (environment != null && environment.getEnvironmentRobots() != null)
      {
         allSimulatedRobotList.addAll(environment.getEnvironmentRobots());

         environment.addContactPoints(simulatedRobot.getAllGroundContactPoints());
         environment.createAndSetContactControllerToARobot();
      }
      return allSimulatedRobotList.toArray(new Robot[0]);
   }

   public FullRobotModelCorruptor getFullRobotModelCorruptor()
   {
      return drcControllerThread.getFullRobotModelCorruptor();
   }
   
   private void createRobotController(DRCRobotModel drcRobotModel, MomentumBasedControllerFactory controllerFactory, GlobalDataProducer globalDataProducer,
         SDFRobot simulatedRobot, SimulationConstructionSet scs, DRCSCSInitialSetup scsInitialSetup,
         DRCRobotInitialSetup<SDFRobot> robotInitialSetup)
   {
      StateEstimatorParameters stateEstimatorParameters = drcRobotModel.getStateEstimatorParameters();

      SensorReaderFactory sensorReaderFactory;

      if (scsInitialSetup.usePerfectSensors())
      {
         sensorReaderFactory = new DRCPerfectSensorReaderFactory(simulatedRobot, stateEstimatorParameters.getEstimatorDT());
      }
      else
      {
         sensorReaderFactory = new SimulatedSensorHolderAndReaderFromRobotFactory(simulatedRobot, stateEstimatorParameters);
      }

      DRCRobotSensorInformation sensorInformation = drcRobotModel.getSensorInformation();

      if (scsInitialSetup.getRunMultiThreaded())
      {
         threadDataSynchronizer = new ThreadDataSynchronizer(drcRobotModel);
      }
      else
      {
         YoVariableRegistry threadDataSynchronizerRegistry = new YoVariableRegistry("ThreadDataSynchronizerRegistry");
         threadDataSynchronizer = new SingleThreadedThreadDataSynchronizer(scs, drcRobotModel, threadDataSynchronizerRegistry);
         scs.addYoVariableRegistry(threadDataSynchronizerRegistry);
      }

      DRCOutputWriter drcOutputWriter = new DRCSimulationOutputWriter(simulatedRobot);

      if (DO_SMOOTH_JOINT_TORQUES_AT_CONTROLLER_STATE_CHANGES)
      {
         DRCOutputWriterWithStateChangeSmoother drcOutputWriterWithStateChangeSmoother = new DRCOutputWriterWithStateChangeSmoother(drcOutputWriter);
         controllerFactory.attachControllerStateChangedListener(drcOutputWriterWithStateChangeSmoother.createControllerStateChangedListener());
         drcOutputWriter = drcOutputWriterWithStateChangeSmoother;
      }
 
      if (DO_SLOW_INTEGRATION_FOR_TORQUE_OFFSET)
      {
         DRCOutputWriterWithTorqueOffsets outputWriterWithTorqueOffsets = new DRCOutputWriterWithTorqueOffsets(drcOutputWriter, drcRobotModel.getControllerDT(), true);
         drcOutputWriter = outputWriterWithTorqueOffsets;
      }
      
      
      drcEstimatorThread = new DRCEstimatorThread(drcRobotModel.getSensorInformation(), drcRobotModel.getContactPointParameters(), drcRobotModel.getStateEstimatorParameters(),
    		  sensorReaderFactory, threadDataSynchronizer, globalDataProducer, yoVariableServer, gravity);
      
      PelvisPoseCorrectionCommunicatorInterface pelvisPoseCorrectionCommunicator = null;
      
      if (globalDataProducer != null)
      {
    	  pelvisPoseCorrectionCommunicator = new PelvisPoseCorrectionCommunicator(globalDataProducer);
    	  globalDataProducer.attachListener(StampedPosePacket.class, pelvisPoseCorrectionCommunicator);
      }
      drcEstimatorThread.setExternalPelvisCorrectorSubscriber(pelvisPoseCorrectionCommunicator);

      drcControllerThread = new DRCControllerThread(drcRobotModel, drcRobotModel.getSensorInformation(), controllerFactory, threadDataSynchronizer, drcOutputWriter,
            globalDataProducer, yoVariableServer, gravity, drcRobotModel.getEstimatorDT());
      

      if (scsInitialSetup.getRunMultiThreaded())
      {
         multiThreadedRobotController = new MultiThreadedRobotController("DRCSimulation", simulatedRobot, scs);
      }
      else
      {
         PrintTools.warn(this, "Running simulation in single threaded mode", true);
         multiThreadedRobotController = new SingleThreadedRobotController("DRCSimulation", simulatedRobot, scs);
      }
      int estimatorTicksPerSimulationTick = (int) Math.round(drcRobotModel.getEstimatorDT() / drcRobotModel.getSimulateDT());
      int controllerTicksPerSimulationTick = (int) Math.round(drcRobotModel.getControllerDT() / drcRobotModel.getSimulateDT());
      int slowPublisherTicksPerSimulationTick = (int) Math.round(10*drcRobotModel.getEstimatorDT() / drcRobotModel.getSimulateDT());

      multiThreadedRobotController.addController(drcEstimatorThread, estimatorTicksPerSimulationTick, false);
      multiThreadedRobotController.addController(drcControllerThread, controllerTicksPerSimulationTick, true);
      MultiThreadedRobotControlElement simulatedHandController = drcRobotModel.createSimulatedHandController(simulatedRobot, threadDataSynchronizer, globalDataProducer);
      if (simulatedHandController != null)
      {
         multiThreadedRobotController.addController(simulatedHandController, controllerTicksPerSimulationTick, true);
      }
      DRCRobotJointMap jointMap = drcRobotModel.getJointMap();
      if(jointMap.getHeadName() != null)
      {
         DRCSimulatedIMUPublisher drcSimulatedIMUPublisher = new DRCSimulatedIMUPublisher(globalDataProducer, drcEstimatorThread.getSimulatedIMUOutput(), jointMap.getHeadName());
         multiThreadedRobotController.addController(drcSimulatedIMUPublisher, slowPublisherTicksPerSimulationTick, false);
      }

      if (scsInitialSetup.getInitializeEstimatorToActual())
      {
         PrintTools.warn(this, "Initializing Estimator to Actual!", true);
         initializeEstimatorToActual(drcEstimatorThread, robotInitialSetup, simulatedRobot, jointMap);
      }

      simulatedRobot.setController(multiThreadedRobotController);
      DRCRobotLidarParameters lidarParams = sensorInformation.getLidarParameters(0);
      if(lidarParams != null && lidarParams.getLidarSpindleJointName() != null)
      {
         PIDLidarTorqueController lidarControllerInterface = new PIDLidarTorqueController(simulatedRobot,
               lidarParams.getLidarSpindleJointName(), lidarParams.getLidarSpindleVelocity(), drcRobotModel.getSimulateDT());
         simulatedRobot.setController(lidarControllerInterface);
      }

      String[] positionControlledJoints = jointMap.getPositionControlledJointsForSimulation();
      if (positionControlledJoints != null)
      {
         for (String jointName : positionControlledJoints)
         {
            OneDegreeOfFreedomJoint simulatedJoint = simulatedRobot.getOneDegreeOfFreedomJoint(jointName);
            SDFFullRobotModel controllerFullRobotModel = threadDataSynchronizer.getControllerFullRobotModel();
            OneDoFJoint controllerJoint = controllerFullRobotModel.getOneDoFJointByName(jointName);
            boolean isUpperBodyJoint = jointMap.getJointRole(jointName) != JointRole.LEG;
            
            JointLowLevelPositionControlSimulator positionControlSimulator = new JointLowLevelPositionControlSimulator(simulatedJoint, controllerJoint, isUpperBodyJoint, drcRobotModel.getSimulateDT());
            simulatedRobot.setController(positionControlSimulator);
         }
      }

      List<Pair<String, YoPDGains>> passiveJointNameWithGains = jointMap.getPassiveJointNameWithGains(simulatedRobot.getRobotsYoVariableRegistry());
      if (passiveJointNameWithGains != null)
      {
         for (int i = 0; i < passiveJointNameWithGains.size(); i++)
         {
            String jointName = passiveJointNameWithGains.get(i).first();
            OneDegreeOfFreedomJoint simulatedJoint = simulatedRobot.getOneDegreeOfFreedomJoint(jointName);
            YoPDGains gains = passiveJointNameWithGains.get(i).second();
            PassiveJointController passiveJointController = new PassiveJointController(simulatedJoint, gains);
            simulatedRobot.setController(passiveJointController);
         }
      }

      simulatedRobot.setController(simulatedDRCRobotTimeProvider);
   }
   
   private static void initializeEstimatorToActual(DRCEstimatorThread drcStateEstimator, DRCRobotInitialSetup<SDFRobot> robotInitialSetup,
         SDFRobot simulatedRobot, DRCRobotJointMap jointMap)
   {
      // The following is to get the initial CoM position from the robot. 
      // It is cheating for now, and we need to move to where the 
      // robot itself determines coordinates, and the sensors are all
      // in the robot-determined world coordinates..
      Point3d initialCoMPosition = new Point3d();
      robotInitialSetup.initializeRobot(simulatedRobot, jointMap);
      updateRobot(simulatedRobot);

      simulatedRobot.computeCenterOfMass(initialCoMPosition);

      Joint estimationJoint = simulatedRobot.getPelvisJoint();

      RigidBodyTransform estimationLinkTransform3D = estimationJoint.getJointTransform3D();
      Quat4d initialEstimationLinkOrientation = new Quat4d();
      estimationLinkTransform3D.get(initialEstimationLinkOrientation);

      if (drcStateEstimator != null)
         drcStateEstimator.initializeEstimatorToActual(initialCoMPosition, initialEstimationLinkOrientation);
   }

   private static void updateRobot(Robot robot)
   {
      try
      {
         robot.update();
         robot.doDynamicsButDoNotIntegrate();
         robot.update();
      }
      catch (UnreasonableAccelerationException e)
      {
         throw new RuntimeException("UnreasonableAccelerationException");
      }
   }

   public SimulationConstructionSet getSimulationConstructionSet()
   {
      return scs;
   }

   public MomentumBasedControllerFactory getControllerFactory()
   {
      return controllerFactory;
   }
   
   public void start()
   {
      if(yoVariableServer != null)
      {
         yoVariableServer.start();         
      }
      Thread simThread = new Thread(scs, "SCS simulation thread");
      simThread.start();
   }
   
   public void simulate()
   {
      scs.simulate();
   }

   public void dispose()
   {
      multiThreadedRobotController.stop();
      if (globalDataProducer != null) globalDataProducer.stop();
      drcEstimatorThread.dispose();
      drcEstimatorThread = null;
      drcControllerThread = null;
      multiThreadedRobotController = null;
      globalDataProducer = null;
   }

   public SDFRobot getRobot()
   {
      return simulatedRobot;
   }

   public TimestampProvider getTimeStampProvider()
   {
      return simulatedDRCRobotTimeProvider;
   }
   
   public void setExternalPelvisCorrectorSubscriber(PelvisPoseCorrectionCommunicatorInterface externalPelvisCorrectorSubscriber)
   {
      drcEstimatorThread.setExternalPelvisCorrectorSubscriber(externalPelvisCorrectorSubscriber);
   }
   
   public ThreadDataSynchronizerInterface getThreadDataSynchronizer()
   {
      return threadDataSynchronizer;
   }
   
   public SDFFullRobotModel getControllerFullRobotModel()
   {
      return threadDataSynchronizer.getControllerFullRobotModel();
   }
   
   
}
