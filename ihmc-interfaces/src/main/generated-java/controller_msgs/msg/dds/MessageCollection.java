package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

/**
 * This message is part of the IHMC whole-body controller API. This message is used mainly with the
 * IHMC whole-body controller. When the execution of a collection of messages is to be synchronized,
 * these messages should be attributed a unique sequence ID. Then by sending beforehand a
 * MessageCollection holding onto the sequence IDs of all these messages, the controller will wait
 * to receive all the messages before processing them.
 */
public class MessageCollection extends Packet<MessageCollection> implements Settable<MessageCollection>, EpsilonComparable<MessageCollection>
{
   /**
    * The sequence IDs of all the messages that are expected to be received.
    */
   public us.ihmc.idl.IDLSequence.Long sequences_;

   public MessageCollection()
   {
      sequences_ = new us.ihmc.idl.IDLSequence.Long(100, "type_4");

   }

   public MessageCollection(MessageCollection other)
   {
      set(other);
   }

   public void set(MessageCollection other)
   {
      sequences_.set(other.sequences_);
   }

   /**
    * The sequence IDs of all the messages that are expected to be received.
    */
   public us.ihmc.idl.IDLSequence.Long getSequences()
   {
      return sequences_;
   }

   @Override
   public boolean epsilonEquals(MessageCollection other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsLongSequence(this.sequences_, other.sequences_, epsilon))
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
      if (!(other instanceof MessageCollection))
         return false;

      MessageCollection otherMyClass = (MessageCollection) other;

      if (!this.sequences_.equals(otherMyClass.sequences_))
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("MessageCollection {");
      builder.append("sequences=");
      builder.append(this.sequences_);
      builder.append("}");
      return builder.toString();
   }
}
