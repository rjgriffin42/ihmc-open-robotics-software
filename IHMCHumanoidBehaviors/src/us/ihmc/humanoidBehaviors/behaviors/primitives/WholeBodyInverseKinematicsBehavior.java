package us.ihmc.humanoidBehaviors.behaviors.primitives;

import us.ihmc.communication.packets.KinematicsToolboxOutputStatus;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.ToolboxStateMessage;
import us.ihmc.communication.packets.ToolboxStateMessage.ToolboxState;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.TrackingWeightsCommand.BodyWeights;
import us.ihmc.humanoidRobotics.communication.packets.KinematicsToolboxOutputConverter;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisHeightTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisOrientationTrajectoryMessage;
import us.ihmc.humanoidRobotics.communication.packets.walking.TrackingWeightsMessage;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;

public class WholeBodyInverseKinematicsBehavior extends AbstractBehavior
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final DoubleYoVariable solutionQualityThreshold;
   private final DoubleYoVariable currentSolutionQuality;
   private final BooleanYoVariable isPaused;
   private final BooleanYoVariable isStopped;
   private final BooleanYoVariable isDone;
   private final BooleanYoVariable hasSolverFailed;
   private final BooleanYoVariable hasSentMessageToController;

   private final SideDependentList<SelectionMatrix6D> handSelectionMatrices = new SideDependentList<>(new SelectionMatrix6D(), new SelectionMatrix6D());
   private final SelectionMatrix3D chestSelectionMatrix = new SelectionMatrix3D();
   private final SelectionMatrix3D pelvisSelectionMatrix = new SelectionMatrix3D();
   private final SideDependentList<YoFramePoint> yoDesiredHandPositions = new SideDependentList<>();
   private final SideDependentList<YoFrameQuaternion> yoDesiredHandOrientations = new SideDependentList<>();
   private final YoFrameQuaternion yoDesiredChestOrientation;
   private final YoFrameQuaternion yoDesiredPelvisOrientation;
   private final DoubleYoVariable yoDesiredPelvisHeight;
   private final DoubleYoVariable trajectoryTime;

   private final KinematicsToolboxOutputConverter outputConverter;
   private final FullHumanoidRobotModel fullRobotModel;
   private ChestTrajectoryMessage chestTrajectoryMessage;
   private PelvisOrientationTrajectoryMessage pelvisOrientationTrajectoryMessage;
   private PelvisHeightTrajectoryMessage pelvisHeightTrajectoryMessage;
   private SideDependentList<HandTrajectoryMessage> handTrajectoryMessage = new SideDependentList<>();
   private TrackingWeightsMessage trackingWeightsMessage;

   private final ConcurrentListeningQueue<KinematicsToolboxOutputStatus> kinematicsToolboxOutputQueue = new ConcurrentListeningQueue<>(40);
   private KinematicsToolboxOutputStatus solutionSentToController = null;
   
   private final ReferenceFrame pelvisZUpFrame;
   private final ReferenceFrame chestFrame;

   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable timeSolutionSentToController;

   public WholeBodyInverseKinematicsBehavior(FullHumanoidRobotModelFactory fullRobotModelFactory, DoubleYoVariable yoTime,
                                             CommunicationBridgeInterface outgoingCommunicationBridge, FullHumanoidRobotModel fullRobotModel)
   {
      this(null, fullRobotModelFactory, yoTime, outgoingCommunicationBridge, fullRobotModel);
   }

   public WholeBodyInverseKinematicsBehavior(String namePrefix, FullHumanoidRobotModelFactory fullRobotModelFactory, DoubleYoVariable yoTime,
                                             CommunicationBridgeInterface outgoingCommunicationBridge, FullHumanoidRobotModel fullRobotModel)
   {
      super(namePrefix, outgoingCommunicationBridge);
      this.yoTime = yoTime;
      this.fullRobotModel = fullRobotModel;
      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      pelvisZUpFrame = referenceFrames.getPelvisZUpFrame();
      chestFrame = fullRobotModel.getChest().getBodyFixedFrame();

      solutionQualityThreshold = new DoubleYoVariable(behaviorName + "SolutionQualityThreshold", registry);
      solutionQualityThreshold.set(0.005);
      isPaused = new BooleanYoVariable(behaviorName + "IsPaused", registry);
      isStopped = new BooleanYoVariable(behaviorName + "IsStopped", registry);
      isDone = new BooleanYoVariable(behaviorName + "IsDone", registry);
      hasSolverFailed = new BooleanYoVariable(behaviorName + "HasSolverFailed", registry);
      hasSentMessageToController = new BooleanYoVariable(behaviorName + "HasSentMessageToController", registry);

      currentSolutionQuality = new DoubleYoVariable(behaviorName + "CurrentSolutionQuality", registry);
      trajectoryTime = new DoubleYoVariable(behaviorName + "TrajectoryTime", registry);
      timeSolutionSentToController = new DoubleYoVariable(behaviorName + "TimeSolutionSentToController", registry);

      for (RobotSide robotSide : RobotSide.values)
      {
         String side = robotSide.getCamelCaseNameForMiddleOfExpression();
         YoFramePoint desiredHandPosition = new YoFramePoint(behaviorName + "Desired" + side + "Hand", worldFrame, registry);
         yoDesiredHandPositions.put(robotSide, desiredHandPosition);
         YoFrameQuaternion desiredHandOrientation = new YoFrameQuaternion(behaviorName + "Desired" + side + "Hand", worldFrame, registry);
         yoDesiredHandOrientations.put(robotSide, desiredHandOrientation);
      }

      yoDesiredChestOrientation = new YoFrameQuaternion(behaviorName + "DesiredChest", worldFrame, registry);
      yoDesiredPelvisOrientation = new YoFrameQuaternion(behaviorName + "DesiredPelvis", worldFrame, registry);
      yoDesiredPelvisHeight = new DoubleYoVariable(behaviorName + "DesiredPelvisHeight", registry);

      outputConverter = new KinematicsToolboxOutputConverter(fullRobotModelFactory);

      attachNetworkListeningQueue(kinematicsToolboxOutputQueue, KinematicsToolboxOutputStatus.class);

      clear();
   }

   public void clear()
   {
      currentSolutionQuality.set(Double.POSITIVE_INFINITY);

      yoDesiredChestOrientation.setToNaN();
      yoDesiredPelvisOrientation.setToNaN();
      yoDesiredPelvisHeight.setToNaN();

      for (RobotSide robotSide : RobotSide.values)
      {
         yoDesiredHandPositions.get(robotSide).setToNaN();
         yoDesiredHandOrientations.get(robotSide).setToNaN();
      }
   }

   /** Change the threshold at which a solution is considered to be good enough */
   public void setSolutionQualityThreshold(double newThreshold)
   {
      solutionQualityThreshold.set(newThreshold);
   }

   public void setTrajectoryTime(double trajectoryTime)
   {
      this.trajectoryTime.set(trajectoryTime);
   }

   private final FramePoint desiredPosition = new FramePoint();
   private final FrameOrientation desiredOrientation = new FrameOrientation();

   public void setDesiredHandPose(RobotSide robotSide, FramePose desiredHandPose)
   {
      desiredHandPose.getPoseIncludingFrame(desiredPosition, desiredOrientation);
      setDesiredHandPose(robotSide, desiredPosition, desiredOrientation);
   }

   public void setDesiredHandPose(RobotSide robotSide, FramePoint desiredHandPosition, FrameOrientation desiredHandOrientation)
   {
      yoDesiredHandPositions.get(robotSide).setAndMatchFrame(desiredHandPosition);
      yoDesiredHandOrientations.get(robotSide).setAndMatchFrame(desiredHandOrientation);
   }

   public void setHandLinearControlOnly(RobotSide robotSide)
   {
      handSelectionMatrices.get(robotSide).setToLinearSelectionOnly();
   }

   public void setHandLinearControlAndYawPitchOnly(RobotSide robotSide)
   {
      handSelectionMatrices.get(robotSide).resetSelection();
      handSelectionMatrices.get(robotSide).selectAngularX(false);
   }

   public void holdCurrentChestOrientation()
   {
      FrameOrientation currentChestOrientation = new FrameOrientation(fullRobotModel.getChest().getBodyFixedFrame());
      yoDesiredChestOrientation.setAndMatchFrame(currentChestOrientation);
   }

   public void setDesiredChestOrientation(FrameOrientation desiredChestOrientation)
   {
      yoDesiredChestOrientation.setAndMatchFrame(desiredChestOrientation);
   }

   public void setChestAngularControl(boolean roll, boolean pitch, boolean yaw)
   {
      chestSelectionMatrix.setAxisSelection(roll, pitch, yaw);
   }

   public void holdCurrentPelvisOrientation()
   {
      FrameOrientation currentPelvisOrientation = new FrameOrientation(fullRobotModel.getPelvis().getBodyFixedFrame());
      yoDesiredPelvisOrientation.setAndMatchFrame(currentPelvisOrientation);
   }

   public void setDesiredPelvisOrientation(FrameOrientation desiredPelvisOrientation)
   {
      yoDesiredPelvisOrientation.setAndMatchFrame(desiredPelvisOrientation);
   }

   public void setPelvisAngularControl(boolean roll, boolean pitch, boolean yaw)
   {
      pelvisSelectionMatrix.setAxisSelection(roll, pitch, yaw);
   }

   public void holdCurrentPelvisHeight()
   {
      FramePoint currentPelvisPosition = new FramePoint(fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint());
      currentPelvisPosition.changeFrame(worldFrame);
      yoDesiredPelvisHeight.set(currentPelvisPosition.getZ());
   }

   public void setDesiredPelvisHeight(FramePoint pointContainingDesiredHeight)
   {
      pointContainingDesiredHeight = new FramePoint(pointContainingDesiredHeight);
      pointContainingDesiredHeight.changeFrame(worldFrame);
      yoDesiredPelvisHeight.set(pointContainingDesiredHeight.getZ());
   }

   public void setDesiredPelvisHeight(double desiredHeightInWorld)
   {
      yoDesiredPelvisHeight.set(desiredHeightInWorld);
   }

   public void setBodyWeights(BodyWeights bodyWeights)
   {
      if (trackingWeightsMessage == null)
      {
         trackingWeightsMessage = new TrackingWeightsMessage(bodyWeights);
      }
      else
      {
         trackingWeightsMessage.setTrackingWeightsMessage(bodyWeights);
      }
   }

   public double getSolutionQuality()
   {
      return currentSolutionQuality.getDoubleValue();
   }

   @Override
   public void onBehaviorEntered()
   {

      System.out.println("init whole body behavior");
      isPaused.set(false);
      isStopped.set(false);
      isDone.set(false);
      hasSentMessageToController.set(false);
      hasSolverFailed.set(false);
      solutionSentToController = null;
      ToolboxStateMessage message = new ToolboxStateMessage(ToolboxState.WAKE_UP);
      message.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
      sendPacket(message);

      for (RobotSide robotSide : RobotSide.values)
      {
         YoFramePoint yoDesiredHandPosition = yoDesiredHandPositions.get(robotSide);
         YoFrameQuaternion yoDesiredHandOrientation = yoDesiredHandOrientations.get(robotSide);

         if (yoDesiredHandPosition.containsNaN() || yoDesiredHandOrientation.containsNaN())
         {
            handTrajectoryMessage.put(robotSide, null);
         }
         else
         {
            Point3D desiredHandPosition = new Point3D();
            Quaternion desiredHandOrientation = new Quaternion();
            yoDesiredHandPosition.get(desiredHandPosition);
            yoDesiredHandOrientation.get(desiredHandOrientation);
            HandTrajectoryMessage temporaryHandTrajectoryMessage = new HandTrajectoryMessage(robotSide, 0.0, desiredHandPosition, desiredHandOrientation, worldFrame, chestFrame);
            handTrajectoryMessage.put(robotSide, temporaryHandTrajectoryMessage);
         }
      }

      if (yoDesiredChestOrientation.containsNaN())
      {
         chestTrajectoryMessage = null;
      }
      else
      {
         YoFrameQuaternion yoDesiredChestQuaternion = yoDesiredChestOrientation;
         Quaternion desiredChestOrientation = new Quaternion();
         yoDesiredChestQuaternion.get(desiredChestOrientation);
         chestTrajectoryMessage = new ChestTrajectoryMessage(0.0, desiredChestOrientation, worldFrame, pelvisZUpFrame);
      }

      if (yoDesiredPelvisOrientation.containsNaN())
      {
         pelvisOrientationTrajectoryMessage = null;
      }
      else
      {
         YoFrameQuaternion yoDesiredPelvisQuaternion = yoDesiredPelvisOrientation;
         Quaternion desiredPelvisOrientation = new Quaternion();
         yoDesiredPelvisQuaternion.get(desiredPelvisOrientation);
         pelvisOrientationTrajectoryMessage = new PelvisOrientationTrajectoryMessage(0.0, desiredPelvisOrientation);
      }

      if (yoDesiredPelvisHeight.isNaN())
      {
         pelvisHeightTrajectoryMessage = null;
      }
      else
      {
         pelvisHeightTrajectoryMessage = new PelvisHeightTrajectoryMessage(0.0, yoDesiredPelvisHeight.getDoubleValue());
      }
   }

   @Override
   public void doControl()
   {
      if (!hasSentMessageToController.getBooleanValue())
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            if (handTrajectoryMessage.get(robotSide) != null)
            {
               handTrajectoryMessage.get(robotSide).setSelectionMatrix(handSelectionMatrices.get(robotSide));
               handTrajectoryMessage.get(robotSide).setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
               sendPacket(handTrajectoryMessage.get(robotSide));
            }
         }

         if (chestTrajectoryMessage != null)
         {
            chestTrajectoryMessage.setSelectionMatrix(chestSelectionMatrix);
            chestTrajectoryMessage.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
            sendPacket(chestTrajectoryMessage);
         }

         if (pelvisOrientationTrajectoryMessage != null)
         {
            pelvisOrientationTrajectoryMessage.setSelectionMatrix(pelvisSelectionMatrix);
            pelvisOrientationTrajectoryMessage.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
            sendPacket(pelvisOrientationTrajectoryMessage);
         }

         if (pelvisHeightTrajectoryMessage != null)
         {
            pelvisHeightTrajectoryMessage.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
            sendPacket(pelvisHeightTrajectoryMessage);
         }

         if (trackingWeightsMessage != null)
         {
            trackingWeightsMessage.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
            sendPacket(trackingWeightsMessage);
         }

      }
      if (kinematicsToolboxOutputQueue.isNewPacketAvailable() && !hasSentMessageToController.getBooleanValue())
      {
         KinematicsToolboxOutputStatus newestSolution = kinematicsToolboxOutputQueue.poll();

         double deltaSolutionQuality = currentSolutionQuality.getDoubleValue() - newestSolution.getSolutionQuality();
         boolean isSolutionStable = deltaSolutionQuality > 0.0 && deltaSolutionQuality < 1.0e-6;
         boolean isSolutionGoodEnough = newestSolution.getSolutionQuality() < solutionQualityThreshold.getDoubleValue();
         boolean sendSolutionToController = isSolutionStable && isSolutionGoodEnough;
         if (!isPaused())
         {
            if (isSolutionStable && !isSolutionGoodEnough)
            {
               hasSolverFailed.set(true);
            }
            else if (sendSolutionToController)
            {
               solutionSentToController = newestSolution;
               outputConverter.setTrajectoryTime(trajectoryTime.getDoubleValue());
               WholeBodyTrajectoryMessage message = new WholeBodyTrajectoryMessage();
               message.setDestination(PacketDestination.CONTROLLER);
               outputConverter.updateFullRobotModel(newestSolution);
               outputConverter.setMessageToCreate(message);
               outputConverter.computeHandTrajectoryMessages();
               outputConverter.computeChestTrajectoryMessage();
               outputConverter.computePelvisTrajectoryMessage();
               sendPacketToController(message);
               hasSentMessageToController.set(true);
               deactivateKinematicsToolboxModule();
               timeSolutionSentToController.set(yoTime.getDoubleValue());
            }
         }
         currentSolutionQuality.set(newestSolution.getSolutionQuality());

         newestSolution.setDestination(PacketDestination.UI);
         sendPacket(newestSolution);
      }
      else if (hasSentMessageToController.getBooleanValue())
      {
         if (solutionSentToController != null && !isDone.getBooleanValue()) // To visualize the solution sent to the controller
            sendPacket(solutionSentToController);

         if (yoTime.getDoubleValue() - timeSolutionSentToController.getDoubleValue() > trajectoryTime.getDoubleValue())
         {
            isDone.set(true);
         }
      }
   }

   public boolean hasSolverFailed()
   {
      return hasSolverFailed.getBooleanValue();
   }

   @Override
   public boolean isDone()
   {
      return isDone.getBooleanValue() || hasSolverFailed.getBooleanValue();
   }

   @Override
   public void onBehaviorExited()
   {
      isPaused.set(false);
      isStopped.set(false);
      isDone.set(false);
      hasSolverFailed.set(false);
      hasSentMessageToController.set(false);
      solutionSentToController = null;
      chestTrajectoryMessage = null;
      pelvisOrientationTrajectoryMessage = null;
      pelvisHeightTrajectoryMessage = null;
      trackingWeightsMessage = null;

      for (RobotSide robotSide : RobotSide.values)
      {
         handTrajectoryMessage.put(robotSide, null);
      }

      deactivateKinematicsToolboxModule();
   }

   private void deactivateKinematicsToolboxModule()
   {
      ToolboxStateMessage message = new ToolboxStateMessage(ToolboxState.SLEEP);
      message.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
      sendPacket(message);
   }

   @Override
   public void onBehaviorAborted()
   {
   }

   @Override
   public void onBehaviorPaused()
   {
   }

   @Override
   public void onBehaviorResumed()
   {
   }

}
