package us.ihmc.commonWalkingControlModules.packetProducers;

import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;

import us.ihmc.communication.packets.manipulation.HandPoseStatus;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.utilities.robotSide.RobotSide;

public class HandPoseStatusProducer
{
   private final GlobalDataProducer objectCommunicator;

   public HandPoseStatusProducer(GlobalDataProducer objectCommunicator)
   {
      this.objectCommunicator = objectCommunicator;
   }

   public void sendStatus(Point3d currentPosePosition, Quat4d currentPoseOrientationInWorldFrame, Point3d desiredPosePosition,
         Quat4d desiredPoseOrientationInWorldFrame, RobotSide robotSide)
   {
      HandPoseStatus handPoseStatus = new HandPoseStatus(currentPosePosition, currentPoseOrientationInWorldFrame, desiredPosePosition,
            desiredPoseOrientationInWorldFrame, robotSide);
      objectCommunicator.sendPacket(handPoseStatus);
   }

   public void sendCompletedStatus(RobotSide robotSide)
   {
      HandPoseStatus handPoseStatus = HandPoseStatus.createPositionIsReachedPacket(robotSide);
      objectCommunicator.sendPacket(handPoseStatus);
   }
   
   public void sendStartedStatus(RobotSide robotSide)
   {
      HandPoseStatus handPoseStatus = HandPoseStatus.createHandPoseIsStarted(robotSide);
      objectCommunicator.sendPacket(handPoseStatus);
   }

   public void sendTimeOutIsReachedStatus(RobotSide robotSide)
   {
      HandPoseStatus handPoseStatus = HandPoseStatus.createTimeOutIsReachedPacket(robotSide);
      objectCommunicator.sendPacket(handPoseStatus);
   }
}
