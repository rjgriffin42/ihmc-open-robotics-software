package us.ihmc.commonWalkingControlModules.capturePoint;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.dynamicPlanning.bipedPlanning.*;
import us.ihmc.commonWalkingControlModules.dynamicPlanning.comPlanning.ContactStateProvider;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.jumpingController.*;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.ContactPlaneProvider;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.ImplicitSE3ModelPredictiveController;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.SE3ModelPredictiveController;
import us.ihmc.commonWalkingControlModules.modelPredictiveController.visualization.MPCCornerPointViewer;
import us.ihmc.euclid.referenceFrame.*;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition.GraphicType;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyReadOnly;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.mecano.spatial.interfaces.WrenchBasics;
import us.ihmc.mecano.yoVariables.spatial.YoFixedFrameWrench;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.math.trajectories.generators.MultipleWaypointsPoseTrajectoryGenerator;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.time.ExecutionTimer;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFramePoint3D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameQuaternion;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameVector3D;
import us.ihmc.yoVariables.euclid.referenceFrame.YoFrameYawPitchRoll;
import us.ihmc.yoVariables.parameters.BooleanParameter;
import us.ihmc.yoVariables.providers.BooleanProvider;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.List;

import static us.ihmc.graphicsDescription.appearance.YoAppearance.*;

public class JumpingBalanceManager
{
   private static final double nominalHeight = 1.0;

   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoRegistry registry = new YoRegistry(getClass().getSimpleName());

   private final BipedSupportPolygons bipedSupportPolygons;

   private final JumpingMomentumRateControlModuleInput jumpingMomentumRateControlModuleInput = new JumpingMomentumRateControlModuleInput();

   private final JumpingControllerToolbox controllerToolbox;

   private final YoFramePoint3D yoDesiredDCM = new YoFramePoint3D("desiredDCM", worldFrame, registry);
   private final YoFrameVector3D yoDesiredDCMVelocity = new YoFrameVector3D("desiredDCMVelocity", worldFrame, registry);
   private final YoFramePoint3D yoDesiredCoMPosition = new YoFramePoint3D("desiredCoMPosition", worldFrame, registry);
   private final YoFrameVector3D yoDesiredCoMVelocity = new YoFrameVector3D("desiredCoMVelocity", worldFrame, registry);
   private final YoFrameVector3D yoDesiredCoMAcceleration = new YoFrameVector3D("desiredCoMAcceleration", worldFrame, registry);
   private final YoFrameQuaternion yoDesiredBodyOrientation = new YoFrameQuaternion("desiredBodyOrientation", worldFrame, registry);
   private final YoFrameYawPitchRoll yoDesiredBodyYawPitchRoll = new YoFrameYawPitchRoll("desiredBodyOrientation", worldFrame, registry);
   private final YoFrameVector3D yoDesiredBodyAngularVelocity = new YoFrameVector3D("desiredBodyAngularVelocity", worldFrame, registry);
   private final YoFrameVector3D yoDesiredBodyAngularAcceleration = new YoFrameVector3D("desiredBodyAngularAcceleration", worldFrame, registry);
   private final YoFramePoint3D touchdownCoMPosition = new YoFramePoint3D("touchdownCoMPosition", worldFrame, registry);
   private final YoFramePoint3D touchdownDCMPosition = new YoFramePoint3D("touchdownDCMPosition", worldFrame, registry);
   private final YoFramePoint3D yoPerfectVRP = new YoFramePoint3D("perfectVRP", worldFrame, registry);

   private final YoFixedFrameWrench desiredWrench;
   private final FrameVector3D desiredLinearMomentumRate = new FrameVector3D();
   private final FrameVector3D desiredAngularMomentumRate = new FrameVector3D();

   private final YoBoolean comPlannerDone = new YoBoolean("ICPPlannerDone", registry);
   private final ExecutionTimer plannerTimer = new ExecutionTimer("icpPlannerTimer", registry);

   private final SideDependentList<? extends ReferenceFrame> soleFrames;
   private final MovingReferenceFrame chestFrame;

   private final YoBoolean minimizeAngularMomentumRate = new YoBoolean("minimizeAngularMomentumRate", registry);
   private final YoDouble yoTime;
   private final YoDouble currentStateDuration = new YoDouble("CurrentStateDuration", registry);
   private final YoDouble totalStateDuration = new YoDouble("totalStateDuration", registry);
   private final YoDouble startTimeForSupportSequence = new YoDouble("startTimeForSupportSequence", registry);
   private final YoDouble timeInSupportSequence = new YoDouble("TimeInSupportSequence", registry);
   private final JumpingCoPTrajectoryGeneratorState copTrajectoryState;

