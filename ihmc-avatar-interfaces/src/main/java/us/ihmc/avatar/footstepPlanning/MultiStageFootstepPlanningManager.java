package us.ihmc.avatar.footstepPlanning;

import controller_msgs.msg.dds.*;
import us.ihmc.commons.Conversions;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.PlanarRegionMessageConverter;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.*;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.footstepPlanning.graphSearch.parameters.YoFootstepPlannerParameters;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.pathPlanning.statistics.ListOfStatistics;
import us.ihmc.pathPlanning.statistics.PlannerStatistics;
import us.ihmc.pathPlanning.statistics.VisibilityGraphStatistics;
import us.ihmc.pathPlanning.visibilityGraphs.tools.BodyPathPlan;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.graphics.YoGraphicPlanarRegionsList;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.wholeBodyController.RobotContactPointParameters;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MultiStageFootstepPlanningManager implements PlannerCompletionCallback
{
   private static final boolean debug = false;
   private static final int stagesToCreate = 4;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final YoEnum<FootstepPlannerType> activePlanner = new YoEnum<>("activePlanner", registry, FootstepPlannerType.class);

   private final AtomicReference<FootstepPlanningRequestPacket> latestRequestReference = new AtomicReference<>(null);
   private final AtomicReference<FootstepPlannerParametersPacket> latestParametersReference = new AtomicReference<>(null);

   private final AtomicReference<BodyPathPlan> bodyPathPlan = new AtomicReference<>(null);
   private final AtomicReference<FootstepPlan> footstepPlan = new AtomicReference<>(null);

   private Optional<PlanarRegionsList> planarRegionsList = Optional.empty();

   private final YoBoolean isDone = new YoBoolean("isDone", registry);
   private final YoBoolean requestedPlanarRegions = new YoBoolean("RequestedPlanarRegions", registry);
   private final YoDouble toolboxTime = new YoDouble("ToolboxTime", registry);
   private final YoDouble timeout = new YoDouble("ToolboxTimeout", registry);
   private final YoInteger planId = new YoInteger("planId", registry);

   private final YoGraphicPlanarRegionsList yoGraphicPlanarRegionsList;

   private final List<FootstepPlannerInfo> poolOfPlanningGoals = new ArrayList<>();

   private final List<FootstepPlanningStage> allPlanningStages = new ArrayList<>();
   private final List<FootstepPlanningStage> availablePlanningStages = new ArrayList<>();
   private final List<FootstepPlanningStage> pathPlanningStagesInProgress = new ArrayList<>();
   private final List<FootstepPlanningStage> stepPlanningStagesInProgress = new ArrayList<>();

   private final HashMap<FootstepPlanningStage, ScheduledFuture<?>> planningTasks = new HashMap<>();

   private final YoBoolean isDonePlanningPath = new YoBoolean("isDonePlanningPath", registry);
   private final YoBoolean isDonePlanningSteps = new YoBoolean("isDonePlanningSteps", registry);

   private final List<ScheduledFuture<?>> scheduledStages = new ArrayList<>();

   private final List<FootstepPlanningResult> completedPathResults = new ArrayList<>();
   private final List<FootstepPlanningResult> completedStepResults = new ArrayList<>();

   private final List<BodyPathPlan> completedPathPlans = new ArrayList<>();
   private final List<FootstepPlan> completedStepPlans = new ArrayList<>();

   private double dt;

   private final YoFootstepPlannerParameters footstepPlanningParameters;
   private IHMCRealtimeROS2Publisher<TextToSpeechPacket> textToSpeechPublisher;

   private final RobotContactPointParameters<RobotSide> contactPointParameters;

   private final StatusMessageOutputManager statusOutputManager;
   private final ScheduledExecutorService executorService;
   private final YoBoolean initialize = new YoBoolean("initialize" + registry.getName(), registry);

   public MultiStageFootstepPlanningManager(RobotContactPointParameters<RobotSide> contactPointParameters, FootstepPlannerParameters footstepPlannerParameters,
                                            StatusMessageOutputManager statusOutputManager, ScheduledExecutorService executorService,
                                            YoVariableRegistry parentRegistry, YoGraphicsListRegistry graphicsListRegistry, double dt)
   {
      this.contactPointParameters = contactPointParameters;
      this.statusOutputManager = statusOutputManager;
      this.dt = dt;
      this.executorService = executorService;
      this.yoGraphicPlanarRegionsList = new YoGraphicPlanarRegionsList("FootstepPlannerToolboxPlanarRegions", 200, 30, registry);

      footstepPlanningParameters = new YoFootstepPlannerParameters(registry, footstepPlannerParameters);

      activePlanner.set(FootstepPlannerType.PLANAR_REGION_BIPEDAL);

      graphicsListRegistry.registerYoGraphic("footstepPlanningToolbox", yoGraphicPlanarRegionsList);
      isDone.set(true);
      planId.set(FootstepPlanningRequestPacket.NO_PLAN_ID);

      for (int i = 0; i < stagesToCreate; i++)
      {
         FootstepPlanningStage planningStage = new FootstepPlanningStage(i, contactPointParameters, footstepPlannerParameters, activePlanner, planId,
                                                                         graphicsListRegistry, dt);
         planningStage.addCompletionCallback(this);
         registry.addChild(planningStage.getYoVariableRegistry());
         allPlanningStages.add(planningStage);
         availablePlanningStages.add(planningStage);
      }

      isDonePlanningPath.set(false);
      isDonePlanningSteps.set(false);

      isDonePlanningPath.addVariableChangedListener(v -> {
         if (!isDonePlanningPath.getBooleanValue())
            statusOutputManager.reportStatusMessage(FootstepPlanningMessageReporter.packStatus(FootstepPlannerStatus.PLANNING_PATH));
         else
            statusOutputManager.reportStatusMessage(FootstepPlanningMessageReporter.packStatus(FootstepPlannerStatus.PLANNING_STEPS));
      });
      isDonePlanningSteps.addVariableChangedListener(v -> {
         if (isDonePlanningSteps.getBooleanValue())
            statusOutputManager.reportStatusMessage(FootstepPlanningMessageReporter.packStatus(FootstepPlannerStatus.IDLE));
      });

      parentRegistry.addChild(registry);
      initialize.set(true);
   }

   private FootstepPlanningStage createNewFootstepPlanningStage()
   {
      FootstepPlanningStage stage = new FootstepPlanningStage(allPlanningStages.size(), contactPointParameters, footstepPlanningParameters, activePlanner,
                                                              planId, null, dt);
      stage.addCompletionCallback(this);
      return stage;
   }

   private FootstepPlanningStage spawnNextAvailablePlanner()
   {
      if (availablePlanningStages.isEmpty())
      {
         FootstepPlanningStage planningStage = createNewFootstepPlanningStage();
         allPlanningStages.add(planningStage);
         availablePlanningStages.add(planningStage);
      }

      return availablePlanningStages.remove(0);
   }

   private FootstepPlanningStage cleanupPlanningStage(int stageIndex)
   {
      FootstepPlanningStage planningStage = stepPlanningStagesInProgress.remove(stageIndex);
      planningStage.requestInitialize();
      planningTasks.remove(planningStage).cancel(true);

      return planningStage;
   }

   private void createPlanningStage()
   {
      if (!pathPlanningStagesInProgress.isEmpty())
      {
         if (debug)
            PrintTools.error(this, "toolboxRunnable is not null.");
         return;
      }

      FootstepPlanningStage stage = createNewFootstepPlanningStage();
      pathPlanningStagesInProgress.add(stage);
      allPlanningStages.add(stage);
   }

   private void cleanupAllPlanningStages()
   {
      pathPlanningStagesInProgress.clear();

      while (!stepPlanningStagesInProgress.isEmpty())
      {
         availablePlanningStages.add(cleanupPlanningStage(0));
      }

      completedPathPlans.clear();
      completedStepPlans.clear();

      completedPathResults.clear();
      completedStepResults.clear();

      poolOfPlanningGoals.clear();
   }

   private void assignGoalsToAvailablePlanners()
   {
      if (poolOfPlanningGoals.isEmpty())
         return;

      if (availablePlanningStages.isEmpty())
         return;

      while (!poolOfPlanningGoals.isEmpty() && !availablePlanningStages.isEmpty())
      {
         FootstepPlanningStage planner = spawnNextAvailablePlanner();
         FootstepPlannerInfo plannerGoal = poolOfPlanningGoals.remove(0);

         ScheduledFuture<?> plannerTask = executorService.scheduleAtFixedRate(planner, 0, (long) Conversions.secondsToMilliseconds(dt), TimeUnit.MILLISECONDS);
         planningTasks.put(planner, plannerTask);

         poolOfPlanningGoals.add(plannerGoal);
         pathPlanningStagesInProgress.add(planner);
         stepPlanningStagesInProgress.add(planner);
      }
   }

   @Override
   public void pathPlanningIsComplete(FootstepPlanningResult pathPlanningResult, FootstepPlanningStage stageFinished)
   {
      completedPathResults.add(pathPlanningResult);
      pathPlanningStagesInProgress.remove(stageFinished);

      if (pathPlanningResult.validForExecution())
         completedPathPlans.add(stageFinished.getPathPlan());
   }

   @Override
   public void stepPlanningIsComplete(FootstepPlanningResult stepPlanningResult, FootstepPlanningStage stageFinished)
   {
      completedStepResults.add(stepPlanningResult);
      stepPlanningStagesInProgress.remove(stageFinished);
      availablePlanningStages.add(stageFinished);

      planningTasks.remove(stageFinished).cancel(true);

      if (stepPlanningResult.validForExecution())
         completedStepPlans.add(stageFinished.getPlan());
   }

   public void update()
   {
      if (initialize.getBooleanValue())
      {
         if (!initialize()) // Return until the initialization succeeds
            return;
         initialize.set(false);
      }

      updateInternal();
   }

   private boolean initialize()
   {
      isDone.set(false);
      requestedPlanarRegions.set(false);
      toolboxTime.set(0.0);

      for (FootstepPlanningStage stage : allPlanningStages)
         stage.setTextToSpeechPublisher(textToSpeechPublisher);

      FootstepPlanningRequestPacket request = latestRequestReference.getAndSet(null);
      if (request == null)
         return false;

      planId.set(request.getPlannerRequestId());
      FootstepPlannerType requestedPlannerType = FootstepPlannerType.fromByte(request.getRequestedFootstepPlannerType());

      FootstepPlannerParametersPacket parameters = latestParametersReference.getAndSet(null);
      if (parameters != null)
         footstepPlanningParameters.set(parameters);

      if (debug)
      {
         PrintTools.info("Starting to plan. Plan id: " + request.getPlannerRequestId() + ". Timeout: " + request.getTimeout());
      }

      if (requestedPlannerType != null)
      {
         activePlanner.set(requestedPlannerType);
      }

      PlanarRegionsListMessage planarRegionsListMessage = request.getPlanarRegionsListMessage();
      if (planarRegionsListMessage == null)
      {
         this.planarRegionsList = Optional.empty();
      }
      else
      {
         PlanarRegionsList planarRegionsList = PlanarRegionMessageConverter.convertToPlanarRegionsList(planarRegionsListMessage);
         this.planarRegionsList = Optional.of(planarRegionsList);
      }

      FootstepPlannerInfo plannerGoal = new FootstepPlannerInfo();

      FramePose3D initialStancePose = new FramePose3D(ReferenceFrame.getWorldFrame());
      initialStancePose.setPosition(new Point3D(request.getStanceFootPositionInWorld()));
      initialStancePose.setOrientation(new Quaternion(request.getStanceFootOrientationInWorld()));
      plannerGoal.setInitialStanceFootPose(initialStancePose);

      FramePose3D goalPose = new FramePose3D(ReferenceFrame.getWorldFrame());
      goalPose.setPosition(new Point3D(request.getGoalPositionInWorld()));
      goalPose.setOrientation(new Quaternion(request.getGoalOrientationInWorld()));

      plannerGoal.setInitialStanceFootSide(RobotSide.fromByte(request.getInitialStanceRobotSide()));

      FootstepPlannerGoal goal = new FootstepPlannerGoal();
      goal.setFootstepPlannerGoalType(FootstepPlannerGoalType.POSE_BETWEEN_FEET);
      goal.setGoalPoseBetweenFeet(goalPose);
      plannerGoal.setGoal(goal);

      double horizonLength = request.getHorizonLength();
      if (horizonLength > 0 && Double.isFinite(horizonLength))
         plannerGoal.setHorizonLength(horizonLength);

      double timeout = request.getTimeout();
      if (timeout > 0.0 && Double.isFinite(timeout))
      {
         plannerGoal.setTimeout(timeout);
         this.timeout.set(timeout);

         if (debug)
         {
            PrintTools.info("Setting timeout to " + timeout);
         }
      }
      else
      {
         plannerGoal.setTimeout(Double.POSITIVE_INFINITY);
      }

      return true;
   }

   private void updateInternal()
   {
      toolboxTime.add(dt);
      if (toolboxTime.getDoubleValue() > 20.0)
      {
         if (debug)
            PrintTools.info("Hard timeout at " + toolboxTime.getDoubleValue());
         reportPlannerFailed(FootstepPlanningResult.TIMED_OUT_BEFORE_SOLUTION);
         return;
      }

      PlanarRegionsList planarRegionsList;
      if (this.planarRegionsList.isPresent())
      {
         planarRegionsList = this.planarRegionsList.get();
         yoGraphicPlanarRegionsList.submitPlanarRegionsListToRender(planarRegionsList);
         yoGraphicPlanarRegionsList.processPlanarRegionsListQueue();
      }
      else
      {
         planarRegionsList = null;
         yoGraphicPlanarRegionsList.clear();
      }

      for (FootstepPlanner footstepPlanner : stepPlanningStagesInProgress)
         footstepPlanner.setPlanarRegions(planarRegionsList);

      // check if there are any more goals, and assign them to the available planners
      assignGoalsToAvailablePlanners();

      // check the status of the path planners
      FootstepPlanningResult pathStatus = getWorstResult(completedPathResults);
      if (!pathStatus.validForExecution())
      {
         reportPlannerFailed(pathStatus);
         return;
      }

      // path planner hasn't failed, so update the path planner status and send out new plan if finished
      boolean pathPlanningStatusChanged = isDonePlanningPath.getBooleanValue() != pathPlanningStagesInProgress.isEmpty();
      if (pathPlanningStagesInProgress.isEmpty() && pathPlanningStatusChanged)
      {
         concatenateBodyPathPlans();
         statusOutputManager.reportStatusMessage(packPathResult(bodyPathPlan.get(), pathStatus));
      }
      if (pathPlanningStatusChanged)
         isDonePlanningPath.set(pathPlanningStagesInProgress.isEmpty());

      // check the status of the step planners
      FootstepPlanningResult stepStatus = getWorstResult(completedStepResults);
      if (!stepStatus.validForExecution())
      {
         reportPlannerFailed(stepStatus);
         return;
      }

      // step planner hasn't failed, so update the step planner stauts and send out the new plan if finished
      boolean stepPlanningStatusChanged = isDonePlanningSteps.getBooleanValue() != stepPlanningStagesInProgress.isEmpty();
      if (stepPlanningStagesInProgress.isEmpty() && stepPlanningStatusChanged)
      {
         sendMessageToUI("Result: " + planId.getIntegerValue() + ", " + stepStatus.toString());
         concatenateFootstepPlans();
         statusOutputManager
               .reportStatusMessage(packStepResult(footstepPlan.getAndSet(null), bodyPathPlan.getAndSet(null), stepStatus, toolboxTime.getDoubleValue()));
      }
      if (stepPlanningStatusChanged)
         isDonePlanningSteps.set(stepPlanningStagesInProgress.isEmpty());

      isDone.set(isDonePlanningPath.getBooleanValue() && isDonePlanningSteps.getBooleanValue());
   }



   private static FootstepPlanningResult getWorstResult(List<FootstepPlanningResult> results)
   {
      FootstepPlanningResult worstResult = FootstepPlanningResult.OPTIMAL_SOLUTION;
      for (FootstepPlanningResult result : results)
         worstResult = FootstepPlanningResult.getWorstResult(worstResult, result);

      return worstResult;
   }

   private void reportPlannerFailed(FootstepPlanningResult result)
   {
      statusOutputManager.reportStatusMessage(packStepResult(null, null, result, toolboxTime.getDoubleValue()));
      statusOutputManager.reportStatusMessage(FootstepPlanningMessageReporter.packStatus(FootstepPlannerStatus.IDLE));
      isDonePlanningSteps.set(true);
      isDonePlanningPath.set(true);
      isDone.set(true);
   }

   private void concatenateBodyPathPlans()
   {
      if (completedPathPlans.isEmpty())
      {
         bodyPathPlan.set(null);
         return;
      }

      BodyPathPlan totalBodyPathPlan = new BodyPathPlan();
      totalBodyPathPlan.setStartPose(completedPathPlans.get(0).getStartPose());
      totalBodyPathPlan.setGoalPose(completedPathPlans.get(completedPathPlans.size() - 1).getStartPose());
      for (BodyPathPlan bodyPathPlan : completedPathPlans)
      {
         for (int i = 0; i < bodyPathPlan.getNumberOfWaypoints(); i++)
            totalBodyPathPlan.addWaypoint(bodyPathPlan.getWaypoint(i));
      }

      completedPathPlans.clear();

      bodyPathPlan.set(totalBodyPathPlan);
   }

   private void concatenateFootstepPlans()
   {
      if (completedStepPlans.isEmpty())
      {
         footstepPlan.set(null);
         return;
      }

      FootstepPlan totalFootstepPlan = new FootstepPlan();
      for (FootstepPlan footstepPlan : completedStepPlans)
      {
         if (footstepPlan.hasLowLevelPlanGoal())
            totalFootstepPlan.setLowLevelPlanGoal(footstepPlan.getLowLevelPlanGoal());

         for (int i = 0; i < footstepPlan.getNumberOfSteps(); i++)
            totalFootstepPlan.addFootstep(footstepPlan.getFootstep(i));
      }

      completedPathPlans.clear();

      footstepPlan.set(totalFootstepPlan);
   }

   private void sendMessageToUI(String message)
   {
      textToSpeechPublisher.publish(MessageTools.createTextToSpeechPacket(message));
   }

   protected boolean isDone()
   {
      return isDone.getBooleanValue();
   }

   private BodyPathPlanMessage packPathResult(BodyPathPlan bodyPathPlan, FootstepPlanningResult status)
   {
      return FootstepPlanningMessageReporter.packPathResult(bodyPathPlan, status, planarRegionsList, planId.getIntegerValue());
   }

   private FootstepPlanningToolboxOutputStatus packStepResult(FootstepPlan footstepPlan, BodyPathPlan bodyPathPlan, FootstepPlanningResult status,
                                                              double timeTaken)
   {
      return FootstepPlanningMessageReporter.packStepResult(footstepPlan, bodyPathPlan, status, timeTaken, planarRegionsList, planId.getIntegerValue());
   }

   public void processRequest(FootstepPlanningRequestPacket request)
   {
      latestRequestReference.set(request);
   }

   public void processPlannerParameters(FootstepPlannerParametersPacket parameters)
   {
      latestParametersReference.set(parameters);
   }

   public void processPlanningStatisticsRequest()
   {
      FootstepPlanner planner = pathPlanningStagesInProgress.get(0);
      sendPlannerStatistics(planner.getPlannerStatistics());
   }

   public void setTextToSpeechPublisher(IHMCRealtimeROS2Publisher<TextToSpeechPacket> textToSpeechPublisher)
   {
      this.textToSpeechPublisher = textToSpeechPublisher;
   }

   private void sendPlannerStatistics(PlannerStatistics plannerStatistics)
   {
      switch (plannerStatistics.getStatisticsType())
      {
      case LIST:
         sendListOfStatistics((ListOfStatistics) plannerStatistics);
         break;
      case VISIBILITY_GRAPH:
         statusOutputManager.reportStatusMessage(VisibilityGraphMessagesConverter.convertToBodyPathPlanStatisticsMessage(planId.getIntegerValue(),
                                                                                                                         (VisibilityGraphStatistics) plannerStatistics));
         break;
      }
   }

   private void sendListOfStatistics(ListOfStatistics listOfStatistics)
   {
      while (listOfStatistics.getNumberOfStatistics() > 0)
         sendPlannerStatistics(listOfStatistics.pollStatistics());
   }

   public void wakeUp()
   {
      if (debug)
         PrintTools.debug(this, "Waking up");

      createPlanningStage();
      //      scheduledStages
      //            .add(executorService.scheduleAtFixedRate(pathPlanningStagesInProgress.get(0), 0, (long) dt, TimeUnit.SECONDS));
      initialize.set(true);
   }

   public void sleep()
   {
      if (debug)
         PrintTools.debug(this, "Going to sleep");

      cleanupAllPlanningStages();

      while (!scheduledStages.isEmpty())
         scheduledStages.remove(0).cancel(true);
   }

   public void destroy()
   {
      sleep();

      if (debug)
         PrintTools.debug(this, "Destroyed");
   }
}
