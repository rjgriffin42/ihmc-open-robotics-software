package us.ihmc.humanoidBehaviors.behaviors.primitives;

import us.ihmc.communication.packets.KinematicsToolboxOutputStatus;
import us.ihmc.communication.packets.KinematicsToolboxRigidBodyMessage;
import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.communication.packets.ToolboxStateMessage;
import us.ihmc.communication.packets.ToolboxStateMessage.ToolboxState;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidBehaviors.behaviors.AbstractBehavior;
import us.ihmc.humanoidBehaviors.communication.CommunicationBridgeInterface;
import us.ihmc.humanoidBehaviors.communication.ConcurrentListeningQueue;
import us.ihmc.humanoidRobotics.communication.packets.KinematicsToolboxOutputConverter;
import us.ihmc.humanoidRobotics.communication.packets.wholebody.WholeBodyTrajectoryMessage;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;

public class WholeBodyInverseKinematicsBehavior extends AbstractBehavior
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoDouble solutionQualityThreshold;
   private final YoDouble currentSolutionQuality;
   private final YoBoolean isPaused;
   private final YoBoolean isStopped;
   private final YoBoolean isDone;
   private final YoBoolean hasSolverFailed;
   private final YoBoolean hasSentMessageToController;

   private final SideDependentList<SelectionMatrix6D> handSelectionMatrices = new SideDependentList<>(new SelectionMatrix6D(), new SelectionMatrix6D());
   private final SelectionMatrix6D chestSelectionMatrix = new SelectionMatrix6D();
   private final SelectionMatrix6D pelvisSelectionMatrix = new SelectionMatrix6D();
   private final SideDependentList<YoFramePoint> yoDesiredHandPositions = new SideDependentList<>();
   private final SideDependentList<YoFrameQuaternion> yoDesiredHandOrientations = new SideDependentList<>();
   private final YoFrameQuaternion yoDesiredChestOrientation;
   private final YoFrameQuaternion yoDesiredPelvisOrientation;
   private final YoFramePoint yoDesiredPelvisPosition;
   private final YoDouble trajectoryTime;

   private final KinematicsToolboxOutputConverter outputConverter;
   private final FullHumanoidRobotModel fullRobotModel;
   private KinematicsToolboxRigidBodyMessage chestMessage;
   private KinematicsToolboxRigidBodyMessage pelvisMessage;
   private SideDependentList<KinematicsToolboxRigidBodyMessage> handMessages = new SideDependentList<>();

   private final ConcurrentListeningQueue<KinematicsToolboxOutputStatus> kinematicsToolboxOutputQueue = new ConcurrentListeningQueue<>(40);
   private KinematicsToolboxOutputStatus solutionSentToController = null;

   private final YoDouble yoTime;
   private final YoDouble timeSolutionSentToController;

   public WholeBodyInverseKinematicsBehavior(FullHumanoidRobotModelFactory fullRobotModelFactory, YoDouble yoTime,
                                             CommunicationBridgeInterface outgoingCommunicationBridge, FullHumanoidRobotModel fullRobotModel)
   {
      this(null, fullRobotModelFactory, yoTime, outgoingCommunicationBridge, fullRobotModel);
   }

   public WholeBodyInverseKinematicsBehavior(String namePrefix, FullHumanoidRobotModelFactory fullRobotModelFactory, YoDouble yoTime,
                                             CommunicationBridgeInterface outgoingCommunicationBridge, FullHumanoidRobotModel fullRobotModel)
   {
      super(namePrefix, outgoingCommunicationBridge);
      this.yoTime = yoTime;
      this.fullRobotModel = fullRobotModel;

      solutionQualityThreshold = new YoDouble(behaviorName + "SolutionQualityThreshold", registry);
      solutionQualityThreshold.set(0.005);
      isPaused = new YoBoolean(behaviorName + "IsPaused", registry);
      isStopped = new YoBoolean(behaviorName + "IsStopped", registry);
      isDone = new YoBoolean(behaviorName + "IsDone", registry);
      hasSolverFailed = new YoBoolean(behaviorName + "HasSolverFailed", registry);
      hasSentMessageToController = new YoBoolean(behaviorName + "HasSentMessageToController", registry);

      currentSolutionQuality = new YoDouble(behaviorName + "CurrentSolutionQuality", registry);
      trajectoryTime = new YoDouble(behaviorName + "TrajectoryTime", registry);
      timeSolutionSentToController = new YoDouble(behaviorName + "TimeSolutionSentToController", registry);

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
      yoDesiredPelvisPosition = new YoFramePoint(behaviorName + "DesiredPelvis", worldFrame, registry);

      pelvisSelectionMatrix.setToAngularSelectionOnly();
      chestSelectionMatrix.setToAngularSelectionOnly();

      outputConverter = new KinematicsToolboxOutputConverter(fullRobotModelFactory);

      attachNetworkListeningQueue(kinematicsToolboxOutputQueue, KinematicsToolboxOutputStatus.class);

      clear();
   }

   public void clear()
   {
      currentSolutionQuality.set(Double.POSITIVE_INFINITY);

      yoDesiredChestOrientation.setToNaN();
      yoDesiredPelvisOrientation.setToNaN();
      yoDesiredPelvisPosition.setToNaN();

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

   private final FramePoint3D desiredPosition = new FramePoint3D();
   private final FrameOrientation desiredOrientation = new FrameOrientation();

   public void setDesiredHandPose(RobotSide robotSide, FramePose desiredHandPose)
   {
      desiredHandPose.getPoseIncludingFrame(desiredPosition, desiredOrientation);
      setDesiredHandPose(robotSide, desiredPosition, desiredOrientation);
   }

   public void setDesiredHandPose(RobotSide robotSide, FramePoint3D desiredHandPosition, FrameOrientation desiredHandOrientation)
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
      chestSelectionMatrix.setToAngularSelectionOnly();
   }

   public void setDesiredChestOrientation(FrameOrientation desiredChestOrientation)
   {
      yoDesiredChestOrientation.setAndMatchFrame(desiredChestOrientation);
   }

   public void setChestAngularControl(boolean roll, boolean pitch, boolean yaw)
   {
      chestSelectionMatrix.setAngularAxisSelection(roll, pitch, yaw);
      chestSelectionMatrix.clearLinearSelection();
   }

   public void holdCurrentPelvisOrientation()
   {
      FrameOrientation currentPelvisOrientation = new FrameOrientation(fullRobotModel.getPelvis().getBodyFixedFrame());
      yoDesiredPelvisOrientation.setAndMatchFrame(currentPelvisOrientation);
      pelvisSelectionMatrix.setToAngularSelectionOnly();
   }

   public void setDesiredPelvisOrientation(FrameOrientation desiredPelvisOrientation)
   {
      yoDesiredPelvisOrientation.setAndMatchFrame(desiredPelvisOrientation);
   }

   public void setPelvisAngularControl(boolean roll, boolean pitch, boolean yaw)
   {
      pelvisSelectionMatrix.setAngularAxisSelection(roll, pitch, yaw);
      pelvisSelectionMatrix.clearLinearSelection();
   }

   public void holdCurrentPelvisHeight()
   {
      yoDesiredPelvisPosition.setFromReferenceFrame(fullRobotModel.getPelvis().getParentJoint().getFrameAfterJoint());
      pelvisSelectionMatrix.clearLinearSelection();
      pelvisSelectionMatrix.selectLinearZ(true);
      pelvisSelectionMatrix.setSelectionFrame(worldFrame);
   }

   public void setDesiredPelvisHeight(FramePoint3D pointContainingDesiredHeight)
   {
      yoDesiredPelvisPosition.setAndMatchFrame(pointContainingDesiredHeight);
      pelvisSelectionMatrix.clearLinearSelection();
      pelvisSelectionMatrix.selectLinearZ(true);
      pelvisSelectionMatrix.setSelectionFrame(worldFrame);
   }

   public void setDesiredPelvisHeight(double desiredHeightInWorld)
   {
      yoDesiredPelvisPosition.setZ(desiredHeightInWorld);
      pelvisSelectionMatrix.clearLinearSelection();
      pelvisSelectionMatrix.selectLinearZ(true);
      pelvisSelectionMatrix.setSelectionFrame(worldFrame);
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
            handMessages.put(robotSide, null);
         }
         else
         {
            Point3D desiredHandPosition = new Point3D();
            Quaternion desiredHandOrientation = new Quaternion();
            yoDesiredHandPosition.get(desiredHandPosition);
            yoDesiredHandOrientation.get(desiredHandOrientation);
            RigidBody hand = fullRobotModel.getHand(robotSide);
            ReferenceFrame handControlFrame = fullRobotModel.getHandControlFrame(robotSide);
            KinematicsToolboxRigidBodyMessage handMessage = new KinematicsToolboxRigidBodyMessage(hand, handControlFrame, desiredHandPosition,
                                                                                                  desiredHandOrientation);
            handMessage.setWeight(20.0);
            handMessages.put(robotSide, handMessage);
         }
      }

      if (yoDesiredChestOrientation.containsNaN())
      {
         chestMessage = null;
      }
      else
      {
         Quaternion desiredChestOrientation = new Quaternion();
         yoDesiredChestOrientation.get(desiredChestOrientation);
         RigidBody chest = fullRobotModel.getChest();
         chestMessage = new KinematicsToolboxRigidBodyMessage(chest, desiredChestOrientation);
         chestMessage.setWeight(0.02);
      }

      RigidBody pelvis = fullRobotModel.getPelvis();

      if (yoDesiredPelvisOrientation.containsNaN() && yoDesiredPelvisPosition.containsNaN())
         pelvisMessage = null;
      else
         pelvisMessage = new KinematicsToolboxRigidBodyMessage(pelvis);

      if (!yoDesiredPelvisOrientation.containsNaN())
      {
         Quaternion desiredPelvisOrientation = new Quaternion();
         yoDesiredPelvisOrientation.get(desiredPelvisOrientation);
         pelvisMessage.setDesiredOrientation(desiredPelvisOrientation);
         pelvisMessage.setWeight(0.02);
      }

      if (!yoDesiredPelvisPosition.containsNaN())
      {
         Point3D desiredPelvisPosition = new Point3D();
         yoDesiredPelvisPosition.get(desiredPelvisPosition);
         pelvisMessage.setDesiredPosition(desiredPelvisPosition);
         pelvisMessage.setWeight(0.02);
      }
   }

   @Override
   public void doControl()
   {
      if (!hasSentMessageToController.getBooleanValue())
      {
         for (RobotSide robotSide : RobotSide.values)
         {
            if (handMessages.get(robotSide) != null)
            {
               handMessages.get(robotSide).setSelectionMatrix(handSelectionMatrices.get(robotSide));
               handMessages.get(robotSide).setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
               sendPacket(handMessages.get(robotSide));
            }
         }

         if (chestMessage != null)
         {
            chestMessage.setSelectionMatrix(chestSelectionMatrix);
            chestMessage.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
            sendPacket(chestMessage);
         }

         if (pelvisMessage != null)
         {
            pelvisMessage.setSelectionMatrix(pelvisSelectionMatrix);
            pelvisMessage.setDestination(PacketDestination.KINEMATICS_TOOLBOX_MODULE);
            sendPacket(pelvisMessage);
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
      chestMessage = null;
      pelvisMessage = null;
      pelvisSelectionMatrix.setToAngularSelectionOnly();
      chestSelectionMatrix.setToAngularSelectionOnly();

      for (RobotSide robotSide : RobotSide.values)
      {
         handMessages.put(robotSide, null);
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
