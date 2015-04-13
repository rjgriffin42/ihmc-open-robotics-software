package us.ihmc.utilities.ros.publisher;

import ihmc_msgs.BatchRawImuData;
import ihmc_msgs.RawImuData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ros.message.Time;

import std_msgs.Header;
import us.ihmc.communication.packets.dataobjects.IMUPacket;
import us.ihmc.utilities.math.TimeTools;
import us.ihmc.utilities.math.geometry.RotationFunctions;

public class RosCachedRawIMUDataPublisher extends RosTopicPublisher<BatchRawImuData>
{
   private int rosPacketSendCounter=0;
   private long rawImuCounter = 0;
   private final double[] currentImuOrientation = new double[3];
   private final double[] previousImuOrientation = new double[3];
   private final String frameId;
   
   private ArrayList<RawImuData> availableImuData = new ArrayList<RawImuData>();
   
   
   public RosCachedRawIMUDataPublisher(boolean latched, String frameId)
   {
      super(ihmc_msgs.BatchRawImuData._TYPE,latched);
      this.frameId = frameId;
   }
   
   public void publish(long timestamp, List<RawImuData> imuData)
   {
      ihmc_msgs.BatchRawImuData message = getMessage();

      Header header = newMessageFromType(Header._TYPE);
      header.setFrameId(frameId);
      header.setStamp(Time.fromNano(timestamp));
      header.setSeq(rosPacketSendCounter);
      rosPacketSendCounter++;
            
      message.setHeader(header);
      message.setData(imuData);
      
      publish(message);
   }
   
   /**
    * @param timestampInNanoSeconds
    * @param rawImuAngularVelocity
    * @param linearAccele
    */
   public synchronized void appendRawImuData(long timestampInNanoSeconds, Quat4d orientation, Vector3d linearAcceleration)
   {
      RawImuData rawImuData = newMessageFromType(RawImuData._TYPE);
      RotationFunctions.setYawPitchRollBasedOnQuaternion(currentImuOrientation, orientation);
      rawImuData.setImuTimestamp(TimeTools.nanoSecondsToMicroseconds(timestampInNanoSeconds));
      //delta angle (radians) in the frame of the IMU
      rawImuData.setDax(currentImuOrientation[2] - previousImuOrientation[2]);
      rawImuData.setDay(currentImuOrientation[1] - previousImuOrientation[1]);
      rawImuData.setDaz(currentImuOrientation[0] - previousImuOrientation[0]);
      //linear acceleration (m/s^2) in the frame of the IM
      rawImuData.setDdx(linearAcceleration.getX());
      rawImuData.setDdy(linearAcceleration.getY());
      rawImuData.setDdz(linearAcceleration.getZ());
      rawImuData.setPacketCount(rawImuCounter);
      rawImuCounter++;
      availableImuData.add(rawImuData);
      if(availableImuData.size() == 15)
      {
         ArrayList<RawImuData> imuBatch = new ArrayList<RawImuData>();
         for(int i = 0; i < 15; i++)
         {
            imuBatch.add(availableImuData.get(14 - i));
         }
         publish(timestampInNanoSeconds, imuBatch);
         availableImuData.remove(0);
      }
      
      for (int i = 0; i < currentImuOrientation.length; i++)
      {
         previousImuOrientation[i] = currentImuOrientation[i];
      }
   }
   
   public void appendRawImuData(long timeStamp, IMUPacket imuPacket)
   {
      appendRawImuData(timeStamp, imuPacket.getOrientation(), imuPacket.getLinearAcceleration());
   } 
   
   public ArrayList<RawImuData> createRandomRawImuData(Random random, int size)
   {
      long timestampInNanoSeconds = random.nextLong();
      ArrayList<RawImuData> imuBatch = new ArrayList<RawImuData>();
      int randomImuCounter = random.nextInt();
      for(int i = 0; i < size; i++)
      {
         RawImuData rawImuData = newMessageFromType(RawImuData._TYPE);
         rawImuData.setImuTimestamp(TimeTools.nanoSecondsToMicroseconds(timestampInNanoSeconds));
         rawImuData.setPacketCount(randomImuCounter);
         rawImuData.setDax(random.nextInt());
         rawImuData.setDay(random.nextInt());
         rawImuData.setDaz(random.nextInt());
         //linear acceleration (m/s^2) in the frame of the IM
         rawImuData.setDdx(random.nextInt());
         rawImuData.setDdy(random.nextInt());
         rawImuData.setDdz(random.nextInt());
         randomImuCounter++;
         imuBatch.add(rawImuData);
      }
      return imuBatch;
   }
}