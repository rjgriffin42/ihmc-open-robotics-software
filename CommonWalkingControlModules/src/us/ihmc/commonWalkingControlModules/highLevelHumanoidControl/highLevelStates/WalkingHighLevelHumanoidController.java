package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.PlaneContactState;
import us.ihmc.commonWalkingControlModules.calculators.EquivalentConstantCoPCalculator;
import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.ChestOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.WalkOnTheEdgesManager;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.endEffector.EndEffectorControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.controlModules.head.HeadOrientationManager;
import us.ihmc.commonWalkingControlModules.controllers.LidarControllerInterface;
import us.ihmc.commonWalkingControlModules.desiredFootStep.DesiredFootstepCalculatorTools;
import us.ihmc.commonWalkingControlModules.desiredFootStep.Footstep;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepProvider;
import us.ihmc.commonWalkingControlModules.desiredFootStep.FootstepUtils;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsDataVisualizer;
import us.ihmc.commonWalkingControlModules.desiredFootStep.UpcomingFootstepList;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingManagers;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.VariousWalkingProviders;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.ICPBasedMomentumRateOfChangeControlModule;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.InstantaneousCapturePointPlanner;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointCalculator;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.CapturePointTrajectoryData;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumControlModuleBridge.MomentumControlModuleType;
import us.ihmc.commonWalkingControlModules.momentumBasedController.dataObjects.MomentumRateOfChangeData;
import us.ihmc.commonWalkingControlModules.packetConsumers.ReinitializeWalkingControllerProvider;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.LegJointName;
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
import us.ihmc.commonWalkingControlModules.trajectories.CurrentOrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.FlatThenPolynomialCoMHeightTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.MaximumConstantJerkFinalToeOffAngleComputer;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationInterpolationTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.OrientationTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.PoseTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.trajectories.SettableOrientationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.SimpleTwoWaypointTrajectoryParameters;
import us.ihmc.commonWalkingControlModules.trajectories.SwingTimeCalculationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.TransferTimeCalculationProvider;
import us.ihmc.commonWalkingControlModules.trajectories.TwoWaypointTrajectoryUtils;
import us.ihmc.commonWalkingControlModules.trajectories.WalkOnTheEdgesProviders;
import us.ihmc.commonWalkingControlModules.trajectories.WalkOnTheEdgesProviders.ToeOffMotionType;
import us.ihmc.commonWalkingControlModules.trajectories.WrapperForPositionAndOrientationTrajectoryGenerators;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.Pair;
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
import us.ihmc.utilities.screwTheory.CenterOfMassJacobian;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.VariableChangedListener;
import com.yobotics.simulationconstructionset.YoVariable;
import com.yobotics.simulationconstructionset.util.GainCalculator;
import com.yobotics.simulationconstructionset.util.PDController;
import com.yobotics.simulationconstructionset.util.errorHandling.WalkingStatusReporter;
import com.yobotics.simulationconstructionset.util.errorHandling.WalkingStatusReporter.ErrorType;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicPosition;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint;
import com.yobotics.simulationconstructionset.util.math.frames.YoFramePoint2d;
import com.yobotics.simulationconstructionset.util.math.frames.YoFrameVector2d;
import com.yobotics.simulationconstructionset.util.statemachines.State;
import com.yobotics.simulationconstructionset.util.statemachines.StateMachine;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransition;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionAction;
import com.yobotics.simulationconstructionset.util.statemachines.StateTransitionCondition;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleProvider;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.PositionTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParameters;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryParametersProvider;
import com.yobotics.simulationconstructionset.util.trajectory.TrajectoryWaypointGenerationMethod;
import com.yobotics.simulationconstructionset.util.trajectory.YoPositionProvider;
import com.yobotics.simulationconstructionset.util.trajectory.YoVariableDoubleProvider;

public class WalkingHighLevelHumanoidController extends AbstractHighLevelHumanoidControlPattern
{
   private boolean VISUALIZE = true;

   private static final boolean DO_TRANSITION_WHEN_TIME_IS_UP = false;

   private final static HighLevelState controllerState = HighLevelState.WALKING;
   private final static MomentumControlModuleType MOMENTUM_CONTROL_MODULE_TO_USE = MomentumControlModuleType.OPTIMIZATION;

   private final static double DELAY_TIME_BEFORE_TRUSTING_CONTACTS = 0.12;
   
   private final double PELVIS_YAW_INITIALIZATION_TIME = 1.5;

   private final BooleanYoVariable alreadyBeenInDoubleSupportOnce;

   private static enum WalkingState {LEFT_SUPPORT, RIGHT_SUPPORT, TRANSFER_TO_LEFT_SUPPORT, TRANSFER_TO_RIGHT_SUPPORT, DOUBLE_SUPPORT}

   private final static boolean DEBUG = false;
   private final StateMachine<WalkingState> stateMachine;
   private final CenterOfMassJacobian centerOfMassJacobian;

   private final CoMHeightTrajectoryGenerator centerOfMassHeightTrajectoryGenerator;
   private final CoMHeightTimeDerivativesCalculator coMHeightTimeDerivativesCalculator = new CoMHeightTimeDerivativesCalculator();
   private final CoMHeightTimeDerivativesSmoother coMHeightTimeDerivativesSmoother;


   private final PDController centerOfMassHeightController;
   private final SideDependentList<WalkingState> singleSupportStateEnums = new SideDependentList<WalkingState>(WalkingState.LEFT_SUPPORT,
                                                                              WalkingState.RIGHT_SUPPORT);

   private final SideDependentList<WalkingState> transferStateEnums = new SideDependentList<WalkingState>(WalkingState.TRANSFER_TO_LEFT_SUPPORT,
                                                                         WalkingState.TRANSFER_TO_RIGHT_SUPPORT);

   private final DoubleYoVariable stopInDoubleSupporTrajectoryTime = new DoubleYoVariable("stopInDoubleSupporTrajectoryTime", registry);
   private final DoubleYoVariable dwellInSingleSupportDuration = new DoubleYoVariable("dwellInSingleSupportDuration", 
         "Amount of time to stay in single support after the ICP trajectory is done if you haven't registered a touchdown yet", registry);
   
   private final BooleanYoVariable loopControllerForever = new BooleanYoVariable("loopControllerForever", "For checking memory and profiling", registry);
   private final BooleanYoVariable justFall = new BooleanYoVariable("justFall", registry);
   
   private final BooleanYoVariable stepOnOrOff = new BooleanYoVariable("stepOnOrOff", registry);
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
   private final DoubleYoVariable footLoadThresholdToHoldPosition = new DoubleYoVariable("footLoadThresholdToHoldPosition", registry);
   private final SideDependentList<BooleanYoVariable> requestSupportFootToHoldPosition = new SideDependentList<BooleanYoVariable>();

   private final DoubleYoVariable minOrbitalEnergyForSingleSupport = new DoubleYoVariable("minOrbitalEnergyForSingleSupport", registry);
   private final DoubleYoVariable amountToBeInsideSingleSupport = new DoubleYoVariable("amountToBeInsideSingleSupport", registry);
   private final DoubleYoVariable amountToBeInsideDoubleSupport = new DoubleYoVariable("amountToBeInsideDoubleSupport", registry);

   private final DoubleYoVariable userDesiredPelvisYaw = new DoubleYoVariable("userDesiredPelvisYaw", registry);
   private final DoubleYoVariable userDesiredPelvisPitch = new DoubleYoVariable("userDesiredPelvisPitch", registry);
   private final DoubleYoVariable userDesiredPelvisRoll = new DoubleYoVariable("userDesiredPelvisRoll", registry);
   private final BooleanYoVariable userSetDesiredPelvis = new BooleanYoVariable("userSetDesiredPelvis", registry);
   
   private final SettableOrientationProvider initialPelvisOrientationProvider;
   private final SettableOrientationProvider finalPelvisOrientationProvider;
   private final OrientationTrajectoryGenerator pelvisOrientationTrajectoryGenerator;

   private final SwingTimeCalculationProvider swingTimeCalculationProvider;
   private final TransferTimeCalculationProvider transferTimeCalculationProvider;
   
   private final TrajectoryParametersProvider trajectoryParametersProvider;

   private final DoubleYoVariable additionalSwingTimeForICP = new DoubleYoVariable("additionalSwingTimeForICP", registry);

   private final YoPositionProvider swingFootFinalPositionProvider;

   private final DoubleYoVariable swingAboveSupportAnkle = new DoubleYoVariable("swingAboveSupportAnkle", registry);
   private final BooleanYoVariable readyToGrabNextFootstep = new BooleanYoVariable("readyToGrabNextFootstep", registry);

   private final DoubleYoVariable minimumICPFromCenterDuringSingleSupport = new DoubleYoVariable("minimumICPFromCenterDuringSingleSupport", registry);
   private final DoubleYoVariable singleSupportTimeLeftBeforeShift = new DoubleYoVariable("singleSupportTimeLeftBeforeShift", registry);
   
   private final HashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters;
   private final InstantaneousCapturePointPlanner instantaneousCapturePointPlanner;
   private final ReinitializeWalkingControllerProvider reinitializeControllerProvider;

   private final SideDependentList<SettableOrientationProvider> finalFootOrientationProviders = new SideDependentList<SettableOrientationProvider>();

   private final ICPBasedMomentumRateOfChangeControlModule icpBasedMomentumRateOfChangeControlModule;

   private final BooleanYoVariable icpTrajectoryHasBeenInitialized;

   private final UpcomingFootstepList upcomingFootstepList;

   private final AverageOrientationCalculator averageOrientationCalculator = new AverageOrientationCalculator();

   private final ICPAndMomentumBasedController icpAndMomentumBasedController;
   private final EnumYoVariable<RobotSide> upcomingSupportLeg;
   private final EnumYoVariable<RobotSide> supportLeg;
   private final BipedSupportPolygons bipedSupportPolygons;
   private final YoFramePoint capturePoint;
   private final YoFramePoint2d desiredICP;
   private final YoFrameVector2d desiredICPVelocity;

   private final DoubleYoVariable controlledCoMHeightAcceleration;
   private final DoubleYoVariable controllerInitializationTime;

   private final TransferToAndNextFootstepsDataVisualizer transferToAndNextFootstepsDataVisualizer;
   
   private final BooleanYoVariable doneFinishingSingleSupportTransfer = new BooleanYoVariable("doneFinishingSingleSupportTransfer", registry);

   private final BooleanYoVariable ecmpBasedToeOffHasBeenInitialized = new BooleanYoVariable("ecmpBasedToeOffHasBeenInitialized", registry);
   private final YoFramePoint2d desiredECMP = new YoFramePoint2d("desiredECMP", "", worldFrame, registry);
   private final BooleanYoVariable desiredECMPinSupportPolygon = new BooleanYoVariable("desiredECMPinSupportPolygon", registry);
   private YoFramePoint ecmpViz = new YoFramePoint("ecmpViz", worldFrame, registry);
   
