package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.projectionAndRecursionMultipliers.interpolation;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.robotics.MathTools;

public class CubicProjectionDerivativeMatrix extends DenseMatrix64F
{
   private final CubicSplineCoefficientMatrix cubicSplineCoefficientMatrix = new CubicSplineCoefficientMatrix();
   private final CubicTimeDerivativeMatrix cubicTimeMatrix = new CubicTimeDerivativeMatrix();

   private double duration;

   public CubicProjectionDerivativeMatrix()
   {
      super(1, 4);
   }

   public void setSegmentDuration(double duration)
   {
      this.duration = duration;
      cubicSplineCoefficientMatrix.setSegmentDuration(duration);
   }

   public void update(double timeRemaining)
   {
      double timeInCurrentState = duration - timeRemaining;
      timeInCurrentState = MathTools.clipToMinMax(timeInCurrentState, 0.0, duration);
      cubicTimeMatrix.setCurrentTime(timeInCurrentState);

      CommonOps.mult(cubicTimeMatrix, cubicSplineCoefficientMatrix, this);
   }
}
