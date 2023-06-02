package us.ihmc.robotics.optimization.constrainedOptimization;

import org.ejml.data.DMatrixD1;
import org.ejml.data.DMatrixRMaj;
import us.ihmc.robotics.optimization.Optimizer;

/**
 * Solves an {@link AugmentedLagrangeOptimizationProblem} iteratively
 * Each iteration updates the lagrange multipliers to better satisfy the constraints
 *
 * The optimizer used to solve the unconstrained lagrangian problem needs to be specified separately
 */
public class AugmentedLagrangeOptimizer
{
   private final AugmentedLagrangeOptimizationProblem problem;
   private final CostFunction lagrangeCostFunction;
   private final Optimizer optimizer;

   private double optimumCost;
   private DMatrixD1 optimumParameters = new DMatrixRMaj();

   private boolean verbose = true;

   public AugmentedLagrangeOptimizer(Optimizer optimizer, AugmentedLagrangeOptimizationProblem problem)
   {
      this.problem = problem;
      this.lagrangeCostFunction = problem.getAugmentedCostFunction();

      this.optimizer = optimizer;
   }

   /**
    * Print diagnostic information during optimization
    */
   public void setVerbose(boolean verbose)
   {
      this.verbose = verbose;
   }

   public DMatrixD1 optimize(int numLagrangeIterations, DMatrixD1 initialParam)
   {
      DMatrixD1 initial = new DMatrixRMaj(initialParam);
      int iteration = 0;
      while (iteration < numLagrangeIterations)
      {
         optimizer.setCostFunction(lagrangeCostFunction);
         optimumParameters = optimizer.optimize(initial);
         optimumCost = optimizer.getOptimumCost();

         problem.updateLagrangeMultipliers(optimumParameters);
         initial.set(optimumParameters);

         if (verbose)
         {
            System.out.println("===== Lagrange Iteration: " + iteration + " ==========");
            problem.printResults(optimumParameters);
         }
         iteration += 1;
      }

      return optimumParameters;
   }

   public double getOptimumCost()
   {
      return optimumCost;
   }
}
