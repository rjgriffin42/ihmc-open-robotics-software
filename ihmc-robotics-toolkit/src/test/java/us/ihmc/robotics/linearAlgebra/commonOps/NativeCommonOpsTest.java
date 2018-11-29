package us.ihmc.robotics.linearAlgebra.commonOps;

import java.util.Random;

import org.apache.commons.math3.util.Precision;
import org.ejml.alg.dense.linsol.qr.LinearSolverQrHouseCol_D64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;

import us.ihmc.commons.Conversions;
import us.ihmc.robotics.functionApproximation.DampedLeastSquaresSolver;
import us.ihmc.robotics.testing.JUnitTools;

public class NativeCommonOpsTest
{
   @Test
   public void testMult()
   {
      Random random = new Random(40L);
      double epsilon = 1.0e-10;
      int iterations = 3000;
      int maxSize = 150;

      System.out.println("Testing matrix multiplications with random matrices...");

      long nativeTime = 0;
      long ejmlTime = 0;
      double matrixSizes = 0.0;

      for (int i = 0; i < iterations; i++)
      {
         int aRows = random.nextInt(maxSize) + 1;
         int aCols = random.nextInt(maxSize) + 1;
         int bCols = random.nextInt(maxSize) + 1;
         matrixSizes += (aRows + aCols + bCols) / 3.0;

         DenseMatrix64F A = RandomMatrices.createRandom(aRows, aCols, random);
         DenseMatrix64F B = RandomMatrices.createRandom(aCols, bCols, random);
         DenseMatrix64F actual = new DenseMatrix64F(aRows, bCols);
         DenseMatrix64F expected = new DenseMatrix64F(aRows, bCols);

         nativeTime -= System.nanoTime();
         NativeCommonOps.mult(A, B, actual);
         nativeTime += System.nanoTime();

         ejmlTime -= System.nanoTime();
         CommonOps.mult(A, B, expected);
         ejmlTime += System.nanoTime();

         JUnitTools.assertMatrixEquals(expected, actual, epsilon);
      }

      System.out.println("Native took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (nativeTime / iterations)), 3) + " ms on average");
      System.out.println("EJML took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (ejmlTime / iterations)), 3) + " ms on average");
      System.out.println("Average matrix size was " + Precision.round(matrixSizes / iterations, 1) + "\n");
   }

   @Test
   public void testMultQuad()
   {
      Random random = new Random(40L);
      double epsilon = 1.0e-10;
      int iterations = 3000;
      int maxSize = 150;

      System.out.println("Testing computing quadratic form with random matrices...");

      long nativeTime = 0;
      long ejmlTime = 0;
      double matrixSizes = 0.0;

      for (int i = 0; i < iterations; i++)
      {
         int aRows = random.nextInt(maxSize) + 1;
         int aCols = random.nextInt(maxSize) + 1;
         matrixSizes += (aRows + aCols) / 2.0;

         DenseMatrix64F A = RandomMatrices.createRandom(aRows, aCols, random);
         DenseMatrix64F B = RandomMatrices.createRandom(aRows, aRows, random);
         DenseMatrix64F actual = new DenseMatrix64F(aCols, aCols);
         DenseMatrix64F expected = new DenseMatrix64F(aCols, aCols);
         DenseMatrix64F tempBA = new DenseMatrix64F(aRows, aCols);

         nativeTime -= System.nanoTime();
         NativeCommonOps.multQuad(A, B, actual);
         nativeTime += System.nanoTime();

         ejmlTime -= System.nanoTime();
         CommonOps.mult(B, A, tempBA);
         CommonOps.multTransA(A, tempBA, expected);
         ejmlTime += System.nanoTime();

         JUnitTools.assertMatrixEquals(expected, actual, epsilon);
      }

      System.out.println("Native took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (nativeTime / iterations)), 3) + " ms on average");
      System.out.println("EJML took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (ejmlTime / iterations)), 3) + " ms on average");
      System.out.println("Average matrix size was " + Precision.round(matrixSizes / iterations, 1) + "\n");
   }

   @Test
   public void testSolve()
   {
      Random random = new Random(40L);
      double epsilon = 1.0e-9;
      int iterations = 3000;
      int maxSize = 150;

      System.out.println("Testing solving linear equations with random matrices...");

      long nativeTime = 0;
      long ejmlTime = 0;
      double matrixSizes = 0;
      LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.lu(maxSize);

      for (int i = 0; i < iterations; i++)
      {
         int aRows = random.nextInt(maxSize) + 1;
         matrixSizes += aRows;

         DenseMatrix64F A = RandomMatrices.createRandom(aRows, aRows, random);
         DenseMatrix64F x = RandomMatrices.createRandom(aRows, 1, random);
         DenseMatrix64F b = new DenseMatrix64F(aRows, 1);
         CommonOps.mult(A, x, b);

         DenseMatrix64F nativeResult = new DenseMatrix64F(aRows, 1);
         DenseMatrix64F ejmlResult = new DenseMatrix64F(aRows, 1);

         nativeTime -= System.nanoTime();
         NativeCommonOps.solve(A, b, nativeResult);
         nativeTime += System.nanoTime();

         ejmlTime -= System.nanoTime();
         solver.setA(A);
         solver.solve(b, ejmlResult);
         ejmlTime += System.nanoTime();

         JUnitTools.assertMatrixEquals(x, nativeResult, epsilon);
         JUnitTools.assertMatrixEquals(x, ejmlResult, epsilon);
      }

      System.out.println("Native took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (nativeTime / iterations)), 3) + " ms on average");
      System.out.println("EJML took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (ejmlTime / iterations)), 3) + " ms on average");
      System.out.println("Average matrix size was " + Precision.round(matrixSizes / iterations, 1) + "\n");
   }

   @Test
   public void testSolveLeastSquare()
   {
      Random random = new Random(40L);
      double epsilon = 1.0e-11;
      int iterations = 3000;
      int maxSize = 200;

      System.out.println("Testing solving least square problem with random matrices...");

      long nativeDampedTime = 0;
      long nativeUndampedTime = 0;
      long ejmlDampedTime = 0;
      long ejmlUndampedTime = 0;
      double matrixSizes = 0;
      double alpha = 0.01;
      LinearSolver<DenseMatrix64F> dampedSolver = new DampedLeastSquaresSolver(maxSize, alpha);
      LinearSolver<DenseMatrix64F> undampedSolver = new LinearSolverQrHouseCol_D64();

      for (int i = 0; i < iterations; i++)
      {
         int aRows = random.nextInt(maxSize) + 2;
         int aCols = random.nextInt(aRows - 1) + 1;
         matrixSizes += (aRows + aCols) / 2.0;

         DenseMatrix64F A = RandomMatrices.createRandom(aRows, aCols, random);
         DenseMatrix64F x = RandomMatrices.createRandom(aCols, 1, random);
         DenseMatrix64F b = new DenseMatrix64F(aRows, 1);
         CommonOps.mult(A, x, b);

         DenseMatrix64F nativeDampedResult = new DenseMatrix64F(aCols, 1);
         DenseMatrix64F nativeUndampedResult = new DenseMatrix64F(aCols, 1);
         DenseMatrix64F ejmlDampedResult = new DenseMatrix64F(aCols, 1);
         DenseMatrix64F ejmlUndampedResult = new DenseMatrix64F(aCols, 1);

         nativeDampedTime -= System.nanoTime();
         NativeCommonOps.solveDamped(A, b, alpha, nativeDampedResult);
         nativeDampedTime += System.nanoTime();

         nativeUndampedTime -= System.nanoTime();
         NativeCommonOps.solveRobust(A, b, nativeUndampedResult);
         nativeUndampedTime += System.nanoTime();

         ejmlDampedTime -= System.nanoTime();
         dampedSolver.setA(A);
         dampedSolver.solve(b, ejmlDampedResult);
         ejmlDampedTime += System.nanoTime();

         ejmlUndampedTime -= System.nanoTime();
         undampedSolver.setA(A);
         undampedSolver.solve(b, ejmlUndampedResult);
         ejmlUndampedTime += System.nanoTime();

         JUnitTools.assertMatrixEquals(ejmlDampedResult, nativeDampedResult, epsilon);
         JUnitTools.assertMatrixEquals(x, nativeUndampedResult, epsilon);
         JUnitTools.assertMatrixEquals(x, ejmlUndampedResult, epsilon);
      }

      System.out.println("Native damped took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (nativeDampedTime / iterations)), 3) + " ms on average");
      System.out.println("Native undamped took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (nativeUndampedTime / iterations)), 3) + " ms on average");
      System.out.println("EJML damped took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (ejmlDampedTime / iterations)), 3) + " ms on average");
      System.out.println("EJML undamped took " + Precision.round(Conversions.nanosecondsToMilliseconds((double) (ejmlUndampedTime / iterations)), 3) + " ms on average");
      System.out.println("Average matrix size was " + Precision.round(matrixSizes / iterations, 1) + "\n");
   }
}
