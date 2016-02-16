package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlMode;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.trajectories.PoseTrajectoryGenerator;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public abstract class TrajectoryBasedTaskspaceHandControlState extends TaskspaceHandControlState
{
   public TrajectoryBasedTaskspaceHandControlState(String namePrefix, HandControlMode stateEnum, MomentumBasedController momentumBasedController,
         int jacobianId, RigidBody base, RigidBody endEffector, YoVariableRegistry parentRegistry)
   {
      super(namePrefix, stateEnum, momentumBasedController, jacobianId, base, endEffector, parentRegistry);
   }

   public abstract void setTrajectory(PoseTrajectoryGenerator poseTrajectoryGenerator);

   public void setTrajectoryWithAngularControlQuality(PoseTrajectoryGenerator poseTrajectoryGenerator, double percentOfTrajectoryWithOrientationBeingControlled,
         double trajectoryTime)
   {
      setTrajectory(poseTrajectoryGenerator);
   }

   public abstract void setHoldPositionDuration(double holdPositionDuration);

   public abstract FramePose getDesiredPose();

   public abstract ReferenceFrame getReferenceFrame();
}
