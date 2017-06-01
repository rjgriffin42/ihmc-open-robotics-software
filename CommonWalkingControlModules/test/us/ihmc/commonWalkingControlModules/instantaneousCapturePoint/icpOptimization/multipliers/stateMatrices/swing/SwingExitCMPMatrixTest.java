package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.swing;

import org.ejml.data.DenseMatrix64F;
import org.junit.Assert;
import org.junit.Test;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.testing.JUnitTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SwingExitCMPMatrixTest
{
   private static final double epsilon = 0.00001;

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCreationSize()
   {
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();
      DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction", registry);
      swingSplitFractions.add(swingSplitFraction);

      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      DoubleYoVariable transferSplitFraction = new DoubleYoVariable("transferSplitFraction", registry);
      transferSplitFractions.add(transferSplitFraction);

      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);

      SwingExitCMPMatrix swingExitCMPMatrix = new SwingExitCMPMatrix(swingSplitFractions, transferSplitFractions, startOfSplineTime, endOfSplineTime, false);

      Assert.assertEquals("", 4, swingExitCMPMatrix.numRows);
      Assert.assertEquals("", 1, swingExitCMPMatrix.numCols);
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculation()
   {
      DenseMatrix64F shouldBe = new DenseMatrix64F(4, 1);
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      Random random = new Random();
      int iters = 100;
      double omega0 = 3.0;

      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(new DoubleYoVariable("singleSupportDuration", registry));
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(new DoubleYoVariable("doubleSupportDuration1", registry));
      doubleSupportDurations.add(new DoubleYoVariable("doubleSupportDuration2", registry));

      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();
      DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction", registry);
      swingSplitFractions.add(swingSplitFraction);

      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      DoubleYoVariable transferSplitFraction1 = new DoubleYoVariable("transferSplitFraction1", registry);
      DoubleYoVariable transferSplitFraction2 = new DoubleYoVariable("transferSplitFraction2", registry);
      transferSplitFractions.add(transferSplitFraction1);
      transferSplitFractions.add(transferSplitFraction2);

      SwingExitCMPMatrix swingExitCMPMatrix = new SwingExitCMPMatrix(swingSplitFractions, transferSplitFractions, startOfSplineTime, endOfSplineTime, false);

      for (int i = 0; i < iters; i++)
      {
         double splitRatio = 0.7 * random.nextDouble();
         swingSplitFraction.set(splitRatio);

         double transferRatio1 = 0.7 * random.nextDouble();
         double transferRatio2 = 0.7 * random.nextDouble();
         transferSplitFraction1.set(transferRatio1);
         transferSplitFraction2.set(transferRatio2);

         double singleSupportDuration = 5.0 * random.nextDouble();
         singleSupportDurations.get(0).set(singleSupportDuration);

         double doubleSupportDuration = 5.0 * random.nextDouble();
         double nextDoubleSupportDuration = 5.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(doubleSupportDuration);
         doubleSupportDurations.get(1).set(nextDoubleSupportDuration);

         double minimumSplineTime = Math.min(singleSupportDuration, 0.5);
         double startOfSpline = 0.2 * random.nextDouble();
         double endOfSpline = singleSupportDuration - 0.2 * random.nextDouble();
         if (minimumSplineTime > endOfSpline - startOfSpline)
            startOfSpline = 0.0;
         if (minimumSplineTime > endOfSpline - startOfSpline)
            endOfSpline = singleSupportDuration;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupportDuration);

         double currentSwingOnEntry = splitRatio * singleSupportDuration;
         double currentSwingOnExit = (1.0 - splitRatio) * singleSupportDuration;
         double nextTransferOnExit = transferRatio2 * nextDoubleSupportDuration;
         double timeOnExit = currentSwingOnExit + nextTransferOnExit;
         double currentSplineOnExit = endOfSpline - currentSwingOnEntry;

         double projectionTime = currentSplineOnExit - timeOnExit;
         double projection = Math.exp(omega0 * projectionTime);

         swingExitCMPMatrix.compute(singleSupportDurations, doubleSupportDurations, omega0);

         shouldBe.zero();
         shouldBe.set(2, 0, 1.0 - projection);
         shouldBe.set(3, 0, -omega0 * projection);

         JUnitTools.assertMatrixEquals(shouldBe, swingExitCMPMatrix, epsilon);

         swingExitCMPMatrix.reset();
         swingExitCMPMatrix.compute(singleSupportDurations, doubleSupportDurations, omega0);
         JUnitTools.assertMatrixEquals(shouldBe, swingExitCMPMatrix, epsilon);

         shouldBe.zero();
         swingExitCMPMatrix.reset();
         JUnitTools.assertMatrixEquals(shouldBe, swingExitCMPMatrix, epsilon);
      }
   }

   @ContinuousIntegrationTest(estimatedDuration = 1.0)
   @Test(timeout = 21000)
   public void testCalculationWithBlending()
   {
      DenseMatrix64F shouldBe = new DenseMatrix64F(4, 1);
      YoVariableRegistry registry = new YoVariableRegistry("registry");

      Random random = new Random();
      int iters = 100;
      double omega0 = 3.0;

      DoubleYoVariable startOfSplineTime = new DoubleYoVariable("startOfSplineTime", registry);
      DoubleYoVariable endOfSplineTime = new DoubleYoVariable("endOfSplineTime", registry);
      DoubleYoVariable totalTrajectoryTime = new DoubleYoVariable("totalTrajectoryTime", registry);

      ArrayList<DoubleYoVariable> singleSupportDurations = new ArrayList<>();
      singleSupportDurations.add(new DoubleYoVariable("singleSupportDuration", registry));
      ArrayList<DoubleYoVariable> doubleSupportDurations = new ArrayList<>();
      doubleSupportDurations.add(new DoubleYoVariable("doubleSupportDuration1", registry));
      doubleSupportDurations.add(new DoubleYoVariable("doubleSupportDuration2", registry));

      List<DoubleYoVariable> swingSplitFractions = new ArrayList<>();
      DoubleYoVariable swingSplitFraction = new DoubleYoVariable("swingSplitFraction", registry);
      swingSplitFractions.add(swingSplitFraction);

      List<DoubleYoVariable> transferSplitFractions = new ArrayList<>();
      DoubleYoVariable transferSplitFraction1 = new DoubleYoVariable("transferSplitFraction1", registry);
      DoubleYoVariable transferSplitFraction2 = new DoubleYoVariable("transferSplitFraction2", registry);
      transferSplitFractions.add(transferSplitFraction1);
      transferSplitFractions.add(transferSplitFraction2);

      SwingExitCMPMatrix swingExitCMPMatrix = new SwingExitCMPMatrix(swingSplitFractions, transferSplitFractions, startOfSplineTime, endOfSplineTime, true);

      for (int i = 0; i < iters; i++)
      {
         double splitRatio = 0.7 * random.nextDouble();
         swingSplitFraction.set(splitRatio);

         double transferRatio1 = 0.7 * random.nextDouble();
         double transferRatio2 = 0.7 * random.nextDouble();
         transferSplitFraction1.set(transferRatio1);
         transferSplitFraction2.set(transferRatio2);

         double singleSupportDuration = 5.0 * random.nextDouble();
         singleSupportDurations.get(0).set(singleSupportDuration);

         double doubleSupportDuration = 5.0 * random.nextDouble();
         double nextDoubleSupportDuration = 5.0 * random.nextDouble();
         doubleSupportDurations.get(0).set(doubleSupportDuration);
         doubleSupportDurations.get(1).set(nextDoubleSupportDuration);

         double minimumSplineTime = Math.min(singleSupportDuration, 0.5);
         double startOfSpline = 0.2 * random.nextDouble();
         double endOfSpline = singleSupportDuration - 0.2 * random.nextDouble();
         if (minimumSplineTime > endOfSpline - startOfSpline)
            startOfSpline = 0.0;
         if (minimumSplineTime > endOfSpline - startOfSpline)
            endOfSpline = singleSupportDuration;

         startOfSplineTime.set(startOfSpline);
         endOfSplineTime.set(endOfSpline);
         totalTrajectoryTime.set(singleSupportDuration);

         double currentSwingOnEntry = splitRatio * singleSupportDuration;
         double currentSwingOnExit = (1.0 - splitRatio) * singleSupportDuration;
         double nextTransferOnExit = transferRatio2 * nextDoubleSupportDuration;
         double timeOnExit = currentSwingOnExit + nextTransferOnExit;
         double currentSplineOnExit = endOfSpline - currentSwingOnEntry;
         double currentSplineOnEntry = currentSwingOnEntry - startOfSpline;

         double projectionTime = currentSplineOnExit - timeOnExit;
         double projection = Math.exp(omega0 * projectionTime);

         double initialProjection = Math.exp(-omega0 * currentSplineOnEntry) * (1.0 - Math.exp(-omega0 * timeOnExit));

         swingExitCMPMatrix.compute(singleSupportDurations, doubleSupportDurations, omega0);

         shouldBe.zero();
         shouldBe.set(0, 0, initialProjection);
         shouldBe.set(1, 0, omega0 * initialProjection);
         shouldBe.set(2, 0, 1.0 - projection);
         shouldBe.set(3, 0, -omega0 * projection);

         JUnitTools.assertMatrixEquals(shouldBe, swingExitCMPMatrix, epsilon);

         swingExitCMPMatrix.reset();
         swingExitCMPMatrix.compute(singleSupportDurations, doubleSupportDurations, omega0);
         JUnitTools.assertMatrixEquals(shouldBe, swingExitCMPMatrix, epsilon);

         shouldBe.zero();
         swingExitCMPMatrix.reset();
         JUnitTools.assertMatrixEquals(shouldBe, swingExitCMPMatrix, epsilon);
      }
   }
}
