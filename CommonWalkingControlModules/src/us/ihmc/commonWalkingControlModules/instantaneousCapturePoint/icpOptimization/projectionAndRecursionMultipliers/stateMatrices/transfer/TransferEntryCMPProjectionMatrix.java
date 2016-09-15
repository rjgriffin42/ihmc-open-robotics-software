package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.transfer;

import org.ejml.data.DenseMatrix64F;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.ArrayList;

public class TransferEntryCMPProjectionMatrix extends DenseMatrix64F
{
   private final DoubleYoVariable doubleSupportSplitRatio;

   public TransferEntryCMPProjectionMatrix(DoubleYoVariable doubleSupportSplitRatio)
   {
      super(4, 1);

      this.doubleSupportSplitRatio = doubleSupportSplitRatio;
   }

   public void reset()
   {
      zero();
   }

   public void compute(ArrayList<DoubleYoVariable> doubleSupportDurations, boolean useTwoCMPs, double omega0)
   {
      this.compute(doubleSupportDurations.get(0).getDoubleValue(), useTwoCMPs, omega0);
   }

   public void compute(double doubleSupportDuration, boolean useTwoCMPs, double omega0)
   {
      zero();

      if (useTwoCMPs)
      {
         double initialDoubleSupportDuration = doubleSupportSplitRatio.getDoubleValue() * doubleSupportDuration;
         double endOfDoubleSupportDuration = (1.0 - doubleSupportSplitRatio.getDoubleValue()) * doubleSupportDuration;

         double initialDoubleSupportProjection = Math.exp(-omega0 * initialDoubleSupportDuration);
         double endOfDoubleSupportProjection = Math.exp(-omega0 * endOfDoubleSupportDuration);

         double stepProjection = initialDoubleSupportProjection * (1.0 - endOfDoubleSupportProjection);

         set(0, 0, stepProjection);
         set(1, 0, omega0 * stepProjection);
         set(3, 0, -omega0);
      }
   }

}
