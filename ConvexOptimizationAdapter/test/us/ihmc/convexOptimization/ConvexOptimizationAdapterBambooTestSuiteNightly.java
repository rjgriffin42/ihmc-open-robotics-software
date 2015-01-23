package us.ihmc.convexOptimization;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import us.ihmc.utilities.code.unitTesting.JUnitTestSuiteConstructor;

@RunWith(Suite.class)
@Suite.SuiteClasses
({
//   us.ihmc.convexOptimization.experimental.ExperimentalSOCPSolverUsingJOptimizerTest.class,
//   us.ihmc.convexOptimization.jOptimizer.JOptimizerConvexOptimizationAdapterTest.class,
//   us.ihmc.convexOptimization.jOptimizer.SimpleJOptimizerTest.class,
   us.ihmc.convexOptimization.quadraticProgram.GenericActiveSetQPSolverTest.class,
   us.ihmc.convexOptimization.quadraticProgram.SimpleActiveSetQPSolverTest.class,
//   us.ihmc.convexOptimization.randomSearch.RandomSearchConvexOptimizationAdapterTest.class
})

public class ConvexOptimizationAdapterBambooTestSuiteNightly
{
   public static void main(String[] args)
   {
      JUnitTestSuiteConstructor.generateTestSuite(ConvexOptimizationAdapterBambooTestSuiteNightly.class);
   }
}

