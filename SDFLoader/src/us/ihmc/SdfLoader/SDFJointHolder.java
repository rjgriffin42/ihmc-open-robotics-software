package us.ihmc.SdfLoader;

import java.io.IOException;
import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.SdfLoader.xmlDescription.SDFJoint;

public class SDFJointHolder
{
   // Data from SDF
   private final String name;
   private final JointType type;
   private final Vector3d axisInModelFrame;
   
   private final boolean hasLimits;
   private final double upperLimit;
   private final double lowerLimit;
   
   private final double effortLimit;
   private final double velocityLimit;
   
   
   private final Transform3D transformFromChildLink;
   private double damping = 0.0;
   private double friction = 0.0;
   
   // Extra data
   private final ArrayList<SDFForceSensor> forceSensors = new ArrayList<>();
   
   // Set by loader
   private SDFLinkHolder parent;
   private SDFLinkHolder child;

   //Calculated 
   private Transform3D transformToParentJoint = null;
   private final Matrix3d linkRotation = new Matrix3d();
   private final Vector3d offsetFromParentJoint = new Vector3d();
   private final Vector3d axisInParentFrame = new Vector3d();
   private final Vector3d axisInJointFrame = new Vector3d();
   
   private double contactKp;
   private double contactKd;
   private double maxVel;

   public SDFJointHolder(SDFJoint sdfJoint, SDFLinkHolder parent, SDFLinkHolder child)   throws IOException
   {
      name = createValidVariableName(sdfJoint.getName());
      String typeString = sdfJoint.getType();

      if (typeString.equalsIgnoreCase("revolute"))
      {
         type = JointType.REVOLUTE;
      }
      else if (typeString.equalsIgnoreCase("prismatic"))
      {
         type = JointType.PRISMATIC;
      }
      else
      {
         throw new IOException("Joint type " + typeString + " not implemented yet");
      }

      axisInModelFrame = SDFConversionsHelper.stringToNormalizedVector3d(sdfJoint.getAxis().getXyz());
      
      if(sdfJoint.getAxis().getLimit() != null)
      {
         hasLimits = true;
         upperLimit = Double.parseDouble(sdfJoint.getAxis().getLimit().getUpper());
         lowerLimit = Double.parseDouble(sdfJoint.getAxis().getLimit().getLower());
         
         if(sdfJoint.getAxis().getLimit().getVelocity() != null)
         {
            velocityLimit = Double.parseDouble(sdfJoint.getAxis().getLimit().getVelocity());
         }
         else
         {
            velocityLimit = Double.NaN;
         }
         
         if(sdfJoint.getAxis().getLimit().getEffort() != null)
         {
            effortLimit = Double.parseDouble(sdfJoint.getAxis().getLimit().getEffort());
         }
         else
         {
            effortLimit = Double.NaN;
         }
      }
      else
      {
         hasLimits = false;
         upperLimit = Double.POSITIVE_INFINITY;
         lowerLimit = Double.NEGATIVE_INFINITY;
         
         velocityLimit = Double.NaN;
         effortLimit = Double.NaN;
      }
      
      if(sdfJoint.getAxis().getDynamics() != null)
      {
         if(sdfJoint.getAxis().getDynamics().getFriction() != null)
         {
            friction = Double.parseDouble(sdfJoint.getAxis().getDynamics().getFriction());
         }
         if(sdfJoint.getAxis().getDynamics().getDamping() != null)
         {
            damping = Double.parseDouble(sdfJoint.getAxis().getDynamics().getDamping());
         }
      }
      
      transformFromChildLink = SDFConversionsHelper.poseToTransform(sdfJoint.getPose());

      if(parent == null || child == null)
      {
         throw new IOException("Cannot make joint with null parent or child links, joint name is " + sdfJoint.getName());
      }
      
      this.parent = parent;
      this.child = child;
      parent.addChild(this);
      child.setJoint(this);
      
      calculateContactGains();
   }
   
   public static String createValidVariableName(String name)
   {
      name = name.trim().replaceAll("[//[//]///]", "");
      return name;
   }
   
