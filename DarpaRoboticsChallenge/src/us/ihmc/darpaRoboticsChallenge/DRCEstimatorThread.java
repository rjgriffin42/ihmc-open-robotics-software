package us.ihmc.darpaRoboticsChallenge;

import java.util.Arrays;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ContactableBodiesFactory;
import us.ihmc.commonWalkingControlModules.referenceFrames.ReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.WrenchBasedFootSwitch;
import us.ihmc.commonWalkingControlModules.visualizer.RobotVisualizer;
import us.ihmc.communication.packets.StampedPosePacket;
import us.ihmc.communication.subscribers.ExternalPelvisPoseSubscriberInterface;
import us.ihmc.communication.subscribers.ExternalTimeStampedPoseSubscriber;
import us.ihmc.darpaRoboticsChallenge.controllers.concurrent.ThreadDataSynchronizer;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotContactPointParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotSensorInformation;
import us.ihmc.darpaRoboticsChallenge.networking.dataProducers.JointConfigurationGatherer;
import us.ihmc.darpaRoboticsChallenge.sensors.RobotJointLimitWatcher;
import us.ihmc.darpaRoboticsChallenge.stateEstimation.kinematicsBasedStateEstimator.DRCKinematicsBasedStateEstimator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensors.ForceSensorData;
import us.ihmc.sensorProcessing.sensors.ForceSensorDataHolder;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReaderFactory;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.ForceSensorDefinition;
import us.ihmc.utilities.IMUDefinition;
import us.ihmc.utilities.io.streamingData.GlobalDataProducer;
import us.ihmc.utilities.math.TimeTools;
import us.ihmc.utilities.net.AtomicSettableTimestampProvider;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.screwTheory.TotalMassCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicsListRegistry;

import com.yobotics.simulationconstructionset.robotController.ModularRobotController;
import com.yobotics.simulationconstructionset.robotController.MultiThreadedRobotControlElement;
import com.yobotics.simulationconstructionset.time.ExecutionTimer;

public class DRCEstimatorThread implements MultiThreadedRobotControlElement
{
   private final YoVariableRegistry estimatorRegistry = new YoVariableRegistry("DRCEstimatorThread");
   private final RobotVisualizer robotVisualizer;
   private final SDFFullRobotModel estimatorFullRobotModel;
   private final ForceSensorDataHolder forceSensorDataHolderForEstimator;
   private final ModularRobotController estimatorController;
   private final YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
   private final DRCKinematicsBasedStateEstimator drcStateEstimator;

   private final ThreadDataSynchronizer threadDataSynchronizer;
   private final SensorReader sensorReader;

   private final DoubleYoVariable estimatorTime = new DoubleYoVariable("estimatorTime", estimatorRegistry);
   private final LongYoVariable estimatorTick = new LongYoVariable("estimatorTick", estimatorRegistry);
   private final BooleanYoVariable firstTick = new BooleanYoVariable("firstTick", estimatorRegistry);
   
   private final LongYoVariable startClockTime = new LongYoVariable("startTime", estimatorRegistry);
   private final ExecutionTimer estimatorTimer = new ExecutionTimer("estimatorTimer", 10.0, estimatorRegistry);
   
   private final LongYoVariable actualEstimatorDT = new LongYoVariable("actualEstimatorDT", estimatorRegistry);
   
   private final AtomicSettableTimestampProvider timestampProvider = new AtomicSettableTimestampProvider();

