package us.ihmc.commonWalkingControlModules.modelPredictiveController.core;

import gnu.trove.list.TIntList;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import gnu.trove.list.array.TIntArrayList;
import us.ihmc.convexOptimization.IntermediateSolutionListener;
import us.ihmc.convexOptimization.quadraticProgram.ActiveSetQPSolver;
import us.ihmc.convexOptimization.quadraticProgram.InverseMatrixCalculator;
import us.ihmc.log.LogTools;
import us.ihmc.matrixlib.NativeMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Solves a Quadratic Program using a simple active set method. Does not work for problems where
 * having multiple inequality constraints in the active set make the problem infeasible. Seems to
 * work well for problems with benign inequality constraints, such as variable bounds. Algorithm is
 * very fast when it can find a solution. Uses the algorithm and naming convention found in MIT
 * Paper "An efficiently solvable quadratic program for stabilizing dynamic locomotion" by Scott
 * Kuindersma, Frank Permenter, and Russ Tedrake.
 *
 * @author JerryPratt
 */
public class MPCQPSolver
{
   private static final double violationFractionToAdd = 0.8;
   private static final double violationFractionToRemove = 0.95;
   //private static final double violationFractionToAdd = 1.0;
   //private static final double violationFractionToRemove = 1.0;
   private double convergenceThreshold = 1e-10;
   //private double convergenceThresholdForLagrangeMultipliers = 0.0;
   private double convergenceThresholdForLagrangeMultipliers = 1e-10;
   private int maxNumberOfIterations = 10;
   private boolean reportFailedConvergenceAsNaN = true;
   private boolean resetActiveSetOnSizeChange = true;

   protected double quadraticCostScalar;

   private final DMatrixRMaj activeVariables = new DMatrixRMaj(0, 0);

   private final TIntArrayList activeInequalityIndices = new TIntArrayList();

   // Some temporary matrices:
   protected final NativeMatrix nativexSolutionMatrix = new NativeMatrix(0, 0);
   protected final NativeMatrix costQuadraticMatrix = new NativeMatrix(0, 0);
   protected final NativeMatrix symmetricCostQuadraticMatrix = new NativeMatrix(0, 0);

   private final NativeMatrix linearInequalityConstraintsCheck = new NativeMatrix(0, 0);

   protected final NativeMatrix quadraticCostQVector = new NativeMatrix(0, 0);
   protected final NativeMatrix quadraticCostQMatrix = new NativeMatrix(0, 0);
   protected final NativeMatrix linearEqualityConstraintsAMatrix = new NativeMatrix(0, 0);
   protected final NativeMatrix linearEqualityConstraintsBVector = new NativeMatrix(0, 0);

   protected final NativeMatrix linearInequalityConstraintsCMatrixO = new NativeMatrix(0, 0);
   protected final NativeMatrix linearInequalityConstraintsDVectorO = new NativeMatrix(0, 0);

   /** Active inequality constraints */
   private final NativeMatrix CBar = new NativeMatrix(0, 0);
   private final NativeMatrix DBar = new NativeMatrix(0, 0);

   private final NativeMatrix QInverse = new NativeMatrix(0, 0);
   private final NativeMatrix AQInverse = new NativeMatrix(0, 0);
   private final NativeMatrix QInverseATranspose = new NativeMatrix(0, 0);
   private final NativeMatrix CBarQInverse = new NativeMatrix(0, 0);
   private final NativeMatrix AQInverseATranspose = new NativeMatrix(0, 0);
   private final NativeMatrix AQInverseCBarTranspose = new NativeMatrix(0, 0);
   private final NativeMatrix CBarQInverseATranspose = new NativeMatrix(0, 0);
   private final NativeMatrix QInverseCBarTranspose = new NativeMatrix(0, 0);
   private final NativeMatrix CBarQInverseCBarTranspose = new NativeMatrix(0, 0);

