package us.ihmc.humanoidRobotics.communication.packets.walking;

import us.ihmc.communication.packets.Packet;

public class AutomaticManipulationAbortMessage extends Packet<AutomaticManipulationAbortMessage>
{
   public boolean enable;

   public AutomaticManipulationAbortMessage()
   {
   }

   public AutomaticManipulationAbortMessage(boolean enable)
   {
      this.enable = enable;
   }

   public void set(AutomaticManipulationAbortMessage other)
   {
      enable = other.enable;
   }

   public boolean getEnable()
   {
      return enable;
   }

   public void setEnable(boolean enable)
   {
      this.enable = enable;
   }

   @Override
   public boolean epsilonEquals(AutomaticManipulationAbortMessage other, double epsilon)
   {
      if (other == null)
         return false;
      if (enable != other.enable)
         return false;
      return true;
   }
}
