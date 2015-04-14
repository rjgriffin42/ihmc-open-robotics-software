package us.ihmc.commonWalkingControlModules.configurations;

/**
 * Parameters to tune the ICP (Instantaneous Capture Point) planner for each robot.
 * The ICP planner is a module that predicts the desired ICP trajectory accordingly to the upcoming footsteps.
 * This is one of the critical modules of the walking controller as the desired ICP is used to control the balance of the robot.
 * 
 * <p>
 * There is two different implementations of the ICP planner, which are referred as the old and new planners.
 * Both can predict the ICP trajectory by considering a constant CMP (Centroidal Momentum Pivot) location when in single support ({@link #useTwoCMPsPerSupport()} == false).
 * In this mode, the constant CMP location used in single support is referred as the entryCMP.
 * <p/>
 * 
 * <p>
 * When using the new ICP planner ({@link #useNewICPPlanner()} == true), there is the possibility to use a different mode for planning the ICP trajectory.
 * The mode considers for each single support, an initial CMP (referred here as entryCMP) location that is used at the beginning of single support, and a final CMP location that is used at the end of single support (referred here as exitCMP).
 * This mode is referred as using two CMPs per support ({@link #useTwoCMPsPerSupport()} == true) and is preferable as it helps the robot moving more towards the walking heading direction.
 * It also helps triggering the toe off earlier, and helps stepping down obstacles.
 * However, it requires to tune some more parameters.
 * </p>
 */
public interface CapturePointPlannerParameters
{
   /** Refers to the duration of the first transfer when starting to walk. */
   public abstract double getDoubleSupportInitialTransferDuration();

   @Deprecated
   /** TODO Not the right place to get this value from and it is not really used. */
   public abstract double getDoubleSupportDuration();

   @Deprecated
   /** FIXME That's a hack which makes the planner slower than the swing foot. Need to get rid of it. */
   public abstract double getAdditionalTimeForSingleSupport();

   @Deprecated
   /** TODO Not the right place to get this value from and it is not really used. */
   public abstract double getSingleSupportDuration();

   /**
    * How many footsteps the ICP planner will use to build the plan.
    * The more the better, but will increase the number of YoVariables and increase the computation time.
    * The values 3 and 4 seem to be good.
    */
   public abstract int getNumberOfFootstepsToConsider();

   @Deprecated
   /** TODO Dangerous parameter, should be set to 4. */
   public abstract int getNumberOfCoefficientsForDoubleSupportPolynomialTrajectory();

   @Deprecated
   /** TODO This is not a parameter, where it is used it is supposed to be equal to 2. */
   public abstract int getNumberOfFootstepsToStop();

   @Deprecated
   /** TODO That is not a user parameter, should be less than the control DT. */
   public abstract double getIsDoneTimeThreshold();

   /**
    * Represents in percent of the current double support duration, how much time the transfer will spend before reaching the next entry CMP.
    * The returned value should be between 0.0 and 1.0:
    * <li> 0.0 is equivalent to spend the entire double support on the initial CMP (last entry CMP if using one CMP per support, last exit CMP otherwise), </li>
    * <li> 1.0 is equivalent to spend the entire double support on the next entry CMP. </li>
    * <p> A value close to 0.5 is preferable. </p>
    */
   public abstract double getDoubleSupportSplitFraction();

   /**
    * Slow down factor on the time when doing partial time freeze
    */
   public abstract double getFreezeTimeFactor();

   /**
    * Threshold on the ICP error used to trigger the complete time freeze.
    */
   public abstract double getMaxInstantaneousCapturePointErrorForStartingSwing();

   /**
    * Threshold on the ICP error used to trigger the partial time freeze which consists in slowing down time.
    */
   public abstract double getMaxAllowedErrorWithoutPartialTimeFreeze();

