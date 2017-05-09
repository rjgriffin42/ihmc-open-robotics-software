package us.ihmc.robotics.geometry;

import us.ihmc.euclid.geometry.tools.EuclidGeometryTools;
import us.ihmc.euclid.interfaces.GeometryObject;
import us.ihmc.euclid.transform.interfaces.Transform;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple2D.Vector2D;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Point2DReadOnly;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DBasics;
import us.ihmc.euclid.tuple2D.interfaces.Vector2DReadOnly;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.robotSide.RobotSide;

/**
 * Represents an infinitely-long 2D line defined by a 2D point and a 2D unit-vector.
 */
public class Line2d implements GeometryObject<Line2d>
{
   private final static double minAllowableVectorPart = Math.sqrt(Double.MIN_NORMAL);

   /** Coordinates of a point located on this line. */
   private final Point2D point = new Point2D();
   /** Normalized direction of this line. */
   private final Vector2D direction = new Vector2D();

   private boolean hasPointBeenSet = false;
   private boolean hasDirectionBeenSet = false;

   /**
    * Default constructor that initializes both {@link #point} and {@link #direction} to zero. This
    * point and vector have to be set to valid values to make this line usable.
    */
   public Line2d()
   {
      hasPointBeenSet = false;
      hasDirectionBeenSet = false;
   }

   /**
    * Creates a new line 2D and initializes it to {@code other}.
    * 
    * @param other the other line used to initialize this line. Not modified.
    */
   public Line2d(Line2d other)
   {
      set(other);
   }

   /**
    * Initializes this line to be passing through the given point, with the vector as the direction.
    * 
    * @param pointOnLine point on this line. Not modified.
    * @param lineDirection direction of this line. Not modified.
    * @throws RuntimeException if the new direction is unreasonably small.
    */
   public Line2d(Point2DReadOnly pointOnLine, Vector2DReadOnly lineDirection)
   {
      set(pointOnLine, lineDirection);
   }

   /**
    * Initializes this line to be passing through the two given points.
    * 
    * @param firstPointOnLine first point on this line. Not modified.
    * @param secondPointOnLine second point on this line. Not modified.
    * @throws RuntimeException if the new direction is unreasonably small.
    * @throws RuntimeException if the two given points are exactly equal.
    */
   public Line2d(Point2DReadOnly firstPointOnLine, Point2DReadOnly secondPointOnLine)
   {
      set(firstPointOnLine, secondPointOnLine);
   }

   /**
    * Initializes this line to be passing through the given point, with the vector as the direction.
    * 
    * @param pointOnLineX the x-coordinate of a point on this line.
    * @param pointOnLineY the y-coordinate of a point on this line.
    * @param lineDirectionX the x-component of the direction of this line.
    * @param lineDirectionY the y-component of the direction of this line.
    * @throws RuntimeException if the new direction is unreasonably small.
    */
   public Line2d(double pointOnLineX, double pointOnLineY, double lineDirectionX, double lineDirectionY)
   {
      set(pointOnLineX, pointOnLineY, lineDirectionX, lineDirectionY);
   }

   /**
    * Changes the point through which this line has to go.
    * 
    * @param pointX the new x-coordinate of the point on this line.
    * @param pointY the new y-coordinate of the point on this line.
    */
   public void setPoint(double pointOnLineX, double pointOnLineY)
   {
      point.set(pointOnLineX, pointOnLineY);
      hasPointBeenSet = true;
   }

   /**
    * Changes the point through which this line has to go.
    * 
    * @param pointOnLine new point on this line. Not modified.
    */
   public void setPoint(Point2DReadOnly pointOnLine)
   {
      setPoint(pointOnLine.getX(), pointOnLine.getY());
   }

