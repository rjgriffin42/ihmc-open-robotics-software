package us.ihmc.humanoidBehaviors.behaviors.primitives;

import org.apache.commons.lang3.StringUtils;

import us.ihmc.communication.packets.PacketDestination;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.humanoidRobotics.communication.packets.walking.ChestTrajectoryMessage;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.tools.io.printing.PrintTools;

public class ChestTrajectoryBehavior extends BehaviorInterface
{
   private static final boolean DEBUG = false;
   
   private ChestTrajectoryMessage outgoingChestTrajectoryMessage;

   private final BooleanYoVariable hasPacketBeenSent;
   private final DoubleYoVariable yoTime;
   private final DoubleYoVariable startTime;
   private final DoubleYoVariable trajectoryTime;
   private final BooleanYoVariable trajectoryTimeHasElapsed;

   public ChestTrajectoryBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridge);

      this.yoTime = yoTime;
      String behaviorNameFirstLowerCase = StringUtils.uncapitalize(getName());
      hasPacketBeenSent = new BooleanYoVariable(behaviorNameFirstLowerCase + "HasPacketBeenSent", registry);
      startTime = new DoubleYoVariable(behaviorNameFirstLowerCase + "StartTime", registry);
      startTime.set(Double.NaN);
      trajectoryTime = new DoubleYoVariable(behaviorNameFirstLowerCase + "TrajectoryTime", registry);
      trajectoryTime.set(Double.NaN);
      trajectoryTimeHasElapsed = new BooleanYoVariable(behaviorNameFirstLowerCase + "TrajectoryTimeHasElapsed", registry);
   }

   public void setInput(ChestTrajectoryMessage chestOrientationPacket)
   {
      this.outgoingChestTrajectoryMessage = chestOrientationPacket;
   }

   @Override
   public void doControl()
   {
      if (!hasPacketBeenSent.getBooleanValue() && (outgoingChestTrajectoryMessage != null))
      {
         sendChestPoseToController();
      }
   }

   private void sendChestPoseToController()
   {
      if (!isPaused.getBooleanValue() && !isStopped.getBooleanValue())
      {
         outgoingChestTrajectoryMessage.setDestination(PacketDestination.UI);
         sendPacketToNetworkProcessor(outgoingChestTrajectoryMessage);
         sendPacketToController(outgoingChestTrajectoryMessage);
         hasPacketBeenSent.set(true);
         startTime.set(yoTime.getDoubleValue());
         trajectoryTime.set(outgoingChestTrajectoryMessage.getTrajectoryTime());
      }
   }

   @Override
   public void initialize()
   {
      hasPacketBeenSent.set(false);
      
      hasBeenInitialized.set(true);
      
      isPaused.set(false);
      isStopped.set(false);
   }

   @Override
   public void doPostBehaviorCleanup()
   {
      hasPacketBeenSent.set(false);
      outgoingChestTrajectoryMessage = null;

      isPaused.set(false);
      isStopped.set(false);

      startTime.set(Double.NaN);
      trajectoryTime.set(Double.NaN);
   }

   @Override
   public void stop()
   {
      isStopped.set(true);
   }

   @Override
   public void pause()
   {
      isPaused.set(true);
   }

   @Override
   public void resume()
   {
      isPaused.set(false);
   }

   @Override
   public boolean isDone()
   {
      boolean startTimeUndefined = Double.isNaN(startTime.getDoubleValue());
      boolean trajectoryTimeUndefined = Double.isNaN(trajectoryTime.getDoubleValue());
      double trajectoryTimeElapsed = yoTime.getDoubleValue() - startTime.getDoubleValue();

      if (DEBUG)
      {
         PrintTools.debug(this, "StartTimeUndefined: " + startTimeUndefined + ".  TrajectoryTimeUndefined: " + trajectoryTimeUndefined);
         PrintTools.debug(this, "TrajectoryTimeElapsed: " + trajectoryTimeElapsed);
      }

      if ( startTimeUndefined || trajectoryTimeUndefined )
         trajectoryTimeHasElapsed.set(false);
      else
         trajectoryTimeHasElapsed.set( trajectoryTimeElapsed > trajectoryTime.getDoubleValue());

      return trajectoryTimeHasElapsed.getBooleanValue() && !isPaused.getBooleanValue();
   }

   @Override
   public void enableActions()
   {
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
   public boolean hasInputBeenSet()
   {
      return outgoingChestTrajectoryMessage != null;
   }
}
