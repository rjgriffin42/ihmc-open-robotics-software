package us.ihmc.simulationconstructionset.yoUtilities.graphics;

import javax.vecmath.Vector3d;

import us.ihmc.plotting.Artifact;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.Transform3d;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFramePoint;

public abstract class YoGraphicAbstractShape extends YoGraphic
{
   protected final YoFramePoint yoFramePoint;
   protected final YoFrameOrientation yoFrameOrientation;
   protected final double scale;
   private Vector3d translationVector = new Vector3d();

   protected YoGraphicAbstractShape(String name, YoFramePoint framePoint, YoFrameOrientation frameOrientation, double scale)
   {
      super(name);
      framePoint.checkReferenceFrameMatch(ReferenceFrame.getWorldFrame());

      this.yoFramePoint = framePoint;
      this.yoFrameOrientation = frameOrientation;

      this.scale = scale;
   }

   public void setPosition(double x, double y, double z)
   {
      yoFramePoint.set(x, y, z);
   }

   public void getPosition(FramePoint framePointToPack)
   {
      yoFramePoint.getFrameTuple(framePointToPack);
   }

   public void setPosition(FramePoint position)
   {
      yoFramePoint.set(position);
   }

   public void getOrientation(FrameOrientation orientationToPack)
   {
      this.yoFrameOrientation.getFrameOrientationIncludingFrame(orientationToPack);
   }

   public void setOrientation(FrameOrientation orientation)
   {
      this.yoFrameOrientation.set(orientation);
   }

   public void setYawPitchRoll(double yaw, double pitch, double roll)
   {
      this.yoFrameOrientation.setYawPitchRoll(yaw, pitch, roll);
   }

   public void setTransformToWorld(RigidBodyTransform transformToWorld)
   {
      Vector3d translationToWorld = new Vector3d();

      transformToWorld.get(translationToWorld);

      this.yoFramePoint.set(translationToWorld);
      FrameOrientation orientation = new FrameOrientation(ReferenceFrame.getWorldFrame(), transformToWorld);

      double[] yawPitchRoll = orientation.getYawPitchRoll();
      yoFrameOrientation.setYawPitchRoll(yawPitchRoll[0], yawPitchRoll[1], yawPitchRoll[2]);
   }

   public void setToReferenceFrame(ReferenceFrame referenceFrame)
   {
      if (referenceFrame == null)
         throw new RuntimeException("referenceFrame == null");

      RigidBodyTransform transformToWorld = new RigidBodyTransform();
      ReferenceFrame ancestorFrame = referenceFrame;

      // March up the parents until you get to the world:
      while (!ancestorFrame.isWorldFrame())
      {
         RigidBodyTransform transformToAncestor = ancestorFrame.getTransformToParent();

         RigidBodyTransform tempTransform3D = new RigidBodyTransform(transformToAncestor);
         tempTransform3D.multiply(transformToWorld);

         transformToWorld = tempTransform3D;

         ReferenceFrame newAncestorFrame = ancestorFrame.getParent();

         if (newAncestorFrame == null)
            throw new RuntimeException("No ancestor path to world. referenceFrame = " + referenceFrame + ", most ancient = " + ancestorFrame);

         ancestorFrame = newAncestorFrame;
      }

      setTransformToWorld(transformToWorld);
   }

   private Vector3d rotationEulerVector = new Vector3d();

   protected void computeRotationTranslation(Transform3d transform3D)
   {
      transform3D.setIdentity();
      translationVector.set(yoFramePoint.getX(), yoFramePoint.getY(), yoFramePoint.getZ());
      yoFrameOrientation.getEulerAngles(rotationEulerVector);

      transform3D.setEuler(rotationEulerVector);
      transform3D.setTranslation(translationVector);
      transform3D.setScale(scale);
   }

   public Artifact createArtifact()
   {
      throw new RuntimeException("Implement Me!");
   }

   @Override
   protected boolean containsNaN()
   {
      if (yoFramePoint.containsNaN())
         return true;
      if (yoFrameOrientation.containsNaN())
         return true;

      return false;
   }
}