package us.ihmc.commonWalkingControlModules.controllers.regularWalkingGait;

import us.ihmc.commonWalkingControlModules.configurations.BalanceOnOneLegConfiguration;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.DesiredPelvisOrientationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.BalanceSupportControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.FootOrientationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.KneeExtensionControlModule;
import us.ihmc.commonWalkingControlModules.couplingRegistry.CouplingRegistry;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredVelocityControlModule;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegTorques;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LowerBodyTorques;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.LegToTrustForVelocityWriteOnly;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.Orientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.Wrench;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class CommonStanceSubController implements StanceSubController
{
   private final CouplingRegistry couplingRegistry;
   private final CommonWalkingReferenceFrames referenceFrames;
   private final DesiredVelocityControlModule desiredVelocityControlModule;
   private final DesiredPelvisOrientationControlModule desiredPelvisOrientationControlModule;
   private final BalanceSupportControlModule balanceSupportControlModule;
   private final FootOrientationControlModule footOrientationControlModule;
   private final DesiredHeadingControlModule desiredHeadingControlModule;
   private final KneeExtensionControlModule kneeExtensionControlModule;

   private final YoVariableRegistry registry = new YoVariableRegistry("StanceSubController");

   private final DoubleYoVariable minDoubleSupportTime = new DoubleYoVariable("minDoubleSupportTime", "Time to stay in double support.", registry);

   private final DoubleYoVariable kVelocityDoubleSupportTransfer =
      new DoubleYoVariable("kVelocityDoubleSupportTransfer", "Gain from velocity error to amount of capture point motion to extend double support phase.",
                           registry);

   private final DoubleYoVariable toeOffFootPitch = new DoubleYoVariable("toeOffFootPitch",
                                                       "This is the desired foot pitch at the end of toe-off during stance", registry);
   private final DoubleYoVariable toeOffMoveDuration = new DoubleYoVariable("toeOffMoveDuration", "The duration of the toe-off move during stance", registry);
   private final DoubleYoVariable timeSpentInEarlyStance = new DoubleYoVariable("timeSpentInEarlyStance", registry);
   private final DoubleYoVariable timeSpentInLateStance = new DoubleYoVariable("timeSpentInLateStance", registry);
   private final DoubleYoVariable timeSpentInTerminalStance = new DoubleYoVariable("timeSpentInTerminalStance", registry);

   private final DoubleYoVariable timeSpentInLoadingPreSwingA = new DoubleYoVariable("timeSpentInLoadingPreSwingA", registry);
   private final DoubleYoVariable timeSpentInLoadingPreSwingB = new DoubleYoVariable("timeSpentInLoadingPreSwingB", registry);
   private final DoubleYoVariable doubleSupportDuration = new DoubleYoVariable("doubleSupportDuration", registry);

   private final DoubleYoVariable minDoubleSupportTimeBeforeWalking = new DoubleYoVariable("minDoubleSupportTimeBeforeWalking", registry);
   private final DoubleYoVariable xCaptureToTransfer = new DoubleYoVariable("xCaptureToTransfer", registry);
   private final DoubleYoVariable yCaptureToTransfer = new DoubleYoVariable("yCaptureToTransfer", registry);
   private final DoubleYoVariable minPercentageTowardsDesired = new DoubleYoVariable("minPercentageTowardsDesired", registry);

   private final LegToTrustForVelocityWriteOnly supportLegAndLegToTrustForVelocity;    // FIXME: update things
   
   private final BooleanYoVariable doPushRecovery = new BooleanYoVariable("doPushRecovery", registry);

   private final double footWidth;
   private boolean waitInLoadingPreswingB;


   public CommonStanceSubController(CouplingRegistry couplingRegistry, CommonWalkingReferenceFrames referenceFrames,
                                    DesiredHeadingControlModule desiredHeadingControlModule, DesiredVelocityControlModule desiredVelocityControlModule,
                                    DesiredPelvisOrientationControlModule desiredPelvisOrientationControlModule,
                                    BalanceSupportControlModule balanceSupportControlModule,
                                    FootOrientationControlModule footOrientationControlModule, KneeExtensionControlModule kneeExtensionControlModule,
                                    LegToTrustForVelocityWriteOnly supportLegAndLegToTrustForVelocity,
                                    YoVariableRegistry parentRegistry, double footWidth)
   {
      this.couplingRegistry = couplingRegistry;
      this.referenceFrames = referenceFrames;
      this.desiredVelocityControlModule = desiredVelocityControlModule;
      this.desiredPelvisOrientationControlModule = desiredPelvisOrientationControlModule;
      this.balanceSupportControlModule = balanceSupportControlModule;
      this.footOrientationControlModule = footOrientationControlModule;
      this.desiredHeadingControlModule = desiredHeadingControlModule;
      this.kneeExtensionControlModule = kneeExtensionControlModule;
      this.supportLegAndLegToTrustForVelocity = supportLegAndLegToTrustForVelocity;
      this.footWidth = footWidth;

      doubleSupportDuration.set(1.0);    // FIXME: This is a hack but allows to compute the first desired step
      couplingRegistry.setDoubleSupportDuration(doubleSupportDuration.getDoubleValue());

      parentRegistry.addChild(registry);
   }

   public boolean canWeStopNow()
   {
      return true;
   }

   public void doEarlyStance(LegTorques legTorquesToPackForStanceSide, double timeInState)
   {
      doSingleSupportControl(legTorquesToPackForStanceSide, SingleSupportCondition.EarlyStance, timeInState);

      // Here is where we want to add the torque for the kneeExtensionController
      kneeExtensionControlModule.doEarlyStanceKneeExtensionControl(legTorquesToPackForStanceSide);
      timeSpentInEarlyStance.set(timeInState);
   }

   public void doLateStance(LegTorques legTorquesToPackForStanceSide, double timeInState)
   {
      doSingleSupportControl(legTorquesToPackForStanceSide, SingleSupportCondition.LateStance, timeInState);
      footOrientationControlModule.addAdditionalTorqueForFootOrientationControl(legTorquesToPackForStanceSide, timeInState);
      kneeExtensionControlModule.doLateStanceKneeExtensionControl(legTorquesToPackForStanceSide);


      timeSpentInLateStance.set(timeInState);
   }

   public void doTerminalStance(LegTorques legTorquesToPackForStanceSide, double timeInState)
   {
      RobotSide supportLeg = legTorquesToPackForStanceSide.getRobotSide();

//    doSingleSupportControl(legTorquesToPackForStanceSide);

      doSingleSupportControl(legTorquesToPackForStanceSide, SingleSupportCondition.TerminalStance, timeInState);

      footOrientationControlModule.addAdditionalTorqueForFootOrientationControl(legTorquesToPackForStanceSide,
              timeInState + timeSpentInLateStance.getDoubleValue());

      kneeExtensionControlModule.breakKneeForDownhillSlopes(supportLeg.getOppositeSide());

      timeSpentInTerminalStance.set(timeInState);
   }


   public void doLoadingPreSwingA(LowerBodyTorques lowerBodyTorquesToPack, RobotSide loadingLeg, double timeInState)
   {
      doDoubleSupportControl(lowerBodyTorquesToPack, loadingLeg, true);

      if ((timeSpentInLateStance.getDoubleValue() + timeSpentInTerminalStance.getDoubleValue()) != 0.0)
         footOrientationControlModule.addAdditionalTorqueForFootOrientationControl(lowerBodyTorquesToPack.getLegTorques(loadingLeg.getOppositeSide()),
                 timeInState + timeSpentInLateStance.getDoubleValue() + timeSpentInTerminalStance.getDoubleValue());

      kneeExtensionControlModule.doLoadingControl(lowerBodyTorquesToPack.getLegTorques(loadingLeg));

      timeSpentInLoadingPreSwingA.set(timeInState);
   }

   public void doLoadingPreSwingB(LowerBodyTorques lowerBodyTorquesToPack, RobotSide loadingLeg, double timeInState)
   {
      doDoubleSupportControl(lowerBodyTorquesToPack, loadingLeg, true);
      kneeExtensionControlModule.doLoadingControl(lowerBodyTorquesToPack.getLegTorques(loadingLeg));

      timeSpentInLoadingPreSwingB.set(timeInState);
   }

   public void doLoadingPreSwingC(LegTorques legTorquesToPackForStanceSide, RobotSide loadingLeg, double timeInState)
   {
      doSingleSupportControl(legTorquesToPackForStanceSide, SingleSupportCondition.Loading, timeInState);
      kneeExtensionControlModule.doLoadingControl(legTorquesToPackForStanceSide);
   }

   public void doStartWalkingDoubleSupport(LowerBodyTorques lowerBodyTorquesToPack, RobotSide loadingLeg, double timeInState)
   {
      doDoubleSupportControl(lowerBodyTorquesToPack, loadingLeg, false);
   }

   public void doUnloadLegToTransferIntoWalking(LowerBodyTorques lowerBodyTorquesToPack, RobotSide supportLeg, double timeInState)
   {
      doDoubleSupportControl(lowerBodyTorquesToPack, supportLeg, true);
   }

   public void doLoadingForSingleLegBalance(LowerBodyTorques lowerBodyTorques, RobotSide upcomingSupportSide, double timeInCurrentState)
   {
      doDoubleSupportControl(lowerBodyTorques, upcomingSupportSide, false);
   }

   public void doSingleLegBalance(LegTorques legTorquesToPack, RobotSide supportLeg, double timeInCurrentState)
   {
      doSingleSupportControl(legTorquesToPack, SingleSupportCondition.StopWalking, timeInCurrentState);
   }

   public void doTransitionIntoEarlyStance(RobotSide stanceSide)
   {
      kneeExtensionControlModule.doTransitionIntoStance(stanceSide);
   }

   public void doTransitionIntoLateStance(RobotSide stanceSide)
   {
      ReferenceFrame desiredHeadingFrame = desiredHeadingControlModule.getDesiredHeadingFrame();
      Orientation finalOrientation = new Orientation(desiredHeadingFrame, 0.0, toeOffFootPitch.getDoubleValue(), 0.0);
      footOrientationControlModule.initializeFootOrientationMove(toeOffMoveDuration.getDoubleValue(), finalOrientation, stanceSide);

      timeSpentInLateStance.set(0.0);
   }

   public void doTransitionIntoTerminalStance(RobotSide stanceSide)
   {
      timeSpentInTerminalStance.set(0.0);
   }

   public void doTransitionIntoLoadingPreSwingA(RobotSide loadingLeg)
   {
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg, true);
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg.getOppositeSide(), false);
      supportLegAndLegToTrustForVelocity.setSupportLeg(null);
      supportLegAndLegToTrustForVelocity.setLegToUseForCOMOffset(loadingLeg);

      kneeExtensionControlModule.doTransitionIntoLoading(loadingLeg);

      // Reset the timers
      timeSpentInLoadingPreSwingA.set(0.0);
      timeSpentInLoadingPreSwingB.set(0.0);
      doubleSupportDuration.set(0.0);
   }

   public void doTransitionIntoLoadingPreSwingB(RobotSide loadingLeg)
   {
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg, true);
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg.getOppositeSide(), false);
   }

   public void doTransitionIntoLoadingPreSwingC(RobotSide loadingLeg)
   {
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg, true);
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg.getOppositeSide(), false);
      supportLegAndLegToTrustForVelocity.setSupportLeg(loadingLeg);
      supportLegAndLegToTrustForVelocity.setLegToUseForCOMOffset(loadingLeg);
   }

   public void doTransitionIntoStartStopWalkingDoubleSupport(RobotSide stanceSide)
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(robotSide, true);
      }
      supportLegAndLegToTrustForVelocity.setSupportLeg(null);
      supportLegAndLegToTrustForVelocity.setLegToUseForCOMOffset(null);