   /**
    * Changes the direction of this line by setting it to the normalized value of the given vector.
    * 
    * @param directionX the new x-component of the direction of this line.
    * @param directionY the new y-component of the direction of this line.
    * @throws RuntimeException if the new direction is unreasonably small.
    */
   public void setDirection(double lineDirectionX, double lineDirectionY)
   {
      direction.set(lineDirectionX, lineDirectionY);
      checkReasonableVector(direction);
      direction.normalize();
      hasDirectionBeenSet = true;
   }

   /**
    * Changes the direction of this line by setting it to the normalized value of the given vector.
    * 
    * @param lineDirection new direction of this line. Not modified.
    * @throws RuntimeException if the new direction is unreasonably small.
    */
   public void setDirection(Vector2DReadOnly lineDirection)
   {
      setDirection(lineDirection.getX(), lineDirection.getY());
   }

   /**
    * Redefines this line with a new point and a new direction vector.
    * 
    * @param pointOnLine new point on this line. Not modified.
    * @param lineDirection new direction of this line. Not modified.
    * @throws RuntimeException if the new direction is unreasonably small.
    */
   public void set(Point2DReadOnly pointOnLine, Vector2DReadOnly lineDirection)
   {
      setPoint(pointOnLine);
      setDirection(lineDirection);
   }

   /**
    * Redefines this line with a new point and a new direction vector.
    * 
    * @param pointOnLineX the new x-coordinate of the point on this line.
    * @param pointOnLineY the new y-coordinate of the point on this line.
    * @param lineDirectionX the new x-component of the direction of this line.
    * @param lineDirectionY the new y-component of the direction of this line.
    * @throws RuntimeException if the new direction is unreasonably small.
    */
   public void set(double pointOnLineX, double pointOnLineY, double lineDirectionX, double lineDirectionY)
   {
      setPoint(pointOnLineX, pointOnLineY);
      setDirection(lineDirectionX, lineDirectionY);
   }

   /**
    * Redefines this line such that it goes through the two given points.
    * 
    * @param firstPointOnLine first point on this line. Not modified.
    * @param secondPointOnLine second point on this line. Not modified.
    * @throws RuntimeException if the new direction is unreasonably small.
    * @throws RuntimeException if the two given points are exactly equal.
    */
   public void set(Point2DReadOnly firstPointOnLine, Point2DReadOnly secondPointOnLine)
   {
      checkDistinctPoints(firstPointOnLine, secondPointOnLine);
      setPoint(firstPointOnLine);
      setDirection(secondPointOnLine.getX() - firstPointOnLine.getX(), secondPointOnLine.getY() - firstPointOnLine.getY());
   }

   /**
    * Redefines this line such that it goes through the two given points.
    * 
    * @param twoPointsOnLine a two-element array containing in order the first point and second
    *           point this line line is to go through. Not modified.
    * @throws RuntimeException if the new direction is unreasonably small.
    * @throws RuntimeException if the two given points are exactly equal.
    * @throws IllegalArgumentException if the given array has a length different than 2.
    */
   public void set(Point2DReadOnly[] twoPointsOnLine)
   {
      if (twoPointsOnLine.length != 2)
         throw new IllegalArgumentException("Length of input array is not correct. Length = " + twoPointsOnLine.length + ", expected an array of two elements");
      set(twoPointsOnLine[0], twoPointsOnLine[1]);
   }

   /**
    * Sets this line to be the same as the given line.
    * 
    * @param other the other line to copy. Not modified.
    */
   @Override
   public void set(Line2d other)
   {
      set(other.getPoint(), other.getDirection());
   }

   /**
    * Sets the point and vector of this line to zero. After calling this method, this line becomes
    * invalid. A new valid point and valid vector will have to be set so this line is again usable.
    */
   @Override
   public void setToZero()
   {
      point.setToZero();
      direction.setToZero();
      hasPointBeenSet = false;
      hasDirectionBeenSet = false;
   }

   /**
    * Sets the point and vector of this line to {@link Double#NaN}. After calling this method, this
    * line becomes invalid. A new valid point and valid vector will have to be set so this line is
    * again usable.
    */
   @Override
   public void setToNaN()
   {
      point.setToNaN();
      direction.setToNaN();
   }

