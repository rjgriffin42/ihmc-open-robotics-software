package us.ihmc.commonWalkingControlModules.momentumBasedController.optimization;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import us.ihmc.utilities.screwTheory.Momentum;

/**
 * @author twan
 *         Date: 5/1/13
 */
public class MomentumOptimizationSettings
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final DoubleYoVariable linearMomentumXYWeight = new DoubleYoVariable("linearMomentumXYWeight", registry);
   private final DoubleYoVariable linearMomentumZWeight = new DoubleYoVariable("linearMomentumZWeight", registry);
   private final DoubleYoVariable angularMomentumXYWeight = new DoubleYoVariable("angularMomentumXYWeight", registry);
   private final DoubleYoVariable angularMomentumZWeight = new DoubleYoVariable("angularMomentumZWeight", registry);
   private final double[] momentumWeightDiagonal = new double[Momentum.SIZE];

   private final DenseMatrix64F C = new DenseMatrix64F(Momentum.SIZE, Momentum.SIZE);
   private double wRho;
   private double lambda;
   private double rhoMin;

   private final DenseMatrix64F momentumSubspaceProjector = new DenseMatrix64F(Momentum.SIZE, Momentum.SIZE);
   private final DenseMatrix64F tempMatrix = new DenseMatrix64F(Momentum.SIZE, Momentum.SIZE);
   private final Momentum tempMomentum = new Momentum(); // just to make sure that the ordering of force and torque are correct

   public MomentumOptimizationSettings(YoVariableRegistry parentRegistry)
   {
      parentRegistry.addChild(registry);
   }

   public void setMomentumWeight(double linearMomentumXYWeight, double linearMomentumZWeight, double angularMomentumXYWeight, double angularMomentumZWeight)
   {
      this.linearMomentumXYWeight.set(linearMomentumXYWeight);
      this.linearMomentumZWeight.set(linearMomentumZWeight);
      this.angularMomentumXYWeight.set(angularMomentumXYWeight);
      this.angularMomentumZWeight.set(angularMomentumZWeight);
   }

   public void setGroundReactionForceRegularization(double wRho)
   {
      this.wRho = wRho;
   }

   public void setDampedLeastSquaresFactor(double lambda)
   {
      this.lambda = lambda;
   }

   public void setRhoMin(double rhoMin)
   {
      this.rhoMin = rhoMin;
   }

   public DenseMatrix64F getMomentumDotWeight(DenseMatrix64F momentumSubspace)
   {
      CommonOps.multOuter(momentumSubspace, momentumSubspaceProjector);

      tempMomentum.setLinearPartX(linearMomentumXYWeight.getDoubleValue());
      tempMomentum.setLinearPartY(linearMomentumXYWeight.getDoubleValue());
      tempMomentum.setLinearPartZ(linearMomentumZWeight.getDoubleValue());

      tempMomentum.setAngularPartX(angularMomentumXYWeight.getDoubleValue());
      tempMomentum.setAngularPartY(angularMomentumXYWeight.getDoubleValue());
      tempMomentum.setAngularPartZ(angularMomentumXYWeight.getDoubleValue());

      tempMomentum.packMatrix(momentumWeightDiagonal);
      CommonOps.diag(tempMatrix, Momentum.SIZE, momentumWeightDiagonal);

      CommonOps.mult(momentumSubspaceProjector, tempMatrix, C);

      return C;
   }

   public double getGroundReactionForceRegularization()
   {
      return wRho;
   }

   public DenseMatrix64F getDampedLeastSquaresFactorMatrix(int size)
   {
      DenseMatrix64F ret = new DenseMatrix64F(size, size);
      CommonOps.setIdentity(ret);
      CommonOps.scale(lambda, ret);

      return ret;
   }

   public double getRhoMinScalar()
   {
      return rhoMin;
   }
}
