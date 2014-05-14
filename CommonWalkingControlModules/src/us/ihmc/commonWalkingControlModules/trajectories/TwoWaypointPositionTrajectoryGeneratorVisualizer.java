package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.Robot;
import com.yobotics.simulationconstructionset.SimulationConstructionSet;
import com.yobotics.simulationconstructionset.SupportedGraphics3DAdapter;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.BagOfBalls;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicVector;
import com.yobotics.simulationconstructionset.util.inputdevices.SliderBoardConfigurationManager;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import com.yobotics.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.PositionProvider;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParametersProvider;
import com.yobotics.simulationconstructionset.util.trajectory.VectorProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoPositionProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoVariableDoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoVelocityProvider;

public class TwoWaypointPositionTrajectoryGeneratorVisualizer
{
   private static final int numberOfTicks = 100;

   private final YoVariableRegistry registry;
   private final DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry;
   private static final SupportedGraphics3DAdapter graphics3DAdapterToUse = SupportedGraphics3DAdapter.JAVA_MONKEY_ENGINE;
   private static final String namePrefix = "Viz";
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoFramePoint initialPosition;
   private final YoFramePoint finalPosition;
   private final List<YoFramePoint> waypoints = new ArrayList<YoFramePoint>();
   private final YoFrameVector initialVelocity;
   private final YoFrameVector finalVelocity;
   private final DoubleYoVariable stepTime;
   private final DoubleYoVariable timeIntoStep;
   private final DoubleYoVariable cartesianSpeed;
   private final BooleanYoVariable drawTrajectory;

   private final BagOfBalls trajectoryBagOfBalls;
   private final DynamicGraphicPosition[] dynamicFixedPositions = new DynamicGraphicPosition[4];
   private final DynamicGraphicVector[] dynamicFixedVelocities = new DynamicGraphicVector[2];

