package us.ihmc.valkyrie.paramaters;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.media.j3d.Transform3D;

import us.ihmc.commonWalkingControlModules.configurations.WalkingControllerParameters;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.OneDoFJoint;

public class ValkyrieWalkingControllerParameters implements WalkingControllerParameters
{
   private final boolean runningOnRealRobot;

   private final SideDependentList<Transform3D> handControlFramesWithRespectToFrameAfterWrist = new SideDependentList<Transform3D>();
   private final SideDependentList<Transform3D> handPosesWithRespectToChestFrame = new SideDependentList<Transform3D>();
   private final double minElbowRollAngle = 0.5;
   
   public ValkyrieWalkingControllerParameters()
   {
      this(false);
   }
   
   public ValkyrieWalkingControllerParameters(boolean runningOnRealRobot)
   {
      this.runningOnRealRobot = runningOnRealRobot;

      for (RobotSide robotSide : RobotSide.values)
      {
         handControlFramesWithRespectToFrameAfterWrist.put(robotSide, new Transform3D());
      }

      
      // Genreated using ValkyrieFullRobotModelVisualizer
      Transform3D leftHandLocation = new Transform3D(new double[] {0.8772111323383822, -0.47056204413925823, 0.09524700476706424, 0.11738015536007923,
            1.5892231999088989E-4, 0.1986725292086453, 0.980065916600275, 0.3166524835978034,
            -0.48010478444326166, -0.8597095955922112, 0.1743525371234003, -0.13686311108389013,
            0.0, 0.0, 0.0, 1.0});
      
      Transform3D rightHandLocation = new Transform3D(new double[] {0.8772107606751612, -0.47056267784177724, -0.09524729695945025, 0.11738015535642271,
            -1.5509783447718197E-4, -0.19866600827375044, 0.9800672390715021, -0.3166524835989298,
            -0.48010546476828164, -0.8597107556492186, -0.17434494349043353, -0.13686311108617974,
            0.0, 0.0, 0.0, 1.0});
      
      handPosesWithRespectToChestFrame.put(RobotSide.LEFT, leftHandLocation);
      handPosesWithRespectToChestFrame.put(RobotSide.RIGHT, rightHandLocation);
      
      
   }

   public SideDependentList<Transform3D> getDesiredHandPosesWithRespectToChestFrame()
   {
      return handPosesWithRespectToChestFrame;
   }

