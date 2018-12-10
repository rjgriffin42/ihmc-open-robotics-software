package us.ihmc.pathPlanning.visibilityGraphs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import us.ihmc.euclid.geometry.BoundingBox3D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.pathPlanning.visibilityGraphs.clusterManagement.Cluster;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.Connection;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.ConnectionPoint3D;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.InterRegionVisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.NavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.SingleSourceVisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphEdge;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityGraphNode;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMap;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapSolution;
import us.ihmc.pathPlanning.visibilityGraphs.dataStructure.VisibilityMapWithNavigableRegion;
import us.ihmc.pathPlanning.visibilityGraphs.interfaces.InterRegionConnectionFilter;
import us.ihmc.pathPlanning.visibilityGraphs.tools.PlanarRegionTools;
import us.ihmc.robotics.geometry.PlanarRegion;

public class VisibilityGraph
{
   private ArrayList<VisibilityGraphNavigableRegion> visibilityGraphNavigableRegions = new ArrayList<>();
   private final NavigableRegions navigableRegions;
   private final ArrayList<VisibilityGraphEdge> crossRegionEdges = new ArrayList<VisibilityGraphEdge>();

   private VisibilityGraphNode startNode, goalNode;

   private final InterRegionConnectionFilter interRegionConnectionFilter;

   public VisibilityGraph(NavigableRegions navigableRegions, InterRegionConnectionFilter interRegionConnectionFilter)
   {
      this.navigableRegions = navigableRegions;
      this.interRegionConnectionFilter = interRegionConnectionFilter;

      List<NavigableRegion> naviableRegionsList = navigableRegions.getNaviableRegionsList();

      for (NavigableRegion navigableRegion : naviableRegionsList)
      {
         VisibilityGraphNavigableRegion visibilityGraphNavigableRegion = new VisibilityGraphNavigableRegion(navigableRegion);
 
         //TODO: +++JEP: Just get rid of createEdgesAroundClusterRing? Or store Nodes with clusters somehow?
         // Set createEdgesAroundClusterRing to true if at the beginning you want to create loops around the clusters.
         boolean createEdgesAroundClusterRing = false;

         visibilityGraphNavigableRegion.createGraphAroundClusterRings(createEdgesAroundClusterRing);

         visibilityGraphNavigableRegions.add(visibilityGraphNavigableRegion);
      }
   }

   public void fullyExpandVisibilityGraph()
   {
      for (VisibilityGraphNavigableRegion visibilityGraphNavigableRegion : visibilityGraphNavigableRegions)
      {
         visibilityGraphNavigableRegion.createGraphBetweenInnerClusterRings();
      }

      for (int sourceIndex = 0; sourceIndex < visibilityGraphNavigableRegions.size(); sourceIndex++)
      {
         VisibilityGraphNavigableRegion sourceNavigableRegion = visibilityGraphNavigableRegions.get(sourceIndex);

         for (int targetIndex = sourceIndex + 1; targetIndex < visibilityGraphNavigableRegions.size(); targetIndex++)
         {
            VisibilityGraphNavigableRegion targetNavigableRegion = visibilityGraphNavigableRegions.get(targetIndex);
            createInterRegionVisibilityConnections(sourceNavigableRegion, targetNavigableRegion);
         }
      }

      for (VisibilityGraphNavigableRegion visibilityGraphNavigableRegion : visibilityGraphNavigableRegions)
      {
         List<VisibilityGraphNode> allNavigableNodes = visibilityGraphNavigableRegion.getAllNavigableNodes();
         for (VisibilityGraphNode node : allNavigableNodes)
         {
            node.setEdgesHaveBeenDetermined(true);
         }
      }
   }

