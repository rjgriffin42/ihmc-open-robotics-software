package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator.YoFrameTrajectory3D;
import us.ihmc.commonWalkingControlModules.angularMomentumTrajectoryGenerator.YoTrajectory;
import us.ihmc.commons.PrintTools;
import us.ihmc.robotics.geometry.Direction;
import us.ihmc.robotics.geometry.FrameTuple;

public class CapturePointMatrixTools extends CapturePointTools
{
   // private static final ThreadLocal<DenseMatrix64F> dummyName = new ThreadLocal<>();
   
   private static final int defaultSize = 1000;
   
   private static final DenseMatrix64F tPowersDerivativeVector = new DenseMatrix64F(defaultSize, 1);
   private static final DenseMatrix64F tPowersDerivativeVectorTranspose = new DenseMatrix64F(defaultSize, 1);
   
   private static final DenseMatrix64F generalizedAlphaPrimeRow = new DenseMatrix64F(1, defaultSize);
   private static final DenseMatrix64F generalizedBetaPrimeRow = new DenseMatrix64F(1, defaultSize);
   
   private static final DenseMatrix64F polynomialCoefficientCombinedVector= new DenseMatrix64F(defaultSize, defaultSize);
   private static final DenseMatrix64F polynomialCoefficientVector= new DenseMatrix64F(defaultSize, 1);
   
   private static final DenseMatrix64F generalizedAlphaPrimeMatrix = new DenseMatrix64F(defaultSize, defaultSize);
   private static final DenseMatrix64F generalizedBetaPrimeMatrix = new DenseMatrix64F(defaultSize, defaultSize);
   private static final DenseMatrix64F generalizedGammaPrimeMatrix = new DenseMatrix64F(1, 1);
   private static final DenseMatrix64F generalizedAlphaBetaPrimeMatrix = new DenseMatrix64F(defaultSize, defaultSize);
   
   private static final DenseMatrix64F M1 = new DenseMatrix64F(defaultSize, defaultSize);
   private static final DenseMatrix64F M2 = new DenseMatrix64F(defaultSize, defaultSize);
   
   /**
    * Variation of J. Englsberger's "Smooth trajectory generation and push-recovery based on DCM"
    * <br>
    * The approach for calculating DCMs is based on CMP polynomials instead of discrete waypoints
    * 
    * @param icpDerivativeOrder
    * @param cmpPolynomial
    * @param icpPositionDesiredFinal
    * @param time
    * @return
    */
   public static void calculateICPQuantityFromCorrespondingCMPPolynomial3D(double omega0, double time, int icpDerivativeOrder, 
                                                                           YoFrameTrajectory3D cmpPolynomial3D, 
                                                                           FrameTuple<?, ?> icpPositionDesiredFinal, 
                                                                           FrameTuple<?, ?> icpQuantityDesired)
   {        
      int numberOfCoefficients = cmpPolynomial3D.getNumberOfCoefficients();
      if(numberOfCoefficients == -1)
      {
         icpQuantityDesired.setToNaN();
         return;
      }
      
      initializeMatrices(numberOfCoefficients);
      setPolynomialCoefficientMatrix3D(polynomialCoefficientCombinedVector, cmpPolynomial3D);

      calculateGeneralizedAlphaPrimeOnCMPSegment3D(omega0, time, generalizedAlphaPrimeMatrix, icpDerivativeOrder, cmpPolynomial3D);
      calculateGeneralizedBetaPrimeOnCMPSegment3D(omega0, time, generalizedBetaPrimeMatrix, icpDerivativeOrder, cmpPolynomial3D);
      calculateGeneralizedGammaPrimeOnCMPSegment3D(omega0, time, generalizedGammaPrimeMatrix, icpDerivativeOrder, cmpPolynomial3D);
      CommonOps.subtract(generalizedAlphaPrimeMatrix, generalizedBetaPrimeMatrix, generalizedAlphaBetaPrimeMatrix);

      calculateICPQuantity3D(generalizedAlphaBetaPrimeMatrix, generalizedGammaPrimeMatrix, polynomialCoefficientCombinedVector, 
                             icpPositionDesiredFinal, icpQuantityDesired);
      
//      PrintTools.debug("A: " + generalizedAlphaPrimeMatrix.toString());
//      PrintTools.debug("B: " + generalizedBetaPrimeMatrix.toString());
//      PrintTools.debug("C: " + generalizedGammaPrimeMatrix.toString());
//      PrintTools.debug("AB: " + generalizedAlphaBetaPrimeMatrix.toString());
//      PrintTools.debug("P: " + polynomialCoefficientCombinedVector.toString());
   }
   
