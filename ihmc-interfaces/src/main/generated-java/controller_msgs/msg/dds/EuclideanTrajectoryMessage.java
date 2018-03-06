package controller_msgs.msg.dds;

import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.interfaces.Settable;

/**
 * This message is part of the IHMC whole-body controller API.
 * This message carries the information to execute a trajectory in taskspace (position only) by defining trajectory points.
 * A third order polynomial function is used to interpolate positions.
 * To execute a single straight line trajectory to reach a desired position, set only one trajectory point with zero velocity and its time to be equal to the desired trajectory time.
 */
public class EuclideanTrajectoryMessage implements Settable<EuclideanTrajectoryMessage>, EpsilonComparable<EuclideanTrajectoryMessage>
{
   /**
    * List of trajectory points (in taskpsace) to go through while executing the trajectory.
    */
   private us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.EuclideanTrajectoryPointMessage> taskspace_trajectory_points_;
   /**
    * The selection matrix for each axis.
    */
   private controller_msgs.msg.dds.SelectionMatrix3DMessage selection_matrix_;
   /**
    * Frame information for this message.
    */
   private controller_msgs.msg.dds.FrameInformation frame_information_;
   /**
    * The weight matrix for each axis.
    */
   private controller_msgs.msg.dds.WeightMatrix3DMessage weight_matrix_;
   /**
    * Flag that tells the controller whether the use of a custom control frame is requested.
    */
   private boolean use_custom_control_frame_;
   /**
    * Pose of custom control frame. This is the frame attached to the rigid body that the taskspace trajectory is defined for.
    */
   private us.ihmc.euclid.geometry.Pose3D control_frame_pose_;
   /**
    * Properties for queueing trajectories.
    */
   private controller_msgs.msg.dds.QueueableMessage queueing_properties_;

   public EuclideanTrajectoryMessage()
   {
      taskspace_trajectory_points_ = new us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.EuclideanTrajectoryPointMessage>(100,
                                                                                                                                 controller_msgs.msg.dds.EuclideanTrajectoryPointMessage.class,
                                                                                                                                 new controller_msgs.msg.dds.EuclideanTrajectoryPointMessagePubSubType());

      selection_matrix_ = new controller_msgs.msg.dds.SelectionMatrix3DMessage();
      frame_information_ = new controller_msgs.msg.dds.FrameInformation();
      weight_matrix_ = new controller_msgs.msg.dds.WeightMatrix3DMessage();

      control_frame_pose_ = new us.ihmc.euclid.geometry.Pose3D();
      queueing_properties_ = new controller_msgs.msg.dds.QueueableMessage();
   }

   public EuclideanTrajectoryMessage(EuclideanTrajectoryMessage other)
   {
      set(other);
   }

   public void set(EuclideanTrajectoryMessage other)
   {
      taskspace_trajectory_points_.set(other.taskspace_trajectory_points_);
      controller_msgs.msg.dds.SelectionMatrix3DMessagePubSubType.staticCopy(other.selection_matrix_, selection_matrix_);
      controller_msgs.msg.dds.FrameInformationPubSubType.staticCopy(other.frame_information_, frame_information_);
      controller_msgs.msg.dds.WeightMatrix3DMessagePubSubType.staticCopy(other.weight_matrix_, weight_matrix_);
      use_custom_control_frame_ = other.use_custom_control_frame_;

      geometry_msgs.msg.dds.PosePubSubType.staticCopy(other.control_frame_pose_, control_frame_pose_);
      controller_msgs.msg.dds.QueueableMessagePubSubType.staticCopy(other.queueing_properties_, queueing_properties_);
   }

   /**
    * List of trajectory points (in taskpsace) to go through while executing the trajectory.
    */
   public us.ihmc.idl.IDLSequence.Object<controller_msgs.msg.dds.EuclideanTrajectoryPointMessage> getTaskspaceTrajectoryPoints()
   {
      return taskspace_trajectory_points_;
   }

   /**
    * The selection matrix for each axis.
    */
   public controller_msgs.msg.dds.SelectionMatrix3DMessage getSelectionMatrix()
   {
      return selection_matrix_;
   }

   /**
    * Frame information for this message.
    */
   public controller_msgs.msg.dds.FrameInformation getFrameInformation()
   {
      return frame_information_;
   }

