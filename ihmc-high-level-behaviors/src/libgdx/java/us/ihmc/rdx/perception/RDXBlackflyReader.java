package us.ihmc.rdx.perception;

import imgui.ImGui;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.spinnaker.Spinnaker_C.spinImage;
import org.bytedeco.spinnaker.global.Spinnaker_C;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.perception.spinnaker.SpinnakerBlackfly;
import us.ihmc.perception.spinnaker.SpinnakerSystemManager;
import us.ihmc.rdx.imgui.ImGuiPanel;
import us.ihmc.rdx.ui.graphics.ImGuiOpenCVSwapVideoPanel;
import us.ihmc.rdx.ui.graphics.ImGuiOpenCVSwapVideoPanelData;
import us.ihmc.rdx.ui.tools.ImPlotFrequencyPlot;
import us.ihmc.rdx.ui.tools.ImPlotStopwatchPlot;
import us.ihmc.tools.thread.Activator;

import java.util.function.Consumer;

public class RDXBlackflyReader
{
   private final ImGuiPanel panel = new ImGuiPanel("Blackfly Reader", this::renderImGuiWidgets);
   private final Activator nativesLoadedActivator;
   private final String serialNumber;
   private volatile long imageWidth = -1;
   private volatile long imageHeight = -1;
   private SpinnakerSystemManager spinnakerSystemManager;
   private SpinnakerBlackfly blackfly;
   private spinImage spinImage;
   private BytePointer spinImageDataPointer;
   private Mat blackflySourceMat;
   private ImGuiOpenCVSwapVideoPanel swapCVPanel;
   private final ImPlotStopwatchPlot readDurationPlot = new ImPlotStopwatchPlot("Read duration");
   private final ImPlotFrequencyPlot readFrequencyPlot = new ImPlotFrequencyPlot("Read frequency");
   private boolean imageWasRead = false;
   private long numberOfImagesRead = 0;
   private Consumer<ImGuiOpenCVSwapVideoPanelData> monitorPanelUIThreadPreprocessor = null;

   public RDXBlackflyReader(Activator nativesLoadedActivator, String serialNumber)
   {
      this.nativesLoadedActivator = nativesLoadedActivator;
      this.serialNumber = serialNumber;
   }

   public void create()
   {
      spinnakerSystemManager = new SpinnakerSystemManager();
      blackfly = spinnakerSystemManager.createBlackfly(serialNumber);

      spinImage = new spinImage();

      blackfly.setAcquisitionMode(Spinnaker_C.spinAcquisitionModeEnums.AcquisitionMode_Continuous);
      blackfly.setPixelFormat(Spinnaker_C.spinPixelFormatEnums.PixelFormat_RGB8);
      blackfly.startAcquiringImages();

      swapCVPanel = new ImGuiOpenCVSwapVideoPanel("Blackfly Monitor", this::monitorUpdateOnAsynchronousThread, this::monitorPanelUpdateOnUIThread);
   }

   /**
    * Allows the user to do some processing on the image after it is read
    * on the UI update thread. It's not ideal to do too much processing here,
    * just quick and easy stuff.
    */
   public void setMonitorPanelUIThreadPreprocessor(Consumer<ImGuiOpenCVSwapVideoPanelData> monitorPanelUIThreadPreprocessor)
   {
      this.monitorPanelUIThreadPreprocessor = monitorPanelUIThreadPreprocessor;
   }

   /**
    * This method works best if called asynchronously, as it runs slower
    * than UI framerates typically.
    *
    * However, it can also be called just before update() if in a pinch.
    */
   public void readBlackflyImage()
   {
      readDurationPlot.start();
      swapCVPanel.updateOnAsynchronousThread();
      readDurationPlot.stop();
      readFrequencyPlot.ping();
   }

   private void monitorUpdateOnAsynchronousThread(ImGuiOpenCVSwapVideoPanelData data)
   {
      imageWasRead = blackfly.getNextImage(spinImage);

      if (imageWasRead)
      {
         if (spinImageDataPointer == null)
         {
            imageWidth = blackfly.getWidth(spinImage);
            imageHeight = blackfly.getHeight(spinImage);
            spinImageDataPointer = new BytePointer(imageWidth * imageHeight * 3); // RGB8
            blackflySourceMat = new Mat((int) imageHeight, (int) imageWidth, opencv_core.CV_8UC3);
            swapCVPanel.allocateInitialTextures((int) imageWidth, (int) imageHeight);
         }

         Spinnaker_C.spinImageGetData(spinImage, spinImageDataPointer);
         blackflySourceMat.data(spinImageDataPointer);

         opencv_imgproc.cvtColor(blackflySourceMat, data.getRGBA8Mat(), opencv_imgproc.COLOR_RGB2RGBA, 0);

         Spinnaker_C.spinImageRelease(spinImage);
      }
   }

   /**
    * This should be called on the render thread each frame. It will draw the
    * latest image to the framebuffer.
    */
   public void updateOnUIThread()
   {
      swapCVPanel.updateOnUIThread();
   }

   private void monitorPanelUpdateOnUIThread(ImGuiOpenCVSwapVideoPanelData data)
   {
      if (imageWasRead)
      {
         imageWasRead = false;
         ++numberOfImagesRead;

         if (monitorPanelUIThreadPreprocessor != null && data.getRGBA8Image() != null)
            monitorPanelUIThreadPreprocessor.accept(data);

         data.updateOnUIThread(swapCVPanel.getVideoPanel());
      }
   }

   public void renderImGuiWidgets()
   {
      if (nativesLoadedActivator.peek())
      {
         ImGui.text("Serial number: " + serialNumber);
         ImGui.text("Image dimensions: " + imageWidth + " x " + imageHeight);
         ImGui.text("Number of images read: " + numberOfImagesRead);
         readFrequencyPlot.renderImGuiWidgets();
         readDurationPlot.renderImGuiWidgets();
      }
   }

   public void dispose()
   {
      ThreadTools.sleep(250);
      blackfly.stopAcquiringImages();
      ThreadTools.sleep(100);
      spinnakerSystemManager.destroy();
   }

   public boolean getImageWasRead()
   {
      return imageWasRead;
   }

   public ImGuiPanel getStatisticsPanel()
   {
      return panel;
   }

   public ImGuiOpenCVSwapVideoPanel getSwapCVPanel()
   {
      return swapCVPanel;
   }

   public long getImageHeight()
   {
      return imageHeight;
   }

   public long getImageWidth()
   {
      return imageWidth;
   }

   public Mat getRGBImage()
   {
      return blackflySourceMat;
   }
}
