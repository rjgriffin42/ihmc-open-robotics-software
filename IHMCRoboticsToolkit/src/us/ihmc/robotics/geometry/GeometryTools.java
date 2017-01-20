package us.ihmc.robotics.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple2d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.math.Epsilons;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;

public class GeometryTools
{
   public static final boolean DEBUG = false;

   private static final double EPSILON = 1e-6;

   /**
    * Computes the minimum distance between a 3D point and an infinitely long 3D line defined by a point and a direction.
    * <a href="http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html"> Useful link</a>.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if {@code lineDirection.length() < Epsilons.ONE_TRILLIONTH}, this method returns the distance between {@code pointOnLine} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param point 3D point to compute the distance from the line. Not modified.
    * @param pointOnLine point located on the line. Not modified.
    * @param lineDirection direction of the line. Not modified.
    * @return the minimum distance between the 3D point and the 3D line.
    */
   public static double distanceFromPointToLine(Point3d point, Point3d pointOnLine, Vector3d lineDirection)
   {
      return distanceFromPointToLine(point, pointOnLine.getX(), pointOnLine.getY(), pointOnLine.getZ(), lineDirection.getX(), lineDirection.getY(),
                                     lineDirection.getZ());
   }

   /**
    * Computes the minimum distance between a 3D point and an infinitely long 3D line defined by two points.
    * <a href="http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html"> Useful link</a>.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if {@code firstPointOnLine.distance(secondPointOnLine) < Epsilons.ONE_TRILLIONTH}, this method returns the distance between {@code firstPointOnLine} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param point 3D point to compute the distance from the line. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @return the minimum distance between the 3D point and the 3D line.
    */
   public static double distanceFromPointToLine(Point3d point, Point3d firstPointOnLine, Point3d secondPointOnLine)
   {
      double pointOnLineX = firstPointOnLine.getX();
      double pointOnLineY = firstPointOnLine.getY();
      double pointOnLineZ = firstPointOnLine.getZ();
      double lineDirectionX = secondPointOnLine.getX() - firstPointOnLine.getX();
      double lineDirectionY = secondPointOnLine.getY() - firstPointOnLine.getY();
      double lineDirectionZ = secondPointOnLine.getZ() - firstPointOnLine.getZ();
      return distanceFromPointToLine(point, pointOnLineX, pointOnLineY, pointOnLineZ, lineDirectionX, lineDirectionY, lineDirectionZ);
   }

   /**
    * Computes the minimum distance between a 3D point and an infinitely long 3D line defined by a point and a direction.
    * <a href="http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html"> Useful link</a>.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if {@code lineDirection.length() < Epsilons.ONE_TRILLIONTH}, this method returns the distance between {@code pointOnLine} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param point 3D point to compute the distance from the line. Not modified.
    * @param pointOnLineX x-coordinate of a point located on the line.
    * @param pointOnLineY y-coordinate of a point located on the line.
    * @param pointOnLineZ z-coordinate of a point located on the line.
    * @param lineDirectionX x-component of the line direction.
    * @param lineDirectionY y-component of the line direction. 
    * @param lineDirectionZ z-component of the line direction.
    * @return the minimum distance between the 3D point and the 3D line.
    */
   public static double distanceFromPointToLine(Point3d point, double pointOnLineX, double pointOnLineY, double pointOnLineZ, double lineDirectionX,
                                                double lineDirectionY, double lineDirectionZ)
   {
      double directionMagnitude = lineDirectionX * lineDirectionX + lineDirectionY * lineDirectionY + lineDirectionZ * lineDirectionZ;
      directionMagnitude = Math.sqrt(directionMagnitude);

      double dx = pointOnLineX - point.getX();
      double dy = pointOnLineY - point.getY();
      double dz = pointOnLineZ - point.getZ();

      if (directionMagnitude < Epsilons.ONE_TRILLIONTH)
      {
         return Math.sqrt(dx * dx + dy * dy + dz * dz);
      }
      else
      {
         double crossX = lineDirectionY * dz - lineDirectionZ * dy;
         double crossY = lineDirectionZ * dx - lineDirectionX * dz;
         double crossZ = lineDirectionX * dy - lineDirectionY * dx;
         double distance = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
         distance /= directionMagnitude;

         return distance;
      }
   }

   /**
    * Returns the minimum distance between a 2D point and an infinitely long 2D line defined by two points.
    * <p>
    * WARNING: the 3D arguments are projected onto the XY-plane to perform the actual computation in 2D.
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if {@code firstPointOnLine2d.distance(secondPointOnLine2d) < Epsilons.ONE_TRILLIONTH}, this method returns the distance between {@code firstPointOnLine2d} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param point the 3D point is projected onto the xy-plane. It's projection is used to compute the distance from the line. Not modified.
    * @param firstPointOnLine the projection of this 3D onto the xy-plane refers to the first point on the 2D line. Not modified.
    * @param secondPointOnLine the projection of this 3D onto the xy-plane refers to the second point one the 2D line. Not modified.
    * @return the minimum distance between the 2D point and the 2D line.
    */
   public static double distanceFromPointToLine2d(FramePoint point, FramePoint firstPointOnLine, FramePoint secondPointOnLine)
   {
      // FIXME Need to verify that all the arguments are expressed in the same reference frame.
      return distanceFromPointToLine(point.getX(), point.getY(), firstPointOnLine.getX(), firstPointOnLine.getY(), secondPointOnLine.getX(), secondPointOnLine.getY());
   }

   /**
    * Returns the minimum distance between a 2D point and an infinitely long 2D line defined by two points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if {@code firstPointOnLine.distance(secondPointOnLine) < Epsilons.ONE_TRILLIONTH}, this method returns the distance between {@code firstPointOnLine} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param point 2D point to compute the distance from the line. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @return the minimum distance between the 2D point and the 2D line.
    */
   public static double distanceFromPointToLine(Point2d point, Point2d firstPointOnLine, Point2d secondPointOnLine)
   {
      return distanceFromPointToLine(point.getX(), point.getY(), firstPointOnLine.getX(), firstPointOnLine.getY(), secondPointOnLine.getX(), secondPointOnLine.getY());
   }

   /**
    * Returns the minimum distance between a 2D point and an infinitely long 2D line defined by two points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if {@code firstPointOnLine.distance(secondPointOnLine) < Epsilons.ONE_TRILLIONTH}, this method returns the distance between {@code firstPointOnLine} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param pointX x-coordinate of the query.
    * @param pointY y-coordinate of the query.
    * @param firstPointOnLineX x-coordinate of a first point located on the line.
    * @param firstPointOnLineY y-coordinate of a first point located on the line.
    * @param secondPointOnLineX x-coordinate of a second point located on the line.
    * @param secondPointOnLineY y-coordinate of a second point located on the line.
    * @return the minimum distance between the 2D point and the 2D line.
    */
   public static double distanceFromPointToLine(double pointX, double pointY, double firstPointOnLineX, double firstPointOnLineY, double secondPointOnLineX, double secondPointOnLineY)
   {
      double dx = firstPointOnLineY - pointY;
      double dy = firstPointOnLineX - pointX;

      if (firstPointOnLineX - secondPointOnLineX == 0 && firstPointOnLineY - secondPointOnLineY == 0)
      {
         return Math.sqrt(dx * dx + dy * dy);
      }
      else
      {
         double lineDx = secondPointOnLineX - firstPointOnLineX;
         double lineDy = secondPointOnLineY - firstPointOnLineY;

         double numerator = Math.abs(lineDx * dx - dy * lineDy);
         double denominator = Math.sqrt(lineDx * lineDx + lineDy * lineDy);

         return numerator / denominator;
      }
   }

   /**
    * Returns the minimum distance between a point and a given line segment.
    * Holds true if line segment shrinks to a single point.
    *
    * @param pointX x coordinate of point to be tested.
    * @param pointY y coordinate of point to be tested.
    * @param lineSegmentStart starting point of the line segment. Not modified.
    * @param lineSegmentEnd end point of the line segment. Not modified.
    * @return the minimum distance between the 2D point and the 2D line segment.
    */
   public static double distanceFromPointToLineSegment(double pointX, double pointY, Point2d lineSegmentStart, Point2d lineSegmentEnd)
   {
      double startAngleDot, endAngleDot;

      startAngleDot = (lineSegmentEnd.x - lineSegmentStart.x) * (pointX - lineSegmentStart.x);
      startAngleDot += (lineSegmentEnd.y - lineSegmentStart.y) * (pointY - lineSegmentStart.y);

      endAngleDot = (lineSegmentStart.x - lineSegmentEnd.x) * (pointX - lineSegmentEnd.x);
      endAngleDot += (lineSegmentStart.y - lineSegmentEnd.y) * (pointY - lineSegmentEnd.y);

      if ((startAngleDot >= 0.0) && (endAngleDot >= 0.0))
      {
         return distanceFromPointToLine(pointX, pointY, lineSegmentStart.getX(), lineSegmentStart.getY(), lineSegmentEnd.getX(), lineSegmentEnd.getY());
      }

      if (startAngleDot < 0.0)
      {
         return distanceBetweenPoints(lineSegmentStart.getX(), lineSegmentStart.getY(), pointX, pointY);
      }
      else
      {
         if (endAngleDot >= 0.0)
         {
            throw new RuntimeException("totally not a physical situation here");
         }

         return distanceBetweenPoints(lineSegmentEnd.getX(), lineSegmentEnd.getY(), pointX, pointY);
      }
   }

