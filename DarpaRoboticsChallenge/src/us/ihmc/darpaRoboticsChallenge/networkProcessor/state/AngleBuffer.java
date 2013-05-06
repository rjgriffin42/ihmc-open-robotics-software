package us.ihmc.darpaRoboticsChallenge.networkProcessor.state;


public class AngleBuffer
{
   private final int size;
   
   private long[] timestamps;
   private double[] angles;
   
   private int currentIndex;
   private long oldestTimeStamp;
   private long newestTimestamp;
   
   public AngleBuffer(int size)
   {
      this.size = size;
      timestamps = new long[size];
      angles = new double[size];
      
      for(int i = 0; i < size; i++)
      {
         timestamps[i] = Long.MAX_VALUE;
      }
   }
   
   public void addAngle(long timestamp, double angle)
   {
      timestamps[currentIndex] = timestamp;
      angles[currentIndex] = angle;
      
      newestTimestamp = timestamp;
      currentIndex++;

      if (currentIndex >= size)
      {
         currentIndex = 0;
      }

      if (timestamps[currentIndex] == Long.MAX_VALUE)
      {
         oldestTimeStamp = newestTimestamp;
      }
      else
      {
         oldestTimeStamp = timestamps[currentIndex];
      }
   }
   
   public boolean isInRange(long timestamp)
   {
      return ((timestamp >= oldestTimeStamp) && (timestamp <= newestTimestamp));
   }
   
   public double interpolate(long timestamp)
   {
      if(!isInRange(timestamp))
      {
         return 0.0;
      }
      
      for (int i = currentIndex - 1; i >= -size + currentIndex; i--)
      {
         int index = (i < 0) ? size + i : i;

         if(timestamps[index] == timestamp)
         {
            return angles[index];
         }
         else if(timestamps[index] < timestamp)
         {
            long floor = timestamps[index];
            double floorAngle = angles[index];
            index++;

            if (index >= size)
            {
               index = 0;
            }
            
            long ceiling = timestamps[index];
            double ceilingAngle = angles[index];
            
            
            double percentage = ((double) (timestamp - floor)) / ((double) (ceiling - floor));
            
            return floorAngle + percentage * (ceilingAngle - floorAngle);   
         }
         
      }
      return 0.0;
   
   }
   
   public boolean isPending(long timestamp)
   {
      return timestamp > newestTimestamp;
   }
}
