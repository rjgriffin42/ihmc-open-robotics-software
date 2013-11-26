package us.ihmc.darpaRoboticsChallenge.ros;

import java.net.URI;
import java.net.URISyntaxException;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import us.ihmc.commonWalkingControlModules.packetConsumers.FingerStateProvider;
import us.ihmc.commonWalkingControlModules.packets.FingerStatePacket;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.ros.RosTools;

public class ROSiRobotCommandDispatcher implements Runnable
{
   private final FingerStateProvider fingerStateProvider = new FingerStateProvider();

   private final ROSiRobotCommunicator rosHandCommunicator;

   public ROSiRobotCommandDispatcher(ObjectCommunicator objectCommunicator, String rosHostIP)
   {
      objectCommunicator.attachListener(FingerStatePacket.class, fingerStateProvider);
      
      String rosURI = "http://" + rosHostIP + ":11311";
      
      rosHandCommunicator = new ROSiRobotCommunicator(rosURI);
      
      try
      {
         NodeConfiguration nodeConfiguration = RosTools.createNodeConfiguration(new URI(rosURI));
         NodeMainExecutor nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
         nodeMainExecutor.execute(rosHandCommunicator, nodeConfiguration);
      }
      catch (URISyntaxException e)
      {
         e.printStackTrace();
      }
   }

   @Override
   public void run()
   {
      while (true)
      {
         if (fingerStateProvider.isNewFingerStateAvailable())
         {
            FingerStatePacket packet = fingerStateProvider.pullPacket();
            rosHandCommunicator.sendHandCommand(packet);
         }
      }
   }
}
