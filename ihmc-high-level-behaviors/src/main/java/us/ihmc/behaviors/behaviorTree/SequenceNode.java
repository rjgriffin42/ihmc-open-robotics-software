package us.ihmc.behaviors.behaviorTree;

import static us.ihmc.behaviors.behaviorTree.BehaviorTreeNodeStatus.*;

/**
 * A sequence node proceeds through children left to right while they are SUCCESSful.
 */
public class SequenceNode extends LocalOnlyBehaviorTreeNodeExecutor
{
   public SequenceNode()
   {

   }

   public BehaviorTreeNodeStatus tickInternal()
   {
      for (BehaviorTreeNodeExecutor child : getChildren())
      {
         BehaviorTreeNodeStatus childStatus = child.tick();

         if (childStatus == RUNNING)
         {
            return RUNNING;
         }
         else if (childStatus == FAILURE)
         {
            return FAILURE;
         }

         // SUCCESS, continue
      }

      return SUCCESS;
   }
}
