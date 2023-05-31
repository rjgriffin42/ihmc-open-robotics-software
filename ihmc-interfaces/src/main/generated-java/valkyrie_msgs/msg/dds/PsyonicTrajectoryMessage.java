package valkyrie_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

public class PsyonicTrajectoryMessage extends Packet<PsyonicTrajectoryMessage> implements Settable<PsyonicTrajectoryMessage>, EpsilonComparable<PsyonicTrajectoryMessage>
{
   /**
          * This message is Valkyrie specific and is to be used with a Psyonic hand.
          * This message commands the finger controller to move the robot fingers.
          */
   public static final byte ROBOT_SIDE_LEFT = (byte) 0;
   public static final byte ROBOT_SIDE_RIGHT = (byte) 1;
   public static final byte THUMB_Q1 = (byte) 0;
   public static final byte THUMB_Q2 = (byte) 1;
   public static final byte INDEX_Q1 = (byte) 2;
   public static final byte INDEX_Q2 = (byte) 3;
   public static final byte MIDDLE_Q1 = (byte) 4;
   public static final byte MIDDLE_Q2 = (byte) 5;
   public static final byte RING_Q1 = (byte) 6;
   public static final byte RING_Q2 = (byte) 7;
   public static final byte PINKY_Q1 = (byte) 8;
   public static final byte PINKY_Q2 = (byte) 9;
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   /**
            * Specifies the side of the robot that will execute the trajectory.
            */
   public byte robot_side_ = (byte) 255;
   /**
            * Specifies the list of joints that we want to control.
            */
   public us.ihmc.idl.IDLSequence.Byte  joint_names_;
   /**
            * The indexing for the joints refers the list of joint names.
            */
   public controller_msgs.msg.dds.JointspaceTrajectoryMessage jointspace_trajectory_;

   public PsyonicTrajectoryMessage()
   {
      joint_names_ = new us.ihmc.idl.IDLSequence.Byte (10, "type_9");

      jointspace_trajectory_ = new controller_msgs.msg.dds.JointspaceTrajectoryMessage();
   }

   public PsyonicTrajectoryMessage(PsyonicTrajectoryMessage other)
   {
      this();
      set(other);
   }

   public void set(PsyonicTrajectoryMessage other)
   {
      sequence_id_ = other.sequence_id_;

      robot_side_ = other.robot_side_;

      joint_names_.set(other.joint_names_);
      controller_msgs.msg.dds.JointspaceTrajectoryMessagePubSubType.staticCopy(other.jointspace_trajectory_, jointspace_trajectory_);
   }

   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }

   /**
            * Specifies the side of the robot that will execute the trajectory.
            */
   public void setRobotSide(byte robot_side)
   {
      robot_side_ = robot_side;
   }
   /**
            * Specifies the side of the robot that will execute the trajectory.
            */
   public byte getRobotSide()
   {
      return robot_side_;
   }


   /**
            * Specifies the list of joints that we want to control.
            */
   public us.ihmc.idl.IDLSequence.Byte  getJointNames()
   {
      return joint_names_;
   }


   /**
            * The indexing for the joints refers the list of joint names.
            */
   public controller_msgs.msg.dds.JointspaceTrajectoryMessage getJointspaceTrajectory()
   {
      return jointspace_trajectory_;
   }


   public static Supplier<PsyonicTrajectoryMessagePubSubType> getPubSubType()
   {
      return PsyonicTrajectoryMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return PsyonicTrajectoryMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(PsyonicTrajectoryMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.robot_side_, other.robot_side_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsByteSequence(this.joint_names_, other.joint_names_, epsilon)) return false;

      if (!this.jointspace_trajectory_.epsilonEquals(other.jointspace_trajectory_, epsilon)) return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof PsyonicTrajectoryMessage)) return false;

      PsyonicTrajectoryMessage otherMyClass = (PsyonicTrajectoryMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if(this.robot_side_ != otherMyClass.robot_side_) return false;

      if (!this.joint_names_.equals(otherMyClass.joint_names_)) return false;
      if (!this.jointspace_trajectory_.equals(otherMyClass.jointspace_trajectory_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("PsyonicTrajectoryMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("robot_side=");
      builder.append(this.robot_side_);      builder.append(", ");
      builder.append("joint_names=");
      builder.append(this.joint_names_);      builder.append(", ");
      builder.append("jointspace_trajectory=");
      builder.append(this.jointspace_trajectory_);
      builder.append("}");
      return builder.toString();
   }
}
