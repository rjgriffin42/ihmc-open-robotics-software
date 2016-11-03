package us.ihmc.darpaRoboticsChallenge.controllers;

import us.ihmc.simulationconstructionset.FloatingRootJointRobot;
import us.ihmc.robotics.controllers.PIDController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.robotController.RobotController;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;

public class PIDLidarTorqueController implements RobotController
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final PIDController lidarJointController = new PIDController("lidar", registry);
   
   private final DoubleYoVariable desiredLidarAngle = new DoubleYoVariable("desiredLidarAngle", registry);
   private final DoubleYoVariable desiredLidarVelocity = new DoubleYoVariable("desiredLidarVelocity", registry);

   private final double controlDT;
   private final OneDegreeOfFreedomJoint  lidarJoint;

   public PIDLidarTorqueController(FloatingRootJointRobot robot, String jointName, double desiredSpindleSpeed, double controlDT)
   {
      this.controlDT = controlDT;
      this.lidarJoint = robot.getOneDegreeOfFreedomJoint(jointName);

      desiredLidarVelocity.set(desiredSpindleSpeed);
      lidarJointController.setProportionalGain(10.0);
      lidarJointController.setDerivativeGain(1.0);
   }

   public void doControl()
   {
      desiredLidarAngle.add(desiredLidarVelocity.getDoubleValue() * controlDT);
         
      double lidarJointTau = lidarJointController.compute(lidarJoint.getQYoVariable().getDoubleValue(), desiredLidarAngle.getDoubleValue(), lidarJoint.getQDYoVariable()
            .getDoubleValue(), desiredLidarVelocity.getDoubleValue(), controlDT) + lidarJoint.getDamping() * desiredLidarVelocity.getDoubleValue();
      lidarJoint.setTau(lidarJointTau);
   }

   public void initialize()
   {
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }

   public String getDescription()
   {
      return getName();
   }
}
