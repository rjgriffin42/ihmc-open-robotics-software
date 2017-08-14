package us.ihmc.quadrupedRobotics.controller.force.toolbox;

import us.ihmc.robotics.controllers.EuclideanPositionController;
import us.ihmc.robotics.controllers.YoEuclideanPositionGains;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint3D;
import us.ihmc.robotics.geometry.FrameVector3D;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class QuadrupedComPositionController
{
   public static class Setpoints
   {
      private final FramePoint3D comPosition = new FramePoint3D();
      private final FrameVector3D comVelocity = new FrameVector3D();
      private final FrameVector3D comForceFeedforward = new FrameVector3D();

      public void initialize(QuadrupedTaskSpaceEstimator.Estimates estimates)
      {
         comPosition.setIncludingFrame(estimates.getComPosition());
         comPosition.changeFrame(ReferenceFrame.getWorldFrame());
         comVelocity.setToZero();
         comForceFeedforward.setToZero();
      }

      public FramePoint3D getComPosition()
      {
         return comPosition;
      }

      public FrameVector3D getComVelocity()
      {
         return comVelocity;
      }

      public FrameVector3D getComForceFeedforward()
      {
         return comForceFeedforward;
      }
   }

   private final ReferenceFrame comZUpFrame;
   private final EuclideanPositionController comPositionController;
   private final YoEuclideanPositionGains comPositionControllerGains;
   private final YoFramePoint yoComPositionSetpoint;
   private final YoFrameVector yoComVelocitySetpoint;
   private final YoFrameVector yoComForceFeedforwardSetpoint;

   public QuadrupedComPositionController(ReferenceFrame comZUpFrame, double controlDT, YoVariableRegistry registry)
   {
      this.comZUpFrame = comZUpFrame;
      comPositionController = new EuclideanPositionController("comPosition", comZUpFrame, controlDT, registry);
      comPositionControllerGains = new YoEuclideanPositionGains("comPosition", registry);
      yoComPositionSetpoint = new YoFramePoint("comPositionSetpoint", ReferenceFrame.getWorldFrame(), registry);
      yoComVelocitySetpoint = new YoFrameVector("comVelocitySetpoint", ReferenceFrame.getWorldFrame(), registry);
      yoComForceFeedforwardSetpoint = new YoFrameVector("comForceFeedforwardSetpoint", ReferenceFrame.getWorldFrame(), registry);
   }

   public ReferenceFrame getReferenceFrame()
   {
      return comZUpFrame;
   }

   public YoEuclideanPositionGains getGains()
   {
      return comPositionControllerGains;
   }

   public void reset()
   {
      comPositionController.reset();
      comPositionController.resetIntegrator();
   }

   public void compute(FrameVector3D comForceCommand, Setpoints setpoints, QuadrupedTaskSpaceEstimator.Estimates estimates)
   {
      FramePoint3D comPositionSetpoint = setpoints.getComPosition();
      FrameVector3D comVelocitySetpoint = setpoints.getComVelocity();
      FrameVector3D comVelocityEstimate = estimates.getComVelocity();
      FrameVector3D comForceFeedforwardSetpoint = setpoints.getComForceFeedforward();

      ReferenceFrame comPositionSetpointFrame = comPositionSetpoint.getReferenceFrame();
      ReferenceFrame comVelocitySetpointFrame = comVelocitySetpoint.getReferenceFrame();
      ReferenceFrame comVelocityEstimateFrame = comVelocityEstimate.getReferenceFrame();
      ReferenceFrame comForceFeedforwardSetpointFrame = comForceFeedforwardSetpoint.getReferenceFrame();

      // compute com force
      comForceCommand.setToZero(comZUpFrame);
      comPositionSetpoint.changeFrame(comZUpFrame);
      comVelocitySetpoint.changeFrame(comZUpFrame);
      comVelocityEstimate.changeFrame(comZUpFrame);
      comForceFeedforwardSetpoint.changeFrame(comZUpFrame);
      comPositionController.setGains(comPositionControllerGains);
      comPositionController.compute(comForceCommand, comPositionSetpoint, comVelocitySetpoint, comVelocityEstimate, comForceFeedforwardSetpoint);

      // update log variables
      yoComPositionSetpoint.setAndMatchFrame(comPositionSetpoint);
      yoComVelocitySetpoint.setAndMatchFrame(comVelocitySetpoint);
      yoComForceFeedforwardSetpoint.setAndMatchFrame(comForceFeedforwardSetpoint);

      comPositionSetpoint.changeFrame(comPositionSetpointFrame);
      comVelocitySetpoint.changeFrame(comVelocitySetpointFrame);
      comVelocityEstimate.changeFrame(comVelocityEstimateFrame);
      comForceFeedforwardSetpoint.changeFrame(comForceFeedforwardSetpointFrame);
   }
}
