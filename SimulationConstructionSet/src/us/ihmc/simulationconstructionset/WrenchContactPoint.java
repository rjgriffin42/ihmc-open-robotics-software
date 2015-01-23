package us.ihmc.simulationconstructionset;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import us.ihmc.utilities.math.geometry.RigidBodyTransform;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

// TODO delete if not used
public class WrenchContactPoint extends ExternalForcePoint
{

   //TODO: Change to a YoFramePoint. Make things private instead of public
   public DoubleYoVariable tdx, tdy, tdz;    // Touchdown position
   public DoubleYoVariable fs;    // Foot Switch TODO: BooleanYoVariable or EnumYoVariable

   Link link;
   RigidBodyTransform toWorld = new RigidBodyTransform();
   Vector3d v = new Vector3d();

   public WrenchContactPoint(String name, YoVariableRegistry registry, Link link )
   {
      super(name, new Vector3d(), registry);

      this.link = link;

      tdx = new DoubleYoVariable(name + "_tdX", "WrenchContactPoint x touchdown location", registry);
      tdy = new DoubleYoVariable(name + "_tdY", "WrenchContactPoint y touchdown location", registry);
      tdz = new DoubleYoVariable(name + "_tdZ", "WrenchContactPoint z touchdown location", registry);

      fs = new DoubleYoVariable(name + "_fs", "WrenchContactPoint foot switch", registry);
   }

   public void updateForce() {
      Vector3d force = link.getParentJoint().physics.Z_hat_i.top;
      fs.set(force.length());
      System.out.println("force on sensor: "+fs.getDoubleValue());
   }

   public void updateStatePostPhysicsComputation()
   {
      // compute location in world
      v.set(0, 0, 0);
      link.getParentJoint().getTransformToWorld(toWorld);
      toWorld.transform(v);

      tdx.set(v.getX());
      tdy.set(v.getY());
      tdz.set(v.getZ());
   }
   
   public boolean isInContact()
   {
      return (fs.getDoubleValue() > 0.5);
   }
   
   public void setIsInContact(boolean isInContact)
   {
      if (isInContact)
         fs.set(1.0);
      else
         fs.set(0.0);
   }

   public void getTouchdownLocation(Point3d touchdownLocationToPack)
   {
      touchdownLocationToPack.set(tdx.getDoubleValue(), tdy.getDoubleValue(), tdz.getDoubleValue());
   }

   public void setTouchdownLocation(Point3d touchdownLocation)
   {
      tdx.set(touchdownLocation.getX());
      tdy.set(touchdownLocation.getY());
      tdz.set(touchdownLocation.getZ());
   }
}