   public void computeInnerAndInterEdges(VisibilityGraphNode sourceNode)
   {
      VisibilityGraphNavigableRegion sourceVisibilityGraphNavigableRegion = sourceNode.getVisibilityGraphNavigableRegion();
      sourceVisibilityGraphNavigableRegion.addInnerRegionEdgesFromSourceNode(sourceNode);

      NavigableRegion sourceNavigableRegion = sourceVisibilityGraphNavigableRegion.getNavigableRegion();
      List<Cluster> sourceObstacleClusters = sourceNavigableRegion.getObstacleClusters();

      for (VisibilityGraphNavigableRegion targetVisibilityGraphNavigableRegion : visibilityGraphNavigableRegions)
      {
         if (targetVisibilityGraphNavigableRegion == sourceVisibilityGraphNavigableRegion)
            continue;

         //TOOD: +++JEP: Efficiently add Bounding Box check before going through all combinations. Perhaps have a cached bounding box reachable map?

         NavigableRegion targetNavigableRegion = targetVisibilityGraphNavigableRegion.getNavigableRegion();
         List<Cluster> targetObstacleClusters = targetNavigableRegion.getObstacleClusters();

         List<VisibilityGraphNode> allNavigableNodes = targetVisibilityGraphNavigableRegion.getAllNavigableNodes();
         createInterRegionVisibilityConnections(sourceNode, allNavigableNodes, sourceObstacleClusters, targetObstacleClusters, interRegionConnectionFilter,
                                                crossRegionEdges);
      }

      sourceNode.setEdgesHaveBeenDetermined(true);
   }

   public static void connectNodeToInnerRegionNodes(VisibilityGraphNode sourceNode, VisibilityGraphNavigableRegion visibilityGraphNavigableRegion,
                                                    VisibilityGraphNode nodeToAttachToIfInSameRegion)
   {
      visibilityGraphNavigableRegion.addInnerRegionEdgesFromSourceNode(sourceNode);

      if (nodeToAttachToIfInSameRegion != null)
      {
         if (sourceNode.getRegionId() == nodeToAttachToIfInSameRegion.getRegionId())
         {
            visibilityGraphNavigableRegion.addInnerEdgeFromSourceToTargetNodeIfVisible(sourceNode, nodeToAttachToIfInSameRegion);
         }
      }

      sourceNode.setEdgesHaveBeenDetermined(true);
   }

   public static VisibilityGraphNode createNode(Point3DReadOnly sourceInWorld, VisibilityGraphNavigableRegion visibilityGraphNavigableRegion)
   {
      NavigableRegion navigableRegion = visibilityGraphNavigableRegion.getNavigableRegion();
      Point3D sourceInLocal3D = new Point3D(sourceInWorld);
      navigableRegion.transformFromWorldToLocal(sourceInLocal3D);
      Point2D sourceInLocal = new Point2D(sourceInLocal3D);

      Point3D projectedSourceInWorld = new Point3D(sourceInLocal);
      navigableRegion.transformFromLocalToWorld(projectedSourceInWorld);

      return new VisibilityGraphNode(projectedSourceInWorld, sourceInLocal, visibilityGraphNavigableRegion);
   }

   public void createInterRegionVisibilityConnections(VisibilityGraphNavigableRegion sourceNavigableRegion,
                                                      VisibilityGraphNavigableRegion targetNavigableRegion)
   {
      createInterRegionVisibilityConnections(sourceNavigableRegion, targetNavigableRegion, interRegionConnectionFilter, crossRegionEdges);
   }

   public VisibilityGraphNode getStartNode()
   {
      return startNode;
   }

   public VisibilityGraphNode getGoalNode()
   {
      return goalNode;
   }

   public List<VisibilityGraphEdge> getStartEdges()
   {
      if (startNode == null)
         return null;
      return startNode.getEdges();
   }

   public List<VisibilityGraphEdge> getGoalEdges()
   {
      if (goalNode == null)
         return null;
      return goalNode.getEdges();
   }

   public VisibilityGraphNode setStart(Point3DReadOnly sourceLocationInWorld, double searchHostEpsilon)
   {
      //TODO: Need a fallback map if start is not connectable...

      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion = getVisibilityGraphNavigableRegionContainingThisPoint(sourceLocationInWorld,
                                                                                                                           searchHostEpsilon);
      if (visibilityGraphNavigableRegion == null)
         return null;

      startNode = createNode(sourceLocationInWorld, visibilityGraphNavigableRegion);
      connectNodeToInnerRegionNodes(startNode, visibilityGraphNavigableRegion, goalNode);

      return startNode;
   }

