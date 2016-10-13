package us.ihmc.communication.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import us.ihmc.tools.continuousIntegration.ContinuousIntegrationSuite;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationSuite.ContinuousIntegrationSuiteCategory;
import us.ihmc.tools.continuousIntegration.IntegrationCategory;

/** WARNING: AUTO-GENERATED FILE. DO NOT MAKE MANUAL CHANGES TO THIS FILE. **/
@RunWith(ContinuousIntegrationSuite.class)
@ContinuousIntegrationSuiteCategory(IntegrationCategory.FAST)
@SuiteClasses
({
   us.ihmc.communication.kryo.KryoAnnotationTest.class,
   us.ihmc.communication.net.KryoStreamSerializerTest.class,
   us.ihmc.communication.net.local.InterprocessObjectCommunicatorTest.class,
   us.ihmc.communication.remote.DataObjectTransponderTest.class,
   us.ihmc.communication.streamingData.PersistentTCPClientTest.class,
   us.ihmc.communication.streamingData.PersistentTCPServerTest.class,
   us.ihmc.communication.streamingData.StreamingDataProducerConsumerTest.class
})

public class IHMCCommunicationAFastTestSuite
{
   public static void main(String[] args)
   {

   }
}
