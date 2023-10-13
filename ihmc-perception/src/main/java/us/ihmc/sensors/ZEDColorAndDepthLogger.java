package us.ihmc.sensors;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_cudaimgproc;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.GpuMat;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.zed.SL_CalibrationParameters;
import org.bytedeco.zed.SL_InitParameters;
import org.bytedeco.zed.SL_RuntimeParameters;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.log.LogTools;
import us.ihmc.perception.logging.PerceptionDataLogger;
import us.ihmc.perception.logging.PerceptionLoggerConstants;
import us.ihmc.perception.opencv.OpenCVTools;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.tools.IHMCCommonPaths;
import us.ihmc.tools.thread.Throttler;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.zed.global.zed.*;

public class ZEDColorAndDepthLogger
{
   private static final int CAMERA_FPS = 30;
   private static final float MILLIMETER_TO_METERS = 0.001f;
   private static final FramePose3D DEFAULT_FRAME_POSE = new FramePose3D();

   private final int cameraID;
   private ZEDModelData zedModelData;
   private final SL_RuntimeParameters zedRuntimeParameters = new SL_RuntimeParameters();

   private final int imageWidth; // Width of rectified image in pixels (color image width == depth image width)
   private final int imageHeight; // Height of rectified image in pixels (color image height ==  depth image height)

   private final SideDependentList<Pointer> colorImagePointers;
   private final Pointer depthImagePointer;
   private final SideDependentList<Long> colorImageSequenceNumber = new SideDependentList<>(0L, 0L);

   private final AtomicReference<Instant> colorImageAcquisitionTime = new AtomicReference<>();
   private final AtomicReference<Instant> depthImageAcquisitionTime = new AtomicReference<>();
   private final Throttler throttler = new Throttler();
   private boolean running = true;

   private final String depthChannelName;
   private final SideDependentList<String> colorChannelNames;
   private final String timeChannelName; // TODO: Add time logging
   private final PerceptionDataLogger perceptionDataLogger = new PerceptionDataLogger();

