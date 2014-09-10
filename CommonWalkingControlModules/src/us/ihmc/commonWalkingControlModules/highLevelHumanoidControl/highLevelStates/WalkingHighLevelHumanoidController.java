package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.YoFramePoint2dInPolygonCoordinate;
import us.ihmc.commonWalkingControlModules.captureRegion.PushRecoveryControlModule;
import us.ihmc.commonWalkingControlModules.captureRegion.PushRecoveryControlModule.IsFallingFromDoubleSupportCondition;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.ChestOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.foot.LegSingularityAndKneeCollapseAvoidanceControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationManager;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsDataVisualizer;
import us.ihmc.commonWalkingControlModules.desiredFootStep.UpcomingFootstepList;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingManagers;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingProviders;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ICPBasedMomentumRateOfChangeControlModule;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.InstantaneousCapturePointPlanner;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointTrajectoryData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumControlModuleBridge.MomentumControlModuleType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.packetConsumers.FootPoseProvider;
import us.ihmc.commonWalkingControlModules.sensors.FootSwitchInterface;
import us.ihmc.commonWalkingControlModules.sensors.HeelSwitch;
import us.ihmc.commonWalkingControlModules.sensors.ToeSwitch;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightPartialDerivativesData;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesCalculator;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesData;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTimeDerivativesSmoother;
import us.ihmc.commonWalkingControlModules.trajectories.CoMHeightTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.CoMXYTimeDerivativesData;
import us.ihmc.commonWalkingControlModules.trajectories.ContactStatesAndUpcomingFootstepData;
import us.ihmc.commonWalkingControlModules.trajectories.FlatThenPolynomialCoMHeightTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.SwingTimeCalculationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.TransferTimeCalculationProvider;
import us.ihmc.communication.packets.dataobjects.HighLevelState;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.kinematics.AverageOrientationCalculator;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.PoseReferenceFrame;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.math.trajectories.providers.TrajectoryParameters;
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.Twist;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.graphics.YoGraphicPosition;
import us.ihmc.yoUtilities.math.frames.YoFramePoint;
import us.ihmc.yoUtilities.math.frames.YoFramePoint2d;
import us.ihmc.yoUtilities.math.frames.YoFrameVector2d;

import com.yobotics.simulationconstructionset.util.controller.GainCalculator;
import com.yobotics.simulationconstructionset.util.controller.PDController;
import com.yobotics.simulationconstructionset.util.statemachines.State;
import com.yobotics.simulationconstructionset.util.statemachines.StateMachine;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransition;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionAction;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionCondition;

public class WalkingHighLevelHumanoidController extends AbstractHighLevelHumanoidControlPattern
{
   private boolean VISUALIZE = true;

   private static final boolean DO_TRANSITION_WHEN_TIME_IS_UP = false;
   private static final boolean DESIREDICP_FROM_POLYGON_COORDINATE = false;

   private final static HighLevelState controllerState = HighLevelState.WALKING;
   private final static MomentumControlModuleType MOMENTUM_CONTROL_MODULE_TO_USE = MomentumControlModuleType.OPT_NULLSPACE;

   private final double PELVIS_YAW_INITIALIZATION_TIME = 1.5;

   private PushRecoveryControlModule pushRecoveryModule;

   private final BooleanYoVariable alreadyBeenInDoubleSupportOnce;

   private final static boolean DEBUG = false;

   private final StateMachine<WalkingState> stateMachine;
   private final CenterOfMassJacobian centerOfMassJacobian;

   private final CoMHeightTrajectoryGenerator centerOfMassHeightTrajectoryGenerator;
   private final CoMHeightTimeDerivativesCalculator coMHeightTimeDerivativesCalculator = new CoMHeightTimeDerivativesCalculator();
   private final CoMHeightTimeDerivativesSmoother coMHeightTimeDerivativesSmoother;
   private final DoubleYoVariable desiredCoMHeightFromTrajectory = new DoubleYoVariable("desiredCoMHeightFromTrajectory", registry);
   private final DoubleYoVariable desiredCoMHeightVelocityFromTrajectory = new DoubleYoVariable("desiredCoMHeightVelocityFromTrajectory", registry);
   private final DoubleYoVariable desiredCoMHeightAccelerationFromTrajectory = new DoubleYoVariable("desiredCoMHeightAccelerationFromTrajectory", registry);
   //   private final DoubleYoVariable desiredCoMHeightBeforeSmoothing = new DoubleYoVariable("desiredCoMHeightBeforeSmoothing", registry);
   //   private final DoubleYoVariable desiredCoMHeightVelocityBeforeSmoothing = new DoubleYoVariable("desiredCoMHeightVelocityBeforeSmoothing", registry);
   //   private final DoubleYoVariable desiredCoMHeightAccelerationBeforeSmoothing = new DoubleYoVariable("desiredCoMHeightAccelerationBeforeSmoothing", registry);
   private final DoubleYoVariable desiredCoMHeightCorrected = new DoubleYoVariable("desiredCoMHeightCorrected", registry);
   private final DoubleYoVariable desiredCoMHeightVelocityCorrected = new DoubleYoVariable("desiredCoMHeightVelocityCorrected", registry);
   private final DoubleYoVariable desiredCoMHeightAccelerationCorrected = new DoubleYoVariable("desiredCoMHeightAccelerationCorrected", registry);
   private final DoubleYoVariable desiredCoMHeightAfterSmoothing = new DoubleYoVariable("desiredCoMHeightAfterSmoothing", registry);
   private final DoubleYoVariable desiredCoMHeightVelocityAfterSmoothing = new DoubleYoVariable("desiredCoMHeightVelocityAfterSmoothing", registry);
   private final DoubleYoVariable desiredCoMHeightAccelerationAfterSmoothing = new DoubleYoVariable("desiredCoMHeightAccelerationAfterSmoothing", registry);

   private final PDController centerOfMassHeightController;

   private final YoFramePoint2d transferToFootstep = new YoFramePoint2d("transferToFootstep", worldFrame, registry);

   private final BooleanYoVariable resetIntegratorsAfterSwing = new BooleanYoVariable("resetIntegratorsAfterSwing", registry);

   private final DoubleYoVariable stopInDoubleSupporTrajectoryTime = new DoubleYoVariable("stopInDoubleSupporTrajectoryTime", registry);
   private final DoubleYoVariable dwellInSingleSupportDuration = new DoubleYoVariable("dwellInSingleSupportDuration",
         "Amount of time to stay in single support after the ICP trajectory is done if you haven't registered a touchdown yet", registry);

   private final BooleanYoVariable loopControllerForever = new BooleanYoVariable("loopControllerForever", "For checking memory and profiling", registry);
   private final BooleanYoVariable justFall = new BooleanYoVariable("justFall", registry);

   private final BooleanYoVariable controlPelvisHeightInsteadOfCoMHeight = new BooleanYoVariable("controlPelvisHeightInsteadOfCoMHeight", registry);

   private final BooleanYoVariable hasMinimumTimePassed = new BooleanYoVariable("hasMinimumTimePassed", registry);
   private final DoubleYoVariable minimumSwingFraction = new DoubleYoVariable("minimumSwingFraction", registry);

   private final BooleanYoVariable hasICPPlannerFinished = new BooleanYoVariable("hasICPPlannerFinished", registry);
   private final DoubleYoVariable timeThatICPPlannerFinished = new DoubleYoVariable("timeThatICPPlannerFinished", registry);
   private final BooleanYoVariable initializingICPTrajectory = new BooleanYoVariable("initializingICPTrajectory", registry);

   // private final FinalDesiredICPCalculator finalDesiredICPCalculator;

   private final BooleanYoVariable rememberFinalICPFromSingleSupport = new BooleanYoVariable("rememberFinalICPFromSingleSupport", registry);
   private final YoFramePoint2d finalDesiredICPInWorld = new YoFramePoint2d("finalDesiredICPInWorld", "", worldFrame, registry);

   private final SideDependentList<FootSwitchInterface> footSwitches;

   private final DoubleYoVariable maxICPErrorBeforeSingleSupport = new DoubleYoVariable("maxICPErrorBeforeSingleSupport", registry);

   private final DoubleYoVariable minOrbitalEnergyForSingleSupport = new DoubleYoVariable("minOrbitalEnergyForSingleSupport", registry);
   private final DoubleYoVariable amountToBeInsideSingleSupport = new DoubleYoVariable("amountToBeInsideSingleSupport", registry);
   private final DoubleYoVariable amountToBeInsideDoubleSupport = new DoubleYoVariable("amountToBeInsideDoubleSupport", registry);

   private final SwingTimeCalculationProvider swingTimeCalculationProvider;
   private final TransferTimeCalculationProvider transferTimeCalculationProvider;

   private final DoubleYoVariable additionalSwingTimeForICP = new DoubleYoVariable("additionalSwingTimeForICP", registry);

   private final BooleanYoVariable readyToGrabNextFootstep = new BooleanYoVariable("readyToGrabNextFootstep", registry);

   private final EnumYoVariable<RobotSide> previousSupportSide = new EnumYoVariable<RobotSide>("previousSupportSide", registry, RobotSide.class);

   private final DoubleYoVariable moveICPAwayDuringSwingDistance = new DoubleYoVariable("moveICPAwayDuringSwingDistance", registry);
   private final DoubleYoVariable moveICPAwayAtEndOfSwingDistance = new DoubleYoVariable("moveICPAwayAtEndOfSwingDistance", registry);
   private final DoubleYoVariable singleSupportTimeLeftBeforeShift = new DoubleYoVariable("singleSupportTimeLeftBeforeShift", registry);

   private final HashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters;
   private final InstantaneousCapturePointPlanner instantaneousCapturePointPlanner;

   private final ICPBasedMomentumRateOfChangeControlModule icpBasedMomentumRateOfChangeControlModule;

   private final BooleanYoVariable icpTrajectoryHasBeenInitialized;

   private final UpcomingFootstepList upcomingFootstepList;
   private final FootPoseProvider footPoseProvider;

   private final AverageOrientationCalculator averageOrientationCalculator = new AverageOrientationCalculator();

   private final ICPAndMomentumBasedController icpAndMomentumBasedController;

   private final EnumYoVariable<RobotSide> upcomingSupportLeg;
   private final EnumYoVariable<RobotSide> supportLeg;
   private final BipedSupportPolygons bipedSupportPolygons;
   private final YoFramePoint capturePoint;
   private final YoFramePoint2d desiredICP;
   private final DoubleYoVariable icpStandOffsetX = new DoubleYoVariable("icpStandOffsetX", registry);
   private final DoubleYoVariable icpStandOffsetY = new DoubleYoVariable("icpStandOffsetY", registry);
   private final YoFrameVector2d desiredICPVelocity;

