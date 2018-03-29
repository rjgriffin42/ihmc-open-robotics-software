package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

public class PilotAlarmPacket extends Packet<PilotAlarmPacket> implements Settable<PilotAlarmPacket>, EpsilonComparable<PilotAlarmPacket>
{
   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header header_;
   public double beep_rate_;
   public boolean enable_tone_;

   public PilotAlarmPacket()
   {
      header_ = new std_msgs.msg.dds.Header();
   }

   public PilotAlarmPacket(PilotAlarmPacket other)
   {
      this();
      set(other);
   }

   public void set(PilotAlarmPacket other)
   {
      std_msgs.msg.dds.HeaderPubSubType.staticCopy(other.header_, header_);
      beep_rate_ = other.beep_rate_;

      enable_tone_ = other.enable_tone_;

   }

   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header getHeader()
   {
      return header_;
   }

   public void setBeepRate(double beep_rate)
   {
      beep_rate_ = beep_rate;
   }

   public double getBeepRate()
   {
      return beep_rate_;
   }

   public void setEnableTone(boolean enable_tone)
   {
      enable_tone_ = enable_tone;
   }

   public boolean getEnableTone()
   {
      return enable_tone_;
   }

   @Override
   public boolean epsilonEquals(PilotAlarmPacket other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!this.header_.epsilonEquals(other.header_, epsilon))
         return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.beep_rate_, other.beep_rate_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.enable_tone_, other.enable_tone_, epsilon))
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
      if (!(other instanceof PilotAlarmPacket))
         return false;

      PilotAlarmPacket otherMyClass = (PilotAlarmPacket) other;

      if (!this.header_.equals(otherMyClass.header_))
         return false;
      if (this.beep_rate_ != otherMyClass.beep_rate_)
         return false;

      if (this.enable_tone_ != otherMyClass.enable_tone_)
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("PilotAlarmPacket {");
      builder.append("header=");
      builder.append(this.header_);
      builder.append(", ");
      builder.append("beep_rate=");
      builder.append(this.beep_rate_);
      builder.append(", ");
      builder.append("enable_tone=");
      builder.append(this.enable_tone_);
      builder.append("}");
      return builder.toString();
   }
}
