package us.ihmc.commonWalkingControlModules.trajectories;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import gnu.trove.list.array.TDoubleArrayList;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPolynomial3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPolynomial3D.TrajectoryColorType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.MathTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;
import us.ihmc.robotics.geometry.Direction;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.lists.GenericTypeBuilder;
import us.ihmc.robotics.lists.RecyclingArrayList;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.math.trajectories.YoPolynomial;
import us.ihmc.robotics.math.trajectories.YoPolynomial3D;
import us.ihmc.robotics.math.trajectories.waypoints.PolynomialOrder;
import us.ihmc.robotics.math.trajectories.waypoints.TrajectoryPointOptimizer;

/**
 * This class is a wrapper for the TrajectoryPointOptimizer. It was made for trajectories in 3d
 * space and creates third order trajectories. It can be used either to generate waypoint times as
 * velocities, or it can serve as an actual trajectory with the optimization taking place when the
 * trajectory is initialized.
 *
 * The trajectory is continuous in acceleration but does not have zero initial and final
 * acceleration. The optimization finds waypoint times and velocities such that the overall squared
 * acceleration is minimized.
 *
 * @author gwiedebach
 *
 */
public class PositionOptimizedTrajectoryGenerator
{
   public static final int dimensions = 3;
   public static final PolynomialOrder order = PolynomialOrder.ORDER3;
   public static final ReferenceFrame trajectoryFrame = ReferenceFrame.getWorldFrame();

   private final String namePrefix;

   private final TrajectoryPointOptimizer optimizer;
   private final YoInteger maxIterations;
   private final RecyclingArrayList<TDoubleArrayList> coefficients;
   private final EnumMap<Direction, ArrayList<YoPolynomial>> trajectories = new EnumMap<>(Direction.class);
   private final double[] tempCoeffs = new double[order.getCoefficients()];

   private final FramePoint3D initialPosition = new FramePoint3D();
   private final FrameVector3D initialVelocity = new FrameVector3D();
   private final FramePoint3D finalPosition = new FramePoint3D();
   private final FrameVector3D finalVelocity = new FrameVector3D();
   private final FramePoint3D waypointPosition = new FramePoint3D();
   private final RecyclingArrayList<TDoubleArrayList> waypointPositions;

   private final TDoubleArrayList initialPositionArray = new TDoubleArrayList(dimensions);
   private final TDoubleArrayList initialVelocityArray = new TDoubleArrayList(dimensions);
   private final TDoubleArrayList finalPositionArray = new TDoubleArrayList(dimensions);
   private final TDoubleArrayList finalVelocityArray = new TDoubleArrayList(dimensions);
   private final TDoubleArrayList waypointVelocity = new TDoubleArrayList(dimensions);

   private final YoVariableRegistry registry;
   private final YoBoolean isDone;
   private final YoBoolean optimizeInOneTick;
   private final YoBoolean hasConverged;
   private final YoInteger segments;
   private final YoInteger activeSegment;
   private final ArrayList<YoDouble> waypointTimes = new ArrayList<>();

   private final YoFramePoint desiredPosition;
   private final YoFrameVector desiredVelocity;
   private final YoFrameVector desiredAcceleration;

   private final YoGraphicPolynomial3D trajectoryViz;

   private final YoDouble maxSpeed;
   private final YoDouble maxSpeedTime;
   private final FrameVector3D tempVelocity = new FrameVector3D();

   public PositionOptimizedTrajectoryGenerator()
   {
      this("", new YoVariableRegistry(""));
   }

   public PositionOptimizedTrajectoryGenerator(String namePrefix, YoVariableRegistry parentRegistry)
   {
      this(namePrefix, parentRegistry, null, TrajectoryPointOptimizer.maxIterations, TrajectoryPointOptimizer.maxWaypoints);
   }

   public PositionOptimizedTrajectoryGenerator(String namePrefix, YoVariableRegistry parentRegistry, YoGraphicsListRegistry graphicsListRegistry)
   {
      this(namePrefix, parentRegistry, graphicsListRegistry, TrajectoryPointOptimizer.maxIterations, TrajectoryPointOptimizer.maxWaypoints);
   }

