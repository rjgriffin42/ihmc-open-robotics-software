package us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization;

import org.ejml.data.DenseMatrix64F;

import us.ihmc.commonWalkingControlModules.wrenchDistribution.PlaneContactWrenchMatrixCalculator;
import us.ihmc.utilities.exeptions.NoConvergenceException;

public class MomentumOptimizerAdapter
{
   private CVXMomentumOptimizerWithGRFPenalizedSmootherNative momentumOptimizerWithGRFPenalizedSmootherNative;
   private CVXMomentumOptimizerWithGRFPenalizedSmootherNativeInput momentumOptimizerWithGRFPenalizedSmootherNativeInput;

   private final int rhoSize;
   private final int nSupportVectors;
   private final int nPointsPerPlane;
   private final int nPlanes;

   private final DenseMatrix64F rhoPrevious;

   private DenseMatrix64F outputRho, outputJointAccelerations;
   private double outputOptVal;

   public MomentumOptimizerAdapter(int nDoF)
   {
      rhoSize = CVXMomentumOptimizerWithGRFPenalizedSmootherNative.rhoSize;
      nSupportVectors = CVXMomentumOptimizerWithGRFPenalizedSmootherNative.nSupportVectors;
      nPointsPerPlane = CVXMomentumOptimizerWithGRFPenalizedSmootherNative.nPointsPerPlane;
      nPlanes = CVXMomentumOptimizerWithGRFPenalizedSmootherNative.nPlanes;

      momentumOptimizerWithGRFPenalizedSmootherNative = new CVXMomentumOptimizerWithGRFPenalizedSmootherNative(nDoF, rhoSize);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput = new CVXMomentumOptimizerWithGRFPenalizedSmootherNativeInput();

      outputRho = new DenseMatrix64F(rhoSize, 1);
      rhoPrevious = new DenseMatrix64F(rhoSize, 1);
   }

   public void setRateOfChangeOfGroundReactionForceRegularization(DenseMatrix64F wRhoSmoother)
   {
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setRateOfChangeOfGroundReactionForceRegularization(wRhoSmoother);
   }

   public void reset()
   {
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.reset();
   }

   public int getRhoSize()
   {
      return rhoSize;
   }

   public int getNSupportVectors()
   {
      return nSupportVectors;
   }

   public int getNPointsPerPlane()
   {
      return nPointsPerPlane;
   }

   public int getNPlanes()
   {
      return nPlanes;
   }

   public void setInputs(DenseMatrix64F a, DenseMatrix64F b, PlaneContactWrenchMatrixCalculator wrenchMatrixCalculator,
                         DenseMatrix64F wrenchEquationRightHandSide, DenseMatrix64F momentumDotWeight, DenseMatrix64F dampedLeastSquaresFactorMatrix,
                         DenseMatrix64F jSecondary, DenseMatrix64F pSecondary, DenseMatrix64F weightMatrixSecondary)
   {
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setCentroidalMomentumMatrix(a);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setMomentumDotEquationRightHandSide(b);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setContactPointWrenchMatrix(wrenchMatrixCalculator.getQRho());
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setRhoMin(wrenchMatrixCalculator.getRhoMin());
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setRhoPrevious(rhoPrevious);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setWrenchEquationRightHandSide(wrenchEquationRightHandSide);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setMomentumDotWeight(momentumDotWeight);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setJointAccelerationRegularization(dampedLeastSquaresFactorMatrix);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setSecondaryConstraintJacobian(jSecondary);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setSecondaryConstraintRightHandSide(pSecondary);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setSecondaryConstraintWeight(weightMatrixSecondary);
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setGroundReactionForceRegularization(wrenchMatrixCalculator.getWRho());
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setRateOfChangeOfGroundReactionForceRegularization(wrenchMatrixCalculator.getWRhoSmoother());
      
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setRhoPreviousAverage(wrenchMatrixCalculator.getRhoPreviousAverage());
      momentumOptimizerWithGRFPenalizedSmootherNativeInput.setCenterOfPressurePenalizedRegularization(wrenchMatrixCalculator.getWRhoPenalizer());
   }

   public void solve() throws NoConvergenceException
   {
      rhoPrevious.set(outputRho);

      try
      {
         momentumOptimizerWithGRFPenalizedSmootherNative.solve(momentumOptimizerWithGRFPenalizedSmootherNativeInput);
      }
      finally
      {
         CVXMomentumOptimizerWithGRFPenalizedSmootherNativeOutput momentumOptimizerWithGRFPenalizedSmootherNativeOutput =
               momentumOptimizerWithGRFPenalizedSmootherNative.getOutput();
         outputRho = momentumOptimizerWithGRFPenalizedSmootherNativeOutput.getRho();
         outputJointAccelerations = momentumOptimizerWithGRFPenalizedSmootherNativeOutput.getJointAccelerations();
         outputOptVal = momentumOptimizerWithGRFPenalizedSmootherNativeOutput.getOptVal();
      }
   }

   public DenseMatrix64F getOutputRho()
   {
      return outputRho;
   }

   public DenseMatrix64F getOutputJointAccelerations()
   {
      return outputJointAccelerations;
   }

   public double getOutputOptVal()
   {
      return outputOptVal;
   }
}
