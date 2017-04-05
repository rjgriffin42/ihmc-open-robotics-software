package us.ihmc.commonWalkingControlModules.controlModules.rigidBody;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.InverseDynamicsCommand;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.communication.controllerAPI.command.QueueableCommand;
import us.ihmc.communication.packets.Packet;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphic;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicCoordinateSystem;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicPosition;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicReferenceFrame;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicVector;
import us.ihmc.humanoidRobotics.communication.packets.ExecutionMode;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.LongYoVariable;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.FinishableState;

public abstract class RigidBodyControlState extends FinishableState<RigidBodyControlMode>
{
   protected final YoVariableRegistry registry;
   protected final String warningPrefix;

   protected final BooleanYoVariable trajectoryStopped;
   protected final BooleanYoVariable trajectoryDone;

   private final LongYoVariable lastCommandId;
   private final DoubleYoVariable trajectoryStartTime;
   private final DoubleYoVariable yoTime;

   protected final ArrayList<YoGraphic> graphics = new ArrayList<>();

   public RigidBodyControlState(RigidBodyControlMode stateEnum, String bodyName, DoubleYoVariable yoTime, YoVariableRegistry parentRegistry)
   {
      super(stateEnum);
      this.yoTime = yoTime;

      String prefix = bodyName + StringUtils.capitalize(stateEnum.toString().toLowerCase());
      warningPrefix = getClass().getSimpleName() + " for " + bodyName + ": ";
      registry = new YoVariableRegistry(prefix + "ControlModule");
      lastCommandId = new LongYoVariable(prefix + "LastCommandId", registry);
      lastCommandId.set(Packet.INVALID_MESSAGE_ID);

      trajectoryStopped = new BooleanYoVariable(prefix + "TrajectoryStopped", registry);
      trajectoryDone = new BooleanYoVariable(prefix + "TrajectoryDone", registry);
      trajectoryStartTime = new DoubleYoVariable(prefix + "TrajectoryStartTime", registry);

      parentRegistry.addChild(registry);
   }

   protected boolean handleCommandInternal(Command<?, ?> command)
   {
      if (command instanceof QueueableCommand<?, ?>)
      {
         QueueableCommand<?, ?> queueableCommand = (QueueableCommand<?, ?>) command;

         if (queueableCommand.getCommandId() == Packet.INVALID_MESSAGE_ID)
         {
            PrintTools.warn(warningPrefix + "Recieved packet with invalid ID.");
            return false;
         }

         boolean wantToQueue = queueableCommand.getExecutionMode() == ExecutionMode.QUEUE;
         boolean previousIdMatch = queueableCommand.getPreviousCommandId() == lastCommandId.getLongValue();

         if (!isEmpty() && wantToQueue && !previousIdMatch)
         {
            PrintTools.warn(warningPrefix + "Unexpected command ID.");
            return false;
         }

         if (!wantToQueue || isEmpty())
            trajectoryStartTime.set(yoTime.getDoubleValue());
         else
            queueableCommand.addTimeOffset(getLastTrajectoryPointTime());

         lastCommandId.set(queueableCommand.getCommandId());
      }
      else
      {
         trajectoryStartTime.set(yoTime.getDoubleValue());
      }

      trajectoryStopped.set(false);
      trajectoryDone.set(false);
      return true;
   }

   protected double getTimeInTrajectory()
   {
      return yoTime.getDoubleValue() - trajectoryStartTime.getDoubleValue();
   }

   protected void resetLastCommandId()
   {
      lastCommandId.set(Packet.INVALID_MESSAGE_ID);
   }

   public boolean abortState()
   {
      return false;
   }

   public abstract InverseDynamicsCommand<?> getInverseDynamicsCommand();

   public abstract FeedbackControlCommand<?> getFeedbackControlCommand();

   public abstract boolean isEmpty();

   public abstract double getLastTrajectoryPointTime();

   @Override
   public boolean isDone()
   {
      return true;
   }

   public InverseDynamicsCommand<?> getTransitionOutOfStateCommand()
   {
      return null;
   }

   protected void updateGraphics()
   {
      for (int graphicsIdx = 0; graphicsIdx < graphics.size(); graphicsIdx++)
         graphics.get(graphicsIdx).update();
   }

   protected void hideGraphics()
   {
      for (int graphicsIdx = 0; graphicsIdx < graphics.size(); graphicsIdx++)
      {
         YoGraphic yoGraphic = graphics.get(graphicsIdx);
         if (yoGraphic instanceof YoGraphicReferenceFrame)
            ((YoGraphicReferenceFrame) yoGraphic).hide();
         else if (yoGraphic instanceof YoGraphicPosition)
            ((YoGraphicPosition) yoGraphic).setPositionToNaN();
         else if (yoGraphic instanceof YoGraphicVector)
            ((YoGraphicVector) yoGraphic).hide();
         else if (yoGraphic instanceof YoGraphicCoordinateSystem)
            ((YoGraphicCoordinateSystem) yoGraphic).hide();
         else
            throw new RuntimeException("Implement hiding this.");
      }
   }
}
