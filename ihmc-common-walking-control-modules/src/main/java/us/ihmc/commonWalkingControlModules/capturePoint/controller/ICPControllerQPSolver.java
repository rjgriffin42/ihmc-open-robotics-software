package us.ihmc.commonWalkingControlModules.capturePoint.controller;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import us.ihmc.commonWalkingControlModules.capturePoint.optimization.ICPOptimizationController;
import us.ihmc.commonWalkingControlModules.capturePoint.optimization.qpInput.*;
import us.ihmc.convexOptimization.quadraticProgram.AbstractSimpleActiveSetQPSolver;
import us.ihmc.convexOptimization.quadraticProgram.JavaQuadProgSolver;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.interfaces.*;
import us.ihmc.log.LogTools;
import us.ihmc.matrixlib.MatrixTools;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that sets up the actual optimization framework and handles the inputs to generate an optimized solution
 * designed to stabilize ICP based walking trajectories using both CMP feedback and step adjustment. Designed to
 * work inside the {@link ICPOptimizationController}.
 */
public class ICPControllerQPSolver
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private static final boolean useWarmStart = true;
   private static final int maxNumberOfIterations = 100;

   /** Index for the CoP Feedback action term. */
   static final int copFeedbackIndex = 0;
   /** Index for the CMP feedback term. */
   static final int cmpFeedbackIndex = 2;

   private boolean resetActiveSet;
   private boolean previousTickFailed = false;

   /** Input calculator that formulates the different objectives and handles adding them to the full program. */
   private final ICPControllerQPInputCalculator inputCalculator;

   /**
    * Has the form 0.5 x<sup>T</sup> H x + h x
    */
   /** Total quadratic cost matrix for the quadratic program. */
   private final DenseMatrix64F solverInput_H;
   /** Total linear cost vector for the quadratic program. */
   private final DenseMatrix64F solverInput_h;
   /** Total scalar cost for the quadratic program. */
   private final DenseMatrix64F solverInputResidualCost;

   /**
    * Has the form A<sub>ineq</sub> x >= b<sub>ineq</sub>
    */
   /** Total linear inequality constraint matrix for the quadratic program. */
   private final DenseMatrix64F solverInput_Aineq;
   /** Total linear inequality constraint objective vector for the quadratic program. */
   private final DenseMatrix64F solverInput_bineq;

   /** QP Objective to minimize the amount of feedback action. Also contains feedback rate. */
   private final ICPQPInput copFeedbackTaskInput;
   /** QP Objective to minimize the cmp feedback magnitude. */
   private final ICPQPInput cmpFeedbackTaskInput;
   /** QP Objective to minimize the amount of feedback action. Also contains feedback rate. */
   private final ICPQPInput feedbackRateTaskInput;
   /** QP Objective to minimize the difference between the dynamics */
   private final ICPQPInput dynamicsTaskInput;

   private final List<ICPQPInput> inputList = new ArrayList<>();

   /** QP Inequality constraints to limit the total amount of feedback action. */
   private final ICPInequalityInput feedbackRateLimitConstraint;
   /** QP Inequality constraints to limit the total rate of feedback action. */
   private final ICPInequalityInput feedbackLimitConstraint;

   private final List<ICPInequalityInput> constraintList = new ArrayList<>();

   /** Constraint on the CoP location to the support polygon. */
   private final ConstraintToConvexRegion copLocationConstraint;
   /** Constraint on the CMP location to the bounds. */
   private final ConstraintToConvexRegion cmpLocationConstraint;

   private final DenseMatrix64F desiredCoP = new DenseMatrix64F(2, 1);
   private final DenseMatrix64F desiredCMP = new DenseMatrix64F(2, 1);
   private final DenseMatrix64F desiredCMPOffset = new DenseMatrix64F(2, 1);
   /** Current ICP Error location. */
   private final DenseMatrix64F currentICPError = new DenseMatrix64F(2, 1);

   /** Weight minimizing the CoP feedback action. */
   private final DenseMatrix64F copFeedbackWeight = new DenseMatrix64F(2, 2);
   /** Weight minimizing the feedback rate. */
   private final DenseMatrix64F feedbackRateWeight = new DenseMatrix64F(2, 2);
   /** Weight minimizing the CoP and CMP feedback rate. */
   private final DenseMatrix64F copCMPFeedbackRateWeight = new DenseMatrix64F(2, 2);
   /** Weight minimizing the CMP feedback action. */
   private final DenseMatrix64F cmpFeedbackWeight = new DenseMatrix64F(2, 2);
   /** Weight minimizing the dynamic relaxation magnitude. */
   private final DenseMatrix64F dynamicsWeight = new DenseMatrix64F(2, 2);
   /** Proportional gain on the ICP feedback controller. */
   private final DenseMatrix64F feedbackGain = new DenseMatrix64F(2, 2);

   /** Flag to use the quad prog QP solver vs. the active set QP solver. **/
   private final AbstractSimpleActiveSetQPSolver solver = new JavaQuadProgSolver();

   /** Full solution vector to the quadratic program. */
   private final DenseMatrix64F solution;
   /** CoP Feedback action solution to the quadratic program. */
   private final DenseMatrix64F copDeltaSolution;
   /** CMP different from the CoP solution to the quadratic program. */
   private final DenseMatrix64F cmpDeltaSolution;
   private final DenseMatrix64F dynamicsError;

   /** Previous solution for the feedback action, used in the feedback rate objective. */
   private final DenseMatrix64F previousFeedbackDeltaSolution;
   private final DenseMatrix64F previousCMPFeedbackDeltaSolution;
   private final DenseMatrix64F previousCoPFeedbackDeltaSolution;


   /** Number of iterations required for the active set solver to find a solution. */
   private int numberOfIterations;

   private int currentInequalityConstraintIndex;

   /** boolean to determine whether or not to compute the cost to go. specified at compile time. */
   private final boolean autoSetPreviousSolution;

   /** boolean indicating whether or not the feedback rate term has been added and can be used. */
   private final YoBoolean hasFeedbackRateTerm;
   /** Whether or not to use angular momentum during feedback. This means the CMP will be constrained to being in the support polygon. */
   private final YoBoolean useAngularMomentum;


   private double maxFeedbackXMagnitude = Double.POSITIVE_INFINITY;
   private double maxFeedbackYMagnitude = Double.POSITIVE_INFINITY;
   private double maximumFeedbackRate = Double.POSITIVE_INFINITY;
   private double controlDT = Double.POSITIVE_INFINITY;

   private double copSafeDistanceToEdge = 0.0001;
   private double cmpSafeDistanceFromEdge = Double.POSITIVE_INFINITY;

   /**
    * Creates the ICP Optimization Solver. Refer to the class documentation: {@link ICPControllerQPSolver}.
    *
    * @param maximumNumberOfCMPVertices maximum number of vertices to be considered by the CoP location constraint.
    */
   public ICPControllerQPSolver(int maximumNumberOfCMPVertices)
   {
      this(maximumNumberOfCMPVertices, true, null);
   }

   public ICPControllerQPSolver(int maximumNumberOfCMPVertices, boolean autoSetPreviousSolution, YoVariableRegistry registry)
   {
      this.autoSetPreviousSolution = autoSetPreviousSolution;

      hasFeedbackRateTerm = new YoBoolean("icpQPHasFeedbackRateTerm", registry);
      hasFeedbackRateTerm.set(false);
      useAngularMomentum = new YoBoolean("icpQPUseAngularMomentum", registry);
      useAngularMomentum.set(false);

      inputCalculator = new ICPControllerQPInputCalculator();

      int maximumNumberOfFreeVariables = 6;
      int maximumNumberOfLagrangeMultipliers = 8;

      solverInput_H = new DenseMatrix64F(maximumNumberOfFreeVariables, maximumNumberOfFreeVariables);
      solverInput_h = new DenseMatrix64F(maximumNumberOfFreeVariables, 1);
      solverInputResidualCost = new DenseMatrix64F(1, 1);

      copFeedbackTaskInput = new ICPQPInput(2);
      cmpFeedbackTaskInput = new ICPQPInput(2);
      dynamicsTaskInput = new ICPQPInput(6);
      feedbackRateTaskInput = new ICPQPInput(4);

      inputList.add(copFeedbackTaskInput);
      inputList.add(cmpFeedbackTaskInput);
      inputList.add(dynamicsTaskInput);
      inputList.add(feedbackRateTaskInput);

      feedbackLimitConstraint = new ICPInequalityInput(0, 6);
      feedbackRateLimitConstraint = new ICPInequalityInput(0, 6);

      constraintList.add(feedbackLimitConstraint);
      constraintList.add(feedbackRateLimitConstraint);

      copLocationConstraint = new ConstraintToConvexRegion(maximumNumberOfCMPVertices);
      cmpLocationConstraint = new ConstraintToConvexRegion(maximumNumberOfCMPVertices);

      solverInput_Aineq = new DenseMatrix64F(maximumNumberOfCMPVertices,
                                             maximumNumberOfCMPVertices);
      solverInput_bineq = new DenseMatrix64F(maximumNumberOfCMPVertices, 1);

      solution = new DenseMatrix64F(maximumNumberOfFreeVariables + maximumNumberOfLagrangeMultipliers, 1);
      copDeltaSolution = new DenseMatrix64F(2, 1);
      cmpDeltaSolution = new DenseMatrix64F(2, 1);
      dynamicsError = new DenseMatrix64F(2, 1);

      previousFeedbackDeltaSolution = new DenseMatrix64F(2, 1);
      previousCoPFeedbackDeltaSolution = new DenseMatrix64F(2, 1);
      previousCMPFeedbackDeltaSolution = new DenseMatrix64F(2, 1);

//      solver.setConvergenceThreshold(convergenceThreshold);
      solver.setMaxNumberOfIterations(maxNumberOfIterations);
      solver.setUseWarmStart(useWarmStart);
   }

   /**
    * Let the QP solver know that the active set has been changed, so it must be reset.
    */
   public void notifyResetActiveSet()
   {
      this.resetActiveSet = true;
   }

   /**
    * Check whether or not the active set must be reset.
    */
   private boolean pollResetActiveSet()
   {
      boolean ret = resetActiveSet;
      resetActiveSet = false;
      return ret;
   }

   /**
    * Sets the maximum number of iterations to be used by the active set solver.
    */
   public void setMaxNumberOfIterations(int maxNumberOfIterations)
   {
      solver.setMaxNumberOfIterations(maxNumberOfIterations);
   }

   /**
    * Resets the constraint on the CoP location. This constraint requires that the CoP not exit the convex hull of the support polygon.
    */
   public void resetCoPLocationConstraint()
   {
      copLocationConstraint.reset();
      cmpLocationConstraint.reset();
   }

   /**
    * Sets the minimum distance that the CoP must lie within the support polygon (m). This is the distance that the CoP
    * will be to the edge, from the inside. Note that this is not a Euclidean distance, but is the maximum distance in X,
    * and then the maximum distance in Y, rather than their distance sum.
    */
   public void setCopSafeDistanceToEdge(double copSafeDistanceToEdge)
   {
      this.copSafeDistanceToEdge = copSafeDistanceToEdge;
   }

   /**
    * Sets the maximum distance that the CMP can lie from the support polygon (m). This is the maximum distance from the CMP
    * when it is outside the support polygon to the support polygon edge. Note that this is not a Euclidean distance, but is
    * the maximum distance in X, and then the maximum distance in Y, rather than their distance sum.
    */
   public void setMaxCMPDistanceFromEdge(double maxCMPDistance)
   {
      this.cmpSafeDistanceFromEdge = maxCMPDistance;
   }

   /**
    * Adds a convex polygon to the CoP location constraint. The CoP is constrained to the inside of the convex hull described
    * by this polygon.
    *
    * @param polygon polygon to add.
    */
   public void addSupportPolygon(FrameConvexPolygon2DReadOnly polygon)
   {
      if (polygon == null)
         return;

      polygon.checkReferenceFrameMatch(worldFrame);
      copLocationConstraint.addPolygon(polygon);
      cmpLocationConstraint.addPolygon(polygon);
   }

   /**
    * Sets the maximum allowable feedback magnitude in X and Y. This defines an inequality constraint on the sum of the feedback terms in the QP.
    */
   public void setMaximumFeedbackMagnitude(FrameVector2DReadOnly maximumFeedbackMagnitude)
   {
      maximumFeedbackMagnitude.checkReferenceFrameMatch(worldFrame);
      this.maxFeedbackXMagnitude = Math.abs(maximumFeedbackMagnitude.getX());
      this.maxFeedbackYMagnitude = Math.abs(maximumFeedbackMagnitude.getY());
   }

   /**
    * Sets the maximum allowable feedback rate in X and Y. This defines an inequality constraint on the sum of the feedback terms in the QP.
    */
   public void setMaximumFeedbackRate(double maximumFeedbackRate, double controlDT)
   {
      this.maximumFeedbackRate = maximumFeedbackRate;
      this.controlDT = controlDT;
   }

   /**
    * Zeros all the pertinent scalars, vectors, and matrices for the solver. Should be called at the beginning of every computation tick.
    */
   private void reset()
   {
      solverInput_H.zero();
      solverInput_h.zero();
      solverInputResidualCost.zero();

      solverInput_Aineq.zero();
      solverInput_bineq.zero();

      for (int i = 0; i < inputList.size(); i++)
      {
         inputList.get(i).reset();
      }

      for (int i = 0; i < constraintList.size(); i++)
      {
         constraintList.get(i).reset();
      }

      solution.zero();
      copDeltaSolution.zero();
      cmpDeltaSolution.zero();
      dynamicsError.zero();

      currentInequalityConstraintIndex = 0;
   }

   /**
    * Reshapes all the vectors and matrices to the appropriate size, based on the number of footsteps to handle. Should be called after {@link #reset()}
    * at the beginning of every computation tick.
    */
   private void reshape()
   {
      int problemSize = useAngularMomentum.getBooleanValue() ? 4 : 2;
      int numberOfInequalityConstraints;

      copLocationConstraint.setPolygon();
      cmpLocationConstraint.setPolygon();

      numberOfInequalityConstraints = copLocationConstraint.getInequalityConstraintSize();
      if (useAngularMomentum.getBooleanValue() && Double.isFinite(cmpSafeDistanceFromEdge))
      {
         numberOfInequalityConstraints += cmpLocationConstraint.getInequalityConstraintSize();
      }

      solverInput_H.reshape(problemSize, problemSize);
      solverInput_h.reshape(problemSize, 1);

      copFeedbackTaskInput.reshape(2);
      dynamicsTaskInput.reshape(problemSize);
      cmpFeedbackTaskInput.reshape(2);

      numberOfInequalityConstraints += (Double.isFinite(maximumFeedbackRate) && Double.isFinite(controlDT)) ? 4 : 0;
      numberOfInequalityConstraints += Double.isFinite(maxFeedbackXMagnitude) ? 2 : 0;
      numberOfInequalityConstraints += Double.isFinite(maxFeedbackYMagnitude) ? 2 : 0;

      solverInput_Aineq.reshape(numberOfInequalityConstraints, problemSize);
      solverInput_bineq.reshape(numberOfInequalityConstraints, 1);

      solution.reshape(problemSize, 1);
   }

   /**
    * Resets the controller conditions on the feedback minimization task, the feedback gains, and the dynamic relaxation minimization task.
    * Also sets that the controller is not to attempt to regularize the feedback minimization task.
    *
    */
   public void resetCoPFeedbackConditions()
   {
      copFeedbackWeight.zero();
      feedbackGain.zero();
      dynamicsWeight.zero();

      hasFeedbackRateTerm.set(false);
   }

   /**
    * Sets the conditions for the minimization of the cmp feedback task. This includes whether or not to utilize angular momentum to help stabilize
    * the ICP dynamics, as well as the weight on its minimization.
    *
    * @param cmpFeedbackWeight weight on minimizing cmp feedback.
    * @param useAngularMomentum whether or not to use angular momentum in the problem.
    */
   public void setCMPFeedbackConditions(double cmpFeedbackWeight, boolean useAngularMomentum)
   {
      MatrixTools.setDiagonal(this.cmpFeedbackWeight, cmpFeedbackWeight);

      this.useAngularMomentum.set(useAngularMomentum);
   }

   /**
    * Resets the feedback rate objective.
    *
    * @param previousFeedbackDeltaSolution new location of the previous feedback location to try and minimize against.
    */
   public void resetFeedbackRate(FramePoint2D previousFeedbackDeltaSolution)
   {
      previousFeedbackDeltaSolution.changeFrame(worldFrame);
      this.previousFeedbackDeltaSolution.set(0, 0, previousFeedbackDeltaSolution.getX());
      this.previousFeedbackDeltaSolution.set(1, 0, previousFeedbackDeltaSolution.getY());
   }

   /**
    * Sets the conditions for the feedback minimization task and the dynamic relaxation minimization task. This task minimizes the difference between
    * the nominal CMP location and the one used to control the ICP dynamics. The dynamic relaxation allows the ICP recursive dynamics to be violated by a
    * small magnitude, which is critical to not overconstraining the problem.
    *
    * @param copFeedbackWeight weight on the minimization of the CoP feedback action for the solver.
    * @param feedbackGain ICP controller proportional gain.
    * @param dynamicsWeight weight on the minimization of the dynamic relaxation for the solver.
    */
   public void setFeedbackConditions(double copFeedbackWeight, double feedbackGain, double dynamicsWeight)
   {
      this.setFeedbackConditions(copFeedbackWeight, copFeedbackWeight, feedbackGain, feedbackGain, dynamicsWeight);
   }


   private final DenseMatrix64F tempCoPFeedbackWeight = new DenseMatrix64F(2, 2);
   private final DenseMatrix64F tempFeedbackGains = new DenseMatrix64F(2, 2);

   /**
    * Sets the conditions for the feedback minimization task and the dynamic relaxation minimization task. This task minimizes the difference between
    * the nominal CMP location and the one used to control the ICP dynamics. The dynamic relaxation allows the ICP recursive dynamics to be violated by a
    * small magnitude, which is critical to not overconstraining the problem.
    *
    * @param copFeedbackXWeight weight on the minimization of the CoP feedback action for the solver in the Cartesian x coordinate direction.
    * @param copFeedbackYWeight weight on the minimization of the CoP feedback action for the solver in the Cartesian y coordinate direction.
    * @param feedbackXGain ICP controller proportional gain in the Cartesian x coordinate direction.
    * @param feedbackYGain ICP controller proportional gain in the Cartesian y coordinate direction.
    * @param dynamicsWeight weight on the minimization of the dynamic relaxation for the solver.
    */
   public void setFeedbackConditions(double copFeedbackXWeight, double copFeedbackYWeight, double feedbackXGain, double feedbackYGain, double dynamicsWeight)
   {
      tempCoPFeedbackWeight.set(0, 0, copFeedbackXWeight);
      tempCoPFeedbackWeight.set(1, 1, copFeedbackYWeight);

      tempFeedbackGains.set(0, 0, feedbackXGain);
      tempFeedbackGains.set(1, 1, feedbackYGain);

      this.setFeedbackConditions(tempCoPFeedbackWeight, tempFeedbackGains, dynamicsWeight);
   }


   /**
    * Sets the conditions for the feedback minimization task and the dynamic relaxation minimization task. This task minimizes the difference between
    * the nominal CMP location and the one used to control the ICP dynamics. The dynamic relaxation allows the ICP recursive dynamics to be violated by a
    * small magnitude, which is critical to not overconstraining the problem.
    *
    * @param copFeedbackWeights weight on the minimization of the CoP feedback action for the solver in the world frame.
    * @param feedbackGains ICP controller proportional gain in the world frame.
    * @param dynamicsWeight weight on the minimization of the dynamic relaxation for the solver.
    */
   public void setFeedbackConditions(DenseMatrix64F copFeedbackWeights, DenseMatrix64F feedbackGains, double dynamicsWeight)
   {
      this.copFeedbackWeight.set(copFeedbackWeights);
      this.feedbackGain.set(feedbackGains);

      MatrixTools.setDiagonal(this.dynamicsWeight, dynamicsWeight);
   }

   /**
    * Enables the use of cop feedback rate in the solver, and also sets the weight on it. This task minimizes the differences between solutions of the
    * amount of CoP feedback to stabilize the ICP dynamics.
    *
    * @param copCMPFeedbackRateWeight weight placed on changes in the CoP and CMP feedback solution.
    * @param feedbackRateWeight weight placed on changes in the total (CoP + CMP) feedback solution.
    */
   public void setFeedbackRateWeight(double copCMPFeedbackRateWeight, double feedbackRateWeight)
   {
      MatrixTools.setDiagonal(this.feedbackRateWeight, feedbackRateWeight);
      MatrixTools.setDiagonal(this.copCMPFeedbackRateWeight, copCMPFeedbackRateWeight);

      hasFeedbackRateTerm.set(true);
   }

   private final FrameVector2D cmpOffsetToThrowAway = new FrameVector2D();

   /**
    * Solves a linearly constrained quadratic program that computes the desired CMP feedback action combined with the desired step adjustment to stabilize the
    * ICP dynamics. This problem attempts to minimize the magnitude of CMP feedback while minimizing the amount of step adjustment. This is achieved by noting
    * that the current desired ICP location is a linear transformation of the upcoming step locations and the final desired ICP location.
    *
    * All the tasks must be set every tick before calling this method.
    *
    * @param desiredCoP current desired value of the CMP based on the nominal ICP location.
    * @return whether a new solution was found if this is false the last valid solution will be used.
    */
   public boolean compute(FrameVector2DReadOnly currentICPError, FramePoint2DReadOnly desiredCoP)
   {
      cmpOffsetToThrowAway.setToZero(worldFrame);
      return compute(currentICPError, desiredCoP, cmpOffsetToThrowAway);
   }

   /**
    * Solves a linearly constrained quadratic program that computes the desired CMP feedback action combined with the desired step adjustment to stabilize the
    * ICP dynamics. This problem attempts to minimize the magnitude of CMP feedback while minimizing the amount of step adjustment. This is achieved by noting
    * that the current desired ICP location is a linear transformation of the upcoming step locations and the final desired ICP location.
    *
    * All the tasks must be set every tick before calling this method.
    *
    * @param desiredCoP current desired value of the CMP based on the nominal ICP location.
    * @param desiredCMPOffset current desired distance from the CoP to the CMP.
    * @return whether a new solution was found if this is false the last valid solution will be used.
    */
   public boolean compute(FrameVector2DReadOnly currentICPError, FramePoint2DReadOnly desiredCoP, FrameVector2DReadOnly desiredCMPOffset)
   {
      reset();
      reshape();

      currentICPError.checkReferenceFrameMatch(worldFrame);
      desiredCoP.checkReferenceFrameMatch(worldFrame);
      desiredCMPOffset.checkReferenceFrameMatch(worldFrame);

      currentICPError.get(this.currentICPError);
      desiredCoP.get(this.desiredCoP);
      desiredCMPOffset.get(this.desiredCMPOffset);
      CommonOps.add(this.desiredCoP, this.desiredCMPOffset, desiredCMP);

      addCoPFeedbackTask();

      if (useAngularMomentum.getBooleanValue())
         addCMPFeedbackTask();

      if (hasFeedbackRateTerm.getBooleanValue())
         addFeedbackRateTask();

      addDynamicConstraintTask();

      if (copLocationConstraint.getInequalityConstraintSize() > 0)
         addCoPLocationConstraint();

      if (useAngularMomentum.getBooleanValue() && cmpLocationConstraint.getInequalityConstraintSize() > 0)
         addCMPLocationConstraint();

      if (!previousTickFailed)
      { // this can occasionally over-constrain the problem, so remove it if the previous tick failed.
         addMaximumFeedbackMagnitudeConstraint();
         addMaximumFeedbackRateConstraint();
      }

      boolean foundSolution = solve(solution);
      previousTickFailed = !foundSolution;

      if (foundSolution)
      {
         extractCoPFeedbackDeltaSolution(copDeltaSolution);
         extractCMPFeedbackDeltaSolution(cmpDeltaSolution);

         inputCalculator.computeDynamicConstraintError(solution, dynamicsError);

         if (autoSetPreviousSolution)
            setPreviousFeedbackDeltaSolution(copDeltaSolution, cmpDeltaSolution);
      }

      return foundSolution;
   }

   public boolean previousTickFailed()
   {
      return previousTickFailed;
   }


   /**
    * Adds the minimization of cop feedback task to the quadratic program's cost objectives.<br>
    * Also adds the rate of the cop feedback term, if enabled.
    */
   private void addCoPFeedbackTask()
   {
      ICPQPInputCalculator.computeCoPFeedbackTask(copFeedbackTaskInput, copFeedbackWeight);
      inputCalculator.submitCoPFeedbackTask(copFeedbackTaskInput, solverInput_H, solverInput_h, solverInputResidualCost);
   }

   /**
    * Adds the minimization of cmp feedback to the quadratic program's cost objectives.
    */
   private void addCMPFeedbackTask()
   {
      inputCalculator.computeCMPFeedbackTask(cmpFeedbackTaskInput, cmpFeedbackWeight);
      inputCalculator.submitCMPFeedbackTask(cmpFeedbackTaskInput, solverInput_H, solverInput_h, solverInputResidualCost);
   }

   /**
    * Adds the minimization of feedback rate task to the quadratic program's cost objectives.<br>
    */
   private void addFeedbackRateTask()
   {
      inputCalculator.computeCoPFeedbackRateTask(feedbackRateTaskInput, copCMPFeedbackRateWeight, previousCoPFeedbackDeltaSolution);
      if (useAngularMomentum.getBooleanValue())
         inputCalculator.computeCMPFeedbackRateTask(feedbackRateTaskInput, copCMPFeedbackRateWeight, previousCMPFeedbackDeltaSolution);

      inputCalculator.computeFeedbackRateTask(feedbackRateTaskInput, feedbackRateWeight, previousFeedbackDeltaSolution, useAngularMomentum.getBooleanValue());
      inputCalculator.submitFeedbackRateTask(feedbackRateTaskInput, solverInput_H, solverInput_h, solverInputResidualCost);
   }

   /**
    * Adds the convex CoP location constraint that requires the CoP to be in the support polygon.
    *
    * <p>
    * Takes the form Ax <= b.
    * </p>
    */
   private void addCoPLocationConstraint()
   {
      copLocationConstraint.setPositionOffset(desiredCoP);
      copLocationConstraint.setDeltaInside(copSafeDistanceToEdge);
      copLocationConstraint.formulateConstraint();

      int constraintSize = copLocationConstraint.getInequalityConstraintSize();
      MatrixTools.setMatrixBlock(solverInput_Aineq, currentInequalityConstraintIndex, copFeedbackIndex, copLocationConstraint.Aineq, 0, 0,
                                 constraintSize, 2, 1.0);
      MatrixTools.setMatrixBlock(solverInput_bineq, currentInequalityConstraintIndex, 0, copLocationConstraint.bineq, 0, 0, constraintSize, 1, 1.0);

      currentInequalityConstraintIndex += constraintSize;
   }

   /**
    * Adds the convex CMP location constraint that requires the CMP to be within some distance of in the support polygon.
    *
    * <p>
    * Takes the form Ax <= b.
    * </p>
    */
   private void addCMPLocationConstraint()
   {
      double cmpConstraintBound;
      if (useAngularMomentum.getBooleanValue())
         cmpConstraintBound = -cmpSafeDistanceFromEdge;
      else
         cmpConstraintBound = copSafeDistanceToEdge;

      if (!Double.isFinite(cmpConstraintBound))
         return;

      cmpLocationConstraint.setPositionOffset(desiredCMP);
      cmpLocationConstraint.setDeltaInside(cmpConstraintBound);
      cmpLocationConstraint.formulateConstraint();

      int constraintSize = cmpLocationConstraint.getInequalityConstraintSize();
      MatrixTools.setMatrixBlock(solverInput_Aineq, currentInequalityConstraintIndex, copFeedbackIndex, cmpLocationConstraint.Aineq, 0, 0,
                                 constraintSize, 2, 1.0);
      MatrixTools.setMatrixBlock(solverInput_Aineq, currentInequalityConstraintIndex, cmpFeedbackIndex, cmpLocationConstraint.Aineq, 0, 0,
                                 constraintSize, 2, 1.0);
      MatrixTools.setMatrixBlock(solverInput_bineq, currentInequalityConstraintIndex, 0, cmpLocationConstraint.bineq, 0, 0, constraintSize, 1, 1.0);

      currentInequalityConstraintIndex += constraintSize;
   }



   /**
    * Adds the recursive dynamics as an equality constraint to the optimization. Takes the form
    *
    * <p>
    *    &delta; = k<sub>p</sub> ( x<sub>icp</sub>; - x<sub>icp,r</sub> ),
    * </p>
    * where
    * <p>
    *    x<sub>icp,r</sub> = &Phi;<sub>f</sub> + &Phi;<sub>const</sub> + &gamma;<sub>f,i</sub> r<sub>f,i</sub>,
    *
    * </p>
    * <p>
    * where
    *    <li>&Phi;<sub>f</sub> is the final ICP Recursion</li>
    *    <li>&Phi;<sub>const</sub> encodes the effects of the recursive CMP offsets in the upcoming footsteps and the stance CMP locations</li>
    *    <li>&gamma;<sub>f,i</sub> is the recursion multiplier of the i<sup>th</sup> footstep</li>
    *    <li>r<sub>f,i</sub> is the location of the i<sup>th</sup> footstep</li>
    * </p>
    */
   private void addDynamicConstraintTask()
   {
      inputCalculator
            .computeDynamicsTask(dynamicsTaskInput, currentICPError, feedbackGain, dynamicsWeight, useAngularMomentum.getBooleanValue());
      inputCalculator.submitDynamicsTask(dynamicsTaskInput, solverInput_H, solverInput_h, solverInputResidualCost);
   }

   /**
    * Submits the feedback magnitude inequality constraints to the solver.
    */
   private void addMaximumFeedbackMagnitudeConstraint()
   {
      ICPControllerQPConstraintCalculator.calculateMaxFeedbackMagnitudeConstraint(feedbackLimitConstraint, maxFeedbackXMagnitude, maxFeedbackYMagnitude, useAngularMomentum.getBooleanValue());

      int feedbackMagnitudeConstraintSize = feedbackLimitConstraint.getNumberOfConstraints();
      int numberOfVariables = feedbackLimitConstraint.getNumberOfVariables();

      MatrixTools.setMatrixBlock(solverInput_Aineq, currentInequalityConstraintIndex, 0, feedbackLimitConstraint.Aineq, 0, 0, feedbackMagnitudeConstraintSize,
                                 numberOfVariables, 1.0);
      MatrixTools
            .setMatrixBlock(solverInput_bineq, currentInequalityConstraintIndex, 0, feedbackLimitConstraint.bineq, 0, 0, feedbackMagnitudeConstraintSize, 1,
                            1.0);

      currentInequalityConstraintIndex += feedbackMagnitudeConstraintSize;
   }

   /**
    * Submits the feedback rate inequality constraints to the solver.
    */
   private void addMaximumFeedbackRateConstraint()
   {
      ICPControllerQPConstraintCalculator.calculateMaxFeedbackRateConstraint(feedbackRateLimitConstraint, maximumFeedbackRate, previousFeedbackDeltaSolution, controlDT, useAngularMomentum.getBooleanValue());

      int feedbackRateConstraintSize = feedbackRateLimitConstraint.getNumberOfConstraints();
      int numberOfVariables = feedbackLimitConstraint.getNumberOfVariables();

      MatrixTools.setMatrixBlock(solverInput_Aineq, currentInequalityConstraintIndex, 0, feedbackRateLimitConstraint.Aineq, 0, 0, feedbackRateConstraintSize,
                                 numberOfVariables, 1.0);
      MatrixTools.setMatrixBlock(solverInput_bineq, currentInequalityConstraintIndex, 0, feedbackRateLimitConstraint.bineq, 0, 0, feedbackRateConstraintSize, 1,
                                 1.0);

      currentInequalityConstraintIndex += feedbackRateConstraintSize;
   }

   /**
    * Internal call to solves the quadratic program. Adds all the objectives and constraints to the problem and then solves it.
    *
    * @param solutionToPack solution of the QP.
    * @return whether a solution was found.
    */
   private boolean solve(DenseMatrix64F solutionToPack)
   {
      CommonOps.scale(-1.0, solverInput_h);

      for (int i = 0; i < solverInput_H.numCols; i++)
      {
         if (solverInput_H.get(i, i) < 0.0)
            throw new RuntimeException("Hey this is bad.");
      }

      solver.clear();

      if (useWarmStart && pollResetActiveSet() || previousTickFailed)
         solver.resetActiveConstraints();

      solver.setQuadraticCostFunction(solverInput_H, solverInput_h, solverInputResidualCost.get(0, 0));
      solver.setLinearInequalityConstraints(solverInput_Aineq, solverInput_bineq);

      try
      {
         numberOfIterations = solver.solve(solutionToPack);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         // GW: observed this sometimes when the robot was close to falling (see related issue FB-155).
         // This seems to happen only after the solver failed to find a solution for a few ticks anyway and the
         // robot will probably fall. That is just a suspicion though.
         LogTools.warn("ICP optimization crashed with exception.");
         return false;
      }

      return !MatrixTools.containsNaN(solutionToPack);
   }

   /**
    * Extracts the amount of CMP feedback from the solution vector
    *
    * @param copFeedbackSolutionToPack 2d feedback solution. Modified.
    */
   private void extractCoPFeedbackDeltaSolution(DenseMatrix64F copFeedbackSolutionToPack)
   {
      MatrixTools.setMatrixBlock(copFeedbackSolutionToPack, 0, 0, solution, copFeedbackIndex, 0, 2, 1, 1.0);
   }

   /**
    * Extracts the difference between the CMP and CoP from the solution vector.
    *
    * @param cmpDeltaSolutionToPack difference between the CMP and CoP. Modified.
    */
   private void extractCMPFeedbackDeltaSolution(DenseMatrix64F cmpDeltaSolutionToPack)
   {
      if (useAngularMomentum.getBooleanValue())
         MatrixTools.setMatrixBlock(cmpDeltaSolutionToPack, 0, 0, solution, cmpFeedbackIndex, 0, 2, 1, 1.0);
      else
         cmpDeltaSolutionToPack.zero();
   }

   /**
    * Sets the location of the previous CoP feedback for the feedback rate task.
    *
    * @param copFeedbackSolution amount of CoP feedback.
    */
   private void setPreviousFeedbackDeltaSolution(DenseMatrix64F copFeedbackSolution, DenseMatrix64F cmpFeedbackSolution)
   {
      previousCoPFeedbackDeltaSolution.set(copFeedbackSolution);
      previousCMPFeedbackDeltaSolution.set(cmpFeedbackSolution);
      CommonOps.add(cmpFeedbackSolution, copFeedbackSolution, previousFeedbackDeltaSolution);
   }


   /**
    * Gets the CMP Feedback difference solution for the ICP Proportional feedback problem.
    *
    * @param cmpFeedbackDifferenceToPack difference between the nominal CMP and the desired CMP.
    */
   public void getCoPFeedbackDifference(FixedFrameVector2DBasics cmpFeedbackDifferenceToPack)
   {
      cmpFeedbackDifferenceToPack.checkReferenceFrameMatch(worldFrame);
      cmpFeedbackDifferenceToPack.setX(copDeltaSolution.get(0, 0));
      cmpFeedbackDifferenceToPack.setY(copDeltaSolution.get(1, 0));
   }

   /**
    * Gets the difference between the CMP and the desired CoP. This is equivalent to a scaled version of
    * the cmp feedback of the system.
    *
    * @param differenceToPack difference between the two points. Modified.
    */
   public void getCMPFeedbackDifference(FixedFrameVector2DBasics differenceToPack)
   {
      differenceToPack.checkReferenceFrameMatch(worldFrame);
      differenceToPack.setX(cmpDeltaSolution.get(0, 0));
      differenceToPack.setY(cmpDeltaSolution.get(1, 0));
   }



   public void getDynamicsError(FixedFrameVector2DBasics errorToPack)
   {
      errorToPack.checkReferenceFrameMatch(worldFrame);
      errorToPack.setX(dynamicsError.get(0));
      errorToPack.setY(dynamicsError.get(1));
   }

   /**
    * Gets the number of iterations required to solve by the active set solver. Will return 1 if using the Quad Prog solver.
    * @return number of iterations
    */
   public int getNumberOfIterations()
   {
      return numberOfIterations;
   }

   /**
    * Returns the location constraint on the center of pressure, formulated as an inequality constraint. Useful for testing purposes.
    */
   ConstraintToConvexRegion getCoPLocationConstraint()
   {
      return copLocationConstraint;
   }

   /**
    * Returns the location constraint on the CMP, formulated as an inequality constraint. Useful for testing purposes.
    */
   ConstraintToConvexRegion getCMPLocationConstraint()
   {
      return cmpLocationConstraint;
   }
}
