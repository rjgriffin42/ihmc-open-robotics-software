package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.stateMatrices.swing;

import org.ejml.data.DenseMatrix64F;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.ArrayList;

public class SwingStateEndRecursionMatrix extends DenseMatrix64F
{
   private final DoubleYoVariable doubleSupportSplitRatio;
   private final DoubleYoVariable startOfSplineTime;
   private final DoubleYoVariable endOfSplineTime;
   private final DoubleYoVariable totalTrajectoryTime;

   public SwingStateEndRecursionMatrix(DoubleYoVariable doubleSupportSplitRatio, DoubleYoVariable startOfSplineTime, DoubleYoVariable endOfSplineTime,
         DoubleYoVariable totalTrajectoryTime)
   {
      super(4, 1);

      this.doubleSupportSplitRatio = doubleSupportSplitRatio;
      this.startOfSplineTime = startOfSplineTime;
      this.endOfSplineTime = endOfSplineTime;
      this.totalTrajectoryTime = totalTrajectoryTime;
   }

   public void reset()
   {
      zero();
   }

   public void compute(ArrayList<DoubleYoVariable> doubleSupportDurations, ArrayList<DoubleYoVariable> singleSupportDurations, double omega0)
   {
      double upcomingDoubleSupportDuration = doubleSupportDurations.get(1).getDoubleValue();
      double currentDoubleSupportDuration = doubleSupportDurations.get(0).getDoubleValue();

      compute(upcomingDoubleSupportDuration, currentDoubleSupportDuration, singleSupportDurations.get(0).getDoubleValue(), omega0);
   }

   public void compute(double upcomingDoubleSupportDuration, double currentDoubleSupportDuration, double singleSupportDuration, double omega0)
   {
      double stepDuration = currentDoubleSupportDuration + singleSupportDuration;

      double endOfCurrentDoubleSupportDuration = (1.0 - doubleSupportSplitRatio.getDoubleValue()) * currentDoubleSupportDuration;
      double upcomingInitialDoubleSupportDuration = doubleSupportSplitRatio.getDoubleValue() * upcomingDoubleSupportDuration;

      double lastSegmentDuration = totalTrajectoryTime.getDoubleValue() - endOfSplineTime.getDoubleValue();
      double stateRecursionToEnd = Math.exp(-omega0 * lastSegmentDuration);

      double recursionTimeToInitial = upcomingInitialDoubleSupportDuration + endOfCurrentDoubleSupportDuration + startOfSplineTime.getDoubleValue() - stepDuration;
      double stateRecursionToStart = Math.exp(omega0 * recursionTimeToInitial);

      set(0, 0, stateRecursionToStart);
      set(1, 0, omega0 * stateRecursionToStart);
      set(2, 0, stateRecursionToEnd);
      set(3, 0, omega0 * stateRecursionToEnd);
   }
}

