package us.ihmc.quadrupedRobotics.controller.states;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerToolbox;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedJointControlParameters;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.robotics.controllers.pidGains.PDGainsReadOnly;
import us.ihmc.robotics.partNames.QuadrupedJointName;
import us.ihmc.quadrupedRobotics.controller.ControllerEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.model.QuadrupedRuntimeEnvironment;
import us.ihmc.quadrupedRobotics.model.QuadrupedInitialPositionParameters;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.trajectories.MinimumJerkTrajectory;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutput;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputList;
import us.ihmc.yoVariables.parameters.DoubleParameter;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;

/**
 * A controller that will track the minimum jerk trajectory to bring joints to a preparatory pose.
 */
public class QuadrupedStandPrepController implements QuadrupedController
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final DoubleParameter trajectoryTimeParameter = new DoubleParameter("trajectoryTime", registry, 1.0);
   private final QuadrupedInitialPositionParameters initialPositionParameters;

   private final FullQuadrupedRobotModel fullRobotModel;
   private final double dt;

   private final List<MinimumJerkTrajectory> trajectories;
   private final JointDesiredOutputList jointDesiredOutputList;

   private final QuadrupedControllerToolbox controllerToolbox;

   private final QuadrupedJointControlParameters jointControlParameters;

   /**
    * The time from the beginning of the current preparation trajectory in seconds.
    */
   private double timeInTrajectory = 0.0;

   public QuadrupedStandPrepController(QuadrupedControllerToolbox controllerToolbox, QuadrupedInitialPositionParameters initialPositionParameters,
                                       YoVariableRegistry parentRegistry)
   {
      QuadrupedRuntimeEnvironment environment = controllerToolbox.getRuntimeEnvironment();
      this.controllerToolbox = controllerToolbox;
      this.jointControlParameters = controllerToolbox.getJointControlParameters();
      this.initialPositionParameters = initialPositionParameters;
      this.fullRobotModel = environment.getFullRobotModel();
      this.jointDesiredOutputList = environment.getJointDesiredOutputList();
      this.dt = environment.getControlDT();

      this.trajectories = new ArrayList<>(fullRobotModel.getOneDoFJoints().length);
      for (int i = 0; i < fullRobotModel.getOneDoFJoints().length; i++)
      {
         trajectories.add(new MinimumJerkTrajectory());
      }


      parentRegistry.addChild(registry);
   }

   @Override
   public void onEntry()
   {
      for (int i = 0; i < fullRobotModel.getOneDoFJoints().length; i++)
      {
         OneDoFJoint joint = fullRobotModel.getOneDoFJoints()[i];
         JointDesiredOutput jointDesiredOutput = jointDesiredOutputList.getJointDesiredOutput(joint);

         QuadrupedJointName jointId = fullRobotModel.getNameForOneDoFJoint(joint);
         double desiredPosition = initialPositionParameters.getInitialJointPosition(jointId);

         // Start the trajectory from the current pos/vel/acc.
         MinimumJerkTrajectory trajectory = trajectories.get(i);

         double initialPosition = jointDesiredOutput.hasDesiredPosition() ? jointDesiredOutput.getDesiredPosition() : joint.getQ();
         double initialVelocity = jointDesiredOutput.hasDesiredVelocity() ? jointDesiredOutput.getDesiredVelocity() : joint.getQd();
         double initialAcceleration = 0.0;

         trajectory.setMoveParameters(initialPosition, initialVelocity, initialAcceleration, desiredPosition, 0.0, 0.0, trajectoryTimeParameter.getValue());

         jointDesiredOutput.clear();
         jointDesiredOutput.setControlMode(jointControlParameters.getStandPrepJointMode());
      }

      // This is a new trajectory. We start at time 0.
      timeInTrajectory = 0.0;
   }

   @Override
   public void doAction(double timeInState)
   {
      fullRobotModel.updateFrames();

      for (int i = 0; i < fullRobotModel.getOneDoFJoints().length; i++)
      {
         OneDoFJoint joint = fullRobotModel.getOneDoFJoints()[i];
         MinimumJerkTrajectory trajectory = trajectories.get(i);

         trajectory.computeTrajectory(timeInTrajectory);
         JointDesiredOutput jointDesiredOutput = jointDesiredOutputList.getJointDesiredOutput(joint);
         jointDesiredOutput.setDesiredPosition(trajectory.getPosition());
         jointDesiredOutput.setDesiredVelocity(trajectory.getVelocity());
         jointDesiredOutput.setDesiredTorque(0.0);

         jointDesiredOutput.setControlMode(jointControlParameters.getStandPrepJointMode());
         PDGainsReadOnly pdGainsReadOnly = jointControlParameters.getStandPrepJointGains();

         jointDesiredOutput.setStiffness(pdGainsReadOnly.getKp());
         jointDesiredOutput.setDamping(pdGainsReadOnly.getKd());
         jointDesiredOutput.setMaxPositionError(pdGainsReadOnly.getMaximumFeedback());
         jointDesiredOutput.setMaxVelocityError(pdGainsReadOnly.getMaximumFeedbackRate());
      }

      timeInTrajectory += dt;
   }

   @Override
   public ControllerEvent fireEvent(double timeInState)
   {
      return isMotionExpired() ? ControllerEvent.DONE : null;
   }

   @Override
   public void onExit()
   {
   }

   private boolean isMotionExpired()
   {
      return timeInTrajectory > trajectoryTimeParameter.getValue();
   }
}