   private final NativeMatrix AAndC = new NativeMatrix(0, 0);
   private final NativeMatrix ATransposeMuAndCTransposeLambda = new NativeMatrix(0, 0);

   private final NativeMatrix bigMatrixForLagrangeMultiplierSolution = new NativeMatrix(0, 0);
   private final NativeMatrix bigVectorForLagrangeMultiplierSolution = new NativeMatrix(0, 0);

   private final NativeMatrix tempVector = new NativeMatrix(0, 0);
   private final NativeMatrix augmentedLagrangeMultipliers = new NativeMatrix(0, 0);

   private final TIntArrayList inequalityIndicesToAddToActiveSet = new TIntArrayList();
   private final TIntArrayList inequalityIndicesToRemoveFromActiveSet = new TIntArrayList();

   protected final NativeMatrix computedObjectiveFunctionValue = new NativeMatrix(1, 1);

   private InverseMatrixCalculator<NativeMatrix> inverseSolver = new DefaultInverseMatrixCalculator();

   private boolean useWarmStart = false;

   private int previousNumberOfVariables = 0;
   private int previousNumberOfEqualityConstraints = 0;
   private int previousNumberOfInequalityConstraints = 0;

   private final List<IntermediateSolutionListener> solutionListeners = new ArrayList<>();

   public void setConvergenceThreshold(double convergenceThreshold)
   {
      this.convergenceThreshold = convergenceThreshold;
   }

   public void setConvergenceThresholdForLagrangeMultipliers(double convergenceThresholdForLagrangeMultipliers)
   {
      this.convergenceThresholdForLagrangeMultipliers = convergenceThresholdForLagrangeMultipliers;
   }

   public void setMaxNumberOfIterations(int maxNumberOfIterations)
   {
      this.maxNumberOfIterations = maxNumberOfIterations;
   }

   public void addIntermediateSolutionListener(IntermediateSolutionListener solutionListener)
   {
      this.solutionListeners.add(solutionListener);
   }

   public void setReportFailedConvergenceAsNaN(boolean reportFailedConvergenceAsNaN)
   {
      this.reportFailedConvergenceAsNaN = reportFailedConvergenceAsNaN;
   }

   public void setResetActiveSetOnSizeChange(boolean resetActiveSetOnSizeChange)
   {
      this.resetActiveSetOnSizeChange = resetActiveSetOnSizeChange;
   }

   public void setActiveInequalityIndices(TIntList activeInequalityIndices)
   {
      this.activeInequalityIndices.reset();
      for (int i = 0; i < activeInequalityIndices.size(); i++)
         this.activeInequalityIndices.add(activeInequalityIndices.get(i));
   }

   public TIntList getActiveInequalityIndices()
   {
      return activeInequalityIndices;
   }

   public void clear()
   {
      quadraticCostQMatrix.reshape(0, 0);
      quadraticCostQVector.reshape(0, 0);

      linearEqualityConstraintsAMatrix.reshape(0, 0);
      linearEqualityConstraintsBVector.reshape(0, 0);

      linearInequalityConstraintsCMatrixO.reshape(0, 0);
      linearInequalityConstraintsDVectorO.reshape(0, 0);
   }

   public void setQuadraticCostFunction(DMatrix H, DMatrix f)
   {
      setQuadraticCostFunction(H, f, 0.0);
   }

