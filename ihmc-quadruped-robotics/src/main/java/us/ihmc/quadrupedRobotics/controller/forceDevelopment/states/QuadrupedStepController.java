package us.ihmc.quadrupedRobotics.controller.forceDevelopment.states;

import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.quadrupedRobotics.controlModules.QuadrupedBalanceManager;
import us.ihmc.quadrupedRobotics.controlModules.QuadrupedBodyOrientationManager;
import us.ihmc.quadrupedRobotics.controlModules.QuadrupedControlManagerFactory;
import us.ihmc.quadrupedRobotics.controlModules.foot.QuadrupedFeetManager;
import us.ihmc.quadrupedRobotics.controller.ControllerEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedForceControllerToolbox;
import us.ihmc.quadrupedRobotics.controller.force.toolbox.*;
import us.ihmc.quadrupedRobotics.estimator.GroundPlaneEstimator;
import us.ihmc.quadrupedRobotics.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.messageHandling.QuadrupedStepMessageHandler;
import us.ihmc.quadrupedRobotics.planning.ContactState;
import us.ihmc.quadrupedRobotics.planning.QuadrupedStepCrossoverProjection;
import us.ihmc.quadrupedRobotics.planning.YoQuadrupedTimedStep;
import us.ihmc.quadrupedRobotics.planning.stepStream.QuadrupedStepStream;
import us.ihmc.quadrupedRobotics.planning.trajectory.ThreeDoFMinimumJerkTrajectory;
import us.ihmc.quadrupedRobotics.providers.QuadrupedPostureInputProviderInterface;
import us.ihmc.robotics.dataStructures.parameter.DoubleArrayParameter;
import us.ihmc.robotics.dataStructures.parameter.DoubleParameter;
import us.ihmc.robotics.dataStructures.parameter.ParameterFactory;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

import java.util.ArrayList;

