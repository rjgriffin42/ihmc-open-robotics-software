package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.robotics.controllers.AxisAngleOrientationController;
import us.ihmc.robotics.controllers.OrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.robotics.screwTheory.TwistCalculator;

public class RigidBodyOrientationControlModule
{
   private final AxisAngleOrientationController axisAngleOrientationController;

   private final RigidBody endEffector;
   private final TwistCalculator twistCalculator;
   private final Twist endEffectorTwist = new Twist();
   private final FrameVector currentAngularVelocity = new FrameVector(ReferenceFrame.getWorldFrame());

   public RigidBodyOrientationControlModule(String namePrefix, RigidBody endEffector, TwistCalculator twistCalculator, double dt, YoVariableRegistry parentRegistry)
   {
      this(namePrefix, endEffector, twistCalculator, dt, null, parentRegistry);
   }

   public RigidBodyOrientationControlModule(String namePrefix, RigidBody endEffector, TwistCalculator twistCalculator, double dt, YoOrientationPIDGainsInterface gains,
         YoVariableRegistry parentRegistry)
   {
      this.endEffector = endEffector;
      this.axisAngleOrientationController = new AxisAngleOrientationController(namePrefix, endEffector.getBodyFixedFrame(), dt, gains, parentRegistry);
      this.twistCalculator = twistCalculator;
   }

   public void reset()
   {
      axisAngleOrientationController.reset();
   }

   public void compute(FrameVector outputToPack, FrameOrientation desiredOrientation, FrameVector desiredAngularVelocity,
         FrameVector feedForwardAngularAcceleration, RigidBody base)
   {
      // using twists is a bit overkill; optimize if needed.
      twistCalculator.getRelativeTwist(endEffectorTwist, base, endEffector);
      currentAngularVelocity.setToZero(endEffectorTwist.getExpressedInFrame());
      endEffectorTwist.getAngularPart(currentAngularVelocity);

      desiredAngularVelocity.changeFrame(currentAngularVelocity.getReferenceFrame());

      feedForwardAngularAcceleration.changeFrame(endEffectorTwist.getExpressedInFrame());

      axisAngleOrientationController.compute(outputToPack, desiredOrientation, desiredAngularVelocity, currentAngularVelocity, feedForwardAngularAcceleration);
   }

   public void setGains(OrientationPIDGainsInterface gains)
   {
      axisAngleOrientationController.setGains(gains);
   }

   public void setProportionalGains(double proportionalGainX, double proportionalGainY, double proportionalGainZ)
   {
      axisAngleOrientationController.setProportionalGains(proportionalGainX, proportionalGainY, proportionalGainZ);
   }

   public void setDerivativeGains(double derivativeGainX, double derivativeGainY, double derivativeGainZ)
   {
      axisAngleOrientationController.setDerivativeGains(derivativeGainX, derivativeGainY, derivativeGainZ);
   }

   public void setMaxAccelerationAndJerk(double maxAcceleration, double maxJerk)
   {
      axisAngleOrientationController.setMaxAccelerationAndJerk(maxAcceleration, maxJerk);
   }

   public void setMaxVelocityError(double maxVelocityError)
   {
      axisAngleOrientationController.setMaxVelocityError(maxVelocityError);
   }

   public void setMaxError(double maxError)
   {
      axisAngleOrientationController.setMaxError(maxError);
   }

   public void getEndEffectorCurrentAngularVelocity(FrameVector angularVelocityToPack)
   {
      angularVelocityToPack.setIncludingFrame(currentAngularVelocity);
   }

   public RigidBody getEndEffector()
   {
      return endEffector;
   }
}
