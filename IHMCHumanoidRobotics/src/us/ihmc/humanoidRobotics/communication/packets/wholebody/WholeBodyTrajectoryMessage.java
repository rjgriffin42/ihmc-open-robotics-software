package us.ihmc.humanoidRobotics.communication.packets.wholebody;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.communication.annotations.ros.RosMessagePacket;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.packets.MultiplePacketHolder;
import us.ihmc.communication.packets.VisualizablePacket;
import us.ihmc.humanoidRobotics.communication.TransformableDataObject;
import us.ihmc.humanoidRobotics.communication.packets.PacketValidityChecker;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.ArmTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.robotSide.RobotSide;

@RosMessagePacket(documentation = "Send whole body trajectories to the robot. A best effort is made to execute the trajectory while balance is kept.\n"
      + " A message with a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller. This rule DOES apply to the fields of this message."
      + " If setting a field to null is not an option (going through IHMC ROS API), the user can use the latter rule to select the messages to be processed by the controller.",
      rosPackage = "ihmc_msgs",
      topic = "/control/whole_body_trajectory")
public class WholeBodyTrajectoryMessage extends Packet<WholeBodyTrajectoryMessage>
      implements VisualizablePacket, TransformableDataObject<WholeBodyTrajectoryMessage>, MultiplePacketHolder
{
   public HandTrajectoryMessage leftHandTrajectoryMessage, rightHandTrajectoryMessage;
   public ArmTrajectoryMessage leftArmTrajectoryMessage, rightArmTrajectoryMessage;
   public ChestTrajectoryMessage chestTrajectoryMessage;
   public PelvisTrajectoryMessage pelvisTrajectoryMessage;
   public FootTrajectoryMessage leftFootTrajectoryMessage, rightFootTrajectoryMessage;

   /**
    * Empty constructor for serialization.
    * Set the id of the message to {@link Packet#VALID_MESSAGE_DEFAULT_ID}.
    */
   public WholeBodyTrajectoryMessage()
   {
      setUniqueId(VALID_MESSAGE_DEFAULT_ID);
   }

   public HandTrajectoryMessage getHandTrajectoryMessage(RobotSide robotSide)
   {
      switch (robotSide)
      {
      case LEFT:
         return leftHandTrajectoryMessage;
      case RIGHT:
         return rightHandTrajectoryMessage;
      default:
         throw new RuntimeException("Should not get there.");
      }
   }

   public ArmTrajectoryMessage getArmTrajectoryMessage(RobotSide robotSide)
   {
      switch (robotSide)
      {
      case LEFT:
         return leftArmTrajectoryMessage;
      case RIGHT:
         return rightArmTrajectoryMessage;
      default:
         throw new RuntimeException("Should not get there.");
      }
   }

   public ChestTrajectoryMessage getChestTrajectoryMessage()
   {
      return chestTrajectoryMessage;
   }

   public PelvisTrajectoryMessage getPelvisTrajectoryMessage()
   {
      return pelvisTrajectoryMessage;
   }

   public FootTrajectoryMessage getFootTrajectoryMessage(RobotSide robotSide)
   {
      switch (robotSide)
      {
      case LEFT:
         return leftFootTrajectoryMessage;
      case RIGHT:
         return rightFootTrajectoryMessage;
      default:
         throw new RuntimeException("Should not get there.");
      }
   }

   public void setHandTrajectoryMessage(HandTrajectoryMessage handTrajectoryMessage)
   {
      if (handTrajectoryMessage.getUniqueId() == INVALID_MESSAGE_ID)
         handTrajectoryMessage.setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      switch (handTrajectoryMessage.getRobotSide())
      {
      case LEFT:
         leftHandTrajectoryMessage = handTrajectoryMessage;
         return;
      case RIGHT:
         rightHandTrajectoryMessage = handTrajectoryMessage;
         return;
      default:
         throw new RuntimeException("Should not get there.");
      }
   }

   public void setArmTrajectoryMessage(ArmTrajectoryMessage armTrajectoryMessage)
   {
      if (armTrajectoryMessage.getUniqueId() == INVALID_MESSAGE_ID)
         armTrajectoryMessage.setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      switch (armTrajectoryMessage.getRobotSide())
      {
      case LEFT:
         leftArmTrajectoryMessage = armTrajectoryMessage;
         return;
      case RIGHT:
         rightArmTrajectoryMessage = armTrajectoryMessage;
         return;
      default:
         throw new RuntimeException("Should not get there.");
      }
   }

   public void setChestTrajectoryMessage(ChestTrajectoryMessage chestTrajectoryMessage)
   {
      if (chestTrajectoryMessage.getUniqueId() == INVALID_MESSAGE_ID)
         chestTrajectoryMessage.setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      this.chestTrajectoryMessage = chestTrajectoryMessage;
   }

   public void setPelvisTrajectoryMessage(PelvisTrajectoryMessage pelvisTrajectoryMessage)
   {
      if (pelvisTrajectoryMessage.getUniqueId() == INVALID_MESSAGE_ID)
         pelvisTrajectoryMessage.setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      this.pelvisTrajectoryMessage = pelvisTrajectoryMessage;
   }

   public void setFootTrajectoryMessage(FootTrajectoryMessage footTrajectoryMessage)
   {
      if (footTrajectoryMessage.getUniqueId() == INVALID_MESSAGE_ID)
         footTrajectoryMessage.setUniqueId(VALID_MESSAGE_DEFAULT_ID);

      switch (footTrajectoryMessage.getRobotSide())
      {
      case LEFT:
         leftFootTrajectoryMessage = footTrajectoryMessage;
         return;
      case RIGHT:
         rightFootTrajectoryMessage = footTrajectoryMessage;
         return;
      default:
         throw new RuntimeException("Should not get there.");
      }
   }

   public boolean checkRobotSideConsistency()
   {
      if (leftHandTrajectoryMessage != null && leftHandTrajectoryMessage.getRobotSide() != RobotSide.LEFT)
         return false;
      if (rightHandTrajectoryMessage != null && rightHandTrajectoryMessage.getRobotSide() != RobotSide.RIGHT)
         return false;
      if (leftArmTrajectoryMessage != null && leftArmTrajectoryMessage.getRobotSide() != RobotSide.LEFT)
         return false;
      if (rightArmTrajectoryMessage != null && rightArmTrajectoryMessage.getRobotSide() != RobotSide.RIGHT)
         return false;
      if (leftFootTrajectoryMessage != null && leftFootTrajectoryMessage.getRobotSide() != RobotSide.LEFT)
         return false;
      if (rightFootTrajectoryMessage != null && rightFootTrajectoryMessage.getRobotSide() != RobotSide.RIGHT)
         return false;

      return true;
   }

   @Override
   public boolean epsilonEquals(WholeBodyTrajectoryMessage other, double epsilon)
   {
      if (leftHandTrajectoryMessage == null && other.leftHandTrajectoryMessage != null)
         return false;
      if (rightHandTrajectoryMessage == null && other.rightHandTrajectoryMessage != null)
         return false;
      if (leftArmTrajectoryMessage == null && other.leftArmTrajectoryMessage != null)
         return false;
      if (rightArmTrajectoryMessage == null && other.rightArmTrajectoryMessage != null)
         return false;
      if (chestTrajectoryMessage == null && other.chestTrajectoryMessage != null)
         return false;
      if (pelvisTrajectoryMessage == null && other.pelvisTrajectoryMessage != null)
         return false;
      if (leftFootTrajectoryMessage == null && other.leftFootTrajectoryMessage != null)
         return false;
      if (rightFootTrajectoryMessage == null && other.rightFootTrajectoryMessage != null)
         return false;

      if (leftHandTrajectoryMessage != null && other.leftHandTrajectoryMessage == null)
         return false;
      if (rightHandTrajectoryMessage != null && other.rightHandTrajectoryMessage == null)
         return false;
      if (leftArmTrajectoryMessage != null && other.leftArmTrajectoryMessage == null)
         return false;
      if (rightArmTrajectoryMessage != null && other.rightArmTrajectoryMessage == null)
         return false;
      if (chestTrajectoryMessage != null && other.chestTrajectoryMessage == null)
         return false;
      if (pelvisTrajectoryMessage != null && other.pelvisTrajectoryMessage == null)
         return false;
      if (leftFootTrajectoryMessage != null && other.leftFootTrajectoryMessage == null)
         return false;
      if (rightFootTrajectoryMessage != null && other.rightFootTrajectoryMessage == null)
         return false;

      if (!leftHandTrajectoryMessage.epsilonEquals(other.leftHandTrajectoryMessage, epsilon))
         return false;
      if (!rightHandTrajectoryMessage.epsilonEquals(other.rightHandTrajectoryMessage, epsilon))
         return false;
      if (!leftArmTrajectoryMessage.epsilonEquals(other.leftArmTrajectoryMessage, epsilon))
         return false;
      if (!rightArmTrajectoryMessage.epsilonEquals(other.rightArmTrajectoryMessage, epsilon))
         return false;
      if (!chestTrajectoryMessage.epsilonEquals(other.chestTrajectoryMessage, epsilon))
         return false;
      if (!pelvisTrajectoryMessage.epsilonEquals(other.pelvisTrajectoryMessage, epsilon))
         return false;
      if (!leftFootTrajectoryMessage.epsilonEquals(other.leftFootTrajectoryMessage, epsilon))
         return false;
      if (!rightFootTrajectoryMessage.epsilonEquals(other.rightFootTrajectoryMessage, epsilon))
         return false;

      return true;
   }

   @Override
   public WholeBodyTrajectoryMessage transform(RigidBodyTransform transform)
   {
      WholeBodyTrajectoryMessage transformedWholeBodyTrajectoryMessage = new WholeBodyTrajectoryMessage();

      if (leftHandTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.leftHandTrajectoryMessage = leftHandTrajectoryMessage.transform(transform);
      if (rightHandTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.rightHandTrajectoryMessage = rightHandTrajectoryMessage.transform(transform);
      if (leftArmTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.leftArmTrajectoryMessage = new ArmTrajectoryMessage(leftArmTrajectoryMessage);
      if (rightArmTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.rightArmTrajectoryMessage = new ArmTrajectoryMessage(rightArmTrajectoryMessage);
      if (chestTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.chestTrajectoryMessage = chestTrajectoryMessage.transform(transform);
      if (pelvisTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.pelvisTrajectoryMessage = pelvisTrajectoryMessage.transform(transform);
      if (leftFootTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.leftFootTrajectoryMessage = leftFootTrajectoryMessage.transform(transform);
      if (rightFootTrajectoryMessage != null)
         transformedWholeBodyTrajectoryMessage.rightFootTrajectoryMessage = rightFootTrajectoryMessage.transform(transform);

      return transformedWholeBodyTrajectoryMessage;
   }

   /** {@inheritDoc} */
   @Override
   public String validateMessage()
   {
      String errorMessage = PacketValidityChecker.validatePacket(this, true);
      if (errorMessage != null)
         return errorMessage;
      if (!checkRobotSideConsistency())
      {
         errorMessage = "The robotSide of a field is inconsistent with its name.";
         return errorMessage;
      }
      return null;
   }

   @Override
   public void parseMessageIdToChildren()
   {
      if (leftHandTrajectoryMessage != null)
         leftHandTrajectoryMessage.setUniqueId(uniqueId);
      if (rightHandTrajectoryMessage != null)
         rightHandTrajectoryMessage.setUniqueId(uniqueId);
      if (leftArmTrajectoryMessage != null)
         leftArmTrajectoryMessage.setUniqueId(uniqueId);
      if (rightArmTrajectoryMessage != null)
         rightArmTrajectoryMessage.setUniqueId(uniqueId);
      if (chestTrajectoryMessage != null)
         chestTrajectoryMessage.setUniqueId(uniqueId);
      if (pelvisTrajectoryMessage != null)
         pelvisTrajectoryMessage.setUniqueId(uniqueId);
      if (leftFootTrajectoryMessage != null)
         leftFootTrajectoryMessage.setUniqueId(uniqueId);
      if (rightFootTrajectoryMessage != null)
         rightFootTrajectoryMessage.setUniqueId(uniqueId);
   }

   @Override
   public List<Packet<?>> getPackets()
   {
      ArrayList<Packet<?>> wholeBodyPackets = new ArrayList<>();
      wholeBodyPackets.add(leftHandTrajectoryMessage);
      wholeBodyPackets.add(rightHandTrajectoryMessage);
      wholeBodyPackets.add(leftArmTrajectoryMessage);
      wholeBodyPackets.add(rightArmTrajectoryMessage);
      wholeBodyPackets.add(chestTrajectoryMessage);
      wholeBodyPackets.add(pelvisTrajectoryMessage);
      wholeBodyPackets.add(leftFootTrajectoryMessage);
      wholeBodyPackets.add(rightFootTrajectoryMessage);
      return wholeBodyPackets;
   }
}
