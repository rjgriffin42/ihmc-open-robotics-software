package us.ihmc.footstepPlanning.graphSearch.graph.visualization;

import controller_msgs.msg.dds.FootstepNodeDataListMessage;
import controller_msgs.msg.dds.FootstepNodeDataMessage;
import controller_msgs.msg.dds.FootstepPlannerCellMessage;
import controller_msgs.msg.dds.FootstepPlannerOccupancyMapMessage;
import us.ihmc.euclid.geometry.interfaces.Pose3DReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapperReadOnly;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.footstepPlanning.graphSearch.graph.LatticeNode;
import us.ihmc.footstepPlanning.graphSearch.listeners.BipedalFootstepPlannerListener;
import us.ihmc.idl.IDLSequence.Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class MessageBasedPlannerListener implements BipedalFootstepPlannerListener
{
   private final FootstepNodeSnapperReadOnly snapper;
   private final HashMap<FootstepNode, BipedalFootstepPlannerNodeRejectionReason> rejectionReasons = new HashMap<>();
   private final HashMap<FootstepNode, List<FootstepNode>> childMap = new HashMap<>();
   private final List<FootstepNode> lowestCostPlan = new ArrayList<>();
   private final PlannerOccupancyMap occupancyMapSinceLastReport = new PlannerOccupancyMap();
   private final PlannerLatticeMap latticeMapSinceLastReport = new PlannerLatticeMap();

   private final long occupancyMapBroadcastDt;
   private long lastBroadcastTime = -1;

   public MessageBasedPlannerListener(FootstepNodeSnapperReadOnly snapper, long occupancyMapBroadcastDt)
   {
      this.snapper = snapper;
      this.occupancyMapBroadcastDt = occupancyMapBroadcastDt;
   }

   @Override
   public void addNode(FootstepNode node, FootstepNode previousNode)
   {
      if (previousNode == null)
      {
         rejectionReasons.clear();
         childMap.clear();
         lowestCostPlan.clear();
      }
      else
      {
         childMap.computeIfAbsent(previousNode, n -> new ArrayList<>()).add(node);
         occupancyMapSinceLastReport.addOccupiedCell(new PlannerCell(node.getXIndex(), node.getYIndex()));
         latticeMapSinceLastReport.addLatticeNode(new LatticeNode(node.getXIndex(), node.getYIndex(), node.getYawIndex()));
      }
   }

   @Override
   public void reportLowestCostNodeList(List<FootstepNode> plan)
   {
      lowestCostPlan.clear();
      lowestCostPlan.addAll(plan);
   }

   @Override
   public void rejectNode(FootstepNode rejectedNode, FootstepNode parentNode, BipedalFootstepPlannerNodeRejectionReason reason)
   {
      rejectionReasons.put(rejectedNode, reason);
   }

   @Override
   public void tickAndUpdate()
   {
      long currentTime = System.currentTimeMillis();

      if (lastBroadcastTime == -1)
         lastBroadcastTime = currentTime;

      if (currentTime - lastBroadcastTime > occupancyMapBroadcastDt)
      {
         broadcastOccupancyMap(occupancyMapSinceLastReport);
         broadcastLatticeMap(latticeMapSinceLastReport);

         occupancyMapSinceLastReport.clear();
         latticeMapSinceLastReport.clear();

         FootstepNodeDataListMessage nodeDataListMessage = packLowestCostPlanMessage();
         if (nodeDataListMessage != null)
            broadcastNodeData(nodeDataListMessage);

         lastBroadcastTime = currentTime;
      }
   }

   @Override
   public void plannerFinished(List<FootstepNode> plan)
   {
      broadcastOccupancyMap(occupancyMapSinceLastReport);
   }

   abstract void broadcastOccupancyMap(PlannerOccupancyMap occupancyMap);

   abstract void broadcastLatticeMap(PlannerLatticeMap latticeMap);

   abstract void broadcastNodeData(FootstepNodeDataListMessage nodeDataListMessage);

   FootstepNodeDataListMessage packLowestCostPlanMessage()
   {
      if (lowestCostPlan.isEmpty())
         return null;

      FootstepNodeDataListMessage nodeDataListMessage = new FootstepNodeDataListMessage();
      Object<FootstepNodeDataMessage> nodeDataList = nodeDataListMessage.getNodeData();
      nodeDataList.clear();
      for (int i = 0; i < lowestCostPlan.size(); i++)
      {
         FootstepNode node = lowestCostPlan.get(i);
         FootstepNodeDataMessage nodeDataMessage = nodeDataList.add();
         setNodeDataMessage(nodeDataMessage, node, -1);
      }

      nodeDataListMessage.setIsFootstepGraph(false);
      lowestCostPlan.clear();

      return nodeDataListMessage;
   }

   private void setNodeDataMessage(FootstepNodeDataMessage nodeDataMessage, FootstepNode node, int parentNodeIndex)
   {
      byte rejectionReason = rejectionReasons.containsKey(node) ? rejectionReasons.get(node).toByte() : (byte) 255;

      FootstepNodeSnapData snapData = snapper.getSnapData(node);

      Pose3DReadOnly nodePose = FootstepNodeTools.getNodePoseInWorld(node, snapData.getSnapTransform());
      nodeDataMessage.setParentNodeId(parentNodeIndex);
      nodeDataMessage.setRobotSide(node.getRobotSide().toByte());
      nodeDataMessage.getPosition().set(nodePose.getPosition());
      nodeDataMessage.getOrientation().set(nodePose.getOrientation());
      nodeDataMessage.setBipedalFootstepPlannerNodeRejectionReason(rejectionReason);
   }

}