   public TwoWaypointPositionTrajectoryGeneratorVisualizer() throws SimulationExceededMaximumTimeException
   {
      registry = new YoVariableRegistry(namePrefix);
      dynamicGraphicObjectsListRegistry = new DynamicGraphicObjectsListRegistry();

      initialPosition = new YoFramePoint(namePrefix + "P0", worldFrame, registry);
      finalPosition = new YoFramePoint(namePrefix + "P3", worldFrame, registry);
      waypoints.add(new YoFramePoint(namePrefix + "P1", worldFrame, registry));
      waypoints.add(new YoFramePoint(namePrefix + "P2", worldFrame, registry));
      initialVelocity = new YoFrameVector(namePrefix + "V0", worldFrame, registry);
      finalVelocity = new YoFrameVector(namePrefix + "V3", worldFrame, registry);
      stepTime = new DoubleYoVariable(namePrefix + "StepT", registry);
      timeIntoStep = new DoubleYoVariable(namePrefix + "T", registry);
      cartesianSpeed = new DoubleYoVariable(namePrefix + "CartesianSpeed", registry);
      drawTrajectory = new BooleanYoVariable(namePrefix + "DrawTrajectory", registry);

      trajectoryBagOfBalls = new BagOfBalls(numberOfTicks, 0.005, namePrefix + "TrajectoryBagOfBallsVisualizer", registry, dynamicGraphicObjectsListRegistry);
      dynamicFixedPositions[0] = new DynamicGraphicPosition(namePrefix + "DynamicInitialPosition", initialPosition, 0.01, YoAppearance.White());
      dynamicFixedPositions[1] = new DynamicGraphicPosition(namePrefix + "DynamicWaypointPosition0", waypoints.get(0), 0.01, YoAppearance.White());
      dynamicFixedPositions[2] = new DynamicGraphicPosition(namePrefix + "DynamicWaypointPosition1", waypoints.get(1), 0.01, YoAppearance.White());
      dynamicFixedPositions[3] = new DynamicGraphicPosition(namePrefix + "DynamicFinalPosition", finalPosition, 0.01, YoAppearance.White());
      dynamicFixedVelocities[0] = new DynamicGraphicVector(namePrefix + "DynamicInitialVelocity", initialPosition, initialVelocity, YoAppearance.Red());
      dynamicFixedVelocities[1] = new DynamicGraphicVector(namePrefix + "DynamicFinalVelocity", finalPosition, finalVelocity, YoAppearance.Red());
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjects(namePrefix + "DynamicFixedPositions", dynamicFixedPositions);
      dynamicGraphicObjectsListRegistry.registerDynamicGraphicObjects(namePrefix + "DynamicFixedVelocities", dynamicFixedVelocities);

      initialPosition.set(0.0, 0.0, 0.0);
      finalPosition.set(0.6, 0.0, 0.0);
      waypoints.get(0).set(0.2, 0.0, 0.1);
      waypoints.get(1).set(0.4, 0.0, 0.1);
      initialVelocity.set(0.1, 0.1, 0.1);
      finalVelocity.set(0.0, 0.0, -0.1);
      stepTime.set(0.6);
      drawTrajectory.set(true);

      TwoWaypointPositionTrajectoryGenerator trajectoryGenerator = getTrajectoryGenerator();

      int initialBufferSize = 8192;
      SimulationConstructionSet scs = new SimulationConstructionSet(new Robot[] {new Robot("null")}, graphics3DAdapterToUse, initialBufferSize);
      scs.setDT(stepTime.getDoubleValue() / (double) numberOfTicks, 5);
      scs.addYoVariableRegistry(registry);
      dynamicGraphicObjectsListRegistry.addDynamicGraphicsObjectListsToSimulationConstructionSet(scs);

      SliderBoardConfigurationManager sliderBoardConfigurationManager = new SliderBoardConfigurationManager(scs);
      YoFramePoint[] positions = new YoFramePoint[] {initialPosition, waypoints.get(0), waypoints.get(1), finalPosition};
      YoFrameVector[] velocities = new YoFrameVector[] {initialVelocity, finalVelocity};
      
      for (int i = 0; i < positions.length; i++)
      {
         sliderBoardConfigurationManager.setSlider(i + 1, positions[i].getYoX(), -2.0, 2.0);
         sliderBoardConfigurationManager.setSlider(i + 1, positions[i].getYoY(), -2.0, 2.0);
         sliderBoardConfigurationManager.setSlider(i + 1, positions[i].getYoZ(), -2.0, 2.0);
      }

      for (int i = 0; i < velocities.length; i++)
      {
         sliderBoardConfigurationManager.setSlider(i + 5, velocities[i].getYoX(), -2.0, 2.0);
         sliderBoardConfigurationManager.setSlider(i + 5, velocities[i].getYoY(), -2.0, 2.0);
         sliderBoardConfigurationManager.setSlider(i + 5, velocities[i].getYoZ(), -2.0, 2.0);
      }
      
      sliderBoardConfigurationManager.setSlider(7, stepTime, 0.01, 2.0);
      
      Thread thread = new Thread(scs);

      thread.start();

      while (true)
      {
         if (drawTrajectory.getBooleanValue())
         {
            scs.setInPoint();
            drawTrajectory.set(false);
            
            trajectoryGenerator.initialize();

            for (int tick = 0; tick < numberOfTicks; tick++)
            {
               double t = ((double) tick * stepTime.getDoubleValue()) / (double) numberOfTicks;
               updateTrajectory(trajectoryGenerator, t, tick);
               if (notLastTick(tick))
                  scs.tickAndUpdate();
            }
         }

         Thread.yield();
      }
   }

   private void updateTrajectory(TwoWaypointPositionTrajectoryGenerator trajectoryGenerator, double t, int tick)
   {
      timeIntoStep.set(t);
      trajectoryGenerator.compute(t);
      setBall(trajectoryGenerator, tick);
      setCartesianSpeed(trajectoryGenerator);
   }

