package us.ihmc.darpaRoboticsChallenge.networkProcessor.time;

import org.ros.message.Time;

import us.ihmc.utilities.ros.RosClockPublisher;
import us.ihmc.utilities.ros.RosMainNode;

public class SimulationRosClockPPSTimestampOffsetProvider implements
      PPSTimestampOffsetProvider {
   
   private RosClockPublisher clockPubisher;
   private long previousTimestamp = 0;
   
   public SimulationRosClockPPSTimestampOffsetProvider()
   {
      clockPubisher = new RosClockPublisher();
   }
   
   public long getCurrentTimestampOffset() {
      return 0;
   }

   public long requestNewestRobotTimestamp() {
      return 0;
   }

   public long adjustTimeStampToRobotClock(long timeStamp) {
      return timeStamp;
   }

   public void attachToRosMainNode(RosMainNode rosMainNode)
   {
      rosMainNode.attachPublisher("/clock", clockPubisher);
   }

   public boolean offsetIsDetermined() {
      return true;
   }

   @Override
   public long adjustRobotTimeStampToRosClock(long timeStamp)
   {
      publishRosClock(timeStamp);
      return timeStamp;
   }

   public void publishRosClock(long timestamp)
   {
      if (timestamp <= previousTimestamp) return; // Do not set timestamps from the past, screws up ROS.
      
      Time time = Time.fromNano(timestamp);
      clockPubisher.publish(time);
      previousTimestamp = timestamp;
   }
}
