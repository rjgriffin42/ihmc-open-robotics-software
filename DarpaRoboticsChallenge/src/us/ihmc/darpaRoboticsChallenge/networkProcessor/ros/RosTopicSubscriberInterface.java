package us.ihmc.darpaRoboticsChallenge.networkProcessor.ros;

import org.ros.message.MessageListener;

public interface RosTopicSubscriberInterface<T> extends MessageListener<T>
{

   public abstract String getMessageType();

   public abstract void connected();

}