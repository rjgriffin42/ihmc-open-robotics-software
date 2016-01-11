package us.ihmc.valkyrie.parameters;

import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;

public class ValkyrieOrderedJointMap
{
   public final static int LeftHipYaw= 0;
   public final static int LeftHipRoll= 1;
   public final static int LeftHipPitch= 2;
   public final static int LeftKneePitch= 3;
   public final static int LeftAnklePitch= 4;
   public final static int LeftAnkleRoll= 5;
   public final static int RightHipYaw= 6;
   public final static int RightHipRoll= 7;
   public final static int RightHipPitch= 8;
   public final static int RightKneePitch= 9;
   public final static int RightAnklePitch= 10;
   public final static int RightAnkleRoll= 11;
   public final static int TorsoYaw= 12;
   public final static int TorsoPitch= 13;
   public final static int TorsoRoll= 14;
   public final static int LeftShoulderPitch= 15;
   public final static int LeftShoulderRoll= 16;
   public final static int LeftShoulderYaw= 17;
   public final static int LeftElbowPitch= 18;
   public final static int LeftForearmYaw= 19;
   public final static int LeftWristRoll= 20;
   public final static int LeftWristPitch= 21;
   public final static int LeftIndexFingerPitch1= 22;
   public final static int LeftIndexFingerPitch2= 23;
   public final static int LeftIndexFingerPitch3= 24;
   public final static int LeftMiddleFingerPitch1= 25;
   public final static int LeftMiddleFingerPitch2= 26;
   public final static int LeftMiddleFingerPitch3= 27;
   public final static int LeftPinkyPitch1= 28;
   public final static int LeftPinkyPitch2= 29;
   public final static int LeftPinkyPitch3= 30;
   public final static int LeftThumbRoll= 31;
   public final static int LeftThumbPitch1= 32;
   public final static int LeftThumbPitch2= 33;
   public final static int LeftThumbPitch3= 34;
   public final static int LowerNeckPitch= 35;
   public final static int NeckYaw= 36;
   public final static int UpperNeckPitch= 37;
   public final static int MultisenseSLSpinnyJointFrame= 38;
   public final static int RightShoulderPitch= 39;
   public final static int RightShoulderRoll= 40;
   public final static int RightShoulderYaw= 41;
   public final static int RightElbowPitch= 42;
   public final static int RightForearmYaw= 43;
   public final static int RightWristRoll= 44;
   public final static int RightWristPitch= 45;
   public final static int RightIndexFingerPitch1= 46;
   public final static int RightIndexFingerPitch2= 47;
   public final static int RightIndexFingerPitch3= 48;
   public final static int RightMiddleFingerPitch1= 49;
   public final static int RightMiddleFingerPitch2= 50;
   public final static int RightMiddleFingerPitch3= 51;
   public final static int RightPinkyPitch1= 52;
   public final static int RightPinkyPitch2= 53;
   public final static int RightPinkyPitch3= 54;
   public final static int RightThumbRoll= 55;
   public final static int RightThumbPitch1= 56;
   public final static int RightThumbPitch2= 57;
   public final static int RightThumbPitch3= 58;

   public final static int numberOfJoints = RightThumbPitch3 + 1;

