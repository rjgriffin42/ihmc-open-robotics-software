package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.MomentumModuleSolution;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelJointControlMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolder;
import us.ihmc.commonWalkingControlModules.momentumBasedController.PlaneContactWrenchProcessor;
import us.ihmc.commonWalkingControlModules.visualizer.WrenchVisualizer;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.screwTheory.InverseDynamicsCalculator;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class WholeBodyInverseDynamicsSolver
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final InverseDynamicsCalculator inverseDynamicsCalculator;
   private final InverseDynamicsOptimizationControlModule optimizationControlModule;

   private final LowLevelOneDoFJointDesiredDataHolder lowLevelOneDoFJointDesiredDataHolder = new LowLevelOneDoFJointDesiredDataHolder();
   private final Map<OneDoFJoint, DoubleYoVariable> jointAccelerationsSolution = new HashMap<>();

   private final PlaneContactWrenchProcessor planeContactWrenchProcessor;
   private final WrenchVisualizer wrenchVisualizer;

   private final OneDoFJoint[] controlledOneDoFJoints;
   private final InverseDynamicsJoint[] jointsToOptimizeFor;

   public WholeBodyInverseDynamicsSolver(WholeBodyControlCoreToolbox toolbox, MomentumOptimizationSettings momentumOptimizationSettings,
         YoVariableRegistry parentRegistry)
   {
      TwistCalculator twistCalculator = toolbox.getTwistCalculator();
      double gravityZ = toolbox.getGravityZ();
      List<? extends ContactablePlaneBody> contactablePlaneBodies = toolbox.getContactablePlaneBodies();
      YoGraphicsListRegistry yoGraphicsListRegistry = toolbox.getYoGraphicsListRegistry();

      inverseDynamicsCalculator = new InverseDynamicsCalculator(twistCalculator, gravityZ);
      optimizationControlModule = new InverseDynamicsOptimizationControlModule(toolbox, momentumOptimizationSettings, registry);
      optimizationControlModule.setMinRho(momentumOptimizationSettings.getRhoMinScalar());

      jointsToOptimizeFor = momentumOptimizationSettings.getJointsToOptimizeFor();
      controlledOneDoFJoints = ScrewTools.filterJoints(jointsToOptimizeFor, OneDoFJoint.class);
      lowLevelOneDoFJointDesiredDataHolder.registerJointsWithEmptyData(controlledOneDoFJoints);
      lowLevelOneDoFJointDesiredDataHolder.setJointsControlMode(controlledOneDoFJoints, LowLevelJointControlMode.FORCE_CONTROL);

      for (int i = 0; i < controlledOneDoFJoints.length; i++)
      {
         OneDoFJoint joint = controlledOneDoFJoints[i];
         DoubleYoVariable jointAccelerationSolution = new DoubleYoVariable("qdd_qp_" + joint.getName(), registry);
         jointAccelerationsSolution.put(joint, jointAccelerationSolution);
      }

      planeContactWrenchProcessor = new PlaneContactWrenchProcessor(contactablePlaneBodies, yoGraphicsListRegistry, registry);

      wrenchVisualizer = WrenchVisualizer.createWrenchVisualizerWithContactableBodies("DesiredExternalWrench", contactablePlaneBodies, 1.0,
            yoGraphicsListRegistry, registry);

      parentRegistry.addChild(registry);
   }

   public void reset()
   {
      inverseDynamicsCalculator.reset();
      optimizationControlModule.initialize();
   }

   public void initialize()
   {
      // When you initialize into this controller, reset the estimator positions to current. Otherwise it might be in a bad state
      // where the feet are all jacked up. For example, after falling and getting back up.
      inverseDynamicsCalculator.compute();
      optimizationControlModule.initialize();
      planeContactWrenchProcessor.initialize();
   }

   public void compute()
   {
      MomentumModuleSolution momentumModuleSolution;

      try
      {
         momentumModuleSolution = optimizationControlModule.compute();
      }
      catch (MomentumControlModuleException momentumControlModuleException)
      {
         // Don't crash and burn. Instead do the best you can with what you have.
         // Or maybe just use the previous ticks solution.
         momentumModuleSolution = momentumControlModuleException.getMomentumModuleSolution();
      }

      DenseMatrix64F jointAccelerations = momentumModuleSolution.getJointAccelerations();
      Map<RigidBody, Wrench> externalWrenchSolution = momentumModuleSolution.getExternalWrenchSolution();
      List<RigidBody> rigidBodiesWithExternalWrench = momentumModuleSolution.getRigidBodiesWithExternalWrench();

      for (int i = 0; i < rigidBodiesWithExternalWrench.size(); i++)
      {
         RigidBody rigidBody = rigidBodiesWithExternalWrench.get(i);
         inverseDynamicsCalculator.setExternalWrench(rigidBody, externalWrenchSolution.get(rigidBody));
      }

      ScrewTools.setDesiredAccelerations(jointsToOptimizeFor, jointAccelerations);

      inverseDynamicsCalculator.compute();
      lowLevelOneDoFJointDesiredDataHolder.setDesiredTorqueFromJoints(controlledOneDoFJoints);
      lowLevelOneDoFJointDesiredDataHolder.setDesiredAccelerationFromJoints(controlledOneDoFJoints);

      for (int i = 0; i < controlledOneDoFJoints.length; i++)
      {
         OneDoFJoint joint = controlledOneDoFJoints[i];
         jointAccelerationsSolution.get(joint).set(joint.getQddDesired());
      }

      planeContactWrenchProcessor.compute(externalWrenchSolution);
      wrenchVisualizer.visualize(externalWrenchSolution);
   }

   public void submitInverseDynamicsCommand(InverseDynamicsCommand<?> inverseDynamicsCommand)
   {
      optimizationControlModule.submitInverseDynamicsCommand(inverseDynamicsCommand);
   }

   public LowLevelOneDoFJointDesiredDataHolder getOutput()
   {
      return lowLevelOneDoFJointDesiredDataHolder;
   }

   public void getDesiredCenterOfPressure(ContactablePlaneBody contactablePlaneBody, FramePoint2d desiredCoPToPack)
   {
      planeContactWrenchProcessor.getDesiredCenterOfPressure(contactablePlaneBody, desiredCoPToPack);
   }

   public CenterOfPressureDataHolder getDesiredCenterOfPressureDataHolder()
   {
      return planeContactWrenchProcessor.getDesiredCenterOfPressureDataHolder();
   }

   public InverseDynamicsJoint[] getJointsToOptimizeFors()
   {
      return jointsToOptimizeFor;
   }
}
