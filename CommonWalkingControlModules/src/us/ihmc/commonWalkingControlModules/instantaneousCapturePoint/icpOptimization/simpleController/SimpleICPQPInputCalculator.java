package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.simpleController;

import org.ejml.data.DenseMatrix64F;
import org.ejml.data.Matrix;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPQPOptimizationSolver;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.qpInput.ICPEqualityConstraintInput;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.qpInput.ICPQPIndexHandler;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.qpInput.ICPQPInput;
import us.ihmc.robotics.linearAlgebra.DiagonalMatrixTools;
import us.ihmc.robotics.linearAlgebra.MatrixTools;

import java.util.ArrayList;

/**
 * This class is used by the {@link ICPQPOptimizationSolver} to  convert weights and gains into the actual objects for the quadratic program.
 */
public class SimpleICPQPInputCalculator
{
   private static final boolean consider_angular_momentum_in_adjustment = true;
   private static final boolean consider_feedback_in_adjustment = true;

   /** Input calculator that formulates the different objectives and handles adding them to the full program. */
   public SimpleICPQPIndexHandler indexHandler;

   private static final DenseMatrix64F identity = CommonOps.identity(2, 2);
   private final DenseMatrix64F tmpObjective = new DenseMatrix64F(2, 1);

   private final DenseMatrix64F feedbackJacobian = new DenseMatrix64F(2, 6);
   private final DenseMatrix64F feedbackJtW = new DenseMatrix64F(6, 2);
   private final DenseMatrix64F feedbackObjective = new DenseMatrix64F(2, 1);

   private final DenseMatrix64F adjustmentJacobian = new DenseMatrix64F(2,6);
   private final DenseMatrix64F adjustmentJtW = new DenseMatrix64F(6,2);
   private final DenseMatrix64F adjustmentObjective = new DenseMatrix64F(2, 1);

   /**
    * Creates the ICP Quadratic Problem Input Calculator. Refer to the class documentation: {@link SimpleICPQPInputCalculator}.
    *
    * @param indexHandler holder of the indices for the different optimization terms.
    */
   public SimpleICPQPInputCalculator(SimpleICPQPIndexHandler indexHandler)
   {
      this.indexHandler = indexHandler;
   }

