package us.ihmc.sensorProcessing.pointClouds.shape;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Iterator;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;



public class PointCloud
{
   protected AssetManager assetManager;

   public PointCloud(AssetManager assetManager)
   {
      this.assetManager = assetManager;
   }

   protected Node generatePointCloudGraphFrom(FloatBuffer pointCoordinates3d)
   {
      FloatBuffer colors = createColorBuffer(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f), pointCoordinates3d);

      return generatePointCloudGraphFrom(pointCoordinates3d, colors);
   }


   protected Node generatePointCloudGraphFrom(FloatBuffer pointCoordinates3d, FloatBuffer colorsRGBA)
   {
      FloatBuffer sizes = createSizeBuffer(1.0f, pointCoordinates3d);

      return generatePointCloudGraphFrom(pointCoordinates3d, colorsRGBA, sizes);
   }


   protected Node generatePointCloudGraphFrom(FloatBuffer pointCoordinates3d, FloatBuffer colorsRGBA, FloatBuffer sizes)
   {
      Node result = new Node();

      Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
      mat.getAdditionalRenderState().setPointSprite(true);

//    mat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
      mat.setBoolean("PointSprite", true);
      mat.setFloat("Quadratic", 0.75f);

      Mesh m = new Mesh();
      m.setMode(Mode.Points);
      m.setBuffer(VertexBuffer.Type.Position, 3, pointCoordinates3d);
      m.setBuffer(VertexBuffer.Type.Color, 4, colorsRGBA);
      m.setBuffer(VertexBuffer.Type.Size, 1, sizes);
      m.setStatic();
      m.updateBound();

      Geometry g = new Geometry("Point Cloud", m);
      g.setShadowMode(ShadowMode.CastAndReceive);
      g.setQueueBucket(Bucket.Transparent);
      g.setCullHint(CullHint.Dynamic);

      g.setMaterial(mat);
      g.updateModelBound();

      result.attachChild(g);
      result.updateModelBound();

      return result;
   }

   protected FloatBuffer createColorBuffer(ColorRGBA color, FloatBuffer points)
   {
      int bufferSize = (points.limit() / 3) * 4;
      FloatBuffer result = BufferUtils.createFloatBuffer(bufferSize);
      for (int i = 0; i < (bufferSize / 4); i++)
      {
         result.put(color.r).put(color.g).put(color.b).put(color.a);
      }

      return result;
   }

   protected FloatBuffer createSizeBuffer(float pointSize, FloatBuffer points)
   {
      int bufferSize = points.limit() / 3;
      FloatBuffer result = BufferUtils.createFloatBuffer(bufferSize);
      for (int i = 0; i < bufferSize; i++)
      {
         result.put(pointSize);
      }

      return result;
   }


   public Node generatePointCloudGraph(float[] pointCoordinates3d) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");

      if ((pointCoordinates3d.length % 3) != 0)
         throw new Exception("number of point coordinates must be a multiple of 3!");

      FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

      return generatePointCloudGraph(coords, null);
   }


   public Node generatePointCloudGraph(FloatBuffer pointCoordinates3d) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");

      FloatBuffer coords = pointCoordinates3d;

      return generatePointCloudGraph(coords, null);
   }


   public Node generatePointCloudGraph(Vector3f[] pointCoordinates3d) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");

      FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

      return generatePointCloudGraph(coords, null);
   }


   public Node generatePointCloudGraph(Collection<Vector3f> pointCoordinates3d) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");

      FloatBuffer coords = BufferUtils.createFloatBuffer(3 * pointCoordinates3d.size());
      Iterator<Vector3f> it = pointCoordinates3d.iterator();
      Vector3f current;
      while (it.hasNext())
      {
         current = it.next();
         coords.put(current.x).put(current.y).put(current.z);
      }

      coords.rewind();

      return generatePointCloudGraph(coords, null);
   }


   public Node generatePointCloudGraph(float[] pointCoordinates3d, float[] colorsRGBA) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");
      if (colorsRGBA == null)
         return generatePointCloudGraph(pointCoordinates3d);

      if ((pointCoordinates3d.length % 3) != 0)
         throw new NumberFormatException("number of point coordinates must be a multiple of 3!");

      if ((colorsRGBA.length % 4) != 0)
         throw new NumberFormatException("number of color values must be a multiple of 4!");

      if (pointCoordinates3d.length / 3 != colorsRGBA.length / 4)
         throw new Exception("There should be a color value for each point, if colors are used!");

      FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

      FloatBuffer colors = BufferUtils.createFloatBuffer(colorsRGBA);

      return generatePointCloudGraph(coords, colors);
   }


   public Node generatePointCloudGraph(FloatBuffer pointCoordinates3d, FloatBuffer colorsRGBA) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");

      // now - this method calls the main generator function:
      if (colorsRGBA == null)
         return generatePointCloudGraphFrom(pointCoordinates3d);
      else
         return generatePointCloudGraphFrom(pointCoordinates3d, colorsRGBA);
   }


   public Node generatePointCloudGraph(Vector3f[] pointCoordinates3d, ColorRGBA[] colorsRGBA) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud mustn'nt be null!");
      if (colorsRGBA == null)
         return generatePointCloudGraph(pointCoordinates3d);

      if (pointCoordinates3d.length != colorsRGBA.length)
         throw new Exception("There should be a color value for each point, if colors are used!");

      FloatBuffer coords = BufferUtils.createFloatBuffer(pointCoordinates3d);

      FloatBuffer colors = BufferUtils.createFloatBuffer(4 * colorsRGBA.length);
      for (int i = 0; i < colorsRGBA.length; i++)
      {
         colors.put(colorsRGBA[i].r).put(colorsRGBA[i].g).put(colorsRGBA[i].b).put(colorsRGBA[i].a);
      }

      colors.rewind();

      return generatePointCloudGraph(coords, colors);
   }


   public Node generatePointCloudGraph(Collection<Vector3f> pointCoordinates3d, Collection<ColorRGBA> colorsRGBA) throws Exception
   {
      if (pointCoordinates3d == null)
         throw new Exception("point cloud must not be null!");
      if (colorsRGBA == null)
         return generatePointCloudGraph(pointCoordinates3d);

      if (pointCoordinates3d.size() != colorsRGBA.size())
         throw new Exception("There should be a color value for each point, if colors are used!");

      FloatBuffer coords = BufferUtils.createFloatBuffer(3 * pointCoordinates3d.size());
      Iterator<Vector3f> it = pointCoordinates3d.iterator();
      Vector3f current;
      while (it.hasNext())
      {
         current = it.next();
         coords.put(current.x).put(current.y).put(current.z);
      }

      coords.rewind();

      FloatBuffer colors = BufferUtils.createFloatBuffer(4 * colorsRGBA.size());
      Iterator<ColorRGBA> it2 = colorsRGBA.iterator();
      ColorRGBA current2;
      while (it2.hasNext())
      {
         current2 = it2.next();
         colors.put(current2.r).put(current2.g).put(current2.b).put(current2.a);
      }

      colors.rewind();

      return generatePointCloudGraph(coords, colors);
   }

}
