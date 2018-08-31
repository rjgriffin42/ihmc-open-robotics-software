package us.ihmc.footstepPlanning.frameworkTests;

import org.junit.Assert;
import org.junit.Test;
import us.ihmc.commons.PrintTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.footstepPlanning.FootstepPlannerType;
import us.ihmc.footstepPlanning.tools.FootstepPlannerIOTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityGraphsIOTools;
import us.ihmc.pathPlanning.visibilityGraphs.tools.VisibilityGraphsIOTools.VisibilityGraphsUnitTestDataset;
import us.ihmc.pathPlanning.visibilityGraphs.ui.VisibilityGraphsDataExporter;
import us.ihmc.robotics.geometry.PlanarRegionsList;

import java.util.List;
import java.util.Random;

public abstract class DataSetFrameworkTest
{
   // Set that to MAX_VALUE when visualizing. Before pushing, it has to be reset to a reasonable value.
   protected static final long TIMEOUT = 100000; // Long.MAX_VALUE; //

   // Whether to start the UI or not.
   protected static boolean VISUALIZE = false;
   // For enabling helpful prints.
   protected static boolean DEBUG = true;


   public abstract FootstepPlannerType getPlannerType();


   @Test(timeout = TIMEOUT)
   @ContinuousIntegrationTest(estimatedDuration = 13.0)
   public void testDatasetsWithoutOcclusion()
   {
      runAssertionsOnAllDatasets(dataset -> runAssertionsWithoutOcclusion(dataset));
   }


   private void runAssertionsOnAllDatasets(DatasetTestRunner datasetTestRunner)
   {
      List<FootstepPlannerIOTools.FootstepPlannerUnitTestDataset> allDatasets = FootstepPlannerIOTools.loadAllFootstepPlannerDatasets(VisibilityGraphsDataExporter.class);

      if (DEBUG)
      {
         PrintTools.info("Unit test files found: " + allDatasets.size());
      }

      int numberOfFailingDatasets = 0;
      String errorMessages = "";

      int currentDatasetIndex = 0;
      if (allDatasets.isEmpty())
         Assert.fail("Did not find any datasets to test.");

      // Randomizing the regionIds so the viz is better
      Random random = new Random(324);
      allDatasets.stream().map(VisibilityGraphsUnitTestDataset::getPlanarRegionsList).map(PlanarRegionsList::getPlanarRegionsAsList)
                 .forEach(regionsList -> regionsList.forEach(region -> region.setRegionId(random.nextInt())));

      VisibilityGraphsUnitTestDataset dataset = allDatasets.get(currentDatasetIndex);

      while (dataset != null)
      {

         if (DEBUG)
         {
            PrintTools.info("Processing file: " + dataset.getDatasetName());
         }

         String errorMessagesForCurrentFile = datasetTestRunner.testDataset(dataset);
         if (!errorMessagesForCurrentFile.isEmpty())
            numberOfFailingDatasets++;
         errorMessages += errorMessagesForCurrentFile;

            currentDatasetIndex++;
            if (currentDatasetIndex < allDatasets.size())
               dataset = allDatasets.get(currentDatasetIndex);
            else
               dataset = null;

         ThreadTools.sleep(100); // Apparently need to give some time for the prints to appear in the right order.
      }

      Assert.assertTrue("Number of failing datasets: " + numberOfFailingDatasets + " out of " + allDatasets.size() + ". Errors:" + errorMessages,
                        errorMessages.isEmpty());
   }

   private String runAssertionsWithoutOcclusion(VisibilityGraphsUnitTestDataset dataset)
   {
      submitDataSet(dataset);

      return findPlanAndAssertGoodResult(dataset);
   }

   public abstract void submitDataSet(VisibilityGraphsUnitTestDataset dataset);

   public abstract String findPlanAndAssertGoodResult(VisibilityGraphsUnitTestDataset dataset);





   private static interface DatasetTestRunner
   {
      String testDataset(VisibilityGraphsUnitTestDataset dataset);
   }
}