//    balanceSupportControlModule.setDesiredCoPOffset(new FramePoint2d(ReferenceFrame.getWorldFrame())); // didn't do anything...
   }

   public void doTransitionIntoUnloadLegToTransferIntoWalking(RobotSide stanceSide)
   {
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(stanceSide, true);
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(stanceSide.getOppositeSide(), false);
      supportLegAndLegToTrustForVelocity.setSupportLeg(null);
      supportLegAndLegToTrustForVelocity.setLegToUseForCOMOffset(stanceSide);

//    BipedSupportPolygons bipedSupportPolygons = couplingRegistry.getBipedSupportPolygons();
//
//    FrameConvexPolygon2d singleSupportFootPolygon = bipedSupportPolygons.getFootPolygonInAnkleZUp(stanceSide);    // processedSensors.getFootPolygons().get(stanceSide).getFrameConvexPolygon2dCopy();
//    FramePoint2d singleSupportCentroid = singleSupportFootPolygon.getCentroidCopy();
//
//    FrameConvexPolygon2d supportPolygon = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();
//    FramePoint2d doubleSupportCentroid = getCenterOfBoundingBoxOfPolygon(supportPolygon).changeFrameCopy(singleSupportCentroid.getReferenceFrame());
//
//    singleSupportCentroid.sub(doubleSupportCentroid);
//
//    balanceSupportControlModule.setDesiredCoPOffset(singleSupportCentroid); // didn't do anything...
   }


   public void doTransitionIntoLoadingForSingleLegBalance(RobotSide upcomingSupportSide)
   {
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(upcomingSupportSide, true);
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(upcomingSupportSide.getOppositeSide(), false);
      supportLegAndLegToTrustForVelocity.setSupportLeg(upcomingSupportSide);
      supportLegAndLegToTrustForVelocity.setLegToUseForCOMOffset(upcomingSupportSide);
   }

   public void doTransitionIntoSingleLegBalance(RobotSide supportLeg, BalanceOnOneLegConfiguration currentConfiguration)
   {
      FrameVector2d finalHeadingTarget = new FrameVector2d(referenceFrames.getAnkleZUpFrame(supportLeg), 1.0, 0.0);
      finalHeadingTarget.changeFrame(ReferenceFrame.getWorldFrame());
      desiredHeadingControlModule.setFinalHeadingTarget(finalHeadingTarget);
      desiredPelvisOrientationControlModule.setDesiredPelvisOrientation(new Orientation(desiredHeadingControlModule.getDesiredHeadingFrame(), currentConfiguration.getYawPitchRoll()));
   }

   public void doTransitionOutOfEarlyStance(RobotSide stanceSide)
   {
   }

   public void doTransitionOutOfLateStance(RobotSide stanceSide)
   {
   }

   public void doTransitionOutOfLoadingPreSwingA(RobotSide loadingLeg)
   {
   }

   public void doTransitionOutOfLoadingPreSwingB(RobotSide loadingLeg)
   {
      doubleSupportDuration.set(timeSpentInLoadingPreSwingA.getDoubleValue() + timeSpentInLoadingPreSwingB.getDoubleValue());
      couplingRegistry.setDoubleSupportDuration(doubleSupportDuration.getDoubleValue());
   }

   public void doTransitionOutOfLoadingPreSwingC(RobotSide loadingLeg)
   {
   }

   public void doTransitionOutOfStartStopWalkingDoubleSupport(RobotSide loadingLeg)
   {
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg, true);
      supportLegAndLegToTrustForVelocity.setLegToTrustForVelocity(loadingLeg.getOppositeSide(), false);
      supportLegAndLegToTrustForVelocity.setSupportLeg(loadingLeg);
      supportLegAndLegToTrustForVelocity.setLegToUseForCOMOffset(loadingLeg);
   }

   public void doTransitionOutOfTerminalStance(RobotSide stanceSide)
   {
   }

   public void doTransitionOutOfUnloadLegToTransferIntoWalking(RobotSide stanceSide)
   {
   }


   public void doTransitionOutOfLoadingForSingleLegBalance(RobotSide upcomingSupportSide)
   {
      // TODO Auto-generated method stub
   }

   public void doTransitionOutOfSingleLegBalance(RobotSide supportLeg)
   {
      desiredPelvisOrientationControlModule.setDesiredPelvisOrientation(null);
   }

   public boolean isReadyToStartStopWalkingDoubleSupport(RobotSide loadingLeg, double timeInState)
   {
      return (timeInState > minDoubleSupportTimeBeforeWalking.getDoubleValue());
   }

   public boolean isDoneUnloadLegToTransferIntoWalking(RobotSide loadingLeg, double timeInState)
   {
//      ReferenceFrame loadingLegZUpFrame = referenceFrames.getAnkleZUpFrame(loadingLeg);
//      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(loadingLegZUpFrame).toFramePoint2d();
//
//      boolean capturePointFarEnoughForward = capturePoint.getX() > xCaptureToTransfer.getDoubleValue();
//      boolean capturePointInside;
//      if (loadingLeg == RobotSide.LEFT)
//         capturePointInside = capturePoint.getY() > -yCaptureToTransfer.getDoubleValue();
//      else
//         capturePointInside = capturePoint.getY() < yCaptureToTransfer.getDoubleValue();
//
//      return (capturePointFarEnoughForward && capturePointInside);

      return isOverPercentageTowardDesired(loadingLeg, 0.9);
   }

   public boolean isDoneWithLoadingPreSwingA(RobotSide loadingLeg, double timeInState)
   {
      boolean inStateLongEnough = (timeInState > minDoubleSupportTime.getDoubleValue());
      if (!inStateLongEnough)
         return false;

      return isOverPercentageTowardDesired(loadingLeg, minPercentageTowardsDesired.getDoubleValue());
   }

   private boolean isOverPercentageTowardDesired(RobotSide loadingLeg, double minPercentageTowardsDesired)
   {
      ReferenceFrame loadingLegZUpFrame = referenceFrames.getAnkleZUpFrame(loadingLeg);
      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(loadingLegZUpFrame).toFramePoint2d();

      FramePoint2d unLoadingSweetSpot = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(loadingLeg.getOppositeSide());
      FramePoint2d loadingSweetSpot = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(loadingLeg);

      unLoadingSweetSpot.changeFrame(loadingLegZUpFrame);
      loadingSweetSpot.changeFrame(loadingLegZUpFrame);

      FrameLineSegment2d footToFoot = new FrameLineSegment2d(unLoadingSweetSpot, loadingSweetSpot);

//    FramePoint2d projectedCapturePoint = footToFoot.orthogonalProjectionCopy(capturePoint);
//    double distanceFromLineSegment = projectedCapturePoint.distance(capturePoint);
      double percentageAlongLineSegment = footToFoot.percentageAlongLineSegment(capturePoint);

      return percentageAlongLineSegment > minPercentageTowardsDesired;
   }

   public boolean isDoneWithLoadingPreSwingB(RobotSide loadingLeg, double timeInState)
   {
      if (waitInLoadingPreswingB)
      {
         boolean inStateLongEnough = timeInState > 0.1;

         return inStateLongEnough || isCapturePointOutsideBaseOfSupport();
      }
      else
         return true;
   }

   public boolean isDoneLoadingForSingleLegBalance(RobotSide upcomingSupportSide, double timeInCurrentState)
   {
      FramePoint2d sweetSpot = couplingRegistry.getBipedSupportPolygons().getSweetSpotCopy(upcomingSupportSide);
      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(sweetSpot.getReferenceFrame()).toFramePoint2d();

      double doneLoadingForSingleLegBalanceSafetyFactor = 4.0;
      double minDistanceToSweetSpot = footWidth / doneLoadingForSingleLegBalanceSafetyFactor;
      boolean isCapturePointCloseEnoughToSweetSpot = capturePoint.distance(sweetSpot) < minDistanceToSweetSpot;
      boolean inStateLongEnough = timeInCurrentState > 1.0;

      return isCapturePointCloseEnoughToSweetSpot && inStateLongEnough;
   }

   private boolean isCapturePointOutsideBaseOfSupport()
   {
      FrameConvexPolygon2d supportPolygon = couplingRegistry.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp();
      FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(supportPolygon.getReferenceFrame()).toFramePoint2d();
      boolean hasCapturePointLeftBaseOfSupport = !(supportPolygon.isPointInside(capturePoint));

      return hasCapturePointLeftBaseOfSupport;
   }

   private void doSingleSupportControl(LegTorques legTorquesToPackForStanceSide, SingleSupportCondition singleSupportCondition, double timeInState)
   {
      FrameVector2d desiredVelocity;
      
      if (singleSupportCondition == SingleSupportCondition.StopWalking)
      {
         desiredVelocity = new FrameVector2d(ReferenceFrame.getWorldFrame());
      }
      else
      {
         desiredVelocity = desiredVelocityControlModule.getDesiredVelocity();
      }

      Orientation desiredPelvisOrientation =
         desiredPelvisOrientationControlModule.getDesiredPelvisOrientationSingleSupport(legTorquesToPackForStanceSide.getRobotSide());
      Wrench upperBodyWrench = couplingRegistry.getUpperBodyWrench();
      balanceSupportControlModule.doSingleSupportBalance(legTorquesToPackForStanceSide, desiredVelocity, desiredPelvisOrientation, upperBodyWrench, singleSupportCondition, timeInState);
   }

   private void doDoubleSupportControl(LowerBodyTorques lowerBodyTorquesToPack, RobotSide loadingLeg, boolean walk)
   {
      FrameVector2d desiredVelocity = walk
                                      ? desiredVelocityControlModule.getDesiredVelocity()
                                      : new FrameVector2d(desiredVelocityControlModule.getDesiredVelocity().getReferenceFrame());
      Orientation desiredPelvisOrientation = desiredPelvisOrientationControlModule.getDesiredPelvisOrientationDoubleSupport();
      balanceSupportControlModule.doDoubleSupportBalance(lowerBodyTorquesToPack, loadingLeg, desiredVelocity, desiredPelvisOrientation);

//    balanceSupportControlModule.doDoubleSupportBalanceToeOff(lowerBodyTorquesToPack, loadingLeg, desiredVelocity, desiredPelvisOrientation);
   }

   public void doStopWalkingDoubleSupport(LowerBodyTorques lowerBodyTorquesToPack, RobotSide loadingLeg, double timeInState)
   {
      FrameVector zeroVelocity = new FrameVector(ReferenceFrame.getWorldFrame());
      zeroVelocity = zeroVelocity.changeFrameCopy(desiredHeadingControlModule.getDesiredHeadingFrame());

//    desiredVelocityControlModule.setDesiredVelocity(zeroVelocity);
      Orientation desiredPelvisOrientation = desiredPelvisOrientationControlModule.getDesiredPelvisOrientationDoubleSupport();
      balanceSupportControlModule.doDoubleSupportBalance(lowerBodyTorquesToPack, loadingLeg, zeroVelocity.toFrameVector2d(), desiredPelvisOrientation);
   }

