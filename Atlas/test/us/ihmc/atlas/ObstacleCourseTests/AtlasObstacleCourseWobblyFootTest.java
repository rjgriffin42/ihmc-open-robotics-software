package us.ihmc.atlas.ObstacleCourseTests;

import java.util.List;

import javax.vecmath.Vector3d;

import org.apache.commons.lang3.tuple.ImmutablePair;

import us.ihmc.atlas.AtlasJointMap;
import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.atlas.parameters.AtlasContactPointParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.DRCObstacleCourseWobblyFootTest;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationPlan;
import us.ihmc.wholeBodyController.DRCRobotJointMap;

@ContinuousIntegrationPlan(categories = {IntegrationCategory.SLOW, IntegrationCategory.VIDEO})
public class AtlasObstacleCourseWobblyFootTest extends DRCObstacleCourseWobblyFootTest
{
   @Override
   public DRCRobotModel getRobotModel()
   {
      final AtlasRobotVersion atlasVersion = AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS;

      DRCRobotModel robotModel = new AtlasRobotModel(atlasVersion, DRCRobotModel.RobotTarget.SCS, false)
      {
         @Override
         public AtlasJointMap getJointMap()
         {
            return createJointMapWithWobblyFeet(getAtlasVersion());
         }
      };

      return robotModel;
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }

   @Override
   protected DoubleYoVariable getPelvisOrientationErrorVariableName(SimulationConstructionSet scs)
   {
      return (DoubleYoVariable) scs.getVariable("root.atlas.DRCSimulation.DRCControllerThread.DRCMomentumBasedController.HighLevelHumanoidControllerManager.MomentumBasedControllerFactory.WholeBodyControllerCore.WholeBodyFeedbackController.pelvisOrientationFBController.pelvisAxisAngleOrientationController",
            "pelvisRotationErrorInBodyZ");
   }

   private AtlasJointMap createJointMapWithWobblyFeet(final AtlasRobotVersion atlasVersion)
   {
      AtlasJointMap atlasJointMap = new AtlasJointMap(atlasVersion)
      {
         @Override
         public List<ImmutablePair<String, Vector3d>> getJointNameGroundContactPointMap()
         {
            return createWobblyContactPoints(this, atlasVersion).getJointNameGroundContactPointMap();
         }
      };

      return atlasJointMap;
   }

   private AtlasContactPointParameters createWobblyContactPoints(DRCRobotJointMap jointMap, AtlasRobotVersion atlasVersion)
   {
      AtlasContactPointParameters contactPointParameters = new AtlasContactPointParameters(jointMap, atlasVersion, false);
      double footZWobbleForTests = 0.01;
      contactPointParameters.createWobblyFootContactPoints(footZWobbleForTests);
      return contactPointParameters;
   }
}
