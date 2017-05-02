package us.ihmc.commonWalkingControlModules.controlModules.pelvis;

import java.util.Collection;

import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyTaskspaceControlState;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisOrientationTrajectoryCommand;
import us.ihmc.robotics.controllers.YoOrientationPIDGainsInterface;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;

public class UserPelvisOrientationManager extends PelvisOrientationControlState
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final FramePose tempPose = new FramePose();
   private final RigidBodyTaskspaceControlState taskspaceControlState;
   private final FramePose homePose = new FramePose();
   private final ReferenceFrame baseFrame;

   private final OrientationFeedbackControlCommand orientationFeedbackControlCommand = new OrientationFeedbackControlCommand();
   private final FrameOrientation desiredOrientation = new FrameOrientation();
   private final FrameVector desiredAngularVelocity = new FrameVector();
   private final FrameVector feedForwardAngularAcceleration = new FrameVector();

   public UserPelvisOrientationManager(YoOrientationPIDGainsInterface gains, HighLevelHumanoidControllerToolbox controllerToolbox, YoVariableRegistry parentRegistry)
   {
      super(PelvisOrientationControlMode.USER);

      RigidBody pelvis = controllerToolbox.getFullRobotModel().getPelvis();
      RigidBody elevator = controllerToolbox.getFullRobotModel().getElevator();
      Collection<ReferenceFrame> trajectoryFrames = controllerToolbox.getTrajectoryFrames();
      ReferenceFrame pelvisFixedFrame = pelvis.getBodyFixedFrame();
      baseFrame = controllerToolbox.getReferenceFrames().getMidFootZUpGroundFrame();
      DoubleYoVariable yoTime = controllerToolbox.getYoTime();
      YoGraphicsListRegistry graphicsListRegistry = controllerToolbox.getYoGraphicsListRegistry();

      taskspaceControlState = new RigidBodyTaskspaceControlState(pelvis, elevator, elevator, trajectoryFrames, pelvisFixedFrame, baseFrame, yoTime, graphicsListRegistry, registry);
      taskspaceControlState.setGains(gains, null);

      orientationFeedbackControlCommand.set(elevator, pelvis);
      orientationFeedbackControlCommand.setPrimaryBase(elevator);

      parentRegistry.addChild(registry);
   }

   public void setWeights(Vector3D angularWeight)
   {
      taskspaceControlState.setWeights(angularWeight, null);
   }

   public void goHome(double trajectoryTime, FrameOrientation initialOrientation)
   {
      tempPose.setToNaN(initialOrientation.getReferenceFrame());
      tempPose.setOrientation(initialOrientation);
      homePose.setToZero(baseFrame);
      taskspaceControlState.goToPose(homePose, tempPose, trajectoryTime);
   }

   @Override
   public void doAction()
   {
      taskspaceControlState.doAction();
   }

   public void handlePelvisOrientationTrajectoryCommands(PelvisOrientationTrajectoryCommand command, FrameOrientation initialOrientation)
   {
      tempPose.setToNaN(initialOrientation.getReferenceFrame());
      tempPose.setOrientation(initialOrientation);
      taskspaceControlState.handleOrientationTrajectoryCommand(command, tempPose);
   }

   public ReferenceFrame getControlFrame()
   {
      return taskspaceControlState.getControlFrame();
   }

   @Override
   public void getCurrentDesiredOrientation(FrameOrientation desiredOrientation)
   {
      taskspaceControlState.getDesiredPose(tempPose);
      tempPose.getOrientationIncludingFrame(desiredOrientation);
   }

   @Override
   public OrientationFeedbackControlCommand getFeedbackControlCommand()
   {
      SpatialFeedbackControlCommand spatialFeedbackControlCommand = taskspaceControlState.getSpatialFeedbackControlCommand();
      orientationFeedbackControlCommand.setGains(spatialFeedbackControlCommand.getGains().getOrientationGains());
      orientationFeedbackControlCommand.getSpatialAccelerationCommand().set(spatialFeedbackControlCommand.getSpatialAccelerationCommand());
      spatialFeedbackControlCommand.getIncludingFrame(desiredOrientation, desiredAngularVelocity, feedForwardAngularAcceleration);
      orientationFeedbackControlCommand.set(desiredOrientation, desiredAngularVelocity, feedForwardAngularAcceleration);
      return orientationFeedbackControlCommand;
   }
}
