package us.ihmc.atlas.utilities.kinematics;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.atlas.AtlasRobotModel;
import us.ihmc.atlas.AtlasRobotVersion;
import us.ihmc.darpaRoboticsChallenge.NumericalInverseKinematicsCalculatorWithRobotTest;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.simulationconstructionset.bambooTools.BambooTools;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class AtlasNumericalInverseKinematicsCalculatorWithRobotTest extends NumericalInverseKinematicsCalculatorWithRobotTest
{
   @Override
   public DRCRobotModel getRobotModel()
   {
      return new AtlasRobotModel(AtlasRobotVersion.DRC_NO_HANDS, AtlasRobotModel.AtlasTarget.SIM, false);
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.ATLAS);
   }

   
   @Ignore
   @AverageDuration(duration = 0.0)
   @Test(timeout = 120000)
   public void testTroublesomeCaseOne()
   {
      FramePoint handEndEffectorPositionFK = new FramePoint(ReferenceFrame.getWorldFrame(), -0.10094331252710122, 0.702327488375448, 0.8842020873774364);
      FrameOrientation handEndEffectorOrientationFK = new FrameOrientation(ReferenceFrame.getWorldFrame(), 0.5948015455279927, -0.24418998175404205, 0.11864264766705496, 0.7566414582898712);

      InitialGuessForTests initialGuessForTests = InitialGuessForTests.MIDRANGE;
      boolean updateListenersEachStep = true;
      double errorThreshold = 10.01;
      boolean success = testAPose(handEndEffectorPositionFK, handEndEffectorOrientationFK, initialGuessForTests, errorThreshold , updateListenersEachStep);
      assertTrue(success);
   }
   
}
