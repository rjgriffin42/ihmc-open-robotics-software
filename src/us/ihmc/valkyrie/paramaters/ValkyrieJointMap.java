package us.ihmc.valkyrie.paramaters;

import static us.ihmc.valkyrie.paramaters.ValkyrieOrderedJointMap.*;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.humanoidRobot.partNames.LimbName;
import us.ihmc.utilities.humanoidRobot.partNames.NeckJointName;
import us.ihmc.utilities.humanoidRobot.partNames.SpineJointName;

public class ValkyrieJointMap extends DRCRobotJointMap
{
   public static final String chestName = "v1Trunk";
   public static final String pelvisName = "v1Pelvis";
   public static final String headName = "v1Head";

   private final LegJointName[] legJoints = { LegJointName.HIP_YAW, LegJointName.HIP_ROLL, LegJointName.HIP_PITCH, LegJointName.KNEE, LegJointName.ANKLE_PITCH, LegJointName.ANKLE_ROLL };
   private final ArmJointName[] armJoints = { ArmJointName.SHOULDER_PITCH, ArmJointName.SHOULDER_ROLL, ArmJointName.SHOULDER_YAW, ArmJointName.ELBOW_PITCH, ArmJointName.ELBOW_YAW, ArmJointName.WRIST_ROLL, ArmJointName.WRIST_PITCH };
   private final SpineJointName[] spineJoints = { SpineJointName.SPINE_YAW, SpineJointName.SPINE_PITCH, SpineJointName.SPINE_ROLL };
   private final NeckJointName[] neckJoints = { NeckJointName.LOWER_NECK_PITCH, NeckJointName.NECK_YAW, NeckJointName.UPPER_NECK_PITCH };

   private final LinkedHashMap<String, JointRole> jointRoles = new LinkedHashMap<String, JointRole>();
   private final LinkedHashMap<String, Pair<RobotSide, LimbName>> limbNames = new LinkedHashMap<String, Pair<RobotSide, LimbName>>();

   private final LinkedHashMap<String, Pair<RobotSide, LegJointName>> legJointNames = new LinkedHashMap<String, Pair<RobotSide, LegJointName>>();
   private final LinkedHashMap<String, Pair<RobotSide, ArmJointName>> armJointNames = new LinkedHashMap<String, Pair<RobotSide, ArmJointName>>();
   private final LinkedHashMap<String, SpineJointName> spineJointNames = new LinkedHashMap<String, SpineJointName>();
   private final LinkedHashMap<String, NeckJointName> neckJointNames = new LinkedHashMap<String, NeckJointName>();

   private final SideDependentList<EnumMap<LegJointName, String>> legJointStrings = SideDependentList.createListOfEnumMaps(LegJointName.class);
   private final SideDependentList<EnumMap<ArmJointName, String>> armJointStrings = SideDependentList.createListOfEnumMaps(ArmJointName.class);
   private final EnumMap<SpineJointName, String> spineJointStrings = new EnumMap<>(SpineJointName.class);
   private final EnumMap<NeckJointName, String> neckJointStrings = new EnumMap<>(NeckJointName.class);

   private final ValkyrieContactPointParameters contactPointParameters;

   public ValkyrieJointMap()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         String[] forcedSideJointNames = forcedSideDependentJointNames.get(robotSide);
         legJointNames.put(forcedSideJointNames[LeftHipRotator], new Pair<RobotSide, LegJointName>(robotSide, LegJointName.HIP_YAW));
         legJointNames.put(forcedSideJointNames[LeftHipAdductor], new Pair<RobotSide, LegJointName>(robotSide, LegJointName.HIP_ROLL));
         legJointNames.put(forcedSideJointNames[LeftHipExtensor], new Pair<RobotSide, LegJointName>(robotSide, LegJointName.HIP_PITCH));
         legJointNames.put(forcedSideJointNames[LeftKneeExtensor], new Pair<RobotSide, LegJointName>(robotSide, LegJointName.KNEE));
         legJointNames.put(forcedSideJointNames[LeftAnkleExtensor], new Pair<RobotSide, LegJointName>(robotSide, LegJointName.ANKLE_PITCH));
         legJointNames.put(forcedSideJointNames[LeftAnkle], new Pair<RobotSide, LegJointName>(robotSide, LegJointName.ANKLE_ROLL));

