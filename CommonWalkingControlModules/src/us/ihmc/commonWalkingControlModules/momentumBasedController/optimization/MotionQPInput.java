package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import org.ejml.data.DenseMatrix64F;

public class MotionQPInput
{
   public final DenseMatrix64F taskJacobian;
   public final DenseMatrix64F taskObjective;
   public final DenseMatrix64F taskWeightMatrix;
   private double taskWeightScalar;
   private boolean useWeightScalar = false;
   private boolean isMotionConstraint = false;
   private final int numberOfDoFs;

   /**
    * <p>
    *    Motion objective input into the QP solver.
    *    Must be in the form
    * </p>
    * <p>
    *    J*x - x
    * </p>
    * <p>
    *    where the overall desire is minimize the objective.
    * </p>
    */
   public MotionQPInput(int numberOfDoFs)
   {
      this.numberOfDoFs = numberOfDoFs;
      taskJacobian = new DenseMatrix64F(numberOfDoFs, numberOfDoFs);
      taskObjective = new DenseMatrix64F(numberOfDoFs, 1);
      taskWeightMatrix = new DenseMatrix64F(numberOfDoFs, numberOfDoFs);
   }

   public void reshape(int taskSize)
   {
      taskJacobian.reshape(taskSize, numberOfDoFs);
      taskObjective.reshape(taskSize, 1);
      taskWeightMatrix.reshape(taskSize, taskSize);
   }

   public void setTaskJacobian(DenseMatrix64F taskJacobian)
   {
      this.taskJacobian.set(taskJacobian);
   }

   public void setTaskObjective(DenseMatrix64F taskObjective)
   {
      this.taskObjective.set(taskObjective);
   }

   public void setTaskWeightMatrix(DenseMatrix64F taskWeightMatrix)
   {
      this.taskWeightMatrix.set(taskWeightMatrix);
   }

   public void setIsMotionConstraint(boolean isMotionConstraint)
   {
      this.isMotionConstraint = isMotionConstraint;
   }

   public void setUseWeightScalar(boolean useWeightScalar)
   {
      this.useWeightScalar = useWeightScalar;
   }

   public void setWeight(double weight)
   {
      this.taskWeightScalar = weight;
   }

   public double getWeightScalar()
   {
      return taskWeightScalar;
   }

   public boolean useWeightScalar()
   {
      return useWeightScalar;
   }

   public boolean isMotionConstraint()
   {
      return isMotionConstraint;
   }
}
