package us.ihmc.acsell;

import org.junit.Test;

import us.ihmc.acsell.controlParameters.BonoStateEstimatorParameters;
import us.ihmc.acsell.controlParameters.BonoWalkingControllerParameters;
import us.ihmc.acsell.parameters.BonoRobotModel;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.DRCFlatGroundWalkingTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.sensorProcessing.stateEstimation.FootSwitchType;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;

public class BonoFlatGroundWalkingKinematicFootSwitchTest extends DRCFlatGroundWalkingTest
{

   private BonoRobotModel robotModel;

   @Test(timeout=300000)
   public void testBONOFlatGroundWalking() throws SimulationExceededMaximumTimeException
   {
      BambooTools.reportTestStartedMessage();

      String runName = "BONOFlatGroundWalkingTest";
      final boolean runningOnRealRobot = false;
      
      //create a subclass of standard DRCRobot model but overrides FootSwitchType for both WalkingControl/StateEstimation parameters.
      robotModel = new BonoRobotModel(runningOnRealRobot, false)
      {
         @Override
         public WalkingControllerParameters getWalkingControllerParameters()
         {
            return new BonoWalkingControllerParameters(super.getJointMap(), runningOnRealRobot)
            {
               @Override
               public FootSwitchType getFootSwitchType()
               {
                  return FootSwitchType.KinematicBased;
               }
            };
         }

         @Override
         public StateEstimatorParameters getStateEstimatorParameters()
         {
            return new BonoStateEstimatorParameters(runningOnRealRobot, getEstimatorDT())
            {
               @Override
               public FootSwitchType getFootSwitchType()
               {
                  return FootSwitchType.KinematicBased;
               }
            };

         }
      };

      boolean doPelvisYawWarmup = false;
      setupAndTestFlatGroundSimulationTrack(robotModel, runName, doPelvisYawWarmup);
   }

   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.BONO);
   }
}
