
package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters;

import controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket;

public interface FootstepPlannerParametersBasics extends FootstepPlannerParameters
{
   void setMaximumFrontStepReach(double maximumStepReach);

   void setMaximumFrontStepLength(double maximumStepLength);

   void setMinimumFrontStepLength(double minimumStepLength);

   void setMaximumHindStepReach(double maximumStepReach);

   void setMaximumHindStepLength(double maximumStepLength);

   void setMinimumHindStepLength(double minimumStepLength);

   void setMaximumStepWidth(double maximumStepWidth);

   void setMinimumStepWidth(double minimumStepWidth);

   void setMaximumFrontStepLengthWhenSteppingUp(double maximumStepLength);

   void setMinimumFrontStepLengthWhenSteppingUp(double minimumStepLength);

   void setMaximumHindStepLengthWhenSteppingUp(double maximumStepLength);

   void setMinimumHindStepLengthWhenSteppingUp(double minimumStepLength);

   void setStepZForSteppingUp(double stepZ);

   void setMaximumFrontStepLengthWhenSteppingDown(double maximumStepLength);

   void setMinimumFrontStepLengthWhenSteppingDown(double minimumStepLength);

   void setMaximumHindStepLengthWhenSteppingDown(double maximumStepLength);

   void setMinimumHindStepLengthWhenSteppingDown(double minimumStepLength);

   void setStepZForSteppingDown(double stepZ);

   void setMinimumStepYaw(double minimumStepYaw);

   void setMaximumStepYaw(double maximumStepYaw);

   void setMaximumStepChangeZ(double maximumStepChangeZ);

   void setBodyGroundClearance(double bodyGroundClearance);

   void setDistanceHeuristicWeight(double distanceHeuristicWeight);

   void setYawWeight(double yawWeight);

   void setXGaitWeight(double xGaitWeight);

   void setCostPerStep(double costPerStep);

   void setStepUpWeight(double stepUpWeight);

   void setStepDownWeight(double stepDownWeight);

   void setHeuristicsInflationWeight(double heuristicsInflationWeight);

   void setMinXClearanceFromFoot(double minXClearanceFromFoot);

   void setMinYClearanceFromFoot(double minYClearanceFromFoot);

   void setMaxWalkingSpeedMultiplier(double multiplier);

   void setProjectInsideDistanceForExpansion(double projectionInsideDistanceForExpansion);

   void setProjectInsideDistanceForPostProcessing(double projectionInsideDistanceForPostProcessing);

   void setProjectInsideUsingConvexHullDuringExpansion(boolean projectInsideUsingConvexHull);

   void setProjectInsideUsingConvexHullDuringPostProcessing(boolean projectInsideUsingConvexHull);

   void setMaximumXYWiggleDistance(double wiggleDistance);

   void setMinimumSurfaceInclineRadians(double minimumSurfaceIncline);

   void setCliffHeightToAvoid(double cliffHeightToAvoid);

   void setMinimumFrontEndForwardDistanceFromCliffBottoms(double distance);

   void setMinimumFrontEndBackwardDistanceFromCliffBottoms(double distance);

   void setMinimumHindEndForwardDistanceFromCliffBottoms(double distance);

   void setMinimumHindEndBackwardDistanceFromCliffBottoms(double distance);

   void setMinimumLateralDistanceFromCliffBottoms(double distance);

