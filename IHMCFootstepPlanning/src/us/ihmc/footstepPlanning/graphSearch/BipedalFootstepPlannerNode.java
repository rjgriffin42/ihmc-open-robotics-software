package us.ihmc.footstepPlanning.graphSearch;

import java.util.ArrayList;

import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.robotSide.RobotSide;

public class BipedalFootstepPlannerNode
{
   private RobotSide footstepSide;
   private RigidBodyTransform soleTransform = new RigidBodyTransform();
   private BipedalFootstepPlannerNode parentNode;

   private ArrayList<BipedalFootstepPlannerNode> childrenNodes;
   private double costFromParent;
   private double costToHereFromStart;
   private double estimatedCostToGoal;

   public BipedalFootstepPlannerNode(RobotSide footstepSide, RigidBodyTransform soleTransform)
   {
      this.footstepSide = footstepSide;
      this.soleTransform.set(soleTransform);
   }

   public RobotSide getRobotSide()
   {
      return footstepSide;
   }

   public void getSoleTransform(RigidBodyTransform soleTransformToPack)
   {
      soleTransformToPack.set(soleTransform);
   }

   public void transformSoleTransformWithSnapTransformFromZeroZ(RigidBodyTransform snapTransform)
   {
      // Ignore the z since the snap transform snapped from z = 0. Keep everything else.
      soleTransform.setM23(0.0);
      soleTransform.multiply(snapTransform, soleTransform);
   }

   public BipedalFootstepPlannerNode getParentNode()
   {
      return parentNode;
   }

   public void setParentNode(BipedalFootstepPlannerNode parentNode)
   {
      this.parentNode = parentNode;
   }

   public void addChild(BipedalFootstepPlannerNode childNode)
   {
      if (childrenNodes == null)
      {
         childrenNodes = new ArrayList<>();
      }

      this.childrenNodes.add(childNode);
   }

   public void getChildren(ArrayList<BipedalFootstepPlannerNode> childrenNodesToPack)
   {
      childrenNodesToPack.addAll(childrenNodes);
   }

   public double getCostFromParent()
   {
      return costFromParent;
   }

   public void setCostFromParent(double costFromParent)
   {
      this.costFromParent = costFromParent;
   }

   public double getCostToHereFromStart()
   {
      return costToHereFromStart;
   }

   public void setCostToHereFromStart(double costToHereFromStart)
   {
      this.costToHereFromStart = costToHereFromStart;
   }

   public double getEstimatedCostToGoal()
   {
      return estimatedCostToGoal;
   }

   public void setEstimatedCostToGoal(double estimatedCostToGoal)
   {
      this.estimatedCostToGoal = estimatedCostToGoal;
   }


}
