package us.ihmc.simulationconstructionset.util.perturbance;

import javax.vecmath.Vector3d;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;


public class CollisionPerturbance implements DirectedPerturbance
{
   private final String name = "CollisionPerturbance";
   private final YoVariableRegistry registry = new YoVariableRegistry(name);
   private final Collidable collidable;
   private final double ballVelocityMagnitude;
   private final DoubleYoVariable disturbanceEnergy = new DoubleYoVariable("disturbanceEnergy", registry);
   private final DoubleYoVariable coefficientOfRestitution = new DoubleYoVariable("coefficientOfRestitution", registry);

   public CollisionPerturbance(Collidable collidable, double ballVelocity, double disturbanceEnergy, double coefficientOfRestitution, YoVariableRegistry parentRegistry)
   {
      this.collidable = collidable;
      this.ballVelocityMagnitude = ballVelocity;
      this.disturbanceEnergy.set(disturbanceEnergy);
      this.coefficientOfRestitution.set(coefficientOfRestitution);
      
      if (parentRegistry != null)
         parentRegistry.addChild(registry);
   }

   public void perturb(Vector3d direction)
   {
      Vector3d ballVelocity = new Vector3d(direction);
      if (ballVelocity.lengthSquared() > 0.0)
      {
         ballVelocity.normalize();
         ballVelocity.scale(ballVelocityMagnitude);
         collidable.handleCollision(ballVelocity, getBallMass(), coefficientOfRestitution);
      }
   }

   public double getBallMass()
   {
      return 2.0 * disturbanceEnergy.getDoubleValue() / (ballVelocityMagnitude * ballVelocityMagnitude);
   }
   
   public double getBallVelocityMagnitude()
   {
      return ballVelocityMagnitude;
   }

   public void doEveryTick()
   {
      // empty
   }
}
