package us.ihmc.footstepPlanning.frameworkTests;

import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.footstepPlanning.tools.PlannerTools;
import us.ihmc.footstepPlanning.ui.FootstepPlannerUserInterfaceAPI;
import us.ihmc.footstepPlanning.ui.StandaloneFootstepPlannerUI;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityGraphsIOTools.VisibilityGraphsUnitTestDataset;

import java.util.concurrent.atomic.AtomicReference;

import static us.ihmc.footstepPlanning.ui.FootstepPlannerUserInterfaceAPI.*;

public abstract class FootstepPlannerFrameworkTest extends DataSetFrameworkTest
{

   protected StandaloneFootstepPlannerUI ui;

   @Override
   public void submitDataSet(VisibilityGraphsUnitTestDataset dataset)
   {
      JavaFXMessager messager = ui.getMessager();

      messager.submitMessage(FootstepPlannerUserInterfaceAPI.PlanarRegionDataTopic, dataset.getPlanarRegionsList());
      messager.submitMessage(FootstepPlannerUserInterfaceAPI.StartPositionTopic, dataset.getStart());
      messager.submitMessage(FootstepPlannerUserInterfaceAPI.GoalPositionTopic, dataset.getGoal());

      messager.submitMessage(PlannerTypeTopic, getPlannerType());
   }

   @Override
   public String findPlanAndAssertGoodResult(VisibilityGraphsUnitTestDataset dataset)
   {
      JavaFXMessager messager = ui.getMessager();

      AtomicReference<Boolean> receivedPlan = new AtomicReference<>(false);
      AtomicReference<Boolean> receivedResult = new AtomicReference<>(false);
      messager.registerTopicListener(FootstepPlanTopic, request -> receivedPlan.set(true));
      messager.registerTopicListener(PlanningResultTopic, request -> receivedResult.set(true));

      AtomicReference<FootstepPlan> footstepPlanReference = messager.createInput(FootstepPlanTopic);
      AtomicReference<FootstepPlanningResult> footstepPlanningResult = messager.createInput(PlanningResultTopic);

      messager.submitMessage(FootstepPlannerUserInterfaceAPI.ComputePathTopic, true);

      while (!receivedPlan.get() && !receivedResult.get())
      {
         ThreadTools.sleep(10);
      }
      String datasetName = dataset.getDatasetName();


      String errorMessage = "";
      errorMessage += assertTrue(datasetName, "Planning result for " + datasetName + " is invalid, result was " + footstepPlanningResult.get(),
                                 footstepPlanningResult.get().validForExecution());

      if (footstepPlanningResult.get().validForExecution())
         errorMessage += assertTrue(datasetName, datasetName + "did not reach goal.", PlannerTools.isGoalNextToLastStep(dataset.getGoal(), footstepPlanReference.get()));

      return errorMessage;
   }

   private String assertTrue(String datasetName, String message, boolean condition)
   {
      if (VISUALIZE || DEBUG)
      {
         if (!condition)
            PrintTools.error(datasetName + ": " + message);
      }
      return !condition ? "\n" + message : "";
   }
}
