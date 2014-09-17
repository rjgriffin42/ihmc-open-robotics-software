package us.ihmc.SdfLoader;

import us.ihmc.utilities.math.geometry.Transform3d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.xmlDescription.SDFInertia;

public class SDFConversionsHelper
{

   public static String sanitizeJointName(String dirtyName)
   {
      return dirtyName.trim().replaceAll("[//[//]///]", "").replace(".", "_");
   }

   public static Vector3d stringToNormalizedVector3d(String vector)
   {
      Vector3d vector3d = stringToVector3d(vector);
      vector3d.normalize();
      return vector3d;

   }
   

   public static Vector3d stringToVector3d(String vector)
   {
      String[] vecString = vector.split("\\s+");
      Vector3d vector3d = new Vector3d(Double.parseDouble(vecString[0]), Double.parseDouble(vecString[1]), Double.parseDouble(vecString[2]));
      return vector3d;
   }
   
   public static Vector2d stringToVector2d(String xy)
   {
      String[] vecString = xy.split("\\s+");
   
      Vector2d vector = new Vector2d(Double.parseDouble(vecString[0]), Double.parseDouble(vecString[1]));
      return vector;
   
   }

   public static Transform3d poseToTransform(String pose)
   {
      Transform3d ret = new Transform3d();
      if(pose == null)
      {
         return ret;
      }
      pose = pose.trim();
      String[] data = pose.split("\\s+");
      
      Transform3d translation = new Transform3d();
      Vector3d translationVector = new Vector3d();
      translationVector.setX(Double.parseDouble(data[0]));
      translationVector.setY(Double.parseDouble(data[1]));
      translationVector.setZ(Double.parseDouble(data[2]));
      translation.set(translationVector);
   
      Transform3d rotation = new Transform3d();
      Vector3d eulerAngels = new Vector3d();
      eulerAngels.setX(Double.parseDouble(data[3]));
      eulerAngels.setY(Double.parseDouble(data[4]));
      eulerAngels.setZ(Double.parseDouble(data[5]));
      rotation.setEuler(eulerAngels);
   
      ret.mul(translation, rotation);
   
      return ret;
   }

   public static Matrix3d sdfInertiaToMatrix3d(SDFInertia sdfInertia)
   {
      
      Matrix3d inertia = new Matrix3d();
      if(sdfInertia != null)
      {
         double ixx = Double.parseDouble(sdfInertia.getIxx());
         double ixy = Double.parseDouble(sdfInertia.getIxy());
         double ixz = Double.parseDouble(sdfInertia.getIxz());
         double iyy = Double.parseDouble(sdfInertia.getIyy());
         double iyz = Double.parseDouble(sdfInertia.getIyz());
         double izz = Double.parseDouble(sdfInertia.getIzz());
         inertia.m00 = ixx;
         inertia.m01 = ixy;
         inertia.m02 = ixz;
         inertia.m10 = ixy;
         inertia.m11 = iyy;
         inertia.m12 = iyz;
         inertia.m20 = ixz;
         inertia.m21 = iyz;
         inertia.m22 = izz;
      }
      return inertia;
   }

}
