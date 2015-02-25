package us.ihmc.commonWalkingControlModules.trajectories;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.Test;

import us.ihmc.utilities.code.agileTesting.BambooAnnotations.AverageDuration;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.ConstantPositionProvider;
import us.ihmc.utilities.math.trajectories.providers.ConstantVectorProvider;
import us.ihmc.utilities.math.trajectories.providers.PositionProvider;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParametersProvider;
import us.ihmc.utilities.math.trajectories.providers.VectorProvider;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.trajectories.PositionTrajectoryGenerator;
import us.ihmc.yoUtilities.math.trajectories.providers.YoPositionProvider;
import us.ihmc.yoUtilities.math.trajectories.providers.YoVariableDoubleProvider;

public class PushRecoveryTrajectoryGeneratorTest
{

   private static ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   @AverageDuration
   @Test(timeout = 300000)
   public void testSimpleTrajectories()
   {
      testSimpleTrajectory(3);
      testSimpleTrajectory(4);
   }

   private void testSimpleTrajectory(int numDesiredSplines)
   {
      YoVariableDoubleProvider stepTimeProvider = new YoVariableDoubleProvider("", new YoVariableRegistry(""));
      stepTimeProvider.set(0.8);
      YoVariableDoubleProvider timeRemainingProvider = new YoVariableDoubleProvider("", new YoVariableRegistry(""));
      PositionProvider initialPositionProvider = new ConstantPositionProvider(new FramePoint(worldFrame, new double[] { -0.1, 2.3, 0.0 }));
      VectorProvider initialVelocityProvider = new ConstantVectorProvider(new FrameVector(worldFrame, new double[] { 0.2, 0.0, -0.05 }));

      Point3d firstIntermediatePosition = new Point3d(new double[] { 0.12, 2.4, 0.2 });
      Point3d secondIntermediatePosition = new Point3d(new double[] { 0.16, 2.3, 0.15 });
      ArrayList<Point3d> waypoints = new ArrayList<Point3d>();
      waypoints.add(firstIntermediatePosition);
      waypoints.add(secondIntermediatePosition);

      YoFramePoint finalPosition = new YoFramePoint("", worldFrame, new YoVariableRegistry(""));
      finalPosition.set(new FramePoint(worldFrame, new double[] { 0.2, 2.35, 0.03 }));
      YoPositionProvider finalPositionProvider = new YoPositionProvider(finalPosition);
      VectorProvider finalVelocityProvider = new ConstantVectorProvider(new FrameVector(worldFrame, new double[] { 0, 0, -0.02 }));

      TrajectoryParameters trajectoryParameters = new TrajectoryParameters();
      TrajectoryParametersProvider trajectoryParametersProvider = new TrajectoryParametersProvider(trajectoryParameters);

      TwoWaypointPositionTrajectoryGenerator trajectory = new TwoWaypointPositionTrajectoryGenerator("", worldFrame, stepTimeProvider, initialPositionProvider,
            initialVelocityProvider, null, finalPositionProvider, finalVelocityProvider, trajectoryParametersProvider, new YoVariableRegistry(""), null, null, false);

      List<Point3d> points = new ArrayList<Point3d>();
      points.add(firstIntermediatePosition);
      points.add(secondIntermediatePosition);
      trajectory.initialize();
      trajectory.compute(0.0);
      FramePoint actual = new FramePoint(worldFrame);
      FramePoint expected = new FramePoint(worldFrame);
      initialPositionProvider.get(expected);
      trajectory.get(actual);
      assertEquals(actual.getX(), expected.getX(), 1e-7);
      assertEquals(actual.getY(), expected.getY(), 1e-7);
      assertEquals(actual.getZ(), expected.getZ(), 1e-7);

      FrameVector actualVel = new FrameVector(worldFrame);
      FrameVector expectedVel = new FrameVector(worldFrame);
      trajectory.packVelocity(actualVel);
      initialVelocityProvider.get(expectedVel);
      assertEquals(actualVel.getX(), expectedVel.getX(), 1e-7);
      assertEquals(actualVel.getY(), expectedVel.getY(), 1e-7);
      assertEquals(actualVel.getZ(), expectedVel.getZ(), 1e-7);

      trajectory.compute(0.8);
      finalPositionProvider.get(expected);
      trajectory.get(actual);
      assertEquals(actual.getX(), expected.getX(), 1e-7);
      assertEquals(actual.getY(), expected.getY(), 1e-7);
      assertEquals(actual.getZ(), expected.getZ(), 1e-7);

      trajectory.packVelocity(actualVel);
      finalVelocityProvider.get(expectedVel);
      assertEquals(actualVel.getX(), expectedVel.getX(), 1e-7);
      assertEquals(actualVel.getY(), expectedVel.getY(), 1e-7);
      assertEquals(actualVel.getZ(), expectedVel.getZ(), 1e-7);

      trajectory.compute(0.4);

      FramePoint intermediatePosition = new FramePoint();
      FrameVector intermediateVelocity = new FrameVector();
      trajectory.get(intermediatePosition);
      trajectory.packVelocity(intermediateVelocity);

      PositionProvider intermediatePositionProvider = new ConstantPositionProvider(intermediatePosition);
      VectorProvider intermediateVelocityProvider = new ConstantVectorProvider(intermediateVelocity);

      PositionTrajectoryGenerator pushRecoveryTrajectoryGenerator = new PushRecoveryTrajectoryGenerator("", worldFrame, stepTimeProvider,
            timeRemainingProvider, intermediatePositionProvider, intermediateVelocityProvider, finalPositionProvider, new YoVariableRegistry(""), null,
            trajectory);

      double tIntoStep = 0.4;
      timeRemainingProvider.set(stepTimeProvider.getValue() - tIntoStep);

      pushRecoveryTrajectoryGenerator.initialize();
      pushRecoveryTrajectoryGenerator.compute(0.4);

      intermediatePositionProvider.get(expected);
      pushRecoveryTrajectoryGenerator.get(actual);
      assertEquals(actual.getX(), expected.getX(), 1e-7);
      assertEquals(actual.getY(), expected.getY(), 1e-7);

      intermediateVelocityProvider.get(expectedVel);
      pushRecoveryTrajectoryGenerator.packVelocity(actualVel);
      assertEquals(actualVel.getX(), expectedVel.getX(), 1e-7);
      assertEquals(actualVel.getY(), expectedVel.getY(), 1e-7);

      pushRecoveryTrajectoryGenerator.compute(0.8);

      finalPositionProvider.get(expected);
      pushRecoveryTrajectoryGenerator.get(actual);
      assertEquals(actual.getX(), expected.getX(), 1e-7);
      assertEquals(actual.getY(), expected.getY(), 1e-7);
   }
}