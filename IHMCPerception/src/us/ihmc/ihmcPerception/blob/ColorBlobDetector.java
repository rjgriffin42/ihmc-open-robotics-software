package us.ihmc.ihmcPerception.blob;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.vecmath.Point2f;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import us.ihmc.ihmcPerception.OpenCVTools;
import us.ihmc.tools.FormattingTools;
import us.ihmc.tools.nativelibraries.NativeLibraryLoader;
import us.ihmc.tools.time.Timer;

/**
 * Taken from https://github.com/Itseez/opencv/blob/master/samples/android/color-blob-detection/src/org/opencv/samples/colorblobdetect/ColorBlobDetector.java
 */
public class ColorBlobDetector
{
   static
   {
      NativeLibraryLoader.loadLibrary("org.opencv", "opencv_java2411");
   }

   public static final HueSaturationValueRange TIGA_ORANGE_PING_PONG_BALL_HSV_RANGE_MAC = new HueSaturationValueRange(21, 27, 130, 230, 180, 255);
   public static final HueSaturationValueRange TIGA_ORANGE_PING_PONG_BALL_HSV_RANGE_KINECT = new HueSaturationValueRange(15, 50, 130, 230, 180, 255);
   public static final int TIGA_ORANGE_PING_PONG_BALL_SIZE = 5;
   
