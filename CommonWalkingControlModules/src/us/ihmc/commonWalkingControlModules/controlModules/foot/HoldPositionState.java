package us.ihmc.commonWalkingControlModules.controlModules.foot;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoPlaneContactState;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controllerCore.command.SolverWeightLevels;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.sensors.footSwitch.FootSwitchInterface;
import us.ihmc.robotics.controllers.YoSE3PIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FrameLineSegment2d;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFrameVector;

public class HoldPositionState extends AbstractFootControlState
{
   private final SpatialFeedbackControlCommand spatialFeedbackControlCommand = new SpatialFeedbackControlCommand();

   private static final double EPSILON = 0.010;

   private final FrameVector holdPositionNormalContactVector = new FrameVector();
   private final FrameVector fullyConstrainedNormalContactVector;

   private final FramePoint2d cop = new FramePoint2d();
   private final FramePoint2d desiredCoP = new FramePoint2d();
   private final PartialFootholdControlModule partialFootholdControlModule;

   private final FootSwitchInterface footSwitch;
   private final FrameConvexPolygon2d footPolygon = new FrameConvexPolygon2d();
   private final FrameLineSegment2d closestEdgeToCoP = new FrameLineSegment2d();
   private final FrameVector2d edgeVector2d = new FrameVector2d();
   private final FrameVector edgeVector = new FrameVector();
   private final FrameOrientation desiredOrientationCopy = new FrameOrientation();
   private final AxisAngle4d desiredAxisAngle = new AxisAngle4d();
   private final Vector3d desiredRotationVector = new Vector3d();

   private final BooleanYoVariable doSmartHoldPosition;
   private final YoFrameOrientation desiredHoldOrientation;

   private final YoSE3PIDGainsInterface gains;
   private final YoFrameVector yoAngularWeight;
   private final YoFrameVector yoLinearWeight;

   private final Vector3d tempAngularWeightVector = new Vector3d();
   private final Vector3d tempLinearWeightVector = new Vector3d();

   public HoldPositionState(FootControlHelper footControlHelper, YoSE3PIDGainsInterface gains, YoVariableRegistry registry)
   {
      super(ConstraintType.HOLD_POSITION, footControlHelper, registry);
      this.gains = gains;

      fullyConstrainedNormalContactVector = footControlHelper.getFullyConstrainedNormalContactVector();
      partialFootholdControlModule = footControlHelper.getPartialFootholdControlModule();
      footSwitch = momentumBasedController.getFootSwitches().get(robotSide);
      footPolygon.setIncludingFrameAndUpdate(footControlHelper.getContactableFoot().getContactPoints2d());
      String namePrefix = footControlHelper.getContactableFoot().getName();
      desiredHoldOrientation = new YoFrameOrientation(namePrefix + "DesiredHoldOrientation", worldFrame, registry);
      doSmartHoldPosition = new BooleanYoVariable(namePrefix + "DoSmartHoldPosition", registry);

      yoAngularWeight = new YoFrameVector(namePrefix + "HoldAngularWeight", null, registry);
      yoLinearWeight = new YoFrameVector(namePrefix + "HoldLinearWeight", null, registry);
      yoAngularWeight.set(1.0, 1.0, 1.0);
      yoAngularWeight.scale(SolverWeightLevels.FOOT_SUPPORT_WEIGHT);
      yoLinearWeight.set(1.0, 1.0, 1.0);
      yoLinearWeight.scale(SolverWeightLevels.FOOT_SUPPORT_WEIGHT);

      doSmartHoldPosition.set(true);
      spatialFeedbackControlCommand.set(rootBody, contactableFoot.getRigidBody());
      FramePose anklePoseInFoot = new FramePose(contactableFoot.getFrameAfterParentJoint());
      anklePoseInFoot.changeFrame(contactableFoot.getRigidBody().getBodyFixedFrame());
      spatialFeedbackControlCommand.setControlFrameFixedInEndEffector(anklePoseInFoot);
   }

   public void setWeight(double weight)
   {
      yoAngularWeight.set(1.0, 1.0, 1.0);
      yoAngularWeight.scale(weight);
      yoLinearWeight.set(1.0, 1.0, 1.0);
      yoLinearWeight.scale(weight);
   }

   public void setWeights(Vector3d angular, Vector3d linear)
   {
      yoAngularWeight.set(angular);
      yoLinearWeight.set(linear);
   }

