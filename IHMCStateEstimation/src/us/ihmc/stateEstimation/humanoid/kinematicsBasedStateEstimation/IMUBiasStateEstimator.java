package us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation;

import static us.ihmc.robotics.math.filters.AlphaFilteredYoFrameVector.createAlphaFilteredYoFrameVector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.filters.AlphaFilteredYoFrameQuaternion;
import us.ihmc.robotics.math.filters.AlphaFilteredYoFrameVector;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.tools.FormattingTools;

public class IMUBiasStateEstimator implements IMUBiasProvider
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final List<YoFrameQuaternion> rawOrientationBiases = new ArrayList<>();
   private final List<AlphaFilteredYoFrameQuaternion> orientationBiases = new ArrayList<>();
   private final List<DoubleYoVariable> orientationBiasMagnitudes = new ArrayList<>();
   private final List<AlphaFilteredYoFrameVector> angularVelocityBiases = new ArrayList<>();
   private final List<AlphaFilteredYoFrameVector> linearAccelerationBiases = new ArrayList<>();
   private final List<YoFrameVector> angularVelocityBiasesInWorld = new ArrayList<>();
   private final List<YoFrameVector> linearAccelerationBiasesInWorld = new ArrayList<>();

   private final List<YoFrameVector> linearAccelerationsInWorld = new ArrayList<>();
   private final List<DoubleYoVariable> linearAccelerationMagnitudes = new ArrayList<>();

   private final DoubleYoVariable biasAlphaFilter = new DoubleYoVariable("imuBiasAlphaFilter", registry);

   private final List<DoubleYoVariable> feetToIMUAngularVelocityMagnitudes = new ArrayList<>();
   private final List<DoubleYoVariable> feetToIMULinearVelocityMagnitudes = new ArrayList<>();
   private final List<BooleanYoVariable> isBiasEstimated = new ArrayList<>();

   private final DoubleYoVariable imuBiasEstimationThreshold = new DoubleYoVariable("imuBiasEstimationThreshold", registry);
   private final BooleanYoVariable isIMUOrientationBiasEstimated = new BooleanYoVariable("isIMUOrientationBiasEstimated", registry);

   private final List<? extends IMUSensorReadOnly> imuProcessedOutputs;
   private final Map<IMUSensorReadOnly, Integer> imuToIndexMap = new HashMap<>();
   private final List<RigidBody> feet;

   private final TwistCalculator twistCalculator;

   private final Vector3d gravityVectorInWorld = new Vector3d();
   private final Vector3d zUpVector = new Vector3d();

   private final boolean isAccelerationIncludingGravity;

   public IMUBiasStateEstimator(List<? extends IMUSensorReadOnly> imuProcessedOutputs, Collection<RigidBody> feet, TwistCalculator twistCalculator,
         double gravitationalAcceleration, boolean isAccelerationIncludingGravity, YoVariableRegistry parentRegistry)
   {
      this.imuProcessedOutputs = imuProcessedOutputs;
      this.feet = new ArrayList<>(feet);
      this.twistCalculator = twistCalculator;
      this.isAccelerationIncludingGravity = isAccelerationIncludingGravity;

      biasAlphaFilter.set(0.99995);
      imuBiasEstimationThreshold.set(0.015);

      gravityVectorInWorld.set(0.0, 0.0, -Math.abs(gravitationalAcceleration));
      zUpVector.set(0.0, 0.0, 1.0);

      for (int i = 0; i < imuProcessedOutputs.size(); i++)
      {
         IMUSensorReadOnly imuSensor = imuProcessedOutputs.get(i);
         ReferenceFrame measurementFrame = imuSensor.getMeasurementFrame();
         String sensorName = imuSensor.getSensorName();
         sensorName = sensorName.replaceFirst(imuSensor.getMeasurementLink().getName(), "");
         sensorName = FormattingTools.underscoredToCamelCase(sensorName, true);

         imuToIndexMap.put(imuSensor, i);

         AlphaFilteredYoFrameVector angularVelocityBias = createAlphaFilteredYoFrameVector("estimated" + sensorName + "AngularVelocityBias", "", registry, biasAlphaFilter, measurementFrame);
         angularVelocityBias.update(0.0, 0.0, 0.0);
         angularVelocityBiases.add(angularVelocityBias);

         AlphaFilteredYoFrameVector linearAccelerationBias = createAlphaFilteredYoFrameVector("estimated" + sensorName + "LinearAccelerationBias", "", registry, biasAlphaFilter, measurementFrame);
         linearAccelerationBias.update(0.0, 0.0, 0.0);
         linearAccelerationBiases.add(linearAccelerationBias);

         YoFrameQuaternion rawOrientationBias = new YoFrameQuaternion("estimated" + sensorName + "RawQuaternionBias", measurementFrame, registry);
         rawOrientationBiases.add(rawOrientationBias);

         AlphaFilteredYoFrameQuaternion orientationBias = new AlphaFilteredYoFrameQuaternion("estimated" + sensorName + "QuaternionBias", "", rawOrientationBias, biasAlphaFilter, registry);
         orientationBias.update();
         orientationBiases.add(orientationBias);

         linearAccelerationsInWorld.add(new YoFrameVector("unprocessed" + sensorName + "LinearAccelerationWorld", worldFrame, registry));
         linearAccelerationMagnitudes.add(new DoubleYoVariable("unprocessed" + sensorName + "LinearAccelerationMagnitude", registry));

         orientationBiasMagnitudes.add(new DoubleYoVariable("estimated" + sensorName + "OrientationBiasMagnitude", registry));

         feetToIMUAngularVelocityMagnitudes.add(new DoubleYoVariable("feetTo" + sensorName + "AngularVelocityMagnitude", registry));
         feetToIMULinearVelocityMagnitudes.add(new DoubleYoVariable("feetTo" + sensorName + "LinearVelocityMagnitude", registry));
         isBiasEstimated.add(new BooleanYoVariable("is" + sensorName + "BiasEstimated", registry));

         angularVelocityBiasesInWorld.add(new YoFrameVector("estimated" + sensorName + "AngularVelocityBiasWorld", worldFrame, registry));
         linearAccelerationBiasesInWorld.add(new YoFrameVector("estimated" + sensorName + "LinearAccelerationBias", worldFrame, registry));
      }

      parentRegistry.addChild(registry);
   }

   private final Twist twist = new Twist();

   private final Vector3d measurement = new Vector3d();
   private final Vector3d measurementInWorld = new Vector3d();
   private final Vector3d measurementNormalizedInWorld = new Vector3d();
   private final Vector3d measurementMinusGravity = new Vector3d();
   private final Vector3d measurementMinusGravityInWorld = new Vector3d();
   private final Vector3d measurementBias = new Vector3d();

   private final Vector3d biasRotationAxis = new Vector3d();
   private final AxisAngle4d biasAxisAngle = new AxisAngle4d();
   private final Matrix3d orientationMeasurement = new Matrix3d();
   private final Matrix3d orientationMeasurementTransposed = new Matrix3d();

   public void compute(List<RigidBody> trustedFeet)
   {
      boolean atLeastOneIMUBiasCanBeEstimated = checkIfBiasEstimationPossible(trustedFeet);

      if (atLeastOneIMUBiasCanBeEstimated)
         estimateBiases();
   }

   private boolean checkIfBiasEstimationPossible(List<RigidBody> trustedFeet)
   {
      boolean atLeastOneIMUBiasCanBeEstimated = false;

      if (trustedFeet.size() < feet.size())
      {
         isIMUOrientationBiasEstimated.set(false);
         for (int i = 0; i < isBiasEstimated.size(); i++)
            isBiasEstimated.get(i).set(false);
         return atLeastOneIMUBiasCanBeEstimated;
      }

      for (int imuIndex = 0; imuIndex < imuProcessedOutputs.size(); imuIndex++)
      {
         IMUSensorReadOnly imuSensor = imuProcessedOutputs.get(imuIndex);
         RigidBody measurementLink = imuSensor.getMeasurementLink();

         double feetToIMUAngularVelocityMagnitude = 0.0;
         double feetToIMULinearVelocityMagnitude = 0.0;

         for (int footIndex = 0; footIndex < trustedFeet.size(); footIndex++)
         {
            RigidBody trustedFoot = trustedFeet.get(footIndex);

            twistCalculator.getRelativeTwist(twist, trustedFoot, measurementLink);
            feetToIMUAngularVelocityMagnitude += twist.getAngularPartMagnitude();
            feetToIMULinearVelocityMagnitude += twist.getLinearPartMagnitude();
         }

         feetToIMUAngularVelocityMagnitudes.get(imuIndex).set(feetToIMUAngularVelocityMagnitude);
         feetToIMULinearVelocityMagnitudes.get(imuIndex).set(feetToIMULinearVelocityMagnitude);

         if (feetToIMUAngularVelocityMagnitude < imuBiasEstimationThreshold.getDoubleValue()
               && feetToIMULinearVelocityMagnitude < imuBiasEstimationThreshold.getDoubleValue())
         {
            isBiasEstimated.get(imuIndex).set(true);
            atLeastOneIMUBiasCanBeEstimated = true;
            isIMUOrientationBiasEstimated.set(isAccelerationIncludingGravity);
         }
      }
      return atLeastOneIMUBiasCanBeEstimated;
   }

   private void estimateBiases()
   {
      for (int imuIndex = 0; imuIndex < imuProcessedOutputs.size(); imuIndex++)
      {
         IMUSensorReadOnly imuSensor = imuProcessedOutputs.get(imuIndex);

         if (isBiasEstimated.get(imuIndex).getBooleanValue())
         {
            imuSensor.getOrientationMeasurement(orientationMeasurement);
            orientationMeasurementTransposed.transpose(orientationMeasurement);

            imuSensor.getAngularVelocityMeasurement(measurement);
            angularVelocityBiases.get(imuIndex).update(measurement);
            angularVelocityBiases.get(imuIndex).get(measurementBias);
            orientationMeasurement.transform(measurementBias);
            angularVelocityBiasesInWorld.get(imuIndex).set(measurementBias);

            imuSensor.getLinearAccelerationMeasurement(measurement);
            orientationMeasurement.transform(measurement, measurementInWorld);
            linearAccelerationsInWorld.get(imuIndex).set(measurementInWorld);

            if (isAccelerationIncludingGravity)
            {
               measurementNormalizedInWorld.normalize(measurementInWorld);

               biasRotationAxis.cross(zUpVector, measurementNormalizedInWorld);
               double biasMagnitude = zUpVector.angle(measurementNormalizedInWorld);

               if (Math.abs(biasMagnitude) < 1.0e-7)
               {
                  rawOrientationBiases.get(imuIndex).setToZero();
               }
               else
               {
                  biasRotationAxis.scale(biasMagnitude);
                  biasMagnitude = biasRotationAxis.length();
                  biasRotationAxis.scale(1.0 / biasMagnitude);
                  orientationMeasurementTransposed.transform(biasRotationAxis);
                  biasAxisAngle.set(biasRotationAxis, biasMagnitude);
                  rawOrientationBiases.get(imuIndex).set(biasAxisAngle);
               }

               AlphaFilteredYoFrameQuaternion yoOrientationBias = orientationBiases.get(imuIndex);
               yoOrientationBias.update();
               yoOrientationBias.get(biasAxisAngle);
               orientationBiasMagnitudes.get(imuIndex).set(Math.abs(biasAxisAngle.getAngle()));

               measurementMinusGravityInWorld.add(measurementInWorld, gravityVectorInWorld);
               orientationMeasurementTransposed.transform(measurementMinusGravityInWorld, measurementMinusGravity);
               linearAccelerationBiases.get(imuIndex).update(measurementMinusGravity);
            }
            else
            {
               linearAccelerationBiases.get(imuIndex).update(measurement);
            }

            linearAccelerationBiases.get(imuIndex).get(measurementBias);
            orientationMeasurement.transform(measurementBias);
            linearAccelerationBiasesInWorld.get(imuIndex).set(measurementBias);
         }
      }
   }

   @Override
   public void getAngularVelocityBiasInIMUFrame(IMUSensorReadOnly imu, Vector3d angularVelocityBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         angularVelocityBiasToPack.set(0.0, 0.0, 0.0);
      else
         angularVelocityBiases.get(imuIndex.intValue()).get(angularVelocityBiasToPack);
   }

   @Override
   public void getAngularVelocityBiasInIMUFrame(IMUSensorReadOnly imu, FrameVector angularVelocityBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         angularVelocityBiasToPack.set(0.0, 0.0, 0.0);
      else
         angularVelocityBiases.get(imuIndex.intValue()).getFrameTupleIncludingFrame(angularVelocityBiasToPack);
   }

   @Override
   public void getAngularVelocityBiasInWorldFrame(IMUSensorReadOnly imu, Vector3d angularVelocityBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         angularVelocityBiasToPack.set(0.0, 0.0, 0.0);
      else
         angularVelocityBiasesInWorld.get(imuIndex.intValue()).get(angularVelocityBiasToPack);
   }

   @Override
   public void getAngularVelocityBiasInWorldFrame(IMUSensorReadOnly imu, FrameVector angularVelocityBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         angularVelocityBiasToPack.set(0.0, 0.0, 0.0);
      else
         angularVelocityBiasesInWorld.get(imuIndex.intValue()).getFrameTupleIncludingFrame(angularVelocityBiasToPack);
   }

   @Override
   public void getLinearAccelerationBiasInIMUFrame(IMUSensorReadOnly imu, Vector3d linearAccelerationBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         linearAccelerationBiasToPack.set(0.0, 0.0, 0.0);
      else
         linearAccelerationBiases.get(imuIndex.intValue()).get(linearAccelerationBiasToPack);
   }

   @Override
   public void getLinearAccelerationBiasInIMUFrame(IMUSensorReadOnly imu, FrameVector linearAccelerationBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         linearAccelerationBiasToPack.set(0.0, 0.0, 0.0);
      else
         linearAccelerationBiases.get(imuIndex.intValue()).getFrameTupleIncludingFrame(linearAccelerationBiasToPack);
   }

   @Override
   public void getLinearAccelerationBiasInWorldFrame(IMUSensorReadOnly imu, Vector3d linearAccelerationBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         linearAccelerationBiasToPack.set(0.0, 0.0, 0.0);
      else
         linearAccelerationBiasesInWorld.get(imuIndex.intValue()).get(linearAccelerationBiasToPack);
   }

   @Override
   public void getLinearAccelerationBiasInWorldFrame(IMUSensorReadOnly imu, FrameVector linearAccelerationBiasToPack)
   {
      Integer imuIndex = imuToIndexMap.get(imu);
      if (imuIndex == null)
         linearAccelerationBiasToPack.set(0.0, 0.0, 0.0);
      else
         linearAccelerationBiasesInWorld.get(imuIndex.intValue()).getFrameTupleIncludingFrame(linearAccelerationBiasToPack);
   }
}