   public VisibilityGraphNode setGoal(Point3DReadOnly sourceLocationInWorld, double searchHostEpsilon)
   {
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion = getVisibilityGraphNavigableRegionContainingThisPoint(sourceLocationInWorld,
                                                                                                                           searchHostEpsilon);
      if (visibilityGraphNavigableRegion == null)
         return null;

      goalNode = createNode(sourceLocationInWorld, visibilityGraphNavigableRegion);
      connectNodeToInnerRegionNodes(goalNode, visibilityGraphNavigableRegion, startNode);

      return goalNode;
   }

   private VisibilityGraphNavigableRegion getVisibilityGraphNavigableRegionContainingThisPoint(Point3DReadOnly sourceLocationInWorld, double searchHostEpsilon)
   {
      NavigableRegion hostNavigableRegion = PlanarRegionTools.getNavigableRegionContainingThisPoint(sourceLocationInWorld, navigableRegions, searchHostEpsilon);
      VisibilityGraphNavigableRegion visibilityGraphNavigableRegion = getVisibilityGraphNavigableRegion(hostNavigableRegion);
      return visibilityGraphNavigableRegion;
   }

   private VisibilityGraphNavigableRegion getVisibilityGraphNavigableRegion(NavigableRegion navigableRegion)
   {
      if (navigableRegion == null)
         return null;

      for (VisibilityGraphNavigableRegion visibilityGraphNavigableRegion : visibilityGraphNavigableRegions)
      {
         if (visibilityGraphNavigableRegion.getNavigableRegion() == navigableRegion)
            return visibilityGraphNavigableRegion;
      }

      return null;
   }

   public ArrayList<VisibilityGraphNavigableRegion> getVisibilityGraphNavigableRegions()
   {
      return visibilityGraphNavigableRegions;
   }

   public ArrayList<VisibilityGraphEdge> getCrossRegionEdges()
   {
      return crossRegionEdges;
   }

   public static void createInterRegionVisibilityConnections(VisibilityGraphNavigableRegion sourceNavigableRegion,
                                                             VisibilityGraphNavigableRegion targetNavigableRegion, InterRegionConnectionFilter filter,
                                                             ArrayList<VisibilityGraphEdge> edgesToPack)
   {
      int sourceId = sourceNavigableRegion.getMapId();
      int targetId = targetNavigableRegion.getMapId();

      if (sourceId == targetId)
         return;

      PlanarRegion sourceHomePlanarRegion = sourceNavigableRegion.getNavigableRegion().getHomePlanarRegion();
      PlanarRegion targetHomePlanarRegion = targetNavigableRegion.getNavigableRegion().getHomePlanarRegion();

      BoundingBox3D sourceHomeRegionBoundingBox = sourceHomePlanarRegion.getBoundingBox3dInWorld();
      BoundingBox3D targetHomeRegionBoundingBox = targetHomePlanarRegion.getBoundingBox3dInWorld();

      // If the source and target regions are simply too far apart, then do not check their individual points.
      if (!sourceHomeRegionBoundingBox.intersectsEpsilon(targetHomeRegionBoundingBox, filter.getMaximumInterRegionConnetionDistance()))
      {
         return;
      }

      List<Cluster> sourceObstacleClusters = sourceNavigableRegion.getNavigableRegion().getObstacleClusters();
      List<Cluster> targetObstacleClusters = targetNavigableRegion.getNavigableRegion().getObstacleClusters();

      List<VisibilityGraphNode> sourceRegionNodes = sourceNavigableRegion.getAllNavigableNodes();
      List<VisibilityGraphNode> targetRegionNodes = targetNavigableRegion.getAllNavigableNodes();

      createInterRegionVisibilityConnections(sourceRegionNodes, targetRegionNodes, sourceObstacleClusters, targetObstacleClusters, filter, edgesToPack);
   }