   /**
    * Tests if this line contains {@link Double#NaN}.
    * 
    * @return {@code true} if {@link #point} and/or {@link #direction} contains {@link Double#NaN},
    *         {@code false} otherwise.
    */
   @Override
   public boolean containsNaN()
   {
      return point.containsNaN() || direction.containsNaN();
   }

   /**
    * Computes the minimum distance the given 3D point and this line.
    * <p>
    * Edge cases:
    * <ul>
    * <li>if {@code direction.length() < Epsilons.ONE_TRILLIONTH}, this method returns the distance
    * between {@code point} and the given {@code point}.
    * </ul>
    * </p>
    *
    * @param point 2D point to compute the distance from the line. Not modified.
    * @return the minimum distance between the 3D point and this 3D line.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double distance(Point2DReadOnly point)
   {
      checkHasBeenInitialized();
      return EuclidGeometryTools.distanceFromPoint2DToLine2D(point, this.point, direction);
   }

   /**
    * Gets the read-only reference to the point through which this line is going.
    * 
    * @return the reference to the point.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public Point2DReadOnly getPoint()
   {
      checkHasBeenInitialized();
      return point;
   }

   /**
    * Gets the read-only reference to the direction of this line.
    * 
    * @return the reference to the direction.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public Vector2DReadOnly getDirection()
   {
      checkHasBeenInitialized();
      return direction;
   }

   /**
    * Gets the point defining this line by storing its coordinates in the given argument
    * {@code pointToPack}.
    * 
    * @param pointToPack point in which the coordinates of this line's point are stored. Modified.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public void getPoint(Point2DBasics pointOnLineToPack)
   {
      pointOnLineToPack.set(point);
   }

   /**
    * Gets the direction defining this line by storing its components in the given argument
    * {@code directionToPack}.
    * 
    * @param directionToPack vector in which the components of this line's direction are stored.
    *           Modified.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public void getDirection(Vector2DBasics directionToPack)
   {
      checkHasBeenInitialized();
      directionToPack.set(direction);
   }

   /**
    * Gets the point and direction defining this line by storing their components in the given
    * arguments {@code pointToPack} and {@code directionToPack}.
    * 
    * @param pointToPack point in which the coordinates of this line's point are stored. Modified.
    * @param directionToPack vector in which the components of this line's direction are stored.
    *           Modified.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public void getPointAndDirection(Point2DBasics pointToPack, Vector2DBasics directionToPack)
   {
      getPoint(pointToPack);
      getDirection(directionToPack);
   }

   /**
    * Gets the coordinates of two distinct points this line goes through.
    * 
    * @param firstPointOnLineToPack the coordinates of a first point located on this line. Modified.
    * @param secondPointOnLineToPack the coordinates of a second point located on this line.
    *           Modified.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public void getTwoPointsOnLine(Point2DBasics firstPointOnLineToPack, Point2DBasics secondPointOnLineToPack)
   {
      checkHasBeenInitialized();
      firstPointOnLineToPack.set(point);
      secondPointOnLineToPack.add(point, direction);
   }

   /**
    * Gets the x-coordinate of a point this line goes through.
    * 
    * @return the x-coordinate of this line's point.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getPointX()
   {
      checkHasBeenInitialized();
      return point.getX();
   }

   /**
    * Gets the y-coordinate of a point this line goes through.
    * 
    * @return the y-coordinate of this line's point.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getPointY()
   {
      checkHasBeenInitialized();
      return point.getY();
   }

   /**
    * Gets the x-component of this line's direction.
    * 
    * @return the x-component of this line's direction.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getDirectionX()
   {
      checkHasBeenInitialized();
      return direction.getX();
   }

   /**
    * Gets the y-component of this line's direction.
    * 
    * @return the y-component of this line's direction.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getDirectionY()
   {
      checkHasBeenInitialized();
      return direction.getY();
   }

   /**
    * Calculates the slope value of this line.
    * <p>
    * The slope 's' can be used to calculate the y-coordinate of a point located on the line given
    * its x-coordinate:<br>
    * y = s * x + y<sub>0</sub><br>
    * where y<sub>0</sub> is the y-coordinate at which this line intercepts the y-axis and which can
    * be obtained with {@link #getYIntercept()}.
    * </p>
    * 
    * @return the value of the slope of this line.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getSlope()
   {
      checkHasBeenInitialized();
      if (direction.getX() == 0.0 && direction.getY() > 0.0)
      {
         return Double.POSITIVE_INFINITY;
      }

      if (direction.getX() == 0.0 && direction.getY() < 0.0)
      {
         return Double.NEGATIVE_INFINITY;
      }

      return direction.getY() / direction.getX();
   }

   /**
    * Calculates the coordinates of the point 'p' given the parameter 't' as follows:<br>
    * p = t * n + p<sub>0</sub><br>
    * where n is the unit-vector defining the direction of this line and p<sub>0</sub> is the point
    * defining this line which also corresponds to the point for which t=0.
    * <p>
    * Note that the absolute value of 't' is equal to the distance between the point 'p' and the
    * point p<sub>0</sub> defining this line.
    * </p>
    * 
    * @param t the parameter used to calculate the point coordinates.
    * @param pointToPack the point in which the coordinates of 'p' are stored. Modified.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public void getPointGivenParameter(double t, Point2DBasics pointToPack)
   {
      checkHasBeenInitialized();
      pointToPack.scaleAdd(t, direction, point);
   }

   /**
    * Calculates the coordinates of the point 'p' given the parameter 't' as follows:<br>
    * p = t * n + p<sub>0</sub><br>
    * where n is the unit-vector defining the direction of this line and p<sub>0</sub> is the point
    * defining this line which also corresponds to the point for which t=0.
    * <p>
    * Note that the absolute value of 't' is equal to the distance between the point 'p' and the
    * point p<sub>0</sub> defining this line.
    * </p>
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    * 
    * @param t the parameter used to calculate the point coordinates.
    * @return the coordinates of the point 'p'.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public Point2D getPointGivenParameter(double t)
   {
      Point2D pointToReturn = new Point2D();
      getPointGivenParameter(t, pointToReturn);
      return pointToReturn;
   }

   /**
    * Calculates the parameter 't' corresponding to the coordinates of the given {@code pointOnLine}
    * 'p' by solving the line equation:<br>
    * p = t * n + p<sub>0</sub><br>
    * where n is the unit-vector defining the direction of this line and p<sub>0</sub> is the point
    * defining this line which also corresponds to the point for which t=0.
    * <p>
    * Note that the absolute value of 't' is equal to the distance between the point 'p' and the
    * point p<sub>0</sub> defining this line.
    * </p>
    * 
    * @param pointOnLine the coordinates of the 'p' from which the parameter 't' is to be
    *           calculated. The point has to be on the line. Not modified.
    * @param epsilon the maximum distance allowed between the given point and this line. If the
    *           given point is at a distance less than {@code epsilon} from this line, it is
    *           considered as being located on this line.
    * @return the value of the parameter 't' corresponding to the given point.
    * @throws RuntimeException if this line has not been initialized yet.
    * @throws RuntimeException if the given point is located at a distance greater than
    *            {@code epsilon} from this line.
    */
   public double getParameterGivenPointEpsilon(Point2DReadOnly pointOnLine, double epsilon)
   {
      if (!isPointOnLine(pointOnLine, epsilon))
      {
         throw new RuntimeException("getParameterGivenPoint: point not part of line");
      }
      else
      {
         double x0 = this.point.getX();
         double y0 = this.point.getY();
         double x1 = x0 + direction.getX();
         double y1 = y0 + direction.getY();
         return EuclidGeometryTools.percentageAlongLineSegment2D(pointOnLine.getX(), pointOnLine.getY(), x0, y0, x1, y1);
      }
   }

