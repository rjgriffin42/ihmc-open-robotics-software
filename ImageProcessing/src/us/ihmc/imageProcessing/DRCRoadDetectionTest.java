


package us.ihmc.imageProcessing;

import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.point.Point2D_F32;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point2d;

import jxl.format.RGB;
import us.ihmc.imageProcessing.ImageFilters.ColorFilter;
import us.ihmc.imageProcessing.ImageFilters.CropFilter;
import us.ihmc.imageProcessing.driving.*;
import us.ihmc.imageProcessing.utilities.BoundsPainter;
import us.ihmc.imageProcessing.utilities.LinePainter;
import us.ihmc.imageProcessing.utilities.PaintableImageViewer;
import us.ihmc.imageProcessing.utilities.VideoPlayer;
import us.ihmc.utilities.camera.VideoListener;
import us.ihmc.utilities.math.geometry.BoundingBox2d;
import us.ihmc.utilities.math.geometry.ConvexPolygon2d;
import us.ihmc.utilities.math.geometry.Line2d;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

public class DRCRoadDetectionTest implements VideoListener, KeyListener
{
   private boolean PAUSE = false;

   private PaintableImageViewer rawImageViewer = new PaintableImageViewer();
   private PaintableImageViewer analyzedImageViewer = new PaintableImageViewer();

// private PaintableImageViewer blobDetectionViewer = new PaintableImageViewer();
   private LinePainter linePainter = new LinePainter(4.0f);
   private BoundsPainter boundsPainter = new BoundsPainter(4.0f);

   private VanishingPointDetector vanishingPointDetector = new VanishingPointDetector(Color.cyan, 20);

   private LanePositionEstimator lanePositionEstimator;
   private SteeringInputEstimator steeringInputEstimator = new SteeringInputEstimator();
   private ObstaclePositionEstimator obstaclePositionEstimator;

   private JFrame mainFrame;
   private LanePositionIndicatorPanel lanePositionIndicatorPanel;
   private double offset = 0.0;
   private double increment = 0.01;

   private ColorFilter coneColorFilter;
   private ColorFilter roadColorfilter;
   private int maxLines = 10;
   private int localMaxRadius = 3;
   private int minCounts = 100;
   private double resolutionRange = 1;
   private double resolutionAngle = Math.PI / 180;
   private float edgeThreshold = 25;


   private int erodeCount = 1;
   private int dilateCount = 3;

   ArrayList<BoundingBox2d> boundingBoxes = new ArrayList<BoundingBox2d>();



   public DRCRoadDetectionTest()
   {
      setUpFilters();

      setUpImageViewerPostProcessors();

      setUpJFrame();

      createMainWindow();


   }

   private void setUpImageViewerPostProcessors()
   {
      analyzedImageViewer.addPostProcessor(linePainter);
      analyzedImageViewer.addPostProcessor(boundsPainter);
      analyzedImageViewer.addPostProcessor(vanishingPointDetector);
      rawImageViewer.addPostProcessor(vanishingPointDetector);
      rawImageViewer.addPostProcessor(steeringInputEstimator);
      rawImageViewer.addPostProcessor(linePainter);

   }