   public ZEDColorAndDepthLogger(int cameraID,String depthChannelName, SideDependentList<String> colorChannelNames, String timeChannelName)
   {
      this.cameraID = cameraID;
      this.depthChannelName = depthChannelName;
      this.colorChannelNames = colorChannelNames;
      this.timeChannelName = timeChannelName;

      LogTools.info("ZED SDK version: " + sl_get_sdk_version().getString());

      // Create and initialize the camera
      sl_create_camera(cameraID);

      SL_InitParameters zedInitializationParameters = new SL_InitParameters();

      // Open camera with default parameters to find model
      // Can't get the model number without opening the camera first
      checkError("sl_open_camera", sl_open_camera(cameraID, zedInitializationParameters, 0, "", "", 0, "", "", ""));
      setZEDConfiguration(cameraID);
      sl_close_camera(cameraID);

      // Set initialization parameters based on camera model
      zedInitializationParameters.camera_fps(CAMERA_FPS);
      zedInitializationParameters.resolution(SL_RESOLUTION_HD720);
      zedInitializationParameters.input_type(SL_INPUT_TYPE_USB);
      zedInitializationParameters.camera_device_id(cameraID);
      zedInitializationParameters.camera_image_flip(SL_FLIP_MODE_OFF);
      zedInitializationParameters.camera_disable_self_calib(false);
      zedInitializationParameters.enable_image_enhancement(true);
      zedInitializationParameters.svo_real_time_mode(true);
      zedInitializationParameters.depth_mode(SL_DEPTH_MODE_ULTRA);
      zedInitializationParameters.depth_stabilization(1);
      zedInitializationParameters.depth_maximum_distance(zedModelData.getMaximumDepthDistance());
      zedInitializationParameters.depth_minimum_distance(zedModelData.getMinimumDepthDistance());
      zedInitializationParameters.coordinate_unit(SL_UNIT_METER);
      zedInitializationParameters.coordinate_system(SL_COORDINATE_SYSTEM_RIGHT_HANDED_Z_UP_X_FWD);
      zedInitializationParameters.sdk_gpu_id(-1); // Will find and use the best available GPU
      zedInitializationParameters.sdk_verbose(0); // false
      zedInitializationParameters.sensors_required(true);
      zedInitializationParameters.enable_right_side_measure(false);
      zedInitializationParameters.open_timeout_sec(5.0f);
      zedInitializationParameters.async_grab_camera_recovery(false);

      // Reopen camera with specific parameters set
      checkError("sl_open_camera", sl_open_camera(cameraID, zedInitializationParameters, 0, "", "", 0, "", "", ""));

      zedRuntimeParameters.enable_depth(true);
      zedRuntimeParameters.confidence_threshold(100);
      zedRuntimeParameters.reference_frame(SL_REFERENCE_FRAME_CAMERA);
      zedRuntimeParameters.texture_confidence_threshold(100);
      zedRuntimeParameters.remove_saturated_areas(true);
      zedRuntimeParameters.enable_fill_mode(false);

      // Get camera's parameters
      SL_CalibrationParameters zedCalibrationParameters = sl_get_calibration_parameters(cameraID, false);
      imageWidth = sl_get_width(cameraID);
      imageHeight = sl_get_height(cameraID);

      // Create Pointers to the SL mats for the images
      colorImagePointers = new SideDependentList<>(new Pointer(sl_mat_create_new(imageWidth, imageHeight, SL_MAT_TYPE_U8_C4, SL_MEM_GPU)),
                                                   new Pointer(sl_mat_create_new(imageWidth, imageHeight, SL_MAT_TYPE_U8_C4, SL_MEM_GPU)));
      depthImagePointer = new Pointer(sl_mat_create_new(imageWidth, imageHeight, SL_MAT_TYPE_U16_C1, SL_MEM_GPU));

      // Setup logger
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
      String logFileName = dateFormat.format(new Date()) + "_" + "PerceptionLog.hdf5";
      perceptionDataLogger.openLogFile(IHMCCommonPaths.PERCEPTION_LOGS_DIRECTORY.resolve(logFileName).toString());
      perceptionDataLogger.addIntChannel(depthChannelName, imageWidth * imageHeight, 16);
      perceptionDataLogger.setChannelEnabled(depthChannelName, true);
      perceptionDataLogger.addLongChannel(timeChannelName, 1, PerceptionLoggerConstants.DEFAULT_BLOCK_SIZE);
      perceptionDataLogger.setChannelEnabled(timeChannelName, true);
      for (RobotSide side : RobotSide.values)
      {
         perceptionDataLogger.addImageChannel(colorChannelNames.get(side));
         perceptionDataLogger.setChannelEnabled(colorChannelNames.get(side), true);
      }

      Runtime.getRuntime().addShutdownHook(new Thread(this::destroy, getClass().getSimpleName() + "Shutdown"));
      throttler.setFrequency(CAMERA_FPS);

      while (running)
      {
         logImages();
         throttler.waitAndRun();
      }
   }

