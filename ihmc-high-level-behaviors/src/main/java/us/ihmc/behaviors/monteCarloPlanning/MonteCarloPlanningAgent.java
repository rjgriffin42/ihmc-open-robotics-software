package us.ihmc.behaviors.monteCarloPlanning;

import us.ihmc.euclid.tuple2D.Point2D;

import java.util.ArrayList;

/**
 * This class represents the robot and stores the state of the agent in
 * the Monte Carlo Planner including the current and past locations of the agent
 * as well as for handling the virtual sensor data available on the agent for simulated
 * rollouts in the Monte Carlo Tree Search.
 */
public class MonteCarloPlanningAgent
{
   private final RangeScanner rangeScanner = new RangeScanner();

   private final Point2D position = new Point2D();
   private final Point2D previousPosition = new Point2D();
   private final Point2D averagePosition = new Point2D();

   private ArrayList<Point2D> points = new ArrayList<>();

   public MonteCarloPlanningAgent(Point2D position)
   {
      this.position.set(position);
      this.previousPosition.set(position);
      this.averagePosition.set(position);
   }

   public void changeStateTo(Point2D newState)
   {
      previousPosition.set(position);
      position.set(newState);

      averagePosition.interpolate(position, 0.05);
   }

   public void measure(MonteCarloPlanningWorld world)
   {
      points.clear();
      points.addAll(rangeScanner.scan(position, world));
   }

   public void addMeasurements(ArrayList<Point2D> measurements)
   {
      points.clear();
      points.addAll(measurements);
   }

   public Point2D getPosition()
   {
      return position;
   }

   public RangeScanner getRangeScanner()
   {
      return rangeScanner;
   }

   public Point2D getAveragePosition()
   {
      return averagePosition;
   }

   public Point2D getPreviousPosition()
   {
      return previousPosition;
   }

   public ArrayList<Point2D> getScanPoints()
   {
      return points;
   }

}
