package us.ihmc.humanoidBehaviors.taskExecutor;

import java.util.ArrayList;

import us.ihmc.humanoidBehaviors.behaviors.primitives.FootstepListBehavior;
import us.ihmc.humanoidBehaviors.behaviors.simpleBehaviors.BehaviorAction;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.partNames.LimbName;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.RigidBody;

public class FootstepTask<E extends Enum<E>> extends BehaviorAction<E>
{
   private final FootstepListBehavior footstepListBehavior;
   private ArrayList<Footstep> footsteps = new ArrayList<Footstep>();

   public FootstepTask(FullHumanoidRobotModel fullRobotModel, RobotSide robotSide, FootstepListBehavior footstepListBehavior, FramePose footPose)
   {
      this(null, fullRobotModel, robotSide, footstepListBehavior, footPose);
   }

   public FootstepTask(E stateEnum, FullHumanoidRobotModel fullRobotModel, RobotSide robotSide, FootstepListBehavior footstepListBehavior, FramePose footPose)
   {
      super(stateEnum, footstepListBehavior);
      RigidBody endEffector = fullRobotModel.getEndEffector(robotSide, LimbName.LEG);
      footsteps.add(new Footstep(endEffector, robotSide, footPose));
      this.footstepListBehavior = footstepListBehavior;
   }

   @Override
   protected void setBehaviorInput()
   {
      footstepListBehavior.set(footsteps);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      footstepListBehavior.doPostBehaviorCleanup();
      footsteps.clear();
   }
}
