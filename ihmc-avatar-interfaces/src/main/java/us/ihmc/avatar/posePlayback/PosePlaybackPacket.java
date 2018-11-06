package us.ihmc.avatar.posePlayback;

import java.util.Map;

import us.ihmc.mecano.multiBodySystem.OneDoFJoint;

public interface PosePlaybackPacket
{
   public abstract Map<OneDoFJoint, Double> getJointKps();
   public abstract Map<OneDoFJoint, Double> getJointKds();
   
   public abstract PlaybackPoseSequence getPlaybackPoseSequence();
   
   public abstract double getInitialGainScaling();
}
