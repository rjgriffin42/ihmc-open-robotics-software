package us.ihmc.commonWalkingControlModules.packetConsumers;

import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.communication.packets.LookAtPacket;
import us.ihmc.communication.packets.walking.HeadOrientationPacket;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.net.ObjectConsumer;

import com.google.common.util.concurrent.AtomicDouble;

public class DesiredHeadOrientationProvider implements HeadOrientationProvider
{
   private final AtomicReference<LookAtPacket> lookAtPacket = new AtomicReference<LookAtPacket>();
   private final AtomicReference<HeadOrientationPacket> headOrientationPacket = new AtomicReference<HeadOrientationPacket>();

   private final HeadOrientationPacketConsumer headOrientationPacketConsumer;
   private final LookAtPacketConsumer lookAtPacketConsumer;
   private final ReferenceFrame headOrientationFrame;

   private AtomicDouble desiredJointForExtendedNeckPitchRangeAngle = new AtomicDouble(0.0);
   private AtomicDouble desiredNeckPitchAngle = new AtomicDouble(0.0);

   private final ReferenceFrame lookAtFrame = ReferenceFrame.getWorldFrame();
   private final FramePoint pointToTrack = new FramePoint(lookAtFrame);
   private final FrameOrientation headOrientation;

   public DesiredHeadOrientationProvider(ReferenceFrame headOrientationFrame)
   {
      headOrientationPacketConsumer = new HeadOrientationPacketConsumer();
      lookAtPacketConsumer = new LookAtPacketConsumer();
      this.headOrientationFrame = headOrientationFrame;
      headOrientation = new FrameOrientation(headOrientationFrame);
   }

   public ObjectConsumer<HeadOrientationPacket> getHeadOrientationPacketConsumer()
   {
      return headOrientationPacketConsumer;
   }

   public ObjectConsumer<LookAtPacket> getLookAtPacketConsumer()
   {
      return lookAtPacketConsumer;
   }

   private class LookAtPacketConsumer implements ObjectConsumer<LookAtPacket>
   {
      public void consumeObject(LookAtPacket object)
      {
         lookAtPacket.set(object);
      }
   }

   private class HeadOrientationPacketConsumer implements ObjectConsumer<HeadOrientationPacket>
   {
      public void consumeObject(HeadOrientationPacket packet)
      {
         headOrientationPacket.set(packet);
      }
   }

   @Override
   public boolean isNewLookAtInformationAvailable()
   {
      LookAtPacket object = lookAtPacket.getAndSet(null);
      if (object != null)
      {
         pointToTrack.setIncludingFrame(lookAtFrame, object.getLookAtPoint());
         desiredJointForExtendedNeckPitchRangeAngle.set(0.0);
         return true;
      }
      else
      {
         return false;
      }
   }

   @Override
   public boolean isNewHeadOrientationInformationAvailable()
   {
      HeadOrientationPacket packet = headOrientationPacket.getAndSet(null);
      if (packet != null)
      {
         headOrientation.set(packet.getQuaternion());
         desiredJointForExtendedNeckPitchRangeAngle.set(packet.getDesiredJointForExtendedNeckPitchRangeAngle());
         desiredNeckPitchAngle.set(packet.getDesiredNeckPitchAngle());
         return true;
      }
      else
      {
         return false;
      }

   }

   @Override
   public FrameOrientation getDesiredHeadOrientation()
   {
      return headOrientation;
   }

   @Override
   public FramePoint getLookAtPoint()
   {
      return pointToTrack;
   }

   @Override
   public ReferenceFrame getHeadOrientationExpressedInFrame()
   {
      return headOrientationFrame;
   }

   @Override
   public double getDesiredExtendedNeckPitchJointAngle()
   {
      return desiredJointForExtendedNeckPitchRangeAngle.getAndSet(Double.NaN);
   }
}
