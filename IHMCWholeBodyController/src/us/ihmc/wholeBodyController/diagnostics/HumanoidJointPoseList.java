package us.ihmc.wholeBodyController.diagnostics;

import java.util.ArrayList;

import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HumanoidArmPose;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HumanoidLegPose;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HumanoidSpinePose;
import us.ihmc.utilities.humanoidRobot.model.FullRobotModel;
import us.ihmc.utilities.humanoidRobot.partNames.ArmJointName;
import us.ihmc.utilities.humanoidRobot.partNames.LegJointName;
import us.ihmc.utilities.humanoidRobot.partNames.SpineJointName;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.IntegerYoVariable;

public class HumanoidJointPoseList
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   
   private final IntegerYoVariable humanoidJointPoseIndex = new IntegerYoVariable("humanoidJointPoseIndex", registry);
   
   private final ArrayList<HumanoidJointPose> humanoidJointPoses = new ArrayList<HumanoidJointPose>();
   
   private final EnumYoVariable<HumanoidArmPose> desiredArmPose = new EnumYoVariable<HumanoidArmPose>("desiredArmPose", registry, HumanoidArmPose.class);
   private final EnumYoVariable<HumanoidSpinePose> desiredSpinePose = new EnumYoVariable<HumanoidSpinePose>("desiredSpinePose", registry, HumanoidSpinePose.class);
   private final EnumYoVariable<HumanoidLegPose> desiredLegPose = new EnumYoVariable<HumanoidLegPose>("desiredLegPose", registry, HumanoidLegPose.class);
   
   public HumanoidJointPoseList()
   {
      createPoseSetters();
      createPoseSettersJustArms();
      createPoseSettersTuneWaist();
   }
   
   public void setParentRegistry(YoVariableRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
   }
   
   public SideDependentList<ArrayList<OneDoFJoint>> getArmJoints(FullRobotModel fullRobotModel)
   {
      SideDependentList<ArrayList<OneDoFJoint>> ret = new SideDependentList<ArrayList<OneDoFJoint>>();

      for (RobotSide robotSide : RobotSide.values)
      {
         ArrayList<OneDoFJoint> armJoints = new ArrayList<OneDoFJoint>();
         
         armJoints.add(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_PITCH));
         armJoints.add(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_ROLL));
         armJoints.add(fullRobotModel.getArmJoint(robotSide, ArmJointName.SHOULDER_YAW));
         armJoints.add(fullRobotModel.getArmJoint(robotSide, ArmJointName.ELBOW_PITCH));
         
         ret.set(robotSide, armJoints);
      }

      return ret;
   }
   
   public SideDependentList<ArrayList<OneDoFJoint>> getLegJoints(FullRobotModel fullRobotModel)
   {
      SideDependentList<ArrayList<OneDoFJoint>> ret = new SideDependentList<ArrayList<OneDoFJoint>>();

      for (RobotSide robotSide : RobotSide.values)
      {
         ArrayList<OneDoFJoint> legJoints = new ArrayList<OneDoFJoint>();
         
         legJoints.add(fullRobotModel.getLegJoint(robotSide, LegJointName.HIP_YAW));
         legJoints.add(fullRobotModel.getLegJoint(robotSide, LegJointName.HIP_ROLL));
         legJoints.add(fullRobotModel.getLegJoint(robotSide, LegJointName.HIP_PITCH));
         legJoints.add(fullRobotModel.getLegJoint(robotSide, LegJointName.KNEE));
         legJoints.add(fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_PITCH));
         legJoints.add(fullRobotModel.getLegJoint(robotSide, LegJointName.ANKLE_ROLL));
         
         ret.set(robotSide, legJoints);
      }

      return ret;
   }
   
   public ArrayList<OneDoFJoint> getSpineJoints(FullRobotModel fullRobotModel)
   {
      ArrayList<OneDoFJoint> spineJoints = new ArrayList<OneDoFJoint>();

      spineJoints.add(fullRobotModel.getSpineJoint(SpineJointName.SPINE_YAW));
      spineJoints.add(fullRobotModel.getSpineJoint(SpineJointName.SPINE_PITCH));
      spineJoints.add(fullRobotModel.getSpineJoint(SpineJointName.SPINE_ROLL));

      return spineJoints;
   }
   
   public void next()
   {
      humanoidJointPoseIndex.increment();
   }
   
   public void reset()
   {
      humanoidJointPoseIndex.set(0);
   }
   
   public boolean isDone()
   {
      return (humanoidJointPoseIndex.getIntegerValue() >= humanoidJointPoses.size());
   }
   
   public SideDependentList<double[]> getArmJointAngles()
   {
      HumanoidJointPose humanoidJointPose = humanoidJointPoses.get(humanoidJointPoseIndex.getIntegerValue());
      desiredArmPose.set(humanoidJointPose.getArmPose());
      return humanoidJointPose.getArmJointAngles();
   }
   
   public SideDependentList<double[]>getLegJointAngles()
   {
      HumanoidJointPose humanoidJointPose = humanoidJointPoses.get(humanoidJointPoseIndex.getIntegerValue());
      desiredLegPose.set(humanoidJointPose.getLegPose());
      return humanoidJointPose.getLegJointAngles();
   }
   
   public double[] getSpineJointAngles()
   {
      HumanoidJointPose humanoidJointPose = humanoidJointPoses.get(humanoidJointPoseIndex.getIntegerValue());
      desiredSpinePose.set(humanoidJointPose.getSpinePose());
      return humanoidJointPose.getSpineJointAngles();
   }
   
   private class HumanoidJointPose
   {
      private final HumanoidArmPose armPose;
      private final HumanoidSpinePose spinePose;
      private final HumanoidLegPose legPose;
      
      private final boolean hipRollSymmetric;
      
      public HumanoidJointPose(HumanoidArmPose armPose, HumanoidSpinePose spinePose, HumanoidLegPose legPose)
      {
         this(armPose, spinePose, legPose, true);
      }
      
      public HumanoidJointPose(HumanoidArmPose armPose, HumanoidSpinePose spinePose, HumanoidLegPose legPose, boolean hipRollSymmetric)
      {   
         this.armPose = armPose;
         this.spinePose = spinePose;
         this.legPose = legPose;

         this.hipRollSymmetric = hipRollSymmetric;
      }
      
      public HumanoidArmPose getArmPose()
      {
         return armPose;
      }
      
      public HumanoidSpinePose getSpinePose()
      {
         return spinePose;
      }
      
      public HumanoidLegPose getLegPose()
      {
         return legPose;
      }

      public SideDependentList<double[]> getArmJointAngles()
      {
         double[] leftArmJointAngles = armPose.getArmJointAngles();
         double[] rightArmJointAngles = armPose.getArmJointAngles();
         
         return new SideDependentList<double[]>(leftArmJointAngles, rightArmJointAngles);
      }
      
      public SideDependentList<double[]> getLegJointAngles()
      {
         double[] leftLegJointAngles = legPose.getLegJointAngles();
         double[] rightLegJointAngles = legPose.getLegJointAngles();
         
         if (!hipRollSymmetric)
         {
            rightLegJointAngles[1] = -1.0 * rightLegJointAngles[1];
         }
         
         return new SideDependentList<double[]>(leftLegJointAngles, rightLegJointAngles);
      }
      
      public double[] getSpineJointAngles()
      {
         return spinePose.getSpineJointAngles();  
      }

   }
   
   private void createPoseSetters()
   {
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_BACKWARD, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SMALL_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_MORE));
      
      // Low torque poses
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_A_LITTLE, HumanoidSpinePose.LEAN_LEFT, HumanoidLegPose.RELAXED_SLIGHTLY_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STRAIGHTEN_ELBOWS, HumanoidSpinePose.LEAN_LEFT, HumanoidLegPose.STAND_PREP_HIPS_OUT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SMALL_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_RIGHT, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.THIGHS_FORWARD_A_LITTLE_SLIGHLY_BENT_KNEES));
      
      // High torque on waist!
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD_A_LOT, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LOT));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STRAIGHTEN_ELBOWS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.SUPERMAN));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_A_LITTLE, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.SUPERMAN_BENT_KNEES));
      
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGER_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD, HumanoidLegPose.THIGHS_FORWARD_A_LITTLE_SLIGHLY_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.THIGHS_UP_STRAIGHT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGER_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.ARMS_OUT_EXTENDED, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.THIGHS_UP_BENT_KNEES_MORE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGER_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP_HIPS_OUT_A_BIT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_MORE, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.HIPS_OUT_A_BIT_ROTATED_OUT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_BACKWARD, HumanoidLegPose.LEGS_STRAIGHT_KNEES_FULLY_BENT));
      
      // High torque on waist!
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_A_LOT, HumanoidSpinePose.LEAN_FORWARD, HumanoidLegPose.STAND_PREP_LEGS_OUT_AND_FORWARD));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPER_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.LEGS_OUT_FORWARD_WITH_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_A_LOT, HumanoidSpinePose.LEAN_FORWARD, HumanoidLegPose.STAND_PREP_LEGS_OUT_AND_FORWARD));
      
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.LEGS_STRAIGHT_KNEES_FULLY_BENT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.RELAXED_SLIGHTLY_BENT_KNEES));
      humanoidJointPoses.add( new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.HIPS_OUT_MORE_SLIGHTLY_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP_HIPS_OUT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.STAND_PREP_HIPS_OUT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_LEFT, HumanoidLegPose.STAND_PREP_HIPS_OUT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.LEAN_RIGHT, HumanoidLegPose.STAND_PREP_HIPS_OUT_A_LITTLE));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.HIPS_IN_A_LOT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.HIPS_OUT_MORE_SLIGHTLY_BENT_KNEES));
      
      // High torque on arms!
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.ARMS_OUT_EXTENDED, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.STAND_PREP));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.FLYING, HumanoidSpinePose.LEAN_BACKWARD_A_LOT, HumanoidLegPose.STAND_PREP));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.FLYING_SUPPINATE_IN, HumanoidSpinePose.LEAN_LEFT, HumanoidLegPose.STAND_PREP));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.FLYING_SUPPINATE_OUT, HumanoidSpinePose.LEAN_RIGHT, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.TURN_LEFT, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.ARMS_OUT_EXTENDED, HumanoidSpinePose.TURN_RIGHT, HumanoidLegPose.STAND_PREP));   
      
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
   }
   
   
   private void createPoseSettersTuneWaist()
   {
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_LEFT, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_RIGHT, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD_A_LOT, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.STAND_PREP));
      
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.THIGHS_UP_BENT_KNEES));

