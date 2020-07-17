package us.ihmc.humanoidRobotics.communication.kinematicsToolboxAPI;

import controller_msgs.msg.dds.HumanoidKinematicsToolboxConfigurationMessage;
import us.ihmc.communication.controllerAPI.command.Command;

public class HumanoidKinematicsToolboxConfigurationCommand
      implements Command<HumanoidKinematicsToolboxConfigurationCommand, HumanoidKinematicsToolboxConfigurationMessage>
{
   private long sequenceId;
   private boolean enableSupportPolygonConstraint = true;
   private boolean holdCurrentCenterOfMassXYPosition = true;
   private boolean holdSupportRigidBodies = true;

   @Override
   public void clear()
   {
      sequenceId = 0;
      enableSupportPolygonConstraint = true;
      holdCurrentCenterOfMassXYPosition = true;
      holdSupportRigidBodies = true;
   }

   @Override
   public void set(HumanoidKinematicsToolboxConfigurationCommand other)
   {
      sequenceId = other.sequenceId;
      enableSupportPolygonConstraint = other.enableSupportPolygonConstraint;
      holdCurrentCenterOfMassXYPosition = other.holdCurrentCenterOfMassXYPosition;
      holdSupportRigidBodies = other.holdSupportRigidBodies;
   }

   @Override
   public void setFromMessage(HumanoidKinematicsToolboxConfigurationMessage message)
   {
      sequenceId = message.getSequenceId();
      enableSupportPolygonConstraint = message.getEnableSupportPolygonConstraint();
      holdCurrentCenterOfMassXYPosition = message.getHoldCurrentCenterOfMassXyPosition();
      holdSupportRigidBodies = message.getHoldSupportRigidBodies();
   }

   public boolean enableSupportPolygonConstraint()
   {
      return enableSupportPolygonConstraint;
   }

   public boolean holdCurrentCenterOfMassXYPosition()
   {
      return holdCurrentCenterOfMassXYPosition;
   }

   public boolean holdSupportRigidBodies()
   {
      return holdSupportRigidBodies;
   }

   @Override
   public Class<HumanoidKinematicsToolboxConfigurationMessage> getMessageClass()
   {
      return HumanoidKinematicsToolboxConfigurationMessage.class;
   }

   @Override
   public boolean isCommandValid()
   {
      return true;
   }

   @Override
   public long getSequenceId()
   {
      return sequenceId;
   }
}
