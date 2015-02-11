package us.ihmc.wholeBodyController.concurrent;

import java.util.Arrays;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.concurrent.ConcurrentCopier;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.utilities.humanoidRobot.model.CenterOfPressureDataHolder;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDataHolder;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;

public class ThreadDataSynchronizer implements ThreadDataSynchronizerInterface
{
   private final SDFFullRobotModel estimatorFullRobotModel;
   private final ForceSensorDataHolder estimatorForceSensorDataHolder;
   private final RawJointSensorDataHolderMap estimatorRawJointSensorDataHolderMap;
   private final CenterOfPressureDataHolder estimatorCenterOfPressureDataHolder;

   private final SDFFullRobotModel controllerFullRobotModel;
   private final ForceSensorDataHolder controllerForceSensorDataHolder;
   private final RawJointSensorDataHolderMap controllerRawJointSensorDataHolderMap;
   private final CenterOfPressureDataHolder controllerCenterOfPressureDataHolder;

   private final ConcurrentCopier<IntermediateEstimatorStateHolder> estimatorStateCopier;

   private final ConcurrentCopier<ControllerDataForEstimatorHolder> controllerStateCopier;

   private long timestamp;
   private long estimatorClockStartTime;
   private long estimatorTick;

   public ThreadDataSynchronizer(WholeBodyControllerParameters wholeBodyControlParameters)
   {
      estimatorFullRobotModel = wholeBodyControlParameters.createFullRobotModel();
      estimatorForceSensorDataHolder = new ForceSensorDataHolder(Arrays.asList(estimatorFullRobotModel.getForceSensorDefinitions()));
      estimatorRawJointSensorDataHolderMap = new RawJointSensorDataHolderMap(estimatorFullRobotModel);
      estimatorCenterOfPressureDataHolder = new CenterOfPressureDataHolder(estimatorFullRobotModel.getSoleFrames());

      controllerFullRobotModel = wholeBodyControlParameters.createFullRobotModel();
      controllerForceSensorDataHolder = new ForceSensorDataHolder(Arrays.asList(controllerFullRobotModel.getForceSensorDefinitions()));
      controllerRawJointSensorDataHolderMap = new RawJointSensorDataHolderMap(controllerFullRobotModel);
      controllerCenterOfPressureDataHolder =  new CenterOfPressureDataHolder(controllerFullRobotModel.getSoleFrames());

      IntermediateEstimatorStateHolder.Builder stateCopierBuilder = new IntermediateEstimatorStateHolder.Builder(wholeBodyControlParameters,
            estimatorFullRobotModel.getElevator(), controllerFullRobotModel.getElevator(), estimatorForceSensorDataHolder, controllerForceSensorDataHolder,
            estimatorRawJointSensorDataHolderMap, controllerRawJointSensorDataHolderMap);
      estimatorStateCopier = new ConcurrentCopier<IntermediateEstimatorStateHolder>(stateCopierBuilder);

      ControllerDataForEstimatorHolder.Builder controllerStateCopierBuilder = new ControllerDataForEstimatorHolder.Builder(estimatorCenterOfPressureDataHolder,
            controllerCenterOfPressureDataHolder);
      controllerStateCopier = new ConcurrentCopier<>(controllerStateCopierBuilder);
   }

   @Override
   public boolean receiveEstimatorStateForController()
   {
      IntermediateEstimatorStateHolder estimatorStateHolder = estimatorStateCopier.getCopyForReading();
      if (estimatorStateHolder != null)
      {
         estimatorStateHolder.getIntoControllerModel();
         timestamp = estimatorStateHolder.getTimestamp();
         estimatorClockStartTime = estimatorStateHolder.getEstimatorClockStartTime();
         estimatorTick = estimatorStateHolder.getEstimatorTick();
         return true;
      }
      else
      {
         return false;
      }
   }

   @Override
   public void publishEstimatorState(long timestamp, long estimatorTick, long estimatorClockStartTime)
   {      
      IntermediateEstimatorStateHolder estimatorStateHolder = estimatorStateCopier.getCopyForWriting();
      estimatorStateHolder.setFromEstimatorModel(timestamp, estimatorTick, estimatorClockStartTime);
      estimatorStateCopier.commit();
   }

   @Override
   public SDFFullRobotModel getEstimatorFullRobotModel()
   {
      return estimatorFullRobotModel;
   }

   @Override
   public ForceSensorDataHolder getEstimatorForceSensorDataHolder()
   {
      return estimatorForceSensorDataHolder;
   }

   @Override
   public SDFFullRobotModel getControllerFullRobotModel()
   {
      return controllerFullRobotModel;
   }

   @Override
   public ForceSensorDataHolder getControllerForceSensorDataHolder()
   {
      return controllerForceSensorDataHolder;
   }

   @Override
   public RawJointSensorDataHolderMap getEstimatorRawJointSensorDataHolderMap()
   {
      return estimatorRawJointSensorDataHolderMap;
   }

   @Override
   public RawJointSensorDataHolderMap getControllerRawJointSensorDataHolderMap()
   {
      return controllerRawJointSensorDataHolderMap;
   }

   @Override
   public CenterOfPressureDataHolder getEstimatorCenterOfPressureDataHolder()
   {
      return estimatorCenterOfPressureDataHolder;
   }

   @Override
   public CenterOfPressureDataHolder getControllerCenterOfPressureDataHolder()
   {
      return controllerCenterOfPressureDataHolder;
   }

   @Override
   public long getTimestamp()
   {
      return timestamp;
   }

   @Override
   public long getEstimatorClockStartTime()
   {
      return estimatorClockStartTime;
   }

   @Override
   public long getEstimatorTick()
   {
      return estimatorTick;
   }

   @Override
   public void publishControllerData()
   {
      ControllerDataForEstimatorHolder holder = controllerStateCopier.getCopyForWriting();
      if (holder != null)
      {
         holder.getCenterOfPressureInSoleFrame();
         controllerStateCopier.commit();
      }
   }

   @Override
   public boolean receiveControllerDataForEstimator()
   {
      ControllerDataForEstimatorHolder holder = controllerStateCopier.getCopyForReading();
      if (holder != null)
      {
         holder.setCenterOfPressureInSoleFrame();
         return true;
      }
      else
      {
         return false;
      }
   }

}
