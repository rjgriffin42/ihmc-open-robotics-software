package us.ihmc.robotics.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import us.ihmc.tools.testing.TestPlanSuite;
import us.ihmc.tools.testing.TestPlanSuite.TestSuiteTarget;
import us.ihmc.tools.testing.TestPlanTarget;

/** WARNING: AUTO-GENERATED FILE. DO NOT MAKE MANUAL CHANGES TO THIS FILE. **/
@RunWith(TestPlanSuite.class)
@TestSuiteTarget(TestPlanTarget.Fast)
@SuiteClasses
({
   us.ihmc.robotics.math.filters.DelayedBooleanYoVariableTest.class,
   us.ihmc.robotics.math.filters.DelayedDoubleYoVariableTest.class,
   us.ihmc.robotics.math.filters.DeltaLimitedYoVariableTest.class,
   us.ihmc.robotics.math.filters.FilteredDiscreteVelocityYoVariableTest.class,
   us.ihmc.robotics.math.filters.FilteredVelocityYoVariableTest.class,
   us.ihmc.robotics.math.filters.FirstOrderFilteredYoVariableTest.class,
   us.ihmc.robotics.math.filters.GlitchFilteredBooleanYoVariableTest.class,
   us.ihmc.robotics.math.filters.HysteresisFilteredYoVariableTest.class,
   us.ihmc.robotics.math.filters.RateLimitedYoVariableTest.class,
   us.ihmc.robotics.math.filters.SimpleMovingAverageFilteredYoVariableTest.class,
   us.ihmc.robotics.math.frames.YoFramePointInMultipleFramesTest.class,
   us.ihmc.robotics.math.frames.YoFrameQuaternionTest.class,
   us.ihmc.robotics.math.frames.YoMatrixTest.class,
   us.ihmc.robotics.math.frames.YoMultipleFramesHelperTest.class,
   us.ihmc.robotics.math.interpolators.OrientationInterpolationCalculatorTest.class,
   us.ihmc.robotics.math.interpolators.QuinticSplineInterpolatorTest.class,
   us.ihmc.robotics.math.MatrixYoVariableConversionToolsTest.class,
   us.ihmc.robotics.math.QuaternionCalculusTest.class,
   us.ihmc.robotics.math.TimestampedVelocityYoVariableTest.class,
   us.ihmc.robotics.math.trajectories.CirclePositionTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.ConstantAccelerationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.ConstantForceTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.ConstantOrientationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.ConstantPoseTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.ConstantPositionTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.ConstantVelocityTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.MultipleWaypointsOrientationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.MultipleWaypointsPositionTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.MultipleWaypointsTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.OneDoFJointQuinticTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.OrientationInterpolationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.PositionTrajectorySmootherTest.class,
   us.ihmc.robotics.math.trajectories.ProviderBasedConstantOrientationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.providers.YoOrientationProviderTest.class,
   us.ihmc.robotics.math.trajectories.providers.YoPositionProviderTest.class,
   us.ihmc.robotics.math.trajectories.providers.YoSE3ConfigurationProviderTest.class,
   us.ihmc.robotics.math.trajectories.providers.YoVariableDoubleProviderTest.class,
   us.ihmc.robotics.math.trajectories.SimpleOrientationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.StraightLinePositionTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.VelocityConstrainedOrientationTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.WrapperForMultiplePositionTrajectoryGeneratorsTest.class,
   us.ihmc.robotics.math.trajectories.WrapperForPositionAndOrientationTrajectoryGeneratorsTest.class,
   us.ihmc.robotics.math.trajectories.YoConcatenatedSplinesTest.class,
   us.ihmc.robotics.math.trajectories.YoMinimumJerkTrajectoryTest.class,
   us.ihmc.robotics.math.trajectories.YoParabolicTrajectoryGeneratorTest.class,
   us.ihmc.robotics.math.trajectories.YoSpline3DTest.class,
   us.ihmc.robotics.math.YoRMSCalculatorTest.class,
   us.ihmc.robotics.math.YoSignalDerivativeTest.class,
   us.ihmc.robotics.MathToolsTest.class,
   us.ihmc.robotics.numericalMethods.DifferentiatorTest.class,
   us.ihmc.robotics.numericalMethods.NewtonRaphsonMethodTest.class,
   us.ihmc.robotics.numericalMethods.QuarticEquationSolverTest.class,
   us.ihmc.robotics.numericalMethods.QuarticRootFinderTest.class,
   us.ihmc.robotics.optimization.ActiveSearchQuadraticProgramOptimizerTest.class,
   us.ihmc.robotics.optimization.EqualityConstraintEnforcerTest.class,
   us.ihmc.robotics.quadTree.QuadTreeForGroundTest.class,
   us.ihmc.robotics.random.RandomToolsTest.class,
   us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrameTest.class,
   us.ihmc.robotics.referenceFrames.Pose2dReferenceFrameTest.class,
   us.ihmc.robotics.referenceFrames.PoseReferenceFrameTest.class,
   us.ihmc.robotics.referenceFrames.ReferenceFrameTest.class,
   us.ihmc.robotics.robotSide.QuadrantDependentListTest.class,
   us.ihmc.robotics.robotSide.RecyclingQuadrantDependentListTest.class,
   us.ihmc.robotics.robotSide.RobotQuadrantTest.class,
   us.ihmc.robotics.robotSide.RobotSideTest.class,
   us.ihmc.robotics.robotSide.SideDependentListTest.class,
   us.ihmc.robotics.screwTheory.CenterOfMassAccelerationCalculatorTest.class,
   us.ihmc.robotics.screwTheory.CenterOfMassJacobianTest.class,
   us.ihmc.robotics.screwTheory.CompositeRigidBodyMassMatrixCalculatorTest.class,
   us.ihmc.robotics.screwTheory.ConvectiveTermCalculatorTest.class,
   us.ihmc.robotics.screwTheory.DesiredJointAccelerationCalculatorTest.class,
   us.ihmc.robotics.screwTheory.DifferentialIDMassMatrixCalculatorTest.class,
   us.ihmc.robotics.screwTheory.GenericCRC32Test.class,
   us.ihmc.robotics.screwTheory.GeometricJacobianTest.class,
   us.ihmc.robotics.screwTheory.MomentumCalculatorTest.class,
   us.ihmc.robotics.screwTheory.MomentumTest.class,
   us.ihmc.robotics.screwTheory.PointJacobianTest.class,
   us.ihmc.robotics.screwTheory.RigidBodyInertiaTest.class,
   us.ihmc.robotics.screwTheory.ScrewToolsTest.class,
   us.ihmc.robotics.screwTheory.SpatialAccelerationVectorTest.class,
   us.ihmc.robotics.screwTheory.SpatialMotionVectorTest.class,
   us.ihmc.robotics.screwTheory.ThreeDoFAngularAccelerationCalculatorTest.class,
   us.ihmc.robotics.screwTheory.TotalMassCalculatorTest.class,
   us.ihmc.robotics.screwTheory.TwistTest.class,
   us.ihmc.robotics.screwTheory.WrenchTest.class,
   us.ihmc.robotics.stateMachines.StateChangeRecorderTest.class,
   us.ihmc.robotics.stateMachines.StateMachineTest.class,
   us.ihmc.robotics.statistics.CovarianceDerivationTest.class,
   us.ihmc.robotics.statistics.OnePassMeanAndStandardDeviationTest.class,
   us.ihmc.robotics.statistics.PermutationTest.class,
   us.ihmc.robotics.time.CallFrequencyCalculatorTest.class,
   us.ihmc.robotics.time.ExecutionTimerTest.class,
   us.ihmc.robotics.time.GlobalTimerTest.class,
   us.ihmc.robotics.time.TimeToolsTest.class,
   us.ihmc.robotics.trajectories.LinearInterpolatorTest.class,
   us.ihmc.robotics.trajectories.MinimumJerkTrajectoryTest.class,
   us.ihmc.robotics.trajectories.ParametricSplineTrajectorySolverTest.class,
   us.ihmc.robotics.trajectories.PolynomialSplineTest.class,
   us.ihmc.robotics.trajectories.PolynomialTrajectoryTest.class,
   us.ihmc.robotics.trajectories.providers.ConstantDoubleProviderTest.class,
   us.ihmc.robotics.trajectories.providers.ConstantPositionProviderTest.class,
   us.ihmc.robotics.trajectories.providers.CurrentPositionProviderTest.class,
   us.ihmc.robotics.trajectories.TrapezoidalVelocityTrajectoryTest.class,
   us.ihmc.robotics.trajectories.WaypointMotionGeneratorTest.class
})

public class IHMCRoboticsToolkitBFastTestSuite
{
   public static void main(String[] args)
   {

   }
}
