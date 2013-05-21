package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.Box3d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.BagOfBalls;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.PositionProvider;
import com.yobotics.simulationconstructionset.util.trajectory.PositionTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParameters;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParametersProvider;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryWaypointGenerationMethod;
import com.yobotics.simulationconstructionset.util.trajectory.VectorProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoConcatenatedSplines;

public class TwoWaypointPositionTrajectoryGenerator implements PositionTrajectoryGenerator
{
   private final static double EPSILON = 1e-3;
   private final static double WAYPOINT_CLOSENESS_FACTOR = .15;    // waypoints are considered close together if the distance between them is less than the total
   // distance times this fraction; waypoints that are close together are both set to their midpoint and passed through at a velocity of zero

   private final WalkingControllerParameters walkingControllerParameters;

   private final DoubleYoVariable linearSplineLengthFactor;

   private final String namePostFix = getClass().getSimpleName();
   private final YoVariableRegistry registry;
   private final int numberOfVisualizationMarkers = 50;
   private final BooleanYoVariable visualize;
   
   protected final EnumYoVariable<TrajectoryWaypointGenerationMethod> waypointGenerationMethod;

   private final BagOfBalls trajectoryBagOfBalls;
   private final BagOfBalls fixedPointBagOfBalls;

   private final DoubleProvider stepTimeProvider;
   private final PositionProvider[] positionSources = new PositionProvider[2];
   private final VectorProvider[] velocitySources = new VectorProvider[2];

   private final DoubleYoVariable stepTime;
   private final DoubleYoVariable timeIntoStep;

   private final YoFramePoint desiredPosition;
   private final YoFrameVector desiredVelocity;
   private final YoFrameVector desiredAcceleration;
   private final ReferenceFrame referenceFrame;

   private final DoubleYoVariable[] allTimes = new DoubleYoVariable[6];
   protected final YoFramePoint[] allPositions = new YoFramePoint[6];
   private final YoFrameVector[] allVelocities = new YoFrameVector[6];

   private final TrajectoryParametersProvider trajectoryParametersProvider;
   protected TwoWaypointTrajectoryParameters trajectoryParameters;

   private static final int[] endpointIndices = new int[] {0, 5};
   private static final int[] waypointIndices = new int[] {2, 3};
   private static final int[] oppositeWaypointIndices = new int[] {3, 2};
   private static final int[] accelerationEndpointIndices = new int[] {1, 4};
   private static final int[] nonAccelerationEndpointIndices = new int[] {0, 2, 3, 5};
   private static final int[] allIndices = new int[]
   {
      0, 1, 2, 3, 4, 5
   };

   private final YoConcatenatedSplines concatenatedSplinesWithArcLengthApproximatedByDistance;
   private final YoConcatenatedSplines concatenatedSplinesWithArcLengthCalculatedIteratively;

   private boolean waypointsAreTheSamePoint = false;

