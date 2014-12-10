package us.ihmc.multicastLogDataProtocol.broadcast;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class AnnounceRequest
{
   enum AnnounceType
   {
      CAN_I_HAZ(0x5), ANNOUNCE(0x12);

      private byte header;

      AnnounceType(int header)
      {
         this.header = (byte) header;
      }

      public byte getHeader()
      {
         return header;
      }

      public static AnnounceRequest.AnnounceType[] values = values();

      public static AnnounceRequest.AnnounceType fromHeader(byte header)
      {
         for (AnnounceRequest.AnnounceType type : values)
         {
            if (type.getHeader() == header)
            {
               return type;
            }
         }

         return null;
      }
   }

   public AnnounceRequest.AnnounceType type;
   public long sessionID;
   public byte[] group = new byte[4];
   public short dataPort;
   public byte[] controlIP = new byte[4];
   public short controlPort;
   public byte[] cameras;

   public String name;

   public boolean log = true;

   public AnnounceRequest()
   {

   }

   public AnnounceRequest(AnnounceRequest original)
   {
      this.type = original.type;
      this.sessionID = original.sessionID;
      System.arraycopy(original.group, 0, group, 0, group.length);
      this.dataPort = original.dataPort;
      System.arraycopy(original.controlIP, 0, controlIP, 0, controlIP.length);
      this.controlPort = original.controlPort;
      this.name = new String(original.name);
      if (original.cameras != null)
      {
         this.cameras = new byte[original.cameras.length];
         System.arraycopy(original.cameras, 0, cameras, 0, original.cameras.length);
      }
   }

   public boolean readHeader(ByteBuffer buffer)
   {
      if (buffer.remaining() >= 19)
      {
         type = AnnounceType.fromHeader(buffer.get());
         sessionID = buffer.getLong();
         buffer.get(group);
         dataPort = buffer.getShort();
         buffer.get(controlIP);
         controlPort = buffer.getShort();
         return true;
      }
      else
      {
         return false;
      }
   }

   public boolean readVariableLengthData(ByteBuffer buffer)
   {
      if (buffer.hasRemaining())
      {
         int numberOfCameras = buffer.get() & 0xFF;
         if (buffer.remaining() >= numberOfCameras)
         {
            cameras = new byte[numberOfCameras];
            for (int i = 0; i < numberOfCameras; i++)
            {
               cameras[i] = buffer.get();
            }
         }
         else
         {
            return false;
         }
      }
      else
      {
         return false;
      }
      if (buffer.hasRemaining())
      {
         int nameLength = buffer.get() & 0xFF;
         if (buffer.remaining() >= nameLength)
         {
            byte[] namebytes = new byte[nameLength];
            buffer.get(namebytes);
            name = new String(namebytes);
         }
         else
         {
            return false;
         }
      }
      else
      {
         return false;
      }

      return true;
   }

   public ByteBuffer createRequest(ByteBuffer buffer)
   {
      buffer.clear();

      byte[] namebytes = name.getBytes();
      buffer.put((byte) type.getHeader());
      buffer.putLong(sessionID);
      buffer.put(group);
      buffer.putShort(dataPort);
      buffer.put(controlIP);
      buffer.putShort(controlPort);
      if (cameras != null)
      {
         buffer.put((byte) cameras.length);
         buffer.put(cameras);
      }
      else
      {
         buffer.put((byte) 0);
      }

      buffer.put((byte) namebytes.length);
      buffer.put(namebytes);
      buffer.flip();

      return buffer;
   }

   public AnnounceRequest.AnnounceType getType()
   {
      return type;
   }

   public void setType(AnnounceRequest.AnnounceType type)
   {
      this.type = type;
   }

   public long getSessionID()
   {
      return sessionID;
   }

   public void setSessionID(long sessionID)
   {
      this.sessionID = sessionID;
   }

   public byte[] getGroup()
   {
      return group;
   }

   public void setGroup(byte[] group)
   {
      this.group = group;
   }

   public String getName()
   {
      return name;
   }

   public void setName(String name)
   {
      if (name.length() > 255)
      {
         throw new RuntimeException("Name cannot be longer than 255 characters");
      }
      this.name = name;
   }

   public byte[] getControlIP()
   {
      return controlIP;
   }

   public void setControlIP(byte[] controlIP)
   {
      this.controlIP = controlIP;
   }

   public int getControlPort()
   {
      return controlPort & 0xFFFF;
   }

   public void setControlPort(int controlPort)
   {
      this.controlPort = (short) controlPort;
   }

   public int getDataPort()
   {
      return dataPort & 0xFFFF;
   }

   public void setDataPort(int dataPort)
   {
      this.dataPort = (short) dataPort;
   }

   public boolean isLog()
   {
      return log;
   }

   public void setLog(boolean log)
   {
      this.log = log;
   }

   public byte[] getCameras()
   {
      return cameras;
   }

   public void setCameras(byte[] cameras)
   {
      this.cameras = cameras;
   }

   public void setCameras(int[] cameras)
   {
      this.cameras = new byte[cameras.length];
      for (int i = 0; i < cameras.length; i++)
      {
         this.cameras[i] = (byte) cameras[i];
      }
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(cameras);
      result = prime * result + Arrays.hashCode(controlIP);
      result = prime * result + controlPort;
      result = prime * result + Arrays.hashCode(group);
      result = prime * result + (log ? 1231 : 1237);
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + (int) (sessionID ^ (sessionID >>> 32));
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AnnounceRequest other = (AnnounceRequest) obj;
      if (!Arrays.equals(cameras, other.cameras))
         return false;
      if (!Arrays.equals(controlIP, other.controlIP))
         return false;
      if (controlPort != other.controlPort)
         return false;
      if (!Arrays.equals(group, other.group))
         return false;
      if (log != other.log)
         return false;
      if (name == null)
      {
         if (other.name != null)
            return false;
      }
      else if (!name.equals(other.name))
         return false;
      if (sessionID != other.sessionID)
         return false;
      if (type != other.type)
         return false;
      return true;
   }

   public String toString()
   {
      return String.valueOf(sessionID);
   }
}