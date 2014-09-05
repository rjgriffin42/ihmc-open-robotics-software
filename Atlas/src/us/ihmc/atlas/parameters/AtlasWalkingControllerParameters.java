package us.ihmc.atlas.parameters;

import static us.ihmc.atlas.ros.AtlasOrderedJointMap.back_bky;
import static us.ihmc.atlas.ros.AtlasOrderedJointMap.back_bkz;
import static us.ihmc.atlas.ros.AtlasOrderedJointMap.jointNames;
import static us.ihmc.atlas.ros.AtlasOrderedJointMap.neck_ry;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import com.yobotics.simulationconstructionset.util.controller.YoOrientationPIDGains;
import com.yobotics.simulationconstructionset.util.controller.YoSymmetricSE3PIDGains;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.RotationFunctions;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;


public class AtlasWalkingControllerParameters implements WalkingControllerParameters
{   
   private final boolean runningOnRealRobot;
   private final SideDependentList<Transform3D> handPosesWithRespectToChestFrame = new SideDependentList<Transform3D>();

   // Limits
   private final double neck_pitch_upper_limit = 1.14494; //0.83;    // true limit is = 1.134460, but pitching down more just looks at more robot chest
   private final double neck_pitch_lower_limit = -0.602139; //-0.610865;    // -math.pi/2.0;
   private final double head_yaw_limit = Math.PI / 4.0;
   private final double head_roll_limit = Math.PI / 4.0;
   private final double pelvis_pitch_upper_limit = 0.0;
   private final double pelvis_pitch_lower_limit = -0.35; //-math.pi / 6.0;

   private final double  min_leg_length_before_collapsing_single_support = 0.53; // corresponds to q_kny = 1.70 rad

   public AtlasWalkingControllerParameters()
   {
      this(false);
   }
   
   public AtlasWalkingControllerParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;
      
      for (RobotSide robotSide : RobotSide.values)
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
   
   @Override
   public boolean stayOnToes()
   {
      return false; // Not working for now
   }
   
   @Override
   public boolean doToeOffIfPossible()
   {
      return true; 
   }

   @Override
   public double getMaximumToeOffAngle()
   {
      return Math.toRadians(45.0);
   }

   @Override
   public boolean doToeTouchdownIfPossible()
   {
      return false;
   }

   @Override
   public double getToeTouchdownAngle()
   {
      return Math.toRadians(20.0);
   }

   @Override
   public boolean doHeelTouchdownIfPossible()
   {
      return false;
   }

   @Override
   public double getHeelTouchdownAngle()
   {
      return Math.toRadians(-20.0);
   }

   @Override
   public String[] getDefaultHeadOrientationControlJointNames()
   {
      return new String[] {jointNames[back_bkz], jointNames[neck_ry]}; 
   }
   
   @Override
   public String[] getDefaultChestOrientationControlJointNames()
   {
      return new String[]{};
   }

   @Override
   public boolean checkOrbitalEnergyCondition()
   {
      return false;
   }

// USE THESE FOR Real Atlas Robot and sims when controlling pelvis height instead of CoM.
   private final double minimumHeightAboveGround = 0.595 + 0.03;                                       
   private double nominalHeightAboveGround = 0.675 + 0.03; 
   private final double maximumHeightAboveGround = 0.735 + 0.03;

// USE THESE FOR DRC Atlas Model TASK 2 UNTIL WALKING WORKS BETTER WITH OTHERS.
//   private final double minimumHeightAboveGround = 0.785;                                       
//   private double nominalHeightAboveGround = 0.865; 
//   private final double maximumHeightAboveGround = 0.925; 
   
//   // USE THESE FOR VRC Atlas Model TASK 2 UNTIL WALKING WORKS BETTER WITH OTHERS.
//   private double minimumHeightAboveGround = 0.68;                                       
//   private double nominalHeightAboveGround = 0.76; 
//   private double maximumHeightAboveGround = 0.82; 

//   // USE THESE FOR IMPROVING WALKING, BUT DONT CHECK THEM IN UNTIL IT IMPROVED WALKING THROUGH MUD.
//   private double minimumHeightAboveGround = 0.68;                                       
//   private double nominalHeightAboveGround = 0.80;  // NOTE: used to be 0.76, jojo        
//   private double maximumHeightAboveGround = 0.84;  // NOTE: used to be 0.82, jojo        
   
   @Override
   public double minimumHeightAboveAnkle()
   {
      return minimumHeightAboveGround;
   }
   
   @Override
   public double nominalHeightAboveAnkle()
   {
      return nominalHeightAboveGround;
   }
   
   @Override
   public double maximumHeightAboveAnkle()
   {
      return maximumHeightAboveGround;
   }

   public void setNominalHeightAboveAnkle(double nominalHeightAboveAnkle)
   {
      this.nominalHeightAboveGround = nominalHeightAboveAnkle;
   }

   @Override
   public double getGroundReactionWrenchBreakFrequencyHertz()
   {
      return 7.0;
   }

   @Override
   public boolean resetDesiredICPToCurrentAtStartOfSwing()
   {
      return false;
   }

