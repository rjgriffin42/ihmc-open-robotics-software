package us.ihmc.quadrupedRobotics.controller.force.states;

import us.ihmc.graphics3DAdapter.graphics.appearances.AppearanceDefinition;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.quadrupedRobotics.controller.ControllerEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerToolbox;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.*;
import us.ihmc.quadrupedRobotics.estimator.GroundPlaneEstimator;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.params.DoubleArrayParameter;
import us.ihmc.quadrupedRobotics.params.DoubleParameter;
import us.ihmc.quadrupedRobotics.params.ParameterFactory;
import us.ihmc.quadrupedRobotics.planning.*;
import us.ihmc.quadrupedRobotics.planning.trajectory.PiecewiseForwardDcmTrajectory;
import us.ihmc.quadrupedRobotics.planning.trajectory.PiecewiseReverseDcmTrajectory;
import us.ihmc.quadrupedRobotics.planning.trajectory.ThreeDoFMinimumJerkTrajectory;
import us.ihmc.quadrupedRobotics.providers.QuadrupedControllerInputProviderInterface;
import us.ihmc.quadrupedRobotics.providers.QuadrupedXGaitSettingsProvider;
import us.ihmc.quadrupedRobotics.state.FiniteStateMachine;
import us.ihmc.quadrupedRobotics.state.FiniteStateMachineBuilder;
import us.ihmc.quadrupedRobotics.state.FiniteStateMachineState;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.*;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.BagOfBalls;

import javax.vecmath.Point3d;
import java.util.ArrayList;

