package us.ihmc.wholeBodyController.concurrent;

import us.ihmc.SdfLoader.SDFFullHumanoidRobotModel;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.robotics.sensors.CenterOfMassDataHolder;
import us.ihmc.robotics.sensors.CenterOfMassDataHolderReadOnly;
import us.ihmc.robotics.sensors.ContactSensorHolder;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.robotics.sensors.ForceSensorDataHolderReadOnly;
import us.ihmc.sensorProcessing.model.DesiredJointDataHolder;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;

public interface ThreadDataSynchronizerInterface
{

   public abstract boolean receiveEstimatorStateForController();

   public abstract void publishEstimatorState(long timestamp, long estimatorTick, long estimatorClockStartTime);

   public abstract SDFFullHumanoidRobotModel getEstimatorFullRobotModel();

   public abstract ForceSensorDataHolder getEstimatorForceSensorDataHolder();

   public abstract CenterOfMassDataHolder getEstimatorCenterOfMassDataHolder();

   public abstract SDFFullHumanoidRobotModel getControllerFullRobotModel();

   public abstract ForceSensorDataHolderReadOnly getControllerForceSensorDataHolder();
   
   public abstract CenterOfMassDataHolderReadOnly getControllerCenterOfMassDataHolder();

   public abstract RawJointSensorDataHolderMap getEstimatorRawJointSensorDataHolderMap();

   public abstract RawJointSensorDataHolderMap getControllerRawJointSensorDataHolderMap();

   public abstract CenterOfPressureDataHolder getEstimatorCenterOfPressureDataHolder();

   public abstract CenterOfPressureDataHolder getControllerCenterOfPressureDataHolder();

   public abstract ContactSensorHolder getControllerContactSensorHolder();

   public abstract ContactSensorHolder getEstimatorContactSensorHolder();

   public abstract RobotMotionStatusHolder getEstimatorRobotMotionStatusHolder();

   public abstract RobotMotionStatusHolder getControllerRobotMotionStatusHolder();

   public abstract long getTimestamp();

   public abstract long getEstimatorClockStartTime();

   public abstract long getEstimatorTick();

   public abstract void publishControllerData();

   public abstract boolean receiveControllerDataForEstimator();

   public abstract DesiredJointDataHolder getEstimatorDesiredJointDataHolder();

}