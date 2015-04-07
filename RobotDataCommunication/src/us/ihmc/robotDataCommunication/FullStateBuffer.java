package us.ihmc.robotDataCommunication;

import java.nio.LongBuffer;
import java.util.List;

import us.ihmc.robotDataCommunication.jointState.JointHolder;
import us.ihmc.util.RealtimeTools;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

public class FullStateBuffer extends RegistryBuffer
{
   private long uid = 0;
   private final double[] jointStates;
   private final List<JointHolder> jointHolders;
   private final int numberOfJointStates;

   public FullStateBuffer(int variableOffset, List<YoVariable<?>> variables, List<JointHolder> jointHolders)
   {
      super(variableOffset, variables);
      this.numberOfJointStates = getNumberOfJointStates(jointHolders);
      this.jointStates = new double[RealtimeTools.nextDivisibleByEight(this.numberOfJointStates)];
      this.jointHolders = jointHolders;
   }

   public long getUid()
   {
      return uid;
   }
   
   public static int getNumberOfJointStates(List<JointHolder> jointHolders)
   {
      int numberOfJointStates = 0;
      for (int i = 0; i < jointHolders.size(); i++)
      {
         numberOfJointStates += jointHolders.get(i).getNumberOfStateVariables();
      }
      return numberOfJointStates;
   }

   @Override
   public void update(long timestamp)
   {
      super.update(timestamp);
      
      uid++;
      int offset = 0;
      
      for (int i = 0; i < jointHolders.size(); i++)
      {
         JointHolder jointHolder = jointHolders.get(i);
         jointHolder.get(jointStates, offset);
         offset += jointHolder.getNumberOfStateVariables();
      }
   }

   public void getJointStatesInBuffer(LongBuffer buffer, int offset)
   {
      for (int i = 0; i < numberOfJointStates; i++)
      {
         buffer.put(i + offset, Double.doubleToLongBits(jointStates[i]));
      }
   }

   public static class Builder implements us.ihmc.concurrent.Builder<FullStateBuffer>
   {
      private final List<JointHolder> jointHolders;
      private final int variableOffset;
      private final List<YoVariable<?>> variables;

      public Builder(int variableOffset, List<YoVariable<?>> variables, List<JointHolder> jointHolders)
      {
         this.jointHolders = jointHolders;
         this.variableOffset = variableOffset;
         this.variables = variables;
      }

      public FullStateBuffer newInstance()
      {
         return new FullStateBuffer(variableOffset, variables, jointHolders);
      }

   }

}
