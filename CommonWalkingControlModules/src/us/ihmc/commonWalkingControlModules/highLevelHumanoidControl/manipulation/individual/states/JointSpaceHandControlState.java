package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states;

import java.util.Map;

import us.ihmc.commonWalkingControlModules.controllerCore.command.SolverWeightLevels;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.JointspaceFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.JointspaceAccelerationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelJointControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolderInterface;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.HandControlMode;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.robotics.controllers.YoPIDGains;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.math.trajectories.DoubleTrajectoryGenerator;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class JointSpaceHandControlState extends HandControlState
{
   private final OneDoFJoint[] oneDoFJoints;
   private Map<OneDoFJoint, ? extends DoubleTrajectoryGenerator> trajectories;
   private final JointspaceFeedbackControlCommand jointspaceFeedbackControlCommand;
   private final JointspaceAccelerationCommand jointspaceAccelerationCommand;
   private final LowLevelOneDoFJointDesiredDataHolder lowLevelJointDesiredData;

   private final BooleanYoVariable setDesiredJointAccelerations;

   private final YoVariableRegistry registry;

   private final boolean doPositionControl;

   private final boolean[] doIntegrateDesiredAccelerations;

   public JointSpaceHandControlState(String namePrefix, OneDoFJoint[] controlledJoints, boolean doPositionControl,
         MomentumBasedController momentumBasedController, YoPIDGains gains, double dt, YoVariableRegistry parentRegistry)
   {
      super(HandControlMode.JOINT_SPACE);

      this.doPositionControl = doPositionControl;

      String name = namePrefix + getClass().getSimpleName();
      registry = new YoVariableRegistry(name);

      oneDoFJoints = controlledJoints;

      if (!doPositionControl)
      {
         lowLevelJointDesiredData = null;
         setDesiredJointAccelerations = null;
         jointspaceFeedbackControlCommand = new JointspaceFeedbackControlCommand();
         jointspaceAccelerationCommand = null;

         jointspaceFeedbackControlCommand.setWeightForSolver(SolverWeightLevels.ARM_JOINTSPACE_WEIGHT);
         jointspaceFeedbackControlCommand.setGains(gains);

         for (int i = 0; i < oneDoFJoints.length; i++)
         {
            OneDoFJoint joint = oneDoFJoints[i];
            jointspaceFeedbackControlCommand.addJoint(joint, Double.NaN, Double.NaN, Double.NaN);
         }
      }
      else
      {
         lowLevelJointDesiredData = new LowLevelOneDoFJointDesiredDataHolder(oneDoFJoints.length);
         lowLevelJointDesiredData.registerJointsWithEmptyData(oneDoFJoints);
         lowLevelJointDesiredData.setJointsControlMode(oneDoFJoints, LowLevelJointControlMode.POSITION_CONTROL);
         jointspaceFeedbackControlCommand = null;
         jointspaceAccelerationCommand = new JointspaceAccelerationCommand();
         for (int i = 0; i < oneDoFJoints.length; i++)
         {
            OneDoFJoint joint = oneDoFJoints[i];
            jointspaceAccelerationCommand.addJoint(joint, Double.NaN);
         }

         setDesiredJointAccelerations = new BooleanYoVariable(namePrefix + "SetDesiredJointAccelerations", registry);
         setDesiredJointAccelerations.set(false);
      }

      doIntegrateDesiredAccelerations = new boolean[oneDoFJoints.length];

      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint joint = oneDoFJoints[i];
         doIntegrateDesiredAccelerations[i] = joint.getIntegrateDesiredAccelerations();
      }

      parentRegistry.addChild(registry);
   }

   public void setWeight(double weight)
   {
      if (jointspaceFeedbackControlCommand != null)
         jointspaceFeedbackControlCommand.setWeightForSolver(weight);
   }

   @Override
   public void doAction()
   {
      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint joint = oneDoFJoints[i];

         DoubleTrajectoryGenerator trajectoryGenerator = trajectories.get(joint);
         trajectoryGenerator.compute(getTimeInCurrentState());

         double desiredPosition = trajectoryGenerator.getValue();
         joint.setqDesired(desiredPosition);
         double desiredVelocity = trajectoryGenerator.getVelocity();
         joint.setQdDesired(desiredVelocity);
         double feedForwardAcceleration = trajectoryGenerator.getAcceleration();

         if (doPositionControl)
         {
            enablePositionControl();
            if (!setDesiredJointAccelerations.getBooleanValue())
            {
               feedForwardAcceleration = 0.0;
            }
            jointspaceAccelerationCommand.setOneDoFJointDesiredAcceleration(i, feedForwardAcceleration);
         }
         else
         {
            jointspaceFeedbackControlCommand.setOneDoFJoint(i, desiredPosition, desiredVelocity, feedForwardAcceleration);
         }
      }
   }

   @Override
   public void doTransitionIntoAction()
   {
      saveDoAccelerationIntegration();

      if (doPositionControl)
         enablePositionControl();
   }

   @Override
   public void doTransitionOutOfAction()
   {
      disablePositionControl();
   }

   private void saveDoAccelerationIntegration()
   {
      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint joint = oneDoFJoints[i];
         doIntegrateDesiredAccelerations[i] = joint.getIntegrateDesiredAccelerations();
      }
   }

   private void enablePositionControl()
   {
      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint joint = oneDoFJoints[i];
         joint.setIntegrateDesiredAccelerations(false);
         joint.setUnderPositionControl(true);
      }
   }

   private void disablePositionControl()
   {
      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint joint = oneDoFJoints[i];
         joint.setIntegrateDesiredAccelerations(doIntegrateDesiredAccelerations[i]);
         joint.setUnderPositionControl(false);
      }
   }

   @Override
   public boolean isDone()
   {
      for (OneDoFJoint oneDoFJoint : oneDoFJoints)
      {
         if (!trajectories.get(oneDoFJoint).isDone())
            return false;
      }

      return true;
   }

   public void setTrajectories(Map<OneDoFJoint, ? extends DoubleTrajectoryGenerator> trajectories)
   {
      this.trajectories = trajectories;
   }

   @Override
   public JointspaceAccelerationCommand getInverseDynamicsCommand()
   {
      return jointspaceAccelerationCommand;
   }

   @Override
   public JointspaceFeedbackControlCommand getFeedbackControlCommand()
   {
      return jointspaceFeedbackControlCommand;
   }

   @Override
   public LowLevelOneDoFJointDesiredDataHolderInterface getLowLevelJointDesiredData()
   {
      return lowLevelJointDesiredData;
   }
}
