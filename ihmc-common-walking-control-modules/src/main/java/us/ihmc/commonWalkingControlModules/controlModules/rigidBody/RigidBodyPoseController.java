package us.ihmc.commonWalkingControlModules.controlModules.rigidBody;

import java.util.Collection;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.FramePose3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.EuclideanTrajectoryControllerCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.JointspaceTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.SE3TrajectoryControllerCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.SO3TrajectoryControllerCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.converter.CommandConversionTools;
import us.ihmc.robotics.controllers.pidGains.PID3DGainsReadOnly;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.yoVariables.parameters.BooleanParameter;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoInteger;

public class RigidBodyPoseController extends RigidBodyTaskspaceControlState
{
   private final SpatialFeedbackControlCommand feedbackControlCommand = new SpatialFeedbackControlCommand();
   private final FeedbackControlCommandList feedbackControlCommandList = new FeedbackControlCommandList();

   private final YoBoolean usingWeightFromMessage;

   private final YoInteger numberOfPointsInQueue;
   private final YoInteger numberOfPointsInGenerator;
   private final YoInteger numberOfPoints;

   private final SO3TrajectoryControllerCommand so3Command = new SO3TrajectoryControllerCommand();
   private final EuclideanTrajectoryControllerCommand euclideanCommand = new EuclideanTrajectoryControllerCommand();

   private final RigidBodyPositionControlHelper positionHelper;
   private final RigidBodyOrientationControlHelper orientationHelper;

   private final YoBoolean hybridModeActive;
   private final RigidBodyJointControlHelper jointControlHelper;

   public RigidBodyPoseController(RigidBody bodyToControl, RigidBody baseBody, RigidBody elevator, Collection<ReferenceFrame> trajectoryFrames,
                                  ReferenceFrame controlFrame, ReferenceFrame baseFrame, YoDouble yoTime, RigidBodyJointControlHelper jointControlHelper,
                                  YoGraphicsListRegistry graphicsListRegistry, YoVariableRegistry parentRegistry)
   {
      super(RigidBodyControlMode.TASKSPACE, bodyToControl.getName(), yoTime, parentRegistry);
      String bodyName = bodyToControl.getName();
      String prefix = bodyName + "Taskspace";

      numberOfPointsInQueue = new YoInteger(prefix + "NumberOfPointsInQueue", registry);
      numberOfPointsInGenerator = new YoInteger(prefix + "NumberOfPointsInGenerator", registry);
      numberOfPoints = new YoInteger(prefix + "NumberOfPoints", registry);

      usingWeightFromMessage = new YoBoolean(prefix + "UsingWeightFromMessage", registry);
      BooleanParameter useBaseFrameForControl = new BooleanParameter(prefix + "UseBaseFrameForControl", registry, false);
      positionHelper = new RigidBodyPositionControlHelper(warningPrefix, bodyToControl, baseBody, elevator, trajectoryFrames, controlFrame, baseFrame,
                                                          useBaseFrameForControl, usingWeightFromMessage, registry, graphicsListRegistry);
      orientationHelper = new RigidBodyOrientationControlHelper(warningPrefix, bodyToControl, baseBody, elevator, trajectoryFrames, controlFrame, baseFrame,
                                                                useBaseFrameForControl, usingWeightFromMessage, registry);

      this.jointControlHelper = jointControlHelper;
      hybridModeActive = new YoBoolean(prefix + "HybridModeActive", registry);

      feedbackControlCommand.set(elevator, bodyToControl);
      feedbackControlCommand.setPrimaryBase(baseBody);
      feedbackControlCommand.setSelectionMatrixToIdentity();
   }

   public void setWeights(Vector3DReadOnly angularWeight, Vector3DReadOnly linearWeight)
   {
      positionHelper.setWeights(angularWeight);
      orientationHelper.setWeights(angularWeight);
   }

   public void setGains(PID3DGainsReadOnly orientationGains, PID3DGainsReadOnly positionGains)
   {
      positionHelper.setGains(positionGains);
      orientationHelper.setGains(orientationGains);
   }

   @Override
   public void doAction(double timeInState)
   {
      double timeInTrajectory = getTimeInTrajectory();
      boolean positionDone = positionHelper.doAction(timeInTrajectory);
      boolean orientationDone = orientationHelper.doAction(timeInTrajectory);
      trajectoryDone.set(positionDone && orientationDone);

      updateCommand();

      numberOfPointsInQueue.set(positionHelper.getNumberOfPointsInQueue());
      numberOfPointsInGenerator.set(positionHelper.getNumberOfPointsInGenerator());
      numberOfPoints.set(numberOfPointsInQueue.getIntegerValue() + numberOfPointsInGenerator.getIntegerValue());

      if (hybridModeActive.getBooleanValue())
      {
         jointControlHelper.doAction(timeInTrajectory);
      }

      updateGraphics();
   }

