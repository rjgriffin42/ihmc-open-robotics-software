package us.ihmc.commonWalkingControlModules.desiredFootStep;

import us.ihmc.commonWalkingControlModules.desiredFootStep.dataObjects.PauseCommand;
import us.ihmc.utilities.net.ObjectConsumer;

/**
 * User: Matt
 * Date: 1/18/13
 */
public class PauseCommandConsumer implements ObjectConsumer<PauseCommand>
{
   private FootstepPathCoordinator footstepPathCoordinator;

   public PauseCommandConsumer(FootstepPathCoordinator footstepPathCoordinator)
   {
      this.footstepPathCoordinator = footstepPathCoordinator;
   }

   public void consumeObject(PauseCommand object)
   {
      footstepPathCoordinator.setPaused(object.isPaused());
   }
}
