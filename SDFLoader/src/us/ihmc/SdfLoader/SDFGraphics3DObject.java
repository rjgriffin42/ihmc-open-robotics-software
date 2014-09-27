package us.ihmc.SdfLoader;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import us.ihmc.utilities.math.geometry.Transform3d;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.apache.commons.lang.builder.ToStringBuilder;

import us.ihmc.SdfLoader.xmlDescription.AbstractSDFMesh;
import us.ihmc.SdfLoader.xmlDescription.SDFGeometry;
import us.ihmc.SdfLoader.xmlDescription.SDFGeometry.HeightMap.Blend;
import us.ihmc.SdfLoader.xmlDescription.SDFGeometry.HeightMap.Texture;
import us.ihmc.SdfLoader.xmlDescription.SDFGeometry.Mesh;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.ModelFileType;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.HeightBasedTerrainBlend;
import us.ihmc.graphics3DAdapter.graphics.appearances.SDFAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceMaterial;
import us.ihmc.utilities.math.geometry.GeometryTools;


public class SDFGraphics3DObject extends Graphics3DObject
{
   private static final boolean SHOW_COORDINATE_SYSTEMS = false;
   private static final AppearanceDefinition DEFAULT_APPEARANCE = YoAppearance.Orange();
   static
   {
      YoAppearance.makeTransparent(DEFAULT_APPEARANCE, 0.4);
   }
   
   public SDFGraphics3DObject(List<? extends AbstractSDFMesh> sdfVisuals, ArrayList<String> resourceDirectories)
   {
      this(sdfVisuals, resourceDirectories, new Transform3d());
   }
   
   public SDFGraphics3DObject(List<? extends AbstractSDFMesh> sdfVisuals, ArrayList<String> resourceDirectories, Transform3d graphicsTransform)
   {
      Matrix3d rotation = new Matrix3d();
      Vector3d offset = new Vector3d();
      graphicsTransform.get(rotation, offset);
      
      if(sdfVisuals != null)
      {
         for(AbstractSDFMesh sdfVisual : sdfVisuals)
         {
            identity();
            translate(offset);
            rotate(rotation);
            
            Transform3d visualPose = SDFConversionsHelper.poseToTransform(sdfVisual.getPose());
            Vector3d modelOffset = new Vector3d();
            Matrix3d modelRotation = new Matrix3d();
            visualPose.get(modelRotation, modelOffset);
            
            if (SHOW_COORDINATE_SYSTEMS)
            {
               addCoordinateSystem(0.1);         
            }
            translate(modelOffset);
            rotate(modelRotation);     
            AppearanceDefinition appearance = null;
            if(sdfVisual.getMaterial() != null)
            {
               if(sdfVisual.getMaterial().getScript() != null)
               {
                  ArrayList<String> resourceUrls = new ArrayList<>();
                  
                  if(sdfVisual.getMaterial().getScript().getUri() != null)
                  {
                     for(String uri : sdfVisual.getMaterial().getScript().getUri())
                     {
                        if(uri.equals("__default__"))
                        {
                           resourceUrls.add("/scripts/gazebo.material");
                        }
                        else
                        {
                           String id = convertToResourceIdentifier(resourceDirectories, uri);
                           resourceUrls.add(id);
                        }
                     }
                  }
                  
                  String name = sdfVisual.getMaterial().getScript().getName();
                  
                  appearance = new SDFAppearance(resourceUrls, name, resourceDirectories);
               }
               else
               {  
                  YoAppearanceMaterial mat = new YoAppearanceMaterial();
                  
                  mat.setAmbientColor(SDFConversionsHelper.stringToColor(sdfVisual.getMaterial().getAmbient()));
                  mat.setDiffuseColor(SDFConversionsHelper.stringToColor(sdfVisual.getMaterial().getDiffuse()));
                  mat.setSpecularColor(SDFConversionsHelper.stringToColor(sdfVisual.getMaterial().getSpecular()));
                  
                  appearance = mat;
               }
            }
            
            SDFGeometry geometry = sdfVisual.getGeometry();
            Mesh mesh = geometry.getMesh();
            if(mesh != null)
            {
               String resourceUrl = convertToResourceIdentifier(resourceDirectories, mesh.getUri());
               if(mesh.getScale() != null)
               {
                  Vector3d scale = SDFConversionsHelper.stringToVector3d(mesh.getScale());
                  scale(scale);
               }
               String submesh = null;
               boolean centerSubmesh = false;
               if(mesh.getSubmesh() != null)
               {
                  submesh = mesh.getSubmesh().getName().trim();
                  centerSubmesh = mesh.getSubmesh().getCenter().trim().equals("1");
               }
               addMesh(resourceUrl, submesh, centerSubmesh, visualPose, appearance, resourceDirectories);
            }
            else if(geometry.getCylinder() != null)
            {
               double length = Double.parseDouble(geometry.getCylinder().getLength());
               double radius = Double.parseDouble(geometry.getCylinder().getRadius()); 
               translate(0.0, 0.0, -length/2.0);
               addCylinder(length, radius, getDefaultAppearanceIfNull(appearance));
            }
            else if(geometry.getBox() != null)
            {
               String[] boxDimensions = geometry.getBox().getSize().split(" ");            
               double bx = Double.parseDouble(boxDimensions[0]);
               double by = Double.parseDouble(boxDimensions[1]);
               double bz = Double.parseDouble(boxDimensions[2]);
               translate(0.0, 0.0, -bz/2.0);
               addCube(bx, by, bz, getDefaultAppearanceIfNull(appearance));
            }
            else if(geometry.getSphere() != null)
            {
               double radius = Double.parseDouble(geometry.getSphere().getRadius());
               addSphere(radius, getDefaultAppearanceIfNull(appearance));
            }
            else if(geometry.getPlane() != null)
            {
               Vector3d normal = SDFConversionsHelper.stringToNormalizedVector3d(geometry.getPlane().getNormal());
               Vector2d size = SDFConversionsHelper.stringToVector2d(geometry.getPlane().getSize());
               
               AxisAngle4d planeRotation = GeometryTools.getRotationBasedOnNormal(normal);
               rotate(planeRotation);
               addCube(size.x, size.y, 0.005, getDefaultAppearanceIfNull(appearance));
            }
            else if(geometry.getHeightMap() != null)
            {
               String id = convertToResourceIdentifier(resourceDirectories, geometry.getHeightMap().getUri());
               SDFHeightMap heightMap = new SDFHeightMap(id, geometry.getHeightMap());
               
               
               AppearanceDefinition app = DEFAULT_APPEARANCE;
               if(geometry.getHeightMap().getTextures() != null)
               {
                  double width = heightMap.getBoundingBox().getXMax() - heightMap.getBoundingBox().getXMin();
                  HeightBasedTerrainBlend sdfTerrainBlend = new HeightBasedTerrainBlend(heightMap);
                  for(Texture text : geometry.getHeightMap().getTextures())
                  {
                     double size = Double.parseDouble(text.getSize());
                     double scale = width/size;
                     sdfTerrainBlend.addTexture(scale, convertToResourceIdentifier(resourceDirectories, text.getDiffuse()),
                           convertToResourceIdentifier(resourceDirectories, text.getNormal()));
                  }
                  
                  for(Blend blend : geometry.getHeightMap().getBlends())
                  {
                     sdfTerrainBlend.addBlend(Double.parseDouble(blend.getMinHeight()), Double.parseDouble(blend.getFadeDist()));
                  }
                  
                  app = sdfTerrainBlend;
               }
               translate(heightMap.getOffset());
               addHeightMap(heightMap, 1000, 1000, app);
            }
            else
            {
               System.err.println("Visual for " + sdfVisual.getName() + " not implemented yet");
               System.err.println("Defined visual" + ToStringBuilder.reflectionToString(geometry));
               
            }
   
            
         }
      }
   }
   
