package us.ihmc.steppr.hardware.command;

import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolder;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class StepprJointCommand
{
   private final YoVariableRegistry registry;
   
   private final DoubleYoVariable tauDesired;
   private final DoubleYoVariable damping;
   
   private final int numberOfActuators;
   private final double[] motorAngles;
   
   private double q, qd;
   
   
   public StepprJointCommand(String name, int numberOfActuators, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.tauDesired = new DoubleYoVariable(name + "TauDesired", registry);
      this.damping = new DoubleYoVariable(name + "Damping", registry);
      this.numberOfActuators = numberOfActuators;
      this.motorAngles = new double[numberOfActuators];
   }
      
   public void setTauDesired(double tau, RawJointSensorDataHolder rawSensorData)
   {
      this.tauDesired.set(tau);
      
      this.q = rawSensorData.getQ_raw();
      this.qd = rawSensorData.getQd_raw();
      for(int i = 0; i < numberOfActuators; i++)
      {
         motorAngles[i] = rawSensorData.getMotorAngle(i);
      }
   }
   
   public double getQ()
   {
      return q;
   }
   
   public double getQd()
   {
      return qd;
   }
   
   public double getTauDesired()
   {
      return tauDesired.getDoubleValue();
   }
   
   public int getNumberOfActuators()
   {
      return numberOfActuators;
   }
   
   public double getMotorAngle(int actuator)
   {
      return motorAngles[actuator];
   }
   
   public double getDamping()
   {
      return damping.getDoubleValue();
   }
   
   public void setDamping(double value)
   {
      damping.set(value);
   }
}
