package us.ihmc.graphics3DAdapter.jme;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import jme3tools.optimize.GeometryBatchFactory;

import org.jmonkeyengine.tralala.ShapeUtilities;

import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DInstructionExecutor;
import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.graphics3DAdapter.graphics.MeshDataHolder;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddExtrusionInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddHeightMapInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddMeshDataInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DAddModelFileInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.Graphics3DInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.listeners.AppearanceChangedListener;
import us.ihmc.graphics3DAdapter.graphics.instructions.listeners.ExtrusionChangedListener;
import us.ihmc.graphics3DAdapter.graphics.instructions.listeners.MeshChangedListener;
import us.ihmc.graphics3DAdapter.graphics.instructions.listeners.ScaleChangedListener;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DRotateInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DScaleInstruction;
import us.ihmc.graphics3DAdapter.graphics.instructions.primitives.Graphics3DTranslateInstruction;
import us.ihmc.graphics3DAdapter.jme.terrain.JMEHeightMapTerrain;
import us.ihmc.graphics3DAdapter.jme.util.JMEDataTypeUtils;
import us.ihmc.utilities.math.geometry.RotationFunctions;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class JMEGraphicsObject extends Graphics3DInstructionExecutor
{

   private final JMEAssetLocator jmeAssetLocator;
   private final Application application;

   private Node rootNode = new Node();
   protected Node currentNode;

   private final boolean immutable;

   private boolean optimizeGraphicsObject = false;

   public JMEGraphicsObject(Application application, JMEAssetLocator jmeAssetLocator, Graphics3DObject graphics3dObject)
   {
      this.application = application;
      this.jmeAssetLocator = jmeAssetLocator;
      immutable = !graphics3dObject.isChangeable();

      currentNode = this.rootNode;
      setUpGraphicsFromDefinition(graphics3dObject.getGraphics3DInstructions());

      // Optimize geometries. Cannot change geometries on an optimized node.
      if (optimizeGraphicsObject && immutable)
      {
         rootNode = (Node) GeometryBatchFactory.optimize(rootNode);
      }
   }

   public static Spatial createGraphics3DObjectFromModel(String fileName, String submesh, boolean centerSubmesh, AppearanceDefinition appearanceDefinition,
         JMEAssetLocator jmeAssetLocator)
   {
      Spatial spatial;

      try
      {
         spatial = jmeAssetLocator.loadModel(fileName);

         if (submesh != null)
         {
            spatial = findSubmesh(spatial, submesh, centerSubmesh);
            if (spatial == null)
            {
               System.err.println("Cannot find submesh " + submesh);
               spatial = new Node();
               appearanceDefinition = null;
            }
            else
            {
               if (centerSubmesh)
               {
                  spatial.center();
               }
            }
         }
         //      spatial.updateModelBound();
         //      System.out.println(fileName);
         //      System.out.println(spatial.getWorldBound().getCenter());
         //      printSubmeshes(spatial);

         if (appearanceDefinition != null)
         {
            ArrayList<Geometry> geometries = new ArrayList<Geometry>();
            GeometryBatchFactory.gatherGeoms(spatial, geometries);
            Mesh outMesh = new Mesh();
            GeometryBatchFactory.mergeGeometries(geometries, outMesh);

            spatial = new Geometry(spatial.getName(), outMesh);
            setGeometryMaterialBasedOnAppearance(spatial, appearanceDefinition, jmeAssetLocator);

         }

         spatial.updateModelBound();
      }
      catch (Exception e)
      {
         e.printStackTrace();
         spatial = new Node();
      }
      return spatial;
   }

   public static Spatial findSubmesh(Spatial spatial, String submesh, boolean centerSubmesh)
   {

      if (spatial instanceof Node)
      {
         if (spatial.getName().equals(submesh))
         {
            return spatial;
         }

         List<Spatial> spatials = ((Node) spatial).getChildren();
         for (Spatial child : spatials)
         {
            Spatial ret = findSubmesh(child, submesh, centerSubmesh);
            if (ret != null)
            {
               return ret;
            }
         }
      }

      return null;
   }

   public static void printSubmeshes(Spatial spatial)
   {
      System.out.println(spatial.getName() + " " + spatial.getLocalTransform());
      //      spatial.setLocalTranslation(spatial.getLocalTranslation().mult(0.5f));
      if (spatial instanceof Node)
      {

         List<Spatial> spatials = ((Node) spatial).getChildren();
         for (Spatial child : spatials)
         {
            printSubmeshes(child);

         }
      }

   }

   public Node getNode()
   {
      return rootNode;
   }

   @Override
   protected void doAddModelFileInstruction(Graphics3DAddModelFileInstruction graphics3dObjectAddModelFile)
   {

//      jmeAssetLocator.registerAssetDirectories(graphics3dObjectAddModelFile.getResourceDirectories());
      Spatial spatial = createGraphics3DObjectFromModel(graphics3dObjectAddModelFile.getFileName(), graphics3dObjectAddModelFile.getSubmesh(),
            graphics3dObjectAddModelFile.centerSubmesh(), graphics3dObjectAddModelFile.getAppearance(), jmeAssetLocator);
      currentNode.attachChild(spatial);
      if (graphics3dObjectAddModelFile.getAppearance() != null)
      {
         addAppearanceChangedListener(graphics3dObjectAddModelFile, spatial);
      }
   }

   @Override
   protected void doIdentityInstruction()
   {
      currentNode = this.rootNode;
   }

   @Override
   protected void doRotateInstruction(Graphics3DRotateInstruction graphics3dObjectRotateMatrix)
   {
      Quat4d quat4d = new Quat4d();
      RotationFunctions.setQuaternionBasedOnMatrix3d(quat4d, graphics3dObjectRotateMatrix.getRotationMatrix());

      // DON'T USE THIS: the method in Quat4d is flawed and doesn't work for some rotation matrices!
      //      quat4d.set(graphics3dObjectRotateMatrix.getRotationMatrix());

      Quaternion quaternion = new Quaternion();
      JMEDataTypeUtils.packVectMathQuat4dInJMEQuaternion(quat4d, quaternion);

      rotate(quaternion);
   }

   @Override
   protected void doScaleInstruction(Graphics3DScaleInstruction graphics3dObjectScale)
   {
      Node scale = new Node();
      Vector3d scaleFactor = graphics3dObjectScale.getScaleFactor();
      scale.setLocalScale((float) scaleFactor.getX(), (float) scaleFactor.getY(), (float) scaleFactor.getZ());

      currentNode.attachChild(scale);
      currentNode = scale;

      addChangedScaleListener(scale, graphics3dObjectScale);
   }

   private void addChangedScaleListener(final Node scale, Graphics3DScaleInstruction graphics3dObjectScale)
   {
      graphics3dObjectScale.addChangeScaleListener(new ScaleChangedListener()
      {

         public void setScale(final Vector3d scaleFactor)
         {
            checkIfNotImmutable();
            application.enqueue(new Callable<Object>()
            {

               public Object call() throws Exception
               {
                  scale.setLocalScale((float) scaleFactor.getX(), (float) scaleFactor.getY(), (float) scaleFactor.getZ());
                  return null;
               }

            });
         }
      });
   }

   @Override
   protected void doTranslateInstruction(Graphics3DTranslateInstruction graphics3dObjectTranslate)
   {
      Vector3f offset = JMEDataTypeUtils.vecMathTuple3dToJMEVector3f(graphics3dObjectTranslate.getTranslation());
      translate(offset);
   }

   private void setGeometryMaterialBasedOnAppearance(Spatial geometry, AppearanceDefinition appearance)
   {
      setGeometryMaterialBasedOnAppearance(geometry, appearance, jmeAssetLocator);
   }

   private static void setGeometryMaterialBasedOnAppearance(Spatial geometry, AppearanceDefinition appearance, JMEAssetLocator jmeAssetLocator)
   {
	   Material jmeAppearance;
	   try
	   {
		   jmeAppearance = JMEAppearanceMaterial.createMaterial(jmeAssetLocator, appearance);

	   }
	   catch(Exception e)
	   {
		   YoAppearanceRGBColor color = new YoAppearanceRGBColor(0.5, 0.5, 0.5, 0.5);
		   jmeAppearance = JMEAppearanceMaterial.createMaterialFromYoAppearanceRGBColor(jmeAssetLocator, color);
		   System.err.println("JMEGraphicsObject: Couldn't load appearance.");
	   }

      geometry.setMaterial(jmeAppearance);

      if (appearance.getTransparency() < 0.99)
      {
         geometry.setQueueBucket(Bucket.Transparent);
      }
      else
      {
         geometry.setQueueBucket(Bucket.Opaque);
      }
   }

   private void translate(Vector3f offset)
   {
      Node translation = new Node();
      translation.move(offset);
      currentNode.attachChild(translation);
      currentNode = translation;
   }

   private void rotate(Quaternion quaternion)
   {
      Node rotation = new Node();
      rotation.rotate(quaternion);

      currentNode.attachChild(rotation);
      currentNode = rotation;
   }

   @Override
   protected void doAddExtrusionInstruction(final Graphics3DAddExtrusionInstruction graphics3dObjectAddExtrusion)
   {
      BufferedImage bufferedImage = graphics3dObjectAddExtrusion.getBufferedImage();
      AppearanceDefinition appearance = graphics3dObjectAddExtrusion.getAppearance();
      double thickness = graphics3dObjectAddExtrusion.getHeight();
      Geometry textGeometry = getExtrusionGeometry(bufferedImage, thickness, appearance);
      final Node textHolder = new Node();
      textHolder.attachChild(textGeometry);

      currentNode.attachChild(textHolder);

      graphics3dObjectAddExtrusion.setTextChangedListener(new ExtrusionChangedListener()
      {

         public void extrusionChanged(final BufferedImage newImage, final double thickness)
         {
            checkIfNotImmutable();

            application.enqueue(new Callable<Object>()
            {

               public Object call() throws Exception
               {
                  textHolder.detachAllChildren();
                  Geometry textGeometry = getExtrusionGeometry(newImage, thickness, graphics3dObjectAddExtrusion.getAppearance());
                  textHolder.attachChild(textGeometry);
                  addAppearanceChangedListener(graphics3dObjectAddExtrusion, textGeometry);
                  return null;
               }
            });
         }
      });

      addAppearanceChangedListener(graphics3dObjectAddExtrusion, textGeometry);

   }

   private Geometry getExtrusionGeometry(BufferedImage bufferedImage, double thickness, AppearanceDefinition appearance)
   {

      Geometry geometry = ShapeUtilities.createShape(bufferedImage, (float) thickness);

      geometry.scale((float) bufferedImage.getWidth() / (float) bufferedImage.getHeight(), 1.0f, 1.0f);
//      geometry.setLocalRotation(JMEGeometryUtils.getRotationFromJMEToZupCoordinates());

      setGeometryMaterialBasedOnAppearance(geometry, appearance);

      return geometry;
   }

   @Override
   protected void doAddHeightMapInstruction(Graphics3DAddHeightMapInstruction graphics3dObjectAddHeightMap)
   {
      optimizeGraphicsObject = false;

      HeightMap heightMap = graphics3dObjectAddHeightMap.getHeightMap();

      AppearanceDefinition appearanceDefinition = graphics3dObjectAddHeightMap.getAppearance();

      Material material = null;
      if (appearanceDefinition != null)
      {
         material = JMEAppearanceMaterial.createMaterial(jmeAssetLocator, appearanceDefinition);
      }

      AssetManager assetManager = this.jmeAssetLocator.getAssetManager();
      JMEHeightMapTerrain jmeTerrain = new JMEHeightMapTerrain(heightMap, assetManager, material);

      Node terrainNode = jmeTerrain.getTerrain();
      currentNode.attachChild(terrainNode);
      addAppearanceChangedListener(graphics3dObjectAddHeightMap, terrainNode);
   }

   @Override
   protected void doAddMeshDataInstruction(final Graphics3DAddMeshDataInstruction graphics3dObjectAddMeshData)
   {
      MeshDataHolder meshData = graphics3dObjectAddMeshData.getMeshData();
      AppearanceDefinition appearance = graphics3dObjectAddMeshData.getAppearance();

      Mesh mesh = JMEMeshDataInterpreter.interpretMeshData(meshData);
      Geometry geometry = new Geometry("MeshData", mesh);
      setGeometryMaterialBasedOnAppearance(geometry, appearance);

      final Node meshHolder = new Node();
      meshHolder.attachChild(geometry);
      currentNode.attachChild(meshHolder);

      addAppearanceChangedListener(graphics3dObjectAddMeshData, geometry);

      graphics3dObjectAddMeshData.setMeshChangedListener(new MeshChangedListener()
      {

         public void meshChanged(final MeshDataHolder newMesh)
         {
            checkIfNotImmutable();

            application.enqueue(new Callable<Object>()
            {
               public Object call() throws Exception
               {
                  meshHolder.detachAllChildren();
                  Mesh mesh = JMEMeshDataInterpreter.interpretMeshData(newMesh);
                  Geometry geometry = new Geometry("MeshData", mesh);
                  setGeometryMaterialBasedOnAppearance(geometry, graphics3dObjectAddMeshData.getAppearance());
                  meshHolder.attachChild(geometry);
                  return null;
               }
            });

         }
      });
   }

   private void addAppearanceChangedListener(final Graphics3DInstruction instruction, final Spatial spatial)
   {
      instruction.setAppearanceChangedListener(new AppearanceChangedListener()
      {

         public void appearanceChanged(final AppearanceDefinition newAppearance)
         {
            checkIfNotImmutable();

            if (newAppearance != null)
            {
               application.enqueue(new Callable<Object>()
               {
                  public Material call() throws Exception
                  {
                     setGeometryMaterialBasedOnAppearance(spatial, newAppearance);
                     return null;
                  }
               });
            }
         }
      });
   }

   private void checkIfNotImmutable()
   {
      if (immutable)
      {
         throw new RuntimeException("Graphics3DObject is not changable. Call Graphics3dObject.isChangable(true) before adding the graphics object");
      }
   }
}