   /**
    * The x-coordinate at which this line intercept the x-axis, i.e. the line defined by
    * {@code y=0}.
    * 
    * @return the x-coordinate of the intersection between this line and the x-axis.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getXIntercept()
   {
      checkHasBeenInitialized();

      double parameterAtIntercept = -point.getY() / direction.getY();

      return parameterAtIntercept * direction.getX() + point.getX();
   }

   /**
    * The y-coordinate at which this line intercept the y-axis, i.e. the line defined by
    * {@code x=0}.
    * 
    * @return the y-coordinate of the intersection between this line and the y-axis.
    * @throws RuntimeException if this line has not been initialized yet.
    */
   public double getYIntercept()
   {
      checkHasBeenInitialized();
      double parameterAtIntercept = -point.getX() / direction.getX();
      return parameterAtIntercept * direction.getY() + point.getY();
   }

   public boolean isPointOnLine(Point2DReadOnly point, double epsilon)
   {
      checkHasBeenInitialized();
      return EuclidGeometryTools.distanceFromPoint2DToLine2D(point, this.point, direction) < epsilon;
   }

   public void negateDirection()
   {
      checkHasBeenInitialized();
      direction.negate();
   }

   public Line2d negateDirectionCopy()
   {
      checkHasBeenInitialized();
      Line2d ret = new Line2d(this);
      ret.negateDirection();

      return ret;
   }

