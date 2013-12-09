package us.ihmc.commonWalkingControlModules.packetProviders;

import us.ihmc.commonWalkingControlModules.packets.ControlStatusPacket;
import us.ihmc.commonWalkingControlModules.packets.ControlStatusPacket.ControlStatus;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.io.streamingData.GlobalDataProducer;

public class NetworkControlStatusProducer implements ControlStatusProducer
{
   
   private final GlobalDataProducer globalDataProducer;
   
   public NetworkControlStatusProducer(GlobalDataProducer globalDataProducer)
   {
      this.globalDataProducer = globalDataProducer;
   }

   public void notifyHandTrajectoryInfeasible(RobotSide robotSide)
   {
      globalDataProducer.queueDataToSend(new ControlStatusPacket(robotSide, ControlStatus.HAND_TRAJECTORY_INFEASIBLE));
   }

}
