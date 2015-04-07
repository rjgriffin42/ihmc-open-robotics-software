package us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation;

import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.utilities.screwTheory.InverseDynamicsJoint;
import us.ihmc.utilities.screwTheory.OneDoFJoint;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.ScrewTools;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.utilities.screwTheory.TwistCalculator;
import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;

/**
 * JointStateUpdater simply reads the joint position/velocity sensors and updates the FullInverseDynamicsStructure.
 * (Based on {@link us.ihmc.sensorProcessing.stateEstimation.JointStateFullRobotModelUpdater}.)
 * @author Sylvain
 *
 */
public class JointStateUpdater
{
   private final TwistCalculator twistCalculator;
   private final SpatialAccelerationCalculator spatialAccelerationCalculator;
   private final RigidBody rootBody;

   private final OneDoFJoint[] oneDoFJoints;
   private final SensorOutputMapReadOnly sensorMap;
   private IMUBasedPelvisToTorsoEncodersVelocityFilter iMUBasedPelvisToTorsoEncodersVelocityFilter;

   private BooleanYoVariable enableIMUBasedPelvisToTorsoEncodersVelocityFilter;
   private double qd_filtered;

   public JointStateUpdater(FullInverseDynamicsStructure inverseDynamicsStructure, SensorOutputMapReadOnly sensorOutputMapReadOnly,
         StateEstimatorParameters stateEstimatorParameters, YoVariableRegistry parentRegistry)
   {
      twistCalculator = inverseDynamicsStructure.getTwistCalculator();
      spatialAccelerationCalculator = inverseDynamicsStructure.getSpatialAccelerationCalculator();
      rootBody = twistCalculator.getRootBody();

      this.sensorMap = sensorOutputMapReadOnly;

      InverseDynamicsJoint[] joints = ScrewTools.computeSupportAndSubtreeJoints(inverseDynamicsStructure.getRootJoint().getSuccessor());
      this.oneDoFJoints = ScrewTools.filterJoints(joints, OneDoFJoint.class);

      enableIMUBasedPelvisToTorsoEncodersVelocityFilter = new BooleanYoVariable("enable_IMUBasedVelFilter", parentRegistry);
      setupSpineJointVelocitiesSmoothener(sensorOutputMapReadOnly, stateEstimatorParameters, parentRegistry);
   }

   public void setupSpineJointVelocitiesSmoothener(SensorOutputMapReadOnly sensorOutputMapReadOnly, StateEstimatorParameters stateEstimatorParameters,
         YoVariableRegistry parentRegistry)
   {
      if (stateEstimatorParameters == null || stateEstimatorParameters.getIMUsForSpineJointVelocityEstimation() == null)
         return;

      enableIMUBasedPelvisToTorsoEncodersVelocityFilter.set(stateEstimatorParameters.useIMUsForSpineJointVelocityEstimation());

      IMUSensorReadOnly pelvisIMU = null;
      IMUSensorReadOnly chestIMU = null;

      for (int i = 0; i < sensorOutputMapReadOnly.getIMUProcessedOutputs().size(); i++)
      {
         IMUSensorReadOnly sensorReadOnly = sensorOutputMapReadOnly.getIMUProcessedOutputs().get(i);
         if (sensorReadOnly.getSensorName().equals(stateEstimatorParameters.getIMUsForSpineJointVelocityEstimation().first()))
            pelvisIMU = sensorReadOnly;

         if (sensorReadOnly.getSensorName().equals(stateEstimatorParameters.getIMUsForSpineJointVelocityEstimation().second()))
            chestIMU = sensorReadOnly;
      }

      // TODO create the module with the two IMUs to compute and smoothen the spine joint velocities here.
      if (pelvisIMU != null && chestIMU != null)
      {
         iMUBasedPelvisToTorsoEncodersVelocityFilter = new IMUBasedPelvisToTorsoEncodersVelocityFilter(parentRegistry, pelvisIMU, chestIMU,
               sensorOutputMapReadOnly);
         iMUBasedPelvisToTorsoEncodersVelocityFilter.compute();
         iMUBasedPelvisToTorsoEncodersVelocityFilter.setAlphaFuse(stateEstimatorParameters.getAlphaIMUsForSpineJointVelocityEstimation());
      }
      else
      {
         throw new RuntimeException("Could not find the given pelvis and/or chest IMUs: pelvisIMU = " + pelvisIMU + ", chestIMU = " + chestIMU);
      }
   }

   public void initialize()
   {
      updateJointState();
   }

   public void updateJointState()
   {

      if (iMUBasedPelvisToTorsoEncodersVelocityFilter != null)
      {
         iMUBasedPelvisToTorsoEncodersVelocityFilter.compute();
      }

      for (int i = 0; i < oneDoFJoints.length; i++)
      {
         OneDoFJoint oneDoFJoint = oneDoFJoints[i];

         double positionSensorData = sensorMap.getJointPositionProcessedOutput(oneDoFJoint);
         double velocitySensorData = sensorMap.getJointVelocityProcessedOutput(oneDoFJoint);
         double torqueSensorData = sensorMap.getJointTauProcessedOutput(oneDoFJoint);
         boolean jointEnabledIndicator = sensorMap.isJointEnabled(oneDoFJoint);
         
         oneDoFJoint.setQ(positionSensorData);
         oneDoFJoint.setEnabled(jointEnabledIndicator);

         if (enableIMUBasedPelvisToTorsoEncodersVelocityFilter.getBooleanValue()
               && iMUBasedPelvisToTorsoEncodersVelocityFilter.getJointVelocities().get(oneDoFJoint) != null)
         {
            qd_filtered = iMUBasedPelvisToTorsoEncodersVelocityFilter.getJointVelocities().get(oneDoFJoint).getDoubleValue();
            oneDoFJoint.setQd(qd_filtered);
         }
         else
         {
            oneDoFJoint.setQd(velocitySensorData);
         }
         
         oneDoFJoint.setTauMeasured(torqueSensorData);
      }

      rootBody.updateFramesRecursively();
      twistCalculator.compute();
      spatialAccelerationCalculator.compute();

   }
}
