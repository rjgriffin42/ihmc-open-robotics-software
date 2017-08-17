package us.ihmc.avatar.networkProcessor.footstepPlanningToolboxModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packetCommunicator.PacketCommunicator;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.communication.packets.PlanarRegionsListMessage;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage;
import us.ihmc.communication.packets.RequestPlanarRegionsListMessage.RequestType;
import us.ihmc.communication.packets.TextToSpeechPacket;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlanner;
import us.ihmc.footstepPlanning.FootstepPlannerGoal;
import us.ihmc.footstepPlanning.FootstepPlannerGoalType;
import us.ihmc.footstepPlanning.FootstepPlannerUtils;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.footstepPlanning.aStar.AStarFootstepPlanner;
import us.ihmc.footstepPlanning.aStar.FootstepNodeExpansion;
import us.ihmc.footstepPlanning.graphSearch.BipedalFootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.PlanarRegionBipedalFootstepPlanner;
import us.ihmc.footstepPlanning.graphSearch.PlanarRegionBipedalFootstepPlannerVisualizer;
import us.ihmc.footstepPlanning.simplePlanners.PlanThenSnapPlanner;
import us.ihmc.footstepPlanning.simplePlanners.TurnWalkTurnPlanner;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidBehaviors.behaviors.roughTerrain.PlanarRegionBipedalFootstepPlannerVisualizerFactory;
import us.ihmc.humanoidRobotics.communication.packets.ExecutionMode;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataListMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepDataMessageConverter;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningToolboxOutputStatus;
import us.ihmc.humanoidRobotics.communication.subscribers.HumanoidRobotDataReceiver;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullRobotModel;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.sensors.ForceSensorDataHolder;
import us.ihmc.sensorProcessing.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.wholeBodyController.RobotContactPointParameters;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;

public class FootstepPlanningToolboxController extends ToolboxController
{
   private final boolean visualize = true;

   private enum Planners
   {
      PLANAR_REGION_BIPEDAL,
      PLAN_THEN_SNAP,
      A_STAR
   }
   private final YoEnum<Planners> activePlanner = new YoEnum<>("activePlanner", registry, Planners.class);
   private final EnumMap<Planners, FootstepPlanner> plannerMap = new EnumMap<>(Planners.class);

   private final AtomicReference<FootstepPlanningRequestPacket> latestRequestReference = new AtomicReference<FootstepPlanningRequestPacket>(null);
   private final AtomicReference<PlanarRegionsListMessage> latestPlanarRegionsReference = new AtomicReference<PlanarRegionsListMessage>(null);
   private Optional<PlanarRegionsList> planarRegionsList = Optional.empty();

   private final YoBoolean usePlanarRegions = new YoBoolean("usePlanarRegions", registry);
   private final YoBoolean isDone = new YoBoolean("isDone", registry);
   private final YoBoolean requestedPlanarRegions = new YoBoolean("RequestedPlanarRegions", registry);
   private final YoDouble toolboxTime = new YoDouble("ToolboxTime", registry);
   private final YoDouble timeReceivedPlanarRegion = new YoDouble("timeReceivedPlanarRegion", registry);

   private final HumanoidReferenceFrames humanoidReferenceFrames;
   private final RobotContactPointParameters contactPointParameters;
   private final WalkingControllerParameters walkingControllerParameters;
   private final FootstepDataListWithSwingOverTrajectoriesAssembler footstepDataListWithSwingOverTrajectoriesAssembler;
   private final FootstepNodeExpansion expansion;
   
   private final PacketCommunicator packetCommunicator;
   private long plannerCount = 0;
   private double dt;