   private final DoubleYoVariable swingTimeRemainingForICPMoveViz = new DoubleYoVariable("swingTimeRemainingForICPMoveViz", registry);
   private final DoubleYoVariable amountToMoveICPAway = new DoubleYoVariable("amountToMoveICPAway", registry);
   private final DoubleYoVariable distanceFromLineToOriginalICP = new DoubleYoVariable("distanceFromLineToOriginalICP", registry);

   private final DoubleYoVariable controlledCoMHeightAcceleration;
   private final DoubleYoVariable controllerInitializationTime;

   private final TransferToAndNextFootstepsDataVisualizer transferToAndNextFootstepsDataVisualizer;

   private final BooleanYoVariable doneFinishingSingleSupportTransfer = new BooleanYoVariable("doneFinishingSingleSupportTransfer", registry);
   private final BooleanYoVariable stayInTransferWalkingState = new BooleanYoVariable("stayInTransferWalkingState", registry);

   private final BooleanYoVariable ecmpBasedToeOffHasBeenInitialized = new BooleanYoVariable("ecmpBasedToeOffHasBeenInitialized", registry);
   private final YoFramePoint2d desiredECMP = new YoFramePoint2d("desiredECMP", "", worldFrame, registry);
   private final BooleanYoVariable desiredECMPinSupportPolygon = new BooleanYoVariable("desiredECMPinSupportPolygon", registry);
   private YoFramePoint ecmpViz = new YoFramePoint("ecmpViz", worldFrame, registry);
   private TransferToAndNextFootstepsData neutralFootstepsData;

   private final YoFramePoint2dInPolygonCoordinate doubleSupportDesiredICP;

   private final BooleanYoVariable doPrepareManipulationForLocomotion = new BooleanYoVariable("doPrepareManipulationForLocomotion", registry);

   private final BooleanYoVariable isInFlamingoStance = new BooleanYoVariable("isInFlamingoStance", registry);
   private final DoubleYoVariable icpProjectionTimeOffset = new DoubleYoVariable("icpProjectionTimeOffset", registry);

   public WalkingHighLevelHumanoidController(VariousWalkingProviders variousWalkingProviders, VariousWalkingManagers variousWalkingManagers,
         CoMHeightTrajectoryGenerator centerOfMassHeightTrajectoryGenerator, TransferTimeCalculationProvider transferTimeCalculationProvider,
         SwingTimeCalculationProvider swingTimeCalculationProvider, WalkingControllerParameters walkingControllerParameters,
         ICPBasedMomentumRateOfChangeControlModule momentumRateOfChangeControlModule, InstantaneousCapturePointPlanner instantaneousCapturePointPlanner,
         ICPAndMomentumBasedController icpAndMomentumBasedController, MomentumBasedController momentumBasedController)
   {
      super(variousWalkingProviders, variousWalkingManagers, momentumBasedController, walkingControllerParameters, controllerState);

      super.addUpdatables(icpAndMomentumBasedController.getUpdatables());

      setupManagers(variousWalkingManagers);

      doPrepareManipulationForLocomotion.set(walkingControllerParameters.doPrepareManipulationForLocomotion());

      if (dynamicGraphicObjectsListRegistry == null)
      {
         VISUALIZE = false;
      }

      if (VISUALIZE)
      {
         transferToAndNextFootstepsDataVisualizer = new TransferToAndNextFootstepsDataVisualizer(registry, dynamicGraphicObjectsListRegistry);
      }
      else
      {
         transferToAndNextFootstepsDataVisualizer = null;
      }

      if (VISUALIZE)
      {
         YoGraphicPosition dynamicGraphicPositionECMP = new YoGraphicPosition("ecmpviz", ecmpViz, 0.002, YoAppearance.BlueViolet());
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("ecmpviz", dynamicGraphicPositionECMP);
         dynamicGraphicObjectsListRegistry.registerArtifact("ecmpviz", dynamicGraphicPositionECMP.createArtifact());
      }

      // Getting parameters from the icpAndMomentumBasedController
      this.icpAndMomentumBasedController = icpAndMomentumBasedController;

      //    contactStates = momentumBasedController.getContactStates();
      upcomingSupportLeg = momentumBasedController.getUpcomingSupportLeg();
      supportLeg = icpAndMomentumBasedController.getYoSupportLeg();
      capturePoint = icpAndMomentumBasedController.getCapturePoint();
      desiredICP = icpAndMomentumBasedController.getDesiredICP();
      desiredICPVelocity = icpAndMomentumBasedController.getDesiredICPVelocity();
      bipedSupportPolygons = icpAndMomentumBasedController.getBipedSupportPolygons();
      controlledCoMHeightAcceleration = icpAndMomentumBasedController.getControlledCoMHeightAcceleration();
      centerOfMassJacobian = momentumBasedController.getCenterOfMassJacobian();

      coMHeightTimeDerivativesSmoother = new CoMHeightTimeDerivativesSmoother(controlDT, registry);

      // this.finalDesiredICPCalculator = finalDesiredICPCalculator;
      this.centerOfMassHeightTrajectoryGenerator = centerOfMassHeightTrajectoryGenerator;

      this.swingTimeCalculationProvider = swingTimeCalculationProvider;
      this.transferTimeCalculationProvider = transferTimeCalculationProvider;

      this.mapFromFootstepsToTrajectoryParameters = variousWalkingProviders.getMapFromFootstepsToTrajectoryParameters();
      this.footSwitches = momentumBasedController.getFootSwitches();
      this.icpBasedMomentumRateOfChangeControlModule = momentumRateOfChangeControlModule;

      this.instantaneousCapturePointPlanner = instantaneousCapturePointPlanner;

      FootstepProvider footstepProvider = variousWalkingProviders.getFootstepProvider();
      this.upcomingFootstepList = new UpcomingFootstepList(footstepProvider, registry);
      footPoseProvider = variousWalkingProviders.getDesiredFootPoseProvider();

      this.centerOfMassHeightController = new PDController("comHeight", registry);
      double kpCoMHeight = walkingControllerParameters.getKpCoMHeight();
      centerOfMassHeightController.setProportionalGain(kpCoMHeight);
      double zetaCoMHeight = walkingControllerParameters.getZetaCoMHeight();
      centerOfMassHeightController.setDerivativeGain(GainCalculator.computeDerivativeGain(centerOfMassHeightController.getProportionalGain(), zetaCoMHeight));

      String namePrefix = "walking";

      this.stateMachine = new StateMachine<WalkingState>(namePrefix + "State", namePrefix + "SwitchTime", WalkingState.class, yoTime, registry); // this is used by name, and it is ugly.

      this.icpTrajectoryHasBeenInitialized = new BooleanYoVariable("icpTrajectoryHasBeenInitialized", registry);

      rememberFinalICPFromSingleSupport.set(false); // true);
      finalDesiredICPInWorld.set(Double.NaN, Double.NaN);

      coefficientOfFriction.set(0.0); // TODO Remove coefficient of friction from the abstract high level stuff and let the EndEffector controlModule deal with it

      this.centerOfMassHeightTrajectoryGenerator.attachWalkOnToesManager(feetManager.getWalkOnTheEdgesManager());

      pushRecoveryModule = new PushRecoveryControlModule(momentumBasedController, walkingControllerParameters, readyToGrabNextFootstep,
            icpAndMomentumBasedController, stateMachine, registry, swingTimeCalculationProvider, feet);

      setupStateMachine();
      readyToGrabNextFootstep.set(true);

      dwellInSingleSupportDuration.set(0.2);

      maxICPErrorBeforeSingleSupport.set(0.02); // 0.03); // Don't transition to single support until ICP is within 1.5 cm of desired.

      minOrbitalEnergyForSingleSupport.set(0.007); // 0.008
      amountToBeInsideSingleSupport.set(0.0);
      amountToBeInsideDoubleSupport.set(0.03); // 0.02);    // TODO: necessary for stairs...
      transferTimeCalculationProvider.updateTransferTime();

      stopInDoubleSupporTrajectoryTime.set(0.5);

      additionalSwingTimeForICP.set(0.1);
      minimumSwingFraction.set(0.5); // 0.8);

      upcomingSupportLeg.set(RobotSide.RIGHT); // TODO: stairs hack, so that the following lines use the correct leading leg

      controllerInitializationTime = new DoubleYoVariable("controllerInitializationTime", registry);
      alreadyBeenInDoubleSupportOnce = new BooleanYoVariable("alreadyBeenInDoubleSupportOnce", registry);

      // TODO: Fix low level stuff so that we are truly controlling pelvis height and not CoM height.
      controlPelvisHeightInsteadOfCoMHeight.set(true);

      moveICPAwayDuringSwingDistance.set(0.012); // 0.03);
      moveICPAwayAtEndOfSwingDistance.set(0.04); // 0.08);
      singleSupportTimeLeftBeforeShift.set(0.26);
      neutralFootstepsData = null;

      if (DESIREDICP_FROM_POLYGON_COORDINATE)
      {
         doubleSupportDesiredICP = new YoFramePoint2dInPolygonCoordinate("desiredICP", registry);
      }
      else
      {
         doubleSupportDesiredICP = null;
      }

      resetIntegratorsAfterSwing.set(true);
   }

