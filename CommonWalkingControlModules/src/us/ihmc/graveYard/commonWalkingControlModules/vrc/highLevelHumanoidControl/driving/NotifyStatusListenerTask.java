package us.ihmc.graveYard.commonWalkingControlModules.vrc.highLevelHumanoidControl.driving;

import us.ihmc.utilities.io.streamingData.GlobalDataProducer;
import us.ihmc.utilities.taskExecutor.Task;

public class NotifyStatusListenerTask<T> implements Task
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

}
