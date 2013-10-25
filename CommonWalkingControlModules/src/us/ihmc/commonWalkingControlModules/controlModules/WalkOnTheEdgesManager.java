package us.ihmc.commonWalkingControlModules.controlModules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.trajectories.WalkOnTheEdgesProviders;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class WalkOnTheEdgesManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   
   // TODO it would be nice to use toe touchdown for side steps, but it requires too often an unreachable orientation of the foot resulting in unstable behaviors
   private static final boolean DO_TOE_TOUCHDOWN_ONLY_WHEN_STEPPING_DOWN = true;

   public enum SwitchToToeOffMethods
   {
      USE_ECMP, USE_ICP
   };

   public static final SwitchToToeOffMethods TOEOFF_TRIGGER_METHOD = SwitchToToeOffMethods.USE_ECMP;

   private final BooleanYoVariable stayOnToes = new BooleanYoVariable("stayOnToes", registry);

   private final BooleanYoVariable doToeOffIfPossible = new BooleanYoVariable("doToeOffIfPossible", registry);
   private final BooleanYoVariable doToeOff = new BooleanYoVariable("doToeOff", registry);

   private final BooleanYoVariable doToeTouchdownIfPossible = new BooleanYoVariable("doToeTouchdownIfPossible", registry);
   private final BooleanYoVariable doToeTouchdown = new BooleanYoVariable("doToeTouchdown", registry);

   private final BooleanYoVariable doHeelTouchdownIfPossible = new BooleanYoVariable("doHeelTouchdownIfPossible", registry);
   private final BooleanYoVariable doHeelTouchdown = new BooleanYoVariable("doHeelTouchdown", registry);

   private final DoubleYoVariable onToesTriangleArea = new DoubleYoVariable("onToesTriangleArea", registry);
   private final DoubleYoVariable onToesTriangleAreaLimit = new DoubleYoVariable("onToesTriangleAreaLimit", registry);
   private final BooleanYoVariable isOnToesTriangleLargeEnough = new BooleanYoVariable("isOnToesTriangleLargeEnough", registry);
   private FrameConvexPolygon2d onToesTriangle;

   private final BooleanYoVariable isDesiredICPOKForToeOff = new BooleanYoVariable("isDesiredICPOKForToeOff", registry);
   private final BooleanYoVariable isDesiredECMPOKForToeOff = new BooleanYoVariable("isDesiredECMPOKForToeOff", registry);

   private final DoubleYoVariable minStepLengthForToeOff = new DoubleYoVariable("minStepLengthForToeOff", registry);
   private final DoubleYoVariable minStepHeightForToeOff = new DoubleYoVariable("minStepHeightForToeOff", registry);
   private final DoubleYoVariable minStepLengthForToeTouchdown = new DoubleYoVariable("minStepLengthForToeTouchdown", registry);

   private final SideDependentList<? extends ContactablePlaneBody> feet;
   private final Map<ContactablePlaneBody, EndEffectorControlModule> footEndEffectorControlModules;

   private final DoubleYoVariable extraCoMMaxHeightWithToes = new DoubleYoVariable("extraCoMMaxHeightWithToes", registry);

   private final FramePoint tempLeadingFootPosition = new FramePoint();
   private final FramePoint tempTrailingFootPosition = new FramePoint();
   private final FramePoint tempLeadingFootPositionInWorld = new FramePoint();
   private final FramePoint tempTrailingFootPositionInWorld = new FramePoint();
   
   private final WalkingControllerParameters walkingControllerParameters;
   private final WalkOnTheEdgesProviders walkOnTheEdgesProviders;

   public WalkOnTheEdgesManager(WalkingControllerParameters walkingControllerParameters, WalkOnTheEdgesProviders walkOnTheEdgesProviders,
         SideDependentList<? extends ContactablePlaneBody> feet, Map<ContactablePlaneBody, EndEffectorControlModule> footEndEffectorControlModules,
         YoVariableRegistry parentRegistry)
   {
      this.stayOnToes.set(walkingControllerParameters.stayOnToes());
      this.doToeOffIfPossible.set(walkingControllerParameters.doToeOffIfPossible());
      this.doToeTouchdownIfPossible.set(walkingControllerParameters.doToeTouchdownIfPossible());
      this.doHeelTouchdownIfPossible.set(walkingControllerParameters.doHeelTouchdownIfPossible());
      
      this.walkingControllerParameters = walkingControllerParameters;
      this.walkOnTheEdgesProviders = walkOnTheEdgesProviders;
      
      this.feet = feet;
      this.footEndEffectorControlModules = footEndEffectorControlModules;

      onToesTriangleAreaLimit.set(0.01);

      extraCoMMaxHeightWithToes.set(0.07);

      minStepLengthForToeOff.set(0.40);
      minStepHeightForToeOff.set(0.10);

      minStepLengthForToeTouchdown.set(0.40);

      parentRegistry.addChild(registry);
   }

   public void updateToeOffStatusBasedOnECMP(RobotSide trailingLeg, FramePoint2d desiredECMP)
   {
      if (!doToeOffIfPossible.getBooleanValue() || stayOnToes.getBooleanValue() || TOEOFF_TRIGGER_METHOD != SwitchToToeOffMethods.USE_ECMP)
      {
         doToeOff.set(false);
         isDesiredECMPOKForToeOff.set(false);
         return;
      }

      ContactablePlaneBody trailingFoot = feet.get(trailingLeg);
      ContactablePlaneBody leadingFoot = feet.get(trailingLeg.getOppositeSide());
      FrameConvexPolygon2d OnToesSupportPolygon = getOnToesSupportPolygon(trailingFoot, leadingFoot);
      isDesiredECMPOKForToeOff.set(Math.abs(OnToesSupportPolygon.distance(desiredECMP)) < 0.06);
      //    isDesiredCMPOKForToeOff.set(OnToesSupportPolygon.isPointInside(desiredECMP));

      if (!isDesiredECMPOKForToeOff.getBooleanValue())
      {
         doToeOff.set(false);
         return;
      }

      isReadyToSwitchToToeOff(trailingLeg);
   }

   public void updateToeOffStatusBasedOnICP(RobotSide trailingLeg, FramePoint2d desiredICP, FramePoint2d finalDesiredICP)
   {
      if (!doToeOffIfPossible.getBooleanValue() || stayOnToes.getBooleanValue() || TOEOFF_TRIGGER_METHOD != SwitchToToeOffMethods.USE_ICP)
      {
         doToeOff.set(false);
         isDesiredICPOKForToeOff.set(false);
         return;
      }

      updateOnToesTriangle(finalDesiredICP, trailingLeg);

      isDesiredICPOKForToeOff.set(onToesTriangle.isPointInside(desiredICP) && isOnToesTriangleLargeEnough.getBooleanValue());

      if (!isDesiredICPOKForToeOff.getBooleanValue())
      {
         doToeOff.set(false);
         return;
      }

      isReadyToSwitchToToeOff(trailingLeg);
   }

   public void updateOnToesTriangle(FramePoint2d finalDesiredICP, RobotSide supportSide)
   {
      onToesTriangle = getOnToesTriangle(finalDesiredICP, feet.get(supportSide));
      onToesTriangleArea.set(onToesTriangle.getArea());
      isOnToesTriangleLargeEnough.set(onToesTriangleArea.getDoubleValue() > onToesTriangleAreaLimit.getDoubleValue());
   }

   public boolean isOnToesTriangleLargeEnough()
   {
      return isOnToesTriangleLargeEnough.getBooleanValue();
   }

   private void isReadyToSwitchToToeOff(RobotSide trailingLeg)
   {
      RobotSide leadingLeg = trailingLeg.getOppositeSide();
      ReferenceFrame frontFootFrame = feet.get(leadingLeg).getBodyFrame();

      if (!isFrontFootWellPositionedForToeOff(trailingLeg, frontFootFrame))
      {
         doToeOff.set(false);
         return;
      }

      EndEffectorControlModule trailingEndEffectorControlModule = footEndEffectorControlModules.get(feet.get(trailingLeg));
      doToeOff.set(Math.abs(trailingEndEffectorControlModule.getJacobianDeterminant()) < 0.06);
   }

   private boolean isFrontFootWellPositionedForToeOff(RobotSide trailingLeg, ReferenceFrame frontFootFrame)
   {
      ReferenceFrame trailingFootFrame = feet.get(trailingLeg).getBodyFrame();
      tempLeadingFootPosition.setToZero(frontFootFrame);
      tempTrailingFootPosition.setToZero(trailingFootFrame);
      tempLeadingFootPosition.changeFrame(trailingFootFrame);

      tempLeadingFootPositionInWorld.setToZero(frontFootFrame);
      tempTrailingFootPositionInWorld.setToZero(trailingFootFrame);
      tempLeadingFootPositionInWorld.changeFrame(worldFrame);
      tempTrailingFootPositionInWorld.changeFrame(worldFrame);

      double stepHeight = tempLeadingFootPositionInWorld.getZ() - tempTrailingFootPositionInWorld.getZ();

      boolean isNextStepHighEnough = stepHeight > minStepHeightForToeOff.getDoubleValue();
      if (isNextStepHighEnough)
         return true;

      boolean isNextStepTooLow = stepHeight < -0.10;
      if (isNextStepTooLow)
         return false;

      boolean isForwardOrSideStepping = tempLeadingFootPosition.getX() > -0.05;
      if (!isForwardOrSideStepping)
         return false;

      boolean isStepLongEnough = tempLeadingFootPosition.distance(tempTrailingFootPosition) > minStepLengthForToeOff.getDoubleValue();
      return isStepLongEnough;
   }

   @SuppressWarnings("unused")
   private boolean isFrontFootWellPositionedForToeTouchdown(RobotSide trailingLeg, ReferenceFrame frontFootFrame)
   {
      ReferenceFrame trailingFootFrame = feet.get(trailingLeg).getBodyFrame();
      tempLeadingFootPosition.setToZero(frontFootFrame);
      tempTrailingFootPosition.setToZero(trailingFootFrame);
      tempLeadingFootPosition.changeFrame(trailingFootFrame);

      tempLeadingFootPositionInWorld.setToZero(frontFootFrame);
      tempTrailingFootPositionInWorld.setToZero(trailingFootFrame);
      tempLeadingFootPositionInWorld.changeFrame(worldFrame);
      tempTrailingFootPositionInWorld.changeFrame(worldFrame);

      double stepHeight = tempLeadingFootPositionInWorld.getZ() - tempTrailingFootPositionInWorld.getZ();

      boolean isNextStepTooHigh = stepHeight > 0.05;
      if (isNextStepTooHigh)
         return false;

      boolean isNextStepLowEnough = stepHeight < -minStepHeightForToeOff.getDoubleValue();
      if (isNextStepLowEnough)
         return true;

      if (DO_TOE_TOUCHDOWN_ONLY_WHEN_STEPPING_DOWN)
         return false;
      
      boolean isBackardOrSideStepping = tempLeadingFootPosition.getX() < 0.05;
      if (!isBackardOrSideStepping)
         return false;

      boolean isStepLongEnough = tempLeadingFootPosition.distance(tempTrailingFootPosition) > minStepLengthForToeTouchdown.getDoubleValue();
      return isStepLongEnough;
   }

   private boolean isFrontFootWellPositionedForHeelTouchdown(RobotSide trailingLeg, ReferenceFrame frontFootFrame)
   {
      ReferenceFrame trailingFootFrame = feet.get(trailingLeg).getBodyFrame();
      tempLeadingFootPosition.setToZero(frontFootFrame);
      tempTrailingFootPosition.setToZero(trailingFootFrame);
      tempLeadingFootPosition.changeFrame(trailingFootFrame);

      tempLeadingFootPositionInWorld.setToZero(frontFootFrame);
      tempTrailingFootPositionInWorld.setToZero(trailingFootFrame);
      tempLeadingFootPositionInWorld.changeFrame(worldFrame);
      tempTrailingFootPositionInWorld.changeFrame(worldFrame);

      boolean isBackardOrSideStepping = tempLeadingFootPosition.getX() < 0.15;
      if (isBackardOrSideStepping)
         return false;

      boolean isStepLongEnough = tempLeadingFootPosition.distance(tempTrailingFootPosition) > minStepLengthForToeTouchdown.getDoubleValue();
      return isStepLongEnough;
   }

   public void updateEdgeTouchdownStatus(RobotSide supportLeg, Footstep nextFootstep)
   {
      RobotSide nextTrailingLeg = supportLeg;
      ReferenceFrame nextFrontFootFrame;

      if (nextFootstep != null)
         nextFrontFootFrame = nextFootstep.getPoseReferenceFrame();
      else
         nextFrontFootFrame = feet.get(nextTrailingLeg.getOppositeSide()).getBodyFrame();

      if (!doToeTouchdownIfPossible.getBooleanValue())
      {
         doToeTouchdown.set(false);
      }
      else
      {
         boolean frontFootWellPositionedForToeTouchdown = isFrontFootWellPositionedForToeTouchdown(nextTrailingLeg, nextFrontFootFrame);
         doToeTouchdown.set(frontFootWellPositionedForToeTouchdown);
         
         if (doToeTouchdown.getBooleanValue())
            walkOnTheEdgesProviders.setToeTouchdownInitialPitch();
      }
      
      if (!doHeelTouchdownIfPossible.getBooleanValue() || doToeTouchdown.getBooleanValue())
      {
         doHeelTouchdown.set(false);
         return;
      }
      
      boolean frontFootWellPositionedForHeelTouchdown = isFrontFootWellPositionedForHeelTouchdown(nextTrailingLeg, nextFrontFootFrame);
      doHeelTouchdown.set(frontFootWellPositionedForHeelTouchdown);
      
      if (doHeelTouchdown.getBooleanValue())
         walkOnTheEdgesProviders.setHeelTouchdownInitialPitch();
   }

   public Footstep createFootstepForEdgeTouchdown(Footstep footstepToModify)
   {
      if (!doToeTouchdown.getBooleanValue() && !doHeelTouchdown.getBooleanValue())
         return footstepToModify;

      FramePose oldPose = footstepToModify.getPoseCopy();

      FrameOrientation newOrientation = oldPose.getOrientationCopy();
      FramePoint newPosition = oldPose.getPostionCopy();
      double[] yawPitchRoll = newOrientation.getYawPitchRoll();
      yawPitchRoll[1] += walkOnTheEdgesProviders.getTouchdownInitialPitch();

      Vector3d ankleToEdge, edgeToAnkle;
      
      if (doToeTouchdown.getBooleanValue())
         ankleToEdge = new Vector3d(walkingControllerParameters.getFootForwardOffset(), 0.0, -walkingControllerParameters.getAnkleHeight());
      else
         ankleToEdge = new Vector3d(-walkingControllerParameters.getFootBackwardOffset(), 0.0, walkingControllerParameters.getAnkleHeight());
      
      edgeToAnkle = new Vector3d(ankleToEdge);
      edgeToAnkle.negate();
      
      Transform3D rotationByPitch = new Transform3D();
      rotationByPitch.rotY(walkOnTheEdgesProviders.getTouchdownInitialPitch());
      rotationByPitch.transform(edgeToAnkle);

      double newX = newPosition.getX() + ankleToEdge.x + edgeToAnkle.x;
      double newHeight = newPosition.getZ() + ankleToEdge.z + edgeToAnkle.z;

      newPosition.setX(newX);
      newPosition.setZ(newHeight);
      newOrientation.setYawPitchRoll(yawPitchRoll);
      FramePose newPose = new FramePose(newPosition, newOrientation);
      PoseReferenceFrame newReferenceFrame = new PoseReferenceFrame("newPoseFrame", newPose);

      return Footstep.copyChangePoseFrame(footstepToModify, newReferenceFrame);
   }

   public void updateTouchdownInitialAngularVelocity()
   {
      if (!doToeTouchdown.getBooleanValue() && !doHeelTouchdown.getBooleanValue())
         return;
      
      Vector3d edgeToAnkle;
      
      if (doToeTouchdown.getBooleanValue())
         edgeToAnkle = new Vector3d(-walkingControllerParameters.getFootForwardOffset(), 0.0, walkingControllerParameters.getAnkleHeight());
      else
         edgeToAnkle = new Vector3d(walkingControllerParameters.getFootBackwardOffset(), 0.0, walkingControllerParameters.getAnkleHeight());
      
      Transform3D rotationByPitch = new Transform3D();
      rotationByPitch.rotY(-walkOnTheEdgesProviders.getTouchdownInitialPitch());
      rotationByPitch.transform(edgeToAnkle);

      Vector3d orthonormalToHeelToAnkle = new Vector3d(edgeToAnkle);
      Transform3D perpendicularRotation = new Transform3D();
      perpendicularRotation.rotY(-Math.PI / 2.0);
      perpendicularRotation.transform(orthonormalToHeelToAnkle);
      orthonormalToHeelToAnkle.normalize();

      Vector3d linearVelocity = new Vector3d();
      walkOnTheEdgesProviders.getTouchdownDesiredVelocity(linearVelocity);
      double angularSpeed = -orthonormalToHeelToAnkle.dot(linearVelocity) / edgeToAnkle.length();

      walkOnTheEdgesProviders.setTouchdownInitialAngularVelocityProvider(angularSpeed);
   }

   public boolean willLandOnEdge()
   {
      if (!doToeTouchdownIfPossible.getBooleanValue() && !doHeelTouchdownIfPossible.getBooleanValue())
         return false;
      
      return doToeTouchdown.getBooleanValue() || doHeelTouchdown.getBooleanValue();
   }

   public boolean willLandOnToes()
   {
      if (!doToeTouchdownIfPossible.getBooleanValue())
         return false;

      return doToeTouchdown.getBooleanValue();
   }

   public boolean willLandOnHeel()
   {
      if (!doHeelTouchdownIfPossible.getBooleanValue())
         return false;

      return doHeelTouchdown.getBooleanValue();
   }

   public boolean willDoToeOff(TransferToAndNextFootstepsData transferToAndNextFootstepsData)
   {
      if (stayOnToes.getBooleanValue())
         return true;

      if (!doToeOffIfPossible.getBooleanValue())
         return false;

      RobotSide nextTrailingLeg = transferToAndNextFootstepsData.getTransferToSide().getOppositeSide();
      Footstep nextFootstep = transferToAndNextFootstepsData.getNextFootstep();
      ReferenceFrame nextFrontFootFrame;
      if (nextFootstep != null)
         nextFrontFootFrame = nextFootstep.getPoseReferenceFrame();
      else
         nextFrontFootFrame = feet.get(nextTrailingLeg.getOppositeSide()).getBodyFrame();

      boolean frontFootWellPositionedForToeOff = isFrontFootWellPositionedForToeOff(nextTrailingLeg, nextFrontFootFrame);

      return frontFootWellPositionedForToeOff;
   }

   public boolean stayOnToes()
   {
      return stayOnToes.getBooleanValue();
   }

   public boolean doToeOff()
   {
      return doToeOff.getBooleanValue();
   }

   public boolean doToeOffIfPossible()
   {
      return doToeOffIfPossible.getBooleanValue();
   }

   public double getExtraCoMMaxHeightWithToes()
   {
      return extraCoMMaxHeightWithToes.getDoubleValue();
   }

   public void reset()
   {
      isDesiredECMPOKForToeOff.set(false);
      isDesiredICPOKForToeOff.set(false);
      
      doToeOff.set(false);
      doToeTouchdown.set(false);
      doHeelTouchdown.set(false);
   }

   private FrameConvexPolygon2d getOnToesTriangle(FramePoint2d finalDesiredICP, ContactablePlaneBody supportFoot)
   {
      List<FramePoint> toePoints = getToePoints(supportFoot);
      Collection<FramePoint2d> points = new ArrayList<FramePoint2d>();
      for (FramePoint toePoint : toePoints)
      {
         toePoint.changeFrame(worldFrame);
         points.add(toePoint.toFramePoint2d());
      }

      points.add(finalDesiredICP);

      return new FrameConvexPolygon2d(points);
   }

   private List<FramePoint> getToePoints(ContactablePlaneBody supportFoot)
   {
      FrameVector forward = new FrameVector(supportFoot.getPlaneFrame(), 1.0, 0.0, 0.0);
      int nToePoints = 2;
      List<FramePoint> toePoints = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(supportFoot.getContactPoints(), forward, nToePoints);

      return toePoints;
   }

   private FrameConvexPolygon2d getOnToesSupportPolygon(ContactablePlaneBody trailingFoot, ContactablePlaneBody leadingFoot)
   {
      List<FramePoint> toePoints = getToePoints(trailingFoot);
      List<FramePoint> leadingFootPoints = leadingFoot.getContactPoints();

      List<FramePoint2d> allPoints = new ArrayList<FramePoint2d>();
      for (FramePoint framePoint : toePoints)
      {
         framePoint.changeFrame(worldFrame);
         allPoints.add(framePoint.toFramePoint2d());
      }

      for (FramePoint framePoint : leadingFootPoints)
      {
         framePoint.changeFrame(worldFrame);
         allPoints.add(framePoint.toFramePoint2d());
      }

      return new FrameConvexPolygon2d(allPoints);
   }

}