   public TwoWaypointPositionTrajectoryGenerator(String namePrefix, ReferenceFrame referenceFrame, DoubleProvider stepTimeProvider,
           PositionProvider initialPositionProvider, VectorProvider initialVelocityProvider, PositionProvider finalPositionProvider,
           VectorProvider finalDesiredVelocityProvider, TrajectoryParametersProvider trajectoryParametersProvider, YoVariableRegistry parentRegistry,
           int arcLengthCalculatorDivisionsPerPolynomial, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
           WalkingControllerParameters walkingControllerParameters, boolean visualize)
   {
      registry = new YoVariableRegistry(namePrefix + namePostFix);
      parentRegistry.addChild(registry);

      if (visualize)
      {
         trajectoryBagOfBalls = new BagOfBalls(numberOfVisualizationMarkers, 0.01, namePrefix + "TrajectoryBagOfBalls", registry,
                 dynamicGraphicObjectsListRegistry);
         fixedPointBagOfBalls = new BagOfBalls(6, 0.02, namePrefix + "WaypointBagOfBalls", registry, dynamicGraphicObjectsListRegistry);
      }

      else
      {
         trajectoryBagOfBalls = null;
         fixedPointBagOfBalls = null;
      }
      
      this.waypointGenerationMethod = new EnumYoVariable<TrajectoryWaypointGenerationMethod>(namePrefix + "WaypointGenerationMethod", registry, TrajectoryWaypointGenerationMethod.class);

      this.walkingControllerParameters = walkingControllerParameters;

      this.stepTimeProvider = stepTimeProvider;

      this.referenceFrame = referenceFrame;

      positionSources[0] = initialPositionProvider;
      positionSources[1] = finalPositionProvider;

      velocitySources[0] = initialVelocityProvider;
      velocitySources[1] = finalDesiredVelocityProvider;

      stepTime = new DoubleYoVariable(namePrefix + "StepTime", registry);
      timeIntoStep = new DoubleYoVariable(namePrefix + "TimeIntoStep", registry);

      desiredPosition = new YoFramePoint(namePrefix + "DesiredPosition", referenceFrame, registry);
      desiredVelocity = new YoFrameVector(namePrefix + "DesiredVelocity", referenceFrame, registry);
      desiredAcceleration = new YoFrameVector(namePrefix + "DesiredAcceleration", referenceFrame, registry);

      linearSplineLengthFactor = new DoubleYoVariable(namePrefix + "LinearSplineLengthFactor", registry);

      this.trajectoryParametersProvider = trajectoryParametersProvider;

      for (int i = 0; i < 6; i++)
      {
         allTimes[i] = new DoubleYoVariable(namePrefix + "FixedPointTime" + i, registry);
         allPositions[i] = new YoFramePoint(namePrefix + "FixedPointPosition" + i, referenceFrame, registry);
         allVelocities[i] = new YoFrameVector(namePrefix + "FixedPointVelocity" + i, referenceFrame, registry);
      }

      concatenatedSplinesWithArcLengthApproximatedByDistance = new YoConcatenatedSplines(new int[] {4, 2, 6, 2, 4}, referenceFrame,
              arcLengthCalculatorDivisionsPerPolynomial, registry, namePrefix + "ConcatenatedSplinesWithArcLengthApproximatedByDistance");
      concatenatedSplinesWithArcLengthCalculatedIteratively = new YoConcatenatedSplines(new int[] {4, 2, 6, 2, 4}, referenceFrame, 2, registry,
              namePrefix + "ConcatenatedSplinesWithArcLengthCalculatedIteratively");

      this.visualize = new BooleanYoVariable(namePrefix + "Visualize", registry);
      this.visualize.set(visualize);
   }

   public void compute(double time)
   {
      timeIntoStep.set(time);

      double totalTime = stepTime.getDoubleValue();
      if (time > totalTime)
         time = totalTime;

      concatenatedSplinesWithArcLengthCalculatedIteratively.compute(time);
      desiredPosition.set(concatenatedSplinesWithArcLengthCalculatedIteratively.getPosition());
      desiredVelocity.set(concatenatedSplinesWithArcLengthCalculatedIteratively.getVelocity());
      desiredAcceleration.set(concatenatedSplinesWithArcLengthCalculatedIteratively.getAcceleration());

//    concatenatedSplinesWithArcLengthCalculatedIteratively.compute(time - stepTime.getDoubleValue() / (double) numberOfTimeIntervals);
//    FrameVector previousVelocity = concatenatedSplinesWithArcLengthCalculatedIteratively.getVelocity();
//
//    concatenatedSplinesWithArcLengthCalculatedIteratively.compute(time + stepTime.getDoubleValue() / (double) numberOfTimeIntervals);
//    FrameVector nextVelocity = concatenatedSplinesWithArcLengthCalculatedIteratively.getVelocity();
//    
//    desiredAcceleration.set(nextVelocity);
//    desiredAcceleration.sub(previousVelocity);
//    desiredAcceleration.scale(0.5 * (double) numberOfTimeIntervals / stepTime.getDoubleValue());

   }

   public void get(FramePoint positionToPack)
   {
      desiredPosition.getFramePointAndChangeFrameOfPackedPoint(positionToPack);
   }

   public void packVelocity(FrameVector velocityToPack)
   {
      desiredVelocity.getFrameVectorAndChangeFrameOfPackedVector(velocityToPack);
   }

