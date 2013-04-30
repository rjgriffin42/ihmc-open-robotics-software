package us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.utilities.IMUDefinition;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;

public class SensorConfigurationFactory
{
   private final Vector3d gravitationalAcceleration;
   private final SensorNoiseParameters sensorNoiseParameters;

   public SensorConfigurationFactory(SensorNoiseParameters sensorNoiseParameters, Vector3d gravitationalAcceleration)
   {
      this.gravitationalAcceleration = new Vector3d();
      this.gravitationalAcceleration.set(gravitationalAcceleration);
      this.sensorNoiseParameters = sensorNoiseParameters;
   }

   public Collection<OrientationSensorConfiguration> createOrientationSensorConfigurations(Map<IMUDefinition,
                     ControlFlowOutputPort<Matrix3d>> orientationSensors)
   {
      ArrayList<OrientationSensorConfiguration> orientationSensorConfigurations = new ArrayList<OrientationSensorConfiguration>();

      Set<IMUDefinition> imuDefinitions = orientationSensors.keySet();
      for (IMUDefinition estimatedIMUDefinition : imuDefinitions)
      {
         String sensorName = estimatedIMUDefinition.getName() + "Orientation";

         double orientationMeasurementStandardDeviation = sensorNoiseParameters.getOrientationMeasurementStandardDeviation();
         DenseMatrix64F orientationNoiseCovariance = createDiagonalCovarianceMatrix(orientationMeasurementStandardDeviation, 3);

         RigidBody estimatedMeasurementBody = estimatedIMUDefinition.getRigidBody();
         ReferenceFrame estimatedMeasurementFrame = createMeasurementFrame(sensorName, "EstimatedMeasurementFrame", estimatedIMUDefinition,
                                                       estimatedMeasurementBody);

         ControlFlowOutputPort<Matrix3d> outputPort = orientationSensors.get(estimatedIMUDefinition);

         OrientationSensorConfiguration orientationSensorConfiguration = new OrientationSensorConfiguration(outputPort, sensorName, estimatedMeasurementFrame,
                                                                            orientationNoiseCovariance);
         orientationSensorConfigurations.add(orientationSensorConfiguration);
      }


      return orientationSensorConfigurations;
   }

   public ArrayList<AngularVelocitySensorConfiguration> createAngularVelocitySensorConfigurations(Map<IMUDefinition,
                    ControlFlowOutputPort<Vector3d>> angularVelocitySensors)
   {
      ArrayList<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations = new ArrayList<AngularVelocitySensorConfiguration>();

      Set<IMUDefinition> imuDefinitions = angularVelocitySensors.keySet();
      for (IMUDefinition estimatedIMUDefinition : imuDefinitions)
      {
         String sensorName = estimatedIMUDefinition.getName() + "AngularVelocity";

         double angularVelocityMeasurementStandardDeviation = sensorNoiseParameters.getAngularVelocityMeasurementStandardDeviation();
         DenseMatrix64F angularVelocityNoiseCovariance = createDiagonalCovarianceMatrix(angularVelocityMeasurementStandardDeviation, 3);
         double angularVelocityBiasProcessNoiseStandardDeviation = sensorNoiseParameters.getAngularVelocityBiasProcessNoiseStandardDeviation();
         DenseMatrix64F angularVelocityBiasProcessNoiseCovariance = createDiagonalCovarianceMatrix(angularVelocityBiasProcessNoiseStandardDeviation, 3);

         RigidBody estimatedMeasurementBody = estimatedIMUDefinition.getRigidBody();
         ReferenceFrame estimatedMeasurementFrame = createMeasurementFrame(sensorName, "EstimatedMeasurementFrame", estimatedIMUDefinition,
                                                       estimatedMeasurementBody);

         ControlFlowOutputPort<Vector3d> outputPort = angularVelocitySensors.get(estimatedIMUDefinition);

         AngularVelocitySensorConfiguration angularVelocitySensorConfiguration = new AngularVelocitySensorConfiguration(outputPort, sensorName,
                                                                                    estimatedMeasurementBody, estimatedMeasurementFrame,
                                                                                    angularVelocityNoiseCovariance, angularVelocityBiasProcessNoiseCovariance);
         angularVelocitySensorConfigurations.add(angularVelocitySensorConfiguration);
      }


      return angularVelocitySensorConfigurations;
   }


