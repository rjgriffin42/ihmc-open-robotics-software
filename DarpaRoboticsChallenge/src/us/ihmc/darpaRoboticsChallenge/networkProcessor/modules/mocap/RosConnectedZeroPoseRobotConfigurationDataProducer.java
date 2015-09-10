package us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.mocap;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Subscriber;

import sensor_msgs.JointState;
import us.ihmc.SdfLoader.SDFFullHumanoidRobotModel;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.IMUPacket;
import us.ihmc.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.sensorProcessing.parameters.DRCRobotLidarParameters;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.robotics.sensors.IMUDefinition;
import us.ihmc.sensorProcessing.model.RobotMotionStatus;
import us.ihmc.robotics.sensors.ForceSensorDefinition;
import us.ihmc.SdfLoader.partNames.NeckJointName;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.utilities.ros.RosTools;

public class RosConnectedZeroPoseRobotConfigurationDataProducer extends AbstractNodeMain
{
   private final PacketCommunicator packetCommunicator;
   private Subscriber<JointState> jointSubscriber;

   private final DRCRobotModel robotModel;
   private final SDFFullHumanoidRobotModel fullRobotModel;
   private final ForceSensorDefinition[] forceSensorDefinitions;
   private final HumanoidReferenceFrames referenceFrames;
   private final ReferenceFrame pelvisFrame;
   private final ReferenceFrame headFrame;
   private final AtomicReference<RigidBodyTransform> atomicPelvisPose = new AtomicReference<RigidBodyTransform>(new RigidBodyTransform());
   private final Random random = new Random();
   
   public RosConnectedZeroPoseRobotConfigurationDataProducer(URI rosMasterURI, PacketCommunicator objectCommunicator, final DRCRobotModel robotModel)
   {
      this.robotModel = robotModel;
      this.packetCommunicator = objectCommunicator;
      fullRobotModel = robotModel.createFullRobotModel();
      referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      
      pelvisFrame = referenceFrames.getPelvisFrame();
      headFrame = referenceFrames.getNeckFrame(NeckJointName.LOWER_NECK_PITCH);
      
      forceSensorDefinitions = fullRobotModel.getForceSensorDefinitions();
      
      updateRobotLocationBasedOnMultisensePose(new RigidBodyTransform());

      if(rosMasterURI != null)
      {
         NodeConfiguration nodeConfiguration = RosTools.createNodeConfiguration(rosMasterURI);
         NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
         nodeMainExecutor.execute(this, nodeConfiguration);
      }
   }

   public void onStart(ConnectedNode connectedNode)
   {
      setupSubscribers(connectedNode);
      setUpListeners();
   }

   private void setUpListeners()
   {
	   if(jointSubscriber != null)
	   {
		   jointSubscriber.addMessageListener(new MessageListener<JointState>()
		   {
			   public void onNewMessage(JointState message)
			   {
				   receivedClockMessage(message.getHeader().getStamp().totalNsecs());
			   }
		   });
	   }
   }

   protected void setupSubscribers(ConnectedNode connectedNode)
   {
      DRCRobotSensorInformation sensorInforamtion = robotModel.getSensorInformation();
      DRCRobotLidarParameters lidarParameters = sensorInforamtion.getLidarParameters(0);
      if (lidarParameters != null && lidarParameters.getLidarSpindleJointTopic() != null)
      {
         jointSubscriber = connectedNode.newSubscriber(lidarParameters.getLidarSpindleJointTopic(), sensor_msgs.JointState._TYPE);
      }
   }

   public void receivedClockMessage(long totalNsecs)
   {
      RigidBodyTransform pelvisPoseInMocapFrame = atomicPelvisPose.get();
      IMUDefinition[] imuDefinitions = fullRobotModel.getIMUDefinitions();
      RobotConfigurationData robotConfigurationData = new RobotConfigurationData(fullRobotModel.getOneDoFJoints(), forceSensorDefinitions, null, imuDefinitions);

      for(int sensorNumber = 0; sensorNumber <  imuDefinitions.length; sensorNumber++)
      {
         IMUPacket imuPacket = robotConfigurationData.getImuPacketForSensor(sensorNumber);
         imuPacket.set(RandomTools.generateRandomVector3f(random), RandomTools.generateRandomQuaternion4f(random), RandomTools.generateRandomVector3f(random));
      }
      
      robotConfigurationData.setRobotMotionStatus(RobotMotionStatus.STANDING);
      
      robotConfigurationData.setTimestamp(totalNsecs);
      if(pelvisPoseInMocapFrame != null)
      {
         Vector3d translation = new Vector3d();
         Quat4d orientation = new Quat4d();
         pelvisPoseInMocapFrame.getTranslation(translation);
         pelvisPoseInMocapFrame.getRotation(orientation);
         robotConfigurationData.setRootTranslation(translation);
         robotConfigurationData.setRootOrientation(orientation);
      }
      fullRobotModel.updateFrames();
      packetCommunicator.send(robotConfigurationData);
   }

   public GraphName getDefaultNodeName()
   {
      return GraphName.of("darpaRoboticsChallenge/RosConnectedZeroPoseRobotConfigurationDataProducer");
   }

   public void updateRobotLocationBasedOnMultisensePose(RigidBodyTransform headPose)
   {
      RigidBodyTransform pelvisPose = new RigidBodyTransform();
      RigidBodyTransform transformFromHeadToPelvis = pelvisFrame.getTransformToDesiredFrame(headFrame);
      pelvisPose.multiply(headPose, transformFromHeadToPelvis);
      atomicPelvisPose.set(pelvisPose);
   }

   public HumanoidReferenceFrames getReferenceFrames()
   {
      return referenceFrames;
   }
}
