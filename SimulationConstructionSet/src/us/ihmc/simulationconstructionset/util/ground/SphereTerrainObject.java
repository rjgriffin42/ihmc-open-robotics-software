package us.ihmc.simulationconstructionset.util.ground;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.HeightMapWithNormals;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotics.geometry.BoundingBox3d;


public class SphereTerrainObject implements TerrainObject3D, HeightMapWithNormals
{
   private final BoundingBox3d boundingBox;
   private Graphics3DObject linkGraphics;


   public SphereTerrainObject(double centerX, double centerY,double centerZ, double radius, AppearanceDefinition appearance)
   {
     double xMin = centerX - radius;
     double xMax = centerX + radius;

     double yMin = centerY - radius;
     double yMax = centerY + radius;

     double zMin = centerZ - radius;
     double zMax = centerZ + radius;

     
     Point3d minPoint = new Point3d(xMin, yMin, zMin);
     Point3d maxPoint = new Point3d(xMax, yMax, zMax);
     
     boundingBox = new BoundingBox3d(minPoint, maxPoint);
     
     linkGraphics = new Graphics3DObject();
          
     linkGraphics.translate(centerX,centerY, centerZ);

     linkGraphics.addSphere(radius, appearance);
 }

 





 public Graphics3DObject getLinkGraphics()
 {
   return linkGraphics;

 }

 public double heightAndNormalAt(double x, double y, double z, Vector3d normalToPack)
 {
    double heightAt = this.heightAt(x, y, z);
    this.surfaceNormalAt(x, y, z, normalToPack);
    
    return heightAt;
 }
 
 public double heightAt(double x, double y, double z)
 {
   if ((x > boundingBox.getXMin()) && (x < boundingBox.getXMax()) && (y > boundingBox.getYMin()) && (y < boundingBox.getYMax()))
   {
     return boundingBox.getZMax();
   }

   return 0.0;
 }
 
 
   private void surfaceNormalAt(double x, double y, double z, Vector3d normal)
   {
      double threshhold = 0.015;
      normal.x = 0.0;
      normal.y = 0.0;
      normal.z = 1.0;

      if (!boundingBox.isXYInside(x, y) || (z > boundingBox.getZMax() - threshhold))
         return;

      if (Math.abs(x - boundingBox.getXMin()) < threshhold)
      {
         normal.x = -1.0;
         normal.y = 0.0;
         normal.z = 0.0;
      }

      else if (Math.abs(x - boundingBox.getXMax()) < threshhold)
      {
         normal.x = 1.0;
         normal.y = 0.0;
         normal.z = 0.0;
      }

      else if (Math.abs(y - boundingBox.getYMin()) < threshhold)
      {
         normal.x = 0.0;
         normal.y = -1.0;
         normal.z = 0.0;
      }

      else if (Math.abs(y - boundingBox.getYMax()) < threshhold)
      {
         normal.x = 0.0;
         normal.y = 1.0;
         normal.z = 0.0;
      }
   }

   public void closestIntersectionAndNormalAt(double x, double y, double z, Point3d intersection, Vector3d normal)
   {
      intersection.x = x;    // Go Straight Up for now...
      intersection.y = y;
      intersection.z = heightAt(x, y, z);

      surfaceNormalAt(x, y, z, normal);
   }

   public boolean checkIfInside(double x, double y, double z, Point3d intersectionToPack, Vector3d normalToPack)
   {
      intersectionToPack.x = x;    // Go Straight Up for now...
      intersectionToPack.y = y;
      intersectionToPack.z = heightAt(x, y, z);

      surfaceNormalAt(x, y, z, normalToPack);
      
      return (z < intersectionToPack.getZ());
   }

   public boolean isClose(double x, double y, double z)
   {
      return (boundingBox.isXYInside(x, y));
   }


   public double getXMin()
   {
      return boundingBox.getXMin();
   }

   public double getYMin()
   {
      return boundingBox.getYMin();
   }

   public double getXMax()
   {
      return boundingBox.getXMax();
   }

   public double getYMax()
   {
      return boundingBox.getYMax();
   }

   public BoundingBox3d getBoundingBox()
   {
      return boundingBox;
   }

   public HeightMapWithNormals getHeightMapIfAvailable()
   {
      return this;
   }

}
