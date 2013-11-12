package us.ihmc.commonWalkingControlModules.packetConsumers;

import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.commonWalkingControlModules.packets.HeadOrientationPacket;
import us.ihmc.commonWalkingControlModules.packets.LookAtPacket;
import us.ihmc.utilities.math.geometry.FrameOrientation;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.net.ObjectConsumer;

public class DesiredHeadOrientationProvider
{
   private final AtomicReference<LookAtPacket> lookAtPacket = new AtomicReference<LookAtPacket>();
   private final AtomicReference<HeadOrientationPacket> headOrientationPacket = new AtomicReference<HeadOrientationPacket>();
   
   private final HeadOrientationPacketConsumer headOrientationPacketConsumer;
   private final LookAtPacketConsumer lookAtPacketConsumer;
   private final ReferenceFrame headOrientationFrame;

   private double desiredJointForExtendedNeckPitchRangeAngle;
   
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


   public boolean isNewLookAtInformationAvailable()
   {
      LookAtPacket object = lookAtPacket.getAndSet(null);
      if(object != null)
      {
         pointToTrack.set(lookAtFrame, object.getLookAtPoint());
         desiredJointForExtendedNeckPitchRangeAngle = 0.0;
         return true;
      }
      else
      {
         return false;
      }
   }

   public boolean isNewHeadOrientationInformationAvailable()
   {
      HeadOrientationPacket packet = headOrientationPacket.getAndSet(null);
      if(packet != null)
      {
         headOrientation.set(packet.getQuaternion());
         desiredJointForExtendedNeckPitchRangeAngle = packet.getDesiredJointForExtendedNeckPitchRangeAngle();
         return true;
      }
      else
      {
         return false;
      }
      
   }

   public FrameOrientation getDesiredHeadOrientation()
   {
      return headOrientation;
   }

   public FramePoint getLookAtPoint()
   {
      return pointToTrack;
   }

   public ReferenceFrame getHeadOrientationExpressedInFrame()
   {
      return headOrientationFrame;
   }

   public double getDesiredExtendedNeckPitchJointAngle()
   {
      return desiredJointForExtendedNeckPitchRangeAngle;
   }

}
