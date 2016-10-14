package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicProjectionDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicProjectionMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.swing.SwingEntryCMPProjectionMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.transfer.TransferEntryCMPProjectionMatrix;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.ArrayList;

public class EntryCMPProjectionMultiplier
{
   private final CubicProjectionMatrix cubicProjectionMatrix;
   private final CubicProjectionDerivativeMatrix cubicProjectionDerivativeMatrix;

   private final TransferEntryCMPProjectionMatrix transferEntryCMPProjectionMatrix;
   private final SwingEntryCMPProjectionMatrix swingEntryCMPProjectionMatrix;

   private final DoubleYoVariable exitCMPRatio;
   private final DoubleYoVariable doubleSupportSplitRatio;

   private final DoubleYoVariable startOfSplineTime;
   private final DoubleYoVariable endOfSplineTime;
   private final DoubleYoVariable totalTrajectoryTime;

   private final DenseMatrix64F matrixOut = new DenseMatrix64F(1, 1);

   private final DoubleYoVariable positionMultiplier;
   private final DoubleYoVariable velocityMultiplier;

   public EntryCMPProjectionMultiplier(YoVariableRegistry registry, DoubleYoVariable doubleSupportSplitRatio,
         DoubleYoVariable exitCMPRatio, DoubleYoVariable startOfSplineTime, DoubleYoVariable endOfSplineTime, DoubleYoVariable totalTrajectoryTime)
   {
      positionMultiplier = new DoubleYoVariable("EntryCMPProjectionMultiplier", registry);
      velocityMultiplier = new DoubleYoVariable("EntryCMPVelocityProjectionMultiplier", registry);

      this.exitCMPRatio = exitCMPRatio;
      this.doubleSupportSplitRatio = doubleSupportSplitRatio;

      this.startOfSplineTime = startOfSplineTime;
      this.endOfSplineTime = endOfSplineTime;
      this.totalTrajectoryTime = totalTrajectoryTime;

      cubicProjectionMatrix = new CubicProjectionMatrix();
      cubicProjectionDerivativeMatrix = new CubicProjectionDerivativeMatrix();

      transferEntryCMPProjectionMatrix = new TransferEntryCMPProjectionMatrix(doubleSupportSplitRatio);
      swingEntryCMPProjectionMatrix = new SwingEntryCMPProjectionMatrix(doubleSupportSplitRatio, exitCMPRatio, startOfSplineTime);
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
         boolean useTwoCMPs, boolean isInTransfer, double omega0, boolean useInitialICP)
   {
      double positionMultiplier, velocityMultiplier;
      if (isInTransfer)
      {
         positionMultiplier = computeInTransfer(doubleSupportDurations, timeRemaining, useTwoCMPs, omega0, useInitialICP);
      }
      else
      {
         if (useTwoCMPs)
            positionMultiplier = computeSegmentedProjection(doubleSupportDurations, singleSupportDurations, timeRemaining, omega0, useInitialICP);
         else
            positionMultiplier = computeInSwingOneCMP();
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
            velocityMultiplier = computeInSwingOneCMPVelocity();
      }

      this.velocityMultiplier.set(velocityMultiplier);
   }

   private double computeInTransfer(ArrayList<DoubleYoVariable> doubleSupportDurations, double timeRemaining, boolean useTwoCMPs, double omega0, boolean useInitialICP)
   {
      transferEntryCMPProjectionMatrix.compute(doubleSupportDurations, useTwoCMPs, omega0, useInitialICP);

      double splineDuration = doubleSupportDurations.get(0).getDoubleValue();

      cubicProjectionDerivativeMatrix.setSegmentDuration(splineDuration);
      cubicProjectionDerivativeMatrix.update(timeRemaining);
      cubicProjectionMatrix.setSegmentDuration(splineDuration);
      cubicProjectionMatrix.update(timeRemaining);

      CommonOps.mult(cubicProjectionMatrix, transferEntryCMPProjectionMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeInSwingOneCMP()
   {
      return 0.0;
   }

   private double computeInTransferVelocity()
   {
      CommonOps.mult(cubicProjectionDerivativeMatrix, transferEntryCMPProjectionMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeInSwingOneCMPVelocity()
   {
      return 0.0;
   }

   private double computeSegmentedProjection(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeRemaining, double omega0, boolean useInitialICP)
   {
      double timeInState = totalTrajectoryTime.getDoubleValue() - timeRemaining;

      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeFirstSegmentProjection(doubleSupportDurations, singleSupportDurations, timeInState, omega0, useInitialICP);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeThirdSegmentProjection();
      else
         return computeSecondSegmentProjection(doubleSupportDurations, singleSupportDurations, timeRemaining, omega0, useInitialICP);
   }

   private double computeFirstSegmentProjection(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeInState, double omega0, boolean useInitialICP)
   {
      if (!useInitialICP)
      {
         double currentDoubleSupportDuration = doubleSupportDurations.get(0).getDoubleValue();
         double singleSupportDuration = singleSupportDurations.get(0).getDoubleValue();

         double stepDuration = currentDoubleSupportDuration + singleSupportDuration;

         double timeSpentOnEntryCMP = (1.0 - exitCMPRatio.getDoubleValue()) * stepDuration;

         double endOfDoubleSupportDuration = (1.0 - doubleSupportSplitRatio.getDoubleValue()) * currentDoubleSupportDuration;

         double entryRecursionTime = timeInState + endOfDoubleSupportDuration - timeSpentOnEntryCMP;
         double entryRecursion = Math.exp(omega0 * entryRecursionTime);

         double recursion = 1.0 - entryRecursion;

         return recursion;
      }
      else
      {
         return 1.0 - Math.exp(omega0 * timeInState);
      }
   }

   private double computeSecondSegmentProjection(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations,
         double timeRemaining, double omega0, boolean useInitialICP)
   {
      swingEntryCMPProjectionMatrix.compute(doubleSupportDurations, singleSupportDurations, omega0, useInitialICP);

      double lastSegmentDuration = totalTrajectoryTime.getDoubleValue() - endOfSplineTime.getDoubleValue();
      double timeRemainingInSpline = timeRemaining - lastSegmentDuration;
      double splineDuration = endOfSplineTime.getDoubleValue() - startOfSplineTime.getDoubleValue();

      cubicProjectionMatrix.setSegmentDuration(splineDuration);
      cubicProjectionMatrix.update(timeRemainingInSpline);

      cubicProjectionDerivativeMatrix.setSegmentDuration(splineDuration);
      cubicProjectionDerivativeMatrix.update(timeRemainingInSpline);

      CommonOps.mult(cubicProjectionMatrix, swingEntryCMPProjectionMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeThirdSegmentProjection()
   {
      return computeInSwingOneCMP();
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
      return omega0 * (positionMultiplier.getDoubleValue() - 1.0);
   }

   private double computeSecondSegmentVelocityProjection()
   {
      CommonOps.mult(cubicProjectionDerivativeMatrix, swingEntryCMPProjectionMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   private double computeThirdSegmentVelocityProjection(double omega0)
   {
      return omega0 * computeInSwingOneCMPVelocity();
   }
}