   /**
    * Compute the i-th derivative of &xi;<sub>ref,&phi;</sub> at time t<sub>&phi;</sub>: 
    * <P>
    * &xi;<sup>(i)</sup><sub>ref,&phi;</sub>(t<sub>&phi;</sub>) = 
    * (&alpha;<sup>(i)</sup><sub>&phi;</sub>(t<sub>&phi;</sub>)
    *  - &beta;<sup>(i)</sup><sub>&phi;</sub>(t<sub>&phi;</sub>)) * p<sub>&phi;</sub>
    *  + &gamma;<sup>(i)</sup><sub>&phi;</sub>(t<sub>&phi;</sub>) * &xi;<sub>ref,&phi;</sub>(T<sub>&phi;</sub>)
    * 
    * @param generalizedAlphaBetaPrimeMatrix
    * @param generalizedGammaPrimeMatrix
    * @param polynomialCoefficientVector
    * @param icpPositionDesiredFinal
    * @return
    */
   public static void calculateICPQuantity3D(DenseMatrix64F generalizedAlphaBetaPrimeMatrix, DenseMatrix64F generalizedGammaPrimeMatrix,
                                              DenseMatrix64F polynomialCoefficientCombinedVector, FrameTuple<?, ?> icpPositionDesiredFinal,
                                              FrameTuple<?, ?> icpQuantityDesired)
   {
      M1.reshape(generalizedAlphaBetaPrimeMatrix.getNumRows(), polynomialCoefficientCombinedVector.getNumCols());
      M1.zero();

      CommonOps.mult(generalizedAlphaBetaPrimeMatrix, polynomialCoefficientCombinedVector, M1);

      M2.reshape(M1.getNumRows(),  M1.getNumCols());
      M2.set(0, 0, generalizedGammaPrimeMatrix.get(0, 0) * icpPositionDesiredFinal.getX());
      M2.set(1, 0, generalizedGammaPrimeMatrix.get(0, 0) * icpPositionDesiredFinal.getY());
      M2.set(2, 0, generalizedGammaPrimeMatrix.get(0, 0) * icpPositionDesiredFinal.getZ());
            
      CommonOps.addEquals(M1, M2);
      
      icpQuantityDesired.set(M1.get(0, 0), M1.get(1, 0), M1.get(2, 0));
   }
   
   /**
    * Compute the i-th derivative of &alpha;<sub>&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &alpha;<sup>(i)</sup><sub>&phi;</sub>(t<sub>&phi;</sub>) = &Sigma;<sub>j=0</sub><sup>n</sup> &omega;<sub>0</sub><sup>-j</sup> *
    * t<sup>(j+i)<sup>T</sup></sup> (t<sub>&phi;</sub>)
    * 
    * @param generalizedAlphaPrime
    * @param alphaDerivativeOrder
    * @param cmpPolynomial
    * @param time
    */
   public static void calculateGeneralizedAlphaPrimeOnCMPSegment3D(double omega0, double time, DenseMatrix64F generalizedAlphaPrime, 
                                                                 int alphaDerivativeOrder, YoFrameTrajectory3D cmpPolynomial3D)
   {
      for(Direction dir : Direction.values())
      {
         YoTrajectory cmpPolynomial = cmpPolynomial3D.getYoTrajectory(dir);
         
         int numberOfCoefficients = cmpPolynomial.getNumberOfCoefficients();
         
         tPowersDerivativeVector.reshape(numberOfCoefficients, 1);
         tPowersDerivativeVectorTranspose.reshape(1, numberOfCoefficients);
         
         generalizedAlphaPrimeRow.reshape(1, numberOfCoefficients);
         generalizedAlphaPrimeRow.zero();
         
         for(int i = 0; i < numberOfCoefficients; i++)
         {
            tPowersDerivativeVector.zero();
            tPowersDerivativeVectorTranspose.zero();
            
            tPowersDerivativeVector.set(cmpPolynomial.getXPowersDerivativeVector(i + alphaDerivativeOrder, time));
            CommonOps.transpose(tPowersDerivativeVector, tPowersDerivativeVectorTranspose);
                     
            double scalar = Math.pow(omega0, -i);
            CommonOps.addEquals(generalizedAlphaPrimeRow, scalar, tPowersDerivativeVectorTranspose);
         }
         
         CommonOps.insert(generalizedAlphaPrimeRow, generalizedAlphaPrime, dir.ordinal(), dir.ordinal() * generalizedAlphaPrimeRow.numCols);
      }
   }
   
