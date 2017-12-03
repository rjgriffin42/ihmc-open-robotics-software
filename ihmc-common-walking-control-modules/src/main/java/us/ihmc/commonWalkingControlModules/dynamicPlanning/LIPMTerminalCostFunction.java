package us.ihmc.commonWalkingControlModules.dynamicPlanning;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.robotics.linearAlgebra.DiagonalMatrixTools;

import static us.ihmc.commonWalkingControlModules.dynamicPlanning.LIPMDynamics.controlVectorSize;
import static us.ihmc.commonWalkingControlModules.dynamicPlanning.LIPMDynamics.stateVectorSize;

public class LIPMTerminalCostFunction implements DDPCostFunction
{
   private final DenseMatrix64F Q = new DenseMatrix64F(stateVectorSize, stateVectorSize);
   private final DenseMatrix64F R = new DenseMatrix64F(controlVectorSize, controlVectorSize);

   public LIPMTerminalCostFunction()
   {
      Q.set(0, 0, 1000);
      Q.set(1, 1, 1000);
      Q.set(2, 2, 1000);
      Q.set(3, 3, 10000);
      Q.set(4, 4, 10000);
      Q.set(5, 5, 10000);

      R.set(0, 0, 100);
      R.set(1, 1, 100);
      R.set(2, 2, 1e-6);
   }

   private DenseMatrix64F tempStateMatrix = new DenseMatrix64F(stateVectorSize, 1);
   private DenseMatrix64F tempControlMatrix = new DenseMatrix64F(controlVectorSize, 1);
   private DenseMatrix64F tempWX = new DenseMatrix64F(stateVectorSize, 1);
   private DenseMatrix64F tempWU = new DenseMatrix64F(controlVectorSize, 1);

   public double getCost(DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F desiredControlVector,
                  DenseMatrix64F desiredStateVector)
   {
      CommonOps.subtract(controlVector, desiredControlVector, tempControlMatrix);
      CommonOps.subtract(stateVector, desiredStateVector, tempStateMatrix);

      DiagonalMatrixTools.preMult(Q, tempStateMatrix, tempWX);
      DiagonalMatrixTools.preMult(R, tempControlMatrix, tempWU);

      return CommonOps.dot(tempControlMatrix, tempWU) + CommonOps.dot(tempStateMatrix, tempWX);
   }

   /** L_x(X_k, U_k) */
   public void getCostStateGradient(DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F desiredControlVector,
                                    DenseMatrix64F desiredStateVector, DenseMatrix64F matrixToPack)
   {
      CommonOps.subtract(stateVector, desiredStateVector, tempStateMatrix);
      DiagonalMatrixTools.preMult(Q, tempStateMatrix, matrixToPack);
   }

   /** L_u(X_k, U_k) */
   public void getCostControlGradient(DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F desiredControlVecotr,
                               DenseMatrix64F desiredStateVector, DenseMatrix64F matrixToPack)
   {
      CommonOps.subtract(controlVector, desiredControlVecotr, tempControlMatrix);
      DiagonalMatrixTools.preMult(R, tempControlMatrix, matrixToPack);
   }

   /** L_xx(X_k, U_k) */
   public void getCostStateHessian(DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(Q);
   }

   /** L_uu(X_k, U_k) */
   public void getCostControlHessian(DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack)
   {
      matrixToPack.set(R);
   }

   /** L_ux(X_k, U_k) */
   public void getCostStateGradientOfControlGradient(DenseMatrix64F controlVector, DenseMatrix64F stateVector, DenseMatrix64F matrixToPack)
   {
      matrixToPack.reshape(controlVectorSize, stateVectorSize);
   }
}
