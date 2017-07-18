package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint;

import static us.ihmc.graphicsDescription.appearance.YoAppearance.Beige;
import static us.ihmc.graphicsDescription.appearance.YoAppearance.Black;
import static us.ihmc.graphicsDescription.appearance.YoAppearance.BlueViolet;
import static us.ihmc.graphicsDescription.appearance.YoAppearance.DarkRed;
import static us.ihmc.graphicsDescription.appearance.YoAppearance.Purple;
import static us.ihmc.graphicsDescription.appearance.YoAppearance.Yellow;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.captureRegion.PushRecoveryControlModule;
import us.ihmc.commonWalkingControlModules.configurations.CapturePointPlannerParameters;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.PelvisICPBasedTranslationManager;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.MomentumRateCommand;
import us.ihmc.commonWalkingControlModules.desiredFootStep.CenterOfMassTrajectoryHandler;
import us.ihmc.commonWalkingControlModules.desiredFootStep.WalkingMessageHandler;
import us.ihmc.commonWalkingControlModules.dynamicReachability.DynamicReachabilityCalculator;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationController;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPOptimizationParameters;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothICPGenerator.CapturePointTools;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.graphicsDescription.yoGraphics.plotting.YoArtifactPosition;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.StopAllTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.packets.walking.CapturabilityBasedStatus;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.controllers.YoPDGains;
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
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class BalanceManager
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private static final boolean ENABLE_DYN_REACHABILITY = true;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final BipedSupportPolygons bipedSupportPolygons;
   private final ICPPlannerWithAngularMomentumOffset icpPlanner;
   private final LinearMomentumRateOfChangeControlModule linearMomentumRateOfChangeControlModule;
   private final DynamicReachabilityCalculator dynamicReachabilityCalculator;

   private final PelvisICPBasedTranslationManager pelvisICPBasedTranslationManager;
   private final PushRecoveryControlModule pushRecoveryControlModule;
   private final MomentumRecoveryControlModule momentumRecoveryControlModule;
   private final HighLevelHumanoidControllerToolbox controllerToolbox;

   private final YoFramePoint yoCenterOfMass = new YoFramePoint("centerOfMass", worldFrame, registry);
   private final YoFramePoint2d yoDesiredCapturePoint = new YoFramePoint2d("desiredICP", worldFrame, registry);
   private final YoFrameVector2d yoDesiredICPVelocity = new YoFrameVector2d("desiredICPVelocity", worldFrame, registry);
   private final YoFramePoint2d yoFinalDesiredICP = new YoFramePoint2d("finalDesiredICP", worldFrame, registry);

   private final YoFramePoint2d yoPerfectCMP = new YoFramePoint2d("perfectCMP", worldFrame, registry);
   private final YoFramePoint2d yoDesiredCMP = new YoFramePoint2d("desiredCMP", worldFrame, registry);

   // TODO It seems that the achieved CMP can be off sometimes.
   // Need to review the computation of the achieved linear momentum rate or of the achieved CMP. (Sylvain)
   private final YoFramePoint2d yoAchievedCMP = new YoFramePoint2d("achievedCMP", worldFrame, registry);

   private final YoBoolean editStepTimingForReachability = new YoBoolean("editStepTimingForReachability", registry);

   private final YoDouble yoTime;

   private final ReferenceFrame centerOfMassFrame;

   private final FramePoint centerOfMassPosition = new FramePoint();
   private final FramePoint2d centerOfMassPosition2d = new FramePoint2d();

   private final FramePoint2d capturePoint2d = new FramePoint2d();
   private final FramePoint tempCapturePoint = new FramePoint();
   private final FramePoint2d desiredCapturePoint2d = new FramePoint2d();
   private final FrameVector2d desiredCapturePointVelocity2d = new FrameVector2d();
   private final FramePoint2d finalDesiredCapturePoint2d = new FramePoint2d();
   /** CMP position according to the ICP planner */
   private final FramePoint2d perfectCMP = new FramePoint2d();

   private final FramePoint2d adjustedDesiredCapturePoint2d = new FramePoint2d();
   private final YoFramePoint2d yoAdjustedDesiredCapturePoint = new YoFramePoint2d("adjustedDesiredICP", worldFrame, registry);

   private final FramePoint2d desiredCMP = new FramePoint2d();
   private final FramePoint2d achievedCMP = new FramePoint2d();

   private final FrameVector2d icpError2d = new FrameVector2d();

   private final ConvexPolygonShrinker convexPolygonShrinker = new ConvexPolygonShrinker();
   private final FrameConvexPolygon2d shrunkSupportPolygon = new FrameConvexPolygon2d();

   private final YoDouble safeDistanceFromSupportEdgesToStopCancelICPPlan = new YoDouble("safeDistanceFromSupportEdgesToStopCancelICPPlan", registry);
   private final YoDouble distanceToShrinkSupportPolygonWhenHoldingCurrent = new YoDouble("distanceToShrinkSupportPolygonWhenHoldingCurrent", registry);

   private final YoBoolean holdICPToCurrentCoMLocationInNextDoubleSupport = new YoBoolean("holdICPToCurrentCoMLocationInNextDoubleSupport", registry);
   private final YoBoolean controlHeightWithMomentum = new YoBoolean("controlHeightWithMomentum", registry);

   private final YoDouble maxICPErrorBeforeSingleSupportX = new YoDouble("maxICPErrorBeforeSingleSupportX", registry);
   private final YoDouble maxICPErrorBeforeSingleSupportY = new YoDouble("maxICPErrorBeforeSingleSupportY", registry);

   private final CapturabilityBasedStatus capturabilityBasedStatus = new CapturabilityBasedStatus();

   private final FrameConvexPolygon2d areaToProjectInto = new FrameConvexPolygon2d();
   private final FrameConvexPolygon2d safeArea = new FrameConvexPolygon2d();

   private final boolean useICPOptimizationControl;
   private final boolean useICPTimingOptimization;

   public BalanceManager(HighLevelHumanoidControllerToolbox controllerToolbox, WalkingControllerParameters walkingControllerParameters,
         CapturePointPlannerParameters capturePointPlannerParameters, ICPOptimizationParameters icpOptimizationParameters, YoVariableRegistry parentRegistry)
   {
      this(controllerToolbox, walkingControllerParameters, capturePointPlannerParameters, icpOptimizationParameters, parentRegistry, true);
   }

   public BalanceManager(HighLevelHumanoidControllerToolbox controllerToolbox, WalkingControllerParameters walkingControllerParameters,
         CapturePointPlannerParameters capturePointPlannerParameters, ICPOptimizationParameters icpOptimizationParameters, YoVariableRegistry parentRegistry,
         boolean use2DCMPProjection)
   {
      CommonHumanoidReferenceFrames referenceFrames = controllerToolbox.getReferenceFrames();
      FullHumanoidRobotModel fullRobotModel = controllerToolbox.getFullRobotModel();

      YoGraphicsListRegistry yoGraphicsListRegistry = controllerToolbox.getYoGraphicsListRegistry();
      SideDependentList<? extends ContactablePlaneBody> contactableFeet = controllerToolbox.getContactableFeet();

      ICPControlGains icpControlGains = walkingControllerParameters.createICPControlGains(registry);

      double controlDT = controllerToolbox.getControlDT();
      double gravityZ = controllerToolbox.getGravityZ();
      double totalMass = TotalMassCalculator.computeSubTreeMass(fullRobotModel.getElevator());

      this.controllerToolbox = controllerToolbox;
      yoTime = controllerToolbox.getYoTime();

      centerOfMassFrame = referenceFrames.getCenterOfMassFrame();

      bipedSupportPolygons = controllerToolbox.getBipedSupportPolygons();

      useICPOptimizationControl = walkingControllerParameters.useOptimizationBasedICPController() && (icpOptimizationParameters != null);
      useICPTimingOptimization = useICPOptimizationControl && icpOptimizationParameters.useTimingOptimization();

      if (useICPOptimizationControl)
      {
         linearMomentumRateOfChangeControlModule = new ICPOptimizationLinearMomentumRateOfChangeControlModule(referenceFrames, bipedSupportPolygons,
               contactableFeet, capturePointPlannerParameters, icpOptimizationParameters, walkingControllerParameters, yoTime, totalMass, gravityZ, controlDT,
               registry, yoGraphicsListRegistry);
      }
      else
      {
         linearMomentumRateOfChangeControlModule = new ICPBasedLinearMomentumRateOfChangeControlModule(referenceFrames, bipedSupportPolygons, controlDT,
               totalMass, gravityZ,icpControlGains, registry, yoGraphicsListRegistry, use2DCMPProjection);
      }
      ICPOptimizationController icpOptimizationController = linearMomentumRateOfChangeControlModule.getICPOptimizationController();

      //icpPlanner = new ICPPlannerWithTimeFreezer(bipedSupportPolygons, contactableFeet, capturePointPlannerParameters, registry, yoGraphicsListRegistry);
      icpPlanner = new ICPPlannerWithAngularMomentumOffset(bipedSupportPolygons, contactableFeet, capturePointPlannerParameters, registry,
                                                           yoGraphicsListRegistry);
      icpPlanner.setOmega0(controllerToolbox.getOmega0());
      icpPlanner.setFinalTransferDuration(walkingControllerParameters.getDefaultTransferTime());

      WalkingMessageHandler walkingMessageHandler = controllerToolbox.getWalkingMessageHandler();
      CenterOfMassTrajectoryHandler comTrajectoryHandler = walkingMessageHandler.getComTrajectoryHandler();


      if (ENABLE_DYN_REACHABILITY)
      {
         dynamicReachabilityCalculator = new DynamicReachabilityCalculator(icpPlanner, icpOptimizationController, fullRobotModel, centerOfMassFrame,
               walkingControllerParameters.getDynamicReachabilityParameters(), registry, yoGraphicsListRegistry);
      }
      else
      {
         dynamicReachabilityCalculator = null;
      }
      editStepTimingForReachability.set(walkingControllerParameters.editStepTimingForReachability());

      safeDistanceFromSupportEdgesToStopCancelICPPlan.set(0.05);
      distanceToShrinkSupportPolygonWhenHoldingCurrent.set(0.08);

      maxICPErrorBeforeSingleSupportX.set(walkingControllerParameters.getMaxICPErrorBeforeSingleSupportX());
      maxICPErrorBeforeSingleSupportY.set(walkingControllerParameters.getMaxICPErrorBeforeSingleSupportY());

      YoPDGains pelvisXYControlGains = walkingControllerParameters.createPelvisICPBasedXYControlGains(registry);
      pelvisICPBasedTranslationManager = new PelvisICPBasedTranslationManager(controllerToolbox, bipedSupportPolygons, pelvisXYControlGains, registry);

      pushRecoveryControlModule = new PushRecoveryControlModule(bipedSupportPolygons, controllerToolbox, walkingControllerParameters, registry);

      double maxAllowedDistanceCMPSupport = walkingControllerParameters.getMaxAllowedDistanceCMPSupport();
      SideDependentList<FrameConvexPolygon2d> defaultFootPolygons = controllerToolbox.getDefaultFootPolygons();
      momentumRecoveryControlModule = new MomentumRecoveryControlModule(defaultFootPolygons, maxAllowedDistanceCMPSupport, registry, yoGraphicsListRegistry);

      controlHeightWithMomentum.set(walkingControllerParameters.controlHeightWithMomentum());

      String graphicListName = getClass().getSimpleName();

      if (yoGraphicsListRegistry != null)
      {
         YoGraphicPosition centerOfMassViz = new YoGraphicPosition("Center Of Mass", yoCenterOfMass, 0.006, Black(), GraphicType.BALL_WITH_CROSS);
         YoGraphicPosition desiredCapturePointViz = new YoGraphicPosition("Desired Capture Point", yoDesiredCapturePoint, 0.01, Yellow(), GraphicType.BALL_WITH_ROTATED_CROSS);
         YoGraphicPosition finalDesiredCapturePointViz = new YoGraphicPosition("Final Desired Capture Point", yoFinalDesiredICP, 0.01, Beige(), GraphicType.BALL_WITH_ROTATED_CROSS);
         YoGraphicPosition desiredCMPViz = new YoGraphicPosition("Desired CMP", yoDesiredCMP, 0.012, Purple(), GraphicType.BALL_WITH_CROSS);
         YoGraphicPosition achievedCMPViz = new YoGraphicPosition("Achieved CMP", yoAchievedCMP, 0.005, DarkRed(), GraphicType.BALL_WITH_CROSS);
         YoGraphicPosition perfectCMPViz = new YoGraphicPosition("Perfect CMP", yoPerfectCMP, 0.002, BlueViolet());

         YoGraphicPosition adjustedDesiredCapturePointViz = new YoGraphicPosition("Adjusted Desired Capture Point", yoAdjustedDesiredCapturePoint, 0.005, Yellow(), GraphicType.DIAMOND);
         yoGraphicsListRegistry.registerArtifact(graphicListName, adjustedDesiredCapturePointViz.createArtifact());

         yoGraphicsListRegistry.registerArtifact(graphicListName, centerOfMassViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, desiredCapturePointViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, finalDesiredCapturePointViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, desiredCMPViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, achievedCMPViz.createArtifact());
         YoArtifactPosition perfectCMPArtifact = perfectCMPViz.createArtifact();
         perfectCMPArtifact.setVisible(false);
         yoGraphicsListRegistry.registerArtifact(graphicListName, perfectCMPArtifact);
      }
      yoCenterOfMass.setToNaN();
      yoDesiredCapturePoint.setToNaN();
      yoFinalDesiredICP.setToNaN();
      yoDesiredCMP.setToNaN();
      yoAchievedCMP.setToNaN();
      yoPerfectCMP.setToNaN();

      parentRegistry.addChild(registry);
   }

   public void setMomentumWeight(Vector3D linearWeight)
   {
      linearMomentumRateOfChangeControlModule.setMomentumWeight(linearWeight);
   }

   public void setMomentumWeight(Vector3D angularWeight, Vector3D linearWeight)
   {
      linearMomentumRateOfChangeControlModule.setMomentumWeight(angularWeight, linearWeight);
   }

   public void setHighMomentumWeightForRecovery(Vector3D highLinearWeight)
   {
      linearMomentumRateOfChangeControlModule.setHighMomentumWeightForRecovery(highLinearWeight);
   }

   public void addFootstepToPlan(Footstep footstep, FootstepTiming timing)
   {
      icpPlanner.addFootstepToPlan(footstep, timing);
      linearMomentumRateOfChangeControlModule.addFootstepToPlan(footstep, timing);
   }

   /**
    * Sets the next footstep that the robot will take. Should be set at the beginning of transfer.
    * @param upcomingFootstep
    */
   public void setUpcomingFootstep(Footstep upcomingFootstep)
   {
      if (ENABLE_DYN_REACHABILITY)
         dynamicReachabilityCalculator.setUpcomingFootstep(upcomingFootstep);
   }

   /**
    * Sets the next footstep that the robot will take. Should be set at the beginning of swing. Modifies the momentum recovery control module, which checks
    * the stability of the robot.
    * @param nextFootstep
    */
   public void setNextFootstep(Footstep nextFootstep)
   {
      momentumRecoveryControlModule.setNextFootstep(nextFootstep);
      if (ENABLE_DYN_REACHABILITY)
         dynamicReachabilityCalculator.setUpcomingFootstep(nextFootstep);
   }

   public boolean wasTimingAdjustedForReachability()
   {
      if (ENABLE_DYN_REACHABILITY)
         return dynamicReachabilityCalculator.wasTimingAdjusted();
      else
         return false;
   }

   public double getCurrentTransferDurationAdjustedForReachability()
   {
      return icpPlanner.getTransferDuration(0);
   }

   public double getCurrentSwingDurationAdjustedForReachability()
   {
      return icpPlanner.getSwingDuration(0);
   }

   public double getNextTransferDurationAdjustedForReachability()
   {
      return icpPlanner.getTransferDuration(1);
   }


   public boolean checkAndUpdateFootstep(Footstep footstep)
   {
      return pushRecoveryControlModule.checkAndUpdateFootstep(getTimeRemainingInCurrentState(), footstep);
   }

   public boolean checkAndUpdateFootstepFromICPOptimization(Footstep footstep)
   {
      return linearMomentumRateOfChangeControlModule.getUpcomingFootstepSolution(footstep);
   }

   public void clearICPPlan()
   {
      icpPlanner.clearPlan();
      linearMomentumRateOfChangeControlModule.clearPlan();
   }

   public void setICPPlanSupportSide(RobotSide robotSide)
   {
      icpPlanner.setSupportLeg(robotSide);
      linearMomentumRateOfChangeControlModule.setSupportLeg(robotSide);
   }

   public void setICPPlanTransferToSide(RobotSide robotSide)
   {
      icpPlanner.setTransferToSide(robotSide);
      linearMomentumRateOfChangeControlModule.setTransferToSide(robotSide);
   }

   public void setICPPlanTransferFromSide(RobotSide robotSide)
   {
      icpPlanner.setTransferFromSide(robotSide);
      linearMomentumRateOfChangeControlModule.setTransferFromSide(robotSide);
   }

   private final FramePoint copEstimate = new FramePoint();
   public void compute(RobotSide supportLeg, double desiredCoMHeightAcceleration, boolean keepCMPInsideSupportPolygon, boolean controlHeightWithMomentum)
   {
      controllerToolbox.getCapturePoint(capturePoint2d);
      controllerToolbox.getCoP(copEstimate);

      icpPlanner.compute(capturePoint2d, yoTime.getDoubleValue());
      icpPlanner.modifyDesiredICPForAngularMomentum(copEstimate, supportLeg);

      icpPlanner.getDesiredCapturePointPosition(desiredCapturePoint2d);
      icpPlanner.getDesiredCapturePointVelocity(desiredCapturePointVelocity2d);
      icpPlanner.getDesiredCentroidalMomentumPivotPosition(perfectCMP);

      pelvisICPBasedTranslationManager.compute(supportLeg, capturePoint2d);
      pelvisICPBasedTranslationManager.addICPOffset(desiredCapturePoint2d, desiredCapturePointVelocity2d);

      double omega0 = controllerToolbox.getOmega0();
      if (supportLeg == null)
         pushRecoveryControlModule.updateForDoubleSupport(desiredCapturePoint2d, capturePoint2d, omega0);
      else
         pushRecoveryControlModule.updateForSingleSupport(desiredCapturePoint2d, capturePoint2d, omega0);

      yoDesiredCapturePoint.set(desiredCapturePoint2d);
      yoDesiredICPVelocity.set(desiredCapturePointVelocity2d);

      yoFinalDesiredICP.getFrameTuple2dIncludingFrame(finalDesiredCapturePoint2d);

      // --- compute adjusted desired capture point
      controllerToolbox.getAdjustedDesiredCapturePoint(desiredCapturePoint2d, adjustedDesiredCapturePoint2d);
      yoAdjustedDesiredCapturePoint.set(adjustedDesiredCapturePoint2d);
      desiredCapturePoint2d.setIncludingFrame(adjustedDesiredCapturePoint2d);
      // ---

      getICPError(icpError2d);
      momentumRecoveryControlModule.setICPError(icpError2d);
      momentumRecoveryControlModule.setSupportSide(supportLeg);
      momentumRecoveryControlModule.setCapturePoint(capturePoint2d);
      momentumRecoveryControlModule.setSupportPolygon(bipedSupportPolygons.getSupportPolygonInWorld());
      momentumRecoveryControlModule.compute();

      momentumRecoveryControlModule.getCMPProjectionArea(areaToProjectInto, safeArea);
      if (!keepCMPInsideSupportPolygon)
         areaToProjectInto.clearAndUpdate(worldFrame);
      linearMomentumRateOfChangeControlModule.setCMPProjectionArea(areaToProjectInto, safeArea);

      if (momentumRecoveryControlModule.getUseHighMomentumWeight())
      {
         linearMomentumRateOfChangeControlModule.setHighMomentumWeight();
      }
      else
      {
         linearMomentumRateOfChangeControlModule.setDefaultMomentumWeight();
      }

      linearMomentumRateOfChangeControlModule.setControlHeightWithMomentum(this.controlHeightWithMomentum.getBooleanValue() && controlHeightWithMomentum);
      linearMomentumRateOfChangeControlModule.setDesiredCenterOfMassHeightAcceleration(desiredCoMHeightAcceleration);
      linearMomentumRateOfChangeControlModule.setCapturePoint(capturePoint2d);
      linearMomentumRateOfChangeControlModule.setOmega0(omega0);
      linearMomentumRateOfChangeControlModule.setDesiredCapturePoint(desiredCapturePoint2d);
      linearMomentumRateOfChangeControlModule.setFinalDesiredCapturePoint(finalDesiredCapturePoint2d);
      linearMomentumRateOfChangeControlModule.setDesiredCapturePointVelocity(desiredCapturePointVelocity2d);
      linearMomentumRateOfChangeControlModule.setPerfectCMP(perfectCMP);
      linearMomentumRateOfChangeControlModule.setSupportLeg(supportLeg);
      yoDesiredCMP.getFrameTuple2d(desiredCMP);
      linearMomentumRateOfChangeControlModule.compute(desiredCMP, desiredCMP);
      yoDesiredCMP.set(desiredCMP);
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

   public double getOptimizedTimeRemaining()
   {
      return linearMomentumRateOfChangeControlModule.getOptimizedTimeRemaining();
   }

   public double estimateTimeRemainingForSwingUnderDisturbance()
   {
      controllerToolbox.getCapturePoint(capturePoint2d);

      return icpPlanner.estimateTimeRemainingForStateUnderDisturbance(capturePoint2d);
   }

   public void freezePelvisXYControl()
   {
      pelvisICPBasedTranslationManager.freeze();
   }

   public void getDesiredCMP(FramePoint2d desiredCMPToPack)
   {
      yoDesiredCMP.getFrameTuple2dIncludingFrame(desiredCMPToPack);
   }

   public void getPerfectCMP(FramePoint2d desiredCMPToPack)
   {
      yoPerfectCMP.getFrameTuple2dIncludingFrame(desiredCMPToPack);
   }

   public void getDesiredICP(FramePoint2d desiredICPToPack)
   {
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredICPToPack);
   }

   public void getDesiredICPVelocity(FrameVector2d desiredICPVelocityToPack)
   {
      yoDesiredICPVelocity.getFrameTuple2dIncludingFrame(desiredICPVelocityToPack);
   }

   public MomentumRateCommand getInverseDynamicsCommand()
   {
      return linearMomentumRateOfChangeControlModule.getMomentumRateCommand();
   }

   public void getNextExitCMP(FramePoint entryCMPToPack)
   {
      icpPlanner.getNextExitCMP(entryCMPToPack);
   }

   public double getTimeRemainingInCurrentState()
   {
      return icpPlanner.getTimeInCurrentState();
   }

   public void goHome()
   {
      if (pelvisICPBasedTranslationManager.isEnabled())
         pelvisICPBasedTranslationManager.goToHome();
   }

   public void handlePelvisTrajectoryCommand(PelvisTrajectoryCommand command)
   {
      pelvisICPBasedTranslationManager.handlePelvisTrajectoryCommand(command);
   }

   public void handleStopAllTrajectoryCommand(StopAllTrajectoryCommand command)
   {
      pelvisICPBasedTranslationManager.handleStopAllTrajectoryCommand(command);
   }

   public void initialize()
   {
      update();
      yoFinalDesiredICP.set(Double.NaN, Double.NaN);
      controllerToolbox.getCapturePoint(tempCapturePoint);
      yoDesiredCapturePoint.setByProjectionOntoXYPlane(tempCapturePoint);

      icpPlanner.holdCurrentICP(tempCapturePoint);
      icpPlanner.initializeForStanding(yoTime.getDoubleValue());

      linearMomentumRateOfChangeControlModule.initializeForStanding();
   }

   public void prepareForDoubleSupportPushRecovery()
   {
      pushRecoveryControlModule.initializeParametersForDoubleSupportPushRecovery();
   }

   public void initializeICPPlanForSingleSupport(double swingTime, double transferTime, double finalTransferTime)
   {
      setFinalTransferTime(finalTransferTime);
      icpPlanner.initializeForSingleSupport(yoTime.getDoubleValue());
      linearMomentumRateOfChangeControlModule.initializeForSingleSupport();

      if (Double.isFinite(swingTime) && Double.isFinite(transferTime) && ENABLE_DYN_REACHABILITY)
      {
         dynamicReachabilityCalculator.setInSwing();

         if (editStepTimingForReachability.getBooleanValue())
            dynamicReachabilityCalculator.verifyAndEnsureReachability();
         else
            dynamicReachabilityCalculator.checkReachabilityOfStep();
      }
   }

   public void initializeICPPlanForStanding(double finalTransferTime)
   {
      if (holdICPToCurrentCoMLocationInNextDoubleSupport.getBooleanValue())
      {
         requestICPPlannerToHoldCurrentCoM();
         holdICPToCurrentCoMLocationInNextDoubleSupport.set(false);
      }
      setFinalTransferTime(finalTransferTime);
      icpPlanner.initializeForStanding(yoTime.getDoubleValue());
      linearMomentumRateOfChangeControlModule.initializeForStanding();
   }

   public void initializeICPPlanForTransfer(double swingTime, double transferTime, double finalTransferTime)
   {
      if (holdICPToCurrentCoMLocationInNextDoubleSupport.getBooleanValue())
      {
         requestICPPlannerToHoldCurrentCoM();
         holdICPToCurrentCoMLocationInNextDoubleSupport.set(false);
      }
      setFinalTransferTime(finalTransferTime);
      icpPlanner.initializeForTransfer(yoTime.getDoubleValue());

      linearMomentumRateOfChangeControlModule.setReferenceICPVelocity(yoDesiredICPVelocity.getFrameTuple2d());
      linearMomentumRateOfChangeControlModule.initializeForTransfer();

      if (Double.isFinite(swingTime) && Double.isFinite(transferTime) && ENABLE_DYN_REACHABILITY)
      {
         dynamicReachabilityCalculator.setInTransfer();

         if (editStepTimingForReachability.getBooleanValue())
            dynamicReachabilityCalculator.verifyAndEnsureReachability();
         else
            dynamicReachabilityCalculator.checkReachabilityOfStep();
      }
   }

   public boolean isTransitionToSingleSupportSafe(RobotSide transferToSide)
   {
      getICPError(icpError2d);
      ReferenceFrame leadingAnkleZUpFrame = bipedSupportPolygons.getAnkleZUpFrames().get(transferToSide);
      icpError2d.changeFrame(leadingAnkleZUpFrame);
      double ellipticErrorSquared = MathTools.square(icpError2d.getX() / maxICPErrorBeforeSingleSupportX.getDoubleValue())
            + MathTools.square(icpError2d.getY() / maxICPErrorBeforeSingleSupportY.getDoubleValue());
      boolean closeEnough = ellipticErrorSquared < 1.0;
      return closeEnough;
   }

   public boolean isTransitionToStandingSafe()
   {
      FrameConvexPolygon2d supportPolygonInWorld = bipedSupportPolygons.getSupportPolygonInWorld();
      controllerToolbox.getCapturePoint(capturePoint2d);

      // signed distance returns a negative number if the point is inside the polygon.
      return supportPolygonInWorld.signedDistance(capturePoint2d) < -safeDistanceFromSupportEdgesToStopCancelICPPlan.getDoubleValue();
   }

   public double getICPErrorMagnitude()
   {
      controllerToolbox.getCapturePoint(capturePoint2d);
      return capturePoint2d.distance(yoDesiredCapturePoint.getFrameTuple2d());
   }

   public void getICPError(FrameVector2d icpErrorToPack)
   {
      controllerToolbox.getCapturePoint(capturePoint2d);
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredCapturePoint2d);
      icpErrorToPack.setIncludingFrame(desiredCapturePoint2d);
      icpErrorToPack.sub(capturePoint2d);
   }

   public boolean isICPPlanDone()
   {
      return icpPlanner.isDone();
   }

   public boolean isOnExitCMP()
   {
      return icpPlanner.isOnExitCMP();
   }

   public boolean isPushRecoveryEnabled()
   {
      return pushRecoveryControlModule.isEnabled();
   }

   public boolean useICPOptimization()
   {
      return useICPOptimizationControl;
   }

   public boolean useICPTimingOptimization()
   {
      return useICPTimingOptimization;
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

   public void requestICPPlannerToHoldCurrentCoMInNextDoubleSupport()
   {
      holdICPToCurrentCoMLocationInNextDoubleSupport.set(true);
   }

   public void requestICPPlannerToHoldCurrentCoM()
   {
      centerOfMassPosition.setToZero(centerOfMassFrame);

      FrameConvexPolygon2d supportPolygonInMidFeetZUp = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();
      convexPolygonShrinker.shrinkConstantDistanceInto(supportPolygonInMidFeetZUp, distanceToShrinkSupportPolygonWhenHoldingCurrent.getDoubleValue(), shrunkSupportPolygon);

      centerOfMassPosition.changeFrame(shrunkSupportPolygon.getReferenceFrame());
      centerOfMassPosition2d.setByProjectionOntoXYPlaneIncludingFrame(centerOfMassPosition);
      shrunkSupportPolygon.orthogonalProjection(centerOfMassPosition2d);
      centerOfMassPosition.setXY(centerOfMassPosition2d);

      centerOfMassPosition.changeFrame(worldFrame);
      icpPlanner.holdCurrentICP(centerOfMassPosition);
   }

   public void setFinalTransferTime(double finalTransferTime)
   {
      icpPlanner.setFinalTransferDuration(finalTransferTime);
      linearMomentumRateOfChangeControlModule.setFinalTransferDuration(finalTransferTime);
   }

   /**
    * Update the basics: capture point, omega0, and the support polygons.
    */
   public void update()
   {
      centerOfMassPosition.setToZero(centerOfMassFrame);
      yoCenterOfMass.setAndMatchFrame(centerOfMassPosition);
      double omega0 = controllerToolbox.getOmega0();
      CapturePointTools.computeDesiredCentroidalMomentumPivot(yoDesiredCapturePoint, yoDesiredICPVelocity, omega0, yoPerfectCMP);
      icpPlanner.getFinalDesiredCapturePointPosition(yoFinalDesiredICP);
   }

   public void computeAchievedCMP(FrameVector achievedLinearMomentumRate)
   {
      linearMomentumRateOfChangeControlModule.computeAchievedCMP(achievedLinearMomentumRate, achievedCMP);
      yoAchievedCMP.setAndMatchFrame(achievedCMP);
   }

   public CapturabilityBasedStatus updateAndReturnCapturabilityBasedStatus()
   {
      yoDesiredCapturePoint.getFrameTuple2dIncludingFrame(desiredCapturePoint2d);
      centerOfMassPosition.setToZero(centerOfMassFrame);
      centerOfMassPosition.changeFrame(worldFrame);

      controllerToolbox.getCapturePoint(capturePoint2d);
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

   public void updateCurrentICPPlan()
   {
      icpPlanner.updateCurrentPlan();
   }

   public void updateSwingTimeRemaining(double timeRemainingInSwing)
   {
      linearMomentumRateOfChangeControlModule.submitRemainingTimeInSwingUnderDisturbance(timeRemainingInSwing);
   }

   public void getCapturePoint(FramePoint2d capturePointToPack)
   {
      controllerToolbox.getCapturePoint(capturePointToPack);
   }

   public void minimizeAngularMomentumRateZ(boolean enable)
   {
      linearMomentumRateOfChangeControlModule.minimizeAngularMomentumRateZ(enable);
   }
}
