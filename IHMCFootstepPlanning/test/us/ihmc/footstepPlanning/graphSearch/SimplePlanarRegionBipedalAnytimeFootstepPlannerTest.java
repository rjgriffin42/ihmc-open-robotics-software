package us.ihmc.footstepPlanning.graphSearch;

import static org.junit.Assert.assertTrue;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.junit.Test;

import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.FootstepPlanner;
import us.ihmc.footstepPlanning.FootstepPlannerGoal;
import us.ihmc.footstepPlanning.FootstepPlannerGoalType;
import us.ihmc.footstepPlanning.FootstepPlanningResult;
import us.ihmc.footstepPlanning.SimpleFootstep;
import us.ihmc.footstepPlanning.polygonSnapping.PlanarRegionsListExamples;
import us.ihmc.footstepPlanning.roughTerrainPlanning.SCSPlanarRegionBipedalFootstepPlannerVisualizer;
import us.ihmc.footstepPlanning.simplePlanners.FlatGroundPlanningUtils;
import us.ihmc.footstepPlanning.testTools.PlanningTestTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.ConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FramePose2d;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.tools.continuousIntegration.ContinuousIntegrationAnnotations;
import us.ihmc.tools.thread.ThreadTools;

public class SimplePlanarRegionBipedalAnytimeFootstepPlannerTest
{
   private final boolean visualize = false;

   @ContinuousIntegrationAnnotations.ContinuousIntegrationTest(estimatedDuration = 20.0)
   @Test(timeout = 300000)
   public void testSameResultsAsNormalPlannerWhenUsedAsANormalPlanner()
   {
      SimplePlanarRegionBipedalAnytimeFootstepPlanner anytimePlannerOne = createAnytimePlanner();
      PlanarRegionBipedalFootstepPlanner normalPlannerTwo = createNormalPlanner();
      SimplePlanarRegionBipedalAnytimeFootstepPlanner anytimePlannerThree = createAnytimePlanner();

      SideDependentList<ConvexPolygon2d> footPolygonsInSoleFrameOne = PlanningTestTools.createDefaultFootPolygons();
      anytimePlannerOne.setFeetPolygons(footPolygonsInSoleFrameOne);

      SideDependentList<ConvexPolygon2d> footPolygonsInSoleFrameTwo = PlanningTestTools.createDefaultFootPolygons();
      normalPlannerTwo.setFeetPolygons(footPolygonsInSoleFrameTwo);
      
      SideDependentList<ConvexPolygon2d> footPolygonsInSoleFrameThree = PlanningTestTools.createDefaultFootPolygons();
      anytimePlannerThree.setFeetPolygons(footPolygonsInSoleFrameThree);

      PlanarRegionBipedalFootstepPlannerVisualizer visualizerOne = null;
      PlanarRegionBipedalFootstepPlannerVisualizer visualizerTwo = null;
      PlanarRegionBipedalFootstepPlannerVisualizer visualizerThree = null;

      if (visualize)
      {
         Point3d cameraPosition = new Point3d(4.5, -8.0, 12.0);
         Point3d cameraFix = new Point3d(4.5, 0.0, 0.25);
         
         visualizerOne = SCSPlanarRegionBipedalFootstepPlannerVisualizer.createWithSimulationConstructionSet(0.01, cameraFix, cameraPosition, footPolygonsInSoleFrameOne);
         visualizerTwo = SCSPlanarRegionBipedalFootstepPlannerVisualizer.createWithSimulationConstructionSet(0.01, cameraFix, cameraPosition,  footPolygonsInSoleFrameTwo);
         visualizerThree = SCSPlanarRegionBipedalFootstepPlannerVisualizer.createWithSimulationConstructionSet(0.01, cameraFix, cameraPosition,  footPolygonsInSoleFrameThree);
         anytimePlannerOne.setBipedalFootstepPlannerListener(visualizerOne);
         normalPlannerTwo.setBipedalFootstepPlannerListener(visualizerTwo);
         anytimePlannerThree.setBipedalFootstepPlannerListener(visualizerThree);
      }

      ThreadTools.sleep(3000L);
 
      setPlanarRegions(anytimePlannerOne);
      setPlanarRegions(normalPlannerTwo); 

      setGoalAndInitialStanceFoot(anytimePlannerOne);
      setGoalAndInitialStanceFoot(normalPlannerTwo);

      FootstepPlanningResult resultOne = anytimePlannerOne.plan();
      FootstepPlanningResult resultTwo = normalPlannerTwo.plan();

      assertTrue(resultOne == resultTwo);

      FootstepPlan planOne = anytimePlannerOne.getPlan();
      FootstepPlan planTwo = normalPlannerTwo.getPlan();

      assertPlansAreEqual(planOne, planTwo, 0);

      
      // Planner three we'll try to throw off threading...
      Thread thread = new Thread(anytimePlannerThree);

      ThreadTools.sleep(2000L);
      setPlanarRegions(anytimePlannerThree); 
      setGoalAndInitialStanceFoot(anytimePlannerThree);
      thread.start();
      ThreadTools.sleep(2000L);
      FootstepPlan bestPlanYet = anytimePlannerThree.getBestPlanYet();
      assertPlansAreEqual(planOne, bestPlanYet, 1);
      
      int numberOfIterations = 2;
      
      for (int i=0; i<numberOfIterations; i++)
      {
         setPlanarRegions(anytimePlannerThree); 
         setGoalAndInitialStanceFoot(anytimePlannerThree);
         ThreadTools.sleep(10L);
         setPlanarRegions(anytimePlannerThree); 

         ThreadTools.sleep(2000L);
         bestPlanYet = anytimePlannerThree.getBestPlanYet();
         assertPlansAreEqual(planOne, bestPlanYet, 1);

      }

      anytimePlannerThree.requestStop();
      
      if (visualize)
      {
         ThreadTools.sleepForever();
      }
   }