public class QuadrupedDcmBasedXGaitController implements QuadrupedController
{
   private final QuadrupedControllerInputProviderInterface inputProvider;
   private final QuadrupedXGaitSettingsProvider settingsProvider;
   private final DoubleYoVariable robotTimestamp;
   private final double controlDT;
   private final double gravity;
   private final double mass;
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   // parameters
   private final ParameterFactory parameterFactory = new ParameterFactory(getClass());
   private final DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 2);
   private final DoubleArrayParameter bodyOrientationProportionalGainsParameter = parameterFactory.createDoubleArray("bodyOrientationProportionalGains", 5000, 5000, 5000);
   private final DoubleArrayParameter bodyOrientationDerivativeGainsParameter = parameterFactory.createDoubleArray("bodyOrientationDerivativeGains", 750, 750, 750);
   private final DoubleArrayParameter bodyOrientationIntegralGainsParameter = parameterFactory.createDoubleArray("bodyOrientationIntegralGains", 0, 0, 0);
   private final DoubleParameter bodyOrientationMaxIntegralErrorParameter = parameterFactory.createDouble("bodyOrientationMaxIntegralError", 0);
   private final DoubleArrayParameter comPositionProportionalGainsParameter = parameterFactory.createDoubleArray("comPositionProportionalGains", 0, 0, 5000);
   private final DoubleArrayParameter comPositionDerivativeGainsParameter = parameterFactory.createDoubleArray("comPositionDerivativeGains", 0, 0, 750);
   private final DoubleArrayParameter comPositionIntegralGainsParameter = parameterFactory.createDoubleArray("comPositionIntegralGains", 0, 0, 0);
   private final DoubleParameter comPositionMaxIntegralErrorParameter = parameterFactory.createDouble("comPositionMaxIntegralError", 0);
   private final DoubleArrayParameter dcmPositionProportionalGainsParameter = parameterFactory.createDoubleArray("dcmPositionProportionalGains", 1, 1, 0);
   private final DoubleArrayParameter dcmPositionDerivativeGainsParameter = parameterFactory.createDoubleArray("dcmPositionDerivativeGains", 0, 0, 0);
   private final DoubleArrayParameter dcmPositionIntegralGainsParameter = parameterFactory.createDoubleArray("dcmPositionIntegralGains", 0, 0, 0);
   private final DoubleParameter dcmPositionMaxIntegralErrorParameter = parameterFactory.createDouble("dcmPositionMaxIntegralError", 0);
   private final DoubleParameter initialTransitionDurationParameter = parameterFactory.createDouble("initialTransitionDuration", 1.00);
   private final DoubleParameter footholdDistanceLowerLimitParameter = parameterFactory.createDouble("footholdDistanceLowerLimit", 0.15);

   // frames
   private final ReferenceFrame supportFrame;
   private final ReferenceFrame worldFrame;

   // feedback controllers
   private final FramePoint dcmPositionEstimate;
   private final DivergentComponentOfMotionController.Setpoints dcmPositionControllerSetpoints;
   private final DivergentComponentOfMotionController dcmPositionController;
   private final QuadrupedComPositionController.Setpoints comPositionControllerSetpoints;
   private final QuadrupedComPositionController comPositionController;
   private final QuadrupedBodyOrientationController.Setpoints bodyOrientationControllerSetpoints;
   private final QuadrupedBodyOrientationController bodyOrientationController;
   private final QuadrupedTimedStepController.Setpoints timedStepControllerSetpoints;
   private final QuadrupedTimedStepController timedStepController;

   // task space controller
   private final QuadrupedTaskSpaceEstimator.Estimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;

   // planning
   private static int NUMBER_OF_PREVIEW_STEPS = 32;
   private double bodyYawSetpoint;
   private final GroundPlaneEstimator groundPlaneEstimator;
   private final QuadrantDependentList<FramePoint> groundPlanePositions;
   private final QuadrupedXGaitSettings xGaitSettings;
   private final QuadrupedXGaitPlanner xGaitStepPlanner;
   private final ArrayList<QuadrupedTimedStep> xGaitPreviewSteps;
   private final QuadrupedTimedStepCopPlanner timedStepCopPlanner;
   private final QuadrantDependentList<FrameVector> timedStepAdjustmentAtContactSwitch;

   // graphics
   private final BagOfBalls xGaitPreviewStepVisualization;
   private final FramePoint xGaitPreviewStepVisualizationPosition;
   private static final QuadrantDependentList<AppearanceDefinition> xGaitPreviewStepAppearance = new QuadrantDependentList<>(
         YoAppearance.Red(), YoAppearance.Blue(), YoAppearance.RGBColor(1, 0.5, 0.0), YoAppearance.RGBColor(0.0, 0.5, 1.0));

   // state machine
   public enum XGaitState
   {
      INITIAL_TRANSITION, FORWARD_XGAIT
   }
   public enum XGaitEvent
   {
      TIMEOUT, FORWARD
   }
   private final FiniteStateMachine<XGaitState, XGaitEvent> xGaitStateMachine;

   public QuadrupedDcmBasedXGaitController(QuadrupedRuntimeEnvironment runtimeEnvironment, QuadrupedForceControllerToolbox controllerToolbox,
         QuadrupedControllerInputProviderInterface inputProvider, QuadrupedXGaitSettingsProvider settingsProvider)
   {
      this.inputProvider = inputProvider;
      this.settingsProvider = settingsProvider;
      this.robotTimestamp = runtimeEnvironment.getRobotTimestamp();
      this.controlDT = runtimeEnvironment.getControlDT();
      this.gravity = 9.81;
      this.mass = runtimeEnvironment.getFullRobotModel().getTotalMass();

      // frames
      QuadrupedReferenceFrames referenceFrames = controllerToolbox.getReferenceFrames();
      supportFrame = referenceFrames.getCenterOfFeetZUpFrameAveragingLowestZHeightsAcrossEnds();
      worldFrame = ReferenceFrame.getWorldFrame();

      // feedback controllers
      dcmPositionEstimate = new FramePoint();
      dcmPositionControllerSetpoints = new DivergentComponentOfMotionController.Setpoints();
      dcmPositionController = controllerToolbox.getDcmPositionController();
      comPositionControllerSetpoints = new QuadrupedComPositionController.Setpoints();
      comPositionController = controllerToolbox.getComPositionController();
      bodyOrientationControllerSetpoints = new QuadrupedBodyOrientationController.Setpoints();
      bodyOrientationController = controllerToolbox.getBodyOrientationController();
      timedStepControllerSetpoints = new QuadrupedTimedStepController.Setpoints();
      timedStepController = controllerToolbox.getTimedStepController();

      // task space controllers
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimator.Estimates();
      taskSpaceEstimator = controllerToolbox.getTaskSpaceEstimator();
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      taskSpaceController = controllerToolbox.getTaskSpaceController();

      // planning
      groundPlaneEstimator = new GroundPlaneEstimator();
      groundPlanePositions = new QuadrantDependentList<>();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.set(robotQuadrant, new FramePoint());
      }
      xGaitStepPlanner = new QuadrupedXGaitPlanner();
      xGaitPreviewSteps = new ArrayList<>(NUMBER_OF_PREVIEW_STEPS);
      for (int i = 0; i < NUMBER_OF_PREVIEW_STEPS; i++)
      {
         xGaitPreviewSteps.add(new QuadrupedTimedStep());
      }
      xGaitSettings = new QuadrupedXGaitSettings();
      timedStepCopPlanner = new QuadrupedTimedStepCopPlanner(NUMBER_OF_PREVIEW_STEPS + 4);
      timedStepAdjustmentAtContactSwitch = new QuadrantDependentList<>();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         timedStepAdjustmentAtContactSwitch.set(robotQuadrant, new FrameVector());
      }

      // state machine
      FiniteStateMachineBuilder<XGaitState, XGaitEvent> ambleStateMachineBuilder = new FiniteStateMachineBuilder<>(XGaitState.class, XGaitEvent.class, "XGaitState", registry);
      ambleStateMachineBuilder.addState(XGaitState.INITIAL_TRANSITION, new InitialTransitionState());
      ambleStateMachineBuilder.addState(XGaitState.FORWARD_XGAIT, new ForwardXGaitState());
      ambleStateMachineBuilder.addTransition(XGaitEvent.TIMEOUT, XGaitState.INITIAL_TRANSITION, XGaitState.FORWARD_XGAIT);
      xGaitStateMachine = ambleStateMachineBuilder.build(XGaitState.INITIAL_TRANSITION);
      runtimeEnvironment.getParentRegistry().addChild(registry);

      // graphics
      xGaitPreviewStepVisualization = BagOfBalls.createRainbowBag(xGaitPreviewSteps.size(), 0.015, "xGaitPreviewSteps", registry, runtimeEnvironment.getGraphicsListRegistry());
      xGaitPreviewStepVisualizationPosition = new FramePoint();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   private void updateEstimates()
   {
      // update task space estimates
      taskSpaceEstimator.compute(taskSpaceEstimates);

      // update dcm estimate
      taskSpaceEstimates.getComPosition().changeFrame(worldFrame);
      taskSpaceEstimates.getComVelocity().changeFrame(worldFrame);
      dcmPositionEstimate.changeFrame(worldFrame);
      dcmPositionEstimate.set(taskSpaceEstimates.getComVelocity());
      dcmPositionEstimate.scale(1.0 / dcmPositionController.getNaturalFrequency());
      dcmPositionEstimate.add(taskSpaceEstimates.getComPosition());
   }

   private void updateSetpoints()
   {
      // update state machines
      xGaitStateMachine.process();

      // update desired horizontal com forces
      dcmPositionController.compute(taskSpaceControllerCommands.getComForce(), dcmPositionControllerSetpoints, dcmPositionEstimate);
      taskSpaceControllerCommands.getComForce().changeFrame(supportFrame);

      // update desired com position, velocity, and vertical force
      comPositionControllerSetpoints.getComPosition().changeFrame(supportFrame);
      comPositionControllerSetpoints.getComPosition().set(inputProvider.getComPositionInput());
      comPositionControllerSetpoints.getComVelocity().changeFrame(supportFrame);
      comPositionControllerSetpoints.getComVelocity().set(inputProvider.getComVelocityInput());
      comPositionControllerSetpoints.getComForceFeedforward().changeFrame(supportFrame);
      comPositionControllerSetpoints.getComForceFeedforward().set(taskSpaceControllerCommands.getComForce());
      comPositionControllerSetpoints.getComForceFeedforward().setZ(mass * gravity);
      comPositionController.compute(taskSpaceControllerCommands.getComForce(), comPositionControllerSetpoints, taskSpaceEstimates);

      // update desired body orientation, angular velocity, and torque
      if (xGaitStateMachine.getState() != XGaitState.INITIAL_TRANSITION)
      {
         bodyYawSetpoint += inputProvider.getPlanarVelocityInput().getZ() * controlDT;
      }
      bodyOrientationControllerSetpoints.getBodyOrientation().changeFrame(worldFrame);
      bodyOrientationControllerSetpoints.getBodyOrientation().setYawPitchRoll(bodyYawSetpoint,
            RotationTools.computePitch(inputProvider.getBodyOrientationInput()) + groundPlaneEstimator.getPitch(bodyYawSetpoint),
            RotationTools.computeRoll(inputProvider.getBodyOrientationInput()));
      bodyOrientationControllerSetpoints.getBodyAngularVelocity().setToZero();
      bodyOrientationControllerSetpoints.getComTorqueFeedforward().setToZero();
      bodyOrientationController.compute(taskSpaceControllerCommands.getComTorque(), bodyOrientationControllerSetpoints, taskSpaceEstimates);

      // update desired contact state and sole forces
      FramePoint dcmPositionSetpoint = dcmPositionControllerSetpoints.getDcmPosition();
      dcmPositionSetpoint.changeFrame(worldFrame);
      dcmPositionEstimate.changeFrame(worldFrame);
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         timedStepControllerSetpoints.getStepAdjustment(robotQuadrant).changeFrame(worldFrame);
         timedStepControllerSetpoints.getStepAdjustment(robotQuadrant).set(timedStepAdjustmentAtContactSwitch.get(robotQuadrant));
         timedStepControllerSetpoints.getStepAdjustment(robotQuadrant).add(dcmPositionEstimate.getX(), dcmPositionEstimate.getY(), 0.0);
         timedStepControllerSetpoints.getStepAdjustment(robotQuadrant).sub(dcmPositionSetpoint.getX(), dcmPositionSetpoint.getY(), 0.0);
      }
      timedStepController.compute(taskSpaceControllerSettings.getContactState(), taskSpaceControllerCommands.getSoleForce(), timedStepControllerSetpoints, taskSpaceEstimates);

      // update joint setpoints
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);
   }

   private void updateGraphics()
   {
      for (int i = 0; i < xGaitPreviewSteps.size(); i++)
      {
         xGaitPreviewSteps.get(i).getGoalPosition(xGaitPreviewStepVisualizationPosition);
         xGaitPreviewStepVisualization.setBallLoop(xGaitPreviewStepVisualizationPosition, xGaitPreviewStepAppearance.get(xGaitPreviewSteps.get(i).getRobotQuadrant()));
      }
   }

   private void updateSettings()
   {
      settingsProvider.getXGaitSettings(xGaitSettings);

      // increase stance dimensions to prevent self collisions
      double strideLength = Math.abs(2 * inputProvider.getPlanarVelocityInput().getX() * xGaitSettings.getStepDuration());
      double strideWidth = Math.abs(2 * inputProvider.getPlanarVelocityInput().getY() * xGaitSettings.getStepDuration());
      strideLength += Math.abs(xGaitSettings.getStanceWidth() / 2 * Math.sin(2 * inputProvider.getPlanarVelocityInput().getZ() * xGaitSettings.getStepDuration()));
      strideWidth += Math.abs(xGaitSettings.getStanceLength() / 2 * Math.sin(2 * inputProvider.getPlanarVelocityInput().getZ() * xGaitSettings.getStepDuration()));
      xGaitSettings.setStanceLength(Math.max(xGaitSettings.getStanceLength(), strideLength / 2 + footholdDistanceLowerLimitParameter.get()));
      xGaitSettings.setStanceWidth(Math.max(xGaitSettings.getStanceWidth(), strideWidth / 2 + footholdDistanceLowerLimitParameter.get()));
   }

   @Override public ControllerEvent process()
   {
      updateSettings();
      updateEstimates();
      updateSetpoints();
      updateGraphics();
      return null;
   }

   @Override public void onEntry()
   {
      // initialize estimates
      dcmPositionController.setComHeight(inputProvider.getComPositionInput().getZ());
      updateEstimates();

      // initialize feedback controllers
      dcmPositionControllerSetpoints.initialize(dcmPositionEstimate);
      dcmPositionController.reset();
      dcmPositionController.getGains().setProportionalGains(dcmPositionProportionalGainsParameter.get());
      dcmPositionController.getGains().setIntegralGains(dcmPositionIntegralGainsParameter.get(), dcmPositionMaxIntegralErrorParameter.get());
      dcmPositionController.getGains().setDerivativeGains(dcmPositionDerivativeGainsParameter.get());
      comPositionControllerSetpoints.initialize(taskSpaceEstimates);
      comPositionController.reset();
      comPositionController.getGains().setProportionalGains(comPositionProportionalGainsParameter.get());
      comPositionController.getGains().setIntegralGains(comPositionIntegralGainsParameter.get(), comPositionMaxIntegralErrorParameter.get());
      comPositionController.getGains().setDerivativeGains(comPositionDerivativeGainsParameter.get());
      bodyOrientationControllerSetpoints.initialize(taskSpaceEstimates);
      bodyOrientationController.reset();
      bodyOrientationController.getGains().setProportionalGains(bodyOrientationProportionalGainsParameter.get());
      bodyOrientationController.getGains().setIntegralGains(bodyOrientationIntegralGainsParameter.get(), bodyOrientationMaxIntegralErrorParameter.get());
      bodyOrientationController.getGains().setDerivativeGains(bodyOrientationDerivativeGainsParameter.get());
      timedStepControllerSetpoints.initialize(taskSpaceEstimates);
      timedStepController.reset();

      // initialize task space controller
      taskSpaceControllerSettings.initialize();
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      taskSpaceControllerSettings.getContactForceOptimizationSettings().setComForceCommandWeights(1.0, 1.0, 1.0);
      taskSpaceControllerSettings.getContactForceOptimizationSettings().setComTorqueCommandWeights(1.0, 1.0, 1.0);
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.getContactForceOptimizationSettings().setContactForceCommandWeights(robotQuadrant, 0.0, 0.0, 0.0);
         taskSpaceControllerSettings.setContactState(robotQuadrant, ContactState.IN_CONTACT);
      }
      taskSpaceController.reset();

      // initialize body yaw trajectory
      taskSpaceEstimates.getBodyOrientation().changeFrame(worldFrame);
      bodyYawSetpoint = taskSpaceEstimates.getBodyOrientation().getYaw();

      // initialize state machine
      updateSettings();
      xGaitStateMachine.reset();

      // initialize graphics
      xGaitPreviewStepVisualization.setVisible(true);
   }

   @Override public void onExit()
   {
      xGaitStateMachine.reset();
      timedStepController.removeSteps();
      xGaitPreviewStepVisualization.setVisible(false);
   }

   private class InitialTransitionState implements FiniteStateMachineState<XGaitEvent>
   {
      double initialTransitionTime;
      private RobotQuadrant initialQuadrant;
      private final FramePoint initialSupportCentroid;
      private final ThreeDoFMinimumJerkTrajectory transitionDcmTrajectory;
      private final PiecewiseReverseDcmTrajectory reverseDcmTrajectory;
      private final FramePoint reverseDcmPositionAtSoS;

      public InitialTransitionState()
      {
         initialSupportCentroid = new FramePoint();
         transitionDcmTrajectory = new ThreeDoFMinimumJerkTrajectory();
         reverseDcmTrajectory = new PiecewiseReverseDcmTrajectory(2 * xGaitPreviewSteps.size() + 1, gravity, dcmPositionController.getComHeight());
         reverseDcmPositionAtSoS = new FramePoint();
      }

      @Override public void onEntry()
      {
         double currentTime = robotTimestamp.getDoubleValue();

         initialSupportCentroid.setToZero(supportFrame);
         initialQuadrant = (xGaitSettings.getEndPhaseShift() < 90) ? RobotQuadrant.HIND_LEFT : RobotQuadrant.FRONT_LEFT;
         initialTransitionTime = currentTime + initialTransitionDurationParameter.get();

         // initialize dcm height
         dcmPositionController.setComHeight(inputProvider.getComPositionInput().getZ());

         // initialize ground plane
         for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
         {
            groundPlanePositions.get(robotQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(robotQuadrant));
            groundPlanePositions.get(robotQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
         }
         groundPlaneEstimator.compute(groundPlanePositions);

         // initialize xGait step plan
         xGaitStepPlanner.computeInitialPlan(xGaitPreviewSteps, inputProvider.getPlanarVelocityInput(),
               initialQuadrant, initialSupportCentroid, initialTransitionTime, bodyYawSetpoint, xGaitSettings);
         for (int i = 0; i < xGaitPreviewSteps.size(); i++)
         {
            timedStepController.addStep(xGaitPreviewSteps.get(i));
         }

         // compute reverse dcm trajectory
         int nIntervals = timedStepCopPlanner.compute(xGaitPreviewSteps, taskSpaceEstimates.getSolePosition(), taskSpaceControllerSettings.getContactState(), currentTime);
         reverseDcmTrajectory.setComHeight(dcmPositionController.getComHeight());
         reverseDcmTrajectory.initializeTrajectory(nIntervals, timedStepCopPlanner.getTimeAtStartOfInterval(), timedStepCopPlanner.getCopAtStartOfInterval(), timedStepCopPlanner.getTimeAtStartOfInterval(nIntervals - 1),
               timedStepCopPlanner.getCopAtStartOfInterval(nIntervals - 1));
         reverseDcmTrajectory.computeTrajectory(initialTransitionTime);
         reverseDcmTrajectory.getPosition(reverseDcmPositionAtSoS);
         reverseDcmPositionAtSoS.changeFrame(worldFrame);

         // compute transition dcm trajectory
         transitionDcmTrajectory.initializeTrajectory(dcmPositionEstimate, reverseDcmPositionAtSoS, currentTime, initialTransitionTime);

         initialTransitionTime = timedStepController.getQueue().get(1).getTimeInterval().getStartTime();
      }

      @Override public XGaitEvent process()
      {
         // compute nominal dcm trajectory
         transitionDcmTrajectory.computeTrajectory(robotTimestamp.getDoubleValue());
         transitionDcmTrajectory.getPosition(dcmPositionControllerSetpoints.getDcmPosition());
         transitionDcmTrajectory.getVelocity(dcmPositionControllerSetpoints.getDcmVelocity());

         if (robotTimestamp.getDoubleValue() > initialTransitionTime + controlDT)
            return XGaitEvent.TIMEOUT;
         else
            return null;
      }

      @Override public void onExit()
      {
      }
   }

   private class ForwardXGaitState implements FiniteStateMachineState<XGaitEvent>, QuadrupedTimedStepTransitionCallback
   {
      private final PiecewiseForwardDcmTrajectory forwardDcmTrajectory;
      private final PiecewiseReverseDcmTrajectory reverseDcmTrajectory;
      private final FramePoint forwardDcmPositionAtEoTS;
      private final FramePoint reverseDcmPositionAtEoNS;
      private final FramePoint nominalDcmOffsetAtEoNS;
      private final FrameVector goalPositionAdjustment;
      private EndDependentList<QuadrupedTimedStep> latestSteps;

      public ForwardXGaitState()
      {
         forwardDcmTrajectory = new PiecewiseForwardDcmTrajectory(2 * (xGaitPreviewSteps.size() + 4) + 1, gravity, dcmPositionController.getComHeight());
         reverseDcmTrajectory = new PiecewiseReverseDcmTrajectory(2 * (xGaitPreviewSteps.size() + 4) + 1, gravity, dcmPositionController.getComHeight());
         forwardDcmPositionAtEoTS = new FramePoint();
         reverseDcmPositionAtEoNS = new FramePoint();
         nominalDcmOffsetAtEoNS = new FramePoint();
         goalPositionAdjustment = new FrameVector();
         latestSteps = new EndDependentList<>();
         for (RobotEnd robotEnd : RobotEnd.values)
         {
            latestSteps.set(robotEnd, new QuadrupedTimedStep());
         }
      }

      private void computeStepGoalPosition(Point3d thisGoalPosition, Point3d lastGoalPosition, FramePoint dcmPositionAtEoTS, FramePoint dcmOffsetAtEoNS, double supportTime, double naturalFrequency)
      {
         double exp = Math.exp(naturalFrequency * supportTime);
         dcmPositionAtEoTS.scale(exp);
         dcmPositionAtEoTS.sub(dcmOffsetAtEoNS);
         dcmPositionAtEoTS.scale(2 / (1 + exp));
         thisGoalPosition.set(lastGoalPosition);
         thisGoalPosition.scale((1 - exp) / (1 + exp));
         thisGoalPosition.add(dcmPositionAtEoTS.getPoint());
      }

      @Override public void onEntry()
      {
         double currentTime = robotTimestamp.getDoubleValue();
         timedStepController.registerStepTransitionCallback(this);

         // initialize dcm height
         dcmPositionController.setComHeight(inputProvider.getComPositionInput().getZ());

         // initialize forward dcm trajectory
         int nIntervals = timedStepCopPlanner.compute(timedStepController.getQueue(), taskSpaceEstimates.getSolePosition(), taskSpaceControllerSettings.getContactState(), currentTime);
         forwardDcmTrajectory.setComHeight(dcmPositionController.getComHeight());
         forwardDcmTrajectory.initializeTrajectory(nIntervals, timedStepCopPlanner.getTimeAtStartOfInterval(), timedStepCopPlanner.getCopAtStartOfInterval(), dcmPositionEstimate);

         // initialize latest steps
         for (int i = 0; i < 2; i++)
         {
            QuadrupedTimedStep step = timedStepController.getQueue().get(i);
            latestSteps.get(step.getRobotQuadrant().getEnd()).set(step);
         }
      }

      @Override public void onLiftOff(RobotQuadrant thisStepQuadrant, QuadrantDependentList<ContactState> contactState)
      {
         double currentTime = robotTimestamp.getDoubleValue();
         RobotEnd thisStepEnd = thisStepQuadrant.getEnd();
         RobotQuadrant pastStepQuadrant = thisStepQuadrant.getNextReversedRegularGaitSwingQuadrant();
         RobotQuadrant nextStepQuadrant = thisStepQuadrant.getNextRegularGaitSwingQuadrant();
         QuadrupedTimedStep thisStep = timedStepController.getEarliestStep(thisStepQuadrant);
         QuadrupedTimedStep nextStep = timedStepController.getEarliestStep(nextStepQuadrant);
         int nIntervals;

         if (latestSteps.get(thisStepEnd.getOppositeEnd()).getRobotQuadrant() != pastStepQuadrant)
         {
            throw new RuntimeException("Timing error: xgait steps triggered in incorrect order.");
         }

         // initialize dcm height
         dcmPositionController.setComHeight(inputProvider.getComPositionInput().getZ());

         // initialize step adjustments
         timedStepAdjustmentAtContactSwitch.get(pastStepQuadrant).setIncludingFrame(timedStepControllerSetpoints.getStepAdjustment(pastStepQuadrant));
         timedStepAdjustmentAtContactSwitch.get(thisStepQuadrant).setToZero();

         // compute ground plane
         groundPlanePositions.get(thisStepQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(thisStepQuadrant));
         groundPlanePositions.get(thisStepQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
         groundPlaneEstimator.compute(groundPlanePositions);
         groundPlaneEstimator.projectZ(thisStep.getGoalPosition());

         // compute forward dcm trajectory
         nIntervals = timedStepCopPlanner.compute(timedStepController.getQueue(), taskSpaceEstimates.getSolePosition(), contactState, currentTime - controlDT);
         forwardDcmTrajectory.setComHeight(dcmPositionController.getComHeight());
         forwardDcmTrajectory.initializeTrajectory(nIntervals, timedStepCopPlanner.getTimeAtStartOfInterval(), timedStepCopPlanner.getCopAtStartOfInterval(), dcmPositionEstimate);
         forwardDcmTrajectory.computeTrajectory(thisStep.getTimeInterval().getEndTime());
         forwardDcmTrajectory.getPosition(forwardDcmPositionAtEoTS);
         forwardDcmPositionAtEoTS.changeFrame(worldFrame);

         // compute reverse dcm trajectory
         nIntervals = timedStepCopPlanner.compute(xGaitPreviewSteps, taskSpaceEstimates.getSolePosition(), contactState, currentTime - controlDT);
         reverseDcmTrajectory.setComHeight(dcmPositionController.getComHeight());
         reverseDcmTrajectory.initializeTrajectory(nIntervals, timedStepCopPlanner.getTimeAtStartOfInterval(), timedStepCopPlanner.getCopAtStartOfInterval(), timedStepCopPlanner.getTimeAtStartOfInterval(nIntervals - 1),
               timedStepCopPlanner.getCopAtStartOfInterval(nIntervals - 1));
         reverseDcmTrajectory.computeTrajectory(nextStep.getTimeInterval().getEndTime());
         reverseDcmTrajectory.getPosition(reverseDcmPositionAtEoNS);
         reverseDcmPositionAtEoNS.changeFrame(worldFrame);

         // compute this step goal position based on dcm offset
         double supportTime = nextStep.getTimeInterval().getEndTime() - thisStep.getTimeInterval().getEndTime();
         nominalDcmOffsetAtEoNS.setIncludingFrame(reverseDcmPositionAtEoNS);
         nominalDcmOffsetAtEoNS.sub(thisStep.getGoalPosition());
         computeStepGoalPosition(thisStep.getGoalPosition(), latestSteps.get(thisStepEnd.getOppositeEnd()).getGoalPosition(), forwardDcmPositionAtEoTS, nominalDcmOffsetAtEoNS, supportTime, dcmPositionController.getNaturalFrequency());
         groundPlaneEstimator.projectZ(thisStep.getGoalPosition());

         // compute new preview step goal positions
         goalPositionAdjustment.setToZero(worldFrame);
         goalPositionAdjustment.add(thisStep.getGoalPosition());
         goalPositionAdjustment.sub(xGaitPreviewSteps.get(0).getGoalPosition());
         for (int i = 0; i < xGaitPreviewSteps.size(); i++)
         {
            xGaitPreviewSteps.get(i).getGoalPosition().add(goalPositionAdjustment.getVector());
            groundPlaneEstimator.projectZ(xGaitPreviewSteps.get(i).getGoalPosition());
         }

         // update latest step
         latestSteps.get(thisStepEnd).set(thisStep);
      }

      @Override public void onTouchDown(RobotQuadrant thisStepQuadrant, QuadrantDependentList<ContactState> contactState)
      {
         double currentTime = robotTimestamp.getDoubleValue();

         // compute forward dcm trajectory
         int nIntervals = timedStepCopPlanner.compute(timedStepController.getQueue(), taskSpaceEstimates.getSolePosition(), contactState, currentTime - controlDT);
         forwardDcmTrajectory.setComHeight(dcmPositionController.getComHeight());
         forwardDcmTrajectory.initializeTrajectory(nIntervals, timedStepCopPlanner.getTimeAtStartOfInterval(), timedStepCopPlanner.getCopAtStartOfInterval(), dcmPositionEstimate);
      }

      @Override public XGaitEvent process()
      {
         // compute xgait preview steps
         double currentTime = robotTimestamp.getDoubleValue();
         xGaitStepPlanner.computeOnlinePlan(xGaitPreviewSteps, latestSteps, inputProvider.getPlanarVelocityInput(), currentTime, bodyYawSetpoint, xGaitSettings);
         timedStepController.removeSteps();
         for (int i = 0; i < xGaitPreviewSteps.size(); i++)
         {
            timedStepController.addStep(xGaitPreviewSteps.get(i));
         }

         // compute nominal dcm trajectory
         forwardDcmTrajectory.computeTrajectory(robotTimestamp.getDoubleValue());
         forwardDcmTrajectory.getPosition(dcmPositionControllerSetpoints.getDcmPosition());
         forwardDcmTrajectory.getVelocity(dcmPositionControllerSetpoints.getDcmVelocity());
         return null;
      }

      @Override public void onExit()
      {
         timedStepController.registerStepTransitionCallback(null);
      }
   }
}
