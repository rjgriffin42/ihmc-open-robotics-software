package us.ihmc.humanoidBehaviors;

import java.io.IOException;
import java.util.Arrays;

import controller_msgs.msg.dds.BehaviorControlModePacketPubSubType;
import controller_msgs.msg.dds.CapturabilityBasedStatusPubSubType;
import controller_msgs.msg.dds.HumanoidBehaviorTypePacketPubSubType;
import controller_msgs.msg.dds.RobotConfigurationDataPubSubType;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidBehaviors.behaviors.behaviorServices.FiducialDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.behaviors.behaviorServices.ObjectDetectorBehaviorService;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.BasicPipeLineBehavior;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.BasicStateMachineBehavior;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.FireFighterStanceBehavior;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.PickUpBallBehaviorStateMachine;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.RepeatedlyWalkFootstepListBehavior;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.ResetRobotBehavior;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.TurnValveBehaviorStateMachine;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.WalkThroughDoorBehavior;
import us.ihmc.humanoidBehaviors.behaviors.complexBehaviors.WalkToGoalBehavior;
import us.ihmc.humanoidBehaviors.behaviors.debug.PartialFootholdBehavior;
import us.ihmc.humanoidBehaviors.behaviors.debug.TestGarbageGenerationBehavior;
import us.ihmc.humanoidBehaviors.behaviors.debug.TestICPOptimizationBehavior;
import us.ihmc.humanoidBehaviors.behaviors.debug.TestSmoothICPPlannerBehavior;
import us.ihmc.humanoidBehaviors.behaviors.diagnostic.DiagnosticBehavior;
import us.ihmc.humanoidBehaviors.behaviors.examples.ExampleComplexBehaviorStateMachine;
import us.ihmc.humanoidBehaviors.behaviors.fiducialLocation.FollowFiducialBehavior;
import us.ihmc.humanoidBehaviors.behaviors.goalLocation.LocateGoalBehavior;
import us.ihmc.humanoidBehaviors.behaviors.primitives.AtlasPrimitiveActions;
import us.ihmc.humanoidBehaviors.behaviors.roughTerrain.CollaborativeBehavior;
import us.ihmc.humanoidBehaviors.behaviors.roughTerrain.WalkOverTerrainStateMachineBehavior;
import us.ihmc.humanoidBehaviors.dispatcher.BehaviorControlModeSubscriber;
import us.ihmc.humanoidBehaviors.dispatcher.BehaviorDispatcher;
import us.ihmc.humanoidBehaviors.dispatcher.HumanoidBehaviorTypeSubscriber;
import us.ihmc.humanoidBehaviors.utilities.CapturePointUpdatable;
import us.ihmc.humanoidBehaviors.utilities.WristForceSensorFilteredUpdatable;
import us.ihmc.humanoidRobotics.communication.packets.behaviors.HumanoidBehaviorType;
import us.ihmc.humanoidRobotics.communication.subscribers.CapturabilityBasedStatusSubscriber;
import us.ihmc.humanoidRobotics.communication.subscribers.HumanoidRobotDataReceiver;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.multicastLogDataProtocol.modelLoaders.LogModelProvider;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.robotDataLogger.YoVariableServer;
import us.ihmc.robotDataLogger.logger.LogSettings;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.sensorProcessing.parameters.DRCRobotSensorInformation;
import us.ihmc.util.PeriodicNonRealtimeThreadSchedulerFactory;
import us.ihmc.util.PeriodicThreadSchedulerFactory;
import us.ihmc.wholeBodyController.WholeBodyControllerParameters;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;
import us.ihmc.yoVariables.variable.YoFrameConvexPolygon2D;

public class IHMCHumanoidBehaviorManager
{
   public static final double BEHAVIOR_YO_VARIABLE_SERVER_DT = 0.01;

   private static double runAutomaticDiagnosticTimeToWait = Double.NaN;

   private final Ros2Node ros2Node = ROS2Tools.createRos2Node(PubSubImplementation.FAST_RTPS, "ihmc_humanoid_behavior_node");

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final YoDouble yoTime = new YoDouble("yoTime", registry);

   private YoVariableServer yoVariableServer = null;

   public IHMCHumanoidBehaviorManager(WholeBodyControllerParameters wholeBodyControllerParameters, FullHumanoidRobotModelFactory robotModelFactory,
                                      LogModelProvider modelProvider, boolean startYoVariableServer, DRCRobotSensorInformation sensorInfo)
         throws IOException
   {
      this(wholeBodyControllerParameters, robotModelFactory, modelProvider, startYoVariableServer, sensorInfo, false);
   }