   /**
    * Computes the CMP feedback minimization task. This simply tries to minimize the total feedback magnitude.
    * Has the form<br>
    *    &delta;<sup>T</sup> Q &delta;<br>
    * where &delta; is the CMP feedback.
    *
    * @param icpQPInputToPack QP input to store the CMP feedback minimization task. Modified.
    * @param feedbackWeight weight attached to minimizing the CMP feedback.
    */
   public void computeFeedbackTask(ICPQPInput icpQPInputToPack, DenseMatrix64F feedbackWeight)
   {
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, 0, 0, feedbackWeight, 0, 0, 2, 2, 1.0);
   }

   /**
    * Computes the CMP feedback regularization task. This tries to minimize the distance of the current solution
    * from the previous solution. Has the form<br>
    *    (&delta; - &delta;<sub>prev</sub>)<sup>T</sup> Q (&delta; - &delta;<sub>prev</sub>)<br>
    * where &delta; is the CMP feedback and &delta;<sub>prev</sub> is the previous solution.
    *
    * @param icpQPInputToPack QP input to store the CMP feedback regularization task. Modified.
    * @param regularizationWeight weight attached to regularization the CMP feedback.
    * @param objective the previous solution value, &delta;<sub>prev</sub>
    */
   public void computeFeedbackRegularizationTask(ICPQPInput icpQPInputToPack, DenseMatrix64F regularizationWeight, DenseMatrix64F objective)
   {
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, 0, 0, regularizationWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(objective);
      CommonOps.mult(regularizationWeight, tmpObjective, tmpObjective);

      MatrixTools.addMatrixBlock(icpQPInputToPack.linearTerm, 0, 0, tmpObjective, 0, 0, 2, 1, 1.0);

      CommonOps.multTransA(objective, tmpObjective, icpQPInputToPack.residualCost);
      CommonOps.scale(0.5, icpQPInputToPack.residualCost);
   }

   /**
    * Computes the angular momentum minimization task. This simply tries to minimize the angular momentum.
    * Has the form<br>
    *    &kappa;<sup>T</sup> Q &kappa;<br>
    * where &kappa; is the angular momentum.
    *
    * @param icpQPInputToPack QP input to store the angular momentum minimization task. Modified.
    * @param angularMomentumMinimizationWeight weight attached to minimizing the angular momentum.
    */
   public void computeAngularMomentumMinimizationTask(ICPQPInput icpQPInputToPack, DenseMatrix64F angularMomentumMinimizationWeight)
   {
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, 0, 0, angularMomentumMinimizationWeight, 0, 0, 2, 2, 1.0);
   }

   /**
    * Computes the step adjustment task for a single footstep. This attempts to have the footstep track a nominal location.
    * Has the form<br>
    *    (r<sub>f</sub> - r<sub>f,r</sub>)<sup>T</sup> Q (r<sub>f</sub> - r<sub>f,r</sub>)<br>
    * where r<sub>f</sub> is the footstep location and r<sub>f,r</sub> is the reference footstep location.
    *
    *
    * @param footstepNumber current footstep number of the task to formulate.
    * @param icpQPInputToPack QP input to store the step adjustment minimization task. Modified.
    * @param footstepWeight weight attached to minimizing the step adjustment.
    * @param objective reference footstep location, r<sub>f,r</sub>
    */
   public void computeFootstepTask(int footstepNumber, ICPQPInput icpQPInputToPack, DenseMatrix64F footstepWeight, DenseMatrix64F objective)
   {
      int footstepIndex = 2 * footstepNumber;
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, footstepIndex, footstepIndex, footstepWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(objective);
      CommonOps.mult(footstepWeight, tmpObjective, tmpObjective);

      MatrixTools.addMatrixBlock(icpQPInputToPack.linearTerm, footstepIndex, 0, tmpObjective, 0, 0, 2, 1, 1.0);

      CommonOps.multTransA(objective, tmpObjective, icpQPInputToPack.residualCost);
      CommonOps.scale(0.5, icpQPInputToPack.residualCost);
   }

   /**
    * Computes the step adjustment regularization task for a single footstep. This attempts to minimize the change from the previous step
    * adjustment solution. Has the form<br>
    *    (r<sub>f</sub> - r<sub>f,prev</sub>)<sup>T</sup> Q (r<sub>f</sub> - r<sub>f,prev</sub>)<br>
    * where r<sub>f</sub> is the footstep location and r<sub>f,r</sub> is the previous footstep location solution.
    *
    *
    * @param footstepNumber current footstep number of the task to formulate.
    * @param icpQPInputToPack QP input to store the step adjustment regularization task. Modified.
    * @param regularizationWeight weight attached to regularizing the step adjustment.
    * @param objective previous footstep location, r<sub>f,prev</sub>
    */
   public void computeFootstepRegularizationTask(int footstepNumber, ICPQPInput icpQPInputToPack, DenseMatrix64F regularizationWeight, DenseMatrix64F objective)
   {
      int footstepIndex = 2 * footstepNumber;
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, footstepIndex, footstepIndex, regularizationWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(objective);
      CommonOps.mult(regularizationWeight, tmpObjective, tmpObjective);

      MatrixTools.addMatrixBlock(icpQPInputToPack.linearTerm, footstepIndex, 0, tmpObjective, 0, 0, 2, 1, 1.0);

      CommonOps.multTransA(objective, tmpObjective, icpQPInputToPack.residualCost);
      CommonOps.scale(0.5, icpQPInputToPack.residualCost);
   }

   public void computeDynamicsTask(ICPQPInput icpQPInput, DenseMatrix64F currentICPError, DenseMatrix64F referenceFootstepLocation,
         DenseMatrix64F feedbackGain, DenseMatrix64F weight, double footstepRecursionMultiplier, double footstepAdjustmentSafetyFactor)
   {
      DiagonalMatrixTools.invertDiagonalMatrix(feedbackGain);

      int size = 2;
      if (indexHandler.useAngularMomentum())
         size += 2;
      if (indexHandler.useStepAdjustment())
         size += 2;

      feedbackJacobian.reshape(2, size);
      feedbackJtW.reshape(size, 2);
      adjustmentJacobian.reshape(2, size);
      adjustmentJtW.reshape(size, 2);

      feedbackJacobian.zero();
      feedbackJtW.zero();
      feedbackObjective.zero();

      adjustmentJacobian.zero();
      adjustmentJtW.zero();
      adjustmentObjective.zero();

      MatrixTools.setMatrixBlock(feedbackJacobian, 0, indexHandler.getFeedbackCMPIndex(), feedbackGain, 0, 0, 2, 2, 1.0);

      if (indexHandler.useAngularMomentum())
         MatrixTools.setMatrixBlock(feedbackJacobian, 0, indexHandler.getAngularMomentumIndex(), feedbackGain, 0, 0, 2, 2, 1.0);

      if (indexHandler.useStepAdjustment())
      {
         CommonOps.setIdentity(identity);
         CommonOps.scale(footstepRecursionMultiplier / footstepAdjustmentSafetyFactor, identity, identity);

         if (consider_angular_momentum_in_adjustment)
         {
            MatrixTools.setMatrixBlock(feedbackJacobian, 0, indexHandler.getFootstepStartIndex(), identity, 0, 0, 2, 2, 1.0);

            MatrixTools.addMatrixBlock(feedbackObjective, 0, 0, referenceFootstepLocation, 0, 0, 2, 1, footstepRecursionMultiplier);
         }
         else
         {
            MatrixTools.setMatrixBlock(adjustmentJacobian, 0, indexHandler.getFootstepStartIndex(), identity, 0, 0, 2, 2, 1.0);

            if (consider_feedback_in_adjustment)
               MatrixTools.setMatrixBlock(adjustmentJacobian, 0, indexHandler.getFeedbackCMPIndex(), feedbackGain, 0, 0, 2, 2, 1.0);

            MatrixTools.addMatrixBlock(adjustmentObjective, 0, 0, referenceFootstepLocation, 0, 0, 2, 1, footstepRecursionMultiplier);
            MatrixTools.addMatrixBlock(adjustmentObjective, 0, 0, currentICPError, 0, 0, 2, 1, 1.0);
         }
      }

      MatrixTools.addMatrixBlock(feedbackObjective, 0, 0, currentICPError, 0, 0, 2, 1, 1.0);

      CommonOps.multTransA(feedbackJacobian, weight, feedbackJtW);
      CommonOps.multTransA(5.0, adjustmentJacobian, weight, adjustmentJtW);

      CommonOps.multAdd(feedbackJtW, feedbackJacobian, icpQPInput.quadraticTerm);
      CommonOps.multAdd(adjustmentJtW, adjustmentJacobian, icpQPInput.quadraticTerm);

      CommonOps.multAdd(feedbackJtW, feedbackObjective, icpQPInput.linearTerm);
      CommonOps.multAdd(adjustmentJtW, adjustmentObjective, icpQPInput.linearTerm);

      // todo residual cost
   }

   /**
    * Submits the CMP feedback action task to the total quadratic program cost terms.
    *
    * @param icpQPInput QP Input that stores the data.
    * @param solverInput_H_ToPack full problem quadratic cost term. Modified.
    * @param solverInput_h_ToPack full problem linear cost term. Modified.
    * @param solverInputResidualCost full problem residual cost term.
    */
   public void submitFeedbackTask(ICPQPInput icpQPInput, DenseMatrix64F solverInput_H_ToPack, DenseMatrix64F solverInput_h_ToPack,
         DenseMatrix64F solverInputResidualCost)
   {
      int feedbackCMPIndex = indexHandler.getFeedbackCMPIndex();
      MatrixTools.addMatrixBlock(solverInput_H_ToPack, feedbackCMPIndex, feedbackCMPIndex, icpQPInput.quadraticTerm, 0, 0, 2, 2, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h_ToPack, feedbackCMPIndex, 0, icpQPInput.linearTerm, 0, 0, 2, 1, 1.0);
      MatrixTools.addMatrixBlock(solverInputResidualCost, 0, 0, icpQPInput.residualCost, 0, 0, 1, 1, 1.0);
   }

   /**
    * Submits the dynamic relaxation minimization task to the total quadratic program cost terms.
    *
    * @param icpQPInput QP Input that stores the data.
    * @param solverInput_H_ToPack full problem quadratic cost term.
    * @param solverInput_h_ToPack full problem linear cost term.
    * @param solverInputResidualCost full problem residual cost term.
    */
   public void submitDynamicsTask(ICPQPInput icpQPInput, DenseMatrix64F solverInput_H_ToPack, DenseMatrix64F solverInput_h_ToPack,
         DenseMatrix64F solverInputResidualCost)
   {
      int size = icpQPInput.linearTerm.getNumRows();
      MatrixTools.addMatrixBlock(solverInput_H_ToPack, 0, 0, icpQPInput.quadraticTerm, 0, 0, size, size, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h_ToPack, 0, 0, icpQPInput.linearTerm, 0, 0, size, 1, 1.0);
      MatrixTools.addMatrixBlock(solverInputResidualCost, 0, 0, icpQPInput.residualCost, 0, 0, 1, 1, 1.0);
   }

   /**
    * Submits the angular momentum minimization task to the total quadratic program cost terms.
    *
    * @param icpQPInput QP Input that stores the data.
    * @param solverInput_H_ToPack full problem quadratic cost term.
    * @param solverInput_h_ToPack full problem linear cost term.
    * @param solverInputResidualCost full problem residual cost term.
    */
   public void submitAngularMomentumMinimizationTask(ICPQPInput icpQPInput, DenseMatrix64F solverInput_H_ToPack, DenseMatrix64F solverInput_h_ToPack,
         DenseMatrix64F solverInputResidualCost)
   {
      int angularMomentumIndex = indexHandler.getAngularMomentumIndex();
      MatrixTools.addMatrixBlock(solverInput_H_ToPack, angularMomentumIndex, angularMomentumIndex, icpQPInput.quadraticTerm, 0, 0, 2, 2, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h_ToPack, angularMomentumIndex, 0, icpQPInput.linearTerm, 0, 0, 2, 1, 1.0);
      MatrixTools.addMatrixBlock(solverInputResidualCost, 0, 0, icpQPInput.residualCost, 0, 0, 1, 1, 1.0);
   }

   /**
    * Submits the footstep adjustment minimization task to the total quadratic program cost terms.
    *
    * @param icpQPInput QP Input that stores the data.
    * @param solverInput_H_ToPack full problem quadratic cost term.
    * @param solverInput_h_ToPack full problem linear cost term.
    * @param solverInputResidualCost full problem residual cost term.
    */
   public void submitFootstepTask(ICPQPInput icpQPInput, DenseMatrix64F solverInput_H_ToPack, DenseMatrix64F solverInput_h_ToPack,
         DenseMatrix64F solverInputResidualCost)
   {
      int numberOfFootstepVariables = indexHandler.getNumberOfFootstepVariables();

      int footstepStartIndex = indexHandler.getFootstepStartIndex();
      MatrixTools.addMatrixBlock(solverInput_H_ToPack, footstepStartIndex, footstepStartIndex, icpQPInput.quadraticTerm, 0, 0, numberOfFootstepVariables, numberOfFootstepVariables, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h_ToPack, footstepStartIndex, 0, icpQPInput.linearTerm, 0, 0, numberOfFootstepVariables, 1, 1.0);
      MatrixTools.addMatrixBlock(solverInputResidualCost, 0, 0, icpQPInput.residualCost, 0, 0, 1, 1, 1.0);
   }
}
