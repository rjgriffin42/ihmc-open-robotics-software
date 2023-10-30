package us.ihmc.avatar.colorVision;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_cudaimgproc;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.GpuMat;
import org.bytedeco.spinnaker.Spinnaker_C.spinImage;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.log.LogTools;
import us.ihmc.perception.BytedecoImage;
import us.ihmc.perception.RawImage;
import us.ihmc.perception.parameters.IntrinsicCameraMatrixProperties;
import us.ihmc.perception.sensorHead.BlackflyLensProperties;
import us.ihmc.perception.sensorHead.SensorHeadParameters;
import us.ihmc.perception.spinnaker.SpinnakerBlackfly;
import us.ihmc.perception.spinnaker.SpinnakerBlackflyManager;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.thread.RestartableThrottledThread;

import java.time.Instant;
import java.util.function.Supplier;

public class BlackflyImageRetriever
{
   private static final double BLACKFLY_FPS = 30.0;

   private final String blackflySerialNumber;
   private SpinnakerBlackflyManager blackflyManager;
   private SpinnakerBlackfly blackfly;
   private final IntrinsicCameraMatrixProperties ousterFisheyeColoringIntrinsics;

   private final Supplier<ReferenceFrame> cameraFrameSupplier;
   private final FramePose3D cameraPose = new FramePose3D();

   private long sequenceNumber = 0L;
   private int imageWidth = 0;
   private int imageHeight = 0;
   private RawImage distortedImage = null;
   private final RestartableThrottledThread imageGrabThread;
   private int numberOfFailedReads = 0;

   public BlackflyImageRetriever(String blackflySerialNumber,
                                 BlackflyLensProperties lensProperties,
                                 RobotSide side,
                                 Supplier<ReferenceFrame> cameraFrameSupplier)
   {
      this.blackflySerialNumber = blackflySerialNumber;
      this.ousterFisheyeColoringIntrinsics = SensorHeadParameters.loadOusterFisheyeColoringIntrinsicsOnRobot(lensProperties);
      this.cameraFrameSupplier = cameraFrameSupplier;

      imageGrabThread = new RestartableThrottledThread(side.getCamelCaseName() + "BlackflyImageGrabber", BLACKFLY_FPS, this::grabImage);
   }

   private void grabImage()
   {
      while (blackfly == null || numberOfFailedReads > 30)
      {
         if (!initializeBlackfly())
            ThreadTools.sleep(3000);
      }
      // grab image
      spinImage spinImage = new spinImage();
      if (blackfly.getNextImage(spinImage))
      {
         Instant acquisitionTime = Instant.now();
         cameraPose.setToZero(cameraFrameSupplier.get());
         cameraPose.changeFrame(ReferenceFrame.getWorldFrame());

         // Initialize image dimensions from first image
         if (imageWidth == 0 || imageHeight == 0)
         {
            this.imageWidth = blackfly.getWidth(spinImage);
            this.imageHeight = blackfly.getHeight(spinImage);
         }

         // Get image data
         BytePointer spinImageData = new BytePointer((long) imageWidth * imageHeight);
         blackfly.setPointerToSpinImageData(spinImage, spinImageData);
         BytedecoImage sourceByedecoImage = new BytedecoImage(imageWidth, imageHeight, opencv_core.CV_8UC1);
         sourceByedecoImage.changeAddress(spinImageData.address());

         // Upload image to GPU
         GpuMat deviceSourceImage = new GpuMat(imageHeight, imageWidth, opencv_core.CV_8UC1);
         deviceSourceImage.upload(sourceByedecoImage.getBytedecoOpenCVMat());

         // Convert from BayerRG8 to BGR
         GpuMat sourceImageBGR = new GpuMat(imageHeight, imageWidth, opencv_core.CV_8UC3);
         opencv_cudaimgproc.cvtColor(deviceSourceImage, sourceImageBGR, opencv_imgproc.COLOR_BayerBG2BGR);

         distortedImage = new RawImage(sequenceNumber++,
                                       acquisitionTime,
                                       imageWidth,
                                       imageHeight,
                                       0,
                                       null,
                                       sourceImageBGR.clone(),
                                       opencv_core.CV_8UC3,
                                       (float) ousterFisheyeColoringIntrinsics.getFocalLengthX(),
                                       (float) ousterFisheyeColoringIntrinsics.getFocalLengthY(),
                                       (float) ousterFisheyeColoringIntrinsics.getPrinciplePointX(),
                                       (float) ousterFisheyeColoringIntrinsics.getPrinciplePointY(),
                                       cameraPose.getPosition(),
                                       cameraPose.getOrientation());

         // close stuff
         spinImageData.close();
         deviceSourceImage.close();
         sourceImageBGR.close();
      }
      else
      {
         numberOfFailedReads++;
      }
      blackfly.releaseImage(spinImage);
   }

   public RawImage getLatestRawImage()
   {
      return distortedImage;
   }

   public void start()
   {
      imageGrabThread.start();
   }

   public void stop()
   {
      imageGrabThread.stop();
   }

   public void destroy()
   {
      stop();
      blackfly.stopAcquiringImages();
      blackflyManager.destroy();
   }

   private boolean initializeBlackfly()
   {
      LogTools.info("Initializing Blackfly: " + blackflySerialNumber);
      if (blackfly != null)
      {
         blackfly.stopAcquiringImages();
         blackflyManager.destroy();
      }

      blackflyManager = new SpinnakerBlackflyManager();
      blackfly = blackflyManager.createSpinnakerBlackfly(blackflySerialNumber);

      if (blackfly == null)
      {
         LogTools.error("Failed to initialize Blackfly: " + blackflySerialNumber);
      }
      else
      {
         LogTools.info("Initialized Blackfly: " + blackflySerialNumber);
         numberOfFailedReads = 0;
      }

      return blackfly != null;
   }
}
