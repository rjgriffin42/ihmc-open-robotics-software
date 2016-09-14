package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Assert;
import org.junit.Test;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.CurrentStateProjectionMultiplier;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicProjectionDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation.CubicProjectionMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.swing.SwingStateEndRecursionMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.transfer.TransferStateEndRecursionMatrix;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

import java.util.ArrayList;

import static com.badlogic.gdx.math.MathUtils.random;

public class CurrentStateProjectionMultiplierTest
{
   private static final double epsilon = 0.0001;

   @DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testOneCMPSS()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      DoubleYoVariable omega = new DoubleYoVariable("omega", registry);
      DoubleYoVariable doubleSupportSplitRatio = new DoubleYoVariable("doubleSupportSplitRatio", registry);
      DoubleYoVariable exitCMPDurationInPercentOfSteptime = new DoubleYoVariable("exitCMPDurationInPercentOfSteptime", registry);
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      DoubleYoVariable currentDoubleSupportDuration = new DoubleYoVariable("currentDoubleSupportDuration", registry);
      DoubleYoVariable upcomingDoubleSupportDuration = new DoubleYoVariable("upcomingDoubleSupportDuration", registry);

      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(currentDoubleSupportDuration);
      doubleSupportDurations.add(upcomingDoubleSupportDuration);

      DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", registry);
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(singleSupportDuration);

      CurrentStateProjectionMultiplier multiplier = new CurrentStateProjectionMultiplier(registry, omega, doubleSupportSplitRatio, startOfSplineTime,
                                                                                         endOfSplineTime, totalTrajectoryTime);

      int iters = 100;
      for (int i = 0; i < iters; i++)
      {
         double omega0 = 3.0;
         double splitRatio = 0.7 * random.nextDouble();
         double exitRatio = 0.7 * random.nextDouble();

         omega.set(omega0);
         doubleSupportSplitRatio.set(splitRatio);
         exitCMPDurationInPercentOfSteptime.set(exitRatio);

         double currentDoubleSupport = 2.0 * random.nextDouble();
         double upcomingDoubleSupport = 2.0 * random.nextDouble();
         double singleSupport = 5.0 * random.nextDouble();

         double timeRemaining = random.nextDouble() * singleSupport;

         currentDoubleSupportDuration.set(currentDoubleSupport);
         upcomingDoubleSupportDuration.set(upcomingDoubleSupport);
         singleSupportDuration.set(singleSupport);

         double alpha1 = random.nextDouble();
         double alpha2 = random.nextDouble();
         double startOfSpline = Math.min(alpha1, alpha2) * singleSupport;
         double endOfSpline = Math.max(alpha1, alpha2) * singleSupport;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupport);

         double recursionMultiplier = Math.exp(-omega0 * timeRemaining);
         double projectionMultiplier = 1.0 / recursionMultiplier;

         double velocityMultiplier = omega0 * recursionMultiplier;

         multiplier.compute(doubleSupportDurations, singleSupportDurations, timeRemaining, false, false);