   private final StandingCoPTrajectoryGenerator copTrajectoryForStanding;
   private final JumpingCoPTrajectoryGenerator copTrajectoryForJumping;

   private final BooleanProvider useAngularMomentumOffset = new BooleanParameter("useAngularMomentumOffset", registry, true);
   private final BooleanProvider useAngularMomentumOffsetInStanding = new BooleanParameter("useAngularMomentumOffsetInStanding", registry, true);
   private final YoBoolean computeAngularMomentumOffset = new YoBoolean("computeAngularMomentumOffset", registry);

   private final AngularMomentumHandler<ContactPlaneProvider> angularMomentumHandler;
   private final ImplicitSE3ModelPredictiveController comTrajectoryPlanner;

   public JumpingBalanceManager(JumpingControllerToolbox controllerToolbox,
                                CoPTrajectoryParameters copTrajectoryParameters,
                                JumpingCoPTrajectoryParameters jumpingCoPTrajectoryParameters,
                                JumpingParameters jumpingParameters,
                                YoRegistry parentRegistry)
   {
      YoGraphicsListRegistry yoGraphicsListRegistry = controllerToolbox.getYoGraphicsListRegistry();

      yoTime = controllerToolbox.getYoTime();
      this.controllerToolbox = controllerToolbox;

      bipedSupportPolygons = controllerToolbox.getBipedSupportPolygons();

      RigidBodyReadOnly chest = controllerToolbox.getFullRobotModel().getChest();
      chestFrame = chest.getBodyFixedFrame();
      soleFrames = controllerToolbox.getReferenceFrames().getSoleFrames();
      registry.addChild(copTrajectoryParameters.getRegistry());

      double totalMass = controllerToolbox.getFullRobotModel().getTotalMass();
      double gravityZ = controllerToolbox.getGravityZ();
      angularMomentumHandler = new AngularMomentumHandler<>(totalMass,
                                                            gravityZ,
                                                            controllerToolbox.getCenterOfMassJacobian(),
                                                            controllerToolbox.getReferenceFrames().getSoleFrames(),
                                                            ContactPlaneProvider::new,
                                                            registry,
                                                            yoGraphicsListRegistry);

      comTrajectoryPlanner = new ImplicitSE3ModelPredictiveController(chest.getInertia().getMomentOfInertia(),
                                                                      gravityZ,
                                                                      nominalHeight,
                                                                      totalMass,
                                                                      controllerToolbox.getControlDT(),
                                                                      registry);
//      comTrajectoryPlanner.addCostPolicy(new TouchDownHeightObjectivePolicy(controllerToolbox.getOmega0Provider(), OptimizedCoMTrajectoryPlanner.MEDIUM_WEIGHT));
//      comTrajectoryPlanner.addCostPolicy(new TakeOffHeightObjectivePolicy(controllerToolbox.getOmega0Provider(), OptimizedCoMTrajectoryPlanner.MEDIUM_WEIGHT));

      copTrajectoryState = new JumpingCoPTrajectoryGeneratorState(registry);
      copTrajectoryState.registerStateToSave(copTrajectoryParameters);
      copTrajectoryState.registerStateToSave(jumpingCoPTrajectoryParameters);

      copTrajectoryForStanding = new StandingCoPTrajectoryGenerator(copTrajectoryParameters, registry);
      copTrajectoryForStanding.registerState(copTrajectoryState);

      copTrajectoryForJumping = new JumpingCoPTrajectoryGenerator(copTrajectoryParameters, controllerToolbox.getDefaultFootPolygon(), jumpingCoPTrajectoryParameters, jumpingParameters, registry);
      copTrajectoryForJumping.registerState(copTrajectoryState);

      minimizeAngularMomentumRate.set(true);

      ReferenceFrame comFrame = controllerToolbox.getCenterOfMassFrame();
      desiredWrench = new YoFixedFrameWrench("DesiredCoMWrench", worldFrame, comFrame, registry);

      String graphicListName = getClass().getSimpleName();

      if (yoGraphicsListRegistry != null)
      {
         comTrajectoryPlanner.setCornerPointViewer(new MPCCornerPointViewer(false, true, registry, yoGraphicsListRegistry));
         comTrajectoryPlanner.setupCoMTrajectoryViewer(yoGraphicsListRegistry);

         YoGraphicPosition desiredDCMViz = new YoGraphicPosition("Desired DCM",
                                                                          yoDesiredDCM,
                                                                          0.01,
                                                                          Yellow(),
                                                                          GraphicType.BALL_WITH_ROTATED_CROSS);
         YoGraphicPosition desiredCoMViz = new YoGraphicPosition("Desired CoM", yoDesiredCoMPosition, 0.01, Red(), GraphicType.SOLID_BALL);
         YoGraphicPosition perfectVRPViz = new YoGraphicPosition("Perfect VRP", yoPerfectVRP, 0.0075, BlueViolet(), GraphicType.SOLID_BALL);
         YoGraphicPosition desiredTouchdownCoMViz = new YoGraphicPosition("Touchdown CoM", touchdownCoMPosition, 0.01, Black(), GraphicType.SOLID_BALL);
         YoGraphicPosition desiredTouchdownDCMViz = new YoGraphicPosition("Touchdown DCM", touchdownDCMPosition, 0.01, Yellow(), GraphicType.SOLID_BALL);

         yoGraphicsListRegistry.registerYoGraphic(graphicListName, desiredDCMViz);
         yoGraphicsListRegistry.registerYoGraphic(graphicListName, desiredCoMViz);
         yoGraphicsListRegistry.registerYoGraphic(graphicListName, perfectVRPViz);
         yoGraphicsListRegistry.registerYoGraphic(graphicListName, desiredTouchdownCoMViz);
         yoGraphicsListRegistry.registerYoGraphic(graphicListName, desiredTouchdownDCMViz);
         yoGraphicsListRegistry.registerArtifact(graphicListName, desiredDCMViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, desiredCoMViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, perfectVRPViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, desiredTouchdownCoMViz.createArtifact());
         yoGraphicsListRegistry.registerArtifact(graphicListName, desiredTouchdownDCMViz.createArtifact());
      }
      yoDesiredDCM.setToNaN();
      yoPerfectVRP.setToNaN();

      parentRegistry.addChild(registry);
   }

