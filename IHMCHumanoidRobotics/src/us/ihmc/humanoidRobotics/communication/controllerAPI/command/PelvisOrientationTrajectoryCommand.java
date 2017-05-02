package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import us.ihmc.humanoidRobotics.communication.packets.walking.PelvisOrientationTrajectoryMessage;
import us.ihmc.sensorProcessing.frames.ReferenceFrameHashCodeResolver;

public class PelvisOrientationTrajectoryCommand extends SO3TrajectoryControllerCommand<PelvisOrientationTrajectoryCommand, PelvisOrientationTrajectoryMessage>
{
   private boolean enableUserPelvisControlDuringWalking = false;

   @Override
   public void set(PelvisOrientationTrajectoryCommand other)
   {
      setEnableUserPelvisControlDuringWalking(other.isEnableUserPelvisControlDuringWalking());
      super.set(other);
   }

   @Override
   public void set(ReferenceFrameHashCodeResolver resolver, PelvisOrientationTrajectoryMessage message)
   {
      setEnableUserPelvisControlDuringWalking(message.isEnableUserPelvisControlDuringWalking());
      super.set(resolver, message);
   }

   @Override
   public void set(PelvisOrientationTrajectoryMessage message)
   {
      setEnableUserPelvisControlDuringWalking(message.isEnableUserPelvisControlDuringWalking());
      super.set(message);
   }

   public boolean isEnableUserPelvisControlDuringWalking()
   {
      return enableUserPelvisControlDuringWalking;
   }

   public void setEnableUserPelvisControlDuringWalking(boolean enableUserPelvisControlDuringWalking)
   {
      this.enableUserPelvisControlDuringWalking = enableUserPelvisControlDuringWalking;
   }

   public PelvisOrientationTrajectoryCommand()
   {
   }

   @Override
   public Class<PelvisOrientationTrajectoryMessage> getMessageClass()
   {
      return PelvisOrientationTrajectoryMessage.class;
   }
}
