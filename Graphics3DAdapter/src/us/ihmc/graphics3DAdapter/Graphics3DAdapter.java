package us.ihmc.graphics3DAdapter;

import java.awt.GraphicsDevice;
import java.net.URL;

import javax.vecmath.Color3f;

import us.ihmc.graphics3DAdapter.camera.ViewportAdapter;
import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.input.KeyListener;
import us.ihmc.graphics3DAdapter.input.MouseListener;
import us.ihmc.graphics3DAdapter.input.SelectedListener;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;

public interface Graphics3DAdapter
{
   public void addRootNode(Graphics3DNode rootNode);
   public void removeRootNode(Graphics3DNode rootNode);
   
   public ViewportAdapter createNewViewport(GraphicsDevice graphicsDevice, boolean isMainViewport, boolean isOffScreen);
   public void closeViewport(ViewportAdapter viewport);
   
   public void setHeightMap(HeightMap heightMap);
   
   public Object getGraphicsConch();
   public void setGroundVisible(boolean isVisible);
   
   public void addSelectedListener(SelectedListener selectedListener);
   public void addKeyListener(KeyListener keyListener);
   public void addMouseListener(MouseListener mouseListener);
   
   public void closeAndDispose();
   
   public void setBackgroundColor(Color3f color);

   public void setBackgroundImage(URL fileURL, Graphics3DBackgroundScaleMode backgroundScaleMode);

   public void setGroundAppearance(AppearanceDefinition app);
   public void freezeFrame(Graphics3DNode rootJoint);

   public ContextManager getContextManager();
   
   public GPULidar createGPULidar(GPULidarCallback callback, int horizontalRays, double fov, double near, double far);
}
