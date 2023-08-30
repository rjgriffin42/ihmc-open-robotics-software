package us.ihmc.rdx.ui.affordances;

import controller_msgs.msg.dds.ArmTrajectoryMessage;
import controller_msgs.msg.dds.GoHomeMessage;
import controller_msgs.msg.dds.HandTrajectoryMessage;
import imgui.ImGui;
import imgui.type.ImBoolean;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.ROS2SyncedRobotModel;
import us.ihmc.avatar.inverseKinematics.ArmIKSolver;
import us.ihmc.behaviors.tools.CommunicationHelper;
import us.ihmc.behaviors.tools.HandWrenchCalculator;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.thread.TypedNotification;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.rdx.imgui.ImGuiUniqueLabelMap;
import us.ihmc.rdx.ui.RDXBaseUI;
import us.ihmc.rdx.ui.teleoperation.RDXDesiredRobot;
import us.ihmc.rdx.ui.teleoperation.RDXHandConfigurationManager;
import us.ihmc.rdx.ui.teleoperation.RDXTeleoperationParameters;
import us.ihmc.robotModels.FullRobotModelUtils;
import us.ihmc.robotics.MultiBodySystemMissingTools;
import us.ihmc.robotics.partNames.ArmJointName;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.tools.thread.MissingThreadTools;

/**
 * This class manages the UI for operating the arms of a humanoid robot.
 * This includes sending the arms to predefined joint angles poses and
 * operating a IK solver to achieve desired hand and elbow poses.
 */
public class RDXArmManager
{
   /* Sake hand's fingers do not extend much outward from the hand at 15 degrees apart, i.e. they are nearly parallel
      kinda look like this:
      15 degrees apart  |  0 degrees apart

        /\    /\                 /||\
       | |    | | <- fingers -> / /\ \
       | |    | |              / /  \ \
       |  ----  |             |  ----  |
       |        | <-  palm -> |        |
       |________|             |________|
    */
   private final static double SAKE_HAND_SAFEE_FINGER_ANGLE = 15.0;

   private final ImGuiUniqueLabelMap labels = new ImGuiUniqueLabelMap(getClass());
   private final CommunicationHelper communicationHelper;
   private final ROS2SyncedRobotModel syncedRobot;
   private final RDXDesiredRobot desiredRobot;

   private final RDXTeleoperationParameters teleoperationParameters;
   private final SideDependentList<RDXInteractableHand> interactableHands;

   private final ArmJointName[] armJointNames;
   private RDXArmControlMode armControlMode = RDXArmControlMode.JOINT_ANGLES;
   private final SideDependentList<double[]> armsWide = new SideDependentList<>();
   private final SideDependentList<double[]> doorAvoidanceArms = new SideDependentList<>();
   /** Nadia's arm positions where it's holding a shield in front of the chest. */
   private final SideDependentList<double[]> shieldHoldingArms = new SideDependentList<>();
   private final RDXHandConfigurationManager handManager;

   private final SideDependentList<ArmIKSolver> armIKSolvers = new SideDependentList<>();
   private final SideDependentList<OneDoFJointBasics[]> desiredRobotArmJoints = new SideDependentList<>();

   private volatile boolean readyToSolve = true;
   private volatile boolean readyToCopySolution = false;

   private final HandWrenchCalculator handWrenchCalculator;
   private final ImBoolean indicateWrenchOnScreen = new ImBoolean(false);
   private RDX3DPanelHandWrenchIndicator panelHandWrenchIndicator;

   private final TypedNotification<RobotSide> showWarningNotification = new TypedNotification<>();

