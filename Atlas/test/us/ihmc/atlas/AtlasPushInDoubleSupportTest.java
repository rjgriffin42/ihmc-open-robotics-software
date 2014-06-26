package us.ihmc.atlas;

import us.ihmc.bambooTools.BambooTools;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;


public class AtlasPushInDoubleSupportTest extends DRCPushRecoveryInDoubleSupportTest {

	@Override
	public DRCRobotModel getRobotModel() {
		return new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, false, false);
	}

	@Override
	public String getSimpleRobotName() {
		return BambooTools
				.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
	}
}
