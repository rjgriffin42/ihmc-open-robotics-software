package us.ihmc.valkyrie;

import org.junit.runner.*;
import org.junit.runners.*;

import us.ihmc.utilities.test.JUnitTestSuiteConstructor;

@RunWith(Suite.class)
@Suite.SuiteClasses(
{
   us.ihmc.valkyrie.ValkyrieFlatGroundWalkingTest.class,
   us.ihmc.valkyrie.ValkyriePushInSingleSupportTest.class,
   us.ihmc.valkyrie.simulation.ValkyriePosePlaybackDemoTest.class,
   us.ihmc.valkyrie.ValkyrieControllerFactoryTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientButReadablePushrodTransmissionTest.class
})

public class ValkyrieBambooTestSuite
{
   public static void main(String[] args)
   {
      String packageName = "us.ihmc.valkyrie";
      System.out.println(JUnitTestSuiteConstructor.createTestSuite("ValkyrieBambooTestSuite", packageName));
   }
}