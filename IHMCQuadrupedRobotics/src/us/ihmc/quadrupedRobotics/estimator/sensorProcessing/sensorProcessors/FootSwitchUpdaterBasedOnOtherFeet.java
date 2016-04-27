package us.ihmc.quadrupedRobotics.estimator.sensorProcessing.sensorProcessors;

import us.ihmc.robotics.MathTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;

public class FootSwitchUpdaterBasedOnOtherFeet implements FootSwitchUpdater
{ 
   private static final double EPSILON = 1e-5;
   private QuadrantDependentList<FramePoint> footPositions = new QuadrantDependentList<FramePoint>();
   
   public FootSwitchUpdaterBasedOnOtherFeet(QuadrantDependentList<FramePoint> footPositions)
   {
      this.footPositions = footPositions;
   }

   @Override
   public boolean isFootInContactWithGround(RobotQuadrant footToBeChecked)
   {
      double footToBeCheckedZ = footPositions.get(footToBeChecked).getZ(); 
      double otherFeetZ = 0.0;
      
      for(RobotQuadrant robotQuadrant : RobotQuadrant.values())
      {
         if(robotQuadrant != footToBeChecked)
         {
            otherFeetZ += footPositions.get(robotQuadrant).getZ();
         }
         otherFeetZ /= 3.0;
      }
     
      if(MathTools.epsilonEquals(footToBeCheckedZ, otherFeetZ, EPSILON))
         return true;
      
      return false;
   }
}