   public void setQuadraticCostFunction(DMatrix costQuadraticMatrix, DMatrix costLinearVector, double quadraticCostScalar)
   {
      if (costLinearVector.getNumCols() != 1)
         throw new RuntimeException("costLinearVector.getNumCols() != 1");
      if (costQuadraticMatrix.getNumRows() != costLinearVector.getNumRows())
         throw new RuntimeException("costQuadraticMatrix.getNumRows() != costLinearVector.getNumRows()");
      if (costQuadraticMatrix.getNumRows() != costQuadraticMatrix.getNumCols())
         throw new RuntimeException("costQuadraticMatrix.getNumRows() != costQuadraticMatrix.getNumCols()");

      this.costQuadraticMatrix.set(costQuadraticMatrix);
      symmetricCostQuadraticMatrix.transpose(this.costQuadraticMatrix);

      symmetricCostQuadraticMatrix.add(this.costQuadraticMatrix, symmetricCostQuadraticMatrix);

      quadraticCostQMatrix.set(symmetricCostQuadraticMatrix);
      quadraticCostQMatrix.scale(0.5);

      quadraticCostQVector.set(costLinearVector);
      this.quadraticCostScalar = quadraticCostScalar;
   }

   public double getObjectiveCost(DMatrixRMaj x)
   {
      nativexSolutionMatrix.set(x);

      computedObjectiveFunctionValue.multQuad(nativexSolutionMatrix, quadraticCostQMatrix);
      computedObjectiveFunctionValue.scale(0.5);

      computedObjectiveFunctionValue.multAddTransA(quadraticCostQVector, nativexSolutionMatrix);
      return computedObjectiveFunctionValue.get(0, 0) + quadraticCostScalar;
   }

   public void setLinearEqualityConstraints(DMatrix linearEqualityConstraintsAMatrix, DMatrix linearEqualityConstraintsBVector)
   {
      if (linearEqualityConstraintsBVector.getNumCols() != 1)
         throw new RuntimeException("linearEqualityConstraintsBVector.getNumCols() != 1");
      if (linearEqualityConstraintsAMatrix.getNumRows() != linearEqualityConstraintsBVector.getNumRows())
         throw new RuntimeException("linearEqualityConstraintsAMatrix.getNumRows() != linearEqualityConstraintsBVector.getNumRows()");
      if (linearEqualityConstraintsAMatrix.getNumCols() != quadraticCostQMatrix.getNumCols())
         throw new RuntimeException("linearEqualityConstraintsAMatrix.getNumCols() != quadraticCostQMatrix.getNumCols()");

      this.linearEqualityConstraintsBVector.set(linearEqualityConstraintsBVector);
      this.linearEqualityConstraintsAMatrix.set(linearEqualityConstraintsAMatrix);
   }

   public void setLinearInequalityConstraints(DMatrix linearInequalityConstraintCMatrix, DMatrix linearInequalityConstraintDVector)
   {
      if (linearInequalityConstraintDVector.getNumCols() != 1)
         throw new RuntimeException("linearInequalityConstraintDVector.getNumCols() != 1");
      if (linearInequalityConstraintCMatrix.getNumRows() != linearInequalityConstraintDVector.getNumRows())
         throw new RuntimeException("linearInequalityConstraintCMatrix.getNumRows() != linearInequalityConstraintDVector.getNumRows()");
      if (linearInequalityConstraintCMatrix.getNumCols() != quadraticCostQMatrix.getNumCols())
         throw new RuntimeException("linearInequalityConstraintCMatrix.getNumCols() != quadraticCostQMatrix.getNumCols()");

      linearInequalityConstraintsDVectorO.set(linearInequalityConstraintDVector);
      linearInequalityConstraintsCMatrixO.set(linearInequalityConstraintCMatrix);
   }

   public void setUseWarmStart(boolean useWarmStart)
   {
      this.useWarmStart = useWarmStart;
   }

   public void resetActiveSet()
   {
      CBar.reshape(0, 0);
      DBar.reshape(0, 0);

      activeInequalityIndices.reset();
   }

   private final NativeMatrix lagrangeEqualityConstraintMultipliers = new NativeMatrix(0, 0);
   private final NativeMatrix lagrangeInequalityConstraintMultipliers = new NativeMatrix(0, 0);