   private void updateCommand()
   {
      PointFeedbackControlCommand positionCommand = positionHelper.getFeedbackControlCommand();
      OrientationFeedbackControlCommand orientationCommand = orientationHelper.getFeedbackControlCommand();

      // Set weight and selection matrices
      SpatialAccelerationCommand accelerationCommand = feedbackControlCommand.getSpatialAccelerationCommand();
      accelerationCommand.getWeightMatrix().setLinearPart(positionCommand.getSpatialAccelerationCommand().getWeightMatrix().getLinearPart());
      accelerationCommand.getWeightMatrix().setAngularPart(orientationCommand.getSpatialAccelerationCommand().getWeightMatrix().getAngularPart());
      accelerationCommand.getSelectionMatrix().setLinearPart(positionCommand.getSpatialAccelerationCommand().getSelectionMatrix().getLinearPart());
      accelerationCommand.getSelectionMatrix().setAngularPart(orientationCommand.getSpatialAccelerationCommand().getSelectionMatrix().getAngularPart());

      // Set feedback gains
      feedbackControlCommand.getGains().setPositionGains(positionCommand.getGains());
      feedbackControlCommand.getGains().setOrientationGains(orientationCommand.getGains());
      feedbackControlCommand.setGainsFrames(orientationCommand.getAngularGainsFrame(), positionCommand.getLinearGainsFrame());

      // Copy over desired values
      feedbackControlCommand.set(positionCommand.getDesiredPosition(), positionCommand.getDesiredLinearVelocity());
      feedbackControlCommand.setFeedForwardLinearAction(positionCommand.getFeedForwardLinearAction());
      feedbackControlCommand.set(orientationCommand.getDesiredOrientation(), orientationCommand.getDesiredAngularVelocity());
      feedbackControlCommand.setFeedForwardAngularAction(orientationCommand.getFeedForwardAngularAction());

      // Copy from the position command since the orientation does not have a control frame.
      feedbackControlCommand.setControlFrameFixedInEndEffector(positionCommand.getBodyFixedPointToControl(),
                                                               orientationCommand.getBodyFixedOrientationToControl());

      feedbackControlCommand.setControlBaseFrame(positionCommand.getControlBaseFrame());
   }

   @Override
   public void onEntry()
   {
   }

   @Override
   public void onExit()
   {
      positionHelper.onExit();
      orientationHelper.onExit();
      hideGraphics();
      clear();
   }

   @Override
   public void holdCurrent()
   {
      clear();
      setTrajectoryStartTimeToCurrentTime();
      positionHelper.holdCurrent();
      orientationHelper.holdCurrent();
   }

   @Override
   public void holdCurrentDesired()
   {
      clear();
      setTrajectoryStartTimeToCurrentTime();
      positionHelper.holdCurrentDesired();
      orientationHelper.holdCurrentDesired();
   }

   @Override
   public void goToPoseFromCurrent(FramePose3DReadOnly pose, double trajectoryTime)
   {
      clear();
      setTrajectoryStartTimeToCurrentTime();
      positionHelper.goToPositionFromCurrent(pose.getPosition(), trajectoryTime);
      orientationHelper.goToOrientationFromCurrent(pose.getOrientation(), trajectoryTime);
   }

   @Override
   public void goToPose(FramePose3DReadOnly pose, double trajectoryTime)
   {
      clear();
      setTrajectoryStartTimeToCurrentTime();
      positionHelper.goToPosition(pose.getPosition(), trajectoryTime);
      orientationHelper.goToOrientation(pose.getOrientation(), trajectoryTime);
   }

   @Override
   public boolean handleTrajectoryCommand(SO3TrajectoryControllerCommand command)
   {
      positionHelper.disable();
      if (handleCommandInternal(command) && orientationHelper.handleTrajectoryCommand(command))
      {
         usingWeightFromMessage.set(orientationHelper.isMessageWeightValid());
         return true;
      }

      clear();
      return false;
   }

   @Override
   public boolean handleTrajectoryCommand(EuclideanTrajectoryControllerCommand command)
   {
      orientationHelper.disable();
      if (handleCommandInternal(command) && positionHelper.handleTrajectoryCommand(command))
      {
         usingWeightFromMessage.set(positionHelper.isMessageWeightValid());
         return true;
      }

      clear();
      return false;
   }

   @Override
   public boolean handleTrajectoryCommand(SE3TrajectoryControllerCommand command)
   {
      CommandConversionTools.convertToEuclidean(command, euclideanCommand);
      CommandConversionTools.convertToSO3(command, so3Command);

      if (handleCommandInternal(command) && positionHelper.handleTrajectoryCommand(euclideanCommand) && orientationHelper.handleTrajectoryCommand(so3Command))
      {
         usingWeightFromMessage.set(positionHelper.isMessageWeightValid() && orientationHelper.isMessageWeightValid());
         return true;
      }

      clear();
      return false;
   }

   @Override
   public boolean handleHybridTrajectoryCommand(SE3TrajectoryControllerCommand command, JointspaceTrajectoryCommand jointspaceCommand,
                                                double[] initialJointPositions)
   {
      if (handleTrajectoryCommand(command) && jointControlHelper.handleTrajectoryCommand(jointspaceCommand, initialJointPositions))
      {
         hybridModeActive.set(true);
         return true;
      }

      clear();
      return false;
   }

   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      if (hybridModeActive.getBooleanValue())
      {
         feedbackControlCommandList.clear();
         feedbackControlCommandList.addCommand(feedbackControlCommand);
         feedbackControlCommandList.addCommand(jointControlHelper.getJointspaceCommand());
         return feedbackControlCommandList;
      }

      return feedbackControlCommand;
   }

   @Override
   public FeedbackControlCommand<?> createFeedbackControlTemplate()
   {
      feedbackControlCommandList.clear();
      feedbackControlCommandList.addCommand(feedbackControlCommand);
      if (jointControlHelper != null)
      {
         feedbackControlCommandList.addCommand(jointControlHelper.getJointspaceCommand());
      }
      return feedbackControlCommandList;
   }

   @Override
   public double getLastTrajectoryPointTime()
   {
      return positionHelper.getLastTrajectoryPointTime();
   }

   @Override
   public boolean isEmpty()
   {
      return positionHelper.isEmpty();
   }

   private void clear()
   {
      positionHelper.clear();
      orientationHelper.clear();
      numberOfPointsInQueue.set(0);
      numberOfPointsInGenerator.set(0);
      numberOfPoints.set(0);
      hybridModeActive.set(false);
      usingWeightFromMessage.set(false);
      trajectoryDone.set(true);
      resetLastCommandId();
   }
}
