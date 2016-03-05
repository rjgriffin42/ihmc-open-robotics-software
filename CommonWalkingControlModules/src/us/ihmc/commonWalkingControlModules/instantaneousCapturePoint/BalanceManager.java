package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import us.ihmc.SdfLoader.models.FullHumanoidRobotModel;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.captureRegion.PushRecoveryControlModule;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.PelvisICPBasedTranslationManager;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableGoHomeMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiablePelvisTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.controllerAPI.input.command.ModifiableStopAllTrajectoryMessage;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointCalculator;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.solver.InverseDynamicsCommand;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.packets.walking.CapturabilityBasedStatus;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.controllers.YoPDGains;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.ConvexPolygonShrinker;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFramePoint2d;
import us.ihmc.robotics.math.frames.YoFrameVector2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.TotalMassCalculator;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicPosition.GraphicType;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

public class BalanceManager
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final BipedSupportPolygons bipedSupportPolygons;
   private final ICPPlannerWithTimeFreezer icpPlanner;
   private final ICPBasedLinearMomentumRateOfChangeControlModule icpBasedLinearMomentumRateOfChangeControlModule;
   private final PelvisICPBasedTranslationManager pelvisICPBasedTranslationManager;
   private final PushRecoveryControlModule pushRecoveryControlModule;
   private final MomentumBasedController momentumBasedController;

   private final YoFramePoint2d finalDesiredICPInWorld = new YoFramePoint2d("finalDesiredICPInWorld", worldFrame, registry);

   private final YoFramePoint2d desiredECMP = new YoFramePoint2d("desiredECMP", worldFrame, registry);
   private final YoFramePoint ecmpViz = new YoFramePoint("ecmpViz", worldFrame, registry);

   private final YoFramePoint2d yoDesiredCapturePoint = new YoFramePoint2d("desiredICP", worldFrame, registry);
   private final YoFrameVector2d yoDesiredICPVelocity = new YoFrameVector2d("desiredICPVelocity", worldFrame, registry);
   private final YoFramePoint yoCapturePoint = new YoFramePoint("capturePoint", worldFrame, registry);
   private final DoubleYoVariable omega0 = new DoubleYoVariable("omega0", registry);

   private final DoubleYoVariable yoTime;

   private final ReferenceFrame centerOfMassFrame;

   private final FramePoint centerOfMassPosition = new FramePoint();
   private final FramePoint2d centerOfMassPosition2d = new FramePoint2d();
   private final FrameVector centerOfMassVelocity = new FrameVector(worldFrame);
   private final FrameVector2d centerOfMassVelocity2d = new FrameVector2d();

   private final FramePoint2d capturePoint2d = new FramePoint2d();
   private final FramePoint2d desiredCapturePoint2d = new FramePoint2d();
   private final FrameVector2d desiredCapturePointVelocity2d = new FrameVector2d();
   private final FramePoint2d finalDesiredCapturePoint2d = new FramePoint2d();

   private final ConvexPolygonShrinker convexPolygonShrinker = new ConvexPolygonShrinker();
   private final FrameConvexPolygon2d shrunkSupportPolygon = new FrameConvexPolygon2d();

   private final CapturabilityBasedStatus capturabilityBasedStatus = new CapturabilityBasedStatus();

   public BalanceManager(MomentumBasedController momentumBasedController, WalkingControllerParameters walkingControllerParameters,
         CapturePointPlannerParameters capturePointPlannerParameters, YoVariableRegistry parentRegistry)
   {
      CommonHumanoidReferenceFrames referenceFrames = momentumBasedController.getReferenceFrames();
      FullHumanoidRobotModel fullRobotModel = momentumBasedController.getFullRobotModel();

      SideDependentList<ReferenceFrame> ankleZUpFrames = referenceFrames.getAnkleZUpReferenceFrames();
      SideDependentList<ReferenceFrame> soleZUpFrames = referenceFrames.getSoleZUpFrames();
      ReferenceFrame midFeetZUpFrame = referenceFrames.getMidFeetZUpFrame();
      YoGraphicsListRegistry yoGraphicsListRegistry = momentumBasedController.getDynamicGraphicObjectsListRegistry();
      SideDependentList<? extends ContactablePlaneBody> contactableFeet = momentumBasedController.getContactableFeet();
      double omega0 = walkingControllerParameters.getOmega0();
      ICPControlGains icpControlGains = walkingControllerParameters.getICPControlGains();
      double controlDT = momentumBasedController.getControlDT();
      double gravityZ = momentumBasedController.getGravityZ();
      double totalMass = TotalMassCalculator.computeSubTreeMass(fullRobotModel.getElevator());
      double minimumSwingTimeForDisturbanceRecovery = walkingControllerParameters.getMinimumSwingTimeForDisturbanceRecovery();

      this.momentumBasedController = momentumBasedController;
      yoTime = momentumBasedController.getYoTime();

      centerOfMassFrame = referenceFrames.getCenterOfMassFrame();
      this.omega0.set(omega0);

      bipedSupportPolygons = new BipedSupportPolygons(ankleZUpFrames, midFeetZUpFrame, soleZUpFrames, registry, yoGraphicsListRegistry);

      icpBasedLinearMomentumRateOfChangeControlModule = new ICPBasedLinearMomentumRateOfChangeControlModule(referenceFrames, bipedSupportPolygons, controlDT,
            totalMass, gravityZ, icpControlGains, registry, yoGraphicsListRegistry);

      icpPlanner = new ICPPlannerWithTimeFreezer(bipedSupportPolygons, contactableFeet, capturePointPlannerParameters, registry, yoGraphicsListRegistry);
      icpPlanner.setMinimumSingleSupportTimeForDisturbanceRecovery(minimumSwingTimeForDisturbanceRecovery);
      icpPlanner.setOmega0(omega0);
      icpPlanner.setSingleSupportTime(walkingControllerParameters.getDefaultSwingTime());
      icpPlanner.setDoubleSupportTime(walkingControllerParameters.getDefaultTransferTime());
      YoPDGains pelvisXYControlGains = walkingControllerParameters.createPelvisICPBasedXYControlGains(registry);
      pelvisICPBasedTranslationManager = new PelvisICPBasedTranslationManager(momentumBasedController, bipedSupportPolygons, pelvisXYControlGains, registry);

      pushRecoveryControlModule = new PushRecoveryControlModule(bipedSupportPolygons, momentumBasedController, walkingControllerParameters, registry);

      YoGraphicPosition dynamicGraphicPositionECMP = new YoGraphicPosition("ecmpviz", ecmpViz, 0.002, YoAppearance.BlueViolet());
      yoGraphicsListRegistry.registerYoGraphic("ecmpviz", dynamicGraphicPositionECMP);
      yoGraphicsListRegistry.registerArtifact("ecmpviz", dynamicGraphicPositionECMP.createArtifact());

      if (yoGraphicsListRegistry != null)
      {
         YoGraphicPosition capturePointViz = new YoGraphicPosition("Capture Point", yoCapturePoint, 0.01, YoAppearance.Blue(), GraphicType.ROTATED_CROSS);
         yoGraphicsListRegistry.registerYoGraphic("Capture Point", capturePointViz);
         yoGraphicsListRegistry.registerArtifact("Capture Point", capturePointViz.createArtifact());
      }

      parentRegistry.addChild(registry);
   }

   public void addFootstepToPlan(Footstep footstep)
   {
      icpPlanner.addFootstepToPlan(footstep);
   }

   public boolean checkAndUpdateFootstep(Footstep footstep)
   {
      return pushRecoveryControlModule.checkAndUpdateFootstep(getTimeRemainingInCurrentState(), footstep);
   }

   public void clearICPPlan()
   {
      icpPlanner.clearPlan();
   }

   public void compute(RobotSide supportLeg, double desiredCoMHeightAcceleration, boolean keepCMPInsideSupportPolygon)
   {
      if (supportLeg == null)
         computeForDoubleSupport();
      else
         computeForSingleSupport(supportLeg);

      finalDesiredICPInWorld.getFrameTuple2dIncludingFrame(finalDesiredCapturePoint2d);

      yoCapturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredCapturePoint2d);
      yoDesiredICPVelocity.getFrameTuple2dIncludingFrame(desiredCapturePointVelocity2d);

      icpBasedLinearMomentumRateOfChangeControlModule.keepCMPInsideSupportPolygon(keepCMPInsideSupportPolygon);
      icpBasedLinearMomentumRateOfChangeControlModule.setDesiredCenterOfMassHeightAcceleration(desiredCoMHeightAcceleration);

      icpBasedLinearMomentumRateOfChangeControlModule.setCapturePoint(capturePoint2d);
      icpBasedLinearMomentumRateOfChangeControlModule.setOmega0(omega0.getDoubleValue());

      icpBasedLinearMomentumRateOfChangeControlModule.setDesiredCapturePoint(desiredCapturePoint2d);
      icpBasedLinearMomentumRateOfChangeControlModule.setFinalDesiredCapturePoint(finalDesiredCapturePoint2d);
      icpBasedLinearMomentumRateOfChangeControlModule.setDesiredCapturePointVelocity(desiredCapturePointVelocity2d);

      icpBasedLinearMomentumRateOfChangeControlModule.setSupportLeg(supportLeg);

      icpBasedLinearMomentumRateOfChangeControlModule.compute();
   }

   public void computeForDoubleSupport()
   {
      getCapturePoint(capturePoint2d);
      icpPlanner.getDesiredCapturePointPositionAndVelocity(desiredCapturePoint2d, desiredCapturePointVelocity2d, capturePoint2d, yoTime.getDoubleValue());
      computePelvisXY(null, desiredCapturePoint2d, desiredCapturePointVelocity2d);
      yoDesiredCapturePoint.set(desiredCapturePoint2d);
      yoDesiredICPVelocity.set(desiredCapturePointVelocity2d);
      updatePushRecovery(true);
   }

   public void computeForSingleSupport(RobotSide supportSide)
   {
      icpPlanner.getDesiredCapturePointPositionAndVelocity(desiredCapturePoint2d, desiredCapturePointVelocity2d, yoTime.getDoubleValue());
      computePelvisXY(supportSide, desiredCapturePoint2d, desiredCapturePointVelocity2d);
      yoDesiredCapturePoint.set(desiredCapturePoint2d);
      yoDesiredICPVelocity.set(desiredCapturePointVelocity2d);
      updatePushRecovery(false);
   }

   public void computePelvisXY(RobotSide supportLeg, FramePoint2d desiredICPToModify, FrameVector2d desiredICPVelocityToModify)
   {
      yoCapturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);
      pelvisICPBasedTranslationManager.compute(supportLeg, capturePoint2d);
      pelvisICPBasedTranslationManager.addICPOffset(desiredICPToModify, desiredICPVelocityToModify);
   }

   public Footstep createFootstepForRecoveringFromDisturbance(RobotSide swingSide, double swingTimeRemaining)
   {
      return pushRecoveryControlModule.createFootstepForRecoveringFromDisturbance(swingSide, swingTimeRemaining);
   }

   public void disablePelvisXYControl()
   {
      pelvisICPBasedTranslationManager.disable();
   }

   public void disablePushRecovery()
   {
      pushRecoveryControlModule.setIsEnabled(false);
   }

   public void enablePelvisXYControl()
   {
      pelvisICPBasedTranslationManager.enable();
   }

   public void enablePushRecovery()
   {
      pushRecoveryControlModule.setIsEnabled(true);
   }

   public double estimateTimeRemainingForSwingUnderDisturbance()
   {
      yoCapturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);
      return icpPlanner.estimateTimeRemainingForStateUnderDisturbance(yoTime.getDoubleValue(), capturePoint2d);
   }

   public void freezePelvisXYControl()
   {
      pelvisICPBasedTranslationManager.freeze();
   }

   public BipedSupportPolygons getBipedSupportPolygons()
   {
      return bipedSupportPolygons;
   }

   public void getCapturePoint(FramePoint2d capturePointToPack)
   {
      yoCapturePoint.getFrameTuple2dIncludingFrame(capturePointToPack);
   }

   public void getDesiredCMP(FramePoint2d desiredCMPToPack)
   {
      icpBasedLinearMomentumRateOfChangeControlModule.getDesiredCMP(desiredCMPToPack);
   }

   public void getDesiredICP(FramePoint2d desiredICPToPack)
   {
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredICPToPack);
   }

   public void getDesiredICPVelocity(FrameVector2d desiredICPVelocityToPack)
   {
      yoDesiredICPVelocity.getFrameTuple2dIncludingFrame(desiredICPVelocityToPack);
   }

   public double getInitialTransferDuration()
   {
      return icpPlanner.getInitialTransferDuration();
   }

   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return icpBasedLinearMomentumRateOfChangeControlModule.getMomentumRateCommand();
   }

   public void getNextExitCMP(FramePoint entryCMPToPack)
   {
      icpPlanner.getNextExitCMP(entryCMPToPack);
   }

   public double getOmega0()
   {
      return omega0.getDoubleValue();
   }

   public double getTimeRemainingInCurrentState()
   {
      return icpPlanner.computeAndReturnTimeInCurrentState(yoTime.getDoubleValue());
   }

   public void handleGoHomeMessage(ModifiableGoHomeMessage message)
   {
      pelvisICPBasedTranslationManager.handleGoHomeMessage(message);
   }

   public void handlePelvisTrajectoryMessage(ModifiablePelvisTrajectoryMessage message)
   {
      pelvisICPBasedTranslationManager.handlePelvisTrajectoryMessage(message);
   }

   public void handleStopAllTrajectoryMessage(ModifiableStopAllTrajectoryMessage message)
   {
      pelvisICPBasedTranslationManager.handleStopAllTrajectoryMessage(message);
   }

   public void initialize()
   {
      initialize(null);
   }

   public void initialize(SideDependentList<? extends PlaneContactState> footContactStates)
   {
      if (footContactStates != null)
         update(footContactStates);
      finalDesiredICPInWorld.set(Double.NaN, Double.NaN);
      yoDesiredCapturePoint.setByProjectionOntoXYPlane(yoCapturePoint);
   }

   public void prepareForDoubleSupportPushRecovery()
   {
      pushRecoveryControlModule.initializeParametersForDoubleSupportPushRecovery();
   }

   public void initializeICPPlanForSingleSupport(RobotSide supportSide)
   {
      icpPlanner.initializeForSingleSupport(yoTime.getDoubleValue(), supportSide);
      icpPlanner.getFinalDesiredCapturePointPosition(finalDesiredICPInWorld);
   }

   public void initializeICPPlanForStanding()
   {
      icpPlanner.initializeForStanding(yoTime.getDoubleValue());
   }

   public void initializeICPPlanForTransfer(RobotSide transferToSide)
   {
      icpPlanner.initializeForTransfer(yoTime.getDoubleValue(), transferToSide);
      icpPlanner.getFinalDesiredCapturePointPosition(finalDesiredICPInWorld);
   }

   public double getICPErrorMagnitude()
   {
      return yoCapturePoint.getXYPlaneDistance(yoDesiredCapturePoint);
   }

   public void getICPError(FrameVector2d icpErrorToPack)
   {
      yoCapturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredCapturePoint2d);
      icpErrorToPack.setIncludingFrame(desiredCapturePoint2d);
      icpErrorToPack.sub(capturePoint2d);
   }

   public boolean isICPPlanDone()
   {
      return icpPlanner.isDone(yoTime.getDoubleValue());
   }

   public boolean isOnExitCMP()
   {
      return icpPlanner.isOnExitCMP();
   }

   public boolean isPushRecoveryEnabled()
   {
      return pushRecoveryControlModule.isEnabled();
   }

   public boolean isRecovering()
   {
      return pushRecoveryControlModule.isRecovering();
   }

   public boolean isRecoveringFromDoubleSupportFall()
   {
      return pushRecoveryControlModule.isEnabled() && pushRecoveryControlModule.isRecoveringFromDoubleSupportFall();
   }

   public boolean isRecoveryImpossible()
   {
      return pushRecoveryControlModule.isCaptureRegionEmpty();
   }

   public boolean isRobotBackToSafeState()
   {
      return pushRecoveryControlModule.isRobotBackToSafeState();
   }

   public RobotSide isRobotFallingFromDoubleSupport()
   {
      return pushRecoveryControlModule.isRobotFallingFromDoubleSupport();
   }

   public void resetPushRecovery()
   {
      pushRecoveryControlModule.reset();
   }

   public void requestICPPlannerToHoldCurrentCoM(double distancceFromSupportPolygonEdges)
   {
      centerOfMassPosition.setToZero(centerOfMassFrame);

      FrameConvexPolygon2d supportPolygonInMidFeetZUp = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();
      convexPolygonShrinker.shrinkConstantDistanceInto(supportPolygonInMidFeetZUp, distancceFromSupportPolygonEdges, shrunkSupportPolygon);

      centerOfMassPosition.changeFrame(shrunkSupportPolygon.getReferenceFrame());
      centerOfMassPosition2d.setByProjectionOntoXYPlaneIncludingFrame(centerOfMassPosition);
      shrunkSupportPolygon.orthogonalProjection(centerOfMassPosition2d);
      centerOfMassPosition.setXY(centerOfMassPosition2d);

      centerOfMassPosition.changeFrame(worldFrame);
      icpPlanner.holdCurrentICP(yoTime.getDoubleValue(), centerOfMassPosition);
   }

   public void setDoubleSupportTime(double newDoubleSupportTime)
   {
      icpPlanner.setDoubleSupportTime(newDoubleSupportTime);
   }

   public void setSingleSupportTime(double newSingleSupportTime)
   {
      icpPlanner.setSingleSupportTime(newSingleSupportTime);
   }

   /**
    * Update the basics: capture point, omega0, and the support polygons.
    */
   public void update(SideDependentList<? extends PlaneContactState> footContactStates)
   {
      CapturePointTools.computeDesiredCentroidalMomentumPivot(yoDesiredCapturePoint, yoDesiredICPVelocity, getOmega0(), desiredECMP);
      ecmpViz.setXY(desiredECMP);
      updateBipedSupportPolygons(footContactStates);
      computeCapturePoint();
      icpBasedLinearMomentumRateOfChangeControlModule.updateCenterOfMassViz();
   }

   private void computeCapturePoint()
   {
      centerOfMassPosition.setToZero(centerOfMassFrame);
      momentumBasedController.getCenterOfMassVelocity(centerOfMassVelocity);

      centerOfMassPosition.changeFrame(worldFrame);
      centerOfMassVelocity.changeFrame(worldFrame);

      centerOfMassPosition2d.setByProjectionOntoXYPlane(centerOfMassPosition);
      centerOfMassVelocity2d.setByProjectionOntoXYPlane(centerOfMassVelocity);

      CapturePointCalculator.computeCapturePoint(capturePoint2d, centerOfMassPosition2d, centerOfMassVelocity2d, getOmega0());

      capturePoint2d.changeFrame(yoCapturePoint.getReferenceFrame());
      yoCapturePoint.setXY(capturePoint2d);
   }

   public void updateBipedSupportPolygons(SideDependentList<? extends PlaneContactState> footContactStates)
   {
      bipedSupportPolygons.updateUsingContactStates(footContactStates);
   }

   public CapturabilityBasedStatus updateAndReturnCapturabilityBasedStatus()
   {
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredCapturePoint2d);
      centerOfMassPosition.setToZero(centerOfMassFrame);
      centerOfMassPosition.changeFrame(worldFrame);

      capturePoint2d.checkReferenceFrameMatch(worldFrame);
      desiredCapturePoint2d.checkReferenceFrameMatch(worldFrame);

      SideDependentList<FrameConvexPolygon2d> footSupportPolygons = bipedSupportPolygons.getFootPolygonsInWorldFrame();

      capturePoint2d.get(capturabilityBasedStatus.capturePoint);
      desiredCapturePoint2d.get(capturabilityBasedStatus.desiredCapturePoint);
      centerOfMassPosition.get(capturabilityBasedStatus.centerOfMass);
      for (RobotSide robotSide : RobotSide.values)
      {
         capturabilityBasedStatus.setSupportPolygon(robotSide, footSupportPolygons.get(robotSide));
      }

      return capturabilityBasedStatus;
   }

   public void updateICPPlanForSingleSupportDisturbances()
   {
      yoCapturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);
      icpPlanner.updatePlanForSingleSupportDisturbances(yoTime.getDoubleValue(), capturePoint2d);
      icpPlanner.getFinalDesiredCapturePointPosition(finalDesiredICPInWorld);
   }

   public void updatePushRecovery(boolean isInDoubleSupport)
   {
      getDesiredICP(desiredCapturePoint2d);
      getCapturePoint(capturePoint2d);
      if (isInDoubleSupport)
      {
         pushRecoveryControlModule.updateForDoubleSupport(desiredCapturePoint2d, capturePoint2d, getOmega0());
      }
      else
      {
         pushRecoveryControlModule.updateForSingleSupport(desiredCapturePoint2d, capturePoint2d, getOmega0());
      }
   }
}
