package us.ihmc.aware.controller.toolbox;

import us.ihmc.robotics.controllers.AxisAngleOrientationController;
import us.ihmc.robotics.controllers.YoAxisAngleOrientationGains;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class QuadrupedBodyOrientationController
{
   public static class Setpoints
   {
      private final FrameOrientation bodyOrientation = new FrameOrientation();
      private final FrameVector bodyAngularVelocity = new FrameVector();
      private final FrameVector comTorqueFeedforward = new FrameVector();

      public void initialize(QuadrupedTaskSpaceEstimator.Estimates estimates)
      {
         bodyOrientation.setIncludingFrame(estimates.getBodyOrientation());
         bodyAngularVelocity.setToZero();
         comTorqueFeedforward.setToZero();
      }

      public FrameOrientation getBodyOrientation()
      {
         return bodyOrientation;
      }

      public FrameVector getBodyAngularVelocity()
      {
         return bodyAngularVelocity;
      }

      public FrameVector getComTorqueFeedforward()
      {
         return comTorqueFeedforward;
      }
   }

   private final ReferenceFrame bodyFrame;
   private final AxisAngleOrientationController bodyOrientationController;
   private final YoAxisAngleOrientationGains bodyOrientationControllerGains;

   public QuadrupedBodyOrientationController(ReferenceFrame bodyFrame, double controlDT, YoVariableRegistry registry)
   {
      this.bodyFrame = bodyFrame;
      bodyOrientationController = new AxisAngleOrientationController("bodyOrientation", bodyFrame, controlDT, registry);
      bodyOrientationControllerGains = new YoAxisAngleOrientationGains("bodyOrientation", registry);
   }

   public YoAxisAngleOrientationGains getGains()
   {
      return bodyOrientationControllerGains;
   }

   public void reset()
   {
      bodyOrientationController.reset();
   }

   public void compute(FrameVector comTorqueCommand, Setpoints setpoints, QuadrupedTaskSpaceEstimator.Estimates estimates)
   {
      FrameOrientation bodyOrientationSetpoint = setpoints.getBodyOrientation();
      FrameVector bodyAngularVelocitySetpoint = setpoints.getBodyAngularVelocity();
      FrameVector bodyAngularVelocityEstimate = estimates.getBodyAngularVelocity();
      FrameVector comTorqueFeedforwardSetpoint = setpoints.getComTorqueFeedforward();

      // compute body torque
      comTorqueCommand.setToZero(bodyFrame);
      bodyOrientationSetpoint.changeFrame(bodyFrame);
      bodyAngularVelocitySetpoint.changeFrame(bodyFrame);
      bodyAngularVelocityEstimate.changeFrame(bodyFrame);
      comTorqueFeedforwardSetpoint.changeFrame(bodyFrame);
      bodyOrientationController.setGains(bodyOrientationControllerGains);
      bodyOrientationController.compute(comTorqueCommand, bodyOrientationSetpoint, bodyAngularVelocitySetpoint, bodyAngularVelocityEstimate, comTorqueFeedforwardSetpoint);
   }
}