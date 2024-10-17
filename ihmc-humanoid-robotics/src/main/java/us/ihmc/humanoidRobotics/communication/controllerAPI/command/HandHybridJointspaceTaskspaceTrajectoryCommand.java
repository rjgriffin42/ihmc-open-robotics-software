package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import java.util.Random;

import controller_msgs.msg.dds.HandHybridJointspaceTaskspaceTrajectoryMessage;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.humanoidRobotics.communication.controllerAPI.converter.FrameBasedCommand;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.sensorProcessing.frames.ReferenceFrameHashCodeResolver;

public class HandHybridJointspaceTaskspaceTrajectoryCommand
      implements Command<HandHybridJointspaceTaskspaceTrajectoryCommand, HandHybridJointspaceTaskspaceTrajectoryMessage>,
      FrameBasedCommand<HandHybridJointspaceTaskspaceTrajectoryMessage>
{
   private long sequenceId;
   private RobotSide robotSide;
   private boolean forceExecution = false;
   private final JointspaceTrajectoryCommand jointspaceTrajectoryCommand = new JointspaceTrajectoryCommand();
   private final SE3TrajectoryControllerCommand taskspaceTrajectoryCommand = new SE3TrajectoryControllerCommand();
   private final WrenchTrajectoryControllerCommand feedForwardTrajectoryCommand = new WrenchTrajectoryControllerCommand();
   private final SE3PIDGainsTrajectoryControllerCommand pidGainsTrajectoryCommand = new SE3PIDGainsTrajectoryControllerCommand();

   public HandHybridJointspaceTaskspaceTrajectoryCommand()
   {
   }

   public HandHybridJointspaceTaskspaceTrajectoryCommand(RobotSide robotSide, boolean forceExecution, SE3TrajectoryControllerCommand taskspaceTrajectoryCommand,
                                                         JointspaceTrajectoryCommand jointspaceTrajectoryCommand)
   {
      this.robotSide = robotSide;
      this.setForceExecution(forceExecution);
      this.jointspaceTrajectoryCommand.set(jointspaceTrajectoryCommand);
      this.taskspaceTrajectoryCommand.set(taskspaceTrajectoryCommand);
   }

   public HandHybridJointspaceTaskspaceTrajectoryCommand(RobotSide robotSide, boolean forceExecution, SE3TrajectoryControllerCommand taskspaceTrajectoryCommand,
                                                         JointspaceTrajectoryCommand jointspaceTrajectoryCommand,
                                                         SE3PIDGainsTrajectoryControllerCommand pidGainsTrajectoryCommand)
   {
      this.robotSide = robotSide;
      this.setForceExecution(forceExecution);
      this.jointspaceTrajectoryCommand.set(jointspaceTrajectoryCommand);
      this.taskspaceTrajectoryCommand.set(taskspaceTrajectoryCommand);
      this.pidGainsTrajectoryCommand.set(pidGainsTrajectoryCommand);
   }

   public HandHybridJointspaceTaskspaceTrajectoryCommand(RobotSide robotSide, boolean forceExecution, SE3TrajectoryControllerCommand taskspaceTrajectoryCommand,
                                                         JointspaceTrajectoryCommand jointspaceTrajectoryCommand,
                                                         WrenchTrajectoryControllerCommand feedForwardTrajectoryCommand)
   {
      this.robotSide = robotSide;
      this.setForceExecution(forceExecution);
      this.jointspaceTrajectoryCommand.set(jointspaceTrajectoryCommand);
      this.taskspaceTrajectoryCommand.set(taskspaceTrajectoryCommand);
      this.feedForwardTrajectoryCommand.set(feedForwardTrajectoryCommand);
   }

   public HandHybridJointspaceTaskspaceTrajectoryCommand(RobotSide robotSide, boolean forceExecution, SE3TrajectoryControllerCommand taskspaceTrajectoryCommand,
                                                         JointspaceTrajectoryCommand jointspaceTrajectoryCommand,
                                                         WrenchTrajectoryControllerCommand feedForwardTrajectoryCommand,
                                                         SE3PIDGainsTrajectoryControllerCommand pidGainsTrajectoryCommand)
   {
      this.robotSide = robotSide;
      this.setForceExecution(forceExecution);
      this.jointspaceTrajectoryCommand.set(jointspaceTrajectoryCommand);
      this.taskspaceTrajectoryCommand.set(taskspaceTrajectoryCommand);
      this.feedForwardTrajectoryCommand.set(feedForwardTrajectoryCommand);
      this.pidGainsTrajectoryCommand.set(pidGainsTrajectoryCommand);
   }

   public HandHybridJointspaceTaskspaceTrajectoryCommand(Random random)
   {
      this(RobotSide.generateRandomRobotSide(random), random.nextBoolean(), new SE3TrajectoryControllerCommand(random),
           new JointspaceTrajectoryCommand(random));
   }

   @Override
   public Class<HandHybridJointspaceTaskspaceTrajectoryMessage> getMessageClass()
   {
      return HandHybridJointspaceTaskspaceTrajectoryMessage.class;
   }

   @Override
   public void clear()
   {
      sequenceId = 0;
      robotSide = null;
      setForceExecution(false);
      jointspaceTrajectoryCommand.clear();
      taskspaceTrajectoryCommand.clear();
      feedForwardTrajectoryCommand.clear();
      pidGainsTrajectoryCommand.clear();
   }

   @Override
   public void setFromMessage(HandHybridJointspaceTaskspaceTrajectoryMessage message)
   {
      FrameBasedCommand.super.setFromMessage(message);
   }

   @Override
   public void set(ReferenceFrameHashCodeResolver resolver, HandHybridJointspaceTaskspaceTrajectoryMessage message)
   {
      sequenceId = message.getSequenceId();
      robotSide = RobotSide.fromByte(message.getRobotSide());
      setForceExecution(message.getForceExecution());
      jointspaceTrajectoryCommand.setFromMessage(message.getJointspaceTrajectoryMessage());
      taskspaceTrajectoryCommand.set(resolver, message.getTaskspaceTrajectoryMessage());
      feedForwardTrajectoryCommand.set(resolver, message.getFeedforwardTaskspaceTrajectoryMessage());
      pidGainsTrajectoryCommand.setFromMessage(message.getTaskspacePidGains());
   }

   @Override
   public boolean isCommandValid()
   {
      return robotSide != null && jointspaceTrajectoryCommand.isCommandValid() && taskspaceTrajectoryCommand.isCommandValid()
             && feedForwardTrajectoryCommand.isCommandValid() && pidGainsTrajectoryCommand.isCommandValid();
   }

   @Override
   public void set(HandHybridJointspaceTaskspaceTrajectoryCommand other)
   {
      sequenceId = other.sequenceId;
      robotSide = other.robotSide;
      setForceExecution(other.getForceExecution());
      taskspaceTrajectoryCommand.set(other.getTaskspaceTrajectoryCommand());
      jointspaceTrajectoryCommand.set(other.getJointspaceTrajectoryCommand());
      feedForwardTrajectoryCommand.set(other.getFeedForwardTrajectoryCommand());
      pidGainsTrajectoryCommand.set(other.getPIDGainsTrajectoryCommand());
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public boolean getForceExecution()
   {
      return forceExecution;
   }

   public void setForceExecution(boolean forceExecution)
   {
      this.forceExecution = forceExecution;
   }

   public JointspaceTrajectoryCommand getJointspaceTrajectoryCommand()
   {
      return jointspaceTrajectoryCommand;
   }

   public SE3TrajectoryControllerCommand getTaskspaceTrajectoryCommand()
   {
      return taskspaceTrajectoryCommand;
   }

   public WrenchTrajectoryControllerCommand getFeedForwardTrajectoryCommand()
   {
      return feedForwardTrajectoryCommand;
   }

   public SE3PIDGainsTrajectoryControllerCommand getPIDGainsTrajectoryCommand()
   {
      return pidGainsTrajectoryCommand;
   }

   @Override
   public boolean isDelayedExecutionSupported()
   {
      return true;
   }

   @Override
   public void setExecutionDelayTime(double delayTime)
   {
      taskspaceTrajectoryCommand.setExecutionDelayTime(delayTime);
   }

   @Override
   public void setExecutionTime(double adjustedExecutionTime)
   {
      taskspaceTrajectoryCommand.setExecutionTime(adjustedExecutionTime);
   }

   @Override
   public double getExecutionDelayTime()
   {
      return taskspaceTrajectoryCommand.getExecutionDelayTime();
   }

   @Override
   public double getExecutionTime()
   {
      return taskspaceTrajectoryCommand.getExecutionTime();
   }

   @Override
   public long getSequenceId()
   {
      return sequenceId;
   }
}
