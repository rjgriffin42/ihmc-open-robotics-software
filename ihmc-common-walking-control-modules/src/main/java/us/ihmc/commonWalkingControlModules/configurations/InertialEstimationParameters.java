package us.ihmc.commonWalkingControlModules.configurations;

import org.ejml.data.DMatrixRMaj;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.InertialParameterManagerFactory;
import us.ihmc.mecano.algorithms.JointTorqueRegressorCalculator;

import java.util.Set;

public interface InertialEstimationParameters
{
   public abstract InertialParameterManagerFactory.EstimatorType getTypeOfEstimatorToUse();

   public abstract Set<JointTorqueRegressorCalculator.SpatialInertiaBasisOption>[] getParametersToEstimate();

   public abstract DMatrixRMaj getURDFParameters(Set<JointTorqueRegressorCalculator.SpatialInertiaBasisOption>[] basisSets);

   public abstract double getBreakFrequencyForPostProcessing();
   public abstract double getBreakFrequencyForEstimateFiltering();

   public abstract double getBreakFrequencyForAccelerationCalculation();

   public abstract double getBiasCompensationWindowSizeInSeconds();

   public abstract double getProcessModelCovariance();
   public double[] getProcessModelCovarianceForBody();
   public abstract double getProcessCovarianceMultiplierForWalking();


   public abstract double getFloatingBaseMeasurementCovariance();
   public abstract double getLegMeasurementCovariance();
   public abstract double getArmMeasurementCovariance();
   public abstract double getSpineMeasurementCovariance();

   public abstract double getNormalizedInnovationThreshold();

   /** CONSTRAINED_KF */

   public abstract int getMaxNumberOfIterationsForQP();

   public abstract double getMinimumMassMultiplier();
   public abstract double getMinimumDiagonalInertiaMultiplier();
}
