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
import perception_msgs.msg.dds.ImageMessage;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.PerceptionAPI;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.log.LogTools;
import us.ihmc.perception.CameraModel;
import us.ihmc.perception.comms.ImageMessageFormat;
import us.ihmc.perception.cuda.CUDAImageEncoder;
import us.ihmc.perception.opencv.OpenCVTools;
import us.ihmc.perception.tools.ImageMessageDataPacker;
import us.ihmc.pubsub.DomainFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.ros2.ROS2QosProfile;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.tools.thread.Throttler;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.bytedeco.zed.global.zed.*;

/**
 * Encodes and publishes color and depth images from a ZED 2 sensor.
 * The depth image is aligned to the left camera of the ZED.
 */
public class ZED2ColorStereoDepthPublisher
{
   private static final int CAMERA_FPS = 30;
   private static final float MILLIMETER_TO_METERS = 0.001f;

   private final int cameraID;
   private final SL_RuntimeParameters zedRuntimeParameters = new SL_RuntimeParameters();

   private final int imageWidth; // Width of rectified image in pixels (color image width == depth image width)
   private final int imageHeight; // Height of rectified image in pixels (color image height ==  depth image height)
   private final SideDependentList<Float> cameraFocalLengthX;
   private final SideDependentList<Float> cameraFocalLengthY;
   private final SideDependentList<Float> cameraPrincipalPointX;
   private final SideDependentList<Float> cameraPrincipalPointY;


   private final SideDependentList<Pointer> colorImagePointers;
   private final Pointer depthImagePointer;
   private long depthImageSequenceNumber = 0L;
   private final SideDependentList<Long> colorImageSequenceNumber = new SideDependentList<>(0L, 0L);

   private final ImageMessage colorImageMessage = new ImageMessage();
   private final ImageMessage depthImageMessage = new ImageMessage();
   private final AtomicReference<Instant> colorImageAcquisitionTime = new AtomicReference<>();
   private final AtomicReference<Instant> depthImageAcquisitionTime = new AtomicReference<>();
   private final SideDependentList<IHMCROS2Publisher<ImageMessage>> ros2ColorImagePublishers;
   private final IHMCROS2Publisher<ImageMessage> ros2DepthImagePublisher;
   private final SideDependentList<FramePose3D> zedCameraFramePose = new SideDependentList<>(new FramePose3D(), new FramePose3D());
   private final ROS2Node ros2Node;
   private final CUDAImageEncoder imageEncoder;

   private final Thread grabImageThread;
   private final Thread colorImagePublishThread;
   private final Thread depthImagePublishThread;
   private final Throttler throttler = new Throttler();
   private volatile boolean running = true;

