package us.ihmc.sensorProcessing.sensorData;

import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.math.filters.AlphaFilteredYoVariable;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.CenterOfMassCalculator;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.robotics.sensors.ForceSensorData;
import us.ihmc.robotics.sensors.ForceSensorDefinition;

public class ForceSensorDistalMassCompensator
{
   private final double GRAVITY = 9.81;

   private final ReferenceFrame world = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame sensorFrame;

   private final FramePose sensorPose;
   private final YoFramePoint yoSensorPositionInWorld;

   private final CenterOfMassCalculator distalMassCalc;
   private final YoDouble distalMass;
   private final AlphaFilteredYoVariable lowPassSensorForceZ;
   private final YoFrameVector distalMassForceInWorld;
   private final YoFramePoint distalCoMInWorld;

   private final YoFrameVector yoSensorToDistalCoMvectorInWorld;
   private final Wrench distalMassWrench;

   private final YoFrameVector yoSensorForce;
   private final YoFrameVector yoSensorTorque;

   private final YoFrameVector yoSensorForceFromDistalMass;
   private final YoFrameVector yoSensorTorqueFromDistalMass;

   private final YoFrameVector yoSensorForceMassCompensated;
   private final YoFrameVector yoSensorTorqueMassCompensated;
   
   private final YoBoolean addSimulatedSensorNoise;

   public ForceSensorDistalMassCompensator(ForceSensorDefinition forceSensorDefinition, double dtForLowpassFilter, YoVariableRegistry registry)
   {
      String sensorName = forceSensorDefinition.getSensorName();

      InverseDynamicsJoint parentJointOfSensorBody = forceSensorDefinition.getRigidBody().getParentJoint();
      sensorFrame = forceSensorDefinition.getSensorFrame();

      sensorPose = new FramePose(world);
      yoSensorPositionInWorld = new YoFramePoint(sensorName + "Position", world, registry);

      distalMassCalc = new CenterOfMassCalculator(ScrewTools.computeRigidBodiesAfterThisJoint(parentJointOfSensorBody), world);
      distalMass = new YoDouble(sensorName + "DistalMass", registry);
      lowPassSensorForceZ = new AlphaFilteredYoVariable(sensorName + "LowPassFz", registry, AlphaFilteredYoVariable.computeAlphaGivenBreakFrequencyProperly(0.0001, dtForLowpassFilter));
      distalMassForceInWorld = new YoFrameVector(sensorName + "DistalWeight", world, registry);
      distalCoMInWorld = new YoFramePoint(sensorName + "DistalCoM", world, registry);

      yoSensorToDistalCoMvectorInWorld = new YoFrameVector(sensorName + "ToDistalCoM", world, registry);
      distalMassWrench = new Wrench(sensorFrame, world);

      // Put sensor values in world frame since it's easy to interpret from looking at GUI
      yoSensorForce = new YoFrameVector(sensorName + "Force", world, registry);
      yoSensorTorque = new YoFrameVector(sensorName + "Torque", world, registry);

      yoSensorForceFromDistalMass = new YoFrameVector(sensorName + "ForceDueToDistalMass", world, registry);
      yoSensorTorqueFromDistalMass = new YoFrameVector(sensorName + "TorqueDueToDistalMass", world, registry);

      yoSensorForceMassCompensated = new YoFrameVector(sensorName + "ForceMassCompensated", world, registry);
      yoSensorTorqueMassCompensated = new YoFrameVector(sensorName + "TorqueMassCompensated", world, registry);
      
      addSimulatedSensorNoise = new YoBoolean(sensorName + "AddSimulatedNoise", registry);
      addSimulatedSensorNoise.set(false);
   }

   public double getDistalMass()
   {
      return distalMass.getDoubleValue();
   }

   public FramePoint3D getSensorPosition()
   {
      return yoSensorPositionInWorld.getFrameTuple();
   }
   
   public ReferenceFrame getSensorReferenceFrame()
   {
      return sensorFrame;
   }

   public FrameVector3D getSensorForceRaw(ReferenceFrame desiredFrame)
   {
      FrameVector3D force = yoSensorForce.getFrameTuple();
      force.changeFrame(desiredFrame);
      
      return force;
   }