         armJointNames.put(forcedSideJointNames[LeftShoulderExtensor], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.SHOULDER_PITCH));
         armJointNames.put(forcedSideJointNames[LeftShoulderAdductor], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.SHOULDER_ROLL));
         armJointNames.put(forcedSideJointNames[LeftShoulderSupinator], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.SHOULDER_YAW));
         armJointNames.put(forcedSideJointNames[LeftElbowExtensor], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.ELBOW_PITCH));
         armJointNames.put(forcedSideJointNames[LeftForearmSupinator], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.ELBOW_YAW));
         armJointNames.put(forcedSideJointNames[LeftWristExtensor], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.WRIST_ROLL));
         armJointNames.put(forcedSideJointNames[LeftWrist], new Pair<RobotSide, ArmJointName>(robotSide, ArmJointName.WRIST_PITCH));

         String prefix = getRobotSidePrefix(robotSide);
         limbNames.put(prefix + "Palm", new Pair<RobotSide, LimbName>(robotSide, LimbName.ARM));
         limbNames.put(prefix + "UpperFoot", new Pair<RobotSide, LimbName>(robotSide, LimbName.LEG));
      }

      spineJointNames.put(jointNames[WaistRotator], SpineJointName.SPINE_YAW);
      spineJointNames.put(jointNames[WaistExtensor], SpineJointName.SPINE_PITCH);
      spineJointNames.put(jointNames[WaistLateralExtensor], SpineJointName.SPINE_ROLL);

      neckJointNames.put(jointNames[LowerNeckExtensor], NeckJointName.LOWER_NECK_PITCH);
      neckJointNames.put(jointNames[NeckRotator], NeckJointName.NECK_YAW);
      neckJointNames.put(jointNames[UpperNeckExtensor], NeckJointName.UPPER_NECK_PITCH);

      for (String legJointString : legJointNames.keySet())
      {
         RobotSide robotSide = legJointNames.get(legJointString).first();
         LegJointName legJointName = legJointNames.get(legJointString).second();
         legJointStrings.get(robotSide).put(legJointName, legJointString);
         jointRoles.put(legJointString, JointRole.LEG);
      }

      for (String armJointString : armJointNames.keySet())
      {
         RobotSide robotSide = armJointNames.get(armJointString).first();
         ArmJointName armJointName = armJointNames.get(armJointString).second();
         armJointStrings.get(robotSide).put(armJointName, armJointString);
         jointRoles.put(armJointString, JointRole.ARM);
      }

      for (String spineJointString : spineJointNames.keySet())
      {
         spineJointStrings.put(spineJointNames.get(spineJointString), spineJointString);
         jointRoles.put(spineJointString, JointRole.SPINE);
      }

      for (String neckJointString : neckJointNames.keySet())
      {
         neckJointStrings.put(neckJointNames.get(neckJointString), neckJointString);
         jointRoles.put(neckJointString, JointRole.NECK);
      }

      contactPointParameters = new ValkyrieContactPointParameters(this);
   }

   private String getRobotSidePrefix(RobotSide robotSide)
   {
      return (robotSide == RobotSide.LEFT) ? "v1Left" : "v1Right";
   }

   @Override
   public String getNameOfJointBeforeHand(RobotSide robotSide)
   {
      return armJointStrings.get(robotSide).get(ArmJointName.WRIST_PITCH);
   }

   @Override
   public String getNameOfJointBeforeThigh(RobotSide robotSide)
   {
      return legJointStrings.get(robotSide).get(LegJointName.HIP_PITCH);
   }

   @Override
   public String getNameOfJointBeforeChest()
   {
      return spineJointStrings.get(SpineJointName.SPINE_ROLL);
   }

   @Override
   public Pair<RobotSide, LegJointName> getLegJointName(String jointName)
   {
      return legJointNames.get(jointName);
   }

   @Override
   public Pair<RobotSide, ArmJointName> getArmJointName(String jointName)
   {
      return armJointNames.get(jointName);
   }

   @Override
   public Pair<RobotSide, LimbName> getLimbName(String limbName)
   {
      return limbNames.get(limbName);
   }

   @Override
   public JointRole getJointRole(String jointName)
   {
      return jointRoles.get(jointName);
   }

   @Override
   public NeckJointName getNeckJointName(String jointName)
   {
      return neckJointNames.get(jointName);
   }

   @Override
   public SpineJointName getSpineJointName(String jointName)
   {
      return spineJointNames.get(jointName);
   }

   @Override
   public String getPelvisName()
   {
      return pelvisName;
   }

   @Override
   public String getChestName()
   {
      return chestName;
   }

   @Override
   public String getHeadName()
   {
      return headName;
   }

   @Override
   public LegJointName[] getLegJointNames()
   {
      return legJoints;
   }

   @Override
   public ArmJointName[] getArmJointNames()
   {
      return armJoints;
   }

   @Override
   public SpineJointName[] getSpineJointNames()
   {
      return spineJoints;
   }

   @Override
   public NeckJointName[] getNeckJointNames()
   {
      return neckJoints;
   }

   @Override
   public String getJointBeforeFootName(RobotSide robotSide)
   {
      return legJointStrings.get(robotSide).get(LegJointName.ANKLE_ROLL);
   }

   @Override
   public List<Pair<String, Vector3d>> getJointNameGroundContactPointMap()
   {
      return contactPointParameters.getJointNameGroundContactPointMap();
   }

   @Override
   public boolean isTorqueVelocityLimitsEnabled()
   {
      return false;
   }

   @Override
   public Set<String> getLastSimulatedJoints()
   {
      HashSet<String> lastSimulatedJoints = new HashSet<>();
      for (RobotSide robotSide : RobotSide.values)
         lastSimulatedJoints.add(armJointStrings.get(robotSide).get(ArmJointName.WRIST_PITCH));
      return lastSimulatedJoints;
   }

   @Override
   public String getModelName()
   {
      return "V1";
   }

   @Override
   public String[] getOrderedJointNames()
   {
      return ValkyrieOrderedJointMap.jointNames;
   }

   @Override
   public SideDependentList<Transform3D> getAnkleToSoleFrameTransform()
   {
      return ValkyriePhysicalProperties.ankleToSoleFrameTransforms;
   }

   @Override
   public String getLegJointName(RobotSide robotSide, LegJointName legJointName)
   {
      return legJointStrings.get(robotSide).get(legJointName);
   }

   @Override
   public String getArmJointName(RobotSide robotSide, ArmJointName armJointName)
   {
      return armJointStrings.get(robotSide).get(armJointName);
   }

   @Override
   public String getNeckJointName(NeckJointName neckJointName)
   {
      return neckJointStrings.get(neckJointName);
   }

   @Override
   public String getSpineJointName(SpineJointName spineJointName)
   {
      return spineJointStrings.get(spineJointName);
   }
}
