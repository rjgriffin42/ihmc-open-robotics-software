package us.ihmc.robotDataCommunication.logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import us.ihmc.codecs.builder.MP4MJPEGMovieBuilder;
import us.ihmc.multicastLogDataProtocol.LogPacketHandler;
import us.ihmc.multicastLogDataProtocol.LogUtils;
import us.ihmc.multicastLogDataProtocol.SegmentedDatagramClient;
import us.ihmc.multicastLogDataProtocol.SegmentedPacketBuffer;
import us.ihmc.multicastLogDataProtocol.broadcast.AnnounceRequest;
import us.ihmc.robotDataCommunication.gui.GUICaptureStreamer;

import com.esotericsoftware.kryo.io.ByteBufferInputStream;

public class NetworkStreamVideoDataLogger extends VideoDataLoggerInterface implements LogPacketHandler
{
   private final static String description = "NetworkStream";
   private final SegmentedDatagramClient client;
   
   private MP4MJPEGMovieBuilder builder;
   private PrintStream timestampStream;
   private int dts = 0;
   
   private volatile long timestamp = 0;
   
   public NetworkStreamVideoDataLogger(AnnounceRequest request, File logPath, LogProperties logProperties, YoVariableLoggerOptions options, InetSocketAddress address) throws SocketException, UnknownHostException
   {
      super(logPath, logProperties, description, false);
      
      NetworkInterface iface = NetworkInterface.getByInetAddress(LogUtils.getMyIP(request.getControlIP()));

      System.out.println("Connecting network stream to " + iface);
      client = new SegmentedDatagramClient(GUICaptureStreamer.MAGIC_SESSION_ID, iface,
            InetAddress.getByAddress(request.getVideoStream())
            , request.getVideoPort(), this);
      client.start();
   }

   @Override
   public void timestampChanged(long newTimestamp)
   {
      this.timestamp = newTimestamp;
   }

   @Override
   public void restart() throws IOException
   {
      try
      {
         if(builder != null)
         {
            builder.close();
         }
         if(timestampStream != null)
         {
            timestampStream.close();
         }
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      builder = null;
      timestampStream = null;
      removeLogFiles();
   }

   @Override
   public void close()
   {
      client.close();
      try
      {
         if(builder != null)
         {
            builder.close();
         }
         if(timestampStream != null)
         {
            timestampStream.close();
         }
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      builder = null;
      timestampStream = null;
   }
   
   @Override
   public void timestampReceived(long timestamp)
   {
      
   }

   @Override
   public void newDataAvailable(SegmentedPacketBuffer buffer)
   {
      ByteBuffer byteBuffer = buffer.getBuffer();
      if(builder == null)
      {
         try
         {
            ByteBufferInputStream is = new ByteBufferInputStream();
            is.setByteBuffer(byteBuffer);
            BufferedImage img = ImageIO.read(is);
            if(img == null)
            {
               System.err.println("Cannot decode image");
               return;
            }
            File videoFileFile = new File(videoFile);
            builder = new MP4MJPEGMovieBuilder(videoFileFile, img.getWidth(), img.getHeight(), 10, 0.9f);
            File timestampFile = new File(timestampData);
            timestampStream = new PrintStream(timestampFile);
            timestampStream.println("1");
            timestampStream.println("10");
            byteBuffer.clear();
            
            dts = 0;
         }
         catch (IOException e)
         {
            e.printStackTrace();
            return;
         }
         
         
      }
      try
      {
         builder.encodeFrame(byteBuffer);
         timestampStream.println(timestamp + " " +  dts);
         dts++;
      }
      catch (IOException e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   public void timeout(long timeoutInMillis)
   {
      
   }

}
