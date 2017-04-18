package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.current;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.CubicDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.CubicMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.transfer.NewTransferInitialICPVelocityMatrix;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.List;

public class NewInitialICPVelocityCurrentMultiplier
{
   private final CubicMatrix cubicMatrix;
   private final CubicDerivativeMatrix cubicDerivativeMatrix;

   private final boolean givenCubicMatrix;
   private final boolean givenCubicDerivativeMatrix;

   private final NewTransferInitialICPVelocityMatrix transferInitialICPVelocityMatrix;

   private final DenseMatrix64F matrixOut = new DenseMatrix64F(1, 1);

   private final DoubleYoVariable positionMultiplier;
   private final DoubleYoVariable velocityMultiplier;

   public NewInitialICPVelocityCurrentMultiplier(String yoNamePrefix, YoVariableRegistry registry)
   {
      this(null, null, yoNamePrefix, registry);
   }

   public NewInitialICPVelocityCurrentMultiplier(CubicMatrix cubicMatrix, CubicDerivativeMatrix cubicDerivativeMatrix, String yoNamePrefix, YoVariableRegistry registry)
   {
      positionMultiplier = new DoubleYoVariable(yoNamePrefix + "InitialICPVelocityCurrentMultiplier", registry);
      velocityMultiplier = new DoubleYoVariable(yoNamePrefix + "InitialICPCVelocityCurrentVelocityMultiplier", registry);

      if (cubicMatrix == null)
      {
         this.cubicMatrix = new CubicMatrix();
         givenCubicMatrix = false;
      }
      else
      {
         this.cubicMatrix = cubicMatrix;
         givenCubicMatrix = true;
      }

      if (cubicDerivativeMatrix == null)
      {
         this.cubicDerivativeMatrix = new CubicDerivativeMatrix();
         givenCubicDerivativeMatrix = false;
      }
      else
      {
         this.cubicDerivativeMatrix = cubicDerivativeMatrix;
         givenCubicDerivativeMatrix = true;
      }

      transferInitialICPVelocityMatrix = new NewTransferInitialICPVelocityMatrix();
   }

   public void reset()
   {
      positionMultiplier.setToNaN();
      velocityMultiplier.setToNaN();
   }

   public double getPositionMultiplier()
   {
      return positionMultiplier.getDoubleValue();
   }

   public double getVelocityMultiplier()
   {
      return velocityMultiplier.getDoubleValue();
   }

   public void compute(List<DoubleYoVariable> doubleSupportDurations, double timeInState, boolean isInTransfer)
   {
      double positionMultiplier, velocityMultiplier;
      if (isInTransfer)
         positionMultiplier = computeInTransfer(doubleSupportDurations, timeInState);
      else
         positionMultiplier = 0.0;
      this.positionMultiplier.set(positionMultiplier);

      if (isInTransfer)
         velocityMultiplier = computeInTransferVelocity();
      else
         velocityMultiplier = 0.0;

      this.velocityMultiplier.set(velocityMultiplier);
   }

   private double computeInTransfer(List<DoubleYoVariable> doubleSupportDurations, double timeInState)
   {
      transferInitialICPVelocityMatrix.compute();

      double splineDuration = doubleSupportDurations.get(0).getDoubleValue();

      if (!givenCubicDerivativeMatrix)
      {
         cubicDerivativeMatrix.setSegmentDuration(splineDuration);
         cubicDerivativeMatrix.update(timeInState);
      }

      if (!givenCubicMatrix)
      {
         cubicMatrix.setSegmentDuration(splineDuration);
         cubicMatrix.update(timeInState);
      }

      CommonOps.mult(cubicMatrix, transferInitialICPVelocityMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeInTransferVelocity()
   {
      CommonOps.mult(cubicDerivativeMatrix, transferInitialICPVelocityMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }
}
