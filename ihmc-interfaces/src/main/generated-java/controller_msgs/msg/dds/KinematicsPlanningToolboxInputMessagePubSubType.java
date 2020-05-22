package controller_msgs.msg.dds;

/**
* 
* Topic data type of the struct "KinematicsPlanningToolboxInputMessage" defined in "KinematicsPlanningToolboxInputMessage_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from KinematicsPlanningToolboxInputMessage_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit KinematicsPlanningToolboxInputMessage_.idl instead.
*
*/
public class KinematicsPlanningToolboxInputMessagePubSubType implements us.ihmc.pubsub.TopicDataType<controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage>
{
   public static final java.lang.String name = "controller_msgs::msg::dds_::KinematicsPlanningToolboxInputMessage_";

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data) throws java.io.IOException
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


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);for(int i0 = 0; i0 < 100; ++i0)
      {
          current_alignment += controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessagePubSubType.getMaxCdrSerializedSize(current_alignment);}

      current_alignment += controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessagePubSubType.getMaxCdrSerializedSize(current_alignment);


      current_alignment += controller_msgs.msg.dds.KinematicsToolboxConfigurationMessagePubSubType.getMaxCdrSerializedSize(current_alignment);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, int current_alignment)
   {
      int initial_alignment = current_alignment;


      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);



      current_alignment += 4 + us.ihmc.idl.CDR.alignment(current_alignment, 4);
      for(int i0 = 0; i0 < data.getRigidBodyMessages().size(); ++i0)
      {
          current_alignment += controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessagePubSubType.getCdrSerializedSize(data.getRigidBodyMessages().get(i0), current_alignment);}


      current_alignment += controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessagePubSubType.getCdrSerializedSize(data.getCenterOfMassMessage(), current_alignment);


      current_alignment += controller_msgs.msg.dds.KinematicsToolboxConfigurationMessagePubSubType.getCdrSerializedSize(data.getKinematicsConfigurationMessage(), current_alignment);


      return current_alignment - initial_alignment;
   }

   public static void write(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, us.ihmc.idl.CDR cdr)
   {

      cdr.write_type_4(data.getSequenceId());


      if(data.getRigidBodyMessages().size() <= 100)
      cdr.write_type_e(data.getRigidBodyMessages());else
          throw new RuntimeException("rigid_body_messages field exceeds the maximum length");


      controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessagePubSubType.write(data.getCenterOfMassMessage(), cdr);

      controller_msgs.msg.dds.KinematicsToolboxConfigurationMessagePubSubType.write(data.getKinematicsConfigurationMessage(), cdr);
   }

   public static void read(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, us.ihmc.idl.CDR cdr)
   {

      data.setSequenceId(cdr.read_type_4());
      	

      cdr.read_type_e(data.getRigidBodyMessages());	

      controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessagePubSubType.read(data.getCenterOfMassMessage(), cdr);	

      controller_msgs.msg.dds.KinematicsToolboxConfigurationMessagePubSubType.read(data.getKinematicsConfigurationMessage(), cdr);	

   }

   @Override
   public final void serialize(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, us.ihmc.idl.InterchangeSerializer ser)
   {

      ser.write_type_4("sequence_id", data.getSequenceId());

      ser.write_type_e("rigid_body_messages", data.getRigidBodyMessages());

      ser.write_type_a("center_of_mass_message", new controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessagePubSubType(), data.getCenterOfMassMessage());


      ser.write_type_a("kinematics_configuration_message", new controller_msgs.msg.dds.KinematicsToolboxConfigurationMessagePubSubType(), data.getKinematicsConfigurationMessage());

   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data)
   {

      data.setSequenceId(ser.read_type_4("sequence_id"));

      ser.read_type_e("rigid_body_messages", data.getRigidBodyMessages());

      ser.read_type_a("center_of_mass_message", new controller_msgs.msg.dds.KinematicsPlanningToolboxCenterOfMassMessagePubSubType(), data.getCenterOfMassMessage());


      ser.read_type_a("kinematics_configuration_message", new controller_msgs.msg.dds.KinematicsToolboxConfigurationMessagePubSubType(), data.getKinematicsConfigurationMessage());

   }

   public static void staticCopy(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage src, controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage dest)
   {
      dest.set(src);
   }

   @Override
   public controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage createData()
   {
      return new controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage();
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
   
   public void serialize(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage src, controller_msgs.msg.dds.KinematicsPlanningToolboxInputMessage dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public KinematicsPlanningToolboxInputMessagePubSubType newInstance()
   {
      return new KinematicsPlanningToolboxInputMessagePubSubType();
   }
}
