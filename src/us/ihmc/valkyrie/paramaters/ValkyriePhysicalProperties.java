package us.ihmc.valkyrie.paramaters;


import java.util.EnumMap;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;


public class ValkyriePhysicalProperties extends DRCRobotPhysicalProperties
{
   public static final double ankleHeight = 0.082;
   public static final double footForward = 0.23;
   public static final double footBack = 0.07;
   public static final double footLength = footBack + footForward;
   public static final double footWidth = 0.128;
   public static final double thighLength = 0.431;
   public static final double shinLength = 0.406;
   public static final double pelvisToFoot = 0.887 + 0.3;

   public static final SideDependentList<Transform3D> ankleToSoleFrameTransforms = new SideDependentList<>();

   static
   {
      for (RobotSide side : RobotSide.values)
      {
         Transform3D ankleToSoleFrame = new Transform3D();
//         ankleToSoleFrame.setEuler(new Vector3d(0.0, 1.4408 - Math.PI / 2.0, 0.0));
         ankleToSoleFrame.setTranslation(new Vector3d(0.0, 0.0, -ValkyriePhysicalProperties.ankleHeight));
         ankleToSoleFrameTransforms.put(side, ankleToSoleFrame);
      }
   }

   {
      // XXX: nathan: need to update Valkyrie joint limits to match available joints
      armJointLimits.put(ArmJointName.SHOULDER_PITCH, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.SHOULDER_ROLL, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.ELBOW_PITCH, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.ELBOW_ROLL, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.WRIST_PITCH, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
      armJointLimits.put(ArmJointName.WRIST_ROLL, new Pair<Double, Double>(-2 * Math.PI, 2 * Math.PI));
   }

   @Override
   public double getAnkleHeight()
   {
      return ankleHeight;
   }

   public static Transform3D getAnkleToSoleFrameTransform(RobotSide side)
   {
      return ankleToSoleFrameTransforms.get(side);
   }

}
