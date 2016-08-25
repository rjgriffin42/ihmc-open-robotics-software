package us.ihmc.quadrupedRobotics.controller.force.states;

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
import us.ihmc.quadrupedRobotics.planning.ContactState;
import us.ihmc.quadrupedRobotics.planning.QuadrupedStepCrossoverProjection;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.QuadrupedTimedStepPressurePlanner;
import us.ihmc.quadrupedRobotics.planning.stepStream.QuadrupedStepStream;
import us.ihmc.quadrupedRobotics.planning.trajectory.PiecewiseReverseDcmTrajectory;
import us.ihmc.quadrupedRobotics.planning.trajectory.ThreeDoFMinimumJerkTrajectory;
import us.ihmc.quadrupedRobotics.providers.QuadrupedPostureInputProviderInterface;
import us.ihmc.quadrupedRobotics.util.PreallocatedQueue;
import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.Direction;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.OrientationFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class QuadrupedDcmBasedStepController implements QuadrupedController, QuadrupedTimedStepTransitionCallback
{
   private final QuadrupedPostureInputProviderInterface postureProvider;
   private final QuadrupedStepStream stepStream;
   private final DoubleYoVariable robotTimestamp;
   private final double controlDT;
   private final double gravity;
   private final double mass;
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   // parameters
   private final ParameterFactory parameterFactory = ParameterFactory.createWithRegistry(getClass(), registry);
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
   private final DoubleParameter comPositionGravityCompensationParameter = parameterFactory.createDouble("comPositionGravityCompensation", 1);
   private final DoubleArrayParameter comForceCommandWeightsParameter = parameterFactory.createDoubleArray("comForceCommandWeights", 1, 1, 1);
   private final DoubleArrayParameter comTorqueCommandWeightsParameter = parameterFactory.createDoubleArray("comTorqueCommandWeights", 1, 1, 1);
   private final DoubleArrayParameter dcmPositionProportionalGainsParameter = parameterFactory.createDoubleArray("dcmPositionProportionalGains", 1, 1, 0);
   private final DoubleArrayParameter dcmPositionDerivativeGainsParameter = parameterFactory.createDoubleArray("dcmPositionDerivativeGains", 0, 0, 0);
   private final DoubleArrayParameter dcmPositionIntegralGainsParameter = parameterFactory.createDoubleArray("dcmPositionIntegralGains", 0, 0, 0);
   private final DoubleParameter dcmPositionMaxIntegralErrorParameter = parameterFactory.createDouble("dcmPositionMaxIntegralError", 0);
   private final DoubleParameter dcmPositionStepAdjustmentGainParameter = parameterFactory.createDouble("dcmPositionStepAdjustmentGain", 1.5);
   private final DoubleParameter dcmPositionStepAdjustmentDeadbandParameter = parameterFactory.createDouble("dcmPositionStepAdjustmentDeadband", 0.0);
   private final DoubleParameter vrpPositionRateLimitParameter = parameterFactory.createDouble("vrpPositionRateLimit", Double.MAX_VALUE);
   private final DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 1);
   private final DoubleParameter jointPositionLimitDampingParameter = parameterFactory.createDouble("jointPositionLimitDamping", 10);
   private final DoubleParameter jointPositionLimitStiffnessParameter = parameterFactory.createDouble("jointPositionLimitStiffness", 100);
   private final DoubleParameter initialTransitionDurationParameter = parameterFactory.createDouble("initialTransitionDuration", 0.5);
   private final DoubleParameter haltTransitionDurationParameter = parameterFactory.createDouble("haltTransitionDuration", 1.0);
   private final DoubleParameter minimumStepClearanceParameter = parameterFactory.createDouble("minimumStepClearance", 0.075);
   private final DoubleParameter maximumStepStrideParameter = parameterFactory.createDouble("maximumStepStride", 1.0);

   // frames
   private final ReferenceFrame supportFrame;
   private final ReferenceFrame worldFrame;

   // model
   private final LinearInvertedPendulumModel lipModel;

   // feedback controllers
   private final FramePoint dcmPositionEstimate;
   private final DivergentComponentOfMotionEstimator dcmPositionEstimator;
   private final DivergentComponentOfMotionController.Setpoints dcmPositionControllerSetpoints;
   private final DivergentComponentOfMotionController dcmPositionController;
   private final QuadrupedComPositionController.Setpoints comPositionControllerSetpoints;
   private final QuadrupedComPositionController comPositionController;
   private final QuadrupedBodyOrientationController.Setpoints bodyOrientationControllerSetpoints;
   private final QuadrupedBodyOrientationController bodyOrientationController;
   private final QuadrupedTimedStepController timedStepController;

   // task space controller
   private final QuadrupedTaskSpaceEstimator.Estimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;

   // step planner
   private static int MAXIMUM_STEP_QUEUE_SIZE = 100;
   private final GroundPlaneEstimator groundPlaneEstimator;
   private final QuadrantDependentList<FramePoint> groundPlanePositions;
   private final QuadrupedTimedStepPressurePlanner copPlanner;
   private final PiecewiseReverseDcmTrajectory dcmTrajectory;
   private final ThreeDoFMinimumJerkTrajectory dcmTransitionTrajectory;
   private final FramePoint dcmPositionWaypoint;
   private final YoFrameVector stepAdjustmentForControl;
   private final YoFrameVector stepAdjustmentForPlanning;
   private final YoFrameVector accumulatedStepAdjustmentForPlanning;
   private final QuadrupedStepCrossoverProjection crossoverProjection;
   private final PreallocatedQueue<QuadrupedTimedStep> stepPlan;
   private final FrameOrientation bodyOrientationReference;
   private final OrientationFrame bodyOrientationReferenceFrame;

   // inputs
   private final DoubleYoVariable haltTime = new DoubleYoVariable("haltTime", registry);
   private final BooleanYoVariable haltFlag = new BooleanYoVariable("haltFlag", registry);
   private final BooleanYoVariable onLiftOffTriggered = new BooleanYoVariable("onLiftOffTriggered", registry);
   private final BooleanYoVariable onTouchDownTriggered = new BooleanYoVariable("onTouchDownTriggered", registry);

   public QuadrupedDcmBasedStepController(QuadrupedRuntimeEnvironment runtimeEnvironment, QuadrupedForceControllerToolbox controllerToolbox,
         QuadrupedPostureInputProviderInterface postureProvider, QuadrupedStepStream stepStream)
   {
      this.postureProvider = postureProvider;
      this.stepStream = stepStream;
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
      dcmPositionEstimate = new FramePoint();
      dcmPositionEstimator = controllerToolbox.getDcmPositionEstimator();
      dcmPositionControllerSetpoints = new DivergentComponentOfMotionController.Setpoints();
      dcmPositionController = controllerToolbox.getDcmPositionController();
      comPositionControllerSetpoints = new QuadrupedComPositionController.Setpoints();
      comPositionController = controllerToolbox.getComPositionController();
      bodyOrientationControllerSetpoints = new QuadrupedBodyOrientationController.Setpoints();
      bodyOrientationController = controllerToolbox.getBodyOrientationController();
      timedStepController = controllerToolbox.getTimedStepController();

      // task space controllers
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimator.Estimates();
      taskSpaceEstimator = controllerToolbox.getTaskSpaceEstimator();
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      taskSpaceController = controllerToolbox.getTaskSpaceController();

      // step planner
      groundPlaneEstimator = controllerToolbox.getGroundPlaneEstimator();
      groundPlanePositions = new QuadrantDependentList<>();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.set(robotQuadrant, new FramePoint());
      }
      copPlanner = new QuadrupedTimedStepPressurePlanner(timedStepController.getQueueCapacity());
      dcmTrajectory = new PiecewiseReverseDcmTrajectory(timedStepController.getQueueCapacity(), gravity, postureProvider.getComPositionInput().getZ());
      dcmTransitionTrajectory = new ThreeDoFMinimumJerkTrajectory();
      dcmPositionWaypoint = new FramePoint();
      stepAdjustmentForControl = new YoFrameVector("stepAdjustmentForControl", worldFrame, registry);
      stepAdjustmentForPlanning = new YoFrameVector("stepAdjustmentForPlanning", worldFrame, registry);
      accumulatedStepAdjustmentForPlanning = new YoFrameVector("accumulatedStepAdjustmentForPlanning", worldFrame, registry);
      crossoverProjection = new QuadrupedStepCrossoverProjection(referenceFrames.getBodyZUpFrame(), minimumStepClearanceParameter.get(),
            maximumStepStrideParameter.get());
      stepPlan = new PreallocatedQueue<>(QuadrupedTimedStep.class, MAXIMUM_STEP_QUEUE_SIZE);
      bodyOrientationReference = new FrameOrientation();
      bodyOrientationReferenceFrame = new OrientationFrame(bodyOrientationReference);

      runtimeEnvironment.getParentRegistry().addChild(registry);
   }

   private void updateGains()
   {
      dcmPositionController.setVrpPositionRateLimit(vrpPositionRateLimitParameter.get());
      dcmPositionController.getGains().setProportionalGains(dcmPositionProportionalGainsParameter.get());
      dcmPositionController.getGains().setIntegralGains(dcmPositionIntegralGainsParameter.get(), dcmPositionMaxIntegralErrorParameter.get());
      dcmPositionController.getGains().setDerivativeGains(dcmPositionDerivativeGainsParameter.get());
      comPositionController.getGains().setProportionalGains(comPositionProportionalGainsParameter.get());
      comPositionController.getGains().setIntegralGains(comPositionIntegralGainsParameter.get(), comPositionMaxIntegralErrorParameter.get());
      comPositionController.getGains().setDerivativeGains(comPositionDerivativeGainsParameter.get());
      bodyOrientationController.getGains().setProportionalGains(bodyOrientationProportionalGainsParameter.get());
      bodyOrientationController.getGains().setIntegralGains(bodyOrientationIntegralGainsParameter.get(), bodyOrientationMaxIntegralErrorParameter.get());
      bodyOrientationController.getGains().setDerivativeGains(bodyOrientationDerivativeGainsParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitDamping(jointPositionLimitDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitStiffness(jointPositionLimitStiffnessParameter.get());
      taskSpaceControllerSettings.getContactForceOptimizationSettings().setComForceCommandWeights(comForceCommandWeightsParameter.get());
      taskSpaceControllerSettings.getContactForceOptimizationSettings().setComTorqueCommandWeights(comTorqueCommandWeightsParameter.get());
   }

   private void updateEstimates()
   {
      // update model
      lipModel.setComHeight(postureProvider.getComPositionInput().getZ());

      // update task space estimates
      taskSpaceEstimator.compute(taskSpaceEstimates);

      // update dcm estimate
      dcmPositionEstimator.compute(dcmPositionEstimate, taskSpaceEstimates.getComVelocity());
   }

   private void updateSetpoints()
   {
      // update step planner
      stepStream.process();
      stepStream.getBodyOrientation(bodyOrientationReference);

      // update desired horizontal com forces
      computeDcmSetpoints();
      dcmPositionController.compute(taskSpaceControllerCommands.getComForce(), dcmPositionControllerSetpoints, dcmPositionEstimate);
      taskSpaceControllerCommands.getComForce().changeFrame(supportFrame);

      // update desired com position, velocity, and vertical force
      comPositionControllerSetpoints.getComPosition().changeFrame(supportFrame);
      comPositionControllerSetpoints.getComPosition().set(postureProvider.getComPositionInput());
      comPositionControllerSetpoints.getComVelocity().changeFrame(supportFrame);
      comPositionControllerSetpoints.getComVelocity().set(postureProvider.getComVelocityInput());
      comPositionControllerSetpoints.getComForceFeedforward().changeFrame(supportFrame);
      comPositionControllerSetpoints.getComForceFeedforward().set(taskSpaceControllerCommands.getComForce());
      comPositionControllerSetpoints.getComForceFeedforward().setZ(comPositionGravityCompensationParameter.get() * mass * gravity);
      comPositionController.compute(taskSpaceControllerCommands.getComForce(), comPositionControllerSetpoints, taskSpaceEstimates);

      // update desired body orientation, angular velocity, and torque
      bodyOrientationReferenceFrame.update();
      bodyOrientationControllerSetpoints.getBodyOrientation().changeFrame(bodyOrientationReferenceFrame);
      bodyOrientationControllerSetpoints.getBodyOrientation().set(postureProvider.getBodyOrientationInput());
      bodyOrientationControllerSetpoints.getBodyOrientation().changeFrame(worldFrame);
      double bodyOrientationYaw = bodyOrientationControllerSetpoints.getBodyOrientation().getYaw();
      double bodyOrientationPitch = bodyOrientationControllerSetpoints.getBodyOrientation().getPitch();
      double bodyOrientationRoll = bodyOrientationControllerSetpoints.getBodyOrientation().getRoll();
      bodyOrientationControllerSetpoints.getBodyOrientation()
            .setYawPitchRoll(bodyOrientationYaw, bodyOrientationPitch + groundPlaneEstimator.getPitch(bodyOrientationYaw), bodyOrientationRoll);
      bodyOrientationControllerSetpoints.getBodyAngularVelocity().setToZero();
      bodyOrientationControllerSetpoints.getComTorqueFeedforward().setToZero();
      bodyOrientationController.compute(taskSpaceControllerCommands.getComTorque(), bodyOrientationControllerSetpoints, taskSpaceEstimates);

      // update desired contact state and sole forces
      timedStepController.compute(taskSpaceControllerSettings.getContactState(), taskSpaceControllerSettings.getContactForceLimits(),
            taskSpaceControllerCommands.getSoleForce(), taskSpaceEstimates);

      // update joint setpoints
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);

      // update absolute step adjustment
      computeAbsoluteStepAdjustment();

      // update relative step adjustment
      computeRelativeStepAdjustment();

      // update dcm trajectory
      computeDcmTrajectory();

      // update accumulated step adjustment
      if (onLiftOffTriggered.getBooleanValue())
      {
         onLiftOffTriggered.set(false);
      }
      if (onTouchDownTriggered.getBooleanValue())
      {
         onTouchDownTriggered.set(false);
         accumulatedStepAdjustmentForPlanning.add(stepAdjustmentForPlanning.getFrameTuple().getVector());
         accumulatedStepAdjustmentForPlanning.setZ(0);
      }
   }

   private void computeDcmTrajectory()
   {
      double currentTime = robotTimestamp.getDoubleValue();
      int nIntervals = copPlanner.compute(stepPlan.size(), stepPlan, taskSpaceEstimates.getSolePosition(),
            taskSpaceControllerSettings.getContactState(), currentTime);
      dcmPositionWaypoint.setIncludingFrame(copPlanner.getCenterOfPressureAtStartOfInterval(nIntervals - 1));
      dcmPositionWaypoint.changeFrame(ReferenceFrame.getWorldFrame());
      dcmPositionWaypoint.add(0, 0, lipModel.getComHeight());
      dcmTrajectory.setComHeight(lipModel.getComHeight());
      dcmTrajectory.initializeTrajectory(nIntervals, copPlanner.getTimeAtStartOfInterval(), copPlanner.getCenterOfPressureAtStartOfInterval(),
            copPlanner.getTimeAtStartOfInterval(nIntervals - 1), dcmPositionWaypoint);
   }

   private void computeDcmSetpoints()
   {
      if (robotTimestamp.getDoubleValue() <= dcmTransitionTrajectory.getEndTime())
      {
         dcmTransitionTrajectory.computeTrajectory(robotTimestamp.getDoubleValue());
         dcmTransitionTrajectory.getPosition(dcmPositionControllerSetpoints.getDcmPosition());
         dcmTransitionTrajectory.getVelocity(dcmPositionControllerSetpoints.getDcmVelocity());
      }
      else
      {
         dcmTrajectory.computeTrajectory(robotTimestamp.getDoubleValue());
         dcmTrajectory.getPosition(dcmPositionControllerSetpoints.getDcmPosition());
         dcmTrajectory.getVelocity(dcmPositionControllerSetpoints.getDcmVelocity());
      }
   }

   private void computeAbsoluteStepAdjustment()
   {
      // update step plan
      stepPlan.clear();
      for (int i = 0; i < stepStream.getSteps().size(); i++)
      {
         if (!haltFlag.getBooleanValue() || stepStream.getSteps().get(i).getTimeInterval().getEndTime() < haltTime.getDoubleValue())
         {
            stepPlan.enqueue();
            stepPlan.getTail().set(stepStream.getSteps().get(i));
            stepPlan.getTail().getGoalPosition().add(accumulatedStepAdjustmentForPlanning.getFrameTuple().getVector());
         }
      }
   }


   private void computeRelativeStepAdjustment()
   {
      if (robotTimestamp.getDoubleValue() > dcmTransitionTrajectory.getEndTime())
      {
         // compute step adjustment for ongoing steps (proportional to dcm tracking error)
         FramePoint dcmPositionSetpoint = dcmPositionControllerSetpoints.getDcmPosition();
         dcmPositionSetpoint.changeFrame(stepAdjustmentForControl.getReferenceFrame());
         dcmPositionEstimate.changeFrame(stepAdjustmentForControl.getReferenceFrame());
         stepAdjustmentForControl.set(dcmPositionEstimate);
         stepAdjustmentForControl.sub(dcmPositionSetpoint);
         stepAdjustmentForControl.scale(dcmPositionStepAdjustmentGainParameter.get());
         stepAdjustmentForControl.setZ(0);

         // compute step adjustment for upcoming steps (apply horizontal deadband to reduce drift)
         for (Direction direction : Direction.values2D())
         {
            double deadband = dcmPositionStepAdjustmentDeadbandParameter.get();
            stepAdjustmentForPlanning.set(direction, MathTools.applyDeadband(stepAdjustmentForControl.get(direction), deadband));
         }

         // adjust nominal step goal positions and update step controller queue
         timedStepController.removeSteps();
         for (int i = 0; i < stepPlan.size(); i++)
         {
            RobotQuadrant robotQuadrant = stepPlan.get(i).getRobotQuadrant();
            double currentTime = robotTimestamp.getDoubleValue();
            double startTime = stepPlan.get(i).getTimeInterval().getStartTime();
            double endTime = stepPlan.get(i).getTimeInterval().getEndTime();
            if (startTime < currentTime && currentTime < endTime)
            {
               QuadrupedTimedStep adjustedStep = timedStepController.getCurrentStep(robotQuadrant);
               if (adjustedStep != null)
               {
                  adjustedStep.getGoalPosition().set(stepPlan.get(i).getGoalPosition());
                  adjustedStep.getGoalPosition().add(stepAdjustmentForControl.getFrameTuple().getVector());
                  crossoverProjection.project(adjustedStep, taskSpaceEstimates.getSolePosition());
                  groundPlaneEstimator.projectZ(adjustedStep.getGoalPosition());
               }
            }
            else if (timedStepController.addStep(stepPlan.get(i)))
            {
               QuadrupedTimedStep adjustedStep = timedStepController.getQueue().getTail();
               adjustedStep.getGoalPosition().add(stepAdjustmentForPlanning.getFrameTuple().getVector());
               groundPlaneEstimator.projectZ(adjustedStep.getGoalPosition());
            }
         }
      }
   }

   @Override
   public void onLiftOff(RobotQuadrant thisStepQuadrant, QuadrantDependentList<ContactState> thisContactState)
   {
      // update ground plane estimate
      groundPlanePositions.get(thisStepQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(thisStepQuadrant));
      groundPlanePositions.get(thisStepQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
      groundPlaneEstimator.compute(groundPlanePositions);

      onLiftOffTriggered.set(true);
   }

   @Override
   public void onTouchDown(RobotQuadrant thisStepQuadrant, QuadrantDependentList<ContactState> thisContactState)
   {
      onTouchDownTriggered.set(true);
   }

   @Override
   public void onEntry()
   {
      // initialize state
      haltFlag.set(false);
      onLiftOffTriggered.set(false);
      onTouchDownTriggered.set(false);
      accumulatedStepAdjustmentForPlanning.setToZero();

      // initialize estimates
      lipModel.setComHeight(postureProvider.getComPositionInput().getZ());
      updateEstimates();

      // initialize feedback controllers
      dcmPositionControllerSetpoints.initialize(dcmPositionEstimate);
      dcmPositionController.reset();
      comPositionControllerSetpoints.initialize(taskSpaceEstimates);
      comPositionController.reset();
      bodyOrientationControllerSetpoints.initialize(taskSpaceEstimates);
      bodyOrientationController.reset();
      timedStepController.reset();
      timedStepController.registerStepTransitionCallback(this);

      // initialize task space controller
      taskSpaceControllerSettings.initialize();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.getContactForceOptimizationSettings().setContactForceCommandWeights(robotQuadrant, 0.0, 0.0, 0.0);
         taskSpaceControllerSettings.setContactState(robotQuadrant, ContactState.IN_CONTACT);
      }
      taskSpaceController.reset();

      // initialize ground plane
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.get(robotQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(robotQuadrant));
         groundPlanePositions.get(robotQuadrant).changeFrame(ReferenceFrame.getWorldFrame());
      }
      groundPlaneEstimator.compute(groundPlanePositions);

      // initialize step plan
      stepStream.onEntry();
      stepStream.process();

      // compute absolute step adjustment
      computeAbsoluteStepAdjustment();

      // compute relative step adjustment
      computeRelativeStepAdjustment();

      // compute dcm trajectory
      computeDcmTrajectory();
      double transitionEndTime = copPlanner.getTimeAtStartOfInterval(1);
      double transitionStartTime = Math.max(robotTimestamp.getDoubleValue(), transitionEndTime - initialTransitionDurationParameter.get());
      dcmTrajectory.computeTrajectory(transitionEndTime);
      dcmTrajectory.getPosition(dcmPositionWaypoint);
      dcmTransitionTrajectory.initializeTrajectory(dcmPositionEstimate, dcmPositionWaypoint, transitionStartTime, transitionEndTime);
      computeDcmSetpoints();
   }

   @Override
   public ControllerEvent process()
   {
      if (timedStepController.getQueueSize() == 0)
      {
         return ControllerEvent.DONE;
      }
      updateGains();
      updateEstimates();
      updateSetpoints();
      return null;
   }

   @Override
   public void onExit()
   {
      // remove remaining steps from the queue
      timedStepController.removeSteps();
      timedStepController.registerStepTransitionCallback(null);
   }

   public void halt()
   {
      haltFlag.set(true);
      haltTime.set(robotTimestamp.getDoubleValue() + haltTransitionDurationParameter.get());
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }
}
