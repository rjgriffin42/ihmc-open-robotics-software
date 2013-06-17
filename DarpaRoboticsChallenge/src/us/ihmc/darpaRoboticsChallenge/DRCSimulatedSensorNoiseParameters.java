package us.ihmc.darpaRoboticsChallenge;

import java.util.Random;

import javax.vecmath.Vector3d;

import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.Direction;

public class DRCSimulatedSensorNoiseParameters
{
   private static final Random random = new Random(1999L);
   private static final boolean TURN_IMU_BIASES_OFF = true;

   public static SensorNoiseParameters createSensorNoiseParametersZeroNoise()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();
      return sensorNoiseParameters;
   }
      
   public static SensorNoiseParameters createSensorNoiseParametersALittleNoise()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();

      sensorNoiseParameters.setComAccelerationProcessNoiseStandardDeviation(0.0); // Not used.
      sensorNoiseParameters.setAngularAccelerationProcessNoiseStandardDeviation(0.0); // Not used.
      
      sensorNoiseParameters.setOrientationMeasurementStandardDeviation(0.01);
      sensorNoiseParameters.setAngularVelocityMeasurementStandardDeviation(0.01);
      sensorNoiseParameters.setLinearAccelerationMeasurementStandardDeviation(0.2);

      sensorNoiseParameters.setAngularVelocityBiasProcessNoiseStandardDeviation(0.001);
      sensorNoiseParameters.setLinearAccelerationBiasProcessNoiseStandardDeviation(0.001);
      
      Vector3d initialLinearAccelerationBias = new Vector3d(0.01, 0.02, 0.033);
      Vector3d initialAngularVelocityBias = new Vector3d(0.005, -0.007, 0.02);

      sensorNoiseParameters.setInitialLinearVelocityBias(initialLinearAccelerationBias);
      sensorNoiseParameters.setInitialAngularVelocityBias(initialAngularVelocityBias);

      return sensorNoiseParameters;
   }
   
   public static SensorNoiseParameters createSensorNoiseParametersMediumNoisy()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();

      sensorNoiseParameters.setComAccelerationProcessNoiseStandardDeviation(Math.sqrt(1e-1));
      sensorNoiseParameters.setAngularAccelerationProcessNoiseStandardDeviation(Math.sqrt(1e-1));
      
      sensorNoiseParameters.setOrientationMeasurementStandardDeviation(Math.sqrt(1e-2));
      sensorNoiseParameters.setAngularVelocityMeasurementStandardDeviation(1e-1);
      sensorNoiseParameters.setLinearAccelerationMeasurementStandardDeviation(1e0);

      sensorNoiseParameters.setAngularVelocityBiasProcessNoiseStandardDeviation(Math.sqrt(1e-5));
      sensorNoiseParameters.setLinearAccelerationBiasProcessNoiseStandardDeviation(Math.sqrt(1e-4));

      double gazeboAngularVelocityBiasStandardDeviation = 0.0000008;
      double gazeboLinearAccelerationBiasStandardDeviation = 0.001;

      double gazeboAngularVelocityBiasMean = 0.0000075;
      double gazeboLinearAccelerationBiasMean = 0.1;
      
      Vector3d initialLinearAccelerationBias = computeGazeboBiasVector(gazeboLinearAccelerationBiasMean, gazeboLinearAccelerationBiasStandardDeviation, random);
      Vector3d initialAngularVelocityBias = computeGazeboBiasVector(gazeboAngularVelocityBiasMean, gazeboAngularVelocityBiasStandardDeviation, random);

      sensorNoiseParameters.setInitialLinearVelocityBias(initialLinearAccelerationBias);
      sensorNoiseParameters.setInitialAngularVelocityBias(initialAngularVelocityBias);

      return sensorNoiseParameters;
   }
   
   
   public static SensorNoiseParameters createSensorNoiseParametersGazeboSDF()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();

