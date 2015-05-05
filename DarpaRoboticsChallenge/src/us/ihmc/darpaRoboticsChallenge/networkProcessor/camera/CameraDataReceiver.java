package us.ihmc.darpaRoboticsChallenge.networkProcessor.camera;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import boofcv.struct.calib.IntrinsicParameters;
import us.ihmc.SdfLoader.SDFFullRobotModel;
import us.ihmc.SdfLoader.SDFFullRobotModelFactory;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.packets.sensing.CropVideoPacket;
import us.ihmc.communication.producers.CompressedVideoDataFactory;
import us.ihmc.communication.producers.CompressedVideoHandler;
import us.ihmc.communication.producers.RobotConfigurationDataBuffer;
import us.ihmc.sensorProcessing.sensorData.DRCStereoListener;
import us.ihmc.utilities.VideoDataServer;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.ros.PPSTimestampOffsetProvider;

public abstract class CameraDataReceiver extends Thread implements PacketConsumer<CropVideoPacket>
{
   protected static final boolean DEBUG = false;
   private final VideoDataServer compressedVideoDataServer;
   private final ArrayList<DRCStereoListener> stereoListeners = new ArrayList<DRCStereoListener>();
   private final RobotConfigurationDataBuffer robotConfigurationDataBuffer;

   private final SDFFullRobotModel fullRobotModel;
   private ReferenceFrame cameraFrame;

   private final Point3d cameraPosition = new Point3d();
   private final Quat4d cameraOrientation = new Quat4d();

   private final PPSTimestampOffsetProvider ppsTimestampOffsetProvider;

   private final LinkedBlockingQueue<CameraData> dataQueue = new LinkedBlockingQueue<>();
   private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
   private volatile boolean running = true;
   
   private boolean cropVideo = false;
   private int cropX;
   private int cropY;

   public CameraDataReceiver(SDFFullRobotModelFactory fullRobotModelFactory, String sensorNameInSdf, RobotConfigurationDataBuffer robotConfigurationDataBuffer,
         CompressedVideoHandler compressedVideoHandler, PPSTimestampOffsetProvider ppsTimestampOffsetProvider)
   {
      this.fullRobotModel = fullRobotModelFactory.createFullRobotModel();
      this.ppsTimestampOffsetProvider = ppsTimestampOffsetProvider;
      this.robotConfigurationDataBuffer = robotConfigurationDataBuffer;
      this.cameraFrame = fullRobotModel.getCameraFrame(sensorNameInSdf);

      compressedVideoDataServer = CompressedVideoDataFactory.createCompressedVideoDataServer(compressedVideoHandler);
      
   }

   public void setCameraFrame(ReferenceFrame cameraFrame)
   {
      this.cameraFrame = cameraFrame;
   }

   public ReferenceFrame getHeadFrame()
   {
      return fullRobotModel.getHeadBaseFrame();
   }

   @Override
   public void run()
   {
      while (running)
      {
         try
         {
            CameraData data = dataQueue.take();
            if (data != null)
            {
               readWriteLock.writeLock().lock();

               if (DEBUG)
               {
                  System.out.println("Updating full robot model");
               }
               long robotTimestamp = ppsTimestampOffsetProvider.adjustTimeStampToRobotClock(data.timestamp);
               if (robotConfigurationDataBuffer.updateFullRobotModel(false, robotTimestamp, fullRobotModel, null) < 0)
               {
                  continue;
               }
               cameraFrame.update();
               cameraFrame.getTransformToWorldFrame().get(cameraOrientation, cameraPosition);
               if (DEBUG)
               {
                  System.out.println(cameraFrame.getTransformToParent());
                  System.out.println(cameraPosition);
                  System.out.println(cameraOrientation);
               }
               for (int i = 0; i < stereoListeners.size(); i++)
               {
                  stereoListeners.get(i).newImageAvailable(data.robotSide, data.image, robotTimestamp, data.intrinsicParameters);
               }

               
               BufferedImage image = data.image;
               if (cropVideo)
               {
                  int width = image.getWidth();
                  int height = image.getHeight();

                  int newWidth = width / 2;
                  int newHeight = height / 2;

                  int x = (newWidth * cropX) / 100;
                  int y = (newHeight * cropY) / 100;

                  BufferedImage croppedImage = new BufferedImage(newWidth, newHeight, image.getType());
                  Graphics2D graphics = croppedImage.createGraphics();
                  graphics.drawImage(image, 0, 0, newWidth, newHeight, x, y, x + newWidth, y + newHeight, null);
                  graphics.dispose();
                  image = croppedImage;
               }
               
               
               compressedVideoDataServer.updateImage(data.robotSide, image, robotTimestamp, cameraPosition, cameraOrientation, data.intrinsicParameters);
               readWriteLock.writeLock().unlock();
            }
         }
         catch (InterruptedException e)
         {
            continue;
         }
      }

   }

   protected void updateImage(RobotSide robotSide, BufferedImage bufferedImage, long timeStamp, IntrinsicParameters intrinsicParameters)
   {
      try
      {
         dataQueue.put(new CameraData(robotSide, bufferedImage, timeStamp, intrinsicParameters));
      }
      catch (InterruptedException e)
      {
      }
   }

   public void registerCameraListener(DRCStereoListener drcStereoListener)
   {
      stereoListeners.add(drcStereoListener);
   }

   private static class CameraData
   {
      private final RobotSide robotSide;
      private final BufferedImage image;
      private final long timestamp;
      private final IntrinsicParameters intrinsicParameters;

      public CameraData(RobotSide robotSide, BufferedImage image, long timestamp, IntrinsicParameters intrinsicParameters)
      {
         this.robotSide = robotSide;
         this.image = image;
         this.timestamp = timestamp;
         this.intrinsicParameters = intrinsicParameters;
      }

   }

   public void receivedPacket(CropVideoPacket packet)
   {
      cropX = MathTools.clipToMinMax(packet.cropX(), 0, 100);
      cropY = MathTools.clipToMinMax(packet.cropY(), 0, 100);
      cropVideo = packet.crop();
   }

}
