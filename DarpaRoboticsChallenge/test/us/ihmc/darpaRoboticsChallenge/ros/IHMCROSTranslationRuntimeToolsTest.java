package us.ihmc.darpaRoboticsChallenge.ros;

import org.junit.Test;
import org.reflections.Reflections;
import org.ros.internal.message.Message;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.tools.testing.TestPlanAnnotations;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Doug Stephen <a href="mailto:dstephen@ihmc.us">(dstephen@ihmc.us)</a>
 */
public class IHMCROSTranslationRuntimeToolsTest
{

   @TestPlanAnnotations.DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 30000)
   public void testBidirectionalConversionWithRandomConstructors()
   {
      Reflections reflections = new Reflections("us.ihmc");
      Set<Class<?>> concreteTypes = new HashSet<>();
      for (Class<?> aClass : reflections.getTypesAnnotatedWith(RosMessagePacket.class))
      {
         if(!Modifier.isAbstract(aClass.getModifiers()))
         {
            concreteTypes.add(aClass);
         }
      }

      Message rosMessage = null;
      Packet<?> ihmcMessage = null;
      Random random = new Random(1976L);

      for(int i = 0; i < 10000; i++)
      {
         for (Class<?> concreteType : concreteTypes)
         {
            Constructor<?> randomConstructor = null;
            try
            {
               randomConstructor = concreteType.getConstructor(Random.class);
               ihmcMessage = (Packet<?>) randomConstructor.newInstance(random);
               rosMessage = IHMCROSTranslationRuntimeTools.convertToRosMessage(ihmcMessage);
               Packet packet = IHMCROSTranslationRuntimeTools.convertToIHMCMessage(rosMessage);
               assertTrue("Problem with packet " + concreteType + ". \n" + ihmcMessage + ", \n" + packet, packet.epsilonEquals(ihmcMessage, 0.1));
            }
            catch (Exception e)
            {
               System.out.println("Conversion failed!");
               System.out.println("Message type: " + concreteType);
               e.printStackTrace();
               fail();
            }
         }
      }
   }
}