   default void set(FootstepPlannerParameters other)
   {
      setMaximumFrontStepReach(other.getMaximumFrontStepReach());
      setMaximumFrontStepLength(other.getMaximumFrontStepLength());
      setMinimumFrontStepLength(other.getMinimumFrontStepLength());
      setMaximumHindStepReach(other.getMaximumHindStepReach());
      setMaximumHindStepLength(other.getMaximumHindStepLength());
      setMinimumHindStepLength(other.getMinimumHindStepLength());
      setMaximumFrontStepLengthWhenSteppingUp(other.getMaximumFrontStepLengthWhenSteppingUp());
      setMinimumFrontStepLengthWhenSteppingUp(other.getMinimumFrontStepLengthWhenSteppingUp());
      setMaximumHindStepLengthWhenSteppingUp(other.getMaximumHindStepLengthWhenSteppingUp());
      setMinimumHindStepLengthWhenSteppingUp(other.getMinimumHindStepLengthWhenSteppingUp());
      setStepZForSteppingUp(other.getStepZForSteppingUp());
      setMaximumFrontStepLengthWhenSteppingDown(other.getMaximumFrontStepLengthWhenSteppingDown());
      setMinimumFrontStepLengthWhenSteppingDown(other.getMinimumFrontStepLengthWhenSteppingDown());
      setMaximumHindStepLengthWhenSteppingDown(other.getMaximumHindStepLengthWhenSteppingDown());
      setMinimumHindStepLengthWhenSteppingDown(other.getMinimumHindStepLengthWhenSteppingDown());
      setStepZForSteppingDown(other.getStepZForSteppingDown());
      setMaximumStepWidth(other.getMaximumStepWidth());
      setMinimumStepWidth(other.getMinimumStepWidth());
      setMinimumStepYaw(other.getMinimumStepYaw());
      setMaximumStepYaw(other.getMaximumStepYaw());
      setMaximumStepChangeZ(other.getMaximumStepChangeZ());
      setBodyGroundClearance(other.getBodyGroundClearance());
      setDistanceHeuristicWeight(other.getDistanceHeuristicWeight());
      setYawWeight(other.getYawWeight());
      setXGaitWeight(other.getXGaitWeight());
      setCostPerStep(other.getCostPerStep());
      setStepUpWeight(other.getStepUpWeight());
      setStepDownWeight(other.getStepDownWeight());
      setHeuristicsInflationWeight(other.getHeuristicsInflationWeight());
      setMinXClearanceFromFoot(other.getMinXClearanceFromFoot());
      setMinYClearanceFromFoot(other.getMinYClearanceFromFoot());
      setMaxWalkingSpeedMultiplier(other.getMaxWalkingSpeedMultiplier());
      setProjectInsideDistanceForExpansion(other.getProjectInsideDistanceForExpansion());
      setProjectInsideDistanceForPostProcessing(other.getProjectInsideDistanceForPostProcessing());
      setProjectInsideUsingConvexHullDuringExpansion(other.getProjectInsideUsingConvexHullDuringExpansion());
      setProjectInsideUsingConvexHullDuringPostProcessing(other.getProjectInsideUsingConvexHullDuringPostProcessing());
      setMaximumXYWiggleDistance(other.getMaximumXYWiggleDistance());
      setMinimumSurfaceInclineRadians(other.getMinimumSurfaceInclineRadians());
      setCliffHeightToAvoid(other.getCliffHeightToAvoid());
      setMinimumFrontEndForwardDistanceFromCliffBottoms(other.getMinimumFrontEndForwardDistanceFromCliffBottoms());
      setMinimumFrontEndBackwardDistanceFromCliffBottoms(other.getMinimumFrontEndBackwardDistanceFromCliffBottoms());
      setMinimumHindEndForwardDistanceFromCliffBottoms(other.getMinimumHindEndForwardDistanceFromCliffBottoms());
      setMinimumHindEndBackwardDistanceFromCliffBottoms(other.getMinimumHindEndBackwardDistanceFromCliffBottoms());
      setMinimumLateralDistanceFromCliffBottoms(other.getMinimumLateralDistanceFromCliffBottoms());
   }

