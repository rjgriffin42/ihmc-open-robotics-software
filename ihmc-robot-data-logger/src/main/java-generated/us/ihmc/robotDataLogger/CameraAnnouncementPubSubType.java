package us.ihmc.robotDataLogger;

import java.io.IOException;

import us.ihmc.pubsub.TopicDataType;
import us.ihmc.pubsub.common.SerializedPayload;
import us.ihmc.idl.InterchangeSerializer;
import us.ihmc.idl.CDR;
import us.ihmc.idl.IDLSequence;

/**
* 
* Topic data type of the struct "CameraAnnouncement" defined in "Announcement.idl". Use this class to provide the TopicDataType to a Participant. 
*
* This file was automatically generated from Announcement.idl by us.ihmc.idl.generator.IDLGenerator. 
* Do not update this file directly, edit Announcement.idl instead.
*
*/
public class CameraAnnouncementPubSubType implements TopicDataType<us.ihmc.robotDataLogger.CameraAnnouncement>
{
	public static final String name = "us::ihmc::robotDataLogger::CameraAnnouncement";
	
	
	
    public CameraAnnouncementPubSubType()
    {
        
    }

	private final CDR serializeCDR = new CDR();
	private final CDR deserializeCDR = new CDR();

    
    @Override
   public void serialize(us.ihmc.robotDataLogger.CameraAnnouncement data, SerializedPayload serializedPayload) throws IOException
   {
      serializeCDR.serialize(serializedPayload);
      write(data, serializeCDR);
      serializeCDR.finishSerialize();
   }
   @Override
   public void deserialize(SerializedPayload serializedPayload, us.ihmc.robotDataLogger.CameraAnnouncement data) throws IOException
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
	            
	    current_alignment += 4 + CDR.alignment(current_alignment, 4);

	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + 255 + 1;

	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + 255 + 1;

	
	    return current_alignment - initial_alignment;
	}


	public final static int getCdrSerializedSize(us.ihmc.robotDataLogger.CameraAnnouncement data)
	{
		return getCdrSerializedSize(data, 0);
	}

	public final static int getCdrSerializedSize(us.ihmc.robotDataLogger.CameraAnnouncement data, int current_alignment)
	{
	    int initial_alignment = current_alignment;
	            
	    current_alignment += 4 + CDR.alignment(current_alignment, 4);

	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + data.getName().length() + 1;

	    current_alignment += 4 + CDR.alignment(current_alignment, 4) + data.getIdentifier().length() + 1;

	
	    return current_alignment - initial_alignment;
	}
	
   public static void write(us.ihmc.robotDataLogger.CameraAnnouncement data, CDR cdr)
   {

	    cdr.write_type_c(data.getType().ordinal());


	    if(data.getName().length() <= 255)
	    cdr.write_type_d(data.getName());else
	        throw new RuntimeException("name field exceeds the maximum length");

	    if(data.getIdentifier().length() <= 255)
	    cdr.write_type_d(data.getIdentifier());else
	        throw new RuntimeException("identifier field exceeds the maximum length");
   }

   public static void read(us.ihmc.robotDataLogger.CameraAnnouncement data, CDR cdr)
   {

	    	data.setType(us.ihmc.robotDataLogger.CameraType.values[cdr.read_type_c()]);
	    	

	    	cdr.read_type_d(data.getName());	

	    	cdr.read_type_d(data.getIdentifier());	
   }
   
	@Override
	public final void serialize(us.ihmc.robotDataLogger.CameraAnnouncement data, InterchangeSerializer ser)
	{
			    ser.write_type_c("type", data.getType());
			    
			    ser.write_type_d("name", data.getName());
			    
			    ser.write_type_d("identifier", data.getIdentifier());
			    
	}
	
	@Override
	public final void deserialize(InterchangeSerializer ser, us.ihmc.robotDataLogger.CameraAnnouncement data)
	{
	    			data.setType((us.ihmc.robotDataLogger.CameraType)ser.read_type_c("type", us.ihmc.robotDataLogger.CameraType.class));
	    	
	    	    
	    			ser.read_type_d("name", data.getName());	
	    	    
	    			ser.read_type_d("identifier", data.getIdentifier());	
	    	    
	}

   public static void staticCopy(us.ihmc.robotDataLogger.CameraAnnouncement src, us.ihmc.robotDataLogger.CameraAnnouncement dest)
   {
      dest.set(src);
   }
   
   
   @Override
   public us.ihmc.robotDataLogger.CameraAnnouncement createData()
   {
      return new us.ihmc.robotDataLogger.CameraAnnouncement();
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
   
   public void serialize(us.ihmc.robotDataLogger.CameraAnnouncement data, CDR cdr)
	{
		write(data, cdr);
	}

   public void deserialize(us.ihmc.robotDataLogger.CameraAnnouncement data, CDR cdr)
   {
        read(data, cdr);
   }
   
   public void copy(us.ihmc.robotDataLogger.CameraAnnouncement src, us.ihmc.robotDataLogger.CameraAnnouncement dest)
   {
      staticCopy(src, dest);
   }	

   
   @Override
   public CameraAnnouncementPubSubType newInstance()
   {
   	  return new CameraAnnouncementPubSubType();
   }
}