   public FrameVector3D getSensorTorqueRaw(ReferenceFrame desiredFrame)
   {
      FrameVector3D torque = yoSensorTorque.getFrameTuple();
      torque.changeFrame(desiredFrame);
      
      return torque;
   }

   public FrameVector3D getSensorForceMassCompensated(ReferenceFrame desiredFrame)
   {
      FrameVector3D force = yoSensorForceMassCompensated.getFrameTuple();
      force.changeFrame(desiredFrame);
      
      return force;
   }

   public FrameVector3D getSensorTorqueMassCompensated(ReferenceFrame desiredFrame)
   {
      FrameVector3D torque = yoSensorTorqueMassCompensated.getFrameTuple();
      torque.changeFrame(desiredFrame);
      
      return torque;
   }
   
   public double getSensorZForceLowPassFilteredInWorld()
   {
      return lowPassSensorForceZ.getDoubleValue();
   }


   private final Wrench sensorWrench = new Wrench();

   public void update(ForceSensorData forceSensorData)
   {
      forceSensorData.getWrench(sensorWrench);
      update(sensorWrench);
   }

   public void update(Wrench sensorWrench)
   {
      sensorWrench.changeFrame(world);

      yoSensorForce.set(sensorWrench.getExpressedInFrame(), sensorWrench.getLinearPartX(), sensorWrench.getLinearPartY(), sensorWrench.getLinearPartZ());
      yoSensorTorque.set(sensorWrench.getExpressedInFrame(), sensorWrench.getAngularPartX(), sensorWrench.getAngularPartY(), sensorWrench.getAngularPartZ());

      if (addSimulatedSensorNoise.getBooleanValue())
      {
         double amp = 0.1;
         double bias = 0.25;
         
         yoSensorForce.add(amp*2.0*(Math.random()-0.5)+bias, amp*2.0*(Math.random()-0.5)+bias, amp*2.0*(Math.random()-0.5)+bias);
         yoSensorTorque.add(amp*2.0*(Math.random()-0.5)+bias, amp*2.0*(Math.random()-0.5)+bias, amp*2.0*(Math.random()-0.5)+bias);
      }
      
      updateSensorPosition();
      updateCenterOfMass();
      yoSensorToDistalCoMvectorInWorld.sub(distalCoMInWorld, yoSensorPositionInWorld);

      distalMassWrench.setToZero(world);
      distalMassWrench.setUsingArm(world, distalMassForceInWorld.getFrameTuple().getVector(), yoSensorToDistalCoMvectorInWorld.getFrameTuple().getVector());

      yoSensorForceFromDistalMass.set(distalMassWrench.getExpressedInFrame(), distalMassWrench.getLinearPartX(), distalMassWrench.getLinearPartY(), distalMassWrench.getLinearPartZ());
      yoSensorTorqueFromDistalMass.set(distalMassWrench.getExpressedInFrame(), distalMassWrench.getAngularPartX(), distalMassWrench.getAngularPartY(), distalMassWrench.getAngularPartZ());

      yoSensorForceMassCompensated.sub(yoSensorForce, yoSensorForceFromDistalMass);
      yoSensorTorqueMassCompensated.sub(yoSensorTorque, yoSensorTorqueFromDistalMass);
   }

   private final FramePoint3D temp = new FramePoint3D();

   private void updateSensorPosition()
   {
      sensorFrame.update();

      sensorPose.setPose(sensorFrame.getTransformToDesiredFrame(world));
      sensorPose.getPositionIncludingFrame(temp);
      yoSensorPositionInWorld.set(temp.getReferenceFrame(), temp.getX(), temp.getY(), temp.getZ());
   }

   private void updateCenterOfMass()
   {
      distalMassCalc.compute();
      distalMass.set(distalMassCalc.getTotalMass());
      distalMassForceInWorld.set(0.0, 0.0, Math.abs(GRAVITY) * distalMass.getDoubleValue());

      FramePoint3D distalCoMinWorld = distalMassCalc.getCenterOfMass();
      distalCoMInWorld.set(distalCoMinWorld);
      
      lowPassSensorForceZ.update(yoSensorForce.getZ());
   }
}
