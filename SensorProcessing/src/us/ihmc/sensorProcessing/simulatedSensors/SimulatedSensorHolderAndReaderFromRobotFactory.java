package us.ihmc.sensorProcessing.simulatedSensors;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.vecmath.Vector3d;

import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.sensorProcessing.signalCorruption.GaussianDoubleCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.GaussianOrientationCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.GaussianVectorCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.LatencyVectorCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.OrientationConstantAcceleratingYawDriftCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.OrientationLatencyCorruptor;
import us.ihmc.sensorProcessing.signalCorruption.RandomWalkBiasVectorCorruptor;
import us.ihmc.utilities.IMUDefinition;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDataHolder;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDefinition;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.SixDoFJoint;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

import com.yobotics.simulationconstructionset.FloatingJoint;
import com.yobotics.simulationconstructionset.IMUMount;
import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.simulatedSensors.WrenchCalculatorInterface;

public class SimulatedSensorHolderAndReaderFromRobotFactory implements SensorReaderFactory
{
   private final YoVariableRegistry registry = new YoVariableRegistry("SensorReaderFactory");
   private final Robot robot;
   private final double estimateDT;
   private final SensorNoiseParameters sensorNoiseParameters;
   
   private final ArrayList<IMUMount> imuMounts = new ArrayList<IMUMount>();
   private final ArrayList<WrenchCalculatorInterface> groundContactPointBasedWrenchCalculators = new ArrayList<WrenchCalculatorInterface>();

   private SimulatedSensorHolderAndReader simulatedSensorHolderAndReader;
   private StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions;
   private final SensorFilterParameters sensorFilterParameters;
   
   public SimulatedSensorHolderAndReaderFromRobotFactory(Robot robot, SensorNoiseParameters sensorNoiseParameters,
         SensorFilterParameters sensorFilterParameters)
   {
      this.robot = robot;
      this.sensorNoiseParameters = sensorNoiseParameters;
      this.sensorFilterParameters = sensorFilterParameters;
      
      this.estimateDT = sensorFilterParameters.getEstimatorDT();

      robot.getIMUMounts(imuMounts);
      robot.getForceSensors(groundContactPointBasedWrenchCalculators);
   }

   public void build(SixDoFJoint rootJoint, IMUDefinition[] imuDefinition, ForceSensorDefinition[] forceSensorDefinitions,
         ForceSensorDataHolder forceSensorDataHolderForEstimator, RawJointSensorDataHolderMap rawJointSensorDataHolderMap, YoVariableRegistry parentRegistry)
   {
      ArrayList<Joint> rootJoints = robot.getRootJoints();
      if (rootJoints.size() > 1)
      {
         throw new RuntimeException("Robot has more than 1 rootJoint");
      }

      final Joint scsRootJoint = rootJoints.get(0);
      if (!(scsRootJoint instanceof FloatingJoint))
         throw new RuntimeException("Not FloatingJoint rootjoint found");

      SCSToInverseDynamicsJointMap scsToInverseDynamicsJointMap = SCSToInverseDynamicsJointMap.createByName((FloatingJoint) scsRootJoint, rootJoint);
      StateEstimatorSensorDefinitionsFromRobotFactory stateEstimatorSensorDefinitionsFromRobotFactory = new StateEstimatorSensorDefinitionsFromRobotFactory(
            scsToInverseDynamicsJointMap, imuMounts, groundContactPointBasedWrenchCalculators);

      this.stateEstimatorSensorDefinitions = stateEstimatorSensorDefinitionsFromRobotFactory.getStateEstimatorSensorDefinitions();
      Map<IMUMount, IMUDefinition> imuDefinitions = stateEstimatorSensorDefinitionsFromRobotFactory.getIMUDefinitions();
      Map<WrenchCalculatorInterface, ForceSensorDefinition> forceSensors = stateEstimatorSensorDefinitionsFromRobotFactory.getForceSensorDefinitions();
      this.simulatedSensorHolderAndReader = new SimulatedSensorHolderAndReader(sensorFilterParameters, stateEstimatorSensorDefinitions, forceSensorDataHolderForEstimator, sensorNoiseParameters, robot.getYoTime(), registry);

      createAndAddOrientationSensors(imuDefinitions, registry);
      createAndAddAngularVelocitySensors(imuDefinitions, registry);
      createAndAddForceSensors(forceSensors, registry);
      createAndAddLinearAccelerationSensors(imuDefinitions, registry);
      createAndAddOneDoFPositionAndVelocitySensors(scsToInverseDynamicsJointMap);

      parentRegistry.addChild(registry);
   }

