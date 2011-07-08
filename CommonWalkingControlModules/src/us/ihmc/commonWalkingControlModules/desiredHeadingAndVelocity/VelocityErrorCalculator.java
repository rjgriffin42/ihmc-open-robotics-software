package us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity;

import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

public class VelocityErrorCalculator
{
   private final DesiredVelocityControlModule desiredVelocityControlModule;
   private final ProcessedSensorsInterface processedSensors;
   
   public VelocityErrorCalculator(ProcessedSensorsInterface processedSensors, DesiredVelocityControlModule desiredVelocityControlModule)
   {
      this.processedSensors = processedSensors;
      this.desiredVelocityControlModule = desiredVelocityControlModule;
      
   }
   
   public FrameVector2d getVelocityErrorInFrame(ReferenceFrame referenceFrame, RobotSide legToTrustForCoMVelocity)
   {
      if (!referenceFrame.isZupFrame())
      {
         throw new RuntimeException("Must be a ZUp frame!");
      }

      FrameVector centerOfMassVelocity = processedSensors.getCenterOfMassVelocityInFrame(referenceFrame, legToTrustForCoMVelocity);
      FrameVector2d centerOfMassVelocity2d = centerOfMassVelocity.toFrameVector2d();

      FrameVector2d desiredCenterOfMassVelocity = desiredVelocityControlModule.getDesiredVelocity();
      desiredCenterOfMassVelocity = desiredCenterOfMassVelocity.changeFrameCopy(referenceFrame);

      FrameVector2d ret = new FrameVector2d(referenceFrame);
      ret.sub(desiredCenterOfMassVelocity, centerOfMassVelocity2d);

      return ret;
   }

}