   private void setUpFilters()
   {
      coneColorFilter = new ColorFilter();
      coneColorFilter.setThreshold(60);
      coneColorFilter.filterHorizon(true);


      coneColorFilter.addColorToLookFor(new RGB(0, 0, 0));
      coneColorFilter.addColorToLookFor(new RGB(5, 5, 5));
      coneColorFilter.addColorToLookFor(new RGB(10, 10, 10));

//    coneColorFilter.addColorToLookFor(new RGB(122, 43, 4));
//  coneColorFilter.addColorToLookFor(new RGB(121, 124, 139));

//    coneColorFilter.addColorToLookFor(new RGB(95, 44, 15));
//    coneColorFilter.addColorToLookFor(new RGB(122, 45, 0));
//    coneColorFilter.addColorToLookFor(new RGB(131, 49, 4));
//    coneColorFilter.addColorToLookFor(new RGB(129, 47, 2));
//    coneColorFilter.addColorToLookFor(new RGB(95, 51, 0));
//
//    coneColorFilter.addColorToLookFor(new RGB(120, 52, 14));
//    coneColorFilter.addColorToLookFor(new RGB(164, 63, 7));




      roadColorfilter = new ColorFilter();
      roadColorfilter.setThreshold(34);

      roadColorfilter.filterHorizon(true);
      roadColorfilter.addColorToLookFor(new RGB(68, 63, 69));
      roadColorfilter.addColorToLookFor(new RGB(63, 59, 60));
      roadColorfilter.addColorToLookFor(new RGB(63, 58, 62));
      roadColorfilter.addColorToLookFor(new RGB(76, 72, 73));
      roadColorfilter.addColorToLookFor(new RGB(79, 74, 78));
      roadColorfilter.addColorToLookFor(new RGB(78, 73, 77));
      roadColorfilter.addColorToLookFor(new RGB(78, 73, 77));
      roadColorfilter.addColorToLookFor(new RGB(54, 52, 53));
      roadColorfilter.addColorToLookFor(new RGB(51, 51, 51));

      roadColorfilter.addColorToLookFor(new RGB(40, 40, 40));
      roadColorfilter.addColorToLookFor(new RGB(45, 45, 45));
      roadColorfilter.addColorToLookFor(new RGB(50, 50, 50));
      roadColorfilter.addColorToLookFor(new RGB(55, 55, 55));
      roadColorfilter.addColorToLookFor(new RGB(60, 60, 60));


      roadColorfilter.addColorToLookFor(new RGB(44, 44, 44));




   }




   /**
    * Detects lines inside the image using different types of Hough detectors
    *
    * @param image     Input image.
    * @param imageType Type of image processed by line detector.
    * @param derivType Type of image derivative.
    */
   public <T extends ImageSingleBand<?>, D extends ImageSingleBand<?>> List<LineParametric2D_F32> detectLines(BufferedImage image, Class<T> imageType,
           Class<D> derivType)
   {
      T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);
      DetectLineHoughPolar<T, D> detector = FactoryDetectLineAlgs.houghPolar(localMaxRadius, minCounts, resolutionRange, resolutionAngle, edgeThreshold,
                                               maxLines, imageType, derivType);
      List<LineParametric2D_F32> found = detector.detect(input);

      return found;
   }


   public void updateImage(BufferedImage bufferedImage)
   {
      process(bufferedImage);
   }

   private BufferedImage deepCopy(BufferedImage bi)
   {
      ColorModel cm = bi.getColorModel();
      boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
      WritableRaster raster = bi.copyData(null);

      return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
   }


   public BufferedImage countConeBlobs(BufferedImage inputImage)
   {
      BufferedImage croppedImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);

      coneColorFilter.filter(inputImage, croppedImage);
      ImageFloat32 input = ConvertBufferedImage.convertFromSingle(croppedImage, null, ImageFloat32.class);
      ImageUInt8 binary = new ImageUInt8(input.width, input.height);
      ImageSInt32 blobs = new ImageSInt32(input.width, input.height);

      double mean = 10;

      // create a binary image
      ThresholdImageOps.threshold(input, binary, (float) mean, true);

      // remove small blobs through erosion and dilation
      // The null in the input indicates that it should internally declare the work image it needs
      // this is less efficient, but easier to code.
      // binary = BinaryImageOps.erode8(binary, null);
      // binary = BinaryImageOps.erode8(binary, null);

      for (int i = 0; i < erodeCount; i++)
      {
         binary = BinaryImageOps.erode8(binary, null);
      }

      for (int i = 0; i < dilateCount; i++)
      {
         binary = BinaryImageOps.dilate8(binary, null);
      }

      // Detect blobs inside the binary image and assign labels to them
      List<Contour> blobContours = BinaryImageOps.contour(binary, 4, blobs);

      boundingBoxes = findBlobBoundingBoxes(blobs, blobContours.size());


      BufferedImage visualized = VisualizeBinaryData.renderLabeled(blobs, blobContours.size(), null);

