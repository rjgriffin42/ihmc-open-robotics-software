package us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation;

import javax.vecmath.Vector3d;

import us.ihmc.communication.packets.IMUPacket;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.sensorProcessing.sensorProcessors.SensorOutputMapReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;
import us.ihmc.tools.io.printing.PrintTools;

/**
 * JointStateUpdater simply reads the joint position/velocity sensors and updates the FullInverseDynamicsStructure.
 * (Based on {@link us.ihmc.sensorProcessing.stateEstimation.JointStateFullRobotModelUpdater}.)
 * @author Sylvain
 *
 */
public class JointStateUpdater
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final TwistCalculator twistCalculator;
   private final SpatialAccelerationCalculator spatialAccelerationCalculator;
   private final RigidBody rootBody;

   private OneDoFJoint[] oneDoFJoints;
   private final SensorOutputMapReadOnly sensorMap;
   private final IMUBasedJointVelocityEstimator iMUBasedJointVelocityEstimator;

   private HeadIMUSubscriber headIMUSubscriber = null;
   private final IMUPacket imuPacket = new IMUPacket();
   private final BooleanYoVariable recievedHeadIMUPacket = new BooleanYoVariable("RecievedHeadIMUPacket", registry);
   private final DoubleYoVariable headIMUDelay = new DoubleYoVariable("HeadIMUDelay", registry);
   private final YoFrameVector headIMULinearAcceleration = new YoFrameVector("HeadIMULinearAcceleration", ReferenceFrame.getWorldFrame(), registry);

   private final BooleanYoVariable enableIMUBasedJointVelocityEstimator = new BooleanYoVariable("enableIMUBasedJointVelocityEstimator", registry);

   public JointStateUpdater(FullInverseDynamicsStructure inverseDynamicsStructure, SensorOutputMapReadOnly sensorOutputMapReadOnly,
         StateEstimatorParameters stateEstimatorParameters, YoVariableRegistry parentRegistry)
   {
      twistCalculator = inverseDynamicsStructure.getTwistCalculator();
      spatialAccelerationCalculator = inverseDynamicsStructure.getSpatialAccelerationCalculator();
      rootBody = twistCalculator.getRootBody();

      this.sensorMap = sensorOutputMapReadOnly;

      InverseDynamicsJoint[] joints = ScrewTools.computeSupportAndSubtreeJoints(inverseDynamicsStructure.getRootJoint().getSuccessor());
      this.oneDoFJoints = ScrewTools.filterJoints(joints, OneDoFJoint.class);

      iMUBasedJointVelocityEstimator = createIMUBasedJointVelocityEstimator(sensorOutputMapReadOnly, stateEstimatorParameters, registry);

      parentRegistry.addChild(registry);
   }

   public void setJointsToUpdate(OneDoFJoint[] oneDoFJoints)
   {
      this.oneDoFJoints = oneDoFJoints;
   }

   public IMUBasedJointVelocityEstimator createIMUBasedJointVelocityEstimator(SensorOutputMapReadOnly sensorOutputMapReadOnly,
         StateEstimatorParameters stateEstimatorParameters, YoVariableRegistry parentRegistry)
   {
      if (stateEstimatorParameters == null || stateEstimatorParameters.getIMUsForSpineJointVelocityEstimation() == null)
         return null;

      enableIMUBasedJointVelocityEstimator.set(stateEstimatorParameters.useIMUsForSpineJointVelocityEstimation());

      IMUSensorReadOnly pelvisIMU = null;
      IMUSensorReadOnly chestIMU = null;

      String pelvisIMUName = stateEstimatorParameters.getIMUsForSpineJointVelocityEstimation().getLeft();
      String chestIMUName = stateEstimatorParameters.getIMUsForSpineJointVelocityEstimation().getRight();

      for (int i = 0; i < sensorOutputMapReadOnly.getIMUProcessedOutputs().size(); i++)
      {
         IMUSensorReadOnly sensorReadOnly = sensorOutputMapReadOnly.getIMUProcessedOutputs().get(i);
         if (sensorReadOnly.getSensorName().equals(pelvisIMUName))
            pelvisIMU = sensorReadOnly;

         if (sensorReadOnly.getSensorName().equals(chestIMUName))
            chestIMU = sensorReadOnly;
      }

      // TODO create the module with the two IMUs to compute and smoothen the spine joint velocities here.
      if (pelvisIMU != null && chestIMU != null)
      {
         double estimatorDT = stateEstimatorParameters.getEstimatorDT();
         double slopTime = stateEstimatorParameters.getPelvisVelocityBacklashSlopTime();
         IMUBasedJointVelocityEstimator iMUBasedJointVelocityEstimator = new IMUBasedJointVelocityEstimator(pelvisIMU, chestIMU, sensorOutputMapReadOnly, estimatorDT, slopTime, parentRegistry);
         iMUBasedJointVelocityEstimator.compute();
         iMUBasedJointVelocityEstimator.setAlphaFuse(stateEstimatorParameters.getAlphaIMUsForSpineJointVelocityEstimation());
         return iMUBasedJointVelocityEstimator;
      }
      else
      {
         PrintTools.warn("Could not find the given pelvis and/or chest IMUs: pelvisIMU = " + pelvisIMUName + ", chestIMU = " + chestIMUName);
         if(pelvisIMU == null)
         {
            PrintTools.warn("Pelvis IMU is null.");
         }

         if(chestIMU == null)
         {
            PrintTools.warn("Pelvis IMU is null.");
         }

         return null;
      }
   }

   public void initialize()
   {
      updateJointState();
   }

   public void updateJointState()
   {
      if (iMUBasedJointVelocityEstimator != null)
      {
         iMUBasedJointVelocityEstimator.compute();
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

         if (enableIMUBasedJointVelocityEstimator.getBooleanValue() && iMUBasedJointVelocityEstimator != null)
         {
            double estimatedJointVelocity = iMUBasedJointVelocityEstimator.getEstimatedJointVelocitiy(oneDoFJoint);
            if (!Double.isNaN(estimatedJointVelocity))
               velocitySensorData = estimatedJointVelocity;
         }

         oneDoFJoint.setQd(velocitySensorData);
         oneDoFJoint.setTauMeasured(torqueSensorData);
      }

      rootBody.updateFramesRecursively();
      twistCalculator.compute();
      spatialAccelerationCalculator.compute();
   }

   private final FrameVector headIMULinearAccelerationTemp = new FrameVector();
   private final Vector3d linearAcceleration = new Vector3d();
   public void checkForIMUPacket(double time)
   {
      if (headIMUSubscriber == null)
      {
         recievedHeadIMUPacket.set(false);
         return;
      }
      if (!headIMUSubscriber.getPacket(imuPacket))
      {
         recievedHeadIMUPacket.set(false);
         return;
      }
      recievedHeadIMUPacket.set(true);

      headIMUDelay.set(time - imuPacket.time);
      linearAcceleration.set(imuPacket.linearAcceleration);
      headIMULinearAccelerationTemp.setIncludingFrame(headIMUSubscriber.getImuDefinition().getIMUFrame(), linearAcceleration);
      headIMULinearAccelerationTemp.changeFrame(ReferenceFrame.getWorldFrame());
      headIMULinearAcceleration.set(headIMULinearAccelerationTemp);
   }

   public void addHeadIMUSubscriber(HeadIMUSubscriber headIMUSubscriber)
   {
      this.headIMUSubscriber = headIMUSubscriber;
   }
}
