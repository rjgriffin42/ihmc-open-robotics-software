package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.nodeExpansion;

import us.ihmc.euclid.axisAngle.AxisAngle;
import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Tuple2DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DReadOnly;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters.FootstepPlannerParameters;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettings;
import us.ihmc.quadrupedPlanning.QuadrupedXGaitSettingsReadOnly;
import us.ihmc.robotics.robotSide.RobotQuadrant;

import java.util.ArrayList;
import java.util.HashSet;

import static us.ihmc.robotics.robotSide.RobotQuadrant.*;

public class ParameterBasedNodeExpansion implements FootstepNodeExpansion
{
   private FootstepNode goalNode;
   private final FootstepPlannerParameters parameters;
   private final QuadrupedXGaitSettingsReadOnly xGaitSettings;

   public ParameterBasedNodeExpansion(FootstepPlannerParameters parameters, QuadrupedXGaitSettingsReadOnly xGaitSettings)
   {
      this.parameters = parameters;
      this.xGaitSettings = xGaitSettings;
   }

   public void setGoalNode(FootstepNode goalNode)
   {
      this.goalNode = goalNode;
   }

   @Override
   public HashSet<FootstepNode> expandNode(FootstepNode node)
   {
      HashSet<FootstepNode> expansion = new HashSet<>();
      addDefaultFootsteps(node, expansion);

      return expansion;
   }

   private void addGoalNodeIfReachable(FootstepNode node, HashSet<FootstepNode> expansion)
   {
      if (node.euclideanDistance(goalNode) < parameters.getMaximumStepCycleDistance())
      {
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            if (node.quadrantEuclideanDistance(robotQuadrant, goalNode) >= parameters.getMaximumStepReach())
               return;
         }

         expansion.add(goalNode);
      }

   }

   private void addDefaultFootsteps(FootstepNode node, HashSet<FootstepNode> neighboringNodesToPack)
   {
      Orientation3DReadOnly nodeOrientation = getNodeOrientation(node);
      Point2DReadOnly nominalMovingNodePosition = getNominalMovingNodePosition(node, nodeOrientation);

      Vector2D clearanceVector = new Vector2D(parameters.getMinXClearanceFromFoot(), parameters.getMinYClearanceFromFoot());
      nodeOrientation.transform(clearanceVector);


      for (double movingX = parameters.getMinimumStepLength(); movingX < parameters.getMaximumStepReach(); movingX += FootstepNode.gridSizeXY)
      {
         for (double movingY = parameters.getMinimumStepWidth(); movingY < parameters.getMaximumStepWidth(); movingY += FootstepNode.gridSizeXY)
         {
            Vector2D movingVector = new Vector2D(movingX, movingY);
            nodeOrientation.transform(movingVector);

            Point2D newNodePosition = new Point2D(nominalMovingNodePosition);
            newNodePosition.add(movingVector);

            if (!checkNodeIsFarEnoughFromOtherFeet(newNodePosition, clearanceVector, node))
               continue;

            FootstepNode offsetNode = constructNodeInPreviousNodeFrame(newNodePosition, node);
            neighboringNodesToPack.add(offsetNode);
         }
      }
   }

   private static Orientation3DReadOnly getNodeOrientation(FootstepNode node)
   {
      double nodeYaw = node.getNominalYaw();
      return new AxisAngle(nodeYaw, 0.0, 0.0);
   }

   private Point2DReadOnly getNominalMovingNodePosition(FootstepNode previousNode, Orientation3DReadOnly previousOrientation)
   {
      RobotQuadrant nextQuadrant = previousNode.getMovingQuadrant().getNextRegularGaitSwingQuadrant();
      Point2DReadOnly midstancePoint = previousNode.getOrComputeMidStancePoint();

      Vector2D offsetVector = new Vector2D(nextQuadrant.getEnd().negateIfHindEnd(xGaitSettings.getStanceLength()), nextQuadrant.getSide().negateIfRightSide(xGaitSettings.getStanceWidth()));
      offsetVector.scale(0.5);

      previousOrientation.transform(offsetVector);

      Point2D newPoint = new Point2D(midstancePoint);
      newPoint.add(offsetVector);

      return newPoint;
   }

   private static boolean checkNodeIsFarEnoughFromOtherFeet(Point2DReadOnly nodePositionToCheck, Vector2DReadOnly requiredClearance, FootstepNode previousNode)
   {
      RobotQuadrant nextQuadrant = previousNode.getMovingQuadrant().getNextRegularGaitSwingQuadrant();

      for (RobotQuadrant otherQuadrant : RobotQuadrant.values)
      {
         if (nextQuadrant == otherQuadrant)
            continue;

         double otherNodeX = previousNode.getX(otherQuadrant);
         double otherNodeY = previousNode.getY(otherQuadrant);

         if (!checkNodeIsFarEnoughFromOtherNode(nodePositionToCheck, requiredClearance, otherNodeX, otherNodeY))
            return false;
      }

      return true;
   }

   private static boolean checkNodeIsFarEnoughFromOtherNode(Point2DReadOnly nodePositionToCheck, Vector2DReadOnly requiredClearance, double otherNodeX, double otherNodeY)
   {
      double maxForward = otherNodeX + requiredClearance.getX();
      double maxBackward = otherNodeX - requiredClearance.getX();

      double maxLeft = otherNodeY + requiredClearance.getY();
      double maxRight = otherNodeY - requiredClearance.getY();

      if (maxForward >= nodePositionToCheck.getX() && maxBackward <= nodePositionToCheck.getX() && nodePositionToCheck.getY() >= maxRight && nodePositionToCheck.getY() <= maxLeft)
         return false;
      else
         return true;
   }

   private static FootstepNode constructNodeInPreviousNodeFrame(Point2DReadOnly newNodePosition, FootstepNode previousNode)
   {
      RobotQuadrant nextQuadrant = previousNode.getMovingQuadrant().getNextRegularGaitSwingQuadrant();
      Point2D frontLeft = new Point2D(previousNode.getX(FRONT_LEFT), previousNode.getY(FRONT_LEFT));
      Point2D frontRight = new Point2D(previousNode.getX(FRONT_RIGHT), previousNode.getY(FRONT_RIGHT));
      Point2D hindLeft = new Point2D(previousNode.getX(HIND_LEFT), previousNode.getY(HIND_LEFT));
      Point2D hindRight = new Point2D(previousNode.getX(HIND_RIGHT), previousNode.getY(HIND_RIGHT));

      switch (nextQuadrant)
      {
      case FRONT_LEFT:
         frontLeft.set(newNodePosition);
         break;
      case FRONT_RIGHT:
         frontRight.set(newNodePosition);
         break;
      case HIND_LEFT:
         hindLeft.set(newNodePosition);
         break;
      default:
         hindRight.set(newNodePosition);
         break;
      }

      return new FootstepNode(nextQuadrant, frontLeft, frontRight, hindLeft, hindRight);
   }
}
