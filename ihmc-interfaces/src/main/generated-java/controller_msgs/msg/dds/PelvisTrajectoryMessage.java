package controller_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;

/**
 * This message is part of the IHMC whole-body controller API. This message commands the controller
 * to move in taskspace the pelvis to the desired pose (position & orientation) while going through
 * the specified trajectory points. A third order polynomial function is used to interpolate
 * positions and a Hermite based curve (third order) is used to interpolate the orientations. To
 * excute a single straight line trajectory to reach a desired pelvis pose, set only one trajectory
 * point with zero velocity and its time to be equal to the desired trajectory time. Note that the
 * pelvis position is limited keep the robot's balance (center of mass has to remain inside the
 * support polygon).
 */
public class PelvisTrajectoryMessage extends Packet<PelvisTrajectoryMessage>
      implements Settable<PelvisTrajectoryMessage>, EpsilonComparable<PelvisTrajectoryMessage>
{
   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header header_;
   /**
    * Execute this trajectory in user mode. User mode tries to achieve the desired regardless of the
    * leg kinematics.
    */
   public boolean enable_user_pelvis_control_;
   /**
    * If enable_user_pelvis_control is true then enable_user_pelvis_control_during_walking will keep
    * the manager in user mode while walking. If this is false the manager will switch back to
    * controller mode when walking.
    */
   public boolean enable_user_pelvis_control_during_walking_;
   /**
    * The position/orientation trajectory information.
    */
   public controller_msgs.msg.dds.SE3TrajectoryMessage se3_trajectory_;

   public PelvisTrajectoryMessage()
   {
      header_ = new std_msgs.msg.dds.Header();
      se3_trajectory_ = new controller_msgs.msg.dds.SE3TrajectoryMessage();
   }

   public PelvisTrajectoryMessage(PelvisTrajectoryMessage other)
   {
      this();
      set(other);
   }

   public void set(PelvisTrajectoryMessage other)
   {
      std_msgs.msg.dds.HeaderPubSubType.staticCopy(other.header_, header_);
      enable_user_pelvis_control_ = other.enable_user_pelvis_control_;

      enable_user_pelvis_control_during_walking_ = other.enable_user_pelvis_control_during_walking_;

      controller_msgs.msg.dds.SE3TrajectoryMessagePubSubType.staticCopy(other.se3_trajectory_, se3_trajectory_);
   }

   /**
    * As of March 2018, the header for this message is only use for its sequence ID.
    */
   public std_msgs.msg.dds.Header getHeader()
   {
      return header_;
   }

   /**
    * Execute this trajectory in user mode. User mode tries to achieve the desired regardless of the
    * leg kinematics.
    */
   public void setEnableUserPelvisControl(boolean enable_user_pelvis_control)
   {
      enable_user_pelvis_control_ = enable_user_pelvis_control;
   }

   /**
    * Execute this trajectory in user mode. User mode tries to achieve the desired regardless of the
    * leg kinematics.
    */
   public boolean getEnableUserPelvisControl()
   {
      return enable_user_pelvis_control_;
   }

   /**
    * If enable_user_pelvis_control is true then enable_user_pelvis_control_during_walking will keep
    * the manager in user mode while walking. If this is false the manager will switch back to
    * controller mode when walking.
    */
   public void setEnableUserPelvisControlDuringWalking(boolean enable_user_pelvis_control_during_walking)
   {
      enable_user_pelvis_control_during_walking_ = enable_user_pelvis_control_during_walking;
   }

   /**
    * If enable_user_pelvis_control is true then enable_user_pelvis_control_during_walking will keep
    * the manager in user mode while walking. If this is false the manager will switch back to
    * controller mode when walking.
    */
   public boolean getEnableUserPelvisControlDuringWalking()
   {
      return enable_user_pelvis_control_during_walking_;
   }

   /**
    * The position/orientation trajectory information.
    */
   public controller_msgs.msg.dds.SE3TrajectoryMessage getSe3Trajectory()
   {
      return se3_trajectory_;
   }

   @Override
   public boolean epsilonEquals(PelvisTrajectoryMessage other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (!this.header_.epsilonEquals(other.header_, epsilon))
         return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.enable_user_pelvis_control_, other.enable_user_pelvis_control_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.enable_user_pelvis_control_during_walking_, other.enable_user_pelvis_control_during_walking_,
                                                     epsilon))
         return false;

      if (!this.se3_trajectory_.epsilonEquals(other.se3_trajectory_, epsilon))
         return false;

      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;
      if (!(other instanceof PelvisTrajectoryMessage))
         return false;

      PelvisTrajectoryMessage otherMyClass = (PelvisTrajectoryMessage) other;

      if (!this.header_.equals(otherMyClass.header_))
         return false;
      if (this.enable_user_pelvis_control_ != otherMyClass.enable_user_pelvis_control_)
         return false;

      if (this.enable_user_pelvis_control_during_walking_ != otherMyClass.enable_user_pelvis_control_during_walking_)
         return false;

      if (!this.se3_trajectory_.equals(otherMyClass.se3_trajectory_))
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("PelvisTrajectoryMessage {");
      builder.append("header=");
      builder.append(this.header_);
      builder.append(", ");
      builder.append("enable_user_pelvis_control=");
      builder.append(this.enable_user_pelvis_control_);
      builder.append(", ");
      builder.append("enable_user_pelvis_control_during_walking=");
      builder.append(this.enable_user_pelvis_control_during_walking_);
      builder.append(", ");
      builder.append("se3_trajectory=");
      builder.append(this.se3_trajectory_);
      builder.append("}");
      return builder.toString();
   }
}
