package us.ihmc.darpaRoboticsChallenge.networkProcessor;

import java.io.IOException;
import java.util.ArrayList;

import us.ihmc.communication.PacketRouter;
import us.ihmc.communication.packetCommunicator.interfaces.PacketCommunicator;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.handControl.HandCommandManager;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.RosModule;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.modules.uiConnector.UiConnectionModule;
import us.ihmc.darpaRoboticsChallenge.sensors.DRCSensorSuiteManager;
import us.ihmc.humanoidBehaviors.IHMCHumanoidBehaviorManager;
import us.ihmc.ihmcPerception.IHMCPerceptionManager;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.utilities.io.printing.PrintTools;

public class DRCNetworkProcessor
{
   private final PacketRouter networkProcessor;
   private final boolean DEBUG = false;

   public DRCNetworkProcessor(DRCRobotModel robotModel, DRCNetworkModuleParameters params)
   {
      networkProcessor = new PacketRouter();
      ArrayList<PacketCommunicator> communicators = createRequestedModules(robotModel, params);
      
      for (int i = 0; i < communicators.size(); i++)
      {
         PacketCommunicator packetCommunicator = communicators.get(i);
         if(packetCommunicator != null)
         {
            networkProcessor.attachPacketCommunicator(packetCommunicator);
            connect(packetCommunicator);
         } 
         else if(DEBUG)
         {
            PrintTools.debug(this, "Null Communicator!");
         }
      }
   }

   private ArrayList<PacketCommunicator> createRequestedModules(DRCRobotModel robotModel, DRCNetworkModuleParameters params)
   {
      ArrayList<PacketCommunicator> communicators = new ArrayList<PacketCommunicator>();
      
      if(params.useController())
      {
         PacketCommunicator simulatedControllerCommunicator = params.getControllerCommunicator();
         communicators.add(simulatedControllerCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "useSimulatedController" + simulatedControllerCommunicator.getName() + " " + simulatedControllerCommunicator.getId());
         }
      }
      
      if(params.useUiModule())
      {
         PacketCommunicator uiModuleCommunicator = createUiModule(robotModel, params);
         communicators.add(uiModuleCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "useSensorModule" + uiModuleCommunicator.getName() + " " + uiModuleCommunicator.getId());
         }
      }
      
      if(params.useSensorModule())
      {
         PacketCommunicator sensorModuleCommunicator = createSensorModule(robotModel, params);
         communicators.add(sensorModuleCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "useSensorModule" + sensorModuleCommunicator.getName() + " " + sensorModuleCommunicator.getId());
         }
      }
      
      if(params.usePerceptionModule())
      {
         PacketCommunicator perceptionModuleCommunicator = createPerceptionModule(robotModel);
         communicators.add(perceptionModuleCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "usePerceptionModule" + perceptionModuleCommunicator.getName() + " " + perceptionModuleCommunicator.getId());
         }
      }
      
      if(params.useRosModule())
      {
         PacketCommunicator rosModuleCommunicator = createRosModule(robotModel);
         communicators.add(rosModuleCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "useRosModule" + rosModuleCommunicator.getName() + " " + rosModuleCommunicator.getId());
         }
      }
      
      if(params.useBehaviorModule())
      {
         PacketCommunicator behaviorModuleCommunicator = createBehaviorModule(robotModel, params.useBehaviorVisualizer());
         communicators.add(behaviorModuleCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "useBehaviorModule" + behaviorModuleCommunicator.getName() + " " + behaviorModuleCommunicator.getId());
         }
      }
      
      if(params.useHandModule())
      {
         PacketCommunicator handModuleCommunicator = createHandModule(robotModel);
         communicators.add(handModuleCommunicator);
         if(DEBUG)
         {
            PrintTools.debug(this, "useHandModule" + handModuleCommunicator.getName() + " " + handModuleCommunicator.getId());
         }
      }
      return communicators;
   }
   
   private PacketCommunicator createBehaviorModule(DRCRobotModel robotModel, boolean startYoVariableServer)
   {
      DRCRobotSensorInformation sensorInformation = robotModel.getSensorInformation();
      LogModelProvider logModelProvider = robotModel.getLogModelProvider();
      IHMCHumanoidBehaviorManager behaviorManager = new IHMCHumanoidBehaviorManager(robotModel , logModelProvider, startYoVariableServer, sensorInformation);
      return behaviorManager.getCommunicator();
   }

   private PacketCommunicator createRosModule(DRCRobotModel robotModel)
   {
      RosModule rosModule = new RosModule();
      return rosModule.getCommunicator();
   }

   private PacketCommunicator createPerceptionModule(DRCRobotModel robotModel)
   {
      IHMCPerceptionManager perceptionModule = new IHMCPerceptionManager();
      return perceptionModule.getPerceptionCommunicator();
   }

   private PacketCommunicator createSensorModule(DRCRobotModel robotModel, DRCNetworkModuleParameters params)
   {
      DRCSensorSuiteManager sensorSuiteManager = robotModel.getSensorSuiteManager();
      if(params.useSimulatedSensors())
      {
         sensorSuiteManager.initializeSimulatedSensors(params.getSimulatedSensorCommunicator());
      } 
      else 
      {
         sensorSuiteManager.initializePhysicalSensors(params.getRosUri());
      }
      PacketCommunicator sensorModuleCommunicator = sensorSuiteManager.getProcessedSensorsCommunicator();
      return sensorModuleCommunicator;
   }

   private PacketCommunicator createUiModule(DRCRobotModel robotModel, DRCNetworkModuleParameters params)
   {
      UiConnectionModule uiConnectionModule = new UiConnectionModule();
      return uiConnectionModule.getPacketCommunicator();
   }

   private PacketCommunicator createHandModule(DRCRobotModel robotModel)
   {
      HandCommandManager handCommandModule = robotModel.createHandCommandManager();
      if(handCommandModule != null)
      {
         return handCommandModule.getCommunicator();
      }
      return null;
      //      this.fieldComputerClient.attachListener(HandJointAnglePacket.class, new PacketConsumer<HandJointAnglePacket>()
      //      {
      //         @Override
      //         public void receivedPacket(HandJointAnglePacket object)
      //         {
      //            networkingManager.getControllerStateHandler().sendPacket(object);
      //         }
      //      });
   }

   protected void connect(PacketCommunicator communicator)
   {
      try
      {
         communicator.connect();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void addPacketCommunicatorToRouter(PacketCommunicator packetCommunicator)
   {
      networkProcessor.attachPacketCommunicator(packetCommunicator);
      connect(packetCommunicator);
   }
}
