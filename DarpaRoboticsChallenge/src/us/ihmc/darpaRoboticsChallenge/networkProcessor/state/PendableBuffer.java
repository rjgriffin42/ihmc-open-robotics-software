package us.ihmc.darpaRoboticsChallenge.networkProcessor.state;

public interface PendableBuffer
{
   public boolean isPending(long timestamp);
}
