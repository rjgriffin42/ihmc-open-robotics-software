package us.ihmc.communication.net;

import java.util.ArrayList;

public class NetClassList
{
   private final ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
   private final ArrayList<Class<?>> typeList = new ArrayList<Class<?>>();

   public NetClassList()
   {
   }

   public NetClassList(Class<?>... classes)
   {
      registerPacketClasses(classes);
   }

   public void registerPacketClass(Class<?> clazz)
   {
      classList.add(clazz);
   }

   public void registerPacketClasses(Class<?>... classes)
   {
      for (Class<?> clazz : classes)
      {
         registerPacketClass(clazz);
      }
   }
   
   public void registerPacketField(Class<?> type)
   {
      typeList.add(type);
   }
   
   public void registerPacketFields(Class<?>... types)
   {
      for (Class<?> type : types)
      {
         registerPacketField(type);
      }
   }

   /* package-private */ ArrayList<Class<?>> getPacketClassList()
   {
      return classList;
   }

   /* package-private */ ArrayList<Class<?>> getPacketFieldList()
   {
      return typeList;
   }
}
