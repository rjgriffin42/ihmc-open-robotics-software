package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicProjectionDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicProjectionMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.swing.SwingStateEndRecursionMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.transfer.TransferStateEndRecursionMatrix;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.ArrayList;

public class CurrentStateProjectionMultiplier
{
   private final CubicProjectionMatrix cubicProjectionMatrix;
   private final CubicProjectionDerivativeMatrix cubicProjectionDerivativeMatrix;

   private final TransferStateEndRecursionMatrix transferStateEndRecursionMatrix;
   private final SwingStateEndRecursionMatrix swingStateEndRecursionMatrix;

   private final DoubleYoVariable doubleSupportSplitRatio;

   private final DoubleYoVariable startOfSplineTime;
   private final DoubleYoVariable endOfSplineTime;
   private final DoubleYoVariable totalTrajectoryTime;

   private final DenseMatrix64F matrixOut = new DenseMatrix64F(1, 1);

   private final DoubleYoVariable positionMultiplier;
   private final DoubleYoVariable velocityMultiplier;

   public CurrentStateProjectionMultiplier(YoVariableRegistry registry, DoubleYoVariable doubleSupportSplitRatio, DoubleYoVariable startOfSplineTime,
         DoubleYoVariable endOfSplineTime, DoubleYoVariable totalTrajectoryTime)
   {
      positionMultiplier = new DoubleYoVariable("CurrentStateProjectionMultiplier", registry);
      velocityMultiplier = new DoubleYoVariable("CurrentStateVelocityProjectionMultiplier", registry);

      this.doubleSupportSplitRatio = doubleSupportSplitRatio;

      this.startOfSplineTime = startOfSplineTime;
      this.endOfSplineTime = endOfSplineTime;
      this.totalTrajectoryTime = totalTrajectoryTime;

      transferStateEndRecursionMatrix = new TransferStateEndRecursionMatrix();
      swingStateEndRecursionMatrix = new SwingStateEndRecursionMatrix(doubleSupportSplitRatio, startOfSplineTime, endOfSplineTime, totalTrajectoryTime);

      cubicProjectionMatrix = new CubicProjectionMatrix();
      cubicProjectionDerivativeMatrix = new CubicProjectionDerivativeMatrix();
   }

   public void reset()
   {
      positionMultiplier.set(0.0);
      velocityMultiplier.set(0.0);
   }

   public double getPositionMultiplier()
   {
      return positionMultiplier.getDoubleValue();
   }

   public double getVelocityMultiplier()
   {
      return velocityMultiplier.getDoubleValue();
   }

   public void compute(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations, double timeRemaining,
         boolean useTwoCMPs, boolean isInTransfer, double omega0)
   {
      double positionMultiplier, velocityMultiplier;
      if (isInTransfer)
      {
         positionMultiplier = computeInTransfer(doubleSupportDurations, timeRemaining, omega0);
      }
      else
      {
         if (useTwoCMPs)
            positionMultiplier = computeSegmentedProjection(doubleSupportDurations, singleSupportDurations, timeRemaining, omega0);
         else
            positionMultiplier = computeInSwingOneCMP(timeRemaining, omega0);
      }
      this.positionMultiplier.set(positionMultiplier);

      if (isInTransfer)
      {
         velocityMultiplier = computeInTransferVelocity();
      }
      else
      {
         if (useTwoCMPs)
            velocityMultiplier = computeSegmentedVelocityProjection(timeRemaining, omega0);
         else
            velocityMultiplier = computeInSwingOneCMPVelocity(omega0);
      }

      this.velocityMultiplier.set(velocityMultiplier);
   }

   private double computeInTransfer(ArrayList<DoubleYoVariable> doubleSupportDurations, double timeRemaining, double omega0)
   {
      transferStateEndRecursionMatrix.compute(doubleSupportDurations, omega0);

      double splineDuration = doubleSupportDurations.get(0).getDoubleValue();

      cubicProjectionDerivativeMatrix.setSegmentDuration(splineDuration);
      cubicProjectionDerivativeMatrix.update(timeRemaining);
      cubicProjectionMatrix.setSegmentDuration(splineDuration);
      cubicProjectionMatrix.update(timeRemaining);
      CommonOps.mult(cubicProjectionMatrix, transferStateEndRecursionMatrix, matrixOut);

      return 1.0 / matrixOut.get(0, 0);
   }

