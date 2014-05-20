package us.ihmc.darpaRoboticsChallenge.networkProcessor.time;


import us.ihmc.utilities.ros.RosMainNode;

public class AlwaysZeroOffsetPPSTimestampOffsetProvider implements PPSTimestampOffsetProvider
{
   public long getCurrentTimestampOffset()
   {
      return 0;
   }

   public long requestNewestRobotTimestamp()
   {
      return 0;
   }

   public long adjustTimeStampToRobotClock(long timeStamp)
   {
      return timeStamp;
   }

   public void attachToRosMainNode(RosMainNode rosMainNode)
   {
      return;
   }

   public boolean offsetIsDetermined()
   {
      return true;
   }
}
