package us.ihmc.trajectoryOptimization;

import org.ejml.data.DenseMatrix64F;

public interface LQCostFunction<E extends Enum>
{
   /** L(X_k, U_k) */
   double getCost(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector);

   /** L_x(X_k, U_k) */
   void getCostStateGradient(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack);
   /** L_u(X_k, U_k) */
   void getCostControlGradient(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack);

   /** L_xx(X_k, U_k) */
   void getCostStateHessian(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack);
   /** L_uu(X_k, U_k) */
   void getCostControlHessian(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack);
   /** L_ux(X_k, U_k) */
   void getCostStateGradientOfControlGradient(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack);
   /** L_xu(X_k, U_k) */
   void getCostControlGradientOfStateGradient(E state, DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack);
}
