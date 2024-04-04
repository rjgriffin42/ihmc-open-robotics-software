package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * Message for commanding the Sake hands to perform various predefined grasps.
       * Also allows for custom grasps with set positions/torques
       */
public class SakeHandDesiredCommandMessage extends Packet<SakeHandDesiredCommandMessage> implements Settable<SakeHandDesiredCommandMessage>, EpsilonComparable<SakeHandDesiredCommandMessage>
{
   public static final byte ROBOT_SIDE_LEFT = (byte) 0;
   public static final byte ROBOT_SIDE_RIGHT = (byte) 1;
   /**
            * Specifies the side of the robot of the hand being referred to
            */
   public byte robot_side_ = (byte) 255;
   /**
            * Request the gripper to perform a calibration sequence
            */
   public boolean request_calibration_;
   /**
            * Request to reset the gripper error state after overheating
            */
   public boolean request_reset_errors_;
   /**
            * The desired dynamixel position, normalized to the gripper range of motion
            * 0.0 (fingers touching) -> 1.0 (open 210 degrees between fingers)
            * -1.0 means "unspecified". Gripper will keep current value
            */
   public double normalized_gripper_desired_position_;
   /**
            * The dynamixel torque limit setting in achieving the desired position,
            * normalized to the peak dynamixel torque.
            * 0.0: dynamixel will not apply any force and will not achieve desired position
            * 0.3: A reasonable normal value
            * 1.0: dynamixel max torque which will quickly overheat the motor
            * -1.0 means "unspecified". Gripper will keep current value
            */
   public double normalized_gripper_torque_limit_;
   /**
            * Keeping the torque off when not needed can help keep the hand's temperature down
            */
   public boolean torque_on_;

   public SakeHandDesiredCommandMessage()
   {
   }

   public SakeHandDesiredCommandMessage(SakeHandDesiredCommandMessage other)
   {
      this();
      set(other);
   }

   public void set(SakeHandDesiredCommandMessage other)
   {
      robot_side_ = other.robot_side_;

      request_calibration_ = other.request_calibration_;

      request_reset_errors_ = other.request_reset_errors_;

      normalized_gripper_desired_position_ = other.normalized_gripper_desired_position_;

      normalized_gripper_torque_limit_ = other.normalized_gripper_torque_limit_;

      torque_on_ = other.torque_on_;

   }

   /**
            * Specifies the side of the robot of the hand being referred to
            */
   public void setRobotSide(byte robot_side)
   {
      robot_side_ = robot_side;
   }
   /**
            * Specifies the side of the robot of the hand being referred to
            */
   public byte getRobotSide()
   {
      return robot_side_;
   }

   /**
            * Request the gripper to perform a calibration sequence
            */
   public void setRequestCalibration(boolean request_calibration)
   {
      request_calibration_ = request_calibration;
   }
   /**
            * Request the gripper to perform a calibration sequence
            */
   public boolean getRequestCalibration()
   {
      return request_calibration_;
   }

   /**
            * Request to reset the gripper error state after overheating
            */
   public void setRequestResetErrors(boolean request_reset_errors)
   {
      request_reset_errors_ = request_reset_errors;
   }
   /**
            * Request to reset the gripper error state after overheating
            */
   public boolean getRequestResetErrors()
   {
      return request_reset_errors_;
   }

   /**
            * The desired dynamixel position, normalized to the gripper range of motion
            * 0.0 (fingers touching) -> 1.0 (open 210 degrees between fingers)
            * -1.0 means "unspecified". Gripper will keep current value
            */
   public void setNormalizedGripperDesiredPosition(double normalized_gripper_desired_position)
   {
      normalized_gripper_desired_position_ = normalized_gripper_desired_position;
   }
   /**
            * The desired dynamixel position, normalized to the gripper range of motion
            * 0.0 (fingers touching) -> 1.0 (open 210 degrees between fingers)
            * -1.0 means "unspecified". Gripper will keep current value
            */
   public double getNormalizedGripperDesiredPosition()
   {
      return normalized_gripper_desired_position_;
   }

   /**
            * The dynamixel torque limit setting in achieving the desired position,
            * normalized to the peak dynamixel torque.
            * 0.0: dynamixel will not apply any force and will not achieve desired position
            * 0.3: A reasonable normal value
            * 1.0: dynamixel max torque which will quickly overheat the motor
            * -1.0 means "unspecified". Gripper will keep current value
            */
   public void setNormalizedGripperTorqueLimit(double normalized_gripper_torque_limit)
   {
      normalized_gripper_torque_limit_ = normalized_gripper_torque_limit;
   }
   /**
            * The dynamixel torque limit setting in achieving the desired position,
            * normalized to the peak dynamixel torque.
            * 0.0: dynamixel will not apply any force and will not achieve desired position
            * 0.3: A reasonable normal value
            * 1.0: dynamixel max torque which will quickly overheat the motor
            * -1.0 means "unspecified". Gripper will keep current value
            */
   public double getNormalizedGripperTorqueLimit()
   {
      return normalized_gripper_torque_limit_;
   }

   /**
            * Keeping the torque off when not needed can help keep the hand's temperature down
            */
   public void setTorqueOn(boolean torque_on)
   {
      torque_on_ = torque_on;
   }
   /**
            * Keeping the torque off when not needed can help keep the hand's temperature down
            */
   public boolean getTorqueOn()
   {
      return torque_on_;
   }


   public static Supplier<SakeHandDesiredCommandMessagePubSubType> getPubSubType()
   {
      return SakeHandDesiredCommandMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return SakeHandDesiredCommandMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(SakeHandDesiredCommandMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.robot_side_, other.robot_side_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.request_calibration_, other.request_calibration_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.request_reset_errors_, other.request_reset_errors_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.normalized_gripper_desired_position_, other.normalized_gripper_desired_position_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.normalized_gripper_torque_limit_, other.normalized_gripper_torque_limit_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.torque_on_, other.torque_on_, epsilon)) return false;


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof SakeHandDesiredCommandMessage)) return false;

      SakeHandDesiredCommandMessage otherMyClass = (SakeHandDesiredCommandMessage) other;

      if(this.robot_side_ != otherMyClass.robot_side_) return false;

      if(this.request_calibration_ != otherMyClass.request_calibration_) return false;

      if(this.request_reset_errors_ != otherMyClass.request_reset_errors_) return false;

      if(this.normalized_gripper_desired_position_ != otherMyClass.normalized_gripper_desired_position_) return false;

      if(this.normalized_gripper_torque_limit_ != otherMyClass.normalized_gripper_torque_limit_) return false;

      if(this.torque_on_ != otherMyClass.torque_on_) return false;


      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("SakeHandDesiredCommandMessage {");
      builder.append("robot_side=");
      builder.append(this.robot_side_);      builder.append(", ");
      builder.append("request_calibration=");
      builder.append(this.request_calibration_);      builder.append(", ");
      builder.append("request_reset_errors=");
      builder.append(this.request_reset_errors_);      builder.append(", ");
      builder.append("normalized_gripper_desired_position=");
      builder.append(this.normalized_gripper_desired_position_);      builder.append(", ");
      builder.append("normalized_gripper_torque_limit=");
      builder.append(this.normalized_gripper_torque_limit_);      builder.append(", ");
      builder.append("torque_on=");
      builder.append(this.torque_on_);
      builder.append("}");
      return builder.toString();
   }
}
