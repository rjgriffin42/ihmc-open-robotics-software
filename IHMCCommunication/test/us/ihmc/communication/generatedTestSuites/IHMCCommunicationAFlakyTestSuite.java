package us.ihmc.communication.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import us.ihmc.tools.testing.TestPlanSuite;
import us.ihmc.tools.testing.TestPlanSuite.TestSuiteTarget;
import us.ihmc.tools.testing.TestPlanTarget;

/** WARNING: AUTO-GENERATED FILE. DO NOT MAKE MANUAL CHANGES TO THIS FILE. **/
@RunWith(TestPlanSuite.class)
@TestSuiteTarget(TestPlanTarget.Flaky)
@SuiteClasses
({
   us.ihmc.communication.net.KryoObjectCommunicatorTest.class,
   us.ihmc.communication.remote.DataObjectTransponderTest.class,
   us.ihmc.communication.streamingData.StreamingDataTCPServerTest.class
})

public class IHMCCommunicationAFlakyTestSuite
{
   public static void main(String[] args)
   {

   }
}
