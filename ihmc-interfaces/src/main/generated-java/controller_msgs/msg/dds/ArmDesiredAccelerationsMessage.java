package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

/**
 * This message is part of the IHMC whole-body controller API. This message gives the user the
 * option to bypass IHMC feedback controllers for the arm joints by sending desired arm joint
 * accelerations. One needs experience in control when activating the bypass as it can result in
 * unexpected behaviors for unreasonable accelerations. A message with a unique id equals to 0 will
 * be interpreted as invalid and will not be processed by the controller.
 */
public class ArmDesiredAccelerationsMessage extends Packet<ArmDesiredAccelerationsMessage>
      implements Settable<ArmDesiredAccelerationsMessage>, EpsilonComparable<ArmDesiredAccelerationsMessage>
{
   public static final byte ROBOT_SIDE_LEFT = (byte) 0;
   public static final byte ROBOT_SIDE_RIGHT = (byte) 1;
   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header header_;
   /**
    * Specifies the side of the robot that will execute the command.
    */
   public byte robot_side_ = (byte) 255;
   /**
    * The desired joint acceleration information.
    */
   public controller_msgs.msg.dds.DesiredAccelerationsMessage desired_accelerations_;

   public ArmDesiredAccelerationsMessage()
   {
      header_ = new std_msgs.msg.dds.Header();
      desired_accelerations_ = new controller_msgs.msg.dds.DesiredAccelerationsMessage();
   }

   public ArmDesiredAccelerationsMessage(ArmDesiredAccelerationsMessage other)
   {
      this();
      set(other);
   }

   public void set(ArmDesiredAccelerationsMessage other)
   {
      std_msgs.msg.dds.HeaderPubSubType.staticCopy(other.header_, header_);
      robot_side_ = other.robot_side_;

      controller_msgs.msg.dds.DesiredAccelerationsMessagePubSubType.staticCopy(other.desired_accelerations_, desired_accelerations_);
   }

   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header getHeader()
   {
      return header_;
   }

   /**
    * Specifies the side of the robot that will execute the command.
    */
   public void setRobotSide(byte robot_side)
   {
      robot_side_ = robot_side;
   }

   /**
    * Specifies the side of the robot that will execute the command.
    */
   public byte getRobotSide()
   {
      return robot_side_;
   }

   /**
    * The desired joint acceleration information.
    */
   public controller_msgs.msg.dds.DesiredAccelerationsMessage getDesiredAccelerations()
   {
      return desired_accelerations_;
   }

   @Override
   public boolean epsilonEquals(ArmDesiredAccelerationsMessage other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!this.header_.epsilonEquals(other.header_, epsilon))
         return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.robot_side_, other.robot_side_, epsilon))
         return false;

      if (!this.desired_accelerations_.epsilonEquals(other.desired_accelerations_, epsilon))
         return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;
      if (!(other instanceof ArmDesiredAccelerationsMessage))
         return false;

      ArmDesiredAccelerationsMessage otherMyClass = (ArmDesiredAccelerationsMessage) other;

      if (!this.header_.equals(otherMyClass.header_))
         return false;
      if (this.robot_side_ != otherMyClass.robot_side_)
         return false;

      if (!this.desired_accelerations_.equals(otherMyClass.desired_accelerations_))
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("ArmDesiredAccelerationsMessage {");
      builder.append("header=");
      builder.append(this.header_);
      builder.append(", ");
      builder.append("robot_side=");
      builder.append(this.robot_side_);
      builder.append(", ");
      builder.append("desired_accelerations=");
      builder.append(this.desired_accelerations_);
      builder.append("}");
      return builder.toString();
   }
}
