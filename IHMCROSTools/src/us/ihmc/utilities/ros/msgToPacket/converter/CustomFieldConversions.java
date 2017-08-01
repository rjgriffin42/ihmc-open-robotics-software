package us.ihmc.utilities.ros.msgToPacket.converter;

import org.ros.internal.message.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CustomFieldConversions
{
   private static CustomFieldConversions instance = null;

   private final Map<Class, Function> classToPacketFieldConveterMap = new HashMap<>();
   private final Map<Class<? extends Message>, Function> classToMessageFieldConverterMap = new HashMap<>();

   private CustomFieldConversions()
   {
      // singleton constructor
   }

   public static CustomFieldConversions getInstance()
   {
      if(instance == null) instance = new CustomFieldConversions();

      return instance;
   }

   public <T, S extends Message> void registerIHMCPacketFieldConverter(Class<T> clazz, Function<T, S> converter)
   {
      classToPacketFieldConveterMap.put(clazz, converter);
   }

   public <T, S extends Message> void registerROSMessageFieldConverter(Class<S> clazz, Function<S, T> converter)
   {
      classToMessageFieldConverterMap.put(clazz, converter);
   }

   public <T, S extends Message> S convert(T field)
   {
      Function<T, S> function = classToPacketFieldConveterMap.get(field.getClass());
      return function.apply(field);
   }

   public <T, S extends Message> T convert(S field)
   {
      Function<S, T> function = classToMessageFieldConverterMap.get(field);
      return function.apply(field);
   }

   public boolean contains(Class<?> clazz)
   {
      return classToPacketFieldConveterMap.containsKey(clazz) || classToMessageFieldConverterMap.containsKey(clazz);
   }
}