   public RDXArmManager(CommunicationHelper communicationHelper,
                        DRCRobotModel robotModel,
                        ROS2SyncedRobotModel syncedRobot,
                        RDXDesiredRobot desiredRobot,
                        RDXTeleoperationParameters teleoperationParameters,
                        SideDependentList<RDXInteractableHand> interactableHands)
   {
      this.communicationHelper = communicationHelper;
      this.syncedRobot = syncedRobot;
      this.desiredRobot = desiredRobot;
      this.teleoperationParameters = teleoperationParameters;
      this.interactableHands = interactableHands;
      armJointNames = robotModel.getJointMap().getArmJointNames();

      for (RobotSide side : RobotSide.values)
      {
         armsWide.put(side,
                      new double[] {0.6,
                                    side.negateIfRightSide(0.3),
                                    side.negateIfRightSide(-0.5),
                                    -1.0,
                                    side.negateIfRightSide(-0.6),
                                    0.000,
                                    side.negateIfLeftSide(0.0)});
      }
      doorAvoidanceArms.put(RobotSide.LEFT, new double[] {-0.121, -0.124, -0.971, -1.513, -0.935, -0.873, 0.245});
      doorAvoidanceArms.put(RobotSide.RIGHT, new double[] {-0.523, -0.328, 0.586, -2.192, 0.828, 1.009, -0.281});
      shieldHoldingArms.put(RobotSide.LEFT, new double[] {-1.01951, 0.72311, -1.29244, -1.26355, -0.51712, -0.04580, -0.00659});
      shieldHoldingArms.put(RobotSide.RIGHT, new double[7]);
      boolean[] invert = new boolean[] {false, true, true, false, true, false, false};
      for (int i = 0; i < shieldHoldingArms.get(RobotSide.LEFT).length; i++)
      {
         shieldHoldingArms.get(RobotSide.RIGHT)[i] = (invert[i] ? -1.0 : 1.0) * shieldHoldingArms.get(RobotSide.LEFT)[i];
      }

      handWrenchCalculator = new HandWrenchCalculator(syncedRobot);

      for (RobotSide side : RobotSide.values)
      {
         armIKSolvers.put(side, new ArmIKSolver(side, robotModel, syncedRobot.getFullRobotModel()));
         desiredRobotArmJoints.put(side, FullRobotModelUtils.getArmJoints(desiredRobot.getDesiredFullRobotModel(), side, robotModel.getJointMap().getArmJointNames()));
      }

      handManager = new RDXHandConfigurationManager();
   }

   public void create(RDXBaseUI baseUI)
   {
      panelHandWrenchIndicator = new RDX3DPanelHandWrenchIndicator(baseUI.getPrimary3DPanel());
      baseUI.getPrimary3DPanel().addImGuiOverlayAddition(() ->
      {
         if (indicateWrenchOnScreen.get())
            panelHandWrenchIndicator.renderImGuiOverlay();
      });

      handManager.create(baseUI, communicationHelper, syncedRobot);
   }

   public void update()
   {
      handWrenchCalculator.compute();

      boolean desiredHandPoseChanged = false;
      for (RobotSide side : interactableHands.sides())
      {
         armIKSolvers.get(side).update(interactableHands.get(side).getControlReferenceFrame());

         // wrench expressed in wrist pitch body fixed-frame
         boolean showWrench = indicateWrenchOnScreen.get();
         if (interactableHands.get(side).getEstimatedHandWrenchArrows().getShow() != showWrench)
            interactableHands.get(side).getEstimatedHandWrenchArrows().setShow(showWrench);
         interactableHands.get(side).updateEstimatedWrench(handWrenchCalculator.getFilteredWrench().get(side));

         // Check if the desired hand pose changed and we need to run the solver again.
         // We only want to evaluate this when we are going to take action on it
         // Otherwise, we will not notice the desired changed while the solver was still solving
         if (readyToSolve)
         {
            desiredHandPoseChanged |= armIKSolvers.get(side).getDesiredHandControlPoseChanged();
         }

         if (showWrench)
         {
            panelHandWrenchIndicator.update(side,
                                            handWrenchCalculator.getLinearWrenchMagnitude(side, true),
                                            handWrenchCalculator.getAngularWrenchMagnitude(side, true));
         }
      }

      // The following puts the solver on a thread as to not slow down the UI
      if (readyToSolve && desiredHandPoseChanged)
      {
         readyToSolve = false;
         for (RobotSide side : interactableHands.sides())
         {
            armIKSolvers.get(side).copyActualToWork();
         }

         MissingThreadTools.startAThread("IKSolver", DefaultExceptionHandler.MESSAGE_AND_STACKTRACE, () ->
         {
            try
            {
               for (RobotSide side : interactableHands.sides())
               {
                  armIKSolvers.get(side).solve();
               }
            }
            finally
            {
               readyToCopySolution = true;
            }
         });
      }

      if (readyToCopySolution)
      {
         readyToCopySolution = false;
         for (RobotSide side : interactableHands.sides())
         {
            MultiBodySystemMissingTools.copyOneDoFJointsConfiguration(armIKSolvers.get(side).getSolutionOneDoFJoints(), desiredRobotArmJoints.get(side));
         }

         readyToSolve = true;
      }

      desiredRobot.getDesiredFullRobotModel().getRootJoint().setJointConfiguration(syncedRobot.getFullRobotModel().getRootJoint().getJointPose());
      desiredRobot.getDesiredFullRobotModel().updateFrames();
   }

