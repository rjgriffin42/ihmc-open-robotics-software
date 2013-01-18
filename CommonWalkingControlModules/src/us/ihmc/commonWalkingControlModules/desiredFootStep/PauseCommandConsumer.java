package us.ihmc.commonWalkingControlModules.desiredFootStep;

import us.ihmc.utilities.io.streamingData.AbstractStreamingDataConsumer;

/**
 * User: Matt
 * Date: 1/18/13
 */
public class PauseCommandConsumer extends AbstractStreamingDataConsumer<Boolean>
{
   private FootstepPathCoordinator footstepPathCoordinator;

   public PauseCommandConsumer(long dataIdentifier, FootstepPathCoordinator footstepPathCoordinator)
   {
      super(dataIdentifier, Boolean.class);
      this.footstepPathCoordinator = footstepPathCoordinator;
   }

   protected void processPacket(Boolean packet)
   {
      Boolean isPaused = (Boolean) packet;
      footstepPathCoordinator.setPaused(isPaused);
   }
}
