package us.ihmc.darpaRoboticsChallenge;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.SdfLoader.SDFJointNameMap;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.commonAvatarInterfaces.CommonAvatarEnvironmentInterface;
import us.ihmc.commonWalkingControlModules.controllers.ControllerFactory;
import us.ihmc.commonWalkingControlModules.controllers.LidarControllerInterface;
import us.ihmc.commonWalkingControlModules.controllers.PIDLidarTorqueController;
import us.ihmc.commonWalkingControlModules.visualizer.RobotVisualizer;
import us.ihmc.darpaRoboticsChallenge.controllers.EstimationLinkHolder;
import us.ihmc.darpaRoboticsChallenge.controllers.concurrent.BlockingThreadSynchronizer;
import us.ihmc.darpaRoboticsChallenge.controllers.concurrent.ThreadSynchronizer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotDampingParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.handControl.DRCHandModel;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;
import us.ihmc.darpaRoboticsChallenge.outputs.DRCOutputWriter;
import us.ihmc.darpaRoboticsChallenge.outputs.DRCSimulationOutputWriter;
import us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap;
import us.ihmc.darpaRoboticsChallenge.ros.ROSSandiaJointMap;
import us.ihmc.darpaRoboticsChallenge.sensors.DRCPerfectSensorReaderFactory;
import us.ihmc.darpaRoboticsChallenge.stateEstimation.DRCSimulatedSensorNoiseParameters;
import us.ihmc.darpaRoboticsChallenge.stateEstimation.DRCStateEstimatorInterface;
import us.ihmc.projectM.R2Sim02.initialSetup.ScsInitialSetup;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.sensorProcessing.simulatedSensors.GroundContactPointBasedWrenchCalculator;
import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReaderFactory;
import us.ihmc.sensorProcessing.simulatedSensors.SimulatedSensorHolderAndReaderFromRobotFactory;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimator;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.RunnableRunnerController;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.StateEstimatorErrorCalculatorController;
import us.ihmc.util.NonRealtimeThreadFactory;
import us.ihmc.util.ThreadFactory;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.io.streamingData.GlobalDataProducer;

import com.yobotics.simulationconstructionset.GroundContactPoint;
import com.yobotics.simulationconstructionset.IMUMount;
import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.UnreasonableAccelerationException;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.gui.GUISetterUpperRegistry;
import com.yobotics.simulationconstructionset.robotController.ModularRobotController;
import com.yobotics.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;

public class DRCSimulationFactory
{
   private static final boolean COMPUTE_ESTIMATOR_ERROR = true;

