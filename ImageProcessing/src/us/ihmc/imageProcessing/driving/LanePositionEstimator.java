package us.ihmc.imageProcessing.driving;

import us.ihmc.utilities.math.geometry.BoundingBox2d;
import us.ihmc.utilities.math.geometry.Line2d;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * User: Matt
 * Date: 3/7/13
 */
public class LanePositionEstimator
{
   private ArrayList<Line2d> lines = new ArrayList<Line2d>();
   private LanePositionIndicatorPanel lanePositionIndicatorPanel;
   private SteeringInputEstimator steeringInputEstimator;
   private Dimension imageSize = new Dimension(640, 480);
   private Line2d axis = new Line2d(new Point2d(0, 320), new Vector2d(1.0, 0.0));
   private double maxAngle = 90.0;

   public LanePositionEstimator(LanePositionIndicatorPanel lanePositionIndicatorPanel, SteeringInputEstimator steeringInputEstimator)
   {
      this.lanePositionIndicatorPanel = lanePositionIndicatorPanel;
      this.steeringInputEstimator = steeringInputEstimator;
   }

   public void setLines(ArrayList<Line2d> lines)
   {
      this.lines = lines;
      updateLanePositionEstimate();
   }

   public void updateLanePositionEstimate()
   {
      Line2d leftEdgeOfRoad = null;
      Line2d rightEdgeOfRoad = null;
      for (Line2d line : lines)
      {
         if (leftEdgeOfRoad == null && rightEdgeOfRoad == null)
         {
            leftEdgeOfRoad = line;
            rightEdgeOfRoad = line;
         }
         else
         {
            if (line.getSlope() < leftEdgeOfRoad.getSlope())
            {
               leftEdgeOfRoad = line;
            }
            if (line.getSlope() > rightEdgeOfRoad.getSlope())
            {
               rightEdgeOfRoad = line;
            }
         }
      }

      if (leftEdgeOfRoad == null)
      {
         return;
      }
      double leftX = leftEdgeOfRoad.intersectionWith(axis).getX();
      double rightX = rightEdgeOfRoad.intersectionWith(axis).getX();
      double range = rightX - leftX;
      double midPoint = leftX + (range / 2.0);
      double carX = imageSize.width / 2.0;
      double offset = (carX - midPoint) / (range / 2.0);

      lanePositionIndicatorPanel.setOffset(offset);
      steeringInputEstimator.setAngleInDegrees(maxAngle * offset);
   }

   public void setScreenDimension(Dimension screenDimension)
   {
      imageSize = screenDimension;
      axis = new Line2d(new Point2d(0, screenDimension.getHeight()), new Vector2d(1.0, 0.0));
   }

}
