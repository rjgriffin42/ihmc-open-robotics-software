package us.ihmc.communication.net;

import us.ihmc.communication.interfaces.Connectable;

public interface ObjectCommunicator extends GlobalObjectConsumer, Connectable
{
   public abstract void attachStateListener(ConnectionStateListener stateListener);

   public abstract <T> void attachListener(Class<T> clazz, ObjectConsumer<T> listener);

   public abstract <T> void detachListener(Class<T> clazz, ObjectConsumer<T> listener);

   public abstract void attachGlobalListener(GlobalObjectConsumer listener);

   public abstract void detachGlobalListener(GlobalObjectConsumer listener);
}