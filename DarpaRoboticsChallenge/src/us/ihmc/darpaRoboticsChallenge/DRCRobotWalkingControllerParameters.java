package us.ihmc.darpaRoboticsChallenge;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.darpaRoboticsChallenge.drcRobot.DRCRobotParameters;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.RotationFunctions;

public class DRCRobotWalkingControllerParameters implements WalkingControllerParameters
{
   private final SideDependentList<Transform3D> handControlFramesWithRespectToFrameAfterWrist = new SideDependentList<Transform3D>();
   private final SideDependentList<Transform3D> handPosesWithRespectToChestFrame = new SideDependentList<Transform3D>();
   public DRCRobotWalkingControllerParameters()
   {
      for(RobotSide robotSide : RobotSide.values)
      {
         Transform3D rotationPart = new Transform3D();
         double yaw = robotSide.negateIfRightSide(Math.PI / 2.0);
         double pitch = 0.0;
         double roll = 0.0;
         rotationPart.setEuler(new Vector3d(roll, pitch, yaw));
   
         Transform3D toHand = new Transform3D();
//         double x = 0.2;
//         double y = robotSide.negateIfRightSide(-0.03);
//         double z = 0.04;
         double x = 0.1;
         double y = 0.0;
         double z = 0.0;
         toHand.setTranslation(new Vector3d(x, y, z));
   
         Transform3D transform = new Transform3D();
         transform.mul(rotationPart, toHand);
         
         handControlFramesWithRespectToFrameAfterWrist.put(robotSide, transform);
      }
      
      
      for (RobotSide robotSide : RobotSide.values())
      {
         Transform3D transform = new Transform3D();

         double x = 0.20;
         double y = robotSide.negateIfRightSide(0.35); //0.30);
         double z = -0.40;
         transform.setTranslation(new Vector3d(x, y, z));

         Matrix3d rotation = new Matrix3d();
         double yaw = 0.0;//robotSide.negateIfRightSide(-1.7);
         double pitch = 0.7;
         double roll = 0.0;//robotSide.negateIfRightSide(-0.8);
         RotationFunctions.setYawPitchRoll(rotation, yaw, pitch, roll);
         transform.setRotation(rotation);

         handPosesWithRespectToChestFrame.put(robotSide, transform);
      }
   }
   
   public SideDependentList<Transform3D> getDesiredHandPosesWithRespectToChestFrame()
   {
      return handPosesWithRespectToChestFrame;
   }


   public boolean doStrictPelvisControl()
   {
      return true;
   }

   public String[] getHeadOrientationControlJointNames()
   {
      // Get rid of back_ubx to prevent hip roll jumps.
//      return new String[] {"back_lbz", "back_ubx", "neck_ay"}; // Pelvis will jump around with these setting.
      return new String[] {"back_lbz", "neck_ay"}; 
//      return new String[] {"neck_ay"};
   }

   public String[] getChestOrientationControlJointNames()
   {
//    return new String[] {"back_mby"};
      return new String[]
      {
      };
   }

   public boolean checkOrbitalEnergyCondition()
   {
      return false;
   }

   double nominalHeightAboveGround = 0.78;

   public double nominalHeightAboveAnkle()
   {
      return nominalHeightAboveGround;
   }

   public void setNominalHeightAboveAnkle(double nominalHeightAboveAnkle)
   {
      this.nominalHeightAboveGround = nominalHeightAboveAnkle;
   }

   public double getGroundReactionWrenchBreakFrequencyHertz()
   {
      return 7.0;
   }

   public boolean resetDesiredICPToCurrentAtStartOfSwing()
   {
      return false;
   }

   public double getUpperNeckPitchLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_NECK_PITCH_UPPER_LIMIT;
   }

   public double getLowerNeckPitchLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_NECK_PITCH_LOWER_LIMIT;
   }

   public double getHeadYawLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_HEAD_YAW_LIMIT;
   }

   public double getHeadRollLimit()
   {
      return DRCRobotParameters.DRC_ROBOT_HEAD_ROLL_LIMIT;
   }

   public String getJointNameForExtendedPitchRange()
   {
      return "back_mby";
   }


   public SideDependentList<Transform3D> getHandControlFramesWithRespectToFrameAfterWrist()
   {
      return handControlFramesWithRespectToFrameAfterWrist;
   }

   public boolean finishSwingWhenTrajectoryDone()
   {
      return false;
   }

   public double getFootForwardOffset()
   {
      return DRCRobotParameters.DRC_ROBOT_FOOT_FORWARD;
   }

   public double getFootBackwardOffset()
   {
      return DRCRobotParameters.DRC_ROBOT_FOOT_BACK;
   }
   
   public double getAnkleHeight()
   {
      return DRCRobotParameters.DRC_ROBOT_ANKLE_HEIGHT;
   }
}