   public static void setAutomaticDiagnosticTimeToWait(double timeToWait)
   {
      runAutomaticDiagnosticTimeToWait = timeToWait;
   }

   private IHMCHumanoidBehaviorManager(WholeBodyControllerParameters wholeBodyControllerParameters, FullHumanoidRobotModelFactory robotModelFactory,
                                       LogModelProvider modelProvider, boolean startYoVariableServer, DRCRobotSensorInformation sensorInfo,
                                       boolean runAutomaticDiagnostic)
         throws IOException
   {
      System.out.println(PrintTools.INFO + getClass().getSimpleName() + ": Initializing");

      if (startYoVariableServer)
      {
         PeriodicThreadSchedulerFactory scheduler = new PeriodicNonRealtimeThreadSchedulerFactory();
         yoVariableServer = new YoVariableServer(getClass(), scheduler, modelProvider, LogSettings.BEHAVIOR, BEHAVIOR_YO_VARIABLE_SERVER_DT);
      }

      FullHumanoidRobotModel fullRobotModel = robotModelFactory.createFullRobotModel();

      YoGraphicsListRegistry yoGraphicsListRegistry = new YoGraphicsListRegistry();
      yoGraphicsListRegistry.setYoGraphicsUpdatedRemotely(false);
      ForceSensorDataHolder forceSensorDataHolder = new ForceSensorDataHolder(Arrays.asList(fullRobotModel.getForceSensorDefinitions()));
      HumanoidRobotDataReceiver robotDataReceiver = new HumanoidRobotDataReceiver(fullRobotModel, forceSensorDataHolder);

      HumanoidReferenceFrames referenceFrames = robotDataReceiver.getReferenceFrames();
      ROS2Tools.createCallbackSubscription(ros2Node, new RobotConfigurationDataPubSubType(), "/ihmc/robot_configuration_data",
                                           s -> robotDataReceiver.receivedPacket(s.readNextData()));

      BehaviorControlModeSubscriber desiredBehaviorControlSubscriber = new BehaviorControlModeSubscriber();
      HumanoidBehaviorTypeSubscriber desiredBehaviorSubscriber = new HumanoidBehaviorTypeSubscriber();

      BehaviorDispatcher<HumanoidBehaviorType> dispatcher = new BehaviorDispatcher<>(yoTime, robotDataReceiver, desiredBehaviorControlSubscriber,
                                                                                     desiredBehaviorSubscriber, ros2Node, yoVariableServer,
                                                                                     HumanoidBehaviorType.class, HumanoidBehaviorType.STOP, registry,
                                                                                     yoGraphicsListRegistry);

      CapturabilityBasedStatusSubscriber capturabilityBasedStatusSubsrciber = new CapturabilityBasedStatusSubscriber();
      ROS2Tools.createCallbackSubscription(ros2Node, new CapturabilityBasedStatusPubSubType(), "/ihmc/capturability_based_status",
                                           s -> capturabilityBasedStatusSubsrciber.receivedPacket(s.readNextData()));

      CapturePointUpdatable capturePointUpdatable = new CapturePointUpdatable(capturabilityBasedStatusSubsrciber, yoGraphicsListRegistry, registry);
      dispatcher.addUpdatable(capturePointUpdatable);

      //      YoDouble minIcpDistanceToSupportPolygon = capturePointUpdatable.getMinIcpDistanceToSupportPolygon();
      //      YoDouble icpError = capturePointUpdatable.getIcpError();

      SideDependentList<WristForceSensorFilteredUpdatable> wristSensorUpdatables = null;
      if (sensorInfo.getWristForceSensorNames() != null && !sensorInfo.getWristForceSensorNames().containsValue(null))
      {
         wristSensorUpdatables = new SideDependentList<>();
         for (RobotSide robotSide : RobotSide.values)
         {
            WristForceSensorFilteredUpdatable wristSensorUpdatable = new WristForceSensorFilteredUpdatable(robotSide, fullRobotModel, sensorInfo,
                                                                                                           forceSensorDataHolder,
                                                                                                           BEHAVIOR_YO_VARIABLE_SERVER_DT, ros2Node, registry);
            wristSensorUpdatables.put(robotSide, wristSensorUpdatable);
            dispatcher.addUpdatable(wristSensorUpdatable);
         }
      }

      if (runAutomaticDiagnostic && !Double.isNaN(runAutomaticDiagnosticTimeToWait) && !Double.isInfinite(runAutomaticDiagnosticTimeToWait))
      {
         createAndRegisterAutomaticDiagnostic(dispatcher, fullRobotModel, referenceFrames, yoTime, ros2Node, capturePointUpdatable,
                                              wholeBodyControllerParameters, runAutomaticDiagnosticTimeToWait, yoGraphicsListRegistry);
      }
      else
      {
         createAndRegisterBehaviors(dispatcher, modelProvider, fullRobotModel, robotModelFactory, wristSensorUpdatables, referenceFrames, yoTime, ros2Node,
                                    yoGraphicsListRegistry, capturePointUpdatable, wholeBodyControllerParameters);
      }

      dispatcher.finalizeStateMachine();
      ROS2Tools.createCallbackSubscription(ros2Node, new BehaviorControlModePacketPubSubType(), "/ihmc/behavior_control_mode",
                                           s -> desiredBehaviorControlSubscriber.receivedPacket(s.readNextData()));
      ROS2Tools.createCallbackSubscription(ros2Node, new HumanoidBehaviorTypePacketPubSubType(), "/ihmc/humanoid_behavior_type",
                                           s -> desiredBehaviorSubscriber.receivedPacket(s.readNextData()));

      if (startYoVariableServer)
      {
         yoVariableServer.setMainRegistry(registry, fullRobotModel.getElevator(), yoGraphicsListRegistry);
         yoVariableServer.start();
      }

      dispatcher.start();
   }