   public void renderImGuiWidgets()
   {
      handManager.renderImGuiWidgets();

      ImGui.text("Arms:");
      for (RobotSide side : RobotSide.values)
      {
         ImGui.sameLine();
         if (ImGui.button(labels.get("Home " + side.getPascalCaseName())))
         {
            executeArmHome(side);
         }
      }

      ImGui.text("Wide Arms:");
      for (RobotSide side : RobotSide.values)
      {
         ImGui.sameLine();
         if (ImGui.button(labels.get("Wide " + side.getPascalCaseName())))
         {
            ArmTrajectoryMessage armTrajectoryMessage = HumanoidMessageTools.createArmTrajectoryMessage(side,
                                                                                                        teleoperationParameters.getTrajectoryTime(),
                                                                                                        armsWide.get(side));
            communicationHelper.publishToController(armTrajectoryMessage);
         }
      }
      ImGui.text("Door avoidance arms:");
      for (RobotSide side : RobotSide.values)
      {
         ImGui.sameLine();
         if (ImGui.button(labels.get(side.getPascalCaseName(), "Door avoidance")))
         {
            executeDoorAvoidanceArmAngles(side);
         }
      }
      ImGui.sameLine();
      ImGui.text("Shield holding arms:");
      for (RobotSide side : RobotSide.values)
      {
         ImGui.sameLine();
         if (ImGui.button(labels.get(side.getPascalCaseName(), "Shield holding")))
         {
            executeShieldHoldingArmAngles(side);
         }
      }

      ImGui.text("Arm & hand control mode:");
      ImGui.sameLine();
      if (ImGui.radioButton(labels.get("Joint angles (DDogleg)"), armControlMode == RDXArmControlMode.JOINT_ANGLES))
      {
         armControlMode = RDXArmControlMode.JOINT_ANGLES;
      }
      ImGui.text("Hand pose only:");
      ImGui.sameLine();
      if (ImGui.radioButton(labels.get("World"), armControlMode == RDXArmControlMode.POSE_WORLD))
      {
         armControlMode = RDXArmControlMode.POSE_WORLD;
      }
      ImGui.sameLine();
      if (ImGui.radioButton(labels.get("Chest"), armControlMode == RDXArmControlMode.POSE_CHEST))
      {
         armControlMode = RDXArmControlMode.POSE_CHEST;
      }

      ImGui.checkbox(labels.get("Hand wrench magnitudes on 3D View"), indicateWrenchOnScreen);

      // Pop up warning if notification is set
      if (showWarningNotification.peekHasValue() && showWarningNotification.poll())
      {
         ImGui.openPopup(labels.get("Warning"));
      }

      if (ImGui.beginPopupModal(labels.get("Warning")))
      {
         ImGui.text("""
                          The hand is currently open.
                                                    
                          Continuing to door avoidance
                          may cause the hand to collide
                          with the body of the robot.""");

         ImGui.separator();
         if (ImGui.button("Continue"))
         {
            executeArmAngles(showWarningNotification.read(), doorAvoidanceArms, teleoperationParameters.getTrajectoryTime());
            ImGui.closeCurrentPopup();
         }
         ImGui.sameLine();
         if (ImGui.button("Cancel"))
         {
            ImGui.closeCurrentPopup();
         }
         ImGui.endPopup();
      }
   }

