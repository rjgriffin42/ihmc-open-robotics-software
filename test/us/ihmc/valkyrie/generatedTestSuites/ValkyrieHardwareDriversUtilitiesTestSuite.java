package us.ihmc.valkyrie.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//import us.ihmc.utilities.code.unitTesting.runner.JUnitTestSuiteRunner;

@RunWith(Suite.class)
@Suite.SuiteClasses
({
   us.ihmc.valkyrie.codeGenerators.APIBuilderTest.class,
   us.ihmc.valkyrie.kinematics.ClosedFormJacobianTest.class,
   us.ihmc.valkyrie.kinematics.PushrodTransmissionTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.ComparePushRodTransmissionsTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientPushrodTransmissionJacobianTest.class,
   us.ihmc.valkyrie.kinematics.transmissions.InefficientPushRodTransmissionTest.class,
   us.ihmc.valkyrie.kinematics.will.WillsPushrodTransmissionTest.class,
   us.ihmc.valkyrie.simulation.ValkyriePosePlaybackDemoTest.class
})

public class ValkyrieHardwareDriversUtilitiesTestSuite
{
   public static void main(String[] args)
   {
      //new JUnitTestSuiteRunner(ValkyrieHardwareDriversUtilitiesTestSuite.class);
   }
}

