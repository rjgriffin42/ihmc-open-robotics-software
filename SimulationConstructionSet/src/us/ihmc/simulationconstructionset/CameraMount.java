package us.ihmc.simulationconstructionset;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.camera.CameraConfiguration;
import us.ihmc.graphics3DAdapter.camera.CameraMountInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.TransformTools;

public class CameraMount implements CameraMountInterface
{

   private final String name;

   private final RigidBodyTransform offsetTransform;
   private final RigidBodyTransform transformToMount = new RigidBodyTransform();
   private final RigidBodyTransform transformToCamera = new RigidBodyTransform();

   private Joint parentJoint;

   private final Robot rob;

   private DoubleYoVariable pan, tilt, roll;
   
   private double fieldOfView, clipDistanceNear, clipDistanceFar;
   
   public CameraMount(String name, Vector3d offsetVector, Robot rob)
   {
      this(name, offsetVector, CameraConfiguration.DEFAULT_FIELD_OF_VIEW, CameraConfiguration.DEFAULT_CLIP_DISTANCE_NEAR, CameraConfiguration.DEFAULT_CLIP_DISTANCE_FAR, rob);
   }
   
   public CameraMount(String name, RigidBodyTransform camRotation, Robot rob)
   {
      this(name, camRotation, CameraConfiguration.DEFAULT_FIELD_OF_VIEW, CameraConfiguration.DEFAULT_CLIP_DISTANCE_NEAR, CameraConfiguration.DEFAULT_CLIP_DISTANCE_FAR, rob);
   } 

   public CameraMount(String name, Vector3d offsetVector, double fieldOfView, double clipDistanceNear, double clipDistanceFar, Robot rob)
   {
      this(name, TransformTools.createTranslationTransform(offsetVector), fieldOfView, clipDistanceNear, clipDistanceFar, rob);
   }

   public CameraMount(String name, RigidBodyTransform offset, double fieldOfView, double clipDistanceNear, double clipDistanceFar, Robot rob)
   {
      this.name = name;
      this.rob = rob;

      offsetTransform = new RigidBodyTransform(offset);
      
      this.fieldOfView = fieldOfView;
      this.clipDistanceNear = clipDistanceNear;
      this.clipDistanceFar = clipDistanceFar;
   }


   public String getName()
   {
      return name;
   }
   
   protected void setParentJoint(Joint parent)
   {
      this.parentJoint = parent;
   }

   public Joint getParentJoint()
   {
      return parentJoint;
   }

   public String toString()
   {
      return ("name: " + name);
   }

   private boolean enablePanTiltRoll = false;
   private RigidBodyTransform panTiltRollTransform3D, temp1, temp2;

   public void enablePanTiltRoll()
   {
      YoVariableRegistry registry = new YoVariableRegistry("CameraMount");

      pan = new DoubleYoVariable("pan_" + name, registry);
      tilt = new DoubleYoVariable("tilt_" + name, registry);
      roll = new DoubleYoVariable("roll_" + name, registry);

      panTiltRollTransform3D = new RigidBodyTransform();
      temp1 = new RigidBodyTransform();
      temp2 = new RigidBodyTransform();

      enablePanTiltRoll = true;

      rob.addYoVariableRegistry(registry);
   }

   private Point3d tempPoint3d;

   public void lookAt(Point3d center)
   {
      lookAt(center.x, center.y, center.z);
   }

   public RigidBodyTransform lookAtTransform3D;

   public void lookAt(double x, double y, double z)
   {
      if (tempPoint3d == null)
         tempPoint3d = new Point3d();
      if (lookAtTransform3D == null)
         lookAtTransform3D = new RigidBodyTransform();

      // Make camera look at the point "center" by adjusting pan and tilt. Roll doesn't change.
      // This is fairly involved since the camera can be all willy nilly.
      tempPoint3d.set(x, y, z);

      lookAtTransform3D.set(transformToMount);
      lookAtTransform3D.invert();

      lookAtTransform3D.transform(tempPoint3d);    // Put center from world coordinates to mount coordinates.

      // Compute pan and tilt to get there.
      pan.set(Math.atan2(tempPoint3d.y, tempPoint3d.x));
      tilt.set(Math.atan2(-tempPoint3d.z, Math.sqrt(tempPoint3d.x * tempPoint3d.x + tempPoint3d.y * tempPoint3d.y)));
   }


   protected void updateTransform(RigidBodyTransform t1)
   {
      transformToMount.multiply(t1, offsetTransform);    // transformToMount. = t1 * offsetTransform;

      if (enablePanTiltRoll)
      {
         // Note that pan, tilt, roll are in z, y, x ccordinates.
         // transformToCamera = transformToMount * panTiltRollTransform3D

         panTiltRollTransform3D.setRotationYawAndZeroTranslation(pan.getDoubleValue());
         temp1.setRotationPitchAndZeroTranslation(tilt.getDoubleValue());
         panTiltRollTransform3D.multiply(temp1);
         temp1.setRotationRollAndZeroTranslation(roll.getDoubleValue());
         panTiltRollTransform3D.multiply(temp1);

         transformToCamera.multiply(transformToMount, panTiltRollTransform3D);
      }

      else
      {
         transformToCamera.set(transformToMount);
      }

   }

   public void getTransformToMount(RigidBodyTransform transform)
   {
      transform.set(transformToMount);
   }

   public void getTransformToCamera(RigidBodyTransform transform)
   {
      transform.set(transformToCamera);
   }
   
   public void setOffset(RigidBodyTransform newOffsetTransform)
   {
      offsetTransform.set(newOffsetTransform);
   }
   
   public void setRoll(double roll)
   {
      if(enablePanTiltRoll)
      {
         this.roll.set(roll);
      }
   }
   
   public void setPan(double pan)
   {
      if(enablePanTiltRoll)
      {
         this.pan.set(pan);
      }
   }
   
   public void setTilt(double tilt)
   {
      if(enablePanTiltRoll)
      {
         this.tilt.set(tilt);
      }
   }

   public double getFieldOfView()
   {
      return fieldOfView;
   }

   public double getClipDistanceNear()
   {
      return clipDistanceNear;
   }

   public double getClipDistanceFar()
   {
      return clipDistanceFar;
   }

   public void zoom(double amount)
   {
      fieldOfView = fieldOfView + amount;
      
      if(fieldOfView < 0.01)
      {
         fieldOfView = 0.01;
      }
      else if (fieldOfView > 3.0)
      {
         fieldOfView = 3.0;
      }
   }
}
