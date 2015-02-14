package us.ihmc.communication.net.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.junit.After;
import org.junit.Test;

import us.ihmc.communication.net.NetClassList;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.utilities.code.unitTesting.BambooAnnotations.AverageDuration;

public class InterprocessObjectCommunicatorTest
{
   @AverageDuration(duration = 0.1)
   @Test(timeout = 2000)
   public void testOpeningAndClosingALotOfPorts() throws IOException
   {
      TestNetClassList classList = new TestNetClassList();
      ArrayList<InterprocessObjectCommunicator> communicators = new ArrayList<>();
      for (int i = 0; i < 2560; i += 20)
      {
         InterprocessObjectCommunicator communicator = new InterprocessObjectCommunicator(i, classList);
         communicator.connect();
         communicators.add(communicator);
      }
      assertTrue("Hashmap is empty", InterprocessCommunicationNetwork.hasMap());
      assertEquals("Open ports does not equal number of communicators", communicators.size(), InterprocessCommunicationNetwork.getOpenPorts());
      for (int i = 0; i < communicators.size(); i++)
      {
         communicators.get(i).close();
      }

      assertEquals("Open ports does not equal zero", 0, InterprocessCommunicationNetwork.getOpenPorts());
      assertFalse("Hashmap is not cleaned up", InterprocessCommunicationNetwork.hasMap());

   }

   @AverageDuration(duration = 0.1)
   @Test(timeout = 5000)
   public void testSendingObjectsToClients() throws IOException
   {
      InterprocessObjectCommunicator port128ClientA = new InterprocessObjectCommunicator(128, new TestNetClassList());
      InterprocessObjectCommunicator port128ClientB = new InterprocessObjectCommunicator(128, new TestNetClassList());
      InterprocessObjectCommunicator port128ClientDisconnected = new InterprocessObjectCommunicator(128, new TestNetClassList());
      InterprocessObjectCommunicator port256Client = new InterprocessObjectCommunicator(256, new TestNetClassList());

      // Check for not getting packets back I sent
      port128ClientA.attachListener(MutableInt.class, new FailConsumer<MutableInt>());
      // Check if no packets arrive at a disconnected listener
      port128ClientDisconnected.attachListener(MutableInt.class, new FailConsumer<MutableInt>());
      // Check if no packets arrive at a different port
      port256Client.attachListener(MutableInt.class, new FailConsumer<MutableInt>());
      // Check if no packets of the wrong type get received
      port128ClientB.attachListener(MutableDouble.class, new FailConsumer<MutableDouble>());

      final AtomicReference<MutableInt> reference = new AtomicReference<>();
      port128ClientB.attachListener(MutableInt.class, new ObjectConsumer<MutableInt>()
      {
         Random random = new Random(1511358L);

         @Override
         public void consumeObject(MutableInt object)
         {
            assertEquals(random.nextInt(), object.intValue());
            assertEquals(reference.get().intValue(), object.intValue());
            assertFalse(object == reference.get());
         }
      });

      port128ClientA.connect();
      port128ClientB.connect();
      port256Client.connect();

      Random random = new Random(1511358L);
      int iterations = 1200000;
      for (int i = 0; i < iterations; i++)
      {
         MutableInt object = new MutableInt(random.nextInt());
         reference.set(object);
         port128ClientA.consumeObject(object);
      }

      port128ClientA.close();
      port128ClientB.close();
      port256Client.close();

      assertEquals("Open ports does not equal zero", 0, InterprocessCommunicationNetwork.getOpenPorts());
      assertFalse("Hashmap is not cleaned up", InterprocessCommunicationNetwork.hasMap());

   }

   @After
   public void closeNetwork()
   {
      InterprocessCommunicationNetwork.closeAllConnectionsForMyJUnitTests();
   }

   private final class FailConsumer<T extends Number> implements ObjectConsumer<T>
   {
      @Override
      public void consumeObject(T object)
      {
         fail();
      }
   }

   private static class TestNetClassList extends NetClassList
   {
      public TestNetClassList()
      {
         registerPacketClass(MutableInt.class);
         registerPacketClass(MutableDouble.class);
      }
   }
}
