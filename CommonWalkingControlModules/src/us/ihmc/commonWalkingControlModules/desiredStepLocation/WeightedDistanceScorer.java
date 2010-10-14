package us.ihmc.commonWalkingControlModules.desiredStepLocation;


import javax.vecmath.Point2d;

import us.ihmc.commonWalkingControlModules.RobotSide;
import us.ihmc.commonWalkingControlModules.SideDependentList;
import us.ihmc.commonWalkingControlModules.captureRegion.CaptureRegionCalculator;
import us.ihmc.commonWalkingControlModules.desiredStepLocation.BasicReachableScorer;
import us.ihmc.commonWalkingControlModules.desiredStepLocation.Footstep;
import us.ihmc.utilities.math.geometry.BoundingBox2d;
import us.ihmc.utilities.math.geometry.FrameConvexPolygon2d;
import us.ihmc.utilities.math.geometry.FramePoint;
import us.ihmc.utilities.math.geometry.ReferenceFrame;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;


public class WeightedDistanceScorer extends BasicReachableScorer
{
   @SuppressWarnings("unused")
   private final YoVariableRegistry registry = new YoVariableRegistry("WeightedDistanceScorer");
   private final SideDependentList<ReferenceFrame> footZUpFrames;

   @SuppressWarnings("unused")
   private final DoubleYoVariable stanceWidth;
   @SuppressWarnings("unused")
   private final DoubleYoVariable stanceLength;
   private final DoubleYoVariable stepAngle;
   private final DoubleYoVariable stepDistance;

   private final double GREAT = 1.0;
   private final double GOOD = 0.8;
   private final double FAIR = 0.5;
   private final double POOR = 0.1;

   private final double GreatDX = 0.05;
   private final double GoodDX = 0.10;
   private final double FairDX = 0.15;

   private final double GreatDY = 0.03;
   @SuppressWarnings("unused")
   private final double GoodDY = 0.06;
   @SuppressWarnings("unused")
   private final double FairDY = 0.09;

   private double yWeight = GreatDX / GreatDY;


   public WeightedDistanceScorer(CaptureRegionCalculator captureRegionCalculator, SideDependentList<ReferenceFrame> footZUpFrames,
                                 DoubleYoVariable stanceWidth, DoubleYoVariable stanceLength, DoubleYoVariable stepAngle, DoubleYoVariable stepDistance)
   {
      super(captureRegionCalculator);
      this.footZUpFrames = footZUpFrames;
      this.stanceWidth = stanceWidth;
      this.stanceLength = stanceLength;
      this.stepAngle = stepAngle;
      this.stepDistance = stepDistance;
   }

   public double getStepLocationScore(RobotSide supportLeg, Footstep desiredFootstep)
   {
      // return 0.0 if unreachable
      double score = super.getStepLocationScore(supportLeg, desiredFootstep);
      if (score == 0.0)
         return score;

      // otherwise return distance score

      // find the scale (max possible distance)
      FrameConvexPolygon2d reachableRegion = captureRegionCalculator.getReachableRegion(supportLeg);
      Point2d minPoint = new Point2d();
      Point2d maxPoint = new Point2d();
      BoundingBox2d boundingBox = reachableRegion.getBoundingBox();

      boundingBox.getMinPoint(minPoint);
      boundingBox.getMaxPoint(maxPoint);

      @SuppressWarnings("unused") double scale = minPoint.distance(maxPoint);

      // find the nominal
      double sideDependentAngle = stepAngle.getDoubleValue();
      if (supportLeg == RobotSide.LEFT)
      {
         sideDependentAngle = -sideDependentAngle;
      }

      ReferenceFrame supportLegAnkleZUpFrame = footZUpFrames.get(supportLeg);
      FramePoint nominalSwingToPosition = new FramePoint(supportLegAnkleZUpFrame);
      nominalSwingToPosition.setX(nominalSwingToPosition.getX() + stepDistance.getDoubleValue() * Math.cos(sideDependentAngle));
      nominalSwingToPosition.setY(nominalSwingToPosition.getY() + stepDistance.getDoubleValue() * Math.sin(sideDependentAngle));

      // find the weighted distance from nominal
      double dx = Math.abs(nominalSwingToPosition.getX() - desiredFootstep.footstepPosition.getX());
      double dy = Math.abs(nominalSwingToPosition.getY() - desiredFootstep.footstepPosition.getY()) * yWeight;
      double weightedDistance = Math.sqrt((dx * dx) + (dy * dy));

      if (weightedDistance < GreatDX)
         score = GREAT;
      else if (weightedDistance < GoodDX)
         score = GOOD;
      else if (weightedDistance < FairDX)
         score = FAIR;
      else
         score = POOR;

      // make sure it is 0-1
      if (score < 0.0)
         score = 0.0;
      else if (score > 1.0)
         score = 1.0;

      return score;
   }
}
