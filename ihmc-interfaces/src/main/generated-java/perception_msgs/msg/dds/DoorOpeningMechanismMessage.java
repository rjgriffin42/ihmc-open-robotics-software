package perception_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

public class DoorOpeningMechanismMessage extends Packet<DoorOpeningMechanismMessage> implements Settable<DoorOpeningMechanismMessage>, EpsilonComparable<DoorOpeningMechanismMessage>
{
   public byte type_;
   public us.ihmc.euclid.geometry.Pose3D grasp_pose_;

   public DoorOpeningMechanismMessage()
   {
      grasp_pose_ = new us.ihmc.euclid.geometry.Pose3D();
   }

   public DoorOpeningMechanismMessage(DoorOpeningMechanismMessage other)
   {
      this();
      set(other);
   }

   public void set(DoorOpeningMechanismMessage other)
   {
      type_ = other.type_;

      geometry_msgs.msg.dds.PosePubSubType.staticCopy(other.grasp_pose_, grasp_pose_);
   }

   public void setType(byte type)
   {
      type_ = type;
   }
   public byte getType()
   {
      return type_;
   }


   public us.ihmc.euclid.geometry.Pose3D getGraspPose()
   {
      return grasp_pose_;
   }


   public static Supplier<DoorOpeningMechanismMessagePubSubType> getPubSubType()
   {
      return DoorOpeningMechanismMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return DoorOpeningMechanismMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(DoorOpeningMechanismMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.type_, other.type_, epsilon)) return false;

      if (!this.grasp_pose_.epsilonEquals(other.grasp_pose_, epsilon)) return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof DoorOpeningMechanismMessage)) return false;

      DoorOpeningMechanismMessage otherMyClass = (DoorOpeningMechanismMessage) other;

      if(this.type_ != otherMyClass.type_) return false;

      if (!this.grasp_pose_.equals(otherMyClass.grasp_pose_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("DoorOpeningMechanismMessage {");
      builder.append("type=");
      builder.append(this.type_);      builder.append(", ");
      builder.append("grasp_pose=");
      builder.append(this.grasp_pose_);
      builder.append("}");
      return builder.toString();
   }
}
