package us.ihmc.sensorProcessing.imu;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.utilities.IMUDefinition;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

public class IMUSensor implements IMUSensorReadOnly
{
   private final ReferenceFrame measurementFrame;
   private final RigidBody measurementLink;
   
   private final Matrix3d orientationMeasurement = new Matrix3d();
   private final Vector3d angularVelocityMeasurement = new Vector3d();
   private final Vector3d linearAccelerationMeasurement = new Vector3d();

   private final DenseMatrix64F orientationNoiseCovariance;
   private final DenseMatrix64F angularVelocityNoiseCovariance;
   private final DenseMatrix64F angularVelocityBiasProcessNoiseCovariance;
   private final DenseMatrix64F linearAccelerationNoiseCovariance;
   private final DenseMatrix64F linearAccelerationBiasProcessNoiseCovariance;

   public IMUSensor(IMUDefinition imuDefinition, SensorNoiseParameters sensorNoiseParameters)
   {
      measurementFrame = imuDefinition.getIMUFrame();
      measurementLink = imuDefinition.getRigidBody();

      orientationNoiseCovariance = createDiagonalCovarianceMatrix(sensorNoiseParameters.getOrientationMeasurementStandardDeviation());

      angularVelocityNoiseCovariance = createDiagonalCovarianceMatrix(sensorNoiseParameters.getAngularVelocityMeasurementStandardDeviation());
      angularVelocityBiasProcessNoiseCovariance = createDiagonalCovarianceMatrix(sensorNoiseParameters.getAngularVelocityBiasProcessNoiseStandardDeviation());

      linearAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(sensorNoiseParameters.getLinearAccelerationMeasurementStandardDeviation());
      linearAccelerationBiasProcessNoiseCovariance = createDiagonalCovarianceMatrix(sensorNoiseParameters.getLinearAccelerationBiasProcessNoiseStandardDeviation());
   }

   public ReferenceFrame getMeasurementFrame()
   {
      return measurementFrame;
   }

   public RigidBody getMeasurementLink()
   {
      return measurementLink;
   }

   public void setOrientationMeasurement(Matrix3d newOrientation)
   {
      orientationMeasurement.set(newOrientation);
   }

   public void setAngularVelocityMeasurement(Vector3d newAngularOrientation)
   {
      angularVelocityMeasurement.set(newAngularOrientation);
   }

   public void setLinearAccelerationMeasurement(Vector3d newLinearAcceleration)
   {
      linearAccelerationMeasurement.set(newLinearAcceleration);
   }

   public void getOrientationMeasurement(Matrix3d orientationToPack)
   {
      orientationToPack.set(orientationMeasurement);
   }

   public void getAngularVelocityMeasurement(Vector3d angularVelocityToPack)
   {
      angularVelocityToPack.set(angularVelocityMeasurement);
   }

   public void getLinearAccelerationMeasurement(Vector3d linearAccelerationToPack)
   {
      linearAccelerationToPack.set(linearAccelerationMeasurement);
   }

   public void getOrientationNoiseCovariance(DenseMatrix64F noiseCovarianceToPack)
   {
      noiseCovarianceToPack.set(orientationNoiseCovariance);
   }

   public void getAngularVelocityNoiseCovariance(DenseMatrix64F noiseCovarianceToPack)
   {
      noiseCovarianceToPack.set(angularVelocityNoiseCovariance);
   }

   public void getAngularVelocityBiasProcessNoiseCovariance(DenseMatrix64F biasProcessNoiseCovarianceToPack)
   {
      biasProcessNoiseCovarianceToPack.set(angularVelocityBiasProcessNoiseCovariance);
   }

   public void getLinearAccelerationNoiseCovariance(DenseMatrix64F noiseCovarianceToPack)
   {
      noiseCovarianceToPack.set(linearAccelerationNoiseCovariance);
   }

   public void getLinearAccelerationBiasProcessNoiseCovariance(DenseMatrix64F biasProcessNoiseCovarianceToPack)
   {
      biasProcessNoiseCovarianceToPack.set(linearAccelerationBiasProcessNoiseCovariance);
   }

   private static DenseMatrix64F createDiagonalCovarianceMatrix(double standardDeviation)
   {
      DenseMatrix64F orientationCovarianceMatrix = new DenseMatrix64F(3, 3);
      CommonOps.setIdentity(orientationCovarianceMatrix);
      CommonOps.scale(MathTools.square(standardDeviation), orientationCovarianceMatrix);

      return orientationCovarianceMatrix;
   }
}
