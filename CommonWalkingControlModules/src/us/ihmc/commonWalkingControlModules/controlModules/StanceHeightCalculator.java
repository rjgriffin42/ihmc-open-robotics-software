package us.ihmc.commonWalkingControlModules.controlModules;

import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.ReferenceFrame;
import us.ihmc.utilities.robotSide.RobotSide;
import us.ihmc.utilities.robotSide.SideDependentList;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.DoubleYoVariable;


public class StanceHeightCalculator
{
   private final YoVariableRegistry registry = new YoVariableRegistry("stanceHeight");
   private final SideDependentList<DoubleYoVariable> stanceHeight = new SideDependentList<DoubleYoVariable>();
   private final SideDependentList<ReferenceFrame> footFrames;
   private final ReferenceFrame upperBodyFrame;

   public StanceHeightCalculator(SideDependentList<ReferenceFrame> footFrames, ReferenceFrame upperBodyFrame, YoVariableRegistry parentRegistry)
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         DoubleYoVariable doubleYoVariable = new DoubleYoVariable("stanceHeight" + robotSide.getCamelCaseNameForMiddleOfExpression(), registry);
         stanceHeight.put(robotSide, doubleYoVariable);
      }
      
      this.footFrames = new SideDependentList<ReferenceFrame>(footFrames);
      this.upperBodyFrame = upperBodyFrame;

      if (parentRegistry != null)
         parentRegistry.addChild(registry);
   }


   public double getStanceHeightUsingOneFoot(RobotSide sideToGetStanceHeightFor)
   {
      FramePoint footOriginPosition = new FramePoint(footFrames.get(sideToGetStanceHeightFor));
      footOriginPosition.changeFrame(upperBodyFrame);

      // Now make a vector of it:
      FrameVector upperBodyToFoot = new FrameVector(footOriginPosition);

      // Get Z
      upperBodyToFoot.changeFrame(ReferenceFrame.getWorldFrame());

      double ret = -upperBodyToFoot.getZ();
      stanceHeight.get(sideToGetStanceHeightFor).set(ret);

      return ret;
   }

   public double getStanceHeightUsingBothFeet()
   {
      double sum = 0.0;
      for (RobotSide robotSide : RobotSide.values)
      {
         sum += getStanceHeightUsingOneFoot(robotSide);
      }

      return sum / RobotSide.values.length;
   }
}
