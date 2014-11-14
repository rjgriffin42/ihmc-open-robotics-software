package us.ihmc.simulationconstructionset.util.ground;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.MeshDataGenerator;
import us.ihmc.graphics3DAdapter.graphics.MeshDataHolder;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.utilities.ThreadTools;
import us.ihmc.utilities.math.geometry.BoundingBox3d;

import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.SimulationConstructionSet;

public class RollingGroundProfile extends GroundProfileFromHeightMap
{
   private static final double xMinDefault = -20.0, xMaxDefault = 20.0, yMinDefault = -20.0, yMaxDefault = 20.0;
   private static final double amplitudeDefault = 0.1, frequencyDefault = 0.3, offsetDefault = 0.0;
   
   private final BoundingBox3d boundingBox;
   

   protected final double amplitude, frequency, offset;

   public RollingGroundProfile()
   {
      this(amplitudeDefault, frequencyDefault, offsetDefault);  
   }

   public RollingGroundProfile(double amplitude, double frequency, double offset)
   {
      this(amplitude, frequency, offset, xMinDefault, xMaxDefault, yMinDefault, yMaxDefault);
   }

   public RollingGroundProfile(double amplitude, double frequency, double offset, double xMin, double xMax, double yMin, double yMax)
   {
      this.amplitude = amplitude;
      this.frequency = frequency;
      this.offset = offset;
      
      double zMin = Double.NEGATIVE_INFINITY; //-100.0;
      double zMax = Math.abs(amplitude) + 1e-4;
            
      boundingBox = new BoundingBox3d(new Point3d(xMin, yMin, zMin), new Point3d(xMax, yMax, zMax));
   }

   public BoundingBox3d getBoundingBox()
   {
      return boundingBox;
   }

   public double heightAt(double x, double y, double z)
   {
      double height = amplitude * Math.sin(2.0 * Math.PI * frequency * (x + offset));
      return height;
   }


   public void surfaceNormalAt(double x, double y, double z, Vector3d normal)
   {
      double dzdx = 0.0;

      dzdx = amplitude * 2.0 * Math.PI * frequency * Math.cos(2.0 * Math.PI * frequency * (x + offset));

      normal.x = -dzdx;
      normal.y = 0.0;
      normal.z = 1.0;

      normal.normalize();
   }
   
   public double heightAndNormalAt(double x, double y, double z, Vector3d normalToPack)
   {
      double heightAt = heightAt(x, y, z);
      surfaceNormalAt(x, y, z, normalToPack);
      return heightAt;
   }

   public static void main(String[] args)
   {
      RollingGroundProfile rollingGroundProfile = new RollingGroundProfile();

      SimulationConstructionSet scs = new SimulationConstructionSet(new Robot("Null"));
      scs.setGroundVisible(false);
      scs.startOnAThread();
      
      ThreadTools.sleep(1000);
      Graphics3DObject linkGraphics = new Graphics3DObject();
      linkGraphics.translate(new Vector3d(0.0, 0.0, 1.0));
      linkGraphics.addSphere(0.5);
      scs.addStaticLinkGraphics(linkGraphics);
      
      
      MeshDataHolder meshData = MeshDataGenerator.Cone(0.8, 0.4, 20);
      Graphics3DObject meshLinkGraphics = new Graphics3DObject();
      meshLinkGraphics.translate(2.0, 0.0, 0.0);
      meshLinkGraphics.addMeshData(meshData, YoAppearance.Green());
      scs.addStaticLinkGraphics(meshLinkGraphics);
      
      Graphics3DObject groundLinkGraphics = new Graphics3DObject();
      groundLinkGraphics.addCoordinateSystem(1.0);
      
      HeightMap heightMap = rollingGroundProfile;
      groundLinkGraphics.addHeightMap(heightMap, 300, 300, YoAppearance.Red());
      scs.addStaticLinkGraphics(groundLinkGraphics);    
   }
   
}
