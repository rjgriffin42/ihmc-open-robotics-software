package us.ihmc.darpaRoboticsChallenge.controllers;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.OneDegreeOfFreedomJoint;
import com.yobotics.simulationconstructionset.robotController.RobotController;
import com.yobotics.simulationconstructionset.util.controller.PIDController;

public class PIDLidarTorqueController implements RobotController
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final PIDController lidarJointController = new PIDController("lidar", registry);
   
   private final DoubleYoVariable desiredLidarAngle = new DoubleYoVariable("desiredLidarAngle", registry);
   private final DoubleYoVariable desiredLidarVelocity = new DoubleYoVariable("desiredLidarVelocity", registry);

   private final double controlDT;
   private final OneDegreeOfFreedomJoint  lidarJoint;

   public PIDLidarTorqueController(SDFRobot robot, String jointName, double desiredSpindleSpeed, double controlDT)
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
         
      double lidarJointTau = lidarJointController.compute(lidarJoint.getQ().getDoubleValue(), desiredLidarAngle.getDoubleValue(), lidarJoint.getQD()
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
