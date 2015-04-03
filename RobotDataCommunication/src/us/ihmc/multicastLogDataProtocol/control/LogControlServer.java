package us.ihmc.multicastLogDataProtocol.control;

import java.io.IOException;
import java.net.BindException;
import java.util.LinkedHashMap;

import us.ihmc.communication.net.KryoObjectServer;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.concurrent.ConcurrentRingBuffer;
import us.ihmc.multicastLogDataProtocol.LogDataProtocolSettings;
import us.ihmc.robotDataCommunication.VariableChangedMessage;
import us.ihmc.robotDataCommunication.YoVariableHandShakeBuilder;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

public class LogControlServer
{
   private int port = LogDataProtocolSettings.LOG_DATA_PORT_RANGE_START;
   private KryoObjectServer server;

   private final YoVariableHandShakeBuilder handshakeBuilder;
   private final LinkedHashMap<YoVariableRegistry, ConcurrentRingBuffer<VariableChangedMessage>> variableChangeData;


   public LogControlServer(YoVariableHandShakeBuilder handshakeBuilder, LinkedHashMap<YoVariableRegistry, ConcurrentRingBuffer<VariableChangedMessage>> variableChangeData, double dt)
   {
      this.variableChangeData = variableChangeData;
      this.handshakeBuilder = handshakeBuilder;
   }

   public YoVariableHandShakeBuilder getHandshakeBuilder()
   {
      return handshakeBuilder;
   }

   public void start()
   {
      try
      {
         boolean connected = false;
         do
         {
            server = new KryoObjectServer(port, new LogControlClassList());
            try
            {
               server.connect();
               connected = true;
            }
            catch (BindException e)
            {
               server.close();
               port++;
            }
         }
         while (!connected);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }

      
      server.attachListener(VariableChangeRequest.class, new VariableChangeRequestListener());
      server.attachListener(ClearLogRequest.class, new ClearLogRequestListener());
   }

   public class VariableChangeRequestListener implements ObjectConsumer<VariableChangeRequest>
   {

      @Override
      public void consumeObject(VariableChangeRequest object)
      {
         VariableChangedMessage message;
         Pair<YoVariable<?>, YoVariableRegistry> variableAndRootRegistry = handshakeBuilder.getVariablesAndRootRegistries().get(object.variableID);

         ConcurrentRingBuffer<VariableChangedMessage> buffer = variableChangeData.get(variableAndRootRegistry.second());
         while ((message = buffer.next()) == null)
         {
            ThreadTools.sleep(1);
         }

         if (message != null)
         {
            message.setVariable(variableAndRootRegistry.first());
            message.setVal(object.requestedValue);
            buffer.commit();
         }
      }

   }

   public class ClearLogRequestListener implements ObjectConsumer<ClearLogRequest>
   {

      @Override
      public void consumeObject(ClearLogRequest object)
      {
         System.out.println("Broadcasting clear log");
         server.consumeObject(object);
      }
      
   }

   
   public int getPort()
   {
      return port;
   }

   public void close()
   {
      server.close();
   }

}
