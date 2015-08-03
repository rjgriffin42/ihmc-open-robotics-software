package us.ihmc.graphics3DAdapter.jme;

import java.awt.GraphicsDevice;
import java.net.URL;

import javax.vecmath.Color3f;

import us.ihmc.graphics3DAdapter.ContextManager;
import us.ihmc.graphics3DAdapter.GPULidarListener;
import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.Graphics3DBackgroundScaleMode;
import us.ihmc.graphics3DAdapter.HeightMap;
import us.ihmc.graphics3DAdapter.camera.ViewportAdapter;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.jme.JMERenderer.RenderType;
import us.ihmc.graphics3DAdapter.jme.lidar.JMEGPULidar;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.utilities.inputDevices.keyboard.KeyListener;
import us.ihmc.utilities.inputDevices.mouse.MouseListener;
import us.ihmc.utilities.inputDevices.mouse3DJoystick.Mouse3DListener;
import us.ihmc.utilities.lidar.LidarScanParameters;

/*
* Pass-through class to avoid having to import JME on all projects
 */
public class JMEGraphics3DAdapter implements Graphics3DAdapter
{
   private JMERenderer jmeRenderer = new JMERenderer(RenderType.AWTPANELS);

   public JMEGraphics3DAdapter()
   {
      this(true);
   }
   
   public JMEGraphics3DAdapter(boolean setupSky)
   {
      if (setupSky)
      {
         jmeRenderer.setupSky();
      }
   }
   
   public void setupSky()
   {
      jmeRenderer.setupSky();
   }

   public void addRootNode(Graphics3DNode rootNode)
   {
      jmeRenderer.addRootNode(rootNode);
   }

   public void removeRootNode(Graphics3DNode rootNode)
   {
      jmeRenderer.removeRootNode(rootNode);
   }

   public Object getGraphicsConch()
   {
      return jmeRenderer.getGraphicsConch();
   }

   public void setHeightMap(HeightMap heightMap)
   {
      jmeRenderer.setHeightMap(heightMap);
   }

   public void setGroundVisible(boolean isVisible)
   {
      jmeRenderer.setGroundVisible(isVisible);
   }

   public void addSelectedListener(SelectedListener selectedListener)
   {
      jmeRenderer.addSelectedListener(selectedListener);
   }

   public ViewportAdapter createNewViewport(GraphicsDevice graphicsDevice, boolean isMainViewport, boolean isOffScreen)
   {
      return jmeRenderer.createNewViewport(graphicsDevice, isMainViewport, isOffScreen);
   }

   public void closeAndDispose()
   {
      jmeRenderer.closeAndDispose();
      jmeRenderer = null;
   }

   public void setBackgroundColor(Color3f color)
   {
      jmeRenderer.setBackgroundColor(color);
   }

   public void setBackgroundImage(URL fileURL, Graphics3DBackgroundScaleMode backgroundScaleMode)
   {
      jmeRenderer.setBackgroundImage(fileURL, backgroundScaleMode);
   }

   public void setGroundAppearance(AppearanceDefinition app)
   {
      jmeRenderer.setGroundAppearance(app);
   }

   public void addKeyListener(KeyListener keyListener)
   {
      jmeRenderer.addKeyListener(keyListener);
   }

   public void addMouseListener(MouseListener mouseListener)
   {
      jmeRenderer.addMouseListener(mouseListener);
   }

   @Override
   public void addMouse3DListener(Mouse3DListener mouse3DListener)
   {
      jmeRenderer.addMouse3DListener(mouse3DListener);
   }

   public void freezeFrame(Graphics3DNode rootJoint)
   {
      jmeRenderer.freezeFrame(rootJoint);
   }

   public ContextManager getContextManager()
   {
      return jmeRenderer.getContextManager();
   }

   public void closeViewport(ViewportAdapter viewport)
   {
      if (jmeRenderer != null) jmeRenderer.closeViewport(viewport);
   }

   public JMERenderer getRenderer()
   {
      return jmeRenderer;
   }
   
   @Override
   public JMEGPULidar createGPULidar(int pointsPerSweep, double fieldOfView, double minRange, double maxRange)
   {
      return jmeRenderer.createGPULidar(pointsPerSweep, fieldOfView, minRange, maxRange);
   }

   @Override
   public JMEGPULidar createGPULidar(GPULidarListener listener, int pointsPerSweep, double fieldOfView, double minRange, double maxRange)
   {
      return jmeRenderer.createGPULidar(listener, pointsPerSweep, fieldOfView, minRange, maxRange);
   }
   
   @Override
   public JMEGPULidar createGPULidar(GPULidarListener listener, LidarScanParameters lidarScanParameters)
   {
      return jmeRenderer.createGPULidar(listener, lidarScanParameters);
   }

   @Override
   public JMEGPULidar createGPULidar(LidarScanParameters lidarScanParameters)
   {
      return jmeRenderer.createGPULidar(lidarScanParameters);
   }
}
