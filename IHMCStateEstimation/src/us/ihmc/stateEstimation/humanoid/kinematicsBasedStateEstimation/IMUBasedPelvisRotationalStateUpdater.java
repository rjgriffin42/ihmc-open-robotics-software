package us.ihmc.stateEstimation.humanoid.kinematicsBasedStateEstimation;

import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;

import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.geometry.AngleTools;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.geometry.RotationTools;
import us.ihmc.robotics.math.filters.FiniteDifferenceAngularVelocityYoFrameVector;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFrameQuaternion;
import us.ihmc.robotics.math.frames.YoFrameVector;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.SixDoFJoint;
import us.ihmc.robotics.screwTheory.Twist;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.sensorProcessing.stateEstimation.IMUSelectorAndDataConverter;
import us.ihmc.sensorProcessing.stateEstimation.IMUSensorReadOnly;
import us.ihmc.sensorProcessing.stateEstimation.OrientationStateRobotModelUpdater;
import us.ihmc.sensorProcessing.stateEstimation.evaluation.FullInverseDynamicsStructure;

/**
 * PelvisRotationalStateUpdater reads and transforms the orientation and angular velocity obtained from the IMU to update the pelvis orientation and angular velocity in world. 
 * (Based on {@link IMUSelectorAndDataConverter} and {@link OrientationStateRobotModelUpdater})
 * @author Sylvain
 *
 */