   private void createAndAddForceSensors(Map<WrenchCalculatorInterface, ForceSensorDefinition> forceSensorDefinitions2, YoVariableRegistry registry)
   {
      for(Entry<WrenchCalculatorInterface, ForceSensorDefinition> forceSensorDefinitionEntry : forceSensorDefinitions2.entrySet())
      {         
         WrenchCalculatorInterface groundContactPointBasedWrenchCalculator = forceSensorDefinitionEntry.getKey();
         simulatedSensorHolderAndReader.addForceTorqueSensorPort(forceSensorDefinitionEntry.getValue(), groundContactPointBasedWrenchCalculator);
      }
   }

   public SimulatedSensorHolderAndReader getSensorReader()
   {
      return simulatedSensorHolderAndReader;
   }

   private void createAndAddOneDoFPositionAndVelocitySensors(SCSToInverseDynamicsJointMap scsToInverseDynamicsJointMap)
   {
      ArrayList<OneDegreeOfFreedomJoint> oneDegreeOfFreedomJoints = new ArrayList<OneDegreeOfFreedomJoint>(scsToInverseDynamicsJointMap.getSCSOneDegreeOfFreedomJoints());

      long seed = 18735L;
      
      for (OneDegreeOfFreedomJoint oneDegreeOfFreedomJoint : oneDegreeOfFreedomJoints)
      {
         OneDoFJoint oneDoFJoint = scsToInverseDynamicsJointMap.getInverseDynamicsOneDoFJoint(oneDegreeOfFreedomJoint);

         SimulatedOneDoFJointPositionSensor positionSensor = new SimulatedOneDoFJointPositionSensor(oneDegreeOfFreedomJoint.getName(), oneDegreeOfFreedomJoint);
         SimulatedOneDoFJointVelocitySensor velocitySensor = new SimulatedOneDoFJointVelocitySensor(oneDegreeOfFreedomJoint.getName(), oneDegreeOfFreedomJoint);
         SimulatedOneDoFJointTorqueSensor torqueSensor = new SimulatedOneDoFJointTorqueSensor(oneDegreeOfFreedomJoint.getName(), oneDegreeOfFreedomJoint);

//         if (oneDegreeOfFreedomJoint.getName().contains("bky"))
//         {
//            SignalCorruptor<MutableDouble> backVelocityLatencyCorruptor = new LatencyDoubleCorruptor("backLatency", 100, registry);
//            
//            velocitySensor.addSignalCorruptor(backVelocityLatencyCorruptor );
//         }
         
         if (sensorNoiseParameters != null)
         {
            GaussianDoubleCorruptor jointPositionCorruptor = new GaussianDoubleCorruptor(seed, "posNoise" + oneDegreeOfFreedomJoint.getName(), registry);
            double positionMeasurementStandardDeviation = sensorNoiseParameters.getJointPositionMeasurementStandardDeviation();
            jointPositionCorruptor.setStandardDeviation(positionMeasurementStandardDeviation);
            positionSensor.addSignalCorruptor(jointPositionCorruptor);
            seed = seed + 10;
            
            GaussianDoubleCorruptor jointVelocityCorruptor = new GaussianDoubleCorruptor(17735L, "velNoise" + oneDegreeOfFreedomJoint.getName(), registry);
            double velocityMeasurementStandardDeviation = sensorNoiseParameters.getJointVelocityMeasurementStandardDeviation();
            jointVelocityCorruptor.setStandardDeviation(velocityMeasurementStandardDeviation);
            velocitySensor.addSignalCorruptor(jointVelocityCorruptor);
            seed = seed + 10;
         }
         
         simulatedSensorHolderAndReader.addJointPositionSensorPort(oneDoFJoint, positionSensor);
         simulatedSensorHolderAndReader.addJointVelocitySensorPort(oneDoFJoint, velocitySensor);
         simulatedSensorHolderAndReader.addJointTorqueSensorPort(oneDoFJoint, torqueSensor);
      }
   }