   private final YoVariableDoubleProvider totalEstimatedToeOffTimeProvider = new YoVariableDoubleProvider("totalEstimatedToeOffTimeProvider", registry);
   
   private final DoubleYoVariable singularityEscapeNullspaceMultiplierSwingLeg = new DoubleYoVariable("singularityEscapeNullspaceMultiplierSwingLeg", registry);
   private final DoubleYoVariable singularityEscapeNullspaceMultiplierSupportLeg = new DoubleYoVariable("singularityEscapeNullspaceMultiplierSupportLeg", registry);
   private final DoubleYoVariable singularityEscapeNullspaceMultiplierSupportLegLocking = new DoubleYoVariable("singularityEscapeNullspaceMultiplierSupportLegLocking", registry);

   private double referenceTime = 0.22; 
   private MaximumConstantJerkFinalToeOffAngleComputer maximumConstantJerkFinalToeOffAngleComputer = new MaximumConstantJerkFinalToeOffAngleComputer();  

   private final VariousWalkingProviders variousWalkingProviders;
   private final VariousWalkingManagers variousWalkingManagers;

   private final DoubleYoVariable walkingHeadOrientationKp = new DoubleYoVariable("walkingHeadOrientationKp", registry);
   private final DoubleYoVariable walkingHeadOrientationZeta = new DoubleYoVariable("walkingHeadOrientationZeta", registry);
   
   private final DoubleYoVariable swingKpXY = new DoubleYoVariable("swingKpXY", registry);
   private final DoubleYoVariable swingKpZ = new DoubleYoVariable("swingKpZ", registry);
   private final DoubleYoVariable swingKpOrientation = new DoubleYoVariable("swingKpOrientation", registry);
   private final DoubleYoVariable swingZeta = new DoubleYoVariable("swingZeta", registry);
   
   private final DoubleYoVariable swingMaxPositionAcceleration = new DoubleYoVariable("swingMaxPositionAcceleration", registry);
   private final DoubleYoVariable swingMaxPositionJerk = new DoubleYoVariable("swingMaxPositionJerk", registry);
   private final DoubleYoVariable swingMaxOrientationAcceleration = new DoubleYoVariable("swingMaxOrientationAcceleration", registry);
   private final DoubleYoVariable swingMaxOrientationJerk = new DoubleYoVariable("swingMaxOrientationJerk", registry);

   private final WalkOnTheEdgesManager walkOnTheEdgesManager;
   private final WalkOnTheEdgesProviders walkOnTheEdgesProviders;

   public WalkingHighLevelHumanoidController(VariousWalkingProviders variousWalkingProviders, VariousWalkingManagers variousWalkingManagers,
         SideDependentList<FootSwitchInterface> footSwitches, DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
         CoMHeightTrajectoryGenerator centerOfMassHeightTrajectoryGenerator, SideDependentList<PositionTrajectoryGenerator> footPositionTrajectoryGenerators,
         WalkOnTheEdgesProviders walkOnTheEdgesProviders, SwingTimeCalculationProvider swingTimeCalculationProvider,
         TransferTimeCalculationProvider transferTimeCalculationProvider, YoPositionProvider swingFootFinalPositionProvider,
         TrajectoryParametersProvider trajectoryParametersProvider, double desiredPelvisPitch, WalkingControllerParameters walkingControllerParameters,
         ICPBasedMomentumRateOfChangeControlModule momentumRateOfChangeControlModule, LidarControllerInterface lidarControllerInterface,
         InstantaneousCapturePointPlanner instantaneousCapturePointPlanner, ICPAndMomentumBasedController icpAndMomentumBasedController,
         MomentumBasedController momentumBasedController, WalkingStatusReporter walkingStatusReporter)
   {
      
      super(variousWalkingProviders, variousWalkingManagers, momentumBasedController, walkingControllerParameters, 
            lidarControllerInterface, dynamicGraphicObjectsListRegistry, controllerState);
     
      super.addUpdatables(icpAndMomentumBasedController.getUpdatables());

      userSetDesiredPelvis.addVariableChangedListener(new VariableChangedListener(){
         public void variableChanged(YoVariable v)
         {
            FrameOrientation frameOrientation = new FrameOrientation(referenceFrames.getPelvisFrame());
            frameOrientation.changeFrame(ReferenceFrame.getWorldFrame());
            
            userDesiredPelvisYaw.set(frameOrientation.getYawPitchRoll()[0]);
         }});
      
      this.variousWalkingProviders = variousWalkingProviders;
      this.variousWalkingManagers = variousWalkingManagers;
      
      setupManagers(variousWalkingManagers);
      
      FootstepProvider footstepProvider = variousWalkingProviders.getFootstepProvider();
      HashMap<Footstep, TrajectoryParameters> mapFromFootstepsToTrajectoryParameters = variousWalkingProviders.getMapFromFootstepsToTrajectoryParameters();
      this.reinitializeControllerProvider = variousWalkingProviders.getReinitializeWalkingControllerProvider();

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
         DynamicGraphicPosition dynamicGraphicPositionECMP = new DynamicGraphicPosition("ecmpviz", ecmpViz, 0.002, YoAppearance.BlueViolet());
         dynamicGraphicObjectsListRegistry.registerDynamicGraphicObject("ecmpviz", dynamicGraphicPositionECMP);
         dynamicGraphicObjectsListRegistry.registerArtifact("ecmpviz", dynamicGraphicPositionECMP.createArtifact());
      }

      // Getting parameters from the icpAndMomentumBasedController
      this.icpAndMomentumBasedController = icpAndMomentumBasedController;
      
//      contactStates = momentumBasedController.getContactStates();
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
      
      this.trajectoryParametersProvider = trajectoryParametersProvider;
      this.mapFromFootstepsToTrajectoryParameters = mapFromFootstepsToTrajectoryParameters;
      this.footSwitches = footSwitches;
      this.icpBasedMomentumRateOfChangeControlModule = momentumRateOfChangeControlModule;

      this.instantaneousCapturePointPlanner = instantaneousCapturePointPlanner;

      this.upcomingFootstepList = new UpcomingFootstepList(footstepProvider, registry);

      this.centerOfMassHeightController = new PDController("comHeight", registry);
      double kpCoMHeight = walkingControllerParameters.getKpCoMHeight();
      centerOfMassHeightController.setProportionalGain(kpCoMHeight); 
      double zetaCoMHeight =  walkingControllerParameters.getZetaCoMHeight(); 
      centerOfMassHeightController.setDerivativeGain(GainCalculator.computeDerivativeGain(centerOfMassHeightController.getProportionalGain(), zetaCoMHeight));

      String namePrefix = "walking";

      this.stateMachine = new StateMachine<WalkingState>(namePrefix + "State", namePrefix + "SwitchTime", WalkingState.class, yoTime, registry);    // this is used by name, and it is ugly.

      this.swingFootFinalPositionProvider = swingFootFinalPositionProvider;

      this.icpTrajectoryHasBeenInitialized = new BooleanYoVariable("icpTrajectoryHasBeenInitialized", registry);

      rememberFinalICPFromSingleSupport.set(false);    // true);
      finalDesiredICPInWorld.set(Double.NaN, Double.NaN);

      coefficientOfFriction.set(0.0); //TODO Remove coefficient of friction from the abstract high level stuff and let the EndEffector controlModule deal with it

      setupLegJacobians(fullRobotModel);

      this.walkOnTheEdgesProviders = walkOnTheEdgesProviders;
      walkOnTheEdgesManager = new WalkOnTheEdgesManager(walkingControllerParameters, walkOnTheEdgesProviders, feet, footEndEffectorControlModules, registry);
      this.centerOfMassHeightTrajectoryGenerator.attachWalkOnToesManager(walkOnTheEdgesManager);
      
      maximumConstantJerkFinalToeOffAngleComputer.reinitialize(walkOnTheEdgesProviders.getMaximumToeOffAngle(), referenceTime);
      
      setupFootControlModules(footPositionTrajectoryGenerators);

      initialPelvisOrientationProvider = new SettableOrientationProvider("initialPelvis", worldFrame, registry);
      finalPelvisOrientationProvider = new SettableOrientationProvider("finalPelvis", worldFrame, registry);
      this.pelvisOrientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator("pelvis", worldFrame, swingTimeCalculationProvider,
              initialPelvisOrientationProvider, finalPelvisOrientationProvider, registry);

      setUpStateMachine();
      readyToGrabNextFootstep.set(true);

      dwellInSingleSupportDuration.set(0.2);
      
      minOrbitalEnergyForSingleSupport.set(0.007);    // 0.008
      amountToBeInsideSingleSupport.set(0.0);
      amountToBeInsideDoubleSupport.set(0.03);    // 0.02);    // TODO: necessary for stairs...
      transferTimeCalculationProvider.setTransferTime();   
      
      totalEstimatedToeOffTimeProvider.set(transferTimeCalculationProvider.getValue());
            
      stopInDoubleSupporTrajectoryTime.set(0.5);
      this.userDesiredPelvisPitch.set(desiredPelvisPitch);
      
      additionalSwingTimeForICP.set(0.1);
      minimumSwingFraction.set(0.5); //0.8);

      upcomingSupportLeg.set(RobotSide.RIGHT);    // TODO: stairs hack, so that the following lines use the correct leading leg

      controllerInitializationTime = new DoubleYoVariable("controllerInitializationTime", registry);
      alreadyBeenInDoubleSupportOnce = new BooleanYoVariable("alreadyBeenInDoubleSupportOnce", registry);

      controlPelvisHeightInsteadOfCoMHeight.set(false);
      
      minimumICPFromCenterDuringSingleSupport.set(0.01);   //0.04);  //0.01); 
      singleSupportTimeLeftBeforeShift.set(1.0);   //0.5);   //0.1); 
      
      footLoadThresholdToHoldPosition.set(0.2);
   }


   protected void setupFootControlModules(SideDependentList<PositionTrajectoryGenerator> footPositionTrajectoryGenerators)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         BooleanYoVariable requestHoldPosition = new BooleanYoVariable(robotSide.getCamelCaseNameForStartOfExpression() + "RequestSupportFootToHoldPosition", registry);
         requestSupportFootToHoldPosition.put(robotSide, requestHoldPosition);
      }
      
      //TODO: Pull these up to a higher level.

      singularityEscapeNullspaceMultiplierSwingLeg.set(100.0);
      singularityEscapeNullspaceMultiplierSupportLeg.set(20.0);
      singularityEscapeNullspaceMultiplierSupportLegLocking.set(-0.5);
      double minJacobianDeterminantForSingularityEscape = 0.03;
      
      swingKpXY.set(100.0);
      swingKpZ.set(200.0);
      swingKpOrientation.set(200.0);
      swingZeta.set(1.0);