//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_LEFT, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
//      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_RIGHT, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD_A_LOT, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.THIGHS_UP_BENT_KNEES));
      
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LOT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_BACKWARD_A_LOT, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LOT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LOT));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.LEAN_FORWARD_A_LOT, HumanoidLegPose.THIGHS_BACK_AND_STRAIGHT_A_LOT));
      

   }
   
   
   private void createPoseSettersJustArms()
   {
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.REACH_BACK, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.REACH_WAY_BACK, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.ARMS_03, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.REACH_FORWARD, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SMALL_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGE_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STRAIGHTEN_ELBOWS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_A_LITTLE, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.ARMS_BACK, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.LARGER_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.ARMS_OUT_EXTENDED, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_MORE, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPPINATE_ARMS_IN_A_LOT, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.SUPER_CHICKEN_WINGS, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.FLYING, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.FLYING_SUPPINATE_IN, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.FLYING_SUPPINATE_OUT, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose(HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
   }
   
   private void createPoseSettersTuneElbowComParameters()
   {
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.STAND_PREP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
//      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_STRAIGHT_DOWN, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_DOWN, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_DOWN2, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_DOWN, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_FORWARD, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_FORWARD2, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_FORWARD, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_UP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_UP2, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_NINETY_ELBOW_UP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_UP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_UP2, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_UP3, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_UP, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_DOWN, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_DOWN2, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_DOWN3, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_FORTFIVE_ELBOW_DOWN, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
      humanoidJointPoses.add(new HumanoidJointPose( HumanoidArmPose.ARM_OUT_TRICEP_EXERCISE, HumanoidSpinePose.STAND_PREP, HumanoidLegPose.STAND_PREP));
   }

}