   public void rotate(double radians)
   {
      checkHasBeenInitialized();
      double vXOld = direction.getX();
      double vYOld = direction.getY();

      double vXNew = Math.cos(radians) * vXOld - Math.sin(radians) * vYOld;
      double vYNew = Math.sin(radians) * vXOld + Math.cos(radians) * vYOld;

      direction.set(vXNew, vYNew);
   }

   public void translate(double x, double y)
   {
      point.add(x, y);
   }

   public void shiftToLeft(double distanceToShift)
   {
      shift(true, distanceToShift);
   }

   public void shiftToRight(double distanceToShift)
   {
      shift(false, distanceToShift);
   }

   private void shift(boolean shiftToLeft, double distanceToShift)
   {
      checkHasBeenInitialized();
      double vectorX = direction.getX();
      double vectorY = direction.getY();

      double vectorXPerpToRight = -vectorY;
      double vectorYPerpToRight = vectorX;

      if (!shiftToLeft)
      {
         vectorXPerpToRight = -vectorXPerpToRight;
         vectorYPerpToRight = -vectorYPerpToRight;
      }

      vectorXPerpToRight = distanceToShift * vectorXPerpToRight;
      vectorYPerpToRight = distanceToShift * vectorYPerpToRight;

      point.setX(point.getX() + vectorXPerpToRight);
      point.setY(point.getY() + vectorYPerpToRight);
   }

   public Line2d interiorBisector(Line2d secondLine)
   {
      checkHasBeenInitialized();
      Point2D pointOnLine = intersectionWith(secondLine);
      if (pointOnLine == null)
      {
         double distanceBetweenLines = secondLine.distance(point);
         double epsilon = 1E-7;

         boolean sameLines = distanceBetweenLines < epsilon;
         if (sameLines)
         {
            return new Line2d(this);
         }
         else
         {
            return null;
         }
      }

      Vector2D directionVector = new Vector2D(direction);
      directionVector.add(secondLine.direction);

      return new Line2d(pointOnLine, directionVector);

   }

   public void perpendicularVector(Vector2DBasics vectorToPack)
   {
      checkHasBeenInitialized();
      vectorToPack.set(direction.getY(), -direction.getX());
   }

   public Vector2D perpendicularVector()
   {
      Vector2D vectorToReturn = new Vector2D();
      perpendicularVector(vectorToReturn);
      return vectorToReturn;
   }

