package us.ihmc.SdfLoader;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.xmlDescription.Collision;
import us.ihmc.SdfLoader.xmlDescription.SDFLink;
import us.ihmc.SdfLoader.xmlDescription.SDFSensor;
import us.ihmc.SdfLoader.xmlDescription.SDFVisual;
import us.ihmc.utilities.math.MatrixTools;

public class SDFLinkHolder
{
   // From SDF File
   private final String name;
   private final Transform3D transformToModelReferenceFrame;
   private final double mass;
   private final Transform3D inertialFrameWithRespectToLinkFrame;
   private final Matrix3d inertia;
   
   private double contactKp = 0.0;
   private double contactKd = 0.0;
   private double contactMaxVel = 0.0;;
   
   private final List<SDFVisual> visuals;
   private final List<SDFSensor> sensors;
   private final List<Collision> collisions;
   
   // Set by loader
   private SDFJointHolder joint = null;
   private final ArrayList<SDFJointHolder> childeren = new ArrayList<SDFJointHolder>();

   
   // Calculated
   private final Vector3d CoMOffset = new Vector3d();

   public SDFLinkHolder(SDFLink sdfLink)
   {
     name = SDFConversionsHelper.sanitizeJointName(sdfLink.getName());
     transformToModelReferenceFrame = SDFConversionsHelper.poseToTransform(sdfLink.getPose());
     
     if(sdfLink.getInertial() != null)
     {
        inertialFrameWithRespectToLinkFrame = SDFConversionsHelper.poseToTransform(sdfLink.getInertial().getPose());
        mass = Double.parseDouble(sdfLink.getInertial().getMass());
        inertia = SDFConversionsHelper.sdfInertiaToMatrix3d(sdfLink.getInertial().getInertia());        
     }
     else
     {
        inertialFrameWithRespectToLinkFrame = new Transform3D();
        mass = 0.0;
        inertia = new Matrix3d();
     }
     visuals = sdfLink.getVisuals();
     
     sensors = sdfLink.getSensors();
     if(sdfLink.getCollisions() != null)
     {
        collisions = sdfLink.getCollisions();
        
        if((sdfLink.getCollisions().get(0) != null) && (sdfLink.getCollisions().get(0).getSurface() != null)
                 && (sdfLink.getCollisions().get(0).getSurface().getContact() != null)
                 && (sdfLink.getCollisions().get(0).getSurface().getContact().getOde() != null))
         {
            if (sdfLink.getCollisions().get(0).getSurface().getContact().getOde().getKp() != null)
               contactKp = Double.parseDouble(sdfLink.getCollisions().get(0).getSurface().getContact().getOde().getKp());
            if (sdfLink.getCollisions().get(0).getSurface().getContact().getOde().getKd() != null)
               contactKd = Double.parseDouble(sdfLink.getCollisions().get(0).getSurface().getContact().getOde().getKd());
            if (sdfLink.getCollisions().get(0).getSurface().getContact().getOde().getMaxVel() != null)
               contactMaxVel = Double.parseDouble(sdfLink.getCollisions().get(0).getSurface().getContact().getOde().getMaxVel());
         }
      }
      else
      {
         collisions = new ArrayList<Collision>();
      }
   }

   public Transform3D getTransformFromModelReferenceFrame()
   {
      return transformToModelReferenceFrame;
   }
   
   public void calculateCoMOffset()
   {
      
      Transform3D modelFrameToJointFrame = new Transform3D();
      if(joint != null)
      {
         modelFrameToJointFrame.set(joint.getTransformFromChildLink()); // H_4^3
      }
      Transform3D jointFrameToModelFrame = new Transform3D();    // H_3^4
      jointFrameToModelFrame.invert(modelFrameToJointFrame);
      Transform3D modelFrameToInertialFrame = inertialFrameWithRespectToLinkFrame;    // H_4^5
      
      Transform3D jointFrameToInertialFrame = new Transform3D();
      jointFrameToInertialFrame.mul(jointFrameToModelFrame, modelFrameToInertialFrame);
      
      Vector3d CoMOffset = new Vector3d();
      Matrix3d inertialFrameRotation = new Matrix3d();
      
      jointFrameToInertialFrame.get(inertialFrameRotation, CoMOffset);
      
      if(!inertialFrameRotation.epsilonEquals(MatrixTools.IDENTITY, 1e-5))
      {
         inertialFrameRotation.transpose();
         inertia.mul(inertialFrameRotation);
         inertialFrameRotation.transpose();
         inertialFrameRotation.mul(inertia);

         inertia.set(inertialFrameRotation);
//         inertia.set(InertiaTools.rotate(inertialFrameRotation, inertia));
         inertialFrameWithRespectToLinkFrame.set(MatrixTools.IDENTITY);
      }

      this.CoMOffset.set(CoMOffset);
   }

   public ArrayList<SDFJointHolder> getChildren()
   {
      return childeren;
   }
   
   public void addChild(SDFJointHolder child)
   {
      childeren.add(child);
   }
   
   public SDFJointHolder getJoint()
   {
      return joint;
   }
   
   public void setJoint(SDFJointHolder joint)
   {
      this.joint = joint;
   }
   
   public String toString()
   {
      return name;
   }

   public String getName()
   {
      return name;
   }

   public List<SDFVisual> getVisuals()
   {
     return visuals;
   }

   public double getMass()
   {
      return mass;
   }

   public Matrix3d getInertia()
   {
      return inertia;
   }

   public Vector3d getCoMOffset()
   {
      return CoMOffset;
   }

   public double getContactKp()
   {
      return contactKp;
   }

   public double getContactKd()
   {
      return contactKd;
   }

   public double getContactMaxVel()
   {
      return contactMaxVel;
   }

   public List<SDFSensor> getSensors()
   {
      return sensors;
   }

   public List<Collision> getCollisions()
   {
      return collisions;
   }
   
   
}
