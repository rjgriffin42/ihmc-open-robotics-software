package us.ihmc.robotDataLogger.websocket.server;

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.pubsub.common.SerializedPayload;
import us.ihmc.robotDataLogger.dataBuffers.LoggerDebugRegistry;
import us.ihmc.robotDataLogger.dataBuffers.RegistrySendBuffer;
import us.ihmc.robotDataLogger.dataBuffers.RegistrySendBufferBuilder;
import us.ihmc.robotDataLogger.interfaces.RegistryPublisher;
import us.ihmc.robotDataLogger.rtps.CustomLogDataPublisherType;
import us.ihmc.util.PeriodicThreadScheduler;
import us.ihmc.util.PeriodicThreadSchedulerFactory;

public class WebsocketRegistryPublisher implements RegistryPublisher
{
   
   private static final int BUFFER_CAPACITY = 128;

   private long uid = 0;
   private final ConcurrentRingBuffer<RegistrySendBuffer> ringBuffer;
   
   private final WebsocketDataBroadcaster broadcaster;
   private final LoggerDebugRegistry loggerDebugRegistry;

   private final PeriodicThreadScheduler scheduler;

   private final VariableUpdateThread variableUpdateThread = new VariableUpdateThread();
   
   private final CustomLogDataPublisherType publisherType;
   private final ByteBuf sendBuffer;
   private final SerializedPayload serializedPayload;
   private final BinaryWebSocketFrame frame;

   private final int numberOfVariables;
   
   public WebsocketRegistryPublisher(PeriodicThreadSchedulerFactory schedulerFactory, RegistrySendBufferBuilder builder, WebsocketDataBroadcaster broadcaster)
   {
      this.broadcaster = broadcaster;
      
      this.ringBuffer = new ConcurrentRingBuffer<>(builder, BUFFER_CAPACITY);
      this.scheduler = schedulerFactory.createPeriodicThreadScheduler("Registry-" + builder.getRegistryID() + "-Publisher");
      
      this.loggerDebugRegistry = builder.getLoggerDebugRegistry();
      this.numberOfVariables = builder.getNumberOfVariables();
      
      publisherType = new CustomLogDataPublisherType(builder.getNumberOfVariables(), builder.getNumberOfJointStates());
      
      
      serializedPayload = new SerializedPayload(publisherType.getMaximumTypeSize());
      sendBuffer = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer(serializedPayload.getData()));
      frame = new BinaryWebSocketFrame(sendBuffer);

   }

   @Override
   public void start()
   {
      scheduler.schedule(variableUpdateThread, 1, TimeUnit.MILLISECONDS);

   }

   @Override
   public void stop()
   {
      scheduler.shutdown();
      try
      {
         scheduler.awaitTermination(5, TimeUnit.SECONDS);
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
      
   }

   @Override
   public void update(long timestamp)
   {
      RegistrySendBuffer buffer = ringBuffer.next();
      if (buffer != null)
      {
         buffer.updateBufferFromVariables(timestamp, uid, 0, 0, numberOfVariables);
         ringBuffer.commit();
      }
      else
      {
         this.loggerDebugRegistry.circularBufferFull();
      }
      
      uid++;
   }

   

   private class VariableUpdateThread implements Runnable
   {
      private long previousUid = -1;
      
      private VariableUpdateThread()
      {

      }

      @Override
      public void run()
      {
         try
         {
            while (ringBuffer.poll())
            {
               RegistrySendBuffer buffer;

               if ((buffer = ringBuffer.read()) != null)
               {


                  serializedPayload.getData().clear();
                  sendBuffer.clear();
                  publisherType.serialize(buffer, serializedPayload);
                  sendBuffer.setIndex(0, serializedPayload.getLength());
                  broadcaster.write(frame);

                  if(previousUid != -1)
                  {
                     if(buffer.getUid() != previousUid + 1)
                     {
                        loggerDebugRegistry.lostTickInCircularBuffer();
                     }
                  }
                  previousUid = buffer.getUid();
               }
               ringBuffer.flush();
            }

         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }

      }

   }

}
