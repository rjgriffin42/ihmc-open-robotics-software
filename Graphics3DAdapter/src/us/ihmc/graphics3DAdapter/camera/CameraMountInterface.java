package us.ihmc.graphics3DAdapter.camera;

import us.ihmc.robotics.geometry.RigidBodyTransform;

public interface CameraMountInterface
{
   public abstract void getTransformToCamera(RigidBodyTransform transformToPack);
   public abstract double getFieldOfView();
   public abstract double getClipDistanceNear();
   public abstract double getClipDistanceFar();
   public abstract String getName();
   
   public void zoom(double amount);
}
