package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class YoRollingContactState implements PlaneContactState, ModifiableContactState
{
   private final RigidBody rigidBody;
   private final YoVariableRegistry registry;
   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final ReferenceFrame updatableContactFrame;
   private final Transform3D transformFromContactFrameToBodyFrame = new Transform3D();
   private final BooleanYoVariable inContact;
   private final DoubleYoVariable coefficientOfFriction;
   private final FrameVector contactNormalFrameVector;
   private final List<YoContactPoint> contactPoints = new ArrayList<YoContactPoint>();
   private final ContactableRollingBody contactableCylinderBody;
   private final int totalNumberOfContactPoints;

   // TODO: Probably get rid of that. Now, it is used for smooth unload/load transitions in the CarIngressEgressController.
   private final DoubleYoVariable wRho;

   // Class enabling to update the contact points of a contactable rolling body as it is rolling on the ground or on another contactable surface
   public YoRollingContactState(String namePrefix, ContactableRollingBody contactableCylinderBody, List<FramePoint2d> contactFramePoints, YoVariableRegistry parentRegistry)
   {
      // The rolling contactable body
      this.contactableCylinderBody = contactableCylinderBody;
      this.rigidBody = contactableCylinderBody.getRigidBody();
      this.registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      this.inContact = new BooleanYoVariable(namePrefix + "InContact", registry);
      this.coefficientOfFriction = new DoubleYoVariable(namePrefix + "CoefficientOfFriction", registry);
      this.updatableContactFrame = new ReferenceFrame(namePrefix + "ContactFrame", getFrameAfterParentJoint())
      {
         private static final long serialVersionUID = 6993243554111815201L;

         @Override
         public void updateTransformToParent(Transform3D transformToParent)
         {
            transformToParent.set(transformFromContactFrameToBodyFrame);
         }
      };
      
      updateContactPoints();
      
      parentRegistry.addChild(registry);
      
      this.contactNormalFrameVector = new FrameVector(updatableContactFrame, 0.0, 0.0, 1.0);

      wRho = new DoubleYoVariable(namePrefix + "_wRhoContactRegularization", registry);
      resetContactRegularization();

      FramePoint2d tempFramePoint = new FramePoint2d(updatableContactFrame);
      
      for (int i = 0; i < contactFramePoints.size(); i++)
      {
         tempFramePoint.setAndChangeFrame(contactFramePoints.get(i));
         tempFramePoint.changeFrame(updatableContactFrame);
         YoContactPoint contactPoint = new YoContactPoint(namePrefix, i, tempFramePoint, this, parentRegistry);
         contactPoints.add(contactPoint);
      }

      totalNumberOfContactPoints = contactPoints.size();
   }

   public void setContactPointsInContact(boolean[] inContact)
   {
      if (inContact.length != totalNumberOfContactPoints)
         throw new RuntimeException("Arrays should be of same length!");

      this.inContact.set(false);
      
      for (int i = 0; i < inContact.length; i++)
      {
         contactPoints.get(i).setInContact(inContact[i]);
         
         if (inContact[i])
         {
            this.inContact.set(true);
         }
      }
   }

   public void setContactPointInContact(int contactPointIndex, boolean inContact)
   {
      contactPoints.get(contactPointIndex).setInContact(inContact);

      if (inContact)
      {
         this.inContact.set(true);
      }
      else
      {
         this.inContact.set(false);
         for (int i = 0; i < totalNumberOfContactPoints; i++)
         {
            if (contactPoints.get(i).isInContact())
            {
               this.inContact.set(true);
            }
         }
      }
   }
   
   public void setCoefficientOfFriction(double coefficientOfFriction)
   {
      if (coefficientOfFriction < 0.0)
         throw new RuntimeException("Coefficient of friction is negative: " + coefficientOfFriction);
      
      this.coefficientOfFriction.set(coefficientOfFriction);
   }

   public void updateContactPoints()
   {
      // The contact reference frame is updated such as:
      // 1- it remains tangential to the contactable cylindrical body,
      // 2- it remains under the contactable cylindrical body (at the lowest height)
      Transform3D transformFromRigiBodyToWorld = getFrameAfterParentJoint().getTransformToDesiredFrame(worldFrame );
      Matrix3d rotationFromRigiBodyToWorld = new Matrix3d();
      transformFromRigiBodyToWorld.get(rotationFromRigiBodyToWorld);

      // Look for the angle theta that will position the reference contact frame. The contact points will be automatically positioned as they are expressed in that reference frame.
      // Ex. for the thigh: theta == 0 => back of the thigh, theta == PI/2 => left side of the thigh (whatever it is the left or right thigh)
      double theta = -Math.PI / 2.0 + Math.atan2(rotationFromRigiBodyToWorld.m20, rotationFromRigiBodyToWorld.m21);

      transformFromContactFrameToBodyFrame.setIdentity();
      Vector3d eulerAngles = new Vector3d(theta, Math.PI / 2.0, 0.0);
      transformFromContactFrameToBodyFrame.setEuler(eulerAngles);
      FramePoint originInBodyFrame = contactableCylinderBody.getCopyOfCylinderOriginInBodyFrame();
      double cylinderRadius = contactableCylinderBody.getCylinderRadius();
      Vector3d translation = new Vector3d(-cylinderRadius  * Math.cos(theta) + originInBodyFrame .getX(), cylinderRadius * Math.sin(theta)
            + originInBodyFrame.getY(), originInBodyFrame.getZ());
      transformFromContactFrameToBodyFrame.setTranslation(translation);

      updatableContactFrame.update();
   }
   
   public List<FramePoint> getCopyOfContactFramePointsInContact()
   {
      List<FramePoint> ret = new ArrayList<FramePoint>(totalNumberOfContactPoints);

      for (int i = 0; i < totalNumberOfContactPoints; i++)
      {
         YoContactPoint contactPoint = contactPoints.get(i);

         if (contactPoint.isInContact())
         {
            FramePoint2d framePoint2d = contactPoint.getPosition2d();
            FramePoint framePoint = new FramePoint(framePoint2d.getReferenceFrame(), framePoint2d.getX(), framePoint2d.getY(), 0.0);
            ret.add(framePoint);
         }
      }

      return ret;
   }

   public List<FramePoint2d> getCopyOfContactFramePoints2dInContact()
   {
      List<FramePoint2d> ret = new ArrayList<FramePoint2d>(totalNumberOfContactPoints);
      for (int i = 0; i < totalNumberOfContactPoints; i++)
      {
         YoContactPoint contactPoint = contactPoints.get(i);
         if (contactPoint.isInContact())
         {
            ret.add(new FramePoint2d(contactPoint.getPosition2d()));
         }
      }

      return ret;
   }

   public ReferenceFrame getFrameAfterParentJoint()
   {
      return rigidBody.getParentJoint().getFrameAfterJoint();
   }

   public ReferenceFrame getPlaneFrame()
   {
      return updatableContactFrame;
   }

   public boolean inContact()
   {
      return inContact.getBooleanValue();
   }

   public double getCoefficientOfFriction()
   {
      return coefficientOfFriction.getDoubleValue();
   }

   public int getNumberOfContactPointsInContact()
   {
      int numberOfContactPointsInContact = 0;
      
      for (int i = 0; i < totalNumberOfContactPoints; i++)
      {
         if (contactPoints.get(i).isInContact())
            numberOfContactPointsInContact++;
      }
      
      return numberOfContactPointsInContact;
   }

   public FrameVector getContactNormalFrameVectorCopy()
   {
      return new FrameVector(contactNormalFrameVector);
   }

   public void getContactNormalFrameVector(FrameVector frameVectorToPack)
   {
	   frameVectorToPack.setAndChangeFrame(contactNormalFrameVector);
   }
   
   public void clear()
   {
      for (int i = 0; i < totalNumberOfContactPoints; i++)
      {
         contactPoints.get(i).setInContact(false);
      }

      inContact.set(false);
   }
   
   public void setFullyConstrained()
   {
      for (int i = 0; i < totalNumberOfContactPoints; i++)
      {
         contactPoints.get(i).setInContact(true);
      }
      
      inContact.set(true);
   }

   public void setRhoContactRegularization(double wRho)
   {
      this.wRho.set(wRho);
   }

   public double getRhoContactRegularization()
   {
      return wRho.getDoubleValue();
   }

   public void resetContactRegularization()
   {
      wRho.set(DEFAULT_WRHO);
   }

   public List<YoContactPoint> getContactPoints()
   {
      return contactPoints;
   }

   public int getTotalNumberOfContactPoints()
   {
      return totalNumberOfContactPoints;
   }

   public RigidBody getRigidBody()
   {
      return rigidBody;
   }
}
