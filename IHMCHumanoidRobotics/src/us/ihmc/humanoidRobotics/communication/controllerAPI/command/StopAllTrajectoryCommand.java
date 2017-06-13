package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.StopAllTrajectoryMessage;

public class StopAllTrajectoryCommand implements Command<StopAllTrajectoryCommand, StopAllTrajectoryMessage>
{
   private boolean stopAllTrajectory = false;
   
   /** the time to delay this command on the controller side before being executed **/
   private double executionDelayTime;
   /** the execution time. This number is set if the execution delay is non zero**/
   public double adjustedExecutionTime;

   @Override
   public void clear()
   {
      stopAllTrajectory = false;
   }

   @Override
   public void set(StopAllTrajectoryCommand other)
   {
      stopAllTrajectory = other.stopAllTrajectory;
      executionDelayTime = other.executionDelayTime;
   }

   @Override
   public void set(StopAllTrajectoryMessage message)
   {
      stopAllTrajectory = true;
   }

   public boolean isStopAllTrajectory()
   {
      return stopAllTrajectory;
   }

   public void setStopAllTrajectory(boolean stopAllTrajectory)
   {
      this.stopAllTrajectory = stopAllTrajectory;
   }

   @Override
   public Class<StopAllTrajectoryMessage> getMessageClass()
   {
      return StopAllTrajectoryMessage.class;
   }

   @Override
   public boolean isCommandValid()
   {
      return true;
   }

   /**
    * returns the amount of time this command is delayed on the controller side before executing
    * @return the time to delay this command in seconds
    */
   @Override
   public double getExecutionDelayTime()
   {
      return executionDelayTime;
   }
   
   /**
    * sets the amount of time this command is delayed on the controller side before executing
    * @param delayTime the time in seconds to delay after receiving the command before executing
    */
   @Override
   public void setExecutionDelayTime(double delayTime)
   {
      this.executionDelayTime = delayTime;
   }
   
   /**
    * returns the expected execution time of this command. The execution time will be computed when the controller 
    * receives the command using the controllers time plus the execution delay time.
    * This is used when {@code getExecutionDelayTime} is non-zero
    */
   @Override
   public double getExecutionTime()
   {
      return adjustedExecutionTime;
   }

   /**
    * sets the execution time for this command. This is called by the controller when the command is received.
    */
   @Override
   public void setExecutionTime(double adjustedExecutionTime)
   {
      this.adjustedExecutionTime = adjustedExecutionTime;
   }

}