//      swingMaxPositionAcceleration.set(10.0); 
//      swingMaxPositionJerk.set(150.0);
//      swingMaxOrientationAcceleration.set(100.0);
//      swingMaxOrientationJerk.set(1500.0);

      swingMaxPositionAcceleration.set(Double.POSITIVE_INFINITY); 
      swingMaxPositionJerk.set(Double.POSITIVE_INFINITY);
      swingMaxOrientationAcceleration.set(Double.POSITIVE_INFINITY);
      swingMaxOrientationJerk.set(Double.POSITIVE_INFINITY);
      
      for (RobotSide robotSide : RobotSide.values)
      {
         ContactablePlaneBody bipedFoot = feet.get(robotSide);

         //TODO: If we know the surface normal here, use it.
         momentumBasedController.setPlaneContactStateFullyConstrained(bipedFoot);
         
         String sideString = robotSide.getCamelCaseNameForStartOfExpression();

         PositionTrajectoryGenerator swingPositionTrajectoryGenerator = footPositionTrajectoryGenerators.get(robotSide);
         DoubleTrajectoryGenerator heelPitchTrajectoryGenerator = walkOnTheEdgesProviders.getFootTouchdownPitchTrajectoryGenerator(robotSide);

         OrientationProvider initialOrientationProvider = new CurrentOrientationProvider(worldFrame, bipedFoot.getBodyFrame());
         SettableOrientationProvider finalFootOrientationProvider = new SettableOrientationProvider(sideString + "FinalFootOrientation", worldFrame, registry);
         finalFootOrientationProviders.put(robotSide, finalFootOrientationProvider);

         OrientationTrajectoryGenerator swingOrientationTrajectoryGenerator = new OrientationInterpolationTrajectoryGenerator(sideString
                                                                                 + "SwingFootOrientation", worldFrame, swingTimeCalculationProvider,
                                                                                    initialOrientationProvider, finalFootOrientationProvider, registry);
         
         PoseTrajectoryGenerator swingPoseTrajectoryGenerator = new WrapperForPositionAndOrientationTrajectoryGenerators(swingPositionTrajectoryGenerator,
                                                                   swingOrientationTrajectoryGenerator);
         
         int jacobianId = legJacobianIds.get(robotSide);
         OneDoFJoint kneeJoint = fullRobotModel.getLegJoint(robotSide, LegJointName.KNEE);
         
         EndEffectorControlModule endEffectorControlModule;

         BooleanYoVariable requestHoldPosition = requestSupportFootToHoldPosition.get(robotSide);
         
         if (WalkOnTheEdgesProviders.TOEOFF_MOTION_TYPE_USED != ToeOffMotionType.FREE)
         {
            DoubleTrajectoryGenerator onToesPitchTrajectoryGenerator = walkOnTheEdgesProviders.getToeOffPitchTrajectoryGenerators(robotSide);
            endEffectorControlModule = new EndEffectorControlModule(controlDT, bipedFoot, jacobianId, kneeJoint, swingPoseTrajectoryGenerator,
                                          heelPitchTrajectoryGenerator, onToesPitchTrajectoryGenerator, requestHoldPosition, momentumBasedController, registry);
         }
         else
         {
            // Let the toe pitch motion free. It seems to work better.
            DoubleProvider maximumToeOffAngleProvider = walkOnTheEdgesProviders.getMaximumToeOffAngleProvider();
            endEffectorControlModule = new EndEffectorControlModule(controlDT, bipedFoot, jacobianId, kneeJoint, swingPoseTrajectoryGenerator,
                                          heelPitchTrajectoryGenerator, maximumToeOffAngleProvider, requestHoldPosition, momentumBasedController, registry);
         }
         
                  
         VariableChangedListener swingGainsChangedListener = createSwingGainsChangedListener(endEffectorControlModule);
         swingGainsChangedListener.variableChanged(null);

         endEffectorControlModule.setParameters(minJacobianDeterminantForSingularityEscape, singularityEscapeNullspaceMultiplierSwingLeg.getDoubleValue());
         footEndEffectorControlModules.put(robotSide, endEffectorControlModule);
      }
   }

   private RobotSide getUpcomingSupportLeg()
   {
      return upcomingSupportLeg.getEnumValue();
   }

   private RobotSide getSupportLeg()
   {
      return supportLeg.getEnumValue();
   }

   private void setUpStateMachine()
   {
      DoubleSupportState doubleSupportState = new DoubleSupportState(null);

      stateMachine.addState(doubleSupportState);

      ResetICPTrajectoryAction resetICPTrajectoryAction = new ResetICPTrajectoryAction();
      for (RobotSide robotSide : RobotSide.values)
      {
         EndEffectorControlModule swingEndEffectorControlModule = footEndEffectorControlModules.get(robotSide.getOppositeSide());
         StopWalkingCondition stopWalkingCondition = new StopWalkingCondition(swingEndEffectorControlModule);
         ResetSwingTrajectoryDoneAction resetSwingTrajectoryDoneAction = new ResetSwingTrajectoryDoneAction(swingEndEffectorControlModule);

         ArrayList<StateTransitionAction> stopWalkingStateTransitionActions = new ArrayList<StateTransitionAction>();
         stopWalkingStateTransitionActions.add(resetICPTrajectoryAction);
         stopWalkingStateTransitionActions.add(resetSwingTrajectoryDoneAction);

         State<WalkingState> transferState = new DoubleSupportState(robotSide);
         StateTransition<WalkingState> toDoubleSupport = new StateTransition<WalkingState>(doubleSupportState.getStateEnum(), stopWalkingCondition,
                                                            stopWalkingStateTransitionActions);
         transferState.addStateTransition(toDoubleSupport);
         StateTransition<WalkingState> toSingleSupport = new StateTransition<WalkingState>(singleSupportStateEnums.get(robotSide),
                                                            new DoneWithTransferCondition(robotSide));
         transferState.addStateTransition(toSingleSupport);
         stateMachine.addState(transferState);

         State<WalkingState> singleSupportState = new SingleSupportState(robotSide);
         StateTransition<WalkingState> toDoubleSupport2 = new StateTransition<WalkingState>(doubleSupportState.getStateEnum(), stopWalkingCondition,
                                                             stopWalkingStateTransitionActions);
         singleSupportState.addStateTransition(toDoubleSupport2);

         ContactablePlaneBody sameSideFoot = feet.get(robotSide);
         SingleSupportToTransferToCondition doneWithSingleSupportAndTransferToOppositeSideCondition = new SingleSupportToTransferToCondition(sameSideFoot, swingEndEffectorControlModule);
         StateTransition<WalkingState> toTransferOppositeSide = new StateTransition<WalkingState>(transferStateEnums.get(robotSide.getOppositeSide()),
               doneWithSingleSupportAndTransferToOppositeSideCondition, resetSwingTrajectoryDoneAction);
         singleSupportState.addStateTransition(toTransferOppositeSide);
      
         // Sometimes need transfer to same side when two steps are commanded on the same side. Otherwise, the feet cross over.
         ContactablePlaneBody oppositeSideFoot = feet.get(robotSide.getOppositeSide());
         SingleSupportToTransferToCondition doneWithSingleSupportAndTransferToSameSideCondition = new SingleSupportToTransferToCondition(oppositeSideFoot, swingEndEffectorControlModule);
         StateTransition<WalkingState> toTransferSameSide = new StateTransition<WalkingState>(transferStateEnums.get(robotSide),
               doneWithSingleSupportAndTransferToSameSideCondition, resetSwingTrajectoryDoneAction);
         singleSupportState.addStateTransition(toTransferSameSide);
         
         stateMachine.addState(singleSupportState);
      }

      for (RobotSide robotSide : RobotSide.values)
      {
         StateTransition<WalkingState> toTransfer = new StateTransition<WalkingState>(transferStateEnums.get(robotSide),
                                                       new DoneWithDoubleSupportCondition(robotSide));
         doubleSupportState.addStateTransition(toTransfer);
      }
   }

   private RigidBody baseForHeadOrientationControl;
   private int jacobianIdForHeadOrientationControl;
   
   public void setupManagers(VariousWalkingManagers variousWalkingManagers)
   {
      baseForHeadOrientationControl = fullRobotModel.getElevator();
      HeadOrientationManager headOrientationManager = variousWalkingManagers.getHeadOrientationManager();
      String[] headOrientationControlJointNames = walkingControllerParameters.getDefaultHeadOrientationControlJointNames(); 

      jacobianIdForHeadOrientationControl = headOrientationManager.createJacobian(fullRobotModel, baseForHeadOrientationControl, headOrientationControlJointNames);
   }
  
   public void initialize()
   {
      super.initialize();
      
      momentumBasedController.setMomentumControlModuleToUse(MOMENTUM_CONTROL_MODULE_TO_USE);
      momentumBasedController.setDelayTimeBeforeTrustingContacts(DELAY_TIME_BEFORE_TRUSTING_CONTACTS);
      
      initializeContacts();

      ChestOrientationManager chestOrientationManager = variousWalkingManagers.getChestOrientationManager();
      chestOrientationManager.turnOff();

      HeadOrientationManager headOrientationManager = variousWalkingManagers.getHeadOrientationManager();

      headOrientationManager.setUp(baseForHeadOrientationControl, jacobianIdForHeadOrientationControl);
      walkingHeadOrientationKp.set(walkingControllerParameters.getKpHeadOrientation()); 
      walkingHeadOrientationZeta.set(walkingControllerParameters.getZetaHeadOrientation());
      VariableChangedListener headGainsChangedListener = createHeadGainsChangedListener();
      headGainsChangedListener.variableChanged(null);
      
      FrameOrientation initialDesiredPelvisOrientation = new FrameOrientation(referenceFrames.getAnkleZUpFrame(getUpcomingSupportLeg()));
      initialDesiredPelvisOrientation.changeFrame(worldFrame);
      double yaw = initialDesiredPelvisOrientation.getYawPitchRoll()[0];
      initialDesiredPelvisOrientation.setYawPitchRoll(yaw, userDesiredPelvisPitch.getDoubleValue(), userDesiredPelvisRoll.getDoubleValue());
      desiredPelvisOrientation.set(initialDesiredPelvisOrientation);
      finalPelvisOrientationProvider.setOrientation(initialDesiredPelvisOrientation);    // yes, final. To make sure that the first swing phase has the right initial

      icpAndMomentumBasedController.computeCapturePoint();
      desiredICP.set(capturePoint.getFramePoint2dCopy());

      stateMachine.setCurrentState(WalkingState.DOUBLE_SUPPORT);

   }
   
   private VariableChangedListener createHeadGainsChangedListener()
   {
      VariableChangedListener ret = new VariableChangedListener()
      {
         public void variableChanged(YoVariable v)
         {
            double headKp = walkingHeadOrientationKp.getDoubleValue();
            double headZeta = walkingHeadOrientationZeta.getDoubleValue();
            double headKd = GainCalculator.computeDerivativeGain(headKp, headZeta);
            headOrientationManager.setControlGains(headKp, headKd); 
         }};
         
         walkingHeadOrientationKp.addVariableChangedListener(ret);
         walkingHeadOrientationZeta.addVariableChangedListener(ret);
      
      return ret;
   }
   
   
   private VariableChangedListener createSwingGainsChangedListener(final EndEffectorControlModule endEffectorControlModule)
   {
      VariableChangedListener ret = new VariableChangedListener()
      {
         public void variableChanged(YoVariable v)
         {
            endEffectorControlModule.setSwingGains(swingKpXY.getDoubleValue(), swingKpZ.getDoubleValue(), swingKpOrientation.getDoubleValue(), swingZeta.getDoubleValue());
            endEffectorControlModule.setMaxAccelerationAndJerk(swingMaxPositionAcceleration.getDoubleValue(), swingMaxPositionJerk.getDoubleValue(), 
                  swingMaxOrientationAcceleration.getDoubleValue(), swingMaxOrientationJerk.getDoubleValue());
         }};
         
         swingKpXY.addVariableChangedListener(ret);
         swingKpZ.addVariableChangedListener(ret);
         swingKpOrientation.addVariableChangedListener(ret);
         swingZeta.addVariableChangedListener(ret);
         
         swingMaxPositionAcceleration.addVariableChangedListener(ret);
         swingMaxPositionJerk.addVariableChangedListener(ret);
         swingMaxOrientationAcceleration.addVariableChangedListener(ret);
         swingMaxOrientationJerk.addVariableChangedListener(ret);
   
      return ret;
   }

   private void initializeContacts()
   {
      momentumBasedController.clearContacts();

      for (RobotSide robotSide : RobotSide.values)
      {
         setFlatFootContactState(robotSide);
      }
   }

   private class DoubleSupportState extends State<WalkingState>
   {
      private final RobotSide transferToSide;
      private final FramePoint2d desiredICPLocal = new FramePoint2d();
      private final FrameVector2d desiredICPVelocityLocal = new FrameVector2d();
      private final FramePoint2d ecmpLocal = new FramePoint2d();
      private final FramePoint2d capturePoint2d = new FramePoint2d();

      public DoubleSupportState(RobotSide transferToSide)
      {
         super((transferToSide == null) ? WalkingState.DOUBLE_SUPPORT : transferStateEnums.get(transferToSide));
         this.transferToSide = transferToSide;
      }

      @Override
      public void doAction()
      {
         doNotIntegrateAnkleAccelerations();
         
         checkForReinitialization();
         RobotSide trailingLegSide;
         RobotSide leadingLegSide;

         if (transferToSide == null)
         {
            trailingLegSide = RobotSide.LEFT;
            leadingLegSide = RobotSide.RIGHT;
         }
         else
         {
            trailingLegSide = transferToSide.getOppositeSide();
            leadingLegSide = transferToSide;
         }

         if ((footEndEffectorControlModules.get(transferToSide) != null) && walkOnTheEdgesManager.isEdgeTouchDownDone(leadingLegSide))
         {
            setFlatFootContactState(transferToSide);
         }

         // note: this has to be done before the ICP trajectory generator is initialized, since it is using nextFootstep
         // TODO: Make a LOADING state and clean all of these timing hacks up.
         doneFinishingSingleSupportTransfer.set(instantaneousCapturePointPlanner.isPerformingICPDoubleSupport());
         double estimatedTimeRemainingForState = instantaneousCapturePointPlanner.getEstimatedTimeRemainingForState(yoTime.getDoubleValue()); 
         
         if (doneFinishingSingleSupportTransfer.getBooleanValue() || estimatedTimeRemainingForState < 0.02)
         {
            upcomingFootstepList.checkForFootsteps(momentumBasedController.getPointPositionGrabber(), readyToGrabNextFootstep, upcomingSupportLeg, feet);
            checkForSteppingOnOrOff(transferToSide);
         }
         
         initializeICPPlannerIfNecessary();


         if (instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()) && (transferToSide == null))
         {
            desiredICPVelocity.set(0.0, 0.0);
         }
         else
         {
            desiredICPLocal.setToZero(desiredICP.getReferenceFrame());
            desiredICPVelocityLocal.setToZero(desiredICPVelocity.getReferenceFrame());
            ecmpLocal.setToZero(worldFrame);
            capturePoint.getFramePoint2dAndChangeFrameOfPackedPoint(capturePoint2d);
            
            instantaneousCapturePointPlanner.getICPPositionAndVelocity(
                  desiredICPLocal, desiredICPVelocityLocal, ecmpLocal, 
                  capturePoint2d, yoTime.getDoubleValue());
            
            
            if (transferToSide != null)
            {
               moveICPToInsideOfFootAtEndOfSwing(transferToSide.getOppositeSide(), 0.0, desiredICPLocal);
               limitICPToMiddleOfFootOrInside(transferToSide, desiredICPLocal);
            }
            else
            {
               limitICPToMiddleOfFootOrInside(RobotSide.LEFT, desiredICPLocal);
               limitICPToMiddleOfFootOrInside(RobotSide.RIGHT, desiredICPLocal);
            }
            
            desiredICP.set(desiredICPLocal);
            desiredICPVelocity.set(desiredICPVelocityLocal);

            desiredECMP.set(ecmpLocal);

            if (VISUALIZE)
            {
               ecmpViz.set(desiredECMP.getX(), desiredECMP.getY(), 0.0);
            }
         }
         
         initializeECMPbasedToeOffIfNotInitializedYet();

         // Only during the first few seconds, we will control the pelvis orientation based on midfeetZup
         if (((yoTime.getDoubleValue() - controllerInitializationTime.getDoubleValue()) < PELVIS_YAW_INITIALIZATION_TIME)
                 &&!alreadyBeenInDoubleSupportOnce.getBooleanValue())
         {
            setDesiredPelvisYawToAverageOfFeetOnStartupOnly(transferToSide);
         }

         if (userSetDesiredPelvis.getBooleanValue())
         {
            desiredPelvisOrientation.set(userDesiredPelvisYaw.getDoubleValue(), userDesiredPelvisPitch.getDoubleValue(), userDesiredPelvisRoll.getDoubleValue());
         }
         
         // keep desired pelvis orientation as it is
         desiredPelvisAngularVelocity.set(0.0, 0.0, 0.0);
         desiredPelvisAngularAcceleration.set(0.0, 0.0, 0.0);
      }

      public void initializeICPPlannerIfNecessary()
      {
         if (!icpTrajectoryHasBeenInitialized.getBooleanValue() && instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()))
         {
            initializingICPTrajectory.set(true);
            
            Pair<FramePoint2d, Double> finalDesiredICPAndTrajectoryTime = computeFinalDesiredICPAndTrajectoryTime();

            if (transferToSide != null)    // the only case left for determining the contact state of the trailing foot
            {
               FramePoint2d finalDesiredICP = finalDesiredICPAndTrajectoryTime.first();
               finalDesiredICP.changeFrame(desiredICP.getReferenceFrame());

               RobotSide trailingLeg = transferToSide.getOppositeSide();
               walkOnTheEdgesManager.updateToeOffStatusBasedOnICP(trailingLeg, desiredICP.getFramePoint2dCopy(), finalDesiredICP);

               if (walkOnTheEdgesManager.doToeOff())
               {
                  setOnToesContactState(trailingLeg);
               }
            }

            icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons);    // need to always update biped support polygons after a change to the contact states
            icpTrajectoryHasBeenInitialized.set(true);
         }
         else
         {           
            initializingICPTrajectory.set(false);
         }
      }

      public void initializeECMPbasedToeOffIfNotInitializedYet()
      {
         // the only case left for determining the contact state of the trailing foot
         if ((!ecmpBasedToeOffHasBeenInitialized.getBooleanValue()) && (transferToSide != null))
         {
            RobotSide trailingLeg = transferToSide.getOppositeSide();
            walkOnTheEdgesManager.updateToeOffStatusBasedOnECMP(trailingLeg, desiredECMP.getFramePoint2dCopy());

            if (walkOnTheEdgesManager.doToeOff())
            {
               double remainingToeOffTime = instantaneousCapturePointPlanner.getEstimatedTimeRemainingForState(yoTime.getDoubleValue());
               walkOnTheEdgesProviders.setToeOffFinalAngle(maximumConstantJerkFinalToeOffAngleComputer.getMaximumFeasibleConstantJerkFinalToeOffAngle
                     (walkOnTheEdgesProviders.getToeOffInitialAngle(trailingLeg), remainingToeOffTime));

               setOnToesContactState(trailingLeg);
               icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons);    // need to always update biped support polygons after a change to the contact states
               ecmpBasedToeOffHasBeenInitialized.set(true);

               totalEstimatedToeOffTimeProvider.set(remainingToeOffTime);
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
//            finalDesiredICPInWorld.set(finalDesiredICP);

            finalDesiredICPAndTrajectoryTime = new Pair<FramePoint2d, Double>(finalDesiredICP, trajectoryTime);
         }

         else if (rememberFinalICPFromSingleSupport.getBooleanValue() &&!finalDesiredICPInWorld.containsNaN())
         {
            FramePoint2d finalDesiredICP = finalDesiredICPInWorld.getFramePoint2dCopy();
            double trajectoryTime = transferTimeCalculationProvider.getValue();

            finalDesiredICPAndTrajectoryTime = new Pair<FramePoint2d, Double>(finalDesiredICP, trajectoryTime);
         }

         else
         {
            boolean inInitialize = false;
            TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForDoubleSupport(transferToSide, inInitialize );

            instantaneousCapturePointPlanner.initializeDoubleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue());

            FramePoint2d finalDesiredICP = instantaneousCapturePointPlanner.getFinalDesiredICP();
            double trajectoryTime = transferTimeCalculationProvider.getValue();

            finalDesiredICPInWorld.set(finalDesiredICP.changeFrameCopy(worldFrame));
            finalDesiredICPAndTrajectoryTime = new Pair<FramePoint2d, Double>(finalDesiredICP, trajectoryTime);
         }

         return finalDesiredICPAndTrajectoryTime;
      }

     
      public TransferToAndNextFootstepsData createTransferToAndNextFootstepDataForDoubleSupport(RobotSide transferToSide, boolean inInitialize)
      {
         Footstep transferFromFootstep = createFootstepFromFootAndContactablePlaneBody(referenceFrames.getFootFrame(transferToSide.getOppositeSide()),
                                            feet.get(transferToSide.getOppositeSide()));
         Footstep transferToFootstep = createFootstepFromFootAndContactablePlaneBody(referenceFrames.getFootFrame(transferToSide),
                                          feet.get(transferToSide));

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
         double doubleSupportInitialTransferDuration = 0.4;    // TODO: Magic Number
         transferToAndNextFootstepsData.setDoubleSupportInitialTransferDuration(doubleSupportInitialTransferDuration);
         boolean stopIfReachedEnd = (upcomingFootstepList.getNumberOfFootstepsToProvide() <= 3);    // TODO: Magic Number
         transferToAndNextFootstepsData.setStopIfReachedEnd(stopIfReachedEnd);

         if (VISUALIZE)
         {
            transferToAndNextFootstepsDataVisualizer.visualizeFootsteps(transferToAndNextFootstepsData);
         }

         return transferToAndNextFootstepsData;
      }

      @Override
      public void doTransitionIntoAction()
      {
         desiredECMPinSupportPolygon.set(false);
         ecmpBasedToeOffHasBeenInitialized.set(false);

         icpTrajectoryHasBeenInitialized.set(false);
         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: enteringDoubleSupportState");
         setSupportLeg(null);    // TODO: check if necessary

         if (walkOnTheEdgesManager.stayOnToes())   
         {
            setOnToesContactStates();
         }
         else if (transferToSide == null)
         {
            setFlatFootContactStates();
         }
         else if (walkOnTheEdgesManager.willLandOnToes())
         {
            setTouchdownOnToesContactState(transferToSide);
         }
         else if (walkOnTheEdgesManager.willLandOnHeel())
         {
            setTouchdownOnHeelContactState(transferToSide);
         }
         else
         {
            setFlatFootContactState(transferToSide); // still need to determine contact state for trailing leg. This is done in doAction as soon as the previous ICP trajectory is done
         }
         
         walkOnTheEdgesManager.reset();

         if (!instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()) && (transferToSide != null))
         {
            Footstep transferToFootstep = createFootstepFromFootAndContactablePlaneBody(referenceFrames.getFootFrame(transferToSide), feet.get(transferToSide));
            TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(transferToFootstep, transferToSide);

            instantaneousCapturePointPlanner.reInitializeSingleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue());
         }

         icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons);    // need to always update biped support polygons after a change to the contact states


         RobotSide transferToSideToUseInFootstepData = transferToSide;
         if (transferToSideToUseInFootstepData == null) transferToSideToUseInFootstepData = RobotSide.LEFT; //Arbitrary here.
         
         if (!centerOfMassHeightTrajectoryGenerator.hasBeenInitializedWithNextStep())
         {
//            System.out.println("Initializing centerOfMassHeightTrajectoryGenerator. transferToSide = " + transferToSide);

            boolean inInitialize = true;
            TransferToAndNextFootstepsData transferToAndNextFootstepsDataForDoubleSupport = createTransferToAndNextFootstepDataForDoubleSupport(transferToSideToUseInFootstepData, inInitialize);

            centerOfMassHeightTrajectoryGenerator.initialize(transferToAndNextFootstepsDataForDoubleSupport, transferToAndNextFootstepsDataForDoubleSupport.getTransferToSide(), null, getContactStatesList());
         }
      }

      @Override
      public void doTransitionOutOfAction()
      {
         // Before swinging a foot, relatch where all the other foot positions are. 
         // Otherwise there might be a jump.
         momentumBasedController.requestResetEstimatorPositionsToCurrent();
         
         desiredECMPinSupportPolygon.set(false);
         walkOnTheEdgesManager.reset();
         ecmpBasedToeOffHasBeenInitialized.set(false);

         alreadyBeenInDoubleSupportOnce.set(true);

         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: leavingDoubleSupportState");
         
         desiredICPVelocity.set(0.0, 0.0);
         manipulationControlModule.prepareForLocomotion();
      }
   }


   private void setDesiredPelvisYawToAverageOfFeetOnStartupOnly(RobotSide transferToSide)
   {
      FrameOrientation averageOrientation = new FrameOrientation(worldFrame);
      averageOrientationCalculator.computeAverageOrientation(averageOrientation, feet.get(RobotSide.LEFT).getPlaneFrame(),
              feet.get(RobotSide.RIGHT).getPlaneFrame(), worldFrame);

      double[] yawPitchRoll = averageOrientation.getYawPitchRoll();

      double yawOffset = 0.0;
      if (transferToSide != null)
         yawOffset = transferToSide.negateIfLeftSide(userDesiredPelvisYaw.getDoubleValue());
      
      averageOrientation.setYawPitchRoll(yawPitchRoll[0] + yawOffset, userDesiredPelvisPitch.getDoubleValue(), userDesiredPelvisRoll.getDoubleValue());
      desiredPelvisOrientation.set(averageOrientation);
   }

   private class SingleSupportState extends State<WalkingState>
   {
      private final RobotSide swingSide;
      private final FrameOrientation desiredPelvisOrientationToPack;
      private final FrameVector desiredPelvisAngularVelocityToPack;
      private final FrameVector desiredPelvisAngularAccelerationToPack;
      private final ErrorType[] singleSupportErrorToMonitor = new ErrorType[] {ErrorType.COM_Z, ErrorType.ICP_X, ErrorType.ICP_Y, ErrorType.PELVIS_ORIENTATION};

      private final FramePoint2d desiredICPLocal = new FramePoint2d();
      private final FrameVector2d desiredICPVelocityLocal = new FrameVector2d();
      private final FramePoint2d ecmpLocal = new FramePoint2d();
      private final FramePoint2d capturePoint2d = new FramePoint2d();
      
      public SingleSupportState(RobotSide robotSide)
      {
         super(singleSupportStateEnums.get(robotSide));
         this.swingSide = robotSide.getOppositeSide();
         this.desiredPelvisOrientationToPack = new FrameOrientation(worldFrame);
         this.desiredPelvisAngularVelocityToPack = new FrameVector(fullRobotModel.getRootJoint().getFrameAfterJoint());
         this.desiredPelvisAngularAccelerationToPack = new FrameVector(fullRobotModel.getRootJoint().getFrameAfterJoint());
      }

      @Override
      public void doAction()
      {
         integrateAnkleAccelerationsOnSwingLeg(swingSide);
         
         checkForReinitialization();
         desiredICPLocal.setToZero(desiredICP.getReferenceFrame());
         desiredICPVelocityLocal.setToZero(desiredICPVelocity.getReferenceFrame());
         ecmpLocal.setToZero(worldFrame);

         capturePoint.getFramePoint2dAndChangeFrameOfPackedPoint(capturePoint2d);

         instantaneousCapturePointPlanner.getICPPositionAndVelocity(
               desiredICPLocal, desiredICPVelocityLocal, ecmpLocal, 
               capturePoint2d, yoTime.getDoubleValue());
         
         RobotSide supportSide = swingSide.getOppositeSide();
         double swingTimeRemaining = swingTimeCalculationProvider.getValue() - stateMachine.timeInCurrentState();
         moveICPToInsideOfFootAtEndOfSwing(supportSide, swingTimeRemaining, desiredICPLocal);
         
         desiredICP.set(desiredICPLocal);
         desiredICPVelocity.set(desiredICPVelocityLocal);
         
         desiredECMP.set(ecmpLocal);

         if (VISUALIZE)
         {
            ecmpViz.set(desiredECMP.getX(), desiredECMP.getY(), 0.0);
         }

         pelvisOrientationTrajectoryGenerator.compute(stateMachine.timeInCurrentState());
         pelvisOrientationTrajectoryGenerator.get(desiredPelvisOrientationToPack);
         pelvisOrientationTrajectoryGenerator.packAngularVelocity(desiredPelvisAngularVelocityToPack);
         pelvisOrientationTrajectoryGenerator.packAngularAcceleration(desiredPelvisAngularAccelerationToPack);
         desiredPelvisOrientation.set(desiredPelvisOrientationToPack);
         desiredPelvisAngularVelocity.set(desiredPelvisAngularVelocityToPack);
         desiredPelvisAngularAcceleration.set(desiredPelvisAngularAccelerationToPack);

         if (stateMachine.timeInCurrentState() < 0.5 * swingTimeCalculationProvider.getValue() && footEndEffectorControlModules.get(swingSide).isInSingularityNeighborhood())
         {
            footEndEffectorControlModules.get(swingSide).doSingularityEscape(true);
         }
      }

      @Override
      public void doTransitionIntoAction()
      {
         hasICPPlannerFinished.set(false);
         
         footSwitches.get(swingSide).reset();

         Footstep nextFootstep = upcomingFootstepList.getNextFootstep();
         boolean nextFootstepHasBeenReplaced = false;
         Footstep oldNextFootstep = nextFootstep;

         if (!nextFootstep.getTrustHeight())
         {
            // TODO: This might be better placed somewhere else.
            // TODO: Do more than just step at the previous ankle height.
            // Probably do something a little smarter like take a cautious high step.
            // Or we should have a mode that the user can set on how cautious to step.

            FramePoint supportAnklePosition = new FramePoint(referenceFrames.getAnkleZUpFrame(swingSide.getOppositeSide()));
            supportAnklePosition.changeFrame(nextFootstep.getReferenceFrame());
            double newHeight = supportAnklePosition.getZ() + swingAboveSupportAnkle.getDoubleValue();

            nextFootstep = Footstep.copyButChangeHeight(nextFootstep, newHeight);
            nextFootstepHasBeenReplaced = true;
         }

         walkOnTheEdgesManager.updateEdgeTouchdownStatus(swingSide.getOppositeSide(), nextFootstep);
         
         if (walkOnTheEdgesManager.willLandOnEdge())
         {
            nextFootstep = walkOnTheEdgesManager.createFootstepForEdgeTouchdown(nextFootstep);
            walkOnTheEdgesManager.updateTouchdownInitialAngularVelocity();
            nextFootstepHasBeenReplaced = true;
         }

         if (nextFootstepHasBeenReplaced)
            switchTrajectoryParametersMapping(oldNextFootstep, nextFootstep);

         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: enteringSingleSupportState");
         RobotSide supportSide = swingSide.getOppositeSide();

         setSupportLeg(supportSide);

         if (walkOnTheEdgesManager.stayOnToes())
         {
            setOnToesContactState(supportSide);
         }
         else
         {
            setFlatFootContactState(supportSide);
         }

         swingFootFinalPositionProvider.set(nextFootstep.getPositionInFrame(worldFrame));

         SideDependentList<Transform3D> footToWorldTransform = new SideDependentList<Transform3D>();
         for (RobotSide robotSide : RobotSide.values)
         {
            Transform3D transform = feet.get(robotSide).getBodyFrame().getTransformToDesiredFrame(worldFrame);
            footToWorldTransform.set(robotSide, transform);
         }

         Vector3d initialVectorPosition = new Vector3d();
         footToWorldTransform.get(supportSide.getOppositeSide()).get(initialVectorPosition);
         FramePoint initialFramePosition = new FramePoint(worldFrame, initialVectorPosition);
         FramePoint footFinalPosition = new FramePoint(worldFrame);
         swingFootFinalPositionProvider.get(footFinalPosition);
         double stepDistance = initialFramePosition.distance(footFinalPosition);
         swingTimeCalculationProvider.setSwingTime(stepDistance);
         transferTimeCalculationProvider.setTransferTime();

         trajectoryParametersProvider.set(mapFromFootstepsToTrajectoryParameters.get(nextFootstep));
         finalFootOrientationProviders.get(swingSide).setOrientation(nextFootstep.getOrientationInFrame(worldFrame));

         FrameOrientation orientation = new FrameOrientation(desiredPelvisOrientation.getReferenceFrame());
         desiredPelvisOrientation.get(orientation);
         initialPelvisOrientationProvider.setOrientation(orientation);

         FrameOrientation finalPelvisOrientation = nextFootstep.getOrientationInFrame(worldFrame);
//         finalPelvisOrientation.setYawPitchRoll(0.5 * finalPelvisOrientation.getYaw() + 0.5 * orientation.getYaw(), 0.0, 0.0);
         FramePoint swingFootFinalPosition = nextFootstep.getPositionInFrame(referenceFrames.getAnkleZUpFrame(swingSide.getOppositeSide()));
         FrameVector supportFootToSwingFoot = new FrameVector(swingFootFinalPosition);
         Vector3d temp = supportFootToSwingFoot.getVectorCopy();
         double desiredPelvisYawAngle = 0.0;
         if (Math.abs(temp.x) > 0.1)
         {
            desiredPelvisYawAngle = Math.atan2(temp.y, temp.x);
            desiredPelvisYawAngle -= swingSide.negateIfRightSide(Math.PI/2.0);
         }
         finalPelvisOrientation.setYawPitchRoll(finalPelvisOrientation.getYaw() + userDesiredPelvisYaw.getDoubleValue() * desiredPelvisYawAngle, userDesiredPelvisPitch.getDoubleValue(), userDesiredPelvisRoll.getDoubleValue());
         finalPelvisOrientationProvider.setOrientation(finalPelvisOrientation);
         pelvisOrientationTrajectoryGenerator.initialize();

         double stepPitch = nextFootstep.getOrientationInFrame(worldFrame).getYawPitchRoll()[1];
         walkOnTheEdgesProviders.setToeOffInitialAngle(swingSide, stepPitch);

         FramePoint centerOfMass = new FramePoint(referenceFrames.getCenterOfMassFrame());
         centerOfMass.changeFrame(worldFrame);
         ContactablePlaneBody supportFoot = feet.get(supportSide);
         Transform3D supportFootToWorldTransform = footToWorldTransform.get(supportSide);
         double footHeight = DesiredFootstepCalculatorTools.computeMinZPointInFrame(supportFootToWorldTransform, supportFoot, worldFrame).getZ();
         double comHeight = centerOfMass.getZ() - footHeight;
         double omega0 = CapturePointCalculator.computeOmega0ConstantHeight(gravity, comHeight);
         icpAndMomentumBasedController.setOmega0(omega0);
         icpAndMomentumBasedController.computeCapturePoint();

         if (walkingControllerParameters.resetDesiredICPToCurrentAtStartOfSwing())
         {
            desiredICP.set(capturePoint.getFramePoint2dCopy());    // TODO: currently necessary for stairs because of the omega0 jump, but should get rid of this
         }

         TransferToAndNextFootstepsData transferToAndNextFootstepsData = createTransferToAndNextFootstepDataForSingleSupport(nextFootstep, swingSide);
         FramePoint2d finalDesiredICP = getSingleSupportFinalDesiredICPForWalking(transferToAndNextFootstepsData, swingSide);

         setContactStateForSwing(swingSide);
         setSupportLeg(supportSide);
         icpAndMomentumBasedController.updateBipedSupportPolygons(bipedSupportPolygons);

         // Shouldn't have to do this init anymore since it's done above...
         // icpTrajectoryGenerator.initialize(desiredICP.getFramePoint2dCopy(), finalDesiredICP, swingTimeCalculationProvider.getValue(), omega0,
         // amountToBeInsideSingleSupport.getDoubleValue(), getSupportLeg(), yoTime.getDoubleValue());

         centerOfMassHeightTrajectoryGenerator.initialize(transferToAndNextFootstepsData, getSupportLeg(), nextFootstep, getContactStatesList());

         if (DEBUG)
            System.out.println("WalkingHighLevelHumanoidController: nextFootstep will change now!");
         readyToGrabNextFootstep.set(true);
      }

      private void switchTrajectoryParametersMapping(Footstep oldFootstep, Footstep newFootstep)
      {
         mapFromFootstepsToTrajectoryParameters.put(newFootstep, mapFromFootstepsToTrajectoryParameters.get(oldFootstep));
         mapFromFootstepsToTrajectoryParameters.remove(oldFootstep);
      }

      @Override
      public void doTransitionOutOfAction()
      {
         if (DEBUG)
            System.out.println("WalkingHighLevelController: leavingDoubleSupportState");

         upcomingFootstepList.notifyComplete();

         // ContactableBody swingFoot = contactablePlaneBodies.get(swingSide);
         // Footstep desiredFootstep = desiredFootstepCalculator.updateAndGetDesiredFootstep(swingSide.getOppositeSide());
         // contactStates.get(swingFoot).setContactPoints(desiredFootstep.getExpectedContactPoints());
         // updateFootStateMachines(swingFoot);
      }
   }


   public class DoneWithDoubleSupportCondition implements StateTransitionCondition
   {
      private final RobotSide transferToSide;

      public DoneWithDoubleSupportCondition(RobotSide robotSide)
      {
         this.transferToSide = robotSide;
      }

      public boolean checkCondition()
      {
         if (readyToGrabNextFootstep.getBooleanValue())
            return false;
         else
         {
            boolean doubleSupportTimeHasPassed = stateMachine.timeInCurrentState() > transferTimeCalculationProvider.getValue();
            boolean transferringToThisRobotSide = transferToSide == getUpcomingSupportLeg();

            return transferringToThisRobotSide && doubleSupportTimeHasPassed;
         }
      }
   }


   public class DoneWithTransferCondition implements StateTransitionCondition
   {
      RobotSide robotSide;
      public DoneWithTransferCondition(RobotSide robotSide)
      {
         this.robotSide = robotSide;
      }
      public boolean checkCondition()
      {
         if (walkingControllerParameters.checkOrbitalEnergyCondition())
         {
            // TODO: not really nice, but it'll do:
            FlatThenPolynomialCoMHeightTrajectoryGenerator flatThenPolynomialCoMHeightTrajectoryGenerator =
               (FlatThenPolynomialCoMHeightTrajectoryGenerator) centerOfMassHeightTrajectoryGenerator;
            double orbitalEnergy = flatThenPolynomialCoMHeightTrajectoryGenerator.computeOrbitalEnergyIfInitializedNow(getUpcomingSupportLeg());

            // return transferICPTrajectoryDone.getBooleanValue() && orbitalEnergy > minOrbitalEnergy;
            return icpTrajectoryHasBeenInitialized.getBooleanValue() && (orbitalEnergy > minOrbitalEnergyForSingleSupport.getDoubleValue());
         }
         else
         {
            return icpTrajectoryHasBeenInitialized.getBooleanValue() && instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue());
         }
      }
   }


   private class SingleSupportToTransferToCondition extends DoneWithSingleSupportCondition
   {
      private final ContactablePlaneBody nextSwingFoot;
      
      public SingleSupportToTransferToCondition(ContactablePlaneBody nextSwingFoot, EndEffectorControlModule endEffectorControlModule)
      {
         super(endEffectorControlModule);
         
         this.nextSwingFoot = nextSwingFoot;
      }
      
      public boolean checkCondition()
      {
         Footstep nextFootstep = upcomingFootstepList.getNextNextFootstep(); 
         if (nextFootstep == null) return super.checkCondition();
         
         ContactablePlaneBody nextSwingFoot = nextFootstep.getBody();
         if (this.nextSwingFoot != nextSwingFoot ) return false;

         boolean condition = super.checkCondition();
         return condition;
      }
   
   }
      
   private class DoneWithSingleSupportCondition implements StateTransitionCondition
   {

      public DoneWithSingleSupportCondition(EndEffectorControlModule endEffectorControlModule)
      {
      }
      
      public boolean checkCondition()
      {
         RobotSide swingSide = getSupportLeg().getOppositeSide();
         hasMinimumTimePassed.set(hasMinimumTimePassed());
         if(!walkOnTheEdgesManager.isEdgeTouchDownDone(swingSide)) return false;   
         if (!hasICPPlannerFinished.getBooleanValue())
         {
            hasICPPlannerFinished.set(instantaneousCapturePointPlanner.isDone(yoTime.getDoubleValue()));
            if (hasICPPlannerFinished.getBooleanValue())
            {
               timeThatICPPlannerFinished.set(yoTime.getDoubleValue());
            }
         }
         
         FootSwitchInterface footSwitch = footSwitches.get(swingSide);

         // TODO probably make all FootSwitches in this class be HeelSwitches and get rid of instanceof
         boolean footSwitchActivated;
         
         if (walkOnTheEdgesManager.willLandOnToes())
         {
            if (!(footSwitch instanceof ToeSwitch))
            {
               throw new RuntimeException("toe touchdown should not be used if Robot is not using a ToeSwitch.");
            }
            
            ToeSwitch toeSwitch = (ToeSwitch) footSwitch;
            footSwitchActivated = toeSwitch.hasToeHitGround();
         }
         else if (walkOnTheEdgesManager.willLandOnHeel())
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

         if (hasMinimumTimePassed.getBooleanValue() && justFall.getBooleanValue()) return true;

         //Just switch states if icp is done, plus a little bit more. You had enough time and more isn't going to do any good.

         if (DO_TRANSITION_WHEN_TIME_IS_UP)
         {
            if (hasICPPlannerFinished.getBooleanValue() && (yoTime.getDoubleValue() > timeThatICPPlannerFinished.getDoubleValue() + dwellInSingleSupportDuration.getDoubleValue())) return true;
         }
         
         if (walkingControllerParameters.finishSwingWhenTrajectoryDone())
         {
            return  hasMinimumTimePassed.getBooleanValue() && (hasICPPlannerFinished.getBooleanValue() || footSwitchActivated);
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


   private class ResetSwingTrajectoryDoneAction implements StateTransitionAction
   {
      private EndEffectorControlModule endEffectorControlModule;

      public ResetSwingTrajectoryDoneAction(EndEffectorControlModule endEffectorControlModule)
      {
         this.endEffectorControlModule = endEffectorControlModule;
      }

      public void doTransitionAction()
      {
         endEffectorControlModule.resetTrajectoryDone();
      }
   }


   private class StopWalkingCondition extends DoneWithSingleSupportCondition
   {
      public StopWalkingCondition(EndEffectorControlModule endEffectorControlModule)
      {
         super(endEffectorControlModule);
      }

      public boolean checkCondition()
      {
         Footstep nextFootstep = upcomingFootstepList.getNextFootstep();
         boolean readyToStopWalking = (upcomingFootstepList.isFootstepProviderEmpty() && (nextFootstep == null))
                                      && ((getSupportLeg() == null) || super.checkCondition());

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
      double trailingFootToLeadingFootFactor = 0.5;    // 0.25;
      for (RobotSide robotSide : RobotSide.values)
      {
         FramePoint2d centroid = new FramePoint2d(ret.getReferenceFrame());
         FrameConvexPolygon2d footPolygon = computeFootPolygon(robotSide, referenceFrames.getAnkleZUpFrame(robotSide));
         footPolygon.getCentroid(centroid);
         centroid.changeFrame(ret.getReferenceFrame());
         if (robotSide == getUpcomingSupportLeg())
            centroid.scale(trailingFootToLeadingFootFactor);
         else
            centroid.scale(1.0 - trailingFootToLeadingFootFactor);
         ret.add(centroid);
      }

      return ret;
   }

   private FramePoint2d getSingleSupportFinalDesiredICPForWalking(TransferToAndNextFootstepsData transferToAndNextFootstepsData, RobotSide swingSide)
   {
      ReferenceFrame referenceFrame = worldFrame;

      // FramePoint2d initialDesiredICP = desiredICP.getFramePoint2dCopy();
      // initialDesiredICP.changeFrame(referenceFrame);

      instantaneousCapturePointPlanner.initializeSingleSupport(transferToAndNextFootstepsData, yoTime.getDoubleValue());

      FramePoint2d finalDesiredICP = instantaneousCapturePointPlanner.getFinalDesiredICP();
      finalDesiredICP.changeFrame(referenceFrame);

      finalDesiredICPInWorld.set(finalDesiredICP);

      RobotSide supportSide = swingSide.getOppositeSide();
      walkOnTheEdgesManager.updateOnToesTriangle(finalDesiredICP, supportSide);

      FramePoint2d icpWayPoint;
      if (walkOnTheEdgesManager.doToeOffIfPossible() && walkOnTheEdgesManager.isOnToesTriangleLargeEnough())
      {
         FramePoint toeOffPoint = new FramePoint(worldFrame);
         ContactablePlaneBody supportFoot = feet.get(supportSide);
         List<FramePoint> toePoints = getToePoints(supportFoot);
         toeOffPoint.interpolate(toePoints.get(0), toePoints.get(1), 0.5);
         FramePoint2d toeOffPoint2d = toeOffPoint.toFramePoint2d();
         double toeOffPointToFinalDesiredFactor = 0.2;    // TODO: magic number
         FramePoint2d desiredToeOffCoP = new FramePoint2d(worldFrame);
         desiredToeOffCoP.interpolate(toeOffPoint2d, finalDesiredICP, toeOffPointToFinalDesiredFactor);
         icpWayPoint = EquivalentConstantCoPCalculator.computeICPPositionWithConstantCMP(finalDesiredICP, desiredToeOffCoP, -transferTimeCalculationProvider.getValue(),
                 icpAndMomentumBasedController.getOmega0());
      }
      else
      {
         icpWayPoint = EquivalentConstantCoPCalculator.computeIntermediateICPWithConstantCMP(desiredICP.getFramePoint2dCopy(), finalDesiredICP,
                 swingTimeCalculationProvider.getValue() + transferTimeCalculationProvider.getValue(), swingTimeCalculationProvider.getValue(),
                 icpAndMomentumBasedController.getOmega0());
      }

      return icpWayPoint;
   }

   public TransferToAndNextFootstepsData createTransferToAndNextFootstepDataForSingleSupport(Footstep transferToFootstep, RobotSide swingSide)
   {
      Footstep transferFromFootstep = createFootstepFromFootAndContactablePlaneBody(referenceFrames.getFootFrame(swingSide.getOppositeSide()),
                                         feet.get(swingSide.getOppositeSide()));

      FrameConvexPolygon2d footPolygon;
      ContactablePlaneBody contactableBody = feet.get(swingSide);
      if (walkOnTheEdgesManager.stayOnToes())
      {
         List<FramePoint> contactPoints = getToePoints(contactableBody);
         footPolygon = FrameConvexPolygon2d.constructByProjectionOntoXYPlane(contactPoints, referenceFrames.getSoleFrame(swingSide));
      }
      else
      {
         footPolygon = new FrameConvexPolygon2d(contactableBody.getContactPoints2d());
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
      double doubleSupportInitialTransferDuration = 0.4;    // TODO: Magic Number
      transferToAndNextFootstepsData.setDoubleSupportInitialTransferDuration(doubleSupportInitialTransferDuration);
      boolean stopIfReachedEnd = (upcomingFootstepList.getNumberOfFootstepsToProvide() <= 3);    // TODO: Magic Number
      transferToAndNextFootstepsData.setStopIfReachedEnd(stopIfReachedEnd);

      if (VISUALIZE)
      {
         transferToAndNextFootstepsDataVisualizer.visualizeFootsteps(transferToAndNextFootstepsData);
      }

      return transferToAndNextFootstepsData;
   }

   private List<FramePoint> getToePoints(ContactablePlaneBody supportFoot)
   {
      FrameVector forward = new FrameVector(supportFoot.getPlaneFrame(), 1.0, 0.0, 0.0);
      int nToePoints = 2;
      List<FramePoint> toePoints = DesiredFootstepCalculatorTools.computeMaximumPointsInDirection(supportFoot.getContactPointsCopy(), forward, nToePoints);
      for (FramePoint toePoint : toePoints)
      {
         toePoint.changeFrame(worldFrame);
      }

      return toePoints;
   }

   private void setSupportLeg(RobotSide supportLeg)
   {
      this.supportLeg.set(supportLeg);
   }

   public void doMotionControl()
   { 
      if (loopControllerForever.getBooleanValue())
      {
         while(true)
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
   public void doMotionControlInternal()
   {
      for (RobotSide robotSide : RobotSide.values)
         requestSupportFootToHoldPosition.get(robotSide).set(footSwitches.get(robotSide).computeFootLoadPercentage() < footLoadThresholdToHoldPosition.getDoubleValue());
      
      momentumBasedController.doPrioritaryControl();
      super.callUpdatables();

      icpAndMomentumBasedController.computeCapturePoint();
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();

      controlledCoMHeightAcceleration.set(computeDesiredCoMHeightAcceleration(desiredICPVelocity.getFrameVector2dCopy()));

      doFootControl();
      doArmControl();
      doHeadControl();
      doLidarJointControl();
//    doCoMControl(); //TODO: Should we be doing this too?
      doChestControl();
      setICPBasedMomentumRateOfChangeControlModuleInputs();
      doPelvisControl();
      doJointPositionControl();

      setTorqueControlJointsToZeroDersiredAcceleration();

      momentumBasedController.doSecondaryControl();
      
      momentumBasedController.doPassiveKneeControl();
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
      icpBasedMomentumRateOfChangeControlModule.getDesiredCapturePointTrajectoryInputPort().setData(capturePointTrajectoryData);

      icpBasedMomentumRateOfChangeControlModule.getSupportLegInputPort().setData(getSupportLeg());

      icpBasedMomentumRateOfChangeControlModule.getDesiredCenterOfMassHeightAccelerationInputPort().setData(controlledCoMHeightAcceleration.getDoubleValue());

      icpBasedMomentumRateOfChangeControlModule.startComputation();
      icpBasedMomentumRateOfChangeControlModule.waitUntilComputationIsDone();
      MomentumRateOfChangeData momentumRateOfChangeData = icpBasedMomentumRateOfChangeControlModule.getMomentumRateOfChangeOutputPort().getData();
      momentumBasedController.setDesiredRateOfChangeOfMomentum(momentumRateOfChangeData);
   }

   // Temporary objects to reduce garbage collection.
   private final CoMHeightPartialDerivativesData coMHeightPartialDerivatives = new CoMHeightPartialDerivativesData();
   private final ContactStatesAndUpcomingFootstepData centerOfMassHeightInputData = new ContactStatesAndUpcomingFootstepData();

   private double computeDesiredCoMHeightAcceleration(FrameVector2d desiredICPVelocity)
   {
      ReferenceFrame frame = worldFrame;

      centerOfMassHeightInputData.setCenterOfMassAndPelvisZUpFrames(momentumBasedController.getCenterOfMassFrame(), momentumBasedController.getPelvisZUpFrame());

      List<? extends PlaneContactState> contactStatesList = getContactStatesList();

      centerOfMassHeightInputData.setContactStates(contactStatesList);

      centerOfMassHeightInputData.setSupportLeg(getSupportLeg());

      Footstep nextFootstep = upcomingFootstepList.getNextFootstep();
      centerOfMassHeightInputData.setUpcomingFootstep(nextFootstep);

      centerOfMassHeightTrajectoryGenerator.solve(coMHeightPartialDerivatives, centerOfMassHeightInputData);

      FramePoint comPosition = new FramePoint(referenceFrames.getCenterOfMassFrame());
      FrameVector comVelocity = new FrameVector(frame);
      centerOfMassJacobian.packCenterOfMassVelocity(comVelocity);
      comPosition.changeFrame(frame);
      comVelocity.changeFrame(frame);

      // TODO: use current omega0 instead of previous
      FrameVector2d comXYVelocity = comVelocity.toFrameVector2d();
      FrameVector2d comXYAcceleration = new FrameVector2d(desiredICPVelocity);
      comXYAcceleration.sub(comXYVelocity);
      comXYAcceleration.scale(icpAndMomentumBasedController.getOmega0());    // MathTools.square(omega0.getDoubleValue()) * (com.getX() - copX);

      // FrameVector2d comd2dSquared = new FrameVector2d(comXYVelocity.getReferenceFrame(), comXYVelocity.getX() * comXYVelocity.getX(), comXYVelocity.getY() * comXYVelocity.getY());

      CoMHeightTimeDerivativesData comHeightDataBeforeSmoothing = new CoMHeightTimeDerivativesData();
      CoMHeightTimeDerivativesData comHeightDataAfterSmoothing = new CoMHeightTimeDerivativesData();

      CoMXYTimeDerivativesData comXYTimeDerivatives = new CoMXYTimeDerivativesData();

      comXYTimeDerivatives.setCoMXYPosition(comPosition.toFramePoint2d());
      comXYTimeDerivatives.setCoMXYVelocity(comXYVelocity);
      comXYTimeDerivatives.setCoMXYAcceleration(comXYAcceleration);

      coMHeightTimeDerivativesCalculator.computeCoMHeightTimeDerivatives(comHeightDataBeforeSmoothing, comXYTimeDerivatives, coMHeightPartialDerivatives);

      coMHeightTimeDerivativesSmoother.smooth(comHeightDataAfterSmoothing, comHeightDataBeforeSmoothing);

      FramePoint centerOfMassHeightPoint = new FramePoint(worldFrame);
      comHeightDataAfterSmoothing.getComHeight(centerOfMassHeightPoint);
      double zDesired = centerOfMassHeightPoint.getZ();

      double zdDesired = comHeightDataAfterSmoothing.getComHeightVelocity();
      double zddFeedForward = comHeightDataAfterSmoothing.getComHeightAcceleration();

      double zCurrent = comPosition.getZ();
      double zdCurrent = comVelocity.getZ();
      
      if (controlPelvisHeightInsteadOfCoMHeight.getBooleanValue())
      {
         FramePoint pelvisPosition = new FramePoint(referenceFrames.getPelvisFrame());
         pelvisPosition.changeFrame(frame);
         zCurrent = pelvisPosition.getZ(); 
         
         zdCurrent = comVelocity.getZ(); // Just use com velocity for now for damping...
      }

      double zddDesired = centerOfMassHeightController.compute(zCurrent, zDesired, zdCurrent, zdDesired) + zddFeedForward;

      
      for (RobotSide robotSide : RobotSide.values)
      {
         EndEffectorControlModule endEffectorControlModule = footEndEffectorControlModules.get(robotSide);
         
         if (endEffectorControlModule.getCurrentConstraintType() == ConstraintType.FULL && endEffectorControlModule.isInSingularityNeighborhood())
         {
            // Can't achieve a desired height acceleration
            zddDesired = 0.0;
            double zTreshold = 0.01;

            if (zDesired >= zCurrent - zTreshold)
            {
               // Can't achieve the desired height, just lock the knee
               endEffectorControlModule.doSingularityEscape(singularityEscapeNullspaceMultiplierSupportLegLocking.getDoubleValue());
            }
            else
            {
               // Do the singularity escape before trying to achieve the desired height
               endEffectorControlModule.doSingularityEscape(singularityEscapeNullspaceMultiplierSupportLeg.getDoubleValue());
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
         
//         YoPlaneContactState contactState = contactStates.get(contactablePlaneBody);
         if (contactState.inContact())
            contactStatesList.add(contactState);
      }

      return contactStatesList;
   }

   private void setOnToesContactStates()
   {
      for (RobotSide robotSide : RobotSide.values)
         setOnToesContactState(robotSide);
   }

   private final FrameVector zUp = new FrameVector();

   private void setOnToesContactState(RobotSide robotSide)
   {
      // TODO cannot use world or elevator frames with non perfect sensors... some bug to fix obviously
      zUp.set(referenceFrames.getAnkleZUpFrame(robotSide), 0.0, 0.0, 1.0);
      footEndEffectorControlModules.get(robotSide).setContactState(ConstraintType.TOES, zUp);
   }

   private void setTouchdownOnHeelContactState(RobotSide robotSide)
   {
      // TODO cannot use world or elevator frames with non perfect sensors... some bug to fix obviously
      zUp.set(referenceFrames.getAnkleZUpFrame(robotSide), 0.0, 0.0, 1.0);
      footEndEffectorControlModules.get(robotSide).setContactState(ConstraintType.HEEL_TOUCHDOWN, zUp);
   }

   private void setTouchdownOnToesContactState(RobotSide robotSide)
   {
      // TODO cannot use world or elevator frames with non perfect sensors... some bug to fix obviously
      zUp.set(referenceFrames.getAnkleZUpFrame(robotSide), 0.0, 0.0, 1.0);
      footEndEffectorControlModules.get(robotSide).setContactState(ConstraintType.TOES_TOUCHDOWN, zUp);
   }

   
   private void setFlatFootContactStates()
   {
      for (RobotSide robotSide : RobotSide.values)
         setFlatFootContactState(robotSide);
   }

   private void setFlatFootContactState(RobotSide robotSide)
   {
      // TODO cannot use world or elevator frames with non perfect sensors... some bug to fix obviously
      zUp.set(referenceFrames.getAnkleZUpFrame(robotSide), 0.0, 0.0, 1.0);
      footEndEffectorControlModules.get(robotSide).setContactState(ConstraintType.FULL, zUp);
   }

   private void setContactStateForSwing(RobotSide robotSide)
   {
      EndEffectorControlModule endEffectorControlModule = footEndEffectorControlModules.get(robotSide);
      endEffectorControlModule.doSingularityEscape(true);
      endEffectorControlModule.setContactState(ConstraintType.UNCONSTRAINED);
   }

   private final List<FramePoint> tempContactPoints = new ArrayList<FramePoint>();
   private final FrameConvexPolygon2d tempFootPolygon = new FrameConvexPolygon2d(worldFrame);
   
   // TODO: should probably precompute this somewhere else
   private FrameConvexPolygon2d computeFootPolygon(RobotSide robotSide, ReferenceFrame referenceFrame)
   {
      momentumBasedController.getContactPoints(feet.get(robotSide), tempContactPoints);
      tempFootPolygon.updateByProjectionOntoXYPlane(tempContactPoints, referenceFrame);

      return tempFootPolygon;
   }

   private void checkForSteppingOnOrOff(RobotSide transferToSide)
   {
      if ((transferToSide != null) && upcomingFootstepList.hasNextFootsteps())
      {
         ReferenceFrame initialSoleFrame;

         // NOTE: the foot may have moved so its ideal to get the previous footstep, rather than the current foot frame, if possible
         if (upcomingFootstepList.doesNextFootstepListHaveFewerThanTwoElements())
         {
            initialSoleFrame = feet.get(transferToSide.getOppositeSide()).getPlaneFrame();
         }
         else
         {
            initialSoleFrame = upcomingFootstepList.getFootstepTwoBackFromNextFootstepList().getSoleReferenceFrame();
         }

         Footstep nextFootstep = upcomingFootstepList.getNextFootstep();
         ReferenceFrame finalSoleFrame = nextFootstep.getSoleReferenceFrame();

         boolean isBlindWalking = variousWalkingProviders.getFootstepProvider().isBlindWalking();
         
         if (isBlindWalking)
         {
            this.stepOnOrOff.set(false);
         }
         else
         {
            this.stepOnOrOff.set(TwoWaypointTrajectoryUtils.stepOnOrOff(initialSoleFrame, finalSoleFrame));
         }
         
         if (stepOnOrOff.getBooleanValue())
         {
            TrajectoryParameters trajectoryParameters = new SimpleTwoWaypointTrajectoryParameters(TrajectoryWaypointGenerationMethod.STEP_ON_OR_OFF);
            mapFromFootstepsToTrajectoryParameters.put(nextFootstep, trajectoryParameters);
         }
      }
   }

   private static Footstep createFootstepFromFootAndContactablePlaneBody(ReferenceFrame footReferenceFrame, ContactablePlaneBody contactablePlaneBody)
   {
      FramePose framePose = new FramePose(footReferenceFrame);
      framePose.changeFrame(worldFrame);

      PoseReferenceFrame poseReferenceFrame = new PoseReferenceFrame("poseReferenceFrame", framePose);

      ReferenceFrame soleReferenceFrame = FootstepUtils.createSoleFrame(poseReferenceFrame, contactablePlaneBody);
      List<FramePoint> expectedContactPoints = FootstepUtils.getContactPointsInFrame(contactablePlaneBody, soleReferenceFrame);
      boolean trustHeight = true;

      Footstep footstep = new Footstep(contactablePlaneBody, poseReferenceFrame, soleReferenceFrame, expectedContactPoints, trustHeight);

      return footstep;
   }
   
   private void checkForReinitialization()
   {
      if (reinitializeControllerProvider == null) return;
      
      if(reinitializeControllerProvider.isReinitializeRequested() && (stateMachine.getCurrentStateEnum() == WalkingState.DOUBLE_SUPPORT))
      {
         reinitializeControllerProvider.set(false);
         initialize();
      }
   }
   
   public void integrateAnkleAccelerationsOnSwingLeg(RobotSide swingSide)
   {
      fullRobotModel.getLegJoint(swingSide, LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(true);
      fullRobotModel.getLegJoint(swingSide, LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(true);
      fullRobotModel.getLegJoint(swingSide.getOppositeSide(), LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(false);
      fullRobotModel.getLegJoint(swingSide.getOppositeSide(), LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(false);
   }
   
   public void doNotIntegrateAnkleAccelerations()
   {
      fullRobotModel.getLegJoint(RobotSide.LEFT, LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(false);
      fullRobotModel.getLegJoint(RobotSide.LEFT, LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(false);
      fullRobotModel.getLegJoint(RobotSide.RIGHT, LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(false);
      fullRobotModel.getLegJoint(RobotSide.RIGHT, LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(false);
   }
   
   private void moveICPToInsideOfFootAtEndOfSwing(RobotSide supportSide, double swingTimeRemaining, FramePoint2d desiredICPLocal)
   {
      desiredICPLocal.changeFrame(referenceFrames.getAnkleZUpFrame(supportSide));
      
      double percent = (1.0 - swingTimeRemaining / singleSupportTimeLeftBeforeShift.getDoubleValue());
      percent = MathTools.clipToMinMax(percent,  0.0, 1.0);

      double minimumInside = percent * minimumICPFromCenterDuringSingleSupport.getDoubleValue() + 0.015;

      if (supportSide.negateIfLeftSide(desiredICPLocal.getY()) < minimumInside)
      {
         desiredICPLocal.setY(supportSide.negateIfLeftSide(minimumInside));
      }
      
      desiredICPLocal.changeFrame(desiredICP.getReferenceFrame());
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

