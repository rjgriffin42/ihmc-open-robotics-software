package us.ihmc.aware.packets;

import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedJointName;
import us.ihmc.tools.ArrayTools;

import java.util.HashMap;
import java.util.Map;

public class QuadrupedNeckJointPositionPacket extends Packet<QuadrupedNeckJointPositionPacket>
{
   public QuadrupedJointName neckJointName[];
   public double neckJointPosition[];

   public QuadrupedNeckJointPositionPacket()
   {
      setDestination(PacketDestination.CONTROLLER);
      neckJointName = new QuadrupedJointName[0];
      neckJointPosition = new double[0];
   }

   public QuadrupedNeckJointPositionPacket(QuadrupedJointName[] neckJointName, double[] neckJointPosition)
   {
      this();
      this.neckJointName = new QuadrupedJointName[neckJointName.length];
      this.neckJointPosition = new double[neckJointPosition.length];
      for (int i = 0; i < neckJointPosition.length; i++)
      {
         this.neckJointName[i] = neckJointName[i];
         this.neckJointPosition[i] = neckJointPosition[i];
      }
   }

   public QuadrupedNeckJointPositionPacket(HashMap<QuadrupedJointName, Double> neckJointPositionMap)
   {
      this();
      this.neckJointName = new QuadrupedJointName[neckJointPositionMap.size()];
      this.neckJointPosition = new double[neckJointPositionMap.size()];
      int i = 0;
      for (Map.Entry<QuadrupedJointName, Double> entry : neckJointPositionMap.entrySet())
      {
         this.neckJointName[i] = entry.getKey();
         this.neckJointPosition[i] = entry.getValue();
         i++;
      }
   }

   public int size()
   {
      return neckJointPosition.length;
   }

   public QuadrupedJointName getJointName(int i)
   {
      return neckJointName[i];
   }

   public double getJointPosition(int i)
   {
      return neckJointPosition[i];
   }

   @Override
   public String toString()
   {
      String string = "joint position:";
      for (int i = 0; i < neckJointPosition.length; i++)
      {
         string += " ";
         string += neckJointPosition[i];
      }
      return string;
   }

   @Override
   public boolean epsilonEquals(QuadrupedNeckJointPositionPacket other, double epsilon)
   {
      return ArrayTools.deltaEquals(neckJointPosition, other.neckJointPosition, epsilon);
   }
}
