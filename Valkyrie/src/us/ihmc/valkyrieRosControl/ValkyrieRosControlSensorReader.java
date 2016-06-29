package us.ihmc.valkyrieRosControl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.vecmath.Matrix3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.yaml.snakeyaml.Yaml;

import us.ihmc.communication.controllerAPI.CommandInputManager;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager.StatusMessageListener;
import us.ihmc.humanoidRobotics.communication.packets.walking.WalkingControllerFailureStatusMessage;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.time.TimeTools;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.AuxiliaryRobotData;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorProcessing;
import us.ihmc.sensorProcessing.sensorProcessors.SensorRawOutputMapReadOnly;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.sensorProcessing.simulatedSensors.StateEstimatorSensorDefinitions;
import us.ihmc.sensorProcessing.stateEstimation.SensorProcessingConfiguration;
import us.ihmc.tools.TimestampProvider;
import us.ihmc.tools.io.printing.PrintTools;
import us.ihmc.valkyrie.imu.MicroStrainData;
import us.ihmc.valkyrieRosControl.dataHolders.YoEffortJointHandleHolder;
import us.ihmc.valkyrieRosControl.dataHolders.YoForceTorqueSensorHandle;
import us.ihmc.valkyrieRosControl.dataHolders.YoIMUHandleHolder;
import us.ihmc.valkyrieRosControl.dataHolders.YoPositionJointHandleHolder;
import us.ihmc.wholeBodyController.JointTorqueOffsetProcessor;

public class ValkyrieRosControlSensorReader implements SensorReader, JointTorqueOffsetProcessor
{
   private final SensorProcessing sensorProcessing;

   private final TimestampProvider timestampProvider;

   private final List<YoEffortJointHandleHolder> yoEffortJointHandleHolders;
   private final List<YoPositionJointHandleHolder> yoPositionJointHandleHolders;
   private final List<YoIMUHandleHolder> yoIMUHandleHolders;
   private final List<YoForceTorqueSensorHandle> yoForceTorqueSensorHandles;

   private final Vector3d linearAcceleration = new Vector3d();
   private final Vector3d angularVelocity = new Vector3d();
   private final Quat4d orientation = new Quat4d();

   private final DenseMatrix64F torqueForce = new DenseMatrix64F(6, 1);

   private final AtomicBoolean resetIHMCControlRatioAndStandPrepRequested = new AtomicBoolean(false);

   private final ArrayList<ValkyrieRosControlEffortJointControlCommandCalculator> effortControlCommandCalculators = new ArrayList<>();
   private final LinkedHashMap<String, ValkyrieRosControlEffortJointControlCommandCalculator> effortJointToControlCommandCalculatorMap = new LinkedHashMap<>();
   private final ArrayList<ValkyrieRosControlPositionJointControlCommandCalculator> positionControlCommandCalculators = new ArrayList<>();
   private final LinkedHashMap<String, ValkyrieRosControlPositionJointControlCommandCalculator> positionJointToControlCommandCalculatorMap = new LinkedHashMap<>();

   private final DoubleYoVariable doIHMCControlRatio;
   private final DoubleYoVariable timeInStandprep;
   private final DoubleYoVariable standPrepRampUpTime;
   private final DoubleYoVariable masterGain;

   private long standPrepStartTime = -1;

   private final Matrix3d quaternionConversionMatrix = new Matrix3d();
   private final Matrix3d orientationMatrix = new Matrix3d();

   private final ValkyrieTorqueHysteresisCompensator torqueHysteresisCompensator;
   private final ValkyrieAccelerationIntegration accelerationIntegration;