   public ZED2ColorStereoDepthPublisher(int cameraID,
                                        SideDependentList<ROS2Topic<ImageMessage>> colorTopics,
                                        ROS2Topic<ImageMessage> depthTopic,
                                        Supplier<SideDependentList<ReferenceFrame>> zed2FrameSupplier)
   {
      this.cameraID = cameraID;

      // Create and initialize the camera
      sl_create_camera(cameraID);

      SL_InitParameters zedInitializationParameters = new SL_InitParameters();
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
      zedInitializationParameters.depth_maximum_distance(40);
      zedInitializationParameters.depth_minimum_distance(-1);
      zedInitializationParameters.coordinate_unit(SL_UNIT_METER);
      zedInitializationParameters.coordinate_system(SL_COORDINATE_SYSTEM_RIGHT_HANDED_Z_UP_X_FWD);
      zedInitializationParameters.sdk_gpu_id(-1);
      zedInitializationParameters.sdk_verbose(0); // false
      zedInitializationParameters.sensors_required(true);
      zedInitializationParameters.enable_right_side_measure(false);
      zedInitializationParameters.open_timeout_sec(5.0f);
      zedInitializationParameters.async_grab_camera_recovery(false);

      checkError("sl_open_camera", sl_open_camera(cameraID, zedInitializationParameters, 0, "", "", 0, "", "", ""));

      zedRuntimeParameters.enable_depth(true);
      zedRuntimeParameters.confidence_threshold(100);
      zedRuntimeParameters.reference_frame(SL_REFERENCE_FRAME_CAMERA);
      zedRuntimeParameters.texture_confidence_threshold(100);
      zedRuntimeParameters.remove_saturated_areas(true);
      zedRuntimeParameters.enable_fill_mode(false);

      // Get camera's parameters
      SL_CalibrationParameters zedCalibrationParameters = sl_get_calibration_parameters(cameraID, false);
      cameraFocalLengthX = new SideDependentList<>(zedCalibrationParameters.left_cam().fx(), zedCalibrationParameters.right_cam().fx());
      cameraFocalLengthY = new SideDependentList<>(zedCalibrationParameters.left_cam().fy(), zedCalibrationParameters.right_cam().fy());
      cameraPrincipalPointX = new SideDependentList<>(zedCalibrationParameters.left_cam().cx(), zedCalibrationParameters.right_cam().cx());
      cameraPrincipalPointY = new SideDependentList<>(zedCalibrationParameters.left_cam().cy(), zedCalibrationParameters.right_cam().cy());
      imageWidth = sl_get_width(cameraID);
      imageHeight = sl_get_height(cameraID);

      // Create Pointers to the SL mats for the images
      colorImagePointers = new SideDependentList<>(new Pointer(sl_mat_create_new(imageWidth, imageHeight, SL_MAT_TYPE_U8_C4, SL_MEM_GPU)),
                                                   new Pointer(sl_mat_create_new(imageWidth, imageHeight, SL_MAT_TYPE_U8_C4, SL_MEM_GPU)));
      depthImagePointer = new Pointer(sl_mat_create_new(imageWidth, imageHeight, SL_MAT_TYPE_U16_C1, SL_MEM_GPU));

      ros2Node = ROS2Tools.createROS2Node(DomainFactory.PubSubImplementation.FAST_RTPS, "zed2_node");

      ros2ColorImagePublishers = new SideDependentList<>(ROS2Tools.createPublisher(ros2Node, colorTopics.get(RobotSide.LEFT), ROS2QosProfile.BEST_EFFORT()),
                                                         ROS2Tools.createPublisher(ros2Node, colorTopics.get(RobotSide.RIGHT), ROS2QosProfile.BEST_EFFORT()));
      ros2DepthImagePublisher = ROS2Tools.createPublisher(ros2Node, depthTopic, ROS2QosProfile.BEST_EFFORT());

      imageEncoder = new CUDAImageEncoder();
      throttler.setFrequency(CAMERA_FPS);

      Runtime.getRuntime().addShutdownHook(new Thread(this::destroy, getClass().getName() + "-Shutdown"));

      grabImageThread = new Thread(() ->
      {
         while (running)
         {
            // Continuously grab images from the camera. These images go to GPU memory.
            // sl_grab processes the stereo images to create the depth image
            checkError("sl_grab", sl_grab(cameraID, zedRuntimeParameters));
            for (RobotSide side : RobotSide.values())
            {
               zedCameraFramePose.get(side).setToZero(zed2FrameSupplier.get().get(side));
            }
         }
      }, "ZED2ImageGrabThread");

      colorImagePublishThread = new Thread(() ->
      {
         while (running)
         {
            throttler.waitAndRun();
            retrieveAndPublishLeftColorImage();
         }
      }, "ZED2ColorImagePublishThread");

      depthImagePublishThread = new Thread(() ->
      {
         while (running)
         {
            throttler.waitAndRun();
            retrieveAndPublishDepthImage();
         }
      }, "ZED2DepthImagePublishThread");

      colorImagePublishThread.setDaemon(true);
      depthImagePublishThread.setDaemon(true);

      grabImageThread.start();
      colorImagePublishThread.start();
      depthImagePublishThread.start();
   }

