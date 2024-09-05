package us.ihmc.perception.videoStreaming;

import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import us.ihmc.commons.thread.Notification;
import us.ihmc.log.LogTools;

import java.net.InetSocketAddress;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;

/**
 * Demo for the SRTVideoStreamer. Grabs images from a webcam
 * and give them to the streamer to be streamed over SRT.
 * To view the stream, use the following command:
 * {@code ffplay srt:127.0.0.1:60001 -fflags nobuffer}
 */
public class SRTVideoStreamerDemo
{
   private static final InetSocketAddress CALLER_ADDRESS = InetSocketAddress.createUnresolved("127.0.0.1", 60001);

   private final VideoCapture videoCapture;
   private final Mat frame;
   private final SRTVideoStreamer videoStreamer;

   private boolean shutdown = false;
   private final Notification shutdownReady = new Notification();

   private SRTVideoStreamerDemo()
   {
      // Open a webcam
      videoCapture = new VideoCapture(-1);

      frame = new Mat();

      // Read image data from webcam
      int imageWidth = (int) videoCapture.get(opencv_videoio.CAP_PROP_FRAME_WIDTH);
      int imageHeight = (int) videoCapture.get(opencv_videoio.CAP_PROP_FRAME_HEIGHT);
      double reportedFPS = videoCapture.get(opencv_videoio.CAP_PROP_FPS);

      // Create and initialize the video streamer
      videoStreamer = new SRTVideoStreamer();
      videoStreamer.initialize(imageWidth, imageHeight, reportedFPS, AV_PIX_FMT_BGR24);
      videoStreamer.queueCallerToConnect(CALLER_ADDRESS);

      Runtime.getRuntime().addShutdownHook(new Thread(this::destroy, "SRTStreamerDemoDestruction"));

      run();
   }

   private void run()
   {
      LogTools.info("Connecting to caller...");
      videoStreamer.connectToNewCallers();

      LogTools.info("Got a connection! Streaming!");
      while (!shutdown && videoStreamer.totalCallerCount() > 0)
      {
         videoCapture.read(frame);
         videoStreamer.sendFrame(frame);
      }

      shutdownReady.set();
   }

   private void destroy()
   {
      shutdown = true;

      shutdownReady.blockingPoll();

      videoCapture.close();
      videoStreamer.destroy();
      frame.close();
   }

   public static void main(String[] args)
   {
      new SRTVideoStreamerDemo();
   }
}
