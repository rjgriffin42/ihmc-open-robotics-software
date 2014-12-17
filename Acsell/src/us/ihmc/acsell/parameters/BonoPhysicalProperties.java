package us.ihmc.acsell.parameters;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;

import javax.vecmath.Vector3d;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.utilities.math.geometry.TransformTools;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;

public class BonoPhysicalProperties extends DRCRobotPhysicalProperties
{
   public static final double ankleHeight = 2.0 * 0.0254;
   public static final double footForward = 0.202;
   public static final double footBack = 0.05;
   public static final double footLength = footForward + footBack;
   public static final double toeWidth = 0.152;
   public static final double footWidth = toeWidth - 0.022;
   public static final double thighLength = 0.37694;
   public static final double shinLength = 0.42164;
   public static final double legLength = thighLength + shinLength;
   public static final double pelvisToFoot = 0.887;

   public static final SideDependentList<RigidBodyTransform> soleToAnkleFrameTransforms = new SideDependentList<RigidBodyTransform>();
   static
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         RigidBodyTransform soleToAnkleFrame = TransformTools.yawPitchDegreesTransform(new Vector3d(footLength / 2.0 - footBack, 0.0, -ankleHeight), 0.0, Math.toDegrees(0.0 * 0.18704));
         soleToAnkleFrameTransforms.put(robotSide, soleToAnkleFrame);
      }
   }

   @Override
   public double getAnkleHeight()
   {
      return ankleHeight;
   }

   public static RigidBodyTransform getAnkleToSoleFrameTransform(RobotSide side)
   {
      return soleToAnkleFrameTransforms.get(side);
   }
}
