package us.ihmc.commonWalkingControlModules.desiredFootStep;

import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePose;

/**
 * This class is designed to score potential step locations
 */
public interface StepLocationScorer
{
   /**
    * This method will return a double between 0.0 and 1.0 that represents the score
    * 0.0 = BAD
    * 1.0 = GREAT
    * @param supportLeg RobotSide
    * @param desiredFootstep Footstep
    * @return double
    */
   public double getStepLocationScore(RobotSide supportLeg, FramePose desiredFramePose);

}