   private void setupStateMachine()
   {
      DoubleSupportState doubleSupportState = new DoubleSupportState(null);

      stateMachine.addState(doubleSupportState);

      ResetICPTrajectoryAction resetICPTrajectoryAction = new ResetICPTrajectoryAction();
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody sameSideFoot = feet.get(robotSide);
         ContactablePlaneBody oppositeSideFoot = feet.get(robotSide.getOppositeSide());

         WalkingState doubleSupportStateEnum = doubleSupportState.getStateEnum();
         WalkingState singleSupportStateEnum = WalkingState.getSingleSupportState(robotSide);
         WalkingState transferStateEnum = WalkingState.getTransferState(robotSide);
         WalkingState oppTransferStateEnum = WalkingState.getTransferState(robotSide.getOppositeSide());

         State<WalkingState> transferState = new DoubleSupportState(robotSide);
         State<WalkingState> singleSupportState = new SingleSupportState(robotSide);

         StopWalkingCondition stopWalkingCondition = new StopWalkingCondition(robotSide);
         DoneWithTransferCondition doneWithTransferCondition = new DoneWithTransferCondition(robotSide);
         SingleSupportToTransferToCondition singleSupportToTransferToOppositeSideCondition = new SingleSupportToTransferToCondition(sameSideFoot);
         SingleSupportToTransferToCondition singleSupportToTransferToSameSideCondition = new SingleSupportToTransferToCondition(oppositeSideFoot);
         StartWalkingCondition startWalkingCondition = new StartWalkingCondition(robotSide);
         IsFallingFromDoubleSupportCondition isFallingFromDoubleSupportCondition = pushRecoveryModule.new IsFallingFromDoubleSupportCondition(robotSide);
         FlamingoStanceCondition flamingoStanceCondition = new FlamingoStanceCondition(robotSide);

         StateTransition<WalkingState> toDoubleSupport = new StateTransition<WalkingState>(doubleSupportStateEnum, stopWalkingCondition,
               resetICPTrajectoryAction);
         StateTransition<WalkingState> toSingleSupport = new StateTransition<WalkingState>(singleSupportStateEnum, doneWithTransferCondition);
         //         StateTransition<WalkingState> toDoubleSupport2 = new StateTransition<WalkingState>(doubleSupportStateEnum, stopWalkingCondition, resetICPTrajectoryAction);
         StateTransition<WalkingState> toTransferOppositeSide = new StateTransition<WalkingState>(oppTransferStateEnum,
               singleSupportToTransferToOppositeSideCondition);
         StateTransition<WalkingState> toTransferSameSide = new StateTransition<WalkingState>(transferStateEnum, singleSupportToTransferToSameSideCondition);
         StateTransition<WalkingState> toTransfer = new StateTransition<WalkingState>(transferStateEnum, startWalkingCondition);
         StateTransition<WalkingState> toFalling = new StateTransition<WalkingState>(singleSupportStateEnum, isFallingFromDoubleSupportCondition);
         StateTransition<WalkingState> toTransfer2 = new StateTransition<WalkingState>(transferStateEnum, flamingoStanceCondition);

         transferState.addStateTransition(toDoubleSupport);
         transferState.addStateTransition(toSingleSupport);
         transferState.addStateTransition(toFalling);
         //         singleSupportState.addStateTransition(toDoubleSupport2);
         singleSupportState.addStateTransition(toTransferOppositeSide);
         singleSupportState.addStateTransition(toTransferSameSide);
         doubleSupportState.addStateTransition(toTransfer);
         doubleSupportState.addStateTransition(toFalling);
         doubleSupportState.addStateTransition(toTransfer2);

         stateMachine.addState(transferState);
         stateMachine.addState(singleSupportState);
      }
   }

   private RigidBody baseForChestOrientationControl;
   private int jacobianForChestOrientationControlId;

   private RigidBody baseForHeadOrientationControl;
   private int jacobianIdForHeadOrientationControl;

   public void setupManagers(VariousWalkingManagers variousWalkingManagers)
   {
      baseForChestOrientationControl = fullRobotModel.getElevator();
      ChestOrientationManager chestOrientationManager = variousWalkingManagers.getChestOrientationManager();
      String[] chestOrientationControlJointNames = walkingControllerParameters.getDefaultChestOrientationControlJointNames();

      if (chestOrientationManager != null)
      {
         jacobianForChestOrientationControlId = chestOrientationManager.createJacobian(fullRobotModel, chestOrientationControlJointNames);
         chestOrientationManager.setUp(baseForChestOrientationControl, jacobianIdForHeadOrientationControl);
      }

      baseForHeadOrientationControl = fullRobotModel.getElevator();
      HeadOrientationManager headOrientationManager = variousWalkingManagers.getHeadOrientationManager();
      String[] headOrientationControlJointNames = walkingControllerParameters.getDefaultHeadOrientationControlJointNames();

      if (headOrientationManager != null)
      {
         jacobianIdForHeadOrientationControl = headOrientationManager.createJacobian(headOrientationControlJointNames);
         headOrientationManager.setUp(baseForHeadOrientationControl, jacobianIdForHeadOrientationControl);
      }
   }

   public void initialize()
   {
      super.initialize();

      momentumBasedController.setMomentumControlModuleToUse(MOMENTUM_CONTROL_MODULE_TO_USE);

      initializeContacts();

      ChestOrientationManager chestOrientationManager = variousWalkingManagers.getChestOrientationManager();
      if (chestOrientationManager != null)
      {
         chestOrientationManager.setUp(baseForChestOrientationControl, jacobianForChestOrientationControlId);
      }

      HeadOrientationManager headOrientationManager = variousWalkingManagers.getHeadOrientationManager();
      if (headOrientationManager != null)
      {
         headOrientationManager.setUp(baseForHeadOrientationControl, jacobianIdForHeadOrientationControl);
      }

      pelvisOrientationManager.setToZeroInSupportFoot(upcomingSupportLeg.getEnumValue());

      icpAndMomentumBasedController.computeCapturePoint();
      desiredICP.set(capturePoint.getFramePoint2dCopy());

      stateMachine.setCurrentState(WalkingState.DOUBLE_SUPPORT);

   }

   private void initializeContacts()
   {
      momentumBasedController.clearContacts();

      for (RobotSide robotSide : RobotSide.values)
      {
         feetManager.setFlatFootContactState(robotSide);
      }
   }

   private EnumYoVariable<RobotSide> trailingLeg = new EnumYoVariable<RobotSide>("trailingLeg", "", registry, RobotSide.class, true);

   private class DoubleSupportState extends State<WalkingState>
   {
      private final RobotSide transferToSide;
      private final FramePoint2d desiredICPLocal = new FramePoint2d();
      private final FrameVector2d desiredICPVelocityLocal = new FrameVector2d();
      private final FramePoint2d ecmpLocal = new FramePoint2d();
      private final FramePoint2d capturePoint2d = new FramePoint2d();
      private final FramePoint2d stanceFootLocation = new FramePoint2d();

      public DoubleSupportState(RobotSide transferToSide)
      {
         super((transferToSide == null) ? WalkingState.DOUBLE_SUPPORT : WalkingState.getTransferState(transferToSide));
         this.transferToSide = transferToSide;
      }

      @Override
      public void doAction()
      {
         doNotIntegrateAnkleAccelerations();

         feetManager.updateContactStatesInDoubleSupport(transferToSide);

         // note: this has to be done before the ICP trajectory generator is initialized, since it is using nextFootstep
         // TODO: Make a LOADING state and clean all of these timing hacks up.
         doneFinishingSingleSupportTransfer.set(instantaneousCapturePointPlanner.isPerformingICPDoubleSupport());
         double estimatedTimeRemainingForState = instantaneousCapturePointPlanner.getEstimatedTimeRemainingForState(yoTime.getDoubleValue());

         if (doneFinishingSingleSupportTransfer.getBooleanValue() || (estimatedTimeRemainingForState < 0.02))
         {
            upcomingFootstepList.checkForFootsteps(readyToGrabNextFootstep, upcomingSupportLeg, feet);
         }

         initializeICPPlannerIfNecessary();

         desiredICPLocal.setToZero(desiredICP.getReferenceFrame());
         desiredICPVelocityLocal.setToZero(desiredICPVelocity.getReferenceFrame());
         ecmpLocal.setToZero(worldFrame);
         capturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);

         instantaneousCapturePointPlanner.getICPPositionAndVelocity(desiredICPLocal, desiredICPVelocityLocal, ecmpLocal, capturePoint2d,
               yoTime.getDoubleValue());

         if (transferToSide != null)
         {
            stanceFootLocation.setToZero(referenceFrames.getAnkleZUpFrame(transferToSide));
            moveICPToInsideOfFootAtEndOfSwing(transferToSide.getOppositeSide(), stanceFootLocation, swingTimeCalculationProvider.getValue(), 0.0,
                  desiredICPLocal);
         }
         else
         {
            RobotSide previousSupport = previousSupportSide.getEnumValue();
            if (previousSupport != null)
            {
               stanceFootLocation.setToZero(referenceFrames.getAnkleZUpFrame(previousSupport.getOppositeSide()));
               moveICPToInsideOfFootAtEndOfSwing(previousSupport, stanceFootLocation, swingTimeCalculationProvider.getValue(), 0.0, desiredICPLocal);
            }

            desiredICPLocal.changeFrame(referenceFrames.getMidFeetZUpFrame());
            desiredICPLocal.setX(desiredICPLocal.getX() + icpStandOffsetX.getDoubleValue());
            desiredICPLocal.setY(desiredICPLocal.getY() + icpStandOffsetY.getDoubleValue());

            FrameConvexPolygon2d supportPolygonInMidFeetZUp = bipedSupportPolygons.getSupportPolygonInMidFeetZUp();
            supportPolygonInMidFeetZUp.orthogonalProjection(desiredICPLocal);
            supportPolygonInMidFeetZUp.pullPointTowardsCentroid(desiredICPLocal, 0.10);

            desiredICPLocal.changeFrame(worldFrame);
         }

         desiredICP.set(desiredICPLocal);
         desiredICPVelocity.set(desiredICPVelocityLocal);

         desiredECMP.set(ecmpLocal);

         if (VISUALIZE)
         {
            ecmpViz.set(desiredECMP.getX(), desiredECMP.getY(), 0.0);
         }

         initializeECMPbasedToeOffIfNotInitializedYet();
      }

      boolean initializedAtStart = false;

      public void initializeICPPlannerIfNecessary()
      {
         if (!icpTrajectoryHasBeenInitialized.getBooleanValue() && instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()))
         {
            initializingICPTrajectory.set(true);

            Pair<FramePoint2d, Double> finalDesiredICPAndTrajectoryTime = computeFinalDesiredICPAndTrajectoryTime();

            if (transferToSide != null) // the only case left for determining the contact state of the trailing foot
            {
               FramePoint2d finalDesiredICP = finalDesiredICPAndTrajectoryTime.first();
               finalDesiredICP.changeFrame(desiredICP.getReferenceFrame());

               RobotSide trailingLeg = transferToSide.getOppositeSide();
               feetManager.requestToeOffBasedOnICP(trailingLeg, desiredICP.getFramePoint2dCopy(), finalDesiredICP);
            }
            else if (!initializedAtStart)
            {
               FramePoint2d finalDesiredICP = finalDesiredICPAndTrajectoryTime.first();
               finalDesiredICP.changeFrame(desiredICP.getReferenceFrame());

               desiredICP.set(finalDesiredICP);

               TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForDoubleSupport(RobotSide.LEFT, true);
               instantaneousCapturePointPlanner.initializeDoubleSupport(transferToAndNextFootstepsData, 0.1);

               neutralFootstepsData = createTransferToAndNextFootstepDataForDoubleSupport(RobotSide.LEFT, true);
               neutralFootstepsData.setTransferToSide(null);

               initializedAtStart = true;
            }

            icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons); // need to always update biped support polygons after a change to the contact states
            icpTrajectoryHasBeenInitialized.set(true);
         }
         else
         {
            initializingICPTrajectory.set(false);
         }
      }

      private final FramePoint2d desiredCMP = new FramePoint2d();

      public void initializeECMPbasedToeOffIfNotInitializedYet()
      {
         // the only case left for determining the contact state of the trailing foot
         if (!ecmpBasedToeOffHasBeenInitialized.getBooleanValue() && transferToSide != null)
         {
            RobotSide trailingLeg = transferToSide.getOppositeSide();
            icpBasedMomentumRateOfChangeControlModule.getDesiredCMP(desiredCMP);
            feetManager.requestToeOffBasedOnECMP(trailingLeg, desiredCMP, desiredICP.getFramePoint2dCopy(), capturePoint2d);

            if (feetManager.doToeOff())
            {
               icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons); // need to always update biped support polygons after a change to the contact states
               ecmpBasedToeOffHasBeenInitialized.set(true);
            }
         }
      }

      private Pair<FramePoint2d, Double> computeFinalDesiredICPAndTrajectoryTime()
      {
         Pair<FramePoint2d, Double> finalDesiredICPAndTrajectoryTime;

         if (transferToSide == null)
         {
            FramePoint2d finalDesiredICP = getDoubleSupportFinalDesiredICPForDoubleSupportStance();
            double trajectoryTime = stopInDoubleSupporTrajectoryTime.getDoubleValue();

            finalDesiredICPInWorld.set(Double.NaN, Double.NaN);

            finalDesiredICPAndTrajectoryTime = new Pair<FramePoint2d, Double>(finalDesiredICP, trajectoryTime);
         }

         else if (rememberFinalICPFromSingleSupport.getBooleanValue() && !finalDesiredICPInWorld.containsNaN())
         {
            FramePoint2d finalDesiredICP = finalDesiredICPInWorld.getFramePoint2dCopy();
            double trajectoryTime = transferTimeCalculationProvider.getValue();

            finalDesiredICPAndTrajectoryTime = new Pair<FramePoint2d, Double>(finalDesiredICP, trajectoryTime);
         }

         else
         {
            boolean inInitialize = false;
            TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForDoubleSupport(transferToSide, inInitialize);
            if (footPoseProvider != null && footPoseProvider.checkForNewPose() != null)
               transferToAndNextFootstepsData.setNextFootstep(createFootstepAtCurrentLocation(transferToSide));

            instantaneousCapturePointPlanner.initializeDoubleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue());

            FramePoint2d finalDesiredICP = instantaneousCapturePointPlanner.getFinalDesiredICP();
            double trajectoryTime = transferTimeCalculationProvider.getValue();

            FramePoint2d temp = new FramePoint2d(finalDesiredICP);
            temp.changeFrame(worldFrame);
            finalDesiredICPInWorld.set(temp);
            finalDesiredICPAndTrajectoryTime = new Pair<FramePoint2d, Double>(finalDesiredICP, trajectoryTime);
         }

         return finalDesiredICPAndTrajectoryTime;
      }

      public TransferToAndNextFootstepsData createTransferToAndNextFootstepDataForDoubleSupport(RobotSide transferToSide, boolean inInitialize)
      {
         Footstep transferFromFootstep = createFootstepAtCurrentLocation(transferToSide.getOppositeSide());
         Footstep transferToFootstep = createFootstepAtCurrentLocation(transferToSide);

         FrameConvexPolygon2d transferToFootPolygon = computeFootPolygon(transferToSide, referenceFrames.getSoleFrame(transferToSide));

         Footstep nextFootstep, nextNextFootstep;

         if (inInitialize)
         {
            // Haven't popped the footstep off yet...
            nextFootstep = upcomingFootstepList.getNextNextFootstep();
            nextNextFootstep = upcomingFootstepList.getNextNextNextFootstep();
         }
         else
         {
            nextFootstep = upcomingFootstepList.getNextFootstep();
            nextNextFootstep = upcomingFootstepList.getNextNextFootstep();
         }

         double timeAllottedForSingleSupportForICP = swingTimeCalculationProvider.getValue() + additionalSwingTimeForICP.getDoubleValue();

         TransferToAndNextFootstepsData transferToAndNextFootstepsData = new TransferToAndNextFootstepsData();
         transferToAndNextFootstepsData.setTransferFromFootstep(transferFromFootstep);
         transferToAndNextFootstepsData.setTransferToFootstep(transferToFootstep);
         transferToAndNextFootstepsData.setTransferToFootPolygonInSoleFrame(transferToFootPolygon);
         transferToAndNextFootstepsData.setTransferToSide(transferToSide);
         transferToAndNextFootstepsData.setNextFootstep(nextFootstep);
         transferToAndNextFootstepsData.setNextNextFootstep(nextNextFootstep);
         transferToAndNextFootstepsData.setEstimatedStepTime(timeAllottedForSingleSupportForICP + transferTimeCalculationProvider.getValue());
         transferToAndNextFootstepsData.setW0(icpAndMomentumBasedController.getOmega0());
         transferToAndNextFootstepsData.setDoubleSupportDuration(transferTimeCalculationProvider.getValue());
         transferToAndNextFootstepsData.setSingleSupportDuration(timeAllottedForSingleSupportForICP);
         double doubleSupportInitialTransferDuration = 0.4; // TODO: Magic Number
         transferToAndNextFootstepsData.setDoubleSupportInitialTransferDuration(doubleSupportInitialTransferDuration);
         boolean stopIfReachedEnd = (upcomingFootstepList.getNumberOfFootstepsToProvide() <= 3); // TODO: Magic Number
         transferToAndNextFootstepsData.setStopIfReachedEnd(stopIfReachedEnd);
         transferToAndNextFootstepsData.setCurrentDesiredICP(desiredICP.getFramePoint2dCopy(), desiredICPVelocity.getFrameVector2dCopy());

         if (VISUALIZE)
         {
            transferToAndNextFootstepsDataVisualizer.visualizeFootsteps(transferToAndNextFootstepsData);
         }

         return transferToAndNextFootstepsData;
      }

      @Override
      public void doTransitionIntoAction()
      {
         icpStandOffsetX.set(0.0);
         icpStandOffsetY.set(0.0);

         desiredECMPinSupportPolygon.set(false);
         ecmpBasedToeOffHasBeenInitialized.set(false);
         trailingLeg.set(transferToSide); // FIXME

         icpTrajectoryHasBeenInitialized.set(false);
         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: enteringDoubleSupportState");
         supportLeg.set(null); // TODO: check if necessary

         feetManager.initializeContactStatesForDoubleSupport(transferToSide);

         if (transferToSide != null)
         {
            if (!instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()))
            {
               Footstep transferToFootstep = createFootstepAtCurrentLocation(transferToSide);
               TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(transferToFootstep,
                     transferToSide);

               instantaneousCapturePointPlanner.reInitializeSingleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue());
            }
         }
         else
         {
            // Do something smart here when going to DoubleSupport state.
            //            instantaneousCapturePointPlanner.initializeForStoppingInDoubleSupport(yoTime.getDoubleValue());
         }

         icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons); // need to always update biped support polygons after a change to the contact states

         if (DESIREDICP_FROM_POLYGON_COORDINATE)
         {
            doubleSupportDesiredICP.updatePointAndPolygon(bipedSupportPolygons.getSupportPolygonInMidFeetZUp(), desiredICP.getFramePoint2dCopy());
         }

         RobotSide transferToSideToUseInFootstepData = transferToSide;
         if (transferToSideToUseInFootstepData == null)
            transferToSideToUseInFootstepData = RobotSide.LEFT; // Arbitrary here.

         if (!centerOfMassHeightTrajectoryGenerator.hasBeenInitializedWithNextStep())
         {
            boolean inInitialize = true;
            TransferToAndNextFootstepsData transferToAndNextFootstepsDataForDoubleSupport = createTransferToAndNextFootstepDataForDoubleSupport(
                  transferToSideToUseInFootstepData, inInitialize);

            centerOfMassHeightTrajectoryGenerator.initialize(transferToAndNextFootstepsDataForDoubleSupport,
                  transferToAndNextFootstepsDataForDoubleSupport.getTransferToSide(), null, getContactStatesList());
         }

         if (pushRecoveryModule.isEnabled())
         {
            pushRecoveryModule.setRecoverFromDoubleSupportFootStep(null);
            pushRecoveryModule.setRecoveringFromDoubleSupportState(false);
         }
         
         pelvisOrientationManager.setToHoldCurrentDesired();
      }

      @Override
      public void doTransitionOutOfAction()
      {
         icpStandOffsetX.set(0.0);
         icpStandOffsetY.set(0.0);

         desiredECMPinSupportPolygon.set(false);
         feetManager.reset();
         ecmpBasedToeOffHasBeenInitialized.set(false);

         alreadyBeenInDoubleSupportOnce.set(true);

         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: leavingDoubleSupportState");

         desiredICPVelocity.set(0.0, 0.0);

         if (manipulationControlModule != null && doPrepareManipulationForLocomotion.getBooleanValue())
            manipulationControlModule.prepareForLocomotion();
      }
   }

   private class SingleSupportState extends State<WalkingState>
   {
      private final RobotSide swingSide;
      private final FrameOrientation desiredPelvisOrientationToPack;
      private final FrameVector desiredPelvisAngularVelocityToPack;
      private final FrameVector desiredPelvisAngularAccelerationToPack;

      private final FramePoint2d desiredICPLocal = new FramePoint2d();
      private final FrameVector2d desiredICPVelocityLocal = new FrameVector2d();
      private final FramePoint2d ecmpLocal = new FramePoint2d();
      private final FramePoint2d capturePoint2d = new FramePoint2d();

      private Footstep nextFootstep;
      private double captureTime;

      public SingleSupportState(RobotSide supportSide)
      {
         super(WalkingState.getSingleSupportState(supportSide));
         this.swingSide = supportSide.getOppositeSide();
         this.desiredPelvisOrientationToPack = new FrameOrientation(worldFrame);
         this.desiredPelvisAngularVelocityToPack = new FrameVector(fullRobotModel.getRootJoint().getFrameAfterJoint());
         this.desiredPelvisAngularAccelerationToPack = new FrameVector(fullRobotModel.getRootJoint().getFrameAfterJoint());
      }

      @Override
      public void doAction()
      {
         integrateAnkleAccelerationsOnSwingLeg(swingSide);

         desiredICPLocal.setToZero(desiredICP.getReferenceFrame());
         desiredICPVelocityLocal.setToZero(desiredICPVelocity.getReferenceFrame());
         ecmpLocal.setToZero(worldFrame);

         capturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);

         instantaneousCapturePointPlanner.getICPPositionAndVelocity(desiredICPLocal, desiredICPVelocityLocal, ecmpLocal, capturePoint2d,
               yoTime.getDoubleValue() + icpProjectionTimeOffset.getDoubleValue());

         if (pushRecoveryModule.usePushRecoveryICPPlanner() && pushRecoveryModule.isRecovering())
         {
            pushRecoveryModule.getICPPlanner().compute(stateMachine.timeInCurrentState() - captureTime);
            pushRecoveryModule.getICPPlanner().getICPPosition(desiredICPLocal, capturePoint2d);
            pushRecoveryModule.getICPPlanner().getICPVelocity(desiredICPVelocityLocal);
         }

         if (isInFlamingoStance.getBooleanValue() && footPoseProvider.checkForNewPose(swingSide))
            feetManager.requestMoveStraight(swingSide, footPoseProvider.getDesiredFootPose(swingSide));

         RobotSide supportSide = swingSide.getOppositeSide();
         double swingTimeRemaining = swingTimeCalculationProvider.getValue() - stateMachine.timeInCurrentState();
         FramePoint2d transferToFootstepLocation = transferToFootstep.getFramePoint2dCopy();
         FrameConvexPolygon2d footPolygon = computeFootPolygon(supportSide, referenceFrames.getAnkleZUpFrame(supportSide));
         double omega0 = icpAndMomentumBasedController.getOmega0();

         if (pushRecoveryModule.isEnabled())
         {
            boolean footstepHasBeenAdjusted = pushRecoveryModule.checkAndUpdateFootstep(swingSide, swingTimeRemaining, capturePoint2d, nextFootstep, omega0,
                  footPolygon);

            if (footstepHasBeenAdjusted)
            {
               if (pushRecoveryModule.isRecoveringFromDoubleSupportFall())
               {
                  neutralFootstepsData.setTransferToSide(swingSide.getOppositeSide());
                  instantaneousCapturePointPlanner.initializeDoubleSupport(neutralFootstepsData, yoTime.getDoubleValue());
               }

               updateFootstepParameters();

               captureTime = stateMachine.timeInCurrentState();
               feetManager.replanSwingTrajectory(swingSide, nextFootstep, swingTimeCalculationProvider.getValue() - captureTime,
                     pushRecoveryModule.isRecoveringFromDoubleSupportFall());

               TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(nextFootstep, swingSide);
               instantaneousCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue() - captureTime);

               if (pushRecoveryModule.usePushRecoveryICPPlanner())
               {
                  // initialize the push recovery ICP planner getting initial and final points from the main ICP planner
                  FramePoint2d initialICP = new FramePoint2d(worldFrame);
                  FramePoint2d constantCenterOfPressure = instantaneousCapturePointPlanner.getConstantCenterOfPressure();
                  FramePoint2d finalDesiredICP = instantaneousCapturePointPlanner.getFinalDesiredICP();
                  FramePoint2d startICP = instantaneousCapturePointPlanner.getSingleSupportStartICP();
                  icpProjectionTimeOffset.set(pushRecoveryModule.computeTimeToProjectDesiredICPToClosestPointOnTrajectoryToActualICP(capturePoint2d,
                        constantCenterOfPressure, startICP, finalDesiredICP, omega0) - captureTime);
                  instantaneousCapturePointPlanner.getICPPositionAndVelocity(initialICP, desiredICPVelocityLocal, ecmpLocal, capturePoint2d,
                        yoTime.getDoubleValue() + icpProjectionTimeOffset.getDoubleValue());
                  pushRecoveryModule.setSwingTimeRemaining(swingTimeRemaining);
                  pushRecoveryModule.setInitialDesiredICP(initialICP);
                  pushRecoveryModule.setFinalDesiredICP(finalDesiredICP);
                  pushRecoveryModule.getICPPlanner().initialize(desiredICPLocal, capturePoint2d);
                  pushRecoveryModule.getICPPlanner().compute(stateMachine.timeInCurrentState() - captureTime);
                  pushRecoveryModule.getICPPlanner().getICPPosition(desiredICPLocal, capturePoint2d);
                  pushRecoveryModule.getICPPlanner().getICPVelocity(desiredICPVelocityLocal);
               }

               removeAllUpcomingFootstepsAndStand();
            }

            if (pushRecoveryModule.isRecovering())
            {
               desiredICPVelocityLocal.setToZero();
            }
         }

         if (!isInFlamingoStance.getBooleanValue())
                        moveICPToInsideOfFootAtEndOfSwing(supportSide, transferToFootstepLocation, swingTimeCalculationProvider.getValue(), swingTimeRemaining, desiredICPLocal);

            desiredICP.set(desiredICPLocal);
         if (isInFlamingoStance.getBooleanValue())
            desiredICP.add(icpStandOffsetX.getDoubleValue(), icpStandOffsetY.getDoubleValue());
         desiredICPVelocity.set(desiredICPVelocityLocal);

         desiredECMP.set(ecmpLocal);

         if (VISUALIZE)
         {
            ecmpViz.set(desiredECMP.getX(), desiredECMP.getY(), 0.0);
         }

         if ((stateMachine.timeInCurrentState() - captureTime < 0.5 * swingTimeCalculationProvider.getValue())
               && feetManager.isInSingularityNeighborhood(swingSide))
         {
            feetManager.doSingularityEscape(swingSide);
         }
      }

      // once the footsteps are refactored this should be rewritten to remove all upcoming footsteps and add
      // a footstep that brings the robot in a standing position.
      private void removeAllUpcomingFootstepsAndStand()
      {
         int numberOfUpcomingFootsteps = upcomingFootstepList.getNumberOfFootstepsToProvide();

         // horrible hack that does not remove footsteps if we are sending them every tick like in FlatGroundWalking.
         // Removing this should neither break Bamboo nor Atlas but it messes up the PushRecoveryWalkingTrack.
         if (numberOfUpcomingFootsteps > 1000)
            return;

         while (numberOfUpcomingFootsteps > 0)
         {
            readyToGrabNextFootstep.set(true);
            upcomingFootstepList.checkForFootsteps(readyToGrabNextFootstep, upcomingSupportLeg, feet);
            upcomingFootstepList.notifyComplete();
            numberOfUpcomingFootsteps--;
         }

         FramePoint2d neutralPosition = new FramePoint2d();
         nextFootstep.getPosition2d(neutralPosition);
         double y = swingSide == RobotSide.LEFT ? -0.3 : 0.3;
         FrameVector2d translate = new FrameVector2d(neutralPosition.getReferenceFrame(), 0.0, y);
         neutralPosition.add(translate);

         Footstep next = upcomingFootstepList.getNextFootstep();
         if (next != null)
         {
            next.setPositionChangeOnlyXY(neutralPosition);
         }
      }

      @Override
      public void doTransitionIntoAction()
      {
         captureTime = 0.0;
         hasICPPlannerFinished.set(false);
         trailingLeg.set(null);

         footSwitches.get(swingSide).reset();

         if (pushRecoveryModule.isEnabled() && pushRecoveryModule.isRecoveringFromDoubleSupportFall())
         {
            nextFootstep = pushRecoveryModule.getRecoverFromDoubleSupportFootStep();
         }
         else
         {
            nextFootstep = upcomingFootstepList.getNextFootstep();
            swingTimeCalculationProvider.updateSwingTime();
         }

         if (nextFootstep != null)
            feetManager.requestSwing(swingSide, nextFootstep, mapFromFootstepsToTrajectoryParameters.get(nextFootstep));
         else if (footPoseProvider != null && footPoseProvider.checkForNewPose(swingSide))
         {
            FramePose nextFootPose = footPoseProvider.getDesiredFootPose(swingSide);
            feetManager.requestMoveStraight(swingSide, nextFootPose);
            icpStandOffsetX.set(0.0);
            icpStandOffsetY.set(0.0);
            isInFlamingoStance.set(true);
         }

         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: enteringSingleSupportState");
         RobotSide supportSide = swingSide.getOppositeSide();

         supportLeg.set(supportSide);

         transferTimeCalculationProvider.updateTransferTime();

         if (nextFootstep != null)
         {
            updateFootstepParameters();

            TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(nextFootstep, swingSide);
            instantaneousCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue());
         }
         else
         {
            pelvisOrientationManager.setToZeroInSupportFoot(supportSide);
         }

         if (walkingControllerParameters.resetDesiredICPToCurrentAtStartOfSwing())
         {
            desiredICP.set(capturePoint.getFramePoint2dCopy()); // TODO: currently necessary for stairs because of the omega0 jump, but should get rid of this
         }
      }

      private void updateFootstepParameters()
      {
         FramePoint2d nextFootstepPosition = new FramePoint2d();
         nextFootstep.getPosition2d(nextFootstepPosition);
         transferToFootstep.set(nextFootstepPosition);
         RobotSide supportSide = swingSide.getOppositeSide();
         
         pelvisOrientationManager.setWithUpcomingFootstep(nextFootstep, swingSide);

         FramePoint centerOfMass = new FramePoint(referenceFrames.getCenterOfMassFrame());
         centerOfMass.changeFrame(worldFrame);
         //ContactablePlaneBody supportFoot = feet.get(supportSide);
         //Transform3D supportFootToWorldTransform = footToWorldTransform.get(supportSide);
         //double footHeight = DesiredFootstepCalculatorTools.computeMinZPointInFrame(supportFootToWorldTransform, supportFoot, worldFrame).getZ();
         //double comHeight = centerOfMass.getZ() - footHeight;
         icpAndMomentumBasedController.computeCapturePoint();

         TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(nextFootstep, swingSide);
         //FramePoint2d finalDesiredICP = getSingleSupportFinalDesiredICPForWalking(transferToAndNextFootstepsData, swingSide);

         supportLeg.set(supportSide);
         icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons);

         // Shouldn't have to do this init anymore since it's done above...
         // icpTrajectoryGenerator.initialize(desiredICP.getFramePoint2dCopy(), finalDesiredICP, swingTimeCalculationProvider.getValue(), omega0,
         // amountToBeInsideSingleSupport.getDoubleValue(), getSupportLeg(), yoTime.getDoubleValue());

         centerOfMassHeightTrajectoryGenerator.initialize(transferToAndNextFootstepsData, supportLeg.getEnumValue(), nextFootstep, getContactStatesList());

         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: nextFootstep will change now!");
         readyToGrabNextFootstep.set(true);

         //       instantaneousCapturePointPlanner.setDoHeelToToeTransfer(walkOnTheEdgesManager.willDoToeOff(transferToAndNextFootstepsData));
      }

      @Override
      public void doTransitionOutOfAction()
      {
         if (DEBUG)
            System.out.println("WalkingHighLevelController: leavingDoubleSupportState");

         if (!isInFlamingoStance.getBooleanValue())
            upcomingFootstepList.notifyComplete();
         isInFlamingoStance.set(false);

         if (pushRecoveryModule.isEnabled())
         {
            if (pushRecoveryModule.usePushRecoveryICPPlanner() && pushRecoveryModule.isRecovering())
            {
               instantaneousCapturePointPlanner.reset(yoTime.getDoubleValue() - (icpProjectionTimeOffset.getDoubleValue() + captureTime));
            }

            captureTime = 0.0;
            pushRecoveryModule.reset();
            icpProjectionTimeOffset.set(0.0);
         }

         previousSupportSide.set(swingSide.getOppositeSide());

         resetLoadedLegIntegrators(swingSide);
      }
   }

   public class StartWalkingCondition implements StateTransitionCondition
   {
      private final RobotSide transferToSide;

      public StartWalkingCondition(RobotSide robotSide)
      {
         this.transferToSide = robotSide;
      }

      public boolean checkCondition()
      {
         if (readyToGrabNextFootstep.getBooleanValue())
            return false;

         boolean doubleSupportTimeHasPassed = stateMachine.timeInCurrentState() > transferTimeCalculationProvider.getValue();
         boolean transferringToThisRobotSide = transferToSide == upcomingSupportLeg.getEnumValue();

         return transferringToThisRobotSide && doubleSupportTimeHasPassed;
      }
   }

   public class FlamingoStanceCondition implements StateTransitionCondition
   {
      private final RobotSide transferToSide;

      public FlamingoStanceCondition(RobotSide robotSide)
      {
         this.transferToSide = robotSide;
      }

      public boolean checkCondition()
      {
         if (!readyToGrabNextFootstep.getBooleanValue())
            return false;

         boolean doubleSupportTimeHasPassed = stateMachine.timeInCurrentState() > transferTimeCalculationProvider.getValue();
         boolean transferringToThisRobotSide = footPoseProvider != null && footPoseProvider.checkForNewPose(transferToSide.getOppositeSide());

         if (transferringToThisRobotSide && doubleSupportTimeHasPassed)
            upcomingSupportLeg.set(transferToSide);

         return transferringToThisRobotSide && doubleSupportTimeHasPassed;
      }
   }

   public class DoneWithTransferCondition implements StateTransitionCondition
   {
      private final RobotSide robotSide;
      private final FramePoint2d capturePoint2d = new FramePoint2d();
      private final FramePoint2d desiredICP2d = new FramePoint2d();

      public DoneWithTransferCondition(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }

      public boolean checkCondition()
      {
         if (stayInTransferWalkingState.getBooleanValue())
            return false;

         if (!feetManager.isEdgeTouchDownDone(robotSide))
            return false;

         if (walkingControllerParameters.checkOrbitalEnergyCondition())
         {
            // TODO: not really nice, but it'll do:
            FlatThenPolynomialCoMHeightTrajectoryGenerator flatThenPolynomialCoMHeightTrajectoryGenerator = (FlatThenPolynomialCoMHeightTrajectoryGenerator) centerOfMassHeightTrajectoryGenerator;
            double orbitalEnergy = flatThenPolynomialCoMHeightTrajectoryGenerator.computeOrbitalEnergyIfInitializedNow(upcomingSupportLeg.getEnumValue());

            // return transferICPTrajectoryDone.getBooleanValue() && orbitalEnergy > minOrbitalEnergy;
            return icpTrajectoryHasBeenInitialized.getBooleanValue() && (orbitalEnergy > minOrbitalEnergyForSingleSupport.getDoubleValue());
         }
         else
         {
            boolean icpTrajectoryIsDone = icpTrajectoryHasBeenInitialized.getBooleanValue() && instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue());
            if (!icpTrajectoryIsDone)
               return false;

            capturePoint.getFrameTuple2dIncludingFrame(capturePoint2d);
            desiredICP.getFrameTuple2dIncludingFrame(desiredICP2d);

            double distanceFromDesiredToActual = capturePoint2d.distance(desiredICP2d);
            boolean closeEnough = distanceFromDesiredToActual < maxICPErrorBeforeSingleSupport.getDoubleValue();

            return closeEnough;
         }
      }
   }

   private class SingleSupportToTransferToCondition extends DoneWithSingleSupportCondition
   {
      private final ContactablePlaneBody nextSwingFoot;

      public SingleSupportToTransferToCondition(ContactablePlaneBody nextSwingFoot)
      {
         super();

         this.nextSwingFoot = nextSwingFoot;
      }

      public boolean checkCondition()
      {
         Footstep nextFootstep = upcomingFootstepList.getNextNextFootstep();
         if (nextFootstep == null)
            return super.checkCondition();

         ContactablePlaneBody nextSwingFoot = nextFootstep.getBody();
         if (this.nextSwingFoot != nextSwingFoot)
            return false;

         boolean condition = super.checkCondition();

         return condition;
      }
   }

   private class DoneWithSingleSupportCondition implements StateTransitionCondition
   {
      private boolean footSwitchActivated;

      public DoneWithSingleSupportCondition()
      {
      }

      public boolean checkCondition()
      {
         RobotSide swingSide = supportLeg.getEnumValue().getOppositeSide();
         hasMinimumTimePassed.set(hasMinimumTimePassed());

         if (!hasICPPlannerFinished.getBooleanValue())
         {
            hasICPPlannerFinished.set(instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()));

            if (hasICPPlannerFinished.getBooleanValue())
            {
               timeThatICPPlannerFinished.set(yoTime.getDoubleValue());
            }
         }

         FootSwitchInterface footSwitch = footSwitches.get(swingSide);

         if (feetManager.willLandOnToes())
         {
            if (!(footSwitch instanceof ToeSwitch))
            {
               throw new RuntimeException("toe touchdown should not be used if Robot is not using a ToeSwitch.");
            }

            ToeSwitch toeSwitch = (ToeSwitch) footSwitch;
            footSwitchActivated = toeSwitch.hasToeHitGround();
         }
         else if (feetManager.willLandOnHeel())
         {
            if (!(footSwitch instanceof HeelSwitch))
            {
               throw new RuntimeException("landOnHeels should not be set to true if Robot is not using a HeelSwitch.");
            }

            HeelSwitch heelSwitch = (HeelSwitch) footSwitch;
            footSwitchActivated = heelSwitch.hasHeelHitGround();
         }
         else
         {
            footSwitchActivated = footSwitch.hasFootHitGround();
         }

         if (hasMinimumTimePassed.getBooleanValue() && justFall.getBooleanValue())
            return true;

         // Just switch states if icp is done, plus a little bit more. You had enough time and more isn't going to do any good.

         if (pushRecoveryModule.isEnabled() && pushRecoveryModule.isRecoveringFromDoubleSupportFall())
         {
            if (stateMachine.timeInCurrentState() > pushRecoveryModule.getTrustTimeToConsiderSwingFinished())
               return true;
         }

         if (DO_TRANSITION_WHEN_TIME_IS_UP)
         {
            if (hasICPPlannerFinished.getBooleanValue()
                  && (yoTime.getDoubleValue() > timeThatICPPlannerFinished.getDoubleValue() + dwellInSingleSupportDuration.getDoubleValue()))
               return true;
         }

         if (walkingControllerParameters.finishSwingWhenTrajectoryDone())
         {
            return hasMinimumTimePassed.getBooleanValue() && (hasICPPlannerFinished.getBooleanValue() || footSwitchActivated);
         }
         else
         {
            return hasMinimumTimePassed.getBooleanValue() && footSwitchActivated;
         }
      }

      private boolean hasMinimumTimePassed()
      {
         double minimumSwingTime = swingTimeCalculationProvider.getValue() * minimumSwingFraction.getDoubleValue();

         return stateMachine.timeInCurrentState() > minimumSwingTime;
      }
   }

   private class StopWalkingCondition extends DoneWithSingleSupportCondition
   {
      private final RobotSide robotSide;

      public StopWalkingCondition(RobotSide robotSide)
      {
         super();

         this.robotSide = robotSide;
      }

      public boolean checkCondition()
      {
         boolean isNextFootstepNull = upcomingFootstepList.getNextFootstep() == null;
         boolean isSupportLegNull = supportLeg.getEnumValue() == null;
         boolean noMoreFootsteps = upcomingFootstepList.isFootstepProviderEmpty() && isNextFootstepNull;
         boolean noMoreFootPoses = footPoseProvider == null || !footPoseProvider.checkForNewPose(robotSide.getOppositeSide());
         boolean readyToStopWalking = noMoreFootsteps && noMoreFootPoses && (isSupportLegNull || super.checkCondition());
         return readyToStopWalking;
      }
   }

   public class ResetICPTrajectoryAction implements StateTransitionAction
   {
      public void doTransitionAction()
      {
         instantaneousCapturePointPlanner.reset(yoTime.getDoubleValue());
      }
   }

   private FramePoint2d getDoubleSupportFinalDesiredICPForDoubleSupportStance()
   {
      FramePoint2d ret = new FramePoint2d(worldFrame);
      double trailingFootToLeadingFootFactor = 0.5; // 0.25;
      for (RobotSide robotSide : RobotSide.values)
      {
         FramePoint2d centroid = new FramePoint2d(ret.getReferenceFrame());
         FrameConvexPolygon2d footPolygon = computeFootPolygon(robotSide, referenceFrames.getAnkleZUpFrame(robotSide));
         footPolygon.getCentroid(centroid);
         centroid.changeFrame(ret.getReferenceFrame());
         if (robotSide == upcomingSupportLeg.getEnumValue())
            centroid.scale(trailingFootToLeadingFootFactor);
         else
            centroid.scale(1.0 - trailingFootToLeadingFootFactor);
         ret.add(centroid);
      }

      return ret;
   }

   public TransferToAndNextFootstepsData createTransferToAndNextFootstepDataForSingleSupport(Footstep transferToFootstep, RobotSide swingSide)
   {
      Footstep transferFromFootstep = createFootstepAtCurrentLocation(swingSide.getOppositeSide());

      final FrameConvexPolygon2d footPolygon = new FrameConvexPolygon2d();
      ContactablePlaneBody contactableBody = feet.get(swingSide);
      if (feetManager.stayOnToes())
      {
         List<FramePoint> contactPoints = getToePoints(contactableBody);
         footPolygon.setIncludingFrameByProjectionOntoXYPlaneAndUpdate(referenceFrames.getSoleFrame(swingSide), contactPoints);
      }
      else
      {
         footPolygon.setIncludingFrameAndUpdate(contactableBody.getContactPoints2d());
      }

      TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(transferToFootstep, swingSide,
            transferFromFootstep, footPolygon);

      return transferToAndNextFootstepsData;
   }

   public TransferToAndNextFootstepsData createTransferToAndNextFootstepDataForSingleSupport(Footstep transferToFootstep, RobotSide swingSide,
         Footstep transferFromFootstep, FrameConvexPolygon2d footPolygon)
   {
      TransferToAndNextFootstepsData transferToAndNextFootstepsData = new TransferToAndNextFootstepsData();

      transferToAndNextFootstepsData.setTransferFromFootstep(transferFromFootstep);
      transferToAndNextFootstepsData.setTransferToFootstep(transferToFootstep);

      double timeAllottedForSingleSupportForICP = swingTimeCalculationProvider.getValue() + additionalSwingTimeForICP.getDoubleValue();

      transferToAndNextFootstepsData.setTransferToFootPolygonInSoleFrame(footPolygon);
      transferToAndNextFootstepsData.setTransferToSide(swingSide);
      transferToAndNextFootstepsData.setNextFootstep(upcomingFootstepList.getNextNextFootstep());
      transferToAndNextFootstepsData.setNextNextFootstep(upcomingFootstepList.getNextNextNextFootstep());
      transferToAndNextFootstepsData.setEstimatedStepTime(timeAllottedForSingleSupportForICP + transferTimeCalculationProvider.getValue());
      transferToAndNextFootstepsData.setW0(icpAndMomentumBasedController.getOmega0());
      transferToAndNextFootstepsData.setDoubleSupportDuration(transferTimeCalculationProvider.getValue());
      transferToAndNextFootstepsData.setSingleSupportDuration(timeAllottedForSingleSupportForICP);
      double doubleSupportInitialTransferDuration = 0.4; // TODO: Magic Number
      transferToAndNextFootstepsData.setDoubleSupportInitialTransferDuration(doubleSupportInitialTransferDuration);
      boolean stopIfReachedEnd = (upcomingFootstepList.getNumberOfFootstepsToProvide() <= 3); // TODO: Magic Number
      transferToAndNextFootstepsData.setStopIfReachedEnd(stopIfReachedEnd);

      if (VISUALIZE)
      {
         transferToAndNextFootstepsDataVisualizer.visualizeFootsteps(transferToAndNextFootstepsData);
      }

      return transferToAndNextFootstepsData;
   }

   private List<FramePoint> getToePoints(ContactablePlaneBody supportFoot)
   {
      FrameVector forward = new FrameVector(supportFoot.getSoleFrame(), 1.0, 0.0, 0.0);
      int nToePoints = 2;
      List<FramePoint> toePoints = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(supportFoot.getContactPointsCopy(), forward, nToePoints);
      for (FramePoint toePoint : toePoints)
      {
         toePoint.changeFrame(worldFrame);
      }

      return toePoints;
   }

   public void doMotionControl()
   {
      if (loopControllerForever.getBooleanValue())
      {
         while (true)
         {
            doMotionControlInternal();
         }
      }
      else
      {
         doMotionControlInternal();
      }
   }

   // FIXME: don't override
   private void doMotionControlInternal()
   {
      momentumBasedController.doPrioritaryControl();
      super.callUpdatables();

      icpAndMomentumBasedController.computeCapturePoint();
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();

      controlledCoMHeightAcceleration.set(computeDesiredCoMHeightAcceleration(desiredICPVelocity.getFrameVector2dCopy()));

      doFootControl();
      doArmControl();
      doHeadControl();

      //    doCoMControl(); //TODO: Should we be doing this too?
      doChestControl();
      setICPBasedMomentumRateOfChangeControlModuleInputs();
      doPelvisControl();
      doJointPositionControl();

      setTorqueControlJointsToZeroDersiredAcceleration();

      momentumBasedController.doSecondaryControl();

      momentumBasedController.doPassiveKneeControl();
      momentumBasedController.doProportionalControlOnCoP();
   }

   // TODO: connect ports instead
   private void setICPBasedMomentumRateOfChangeControlModuleInputs()
   {
      icpBasedMomentumRateOfChangeControlModule.getBipedSupportPolygonsInputPort().setData(bipedSupportPolygons);

      CapturePointData capturePointData = new CapturePointData();
      capturePointData.set(capturePoint.getFramePoint2dCopy(), icpAndMomentumBasedController.getOmega0());
      icpBasedMomentumRateOfChangeControlModule.getCapturePointInputPort().setData(capturePointData);

      CapturePointTrajectoryData capturePointTrajectoryData = new CapturePointTrajectoryData();
      capturePointTrajectoryData.set(finalDesiredICPInWorld.getFramePoint2dCopy(), desiredICP.getFramePoint2dCopy(), desiredICPVelocity.getFrameVector2dCopy());

      boolean keepCMPInsideSupportPolygon = true;
      if (feetManager.stayOnToes())
         keepCMPInsideSupportPolygon = false;
      if ((manipulationControlModule != null) && (manipulationControlModule.isAtLeastOneHandLoadBearing()))
         keepCMPInsideSupportPolygon = false;

      capturePointTrajectoryData.setProjectCMPIntoSupportPolygon(keepCMPInsideSupportPolygon);
      icpBasedMomentumRateOfChangeControlModule.getDesiredCapturePointTrajectoryInputPort().setData(capturePointTrajectoryData);

      icpBasedMomentumRateOfChangeControlModule.getSupportLegInputPort().setData(supportLeg.getEnumValue());

      icpBasedMomentumRateOfChangeControlModule.getDesiredCenterOfMassHeightAccelerationInputPort().setData(controlledCoMHeightAcceleration.getDoubleValue());

      icpBasedMomentumRateOfChangeControlModule.startComputation();
      icpBasedMomentumRateOfChangeControlModule.waitUntilComputationIsDone();
      MomentumRateOfChangeData momentumRateOfChangeData = icpBasedMomentumRateOfChangeControlModule.getMomentumRateOfChangeOutputPort().getData();
      momentumBasedController.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);
   }

   // Temporary objects to reduce garbage collection.
   private final CoMHeightPartialDerivativesData coMHeightPartialDerivatives = new CoMHeightPartialDerivativesData();
   private final ContactStatesAndUpcomingFootstepData centerOfMassHeightInputData = new ContactStatesAndUpcomingFootstepData();
   private final FramePoint comPosition = new FramePoint();
   private final FrameVector comVelocity = new FrameVector(worldFrame);
   private final FrameVector2d comXYVelocity = new FrameVector2d();
   private final FrameVector2d comXYAcceleration = new FrameVector2d();
   private final CoMHeightTimeDerivativesData comHeightDataBeforeSmoothing = new CoMHeightTimeDerivativesData();
   private final CoMHeightTimeDerivativesData comHeightDataAfterSmoothing = new CoMHeightTimeDerivativesData();
   private final CoMXYTimeDerivativesData comXYTimeDerivatives = new CoMXYTimeDerivativesData();
   private final FramePoint desiredCenterOfMassHeightPoint = new FramePoint(worldFrame);

   private double computeDesiredCoMHeightAcceleration(FrameVector2d desiredICPVelocity)
   {
      double zCurrent = comPosition.getZ();
      double zdCurrent = comVelocity.getZ();
      Footstep nextFootstep;

      if (controlPelvisHeightInsteadOfCoMHeight.getBooleanValue())
      {
         FramePoint pelvisPosition = new FramePoint(referenceFrames.getPelvisFrame());
         pelvisPosition.changeFrame(worldFrame);
         zCurrent = pelvisPosition.getZ();
         Twist pelvisTwist = new Twist();
         twistCalculator.packTwistOfBody(pelvisTwist, fullRobotModel.getPelvis());
         pelvisTwist.changeFrame(worldFrame);
         zdCurrent = comVelocity.getZ(); // Just use com velocity for now for damping...
      }

      centerOfMassHeightInputData
            .setCenterOfMassAndPelvisZUpFrames(momentumBasedController.getCenterOfMassFrame(), momentumBasedController.getPelvisZUpFrame());

      List<? extends PlaneContactState> contactStatesList = getContactStatesList();

      centerOfMassHeightInputData.setContactStates(contactStatesList);

      centerOfMassHeightInputData.setSupportLeg(supportLeg.getEnumValue());

      if (pushRecoveryModule.isEnabled() && pushRecoveryModule.isRecoveringFromDoubleSupportFall())
      {
         nextFootstep = pushRecoveryModule.getRecoverFromDoubleSupportFootStep();
      }
      else
      {
         nextFootstep = upcomingFootstepList.getNextFootstep();
      }

      centerOfMassHeightInputData.setUpcomingFootstep(nextFootstep);

      centerOfMassHeightTrajectoryGenerator.solve(coMHeightPartialDerivatives, centerOfMassHeightInputData);

      comPosition.setToZero(referenceFrames.getCenterOfMassFrame());
      centerOfMassJacobian.packCenterOfMassVelocity(comVelocity);
      comPosition.changeFrame(worldFrame);
      comVelocity.changeFrame(worldFrame);

      // TODO: use current omega0 instead of previous
      comXYVelocity.setIncludingFrame(comVelocity.getReferenceFrame(), comVelocity.getX(), comVelocity.getY());
      comXYAcceleration.setIncludingFrame(desiredICPVelocity);
      comXYAcceleration.sub(comXYVelocity);
      comXYAcceleration.scale(icpAndMomentumBasedController.getOmega0()); // MathTools.square(omega0.getDoubleValue()) * (com.getX() - copX);

      // FrameVector2d comd2dSquared = new FrameVector2d(comXYVelocity.getReferenceFrame(), comXYVelocity.getX() * comXYVelocity.getX(), comXYVelocity.getY() * comXYVelocity.getY());

      comXYTimeDerivatives.setCoMXYPosition(comPosition.toFramePoint2d());
      comXYTimeDerivatives.setCoMXYVelocity(comXYVelocity);
      comXYTimeDerivatives.setCoMXYAcceleration(comXYAcceleration);

      coMHeightTimeDerivativesCalculator.computeCoMHeightTimeDerivatives(comHeightDataBeforeSmoothing, comXYTimeDerivatives, coMHeightPartialDerivatives);

      comHeightDataBeforeSmoothing.getComHeight(desiredCenterOfMassHeightPoint);
      desiredCoMHeightFromTrajectory.set(desiredCenterOfMassHeightPoint.getZ());
      desiredCoMHeightVelocityFromTrajectory.set(comHeightDataBeforeSmoothing.getComHeightVelocity());
      desiredCoMHeightAccelerationFromTrajectory.set(comHeightDataBeforeSmoothing.getComHeightAcceleration());

      //    correctCoMHeight(desiredICPVelocity, zCurrent, comHeightDataBeforeSmoothing, false, false);

      //    comHeightDataBeforeSmoothing.getComHeight(desiredCenterOfMassHeightPoint);
      //    desiredCoMHeightBeforeSmoothing.set(desiredCenterOfMassHeightPoint.getZ());
      //    desiredCoMHeightVelocityBeforeSmoothing.set(comHeightDataBeforeSmoothing.getComHeightVelocity());
      //    desiredCoMHeightAccelerationBeforeSmoothing.set(comHeightDataBeforeSmoothing.getComHeightAcceleration());

      coMHeightTimeDerivativesSmoother.smooth(comHeightDataAfterSmoothing, comHeightDataBeforeSmoothing);

      comHeightDataAfterSmoothing.getComHeight(desiredCenterOfMassHeightPoint);
      desiredCoMHeightAfterSmoothing.set(desiredCenterOfMassHeightPoint.getZ());
      desiredCoMHeightVelocityAfterSmoothing.set(comHeightDataAfterSmoothing.getComHeightVelocity());
      desiredCoMHeightAccelerationAfterSmoothing.set(comHeightDataAfterSmoothing.getComHeightAcceleration());

      feetManager.correctCoMHeight(trailingLeg.getEnumValue(), desiredICPVelocity, zCurrent, comHeightDataAfterSmoothing);

      comHeightDataAfterSmoothing.getComHeight(desiredCenterOfMassHeightPoint);
      desiredCoMHeightCorrected.set(desiredCenterOfMassHeightPoint.getZ());
      desiredCoMHeightVelocityCorrected.set(comHeightDataAfterSmoothing.getComHeightVelocity());
      desiredCoMHeightAccelerationCorrected.set(comHeightDataAfterSmoothing.getComHeightAcceleration());

      comHeightDataAfterSmoothing.getComHeight(desiredCenterOfMassHeightPoint);

      double zDesired = desiredCenterOfMassHeightPoint.getZ();
      double zdDesired = comHeightDataAfterSmoothing.getComHeightVelocity();
      double zddFeedForward = comHeightDataAfterSmoothing.getComHeightAcceleration();

      double zddDesired = centerOfMassHeightController.compute(zCurrent, zDesired, zdCurrent, zdDesired) + zddFeedForward;

      for (RobotSide robotSide : RobotSide.values)
      {
         if (feetManager.isInFlatSupportState(robotSide) && feetManager.isInSingularityNeighborhood(robotSide))
         {
            // Ignore the desired height acceleration only if EndEffectorControlModule is not taking care of singularity during support
            if (!LegSingularityAndKneeCollapseAvoidanceControlModule.USE_SINGULARITY_AVOIDANCE_SUPPORT)
               zddDesired = 0.0;

            double zTreshold = 0.01;

            if (zDesired >= zCurrent - zTreshold)
            {
               // Can't achieve the desired height, just lock the knee
               feetManager.lockKnee(robotSide);
            }
            else
            {
               // Do the singularity escape before trying to achieve the desired height
               feetManager.doSupportSingularityEscape(robotSide);
            }
         }
      }

      double epsilon = 1e-12;
      zddDesired = MathTools.clipToMinMax(zddDesired, -gravity + epsilon, Double.POSITIVE_INFINITY);

      return zddDesired;
   }

   private List<PlaneContactState> getContactStatesList()
   {
      List<PlaneContactState> contactStatesList = new ArrayList<PlaneContactState>();

      for (ContactablePlaneBody contactablePlaneBody : feet)
      {
         PlaneContactState contactState = momentumBasedController.getContactState(contactablePlaneBody);

         //       YoPlaneContactState contactState = contactStates.get(contactablePlaneBody);
         if (contactState.inContact())
            contactStatesList.add(contactState);
      }

      return contactStatesList;
   }

   private final List<FramePoint> tempContactPoints = new ArrayList<FramePoint>();
   private final FrameConvexPolygon2d tempFootPolygon = new FrameConvexPolygon2d(worldFrame);

   // TODO: should probably precompute this somewhere else
   private FrameConvexPolygon2d computeFootPolygon(RobotSide robotSide, ReferenceFrame referenceFrame)
   {
      momentumBasedController.getContactPoints(feet.get(robotSide), tempContactPoints);
      tempFootPolygon.setIncludingFrameByProjectionOntoXYPlaneAndUpdate(referenceFrame, tempContactPoints);

      return tempFootPolygon;
   }

   private Footstep createFootstepAtCurrentLocation(RobotSide robotSide)
   {
      ContactablePlaneBody foot = feet.get(robotSide);
      ReferenceFrame footReferenceFrame = foot.getRigidBody().getParentJoint().getFrameAfterJoint();
      FramePose framePose = new FramePose(footReferenceFrame);
      framePose.changeFrame(worldFrame);

      PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("poseReferenceFrame", framePose);

      boolean trustHeight = true;
      Footstep footstep = new Footstep(foot, poseReferenceFrame, trustHeight);

      return footstep;
   }

   public void integrateAnkleAccelerationsOnSwingLeg(RobotSide swingSide)
   {
      fullRobotModel.getLegJoint(swingSide, LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(true);
      fullRobotModel.getLegJoint(swingSide, LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(true);
      fullRobotModel.getLegJoint(swingSide.getOppositeSide(), LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(false);
      fullRobotModel.getLegJoint(swingSide.getOppositeSide(), LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(false);
   }

   private void doNotIntegrateAnkleAccelerations()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(false);
         fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(false);
      }
   }

   private void resetLoadedLegIntegrators(RobotSide robotSide)
   {
      if (resetIntegratorsAfterSwing.getBooleanValue())
      {
         for (LegJointName jointName : fullRobotModel.getRobotSpecificJointNames().getLegJointNames())
            fullRobotModel.getLegJoint(robotSide, jointName).resetDesiredAccelerationIntegrator();
      }
   }

   private final FrameVector2d stanceAnkleToOriginalICPVector = new FrameVector2d();

   private final FramePoint2d stanceToSwingPoint = new FramePoint2d();
   private final FrameVector2d stanceToSwingVector = new FrameVector2d();

   private void moveICPToInsideOfFootAtEndOfSwing(RobotSide supportSide, FramePoint2d upcomingFootstepLocation, double swingTime, double swingTimeRemaining,
         FramePoint2d desiredICPToMove)
   {
      swingTimeRemainingForICPMoveViz.set(swingTimeRemaining);

      ReferenceFrame supportAnkleFrame = referenceFrames.getAnkleZUpFrame(supportSide);
      desiredICPToMove.changeFrame(supportAnkleFrame);
      stanceAnkleToOriginalICPVector.setIncludingFrame(desiredICPToMove);

      stanceToSwingPoint.setIncludingFrame(upcomingFootstepLocation);
      stanceToSwingPoint.changeFrame(supportAnkleFrame);

      stanceToSwingVector.setIncludingFrame(stanceToSwingPoint);
      double stanceToSwingDistance = stanceToSwingVector.length();
      if (stanceToSwingDistance < 0.001)
      {
         desiredICPToMove.changeFrame(desiredICP.getReferenceFrame());

         return;
      }

      stanceToSwingVector.normalize();

      distanceFromLineToOriginalICP.set(stanceToSwingVector.dot(stanceAnkleToOriginalICPVector));
      double timeToUseBeforeShift = singleSupportTimeLeftBeforeShift.getDoubleValue();
      if (timeToUseBeforeShift < 0.01)
         timeToUseBeforeShift = 0.01;

      double deltaTime = swingTime - timeToUseBeforeShift;
      double percent;
      if (deltaTime <= 1e-7)
         percent = 1.0;
      else
      {
         percent = (swingTime - swingTimeRemaining) / (deltaTime);
      }

      percent = MathTools.clipToMinMax(percent, 0.0, 1.0);

      double maxDistanceToMove = moveICPAwayDuringSwingDistance.getDoubleValue();
      maxDistanceToMove = MathTools.clipToMinMax(maxDistanceToMove, 0.0, stanceToSwingDistance / 2.0);
      double duringSwingDistance = (percent * maxDistanceToMove);

      if (swingTimeRemaining > timeToUseBeforeShift)
      {
         amountToMoveICPAway.set(duringSwingDistance);
      }
      else
      {
         percent = (1.0 - swingTimeRemaining / timeToUseBeforeShift);
         percent = MathTools.clipToMinMax(percent, 0.0, 1.0);

         maxDistanceToMove = moveICPAwayAtEndOfSwingDistance.getDoubleValue();
         maxDistanceToMove = MathTools.clipToMinMax(maxDistanceToMove, 0.0, stanceToSwingDistance / 2.0);

         maxDistanceToMove -= moveICPAwayDuringSwingDistance.getDoubleValue();
         if (maxDistanceToMove < 0.0)
            maxDistanceToMove = 0.0;

         amountToMoveICPAway.set(duringSwingDistance + (percent * maxDistanceToMove));
      }

      if (distanceFromLineToOriginalICP.getDoubleValue() > amountToMoveICPAway.getDoubleValue())
      {
         desiredICPToMove.changeFrame(desiredICP.getReferenceFrame());

         return;
      }

      double additionalDistance = amountToMoveICPAway.getDoubleValue() - distanceFromLineToOriginalICP.getDoubleValue();

      stanceToSwingVector.scale(additionalDistance);
      desiredICPToMove.add(stanceToSwingVector);

      desiredICPToMove.changeFrame(desiredICP.getReferenceFrame());
   }

   private void limitICPToMiddleOfFootOrInside(RobotSide supportSide, FramePoint2d desiredICPLocal)
   {
      desiredICPLocal.changeFrame(referenceFrames.getAnkleZUpFrame(supportSide));
      double minimumInside = 0.005;

      if (supportSide.negateIfLeftSide(desiredICPLocal.getY()) < minimumInside)
      {
         desiredICPLocal.setY(supportSide.negateIfLeftSide(minimumInside));
      }

      desiredICPLocal.changeFrame(desiredICP.getReferenceFrame());
   }

}