   /**
    * Returns the minimum distance between a point and a given line segment.
    * Holds true if line segment shrinks to a single point.
    *
    * @param point 2D point to compute the distance from the line segment. Not modified.
    * @param lineSegmentStart starting point of the line segment. Not modified.
    * @param lineSegmentEnd end point of the line segment. Not modified.
    * @return the minimum distance between the 2D point and the 2D line segment.
    */
   public static double distanceFromPointToLineSegment(Point2d point, Point2d lineSegmentStart, Point2d lineSegmentEnd)
   {
      return distanceFromPointToLineSegment(point.x, point.y, lineSegmentStart, lineSegmentEnd);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left side of an infinitely long line defined by two points.
    * "Left side" is determined based on order of {@code lineStart} and {@code lineEnd}.
    * <p>
    * For instance, given the {@code lineStart} coordinates x = 0, and y = 0, and the {@code lineEnd} coordinates x = 1, y = 0,
    * a point located on the left side of this line has a negative y coordinate.
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param point the query point. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @return {@code true} if the point is on the left side of the line, {@code false} if the point is on the right side or exactly on the line.
    */
   public static boolean isPointOnLeftSideOfLine(Point2d point, Point2d firstPointOnLine, Point2d secondPointOnLine)
   {
      return isPointOnSideOfLine(point, firstPointOnLine, secondPointOnLine, RobotSide.LEFT);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the right side of an infinitely long line defined by two points.
    * "Right side" is determined based on order of {@code lineStart} and {@code lineEnd}.
    * <p>
    * For instance, given the {@code lineStart} coordinates x = 0, and y = 0, and the {@code lineEnd} coordinates x = 1, y = 0,
    * a point located on the right side of this line has a positive y coordinate.
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param point the query point. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @return {@code true} if the point is on the right side of the line, {@code false} if the point is on the left side or exactly on the line.
    */
   public static boolean isPointOnRightSideOfLine(Point2d point, Point2d firstPointOnLine, Point2d secondPointOnLine)
   {
      return isPointOnSideOfLine(point, firstPointOnLine, secondPointOnLine, RobotSide.RIGHT);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left or right side of an infinitely long line defined by two points.
    * The idea of "side" is determined based on order of {@code lineStart} and {@code lineEnd}.
    * <p>
    * For instance, given the {@code lineStart} coordinates x = 0, and y = 0, and the {@code lineEnd} coordinates x = 1, y = 0,
    * a point located on:
    * <ul>
    *    <li> the left side of this line has a negative y coordinate.
    *    <li> the right side of this line has a positive y coordinate.
    * </ul>
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param point the query point. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @param side the query of the side.
    * @return {@code true} if the point is on the query side of the line, {@code false} if the point is on the opposite side or exactly on the line.
    */
   public static boolean isPointOnSideOfLine(Point2d point, Point2d firstPointOnLine, Point2d secondPointOnLine, RobotSide side)
   {
      return isPointOnSideOfLine(point.getX(), point.getY(), firstPointOnLine, secondPointOnLine, side);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left or right side of an infinitely long line defined by two points.
    * The idea of "side" is determined based on order of {@code lineStart} and {@code lineEnd}.
    * <p>
    * For instance, given the {@code lineStart} coordinates x = 0, and y = 0, and the {@code lineEnd} coordinates x = 1, y = 0,
    * a point located on:
    * <ul>
    *    <li> the left side of this line has a negative y coordinate.
    *    <li> the right side of this line has a positive y coordinate.
    * </ul>
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param pointX the x-coordinate of the query point.
    * @param pointY the y-coordinate of the query point.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @param side the query of the side.
    * @return {@code true} if the point is on the query side of the line, {@code false} if the point is on the opposite side or exactly on the line.
    */
   public static boolean isPointOnSideOfLine(double pointX, double pointY, Point2d firstPointOnLine, Point2d secondPointOnLine, RobotSide side)
   {
      double pointOnLineX = firstPointOnLine.getX();
      double pointOnLineY = firstPointOnLine.getY();
      double lineDirectionX = secondPointOnLine.getX() - firstPointOnLine.getX();
      double lineDirectionY = secondPointOnLine.getY() - firstPointOnLine.getY();
      return isPointOnSideOfLine(pointX, pointY, pointOnLineX, pointOnLineY, lineDirectionX, lineDirectionY, side);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left or right side of an infinitely long line.
    * The idea of "side" is determined based on the direction of the line.
    * <p>
    * For instance, given the {@code lineDirection} components x = 0, and y = 1, and the {@code pointOnLine} coordinates x = 0, and y = 0, 
    * a point located on:
    * <ul>
    *    <li> the left side of this line has a negative y coordinate.
    *    <li> the right side of this line has a positive y coordinate.
    * </ul>
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param point the query point. Not modified.
    * @param pointOnLine a point positioned on the infinite line. Not modified.
    * @param lineDirection the direction of the infinite line. Not modified.
    * @param side the query of the side.
    * @return {@code true} if the point is on the query side of the line, {@code false} if the point is on the opposite side or exactly on the line.
    */
   public static boolean isPointOnSideOfLine(Point2d point, Point2d pointOnLine, Vector2d lineDirection, RobotSide side)
   {
      return isPointOnSideOfLine(point.getX(), point.getY(), pointOnLine, lineDirection, side);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left or right side of an infinitely long line.
    * The idea of "side" is determined based on the direction of the line.
    * <p>
    * For instance, given the {@code lineDirection} components x = 0, and y = 1, and the {@code pointOnLine} coordinates x = 0, and y = 0, 
    * a point located on:
    * <ul>
    *    <li> the left side of this line has a negative y coordinate.
    *    <li> the right side of this line has a positive y coordinate.
    * </ul>
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param pointX the x-coordinate of  the query point.
    * @param pointY the y-coordinate of  the query point.
    * @param pointOnLine a point positioned on the infinite line. Not modified.
    * @param lineDirection the direction of the infinite line. Not modified.
    * @param side the query of the side.
    * @return {@code true} if the point is on the query side of the line, {@code false} if the point is on the opposite side or exactly on the line.
    */
   public static boolean isPointOnSideOfLine(double pointX, double pointY, Point2d pointOnLine, Vector2d lineDirection, RobotSide side)
   {
      double pointOnLineX = pointOnLine.getX();
      double pointOnLineY = pointOnLine.getY();
      double lineDirectionX = lineDirection.getX();
      double lineDirectionY = lineDirection.getY();
      return isPointOnSideOfLine(pointX, pointY, pointOnLineX, pointOnLineY, lineDirectionX, lineDirectionY, side);
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left or right side of an infinitely long line.
    * The idea of "side" is determined based on the direction of the line.
    * <p>
    * For instance, given the {@code lineDirection} components x = 0, and y = 1, and the {@code pointOnLine} coordinates x = 0, and y = 0, 
    * a point located on:
    * <ul>
    *    <li> the left side of this line has a negative y coordinate.
    *    <li> the right side of this line has a positive y coordinate.
    * </ul>
    * </p>
    * This method will return false if the point is on the line.
    *
    * @param pointX the x-coordinate of  the query point.
    * @param pointY the y-coordinate of  the query point.
    * @param pointOnLineX the x-coordinate of a point positioned on the infinite line.
    * @param pointOnLineY the y-coordinate of a point positioned on the infinite line.
    * @param lineDirectionX the x-component of the direction of the infinite line.
    * @param lineDirectionY the y-component of the direction of the infinite line.
    * @param side the query of the side.
    * @return {@code true} if the point is on the query side of the line, {@code false} if the point is on the opposite side or exactly on the line.
    */
   public static boolean isPointOnSideOfLine(double pointX, double pointY, double pointOnLineX, double pointOnLineY, double lineDirectionX, double lineDirectionY, RobotSide side)
   {
      double pointToPointX = pointX - pointOnLineX;
      double pointToPointY = pointY - pointOnLineY;
      double crossProduct = lineDirectionX * pointToPointY - pointToPointX * lineDirectionY;
      return side.negateIfRightSide(crossProduct) > 0.0;
   }

   /**
    * Returns a boolean value, stating whether a 2D point is on the left side of a given line.
    * "Left side" is determined based on order of {@code lineStart} and {@code lineEnd}.
    * <p>
    * WARNING: the 3D arguments are projected onto the XY-plane to perform the actual computation in 2D.
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * For instance, given the {@code lineStart} coordinates x = 0, and y = 0, and the {@code lineEnd} coordinates x = 1, y = 0,
    * a point located on the left of this line has a negative y coordinate.
    *<p>
    * This method will return false if the point is on the line.
    * </p>
    * 
    * @param point the projection onto the XY-plane of this point is used as the 2D query point. Not modified.
    * @param firstPointOnLine the projection onto the XY-plane of this point is used as a first point located on the line. Not modified.
    * @param secondPointOnLine the projection onto the XY-plane of this point is used as a second point located on the line. Not modified.
    * @return {@code true} if the 2D projection of the point is on the left side of the 2D projection of the line.
    * {@code false} if the 2D projection of the point is on the right side or exactly on the 2D projection of the line.
    */
   // FIXME this method is confusing and error prone.
   public static boolean isPointOnLeftSideOfLine(FramePoint point, FramePoint firstPointOnLine, FramePoint secondPointOnLine)
   {
      point.checkReferenceFrameMatch(firstPointOnLine);
      point.checkReferenceFrameMatch(secondPointOnLine);
      Point2d lineStartPoint2d = new Point2d(firstPointOnLine.getX(), firstPointOnLine.getY());
      Point2d lineEndPoint2d = new Point2d(secondPointOnLine.getX(), secondPointOnLine.getY());
      Point2d checkPointPoint2d = new Point2d(point.getX(), point.getY());

      return isPointOnLeftSideOfLine(checkPointPoint2d, lineStartPoint2d, lineEndPoint2d);
   }

   /**
    * Returns true only if the point is inside the triangle defined by the vertices a, b, and c.
    * The triangle can be clockwise or counter-clockwise ordered.
    * 
    * @param point the point to check if lying inside the triangle. Not modified.
    * @param a first vertex of the triangle. Not modified.
    * @param b second vertex of the triangle. Not modified.
    * @param c third vertex of the triangle. Not modified.
    * @return {@code true} if the query is exactly inside the triangle. {@code false} if the query point is outside triangle or exactly on an edge of the triangle.
    */
   public static boolean isPointInsideTriangleABC(Point2d point, Point2d a, Point2d b, Point2d c)
   {
      // This makes the assertion working for both clockwise and counter-clockwise ordered vertices.
      RobotSide sideToCheck = isPointOnLeftSideOfLine(b, a, c) ? RobotSide.LEFT : RobotSide.RIGHT;

      if (isPointOnSideOfLine(point, a, b, sideToCheck))
         return false;
      if (isPointOnSideOfLine(point, b, c, sideToCheck))
         return false;
      if (isPointOnSideOfLine(point, c, a, sideToCheck))
         return false;

      return true;
   }

   /**
    * Compute the area of a triangle defined by its three vertices: a, b, and c.
    * No specific ordering of the vertices is required.
    * 
    * @param a first vertex of the triangle. Not modified.
    * @param b second vertex of the triangle. Not modified.
    * @param c third vertex of the triangle. Not modified.
    * @return the are of the triangle.
    */
   public static double computeTriangleArea(Point2d a, Point2d b, Point2d c)
   {
      return Math.abs(0.5 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y)));
   }

   /**
    * Computes the average 2D point from a given collection of 2D points.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param points the collection of 2D points to compute the average from. Not modified.
    * @return the computed average.
    */
   public static Point2d averagePoint2ds(Collection<Point2d> points)
   {
      Point2d totalPoint = new Point2d(0.0, 0.0);

      for (Point2d point : points)
      {
         totalPoint.add(point);
      }

      totalPoint.scale(1.0 / points.size());

      return totalPoint;
   }

   /**
    * Computes the average 3D point from a given collection of 3D points.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param points the collection of 3D points to compute the average from. Not modified.
    * @return the computed average.
    */
   public static Point3d averagePoint3ds(Collection<Point3d> points)
   {
      Point3d totalPoint = new Point3d(0.0, 0.0, 0.0);

      for (Point3d point : points)
      {
         totalPoint.add(point);
      }

      totalPoint.scale(1.0 / points.size());

      return totalPoint;
   }

   /**
    * Returns the average of two 3D points.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param a the first 3D point. Not modified.
    * @param b the second 3D point. Not modified.
    * @return the computed average.
    */
   public static Point3d averagePoints(Point3d a, Point3d b)
   {
      Point3d average = new Point3d(a);
      average.add(b);
      average.scale(0.5);

      return average;
   }

   /**
    * Returns the average of two 3D points.
    *
    * @param a the first 3D point. Not modified.
    * @param b the second 3D point. Not modified.
    * @param avgToPack the point in which the computed average is stored. Modified.
    */
   public static void averagePoints(FramePoint a, FramePoint b, FramePoint avgToPack)
   {
      avgToPack.setIncludingFrame(a);
      avgToPack.add(b);
      avgToPack.scale(0.5);
   }

   /**
    * Computes the orthogonal projection of a 2D point on an infinitely long 2D line defined by a 2D line segment.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two given points on the line are too close, i.e. {@code firstPointOnLine.distanceSquared(secondPointOnLine) < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@code null}.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @return the projection of the point onto the line or {@code null} if the method failed.
    */
   public static Point2d getOrthogonalProjectionOnLine(Point2d testPoint, Point2d firstPointOnLine, Point2d secondPointOnLine)
   {
      Point2d projection = new Point2d();
      boolean success = getOrthogonalProjectionOnLine(testPoint, firstPointOnLine, secondPointOnLine, projection);
      if (!success)
         return null;
      else
         return projection;
   }

   /**
    * Computes the orthogonal projection of a 2D point on an infinitely long 2D line defined by a 2D line segment.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two given points on the line are too close, i.e. {@code firstPointOnLine.distanceSquared(secondPointOnLine) < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param firstPointOnLine a first point located on the line. Not modified.
    * @param secondPointOnLine a second point located on the line. Not modified.
    * @param projectionToPack point in which the projection of the point onto the line is stored. Modified.
    * @return whether the method succeeded or not.
    */
   public static boolean getOrthogonalProjectionOnLine(Point2d testPoint, Point2d firstPointOnLine, Point2d secondPointOnLine, Point2d projectionToPack)
   {
      double pointOnLineX = firstPointOnLine.getX();
      double pointOnLineY = firstPointOnLine.getY();
      double lineDirectionX = secondPointOnLine.getX() - firstPointOnLine.getX();
      double lineDirectionY = secondPointOnLine.getY() - firstPointOnLine.getY();
      return getOrthogonalProjectionOnLine(testPoint, pointOnLineX, pointOnLineY, lineDirectionX, lineDirectionY, projectionToPack);
   }

   /**
    * Computes the orthogonal projection of a 2D point on an infinitely long 2D line defined by a 2D point and a 2D direction.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the given line direction is too small, i.e. {@code lineDirection.lengthSquared() < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@code null}.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param pointOnLine a point located on the line. Not modified.
    * @param lineDirection the direction of the line. Not modified.
    * @return the projection of the point onto the line or {@code null} if the method failed.
    */
   public static Point2d getOrthogonalProjectionOnLine(Point2d testPoint, Point2d pointOnLine, Vector2d lineDirection)
   {
      Point2d projection = new Point2d();
      boolean success = getOrthogonalProjectionOnLine(testPoint, pointOnLine, lineDirection, projection);
      if (!success)
         return null;
      else
         return projection;
   }

   /**
    * Computes the orthogonal projection of a 2D point on an infinitely long 2D line defined by a 2D point and a 2D direction.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the given line direction is too small, i.e. {@code lineDirection.lengthSquared() < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param pointOnLine a point located on the line. Not modified.
    * @param lineDirection the direction of the line. Not modified.
    * @param projectionToPack point in which the projection of the point onto the line is stored. Modified.
    * @return whether the method succeeded or not.
    */
   public static boolean getOrthogonalProjectionOnLine(Point2d testPoint, Point2d pointOnLine, Vector2d lineDirection, Point2d projectionToPack)
   {
      return getOrthogonalProjectionOnLine(testPoint, pointOnLine.getX(), pointOnLine.getY(), lineDirection.getX(), lineDirection.getY(), projectionToPack);
   }

   /**
    * Computes the orthogonal projection of a 2D point on an infinitely long 2D line defined by a 2D point and a 2D direction.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the given line direction is too small, i.e. {@code lineDirection.lengthSquared() < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param pointOnLineX x-coordinate of a point located on the line.
    * @param pointOnLineY y-coordinate of a point located on the line.
    * @param lineDirectionX x-component of the direction of the line.
    * @param lineDirectionY y-component of the direction of the line.
    * @param projectionToPack point in which the projection of the point onto the line is stored. Modified.
    * @return whether the method succeeded or not.
    */
   public static boolean getOrthogonalProjectionOnLine(Point2d testPoint, double pointOnLineX, double pointOnLineY, double lineDirectionX,
                                                       double lineDirectionY, Point2d projectionToPack)
   {
      double directionLengthSquared = lineDirectionX * lineDirectionX + lineDirectionY * lineDirectionY;

      if (directionLengthSquared < Epsilons.ONE_TRILLIONTH)
         return false;

      double dx = testPoint.getX() - pointOnLineX;
      double dy = testPoint.getY() - pointOnLineY;

      double dot = dx * lineDirectionX + dy * lineDirectionY;

      double alpha = dot / directionLengthSquared;

      projectionToPack.setX(pointOnLineX + alpha * lineDirectionX);
      projectionToPack.setY(pointOnLineY + alpha * lineDirectionY);

      return true;
   }

   /**
    * Computes the orthogonal projection of a 2D point on a given 2D line segment defined by its two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of the given line segment is too small,
    *     i.e. {@code lineSegmentStart.distanceSquared(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *      this method fails and returns {@code null}.
    *    <li> the projection can not be outside the line segment.
    *     When the projection on the corresponding line is outside the line segment, the result is the closest of the two end points.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param lineSegmentStart the line segment first end point. Not modified.
    * @param lineSegmentEnd the line segment second end point. Not modified.
    * @return the projection of the point onto the line segment or {@code null} if the method failed.
    */
   public static Point2d getOrthogonalProjectionOnLineSegment(Point2d testPoint, Point2d lineSegmentStart, Point2d lineSegmentEnd)
   {
      Point2d projection = new Point2d();
      boolean success = getOrthogonalProjectionOnLineSegment(testPoint, lineSegmentStart.getX(), lineSegmentStart.getY(), lineSegmentEnd.getX(), lineSegmentEnd.getY(),
                                                  projection);
      if (!success)
         return null;
      else
         return projection;
   }

   /**
    * Computes the orthogonal projection of a 2D point on a given 2D line segment defined by its two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of the given line segment is too small,
    *     i.e. {@code lineSegmentStart.distanceSquared(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *      this method fails and returns {@code false}.
    *    <li> the projection can not be outside the line segment.
    *     When the projection on the corresponding line is outside the line segment, the result is the closest of the two end points.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param lineSegmentStart the line segment first end point. Not modified.
    * @param lineSegmentEnd the line segment second end point. Not modified.
    * @param projectionToPack point in which the projection of the point onto the line segment is stored. Modified.
    * @return whether the method succeeded or not.
    */
   public static boolean getOrthogonalProjectionOnLineSegment(Point2d testPoint, Point2d lineSegmentStart, Point2d lineSegmentEnd, Point2d projectionToPack)
   {
      return getOrthogonalProjectionOnLineSegment(testPoint, lineSegmentStart.getX(), lineSegmentStart.getY(), lineSegmentEnd.getX(), lineSegmentEnd.getY(),
                                                  projectionToPack);
   }

   /**
    * Computes the orthogonal projection of a 2D point on a given 2D line segment defined by its two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of the given line segment is too small,
    *     i.e. {@code lineSegmentStart.distanceSquared(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *      this method fails and returns {@code false}.
    *    <li> the projection can not be outside the line segment.
    *     When the projection on the corresponding line is outside the line segment, the result is the closest of the two end points.
    * </ul>
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param lineSegmentStartX the x-coordinate of the line segment first end point.
    * @param lineSegmentStartY the y-coordinate of the line segment first end point.
    * @param lineSegmentEndX the x-coordinate of the line segment second end point.
    * @param lineSegmentEndY the y-coordinate of the line segment second end point.
    * @param projectionToPack point in which the projection of the point onto the line segment is stored. Modified.
    * @return whether the method succeeded or not.
    */
   public static boolean getOrthogonalProjectionOnLineSegment(Point2d testPoint, double lineSegmentStartX, double lineSegmentStartY, double lineSegmentEndX,
                                                              double lineSegmentEndY, Point2d projectionToPack)
   {
      double percentage = getPercentageAlongLineSegment(testPoint.getX(), testPoint.getY(), lineSegmentStartX, lineSegmentStartY, lineSegmentEndX,
                                                        lineSegmentEndY);
      if (!Double.isFinite(percentage))
         return false;

      percentage = MathTools.clipToMinMax(percentage, 0.0, 1.0);

      projectionToPack.setX((1.0 - percentage) * lineSegmentStartX + percentage * lineSegmentEndX);
      projectionToPack.setY((1.0 - percentage) * lineSegmentStartY + percentage * lineSegmentEndY);
      return true;
   }

   /**
    * This is the same calculation as for {@link #getOrthogonalProjectionOnLineSegment(Point2d, Point2d, Point2d)}: </br>
    * Computes the orthogonal projection of a 2D point on a given 2D line segment defined by its two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of the given line segment is too small,
    *     i.e. {@code lineSegmentStart.distanceSquared(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *      this method fails and returns {@code null}.
    *    <li> the projection can not be outside the line segment.
    *     When the projection on the corresponding line is outside the line segment, the result is the closest of the two end points.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param testPoint the point to compute the projection of. Not modified.
    * @param lineSegmentStart the line segment first end point. Not modified.
    * @param lineSegmentEnd the line segment second end point. Not modified.
    * @return the projection of the point onto the line segment or {@code null} if the method failed.
    */
   public static Point2d getClosestPointToLineSegment(Point2d testPoint, Point2d lineSegmentStart, Point2d lineSegmentEnd)
   {
      return getOrthogonalProjectionOnLineSegment(testPoint, lineSegmentStart, lineSegmentEnd);
   }

   /**
    * Computes a percentage along the line segment representing the location of the projection onto the line segment of the given point.
    * The returned percentage is in ] -&infin;; &infin; [, {@code 0.0} representing {@code lineSegmentStart}, and {@code 1.0} representing {@code lineSegmentEnd}.
    * <p>
    * For example, if the returned percentage is {@code 0.5}, it means that the projection of the given point is located at the middle of the line segment.
    * The coordinates of the projection of the point can be computed from the {@code percentage} as follows:
    * <code>
    * Point2d projection = new Point2d(); </br>
    * projection.interpolate(lineSegmentStart, lineSegmentEnd, percentage); </br>
    * </code>
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of the given line segment is too small, i.e. {@code lineSegmentStart.distanceSquared(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@link Double#NaN}.
    * </ul>
    * </p>
    * 
    * @param point the query. Not modified.
    * @param lineSegmentStart the line segment first end point. Not modified.
    * @param lineSegmentEnd the line segment second end point. Not modified.
    * @return the computed percentage along the line segment representing where the point projection is located.
    */
   public static double getPercentageAlongLineSegment(Point2d point, Point2d lineSegmentStart, Point2d lineSegmentEnd)
   {
      return getPercentageAlongLineSegment(point.getX(), point.getY(), lineSegmentStart.getX(), lineSegmentStart.getY(), lineSegmentEnd.getX(),
                                           lineSegmentEnd.getY());
   }

   /**
    * Computes a percentage along the line segment representing the location of the given point once projected onto the line segment.
    * The returned percentage is in ] -&infin;; &infin; [, {@code 0.0} representing {@code lineSegmentStart}, and {@code 1.0} representing {@code lineSegmentEnd}.
    * <p>
    * For example, if the returned percentage is {@code 0.5}, it means that the projection of the given point is located at the middle of the line segment.
    * The coordinates of the projection of the point can be computed from the {@code percentage} as follows:
    * <code>
    * Point2d projection = new Point2d(); </br>
    * projection.interpolate(lineSegmentStart, lineSegmentEnd, percentage); </br>
    * </code>
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of the given line segment is too small, i.e. {@code lineSegmentStart.distanceSquared(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH}, this method fails and returns {@link Double#NaN}.
    * </ul>
    * </p>
    * 
    * @param pointX the x-coordinate of the query point.
    * @param pointY the y-coordinate of the query point.
    * @param lineSegmentStartX the x-coordinate of the line segment first end point.
    * @param lineSegmentStartY the y-coordinate of the line segment first end point.
    * @param lineSegmentEndX the x-coordinate of the line segment second end point.
    * @param lineSegmentEndY the y-coordinate of the line segment second end point.
    * @return the computed percentage along the line segment representing where the point projection is located.
    */
   public static double getPercentageAlongLineSegment(double pointX, double pointY, double lineSegmentStartX, double lineSegmentStartY, double lineSegmentEndX,
                                                      double lineSegmentEndY)
   {
      double lineSegmentDx = lineSegmentEndX - lineSegmentStartX;
      double lineSegmentDy = lineSegmentEndY - lineSegmentStartY;
      double lengthSquared = lineSegmentDx * lineSegmentDx + lineSegmentDy * lineSegmentDy;

      if (lengthSquared < Epsilons.ONE_TRILLIONTH)
         return Double.NaN;

      double dx = pointX - lineSegmentStartX;
      double dy = pointY - lineSegmentStartY;

      double dot = dx * lineSegmentDx + dy * lineSegmentDy;

      double alpha = dot / lengthSquared;

      return alpha;
   }

   /**
    * Given two 3D infinitely long lines, this methods computes two points P &in; line1 and Q &in; lin2 such that the distance || P - Q || is the minimum distance between the two 3D lines.
    * <a href="http://geomalgorithms.com/a07-_distance.html"> Useful link</a>.
    * 
    * @param pointOnLine1 a 3D point on the first line. Not modified.
    * @param lineDirection1 the 3D direction of the first line. Not modified.
    * @param pointOnLine2 a 3D point on the second line. Not modified.
    * @param lineDirection2 the 3D direction of the second line. Not modified.
    * @param closestPointOnLine1ToPack the 3D coordinates of the point P are packed in this 3D point. Modified.
    * @param closestPointOnLine2ToPack the 3D coordinates of the point Q are packed in this 3D point. Modified.
    * @throws ReferenceFrameMismatchException if the input arguments are not expressed in the same reference frame, except for {@code closestPointOnLine1ToPack} and  {@code closestPointOnLine2ToPack}.
    */
   public static void getClosestPointsForTwoLines(FramePoint pointOnLine1, FrameVector lineDirection1, FramePoint pointOnLine2, FrameVector lineDirection2, FramePoint closestPointOnLine1ToPack,
           FramePoint closestPointOnLine2ToPack)
   {
      pointOnLine1.checkReferenceFrameMatch(lineDirection1);
      pointOnLine2.checkReferenceFrameMatch(lineDirection2);
      pointOnLine1.checkReferenceFrameMatch(pointOnLine2);

      closestPointOnLine1ToPack.setToZero(pointOnLine1.getReferenceFrame());
      closestPointOnLine2ToPack.setToZero(pointOnLine1.getReferenceFrame());

      getClosestPointsForTwoLines(pointOnLine1.getPoint(), lineDirection1.getVector(), pointOnLine2.getPoint(), lineDirection2.getVector(), closestPointOnLine1ToPack.getPoint(), closestPointOnLine2ToPack.getPoint());
   }

   /**
    * Given two 3D infinitely long lines, this methods computes two points P &in; line1 and Q &in; lin2 such that the distance || P - Q || is the minimum distance between the two 3D lines.
    * <a href="http://geomalgorithms.com/a07-_distance.html"> Useful link</a>.
    * 
    * @param pointOnLine1 a 3D point on the first line. Not modified.
    * @param lineDirection1 the 3D direction of the first line. Not modified.
    * @param pointOnLine2 a 3D point on the second line. Not modified.
    * @param lineDirection2 the 3D direction of the second line. Not modified.
    * @param closestPointOnLine1ToPack the 3D coordinates of the point P are packed in this 3D point. Modified.
    * @param closestPointOnLine2ToPack the 3D coordinates of the point Q are packed in this 3D point. Modified.
    */
   public static void getClosestPointsForTwoLines(Point3d pointOnLine1, Vector3d lineDirection1, Point3d pointOnLine2, Vector3d lineDirection2, Point3d closestPointOnLine1ToPack,
           Point3d closestPointOnLine2ToPack)
   {
      // Switching to the notation used in http://geomalgorithms.com/a07-_distance.html.
      // The line1 is defined by (P0, u) and the line2 by (Q0, v).
      // Note: the algorithm is independent from the magnitudes of lineDirection1 and lineDirection2
      Point3d P0 = pointOnLine1;
      Vector3d u = lineDirection1;
      Point3d Q0 = pointOnLine2;
      Vector3d v = lineDirection2;

      Point3d Psc = closestPointOnLine1ToPack;
      Point3d Qtc = closestPointOnLine2ToPack;

      Vector3d w0 = new Vector3d();
      w0.sub(P0, Q0);
      
      double a = u.dot(u);
      double b = u.dot(v);
      double c = v.dot(v);
      double d = u.dot(w0);
      double e = v.dot(w0);

      double delta = a * c - b * b;

      double sc, tc;

      // check to see if the lines are parallel
      if (Math.abs(delta) <= EPSILON)
      {
         /*
          * The lines are parallel, there's an infinite number of pairs,
          * but for one chosen point on one of the lines, there's only one closest point to it on the other line.
          * So let's chose arbitrarily a point on the line1 and calculate the point that is closest to it on the line2.
          */
         sc = 0.0;
         tc = d / b;
      }
      else
      {
         sc = (b * e - c * d) / delta;
         tc = (a * e - b * d) / delta;
      }

      Psc.scaleAdd(sc, u, P0);
      Qtc.scaleAdd(tc, v, Q0);
   }

   /**
    * Computes the coordinates of the intersection between a plane and an infinitely long line.
    * In the case the line is parallel to the plane, this method will return {@code null}.
    * <a href="https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection"> Useful link </a>.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @param pointOnLine a point located on the line. Not modified.
    * @param lineDirection the direction of the line. Not modified.
    * @return the coordinates of the intersection, or {@code null} if the line is parallel to the plane.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same frame.
    */
   public static FramePoint getIntersectionBetweenLineAndPlane(FramePoint pointOnPlane, FrameVector planeNormal, FramePoint pointOnLine, FrameVector lineDirection)
   {
      pointOnPlane.checkReferenceFrameMatch(planeNormal);
      pointOnLine.checkReferenceFrameMatch(lineDirection);
      pointOnPlane.checkReferenceFrameMatch(pointOnLine);

      Point3d intersection = getIntersectionBetweenLineAndPlane(pointOnPlane.getPoint(), planeNormal.getVector(), pointOnLine.getPoint(), lineDirection.getVector());

      if (intersection == null)
         return null;
      else
         return new FramePoint(pointOnPlane.getReferenceFrame(), intersection);
   }

   /**
    * Computes the coordinates of the intersection between a plane and an infinitely long line.
    * In the case the line is parallel to the plane, this method will return {@code null}.
    * <a href="https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection"> Useful link </a>.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @param pointOnLine a point located on the line. Not modified.
    * @param lineDirection the direction of the line. Not modified.
    * @return the coordinates of the intersection, or {@code null} if the line is parallel to the plane.
    */
   public static Point3d getIntersectionBetweenLineAndPlane(Point3d pointOnPlane, Vector3d planeNormal, Point3d pointOnLine, Vector3d lineDirection)
   {
      // Switching to the notation used in https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection
      // Note: the algorithm is independent from the magnitudes of planeNormal and lineDirection
      Point3d p0 = pointOnPlane;
      Vector3d n = planeNormal;
      Point3d l0 = pointOnLine;
      Vector3d l = lineDirection;

      // Let's compute the value of the coefficient d = ( (p0 - l0).n ) / ( l.n )
      double d, numerator, denominator;
      numerator = (p0.getX() - l0.getX()) * n.getX();
      numerator += (p0.getY() - l0.getY()) * n.getY();
      numerator += (p0.getZ() - l0.getZ()) * n.getZ();
      denominator = l.dot(n);

      // Check if the line is parallel to the plane
      if (Math.abs(denominator) < EPSILON)
      {
         return null;
      }
      else
      {
         d = numerator / denominator;
         
         Point3d intersection = new Point3d();
         intersection.scaleAdd(d, l, l0);
         return intersection;
      }
   }

   /**
    * Computes the coordinates of the intersection between a plane and a finite length line segment.
    * <p>
    * This method returns null for the following cases:
    * <ul>
    *    <li> the line segment is parallel to the plane,
    *    <li> the line segment end points are on one side of the plane,
    *    <li> the line segment length is equal to zero ({@code lineSegmentStart == lineSegmentEnd}),
    *    <li> one of the line segment end points lies on the plane.
    * </ul>
    * </p>
    * Once the existence of an intersection is verified,
    * this method calls {@link #getIntersectionBetweenLineAndPlane(Point3d, Vector3d, Point3d, Vector3d)}
    * to perform the actual computation.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @param lineSegmentStart first end point of the line segment. Not modified.
    * @param lineSegmentEnd second end point of the line segment. Not modified.
    * @return the intersection, or {@code null} if there is no intersection.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame.
    */
   public static FramePoint getIntersectionBetweenLineSegmentAndPlane(FramePoint pointOnPlane, FrameVector planeNormal, FramePoint lineSegmentStart,
                                                                      FramePoint lineSegmentEnd)
   {
      pointOnPlane.checkReferenceFrameMatch(planeNormal);
      lineSegmentStart.checkReferenceFrameMatch(lineSegmentEnd);
      pointOnPlane.checkReferenceFrameMatch(lineSegmentStart);

      Point3d intersection = getIntersectionBetweenLineSegmentAndPlane(pointOnPlane.getPoint(), planeNormal.getVector(), lineSegmentStart.getPoint(),
                                                                       lineSegmentEnd.getPoint());

      if (intersection == null)
         return null;
      else
         return new FramePoint(pointOnPlane.getReferenceFrame(), intersection);
   }

   /**
    * Computes the coordinates of the intersection between a plane and a finite length line segment.
    * <p>
    * This method returns null for the following cases:
    * <ul>
    *    <li> the line segment is parallel to the plane,
    *    <li> the line segment end points are on one side of the plane,
    *    <li> the line segment length is equal to zero ({@code lineSegmentStart == lineSegmentEnd}),
    *    <li> one of the line segment end points lies on the plane.
    * </ul>
    * </p>
    * Once the existence of an intersection is verified,
    * this method calls {@link #getIntersectionBetweenLineAndPlane(Point3d, Vector3d, Point3d, Vector3d)}
    * to perform the actual computation.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @param lineSegmentStart first end point of the line segment. Not modified.
    * @param lineSegmentEnd second end point of the line segment. Not modified.
    * @return the intersection, or {@code null} if there is no intersection.
    */
   public static Point3d getIntersectionBetweenLineSegmentAndPlane(Point3d pointOnPlane, Vector3d planeNormal, Point3d lineSegmentStart, Point3d lineSegmentEnd)
   {
      if (isLineSegmentIntersectingPlane(pointOnPlane, planeNormal, lineSegmentStart, lineSegmentEnd))
      { // Since an intersection exists, it is now the same as computing the intersection line-plane
         Vector3d lineDirection = new Vector3d();
         lineDirection.sub(lineSegmentEnd, lineSegmentStart);
         return getIntersectionBetweenLineAndPlane(pointOnPlane, planeNormal, lineSegmentStart, lineDirection);
      }
      else
      {
         return null;
      }
   }

   /**
    * Test if a given line segment intersects a given plane.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> the line segment end points are equal, this method returns false whether the end points are on the plane or not.
    *    <li> one of the line segment end points is exactly on the plane, this method returns false.
    * </ul>
    * </p>
    * 
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @param lineSegmentStart first end point of the line segment. Not modified.
    * @param lineSegmentEnd second end point of the line segment. Not modified.
    * @return {@code true} if an intersection line segment - plane exists, {@code false} otherwise.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame.
    */
   public static boolean isLineSegmentIntersectingPlane(FramePoint pointOnPlane, FrameVector planeNormal, FramePoint lineSegmentStart, FramePoint lineSegmentEnd)
   {
      pointOnPlane.checkReferenceFrameMatch(planeNormal);
      lineSegmentStart.checkReferenceFrameMatch(lineSegmentEnd);
      pointOnPlane.checkReferenceFrameMatch(lineSegmentStart);
      return isLineSegmentIntersectingPlane(pointOnPlane.getPoint(), planeNormal.getVector(), lineSegmentStart.getPoint(), lineSegmentEnd.getPoint());
   }

   /**
    * Test if a given line segment intersects a given plane.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> the line segment end points are equal, this method returns false whether the end points are on the plane or not.
    *    <li> one of the line segment end points is exactly on the plane, this method returns false.
    * </ul>
    * </p>
    * 
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @param lineSegmentStart first end point of the line segment. Not modified.
    * @param lineSegmentEnd second end point of the line segment. Not modified.
    * @return {@code true} if an intersection line segment - plane exists, {@code false} otherwise.
    */
   public static boolean isLineSegmentIntersectingPlane(Point3d pointOnPlane, Vector3d planeNormal, Point3d lineSegmentStart, Point3d lineSegmentEnd)
   {
      double d, ansStart, ansEnd;

      d = -planeNormal.getX() * pointOnPlane.getX();
      d -= planeNormal.getY() * pointOnPlane.getY();
      d -= planeNormal.getZ() * pointOnPlane.getZ();

      ansStart = planeNormal.getX() * lineSegmentStart.getX();
      ansStart += planeNormal.getY() * lineSegmentStart.getY();
      ansStart += planeNormal.getZ() * lineSegmentStart.getZ();
      ansStart += d;

      ansEnd = planeNormal.getX() * lineSegmentEnd.getX();
      ansEnd += planeNormal.getY() * lineSegmentEnd.getY();
      ansEnd += planeNormal.getZ() * lineSegmentEnd.getZ();
      ansEnd += d;

      return ansStart * ansEnd < 0.0;
   }

   /**
    * Computes the minimum distance between a given point and a plane.
    * 
    * @param point the 3D query. Not modified.
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @return the distance between the point and the plane.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame.
    */
   public static double distanceFromPointToPlane(FramePoint point, FramePoint pointOnPlane, FrameVector planeNormal)
   {
      point.checkReferenceFrameMatch(pointOnPlane);
      point.checkReferenceFrameMatch(planeNormal);

      return distanceFromPointToPlane(point.getPoint(), pointOnPlane.getPoint(), planeNormal.getVector());
   }

   /**
    * Computes the minimum distance between a given point and a plane.
    * 
    * @param point the 3D query. Not modified.
    * @param pointOnPlane a point located on the plane. Not modified.
    * @param planeNormal the normal of the plane. Not modified.
    * @return the distance between the point and the plane.
    */
   public static double distanceFromPointToPlane(Point3d point, Point3d pointOnPlane, Vector3d planeNormal)
   {
      double d = -planeNormal.getX() * pointOnPlane.getX() - planeNormal.getY() * pointOnPlane.getY() - planeNormal.getZ() * pointOnPlane.getZ();

      double numerator = planeNormal.getX() * point.getX() + planeNormal.getY() * point.getY() + planeNormal.getZ() * point.getZ() + d;
      double denominator = planeNormal.length();

      return Math.abs(numerator) / denominator;
   }

   /**
    * Test if two line segments intersect each other.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the two line segments are parallel but not collinear, this method returns false.
    *    <li> When the two line segments are collinear,
    *     this methods returns true only if the two line segments overlap or have at least one common end point.
    *    <li> When the two line segments have a common end point, this method returns true.
    * </ul>
    * </p>
    * 
    * @param lineSegmentStart1 first end point of the first line segment. Not modified.
    * @param lineSegmentEnd1 second end point of the first line segment. Not modified.
    * @param lineSegmentStart1 first end point of the second line segment. Not modified.
    * @param lineSegmentEnd1 second end point of the second line segment. Not modified.
    * @return {@code true} if the two line segments intersect, {@code false} otherwise.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame.
    */
   public static boolean doLineSegmentsIntersect(FramePoint2d lineSegmentStart1, FramePoint2d lineSegmentEnd1, FramePoint2d lineSegmentStart2, FramePoint2d lineSegmentEnd2)
   {
      lineSegmentStart1.checkReferenceFrameMatch(lineSegmentEnd1);
      lineSegmentStart2.checkReferenceFrameMatch(lineSegmentEnd2);
      lineSegmentStart1.checkReferenceFrameMatch(lineSegmentStart2);
      return doLineSegmentsIntersect(lineSegmentStart1.getPoint(), lineSegmentEnd1.getPoint(), lineSegmentStart2.getPoint(), lineSegmentEnd2.getPoint());
   }

   /**
    * Test if two line segments intersect each other.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the two line segments are parallel but not collinear, this method returns false.
    *    <li> When the two line segments are collinear,
    *     this methods returns true only if the two line segments overlap or have at least one common end point.
    *    <li> When the two line segments have a common end point, this method returns true.
    * </ul>
    * </p>
    * 
    * @param lineSegmentStart1 first end point of the first line segment. Not modified.
    * @param lineSegmentEnd1 second end point of the first line segment. Not modified.
    * @param lineSegmentStart1 first end point of the second line segment. Not modified.
    * @param lineSegmentEnd1 second end point of the second line segment. Not modified.
    * @return {@code true} if the two line segments intersect, {@code false} otherwise.
    */
   public static boolean doLineSegmentsIntersect(Point2d lineSegmentStart1, Point2d lineSegmentEnd1, Point2d lineSegmentStart2, Point2d lineSegmentEnd2)
   {
      return doLineSegmentsIntersect(lineSegmentStart1.getX(), lineSegmentStart1.getY(), lineSegmentEnd1.getX(), lineSegmentEnd1.getY(),
                                     lineSegmentStart2.getX(), lineSegmentStart2.getY(), lineSegmentEnd2.getX(), lineSegmentEnd2.getY());
   }

   /**
    * Test if two line segments intersect each other.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the two line segments are parallel but not collinear, this method returns false.
    *    <li> When the two line segments are collinear,
    *     this methods returns true only if the two line segments overlap or have at least one common end point.
    *    <li> When the two line segments have a common end point, this method returns true.
    * </ul>
    * </p>
    * 
    * @param lineSegmentStart1x x-coordinate of the first end point of the first line segment.
    * @param lineSegmentStart1y y-coordinate of the first end point of the first line segment.
    * @param lineSegmentEnd1x x-coordinate of the second end point of the first line segment.
    * @param lineSegmentEnd1y y-coordinate of the second end point of the first line segment.
    * @param lineSegmentStart2x x-coordinate of the first end point of the second line segment.
    * @param lineSegmentStart2y y-coordinate of the first end point of the second line segment.
    * @param lineSegmentEnd2x x-coordinate of the second end point of the second line segment.
    * @param lineSegmentEnd2y y-coordinate of the second end point of the second line segment.
    * @return {@code true} if the two line segments intersect, {@code false} otherwise.
    */
   public static boolean doLineSegmentsIntersect(double lineSegmentStart1x, double lineSegmentStart1y, double lineSegmentEnd1x,
                                                 double lineSegmentEnd1y, double lineSegmentStart2x, double lineSegmentStart2y,
                                                 double lineSegmentEnd2x, double lineSegmentEnd2y)
   {
      double eps = Epsilons.ONE_TRILLIONTH;
      double r1numerator, r1denominator, r2numerator, r2denominator;

      double deltax1 = lineSegmentEnd1x - lineSegmentStart1x;
      double deltay1 = lineSegmentEnd1y - lineSegmentStart1y;

      double deltax2 = lineSegmentEnd2x - lineSegmentStart2x;
      double deltay2 = lineSegmentEnd2y - lineSegmentStart2y;

      double startDx = lineSegmentStart1x - lineSegmentStart2x;
      double startDy = lineSegmentStart1y - lineSegmentStart2y;

      r1numerator = deltax2 * startDy - deltay2 * startDx;
      r1denominator = deltay2 * deltax1 - deltax2 * deltay1;

      r2numerator = deltax1 * startDy - deltay1 * startDx;
      r2denominator = r1denominator;

      // denominator == 0 => the line segments are parallel.
      if (Math.abs(r1denominator) < eps)
      {
         // If both numerators and the denominator are zero, the lines are collinear.
         // We must project the lines onto the X- or Y-axis check if the segments overlap.
         if (Math.abs(r1numerator) < eps && Math.abs(r2numerator) < eps)
         {
            double ls1, le1, ls2, le2;
            if (Math.abs(lineSegmentStart1x - lineSegmentEnd1x) > eps)
            {
               ls1 = lineSegmentStart1x;
               le1 = lineSegmentEnd1x;
               ls2 = lineSegmentStart2x;
               le2 = lineSegmentEnd2x;
            }
            else
            {
               ls1 = lineSegmentStart1y;
               le1 = lineSegmentEnd1y;
               ls2 = lineSegmentStart2y;
               le2 = lineSegmentEnd2y;
            }

            // If both first points are less than both second points, the line
            // segments do not intersect.
            if (((ls1 < ls2) && (le1 < ls2)) && ((ls1 < le2) && (le1 < le2)))
               return false;

            // If both first points are greater than both second points, the line
            // segments do not intersect.
            if (((ls1 > ls2) && (le1 > ls2)) && ((ls1 > le2) && (le1 > le2)))
               return false;

            // Otherwise, the line segments must overlap. So we return true.
            return true;
         }
         // The line segments are parallel but are not collinear, they do not intersect
         else
         {
            return false;
         }
      }

      double r1 = r1numerator / r1denominator;
      double r2 = r2numerator / r2denominator;

      // If both r1 and r2 are between zero and one, the line segments intersect.
      return (0.0 - eps < r1) && (r1 < 1.0 + eps) && (0.0 - eps < r2) && (r2 < 1.0 + eps);
   }

   /**
    * Computes the intersection between two infinitely long 2D lines each defined by two 2D points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two lines are parallel but not collinear, the two lines do not intersect and this method returns null.
    *    <li> if the two lines are collinear, the two lines are assumed to be intersecting at {@code pointOnLine1}.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param firstPointOnLine1 a first point located on the first line. Not modified.
    * @param secondPointOnLine1 a second point located on the first line. Not modified.
    * @param firstPointOnLine2 a first point located on the second line. Not modified.
    * @param secondPointOnLine2 a second point located on the second line. Not modified.
    * @param intersectionToPack 2D point in which the result is stored. Modified.
    * @return the 2D point of intersection if the two lines intersect, {@code null} otherwise.
    */
   public static Point2d getIntersectionBetweenTwoLines(Point2d firstPointOnLine1, Point2d secondPointOnLine1, Point2d firstPointOnLine2, Point2d secondPointOnLine2)
   {
      Point2d intersection = new Point2d();

      double pointOnLine1x = firstPointOnLine1.getX();
      double pointOnLine1y = firstPointOnLine1.getY();
      double lineDirection1x = secondPointOnLine1.getX() - firstPointOnLine1.getX();
      double lineDirection1y = secondPointOnLine1.getY() - firstPointOnLine1.getY();
      double pointOnLine2x = firstPointOnLine2.getX();
      double pointOnLine2y = firstPointOnLine2.getY();
      double lineDirection2x = secondPointOnLine2.getX() - firstPointOnLine2.getX();
      double lineDirection2y = secondPointOnLine2.getY() - firstPointOnLine2.getY();
      boolean success = getIntersectionBetweenTwoLines(pointOnLine1x, pointOnLine1y, lineDirection1x, lineDirection1y, pointOnLine2x, pointOnLine2y, lineDirection2x, lineDirection2y, intersection);

      if (!success)
         return null;
      else
         return intersection;
   }

   /**
    * Computes the intersection between two infinitely long 2D lines each defined by a 2D point and a 2D direction.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two lines are parallel but not collinear, the two lines do not intersect and this method returns null.
    *    <li> if the two lines are collinear, the two lines are assumed to be intersecting at {@code pointOnLine1}.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param pointOnLine1 point located on the first line. Not modified.
    * @param lineDirection1 the first line direction. Not modified.
    * @param pointOnLine2x point located on the second line. Not modified.
    * @param lineDirection2x the second line direction. Not modified.
    * @param intersectionToPack 2D point in which the result is stored. Modified.
    * @return the 2D point of intersection if the two lines intersect, {@code null} otherwise.
    */
   public static Point2d getIntersectionBetweenTwoLines(Point2d pointOnLine1, Vector2d lineDirection1, Point2d pointOnLine2, Vector2d lineDirection2)
   {
      Point2d intersection = new Point2d();
      boolean success = getIntersectionBetweenTwoLines(pointOnLine1, lineDirection1, pointOnLine2, lineDirection2, intersection);
      if (success)
         return intersection;
      else
         return null;
   }

   /**
    * Computes the intersection between two infinitely long 2D lines each defined by a 2D point and a 2D direction.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two lines are parallel but not collinear, the two lines do not intersect.
    *    <li> if the two lines are collinear, the two lines are assumed to be intersecting at {@code pointOnLine1}.
    * </ul>
    * </p>
    * 
    * @param pointOnLine1 point located on the first line. Not modified.
    * @param lineDirection1 the first line direction. Not modified.
    * @param pointOnLine2x point located on the second line. Not modified.
    * @param lineDirection2x the second line direction. Not modified.
    * @param intersectionToPack 2D point in which the result is stored. Modified.
    * @return {@code true} if the two lines intersect, {@code false} otherwise.
    */
   public static boolean getIntersectionBetweenTwoLines(Point2d pointOnLine1, Vector2d lineDirection1, Point2d pointOnLine2, Vector2d lineDirection2, Point2d intersectionToPack)
   {
      return getIntersectionBetweenTwoLines(pointOnLine1.getX(), pointOnLine1.getY(), lineDirection1.getX(), lineDirection1.getY(), pointOnLine2.getX(),
                                            pointOnLine2.getY(), lineDirection2.getX(), lineDirection2.getY(), intersectionToPack);
   }

   private static final ThreadLocal<Point2d> tempIntersection = new ThreadLocal<Point2d>()
   {
      @Override
      public Point2d initialValue()
      {
         return new Point2d();
      }
   };

   /**
    * Computes the intersection between two infinitely long 2D lines each defined by two 2D points.
    * <p>
    * WARNING: the actual computation only uses the x and y components of each argument.
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two lines are parallel but not collinear, the two lines do not intersect.
    *    <li> if the two lines are collinear, the two lines are assumed to be intersecting at {@code firstPointOnLine1}.
    * </ul>
    * </p>
    * 
    * @param intersectionToPack the result is stored in the x and y components of this 3D point. Modified.
    * @param firstPointOnLine1 the x and y coordinates are used to define a first 2D point on the first line. Not modified.
    * @param secondPointOnLine1 the x and y coordinates are used to define a second 2D point on the first line. Not modified.
    * @param firstPointOnLine2 the x and y coordinates are used to define a first 2D point on the second line. Not modified.
    * @param secondPointOnLine2 the x and y coordinates are used to define a second 2D point on the second line. Not modified.
    * @return {@code true} if the two lines intersect, {@code false} otherwise.
    */
   // FIXME This method is too confusing and error prone.
   // FIXME It also needs to verify the reference frame of the arguments.
   // FIXME change method signature to have the intersectionToPack at the end.
   public static boolean getIntersectionBetweenTwoLines2d(FramePoint intersectionToPack, FramePoint firstPointOnLine1, FramePoint secondPointOnLine1,
                                                          FramePoint firstPointOnLine2, FramePoint secondPointOnLine2)
   {
      double pointOnLine1x = firstPointOnLine1.getX();
      double pointOnLine1y = firstPointOnLine1.getY();
      double lineDirection1x = secondPointOnLine1.getX() - firstPointOnLine1.getX();
      double lineDirection1y = secondPointOnLine1.getY() - firstPointOnLine1.getY();
      double pointOnLine2x = firstPointOnLine2.getX();
      double pointOnLine2y = firstPointOnLine2.getY();
      double lineDirection2x = secondPointOnLine2.getX() - firstPointOnLine2.getX();
      double lineDirection2y = secondPointOnLine2.getY() - firstPointOnLine2.getY();

      boolean success = getIntersectionBetweenTwoLines(pointOnLine1x, pointOnLine1y, lineDirection1x, lineDirection1y, pointOnLine2x, pointOnLine2y,
                                                       lineDirection2x, lineDirection2y, tempIntersection.get());

      if (!success)
         intersectionToPack.setToNaN();
      else
         intersectionToPack.set(tempIntersection.get().getX(), tempIntersection.get().getY(), intersectionToPack.getZ());
      return success;
   }

   /**
    * Computes the intersection between two infinitely long 2D lines each defined by a 2D point and a 2D direction.
    * <p>
    * WARNING: the actual computation only uses the x and y components of each argument.
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two lines are parallel but not collinear, the two lines do not intersect.
    *    <li> if the two lines are collinear, the two lines are assumed to be intersecting at {@code pointOnLine1}.
    * </ul>
    * </p>
    * 
    * @param intersectionToPack the result is stored in the x and y components of this 3D point. Modified.
    * @param pointOnLine1 the x and y coordinates are used to define a 2D point on the first line. Not modified.
    * @param lineDirection1 the x and y components are used to define the 2D direction of the first line. Not modified.
    * @param pointOnLine2 the x and y coordinates are used to define a 2D point on the second line. Not modified.
    * @param lineDirection2 the x and y components are used to define the 2D direction of the second line. Not modified.
    * @return {@code true} if the two lines intersect, {@code false} otherwise.
    */
   // FIXME This method is too confusing and error prone.
   // FIXME It also needs to verify the reference frame of the arguments.
   // FIXME change method signature to have the intersectionToPack at the end.
   public static boolean getIntersectionBetweenTwoLines2d(FramePoint intersectionToPack, FramePoint pointOnLine1, FrameVector lineDirection1,
                                                          FramePoint pointOnLine2, FrameVector lineDirection2)
   {
      double pointOnLine1x = pointOnLine1.getX();
      double pointOnLine1y = pointOnLine1.getY();
      double lineDirection1x = lineDirection1.getX();
      double lineDirection1y = lineDirection1.getY();
      double pointOnLine2x = pointOnLine2.getX();
      double pointOnLine2y = pointOnLine2.getY();
      double lineDirection2x = lineDirection2.getX();
      double lineDirection2y = lineDirection2.getY();

      boolean success = getIntersectionBetweenTwoLines(pointOnLine1x, pointOnLine1y, lineDirection1x, lineDirection1y, pointOnLine2x, pointOnLine2y,
                                                       lineDirection2x, lineDirection2y, tempIntersection.get());

      if (!success)
         intersectionToPack.setToNaN();
      else
         intersectionToPack.set(tempIntersection.get().getX(), tempIntersection.get().getY(), intersectionToPack.getZ());
      return success;
   }

   /**
    * Computes the intersection between two infinitely long 2D lines each defined by a 2D point and a 2D direction.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the two lines are parallel but not collinear, the two lines do not intersect.
    *    <li> if the two lines are collinear, the two lines are assumed to be intersecting at {@code pointOnLine1}.
    * </ul>
    * </p>
    * 
    * @param pointOnLine1x x-coordinate of a point located on the first line.
    * @param pointOnLine1y y-coordinate of a point located on the first line.
    * @param lineDirection1x x-component of the first line direction.
    * @param lineDirection1y y-component of the first line direction.
    * @param pointOnLine2x x-coordinate of a point located on the second line.
    * @param pointOnLine2y y-coordinate of a point located on the second line.
    * @param lineDirection2x x-component of the second line direction.
    * @param lineDirection2y y-component of the second line direction.
    * @param intersectionToPack 2D point in which the result is stored. Modified.
    * @return {@code true} if the two lines intersect, {@code false} otherwise.
    */
   public static boolean getIntersectionBetweenTwoLines(double pointOnLine1x, double pointOnLine1y, double lineDirection1x, double lineDirection1y,
                                                        double pointOnLine2x, double pointOnLine2y, double lineDirection2x, double lineDirection2y,
                                                        Point2d intersectionToPack)
   {
      //      We solve for x the problem of the form: A * x = b
      //            A      *     x     =      b
      //      / lineDirection2x -lineDirection1x \   / alpha \   / pointOnLine2x - pointOnLine1x \
      //      |                                  | * |       | = |                               |
      //      \ lineDirection2y -lineDirection1y /   \ beta  /   \ pointOnLine2y - pointOnLine1y /
      // Here, only alpha or beta is needed.

      double determinant = -lineDirection1x * lineDirection2y + lineDirection1y * lineDirection2x;

      double dx = pointOnLine2x - pointOnLine1x;
      double dy = pointOnLine2y - pointOnLine1y;

      double epsilon = 1.0E-12;
      if (Math.abs(determinant) < epsilon)
      { // The lines are parallel
         // Check if they are collinear
         double cross = dx * lineDirection1y - dy * lineDirection1x;
         if (Math.abs(cross) < epsilon)
         {
            /*
             *  The two lines are collinear.
             *  There's an infinite number of intersection.
             *  Let's just set the result to pointOnLine1.
             */
            intersectionToPack.set(pointOnLine1x, pointOnLine1y);
            return true;
         }
         else
         {
            return false;
         }
      }
      else
      {
         double oneOverDeterminant = 1.0 / determinant;
         double AInverse00 = oneOverDeterminant * -lineDirection2y;
         double AInverse01 = oneOverDeterminant * lineDirection2x;

         double alpha = AInverse00 * dx + AInverse01 * dy;

         intersectionToPack.setX(pointOnLine1x + alpha * lineDirection1x);
         intersectionToPack.setY(pointOnLine1y + alpha * lineDirection1y);
         return true;
      }
   }

   /**
    * Computes the intersection between two 2D line segments each defined by their two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the two line segments are parallel but not collinear, the two line segments do not intersect, this method returns {@code null}.
    *    <li> When the two line segments are collinear, if the two line segments do not overlap do not have at least one common end point, this method returns {@code null}.
    *    <li> When the two line segments have a common end point, this method returns the common end point as the intersection.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param lineSegmentStart1 the first end point of the first line segment. Not modified.
    * @param lineSegmentEnd1 the second end point of the first line segment. Not modified.
    * @param lineSegmentStart2 the first end point of the second line segment. Not modified.
    * @param lineSegmentEnd2 the second end point of the second line segment. Not modified.
    * @return the intersection point if it exists, {@code null} otherwise.
    */
   public static Point2d getIntersectionBetweenTwoLineSegments(Point2d lineSegmentStart1, Point2d lineSegmentEnd1, Point2d lineSegmentStart2,
                                                               Point2d lineSegmentEnd2)
   {
      Point2d intersection = new Point2d();
      boolean success = getIntersectionBetweenTwoLineSegments(lineSegmentStart1, lineSegmentEnd1, lineSegmentStart2, lineSegmentEnd2, intersection);
      if (!success)
         return null;
      else
         return intersection;
   }

   /**
    * Computes the intersection between two 2D line segments each defined by their two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the two line segments are parallel but not collinear, the two line segments do not intersect.
    *    <li> When the two line segments are collinear, this methods returns true only if the two line segments overlap or have at least one common end point.
    *    <li> When the two line segments have a common end point, this method returns true.
    * </ul>
    * </p>
    * 
    * @param lineSegmentStart1 the first end point of the first line segment. Not modified.
    * @param lineSegmentEnd1 the second end point of the first line segment. Not modified.
    * @param lineSegmentStart2 the first end point of the second line segment. Not modified.
    * @param lineSegmentEnd2 the second end point of the second line segment. Not modified.
    * @param intersectionToPack the 2D point in which the result is stored. Modified.
    * @return {@code true} if the two line segments intersect, {@code false} otherwise.
    */
   public static boolean getIntersectionBetweenTwoLineSegments(Point2d lineSegmentStart1, Point2d lineSegmentEnd1, Point2d lineSegmentStart2,
                                                               Point2d lineSegmentEnd2, Point2d intersectionToPack)
   {
      return getIntersectionBetweenTwoLineSegments(lineSegmentStart1.getX(), lineSegmentStart1.getY(), lineSegmentEnd1.getX(), lineSegmentEnd1.getY(),
                                                   lineSegmentStart2.getX(), lineSegmentStart2.getY(), lineSegmentEnd2.getX(), lineSegmentEnd2.getY(),
                                                   intersectionToPack);
   }

   /**
    * Computes the intersection between two 2D line segments each defined by their two 2D end points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the two line segments are parallel but not collinear, the two line segments do not intersect.
    *    <li> When the two line segments are collinear, this methods returns true only if the two line segments overlap or have at least one common end point.
    *    <li> When the two line segments have a common end point, this method returns true.
    * </ul>
    * 
    * @param lineSegmentStart1x x-coordinate of the first end point of the first line segment.
    * @param lineSegmentStart1y y-coordinate of the first end point of the first line segment.
    * @param lineSegmentEnd1x x-coordinate of the second end point of the first line segment.
    * @param lineSegmentEnd1y y-coordinate of the second end point of the first line segment.
    * @param lineSegmentStart2x x-coordinate of the first end point of the second line segment.
    * @param lineSegmentStart2y y-coordinate of the first end point of the second line segment.
    * @param lineSegmentEnd2x x-coordinate of the second end point of the second line segment.
    * @param lineSegmentEnd2y y-coordinate of the second end point of the second line segment.
    * @param intersectionToPack the 2D point in which the result is stored. Modified.
    * @return {@code true} if the two line segments intersect, {@code false} otherwise.
    */
   public static boolean getIntersectionBetweenTwoLineSegments(double lineSegmentStart1x, double lineSegmentStart1y, double lineSegmentEnd1x,
                                                               double lineSegmentEnd1y, double lineSegmentStart2x, double lineSegmentStart2y,
                                                               double lineSegmentEnd2x, double lineSegmentEnd2y, Point2d intersectionToPack)
   {
      if (doLineSegmentsIntersect(lineSegmentStart1x, lineSegmentStart1y, lineSegmentEnd1x, lineSegmentEnd1y, lineSegmentStart2x, lineSegmentStart2y,
                                  lineSegmentEnd2x, lineSegmentEnd2y))
      {
         double lineDirection1x = lineSegmentEnd1x - lineSegmentStart1x;
         double lineDirection1y = lineSegmentEnd1y - lineSegmentStart1y;
         double lineDirection2x = lineSegmentEnd2x - lineSegmentStart2x;
         double lineDirection2y = lineSegmentEnd2y - lineSegmentStart2y;

         if (Math.abs(-lineDirection1x * lineDirection2y + lineDirection1y * lineDirection2x) > Epsilons.ONE_TRILLIONTH)
         { // The line segments are not parallel and are intersecting, same as finding the intersection of two lines.
            double pointOnLine1x = lineSegmentStart1x;
            double pointOnLine1y = lineSegmentStart1y;
            double pointOnLine2x = lineSegmentStart2x;
            double pointOnLine2y = lineSegmentStart2y;
            return getIntersectionBetweenTwoLines(pointOnLine1x, pointOnLine1y, lineDirection1x, lineDirection1y, pointOnLine2x, pointOnLine2y, lineDirection2x,
                                                  lineDirection2y, intersectionToPack);
         }
         else
         { // The line segments are parallel and intersecting, they must be overlapping.
            // Let's first check for a common endpoint
            double epsilon = Epsilons.ONE_TRILLIONTH;

            // Let's find the first end point that is inside the other line segment and return it.
            double lineSegment1LengthSquare = lineDirection1x * lineDirection1x + lineDirection1y * lineDirection1y;
            double dx, dy, dot;

            // Check if lineSegmentStart2 is inside lineSegment1
            dx = lineSegmentStart2x - lineSegmentStart1x;
            dy = lineSegmentStart2y - lineSegmentStart1y;
            dot = dx * lineDirection1x + dy * lineDirection1y;

            if (0.0 - epsilon < dot && dot < lineSegment1LengthSquare + epsilon)
            {
               intersectionToPack.set(lineSegmentStart2x, lineSegmentStart2y);
               return true;
            }

            // Check if lineSegmentEnd2 is inside lineSegment1
            dx = lineSegmentEnd2x - lineSegmentStart1x;
            dy = lineSegmentEnd2y - lineSegmentStart1y;
            dot = dx * lineDirection1x + dy * lineDirection1y;

            if (0.0 - epsilon < dot && dot < lineSegment1LengthSquare + epsilon)
            {
               intersectionToPack.set(lineSegmentEnd2x, lineSegmentEnd2y);
               return true;
            }

            double lineSegment2LengthSquare = lineDirection2x * lineDirection2x + lineDirection2y * lineDirection2y;

            // Check if lineSegmentStart1 is inside lineSegment2
            dx = lineSegmentStart1x - lineSegmentStart2x;
            dy = lineSegmentStart1y - lineSegmentStart2y;
            dot = dx * lineDirection2x + dy * lineDirection2y;

            if (0.0 - epsilon < dot && dot < lineSegment2LengthSquare + epsilon)
            {
               intersectionToPack.set(lineSegmentStart1x, lineSegmentStart1y);
               return true;
            }

            // Check if lineSegmentEnd1 is inside lineSegment2
            dx = lineSegmentEnd1x - lineSegmentStart2x;
            dy = lineSegmentEnd1y - lineSegmentStart2y;
            dot = dx * lineDirection2x + dy * lineDirection2y;

            if (0.0 - epsilon < dot && dot < lineSegment2LengthSquare + epsilon)
            {
               intersectionToPack.set(lineSegmentEnd1x, lineSegmentEnd1y);
               return true;
            }

            // There is some inconsistency between doLineSegmentsIntersect and this method, crashing.
            throw new RuntimeException("Unexpected state.");
         }
      }
      else
      {
         return false;
      }
   }

   /**
    * Computes the intersection between an infinitely long 2D line (defined by a 2D point and a 2D direction) and a 2D line segment (defined by its two 2D end points).
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the line and the line segment are parallel but not collinear, they do not intersect, this method returns {@code null}.
    *    <li> When the line and the line segment are collinear, they are assumed to intersect at {@code lineSegmentStart}.
    *    <li> When the line intersects the line segment at one of its end points, this method returns that same end point.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param pointOnLine a point located on the line. Not modified.
    * @param lineDirection the line direction. Not modified.
    * @param lineSegmentStart the first end point of the line segment. Not modified.
    * @param lineSegmentEnd the second end point of the line segment. Not modified.
    * @param intersectionToPack the 2D point in which the result is stored. Modified.
    * @return the 2D point of intersection if it exist, {@code null} otherwise.
    */
   public static Point2d getIntersectionBetweenLineAndLineSegment(Point2d pointOnLine, Vector2d lineDirection, Point2d lineSegmentStart, Point2d lineSegmentEnd)
   {
      Point2d intersection = new Point2d();
      boolean success = getIntersectionBetweenLineAndLineSegment(pointOnLine, lineDirection, lineSegmentStart, lineSegmentEnd, intersection);
      if (!success)
         return null;
      else
         return intersection;
   }

   /**
    * Computes the intersection between an infinitely long 2D line (defined by a 2D point and a 2D direction) and a 2D line segment (defined by its two 2D end points).
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the line and the line segment are parallel but not collinear, they do not intersect.
    *    <li> When the line and the line segment are collinear, they are assumed to intersect at {@code lineSegmentStart}.
    *    <li> When the line intersects the line segment at one of its end points, this method returns true and the end point is the intersection.
    * </ul>
    * </p>
    * 
    * @param pointOnLine a point located on the line. Not modified.
    * @param lineDirection the line direction. Not modified.
    * @param lineSegmentStart the first end point of the line segment. Not modified.
    * @param lineSegmentEnd the second end point of the line segment. Not modified.
    * @param intersectionToPack the 2D point in which the result is stored. Modified.
    * @return {@code true} if the line intersects the line segment, {@code false} otherwise.
    */
   public static boolean getIntersectionBetweenLineAndLineSegment(Point2d pointOnLine, Vector2d lineDirection, Point2d lineSegmentStart, Point2d lineSegmentEnd,
                                                                  Point2d intersectionToPack)
   {
      return getIntersectionBetweenLineAndLineSegment(pointOnLine.getX(), pointOnLine.getY(), lineDirection.getX(), lineDirection.getY(),
                                                      lineSegmentStart.getX(), lineSegmentStart.getY(), lineSegmentEnd.getX(), lineSegmentEnd.getY(),
                                                      intersectionToPack);
   }

   /**
    * Computes the intersection between an infinitely long 2D line (defined by a 2D point and a 2D direction) and a 2D line segment (defined by its two 2D end points).
    * <p>
    * Edge cases:
    * <ul>
    *    <li> When the line and the line segment are parallel but not collinear, they do not intersect.
    *    <li> When the line and the line segment are collinear, they are assumed to intersect at {@code lineSegmentStart}.
    *    <li> When the line intersects the line segment at one of its end points, this method returns true and the end point is the intersection.
    * </ul>
    * </p>
    * 
    * @param pointOnLineX x-coordinate of a point located on the line.
    * @param pointOnLineX y-coordinate of a point located on the line.
    * @param lineDirectionX x-component of the line direction.
    * @param lineDirectionY y-component of the line direction.
    * @param lineSegmentStartX x-coordinate of the first end point of the line segment.
    * @param lineSegmentStartY y-coordinate of the first end point of the line segment.
    * @param lineSegmentEndX x-coordinate of the second end point of the line segment.
    * @param lineSegmentEndY y-coordinate of the second end point of the line segment.
    * @param intersectionToPack the 2D point in which the result is stored. Modified.
    * @return {@code true} if the line intersects the line segment, {@code false} otherwise.
    */
   public static boolean getIntersectionBetweenLineAndLineSegment(double pointOnLineX, double pointOnLineY, double lineDirectionX, double lineDirectionY,
                                                                  double lineSegmentStartX, double lineSegmentStartY, double lineSegmentEndX,
                                                                  double lineSegmentEndY, Point2d intersectionToPack)
   {
      double lineSegmentDirectionX = lineSegmentEndX - lineSegmentStartX;
      double lineSegmentDirectionY = lineSegmentEndY - lineSegmentStartY;

      //      We solve for x the problem of the form: A * x = b
      //            A      *     x     =      b
      //      / lineDirectionX -lineSegmentDirectionX \   / alpha \   / lineSegmentStartX - pointOnLineX \
      //      |                                       | * |       | = |                                  |
      //      \ lineDirectionY -lineSegmentDirectionY /   \ beta  /   \ lineSegmentStartY - pointOnLineY /
      //
      // Only one coefficient of the pair {alpha, beta} is needed to find the coordinates of the intersection.
      // By using beta, it is possible to also determine if the intersection is between the line segment end points: 0 <= beta <= 1.

      double determinant = -lineDirectionX * lineSegmentDirectionY + lineDirectionY * lineSegmentDirectionX; //(A[0][0] * A[1][1]) - (A[1][0] * A[0][1]);
      double dx = lineSegmentStartX - pointOnLineX;
      double dy = lineSegmentStartY - pointOnLineY;

      double epsilon = 1.0E-12;
      if (Math.abs(determinant) < epsilon)
      { // The line and the line segment are parallel
         // Check if they are collinear
         double cross = dx * lineDirectionY - dy * lineDirectionX;
         if (Math.abs(cross) < epsilon)
         {
            /*
             *  The line and the line segment are collinear.
             *  There's an infinite number of intersection.
             *  Let's just set the result to lineSegmentStart such that it at least belongs to the line segment.
             */
            intersectionToPack.set(lineSegmentStartX, lineSegmentStartY);
            return true;
         }
         else
         {
            return false;
         }
      }
      else
      {
         double oneOverDeterminant = 1.0 / determinant;
         double AInverse10 = oneOverDeterminant * -lineDirectionY; //-A[1][0];
         double AInverse11 = oneOverDeterminant * lineDirectionX; // A[0][0];

         double beta = AInverse10 * dx + AInverse11 * dy;// AInverse10 * b[0] + AInverse11 * b[1];

         if (0.0 - epsilon < beta && beta < 1.0 + epsilon)
         {
            beta = MathTools.clipToMinMax(beta, 0.0, 1.0);
            intersectionToPack.setX(lineSegmentStartX + beta * lineSegmentDirectionX);
            intersectionToPack.setY(lineSegmentStartY + beta * lineSegmentDirectionY);
            return true;
         }
         else
         {
            return false;
         }
      }
   }

   /**
    * Computes the normal of a plane that is defined by three points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> Returns a {@code null} if the three points are on a line.
    *    <li> Returns {@code null} if two or three points are equal.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param firstPointOnPlane first point on the plane. Not modified.
    * @param secondPointOnPlane second point on the plane. Not modified.
    * @param thirdPointOnPlane third point on the plane. Not modified.
    * @return the plane normal or {@code null} when the normal could not be determined.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame.
    */
   public static FrameVector getPlaneNormalGivenThreePoints(FramePoint firstPointOnPlane, FramePoint secondPointOnPlane, FramePoint thirdPointOnPlane)
   {
      FrameVector normal = new FrameVector();
      boolean success = getPlaneNormalGivenThreePoints(firstPointOnPlane, secondPointOnPlane, thirdPointOnPlane, normal);
      if (!success)
         return null;
      else
         return normal;
   }

   /**
    * Computes the normal of a plane that is defined by three points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> Fails and returns {@code false} if the three points are on a line.
    *    <li> Fails and returns {@code false} if two or three points are equal.
    * </ul>
    * </p>
    *
    * @param firstPointOnPlane first point on the plane. Not modified.
    * @param secondPointOnPlane second point on the plane. Not modified.
    * @param thirdPointOnPlane third point on the plane. Not modified.
    * @param normalToPack the vector in which the result is stored. Modified.
    * @return whether the plane normal is properly determined.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame, except for {@code normalToPack}.
    */
   public static boolean getPlaneNormalGivenThreePoints(FramePoint firstPointOnPlane, FramePoint secondPointOnPlane, FramePoint thirdPointOnPlane, FrameVector normalToPack)
   {
      firstPointOnPlane.checkReferenceFrameMatch(secondPointOnPlane);
      firstPointOnPlane.checkReferenceFrameMatch(thirdPointOnPlane);
      normalToPack.setToZero(firstPointOnPlane.getReferenceFrame());

      return getPlaneNormalGivenThreePoints(firstPointOnPlane.getPoint(), secondPointOnPlane.getPoint(), thirdPointOnPlane.getPoint(), normalToPack.getVector());
   }

   /**
    * Computes the normal of a plane that is defined by three points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> Returns a {@code null} if the three points are on a line.
    *    <li> Returns {@code null} if two or three points are equal.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param firstPointOnPlane first point on the plane. Not modified.
    * @param secondPointOnPlane second point on the plane. Not modified.
    * @param thirdPointOnPlane third point on the plane. Not modified.
    * @return the plane normal or {@code null} when the normal could not be determined.
    */
   public static Vector3d getPlaneNormalGivenThreePoints(Point3d firstPointOnPlane, Point3d secondPointOnPlane, Point3d thirdPointOnPlane)
   {
      Vector3d normal = new Vector3d();
      boolean success = getPlaneNormalGivenThreePoints(firstPointOnPlane, secondPointOnPlane, thirdPointOnPlane, normal);
      if (!success)
         return null;
      else
         return normal;
   }

   /**
    * Computes the normal of a plane that is defined by three points.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> Fails and returns {@code false} if the three points are on a line.
    *    <li> Fails and returns {@code false} if two or three points are equal.
    * </ul>
    * </p>
    *
    * @param firstPointOnPlane first point on the plane. Not modified.
    * @param secondPointOnPlane second point on the plane. Not modified.
    * @param thirdPointOnPlane third point on the plane. Not modified.
    * @param normalToPack the vector in which the result is stored. Modified.
    * @return whether the plane normal is properly determined.
    */
   public static boolean getPlaneNormalGivenThreePoints(Point3d firstPointOnPlane, Point3d secondPointOnPlane, Point3d thirdPointOnPlane, Vector3d normalToPack)
   {
      double v1_x = secondPointOnPlane.getX() - firstPointOnPlane.getX();
      double v1_y = secondPointOnPlane.getY() - firstPointOnPlane.getY();
      double v1_z = secondPointOnPlane.getZ() - firstPointOnPlane.getZ();
   
      double v2_x = thirdPointOnPlane.getX() - firstPointOnPlane.getX();
      double v2_y = thirdPointOnPlane.getY() - firstPointOnPlane.getY();
      double v2_z = thirdPointOnPlane.getZ() - firstPointOnPlane.getZ();
   
      normalToPack.setX(v1_y * v2_z - v1_z * v2_y);
      normalToPack.setY(v2_x * v1_z - v2_z * v1_x);
      normalToPack.setZ(v1_x * v2_y - v1_y * v2_x);

      double normalLength = normalToPack.length();
      if (normalLength < 1.0e-7)
         return false;

      normalToPack.scale(1.0 / normalLength);
      return true;
   }

   /**
    * Computes the perpendicular defined by an infinitely long 3D line (defined by two 3D points) and a 3D point.
    * To do so, the orthogonal projection of the {@code point} on line is first computed.
    * The perpendicular vector is computed as follows: {@code perpendicularVector = point - orthogonalProjection},
    * resulting in a vector going from the computed projection to the given {@code point}
    * with a length equal to the distance between the point and the line.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the distance between the two points defining the line is below {@value Epsilons#ONE_TRILLIONTH}, the method fails and returns {@code null}.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param point the 3D point towards which the perpendicular vector should be pointing at. Not modified.
    * @param firstPointOnLine a first point on the line. Not modified.
    * @param secondPointOnLine a second point on the line. Not modified.
    * @param orthogonalProjectionToPack a 3D point in which the projection of {@code point} onto the line is stored. Modified. Can be {@code null}.
    * @return the vector perpendicular to the line and pointing to the {@code point}, or {@code null} when the method fails.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame, except for {@code orthogonalProjectionToPack}.
    */
   public static FrameVector getPerpendicularVectorFromLineToPoint(FramePoint point, FramePoint firstPointOnLine, FramePoint secondPointOnLine, FramePoint orthogonalProjectionToPack)
   {
      FrameVector perpendicularVector = new FrameVector();

      boolean success = getPerpendicularVectorFromLineToPoint(point, firstPointOnLine, secondPointOnLine, orthogonalProjectionToPack, perpendicularVector);
      if (!success)
         return null;
      else
         return perpendicularVector;
   }

   /**
    * Computes the perpendicular defined by an infinitely long 3D line (defined by two 3D points) and a 3D point.
    * To do so, the orthogonal projection of the {@code point} on line is first computed.
    * The perpendicular vector is computed as follows: {@code perpendicularVector = point - orthogonalProjection},
    * resulting in a vector going from the computed projection to the given {@code point}
    * with a length equal to the distance between the point and the line.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the distance between the two points defining the line is below {@value Epsilons#ONE_TRILLIONTH}, the method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param point the 3D point towards which the perpendicular vector should be pointing at. Not modified.
    * @param firstPointOnLine a first point on the line. Not modified.
    * @param secondPointOnLine a second point on the line. Not modified.
    * @param orthogonalProjectionToPack a 3D point in which the projection of {@code point} onto the line is stored. Modified. Can be {@code null}.
    * @param perpendicularVectorToPack a 3D vector in which the vector perpendicular to the line and pointing to the {@code point} is stored. Modified. Can NOT be {@code null}.
    * @return {@code true} if the method succeeded, {@code false} otherwise.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame, except for {@code orthogonalProjectionToPack} and {@code perpendicularVectorToPack}.
    */
   public static boolean getPerpendicularVectorFromLineToPoint(FramePoint point, FramePoint firstPointOnLine, FramePoint secondPointOnLine, FramePoint orthogonalProjectionToPack, FrameVector perpendicularVectorToPack)
   {
      point.checkReferenceFrameMatch(firstPointOnLine);
      point.checkReferenceFrameMatch(secondPointOnLine);
      perpendicularVectorToPack.setToZero(point.getReferenceFrame());

      if (orthogonalProjectionToPack == null)
      {
         return getPerpendicularVectorFromLineToPoint(point.getPoint(), firstPointOnLine.getPoint(), secondPointOnLine.getPoint(), null, perpendicularVectorToPack.getVector());
      }
      else
      {
         orthogonalProjectionToPack.setToZero(point.getReferenceFrame());
         return getPerpendicularVectorFromLineToPoint(point.getPoint(), firstPointOnLine.getPoint(), secondPointOnLine.getPoint(), orthogonalProjectionToPack.getPoint(), perpendicularVectorToPack.getVector());
      }
   }

   /**
    * Computes the perpendicular defined by an infinitely long 3D line (defined by two 3D points) and a 3D point.
    * To do so, the orthogonal projection of the {@code point} on line is first computed.
    * The perpendicular vector is computed as follows: {@code perpendicularVector = point - orthogonalProjection},
    * resulting in a vector going from the computed projection to the given {@code point}
    * with a length equal to the distance between the point and the line.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the distance between the two points defining the line is below {@value Epsilons#ONE_TRILLIONTH}, the method fails and returns {@code null}.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param point the 3D point towards which the perpendicular vector should be pointing at. Not modified.
    * @param firstPointOnLine a first point on the line. Not modified.
    * @param secondPointOnLine a second point on the line. Not modified.
    * @param orthogonalProjectionToPack a 3D point in which the projection of {@code point} onto the line is stored. Modified. Can be {@code null}.
    * @return the vector perpendicular to the line and pointing to the {@code point}, or {@code null} when the method fails.
    */
   public static Vector3d getPerpendicularVectorFromLineToPoint(Point3d point, Point3d firstPointOnLine, Point3d secondPointOnLine, Point3d orthogonalProjectionToPack)
   {
      Vector3d perpendicularVector = new Vector3d();
      boolean success = getPerpendicularVectorFromLineToPoint(point, firstPointOnLine, secondPointOnLine, orthogonalProjectionToPack, perpendicularVector);
      if (!success)
         return null;
      else
         return perpendicularVector;
   }

   /**
    * Computes the perpendicular defined by an infinitely long 3D line (defined by two 3D points) and a 3D point.
    * To do so, the orthogonal projection of the {@code point} on line is first computed.
    * The perpendicular vector is computed as follows: {@code perpendicularVector = point - orthogonalProjection},
    * resulting in a vector going from the computed projection to the given {@code point}
    * with a length equal to the distance between the point and the line.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the distance between the two points defining the line is below {@value Epsilons#ONE_TRILLIONTH}, the method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param point the 3D point towards which the perpendicular vector should be pointing at. Not modified.
    * @param firstPointOnLine a first point on the line. Not modified.
    * @param secondPointOnLine a second point on the line. Not modified.
    * @param orthogonalProjectionToPack a 3D point in which the projection of {@code point} onto the line is stored. Modified. Can be {@code null}.
    * @param perpendicularVectorToPack a 3D vector in which the vector perpendicular to the line and pointing to the {@code point} is stored. Modified. Can NOT be {@code null}.
    * @return {@code true} if the method succeeded, {@code false} otherwise.
    */
   public static boolean getPerpendicularVectorFromLineToPoint(Point3d point, Point3d firstPointOnLine, Point3d secondPointOnLine, Point3d orthogonalProjectionToPack, Vector3d perpendicularVectorToPack)
   {
      double lineDirectionX = secondPointOnLine.getX() - firstPointOnLine.getX();
      double lineDirectionY = secondPointOnLine.getY() - firstPointOnLine.getY();
      double lineDirectionZ = secondPointOnLine.getZ() - firstPointOnLine.getZ();
      double lineLength = 1.0 / Math.sqrt(lineDirectionX * lineDirectionX + lineDirectionY * lineDirectionY + lineDirectionZ * lineDirectionZ);

      if (lineLength < Epsilons.ONE_TRILLIONTH)
         return false;

      lineDirectionX *= lineLength;
      lineDirectionY *= lineLength;
      lineDirectionZ *= lineLength;

      double dx = point.getX() - firstPointOnLine.getX();
      double dy = point.getY() - firstPointOnLine.getY();
      double dz = point.getZ() - firstPointOnLine.getZ();

      double distanceFromFirstPointOnLine = lineDirectionX * dx + lineDirectionY * dy + lineDirectionZ * dz;

      if (orthogonalProjectionToPack != null)
      {
         orthogonalProjectionToPack.set(lineDirectionX, lineDirectionY, lineDirectionZ);
         orthogonalProjectionToPack.scaleAdd(distanceFromFirstPointOnLine, orthogonalProjectionToPack, firstPointOnLine);
         perpendicularVectorToPack.sub(point, orthogonalProjectionToPack);
      }
      else
      {
         perpendicularVectorToPack.set(lineDirectionX, lineDirectionY, lineDirectionZ);
         perpendicularVectorToPack.scale(distanceFromFirstPointOnLine);
         perpendicularVectorToPack.add(firstPointOnLine);
         perpendicularVectorToPack.negate();
         perpendicularVectorToPack.add(point);
      }
      return true;
   }

   /**
    * Computes the vector perpendicular to the given {@code vector} such that:
    * <ul>
    *    <li> {@code vector.dot(perpendicularVector) == 0.0}.
    *    <li> {@code vector.angle(perpendicularVector) == Math.PI / 2.0}.
    * </ul>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param vector the vector to compute the perpendicular of. Not modified.
    * @return the perpendicular vector.
    */
   public static Vector2d getPerpendicularVector(Vector2d vector)
   {
      Vector2d perpendicularVector = new Vector2d();
      getPerpendicularVector(perpendicularVector, vector);

      return new Vector2d(-vector.getY(), vector.getX());
   }

   /**
    * Computes the vector perpendicular to the given {@code vector} such that:
    * <ul>
    *    <li> {@code vector.dot(perpendicularVector) == 0.0}.
    *    <li> {@code vector.angle(perpendicularVector) == Math.PI / 2.0}.
    * </ul>
    * 
    * @param perpendicularVectorToPack a 2D vector in which the perpendicular vector is stored. Modified.
    * @param vector the vector to compute the perpendicular of. Not modified.
    */
   // FIXME reorder arguments.
   public static void getPerpendicularVector(Vector2d perpendicularVectorToPack, Vector2d vector)
   {
      perpendicularVectorToPack.set(-vector.getY(), vector.getX());
   }

   /**
    * Computes the 2D vector perpendicular to the given 2D {@code vector} such that:
    * <ul>
    *    <li> {@code vector2d.dot(perpendicularVector2d) == 0.0}.
    *    <li> {@code vector2d.angle(perpendicularVector2d) == Math.PI / 2.0}.
    * </ul>
    * <p>
    * WARNING: the 3D arguments are projected onto the XY-plane to perform the actual computation in 2D.
    * </p>
    * 
    * @param perpendicularVectorToPack a vector in which the x and y components of the 2D perpendicular vector are stored. Modified.
    * @param vector the vector to compute in the xy-plane the perpendicular of. Not modified.
    */
   // FIXME this is just bad.
   // FIXME reorder arguments.
   public static void getPerpendicularVector2d(FrameVector perpendicularVectorToPack, FrameVector vector)
   {
      perpendicularVectorToPack.set(-vector.getY(), vector.getX(), perpendicularVectorToPack.getZ());
   }

   /**
    * Assuming an isosceles triangle defined by three vertices A, B, and C, with |AB| == |BC|, this methods computes the missing vertex B
    * given the vertices A and C, the normal of the triangle, the angle ABC that is equal to the angle at B from the the leg BA to the leg BC.
    * <a href="https://en.wikipedia.org/wiki/Isosceles_triangle"> Useful link</a>.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param baseVertexA the first base vertex of the isosceles triangle ABC. Not modified.
    * @param baseVertexC the second base vertex of the isosceles triangle ABC. Not modified.
    * @param trianglePlaneNormal  the normal of the plane on which is lying. Not modified.
    * @param ccwAngleAboutNormalAtTopVertex the angle at B from the the leg BA to the leg BC.
    * @param topVertexBToPack the missing vertex B. Modified.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame, except for {@code topVertexBToPack}.
    */
   public static void getTopVertexOfIsoscelesTriangle(FramePoint baseVertexA, FramePoint baseVertexC, FrameVector trianglePlaneNormal,
         double ccwAngleAboutNormalAtTopVertex, FramePoint topVertexBToPack)
   {
      ReferenceFrame commonFrame = baseVertexA.getReferenceFrame();
      baseVertexC.checkReferenceFrameMatch(commonFrame);
      trianglePlaneNormal.checkReferenceFrameMatch(commonFrame);
      topVertexBToPack.setToZero(commonFrame);

      getTopVertexOfIsoscelesTriangle(baseVertexA.getPoint(), baseVertexC.getPoint(), trianglePlaneNormal.getVector(),
            ccwAngleAboutNormalAtTopVertex, topVertexBToPack.getPoint());
   }

   /**
    * Assuming an isosceles triangle defined by three vertices A, B, and C, with |AB| == |BC|, this methods computes the missing vertex B
    * given the vertices A and C, the normal of the triangle, the angle ABC that is equal to the angle at B from the the leg BA to the leg BC.
    * <a href="https://en.wikipedia.org/wiki/Isosceles_triangle"> Useful link</a>.
    * 
    * @param baseVertexA the first base vertex of the isosceles triangle ABC. Not modified.
    * @param baseVertexC the second base vertex of the isosceles triangle ABC. Not modified.
    * @param trianglePlaneNormal  the normal of the plane on which is lying. Not modified.
    * @param ccwAngleAboutNormalAtTopVertex the angle at B from the the leg BA to the leg BC.
    * @param topVertexBToPack the missing vertex B. Modified.
    */
   public static void getTopVertexOfIsoscelesTriangle(Point3d baseVertexA, Point3d baseVertexC, Vector3d trianglePlaneNormal,
         double ccwAngleAboutNormalAtTopVertex, Point3d topVertexBToPack)
   {
      double baseEdgeACx = baseVertexC.getX() - baseVertexA.getX();
      double baseEdgeACy = baseVertexC.getY() - baseVertexA.getY();
      double baseEdgeACz = baseVertexC.getZ() - baseVertexA.getZ();
      double baseEdgeACLength = Math.sqrt(baseEdgeACx * baseEdgeACx + baseEdgeACy * baseEdgeACy + baseEdgeACz * baseEdgeACz);

      double legLengthABorCB = getRadiusOfArc(baseEdgeACLength, ccwAngleAboutNormalAtTopVertex);
      double lengthOfBisectorOfBase = pythagorasGetCathetus(legLengthABorCB, 0.5 * baseEdgeACLength);

      double perpendicularBisectorX = trianglePlaneNormal.getY() * baseEdgeACz - trianglePlaneNormal.getZ() * baseEdgeACy;
      double perpendicularBisectorY = trianglePlaneNormal.getZ() * baseEdgeACx - trianglePlaneNormal.getX() * baseEdgeACz;
      double perpendicularBisectorZ = trianglePlaneNormal.getX() * baseEdgeACy - trianglePlaneNormal.getY() * baseEdgeACx;
      double scale = lengthOfBisectorOfBase;
      scale /= Math.sqrt(perpendicularBisectorX * perpendicularBisectorX + perpendicularBisectorY * perpendicularBisectorY + perpendicularBisectorZ * perpendicularBisectorZ);
      perpendicularBisectorX *= scale;
      perpendicularBisectorY *= scale;
      perpendicularBisectorZ *= scale;

      topVertexBToPack.interpolate(baseVertexA, baseVertexC, 0.5);
      topVertexBToPack.setX(topVertexBToPack.getX() + perpendicularBisectorX);
      topVertexBToPack.setY(topVertexBToPack.getY() + perpendicularBisectorY);
      topVertexBToPack.setZ(topVertexBToPack.getZ() + perpendicularBisectorZ);
   }

   /**
    * Returns the radius of an arc with the specified chord length and angle.
    * <a href="http://planetcalc.com/1421/"> Useful link</a>.
    *
    * @param chordLength the length of the chord.
    * @param chordAngle angle covered by the chord.
    * @return the radius of the arc, or {@code Double.NaN} if {@code chordAngle % Math.PI == 0.0}.
    */
   public static double getRadiusOfArc(double chordLength, double chordAngle)
   {
      if (chordAngle % Math.PI == 0.0)
         return Double.NaN;
      else
         return chordLength / (2.0 * Math.sin(0.5 * chordAngle));
   }

   /**
    * Clip each component of the given tuple to the axis-aligned bounding box.
    * Each of the bounding box minimum coordinates is defined as follows: {@code minX = x1 < x2 ? x1 : x2}.
    * Each of the bounding box maximum coordinates is defined as follows: {@code maxX = x1 > x2 ? x1 : x2}.
    * 
    * @param tupleToClip the 3D tuple to clip to the bounding box. Modified.
    * @param x1 minimum/maximum x coordinate of the bounding box.
    * @param x2 minimum/maximum x coordinate of the bounding box.
    * @param y1 minimum/maximum y coordinate of the bounding box.
    * @param y2 minimum/maximum y coordinate of the bounding box.
    * @param z1 minimum/maximum z coordinate of the bounding box.
    * @param z2 minimum/maximum z coordinate of the bounding box.
    */
   // FIXME this is rather unsafe, the user should know the difference between the minimum and maximum coordinates of the bounding box.
   public static void clipToBoundingBox(Tuple3d tupleToClip, double x1, double x2, double y1, double y2, double z1, double z2)
   {
      tupleToClip.setX(x1 < x2 ? MathTools.clipToMinMax(tupleToClip.getX(), x1, x2) : MathTools.clipToMinMax(tupleToClip.getX(), x2, x1));
      tupleToClip.setY(y1 < y2 ? MathTools.clipToMinMax(tupleToClip.getY(), y1, y2) : MathTools.clipToMinMax(tupleToClip.getY(), y2, y1));
      tupleToClip.setZ(z1 < z2 ? MathTools.clipToMinMax(tupleToClip.getZ(), z1, z2) : MathTools.clipToMinMax(tupleToClip.getZ(), z2, z1));
   }

   /**
    * Computes the 2D perpendicular bisector of 2D line segment defined by its two 2D end points.
    * The bisector starts off the the middle of the 2D line segment and points toward the left side of the 2D line segment.
    * <p>
    * WARNING: the 3D arguments are projected onto the XY-plane to perform the actual computation in 2D.
    * </p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the line segment end points are equal,
    *     more precisely when {@code lineSegmentStart.distance(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *     the method fails and returns {@code false}.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param lineSegmentStart the x and y coordinates are used to defined the first end point of the 2D line segment. Not modified.
    * @param lineSegmentEnd the x and y coordinates are used to defined the second end point of the line segment. Not modified.
    * @param bisectorStartToPack a 2D point in which the x and y coordinates of the origin of the bisector are stored. Modified.
    * @param bisectorDirectionToPack a 2D vector in which the x and y components of the direction of the bisector are stored. Modified.
    * @return whether the perpendicular bisector could be determined or not.
    */
   // FIXME same thing, the use of 3D arguments for doing computation in 2D is confusing and error prone.
   // FIXME the reference frames of the arguments need to checked and throw an exception if they are not the same.
   public static boolean getZPlanePerpendicularBisector(FramePoint lineSegmentStart, FramePoint lineSegmentEnd, FramePoint bisectorStartToPack, FrameVector bisectorDirectionToPack)
   {
      Point2d lineStart2d = new Point2d(lineSegmentStart.getX(), lineSegmentStart.getY());
      Point2d lineEnd2d = new Point2d(lineSegmentEnd.getX(), lineSegmentEnd.getY());
      Point2d bisectorStart2d = new Point2d();
      Vector2d bisectorDirection2d = new Vector2d();

      boolean success = getPerpendicularBisector(lineStart2d, lineEnd2d, bisectorStart2d, bisectorDirection2d);
      if (!success)
         return false;
      bisectorDirectionToPack.set(bisectorDirection2d.getX(), bisectorDirection2d.getY(), 0.0);
      bisectorStartToPack.set(bisectorStart2d.getX(), bisectorStart2d.getY(), 0.0);
      return true;
   }

   /**
    * Computes the perpendicular bisector of line segment defined by its two end points.
    * The bisector starts off the the middle of the line segment and points toward the left side of the line segment.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the line segment end points are equal,
    *     more precisely when {@code lineSegmentStart.distance(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *     the method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param lineSegmentStart the first end point of the line segment. Not modified.
    * @param lineSegmentEnd the second end point of the line segment. Not modified.
    * @param bisectorStartToPack a 2D point in which the origin of the bisector is stored. Modified.
    * @param bisectorDirectionToPack a 2D vector in which the direction of the bisector is stored. Modified.
    * @return whether the perpendicular bisector could be determined or not.
    */
   public static boolean getPerpendicularBisector(Point2d lineSegmentStart, Point2d lineSegmentEnd, Point2d bisectorStartToPack, Vector2d bisectorDirectionToPack)
   {
      if (lineSegmentStart.distance(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH)
         return false;

      // direction will be on left side of line
      bisectorStartToPack.interpolate(lineSegmentStart, lineSegmentEnd, 0.5);
      bisectorDirectionToPack.sub(lineSegmentEnd, lineSegmentStart);
      getPerpendicularVector(bisectorDirectionToPack, bisectorDirectionToPack);
      bisectorDirectionToPack.normalize();
      return true;
   }

   /**
    * Computes the end points of the perpendicular bisector segment to a line segment defined by its end points, such that:
    * <ul>
    *    <li> each end point of the perpendicular bisector is at a distance of {@code bisectorSegmentHalfLength} from the line segment.
    *    <li> the first perpendicular bisector end point is located on the left side on the line segment.
    *    <li> the second perpendicular bisector end point is located on the right side on the line segment.
    * </ul>
    * <p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the line segment end points are equal,
    *     more precisely when {@code lineSegmentStart.distance(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *     the method fails and returns {@code null}.
    * </ul>
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param lineSegmentStart the first end point of the line segment from which the perpendicular bisector is to be computed. Not modified.
    * @param lineSegmentEnd the second end point of the line segment from which the perpendicular bisector is to be computed. Not modified.
    * @param bisectorSegmentHalfLength distance from the line segment each end point of the perpendicular bisector segment will be positioned.
    * @return a list containing the two end points of the perpendicular bisector segment.
    */
   public static List<Point2d> getPerpendicularBisectorSegment(Point2d lineSegmentStart, Point2d lineSegmentEnd, double bisectorSegmentHalfLength)
   {
      Point2d bisectorSegmentStart = new Point2d();
      Point2d bisectorSegmentEnd = new Point2d();

      boolean success = getPerpendicularBisectorSegment(lineSegmentStart, lineSegmentEnd, bisectorSegmentHalfLength, bisectorSegmentStart, bisectorSegmentEnd);
      if (!success)
         return null;

      List<Point2d> bisectorEndpoints = new ArrayList<>();
      bisectorEndpoints.add(bisectorSegmentStart);
      bisectorEndpoints.add(bisectorSegmentEnd);
      return bisectorEndpoints;
   }

   /**
    * Computes the end points of the perpendicular bisector segment to a line segment defined by its end points, such that:
    * <ul>
    *    <li> each end point of the perpendicular bisector is at a distance of {@code bisectorSegmentHalfLength} from the line segment.
    *    <li> the first perpendicular bisector end point is located on the left side on the line segment.
    *    <li> the second perpendicular bisector end point is located on the right side on the line segment.
    * </ul>
    * <p>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> when the line segment end points are equal,
    *     more precisely when {@code lineSegmentStart.distance(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH},
    *     the method fails and returns false.
    * </ul>
    * </p>
    * 
    * @param lineSegmentStart the first end point of the line segment from which the perpendicular bisector is to be computed. Not modified.
    * @param lineSegmentEnd the second end point of the line segment from which the perpendicular bisector is to be computed. Not modified.
    * @param bisectorSegmentHalfLength distance from the line segment each end point of the perpendicular bisector segment will be positioned.
    * @param bisectorSegmentStartToPack the first end point of the perpendicular bisector segment to be computed. Modified.
    * @param bisectorSegmentEndToPack the second end point of the perpendicular bisector segment to be computed. Modified.
    * @return whether the perpendicular bisector could be determined or not.
    */
   public static boolean getPerpendicularBisectorSegment(Point2d lineSegmentStart, Point2d lineSegmentEnd, double bisectorSegmentHalfLength,
                                                         Point2d bisectorSegmentStartToPack, Point2d bisectorSegmentEndToPack)
   {
      if (lineSegmentStart.distance(lineSegmentEnd) < Epsilons.ONE_TRILLIONTH)
         return false;

      // direction will be on left side of line
      double bisectorDirectionX = -(lineSegmentEnd.getY() - lineSegmentStart.getY());
      double bisectorDirectionY = lineSegmentEnd.getX() - lineSegmentStart.getX();
      double directionInverseMagnitude = 1.0 / Math.sqrt(bisectorDirectionX * bisectorDirectionX + bisectorDirectionY * bisectorDirectionY);
      bisectorDirectionX *= directionInverseMagnitude;
      bisectorDirectionY *= directionInverseMagnitude;

      double midPointX = 0.5 * (lineSegmentStart.getX() + lineSegmentEnd.getX());
      double midPointY = 0.5 * (lineSegmentStart.getY() + lineSegmentEnd.getY());

      bisectorSegmentStartToPack.setX(midPointX + bisectorDirectionX * bisectorSegmentHalfLength);
      bisectorSegmentStartToPack.setY(midPointY + bisectorDirectionY * bisectorSegmentHalfLength);
      bisectorSegmentEndToPack.setX(midPointX - bisectorDirectionX * bisectorSegmentHalfLength);
      bisectorSegmentEndToPack.setY(midPointY - bisectorDirectionY * bisectorSegmentHalfLength);

      return true;
   }

   /**
    * Computes the complete minimum rotation from {@code zUp = (0, 0, 1)} to the given {@code normalVector3d} and packs it into an {@link AxisAngle4d}.
    * The rotation axis if perpendicular to both vectors.
    * The rotation angle is computed as the angle from the {@code zUp} to the {@code normalVector3d}: {@code rotationAngle = referenceNormal.angle(rotatedNormal)}.
    * Note: the vector do not need to be unit length.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> the vector is aligned with {@code zUp}: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    *    <li> the vector collinear pointing opposite direction of {@code zUp}: the rotation angle is equal to {@code Math.PI} and the rotation axis is set to: (1, 0, 0).
    *    <li> if the length of the given normal is below {@code 1.0E-7}: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    * </ul>
    * </p>
    * The calculation becomes less accurate as the two vectors are more collinear.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param normalVector3d the vector that is rotated with respect to {@code zUp}. Not modified.
    * @return the minimum rotation from {@code zUp} to the {@code normalVector3d}.
    */
   // TODO this is a bad name for this method, should be renamed for something like getAxisAngleFromZUpToVector
   public static AxisAngle4d getRotationBasedOnNormal(Vector3d normalVector3d)
   {
      AxisAngle4d newAxisAngle4d = new AxisAngle4d();
      getRotationBasedOnNormal(newAxisAngle4d, normalVector3d);
      return newAxisAngle4d;
   }

   /**
    * Computes the complete minimum rotation from {@code zUp = (0, 0, 1)} to the given {@code normalVector3d} and packs it into an {@link AxisAngle4d}.
    * The rotation axis if perpendicular to both vectors.
    * The rotation angle is computed as the angle from the {@code zUp} to the {@code normalVector3d}: {@code rotationAngle = referenceNormal.angle(rotatedNormal)}.
    * Note: the vector do not need to be unit length.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> the vector is aligned with {@code zUp}: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    *    <li> the vector collinear pointing opposite direction of {@code zUp}: the rotation angle is equal to {@code Math.PI} and the rotation axis is set to: (1, 0, 0).
    *    <li> if the length of the given normal is below {@code 1.0E-7}: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    * </ul>
    * </p>
    * The calculation becomes less accurate as the two vectors are more collinear.
    * 
    * @param rotationToPack the minimum rotation from {@code zUp} to the {@code normalVector3d}. Modified.
    * @param normalVector3d the vector that is rotated with respect to {@code zUp}. Not modified.
    */
   // FIXME reorder the arguments so the argument to comes last.
   // TODO this is a bad name for this method, should be renamed for something like getAxisAngleFromVectorToZUp
   public static void getRotationBasedOnNormal(AxisAngle4d rotationToPack, Vector3d normalVector3d)
   {
      getRotationBasedOnNormal(rotationToPack, normalVector3d.getX(), normalVector3d.getY(), normalVector3d.getZ(), 0.0, 0.0, 1.0);
   }

   /**
    * Computes the complete minimum rotation from {@code referenceNormal} to the {@code rotatedNormal} and packs it into an {@link AxisAngle4d}.
    * The rotation axis if perpendicular to both vectors.
    * The rotation angle is computed as the angle from the {@code referenceNormal} to the {@code rotatedNormal}: {@code rotationAngle = referenceNormal.angle(rotatedNormal)}.
    * Note: the vectors do not need to be unit length.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> the vectors are the same: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    *    <li> the vectors collinear pointing opposite directions: the rotation angle is equal to {@code Math.PI} and the rotation axis is set to: (1, 0, 0).
    *    <li> if the length of either normal is below {@code 1.0E-7}: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    * </ul>
    * </p>
    * The calculation becomes less accurate as the two vectors are more collinear.
    * 
    * @param rotationToPack the minimum rotation from {@code referenceNormal} to the {@code rotatedNormal}. Modified.
    * @param rotatedNormal the vector that is rotated with respect to the reference. Not modified.
    * @param referenceNormal the vector used as reference. Not modified.
    */
   // FIXME reorder the arguments so the argument to comes last.
   // TODO this is a bad name for this method, should be renamed for something like getAxisAngleFromFirstToSecondVector
   public static void getRotationBasedOnNormal(AxisAngle4d rotationToPack, Vector3d rotatedNormal, Vector3d referenceNormal)
   {
      getRotationBasedOnNormal(rotationToPack, rotatedNormal.getX(), rotatedNormal.getY(), rotatedNormal.getZ(), referenceNormal.getX(), referenceNormal.getY(),
                               referenceNormal.getZ());
   }

   /**
    * Computes the complete minimum rotation from {@code referenceNormal} to the {@code rotatedNormal} and packs it into an {@link AxisAngle4d}.
    * The rotation axis if perpendicular to both vectors.
    * The rotation angle is computed as the angle from the {@code referenceNormal} to the {@code rotatedNormal}: {@code rotationAngle = referenceNormal.angle(rotatedNormal)}.
    * Note: the vectors do not need to be unit length.
    * <p>
    * Edge cases:
    * <ul>
    *    <li> the vectors are the same: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    *    <li> the vectors collinear pointing opposite directions: the rotation angle is equal to {@code Math.PI} and the rotation axis is set to: (1, 0, 0).
    *    <li> if the length of either normal is below {@code 1.0E-7}: the rotation angle is equal to {@code 0.0} and the rotation axis is set to: (1, 0, 0).
    * </ul>
    * </p>
    * The calculation becomes less accurate as the two vectors are more collinear.
    * 
    * @param rotationToPack the minimum rotation from {@code referenceNormal} to the {@code rotatedNormal}. Modified.
    * @param rotatedNormalX x-component of the vector that is rotated with respect to the reference.
    * @param rotatedNormalY y-component of the vector that is rotated with respect to the reference.
    * @param rotatedNormalZ z-component of the vector that is rotated with respect to the reference.
    * @param referenceNormal x-component of the vector used as reference.
    * @param referenceNormal y-component of the vector used as reference.
    * @param referenceNormal z-component of the vector used as reference.
    */
   // FIXME reorder the arguments so the argument to comes last.
   // TODO this is a bad name for this method, should be renamed for something like getAxisAngleFromFirstToSecondVector
   public static void getRotationBasedOnNormal(AxisAngle4d rotationToPack, double rotatedNormalX, double rotatedNormalY, double rotatedNormalZ,
                                               double referenceNormalX, double referenceNormalY, double referenceNormalZ)
   {
      double rotationAxisX = referenceNormalY * rotatedNormalZ - referenceNormalZ * rotatedNormalY;
      double rotationAxisY = referenceNormalZ * rotatedNormalX - referenceNormalX * rotatedNormalZ;
      double rotationAxisZ = referenceNormalX * rotatedNormalY - referenceNormalY * rotatedNormalX;
      double rotationAxisLength = Math.sqrt(rotationAxisX * rotationAxisX + rotationAxisY * rotationAxisY + rotationAxisZ * rotationAxisZ);

      boolean normalsAreParallel = rotationAxisLength < 1e-7;

      double dot;
      dot = rotatedNormalX * referenceNormalX;
      dot += rotatedNormalY * referenceNormalY;
      dot += rotatedNormalZ * referenceNormalZ;

      if (normalsAreParallel)
      {
         double rotationAngle = dot > 0.0 ? 0.0 : Math.PI;
         rotationToPack.set(1.0, 0.0, 0.0, rotationAngle);
         return;
      }

      double rotationAngle = getAngleFromFirstToSecondVector(referenceNormalX, referenceNormalY, referenceNormalZ, rotatedNormalX, rotatedNormalY, rotatedNormalZ);

      rotationAxisX /= rotationAxisLength;
      rotationAxisY /= rotationAxisLength;
      rotationAxisZ /= rotationAxisLength;
      rotationToPack.set(rotationAxisX, rotationAxisY, rotationAxisZ, rotationAngle);
   }

   /**
    *  Given a triangle defined by three points (A,B,C), this methods the point
    *  X &in; AC such that the line (B, X) is the angle bisector of B.
    *  As a result, the two angles CBX and XBA are equal.
    *  <a href="https://en.wikipedia.org/wiki/Angle_bisector_theorem"> Useful link</a>.
    *<p>
    *Edge cases:
    *<ul>
    *   <li> if any the triangle's edge is shorter than {@link Epsilons#ONE_TRILLIONTH},
    *    this method fails and returns {@code null}.
    *</ul>
    *</p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param A the first vertex of the triangle. Not modified.
    * @param B the second vertex of the triangle, this is the first end point of the bisector. Not modified.
    * @param C the third vertex of the triangle. Not modified.
    * @return the second end point of the bisector, or {@code null} if the method failed.
    */
   public static Point2d getTriangleBisector(Point2d A, Point2d B, Point2d C)
   {
      Point2d X = new Point2d();
      getTriangleBisector(A, B, C, X);
      return X;
   }

   /**
    *  Given a triangle defined by three points (A,B,C), this methods the point
    *  X &in; AC such that the line (B, X) is the angle bisector of B.
    *  As a result, the two angles CBX and XBA are equal.
    *  <a href="https://en.wikipedia.org/wiki/Angle_bisector_theorem"> Useful link</a>.
    *<p>
    *Edge cases:
    *<ul>
    *   <li> if any the triangle's edge is shorter than {@link Epsilons#ONE_TRILLIONTH},
    *    this method fails and returns {@code false}.
    *</ul>
    *</p>
    *
    * @param A the first vertex of the triangle. Not modified.
    * @param B the second vertex of the triangle, this is the first end point of the bisector. Not modified.
    * @param C the third vertex of the triangle. Not modified.
    * @param XToPack point in which the second end point of the bisector is stored. Modified.
    * @return whether the bisector could be calculated or not.
    */
   public static boolean getTriangleBisector(Point2d A, Point2d B, Point2d C, Point2d XToPack)
   {
      // find all proportional values
      double BA = B.distance(A);
      if (BA < Epsilons.ONE_TRILLIONTH)
         return false;

      double BC = B.distance(C);
      if (BC < Epsilons.ONE_TRILLIONTH)
         return false;

      double AC = A.distance(C);

      if (AC < Epsilons.ONE_TRILLIONTH)
         return false;

      double AX = AC / ((BC / BA) + 1.0);

      // use AX distance to find X along AC
      double vectorAXx = C.getX() - A.getX();
      double vectorAXy = C.getY() - A.getY();
      double inverseMagnitude = 1.0 / Math.sqrt(vectorAXx * vectorAXx + vectorAXy * vectorAXy);
      vectorAXx *= AX * inverseMagnitude;
      vectorAXy *= AX * inverseMagnitude;

      XToPack.set(vectorAXx, vectorAXy);
      XToPack.add(A);
      return false;
   }

   /**
    * Computes the angle in radians from the first 2D vector to the second 2D vector.
    * The computed angle is in the range [-<i>pi</i>; <i>pi</i>].
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either vector is below {@code 1.0E-7}, this method fails and returns an angle of {@code 0.0} radian.
    * </ul>
    * </p>
    * 
    * @param vector1x x-component of first the vector.
    * @param vector1y y-component of first the vector.
    * @param vector2x x-component of second the vector.
    * @param vector2y y-component of second the vector.
    * @return the angle in radians from the first vector to the second vector.
    */
   public static double getAngleFromFirstToSecondVector(Vector2d firstVector, Vector2d secondVector)
   {
      return getAngleFromFirstToSecondVector(firstVector.getX(), firstVector.getY(), secondVector.getX(), secondVector.getY());
   }

   /**
    * Computes the angle in radians from the first 2D vector to the second 2D vector.
    * The computed angle is in the range [-<i>pi</i>; <i>pi</i>].
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either vector is below {@code 1.0E-7}, this method fails and returns an angle of {@code 0.0} radian.
    * </ul>
    * </p>
    * 
    * @param firstVector the first vector. Not modified.
    * @param secondVector the second vector. Not modified.
    * @return the angle in radians from the first vector to the second vector.
    */
   public static double getAngleFromFirstToSecondVector(double firstVectorX, double firstVectorY, double secondVectorX, double secondVectorY)
   {
      double firstVectorLength = Math.sqrt(firstVectorX * firstVectorX + firstVectorY * firstVectorY);

      if (firstVectorLength < 1e-7)
         return 0.0;

      firstVectorX /= firstVectorLength;
      firstVectorY /= firstVectorLength;

      double secondVectorLength = Math.sqrt(secondVectorX * secondVectorX + secondVectorY * secondVectorY);

      if (secondVectorLength < 1e-7)
         return 0.0;

      secondVectorX /= secondVectorLength;
      secondVectorY /= secondVectorLength;

      // The sign of the angle comes from the cross product
      double crossProduct = firstVectorX * secondVectorY - firstVectorY * secondVectorX;
      // the magnitude of the angle comes from the dot product
      double dotProduct = firstVectorX * secondVectorX + firstVectorY * secondVectorY;

      double angle = Math.atan2(crossProduct, dotProduct);
      // This is a hack to get the polygon tests to pass.
      // Probably some edge case not well handled somewhere (Sylvain)
      if (crossProduct == 0.0)
         angle = -angle;

      return angle;
   }

   /**
    * Computes the angle in radians from the first 3D vector to the second 3D vector.
    * The computed angle is in the range [0; <i>pi</i>].
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either vector is below {@code 1.0E-7}, this method fails and returns an angle of {@code 0.0} radian.
    * </ul>
    * </p>
    * 
    * @param firstVector the first vector. Not modified.
    * @param secondVector the second vector. Not modified.
    * @return the angle in radians from the first vector to the second vector.
    */
   public static double getAngleFromFirstToSecondVector(Vector3d firstVector, Vector3d secondVector)
   {
      return getAngleFromFirstToSecondVector(firstVector.getX(), firstVector.getY(), firstVector.getZ(), secondVector.getX(), secondVector.getY(), secondVector.getZ());
   }

   /**
    * Computes the angle in radians from the first 3D vector to the second 3D vector.
    * The computed angle is in the range [0; <i>pi</i>].
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either vector is below {@code 1.0E-7}, this method fails and returns an angle of {@code 0.0} radian.
    * </ul>
    * </p>
    * 
    * @param firstVectorX x-component of first the vector.
    * @param firstVectorY y-component of first the vector.
    * @param firstVectorZ z-component of first the vector.
    * @param secondVectorX x-component of second the vector.
    * @param secondVectorY y-component of second the vector.
    * @param secondVectorZ z-component of second the vector.
    * @return the angle in radians from the first vector to the second vector.
    */
   public static double getAngleFromFirstToSecondVector(double firstVectorX, double firstVectorY, double firstVectorZ, double secondVectorX, double secondVectorY, double secondVectorZ)
   {
      double firstVectorLength = Math.sqrt(firstVectorX * firstVectorX + firstVectorY * firstVectorY + firstVectorZ * firstVectorZ);

      if (firstVectorLength < 1e-7)
         return 0.0;

      double secondVectorLength = Math.sqrt(secondVectorX * secondVectorX + secondVectorY * secondVectorY + secondVectorZ * secondVectorZ);

      if (secondVectorLength < 1e-7)
         return 0.0;

      double dotProduct = firstVectorX * secondVectorX + firstVectorY * secondVectorY + firstVectorZ * secondVectorZ;
      dotProduct /= firstVectorLength * secondVectorLength;

      return Math.acos(MathTools.clipToMinMax(dotProduct, -1.0, 1.0));
   }

   /**
    * Computes the Euclidean distance between the two given n-dimensional points {@code a} and {@code b}:
    * <br>
    * distance = Sqrt{  &sum;<sub>i=1:n</sub> { (a<sub>i</sub> - b<sub>i</sub>)<sup>2</sup>}   }
    * <br>
    *
    * @param a array containing the first point coordinates. Not modified.
    * @param b array containing the second point coordinates. Not modified.
    * @return the distance between the two points.
    * @throws IllegalArgumentException if the two vectors have different lengths.
    */
   public static double distanceBetweenPoints(double[] a, double[] b)
   {
      if (a.length != b.length)
      {
         throw new IllegalArgumentException("cannot find distance between points of different dimensions");
      }

      double distance = 0.0;
      for (int i = 0; i < a.length; i++)
      {
         double delta = a[i] - b[i];
         distance += delta * delta;
      }

      distance = Math.sqrt(distance);

      return distance;
   }

   /**
    * Calculates the distance between two points.
    * 
    * @param firstPointX the x-coordinate of the first point.
    * @param firstPointY the y-coordinate of the first point.
    * @param secondPointX the x-coordinate of the second point.
    * @param secondPointY the y-coordinate of the second point.
    * @return the distance between the two points.
    */
   public static double distanceBetweenPoints(double firstPointX, double firstPointY, double secondPointX, double secondPointY)
   {
      double deltaX = secondPointX - firstPointX;
      double deltaY = secondPointY - firstPointY;
      return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
   }

   /**
    * Calculates the distance on the xy-plane bewteen two 3D points.
    * <p>
    * WARNING: the 3D arguments are projected onto the XY-plane to perform the actual computation in 2D.
    * </p>
    * 
    * @param firstPoint the first point. Not modified.
    * @param secondPoint the second point. Not modified.
    * @return the distance between the two points.
    * @throws ReferenceFrameMismatchException if the arguments are not expressed in the same reference frame.
    */
   public static double getXYDistance(FramePoint firstPoint, FramePoint secondPoint)
   {
      firstPoint.checkReferenceFrameMatch(secondPoint);
      return getXYDistance(firstPoint.getPoint(), secondPoint.getPoint());
   }

   /**
    * Calculates the distance on the xy-plane bewteen two 3D points.
    * <p>
    * WARNING: the 3D arguments are projected onto the XY-plane to perform the actual computation in 2D.
    * </p>
    * 
    * @param firstPoint the first point. Not modified.
    * @param secondPoint the second point. Not modified.
    * @return the distance between the two points.
    */
   public static double getXYDistance(Point3d firstPoint, Point3d secondPoint)
   {
      return distanceBetweenPoints(firstPoint.getX(), firstPoint.getY(), secondPoint.getX(), secondPoint.getY());
   }

   /**
    * Computes the dot product between two vectors each defined by two points:
    * <ul>
    *    <li> {@code vector1 = end1 - start1}
    *    <li> {@code vector2 = end2 - start2}
    * </ul>
    * 
    * @param start1 the origin of the first vector. Not modified.
    * @param end1 the end of the first vector. Not modified.
    * @param start2 the origin of the second vector. Not modified.
    * @param end2 the end of the second vector. Not modified.
    * @return the value of the dot product of the two vectors.
    */
   public static double dotProduct(Point2d start1, Point2d end1, Point2d start2, Point2d end2)
   {
      double vector1X = end1.getX() - start1.getX();
      double vector1Y = end1.getY() - start1.getY();
      double vector2X = end2.getX() - start2.getX();
      double vector2Y = end2.getY() - start2.getY();
      
      return vector1X * vector2X + vector1Y * vector2Y;
   }

   /**
    * Attempts to normalize the given 3D vector.
    * If the vector's length falls below {@value Epsilons#ONE_TRILLIONTH}, the vector is set to (0, 0, 1).
    *  
    * @param vector the 3D vector to normalize. Modified.
    */
   public static void normalizeSafelyZUp(Vector3d vector)
   {
      double distance = vector.length();

      if (distance > Epsilons.ONE_TRILLIONTH)
      {
         vector.scale(1.0 / distance);
      }
      else
      {
         vector.set(0.0, 0.0, 1.0);
      }
   }

   /**
    * Tests if the two given vectors are collinear given a tolerance on the angle between the two vector axes in the range ]0; <i>pi</i>/2[.
    * This method returns {@code true} if the two vectors are collinear, whether they are pointing in the same direction or in opposite directions.
    * 
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either vector is below {@code 1.0E-7}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param firstVector the first vector. Not modified.
    * @param secondVector the second vector. Not modified.
    * @param angleEpsilon tolerance on the angle in radians.
    * @return {@code true} if the two vectors are collinear, {@code false} otherwise.
    */
   public static boolean areVectorsCollinear(Vector3d firstVector, Vector3d secondVector, double angleEpsilon)
   {
      double firstVectorLength = firstVector.length();
      if (firstVectorLength < Epsilons.ONE_TEN_MILLIONTH)
         return false;
      double secondVectorLength = secondVector.length();
      if (secondVectorLength < Epsilons.ONE_TEN_MILLIONTH)
         return false;
      return Math.abs(firstVector.dot(secondVector) / (firstVectorLength * secondVectorLength)) > Math.cos(angleEpsilon);
   }

   /**
    * Tests if the two given planes are coplanar:
    * <ul>
    *    <li> {@code planeNormal1} and {@code planeNormal2} are collinear given the tolerance {@code angleEpsilon}.
    *    <li> the distance of {@code pointOnPlane2} from the first plane is less than {@code distanceEpsilon}.
    * </ul>
    * <p>
    * Edge cases:
    * <ul>
    *    <li> if the length of either normal is below {@code 1.0E-7}, this method fails and returns {@code false}.
    * </ul>
    * </p>
    * 
    * @param pointOnPlane1 a point on the first plane. Not modified.
    * @param planeNormal1 the normal of the first plane. Not modifed.
    * @param pointOnPlane2 a point on the second plane. Not modified.
    * @param planeNormal2 the normal of the second plane. Not modified.
    * @param angleEpsilon tolerance on the angle in radians to determine if the plane normals are collinear. 
    * @param distanceEpsilon tolerance on the distance to determine if {@code pointOnPlane2} belongs to the first plane.
    * @return {@code true} if the two planes are coplanar, {@code false} otherwise.
    */
   public static boolean arePlanesCoplanar(Point3d pointOnPlane1, Vector3d planeNormal1, Point3d pointOnPlane2, Vector3d planeNormal2, double angleEpsilon,
                                           double distanceEpsilon)
   {
      if (!areVectorsCollinear(planeNormal1, planeNormal2, angleEpsilon))
         return false;
      else
         return distanceFromPointToPlane(pointOnPlane2, pointOnPlane1, planeNormal1) < distanceEpsilon;
   }

// TODO move to polygon?
   @Deprecated
   public static void movePointInsidePolygonAlongLine(FramePoint2d point, FrameConvexPolygon2d polygon, FrameLine2d line)
   {
      // Defaults to 2mm for desired capture to prevent some jerky behavior with VirtualToePoints.. // TODO: remove
      double amountToBeInside = 0.002;
      movePointInsidePolygonAlongLine(point, polygon, line, amountToBeInside);
   }

// TODO move to polygon?
   @Deprecated
   public static void movePointInsidePolygonAlongLine(FramePoint2d point, FrameConvexPolygon2d polygon, FrameLine2d line, double amountToBeInside)
   {
      if (!polygon.isPointInside(point))
      {
         FramePoint2d[] intersections = polygon.intersectionWith(line);
         if (intersections != null)
         {
            FramePoint2d intersectionToUse;

            if (intersections.length == 2)
            {
               double distanceSquaredToIntersection0 = point.distanceSquared(intersections[0]);
               double distanceSquaredToIntersection1 = point.distanceSquared(intersections[1]);

               if (distanceSquaredToIntersection0 <= distanceSquaredToIntersection1)
                  intersectionToUse = intersections[0];
               else
                  intersectionToUse = intersections[1];


               point.setX(intersectionToUse.getX());
               point.setY(intersectionToUse.getY());

               // Move in a little along the line:
               FrameLineSegment2d guideLineSegment = new FrameLineSegment2d(intersections);
               FrameVector2d frameVector2d = new FrameVector2d();
               guideLineSegment.getFrameVector(frameVector2d);
               if (intersectionToUse == intersections[1])
                  frameVector2d.scale(-1.0);
               frameVector2d.normalize();
               frameVector2d.scale(amountToBeInside);

               point.setX(point.getX() + frameVector2d.getX());
               point.setY(point.getY() + frameVector2d.getY());
            }
            else
            {
               throw new RuntimeException("This is interesting, shouldn't get here.");
            }
         }
         else
         {
            point.set(polygon.getClosestVertexCopy(line));
         }
      }
   }

   public static void movePointInsidePolygonAlongVector(FramePoint2d pointToMove, FrameVector2d vector, FrameConvexPolygon2d polygon, double distanceToBeInside)
   {
      if (polygon.getNumberOfVertices() < 2)
      {
         return;
      }

      if (distanceToBeInside < 0.0)
         throw new RuntimeException("distanceToBeInside = " + distanceToBeInside);

      FrameLine2d line = new FrameLine2d(pointToMove, vector);
      FramePoint2d[] intersections = polygon.intersectionWith(line);

      if (intersections != null)
      {
         if ((intersections.length != 2) && (intersections.length != 1))
            throw new RuntimeException("intersections.length != 2 && intersections.length != 1. intersections.length = " + intersections.length);

         if (intersections.length == 1)
         {
            pointToMove.set(intersections[0]);

            return;
         }

         // make sure it's inside or on the edge of the polygon
         boolean insidePolygon = polygon.isPointInside(pointToMove);
         if (!insidePolygon)
         {
            double minDistance = Double.POSITIVE_INFINITY;
            FramePoint2d closestIntersection = null;
            for (int i = 0; i < intersections.length; i++)
            {
               FramePoint2d intersection = intersections[i];
               double distance = pointToMove.distance(intersection);
               if (distance < minDistance)
               {
                  minDistance = distance;
                  closestIntersection = intersection;
               }
            }

            pointToMove.set(closestIntersection);
         }

         // make sure distance constraint is met; if infeasible, use midpoint of intersections
         double distanceBetweenIntersections = intersections[0].distance(intersections[1]);
         boolean constraintFeasible = distanceBetweenIntersections > 2.0 * distanceToBeInside;

         if (constraintFeasible)
         {
            for (int i = 0; i < intersections.length; i++)
            {
               double distance = intersections[i].distance(pointToMove);
               if (distance < distanceToBeInside)
               {
                  int j = 1 - i;
                  vector.sub(intersections[j], intersections[i]);
                  vector.normalize();
                  vector.scale(distanceToBeInside);
                  pointToMove.set(intersections[i]);
                  pointToMove.add(vector);
               }
            }
         }
         else
         {
            pointToMove.interpolate(intersections[0], intersections[1], 0.5);
         }
      }
      else
      {
         pointToMove.set(polygon.getClosestVertexCopy(line));

      }

//    else
//    {
//       StringBuilder stringBuilder = new StringBuilder();
//       stringBuilder.append("intersections == null\n");
//       stringBuilder.append("pointToMove = " + pointToMove + "\n");
//       stringBuilder.append("vector = " + vector + "\n");
//       stringBuilder.append("polygon = " + polygon + "\n");
//       stringBuilder.append("distanceToBeInside = " + distanceToBeInside);
//
//       throw new RuntimeException(stringBuilder.toString());
//    }
   }

   public static void projectOntoPolygonAndCheckDistance(FramePoint2d point, FrameConvexPolygon2d polygon, double epsilon)
   {
      ReferenceFrame originalReferenceFrame = point.getReferenceFrame();
      point.changeFrame(polygon.getReferenceFrame());
      FramePoint2d originalPoint = new FramePoint2d(point);
      polygon.orthogonalProjection(point);
      double distance = originalPoint.distance(point);
      if (distance > epsilon)
         throw new RuntimeException("point outside polygon by " + distance);
      point.changeFrame(originalReferenceFrame);
   }


   /**
    * arePointsInOrderColinear: This returns true if:
    * middle point is epsilon close to start or end
    *
    * Otherwise:
    * if the start is epsilon close to the end, return false
    *
    * if |(start to middle unit vector) dot with (start to end unit vector) - 1| > epsilon
    * return false
    * else return true
    *
    * @param startPoint Point2d
    * @param middlePoint Point2d
    * @param endPoint Point2d
    * @return boolean
    */
   public static boolean arePointsInOrderAndColinear(Point2d startPoint, Point2d middlePoint, Point2d endPoint, double epsilon)
   {
      double startToEndDistance = startPoint.distance(endPoint);
      double startToMiddleDistance = startPoint.distance(middlePoint);
      double middleToEndDistance = middlePoint.distance(endPoint);

      if (startToMiddleDistance < epsilon)
      {
         // middle very close to the start
         return true;
      }
      else if (middleToEndDistance < epsilon)
      {
         // middle very close to end
         return true;
      }
      else if (startToEndDistance < epsilon)
      {
         // start too close to end to fit middle in between
         return false;
      }
      else if ((startToMiddleDistance - startToEndDistance) > epsilon)
      {
         // middle farther from start than end point
         return false;
      }
      else if ((middleToEndDistance - startToEndDistance) > epsilon)
      {
         // middle farther from end than start point
         return false;
      }
      else
      {
         Vector2d startToEnd = new Vector2d(endPoint);
         startToEnd.sub(startPoint);
         startToEnd.normalize();

         Vector2d startToMiddle = new Vector2d(middlePoint);
         startToMiddle.sub(startPoint);
         startToMiddle.normalize();

         if (Math.abs(1.0 - startToMiddle.dot(startToEnd)) > epsilon)
            return false;
         else
            return true;
      }
   }

   /**
    * arePointsInOrderColinear: This returns true if:
    * middle point is epsilon close to start or end
    *
    * Otherwise:
    * if the start is epsilon close to the end, return false
    *
    * if |(start to middle unit vector) dot with (start to end unit vector) - 1| > epsilon
    * return false
    * else return true
    *
    * @param startPoint Point3d
    * @param middlePoint Point3d
    * @param endPoint Point3d
    * @return boolean
    */
   public static boolean arePointsInOrderAndColinear(Point3d startPoint, Point3d middlePoint, Point3d endPoint, double epsilon)
   {
      double startToEndDistance = startPoint.distance(endPoint);
      double startToMiddleDistance = startPoint.distance(middlePoint);
      double middleToEndDistance = middlePoint.distance(endPoint);

      if (startToMiddleDistance < epsilon)
      {
         // middle very close to the start
         return true;
      }
      else if (middleToEndDistance < epsilon)
      {
         // middle very close to end
         return true;
      }
      else if (startToEndDistance < epsilon)
      {
         // start too close to end to fit middle in between
         return false;
      }
      else if ((startToMiddleDistance - startToEndDistance) > epsilon)
      {
         // middle farther from start than end point
         return false;
      }
      else if ((middleToEndDistance - startToEndDistance) > epsilon)
      {
         // middle farther from end than start point
         return false;
      }
      else
      {
         Vector3d startToEnd = new Vector3d(endPoint);
         startToEnd.sub(startPoint);
         startToEnd.normalize();

         Vector3d startToMiddle = new Vector3d(middlePoint);
         startToMiddle.sub(startPoint);
         startToMiddle.normalize();

         if (Math.abs(1.0 - startToMiddle.dot(startToEnd)) > epsilon)
            return false;
         else
            return true;
      }
   }

   /**
    * Calculate an unknown side length of a fully defined 2D Triangle by the law of Cosine
    *
    * @param lengthSideA
    * @param lengthSideB
    * @param angleBetweenAAndB
    */
   public static double getUnknownTriangleSideLengthByLawOfCosine(double lengthSideA, double lengthSideB, double angleBetweenAAndB)
   {
      MathTools.checkIfInRange(lengthSideA, 0.0, Double.POSITIVE_INFINITY);
      MathTools.checkIfInRange(lengthSideB, 0.0, Double.POSITIVE_INFINITY);

      if (Math.abs(angleBetweenAAndB) > Math.PI)
      {
         throw new RuntimeException("angleBetweenAAndB " + angleBetweenAAndB + " does not define a triangle.");
      }

      return Math.sqrt(MathTools.square(lengthSideA) + MathTools.square(lengthSideB) - 2.0 * lengthSideA * lengthSideB * Math.cos(angleBetweenAAndB));
   }

   /**
    * Calculate an unknown angle of a fully defined 2D Triangle by the law of Cosine
    *
    * @param lengthNeighbourSideA
    * @param lengthNeighbourSideB
    * @param lengthOppositeSideC
    */
   public static double getUnknownTriangleAngleByLawOfCosine(double lengthNeighbourSideA, double lengthNeighbourSideB, double lengthOppositeSideC)
   {
      MathTools.checkIfInRange(lengthNeighbourSideA, 0.0, Double.POSITIVE_INFINITY);
      MathTools.checkIfInRange(lengthNeighbourSideB, 0.0, Double.POSITIVE_INFINITY);
      MathTools.checkIfInRange(lengthOppositeSideC, 0.0, Double.POSITIVE_INFINITY);

      if (GeometryTools.isFormingTriangle(lengthNeighbourSideA, lengthNeighbourSideB, lengthOppositeSideC))
      {
         return Math.acos((MathTools.square(lengthNeighbourSideA) + MathTools.square(lengthNeighbourSideB) - MathTools.square(lengthOppositeSideC))
                          / (2.0 * lengthNeighbourSideA * lengthNeighbourSideB));
      }
      else
      {
         throw new RuntimeException("Unable to build a Triangle of the given triangle sides a: "
                                    + lengthNeighbourSideA + " b: " + lengthNeighbourSideB + " c: " + lengthOppositeSideC);
      }
   }

   /**
    * Get a unknown cathetus (90-deg triangle one of the two shorter triangle sides, neighbouring the 90-degree angle) by Pythagoras law, a^2+b^2=c^2
    *
    * @param hypothenuseC the longest side
    * @param cathetusA one short side
    * @param cathetusB the other short side
    */
   public static double pythagorasGetCathetus(double hypothenuseC, double cathetusA)
   {
      MathTools.checkIfInRange(cathetusA, 0.0, hypothenuseC);

      return Math.sqrt(MathTools.square(hypothenuseC) - MathTools.square(cathetusA));
   }

   public static boolean isFormingTriangle(double lengthNeighbourSideA, double lengthNeighbourSideB, double lengthOppositeSideC)
   {
      double[] length_checker = new double[3];
      length_checker[0] = lengthNeighbourSideA;
      length_checker[1] = lengthNeighbourSideB;
      length_checker[2] = lengthOppositeSideC;
      Arrays.sort(length_checker);
      if (length_checker[0] + length_checker[1] <= length_checker[2])
         return false;
      else
         return true;
   }

   /**
    * Get the hypothenuse c (90-degree triangle longest triangle length, opposite to the 90-degree angle) by Pythagoras law, a^2+b^2=c^2
    *
    * @param cathetusA one short side
    * @param cathetusB the other short side
    * @param hypothenuseC the longest side
    */
   public static double pythagorasGetHypothenuse(double cathetusA, double cathetusB)
   {
      return Math.hypot(cathetusA, cathetusB);
   }

   // Needs to be reimplemented with EJML and without generating garbage.
   /*
    * Projects point p onto the plane defined by p1, p2, and p3
    */
//   public static Vector3d getProjectionOntoPlane(Vector3d p1, Vector3d p2, Vector3d p3, Vector3d p)
//   {
//      Vector3d p2_minus_p1 = new Vector3d(p2);
//      p2_minus_p1.sub(p1);
//
//      Vector3d p3_minus_p1 = new Vector3d(p3);
//      p3_minus_p1.sub(p1);
//
//      Vector3d n = new Vector3d(p2_minus_p1);
//      n.cross(n, p3_minus_p1);
//      n.normalize();
//
//      // convert to matrix so the following calculation is cleaner
//      Matrix P = MatrixTools.vector3dToMatrix(p);
//      Matrix P1 = MatrixTools.vector3dToMatrix(p1);
//      Matrix N = MatrixTools.vector3dToMatrix(n);
//
//      double scale = (((P1.minus(P)).transpose()).times(N)).get(0, 0);
//      Matrix Perp = N.times(scale);
//
//      Matrix Proj = P.plus(Perp);
//
//      return new Vector3d(Proj.get(0, 0), Proj.get(1, 0), Proj.get(2, 0));
//   }

   public static double minimumDistance(FramePoint testPoint, List<FramePoint> points)
   {
      double ret = Double.POSITIVE_INFINITY;
      for (FramePoint point : points)
      {
         double distanceSquared = testPoint.distanceSquared(point);
         if (distanceSquared < ret)
            ret = distanceSquared;
      }

      return Math.sqrt(ret);
   }

   public static ArrayList<FramePoint2d> changeFrameToZUpAndProjectToXYPlane(ReferenceFrame zUpFrame, List<FramePoint> points)
   {
      ArrayList<FramePoint2d> ret = new ArrayList<FramePoint2d>(points.size());

      for (int i = 0; i < points.size(); i++)
      {
         FramePoint framePoint = new FramePoint(points.get(i));
         framePoint.changeFrame(zUpFrame);

         ret.add(framePoint.toFramePoint2d());
      }

      return ret;
   }

   public static ArrayList<FramePoint2d> projectToXYPlane(List<FramePoint> points)
   {
      ArrayList<FramePoint2d> ret = new ArrayList<FramePoint2d>(points.size());
      for (int i = 0; i < points.size(); i++)
      {
         FramePoint point3d = points.get(i);
         ret.add(new FramePoint2d(point3d.getReferenceFrame(), point3d.getX(), point3d.getY()));
      }

      return ret;
   }

   /**
    * Finds the minimum distance between two convex polygons
    * Taken from http://cygnus-x1.cs.duke.edu/~edels/Papers/1985-J-02-ComputingExtremeDistances.pdf
    * @return Two points, one from each polygon, between which is the minimum distance between the two polygons
    */
   public static Point2d[] computeMinimumDistancePoints(ConvexPolygon2d polygon1, ConvexPolygon2d polygon2, double epsilon)
   {
      // TODO Do something more clever than actually computing the intersection there!
      if (ConvexPolygonTools.computeIntersectionOfPolygons(polygon1, polygon2, new ConvexPolygon2d()))
      {
         throw new RuntimeException("Cannot compute minimum distance between intersecting polygons.");
      }

      if ((polygon1.getNumberOfVertices() < 3) || (polygon2.getNumberOfVertices() < 3))
      {
         throw new RuntimeException("Polygon inputs are degenerate.");
      }

      int[] v1Tangents = findStartAndEndTangents(polygon2.getVertex(0), polygon1, epsilon);
      int[] v2Tangents = findStartAndEndTangents(polygon1.getVertex(0), polygon2, epsilon);

      int v1Start = v1Tangents[0];
      int v1End = v1Tangents[1];
      int v2Start = v2Tangents[0];
      int v2End = v2Tangents[1];

      int[] updatedIndices = binaryElimination(polygon1, polygon2, v1Start, v1End, v2Start, v2End, epsilon);
      v1Start = updatedIndices[0];
      v1End = updatedIndices[1];
      v2Start = updatedIndices[2];
      v2End = updatedIndices[3];

      return getClosestPointsFromRemainingEdgesAndVertices(polygon1, polygon2, v1Start, v1End, v2Start, v2End);
   }

   public static Point2d[] computeMinimumDistancePoints(ConvexPolygon2d polygon1, ConvexPolygon2d polygon2)
   {
      return computeMinimumDistancePoints(polygon1, polygon2, .01);
   }


   // TODO potentially implement [Chazelle and Dobkin] to get logarithmic running time for computeMinimumDistancePoints (though it would actually be log^2 in current
   // implementation, since binaryElimination, which has is O(log(n)) uses this method in each loop)

   /**
    * Finds the indices of the vertices of the polygon that form tangent lines to the polygon with the parameter point
    * @return The desired indices, ordered such that they form a range that includes all vertices visible from the parameter point; if there are more than two
    *          only returns the two necessary to specify this range
    */
   private static int[] findStartAndEndTangents(Point2d point, ConvexPolygon2d polygon, double epsilon)
   {
      int tangentIndex1;
      int tangentIndex2;

      int vIndex = 0;

      while (!pointMakesTangentToPolygon(polygon, point, vIndex, epsilon))
      {
         vIndex++;
         vIndex %= polygon.getNumberOfVertices();
      }

      tangentIndex1 = vIndex;
      Vector2d tangent1 = new Vector2d(polygon.getVertex(tangentIndex1).getX() - point.getX(),polygon.getVertex(tangentIndex1).getY() - point.getY());

      vIndex++;
      vIndex %= polygon.getNumberOfVertices();

      while (!pointMakesTangentToPolygon(polygon, point, vIndex, epsilon))
      {
         vIndex++;
         vIndex %= polygon.getNumberOfVertices();
      }

      tangentIndex2 = vIndex;
      Vector2d tangent2 = new Vector2d(polygon.getVertex(tangentIndex2).getX() - point.getX(), polygon.getVertex(tangentIndex2).getY() - point.getY());

      if (getAngleFromFirstToSecondVector(tangent1, tangent2) > 0)
      {
         return new int[] {tangentIndex1, tangentIndex2};
      }

      return new int[] {tangentIndex2, tangentIndex1};
   }

   /**
    * Uses the fact that if a line passes through a vertex of a convex polygon, the angles to the adjacent edges are going to be in opposite directions
    * @return Whether or not the line including the point and vertex is tangent to the polygon
    */
   private static boolean pointMakesTangentToPolygon(ConvexPolygon2d polygon, Point2d point, int vertexIndex, double epsilon)
   {
      Point2d vertex = polygon.getVertex(vertexIndex);
      Point2d previous = polygon.getPreviousVertex(vertexIndex);
      Point2d next = polygon.getNextVertex(vertexIndex);

      Vector2d base = new Vector2d(point.getX() - vertex.getX(), point.getY() - vertex.getY());
      Vector2d first = new Vector2d(previous.getX() - vertex.getX(), previous.getY() - vertex.getY());
      Vector2d second = new Vector2d(next.getX() - vertex.getX(), next.getY() - vertex.getY());
      double firstAngle = getAngleFromFirstToSecondVector(base, first);
      double secondAngle = getAngleFromFirstToSecondVector(base, second);

      if (firstAngle * secondAngle >= 0)
      {    // if both angles have the same sign, the line does not pass through the polygon
         return true;
      }

      if (MathTools.epsilonEquals(firstAngle, 0, epsilon) || MathTools.epsilonEquals(secondAngle, 0, epsilon))
      {    // if either angle is close to 0, assume floating point arithmetic error
         return true;
      }

      return false;
   }

   /**
    * Checks if index is within range; if low is greater than high, this implies a modularly cyclical range
    * @return True if the index is between low and high
    */
   private static boolean isInRange(int index, int low, int high)
   {
      if ((low <= index) && (index <= high))
      {
         return true;
      }

      if ((high < low) && ((index >= low) || (index <= high)))
      {
         return true;
      }

      return false;
   }

   /**
    * Eliminates vertices and return a range for each polygon, each of which comprises of at most two vertices
    * @return Array with the low and high end of each range, respectively
    */
   private static int[] binaryElimination(ConvexPolygon2d polygon1, ConvexPolygon2d polygon2, int v1Start, int v1End, int v2Start, int v2End, double epsilon)
   {
      Point2d v1Median;
      Point2d v2Median;

      int numberOfVertices1 = polygon1.getNumberOfVertices();
      int numberOfVertices2 = polygon2.getNumberOfVertices();

      while (((numberOfVertices1 + v1End - v1Start) % numberOfVertices1 + 1 > 2) || ((numberOfVertices2 + v2End - v2Start) % numberOfVertices2 + 1 > 2))
      {
         int v1MedianIndex = (v1Start <= v1End) ? (v1End + v1Start + 1) / 2 : ((v1End + v1Start + 1 + numberOfVertices1) / 2) % numberOfVertices1;
         int v2MedianIndex = (v2Start <= v2End) ? (v2End + v2Start) / 2 : ((v2End + v2Start + numberOfVertices2) / 2) % numberOfVertices2;
         v1Median = polygon1.getVertex(v1MedianIndex);
         v2Median = polygon2.getVertex(v2MedianIndex);

         Vector2d m = new Vector2d(v2Median.getX() - v1Median.getX(), v2Median.getY() - v1Median.getY());
         Vector2d mReversed = new Vector2d(v1Median.getX() - v2Median.getX(), v1Median.getY() - v2Median.getY());

         int edge1AStart = ((v1MedianIndex + numberOfVertices1 - 1) % numberOfVertices1);
         int edge1BEnd = (v1MedianIndex + 1) % numberOfVertices1;
         int edge2BStart = ((v2MedianIndex + numberOfVertices2 - 1) % numberOfVertices2);
         int edge2AEnd = (v2MedianIndex + 1) % numberOfVertices2;
         Vector2d edge1A = new Vector2d(polygon1.getVertex(edge1AStart).getX() - v1Median.getX(), polygon1.getVertex(edge1AStart).getY() - v1Median.getY());
         Vector2d edge1B = new Vector2d(polygon1.getVertex(edge1BEnd).getX() - v1Median.getX(), polygon1.getVertex(edge1BEnd).getY() - v1Median.getY());
         Vector2d edge2A = new Vector2d(polygon2.getVertex(edge2AEnd).getX() - v2Median.getX(), polygon2.getVertex(edge2AEnd).getY() - v2Median.getY());
         Vector2d edge2B = new Vector2d(polygon2.getVertex(edge2BStart).getX() - v2Median.getX(), polygon2.getVertex(edge2BStart).getY() - v2Median.getY());

         // see diagram 3.2 in [Edelsbrunner]
         double angle1A = getAngleFromFirstToSecondVector(m, edge1A); // A' in diagram
         double angle1B = getAngleFromFirstToSecondVector(edge1B, m); // A'' in diagram
         double angle2A = getAngleFromFirstToSecondVector(edge2A, mReversed); // B' in diagram
         double angle2B = getAngleFromFirstToSecondVector(mReversed, edge2B); // B'' in diagram

         int[] range1 = findStartAndEndTangents(v2Median, polygon1, epsilon);
         int[] range2 = findStartAndEndTangents(v1Median, polygon2, epsilon);

         angle1A = MathTools.epsilonEquals(angle1A, 0, .01) ? 0 : angle1A;
         angle1B = MathTools.epsilonEquals(angle1B, 0, .01) ? 0 : angle1B;
         angle2A = MathTools.epsilonEquals(angle2A, 0, .01) ? 0 : angle2A;
         angle2B = MathTools.epsilonEquals(angle2B, 0, .01) ? 0 : angle2B;

         angle1A += ((angle1A < 0) && isInRange(v1MedianIndex, range1[0], range1[1])) ? 2 * Math.PI : 0;
         angle1B += ((angle1B < 0) && isInRange(v1MedianIndex, range1[0], range1[1])) ? 2 * Math.PI : 0;
         angle2A += ((angle2A < 0) && isInRange(v2MedianIndex, range2[0], range2[1])) ? 2 * Math.PI : 0;
         angle2B += ((angle2B < 0) && isInRange(v2MedianIndex, range2[0], range2[1])) ? 2 * Math.PI : 0;

         angle1A += ((angle1A < 0) && (angle1B < 0) && (angle1A < angle1B)) ? 2 * Math.PI : 0;
         angle1B += ((angle1A < 0) && (angle1B < 0) && (angle1B < angle1A)) ? 2 * Math.PI : 0;
         angle2A += ((angle2A < 0) && (angle2B < 0) && (angle2A < angle2B)) ? 2 * Math.PI : 0;
         angle2B += ((angle2A < 0) && (angle2B < 0) && (angle2B < angle2A)) ? 2 * Math.PI : 0;

         int[] updatedIndices;

         if ((v1Start == v1End) || (v2Start == v2End))
         {
            updatedIndices = binaryEliminationCase1(angle1A, angle1B, angle2A, angle2B, v1Start, v1MedianIndex, v1End, v2Start, v2MedianIndex, v2End, polygon1, polygon2);
            v1Start = updatedIndices[0];
            v1End = updatedIndices[1];
            v2Start = updatedIndices[2];
            v2End = updatedIndices[3];
         }
         else if ((v1End - v1Start + numberOfVertices1) % numberOfVertices1 == 1)
         {
            updatedIndices = binaryEliminationCase2(angle1A, angle1B, angle2A, angle2B, v1Start, v1MedianIndex, v1End, v2Start, v2MedianIndex, v2End, polygon1, polygon2);
            v1Start = updatedIndices[0];
            v1End = updatedIndices[1];
            v2Start = updatedIndices[2];
            v2End = updatedIndices[3];
         }
         else if ((v2End - v2Start + numberOfVertices2) % numberOfVertices2 == 1)
         {
            updatedIndices = binaryEliminationCase2(angle2A, angle2B, angle1A, angle1B, v2End, v2MedianIndex, v2Start, v1End, v1MedianIndex, v1Start, polygon1, polygon2);
            v2End = updatedIndices[0];
            v2Start = updatedIndices[1];
            v1End = updatedIndices[2];
            v1Start = updatedIndices[3];
         }
         else
         {
            updatedIndices = binaryEliminationCase3(angle1A, angle1B, angle2A, angle2B, v1Start, v1MedianIndex, v1End, v2Start, v2MedianIndex, v2End);
            v1Start = updatedIndices[0];
            v1End = updatedIndices[1];
            v2Start = updatedIndices[2];
            v2End = updatedIndices[3];
         }
      }

      return new int[] { v1Start, v1End, v2Start, v2End };
   }

   /**
    * Binary elimination helper method called if one range has a size of exactly one
    * @return Array with the low and high end of each range, respectively
    */
   private static int[] binaryEliminationCase1(double angle1A, double angle1B, double angle2A, double angle2B, int v1Start, int v1MedianIndex, int v1End,
           int v2Start, int v2MedianIndex, int v2End, ConvexPolygon2d polygon1, ConvexPolygon2d polygon2)
   {
      if (v1Start == v1End)
      {    // v1 contains only 1 viable vertex
         if (angle2A >= Math.PI / 2)
         {
            v2End = v2MedianIndex;
         }

         if (angle2B >= Math.PI / 2)
         {
            v2Start = v2MedianIndex;
         }
      }
      else if (v2Start == v2End)
      {
         if (angle1A >= Math.PI / 2)
         {
            v1Start = v1MedianIndex;
         }

         if (angle1B >= Math.PI / 2)
         {
            v1End = v1MedianIndex;
         }
      }

      return new int[] {v1Start, v1End, v2Start, v2End};
   }

   /**
    * Binary elimination helper method called if one range has a size of exactly two
    * @return Array with the low and high end of each range, respectively
    */
   private static int[] binaryEliminationCase2(double angle1A, double angle1B, double angle2A, double angle2B, int v1Start, int v1MedianIndex, int v1End,
           int v2Start, int v2MedianIndex, int v2End, ConvexPolygon2d polygon1, ConvexPolygon2d polygon2)
   {
      if (angle1A > 0)
      {
         if (angle1A + angle2A >= Math.PI)
         {
            if (angle1A >= Math.PI / 2)
            {
               v1Start = v1End;
            }

            if (angle2A >= Math.PI / 2)
            {
               v2End = v2MedianIndex;
            }
         }

         if (angle2B >= Math.PI / 2)
         {
            v2Start = v2MedianIndex;
         }

         if ((angle1A < angle2B) && (angle2B < Math.PI / 2))
         {
            Point2d proj = getOrthogonalProjectionOnLine(polygon2.getVertex(v2MedianIndex), polygon1.getVertex(v1Start), polygon1.getVertex(v1End));
            LineSegment2d p = new LineSegment2d(polygon1.getVertex(v1Start), polygon1.getVertex(v1End));
            if (p.isBetweenEndpoints(proj, 0))
            {
               v2Start = v2MedianIndex;
            }
            else
            {
               v1End = v1Start;
            }
         }
      }
      else
      {
         v1End = v1Start;

         if (angle2A >= Math.PI)
         {
            v2End = v2MedianIndex;
         }

         if (angle2B >= Math.PI)
         {
            v2Start = v2MedianIndex;
         }
      }

      return new int[] {v1Start, v1End, v2Start, v2End};
   }


   /**
    * Binary Elimination helper method called if both ranges have size greater than two
    * @return Array with the low and high end of each range, respectively
    */
   private static int[] binaryEliminationCase3(double angle1A, double angle1B, double angle2A, double angle2B, int v1Start, int v1MedianIndex, int v1End,
           int v2Start, int v2MedianIndex, int v2End)
   {
      if ((angle1A > 0) && (angle1B > 0) && (angle2A > 0) && (angle2B > 0))
      {
         if (angle1A + angle2A > Math.PI)
         {
            if (angle1A >= Math.PI / 2)
            {
               v1Start = v1MedianIndex;
            }

            if (angle2A >= Math.PI / 2)
            {
               v2End = v2MedianIndex;
            }
         }

         if (angle1B + angle2B > Math.PI)
         {
            if (angle1B >= Math.PI / 2)
            {
               v1End = v1MedianIndex;
            }

            if (angle2B >= Math.PI / 2)
            {
               v2Start = v2MedianIndex;
            }
         }
      }

      if (angle1A <= 0)
      {
         v1End = v1MedianIndex;
      }

      if (angle1B <= 0)
      {
         v1Start = v1MedianIndex;
      }

      if (angle2A <= 0)
      {
         v2Start = v2MedianIndex;
      }

      if (angle2B <= 0)
      {
         v2End = v2MedianIndex;
      }

      return new int[] {v1Start, v1End, v2Start, v2End};
   }


   /**
    * Takes in two ranges each of which are of size at most two and returns the two points on each respective polygon which are closest to one another
    */
   private static Point2d[] getClosestPointsFromRemainingEdgesAndVertices(ConvexPolygon2d polygon1, ConvexPolygon2d polygon2, int v1Start, int v1End, int v2Start, int v2End)
   {
      if ((v1Start == v1End) && (v2Start == v2End))
      {
         return new Point2d[] {polygon1.getVertex(v1Start), polygon2.getVertex(v2Start)};
      }

      else if (v1Start == v1End)
      {
         return finalPhasePointAndEdge(polygon2.getVertex(v2Start), polygon2.getVertex(v2End), polygon1.getVertex(v1Start));
      }

      else if (v2Start == v2End)
      {
         Point2d[] reverseOutput = finalPhasePointAndEdge(polygon1.getVertex(v1Start), polygon1.getVertex(v1End), polygon2.getVertex(v2Start));

         return new Point2d[] {reverseOutput[1], reverseOutput[0]};    // switch order of output so that points are returned in the order that their polygons were inputed
      }

      return finalPhaseTwoEdges(polygon1.getVertex(v1Start), polygon1.getVertex(v1End), polygon2.getVertex(v2Start), polygon2.getVertex(v2End));
   }

   /**
    * Final phase helper method called if each range has size of exactly two
    * @return The two points on each respective polygon which are closest to one another
    */
   private static Point2d[] finalPhaseTwoEdges(Point2d edgePoint1A, Point2d edgePoint1B, Point2d edgePoint2A, Point2d edgePoint2B)
   {
      LineSegment2d edge1 = new LineSegment2d(edgePoint1A, edgePoint1B);
      LineSegment2d edge2 = new LineSegment2d(edgePoint2A, edgePoint2B);
      Point2d proj1AOnto2 = getOrthogonalProjectionOnLine(edgePoint1A, edgePoint2A, edgePoint2B);
      Point2d proj1BOnto2 = getOrthogonalProjectionOnLine(edgePoint1B, edgePoint2A, edgePoint2B);
      Point2d proj2AOnto1 = getOrthogonalProjectionOnLine(edgePoint2A, edgePoint1A, edgePoint1B);
      Point2d proj2BOnto1 = getOrthogonalProjectionOnLine(edgePoint2B, edgePoint1A, edgePoint1B);

      Point2d[][] possiblePointPairsWithProj = new Point2d[4][2];
      Point2d[][] possiblePointPairsWithoutProj = new Point2d[4][2];
      double[] possibleDistancesWithProj = new double[4];
      double[] possibleDistancesWithoutProj = new double[4];

      possiblePointPairsWithProj[0] = edge2.isBetweenEndpoints(proj1AOnto2, 0) ? new Point2d[] {edgePoint1A, proj1AOnto2} : null;
      possiblePointPairsWithProj[1] = edge2.isBetweenEndpoints(proj1BOnto2, 0) ? new Point2d[] {edgePoint1B, proj1BOnto2} : null;
      possiblePointPairsWithProj[2] = edge1.isBetweenEndpoints(proj2AOnto1, 0) ? new Point2d[] {proj2AOnto1, edgePoint2A} : null;
      possiblePointPairsWithProj[3] = edge1.isBetweenEndpoints(proj2BOnto1, 0) ? new Point2d[] {proj2BOnto1, edgePoint2B} : null;

      possiblePointPairsWithoutProj[0] = new Point2d[] {edgePoint1A, edgePoint2A};
      possiblePointPairsWithoutProj[1] = new Point2d[] {edgePoint1A, edgePoint2B};
      possiblePointPairsWithoutProj[2] = new Point2d[] {edgePoint1B, edgePoint2A};
      possiblePointPairsWithoutProj[3] = new Point2d[] {edgePoint1B, edgePoint2B};

      for (int i = 0; i < 4; i++)
      {
         possibleDistancesWithProj[i] = (possiblePointPairsWithProj[i] == null)
                                        ? Double.MAX_VALUE : possiblePointPairsWithProj[i][0].distance(possiblePointPairsWithProj[i][1]);
         possibleDistancesWithoutProj[i] = possiblePointPairsWithoutProj[i][0].distance(possiblePointPairsWithoutProj[i][1]);
      }

      if (possibleDistancesWithProj[indexOfMin(possibleDistancesWithProj)] != Double.MAX_VALUE)
      {
         return possiblePointPairsWithProj[indexOfMin(possibleDistancesWithProj)];
      }

      return possiblePointPairsWithoutProj[indexOfMin(possibleDistancesWithoutProj)];
   }

   /**
    * @return Index of the minimum element in an array of doubles
    */
   private static int indexOfMin(double[] d)
   {
      if ((d == null) || (d.length == 0))
      {
         throw new RuntimeException("Cannot find minimum of empty or null array.");
      }

      int minIndex = 0;
      double minValue = d[minIndex];
      int searchIndex = 1;
      while (searchIndex < d.length)
      {
         if (d[searchIndex] < minValue)
         {
            minIndex = searchIndex;
            minValue = d[searchIndex];
         }

         searchIndex++;
      }

      return minIndex;
   }

   /**
    * Final phase helper method called if one range has a size of exactly one
    * @return The two points on each respective polygon which are closest to one another
    */
   private static Point2d[] finalPhasePointAndEdge(Point2d edgePoint1, Point2d edgePoint2, Point2d lonePoint)
   {
      Point2d proj = getOrthogonalProjectionOnLine(lonePoint, edgePoint1, edgePoint2);
      LineSegment2d p = new LineSegment2d(edgePoint1, edgePoint2);
      if (p.isBetweenEndpoints(proj, 0))
      {
         return new Point2d[] {lonePoint, proj};
      }
      else
      {
         return new Point2d[] {lonePoint, (lonePoint.distance(edgePoint1) < lonePoint.distance(edgePoint2)) ? edgePoint1 : edgePoint2};
      }
   }

   /**
    * from http://softsurfer.com/Archive/algorithm_0111/algorithm_0111.htm#Pseudo-Code:%20Clip%20Segment-Polygon
    * Input: a 2D segment S from point P0 to point P1
    * a 2D convex polygon W with n vertices V0,...,Vn-1,Vn=V0
    */
   public static boolean doesSegmentIntersectConvexPolygon2D(Point2d P0, Point2d P1, ConvexPolygon2d convexPolygon2d)
   {
      // if segment is a single point
      if (P0.equals(P1))
      {
         return convexPolygon2d.isPointInside(P0);
      }

      // if either point is inside polygon
      if (convexPolygon2d.isPointInside(P0, .0001) || convexPolygon2d.isPointInside(P1, .0001))
         return true;

      // if either point touches the polygon
      if (convexPolygon2d.pointIsOnPerimeter(P0) || convexPolygon2d.pointIsOnPerimeter(P1))
         return true;

      return doesSegmentPassCompletelyThroughPolygon(P0, P1, convexPolygon2d);
   }

   private static boolean doesSegmentPassCompletelyThroughPolygon(Point2d P0, Point2d P1, ConvexPolygon2d convexPolygon2d)
   {
      // Initialize:
      double tE = 0.0;    // for the maximum entering segment parameter;
      double tL = 1.0;    // for the minimum leaving segment parameter;

      // segment direction vector
      Vector2d dS = new Vector2d(P1);
      dS.sub(P0);

      if (DEBUG)
      {
         System.out.println("dS = " + dS);
      }

      int numberOfVertices = convexPolygon2d.getNumberOfVertices();
      if (DEBUG)
      {
         System.out.println("ccwPoints = ");

         for (int i = 0; i < numberOfVertices; i++)
         {
            System.out.println(convexPolygon2d.getVertexCCW(i));
         }
      }

      for (int i = 0; i < numberOfVertices; i++)
      {
         // edge vertices
         Point2d V0 = new Point2d(convexPolygon2d.getVertexCCW(i));
         if (DEBUG)
         {
            System.out.println("V0 = " + V0);
         }

         Point2d V1 = new Point2d(convexPolygon2d.getNextVertexCCW(i));
         if (DEBUG)
         {
            System.out.println("V1 = " + V1);
         }

         // edge vector
         Vector2d V0toV1 = new Vector2d(V1);
         V0toV1.sub(V0);

         if (DEBUG)
         {
            System.out.println("V0toV1 = " + V0toV1);
         }

         // outward normal of the edge
         Vector2d ni = new Vector2d(V0toV1.getY(), -V0toV1.getX());
         if (DEBUG)
         {
            System.out.println("ni = " + ni);
         }

         Vector2d P0toVi = new Vector2d(P0);
         P0toVi.sub(V0);

         if (DEBUG)
         {
            System.out.println("P0toVi = " + P0toVi);
         }

         double N = -P0toVi.dot(ni);
         if (DEBUG)
         {
            System.out.println("N = " + N);
         }

         double D = dS.dot(ni);
         if (DEBUG)
         {
            System.out.println("D = " + D);
         }

         if (D == 0)
         {
            // S is parallel to the edge ei

            if (N < 0)
            {
               // then P0 is outside the edge ei
               return false;    // since S cannot intersect W;
            }
            else
            {
               // S cannot enter or leave W across edge ei
               // ignore edge ei and process the next edge
               continue;
            }
         }

         double t = N / D;
         if (DEBUG)
         {
            System.out.println("t = " + t);
         }

         if (D < 0)
         {
            // then segment S is entering W across edge ei
            tE = Math.max(tE, t);

            if (tE > tL)
            {
               // then segment S enters W after leaving
               return false;    // since S cannot intersect W
            }
         }
         else if (D > 0)
         {
            // then segment S is leaving W across edge ei
            tL = Math.min(tL, t);

            if (tL < tE)
            {
               // then segment S leaves W before entering
               return false;    // since S cannot intersect W
            }
         }
      }

      // Output: [Note: to get here, one must have tE <= tL]
      // there is a valid intersection of S with W
      // from the entering point: P(tE) = P0 + tE * dS
      // to the leaving point:    P(tL) = P0 + tL * dS
      return true;
   }

   public static double cross(Vector2d firstVector, Vector2d secondVector)
   {
      return firstVector.getX() * secondVector.getY() - firstVector.getY() * secondVector.getX();
   }
   
   public static boolean isZero(Tuple3d tuple, double epsilon)
   {
      boolean isZero = true;
      isZero &= MathTools.epsilonEquals(tuple.getX(), 0.0, epsilon);
      isZero &= MathTools.epsilonEquals(tuple.getY(), 0.0, epsilon);
      isZero &= MathTools.epsilonEquals(tuple.getZ(), 0.0, epsilon);
      return isZero;
   }
   
   public static boolean isZero(Tuple2d tuple, double epsilon)
   {
      boolean isZero = true;
      isZero &= MathTools.epsilonEquals(tuple.getX(), 0.0, epsilon);
      isZero &= MathTools.epsilonEquals(tuple.getY(), 0.0, epsilon);
      return isZero;
   }
}
