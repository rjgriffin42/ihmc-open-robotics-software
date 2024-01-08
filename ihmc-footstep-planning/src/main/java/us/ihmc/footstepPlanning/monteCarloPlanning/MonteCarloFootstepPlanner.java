package us.ihmc.footstepPlanning.monteCarloPlanning;

import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.footstepPlanning.FootstepPlan;
import us.ihmc.footstepPlanning.MonteCarloFootstepPlannerParameters;
import us.ihmc.log.LogTools;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MonteCarloFootstepPlanner
{
   private MonteCarloFootstepPlannerStatistics statistics;
   private MonteCarloFootstepPlannerParameters parameters;
   private TerrainPlanningDebugger debugger;
   private MonteCarloFootstepNode root;

   private MonteCarloFootstepPlannerRequest request;
   private final HashMap<MonteCarloFootstepNode, MonteCarloFootstepNode> visitedNodes = new HashMap<>();
   private final Random random = new Random();
   private SideDependentList<ConvexPolygon2D> footPolygons;

   private boolean planning = false;
   private int uniqueNodeId = 0;
   private int cellsPerMeter = 50;

   public MonteCarloFootstepPlanner(MonteCarloFootstepPlannerParameters parameters, SideDependentList<ConvexPolygon2D> footPolygons)
   {
      this.parameters = parameters;
      this.footPolygons = footPolygons;
      this.statistics = new MonteCarloFootstepPlannerStatistics();
      this.debugger = new TerrainPlanningDebugger(this);
   }

   public FootstepPlan generateFootstepPlan(MonteCarloFootstepPlannerRequest request)
   {
      this.request = request;
      planning = true;

      // Debug Only
      debugger.setRequest(request);
      debugger.refresh(request.getTerrainMapData());
      statistics.startTotalTime();

      // Initialize Root
      if (root == null)
      {
         Point2D position = new Point2D(request.getStartFootPoses().get(RobotSide.LEFT).getPosition().getX() * cellsPerMeter,
                                        request.getStartFootPoses().get(RobotSide.LEFT).getPosition().getY() * cellsPerMeter);
         float yaw = (float) request.getStartFootPoses().get(RobotSide.LEFT).getYaw();
         Point3D state = new Point3D(position.getX(), position.getY(), yaw);
         root = new MonteCarloFootstepNode(state, null, request.getRequestedInitialStanceSide(), uniqueNodeId++);
      }

      // Perform Monte-Carlo Tree Search
      for (int i = 0; i < parameters.getNumberOfIterations(); i++)
      {
         updateTree(root, request);
      }

      // Compute plan from maximum value path in the tree so far
      FootstepPlan plan = MonteCarloPlannerTools.getFootstepPlanFromTree(root, request, footPolygons);

      // Debug Only
      debugger.printScoreStats(root, request, parameters);
      statistics.stopTotalTime();
      statistics.setLayerCountsString(MonteCarloPlannerTools.getLayerCountsString(root));
      statistics.logToFile(false, true);

      planning = false;
      return plan;
   }

   public void updateTree(MonteCarloFootstepNode node, MonteCarloFootstepPlannerRequest request)
   {
      if (node == null)
      {
         LogTools.debug("Node is null");
         return;
      }

      // Pruning and Sorting
      statistics.startPruningTime();
      node.sortChildren();
      node.prune(parameters.getMaxNumberOfChildNodes());
      statistics.stopPruningTime();

      if (node.getChildren().isEmpty())
      {
         MonteCarloFootstepNode childNode = node;
         if (node.getLevel() < parameters.getMaxTreeDepth())
         {
            // Expansion and Random Selection
            statistics.startExpansionTime();
            childNode = expand(node, request);
            statistics.stopExpansionTime();
         }

         if (childNode != null)
         {
            // Simulation
            statistics.startSimulationTime();
            double score = simulate(childNode, request);
            statistics.stopSimulationTime();

            // Back Propagation
            statistics.startPropagationTime();
            childNode.setValue((float) score);
            backPropagate(node, (float) score);
            statistics.stopPropagationTime();
         }
      }
      else
      {
         // Maximum UCB Search
         statistics.startSearchTime();
         float bestScore = 0;
         MonteCarloFootstepNode bestNode = null;
         for (MonteCarloTreeNode child : node.getChildren())
         {
            child.updateUpperConfidenceBound(parameters.getExplorationConstant());
            if (child.getUpperConfidenceBound() >= bestScore)
            {
               bestScore = child.getUpperConfidenceBound();
               bestNode = (MonteCarloFootstepNode) child;
            }
         }
         statistics.stopSearchTime();

         // Recursion into highest UCB node
         updateTree(bestNode, request);
      }
   }

   public MonteCarloFootstepNode expand(MonteCarloFootstepNode node, MonteCarloFootstepPlannerRequest request)
   {
      ArrayList<?> availableStates = node.getAvailableStates(request, parameters);
      for (Object newStateObj : availableStates)
      {
         MonteCarloFootstepNode newState = (MonteCarloFootstepNode) newStateObj;
         double score = MonteCarloPlannerTools.scoreFootstepNode(node, newState, request, parameters, false);
         if (score > parameters.getInitialValueCutoff())
         {
            // Create node if not previously visited, or pull from visited node map
            if (visitedNodes.getOrDefault(newState, null) != null)
            {
               MonteCarloFootstepNode existingNode = visitedNodes.get(newState);
               node.addChild(existingNode);
               existingNode.getParents().add(node);
            }
            else
            {
               MonteCarloFootstepNode postNode = new MonteCarloFootstepNode(newState.getState(), node, newState.getRobotSide(), uniqueNodeId++);
               postNode.setValue((float) score);
               visitedNodes.put(newState, postNode);
               node.addChild(postNode);
            }
         }
      }

      if (node.getChildren().isEmpty())
      {
         LogTools.debug("No Children");
         return null;
      }

      if (node.getChildren().size() > 1)
         return (MonteCarloFootstepNode) node.getChildren().get(random.nextInt(0, node.getChildren().size() - 1));
      else
         return null;
   }

   public double simulate(MonteCarloFootstepNode node, MonteCarloFootstepPlannerRequest request)
   {
      double score = 0;

      MonteCarloFootstepNode simulationState = new MonteCarloFootstepNode(node.getState(), null, node.getRobotSide().getOppositeSide(), 0);

      for (int i = 0; i < parameters.getNumberOfSimulations(); i++)
      {
         ArrayList<MonteCarloFootstepNode> nextStates = simulationState.getAvailableStates(request, parameters);
         if (nextStates.isEmpty())
            break;

         int actionIndex = random.nextInt(0, nextStates.size() - 1);
         simulationState = nextStates.get(actionIndex);

         score += MonteCarloPlannerTools.scoreFootstepNode(node, simulationState, request, parameters, false);
      }

      return score;
   }

   public void backPropagate(MonteCarloFootstepNode node, float score)
   {
      node.setValue(Math.max(score, node.getValue()));
      node.incrementVisits();

      if (!node.getParents().isEmpty())
      {
         for (MonteCarloTreeNode parent : node.getParents())
         {
            backPropagate((MonteCarloFootstepNode) parent, score);
         }
      }
   }

   public void reset(MonteCarloFootstepPlannerRequest request)
   {
      random.setSeed(100);
      uniqueNodeId = 0;
      visitedNodes.clear();

      if (request == null)
         root = new MonteCarloFootstepNode(new Point3D(), null, RobotSide.LEFT, uniqueNodeId++);
      else
         root = new MonteCarloFootstepNode(new Point3D(request.getStartFootPoses().get(RobotSide.LEFT).getPosition().getX() * cellsPerMeter,
                                                       request.getStartFootPoses().get(RobotSide.LEFT).getPosition().getY() * cellsPerMeter,
                                                       request.getStartFootPoses().get(RobotSide.LEFT).getYaw()),
                                           null,
                                           request.getRequestedInitialStanceSide(),
                                           uniqueNodeId++);
   }

   public Vector3D transitionToOptimal()
   {
      if (root.getChildren().isEmpty())
      {
         reset(request);
         return new Vector3D();
      }

      for (MonteCarloTreeNode child : root.getChildren())
      {
         child.getParents().clear();
      }
      MonteCarloFootstepNode maxNode = (MonteCarloFootstepNode) root.getMaxQueueNode();
      Vector3D action = new Vector3D(maxNode.getState());
      action.sub(root.getState());
      action.scale(1 / 50.0);

      root = maxNode;
      MonteCarloPlannerTools.pruneTree(root, parameters.getMaxNumberOfChildNodes());
      MonteCarloPlannerTools.resetNodeLevels(root, 0);

      return action;
   }

   public MonteCarloTreeNode getRoot()
   {
      return root;
   }

   public List<MonteCarloFootstepNode> getVisitedNodes()
   {
      return new ArrayList<>(visitedNodes.values());
   }

   public TerrainPlanningDebugger getDebugger()
   {
      return debugger;
   }

   public boolean isPlanning()
   {
      return planning;
   }
}
