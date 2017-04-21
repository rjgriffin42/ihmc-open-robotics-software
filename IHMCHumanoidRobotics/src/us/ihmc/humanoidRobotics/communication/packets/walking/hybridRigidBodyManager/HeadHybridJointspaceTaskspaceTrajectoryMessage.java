package us.ihmc.humanoidRobotics.communication.packets.walking.hybridRigidBodyManager;

import java.util.Random;

import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.QueueableMessage;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.humanoidRobotics.communication.packets.FrameBasedMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.HeadTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.NeckTrajectoryMessage;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

@RosMessagePacket(documentation =
      "This message commands the controller to move the chest in both taskspace amd jointspace to the desired orientation and joint angles while going through the specified trajectory points.",
                  rosPackage = RosMessagePacket.CORE_IHMC_PACKAGE,
                  topic = "/control/hybrid_head_trajectory")
public class HeadHybridJointspaceTaskspaceTrajectoryMessage extends QueueableMessage<HeadHybridJointspaceTaskspaceTrajectoryMessage>  implements VisualizablePacket, FrameBasedMessage
{
   public HeadTrajectoryMessage headTrajectoryMessage; 
   public NeckTrajectoryMessage neckTrajectoryMessage;
   
   /**
    * Empty constructor for serialization.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    */
   public HeadHybridJointspaceTaskspaceTrajectoryMessage()
   {
      super();
   }

   /**
    * Random constructor for unit testing this packet
    * @param random seed
    */
   public HeadHybridJointspaceTaskspaceTrajectoryMessage(Random random)
   {
      this(new HeadTrajectoryMessage(random), new NeckTrajectoryMessage(random));
   }

   /**
    * Clone constructor.
    * @param message to clone.
    */
   public HeadHybridJointspaceTaskspaceTrajectoryMessage(HeadHybridJointspaceTaskspaceTrajectoryMessage hybridJointspaceTaskspaceMessage)
   {
      this(hybridJointspaceTaskspaceMessage.getHeadTrajectoryMessage(), hybridJointspaceTaskspaceMessage.getNeckTrajectoryMessage());
   }
   
   /**
    * Typical constructor to use, pack the two taskspace and joint space commands.
    * If these messages conflict, the qp weights and gains will dictate the desireds
    * @param headTrajectoryMessage
    * @param neckTrajectoryMessage
    */
   public HeadHybridJointspaceTaskspaceTrajectoryMessage(HeadTrajectoryMessage headTrajectoryMessage, NeckTrajectoryMessage neckTrajectoryMessage)
   {
      this.headTrajectoryMessage = headTrajectoryMessage;
      this.neckTrajectoryMessage = neckTrajectoryMessage;
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public HeadTrajectoryMessage getHeadTrajectoryMessage()
   {
      return headTrajectoryMessage;
   }

   public void setHeadTrajectoryMessage(HeadTrajectoryMessage headTrajectoryMessage)
   {
      this.headTrajectoryMessage = headTrajectoryMessage;
   }

   public NeckTrajectoryMessage getNeckTrajectoryMessage()
   {
      return neckTrajectoryMessage;
   }

   public void setNeckTrajectoryMessage(NeckTrajectoryMessage neckTrajectoryMessage)
   {
      this.neckTrajectoryMessage = neckTrajectoryMessage;
   }
   
   public void setTrajectoryReferenceFrameId(long trajedtoryReferenceFrameId)
   {
      headTrajectoryMessage.setTrajectoryReferenceFrameId(trajedtoryReferenceFrameId);
   }

   public void setTrajectoryReferenceFrameId(ReferenceFrame trajectoryReferenceFrame)
   {
      headTrajectoryMessage.setTrajectoryReferenceFrameId(trajectoryReferenceFrame);
   }

   public void setDataReferenceFrameId(long expressedInReferenceFrameId)
   {
      headTrajectoryMessage.setDataReferenceFrameId(expressedInReferenceFrameId);
   }

   public void setDataReferenceFrameId(ReferenceFrame expressedInReferenceFrame)
   {
      headTrajectoryMessage.setDataReferenceFrameId(expressedInReferenceFrame);
   }
}