   private void calculateContactGains()
   {
      double parentKp = parent.getContactKp();
      double childKp = child.getContactKp();
      
      if(Math.abs(parentKp) > 1e-3 && Math.abs(childKp) > 1e-3)
      {
         contactKp = 1.0 / (( 1.0 / parentKp ) + (1.0 / childKp));
      }
      else if (Math.abs(parentKp) > 1e-3)
      {
         contactKp = parentKp;
      }
      else if (Math.abs(childKp) > 1e-3)
      {
         contactKp = childKp;
      }
      
      contactKd = parent.getContactKd() + child.getContactKd();
      
      maxVel = Math.min(parent.getContactMaxVel(), child.getContactMaxVel());
      
   }

   public void calculateTransformToParent()
   {


      Transform3D modelToParentLink = getParent().getTransformFromModelReferenceFrame();
      Transform3D modelToChildLink = getChild().getTransformFromModelReferenceFrame();

      Transform3D rotationTransform = new Transform3D();
      Transform3D parentLinkToParentJoint;
      
      SDFJointHolder parentJoint = parent.getJoint();
      if (parentJoint != null)
      {
         rotationTransform.setRotation(parentJoint.getLinkRotation());
         parentLinkToParentJoint = parentJoint.getTransformFromChildLink();
      }
      else
      {
         parentLinkToParentJoint = new Transform3D();
      }

      Transform3D modelToParentJoint = new Transform3D();
      Transform3D modelToChildJoint = new Transform3D();

      modelToParentJoint.mul(modelToParentLink, parentLinkToParentJoint);
      
      modelToChildLink.get(linkRotation);
      
      modelToChildJoint.mul(modelToChildLink, transformFromChildLink);

      Transform3D parentJointToModel = new Transform3D();
      parentJointToModel.invert(modelToParentJoint);

      Transform3D parentJointToChildJoint = new Transform3D();
      parentJointToChildJoint.mul(parentJointToModel, modelToChildJoint);

      transformToParentJoint = parentJointToChildJoint;
      
      parentJointToChildJoint.get(offsetFromParentJoint);
      rotationTransform.transform(offsetFromParentJoint);
      
      linkRotation.transform(axisInModelFrame, axisInParentFrame);
      
      Transform3D transformFromParentJoint = new Transform3D(modelToChildJoint);
      transformFromParentJoint.transform(axisInParentFrame, axisInJointFrame);
      
   }

   public String getName()
   {
      return name;
   }

   public JointType getType()
   {
      return type;
   }

   public Vector3d getAxisInModelFrame()
   {
      return axisInModelFrame;
   }

   public double getUpperLimit()
   {
      return upperLimit;
   }

   public double getLowerLimit()
   {
      return lowerLimit;
   }
   
   public boolean hasLimits()
   {
      return hasLimits;
   }

   public Transform3D getTransformFromChildLink()
   {
      return transformFromChildLink;
   }

   public SDFLinkHolder getParent()
   {
      return parent;
   }

   public SDFLinkHolder getChild()
   {
      return child;
   }

   public Transform3D getTransformToParentJoint()
   {
      return transformToParentJoint;
   }

   public double getContactKp()
   {
      return contactKp;
   }

   public double getContactKd()
   {
      return contactKd;
   }

   public double getMaxVel()
   {
      return maxVel;
   }

   public String toString()
   {
      return name;
   }

   public double getDamping()
   {
      return damping;
   }

   public double getFriction()
   {
      return friction;
   }

   public double getEffortLimit()
   {
      return effortLimit;
   }

   public double getVelocityLimit()
   {
      return velocityLimit;
   }
   
   public Matrix3d getLinkRotation()
   {
      return linkRotation;
   }
   
   public Vector3d getOffsetFromParentJoint()
   {
      return offsetFromParentJoint;
   }
   
   public Vector3d getAxisInParentFrame()
   {
      return axisInParentFrame;
   }
   
   public Vector3d getAxisInJointFrame()
   {
      return axisInJointFrame;
   }

   
   // Temporary hack to get force sensors nicely in the code
   public ArrayList<SDFForceSensor> getForceSensors()
   {
      return forceSensors;
   }
   
   public void addForceSensor(SDFForceSensor forceSensor)
   {
      forceSensors.add(forceSensor);
   }
   
}
