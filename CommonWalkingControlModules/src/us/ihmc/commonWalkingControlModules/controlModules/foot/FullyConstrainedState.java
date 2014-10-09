package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.yoUtilities.controllers.YoSE3PIDGains;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.humanoidRobot.bipedSupportPolygons.ContactablePlaneBody;


public class FullyConstrainedState extends AbstractFootControlState
{
   private final BooleanYoVariable requestHoldPosition;
   private final FrameVector fullyConstrainedNormalContactVector;
   private final BooleanYoVariable doFancyOnToesControl;

   private final EnumYoVariable<ConstraintType> requestedState;

   private final YoSE3PIDGains gains;

   public FullyConstrainedState(RigidBodySpatialAccelerationControlModule accelerationControlModule, MomentumBasedController momentumBasedController,
         ContactablePlaneBody contactableBody, BooleanYoVariable requestHoldPosition, EnumYoVariable<ConstraintType> requestedState, int jacobianId,
         DoubleYoVariable nullspaceMultiplier, BooleanYoVariable jacobianDeterminantInRange, BooleanYoVariable doSingularityEscape,
         FrameVector fullyConstrainedNormalContactVector, BooleanYoVariable doFancyOnToesControl, YoSE3PIDGains gains, RobotSide robotSide, YoVariableRegistry registry)
   {
      super(ConstraintType.FULL, accelerationControlModule, momentumBasedController,
            contactableBody, jacobianId, nullspaceMultiplier, jacobianDeterminantInRange, doSingularityEscape, robotSide, registry);

      this.requestHoldPosition = requestHoldPosition;
      this.fullyConstrainedNormalContactVector = fullyConstrainedNormalContactVector;
      this.doFancyOnToesControl = doFancyOnToesControl;
      this.requestedState = requestedState;
      this.gains = gains;
   }

   public void doTransitionIntoAction()
   {
      momentumBasedController.setPlaneContactStateNormalContactVector(contactableBody, fullyConstrainedNormalContactVector);
   }

   private void setFullyConstrainedStateGains()
   {
      accelerationControlModule.setGains(gains);
   }

   public void doSpecificAction()
   {
      if (doFancyOnToesControl.getBooleanValue())
         determineCoPOnEdge();

      if (FootControlModule.USE_SUPPORT_FOOT_HOLD_POSITION_STATE)
      {
         if (isCoPOnEdge && doFancyOnToesControl.getBooleanValue())
            requestedState.set(ConstraintType.HOLD_POSITION);
         else if (requestHoldPosition != null && requestHoldPosition.getBooleanValue())
            requestedState.set(ConstraintType.HOLD_POSITION);
      }

      if (gains == null)
      {
         footAcceleration.setToZero(contactableBody.getFrameAfterParentJoint(), rootBody.getBodyFixedFrame(), contactableBody.getFrameAfterParentJoint());
      }
      else
      {
         setFullyConstrainedStateGains();

         desiredPosition.setToZero(contactableBody.getFrameAfterParentJoint());
         desiredPosition.changeFrame(worldFrame);

         desiredOrientation.setToZero(contactableBody.getFrameAfterParentJoint());
         desiredOrientation.changeFrame(worldFrame);

         desiredLinearVelocity.setToZero(worldFrame);
         desiredAngularVelocity.setToZero(worldFrame);

         desiredLinearAcceleration.setToZero(worldFrame);
         desiredAngularAcceleration.setToZero(worldFrame);

         accelerationControlModule.doPositionControl(desiredPosition, desiredOrientation, desiredLinearVelocity, desiredAngularVelocity,
               desiredLinearAcceleration, desiredAngularAcceleration, rootBody);
         accelerationControlModule.packAcceleration(footAcceleration);
      }

      setTaskspaceConstraint(footAcceleration);
   }

   @Override
   public void doTransitionOutOfAction()
   {

   }
}