   public Map<OneDoFJoint, Double> getDefaultArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> jointPositions = new LinkedHashMap<OneDoFJoint, Double>();

      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_ROLL), 0.0);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_PITCH), 0.0);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_YAW), 0.0);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_PITCH), -1.0);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_YAW), 0.0);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_PITCH), 0.0);
      jointPositions.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.WRIST_ROLL), 0.0);
      
      return jointPositions;
   }

   public Map<OneDoFJoint, Double> getMinTaskspaceArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> ret = new LinkedHashMap<OneDoFJoint, Double>();
      if (robotSide == RobotSide.LEFT)
      {
         ret.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_YAW), minElbowRollAngle);
      }
      return ret;
   }

   public Map<OneDoFJoint, Double> getMaxTaskspaceArmJointPositions(FullRobotModel fullRobotModel, RobotSide robotSide)
   {
      Map<OneDoFJoint, Double> ret = new LinkedHashMap<OneDoFJoint, Double>();
      if (robotSide == RobotSide.RIGHT)
      {
         ret.put(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_YAW), minElbowRollAngle);
      }
      return ret;
   }

   public boolean stayOnToes()
   {
      return false; // Not working for now
   }

   public boolean doToeOffIfPossible()
   {
      return true;
   }

   public boolean doToeTouchdownIfPossible()
   {
      return false;
   }

   public boolean doHeelTouchdownIfPossible()
   {
      return false;
   }

   public String[] getDefaultHeadOrientationControlJointNames()
   {
      return new String[] { ValkyrieJointMap.upperNeckPitchJointName,ValkyrieJointMap.pelvisYawJointName };
   }

   public String[] getAllowableHeadOrientationControlJointNames()
   {
      return new String[] { ValkyrieJointMap.lowerNeckPitchJointName, ValkyrieJointMap.neckYawJointName, ValkyrieJointMap.upperNeckPitchJointName };
   }

   public String[] getDefaultChestOrientationControlJointNames()
   {
      return new String[] { ValkyrieJointMap.pelvisName };
   }

   public String[] getAllowableChestOrientationControlJointNames()
   {
      return new String[] { ValkyrieJointMap.pelvisPitchJointName };
   }

   public boolean checkOrbitalEnergyCondition()
   {
      return false;
   }

   // USE THESE FOR Real Atlas Robot and sims when controlling pelvis height instead of CoM.
   private final double minimumHeightAboveGround = 0.595 + 0.23;
   private double nominalHeightAboveGround = 0.675 + 0.23;
   private final double maximumHeightAboveGround = 0.735 + 0.23;

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

   public double minimumHeightAboveAnkle()
   {
      return minimumHeightAboveGround;
   }

   public double nominalHeightAboveAnkle()
   {
      return nominalHeightAboveGround;
   }

   public double maximumHeightAboveAnkle()
   {
      return maximumHeightAboveGround;
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
      return 0.785398;//1.14494;
   }

   public double getLowerNeckPitchLimit()
   {
      return -0.0872665;//-0.602139;
   }

   public double getHeadYawLimit()
   {
      return Math.PI / 4.0;
   }

   public double getHeadRollLimit()
   {
      return Math.PI / 4.0;
   }

   public String getJointNameForExtendedPitchRange()
   {
      return ValkyrieJointMap.pelvisPitchJointName;
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
      return ValkyriePhysicalProperties.footForward;
   }

   public double getFootBackwardOffset()
   {
      return ValkyriePhysicalProperties.footBack;
   }

   public double getAnkleHeight()
   {
      return ValkyriePhysicalProperties.ankleHeight;
   }

   public double getLegLength()
   {
      return ValkyriePhysicalProperties.thighLength + ValkyriePhysicalProperties.shinLength;
   }

   public double getMinLegLengthBeforeCollapsingSingleSupport()
   {
      //TODO: Useful values
      return 0.1;
   }

   public double getFinalToeOffPitchAngularVelocity()
   {
      return 3.5;
   }

   public double getInPlaceWidth()
   {
      return 0.25;
   }

   public double getDesiredStepForward()
   {
      return 0.5; //0.35;
   }

   public double getMaxStepLength()
   {
      return 0.6; //0.5; //0.35;
   }

   public double getMinStepWidth()
   {
      return 0.15;
   }

   public double getMaxStepWidth()
   {
      return 0.6; //0.4;
   }

   public double getStepPitch()
   {
      return 0.0;
   }

   public double getCaptureKpParallelToMotion()
   {
      if (!runningOnRealRobot) return 1.0;
      return 1.0; 
   }

   public double getCaptureKpOrthogonalToMotion()
   {
      if (!runningOnRealRobot) return 1.0; 
      return 1.0;
   }

   public double getCaptureKi()
   {
      if (!runningOnRealRobot) return 4.0;
      return 4.0;
   }

   public double getCaptureKiBleedoff()
   {
      return 0.9;
   }

   public double getCaptureFilterBreakFrequencyInHz()
   {
      if (!runningOnRealRobot) return 16.0;
      return 16.0;
   }

   public double getCMPRateLimit()
   {
      if (!runningOnRealRobot) return 60.0;
      return 6.0;
   }

   public double getCMPAccelerationLimit()
   {
      if (!runningOnRealRobot) return 2000.0;
      return 200.0;
   }
   
   public double getKpCoMHeight()
   {
      if (!runningOnRealRobot) return 50.0;
      return 40.0;
   }

   public double getZetaCoMHeight()
   {
      if (!runningOnRealRobot) return 1.0;
      return 0.4;
   }

   public double getKpPelvisOrientation()
   {
      if (!runningOnRealRobot) return 100.0;
      return 80.0;
   }

   public double getZetaPelvisOrientation()
   {
      if (!runningOnRealRobot) return 0.8;
      return 0.25;
   }
   

   public double getMaxAccelerationPelvisOrientation()
   {
      if (!runningOnRealRobot) return 18.0;
      return 12.0; 
   }

   public double getMaxJerkPelvisOrientation()
   {
      if (!runningOnRealRobot) return 270.0;
      return 180.0; 
   }

   public double getKpHeadOrientation()
   {
      if (!runningOnRealRobot) return 40.0;
      return 40.0; 
   }

   public double getZetaHeadOrientation()
   {
      if (!runningOnRealRobot) return 0.8;
      return 0.4;
   }

   public double getTrajectoryTimeHeadOrientation()
   {
      return 3.0;
   }

   public double getKpUpperBody()
   {
      if (!runningOnRealRobot) return 100.0;
      return 80.0;
   }

   public double getZetaUpperBody()
   {
      if (!runningOnRealRobot) return 0.8;
      return 0.25;
   }

   public double getMaxAccelerationUpperBody()
   {
      if (!runningOnRealRobot) return 18.0;
      return 6.0;
   }

   public double getMaxJerkUpperBody()
   {
      if (!runningOnRealRobot) return 270.0;
      return 60.0;
   }

   public double getSwingKpXY()
   {
      return 100.0;
   }

   public double getSwingKpZ()
   {
      return 200.0;
   }

   public double getSwingKpOrientation()
   {
      return 200.0;
   }

   public double getSwingZetaXYZ()
   {
      if (!runningOnRealRobot) return 0.7;
      return 0.25;
   }

   public double getSwingZetaOrientation()
   {
      if (!runningOnRealRobot) return 0.7;
      return 0.7; 
   }

   public double getHoldKpXY()
   {
      return 100.0;
   }

   public double getHoldKpOrientation()
   {
      if (!runningOnRealRobot) return 100.0;
      return 200.0;
   }

   public double getHoldZeta()
   {
      if (!runningOnRealRobot) return 1.0;
      return 0.2;
   }

   public double getSwingMaxPositionAcceleration()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 10.0;
   }

   public double getSwingMaxPositionJerk()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 150.0;
   }

   public double getSwingMaxOrientationAcceleration()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 100.0;
   }

   public double getswingMaxOrientationJerk()
   {
      if (!runningOnRealRobot) return Double.POSITIVE_INFINITY;
      return 1500.0;
   }

   public boolean doPrepareManipulationForLocomotion()
   {
      return true;
   }

   public double getToeOffKpXY()
   {
      return 100.0;
   }

   public double getToeOffKpOrientation()
   {
      return 200.0;
   }

   public double getToeOffZeta()
   {
      return 0.4;
   }

   public boolean isRunningOnRealRobot()
   {
      return false;
   }

   public double getDefaultTransferTime()
   {
      return 0.25;
   }

   public double getDefaultSwingTime()
   {
      return 0.6;
   }

   public double getPelvisPitchUpperLimit()
   {
      return 0.0872665;
   }

   public double getPelvisPitchLowerLimit()
   {
      return -0.698132;
   }

   public boolean isPelvisPitchReversed()
   {
      return true;
   }

   public double getAnkle_height()
   {
      return ValkyriePhysicalProperties.ankleHeight;
   }
   public double getFoot_width()
   {
      return ValkyriePhysicalProperties.footWidth;
   }

   public double getToe_width()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   public double getFoot_length()
   {
      return ValkyriePhysicalProperties.footBack + ValkyriePhysicalProperties.footForward;
   }

   public double getFoot_back()
   {
      return ValkyriePhysicalProperties.footBack;
   }

   public double getFoot_start_toetaper_from_back()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   public double getFoot_forward()
   {
      return ValkyriePhysicalProperties.footForward;
   }

   public double getSideLengthOfBoundingBoxForFootstepHeight()
   {
      return (1 + 0.3) * 2 * Math.sqrt(getFoot_forward() * getFoot_forward()
            + 0.25 * getFoot_width() * getFoot_width());
   }
}
