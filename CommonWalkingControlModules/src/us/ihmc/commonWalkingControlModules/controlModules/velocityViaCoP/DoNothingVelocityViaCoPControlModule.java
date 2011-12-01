package us.ihmc.commonWalkingControlModules.controlModules.velocityViaCoP;

import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredCoPControlModule;
import us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait.SingleSupportCondition;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;

public class DoNothingVelocityViaCoPControlModule implements DesiredCoPControlModule
{
   private final CommonWalkingReferenceFrames referenceFrames;

   public DoNothingVelocityViaCoPControlModule(CommonWalkingReferenceFrames referenceFrames)
   {
      this.referenceFrames = referenceFrames;
   }

   public FramePoint2d computeDesiredCoPSingleSupport(RobotSide supportLeg, FrameVector2d desiredVelocity, SingleSupportCondition singleSupportCondition, double timeInState)
   {
      return new FramePoint2d(referenceFrames.getAnkleZUpFrame(supportLeg));
   }

   public FramePoint2d computeDesiredCoPDoubleSupport(RobotSide loadingLeg, FrameVector2d desiredVelocity)
   {
      return new FramePoint2d(referenceFrames.getMidFeetZUpFrame());
   }

}