   private boolean notLastTick(int tick)
   {
      return tick < numberOfTicks - 1;
   }

   private void setBall(TwoWaypointPositionTrajectoryGenerator trajectoryGenerator, int tick)
   {
      FramePoint position = new FramePoint();
      trajectoryGenerator.get(position);
      trajectoryBagOfBalls.setBall(position, YoAppearance.Black(), tick);
   }

   private void setCartesianSpeed(TwoWaypointPositionTrajectoryGenerator trajectoryGenerator)
   {
      FrameVector velocity = new FrameVector();
      trajectoryGenerator.packVelocity(velocity);
      cartesianSpeed.set(velocity.length());
   }

   private TwoWaypointPositionTrajectoryGenerator getTrajectoryGenerator()
   {
      YoVariableDoubleProvider stepTimeProvider = new YoVariableDoubleProvider(stepTime);
      YoPositionProvider initialPositionProvider = new YoPositionProvider(initialPosition);
      YoPositionProvider finalPositionProvider = new YoPositionProvider(finalPosition);
      YoVelocityProvider initialVelocityProvider = new YoVelocityProvider(initialVelocity);
      YoVelocityProvider finalDesiredVelocityProvider = new YoVelocityProvider(finalVelocity);

      YoTwoWaypointTrajectoryParameters trajectoryParameters = null;
      TrajectoryParametersProvider trajectoryParametersProvider = new TrajectoryParametersProvider(trajectoryParameters);
      
      int arcLengthCalculatorDivisionsPerPolynomial = 20;

      return new TwoWaypointPositionTrajectorySpecifiedByPoints(namePrefix + "Generator", worldFrame, stepTimeProvider, initialPositionProvider,
              initialVelocityProvider, finalPositionProvider, finalDesiredVelocityProvider, trajectoryParametersProvider, registry,
              arcLengthCalculatorDivisionsPerPolynomial, dynamicGraphicObjectsListRegistry, null, false, null);
   }
   
   public static class TwoWaypointPositionTrajectorySpecifiedByPoints extends TwoWaypointPositionTrajectoryGenerator
   {
      private final ReferenceFrame referenceFrame;
      private final List<FramePoint> waypoints;
      
      public TwoWaypointPositionTrajectorySpecifiedByPoints(String namePrefix, ReferenceFrame referenceFrame, DoubleProvider stepTimeProvider,
            PositionProvider initialPositionProvider, VectorProvider initialVelocityProvider, PositionProvider finalPositionProvider,
            VectorProvider finalDesiredVelocityProvider, TrajectoryParametersProvider trajectoryParametersProvider, YoVariableRegistry parentRegistry,
            int arcLengthCalculatorDivisionsPerPolynomial, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
            WalkingControllerParameters walkingControllerParameters, boolean visualize, List<FramePoint> waypoints)
      {
         super(namePrefix, referenceFrame, stepTimeProvider, initialPositionProvider, initialVelocityProvider, finalPositionProvider, finalDesiredVelocityProvider,
               trajectoryParametersProvider, parentRegistry, dynamicGraphicObjectsListRegistry, walkingControllerParameters,
               visualize);
         
         this.referenceFrame = referenceFrame;
         this.waypoints = waypoints;
      }
      
      @Override
      protected void setWaypointPositions()
      {
         int[] waypointIndices = new int[] {2, 3};
         List<FramePoint> wayFramePoints = new ArrayList<FramePoint>();
         
         for (int i = 0; i < 2; i++)
         {
            wayFramePoints.add(waypoints.get(i));
         }
         
         for (int i = 0; i < 2; i++)
         {
            wayFramePoints.get(i).changeFrame(referenceFrame);
            allPositions[waypointIndices[i]].set(wayFramePoints.get(i));
         }
         checkForCloseWaypoints();
      }
   }

   public static void main(String[] args) throws SimulationExceededMaximumTimeException
   {
      new TwoWaypointPositionTrajectoryGeneratorVisualizer();
   }
}