   public static Pair<HumanoidRobotSimulation<SDFRobot>, DRCController> createSimulation(ControllerFactory controllerFactory,
         CommonAvatarEnvironmentInterface commonAvatarEnvironmentInterface, DRCRobotInterface robotInterface, DRCRobotInitialSetup<SDFRobot> robotInitialSetup,
         ScsInitialSetup scsInitialSetup, DRCGuiInitialSetup guiInitialSetup, GlobalDataProducer dataProducer, RobotVisualizer robotVisualizer,
         DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry, DRCRobotModel drcRobotModel)
   {
      GUISetterUpperRegistry guiSetterUpperRegistry = new GUISetterUpperRegistry();

      SDFJointNameMap jointMap = robotInterface.getJointMap();
      DRCRobotJointMap map = drcRobotModel.getJointMap(false, false);


      double estimateDT = DRCConfigParameters.ESTIMATOR_DT;
      double simulateDT = robotInterface.getSimulateDT();
      double controlDT = controllerFactory.getControlDT();
      int estimationTicksPerControlTick = (int) (estimateDT / simulateDT);

      SDFRobot simulatedRobot = robotInterface.getRobot();
      YoVariableRegistry registry = simulatedRobot.getRobotsYoVariableRegistry();
      
      setupJointDamping(simulatedRobot);

      DRCOutputWriter drcOutputWriter = new DRCSimulationOutputWriter(simulatedRobot, registry, dynamicGraphicObjectsListRegistry, robotVisualizer);
      
      if (DRCConfigParameters.LIMIT_CONTROLLER_OUTPUT_TORQUES)
      {
//         drcOutputWriter = new DRCOutputWriterWithTorqueLimits(drcOutputWriter);
         throw new RuntimeException("Atlas torque limits are under NDA");
      }
      
      // TODO: Build LIDAR here
      LidarControllerInterface lidarControllerInterface;

      SensorNoiseParameters sensorNoiseParameters;

      lidarControllerInterface = new PIDLidarTorqueController(DRCConfigParameters.LIDAR_SPINDLE_VELOCITY, controlDT);
      sensorNoiseParameters = DRCSimulatedSensorNoiseParameters.createSensorNoiseParametersZeroNoise();
//         sensorNoiseParameters = DRCSimulatedSensorNoiseParameters.createSensorNoiseParametersALittleNoise();
//         sensorNoiseParameters = DRCSimulatedSensorNoiseParameters.createSensorNoiseParametersGazeboSDF();

      //    SCSToInverseDynamicsJointMap scsToInverseDynamicsJointMapForEstimator;
      //    scsToInverseDynamicsJointMapForEstimator = robotInterface.getSCSToInverseDynamicsJointMap();

      ArrayList<IMUMount> imuMounts = new ArrayList<IMUMount>();
      ArrayList<IMUMount> allIMUMounts = new ArrayList<IMUMount>();
      simulatedRobot.getIMUMounts(allIMUMounts);

      for (IMUMount imuMount : allIMUMounts)
      {
         // Only add the main one now. Not the head one.
         //                   if (imuMount.getName().equals("head_imu_sensor")) imuMounts.add(imuMount);
         for (String imuSensorName : jointMap.getIMUSensorsToUse())
         {
            if (imuMount.getName().equals(imuSensorName))
            {
               imuMounts.add(imuMount);
            }
         }
      }

      ArrayList<OneDegreeOfFreedomJoint> forceTorqueSensorJoints = new ArrayList<OneDegreeOfFreedomJoint>();
      // TODO: Get from SDF file

      for (String name : map.getForceSensorNames())
      {
         forceTorqueSensorJoints.add(simulatedRobot.getOneDoFJoint(name));
      }

      ArrayList<WrenchCalculatorInterface> wrenchProviders = new ArrayList<WrenchCalculatorInterface>();

      for (OneDegreeOfFreedomJoint sensorJoint : forceTorqueSensorJoints)
      {
         ArrayList<GroundContactPoint> groundContactPoints = new ArrayList<GroundContactPoint>();
         sensorJoint.recursiveGetAllGroundContactPoints(groundContactPoints);
         GroundContactPointBasedWrenchCalculator groundContactPointBasedWrenchCalculator = new GroundContactPointBasedWrenchCalculator(groundContactPoints,
               sensorJoint);
         wrenchProviders.add(groundContactPointBasedWrenchCalculator);
      }

      SensorReaderFactory sensorReaderFactory; // this is the connection between the ModularRobotController and the DRCController below
      ModularRobotController controller = new ModularRobotController("SensorReaders");
      if (DRCConfigParameters.USE_PERFECT_SENSORS)
      {
         DRCPerfectSensorReaderFactory drcPerfectSensorReaderFactory = new DRCPerfectSensorReaderFactory(simulatedRobot, wrenchProviders, estimateDT);
         controller.addRobotController(drcPerfectSensorReaderFactory.getSensorReader());
         sensorReaderFactory = drcPerfectSensorReaderFactory;
      }
      else
      {
         double filterFreqHz = DRCConfigParameters.JOINT_VELOCITY_FILTER_FREQ_HZ;
         double slopTime = DRCConfigParameters.JOINT_VELOCITY_SLOP_TIME_FOR_BACKLASH_COMPENSATION;

         SimulatedSensorHolderAndReaderFromRobotFactory simulatedSensorHolderAndReaderFromRobotFactory = new SimulatedSensorHolderAndReaderFromRobotFactory(
               simulatedRobot, sensorNoiseParameters, estimateDT, filterFreqHz, slopTime, imuMounts, wrenchProviders, registry);
         controller.addRobotController(new RunnableRunnerController(simulatedSensorHolderAndReaderFromRobotFactory.getSensorReader()));
         sensorReaderFactory = simulatedSensorHolderAndReaderFromRobotFactory;
      }

//      SimulatedHandControllerDispatcher handControllerDispatcher = new SimulatedHandControllerDispatcher(simulatedRobot, robotInterface.getTimeStampProvider(),
//            estimationTicksPerControlTick);
//      controller.addRobotController(handControllerDispatcher);

      controller.addRobotController(lidarControllerInterface);

      double gravity = robotInterface.getRobot().getGravityZ();
     
      ThreadFactory threadFactory = new NonRealtimeThreadFactory();
      ThreadSynchronizer threadSynchronizer = new BlockingThreadSynchronizer();

      DRCController robotController = new DRCController(robotInterface.getFullRobotModelFactory(), controllerFactory, sensorReaderFactory, drcOutputWriter,
            jointMap, lidarControllerInterface, gravity, estimateDT, controlDT, dataProducer, robotInterface.getTimeStampProvider(),
            dynamicGraphicObjectsListRegistry, guiSetterUpperRegistry, registry, null, threadFactory, threadSynchronizer, drcRobotModel);
      robotController.initialize();

      final HumanoidRobotSimulation<SDFRobot> humanoidRobotSimulation = new HumanoidRobotSimulation<SDFRobot>(simulatedRobot, controller,
            estimationTicksPerControlTick, commonAvatarEnvironmentInterface, simulatedRobot.getAllExternalForcePoints(), robotInitialSetup, scsInitialSetup,
            guiInitialSetup, guiSetterUpperRegistry, dynamicGraphicObjectsListRegistry, drcRobotModel);

      //TODO: Can only do this if we have a simulation...
      if (scsInitialSetup.getInitializeEstimatorToActual())
      {
         System.err.println("Warning! Initializing Estimator to Actual!");
         DRCStateEstimatorInterface drcStateEstimator = robotController.getDRCStateEstimator();
         initializeEstimatorToActual(drcStateEstimator, robotInitialSetup, simulatedRobot, drcRobotModel.getJointMap(false, false));
      }

      if (COMPUTE_ESTIMATOR_ERROR && robotController.getDRCStateEstimator() != null)
      {
         DRCStateEstimatorInterface drcStateEstimator = robotController.getDRCStateEstimator();
         StateEstimator stateEstimator = drcStateEstimator.getStateEstimator();

         Joint estimationJoint = getEstimationJoint(simulatedRobot);

         StateEstimatorErrorCalculatorController stateEstimatorErrorCalculatorController = new StateEstimatorErrorCalculatorController(stateEstimator,
               simulatedRobot, estimationJoint, DRCConfigParameters.ASSUME_PERFECT_IMU, DRCConfigParameters.USE_SIMPLE_PELVIS_POSITION_ESTIMATOR);
         simulatedRobot.setController(stateEstimatorErrorCalculatorController, estimationTicksPerControlTick);
      }

      return new Pair<HumanoidRobotSimulation<SDFRobot>, DRCController>(humanoidRobotSimulation, robotController);
   }