   private static AppearanceDefinition getDefaultAppearanceIfNull(AppearanceDefinition appearance)
   {
      if(appearance == null)
      {
         return DEFAULT_APPEARANCE;
      }
      else
      {
         return appearance;
      }
   }
   
   private void addMesh(String mesh, String submesh, boolean centerSubmesh, Transform3d visualPose, AppearanceDefinition appearance, ArrayList<String> resourceDirectories)
   {

      // STL files do not have appearances
      if (ModelFileType.getModelTypeFromId(mesh) == ModelFileType._STL)
      {
         appearance = getDefaultAppearanceIfNull(appearance);
      }

      addModelFile(mesh, submesh, centerSubmesh, resourceDirectories, appearance);
   }

   private String convertToResourceIdentifier(ArrayList<String> resourceDirectories, String meshPath)
   {
      if(meshPath.equals("__default__"))
      {
         meshPath = "file://media/materials/scripts/gazebo.material";
      }

      for (String resourceDirectory : resourceDirectories)
      {
         try
         {
            URI meshURI = new URI(meshPath);

            String id = resourceDirectory + meshURI.getAuthority() + meshURI.getPath();
//            System.out.println("PATH: " + meshURI.getPath());
//            System.out.println("AUTH: " + meshURI.getAuthority());
//            System.out.println("ID: " + id);
            URL resource = getClass().getClassLoader().getResource(id);
            if (resource != null)
            {
               return id;
            }
         } catch (URISyntaxException e)
         {
            System.err.println("Malformed resource path in .SDF file for path: " + meshPath);
         }
      }

      System.out.println(meshPath);
      throw new RuntimeException("Resource not found");
   }

}
