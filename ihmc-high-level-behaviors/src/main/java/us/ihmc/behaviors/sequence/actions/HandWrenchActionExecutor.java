package us.ihmc.behaviors.sequence.actions;

import controller_msgs.msg.dds.HandWrenchTrajectoryMessage;
import controller_msgs.msg.dds.WrenchTrajectoryMessage;
import controller_msgs.msg.dds.WrenchTrajectoryPointMessage;
import ihmc_common_msgs.msg.dds.FrameInformation;
import ihmc_common_msgs.msg.dds.QueueableMessage;
import us.ihmc.avatar.ros2.ROS2ControllerHelper;
import us.ihmc.behaviors.sequence.ActionNodeExecutor;
import us.ihmc.communication.crdt.CRDTInfo;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.idl.IDLSequence;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.io.WorkspaceResourceDirectory;

public class HandWrenchActionExecutor extends ActionNodeExecutor<HandWrenchActionState, HandWrenchActionDefinition>
{
   private final HandWrenchActionState state;
   private final HandWrenchActionDefinition definition;
   private final ROS2ControllerHelper ros2ControllerHelper;

   public HandWrenchActionExecutor(long id, CRDTInfo crdtInfo, WorkspaceResourceDirectory saveFileDirectory, ROS2ControllerHelper ros2ControllerHelper)
   {
      super(new HandWrenchActionState(id, crdtInfo, saveFileDirectory));

      state = getState();
      definition = getDefinition();

      this.ros2ControllerHelper = ros2ControllerHelper;
   }

   @Override
   public void update()
   {
      super.update();
   }

   @Override
   public void triggerActionExecution()
   {
      super.triggerActionExecution();

      HandWrenchTrajectoryMessage handWrenchTrajectoryMessage = new HandWrenchTrajectoryMessage();
      handWrenchTrajectoryMessage.setRobotSide(getDefinition().getSide().toByte());
      handWrenchTrajectoryMessage.setForceExecution(true);

      WrenchTrajectoryMessage wrenchTrajectory = handWrenchTrajectoryMessage.getWrenchTrajectory();

      IDLSequence.Object<WrenchTrajectoryPointMessage> wrenchTrajectoryPoints
            = wrenchTrajectory.getWrenchTrajectoryPoints();

      double time0 = 0.0;
      Vector3D torque0 = new Vector3D(getDefinition().getSide() == RobotSide.RIGHT ? definition.getTorqueX() : -definition.getTorqueX(),
                                      getDefinition().getSide() == RobotSide.RIGHT ? definition.getTorqueY() : -definition.getTorqueY(),
                                      getDefinition().getSide() == RobotSide.RIGHT ? definition.getTorqueZ() : -definition.getTorqueZ());
      Vector3D force0 = new Vector3D(getDefinition().getSide() == RobotSide.RIGHT ? definition.getForceX() : -definition.getForceX(),
                                     getDefinition().getSide() == RobotSide.RIGHT ? definition.getForceY() : -definition.getForceY(),
                                     getDefinition().getSide() == RobotSide.RIGHT ? definition.getForceZ() : -definition.getForceZ());

      WrenchTrajectoryPointMessage trajectoryPoint = HumanoidMessageTools.createWrenchTrajectoryPointMessage(time0, torque0, force0);
      wrenchTrajectoryPoints.add().set(trajectoryPoint);

      trajectoryPoint.setTime(getDefinition().getTrajectoryDuration());
      wrenchTrajectoryPoints.add().set(trajectoryPoint);

      wrenchTrajectory.getFrameInformation().setTrajectoryReferenceFrameId(FrameInformation.CHEST_FRAME);
      wrenchTrajectory.setUseCustomControlFrame(true);
      wrenchTrajectory.getQueueingProperties().setExecutionMode(QueueableMessage.EXECUTION_MODE_OVERRIDE);

      double handCenterOffset = 0.05;
      wrenchTrajectory.getControlFramePose()
                      .setY(getDefinition().getSide() == RobotSide.RIGHT ? -handCenterOffset : handCenterOffset);

      ros2ControllerHelper.publishToController(handWrenchTrajectoryMessage);
   }
}
