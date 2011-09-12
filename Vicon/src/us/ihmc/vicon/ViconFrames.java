package us.ihmc.vicon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class ViconFrames
{
   protected static ViconFrames viconFramesSingleton;
   protected static final String worldFrameName = "ViconWorld";
   protected ViconClient viconClient;
   protected static HashMap<String, ReferenceFrame> referenceFrames;
   protected static boolean dataValid;
   protected static Transform3D bodyToWorldTransform;
   protected static Vector3d euler;
   protected static Vector3d translation;

   protected ViconFrames() throws Exception
   {
      initialize();
   }

   protected void initialize() throws Exception
   {
      euler = new Vector3d();
      translation = new Vector3d();
      bodyToWorldTransform = new Transform3D();

      ReferenceFrame viconWorldFrame = ReferenceFrame.constructAWorldFrame(worldFrameName);
      referenceFrames = new HashMap<String, ReferenceFrame>();
      referenceFrames.put(viconWorldFrame.getName(), viconWorldFrame);
      System.out.println("adding frame for " + viconWorldFrame.getName());

      viconClient = ViconClient.getInstance();
      ArrayList<String> modelNames = viconClient.getAvailableModels();

      for (String modelName : modelNames)
      {
         final String bodyName = modelName;
         System.out.println("adding frame for " + modelName);
         Transform3D transform3d = new Transform3D();
         ReferenceFrame referenceFrame = new ReferenceFrame(replaceColonWithUnderscore(bodyName), viconWorldFrame, transform3d, false, false, false)
         {
            private static final long serialVersionUID = -9160732749609839626L;

            public void updateTransformToParent(Transform3D transformToParent)
            {
               Pose pose = viconClient.getPose(bodyName);
               if (pose == null)
               {
                  pose = new Pose(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
               }

               dataValid = pose.dataValid;
               euler.set(pose.xAxisRotation, pose.yAxisRotation, pose.zAxisRotation);
               bodyToWorldTransform.setEuler(euler);
               translation.set(pose.xPosition, pose.yPosition, pose.zPosition);
               bodyToWorldTransform.setTranslation(translation);
               transformToParent.set(bodyToWorldTransform);
            }
         };
         
         referenceFrames.put(bodyName, referenceFrame);
      }

      viconClient.attachViconFrames(this);
   }

   public static ViconFrames getInstance() throws Exception
   {
      if (viconFramesSingleton == null)
      {
         viconFramesSingleton = new ViconFrames();
      }

      return viconFramesSingleton;
   }

   public ArrayList<String> getAvailableModels()
   {
      return viconClient.getAvailableModels();
   }

   protected String replaceColonWithUnderscore(String string)
   {
      return string.replace(":", "_");
   }

   public synchronized ReferenceFrame getViconWorldFrame()
   {
      return referenceFrames.get(worldFrameName);
   }

   public void updateTransformToParent(String name)
   {
      ReferenceFrame referenceFrame = referenceFrames.get(name);
      if (referenceFrame != null)
      {
         referenceFrame.update();
      }
   }
   
   public synchronized boolean isDataValid()
   {
      return dataValid;
   }

   public synchronized ReferenceFrame getBodyFrame(String name)
   {
      return referenceFrames.get(name);
   }

   public Collection<ReferenceFrame> getFrames()
   {
      return referenceFrames.values();
   }

   public boolean isConnected()
   {
      return viconClient.isConnected();
   }

   public static void main(String[] args)
   {
      try
      {
         ViconFrames viconFrames = ViconFrames.getInstance();
         Thread.sleep(3000);
         ArrayList<String> modelNames = viconFrames.getAvailableModels();

         while (true)
         {
            ReferenceFrame drone = viconFrames.getBodyFrame(modelNames.get(0));

            FramePose point = new FramePose(drone);

            System.out.println(point.changeFrameCopy(viconFrames.getViconWorldFrame()));
         }

      } catch (Exception e)
      {
         e.printStackTrace();
      }

   }
}
