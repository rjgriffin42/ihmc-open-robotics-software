package us.ihmc.quadrupedRobotics.planning.chooser.swingLegChooser;

import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.quadrupedBasics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class DefaultGaitSwingLegChooser implements NextSwingLegChooser
{
   @Override
   public RobotQuadrant chooseNextSwingLeg(QuadrupedSupportPolygon supportPolygon, RobotQuadrant lastStepQuadrant, FrameVector3DReadOnly desiredVelocity, double desiredYawRate)
   {
      if(desiredVelocity.getX() >= 0)
      {
         return lastStepQuadrant.getNextRegularGaitSwingQuadrant();
      }
      
      return lastStepQuadrant.getNextReversedRegularGaitSwingQuadrant();
   }

}
