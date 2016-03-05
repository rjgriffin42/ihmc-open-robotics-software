package us.ihmc.simulationconstructionset.util;

import javax.vecmath.Vector3d;

import us.ihmc.simulationconstructionset.util.ground.GroundProfileFromHeightMap;
import us.ihmc.robotics.geometry.BoundingBox3d;


public class StepDownsGroundprofile extends GroundProfileFromHeightMap
{
   private static final double xMinDefault = -20.0, xMaxDefault = 20.0, yMinDefault = -20.0, yMaxDefault = 20.0;
   private static final double amplitudeDefault = 0.1, frequencyDefault = 0.3, offsetDefault = 0.0, heightOffsetDefault = 0.0;

   private final BoundingBox3d boundingBox;

   private final double amplitude, frequency, offset, heightOffset;

   public StepDownsGroundprofile()
   {
      this(amplitudeDefault, frequencyDefault, offsetDefault);
   }

   public StepDownsGroundprofile(double amplitude, double frequency, double offset)
   {
      this(amplitude, frequency, offset, heightOffsetDefault);
   }

   public StepDownsGroundprofile(double amplitude, double frequency, double offset, double heightOffset)
   {
      this(amplitude, frequency, offset, heightOffset, xMinDefault, xMaxDefault, yMinDefault, yMaxDefault);
   }

   public StepDownsGroundprofile(double amplitude, double frequency, double offset, double heightOffset, double xMin, double xMax, double yMin, double yMax)
   {
      this.amplitude = amplitude;
      this.frequency = frequency;
      this.offset = offset;
      this.heightOffset = heightOffset;
      
      double zMin = Double.NEGATIVE_INFINITY;
      double zMax = Double.POSITIVE_INFINITY;
      this.boundingBox = new BoundingBox3d(xMin, yMin, zMin, xMax, yMax, zMax);
   }

   public double heightAndNormalAt(double x, double y, double z, Vector3d normalToPack)
   {
      double height = heightAt(x, y, z);
      surfaceNormalAt(x, y, z, normalToPack);
      
      return height;
   }
   
   public double heightAt(double x, double y, double z)
   {
      double height = heightOffset;

      if (boundingBox.isXYInside(x, y))
         height = amplitude * Math.sin(2.0 * Math.PI * frequency * (x + offset));

      if (height > amplitude * 0.8)
         height = amplitude + heightOffset;
      else if (height < -amplitude * 0.8)
         height = -amplitude + heightOffset;
      else
         height = heightOffset;

      return height;
   }


   public void surfaceNormalAt(double x, double y, double z, Vector3d normal)
   {
      double dzdx = 0.0;
      normal.x = -dzdx;
      normal.y = 0.0;
      normal.z = 1.0;

      normal.normalize();
   }

   public BoundingBox3d getBoundingBox()
   {
      return boundingBox;
   }

}