   /**
    * Enable / disable time freezing.
    * When enabled, the time freezer will first slow down time when the actual ICP is behind the plan, and totally freeze time if the actual ICP is far behing the plan.
    * In summary, this makes the plan wait for the actual ICP when there is a lag.
    */
   public abstract boolean getDoTimeFreezing();

   /**
    * Used only with the old planner (inherent in the new planner).
    * Enable/disable foot slip compensation.
    */
   public abstract boolean getDoFootSlipCompensation();

   /**
    * Used only with the old planner (inherent in the new planner).
    * Refers on how fast a foot slipping error should be corrected.
    */
   public abstract double getAlphaDeltaFootPositionForFootslipCompensation();

   /**
    * This parameter forces the entry CMPs to be more inside or outside in the foot.
    */
   public abstract double getEntryCMPInsideOffset();

   /**
    * Only used when using the new ICP planner with two CMPs per support.
    * This parameter forces the exit CMPs to be more inside or outside in the foot.
    */
   public abstract double getExitCMPInsideOffset();

   /**
    * This parameter forces the entry CMPs to be more forward or backward in the foot.
    */
   public abstract double getEntryCMPForwardOffset();

   /**
    * Only used when using the new ICP planner with two CMPs per support.
    * This parameter forces the exit CMPs to be more forward or backward in the foot.
    */
   public abstract double getExitCMPForwardOffset();

   /**
    * Switch between the old and new ICP planner.
    * The new ICP planner has the advantage of exploiting way more the current feet locations.
    * By doing so, it is inherently robust to foot slipping, stepping error, and potential drift in the state estimator when using an external corrector as LIDAR localization.
    * It also allows the use of two reference CMPs per support.
    */
   public abstract boolean useNewICPPlanner();

   /**
    * Only used when using the new ICP planner.
    * Refers to whether use one or two CMPs reference locations per support.
    * Using two CMPs is preferable as it helps the robot moving more towards the walking heading direction.
    * It also helps triggering the toe off earlier, and helps stepping down obstacles.
    * However, it requires to tune some more parameters.
    */
   public abstract boolean useTwoCMPsPerSupport();

   /**
    * Only used when using the new ICP planner with two CMPs per support.
    * The returned value should be between 0.0 and 1.0:
    * <li> 0.0 is equivalent to never use the exit CMP, </li>
    * <li> 1.0 is equivalent to never use the entry CMP. </li>
    * <p> A value close to 0.5 is preferable. </p>
    */
   public abstract double getTimeSpentOnExitCMPInPercentOfStepTime();

   /**
    * This parameter indicates how far front in the foot the entry CMP can be.
    * A large positive value here can be helpful for back steps.
    */
   public abstract double getMaxEntryCMPForwardOffset();

   /**
    * This parameter indicates how far back in the foot the exit CMP can be.
    * Should probably be 0.0, a negative value would delay the toe off when walking forward.
    */
   public abstract double getMinEntryCMPForwardOffset();

   /**
    * This parameter indicates how far front in the foot the exit CMP can be.
    * A large positive value here can improve significantly long forward steps and help trigger the toe off earlier.
    */
   public abstract double getMaxExitCMPForwardOffset();

   /**
    * This parameter indicates how far back in the foot the exit CMP can be.
    * For instance, -0.02m will let the robot move slightly backward in single support when doing back steps.
    */
   public abstract double getMinExitCMPForwardOffset();

   /**
    * This parameter is used when computing the entry CMP and exit CMP to make sure they are contained inside a safe support polygon.
    */
   public abstract double getCMPSafeDistanceAwayFromSupportEdges();

   /**
    *  Pretty refers to how fast the CMP should move from the entry CMP to the exit CMP in single support. 0.5sec seems reasonable.
    *  This parameter allows to reduce unexpected behaviors due to smoothing the exponentially unstable motion of the ICP with a minimum acceleration trajectory (cubic).
    */
   public abstract double getMaxDurationForSmoothingEntryToExitCMPSwitch();

   public abstract boolean useTerribleHackToReduceICPVelocityAtTheEndOfTransfer();
}
