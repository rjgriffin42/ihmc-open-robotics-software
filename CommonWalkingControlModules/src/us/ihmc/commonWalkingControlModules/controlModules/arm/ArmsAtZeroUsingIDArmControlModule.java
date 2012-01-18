package us.ihmc.commonWalkingControlModules.controlModules.arm;

import java.util.EnumMap;

import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.InverseDynamicsCalculator;
import us.ihmc.utilities.screwTheory.RevoluteJoint;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class ArmsAtZeroUsingIDArmControlModule extends IDArmControlModule
{
   public ArmsAtZeroUsingIDArmControlModule(ProcessedSensorsInterface processedSensors, double controlDT, YoVariableRegistry parentRegistry, InverseDynamicsCalculator armsIDCalculator, SideDependentList<EnumMap<ArmJointName, RevoluteJoint>> armJointArrayLists)
    {
      super(processedSensors, controlDT, parentRegistry, armsIDCalculator, armJointArrayLists);
   }

   protected void setDesiredJointPositionsAndVelocities()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
//         desiredArmJointPositions.get(robotSide).get(ArmJointName.SHOULDER_ROLL).set(robotSide.negateIfRightSide(Math.PI / 2.0));
//         desiredArmJointPositions.get(robotSide).get(ArmJointName.SHOULDER_YAW).set(robotSide.negateIfRightSide(0.7));
      }
   }

   protected void setGains()
   {
      
      double gainScaling = 1.0;
      for (RobotSide robotSide : RobotSide.values())
      {
         armDesiredQddControllers.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setProportionalGain(gainScaling * 100.0);
         armDesiredQddControllers.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setProportionalGain(gainScaling * 200.0);
         armDesiredQddControllers.get(robotSide).get(ArmJointName.SHOULDER_YAW).setProportionalGain(gainScaling * 100.0);
         armDesiredQddControllers.get(robotSide).get(ArmJointName.ELBOW).setProportionalGain(gainScaling * 100.0);

         armDesiredQddControllers.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setDerivativeGain(gainScaling * 10.0);
         armDesiredQddControllers.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setDerivativeGain(gainScaling * 20.0);
         armDesiredQddControllers.get(robotSide).get(ArmJointName.SHOULDER_YAW).setDerivativeGain(gainScaling * 10.0);
         armDesiredQddControllers.get(robotSide).get(ArmJointName.ELBOW).setDerivativeGain(gainScaling * 10.0);
      }
   }
}

