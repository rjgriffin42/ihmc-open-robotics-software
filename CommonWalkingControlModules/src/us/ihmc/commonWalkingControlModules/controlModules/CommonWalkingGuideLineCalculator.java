package us.ihmc.commonWalkingControlModules.controlModules;


import us.ihmc.commonWalkingControlModules.bipedSupportPolygons.BipedSupportPolygons;
import us.ihmc.commonWalkingControlModules.controlModuleInterfaces.GuideLineCalculator;
import us.ihmc.commonWalkingControlModules.referenceFrames.CommonWalkingReferenceFrames;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.robotSide.SideDependentList;
import us.ihmc.utilities.math.geometry.FrameLineSegment2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.FramePoint2d;
import us.ihmc.utilities.math.geometry.FrameVector2d;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.yoUtilities.YoVariableRegistry;

import com.yobotics.simulationconstructionset.BooleanYoVariable;
import com.yobotics.simulationconstructionset.DoubleYoVariable;

public class CommonWalkingGuideLineCalculator implements GuideLineCalculator
{
   private final YoVariableRegistry registry = new YoVariableRegistry("GuideLineCalculator");
   private final SideDependentList<FrameLineSegment2d> guideLines = new SideDependentList<FrameLineSegment2d>();

   private final SideDependentList<ReferenceFrame> footZUpFrames;

   private final DoubleYoVariable captureForward = new DoubleYoVariable("captureForward", registry);
   private final DoubleYoVariable captureForwardOfSweet = new DoubleYoVariable("captureForwardOfSweet", registry);

   private final DoubleYoVariable velocityGainX = new DoubleYoVariable("velocityGainX", registry);
   private final DoubleYoVariable velocityGainY = new DoubleYoVariable("velocityGainY", registry);


//   private final double footLength, footBack;

   public CommonWalkingGuideLineCalculator(CommonWalkingReferenceFrames referenceFrames, BooleanYoVariable onFinalStep, YoVariableRegistry parentRegistry)
   {
//      this.footLength = footLength;
//      this.footBack = footBack;
      this.footZUpFrames = referenceFrames.getAnkleZUpReferenceFrames();
      parentRegistry.addChild(registry);
   }

   public FrameLineSegment2d getGuideLine(RobotSide supportLeg)
   {
      return guideLines.get(supportLeg);
   }

   public void reset()
   {
      throw new UnsupportedOperationException();
   }

   public void update(RobotSide supportLeg, BipedSupportPolygons bipedSupportPolygons, FramePoint2d capturePointInSupportFootZUp,
                      FramePoint finalDesiredSwingTarget, FrameVector2d desiredVelocity)
   {
      if (finalDesiredSwingTarget == null)
         throw new RuntimeException("finalDesiredSwingTarget == null");
      
      FramePoint2d supportFootSweetSpot = bipedSupportPolygons.getSweetSpotCopy(supportLeg);
      supportFootSweetSpot.changeFrame(footZUpFrames.get(supportLeg));
      supportFootSweetSpot.setX(supportFootSweetSpot.getX() + captureForwardOfSweet.getDoubleValue());

      FramePoint2d stepToLocation = finalDesiredSwingTarget.toFramePoint2d();
      stepToLocation.changeFrame(footZUpFrames.get(supportLeg));
      stepToLocation.setX(stepToLocation.getX() + captureForward.getDoubleValue());
      FrameLineSegment2d guideLine = new FrameLineSegment2d(supportFootSweetSpot, stepToLocation);

      guideLines.set(supportLeg, guideLine);
   }

   public void setParametersForR2()
   {
      velocityGainX.set(0.25);
      velocityGainY.set(0.05);
      captureForward.set(0.10); // 20);    // (0.08);
      captureForwardOfSweet.set(0.0); // 0.03);
   }
   
   public void setParametersForM2V2PushRecovery()
   {
      velocityGainX.set(0.0);
      velocityGainY.set(0.0);
      captureForward.set(0.0); // 0.08);    // 0.04; //0.08;
      captureForwardOfSweet.set(0.03);
   }

   public void setParametersForM2V2Walking()
   {
      velocityGainX.set(0.0); // 0.2);
      velocityGainY.set(0.05);
      captureForward.set(0.04); // 0.08);    // 0.04; //0.08;
      captureForwardOfSweet.set(0.0);
   }
}
