package us.ihmc.sensorProcessing.simulatedSensors;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

public class JointAndIMUSensorMap
{
   private final LinkedHashMap<OneDoFJoint, ControlFlowOutputPort<Double>> jointPositionSensors = new LinkedHashMap<OneDoFJoint,
                                                                                                     ControlFlowOutputPort<Double>>();
   private final LinkedHashMap<OneDoFJoint, ControlFlowOutputPort<Double>> jointVelocitySensors = new LinkedHashMap<OneDoFJoint,
                                                                                                     ControlFlowOutputPort<Double>>();

   private final LinkedHashMap<IMUDefinition, ControlFlowOutputPort<Matrix3d>> orientationSensors = new LinkedHashMap<IMUDefinition,
                                                                                                       ControlFlowOutputPort<Matrix3d>>();
   private final LinkedHashMap<IMUDefinition, ControlFlowOutputPort<Vector3d>> angularVelocitySensors = new LinkedHashMap<IMUDefinition,
                                                                                                           ControlFlowOutputPort<Vector3d>>();
   private final LinkedHashMap<IMUDefinition, ControlFlowOutputPort<Vector3d>> linearAccelerationSensors = new LinkedHashMap<IMUDefinition,
                                                                                                              ControlFlowOutputPort<Vector3d>>();

   public JointAndIMUSensorMap()
   {
   }

   public Map<OneDoFJoint, ControlFlowOutputPort<Double>> getJointPositionSensors()
   {
      return jointPositionSensors;
   }

   public Map<OneDoFJoint, ControlFlowOutputPort<Double>> getJointVelocitySensors()
   {
      return jointVelocitySensors;
   }

   public Map<IMUDefinition, ControlFlowOutputPort<Matrix3d>> getOrientationSensors()
   {
      return orientationSensors;
   }

   public Map<IMUDefinition, ControlFlowOutputPort<Vector3d>> getAngularVelocitySensors()
   {
      return angularVelocitySensors;
   }

   public Map<IMUDefinition, ControlFlowOutputPort<Vector3d>> getLinearAccelerationSensors()
   {
      return linearAccelerationSensors;
   }

   public ControlFlowOutputPort<Double> getJointPositionSensorPort(OneDoFJoint oneDoFJoint)
   {
      return jointPositionSensors.get(oneDoFJoint);
   }

   public ControlFlowOutputPort<Double> getJointVelocitySensorPort(OneDoFJoint oneDoFJoint)
   {
      return jointVelocitySensors.get(oneDoFJoint);
   }

   public ControlFlowOutputPort<Matrix3d> getOrientationSensorPort(IMUDefinition imuDefinition)
   {
      return orientationSensors.get(imuDefinition);
   }

   public ControlFlowOutputPort<Vector3d> getAngularVelocitySensorPort(IMUDefinition imuDefinition)
   {
      return angularVelocitySensors.get(imuDefinition);
   }

   public ControlFlowOutputPort<Vector3d> getLinearAccelerationSensorPort(IMUDefinition imuDefinition)
   {
      return linearAccelerationSensors.get(imuDefinition);
   }

   public void addJointPositionSensorPort(OneDoFJoint oneDoFJoint, ControlFlowOutputPort<Double> jointPositionSensorPort)
   {
      jointPositionSensors.put(oneDoFJoint, jointPositionSensorPort);
   }

   public void addJointVelocitySensorPort(OneDoFJoint oneDoFJoint, ControlFlowOutputPort<Double> jointVelocitySensorPort)
   {
      jointVelocitySensors.put(oneDoFJoint, jointVelocitySensorPort);
   }

   public void addOrientationSensorPort(IMUDefinition imuDefinition, ControlFlowOutputPort<Matrix3d> orientationSensorPort)
   {
      orientationSensors.put(imuDefinition, orientationSensorPort);
   }

   public void addAngularVelocitySensorPort(IMUDefinition imuDefinition, ControlFlowOutputPort<Vector3d> angularVelocitySensorPort)
   {
      angularVelocitySensors.put(imuDefinition, angularVelocitySensorPort);
   }

   public void addLinearAccelerationSensorPort(IMUDefinition imuDefinition, ControlFlowOutputPort<Vector3d> linearAccelerationSensorPort)
   {
      linearAccelerationSensors.put(imuDefinition, linearAccelerationSensorPort);
   }

   public Collection<ControlFlowOutputPort<Matrix3d>> getOrientationOutputPorts()
   {
      return orientationSensors.values();
   }

   public Collection<ControlFlowOutputPort<Vector3d>> getAngularVelocityOutputPorts()
   {
      return angularVelocitySensors.values();
   }

   public Collection<ControlFlowOutputPort<Vector3d>> getLinearAccelerationOutputPorts()
   {
      return linearAccelerationSensors.values();
   }

}
