package us.ihmc.robotics.geometry;

import us.ihmc.robotics.MathTools;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

public class PointToLineUnProjector
{
   Point2d pointA = new Point2d();
   Vector2d difference = new Vector2d();
   double zA;
   double zDifference;
   boolean useX = true;
   double qA;
   double qMult;

   public void setLine(Point2d point0, Point2d point1, double point0z, double point1z)
   {
      pointA.set(point0);
      difference.sub(point1, point0);
      zA = point0z;
      zDifference = point1z - point0z;
      useX = Math.abs(difference.getX()) > Math.abs(difference.getY());

      if (useX)
      {
         qA = pointA.getX();
         qMult = 1 / difference.getX();
      }
      else
      {
         qA = pointA.getY();
         qMult = 1 / difference.getY();
      }

      if (!MathTools.isFinite(qMult))
         qMult = 0;

   }

   public double unProject(double x, double y)
   {
      double s = ((useX ? x : y) - qA) * qMult;
      return zA + zDifference * s;
   }
}
