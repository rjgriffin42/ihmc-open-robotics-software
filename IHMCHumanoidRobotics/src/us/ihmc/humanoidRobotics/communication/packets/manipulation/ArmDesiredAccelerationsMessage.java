package us.ihmc.humanoidRobotics.communication.packets.manipulation;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;
import us.ihmc.communication.packetAnnotations.FieldDocumentation;
import us.ihmc.communication.packets.IHMCRosApiMessage;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.ArrayTools;
import us.ihmc.tools.DocumentedEnum;

@ClassDocumentation("This message gives the user the option to bypass IHMC feedback controllers for the arm joints by sending desired arm joint accelerations."
      + " One needs experience in control when activating the bypass as it can result in unexpected behaviors for unreasonable accelerations."
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller.")
public class ArmDesiredAccelerationsMessage extends IHMCRosApiMessage<ArmDesiredAccelerationsMessage>
{
   public enum ArmControlMode implements DocumentedEnum<ArmControlMode>
   {
      IHMC_CONTROL_MODE, USER_CONTROL_MODE;

      @Override
      public String getDocumentation(ArmControlMode var)
      {
         switch (var)
         {
         case IHMC_CONTROL_MODE:
            return "The IHMC controller controls the arm joints to execute desired inputs given from ArmTrajectoryMessage, HandTrajectoryMessage, and WholeBodyTrajectoryMessage."
                  + " PD controllers are run for the given inputs and will either compute the desired hand spatial acceleration or arm joint desired accelerations."
                  + "The desired joint torques to achieve there desired accelerations are computed by the IHMC QP solver & inverse dynamics calculator.";
         case USER_CONTROL_MODE:
            return "The user directly sets what the arm joint desired accelerations have to be."
                  + " The IHMC controller will stop tracking positions and the user desired accelerations will be fed to the IHMC QP solver & inverse dynamics to compute the desired joint torques.";

         default:
            return "No documentation available.";
         }
      }

      @Override
      public ArmControlMode[] getDocumentedValues()
      {
         return values();
      }
   }


   @FieldDocumentation("Specifies the side of the robot that will execute the trajectory.")
   public RobotSide robotSide;
   @FieldDocumentation("Specifies the control mode for controlling the arm joints. See ArmControlMode.")
   public ArmControlMode armControlMode;
   @FieldDocumentation("Specifies the joint desired accelerations. Only necessary when armControlMode == USER_CONTROL_MODE.")
   public double[] armJointDesiredAccelerations;

   public ArmDesiredAccelerationsMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public ArmDesiredAccelerationsMessage(RobotSide robotSide, ArmControlMode armControlMode, double[] armJointDesiredAccelerations)
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
      this.robotSide = robotSide;
      this.armControlMode = armControlMode;
      this.armJointDesiredAccelerations = armJointDesiredAccelerations;
   }

   public int getNumberOfJoints()
   {
      return armJointDesiredAccelerations.length;
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public ArmControlMode getArmControlMode()
   {
      return armControlMode;
   }

   public double[] getArmJointDesiredAccelerations()
   {
      return armJointDesiredAccelerations;
   }

   @Override
   public boolean epsilonEquals(ArmDesiredAccelerationsMessage other, double epsilon)
   {
      if (robotSide != other.robotSide)
         return false;
      if (armControlMode != other.armControlMode)
         return false;
      if (ArrayTools.deltaEquals(armJointDesiredAccelerations, other.armJointDesiredAccelerations, epsilon))
         return false;
      return true;
   }
}
