package us.ihmc.valkyrieRosControl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.yaml.snakeyaml.Yaml;

import us.ihmc.commons.Conversions;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.CommandInputManager;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager.StatusMessageListener;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.HighLevelControllerStateCommand;
import us.ihmc.humanoidRobotics.communication.packets.HighLevelStateChangeStatusMessage;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;
import us.ihmc.humanoidRobotics.communication.packets.valkyrie.ValkyrieLowLevelControlModeMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingControllerFailureStatusMessage;
import us.ihmc.robotics.MathTools;
import us.ihmc.yoVariables.listener.VariableChangedListener;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoVariable;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation.ForceSensorCalibrationModule;
import us.ihmc.tools.TimestampProvider;
import us.ihmc.tools.taskExecutor.Task;
import us.ihmc.tools.taskExecutor.TaskExecutor;
import us.ihmc.valkyrieRosControl.dataHolders.YoEffortJointHandleHolder;
import us.ihmc.valkyrieRosControl.dataHolders.YoPositionJointHandleHolder;
import us.ihmc.wholeBodyController.diagnostics.JointTorqueOffsetEstimator;

public class ValkyrieRosControlLowLevelController
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final TimestampProvider timestampProvider;

   private final AtomicBoolean resetIHMCControlRatioAndStandPrepRequested = new AtomicBoolean(false);

   private final ArrayList<ValkyrieRosControlEffortJointControlCommandCalculator> effortControlCommandCalculators = new ArrayList<>();
   private final LinkedHashMap<String, ValkyrieRosControlEffortJointControlCommandCalculator> effortJointToControlCommandCalculatorMap = new LinkedHashMap<>();
   private final ArrayList<ValkyrieRosControlPositionJointControlCommandCalculator> positionControlCommandCalculators = new ArrayList<>();
   private final LinkedHashMap<String, ValkyrieRosControlPositionJointControlCommandCalculator> positionJointToControlCommandCalculatorMap = new LinkedHashMap<>();

   private final YoDouble yoTime = new YoDouble("lowLevelControlTime", registry);
   private final YoDouble wakeUpTime = new YoDouble("lowLevelControlWakeUpTime", registry);

   private final YoDouble standPrepStartTime = new YoDouble("standPrepStartTime", registry);

   private final YoDouble doIHMCControlRatio = new YoDouble("doIHMCControlRatio", registry);
   private final YoDouble standPrepRampDuration = new YoDouble("standPrepRampDuration", registry);
   private final YoDouble masterGain = new YoDouble("standPrepMasterGain", registry);

   private final YoDouble controlRatioRampDuration = new YoDouble("controlRatioRampDuration", registry);
   private final YoDouble calibrationDuration = new YoDouble("calibrationDuration", registry);
   private final YoDouble calibrationStartTime = new YoDouble("calibrationStartTime", registry);
   private final YoDouble timeInCalibration = new YoDouble("timeInCalibration", registry);

   private final YoEnum<ValkyrieLowLevelControlModeMessage.ControlMode> currentControlMode = new YoEnum<>("lowLevelControlMode", registry, ValkyrieLowLevelControlModeMessage.ControlMode.class);

   private final YoEnum<ValkyrieLowLevelControlModeMessage.ControlMode> requestedLowLevelControlMode = new YoEnum<>("requestedLowLevelControlMode", registry, ValkyrieLowLevelControlModeMessage.ControlMode.class, true);
   private final AtomicReference<ValkyrieLowLevelControlModeMessage.ControlMode> requestedLowLevelControlModeAtomic = new AtomicReference<>(null);

   private final ValkyrieTorqueHysteresisCompensator torqueHysteresisCompensator;
   private final ValkyrieAccelerationIntegration accelerationIntegration;

   private CommandInputManager commandInputManager;
   private final AtomicReference<HighLevelController> currentHighLevelState = new AtomicReference<HighLevelController>(null);

   private final HighLevelControllerStateCommand highLevelControllerStateCommand = new HighLevelControllerStateCommand();

   private final TaskExecutor taskExecutor = new TaskExecutor();
   private final Task rampUpIHMCControlRatioTask;
   private final Task rampDownIHMCControlRatioTask;
   private final Task calibrationTask;

   private ForceSensorCalibrationModule forceSensorCalibrationModule;
   private JointTorqueOffsetEstimator jointTorqueOffsetEstimator;

   @SuppressWarnings("unchecked")
   public ValkyrieRosControlLowLevelController(TimestampProvider timestampProvider, final double updateDT, List<YoEffortJointHandleHolder> yoEffortJointHandleHolders,
         List<YoPositionJointHandleHolder> yoPositionJointHandleHolders, YoVariableRegistry parentRegistry)
   {
      this.timestampProvider = timestampProvider;

      standPrepRampDuration.set(3.0);
      controlRatioRampDuration.set(3.0);
      masterGain.set(0.3);
      calibrationDuration.set(10.0);

      currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.STAND_PREP);

      wakeUpTime.set(Double.NaN);
      standPrepStartTime.set(Double.NaN);
      calibrationStartTime.set(Double.NaN);
      
      torqueHysteresisCompensator = new ValkyrieTorqueHysteresisCompensator(yoEffortJointHandleHolders, yoTime, registry);
      accelerationIntegration = new ValkyrieAccelerationIntegration(yoEffortJointHandleHolders, updateDT, registry);

      requestedLowLevelControlMode.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void notifyOfVariableChange(YoVariable<?> v)
         {
            if (requestedLowLevelControlMode.getEnumValue() != null)
               requestedLowLevelControlModeAtomic.set(requestedLowLevelControlMode.getEnumValue());
         }
      });

      rampDownIHMCControlRatioTask = new AbstractLowLevelTask()
      {
         @Override
         public void doAction()
         {
            double newRatio = doIHMCControlRatio.getDoubleValue() - updateDT / controlRatioRampDuration.getDoubleValue();
            doIHMCControlRatio.set(MathTools.clamp(newRatio, 0.0, 1.0));
         }

         @Override
         public boolean isDone()
         {
            return doIHMCControlRatio.getDoubleValue() == 0.0;
         }
      };

      rampUpIHMCControlRatioTask = new AbstractLowLevelTask()
      {
         @Override
         public void doAction()
         {
            double newRatio = doIHMCControlRatio.getDoubleValue() + updateDT / controlRatioRampDuration.getDoubleValue();
            doIHMCControlRatio.set(MathTools.clamp(newRatio, 0.0, 1.0));
         }

         @Override
         public boolean isDone()
         {
            return doIHMCControlRatio.getDoubleValue() == 1.0;
         }
      };

      calibrationTask = new AbstractLowLevelTask()
      {
         private final ValkyrieTorqueOffsetPrinter torqueOffsetPrinter = new ValkyrieTorqueOffsetPrinter();

         @Override
         public void doAction()
         {
            timeInCalibration.set(yoTime.getDoubleValue() - calibrationStartTime.getDoubleValue());
            if (timeInCalibration.getDoubleValue() >= calibrationDuration.getDoubleValue() - 0.1)
            {
               if (jointTorqueOffsetEstimator != null)
                  jointTorqueOffsetEstimator.enableJointTorqueOffsetEstimationAtomic(false);
            }
         }

         @Override
         public void doTransitionIntoAction()
         {
            calibrationStartTime.set(yoTime.getDoubleValue());
            if (jointTorqueOffsetEstimator != null)
               jointTorqueOffsetEstimator.enableJointTorqueOffsetEstimationAtomic(true);
         }

         @Override
         public void doTransitionOutOfAction()
         {
            calibrationStartTime.set(Double.NaN);

            List<OneDoFJoint> oneDoFJoints = jointTorqueOffsetEstimator.getOneDoFJoints();

            torqueOffsetPrinter.printTorqueOffsets(jointTorqueOffsetEstimator);

            for (int i = 0; i < oneDoFJoints.size(); i++)
            {
               OneDoFJoint joint = oneDoFJoints.get(i);
               if (jointTorqueOffsetEstimator.hasTorqueOffsetForJoint(joint))
               {
                  subtractTorqueOffset(joint, jointTorqueOffsetEstimator.getEstimatedJointTorqueOffset(joint));
                  jointTorqueOffsetEstimator.resetEstimatedJointTorqueOffset(joint);
               }
            }

            if (forceSensorCalibrationModule != null)
               forceSensorCalibrationModule.requestFootForceSensorCalibrationAtomic();
         }

         @Override
         public boolean isDone()
         {
            return timeInCalibration.getDoubleValue() >= calibrationDuration.getDoubleValue();
         }
      };

      Yaml yaml = new Yaml();
      InputStream gainStream = getClass().getClassLoader().getResourceAsStream("standPrep/gains.yaml");
      InputStream setpointsStream = getClass().getClassLoader().getResourceAsStream("standPrep/setpoints.yaml");

      Map<String, Map<String, Double>> gainMap = (Map<String, Map<String, Double>>) yaml.load(gainStream);
      Map<String, Double> setPointMap = (Map<String, Double>) yaml.load(setpointsStream);
      Map<String, Double> offsetMap = ValkyrieTorqueOffsetPrinter.loadTorqueOffsetsFromFile();

      try
      {
         gainStream.close();
         setpointsStream.close();
      }
      catch (IOException e)
      {
      }

      for (YoEffortJointHandleHolder effortJointHandleHolder : yoEffortJointHandleHolders)
      {
         String jointName = effortJointHandleHolder.getName();
         Map<String, Double> standPrepGains = gainMap.get(jointName);
         double torqueOffset = 0.0;
         if (offsetMap != null && offsetMap.containsKey(jointName))
            torqueOffset = -offsetMap.get(jointName);

         double standPrepAngle = 0.0;
         if (setPointMap.containsKey(jointName))
         {
            standPrepAngle = setPointMap.get(jointName);
         }
         ValkyrieRosControlEffortJointControlCommandCalculator controlCommandCalculator = new ValkyrieRosControlEffortJointControlCommandCalculator(
               effortJointHandleHolder, standPrepGains, torqueOffset, standPrepAngle, updateDT, registry);
         effortControlCommandCalculators.add(controlCommandCalculator);

         effortJointToControlCommandCalculatorMap.put(jointName, controlCommandCalculator);
      }

      for (YoPositionJointHandleHolder positionJointHandleHolder : yoPositionJointHandleHolders)
      {
         String jointName = positionJointHandleHolder.getName();
         Map<String, Double> standPrepGains = gainMap.get(jointName);

         double standPrepAngle = 0.0;
         if (setPointMap.containsKey(jointName))
         {
            standPrepAngle = setPointMap.get(jointName);
         }
         ValkyrieRosControlPositionJointControlCommandCalculator controlCommandCalculator = new ValkyrieRosControlPositionJointControlCommandCalculator(
               positionJointHandleHolder, standPrepGains, standPrepAngle, updateDT, registry);
         positionControlCommandCalculators.add(controlCommandCalculator);

         positionJointToControlCommandCalculatorMap.put(jointName, controlCommandCalculator);
      }

      parentRegistry.addChild(registry);
   }

   public void setDoIHMCControlRatio(double controlRatio)
   {
      doIHMCControlRatio.set(MathTools.clamp(controlRatio, 0.0, 1.0));
   }

   public void requestCalibration()
   {
      requestedLowLevelControlModeAtomic.set(ValkyrieLowLevelControlModeMessage.ControlMode.CALIBRATION);
   }

   public void doControl()
   {
      long timestamp = timestampProvider.getTimestamp();

      if (wakeUpTime.isNaN())
         wakeUpTime.set(Conversions.nanosecondsToSeconds(timestamp));

      yoTime.set(Conversions.nanosecondsToSeconds(timestamp) - wakeUpTime.getDoubleValue());

      taskExecutor.doControl();
      ValkyrieLowLevelControlModeMessage.ControlMode newRequest = requestedLowLevelControlModeAtomic.getAndSet(null);
      requestedLowLevelControlMode.set(null);

      switch (currentControlMode.getEnumValue())
      {
      case STAND_PREP:
         if (doIHMCControlRatio.getDoubleValue() > 1.0 - 1.0e-3 && currentHighLevelState.get() == HighLevelController.WALKING)
         {
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.HIGH_LEVEL_CONTROL);
            break;
         }

         if (taskExecutor.isDone() && currentHighLevelState.get() != HighLevelController.DO_NOTHING_BEHAVIOR)
         {
            highLevelControllerStateCommand.setHighLevelController(HighLevelController.DO_NOTHING_BEHAVIOR);
            commandInputManager.submitCommand(highLevelControllerStateCommand);
         }

         if (newRequest == null)
            break;

         switch (newRequest)
         {
         case CALIBRATION:
            highLevelControllerStateCommand.setHighLevelController(HighLevelController.CALIBRATION);
            commandInputManager.submitCommand(highLevelControllerStateCommand);
            taskExecutor.submit(rampUpIHMCControlRatioTask);
            taskExecutor.submit(calibrationTask);
            taskExecutor.submit(rampDownIHMCControlRatioTask);
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.CALIBRATION);
            break;
         case HIGH_LEVEL_CONTROL:
            highLevelControllerStateCommand.setHighLevelController(HighLevelController.WALKING);
            commandInputManager.submitCommand(highLevelControllerStateCommand);
            taskExecutor.submit(rampUpIHMCControlRatioTask);
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.HIGH_LEVEL_CONTROL);
            break;
         default:
            break;
         }

      case CALIBRATION:
         if (taskExecutor.isDone())
         {
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.STAND_PREP);
         }
         break;
      case HIGH_LEVEL_CONTROL:
         if (newRequest != null && newRequest == ValkyrieLowLevelControlModeMessage.ControlMode.STAND_PREP)
         {
            taskExecutor.clear();
            taskExecutor.submit(rampDownIHMCControlRatioTask);
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.STAND_PREP);
            break;
         }

         if (resetIHMCControlRatioAndStandPrepRequested.getAndSet(false))
         {
            standPrepStartTime.set(Double.NaN);
            doIHMCControlRatio.set(0.0);
            taskExecutor.clear();
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.STAND_PREP);
            break;
         }

         if (doIHMCControlRatio.getDoubleValue() < 1.0e-3 && taskExecutor.isDone())
         {
            currentControlMode.set(ValkyrieLowLevelControlModeMessage.ControlMode.STAND_PREP);
            break;
         }

         torqueHysteresisCompensator.compute();
         if (ValkyrieRosControlController.INTEGRATE_ACCELERATIONS_AND_CONTROL_VELOCITIES)
            accelerationIntegration.compute();
         break;
      }

      updateCommandCalculators();
   }

   public void updateCommandCalculators()
   {
      for (int i = 0; i < effortControlCommandCalculators.size(); i++)
      {
         ValkyrieRosControlEffortJointControlCommandCalculator commandCalculator = effortControlCommandCalculators.get(i);
         commandCalculator.computeAndUpdateJointTorque(doIHMCControlRatio.getDoubleValue(), masterGain.getDoubleValue());
      }
   }

   public void subtractTorqueOffset(OneDoFJoint oneDoFJoint, double torqueOffset)
   {
      ValkyrieRosControlEffortJointControlCommandCalculator jointCommandCalculator = effortJointToControlCommandCalculatorMap.get(oneDoFJoint.getName());
      if (jointCommandCalculator != null)
         jointCommandCalculator.subtractTorqueOffset(torqueOffset);
      else
         PrintTools.error("Command calculator is NULL for the joint: " + oneDoFJoint.getName());
   }

   public void attachControllerAPI(CommandInputManager commandInputManager, StatusMessageOutputManager statusOutputManager)
   {
      this.commandInputManager = commandInputManager;

      StatusMessageListener<HighLevelStateChangeStatusMessage> highLevelStateChangeListener = new StatusMessageListener<HighLevelStateChangeStatusMessage>()
      {
         @Override
         public void receivedNewMessageStatus(HighLevelStateChangeStatusMessage statusMessage)
         {
            if (statusMessage != null)
               currentHighLevelState.set(statusMessage.endState);
         }
      };
      statusOutputManager.attachStatusMessageListener(HighLevelStateChangeStatusMessage.class, highLevelStateChangeListener);

      StatusMessageListener<WalkingControllerFailureStatusMessage> controllerFailureListener = new StatusMessageListener<WalkingControllerFailureStatusMessage>()
      {
         @Override
         public void receivedNewMessageStatus(WalkingControllerFailureStatusMessage statusMessage)
         {
            if (statusMessage != null)
               resetIHMCControlRatioAndStandPrepRequested.set(true);
         }
      };
      statusOutputManager.attachStatusMessageListener(WalkingControllerFailureStatusMessage.class, controllerFailureListener);
   }
   
   public void attachForceSensorCalibrationModule(DRCRobotSensorInformation sensorInformation, ForceSensorCalibrationModule forceSensorCalibrationModule)
   {
      this.forceSensorCalibrationModule = forceSensorCalibrationModule;
   }

   public void attachJointTorqueOffsetEstimator(JointTorqueOffsetEstimator jointTorqueOffsetEstimator)
   {
      this.jointTorqueOffsetEstimator = jointTorqueOffsetEstimator;
   }

   private abstract class AbstractLowLevelTask implements Task
   {
      @Override
      public void doAction()
      {
      }

      @Override
      public void doTransitionIntoAction()
      {
      }

      @Override
      public void doTransitionOutOfAction()
      {
      }
   }

   public void setupLowLevelControlWithPacketCommunicator(PacketCommunicator packetCommunicator)
   {
      packetCommunicator.attachListener(ValkyrieLowLevelControlModeMessage.class, new PacketConsumer<ValkyrieLowLevelControlModeMessage>()
      {
         @Override
         public void receivedPacket(ValkyrieLowLevelControlModeMessage packet)
         {
            if (packet != null && packet.getRequestedControlMode() != null)
               requestedLowLevelControlModeAtomic.set(packet.getRequestedControlMode());
         }
      });
   }
}
