package us.ihmc.commonWalkingControlModules.sensors;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Vector3d;

import org.junit.Test;

import us.ihmc.commonWalkingControlModules.configurations.ArmControllerParameters;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.robotics.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.SpatialAccelerationVector;
import us.ihmc.utilities.screwTheory.Wrench;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public abstract class ProvidedMassMatrixToolRigidBodyTest
{
   private final YoVariableRegistry registry = new YoVariableRegistry("ProvidedMassMatrixToolRigidBodyTest");
   private final RobotSide robotSide = RobotSide.LEFT;
   private final double gravity = 9.81;
   private final double mass = 2.0;
   
   @EstimatedDuration(duration = 30.0)
   @Test(timeout = 120000)
   public void testprovidedMassMatrixToolRigidBody()
   {
      FullRobotModel fullRobotModel = getFullRobotModel();
      
      ReferenceFrame elevatorFrame = fullRobotModel.getElevatorFrame();
      
      ProvidedMassMatrixToolRigidBody providedMassMatrixToolRigidBody = new ProvidedMassMatrixToolRigidBody(robotSide, fullRobotModel, gravity, getArmControllerParameters(), registry, null);
      providedMassMatrixToolRigidBody.setMass(mass);
      
      SpatialAccelerationVector handSpatialAccelerationVector = new SpatialAccelerationVector(elevatorFrame, elevatorFrame, elevatorFrame);
      Wrench toolWrench = new Wrench();
      
      providedMassMatrixToolRigidBody.control(handSpatialAccelerationVector, toolWrench);
      
      toolWrench.changeFrame(ReferenceFrame.getWorldFrame());
      
      assertTrue(toolWrench.getLinearPart().epsilonEquals(new Vector3d(0.0, 0.0, - mass * gravity), 10e-5));
   }

   public abstract FullRobotModel getFullRobotModel();
   
   public abstract ArmControllerParameters getArmControllerParameters();
}
