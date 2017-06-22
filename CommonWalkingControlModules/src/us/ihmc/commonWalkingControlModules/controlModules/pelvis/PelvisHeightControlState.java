package us.ihmc.commonWalkingControlModules.controlModules.pelvis;

import java.util.Collection;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.YoSE3OffsetFrame;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.rigidBody.RigidBodyTaskspaceControlState;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisHeightTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.PelvisTrajectoryCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.StopAllTrajectoryCommand;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.controllers.PDController;
import us.ihmc.robotics.controllers.YoPositionPIDGainsInterface;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.FrameVector2d;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.MovingReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.robotics.weightMatrices.WeightMatrix3D;
import us.ihmc.robotics.weightMatrices.WeightMatrix6D;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class PelvisHeightControlState extends PelvisAndCenterOfMassHeightControlState
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   /** We take the spatialFeedback command from the RigidBodyTaskspaceControlState and pack it into a point feedback command and set the selection matrix to Z only**/
   private final PointFeedbackControlCommand pointFeedbackCommand = new PointFeedbackControlCommand();
   private final SelectionMatrix6D linearZSelectionMatrix = new SelectionMatrix6D();
   private final WeightMatrix6D linearZWeightMatrix = new WeightMatrix6D();
   
   /** When we handle the PelvisTrajectoryCommand we pull out the z component and pack it into another PelvisTrajectoryCommand**/
   private final PelvisTrajectoryCommand tempPelvisTrajectoryCommand = new PelvisTrajectoryCommand();

   /** handles the trajectory and the queuing**/
   private final RigidBodyTaskspaceControlState taskspaceControlState;
   
   private final RigidBody pelvis;
   private final MovingReferenceFrame pelvisFrame;
   private final ReferenceFrame baseFrame;
   private final YoDouble defaultHeightAboveAnkleForHome;
   private final FramePose tempPose = new FramePose();
   private final Point3D tempPoint = new Point3D();
   private final RigidBodyTransform controlFrame = new RigidBodyTransform();
   
   private final PDController linearMomentumZPDController;
   private final YoSE3OffsetFrame yoControlFrame;
   
   private final YoDouble currentPelvisHeightInWorld;
   private final YoDouble desiredPelvisHeightInWorld;
   private final YoDouble desiredPelvisVelocityInWorld;
   private final YoDouble currentPelvisVelocityInWorld;
   
   public PelvisHeightControlState(YoPositionPIDGainsInterface gains, HighLevelHumanoidControllerToolbox controllerToolbox, WalkingControllerParameters walkingControllerParameters,
         YoVariableRegistry parentRegistry)
   {
      super(PelvisHeightControlMode.USER);
      FullHumanoidRobotModel fullRobotModel = controllerToolbox.getFullRobotModel();
      CommonHumanoidReferenceFrames referenceFrames = controllerToolbox.getReferenceFrames();
      pelvis = fullRobotModel.getPelvis();
      pelvisFrame = referenceFrames.getPelvisFrame();
      RigidBody elevator = fullRobotModel.getElevator();
      Collection<ReferenceFrame> trajectoryFrames = controllerToolbox.getTrajectoryFrames();
      baseFrame = elevator.getBodyFixedFrame();//referenceFrames.getMidFootZUpGroundFrame();
      YoDouble yoTime = controllerToolbox.getYoTime();
      YoGraphicsListRegistry graphicsListRegistry = controllerToolbox.getYoGraphicsListRegistry();
      
      linearMomentumZPDController = new PDController("pelvisHeightControlState_linearMomentumZPDController", registry);
      linearMomentumZPDController.setProportionalGain(gains.getProportionalGains()[2]);
      linearMomentumZPDController.setDerivativeGain(gains.getDerivativeGains()[2]);
      yoControlFrame =  new YoSE3OffsetFrame(pelvis.getName() + "HeightBodyFixedControlFrame", pelvis.getBodyFixedFrame(), registry);

      taskspaceControlState = new RigidBodyTaskspaceControlState("Height", pelvis, elevator, elevator, trajectoryFrames, pelvisFrame, baseFrame, yoTime, null, graphicsListRegistry, registry);
      taskspaceControlState.setGains(null, gains);
      
      // the nominalHeightAboveAnkle is from the ankle to the pelvis, we need to add the ankle to sole frame to get the proper home height
      double soleToAnkleZHeight = computeSoleToAnkleMeanZHeight(controllerToolbox, fullRobotModel);
      defaultHeightAboveAnkleForHome = new YoDouble(getClass().getSimpleName() + "DefaultHeightAboveAnkleForHome", registry);
      defaultHeightAboveAnkleForHome.set(walkingControllerParameters.nominalHeightAboveAnkle() + soleToAnkleZHeight);
      
      
      currentPelvisHeightInWorld = new YoDouble("currentPelvisHeightInWorld", registry);
      desiredPelvisHeightInWorld = new YoDouble("desiredPelvisHeightInWorld", registry);  
      desiredPelvisVelocityInWorld = new YoDouble("desiredPelvisVelocityInWorld", registry);
      currentPelvisVelocityInWorld = new YoDouble("currentPelvisVelocityInWorld", registry);
      
      parentRegistry.addChild(registry);
   }
   
   /**
    * the nominalHeightAboveAnkle is from the ankle to the pelvis, we need to add the ankle to sole frame to get the proper home height
    * @param controllerToolbox
    * @param fullRobotModel
    * @return the z height between the ankle and the sole frame
    */
   private double computeSoleToAnkleMeanZHeight(HighLevelHumanoidControllerToolbox controllerToolbox, FullHumanoidRobotModel fullRobotModel)
   {
      double zHeight = 0.0;
      for (RobotSide robotSide : RobotSide.values)
      {
         RigidBody foot = controllerToolbox.getFullRobotModel().getFoot(robotSide);
         ReferenceFrame ankleFrame = foot.getParentJoint().getFrameAfterJoint();
         ReferenceFrame soleFrame = fullRobotModel.getSoleFrame(robotSide);
         RigidBodyTransform ankleToSole = new RigidBodyTransform();
         ankleFrame.getTransformToDesiredFrame(ankleToSole, soleFrame);
         zHeight += ankleToSole.getTranslationZ();
      }
      zHeight /= 2.0;
      return zHeight;
   }
   
   /**
    * set the qp weights for the taskspace linear z command
    * @param linearWeight
    */
   public void setWeights(Vector3D linearWeight)
   {
      taskspaceControlState.setWeights(null, linearWeight);
   }

   @Override
   public void doAction()
   {
      taskspaceControlState.doAction();
   }
   
   public boolean handlePelvisHeightTrajectoryCommand(PelvisHeightTrajectoryCommand command, FramePose initialPose)
   {
      if (command.useCustomControlFrame())
      {
         tempPelvisTrajectoryCommand.getControlFramePose(controlFrame);
         taskspaceControlState.setControlFramePose(controlFrame);
      }
      else
      {
         taskspaceControlState.setDefaultControlFrame();
      }
      
      //convert the initial point to be consistent with the control frame
      ReferenceFrame controlFrame = taskspaceControlState.getControlFrame();
      tempPose.setToZero(pelvisFrame);
      tempPose.changeFrame(controlFrame);
      tempPose.getPosition(tempPoint);
      
      initialPose.translate(tempPoint);
      
      return taskspaceControlState.handleEuclideanTrajectoryCommand(command, initialPose);
   }
   
   /**
    * check that the command is valid and queue the trajectory
    * @param command
    * @param initialPose the initial pelvis position
    * @return whether the command passed validation and was queued
    */
   public boolean handlePelvisTrajectoryCommand(PelvisTrajectoryCommand command, FramePose initialPose)
   {
      // We have to remove the orientation and xy components of the command, and adjust the selection matrix;
      // We do this to break up the pelvis control, it reduces the complexity of each manager at the expense of these little hacks.
      tempPelvisTrajectoryCommand.set(command);
      
      //set the selection matrix to z only
      SelectionMatrix6D commandSelectionMatrix = tempPelvisTrajectoryCommand.getSelectionMatrix();
      if(commandSelectionMatrix != null)
      {
         linearZSelectionMatrix.set(commandSelectionMatrix);
         ReferenceFrame linearSelectionFrame = linearZSelectionMatrix.getLinearSelectionFrame();
         if(linearSelectionFrame != null && !linearSelectionFrame.isZupFrame())
         {
            PrintTools.warn("Selection Matrix Linear Frame was not Z up, PelvisTrajectoryCommand can only handle Selection matrix linear components with Z up frames.");
            return false;
         }
      }
      else
      {
         linearZSelectionMatrix.clearLinearSelection();
      }
      
      linearZSelectionMatrix.clearAngularSelection();
      linearZSelectionMatrix.setLinearAxisSelection(false, false, true);
      linearZSelectionMatrix.setSelectionFrame(ReferenceFrame.getWorldFrame());
      tempPelvisTrajectoryCommand.setSelectionMatrix(linearZSelectionMatrix);
      
      //set the weight matrix to z only
      WeightMatrix6D commanedWeightMatrix = tempPelvisTrajectoryCommand.getWeightMatrix();
      if(commanedWeightMatrix != null)
      {
         linearZWeightMatrix.set(commanedWeightMatrix);
         ReferenceFrame linearWeightFrame = linearZWeightMatrix.getLinearWeightFrame();
         if(linearWeightFrame != null && !linearWeightFrame.isZupFrame())
         {
            PrintTools.warn("Weight Matrix Linear Frame was not Z up, PelvisTrajectoryCommand can only handle weight matrix linear components with Z up frames.");
            return false;
         }
      }
      else
      {
         linearZWeightMatrix.clearLinearWeights();
      }
      linearZWeightMatrix.clearAngularWeights();
      WeightMatrix3D weightLinearPart = linearZWeightMatrix.getLinearPart();
      linearZWeightMatrix.setLinearWeights(0.0, 0.0, weightLinearPart.getZAxisWeight());
      linearZWeightMatrix.setWeightFrame(ReferenceFrame.getWorldFrame());
      tempPelvisTrajectoryCommand.setWeightMatrix(linearZWeightMatrix);
      
      if (command.useCustomControlFrame())
      {
         tempPelvisTrajectoryCommand.getControlFramePose(controlFrame);
         taskspaceControlState.setControlFramePose(controlFrame);
      }
      else
      {
         taskspaceControlState.setDefaultControlFrame();
      }
      
      //convert the initial point to be consistent with the control frame
      ReferenceFrame controlFrame = taskspaceControlState.getControlFrame();
      tempPose.setToZero(pelvisFrame);
      tempPose.changeFrame(controlFrame);
      tempPose.getPosition(tempPoint);
      
      initialPose.translate(tempPoint);
      
      if(!taskspaceControlState.handlePoseTrajectoryCommand(tempPelvisTrajectoryCommand, initialPose))
      {
         taskspaceControlState.clear();
         return false;
      }
      return true;
   }
   
   /**
    * returns the control frame which is fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint();
    * @return
    */
   public ReferenceFrame getControlFrame()
   {
      return taskspaceControlState.getControlFrame();
   }
   
   /**
    * Packs positionToPack with the current desired height, The parameter's frame will be set to the trajectory frame
    */
   @Override
   public void getCurrentDesiredHeightOfDefaultControlFrame(FramePoint positionToPack)
   {
      taskspaceControlState.getDesiredPose(tempPose);
      tempPose.getPositionIncludingFrame(positionToPack);
      
      ReferenceFrame controlFrame = taskspaceControlState.getControlFrame();
      tempPose.setToZero(controlFrame);
      tempPose.changeFrame(pelvisFrame);
      tempPose.getPosition(tempPoint);
      
      positionToPack.add(tempPoint);
   }

   @Override
   public void initializeDesiredHeightToCurrent()
   {
      taskspaceControlState.holdCurrent();
   }

   /**
    * sets the desired height to defaultHeightAboveAnkleForHome in baseFrame (MidFootZUpGroundFrame)
    */
   @Override
   public void goHome(double trajectoryTime)
   {
      tempPose.setToZero(baseFrame);
      tempPose.setZ(defaultHeightAboveAnkleForHome.getDoubleValue());
      taskspaceControlState.setDefaultControlFrame();
      taskspaceControlState.goToPoseFromCurrent(tempPose, trajectoryTime);
   }

   /**
    * If the command says to stop then set the desired to the actual
    */
   @Override
   public void handleStopAllTrajectoryCommand(StopAllTrajectoryCommand command)
   {
      if(command.isStopAllTrajectory())
      {
         initializeDesiredHeightToCurrent();
      }
   }

   private final FramePoint controlPosition = new FramePoint();
   private final FrameOrientation controlOrientation = new FrameOrientation();
   private final FramePoint desiredPosition = new FramePoint();
   private final FrameVector desiredLinearVelocity = new FrameVector();
   private final FrameVector feedForwardLinearAcceleration = new FrameVector();
   private final FrameVector currentLinearVelocity = new FrameVector();
   private final Twist twist = new Twist();

   /**
    * returns the point feedback command for the z height of the pelvis
    */
   @Override
   public FeedbackControlCommand<?> getFeedbackControlCommand()
   {
      //We have to do some nasty copying, because the taskspaceControlState returns a spatial feedback command, but the controller core doesn't like
      //when you send two overlapping commands (pelvis orientation uses orientation feedback comand)
      SpatialFeedbackControlCommand spatialFeedbackControlCommand = taskspaceControlState.getSpatialFeedbackControlCommand();

      SpatialAccelerationCommand spcatialAccelerationCommand = spatialFeedbackControlCommand.getSpatialAccelerationCommand();
      pointFeedbackCommand.getSpatialAccelerationCommand().set(spcatialAccelerationCommand);
      pointFeedbackCommand.setControlBaseFrame(spatialFeedbackControlCommand.getControlBaseFrame());
      pointFeedbackCommand.set(spatialFeedbackControlCommand.getBase(), spatialFeedbackControlCommand.getEndEffector());
      spatialFeedbackControlCommand.getIncludingFrame(desiredPosition, desiredLinearVelocity, feedForwardLinearAcceleration);
      pointFeedbackCommand.set(desiredPosition, desiredLinearVelocity, feedForwardLinearAcceleration);
      pointFeedbackCommand.setControlBaseFrame(spatialFeedbackControlCommand.getControlBaseFrame());
      pointFeedbackCommand.setGains(spatialFeedbackControlCommand.getGains().getPositionGains());
      spatialFeedbackControlCommand.getControlFramePoseIncludingFrame(controlPosition, controlOrientation);
      pointFeedbackCommand.setBodyFixedPointToControl(controlPosition);
      
      return pointFeedbackCommand;
//      return null;
   }

   /**
    * Returns 0.0, we don't compute the acceleration here, we send the command to a feedback controller to do it for us
    */
   @Override
   public double computeDesiredCoMHeightAcceleration(FrameVector2d desiredICPVelocity, boolean isInDoubleSupport, double omega0, boolean isRecoveringFromPush,
         FeetManager feetManager)
   {
      SpatialFeedbackControlCommand spatialFeedbackControlCommand = taskspaceControlState.getSpatialFeedbackControlCommand();
      spatialFeedbackControlCommand.getIncludingFrame(desiredPosition, desiredLinearVelocity, feedForwardLinearAcceleration);
      spatialFeedbackControlCommand.getControlFramePoseIncludingFrame(controlPosition, controlOrientation);
      controlPosition.changeFrame(pelvis.getBodyFixedFrame());
      
      yoControlFrame.setOffsetToParentToTranslationOnly(controlPosition);
      yoControlFrame.getTwistRelativeToOther(baseFrame, twist);
      twist.getLinearPart(currentLinearVelocity);
      currentLinearVelocity.changeFrame(ReferenceFrame.getWorldFrame());
      
      controlPosition.changeFrame(ReferenceFrame.getWorldFrame());
      
      currentPelvisHeightInWorld.set(controlPosition.getZ());  
      desiredPelvisHeightInWorld.set(desiredPosition.getZ());  
      desiredPelvisVelocityInWorld.set(desiredLinearVelocity.getZ());
      currentPelvisVelocityInWorld.set(currentLinearVelocity.getZ());
   
      double acceleration = linearMomentumZPDController.compute(controlPosition.getZ(), desiredPosition.getZ(), currentLinearVelocity.getZ(), desiredLinearVelocity.getZ());
      return acceleration;
   }
}
