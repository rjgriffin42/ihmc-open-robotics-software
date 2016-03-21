package us.ihmc.convexOptimization.quadraticProgram;

public class SimpleEfficientActiveSetQPSolverTest extends AbstractSimpleActiveSetQPSolverTest
{
   @Override
   public SimpleActiveSetQPSolverInterface createSolverToTest()
   {
      return new SimpleEfficientActiveSetQPSolver();
   }
}
