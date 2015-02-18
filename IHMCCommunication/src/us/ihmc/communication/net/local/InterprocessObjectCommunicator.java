package us.ihmc.communication.net.local;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import us.ihmc.communication.net.GlobalObjectConsumer;
import us.ihmc.communication.net.NetClassList;
import us.ihmc.communication.net.NetStateListener;
import us.ihmc.communication.net.NetworkedObjectCommunicator;
import us.ihmc.communication.net.ObjectConsumer;

import com.esotericsoftware.kryo.Kryo;

public class InterprocessObjectCommunicator implements NetworkedObjectCommunicator
{
   private final Kryo kryo = new Kryo();
   private final LinkedHashMap<Class<?>, ArrayList<ObjectConsumer<?>>> listeners = new LinkedHashMap<Class<?>, ArrayList<ObjectConsumer<?>>>();
   private final ArrayList<GlobalObjectConsumer> globalListeners = new ArrayList<GlobalObjectConsumer>();
   private final ArrayList<NetStateListener> stateListeners = new ArrayList<NetStateListener>();

   private final int port;

   /**
    * Simple client for Interprocess Communication using objects. Will connect to a predefined port, mimicking TCP/UDP. Easily replaceable by an UDP/TCP implementation, but is traceable in debug.
    *  
    * @param port
    * @param classList for copying objects
    */
   public InterprocessObjectCommunicator(int port, NetClassList classList)
   {
      this.port = port;

      for (Class<?> clazz : classList.getPacketClassList())
      {
         kryo.register(clazz);
         listeners.put(clazz, new ArrayList<ObjectConsumer<?>>());
      }

      for (Class<?> type : classList.getPacketFieldList())
      {
         kryo.register(type);
      }
   }

   @Override
   public void consumeObject(Object object)
   {
      send(object);
   }
   
   @Override
   public int send(Object object)
   {
      return InterprocessCommunicationNetwork.sendObject(this, port, object);
   }
   
   @SuppressWarnings("unchecked")
   /* package-private */void receiveObject(Object object)
   {
      Object copy = kryo.copy(object);
      for (GlobalObjectConsumer listener : globalListeners)
      {
         listener.consumeObject(copy);
      }

      ArrayList<ObjectConsumer<?>> objectListeners = listeners.get(object.getClass());
      if (objectListeners != null)
      {
         for (@SuppressWarnings("rawtypes")
         ObjectConsumer listener : objectListeners)
         {
            listener.consumeObject(copy);
         }
      }
   }

   @Override
   public void attachStateListener(NetStateListener stateListener)
   {
      stateListeners.add(stateListener);
   }

   @Override
   public <T> void attachListener(Class<T> clazz, ObjectConsumer<T> listener)
   {
      if (listeners.containsKey(clazz))
      {
         listeners.get(clazz).add(listener);
      }
      else
      {
         throw new RuntimeException("Class " + clazz.getSimpleName() + " is not registered with ObjectCommunicator");
      }
   }

   @Override
   public <T> void detachListener(Class<T> clazz, ObjectConsumer<T> listener)
   {
      if (listeners.containsKey(clazz))
      {
         ArrayList<ObjectConsumer<?>> listenerList = listeners.get(clazz);
         if (listenerList.contains(listener))
         {
            listenerList.remove(listener);
         }
      }
   }

   @Override
   public void attachGlobalListener(GlobalObjectConsumer listener)
   {
      globalListeners.add(listener);
   }

   @Override
   public void detachGlobalListener(GlobalObjectConsumer listener)
   {
      if (globalListeners.contains(listener))
      {
         globalListeners.remove(listener);
      }
   }

   @Override
   public boolean isConnected()
   {
      return InterprocessCommunicationNetwork.isConnected(this, port);
   }

   @Override
   public void close()
   {
      InterprocessCommunicationNetwork.disconnect(this, port);
   }

   @Override
   public void connect() throws IOException
   {
      InterprocessCommunicationNetwork.connect(this, port);
   }

   /* package-private */void connected()
   {
      for (NetStateListener stateListener : stateListeners)
      {
         stateListener.connected();
      }
   }

   /* package-private */void disconnected()
   {
      for (NetStateListener stateListener : stateListeners)
      {
         stateListener.disconnected();
      }
   }

   @Override
   protected void finalize()
   {
      if(isConnected())
      {
         close();         
      }
   }
}
