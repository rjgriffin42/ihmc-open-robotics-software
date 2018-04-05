package us.ihmc.quadrupedRobotics.communication;

import org.junit.Test;
import org.reflections.Reflections;
import us.ihmc.communication.packets.Packet;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.idl.PreallocatedList;
import us.ihmc.idl.RecyclingArrayListPubSub;
import us.ihmc.pubsub.TopicDataType;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class QuadrupedNetClassListTest
{
   public static void main(String[] args)
   {
      Reflections ref = new Reflections();
      Set<Class<? extends TopicDataType>> subTypesOf = ref.getSubTypesOf(TopicDataType.class);

      for (Class<? extends TopicDataType> subTypeOf : subTypesOf)
         System.out.println("                         registerPacketField(" + subTypeOf.getSimpleName() + ".class);");
   }

   @ContinuousIntegrationTest(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testAllClassesRegisteredArePackets()
   {
      QuadrupedNetClassList netClassList = new QuadrupedNetClassList();
      ArrayList<Class<?>> packetClassList = netClassList.getPacketClassList();

      for (Class<?> packetClass : packetClassList)
      {
         assertTrue("The class " + packetClass.getSimpleName() + " is not a packet", Packet.class.isAssignableFrom(packetClass));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.5)
   @Test(timeout = 30000)
   public void testAllPacketFieldsAreRegistered()
         throws InstantiationException, IllegalAccessException, NoSuchFieldException, SecurityException, IllegalArgumentException
   {
      QuadrupedNetClassList netClassList = new QuadrupedNetClassList();
      ArrayList<Class<?>> packetClassList = netClassList.getPacketClassList();
      Set<Class<?>> packetFieldSet = new HashSet<>(netClassList.getPacketFieldList());
      packetClassList.remove(Packet.class);

      for (Class<?> packetClass : packetClassList)
      {
         for (Field field : packetClass.getDeclaredFields())
         {
            assertAllFieldsAreInSetRecursively(packetClass.newInstance(), field, packetFieldSet);
         }
      }
   }

   private static void assertAllFieldsAreInSetRecursively(Object holder, Field field, Set<Class<?>> setWithRegisteredFields)
         throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException
   {
      if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive())
         return;

      Class<?> typeToCheck = field.getType();
      field.setAccessible(true);
      Object fieldInstance;

      if (holder != null)
      {
         fieldInstance = field.get(holder);
      }
      else if (Packet.class.isAssignableFrom(field.getDeclaringClass()))
      {
         holder = field.getDeclaringClass().newInstance();
         fieldInstance = field.get(holder);
      }
      else if (Packet.class.isAssignableFrom(field.getType()))
      {
         fieldInstance = field.getType().newInstance();
      }
      else
      {
         fieldInstance = null;
      }

      if (typeToCheck.isPrimitive() || typeToCheck == Class.class || typeToCheck == String.class)
         return;

      if (PreallocatedList.class.isAssignableFrom(typeToCheck))
      {
         if (fieldInstance == null)
            return;
         Field listClassField = typeToCheck.getDeclaredField("clazz");
         listClassField.setAccessible(true);
         typeToCheck = (Class<?>) listClassField.get(fieldInstance);
         fieldInstance = null;
         
         { // Also need to check the array version of the class
            Class<?> arrayVersion = Array.newInstance(typeToCheck, 1).getClass();
            assertTrue("The class " + arrayVersion.getSimpleName() + " is not registered.", setWithRegisteredFields.contains(arrayVersion));
         }
      }

      assertTrue("The field " + field.getDeclaringClass().getSimpleName() + "." + field.getName() + " is not registered, field type: " + field.getType() + ".",
                 setWithRegisteredFields.contains(typeToCheck));

      while (typeToCheck.isArray())
         typeToCheck = typeToCheck.getComponentType();

      if (Enum.class.isAssignableFrom(typeToCheck))
         return;
      if (ArrayList.class.isAssignableFrom(typeToCheck))
         return;
      if (RecyclingArrayListPubSub.class.isAssignableFrom(typeToCheck))
         return;

      for (Field subField : typeToCheck.getDeclaredFields())
      {
         if (subField.getType() == field.getType())
            continue;

         assertAllFieldsAreInSetRecursively(fieldInstance, subField, setWithRegisteredFields);
      }
   }
}
