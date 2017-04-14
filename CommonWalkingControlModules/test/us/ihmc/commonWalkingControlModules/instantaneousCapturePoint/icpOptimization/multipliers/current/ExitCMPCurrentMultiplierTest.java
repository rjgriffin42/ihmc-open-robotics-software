package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.current;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Assert;
import org.junit.Test;

import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.CubicDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.CubicMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.swing.SwingExitCMPMatrix;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

public class ExitCMPCurrentMultiplierTest
{
   private static final double epsilon = 0.0001;

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationTwoCMPTransfer()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      double omega = 3.0;
      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitRatio = new DoubleYoVariable("transferSplitRatio" + i, registry);
         DoubleYoVariable swingSplitRatio = new DoubleYoVariable("swingSplitRatio" + i, registry);
         transferSplitRatio.setToNaN();
         swingSplitRatio.setToNaN();
         transferSplitFractions.add(transferSplitRatio);
         swingSplitFractions.add(swingSplitRatio);
      }

      ExitCMPCurrentMultiplier exitCMPCurrentMultiplier = new ExitCMPCurrentMultiplier(swingSplitFractions, startOfSplineTime, endOfSplineTime, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         double currentTransferRatio = 0.7 * random.nextDouble();
         double currentSwingRatio = 0.7 * random.nextDouble();
         double nextTransferRatio = 0.7 * random.nextDouble();
         transferSplitFractions.get(0).set(currentTransferRatio);
         swingSplitFractions.get(0).set(currentSwingRatio);
         transferSplitFractions.get(1).set(nextTransferRatio);

         double currentDoubleSupportDuration = 2.0 * random.nextDouble();
         double currentSingleSupportDuration = 5.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(currentDoubleSupportDuration);
         singleSupportDurations.get(0).set(currentSingleSupportDuration);
         doubleSupportDurations.get(1).set(2.0 * random.nextDouble());

         totalTrajectoryTime.set(currentSingleSupportDuration);

         boolean isInTransfer = true;
         boolean useTwoCMPs = true;

         double timeInCurrentState = random.nextDouble() * currentDoubleSupportDuration;

         exitCMPCurrentMultiplier.compute(singleSupportDurations, timeInCurrentState, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getVelocityMultiplier(), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getPositionMultiplier()));
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getVelocityMultiplier()));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationOneCMPTransfer()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      double omega = 3.0;
      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitRatio = new DoubleYoVariable("transferSplitRatio" + i, registry);
         transferSplitRatio.setToNaN();
         transferSplitFractions.add(transferSplitRatio);
      }

      ExitCMPCurrentMultiplier exitCMPCurrentMultiplier = new ExitCMPCurrentMultiplier(swingSplitFractions, startOfSplineTime, endOfSplineTime, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         double currentTransferRatio = 0.7 * random.nextDouble();
         double nextTransferRatio = 0.7 * random.nextDouble();
         transferSplitFractions.get(0).set(currentTransferRatio);
         transferSplitFractions.get(1).set(nextTransferRatio);

         double currentDoubleSupportDuration = 2.0 * random.nextDouble();
         double currentSingleSupportDuration = 5.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(currentDoubleSupportDuration);
         singleSupportDurations.get(0).set(currentSingleSupportDuration);
         doubleSupportDurations.get(1).set(2.0 * random.nextDouble());

         totalTrajectoryTime.set(currentSingleSupportDuration);

         boolean isInTransfer = true;
         boolean useTwoCMPs = false;

         double timeInCurrentState = random.nextDouble() * currentDoubleSupportDuration;

         exitCMPCurrentMultiplier.compute(singleSupportDurations, timeInCurrentState, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getVelocityMultiplier(), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getPositionMultiplier()));
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getVelocityMultiplier()));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationTwoCMPFirstSegment()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      double omega = 3.0;
      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitRatio = new DoubleYoVariable("transferSplitRatio" + i, registry);
         DoubleYoVariable swingSplitRatio = new DoubleYoVariable("swingSplitRatio" + i, registry);
         transferSplitRatio.setToNaN();
         swingSplitRatio.setToNaN();
         transferSplitFractions.add(transferSplitRatio);
         swingSplitFractions.add(swingSplitRatio);
      }

      ExitCMPCurrentMultiplier exitCMPCurrentMultiplier = new ExitCMPCurrentMultiplier(swingSplitFractions, startOfSplineTime, endOfSplineTime, registry);


      for (int iter = 0; iter < iters; iter++)
      {
         double currentTransferRatio = 0.7 * random.nextDouble();
         double currentSwingRatio = 0.7 * random.nextDouble();
         double nextTransferRatio = 0.7 * random.nextDouble();
         transferSplitFractions.get(0).set(currentTransferRatio);
         swingSplitFractions.get(0).set(currentSwingRatio);
         transferSplitFractions.get(1).set(nextTransferRatio);

         double currentDoubleSupportDuration = 2.0 * random.nextDouble();
         double currentSingleSupportDuration = 5.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(currentDoubleSupportDuration);
         singleSupportDurations.get(0).set(currentSingleSupportDuration);
         doubleSupportDurations.get(1).set(2.0 * random.nextDouble());

         double minimumSplineTime = Math.min(currentSingleSupportDuration, 0.5);
         double startOfSpline = 0.2 * random.nextDouble();
         double endOfSpline = currentSingleSupportDuration - 0.2 * random.nextDouble();
         if (minimumSplineTime > endOfSpline - startOfSpline)
            startOfSpline = 0.0;
         if (minimumSplineTime > endOfSpline - startOfSpline)
            endOfSpline = currentSingleSupportDuration;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(currentSingleSupportDuration);

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         double timeInCurrentState = random.nextDouble() * startOfSpline;

         exitCMPCurrentMultiplier.compute(singleSupportDurations, timeInCurrentState, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getVelocityMultiplier(), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getPositionMultiplier()));
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getVelocityMultiplier()));
      }
   }


   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationTwoCMPSecondSegment()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      double omega = 3.0;
      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitRatio = new DoubleYoVariable("transferSplitRatio" + i, registry);
         DoubleYoVariable swingSplitRatio = new DoubleYoVariable("swingSplitRatio" + i, registry);
         transferSplitRatio.setToNaN();
         swingSplitRatio.setToNaN();
         transferSplitFractions.add(transferSplitRatio);
         swingSplitFractions.add(swingSplitRatio);
      }

      ExitCMPCurrentMultiplier exitCMPCurrentMultiplier = new ExitCMPCurrentMultiplier(swingSplitFractions, startOfSplineTime, endOfSplineTime, registry);

      SwingExitCMPMatrix exitCMPMatrix = new SwingExitCMPMatrix(swingSplitFractions, endOfSplineTime);
      CubicMatrix cubicMatrix = new CubicMatrix();
      CubicDerivativeMatrix cubicDerivativeMatrix = new CubicDerivativeMatrix();
      DenseMatrix64F positionMatrixOut = new DenseMatrix64F(1, 1);
      DenseMatrix64F velocityMatrixOut = new DenseMatrix64F(1, 1);

      for (int iter = 0; iter < iters; iter++)
      {
         double currentTransferRatio = 0.7 * random.nextDouble();
         double currentSwingRatio = 0.7 * random.nextDouble();
         double nextTransferRatio = 0.7 * random.nextDouble();
         transferSplitFractions.get(0).set(currentTransferRatio);
         swingSplitFractions.get(0).set(currentSwingRatio);
         transferSplitFractions.get(1).set(nextTransferRatio);

         double currentDoubleSupportDuration = 2.0 * random.nextDouble();
         double currentSingleSupportDuration = 5.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(currentDoubleSupportDuration);
         singleSupportDurations.get(0).set(currentSingleSupportDuration);
         doubleSupportDurations.get(1).set(2.0 * random.nextDouble());

         double minimumSplineTime = Math.min(currentSingleSupportDuration, 0.5);
         double startOfSpline = 0.2 * random.nextDouble();
         double endOfSpline = currentSingleSupportDuration - 0.2 * random.nextDouble();
         if (minimumSplineTime > endOfSpline - startOfSpline)
            startOfSpline = 0.0;
         if (minimumSplineTime > endOfSpline - startOfSpline)
            endOfSpline = currentSingleSupportDuration;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(currentSingleSupportDuration);

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         double timeInCurrentState = random.nextDouble() * (endOfSpline - startOfSpline) + startOfSpline;

         double splineDuration = endOfSpline - startOfSpline;
         cubicMatrix.setSegmentDuration(splineDuration);
         cubicDerivativeMatrix.setSegmentDuration(splineDuration);

         cubicMatrix.update(timeInCurrentState - startOfSpline);
         cubicDerivativeMatrix.update(timeInCurrentState - startOfSpline);

         exitCMPMatrix.compute(singleSupportDurations, omega);
         CommonOps.mult(cubicMatrix, exitCMPMatrix, positionMatrixOut);
         CommonOps.mult(cubicDerivativeMatrix, exitCMPMatrix, velocityMatrixOut);

         exitCMPCurrentMultiplier.compute(singleSupportDurations, timeInCurrentState, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(positionMatrixOut.get(0, 0), exitCMPCurrentMultiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals(velocityMatrixOut.get(0, 0), exitCMPCurrentMultiplier.getVelocityMultiplier(), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getPositionMultiplier()));
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getVelocityMultiplier()));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationTwoCMPThirdSegment()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      double omega = 3.0;
      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitRatio = new DoubleYoVariable("transferSplitRatio" + i, registry);
         DoubleYoVariable swingSplitRatio = new DoubleYoVariable("swingSplitRatio" + i, registry);
         transferSplitRatio.setToNaN();
         swingSplitRatio.setToNaN();
         transferSplitFractions.add(transferSplitRatio);
         swingSplitFractions.add(swingSplitRatio);
      }

      ExitCMPCurrentMultiplier exitCMPCurrentMultiplier = new ExitCMPCurrentMultiplier(swingSplitFractions, startOfSplineTime, endOfSplineTime, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         double currentTransferRatio = 0.7 * random.nextDouble();
         double currentSwingRatio = 0.7 * random.nextDouble();
         double nextTransferRatio = 0.7 * random.nextDouble();
         transferSplitFractions.get(0).set(currentTransferRatio);
         swingSplitFractions.get(0).set(currentSwingRatio);
         transferSplitFractions.get(1).set(nextTransferRatio);

         double currentDoubleSupportDuration = 2.0 * random.nextDouble();
         double currentSingleSupportDuration = 5.0 * random.nextDouble();
         double nextDoubleSupportDuration = 2.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(currentDoubleSupportDuration);
         singleSupportDurations.get(0).set(currentSingleSupportDuration);
         doubleSupportDurations.get(1).set(nextDoubleSupportDuration);

         double minimumSplineTime = Math.min(currentSingleSupportDuration, 0.5);
         double startOfSpline = 0.2 * random.nextDouble();
         double endOfSpline = currentSingleSupportDuration - 0.2 * random.nextDouble();
         if (minimumSplineTime > endOfSpline - startOfSpline)
            startOfSpline = 0.0;
         if (minimumSplineTime > endOfSpline - startOfSpline)
            endOfSpline = currentSingleSupportDuration;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(currentSingleSupportDuration);

         boolean isInTransfer = false;
         boolean useTwoCMPs = true;

         double timeInCurrentState = random.nextDouble() * (currentSingleSupportDuration - endOfSpline) + endOfSpline;

         double upcomingInitialDoubleSupportDuration = nextTransferRatio * nextDoubleSupportDuration;
         double timeSpentOnExitCMP = (1.0 - currentSwingRatio) * currentSingleSupportDuration + upcomingInitialDoubleSupportDuration;

         exitCMPCurrentMultiplier.compute(singleSupportDurations, timeInCurrentState, useTwoCMPs, isInTransfer, omega);

         double projectionTime = timeInCurrentState - currentSingleSupportDuration + timeSpentOnExitCMP - upcomingInitialDoubleSupportDuration;
         double projection = Math.exp(omega * projectionTime);

         Assert.assertEquals(1.0 - projection, exitCMPCurrentMultiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals(-omega * projection, exitCMPCurrentMultiplier.getVelocityMultiplier(), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getPositionMultiplier()));
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getVelocityMultiplier()));
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationOneCMPSwing()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      double omega = 3.0;
      int maxSteps = 5;
      int iters = 100;

      Random random = new Random();
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();

      for (int i = 0 ; i < maxSteps + 1; i++)
      {
         DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration" + i, registry);
         DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration" + i, registry);
         doubleSupportDuration.setToNaN();
         singleSupportDuration.setToNaN();
         doubleSupportDurations.add(doubleSupportDuration);
         singleSupportDurations.add(singleSupportDuration);

         DoubleYoVariable transferSplitRatio = new DoubleYoVariable("transferSplitRatio" + i, registry);
         transferSplitRatio.setToNaN();
         transferSplitFractions.add(transferSplitRatio);
      }

      ExitCMPCurrentMultiplier exitCMPCurrentMultiplier = new ExitCMPCurrentMultiplier(swingSplitFractions, startOfSplineTime, endOfSplineTime, registry);

      for (int iter = 0; iter < iters; iter++)
      {
         double currentTransferRatio = 0.7 * random.nextDouble();
         double nextTransferRatio = 0.7 * random.nextDouble();
         transferSplitFractions.get(0).set(currentTransferRatio);
         transferSplitFractions.get(1).set(nextTransferRatio);

         double currentDoubleSupportDuration = 2.0 * random.nextDouble();
         double currentSingleSupportDuration = 5.0 * random.nextDouble();
         double nextDoubleSupportDuration = 2.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(currentDoubleSupportDuration);
         singleSupportDurations.get(0).set(currentSingleSupportDuration);
         doubleSupportDurations.get(1).set(nextDoubleSupportDuration);

         double minimumSplineTime = Math.min(currentSingleSupportDuration, 0.5);
         double startOfSpline = 0.2 * random.nextDouble();
         double endOfSpline = currentSingleSupportDuration - 0.2 * random.nextDouble();
         if (minimumSplineTime > endOfSpline - startOfSpline)
            startOfSpline = 0.0;
         if (minimumSplineTime > endOfSpline - startOfSpline)
            endOfSpline = currentSingleSupportDuration;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(currentSingleSupportDuration);

         boolean isInTransfer = false;
         boolean useTwoCMPs = false;

         double timeInCurrentState = random.nextDouble() * (currentSingleSupportDuration - endOfSpline) + endOfSpline;

         exitCMPCurrentMultiplier.compute(singleSupportDurations, timeInCurrentState, useTwoCMPs, isInTransfer, omega);

         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals(0.0, exitCMPCurrentMultiplier.getVelocityMultiplier(), epsilon);
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getPositionMultiplier()));
         Assert.assertFalse(Double.isNaN(exitCMPCurrentMultiplier.getVelocityMultiplier()));
      }
   }
}