   public void packAcceleration(FrameVector accelerationToPack)
   {
      desiredAcceleration.getFrameVectorAndChangeFrameOfPackedVector(accelerationToPack);
   }

   private void setStepTime()
   {
      double stepTime = stepTimeProvider.getValue();
      if (stepTime <= 0)
      {
         throw new RuntimeException("stepTimeProvider must provide a positive time value.");
      }

      this.stepTime.set(stepTime);
      timeIntoStep.set(0.0);
   }

   public void initialize()
   {
      setTrajectoryParameters();

      setStepTime();

      setInitialAndFinalTimesPositionsAndVelocities();
      setWaypointPositions();

      double[] arcLengthsIgnoringAccelerationEndpointPositions = getArcLengthsApproximatedByDistance(nonAccelerationEndpointIndices);
      setLinearSplineLengthFactor(arcLengthsIgnoringAccelerationEndpointPositions);
      setAccelerationEndpointPositions();

      double[] arcLengths;

      arcLengths = getArcLengthsApproximatedByDistance(allIndices);
      setWaypointAndAccelerationEndpointTimesAndVelocities(arcLengths);

      if (!waypointsAreTheSamePoint)
      {
         setConcatenatedSplines(concatenatedSplinesWithArcLengthApproximatedByDistance);
         arcLengths = getArcLengthsCalculatedIteratively();
         setWaypointAndAccelerationEndpointTimesAndVelocities(arcLengths);
      }

      setConcatenatedSplines(concatenatedSplinesWithArcLengthCalculatedIteratively);

      if (visualize.getBooleanValue())
      {
         visualizeSpline();
      }
   }

   private void setTrajectoryParameters()
   {
      TrajectoryParameters trajectoryParameters = trajectoryParametersProvider.getTrajectoryParameters();

      if (trajectoryParameters instanceof TwoWaypointTrajectoryParameters)
      {
         this.trajectoryParameters = (TwoWaypointTrajectoryParameters) trajectoryParameters;
      }

      else if (trajectoryParameters == null)
      {
         this.trajectoryParameters = new SimpleTwoWaypointTrajectoryParameters();
      }

      else
      {
         throw new RuntimeException(
             "trajectoryParametersProvider must provide TwoWaypointPositionTrajectoryGenerator with an instance of TwoWaypointTrajectoryParameters.");
      }
   }

   private void setLinearSplineLengthFactor(double[] arcLengths)
   {
      double portionOfArcLengthInMiddleSpline = arcLengths[1] / (arcLengths[0] + arcLengths[1] + arcLengths[2]);
      linearSplineLengthFactor.set(2.0
                                   * Math.max(SimpleTwoWaypointTrajectoryParameters.getMinimumDesiredProportionOfArcLengthTakenAtConstantSpeed()
                                      - portionOfArcLengthInMiddleSpline, 0.0) * (1 / (1 - portionOfArcLengthInMiddleSpline)));
   }

   private void setWaypointAndAccelerationEndpointTimesAndVelocities(double[] arcLengths)
   {
      double waypointSpeed = waypointsAreTheSamePoint ? 0.0 : getWaypointSpeed(arcLengths);

      for (int i = 0; i < 2; i++)
      {
         FrameVector waypointVelocity = getOppositeWaypointToEndpoint(i);
         waypointVelocity.normalize();
         waypointVelocity.scale(waypointSpeed * ((i == 0) ? -1 : 1));
         allVelocities[waypointIndices[i]].set(waypointVelocity);
         allVelocities[accelerationEndpointIndices[i]].set(waypointVelocity);
      }

      if (waypointsAreTheSamePoint)
      {
         double totalArcLength = getTotalArcLength(arcLengths);
         allTimes[1].set((arcLengths[0] + arcLengths[1]) / totalArcLength * stepTime.getDoubleValue());

         for (int i = 2; i < 5; i++)
         {
            allTimes[i].set(allTimes[1].getDoubleValue());
         }
      }

      else
      {
         allTimes[1].set(2 * arcLengths[0] / (allVelocities[0].length() + waypointSpeed));
         allTimes[2].set(allTimes[1].getDoubleValue() + arcLengths[1] / waypointSpeed);
         allTimes[3].set(allTimes[2].getDoubleValue() + arcLengths[2] / waypointSpeed);
         allTimes[4].set(allTimes[3].getDoubleValue() + arcLengths[3] / waypointSpeed);
      }
   }

