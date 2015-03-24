package us.ihmc.communication.packets;

import us.ihmc.communication.packetAnnotations.ClassDocumentation;

public abstract class IHMCRosApiPacket<T> extends Packet<T>
{
   public IHMCRosApiPacket()
   {
      if(!this.getClass().isAnnotationPresent(ClassDocumentation.class))
         throw new RuntimeException("Documentation annotation could not be found for " + this.getClass().getName());
   }
}
