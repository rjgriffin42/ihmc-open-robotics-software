package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * This message is part of the IHMC whole-body controller API.
       * This message gives the status of the footstep queue in the controller in world coordinates.
       * See QueuedFootstepStatusMessage for more information about defining a footstep.
       */
public class FootstepQueueStatusMessage extends Packet<FootstepQueueStatusMessage> implements Settable<FootstepQueueStatusMessage>, EpsilonComparable<FootstepQueueStatusMessage>
{
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   /**
            * Defines the list of footsteps contained in the queue.
            */
   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.QueuedFootstepStatusMessage>  footstep_data_list_;

   public FootstepQueueStatusMessage()
   {
      footstep_data_list_ = new us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.QueuedFootstepStatusMessage> (50, new controller_msgs.msg.dds.QueuedFootstepStatusMessagePubSubType());

   }

   public FootstepQueueStatusMessage(FootstepQueueStatusMessage other)
   {
      this();
      set(other);
   }

   public void set(FootstepQueueStatusMessage other)
   {
      sequence_id_ = other.sequence_id_;

      footstep_data_list_.set(other.footstep_data_list_);
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
            * Defines the list of footsteps contained in the queue.
            */
   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.QueuedFootstepStatusMessage>  getFootstepDataList()
   {
      return footstep_data_list_;
   }


   public static Supplier<FootstepQueueStatusMessagePubSubType> getPubSubType()
   {
      return FootstepQueueStatusMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return FootstepQueueStatusMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(FootstepQueueStatusMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (this.footstep_data_list_.size() != other.footstep_data_list_.size()) { return false; }
      else
      {
         for (int i = 0; i < this.footstep_data_list_.size(); i++)
         {  if (!this.footstep_data_list_.get(i).epsilonEquals(other.footstep_data_list_.get(i), epsilon)) return false; }
      }


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof FootstepQueueStatusMessage)) return false;

      FootstepQueueStatusMessage otherMyClass = (FootstepQueueStatusMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if (!this.footstep_data_list_.equals(otherMyClass.footstep_data_list_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("FootstepQueueStatusMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("footstep_data_list=");
      builder.append(this.footstep_data_list_);
      builder.append("}");
      return builder.toString();
   }
}