   private void logImages()
   {
      sl_grab(cameraID, zedRuntimeParameters);
      for (RobotSide side : RobotSide.values())
      {
         int slViewSide = side == RobotSide.LEFT ? SL_VIEW_LEFT : SL_VIEW_RIGHT;
         // Retrieve color image
         checkError("sl_retrieve_image", sl_retrieve_image(cameraID, colorImagePointers.get(side), slViewSide, SL_MEM_GPU, imageWidth, imageHeight));
         colorImageAcquisitionTime.set(Instant.now());

         // Convert to BGR and encode to jpeg

         GpuMat colorImageBGRA = new GpuMat(imageHeight,
                                            imageWidth,
                                            opencv_core.CV_8UC4,
                                            sl_mat_get_ptr(colorImagePointers.get(side), SL_MEM_GPU),
                                            sl_mat_get_step_bytes(colorImagePointers.get(side), SL_MEM_GPU));

         GpuMat colorImageRGB = new GpuMat(imageHeight, imageWidth, opencv_core.CV_8UC3);
         opencv_cudaimgproc.cvtColor(colorImageBGRA, colorImageRGB, opencv_imgproc.COLOR_BGRA2RGB);

         Mat cpuImageMat = new Mat(imageHeight, imageWidth, opencv_core.CV_8UC3);
         colorImageRGB.download(cpuImageMat);

         Mat yuvColorImage = new Mat(imageHeight * 1.5, imageWidth, opencv_core.CV_8UC1); // deallocate later

         BytePointer compressedColorPointer = new BytePointer();
         OpenCVTools.compressRGBImageJPG(cpuImageMat, yuvColorImage, compressedColorPointer);

         perceptionDataLogger.storeBytesFromPointer(colorChannelNames.get(side), compressedColorPointer);

         colorImageSequenceNumber.set(side, colorImageSequenceNumber.get(side) + 1L);

         // Close stuff
         colorImageRGB.close();
         cpuImageMat.close();
         yuvColorImage.close();
         colorImageBGRA.close();
      }

      // Retrieve depth image
      // There is a bug where retrieving the depth image using SL_MEM_CPU causes the depth image to be misaligned and very dark.
      // Thus, the image is retrieved onto a GpuMat then downloaded onto the CPU for further processing.
      checkError("sl_retrieve_measure", sl_retrieve_measure(cameraID, depthImagePointer, SL_MEASURE_DEPTH_U16_MM, SL_MEM_GPU, imageWidth, imageHeight));
      depthImageAcquisitionTime.set(Instant.now());

      GpuMat gpuDepthImage16UC1 = new GpuMat(imageHeight,
                                             imageWidth,
                                             opencv_core.CV_16UC1,
                                             sl_mat_get_ptr(depthImagePointer, SL_MEM_GPU),
                                             sl_mat_get_step_bytes(depthImagePointer, SL_MEM_GPU));
      Mat cpuDepthImage16UC1 = new Mat(imageHeight, imageWidth, opencv_core.CV_16UC1);
      gpuDepthImage16UC1.download(cpuDepthImage16UC1);

      BytePointer compressedDepthPointer = new BytePointer();
      OpenCVTools.compressImagePNG(cpuDepthImage16UC1, compressedDepthPointer);
      perceptionDataLogger.storeBytesFromPointer(depthChannelName, compressedDepthPointer);

      // Close stuff
      cpuDepthImage16UC1.close();
      gpuDepthImage16UC1.close();
   }

   private void setZEDConfiguration(int cameraID)
   {
      switch (sl_get_camera_model(cameraID))
      {
         case 0 -> zedModelData = ZEDModelData.ZED;
         case 1 -> zedModelData = ZEDModelData.ZED_MINI;
         case 2 -> zedModelData = ZEDModelData.ZED_2;
         case 3 -> zedModelData = ZEDModelData.ZED_2I;
         case 4 -> zedModelData = ZEDModelData.ZED_X;
         case 5 -> zedModelData = ZEDModelData.ZED_X_MINI;
         default ->
         {
            zedModelData = ZEDModelData.ZED;
            LogTools.error("Failed to associate model number with a ZED sensor model");
         }
      }
   }

   private void checkError(String functionName, int returnedState)
   {
      if (returnedState != SL_ERROR_CODE_SUCCESS)
      {
         LogTools.error(String.format("%s returned '%d'", functionName, returnedState));
      }
   }

   private void destroy()
   {
      ThreadTools.sleepSeconds(0.5);

      running = false;
      perceptionDataLogger.closeLogFile();
   }

   public static void main(String[] args)
   {
      new ZEDColorAndDepthLogger(0, PerceptionLoggerConstants.ZED_DEPTH_NAME, PerceptionLoggerConstants.ZED_COLOR_NAMES, PerceptionLoggerConstants.ZED_TIME_NAME);
   }
}