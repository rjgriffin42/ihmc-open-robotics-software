package behavior_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

public class DoorTraversalStateMessage extends Packet<DoorTraversalStateMessage> implements Settable<DoorTraversalStateMessage>, EpsilonComparable<DoorTraversalStateMessage>
{
   /**
            * Parent state fields
            */
   public behavior_msgs.msg.dds.BehaviorTreeNodeStateMessage state_;
   /**
            * Definition
            */
   public behavior_msgs.msg.dds.DoorTraversalDefinitionMessage definition_;

   public DoorTraversalStateMessage()
   {
      state_ = new behavior_msgs.msg.dds.BehaviorTreeNodeStateMessage();
      definition_ = new behavior_msgs.msg.dds.DoorTraversalDefinitionMessage();
   }

   public DoorTraversalStateMessage(DoorTraversalStateMessage other)
   {
      this();
      set(other);
   }

   public void set(DoorTraversalStateMessage other)
   {
      behavior_msgs.msg.dds.BehaviorTreeNodeStateMessagePubSubType.staticCopy(other.state_, state_);
      behavior_msgs.msg.dds.DoorTraversalDefinitionMessagePubSubType.staticCopy(other.definition_, definition_);
   }


   /**
            * Parent state fields
            */
   public behavior_msgs.msg.dds.BehaviorTreeNodeStateMessage getState()
   {
      return state_;
   }


   /**
            * Definition
            */
   public behavior_msgs.msg.dds.DoorTraversalDefinitionMessage getDefinition()
   {
      return definition_;
   }


   public static Supplier<DoorTraversalStateMessagePubSubType> getPubSubType()
   {
      return DoorTraversalStateMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return DoorTraversalStateMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(DoorTraversalStateMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!this.state_.epsilonEquals(other.state_, epsilon)) return false;
      if (!this.definition_.epsilonEquals(other.definition_, epsilon)) return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof DoorTraversalStateMessage)) return false;

      DoorTraversalStateMessage otherMyClass = (DoorTraversalStateMessage) other;

      if (!this.state_.equals(otherMyClass.state_)) return false;
      if (!this.definition_.equals(otherMyClass.definition_)) return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("DoorTraversalStateMessage {");
      builder.append("state=");
      builder.append(this.state_);      builder.append(", ");
      builder.append("definition=");
      builder.append(this.definition_);
      builder.append("}");
      return builder.toString();
   }
}
