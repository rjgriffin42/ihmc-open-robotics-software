package us.ihmc.robotics.math.trajectories;

import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.trajectories.providers.DoubleProvider;
import us.ihmc.robotics.screwTheory.OneDoFJoint;

public class OneDoFJointQuinticTrajectoryGenerator implements OneDoFJointTrajectoryGenerator
{
   private final YoVariableRegistry registry;
   private final DoubleYoVariable finalPosition;
   private final DoubleYoVariable currentPosition;
   private final YoPolynomial polynomial;
   private final DoubleYoVariable trajectoryTime;
   private final DoubleProvider trajectoryTimeProvider;
   private final DoubleYoVariable currentTime;
   private final OneDoFJoint joint;

   public OneDoFJointQuinticTrajectoryGenerator(String namePrefix, OneDoFJoint joint, DoubleProvider trajectoryTimeProvider, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(namePrefix + getClass().getSimpleName());
      this.joint = joint;
      this.polynomial = new YoPolynomial(namePrefix + "Polynomial", 6, registry);
      this.trajectoryTime = new DoubleYoVariable(namePrefix + "TrajectoryTime", registry);
      this.currentTime = new DoubleYoVariable(namePrefix + "CurrentTime", registry);
      this.currentPosition = new DoubleYoVariable(namePrefix + "CurrentPosition", registry);
      this.finalPosition = new DoubleYoVariable(namePrefix + "FinalPosition", registry);
      this.trajectoryTimeProvider = trajectoryTimeProvider;
      parentRegistry.addChild(registry);
   }

   /**
    * Desired joint angles and velocities come from reading the joints, this method can override them those position and velocity values. 
    * @param currentDesiredPosition Sets the desired joint position.
    * @param currentDesiredVelocity Sets the desired joint velocity.
    */
   @Override
   public void initialize(double initialPosition, double initialVelocity)
   {
      currentTime.set(0.0);
      this.trajectoryTime.set(trajectoryTimeProvider.getValue());
      this.polynomial.setQuintic(0.0, trajectoryTime.getDoubleValue(), initialPosition, initialVelocity, 0.0, finalPosition.getDoubleValue(), 0.0, 0.0);
      currentPosition.set(initialPosition);
   }

   @Override
   public void initialize()
   {
      initialize(joint.getQ(), joint.getQd());
   }

   @Override
   public void compute(double time)
   {
      this.currentTime.set(time);
      time = MathTools.clipToMinMax(time, 0.0, trajectoryTime.getDoubleValue());
      polynomial.compute(time);
      if (isDone())
         currentPosition.set(finalPosition.getDoubleValue());
      else
         currentPosition.set(polynomial.getPosition());
   }

   @Override
   public boolean isDone()
   {    
      return currentTime.getDoubleValue() >= trajectoryTime.getDoubleValue();
   }

   @Override
   public double getValue()
   {
      return currentPosition.getDoubleValue();
   }

   @Override
   public double getVelocity()
   {
      if (isDone())
         return 0.0;
      else
         return polynomial.getVelocity();
   }

   @Override
   public double getAcceleration()
   {
      if (isDone())
         return 0.0;
      else
         return polynomial.getAcceleration();
   }

   public void setFinalPosition(double finalPosition)
   {
      this.finalPosition.set(finalPosition);
   }
}