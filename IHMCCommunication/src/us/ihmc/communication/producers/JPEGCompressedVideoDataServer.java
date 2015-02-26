package us.ihmc.communication.producers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.codecs.generated.YUVPicture;
import us.ihmc.codecs.generated.YUVPicture.YUVSubsamplingType;
import us.ihmc.codecs.yuv.JPEGEncoder;
import us.ihmc.codecs.yuv.YUVPictureConverter;
import us.ihmc.utilities.robotSide.RobotSide;

public class JPEGCompressedVideoDataServer implements CompressedVideoDataServer
{
   private final YUVPictureConverter converter = new YUVPictureConverter();
   private final JPEGEncoder encoder = new JPEGEncoder();
   private final CompressedVideoHandler handler;
   
   public JPEGCompressedVideoDataServer(CompressedVideoHandler handler)
   {
      this.handler = handler;
   }

   @Override
   public void updateImage(RobotSide robotSide, BufferedImage bufferedImage, long timeStamp, Point3d cameraPosition, Quat4d cameraOrientation, double fov)
   {
      YUVPicture picture = converter.fromBufferedImage(bufferedImage, YUVSubsamplingType.YUV420);
      try
      {
         ByteBuffer buffer = encoder.encode(picture, 90);
         byte[] data =  new byte[buffer.remaining()];
         buffer.get(data);
         handler.newVideoPacketAvailable(robotSide, timeStamp, data, cameraPosition, cameraOrientation, fov);
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      picture.delete();
   }

   @Override
   public void close()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean isConnected()
   {
      return handler.isConnected();
   }

   @Override
   public void setVideoControlSettings(VideoControlSettings object)
   {
      // TODO Auto-generated method stub
      
   }
}