   /**
    * Create the different behaviors and register them in the dispatcher. When creating a new
    * behavior, that's where you need to add it.
    * 
    * @param fullRobotModel Holds the robot data (like joint angles). The data is updated in the
    *           dispatcher and can be shared with the behaviors.
    * @param referenceFrames Give access to useful references related to the robot. They're
    *           automatically updated.
    * @param yoTime Holds the controller time. It is updated in the dispatcher and can be shared
    *           with the behaviors.
    * @param ros2Node used to send packets to the controller.
    * @param yoGraphicsListRegistry Allows to register YoGraphics that will be displayed in SCS.
    * @param wholeBodyControllerParameters
    * @param wristSensors Holds the force sensor data
    */
   private void createAndRegisterBehaviors(BehaviorDispatcher<HumanoidBehaviorType> dispatcher, LogModelProvider logModelProvider,
                                           FullHumanoidRobotModel fullRobotModel, FullHumanoidRobotModelFactory robotModelFactory,
                                           SideDependentList<WristForceSensorFilteredUpdatable> wristSensors, HumanoidReferenceFrames referenceFrames,
                                           YoDouble yoTime, Ros2Node ros2Node, YoGraphicsListRegistry yoGraphicsListRegistry,
                                           CapturePointUpdatable capturePointUpdatable, WholeBodyControllerParameters wholeBodyControllerParameters)
   {

      WalkingControllerParameters walkingControllerParameters = wholeBodyControllerParameters.getWalkingControllerParameters();
      AtlasPrimitiveActions atlasPrimitiveActions = new AtlasPrimitiveActions(ros2Node, fullRobotModel, robotModelFactory, referenceFrames, yoTime,
                                                                              wholeBodyControllerParameters, registry);
      YoBoolean yoDoubleSupport = capturePointUpdatable.getYoDoubleSupport();
      YoEnum<RobotSide> yoSupportLeg = capturePointUpdatable.getYoSupportLeg();
      YoFrameConvexPolygon2D yoSupportPolygon = capturePointUpdatable.getYoSupportPolygon();

      // CREATE SERVICES
      FiducialDetectorBehaviorService fiducialDetectorBehaviorService = new FiducialDetectorBehaviorService(ros2Node, yoGraphicsListRegistry);
      fiducialDetectorBehaviorService.setTargetIDToLocate(50);
      dispatcher.addBehaviorService(fiducialDetectorBehaviorService);

      ObjectDetectorBehaviorService objectDetectorBehaviorService = null;
      try
      {
         objectDetectorBehaviorService = new ObjectDetectorBehaviorService(ros2Node, yoGraphicsListRegistry);
         dispatcher.addBehaviorService(objectDetectorBehaviorService);
      }
      catch (Exception e)
      {
         System.err.println("Error creating valve detection behavior service");
         e.printStackTrace();
      }

      //      dispatcher.addBehavior(HumanoidBehaviorType.PICK_UP_BALL,
      //            new PickUpBallBehavior(behaviorCommunicationBridge, yoTime, yoDoubleSupport, fullRobotModel, referenceFrames, wholeBodyControllerParameters));

      dispatcher.addBehavior(HumanoidBehaviorType.FIRE_FIGHTING,
                             new FireFighterStanceBehavior("fireFighting", yoTime, ros2Node, fullRobotModel, referenceFrames, wholeBodyControllerParameters,
                                                           atlasPrimitiveActions));

      dispatcher.addBehavior(HumanoidBehaviorType.PICK_UP_BALL,
                             new PickUpBallBehaviorStateMachine(ros2Node, yoTime, yoDoubleSupport, fullRobotModel, referenceFrames,
                                                                wholeBodyControllerParameters, atlasPrimitiveActions));

      dispatcher.addBehavior(HumanoidBehaviorType.RESET_ROBOT, new ResetRobotBehavior(ros2Node, yoTime));

      dispatcher.addBehavior(HumanoidBehaviorType.TURN_VALVE,
                             new TurnValveBehaviorStateMachine(ros2Node, yoTime, yoDoubleSupport, fullRobotModel, referenceFrames,
                                                               wholeBodyControllerParameters, atlasPrimitiveActions));

      dispatcher.addBehavior(HumanoidBehaviorType.WALK_THROUGH_DOOR,
                             new WalkThroughDoorBehavior(ros2Node, yoTime, yoDoubleSupport, fullRobotModel, referenceFrames, wholeBodyControllerParameters,
                                                         atlasPrimitiveActions));

      dispatcher.addBehavior(HumanoidBehaviorType.DEBUG_PARTIAL_FOOTHOLDS, new PartialFootholdBehavior(ros2Node));

      dispatcher.addBehavior(HumanoidBehaviorType.TEST_ICP_OPTIMIZATION, new TestICPOptimizationBehavior(ros2Node, referenceFrames, yoTime));

      dispatcher.addBehavior(HumanoidBehaviorType.TEST_GC_GENERATION, new TestGarbageGenerationBehavior(ros2Node, referenceFrames, yoTime));

      dispatcher.addBehavior(HumanoidBehaviorType.TEST_SMOOTH_ICP_PLANNER,
                             new TestSmoothICPPlannerBehavior(ros2Node, yoTime, yoDoubleSupport, fullRobotModel, referenceFrames,
                                                              wholeBodyControllerParameters, atlasPrimitiveActions));

      DRCRobotSensorInformation sensorInformation = wholeBodyControllerParameters.getSensorInformation();
      dispatcher.addBehavior(HumanoidBehaviorType.COLLABORATIVE_TASK, new CollaborativeBehavior(ros2Node, referenceFrames, fullRobotModel, sensorInformation,
                                                                                                walkingControllerParameters, yoGraphicsListRegistry));

      dispatcher.addBehavior(HumanoidBehaviorType.EXAMPLE_BEHAVIOR, new ExampleComplexBehaviorStateMachine(ros2Node, yoTime, atlasPrimitiveActions));

      dispatcher.addBehavior(HumanoidBehaviorType.LOCATE_FIDUCIAL, new LocateGoalBehavior(ros2Node, fiducialDetectorBehaviorService));
      dispatcher.addBehavior(HumanoidBehaviorType.FOLLOW_FIDUCIAL_50,
                             new FollowFiducialBehavior(ros2Node, fullRobotModel, referenceFrames, fiducialDetectorBehaviorService));
      dispatcher.addBehavior(HumanoidBehaviorType.WALK_OVER_TERRAIN,
                             new WalkOverTerrainStateMachineBehavior(ros2Node, yoTime, wholeBodyControllerParameters, referenceFrames));

      if (objectDetectorBehaviorService != null)
      {
         dispatcher.addBehavior(HumanoidBehaviorType.LOCATE_VALVE, new LocateGoalBehavior(ros2Node, objectDetectorBehaviorService));
         dispatcher.addBehavior(HumanoidBehaviorType.FOLLOW_VALVE,
                                new FollowFiducialBehavior(ros2Node, fullRobotModel, referenceFrames, objectDetectorBehaviorService));
      }

      dispatcher.addBehavior(HumanoidBehaviorType.TEST_PIPELINE,
                             new BasicPipeLineBehavior("pipelineTest", yoTime, ros2Node, fullRobotModel, referenceFrames, wholeBodyControllerParameters));

      dispatcher.addBehavior(HumanoidBehaviorType.TEST_STATEMACHINE,
                             new BasicStateMachineBehavior("StateMachineTest", yoTime, ros2Node, atlasPrimitiveActions));

      // 04/24/2017 GW: removed since this caused trouble with opencv: "Cannot load org/opencv/opencv_java320"
      //      BlobFilteredSphereDetectionBehavior blobFilteredSphereDetectionBehavior = new BlobFilteredSphereDetectionBehavior(behaviorCommunicationBridge,
      //            referenceFrames, fullRobotModel);
      //      blobFilteredSphereDetectionBehavior.addHSVRange(HSVRange.USGAMES_ORANGE_BALL);
      //      blobFilteredSphereDetectionBehavior.addHSVRange(HSVRange.USGAMES_BLUE_BALL);
      //      blobFilteredSphereDetectionBehavior.addHSVRange(HSVRange.USGAMES_RED_BALL);
      //      blobFilteredSphereDetectionBehavior.addHSVRange(HSVRange.USGAMES_YELLOW_BALL);
      //      blobFilteredSphereDetectionBehavior.addHSVRange(HSVRange.USGAMES_GREEN_BALL);
      //      blobFilteredSphereDetectionBehavior.addHSVRange(HSVRange.SIMULATED_BALL);
      //      dispatcher.addBehavior(HumanoidBehaviorType.BALL_DETECTION, blobFilteredSphereDetectionBehavior);

      DiagnosticBehavior diagnosticBehavior = new DiagnosticBehavior(fullRobotModel, yoSupportLeg, referenceFrames, yoTime, yoDoubleSupport, ros2Node,
                                                                     wholeBodyControllerParameters, yoSupportPolygon, yoGraphicsListRegistry);
      diagnosticBehavior.setCanArmsReachFarBehind(robotModelFactory.getRobotDescription().getName().contains("valkyrie"));
      dispatcher.addBehavior(HumanoidBehaviorType.DIAGNOSTIC, diagnosticBehavior);

      WalkToGoalBehavior walkToGoalBehavior = new WalkToGoalBehavior(ros2Node, referenceFrames, walkingControllerParameters, yoTime);
      dispatcher.addBehavior(HumanoidBehaviorType.WALK_TO_GOAL, walkToGoalBehavior);

      RepeatedlyWalkFootstepListBehavior repeatedlyWalkFootstepListBehavior = new RepeatedlyWalkFootstepListBehavior(ros2Node, referenceFrames, registry);
      dispatcher.addBehavior(HumanoidBehaviorType.REPEATEDLY_WALK_FOOTSTEP_LIST, repeatedlyWalkFootstepListBehavior);
   }