   private void createAndAddOrientationSensors(Map<IMUMount, IMUDefinition> imuDefinitions, YoVariableRegistry registry)
   {
      Set<IMUMount> imuMounts = imuDefinitions.keySet();

      for (IMUMount imuMount : imuMounts)
      {
         String sensorName = imuMount.getName() + "Orientation";
         IMUDefinition imuDefinition = imuDefinitions.get(imuMount);

         SimulatedOrientationSensorFromRobot orientationSensor = new SimulatedOrientationSensorFromRobot(sensorName, imuMount, registry);

         if (sensorNoiseParameters != null)
         {
            double yawDriftAcceleration = sensorNoiseParameters.getIMUYawDriftAcceleration();
            OrientationConstantAcceleratingYawDriftCorruptor yawDriftCorruptor = new OrientationConstantAcceleratingYawDriftCorruptor(sensorName, estimateDT, registry);
            yawDriftCorruptor.setYawDriftAcceleration(yawDriftAcceleration);
            orientationSensor.addSignalCorruptor(yawDriftCorruptor);

            double orientationMeasurementStandardDeviation = sensorNoiseParameters.getOrientationMeasurementStandardDeviation();
            GaussianOrientationCorruptor orientationCorruptor = new GaussianOrientationCorruptor(sensorName, 12334255L, registry);
            orientationCorruptor.setStandardDeviation(orientationMeasurementStandardDeviation);
            orientationSensor.addSignalCorruptor(orientationCorruptor);
            
            double orientationMeasurementLatency = sensorNoiseParameters.getOrientationMeasurementLatency();
            int latencyTicks = (int) Math.round(orientationMeasurementLatency / estimateDT);
            OrientationLatencyCorruptor orientationLatencyCorruptor = new OrientationLatencyCorruptor(sensorName, latencyTicks, registry);
            orientationSensor.addSignalCorruptor(orientationLatencyCorruptor);
         }

         simulatedSensorHolderAndReader.addOrientationSensorPort(imuDefinition, orientationSensor);
      }
   }

   private void createAndAddAngularVelocitySensors(Map<IMUMount, IMUDefinition> imuDefinitions, YoVariableRegistry registry)
   {
      Set<IMUMount> imuMounts = imuDefinitions.keySet();

      for (IMUMount imuMount : imuMounts)
      {
         String sensorName = imuMount.getName() + "AngularVelocity";
         IMUDefinition imuDefinition = imuDefinitions.get(imuMount);

         SimulatedAngularVelocitySensorFromRobot angularVelocitySensor = new SimulatedAngularVelocitySensorFromRobot(sensorName, imuMount, registry);

         if (sensorNoiseParameters != null)
         {
            GaussianVectorCorruptor angularVelocityCorruptor = new GaussianVectorCorruptor(1235L, sensorName, registry);
            double angularVelocityMeasurementStandardDeviation = sensorNoiseParameters.getAngularVelocityMeasurementStandardDeviation();

            angularVelocityCorruptor.setStandardDeviation(angularVelocityMeasurementStandardDeviation);
            angularVelocitySensor.addSignalCorruptor(angularVelocityCorruptor);

            RandomWalkBiasVectorCorruptor biasVectorCorruptor = new RandomWalkBiasVectorCorruptor(1236L, sensorName, estimateDT, registry);

            Vector3d initialAngularVelocityBias = new Vector3d();
            sensorNoiseParameters.getInitialAngularVelocityBias(initialAngularVelocityBias);
            biasVectorCorruptor.setBias(initialAngularVelocityBias);

            double angularVelocityBiasProcessNoiseStandardDeviation = sensorNoiseParameters.getAngularVelocityBiasProcessNoiseStandardDeviation();
            biasVectorCorruptor.setStandardDeviation(angularVelocityBiasProcessNoiseStandardDeviation);
            angularVelocitySensor.addSignalCorruptor(biasVectorCorruptor);
            
            
            double getAngularVelocityMeasurementLatency = sensorNoiseParameters.getAngularVelocityMeasurementLatency();
            int latencyTicks = (int) Math.round(getAngularVelocityMeasurementLatency / estimateDT);
            LatencyVectorCorruptor angularVelocityLatencyCorruptor = new LatencyVectorCorruptor(sensorName, latencyTicks, registry);
            angularVelocitySensor.addSignalCorruptor(angularVelocityLatencyCorruptor); 
         }

         simulatedSensorHolderAndReader.addAngularVelocitySensorPort(imuDefinition, angularVelocitySensor);
      }
   }

