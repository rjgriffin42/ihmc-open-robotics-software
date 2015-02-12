package us.ihmc.humanoidBehaviors.taskExecutor;

import us.ihmc.humanoidBehaviors.behaviors.primitives.WholeBodyInverseKinematicBehavior;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;

public class WholeBodyInverseKinematicTask extends BehaviorTask
{
   private final static ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final WholeBodyInverseKinematicBehavior wholeBodyIKBehavior;

   private final RobotSide robotSide;
   private final FramePose desiredHandPose;
   private final double trajectoryTime;

   public WholeBodyInverseKinematicTask(RobotSide robotSide, DoubleYoVariable yoTime, WholeBodyInverseKinematicBehavior wholeBodyIKBehavior,
         FramePose desiredHandPose, double trajectoryTime)
   {
      super(wholeBodyIKBehavior, yoTime);
      this.wholeBodyIKBehavior = wholeBodyIKBehavior;
      this.robotSide = robotSide;
      this.trajectoryTime = trajectoryTime;
      desiredHandPose.checkReferenceFrameMatch(worldFrame);
      this.desiredHandPose = desiredHandPose;
   }

   @Override
   protected void setBehaviorInput()
   {
      wholeBodyIKBehavior.setInputs(robotSide, desiredHandPose, trajectoryTime);
      wholeBodyIKBehavior.computeSolution();      
   }


}
