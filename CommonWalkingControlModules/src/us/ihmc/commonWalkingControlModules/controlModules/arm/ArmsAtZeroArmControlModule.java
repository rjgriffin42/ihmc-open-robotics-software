package us.ihmc.commonWalkingControlModules.controlModules.arm;

import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class ArmsAtZeroArmControlModule extends PDArmControlModule
{
   public ArmsAtZeroArmControlModule(ProcessedSensorsInterface processedSensors, double controlDT, YoVariableRegistry parentRegistry)
   {
      super(processedSensors, controlDT, parentRegistry);
   }

   protected void computeDesireds()
   {
      // do nothing, keep at zero.
   }

   protected void setGains()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setProportionalGain(100.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setProportionalGain(200.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_YAW).setProportionalGain(100.0);
         armControllers.get(robotSide).get(ArmJointName.ELBOW_PITCH).setProportionalGain(100.0);
         armControllers.get(robotSide).get(ArmJointName.WRIST_ROLL).setProportionalGain(100.0);
         armControllers.get(robotSide).get(ArmJointName.WRIST_PITCH).setProportionalGain(100.0);

         armControllers.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setDerivativeGain(10.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setDerivativeGain(20.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_YAW).setDerivativeGain(10.0);
         armControllers.get(robotSide).get(ArmJointName.ELBOW_PITCH).setDerivativeGain(10.0);
         armControllers.get(robotSide).get(ArmJointName.WRIST_ROLL).setDerivativeGain(10.0);
         armControllers.get(robotSide).get(ArmJointName.WRIST_PITCH).setDerivativeGain(10.0);
      }
   }
}

