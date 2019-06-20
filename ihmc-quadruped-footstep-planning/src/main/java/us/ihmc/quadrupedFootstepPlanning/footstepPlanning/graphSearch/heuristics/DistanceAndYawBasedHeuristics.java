package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.heuristics;

import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class DistanceAndYawBasedHeuristics extends CostToGoHeuristics
{
   private final FootstepNodeSnapper snapper;

   public DistanceAndYawBasedHeuristics(FootstepNodeSnapper snapper, FootstepPlannerParameters parameters)
   {
      super(parameters);

      this.snapper = snapper;
   }

   @Override
   protected double computeHeuristics(FootstepNode node, FootstepNode goalNode)
   {
      double bodyDistance = node.euclideanDistance(goalNode);

      double referenceYaw = computeReferenceYaw(node, goalNode);
      double angleDifference = AngleTools.computeAngleDifferenceMinusPiToPi(node.getNominalYaw(), referenceYaw);
      double yawHeuristicCost = parameters.getYawWeight() * Math.abs(angleDifference);


      double heightCost = 0.0;

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         int goalNodeXIndex = goalNode.getXIndex(robotQuadrant);
         int goalNodeYIndex = goalNode.getYIndex(robotQuadrant);
         int nodeXIndex = node.getXIndex(robotQuadrant);
         int nodeYIndex = node.getYIndex(robotQuadrant);

         FootstepNodeSnapData goalNodeData = snapper.snapFootstepNode(goalNodeXIndex, goalNodeYIndex);
         FootstepNodeSnapData nodeData = snapper.snapFootstepNode(nodeXIndex, nodeYIndex);

         if (nodeData == null || goalNodeData == null)
         {
            heightCost = 0.0;
            break;
         }

         RigidBodyTransform nodeTransform = new RigidBodyTransform();
         RigidBodyTransform goalNodeTransform = new RigidBodyTransform();

         FootstepNodeTools.getSnappedNodeTransformToWorld(nodeXIndex, nodeYIndex, nodeData.getSnapTransform(), nodeTransform);
         FootstepNodeTools.getSnappedNodeTransformToWorld(goalNodeXIndex, goalNodeYIndex, goalNodeData.getSnapTransform(), goalNodeTransform);

         if (!nodeTransform.containsNaN() && !goalNodeTransform.containsNaN())
         {
            double heightChange = goalNodeTransform.getTranslationVector().getZ() - nodeTransform.getTranslationVector().getZ();

            if (heightChange > 0.0)
               heightCost += parameters.getStepUpWeight() * heightChange;
            else
               heightCost += Math.abs(parameters.getStepDownWeight() * heightChange);
         }
      }

      return yawHeuristicCost + heightCost + parameters.getDistanceHeuristicWeight() * bodyDistance;
   }

   private double computeReferenceYaw(FootstepNode node, FootstepNode goalNode)
   {
      double distanceToGoal = node.euclideanDistance(goalNode);
      double finalTurnProximity = 1.0;

      double minimumBlendDistance = 0.75 * finalTurnProximity;
      double maximumBlendDistance = 1.25 * finalTurnProximity;

      double pathHeading = Math.atan2(goalNode.getOrComputeXGaitCenterPoint().getY() - node.getOrComputeXGaitCenterPoint().getY(),
                                      goalNode.getOrComputeXGaitCenterPoint().getX() - node.getOrComputeXGaitCenterPoint().getX());
      pathHeading = AngleTools.trimAngleMinusPiToPi(pathHeading);

      double yawMultiplier;
      if (distanceToGoal < minimumBlendDistance)
         yawMultiplier = 0.0;
      else if(distanceToGoal > maximumBlendDistance)
         yawMultiplier = 1.0;
      else
         yawMultiplier = (distanceToGoal - minimumBlendDistance) / (maximumBlendDistance - minimumBlendDistance);

      double referenceHeading = yawMultiplier * pathHeading;
      referenceHeading += (1.0 - yawMultiplier) * goalNode.getNominalYaw();
      return AngleTools.trimAngleMinusPiToPi(referenceHeading);
   }
}
