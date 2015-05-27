package us.ihmc.commonWalkingControlModules.packetConsumers;

import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.communication.packets.manipulation.ArmJointTrajectoryPacket;
import us.ihmc.communication.packets.manipulation.HandPosePacket;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

public interface HandPoseProvider
{
   public abstract FramePose getDesiredHandPose(RobotSide robotSide);
   
   public abstract FramePose[] getDesiredHandPoses(RobotSide robotSide);

   public abstract ReferenceFrame getDesiredReferenceFrame(RobotSide robotSide);

   public abstract double getTrajectoryTime();
   
   public abstract void clear();
   
   public abstract boolean checkForNewPose(RobotSide robotSide);

   public abstract boolean checkForNewPoseList(RobotSide robotSide);
   
   public abstract boolean checkForNewRotateAboutAxisPacket(RobotSide robotSide);
   
   public abstract Point3d getRotationAxisOriginInWorld(RobotSide robotSide);
   
   public abstract Vector3d getRotationAxisInWorld(RobotSide robotSide);
   
   public abstract double getRotationAngleRightHandRule(RobotSide robotSide);

   public abstract boolean controlHandAngleAboutAxis(RobotSide robotSide);
   
   public abstract double getGraspOffsetFromControlFrame(RobotSide robotSide);
   
   public abstract boolean checkAndResetStopCommand(RobotSide robotSide);

   public abstract boolean checkForHomePosition(RobotSide robotSide);

   public abstract HandPosePacket.DataType checkHandPosePacketDataType(RobotSide robotSide);

   public abstract HandPosePacket.DataType checkHandPoseListPacketDataType(RobotSide robotSide);

   public abstract Map<OneDoFJoint, Double> getFinalDesiredJointAngleMaps(RobotSide robotSide);

   public abstract Map<OneDoFJoint, double[]> getDesiredJointAngleForWaypointTrajectory(RobotSide robotSide);

   public abstract boolean checkForNewArmJointTrajectory(RobotSide robotSide);

   public abstract ArmJointTrajectoryPacket getArmJointTrajectoryPacket(RobotSide robotSide);

   /**
    * By default it should be all true, meaning the hand orientation will be full constrained.
    */
   public abstract boolean[] getControlledOrientationAxes(RobotSide robotSide);

   public abstract double getPercentOfTrajectoryWithOrientationBeingControlled(RobotSide robotSide);
}