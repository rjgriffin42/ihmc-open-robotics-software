package us.ihmc.vicon;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.remote.RemoteConnection;
import us.ihmc.utilities.remote.RemoteRequest;

public class ViconClient
{
   private boolean DEBUG = false;
   private static ViconClient viconSingleton;
   protected RemoteConnection viconServer;
   private HashMap<String, PoseReading> mapModelToPoseReading = new HashMap<String, PoseReading>();
   private String requestedModel;
   private String myIP;
   private int myPort = 4444;
   private long desiredUpdateRateInMillis = 5;

   private ViconClient(String ip) throws Exception
   {
      viconServer = new RemoteConnection();
      viconServer.connect(ip);

      ArrayList<String> availableModels = getAvailableModels();
      System.out.println("available models = " + availableModels);

      for (String modelName : availableModels)
      {
         mapModelToPoseReading.put(modelName, null);
      }

      ViconPoseListener poseListener = new ViconPoseListener();
      Thread thread = new Thread(poseListener);
      thread.start();

      myIP = InetAddress.getLocalHost().getHostAddress();
      System.out.println("my ip is " + myIP);
      registerPoseListener(myIP, new Integer(myPort), availableModels.get(0), new Long(desiredUpdateRateInMillis));
      System.out.println(" should be listening for " + availableModels.get(0));
      System.out.println("***" + getPose(availableModels.get(0)));
   }

   public static ViconClient getInstance() throws Exception
   {
      if (viconSingleton == null)
      {
         viconSingleton = new ViconClient("10.4.1.100");
      }

      return viconSingleton;
   }

   public ArrayList<String> getAvailableModels()
   {
      RemoteRequest remoteRequest = new RemoteRequest("getAvailableModels", null);
      try
      {
         return (ArrayList<String>) viconServer.SendRequest(remoteRequest);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      return null;
   }

   public Pose getPose(String modelName)
   {
      PoseReading poseReading = getPoseReading(modelName);
      if (poseReading != null)

         return poseReading.getPose();
      else
         return null;
   }

   public PoseReading getPoseReading(String modelName)
   {
      PoseReading pose;
      synchronized (mapModelToPoseReading)
      {
         pose = mapModelToPoseReading.get(modelName);
      }

      if (pose == null)
         return null;

      return pose;

   }

   public void registerPoseListener(String host, Integer port, String modelName, Long updateRateInMillis)
   {
      requestedModel = modelName;
      Vector<Serializable> parameters = new Vector<Serializable>();
      parameters.add(host);
      parameters.add(port);
      parameters.add(modelName);
      parameters.add(updateRateInMillis);
      RemoteRequest remoteRequest = new RemoteRequest("registerPoseListener", parameters);
      try
      {
         viconServer.SendObject(remoteRequest);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public class ViconPoseListener implements Runnable
   {
      protected ServerSocket serverSocket;
      protected Socket client;

      public void run()
      {
         try
         {
            serverSocket = new ServerSocket(4444);
            client = serverSocket.accept();
            ObjectInputStream _ois = new ObjectInputStream(client.getInputStream());

            long startTime = System.currentTimeMillis();
            int updateCount = 0;
            int nonUpdate = 0;
            PoseReading lastPoseReading = null;
            while ((client != null) &&!client.isClosed())
            {
               Object obj = _ois.readObject();

               if (obj instanceof PoseReading)
               {
                  PoseReading poseReading = (PoseReading) obj;

                  // System.out.println(poseReading);
                  synchronized (mapModelToPoseReading)
                  {
                     mapModelToPoseReading.put(requestedModel, poseReading);
//                     Pose pose = poseReading.getPose();
//                     Transform3D transform3d = new Transform3D();
//                     transform3d.setEuler(new Vector3d(pose.xAxisRotation, pose.yAxisRotation, pose.zAxisRotation));
//                     transform3d.setTranslation(new Vector3d(pose.xPosition, pose.yPosition, pose.zPosition));

//                     ViconFrames.updateTransformToParent(requestedModel, transform3d);
                  }

                  // System.out.println(poseReading);

                  if (DEBUG)
                  {
                     if ((lastPoseReading != null) &&!poseReading.equals(lastPoseReading))
                     {
                        updateCount++;
                        long endTime = System.currentTimeMillis();
                        if ((endTime - startTime) > 1000)
                        {
                           System.out.println("PoseListener updating at " + (int) ((double) updateCount / ((double) (endTime - startTime) / 1000.0)) + " Hz: "
                                              + "(" + nonUpdate + ") " + poseReading);
                           startTime = endTime;
                           updateCount = 0;
                           nonUpdate = 0;
                        }
                     }
                     else
                     {
                        System.out.println("***");
                        nonUpdate++;
                     }

                     lastPoseReading = poseReading;
                  }
               }
               else
               {
                  System.out.println("ViconPoseListenerThread_: received obj which is not a Pose: " + obj.getClass());
               }

               Thread.sleep(1);
            }
         }
         catch (Exception xcp)
         {
            if (DEBUG)
               System.out.println("ViconPoseListenerThread_ done");
            xcp.printStackTrace();
         }

         try
         {
            client.close();
            serverSocket.close();
            if (DEBUG)
               System.out.println(".ViconPoseListenerThread_: ServerAccept Thread has Stopped.");
         }
         catch (IOException xcp)
         {
            xcp.printStackTrace();
         }
      }
   }


   public static void main(String[] args)
   {
//      try
//      {
//         System.out.println("inet: " + InetAddress.getLocalHost());
//         Enumeration inets = NetworkInterface.getNetworkInterfaces();
//         while (inets.hasMoreElements())
//         {
//            System.out.println(inets.nextElement());
//            System.out.println("--------------------------------");
//         }
//      }
//      catch (Exception e)
//      {
//         e.printStackTrace();
//      }

      String ip = "10.4.1.100";

      // String ip = "10.2.36.1";

      for (int i = 0; i < args.length - 1; i++)
      {
         if (args[i].equalsIgnoreCase("-ip"))
         {
            ip = args[i + 1];

            break;
         }
      }

      try
      {
         ViconClient client = new ViconClient(ip);
         ArrayList<String> availableModels = client.getAvailableModels();
         String modelName = availableModels.get(0);
         long startTime = System.currentTimeMillis();
         int updateCount = 0;
         PoseReading lastPose = null;

         while (true)
         {
            PoseReading pose = client.getPoseReading(modelName);
            if ((lastPose != null) && (!pose.equals(lastPose)))
            {
               updateCount++;

               long endTime = System.currentTimeMillis();
               if ((endTime - startTime) > 300)
               {
                  double dt = (endTime - startTime) / 1000.0;
                  System.out.println(modelName + " rate = " + (int) (updateCount / dt) + ": " + pose.getVelocity());
                  startTime = endTime;
                  updateCount = 0;
               }
            }

            lastPose = pose;

            Thread.sleep(1);
         }
      }
      catch (Exception ex1)
      {
         ex1.printStackTrace();
      }

      // wait around until terminated
      synchronized (Thread.currentThread())
      {
         try
         {
            Thread.currentThread().wait();
         }
         catch (InterruptedException ex)
         {
            ex.printStackTrace();
         }
      }
   }

   public boolean isConnected()
   {
      return viconServer.isConnected();
   }
}
