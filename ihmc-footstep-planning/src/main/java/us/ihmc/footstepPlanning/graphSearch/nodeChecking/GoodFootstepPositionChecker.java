package us.ihmc.footstepPlanning.graphSearch.nodeChecking;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.EuclidCoreTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.transform.interfaces.RigidBodyTransformReadOnly;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapData;
import us.ihmc.footstepPlanning.graphSearch.footstepSnapping.FootstepNodeSnapper;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepGraph;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNode;
import us.ihmc.footstepPlanning.graphSearch.graph.FootstepNodeTools;
import us.ihmc.footstepPlanning.graphSearch.graph.visualization.BipedalFootstepPlannerNodeRejectionReason;
import us.ihmc.footstepPlanning.graphSearch.parameters.FootstepPlannerParametersReadOnly;
import us.ihmc.robotics.geometry.PlanarRegionsList;
import us.ihmc.robotics.referenceFrames.TransformReferenceFrame;
import us.ihmc.robotics.referenceFrames.ZUpFrame;
import us.ihmc.robotics.robotSide.RobotSide;

public class GoodFootstepPositionChecker implements SnapBasedCheckerComponent
{
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final TransformReferenceFrame parentSoleFrame = new TransformReferenceFrame("parentSole", ReferenceFrame.getWorldFrame());
   private final ZUpFrame parentSoleZupFrame = new ZUpFrame(worldFrame, parentSoleFrame, "parentSoleZupFrame");
   private final TransformReferenceFrame nodeSoleFrame = new TransformReferenceFrame("nodeSole", ReferenceFrame.getWorldFrame());
   private final FramePoint3D solePositionInParentZUpFrame = new FramePoint3D(parentSoleZupFrame);

   private final FootstepPlannerParametersReadOnly parameters;
   private final FootstepNodeSnapper snapper;

   private BipedalFootstepPlannerNodeRejectionReason rejectionReason;
   private FootstepGraph graph;

   public GoodFootstepPositionChecker(FootstepPlannerParametersReadOnly parameters, FootstepNodeSnapper snapper)
   {
      this.parameters = parameters;
      this.snapper = snapper;
   }

   @Override
   public void setFootstepGraph(FootstepGraph graph)
   {
      this.graph = graph;
   }

   @Override
   public void setPlanarRegions(PlanarRegionsList planarRegions)
   {

   }

   @Override
   public boolean isNodeValid(FootstepNode nodeToCheck, FootstepNode previousNode)
   {
      if (previousNode == null)
         return true;

      FootstepNodeSnapData snapData = snapper.snapFootstepNode(nodeToCheck);
      FootstepNodeSnapData previousSnapData = snapper.snapFootstepNode(previousNode);

      RigidBodyTransformReadOnly snappedSoleTransform = snapData.getOrComputeSnappedNodeTransform(nodeToCheck);
      RigidBodyTransformReadOnly previousSnappedSoleTransform = previousSnapData.getOrComputeSnappedNodeTransform(previousNode);

      parentSoleFrame.setTransformAndUpdate(previousSnappedSoleTransform);
      parentSoleZupFrame.update();

      nodeSoleFrame.setTransformAndUpdate(snappedSoleTransform);

      solePositionInParentZUpFrame.setToZero(nodeSoleFrame);
      solePositionInParentZUpFrame.changeFrame(parentSoleZupFrame);

      RobotSide robotSide = nodeToCheck.getRobotSide();
      double sidedWidth = robotSide.negateIfRightSide(solePositionInParentZUpFrame.getY());
      if (sidedWidth <  parameters.getMinimumStepWidth())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_NOT_WIDE_ENOUGH;
         return false;
      }

      if (sidedWidth > parameters.getMaximumStepWidth())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE;
         return false;
      }

      double minimumStepLength = parameters.getMinimumStepLength();
      if (solePositionInParentZUpFrame.getX() < minimumStepLength)
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_NOT_LONG_ENOUGH;
         return false;
      }

      if (Math.abs(solePositionInParentZUpFrame.getZ()) > parameters.getMaximumStepZ())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_HIGH_OR_LOW;
         return false;
      }

      if (solePositionInParentZUpFrame.getZ() < -Math.abs(parameters.getMaximumStepZWhenForwardAndDown()))
      {
         if ((solePositionInParentZUpFrame.getX() > parameters.getMaximumStepXWhenForwardAndDown()))
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FORWARD_AND_DOWN;
            return false;
         }

         if (sidedWidth > parameters.getMaximumStepYWhenForwardAndDown())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE_AND_DOWN;
            return false;
         }
      }


      double stepReach = EuclidCoreTools.norm(solePositionInParentZUpFrame.getX(), solePositionInParentZUpFrame.getY());
      if (stepReach > parameters.getMaximumStepReach())
      {
         rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR;
         return false;
      }

      if (solePositionInParentZUpFrame.getZ() > parameters.getMaximumStepZWhenSteppingUp())
      {
         if (stepReach > parameters.getMaximumStepReachWhenSteppingUp())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_AND_HIGH;
            return false;
         }

         if (sidedWidth > parameters.getMaximumStepReachWhenSteppingUp())
         {
            rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_WIDE_AND_HIGH;
            return false;
         }
      }

      if (graph != null)
      {
         FootstepNode grandparentNode = graph.getParentNode(previousNode);
         if (grandparentNode != null)
         {
            FootstepNodeSnapData grandparentSnapData = snapper.snapFootstepNode(grandparentNode);
            if (grandparentSnapData == null)
               return true;

            Point3D grandparentPosition = new Point3D(grandparentNode.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth()));
            grandparentSnapData.getSnapTransform().transform(grandparentPosition);
            double grandparentTranslationScaleFactor = parameters.getTranslationScaleFromGrandparentNode();

            Point3D nodePosition = new Point3D(nodeToCheck.getOrComputeMidFootPoint(parameters.getIdealFootstepWidth()));
            snapData.getSnapTransform().transform(nodePosition);

            double heightChangeFromGrandparentNode = nodePosition.getZ() - grandparentPosition.getZ();
            double translationFromGrandparentNode = EuclidCoreTools.norm(nodePosition.getX() - grandparentPosition.getX(),
                                                                         nodePosition.getY() - grandparentPosition.getY());

            boolean largeStepUp = heightChangeFromGrandparentNode > parameters.getMaximumStepZWhenSteppingUp();
            boolean largeStepDown = heightChangeFromGrandparentNode < -parameters.getMaximumStepZWhenForwardAndDown();

            if (largeStepUp && translationFromGrandparentNode > grandparentTranslationScaleFactor * parameters.getMaximumStepReachWhenSteppingUp())
            {
               rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FAR_AND_HIGH;
               return false;
            }

            if (largeStepDown && translationFromGrandparentNode > grandparentTranslationScaleFactor * parameters.getMaximumStepXWhenForwardAndDown())
            {
               rejectionReason = BipedalFootstepPlannerNodeRejectionReason.STEP_TOO_FORWARD_AND_DOWN;
               return false;
            }
         }
      }

      return true;
   }

   @Override
   public BipedalFootstepPlannerNodeRejectionReason getRejectionReason()
   {
      return rejectionReason;
   }
}
