package us.ihmc.commonWalkingControlModules.bipedSupportPolygons;

import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;


public interface ContactableCylinderBody
{
   public abstract String getName();
   public abstract RigidBody getRigidBody();
   public abstract ReferenceFrame getBodyFrame();
   public abstract ReferenceFrame getCylinderFrame();
   public abstract double getHalfHandWidth();
   public abstract double getCylinderRadius();
   public abstract double getGripStrength();
   public abstract double getGripWeaknessFactor();
}