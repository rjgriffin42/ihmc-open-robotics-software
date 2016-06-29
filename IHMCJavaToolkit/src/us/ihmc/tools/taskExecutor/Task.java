package us.ihmc.tools.taskExecutor;

public interface Task
{
   public abstract void doTransitionIntoAction();

   public abstract void doAction();

   public abstract void doTransitionOutOfAction();

   public abstract boolean isDone();
}