   public void executeArmHome(RobotSide side)
   {
      GoHomeMessage armHomeMessage = new GoHomeMessage();
      armHomeMessage.setHumanoidBodyPart(GoHomeMessage.HUMANOID_BODY_PART_ARM);

      if (side == RobotSide.LEFT)
         armHomeMessage.setRobotSide(GoHomeMessage.ROBOT_SIDE_LEFT);
      else
         armHomeMessage.setRobotSide(GoHomeMessage.ROBOT_SIDE_RIGHT);

      armHomeMessage.setTrajectoryTime(teleoperationParameters.getTrajectoryTime());
      communicationHelper.publishToController(armHomeMessage);
   }

   public void executeDoorAvoidanceArmAngles(RobotSide side)
   {
      // Warning pops up if fingers are more than 15 degrees from "zero" (zero = when fingertips are parallel)
      // i.e. when the fingers are more than 30 degrees apart from each other
      // This is an arbitrary value
      if (syncedRobot.getLatestHandJointAnglePacket(side).getJointAngles().get(0) > Math.toRadians(SAKE_HAND_SAFEE_FINGER_ANGLE))
      {
         showWarningNotification.set(side);
      }
      else
      {
         executeArmAngles(side, doorAvoidanceArms, teleoperationParameters.getTrajectoryTime());
      }
   }

   public void executeShieldHoldingArmAngles(RobotSide side)
   {
      executeArmAngles(side, shieldHoldingArms, 3.0);
   }

   public void executeArmAngles(RobotSide side, SideDependentList<double[]> jointAngles, double trajectoryTime)
   {
      ArmTrajectoryMessage armTrajectoryMessage = HumanoidMessageTools.createArmTrajectoryMessage(side,
                                                                                                  trajectoryTime,
                                                                                                  jointAngles.get(side));
      communicationHelper.publishToController(armTrajectoryMessage);
   }

   public Runnable getSubmitDesiredArmSetpointsCallback(RobotSide robotSide)
   {
      Runnable runnable = () ->
      {
         if (armControlMode == RDXArmControlMode.JOINT_ANGLES)
         {
            double[] jointAngles = new double[armJointNames.length];
            int i = -1;
            for (ArmJointName armJoint : armJointNames)
            {
               jointAngles[++i] = desiredRobot.getDesiredFullRobotModel().getArmJoint(robotSide, armJoint).getQ();
            }

            LogTools.info("Sending ArmTrajectoryMessage");
            ArmTrajectoryMessage armTrajectoryMessage = HumanoidMessageTools.createArmTrajectoryMessage(robotSide,
                                                                                                        teleoperationParameters.getTrajectoryTime(),
                                                                                                        jointAngles);
            communicationHelper.publishToController(armTrajectoryMessage);
         }
         else if (armControlMode == RDXArmControlMode.POSE_WORLD || armControlMode == RDXArmControlMode.POSE_CHEST)
         {
            FramePose3D desiredControlFramePose = new FramePose3D(interactableHands.get(robotSide).getControlReferenceFrame());

            ReferenceFrame frame;
            if (armControlMode == RDXArmControlMode.POSE_WORLD)
            {
               frame = ReferenceFrame.getWorldFrame();
            }
            else
            {
               frame = syncedRobot.getReferenceFrames().getChestFrame();
            }
            desiredControlFramePose.changeFrame(frame);

            LogTools.info("Sending HandTrajectoryMessage");
            HandTrajectoryMessage handTrajectoryMessage = HumanoidMessageTools.createHandTrajectoryMessage(robotSide,
                                                                                                           teleoperationParameters.getTrajectoryTime(),
                                                                                                           desiredControlFramePose,
                                                                                                           frame);
            long dataFrameId = MessageTools.toFrameId(frame);
            handTrajectoryMessage.getSe3Trajectory().getFrameInformation().setDataReferenceFrameId(dataFrameId);
            communicationHelper.publishToController(handTrajectoryMessage);
         }
      };
      return runnable;
   }

   public RDXHandConfigurationManager getHandManager()
   {
      return handManager;
   }

   public RDXArmControlMode getArmControlMode()
   {
      return armControlMode;
   }
}