//      sensorNoiseParameters.setComAccelerationProcessNoiseStandardDeviation(Math.sqrt(1e-1));
//      sensorNoiseParameters.setAngularAccelerationProcessNoiseStandardDeviation(Math.sqrt(1e-1));
      
      sensorNoiseParameters.setOrientationMeasurementStandardDeviation(0.0);
      sensorNoiseParameters.setAngularVelocityMeasurementStandardDeviation(0.000200);
      sensorNoiseParameters.setLinearAccelerationMeasurementStandardDeviation(0.017);

      sensorNoiseParameters.setAngularVelocityBiasProcessNoiseStandardDeviation(0.0);
      sensorNoiseParameters.setLinearAccelerationBiasProcessNoiseStandardDeviation(0.0);

      if (!TURN_IMU_BIASES_OFF)
      {
         double gazeboAngularVelocityBiasStandardDeviation = 0.000008;
         double gazeboAngularVelocityBiasMean = 0.000001;

         double gazeboLinearAccelerationBiasStandardDeviation = 0.017000;
         double gazeboLinearAccelerationBiasMean =  0.100000;

         Vector3d initialLinearAccelerationBias = computeGazeboBiasVector(gazeboLinearAccelerationBiasMean, gazeboLinearAccelerationBiasStandardDeviation, random);
         Vector3d initialAngularVelocityBias = computeGazeboBiasVector(gazeboAngularVelocityBiasMean, gazeboAngularVelocityBiasStandardDeviation, random);

         sensorNoiseParameters.setInitialLinearVelocityBias(initialLinearAccelerationBias);
         sensorNoiseParameters.setInitialAngularVelocityBias(initialAngularVelocityBias);
      }
      
      return sensorNoiseParameters;
   }


   public static SensorNoiseParameters createNoiseParametersForEstimatorBasedOnGazeboSDF()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();

      sensorNoiseParameters.setComAccelerationProcessNoiseStandardDeviation(1.0); //0.3);
      sensorNoiseParameters.setAngularAccelerationProcessNoiseStandardDeviation(1.0); //0.3);
      
      sensorNoiseParameters.setOrientationMeasurementStandardDeviation(0.002);
      sensorNoiseParameters.setAngularVelocityMeasurementStandardDeviation(0.0002);
      sensorNoiseParameters.setLinearAccelerationMeasurementStandardDeviation(0.017);

      sensorNoiseParameters.setAngularVelocityBiasProcessNoiseStandardDeviation(1e-8);
      sensorNoiseParameters.setLinearAccelerationBiasProcessNoiseStandardDeviation(0.02); //1e-3);

      return sensorNoiseParameters;
   }
   
   public static SensorNoiseParameters createNoiseParametersForEstimatorJerryTuningApril30()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();

      sensorNoiseParameters.setComAccelerationProcessNoiseStandardDeviation(1.0); //1.0); //0.3);
      sensorNoiseParameters.setAngularAccelerationProcessNoiseStandardDeviation(1.0); //1e3); //0.3);
      
      sensorNoiseParameters.setOrientationMeasurementStandardDeviation(0.001);
      sensorNoiseParameters.setAngularVelocityMeasurementStandardDeviation(0.001);
      sensorNoiseParameters.setLinearAccelerationMeasurementStandardDeviation(1e4); //1e4); //0.01); //1.0); //1.0); //0.1); //0.1);

      sensorNoiseParameters.setAngularVelocityBiasProcessNoiseStandardDeviation(1e-6);
      sensorNoiseParameters.setLinearAccelerationBiasProcessNoiseStandardDeviation(0.001); //1e-6); //0.02); //1e-3);

      return sensorNoiseParameters;
   }
   
   
   public static SensorNoiseParameters createNoiseParametersForEstimatorJerryTuning()
   {
      SensorNoiseParameters sensorNoiseParameters = new SensorNoiseParameters();

      sensorNoiseParameters.setComAccelerationProcessNoiseStandardDeviation(1.0); //3.0);
      sensorNoiseParameters.setAngularAccelerationProcessNoiseStandardDeviation(3.0);
      
      sensorNoiseParameters.setOrientationMeasurementStandardDeviation(0.1);
      sensorNoiseParameters.setAngularVelocityMeasurementStandardDeviation(0.1);
      sensorNoiseParameters.setLinearAccelerationMeasurementStandardDeviation(1.0);

      sensorNoiseParameters.setAngularVelocityBiasProcessNoiseStandardDeviation(0.001);
      sensorNoiseParameters.setLinearAccelerationBiasProcessNoiseStandardDeviation(0.001);

      return sensorNoiseParameters;
   }
   

   private static Vector3d computeGazeboBiasVector(double mean, double standardDeviation, Random random)
   {
      Vector3d ret = new Vector3d();
      for (Direction direction : Direction.values())
      {
         MathTools.set(ret, direction, computeGazeboBias(mean, standardDeviation, random));
      }

      return ret;
   }

   // Pull request
   // https://bitbucket.org/osrf/gazebo/pull-request/421/added-noise-to-rates-and-accels-with-test/diff
   //
   //// Sample the bias that we'll use later
   // this->accelBias = math::Rand::GetDblNormal(accelBiasMean,
   // accelBiasStddev);
   //// With equal probability, we pick a negative bias (by convention,
   //// accelBiasMean should be positive, though it would work fine if
   //// negative).
   // if (math::Rand::GetDblUniform() < 0.5)
   // this->accelBias = -this->accelBias;

   private static double computeGazeboBias(double mean, double standardDeviation, Random random)
   {
      double ret = standardDeviation * random.nextGaussian() + mean;
      if (random.nextBoolean())
         ret = -ret;

      return ret;
   }




}