   public DRCEstimatorThread(DRCRobotModel robotModel, SensorReaderFactory sensorReaderFactory, ThreadDataSynchronizer threadDataSynchronizer,
         GlobalDataProducer dataProducer, RobotVisualizer robotVisualizer, double gravity)
   {
      this.threadDataSynchronizer = threadDataSynchronizer;
      this.robotVisualizer = robotVisualizer;
      estimatorFullRobotModel = threadDataSynchronizer.getEstimatorFullRobotModel();
      forceSensorDataHolderForEstimator = threadDataSynchronizer.getEstimatorForceSensorDataHolder();
      
      DRCRobotSensorInformation sensorInformation = robotModel.getSensorInformation();
      DRCRobotContactPointParameters contactPointParamaters = robotModel.getContactPointParameters();
      StateEstimatorParameters stateEstimatorParameters = robotModel.getStateEstimatorParameters();

      final List<String> imuSensorsToUse = Arrays.asList(sensorInformation.getIMUSensorsToUse());
      IMUDefinition[] imuDefinitions = new IMUDefinition[imuSensorsToUse.size()];
      int index = 0;
      for (IMUDefinition imuDefinition : estimatorFullRobotModel.getIMUDefinitions())
      {
         if (imuSensorsToUse.contains(imuDefinition.getName()))
         {
            imuDefinitions[index++] = imuDefinition;
         }
      }

      sensorReaderFactory.build(estimatorFullRobotModel.getRootJoint(), imuDefinitions, estimatorFullRobotModel.getForceSensorDefinitions(),
            forceSensorDataHolderForEstimator, threadDataSynchronizer.getEstimatorRawJointSensorDataHolderMap(), estimatorRegistry);
      sensorReader = sensorReaderFactory.getSensorReader();
      
      estimatorController = new ModularRobotController("EstimatorController");
      if (sensorReaderFactory.useStateEstimator())
      {
         SensorOutputMapReadOnly sensorOutputMapReadOnly = sensorReader.getSensorOutputMapReadOnly();
         drcStateEstimator = createStateEstimator(estimatorFullRobotModel, robotModel, sensorOutputMapReadOnly, gravity, stateEstimatorParameters,
               contactPointParamaters, forceSensorDataHolderForEstimator, yoGraphicsListRegistry, estimatorRegistry, dataProducer, timestampProvider);
         estimatorController.addRobotController(drcStateEstimator);
      }
      else
      {
         drcStateEstimator = null;
      }

      RobotJointLimitWatcher robotJointLimitWatcher = new RobotJointLimitWatcher(estimatorFullRobotModel.getOneDoFJoints());
      estimatorController.addRobotController(robotJointLimitWatcher);

      for(ForceSensorDefinition forceSensorDefinition:forceSensorDataHolderForEstimator.getForceSensorDefinitions())
      {
        ForceSensorToJointTorqueProjector footSensorToJointTorqueProjector = new ForceSensorToJointTorqueProjector(
              forceSensorDefinition.getSensorName(), 
              forceSensorDataHolderForEstimator.get(forceSensorDefinition), 
              forceSensorDefinition.getRigidBody());
        estimatorController.addRobotController(footSensorToJointTorqueProjector);
      }
            
      
      if (dataProducer != null)
      {
         ObjectCommunicator objectCommunicator = dataProducer.getObjectCommunicator();
         JointConfigurationGatherer jointConfigurationGathererAndProducer = new JointConfigurationGatherer(estimatorFullRobotModel);
         
         estimatorController.setRawOutputWriter(new DRCPoseCommunicator(estimatorFullRobotModel, jointConfigurationGathererAndProducer,
               objectCommunicator, timestampProvider, sensorInformation));
      }
      
      firstTick.set(true);
      estimatorRegistry.addChild(estimatorController.getYoVariableRegistry());
      
      if(robotVisualizer != null)
      {
         robotVisualizer.setMainRegistry(estimatorRegistry, estimatorFullRobotModel, yoGraphicsListRegistry);
      }
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return estimatorRegistry;
   }

   @Override
   public String getName()
   {
      return estimatorRegistry.getName();
   }

   @Override
   public void read(double time, long currentClockTime, long sensorTime)
   {
      actualEstimatorDT.set(currentClockTime - startClockTime.getLongValue());
      estimatorTime.set(time);
      startClockTime.set(currentClockTime);
      sensorReader.read();
      timestampProvider.set(sensorTime);
   }

   @Override
   public void run()
   {
      if(firstTick.getBooleanValue())
      {
         estimatorController.initialize();
         firstTick.set(false);
      }
      
      estimatorTimer.startMeasurement();
      estimatorController.doControl();
      estimatorTimer.stopMeasurement();
   }

   @Override
   public void write(long timestamp)
   {
      long startTimestamp = TimeTools.secondsToNanoSeconds(estimatorTime.getDoubleValue());
      threadDataSynchronizer.publishEstimatorState(startTimestamp, estimatorTick.getLongValue(), startClockTime.getLongValue());
      if(robotVisualizer != null)
      {
         robotVisualizer.update(startTimestamp, estimatorRegistry);
      }
      estimatorTick.increment();
   }

