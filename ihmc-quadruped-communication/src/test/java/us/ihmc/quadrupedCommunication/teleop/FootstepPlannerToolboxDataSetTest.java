package us.ihmc.quadrupedCommunication.teleop;

import controller_msgs.msg.dds.QuadrupedFootstepPlanningToolboxOutputStatus;
import controller_msgs.msg.dds.QuadrupedTimedStepListMessage;
import controller_msgs.msg.dds.QuadrupedTimedStepMessage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.ContinuousIntegrationTools;
import us.ihmc.commons.Conversions;
import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DBasics;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.javaFXToolkit.messager.SharedMemoryJavaFXMessager;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.SharedMemoryMessager;
import us.ihmc.pathPlanning.DataSet;
import us.ihmc.pathPlanning.DataSetIOTools;
import us.ihmc.pathPlanning.PlannerInput;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.quadrupedBasics.gait.QuadrupedTimedOrientedStep;
import us.ihmc.quadrupedBasics.gait.QuadrupedTimedStep;
import us.ihmc.quadrupedCommunication.networkProcessing.footstepPlanning.QuadrupedFootstepPlanningModule;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.*;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.communication.FootstepPlannerCommunicationProperties;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.communication.FootstepPlannerMessagerAPI;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.DefaultFootstepPlannerParameters;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.quadrupedFootstepPlanning.ui.ApplicationRunner;
import us.ihmc.quadrupedFootstepPlanning.ui.FootstepPlannerUI;
import us.ihmc.quadrupedFootstepPlanning.ui.RemoteUIMessageConverter;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.quadrupedPlanning.footstepChooser.DefaultPointFootSnapperParameters;
import us.ihmc.robotics.Assert;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.ros2.RealtimeRos2Node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class FootstepPlannerToolboxDataSetTest
{
   protected static final double bambooTimeScaling = 4.0;


   // Whether to start the UI or not.
   protected static boolean VISUALIZE = false;
   // For enabling helpful prints.
   protected static boolean DEBUG = false;
   protected static boolean VERBOSE = false;

   private FootstepPlannerUI ui = null;
   protected Messager messager = null;

   private QuadrupedXGaitSettingsReadOnly xGaitSettings = null;
   private QuadrupedFootstepPlanningModule footstepPlanningModule = null;

   private RealtimeRos2Node ros2Node;
   private RemoteUIMessageConverter converter;

   private final AtomicReference<FootstepPlan> plannerPlanReference = new AtomicReference<>(null);
   private final AtomicReference<FootstepPlanningResult> plannerResultReference = new AtomicReference<>(null);
   private final AtomicReference<Boolean> plannerReceivedPlan = new AtomicReference<>(false);
   private final AtomicReference<Boolean> plannerReceivedResult = new AtomicReference<>(false);

   private final AtomicReference<FootstepPlan> planReference = new AtomicReference<>(null);
   private final AtomicReference<FootstepPlanningResult> resultReference = new AtomicReference<>(null);

   private static final String robotName = "testBot";
   public static final PubSubImplementation pubSubImplementation = PubSubImplementation.INTRAPROCESS;


   protected abstract FootstepPlannerType getPlannerType();

   protected abstract QuadrupedXGaitSettingsReadOnly getXGaitSettings();

   @BeforeEach
   public void setup()
   {
      VISUALIZE = VISUALIZE && !ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer();

      if (VISUALIZE)
         messager = new SharedMemoryJavaFXMessager(FootstepPlannerMessagerAPI.API);
      else
         messager = new SharedMemoryMessager(FootstepPlannerMessagerAPI.API);

      if (xGaitSettings == null)
         xGaitSettings = getXGaitSettings();

      FootstepPlannerParameters parameters = new DefaultFootstepPlannerParameters()
      {
         @Override
         public double getMaximumStepReach()
         {
            return 0.7;
         }

         @Override
         public double getMaximumStepCycleDistance()
         {
            return 0.65;
         }

         @Override
         public double getMinimumStepLength()
         {
            return -0.3;
         }

         @Override
         public double getMinimumStepWidth()
         {
            return -0.3;
         }

         @Override
         public double getMaximumStepWidth()
         {
            return 0.35;
         }
      };
      footstepPlanningModule = new QuadrupedFootstepPlanningModule(robotName, null, parameters, xGaitSettings,
                                                                   new DefaultPointFootSnapperParameters(), null, false, false, pubSubImplementation);


      ros2Node = ROS2Tools.createRealtimeRos2Node(pubSubImplementation, "ihmc_footstep_planner_test");

      ROS2Tools.createCallbackSubscription(ros2Node, QuadrupedFootstepPlanningToolboxOutputStatus.class,
                                           FootstepPlannerCommunicationProperties.publisherTopicNameGenerator(robotName),
                                           s -> processFootstepPlanningOutputStatus(s.takeNextData()));

      converter = RemoteUIMessageConverter.createConverter(messager, robotName, pubSubImplementation);

      ros2Node.spin();

      try
      {
         messager.startMessager();
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to start messager.");
      }

      messager.submitMessage(FootstepPlannerMessagerAPI.XGaitSettingsTopic, xGaitSettings);

      if (VISUALIZE)
      {
         createUI(messager);
      }

      ThreadTools.sleep(1000);
   }

   @AfterEach
   public void tearDown() throws Exception
   {
      messager.closeMessager();
      footstepPlanningModule.destroy();
      converter.destroy();
      if (ui != null)
         ui.stop();

      converter = null;
      footstepPlanningModule = null;
      ui = null;
      messager = null;
   }

   private void resetAllAtomics()
   {
      plannerPlanReference.set(null);
      plannerResultReference.set(null);
      plannerReceivedPlan.set(false);
      plannerReceivedResult.set(false);


      planReference.set(null);
      resultReference.set(null);
   }

   private void createUI(Messager messager)
   {
      ApplicationRunner.runApplication(new Application()
      {
         @Override
         public void start(Stage stage) throws Exception
         {
            ui = FootstepPlannerUI.createMessagerUI(stage, (SharedMemoryJavaFXMessager) messager);
            ui.show();
         }

         @Override
         public void stop() throws Exception
         {
            ui.stop();
            Platform.exit();
         }
      });

      double maxWaitTime = 5.0;
      double totalTime = 0.0;
      long sleepDuration = 100;

      while (ui == null)
      {
         if (totalTime > maxWaitTime)
            throw new RuntimeException("Timed out waiting for the UI to start.");
         ThreadTools.sleep(sleepDuration);
         totalTime += Conversions.millisecondsToSeconds(sleepDuration);
      }
   }

   @Test
   public void testDataSets()
   {
      List<DataSet> dataSets = DataSetIOTools.loadDataSets(dataSet ->
                                                           {
                                                              if (!dataSet.hasPlannerInput())
                                                                 return false;
                                                              return dataSet.getPlannerInput().getQuadrupedPlannerIsTestable();
                                                           });
      runAssertionsOnAllDatasets(this::runAssertions, dataSets);
   }

   @Disabled
   @Test
   public void runInDevelopmentTests()
   {
      List<DataSet> dataSets = DataSetIOTools.loadDataSets(dataSet ->
                                                           {
                                                              if (!dataSet.hasPlannerInput())
                                                                 return false;
                                                              return dataSet.getPlannerInput().getQuadrupedPlannerIsInDevelopment();
                                                           });
      runAssertionsOnAllDatasets(this::runAssertions, dataSets);
   }

   protected void runAssertionsOnDataset(Function<DataSet, String> dataSetTester, String datasetName)
   {
      DataSet dataset = DataSetIOTools.loadDataSet(datasetName);
      String errorMessages = dataSetTester.apply(dataset);
      Assert.assertTrue("Errors:" + errorMessages, errorMessages.isEmpty());
   }

   private void runAssertionsOnAllDatasets(Function<DataSet, String> dataSetTester, List<DataSet> allDatasets)
   {
      if (VERBOSE || DEBUG)
         LogTools.info("Unit test files found: " + allDatasets.size());

      if (allDatasets.isEmpty())
         Assert.fail("Did not find any datasets to test.");

      int numberOfFailingTests = 0;
      List<String> failingDatasets = new ArrayList<>();
      List<String> failingMessages = new ArrayList<>();
      int numbberOfTestedSets = 0;
      for (int i = 0; i < allDatasets.size(); i++)
      {
         DataSet dataset = allDatasets.get(i);
         if (DEBUG || VERBOSE)
            LogTools.info("Testing file: " + dataset.getName());

         numbberOfTestedSets++;
         resetAllAtomics();
         String errorMessagesForCurrentFile = dataSetTester.apply(dataset);
         if (!errorMessagesForCurrentFile.isEmpty())
         {
            numberOfFailingTests++;
            failingDatasets.add(dataset.getName());
            failingMessages.add(errorMessagesForCurrentFile);
         }

         if (DEBUG || VERBOSE)
         {
            String result = errorMessagesForCurrentFile.isEmpty() ? "passed" : "failed";
            LogTools.info(dataset.getName() + " " + result);
         }

         ThreadTools.sleep(500); // Apparently need to give some time for the prints to appear in the right order.
      }

      String message = "Number of failing datasets: " + numberOfFailingTests + " out of " + numbberOfTestedSets;
      message += "\n Datasets failing: ";
      for (int i = 0; i < failingDatasets.size(); i++)
      {
         message += "\n" + failingDatasets.get(i) + " : " + failingMessages.get(i);
      }
      if (VISUALIZE)
      {
         LogTools.info(message);
         ThreadTools.sleepForever();
      }
      else
      {
         Assert.assertEquals(message, 0, numberOfFailingTests);
      }
   }

   protected String runAssertions(DataSet dataset)
   {
      resetAllAtomics();
      ThreadTools.sleep(1000);
      packPlanningRequest(dataset);
      String errorMessage = findPlanAndAssertGoodResult(dataset);

      return errorMessage;
   }

   protected void packPlanningRequest(DataSet dataset)
   {
      Quaternion startOrientation = new Quaternion();
      Quaternion goalOrientation = new Quaternion();
      if (dataset.getPlannerInput().getHasQuadrupedStartYaw())
         startOrientation.setYawPitchRoll(dataset.getPlannerInput().getQuadrupedStartYaw(), 0.0, 0.0);
      if (dataset.getPlannerInput().getHasQuadrupedGoalYaw())
         goalOrientation.setYawPitchRoll(dataset.getPlannerInput().getQuadrupedGoalYaw(), 0.0, 0.0);

      double timeMultiplier = ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer() ? bambooTimeScaling : 1.0;
      double timeout = timeMultiplier * Double.parseDouble(dataset.getPlannerInput().getAdditionalData(getTimeoutFlag()).get(0));

      PlannerInput plannerInput = dataset.getPlannerInput();

      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerTypeTopic, getPlannerType());
      messager.submitMessage(FootstepPlannerMessagerAPI.PlannerTimeoutTopic, timeout);

      messager.submitMessage(FootstepPlannerMessagerAPI.XGaitSettingsTopic, xGaitSettings);
      messager.submitMessage(FootstepPlannerMessagerAPI.PlanarRegionDataTopic, dataset.getPlanarRegionsList());
      messager.submitMessage(FootstepPlannerMessagerAPI.StartPositionTopic, plannerInput.getQuadrupedStartPosition());
      messager.submitMessage(FootstepPlannerMessagerAPI.GoalPositionTopic, plannerInput.getQuadrupedGoalPosition());
      messager.submitMessage(FootstepPlannerMessagerAPI.StartOrientationTopic, startOrientation);
      messager.submitMessage(FootstepPlannerMessagerAPI.GoalOrientationTopic, goalOrientation);

      ThreadTools.sleep(1000);

      messager.submitMessage(FootstepPlannerMessagerAPI.ComputePathTopic, true);

      //      planner.setHorizonLengthTopic(Double.MAX_VALUE);

      if (DEBUG)
         LogTools.info("Sending out planning request.");
   }

   private void processFootstepPlanningOutputStatus(QuadrupedFootstepPlanningToolboxOutputStatus packet)
   {
      if (DEBUG)
         PrintTools.info("Processed an output from a remote planner.");

      plannerResultReference.set(FootstepPlanningResult.fromByte(packet.getFootstepPlanningResult()));
      plannerPlanReference.set(convertToFootstepPlan(packet.getFootstepDataList()));
      plannerReceivedPlan.set(true);
      plannerReceivedResult.set(true);
   }

   private static FootstepPlan convertToFootstepPlan(QuadrupedTimedStepListMessage footstepDataListMessage)
   {
      FootstepPlan footstepPlan = new FootstepPlan();

      for (QuadrupedTimedStepMessage footstepMessage : footstepDataListMessage.getQuadrupedStepList())
      {
         QuadrupedTimedOrientedStep step = new QuadrupedTimedOrientedStep();
         step.setGoalPosition(footstepMessage.getQuadrupedStepMessage().getGoalPosition());
         step.getTimeInterval().setInterval(footstepMessage.getTimeInterval().getStartTime(), footstepMessage.getTimeInterval().getEndTime());
         step.setGroundClearance(footstepMessage.getQuadrupedStepMessage().getGroundClearance());
         step.setRobotQuadrant(RobotQuadrant.fromByte(footstepMessage.getQuadrupedStepMessage().getRobotQuadrant()));

         footstepPlan.addFootstep(step);
      }

      return footstepPlan;
   }

   private String getTimeoutFlag()
   {
//      return getPlannerType().toString().toLowerCase() + "_timeout";
      return "quadruped_timeout";
   }

   private String findPlanAndAssertGoodResult(DataSet dataset)
   {
      totalTimeTaken = 0.0;
      double timeoutMultiplier = ContinuousIntegrationTools.isRunningOnContinuousIntegrationServer() ? bambooTimeScaling : 1.0;
      double maxTimeToWait = 2.0 * timeoutMultiplier * 60.0;
      String datasetName = "";

      queryPlannerResults();

      String errorMessage = "";
      if (DEBUG)
         PrintTools.info("Waiting for result.");

      errorMessage += waitForResult(() -> resultReference.get() == null, maxTimeToWait, datasetName);

      if (DEBUG)
         PrintTools.info("Received a result (actual = " + resultReference.get() + ", checking it's validity.");

      errorMessage += validateResult(() -> resultReference.get().validForExecution() , resultReference.get(), datasetName);
      if (!errorMessage.isEmpty())
         return errorMessage;

      if (DEBUG)
         PrintTools.info("Results are valid, waiting for plan.");

      errorMessage += waitForPlan(() -> planReference.get() == null, maxTimeToWait, datasetName);
      if (!errorMessage.isEmpty())
         return errorMessage;

      if (DEBUG)
         PrintTools.info("Received a plan, checking it's validity.");

      FootstepPlanningResult result = this.resultReference.getAndSet(null);
      FootstepPlan plan = this.planReference.getAndSet(null);

      plannerReceivedPlan.set(false);
      plannerReceivedResult.set(false);

      errorMessage += assertPlanIsValid(datasetName, result, plan, dataset.getPlannerInput().getQuadrupedGoalPosition(), dataset.getPlannerInput().getQuadrupedGoalYaw());

      for (int i = 0; i < 100; i++)
         ThreadTools.sleep(10);

      return errorMessage;
   }


   private double totalTimeTaken;

   private String waitForResult(ConditionChecker conditionChecker, double maxTimeToWait, String prefix)
   {
      String errorMessage = "";
      long waitTime = 10;
      while (conditionChecker.checkCondition())
      {
         if (totalTimeTaken > maxTimeToWait)
         {
            errorMessage += prefix + " timed out waiting for a result.\n";
            return errorMessage;
         }

         ThreadTools.sleep(waitTime);
         totalTimeTaken += Conversions.millisecondsToSeconds(waitTime);
         queryPlannerResults();
      }

      return errorMessage;
   }

   private String validateResult(ConditionChecker conditionChecker, FootstepPlanningResult result, String prefix)
   {
      String errorMessage = "";

      if (!conditionChecker.checkCondition())
      {
         errorMessage += prefix + " failed to find a valid result. Result : " + result + "\n";
      }

      return errorMessage;
   }

   private String waitForPlan(ConditionChecker conditionChecker, double maxTimeToWait, String prefix)
   {
      String errorMessage = "";

      while (conditionChecker.checkCondition())
      {
         long waitTime = 10;

         if (totalTimeTaken > maxTimeToWait)
         {
            errorMessage += prefix + " timed out waiting on plan.\n";
            return errorMessage;
         }

         ThreadTools.sleep(waitTime);
         totalTimeTaken += Conversions.millisecondsToSeconds(waitTime);
         queryPlannerResults();
      }

      return errorMessage;
   }


   private void queryPlannerResults()
   {
      if (plannerReceivedPlan.get() && plannerPlanReference.get() != null && planReference.get() == null)
      {
         if (DEBUG)
            PrintTools.info("Received a plan from the planner.");
         planReference.set(plannerPlanReference.getAndSet(null));
         plannerReceivedPlan.set(false);
      }

      if (plannerReceivedResult.get() && plannerResultReference.get() != null)
      {
         if (DEBUG)
            PrintTools.info("Received a result " + plannerResultReference.get() + " from the planner.");
         resultReference.set(plannerResultReference.getAndSet(null));
         plannerReceivedResult.set(false);
      }
   }


   private static String assertPlanIsValid(String datasetName, FootstepPlanningResult result, FootstepPlan plannedSteps, Point3DReadOnly goalPosition,
                                           double goalYaw)
   {
      QuadrantDependentList<Point3DBasics> finalSteps = getFinalStepPositions(plannedSteps);

      String errorMessage = "";
      if (!result.validForExecution())
         errorMessage = datasetName + " was not valid for execution " + result + ".\n";


      Point3D centerPoint = new Point3D();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         centerPoint.add(finalSteps.get(robotQuadrant));
      }

      double nominalYaw = FootstepNode.computeNominalYaw(finalSteps.get(RobotQuadrant.FRONT_LEFT).getX(), finalSteps.get(RobotQuadrant.FRONT_LEFT).getY(),
                                                         finalSteps.get(RobotQuadrant.FRONT_RIGHT).getX(), finalSteps.get(RobotQuadrant.FRONT_RIGHT).getY(),
                                                         finalSteps.get(RobotQuadrant.HIND_LEFT).getX(), finalSteps.get(RobotQuadrant.HIND_LEFT).getY(),
                                                         finalSteps.get(RobotQuadrant.HIND_RIGHT).getX(), finalSteps.get(RobotQuadrant.HIND_RIGHT).getY());

      centerPoint.scale(0.25);

      if (!goalPosition.epsilonEquals(centerPoint, 3.0 * FootstepNode.gridSizeXY))
         errorMessage += datasetName + " did not reach goal position. Made it to " + centerPoint + ", trying to get to " + goalPosition;
      if (Double.isFinite(goalYaw))
      {
         if (AngleTools.computeAngleDifferenceMinusPiToPi(goalYaw, nominalYaw) > FootstepNode.gridSizeYaw)
            errorMessage += datasetName + " did not reach goal yaw. Made it to " + nominalYaw + ", trying to get to " + goalYaw;
      }

      if ((VISUALIZE || DEBUG) && !errorMessage.isEmpty())
         LogTools.error(errorMessage);

      return errorMessage;
   }

   private static QuadrantDependentList<Point3DBasics> getFinalStepPositions(FootstepPlan plannedSteps)
   {
      QuadrantDependentList<Point3DBasics> finalSteps = new QuadrantDependentList<>();
      for (int i = plannedSteps.getNumberOfSteps() - 1; i >= 0; i--)
      {
         QuadrupedTimedStep step = plannedSteps.getFootstep(i);
         if (finalSteps.containsKey(step.getRobotQuadrant()))
            continue;
         else
            finalSteps.put(step.getRobotQuadrant(), step.getGoalPosition());
      }

      return finalSteps;
   }

   private static interface ConditionChecker
   {
      boolean checkCondition();
   }
}
