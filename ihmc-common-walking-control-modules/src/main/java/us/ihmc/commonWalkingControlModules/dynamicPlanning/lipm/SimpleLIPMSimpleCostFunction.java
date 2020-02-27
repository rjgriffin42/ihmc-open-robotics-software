package us.ihmc.commonWalkingControlModules.dynamicPlanning.lipm;

import static us.ihmc.commonWalkingControlModules.dynamicPlanning.lipm.SimpleLIPMDynamics.controlVectorSize;
import static us.ihmc.commonWalkingControlModules.dynamicPlanning.lipm.SimpleLIPMDynamics.stateVectorSize;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.matrixlib.DiagonalMatrixTools;
import us.ihmc.trajectoryOptimization.DefaultDiscreteState;
import us.ihmc.trajectoryOptimization.LQTrackingCostFunction;

public class SimpleLIPMSimpleCostFunction implements LQTrackingCostFunction<DefaultDiscreteState>
{
   private final DenseMatrix64F Q = new DenseMatrix64F(stateVectorSize, stateVectorSize);
   private final DenseMatrix64F R = new DenseMatrix64F(controlVectorSize, controlVectorSize);

   /**
    * This is a cost of the form 0.5 (X - X_d)^T Q (X - X_d) + 0.5 (U - U_d)^T R (U - U_d)
    */

   public SimpleLIPMSimpleCostFunction()
   {
      this(1.0);
   }

   public SimpleLIPMSimpleCostFunction(double deltaT)
   {
      Q.set(0, 0, 1e-3);
      Q.set(1, 1, 1e-3);
      Q.set(2, 2, 1e-3);
      Q.set(3, 3, 1e-3);

      R.set(0, 0, 1e1);
      R.set(1, 1, 1e1);

      CommonOps.scale(deltaT * deltaT, Q);
      CommonOps.scale(deltaT * deltaT, R);
   }

   private DenseMatrix64F tempStateMatrix = new DenseMatrix64F(stateVectorSize, 1);
   private DenseMatrix64F tempControlMatrix = new DenseMatrix64F(controlVectorSize, 1);
   private DenseMatrix64F tempWX = new DenseMatrix64F(stateVectorSize, 1);
   private DenseMatrix64F tempWU = new DenseMatrix64F(controlVectorSize, 1);

   @Override
   public double getCost(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F desiredControlVector,
                         DenseMatrix64F desiredStateVector, DenseMatrix64F constants)
   {
      CommonOps.subtract(controlVector, desiredControlVector, tempControlMatrix);
      CommonOps.subtract(stateVector, desiredStateVector, tempStateMatrix);

      DiagonalMatrixTools.preMult(Q, tempStateMatrix, tempWX);
      DiagonalMatrixTools.preMult(R, tempControlMatrix, tempWU);

      return CommonOps.dot(tempControlMatrix, tempWU) + CommonOps.dot(tempStateMatrix, tempWX);
   }

   @Override
   /** L_x(X_k, U_k) */
   public void getCostStateGradient(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F desiredControlVector,
                                    DenseMatrix64F desiredStateVector, DenseMatrix64F constants, DenseMatrix64F matrixToPack)
   {
      CommonOps.subtract(stateVector, desiredStateVector, tempStateMatrix);
      DiagonalMatrixTools.preMult(Q, tempStateMatrix, matrixToPack);
   }

   /** L_u(X_k, U_k) */
   @Override
   public void getCostControlGradient(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F desiredControlVector,
                                      DenseMatrix64F desiredStateVector, DenseMatrix64F constants, DenseMatrix64F matrixToPack)
   {
      CommonOps.subtract(controlVector, desiredControlVector, tempControlMatrix);
      DiagonalMatrixTools.preMult(R, tempControlMatrix, matrixToPack);
   }

   /** L_xx(X_k, U_k) */
   @Override
   public void getCostStateHessian(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F constants,
                                   DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(Q);
   }

   /** L_uu(X_k, U_k) */
   @Override
   public void getCostControlHessian(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F constants,
                                     DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(R);
   }

   /** L_ux(X_k, U_k) */
   @Override
   public void getCostStateGradientOfControlGradient(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector,
                                                     DenseMatrix64F constants, DenseMatrix64F matrixToPack)
   {
      matrixToPack.reshape(controlVectorSize, stateVectorSize);
   }

   /** L_xu(X_k, U_k) */
   @Override
   public void getCostControlGradientOfStateGradient(DefaultDiscreteState state, DenseMatrix64F controlVector, DenseMatrix64F stateVector,
                                                     DenseMatrix64F constants, DenseMatrix64F matrixToPack)
   {
      matrixToPack.reshape(stateVectorSize, controlVectorSize);
   }
}
