package us.ihmc.atlas.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.atlas.parameters.AtlasICPOptimizationParameters;
import us.ihmc.atlas.parameters.AtlasWalkingControllerParameters;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.commonWalkingControlModules.AvatarICPOptimizationPushRecoveryTest;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.continuousIntegration.IntegrationCategory;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.FAST})
public class AtlasSimpleICPOptimizationPushRecoveryTest extends AvatarICPOptimizationPushRecoveryTest
{
   protected DRCRobotModel getRobotModel()
   {
      AtlasRobotModel atlasRobotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, RobotTarget.SCS, false)
      {
         @Override
         public WalkingControllerParameters getWalkingControllerParameters()
         {
            return new AtlasWalkingControllerParameters(RobotTarget.SCS, getJointMap(), getContactPointParameters())
            {
               @Override
               public boolean useOptimizationBasedICPController()
               {
                  return true;
               }
            };
         }

         @Override
         public ICPOptimizationParameters getICPOptimizationParameters()
         {
            return new AtlasICPOptimizationParameters(true)
            {
               @Override
               public boolean useSimpleOptimization()
               {
                  return true;
               }
            };
         }
      };

      return atlasRobotModel;
   }

   public static void main(String[] args)
   {
      AtlasSimpleICPOptimizationPushRecoveryTest test = new AtlasSimpleICPOptimizationPushRecoveryTest();
      try
      {
         test.testPushICPOptimizationNoPush();
      }
      catch(SimulationExceededMaximumTimeException e)
      {

      }
   }
}
