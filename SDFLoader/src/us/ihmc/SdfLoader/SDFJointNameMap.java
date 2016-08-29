package us.ihmc.SdfLoader;

import java.util.List;
import java.util.Set;

import javax.vecmath.Vector3d;

import org.apache.commons.lang3.tuple.ImmutablePair;

import us.ihmc.SdfLoader.partNames.JointRole;
import us.ihmc.SdfLoader.partNames.NeckJointName;
import us.ihmc.SdfLoader.partNames.RobotSpecificJointNames;
import us.ihmc.SdfLoader.partNames.SpineJointName;
import us.ihmc.robotics.robotSide.RobotSegment;

public interface SDFJointNameMap extends RobotSpecificJointNames
{

   String getModelName();

   JointRole getJointRole(String jointName);

   NeckJointName getNeckJointName(String jointName);

   SpineJointName getSpineJointName(String jointName);

   String getPelvisName();

   String getUnsanitizedRootJointInSdf();

   String getChestName();

   String getHeadName();

   List<ImmutablePair<String, Vector3d>> getJointNameGroundContactPointMap();

   boolean isTorqueVelocityLimitsEnabled();

   Set<String> getLastSimulatedJoints();

   String[] getJointNamesBeforeFeet();

   Enum<?>[] getRobotSegments();

}