   public void clearICPPlan()
   {
      copTrajectoryState.clear();
   }

   public void setDesiredCoMHeight(double height)
   {
      comTrajectoryPlanner.setNominalCoMHeight(height);
   }



   public void compute()
   {
      yoDesiredDCM.set(comTrajectoryPlanner.getDesiredDCMPosition());
      yoDesiredDCMVelocity.set(comTrajectoryPlanner.getDesiredDCMVelocity());
      yoPerfectVRP.set(comTrajectoryPlanner.getDesiredVRPPosition());
      yoDesiredCoMPosition.set(comTrajectoryPlanner.getDesiredCoMPosition());
      yoDesiredCoMVelocity.set(comTrajectoryPlanner.getDesiredCoMVelocity());
      yoDesiredCoMAcceleration.set(comTrajectoryPlanner.getDesiredCoMAcceleration());
      yoDesiredBodyOrientation.set(comTrajectoryPlanner.getDesiredBodyOrientationSolution());
      yoDesiredBodyYawPitchRoll.set(comTrajectoryPlanner.getDesiredBodyOrientationSolution());
      yoDesiredBodyAngularVelocity.set(comTrajectoryPlanner.getDesiredBodyAngularVelocitySolution());
      yoDesiredBodyAngularAcceleration.set(comTrajectoryPlanner.getDesiredBodyAngularAccelerationSolution());

      double omega0 = controllerToolbox.getOmega0();
      if (Double.isNaN(omega0))
         throw new RuntimeException("omega0 is NaN");

      CapturePointTools.computeCentroidalMomentumPivot(yoDesiredDCM, yoDesiredDCMVelocity, omega0, yoPerfectVRP);

      jumpingMomentumRateControlModuleInput.setOmega0(omega0);
      jumpingMomentumRateControlModuleInput.setTimeInState(0.0);
      jumpingMomentumRateControlModuleInput.setMinimizeAngularMomentumRate(minimizeAngularMomentumRate.getBooleanValue());
   }

   private final FramePose3D chestPose = new FramePose3D();

