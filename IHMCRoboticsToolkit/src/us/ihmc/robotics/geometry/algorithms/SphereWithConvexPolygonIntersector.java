package us.ihmc.robotics.geometry.algorithms;

import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple2D.interfaces.Point2DBasics;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FramePoint2D;
import us.ihmc.robotics.geometry.shapes.FrameSphere3d;

public class SphereWithConvexPolygonIntersector
{
   private final FramePoint3D closestPointOnPolygon;
   private final FramePoint2D closestPointOnPolygon2d;

   public SphereWithConvexPolygonIntersector()
   {
      closestPointOnPolygon = new FramePoint3D();
      closestPointOnPolygon2d = new FramePoint2D();
   }

   /**
    * All math in polygon frame.
    */
   public boolean checkIfIntersectionExists(FrameSphere3d sphere, FrameConvexPolygon2d polygon)
   {
      ReferenceFrame originalSphereFrame = sphere.getReferenceFrame();
      sphere.changeFrame(polygon.getReferenceFrame());
      
      sphere.getCenter(closestPointOnPolygon);
      closestPointOnPolygon2d.setIncludingFrame(closestPointOnPolygon);
      
      Point2DBasics pointUnsafe = closestPointOnPolygon2d;
      polygon.getConvexPolygon2d().orthogonalProjection(pointUnsafe);
      closestPointOnPolygon2d.set(pointUnsafe.getX(), pointUnsafe.getY());
      
      closestPointOnPolygon.set(closestPointOnPolygon2d, 0.0);
      
      boolean isInsideOrOnSurface = sphere.getSphere3d().isInsideOrOnSurface(closestPointOnPolygon.getPoint());
      
      closestPointOnPolygon.changeFrame(ReferenceFrame.getWorldFrame());
      sphere.changeFrame(originalSphereFrame);
      
      return isInsideOrOnSurface;
   }

   public FramePoint3D getClosestPointOnPolygon()
   {
      return closestPointOnPolygon;
   }
}
