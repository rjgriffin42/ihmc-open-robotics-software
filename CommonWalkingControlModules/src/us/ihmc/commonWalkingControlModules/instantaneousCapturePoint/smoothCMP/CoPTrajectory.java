package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.smoothCMP;

import us.ihmc.commonWalkingControlModules.configurations.CoPSplineType;
import us.ihmc.commons.PrintTools;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.yoVariables.registry.YoVariableRegistry;

public class CoPTrajectory extends YoSegmentedFrameTrajectory3D implements CoPTrajectoryInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String name;
   private final CoPSplineType splineType;
   private final WalkingTrajectoryType trajectoryType;
   private final int stepNumber;

   public CoPTrajectory(String namePrefix, int stepNumber, CoPSplineType splineType, int maxNumberOfSegments, WalkingTrajectoryType type,
                        YoVariableRegistry registry)
   {      
      super(namePrefix + stepNumber + type.toString(), maxNumberOfSegments, splineType.getNumberOfCoefficients(), registry);
      this.name = namePrefix + stepNumber + type.toString();
      this.splineType = splineType;
      this.trajectoryType = type;
      this.stepNumber = stepNumber;
   }

   public void setSegment(double initialTime, double finalTime, FramePoint initialPosition, FramePoint finalPosition)
   {
      //PrintTools.debug("Step:" + stepNumber + " " + trajectoryType.toString() + " Trajectory " + numberOfSegments.getIntegerValue() + " , InitialTime: " + initialTime + " FinalTime: " + finalTime + " InitialPosition: "
      //      + initialPosition.toString() + " FinalPosition: " + finalPosition.toString());
      switch (this.splineType)
      {
      case CUBIC:
         segments.get(numberOfSegments.getIntegerValue()).setCubic(initialTime, finalTime, initialPosition, finalPosition);
         break;

      default:
         segments.get(numberOfSegments.getIntegerValue()).setLinear(initialTime, finalTime, initialPosition, finalPosition);
         break;
      }
      numberOfSegments.increment();
   }
   
   public WalkingTrajectoryType getTrajectoryType()
   {
      return trajectoryType;
   }
   
   public int getStepNumber()
   {
      return stepNumber;
   }
}
