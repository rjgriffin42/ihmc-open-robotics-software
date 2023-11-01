package ihmc_common_msgs.msg.dds;

/**
* 
* Topic data type of the struct "CRDTNotificationMessage" defined in "CRDTNotificationMessage_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from CRDTNotificationMessage_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit CRDTNotificationMessage_.idl instead.
*
*/
public class CRDTNotificationMessagePubSubType implements us.ihmc.pubsub.TopicDataType<ihmc_common_msgs.msg.dds.CRDTNotificationMessage>
{
   public static final java.lang.String name = "ihmc_common_msgs::msg::dds_::CRDTNotificationMessage_";
   
   @Override
   public final java.lang.String getDefinitionChecksum()
   {
   		return "d0bfb6c2a2a2e8df13d508f55acf94ba56e3bf0624baefbe369b4bd2d10a4b16";
   }
   
   @Override
   public final java.lang.String getDefinitionVersion()
   {
   		return "local";
   }

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, ihmc_common_msgs.msg.dds.CRDTNotificationMessage data) throws java.io.IOException
   {
      deserializeCDR.deserialize(serializedPayload);
      read(data, deserializeCDR);
      deserializeCDR.finishDeserialize();
   }

   public static int getMaxCdrSerializedSize()
   {
      return getMaxCdrSerializedSize(0);
   }

   public static int getMaxCdrSerializedSize(int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 2 + us.ihmc.idl.CDR.alignment(current_alignment, 2);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += 2 + us.ihmc.idl.CDR.alignment(current_alignment, 2);



      return current_alignment - initial_alignment;
   }

   public static void write(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, us.ihmc.idl.CDR cdr)
   {
      cdr.write_type_3(data.getValue());

   }

   public static void read(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, us.ihmc.idl.CDR cdr)
   {
      data.setValue(cdr.read_type_3());
      	

   }

   @Override
   public final void serialize(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_3("value", data.getValue());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, ihmc_common_msgs.msg.dds.CRDTNotificationMessage data)
   {
      data.setValue(ser.read_type_3("value"));   }

   public static void staticCopy(ihmc_common_msgs.msg.dds.CRDTNotificationMessage src, ihmc_common_msgs.msg.dds.CRDTNotificationMessage dest)
   {
      dest.set(src);
   }

   @Override
   public ihmc_common_msgs.msg.dds.CRDTNotificationMessage createData()
   {
      return new ihmc_common_msgs.msg.dds.CRDTNotificationMessage();
   }
   @Override
   public int getTypeSize()
   {
      return us.ihmc.idl.CDR.getTypeSize(getMaxCdrSerializedSize());
   }

   @Override
   public java.lang.String getName()
   {
      return name;
   }
   
   public void serialize(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(ihmc_common_msgs.msg.dds.CRDTNotificationMessage data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(ihmc_common_msgs.msg.dds.CRDTNotificationMessage src, ihmc_common_msgs.msg.dds.CRDTNotificationMessage dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public CRDTNotificationMessagePubSubType newInstance()
   {
      return new CRDTNotificationMessagePubSubType();
   }
}
