package us.ihmc.convexOptimization.quadraticProgram;

import org.ejml.data.DenseMatrix64F;
import org.junit.Assert;
import org.junit.Test;

import us.ihmc.convexOptimization.quadraticProgram.CompositeActiveSetQPSolver;
import us.ihmc.convexOptimization.quadraticProgram.ConstrainedQPSolver;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.tools.exceptions.NoConvergenceException;
import us.ihmc.tools.testing.TestPlanAnnotations.DeployableTestMethod;

public class ConstrainedQPSolverTest
{

   @DeployableTestMethod(estimatedDuration = 0.2)
   @Test(timeout = 30000)
   public void testSolveContrainedQP() throws NoConvergenceException
   {
      YoVariableRegistry registry = new YoVariableRegistry("root");
      int numberOfInequalityConstraints = 1;
      int numberOfEqualityConstraints = 1;
      int numberOfVariables = 2;

      DenseMatrix64F Q = new DenseMatrix64F(numberOfVariables, numberOfVariables, true, 1, 0, 0, 1);
      DenseMatrix64F f = new DenseMatrix64F(numberOfVariables, 1, true, 1, 0);
      DenseMatrix64F Aeq = new DenseMatrix64F(numberOfEqualityConstraints, numberOfVariables, true, 1, 1);
      DenseMatrix64F beq = new DenseMatrix64F(numberOfEqualityConstraints, 1, true, 0);
      DenseMatrix64F Ain = new DenseMatrix64F(numberOfInequalityConstraints, numberOfVariables, true, 2, 1);
      DenseMatrix64F bin = new DenseMatrix64F(numberOfInequalityConstraints, 1, true, 0);

      ConstrainedQPSolver[] optimizers = { //new JOptimizerConstrainedQPSolver(),
            new OASESConstrainedQPSolver(registry),
            new QuadProgSolver(registry),
            new CompositeActiveSetQPSolver(registry)
            };

      for (int repeat = 0; repeat < 10000; repeat++)
      {
         for (int i = 0; i < optimizers.length; i++)
         {
            DenseMatrix64F x = new DenseMatrix64F(numberOfVariables, 1, true, -1, 1);
            optimizers[i].solve(Q, f, Aeq, beq, Ain, bin, x, false);
            Assert.assertArrayEquals(x.getData(), new double[] { -0.5, 0.5 }, 1e-10);
         }
      }

      registry = new YoVariableRegistry("root");

      //TODO: Need more test cases. Can't trust these QP solvers without them...
      optimizers = new ConstrainedQPSolver[]{ //new JOptimizerConstrainedQPSolver(),
            new OASESConstrainedQPSolver(registry),
//            new QuadProgSolver(registry),
//            new CompositeActiveSetQPSolver(registry)
            };

      numberOfInequalityConstraints = 1;
      numberOfEqualityConstraints = 2;
      numberOfVariables = 3;

      Q = new DenseMatrix64F(numberOfVariables, numberOfVariables, true, 1, 0, 1, 0, 1, 2, 1, 3, 7);
      f = new DenseMatrix64F(numberOfVariables, 1, true, 1, 0, 9);
      Aeq = new DenseMatrix64F(numberOfEqualityConstraints, numberOfVariables, true, 1, 1, 1, 2, 3, 4);
      beq = new DenseMatrix64F(numberOfEqualityConstraints, 1, true, 0, 7);
      Ain = new DenseMatrix64F(numberOfInequalityConstraints, numberOfVariables, true, 2, 1, 3);
      bin = new DenseMatrix64F(numberOfInequalityConstraints, 1, true, 0);

      for (int repeat = 0; repeat < 10000; repeat++)
      {
         for (int i = 0; i < optimizers.length; i++)
         {
            DenseMatrix64F x = new DenseMatrix64F(numberOfVariables, 1, true, -1, 1, 3);
            optimizers[i].solve(Q, f, Aeq, beq, Ain, bin, x, false);
            Assert.assertArrayEquals("repeat = " + repeat + ", optimizer = " + i, x.getData(), new double[] { -7.75, 8.5, -0.75 }, 1e-10);
         }
      }
   }

}
