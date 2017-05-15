package us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.qpInput;

import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.icpOptimization.ICPQPOptimizationSolver;

/**
 * Class intended to manage the indices of all components used in the {@link ICPQPOptimizationSolver}.
 */
public class ICPQPIndexHandler
{
   /** Number of footsteps registered with the solver. */
   private int numberOfFootstepsToConsider = 0;
   /** The total number of free variables for consideration in the optimization. */
   private int numberOfFreeVariables = 0;
   /** Number of footstep variables registered with the solver. Should be two times {@link #numberOfFootstepsToConsider} */
   private int numberOfFootstepVariables = 0;

   /** Number of equality constraints in the optimization. Only the dynamics in this formulation. */
   private final static int numberOfEqualityConstraints = 2; // these are the dynamics

   /** Index for the start of the footstep variables. */
   private final int footstepStartingIndex = 0;
   /** Index for the CMP Feedback action term. */
   private int feedbackCMPIndex;
   /** Index for the dynamic relaxation term. */
   private int dynamicRelaxationIndex;
   /** Index for the angular momentum term. */
   private int angularMomentumIndex;

   /** Whether or not to use step adjustment in the optimization. If {@link #numberOfFootstepsToConsider} is 0, this term should be false */
   private boolean useStepAdjustment;
   /** Whether or not to use angular momentum in the optimization. */
   private boolean useAngularMomentum = false;

   /**
    * Resets the number of footsteps for the controller to consider.
    */
   public void resetFootsteps()
   {
      useStepAdjustment = false;
      numberOfFootstepsToConsider = 0;
   }

   /**
    * Registers that a footstep was added to the solver.
    */
   public void registerFootstep()
   {
      useStepAdjustment = true;
      numberOfFootstepsToConsider++;
   }

   /**
    * Gets the total number of footsteps to consider in the current optimization tick.
    *
    * @return number of footsteps
    */
   public int getNumberOfFootstepsToConsider()
   {
      return numberOfFootstepsToConsider;
   }

   /**
    * Whether or not the solver should include the recursive dynamics of the upcoming footsteps.
    *
    * @return whether or not to use step adjustment.
    */
   public boolean useStepAdjustment()
   {
      return useStepAdjustment;
   }

   /**
    * Sets whether or not to use angular momentum in the optimization.
    * @param useAngularMomentum whether or not to use angular momentum
    */
   public void setUseAngularMomentum(boolean useAngularMomentum)
   {
      this.useAngularMomentum = useAngularMomentum;
   }

   /**
    * Whether or not the solver should include the use of angular momentum.
    *
    * @return whether or not to angular momentum.
    */
   public boolean useAngularMomentum()
   {
      return useAngularMomentum;
   }

   /**
    * Computes the total problem size for the optimization. Should be called after all steps have been registered using {@link #registerFootstep()}.
    */
   public void computeProblemSize()
   {
      numberOfFootstepVariables = 2 * numberOfFootstepsToConsider;
      numberOfFreeVariables = numberOfFootstepVariables + 4; // all the footstep locations, the CMP delta, and the dynamic relaxation
      if (useAngularMomentum)
         numberOfFreeVariables += 2;

      feedbackCMPIndex = footstepStartingIndex + numberOfFootstepVariables; // this variable is stored after the footsteps

      dynamicRelaxationIndex = feedbackCMPIndex + 2; // this variable is stored after the feedback value
      angularMomentumIndex = dynamicRelaxationIndex + 2; // this variable is stored after the dynamic relaxation index
   }

   /**
    * Gets the total number of equality constraints for the optimization to use.
    *
    * @return number of equality constraints.
    */
   public int getNumberOfEqualityConstraints()
   {
      return numberOfEqualityConstraints;
   }

   /**
    * Gets the index for the first footstep location.
    *
    * @return footstep start index.
    */
   public int getFootstepStartIndex()
   {
      return 0;
   }

   /**
    * Gets the index for the footstep location of the footstep indicated by {@param footstepIndex}.
    *
    * @param footstepIndex footstep in question.
    * @return footstep index number
    */
   public int getFootstepIndex(int footstepIndex)
   {
      return footstepStartingIndex + 2 * footstepIndex;
   }

   /**
    * Gets the index of the CMP feedback action term.
    *
    * @return cmp feedback action index.
    */
   public int getFeedbackCMPIndex()
   {
      return feedbackCMPIndex;
   }

   /**
    * Gets the index of the dynamic relaxation term.
    *
    * @return dynamic relaxation index.
    */
   public int getDynamicRelaxationIndex()
   {
      return dynamicRelaxationIndex;
   }

   /**
    * Gets the index of the angular momentum term.
    *
    * @return angular momentum index.
    */
   public int getAngularMomentumIndex()
   {
      return angularMomentumIndex;
   }

   /**
    * Gets the total number of variables to be used to describe the footstep locations.
    *
    * @return number of footstep variables.
    */
   public int getNumberOfFootstepVariables()
   {
      return numberOfFootstepVariables;
   }

   /**
    * Gets the total number of free variables to be considered by the optimization.
    *
    * @return number of free variables.
    */
   public int getNumberOfFreeVariables()
   {
      return numberOfFreeVariables;
   }
}
