package us.ihmc.valkyrie.controllerAPI;

import us.ihmc.avatar.controllerAPI.EndToEndPelvisHeightTrajectoryMessageTest;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.valkyrie.ValkyrieRobotModel;

public class ValkyrieEndToEndPelvisHeightTrajectoryMessageTest extends EndToEndPelvisHeightTrajectoryMessageTest
{
   private final ValkyrieRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.SCS, false);

   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.VALKYRIE);
   }
   
   @Override
   public void testSingleWaypoint() throws Exception
   {
      super.testSingleWaypoint();
   }
   
   @Override
   public void testSingleWaypointInUserMode() throws Exception
   {
      super.testSingleWaypointInUserMode();
   }
}