   public int solve(DMatrix solutionToPack)
   {
      if (!useWarmStart || (resetActiveSetOnSizeChange && problemSizeChanged()))
         resetActiveSet();
      else
         addActiveSetConstraintsAsEqualityConstraints();

      int numberOfIterations = 0;

      int numberOfVariables = quadraticCostQMatrix.getNumRows();
      int numberOfEqualityConstraints = linearEqualityConstraintsAMatrix.getNumRows();
      int numberOfInequalityConstraints = linearInequalityConstraintsCMatrixO.getNumRows();

      nativexSolutionMatrix.reshape(numberOfVariables, 1);
      lagrangeEqualityConstraintMultipliers.reshape(numberOfEqualityConstraints, 1);
      lagrangeEqualityConstraintMultipliers.zero();
      lagrangeInequalityConstraintMultipliers.reshape(numberOfInequalityConstraints, 1);
      lagrangeInequalityConstraintMultipliers.zero();

      computeQInverseAndAQInverse();

      solveEqualityConstrainedSubproblemEfficiently(nativexSolutionMatrix,
                                                    lagrangeEqualityConstraintMultipliers,
                                                    lagrangeInequalityConstraintMultipliers);

      //      System.out.println(numberOfInequalityConstraints + ", " + numberOfLowerBoundConstraints + ", " + numberOfUpperBoundConstraints);
      if (numberOfInequalityConstraints == 0)
      {
         solutionToPack.set(nativexSolutionMatrix);
         return numberOfIterations;
      }

      for (int i = 0; i < maxNumberOfIterations; i++)
      {
         boolean activeSetWasModified = modifyActiveSetAndTryAgain(nativexSolutionMatrix,
                                                                   lagrangeEqualityConstraintMultipliers,
                                                                   lagrangeInequalityConstraintMultipliers);
         numberOfIterations++;

         if (!activeSetWasModified)
         {
            solutionToPack.set(nativexSolutionMatrix);

            return numberOfIterations;
         }
      }

      // No solution found. Pack NaN in all variables
      if (reportFailedConvergenceAsNaN)
      {
         if (solutionToPack.getNumRows() != numberOfVariables)
            throw new IllegalArgumentException("Bad number of rows.");
         for (int i = 0; i < numberOfVariables; i++)
            solutionToPack.set(i, 0, Double.NaN);
      }
      else
      {
         solutionToPack.set(nativexSolutionMatrix);
      }

      return numberOfIterations;
   }

   private boolean problemSizeChanged()
   {
      boolean sizeChanged = checkProblemSize();

      previousNumberOfVariables = (int) CommonOps_DDRM.elementSum(activeVariables);
      previousNumberOfEqualityConstraints = linearEqualityConstraintsAMatrix.getNumRows();
      previousNumberOfInequalityConstraints = linearInequalityConstraintsCMatrixO.getNumRows();

      return sizeChanged;
   }

   private boolean checkProblemSize()
   {
      if (previousNumberOfVariables != CommonOps_DDRM.elementSum(activeVariables))
         return true;
      if (previousNumberOfEqualityConstraints != linearEqualityConstraintsAMatrix.getNumRows())
         return true;
      if (previousNumberOfInequalityConstraints != linearInequalityConstraintsCMatrixO.getNumRows())
         return true;

      return false;
   }

   private void computeQInverseAndAQInverse()
   {
      int numberOfVariables = quadraticCostQMatrix.getNumRows();
      int numberOfEqualityConstraints = linearEqualityConstraintsAMatrix.getNumRows();

      inverseSolver.computeInverse(quadraticCostQMatrix, QInverse);

      if (numberOfEqualityConstraints > 0)
      {
         AQInverse.mult(linearEqualityConstraintsAMatrix, QInverse);
         QInverseATranspose.multTransB(QInverse, linearEqualityConstraintsAMatrix);
         AQInverseATranspose.multTransB(AQInverse, linearEqualityConstraintsAMatrix);
      }
      else
      {
         AQInverse.reshape(numberOfEqualityConstraints, numberOfVariables);
         QInverseATranspose.reshape(numberOfVariables, numberOfEqualityConstraints);
         AQInverseATranspose.reshape(numberOfEqualityConstraints, numberOfEqualityConstraints);
      }
   }