//    blobDetectionViewer.updateImage(visualized);
      repackConeBlobImageSizeChanges(visualized.getWidth(), visualized.getHeight());


      return visualized;
   }

   /**
    * Set the value of any blob which does not touches the top or bottom image border to zero.  Then
    * relabel the binary image.
    */
   private int filterBlobsNotTouchingEdges(ImageSInt32 labeled, int numLabels)
   {
      int value[] = new int[numLabels + 1];
      for (int i = 0; i < value.length; i++)
      {
         value[i] = 0;
      }

      for (int x = 0; x < labeled.width; x++)
      {
         int top = labeled.startIndex + x;
         int bottom = labeled.startIndex + labeled.stride * (labeled.height - 1) + x;
         value[labeled.data[top]] = labeled.data[top];
         value[labeled.data[bottom]] = labeled.data[bottom];
      }

      int count = 1;
      for (int i = 0; i < value.length; i++)
      {
         if (value[i] != 0)
         {
            value[i] = count++;
         }
      }

      // relabel the image to remove blobs with holes inside
      BinaryImageOps.relabel(labeled, value);

      return count - 1;
   }




   private ArrayList<BoundingBox2d> findBlobBoundingBoxes(ImageSInt32 labeled, int numLabels)
   {
      HashMap<Integer, BoundingBox2d> boundingBoxes = new HashMap<Integer, BoundingBox2d>();


      for (int y = 0; y < labeled.height; y++)
      {
         for (int x = 0; x < labeled.width; x++)
         {
            int index = (labeled.startIndex + labeled.stride * y) + (labeled.startIndex + x);
            int blobID = labeled.data[index];
            if (blobID != 0)
            {
               BoundingBox2d currentBounds = boundingBoxes.get(blobID - 1);
               if (currentBounds == null)
               {
                  currentBounds = new BoundingBox2d(new Point2d(x, y), new Point2d(x, y));
                  boundingBoxes.put(blobID - 1, currentBounds);
               }

               if (currentBounds.getMinPoint().x > x)
               {
                  currentBounds.getMinPoint().x = x;
               }
               else if (currentBounds.getMaxPoint().x < x)
               {
                  currentBounds.getMaxPoint().x = x;
               }

               if (currentBounds.getMinPoint().y > y)
               {
                  currentBounds.getMinPoint().y = y;
               }

               else if (currentBounds.getMaxPoint().y < y)
               {
                  currentBounds.getMaxPoint().y = y;
               }
            }
         }
      }

      // add buffer zone

      double bufferPercent = 0.5;
      ArrayList<BoundingBox2d> finalBoxes = new ArrayList<BoundingBox2d>();
      for (int i = 0; i < numLabels; i++)
      {
         BoundingBox2d bounds = boundingBoxes.get(i);

//       double width = bounds.getMaxPoint().x - bounds.getMinPoint().x;
//       int calcBuffer = new Double(width * bufferPercent).intValue();
//       bounds.getMinPoint().x -= calcBuffer;
//       bounds.getMaxPoint().x += calcBuffer;
         finalBoxes.add(bounds);
      }


      return finalBoxes;
   }


   public BufferedImage findRoad(BufferedImage src)
   {
      // convert into a usable format
      ImageFloat32 input = ConvertBufferedImage.convertFromSingle(src, null, ImageFloat32.class);
      ImageUInt8 binary = new ImageUInt8(input.width, input.height);
      ImageSInt32 blobs = new ImageSInt32(input.width, input.height);

      // the mean pixel value is often a reasonable threshold when creating a binary image
      double mean = ImageStatistics.mean(input);

      // create a binary image
      ThresholdImageOps.threshold(input, binary, (float) mean, true);

      // remove small blobs through erosion and dilation
      // The null in the input indicates that it should internally declare the work image it needs
      // this is less efficient, but easier to code.

      for (int i = 0; i < 1; i++)
      {
         binary = BinaryImageOps.erode8(binary, null);
      }

      for (int i = 0; i < 2; i++)
      {
         binary = BinaryImageOps.dilate8(binary, null);
      }


      // Detect blobs inside the binary image and assign labels to them
      List<Contour> blobContours = BinaryImageOps.contour(binary, 4, blobs);

      int numBlobs = filterBlobsNotTouchingEdges(blobs, blobContours.size());

      // Render the binary image for output and display it in a window
      BufferedImage dst = VisualizeBinaryData.renderLabeled(blobs, numBlobs, null);

      return dst;

   }

   int tick = 4;

   private void process(final BufferedImage input)
   {
      if (!PAUSE)
      {
         repackRawIfImageSizeChanges(input.getWidth(), input.getHeight());

         rawImageViewer.updateImage(input);



//       Thread tmp = new Thread(new Runnable()
//       {
//          @Override
//          public void run()
//          {
//           if (cones.getNumberOfFeatures() > 0)
//              System.out.println("I SEE A CONE");
         // CropFilter cropFilter = new CropFilter(0, 0, input.getWidth(), input.getHeight() - input.getHeight() / 4);
         // BufferedImage copiedImage = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);

//           cropFilter.filter(input, croppedImage);
//             copiedImage = input;

         // if (tick % 5 == 0)
         // {
         // tick = 0;
         coneColorFilter.setHorizonYLocation(input.getHeight() / 2 - 20);

         BufferedImage coneBlobs = countConeBlobs(input);

//       }
//       else
//       {
//          tick++;
//       }
         roadColorfilter.setHorizonYLocation(input.getHeight() / 2 - 20);
         BufferedImage roadFilteredByColor = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
         roadColorfilter.filter(input, roadFilteredByColor);
         BufferedImage roadblobs = findRoad(roadFilteredByColor);

         List<LineParametric2D_F32> list = detectLines(roadblobs, ImageUInt8.class, ImageSInt16.class);

         ArrayList<Line2d> lines = new ArrayList<Line2d>();




         for (LineParametric2D_F32 lineParametric2D_f32 : list)
         {
            Point2D_F32 pointOnLine1 = lineParametric2D_f32.getPointOnLine(0.0f);
            Point2d p1 = new Point2d(pointOnLine1.getX(), pointOnLine1.getY());
            Point2D_F32 pointOnLine2 = lineParametric2D_f32.getPointOnLine(1.0f);
            Point2d p2 = new Point2d(pointOnLine2.getX(), pointOnLine2.getY());

            Line2d line2d = new Line2d(p1, p2);
            lines.add(line2d);
         }

//       lines = removeBadLines(lines);

//       if (lines.size() > 2)
//        System.out.println("TURN APPROACHING");
         linePainter.setLines(lines);
         vanishingPointDetector.setLines(lines);
         lanePositionEstimator.setLines(lines);
         obstaclePositionEstimator.setLines(lines);

         if (boundingBoxes != null)
         {
            boundsPainter.setBoundingBoxes(boundingBoxes);
            obstaclePositionEstimator.setBoundingBoxes(boundingBoxes);
         }

         repackIfImageSizeChanges(roadblobs.getWidth(), roadblobs.getHeight());
         analyzedImageViewer.updateImage(roadblobs);

         // }

//
//       });
//       tmp.start();


      }



//    drawCarPosition();
   }




   private void drawCarPosition()
   {
      offset += increment;

      if ((offset <= -1.0) || (offset >= 1.0))
      {
         increment = -increment;
      }

      lanePositionIndicatorPanel.setOffset(offset);
   }

   private ArrayList<Line2d> removeBadLines(ArrayList<Line2d> lines)
   {
      ArrayList<Line2d> cleanedLines = new ArrayList<Line2d>();
      for (Line2d line : lines)
      {
         if ((line.getSlope() < 3.0))
         {
            cleanedLines.add(line);
         }
      }

      return cleanedLines;
   }

   private void repackIfImageSizeChanges(int width, int height)
   {
      if ((analyzedImageViewer.getWidth() < width) || (analyzedImageViewer.getHeight() < height))
      {
         Dimension dimension = new Dimension(width, height);
         analyzedImageViewer.setPreferredSize(dimension);
         boundsPainter.setImageHeight(height);
         linePainter.setImageHeight(height);
         mainFrame.pack();
      }
   }

   private void repackRawIfImageSizeChanges(int width, int height)
   {
//    System.out.println("width = " + width +":" + height);
      if ((rawImageViewer.getWidth() < width) || (rawImageViewer.getHeight() < height))
      {
         Dimension dimension = new Dimension(width, height);
         rawImageViewer.setPreferredSize(dimension);
         lanePositionEstimator.setScreenDimension(dimension);
         steeringInputEstimator.setScreenDimension(dimension);
         mainFrame.pack();
      }
   }

   private void repackConeBlobImageSizeChanges(int width, int height)
   {
//    System.out.println("width = " + width +":" + height);
//      if ((blobDetectionViewer.getWidth() < width) || (blobDetectionViewer.getHeight() < height))
//      {
//         Dimension dimension = new Dimension(width, height);
//         blobDetectionViewer.setPreferredSize(dimension);
//         mainFrame.pack();
//      }
   }

   public void setUpJFrame()
   {
      JFrame tmp = new JFrame();
      tmp.getContentPane().setLayout(new GridLayout(1, 6));

      {
         final JSlider slider = new JSlider(1, 200, new Double(coneColorFilter.getThreshold()).intValue());
         slider.setOrientation(JSlider.VERTICAL);
         slider.addChangeListener(new ChangeListener()
         {
            @Override
            public void stateChanged(ChangeEvent arg0)
            {
               coneColorFilter.setThreshold(new Double(slider.getValue()));


               System.out.println("coneColorFilter Threshold: " + coneColorFilter.getThreshold());

               // process();

            }
         });

         tmp.getContentPane().add(slider);
      }
      {
         final JSlider slider = new JSlider(0, 5, erodeCount);
         slider.setOrientation(JSlider.VERTICAL);
         slider.addChangeListener(new ChangeListener()
         {
            @Override
            public void stateChanged(ChangeEvent arg0)
            {
               erodeCount = slider.getValue();


               System.out.println("erodeCount: " + erodeCount);

               // process();

            }
         });

         tmp.getContentPane().add(slider);
      }
      {
         final JSlider slider = new JSlider(0, 5, dilateCount);
         slider.setOrientation(JSlider.VERTICAL);
         slider.addChangeListener(new ChangeListener()
         {
            @Override
            public void stateChanged(ChangeEvent arg0)
            {
               dilateCount = slider.getValue();


               System.out.println("dilateCount: " + dilateCount);

               // process();

            }
         });

         tmp.getContentPane().add(slider);
      }


      tmp.pack();

      tmp.setVisible(true);
      tmp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }

   private void createMainWindow()
   {
      mainFrame = new JFrame("Driving Algorithm Test");
      mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      mainFrame.setResizable(false);
      mainFrame.addKeyListener(this);

      Container mainContainer = mainFrame.getContentPane();
      mainContainer.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;

      mainContainer.add(rawImageViewer, gbc);
      gbc.gridx++;
      gbc.gridheight = 2;
      mainContainer.add(analyzedImageViewer, gbc);
      gbc.gridx++;

//    mainContainer.add(blobDetectionViewer, gbc);
//    gbc.gridheight = 1;

      lanePositionIndicatorPanel = new LanePositionIndicatorPanel("./media/images/CarIcon.png", steeringInputEstimator);
      lanePositionIndicatorPanel.setPreferredSize(new Dimension(1, 43));
      gbc.gridx = 0;
      gbc.gridy++;
      gbc.weightx = 1.0;
      gbc.weighty = 0.0;
      mainContainer.add(lanePositionIndicatorPanel, gbc);
      lanePositionEstimator = new LanePositionEstimator(lanePositionIndicatorPanel);
      obstaclePositionEstimator = new ObstaclePositionEstimator(lanePositionIndicatorPanel);
      rawImageViewer.addPostProcessor(obstaclePositionEstimator);

      mainFrame.pack();
      mainFrame.setVisible(true);
   }

   public ArrayList<ConvexPolygon2d> detectConeLocations(BufferedImage image)
   {
      ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);

      // Create a Fast Hessian detector from the SURF paper.
      // Other detectors can be used in this example too.
      InterestPointDetector<ImageFloat32> detector = FactoryInterestPoint.fastHessian(new ConfigFastHessian(10, 2, 100, 2, 9, 3, 4));

      // find interest points in the image
      detector.detect(input);

      // Show the features
      return new ArrayList<ConvexPolygon2d>();
   }


   public static void main(String args[])
   {
      DRCRoadDetectionTest drcRoadDetectionTest = new DRCRoadDetectionTest();
      final VideoPlayer videoPlayer = new VideoPlayer("./media/videos/rightEye.mp4", drcRoadDetectionTest, true);
      videoPlayer.start();
   }

   public void keyTyped(KeyEvent e)
   {
   }

   public void keyPressed(KeyEvent e)
   {
      if (e.getKeyCode() == KeyEvent.VK_SPACE)
      {
         PAUSE = !PAUSE;
      }
   }

   public void keyReleased(KeyEvent e)
   {
   }
}