   public void computeCoMPlanForStanding()
   {
      plannerTimer.startMeasurement();

      computeAngularMomentumOffset.set(useAngularMomentumOffsetInStanding.getValue() && useAngularMomentumOffset.getValue());

      touchdownCoMPosition.setToNaN();
      touchdownDCMPosition.setToNaN();

      // update online to account for foot slip
      for (RobotSide robotSide : RobotSide.values)
      {
         if (controllerToolbox.getFootContactState(robotSide).inContact())
            copTrajectoryState.initializeStance(robotSide, bipedSupportPolygons.getFootPolygonsInSoleZUpFrame().get(robotSide), soleFrames.get(robotSide));
      }
      copTrajectoryForStanding.compute(copTrajectoryState);

      chestPose.setToZero(chestFrame);
      chestPose.changeFrame(worldFrame);
      comTrajectoryPlanner.setCurrentState(controllerToolbox.getCenterOfMassJacobian().getCenterOfMass(),
                                                       controllerToolbox.getCenterOfMassJacobian().getCenterOfMassVelocity(),
                                                       chestPose.getOrientation(),
                                                       chestFrame.getTwistOfFrame().getAngularPart(),
                                                       timeInSupportSequence.getDoubleValue());
//      comTrajectoryPlanner.setCurrentState(chestPose.getOrientation(), chestFrame.getTwistOfFrame().getAngularPart());


      comTrajectoryPlanner.solveForTrajectory(copTrajectoryForStanding.getContactStateProviders());
      comTrajectoryPlanner.compute(timeInSupportSequence.getDoubleValue());

      // If this condition is false we are experiencing a late touchdown or a delayed liftoff. Do not advance the time in support sequence!
      timeInSupportSequence.set(yoTime.getValue() - startTimeForSupportSequence.getDoubleValue());

      comPlannerDone.set(timeInSupportSequence.getValue() >= currentStateDuration.getValue());

      if (comTrajectoryPlanner.getContactStateProviders().size() != comTrajectoryPlanner.getVRPTrajectories().size())
         throw new IllegalArgumentException("What?");

      jumpingMomentumRateControlModuleInput.setVrpTrajectories(comTrajectoryPlanner.getVRPTrajectories());
      jumpingMomentumRateControlModuleInput.setContactStateProviders(comTrajectoryPlanner.getContactStateProviders());

      desiredWrench.setMatchingFrame(comTrajectoryPlanner.getDesiredWrench());

      desiredLinearMomentumRate.setMatchingFrame(desiredWrench.getLinearPart());
      desiredLinearMomentumRate.addZ(controllerToolbox.getFullRobotModel().getTotalMass() * -Math.abs(controllerToolbox.getGravityZ()));
      desiredAngularMomentumRate.setMatchingFrame(desiredWrench.getAngularPart());

      jumpingMomentumRateControlModuleInput.setDesiredLinearMomentumRateOfChange(desiredLinearMomentumRate);
      jumpingMomentumRateControlModuleInput.setDesiredAngularMomentumRateOfChange(desiredAngularMomentumRate);

      plannerTimer.stopMeasurement();
   }