   private void computeCBarTempMatrices()
   {
      if (CBar.getNumRows() > 0)
      {
         CBarQInverseATranspose.mult(CBar, QInverseATranspose);
         AQInverseCBarTranspose.transpose(CBarQInverseATranspose);

         CBarQInverse.mult(CBar, QInverse);
         QInverseCBarTranspose.transpose(CBarQInverse);

         CBarQInverseCBarTranspose.mult(CBar, QInverseCBarTranspose);
      }
      else
      {
         AQInverseCBarTranspose.reshape(0, 0);
         CBarQInverseATranspose.reshape(0, 0);
         CBarQInverse.reshape(0, 0);
         QInverseCBarTranspose.reshape(0, 0);
         CBarQInverseCBarTranspose.reshape(0, 0);
      }
   }

   private boolean modifyActiveSetAndTryAgain(NativeMatrix solutionToPack, NativeMatrix lagrangeEqualityConstraintMultipliersToPack,
                                              NativeMatrix lagrangeInequalityConstraintMultipliersToPack)
   {
      if (solutionToPack.containsNaN())
         return false;

      boolean activeSetWasModified = false;

      // find the constraints to add
      int numberOfInequalityConstraints = linearInequalityConstraintsCMatrixO.getNumRows();

      double maxInequalityViolation = Double.NEGATIVE_INFINITY;
      if (numberOfInequalityConstraints != 0)
      {
         linearInequalityConstraintsCheck.scale(-1.0, linearInequalityConstraintsDVectorO);
         linearInequalityConstraintsCheck.multAdd(linearInequalityConstraintsCMatrixO, solutionToPack);

         for (int i = 0; i < linearInequalityConstraintsCheck.getNumRows(); i++)
         {
            if (activeInequalityIndices.contains(i))
               continue;

            if (linearInequalityConstraintsCheck.get(i, 0) >= maxInequalityViolation)
               maxInequalityViolation = linearInequalityConstraintsCheck.get(i, 0);
         }
      }



      double minViolationToAdd = (1.0 - violationFractionToAdd) * maxInequalityViolation + convergenceThreshold;

      // check inequality constraints
      inequalityIndicesToAddToActiveSet.reset();
      if (maxInequalityViolation > minViolationToAdd)
      {
         for (int i = 0; i < numberOfInequalityConstraints; i++)
         {
            if (activeInequalityIndices.contains(i))
               continue; // Only check violation on those that are not active. Otherwise check should just return 0.0, but roundoff could cause problems.

            if (linearInequalityConstraintsCheck.get(i, 0) > minViolationToAdd)
            {
               activeSetWasModified = true;
               inequalityIndicesToAddToActiveSet.add(i);
            }
         }
      }

      // find the constraints to remove
      int numberOfActiveInequalityConstraints = activeInequalityIndices.size();

      double minLagrangeInequalityMultiplier = Double.POSITIVE_INFINITY;

      if (numberOfActiveInequalityConstraints != 0)
         minLagrangeInequalityMultiplier = lagrangeInequalityConstraintMultipliersToPack.min();

      double maxLagrangeMultiplierToRemove = -(1.0 - violationFractionToRemove) * minLagrangeInequalityMultiplier - convergenceThresholdForLagrangeMultipliers;

      inequalityIndicesToRemoveFromActiveSet.reset();
      if (minLagrangeInequalityMultiplier < maxLagrangeMultiplierToRemove)
      {
         for (int i = 0; i < activeInequalityIndices.size(); i++)
         {
            int indexToCheck = activeInequalityIndices.get(i);

            double lagrangeMultiplier = lagrangeInequalityConstraintMultipliersToPack.get(indexToCheck, 0);
            if (lagrangeMultiplier < maxLagrangeMultiplierToRemove)
            {
               activeSetWasModified = true;
               inequalityIndicesToRemoveFromActiveSet.add(indexToCheck);
            }
         }
      }


      if (!activeSetWasModified)
         return false;

      for (int i = 0; i < inequalityIndicesToAddToActiveSet.size(); i++)
      {
         activeInequalityIndices.add(inequalityIndicesToAddToActiveSet.get(i));
      }
      for (int i = 0; i < inequalityIndicesToRemoveFromActiveSet.size(); i++)
      {
         activeInequalityIndices.remove(inequalityIndicesToRemoveFromActiveSet.get(i));
      }

      // Add active set constraints as equality constraints:
      addActiveSetConstraintsAsEqualityConstraints();

      solveEqualityConstrainedSubproblemEfficiently(solutionToPack,
                                                    lagrangeEqualityConstraintMultipliersToPack,
                                                    lagrangeInequalityConstraintMultipliersToPack);

      return true;
   }

