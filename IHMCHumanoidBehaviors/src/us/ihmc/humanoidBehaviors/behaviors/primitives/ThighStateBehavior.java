package us.ihmc.humanoidBehaviors.behaviors.primitives;

import us.ihmc.communication.packets.walking.ThighStatePacket;
import us.ihmc.humanoidBehaviors.behaviors.BehaviorInterface;
import us.ihmc.humanoidBehaviors.communication.OutgoingCommunicationBridgeInterface;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;

public class ThighStateBehavior extends BehaviorInterface
{
   private final BooleanYoVariable packetHasBeenSent = new BooleanYoVariable("packetHasBeenSent" + behaviorName, registry);
   private ThighStatePacket outgoingThighStatePacket;

   public ThighStateBehavior(OutgoingCommunicationBridgeInterface outgoingCommunicationBridge)
   {
      super(outgoingCommunicationBridge);
   }

   public void setInput(ThighStatePacket thighStatePacket)
   {
      this.outgoingThighStatePacket = thighStatePacket;
   }

   @Override
   public void doControl()
   {
      if (!packetHasBeenSent.getBooleanValue() && (outgoingThighStatePacket != null))
      {
         sendFootStateToController();
      }
   }

   private void sendFootStateToController()
   {
      if (!isPaused.getBooleanValue() &&!isStopped.getBooleanValue())
      {
         sendPacketToController(outgoingThighStatePacket);
         packetHasBeenSent.set(true);
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
      outgoingThighStatePacket = null;

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
      return packetHasBeenSent.getBooleanValue() &&!isPaused.getBooleanValue();
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
