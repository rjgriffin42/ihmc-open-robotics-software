package us.ihmc.atlas.parameters;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.SwingTrajectoryParameters;

public class AtlasSwingTrajectoryParameters extends SwingTrajectoryParameters
{
   private final DRCRobotModel.RobotTarget target;
   private final double modelScale;
   private final boolean runningOnRealRobot;
   private final double min_mechanical_leg_length;

   public AtlasSwingTrajectoryParameters(DRCRobotModel.RobotTarget target, double modelScale)
   {
      this.target = target;
      this.modelScale = modelScale;
      min_mechanical_leg_length = modelScale * 0.420;

      runningOnRealRobot = target == DRCRobotModel.RobotTarget.REAL_ROBOT;
   }

   @Override
   public boolean doToeTouchdownIfPossible()
   {
      return false;
   }

   @Override
   public double getToeTouchdownAngle()
   {
      return Math.toRadians(20.0);
   }

   @Override
   public boolean doHeelTouchdownIfPossible()
   {
      return false;
   }

   @Override
   public double getHeelTouchdownAngle()
   {
      return Math.toRadians(-20.0);
   }

   @Override
   public double getMinMechanicalLegLength()
   {
      return min_mechanical_leg_length;
   }

   @Override
   public double getDesiredTouchdownHeightOffset()
   {
      return 0;
   }

   @Override
   public double getDesiredTouchdownVelocity()
   {
      return modelScale * -0.3;
   }

   @Override
   public double getDesiredTouchdownAcceleration()
   {
      switch (target)
      {
      case SCS:
         return modelScale * -2.0;

      default :
         return modelScale * -1.0;
      }
   }

   /** {@inheritDoc} */
   @Override
   public double getSwingFootVelocityAdjustmentDamping()
   {
      return runningOnRealRobot ? 0.8 : 0.5; // Robert: 0.8
   }


}