public class IMUBasedPelvisRotationalStateUpdater implements PelvisRotationalStateUpdaterInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final YoFrameOrientation yoRootJointFrameOrientation;
   private final YoFrameQuaternion yoRootJointFrameQuaternion;
   private final DoubleYoVariable rootJointYawOffsetFromFrozenState;

   private final YoFrameVector yoRootJointAngularVelocityMeasFrame;
   private final YoFrameVector yoRootJointAngularVelocity;
   private final YoFrameVector yoRootJointAngularVelocityInWorld;

   private final FiniteDifferenceAngularVelocityYoFrameVector yoRootJointAngularVelocityFromFD;

   private final SixDoFJoint rootJoint;
   private final ReferenceFrame rootJointFrame;
   private final TwistCalculator twistCalculator;

   private final IMUSensorReadOnly imuProcessedOutput;
   private final IMUBiasProvider imuBiasProvider;

   private final ReferenceFrame measurementFrame;
   private final RigidBody measurementLink;

   public IMUBasedPelvisRotationalStateUpdater(FullInverseDynamicsStructure inverseDynamicsStructure, List<? extends IMUSensorReadOnly> imuProcessedOutputs, IMUBiasProvider imuBiasProvider, double dt,
         YoVariableRegistry parentRegistry)
   {
      this.imuBiasProvider = imuBiasProvider;
      checkNumberOfSensors(imuProcessedOutputs);

      imuProcessedOutput = imuProcessedOutputs.get(0);

      rootJoint = inverseDynamicsStructure.getRootJoint();
      rootJointFrame = rootJoint.getFrameAfterJoint();
      twistCalculator = inverseDynamicsStructure.getTwistCalculator();

      measurementFrame = imuProcessedOutput.getMeasurementFrame();
      measurementLink = imuProcessedOutput.getMeasurementLink();

      yoRootJointFrameOrientation = new YoFrameOrientation("estimatedRootJoint", worldFrame, registry);
      yoRootJointFrameQuaternion = new YoFrameQuaternion("estimatedRootJoint", worldFrame, registry);

      rootJointYawOffsetFromFrozenState = new DoubleYoVariable("rootJointYawOffsetFromFrozenState", registry);

      yoRootJointAngularVelocity = new YoFrameVector("estimatedRootJointAngularVelocity", rootJointFrame, registry);
      yoRootJointAngularVelocityInWorld = new YoFrameVector("estimatedRootJointAngularVelocityWorld", worldFrame, registry);
      yoRootJointAngularVelocityMeasFrame = new YoFrameVector("estimatedRootJointAngularVelocityMeasFrame", measurementFrame, registry);

      yoRootJointAngularVelocityFromFD = new FiniteDifferenceAngularVelocityYoFrameVector("estimatedRootJointAngularVelocityFromFD", yoRootJointFrameQuaternion, dt, registry);

      parentRegistry.addChild(registry);

      angularVelocityRootJointFrameRelativeToWorld = new FrameVector(rootJointFrame);
   }

   public IMUSensorReadOnly getIMUUsedForEstimation()
   {
      return imuProcessedOutput;
   }

   private void checkNumberOfSensors(List<? extends IMUSensorReadOnly> imuProcessedOutputs)
   {
      if (imuProcessedOutputs.size() > 1)
         System.out.println(getClass().getSimpleName() + ": More than 1 IMU sensor, using only the first one: " + imuProcessedOutputs.get(0).getSensorName());

      if (imuProcessedOutputs.size() == 0)
         throw new RuntimeException("No sensor set up for the IMU.");
   }

   @Override
   public void initialize()
   {
      rotationFrozenOffset.setIdentity();
      updateRootJointOrientationAndAngularVelocity();
   }

   @Override
   public void initializeForFrozenState()
   {
      rotationFrozenOffset.setIdentity();

      // R_{measurementFrame}^{world}
      imuProcessedOutput.getOrientationMeasurement(orientationMeasurement);
      transformFromMeasurementFrameToWorld.setRotationAndZeroTranslation(orientationMeasurement);

      // R_{root}^{measurementFrame}
      rootJointFrame.getTransformToDesiredFrame(transformFromRootJointFrameToMeasurementFrame, measurementFrame);

      // R_{root}^{world} = R_{estimationLink}^{world} * R_{root}^{estimationLink}
      transformFromRootJointFrameToWorld.multiply(transformFromMeasurementFrameToWorld, transformFromRootJointFrameToMeasurementFrame);
      transformFromRootJointFrameToWorld.getRotation(rotationFromRootJointFrameToWorld);
      
      double initialYaw = RotationTools.computeYaw(rotationFromRootJointFrameToWorld);

      rootJointYawOffsetFromFrozenState.set(initialYaw);
      rotationFrozenOffset.rotZ(initialYaw);

      yoRootJointFrameQuaternion.setToZero();
      yoRootJointFrameOrientation.setToZero();

      yoRootJointFrameQuaternion.get(rotationFromRootJointFrameToWorld);
      rootJoint.setRotation(rotationFromRootJointFrameToWorld);

      // Set the rootJoint twist to zero.
      rootJoint.getJointTwist(twistRootBodyRelativeToWorld);
      twistRootBodyRelativeToWorld.setToZero();
      rootJoint.setJointTwist(twistRootBodyRelativeToWorld);
   }

   private final Matrix3d rotationFrozenOffset = new Matrix3d();
   private final double[] lastComputedYawPitchRoll = new double[3];

   @Override
   public void updateForFrozenState()
   {
      // R_{measurementFrame}^{world}
      imuProcessedOutput.getOrientationMeasurement(orientationMeasurement);
      transformFromMeasurementFrameToWorld.setRotationAndZeroTranslation(orientationMeasurement);

      // R_{root}^{measurementFrame}
      rootJointFrame.getTransformToDesiredFrame(transformFromRootJointFrameToMeasurementFrame, measurementFrame);

      // R_{root}^{world} = R_{estimationLink}^{world} * R_{root}^{estimationLink}
      transformFromRootJointFrameToWorld.multiply(transformFromMeasurementFrameToWorld, transformFromRootJointFrameToMeasurementFrame);
      transformFromRootJointFrameToWorld.getRotation(rotationFromRootJointFrameToWorld);

      yoRootJointFrameQuaternion.getYawPitchRoll(lastComputedYawPitchRoll);
      double currentYaw = RotationTools.computeYaw(rotationFromRootJointFrameToWorld);

      double yawDifference = AngleTools.computeAngleDifferenceMinusPiToPi(lastComputedYawPitchRoll[0], currentYaw);
      rootJointYawOffsetFromFrozenState.set(yawDifference);
      rotationFrozenOffset.rotZ(yawDifference);

      // Keep setting the orientation so that the localization updater works properly.
      yoRootJointFrameQuaternion.get(rotationFromRootJointFrameToWorld);
      rootJoint.setRotation(rotationFromRootJointFrameToWorld);

      // Set the rootJoint twist to zero.
      rootJoint.getJointTwist(twistRootBodyRelativeToWorld);
      twistRootBodyRelativeToWorld.setToZero();
      rootJoint.setJointTwist(twistRootBodyRelativeToWorld);

      updateViz();
   }

   @Override
   public void updateRootJointOrientationAndAngularVelocity()
   {
      updateRootJointRotation();
      updateRootJointTwistAngularPart();
      updateViz();
   }

   private final RigidBodyTransform transformFromMeasurementFrameToWorld = new RigidBodyTransform();

   private final RigidBodyTransform transformFromRootJointFrameToWorld = new RigidBodyTransform();
   private final RigidBodyTransform transformFromRootJointFrameToMeasurementFrame = new RigidBodyTransform();

   private final Matrix3d rotationFromRootJointFrameToWorld = new Matrix3d();
   private final Matrix3d orientationMeasurement = new Matrix3d();

   private final Matrix3d yawBiasMatrix = new Matrix3d();

   private void updateRootJointRotation()
   {
      // R_{measurementFrame}^{world}
      imuProcessedOutput.getOrientationMeasurement(orientationMeasurement);
      transformFromMeasurementFrameToWorld.setRotationAndZeroTranslation(orientationMeasurement);

      // R_{root}^{measurementFrame}
      rootJointFrame.getTransformToDesiredFrame(transformFromRootJointFrameToMeasurementFrame, measurementFrame);

      // R_{root}^{world} = R_{estimationLink}^{world} * R_{root}^{estimationLink}
      transformFromRootJointFrameToWorld.multiply(transformFromMeasurementFrameToWorld, transformFromRootJointFrameToMeasurementFrame);
      transformFromRootJointFrameToWorld.getRotation(rotationFromRootJointFrameToWorld);

      rotationFromRootJointFrameToWorld.mul(rotationFrozenOffset, rotationFromRootJointFrameToWorld);

      if (imuBiasProvider != null)
      {
         yawBiasMatrix.rotZ(imuBiasProvider.getYawBiasInWorldFrame(imuProcessedOutput));
         yawBiasMatrix.transpose();
         rotationFromRootJointFrameToWorld.mul(yawBiasMatrix, rotationFromRootJointFrameToWorld);
      }

      rootJoint.setRotation(rotationFromRootJointFrameToWorld);
      rootJointFrame.update();
   }

   private final Vector3d angularVelocityMeasurement = new Vector3d();
   private final Vector3d angularVelocityMeasurementBias = new Vector3d();

   /** Angular velocity of the measurement link, with respect to world. */
   private final FrameVector angularVelocityMeasurementLinkRelativeToWorld = new FrameVector();

   /** Angular velocity of the estimation link, with respect to the measurement link. */
   private final FrameVector angularVelocityRootJointFrameRelativeToMeasurementLink = new FrameVector();

   /** Angular velocity of the root body, with respect to world. */
   private final FrameVector angularVelocityRootJointFrameRelativeToWorld;

   /** Twist of the estimation link, with respect to the measurement link. */
   private final Twist twistRootJointFrameRelativeToMeasurementLink = new Twist();
   /** Twist of the root body, with respect to world. */
   private final Twist twistRootBodyRelativeToWorld = new Twist();

   private void updateRootJointTwistAngularPart()
   {
      // T_{rootBody}^{rootBody, measurementLink}
      twistCalculator.getRelativeTwist(twistRootJointFrameRelativeToMeasurementLink, measurementLink, rootJoint.getSuccessor());
      // T_{rootBody}^{rootJointFrame, measurementLink}
      twistRootJointFrameRelativeToMeasurementLink.changeFrame(rootJointFrame);
      // T_{rootJointFrame}^{rootJointFrame, measurementLink}
      twistRootJointFrameRelativeToMeasurementLink.changeBodyFrameNoRelativeTwist(rootJointFrame);

      // omega_{rootJointFrame}^{rootJointFrame, measurementLink}
      twistRootJointFrameRelativeToMeasurementLink.getAngularPart(angularVelocityRootJointFrameRelativeToMeasurementLink);

      // omega_{measurementLink}^{measurementFrame, world}
      imuProcessedOutput.getAngularVelocityMeasurement(angularVelocityMeasurement);
      if (imuBiasProvider != null)
      {
         imuBiasProvider.getAngularVelocityBiasInIMUFrame(imuProcessedOutput, angularVelocityMeasurementBias);
         angularVelocityMeasurement.sub(angularVelocityMeasurementBias);
      }
      angularVelocityMeasurementLinkRelativeToWorld.setIncludingFrame(measurementFrame, angularVelocityMeasurement);

      // omega_{measurementLink}^{rootJointFrame, world}
      angularVelocityMeasurementLinkRelativeToWorld.changeFrame(rootJointFrame);

      // omega_{rootJointFrame}^{rootJointFrame, world} = omega_{rootJointFrame}^{rootJointFrame, measurementLink} + omega_{measurementLink}^{rootJointFrame, world}
      angularVelocityRootJointFrameRelativeToWorld.add(angularVelocityRootJointFrameRelativeToMeasurementLink, angularVelocityMeasurementLinkRelativeToWorld);

      rootJoint.getJointTwist(twistRootBodyRelativeToWorld);
      twistRootBodyRelativeToWorld.setAngularPart(angularVelocityRootJointFrameRelativeToWorld);
      rootJoint.setJointTwist(twistRootBodyRelativeToWorld);
      twistCalculator.compute();

      yoRootJointAngularVelocity.setAndMatchFrame(angularVelocityMeasurementLinkRelativeToWorld);
      yoRootJointAngularVelocityMeasFrame.setAndMatchFrame(angularVelocityMeasurementLinkRelativeToWorld);
      yoRootJointAngularVelocityInWorld.setAndMatchFrame(angularVelocityRootJointFrameRelativeToWorld);
   }

   private void updateViz()
   {
      yoRootJointFrameQuaternion.checkReferenceFrameMatch(worldFrame);
      yoRootJointFrameQuaternion.set(rotationFromRootJointFrameToWorld);
      yoRootJointAngularVelocityFromFD.update();

      yoRootJointFrameOrientation.checkReferenceFrameMatch(worldFrame);
      yoRootJointFrameOrientation.set(rotationFromRootJointFrameToWorld);
   }

   @Override
   public void getEstimatedOrientation(FrameOrientation estimatedOrientation)
   {
      estimatedOrientation.set(rotationFromRootJointFrameToWorld);
   }

   @Override
   public void getEstimatedAngularVelocity(FrameVector estimatedAngularVelocityToPack)
   {
      estimatedAngularVelocityToPack.setIncludingFrame(angularVelocityRootJointFrameRelativeToWorld);
   }
}
