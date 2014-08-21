package us.ihmc.graphics3DAdapter;

import java.awt.Canvas;
import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.graphics3DAdapter.camera.CameraController;
import us.ihmc.graphics3DAdapter.camera.CameraStreamer;
import us.ihmc.graphics3DAdapter.camera.CaptureDevice;
import us.ihmc.graphics3DAdapter.camera.ViewportAdapter;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.input.KeyListener;
import us.ihmc.graphics3DAdapter.input.MouseListener;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.utilities.lidar.polarLidar.geometry.LidarScanParameters;

public class NullGraphics3DAdapter implements Graphics3DAdapter
{
   private final Object graphicsConch = new Object();

   public void setupSky()
   {
      
   }
   
   public void addRootNode(Graphics3DNode rootNode)
   {
   }

   public void removeRootNode(Graphics3DNode rootNode)
   {
   }

   public ViewportAdapter createNewViewport(GraphicsDevice graphicsDevice, boolean isMainViewport, boolean isOffScreen)
   {
      return new ViewportAdapter()
      {
         public void setupOffscreenView(int width, int height)
         {
         }

         public void setCameraController(CameraController cameraController)
         {
         }

         public double[][] getZBuffer()
         {
            return new double[1][1];
         }

         public Point3d getWorldCoordinatesFromScreenCoordinates(float f, float g, double z)
         {
            return new Point3d();
         }

         public double getPhysicalWidth()
         {
            return 1;
         }

         public double getPhysicalHeight()
         {
            return 1;
         }

         public double getFieldOfView()
         {
            return Math.PI;
         }

         public CaptureDevice getCaptureDevice()
         {
            return new CaptureDevice()
            {
               public void streamTo(CameraStreamer cameraStreamer, int framesPerSecond)
               {
               }

               public void setSize(int width, int height)
               {
               }

               public int getWidth()
               {
                  return 1;
               }

               public int getHeight()
               {
                  return 1;
               }

               public BufferedImage exportSnapshotAsBufferedImage()
               {
                  return new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
               }

               public void exportSnapshot(File snapshotFile)
               {
               }
            };
         }

         public Canvas getCanvas()
         {
            return new Canvas();
         }

         public CameraController getCameraController()
         {
            return new CameraController()
            {
               public double getHorizontalFieldOfViewInRadians()
               {
                  // TODO Auto-generated method stub
                  return Math.PI;
               }

               public double getClipNear()
               {
                  // TODO Auto-generated method stub
                  return 0.1;
               }

               public double getClipFar()
               {
                  // TODO Auto-generated method stub
                  return 100.0;
               }

               public void computeTransform(Transform3D cameraTransform)
               {
                  // TODO Auto-generated method stub

               }
            };
         }

         public CameraAdapter getCamera()
         {
            return new CameraAdapter()
            {
               public float getHorizontalFovInRadians()
               {
                  return (float) Math.PI;
               }

               public Quat4d getCameraRotation()
               {
                  // TODO Auto-generated method stub
                  return new Quat4d();
               }

               public Point3d getCameraPosition()
               {
                  return new Point3d();
               }
            };
         }

         public void addContextSwitchedListener(ContextSwitchedListener contextSwitchedListener)
         {
         }
      };
   }

   public void closeViewport(ViewportAdapter viewport)
   {
   }

   public void setHeightMap(HeightMap heightMap)
   {
   }

   public Object getGraphicsConch()
   {
      return graphicsConch;
   }

   public void setGroundVisible(boolean isVisible)
   {
   }

   public void addSelectedListener(SelectedListener selectedListener)
   {
   }

   public void addKeyListener(KeyListener keyListener)
   {
   }

   public void addMouseListener(MouseListener mouseListener)
   {
   }

   public void closeAndDispose()
   {
   }

   public void setBackgroundColor(Color3f color)
   {
   }

   public void setBackgroundImage(URL fileURL, Graphics3DBackgroundScaleMode backgroundScaleMode)
   {
   }

   public void setGroundAppearance(AppearanceDefinition app)
   {
   }

   public void freezeFrame(Graphics3DNode rootJoint)
   {
   }

   public ContextManager getContextManager()
   {
      return new ContextManager()
      {
         public ViewportAdapter getCurrentViewport()
         {
            return null;
         }
      };
   }

   public GPULidar createGPULidar(GPULidarListener callback, int horizontalRays, double fov, double near, double far)
   {
      return null;
   }

   public GPULidar createGPULidar(LidarScanParameters lidarScanParameters)
   {
      return null;
   }

}
