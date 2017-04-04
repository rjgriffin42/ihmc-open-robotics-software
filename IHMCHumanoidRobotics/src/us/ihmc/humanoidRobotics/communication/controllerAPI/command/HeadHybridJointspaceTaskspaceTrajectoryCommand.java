package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import us.ihmc.communication.controllerAPI.command.QueueableCommand;
import us.ihmc.humanoidRobotics.communication.controllerAPI.converter.FrameBasedCommand;
import us.ihmc.humanoidRobotics.communication.packets.walking.hybridRigidBodyManager.HeadHybridJointspaceTaskspaceMessage;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;

public class HeadHybridJointspaceTaskspaceTrajectoryCommand extends QueueableCommand<HeadHybridJointspaceTaskspaceTrajectoryCommand, HeadHybridJointspaceTaskspaceMessage>  implements FrameBasedCommand<HeadHybridJointspaceTaskspaceMessage>
{
   private final NeckTrajectoryCommand jointspaceTrajectoryCommand = new NeckTrajectoryCommand();
   private final HeadTrajectoryCommand taskspaceTrajectoryCommand = new HeadTrajectoryCommand();
   
   public HeadHybridJointspaceTaskspaceTrajectoryCommand()
   {
   }
   
   public HeadHybridJointspaceTaskspaceTrajectoryCommand(HeadTrajectoryCommand taskspaceTrajectoryCommand, NeckTrajectoryCommand jointspaceTrajectoryCommand)
   {
      super();
      this.jointspaceTrajectoryCommand.set(jointspaceTrajectoryCommand);
      this.taskspaceTrajectoryCommand.set(taskspaceTrajectoryCommand);
   }

   @Override
   public Class<HeadHybridJointspaceTaskspaceMessage> getMessageClass()
   {
      return HeadHybridJointspaceTaskspaceMessage.class;
   }

   @Override
   public void clear()
   {
      jointspaceTrajectoryCommand.clear();
      taskspaceTrajectoryCommand.clear();
   }

   @Override
   public void set(HeadHybridJointspaceTaskspaceMessage message)
   {
      jointspaceTrajectoryCommand.set(message.getNeckTrajectoryMessage());
      taskspaceTrajectoryCommand.set(message.getHeadTrajectoryMessage());
   }
   
   @Override
   public void set(ReferenceFrame dataFrame, ReferenceFrame trajectoryFrame, HeadHybridJointspaceTaskspaceMessage message)
   {
      jointspaceTrajectoryCommand.set(message.getNeckTrajectoryMessage());
      taskspaceTrajectoryCommand.set(dataFrame, trajectoryFrame, message.getHeadTrajectoryMessage());
   }

   @Override
   public boolean isCommandValid()
   {
      return jointspaceTrajectoryCommand.isCommandValid() && taskspaceTrajectoryCommand.isCommandValid();
   }

   @Override
   public void set(HeadHybridJointspaceTaskspaceTrajectoryCommand other)
   {
      taskspaceTrajectoryCommand.set(other.getTaskspaceTrajectoryCommand());
      jointspaceTrajectoryCommand.set(other.getJointspaceTrajectoryCommand());
   }

   @Override
   public void addTimeOffset(double timeOffset)
   {
      taskspaceTrajectoryCommand.addTimeOffset(timeOffset);
      jointspaceTrajectoryCommand.addTimeOffset(timeOffset);
   }

   public NeckTrajectoryCommand getJointspaceTrajectoryCommand()
   {
      return jointspaceTrajectoryCommand;
   }

   public HeadTrajectoryCommand getTaskspaceTrajectoryCommand()
   {
      return taskspaceTrajectoryCommand;
   }
}
