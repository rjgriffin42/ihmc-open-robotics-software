
//A Desired Footstep is always defined in the support foot frame

package us.ihmc.commonWalkingControlModules.desiredFootStep;

import us.ihmc.commonWalkingControlModules.desiredHeadingAndVelocity.DesiredHeadingControlModule;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.FrameVector;
import us.ihmc.utilities.math.geometry.Orientation;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;

public class SimpleDesiredFootstepCalculator implements DesiredFootstepCalculator
{
   private final YoVariableRegistry registry = new YoVariableRegistry("SimpleFootstepCalculator");

   private final DesiredHeadingControlModule desiredHeadingControlModule;

   private final SideDependentList<ReferenceFrame> ankleZUpFrames;

   private final DoubleYoVariable stepLength = new DoubleYoVariable("stepLength", registry);
   private final DoubleYoVariable stepWidth = new DoubleYoVariable("stepWidth", registry);
   private final DoubleYoVariable stepHeight = new DoubleYoVariable("stepHeight", registry);
   private final DoubleYoVariable stepYaw = new DoubleYoVariable("stepYaw", registry);
   private final DoubleYoVariable stepPitch = new DoubleYoVariable("stepPitch", registry);
   private final DoubleYoVariable stepRoll = new DoubleYoVariable("stepRoll", registry);

   public SimpleDesiredFootstepCalculator(SideDependentList<ReferenceFrame> ankleZUpFrames, DesiredHeadingControlModule desiredHeadingControlModule, YoVariableRegistry parentRegistry)
   {
      this.ankleZUpFrames = ankleZUpFrames;
      this.desiredHeadingControlModule = desiredHeadingControlModule;
      parentRegistry.addChild(registry);
   }

   public void initializeDesiredFootstep(RobotSide supportLegSide)
   {
      updateAndGetDesiredFootstep(supportLegSide);
   }

   public Footstep updateAndGetDesiredFootstep(RobotSide supportLegSide)
   {
      RobotSide swingLegSide = supportLegSide.getOppositeSide();

      // Footstep Frame
      ReferenceFrame supportAnkleZUpFrame = ankleZUpFrames.get(supportLegSide);
      ReferenceFrame desiredHeadingFrame = desiredHeadingControlModule.getDesiredHeadingFrame();

      // Footstep Position
      FramePoint footstepPosition = new FramePoint(supportAnkleZUpFrame);
      FrameVector footstepOffset = new FrameVector(desiredHeadingFrame, stepLength.getDoubleValue(), supportLegSide.negateIfLeftSide(stepWidth.getDoubleValue()), stepHeight.getDoubleValue());
      
      footstepOffset.changeFrame(supportAnkleZUpFrame);
      footstepPosition.add(footstepOffset); 

      // Footstep Orientation
      Orientation footstepOrientation = new Orientation(desiredHeadingFrame); 
      footstepOrientation.changeFrame(supportAnkleZUpFrame);
      
      // Create a foot Step Pose from Position and Orientation
      FramePose footstepPose = new FramePose(footstepPosition, footstepOrientation);
      Footstep desiredFootstep = new Footstep(swingLegSide, footstepPose);
      
      return desiredFootstep;
   }

   public void setupParametersForM2V2()
   {
      stepLength.set(0.32);
      stepWidth.set(0.2);
      stepHeight.set(0.0);
      stepYaw.set(0.0);
      stepPitch.set(-0.25);
      stepRoll.set(0.0);
   }

   public void setupParametersForR2()
   {
      stepLength.set(0.6);
      stepWidth.set(0.35);
      stepHeight.set(0.0);
      stepYaw.set(0.0);
      stepPitch.set(-0.25);
      stepRoll.set(0.0);
   }
}
