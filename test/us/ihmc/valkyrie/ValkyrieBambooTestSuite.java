package us.ihmc.valkyrie;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.test.JUnitTestSuiteConstructor;

@RunWith(Suite.class)
@Suite.SuiteClasses(
{
   us.ihmc.valkyrie.ValkyrieFlatGroundWalkingTest.class,
   us.ihmc.valkyrie.simulation.ValkyriePosePlaybackDemoTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientPushrodTransmissionJacobianTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientPushRodTransmissionTest.class,
   us.ihmc.valkyrie.kinematics.ClosedFormJacobianTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.ComparePushRodTransmissionsTest.class,
   us.ihmc.valkyrie.networkProcessor.depthData.ValkyrieDepthDataProcessorTest.class,
})

public class ValkyrieBambooTestSuite
{
   public static void main(String[] args)
   {
      String packageName = "us.ihmc.valkyrie";
      System.out.println(JUnitTestSuiteConstructor.createTestSuite("ValkyrieBambooTestSuite", packageName));
   }
}