   @Override
   public double getUpperNeckPitchLimit()
   {
      return neck_pitch_upper_limit;
   }

   @Override
   public double getLowerNeckPitchLimit()
   {
      return neck_pitch_lower_limit;
   }

   @Override
   public double getHeadYawLimit()
   {
      return head_yaw_limit;
   }

   @Override
   public double getHeadRollLimit()
   {
      return head_roll_limit;
   }

   @Override
   public String getJointNameForExtendedPitchRange()
   {
      return jointNames[back_bky];
   }

   @Override
   public boolean finishSwingWhenTrajectoryDone()
   {
      return false;
   }

   @Override
   public double getFootForwardOffset()
   {
      return AtlasPhysicalProperties.footForward;
   }
   
   @Override
   public double getFootSwitchCoPThresholdFraction()
   {
	   return 0.02;
   }

   @Override
   public double getFootBackwardOffset()
   {
      return AtlasPhysicalProperties.footBack;
   }
   
   @Override
   public double getAnkleHeight()
   {
      return AtlasPhysicalProperties.ankleHeight;
   }

   @Override
   public double getLegLength()
   {
      return AtlasPhysicalProperties.shinLength + AtlasPhysicalProperties.thighLength;
   }
   
   @Override
   public double getMinLegLengthBeforeCollapsingSingleSupport()
   {
      return min_leg_length_before_collapsing_single_support;
   }

   @Override
   public double getFinalToeOffPitchAngularVelocity()
   {
      return 3.5;
   }

   @Override
   public double getInPlaceWidth()
   {
      return 0.25;
   }

   @Override
   public double getDesiredStepForward()
   {
      return 0.5; //0.35;
   }
  
   @Override
   public double getMaxStepLength()
   {
       return 0.6; //0.5; //0.35;
   }

   @Override
   public double getMinStepWidth()
   {
      return 0.15;
   }

   @Override
   public double getMaxStepWidth()
   {
      return 0.6; //0.4;
   }

   @Override
   public double getStepPitch()
   {
      return 0.0;
   }

   @Override
   public double getCaptureKpParallelToMotion()
   {
      if (!runningOnRealRobot) return 1.0;
      return 1.0; 
   }

   @Override
   public double getCaptureKpOrthogonalToMotion()
   {      
      if (!runningOnRealRobot) return 1.0; 
      return 1.0; 
   }
   
   @Override
   public double getCaptureKi()
   {      
      if (!runningOnRealRobot) return 4.0;
      return 4.0; 
   }
   
   @Override
   public double getCaptureKiBleedoff()
   {      
      return 0.9; 
   }
   
   @Override
   public double getCaptureFilterBreakFrequencyInHz()
   {
      if (!runningOnRealRobot) return 16.0; //Double.POSITIVE_INFINITY;
      return 16.0;
   }
   
   @Override
   public double getCMPRateLimit()
   {
      if (!runningOnRealRobot) return 60.0; 
      return 6.0; //3.0; //4.0; //3.0;
   }

   @Override
   public double getCMPAccelerationLimit()
   {
      if (!runningOnRealRobot) return 2000.0;
      return 200.0; //80.0; //40.0;
   }
   
   @Override
   public double getKpCoMHeight()
   {
      if (!runningOnRealRobot) return 40.0;
      return 40.0; //20.0; 
   }

   @Override
   public double getZetaCoMHeight()
   {
      if (!runningOnRealRobot) return 0.8; //1.0;
      return 0.4;
   }

   @Override
   public double getDefaultDesiredPelvisPitch()
   {
      return 0.0;
   }

   @Override
   public double getKpPelvisOrientation()
   {
      if (!runningOnRealRobot) return 80.0; //100.0;
      return 80.0; //30.0; 
   }

   @Override
   public double getZetaPelvisOrientation()
   {
      if (!runningOnRealRobot) return 0.8; //1.0;
      return 0.25;
   }
   

   @Override
   public double getMaxAccelerationPelvisOrientation()
   {
      if (!runningOnRealRobot) return 36.0; // 18.0;
      return 12.0; 
   }

   @Override
   public double getMaxJerkPelvisOrientation()
   {
      if (!runningOnRealRobot) return 540.0; // 270.0;
      return 180.0; 
   }

   @Override
   public double getKpHeadOrientation()
   {
      if (!runningOnRealRobot) return 40.0;
      return 40.0; 
   }

   @Override
   public double getZetaHeadOrientation()
   {
      if (!runningOnRealRobot) return 0.8; //1.0;
      return 0.4;
   }

   @Override
   public double getTrajectoryTimeHeadOrientation()
   {
      return 3.0;
   }

   @Override
   public double getKpUpperBody()
   {
      if (!runningOnRealRobot) return 80.0; //100.0;
      return 80.0; //40.0;
   }

   @Override
   public double getZetaUpperBody()
   {
      if (!runningOnRealRobot) return 0.8; //1.0;
      return 0.25;
   }
   
   @Override
   public double getMaxAccelerationUpperBody()
   {
      if (!runningOnRealRobot) return 36.0; // 18.0; //100.0;
      return 6.0;
   }
   