   private static Joint getEstimationJoint(SDFRobot simulatedRobot)
   {
      Joint estimationJoint;
      if (EstimationLinkHolder.usingChestLink())
      {
         estimationJoint = simulatedRobot.getJoint(ROSAtlasJointMap.jointNames[ROSAtlasJointMap.back_bkx]);
         if (estimationJoint == null)
            throw new RuntimeException("Couldn't find chest joint!");
      }
      else
      {
         estimationJoint = simulatedRobot.getPelvisJoint();
      }
      return estimationJoint;
   }

   private static void setupJointDamping(SDFRobot simulatedRobot)
   {
//      for (int i = 0; i < ROSAtlasJointMap.numberOfJoints; i++)
//      {
//         simulatedRobot.getOneDoFJoint(ROSAtlasJointMap.jointNames[i]).setDamping(DRCRobotDampingParameters.getAtlasDamping(i));
//      }

      for (RobotSide robotSide : RobotSide.values)
      {
    	 {
    		 if(DRCLocalConfigParameters.robotModelToUse.getHandModel() == DRCHandModel.SANDIA)
	         for (int i = 0; i < ROSSandiaJointMap.numberOfJointsPerHand; i++)
	         {
	            try
	            {
	               simulatedRobot.getOneDoFJoint(ROSSandiaJointMap.handNames.get(robotSide)[i]).setDamping(
	                     DRCRobotDampingParameters.getSandiaHandDamping(robotSide, i));
	            }
	            catch (NullPointerException e)
	            {
	               System.err.println("NullPointerException for the joint: " + ROSSandiaJointMap.handNames.get(robotSide)[i]);
	            }
	         }
    	 }
      }
   }

