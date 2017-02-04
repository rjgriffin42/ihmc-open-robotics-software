package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.transfer;

import org.ejml.data.DenseMatrix64F;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.ArrayList;

public class NewTransferEntryCMPMatrix extends DenseMatrix64F
{
   private final DoubleYoVariable doubleSupportSplitRatio;

   public NewTransferEntryCMPMatrix(DoubleYoVariable doubleSupportSplitRatio)
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
         double endOfDoubleSupportDuration = (1.0 - doubleSupportSplitRatio.getDoubleValue()) * doubleSupportDuration;

         double endOfDoubleSupportProjection = Math.exp(omega0 * endOfDoubleSupportDuration);

         double stepProjection = (1.0 - endOfDoubleSupportProjection);

         set(3, 0, stepProjection);
         set(4, 0, -omega0 * endOfDoubleSupportProjection);
      }
      else
      {
         // // TODO: 2/3/17
      }
   }

}
