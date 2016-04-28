package us.ihmc.ihmcPerception.camera;

import boofcv.struct.calib.IntrinsicParameters;
import us.ihmc.SdfLoader.SDFFullRobotModelFactory;
import us.ihmc.humanoidRobotics.kryo.PPSTimestampOffsetProvider;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.sensorProcessing.communication.producers.RobotConfigurationDataBuffer;
import us.ihmc.sensorProcessing.parameters.DRCRobotCameraParameters;
import us.ihmc.tools.io.printing.PrintTools;
import us.ihmc.utilities.ros.RosMainNode;
import us.ihmc.utilities.ros.subscriber.RosCompressedImageSubscriber;
import us.ihmc.utilities.ros.subscriber.RosImageSubscriber;

import java.awt.image.BufferedImage;

public class RosCameraReceiver
{
   static final boolean DEBUG = false;
   private final RigidBodyTransform staticTransform = new RigidBodyTransform();

   public RosCameraReceiver(final DRCRobotCameraParameters cameraParameters, final RosMainNode rosMainNode, final CameraLogger logger,
         final CameraDataReceiver cameraDataReceiver)
   {
      if (cameraParameters.useRosForTransformFromPoseToSensor())
      {
         // Start request for transform
         ROSHeadTransformFrame cameraFrame = new ROSHeadTransformFrame(cameraDataReceiver.getHeadFrame(), rosMainNode, cameraParameters);
         cameraDataReceiver.setCameraFrame(cameraFrame);
         new Thread(cameraFrame).start();

      }
      else if(cameraParameters.useStaticTransformFromHeadFrameToSensor())
      {
         staticTransform.set(cameraParameters.getStaticTransformFromHeadFrameToCameraFrame());
         ReferenceFrame headFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent("headToCamera", cameraDataReceiver.getHeadFrame(), staticTransform);
         cameraDataReceiver.setCameraFrame(headFrame);
      }
      else
      {
         cameraDataReceiver.setCameraFrame(cameraDataReceiver.getHeadFrame());
      }

      final RosCameraInfoSubscriber imageInfoSubscriber;
      if (cameraParameters.useIntrinsicParametersFromRos())
      {
         imageInfoSubscriber = new RosCameraInfoSubscriber(cameraParameters.getRosCameraInfoTopicName());
         rosMainNode.attachSubscriber(cameraParameters.getRosCameraInfoTopicName(), imageInfoSubscriber);
      }
      else
      {
         throw new RuntimeException("You really want to use intrinisic parameters from ROS");
      }

      final RobotSide robotSide = cameraParameters.getRobotSide();

//      RosImageSubscriber imageSubscriber = new RosImageSubscriber()
//      {
//         @Override
//         protected void imageReceived(long timeStamp, BufferedImage image)
//         {
//            if (logger != null)
//            {
//               logger.log(image, timeStamp);
//            }
//            IntrinsicParameters intrinsicParameters = imageInfoSubscriber.getIntrinisicParameters();
//            if (DEBUG)
//            {
//               PrintTools.debug(this, "Sending intrinsicParameters");
//               intrinsicParameters.print();
//            }
//            cameraDataReceiver.updateImage(robotSide, image, timeStamp, intrinsicParameters);
//         }
//      };
//      rosMainNode.attachSubscriber(cameraParameters.getRosTopic(), imageSubscriber);

      RosCompressedImageSubscriber imageSubscriberSubscriber = new RosCompressedImageSubscriber()
      {
         @Override
         protected void imageReceived(long timeStamp, BufferedImage image)
         {
            if (logger != null)
            {
               logger.log(image, timeStamp);
            }
            IntrinsicParameters intrinsicParameters = imageInfoSubscriber.getIntrinisicParameters();
            if (DEBUG)
            {
               PrintTools.debug(this, "Sending intrinsicParameters");
               intrinsicParameters.print();
            }
            cameraDataReceiver.updateImage(robotSide, image, timeStamp, intrinsicParameters);

         }
      };
      rosMainNode.attachSubscriber(cameraParameters.getRosTopic(), imageSubscriberSubscriber);
   }

}
