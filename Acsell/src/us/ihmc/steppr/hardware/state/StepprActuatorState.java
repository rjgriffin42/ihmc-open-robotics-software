package us.ihmc.steppr.hardware.state;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.vecmath.Vector3d;

import us.ihmc.steppr.hardware.state.slowSensors.BusVoltage;
import us.ihmc.steppr.hardware.state.slowSensors.ControlMode;
import us.ihmc.steppr.hardware.state.slowSensors.ControllerTemperature1;
import us.ihmc.steppr.hardware.state.slowSensors.ControllerTemperature2;
import us.ihmc.steppr.hardware.state.slowSensors.IMUAccelSensor;
import us.ihmc.steppr.hardware.state.slowSensors.IMUGyroSensor;
import us.ihmc.steppr.hardware.state.slowSensors.IMUMagSensor;
import us.ihmc.steppr.hardware.state.slowSensors.InphaseControlEffort;
import us.ihmc.steppr.hardware.state.slowSensors.MotorTemperature;
import us.ihmc.steppr.hardware.state.slowSensors.MotorThermistorADTicks;
import us.ihmc.steppr.hardware.state.slowSensors.PressureSensor;
import us.ihmc.steppr.hardware.state.slowSensors.QuadratureControlEffort;
import us.ihmc.steppr.hardware.state.slowSensors.RawEncoderTicks;
import us.ihmc.steppr.hardware.state.slowSensors.RawPhaseCurrentADTicks;
import us.ihmc.steppr.hardware.state.slowSensors.SensorMCUTime;
import us.ihmc.steppr.hardware.state.slowSensors.StatorHalSwitches;
import us.ihmc.steppr.hardware.state.slowSensors.StepprSlowSensor;
import us.ihmc.steppr.hardware.state.slowSensors.StrainSensor;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;

public class StepprActuatorState
{
   private final StepprSlowSensor[] slowSensors = new StepprSlowSensor[30];

   private final YoVariableRegistry registry;

   private final double motorKt;
   private final int SensedCurrentToTorqueDirection;

   private final LongYoVariable microControllerTime;

   private final DoubleYoVariable inphaseCompositeStatorCurrent;
   private final DoubleYoVariable quadratureCompositeStatorCurrent;

   private final DoubleYoVariable controlTarget;

   private final DoubleYoVariable motorEncoderPosition;
   private final DoubleYoVariable motorVelocityEstimate;

   private final DoubleYoVariable jointEncoderPosition;
   private final DoubleYoVariable jointEncoderVelocity;

   private final DoubleYoVariable motorPower;

   private final LongYoVariable lastReceivedControlID;

   private final int[] slowSensorSlotIDs = new int[7];

   private final LongYoVariable checksumFailures;

   private final DoubleYoVariable motorAngleOffset;

   private final int STRAIN_SENSOR_BASE_15 = 15;
   private final int PRESSURE_SENSOR_BASE_11 = 11;
   
   private long lastmicroControllerTime;
   private final LongYoVariable consecutivePacketDropCount;
   private final LongYoVariable totalPacketDropCount;

   public StepprActuatorState(String name, double motorKt, int SensedCurrentToTorqueDirection, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.motorKt = motorKt;
      this.SensedCurrentToTorqueDirection = SensedCurrentToTorqueDirection;
      this.microControllerTime = new LongYoVariable(name + "MicroControllerTime", registry);

      this.inphaseCompositeStatorCurrent = new DoubleYoVariable(name + "InphaseCompositeStatorCurrent", registry);
      this.quadratureCompositeStatorCurrent = new DoubleYoVariable(name + "QuadratureCompositeStatorCurrent", registry);
      this.controlTarget = new DoubleYoVariable(name + "ControlTarget", registry);

      this.motorEncoderPosition = new DoubleYoVariable(name + "MotorEncoderPosition", registry);
      this.motorVelocityEstimate = new DoubleYoVariable(name + "MotorVelocityEstimate", registry);

      this.jointEncoderPosition = new DoubleYoVariable(name + "JointEncoderPosition", registry);
      this.jointEncoderVelocity = new DoubleYoVariable(name + "JointEncoderVelocity", registry);

      this.motorPower = new DoubleYoVariable(name + "MotorPower", registry);

      this.lastReceivedControlID = new LongYoVariable(name + "LastReceivedControlID", registry);

      this.motorAngleOffset = new DoubleYoVariable(name + "MotorAngleOffset", registry);
      
      this.consecutivePacketDropCount = new LongYoVariable(name + "ConsecutivePacketDropCount", registry);
      this.totalPacketDropCount = new LongYoVariable(name + "TotalPacketDropCount", registry);

      this.checksumFailures = new LongYoVariable("checksumFailures", registry);

      createSlowSensors(name);

      parentRegistry.addChild(registry);
   }

