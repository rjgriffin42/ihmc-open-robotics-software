package us.ihmc.steppr.parameters;

import javax.vecmath.Vector3d;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.utilities.math.geometry.TransformTools;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;

public class BonoPhysicalProperties extends DRCRobotPhysicalProperties
{
   /* Original Ankle
   public static final double ankleHeight = 2.0 * 0.0254;
   public static final double shiftFootForward = 0.000;
   public static final double shortenFoot = 0.0;//0.025;
   public static final double footForward = 0.202 + shiftFootForward - shortenFoot;
   public static final double heelExtension = 2*0.0254;
   public static final double footBack = 0.05 + heelExtension - shiftFootForward;
   public static final double footLength = footForward + footBack;
   public static final double toeWidth = 0.152;
   public static final double footWidth = toeWidth - 0.022;
   public static final double thighLength = 0.37694;
   public static final double shinLength = 0.42164;
   public static final double legLength = 1.01 * (thighLength + shinLength);
   public static final double pelvisToFoot = 0.887;
   */
   
   /* Spring Ankle */
   public static final double ankleHeight = 3.0 * 0.0254;
   public static final double shiftFootForward = 0.001;
   public static final double shortenFoot = 0.0;//0.025;
   public static final double footForward = 0.202 + shiftFootForward - shortenFoot;
   public static final double heelExtension = 2*0.0254;
   public static final double footBack = 0.092;//0.05 + heelExtension - shiftFootForward;
   public static final double footLength = footForward + footBack;
   public static final double toeWidth = 0.152;
   public static final double footWidth = toeWidth - 0.022;
   public static final double thighLength = 0.37694;
   public static final double shinLength = 0.42164-0.6*0.0254;
   public static final double legLength = 1.01 * (thighLength + shinLength);
   public static final double pelvisToFoot = 0.869;
   

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