   public FootstepPlanningToolboxController(DRCRobotModel drcRobotModel, FullHumanoidRobotModel fullHumanoidRobotModel,
                                            StatusMessageOutputManager statusOutputManager, PacketCommunicator packetCommunicator,
                                            YoVariableRegistry parentRegistry, double dt)
   {
      super(statusOutputManager, parentRegistry);
      this.packetCommunicator = packetCommunicator;
      this.contactPointParameters = drcRobotModel.getContactPointParameters();
      this.walkingControllerParameters = drcRobotModel.getWalkingControllerParameters();
      this.dt = dt;
      packetCommunicator.attachListener(PlanarRegionsListMessage.class, createPlanarRegionsConsumer());

      /**
       * A robot specific node expansion can be achieved with this. 
       * Currently only supported in A-star planner for Atlas and Valkyrie.
       * Use SimpleSideBasedExpansion ( defaults to Atlas) if using other robots or add custom footstep expansion class.
       * */ 
      this.expansion = drcRobotModel.getPlanarRegionFootstepPlannerParameters().getReachableFootstepExpansion();

      SideDependentList<ConvexPolygon2D> contactPointsInSoleFrame = createFootPolygonsFromContactPoints(contactPointParameters);

      humanoidReferenceFrames = createHumanoidReferenceFrames(fullHumanoidRobotModel);
      footstepDataListWithSwingOverTrajectoriesAssembler = new FootstepDataListWithSwingOverTrajectoriesAssembler(humanoidReferenceFrames, walkingControllerParameters, parentRegistry, new YoGraphicsListRegistry());

      plannerMap.put(Planners.PLANAR_REGION_BIPEDAL, createPlanarRegionBipedalPlanner(contactPointsInSoleFrame, fullHumanoidRobotModel));
      plannerMap.put(Planners.PLAN_THEN_SNAP, new PlanThenSnapPlanner(new TurnWalkTurnPlanner(), contactPointsInSoleFrame));
      plannerMap.put(Planners.A_STAR, AStarFootstepPlanner.createRoughTerrainPlanner(null, contactPointsInSoleFrame, expansion, registry));
      activePlanner.set(Planners.PLANAR_REGION_BIPEDAL);

      usePlanarRegions.set(true);
      isDone.set(true);
   }

   private PlanarRegionBipedalFootstepPlanner createPlanarRegionBipedalPlanner(SideDependentList<ConvexPolygon2D> footPolygonsInSoleFrame, FullRobotModel fullRobotModel)
   {
      BipedalFootstepPlannerParameters footstepPlanningParameters = new BipedalFootstepPlannerParameters(registry);
      FootstepPlannerUtils.setPlannerParametersForToolbox(footstepPlanningParameters);

      PlanarRegionBipedalFootstepPlanner footstepPlanner = new PlanarRegionBipedalFootstepPlanner(footstepPlanningParameters, registry);
      footstepPlanner.setFeetPolygons(footPolygonsInSoleFrame, footPolygonsInSoleFrame);
      footstepPlanner.setMaximumNumberOfNodesToExpand(Integer.MAX_VALUE);
      footstepPlanner.setExitAfterInitialSolution(false);
      footstepPlanner.setTimeout(5.0);

      if (visualize)
      {
         PlanarRegionBipedalFootstepPlannerVisualizer listener = PlanarRegionBipedalFootstepPlannerVisualizerFactory.createWithYoVariableServer(0.01, fullRobotModel,
               null, footPolygonsInSoleFrame, "Toolbox_");
         footstepPlanner.setBipedalFootstepPlannerListener(listener);
      }

      return footstepPlanner;
   }

   @Override
   protected void updateInternal()
   {
      toolboxTime.add(dt);
      if (toolboxTime.getDoubleValue() > 10.0)
      {
         reportMessage(packResult(null, FootstepPlanningResult.TIMED_OUT_BEFORE_SOLUTION));
         isDone.set(true);
         return;
      }

      FootstepPlanner planner = plannerMap.get(activePlanner.getEnumValue());

      if (usePlanarRegions.getBooleanValue())
      {
         if (!requestAndWaitForPlanarRegions(planner))
            return;
      }
      else
      {
         planner.setPlanarRegions(null);
         planarRegionsList = Optional.empty();
      }

      sendMessageToUI("Starting To Plan: " + plannerCount + ", " + activePlanner.getEnumValue().toString());

      FootstepPlanningResult status = planner.plan();
      FootstepPlan footstepPlan = planner.getPlan();

      sendMessageToUI("Result: " + plannerCount + ", " + status.toString());
      plannerCount++;

      reportMessage(packResult(footstepPlan, status));
      isDone.set(true);
   }

   private boolean requestAndWaitForPlanarRegions(FootstepPlanner planner)
   {
      if (!requestedPlanarRegions.getBooleanValue())
         requestPlanarRegions();

      PlanarRegionsListMessage planarRegionsMessage = latestPlanarRegionsReference.getAndSet(null);
      if (planarRegionsMessage == null)
         return false;

      timeReceivedPlanarRegion.set(toolboxTime.getDoubleValue());
      PlanarRegionsList planarRegions = PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsMessage);
      planner.setPlanarRegions(planarRegions);
      planarRegionsList = Optional.of(planarRegions);