   public void computeCoMPlanForJumping(JumpingGoal jumpingGoal)
   {
      plannerTimer.startMeasurement();
      copTrajectoryState.setJumpingGoal(jumpingGoal);

      computeAngularMomentumOffset.set(useAngularMomentumOffset.getValue());

      // update online to account for foot slip
      for (RobotSide robotSide : RobotSide.values)
      {
         if (controllerToolbox.getFootContactState(robotSide).inContact())
            copTrajectoryState.initializeStance(robotSide, bipedSupportPolygons.getFootPolygonsInSoleZUpFrame().get(robotSide), soleFrames.get(robotSide));
      }
      copTrajectoryForJumping.compute(copTrajectoryState);

      MovingReferenceFrame chestFrame = controllerToolbox.getFullRobotModel().getChest().getBodyFixedFrame();
      chestPose.setToZero(chestFrame);
      chestPose.changeFrame(worldFrame);
      comTrajectoryPlanner.setCurrentState(controllerToolbox.getCenterOfMassJacobian().getCenterOfMass(),
                                           controllerToolbox.getCenterOfMassJacobian().getCenterOfMassVelocity(),
                                           chestPose.getOrientation(),
                                           chestFrame.getTwistOfFrame().getAngularPart(),
                                           timeInSupportSequence.getDoubleValue());
//      comTrajectoryPlanner.setCurrentBodyOrientationState(chestPose.getOrientation(), chestFrame.getTwistOfFrame().getAngularPart());

      List<ContactPlaneProvider> contactStateProviders = copTrajectoryForJumping.getContactStateProviders();
      if (computeAngularMomentumOffset.getValue())
      {
         if (comTrajectoryPlanner.hasTrajectories())
         {
            angularMomentumHandler.solveForAngularMomentumTrajectory(copTrajectoryState,
                                                                     contactStateProviders,
                                                                     comTrajectoryPlanner.getCoMTrajectory());
            contactStateProviders = angularMomentumHandler.computeECMPTrajectory(contactStateProviders);
         }
         else
         {
            angularMomentumHandler.resetAngularMomentum();
         }

         angularMomentumHandler.computeAngularMomentum(timeInSupportSequence.getDoubleValue());
         comTrajectoryPlanner.setInternalAngularMomentumTrajectory(angularMomentumHandler.getAngularMomentumTrajectories());
      }
      else
      {
         comTrajectoryPlanner.reset();
      }

      comTrajectoryPlanner.solveForTrajectory(contactStateProviders);

      comTrajectoryPlanner.compute(Math.max(totalStateDuration.getDoubleValue() - timeInSupportSequence.getDoubleValue(), 0.0));
      touchdownCoMPosition.set(comTrajectoryPlanner.getDesiredCoMPosition());
      touchdownDCMPosition.set(comTrajectoryPlanner.getDesiredDCMPosition());

      comTrajectoryPlanner.compute(timeInSupportSequence.getDoubleValue());

      // If this condition is false we are experiencing a late touchdown or a delayed liftoff. Do not advance the time in support sequence!
      timeInSupportSequence.set(yoTime.getValue() - startTimeForSupportSequence.getDoubleValue());

      comPlannerDone.set(timeInSupportSequence.getValue() >= currentStateDuration.getValue());

      if (comTrajectoryPlanner.getContactStateProviders().size() != comTrajectoryPlanner.getVRPTrajectories().size())
         throw new IllegalArgumentException("What?");
      jumpingMomentumRateControlModuleInput.setVrpTrajectories(comTrajectoryPlanner.getVRPTrajectories());
      jumpingMomentumRateControlModuleInput.setContactStateProviders(comTrajectoryPlanner.getContactStateProviders());

      desiredWrench.setMatchingFrame(comTrajectoryPlanner.getDesiredWrench());

      desiredLinearMomentumRate.setMatchingFrame(desiredWrench.getLinearPart());
      desiredLinearMomentumRate.addZ(controllerToolbox.getFullRobotModel().getTotalMass() * -Math.abs(controllerToolbox.getGravityZ()));
      desiredAngularMomentumRate.setMatchingFrame(desiredWrench.getAngularPart());

      jumpingMomentumRateControlModuleInput.setDesiredLinearMomentumRateOfChange(desiredLinearMomentumRate);
      jumpingMomentumRateControlModuleInput.setDesiredAngularMomentumRateOfChange(desiredAngularMomentumRate);

      plannerTimer.stopMeasurement();
   }

   public FramePoint3DReadOnly getDesiredDCM()
   {
      return yoDesiredDCM;
   }

   public FrameVector3DReadOnly getDesiredDCMVelocity()
   {
      return yoDesiredDCMVelocity;
   }

   public FrameVector3DReadOnly getDesiredCoMVelocity()
   {
      return yoDesiredCoMVelocity;
   }

   public FramePoint3DReadOnly getTouchdownCoMPosition()
   {
      return touchdownCoMPosition;
   }

   public void initialize()
   {
      yoDesiredDCM.set(controllerToolbox.getCapturePoint());
      yoDesiredCoMPosition.setFromReferenceFrame(controllerToolbox.getCenterOfMassFrame());
      yoDesiredCoMPosition.setZ(comTrajectoryPlanner.getNominalCoMHeight());
      yoDesiredCoMVelocity.setToZero();

      MovingReferenceFrame chestFrame = controllerToolbox.getFullRobotModel().getChest().getBodyFixedFrame();
      yoDesiredBodyOrientation.setFromReferenceFrame(chestFrame);
      yoDesiredBodyAngularVelocity.setToZero();

      yoPerfectVRP.set(bipedSupportPolygons.getSupportPolygonInWorld().getCentroid());
      yoPerfectVRP.setZ(yoDesiredDCM.getZ());
      copTrajectoryState.setInitialCoP(yoPerfectVRP);
      copTrajectoryState.initializeStance(bipedSupportPolygons.getFootPolygonsInSoleZUpFrame(), soleFrames);
      comTrajectoryPlanner.setInitialCenterOfMassState(yoDesiredCoMPosition, yoDesiredCoMVelocity);
//      comTrajectoryPlanner.setInitialBodyOrientationState(yoDesiredBodyOrientation, yoDesiredBodyAngularVelocity);
      startTimeForSupportSequence.set(yoTime.getValue());
      timeInSupportSequence.set(0.0);
      currentStateDuration.set(Double.NaN);
      totalStateDuration.set(Double.NaN);
   }