   @SuppressWarnings("unchecked")
   public ValkyrieRosControlSensorReader(StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions,
         SensorProcessingConfiguration sensorProcessingConfiguration, TimestampProvider timestampProvider,
         List<YoEffortJointHandleHolder> yoEffortJointHandleHolders, List<YoPositionJointHandleHolder> yoPositionJointHandleHolders,
         List<YoIMUHandleHolder> yoIMUHandleHolders, List<YoForceTorqueSensorHandle> yoForceTorqueSensorHandles, YoVariableRegistry registry)
   {

      this.sensorProcessing = new SensorProcessing(stateEstimatorSensorDefinitions, sensorProcessingConfiguration, registry);
      this.timestampProvider = timestampProvider;
      this.yoEffortJointHandleHolders = yoEffortJointHandleHolders;
      this.yoPositionJointHandleHolders = yoPositionJointHandleHolders;
      this.yoIMUHandleHolders = yoIMUHandleHolders;
      this.yoForceTorqueSensorHandles = yoForceTorqueSensorHandles;

      doIHMCControlRatio = new DoubleYoVariable("doIHMCControlRatio", registry);
      masterGain = new DoubleYoVariable("StandPrepMasterGain", registry);
      timeInStandprep = new DoubleYoVariable("timeInStandprep", registry);
      standPrepRampUpTime = new DoubleYoVariable("standPrepRampUpTime", registry);
      standPrepRampUpTime.set(5.0);
      masterGain.set(0.3);

      double updateDT = sensorProcessingConfiguration.getEstimatorDT();
      torqueHysteresisCompensator = new ValkyrieTorqueHysteresisCompensator(yoEffortJointHandleHolders, timeInStandprep, registry);
      accelerationIntegration = new ValkyrieAccelerationIntegration(yoEffortJointHandleHolders, updateDT, registry);

      Yaml yaml = new Yaml();

      InputStream gainStream = getClass().getClassLoader().getResourceAsStream("standPrep/gains.yaml");
      InputStream setpointsStream = getClass().getClassLoader().getResourceAsStream("standPrep/setpoints.yaml");
      InputStream offsetsStream;
      try
      {
         offsetsStream = new FileInputStream(new File(ValkyrieTorqueOffsetPrinter.IHMC_TORQUE_OFFSET_FILE));
      }
      catch (FileNotFoundException e1)
      {
         offsetsStream = null;
      }

      Map<String, Map<String, Double>> gainMap = (Map<String, Map<String, Double>>) yaml.load(gainStream);
      Map<String, Double> setPointMap = (Map<String, Double>) yaml.load(setpointsStream);
      Map<String, Double> offsetMap = null;
      if (offsetsStream != null)
         offsetMap = (Map<String, Double>) yaml.load(offsetsStream);

      try
      {
         gainStream.close();
         setpointsStream.close();
         if (offsetsStream != null)
            offsetsStream.close();
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
            torqueOffset = offsetMap.get(jointName);

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
   }

   public void setDoIHMCControlRatio(double controlRatio)
   {
      doIHMCControlRatio.set(MathTools.clipToMinMax(controlRatio, 0.0, 1.0));
   }

   @Override
   public void read()
   {
      readSensors();
      writeCommandsToRobot();
   }

   public void readSensors()
   {
      for (int i = 0; i < yoEffortJointHandleHolders.size(); i++)
      {
         YoEffortJointHandleHolder yoEffortJointHandleHolder = yoEffortJointHandleHolders.get(i);
         yoEffortJointHandleHolder.update();

         sensorProcessing.setJointPositionSensorValue(yoEffortJointHandleHolder.getOneDoFJoint(), yoEffortJointHandleHolder.getQ());
         sensorProcessing.setJointVelocitySensorValue(yoEffortJointHandleHolder.getOneDoFJoint(), yoEffortJointHandleHolder.getQd());
         sensorProcessing.setJointTauSensorValue(yoEffortJointHandleHolder.getOneDoFJoint(), yoEffortJointHandleHolder.getTauMeasured());
      }

      for (int i = 0; i < yoPositionJointHandleHolders.size(); i++)
      {
         YoPositionJointHandleHolder yoPositionJointHandleHolder = yoPositionJointHandleHolders.get(i);
         yoPositionJointHandleHolder.update();

         sensorProcessing.setJointPositionSensorValue(yoPositionJointHandleHolder.getOneDoFJoint(), yoPositionJointHandleHolder.getQ());
         sensorProcessing.setJointVelocitySensorValue(yoPositionJointHandleHolder.getOneDoFJoint(), yoPositionJointHandleHolder.getQd());
         sensorProcessing.setJointTauSensorValue(yoPositionJointHandleHolder.getOneDoFJoint(),
               0.0); // TODO: Should be NaN eventually as the position control joints won't be able to return a measured torque
      }

      for (int i = 0; i < yoIMUHandleHolders.size(); i++)
      {
         YoIMUHandleHolder yoIMUHandleHolder = yoIMUHandleHolders.get(i);
         yoIMUHandleHolder.update();

         yoIMUHandleHolder.packLinearAcceleration(linearAcceleration);
         yoIMUHandleHolder.packAngularVelocity(angularVelocity);
         yoIMUHandleHolder.packOrientation(orientation);

         quaternionConversionMatrix.set(orientation);
         orientationMatrix.mul(MicroStrainData.MICROSTRAIN_TO_ZUP_WORLD, quaternionConversionMatrix);
         RotationTools.convertMatrixToQuaternion(orientationMatrix, orientation);

         sensorProcessing.setLinearAccelerationSensorValue(yoIMUHandleHolder.getImuDefinition(), linearAcceleration);
         sensorProcessing.setAngularVelocitySensorValue(yoIMUHandleHolder.getImuDefinition(), angularVelocity);
         sensorProcessing.setOrientationSensorValue(yoIMUHandleHolder.getImuDefinition(), orientation);
      }

      for (int i = 0; i < yoForceTorqueSensorHandles.size(); i++)
      {
         YoForceTorqueSensorHandle yoForceTorqueSensorHandle = yoForceTorqueSensorHandles.get(i);
         yoForceTorqueSensorHandle.update();

         yoForceTorqueSensorHandle.packWrench(torqueForce);
         sensorProcessing.setForceSensorValue(yoForceTorqueSensorHandle.getForceSensorDefinition(), torqueForce);
      }

      long timestamp = timestampProvider.getTimestamp();
      sensorProcessing.startComputation(timestamp, timestamp, -1);
   }

   public void writeCommandsToRobot()
   {
      long timestamp = timestampProvider.getTimestamp();

      if (resetIHMCControlRatioAndStandPrepRequested.getAndSet(false))
      {
         standPrepStartTime = -1;
         doIHMCControlRatio.set(0.0);
      }

      if (standPrepStartTime > 0)
      {
         timeInStandprep.set(TimeTools.nanoSecondstoSeconds(timestamp - standPrepStartTime));

         torqueHysteresisCompensator.compute();
         if (ValkyrieRosControlController.INTEGRATE_ACCELERATIONS_AND_CONTROL_VELOCITIES)
            accelerationIntegration.compute();

         double ramp = timeInStandprep.getDoubleValue() / standPrepRampUpTime.getDoubleValue();
         ramp = MathTools.clipToMinMax(ramp, 0.0, 1.0);

         for (int i = 0; i < effortControlCommandCalculators.size(); i++)
         {
            ValkyrieRosControlEffortJointControlCommandCalculator commandCalculator = effortControlCommandCalculators.get(i);
            commandCalculator.computeAndUpdateJointTorque(ramp, doIHMCControlRatio.getDoubleValue(), masterGain.getDoubleValue());
         }

         for (int i = 0; i < positionControlCommandCalculators.size(); i++)
         {
            ValkyrieRosControlPositionJointControlCommandCalculator commandCalculator = positionControlCommandCalculators.get(i);
            commandCalculator.computeAndUpdateJointPosition(ramp, doIHMCControlRatio.getDoubleValue(), masterGain.getDoubleValue());
         }

      }
      else
      {
         standPrepStartTime = timestamp;
         for (int i = 0; i < effortControlCommandCalculators.size(); i++)
         {
            ValkyrieRosControlEffortJointControlCommandCalculator commandCalculator = effortControlCommandCalculators.get(i);
            commandCalculator.initialize();
         }

         for (int i = 0; i < positionControlCommandCalculators.size(); i++)
         {
            ValkyrieRosControlPositionJointControlCommandCalculator commandCalculator = positionControlCommandCalculators.get(i);
            commandCalculator.initialize();
         }
      }
   }

   @Override
   public SensorOutputMapReadOnly getSensorOutputMapReadOnly()
   {
      return sensorProcessing;
   }

   @Override
   public SensorRawOutputMapReadOnly getSensorRawOutputMapReadOnly()
   {
      return sensorProcessing;
   }

   @Override
   public AuxiliaryRobotData newAuxiliaryRobotDataInstance()
   {
      return null;
   }

   @Override
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
}