   public PositionOptimizedTrajectoryGenerator(String namePrefix, YoVariableRegistry parentRegistry, YoGraphicsListRegistry graphicsListRegistry,
                                               int maxIterations, int maxWaypoints)
   {
      this.namePrefix = namePrefix;

      coefficients = new RecyclingArrayList<>(new GenericTypeBuilder<TDoubleArrayList>()
      {
         @Override
         public TDoubleArrayList newInstance()
         {
            TDoubleArrayList ret = new TDoubleArrayList(order.getCoefficients());
            for (int i = 0; i < order.getCoefficients(); i++)
               ret.add(0.0);
            return ret;
         }
      });

      waypointPositions = new RecyclingArrayList<>(new GenericTypeBuilder<TDoubleArrayList>()
      {
         @Override
         public TDoubleArrayList newInstance()
         {
            TDoubleArrayList ret = new TDoubleArrayList(dimensions);
            for (int i = 0; i < dimensions; i++)
               ret.add(0.0);
            return ret;
         }
      });

      registry = new YoVariableRegistry(namePrefix + "Trajectory");
      optimizer = new TrajectoryPointOptimizer(namePrefix, dimensions, order, registry);
      this.maxIterations = new YoInteger(namePrefix + "MaxIterations", registry);
      this.maxIterations.set(maxIterations);
      isDone = new YoBoolean(namePrefix + "IsDone", registry);
      optimizeInOneTick = new YoBoolean(namePrefix + "OptimizeInOneTick", registry);
      hasConverged = new YoBoolean(namePrefix + "HasConverged", registry);
      segments = new YoInteger(namePrefix + "Segments", registry);
      activeSegment = new YoInteger(namePrefix + "ActiveSegment", registry);

      optimizeInOneTick.set(maxIterations >= 0);
      hasConverged.set(optimizeInOneTick.getBooleanValue());

      desiredPosition = new YoFramePoint(namePrefix + "DesiredPosition", trajectoryFrame, registry);
      desiredVelocity = new YoFrameVector(namePrefix + "DesiredVelocity", trajectoryFrame, registry);
      desiredAcceleration = new YoFrameVector(namePrefix + "DesiredAcceleration", trajectoryFrame, registry);

      for (int i = 0; i < dimensions; i++)
      {
         initialPositionArray.add(0.0);
         initialVelocityArray.add(0.0);
         finalPositionArray.add(0.0);
         finalVelocityArray.add(0.0);
      }

      for (Direction axis : Direction.values)
      {
         ArrayList<YoPolynomial> segments = new ArrayList<>();
         trajectories.put(axis, segments);
      }

      while (waypointTimes.size() <= maxWaypoints)
         extendBySegment(registry);

      reset();
      parentRegistry.addChild(registry);

      if (graphicsListRegistry != null)
      {
         List<YoPolynomial3D> yoPolynomial3Ds = YoPolynomial3D.createYoPolynomial3DList(trajectories.get(Direction.X), trajectories.get(Direction.Y),
                                                                                        trajectories.get(Direction.Z));
         trajectoryViz = new YoGraphicPolynomial3D(namePrefix + "Trajectory", null, yoPolynomial3Ds, waypointTimes, 0.01, 50, 8, registry);
         graphicsListRegistry.registerYoGraphic(namePrefix + "Trajectory", trajectoryViz);

         trajectoryViz.setColorType(TrajectoryColorType.ACCELERATION_BASED);
      }
      else
         trajectoryViz = null;

      maxSpeed = new YoDouble("MaxVelocity", registry);
      maxSpeedTime = new YoDouble("MaxVelocityTime", registry);
   }

   private void extendBySegment(YoVariableRegistry registry)
   {
      int size = waypointTimes.size() + 1;
      for (Direction axis : Direction.values)
         trajectories.get(axis).add(new YoPolynomial(namePrefix + "Segment" + size + "Axis" + axis.getIndex(), order.getCoefficients(), registry));
      waypointTimes.add(new YoDouble(namePrefix + "WaypointTime" + size, registry));
      waypointPositions.add();
   }

   /**
    * Resets the optimizer and removes all waypoints as well as previous start and end conditions.
    */
   public void reset()
   {
      initialPosition.setToNaN(trajectoryFrame);
      initialVelocity.setToNaN(trajectoryFrame);
      finalPosition.setToNaN(trajectoryFrame);
      finalVelocity.setToNaN(trajectoryFrame);
      segments.set(1);

      this.waypointPositions.clear();
      optimizer.setWaypoints(waypointPositions);
      coefficients.clear();
      coefficients.add();
   }

