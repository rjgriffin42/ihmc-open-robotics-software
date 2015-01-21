package us.ihmc.simulationconstructionset.simulationDispatcher.interfaces;

import java.rmi.*;


public interface RemoteIPRegistryInterface extends Remote
{
   public abstract void registerMe(String hostname, String password) throws RemoteException;

   public abstract String[] getAllRegistered(String password) throws RemoteException;
}
