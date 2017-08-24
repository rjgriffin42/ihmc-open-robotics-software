package us.ihmc.wholeBodyController.concurrent;

import java.util.Arrays;
import java.util.LinkedHashMap;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.sensors.CenterOfMassDataHolder;
import us.ihmc.robotics.sensors.CenterOfMassDataHolderReadOnly;
import us.ihmc.robotics.sensors.ContactSensorHolder;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.sensorProcessing.model.DesiredJointDataHolder;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoLong;

public class SingleThreadedThreadDataSynchronizer implements ThreadDataSynchronizerInterface
{
   private final FullHumanoidRobotModel estimatorFullRobotModel;
   private final ForceSensorDataHolder estimatorForceSensorDataHolder;
   private final CenterOfMassDataHolder estimatorCenterOfMassDataHolder;
   private final RawJointSensorDataHolderMap estimatorRawJointSensorDataHolderMap;
   private final CenterOfPressureDataHolder estimatorCenterOfPressureDataHolder;
   private final RobotMotionStatusHolder estimatorRobotMotionStatusHolder;
   private final ContactSensorHolder estimatorContactSensorHolder;
   private final DesiredJointDataHolder estimatorDesiredJointDataHolder;

   private final FullHumanoidRobotModel controllerFullRobotModel;
   private final ForceSensorDataHolder controllerForceSensorDataHolder;
   private final CenterOfMassDataHolder controllerCenterOfMassDataHolder;
   private final RawJointSensorDataHolderMap controllerRawJointSensorDataHolderMap;
   private final CenterOfPressureDataHolder controllerCenterOfPressureDataHolder;
   private final RobotMotionStatusHolder controllerRobotMotionStatusHolder;
   private final ContactSensorHolder controllerContactSensorHolder;
   private final DesiredJointDataHolder controllerDesiredJointDataHolder;

   private final YoLong timestamp;
   private final YoLong estimatorClockStartTime;
   private final YoLong estimatorTick;

   private final FullRobotModelRootJointRewinder fullRobotModelRewinder;

   /**
    * SingleThreadedThreadDataSynchronizer is an alternative to ThreadDataSynchronizer when you want to run on a single thread and have
    * deterministic rewindable execution. The FullRobotModels and other objects are just shared between the estimator and the controller.
    * @param wholeBodyControlParameters
    * @param registry
    */
   public SingleThreadedThreadDataSynchronizer(SimulationConstructionSet scs, FullHumanoidRobotModelFactory robotModelFactory, YoVariableRegistry registry)
   {
      timestamp = new YoLong(getClass().getSimpleName() + "Timestamp", registry);
      estimatorClockStartTime = new YoLong(getClass().getSimpleName() + "EstimatorClockStartTime", registry);
      estimatorTick = new YoLong(getClass().getSimpleName() + "EstimatorTick", registry);

      estimatorFullRobotModel = robotModelFactory.createFullRobotModel();
      estimatorForceSensorDataHolder = new ForceSensorDataHolder(Arrays.asList(estimatorFullRobotModel.getForceSensorDefinitions()));
      estimatorCenterOfMassDataHolder = new CenterOfMassDataHolder();
      estimatorRawJointSensorDataHolderMap = new RawJointSensorDataHolderMap(estimatorFullRobotModel);
      estimatorContactSensorHolder = new ContactSensorHolder(Arrays.asList(estimatorFullRobotModel.getContactSensorDefinitions()));
      estimatorRobotMotionStatusHolder = new RobotMotionStatusHolder();
      estimatorDesiredJointDataHolder = new DesiredJointDataHolder(estimatorFullRobotModel.getOneDoFJoints());

      LinkedHashMap<RigidBody, ReferenceFrame> soleFrames = new LinkedHashMap<RigidBody, ReferenceFrame>();
      for(RobotSide robotSide : RobotSide.values)
      {
         soleFrames.put(estimatorFullRobotModel.getFoot(robotSide), estimatorFullRobotModel.getSoleFrame(robotSide));
      }
      estimatorCenterOfPressureDataHolder = new CenterOfPressureDataHolder(soleFrames);

      controllerFullRobotModel = estimatorFullRobotModel;
      controllerForceSensorDataHolder = estimatorForceSensorDataHolder;
      controllerCenterOfMassDataHolder = estimatorCenterOfMassDataHolder;
      controllerRawJointSensorDataHolderMap = estimatorRawJointSensorDataHolderMap;
      controllerCenterOfPressureDataHolder = estimatorCenterOfPressureDataHolder;
      controllerRobotMotionStatusHolder = estimatorRobotMotionStatusHolder;
      controllerContactSensorHolder = estimatorContactSensorHolder;
      controllerDesiredJointDataHolder = estimatorDesiredJointDataHolder;

      this.fullRobotModelRewinder = new FullRobotModelRootJointRewinder(estimatorFullRobotModel, registry);
      if(scs != null)
      {
    	  scs.attachSimulationRewoundListener(fullRobotModelRewinder);
      }
   }

   @Override
   public boolean receiveEstimatorStateForController()
   {
      return true;
   }

   @Override
   public void publishEstimatorState(long timestamp, long estimatorTick, long estimatorClockStartTime)
   {
      this.timestamp.set(timestamp);
      this.estimatorTick.set(estimatorTick);
      this.estimatorClockStartTime.set(estimatorClockStartTime);

      // Record full robot model here for rewindability.
      fullRobotModelRewinder.recordCurrentState();
   }

   @Override
   public FullHumanoidRobotModel getEstimatorFullRobotModel()
   {
      return estimatorFullRobotModel;
   }

   @Override
   public ForceSensorDataHolder getEstimatorForceSensorDataHolder()
   {
      return estimatorForceSensorDataHolder;
   }

   @Override
   public CenterOfMassDataHolder getEstimatorCenterOfMassDataHolder()
   {
      return estimatorCenterOfMassDataHolder;
   }

   @Override
   public FullHumanoidRobotModel getControllerFullRobotModel()
   {
      return controllerFullRobotModel;
   }

   @Override
   public ForceSensorDataHolder getControllerForceSensorDataHolder()
   {
      return controllerForceSensorDataHolder;
   }

   @Override
   public CenterOfMassDataHolderReadOnly getControllerCenterOfMassDataHolder()
   {
      return controllerCenterOfMassDataHolder;
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
   public RobotMotionStatusHolder getEstimatorRobotMotionStatusHolder()
   {
      return estimatorRobotMotionStatusHolder;
   }

   @Override
   public RobotMotionStatusHolder getControllerRobotMotionStatusHolder()
   {
      return controllerRobotMotionStatusHolder;
   }

   @Override
   public long getTimestamp()
   {
      return timestamp.getLongValue();
   }

   @Override
   public long getEstimatorClockStartTime()
   {
      return estimatorClockStartTime.getLongValue();
   }

   @Override
   public long getEstimatorTick()
   {
      return estimatorTick.getLongValue();
   }

   @Override
   public void publishControllerData()
   {
      controllerDesiredJointDataHolder.updateFromModel();
   }

   @Override
   public boolean receiveControllerDataForEstimator()
   {
      return true;
   }

   @Override
   public ContactSensorHolder getControllerContactSensorHolder()
   {
      return controllerContactSensorHolder;
   }

   @Override
   public ContactSensorHolder getEstimatorContactSensorHolder()
   {
      return estimatorContactSensorHolder;
   }

   @Override
   public DesiredJointDataHolder getEstimatorDesiredJointDataHolder()
   {
      return estimatorDesiredJointDataHolder;
   }


}
