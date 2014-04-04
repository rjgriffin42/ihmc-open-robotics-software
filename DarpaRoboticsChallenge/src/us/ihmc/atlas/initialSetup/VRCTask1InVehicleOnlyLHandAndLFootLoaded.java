package us.ihmc.atlas.initialSetup;

import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.back_bkx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.back_bky;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.back_bkz;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.jointNames;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_arm_elx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_arm_ely;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_arm_shx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_arm_shy;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_arm_wrx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_arm_wry;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_leg_akx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_leg_aky;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_leg_hpx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_leg_hpy;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_leg_hpz;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.l_leg_kny;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_arm_elx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_arm_ely;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_arm_shx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_arm_shy;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_arm_wrx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_arm_wry;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_leg_akx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_leg_aky;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_leg_hpx;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_leg_hpy;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_leg_hpz;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.r_leg_kny;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.SDFRobot;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.initialSetup.DRCRobotInitialSetup;

public class VRCTask1InVehicleOnlyLHandAndLFootLoaded implements DRCRobotInitialSetup<SDFRobot>
{

   private final double groundZ;
   private final Transform3D rootToWorld = new Transform3D();
   private final Vector3d offset = new Vector3d();

   public VRCTask1InVehicleOnlyLHandAndLFootLoaded(double groundZ)
   {
      this.groundZ = groundZ;
   }

   public void initializeRobot(SDFRobot robot, DRCRobotJointMap jointMap)
   {
      // Avoid singularities at startup
      robot.getOneDegreeOfFreedomJoint(jointNames[l_arm_shy]).setQ(-0.8528);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_arm_shx]).setQ(0.1144);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_arm_ely]).setQ(0.9796);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_arm_elx]).setQ(1.6769);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_arm_wry]).setQ(-1.13);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_arm_wrx]).setQ(0.6748);
      
      robot.getOneDegreeOfFreedomJoint(jointNames[r_arm_shy]).setQ(-0.1573);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_arm_shx]).setQ(0.9835);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_arm_ely]).setQ(1.5037);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_arm_elx]).setQ(-1.3852);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_arm_wry]).setQ(-0.4969);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_arm_wrx]).setQ(-0.2671);
      
      robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_hpz]).setQ(-0.0308);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_hpx]).setQ(0.1414);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_hpy]).setQ(-1.5865);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_kny]).setQ(1.7287);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_aky]).setQ(-0.0256);
      robot.getOneDegreeOfFreedomJoint(jointNames[l_leg_akx]).setQ(-0.0064);

      robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_hpz]).setQ(-0.1052);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_hpx]).setQ(-0.2278);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_hpy]).setQ(-1.5537);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_kny]).setQ(1.6453);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_aky]).setQ(-0.0256);
      robot.getOneDegreeOfFreedomJoint(jointNames[r_leg_akx]).setQ(0.3884);

      robot.getOneDegreeOfFreedomJoint(jointNames[back_bkz]).setQ(-0.0312);
      robot.getOneDegreeOfFreedomJoint(jointNames[back_bky]).setQ(-0.2424);
      robot.getOneDegreeOfFreedomJoint(jointNames[back_bkx]).setQ(0.1817);

      robot.getOneDegreeOfFreedomJoint("left_f0_j0").setQ(0.1157);
      robot.getOneDegreeOfFreedomJoint("left_f1_j0").setQ(0.1112);
      robot.getOneDegreeOfFreedomJoint("left_f2_j0").setQ(0.1091);
      robot.getOneDegreeOfFreedomJoint("left_f3_j0").setQ(0.0043);

      robot.getOneDegreeOfFreedomJoint("left_f0_j1").setQ(1.1612);
      robot.getOneDegreeOfFreedomJoint("left_f1_j1").setQ(1.158);
      robot.getOneDegreeOfFreedomJoint("left_f2_j1").setQ(1.155);
      robot.getOneDegreeOfFreedomJoint("left_f3_j1").setQ(1.0436);
      
      robot.getOneDegreeOfFreedomJoint("left_f0_j2").setQ(1.0156);
      robot.getOneDegreeOfFreedomJoint("left_f1_j2").setQ(1.0162);
      robot.getOneDegreeOfFreedomJoint("left_f2_j2").setQ(1.017);
      robot.getOneDegreeOfFreedomJoint("left_f3_j2").setQ(0.0011);

      robot.getOneDegreeOfFreedomJoint("right_f3_j1").setQ(-1.57);

      robot.setPositionInWorld(new Vector3d(-0.079, 0.3955, 0.9872));
      robot.setOrientation(new Quat4d(-0.0672, -0.0334, 0.0296, 0.9967));
   }
}