   public void setSwingFootTrajectory(RobotSide swingSide, MultipleWaypointsPoseTrajectoryGenerator swingTrajectory)
   {
      angularMomentumHandler.setSwingFootTrajectory(swingSide, swingTrajectory);
   }

   public void clearSwingFootTrajectory()
   {
      angularMomentumHandler.clearSwingFootTrajectory();
   }

   public void initializeCoMPlanForStanding()
   {
      copTrajectoryState.setInitialCoP(yoPerfectVRP);
      copTrajectoryState.initializeStance(bipedSupportPolygons.getFootPolygonsInSoleZUpFrame(), soleFrames);
      comTrajectoryPlanner.setInitialCenterOfMassState(yoDesiredCoMPosition, yoDesiredCoMVelocity);
//      comTrajectoryPlanner.setInitialBodyOrientationState(yoDesiredBodyOrientation, yoDesiredBodyAngularVelocity);

      startTimeForSupportSequence.set(yoTime.getValue());
      timeInSupportSequence.set(0.0);
      currentStateDuration.set(Double.POSITIVE_INFINITY);
      totalStateDuration.set(Double.POSITIVE_INFINITY);

      jumpingMomentumRateControlModuleInput.setInFlight(false);

      comPlannerDone.set(false);
   }

   public void initializeCoMPlanForTransferToStanding()
   {
      copTrajectoryState.setInitialCoP(yoPerfectVRP);
      copTrajectoryState.initializeStance(bipedSupportPolygons.getFootPolygonsInSoleZUpFrame(), soleFrames);
      comTrajectoryPlanner.setInitialCenterOfMassState(yoDesiredCoMPosition, yoDesiredCoMVelocity);
//      comTrajectoryPlanner.setInitialBodyOrientationState(yoDesiredBodyOrientation, yoDesiredBodyAngularVelocity);

      startTimeForSupportSequence.set(yoTime.getValue());
      timeInSupportSequence.set(0.0);
      currentStateDuration.set(copTrajectoryState.getFinalTransferDuration());
      totalStateDuration.set(copTrajectoryState.getFinalTransferDuration());

      jumpingMomentumRateControlModuleInput.setInFlight(false);

      comPlannerDone.set(false);
   }

   public void initializeCoMPlanForSupport(JumpingGoal jumpingGoal)
   {
      copTrajectoryState.setInitialCoP(yoPerfectVRP);
      copTrajectoryState.initializeStance(bipedSupportPolygons.getFootPolygonsInSoleZUpFrame(), soleFrames);
      comTrajectoryPlanner.setInitialCenterOfMassState(yoDesiredCoMPosition, yoDesiredCoMVelocity);
//      comTrajectoryPlanner.setInitialBodyOrientationState(yoDesiredBodyOrientation, yoDesiredBodyAngularVelocity);

      startTimeForSupportSequence.set(yoTime.getValue());
      timeInSupportSequence.set(0.0);
      currentStateDuration.set(jumpingGoal.getSupportDuration() + jumpingGoal.getFlightDuration());
      totalStateDuration.set(jumpingGoal.getSupportDuration() + jumpingGoal.getFlightDuration());

      jumpingMomentumRateControlModuleInput.setInFlight(false);

      comPlannerDone.set(false);
   }

   public void initializeCoMPlanForFlight(JumpingGoal jumpingGoal)
   {
      currentStateDuration.set(jumpingGoal.getSupportDuration() + jumpingGoal.getFlightDuration());
      totalStateDuration.set(jumpingGoal.getSupportDuration() + jumpingGoal.getFlightDuration());

      jumpingMomentumRateControlModuleInput.setInFlight(true);

      comPlannerDone.set(false);
   }

   public void setMinimizeAngularMomentumRate(boolean minimizeAngularMomentumRate)
   {
      this.minimizeAngularMomentumRate.set(minimizeAngularMomentumRate);
   }

   public boolean isCoMPlanDone()
   {
      return comPlannerDone.getValue();
   }

   public void setFinalTransferTime(double finalTransferDuration)
   {
      copTrajectoryState.setFinalTransferDuration(finalTransferDuration);
   }

   public FramePoint3DReadOnly getCapturePoint()
   {
      return controllerToolbox.getCapturePoint();
   }

   public JumpingMomentumRateControlModuleInput getJumpingMomentumRateControlModuleInput()
   {
      return jumpingMomentumRateControlModuleInput;
   }
}
