package us.ihmc.simulationconstructionset.util.environments;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.simulationconstructionset.GroundContactPoint;
import us.ihmc.simulationconstructionset.Joint;
import us.ihmc.simulationconstructionset.NullJoint;
import us.ihmc.simulationconstructionset.Robot;
import us.ihmc.simulationconstructionset.util.ground.Contactable;
import us.ihmc.utilities.math.geometry.RigidBodyTransform;

public abstract class ContactableStaticRobot extends Robot implements Contactable
{
   private static final long serialVersionUID = -3371489685439165270L;
   private final InternalArticulatedContactable articulatedContactable;

   public ContactableStaticRobot(String name)
   {
      super(name);
      articulatedContactable = new InternalArticulatedContactable(name, this); 
   }
   
   public abstract NullJoint getNullJoint();

   private static class InternalArticulatedContactable extends ArticulatedContactable
   {
      private final ContactableStaticRobot contactableRobot;

      public InternalArticulatedContactable(String name, ContactableStaticRobot robot)
      {
         super(name, robot);
         this.contactableRobot = robot;
      }

      public boolean isClose(Point3d pointInWorldToCheck)
      {
         return contactableRobot.isClose(pointInWorldToCheck);
      }

      public boolean isPointOnOrInside(Point3d pointInWorldToCheck)
      {
         return contactableRobot.isPointOnOrInside(pointInWorldToCheck);
      }

      public void closestIntersectionAndNormalAt(Point3d intersectionToPack, Vector3d normalToPack, Point3d pointInWorldToCheck)
      {
         contactableRobot.closestIntersectionAndNormalAt(intersectionToPack, normalToPack, pointInWorldToCheck);
      }

      public Joint getJoint()
      {
         return contactableRobot.getNullJoint();
      }
   }
   
   public void createAvailableContactPoints(int groupIdentifier, int totalContactPointsAvailable, double forceVectorScale, boolean addDynamicGraphicForceVectorsForceVectors)
   {
      articulatedContactable.createAvailableContactPoints(groupIdentifier, totalContactPointsAvailable, forceVectorScale, addDynamicGraphicForceVectorsForceVectors);
   }

   public int getAndLockAvailableContactPoint()
   {
      return articulatedContactable.getAndLockAvailableContactPoint();
   }

   public void unlockContactPoint(GroundContactPoint groundContactPoint)
   {
      articulatedContactable.unlockContactPoint(groundContactPoint);
   }

   public GroundContactPoint getLockedContactPoint(int contactPointIndex)
   {
      return articulatedContactable.getLockedContactPoint(contactPointIndex);
   }

   public void updateContactPoints()
   {
      articulatedContactable.updateContactPoints();
   }

   public void getBodyTransformToWorld(RigidBodyTransform transformToWorld)
   {
      getNullJoint().getTransformToWorld(transformToWorld);
   }

}

