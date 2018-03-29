package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

/**
 * Specifies a specific electric motor in the Atlas forearm to power on or off.
 */
public class AtlasElectricMotorEnablePacket extends Packet<AtlasElectricMotorEnablePacket>
      implements Settable<AtlasElectricMotorEnablePacket>, EpsilonComparable<AtlasElectricMotorEnablePacket>
{
   public static final byte L_ARM_WRY = (byte) 0;
   public static final byte L_ARM_WRX = (byte) 1;
   public static final byte L_ARM_WRY2 = (byte) 2;
   public static final byte R_ARM_WRY = (byte) 3;
   public static final byte R_ARM_WRX = (byte) 4;
   public static final byte R_ARM_WRY2 = (byte) 5;
   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header header_;
   /**
    * The Enum value of the motor to enable
    */
   public byte atlas_electric_motor_packet_enum_enable_ = (byte) 255;
   /**
    * Boolean for enable state; true for enable, false for disable.
    */
   public boolean enable_;

   public AtlasElectricMotorEnablePacket()
   {
      header_ = new std_msgs.msg.dds.Header();
   }

   public AtlasElectricMotorEnablePacket(AtlasElectricMotorEnablePacket other)
   {
      this();
      set(other);
   }

   public void set(AtlasElectricMotorEnablePacket other)
   {
      std_msgs.msg.dds.HeaderPubSubType.staticCopy(other.header_, header_);
      atlas_electric_motor_packet_enum_enable_ = other.atlas_electric_motor_packet_enum_enable_;

      enable_ = other.enable_;

   }

   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header getHeader()
   {
      return header_;
   }

   /**
    * The Enum value of the motor to enable
    */
   public void setAtlasElectricMotorPacketEnumEnable(byte atlas_electric_motor_packet_enum_enable)
   {
      atlas_electric_motor_packet_enum_enable_ = atlas_electric_motor_packet_enum_enable;
   }

   /**
    * The Enum value of the motor to enable
    */
   public byte getAtlasElectricMotorPacketEnumEnable()
   {
      return atlas_electric_motor_packet_enum_enable_;
   }

   /**
    * Boolean for enable state; true for enable, false for disable.
    */
   public void setEnable(boolean enable)
   {
      enable_ = enable;
   }

   /**
    * Boolean for enable state; true for enable, false for disable.
    */
   public boolean getEnable()
   {
      return enable_;
   }

   @Override
   public boolean epsilonEquals(AtlasElectricMotorEnablePacket other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!this.header_.epsilonEquals(other.header_, epsilon))
         return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.atlas_electric_motor_packet_enum_enable_, other.atlas_electric_motor_packet_enum_enable_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.enable_, other.enable_, epsilon))
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
      if (!(other instanceof AtlasElectricMotorEnablePacket))
         return false;

      AtlasElectricMotorEnablePacket otherMyClass = (AtlasElectricMotorEnablePacket) other;

      if (!this.header_.equals(otherMyClass.header_))
         return false;
      if (this.atlas_electric_motor_packet_enum_enable_ != otherMyClass.atlas_electric_motor_packet_enum_enable_)
         return false;

      if (this.enable_ != otherMyClass.enable_)
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("AtlasElectricMotorEnablePacket {");
      builder.append("header=");
      builder.append(this.header_);
      builder.append(", ");
      builder.append("atlas_electric_motor_packet_enum_enable=");
      builder.append(this.atlas_electric_motor_packet_enum_enable_);
      builder.append(", ");
      builder.append("enable=");
      builder.append(this.enable_);
      builder.append("}");
      return builder.toString();
   }
}
