package us.ihmc.darpaRoboticsChallenge.networkProcessor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import us.ihmc.SdfLoader.JaxbSDFLoader;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCLocalConfigParameters;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModel;
import us.ihmc.darpaRoboticsChallenge.DRCRobotModelFactory;
import us.ihmc.darpaRoboticsChallenge.DRCRobotSDFLoader;
import us.ihmc.darpaRoboticsChallenge.configuration.DRCNetClassList;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotDataReceiver;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCSensorParameters;
import us.ihmc.darpaRoboticsChallenge.handControl.packetsAndConsumers.HandJointAnglePacket;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.camera.CameraInfoReceiver;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.camera.FishEyeDataReceiver;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.camera.GazeboCameraReceiver;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.camera.MultiSenseCameraInfoReciever;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.camera.SCSCameraDataReceiver;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.lidar.GazeboLidarDataReceiver;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.lidar.RobotBoundingBoxes;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.lidar.SCSLidarDataReceiver;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.ros.RosNativeNetworkProcessor;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.state.RobotPoseBuffer;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.time.AlwaysZeroOffsetPPSTimestampOffsetProvider;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.time.PPSTimestampOffsetProvider;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.time.RealRobotPPSTimestampOffsetProvider;
import us.ihmc.darpaRoboticsChallenge.networking.DRCNetworkProcessorNetworkingManager;
import us.ihmc.darpaRoboticsChallenge.networking.dataProducers.DRCJointConfigurationData;
import us.ihmc.graphics3DAdapter.camera.VideoSettings;
import us.ihmc.graphics3DAdapter.camera.VideoSettingsFactory;
import us.ihmc.utilities.net.AtomicSettableTimestampProvider;
import us.ihmc.utilities.net.KryoObjectClient;
import us.ihmc.utilities.net.LocalObjectCommunicator;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.ObjectConsumer;
import us.ihmc.utilities.ros.RosMainNode;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

public class DRCNetworkProcessor
{
   private final VideoSettings videoSettings;

   private final ObjectCommunicator fieldComputerClient;
   private final AtomicSettableTimestampProvider timestampProvider = new AtomicSettableTimestampProvider();
   private final DRCNetworkProcessorNetworkingManager networkingManager;
   private final RobotPoseBuffer robotPoseBuffer;

   private final PPSTimestampOffsetProvider ppsTimestampOffsetProvider;

   private final SDFFullRobotModel fullRobotModel;
   private final DRCRobotJointMap jointMap;
   private final RobotBoundingBoxes robotBoundingBoxes;

   private static String scsMachineIPAddress = DRCLocalConfigParameters.ROBOT_CONTROLLER_IP_ADDRESS;
   private static String rosMasterURI = DRCConfigParameters.ROS_MASTER_URI;

   /*
    * This will become a stand-alone application in the final competition. Do
    * NOT pass in objects shared with the DRC simulation!
    */
   public DRCNetworkProcessor(URI rosCoreURI, DRCRobotModel robotModel)
   {
      this(rosCoreURI, null, robotModel);
   }

