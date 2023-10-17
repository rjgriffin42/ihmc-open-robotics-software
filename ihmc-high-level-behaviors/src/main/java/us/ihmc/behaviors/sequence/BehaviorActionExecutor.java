package us.ihmc.behaviors.sequence;

import behavior_msgs.msg.dds.ActionExecutionStatusMessage;

/**
 * Base template for a robot action, like a hand pose or a walk goal.
 */
public abstract class BehaviorActionExecutor implements BehaviorActionStateSupplier
{
   private final OldBehaviorActionSequence sequence;

   public BehaviorActionExecutor(OldBehaviorActionSequence sequence)
   {
      this.sequence = sequence;
   }

   /** Called every tick. */
   public void update()
   {
      getState().update();
   }

   /** Trigger the action to begin executing. Called once per execution. */
   public void triggerActionExecution()
   {

   }

   /** Called every tick only when this action is executing. */
   public void updateCurrentlyExecuting()
   {

   }

   /** Should return a precalculated value from {@link #updateCurrentlyExecuting} */
   public ActionExecutionStatusMessage getExecutionStatusMessage()
   {
      return new ActionExecutionStatusMessage();
   }

   public OldBehaviorActionSequence getSequence()
   {
      return sequence;
   }
}
