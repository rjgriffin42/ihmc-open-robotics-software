package us.ihmc.darpaRoboticsChallenge.networkProcessor;

import java.io.IOException;
import java.net.URI;

import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.communication.kryo.IHMCCommunicationKryoNetClassList;
import us.ihmc.communication.packets.manipulation.HandJointAnglePacket;
import us.ihmc.communication.producers.DRCJointConfigurationData;
import us.ihmc.communication.producers.RobotPoseBuffer;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotDataReceiver;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData.DepthDataFilter;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData.RobotBoundingBoxes;
import us.ihmc.darpaRoboticsChallenge.networking.DRCNetworkProcessorNetworkingManager;
import us.ihmc.darpaRoboticsChallenge.sensors.DRCSensorSuiteManager;
import us.ihmc.utilities.net.AtomicSettableTimestampProvider;
import us.ihmc.utilities.net.KryoObjectClient;
import us.ihmc.utilities.net.LocalObjectCommunicator;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.ObjectConsumer;

public class DRCNetworkProcessor
{

   private final boolean useSimulatedSensors;
   private final ObjectCommunicator fieldComputerClient;
   private final AtomicSettableTimestampProvider timestampProvider = new AtomicSettableTimestampProvider();
   private final DRCNetworkProcessorNetworkingManager networkingManager;
   private final SDFFullRobotModel fullRobotModel;
   private final DRCRobotDataReceiver drcRobotDataReceiver;
   private final RobotPoseBuffer robotPoseBuffer;
   private final DepthDataFilter lidarFilter;
   
   /*
    * This will become a stand-alone application in the final competition. Do
    * NOT pass in objects shared with the DRC simulation!
    */
   public DRCNetworkProcessor(URI rosUri, DRCRobotModel robotModel)
   {
      this(rosUri, null, null, robotModel);
   }
   
   public DRCNetworkProcessor(LocalObjectCommunicator objectCommunicator, DRCRobotModel robotModel)
   {
      this(null, objectCommunicator, null, robotModel);
   }

   public DRCNetworkProcessor(LocalObjectCommunicator scsCommunicator, ObjectCommunicator drcNetworkObjectCommunicator, DRCRobotModel robotModel)
   {
      this(null, scsCommunicator, drcNetworkObjectCommunicator, robotModel);
   }
   
   public DRCNetworkProcessor(URI rosUri, LocalObjectCommunicator scsCommunicator, ObjectCommunicator fieldComputerClient, DRCRobotModel robotModel)
   {
      if (fieldComputerClient == null)
      {
         this.fieldComputerClient = new KryoObjectClient(robotModel.getNetworkParameters().getRobotControlComputerIP(), DRCConfigParameters.NETWORK_PROCESSOR_TO_CONTROLLER_TCP_PORT,
               new IHMCCommunicationKryoNetClassList());
         ((KryoObjectClient) this.fieldComputerClient).setReconnectAutomatically(true);
      }
      else
      {
         this.fieldComputerClient = fieldComputerClient;
      }
      
      useSimulatedSensors = !robotModel.isRunningOnRealRobot();
      robotPoseBuffer = new RobotPoseBuffer(this.fieldComputerClient, 1000, timestampProvider);
      networkingManager = new DRCNetworkProcessorNetworkingManager(this.fieldComputerClient, timestampProvider, robotModel);
      fullRobotModel = robotModel.createFullRobotModel();
      drcRobotDataReceiver = new DRCRobotDataReceiver(fullRobotModel);
      RobotBoundingBoxes robotBoundingBoxes = new RobotBoundingBoxes(drcRobotDataReceiver, fullRobotModel);
      lidarFilter = new DepthDataFilter(robotBoundingBoxes, fullRobotModel);
      
      this.fieldComputerClient.attachListener(DRCJointConfigurationData.class, drcRobotDataReceiver);
      this.fieldComputerClient.attachListener(HandJointAnglePacket.class, new ObjectConsumer<HandJointAnglePacket>()
      {
         @Override
         public void consumeObject(HandJointAnglePacket object)
         {
            networkingManager.getControllerStateHandler().sendHandJointAnglePacket(object);
         }
      });

      NetworkProcessorTestbedAlignment testbed = new NetworkProcessorTestbedAlignment(networkingManager);
      networkingManager.getControllerCommandHandler().setTestbed(testbed);
      new Thread(testbed).start();
      
      setSensorManager(robotModel.getSensorSuiteManager(rosUri), scsCommunicator, rosUri);
      connect();
   }

   private void setSensorManager(DRCSensorSuiteManager sensorSuiteManager, LocalObjectCommunicator localObjectCommunicator, URI sensorURI)
   {
      if (useSimulatedSensors)
      {
         sensorSuiteManager.initializeSimulatedSensors(localObjectCommunicator, fieldComputerClient, robotPoseBuffer, networkingManager, fullRobotModel, lidarFilter, sensorURI);
      }
      else
      {
         sensorSuiteManager.initializePhysicalSensors(robotPoseBuffer,networkingManager,fullRobotModel,fieldComputerClient, lidarFilter, sensorURI);
      }
   }
   
   public void connect()
   {
      try
      {
         this.fieldComputerClient.connect();
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
      networkingManager.connect();
   }
}
