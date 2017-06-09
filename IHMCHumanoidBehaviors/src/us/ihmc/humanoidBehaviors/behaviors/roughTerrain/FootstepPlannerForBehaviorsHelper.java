package us.ihmc.humanoidBehaviors.behaviors.roughTerrain;

import java.util.ArrayList;

import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.footstepPlanning.graphSearch.BipedalFootstepPlannerParameters;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.wholeBodyController.RobotContactPointParameters;

public class FootstepPlannerForBehaviorsHelper
{
   
   private static final double SCALING_FACTOR_FOR_FOOTHOLD_X = 1.1;
   private static final double SCALING_FACTOR_FOR_FOOTHOLD_Y = 2.6;
   
   public static SideDependentList<ConvexPolygon2D> createDefaultFootPolygonsForAnytimePlannerAndPlannerToolbox(RobotContactPointParameters contactPointParameters)
   {
      return createDefaultFootPolygons(contactPointParameters, SCALING_FACTOR_FOR_FOOTHOLD_X, SCALING_FACTOR_FOR_FOOTHOLD_Y);
   }

   public static SideDependentList<ConvexPolygon2D> createDefaultFootPolygons(RobotContactPointParameters contactPointParameters, double scalingFactorForFootholdX, double scalingFactorForFootholdY)
   {
      SideDependentList<ConvexPolygon2D> footPolygons = new SideDependentList<>();
      for (RobotSide side : RobotSide.values)
      {
         ArrayList<Point2D> footPoints = contactPointParameters.getFootContactPoints().get(side);         
         ArrayList<Point2D> scaledFootPoints = new ArrayList<Point2D>();
         
         for(int i = 0; i < footPoints.size(); i++)
         {
            Point2D footPoint = new Point2D(footPoints.get(i));
            footPoint.setX(footPoint.getX() * scalingFactorForFootholdX);
            footPoint.setY(footPoint.getY() * scalingFactorForFootholdY);
            scaledFootPoints.add(footPoint);
         }
         
         ConvexPolygon2D scaledFoot = new ConvexPolygon2D(scaledFootPoints);
         footPolygons.set(side, scaledFoot);         
      }
      
      return footPolygons;
   }
   
   public static void setPlannerParametersForAnytimePlannerAndPlannerToolbox(BipedalFootstepPlannerParameters footstepPlanningParameters)
   {
      footstepPlanningParameters.setMaximumStepReach(0.55);
      footstepPlanningParameters.setMaximumStepZ(0.28);

      footstepPlanningParameters.setMaximumStepXWhenForwardAndDown(0.35); //32);
      footstepPlanningParameters.setMaximumStepZWhenForwardAndDown(0.10); //18);

      footstepPlanningParameters.setMaximumStepYaw(0.15);
      footstepPlanningParameters.setMinimumStepWidth(0.16);
      footstepPlanningParameters.setMaximumStepWidth(0.4);
      footstepPlanningParameters.setMinimumStepLength(-0.01);

      footstepPlanningParameters.setMinimumFootholdPercent(0.95);

      footstepPlanningParameters.setWiggleInsideDelta(0.02);
      footstepPlanningParameters.setMaximumXYWiggleDistance(1.0);
      footstepPlanningParameters.setMaximumYawWiggle(0.1);
      footstepPlanningParameters.setRejectIfCannotFullyWiggleInside(true);
      footstepPlanningParameters.setWiggleIntoConvexHullOfPlanarRegions(true);

      footstepPlanningParameters.setCliffHeightToShiftAwayFrom(0.03);
      footstepPlanningParameters.setMinimumDistanceFromCliffBottoms(0.24);
      
      footstepPlanningParameters.setPerformYawExploration(false);
      footstepPlanningParameters.setRandomizeMagnitudes(0.0, 0.0);
      
      double idealFootstepLength = 0.3;
      double idealFootstepWidth = 0.22;
      footstepPlanningParameters.setIdealFootstep(idealFootstepLength, idealFootstepWidth);
   }

   public static void setPlannerParametersForValkyrieSteppingStones(BipedalFootstepPlannerParameters footstepPlanningParameters)
   {
      footstepPlanningParameters.setMaximumStepReach(0.70);
      footstepPlanningParameters.setMaximumStepZ(0.08);

      footstepPlanningParameters.setMaximumStepXWhenForwardAndDown(0.35); //32);
      footstepPlanningParameters.setMaximumStepZWhenForwardAndDown(0.10); //18);

      footstepPlanningParameters.setMaximumStepYaw(0.4);
      footstepPlanningParameters.setMinimumStepWidth(0.16);
      footstepPlanningParameters.setMaximumStepWidth(0.6);
      footstepPlanningParameters.setMinimumStepLength(-0.01);

      footstepPlanningParameters.setMinimumFootholdPercent(0.95);

      footstepPlanningParameters.setWiggleInsideDelta(0.02);
      footstepPlanningParameters.setMaximumXYWiggleDistance(1.0);
      footstepPlanningParameters.setMaximumYawWiggle(0.4);
      footstepPlanningParameters.setRejectIfCannotFullyWiggleInside(true);
      footstepPlanningParameters.setWiggleIntoConvexHullOfPlanarRegions(true);

      footstepPlanningParameters.setCliffHeightToShiftAwayFrom(0.03);
      footstepPlanningParameters.setMinimumDistanceFromCliffBottoms(0.24);
      
      footstepPlanningParameters.setPerformYawExploration(true);
      footstepPlanningParameters.setRandomizeMagnitudes(0.25, 0.25);
      
      double idealFootstepLength = 0.5;
      double idealFootstepWidth = 0.3;
      footstepPlanningParameters.setIdealFootstep(idealFootstepLength, idealFootstepWidth);
   }

   public static SideDependentList<ConvexPolygon2D> createDefaultFootPolygonsForValkyrieSteppingStones(RobotContactPointParameters contactPointParameters)
   {
      return createDefaultFootPolygons(contactPointParameters, 0.8, 0.8);
   }
}
