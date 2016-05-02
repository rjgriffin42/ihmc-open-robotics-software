package us.ihmc.humanoidRobotics.communication.packets;

import org.junit.Test;
import us.ihmc.communication.packets.Packet;
import us.ihmc.communication.ros.generators.RosMessagePacket;
import us.ihmc.humanoidRobotics.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestClass;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;
import us.ihmc.tools.testing.TestPlanTarget;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

@DeployableTestClass(targets = TestPlanTarget.CodeQuality)
public class PacketCodeQualityTest
{
	@DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAllPacketFieldsArePublic()
   {
      IHMCCommunicationKryoNetClassList classList = new IHMCCommunicationKryoNetClassList();

      for (Class<?> clazz : classList.getPacketClassList())
      {
         checkIfAllFieldsArePublic(clazz);
      }

      for (Class<?> clazz : classList.getPacketFieldList())
      {
         checkIfAllFieldsArePublic(clazz);
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.1)
   @Test(timeout = 30000)
   public void testAllRosExportedPacketsHaveRandomConstructor()
   {
      IHMCCommunicationKryoNetClassList classList = new IHMCCommunicationKryoNetClassList();
      Set<Class> badClasses = new HashSet<>();

      for (Class<?> clazz : classList.getPacketClassList())
      {
         checkIfClassHasRandomConstructor(clazz, badClasses);
      }

      if(!badClasses.isEmpty())
      {
         System.err.println("PacketCodeQualityTest.checkIfClassHasRandomConstructor failed: The following classes do not have Random constructors:");
         for (Class badClass : badClasses)
         {
            System.err.println("- " + badClass.getCanonicalName());
         }

         fail("PacketCodeQualityTest.checkIfClassHasRandomConstructor failed. Consult Standard Error logs for list of classes without Random constructors.");
      }
   }

   private void checkIfClassHasRandomConstructor(Class<?> clazz, Set<Class> badClasses)
   {
      // Skip base class
      if(clazz == Packet.class)
      {
         return;
      }

      if(Packet.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) && clazz.isAnnotationPresent(RosMessagePacket.class))
      {
         try
         {
            Constructor<?> constructor = clazz.getConstructor(Random.class);
         }
         catch (NoSuchMethodException e)
         {
            badClasses.add(clazz);
         }
      }
   }

   private void checkIfAllFieldsArePublic(Class<?> clazz)
   {
      if (clazz == String.class) return;
      if (clazz == ArrayList.class) return;

      for (Field field : clazz.getDeclaredFields())
      {
         if (Modifier.isStatic(field.getModifiers())) continue;
         if (Modifier.isTransient(field.getModifiers())) continue;
         if (field.isSynthetic()) continue;

         assertTrue("Class " + clazz.getCanonicalName() + " has non-public field " + field.getName() + " declared by " + field.getDeclaringClass().getCanonicalName(), Modifier.isPublic(field.getModifiers()));
         assertFalse("Class " + clazz.getCanonicalName() + " has final field " + field.getName() + " declared by " + field.getDeclaringClass().getCanonicalName(), Modifier.isFinal(field.getModifiers()));
      }
   }
}
