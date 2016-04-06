package us.ihmc.aware.packets;

import com.google.common.math.DoubleMath;
import us.ihmc.communication.packets.Packet;

public class SetDoubleParameterPacket extends Packet<SetDoubleParameterPacket>
{
   private final String parameterName;
   private final double parameterValue;

   public SetDoubleParameterPacket()
   {
      this(null, Double.NaN);
   }

   public SetDoubleParameterPacket(String parameterName, double parameterValue)
   {
      this.parameterName = parameterName;
      this.parameterValue = parameterValue;
   }

   public String getParameterName()
   {
      return parameterName;
   }

   public double getParameterValue()
   {
      return parameterValue;
   }

   @Override
   public boolean epsilonEquals(SetDoubleParameterPacket other, double epsilon)
   {
      return parameterName.equals(other.parameterName) && DoubleMath.fuzzyEquals(parameterValue, other.parameterValue, 1e-12);
   }
}
