package us.ihmc.aware.controller.force;

import us.ihmc.aware.controller.force.toolbox.*;
import us.ihmc.aware.mechanics.virtualModelControl.QuadrupedJointLimits;
import us.ihmc.aware.model.QuadrupedPhysicalProperties;
import us.ihmc.aware.model.QuadrupedRuntimeEnvironment;
import us.ihmc.aware.estimator.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;

public class QuadrupedForceControllerToolbox
{
   private final QuadrupedJointLimits jointLimits;
   private final QuadrupedReferenceFrames referenceFrames;
   private final QuadrupedTaskSpaceEstimator taskSpaceEstimator;
   private final QuadrupedTaskSpaceController taskSpaceController;
   private final DivergentComponentOfMotionController dcmPositionController;
   private final QuadrupedBodyOrientationController bodyOrientationController;
   private final QuadrupedSolePositionController solePositionController;
   private final QuadrupedComPositionController comPositionController;
   private final QuadrupedTimedStepController timedStepController;

   public QuadrupedForceControllerToolbox(QuadrupedRuntimeEnvironment runtimeEnvironment, QuadrupedPhysicalProperties physicalProperties, YoVariableRegistry registry)
   {
      double gravity = 9.81;
      double mass = runtimeEnvironment.getFullRobotModel().getTotalMass();

      // create controllers and estimators
      jointLimits = new QuadrupedJointLimits();
      referenceFrames = new QuadrupedReferenceFrames(runtimeEnvironment.getFullRobotModel(), physicalProperties);
      taskSpaceEstimator = new QuadrupedTaskSpaceEstimator(runtimeEnvironment.getFullRobotModel(), referenceFrames, registry);
      taskSpaceController = new QuadrupedTaskSpaceController(runtimeEnvironment.getFullRobotModel(), referenceFrames, jointLimits, runtimeEnvironment.getControlDT(), registry);
      comPositionController = new QuadrupedComPositionController(referenceFrames.getCenterOfMassZUpFrame(), runtimeEnvironment.getControlDT(), registry);
      dcmPositionController = new DivergentComponentOfMotionController(referenceFrames.getCenterOfMassZUpFrame(), runtimeEnvironment.getControlDT(), mass, gravity, 1.0, registry);
      bodyOrientationController = new QuadrupedBodyOrientationController(referenceFrames.getBodyFrame(), runtimeEnvironment.getControlDT(), registry);
      solePositionController = new QuadrupedSolePositionController(referenceFrames.getFootReferenceFrames(), runtimeEnvironment.getControlDT(), registry);
      timedStepController = new QuadrupedTimedStepController(solePositionController, runtimeEnvironment.getRobotTimestamp(), registry);

      // register controller graphics
      taskSpaceController.registerGraphics(runtimeEnvironment.getGraphicsListRegistry());
      dcmPositionController.registerGraphics(runtimeEnvironment.getGraphicsListRegistry());
   }

   public QuadrupedJointLimits getJointLimits()
   {
      return jointLimits;
   }

   public QuadrupedReferenceFrames getReferenceFrames()
   {
      return referenceFrames;
   }

   public QuadrupedTaskSpaceEstimator getTaskSpaceEstimator()
   {
      return taskSpaceEstimator;
   }

   public QuadrupedTaskSpaceController getTaskSpaceController()
   {
      return taskSpaceController;
   }

   public QuadrupedComPositionController getComPositionController()
   {
      return comPositionController;
   }

   public DivergentComponentOfMotionController getDcmPositionController()
   {
      return dcmPositionController;
   }

   public QuadrupedBodyOrientationController getBodyOrientationController()
   {
      return bodyOrientationController;
   }

   public QuadrupedSolePositionController getSolePositionController()
   {
      return solePositionController;
   }

   public QuadrupedTimedStepController getTimedStepController()
   {
      return timedStepController;
   }
}
