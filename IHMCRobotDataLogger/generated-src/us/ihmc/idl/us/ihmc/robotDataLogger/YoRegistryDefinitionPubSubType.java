package us.ihmc.idl.us.ihmc.robotDataLogger;

import java.io.IOException;

import us.ihmc.pubsub.TopicDataType;
import us.ihmc.pubsub.common.SerializedPayload;
import us.ihmc.idl.CDR;

/**
* 
* Topic data type of the struct "YoRegistryDefinition" defined in "Handshake.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from Handshake.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit Handshake.idl instead.
*
*/
public class YoRegistryDefinitionPubSubType implements TopicDataType<YoRegistryDefinition>
{
	public static final String name = "us::ihmc::robotDataLogger::YoRegistryDefinition";
	private final CDR serializeCDR = new CDR();
	private final CDR deserializeCDR = new CDR();
	
	
	
    public YoRegistryDefinitionPubSubType()
    {
        
    }
    
       @Override
   public void serialize(YoRegistryDefinition data, SerializedPayload serializedPayload) throws IOException
   {
      serializeCDR.serialize(serializedPayload);
      data.serialize(serializeCDR);
      serializeCDR.finishSerialize();
   }

   @Override
   public void deserialize(SerializedPayload serializedPayload, YoRegistryDefinition data) throws IOException
   {
      deserializeCDR.deserialize(serializedPayload);
      data.deserialize(deserializeCDR);
      deserializeCDR.finishDeserialize();
   }

   @Override
   public int getTypeSize()
   {
      return YoRegistryDefinition.getMaxCdrSerializedSize();
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public YoRegistryDefinition createData()
   {
      return new YoRegistryDefinition();
   }
}