   /**
    * Compute the i-th derivative of &beta;<sub>&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &beta;<sup>(i)</sup><sub>&phi;</sub>(t<sub>&phi;</sub>) = &Sigma;<sub>j=0</sub><sup>n</sup> &omega;<sub>0</sub><sup>-(j-i)</sup> *
    * t<sup>(j)<sup>T</sup></sup> (T<sub>&phi;</sub>) * e<sup>&omega;<sub>0</sub>(t<sub>&phi;</sub>-T<sub>&phi;</sub>)</sup>
    * 
    * @param generalizedBetaPrime
    * @param betaDerivativeOrder
    * @param cmpPolynomial
    * @param time
    */
   public static void calculateGeneralizedBetaPrimeOnCMPSegment3D(double omega0, double time, DenseMatrix64F generalizedBetaPrime, 
                                                                  int betaDerivativeOrder, YoFrameTrajectory3D cmpPolynomial3D)
   {                  
      for(Direction dir : Direction.values())
      {
         YoTrajectory cmpPolynomial = cmpPolynomial3D.getYoTrajectory(dir);
         
         int numberOfCoefficients = cmpPolynomial.getNumberOfCoefficients();
         double timeSegmentTotal = cmpPolynomial.getFinalTime();
         
         tPowersDerivativeVector.reshape(numberOfCoefficients, 1);
         tPowersDerivativeVectorTranspose.reshape(1, numberOfCoefficients);
         
         generalizedBetaPrimeRow.reshape(1, numberOfCoefficients);
         generalizedBetaPrimeRow.zero();
         
         for(int i = 0; i < numberOfCoefficients; i++)
         {
            tPowersDerivativeVector.zero();
            tPowersDerivativeVectorTranspose.zero();
            
            tPowersDerivativeVector.set(cmpPolynomial.getXPowersDerivativeVector(i, timeSegmentTotal));
            CommonOps.transpose(tPowersDerivativeVector, tPowersDerivativeVectorTranspose);
                     
            double scalar = Math.pow(omega0, betaDerivativeOrder-i) * Math.exp(omega0*(time-timeSegmentTotal));
            CommonOps.addEquals(generalizedBetaPrimeRow, scalar, tPowersDerivativeVectorTranspose);
         }
         
         CommonOps.insert(generalizedBetaPrimeRow, generalizedBetaPrime, dir.ordinal(), dir.ordinal() * generalizedBetaPrimeRow.numCols);
      }
   }

   /**
    * Compute the i-th derivative of &gamma;<sub>&phi;</sub> at time t<sub>&phi;</sub>:
    * <P>
    * &gamma;<sup>(i)</sup><sub>&phi;</sub>(t<sub>&phi;</sub>) = &omega;<sub>0</sub><sup>i</sup> * 
    * e<sup>&omega;<sub>0</sub>(t<sub>&phi;</sub>-T<sub>&phi;</sub>)</sup>
    * 
    * @param generalizedGammaPrime
    * @param gammaDerivativeOrder
    * @param cmpPolynomial3D
    * @param time
    */
   public static void calculateGeneralizedGammaPrimeOnCMPSegment3D(double omega0, double time, DenseMatrix64F generalizedGammaPrime, 
                                                                   int gammaDerivativeOrder, YoFrameTrajectory3D cmpPolynomial3D)
   {      
      double timeSegmentTotal = cmpPolynomial3D.getFinalTime();
      double ddGamaPrimeValue = Math.pow(omega0, gammaDerivativeOrder)*Math.exp(omega0 * (time - timeSegmentTotal));
      generalizedGammaPrime.set(0, 0, ddGamaPrimeValue);
   }
   
   public static void initializeMatrices(int numberOfCoefficients)
   {
      polynomialCoefficientCombinedVector.reshape(3 * numberOfCoefficients, 1);
      polynomialCoefficientCombinedVector.zero();
      
      generalizedAlphaPrimeMatrix.reshape(3, 3 * numberOfCoefficients);
      generalizedAlphaPrimeMatrix.zero();
      
      generalizedBetaPrimeMatrix.reshape(3, 3 * numberOfCoefficients);
      generalizedBetaPrimeMatrix.zero();
      
      generalizedAlphaBetaPrimeMatrix.reshape(3, 3 * numberOfCoefficients);
      generalizedAlphaBetaPrimeMatrix.zero();
      
      generalizedGammaPrimeMatrix.reshape(1, 1);
      generalizedGammaPrimeMatrix.zero();
   }
   
   public static void setPolynomialCoefficientMatrix3D(DenseMatrix64F polynomialCoefficientCombinedVector, YoFrameTrajectory3D cmpPolynomial3D)
   {
      for(Direction dir : Direction.values())
      {
         setPolynomialCoefficientVector1D(polynomialCoefficientVector, cmpPolynomial3D.getYoTrajectory(dir));
         
         CommonOps.insert(polynomialCoefficientVector, polynomialCoefficientCombinedVector, dir.ordinal() * polynomialCoefficientVector.numRows, 0);
      }
   }
   
   public static void setPolynomialCoefficientVector1D(DenseMatrix64F polynomialCoefficientVector, YoTrajectory cmpPolynomial)
   {
      double[] polynomialCoefficients = cmpPolynomial.getCoefficients();
      
      polynomialCoefficientVector.reshape(cmpPolynomial.getNumberOfCoefficients(), 1);
      polynomialCoefficientVector.zero();
      
      polynomialCoefficientVector.setData(polynomialCoefficients);
   }
}
