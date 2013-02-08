package us.ihmc.commonWalkingControlModules.desiredFootStep;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;

import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingControlModule;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.commonWalkingControlModules.terrain.VaryingStairGroundProfile;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

//TODO: currently only works when the stairs are oriented in the x direction in world frame
public class VaryingStairDesiredFootstepCalculator extends SimpleWorldDesiredFootstepCalculator
{
   private final DoubleYoVariable stepXAfterNosing = new DoubleYoVariable("stepXAfterNosing", registry);
   private final DoubleYoVariable stepLengthOnFlatGround = new DoubleYoVariable("stepLengthOnFlatGround", registry);
   private final DoubleYoVariable stepLengthMinimum = new DoubleYoVariable("stepLengthMinimum", registry);

   private final VaryingStairGroundProfile groundProfile;


   public VaryingStairDesiredFootstepCalculator(VaryingStairGroundProfile groundProfile, SideDependentList<ContactablePlaneBody> bipedFeet,
           CommonWalkingReferenceFrames referenceFrames, DesiredHeadingControlModule desiredHeadingControlModule, YoVariableRegistry parentRegistry)
   {
      super(bipedFeet, referenceFrames, desiredHeadingControlModule, parentRegistry);
      this.groundProfile = groundProfile;
   }

   @Override
   public void initializeDesiredFootstep(RobotSide supportLegSide)
   {
      Transform3D footToWorldTransform = referenceFrames.getFootFrame(supportLegSide).getTransformToDesiredFrame(ReferenceFrame.getWorldFrame());

      FramePoint maxStanceXPoint = DesiredFootstepCalculatorTools.computeMaxXPointInFrame(footToWorldTransform, bipedFeet.get(supportLegSide),
                                      ReferenceFrame.getWorldFrame());
      double maxStanceX = maxStanceXPoint.getX();

      double oneStepLookAheadX = maxStanceX + stepLengthOnFlatGround.getDoubleValue();
      double twoStepLookAheadX = oneStepLookAheadX + stepLengthOnFlatGround.getDoubleValue();

      int stepNumber = groundProfile.computeStepNumber(maxStanceX);
      double stepX = groundProfile.computeStepStartX(stepNumber);

      if (oneStepLookAheadX > stepX)
      {
         oneStepLookAheadX = stepX + stepXAfterNosing.getDoubleValue();
      }
      else
      {
         if (twoStepLookAheadX > stepX)
         {
            twoStepLookAheadX = stepX + stepXAfterNosing.getDoubleValue();
            oneStepLookAheadX = (twoStepLookAheadX + maxStanceX) / 2.0;
         }
      }

      double oneStepLookAheadZ = groundProfile.heightAt(oneStepLookAheadX, 0.0, 0.0);

      stepLength.set(oneStepLookAheadX - maxStanceX);

      double stanceZ = groundProfile.heightAt(maxStanceXPoint.getX(), maxStanceXPoint.getY(), maxStanceXPoint.getZ());
      stepHeight.set(oneStepLookAheadZ - stanceZ);

      FramePoint stanceAnkle = new FramePoint(referenceFrames.getFootFrame(supportLegSide));
      stanceAnkle.changeFrame(ReferenceFrame.getWorldFrame());


      // roll and pitch with respect to world, yaw with respect to other foot's yaw
      RobotSide swingLegSide = supportLegSide.getOppositeSide();
      double swingFootYaw = footstepOrientations.get(supportLegSide).getYaw().getDoubleValue() + stepYaw.getDoubleValue();
      double swingFootPitch = stepPitch.getDoubleValue();
      double swingFootRoll = stepRoll.getDoubleValue();
      footstepOrientations.get(swingLegSide).setYawPitchRoll(swingFootYaw, swingFootPitch, swingFootRoll);
      Matrix3d footToWorldRotation = new Matrix3d();
      footstepOrientations.get(swingLegSide).getMatrix3d(footToWorldRotation);
      double swingMinZWithRespectToAnkle = DesiredFootstepCalculatorTools.computeMinZWithRespectToAnkleInWorldFrame(footToWorldRotation,
                                              bipedFeet.get(swingLegSide));
      double swingMaxXWithRespectToAnkle = DesiredFootstepCalculatorTools.computeMaxXWithRespectToAnkleInFrame(footToWorldRotation,
                                              bipedFeet.get(swingLegSide), desiredHeadingControlModule.getDesiredHeadingFrame());


      double swingAnkleX = oneStepLookAheadX - swingMaxXWithRespectToAnkle; // + addToStepLength.getDoubleValue();
      double swingAnkleY = stanceAnkle.getY() + supportLegSide.negateIfLeftSide(stepWidth.getDoubleValue());
      double swingAnkleZ = oneStepLookAheadZ - swingMinZWithRespectToAnkle; // + addToStepHeight.getDoubleValue();

      FramePoint newFootstepPosition = new FramePoint(ReferenceFrame.getWorldFrame(), swingAnkleX, swingAnkleY, swingAnkleZ);
      footstepPositions.get(swingLegSide).set(newFootstepPosition);
   }

   @Override
   public void setupParametersForR2InverseDynamics()
   {
      stepXAfterNosing.set(0.05); //035);
      stepLengthOnFlatGround.set(0.35);
      stepLengthMinimum.set(0.15);
      stepWidth.set(0.2);
      stepYaw.set(0.0);
      stepPitch.set(0.2);
      stepRoll.set(0.0);
   }
}
