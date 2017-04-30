package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.current;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.CubicDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.CubicMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.NewCubicDerivativeMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.interpolation.NewCubicMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.swing.SwingInitialICPMatrix;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.multipliers.stateMatrices.transfer.TransferInitialICPMatrix;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

import java.util.List;

/**
 * Multiplier of the initial ICP value at the beginning of the current state. Used to compute the desired current
 * ICP location from the recursively found end-of-state ICP corner point
 */
public class InitialICPCurrentMultiplier
{
   /** Cubic spline matrix that multiplies the boundary conditions to compute the current value. */
   private final NewCubicMatrix cubicMatrix;
   /** Cubic spline matrix that multiplies the boundary conditions to compute the current derivative value. */
   private final NewCubicDerivativeMatrix cubicDerivativeMatrix;

   /** whether or not the cubic matrix needs to be updated inside this class or is updated outside it. */
   private final boolean givenCubicMatrix;
   /** whether or not the cubic derivative matrix needs to be updated inside this class or is updated outside it. */
   private final boolean givenCubicDerivativeMatrix;

   /** Boundary conditions matrix for the initial ICP when in transfer. */
   private final TransferInitialICPMatrix transferInitialICPMatrix;
   /** Boundary conditions matrix for the initial ICP when in swing. */
   private final SwingInitialICPMatrix swingInitialICPMatrix;

   /** time in swing state for the start of using the spline */
   public final DoubleYoVariable startOfSplineTime;
   /** time in swing state for the end of using the spline */
   public final DoubleYoVariable endOfSplineTime;

   /** data holder for multiplied values */
   private final DenseMatrix64F matrixOut = new DenseMatrix64F(1, 1);

   /** multiplier of the initial ICP to compute the current ICP location. */
   private final DoubleYoVariable positionMultiplier;
   /** multiplier of the initial ICP to compute the current ICP velocity. */
   private final DoubleYoVariable velocityMultiplier;

   public InitialICPCurrentMultiplier(DoubleYoVariable startOfSplineTime, DoubleYoVariable endOfSplineTime, String yoNamePrefix, YoVariableRegistry registry)
   {
      this(startOfSplineTime, endOfSplineTime, null, null, yoNamePrefix, registry);
   }

   public InitialICPCurrentMultiplier(DoubleYoVariable startOfSplineTime, DoubleYoVariable endOfSplineTime, NewCubicMatrix cubicMatrix,
         NewCubicDerivativeMatrix cubicDerivativeMatrix, String yoNamePrefix, YoVariableRegistry registry)
   {
      positionMultiplier = new DoubleYoVariable(yoNamePrefix + "InitialICPCurrentMultiplier", registry);
      velocityMultiplier = new DoubleYoVariable(yoNamePrefix + "InitialICPCurrentVelocityMultiplier", registry);

      this.startOfSplineTime = startOfSplineTime;
      this.endOfSplineTime = endOfSplineTime;

      if (cubicMatrix == null)
      {
         this.cubicMatrix = new NewCubicMatrix();
         givenCubicMatrix = false;
      }
      else
      {
         this.cubicMatrix = cubicMatrix;
         givenCubicMatrix = true;
      }

      if (cubicDerivativeMatrix == null)
      {
         this.cubicDerivativeMatrix = new NewCubicDerivativeMatrix();
         givenCubicDerivativeMatrix = false;
      }
      else
      {
         this.cubicDerivativeMatrix = cubicDerivativeMatrix;
         givenCubicDerivativeMatrix = true;
      }

      transferInitialICPMatrix = new TransferInitialICPMatrix();
      swingInitialICPMatrix = new SwingInitialICPMatrix(startOfSplineTime);
   }

   /**
    * Resets the multiplier values to NaN. Done at every tick.
    */
   public void reset()
   {
      positionMultiplier.setToNaN();
      velocityMultiplier.setToNaN();
   }

   /**
    * Gets the value to multiply the initial ICP location by to compute the current ICP location.
    *
    * @return position multiplier.
    */
   public double getPositionMultiplier()
   {
      return positionMultiplier.getDoubleValue();
   }

   /**
    * Gets the value to multiply the initial ICP location by to compute the current ICP velocity.
    *
    * @return velocity multiplier.
    */
   public double getVelocityMultiplier()
   {
      return velocityMultiplier.getDoubleValue();
   }

   /**
    * Computes the initial ICP multiplier. Must be called every control tick.
    *
    * @param doubleSupportDurations vector of double support durations.
    * @param timeInState time in the current state.
    * @param useTwoCMPs whether or not to use two CMPs in the ICP plan.
    * @param isInTransfer whether or not the robot is currently in the transfer phase.
    * @param omega0 natural frequency of the inverted pendulum.
    */
   public void compute(List<DoubleYoVariable> doubleSupportDurations, double timeInState, boolean useTwoCMPs, boolean isInTransfer, double omega0)
   {
      double positionMultiplier, velocityMultiplier;
      if (isInTransfer)
      {
         positionMultiplier = computeInTransfer(doubleSupportDurations, timeInState);
      }
      else
      {
         if (useTwoCMPs)
            positionMultiplier = computeSwingSegmented(timeInState, omega0);
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
            velocityMultiplier = computeSwingSegmentedVelocity(timeInState, omega0);
         else
            velocityMultiplier = computeInSwingOneCMPVelocity();
      }

      this.velocityMultiplier.set(velocityMultiplier);
   }



