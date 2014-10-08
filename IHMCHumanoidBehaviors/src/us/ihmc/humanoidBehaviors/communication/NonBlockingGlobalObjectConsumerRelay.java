package us.ihmc.humanoidBehaviors.communication;

import java.util.concurrent.ConcurrentLinkedQueue;

import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.net.GlobalObjectConsumer;
import us.ihmc.utilities.net.ObjectCommunicator;

public class NonBlockingGlobalObjectConsumerRelay implements GlobalObjectConsumer
{
   private final ConcurrentLinkedQueue<Object> queuedData = new ConcurrentLinkedQueue<Object>(); 
   private final ObjectCommunicator communicatorToForwardFrom;
   private final ObjectCommunicator communicatorToForwardTo;
   
   public NonBlockingGlobalObjectConsumerRelay(ObjectCommunicator communicatorToForwardFrom, ObjectCommunicator communicatorToForwardTo)
   {
      this.communicatorToForwardFrom = communicatorToForwardFrom;
      this.communicatorToForwardTo = communicatorToForwardTo;
      startProducingData();
   }
   
   public void enableForwarding()
   {
      communicatorToForwardFrom.attachGlobalListener(this);
   }
   
   public void disableForwarding()
   {
      communicatorToForwardFrom.detachGlobalListener(this);
   }
   
   public void consumeObject(Object object, boolean consumeGlobal)
   {
      if(!consumeGlobal)
         queuedData.add(object);
   }

   @Override
   public void consumeObject(Object object)
   {
      queuedData.add(object);
   }

   public void startProducingData()
   {
      Runnable runnable = new Runnable()
      {

         public void run()
         {
            while (true)
            {
               Object dataObject;
               while((dataObject = queuedData.poll()) != null)
               {
                  if(!dataObject.getClass().getSimpleName().equals("RobotConfigurationData") && !dataObject.getClass().getSimpleName().equals("RobotPoseData"))
                     System.out.println(dataObject.getClass().getSimpleName());
                  communicatorToForwardTo.consumeObject(dataObject, false);
               }
               
               ThreadTools.sleep(100);

            }
         }
      };
      ThreadTools.startAsDaemon(runnable, "NonBlockingGlobalObjectConsumerRelay for " + communicatorToForwardFrom.getClass().getSimpleName());
   }
}