      return true;
   }

   private void requestPlanarRegions()
   {
      RequestPlanarRegionsListMessage requestPlanarRegionsListMessage = new RequestPlanarRegionsListMessage(RequestType.SINGLE_UPDATE);
      requestPlanarRegionsListMessage.setDestination(PacketDestination.REA_MODULE);
      packetCommunicator.send(requestPlanarRegionsListMessage);
      latestPlanarRegionsReference.set(null);
      requestedPlanarRegions.set(true);
   }

   @Override
   protected boolean initialize()
   {
      isDone.set(false);
      requestedPlanarRegions.set(false);
      toolboxTime.set(0.0);
      timeReceivedPlanarRegion.set(0.0);

      FootstepPlanningRequestPacket request = latestRequestReference.getAndSet(null);
      if (request == null)
         return false;

      usePlanarRegions.set(!request.assumeFlatGround);

      FramePose initialStancePose = new FramePose(ReferenceFrame.getWorldFrame());
      initialStancePose.setPosition(new Point3D(request.stanceFootPositionInWorld));
      initialStancePose.setOrientation(new Quaternion(request.stanceFootOrientationInWorld));

      FramePose goalPose = new FramePose(ReferenceFrame.getWorldFrame());
      goalPose.setPosition(new Point3D(request.goalPositionInWorld));
      goalPose.setOrientation(new Quaternion(request.goalOrientationInWorld));

      FootstepPlanner planner = plannerMap.get(activePlanner.getEnumValue());
      planner.setInitialStanceFoot(initialStancePose, request.initialStanceSide);

      FootstepPlannerGoal goal = new FootstepPlannerGoal();
      goal.setFootstepPlannerGoalType(FootstepPlannerGoalType.POSE_BETWEEN_FEET);
      goal.setGoalPoseBetweenFeet(goalPose);
      planner.setGoal(goal);

      return true;
   }

   public PacketConsumer<FootstepPlanningRequestPacket> createRequestConsumer()
   {
      return new PacketConsumer<FootstepPlanningRequestPacket>()
      {
         @Override
         public void receivedPacket(FootstepPlanningRequestPacket packet)
         {
            if (packet == null)
               return;
            latestRequestReference.set(packet);
         }
      };
   }

   public PacketConsumer<PlanarRegionsListMessage> createPlanarRegionsConsumer()
   {
      return new PacketConsumer<PlanarRegionsListMessage>()
      {
         @Override
         public void receivedPacket(PlanarRegionsListMessage packet)
         {
            if (packet == null)
               return;
            latestPlanarRegionsReference.set(packet);
         }
      };
   }

   public HumanoidReferenceFrames createHumanoidReferenceFrames(FullHumanoidRobotModel fullHumanoidRobotModel)
   {
      ForceSensorDataHolder forceSensorDataHolder = new ForceSensorDataHolder(Arrays.asList(fullHumanoidRobotModel.getForceSensorDefinitions()));
      HumanoidRobotDataReceiver robotDataReceiver = new HumanoidRobotDataReceiver(fullHumanoidRobotModel, forceSensorDataHolder);

      packetCommunicator.attachListener(RobotConfigurationData.class, robotDataReceiver);
      return robotDataReceiver.getReferenceFrames();
   }

   private void sendMessageToUI(String message)
   {
      TextToSpeechPacket packet = new TextToSpeechPacket(message);
      packet.setDestination(PacketDestination.UI);
      packetCommunicator.send(packet);
   }

   @Override
   protected boolean isDone()
   {
      return isDone.getBooleanValue();
   }

   private FootstepPlanningToolboxOutputStatus packResult(FootstepPlan footstepPlan, FootstepPlanningResult status)
   {
      FootstepPlanningToolboxOutputStatus result = new FootstepPlanningToolboxOutputStatus();
      if (footstepPlan == null)
      {
         footstepPlan = new FootstepPlan();
         result.footstepDataList = new FootstepDataListMessage();
      }
      else
      {
         if (planarRegionsList.isPresent())
         {
            PrintTools.debug(this, "Planar regions present. Assembling footstep data list message");
            result.footstepDataList = footstepDataListWithSwingOverTrajectoriesAssembler.assemble(footstepPlan, 0.0, 0.0, ExecutionMode.OVERRIDE,
                                                                                                  planarRegionsList.get());
         }
         else
         {
            PrintTools.debug(this, "Planar regions not present. Won't swing over!");
            result.footstepDataList = FootstepDataMessageConverter.createFootstepDataListFromPlan(footstepPlan, 0.0, 0.0, ExecutionMode.OVERRIDE);
         }
      }
      result.planningResult = status;
      return result;
   }

   private static SideDependentList<ConvexPolygon2D> createFootPolygonsFromContactPoints(RobotContactPointParameters contactPointParameters)
   {
      SideDependentList<ConvexPolygon2D> footPolygons = new SideDependentList<>();
      for (RobotSide side : RobotSide.values)
      {
         ArrayList<Point2D> footPoints = contactPointParameters.getFootContactPoints().get(side);
         ConvexPolygon2D scaledFoot = new ConvexPolygon2D(footPoints);
         footPolygons.set(side, scaledFoot);
      }

      return footPolygons;
   }

}