   private boolean waypointsAreCloseTogether()
   {
      double[] arcLengths = getArcLengthsApproximatedByDistance(nonAccelerationEndpointIndices);
      double totalArcLength = getTotalArcLength(arcLengths);
      double arcLengthOfMiddleSpline = arcLengths[1];
      return arcLengthOfMiddleSpline < WAYPOINT_CLOSENESS_FACTOR * totalArcLength;
   }

   private double getTotalArcLength(double[] arcLengths)
   {
      double totalArcLength = 0.0;
      for (int i = 0; i < arcLengths.length; i++)
      {
         totalArcLength += arcLengths[i];
      }

      return totalArcLength;
   }

   private double getWaypointSpeed(double[] arcLengths)
   {
      double lowerBound = 0.0;
      double upperBound = 10.0;
      while (getStepTimeGivenWaypointSpeed(upperBound, arcLengths) > stepTime.getDoubleValue() + EPSILON)
      {
         upperBound *= 2.0;
      }

      double waypointSpeed;
      double resultingStepTime;

      while (true)
      {
         waypointSpeed = (upperBound + lowerBound) / 2.0;
         resultingStepTime = getStepTimeGivenWaypointSpeed(waypointSpeed, arcLengths);

         if (MathTools.epsilonEquals(resultingStepTime, stepTime.getDoubleValue(), EPSILON))
         {
            return waypointSpeed;
         }
         else if (resultingStepTime > stepTime.getDoubleValue())
         {
            lowerBound = waypointSpeed;
         }
         else if (resultingStepTime < stepTime.getDoubleValue())
         {
            upperBound = waypointSpeed;
         }
      }

   }

   private double getStepTimeGivenWaypointSpeed(double waypointSpeed, double[] arcLengths)
   {
      double initialSpeed = allVelocities[0].length();
      double finalSpeed = allVelocities[5].length();

      return 2 * arcLengths[0] / (initialSpeed + waypointSpeed) + (arcLengths[1] + arcLengths[2] + arcLengths[3]) / waypointSpeed
             + 2 * arcLengths[4] / (waypointSpeed + finalSpeed);
   }

   private void setAccelerationEndpointPositions()
   {
      for (int i : new int[] {0, 1})
      {
         FrameVector waypointToEndpoint = getWaypointToEndpoint(i);

         FrameVector oppositeWaypointToEndpoint = getOppositeWaypointToEndpoint(i);

         double scaleFactor = waypointToEndpoint.dot(oppositeWaypointToEndpoint) / oppositeWaypointToEndpoint.length()
                              * linearSplineLengthFactor.getDoubleValue();

         oppositeWaypointToEndpoint.normalize();
         oppositeWaypointToEndpoint.scale(scaleFactor);

         allPositions[accelerationEndpointIndices[i]].set(allPositions[waypointIndices[i]].getFramePointCopy());
         allPositions[accelerationEndpointIndices[i]].add(oppositeWaypointToEndpoint);
      }
   }

   private FrameVector getOppositeWaypointToEndpoint(int i)
   {
      FrameVector oppositeWaypointToEndpoint = allPositions[endpointIndices[i]].getFrameVectorCopy();
      oppositeWaypointToEndpoint.sub(allPositions[oppositeWaypointIndices[i]].getFrameVectorCopy());

      return oppositeWaypointToEndpoint;
   }

   private FrameVector getWaypointToEndpoint(int i)
   {
      FrameVector waypointToEndpoint = allPositions[endpointIndices[i]].getFrameVectorCopy();
      waypointToEndpoint.sub(allPositions[waypointIndices[i]].getFrameVectorCopy());

      return waypointToEndpoint;
   }

