package us.ihmc.graphics3DAdapter;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.geometry.BoundingBox3d;

public interface GroundProfile3D
{
   public abstract BoundingBox3d getBoundingBox();

   /**
    * <p>isClose is used as an optimization pass. This method is used to check whether or not
    * it's even necessary to perform more mathematically intense checks like {@link #checkIfInside(double, double, double, Point3d, Vector3d)}
    * or {@link us.ihmc.graphics3DAdapter.HeightMapWithNormals#heightAt(double, double, double)}.</p>
    *
    * <p>
    * As such, isClose should always be a very fast check, and when complexity of implementation is under
    * consideration one can always deem it acceptable to do a very efficient implementation that returns
    * a false positive instead of a slower implementation that is more precise.
    * </p>
    *
    * @param x the X coordinate to check
    * @param y the Y coordinate to check
    * @param z the Z coordinate to check
    * @return whether or not the point is close enough to warrant additional work
    */
   public abstract boolean isClose(double x, double y, double z);
   
   /**
    * Returns true if inside the ground object. If inside, must pack the intersection and normal. If not inside, packing those is optional.
    */
   public abstract boolean checkIfInside(double x, double y, double z, Point3d intersectionToPack, Vector3d normalToPack);
   
   public abstract HeightMapWithNormals getHeightMapIfAvailable();
}
