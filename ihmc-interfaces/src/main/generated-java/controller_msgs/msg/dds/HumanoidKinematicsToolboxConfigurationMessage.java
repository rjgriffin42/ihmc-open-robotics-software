package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * This message is part of the IHMC whole-body inverse kinematics module.
       */
public class HumanoidKinematicsToolboxConfigurationMessage extends Packet<HumanoidKinematicsToolboxConfigurationMessage> implements Settable<HumanoidKinematicsToolboxConfigurationMessage>, EpsilonComparable<HumanoidKinematicsToolboxConfigurationMessage>
{
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long sequence_id_;
   /**
            * When true, the solve enforces the solution to have the projection of the center of mass contained
            * inside the current support polygon. By 'current', it means that the solver will use the robot configuration data
            * broadcasted by the controller to obtain the support polygon.
            */
   public boolean enable_support_polygon_constraint_ = true;
   /**
            * When set to true, the solver will maintain, if possible, the current x and y coordinates of the center
            * of mass. By 'current', it means that the solver will use the robot configuration data
            * broadcasted by the controller to obtain the center of mass position.
            */
   public boolean hold_current_center_of_mass_xy_position_ = true;
   /**
            * When set to true, the solver will hold the pose of the rigid-bodies with active contact points.
            * - when the walking controller is running, the rigid-bodies in question are the feet.
            * - when a multi-contact controller is running, any rigid-body of the robot can be in contact.
            */
   public boolean hold_support_rigid_bodies_ = true;

   public HumanoidKinematicsToolboxConfigurationMessage()
   {
   }

   public HumanoidKinematicsToolboxConfigurationMessage(HumanoidKinematicsToolboxConfigurationMessage other)
   {
      this();
      set(other);
   }

   public void set(HumanoidKinematicsToolboxConfigurationMessage other)
   {
      sequence_id_ = other.sequence_id_;

      enable_support_polygon_constraint_ = other.enable_support_polygon_constraint_;

      hold_current_center_of_mass_xy_position_ = other.hold_current_center_of_mass_xy_position_;

      hold_support_rigid_bodies_ = other.hold_support_rigid_bodies_;

   }

   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public void setSequenceId(long sequence_id)
   {
      sequence_id_ = sequence_id;
   }
   /**
            * Unique ID used to identify this message, should preferably be consecutively increasing.
            */
   public long getSequenceId()
   {
      return sequence_id_;
   }

   /**
            * When true, the solve enforces the solution to have the projection of the center of mass contained
            * inside the current support polygon. By 'current', it means that the solver will use the robot configuration data
            * broadcasted by the controller to obtain the support polygon.
            */
   public void setEnableSupportPolygonConstraint(boolean enable_support_polygon_constraint)
   {
      enable_support_polygon_constraint_ = enable_support_polygon_constraint;
   }
   /**
            * When true, the solve enforces the solution to have the projection of the center of mass contained
            * inside the current support polygon. By 'current', it means that the solver will use the robot configuration data
            * broadcasted by the controller to obtain the support polygon.
            */
   public boolean getEnableSupportPolygonConstraint()
   {
      return enable_support_polygon_constraint_;
   }

   /**
            * When set to true, the solver will maintain, if possible, the current x and y coordinates of the center
            * of mass. By 'current', it means that the solver will use the robot configuration data
            * broadcasted by the controller to obtain the center of mass position.
            */
   public void setHoldCurrentCenterOfMassXyPosition(boolean hold_current_center_of_mass_xy_position)
   {
      hold_current_center_of_mass_xy_position_ = hold_current_center_of_mass_xy_position;
   }
   /**
            * When set to true, the solver will maintain, if possible, the current x and y coordinates of the center
            * of mass. By 'current', it means that the solver will use the robot configuration data
            * broadcasted by the controller to obtain the center of mass position.
            */
   public boolean getHoldCurrentCenterOfMassXyPosition()
   {
      return hold_current_center_of_mass_xy_position_;
   }

   /**
            * When set to true, the solver will hold the pose of the rigid-bodies with active contact points.
            * - when the walking controller is running, the rigid-bodies in question are the feet.
            * - when a multi-contact controller is running, any rigid-body of the robot can be in contact.
            */
   public void setHoldSupportRigidBodies(boolean hold_support_rigid_bodies)
   {
      hold_support_rigid_bodies_ = hold_support_rigid_bodies;
   }
   /**
            * When set to true, the solver will hold the pose of the rigid-bodies with active contact points.
            * - when the walking controller is running, the rigid-bodies in question are the feet.
            * - when a multi-contact controller is running, any rigid-body of the robot can be in contact.
            */
   public boolean getHoldSupportRigidBodies()
   {
      return hold_support_rigid_bodies_;
   }


   public static Supplier<HumanoidKinematicsToolboxConfigurationMessagePubSubType> getPubSubType()
   {
      return HumanoidKinematicsToolboxConfigurationMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return HumanoidKinematicsToolboxConfigurationMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(HumanoidKinematicsToolboxConfigurationMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.sequence_id_, other.sequence_id_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.enable_support_polygon_constraint_, other.enable_support_polygon_constraint_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.hold_current_center_of_mass_xy_position_, other.hold_current_center_of_mass_xy_position_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.hold_support_rigid_bodies_, other.hold_support_rigid_bodies_, epsilon)) return false;


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof HumanoidKinematicsToolboxConfigurationMessage)) return false;

      HumanoidKinematicsToolboxConfigurationMessage otherMyClass = (HumanoidKinematicsToolboxConfigurationMessage) other;

      if(this.sequence_id_ != otherMyClass.sequence_id_) return false;

      if(this.enable_support_polygon_constraint_ != otherMyClass.enable_support_polygon_constraint_) return false;

      if(this.hold_current_center_of_mass_xy_position_ != otherMyClass.hold_current_center_of_mass_xy_position_) return false;

      if(this.hold_support_rigid_bodies_ != otherMyClass.hold_support_rigid_bodies_) return false;


      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("HumanoidKinematicsToolboxConfigurationMessage {");
      builder.append("sequence_id=");
      builder.append(this.sequence_id_);      builder.append(", ");
      builder.append("enable_support_polygon_constraint=");
      builder.append(this.enable_support_polygon_constraint_);      builder.append(", ");
      builder.append("hold_current_center_of_mass_xy_position=");
      builder.append(this.hold_current_center_of_mass_xy_position_);      builder.append(", ");
      builder.append("hold_support_rigid_bodies=");
      builder.append(this.hold_support_rigid_bodies_);
      builder.append("}");
      return builder.toString();
   }
}