   /**
    * Set the desired position and velocity at the start and end points of the trajectory.
    *
    * @param initialPosition
    * @param initialVelocity
    * @param finalPosition
    * @param finalVelocity
    */
   public void setEndpointConditions(FramePoint3D initialPosition, FrameVector3D initialVelocity, FramePoint3D finalPosition, FrameVector3D finalVelocity)
   {
      this.initialPosition.setIncludingFrame(initialPosition);
      this.initialVelocity.setIncludingFrame(initialVelocity);
      this.initialPosition.changeFrame(trajectoryFrame);
      this.initialVelocity.changeFrame(trajectoryFrame);

      this.finalPosition.setIncludingFrame(finalPosition);
      this.finalVelocity.setIncludingFrame(finalVelocity);
      this.finalPosition.changeFrame(trajectoryFrame);
      this.finalVelocity.changeFrame(trajectoryFrame);

      for (Direction axis : Direction.values)
      {
         initialPositionArray.set(axis.getIndex(), this.initialPosition.getElement(axis.getIndex()));
         initialVelocityArray.set(axis.getIndex(), this.initialVelocity.getElement(axis.getIndex()));
         finalPositionArray.set(axis.getIndex(), this.finalPosition.getElement(axis.getIndex()));
         finalVelocityArray.set(axis.getIndex(), this.finalVelocity.getElement(axis.getIndex()));
      }

      optimizer.setEndPoints(initialPositionArray, initialVelocityArray, finalPositionArray, finalVelocityArray);
   }

   /**
    * Set the positions of the waypoints on the trajectory.
    *
    * @param waypointPositions
    */
   public void setWaypoints(ArrayList<FramePoint3D> waypointPositions)
   {
      if (waypointPositions.size() > waypointTimes.size())
         throw new RuntimeException("Too many waypoints");

      this.waypointPositions.clear();
      coefficients.clear();

      coefficients.add();
      for (int i = 0; i < waypointPositions.size(); i++)
      {
         waypointPosition.setIncludingFrame(waypointPositions.get(i));
         waypointPosition.changeFrame(trajectoryFrame);
         TDoubleArrayList waypoint = this.waypointPositions.add();
         for (Direction axis : Direction.values)
            waypoint.set(axis.getIndex(), this.waypointPosition.getElement(axis.getIndex()));
         coefficients.add();
      }

      optimizer.setWaypoints(this.waypointPositions);
      segments.set(waypointPositions.size() + 1);
   }

   /**
    * This method initialized the trajectory and does the optimization. This has to be called after
    * setting the end point conditions and waypoints. It has to be called regardless of whether is
    * class is used as an actual trajectory or just to compute optimal waypoint times and
    * velocities.
    */
   public void initialize()
   {
      if (initialPosition.containsNaN())
         throw new RuntimeException("Does not have valid enpoint conditions. Did you call setEndpointConditions?");

      if (optimizeInOneTick.getBooleanValue())
      {
         optimizer.compute(maxIterations.getIntegerValue());
         hasConverged.set(true);
      }
      else
      {
         hasConverged.set(false);
         optimizer.compute(0);
      }

      updateVariablesFromOptimizer();
   }

   private void updateVariablesFromOptimizer()
   {
      for (int i = 0; i < segments.getIntegerValue() - 1; i++)
         waypointTimes.get(i).set(optimizer.getWaypointTime(i));

      waypointTimes.get(segments.getIntegerValue() - 1).set(1.0);

      for (int i = segments.getIntegerValue(); i < waypointTimes.size(); i++)
         waypointTimes.get(i).set(Double.NaN);

      for (int dimension = 0; dimension < Direction.values.length; dimension++)
      {
         optimizer.getPolynomialCoefficients(coefficients, dimension);
         Direction axis = Direction.values[dimension];
         for (int i = 0; i < segments.getIntegerValue(); i++)
         {
            coefficients.get(i).toArray(tempCoeffs);
            trajectories.get(axis).get(i).setDirectlyReverse(tempCoeffs);
         }
      }

      isDone.set(false);
      visualize();
   }

   private void visualize()
   {
      if (trajectoryViz == null)
         return;

      trajectoryViz.showGraphic();
   }
   
   /**
    * Attempt at improving the trajectory if iterative improvement is desired.
    * @return whether an optimization step was done or not.
    */
   public boolean doOptimizationUpdate()
   {
      if (!hasConverged.getBooleanValue())
      {
         hasConverged.set(optimizer.doFullTimeUpdate());
         updateVariablesFromOptimizer();
      }
      
      return !hasConverged();
   }

