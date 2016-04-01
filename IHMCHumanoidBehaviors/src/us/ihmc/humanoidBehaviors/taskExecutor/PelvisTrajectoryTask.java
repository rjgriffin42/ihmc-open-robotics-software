package us.ihmc.humanoidBehaviors.taskExecutor;

import us.ihmc.humanoidBehaviors.behaviors.primitives.PelvisTrajectoryBehavior;
import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisTrajectoryMessage;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;

public class PelvisTrajectoryTask extends BehaviorTask
{
   private final PelvisTrajectoryMessage pelvisTrajectoryMessage;
   private final PelvisTrajectoryBehavior pelvisTrajectoryBehavior;

   public PelvisTrajectoryTask(PelvisTrajectoryMessage pelvisTrajectoryMessage, DoubleYoVariable yoTime, PelvisTrajectoryBehavior pelvisTrajectoryBehavior)
   {
      this(pelvisTrajectoryMessage, yoTime, pelvisTrajectoryBehavior, 0.0);
   }

   public PelvisTrajectoryTask(PelvisTrajectoryMessage pelvisTrajectoryMessage, DoubleYoVariable yoTime, PelvisTrajectoryBehavior pelvisTrajectoryBehavior, double sleepTime)
   {
      super(pelvisTrajectoryBehavior, yoTime, sleepTime);
      this.pelvisTrajectoryBehavior = pelvisTrajectoryBehavior;
      this.pelvisTrajectoryMessage = pelvisTrajectoryMessage;
   }

   @Override
   protected void setBehaviorInput()
   {
      pelvisTrajectoryBehavior.setInput(pelvisTrajectoryMessage); 
   }
}