   default void set(QuadrupedFootstepPlannerParametersPacket other)
   {
      if (other.getMaximumFrontStepReach() != other.NO_VALUE_DOUBLE)
         setMaximumFrontStepReach(other.getMaximumFrontStepReach());
      if (other.getMaximumFrontStepLength() != other.NO_VALUE_DOUBLE)
         setMaximumFrontStepLength(other.getMaximumFrontStepLength());
      if (other.getMinimumFrontStepLength() != other.NO_VALUE_DOUBLE)
         setMinimumFrontStepLength(other.getMinimumFrontStepLength());
      if (other.getMaximumHindStepReach() != other.NO_VALUE_DOUBLE)
         setMaximumHindStepReach(other.getMaximumHindStepReach());
      if (other.getMaximumHindStepLength() != other.NO_VALUE_DOUBLE)
         setMaximumHindStepLength(other.getMaximumHindStepLength());
      if (other.getMinimumHindStepLength() != other.NO_VALUE_DOUBLE)
         setMinimumHindStepLength(other.getMinimumHindStepLength());
      if (other.getMaximumFrontStepLengthWhenSteppingUp() != other.NO_VALUE_DOUBLE)
         setMaximumFrontStepLengthWhenSteppingUp(other.getMaximumFrontStepLengthWhenSteppingUp());
      if (other.getMinimumFrontStepLengthWhenSteppingUp() != other.NO_VALUE_DOUBLE)
         setMinimumFrontStepLengthWhenSteppingUp(other.getMinimumFrontStepLengthWhenSteppingUp());
      if (other.getMaximumHindStepLengthWhenSteppingUp() != other.NO_VALUE_DOUBLE)
         setMaximumHindStepLengthWhenSteppingUp(other.getMaximumHindStepLengthWhenSteppingUp());
      if (other.getMinimumHindStepLengthWhenSteppingUp() != other.NO_VALUE_DOUBLE)
         setMinimumHindStepLengthWhenSteppingUp(other.getMinimumHindStepLengthWhenSteppingUp());
      if (other.getStepZForSteppingUp() != other.NO_VALUE_DOUBLE)
         setStepZForSteppingUp(other.getStepZForSteppingUp());
      if (other.getMaximumFrontStepLengthWhenSteppingDown() != other.NO_VALUE_DOUBLE)
         setMaximumFrontStepLengthWhenSteppingDown(other.getMaximumFrontStepLengthWhenSteppingDown());
      if (other.getMinimumFrontStepLengthWhenSteppingDown() != other.NO_VALUE_DOUBLE)
         setMinimumFrontStepLengthWhenSteppingDown(other.getMinimumFrontStepLengthWhenSteppingDown());
      if (other.getMaximumHindStepLengthWhenSteppingDown() != other.NO_VALUE_DOUBLE)
         setMaximumHindStepLengthWhenSteppingDown(other.getMaximumHindStepLengthWhenSteppingDown());
      if (other.getMinimumHindStepLengthWhenSteppingDown() != other.NO_VALUE_DOUBLE)
         setMinimumHindStepLengthWhenSteppingDown(other.getMinimumHindStepLengthWhenSteppingDown());
      if (other.getStepZForSteppingDown() != other.NO_VALUE_DOUBLE)
         setStepZForSteppingDown(other.getStepZForSteppingDown());
      if (other.getMaximumStepWidth() != other.NO_VALUE_DOUBLE)
         setMaximumStepWidth(other.getMaximumStepWidth());
      if (other.getMinimumStepWidth() != other.NO_VALUE_DOUBLE)
         setMinimumStepWidth(other.getMinimumStepWidth());
      if (other.getMinimumStepYaw() != other.NO_VALUE_DOUBLE)
         setMinimumStepYaw(other.getMinimumStepYaw());
      if (other.getMaximumStepYaw() != other.NO_VALUE_DOUBLE)
         setMaximumStepYaw(other.getMaximumStepYaw());
      if (other.getMaximumStepChangeZ() != other.NO_VALUE_DOUBLE)
         setMaximumStepChangeZ(other.getMaximumStepChangeZ());
      if (other.getBodyGroundClearance() != other.NO_VALUE_DOUBLE)
         setBodyGroundClearance(other.getBodyGroundClearance());
      if (other.getMaxWalkingSpeedMultiplier() != other.NO_VALUE_DOUBLE)
         setMaxWalkingSpeedMultiplier(other.getMaxWalkingSpeedMultiplier());
      if (other.getDistanceHeuristicWeight() != other.NO_VALUE_DOUBLE)
         setDistanceHeuristicWeight(other.getDistanceHeuristicWeight());
      if (other.getYawWeight() != other.NO_VALUE_DOUBLE)
         setYawWeight(other.getYawWeight());
      if (other.getXGaitWeight() != other.NO_VALUE_DOUBLE)
         setXGaitWeight(other.getXGaitWeight());
      if (other.getCostPerStep() != other.NO_VALUE_DOUBLE)
         setCostPerStep(other.getCostPerStep());
      if (other.getStepUpWeight() != other.NO_VALUE_DOUBLE)
         setStepUpWeight(other.getStepUpWeight());
      if (other.getStepDownWeight() != other.NO_VALUE_DOUBLE)
         setStepDownWeight(other.getStepDownWeight());
      if (other.getHeuristicsWeight() != other.NO_VALUE_DOUBLE)
         setHeuristicsInflationWeight(other.getHeuristicsWeight());
      if (other.getMinXClearanceFromFoot() != other.NO_VALUE_DOUBLE)
         setMinXClearanceFromFoot(other.getMinXClearanceFromFoot());
      if (other.getMinYClearanceFromFoot() != other.NO_VALUE_DOUBLE)
         setMinYClearanceFromFoot(other.getMinYClearanceFromFoot());
      if (other.getProjectionInsideDistanceForExpansion() != other.NO_VALUE_DOUBLE)
         setProjectInsideDistanceForExpansion(other.getProjectionInsideDistanceForExpansion());
      if (other.getProjectionInsideDistanceForPostProcessing() != other.NO_VALUE_DOUBLE)
         setProjectInsideDistanceForPostProcessing(other.getProjectionInsideDistanceForPostProcessing());
      if (other.getMaximumXyWiggleDistance() != other.NO_VALUE_DOUBLE)
         setMaximumXYWiggleDistance(other.getMaximumXyWiggleDistance());
      if (other.getMinimumSurfaceInclineRadians() != other.NO_VALUE_DOUBLE)
         setMinimumSurfaceInclineRadians(other.getMinimumSurfaceInclineRadians());
      if (other.getCliffHeightToAvoid() != other.NO_VALUE_DOUBLE)
         setCliffHeightToAvoid(other.getCliffHeightToAvoid());
      if (other.getMinimumFrontEndForwardDistanceFromCliffBottoms() != other.NO_VALUE_DOUBLE)
         setMinimumFrontEndForwardDistanceFromCliffBottoms(other.getMinimumFrontEndForwardDistanceFromCliffBottoms());
      if (other.getMinimumFrontEndBackwardDistanceFromCliffBottoms() != other.NO_VALUE_DOUBLE)
         setMinimumFrontEndBackwardDistanceFromCliffBottoms(other.getMinimumFrontEndBackwardDistanceFromCliffBottoms());
      if (other.getMinimumHindEndForwardDistanceFromCliffBottoms() != other.NO_VALUE_DOUBLE)
         setMinimumHindEndForwardDistanceFromCliffBottoms(other.getMinimumHindEndForwardDistanceFromCliffBottoms());
      if (other.getMinimumHindEndBackwardDistanceFromCliffBottoms() != other.NO_VALUE_DOUBLE)
         setMinimumHindEndBackwardDistanceFromCliffBottoms(other.getMinimumHindEndBackwardDistanceFromCliffBottoms());
      if (other.getMinimumLateralDistanceFromCliffBottoms() != other.NO_VALUE_DOUBLE)
         setMinimumLateralDistanceFromCliffBottoms(other.getMinimumLateralDistanceFromCliffBottoms());

      setProjectInsideUsingConvexHullDuringExpansion(other.getProjectInsideUsingConvexHullDuringExpansion());
      setProjectInsideUsingConvexHullDuringPostProcessing(other.getProjectInsideUsingConvexHullDuringPostProcessing());
   }
}
