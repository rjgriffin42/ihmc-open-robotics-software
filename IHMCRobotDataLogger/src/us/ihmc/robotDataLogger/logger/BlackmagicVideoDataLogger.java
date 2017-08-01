package us.ihmc.robotDataLogger.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import us.ihmc.commons.Conversions;
import us.ihmc.javadecklink.Capture;
import us.ihmc.javadecklink.Capture.CodecID;
import us.ihmc.javadecklink.CaptureHandler;
import us.ihmc.robotDataLogger.LogProperties;
import us.ihmc.tools.maps.CircularLongMap;

public class BlackmagicVideoDataLogger extends VideoDataLoggerInterface implements CaptureHandler
{

   /**
    * Make sure to set a progressive mode, otherwise the timestamps will be all wrong!
    */

   private final int decklink;
   private final YoVariableLoggerOptions options;
   private Capture capture;
   
   private final CircularLongMap circularLongMap = new CircularLongMap(10000);

   private FileWriter timestampWriter;
   
   private int frame;

   public BlackmagicVideoDataLogger(String name, File logPath, LogProperties logProperties, int decklinkID, YoVariableLoggerOptions options) throws IOException
   {
      super(logPath, logProperties, name);
      this.decklink = decklinkID;
      this.options = options;

      createCaptureInterface();
   }
   
   private void createCaptureInterface()
   {
      File timestampFile = new File(timestampData);
      
      switch(options.getVideoCodec())
      {
      case AV_CODEC_ID_H264:
         capture = new Capture(this, CodecID.AV_CODEC_ID_H264);
         capture.setOption("g", "1");
         capture.setOption("crf", String.valueOf(options.getCrf()));
         break;
      case AV_CODEC_ID_MJPEG:
         capture = new Capture(this, CodecID.AV_CODEC_ID_H264);
         capture.setMJPEGQuality(options.getVideoQuality());
         break;
         default: throw new RuntimeException();
      }
      
      
      
      try
      {
         timestampWriter = new FileWriter(timestampFile);
         capture.startCapture(videoFile, decklink);
      }
      catch (IOException e)
      {
         capture = null;
         if(timestampWriter != null)
         {
            try
            {
               timestampWriter.close();
               timestampFile.delete();
            }
            catch (IOException e1)
            {
            }
         }
         timestampWriter = null;
         System.out.println("Cannot start capture interface");
         e.printStackTrace();
      }
   }
   
   /* (non-Javadoc)
    * @see us.ihmc.robotDataLogger.logger.VideoDataLoggerInterface#restart()
    */
   @Override
   public void restart() throws IOException
   {
      close();
      removeLogFiles();
      createCaptureInterface();
   }

   /* (non-Javadoc)
    * @see us.ihmc.robotDataLogger.logger.VideoDataLoggerInterface#timestampChanged(long)
    */
   @Override
   public void timestampChanged(long newTimestamp)
   {
      if(capture != null)
      {
         long hardwareTimestamp = capture.getHardwareTime();
         if(hardwareTimestamp != -1)
         {
            circularLongMap.insert(hardwareTimestamp, newTimestamp);
         }
      }
   }

   /* (non-Javadoc)
    * @see us.ihmc.robotDataLogger.logger.VideoDataLoggerInterface#close()
    */
   @Override
   public void close()
   {
      System.out.println("Signalling recorder");
      if(capture != null)
      {
         try
         {
            capture.stopCapture();
            timestampWriter.close();
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
         capture = null;
         timestampWriter = null;
      }
      
   }

   @Override
   public void receivedFrameAtTime(long hardwareTime, long pts, long timeScaleNumerator, long timeScaleDenumerator)
   {
      
      if(circularLongMap.size() > 0)
      {
         if(frame % 60 == 0)
         {
            System.out.println("[Decklink " + decklink + "] Received frame " + frame + " at time " + hardwareTime + "ns, delay: " + Conversions.nanosecondsToSeconds(circularLongMap.getLatestKey() - hardwareTime) + "s. pts: " + pts);
         }

         
         long robotTimestamp = circularLongMap.getValue(true, hardwareTime);
         
         try
         {
            if(frame == 0)
            {
               timestampWriter.write(timeScaleNumerator + "\n");
               timestampWriter.write(timeScaleDenumerator + "\n");
            }
            timestampWriter.write(robotTimestamp + " " + pts + "\n");
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
         ++frame;
      }
      
   }



}
