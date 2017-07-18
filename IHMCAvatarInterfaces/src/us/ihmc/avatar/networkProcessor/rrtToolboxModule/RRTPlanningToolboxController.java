package us.ihmc.avatar.networkProcessor.rrtToolboxModule;

import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.avatar.networkProcessor.modules.ToolboxController;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.humanoidRobotics.communication.packets.manipulation.RRTPlanningRequestPacket;
import us.ihmc.humanoidRobotics.communication.packets.walking.FootstepPlanningRequestPacket;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class RRTPlanningToolboxController extends ToolboxController
{
   private final AtomicReference<FootstepPlanningRequestPacket> latestRequestReference = new AtomicReference<FootstepPlanningRequestPacket>(null);
   
   public RRTPlanningToolboxController(StatusMessageOutputManager statusOutputManager, YoVariableRegistry parentRegistry)
   {
      super(statusOutputManager, parentRegistry);
      PrintTools.info("ee");

   }

   @Override
   protected void updateInternal()
   {
      PrintTools.info("ff");
      
   }

   @Override
   protected boolean initialize()
   {
      PrintTools.info("dd");
      FootstepPlanningRequestPacket request = latestRequestReference.getAndSet(null);
      PrintTools.info("dd");
      if (request == null)
      {
         PrintTools.info("dd");
         return false;
      }
         
      PrintTools.info("dd");
      
      return true;
   }

   @Override
   protected boolean isDone()
   {
      return false;
   }

   public PacketConsumer<FootstepPlanningRequestPacket> createRequestConsumer()
   {
      PrintTools.info("cc");
      
      return new PacketConsumer<FootstepPlanningRequestPacket>()
      {
         
         @Override
         public void receivedPacket(FootstepPlanningRequestPacket packet)
         {
            PrintTools.info("cc");
            if (packet == null)
               return;
            latestRequestReference.set(packet);
         }
      };
   }

}