   private static void initializeEstimatorToActual(DRCStateEstimatorInterface drcStateEstimator, DRCRobotInitialSetup<SDFRobot> robotInitialSetup, SDFRobot simulatedRobot, DRCRobotJointMap jointMap)
   {
      // The following is to get the initial CoM position from the robot. 
      // It is cheating for now, and we need to move to where the 
      // robot itself determines coordinates, and the sensors are all
      // in the robot-determined world coordinates..
      Point3d initialCoMPosition = new Point3d();
      robotInitialSetup.initializeRobot(simulatedRobot, jointMap);
      updateRobot(simulatedRobot);

      simulatedRobot.computeCenterOfMass(initialCoMPosition);

      Joint estimationJoint = getEstimationJoint(simulatedRobot);

      Transform3D estimationLinkTransform3D = estimationJoint.getJointTransform3D();
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

   //   public static DesiredCoMAccelerationsFromRobotStealerController createAndAddDesiredCoMAccelerationFromRobotStealerController(ReferenceFrame estimationFrame,
   //         double controlDT, int simulationTicksPerControlTick, SDFRobot simulatedRobot, ArrayList<RobotControllerAndParameters> robotControllersAndParameters)
   //   {
   //      InverseDynamicsJointsFromSCSRobotGenerator generator = new InverseDynamicsJointsFromSCSRobotGenerator(simulatedRobot);
   //
   //      // TODO: Better way to get estimationJoint
   //      Joint estimationJoint = simulatedRobot.getRootJoints().get(0);
   //      double comAccelerationProcessNoiseStandardDeviation = Math.sqrt(1e-1);
   //      double angularAccelerationProcessNoiseStandardDeviation = Math.sqrt(1e-1);
   //
   //      DesiredCoMAccelerationsFromRobotStealerController desiredCoMAccelerationsFromRobotStealerController = new DesiredCoMAccelerationsFromRobotStealerController(
   //            estimationFrame, comAccelerationProcessNoiseStandardDeviation, angularAccelerationProcessNoiseStandardDeviation, generator, estimationJoint,
   //            controlDT);
   //
   //      RobotControllerAndParameters desiredCoMAccelerationsFromRobotStealerControllerAndParameters = new RobotControllerAndParameters(
   //            desiredCoMAccelerationsFromRobotStealerController, simulationTicksPerControlTick);
   //      robotControllersAndParameters.add(desiredCoMAccelerationsFromRobotStealerControllerAndParameters);
   //
   //      return desiredCoMAccelerationsFromRobotStealerController;
   //   }

}
