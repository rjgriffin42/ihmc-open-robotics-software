package us.ihmc.aware.controller.force.states;

import us.ihmc.aware.controller.ControllerEvent;
import us.ihmc.aware.controller.QuadrupedController;
import us.ihmc.aware.controller.force.QuadrupedForceControllerToolbox;
import us.ihmc.aware.controller.force.toolbox.QuadrupedSolePositionController;
import us.ihmc.aware.controller.force.toolbox.QuadrupedTaskSpaceController;
import us.ihmc.aware.controller.force.toolbox.QuadrupedTaskSpaceEstimator;
import us.ihmc.aware.model.QuadrupedRuntimeEnvironment;
import us.ihmc.aware.params.DoubleArrayParameter;
import us.ihmc.aware.params.DoubleParameter;
import us.ihmc.aware.params.ParameterFactory;
import us.ihmc.aware.planning.ContactState;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class QuadrupedForceBasedStandReadyController implements QuadrupedController
{
   private final ParameterFactory parameterFactory = new ParameterFactory(getClass());
   private final DoubleParameter jointDampingParameter = parameterFactory.createDouble("jointDamping", 15.0);
   private final DoubleArrayParameter solePositionProportionalGainsParameter = parameterFactory.createDoubleArray("solePositionProportionalGains", 20000, 20000, 20000);
   private final DoubleArrayParameter solePositionDerivativeGainsParameter = parameterFactory.createDoubleArray("solePositionDerivativeGains", 200, 200, 200);
   private final DoubleArrayParameter solePositionIntegralGainsParameter = parameterFactory.createDoubleArray("solePositionIntegralGains", 0, 0, 0);
   private final DoubleParameter solePositionMaxIntegralErrorParameter = parameterFactory.createDouble("solePositionMaxIntegralError", 0);

   private final YoVariableRegistry registry = new YoVariableRegistry(QuadrupedForceBasedStandReadyController.class.getSimpleName());

   // Reference frames
   private final ReferenceFrame bodyFrame;

   // Feedback controller
   private final QuadrupedSolePositionController solePositionController;
   private final QuadrupedSolePositionController.Setpoints solePositionControllerSetpoints;

   // Task space controller
   private final QuadrupedTaskSpaceEstimator.Estimates taskSpaceEstimates;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController.Commands taskSpaceControllerCommands;
   private final QuadrupedTaskSpaceController.Settings taskSpaceControllerSettings;
   private final QuadrupedTaskSpaceController taskSpaceController;

   public QuadrupedForceBasedStandReadyController(QuadrupedRuntimeEnvironment environment, QuadrupedForceControllerToolbox controllerToolbox)
   {
      // Reference frames
      bodyFrame = controllerToolbox.getReferenceFrames().getBodyFrame();

      // Feedback controller
      solePositionController = controllerToolbox.getSolePositionController();
      solePositionControllerSetpoints = new QuadrupedSolePositionController.Setpoints();

      // Task space controller
      taskSpaceEstimates = new QuadrupedTaskSpaceEstimator.Estimates();
      taskSpaceEstimator = controllerToolbox.getTaskSpaceEstimator();
      taskSpaceControllerCommands = new QuadrupedTaskSpaceController.Commands();
      taskSpaceControllerSettings = new QuadrupedTaskSpaceController.Settings();
      taskSpaceController = controllerToolbox.getTaskSpaceController();

      environment.getParentRegistry().addChild(registry);
   }

   @Override
   public void onEntry()
   {
      updateEstimates();

      // Initialize sole position controller
      solePositionControllerSetpoints.initialize(taskSpaceEstimates);
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         solePositionController.getGains(quadrant).setProportionalGains(solePositionProportionalGainsParameter.get());
         solePositionController.getGains(quadrant).setIntegralGains(solePositionIntegralGainsParameter.get(), solePositionMaxIntegralErrorParameter.get());
         solePositionController.getGains(quadrant).setDerivativeGains(solePositionDerivativeGainsParameter.get());
      }
      solePositionController.reset();

      // Initialize task space controller
      taskSpaceControllerSettings.initialize();
      taskSpaceControllerSettings.getVirtualModelControllerSettings().setJointDamping(jointDampingParameter.get());
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         taskSpaceControllerSettings.setContactState(robotQuadrant, ContactState.NO_CONTACT);
      }
      taskSpaceController.reset();

      // Initial sole position setpoints
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         solePositionControllerSetpoints.getSolePosition(robotQuadrant).setIncludingFrame(taskSpaceEstimates.getSolePosition(robotQuadrant));
         solePositionControllerSetpoints.getSolePosition(robotQuadrant).changeFrame(bodyFrame);
      }
   }

   @Override
   public ControllerEvent process()
   {
      updateEstimates();
      updateSetpoints();
      return null;
   }

   @Override
   public void onExit()
   {
   }

   private void updateEstimates()
   {
      taskSpaceEstimator.compute(taskSpaceEstimates);
   }

   private void updateSetpoints()
   {
      solePositionController.compute(taskSpaceControllerCommands.getSoleForce(), solePositionControllerSetpoints, taskSpaceEstimates);
      taskSpaceController.compute(taskSpaceControllerSettings, taskSpaceControllerCommands);
   }
}