   private void createSlowSensors(String name)
   {

      YoVariableRegistry slowSensorRegistry = new YoVariableRegistry("SlowSensors");
      slowSensors[0] = new StatorHalSwitches(name, slowSensorRegistry);
      slowSensors[1] = new RawEncoderTicks(name, slowSensorRegistry);

      slowSensors[2] = new RawPhaseCurrentADTicks(name, "A", slowSensorRegistry);
      slowSensors[3] = new RawPhaseCurrentADTicks(name, "B", slowSensorRegistry);
      slowSensors[4] = new RawPhaseCurrentADTicks(name, "C", slowSensorRegistry);

      slowSensors[5] = new ControlMode(name, slowSensorRegistry);

      slowSensors[6] = new MotorThermistorADTicks(name, slowSensorRegistry);
      slowSensors[7] = new ControllerTemperature1(name, slowSensorRegistry);
      slowSensors[8] = new BusVoltage(name, slowSensorRegistry);

      slowSensors[9] = new InphaseControlEffort(name, slowSensorRegistry);
      slowSensors[10] = new QuadratureControlEffort(name, slowSensorRegistry);

      slowSensors[11] = new PressureSensor(name, 0, slowSensorRegistry);
      slowSensors[12] = new PressureSensor(name, 1, slowSensorRegistry);
      slowSensors[13] = new PressureSensor(name, 2, slowSensorRegistry);
      slowSensors[14] = new PressureSensor(name, 3, slowSensorRegistry);

      slowSensors[STRAIN_SENSOR_BASE_15 + 0] = new StrainSensor(name, 0, slowSensorRegistry);
      slowSensors[STRAIN_SENSOR_BASE_15 + 1] = new StrainSensor(name, 1, slowSensorRegistry);
      slowSensors[STRAIN_SENSOR_BASE_15 + 2] = new StrainSensor(name, 2, slowSensorRegistry);

      slowSensors[18] = new IMUAccelSensor(name, "X", slowSensorRegistry);
      slowSensors[19] = new IMUAccelSensor(name, "Y", slowSensorRegistry);
      slowSensors[20] = new IMUAccelSensor(name, "Z", slowSensorRegistry);

      slowSensors[21] = new IMUGyroSensor(name, "X", slowSensorRegistry);
      slowSensors[22] = new IMUGyroSensor(name, "Y", slowSensorRegistry);
      slowSensors[23] = new IMUGyroSensor(name, "Z", slowSensorRegistry);

      slowSensors[24] = new IMUMagSensor(name, "X", slowSensorRegistry);
      slowSensors[25] = new IMUMagSensor(name, "Y", slowSensorRegistry);
      slowSensors[26] = new IMUMagSensor(name, "Z", slowSensorRegistry);

      slowSensors[27] = new SensorMCUTime(name, slowSensorRegistry);
      slowSensors[28] = new ControllerTemperature2(name, slowSensorRegistry);
      slowSensors[29] = new MotorTemperature(name, slowSensorRegistry);
      this.registry.addChild(slowSensorRegistry);
   }

   private double getInphaseControlEffort()
   {
      return ((InphaseControlEffort) slowSensors[9]).getValue();
   }

   private double getQuadratureControlEffort()
   {
      return ((QuadratureControlEffort) slowSensors[10]).getValue();
   }

   public void getIMUAccelerationVector(Vector3d vectorToPack)
   {
      double x = ((IMUAccelSensor) slowSensors[18]).getValue();
      double y = ((IMUAccelSensor) slowSensors[19]).getValue();
      double z = ((IMUAccelSensor) slowSensors[20]).getValue();
      vectorToPack.set(x, y, z);
   }

   public void getIMUMagnetoVector(Vector3d vectorToPack)
   {
      double x = ((IMUMagSensor) slowSensors[24]).getValue();
      double y = ((IMUMagSensor) slowSensors[25]).getValue();
      double z = ((IMUMagSensor) slowSensors[26]).getValue();
      vectorToPack.set(x, y, z);

   }

