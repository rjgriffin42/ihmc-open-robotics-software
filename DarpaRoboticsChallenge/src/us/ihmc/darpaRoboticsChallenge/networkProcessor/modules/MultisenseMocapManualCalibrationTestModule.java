package us.ihmc.darpaRoboticsChallenge.networkProcessor.modules;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import optitrack.IHMCMocapDataClient;
import optitrack.MocapRigidBody;
import optitrack.MocapRigidbodiesListener;
import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.net.NetClassList;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.DetectedObjectPacket;
import us.ihmc.communication.packets.sensing.MultisenseTest;
import us.ihmc.communication.util.NetworkPorts;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.mocap.MocapToStateEstimatorFrameConverter;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.mocap.RosConnectedZeroPoseRobotConfigurationDataProducer;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.ros.RosMainNode;

public class MultisenseMocapManualCalibrationTestModule implements MocapRigidbodiesListener
{
   private static final boolean ENABLE_ZERO_POSE_CONFIGURATION_PUBLISHER = false;
   private static final boolean USE_ROBOT_FRAME = true;
   
   private static final int MULTISENSE_MOCAP_ID = 0;
   private static final NetClassList NETCLASSLIST = new IHMCCommunicationKryoNetClassList();

   private final PacketCommunicator packetCommunicator = PacketCommunicator.createIntraprocessPacketCommunicator(NetworkPorts.MULTISENSE_MOCAP_MANUAL_CALIBRATION_TEST_MODULE, NETCLASSLIST);
   private final ArrayList<MultisensePointCloudReceiver> multisensePointCloudReceivers = new ArrayList<MultisensePointCloudReceiver>();
   private final RosConnectedZeroPoseRobotConfigurationDataProducer zeroPoseProducer;
   private final MocapToStateEstimatorFrameConverter mocapToStateEstimatorFrameConverter;
   
   private final IHMCMocapDataClient mocapDataClient = new IHMCMocapDataClient();
   
   private final RigidBodyTransform mocapCalibrationTransform = new RigidBodyTransform();
   private static final RigidBodyTransform transformFromMocapCentroidToHeadRoot = new RigidBodyTransform();
   static 
   {
      transformFromMocapCentroidToHeadRoot.setTranslation(0, -0.035, 0.002);
   }
   
   /**
    * Used to inspect the lidar at each stage on the route from multisense to the DRCOperatorInterface
    */
   public MultisenseMocapManualCalibrationTestModule(DRCRobotModel robotModel, URI rosMasterURI)
   {
      RosMainNode rosMainNode = new RosMainNode(rosMasterURI, "atlas/MultisenseMocapManualCalibrationTest", true);
      mocapToStateEstimatorFrameConverter = new MocapToStateEstimatorFrameConverter(robotModel, packetCommunicator);
      
      if (ENABLE_ZERO_POSE_CONFIGURATION_PUBLISHER)
      {
         zeroPoseProducer = new RosConnectedZeroPoseRobotConfigurationDataProducer(rosMasterURI, packetCommunicator, robotModel);
      } else {
         zeroPoseProducer = null;
      }
      
      mocapToStateEstimatorFrameConverter.setTransformFromMocapCentroidToHeadRoot(transformFromMocapCentroidToHeadRoot);
      
      // for each of the MultisenseTests send a point cloud to the ui
      for (MultisenseTest topicToTest : MultisenseTest.values())
      {
         if(topicToTest != MultisenseTest.NEAR_SCAN_IN_POINT_CLOUD_DATA_RECEIVER)
         {
            MultisensePointCloudReceiver lidarReceiver = new MultisensePointCloudReceiver(packetCommunicator, topicToTest, robotModel);
            rosMainNode.attachSubscriber(topicToTest.getRosTopic(), lidarReceiver);
            multisensePointCloudReceivers.add(lidarReceiver);
         }
      }
      
      mocapDataClient.registerRigidBodiesListener(this);
      rosMainNode.execute();
      connect();
   }

	private void connect() {
		try
	      {
	         packetCommunicator.connect();
	      }
	      catch (IOException e)
	      {
	         e.printStackTrace();
	      }
	}

   /**
    * received an update from mocap
    * convert the mocap head pose to zUpFrame
    * send the pose to the multisense lidar receivers
    */
   @Override
   public void updateRigidbodies(ArrayList<MocapRigidBody> listOfRigidbodies)
   {
      for (int rigidBodyIndex = 0; rigidBodyIndex < listOfRigidbodies.size(); rigidBodyIndex++)
      {
         MocapRigidBody mocapObject = listOfRigidbodies.get(rigidBodyIndex);
         RigidBodyTransform pose = new RigidBodyTransform();
         mocapObject.getPose(pose);
         int id = mocapObject.getId();

         if (id == MULTISENSE_MOCAP_ID)
         {
            //Set this to false to pause mocap updates. Remember to turn it back on before restarting
            boolean enableMocapUpdates = true;
            mocapToStateEstimatorFrameConverter.enableMocapUpdates(enableMocapUpdates);
            
            //manual calibration offsets from mocap jig misalignment
            //had to make pitch and roll negative to match frames of ui tool
            mocapCalibrationTransform.setEuler(Math.toRadians(0.0), Math.toRadians(0.0), Math.toRadians(0.0));
            mocapToStateEstimatorFrameConverter.setMocapJigCalibrationTransform(mocapCalibrationTransform);
            mocapToStateEstimatorFrameConverter.update(mocapObject);
            
            RigidBodyTransform poseInStateEstimatorFrame = new RigidBodyTransform(pose);
            if(USE_ROBOT_FRAME)
            {
               mocapToStateEstimatorFrameConverter.convertMocapPoseToRobotFrame(poseInStateEstimatorFrame);
            }
            if (zeroPoseProducer != null)
            {
               zeroPoseProducer.updateRobotLocationBasedOnMultisensePose(poseInStateEstimatorFrame);
            }
            
            updateReceiversWithMocapPose(poseInStateEstimatorFrame);
         }
         sendDetectedObjectPacketToUi(pose, id);
      }
   }

   /**
    * @param headPose the location of the multisense in world
    */
   private void updateReceiversWithMocapPose(RigidBodyTransform headPose)
   {
 	  for (int i = 0; i < multisensePointCloudReceivers.size(); i++)
 	  {
 		  MultisensePointCloudReceiver pointCloudReceiver = multisensePointCloudReceivers.get(i);
		  pointCloudReceiver.setHeadRootInWorldFromMocap(headPose);
      }
   }
   
   private void sendDetectedObjectPacketToUi(RigidBodyTransform pose, int id)
   {
      if(USE_ROBOT_FRAME)
      {
         mocapToStateEstimatorFrameConverter.convertMocapPoseToRobotFrame(pose);
      }
      DetectedObjectPacket detectedMocapObject = new DetectedObjectPacket(pose, id);
      if(packetCommunicator.isConnected())
      {
         packetCommunicator.send(detectedMocapObject);
      }
   }
}