   public static String[]  jointNames = new String[numberOfJoints];
   static
   {
      jointNames[LeftHipYaw] = "leftHipYaw";
      jointNames[LeftHipRoll] = "leftHipRoll";
      jointNames[LeftHipPitch] = "leftHipPitch";
      jointNames[LeftKneePitch] = "leftKneePitch";
      jointNames[LeftAnklePitch] = "leftAnklePitch";
      jointNames[LeftAnkleRoll] = "leftAnkleRoll";
      jointNames[RightHipYaw] = "rightHipYaw";
      jointNames[RightHipRoll] = "rightHipRoll";
      jointNames[RightHipPitch] = "rightHipPitch";
      jointNames[RightKneePitch] = "rightKneePitch";
      jointNames[RightAnklePitch] = "rightAnklePitch";
      jointNames[RightAnkleRoll] = "rightAnkleRoll";
      jointNames[TorsoYaw] = "torsoYaw";
      jointNames[TorsoPitch] = "torsoPitch";
      jointNames[TorsoRoll] = "torsoRoll";
      jointNames[LeftShoulderPitch] = "leftShoulderPitch";
      jointNames[LeftShoulderRoll] = "leftShoulderRoll";
      jointNames[LeftShoulderYaw] = "leftShoulderYaw";
      jointNames[LeftElbowPitch] = "leftElbowPitch";
      jointNames[LeftForearmYaw] = "leftForearmYaw";
      jointNames[LeftWristRoll] = "leftWristRoll";
      jointNames[LeftWristPitch] = "leftWristPitch";
      jointNames[LeftIndexFingerPitch1] = "leftIndexFingerPitch1";
      jointNames[LeftIndexFingerPitch2] = "leftIndexFingerPitch2";
      jointNames[LeftIndexFingerPitch3] = "leftIndexFingerPitch3";
      jointNames[LeftMiddleFingerPitch1] = "leftMiddleFingerPitch1";
      jointNames[LeftMiddleFingerPitch2] = "leftMiddleFingerPitch2";
      jointNames[LeftMiddleFingerPitch3] = "leftMiddleFingerPitch3";
      jointNames[LeftPinkyPitch1] = "leftPinkyPitch1";
      jointNames[LeftPinkyPitch2] = "leftPinkyPitch2";
      jointNames[LeftPinkyPitch3] = "leftPinkyPitch3";
      jointNames[LeftThumbRoll] = "leftThumbRoll";
      jointNames[LeftThumbPitch1] = "leftThumbPitch1";
      jointNames[LeftThumbPitch2] = "leftThumbPitch2";
      jointNames[LeftThumbPitch3] = "leftThumbPitch3";
      jointNames[LowerNeckPitch] = "lowerNeckPitch";
      jointNames[NeckYaw] = "neckYaw";
      jointNames[UpperNeckPitch] = "upperNeckPitch";
      jointNames[MultisenseSLSpinnyJointFrame] = "multisenseSLSpinnyJointFrame";
      jointNames[RightShoulderPitch] = "rightShoulderPitch";
      jointNames[RightShoulderRoll] = "rightShoulderRoll";
      jointNames[RightShoulderYaw] = "rightShoulderYaw";
      jointNames[RightElbowPitch] = "rightElbowPitch";
      jointNames[RightForearmYaw] = "rightForearmYaw";
      jointNames[RightWristRoll] = "rightWristRoll";
      jointNames[RightWristPitch] = "rightWristPitch";
      jointNames[RightIndexFingerPitch1] = "rightIndexFingerPitch1";
      jointNames[RightIndexFingerPitch2] = "rightIndexFingerPitch2";
      jointNames[RightIndexFingerPitch3] = "rightIndexFingerPitch3";
      jointNames[RightMiddleFingerPitch1] = "rightMiddleFingerPitch1";
      jointNames[RightMiddleFingerPitch2] = "rightMiddleFingerPitch2";
      jointNames[RightMiddleFingerPitch3] = "rightMiddleFingerPitch3";
      jointNames[RightPinkyPitch1] = "rightPinkyPitch1";
      jointNames[RightPinkyPitch2] = "rightPinkyPitch2";
      jointNames[RightPinkyPitch3] = "rightPinkyPitch3";
      jointNames[RightThumbRoll] = "rightThumbRoll";
      jointNames[RightThumbPitch1] = "rightThumbPitch1";
      jointNames[RightThumbPitch2] = "rightThumbPitch2";
      jointNames[RightThumbPitch3] = "rightThumbPitch3";
   }

