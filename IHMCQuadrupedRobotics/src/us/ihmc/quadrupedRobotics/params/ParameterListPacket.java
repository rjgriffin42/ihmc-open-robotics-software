package us.ihmc.quadrupedRobotics.params;

import java.util.List;

import us.ihmc.communication.packets.Packet;

public class ParameterListPacket extends Packet<ParameterListPacket>
{
   private List<Parameter> parameters;

   public ParameterListPacket()
   {
   }

   public ParameterListPacket(List<Parameter> parameters)
   {
      this.parameters = parameters;
   }

   public List<Parameter> getParameters()
   {
      return parameters;
   }

   @Override
   public boolean epsilonEquals(ParameterListPacket other, double epsilon)
   {
      return false;
   }
}