   private void addActiveSetConstraintsAsEqualityConstraints()
   {
      int numberOfVariables = quadraticCostQMatrix.getNumRows();

      int sizeOfActiveSet = activeInequalityIndices.size();

      CBar.reshape(sizeOfActiveSet, numberOfVariables);
      DBar.reshape(sizeOfActiveSet, 1);

      for (int i = 0; i < sizeOfActiveSet; i++)
      {
         int inequalityConstraintIndex = activeInequalityIndices.get(i);
         CBar.insert(linearInequalityConstraintsCMatrixO, inequalityConstraintIndex, inequalityConstraintIndex + 1, 0, numberOfVariables, i, 0);
         DBar.insert(linearInequalityConstraintsDVectorO, inequalityConstraintIndex, inequalityConstraintIndex + 1, 0, 1, i, 0);
      }

      //printSetChanges();
   }

   private void printSetChanges()
   {
      if (!inequalityIndicesToAddToActiveSet.isEmpty())
      {
         LogTools.info("Inequality constraint indices added : ");
         for (int i = 0; i < inequalityIndicesToAddToActiveSet.size(); i++)
            LogTools.info("" + inequalityIndicesToAddToActiveSet.get(i));
      }
      if (!inequalityIndicesToRemoveFromActiveSet.isEmpty())
      {
         LogTools.info("Inequality constraint indices removed : ");
         for (int i = 0; i < inequalityIndicesToRemoveFromActiveSet.size(); i++)
            LogTools.info("" + inequalityIndicesToRemoveFromActiveSet.get(i));
      }
   }

