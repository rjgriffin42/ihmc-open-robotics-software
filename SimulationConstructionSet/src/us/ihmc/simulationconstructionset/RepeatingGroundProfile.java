package us.ihmc.simulationconstructionset;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.GroundProfile3D;
import us.ihmc.graphics3DAdapter.HeightMapWithNormals;
import us.ihmc.utilities.math.geometry.BoundingBox3d;

import us.ihmc.simulationconstructionset.util.ground.RepeatingHeightMap;

public class RepeatingGroundProfile implements GroundProfile3D
{
   private double xMin, xMax, yMin, yMax;
   private double xDistance, yDistance;

   private final GroundProfile3D groundProfile;
   private final RepeatingHeightMap heightMap;

   private final BoundingBox3d boundingBox;

   public RepeatingGroundProfile(GroundProfile3D groundProfile, double xMin, double xMax, double yMin, double yMax)
   {
      this.xMin = xMin;
      this.xMax = xMax;

      this.yMin = yMin;
      this.yMax = yMax;

      this.xDistance = this.xMax - this.xMin;
      this.yDistance = this.yMax - this.yMin;

      this.groundProfile = groundProfile;

      double zMin = groundProfile.getBoundingBox().getZMin();
      double zMax = groundProfile.getBoundingBox().getZMax();
      this.boundingBox = new BoundingBox3d(xMin, yMin, zMin, xMax, yMax, zMax);

      this.heightMap = new RepeatingHeightMap(groundProfile.getHeightMapIfAvailable(), xMin, xMax, yMin, yMax);
   }

   private double xLocal(double xGlobal)
   {
      return (Math.abs(xGlobal - xMin) % xDistance) + xMin;
   }

   private double yLocal(double yGlobal)
   {
      return (Math.abs(yGlobal - yMin) % yDistance) + yMin;
   }

   public boolean isClose(double x, double y, double z)
   {
      return groundProfile.isClose(xLocal(x), yLocal(y), z);
   }

   public BoundingBox3d getBoundingBox()
   {
      return boundingBox;
   }

   public boolean checkIfInside(double x, double y, double z, Point3d intersectionToPack, Vector3d normalToPack)
   {
      double localX = xLocal(x);
      double localY = yLocal(y);

      return groundProfile.checkIfInside(localX, localY, z, intersectionToPack, normalToPack);
   }

   public HeightMapWithNormals getHeightMapIfAvailable()
   {
      return heightMap;
   }

}