   public DRCNetworkProcessor(URI rosCoreURI, ObjectCommunicator drcNetworkObjectCommunicator, DRCRobotModel robotModel)
   {
      this(drcNetworkObjectCommunicator, robotModel);

      System.out.println("Connecting to ROS");

      if(DRCLocalConfigParameters.ENABLE_CAMERA_AND_LIDAR)
      {
         RosMainNode rosMainNode;
         rosMainNode = new RosMainNode(rosCoreURI, "darpaRoboticsChallange/networkProcessor");
         
         RosNativeNetworkProcessor rosNativeNetworkProcessor;
         if (RosNativeNetworkProcessor.hasNativeLibrary())
         {
            rosNativeNetworkProcessor = RosNativeNetworkProcessor.getInstance(rosCoreURI.toString());
            rosNativeNetworkProcessor.connect();
         }
         else
         {
            rosNativeNetworkProcessor = null;
         }
         GazeboCameraReceiver cameraReceiver = new GazeboCameraReceiver(robotPoseBuffer, videoSettings, rosMainNode, networkingManager,ppsTimestampOffsetProvider);
         CameraInfoReceiver cameraInfoServer = new MultiSenseCameraInfoReciever(rosMainNode, networkingManager.getControllerStateHandler());
         networkingManager.getControllerCommandHandler().setIntrinsicServer(cameraInfoServer);
         
         new GazeboLidarDataReceiver(rosMainNode, robotPoseBuffer, networkingManager, fullRobotModel, robotBoundingBoxes,
               jointMap, fieldComputerClient, rosNativeNetworkProcessor, ppsTimestampOffsetProvider);
         
         new FishEyeDataReceiver(robotPoseBuffer, videoSettings, rosMainNode, networkingManager, DRCSensorParameters.DEFAULT_FIELD_OF_VIEW, ppsTimestampOffsetProvider);
         
         ppsTimestampOffsetProvider.attachToRosMainNode(rosMainNode);
         
         
         rosMainNode.execute();
         
         if(DRCConfigParameters.CALIBRATE_ARM_MODE)
         {
            new ArmCalibrationHelper(fieldComputerClient, networkingManager, cameraReceiver);
         }
      }
      else
      {
         System.err.println("WARNING: DRCLocalConfigParameters: Camera and LIDAR disabled.");
      }


      //      if (DRCConfigParameters.USE_DUMMY_DRIVNG)
      //      {
      //         DrivingProcessorFactory.createCheatingDrivingProcessor(networkingManager, cameraDataReceiver, timestampProvider, rosCoreURI.toString(),
      //                 transformForDrivingProviderListener);
      //      }
      //      else
      //      {
      //         DrivingProcessorFactory.createDrivingProcessor(networkingManager, cameraDataReceiver, timestampProvider, fieldComputerClient,
      //                 transformForDrivingProviderListener);
      //      }
      
      connect();
   }

   public DRCNetworkProcessor(LocalObjectCommunicator scsCommunicator, ObjectCommunicator drcNetworkObjectCommunicator, DRCRobotModel robotModel)
   {
      this(drcNetworkObjectCommunicator, robotModel);
      SCSCameraDataReceiver cameraReceiver = new SCSCameraDataReceiver(robotPoseBuffer, videoSettings, scsCommunicator, networkingManager,
            ppsTimestampOffsetProvider);
      new SCSLidarDataReceiver(robotPoseBuffer, scsCommunicator, networkingManager, fullRobotModel, robotBoundingBoxes, jointMap, fieldComputerClient,
            ppsTimestampOffsetProvider);
      
      if(DRCConfigParameters.CALIBRATE_ARM_MODE)
      {
         new ArmCalibrationHelper(fieldComputerClient, networkingManager, cameraReceiver);
      }
      
      connect();

   }

