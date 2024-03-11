package behavior_msgs.msg.dds;

/**
* 
* Topic data type of the struct "ActionNodeDefinitionMessage" defined in "ActionNodeDefinitionMessage_.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from ActionNodeDefinitionMessage_.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit ActionNodeDefinitionMessage_.idl instead.
*
*/
public class ActionNodeDefinitionMessagePubSubType implements us.ihmc.pubsub.TopicDataType<behavior_msgs.msg.dds.ActionNodeDefinitionMessage>
{
   public static final java.lang.String name = "behavior_msgs::msg::dds_::ActionNodeDefinitionMessage_";
   
   @Override
   public final java.lang.String getDefinitionChecksum()
   {
   		return "571bdb005672af90578d395fbbf62c34230534a1d0bfcd3efd4fd0318b8f02ed";
   }
   
   @Override
   public final java.lang.String getDefinitionVersion()
   {
   		return "local";
   }

   private final us.ihmc.idl.CDR serializeCDR = new us.ihmc.idl.CDR();
   private final us.ihmc.idl.CDR deserializeCDR = new us.ihmc.idl.CDR();

   @Override
   public void serialize(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, us.ihmc.pubsub.common.SerializedPayload serializedPayload) throws java.io.IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(us.ihmc.pubsub.common.SerializedPayload serializedPayload, behavior_msgs.msg.dds.ActionNodeDefinitionMessage data) throws java.io.IOException
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

      current_alignment += behavior_msgs.msg.dds.BehaviorTreeNodeDefinitionMessagePubSubType.getMaxCdrSerializedSize(current_alignment);

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);

      current_alignment += 2 + us.ihmc.idl.CDR.alignment(current_alignment, 2);


      return current_alignment - initial_alignment;
   }

   public final static int getCdrSerializedSize(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data)
   {
      return getCdrSerializedSize(data, 0);
   }

   public final static int getCdrSerializedSize(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, int current_alignment)
   {
      int initial_alignment = current_alignment;

      current_alignment += behavior_msgs.msg.dds.BehaviorTreeNodeDefinitionMessagePubSubType.getCdrSerializedSize(data.getDefinition(), current_alignment);

      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 1 + us.ihmc.idl.CDR.alignment(current_alignment, 1);


      current_alignment += 2 + us.ihmc.idl.CDR.alignment(current_alignment, 2);



      return current_alignment - initial_alignment;
   }

   public static void write(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, us.ihmc.idl.CDR cdr)
   {
      behavior_msgs.msg.dds.BehaviorTreeNodeDefinitionMessagePubSubType.write(data.getDefinition(), cdr);
      cdr.write_type_7(data.getExecuteAfterPrevious());

      cdr.write_type_7(data.getExecuteAfterBeginning());

      cdr.write_type_3(data.getExecuteAfterNodeId());

   }

   public static void read(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, us.ihmc.idl.CDR cdr)
   {
      behavior_msgs.msg.dds.BehaviorTreeNodeDefinitionMessagePubSubType.read(data.getDefinition(), cdr);	
      data.setExecuteAfterPrevious(cdr.read_type_7());
      	
      data.setExecuteAfterBeginning(cdr.read_type_7());
      	
      data.setExecuteAfterNodeId(cdr.read_type_3());
      	

   }

   @Override
   public final void serialize(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, us.ihmc.idl.InterchangeSerializer ser)
   {
      ser.write_type_a("definition", new behavior_msgs.msg.dds.BehaviorTreeNodeDefinitionMessagePubSubType(), data.getDefinition());

      ser.write_type_7("execute_after_previous", data.getExecuteAfterPrevious());
      ser.write_type_7("execute_after_beginning", data.getExecuteAfterBeginning());
      ser.write_type_3("execute_after_node_id", data.getExecuteAfterNodeId());
   }

   @Override
   public final void deserialize(us.ihmc.idl.InterchangeSerializer ser, behavior_msgs.msg.dds.ActionNodeDefinitionMessage data)
   {
      ser.read_type_a("definition", new behavior_msgs.msg.dds.BehaviorTreeNodeDefinitionMessagePubSubType(), data.getDefinition());

      data.setExecuteAfterPrevious(ser.read_type_7("execute_after_previous"));
      data.setExecuteAfterBeginning(ser.read_type_7("execute_after_beginning"));
      data.setExecuteAfterNodeId(ser.read_type_3("execute_after_node_id"));
   }

   public static void staticCopy(behavior_msgs.msg.dds.ActionNodeDefinitionMessage src, behavior_msgs.msg.dds.ActionNodeDefinitionMessage dest)
   {
      dest.set(src);
   }

   @Override
   public behavior_msgs.msg.dds.ActionNodeDefinitionMessage createData()
   {
      return new behavior_msgs.msg.dds.ActionNodeDefinitionMessage();
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
   
   public void serialize(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, us.ihmc.idl.CDR cdr)
   {
      write(data, cdr);
   }

   public void deserialize(behavior_msgs.msg.dds.ActionNodeDefinitionMessage data, us.ihmc.idl.CDR cdr)
   {
      read(data, cdr);
   }
   
   public void copy(behavior_msgs.msg.dds.ActionNodeDefinitionMessage src, behavior_msgs.msg.dds.ActionNodeDefinitionMessage dest)
   {
      staticCopy(src, dest);
   }

   @Override
   public ActionNodeDefinitionMessagePubSubType newInstance()
   {
      return new ActionNodeDefinitionMessagePubSubType();
   }
}