   public static void createInterRegionVisibilityConnections(List<VisibilityGraphNode> sourceNodeList, List<VisibilityGraphNode> targetNodeList,
                                                             List<Cluster> sourceObstacleClusters, List<Cluster> targetObstacleClusters,
                                                             InterRegionConnectionFilter filter, ArrayList<VisibilityGraphEdge> edgesToPack)
   {
      for (VisibilityGraphNode sourceNode : sourceNodeList)
      {
         createInterRegionVisibilityConnections(sourceNode, targetNodeList, sourceObstacleClusters, targetObstacleClusters, filter, edgesToPack);
      }
   }

   public static void createInterRegionVisibilityConnections(VisibilityGraphNode sourceNode, List<VisibilityGraphNode> targetNodeList,
                                                             List<Cluster> sourceObstacleClusters, List<Cluster> targetObstacleClusters,
                                                             InterRegionConnectionFilter filter, ArrayList<VisibilityGraphEdge> edgesToPack)
   {
      for (VisibilityGraphNode targetNode : targetNodeList)
      {
         if (filter.isConnectionValid(sourceNode.getPointInWorld(), targetNode.getPointInWorld()))
         {
            VisibilityGraphEdge edge = new VisibilityGraphEdge(sourceNode, targetNode);
            sourceNode.addEdge(edge);
            targetNode.addEdge(edge);

            edgesToPack.add(edge);
         }
      }
   }

   public VisibilityMapSolution createVisibilityMapSolution()
   {
      VisibilityMapSolution solution = new VisibilityMapSolution();

      solution.setNavigableRegions(navigableRegions);

      ArrayList<VisibilityMapWithNavigableRegion> visibilityMapsWithNavigableRegions = new ArrayList<>();

      for (VisibilityGraphNavigableRegion visibilityGraphNavigableRegion : visibilityGraphNavigableRegions)
      {

         NavigableRegion navigableRegion = visibilityGraphNavigableRegion.getNavigableRegion();
         List<VisibilityGraphEdge> allEdges = visibilityGraphNavigableRegion.getAllEdges();

         VisibilityMapWithNavigableRegion visibilityMapWithNavigableRegion = new VisibilityMapWithNavigableRegion(navigableRegion);

         Collection<Connection> connections = createConnectionsFromEdges(allEdges);

         VisibilityMap visibilityMapInWorld = new VisibilityMap(connections);
         visibilityMapWithNavigableRegion.setVisibilityMapInWorld(visibilityMapInWorld);
         visibilityMapsWithNavigableRegions.add(visibilityMapWithNavigableRegion);
      }

      solution.setVisibilityMapsWithNavigableRegions(visibilityMapsWithNavigableRegions);

      InterRegionVisibilityMap interRegionVisibilityMap = new InterRegionVisibilityMap();

      for (VisibilityGraphEdge crossRegionEdge : crossRegionEdges)
      {
         Connection connection = new Connection(crossRegionEdge.getSourcePoint(), crossRegionEdge.getTargetPoint());
         interRegionVisibilityMap.addConnection(connection);
      }

      if (startNode != null)
      {
         Collection<Connection> startConnections = createConnectionsFromEdges(startNode.getEdges());
         SingleSourceVisibilityMap startMap = new SingleSourceVisibilityMap(startNode.getPointInWorld(), startNode.getRegionId(), startConnections);
         solution.setStartMap(startMap);
      }

      if (goalNode != null)
      {
         Collection<Connection> goalConnections = createConnectionsFromEdges(goalNode.getEdges());
         SingleSourceVisibilityMap goalMap = new SingleSourceVisibilityMap(goalNode.getPointInWorld(), goalNode.getRegionId(), goalConnections);
         solution.setGoalMap(goalMap);
      }

      solution.setInterRegionVisibilityMap(interRegionVisibilityMap);
      return solution;
   }

   private Collection<Connection> createConnectionsFromEdges(List<VisibilityGraphEdge> edges)
   {
      Collection<Connection> connections = new ArrayList<Connection>();

      for (VisibilityGraphEdge edge : edges)
      {
         ConnectionPoint3D sourcePoint = edge.getSourcePoint();
         ConnectionPoint3D targetPoint = edge.getTargetPoint();
         connections.add(new Connection(sourcePoint, targetPoint));
      }
      return connections;
   }

}