         Assert.assertEquals("", projectionMultiplier, multiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals("", velocityMultiplier, multiplier.getVelocityMultiplier(), epsilon);
      }
   }

   @DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testOneCMPTransfer()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      DoubleYoVariable omega = new DoubleYoVariable("omega", registry);
      DoubleYoVariable doubleSupportSplitRatio = new DoubleYoVariable("doubleSupportSplitRatio", registry);
      DoubleYoVariable exitCMPDurationInPercentOfSteptime = new DoubleYoVariable("exitCMPDurationInPercentOfSteptime", registry);
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      DoubleYoVariable currentDoubleSupportDuration = new DoubleYoVariable("currentDoubleSupportDuration", registry);
      DoubleYoVariable upcomingDoubleSupportDuration = new DoubleYoVariable("upcomingDoubleSupportDuration", registry);

      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(currentDoubleSupportDuration);
      doubleSupportDurations.add(upcomingDoubleSupportDuration);

      DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", registry);
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(singleSupportDuration);

      CurrentStateProjectionMultiplier multiplier = new CurrentStateProjectionMultiplier(registry, omega, doubleSupportSplitRatio, startOfSplineTime,
                                                                                         endOfSplineTime, totalTrajectoryTime);
      TransferStateEndRecursionMatrix transferStateEndRecursionMatrix = new TransferStateEndRecursionMatrix(omega);

      CubicProjectionMatrix cubicProjectionMatrix = new CubicProjectionMatrix();
      CubicProjectionDerivativeMatrix cubicProjectionDerivativeMatrix = new CubicProjectionDerivativeMatrix();

      int iters = 100;
      for (int i = 0; i < iters; i++)
      {
         double omega0 = 3.0;
         double splitRatio = 0.7 * random.nextDouble();
         double exitRatio = 0.7 * random.nextDouble();

         omega.set(omega0);
         doubleSupportSplitRatio.set(splitRatio);
         exitCMPDurationInPercentOfSteptime.set(exitRatio);

         double currentDoubleSupport = 2.0 * random.nextDouble();
         double upcomingDoubleSupport = 2.0 * random.nextDouble();
         double singleSupport = 5.0 * random.nextDouble();

         currentDoubleSupportDuration.set(currentDoubleSupport);
         upcomingDoubleSupportDuration.set(upcomingDoubleSupport);
         singleSupportDuration.set(singleSupport);

         double timeRemaining = random.nextDouble() * currentDoubleSupport;

         double alpha1 = random.nextDouble();
         double alpha2 = random.nextDouble();
         double startOfSpline = Math.min(alpha1, alpha2) * singleSupport;
         double endOfSpline = Math.max(alpha1, alpha2) * singleSupport;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupport);

         transferStateEndRecursionMatrix.compute(currentDoubleSupport);

         cubicProjectionDerivativeMatrix.setSegmentDuration(currentDoubleSupport);
         cubicProjectionDerivativeMatrix.update(timeRemaining);
         cubicProjectionMatrix.setSegmentDuration(currentDoubleSupport);
         cubicProjectionMatrix.update(timeRemaining);

         DenseMatrix64F positionMatrixOut = new DenseMatrix64F(1, 1);
         DenseMatrix64F velocityMatrixOut = new DenseMatrix64F(1, 1);
         CommonOps.mult(cubicProjectionMatrix, transferStateEndRecursionMatrix, positionMatrixOut);
         CommonOps.mult(cubicProjectionDerivativeMatrix, transferStateEndRecursionMatrix, velocityMatrixOut);

         multiplier.compute(doubleSupportDurations, singleSupportDurations, timeRemaining, false, true);

         Assert.assertEquals("", 1.0 / positionMatrixOut.get(0, 0), multiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals("", velocityMatrixOut.get(0, 0), multiplier.getVelocityMultiplier(), epsilon);
      }
   }

   @DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testTwoCMPSSFirstSegment()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      DoubleYoVariable omega = new DoubleYoVariable("omega", registry);
      DoubleYoVariable doubleSupportSplitRatio = new DoubleYoVariable("doubleSupportSplitRatio", registry);
      DoubleYoVariable exitCMPDurationInPercentOfSteptime = new DoubleYoVariable("exitCMPDurationInPercentOfSteptime", registry);
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      DoubleYoVariable currentDoubleSupportDuration = new DoubleYoVariable("currentDoubleSupportDuration", registry);
      DoubleYoVariable upcomingDoubleSupportDuration = new DoubleYoVariable("upcomingDoubleSupportDuration", registry);

      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(currentDoubleSupportDuration);
      doubleSupportDurations.add(upcomingDoubleSupportDuration);

      DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", registry);
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(singleSupportDuration);

      CurrentStateProjectionMultiplier multiplier = new CurrentStateProjectionMultiplier(registry, omega, doubleSupportSplitRatio, startOfSplineTime,
                                                                                         endOfSplineTime, totalTrajectoryTime);

      int iters = 100;
      for (int i = 0; i < iters; i++)
      {
         double omega0 = 3.0;
         double splitRatio = 0.7 * random.nextDouble();
         double exitRatio = 0.7 * random.nextDouble();

         omega.set(omega0);
         doubleSupportSplitRatio.set(splitRatio);
         exitCMPDurationInPercentOfSteptime.set(exitRatio);

         double currentDoubleSupport = 2.0 * random.nextDouble();
         double upcomingDoubleSupport = 2.0 * random.nextDouble();
         double singleSupport = 5.0 * random.nextDouble();

         currentDoubleSupportDuration.set(currentDoubleSupport);
         upcomingDoubleSupportDuration.set(upcomingDoubleSupport);
         singleSupportDuration.set(singleSupport);

         double alpha1 = random.nextDouble();
         double alpha2 = random.nextDouble();
         double startOfSpline = Math.min(alpha1, alpha2) * singleSupport;
         double endOfSpline = Math.max(alpha1, alpha2) * singleSupport;

         double timeInCurrentState = random.nextDouble() * startOfSpline;
         double timeRemaining = singleSupport - timeInCurrentState;


         double stepDuration = currentDoubleSupport + singleSupport;
         double upcomingInitialDoubleSupport = splitRatio * upcomingDoubleSupport;
         double endOfDoubleSupport = (1.0 - splitRatio) * currentDoubleSupport;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupport);

         multiplier.compute(doubleSupportDurations, singleSupportDurations, timeRemaining, true, false);

         double recursionTime = timeInCurrentState + upcomingInitialDoubleSupport + endOfDoubleSupport - stepDuration;
         double recursion = Math.exp(omega0 * recursionTime);
         double velocityRecursion = omega0 * Math.exp(omega0 * recursionTime);

         Assert.assertEquals("", 1.0 / recursion, multiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals("", velocityRecursion, multiplier.getVelocityMultiplier(), epsilon);
      }
   }

   @DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testTwoCMPSSThirdSegment()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      DoubleYoVariable omega = new DoubleYoVariable("omega", registry);
      DoubleYoVariable doubleSupportSplitRatio = new DoubleYoVariable("doubleSupportSplitRatio", registry);
      DoubleYoVariable exitCMPDurationInPercentOfSteptime = new DoubleYoVariable("exitCMPDurationInPercentOfSteptime", registry);
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      DoubleYoVariable currentDoubleSupportDuration = new DoubleYoVariable("currentDoubleSupportDuration", registry);
      DoubleYoVariable upcomingDoubleSupportDuration = new DoubleYoVariable("upcomingDoubleSupportDuration", registry);

      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(currentDoubleSupportDuration);
      doubleSupportDurations.add(upcomingDoubleSupportDuration);

      DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", registry);
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(singleSupportDuration);

      CurrentStateProjectionMultiplier multiplier = new CurrentStateProjectionMultiplier(registry, omega, doubleSupportSplitRatio, startOfSplineTime,
                                                                                         endOfSplineTime, totalTrajectoryTime);

      int iters = 100;
      for (int i = 0; i < iters; i++)
      {
         double omega0 = 3.0;
         double splitRatio = 0.7 * random.nextDouble();
         double exitRatio = 0.7 * random.nextDouble();

         omega.set(omega0);
         doubleSupportSplitRatio.set(splitRatio);
         exitCMPDurationInPercentOfSteptime.set(exitRatio);

         double currentDoubleSupport = 2.0 * random.nextDouble();
         double upcomingDoubleSupport = 2.0 * random.nextDouble();
         double singleSupport = 5.0 * random.nextDouble();

         currentDoubleSupportDuration.set(currentDoubleSupport);
         upcomingDoubleSupportDuration.set(upcomingDoubleSupport);
         singleSupportDuration.set(singleSupport);

         double alpha1 = random.nextDouble();
         double alpha2 = random.nextDouble();
         double startOfSpline = Math.min(alpha1, alpha2) * singleSupport;
         double endOfSpline = Math.max(alpha1, alpha2) * singleSupport;

         double thirdSegmentTime = singleSupport - endOfSpline;

         double timeInCurrentState = random.nextDouble() * thirdSegmentTime + endOfSpline;
         double timeRemaining = singleSupport - timeInCurrentState;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupport);

         multiplier.compute(doubleSupportDurations, singleSupportDurations, timeRemaining, true, false);

         double recursion = Math.exp(-omega0 * timeRemaining);
         double velocityRecursion = omega0 * Math.exp(-omega0 * timeRemaining);

         Assert.assertEquals("", 1.0 / recursion, multiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals("", velocityRecursion, multiplier.getVelocityMultiplier(), epsilon);
      }
   }

   @DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testTwoCMPSSSecondSegment()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      DoubleYoVariable omega = new DoubleYoVariable("omega", registry);
      DoubleYoVariable doubleSupportSplitRatio = new DoubleYoVariable("doubleSupportSplitRatio", registry);
      DoubleYoVariable exitCMPDurationInPercentOfSteptime = new DoubleYoVariable("exitCMPDurationInPercentOfSteptime", registry);
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      DoubleYoVariable currentDoubleSupportDuration = new DoubleYoVariable("currentDoubleSupportDuration", registry);
      DoubleYoVariable upcomingDoubleSupportDuration = new DoubleYoVariable("upcomingDoubleSupportDuration", registry);

      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(currentDoubleSupportDuration);
      doubleSupportDurations.add(upcomingDoubleSupportDuration);

      DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", registry);
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(singleSupportDuration);

      CurrentStateProjectionMultiplier multiplier = new CurrentStateProjectionMultiplier(registry, omega, doubleSupportSplitRatio, startOfSplineTime, endOfSplineTime, totalTrajectoryTime);

      SwingStateEndRecursionMatrix swingStateEndRecursionMatrix = new SwingStateEndRecursionMatrix(omega, doubleSupportSplitRatio, startOfSplineTime,
                                                                                                    endOfSplineTime, totalTrajectoryTime);
      CubicProjectionMatrix cubicProjectionMatrix = new CubicProjectionMatrix();
      CubicProjectionDerivativeMatrix cubicProjectionDerivativeMatrix = new CubicProjectionDerivativeMatrix();
      int iters = 100;
      for (int i = 0; i < iters; i++)
      {
         double omega0 = 3.0;
         double splitRatio = 0.7 * random.nextDouble();
         double exitRatio = 0.7 * random.nextDouble();

         omega.set(omega0);
         doubleSupportSplitRatio.set(splitRatio);
         exitCMPDurationInPercentOfSteptime.set(exitRatio);

         double currentDoubleSupport = 2.0 * random.nextDouble();
         double upcomingDoubleSupport = 2.0 * random.nextDouble();
         double singleSupport = 5.0 * random.nextDouble();

         currentDoubleSupportDuration.set(currentDoubleSupport);
         upcomingDoubleSupportDuration.set(upcomingDoubleSupport);
         singleSupportDuration.set(singleSupport);

         double alpha1 = random.nextDouble();
         double alpha2 = random.nextDouble();
         double startOfSpline = Math.min(alpha1, alpha2) * singleSupport;
         double endOfSpline = Math.max(alpha1, alpha2) * singleSupport;

         double segmentLength = endOfSpline - startOfSpline;
         double timeInSegment = random.nextDouble() * segmentLength;
         double timeRemainingInSegment = segmentLength - timeInSegment;

         double timeInCurrentState = timeInSegment + startOfSpline;
         double timeRemaining = singleSupport - timeInCurrentState;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupport);

         swingStateEndRecursionMatrix.compute(doubleSupportDurations, singleSupportDurations);

         cubicProjectionDerivativeMatrix.setSegmentDuration(segmentLength);
         cubicProjectionDerivativeMatrix.update(timeRemainingInSegment);
         cubicProjectionMatrix.setSegmentDuration(segmentLength);
         cubicProjectionMatrix.update(timeRemainingInSegment);

         DenseMatrix64F positionMatrixOut = new DenseMatrix64F(1, 1);
         DenseMatrix64F velocityMatrixOut = new DenseMatrix64F(1, 1);
         CommonOps.mult(cubicProjectionMatrix, swingStateEndRecursionMatrix, positionMatrixOut);
         CommonOps.mult(cubicProjectionDerivativeMatrix, swingStateEndRecursionMatrix, velocityMatrixOut);

         multiplier.compute(doubleSupportDurations, singleSupportDurations, timeRemaining, true, false);

         Assert.assertEquals("", 1.0 / positionMatrixOut.get(0, 0), multiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals("", velocityMatrixOut.get(0, 0), multiplier.getVelocityMultiplier(), epsilon);
      }
   }

   @DeployableTestMethod(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testTwoCMPTransfer()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      DoubleYoVariable omega = new DoubleYoVariable("omega", registry);
      DoubleYoVariable doubleSupportSplitRatio = new DoubleYoVariable("doubleSupportSplitRatio", registry);
      DoubleYoVariable exitCMPDurationInPercentOfSteptime = new DoubleYoVariable("exitCMPDurationInPercentOfSteptime", registry);
      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      DoubleYoVariable currentDoubleSupportDuration = new DoubleYoVariable("currentDoubleSupportDuration", registry);
      DoubleYoVariable upcomingDoubleSupportDuration = new DoubleYoVariable("upcomingDoubleSupportDuration", registry);

      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(currentDoubleSupportDuration);
      doubleSupportDurations.add(upcomingDoubleSupportDuration);

      DoubleYoVariable singleSupportDuration = new DoubleYoVariable("singleSupportDuration", registry);
      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(singleSupportDuration);

      CurrentStateProjectionMultiplier multiplier = new CurrentStateProjectionMultiplier(registry, omega, doubleSupportSplitRatio, startOfSplineTime, endOfSplineTime, totalTrajectoryTime);
      TransferStateEndRecursionMatrix transferStateEndRecursionMatrix = new TransferStateEndRecursionMatrix(omega);

      CubicProjectionMatrix cubicProjectionMatrix = new CubicProjectionMatrix();
      CubicProjectionDerivativeMatrix cubicProjectionDerivativeMatrix = new CubicProjectionDerivativeMatrix();

      int iters = 100;
      for (int i = 0; i < iters; i++)
      {
         double omega0 = 3.0;
         double splitRatio = 0.7 * random.nextDouble();
         double exitRatio = 0.7 * random.nextDouble();

         omega.set(omega0);
         doubleSupportSplitRatio.set(splitRatio);
         exitCMPDurationInPercentOfSteptime.set(exitRatio);

         double currentDoubleSupport = 2.0 * random.nextDouble();
         double upcomingDoubleSupport = 2.0 * random.nextDouble();
         double singleSupport = 5.0 * random.nextDouble();

         currentDoubleSupportDuration.set(currentDoubleSupport);
         upcomingDoubleSupportDuration.set(upcomingDoubleSupport);
         singleSupportDuration.set(singleSupport);

         double timeRemaining = random.nextDouble() * currentDoubleSupport;

         double alpha1 = random.nextDouble();
         double alpha2 = random.nextDouble();
         double startOfSpline = Math.min(alpha1, alpha2) * singleSupport;
         double endOfSpline = Math.max(alpha1, alpha2) * singleSupport;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupport);

         transferStateEndRecursionMatrix.compute(currentDoubleSupport);

         cubicProjectionDerivativeMatrix.setSegmentDuration(currentDoubleSupport);
         cubicProjectionDerivativeMatrix.update(timeRemaining);
         cubicProjectionMatrix.setSegmentDuration(currentDoubleSupport);
         cubicProjectionMatrix.update(timeRemaining);

         DenseMatrix64F positionMatrixOut = new DenseMatrix64F(1, 1);
         DenseMatrix64F velocityMatrixOut = new DenseMatrix64F(1, 1);
         CommonOps.mult(cubicProjectionMatrix, transferStateEndRecursionMatrix, positionMatrixOut);
         CommonOps.mult(cubicProjectionDerivativeMatrix, transferStateEndRecursionMatrix, velocityMatrixOut);

         multiplier.compute(doubleSupportDurations, singleSupportDurations, timeRemaining, true, true);

         Assert.assertEquals("", 1.0 / positionMatrixOut.get(0, 0), multiplier.getPositionMultiplier(), epsilon);
         Assert.assertEquals("", velocityMatrixOut.get(0, 0), multiplier.getVelocityMultiplier(), epsilon);
      }
   }
}
