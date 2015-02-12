package us.ihmc.graveYard.commonWalkingControlModules.vrc.highLevelHumanoidControl.driving;

import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.utilities.taskExecutor.Task;

public class NotifyStatusListenerTask<T extends Packet> implements Task
{
   private final GlobalDataProducer drivingStatusListener;
   private final T statusObject;
   
   public NotifyStatusListenerTask(GlobalDataProducer statusProducer, T statusObject)
   {
      this.drivingStatusListener = statusProducer;
      this.statusObject = statusObject;
   }

   public void doTransitionIntoAction()
   {
      drivingStatusListener.queueDataToSend(statusObject);
   }

   public void doAction()
   {

   }

   public void doTransitionOutOfAction()
   {

   }

   public boolean isDone()
   {
      return true;
   }

   @Override
   public void pause()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void resume()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void stop()
   {
      // TODO Auto-generated method stub
      
   }

}
