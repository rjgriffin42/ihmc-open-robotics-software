package us.ihmc.atlas.behaviorTests;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.atlas.parameters.AtlasContactPointParameters;
import us.ihmc.darpaRoboticsChallenge.behaviorTests.DRCDetectCollisionUsingWristSensorTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.utilities.code.agileTesting.BambooPlanType;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.BambooPlan;

@BambooPlan(planType = {BambooPlanType.Flaky})
public class AtlasDetectCollisionUsingWristSensorTest extends DRCDetectCollisionUsingWristSensorTest
{
   private final AtlasRobotModel robotModel;
   
   public AtlasDetectCollisionUsingWristSensorTest()
   {
      robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_INVISIBLE_CONTACTABLE_PLANE_HANDS, AtlasRobotModel.AtlasTarget.SIM, false);
      
      AtlasContactPointParameters contactPointParameters = robotModel.getContactPointParameters();
      contactPointParameters.createHandKnobContactPoints();

   }
   
   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }
}