   private double[] getArcLengthsApproximatedByDistance(int[] indices)
   {
      double[] arcLengths = new double[indices.length - 1];

      for (int i = 0; i < indices.length - 1; i++)
      {
         arcLengths[i] = allPositions[indices[i]].distance(allPositions[indices[i + 1]]);
      }

      return arcLengths;
   }

   private double[] getArcLengthsCalculatedIteratively()
   {
      double[] arcLengths = new double[5];

      for (int i = 0; i < arcLengths.length; i++)
      {
         arcLengths[i] = concatenatedSplinesWithArcLengthApproximatedByDistance.getSplineByIndex(i).getArcLength();
      }

      return arcLengths;
   }

   private void setInitialAndFinalTimesPositionsAndVelocities()
   {
      FramePoint tempPositions = new FramePoint(referenceFrame);
      FrameVector tempVelocities = new FrameVector(referenceFrame);
      for (int i = 0; i < 2; i++)
      {
         positionSources[i].get(tempPositions);
         velocitySources[i].get(tempVelocities);
         tempPositions.changeFrame(referenceFrame);
         tempVelocities.changeFrame(referenceFrame);
         allPositions[endpointIndices[i]].set(tempPositions);
         allVelocities[endpointIndices[i]].set(tempVelocities);
      }

      allTimes[endpointIndices[0]].set(0.0);
      allTimes[endpointIndices[1]].set(stepTime.getDoubleValue());
   }

   protected void setWaypointPositions()
   {
      List<FramePoint> waypoints = null;
      waypointGenerationMethod.set(trajectoryParameters.getWaypointGenerationMethod());

      switch (waypointGenerationMethod.getEnumValue())
      {
         case STEP_ON_OR_OFF :
            waypoints = getWaypointsForStepOnOrOff();

            break;

         case BY_BOX :
            waypoints = getWaypointsFromABox(trajectoryParameters.getBox());

            break;

         default :
            waypoints = getWaypointsAtGroundClearance(SimpleTwoWaypointTrajectoryParameters.getDefaultGroundClearance());

            break;
      }

      for (int i = 0; i < 2; i++)
      {
         waypoints.get(i).changeFrame(referenceFrame);
         allPositions[waypointIndices[i]].set(waypoints.get(i));
      }

      checkForCloseWaypoints();
   }

   protected void checkForCloseWaypoints()
   {
      if (waypointsAreCloseTogether())
      {
         FramePoint midpoint = allPositions[waypointIndices[0]].getFramePointCopy();
         midpoint.add(allPositions[waypointIndices[1]].getFramePointCopy());
         midpoint.scale(0.5);
         
         for (int i = 0; i < 2; i++)
         {
            allPositions[waypointIndices[i]].set(midpoint);
         }
         
         waypointsAreTheSamePoint = true;
      }

      else
      {
         waypointsAreTheSamePoint = false;
      }
   }

   private List<FramePoint> getWaypointsForStepOnOrOff()
   {     
      List<FramePoint> waypoints = new ArrayList<FramePoint>();
      waypoints.add(allPositions[endpointIndices[0]].getFramePointCopy());
      waypoints.add(allPositions[endpointIndices[1]].getFramePointCopy());
      int indexOfMaxZ = (waypoints.get(0).getZ() > waypoints.get(1).getZ()) ? 0 : 1;
      
      double maxZ = waypoints.get(indexOfMaxZ).getZ();

      for (FramePoint waypoint : waypoints)
      {
         waypoint.setZ(maxZ + SimpleTwoWaypointTrajectoryParameters.getDefaultGroundClearance());
      }

      FrameVector planarEndpointOffset = allPositions[endpointIndices[1]].getFrameVectorCopy();
      planarEndpointOffset.sub(allPositions[endpointIndices[0]].getFrameVectorCopy());
      planarEndpointOffset.setZ(0.0);
      
      double[] fractionsOfStepDistanceToMoveWaypointForStepOnOrOff = SimpleTwoWaypointTrajectoryParameters.getStepOnOrOffProportionsThroughTrajectoryForGroundClearance();
      
      for(int i = 0; i < 2; i++)
      {
         FramePoint waypoint = waypoints.get(i);
         FrameVector planarWaypointOffset = new FrameVector(planarEndpointOffset);
         double scaleFactor = fractionsOfStepDistanceToMoveWaypointForStepOnOrOff[i];
         if(i == 1) scaleFactor = scaleFactor - 1.0;
         planarWaypointOffset.scale(scaleFactor);
         waypoint.add(planarWaypointOffset);
      }

      return waypoints;
   }

