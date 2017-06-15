package us.ihmc.humanoidRobotics.communication.packets.wholebody;

import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.VisualizablePacket;

/**
 * This message is used to clear the delay buffers on the controller, 
 * If you sent a message with a delay and now you do not want them executed, use this command
 */
public class ClearDelayQueueMessage extends Packet<ClearDelayQueueMessage> implements VisualizablePacket
{   
   /** the class you want to clear **/
   public Class<Command<?,?>> clazz;
   
   /** clear all the delay buffers **/
   public boolean clearAllDelayBuffers;
   
   /**
    * empty constructor, required for kryo
    */
   public ClearDelayQueueMessage()
   {
   }

   /**
    * set the class you want to clear
    * @param clazz the class you want to clear
    */
   public ClearDelayQueueMessage(Class<Command<?,?>> clazz)
   {
      this.clazz = clazz;
   }
   
   /**
    * set the class you want to clear
    * @param clazz the class you want to clear
    */
   public void setClazz(Class<Command<?, ?>> clazz)
   {
      this.clazz = clazz;
   }
   
   /**
    * get the class to clear
    * @param clazz the class to clear
    */
   public Class<Command<?, ?>> getClazz()
   {
      return clazz;
   }
   
   /**
    * set whether or not you want to clear all the delay buffers
    * @param whether or not to clear all the delay buffers
    */
   public void setClearAllDelayBuffers(boolean clearAll)
   {
      this.clearAllDelayBuffers = clearAll;
   }
   
   /**
    * get whether or not to clear all the delay buffers
    * @param whether or not to clear all the delay buffers
    */
   public boolean getClearAllDelayBuffers()
   {
      return clearAllDelayBuffers;
   }
   
   @Override
   public boolean epsilonEquals(ClearDelayQueueMessage other, double epsilon)
   {
      if (this == other)
         return true;
      if (other == null)
         return false;
      if (clazz == null)
      {
         if (other.clazz != null)
            return false;
      }
      else if (!clazz.equals(other.clazz))
         return false;
      if (clearAllDelayBuffers != other.clearAllDelayBuffers)
         return false;
      return true;
   }
}