   /**
    * Computes the position multiplier when in the transfer phase. During this phase, the trajectory is a cubic spline,
    * so this is used to calculate the position multiplier.
    *
    * @param doubleSupportDurations vector of double support durations
    * @param timeInState time in the transfer state
    * @return position multiplier.
    */
   private double computeInTransfer(List<DoubleYoVariable> doubleSupportDurations, double timeInState)
   {
      transferInitialICPMatrix.compute();

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

      CommonOps.mult(cubicMatrix, transferInitialICPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   /**
    * Computes the position multiplier when in the transfer phase. During this phase, the trajectory is a
    * cubic spline, so this is used to calculate the position multiplier.
    *
    * @return velocity multiplier.
    */
   private double computeInTransferVelocity()
   {
      CommonOps.mult(cubicDerivativeMatrix, transferInitialICPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }





   /**
    * Computes the position multiplier in the swing phase when using one CMP in each foot. The desired
    * ICP position computed from the stance entry CMP and the next corner point, not the initial ICP location.
    *
    * @return position multiplier
    */
   private double computeInSwingOneCMP()
   {
      return 0.0;
   }

   /**
    * Computes the velocity multiplier in the swing phase when using one CMP in each foot. The desired
    * ICP position computed from the stance entry CMP and the next corner point, not the initial ICP location.
    *
    * @return velocity multiplier
    */
   private double computeInSwingOneCMPVelocity()
   {
      return 0.0;
   }




   /**
    * Computes the position multiplier in the swing phase when using two CMPs in each foot.
    *
    * @param timeInState time in the swing state.
    * @param omega0 natural frequency of the inverted pendulum
    * @return position multiplier.
    */
   private double computeSwingSegmented(double timeInState, double omega0)
   {
      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeSwingFirstSegment(timeInState, omega0);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeSwingThirdSegment();
      else
         return computeSwingSecondSegment(timeInState, omega0);
   }

   /**
    * Computes the position multiplier when in the first segment in the swing phase. During this
    * segment, the ICP plan is on the entry CMP. In this phase, the position is given by <br>
    *    &xi; = e<sup>&omega; t</sup> &xi;<sub>0</sub> + (1.0 - e<sup>&omega; t</sup>) r<sub>cmp,H</sub><br>
    * So the multiplier is simply &gamma;<sub>0</sub> = e<sup>&omega; t</sup>
    *
    * @param timeInState time in the swing state
    * @param omega0 natural frequency of the inverted pendulum.
    * @return position multiplier.
    */
   private double computeSwingFirstSegment(double timeInState, double omega0)
   {
      return Math.exp(omega0 * timeInState);
   }

   /**
    * Computes the position multiplier when in the second segment in the swing phase. During this
    * segment, the trajectory is a cubic spline, so this is used to calculate the position
    * multiplier.
    *
    * @param timeInState time in the swing state.
    * @param omega0 natural frequency of the inverted pendulum.
    * @return position multiplier.
    */
   private double computeSwingSecondSegment(double timeInState, double omega0)
   {
      swingInitialICPMatrix.compute(omega0);

      double timeInSpline = timeInState - startOfSplineTime.getDoubleValue();
      double splineDuration = endOfSplineTime.getDoubleValue() - startOfSplineTime.getDoubleValue();

      if (!givenCubicDerivativeMatrix)
      {
         cubicDerivativeMatrix.setSegmentDuration(splineDuration);
         cubicDerivativeMatrix.update(timeInSpline);
      }

      if (!givenCubicMatrix)
      {
         cubicMatrix.setSegmentDuration(splineDuration);
         cubicMatrix.update(timeInSpline);
      }

      CommonOps.mult(cubicMatrix, swingInitialICPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   /**
    * Computes the position multiplier in the swing phase when using two CMPs in each foot. The desired
    * ICP position computed from the stance exit CMP and the next corner point, not the initial ICP location.
    *
    * @return position multiplier
    */
   private double computeSwingThirdSegment()
   {
      return 0.0;
   }




   /**
    * Computes the velocity multiplier in the swing phase when using two CMPs in each foot.
    *
    * @param timeInState time in the swing state.
    * @param omega0 natural frequency of the inverted pendulum
    * @return velocity multiplier.
    */
   private double computeSwingSegmentedVelocity(double timeInState, double omega0)
   {
      if (timeInState < startOfSplineTime.getDoubleValue())
         return computeSwingFirstSegmentVelocity(omega0);
      else if (timeInState >= endOfSplineTime.getDoubleValue())
         return computeSwingThirdSegmentVelocity();
      else
         return computeSwingSecondSegmentVelocity();
   }

   /**
    * Computes the velocity multiplier in the first segment of the swing phase.
    *
    * @param omega0 natural frequency of the inverted pendulum.
    * @return velocity multiplier.
    */
   private double computeSwingFirstSegmentVelocity(double omega0)
   {
      return omega0 * positionMultiplier.getDoubleValue();
   }

   /**
    * Computes the velocity multiplier when in the second segment in the swing phase. During this
    * segment, the trajectory is a cubic spline, so this is used to calculate the velocity
    * multiplier.
    *
    * @return velocity multiplier.
    */
   private double computeSwingSecondSegmentVelocity()
   {
      CommonOps.mult(cubicDerivativeMatrix, swingInitialICPMatrix, matrixOut);

      return matrixOut.get(0, 0);
   }

   /**
    * Computes the position multiplier in the swing phase when using two CMPs in each foot. The desired
    * ICP position computed from the stance exit CMP and the next corner point, not the initial ICP location.
    *
    * @return velocity multiplier
    */
   private double computeSwingThirdSegmentVelocity()
   {
      return 0.0;
   }
}
