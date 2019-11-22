package us.ihmc.valkyrie.pushRecovery;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.pushRecovery.AvatarICPOptimizationPushRecoveryBTest;
import us.ihmc.commonWalkingControlModules.capturePoint.optimization.ICPOptimizationParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.parameters.ValkyrieICPOptimizationParameters;
import us.ihmc.valkyrie.parameters.ValkyrieWalkingControllerParameters;

public class ValkyrieICPOptimizationPushRecoveryBTest extends AvatarICPOptimizationPushRecoveryBTest
{
   @Override
   protected DRCRobotModel getRobotModel()
   {
      ValkyrieRobotModel valkyrieRobotModel = new ValkyrieRobotModel(RobotTarget.SCS)
      {
         @Override
         public WalkingControllerParameters getWalkingControllerParameters()
         {
            return new ValkyrieWalkingControllerParameters(getJointMap(), getRobotPhysicalProperties(), RobotTarget.SCS)
            {
               @Override
               public ICPOptimizationParameters getICPOptimizationParameters()
               {
                  return new ValkyrieICPOptimizationParameters(RobotTarget.SCS)
                  {
                     @Override
                     public boolean useAngularMomentum()
                     {
                        return true;
                     }

                     @Override
                     public boolean allowStepAdjustment()
                     {
                        return true;
                     }
                  };
               }
            };

         }
      };

      return valkyrieRobotModel;
   }

   @Override
   protected double getSizeScale()
   {
      return 1.0;
   }

   @Override
   public double getNominalHeight()
   {
      return 0.9;
   }

   @Override
   public double getSlowSwingDuration()
   {
      return 1.2;
   }

   @Override
   public double getSlowTransferDuration()
   {
      return 0.8;
   }

   @Tag("humanoid-push-recovery-slow")
   @Override
   @Test
   public void testPushICPOptimizationDiagonalOutwardPushInSwing() throws Exception
   {
      percentWeight = 0.2;
      super.testPushICPOptimizationDiagonalOutwardPushInSwing();
   }

   @Tag("humanoid-push-recovery-slow")
   @Override
   @Test
   public void testPushICPOptimizationDiagonalYawingOutwardPushInSwing() throws Exception
   {
      percentWeight = 0.11; // Used to be 0.13 before PR #1326
      super.testPushICPOptimizationDiagonalYawingOutwardPushInSwing();
   }

   @Tag("humanoid-push-recovery-slow")
   @Override
   @Test
   public void testPushICPOptimizationLongBackwardPushInSwing() throws Exception
   {
      percentWeight = 0.15;
      super.testPushICPOptimizationLongBackwardPushInSwing();
   }

   @Tag("humanoid-push-recovery")
   @Override
   @Test
   public void testPushICPOptimizationLongForwardPushInSwing() throws Exception
   {
      percentWeight = 0.07;
      super.testPushICPOptimizationLongForwardPushInSwing();
   }

   @Tag("humanoid-push-recovery")
   @Override
   @Test
   public void testPushICPOptimizationNoPush() throws Exception
   {
      super.testPushICPOptimizationNoPush();
   }

   @Tag("humanoid-push-recovery-slow")
   @Override
   @Test
   public void testPushICPOptimizationOutwardPushInSlowSwing() throws Exception
   {
      percentWeight = 0.11;
      super.testPushICPOptimizationOutwardPushInSlowSwing();
   }

   @Tag("humanoid-push-recovery")
   @Override
   @Test
   public void testPushICPOptimizationOutwardPushInSwing() throws Exception
   {
      percentWeight = 0.25;
      super.testPushICPOptimizationOutwardPushInSwing();
   }

   @Tag("humanoid-push-recovery")
   @Override
   @Test
   public void testPushICPOptimizationRandomPushInSwing() throws Exception
   {
      percentWeight = 0.18;
      super.testPushICPOptimizationRandomPushInSwing();
   }

   public static void main(String[] args)
   {
      ValkyrieICPOptimizationPushRecoveryBTest test = new ValkyrieICPOptimizationPushRecoveryBTest();
      try
      {
         test.testPushICPOptimizationNoPush();
      }
      catch (Exception e)
      {

      }
   }
}
