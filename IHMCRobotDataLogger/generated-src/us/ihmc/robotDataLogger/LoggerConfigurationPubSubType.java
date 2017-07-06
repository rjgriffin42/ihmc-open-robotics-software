package us.ihmc.robotDataLogger;

import java.io.IOException;

import us.ihmc.pubsub.TopicDataType;
import us.ihmc.pubsub.common.SerializedPayload;
import us.ihmc.idl.InterchangeSerializer;
import us.ihmc.idl.CDR;
import us.ihmc.idl.IDLSequence;

/**
* 
* Topic data type of the struct "LoggerConfiguration" defined in "LoggerConfiguration.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from LoggerConfiguration.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit LoggerConfiguration.idl instead.
*
*/
public class LoggerConfigurationPubSubType implements TopicDataType<us.ihmc.robotDataLogger.LoggerConfiguration>
{
	public static final String name = "us::ihmc::robotDataLogger::LoggerConfiguration";
	
	
	
    public LoggerConfigurationPubSubType()
    {
        
    }

	private final CDR serializeCDR = new CDR();
	private final CDR deserializeCDR = new CDR();

    
    @Override
   public void serialize(us.ihmc.robotDataLogger.LoggerConfiguration data, SerializedPayload serializedPayload) throws IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }
   @Override
   public void deserialize(SerializedPayload serializedPayload, us.ihmc.robotDataLogger.LoggerConfiguration data) throws IOException
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
	            
	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + 255 + 1;

	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + 255 + 1;

	
	    return current_alignment - initial_alignment;
	}


	public final static int getCdrSerializedSize(us.ihmc.robotDataLogger.LoggerConfiguration data)
	{
		return getCdrSerializedSize(data, 0);
	}

	public final static int getCdrSerializedSize(us.ihmc.robotDataLogger.LoggerConfiguration data, int current_alignment)
	{
	    int initial_alignment = current_alignment;
	            
	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + data.getCamerasToCapture().length() + 1;

	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + data.getLoggerNetwork().length() + 1;

	
	    return current_alignment - initial_alignment;
	}
	
   public static void write(us.ihmc.robotDataLogger.LoggerConfiguration data, CDR cdr)
   {

	    if(data.getCamerasToCapture().length() <= 255)
	    cdr.write_type_d(data.getCamerasToCapture());else
	        throw new RuntimeException("camerasToCapture field exceeds the maximum length");

	    if(data.getLoggerNetwork().length() <= 255)
	    cdr.write_type_d(data.getLoggerNetwork());else
	        throw new RuntimeException("loggerNetwork field exceeds the maximum length");
   }

   public static void read(us.ihmc.robotDataLogger.LoggerConfiguration data, CDR cdr)
   {

	    	cdr.read_type_d(data.getCamerasToCapture());	

	    	cdr.read_type_d(data.getLoggerNetwork());	
   }
   
	@Override
	public final void serialize(us.ihmc.robotDataLogger.LoggerConfiguration data, InterchangeSerializer ser)
	{
			    ser.write_type_d("camerasToCapture", data.getCamerasToCapture());
			    
			    ser.write_type_d("loggerNetwork", data.getLoggerNetwork());
			    
	}
	
	@Override
	public final void deserialize(InterchangeSerializer ser, us.ihmc.robotDataLogger.LoggerConfiguration data)
	{
	    			ser.read_type_d("camerasToCapture", data.getCamerasToCapture());	
	    	    
	    			ser.read_type_d("loggerNetwork", data.getLoggerNetwork());	
	    	    
	}

   public static void staticCopy(us.ihmc.robotDataLogger.LoggerConfiguration src, us.ihmc.robotDataLogger.LoggerConfiguration dest)
   {
      dest.set(src);
   }
   
   
   @Override
   public us.ihmc.robotDataLogger.LoggerConfiguration createData()
   {
      return new us.ihmc.robotDataLogger.LoggerConfiguration();
   }
      

   @Override
   public int getTypeSize()
   {
      return CDR.getTypeSize(getMaxCdrSerializedSize());
   }

   @Override
   public String getName()
   {
      return name;
   }
   
   public void serialize(us.ihmc.robotDataLogger.LoggerConfiguration data, CDR cdr)
	{
		write(data, cdr);
	}

   public void deserialize(us.ihmc.robotDataLogger.LoggerConfiguration data, CDR cdr)
   {
        read(data, cdr);
   }
   
   public void copy(us.ihmc.robotDataLogger.LoggerConfiguration src, us.ihmc.robotDataLogger.LoggerConfiguration dest)
   {
      staticCopy(src, dest);
   }	

   
   @Override
   public LoggerConfigurationPubSubType newInstance()
   {
   	  return new LoggerConfigurationPubSubType();
   }
}