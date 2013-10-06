package us.ihmc.darpaRoboticsChallenge;

import static us.ihmc.darpaRoboticsChallenge.ros.ROSAtlasJointMap.*;


public class DRCRobotMultiContactControllerParameters extends DRCRobotWalkingControllerParameters
{
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
