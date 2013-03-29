package us.ihmc.graphics3DAdapter.camera;

import java.awt.image.BufferedImage;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.graphics3DAdapter.CameraAdapter;
import us.ihmc.graphics3DAdapter.Graphics3DAdapter;

public class OffscreenBufferVideoServer
{

   private final VideoDataServer videoDataServer;

   private final CameraAdapter camera;


   public OffscreenBufferVideoServer(Graphics3DAdapter adapter, CameraMountList mountList, CameraConfiguration cameraConfiguration,
         CameraTrackingAndDollyPositionHolder cameraTrackingAndDollyPositionHolder, VideoSettings settings, VideoDataServer videoDataServer)
   {
      ViewportAdapter viewport = adapter.createNewViewport(null, false, true);
      camera = viewport.getCamera();
      viewport.setupOffscreenView(settings.getWidth(), settings.getHeight());

      ClassicCameraController cameraController = new ClassicCameraController(adapter, viewport, cameraTrackingAndDollyPositionHolder);
      cameraController.setConfiguration(cameraConfiguration, mountList);
      viewport.setCameraController(cameraController);

      CameraUpdater cameraUpdater = new CameraUpdater();
      this.videoDataServer = videoDataServer;
      

      viewport.getCaptureDevice().streamTo(cameraUpdater, 25);

   }

   public void close()
   {
      videoDataServer.close();
   }

   private class CameraUpdater implements CameraStreamer
   {

      public void updateImage(BufferedImage bufferedImage, long timeStamp, Point3d cameraPosition, Quat4d cameraOrientation, double fov)
      {
         videoDataServer.updateImage(bufferedImage, timeStamp, cameraPosition, cameraOrientation, fov);
      }

      public Point3d getCameraPosition()
      {
         return camera.getCameraPosition();
      }

      public Quat4d getCameraOrientation()
      {
         return camera.getCameraRotation();
      }

      public double getFieldOfView()
      {
         return camera.getHorizontalFovInRadians();
      }

      public boolean isReadyForNewData()
      {
         return videoDataServer.isConnected();
      }

      public long getTimeStamp()
      {
         return System.nanoTime();
      }
      
      
   }
}
