package us.ihmc.footstepPlanning.aStar.implementations;

import java.util.HashSet;

import us.ihmc.footstepPlanning.aStar.FootstepNode;
import us.ihmc.footstepPlanning.aStar.FootstepNodeExpansion;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.referenceFrames.PoseReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;

public class SimpleSideBasedExpansion implements FootstepNodeExpansion
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private double[] stepLengths = new double[] {0.0, FootstepNode.gridSizeX, 0.1, 0.2, 0.4};
   private double[] stepWidths = new double[] {0.15, 0.3};
   private double[] stepYaws = new double[] {0.0, FootstepNode.gridSizeYaw, Math.PI / 9.0};

   private double defaultStepWidth = 0.25;

   @Override
   public HashSet<FootstepNode> expandNode(FootstepNode node)
   {
      HashSet<FootstepNode> neighbors = new HashSet<>();

      FramePose stanceFootPose = new FramePose(worldFrame);
      stanceFootPose.setYawPitchRoll(node.getYaw(), 0.0, 0.0);
      stanceFootPose.setX(node.getX());
      stanceFootPose.setY(node.getY());
      ReferenceFrame stanceFrame = new PoseReferenceFrame("stanceFrame", stanceFootPose);

      RobotSide stepSide = node.getRobotSide().getOppositeSide();
      double ySign = stepSide.negateIfRightSide(1.0);

      for (int i = 0; i < stepLengths.length; i++)
      {
         double stepLength = stepLengths[i];
         for (int j = 0; j < stepYaws.length; j++)
         {
            double yaw = stepYaws[j];

            FramePose forwardStep = new FramePose(stanceFrame);
            forwardStep.setX(stepLength);
            forwardStep.setY(ySign * defaultStepWidth);
            forwardStep.changeFrame(worldFrame);
            neighbors.add(new FootstepNode(forwardStep.getX(), forwardStep.getY(), node.getYaw() + ySign * yaw, stepSide));

            FramePose backwardStep = new FramePose(stanceFrame);
            backwardStep.setX(-stepLength);
            backwardStep.setY(ySign * defaultStepWidth);
            backwardStep.changeFrame(worldFrame);
            neighbors.add(new FootstepNode(backwardStep.getX(), backwardStep.getY(), node.getYaw() + ySign * yaw, stepSide));
         }
      }

      for (int i = 0; i < stepWidths.length; i++)
      {
         double stepWidth = stepWidths[i];
         FramePose sideStep = new FramePose(stanceFrame);
         sideStep.setY(ySign * stepWidth);
         sideStep.changeFrame(worldFrame);
         neighbors.add(new FootstepNode(sideStep.getX(), sideStep.getY(), node.getYaw(), stepSide));

      }

      return neighbors;
   }
}