   private List<FramePoint> getWaypointsAtGroundClearance(double groundClearance)
   {
      return getWaypointsAtGroundClearance(groundClearance, SimpleTwoWaypointTrajectoryParameters.getDefaultProportionsThroughTrajectoryForGroundClearance());
   }

   private List<FramePoint> getWaypointsAtGroundClearance(double groundClearance, double[] proportionsThroughTrajectoryForGroundClearance)
   {
      FramePoint initialPosition = allPositions[0].getFramePointCopy();
      FramePoint finalPosition = allPositions[3].getFramePointCopy();
      positionSources[0].get(initialPosition);
      positionSources[1].get(finalPosition);
      initialPosition.changeFrame(referenceFrame);
      finalPosition.changeFrame(referenceFrame);

      List<FramePoint> waypoints = getWaypointsAtSpecifiedGroundClearance(initialPosition, finalPosition, groundClearance,
                                      proportionsThroughTrajectoryForGroundClearance);

      return waypoints;
   }

   public static List<FramePoint> getWaypointsAtSpecifiedGroundClearance(FramePoint initialPosition, FramePoint finalPosition, double groundClearance,
           double[] proportionsThroughTrajectoryForGroundClearance)
   {
      List<FramePoint> waypoints = new ArrayList<FramePoint>();
      waypoints.add(new FramePoint(initialPosition.getReferenceFrame()));
      waypoints.add(new FramePoint(initialPosition.getReferenceFrame()));

      for (int i = 0; i < 2; i++)
      {
         FramePoint waypoint = waypoints.get(i);
         waypoint.set(initialPosition);

         FrameVector offsetFromInitial = new FrameVector(waypoint.getReferenceFrame());
         offsetFromInitial.set(finalPosition);
         offsetFromInitial.sub(initialPosition);
         offsetFromInitial.scale(proportionsThroughTrajectoryForGroundClearance[i]);

         waypoint.add(offsetFromInitial);
         waypoint.setZ(waypoints.get(i).getZ() + groundClearance);
      }

      return waypoints;
   }

   private void setConcatenatedSplines(YoConcatenatedSplines concatenatedSplines)
   {
      double[] nonAccelerationEndpointTimes = new double[4];
      double[] accelerationEndpointTimes = new double[2];
      FramePoint[] nonAccelerationEndpointPositions = new FramePoint[4];
      FrameVector[] nonAccelerationEndpointVelocities = new FrameVector[4];

      for (int i = 0; i < 4; i++)
      {
         nonAccelerationEndpointTimes[i] = allTimes[nonAccelerationEndpointIndices[i]].getDoubleValue();
         nonAccelerationEndpointPositions[i] = allPositions[nonAccelerationEndpointIndices[i]].getFramePointCopy();
         nonAccelerationEndpointVelocities[i] = allVelocities[nonAccelerationEndpointIndices[i]].getFrameVectorCopy();
      }

      for (int i = 0; i < 2; i++)
      {
         accelerationEndpointTimes[i] = allTimes[accelerationEndpointIndices[i]].getDoubleValue();
      }

      concatenatedSplines.setCubicLinearQuinticLinearCubic(nonAccelerationEndpointTimes, nonAccelerationEndpointPositions, nonAccelerationEndpointVelocities,
              accelerationEndpointTimes);
   }

   private void visualizeSpline()
   {
      for (int i = 0; i < numberOfVisualizationMarkers; i++)
      {
         double t0 = concatenatedSplinesWithArcLengthCalculatedIteratively.getT0();
         double tf = concatenatedSplinesWithArcLengthCalculatedIteratively.getTf();
         double t = t0 + (double) i / (double) (numberOfVisualizationMarkers) * (tf - t0);
         compute(t);
         trajectoryBagOfBalls.setBall(desiredPosition.getFramePointCopy(), i);
      }

      for (int i = 0; i < nonAccelerationEndpointIndices.length; i++)
      {
         fixedPointBagOfBalls.setBall(allPositions[nonAccelerationEndpointIndices[i]].getFramePointCopy(), YoAppearance.AliceBlue(), i);
      }
   }

