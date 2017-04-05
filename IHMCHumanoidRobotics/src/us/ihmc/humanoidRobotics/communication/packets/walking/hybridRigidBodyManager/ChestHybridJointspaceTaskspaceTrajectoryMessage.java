package us.ihmc.humanoidRobotics.communication.packets.walking.hybridRigidBodyManager;

import java.util.Random;

import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.QueueableMessage;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidRobotics.communication.packets.FrameBasedMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.SpineTrajectoryMessage;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

@RosMessagePacket(documentation =
      "This message commands the controller to move the chest in both taskspace amd jointspace to the desired orientation and joint angles while going through the specified trajectory points.",
                  rosPackage = RosMessagePacket.CORE_IHMC_PACKAGE,
                  topic = "/control/hybrid_chest_trajectory")
public class ChestHybridJointspaceTaskspaceTrajectoryMessage extends QueueableMessage<ChestHybridJointspaceTaskspaceTrajectoryMessage> implements VisualizablePacket, FrameBasedMessage
{
   
   private ChestTrajectoryMessage chestTrajectoryMessage; 
   private SpineTrajectoryMessage spineTrajectoryMessage;
   
   /**
    * Empty constructor for serialization.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    */
   public ChestHybridJointspaceTaskspaceTrajectoryMessage()
   {
      super();
   }

   /**
    * Random constructor for unit testing this packet
    * @param random seed
    */
   public ChestHybridJointspaceTaskspaceTrajectoryMessage(Random random)
   {
      this(new ChestTrajectoryMessage(random), new SpineTrajectoryMessage(random));
   }

   /**
    * Clone constructor.
    * @param message to clone.
    */
   public ChestHybridJointspaceTaskspaceTrajectoryMessage(ChestHybridJointspaceTaskspaceTrajectoryMessage chestHybridJointspaceTaskspaceMessage)
   {
      this(chestHybridJointspaceTaskspaceMessage.getChestTrajectoryMessage(), chestHybridJointspaceTaskspaceMessage.getSpineTrajectoryMessage());
   }
   
   /**
    * Typical constructor to use, pack the two taskspace and joint space commands.
    * If these messages conflict, the qp weights and gains will dictate the desireds
    * @param chestTrajectoryMessage
    * @param spineTrajectoryMessage
    */
   public ChestHybridJointspaceTaskspaceTrajectoryMessage(ChestTrajectoryMessage chestTrajectoryMessage, SpineTrajectoryMessage spineTrajectoryMessage)
   {
      this.chestTrajectoryMessage = chestTrajectoryMessage;
      this.spineTrajectoryMessage = spineTrajectoryMessage;
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public ChestTrajectoryMessage getChestTrajectoryMessage()
   {
      return chestTrajectoryMessage;
   }

   public void setChestTrajectoryMessage(ChestTrajectoryMessage chestTrajectoryMessage)
   {
      this.chestTrajectoryMessage = chestTrajectoryMessage;
   }

   public SpineTrajectoryMessage getSpineTrajectoryMessage()
   {
      return spineTrajectoryMessage;
   }

   public void setSpineTrajectoryMessage(SpineTrajectoryMessage spineTrajectoryMessage)
   {
      this.spineTrajectoryMessage = spineTrajectoryMessage;
   }

   @Override
   public long getTrajectoryReferenceFrameId()
   {
      return chestTrajectoryMessage.getTrajectoryReferenceFrameId();
   }

   @Override
   public long getDataReferenceFrameId()
   {
      return chestTrajectoryMessage.getDataReferenceFrameId();
   }

   @Override
   public void setTrajectoryReferenceFrameId(long trajedtoryReferenceFrameId)
   {
      chestTrajectoryMessage.setTrajectoryReferenceFrameId(trajedtoryReferenceFrameId);
   }

   @Override
   public void setTrajectoryReferenceFrameId(ReferenceFrame trajectoryReferenceFrame)
   {
      chestTrajectoryMessage.setTrajectoryReferenceFrameId(trajectoryReferenceFrame);
   }

   @Override
   public void setDataReferenceFrameId(long expressedInReferenceFrameId)
   {
      chestTrajectoryMessage.setDataReferenceFrameId(expressedInReferenceFrameId);
   }

   @Override
   public void setDataReferenceFrameId(ReferenceFrame expressedInReferenceFrame)
   {
      chestTrajectoryMessage.setDataReferenceFrameId(expressedInReferenceFrame);
   }

   @Override
   public Point3D getControlFramePosition()
   {
      return chestTrajectoryMessage.getControlFramePosition();
   }

   @Override
   public Quaternion getControlFrameOrientation()
   {
      return chestTrajectoryMessage.getControlFrameOrientation();
   }
}