   /**
    * The weight matrix for each axis.
    */
   public controller_msgs.msg.dds.WeightMatrix3DMessage getWeightMatrix()
   {
      return weight_matrix_;
   }

   /**
    * Flag that tells the controller whether the use of a custom control frame is requested.
    */
   public boolean getUseCustomControlFrame()
   {
      return use_custom_control_frame_;
   }

   /**
    * Flag that tells the controller whether the use of a custom control frame is requested.
    */
   public void setUseCustomControlFrame(boolean use_custom_control_frame)
   {
      use_custom_control_frame_ = use_custom_control_frame;
   }

   /**
    * Pose of custom control frame. This is the frame attached to the rigid body that the taskspace trajectory is defined for.
    */
   public us.ihmc.euclid.geometry.Pose3D getControlFramePose()
   {
      return control_frame_pose_;
   }

   /**
    * Properties for queueing trajectories.
    */
   public controller_msgs.msg.dds.QueueableMessage getQueueingProperties()
   {
      return queueing_properties_;
   }

   @Override
   public boolean epsilonEquals(EuclideanTrajectoryMessage other, double epsilon)
   {
      if (other == null)
         return false;
      if (other == this)
         return true;

      if (this.taskspace_trajectory_points_.isEnum())
      {
         if (!this.taskspace_trajectory_points_.equals(other.taskspace_trajectory_points_))
            return false;
      }
      else if (this.taskspace_trajectory_points_.size() == other.taskspace_trajectory_points_.size())
      {
         return false;
      }
      else
      {
         for (int i = 0; i < this.taskspace_trajectory_points_.size(); i++)
         {
            if (!this.taskspace_trajectory_points_.get(i).epsilonEquals(other.taskspace_trajectory_points_.get(i), epsilon))
               return false;
         }
      }

      if (!this.selection_matrix_.epsilonEquals(other.selection_matrix_, epsilon))
         return false;

      if (!this.frame_information_.epsilonEquals(other.frame_information_, epsilon))
         return false;

      if (!this.weight_matrix_.epsilonEquals(other.weight_matrix_, epsilon))
         return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.use_custom_control_frame_, other.use_custom_control_frame_, epsilon))
         return false;

      if (!this.control_frame_pose_.epsilonEquals(other.control_frame_pose_, epsilon))
         return false;

      if (!this.queueing_properties_.epsilonEquals(other.queueing_properties_, epsilon))
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
      if (!(other instanceof EuclideanTrajectoryMessage))
         return false;

      EuclideanTrajectoryMessage otherMyClass = (EuclideanTrajectoryMessage) other;

      if (!this.taskspace_trajectory_points_.equals(otherMyClass.taskspace_trajectory_points_))
         return false;

      if (!this.selection_matrix_.equals(otherMyClass.selection_matrix_))
         return false;

      if (!this.frame_information_.equals(otherMyClass.frame_information_))
         return false;

      if (!this.weight_matrix_.equals(otherMyClass.weight_matrix_))
         return false;

      if (this.use_custom_control_frame_ != otherMyClass.use_custom_control_frame_)
         return false;

      if (!this.control_frame_pose_.equals(otherMyClass.control_frame_pose_))
         return false;

      if (!this.queueing_properties_.equals(otherMyClass.queueing_properties_))
         return false;

      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("EuclideanTrajectoryMessage {");
      builder.append("taskspace_trajectory_points=");
      builder.append(this.taskspace_trajectory_points_);

      builder.append(", ");
      builder.append("selection_matrix=");
      builder.append(this.selection_matrix_);

      builder.append(", ");
      builder.append("frame_information=");
      builder.append(this.frame_information_);

      builder.append(", ");
      builder.append("weight_matrix=");
      builder.append(this.weight_matrix_);

      builder.append(", ");
      builder.append("use_custom_control_frame=");
      builder.append(this.use_custom_control_frame_);

      builder.append(", ");
      builder.append("control_frame_pose=");
      builder.append(this.control_frame_pose_);

      builder.append(", ");
      builder.append("queueing_properties=");
      builder.append(this.queueing_properties_);

      builder.append("}");
      return builder.toString();
   }
}