package perception_msgs.msg.dds;

import us.ihmc.communication.packets.Packet;
import us.ihmc.euclid.interfaces.Settable;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import java.util.function.Supplier;
import us.ihmc.pubsub.TopicDataType;

/**
       * A detectable perception scene node
       * The topic name identifies the node.
       */
public class DetectableSceneNodeMessage extends Packet<DetectableSceneNodeMessage> implements Settable<DetectableSceneNodeMessage>, EpsilonComparable<DetectableSceneNodeMessage>
{
   /**
            * The name of the scene node
            */
   public java.lang.StringBuilder name_;
   /**
            * Whether or not the node is currently detected
            */
   public boolean currently_detected_;
   /**
            * Transform of the node's frame to world frame
            */
   public controller_msgs.msg.dds.RigidBodyTransformMessage transform_to_world_;
   /**
            * If this node is trackable via an ArUco maker, this is the ArUco marker's
            * transform to world frame. This is so we can reset overriden node
            * poses back to ArUco relative ones.
            */
   public controller_msgs.msg.dds.RigidBodyTransformMessage aruco_marker_transform_to_world_;
   /**
            * Nodes can be set to not track the detected pose
            */
   public boolean track_detected_pose_;
   /**
            * Alpha filter value for nodes that are alpha filtered
            */
   public float alpha_filter_value_;

   public DetectableSceneNodeMessage()
   {
      name_ = new java.lang.StringBuilder(255);
      transform_to_world_ = new controller_msgs.msg.dds.RigidBodyTransformMessage();
      aruco_marker_transform_to_world_ = new controller_msgs.msg.dds.RigidBodyTransformMessage();
   }

   public DetectableSceneNodeMessage(DetectableSceneNodeMessage other)
   {
      this();
      set(other);
   }

   public void set(DetectableSceneNodeMessage other)
   {
      name_.setLength(0);
      name_.append(other.name_);

      currently_detected_ = other.currently_detected_;

      controller_msgs.msg.dds.RigidBodyTransformMessagePubSubType.staticCopy(other.transform_to_world_, transform_to_world_);
      controller_msgs.msg.dds.RigidBodyTransformMessagePubSubType.staticCopy(other.aruco_marker_transform_to_world_, aruco_marker_transform_to_world_);
      track_detected_pose_ = other.track_detected_pose_;

      alpha_filter_value_ = other.alpha_filter_value_;

   }

   /**
            * The name of the scene node
            */
   public void setName(java.lang.String name)
   {
      name_.setLength(0);
      name_.append(name);
   }

   /**
            * The name of the scene node
            */
   public java.lang.String getNameAsString()
   {
      return getName().toString();
   }
   /**
            * The name of the scene node
            */
   public java.lang.StringBuilder getName()
   {
      return name_;
   }

   /**
            * Whether or not the node is currently detected
            */
   public void setCurrentlyDetected(boolean currently_detected)
   {
      currently_detected_ = currently_detected;
   }
   /**
            * Whether or not the node is currently detected
            */
   public boolean getCurrentlyDetected()
   {
      return currently_detected_;
   }


   /**
            * Transform of the node's frame to world frame
            */
   public controller_msgs.msg.dds.RigidBodyTransformMessage getTransformToWorld()
   {
      return transform_to_world_;
   }


   /**
            * If this node is trackable via an ArUco maker, this is the ArUco marker's
            * transform to world frame. This is so we can reset overriden node
            * poses back to ArUco relative ones.
            */
   public controller_msgs.msg.dds.RigidBodyTransformMessage getArucoMarkerTransformToWorld()
   {
      return aruco_marker_transform_to_world_;
   }

   /**
            * Nodes can be set to not track the detected pose
            */
   public void setTrackDetectedPose(boolean track_detected_pose)
   {
      track_detected_pose_ = track_detected_pose;
   }
   /**
            * Nodes can be set to not track the detected pose
            */
   public boolean getTrackDetectedPose()
   {
      return track_detected_pose_;
   }

   /**
            * Alpha filter value for nodes that are alpha filtered
            */
   public void setAlphaFilterValue(float alpha_filter_value)
   {
      alpha_filter_value_ = alpha_filter_value;
   }
   /**
            * Alpha filter value for nodes that are alpha filtered
            */
   public float getAlphaFilterValue()
   {
      return alpha_filter_value_;
   }


   public static Supplier<DetectableSceneNodeMessagePubSubType> getPubSubType()
   {
      return DetectableSceneNodeMessagePubSubType::new;
   }

   @Override
   public Supplier<TopicDataType> getPubSubTypePacket()
   {
      return DetectableSceneNodeMessagePubSubType::new;
   }

   @Override
   public boolean epsilonEquals(DetectableSceneNodeMessage other, double epsilon)
   {
      if(other == null) return false;
      if(other == this) return true;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsStringBuilder(this.name_, other.name_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.currently_detected_, other.currently_detected_, epsilon)) return false;

      if (!this.transform_to_world_.epsilonEquals(other.transform_to_world_, epsilon)) return false;
      if (!this.aruco_marker_transform_to_world_.epsilonEquals(other.aruco_marker_transform_to_world_, epsilon)) return false;
      if (!us.ihmc.idl.IDLTools.epsilonEqualsBoolean(this.track_detected_pose_, other.track_detected_pose_, epsilon)) return false;

      if (!us.ihmc.idl.IDLTools.epsilonEqualsPrimitive(this.alpha_filter_value_, other.alpha_filter_value_, epsilon)) return false;


      return true;
   }

   @Override
   public boolean equals(Object other)
   {
      if(other == null) return false;
      if(other == this) return true;
      if(!(other instanceof DetectableSceneNodeMessage)) return false;

      DetectableSceneNodeMessage otherMyClass = (DetectableSceneNodeMessage) other;

      if (!us.ihmc.idl.IDLTools.equals(this.name_, otherMyClass.name_)) return false;

      if(this.currently_detected_ != otherMyClass.currently_detected_) return false;

      if (!this.transform_to_world_.equals(otherMyClass.transform_to_world_)) return false;
      if (!this.aruco_marker_transform_to_world_.equals(otherMyClass.aruco_marker_transform_to_world_)) return false;
      if(this.track_detected_pose_ != otherMyClass.track_detected_pose_) return false;

      if(this.alpha_filter_value_ != otherMyClass.alpha_filter_value_) return false;


      return true;
   }

   @Override
   public java.lang.String toString()
   {
      StringBuilder builder = new StringBuilder();

      builder.append("DetectableSceneNodeMessage {");
      builder.append("name=");
      builder.append(this.name_);      builder.append(", ");
      builder.append("currently_detected=");
      builder.append(this.currently_detected_);      builder.append(", ");
      builder.append("transform_to_world=");
      builder.append(this.transform_to_world_);      builder.append(", ");
      builder.append("aruco_marker_transform_to_world=");
      builder.append(this.aruco_marker_transform_to_world_);      builder.append(", ");
      builder.append("track_detected_pose=");
      builder.append(this.track_detected_pose_);      builder.append(", ");
      builder.append("alpha_filter_value=");
      builder.append(this.alpha_filter_value_);
      builder.append("}");
      return builder.toString();
   }
}