   public void update(ByteBuffer buffer) throws IOException
   {
      lastmicroControllerTime = microControllerTime.getLongValue();
      // Values are of no use to us
      @SuppressWarnings("unused")
      int frameFormat = buffer.get() & 0xFF;
      @SuppressWarnings("unused")
      int stateDelay = buffer.getShort() & 0xFFFF;
      @SuppressWarnings("unused")
      int length = buffer.get() & 0xFF;

      buffer.mark();

      int calculatedChecksum = 0;
      for (int i = 0; i < 58; i++)
      {
         calculatedChecksum = (calculatedChecksum + (buffer.get() & 0xFF)) & 0xFF;
      }
      int checksum = buffer.get() & 0xFF;

      @SuppressWarnings("unused")
      int unused = buffer.get();

      if (calculatedChecksum != checksum)
      {
         checksumFailures.increment();
         totalPacketDropCount.increment();
         consecutivePacketDropCount.increment();
         return;
      }
      buffer.reset();

      // Only one format is defined for now
      @SuppressWarnings("unused")
      int stateFormat = buffer.get() & 0xFF;

      for (int i = 0; i < slowSensorSlotIDs.length; i++)
      {
         slowSensorSlotIDs[i] = buffer.get() & 0xFF;
      }

      microControllerTime.set(buffer.getInt() & 0xFFFFFFFFl);
      inphaseCompositeStatorCurrent.set(buffer.getFloat());
      quadratureCompositeStatorCurrent.set(buffer.getFloat());
      controlTarget.set(buffer.getFloat());
      motorEncoderPosition.set(buffer.getFloat() + motorAngleOffset.getDoubleValue());
      motorVelocityEstimate.set(buffer.getFloat());

      jointEncoderPosition.set(buffer.getFloat());
      jointEncoderVelocity.set(buffer.getFloat());

      lastReceivedControlID.set(buffer.getInt() & 0xFFFFFFFFl);

      for (int i = 0; i < slowSensorSlotIDs.length; i++)
      {
         int id = slowSensorSlotIDs[i];
         int slowSensorValue = buffer.getShort() & 0xFFFF;

         if (id < slowSensors.length)
         {
            if (slowSensors[id] != null)
            {
               slowSensors[id].update(slowSensorValue);
            }
         }
      }

      checksum = buffer.get() & 0xFF;

      unused = buffer.get();

      motorPower.set(3.0 / 4.0 * (quadratureCompositeStatorCurrent.getDoubleValue() * getQuadratureControlEffort() + inphaseCompositeStatorCurrent
            .getDoubleValue() * getInphaseControlEffort()));
      
      updatePacketDropStatistics();

   }

   public StrainSensor getStrainGuage(int id)
   {
      return (StrainSensor) (slowSensors[STRAIN_SENSOR_BASE_15 + id]);
   }

   public PressureSensor getPressureSensor(int id)
   {
      return (PressureSensor) (slowSensors[PRESSURE_SENSOR_BASE_11 + id]);
   }

   public void updateCanonicalAngle(double angle, double clocking)
   {
      double delta = angle - (motorEncoderPosition.getDoubleValue() - motorAngleOffset.getDoubleValue());
      double offset = Math.round(delta / clocking) * clocking;
      motorAngleOffset.set(offset);

   }

   public double getMotorPosition()
   {
      return motorEncoderPosition.getDoubleValue();
   }

   public double getMotorVelocity()
   {
      return motorVelocityEstimate.getDoubleValue();
   }

   public double getJointPosition()
   {
      return jointEncoderPosition.getDoubleValue();
   }

   public double getJointVelocity()
   {
      return jointEncoderVelocity.getDoubleValue();
   }

   public double getMotorTorque()
   {
      return quadratureCompositeStatorCurrent.getDoubleValue() * motorKt * SensedCurrentToTorqueDirection;
   }

   public StepprSlowSensor[] getSlowSensors()
   {
      return slowSensors;
   }

   public double getMotorPower()
   {
      return motorPower.getDoubleValue();
   }
   
   public long getConsecutivePacketDropCount()
   {
      return consecutivePacketDropCount.getValueAsLongBits();
   }
   
   public boolean isLastPacketDropped()
   {
      return (lastmicroControllerTime==microControllerTime.getLongValue()) && lastmicroControllerTime!=0;
   }
   
   private void updatePacketDropStatistics()
   {
      if(isLastPacketDropped())
      {
         totalPacketDropCount.increment();
         consecutivePacketDropCount.increment();
      }
      else
      {
         consecutivePacketDropCount.set(0);
      }
            
   }
}
