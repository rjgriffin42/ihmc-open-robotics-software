package us.ihmc.robotics.geometry;

import javax.vecmath.Point2d;

import us.ihmc.robotics.geometry.interfaces.GeometryObject;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: </p>
 *
 * @author Twan Koolen
 * @version 1.0
 */
public interface Geometry2d<T extends Geometry2d<T>> extends GeometryObject<T>
{
   // Orthogonal projection:
   public void orthogonalProjection(Point2d tuple);

   public Point2d orthogonalProjectionCopy(Point2d point);

   // Intersection:
   public Object intersectionWith(Line2d line);

   public Object intersectionWith(LineSegment2d lineSegment);

   public Object intersectionWith(ConvexPolygon2d convexPolygon);

   // Distance:
   public double distance(Point2d point2d);

   public double distance(Line2d line);

   public double distance(LineSegment2d lineSegment);

   public double distance(ConvexPolygon2d convexPolygon);

   // Extra Transformations for being 2d:

   public void applyTransformAndProjectToXYPlane(RigidBodyTransform transform);

   public Geometry2d applyTransformCopy(RigidBodyTransform transform);

   public Geometry2d applyTransformAndProjectToXYPlaneCopy(RigidBodyTransform transform);
}
