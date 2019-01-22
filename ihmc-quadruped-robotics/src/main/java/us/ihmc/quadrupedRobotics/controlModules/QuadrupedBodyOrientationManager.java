package us.ihmc.quadrupedRobotics.controlModules;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommand;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisOrientationTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.QuadrupedBodyOrientationCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.QuadrupedBodyTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.SO3TrajectoryControllerCommand;
import us.ihmc.mecano.frames.MovingReferenceFrame;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerToolbox;
import us.ihmc.quadrupedRobotics.estimator.GroundPlaneEstimator;
import us.ihmc.robotics.controllers.pidGains.GainCoupling;
import us.ihmc.robotics.controllers.pidGains.implementations.DefaultPID3DGains;
import us.ihmc.robotics.controllers.pidGains.implementations.ParameterizedPID3DGains;
import us.ihmc.robotics.dataStructures.parameters.ParameterVector3D;
import us.ihmc.robotics.math.trajectories.waypoints.generators.MultipleWaypointsOrientationTrajectoryGenerator;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.sensorProcessing.simulatedSensors.Vector3DProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoFrameVector3D;
import us.ihmc.yoVariables.variable.YoFrameYawPitchRoll;

public class QuadrupedBodyOrientationManager
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final ParameterizedPID3DGains bodyOrientationGainsParameter;

   private final YoBoolean useAbsoluteBodyOrientationTrajectory = new YoBoolean("useBaseBodyOrientationTrajectory", registry);
   private final MultipleWaypointsOrientationTrajectoryGenerator offsetBodyOrientationTrajectory;
   private final MultipleWaypointsOrientationTrajectoryGenerator absoluteBodyOrientationTrajectory;

   private final GroundPlaneEstimator groundPlaneEstimator;

   private final MovingReferenceFrame bodyFrame;
   private final YoFrameYawPitchRoll yoBodyOrientationSetpoint;
   private final YoFrameVector3D yoBodyAngularVelocitySetpoint;
   private final YoFrameVector3D yoBodyAngularAccelerationSetpoint;

   private final FrameQuaternion desiredBodyOrientation;
   private final FrameQuaternion desiredBodyOrientationOffset;
   private final FrameVector3D desiredBodyAngularVelocity;
   private final FrameVector3D desiredBodyAngularAcceleration;

   private final FrameQuaternion desiredAbsoluteYawOrientation = new FrameQuaternion();
   private final FrameVector3D desiredAbsoluteYawVelocity = new FrameVector3D();
   private final FrameVector3D desiredAbsoluteYawAcceleration = new FrameVector3D();

   private ReferenceFrame desiredFrameToHold;

   private final YoDouble robotTimestamp;

   private final OrientationFeedbackControlCommand feedbackControlCommand = new OrientationFeedbackControlCommand();
   private final Vector3DReadOnly bodyAngularWeight;

   public QuadrupedBodyOrientationManager(QuadrupedControllerToolbox controllerToolbox, YoVariableRegistry parentRegistry)
   {
      bodyFrame = controllerToolbox.getReferenceFrames().getBodyFrame();
      robotTimestamp = controllerToolbox.getRuntimeEnvironment().getRobotTimestamp();

      DefaultPID3DGains bodyOrientationDefaultGains = new DefaultPID3DGains();
      bodyOrientationDefaultGains.setProportionalGains(1000.0, 1000.0, 1000.0);
      bodyOrientationDefaultGains.setDerivativeGains(250.0, 250.0, 250.0);
      bodyOrientationDefaultGains.setIntegralGains(0.0, 0.0, 0.0, 0.0);
      bodyOrientationGainsParameter = new ParameterizedPID3DGains("_bodyOrientation", GainCoupling.NONE, true, bodyOrientationDefaultGains, registry);

      bodyAngularWeight = new ParameterVector3D("bodyAngularWeight", new Vector3D(2.5, 2.5, 1.0), registry);

      ReferenceFrame bodyFrame = controllerToolbox.getReferenceFrames().getBodyFrame();
      yoBodyOrientationSetpoint = new YoFrameYawPitchRoll("bodyOrientationSetpoint", worldFrame, registry);
      yoBodyAngularVelocitySetpoint = new YoFrameVector3D("bodyAngularVelocitySetpoint", worldFrame, registry);
      yoBodyAngularAccelerationSetpoint = new YoFrameVector3D("bodyAngularAccelerationSetpoint", worldFrame, registry);

      feedbackControlCommand.setGains(bodyOrientationDefaultGains);
      feedbackControlCommand.set(controllerToolbox.getFullRobotModel().getElevator(), controllerToolbox.getFullRobotModel().getBody());
      feedbackControlCommand.setGainsFrame(bodyFrame);

      groundPlaneEstimator = controllerToolbox.getGroundPlaneEstimator();

      offsetBodyOrientationTrajectory = new MultipleWaypointsOrientationTrajectoryGenerator("offsetBodyOrientationTrajectory", worldFrame, registry);
      absoluteBodyOrientationTrajectory = new MultipleWaypointsOrientationTrajectoryGenerator("baseBodyOrientationTrajectory", worldFrame, registry);

      desiredBodyOrientation = new FrameQuaternion();
      desiredBodyOrientationOffset = new FrameQuaternion();
      desiredBodyAngularVelocity = new FrameVector3D();
      desiredBodyAngularAcceleration = new FrameVector3D();


      parentRegistry.addChild(registry);
   }

   public void initialize()
   {
      ReferenceFrame trajectoryFrame = offsetBodyOrientationTrajectory.getReferenceFrame();
      desiredBodyOrientation.setToZero(trajectoryFrame);
      desiredBodyAngularVelocity.setToZero(trajectoryFrame);
      desiredBodyAngularAcceleration.setToZero(trajectoryFrame);

      double currentTime = robotTimestamp.getDoubleValue();

      offsetBodyOrientationTrajectory.clear();
      offsetBodyOrientationTrajectory.appendWaypoint(currentTime, desiredBodyOrientation, desiredBodyAngularVelocity);
      offsetBodyOrientationTrajectory.initialize();

      useAbsoluteBodyOrientationTrajectory.set(false);
      absoluteBodyOrientationTrajectory.clear();
   }

   public void handleBodyOrientationCommand(QuadrupedBodyOrientationCommand command)
   {
      double currentTime = robotTimestamp.getDoubleValue();
      double timeShift = command.isExpressedInAbsoluteTime() ? 0.0 : currentTime;
      SO3TrajectoryControllerCommand so3Trajectory = command.getSO3Trajectory();
      so3Trajectory.getTrajectoryPointList().addTimeOffset(timeShift);

      if(command.isAnOffsetOrientation())
      {
         handleOffsetOrientationCommand(so3Trajectory);
         useAbsoluteBodyOrientationTrajectory.set(false);
      }
      else
      {
         handleAbsoluteOrientationCommand(so3Trajectory);
         useAbsoluteBodyOrientationTrajectory.set(true);
      }
   }

   private final QuadrupedBodyOrientationCommand tempBodyOrientationTrajectoryCommand = new QuadrupedBodyOrientationCommand();

   public void handleBodyTrajectoryCommand(QuadrupedBodyTrajectoryCommand command)
   {
      SelectionMatrix3D angularSelectionMatrix = command.getSE3Trajectory().getSelectionMatrix().getAngularPart();

      if (angularSelectionMatrix.isXSelected() || angularSelectionMatrix.isYSelected() || angularSelectionMatrix.isZSelected())
      { // At least one axis is to be controlled, process the command
         tempBodyOrientationTrajectoryCommand.set(command);
         handleBodyOrientationCommand(tempBodyOrientationTrajectoryCommand);
      }
   }

   private void handleOffsetOrientationCommand(SO3TrajectoryControllerCommand so3Trajectory)
   {
      double currentTime = robotTimestamp.getDoubleValue();

      if (so3Trajectory.getTrajectoryPoint(0).getTime() > 1.0e-5 + currentTime)
      {
         offsetBodyOrientationTrajectory.getOrientation(desiredBodyOrientation);
         offsetBodyOrientationTrajectory.getAngularVelocity(desiredBodyAngularVelocity);
         desiredBodyOrientation.changeFrame(worldFrame);
         desiredBodyAngularVelocity.changeFrame(worldFrame);

         offsetBodyOrientationTrajectory.clear();
         offsetBodyOrientationTrajectory.appendWaypoint(currentTime, desiredBodyOrientation, desiredBodyAngularVelocity);
      }
      else
      {
         offsetBodyOrientationTrajectory.clear();
      }

      offsetBodyOrientationTrajectory.appendWaypoints(so3Trajectory.getTrajectoryPointList());
      offsetBodyOrientationTrajectory.initialize();
   }

   private void handleAbsoluteOrientationCommand(SO3TrajectoryControllerCommand so3Trajectory)
   {
      double currentTime = robotTimestamp.getDoubleValue();

      if (so3Trajectory.getTrajectoryPoint(0).getTime() > 1.0e-5 + currentTime)
      {
         // add point from current trajectory
         if (! absoluteBodyOrientationTrajectory.isEmpty() &&  ! absoluteBodyOrientationTrajectory.isDone())
         {
            absoluteBodyOrientationTrajectory.compute(robotTimestamp.getDoubleValue());
            absoluteBodyOrientationTrajectory.getAngularData(desiredAbsoluteYawOrientation, desiredAbsoluteYawVelocity, desiredAbsoluteYawAcceleration);
         }
         // trajectory hasn't been commanded, add current orientation as start point
         else
         {
            desiredAbsoluteYawOrientation.setIncludingFrame(yoBodyOrientationSetpoint.getFrameOrientation());
            desiredAbsoluteYawVelocity.setIncludingFrame(yoBodyAngularVelocitySetpoint);

            desiredAbsoluteYawOrientation.changeFrame(worldFrame);
            desiredAbsoluteYawVelocity.changeFrame(worldFrame);
         }

         absoluteBodyOrientationTrajectory.clear();
         absoluteBodyOrientationTrajectory.appendWaypoint(currentTime, desiredAbsoluteYawOrientation, desiredAbsoluteYawVelocity);
      }
      else
      {
         absoluteBodyOrientationTrajectory.clear();
      }

      absoluteBodyOrientationTrajectory.appendWaypoints(so3Trajectory.getTrajectoryPointList());
      absoluteBodyOrientationTrajectory.initialize();
   }

   public void setDesiredFrameToHoldPosition(ReferenceFrame desiredFrameToHold)
   {
      this.desiredFrameToHold = desiredFrameToHold;
   }

   public void compute()
   {
      offsetBodyOrientationTrajectory.compute(robotTimestamp.getDoubleValue());

      desiredBodyOrientation.setToZero(desiredFrameToHold);
      desiredBodyOrientation.changeFrame(worldFrame);

      offsetBodyOrientationTrajectory.getAngularData(desiredBodyOrientationOffset, desiredBodyAngularVelocity, desiredBodyAngularAcceleration);
      desiredBodyOrientation.append(desiredBodyOrientationOffset);
      handleAbsoluteYawOrientationCommand();

      double bodyOrientationYaw = desiredBodyOrientation.getYaw();
      double bodyOrientationPitch = desiredBodyOrientation.getPitch() + groundPlaneEstimator.getPitch(bodyOrientationYaw);
      double bodyOrientationRoll = desiredBodyOrientation.getRoll();
      desiredBodyOrientation.setYawPitchRoll(bodyOrientationYaw, bodyOrientationPitch, bodyOrientationRoll);

      feedbackControlCommand.setGains(bodyOrientationGainsParameter);
      feedbackControlCommand.set(desiredBodyOrientation, desiredBodyAngularVelocity);
      feedbackControlCommand.setFeedForwardAction(desiredBodyAngularAcceleration);
      feedbackControlCommand.setWeightsForSolver(bodyAngularWeight);

      yoBodyOrientationSetpoint.set(desiredBodyOrientation);
      yoBodyAngularVelocitySetpoint.set(desiredBodyAngularVelocity);
      yoBodyAngularAccelerationSetpoint.set(desiredBodyAngularAcceleration);
   }

   private void handleAbsoluteYawOrientationCommand()
   {
      if(!useAbsoluteBodyOrientationTrajectory.getBooleanValue())
      {
         return;
      }
      else if (absoluteBodyOrientationTrajectory.isDone())
      {
         useAbsoluteBodyOrientationTrajectory.set(false);
         absoluteBodyOrientationTrajectory.clear();
         return;
      }
      else
      {
         desiredBodyOrientation.changeFrame(worldFrame);
         absoluteBodyOrientationTrajectory.compute(robotTimestamp.getDoubleValue());
         absoluteBodyOrientationTrajectory.getAngularData(desiredAbsoluteYawOrientation, desiredAbsoluteYawVelocity, desiredAbsoluteYawAcceleration);

         double originalBodyYaw = desiredBodyOrientation.getYaw();
         double newBodyYaw = desiredAbsoluteYawOrientation.getYaw();
         double bodyPitchCorrection = groundPlaneEstimator.getPitch(newBodyYaw) - groundPlaneEstimator.getPitch(originalBodyYaw);

         double bodyOrientationPitch = desiredBodyOrientation.getPitch() + bodyPitchCorrection;
         double bodyOrientationRoll = desiredBodyOrientation.getRoll();

         desiredBodyOrientation.setYawPitchRoll(newBodyYaw, bodyOrientationPitch, bodyOrientationRoll);
         desiredBodyAngularVelocity.setZ(desiredAbsoluteYawVelocity.getZ());
         desiredBodyAngularAcceleration.setZ(desiredAbsoluteYawAcceleration.getZ());
      }
   }

   public FeedbackControlCommand<?> createFeedbackControlTemplate()
   {
      return getFeedbackControlCommand();
   }

   public OrientationFeedbackControlCommand getFeedbackControlCommand()
   {
      return feedbackControlCommand;
   }

   public VirtualModelControlCommand<?> getVirtualModelControlCommand()
   {
      return null;
   }

   public InverseDynamicsCommand<?> getInverseDynamicsCommand()
   {
      return null;
   }
}
