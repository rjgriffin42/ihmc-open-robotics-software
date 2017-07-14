package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.simpleController;

import org.ejml.data.DenseMatrix64F;
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
   /** Input calculator that formulates the different objectives and handles adding them to the full program. */
   public SimpleICPQPIndexHandler indexHandler;

   private static final DenseMatrix64F identity = CommonOps.identity(2, 2);
   private final DenseMatrix64F tmpObjective = new DenseMatrix64F(2, 1);
   private final DenseMatrix64F tmpJacobian = new DenseMatrix64F(6, 1);
   private final DenseMatrix64F tmpJtW = new DenseMatrix64F(6, 1);

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
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, 2 * footstepNumber, 2 * footstepNumber, footstepWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(objective);
      CommonOps.mult(footstepWeight, tmpObjective, tmpObjective);

      MatrixTools.addMatrixBlock(icpQPInputToPack.linearTerm, 2 * footstepNumber, 0, tmpObjective, 0, 0, 2, 1, 1.0);

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
      MatrixTools.addMatrixBlock(icpQPInputToPack.quadraticTerm, 2 * footstepNumber, 2 * footstepNumber, regularizationWeight, 0, 0, 2, 2, 1.0);

      tmpObjective.zero();
      tmpObjective.set(objective);
      CommonOps.mult(regularizationWeight, tmpObjective, tmpObjective);

      MatrixTools.addMatrixBlock(icpQPInputToPack.linearTerm, 2 * footstepNumber, 0, tmpObjective, 0, 0, 2, 1, 1.0);

      CommonOps.multTransA(objective, tmpObjective, icpQPInputToPack.residualCost);
      CommonOps.scale(0.5, icpQPInputToPack.residualCost);
   }

   /**
    * Computes the recursive dynamics constraint for the ICP Optimization solver. The observers that the reference ICP location is a linear
    * function of the upcoming footstep locations. This defines the relationship between the CMP feedback and the upcoming footstep locations.
    * Has the form<br>
    *    &delta; = k<sub>p</sub> ( &xi; - &Phi;<sub>f</sub> - &Phi;<sub>const</sub> - sum &gamma;<sub>i</sub> r<sub>f,i</sub>) + &epsilon;,<br>
    * where
    * <li>&delta; is the CMP feedback action.</li>
    * <li>k<sub>p</sub> is the ICP proportional feedback gain.</li>
    * <li>&xi; is the current ICP location.</li>
    * <li>&Phi;<sub>f</sub> is the value of the final ICP recursion.</li>
    * <li>&Phi;<sub>const</sub> is the value of the recursive effects of the CMP offsets in the upcoming footsteps and the stance CMP values.</li>
    * <li>&gamma;<sub>i</sub> is the recursion multiplier of the i<sup>th</sup> footstep.</li>
    * <li>r<sub>f,i</sub> is the i<sup>th</sup> footstep.</li>
    * <li>&epsilon; is the dynamic relaxation slack variable that prevents over constraining the problem.</li>
    *
    * @param feedbackGain proportional ICP feedback gain, k<sub>p</sub>.
    */
   public void computeDynamicsTask(ICPQPInput icpQPInput, DenseMatrix64F currentICPError, DenseMatrix64F referenceFootstepLocation,
         DenseMatrix64F feedbackGain, DenseMatrix64F weight, double footstepRecursionMultiplier, double footstepAdjustmentSafetyFactor)
   {
      DiagonalMatrixTools.invertDiagonalMatrix(feedbackGain);
      CommonOps.scale(-1.0, feedbackGain);

      int size = 2;
      if (indexHandler.useAngularMomentum())
         size += 2;
      if (indexHandler.useStepAdjustment())
         size += 2;

      tmpJacobian.reshape(2, size);
      tmpJtW.reshape(size, 2);
      tmpJacobian.zero();
      tmpObjective.zero();

      MatrixTools.setMatrixBlock(tmpJacobian, 0, indexHandler.getFeedbackCMPIndex(), feedbackGain, 0, 0, 2, 2, 1.0);

      if (indexHandler.useAngularMomentum())
         MatrixTools.setMatrixBlock(tmpJacobian, 0, indexHandler.getAngularMomentumIndex(), feedbackGain, 0, 0, 2, 2, 1.0);

      if (indexHandler.useStepAdjustment())
      {
         CommonOps.scale(-1.0 / footstepAdjustmentSafetyFactor, feedbackGain);
         MatrixTools.setMatrixBlock(tmpJacobian, 0, indexHandler.getFootstepStartIndex(), feedbackGain, 0, 0, 2, 2, 1.0);
      }

      CommonOps.addEquals(currentICPError, footstepRecursionMultiplier, referenceFootstepLocation);
      MatrixTools.setMatrixBlock(tmpObjective, 0, 0, currentICPError, 0, 0, 2, 1, 1.0);

      CommonOps.multTransA(tmpJacobian, weight, tmpJtW);

      CommonOps.mult(tmpJtW, tmpJacobian, icpQPInput.quadraticTerm);
      CommonOps.mult(tmpJtW, tmpObjective, icpQPInput.linearTerm);

      CommonOps.mult(weight, tmpObjective, icpQPInput.residualCost);
      CommonOps.multTransA(tmpObjective, icpQPInput.residualCost, icpQPInput.residualCost);
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
      MatrixTools.addMatrixBlock(solverInput_H_ToPack, 0, 0, icpQPInput.quadraticTerm, 0, 0, 2, 2, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h_ToPack, 0, 0, icpQPInput.linearTerm, 0, 0, 2, 1, 1.0);
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
      MatrixTools.addMatrixBlock(solverInput_H_ToPack, 0, 0, icpQPInput.quadraticTerm, footstepStartIndex, 0, numberOfFootstepVariables, numberOfFootstepVariables, 1.0);
      MatrixTools.addMatrixBlock(solverInput_h_ToPack, 0, 0, icpQPInput.linearTerm, footstepStartIndex, 0, numberOfFootstepVariables, 1, 1.0);
      MatrixTools.addMatrixBlock(solverInputResidualCost, 0, 0, icpQPInput.residualCost, 0, 0, 1, 1, 1.0);
   }
}