   @Override
   public double getMaxJerkUpperBody()
   {
      if (!runningOnRealRobot) return 540.0; // 270.0; //1000.0;
      return 60.0;
   }

   @Override
   public YoOrientationPIDGains createChestControlGains(YoVariableRegistry registry)
   {
      YoSymmetricSE3PIDGains gains = new YoSymmetricSE3PIDGains("ChestOrientation", registry);

      double kp = 80.0;
      double zeta = runningOnRealRobot ? 0.25 : 0.8;
      double ki = 0.0;
      double maxIntegralError = 0.0;
      double maxAccel = runningOnRealRobot ? 6.0 : 36.0;
      double maxJerk = runningOnRealRobot ? 60.0 : 540.0;

      gains.setProportionalGain(kp);
      gains.setDampingRatio(zeta);
      gains.setIntegralGain(ki);
      gains.setMaximumIntegralError(maxIntegralError);
      gains.setMaximumAcceleration(maxAccel);
      gains.setMaximumJerk(maxJerk);
      gains.createDerivativeGainUpdater(true);

      return gains;
   }

   @Override
   public double getSwingKpXY()
   {
      return 100.0;
   }
   
   @Override
   public double getSwingHeightMaxForPushRecoveryTrajectory()
   {
      return 0.12;
   }
   
   @Override
   public double getSwingKpZ()
   {
      return 200.0;
   }
   
   @Override
   public double getSwingKpOrientation()
   {
      return 200.0;
   }
   
   @Override
   public double getSwingZetaXYZ()
   {
      if (!runningOnRealRobot) return 0.7;
      return 0.25;
   }
   
   @Override
   public double getSwingZetaOrientation()
   {
      if (!runningOnRealRobot) return 0.7;
      return 0.7; 
   }

   @Override
   public double getHoldKpXY()
   {
      return 100.0;
   }
   
   @Override
   public double getHoldKpOrientation()
   {
      if (!runningOnRealRobot) return 100.0;
      return 200.0;
   }
   
   @Override
   public double getHoldZeta()
   {
      if (!runningOnRealRobot) return 1.0;
      return 0.2;
   }

   @Override
   public double getSwingMaxPositionAcceleration()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 10.0;
   }
   
   @Override
   public double getSwingMaxPositionJerk()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 150.0;
   }
   
   @Override
   public double getSwingMaxOrientationAcceleration()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 100.0;
   }
   
   public double getSwingMaxHeightForPushRecoveryTrajectory()
   {
	   return 0.15;
   }
   
   @Override
   public double getSwingMaxOrientationJerk()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 1500.0;
   }
   @Override
   public double getSupportSingularityEscapeMultiplier()
   {
      return 30;
   }

   @Override
   public double getSwingSingularityEscapeMultiplier()
   {
      return runningOnRealRobot ? 50.0 : 200.0;
   }

   @Override
   public boolean doPrepareManipulationForLocomotion()
   {
      return true;
   }

   @Override
   public double getToeOffKpXY()
   {
      return 100.0;
   }

   @Override
   public double getToeOffKpOrientation()
   {
      return 200.0;
   }

   @Override
   public double getToeOffZeta()
   {
      return 0.4;
   }

   @Override
   public boolean isRunningOnRealRobot()
   {
      return runningOnRealRobot;
   }

   @Override
   public double getDefaultTransferTime()
   {
      return runningOnRealRobot ? 1.5 : 0.25;
   }

   @Override
   public double getDefaultSwingTime()
   {
      return runningOnRealRobot ? 1.5 : 0.60;
   }

   @Override
   public double getPelvisPitchUpperLimit()
   {
      return pelvis_pitch_upper_limit;
   }
   
   @Override
   public double getPelvisPitchLowerLimit()
   {
      return pelvis_pitch_lower_limit;
   }

   @Override
   public boolean isPelvisPitchReversed()
   {
      return false;
   }

   @Override
   public double getFootWidth()
   {
      return AtlasPhysicalProperties.footWidth;
   }

   @Override
   public double getToeWidth()
   {
      return AtlasPhysicalProperties.toeWidth;
   }

   @Override
   public double getFootLength()
   {
      return AtlasPhysicalProperties.footLength;
   }

   @Override
   public double getFoot_start_toetaper_from_back()
   {
      return AtlasPhysicalProperties.footStartToetaperFromBack;
   }

   @Override
   public double getSideLengthOfBoundingBoxForFootstepHeight()
   {
      return (1 + 0.3) * 2 * Math.sqrt(getFootForwardOffset() * getFootForwardOffset()
            + 0.25 * getFootWidth() * getFootWidth());
   }
   
   @Override
   public SideDependentList<Transform3D> getDesiredHandPosesWithRespectToChestFrame()
   {
      return handPosesWithRespectToChestFrame;
   }

   @Override
   public double getDesiredTouchdownVelocity()
   {
      return -0.3;
   }

   @Override
   public double getContactThresholdForce()
   {
      return runningOnRealRobot ? 80.0 : 5.0;
   }

   @Override
   public String[] getJointsToIgnoreInController()
   {
      return null;
   }
}
