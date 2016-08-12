package us.ihmc.darpaRoboticsChallenge.sensors.multisense;

import java.net.URI;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFFullHumanoidRobotModelFactory;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.IMUPacket;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.humanoidRobotics.kryo.PPSTimestampOffsetProvider;
import us.ihmc.ihmcPerception.camera.CameraDataReceiver;
import us.ihmc.ihmcPerception.camera.CameraLogger;
import us.ihmc.ihmcPerception.camera.RosCameraCompressedImageReceiver;
import us.ihmc.ihmcPerception.camera.VideoPacketHandler;
import us.ihmc.ihmcPerception.depthData.PointCloudDataReceiver;
import us.ihmc.ihmcPerception.depthData.PointCloudSource;
import us.ihmc.ihmcPerception.depthData.RosPointCloudReceiver;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.time.TimeTools;
import us.ihmc.sensorProcessing.communication.producers.RobotConfigurationDataBuffer;
import us.ihmc.sensorProcessing.parameters.DRCRobotCameraParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotLidarParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotPointCloudParameters;
import us.ihmc.sensorProcessing.sensorData.DRCStereoListener;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.subscriber.RosImuSubscriber;

public class MultiSenseSensorManager
{
   private CameraDataReceiver cameraReceiver;

   private final SDFFullHumanoidRobotModelFactory fullRobotModelFactory;
   private final RobotConfigurationDataBuffer robotConfigurationDataBuffer;
   private final RosMainNode rosMainNode;
   private final PacketCommunicator packetCommunicator;
   private final PPSTimestampOffsetProvider ppsTimestampOffsetProvider;


   private final URI sensorURI;

   private final DRCRobotCameraParameters cameraParamaters;
   private final DRCRobotLidarParameters lidarParamaters;

   private final PointCloudDataReceiver pointCloudDataReceiver;
   private MultiSenseParamaterSetter multiSenseParamaterSetter;

   public MultiSenseSensorManager(SDFFullHumanoidRobotModelFactory sdfFullRobotModelFactory, PointCloudDataReceiver pointCloudDataReceiver, RobotConfigurationDataBuffer robotConfigurationDataBuffer,
         RosMainNode rosMainNode, PacketCommunicator sensorSuitePacketCommunicator, PPSTimestampOffsetProvider ppsTimestampOffsetProvider, URI sensorURI, DRCRobotCameraParameters cameraParamaters,
         DRCRobotLidarParameters lidarParamaters, DRCRobotPointCloudParameters stereoParamaters, boolean setROSParameters)
   {
      this.fullRobotModelFactory = sdfFullRobotModelFactory;
      this.pointCloudDataReceiver = pointCloudDataReceiver;
      this.robotConfigurationDataBuffer = robotConfigurationDataBuffer;
      this.lidarParamaters = lidarParamaters;
      this.cameraParamaters = cameraParamaters;
      this.rosMainNode = rosMainNode;
      this.packetCommunicator = sensorSuitePacketCommunicator;
      this.ppsTimestampOffsetProvider = ppsTimestampOffsetProvider;
      this.sensorURI = sensorURI;
      registerCameraReceivers();
      registerLidarReceivers(sdfFullRobotModelFactory);
      registerIMUReciever();
      if(setROSParameters)
      {
         multiSenseParamaterSetter = new MultiSenseParamaterSetter(rosMainNode, sensorSuitePacketCommunicator);
         setMultiseSenseParams(lidarParamaters.getLidarSpindleVelocity());
      }
      else
      {
         multiSenseParamaterSetter = null;
      }
   }

   public void initializeParameterListeners()
   {

      System.out.println("initialise parameteres--------------------------------------------------------------------------------");
      if(multiSenseParamaterSetter != null)
      {
         multiSenseParamaterSetter.initializeParameterListeners();
//         multiSenseParamaterSetter.setLidarSpindleSpeed(lidarParamaters.getLidarSpindleVelocity());
      }
   }

   private void setMultiseSenseParams(double lidarSpindleVelocity)
   {
      if(multiSenseParamaterSetter != null)
      {
         multiSenseParamaterSetter.setMultisenseResolution(rosMainNode);

         multiSenseParamaterSetter.setupNativeROSCommunicator(lidarSpindleVelocity);
      }
   }

   private void registerLidarReceivers(SDFFullHumanoidRobotModelFactory sdfFullRobotModelFactory)
   {
//      new RosPointCloudReceiver(lidarParamaters.getSensorNameInSdf(), lidarParamaters.getRosTopic(), rosMainNode, ReferenceFrame.getWorldFrame(), pointCloudDataReceiver,PointCloudSource.NEARSCAN);
//      new RosPointCloudReceiver(lidarParamaters.getSensorNameInSdf(), lidarParamaters.getGroundCloudTopic(), rosMainNode, ReferenceFrame.getWorldFrame(), pointCloudDataReceiver,PointCloudSource.QUADTREE);

      SDFFullRobotModel sdfFullRobotModel = sdfFullRobotModelFactory.createFullRobotModel();

      new RosPointCloudReceiver(lidarParamaters.getRosTopic(), rosMainNode, ReferenceFrame.getWorldFrame(), sdfFullRobotModel.getLidarBaseFrame(lidarParamaters.getSensorNameInSdf()),
            pointCloudDataReceiver, PointCloudSource.NEARSCAN);

      new RosPointCloudReceiver(lidarParamaters.getGroundCloudTopic(), rosMainNode, ReferenceFrame.getWorldFrame(), sdfFullRobotModel.getLidarBaseFrame(lidarParamaters.getSensorNameInSdf()),
            pointCloudDataReceiver, PointCloudSource.QUADTREE);
   }

   private void registerCameraReceivers()
   {
      CameraLogger logger = DRCConfigParameters.LOG_PRIMARY_CAMERA_IMAGES ? new CameraLogger("left") : null;
      cameraReceiver = new CameraDataReceiver(fullRobotModelFactory, cameraParamaters.getPoseFrameForSdf(), robotConfigurationDataBuffer, new VideoPacketHandler(packetCommunicator),
            ppsTimestampOffsetProvider);

      new RosCameraCompressedImageReceiver(cameraParamaters, rosMainNode, logger, cameraReceiver);

      cameraReceiver.start();
   }

   private void registerIMUReciever()
   {
      rosMainNode.attachSubscriber("/multisense/imu/imu_data", new RosImuSubscriber()
      {
         @Override
         protected void onNewMessage(long timeStamp, int seqId, Quat4d orientation, Vector3d angularVelocity, Vector3d linearAcceleration)
         {
            long robotTimeStamp = ppsTimestampOffsetProvider.adjustTimeStampToRobotClock(timeStamp);

            IMUPacket imuPacket = new IMUPacket();
            imuPacket.linearAcceleration.set(linearAcceleration);
            imuPacket.orientation.set(orientation);
            imuPacket.angularVelocity.set(angularVelocity);
            imuPacket.time = TimeTools.nanoSecondstoSeconds(robotTimeStamp);
            imuPacket.setDestination(PacketDestination.CONTROLLER);

            packetCommunicator.send(imuPacket);
         }
      });
   }

   public void registerCameraListener(DRCStereoListener drcStereoListener)
   {
      cameraReceiver.registerCameraListener(drcStereoListener);

   }
}