   private double computeInSwingOneCMP(double timeRemaining, double omega0)
   {
      return Math.exp(omega0 * timeRemaining);
   }

   private double computeSegmentedProjection(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeRemaining, double omega0)
   {
      double timeInState = totalTrajectoryTime.getDoubleValue() - timeRemaining;

      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeFirstSegmentProjection(doubleSupportDurations, singleSupportDurations, timeInState, omega0);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeThirdSegmentProjection(timeRemaining, omega0);
      else
         return computeSecondSegmentProjection(doubleSupportDurations, singleSupportDurations, timeRemaining, omega0);
   }

   private double computeFirstSegmentProjection(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeInState, double omega0)
   {
      double upcomingDoubleSupportDuration = doubleSupportDurations.get(1).getDoubleValue();
      double currentDoubleSupportDuration = doubleSupportDurations.get(0).getDoubleValue();
      double singleSupportDuration = singleSupportDurations.get(0).getDoubleValue();

      double stepDuration = currentDoubleSupportDuration + singleSupportDuration;

      double upcomingInitialDoubleSupportDuration = doubleSupportSplitRatio.getDoubleValue() * upcomingDoubleSupportDuration;
      double endOfDoubleSupportDuration = (1.0 - doubleSupportSplitRatio.getDoubleValue()) * currentDoubleSupportDuration;

      double recursionTime = timeInState + upcomingInitialDoubleSupportDuration + endOfDoubleSupportDuration - stepDuration;
      double recursion = Math.exp(omega0 * recursionTime);

      return 1.0 / recursion;
   }

   private double computeSecondSegmentProjection(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeRemaining, double omega0)
   {
      swingStateEndRecursionMatrix.compute(doubleSupportDurations, singleSupportDurations, omega0);

      double lastSegmentDuration = totalTrajectoryTime.getDoubleValue() - endOfSplineTime.getDoubleValue();
      double timeRemainingInSpline = timeRemaining - lastSegmentDuration;
      double splineDuration = endOfSplineTime.getDoubleValue() - startOfSplineTime.getDoubleValue();

      cubicProjectionDerivativeMatrix.setSegmentDuration(splineDuration);
      cubicProjectionDerivativeMatrix.update(timeRemainingInSpline);
      cubicProjectionMatrix.setSegmentDuration(splineDuration);
      cubicProjectionMatrix.update(timeRemainingInSpline);

      CommonOps.mult(cubicProjectionMatrix, swingStateEndRecursionMatrix, matrixOut);

      return 1.0 / matrixOut.get(0, 0);
   }

   private double computeThirdSegmentProjection(double timeRemaining, double omega0)
   {
      return computeInSwingOneCMP(timeRemaining, omega0);
   }

   private double computeInTransferVelocity()
   {
      CommonOps.mult(cubicProjectionDerivativeMatrix, transferStateEndRecursionMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeInSwingOneCMPVelocity(double omega0)
   {
      return omega0 * (1.0 / positionMultiplier.getDoubleValue());
   }

   private double computeSegmentedVelocityProjection(double timeRemaining, double omega0)
   {
      double timeInState = totalTrajectoryTime.getDoubleValue() - timeRemaining;

      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeFirstSegmentVelocityProjection(omega0);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeThirdSegmentVelocityProjection(omega0);
      else
         return computeSecondSegmentVelocityProjection();
   }

   private double computeFirstSegmentVelocityProjection(double omega0)
   {
      return omega0 * (1.0 / positionMultiplier.getDoubleValue());
   }

   private double computeSecondSegmentVelocityProjection()
   {
      CommonOps.mult(cubicProjectionDerivativeMatrix, swingStateEndRecursionMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeThirdSegmentVelocityProjection(double omega0)
   {
      return omega0 * (1.0 / positionMultiplier.getDoubleValue());
   }
}
