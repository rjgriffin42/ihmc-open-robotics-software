package us.ihmc.robotDataCommunication.logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import us.ihmc.SdfLoader.GeneralizedSDFRobotModel;
import us.ihmc.SdfLoader.SDFJointNameMap;
import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.robotDataCommunication.jointState.JointState;
import us.ihmc.robotDataCommunication.visualizer.JointUpdater;
import us.ihmc.yoUtilities.dataStructure.listener.RewoundListener;
import us.ihmc.yoUtilities.dataStructure.listener.VariableChangedListener;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.YoVariable;

import com.yobotics.simulationconstructionset.Joint;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;

public class YoVariableLogPlaybackRobot extends SDFRobot implements RewoundListener
{
   private static final long serialVersionUID = -1461790917855807447L;

   private final SimulationConstructionSet scs;
   private final LongYoVariable timestamp;
   private final FileChannel logChannel;
   private final List<YoVariable> variables;

   private final List<JointState<? extends Joint>> jointStates;
   private final ArrayList<JointUpdater> jointUpdaters = new ArrayList<JointUpdater>();

   private final ByteBuffer logLine;
   private final LongBuffer logLongArray;

   private final LongYoVariable currentRecordTick;

   private final int numberOfEntries;
   private final long initialTimestamp;
   private final long finalTimestamp = 0;
   
   public YoVariableLogPlaybackRobot(GeneralizedSDFRobotModel generalizedSDFRobotModel, SDFJointNameMap sdfJointNameMap,
         List<JointState<? extends Joint>> jointStates, List<YoVariable> variables, FileChannel logChannel, SimulationConstructionSet scs)
   {
      super(generalizedSDFRobotModel, sdfJointNameMap, false);
      this.timestamp = new LongYoVariable("timestamp", getRobotsYoVariableRegistry());
      this.jointStates = jointStates;
      this.variables = variables;
      this.logChannel = logChannel;
      this.scs = scs;

      
      int jointStateOffset = variables.size();
      int numberOfJointStates = JointState.getNumberOfJointStates(jointStates);
      int bufferSize = (1 + jointStateOffset + numberOfJointStates) * 8;
      JointUpdater.getJointUpdaterList(getRootJoints(), jointStates, jointUpdaters);

      logLine = ByteBuffer.allocate(bufferSize);
      logLongArray = logLine.asLongBuffer();

      currentRecordTick = new LongYoVariable("currentRecordTick", yoVariableRegistry);

      try
      {
         numberOfEntries = (int) (logChannel.size() / bufferSize) - 1;
         logChannel.read(logLine);
         initialTimestamp = logLine.getLong(0);
         logLine.clear();
//         logChannel.position((numberOfEntries) * bufferSize);
//         logChannel.read(logLine);
//         finalTimestamp = logLine.getLong(0);
         logLine.clear();
         logChannel.position(0);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      
      
      scs.setRobot(this);
      scs.attachSimulationRewoundListener(this);
   }
   
   public long getInitialTimestamp()
   {
      return initialTimestamp;
   }
   
   public long getFinalTimestamp()
   {
      return finalTimestamp;
   }
   
   public int getNumberOfEntries()
   {
      return numberOfEntries;
   }

   public LongYoVariable getTimestamp()
   {
      return timestamp;
   }
   
   public void seek(long position)
   {
      currentRecordTick.set(position);
      try
      {
         logChannel.position(position * logLine.capacity());
      }
      catch(IOException e)
      {
         throw new RuntimeException(e);
      }
   }
   
   @Override
   public void doDynamicsAndIntegrate(double DT)
   {
      logLine.clear();
      logLongArray.clear();
      try
      {
         int bytesRead = logChannel.read(logLine);
         if (bytesRead == -1)
         {
            System.out.println("Reached end of file, stopping simulation thread");
            scs.stop();
            return;
         }

         if (bytesRead != logLine.capacity())
         {
            throw new RuntimeException("Expected read of " + logLine.capacity() + ", got " + bytesRead
                  + ". TODO: Implement loop for reading the full log line.");
         }

         timestamp.set(logLongArray.get());
               
         for (int i = 0; i < variables.size(); i++)
         {
            YoVariable variable = variables.get(i);
            variable.setValueFromLongBits(logLongArray.get());
         }

         for (int i = 0; i < jointStates.size(); i++)
         {
            jointStates.get(i).update(logLongArray);
         }

         for (int i = 0; i < jointUpdaters.size(); i++)
         {
            jointUpdaters.get(i).update();
         }
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }

      t.add(DT);
      update();

      currentRecordTick.increment();

   }
   
   public void addCurrentRecordTickListener(VariableChangedListener listener)
   {
      currentRecordTick.addVariableChangedListener(listener);
   }

   public void wasRewound()
   {
      long position = currentRecordTick.getLongValue();
      try
      {
         logChannel.position(position * logLine.capacity());
      }
      catch (IOException e)
      {
         throw new RuntimeException("Cannot skip to position " + position);
      }
   }
}
