package us.ihmc.wholeBodyController;

import us.ihmc.communication.packets.wholebody.JointAnglesPacket;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.humanoidRobot.partNames.SpineJointName;
import us.ihmc.utilities.robotSide.RobotSide;

public class WholeBodyUtils
{
   static public JointAnglesPacket createJointAnglesPacketFromFullModel( FullRobotModel fullRobotModel,
         boolean leftLeg, boolean rightLeg, boolean spine, boolean leftArm, boolean rightArm, double trajectoryTime)
   {
      JointAnglesPacket packet = new JointAnglesPacket(
            fullRobotModel.getRobotSpecificJointNames().getArmJointNames().length,
            fullRobotModel.getRobotSpecificJointNames().getLegJointNames().length,
            fullRobotModel.getRobotSpecificJointNames().getSpineJointNames().length );
      
      int index = 0;
      
      packet.trajectoryTime = trajectoryTime;

      
      for (LegJointName legID: fullRobotModel.getRobotSpecificJointNames().getLegJointNames() )
      {
         for (RobotSide side: RobotSide.values)
         {
            double angle =  fullRobotModel.getLegJoint( side,  legID).getQ();

            if( leftArm )  packet.leftArmJointAngle[index] = angle;
            if(rightArm)   packet.rightArmJointAngle[index] = angle;
         }
         index++;
      }
      
      index = 0;
      
      for (ArmJointName armID: fullRobotModel.getRobotSpecificJointNames().getArmJointNames() )
      {
         for (RobotSide side: RobotSide.values)
         {
            double angle =  fullRobotModel.getArmJoint( side,  armID).getQ();

            if( leftLeg )   packet.leftLegJointAngle[index] = angle;
            if( rightLeg )  packet.rightLegJointAngle[index] = angle;
         }
         index++;
      }
      
      index = 0;
      
      for (SpineJointName spineID: fullRobotModel.getRobotSpecificJointNames().getSpineJointNames() )
      {
         double angle =  fullRobotModel.getSpineJoint( spineID ).getQ();
         
         if( spine )   packet.spineJointAngle[index] = angle;
         
         index++;
      }
      
      
      return packet;
  }

}
