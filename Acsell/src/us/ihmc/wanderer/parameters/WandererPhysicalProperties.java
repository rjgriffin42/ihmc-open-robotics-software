package us.ihmc.wanderer.parameters;

import javax.vecmath.Vector3d;

import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotPhysicalProperties;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.TransformTools;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

public class WandererPhysicalProperties extends DRCRobotPhysicalProperties
{
   
   public static final double ankleHeight   = 2.625 * 0.0254 + 0.016;
   public static final double footForward   = 7.975 * 0.0254 - 0.2 * 0.0254 - 0.01 - 0.005;
   public static final double heelExtension = 0.0   * 0.0254;
   public static final double footBack      = 2.280 * 0.0254 - 0.2 * 0.0254 - 0.01 - 0.005;
   public static final double toeWidth      = 6.000 * 0.0254 - 2 * 0.375 * 0.0254 - 0.01 - 0.02;
   public static final double thighLength   = 14.80 * 0.0254;
   public static final double shinLength    = 16.00 * 0.0254;
   public static final double footLength = footForward + footBack;
   public static final double footWidth = toeWidth;
   public static final double legLength = thighLength + shinLength;
   //public static final double pelvisToAnkle = 0.833225;
   //public static final double pelvisToGround = 0.899900;
   

   public static final SideDependentList<RigidBodyTransform> soleToAnkleFrameTransforms = new SideDependentList<RigidBodyTransform>();
   static
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         RigidBodyTransform soleToAnkleFrame = TransformTools.yawPitchDegreesTransform(new Vector3d(footLength / 2.0 - footBack, 0.0, -ankleHeight), 0.0, 0.0);
         soleToAnkleFrameTransforms.put(robotSide, soleToAnkleFrame);
      }
   }

   @Override
   public double getAnkleHeight()
   {
      return ankleHeight;
   }

   public static RigidBodyTransform getSoleToAnkleFrameTransform(RobotSide side)
   {
      return soleToAnkleFrameTransforms.get(side);
   }
}