   public static DRCKinematicsBasedStateEstimator createStateEstimator(SDFFullRobotModel estimatorFullRobotModel, DRCRobotModel drcRobotModel,
         SensorOutputMapReadOnly sensorOutputMapReadOnly, double gravity, StateEstimatorParameters stateEstimatorParameters,
         DRCRobotContactPointParameters contactPointParamaters, ForceSensorDataHolder forceSensorDataHolderForEstimator,
         YoGraphicsListRegistry yoGraphicsListRegistry, YoVariableRegistry registry, GlobalDataProducer dataProducer, AtomicSettableTimestampProvider timestampProvider)
   {
      DRCRobotJointMap jointMap = drcRobotModel.getJointMap();
      FullInverseDynamicsStructure inverseDynamicsStructure = DRCControllerThread.createInverseDynamicsStructure(estimatorFullRobotModel);

      ReferenceFrames estimatorReferenceFrames = new ReferenceFrames(estimatorFullRobotModel);
      ContactableBodiesFactory contactableBodiesFactory = jointMap.getContactPointParameters().getContactableBodiesFactory();
      SideDependentList<ContactablePlaneBody> bipedFeet = contactableBodiesFactory.createFootContactableBodies(estimatorFullRobotModel, estimatorReferenceFrames);

      double gravityMagnitude = Math.abs(gravity);
      double totalRobotWeight = TotalMassCalculator.computeSubTreeMass(estimatorFullRobotModel.getElevator()) * gravityMagnitude;

      SideDependentList<WrenchBasedFootSwitch> footSwitchesForEstimator = new SideDependentList<WrenchBasedFootSwitch>();
      for (RobotSide robotSide : RobotSide.values)
      {
         String footForceSensorName = drcRobotModel.getSensorInformation().getFeetForceSensorNames().get(robotSide);
         ForceSensorData footForceSensorForEstimator = forceSensorDataHolderForEstimator.getByName(footForceSensorName);
         String namePrefix = bipedFeet.get(robotSide).getName() + "StateEstimator";

         //         double footSwitchCoPThresholdFraction = 0.01;
         WalkingControllerParameters walkingControllerParameters = drcRobotModel.getWalkingControllerParameters();
         double footSwitchCoPThresholdFraction = walkingControllerParameters.getFootSwitchCoPThresholdFraction();
         double contactTresholdForce = stateEstimatorParameters.getContactThresholdForce();
         WrenchBasedFootSwitch wrenchBasedFootSwitchForEstimator = new WrenchBasedFootSwitch(namePrefix, footForceSensorForEstimator,
               footSwitchCoPThresholdFraction, totalRobotWeight, bipedFeet.get(robotSide), null, contactTresholdForce, registry);
         footSwitchesForEstimator.put(robotSide, wrenchBasedFootSwitchForEstimator);
      }
      
      ExternalPelvisPoseSubscriberInterface externalPelvisPoseSubscriber = null;
      
      if (dataProducer != null)
      {
         externalPelvisPoseSubscriber = new ExternalTimeStampedPoseSubscriber();
         dataProducer.attachListener(StampedPosePacket.class, externalPelvisPoseSubscriber);
      }
      
      // Create the sensor readers and state estimator here:
      DRCKinematicsBasedStateEstimator drcStateEstimator = new DRCKinematicsBasedStateEstimator(inverseDynamicsStructure, stateEstimatorParameters,
            sensorOutputMapReadOnly, gravityMagnitude, footSwitchesForEstimator, bipedFeet, yoGraphicsListRegistry, externalPelvisPoseSubscriber, timestampProvider);

      return drcStateEstimator;
   }

   @Override
   public YoGraphicsListRegistry getDynamicGraphicObjectsListRegistry()
   {
      return yoGraphicsListRegistry;
   }

   @Override
   public long nextWakeupTime()
   {
      throw new RuntimeException("Estimator thread should not wake up based on clock");
   }

   public void initializeEstimatorToActual(Point3d initialCoMPosition, Quat4d initialEstimationLinkOrientation)
   {
      drcStateEstimator.initializeEstimatorToActual(initialCoMPosition, initialEstimationLinkOrientation);
   }
}
