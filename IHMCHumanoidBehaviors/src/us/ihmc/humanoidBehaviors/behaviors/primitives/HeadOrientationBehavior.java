package us.ihmc.humanoidBehaviors.behaviors.primitives;

import us.ihmc.communication.packets.walking.HeadOrientationPacket;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class HeadOrientationBehavior extends BehaviorInterface
{
   private final BooleanYoVariable packetHasBeenSent = new BooleanYoVariable("packetHasBeenSent" + behaviorName, registry);
   private HeadOrientationPacket outgoingHeadOrientationPacket;

   private final DoubleYoVariable yoTime;
   private double startTime = Double.NaN;
   private double currentTime = Double.NaN;

   // TODO: This is just an estimate of the time out. This needs to be dealt with
   private double behaviorTime = 4.0;

   public HeadOrientationBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge, DoubleYoVariable yoTime)
   {
      super(outgoingCommunicationBridge);

      this.yoTime = yoTime;
   }

   public void setInput(HeadOrientationPacket headOrientationPacket)
   {
      this.outgoingHeadOrientationPacket = headOrientationPacket;
   }

   @Override
   public void doControl()
   {
      if (!packetHasBeenSent.getBooleanValue() && (outgoingHeadOrientationPacket != null))
      {
         sendHandPoseToController();
      }

      currentTime = yoTime.getDoubleValue();
   }

   private void sendHandPoseToController()
   {
      if (!isPaused.getBooleanValue() &&!isStopped.getBooleanValue())
      {
         sendPacketToController(outgoingHeadOrientationPacket);
         packetHasBeenSent.set(true);
         startTime = yoTime.getDoubleValue();
      }
   }

   @Override
   public void initialize()
   {
   }

   @Override
   public void finalize()
   {
      packetHasBeenSent.set(false);
      outgoingHeadOrientationPacket = null;

      isPaused.set(false);
      isStopped.set(false);
   }

   @Override
   public void stop()
   {
      isStopped.set(true);
   }

   @Override
   public void pause()
   {
      isPaused.set(true);
   }

   @Override
   public void resume()
   {
      isPaused.set(false);
   }

   @Override
   public boolean isDone()
   {
      boolean trajectoryTimeElapsed = currentTime - startTime > behaviorTime;

      return trajectoryTimeElapsed &&!isPaused.getBooleanValue();
   }

   @Override
   public void enableActions()
   {
   }

   @Override
   protected void passReceivedNetworkProcessorObjectToChildBehaviors(Object object)
   {
   }

   @Override
   protected void passReceivedControllerObjectToChildBehaviors(Object object)
   {
   }
}
