package behavior_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

public class KickDoorActionStateMessage extends Packet<KickDoorActionStateMessage> implements Settable<KickDoorActionStateMessage>, EpsilonComparable<KickDoorActionStateMessage>
{
   /**
            * Parent state fields
            */
   public behavior_msgs.msg.dds.ActionNodeStateMessage state_;
   /**
            * Definition
            */
   public behavior_msgs.msg.dds.KickDoorActionDefinitionMessage definition_;
   /**
            * Execution state
            */
   public byte execution_state_;

   public KickDoorActionStateMessage()
   {
      state_ = new behavior_msgs.msg.dds.ActionNodeStateMessage();
      definition_ = new behavior_msgs.msg.dds.KickDoorActionDefinitionMessage();
   }

   public KickDoorActionStateMessage(KickDoorActionStateMessage other)
   {
      this();
      set(other);
   }

   public void set(KickDoorActionStateMessage other)
   {
      behavior_msgs.msg.dds.ActionNodeStateMessagePubSubType.staticCopy(other.state_, state_);
      behavior_msgs.msg.dds.KickDoorActionDefinitionMessagePubSubType.staticCopy(other.definition_, definition_);
      execution_state_ = other.execution_state_;

   }


   /**
            * Parent state fields
            */
   public behavior_msgs.msg.dds.ActionNodeStateMessage getState()
   {
      return state_;
   }


   /**
            * Definition
            */
   public behavior_msgs.msg.dds.KickDoorActionDefinitionMessage getDefinition()
   {
      return definition_;
   }

   /**
            * Execution state
            */
   public void setExecutionState(byte execution_state)
   {
      execution_state_ = execution_state;
   }
   /**
            * Execution state
            */
   public byte getExecutionState()
   {
      return execution_state_;
   }


   public static Supplier<KickDoorActionStateMessagePubSubType> getPubSubType()
   {
      return KickDoorActionStateMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return KickDoorActionStateMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(KickDoorActionStateMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!this.state_.epsilonEquals(other.state_, epsilon)) return false;
      if (!this.definition_.epsilonEquals(other.definition_, epsilon)) return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.execution_state_, other.execution_state_, epsilon)) return false;


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof KickDoorActionStateMessage)) return false;

      KickDoorActionStateMessage otherMyClass = (KickDoorActionStateMessage) other;

      if (!this.state_.equals(otherMyClass.state_)) return false;
      if (!this.definition_.equals(otherMyClass.definition_)) return false;
      if(this.execution_state_ != otherMyClass.execution_state_) return false;


      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("KickDoorActionStateMessage {");
      builder.append("state=");
      builder.append(this.state_);      builder.append(", ");
      builder.append("definition=");
      builder.append(this.definition_);      builder.append(", ");
      builder.append("execution_state=");
      builder.append(this.execution_state_);
      builder.append("}");
      return builder.toString();
   }
}
