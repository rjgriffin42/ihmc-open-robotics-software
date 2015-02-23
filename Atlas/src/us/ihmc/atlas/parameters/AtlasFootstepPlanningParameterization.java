package us.ihmc.atlas.parameters;


import java.util.ArrayList;
import java.util.List;

import us.ihmc.pathGeneration.footstepPlanner.FootstepPlanningParameterization;
import us.ihmc.pathGeneration.footstepPlanner.FootstepPlanState;

public class AtlasFootstepPlanningParameterization extends FootstepPlanningParameterization
{
   private static final double goalRadius = 0.25;
   private final double baseOffset = 0.3;
   public double maxStepUp = 0.20;
   public double minStepDown = -0.17;
   public double maxStepDistance = 0.6;
   public double dangerDistance = 0.75;

   private double yawCostGain = 1;

   public AtlasFootstepPlanningParameterization()
   {
      initialize();
   }

   private void initialize()
   {
      setOffsets();
      footWidth = .14;
      footLength = .26;
      cropWidth = 0.085;
      cropLength = 0.22;
      maxSupportPolygonArea = cropWidth * cropLength;
      //minSupportPolygonArea = maxSupportPolygonArea/2;
      minSupportPolygonArea = maxSupportPolygonArea * .99;

   }

   private void setOffsets()
   {
      FootstepOffset sidestep = new FootstepOffset(0, .25, 0);
      List<FootstepOffset> offsets = new ArrayList<FootstepOffset>();
      offsets.add(sidestep);
      double[] xs = new double[]{0.0, 0.0, 0.0, 0.0, 0.3, 0.4, 0.6, -0.3, 0.4};
      double[] ys = new double[]{0.16, 0.6, 0.4, 0.25, 0.25, 0.25, 0.25, 0.25, 0.4};
      double[] thetas = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

      double[] thetaForwards = new double[]{0, Math.PI / 16, Math.PI / 8, Math.PI / 4, 3 * Math.PI / 8};
      double[] thetaBackwards = new double[]{-(Math.PI / 16), 0};

      for (int i = 0; i < xs.length; i++)
      {
         offsets.add(new FootstepOffset(xs[i], ys[i], thetas[i]));
      }
      for (double theta : thetaForwards)
      {
         offsets.add(new FootstepOffset(.3 * Math.cos(theta), .25 + .3 * Math.sin(theta), theta));
      }
      for (double theta : thetaBackwards)
      {
         offsets.add(new FootstepOffset(-.3 * Math.cos(theta), .25 + -.3 * Math.sin(theta), theta));
      }

      offsetList = offsets;
   }

   @Override
   public FootstepOffset getSidestep(FootstepPlanState currentState)
   {
      return offsetList.get(0);
   }

   @Override
   public boolean withinReachForNext(double xdiff, double ydiff, double thetadiff)
   {
      if (xdiff > 0.4 || xdiff < -0.2) return false;
      if (ydiff > 0.4 || ydiff < footWidth + 0.02) return false;
      if (thetadiff > Math.PI / 4 || thetadiff < 0) return false;
      double xObtrustion = footLength / 2 * Math.cos(thetadiff) + footWidth / 2 * Math.sin(thetadiff);
      if (xObtrustion > ydiff + (footWidth + 0.02) / 2) return false; //corner intersection between feet
      return true;
   }

   public double getMaxStepUp()
   {
      return this.maxStepUp;
   }

   public double getMinStepDown()
   {
      return minStepDown;
   }

   public double getMaxStepDistance()
   {
      return maxStepDistance;
   }

   @Override
   public double getDangerDistance()
   {
      return dangerDistance;
   }
}