   public static Mat convertBufferedImageToHSV(BufferedImage bufferedImage)
   {
      Mat matImage;
      byte[] pixelBuffer;
      switch (bufferedImage.getType())
      {
      case BufferedImage.TYPE_3BYTE_BGR:
         matImage = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC3);
         pixelBuffer = ((DataBufferByte) (bufferedImage.getRaster().getDataBuffer())).getData();
         matImage.put(0, 0, pixelBuffer);
         convertImageFromBGRToHSV(matImage, matImage);
         return matImage;
      case BufferedImage.TYPE_4BYTE_ABGR:
      case BufferedImage.TYPE_INT_ARGB:
         //need to double check endianess
         matImage = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8UC4);
         pixelBuffer = new byte[bufferedImage.getHeight() * bufferedImage.getWidth() * 4];
         ByteBuffer pixelBuf = ByteBuffer.wrap(pixelBuffer);
         int[] intBuf = bufferedImage.getRGB(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null, 0, bufferedImage.getWidth());
         pixelBuf.asIntBuffer().put(intBuf);
         matImage.put(0, 0, pixelBuffer);
         convertImageFromRGBToHSV(matImage, matImage);
         return matImage;
      default:
         throw new RuntimeException("Unknown type: " + bufferedImage.getType());
      }
   }
   
   public static void convertImageFromBGRToHSV(Mat bgrImage, Mat hsvResult)
   {
      Imgproc.cvtColor(bgrImage, hsvResult, Imgproc.COLOR_BGR2HSV_FULL);
   }
   
   public static void convertImageFromRGBToHSV(Mat rgbImage, Mat hsvResult)
   {
      Imgproc.cvtColor(rgbImage, hsvResult, Imgproc.COLOR_RGB2HSV_FULL);
   }
   
   public static void thresholdImage(Mat hsvImage, Mat thresholdedImage, HueSaturationValueRange hsvRange)
   {
      Core.inRange(hsvImage, hsvRange.getMin(), hsvRange.getMax(), thresholdedImage);
   }
   
   public static void morphologicallyOpen(Mat hsvImage, int size)
   {
      Imgproc.erode(hsvImage, hsvImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(size, size)));
      Imgproc.dilate(hsvImage, hsvImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(size, size)));
   }
   
   public static void morphologicallyClose(Mat hsvImage, int size)
   {
      Imgproc.dilate(hsvImage, hsvImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(size, size)));
      Imgproc.erode(hsvImage, hsvImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(size, size)));
   }
   
   public static Point2f findBlobFromThresholdImage(Mat thresholdedImage)
   {
      Moments moments = Imgproc.moments(thresholdedImage);
      
      double m01 = moments.get_m01();
      double m10 = moments.get_m10();
      double area = moments.get_m00();
      
      Point2f blobPosition;
      if (m01 != 0.0 && m10 != 0.0 && area != 0.0 && thresholdedImage.width() != 0 && thresholdedImage.height() != 0)
      {
         blobPosition = new Point2f((float) (m10 / area / (double) thresholdedImage.width()), (float) (1.0 - m01 / area / (double) thresholdedImage.height()));
      }
      else
      {
         blobPosition = new Point2f();
      }
      
      return blobPosition;
   }
   
   /**
    * Taken from: http://opencv-srf.blogspot.com/2010/09/object-detection-using-color-seperation.html
    */
   public static Point2f findBlob(BufferedImage bufferedImage, HueSaturationValueRange hsvRange, int size)
   {
      Mat image = convertBufferedImageToHSV(bufferedImage);
      thresholdImage(image, image, hsvRange);
      morphologicallyOpen(image, size);
      morphologicallyClose(image, size);
      return findBlobFromThresholdImage(image);
   }
   
   public static Point2f findBlob(Mat image, HueSaturationValueRange hsvRange, int size)
   {
      convertImageFromRGBToHSV(image, image);
      thresholdImage(image, image, hsvRange);
      morphologicallyOpen(image, size);
      morphologicallyClose(image, size);
      return findBlobFromThresholdImage(image);
   }
   
   public static void main(String[] args) throws IOException
   {
      VideoCapture videoCapture = new VideoCapture(0);
      ImagePanel imagePanel = null;
      ImagePanel imagePanel2 = null;
      ImagePanel imagePanel3 = null;
      ImagePanel imagePanel4 = null;
      Mat image = new Mat();
      MatOfByte matOfByte = new MatOfByte();
      Timer blobTimer = new Timer().start();
      HueSaturationValueRange hsvRange;
      if (args.length > 1)
      {
         double minHue = Double.valueOf(args[1]);
         double maxHue = Double.valueOf(args[2]);
         double minSaturation = Double.valueOf(args[3]);
         double maxSaturation = Double.valueOf(args[4]);
         double minValue = Double.valueOf(args[5]);
         double maxValue = Double.valueOf(args[6]);
         hsvRange = new HueSaturationValueRange(minHue, maxHue, minSaturation, maxSaturation, minValue, maxValue);
      }
      else
      {
         hsvRange = TIGA_ORANGE_PING_PONG_BALL_HSV_RANGE_MAC;
      }
      int size = ColorBlobDetector.TIGA_ORANGE_PING_PONG_BALL_SIZE;
      BufferedImage bufferedImage2 = null;
      BufferedImage bufferedImage3 = null;
      BufferedImage bufferedImage4 = null;
      long count = 0;
      
      while (true)
      {
         videoCapture.read(image);

         Highgui.imencode(".bmp", image, matOfByte);
         BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(matOfByte.toArray()));
         
         blobTimer.lap();

         OpenCVTools.resizeImage(image, 0.5);
         convertImageFromBGRToHSV(image, image);
         bufferedImage3 = OpenCVTools.convertMatToBufferedImage(image);
         thresholdImage(image, image, hsvRange);
         bufferedImage4 = OpenCVTools.convertMatToBufferedImage(image);
         morphologicallyOpen(image, size);
         morphologicallyClose(image, size);
         bufferedImage2 = OpenCVTools.convertMatToBufferedImage(image);
         Point2f ballLocation = findBlobFromThresholdImage(image);
         
         if (count++ % 10 == 0)
         {
            String timeStr = String.valueOf(FormattingTools.roundToSignificantFigures(blobTimer.lapElapsed(), 2));
            String xStr = String.valueOf(FormattingTools.roundToSignificantFigures(ballLocation.x, 2));
            String yStr = String.valueOf(FormattingTools.roundToSignificantFigures(ballLocation.y, 2));
            System.out.println("imgx: " + bufferedImage.getWidth() + " imgy: " + bufferedImage.getHeight() + " time: " + timeStr + " x: " + xStr + " y: " + yStr);
         }
         
         Graphics2D g2d = bufferedImage.createGraphics();
         g2d.setStroke(new BasicStroke(3));
         g2d.setColor(Color.BLUE);
         g2d.drawOval((int) (ballLocation.x * bufferedImage.getWidth()), (int) ((1.0 - ballLocation.y) * bufferedImage.getHeight()), 5, 5);
         g2d.dispose();
         
         if (imagePanel == null)
         {
            imagePanel = ShowImages.showWindow(bufferedImage, "video");
         }
         else
         {
            imagePanel.setBufferedImageSafe(bufferedImage);
         }
         if (imagePanel2 == null)
         {
            imagePanel2 = ShowImages.showWindow(bufferedImage2, "morph");
         }
         else if (bufferedImage2 != null)
         {
            imagePanel2.setBufferedImageSafe(bufferedImage2);
         }
         if (imagePanel3 == null)
         {
            imagePanel3 = ShowImages.showWindow(bufferedImage3, "hue");
         }
         else if (bufferedImage3 != null)
         {
            imagePanel3.setBufferedImageSafe(bufferedImage3);
         }
         if (imagePanel4 == null)
         {
            imagePanel4 = ShowImages.showWindow(bufferedImage4, "thres");
         }
         else if (bufferedImage4 != null)
         {
            imagePanel4.setBufferedImageSafe(bufferedImage4);
         }
      }
   }
}