   public Line2d perpendicularLineThroughPoint(Point2DReadOnly point)
   {
      checkHasBeenInitialized();
      return new Line2d(point, perpendicularVector());
   }

   public Point2D intersectionWith(LineSegment2d lineSegment)
   {
      checkHasBeenInitialized();
      return lineSegment.intersectionWith(this);
   }

   public boolean areLinesPerpendicular(Line2d line)
   {
      checkHasBeenInitialized();
      // Dot product of two vectors is zero if the vectors are perpendicular
      return direction.dot(line.getDirection()) < 1e-7;
   }

   public Point2D intersectionWith(Line2d secondLine)
   {
      checkHasBeenInitialized();
      return EuclidGeometryTools.intersectionBetweenTwoLine2Ds(point, direction, secondLine.point, secondLine.direction);
   }

   public boolean intersectionWith(Line2d secondLine, Point2DBasics intersectionToPack)
   {
      checkHasBeenInitialized();
      return EuclidGeometryTools.intersectionBetweenTwoLine2Ds(point, direction, secondLine.point, secondLine.direction, intersectionToPack);
   }

   public Point2D[] intersectionWith(ConvexPolygon2d convexPolygon)
   {
      checkHasBeenInitialized();
      return convexPolygon.intersectionWith(this);
   }

   public double distanceSquared(Point2DReadOnly point)
   {
      checkHasBeenInitialized();
      double distance = distance(point);

      return distance * distance;
   }

   public double distance(LineSegment2d lineSegment)
   {
      checkHasBeenInitialized();
      throw new RuntimeException("Not yet implemented");
   }

   public double distance(ConvexPolygon2d convexPolygon)
   {
      checkHasBeenInitialized();
      throw new RuntimeException("Not yet implemented");
   }

   @Override
   public String toString()
   {
      String ret = "";

      ret = ret + point + ", " + direction;

      return ret;
   }

   @Override
   public void applyTransform(Transform transform)
   {
      checkHasBeenInitialized();
      point.applyTransform(transform);
      direction.applyTransform(transform);
   }

   public void applyTransformAndProjectToXYPlane(Transform transform)
   {
      checkHasBeenInitialized();
      point.applyTransform(transform, false);
      direction.applyTransform(transform, false);
   }

   public Line2d applyTransformCopy(Transform transform)
   {
      Line2d copy = new Line2d(this);
      copy.applyTransform(transform);
      return copy;
   }

   public Line2d applyTransformAndProjectToXYPlaneCopy(Transform transform)
   {
      Line2d copy = new Line2d(this);
      copy.applyTransformAndProjectToXYPlane(transform);
      return copy;
   }

   public boolean isPointOnLine(Point2DReadOnly point)
   {
      checkHasBeenInitialized();
      double epsilon = 1e-8;
      if (Math.abs(direction.getX()) < 10E-10)
         return MathTools.epsilonEquals(point.getX(), this.point.getX(), epsilon);

      // y = A*x + b with point = (x,y)
      double A = direction.getY() / direction.getX();
      double b = this.point.getY() - A * this.point.getX();

      double value = point.getY() - A * point.getX() - b;

      return epsilon > Math.abs(value);
   }

   public boolean isPointOnLeftSideOfLine(Point2DReadOnly point)
   {
      return isPointOnSideOfLine(point.getX(), point.getY(), RobotSide.LEFT);
   }

   public boolean isPointOnRightSideOfLine(Point2DReadOnly point)
   {
      return isPointOnSideOfLine(point.getX(), point.getY(), RobotSide.RIGHT);
   }

   public boolean isPointOnSideOfLine(Point2DReadOnly point, RobotSide side)
   {
      return isPointOnSideOfLine(point.getX(), point.getY(), side);
   }

   private boolean isPointOnSideOfLine(double x, double y, RobotSide side)
   {
      checkHasBeenInitialized();
      return EuclidGeometryTools.isPoint2DOnSideOfLine2D(x, y, point, direction, side == RobotSide.LEFT);
   }

