package us.ihmc.quadrupedFootstepPlanning.footstepPlanning.graphSearch.parameters;

import controller_msgs.msg.dds.QuadrupedFootstepPlannerParametersPacket;

public interface FootstepPlannerParameters
{
   /**
    * The total maximum Euclidean distance length.
    */
   double getMaximumFrontStepReach();

   double getMaximumFrontStepLength();

   double getMinimumFrontStepLength();

   default double getMaximumHindStepReach()
   {
      return getMaximumFrontStepReach();
   }

   default double getMaximumHindStepLength()
   {
      return getMaximumFrontStepLength();
   }

   default double getMinimumHindStepLength()
   {
      return getMinimumFrontStepLength();
   }

   double getMaximumStepOutward();

   double getMaximumStepInward();

   default double getMaximumFrontStepLengthWhenSteppingUp()
   {
      return getMaximumFrontStepLength();
   }

   default double getMinimumFrontStepLengthWhenSteppingUp()
   {
      return getMinimumFrontStepLength();
   }

   default double getMaximumHindStepLengthWhenSteppingUp()
   {
      return getMaximumHindStepLength();
   }

   default double getMinimumHindStepLengthWhenSteppingUp()
   {
      return getMinimumHindStepLength();
   }

   default double getStepZForSteppingUp()
   {
      return Double.POSITIVE_INFINITY;
   }

   default double getMaximumFrontStepLengthWhenSteppingDown()
   {
      return getMaximumFrontStepLength();
   }

   default double getMinimumFrontStepLengthWhenSteppingDown()
   {
      return getMinimumFrontStepLength();
   }

   default double getMaximumHindStepLengthWhenSteppingDown()
   {
      return getMaximumHindStepLength();
   }

   default double getMinimumHindStepLengthWhenSteppingDown()
   {
      return getMinimumHindStepLength();
   }

   default double getStepZForSteppingDown()
   {
      return Double.NEGATIVE_INFINITY;
   }

   double getMaximumStepYawInward();

   double getMaximumStepYawOutward();

   double getMaximumStepChangeZ();

   double getBodyGroundClearance();

   double getDistanceWeight();

   double getYawWeight();

   double getXGaitWeight();

   double getDesiredVelocityWeight();

   double getCostPerStep();

   double getStepUpWeight();

   double getStepDownWeight();

   double getHeuristicsInflationWeight();

   double getMinXClearanceFromFoot();

   double getMinYClearanceFromFoot();

   default double getMaxWalkingSpeedMultiplier()
   {
      return 0.8;
   }

   /**
    * Distance which a foothold is projected into planar region during expansion and node checking. Should be a positive value,
    * e.g. 0.02 means footholds are projected 2cm inside. If this is a non-positive value then no projection is performed.
    */
   double getProjectInsideDistance();

   boolean getProjectInsideUsingConvexHull();

   /***
    * Maximum distance that the snap and wiggler is allowed to wiggle the footstep node.
    */
   double getMaximumXYWiggleDistance();

   /**
    * The planner will ignore candidate footsteps if they are on a planar region with an incline that is higher
    * then the value specified here.
    *
    * <p>
    * More specifically, if a footstep has an associated planar region and that regions surface normal has a
    * z-value less than cos(minimumSurfaceInclineRadians), it will be rejected.
    * </p>
    */
   default double getMinimumSurfaceInclineRadians()
   {
      return Math.toRadians(45.0);
   }

   /**
    * The planner can be setup to avoid footsteps near the bottom of "cliffs". When the footstep has a planar region
    * nearby that is cliffHeightToShiftAwayFrom higher than the candidate footstep, it will move away from it
    * until it is minimumDistanceFromCliffBottoms away from it.
    *
    * <p>
    * If these values are set to zero, cliff avoidance will be turned off. This creates a risk that the robot will
    * hit the cliff with its swing foot. Therefore, these parameters should be set according to what the swing trajectory
    * generator is capable of swinging over.
    * </p>
    */
   default double getCliffHeightToAvoid()
   {
      return 0.15;
   }

   /**
    * The planner can be setup to avoid footsteps near the bottom of "cliffs". When the footstep has a planar region
    * nearby that is {@link #getCliffHeightToAvoid} higher than the candidate footstep, it will move away from it
    * until it is minimumDistanceFromCliffBottoms away from it.
    *
    * <p>
    * If these values are set to zero, cliff avoidance will be turned off. This creates a risk that the robot will
    * hit the cliff with its swing foot. Therefore, these parameters should be set according to what the swing trajectory
    * generator is capable of swinging over.
    * </p>
    */
   default double getMinimumFrontEndForwardDistanceFromCliffBottoms()
   {
      return 0.1;
   }

   default double getMinimumFrontEndBackwardDistanceFromCliffBottoms()
   {
      return 0.1;
   }

   default double getMinimumHindEndForwardDistanceFromCliffBottoms()
   {
      return 0.1;
   }

   default double getMinimumHindEndBackwardDistanceFromCliffBottoms()
   {
      return 0.1;
   }

