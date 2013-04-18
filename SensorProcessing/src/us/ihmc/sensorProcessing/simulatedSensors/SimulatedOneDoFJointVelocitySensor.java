package us.ihmc.sensorProcessing.simulatedSensors;

import org.apache.commons.lang.mutable.MutableDouble;

import us.ihmc.controlFlow.ControlFlowOutputPort;

import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;

public class SimulatedOneDoFJointVelocitySensor extends SimulatedSensor<MutableDouble>
{
   private final ControlFlowOutputPort<Double> jointVelocityOutputPort = createOutputPort();
   private final OneDegreeOfFreedomJoint joint;
   private final MutableDouble jointVelocity = new MutableDouble();

   public SimulatedOneDoFJointVelocitySensor(String name, OneDegreeOfFreedomJoint joint)
   {
      this.joint = joint;
   }

   public void startComputation()
   {
      jointVelocity.setValue(joint.getQD().getDoubleValue());
      corrupt(jointVelocity);
      jointVelocityOutputPort.setData(jointVelocity.toDouble());
   }

   public void waitUntilComputationIsDone()
   {
      // empty
   }

   public ControlFlowOutputPort<Double> getJointVelocityOutputPort()
   {
      return jointVelocityOutputPort;
   }
}
