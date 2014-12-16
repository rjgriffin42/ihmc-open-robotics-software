package us.ihmc.steppr.hardware.state;

import us.ihmc.steppr.hardware.state.slowSensors.StrainSensor;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class StepprLinearTransmissionJointState implements StepprJointState
{
   private final YoVariableRegistry registry;

   private final StepprActuatorState actuator;

   private final StrainSensor strainSensor;
   private final double ratio;
   private final boolean hasOutputEncoder;

   private double motorAngle;
   
   private final DoubleYoVariable q;
   private final DoubleYoVariable qd;
   private final DoubleYoVariable tau_strain;
   private final DoubleYoVariable tau_current;

   public StepprLinearTransmissionJointState(String name, double ratio, boolean hasOutputEncoder, StepprActuatorState actuator, StrainSensor strainSensor, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.ratio = ratio;
      this.actuator = actuator;
      this.hasOutputEncoder = hasOutputEncoder;
      this.strainSensor = strainSensor;

      this.q = new DoubleYoVariable(name + "_q", registry);
      this.qd = new DoubleYoVariable(name + "_qd", registry);
      this.tau_current = new DoubleYoVariable(name + "_tauPredictedCurrent", registry);
      this.tau_strain = new DoubleYoVariable(name + "_tauMeasuredStrain", registry);
      
      parentRegistry.addChild(registry);
   }

   @Override
   public double getQ()
   {
      return q.getDoubleValue();
   }

   @Override
   public double getQd()
   {
      return qd.getDoubleValue();
   }

   @Override
   public double getTau()
   {
      return tau_current.getDoubleValue();
   }

   @Override
   public void update()
   {
      
      motorAngle = actuator.getMotorPosition();
      if(hasOutputEncoder)
      {
         q.set(AngleTools.trimAngleMinusPiToPi(actuator.getJointPosition()));
         qd.set(actuator.getJointVelocity());
      }
      else
      {
         q.set(AngleTools.trimAngleMinusPiToPi(actuator.getMotorPosition() / ratio)); 
         qd.set(actuator.getMotorVelocity() / ratio); 
      }
      tau_current.set(actuator.getMotorTorque() * ratio);
      if(strainSensor!=null)
         tau_strain.set(strainSensor.getCalibratedValue());
   }

   @Override
   public int getNumberOfActuators()
   {
      return 1;
   }

   @Override
   public double getMotorAngle(int actuator)
   {
      return motorAngle;
   }

   @Override
   public void updateOffsets()
   {
   }

}