   @Override
   public void doTransitionIntoAction()
   {
      super.doTransitionIntoAction();
      // Remember the previous contact normal, in case the foot leaves the ground and rotates
      holdPositionNormalContactVector.setIncludingFrame(fullyConstrainedNormalContactVector);
      holdPositionNormalContactVector.changeFrame(worldFrame);
      momentumBasedController.setPlaneContactStateNormalContactVector(contactableFoot, holdPositionNormalContactVector);

      desiredPosition.setToZero(contactableFoot.getFrameAfterParentJoint());
      desiredPosition.changeFrame(worldFrame);

      desiredOrientation.setToZero(contactableFoot.getFrameAfterParentJoint());
      desiredOrientation.changeFrame(worldFrame);
      desiredHoldOrientation.set(desiredOrientation);

      desiredLinearVelocity.setToZero(worldFrame);
      desiredAngularVelocity.setToZero(worldFrame);

      desiredLinearAcceleration.setToZero(worldFrame);
      desiredAngularAcceleration.setToZero(worldFrame);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      super.doTransitionOutOfAction();
   }

   @Override
   public void doSpecificAction()
   {
      footSwitch.computeAndPackCoP(cop);
      correctDesiredOrientationForSmartHoldPosition();
      desiredHoldOrientation.getFrameOrientationIncludingFrame(desiredOrientation);
      momentumBasedController.getDesiredCenterOfPressure(contactableFoot, desiredCoP);
      partialFootholdControlModule.compute(desiredCoP, cop);
      YoPlaneContactState contactState = momentumBasedController.getContactState(contactableFoot);
      partialFootholdControlModule.applyShrunkPolygon(contactState);

      spatialFeedbackControlCommand.set(desiredPosition, desiredLinearVelocity, desiredLinearAcceleration);
      spatialFeedbackControlCommand.set(desiredOrientation, desiredAngularVelocity, desiredAngularAcceleration);
      spatialFeedbackControlCommand.setGains(gains);
      yoAngularWeight.get(tempAngularWeightVector);
      yoLinearWeight.get(tempLinearWeightVector);
      spatialFeedbackControlCommand.setWeightsForSolver(tempAngularWeightVector, tempLinearWeightVector);
   }

   /**
    * Correct the desired orientation such that if the foot landed on the edge, the foot is still able to rotate towards the ground and eventually be in full support.
    */
   private void correctDesiredOrientationForSmartHoldPosition()
   {
      if (!doSmartHoldPosition.getBooleanValue())
         return;

      if (cop.containsNaN())
         return;

      boolean isCoPOnEdge = !footPolygon.isPointInside(cop, -EPSILON);

      if (!isCoPOnEdge)
         return;

      footPolygon.getClosestEdge(closestEdgeToCoP, cop);
      closestEdgeToCoP.getFrameVector(edgeVector2d);
      edgeVector.setXYIncludingFrame(edgeVector2d);
      edgeVector.normalize();
      desiredOrientationCopy.setIncludingFrame(desiredOrientation);
      desiredOrientationCopy.changeFrame(footPolygon.getReferenceFrame());
      desiredOrientationCopy.getAxisAngle(desiredAxisAngle);
      desiredRotationVector.set(desiredAxisAngle.getX(), desiredAxisAngle.getY(), desiredAxisAngle.getZ());
      desiredRotationVector.scale(desiredAxisAngle.getAngle());

      boolean holdRotationAroundEdge = true;
      double rotationOnEdge = edgeVector.dot(desiredRotationVector);

      if (closestEdgeToCoP.isPointOnLeftSideOfLineSegment(footPolygon.getCentroid()))
      {
         if (rotationOnEdge > 0.0)
            holdRotationAroundEdge = false;
      }
      else
      {
         if (rotationOnEdge < 0.0)
            holdRotationAroundEdge = false;
      }

      if (holdRotationAroundEdge)
         return;

      edgeVector.scale(rotationOnEdge);
      desiredRotationVector.sub(edgeVector.getVector());
      double angle = desiredRotationVector.length();
      desiredRotationVector.scale(1.0 / angle);
      desiredAxisAngle.set(desiredRotationVector, angle);
      desiredOrientationCopy.set(desiredAxisAngle);
      desiredOrientationCopy.changeFrame(worldFrame);
      desiredOrientation.set(desiredOrientationCopy);
   }

   @Override
   public SpatialAccelerationCommand getInverseDynamicsCommand()
   {
      return null;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      return spatialFeedbackControlCommand;
   }
}
