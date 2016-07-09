package us.ihmc.sensorProcessing.stateEstimation;

import org.apache.commons.lang3.tuple.ImmutablePair;
import us.ihmc.robotics.robotSide.SideDependentList;

public interface StateEstimatorParameters extends SensorProcessingConfiguration
{
   public abstract boolean isRunningOnRealRobot();
   
   @Override
   public abstract double getEstimatorDT();
   
   public abstract boolean trustCoPAsNonSlippingContactPoint();

   public abstract boolean useControllerDesiredCenterOfPressure();

   public abstract boolean useIMUsForSpineJointVelocityEstimation();

   /** @deprecated Need to switch to frequency */
   @Deprecated
   public abstract double getAlphaIMUsForSpineJointVelocityEstimation();

   public abstract ImmutablePair<String, String> getIMUsForSpineJointVelocityEstimation();

   public abstract boolean requestWristForceSensorCalibrationAtStart();

   public abstract SideDependentList<String> getWristForceSensorNames();

   public abstract boolean requestFootForceSensorCalibrationAtStart();

   public abstract SideDependentList<String> getFootForceSensorNames();

   // Parameters related to the kinematics based state estimator
   public abstract double getKinematicsPelvisPositionFilterFreqInHertz();
   public abstract double getKinematicsPelvisLinearVelocityFilterFreqInHertz();

   public abstract double getCoPFilterFreqInHertz();
   
   public abstract boolean useAccelerometerForEstimation();
   
   public abstract boolean enableIMUBiasCompensation();
   public abstract double getIMUBiasFilterFreqInHertz();

   public abstract boolean cancelGravityFromAccelerationMeasurement();

   public abstract double getPelvisPositionFusingFrequency();
   public abstract double getPelvisLinearVelocityFusingFrequency();
   public abstract double getPelvisVelocityBacklashSlopTime();
   
   public abstract double getDelayTimeForTrustingFoot();
   
   public abstract double getForceInPercentOfWeightThresholdToTrustFoot();
   
   public abstract boolean estimateIMUDrift();

   public abstract boolean compensateIMUDrift();
   
   public abstract double getIMUDriftFilterFreqInHertz();
   
   public abstract double getFootVelocityUsedForImuDriftFilterFreqInHertz();
   
   public abstract double getFootVelocityThresholdToEnableIMUDriftCompensation();

   public abstract boolean useTwistForPelvisLinearStateEstimation();

   public abstract double getPelvisLinearVelocityAlphaNewTwist();
   
   public abstract boolean createFusedIMUSensor();

   public abstract double getContactThresholdForce();

   public abstract double getFootSwitchCoPThresholdFraction();

   public abstract double getContactThresholdHeight();

   public abstract FootSwitchType getFootSwitchType();

   public abstract boolean getPelvisLinearStateUpdaterTrustImuWhenNoFeetAreInContact();
}