   /**
    * Evaluates the trajectory at the given dimensionless time. Time is assumed to go from 0.0 at
    * the start of the trajectory to 1.0 at the end.
    *
    * @param time
    */
   public void compute(double time)
   {
      doOptimizationUpdate();
      isDone.set(time > 1.0);

      if (isDone())
      {
         desiredPosition.setToZero(true);
         desiredVelocity.setToZero(true);
         desiredAcceleration.setToZero(true);
         return;
      }

      time = MathTools.clamp(time, 0.0, 1.0);

      int activeSegment = 0;
      for (int i = 0; i < segments.getIntegerValue() - 1; i++)
      {
         double waypointTime = waypointTimes.get(i).getDoubleValue();
         if (time > waypointTime)
            activeSegment = i + 1;
         else
            break;
      }
      this.activeSegment.set(activeSegment);

      for (int dimension = 0; dimension < Direction.values.length; dimension++)
      {
         Direction axis = Direction.values[dimension];
         YoPolynomial polynomial = trajectories.get(axis).get(activeSegment);
         polynomial.compute(time);
         desiredPosition.setElement(dimension, polynomial.getPosition());
         desiredVelocity.setElement(dimension, polynomial.getVelocity());
         desiredAcceleration.setElement(dimension, polynomial.getAcceleration());
      }
   }

   /**
    * Call this function after initialize to retrieve the optimal waypoint time for a given waypoint
    * index.
    *
    * @param waypointIndex
    * @return
    */
   public double getWaypointTime(int waypointIndex)
   {
      return optimizer.getWaypointTime(waypointIndex);
   }

   /**
    * Call this function after initialize to retrieve the optimal waypoint velocity for a given
    * waypoint index.
    *
    * @param waypointIndex
    * @param waypointVelocityToPack
    */
   public void getWaypointVelocity(int waypointIndex, FrameVector3D waypointVelocityToPack)
   {
      optimizer.getWaypointVelocity(this.waypointVelocity, waypointIndex);
      waypointVelocityToPack.setToZero(trajectoryFrame);
      for (int d = 0; d < Direction.values.length; d++)
         waypointVelocityToPack.setElement(d, this.waypointVelocity.get(d));
   }

   public boolean isDone()
   {
      return isDone.getBooleanValue();
   }

   public void getPosition(FramePoint3D positionToPack)
   {
      desiredPosition.getFrameTupleIncludingFrame(positionToPack);
   }

   public void getVelocity(FrameVector3D velocityToPack)
   {
      desiredVelocity.getFrameTupleIncludingFrame(velocityToPack);
   }

   public void getAcceleration(FrameVector3D accelerationToPack)
   {
      desiredAcceleration.getFrameTupleIncludingFrame(accelerationToPack);
   }

   public void getLinearData(FramePoint3D positionToPack, FrameVector3D velocityToPack, FrameVector3D accelerationToPack)
   {
      getPosition(positionToPack);
      getVelocity(velocityToPack);
      getAcceleration(accelerationToPack);
   }

   public void informDone()
   {
      desiredPosition.setToZero(true);
      desiredVelocity.setToZero(true);
      desiredAcceleration.setToZero(true);
   }

   public void showVisualization()
   {
      if (trajectoryViz == null)
         return;
      trajectoryViz.showGraphic();
   }

   public void hideVisualization()
   {
      if (trajectoryViz == null)
         return;
      trajectoryViz.hideGraphic();
   }

   /**
    * Returns whether the trajectory optimization has converged or not. This is useful when continuously improving
    * the solution quality instead of waiting for the optimizer to finish in the initialize method.
    *
    * @return whether the optimizer has converged or not
    */
   public boolean hasConverged()
   {
      return hasConverged.getBooleanValue();
   }

   /**
    * Numerically compute the maximum speed along the trajectory and the time at which this speed
    * occurs.
    */
   public void computeMaxSpeed()
   {
      computeMaxSpeed(1.0E-5);
   }

   /**
    * Numerically compute the maximum speed along the trajectory and the time at which this speed
    * occurs. The time precision can be specified.
    */
   public void computeMaxSpeed(double timeIncrement)
   {
      maxSpeed.set(Double.NEGATIVE_INFINITY);
      maxSpeedTime.set(Double.NaN);

      for (double time = 0.0; time <= 1.0; time += timeIncrement)
      {
         compute(time);
         getVelocity(tempVelocity);
         double speed = tempVelocity.length();
         if (speed > maxSpeed.getDoubleValue())
         {
            maxSpeed.set(speed);
            maxSpeedTime.set(time);
         }
      }
   }

   public double getMaxSpeed()
   {
      return maxSpeed.getDoubleValue();
   }

   public double getMaxSpeedTime()
   {
      return maxSpeedTime.getDoubleValue();
   }
}
