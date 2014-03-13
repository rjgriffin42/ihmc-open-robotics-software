package us.ihmc.valkyrie.imu;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import us.ihmc.concurrent.ConcurrentCopier;
import us.ihmc.realtime.PriorityParameters;
import us.ihmc.realtime.RealtimeThread;

public class MicrostrainUDPPacketListener implements Runnable
{
   private static final byte AHRS_DESCRIPTOR = (byte) 0x80;

   private static final byte SCALED_ACCELEROMETER_DESCRIPTOR = 0x04;
   private static final byte SCALED_GYRO_DESCRIPTOR = 0x05;
   private static final byte QUATERNION_DESCRIPTOR = 0x0A;

   private DatagramChannel receiveChannel;
   private volatile boolean requestStop = false;
   private final ByteBuffer receiveBuffer = ByteBuffer.allocate(1024);
   
   private final ConcurrentCopier<MicroStrainData> microstrainBuffer = new ConcurrentCopier<>(new MicroStrainData.MicroStrainDataBuilder());

   public MicrostrainUDPPacketListener(int port) throws IOException
   {
      receiveChannel = DatagramChannel.open();
      receiveChannel.socket().setReceiveBufferSize(65535);
      receiveChannel.socket().bind(new InetSocketAddress(port));
   }

   public boolean isChecksumValid(ByteBuffer buffer)
   {
      int intA = 0;
      int intB = 0;

      for (int i = buffer.position(); i < (buffer.limit() - 2); i++)
      {
         intA = (intA + ((int) buffer.get() & 0xFF)) & 0xFF;
         intB = (intB + (intA & 0xFF)) & 0xFF;
      }

      byte A = (byte) (intA & 0xFF);
      byte B = (byte) (intB & 0xFF);

      byte checkA = buffer.get();
      byte checkB = buffer.get();

      return (A == checkA && B == checkB);
   }

   public void readFields(ByteBuffer buffer)
   {
      MicroStrainData data = microstrainBuffer.getCopyForWriting();
      
      long time = RealtimeThread.getCurrentMonotonicClockTime();
      data.setReceiveTime(time);
      
      while (buffer.position() < buffer.limit() - 2)
      {
         int fieldLength = buffer.get();
         int descriptor = buffer.get();
         switch (descriptor)
         {
         case QUATERNION_DESCRIPTOR:
            data.setQuaternion(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            break;
         case SCALED_ACCELEROMETER_DESCRIPTOR:
            data.setAcceleration(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            break;
         case SCALED_GYRO_DESCRIPTOR:
            data.setGyro(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            break;
         default:
            System.err.println("Unknown field " + descriptor);
            buffer.position(buffer.position() + fieldLength);
            break;
         }
      }
      microstrainBuffer.commit();
   }
   
   public MicroStrainData getLatestData()
   {
      return microstrainBuffer.getCopyForReading();
   }

   @Override
   public void run()
   {
      while (!requestStop)
      {
         try
         {
            receiveBuffer.clear();
            receiveChannel.receive(receiveBuffer);
            receiveBuffer.flip();

            if (!isChecksumValid(receiveBuffer))
            {
               System.err.println("Invalid checksum");
               continue;
            }

            byte descriptor = receiveBuffer.get(2);
            receiveBuffer.position(4);
            switch (descriptor)
            {
            case AHRS_DESCRIPTOR:
               readFields(receiveBuffer);
               break;
            default:
               System.err.println("Unknown packet type  " + descriptor);
            }
         }
         catch (IOException e)
         {
            e.printStackTrace();
            requestStop = true;
         }
      }
   }
   
   public void stop()
   {
      requestStop = true;
   }
   
   public static MicrostrainUDPPacketListener createRealtimeListener(PriorityParameters priority, long serialNumber) throws IOException
   {
      int port = 50000 + (int) (serialNumber % 10000);
      MicrostrainUDPPacketListener listener = new MicrostrainUDPPacketListener(port);
      new RealtimeThread(priority, listener).start();
      
      return listener;
      
   }

   public static void main(String[] args) throws IOException
   {
      new Thread(new MicrostrainUDPPacketListener(50571)).start();
   }
}
