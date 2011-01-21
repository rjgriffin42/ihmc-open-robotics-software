package us.ihmc.commonWalkingControlModules.controlModules.arm;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.SideDependentList;
import us.ihmc.commonWalkingControlModules.partNamesAndTorques.ArmJointName;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.sensors.ProcessedSensorsInterface;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class SwingArmWithOppositeFootControlModule extends PDArmControlModule
{
   private final CommonWalkingReferenceFrames referenceFrames;
   private final SideDependentList<ReferenceFrame> armAttachmentFrames;
   private final ArmJointName[] armJointNames;
   private final double armLength;
   private final double swingMultiplier;

   public SwingArmWithOppositeFootControlModule(ProcessedSensorsInterface processedSensors, CommonWalkingReferenceFrames referenceFrames, double controlDT, SideDependentList<ReferenceFrame> armAttachmentFrames, ArmJointName[] armJointNames, double armLength, double swingMultiplier, YoVariableRegistry parentRegistry)
   {
      super(processedSensors, controlDT, parentRegistry);
      this.referenceFrames = referenceFrames;
      this.armAttachmentFrames = new SideDependentList<ReferenceFrame>(armAttachmentFrames);
      this.armJointNames = armJointNames;
      this.armLength = armLength;
      this.swingMultiplier = swingMultiplier;
   }

   protected void computeDesireds()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         for (ArmJointName armJointName : armJointNames)
         {
            desiredArmPositions.get(robotSide).get(armJointName).set(0.0);
            desiredArmVelocities.get(robotSide).get(armJointName).set(0.0);
         }
         
         desiredArmPositions.get(robotSide).get(ArmJointName.ELBOW).set(-0.3); // bend elbow a little
         
         final RobotSide oppositeSide = robotSide.getOppositeSide();
         final ReferenceFrame oppositeFootFrame = referenceFrames.getFootFrame(oppositeSide);
         FramePoint oppositeFootPosition = new FramePoint(oppositeFootFrame);
         oppositeFootPosition.changeFrame(armAttachmentFrames.get(robotSide));
         
         double handX = oppositeFootPosition.getX();
         final double sine = MathTools.clipToMinMax(handX / armLength, -1.0, 1.0);
         double qShoulderPitch = -swingMultiplier * Math.asin(sine);
         desiredArmPositions.get(robotSide).get(ArmJointName.SHOULDER_PITCH).set(qShoulderPitch);
      }
   }

   protected void setGains()
   {
      for (RobotSide robotSide : RobotSide.values())
      {
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setProportionalGain(200.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setProportionalGain(100.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_YAW).setProportionalGain(100.0);
         armControllers.get(robotSide).get(ArmJointName.ELBOW).setProportionalGain(100.0);

         armControllers.get(robotSide).get(ArmJointName.SHOULDER_PITCH).setDerivativeGain(2.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_ROLL).setDerivativeGain(10.0);
         armControllers.get(robotSide).get(ArmJointName.SHOULDER_YAW).setDerivativeGain(10.0);
         armControllers.get(robotSide).get(ArmJointName.ELBOW).setDerivativeGain(10.0);
      }      
   }
}
