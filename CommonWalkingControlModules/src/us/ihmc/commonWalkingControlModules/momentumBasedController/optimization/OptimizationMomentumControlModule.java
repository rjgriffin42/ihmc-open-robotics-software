package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization.MomentumOptimizerNative;
import us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization.MomentumOptimizerNativeInput;
import us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization.MomentumOptimizerNativeOutput;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumControlModule;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.TaskspaceConstraintData;
import us.ihmc.commonWalkingControlModules.wrenchDistribution.ContactPointWrenchMatrixCalculator;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.exeptions.NoConvergenceException;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author twan
 *         Date: 4/25/13
 */
public class OptimizationMomentumControlModule implements MomentumControlModule
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final CentroidalMomentumHandler centroidalMomentumHandler;
   private final ExternalWrenchHandler externalWrenchHandler;
   private final HardMotionConstraintEnforcer hardMotionConstraintEnforcer = new HardMotionConstraintEnforcer();
   private final MotionConstraintHandler primaryMotionConstraintHandler;
   private final MotionConstraintHandler secondaryMotionConstraintHandler;
   private final ContactPointWrenchMatrixCalculator contactPointWrenchMatrixCalculator;
   private final MomentumOptimizerNativeInput momentumOptimizerNativeInput = new MomentumOptimizerNativeInput();
   private final MomentumOptimizerNative momentumOptimizerNative;
   private final MomentumOptimizationSettings momentumOptimizationSettings;

   private final BooleanYoVariable converged = new BooleanYoVariable("converged", registry);
   private final BooleanYoVariable hasNotConvergedInPast = new BooleanYoVariable("hasNotConvergedInPast", registry);
   private final InverseDynamicsJoint[] jointsToOptimizeFor;
   private final MomentumRateOfChangeData momentumRateOfChangeData;

   public OptimizationMomentumControlModule(InverseDynamicsJoint rootJoint, ReferenceFrame centerOfMassFrame, double controlDT,
           YoVariableRegistry parentRegistry, InverseDynamicsJoint[] jointsToOptimizeFor, MomentumOptimizationSettings momentumOptimizationSettings,
           double gravityZ)
   {
      this.centroidalMomentumHandler = new CentroidalMomentumHandler(rootJoint, centerOfMassFrame, controlDT, registry);
      this.externalWrenchHandler = new ExternalWrenchHandler(gravityZ, centerOfMassFrame, rootJoint);
      this.primaryMotionConstraintHandler = new MotionConstraintHandler(jointsToOptimizeFor);
      this.secondaryMotionConstraintHandler = new MotionConstraintHandler(jointsToOptimizeFor);
      this.contactPointWrenchMatrixCalculator = new ContactPointWrenchMatrixCalculator(centerOfMassFrame, MomentumOptimizerNative.nSupportVectors,
              MomentumOptimizerNative.rhoSize);

      this.momentumOptimizerNative = new MomentumOptimizerNative(ScrewTools.computeDegreesOfFreedom(jointsToOptimizeFor), MomentumOptimizerNative.rhoSize);    // TODO: reduce rhoSize?
      this.momentumOptimizationSettings = momentumOptimizationSettings;

      this.jointsToOptimizeFor = jointsToOptimizeFor;

      this.momentumRateOfChangeData = new MomentumRateOfChangeData(centerOfMassFrame);

      parentRegistry.addChild(registry);
      reset();
   }

   public void initialize()
   {
      centroidalMomentumHandler.initialize();
   }

   public void reset()
   {
      primaryMotionConstraintHandler.reset();
      secondaryMotionConstraintHandler.reset();
      externalWrenchHandler.reset();
   }

   public void compute(Map<ContactablePlaneBody, ? extends PlaneContactState> contactStates, RobotSide upcomingSupportLeg)
   {
      LinkedHashMap<RigidBody, PlaneContactState> planeContactStates = convertContactStates(contactStates);

      centroidalMomentumHandler.compute();
      primaryMotionConstraintHandler.compute();

      hardMotionConstraintEnforcer.setDampedLeastSquaresAlpha(momentumOptimizationSettings.getDampedLeastSquaresFactor());

      hardMotionConstraintEnforcer.compute(centroidalMomentumHandler.getCentroidalMomentumMatrixPart(jointsToOptimizeFor),
              primaryMotionConstraintHandler.getJacobian(), primaryMotionConstraintHandler.getRightHandSide());

      DenseMatrix64F momentumDotEquationRightHandSide = centroidalMomentumHandler.getMomentumDotEquationRightHandSide(momentumRateOfChangeData);
      hardMotionConstraintEnforcer.constrainMomentumEquationRightHandSide(momentumDotEquationRightHandSide);

      momentumOptimizerNativeInput.setCentroidalMomentumMatrix(hardMotionConstraintEnforcer.getConstrainedCentroidalMomentumMatrix());
      momentumOptimizerNativeInput.setMomentumDotEquationRightHandSide(momentumDotEquationRightHandSide);

      momentumOptimizerNativeInput.setRhoMin(contactPointWrenchMatrixCalculator.getRhoMin(planeContactStates.values(),
              momentumOptimizationSettings.getRhoMinScalar()));

      contactPointWrenchMatrixCalculator.computeMatrix(contactStates.values());
      momentumOptimizerNativeInput.setContactPointWrenchMatrix(contactPointWrenchMatrixCalculator.getMatrix());
      DenseMatrix64F wrenchEquationRightHandSide =
         externalWrenchHandler.computeWrenchEquationRightHandSide(centroidalMomentumHandler.getCentroidalMomentumConvectiveTerm());
      hardMotionConstraintEnforcer.constrainWrenchEquationRightHandSide(wrenchEquationRightHandSide);
      momentumOptimizerNativeInput.setWrenchEquationRightHandSide(wrenchEquationRightHandSide);

      momentumOptimizerNativeInput.setMomentumDotWeight(momentumOptimizationSettings.getMomentumDotWeight(momentumRateOfChangeData.getMomentumSubspace()));
      momentumOptimizerNativeInput.setJointAccelerationRegularization(
          momentumOptimizationSettings.getDampedLeastSquaresFactorMatrix(ScrewTools.computeDegreesOfFreedom(jointsToOptimizeFor)));

      secondaryMotionConstraintHandler.compute();
      momentumOptimizerNativeInput.setSecondaryConstraintJacobian(secondaryMotionConstraintHandler.getJacobian());
      momentumOptimizerNativeInput.setSecondaryConstraintRightHandSide(secondaryMotionConstraintHandler.getRightHandSide());
      momentumOptimizerNativeInput.setNullspaceMatrix(secondaryMotionConstraintHandler.getNullspaceMatrix());
      momentumOptimizerNativeInput.setNullspaceMultipliers(secondaryMotionConstraintHandler.getNullspaceMultipliers());
      momentumOptimizerNativeInput.setSecondaryConstraintWeight(secondaryMotionConstraintHandler.getWeightMatrix());

      momentumOptimizerNativeInput.setGroundReactionForceRegularization(momentumOptimizationSettings.getGroundReactionForceRegularization());

      optimize(momentumOptimizerNativeInput);

      MomentumOptimizerNativeOutput output = momentumOptimizerNative.getOutput();

      Map<RigidBody, Wrench> groundReactionWrenches = contactPointWrenchMatrixCalculator.computeWrenches(planeContactStates, output.getRho());

      externalWrenchHandler.computeExternalWrenches(groundReactionWrenches);

      DenseMatrix64F jointAccelerations = hardMotionConstraintEnforcer.computeConstrainedJointAccelerations(output.getJointAccelerations());
      ScrewTools.setDesiredAccelerations(jointsToOptimizeFor, jointAccelerations);

//    assert(arePrimaryConstraintsSatisfied(jointAccelerations));

//    if (!arePrimaryConstraintsSatisfied(jointAccelerations))
//       throw new RuntimeException("primary constraints not satisfied");

      centroidalMomentumHandler.computeCentroidalMomentumRate(jointsToOptimizeFor, jointAccelerations);
   }

   private static LinkedHashMap<RigidBody, PlaneContactState> convertContactStates(Map<ContactablePlaneBody, ? extends PlaneContactState> contactStates)
   {
      LinkedHashMap<RigidBody, PlaneContactState> ret = new LinkedHashMap<RigidBody, PlaneContactState>();
      for (ContactablePlaneBody contactablePlaneBody : contactStates.keySet())
      {
         ret.put(contactablePlaneBody.getRigidBody(), contactStates.get(contactablePlaneBody));
      }

      return ret;
   }

