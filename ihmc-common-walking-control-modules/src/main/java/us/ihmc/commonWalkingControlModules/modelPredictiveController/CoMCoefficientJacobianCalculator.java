package us.ihmc.commonWalkingControlModules.modelPredictiveController;

import org.ejml.data.DMatrix;
import us.ihmc.commons.MathTools;

public class CoMCoefficientJacobianCalculator
{
   public static void calculateCoMJacobian(int segmentId, double time, DMatrix jacobianToPack, int derivative, double scale)
   {
      switch (derivative)
      {
         case 0:
            calculatePositionJacobian(segmentId, time, jacobianToPack, scale);
            break;
         case 1:
            calculateVelocityJacobian(segmentId, jacobianToPack, scale);
            break;
         case 2:
            calculateAccelerationJacobian();
            break;
         case 3:
            calculateJerkJacobian();
            break;
         default:
            throw new IllegalArgumentException("Derivative order must be less than 4.");
      }
   }

   public static void calculateDCMJacobian(int segmentId, double omega, double time, DMatrix jacobianToPack, int derivative, double scale)
   {
      calculateCoMJacobian(segmentId, time, jacobianToPack, derivative, scale);
      calculateCoMJacobian(segmentId, time, jacobianToPack, derivative + 1, scale / omega);
   }

   public static void calculateVRPJacobian(int segmentId, double omega, double time, DMatrix jacobianToPack, int derivative, double scale)
   {
      calculateCoMJacobian(segmentId, time, jacobianToPack, derivative, scale);
      calculateCoMJacobian(segmentId, time, jacobianToPack, derivative + 2, -scale / (omega * omega));
   }

   public static void calculatePositionJacobian(int segmentId, double time, DMatrix positionJacobianToPack, double scale)
   {
      int startIndex = MPCIndexHandler.comCoefficientsPerSegment * segmentId;
      double c1 = scale;

      add(positionJacobianToPack, 0, startIndex + 1, c1);
      add(positionJacobianToPack, 1, startIndex + 3, c1);
      add(positionJacobianToPack, 2, startIndex + 5, c1);

      if (!MathTools.epsilonEquals(time, 0.0, 1e-5))
      {
         double c0 = time * c1;
         add(positionJacobianToPack, 0, startIndex, c0);
         add(positionJacobianToPack, 1, startIndex + 2, c0);
         add(positionJacobianToPack, 2, startIndex + 4, c0);
      }
   }

   public static void calculateVelocityJacobian(int segmentId, DMatrix velocityJacobianToPack, double scale)
   {
      int startIndex = MPCIndexHandler.comCoefficientsPerSegment * segmentId;
      add(velocityJacobianToPack, 0, startIndex, scale);
      add(velocityJacobianToPack, 1, startIndex + 2, scale);
      add(velocityJacobianToPack, 2, startIndex + 4, scale);
   }

   public static void calculateAccelerationJacobian()
   {
   }

   public static void calculateJerkJacobian()
   {
   }

   private static void add(DMatrix matrixToPack, int row, int col, double value)
   {
      matrixToPack.set(row, col, value + matrixToPack.get(row, col));
   }
}
