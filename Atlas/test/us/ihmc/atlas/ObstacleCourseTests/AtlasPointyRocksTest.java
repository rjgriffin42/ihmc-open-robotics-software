package us.ihmc.atlas.ObstacleCourseTests;

import javax.vecmath.Vector3d;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.obstacleCourseTests.HumanoidPointyRocksTest;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanTarget;

@DeployableTestClass(targets = {TestPlanTarget.InDevelopment})
public class AtlasPointyRocksTest extends HumanoidPointyRocksTest
{
   private final DRCRobotModel robotModel = new AtlasRobotModel(AtlasRobotVersion.ATLAS_UNPLUGGED_V5_NO_HANDS, DRCRobotModel.RobotTarget.SCS, false);
   
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

   @Override
   protected Vector3d getFootSlipVector()
   {
      return new Vector3d(0.05, -0.05, 0.0);//(0.06, -0.06, 0.0);
   }
   @Override
   protected DoubleYoVariable getPelvisOrientationErrorVariableName(SimulationConstructionSet scs)
   {
      
      return (DoubleYoVariable) scs.getVariable("root.atlas.DRCSimulation.DRCControllerThread.DRCMomentumBasedController.HighLevelHumanoidControllerManager.MomentumBasedControllerFactory.WholeBodyControllerCore.WholeBodyFeedbackController.pelvisOrientationFBController.pelvisAxisAngleOrientationController",
                                                "pelvisRotationErrorInBodyZ");
   }

   @Override
   protected double getFootSlipTimeDeltaAfterTouchdown()
   {
      return 0.025;
   }
}

