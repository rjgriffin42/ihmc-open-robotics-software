package us.ihmc.commonWalkingControlModules.inverseKinematics;

import java.util.HashMap;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.InverseKinematicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.InverseKinematicsSolution;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelJointControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.RootJointDesiredConfigurationData;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.RootJointDesiredConfigurationDataReadOnly;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.MomentumOptimizationSettings;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.SixDoFJoint;

public class WholeBodyInverseKinematicsSolver
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final InverseKinematicsOptimizationControlModule optimizationControlModule;
   private final RobotJointVelocityAccelerationIntegrator integrator;

   private final RootJointDesiredConfigurationData rootJointDesiredConfiguration = new RootJointDesiredConfigurationData();
   private final LowLevelOneDoFJointDesiredDataHolder lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();
   private final Map<OneDoFJoint, DoubleYoVariable> jointVelocitiesSolution = new HashMap<>();

   private final SixDoFJoint rootJoint;
   private final OneDoFJoint[] controlledOneDoFJoints;
   private final InverseDynamicsJoint[] jointsToOptimizeFor;

   public WholeBodyInverseKinematicsSolver(WholeBodyControlCoreToolbox toolbox, MomentumOptimizationSettings momentumOptimizationSettings,
         YoVariableRegistry parentRegistry)
   {
      rootJoint = toolbox.getRobotRootJoint();
      jointsToOptimizeFor = momentumOptimizationSettings.getJointsToOptimizeFor();
      controlledOneDoFJoints = ScrewTools.filterJoints(jointsToOptimizeFor, OneDoFJoint.class);
      lowLevelOneDoFJointDesiredDataHolder.registerJointsWithEmptyData(controlledOneDoFJoints);
      lowLevelOneDoFJointDesiredDataHolder.setJointsControlMode(controlledOneDoFJoints, LowLevelJointControlMode.FORCE_CONTROL);

      optimizationControlModule = new InverseKinematicsOptimizationControlModule(toolbox, momentumOptimizationSettings, registry);
      integrator = new RobotJointVelocityAccelerationIntegrator(toolbox.getControlDT());

      for (int i = 0; i < controlledOneDoFJoints.length; i++)
      {
         OneDoFJoint joint = controlledOneDoFJoints[i];
         DoubleYoVariable jointVelocitySolution = new DoubleYoVariable("qd_qp_" + joint.getName(), registry);
         jointVelocitiesSolution.put(joint, jointVelocitySolution);
      }

      parentRegistry.addChild(registry);
   }

   public void reset()
   {
      optimizationControlModule.initialize();
   }

   public void compute()
   {
      InverseKinematicsSolution inverseKinematicsSolution;

      try
      {
         inverseKinematicsSolution = optimizationControlModule.compute();
      }
      catch (InverseKinematicsOptimizationException inverseKinematicsOptimizationException)
      {
         // Don't crash and burn. Instead do the best you can with what you have.
         // Or maybe just use the previous ticks solution.
         inverseKinematicsSolution = inverseKinematicsOptimizationException.getSolution();
      }

      DenseMatrix64F jointVelocities = inverseKinematicsSolution.getJointVelocities();

      integrator.integrateJointVelocities(jointsToOptimizeFor, jointVelocities);

      DenseMatrix64F jointConfigurations = integrator.getJointConfigurations();
      jointVelocities = integrator.getJointVelocities();

      int[] rootJointIndices = optimizationControlModule.getJointIndices(rootJoint);
      rootJointDesiredConfiguration.setDesiredConfiguration(jointConfigurations, rootJointIndices[0]);
      rootJointDesiredConfiguration.setDesiredVelocity(jointVelocities, rootJointIndices[0]);

      for (int i = 0; i < controlledOneDoFJoints.length; i++)
      {
         OneDoFJoint joint = controlledOneDoFJoints[i];
         int jointIndex = optimizationControlModule.getOneDoFJointIndex(joint);
         double desiredVelocity = jointVelocities.get(jointIndex, 0);
         lowLevelOneDoFJointDesiredDataHolder.setDesiredJointVelocity(joint, desiredVelocity);

         if (jointIndex > rootJointIndices[rootJointIndices.length - 1])
            jointIndex++; // Because of quaternion :/
         double desiredPosition = jointConfigurations.get(jointIndex, 0);
         lowLevelOneDoFJointDesiredDataHolder.setDesiredJointPosition(joint, desiredPosition);
      }
   }

   public void submitInverseKinematicsCommand(InverseKinematicsCommand<?> inverseKinematicsCommand)
   {
      optimizationControlModule.submitInverseKinematicsCommand(inverseKinematicsCommand);
   }

   public LowLevelOneDoFJointDesiredDataHolder getOutput()
   {
      return lowLevelOneDoFJointDesiredDataHolder;
   }

   public RootJointDesiredConfigurationDataReadOnly getOutputForRootJoint()
   {
      return rootJointDesiredConfiguration;
   }
}
