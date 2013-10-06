package us.ihmc.darpaRoboticsChallenge;


import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.*;

import java.util.LinkedHashMap;
import java.util.Map;

public class DRCRobotDrivingControllerParameters extends DRCRobotWalkingControllerParameters
{
   @Override
   public Map<OneDoFJoint, Double> getDefaultArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> jointPositions = new LinkedHashMap<OneDoFJoint, Double>();

      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_ROLL), robotSide.negateIfRightSide(-1.2));
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_PITCH), 0.34);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_ROLL), robotSide.negateIfRightSide(1.3));
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_PITCH), 1.94);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_PITCH), -0.19);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_ROLL), robotSide.negateIfRightSide(-0.07));

      return jointPositions;
   }


   @Override
   public String[] getDefaultHeadOrientationControlJointNames()
   {
      return new String[] {jointNames[neck_ry]};
   }

   @Override
   public String[] getDefaultChestOrientationControlJointNames()
   {
      return new String[] {jointNames[back_bkz], jointNames[back_bkx], jointNames[back_bky]};
   }

   @Override
   public String getJointNameForExtendedPitchRange()
   {
      return null;
   }


}
