package us.ihmc.humanoidBehaviors.behaviors.primitives;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HandConfiguration;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.HandDesiredConfigurationMessage;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.tools.io.printing.PrintTools;

public class HandDesiredConfigurationBehavior extends BehaviorInterface
{
   private HandDesiredConfigurationMessage outgoingHandDesiredConfigurationMessage;
   private final BooleanYoVariable hasInputBeenSet;
   private final BooleanYoVariable hasPacketBeenSet;

   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable startTime;
   private final DoubleYoVariable trajectoryTime; // hardcoded, to be determined
   private final BooleanYoVariable trajectoryTimeElapsed;

   private final boolean DEBUG = true;

   public HandDesiredConfigurationBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridgeInterface, DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridgeInterface);
      this.yoTime = yoTime;

      hasInputBeenSet = new BooleanYoVariable(getName() + "hasInputBeenSet", registry);
      hasPacketBeenSet = new BooleanYoVariable(getName() + "hasPacketBeenSet", registry);

      startTime = new DoubleYoVariable(getName() + "StartTime", registry);
      startTime.set(Double.NaN);
      trajectoryTime = new DoubleYoVariable(getName() + "TrajectoryTime", registry);
      trajectoryTime.set(Double.NaN);

      trajectoryTimeElapsed = new BooleanYoVariable(getName() + "TrajectoryTimeElapsed", registry);
   }

   public void setInput(HandDesiredConfigurationMessage handDesiredConfigurationMessage)
   {
      this.outgoingHandDesiredConfigurationMessage = handDesiredConfigurationMessage;
      hasInputBeenSet.set(true);

      if (DEBUG)
         PrintTools.debug(this, "Input has been set: " + outgoingHandDesiredConfigurationMessage);
   }

   @Override
   public void doControl()
   {
      if (!hasPacketBeenSet.getBooleanValue() && outgoingHandDesiredConfigurationMessage != null)
         sendHandDesiredConfigurationToController();
   }

   private void sendHandDesiredConfigurationToController()
   {
      if (!isPaused.getBooleanValue() && !isStopped.getBooleanValue())
      {

//         sendPacketToController(outgoingHandDesiredConfigurationMessage);
         
         outgoingHandDesiredConfigurationMessage.setDestination(PacketDestination.BROADCAST);
         
         sendPacketToNetworkProcessor(outgoingHandDesiredConfigurationMessage);
         hasPacketBeenSet.set(true);
         startTime.set(yoTime.getDoubleValue());

         if (DEBUG)
            PrintTools.debug(this, "Sending HandDesiredConfigurationMessage to Controller: " + outgoingHandDesiredConfigurationMessage);
      }
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
   }

   @Override
   public void stop()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         HandDesiredConfigurationMessage stopMessage = new HandDesiredConfigurationMessage(robotSide, HandConfiguration.STOP);
         stopMessage.setDestination(PacketDestination.UI);
         sendPacketToController(stopMessage);
         sendPacketToNetworkProcessor(stopMessage);
      }
      isStopped.set(true);
   }

   @Override
   public void enableActions()
   {
   }

   @Override
   public void pause()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         HandDesiredConfigurationMessage stopMessage = new HandDesiredConfigurationMessage(robotSide, HandConfiguration.STOP);
         stopMessage.setDestination(PacketDestination.UI);
         sendPacketToController(stopMessage);
         sendPacketToNetworkProcessor(stopMessage);
      }
      isPaused.set(true);

      if (DEBUG)
         PrintTools.debug(this, "Pausing Behavior");
   }

   @Override
   public void resume()
   {
      isPaused.set(false);
      hasPacketBeenSet.set(false);
      if (hasInputBeenSet())
      {
         sendHandDesiredConfigurationToController();
      }
      if (DEBUG)
         PrintTools.debug(this, "Resuming Behavior");
   }

   @Override
   public boolean isDone()
   {
      if (Double.isNaN(startTime.getDoubleValue()) || Double.isNaN(trajectoryTime.getDoubleValue()))
         trajectoryTimeElapsed.set(false);
      else
         trajectoryTimeElapsed.set(yoTime.getDoubleValue() - startTime.getDoubleValue() > trajectoryTime.getDoubleValue());

      return trajectoryTimeElapsed.getBooleanValue() && !isPaused.getBooleanValue() && !isStopped.getBooleanValue();
   }

   @Override
   public void initialize()
   {
      if (hasInputBeenSet())
      {
         PrintTools.debug(this, "Re-Initializing Behavior");
      }
      hasInputBeenSet.set(false);
      hasPacketBeenSet.set(false);

      isPaused.set(false);
      isStopped.set(false);
      trajectoryTime.set(1.0); //TODO hardCoded to be determined

      trajectoryTimeElapsed.set(false);
   }

   @Override
   public void doPostBehaviorCleanup()
   {
      hasInputBeenSet.set(false);
      hasPacketBeenSet.set(false);
      outgoingHandDesiredConfigurationMessage = null;
      isPaused.set(false);
      isStopped.set(false);

      trajectoryTime.set(Double.NaN);
      startTime.set(Double.NaN);
      trajectoryTimeElapsed.set(false);
   }

   @Override
   public boolean hasInputBeenSet()
   {
      return hasInputBeenSet.getBooleanValue();
   }
}
