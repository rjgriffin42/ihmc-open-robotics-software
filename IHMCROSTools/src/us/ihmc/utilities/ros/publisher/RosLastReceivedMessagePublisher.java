package us.ihmc.utilities.ros.publisher;

import ihmc_msgs.LastReceivedMessage;
import us.ihmc.tools.time.TimeTools;

public class RosLastReceivedMessagePublisher extends RosTopicPublisher<LastReceivedMessage>
{

   public RosLastReceivedMessagePublisher(boolean latched)
   {
      super(LastReceivedMessage._TYPE, latched);
   }
   
   public void publish(String messageType, long uid, long robotTimestamp, long lastReceivedTimestamp)
   {
      ihmc_msgs.LastReceivedMessage message = getMessage();
      message.setType(messageType);
      message.setUniqueId(uid);
      message.setReceiveTimestamp(lastReceivedTimestamp);
      message.setTimeSinceLastReceived(TimeTools.nanoSecondstoSeconds(robotTimestamp - lastReceivedTimestamp));
      publish(message);
   }

}