   public boolean isDone()
   {
      return timeIntoStep.getDoubleValue() >= stepTime.getDoubleValue();
   }

   private List<FramePoint> getWaypointsFromABox(Box3d box)
   {
      Transform3D boxTransform = box.getTransformCopy();
      Transform3D inverseBoxTransform = new Transform3D();
      inverseBoxTransform.invert(boxTransform);

      FramePoint initialPosition = allPositions[endpointIndices[0]].getFramePointCopy();
      FramePoint finalPosition = allPositions[endpointIndices[1]].getFramePointCopy();
      Point3d boxFrameInitialPosition = initialPosition.getPointCopy();
      Point3d boxFrameFinalPosition = finalPosition.getPointCopy();
      inverseBoxTransform.transform(boxFrameInitialPosition);
      inverseBoxTransform.transform(boxFrameFinalPosition);

      Point3d[] xyPlaneBoxIntersections = new Point3d[2];
      int index = 0;
      double a = (boxFrameFinalPosition.y - boxFrameInitialPosition.y) / (boxFrameFinalPosition.x - boxFrameInitialPosition.x);
      double b = boxFrameInitialPosition.y - a * boxFrameInitialPosition.x;
      Vector3d halfSideLengthsXY = new Vector3d(0.5 * box.getLength(), 0.5 * box.getWidth(), 0.0);
      double zVal = walkingControllerParameters.getAnkleHeight() + SimpleTwoWaypointTrajectoryParameters.getLowStepGroundClearance();

      for (double sign : new double[] {-1.0, 1.0})
      {
         double xEdge = sign * halfSideLengthsXY.x;
         double yEdge = sign * halfSideLengthsXY.y;
         double xGuess = (yEdge - b) / a;
         double yGuess = a * xEdge + b;

         if ((Math.abs(yGuess) <= halfSideLengthsXY.y) && (index < 2))
         {
            xyPlaneBoxIntersections[index] = new Point3d(xEdge, yGuess, zVal);
            index++;
         }

         if ((Math.abs(xGuess) <= halfSideLengthsXY.x) && (index < 2))
         {
            xyPlaneBoxIntersections[index] = new Point3d(xGuess, yEdge, zVal);
            index++;
         }
      }

      if (index != 2)
      {
         Vector3d translation = new Vector3d();
         box.getTransformCopy().get(translation);

         return getWaypointsAtGroundClearance(translation.z + zVal);
      }

      if (xyPlaneBoxIntersections[0].distance(boxFrameInitialPosition) > xyPlaneBoxIntersections[1].distance(boxFrameInitialPosition))
      {
         Point3d tempVec = xyPlaneBoxIntersections[1];
         xyPlaneBoxIntersections[1] = xyPlaneBoxIntersections[0];
         xyPlaneBoxIntersections[0] = tempVec;
      }

      List<FramePoint> waypoints = new ArrayList<FramePoint>();
      for (Point3d intersectionPoint : xyPlaneBoxIntersections)
      {
         Point3d tempPoint = new Point3d(intersectionPoint);
         boxTransform.transform(tempPoint);
         waypoints.add(new FramePoint(referenceFrame, tempPoint));
      }

      Vector3d directionOfFootstep = new Vector3d();
      directionOfFootstep.sub(finalPosition.getVectorCopy(), initialPosition.getVectorCopy());
      directionOfFootstep.normalize();
      double[] waypointShiftsToAvoidFootCollision = new double[] {-walkingControllerParameters.getFootForwardOffset(),
              walkingControllerParameters.getFootBackwardOffset()};
      for (int i = 0; i < 2; i++)
      {
         FramePoint waypointShift = new FramePoint(referenceFrame, directionOfFootstep);
         waypointShift.scale(waypointShiftsToAvoidFootCollision[i]);
         waypoints.get(i).add(waypointShift);
      }

      return waypoints;
   }
}
