package us.ihmc.ihmcPerception;

import org.ros.node.NodeConfiguration;

import us.ihmc.communication.packetCommunicator.PacketCommunicatorMock;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.StampedPosePacket;
import us.ihmc.utilities.kinematics.TimeStampedTransform3D;
import us.ihmc.utilities.ros.PPSTimestampOffsetProvider;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.subscriber.RosPoseStampedSubscriber;

public class RosLocalizationPoseCorrectionSubscriber
{
   private static final boolean DEBUG = false;

   NodeConfiguration nodeConfig = NodeConfiguration.newPrivate();

   public RosLocalizationPoseCorrectionSubscriber(final RosMainNode rosMainNode, final PacketCommunicatorMock controllerCommunicationBridge,
         final PPSTimestampOffsetProvider ppsTimeOffsetProvider)
   {

	   RosPoseStampedSubscriber rosPoseStampedSubscriber = new RosPoseStampedSubscriber()
	   {
		   @Override
		   protected void newPose(String frameID,
				   TimeStampedTransform3D timeStampedTransform)
		   {
			   long timestamp = timeStampedTransform.getTimeStamp();
			   timestamp = ppsTimeOffsetProvider.adjustTimeStampToRobotClock(timestamp);
			   timeStampedTransform.setTimeStamp(timestamp);

			   StampedPosePacket posePacket = new StampedPosePacket(frameID, timeStampedTransform, RosLocalizationConstants.DEFAULT_OVERLAP);
			   posePacket.setDestination(PacketDestination.CONTROLLER.ordinal());
			   if (DEBUG) System.out.println("Pose update received. \ntimestamp: " + timeStampedTransform.getTimeStamp());

			   controllerCommunicationBridge.send(posePacket);
		   }
	   };

	   rosMainNode.attachSubscriber(RosLocalizationConstants.POSE_UPDATE_TOPIC, rosPoseStampedSubscriber);
   }
}
