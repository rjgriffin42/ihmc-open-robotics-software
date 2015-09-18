package us.ihmc.simulationconstructionset.yoUtilities.graphics;

import us.ihmc.graphics3DAdapter.graphics.Graphics3DObject;
import us.ihmc.plotting.Artifact;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.Transform3d;

public abstract class YoGraphic
{
   private static final boolean USE_JESPERS_BUGGY_HACK_TO_TRY_TO_PUT_YOGRAPHICS_WITH_RESPECT_TO_ROBOT = false;

   private final String name;

   private boolean showGraphicObject = true;
   private final RigidBodyTransform rootTransform = new RigidBodyTransform();
   private final Transform3d objectTransform;
   private final Transform3d transform = new Transform3d();

   protected abstract void computeRotationTranslation(Transform3d transform3D);

   protected abstract boolean containsNaN();

   public abstract Graphics3DObject getLinkGraphics();

   public abstract Artifact createArtifact();

   public YoGraphic(String name)
   {
      this.name = name;
      objectTransform = new Transform3d();
   }

   public void showGraphicObject()
   {
      showGraphicObject = true;
   }

   public void hideGraphicObject()
   {
      showGraphicObject = false;
   }

   public void setVisible(boolean visible)
   {
      showGraphicObject = visible;
   }

   public boolean isGraphicObjectShowing()
   {
      return showGraphicObject;
   }

   public String getName()
   {
      return name;
   }
   
   public void setRootTransform(RigidBodyTransform transform)
   {
      rootTransform.set(transform);
   }

   public final Transform3d getTransform()
   {
      if (showGraphicObject && !containsNaN())
      {
         computeRotationTranslation(objectTransform);

         if (USE_JESPERS_BUGGY_HACK_TO_TRY_TO_PUT_YOGRAPHICS_WITH_RESPECT_TO_ROBOT)
         {
            // This is a buggy attempt to make the graphic objects go where the robot is, rather than the estimated robot.
            // It works when simulating, but not when rewinding.
            transform.multiply(rootTransform, objectTransform);
         }
         else
         {
            transform.set(objectTransform);
         }
      }
      else
      {
         transform.setIdentity();
         transform.setScale(0.0);
      }

      return transform;
   }

   @Override
   public String toString()
   {
      return name;
   }
}