   private void retrieveAndPublishLeftColorImage()
   {
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

         GpuMat colorImageBGR = new GpuMat(imageHeight, imageWidth, opencv_core.CV_8UC3);
         opencv_cudaimgproc.cvtColor(colorImageBGRA, colorImageBGR, opencv_imgproc.COLOR_BGRA2BGR);

         BytePointer colorJPEGPointer = new BytePointer((long) imageHeight * imageWidth);
         imageEncoder.encodeBGR(colorImageBGR.data(), colorJPEGPointer, imageWidth, imageHeight, colorImageBGR.step());

         // Publish image
         ImageMessageDataPacker imageMessageDataPacker = new ImageMessageDataPacker(colorJPEGPointer.limit());
         imageMessageDataPacker.pack(colorImageMessage, colorJPEGPointer);
         MessageTools.toMessage(colorImageAcquisitionTime.get(), colorImageMessage.getAcquisitionTime());
         colorImageMessage.setFocalLengthXPixels(cameraFocalLengthX.get(side));
         colorImageMessage.setFocalLengthYPixels(cameraFocalLengthY.get(side));
         colorImageMessage.setPrincipalPointXPixels(cameraPrincipalPointX.get(side));
         colorImageMessage.setPrincipalPointYPixels(cameraPrincipalPointY.get(side));
         colorImageMessage.setImageWidth(imageWidth);
         colorImageMessage.setImageHeight(imageHeight);
         // TODO: Get ZED's FramePose3D on robot
         colorImageMessage.getPosition().set(zedCameraFramePose.get(side).getPosition());
         colorImageMessage.getOrientation().set(zedCameraFramePose.get(side).getOrientation());
         colorImageMessage.setSequenceNumber(colorImageSequenceNumber.get(side));
         colorImageMessage.setDepthDiscretization(MILLIMETER_TO_METERS);
         CameraModel.PINHOLE.packMessageFormat(colorImageMessage);
         ImageMessageFormat.COLOR_JPEG_BGR8.packMessageFormat(colorImageMessage);
         ros2ColorImagePublishers.get(side).publish(colorImageMessage);
         colorImageSequenceNumber.set(side, colorImageSequenceNumber.get(side) + 1L);

         // Close stuff
         colorJPEGPointer.close();
         colorImageBGR.close();
         colorImageBGRA.close();
      }
   }

   private void retrieveAndPublishDepthImage()
   {
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

      // Encode depth image to png
      BytePointer depthPNGPointer = new BytePointer();
      OpenCVTools.compressImagePNG(cpuDepthImage16UC1, depthPNGPointer);

      // Publish image
      ImageMessageDataPacker imageMessageDataPacker = new ImageMessageDataPacker(depthPNGPointer.limit());
      imageMessageDataPacker.pack(depthImageMessage, depthPNGPointer);
      MessageTools.toMessage(depthImageAcquisitionTime.get(), depthImageMessage.getAcquisitionTime());
      depthImageMessage.setFocalLengthXPixels(cameraFocalLengthX.get(RobotSide.LEFT));
      depthImageMessage.setFocalLengthYPixels(cameraFocalLengthY.get(RobotSide.LEFT));
      depthImageMessage.setPrincipalPointXPixels(cameraPrincipalPointX.get(RobotSide.LEFT));
      depthImageMessage.setPrincipalPointYPixels(cameraPrincipalPointY.get(RobotSide.LEFT));
      depthImageMessage.setImageWidth(imageWidth);
      depthImageMessage.setImageHeight(imageHeight);
      depthImageMessage.getPosition().set(zedCameraFramePose.get(RobotSide.LEFT).getPosition());
      depthImageMessage.getOrientation().set(zedCameraFramePose.get(RobotSide.LEFT).getOrientation());
      depthImageMessage.setSequenceNumber(depthImageSequenceNumber++);
      depthImageMessage.setDepthDiscretization(MILLIMETER_TO_METERS);
      CameraModel.PINHOLE.packMessageFormat(depthImageMessage);
      ImageMessageFormat.DEPTH_PNG_16UC1.packMessageFormat(depthImageMessage);
      ros2DepthImagePublisher.publish(depthImageMessage);

      // Close stuff
      depthPNGPointer.close();
      cpuDepthImage16UC1.close();
      gpuDepthImage16UC1.close();
   }

   public void destroy()
   {
      running = false;
      sl_close_camera(cameraID);
      ros2Node.destroy();
   }

   private void checkError(String functionName, int returnedState)
   {
      if (returnedState != SL_ERROR_CODE_SUCCESS)
      {
         LogTools.error(String.format("%s returned '%d'", functionName, returnedState));
      }
   }

   public static void main(String[] args)
   {
      Supplier worldFrameSupplier = new Supplier()
      {
         @Override
         public Object get()
         {
            return new SideDependentList<ReferenceFrame>(ReferenceFrame.getWorldFrame(), ReferenceFrame.getWorldFrame());
         }
      };

      new ZED2ColorStereoDepthPublisher(0, PerceptionAPI.ZED2_COLOR_IMAGES, PerceptionAPI.ZED2_DEPTH, worldFrameSupplier);
   }
}
