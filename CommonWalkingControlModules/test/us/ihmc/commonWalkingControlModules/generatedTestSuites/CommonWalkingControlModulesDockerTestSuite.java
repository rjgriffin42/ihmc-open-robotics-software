package us.ihmc.commonWalkingControlModules.generatedTestSuites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

//import us.ihmc.utilities.code.unitTesting.runner.JUnitTestSuiteRunner;

@RunWith(Suite.class)
@Suite.SuiteClasses
({
   us.ihmc.commonWalkingControlModules.calculators.GroundContactPointBasedWrenchCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.calibration.virtualChain.VirtualChainBuilderTest.class,
   us.ihmc.commonWalkingControlModules.calibration.virtualChain.VirtualChainConstructorFromARobotTest.class,
   us.ihmc.commonWalkingControlModules.captureRegion.CaptureRegionCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.captureRegion.OneStepCaptureRegionCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.controlModules.CenterOfPressureResolverTest.class,
   us.ihmc.commonWalkingControlModules.controlModules.GeometricVirtualToePointCalculatorLogisticParametersTest.class,
   us.ihmc.commonWalkingControlModules.controlModules.GeometricVirtualToePointCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.controlModules.GroundReactionWrenchDistributorTest.class,
   us.ihmc.commonWalkingControlModules.controlModules.nativeOptimization.ConstrainedQPSolverTest.class,
   us.ihmc.commonWalkingControlModules.controlModules.TeeterTotterLegStrengthCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootPosePacketTransformerTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.FootstepDataTansformerTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.HandPosePacketTransformerTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.PelvisPosePacketTransformerTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.SquareDataTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.TorusPosePacketTransformerTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorToolsTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepDataTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepPathCoordinatorTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.RangeOfStep2dTest.class,
   us.ihmc.commonWalkingControlModules.desiredFootStep.RangeOfStep3dTest.class,
   us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.HeadingAndVelocityEvaluationScriptTest.class,
   us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.NewInstantaneousCapturePointPlannerDoubleSupportPushRecoveryVisualizerTest.class,
   us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.NewInstantaneousCapturePointPlannerTest.class,
   us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.NewInstantaneousCapturePointPlannerWithSmootherTest.class,
   us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.SmartCMPProjectorTest.class,
   us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointToolsTest.class,
   us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.NewDoubleSupportICPComputerTest.class,
   us.ihmc.commonWalkingControlModules.kinematics.DampedLeastSquaresJacobianSolverTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.CentroidalMomentumBenchmarkTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.CentroidalMomentumRateADotVTermTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.CentroidalMomentumRateTermCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumOptimizerOldTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumSolverTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.OptimizationMomentumControlModuleTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.SingularValueExplorationAndExamplesTest.class,
   us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.TypicalMotionConstraintsTest.class,
   us.ihmc.commonWalkingControlModules.simulationComparison.AllYoVariablesSimulationComparerTest.class,
   us.ihmc.commonWalkingControlModules.simulationComparison.ReflectionSimulationComparerTest.class,
   us.ihmc.commonWalkingControlModules.terrain.VaryingStairGroundProfileTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.CirclePositionAndOrientationTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.CirclePositionTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesSmootherTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.ConnectableMinJerkTrajectoryTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.Constrained5thOrderPolyForSwingTrajectoryTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.EndPointConstrainedCubicTrajectoryTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.MinJerkXYConstrained5thOrderPolyZTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.SmoothenedConstantCoPICPTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.SplineBasedCoMHeightTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.StraightUpThenParabolicCartesianTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.TakeoffLandingCartesianTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.ThreePointDoubleSplines1DTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.ThreePointDoubleSplines2DTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointPositionTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointTrajectoryGeneratorWithPushRecoveryTest.class,
   us.ihmc.commonWalkingControlModules.trajectories.ZeroToOneParabolicVelocityTrajectoryGeneratorTest.class,
   us.ihmc.commonWalkingControlModules.wrenchDistribution.ContactPointWrenchMatrixCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.wrenchDistribution.CylinderAndPlaneContactForceOptimizerMatrixCalculatorTest.class,
   us.ihmc.commonWalkingControlModules.wrenchDistribution.PlaneContactWrenchMatrixCalculatorTest.class
})

public class CommonWalkingControlModulesDockerTestSuite
{
   public static void main(String[] args)
   {
      //new JUnitTestSuiteRunner(CommonWalkingControlModulesDockerTestSuite.class);
   }
}

