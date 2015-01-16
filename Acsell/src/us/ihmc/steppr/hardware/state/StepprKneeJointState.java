package us.ihmc.steppr.hardware.state;

import us.ihmc.steppr.hardware.StepprJoint;
import us.ihmc.steppr.hardware.state.slowSensors.StrainSensor;
import us.ihmc.utilities.math.geometry.AngleTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class StepprKneeJointState implements StepprJointState
{
   private final YoVariableRegistry registry;

   private final StepprActuatorState actuator;
   private final StepprActuatorState ankle;
   private final StrainSensor strainSensor;
   

   private final double ratio;

   private double motorAngle;
   
   private final DoubleYoVariable q;
   private final DoubleYoVariable qd;
   private final DoubleYoVariable tau_current;
   private final DoubleYoVariable tau_strain;

   public StepprKneeJointState(StepprJoint joint, StepprActuatorState actuator, StepprActuatorState ankle, StrainSensor strainSesnor, YoVariableRegistry parentRegistry)
   {
      String name = joint.getSdfName();
      this.registry = new YoVariableRegistry(name);
      this.ratio = joint.getRatio();
      this.actuator = actuator;
      this.ankle = ankle;
      this.strainSensor = strainSesnor;
      
      
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
      
      final long toleranceWindowSize=1;
      if(ankle.getConsecutivePacketDropCount()<=toleranceWindowSize && actuator.getConsecutivePacketDropCount()<=toleranceWindowSize)
      {
         double ankleAngle = ankle.getMotorPosition();
         double ankleVelocity = ankle.getMotorVelocity();      
         q.set(AngleTools.trimAngleMinusPiToPi(actuator.getJointPosition() + ankleAngle));
         qd.set(actuator.getJointVelocity() + ankleVelocity);
      }
      
      motorAngle = actuator.getMotorPosition();      
      tau_current.set(actuator.getMotorTorque() * ratio);
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