// private boolean arePrimaryConstraintsSatisfied(DenseMatrix64F jointAccelerations)
// {
//    DenseMatrix64F jacobian = primaryMotionConstraintHandler.getJacobian();
//    DenseMatrix64F residual = new DenseMatrix64F(jacobian.getNumRows(), 1);
//    CommonOps.mult(jacobian, jointAccelerations, residual);
//    CommonOps.subEquals(residual, primaryMotionConstraintHandler.getRightHandSide());
//
//    return MatrixFeatures.isConstantVal(residual, 0.0, 1e-9);
// }

   private void optimize(MomentumOptimizerNativeInput momentumOptimizerNativeInput)
   {
      try
      {
         momentumOptimizerNative.solve(momentumOptimizerNativeInput);
         converged.set(true);
      }
      catch (NoConvergenceException e)
      {
         if (!hasNotConvergedInPast.getBooleanValue())
         {
            e.printStackTrace();
            System.err.println("WARNING: Only showing the stack trace of the first " + e.getClass().getSimpleName()
                               + ". This may be happening more than once. See value of YoVariable " + converged.getName() + ".");
         }

         converged.set(false);
         hasNotConvergedInPast.set(true);
      }
   }

   public void resetGroundReactionWrenchFilter()
   {
      // empty for now
   }

   public void setDesiredJointAcceleration(InverseDynamicsJoint joint, DenseMatrix64F jointAcceleration)
   {
      primaryMotionConstraintHandler.setDesiredJointAcceleration(joint, jointAcceleration, Double.POSITIVE_INFINITY);    // weight is arbitrary, actually
   }

   public void setDesiredSpatialAcceleration(GeometricJacobian jacobian, TaskspaceConstraintData taskspaceConstraintData)
   {
      primaryMotionConstraintHandler.setDesiredSpatialAcceleration(jacobian, taskspaceConstraintData, Double.POSITIVE_INFINITY);    // weight is arbitrary, actually
   }

   public void setDesiredRateOfChangeOfMomentum(MomentumRateOfChangeData momentumRateOfChangeData)
   {
      this.momentumRateOfChangeData.set(momentumRateOfChangeData);
   }

   public void setExternalWrenchToCompensateFor(RigidBody rigidBody, Wrench wrench)
   {
      externalWrenchHandler.setExternalWrenchToCompensateFor(rigidBody, wrench);
   }

   public void setDesiredJointAcceleration(InverseDynamicsJoint joint, DenseMatrix64F jointAcceleration, double weight)
   {
      secondaryMotionConstraintHandler.setDesiredJointAcceleration(joint, jointAcceleration, weight);
   }

   public void setDesiredSpatialAcceleration(GeometricJacobian jacobian, TaskspaceConstraintData taskspaceConstraintData, double weight)
   {
      secondaryMotionConstraintHandler.setDesiredSpatialAcceleration(jacobian, taskspaceConstraintData, weight);
   }

   public SpatialForceVector getDesiredCentroidalMomentumRate()
   {
      return centroidalMomentumHandler.getCentroidalMomentumRate();
   }

   public Map<RigidBody, Wrench> getExternalWrenches()
   {
      return externalWrenchHandler.getExternalWrenches();
   }
}