   default double getMinimumLateralDistanceFromCliffBottoms()
   {
      return 0.1;
   }

   default double getFinalTurnProximity()
   {
      return 1.0;
   }

   default double getFinalSlowDownProximity()
   {
      return 0.5;
   }

   default double getMaximumDeviationFromXGaitDuringExpansion()
   {
      return 0.1;
   }

   default boolean returnBestEffortPlan()
   {
      return true;
   }

   default int getMinimumStepsForBestEffortPlan()
   {
      return 4;
   }

   default QuadrupedFootstepPlannerParametersPacket getAsPacket()
   {
      QuadrupedFootstepPlannerParametersPacket packet = new QuadrupedFootstepPlannerParametersPacket();
      packet.setMaximumFrontStepReach(getMaximumFrontStepReach());
      packet.setMaximumFrontStepLength(getMaximumFrontStepLength());
      packet.setMinimumFrontStepLength(getMinimumFrontStepLength());
      packet.setMaximumHindStepReach(getMaximumHindStepReach());
      packet.setMaximumHindStepLength(getMaximumHindStepLength());
      packet.setMinimumHindStepLength(getMinimumHindStepLength());
      packet.setMaximumFrontStepLengthWhenSteppingUp(getMaximumFrontStepLengthWhenSteppingUp());
      packet.setMinimumFrontStepLengthWhenSteppingUp(getMinimumFrontStepLengthWhenSteppingUp());
      packet.setMaximumHindStepLengthWhenSteppingUp(getMaximumHindStepLengthWhenSteppingUp());
      packet.setMinimumHindStepLengthWhenSteppingUp(getMinimumHindStepLengthWhenSteppingUp());
      packet.setStepZForSteppingUp(getStepZForSteppingUp());
      packet.setMaximumFrontStepLengthWhenSteppingDown(getMaximumFrontStepLengthWhenSteppingDown());
      packet.setMinimumFrontStepLengthWhenSteppingDown(getMinimumFrontStepLengthWhenSteppingDown());
      packet.setMaximumHindStepLengthWhenSteppingDown(getMaximumHindStepLengthWhenSteppingDown());
      packet.setMinimumHindStepLengthWhenSteppingDown(getMinimumHindStepLengthWhenSteppingDown());
      packet.setStepZForSteppingDown(getStepZForSteppingDown());
      packet.setMaximumStepOutward(getMaximumStepOutward());
      packet.setMaximumStepInward(getMaximumStepInward());
      packet.setMaximumStepYawInward(getMaximumStepYawInward());
      packet.setMaximumStepYawOutward(getMaximumStepYawOutward());
      packet.setMaximumStepChangeZ(getMaximumStepChangeZ());
      packet.setBodyGroundClearance(getBodyGroundClearance());
      packet.setMaxWalkingSpeedMultiplier(getMaxWalkingSpeedMultiplier());
      packet.setDistanceWeight(getDistanceWeight());
      packet.setYawWeight(getYawWeight());
      packet.setXGaitWeight(getXGaitWeight());
      packet.setDesiredVelocityWeight(getDesiredVelocityWeight());
      packet.setCostPerStep(getCostPerStep());
      packet.setStepUpWeight(getStepUpWeight());
      packet.setStepDownWeight(getStepDownWeight());
      packet.setHeuristicsWeight(getHeuristicsInflationWeight());
      packet.setMinXClearanceFromFoot(getMinXClearanceFromFoot());
      packet.setMinYClearanceFromFoot(getMinYClearanceFromFoot());
      packet.setProjectionInsideDistance(getProjectInsideDistance());
      packet.setProjectInsideUsingConvexHull(getProjectInsideUsingConvexHull());
      packet.setMaximumXyWiggleDistance(getMaximumXYWiggleDistance());
      packet.setMinimumSurfaceInclineRadians(getMinimumSurfaceInclineRadians());
      packet.setCliffHeightToAvoid(getCliffHeightToAvoid());
      packet.setMinimumFrontEndForwardDistanceFromCliffBottoms(getMinimumFrontEndForwardDistanceFromCliffBottoms());
      packet.setMinimumFrontEndBackwardDistanceFromCliffBottoms(getMinimumFrontEndBackwardDistanceFromCliffBottoms());
      packet.setMinimumHindEndForwardDistanceFromCliffBottoms(getMinimumHindEndForwardDistanceFromCliffBottoms());
      packet.setMinimumHindEndBackwardDistanceFromCliffBottoms(getMinimumHindEndBackwardDistanceFromCliffBottoms());
      packet.setMinimumLateralDistanceFromCliffBottoms(getMinimumLateralDistanceFromCliffBottoms());
      packet.setFinalTurnProximity(getFinalTurnProximity());
      packet.setFinalSlowDownProximity(getFinalSlowDownProximity());
      packet.setMaximumDeviationFromXGaitDuringExpansion(getMaximumDeviationFromXGaitDuringExpansion());
      packet.setReturnBestEffortPlan(returnBestEffortPlan());
      packet.setMinimumStepsForBestEffortPlan(getMinimumStepsForBestEffortPlan());

      return packet;
   }
}