   public ArrayList<LinearAccelerationSensorConfiguration> createLinearAccelerationSensorConfigurations(Map<IMUDefinition,
                    ControlFlowOutputPort<Vector3d>> linearAccelerationSensors)
   {
      ArrayList<LinearAccelerationSensorConfiguration> linearAccelerationSensorConfigurations = new ArrayList<LinearAccelerationSensorConfiguration>();

      Set<IMUDefinition> imuDefinitions = linearAccelerationSensors.keySet();
      for (IMUDefinition estimatedIMUDefinition : imuDefinitions)
      {
         String sensorName = estimatedIMUDefinition.getName() + "LinearAcceleration";

         double linearAccelerationMeasurementStandardDeviation = sensorNoiseParameters.getLinearAccelerationMeasurementStandardDeviation();
         DenseMatrix64F linearAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(linearAccelerationMeasurementStandardDeviation, 3);
         double linearAccelerationBiasProcessNoiseStandardDeviation = sensorNoiseParameters.getLinearAccelerationBiasProcessNoiseStandardDeviation();
         DenseMatrix64F linearAccelerationBiasProcessNoiseCovariance = createDiagonalCovarianceMatrix(linearAccelerationBiasProcessNoiseStandardDeviation, 3);

         RigidBody estimatedMeasurementBody = estimatedIMUDefinition.getRigidBody();
         ReferenceFrame estimatedMeasurementFrame = createMeasurementFrame(sensorName, "EstimatedMeasurementFrame", estimatedIMUDefinition,
                                                       estimatedMeasurementBody);

         ControlFlowOutputPort<Vector3d> outputPort = linearAccelerationSensors.get(estimatedIMUDefinition);

         LinearAccelerationSensorConfiguration linearAccelerationSensorConfiguration = new LinearAccelerationSensorConfiguration(outputPort, sensorName,
                                                                                          estimatedMeasurementBody, estimatedMeasurementFrame,
                                                                                          gravitationalAcceleration.getZ(), linearAccelerationNoiseCovariance,
                                                                                          linearAccelerationBiasProcessNoiseCovariance);

         linearAccelerationSensorConfigurations.add(linearAccelerationSensorConfiguration);
      }

      return linearAccelerationSensorConfigurations;
   }

   private static DenseMatrix64F createDiagonalCovarianceMatrix(double standardDeviation, int size)
   {
      DenseMatrix64F orientationCovarianceMatrix = new DenseMatrix64F(size, size);
      CommonOps.setIdentity(orientationCovarianceMatrix);
      CommonOps.scale(MathTools.square(standardDeviation), orientationCovarianceMatrix);

      return orientationCovarianceMatrix;
   }

   public static ReferenceFrame createMeasurementFrame(String sensorName, String frameName, IMUDefinition imuDefinition, RigidBody measurementBody)
   {
      Transform3D transformFromIMUToJoint = new Transform3D();
      imuDefinition.getTransformFromIMUToJoint(transformFromIMUToJoint);

      ReferenceFrame perfectFrameAfterJoint = measurementBody.getParentJoint().getFrameAfterJoint();

      if (transformFromIMUToJoint.epsilonEquals(new Transform3D(), 1e-10))
      {
         return perfectFrameAfterJoint;
      }

      ReferenceFrame perfectMeasurementFrame = ReferenceFrame.constructBodyFrameWithUnchangingTransformToParent(sensorName + frameName, perfectFrameAfterJoint,
                                                  transformFromIMUToJoint);

      return perfectMeasurementFrame;
   }
}
