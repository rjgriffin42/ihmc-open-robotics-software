package us.ihmc.quadrupedRobotics.controller.force.states;

import us.ihmc.quadrupedRobotics.controller.ControllerEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerToolbox;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.*;
import us.ihmc.quadrupedRobotics.estimator.GroundPlaneEstimator;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.optimization.modelPredictiveControl.QuadrupedMpcOptimizationWithLaneChange;
import us.ihmc.quadrupedRobotics.optimization.modelPredictiveControl.QuadrupedDcmBasedMpcOptimizationWithLaneChange;
import us.ihmc.quadrupedRobotics.optimization.modelPredictiveControl.QuadrupedMpcOptimizationWithLaneChangeSettings;
import us.ihmc.quadrupedRobotics.params.DoubleArrayParameter;
import us.ihmc.quadrupedRobotics.params.DoubleParameter;
import us.ihmc.quadrupedRobotics.params.ParameterFactory;
import us.ihmc.quadrupedRobotics.planning.*;
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

import javax.vecmath.Vector3d;
import java.util.ArrayList;

public class QuadrupedMpcBasedXGaitController implements QuadrupedController, QuadrupedTimedStepTransitionCallback
{
   private final QuadrupedControllerInputProviderInterface inputProvider;
   private final QuadrupedXGaitSettingsProvider settingsProvider;
   private final DoubleYoVariable robotTimestamp;
   private final double controlDT;
   private final double gravity;
   private final double mass;
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   // parameters
   private final ParameterFactory parameterFactory = ParameterFactory.createWithRegistry(getClass(), registry);
   private final DoubleParameter mpcMaximumPreviewTimeParameter = parameterFactory.createDouble("maximumPreviewTime", 5);
   private final DoubleParameter mpcStepAdjustmentCostParameter = parameterFactory.createDouble("stepAdjustmentCost", 500);
   private final DoubleParameter mpcCopAdjustmentCostParameter = parameterFactory.createDouble("copAdjustmentCost", 1);
   private final DoubleArrayParameter bodyOrientationProportionalGainsParameter = parameterFactory
         .createDoubleArray("bodyOrientationProportionalGains", 5000, 5000, 5000);
   private final DoubleArrayParameter bodyOrientationDerivativeGainsParameter = parameterFactory
         .createDoubleArray("bodyOrientationDerivativeGains", 750, 750, 750);
   private final DoubleArrayParameter bodyOrientationIntegralGainsParameter = parameterFactory.createDoubleArray("bodyOrientationIntegralGains", 0, 0, 0);
   private final DoubleParameter bodyOrientationMaxIntegralErrorParameter = parameterFactory.createDouble("bodyOrientationMaxIntegralError", 0);
   private final DoubleArrayParameter comPositionProportionalGainsParameter = parameterFactory.createDoubleArray("comPositionProportionalGains", 0, 0, 5000);
   private final DoubleArrayParameter comPositionDerivativeGainsParameter = parameterFactory.createDoubleArray("comPositionDerivativeGains", 0, 0, 750);
   private final DoubleArrayParameter comPositionIntegralGainsParameter = parameterFactory.createDoubleArray("comPositionIntegralGains", 0, 0, 0);
   private final DoubleParameter comPositionMaxIntegralErrorParameter = parameterFactory.createDouble("comPositionMaxIntegralError", 0);
   private final DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 1);
   private final DoubleParameter jointPositionLimitDampingParameter = parameterFactory.createDouble("jointPositionLimitDamping", 10);
   private final DoubleParameter jointPositionLimitStiffnessParameter = parameterFactory.createDouble("jointPositionLimitStiffness", 100);
   private final DoubleParameter contactPressureLowerLimitParameter = parameterFactory.createDouble("contactPressureLowerLimit", 50);
   private final DoubleParameter initialTransitionDurationParameter = parameterFactory.createDouble("initialTransitionDuration", 0.25);
   private final DoubleParameter haltTransitionDurationParameter = parameterFactory.createDouble("haltTransitionDuration", 1.0);
   private final DoubleParameter minimumStepClearanceParameter = parameterFactory.createDouble("minimumStepClearance", 0.075);
   private final DoubleParameter maximumStepStrideParameter = parameterFactory.createDouble("maximumStepStride", 1.0);

   // frames
   private final ReferenceFrame supportFrame;
   private final ReferenceFrame worldFrame;

   // model
   private final LinearInvertedPendulumModel lipModel;

   // feedback controllers
   private final FramePoint cmpPositionSetpoint;
   private final QuadrupedComPositionController.Setpoints comPositionControllerSetpoints;
   private final QuadrupedComPositionController comPositionController;
   private final QuadrupedBodyOrientationController.Setpoints bodyOrientationControllerSetpoints;
   private final QuadrupedBodyOrientationController bodyOrientationController;
   private final QuadrupedTimedStepController timedStepController;
   private final QuadrupedMpcOptimizationWithLaneChange mpcOptimization;
   private final QuadrupedMpcOptimizationWithLaneChangeSettings mpcSettings;

   // task space controller
   private final QuadrupedTaskSpaceEstimator.Estimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;

   // planning
   private static int NUMBER_OF_PREVIEW_STEPS = 16;
   private double bodyYawSetpoint;
   private final GroundPlaneEstimator groundPlaneEstimator;
   private final QuadrantDependentList<FramePoint> groundPlanePositions;
   private final QuadrupedXGaitSettings xGaitSettings;
   private final FiniteStateMachine xGaitPlannerStateMachine;
   private final ArrayList<QuadrupedTimedStep> xGaitPreviewSteps;
   private EndDependentList<QuadrupedTimedStep> latestSteps;
   private final QuadrupedStepCrossoverProjection crossoverProjection;
   private final FrameVector stepAdjustmentVector;

   private enum XGaitPlannerState
   {
      ACTIVE, HALTED
   }

   private enum XGaitPlannerEvent
   {
      HALT
   }

   public QuadrupedMpcBasedXGaitController(QuadrupedRuntimeEnvironment runtimeEnvironment, QuadrupedForceControllerToolbox controllerToolbox,
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

      // model
      lipModel = controllerToolbox.getLinearInvertedPendulumModel();

      // feedback controllers
      cmpPositionSetpoint = new FramePoint();
      comPositionControllerSetpoints = new QuadrupedComPositionController.Setpoints();
      comPositionController = controllerToolbox.getComPositionController();
      bodyOrientationControllerSetpoints = new QuadrupedBodyOrientationController.Setpoints();
      bodyOrientationController = controllerToolbox.getBodyOrientationController();
      timedStepController = controllerToolbox.getTimedStepController();
      mpcOptimization = new QuadrupedDcmBasedMpcOptimizationWithLaneChange(controllerToolbox.getDcmPositionEstimator(), NUMBER_OF_PREVIEW_STEPS, registry,
            runtimeEnvironment.getGraphicsListRegistry());
      mpcSettings = new QuadrupedMpcOptimizationWithLaneChangeSettings(mpcMaximumPreviewTimeParameter.get(), mpcStepAdjustmentCostParameter.get(),
            mpcCopAdjustmentCostParameter.get());

      // task space controllers
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimator.Estimates();
      taskSpaceEstimator = controllerToolbox.getTaskSpaceEstimator();
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      taskSpaceController = controllerToolbox.getTaskSpaceController();

      // planning
      groundPlaneEstimator = controllerToolbox.getGroundPlaneEstimator();
      groundPlanePositions = new QuadrantDependentList<>();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.set(robotQuadrant, new FramePoint());
      }
      xGaitSettings = new QuadrupedXGaitSettings();
      FiniteStateMachineBuilder xGaitStepPlannerStateMachineBuilder = new FiniteStateMachineBuilder(XGaitPlannerState.class, XGaitPlannerEvent.class,
            "xGaitPlanner", registry);
      xGaitStepPlannerStateMachineBuilder.addState(XGaitPlannerState.ACTIVE, new ActiveXGaitPlanner());
      xGaitStepPlannerStateMachineBuilder.addState(XGaitPlannerState.HALTED, new HaltedXGaitPlanner());
      xGaitStepPlannerStateMachineBuilder.addTransition(XGaitPlannerEvent.HALT, XGaitPlannerState.ACTIVE, XGaitPlannerState.HALTED);
      xGaitPlannerStateMachine = xGaitStepPlannerStateMachineBuilder.build(XGaitPlannerState.ACTIVE);
      xGaitPreviewSteps = new ArrayList<>(NUMBER_OF_PREVIEW_STEPS);
      for (int i = 0; i < NUMBER_OF_PREVIEW_STEPS; i++)
      {
         xGaitPreviewSteps.add(new QuadrupedTimedStep());
      }
      latestSteps = new EndDependentList<>();
      for (RobotEnd robotEnd : RobotEnd.values)
      {
         latestSteps.set(robotEnd, new QuadrupedTimedStep());
      }
      stepAdjustmentVector = new FrameVector();
      crossoverProjection = new QuadrupedStepCrossoverProjection(referenceFrames.getBodyZUpFrame(), minimumStepClearanceParameter.get(),
            maximumStepStrideParameter.get());

      runtimeEnvironment.getParentRegistry().addChild(registry);
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public void halt()
   {
      xGaitPlannerStateMachine.trigger(XGaitPlannerEvent.HALT);
   }

   private void updateGains()
   {
      mpcSettings.setMaximumPreviewTime(mpcMaximumPreviewTimeParameter.get());
      mpcSettings.setStepAdjustmentCost(mpcStepAdjustmentCostParameter.get());
      mpcSettings.setCopAdjustmentCost(mpcCopAdjustmentCostParameter.get());
      comPositionController.getGains().setProportionalGains(comPositionProportionalGainsParameter.get());
      comPositionController.getGains().setIntegralGains(comPositionIntegralGainsParameter.get(), comPositionMaxIntegralErrorParameter.get());
      comPositionController.getGains().setDerivativeGains(comPositionDerivativeGainsParameter.get());
      bodyOrientationController.getGains().setProportionalGains(bodyOrientationProportionalGainsParameter.get());
      bodyOrientationController.getGains().setIntegralGains(bodyOrientationIntegralGainsParameter.get(), bodyOrientationMaxIntegralErrorParameter.get());
      bodyOrientationController.getGains().setDerivativeGains(bodyOrientationDerivativeGainsParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitDamping(jointPositionLimitDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitStiffness(jointPositionLimitStiffnessParameter.get());
      taskSpaceControllerSettings.getContactForceLimits().setPressureLowerLimit(contactPressureLowerLimitParameter.get());
   }

   private void updateEstimates()
   {
      // update model
      lipModel.setComHeight(inputProvider.getComPositionInput().getZ());

      // update task space estimates
      taskSpaceEstimator.compute(taskSpaceEstimates);
   }

   private void updateSetpoints()
   {
      // update desired horizontal com forces
      lipModel.computeComForce(taskSpaceControllerCommands.getComForce(), cmpPositionSetpoint);
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
      bodyYawSetpoint += inputProvider.getPlanarVelocityInput().getZ() * controlDT;
      bodyOrientationControllerSetpoints.getBodyOrientation().changeFrame(worldFrame);
      bodyOrientationControllerSetpoints.getBodyOrientation()
            .setYawPitchRoll(RotationTools.computeYaw(inputProvider.getBodyOrientationInput()) + bodyYawSetpoint,
                  RotationTools.computePitch(inputProvider.getBodyOrientationInput()) + groundPlaneEstimator.getPitch(bodyYawSetpoint),
                  RotationTools.computeRoll(inputProvider.getBodyOrientationInput()));
      bodyOrientationControllerSetpoints.getBodyAngularVelocity().setToZero();
      bodyOrientationControllerSetpoints.getComTorqueFeedforward().setToZero();
      bodyOrientationController.compute(taskSpaceControllerCommands.getComTorque(), bodyOrientationControllerSetpoints, taskSpaceEstimates);

      // update desired contact state and sole forces
      timedStepController.compute(taskSpaceControllerSettings.getContactState(), taskSpaceControllerCommands.getSoleForce(), taskSpaceEstimates);

      // update joint setpoints
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);

      // update step plan
      xGaitPlannerStateMachine.process();

      // update cmp position and step adjustment
      if (timedStepController.getQueueSize() > 0)
      {
         computeCmpPositionAndStepAdjustment();
      }
   }

   private void computeCmpPositionAndStepAdjustment()
   {
      double currentTime = robotTimestamp.getDoubleValue();

      mpcOptimization.compute(stepAdjustmentVector, cmpPositionSetpoint, timedStepController.getQueue(), taskSpaceEstimates.getSolePosition(),
            taskSpaceControllerSettings.getContactState(), taskSpaceEstimates.getComPosition(), taskSpaceEstimates.getComVelocity(), currentTime, mpcSettings);
      for (int i = 0; i < timedStepController.getQueue().size(); i++)
      {
         stepAdjustmentVector.changeFrame(worldFrame);
         timedStepController.getQueue().get(i).getGoalPosition().add(stepAdjustmentVector.getVector());
         groundPlaneEstimator.projectZ(timedStepController.getQueue().get(i).getGoalPosition());
      }
      for (RobotEnd robotEnd : RobotEnd.values)
      {
         if (timedStepController.getLatestStep(robotEnd) != null)
         {
            crossoverProjection.project(timedStepController.getLatestStep(robotEnd), taskSpaceEstimates.getSolePosition());
            groundPlaneEstimator.projectZ(timedStepController.getLatestStep(robotEnd).getGoalPosition());
         }
      }
   }

   private void updateXGaitSettings()
   {
      settingsProvider.getXGaitSettings(xGaitSettings);

      // increase stance dimensions to prevent self collisions
      double strideRotation = inputProvider.getPlanarVelocityInput().getZ() * xGaitSettings.getStepDuration();
      double strideLength = Math.abs(2 * inputProvider.getPlanarVelocityInput().getX() * xGaitSettings.getStepDuration());
      double strideWidth = Math.abs(2 * inputProvider.getPlanarVelocityInput().getY() * xGaitSettings.getStepDuration());
      strideLength += Math.abs(xGaitSettings.getStanceWidth() / 2 * Math.sin(2 * strideRotation));
      strideWidth += Math.abs(xGaitSettings.getStanceLength() / 2 * Math.sin(2 * strideRotation));
      xGaitSettings.setStanceLength(Math.max(xGaitSettings.getStanceLength(), strideLength / 2 + minimumStepClearanceParameter.get()));
      xGaitSettings.setStanceWidth(Math.max(xGaitSettings.getStanceWidth(), strideWidth / 2 + minimumStepClearanceParameter.get()));
   }

   @Override
   public void onLiftOff(RobotQuadrant thisStepQuadrant, QuadrantDependentList<ContactState> thisContactState)
   {
      // update ground plane estimate
      groundPlanePositions.get(thisStepQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(thisStepQuadrant));
      groundPlanePositions.get(thisStepQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
      groundPlaneEstimator.compute(groundPlanePositions);

      // update latest step
      RobotEnd thisStepEnd = thisStepQuadrant.getEnd();
      QuadrupedTimedStep thisStep = timedStepController.getLatestStep(thisStepQuadrant);
      latestSteps.get(thisStepEnd).set(thisStep);
   }

   @Override
   public void onTouchDown(RobotQuadrant thisStepQuadrant, QuadrantDependentList<ContactState> thisContactState)
   {
   }

   @Override
   public void onEntry()
   {
      updateXGaitSettings();
      updateEstimates();

      // initialize feedback controllers
      comPositionControllerSetpoints.initialize(taskSpaceEstimates);
      comPositionController.reset();
      bodyOrientationControllerSetpoints.initialize(taskSpaceEstimates);
      bodyOrientationController.reset();
      timedStepController.reset();
      timedStepController.registerStepTransitionCallback(this);

      // initialize task space controller
      taskSpaceControllerSettings.initialize();
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

      // initialize ground plane
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.get(robotQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(robotQuadrant));
         groundPlanePositions.get(robotQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
      }
      groundPlaneEstimator.compute(groundPlanePositions);

      xGaitPlannerStateMachine.process();
      computeCmpPositionAndStepAdjustment();
   }

   @Override
   public ControllerEvent process()
   {
      if (timedStepController.getQueueSize() == 0)
      {
         return ControllerEvent.DONE;
      }
      else
      {
         updateGains();
         updateXGaitSettings();
         updateEstimates();
         updateSetpoints();
         return null;
      }
   }

   @Override
   public void onExit()
   {
      xGaitPlannerStateMachine.reset();
      timedStepController.removeSteps();
      timedStepController.registerStepTransitionCallback(null);
   }

   private class ActiveXGaitPlanner implements FiniteStateMachineState<XGaitPlannerEvent>
   {
      private final QuadrupedXGaitPlanner xGaitStepPlanner;
      private final FramePoint supportCentroid;

      public ActiveXGaitPlanner()
      {
         xGaitStepPlanner = new QuadrupedXGaitPlanner();
         supportCentroid = new FramePoint();
      }

      @Override
      public void onEntry()
      {
         // initialize step plan
         double initialTime = robotTimestamp.getDoubleValue() + initialTransitionDurationParameter.get();
         RobotQuadrant initialQuadrant = (xGaitSettings.getEndPhaseShift() < 90) ? RobotQuadrant.HIND_LEFT : RobotQuadrant.FRONT_LEFT;
         Vector3d planarVelocity = inputProvider.getPlanarVelocityInput();
         supportCentroid.setToZero(supportFrame);
         xGaitStepPlanner.computeInitialPlan(xGaitPreviewSteps, planarVelocity, initialQuadrant, supportCentroid, initialTime, bodyYawSetpoint, xGaitSettings);
         for (int i = 0; i < xGaitPreviewSteps.size(); i++)
            timedStepController.addStep(xGaitPreviewSteps.get(i));
         for (RobotEnd robotEnd : RobotEnd.values)
            latestSteps.get(robotEnd).set(timedStepController.getLatestStep(robotEnd));
      }

      @Override
      public XGaitPlannerEvent process()
      {
         double currentTime = robotTimestamp.getDoubleValue();
         Vector3d planarVelocity = inputProvider.getPlanarVelocityInput();
         xGaitStepPlanner.computeOnlinePlan(xGaitPreviewSteps, latestSteps, planarVelocity, currentTime, bodyYawSetpoint, xGaitSettings);
         timedStepController.removeSteps();
         for (RobotEnd robotEnd : RobotEnd.values)
         {
            if (timedStepController.getLatestStep(robotEnd) != null)
               timedStepController.getLatestStep(robotEnd).set(latestSteps.get(robotEnd));
            else
               timedStepController.addStep(latestSteps.get(robotEnd));
         }
         for (int i = 0; i < xGaitPreviewSteps.size(); i++)
            timedStepController.addStep(xGaitPreviewSteps.get(i));
         return null;
      }

      @Override
      public void onExit()
      {
         double currentTime = robotTimestamp.getDoubleValue();
         Vector3d planarVelocity = inputProvider.getPlanarVelocityInput();
         planarVelocity.set(0.0, 0.0, 0.0);
         xGaitStepPlanner.computeOnlinePlan(xGaitPreviewSteps, latestSteps, planarVelocity, currentTime, bodyYawSetpoint, xGaitSettings);
      }
   }

   private class HaltedXGaitPlanner implements FiniteStateMachineState<XGaitPlannerEvent>
   {
      double haltTime;

      @Override
      public void onEntry()
      {
         haltTime = robotTimestamp.getDoubleValue();
      }

      @Override
      public XGaitPlannerEvent process()
      {
         timedStepController.removeSteps();
         for (RobotEnd robotEnd : RobotEnd.values)
         {
            if (timedStepController.getLatestStep(robotEnd) != null)
               timedStepController.getLatestStep(robotEnd).set(latestSteps.get(robotEnd));
            else
               timedStepController.addStep(latestSteps.get(robotEnd));
         }
         for (int i = 0; i < xGaitPreviewSteps.size(); i++)
         {
            if (xGaitPreviewSteps.get(i).getTimeInterval().getEndTime() < haltTime + haltTransitionDurationParameter.get())
               timedStepController.addStep(xGaitPreviewSteps.get(i));
         }
         return null;
      }

      @Override
      public void onExit()
      {

      }
   }
}
