package us.ihmc.SdfLoader;

import org.apache.commons.lang3.tuple.ImmutablePair;

import us.ihmc.robotics.partNames.ArmJointName;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.partNames.LimbName;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.robotSide.RobotSide;

public interface SDFHumanoidJointNameMap extends SDFJointNameMap
{
   public ImmutablePair<RobotSide, LegJointName> getLegJointName(String jointName);

   public ImmutablePair<RobotSide, ArmJointName> getArmJointName(String jointName);

   public ImmutablePair<RobotSide, LimbName> getLimbName(String limbName);

   public String getJointBeforeFootName(RobotSide robotSide);

   public String getJointBeforeHandName(RobotSide robotSide);

   public RigidBodyTransform getSoleToAnkleFrameTransform(RobotSide robotSide);

   public RigidBodyTransform getHandControlFrameToWristTransform(RobotSide robotSide);
}
