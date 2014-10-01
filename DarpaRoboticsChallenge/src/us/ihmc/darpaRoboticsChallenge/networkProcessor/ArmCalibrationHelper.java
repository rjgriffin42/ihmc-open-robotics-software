package us.ihmc.darpaRoboticsChallenge.networkProcessor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import us.ihmc.communication.AbstractNetworkProcessorNetworkingManager;
import us.ihmc.communication.packets.dataobjects.RobotConfigurationData;
import us.ihmc.communication.packets.manipulation.CalibrateArmPacket;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotJointMap;
import us.ihmc.darpaRoboticsChallenge.driving.DRCStereoListener;
import us.ihmc.utilities.net.ObjectCommunicator;
import us.ihmc.utilities.net.ObjectConsumer;

public class ArmCalibrationHelper implements DRCStereoListener, ObjectConsumer<CalibrateArmPacket>
{
   private final static String basedir = "/tmp/calibration";

   private final ReentrantLock lock = new ReentrantLock();

   private BufferedImage lastLeftEyeImage;
   private long imageTimestamp;
   private double imageFov;
   private final DRCRobotJointMap jointMap;

   private RobotConfigurationData lastJointConfigurationData;

   public ArmCalibrationHelper(ObjectCommunicator fieldComputerClient, AbstractNetworkProcessorNetworkingManager networkingManager,
         DRCRobotJointMap jointMap)
   {
      this.jointMap = jointMap;
      networkingManager.getControllerCommandHandler().attachListener(CalibrateArmPacket.class, this);
      fieldComputerClient.attachListener(RobotConfigurationData.class, new JointAngleConsumer());
      startPeriodicRecording();
   }
   
   public void startPeriodicRecording()
   {
      new Thread(new Runnable()
      {
         
         @Override
         public void run()
         {
            // TODO Auto-generated method stub
            while(true)
            {
               try
               {
                  Thread.sleep(1000);
               }
               catch (InterruptedException e)
               {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }
               calibrateArm();
            }
         }
      }).start();;
   }

   @Override
   public void leftImage(BufferedImage image, long timestamp, double fov)
   {
      lock.lock();
      lastLeftEyeImage = image;
      imageTimestamp = timestamp;
      imageFov = fov;
      lock.unlock();
   }

   @Override
   public void rightImage(BufferedImage image, long timestamp, double fov)
   {
      // TODO Auto-generated method stub

   }

   private class JointAngleConsumer implements ObjectConsumer<RobotConfigurationData>
   {
      @Override
      public void consumeObject(RobotConfigurationData object)
      {
         lock.lock();
         lastJointConfigurationData = object;
         lock.unlock();
      }
   }

   public void calibrateArm()
   {
      lock.lock();
      try
      {
         if (lastLeftEyeImage == null || lastJointConfigurationData == null)
         {
            System.err.println("Did not receive image and robotdata yet");
            return;
         }
         

         File captureDir = new File(basedir, String.valueOf(lastJointConfigurationData.getSimTime()));
         captureDir.mkdirs();
         System.out.println("Writing calibration files to " + captureDir);
         File image = new File(captureDir, "leftEyeImage.png");
         File imageData = new File(captureDir, "imageData.m");
         File q = new File(captureDir, "q.m");
         File qout = new File(captureDir, "qout.m");

         ImageIO.write(lastLeftEyeImage, "png", image);
         PrintWriter imageDataWriter = new PrintWriter(imageData);
         imageDataWriter.print("timestamp = ");
         imageDataWriter.println(imageTimestamp);
         imageDataWriter.print("fov = ");
         imageDataWriter.println(imageFov);
         imageDataWriter.close();

         PrintWriter qWriter = new PrintWriter(q);
//         PrintWriter qoutWriter = new PrintWriter(qout);

         String[] jointNames = lastJointConfigurationData.getJointNames();
         for (int i = 0; i < jointNames.length; i++)
         {
            qWriter.print(jointNames[i]);
            qWriter.print("=");
            qWriter.println(lastJointConfigurationData.getJointAngles()[i]);

//            qoutWriter.print(jointNames[i]);
//            qoutWriter.print("=");
//            qoutWriter.println(lastJointConfigurationData.getJointOutAngles()[i]);
         }

         qWriter.close();
//         qoutWriter.close();
         System.out.println("Wrote calibration files");
      }
      catch (IOException e)
      {
         System.err.println("Cannot write joint angles");
         e.printStackTrace();
      }
      finally
      {
         lock.unlock();
      }
   }

   public void consumeObject(CalibrateArmPacket object)
   {
      calibrateArm();
   }

}