   private void createAndAddLinearAccelerationSensors(Map<IMUMount, IMUDefinition> imuDefinitions, YoVariableRegistry registry)
   {
      Set<IMUMount> imuMounts = imuDefinitions.keySet();

      for (IMUMount imuMount : imuMounts)
      {
         String sensorName = imuMount.getName() + "LinearAcceleration";
         IMUDefinition imuDefinition = imuDefinitions.get(imuMount);

         SimulatedLinearAccelerationSensorFromRobot linearAccelerationSensor = new SimulatedLinearAccelerationSensorFromRobot(sensorName, imuMount, registry);

         if (sensorNoiseParameters != null)
         {
            GaussianVectorCorruptor linearAccelerationCorruptor = new GaussianVectorCorruptor(1237L, sensorName, registry);
            double linearAccelerationMeasurementStandardDeviation = sensorNoiseParameters.getLinearAccelerationMeasurementStandardDeviation();
            linearAccelerationCorruptor.setStandardDeviation(linearAccelerationMeasurementStandardDeviation);
            linearAccelerationSensor.addSignalCorruptor(linearAccelerationCorruptor);

            RandomWalkBiasVectorCorruptor biasVectorCorruptor = new RandomWalkBiasVectorCorruptor(1286L, sensorName, estimateDT, registry);

            Vector3d initialLinearAccelerationBias = new Vector3d();
            sensorNoiseParameters.getInitialLinearVelocityBias(initialLinearAccelerationBias);
            biasVectorCorruptor.setBias(initialLinearAccelerationBias);

            double linearAccelerationBiasProcessNoiseStandardDeviation = sensorNoiseParameters.getLinearAccelerationBiasProcessNoiseStandardDeviation();
            biasVectorCorruptor.setStandardDeviation(linearAccelerationBiasProcessNoiseStandardDeviation);
            linearAccelerationSensor.addSignalCorruptor(biasVectorCorruptor);
         }

         simulatedSensorHolderAndReader.addLinearAccelerationSensorPort(imuDefinition, linearAccelerationSensor);
      }
   }

   public StateEstimatorSensorDefinitions getStateEstimatorSensorDefinitions()
   {
      return stateEstimatorSensorDefinitions;
   }

   public boolean useStateEstimator()
   {
      return true;
   }

//   private void getCenterOfMassPostion(FramePoint estimatedCoMPosition)
//   {
//      estimatedCoMPosition.setX(estimatedCoMPosition.getX() + 0.0855);
//      estimatedCoMPosition.setZ(estimatedCoMPosition.getZ() + 0.9904); //1.2); 
//      
//      Point3d comPoint = new Point3d();
//      try
//      {
//         robot.doDynamicsButDoNotIntegrate();
//      }
//      catch (UnreasonableAccelerationException e)
//      {
//         throw new RuntimeException("UnreasonableAccelerationException in getCenterOfMassPostion");
//      }
//      robot.computeCenterOfMass(comPoint);
//      
//   }
}