   private DRCNetworkProcessor(ObjectCommunicator fieldComputerClientL, DRCRobotModel robotModel)
   {
      if (fieldComputerClientL == null)
      {
         this.fieldComputerClient = new KryoObjectClient(scsMachineIPAddress, DRCConfigParameters.NETWORK_PROCESSOR_TO_CONTROLLER_TCP_PORT,
               new DRCNetClassList());
         ((KryoObjectClient) this.fieldComputerClient).setReconnectAutomatically(true);
      }
      else
      {
         this.fieldComputerClient = fieldComputerClientL;
      }

      robotPoseBuffer = new RobotPoseBuffer(this.fieldComputerClient, 1000, timestampProvider);
      networkingManager = new DRCNetworkProcessorNetworkingManager(this.fieldComputerClient, timestampProvider, robotModel);

      //DRCRobotModel robotModel = DRCLocalConfigParameters.robotModelToUse;
      jointMap = robotModel.getJointMap();
      JaxbSDFLoader loader = DRCRobotSDFLoader.loadDRCRobot(jointMap, true);
      fullRobotModel = loader.createFullRobotModel(jointMap);
      
      DRCRobotDataReceiver drcRobotDataReceiver = new DRCRobotDataReceiver(robotModel, fullRobotModel);
      this.fieldComputerClient.attachListener(DRCJointConfigurationData.class, drcRobotDataReceiver);
      robotBoundingBoxes = new RobotBoundingBoxes(drcRobotDataReceiver, fullRobotModel, robotModel);

      this.fieldComputerClient.attachListener(HandJointAnglePacket.class,new ObjectConsumer<HandJointAnglePacket>()
      {
         @Override
         public void consumeObject(HandJointAnglePacket object)
         {
            networkingManager.getControllerStateHandler().sendHandJointAnglePacket(object); }
      });
      
      

      if (DRCLocalConfigParameters.USING_REAL_HEAD)
      {
         ppsTimestampOffsetProvider = new RealRobotPPSTimestampOffsetProvider();
         videoSettings = VideoSettingsFactory.get32kBitSettingsWide();
      }
      else
      {
         ppsTimestampOffsetProvider = new AlwaysZeroOffsetPPSTimestampOffsetProvider();
         videoSettings = VideoSettingsFactory.get32kBitSettingsSquare();
      }
   }

   private void connect()
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

   public static void main(String[] args) throws URISyntaxException, JSAPException
   {
      JSAP jsap = new JSAP();
      FlaggedOption scsIPFlag = new FlaggedOption("scs-ip").setLongFlag("scs-ip").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false)
            .setStringParser(JSAP.STRING_PARSER);
      FlaggedOption rosURIFlag = new FlaggedOption("ros-uri").setLongFlag("ros-uri").setShortFlag(JSAP.NO_SHORTFLAG).setRequired(false)
            .setStringParser(JSAP.STRING_PARSER);
      Switch simulateController = new Switch("simulate-controller").setShortFlag('d').setLongFlag(JSAP.NO_LONGFLAG);

      FlaggedOption robotModel = new FlaggedOption("robotModel").setLongFlag("model").setShortFlag('m').setRequired(true)
            .setStringParser(JSAP.STRING_PARSER);
      
      robotModel.setHelp("Robot models: " + DRCRobotModelFactory.robotModelsToString());
      jsap.registerParameter(robotModel);
      
      jsap.registerParameter(scsIPFlag);
      jsap.registerParameter(rosURIFlag);
      jsap.registerParameter(simulateController);

      JSAPResult config = jsap.parse(args);

      if (config.success())
      {
         if (config.getString(scsIPFlag.getID()) != null)
         {
            scsMachineIPAddress = config.getString(scsIPFlag.getID());
         }

         if (config.getString(rosURIFlag.getID()) != null)
         {
            rosMasterURI = config.getString(rosURIFlag.getID());
         }
         
         DRCRobotModel model;
         try
         {
            model = DRCRobotModelFactory.CreateDRCRobotModel(config.getString("robotModel"));
         }
         catch(IllegalArgumentException e)
         {
            System.err.println("Incorrect robot model " + config.getString("robotModel"));
            System.out.println(jsap.getHelp());
            return;
         }
         

         if (config.getBoolean(simulateController.getID()))
         {
            System.err.println("WARNING WARNING WARNING :: Simulating DRC Controller - WILL NOT WORK ON REAL ROBOT. Do not use -d argument when running on real robot.");
            ObjectCommunicator objectCommunicator = new LocalObjectCommunicator();

            new DummyController(rosMasterURI, objectCommunicator, model);
            new DRCNetworkProcessor(new URI(rosMasterURI), objectCommunicator, model);
         }
         else
         {
            new DRCNetworkProcessor(new URI(rosMasterURI), model);
         }
      }
      else
      {
         System.err.println("Invalid parameters");
         System.out.println(jsap.getHelp());
         return;
      }
   }
}
