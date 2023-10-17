package us.ihmc.behaviors.behaviorTree;

import behavior_msgs.msg.dds.BehaviorTreeNodeStateMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * The core interface of a Behavior Tree: the node that can be ticked.
 */
public abstract class BehaviorTreeNodeState implements BehaviorTreeNodeDefinitionSupplier
{
   /** The current status of the behavior tree node. */
   private BehaviorTreeNodeStatus status = BehaviorTreeNodeStatus.NOT_TICKED;

   private final List<BehaviorTreeNodeState> children = new ArrayList<>();


   public void toMessage(BehaviorTreeNodeStateMessage message)
   {
      message.setStatus(status.toByte());
   }

   public void fromMessage(BehaviorTreeNodeStateMessage message)
   {
      status = BehaviorTreeNodeStatus.fromByte(message.getStatus());
   }

   public void setStatus(BehaviorTreeNodeStatus status)
   {
      this.status = status;
   }

   /**
    * @return The node's status from the last time it was ticked.
    *         This will be null if the node hasn't been ticked yet.
    */
   public BehaviorTreeNodeStatus getStatus()
   {
      return status;
   }

   public List<BehaviorTreeNodeState> getChildren()
   {
      return children;
   }
}
