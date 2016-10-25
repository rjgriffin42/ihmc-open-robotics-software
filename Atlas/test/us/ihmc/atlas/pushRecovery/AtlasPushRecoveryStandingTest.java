package us.ihmc.atlas.pushRecovery;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.DRCPushRecoveryStandingTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.simulationconstructionset.util.simulationRunner.ControllerFailureException;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.SLOW, IntegrationCategory.VIDEO})
public class AtlasPushRecoveryStandingTest extends DRCPushRecoveryStandingTest
{
   @Override
	public DRCRobotModel getRobotModel() 
	{
		return new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, DRCRobotModel.RobotTarget.SCS, false);
	}

	@Override
	public String getSimpleRobotName() 
	{
		return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
	}
	
	@Override
	public void testDoublePushForwardInDoubleSupportAndContinueWalking() throws SimulationExceededMaximumTimeException, InterruptedException, ControllerFailureException
	{
	   super.testDoublePushForwardInDoubleSupportAndContinueWalking();
	}
	
	@Override
	public void testPushBackwardForwardInDoubleSupportAndContinueWalking() throws SimulationExceededMaximumTimeException, InterruptedException, ControllerFailureException
	{
	   super.testPushBackwardForwardInDoubleSupportAndContinueWalking();
	}
	
	@Override
	public void testPushBackwardInDoubleSupportAndContinueWalking() throws SimulationExceededMaximumTimeException, InterruptedException, ControllerFailureException
	{
	   super.testPushBackwardInDoubleSupportAndContinueWalking();
	}
	
	@Override
	public void testPushForwardInDoubleSupport() throws SimulationExceededMaximumTimeException, InterruptedException, ControllerFailureException
	{
	   super.testPushForwardInDoubleSupport();
	}
	
	@Override
	public void testPushForwardInDoubleSupportAndContinueWalking() throws SimulationExceededMaximumTimeException, InterruptedException, ControllerFailureException
	{
	   super.testPushForwardInDoubleSupportAndContinueWalking();
	}
}
