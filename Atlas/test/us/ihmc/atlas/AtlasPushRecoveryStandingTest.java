package us.ihmc.atlas;

import us.ihmc.darpaRoboticsChallenge.DRCPushRecoveryStandingTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.tools.agileTesting.BambooAnnotations.BambooPlan;
import us.ihmc.tools.agileTesting.BambooPlanType;

@BambooPlan(planType = {BambooPlanType.InDevelopment, BambooPlanType.VideoA})
public class AtlasPushRecoveryStandingTest extends DRCPushRecoveryStandingTest {

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
}
