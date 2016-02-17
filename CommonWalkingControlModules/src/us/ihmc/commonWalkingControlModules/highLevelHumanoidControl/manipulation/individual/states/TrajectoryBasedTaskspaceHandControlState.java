package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlMode;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.TaskspaceToJointspaceCalculator;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.trajectories.PoseTrajectoryGenerator;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public abstract class TrajectoryBasedTaskspaceHandControlState extends HandControlState
{
   public TrajectoryBasedTaskspaceHandControlState(HandControlMode stateEnum)
   {
      super(stateEnum);
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

   public abstract void setControlModuleForForceControl(RigidBodySpatialAccelerationControlModule handRigidBodySpatialAccelerationControlModule);

   public abstract void setControlModuleForPositionControl(TaskspaceToJointspaceCalculator taskspaceToJointspaceCalculator);

   public abstract void setSelectionMatrix(DenseMatrix64F selectionMatrix);
}