   public static final SideDependentList<String[]> forcedSideDependentJointNames = new SideDependentList<String[]>();
   static
   {
      String[] jointNamesRight = new String[numberOfJoints];
      jointNamesRight[LeftHipYaw] = "rightHipYaw";
      jointNamesRight[LeftHipRoll] = "rightHipRoll";
      jointNamesRight[LeftHipPitch] = "rightHipPitch";
      jointNamesRight[LeftKneePitch] = "rightKneePitch";
      jointNamesRight[LeftAnklePitch] = "rightAnklePitch";
      jointNamesRight[LeftAnkleRoll] = "rightAnkleRoll";
      jointNamesRight[RightHipYaw] = "rightHipYaw";
      jointNamesRight[RightHipRoll] = "rightHipRoll";
      jointNamesRight[RightHipPitch] = "rightHipPitch";
      jointNamesRight[RightKneePitch] = "rightKneePitch";
      jointNamesRight[RightAnklePitch] = "rightAnklePitch";
      jointNamesRight[RightAnkleRoll] = "rightAnkleRoll";
      jointNamesRight[TorsoYaw] = "torsoYaw";
      jointNamesRight[TorsoPitch] = "torsoPitch";
      jointNamesRight[TorsoRoll] = "torsoRoll";
      jointNamesRight[LeftShoulderPitch] = "rightShoulderPitch";
      jointNamesRight[LeftShoulderRoll] = "rightShoulderRoll";
      jointNamesRight[LeftShoulderYaw] = "rightShoulderYaw";
      jointNamesRight[LeftElbowPitch] = "rightElbowPitch";
      jointNamesRight[LeftForearmYaw] = "rightForearmYaw";
      jointNamesRight[LeftWristRoll] = "rightWristRoll";
      jointNamesRight[LeftWristPitch] = "rightWristPitch";
      jointNamesRight[LeftIndexFingerPitch1] = "rightIndexFingerPitch1";
      jointNamesRight[LeftIndexFingerPitch2] = "rightIndexFingerPitch2";
      jointNamesRight[LeftIndexFingerPitch3] = "rightIndexFingerPitch3";
      jointNamesRight[LeftMiddleFingerPitch1] = "rightMiddleFingerPitch1";
      jointNamesRight[LeftMiddleFingerPitch2] = "rightMiddleFingerPitch2";
      jointNamesRight[LeftMiddleFingerPitch3] = "rightMiddleFingerPitch3";
      jointNamesRight[LeftPinkyPitch1] = "rightPinkyPitch1";
      jointNamesRight[LeftPinkyPitch2] = "rightPinkyPitch2";
      jointNamesRight[LeftPinkyPitch3] = "rightPinkyPitch3";
      jointNamesRight[LeftThumbRoll] = "rightThumbRoll";
      jointNamesRight[LeftThumbPitch1] = "rightThumbPitch1";
      jointNamesRight[LeftThumbPitch2] = "rightThumbPitch2";
      jointNamesRight[LeftThumbPitch3] = "rightThumbPitch3";
      jointNamesRight[LowerNeckPitch] = "lowerNeckPitch";
      jointNamesRight[NeckYaw] = "neckYaw";
      jointNamesRight[UpperNeckPitch] = "upperNeckPitch";
      jointNamesRight[MultisenseSLSpinnyJointFrame] = "multisenseSLSpinnyJointFrame";
      jointNamesRight[RightShoulderPitch] = "rightShoulderPitch";
      jointNamesRight[RightShoulderRoll] = "rightShoulderRoll";
      jointNamesRight[RightShoulderYaw] = "rightShoulderYaw";
      jointNamesRight[RightElbowPitch] = "rightElbowPitch";
      jointNamesRight[RightForearmYaw] = "rightForearmYaw";
      jointNamesRight[RightWristRoll] = "rightWristRoll";
      jointNamesRight[RightWristPitch] = "rightWristPitch";
      jointNamesRight[RightIndexFingerPitch1] = "rightIndexFingerPitch1";
      jointNamesRight[RightIndexFingerPitch2] = "rightIndexFingerPitch2";
      jointNamesRight[RightIndexFingerPitch3] = "rightIndexFingerPitch3";
      jointNamesRight[RightMiddleFingerPitch1] = "rightMiddleFingerPitch1";
      jointNamesRight[RightMiddleFingerPitch2] = "rightMiddleFingerPitch2";
      jointNamesRight[RightMiddleFingerPitch3] = "rightMiddleFingerPitch3";
      jointNamesRight[RightPinkyPitch1] = "rightPinkyPitch1";
      jointNamesRight[RightPinkyPitch2] = "rightPinkyPitch2";
      jointNamesRight[RightPinkyPitch3] = "rightPinkyPitch3";
      jointNamesRight[RightThumbRoll] = "rightThumbRoll";
      jointNamesRight[RightThumbPitch1] = "rightThumbPitch1";
      jointNamesRight[RightThumbPitch2] = "rightThumbPitch2";
      jointNamesRight[RightThumbPitch3] = "rightThumbPitch3";

      forcedSideDependentJointNames.put(RobotSide.RIGHT, jointNamesRight);

      String[] jointNamesLeft = new String[numberOfJoints];
      jointNamesLeft[LeftHipYaw] = "leftHipYaw";
      jointNamesLeft[LeftHipRoll] = "leftHipRoll";
      jointNamesLeft[LeftHipPitch] = "leftHipPitch";
      jointNamesLeft[LeftKneePitch] = "leftKneePitch";
      jointNamesLeft[LeftAnklePitch] = "leftAnklePitch";
      jointNamesLeft[LeftAnkleRoll] = "leftAnkleRoll";
      jointNamesLeft[RightHipYaw] = "leftHipYaw";
      jointNamesLeft[RightHipRoll] = "leftHipRoll";
      jointNamesLeft[RightHipPitch] = "leftHipPitch";
      jointNamesLeft[RightKneePitch] = "leftKneePitch";
      jointNamesLeft[RightAnklePitch] = "leftAnklePitch";
      jointNamesLeft[RightAnkleRoll] = "leftAnkleRoll";
      jointNamesLeft[TorsoYaw] = "torsoYaw";
      jointNamesLeft[TorsoPitch] = "torsoPitch";
      jointNamesLeft[TorsoRoll] = "torsoRoll";
      jointNamesLeft[LeftShoulderPitch] = "leftShoulderPitch";
      jointNamesLeft[LeftShoulderRoll] = "leftShoulderRoll";
      jointNamesLeft[LeftShoulderYaw] = "leftShoulderYaw";
      jointNamesLeft[LeftElbowPitch] = "leftElbowPitch";
      jointNamesLeft[LeftForearmYaw] = "leftForearmYaw";
      jointNamesLeft[LeftWristRoll] = "leftWristRoll";
      jointNamesLeft[LeftWristPitch] = "leftWristPitch";
      jointNamesLeft[LeftIndexFingerPitch1] = "leftIndexFingerPitch1";
      jointNamesLeft[LeftIndexFingerPitch2] = "leftIndexFingerPitch2";
      jointNamesLeft[LeftIndexFingerPitch3] = "leftIndexFingerPitch3";
      jointNamesLeft[LeftMiddleFingerPitch1] = "leftMiddleFingerPitch1";
      jointNamesLeft[LeftMiddleFingerPitch2] = "leftMiddleFingerPitch2";
      jointNamesLeft[LeftMiddleFingerPitch3] = "leftMiddleFingerPitch3";
      jointNamesLeft[LeftPinkyPitch1] = "leftPinkyPitch1";
      jointNamesLeft[LeftPinkyPitch2] = "leftPinkyPitch2";
      jointNamesLeft[LeftPinkyPitch3] = "leftPinkyPitch3";
      jointNamesLeft[LeftThumbRoll] = "leftThumbRoll";
      jointNamesLeft[LeftThumbPitch1] = "leftThumbPitch1";
      jointNamesLeft[LeftThumbPitch2] = "leftThumbPitch2";
      jointNamesLeft[LeftThumbPitch3] = "leftThumbPitch3";
      jointNamesLeft[LowerNeckPitch] = "lowerNeckPitch";
      jointNamesLeft[NeckYaw] = "neckYaw";
      jointNamesLeft[UpperNeckPitch] = "upperNeckPitch";
      jointNamesLeft[MultisenseSLSpinnyJointFrame] = "multisenseSLSpinnyJointFrame";
      jointNamesLeft[RightShoulderPitch] = "leftShoulderPitch";
      jointNamesLeft[RightShoulderRoll] = "leftShoulderRoll";
      jointNamesLeft[RightShoulderYaw] = "leftShoulderYaw";
      jointNamesLeft[RightElbowPitch] = "leftElbowPitch";
      jointNamesLeft[RightForearmYaw] = "leftForearmYaw";
      jointNamesLeft[RightWristRoll] = "leftWristRoll";
      jointNamesLeft[RightWristPitch] = "leftWristPitch";
      jointNamesLeft[RightIndexFingerPitch1] = "leftIndexFingerPitch1";
      jointNamesLeft[RightIndexFingerPitch2] = "leftIndexFingerPitch2";
      jointNamesLeft[RightIndexFingerPitch3] = "leftIndexFingerPitch3";
      jointNamesLeft[RightMiddleFingerPitch1] = "leftMiddleFingerPitch1";
      jointNamesLeft[RightMiddleFingerPitch2] = "leftMiddleFingerPitch2";
      jointNamesLeft[RightMiddleFingerPitch3] = "leftMiddleFingerPitch3";
      jointNamesLeft[RightPinkyPitch1] = "leftPinkyPitch1";
      jointNamesLeft[RightPinkyPitch2] = "leftPinkyPitch2";
      jointNamesLeft[RightPinkyPitch3] = "leftPinkyPitch3";
      jointNamesLeft[RightThumbRoll] = "leftThumbRoll";
      jointNamesLeft[RightThumbPitch1] = "leftThumbPitch1";
      jointNamesLeft[RightThumbPitch2] = "leftThumbPitch2";
      jointNamesLeft[RightThumbPitch3] = "leftThumbPitch3";

      forcedSideDependentJointNames.put(RobotSide.LEFT, jointNamesLeft);
   }
}
