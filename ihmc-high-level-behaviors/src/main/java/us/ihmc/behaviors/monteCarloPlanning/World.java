package us.ihmc.behaviors.monteCarloPlanning;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.opencv_core.Mat;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple4D.Vector4D32;

import java.util.ArrayList;

public class World
{
   private final Mat grid;
   private final Point2D goal;

   private final int gridHeight;
   private final int gridWidth;
   private final int goalMargin;

   public World(int goalMargin, int gridHeight, int gridWidth)
   {
      this.gridHeight = gridHeight;
      this.gridWidth = gridWidth;
      this.goalMargin = goalMargin;
      this.goal = new Point2D(30, 150);
      this.grid = new Mat(gridHeight, gridWidth, opencv_core.CV_8UC1);
   }

   public World(ArrayList<Vector4D32> obstacles, Point2D goal, int goalMargin, int gridHeight, int gridWidth)
   {
      this.grid = new Mat(gridHeight, gridWidth, opencv_core.CV_8UC1);
      this.gridHeight = gridHeight;
      this.gridWidth = gridWidth;
      this.goal = goal;
      this.goalMargin = goalMargin;
   }

   public void submitObstacles(ArrayList<Vector4D32> obstacles)
   {
      MonteCarloPlannerTools.fillObstacles(obstacles, grid);
   }

   public Mat getGrid()
   {
      return grid;
   }

   public int getGridHeight()
   {
      return gridHeight;
   }

   public int getGridWidth()
   {
      return gridWidth;
   }

   public Point2D getGoal()
   {
      return goal;
   }

   public int getGoalMargin()
   {
      return goalMargin;
   }
}
