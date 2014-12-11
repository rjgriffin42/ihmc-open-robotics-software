package us.ihmc.valkyrie;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.test.JUnitTestSuiteConstructor;

@RunWith(Suite.class)
@Suite.SuiteClasses
({
   us.ihmc.valkyrie.codeGenerators.APIBuilderTest.class,
   us.ihmc.valkyrie.kinematics.ClosedFormJacobianTest.class,
   us.ihmc.valkyrie.kinematics.PushrodTransmissionTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.ComparePushRodTransmissionsTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientPushrodTransmissionJacobianTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientPushRodTransmissionTest.class,
//   us.ihmc.valkyrie.networkProcessor.depthData.ValkyrieDepthDataProcessorTest.class,
//   us.ihmc.valkyrie.simulation.ValkyrieFlatGroundWalkingWithIMUDriftTest.class,
   us.ihmc.valkyrie.simulation.ValkyriePosePlaybackDemoTest.class,
   us.ihmc.valkyrie.ValkyrieFlatGroundWalkingTest.class,
   us.ihmc.valkyrie.ValkyrieObstacleCourseEveryBuildTest.class,
   us.ihmc.valkyrie.ValkyrieObstacleCourseFlatTest.class,
   us.ihmc.valkyrie.ValkyrieObstacleCourseRampsTest.class,
   us.ihmc.valkyrie.ValkyrieObstacleCourseTrialsTerrainTest.class,
   us.ihmc.valkyrie.ValkyriePushRecoveryMultiStepTest.class,
   us.ihmc.valkyrie.ValkyriePushRecoveryStandingTest.class,
   us.ihmc.valkyrie.ValkyriePushRecoveryTest.class,
//   us.ihmc.valkyrie.ValkyriePushRecoveryWalkingTest.class
})

public class ValkyrieBambooTestSuiteNightly
{
   public static void main(String[] args)
   {
      JUnitTestSuiteConstructor.generateTestSuite(ValkyrieBambooTestSuiteNightly.class);
   }
}

