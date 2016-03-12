package us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.InverseKinematicsCommandList;
import us.ihmc.commonWalkingControlModules.momentumBasedController.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.lowLevelControl.LowLevelOneDoFJointDesiredDataHolderInterface;

public interface ControllerCoreCommandInterface
{
   public abstract InverseDynamicsCommandList getInverseDynamicsCommandList();
   public abstract FeedbackControlCommandList getFeedbackControlCommandList();
   public abstract InverseKinematicsCommandList getInverseKinematicsCommandList();
   public abstract LowLevelOneDoFJointDesiredDataHolderInterface getLowLevelOneDoFJointDesiredDataHolder();
   public abstract WholeBodyControllerCoreMode getControllerCoreMode();
}