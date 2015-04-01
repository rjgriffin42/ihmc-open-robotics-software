package us.ihmc.multicastLogDataProtocol;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import us.ihmc.robotDataCommunication.LogDataHeader;
import us.ihmc.utilities.Pair;

public class ThreadedLogPacketHandler extends Thread implements LogPacketHandler
{
   private final LogPacketHandler handler;
   private final ArrayBlockingQueue<Pair<LogDataHeader, ByteBuffer>> dataBuffer;
   
   private volatile boolean stopped;
   
   public ThreadedLogPacketHandler(LogPacketHandler handler, int capacity)
   {
      this.handler = handler;
      this.dataBuffer = new ArrayBlockingQueue<>(capacity);
   }
   
   @Override
   public void run()
   {
      while(!stopped)
      {
         try
         {
            Pair<LogDataHeader, ByteBuffer> buffer = dataBuffer.take();
            handler.newDataAvailable(buffer.first(), buffer.second());
         }
         catch (InterruptedException e)
         {
         }
      }
      
      dataBuffer.clear();
   }
   
   public void shutdown()
   {
      stopped = true;
      interrupt();
   }
   

   @Override
   public void timestampReceived(long timestamp)
   {
      handler.timestampReceived(timestamp);
   }

   @Override
   public void newDataAvailable(LogDataHeader header, ByteBuffer buffer)
   {
      dataBuffer.offer(new Pair<>(header, buffer));
   }

   @Override
   public void timeout()
   {
      handler.timeout();
   }
   
}