   private void createAndRegisterAutomaticDiagnostic(BehaviorDispatcher<HumanoidBehaviorType> dispatcher, FullHumanoidRobotModel fullRobotModel,
                                                     HumanoidReferenceFrames referenceFrames, YoDouble yoTime, Ros2Node ros2Node,
                                                     CapturePointUpdatable capturePointUpdatable, WholeBodyControllerParameters wholeBodyControllerParameters,
                                                     double timeToWait, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      YoBoolean yoDoubleSupport = capturePointUpdatable.getYoDoubleSupport();
      YoEnum<RobotSide> yoSupportLeg = capturePointUpdatable.getYoSupportLeg();
      YoFrameConvexPolygon2D yoSupportPolygon = capturePointUpdatable.getYoSupportPolygon();

      DiagnosticBehavior diagnosticBehavior = new DiagnosticBehavior(fullRobotModel, yoSupportLeg, referenceFrames, yoTime, yoDoubleSupport, ros2Node,
                                                                     wholeBodyControllerParameters, yoSupportPolygon, yoGraphicsListRegistry);
      diagnosticBehavior.setupForAutomaticDiagnostic(timeToWait);
      dispatcher.addBehavior(HumanoidBehaviorType.DIAGNOSTIC, diagnosticBehavior);
      dispatcher.requestBehavior(HumanoidBehaviorType.DIAGNOSTIC);
   }

   public static IHMCHumanoidBehaviorManager createBehaviorModuleForAutomaticDiagnostic(WholeBodyControllerParameters wholeBodyControllerParameters,
                                                                                        FullHumanoidRobotModelFactory robotModelFactory,
                                                                                        LogModelProvider modelProvider, boolean startYoVariableServer,
                                                                                        DRCRobotSensorInformation sensorInfo, double timeToWait)
         throws IOException
   {
      IHMCHumanoidBehaviorManager.setAutomaticDiagnosticTimeToWait(timeToWait);
      IHMCHumanoidBehaviorManager ihmcHumanoidBehaviorManager = new IHMCHumanoidBehaviorManager(wholeBodyControllerParameters, robotModelFactory, modelProvider,
                                                                                                startYoVariableServer, sensorInfo, true);
      return ihmcHumanoidBehaviorManager;
   }
}
