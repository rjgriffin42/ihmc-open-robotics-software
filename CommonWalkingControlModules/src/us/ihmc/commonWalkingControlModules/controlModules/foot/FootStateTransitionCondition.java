package us.ihmc.commonWalkingControlModules.controlModules.foot;

import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.utilities.math.NullspaceCalculator;
import us.ihmc.utilities.screwTheory.GeometricJacobian;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.stateMachines.StateTransitionCondition;

public class FootStateTransitionCondition implements StateTransitionCondition
{
   private static final int velocitySignForSingularityEscape = 1;

   private final ConstraintType stateEnum;
   private final GeometricJacobian jacobian;
   private final BooleanYoVariable doSingularityEscape;
   private final NullspaceCalculator nullspaceCalculator;
   private final DenseMatrix64F jointVelocities;
   private final BooleanYoVariable jacobianDeterminantInRange;
   private final BooleanYoVariable waitSingularityEscapeBeforeTransitionToNextState;
   private final FootControlHelper footControlHelper;

   public FootStateTransitionCondition(AbstractFootControlState stateToTransitionTo, FootControlHelper footControlHelper,
         BooleanYoVariable doSingularityEscape, BooleanYoVariable jacobianDeterminantInRange, BooleanYoVariable waitSingularityEscapeBeforeTransitionToNextState)
   {
      this.stateEnum = stateToTransitionTo.getStateEnum();
      this.footControlHelper = footControlHelper;

      this.jacobian = footControlHelper.getJacobian();
      this.doSingularityEscape = doSingularityEscape;
      this.jacobianDeterminantInRange = jacobianDeterminantInRange;
      this.waitSingularityEscapeBeforeTransitionToNextState = waitSingularityEscapeBeforeTransitionToNextState;

      nullspaceCalculator = new NullspaceCalculator(jacobian.getNumberOfColumns(), true);
      jointVelocities = new DenseMatrix64F(ScrewTools.computeDegreesOfFreedom(jacobian.getJointsInOrder()), 1);
   }

   public boolean checkCondition()
   {
      boolean transitionRequested = footControlHelper.getRequestedState() == stateEnum;

      if (!transitionRequested)
         return false;

      boolean singularityEscapeDone = true;

      if (doSingularityEscape.getBooleanValue() && waitSingularityEscapeBeforeTransitionToNextState.getBooleanValue())
      {
         nullspaceCalculator.setMatrix(jacobian.getJacobianMatrix(), 1);
         DenseMatrix64F nullspace = nullspaceCalculator.getNullspace();
         ScrewTools.packJointVelocitiesMatrix(jacobian.getJointsInOrder(), jointVelocities);
         double nullspaceVelocityDotProduct = VectorVectorMult.innerProd(nullspace, jointVelocities);

         int velocitySign = (int) Math.round(Math.signum(nullspaceVelocityDotProduct));
         boolean velocitySignOK = velocitySign == velocitySignForSingularityEscape;
         if (jacobianDeterminantInRange.getBooleanValue() || !velocitySignOK)
         {
            singularityEscapeDone = false;
         }
      }

      return singularityEscapeDone;
   }
}
