package us.ihmc.humanoidBehaviors.communication;

import java.util.ArrayList;

import javax.swing.SwingUtilities;

import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packets.Packet;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.SimpleCoactiveBehaviorDataPacket;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.WalkToGoalBehaviorPacket;
import us.ihmc.robotics.dataStructures.listener.VariableChangedListener;
import us.ihmc.robotics.dataStructures.variable.YoVariable;

public class CoactiveBehaviorsNetworkManager
{
   private OutgoingCommunicationBridgeInterface outgoingCommunicationBridgeInterface;

   ArrayList<CoactiveDataListenerInterface> listerners = new ArrayList<CoactiveDataListenerInterface>();

   public CoactiveBehaviorsNetworkManager(OutgoingCommunicationBridgeInterface outgoingCommunicationBridgeInterface,
         IncomingCommunicationBridgeInterface incomingCommunicationBridgeInterface)
   {
      this.outgoingCommunicationBridgeInterface = outgoingCommunicationBridgeInterface;

      incomingCommunicationBridgeInterface.attachListener(SimpleCoactiveBehaviorDataPacket.class, new CoactiveDataListener());
   }

   public void registerYovaribleForAutoSendToUI(YoVariable var)
   {
      var.addVariableChangedListener(new VariableChangedListener()
      {
         @Override
         public void variableChanged(YoVariable<?> v)
         {
            sendToUI(v.getName(), v.getValueAsDouble());
         }
      });
   }

   public void registerYovaribleForAutoSendToBehavior(YoVariable var)
   {
      var.addVariableChangedListener(new VariableChangedListener()
      {

         @Override
         public void variableChanged(YoVariable<?> v)
         {
            sendToBehavior(v.getName(), v.getValueAsDouble());
         }
      });
   }

   public void sendToUI(String key, double value)
   {
      SimpleCoactiveBehaviorDataPacket newPacket = new SimpleCoactiveBehaviorDataPacket(key, value);
      outgoingCommunicationBridgeInterface.sendPacketToUI(newPacket);
   }

   public void sendToUI(String key, Object data)
   {
      SimpleCoactiveBehaviorDataPacket newPacket = new SimpleCoactiveBehaviorDataPacket(key, data);
      outgoingCommunicationBridgeInterface.sendPacketToUI(newPacket);
   }

   public void sendToBehavior(String key, double value)
   {
      SimpleCoactiveBehaviorDataPacket newPacket = new SimpleCoactiveBehaviorDataPacket(key, value);
      outgoingCommunicationBridgeInterface.sendPacketToBehavior(newPacket);
   }

   public void sendToBehavior(String key, Object data)
   {
      SimpleCoactiveBehaviorDataPacket newPacket = new SimpleCoactiveBehaviorDataPacket(key, data);
      outgoingCommunicationBridgeInterface.sendPacketToBehavior(newPacket);
   }

   public void sendToBehavior(Packet packet)
   {
      outgoingCommunicationBridgeInterface.sendPacketToBehavior(packet);
   }

   public void sendToUI(Packet packet)
   {
      outgoingCommunicationBridgeInterface.sendPacketToBehavior(packet);
   }

   public void addListeners(CoactiveDataListenerInterface listener)
   {
      listerners.add(listener);
   }
   
   //TODO add the ability to easily listen for any packets

   private void notifyListeners(SimpleCoactiveBehaviorDataPacket packet)
   {
      for (int i = 0; i < listerners.size(); i++)
      {
         listerners.get(i).coactiveDataRecieved(packet);
      }
   }

   private class CoactiveDataListener implements PacketConsumer<SimpleCoactiveBehaviorDataPacket>
   {
      @Override
      public void receivedPacket(final SimpleCoactiveBehaviorDataPacket packet)
      {
         SwingUtilities.invokeLater(new Runnable()
         {
            @Override
            public void run()
            {
               notifyListeners(packet);
            }
         });
      }

   }
}
