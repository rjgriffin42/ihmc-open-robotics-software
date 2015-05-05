package us.ihmc.darpaRoboticsChallenge.networkProcessor.camera;

import us.ihmc.SdfLoader.SDFFullRobotModelFactory;
import us.ihmc.communication.net.ObjectCommunicator;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.LocalVideoPacket;
import us.ihmc.communication.packets.sensing.CropVideoPacket;
import us.ihmc.communication.producers.RobotConfigurationDataBuffer;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.ros.PPSTimestampOffsetProvider;

/**
 * 
 *  Generate simulated camera data and camera info packet from SCS, we use only left eye.
 */
public class SCSCameraDataReceiver extends CameraDataReceiver implements ObjectConsumer<LocalVideoPacket>
{
   
   private final RobotSide robotSide;
   
   public SCSCameraDataReceiver(RobotSide robotSide, SDFFullRobotModelFactory fullRobotModelFactory, String sensorNameInSdf, RobotConfigurationDataBuffer robotConfigurationDataBuffer, ObjectCommunicator scsSensorsCommunicator,
         PacketCommunicator sensorSuitePacketCommunicator, PPSTimestampOffsetProvider ppsTimestampOffsetProvider)
   {
      super(fullRobotModelFactory, sensorNameInSdf, robotConfigurationDataBuffer, new VideoPacketHandler(sensorSuitePacketCommunicator), ppsTimestampOffsetProvider);
      sensorSuitePacketCommunicator.attachListener(CropVideoPacket.class, this);
      
      this.robotSide = robotSide;

      scsSensorsCommunicator.attachListener(LocalVideoPacket.class, this);

      CameraLogger logger = DRCConfigParameters.LOG_PRIMARY_CAMERA_IMAGES ? new CameraLogger("left") : null;

   }

   public void consumeObject(LocalVideoPacket object)
   {
      if (DEBUG)
      {
         System.out.println(getClass().getName() + ": received local video packet!");
      }
      updateImage(robotSide, object.getImage(), object.getTimeStamp(), object.getIntrinsicParameters());
   }
}
