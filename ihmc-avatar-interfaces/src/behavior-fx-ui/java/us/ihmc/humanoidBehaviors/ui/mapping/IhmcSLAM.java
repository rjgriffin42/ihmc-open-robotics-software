package us.ihmc.humanoidBehaviors.ui.mapping;

import controller_msgs.msg.dds.StereoVisionPointCloudMessage;
import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.robotics.numericalMethods.GradientDescentModule;

public class IhmcSLAM extends AbstractSLAM<IhmcSLAMFrame>
{
   public static final double OCTREE_RESOLUTION = 0.02;

   private static final double OPTIMIZER_POSITION_LIMIT = 0.1;
   private static final double OPTIMIZER_ANGLE_LIMIT = Math.toRadians(10.);

   public static final TDoubleArrayList initialQuery = new TDoubleArrayList();
   public static final TDoubleArrayList lowerLimit = new TDoubleArrayList();
   public static final TDoubleArrayList upperLimit = new TDoubleArrayList();

   static
   {
      for (int i = 0; i < 3; i++)
      {
         initialQuery.add(0.0);
         lowerLimit.add(-OPTIMIZER_POSITION_LIMIT);
         upperLimit.add(OPTIMIZER_POSITION_LIMIT);
      }
      for (int i = 0; i < 3; i++)
      {
         initialQuery.add(0.0);
         lowerLimit.add(-OPTIMIZER_ANGLE_LIMIT);
         upperLimit.add(OPTIMIZER_ANGLE_LIMIT);
      }
   }

   public IhmcSLAM()
   {
      this(false);
   }

   public IhmcSLAM(boolean naiveSLAM)
   {
      super(naiveSLAM, OCTREE_RESOLUTION);
   }

   public RigidBodyTransform computeOptimizedMultiplier(SLAMFrame newFrame)
   {
      SLAMFrameOptimizerCostFunction costFunction = newFrame.createCostFunction(this);
      GradientDescentModule optimizer = new GradientDescentModule(costFunction, initialQuery);

      int maxIterations = 100;
      double convergenceThreshold = 10E-5;
      double optimizerStepSize = -0.1;
      double optimizerPerturbationSize = 0.0001;

      optimizer.setInputLowerLimit(lowerLimit);
      optimizer.setInputUpperLimit(upperLimit);
      optimizer.setMaximumIterations(maxIterations);
      optimizer.setConvergenceThreshold(convergenceThreshold);
      optimizer.setStepSize(optimizerStepSize);
      optimizer.setPerturbationSize(optimizerPerturbationSize);
      optimizer.setReducingStepSizeRatio(2);

      int run = optimizer.run();
      System.out.println("optimizer Query() " + run + " " + costFunction.getQuery(initialQuery) + " " + optimizer.getOptimalQuery());
      TDoubleArrayList optimalInput = optimizer.getOptimalInput();

      RigidBodyTransform transformer = new RigidBodyTransform();
      costFunction.convertToSensorPoseMultiplier(optimalInput, transformer);

      return transformer;
   }

   @Override
   public SLAMFrame createFrame(StereoVisionPointCloudMessage pointCloudMessage)
   {
      return new IhmcSLAMFrame(pointCloudMessage);
   }

   @Override
   public SLAMFrame createFrame(SLAMFrame previousFrame, StereoVisionPointCloudMessage pointCloudMessage)
   {
      return new IhmcSLAMFrame(previousFrame, pointCloudMessage);
   }
}