// private static FramePoint2d getCenterOfBoundingBoxOfPolygon(FrameConvexPolygon2d supportPolygon)
// {
//    BoundingBox2d boundingBox = supportPolygon.getBoundingBox();
//    Point2d center = new Point2d();
//    boundingBox.getCenterPointCopy(center);
//    FramePoint2d doubleSupportCentroid = new FramePoint2d(ReferenceFrame.getWorldFrame(), center);
//
//    return doubleSupportCentroid;
// }

   public void setParametersForR2()
   {
      minDoubleSupportTime.set(0.05);
      minDoubleSupportTimeBeforeWalking.set(0.3);
      xCaptureToTransfer.set(0.0);
      yCaptureToTransfer.set(0.04);    // 0.0;
      kVelocityDoubleSupportTransfer.set(0.05);    // 0.1);
      toeOffFootPitch.set(0.1);    // 0.3);
      toeOffMoveDuration.set(0.05);
      waitInLoadingPreswingB = false;
      minPercentageTowardsDesired.set(0.9);
   }

   private void setParametersForM2V2()
   {
      minDoubleSupportTime.set(0.0);
      minDoubleSupportTimeBeforeWalking.set(0.3);
      xCaptureToTransfer.set(0.01);
      yCaptureToTransfer.set(0.04);    // 0.0;
      kVelocityDoubleSupportTransfer.set(0.0);    // 0.1);
      toeOffFootPitch.set(0.1);    // 0.3);
      toeOffMoveDuration.set(0.05);
      waitInLoadingPreswingB = true;
      minPercentageTowardsDesired.set(0.95);
   }
   
   public void setParametersForM2V2PushRecovery()
   {
      setParametersForM2V2();
      doPushRecovery.set(true);      
   }

   public void setParametersForM2V2Walking()
   {
      setParametersForM2V2();
      doPushRecovery.set(false);
   }
   
   public void setParametersForOptimalSwingController()
   {
      kneeExtensionControlModule.setupParametersForOptimalSwingController();
   }

   public void initialize()
   {      
   }

   public boolean needToTakeAStep(RobotSide supportLeg)
   {
      if (doPushRecovery.getBooleanValue())
      {
         double epsilon = 1e-2;

         FrameConvexPolygon2d supportPolygon = couplingRegistry.getBipedSupportPolygons().getSupportPolygonInMidFeetZUp();
         FramePoint2d capturePoint = couplingRegistry.getCapturePointInFrame(supportPolygon.getReferenceFrame()).toFramePoint2d();
         return supportPolygon.distance(capturePoint) > epsilon;  
      }
      else
         return false;
   }
}
