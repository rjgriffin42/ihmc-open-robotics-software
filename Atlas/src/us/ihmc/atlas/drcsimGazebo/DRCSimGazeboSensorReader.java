package us.ihmc.atlas.drcsimGazebo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.communication.packets.dataobjects.AuxiliaryRobotData;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensorProcessors.SensorProcessing;
import us.ihmc.sensorProcessing.sensorProcessors.SensorRawOutputMapReadOnly;
import us.ihmc.sensorProcessing.sensors.RawJointSensorDataHolderMap;
import us.ihmc.sensorProcessing.simulatedSensors.SensorReader;
import us.ihmc.sensorProcessing.simulatedSensors.StateEstimatorSensorDefinitions;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.utilities.IMUDefinition;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorData;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDataHolder;
import us.ihmc.utilities.humanoidRobot.model.ForceSensorDefinition;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.LongYoVariable;

public class DRCSimGazeboSensorReader implements SensorReader
{
   private final YoVariableRegistry registry = new YoVariableRegistry("DRCSimSensorReader");

   private final SocketAddress address = new InetSocketAddress("127.0.0.1", 1234);
   private final ByteBuffer data;

   private final LongYoVariable delay = new LongYoVariable("delay", registry);
   private final ForceSensorDataHolder forceSensorDataHolderForEstimator;
   private final SensorProcessing sensorProcessing;
   private final List<OneDoFJoint> jointList;

   private final IMUDefinition imu;

   private final SocketChannel channel;

   private final int jointDataLength;
   private final int imuDataLength;
   private final int forceSensorDataLength;
   
   private final Quat4d orientation = new Quat4d();
   private final Vector3d linearAcceleration = new Vector3d();
   private final Vector3d angularVelocity = new Vector3d();
   private final DenseMatrix64F wrench = new DenseMatrix64F(6, 1);

   public DRCSimGazeboSensorReader(StateEstimatorSensorDefinitions stateEstimatorSensorDefinitions, DRCRobotSensorInformation sensorInformation,
         StateEstimatorParameters stateEstimatorParameters, ForceSensorDataHolder forceSensorDataHolderForEstimator,
         RawJointSensorDataHolderMap rawJointSensorDataHolderMap, YoVariableRegistry parentRegistry)
   {
      this.sensorProcessing = new SensorProcessing(stateEstimatorSensorDefinitions, stateEstimatorParameters, registry);

      this.jointList = new ArrayList<>(stateEstimatorSensorDefinitions.getJointSensorDefinitions());
      Collections.sort(jointList, new Comparator<OneDoFJoint>()
      {
         @Override
         public int compare(OneDoFJoint o1, OneDoFJoint o2)
         {
            return o1.getName().compareTo(o2.getName());
         }
      });;
      this.imu = stateEstimatorSensorDefinitions.getIMUSensorDefinitions().get(0);
      this.forceSensorDataHolderForEstimator = forceSensorDataHolderForEstimator;

      

      jointDataLength = jointList.size() * 8 * 2;
      imuDataLength = 10 * 8;
      forceSensorDataLength = forceSensorDataHolderForEstimator.getForceSensorDefinitions().size() * 6 * 8;
      data = ByteBuffer.allocate(16 + jointDataLength + imuDataLength + forceSensorDataLength);
      data.order(ByteOrder.nativeOrder());

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
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      parentRegistry.addChild(registry);
   }

   @Override
   public void read()
   {
      try
      {
         data.clear();
         while(data.position() < data.limit())
         {
            channel.read(data);
         }
         data.flip();

         long timestamp = data.getLong();
         long controlTimestamp = data.getLong();
         
         delay.set(timestamp - controlTimestamp);
         for (int i = 0; i < jointList.size(); i++)
         {
            OneDoFJoint joint = jointList.get(i);
            sensorProcessing.setJointPositionSensorValue(joint, data.getDouble());
            sensorProcessing.setJointVelocitySensorValue(joint, data.getDouble());
         }

         orientation.setW(data.getDouble());
         orientation.setX(data.getDouble());
         orientation.setY(data.getDouble());
         orientation.setZ(data.getDouble());
         
         angularVelocity.setX(data.getDouble());
         angularVelocity.setY(data.getDouble());
         angularVelocity.setZ(data.getDouble());
         

         linearAcceleration.setX(data.getDouble());
         linearAcceleration.setY(data.getDouble());
         linearAcceleration.setZ(data.getDouble());

         sensorProcessing.setOrientationSensorValue(imu, orientation);
         sensorProcessing.setLinearAccelerationSensorValue(imu, linearAcceleration);
         sensorProcessing.setAngularVelocitySensorValue(imu, angularVelocity);

         for (int i = 0; i < forceSensorDataHolderForEstimator.getForceSensorDefinitions().size(); i++)
         {
            ForceSensorDefinition definition = forceSensorDataHolderForEstimator.getForceSensorDefinitions().get(i);
            ForceSensorData dataHolder = forceSensorDataHolderForEstimator.get(definition);

            wrench.set(0, 0, data.getDouble());
            wrench.set(1, 0, data.getDouble());
            wrench.set(2, 0, data.getDouble());
            wrench.set(3, 0, data.getDouble());
            wrench.set(4, 0, data.getDouble());
            wrench.set(5, 0, data.getDouble());
            dataHolder.setWrench(wrench);
         }

         sensorProcessing.startComputation(timestamp, timestamp, -1);

      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }

   }

   @Override
   public SensorOutputMapReadOnly getSensorOutputMapReadOnly()
   {
      return sensorProcessing;
   }

   @Override
   public SensorRawOutputMapReadOnly getSensorRawOutputMapReadOnly()
   {
      return sensorProcessing;
   }

   @Override public AuxiliaryRobotData newAuxiliaryRobotDataInstance()
   {
      return null;
   }
}
