package us.ihmc.darpaRoboticsChallenge.networkProcessor.camera;

import java.awt.image.BufferedImage;

import com.vividsolutions.jts.operation.buffer.validate.BufferResultValidator;

import us.ihmc.atlas.PPSTimestampOffsetProvider;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.DRCNetworkProcessor;
import us.ihmc.darpaRoboticsChallenge.networkProcessor.state.RobotPoseBuffer;
import us.ihmc.darpaRoboticsChallenge.networking.DRCNetworkProcessorControllerStateHandler;
import us.ihmc.darpaRoboticsChallenge.networking.DRCNetworkProcessorNetworkingManager;
import us.ihmc.graphics3DAdapter.camera.CameraInformationPacket;
import us.ihmc.graphics3DAdapter.camera.IntrinsicCameraParametersPacket;
import us.ihmc.graphics3DAdapter.camera.LocalVideoPacket;
import us.ihmc.graphics3DAdapter.camera.VideoSettings;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.ObjectConsumer;

/**
 * 
 *  Generate simulated camera data and camera info packet from SCS, we use only left eye.
 *  @ref: GazeboCameraDataReceiver, MultiSenseCameraInfoReciever
 */
public class SCSCameraDataReceiver extends CameraDataReceiver implements ObjectConsumer<LocalVideoPacket>
{

   private long numPacket = 0;
   SCSCameraInfoReceiver scsCameraInfoReceiver;
   
   public SCSCameraDataReceiver(RobotPoseBuffer robotPoseBuffer, VideoSettings videoSettings, ObjectCommunicator scsCommunicator, DRCNetworkProcessorNetworkingManager networkingManager, PPSTimestampOffsetProvider ppsTimestampOffsetProvider)
   {
      super(robotPoseBuffer, videoSettings, networkingManager, ppsTimestampOffsetProvider);
      scsCommunicator.attachListener(LocalVideoPacket.class, this);
      scsCameraInfoReceiver = new SCSCameraInfoReceiver(networkingManager.getControllerStateHandler());
      networkingManager.getControllerCommandHandler().setIntrinsicServer(scsCameraInfoReceiver);
   }

   public void consumeObject(LocalVideoPacket object)
   {
      updateLeftEyeImage(object.getImage(), object.getTimeStamp(), object.getFieldOfView());
      scsCameraInfoReceiver.setIntrinsicPacket(object);
    }
   
 
}


class SCSCameraInfoReceiver implements CameraInfoReceiver
{
   private IntrinsicCameraParametersPacket leftParamPacket=null;
   private DRCNetworkProcessorControllerStateHandler stateHandler;
   private double lastFov=Double.NaN;

   public SCSCameraInfoReceiver(DRCNetworkProcessorControllerStateHandler stateHandler)
   {
      this.stateHandler = stateHandler;
   }
   
   @Override
   public void processInformationRequest(CameraInformationPacket object)
   {
      if(object.intrinsic)
         sendIntrinsic(object.cameraId);
   }
      
   @Override
   public void sendIntrinsic(int cameraId)
   {
      new Thread(){
         @Override
         public void run()
         {
            while(leftParamPacket==null)
            {
               System.out.println("waiting for intrinsic information ...");
               try
               {
                  Thread.sleep(100);
               }
               catch (InterruptedException e)
               {
                  e.printStackTrace();
               }
            }
            stateHandler.sendSerializableObject(leftParamPacket);
            System.out.println("send SCS Intrinsic Parameter Packet");
         
         }
      }.start();
   }
   
   public void setIntrinsicPacket(LocalVideoPacket videoObject)
   {
      if(videoObject.getFieldOfView()!=lastFov)
      {
         System.err.println("SCS FoV changed:" + lastFov + " -> " + videoObject.getFieldOfView());
         lastFov=videoObject.getFieldOfView();
      }
      BufferedImage img = videoObject.getImage();
      double f = videoObject.getImage().getWidth()/2 / Math.tan(videoObject.getFieldOfView()/2);
      leftParamPacket = new IntrinsicCameraParametersPacket(f, f, 0, (img.getWidth()-1)/2f,(img.getHeight()-1)/2f , img.getWidth(), img.getHeight());
   }


};