public class QuadrupedStepController implements QuadrupedController, QuadrupedStepTransitionCallback
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final QuadrupedStepStream stepStream;
   private final YoDouble robotTimestamp;
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   // parameters
   private final ParameterFactory parameterFactory = ParameterFactory.createWithRegistry(getClass(), registry);
   private final DoubleArrayParameter comForceCommandWeightsParameter = parameterFactory.createDoubleArray("comForceCommandWeights", 1, 1, 1);
   private final DoubleArrayParameter comTorqueCommandWeightsParameter = parameterFactory.createDoubleArray("comTorqueCommandWeights", 1, 1, 1);
   private final DoubleParameter dcmPositionStepAdjustmentGainParameter = parameterFactory.createDouble("dcmPositionStepAdjustmentGain", 1.5);
   private final DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 1);
   private final DoubleParameter jointPositionLimitDampingParameter = parameterFactory.createDouble("jointPositionLimitDamping", 10);
   private final DoubleParameter jointPositionLimitStiffnessParameter = parameterFactory.createDouble("jointPositionLimitStiffness", 100);
   private final DoubleParameter minimumStepClearanceParameter = parameterFactory.createDouble("minimumStepClearance", 0.075);
   private final DoubleParameter maximumStepStrideParameter = parameterFactory.createDouble("maximumStepStride", 1.0);
   private final DoubleParameter coefficientOfFrictionParameter = parameterFactory.createDouble("coefficientOfFriction", 0.5);

   private final QuadrupedStepMessageHandler stepMessageHandler;

   // managers
   private final QuadrupedFeetManager feetManager;
   private final QuadrupedBalanceManager balanceManager;
   private final QuadrupedBodyOrientationManager bodyOrientationManager;

   // task space controller
   private final QuadrupedTaskSpaceEstimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;

   // step planner
   private final GroundPlaneEstimator groundPlaneEstimator;
   private final QuadrantDependentList<YoFramePoint> groundPlanePositions;

   private final ThreeDoFMinimumJerkTrajectory dcmTransitionTrajectory;

   private final YoFrameVector instantaneousStepAdjustment;
   private final YoFrameVector accumulatedStepAdjustment;
   private final QuadrupedStepCrossoverProjection crossoverProjection;

   private final FramePoint3D stepGoalPosition;

   // inputs
   private final YoBoolean onLiftOffTriggered = new YoBoolean("onLiftOffTriggered", registry);
   private final YoBoolean onTouchDownTriggered = new YoBoolean("onTouchDownTriggered", registry);

   public QuadrupedStepController(QuadrupedForceControllerToolbox controllerToolbox, QuadrupedControlManagerFactory controlManagerFactory,
                                  QuadrupedPostureInputProviderInterface postureProvider, QuadrupedStepStream stepStream,
                                  YoVariableRegistry parentRegistry)
   {
      this.stepStream = stepStream;
      this.robotTimestamp = controllerToolbox.getRuntimeEnvironment().getRobotTimestamp();

      // frames
      QuadrupedReferenceFrames referenceFrames = controllerToolbox.getReferenceFrames();

      stepMessageHandler = new QuadrupedStepMessageHandler(stepStream, robotTimestamp, registry);

      // feedback controllers
      feetManager = controlManagerFactory.getOrCreateFeetManager();

      // task space controllers
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimates();
      taskSpaceEstimator = controllerToolbox.getTaskSpaceEstimator();
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      taskSpaceController = controllerToolbox.getTaskSpaceController();

      // step planner
      groundPlaneEstimator = controllerToolbox.getGroundPlaneEstimator();
      groundPlanePositions = new QuadrantDependentList<>();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.set(robotQuadrant, new YoFramePoint(robotQuadrant.getCamelCaseName() + "GroundPlanePosition", worldFrame, registry));
      }
      dcmTransitionTrajectory = new ThreeDoFMinimumJerkTrajectory();
      instantaneousStepAdjustment = new YoFrameVector("instantaneousStepAdjustment", worldFrame, registry);
      accumulatedStepAdjustment = new YoFrameVector("accumulatedStepAdjustment", worldFrame, registry);
      crossoverProjection = new QuadrupedStepCrossoverProjection(referenceFrames.getBodyZUpFrame(), minimumStepClearanceParameter.get(),
            maximumStepStrideParameter.get());
      stepGoalPosition = new FramePoint3D();

      balanceManager = new QuadrupedBalanceManager(controllerToolbox, stepMessageHandler.getStepSequence(), postureProvider, dcmTransitionTrajectory, registry);
      bodyOrientationManager = controlManagerFactory.getOrCreateBodyOrientationManager();

      parentRegistry.addChild(registry);
   }

   private void updateGains()
   {
      taskSpaceControllerSettings.getContactForceLimits().setCoefficientOfFriction(coefficientOfFrictionParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitDamping(jointPositionLimitDampingParameter.get());
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointPositionLimitStiffness(jointPositionLimitStiffnessParameter.get());
      taskSpaceControllerSettings.getContactForceOptimizationSettings().setComForceCommandWeights(comForceCommandWeightsParameter.get());
      taskSpaceControllerSettings.getContactForceOptimizationSettings().setComTorqueCommandWeights(comTorqueCommandWeightsParameter.get());
   }

   private void triggerStepEvents()
   {
      ArrayList<YoQuadrupedTimedStep> activeSteps = stepMessageHandler.getActiveSteps();
      for (int i = 0; i < activeSteps.size(); i++)
      {
         feetManager.triggerStep(activeSteps.get(i));
      }
   }

   private void computeStepAdjustment()
   {
      if (robotTimestamp.getDoubleValue() > dcmTransitionTrajectory.getEndTime())
      {
         // compute step adjustment for ongoing steps (proportional to dcm tracking error)
         FramePoint3D dcmPositionSetpoint = balanceManager.getDCMPositionSetpoint();
         FramePoint3D dcmPositionEstimate = balanceManager.getDCMPositionEstimate();
         dcmPositionSetpoint.changeFrame(instantaneousStepAdjustment.getReferenceFrame());
         dcmPositionEstimate.changeFrame(instantaneousStepAdjustment.getReferenceFrame());

         instantaneousStepAdjustment.sub(dcmPositionEstimate, dcmPositionSetpoint);
         instantaneousStepAdjustment.scale(dcmPositionStepAdjustmentGainParameter.get());
         instantaneousStepAdjustment.setZ(0);

         // adjust nominal step goal positions in foot state machine
         ArrayList<YoQuadrupedTimedStep> activeSteps = stepMessageHandler.getActiveSteps();
         for (int i = 0; i < activeSteps.size(); i++)
         {
            YoQuadrupedTimedStep activeStep = activeSteps.get(i);
            RobotQuadrant robotQuadrant = activeStep.getRobotQuadrant();
            activeStep.getGoalPosition(stepGoalPosition);
            stepGoalPosition.changeFrame(worldFrame);
            stepGoalPosition.add(instantaneousStepAdjustment);
            crossoverProjection.project(stepGoalPosition, taskSpaceEstimates.getSolePosition(), robotQuadrant);
            groundPlaneEstimator.projectZ(stepGoalPosition);
            feetManager.adjustStep(robotQuadrant, stepGoalPosition);
         }
      }
   }

   @Override
   public void onLiftOff(RobotQuadrant thisStepQuadrant)
   {
      // update ground plane estimate
      groundPlanePositions.get(thisStepQuadrant).setAndMatchFrame(taskSpaceEstimates.getSolePosition(thisStepQuadrant));
      onLiftOffTriggered.set(true);
   }

   @Override
   public void onTouchDown(RobotQuadrant thisStepQuadrant)
   {
      onTouchDownTriggered.set(true);
   }

   @Override
   public void onEntry()
   {
      // initialize step stream
      stepStream.onEntry();

      // initialize state
      stepMessageHandler.initialize();
      onLiftOffTriggered.set(false);
      onTouchDownTriggered.set(false);
      accumulatedStepAdjustment.setToZero();

      // update task space estimates
      taskSpaceEstimator.compute(taskSpaceEstimates);

      // initialize feedback controllers
      balanceManager.initialize(taskSpaceEstimates);

      bodyOrientationManager.initialize(taskSpaceEstimates);

      feetManager.registerStepTransitionCallback(this);
      feetManager.reset();
      feetManager.requestFullContact();

      // initialize task space controller
      taskSpaceControllerSettings.initialize();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.getContactForceOptimizationSettings().setContactForceCommandWeights(robotQuadrant, 0.0, 0.0, 0.0);
         taskSpaceControllerSettings.setContactState(robotQuadrant, ContactState.IN_CONTACT);
      }
      taskSpaceController.reset();

      // initialize ground plane
      groundPlaneEstimator.clearContactPoints();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlanePositions.get(robotQuadrant).setAndMatchFrame(taskSpaceEstimates.getSolePosition(robotQuadrant));
         groundPlaneEstimator.addContactPoint(groundPlanePositions.get(robotQuadrant));
      }
      groundPlaneEstimator.compute();

      // compute step plan
      stepMessageHandler.consumeIncomingSteps();
      stepMessageHandler.adjustStepQueue(accumulatedStepAdjustment);

      // compute step adjustment
      computeStepAdjustment();

      balanceManager.initializeDcmSetpoints(taskSpaceEstimates, taskSpaceControllerSettings);
   }

   @Override
   public ControllerEvent process()
   {
      if (stepMessageHandler.isDoneWithStepSequence())
      {
         return ControllerEvent.DONE;
      }
      stepStream.process();

      updateGains();

      // update task space estimates
      taskSpaceEstimator.compute(taskSpaceEstimates);

      // trigger step events
      triggerStepEvents();

      // update ground plane estimate
      groundPlaneEstimator.clearContactPoints();
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         groundPlaneEstimator.addContactPoint(groundPlanePositions.get(robotQuadrant));
      }
      groundPlaneEstimator.compute();

      // update step plan
      stepMessageHandler.consumeIncomingSteps();
      stepMessageHandler.adjustStepQueue(accumulatedStepAdjustment);

      // update step adjustment
      computeStepAdjustment();

      // update desired horizontal com forces
      balanceManager.compute(taskSpaceControllerCommands.getComForce(), taskSpaceEstimates, taskSpaceControllerSettings);

      // update desired body orientation, angular velocity, and torque
      bodyOrientationManager.compute(taskSpaceControllerCommands.getComTorque(), stepStream.getBodyOrientation(), taskSpaceEstimates);

      // update desired contact state and sole forces
      feetManager.compute(taskSpaceControllerCommands.getSoleForce(), taskSpaceEstimates);
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.setContactState(robotQuadrant, feetManager.getContactState(robotQuadrant));
      }

      // update joint setpoints
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);

      // update accumulated step adjustment
      if (onLiftOffTriggered.getBooleanValue())
      {
         onLiftOffTriggered.set(false);
      }
      if (onTouchDownTriggered.getBooleanValue())
      {
         onTouchDownTriggered.set(false);
         accumulatedStepAdjustment.add(instantaneousStepAdjustment);
         accumulatedStepAdjustment.setZ(0);
      }
      return null;
   }

   @Override
   public void onExit()
   {
      // clean up step stream
      stepStream.onExit();

      feetManager.registerStepTransitionCallback(null);
   }

   public void halt()
   {
      stepMessageHandler.halt();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }
}
