package us.ihmc.atlas.drcsimGazebo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.simulationconstructionset.OneDegreeOfFreedomJoint;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDataHolder;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.wholeBodyController.DRCOutputWriter;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

public class AtlasDRCSimGazeboOutputWriter implements DRCOutputWriter
{
   private final SocketAddress address = new InetSocketAddress("127.0.0.1", 1235);

   private SocketChannel channel;

   private final YoVariableRegistry registry = new YoVariableRegistry(DRCSimGazeboSensorReaderFactory.class.getSimpleName());

   private final int estimatorTicksPerControlTick;
   private final int estimatorFrequencyInHz;
   private final ArrayList<OneDoFJoint> joints = new ArrayList<>();
   private ByteBuffer jointCommand;

   // Since the finger joint controller doesn't set the OneDoFJoints used in this writer, this acts as an object communicator for finger joint angles
   private HashMap<String, OneDegreeOfFreedomJoint> fingerJointMap = null;

   public AtlasDRCSimGazeboOutputWriter(AtlasRobotModel robotModel)
   {
      estimatorTicksPerControlTick = (int) Math.round(robotModel.getControllerDT() / robotModel.getEstimatorDT());
      estimatorFrequencyInHz = (int) (1.0 / robotModel.getEstimatorDT());
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void writeAfterController(long timestamp)
   {
      jointCommand.clear();

      jointCommand.putLong(estimatorTicksPerControlTick);
      jointCommand.putLong(timestamp);
      jointCommand.putLong(estimatorFrequencyInHz);
      
      for (int i = 0; i < joints.size(); i++)
      {
         OneDoFJoint joint = joints.get(i);

         if (fingerJointMap == null || !fingerJointMap.containsKey(joint.getName()))
         {
            if (joint.isUnderPositionControl())
               jointCommand.putDouble(joint.getqDesired());               
            else
               jointCommand.putDouble(joint.getTau());
         }
         else
            jointCommand.putDouble(fingerJointMap.get(joint.getName()).getqDesired()); // fingers are always position controlled
      }

      jointCommand.flip();

      try
      {
         while (jointCommand.hasRemaining())
         {
            channel.write(jointCommand);
         }
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }

   public void setFingerJointsProvider(SideDependentList<List<OneDegreeOfFreedomJoint>> allFingerJoints)
   {
      fingerJointMap = new HashMap<String, OneDegreeOfFreedomJoint>();

      for (RobotSide robotSide : RobotSide.values)
      {
         for (OneDegreeOfFreedomJoint joint : allFingerJoints.get(robotSide))
         {
            fingerJointMap.put(joint.getName(), joint);
         }
      }
   }

   @Override
   public void setFullRobotModel(SDFFullRobotModel controllerModel, RawJointSensorDataHolderMap rawJointSensorDataHolderMap)
   {
      joints.addAll(Arrays.asList(controllerModel.getOneDoFJoints()));
      Collections.sort(joints, new Comparator<OneDoFJoint>()
      {

         @Override
         public int compare(OneDoFJoint o1, OneDoFJoint o2)
         {
            return o1.getName().compareTo(o2.getName());
         }
      });

      jointCommand = ByteBuffer.allocate(joints.size() * 8 + 24);
      jointCommand.order(ByteOrder.nativeOrder());

      try
      {
         channel = SocketChannel.open();
         channel.configureBlocking(true);
         channel.socket().setKeepAlive(true);
         channel.socket().setReuseAddress(true);
         channel.socket().setSoLinger(false, 0);
         channel.socket().setTcpNoDelay(true);

         System.out.println("[DRCSim] Connecting to " + address);
         channel.connect(address);
         System.out.println("[DRCSim] Connected");

         sendInitialState();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }

      System.out.println("num of joints = " + joints.size());
   }

   private void sendInitialState() throws IOException
   {
      jointCommand.clear();
      jointCommand.putLong(estimatorTicksPerControlTick * 3);
      jointCommand.putLong(0);
      for (int i = 0; i < joints.size(); i++)
      {
         jointCommand.putDouble(0.0);
      }
      jointCommand.flip();

      while (jointCommand.hasRemaining())
      {
         channel.write(jointCommand);
      }
   }

   @Override
   public void setForceSensorDataHolderForController(ForceSensorDataHolder forceSensorDataHolderForController)
   {

   }

   @Override
   public YoVariableRegistry getControllerYoVariableRegistry()
   {
      return registry;
   }

}
