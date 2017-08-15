package us.ihmc.quadrupedRobotics.controller.force.toolbox;

import us.ihmc.robotics.controllers.AxisAngleOrientationController;
import us.ihmc.robotics.controllers.pidGains.GainCoupling;
import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
import us.ihmc.robotics.controllers.pidGains.implementations.DefaultYoPID3DGains;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

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
         bodyOrientation.changeFrame(ReferenceFrame.getWorldFrame());
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
   private final DefaultYoPID3DGains bodyOrientationControllerGains;
   private final YoFrameOrientation yoBodyOrientationSetpoint;
   private final YoFrameVector yoBodyAngularVelocitySetpoint;
   private final YoFrameVector yoComTorqueFeedforwardSetpoint;

   public QuadrupedBodyOrientationController(ReferenceFrame bodyFrame, double controlDT, YoVariableRegistry registry)
   {
      this.bodyFrame = bodyFrame;
      bodyOrientationController = new AxisAngleOrientationController("bodyOrientation", bodyFrame, controlDT, registry);
      bodyOrientationControllerGains = new DefaultYoPID3DGains("bodyOrientation", GainCoupling.NONE, true, registry);
      yoBodyOrientationSetpoint = new YoFrameOrientation("bodyOrientationSetpoint", ReferenceFrame.getWorldFrame(), registry);
      yoBodyAngularVelocitySetpoint = new YoFrameVector("bodyAngularVelocitySetpoint", ReferenceFrame.getWorldFrame(), registry);
      yoComTorqueFeedforwardSetpoint = new YoFrameVector("comTorqueFeedforwardSetpoint", ReferenceFrame.getWorldFrame(), registry);
   }

   public ReferenceFrame getReferenceFrame()
   {
      return bodyFrame;
   }

   public YoPID3DGains getGains()
   {
      return bodyOrientationControllerGains;
   }

   public void reset()
   {
      bodyOrientationController.reset();
      bodyOrientationController.resetIntegrator();
   }

   public void compute(FrameVector comTorqueCommand, Setpoints setpoints, QuadrupedTaskSpaceEstimator.Estimates estimates)
   {
      FrameOrientation bodyOrientationSetpoint = setpoints.getBodyOrientation();
      FrameVector bodyAngularVelocitySetpoint = setpoints.getBodyAngularVelocity();
      FrameVector bodyAngularVelocityEstimate = estimates.getBodyAngularVelocity();
      FrameVector comTorqueFeedforwardSetpoint = setpoints.getComTorqueFeedforward();

      ReferenceFrame bodyOrientationSetpointFrame = bodyOrientationSetpoint.getReferenceFrame();
      ReferenceFrame bodyAngularVelocitySetpointFrame = bodyAngularVelocitySetpoint.getReferenceFrame();
      ReferenceFrame bodyAngularVelocityEstimateFrame = bodyAngularVelocityEstimate.getReferenceFrame();
      ReferenceFrame comTorqueFeedforwardSetpointFrame = comTorqueFeedforwardSetpoint.getReferenceFrame();

      // compute body torque
      comTorqueCommand.setToZero(bodyFrame);
      bodyOrientationSetpoint.changeFrame(bodyFrame);
      bodyAngularVelocitySetpoint.changeFrame(bodyFrame);
      bodyAngularVelocityEstimate.changeFrame(bodyFrame);
      comTorqueFeedforwardSetpoint.changeFrame(bodyFrame);
      bodyOrientationController.setGains(bodyOrientationControllerGains);
      bodyOrientationController
            .compute(comTorqueCommand, bodyOrientationSetpoint, bodyAngularVelocitySetpoint, bodyAngularVelocityEstimate, comTorqueFeedforwardSetpoint);

      // update log variables
      yoBodyOrientationSetpoint.setAndMatchFrame(bodyOrientationSetpoint);
      yoBodyAngularVelocitySetpoint.setAndMatchFrame(bodyAngularVelocitySetpoint);
      yoComTorqueFeedforwardSetpoint.setAndMatchFrame(comTorqueFeedforwardSetpoint);

      bodyOrientationSetpoint.changeFrame(bodyOrientationSetpointFrame);
      bodyAngularVelocitySetpoint.changeFrame(bodyAngularVelocitySetpointFrame);
      bodyAngularVelocityEstimate.changeFrame(bodyAngularVelocityEstimateFrame);
      comTorqueFeedforwardSetpoint.changeFrame(comTorqueFeedforwardSetpointFrame);
   }
}