package us.ihmc.convexOptimization.jOptimizer;

import us.ihmc.convexOptimization.ConvexOptimizationAdapter;
import us.ihmc.convexOptimization.ConvexOptimizationAdapterTest;
import us.ihmc.utilities.code.agileTesting.BambooPlanType;
import us.ihmc.utilities.code.agileTesting.BambooAnnotations.BambooPlan;


//TODO: Get this working some day!!
@BambooPlan(planType = {BambooPlanType.Exclude})
public class JOptimizerConvexOptimizationAdapterTest extends ConvexOptimizationAdapterTest
{
   public ConvexOptimizationAdapter createConvexOptimizationAdapter()
   {
      return new JOptimizerConvexOptimizationAdapter();
   }

   public double getTestErrorEpsilon()
   {
      return 1e-5;
   }

}
