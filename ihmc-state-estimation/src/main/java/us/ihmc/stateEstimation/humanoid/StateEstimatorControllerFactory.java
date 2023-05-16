package us.ihmc.stateEstimation.humanoid;

import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;

public interface StateEstimatorControllerFactory
{
   StateEstimatorController createStateEstimator(FullHumanoidRobotModel fullRobotModel, SensorOutputMapReadOnly rawOutputMap);
}
