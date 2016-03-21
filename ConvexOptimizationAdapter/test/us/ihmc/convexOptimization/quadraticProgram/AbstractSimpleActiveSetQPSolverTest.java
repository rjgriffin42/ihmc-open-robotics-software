package us.ihmc.convexOptimization.quadraticProgram;

import static org.junit.Assert.*;

import java.util.Random;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Ignore;
import org.junit.Test;

import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.random.RandomTools;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public abstract class AbstractSimpleActiveSetQPSolverTest
{
   private static final boolean VERBOSE = false;

   public abstract SimpleActiveSetQPSolverInterface createSolverToTest();

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSimpleCasesWithNoInequalityConstraints()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      // Minimize x^T * x
      double[][] costQuadraticMatrix = new double[][] { { 2.0 } };
      double[] costLinearVector = new double[] { 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[] solution = new double[1];
      double[] lagrangeEqualityMultipliers = new double[0];
      double[] lagrangeInequalityMultipliers = new double[0];
      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);
      assertEquals(1, solution.length);
      assertEquals(0.0, solution[0], 1e-7);

      // Minimize (x-5) * (x-5) = x^2 - 10x + 25
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0 } };
      costLinearVector = new double[] { -10.0 };
      quadraticCostScalar = 25.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      solution = new double[1];
      lagrangeEqualityMultipliers = new double[0];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(1, solution.length);
      assertEquals(5.0, solution[0], 1e-7);
      DenseMatrix64F solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      double objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(0.0, objectiveCost, 1e-7);

      // Minimize (x-5) * (x-5) + (y-3) * (y-3) = 1/2 * (2x^2 + 2y^2) - 10x -6y + 34
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { -10.0, -6.0 };
      quadraticCostScalar = 34.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[0];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(5.0, solution[0], 1e-7);
      assertEquals(3.0, solution[1], 1e-7);
      solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(0.0, objectiveCost, 1e-7);

      // Minimize x^2 + y^2 subject to x + y = 1.0
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0 } };
      double[] linearEqualityConstraintsBVector = new double[] { 1.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[1];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(0.5, solution[0], 1e-7);
      assertEquals(0.5, solution[1], 1e-7);
      assertEquals(-1.0, lagrangeEqualityMultipliers[0], 1e-7); // Lagrange multiplier is -1.0;
      solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(0.5, objectiveCost, 1e-7);

      // Minimize x^2 + y^2 subject to x + y = 2.0, 3x - 3y = 0.0
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0 }, { 3.0, -3.0 } };
      linearEqualityConstraintsBVector = new double[] { 2.0, 0.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[2];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(-2.0, lagrangeEqualityMultipliers[0], 1e-7); // Lagrange multiplier
      assertEquals(0.0, lagrangeEqualityMultipliers[1], 1e-7); // Lagrange multiplier
      solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(2.0, objectiveCost, 1e-7);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSimpleCasesWithInequalityConstraints()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      // Minimize x^T * x subject to x <= 1
      double[][] costQuadraticMatrix = new double[][] { { 2.0 } };
      double[] costLinearVector = new double[] { 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { 1.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { 1.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      double[] solution = new double[1];
      double[] lagrangeEqualityMultipliers = new double[0];
      double[] lagrangeInequalityMultipliers = new double[1];

      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(1, solution.length);
      assertEquals(0.0, solution[0], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);

      // Minimize x^T * x subject to x >= 1 (-x <= -1)
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0 } };
      costLinearVector = new double[] { 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearInequalityConstraintsCMatrix = new double[][] { { -1.0 } };
      linearInqualityConstraintsDVector = new double[] { -1.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[1];
      lagrangeEqualityMultipliers = new double[0];
      lagrangeInequalityMultipliers = new double[1];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(1, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(2.0, lagrangeInequalityMultipliers[0], 1e-7);

      // Minimize (x-5) * (x-5) = x^2 - 10x + 25 subject to x <= 3.0
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0 } };
      costLinearVector = new double[] { -10.0 };
      quadraticCostScalar = 25.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearInequalityConstraintsCMatrix = new double[][] { { 1.0 } };
      linearInqualityConstraintsDVector = new double[] { 3.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[1];
      lagrangeEqualityMultipliers = new double[0];
      lagrangeInequalityMultipliers = new double[1];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(1, solution.length);
      assertEquals(3.0, solution[0], 1e-7);
      assertEquals(4.0, lagrangeInequalityMultipliers[0], 1e-7);

      DenseMatrix64F solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      double objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(4.0, objectiveCost, 1e-7);

      // Minimize (x-5) * (x-5) + (y-3) * (y-3) = 1/2 * (2x^2 + 2y^2) - 10x -6y + 34 subject to x <= 7 y <= 1
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { -10.0, -6.0 };
      quadraticCostScalar = 34.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearInequalityConstraintsCMatrix = new double[][] { { 1.0, 0.0 }, { 0.0, 1.0 } };
      linearInqualityConstraintsDVector = new double[] { 7.0, 1.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[0];
      lagrangeInequalityMultipliers = new double[2];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(5.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(4.0, lagrangeInequalityMultipliers[1], 1e-7);

      solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(4.0, objectiveCost, 1e-7);

      // Minimize x^2 + y^2 subject to x + y = 1.0, x <= y - 1 (x - y <= -1.0)
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0 } };
      double[] linearEqualityConstraintsBVector = new double[] { 1.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      linearInequalityConstraintsCMatrix = new double[][] { { 1.0, -1.0 } };
      linearInqualityConstraintsDVector = new double[] { -1.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[1];
      lagrangeInequalityMultipliers = new double[1];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(0.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(-1.0, lagrangeEqualityMultipliers[0], 1e-7);
      assertEquals(1.0, lagrangeInequalityMultipliers[0], 1e-7);

      solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(1.0, objectiveCost, 1e-7);

      // Minimize x^2 + y^2 subject to x + y = 2.0, 3x - 3y = 0.0, x <= 2, x <= 10, y <= 3
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0 }, { 3.0, -3.0 } };
      linearEqualityConstraintsBVector = new double[] { 2.0, 0.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      linearInequalityConstraintsCMatrix = new double[][] { { 1.0, 0.0 }, { 1.0, 0.0 }, { 0.0, 1.0 } };
      linearInqualityConstraintsDVector = new double[] { 2.0, 10.0, 3.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[2];
      lagrangeInequalityMultipliers = new double[3];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(-2.0, lagrangeEqualityMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeEqualityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[2], 1e-7);

      solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(2.0, objectiveCost, 1e-7);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSimpleCasesWithBoundsConstraints()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      // Minimize x^T * x 
      double[][] costQuadraticMatrix = new double[][] { { 2.0 } };
      double[] costLinearVector = new double[] { 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[] variableLowerBounds = new double[] { Double.NEGATIVE_INFINITY };
      double[] variableUpperBounds = new double[] { Double.POSITIVE_INFINITY };
      solver.setVariableBounds(variableLowerBounds, variableUpperBounds);

      double[] solution = new double[1];
      double[] lagrangeEqualityMultipliers = new double[0];
      double[] lagrangeInequalityMultipliers = new double[0];
      double[] lagrangeLowerBoundMultipliers = new double[1];
      double[] lagrangeUpperBoundMultipliers = new double[1];

      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers, lagrangeLowerBoundMultipliers, lagrangeUpperBoundMultipliers);
      assertEquals(1, numberOfIterations);

      assertEquals(1, solution.length);
      assertEquals(0.0, solution[0], 1e-7);
      assertEquals(0.0, lagrangeLowerBoundMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeUpperBoundMultipliers[0], 1e-7);

      // Minimize x^T * x subject to x >= 1
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0 } };
      costLinearVector = new double[] { 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      variableLowerBounds = new double[] { 1.0 };
      variableUpperBounds = new double[] { Double.POSITIVE_INFINITY };
      solver.setVariableBounds(variableLowerBounds, variableUpperBounds);

      solution = new double[1];
      lagrangeEqualityMultipliers = new double[0];
      lagrangeInequalityMultipliers = new double[0];
      lagrangeLowerBoundMultipliers = new double[1];
      lagrangeUpperBoundMultipliers = new double[1];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers, lagrangeLowerBoundMultipliers, lagrangeUpperBoundMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(1, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(2.0, lagrangeLowerBoundMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeUpperBoundMultipliers[0], 1e-7);

      // Minimize x^2 + y^2 + z^2 subject to x + y = 2.0, y - z <= -8, -5 <= x <= 5, 6 <= y <= 10, 11 <= z 
      solver.clear();

      costQuadraticMatrix = new double[][] { { 2.0, 0.0, 0.0 }, { 0.0, 2.0, 0.0 }, { 0.0, 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0, 0.0 } };
      double[] linearEqualityConstraintsBVector = new double[] { 2.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { 0.0, 1.0, -1.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { -8.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solver.setVariableBounds(new double[] { -5.0, 6.0, 11.0 }, new double[] { 5.0, 10.0, Double.POSITIVE_INFINITY });

      solution = new double[3];
      lagrangeEqualityMultipliers = new double[1];
      lagrangeInequalityMultipliers = new double[1];
      lagrangeLowerBoundMultipliers = new double[3];
      lagrangeUpperBoundMultipliers = new double[3];

      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers, lagrangeLowerBoundMultipliers, lagrangeUpperBoundMultipliers);
      assertEquals(3, numberOfIterations);

      assertEquals(3, solution.length);
      assertEquals(-4.0, solution[0], 1e-7);
      assertEquals(6.0, solution[1], 1e-7);
      assertEquals(14.0, solution[2], 1e-7);
      assertEquals(8.0, lagrangeEqualityMultipliers[0], 1e-7);
      assertEquals(28.0, lagrangeInequalityMultipliers[0], 1e-7);

      assertEquals(0.0, lagrangeLowerBoundMultipliers[0], 1e-7);
      assertEquals(48.0, lagrangeLowerBoundMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeLowerBoundMultipliers[2], 1e-7);

      assertEquals(0.0, lagrangeUpperBoundMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeUpperBoundMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeUpperBoundMultipliers[2], 1e-7);

      DenseMatrix64F solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      double objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(248.0, objectiveCost, 1e-7);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testSolutionMethodsAreAllConsistent()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      // Minimize x^2 + y^2 subject to x + y = 2.0, y >= 0.5, y >= 3.0, y >= x-3  (-y <= -0.5, -y <= -3.0, x - y <= 3
      solver.clear();
      double[][] costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      double[] costLinearVector = new double[] { 0.0, 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0 } };
      double[] linearEqualityConstraintsBVector = new double[] { 2.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { 0.0, -1.0 }, { 0.0, -1.0 }, { 1.0, -1.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { -0.5, -3.0, 3.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      double[] solution = new double[2];
      double[] lagrangeEqualityMultipliers = new double[1];
      double[] lagrangeInequalityMultipliers = new double[3];
      double[] lagrangeBoundMultipliers = new double[3];
      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(-1.0, solution[0], 1e-7);
      assertEquals(3.0, solution[1], 1e-7);
      assertEquals(2.0, lagrangeEqualityMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(8.0, lagrangeInequalityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[2], 1e-7);

      DenseMatrix64F solutionMatrix = new DenseMatrix64F(costQuadraticMatrix.length, 1);
      solutionMatrix.setData(solution);
      double objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(10.0, objectiveCost, 1e-7);

      // Try with other solve method:
      solver.clear();
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);
      numberOfIterations = solver.solve(solution);

      assertEquals(2, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(-1.0, solution[0], 1e-7);
      assertEquals(3.0, solution[1], 1e-7);
      assertEquals(2.0, lagrangeEqualityMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(8.0, lagrangeInequalityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[2], 1e-7);

      solutionMatrix.setData(solution);
      objectiveCost = solver.getObjectiveCost(solutionMatrix);
      assertEquals(10.0, objectiveCost, 1e-7);

      // Try with other solve method:
      solver.clear();
      DenseMatrix64F quadraticCostMatrix64F = new DenseMatrix64F(costQuadraticMatrix);
      DenseMatrix64F linearCostVector64F = new DenseMatrix64F(costLinearVector.length, 1);
      linearCostVector64F.setData(costLinearVector);

      solver.setQuadraticCostFunction(quadraticCostMatrix64F, linearCostVector64F, quadraticCostScalar);
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      DenseMatrix64F solutionMatrix64F = new DenseMatrix64F(quadraticCostMatrix64F.getNumRows(), 1);
      DenseMatrix64F lagrangeEqualityMultipliers64F = new DenseMatrix64F(linearEqualityConstraintsAMatrix.length, 1);
      DenseMatrix64F lagrangeInequalityMultipliers64F = new DenseMatrix64F(linearInequalityConstraintsCMatrix.length, 1);
      DenseMatrix64F lagrangeBoundMultipliers64F = new DenseMatrix64F(0, 0);
      numberOfIterations = solver.solve(solutionMatrix64F, lagrangeEqualityMultipliers64F, lagrangeInequalityMultipliers64F);

      assertEquals(2, numberOfIterations);

      assertEquals(2, solutionMatrix64F.getNumRows());
      assertEquals(-1.0, solutionMatrix64F.get(0, 0), 1e-7);
      assertEquals(3.0, solutionMatrix64F.get(1, 0), 1e-7);
      assertEquals(2.0, lagrangeEqualityMultipliers64F.get(0, 0), 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers64F.get(0, 0), 1e-7);
      assertEquals(8.0, lagrangeInequalityMultipliers64F.get(1, 0), 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers64F.get(2, 0), 1e-7);

      objectiveCost = solver.getObjectiveCost(solutionMatrix64F);
      assertEquals(10.0, objectiveCost, 1e-7);

      // Try with other solve method:
      solver = createSolverToTest();

      solver.setQuadraticCostFunction(quadraticCostMatrix64F, linearCostVector64F, quadraticCostScalar);
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      DenseMatrix64F linearInequalityConstraintsCMatrix64F = new DenseMatrix64F(linearInequalityConstraintsCMatrix);
      DenseMatrix64F linearInqualityConstraintsDVector64F = new DenseMatrix64F(linearInqualityConstraintsDVector.length, 1);
      linearInqualityConstraintsDVector64F.setData(linearInqualityConstraintsDVector);
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix64F, linearInqualityConstraintsDVector64F);

      solutionMatrix64F.zero();
      numberOfIterations = solver.solve(solutionMatrix64F);

      assertEquals(2, numberOfIterations);

      assertEquals(2, solutionMatrix64F.getNumRows());
      assertEquals(-1.0, solutionMatrix64F.get(0, 0), 1e-7);
      assertEquals(3.0, solutionMatrix64F.get(1, 0), 1e-7);

      objectiveCost = solver.getObjectiveCost(solutionMatrix64F);
      assertEquals(10.0, objectiveCost, 1e-7);
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void test2DCasesWithPolygonConstraints()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      // Minimize x^2 + y^2 subject to 3 <= x <= 5, 2 <= y <= 4
      double[][] costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      double[] costLinearVector = new double[] { 0.0, 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { 1.0, 0.0 }, { -1.0, 0.0 }, { 0.0, 1.0 }, { 0.0, -1.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { 5.0, -3.0, 4.0, -2.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      double[] solution = new double[2];
      double[] lagrangeEqualityMultipliers = new double[0];
      double[] lagrangeInequalityMultipliers = new double[4];
      double[] lagrangeBoundMultipliers = new double[3];
      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(2, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(3.0, solution[0], 1e-7);
      assertEquals(2.0, solution[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(6.0, lagrangeInequalityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[2], 1e-7);
      assertEquals(4.0, lagrangeInequalityMultipliers[3], 1e-7);

      // Minimize x^2 + y^2 subject to x + y >= 2 (-x -y <= -2), y <= 10x - 2 (-10x + y <= -2)
      // Equality solution will violate both constraints, but optimal only has the first constraint active.
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearInequalityConstraintsCMatrix = new double[][] { { -1.0, -1.0 }, { -10.0, 1.0 } };
      linearInqualityConstraintsDVector = new double[] { -2.0, -2.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[0];
      lagrangeInequalityMultipliers = new double[2];
      lagrangeBoundMultipliers = new double[3];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(3, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(2.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[1], 1e-7);
   }

   @Ignore // This should pass with a good solver. But a simple one has trouble on it.  
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testChallengingCasesWithPolygonConstraints()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();
      solver.setMaxNumberOfIterations(10);

      // Minimize x^2 + y^2 subject to x + y >= 2 (-x -y <= -2), y <= 10x - 2 (-10x + y <= -2), x <= 10y - 2 (x - 10y <= -2), 
      // Equality solution will violate all three constraints, but optimal only has the first constraint active.
      // However, if you set all three constraints active, there is no solution.
      double[][] costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      double[] costLinearVector = new double[] { 0.0, 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { -1.0, -1.0 }, { -10.0, 1.0 }, { 1.0, -10.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { -2.0, -2.0, -2.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      double[] solution = new double[2];
      double[] lagrangeEqualityMultipliers = new double[0];
      double[] lagrangeInequalityMultipliers = new double[3];
      double[] lagrangeBoundMultipliers = new double[3];
      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(3, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(2.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[2], 1e-7);

      // Reorder and make sure it works:
      // Minimize x^2 + y^2 subject to x + y >= 2 (-x -y <= -2), y <= 10x - 2 (-10x + y <= -2), x <= 10y - 2 (x - 10y <= -2), 
      // Equality solution will violate all three constraints, but optimal only has the first constraint active.
      // However, if you set all three constraints active, there is no solution.
      solver.clear();
      costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      costLinearVector = new double[] { 0.0, 0.0 };
      quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      linearInequalityConstraintsCMatrix = new double[][] { { -10.0, 1.0 }, { -1.0, -1.0 }, { 1.0, -10.0 } };
      linearInqualityConstraintsDVector = new double[] { -2.0, -2.0, -2.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      solution = new double[2];
      lagrangeEqualityMultipliers = new double[0];
      lagrangeInequalityMultipliers = new double[3];
      lagrangeBoundMultipliers = new double[3];
      numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      assertEquals(3, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(1.0, solution[0], 1e-7);
      assertEquals(1.0, solution[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[0], 1e-7);
      assertEquals(2.0, lagrangeInequalityMultipliers[1], 1e-7);
      assertEquals(0.0, lagrangeInequalityMultipliers[2], 1e-7);
   }

   // This should pass with a good solver. But a simple one has trouble on it.  
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testChallengingCasesWithPolygonConstraintsCheckFailsWithSimpleSolver()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();
      solver.setMaxNumberOfIterations(10);

      // Minimize x^2 + y^2 subject to x + y >= 2 (-x -y <= -2), y <= 10x - 2 (-10x + y <= -2), x <= 10y - 2 (x - 10y <= -2), 
      // Equality solution will violate all three constraints, but optimal only has the first constraint active.
      // However, if you set all three constraints active, there is no solution.
      double[][] costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      double[] costLinearVector = new double[] { 0.0, 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { -1.0, -1.0 }, { -10.0, 1.0 }, { 1.0, -10.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { -2.0, -2.0, -2.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      double[] solution = new double[2];
      double[] lagrangeEqualityMultipliers = new double[0];
      double[] lagrangeInequalityMultipliers = new double[3];
      double[] lagrangeBoundMultipliers = new double[3];
      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      //      assertEquals(3, numberOfIterations);

      assertEquals(2, solution.length);
      assertTrue(Double.isNaN(solution[0]));
      assertTrue(Double.isNaN(solution[1]));
      assertTrue(Double.isInfinite(lagrangeInequalityMultipliers[0]) || Double.isNaN(lagrangeInequalityMultipliers[0]));
      assertTrue(Double.isInfinite(lagrangeInequalityMultipliers[1]) || Double.isNaN(lagrangeInequalityMultipliers[1]));
      assertTrue(Double.isInfinite(lagrangeInequalityMultipliers[2]) || Double.isNaN(lagrangeInequalityMultipliers[2]));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testCaseWithNoSolution()
   {
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();
      int maxNumberOfIterations = 10;
      solver.setMaxNumberOfIterations(maxNumberOfIterations);

      // Minimize x^2 + y^2 subject to x + y = 5, x + y <= 2 
      double[][] costQuadraticMatrix = new double[][] { { 2.0, 0.0 }, { 0.0, 2.0 } };
      double[] costLinearVector = new double[] { 0.0, 0.0 };
      double quadraticCostScalar = 0.0;
      solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

      double[][] linearEqualityConstraintsAMatrix = new double[][] { { 1.0, 1.0 } };
      double[] linearEqualityConstraintsBVector = new double[] { 5.0 };
      solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

      double[][] linearInequalityConstraintsCMatrix = new double[][] { { 1.0, 1.0 } };
      double[] linearInqualityConstraintsDVector = new double[] { 2.0 };
      solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInqualityConstraintsDVector);

      double[] solution = new double[2];
      double[] lagrangeEqualityMultipliers = new double[1];
      double[] lagrangeInequalityMultipliers = new double[1];
      double[] lagrangeBoundMultipliers = new double[1];
      int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
      //      assertEquals(maxNumberOfIterations, numberOfIterations);

      assertEquals(2, solution.length);
      assertEquals(Double.NaN, solution[0], 1e-7);
      assertEquals(Double.NaN, solution[1], 1e-7);
      assertTrue(Double.isInfinite(lagrangeEqualityMultipliers[0]));
      assertTrue(Double.isInfinite(lagrangeInequalityMultipliers[0]));
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testLargeRandomProblemWithInequalityConstraints()
   {
      Random random = new Random(1776L);

      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      int numberOfTests = 100;

      long startTimeMillis = System.currentTimeMillis();
      int maxNumberOfIterations = 0;

      int numberOfVariables = 80;
      int numberOfEqualityConstraints = 10;
      int numberOfInequalityConstraints = 36;

      DenseMatrix64F solution = new DenseMatrix64F(numberOfVariables, 1);
      DenseMatrix64F lagrangeEqualityMultipliers = new DenseMatrix64F(numberOfEqualityConstraints, 1);
      DenseMatrix64F lagrangeInequalityMultipliers = new DenseMatrix64F(numberOfInequalityConstraints, 1);
      DenseMatrix64F lagrangeBoundMultipliers = new DenseMatrix64F(0, 0);
      double[] solutionWithSmallPerturbation = new double[numberOfVariables];

      DenseMatrix64F augmentedLinearEqualityConstraintsAMatrix = new DenseMatrix64F(0, 0);
      DenseMatrix64F augmentedLinearEqualityConstraintsBVector = new DenseMatrix64F(0, 0);

      for (int testNumber = 0; testNumber < numberOfTests; testNumber++)
      {
         solver.clear();

         DenseMatrix64F costQuadraticMatrix = RandomTools.generateRandomMatrix(random, numberOfVariables, numberOfVariables);
         DenseMatrix64F identity = CommonOps.identity(numberOfVariables, numberOfVariables); // Add n*I to make sure it is positive definite...
         CommonOps.scale(numberOfVariables, identity);
         CommonOps.addEquals(costQuadraticMatrix, identity);

         DenseMatrix64F costLinearVector = RandomTools.generateRandomMatrix(random, numberOfVariables, 1);
         double quadraticCostScalar = RandomTools.generateRandomDouble(random, 30.0);

         solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

         DenseMatrix64F linearEqualityConstraintsAMatrix = RandomTools.generateRandomMatrix(random, numberOfEqualityConstraints, numberOfVariables);
         DenseMatrix64F linearEqualityConstraintsBVector = RandomTools.generateRandomMatrix(random, numberOfEqualityConstraints, 1);
         solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

         DenseMatrix64F linearInequalityConstraintsCMatrix = RandomTools.generateRandomMatrix(random, numberOfInequalityConstraints, numberOfVariables);
         DenseMatrix64F linearInequalityConstraintsDVector = RandomTools.generateRandomMatrix(random, numberOfInequalityConstraints, 1);
         solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector);

         int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers);
         if (numberOfIterations > maxNumberOfIterations)
            maxNumberOfIterations = numberOfIterations;
         //         System.out.println("numberOfIterations = " + numberOfIterations);

         assertEquals(numberOfVariables, solution.getNumRows());
         assertEquals(numberOfEqualityConstraints, lagrangeEqualityMultipliers.getNumRows());

         double objectiveCost = solver.getObjectiveCost(solution);

         // Verify constraints hold:
         verifyEqualityConstraintsHold(numberOfEqualityConstraints, linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector, solution);
         verifyInequalityConstraintsHold(numberOfInequalityConstraints, linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector, solution);

         // Verify objective is minimized by comparing to small perturbation:
         for (int i = 0; i < numberOfVariables; i++)
         {
            solutionWithSmallPerturbation[i] = solution.get(i, 0) + RandomTools.generateRandomDouble(random, 1e-4);
         }

         solution.zero();
         solution.setData(solutionWithSmallPerturbation);

         verifyEqualityConstraintsDoNotHold(numberOfEqualityConstraints, linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector, solution);

         // Equality constraints usually do not hold. Sometimes they do, so if you run with lots of numberOfTests, comment out the following:
         verifyInequalityConstraintsDoNotHold(numberOfInequalityConstraints, linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector, solution);

         int activeSetSize = 0;
         for (int i = 0; i < numberOfInequalityConstraints; i++)
         {
            double lagrangeMultiplier = lagrangeInequalityMultipliers.get(i, 0);

            if (lagrangeMultiplier < 0.0)
            {
               throw new RuntimeException();
            }
            if (lagrangeMultiplier > 0.0)
            {
               activeSetSize++;
            }
         }

         augmentedLinearEqualityConstraintsAMatrix.reshape(numberOfEqualityConstraints + activeSetSize, numberOfVariables);
         augmentedLinearEqualityConstraintsBVector.reshape(numberOfEqualityConstraints + activeSetSize, 1);

         CommonOps.extract(linearEqualityConstraintsAMatrix, 0, numberOfEqualityConstraints, 0, numberOfVariables, augmentedLinearEqualityConstraintsAMatrix, 0, 0);
         CommonOps.extract(linearEqualityConstraintsBVector, 0, numberOfEqualityConstraints, 0, 1, augmentedLinearEqualityConstraintsBVector, 0, 0);

         int index = 0;
         for (int i = 0; i < numberOfInequalityConstraints; i++)
         {
            double lagrangeMultiplier = lagrangeInequalityMultipliers.get(i, 0);

            if (lagrangeMultiplier < 0.0)
            {
               throw new RuntimeException();
            }
            if (lagrangeMultiplier > 0.0)
            {
               CommonOps.extract(linearInequalityConstraintsCMatrix, i, i + 1, 0, numberOfVariables, augmentedLinearEqualityConstraintsAMatrix, numberOfEqualityConstraints + index, 0);
               CommonOps.extract(linearInequalityConstraintsDVector, i, i + 1, 0, 1, augmentedLinearEqualityConstraintsBVector, numberOfEqualityConstraints + index, 0);
               index++;
            }
         }

         DenseMatrix64F solutionMatrixProjectedOntoEqualityConstraints = projectOntoEqualityConstraints(solution, augmentedLinearEqualityConstraintsAMatrix, augmentedLinearEqualityConstraintsBVector);
         verifyEqualityConstraintsHold(numberOfEqualityConstraints + activeSetSize, augmentedLinearEqualityConstraintsAMatrix, augmentedLinearEqualityConstraintsBVector, solutionMatrixProjectedOntoEqualityConstraints);

         double objectiveCostWithSmallPerturbation = solver.getObjectiveCost(solutionMatrixProjectedOntoEqualityConstraints);

         assertTrue("objectiveCostWithSmallPerturbation = " + objectiveCostWithSmallPerturbation + ", objectiveCost = " + objectiveCost, objectiveCostWithSmallPerturbation > objectiveCost);
      }

      long endTimeMillis = System.currentTimeMillis();

      double timePerTest = ((double) (endTimeMillis - startTimeMillis)) * 0.001 / ((double) numberOfTests);
      if (VERBOSE)
      {
         System.out.println("Time per test is " + timePerTest);
         System.out.println("maxNumberOfIterations is " + maxNumberOfIterations);
      }
   }

   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testLargeRandomProblemWithInequalityAndBoundsConstraints()
   {
      Random random = new Random(1776L);

      SimpleActiveSetQPSolverInterface solver = createSolverToTest();

      int numberOfTests = 100;

      long startTimeMillis = System.currentTimeMillis();
      int maxNumberOfIterations = 0;

      int numberOfVariables = 80;
      int numberOfEqualityConstraints = 10;
      int numberOfInequalityConstraints = 36;

      DenseMatrix64F solution = new DenseMatrix64F(numberOfVariables, 1);
      DenseMatrix64F lagrangeEqualityMultipliers = new DenseMatrix64F(0, 0);
      DenseMatrix64F lagrangeInequalityMultipliers = new DenseMatrix64F(0, 0);
      DenseMatrix64F lagrangeLowerBoundMultipliers = new DenseMatrix64F(0, 0);
      DenseMatrix64F lagrangeUpperBoundMultipliers = new DenseMatrix64F(0, 0);
      double[] solutionWithSmallPerturbation = new double[numberOfVariables];

      DenseMatrix64F augmentedLinearEqualityConstraintsAMatrix = new DenseMatrix64F(0, 0);
      DenseMatrix64F augmentedLinearEqualityConstraintsBVector = new DenseMatrix64F(0, 0);

      int numberOfNaNSolutions = 0;
      for (int testNumber = 0; testNumber < numberOfTests; testNumber++)
      {
         //         System.out.println("testNumber = " + testNumber);
         solver.clear();

         DenseMatrix64F costQuadraticMatrix = RandomTools.generateRandomMatrix(random, numberOfVariables, numberOfVariables);
         DenseMatrix64F identity = CommonOps.identity(numberOfVariables, numberOfVariables); // Add n*I to make sure it is positive definite...
         CommonOps.scale(numberOfVariables, identity);
         CommonOps.addEquals(costQuadraticMatrix, identity);

         DenseMatrix64F costLinearVector = RandomTools.generateRandomMatrix(random, numberOfVariables, 1);
         double quadraticCostScalar = RandomTools.generateRandomDouble(random, 30.0);

         solver.setQuadraticCostFunction(costQuadraticMatrix, costLinearVector, quadraticCostScalar);

         DenseMatrix64F linearEqualityConstraintsAMatrix = RandomTools.generateRandomMatrix(random, numberOfEqualityConstraints, numberOfVariables);
         DenseMatrix64F linearEqualityConstraintsBVector = RandomTools.generateRandomMatrix(random, numberOfEqualityConstraints, 1);
         solver.setLinearEqualityConstraints(linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector);

         DenseMatrix64F linearInequalityConstraintsCMatrix = RandomTools.generateRandomMatrix(random, numberOfInequalityConstraints, numberOfVariables);
         DenseMatrix64F linearInequalityConstraintsDVector = RandomTools.generateRandomMatrix(random, numberOfInequalityConstraints, 1);
         solver.setLinearInequalityConstraints(linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector);

         DenseMatrix64F variableLowerBounds = RandomTools.generateRandomMatrix(random, numberOfVariables, 1, -5.0, -0.01);
         DenseMatrix64F variableUpperBounds = RandomTools.generateRandomMatrix(random, numberOfVariables, 1, 0.01, 5.0);
         solver.setVariableBounds(variableLowerBounds, variableUpperBounds);

         int numberOfIterations = solver.solve(solution, lagrangeEqualityMultipliers, lagrangeInequalityMultipliers, lagrangeLowerBoundMultipliers, lagrangeUpperBoundMultipliers);
         if (numberOfIterations > maxNumberOfIterations)
            maxNumberOfIterations = numberOfIterations;

         //         System.out.println(solution);
         //         System.out.println("numberOfIterations = " + numberOfIterations);

         assertEquals(numberOfVariables, solution.getNumRows());
         assertEquals(numberOfEqualityConstraints, lagrangeEqualityMultipliers.getNumRows());
         assertEquals(numberOfInequalityConstraints, lagrangeInequalityMultipliers.getNumRows());
         assertEquals(variableLowerBounds.getNumRows(), lagrangeLowerBoundMultipliers.getNumRows());
         assertEquals(variableUpperBounds.getNumRows(), lagrangeUpperBoundMultipliers.getNumRows());

         if (Double.isNaN(solution.get(0)))
         {
            numberOfNaNSolutions++;
            continue;
         }

         double objectiveCost = solver.getObjectiveCost(solution);

         // Verify constraints hold:
         verifyEqualityConstraintsHold(numberOfEqualityConstraints, linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector, solution);
         verifyInequalityConstraintsHold(numberOfInequalityConstraints, linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector, solution);
         verifyVariableBoundsHold(variableLowerBounds, variableUpperBounds, solution);

         // Verify objective is minimized by comparing to small perturbation:
         for (int i = 0; i < numberOfVariables; i++)
         {
            solutionWithSmallPerturbation[i] = solution.get(i, 0) + RandomTools.generateRandomDouble(random, 1e-4);
         }

         solution.zero();
         solution.setData(solutionWithSmallPerturbation);

         verifyEqualityConstraintsDoNotHold(numberOfEqualityConstraints, linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector, solution);

         // Equality constraints usually do not hold. Sometimes they do, so if you run with lots of numberOfTests, comment out the following:
         verifyInequalityConstraintsDoNotHold(numberOfInequalityConstraints, linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector, solution);

         int activeInequalitiesSize = 0;
         for (int i = 0; i < numberOfInequalityConstraints; i++)
         {
            double lagrangeMultiplier = lagrangeInequalityMultipliers.get(i, 0);

            if (lagrangeMultiplier < 0.0)
            {
               throw new RuntimeException();
            }
            if (lagrangeMultiplier > 0.0)
            {
               activeInequalitiesSize++;
            }
         }

         int activeLowerBoundsSize = 0;
         for (int i = 0; i < variableLowerBounds.getNumRows(); i++)
         {
            double lagrangeMultiplier = lagrangeLowerBoundMultipliers.get(i, 0);

            if (lagrangeMultiplier < 0.0)
            {
               throw new RuntimeException();
            }
            if (lagrangeMultiplier > 0.0)
            {
               activeLowerBoundsSize++;
            }
         }

         int activeUpperBoundsSize = 0;
         for (int i = 0; i < variableUpperBounds.getNumRows(); i++)
         {
            double lagrangeMultiplier = lagrangeUpperBoundMultipliers.get(i, 0);

            if (lagrangeMultiplier < 0.0)
            {
               throw new RuntimeException();
            }
            if (lagrangeMultiplier > 0.0)
            {
               activeUpperBoundsSize++;
            }
         }

         augmentedLinearEqualityConstraintsAMatrix.reshape(numberOfEqualityConstraints + activeInequalitiesSize + activeLowerBoundsSize + activeUpperBoundsSize, numberOfVariables);
         augmentedLinearEqualityConstraintsBVector.reshape(numberOfEqualityConstraints + activeInequalitiesSize + activeLowerBoundsSize + activeUpperBoundsSize, 1);
         augmentedLinearEqualityConstraintsAMatrix.zero();
         augmentedLinearEqualityConstraintsBVector.zero();

         CommonOps.extract(linearEqualityConstraintsAMatrix, 0, numberOfEqualityConstraints, 0, numberOfVariables, augmentedLinearEqualityConstraintsAMatrix, 0, 0);
         CommonOps.extract(linearEqualityConstraintsBVector, 0, numberOfEqualityConstraints, 0, 1, augmentedLinearEqualityConstraintsBVector, 0, 0);

         int index = 0;
         for (int i = 0; i < numberOfInequalityConstraints; i++)
         {
            double lagrangeMultiplier = lagrangeInequalityMultipliers.get(i, 0);

            if (lagrangeMultiplier < 0.0)
            {
               throw new RuntimeException();
            }
            if (lagrangeMultiplier > 0.0)
            {
               CommonOps.extract(linearInequalityConstraintsCMatrix, i, i + 1, 0, numberOfVariables, augmentedLinearEqualityConstraintsAMatrix, numberOfEqualityConstraints + index, 0);
               CommonOps.extract(linearInequalityConstraintsDVector, i, i + 1, 0, 1, augmentedLinearEqualityConstraintsBVector, numberOfEqualityConstraints + index, 0);
               index++;
            }
         }

         for (int i = 0; i < variableLowerBounds.getNumRows(); i++)
         {
            double lagrangeMultiplier = lagrangeLowerBoundMultipliers.get(i, 0);

            if (lagrangeMultiplier > 0.0)
            {
               augmentedLinearEqualityConstraintsAMatrix.set(numberOfEqualityConstraints + index, i, -1.0);
               augmentedLinearEqualityConstraintsBVector.set(numberOfEqualityConstraints + index, -variableLowerBounds.get(i));
               index++;
            }
         }

         for (int i = 0; i < variableUpperBounds.getNumRows(); i++)
         {
            double lagrangeMultiplier = lagrangeUpperBoundMultipliers.get(i, 0);

            if (lagrangeMultiplier > 0.0)
            {
               augmentedLinearEqualityConstraintsAMatrix.set(numberOfEqualityConstraints + index, i, 1.0);
               augmentedLinearEqualityConstraintsBVector.set(numberOfEqualityConstraints + index, variableUpperBounds.get(i));
               index++;
            }
         }

         assertTrue(index == activeInequalitiesSize + activeLowerBoundsSize + activeUpperBoundsSize);

         DenseMatrix64F solutionMatrixProjectedOntoEqualityConstraints = projectOntoEqualityConstraints(solution, augmentedLinearEqualityConstraintsAMatrix, augmentedLinearEqualityConstraintsBVector);
         verifyEqualityConstraintsHold(numberOfEqualityConstraints + activeInequalitiesSize + activeLowerBoundsSize + activeUpperBoundsSize, augmentedLinearEqualityConstraintsAMatrix,
               augmentedLinearEqualityConstraintsBVector, solutionMatrixProjectedOntoEqualityConstraints);

         double objectiveCostWithSmallPerturbation = solver.getObjectiveCost(solutionMatrixProjectedOntoEqualityConstraints);

         assertTrue("objectiveCostWithSmallPerturbation = " + objectiveCostWithSmallPerturbation + ", objectiveCost = " + objectiveCost, objectiveCostWithSmallPerturbation > objectiveCost);
      }

      assertTrue(numberOfNaNSolutions < numberOfTests / 2);

      long endTimeMillis = System.currentTimeMillis();

      double timePerTest = ((double) (endTimeMillis - startTimeMillis)) * 0.001 / ((double) numberOfTests);
      if (VERBOSE)
      {
         System.out.println("Time per test is " + timePerTest);
         System.out.println("maxNumberOfIterations is " + maxNumberOfIterations);
         System.out.println("numberOfNaNSolutions = " + numberOfNaNSolutions);
         System.out.println("numberOfTests = " + numberOfTests);

      }
   }

   /**
    *  Test with dataset from sim that revealed a bug with the variable lower/upper bounds handling.
    */
   @DeployableTestMethod(estimatedDuration = 0.0)
   @Test(timeout = 30000)
   public void testFindValidSolutionForDataset20160319() throws Exception
   {
      ActualDatasetFrom20160319 dataset = new ActualDatasetFrom20160319();
      SimpleActiveSetQPSolverInterface solver = createSolverToTest();
      solver.clear();
      solver.setQuadraticCostFunction(dataset.getCostQuadraticMatrix(), dataset.getCostLinearVector(), 0.0);
      solver.setVariableBounds(dataset.getVariableLowerBounds(), dataset.getVariableUpperBounds());
      DenseMatrix64F solution = new DenseMatrix64F(dataset.getProblemSize(), 1);
      solver.solve(solution);

      assertFalse(MatrixTools.containsNaN(solution));
   }

   private void verifyEqualityConstraintsHold(int numberOfEqualityConstraints, DenseMatrix64F linearEqualityConstraintsAMatrix, DenseMatrix64F linearEqualityConstraintsBVector, DenseMatrix64F solutionMatrix)
   {
      double maxAbsoluteError = getMaxEqualityConstraintError(numberOfEqualityConstraints, linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector, solutionMatrix);
      assertEquals(0.0, maxAbsoluteError, 1e-5);
   }

   private void verifyInequalityConstraintsHold(int numberOfEqualityConstraints, DenseMatrix64F linearInequalityConstraintsCMatrix, DenseMatrix64F linearInequalityConstraintsDVector, DenseMatrix64F solutionMatrix)
   {
      double maxSignedError = getMaxInequalityConstraintError(numberOfEqualityConstraints, linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector, solutionMatrix);
      assertTrue(maxSignedError < 1e-10);
   }

   private void verifyEqualityConstraintsDoNotHold(int numberOfEqualityConstraints, DenseMatrix64F linearEqualityConstraintsAMatrix, DenseMatrix64F linearEqualityConstraintsBVector, DenseMatrix64F solutionMatrix)
   {
      double maxAbsoluteError = getMaxEqualityConstraintError(numberOfEqualityConstraints, linearEqualityConstraintsAMatrix, linearEqualityConstraintsBVector, solutionMatrix);
      assertTrue(maxAbsoluteError > 1e-5);
   }

   private void verifyInequalityConstraintsDoNotHold(int numberOfInequalityConstraints, DenseMatrix64F linearInequalityConstraintsCMatrix, DenseMatrix64F linearInequalityConstraintsDVector, DenseMatrix64F solutionMatrix)
   {
      double maxError = getMaxInequalityConstraintError(numberOfInequalityConstraints, linearInequalityConstraintsCMatrix, linearInequalityConstraintsDVector, solutionMatrix);
      assertTrue(maxError > 1e-5);
   }

   private void verifyVariableBoundsHold(DenseMatrix64F variableLowerBounds, DenseMatrix64F variableUpperBounds, DenseMatrix64F solution)
   {
      for (int i = 0; i < variableLowerBounds.getNumRows(); i++)
      {
         assertTrue(solution.get(i, 0) >= variableLowerBounds.get(i, 0) - 1e-7);
      }

      for (int i = 0; i < variableUpperBounds.getNumRows(); i++)
      {
         assertTrue(solution.get(i, 0) <= variableUpperBounds.get(i, 0) + 1e-7);
      }
   }

   private double getMaxEqualityConstraintError(int numberOfEqualityConstraints, DenseMatrix64F linearEqualityConstraintsAMatrix, DenseMatrix64F linearEqualityConstraintsBVector, DenseMatrix64F solutionMatrix)
   {
      DenseMatrix64F checkMatrix = new DenseMatrix64F(numberOfEqualityConstraints, 1);
      CommonOps.mult(linearEqualityConstraintsAMatrix, solutionMatrix, checkMatrix);
      CommonOps.subtractEquals(checkMatrix, linearEqualityConstraintsBVector);

      return getMaxAbsoluteDataEntry(checkMatrix);
   }

   private double getMaxInequalityConstraintError(int numberOfInequalityConstraints, DenseMatrix64F linearInequalityConstraintsCMatrix, DenseMatrix64F linearInequalityConstraintsDVector, DenseMatrix64F solutionMatrix)
   {
      DenseMatrix64F checkMatrix = new DenseMatrix64F(numberOfInequalityConstraints, 1);
      CommonOps.mult(linearInequalityConstraintsCMatrix, solutionMatrix, checkMatrix);
      CommonOps.subtractEquals(checkMatrix, linearInequalityConstraintsDVector);

      return getMaxSignedDataEntry(checkMatrix);
   }

   private DenseMatrix64F projectOntoEqualityConstraints(DenseMatrix64F solutionMatrix, DenseMatrix64F linearEqualityConstraintsAMatrix, DenseMatrix64F linearEqualityConstraintsBVector)
   {
      int numberOfVariables = solutionMatrix.getNumRows();
      if (linearEqualityConstraintsAMatrix.getNumCols() != numberOfVariables)
         throw new RuntimeException();

      int numberOfConstraints = linearEqualityConstraintsAMatrix.getNumRows();
      if (linearEqualityConstraintsBVector.getNumRows() != numberOfConstraints)
         throw new RuntimeException();

      DenseMatrix64F AZMinusB = new DenseMatrix64F(numberOfConstraints, 1);
      CommonOps.mult(linearEqualityConstraintsAMatrix, solutionMatrix, AZMinusB);
      CommonOps.subtractEquals(AZMinusB, linearEqualityConstraintsBVector);

      DenseMatrix64F AATransposeInverse = new DenseMatrix64F(numberOfConstraints, numberOfConstraints);
      DenseMatrix64F linearEqualityConstraintsAMatrixTranspose = new DenseMatrix64F(linearEqualityConstraintsAMatrix);
      CommonOps.transpose(linearEqualityConstraintsAMatrixTranspose);

      CommonOps.mult(linearEqualityConstraintsAMatrix, linearEqualityConstraintsAMatrixTranspose, AATransposeInverse);
      CommonOps.invert(AATransposeInverse);

      DenseMatrix64F ATransposeAATransposeInverse = new DenseMatrix64F(numberOfVariables, numberOfConstraints);
      CommonOps.mult(linearEqualityConstraintsAMatrixTranspose, AATransposeInverse, ATransposeAATransposeInverse);

      DenseMatrix64F vectorToSubtract = new DenseMatrix64F(numberOfVariables, 1);
      CommonOps.mult(ATransposeAATransposeInverse, AZMinusB, vectorToSubtract);

      DenseMatrix64F projectedSolutionMatrix = new DenseMatrix64F(solutionMatrix);
      CommonOps.subtractEquals(projectedSolutionMatrix, vectorToSubtract);

      return projectedSolutionMatrix;
   }

   private double getMaxAbsoluteDataEntry(DenseMatrix64F matrix)
   {
      int numberOfRows = matrix.getNumRows();
      int numberOfColumns = matrix.getNumCols();

      double max = Double.NEGATIVE_INFINITY;

      for (int i = 0; i < numberOfRows; i++)
      {
         for (int j = 0; j < numberOfColumns; j++)
         {
            double absoluteValue = Math.abs(matrix.get(i, j));
            if (absoluteValue > max)
            {
               max = absoluteValue;
            }
         }
      }

      return max;
   }

   private double getMaxSignedDataEntry(DenseMatrix64F matrix)
   {
      int numberOfRows = matrix.getNumRows();
      int numberOfColumns = matrix.getNumCols();

      double max = Double.NEGATIVE_INFINITY;

      for (int i = 0; i < numberOfRows; i++)
      {
         for (int j = 0; j < numberOfColumns; j++)
         {
            double value = matrix.get(i, j);
            if (value > max)
            {
               max = value;
            }
         }
      }

      return max;
   }

}