   private void solveEqualityConstrainedSubproblemEfficiently(NativeMatrix xSolutionToPack, NativeMatrix lagrangeEqualityConstraintMultipliersToPack,
                                                              NativeMatrix lagrangeInequalityConstraintMultipliersToPack)
   {
      int numberOfVariables = quadraticCostQMatrix.getNumRows();
      int numberOfOriginalEqualityConstraints = linearEqualityConstraintsAMatrix.getNumRows();

      int numberOfActiveInequalityConstraints = activeInequalityIndices.size();

      int numberOfAugmentedEqualityConstraints = numberOfOriginalEqualityConstraints + numberOfActiveInequalityConstraints;

      if (numberOfAugmentedEqualityConstraints == 0)
      {
         xSolutionToPack.mult(-1.0, QInverse, quadraticCostQVector);
         reportSolution(xSolutionToPack);
         return;
      }

      computeCBarTempMatrices();

      bigMatrixForLagrangeMultiplierSolution.reshape(numberOfAugmentedEqualityConstraints, numberOfAugmentedEqualityConstraints);
      bigVectorForLagrangeMultiplierSolution.reshape(numberOfAugmentedEqualityConstraints, 1);

      bigMatrixForLagrangeMultiplierSolution.insert(AQInverseATranspose, 0, 0);
      bigMatrixForLagrangeMultiplierSolution.insert(AQInverseCBarTranspose, 0, numberOfOriginalEqualityConstraints);

      bigMatrixForLagrangeMultiplierSolution.insert(CBarQInverseATranspose, numberOfOriginalEqualityConstraints, 0);
      bigMatrixForLagrangeMultiplierSolution.insert(CBarQInverseCBarTranspose, numberOfOriginalEqualityConstraints, numberOfOriginalEqualityConstraints);

      if (numberOfOriginalEqualityConstraints > 0)
      {
         bigVectorForLagrangeMultiplierSolution.insert(linearEqualityConstraintsBVector, 0, 0);
         bigVectorForLagrangeMultiplierSolution.multAddBlock(AQInverse, quadraticCostQVector, 0, 0);
      }

      if (numberOfActiveInequalityConstraints > 0)
      {
         bigVectorForLagrangeMultiplierSolution.insert(DBar, numberOfOriginalEqualityConstraints, 0);
         bigVectorForLagrangeMultiplierSolution.multAddBlock(CBarQInverse, quadraticCostQVector, numberOfOriginalEqualityConstraints, 0);
      }

      bigVectorForLagrangeMultiplierSolution.scale(-1.0, bigVectorForLagrangeMultiplierSolution);

      augmentedLagrangeMultipliers.solve(bigMatrixForLagrangeMultiplierSolution, bigVectorForLagrangeMultiplierSolution);
      
      AAndC.reshape(numberOfAugmentedEqualityConstraints, numberOfVariables);
      AAndC.insert(linearEqualityConstraintsAMatrix, 0, 0);
      AAndC.insert(CBar, numberOfOriginalEqualityConstraints, 0);

      ATransposeMuAndCTransposeLambda.multTransA(AAndC, augmentedLagrangeMultipliers);

      tempVector.add(quadraticCostQVector, ATransposeMuAndCTransposeLambda);

      xSolutionToPack.mult(-1.0, QInverse, tempVector);
      reportSolution(xSolutionToPack);

      int startRow = 0;
      int numberOfRows = numberOfOriginalEqualityConstraints;
      lagrangeEqualityConstraintMultipliersToPack.insert(augmentedLagrangeMultipliers, startRow, startRow + numberOfRows, 0, 1, 0, 0);

      startRow += numberOfRows;
      lagrangeInequalityConstraintMultipliersToPack.zero();
      for (int i = 0; i < numberOfActiveInequalityConstraints; i++)
      {
         int inequalityConstraintIndex = activeInequalityIndices.get(i);
         lagrangeInequalityConstraintMultipliersToPack.insert(augmentedLagrangeMultipliers, startRow + i, startRow + i + 1, 0, 1, inequalityConstraintIndex, 0);
      }
   }

   private void reportSolution(NativeMatrix solution)
   {
      for (int i = 0; i < solutionListeners.size(); i++)
         solutionListeners.get(i).reportSolution(solution, activeInequalityIndices, null, null);
   }

   public void getLagrangeEqualityConstraintMultipliers(DMatrixRMaj multipliersMatrixToPack)
   {
      lagrangeEqualityConstraintMultipliers.get(multipliersMatrixToPack);
   }

   public void getLagrangeInequalityConstraintMultipliers(DMatrixRMaj multipliersMatrixToPack)
   {
      lagrangeInequalityConstraintMultipliers.get(multipliersMatrixToPack);
   }

   public void setInverseHessianCalculator(InverseMatrixCalculator<NativeMatrix> inverseSolver)
   {
      this.inverseSolver = inverseSolver;
   }

   private static class DefaultInverseMatrixCalculator implements InverseMatrixCalculator<NativeMatrix>
   {
      @Override
      public void computeInverse(NativeMatrix matrixToInvert, NativeMatrix inverseMatrixToPack)
      {
         inverseMatrixToPack.invert(matrixToInvert);
      }
   }
}