   /**
    * This method could be improved but must be tested better first.
    */
   public boolean isPointInFrontOfLine(Vector2DReadOnly frontDirection, Point2DReadOnly point)
   {
      double crossProduct = frontDirection.cross(direction);
      if (crossProduct > 0.0)
         return isPointOnRightSideOfLine(point);
      else if (crossProduct < 0.0)
         return isPointOnLeftSideOfLine(point);
      else
         throw new RuntimeException("Not defined when line is pointing exactly along the front direction");
   }

   /**
    * isPointInFrontOfLine returns whether the point is in front of the line or not. The front
    * direction is defined as the positive x-direction
    *
    * @param point Point2d
    * @return boolean
    */
   public boolean isPointInFrontOfLine(Point2DReadOnly point)
   {
      return isPointInFrontOfLine(point.getX(), point.getY());
   }

   private boolean isPointInFrontOfLine(double x, double y)
   {
      if (direction.getY() > 0.0)
         return isPointOnSideOfLine(x, y, RobotSide.RIGHT);
      else if (direction.getY() < 0.0)
         return isPointOnSideOfLine(x, y, RobotSide.LEFT);
      else
         throw new RuntimeException("Not defined when line is pointing exactly along the x-axis");
   }

   // TODO: Inconsistency in strictness.
   public boolean isPointBehindLine(Point2DReadOnly point)
   {
      return !isPointInFrontOfLine(point);
   }

   public void setParallelLineThroughPoint(Point2DReadOnly point)
   {
      this.point.set(point);
   }

   /**
    * Computes the orthogonal projection of the given 2D point on this 2D line.
    *
    * @param point2d the point to project on this line. Modified.
    */
   public void orthogonalProjection(Point2DBasics point2d)
   {
      checkHasBeenInitialized();
      EuclidGeometryTools.orthogonalProjectionOnLine2D(point2d, point, direction, point2d);
   }

   /**
    * Computes the orthogonal projection of the given 2D point on this 2D line.
    * <p>
    * WARNING: This method generates garbage.
    * </p>
    *
    * @param point2d the point to compute the projection of. Not modified.
    * @return the projection of the point onto the line or {@code null} if the method failed.
    */
   public Point2D orthogonalProjectionCopy(Point2DReadOnly point2d)
   {
      checkHasBeenInitialized();
      Point2D projection = new Point2D();

      boolean success = EuclidGeometryTools.orthogonalProjectionOnLine2D(point2d, point, direction, projection);
      if (!success)
         return null;
      else
         return projection;
   }

   public boolean equals(Line2d otherLine)
   {
      return point.equals(otherLine.point) && direction.equals(otherLine.direction);
   }

   private void checkReasonableVector(Vector2DReadOnly localVector)
   {
      if (Math.abs(localVector.getX()) < minAllowableVectorPart && Math.abs(localVector.getY()) < minAllowableVectorPart)
      {
         throw new RuntimeException("Line length must be greater than zero");
      }
   }

   private void checkDistinctPoints(Point2DReadOnly firstPointOnLine, Point2DReadOnly secondPointOnLine)
   {
      if (firstPointOnLine.equals(secondPointOnLine))
      {
         throw new RuntimeException("Tried to create a line from two coincidal points");
      }
   }

   private void checkHasBeenInitialized()
   {
      if (!hasPointBeenSet)
         throw new RuntimeException("The point of this line has not been initialized.");
      if (!hasDirectionBeenSet)
         throw new RuntimeException("The direction of this line has not been initialized.");
   }

   @Override
   public boolean epsilonEquals(Line2d other, double epsilon)
   {
      checkHasBeenInitialized();
      if (!point.epsilonEquals(other.point, epsilon))
         return false;
      if (!direction.epsilonEquals(other.direction, epsilon))
         return false;

      return true;
   }
}
