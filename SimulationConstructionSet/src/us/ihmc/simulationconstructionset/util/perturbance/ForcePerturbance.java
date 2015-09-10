package us.ihmc.simulationconstructionset.util.perturbance;

import javax.vecmath.Vector3d;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;


public class ForcePerturbance implements DirectedPerturbance
{
   private final String name = "ForcePerturbance";
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final ForcePerturbable forcePerturbable;

   private final DoubleYoVariable disturbanceMagnitude = new DoubleYoVariable("disturbanceMagnitude", registry);
   private final DoubleYoVariable disturbanceDuration = new DoubleYoVariable("disturbanceDuration", registry);

   private final double ballVelocityMagnitude;

   public ForcePerturbance(ForcePerturbable forcePerturbable, double magnitude, double duration, double ballVelocityMagnitudeForViz, YoVariableRegistry parentRegistry)
   {
      this.forcePerturbable = forcePerturbable;
      this.disturbanceMagnitude.set(magnitude);
      this.disturbanceDuration.set(duration);
      this.ballVelocityMagnitude = ballVelocityMagnitudeForViz;

      if (parentRegistry != null)
         parentRegistry.addChild(registry);
   }

   public void perturb(Vector3d direction)
   {
      Vector3d force = new Vector3d(direction);
      if (direction.lengthSquared() > 0.0)
      {
         force.normalize();
         force.scale(disturbanceMagnitude.getDoubleValue());
         forcePerturbable.setForcePerturbance(force, disturbanceDuration.getDoubleValue());
      }
   }

   public double getBallVelocityMagnitude()
   {
      return ballVelocityMagnitude;
   }

   public double getBallMass()
   {
      return impulse() / getBallVelocityMagnitude();
   }

   private double impulse()
   {
      return disturbanceMagnitude.getDoubleValue() * disturbanceDuration.getDoubleValue();
   }

   public void doEveryTick()
   {
      forcePerturbable.resetPerturbanceForceIfNecessary();
   }
   
   public String toString()
   {
      return name + ": Magnitude=" + disturbanceMagnitude.getDoubleValue() + ", Duration="+disturbanceDuration.getDoubleValue();
   }
}
