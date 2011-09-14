package us.ihmc.vicon;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class ViconReferenceFrame extends ReferenceFrame
{
   private static final long serialVersionUID = 7818344761817957064L;
   private final ViconClient viconClient;
   private final String bodyName;
   private boolean dataValid = false;
   private long lastUpdateTimeStamp = 0;
   
   private final Quat4d bodyToWorldQuaternion = new Quat4d();
   private final Vector3d bodyToWorldTranslation = new Vector3d();
   private final Transform3D bodyToWorldTransform = new Transform3D();

   public ViconReferenceFrame(String bodyName, ReferenceFrame parentFrame, Transform3D transformToParent, ViconClient viconClient)
   {
      super(bodyName.replace(":", "_"), parentFrame, transformToParent, false, false, false);
      this.bodyName = bodyName;
      this.viconClient = viconClient;
   }

   @Override
   public void updateTransformToParent(Transform3D transformToParent)
   {
      QuaternionPose pose = viconClient.getQuaternionPose(bodyName);
      if (pose == null)
      {
         pose = new QuaternionPose();
      }

      setDataValid(pose.dataValid);
      bodyToWorldQuaternion.set(pose.qx, pose.qy, pose.qz, pose.qw);
      bodyToWorldTransform.set(bodyToWorldQuaternion);
      bodyToWorldTranslation.set(pose.xPosition, pose.yPosition, pose.zPosition);
      bodyToWorldTransform.setTranslation(bodyToWorldTranslation);
      transformToParent.set(bodyToWorldTransform);
      setLastUpdateTimeStamp(viconClient.getModelReadingTimeStamp(bodyName));
   }

   private synchronized void setDataValid(boolean dataValid)
   {
      this.dataValid = dataValid;      
   }
   
   public synchronized boolean isDataValid()
   {
      return dataValid;
   }

   public long getLastUpdateTimeStamp()
   {
      return lastUpdateTimeStamp;
   }

   public void setLastUpdateTimeStamp(long lastUpdateTimeStamp)
   {
      this.lastUpdateTimeStamp = lastUpdateTimeStamp;
   }

}