   private PlanarRegionBipedalFootstepPlanner createNormalPlanner()
   {
      YoVariableRegistry registryTwo = new YoVariableRegistry("Two");
      PlanarRegionBipedalFootstepPlanner normalPlannerTwo = new PlanarRegionBipedalFootstepPlanner(registryTwo);
      normalPlannerTwo.setMaximumNumberOfNodesToExpand(1000);
      BipedalFootstepPlannerParameters parametersTwo = normalPlannerTwo.getParameters();
      setParameters(parametersTwo);
      return normalPlannerTwo;
   }

   private SimplePlanarRegionBipedalAnytimeFootstepPlanner createAnytimePlanner()
   {
      YoVariableRegistry registryOne = new YoVariableRegistry("One");
      SimplePlanarRegionBipedalAnytimeFootstepPlanner anytimePlannerOne = new SimplePlanarRegionBipedalAnytimeFootstepPlanner(registryOne);
      BipedalFootstepPlannerParameters parametersOne = anytimePlannerOne.getParameters();
      setParameters(parametersOne);
      return anytimePlannerOne;
   }

   private void assertPlansAreEqual(FootstepPlan planOne, FootstepPlan planTwo, int withinNSteps)
   {
      assertTrue(Math.abs(planOne.getNumberOfSteps() - planTwo.getNumberOfSteps()) <= withinNSteps);

      int numberOfStepsToCheck = Math.min(planOne.getNumberOfSteps(), planTwo.getNumberOfSteps());
      for (int i = 0; i < numberOfStepsToCheck; i++)
      {
         SimpleFootstep footstepOne = planOne.getFootstep(i);
         SimpleFootstep footstepTwo = planTwo.getFootstep(i);

         assertTrue(footstepOne.epsilonEquals(footstepTwo, 1e-5));
      }
   }

   private void setPlanarRegions(PlanarRegionBipedalFootstepPlanner planner)
   {
      double startX = 0.0;
      double startY = 0.0;
      double cinderBlockSize = 0.4;
      int courseWidthXInNumberOfBlocks = 21;
      int courseLengthYInNumberOfBlocks = 6;
      PlanarRegionsList planarRegionsListOne = PlanarRegionsListExamples.generateCinderBlockField(startX, startY, cinderBlockSize, courseWidthXInNumberOfBlocks,
                                                                                                  courseLengthYInNumberOfBlocks);
      planner.setPlanarRegions(planarRegionsListOne);
   }
   
   private void setGoalAndInitialStanceFoot(FootstepPlanner planner)
   {
      RobotSide initialStanceSide = RobotSide.LEFT;

      double xGoal = 9.0;
      double yGoal = 0.3;
      double yawGoal = 0.2;
      Point2d goalPosition = new Point2d(xGoal, yGoal);
      FramePose2d goalPose2d = new FramePose2d(ReferenceFrame.getWorldFrame(), goalPosition, yawGoal);

      double xInitialStanceFoot = 0.0;
      double yInitialStanceFoot = 0.0;
      double yawInitial = 0.0;
      Point2d initialStanceFootPosition = new Point2d(xInitialStanceFoot, yInitialStanceFoot);
      FramePose2d initialStanceFootPose2d = new FramePose2d(ReferenceFrame.getWorldFrame(), initialStanceFootPosition, yawInitial);

      FramePose initialStanceFootPose3d = FlatGroundPlanningUtils.poseFormPose2d(initialStanceFootPose2d);
      FramePose goalPose = FlatGroundPlanningUtils.poseFormPose2d(goalPose2d);

      FootstepPlannerGoal goal = new FootstepPlannerGoal();
      goal.setFootstepPlannerGoalType(FootstepPlannerGoalType.POSE_BETWEEN_FEET);
      goal.setGoalPoseBetweenFeet(goalPose);
      goal.setXYGoal(new Point2d(goalPose.getX(), goalPose.getY()), 0.5);

      planner.setInitialStanceFoot(initialStanceFootPose3d, initialStanceSide);
      planner.setGoal(goal);
   }

   private void setParameters(BipedalFootstepPlannerParameters parameters)
   {
      parameters.setMaximumStepReach(0.4);
      parameters.setMaximumStepZ(0.25);
      parameters.setMaximumStepXWhenForwardAndDown(0.25);
      parameters.setMaximumStepZWhenForwardAndDown(0.25);
      parameters.setMaximumStepYaw(0.15);
      parameters.setMaximumStepWidth(0.4);
      parameters.setMinimumStepWidth(0.15);
      parameters.setMinimumFootholdPercent(0.8);

      double idealFootstepLength = 0.3;
      double idealFootstepWidth = 0.2;
      parameters.setIdealFootstep(idealFootstepLength, idealFootstepWidth);
   }
}
