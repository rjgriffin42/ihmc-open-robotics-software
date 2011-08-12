package us.ihmc.commonWalkingControlModules.kinematics;

import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointPositions;
import us.ihmc.robotSide.RobotSide;

public interface ArmInverseKinematicsCalculator
{
   public abstract void solve(ArmJointPositions armJointPositionsToPack, Vector3d desiredElbowPointInShoulderFrame, Vector3d desiredOrientationOfUpperArmInShoulderFrame, RobotSide robotSide) throws InverseKinematicsException;
}