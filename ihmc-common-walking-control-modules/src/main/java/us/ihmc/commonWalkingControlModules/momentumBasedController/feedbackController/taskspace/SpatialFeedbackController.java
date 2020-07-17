package us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.taskspace;

import static us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox.appendIndex;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData3D.POSITION;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData3D.ROTATION_VECTOR;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData6D.ACCELERATION;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData6D.FORCE;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData6D.VELOCITY;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.ACHIEVED;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.CURRENT;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.DESIRED;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.ERROR;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.ERROR_CUMULATED;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.ERROR_INTEGRATED;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.FEEDBACK;
import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.FEEDFORWARD;

import us.ihmc.commonWalkingControlModules.controlModules.YoSE3OffsetFrame;
import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerException;
import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.MomentumRateCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.SpatialVelocityCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualWrenchCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.data.AlphaFilteredVectorData6D;
import us.ihmc.commonWalkingControlModules.controllerCore.data.PoseData3D;
import us.ihmc.commonWalkingControlModules.controllerCore.data.QuaternionData3D;
import us.ihmc.commonWalkingControlModules.controllerCore.data.RateLimitedVectorData6D;
import us.ihmc.commonWalkingControlModules.controllerCore.data.VectorData3D;
import us.ihmc.commonWalkingControlModules.controllerCore.data.VectorData6D;
import us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.FeedbackControllerInterface;
import us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.FeedbackControllerSettings;
import us.ihmc.euclid.matrix.Matrix3D;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FramePose3D;
import us.ihmc.euclid.referenceFrame.FrameQuaternion;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.algorithms.interfaces.RigidBodyAccelerationProvider;
import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
import us.ihmc.mecano.spatial.SpatialAcceleration;
import us.ihmc.mecano.spatial.Twist;
import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
import us.ihmc.robotics.controllers.pidGains.YoPIDSE3Gains;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;

public class SpatialFeedbackController implements FeedbackControllerInterface
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final YoVariableRegistry registry;

   private final YoBoolean isEnabled;

   private final PoseData3D yoDesiredPose;
   private final PoseData3D yoCurrentPose;

   private final VectorData6D yoErrorVector;
   private final QuaternionData3D yoErrorOrientation;

   private final VectorData3D yoErrorPositionIntegrated;
   private final QuaternionData3D yoErrorOrientationCumulated;
   private final VectorData3D yoErrorRotationVectorIntegrated;

   private final VectorData6D yoDesiredVelocity;
   private final VectorData6D yoCurrentVelocity;
   private final VectorData6D yoErrorVelocity;
   private final AlphaFilteredVectorData6D yoFilteredErrorVelocity;
   private final VectorData6D yoFeedForwardVelocity;
   private final VectorData6D yoFeedbackVelocity;
   private final RateLimitedVectorData6D rateLimitedFeedbackVelocity;

   private final VectorData6D yoDesiredAcceleration;
   private final VectorData6D yoFeedForwardAcceleration;
   private final VectorData6D yoFeedbackAcceleration;
   private final RateLimitedVectorData6D rateLimitedFeedbackAcceleration;
   private final VectorData6D yoAchievedAcceleration;

   private final VectorData6D yoDesiredWrench;
   private final VectorData6D yoFeedForwardWrench;
   private final VectorData6D yoFeedbackWrench;
   private final RateLimitedVectorData6D rateLimitedFeedbackWrench;

   private final VectorData3D yoDesiredRotationVector;
   private final VectorData3D yoCurrentRotationVector;

   private final FramePoint3D desiredPosition = new FramePoint3D();
   private final FrameQuaternion desiredOrientation = new FrameQuaternion();
   private final FramePose3D desiredPose = new FramePose3D();

   private final FrameQuaternion errorOrientationCumulated = new FrameQuaternion();

   private final FrameVector3D desiredLinearVelocity = new FrameVector3D();
   private final FrameVector3D desiredAngularVelocity = new FrameVector3D();
   private final FrameVector3D currentLinearVelocity = new FrameVector3D();
   private final FrameVector3D currentAngularVelocity = new FrameVector3D();
   private final FrameVector3D feedForwardLinearVelocity = new FrameVector3D();
   private final FrameVector3D feedForwardAngularVelocity = new FrameVector3D();

   private final FrameVector3D desiredLinearAcceleration = new FrameVector3D();
   private final FrameVector3D desiredAngularAcceleration = new FrameVector3D();
   private final FrameVector3D feedForwardLinearAction = new FrameVector3D();
   private final FrameVector3D feedForwardAngularAction = new FrameVector3D();
   private final FrameVector3D biasLinearAcceleration = new FrameVector3D();
   private final FrameVector3D achievedAngularAcceleration = new FrameVector3D();
   private final FrameVector3D achievedLinearAcceleration = new FrameVector3D();

   private final FrameVector3D desiredLinearForce = new FrameVector3D();
   private final FrameVector3D desiredAngularTorque = new FrameVector3D();

   private final Twist currentTwist = new Twist();
   private final SpatialAcceleration endEffectorAchievedAcceleration = new SpatialAcceleration();

   private final SpatialAccelerationCommand inverseDynamicsOutput = new SpatialAccelerationCommand();
   private final SpatialVelocityCommand inverseKinematicsOutput = new SpatialVelocityCommand();
   private final VirtualWrenchCommand virtualModelControlOutput = new VirtualWrenchCommand();
   private final MomentumRateCommand virtualModelControlRootOutput = new MomentumRateCommand();
   private final SelectionMatrix6D selectionMatrix = new SelectionMatrix6D();

   private final YoPIDSE3Gains gains;
   private final YoPID3DGains positionGains;
   private final YoPID3DGains orientationGains;
   private final Matrix3D tempGainMatrix = new Matrix3D();

   private final RigidBodyAccelerationProvider rigidBodyAccelerationProvider;

   private final RigidBodyBasics rootBody;
   private RigidBodyBasics base;
   private ReferenceFrame controlBaseFrame;
   private ReferenceFrame angularGainsFrame;
   private ReferenceFrame linearGainsFrame;

   private final RigidBodyBasics endEffector;
   private final YoSE3OffsetFrame controlFrame;

   private final double dt;
   private final boolean isRootBody;
   private final boolean computeIntegralTerm;

   private final int controllerIndex;

   public SpatialFeedbackController(RigidBodyBasics endEffector, WholeBodyControlCoreToolbox ccToolbox, FeedbackControllerToolbox fbToolbox,
                                    YoVariableRegistry parentRegistry)
   {
      this(endEffector, 0, ccToolbox, fbToolbox, parentRegistry);
   }

   public SpatialFeedbackController(RigidBodyBasics endEffector, int controllerIndex, WholeBodyControlCoreToolbox ccToolbox,
                                    FeedbackControllerToolbox fbToolbox, YoVariableRegistry parentRegistry)
   {
      this.endEffector = endEffector;
      this.controllerIndex = controllerIndex;
      FeedbackControllerSettings settings = ccToolbox.getFeedbackControllerSettings();
      if (settings != null)
         computeIntegralTerm = settings.enableIntegralTerm();
      else
         computeIntegralTerm = true;

      if (ccToolbox.getRootJoint() != null)
      {
         this.rootBody = ccToolbox.getRootJoint().getSuccessor();
         isRootBody = this.endEffector.getName().equals(rootBody.getName());
      }
      else
      {
         isRootBody = false;
         rootBody = null;
      }

      rigidBodyAccelerationProvider = ccToolbox.getRigidBodyAccelerationProvider();

      String endEffectorName = endEffector.getName();
      registry = new YoVariableRegistry(appendIndex(endEffectorName, controllerIndex) + "SpatialFBController");
      dt = ccToolbox.getControlDT();
      gains = fbToolbox.getOrCreateSE3PIDGains(endEffector, controllerIndex, computeIntegralTerm);
      positionGains = gains.getPositionGains();
      orientationGains = gains.getOrientationGains();
      YoDouble maximumLinearRate = positionGains.getYoMaximumFeedbackRate();
      YoDouble maximumAngularRate = orientationGains.getYoMaximumFeedbackRate();

      controlFrame = fbToolbox.getOrCreateControlFrame(endEffector, controllerIndex);

      isEnabled = new YoBoolean(appendIndex(endEffectorName, controllerIndex) + "isSpatialFBControllerEnabled", registry);
      isEnabled.set(false);

      yoDesiredPose = fbToolbox.getOrCreatePoseData(endEffector, controllerIndex, DESIRED, isEnabled);
      yoCurrentPose = fbToolbox.getOrCreatePoseData(endEffector, controllerIndex, CURRENT, isEnabled);
      VectorData3D errorPosition = fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, ERROR, POSITION, isEnabled);
      VectorData3D errorRotationVector = fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, ERROR, ROTATION_VECTOR, isEnabled);
      yoErrorVector = new VectorData6D(errorRotationVector, errorPosition);
      yoErrorOrientation = fbToolbox.getOrCreateOrientationData(endEffector, controllerIndex, ERROR, isEnabled);
      yoErrorPositionIntegrated = computeIntegralTerm ? fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, ERROR_INTEGRATED, POSITION, isEnabled)
            : null;
      yoErrorOrientationCumulated = computeIntegralTerm ? fbToolbox.getOrCreateOrientationData(endEffector, controllerIndex, ERROR_CUMULATED, isEnabled) : null;
      yoErrorRotationVectorIntegrated = computeIntegralTerm
            ? fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, ERROR_INTEGRATED, ROTATION_VECTOR, isEnabled)
            : null;

      yoDesiredRotationVector = fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, DESIRED, ROTATION_VECTOR, isEnabled);
      yoCurrentRotationVector = fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, CURRENT, ROTATION_VECTOR, isEnabled);

      yoDesiredVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, DESIRED, VELOCITY, isEnabled);

      if (ccToolbox.isEnableInverseDynamicsModule() || ccToolbox.isEnableVirtualModelControlModule())
      {

         yoCurrentVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, CURRENT, VELOCITY, isEnabled);
         yoErrorVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, ERROR, VELOCITY, isEnabled);

         DoubleProvider breakFrequency = fbToolbox.getErrorVelocityFilterBreakFrequency(endEffectorName);
         if (breakFrequency != null)
         {
            yoFilteredErrorVelocity = fbToolbox.getOrCreateAlphaFilteredVectorData6D(endEffector,
                                                                                     controllerIndex,
                                                                                     ERROR,
                                                                                     VELOCITY,
                                                                                     dt,
                                                                                     breakFrequency,
                                                                                     breakFrequency,
                                                                                     isEnabled);
         }
         else
         {
            yoFilteredErrorVelocity = null;
         }

         if (ccToolbox.isEnableInverseDynamicsModule())
         {
            yoDesiredAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, DESIRED, ACCELERATION, isEnabled);
            yoFeedForwardAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDFORWARD, ACCELERATION, isEnabled);
            yoFeedbackAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDBACK, ACCELERATION, isEnabled);
            rateLimitedFeedbackAcceleration = fbToolbox.getOrCreateRateLimitedVectorData6D(endEffector,
                                                                                           controllerIndex,
                                                                                           FEEDBACK,
                                                                                           ACCELERATION,
                                                                                           dt,
                                                                                           maximumAngularRate,
                                                                                           maximumLinearRate,
                                                                                           isEnabled);
            yoAchievedAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, ACHIEVED, ACCELERATION, isEnabled);
         }
         else
         {
            yoDesiredAcceleration = null;
            yoFeedForwardAcceleration = null;
            yoFeedbackAcceleration = null;
            rateLimitedFeedbackAcceleration = null;
            yoAchievedAcceleration = null;
         }

         if (ccToolbox.isEnableVirtualModelControlModule())
         {
            yoDesiredWrench = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, DESIRED, FORCE, isEnabled);
            yoFeedForwardWrench = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDFORWARD, FORCE, isEnabled);
            yoFeedbackWrench = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDBACK, FORCE, isEnabled);
            rateLimitedFeedbackWrench = fbToolbox.getOrCreateRateLimitedVectorData6D(endEffector,
                                                                                     controllerIndex,
                                                                                     FEEDBACK,
                                                                                     FORCE,
                                                                                     dt,
                                                                                     maximumAngularRate,
                                                                                     maximumLinearRate,
                                                                                     isEnabled);
         }
         else
         {
            yoDesiredWrench = null;
            yoFeedForwardWrench = null;
            yoFeedbackWrench = null;
            rateLimitedFeedbackWrench = null;
         }
      }
      else
      {
         yoCurrentVelocity = null;
         yoErrorVelocity = null;
         yoFilteredErrorVelocity = null;

         yoDesiredAcceleration = null;
         yoFeedForwardAcceleration = null;
         yoFeedbackAcceleration = null;
         rateLimitedFeedbackAcceleration = null;
         yoAchievedAcceleration = null;

         yoDesiredWrench = null;
         yoFeedForwardWrench = null;
         yoFeedbackWrench = null;
         rateLimitedFeedbackWrench = null;
      }

      if (ccToolbox.isEnableInverseKinematicsModule())
      {
         yoFeedbackVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDBACK, VELOCITY, isEnabled);
         yoFeedForwardVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDFORWARD, VELOCITY, isEnabled);
         rateLimitedFeedbackVelocity = fbToolbox.getOrCreateRateLimitedVectorData6D(endEffector,
                                                                                    controllerIndex,
                                                                                    FEEDBACK,
                                                                                    VELOCITY,
                                                                                    dt,
                                                                                    maximumAngularRate,
                                                                                    maximumLinearRate,
                                                                                    isEnabled);
      }
      else
      {
         yoFeedbackVelocity = null;
         yoFeedForwardVelocity = null;
         rateLimitedFeedbackVelocity = null;
      }

      parentRegistry.addChild(registry);
   }

   public void submitFeedbackControlCommand(SpatialFeedbackControlCommand command)
   {
      if (command.getEndEffector() != endEffector)
         throw new FeedbackControllerException("Wrong end effector - received: " + command.getEndEffector() + ", expected: " + endEffector);

      base = command.getBase();
      controlBaseFrame = command.getControlBaseFrame();
      inverseDynamicsOutput.set(command.getSpatialAccelerationCommand());
      inverseKinematicsOutput.setProperties(command.getSpatialAccelerationCommand());
      virtualModelControlOutput.setProperties(command.getSpatialAccelerationCommand());

      gains.set(command.getGains());
      command.getSpatialAccelerationCommand().getSelectionMatrix(selectionMatrix);
      angularGainsFrame = command.getAngularGainsFrame();
      linearGainsFrame = command.getLinearGainsFrame();

      command.getControlFramePoseIncludingFrame(desiredPosition, desiredOrientation);
      controlFrame.setOffsetToParent(desiredPosition, desiredOrientation);

      yoDesiredPose.setIncludingFrame(command.getReferencePosition(), command.getReferenceOrientation());
      yoDesiredPose.getOrientation().getRotationVector(yoDesiredRotationVector);
      yoDesiredVelocity.setIncludingFrame(command.getReferenceAngularVelocity(), command.getReferenceLinearVelocity());
      yoDesiredVelocity.checkReferenceFrameMatch(yoDesiredPose);
      if (yoFeedForwardVelocity != null)
      {
         yoFeedForwardVelocity.setIncludingFrame(command.getReferenceAngularVelocity(), command.getReferenceLinearVelocity());
         yoFeedForwardVelocity.checkReferenceFrameMatch(yoDesiredPose);
      }
      if (yoFeedForwardAcceleration != null)
      {
         yoFeedForwardAcceleration.setIncludingFrame(command.getReferenceAngularAcceleration(), command.getReferenceLinearAcceleration());
         yoFeedForwardAcceleration.checkReferenceFrameMatch(yoDesiredPose);
      }
      if (yoFeedForwardWrench != null)
      {
         yoFeedForwardWrench.setIncludingFrame(command.getReferenceTorque(), command.getReferenceForce());
         yoFeedForwardWrench.checkReferenceFrameMatch(yoDesiredPose);
      }
   }

   @Override
   public void setEnabled(boolean isEnabled)
   {
      this.isEnabled.set(isEnabled);
   }

   @Override
   public void initialize()
   {
      if (rateLimitedFeedbackAcceleration != null)
         rateLimitedFeedbackAcceleration.reset();
      if (rateLimitedFeedbackVelocity != null)
         rateLimitedFeedbackVelocity.reset();
      if (yoFilteredErrorVelocity != null)
         yoFilteredErrorVelocity.reset();
      if (yoErrorPositionIntegrated != null)
         yoErrorPositionIntegrated.setToZero(worldFrame);
      if (yoErrorOrientationCumulated != null)
         yoErrorOrientationCumulated.setToZero(worldFrame);
      if (yoErrorRotationVectorIntegrated != null)
         yoErrorRotationVectorIntegrated.setToZero(worldFrame);
   }

   private final FrameVector3D linearProportionalFeedback = new FrameVector3D();
   private final FrameVector3D linearDerivativeFeedback = new FrameVector3D();
   private final FrameVector3D linearIntegralFeedback = new FrameVector3D();

   private final FrameVector3D angularProportionalFeedback = new FrameVector3D();
   private final FrameVector3D angularDerivativeFeedback = new FrameVector3D();
   private final FrameVector3D angularIntegralFeedback = new FrameVector3D();

   @Override
   public void computeInverseDynamics()
   {
      if (!isEnabled())
         return;

      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();

      computeProportionalTerm(linearProportionalFeedback, angularProportionalFeedback);
      computeDerivativeTerm(linearDerivativeFeedback, angularDerivativeFeedback);
      computeIntegralTerm(linearIntegralFeedback, angularIntegralFeedback);
      feedForwardLinearAction.setIncludingFrame(yoFeedForwardAcceleration.getLinearPart());
      feedForwardAngularAction.setIncludingFrame(yoFeedForwardAcceleration.getAngularPart());
      feedForwardLinearAction.changeFrame(controlFrame);
      feedForwardAngularAction.changeFrame(controlFrame);

      desiredLinearAcceleration.setIncludingFrame(linearProportionalFeedback);
      desiredLinearAcceleration.add(linearDerivativeFeedback);
      desiredLinearAcceleration.add(linearIntegralFeedback);
      desiredLinearAcceleration.clipToMaxLength(positionGains.getMaximumFeedback());

      desiredAngularAcceleration.setIncludingFrame(angularProportionalFeedback);
      desiredAngularAcceleration.add(angularDerivativeFeedback);
      desiredAngularAcceleration.add(angularIntegralFeedback);
      desiredAngularAcceleration.clipToMaxLength(orientationGains.getMaximumFeedback());

      yoFeedbackAcceleration.setIncludingFrame(desiredAngularAcceleration, desiredLinearAcceleration);
      yoFeedbackAcceleration.changeFrame(trajectoryFrame);
      rateLimitedFeedbackAcceleration.changeFrame(trajectoryFrame);
      rateLimitedFeedbackAcceleration.update();
      desiredLinearAcceleration.setIncludingFrame(rateLimitedFeedbackAcceleration.getLinearPart());
      desiredAngularAcceleration.setIncludingFrame(rateLimitedFeedbackAcceleration.getAngularPart());

      desiredLinearAcceleration.changeFrame(controlFrame);
      desiredLinearAcceleration.add(feedForwardLinearAction);

      desiredAngularAcceleration.changeFrame(controlFrame);
      desiredAngularAcceleration.add(feedForwardAngularAction);

      yoDesiredAcceleration.setIncludingFrame(desiredAngularAcceleration, desiredLinearAcceleration);
      yoDesiredAcceleration.changeFrame(trajectoryFrame);

      addCoriolisAcceleration(desiredLinearAcceleration);

      inverseDynamicsOutput.setSpatialAcceleration(controlFrame, desiredAngularAcceleration, desiredLinearAcceleration);
   }

   @Override
   public void computeInverseKinematics()
   {
      if (!isEnabled())
         return;

      inverseKinematicsOutput.setProperties(inverseDynamicsOutput);
      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();

      feedForwardLinearVelocity.setIncludingFrame(yoFeedForwardVelocity.getLinearPart());
      feedForwardAngularVelocity.setIncludingFrame(yoFeedForwardVelocity.getAngularPart());
      computeProportionalTerm(linearProportionalFeedback, angularProportionalFeedback);
      computeIntegralTerm(linearIntegralFeedback, angularIntegralFeedback);

      desiredLinearVelocity.setIncludingFrame(linearProportionalFeedback);
      desiredLinearVelocity.add(linearIntegralFeedback);
      desiredLinearVelocity.clipToMaxLength(positionGains.getMaximumFeedback());

      desiredAngularVelocity.setIncludingFrame(angularProportionalFeedback);
      desiredAngularVelocity.add(angularIntegralFeedback);
      desiredAngularVelocity.clipToMaxLength(orientationGains.getMaximumFeedback());

      yoFeedbackVelocity.setIncludingFrame(desiredAngularVelocity, desiredLinearVelocity);
      yoFeedbackVelocity.changeFrame(trajectoryFrame);
      rateLimitedFeedbackVelocity.changeFrame(trajectoryFrame);
      rateLimitedFeedbackVelocity.update();
      desiredLinearVelocity.setIncludingFrame(rateLimitedFeedbackVelocity.getLinearPart());
      desiredAngularVelocity.setIncludingFrame(rateLimitedFeedbackVelocity.getAngularPart());

      desiredLinearVelocity.add(feedForwardLinearVelocity);
      desiredAngularVelocity.add(feedForwardAngularVelocity);

      yoDesiredVelocity.setIncludingFrame(desiredAngularVelocity, desiredLinearVelocity);
      yoDesiredVelocity.changeFrame(trajectoryFrame);

      desiredLinearVelocity.changeFrame(controlFrame);
      desiredAngularVelocity.changeFrame(controlFrame);
      inverseKinematicsOutput.setSpatialVelocity(controlFrame, desiredAngularVelocity, desiredLinearVelocity);
   }

   @Override
   public void computeVirtualModelControl()
   {
      if (!isEnabled())
         return;

      computeFeedbackWrench();

      if (isRootBody)
      {
         desiredLinearForce.changeFrame(worldFrame);
         desiredAngularTorque.changeFrame(worldFrame);

         virtualModelControlRootOutput.setProperties(inverseDynamicsOutput);
         virtualModelControlRootOutput.setMomentumRate(desiredAngularTorque, desiredLinearForce);
      }
      else
      {
         virtualModelControlOutput.setProperties(inverseDynamicsOutput);
         virtualModelControlOutput.setWrench(controlFrame, desiredAngularTorque, desiredLinearForce);
      }
   }

   private void computeFeedbackWrench()
   {
      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();

      feedForwardLinearAction.setIncludingFrame(yoFeedForwardWrench.getLinearPart());
      feedForwardAngularAction.setIncludingFrame(yoFeedForwardWrench.getAngularPart());
      feedForwardLinearAction.changeFrame(controlFrame);
      feedForwardAngularAction.changeFrame(controlFrame);
      computeProportionalTerm(linearProportionalFeedback, angularProportionalFeedback);
      computeDerivativeTerm(linearDerivativeFeedback, angularDerivativeFeedback);
      computeIntegralTerm(linearIntegralFeedback, angularIntegralFeedback);

      desiredLinearForce.setIncludingFrame(linearProportionalFeedback);
      desiredLinearForce.add(linearDerivativeFeedback);
      desiredLinearForce.add(linearIntegralFeedback);
      desiredLinearForce.clipToMaxLength(positionGains.getMaximumFeedback());

      desiredAngularTorque.setIncludingFrame(angularProportionalFeedback);
      desiredAngularTorque.add(angularDerivativeFeedback);
      desiredAngularTorque.add(angularIntegralFeedback);
      desiredAngularTorque.clipToMaxLength(orientationGains.getMaximumFeedback());

      yoFeedbackWrench.setIncludingFrame(desiredAngularTorque, desiredLinearForce);
      yoFeedbackWrench.changeFrame(trajectoryFrame);
      rateLimitedFeedbackWrench.changeFrame(trajectoryFrame);
      rateLimitedFeedbackWrench.update();
      desiredLinearForce.setIncludingFrame(rateLimitedFeedbackWrench.getLinearPart());
      desiredAngularTorque.setIncludingFrame(rateLimitedFeedbackWrench.getAngularPart());

      desiredLinearForce.changeFrame(controlFrame);
      desiredLinearForce.add(feedForwardLinearAction);

      desiredAngularTorque.changeFrame(controlFrame);
      desiredAngularTorque.add(feedForwardAngularAction);

      yoDesiredWrench.setIncludingFrame(desiredAngularTorque, desiredLinearForce);
      yoDesiredWrench.changeFrame(trajectoryFrame);
   }

   @Override
   public void computeAchievedAcceleration()
   {
      endEffectorAchievedAcceleration.setIncludingFrame(rigidBodyAccelerationProvider.getRelativeAcceleration(base, endEffector));
      endEffectorAchievedAcceleration.changeFrame(controlFrame);
      achievedAngularAcceleration.setIncludingFrame(endEffectorAchievedAcceleration.getAngularPart());
      achievedLinearAcceleration.setIncludingFrame(endEffectorAchievedAcceleration.getLinearPart());
      subtractCoriolisAcceleration(achievedLinearAcceleration);

      yoAchievedAcceleration.setReferenceFrame(yoDesiredPose.getReferenceFrame());
      yoAchievedAcceleration.getAngularPart().setMatchingFrame(achievedAngularAcceleration);
      yoAchievedAcceleration.getLinearPart().setMatchingFrame(achievedLinearAcceleration);
   }

   /**
    * Computes the feedback term resulting from the error in position and orienation:<br>
    * x<sub>FB</sub><sup>linear</sup> = kp<sup>linear</sup> * (x<sub>desired</sub> -
    * x<sub>current</sub>)<br>
    * x<sub>FB</sub><sup>angular</sup> = kp<sup>angular</sup> * &theta;<sub>error</sub><br>
    * where &theta;<sub>error</sub> is a rotation vector representing the current error in orientation.
    * <p>
    * The desired pose of the {@code controlFrame} is obtained from {@link #yoDesiredPose}.
    * </p>
    * <p>
    * This method also updates {@link #yoCurrentPose}, {@link #yoErrorVector}, and
    * {@link #yoErrorOrientation}.
    * </p>
    *
    * @param linearFeedbackTermToPack  the value of the feedback term x<sub>FB</sub><sup>linear</sup>.
    *                                  Modified.
    * @param angularFeedbackTermToPack the value of the feedback term x<sub>FB</sub><sup>angular</sup>.
    *                                  Modified.
    */
   private void computeProportionalTerm(FrameVector3D linearFeedbackTermToPack, FrameVector3D angularFeedbackTermToPack)
   {
      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();

      yoCurrentPose.setToZero(controlFrame);
      yoCurrentPose.changeFrame(trajectoryFrame);
      yoCurrentPose.getOrientation().getRotationVector(yoCurrentRotationVector);

      desiredPose.setIncludingFrame(yoDesiredPose);
      desiredPose.changeFrame(controlFrame);

      desiredPose.getOrientation().normalizeAndLimitToPi();
      linearFeedbackTermToPack.setIncludingFrame(desiredPose.getPosition());
      desiredPose.getRotationVector(angularFeedbackTermToPack);

      selectionMatrix.applyLinearSelection(linearFeedbackTermToPack);
      selectionMatrix.applyAngularSelection(angularFeedbackTermToPack);

      linearFeedbackTermToPack.clipToMaxLength(positionGains.getMaximumProportionalError());
      angularFeedbackTermToPack.clipToMaxLength(orientationGains.getMaximumProportionalError());

      yoErrorVector.setIncludingFrame(angularFeedbackTermToPack, linearFeedbackTermToPack);
      yoErrorVector.changeFrame(trajectoryFrame);
      yoErrorOrientation.setRotationVectorIncludingFrame(yoErrorVector.getAngularPart());

      if (linearGainsFrame != null)
         linearFeedbackTermToPack.changeFrame(linearGainsFrame);
      else
         linearFeedbackTermToPack.changeFrame(controlFrame);

      if (angularGainsFrame != null)
         angularFeedbackTermToPack.changeFrame(angularGainsFrame);
      else
         angularFeedbackTermToPack.changeFrame(controlFrame);

      positionGains.getProportionalGainMatrix(tempGainMatrix);
      tempGainMatrix.transform(linearFeedbackTermToPack);

      orientationGains.getProportionalGainMatrix(tempGainMatrix);
      tempGainMatrix.transform(angularFeedbackTermToPack);

      linearFeedbackTermToPack.changeFrame(controlFrame);
      angularFeedbackTermToPack.changeFrame(controlFrame);
   }

   /**
    * Computes the feedback term resulting from the error in linear velocity:<br>
    * x<sub>FB</sub><sup>linear</sup> = kd<sup>linear</sup> * (xDot<sub>desired</sub> -
    * xDot<sub>current</sub>)<br>
    * x<sub>FB</sub><sup>angular</sup> = kd<sup>angular</sup> * (&omega;<sub>desired</sub> -
    * &omega;<sub>current</sub>)
    * <p>
    * The desired velocity of the {@code controlFrame} relative to the {@code base} is obtained from
    * {@link #yoDesiredVelocity}.
    * </p>
    * <p>
    * This method also updates {@link #yoCurrentVelocity} and {@link #yoErrorVelocity}.
    * </p>
    *
    * @param linearFeedbackTermToPack  the value of the feedback term x<sub>FB</sub><sup>linear</sup>.
    *                                  Modified.
    * @param angularFeedbackTermToPack the value of the feedback term x<sub>FB</sub><sup>angular</sup>.
    *                                  Modified.
    */
   private void computeDerivativeTerm(FrameVector3D linearFeedbackTermToPack, FrameVector3D angularFeedbackTermToPack)
   {
      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();

      controlFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
      yoCurrentVelocity.setIncludingFrame(currentTwist.getAngularPart(), currentTwist.getLinearPart());
      yoCurrentVelocity.changeFrame(trajectoryFrame);

      linearFeedbackTermToPack.setToZero(trajectoryFrame);
      angularFeedbackTermToPack.setToZero(trajectoryFrame);
      linearFeedbackTermToPack.sub(yoDesiredVelocity.getLinearPart(), yoCurrentVelocity.getLinearPart());
      angularFeedbackTermToPack.sub(yoDesiredVelocity.getAngularPart(), yoCurrentVelocity.getAngularPart());
      linearFeedbackTermToPack.changeFrame(controlFrame);
      angularFeedbackTermToPack.changeFrame(controlFrame);
      selectionMatrix.applyLinearSelection(linearFeedbackTermToPack);
      selectionMatrix.applyAngularSelection(angularFeedbackTermToPack);

      linearFeedbackTermToPack.clipToMaxLength(positionGains.getMaximumDerivativeError());
      angularFeedbackTermToPack.clipToMaxLength(orientationGains.getMaximumDerivativeError());

      if (yoFilteredErrorVelocity != null)
      {
         // If the trajectory frame changed reset the filter.
         if (yoFilteredErrorVelocity.getReferenceFrame() != trajectoryFrame)
         {
            yoFilteredErrorVelocity.setReferenceFrame(trajectoryFrame);
            yoFilteredErrorVelocity.reset();
         }
         linearFeedbackTermToPack.changeFrame(trajectoryFrame);
         angularFeedbackTermToPack.changeFrame(trajectoryFrame);
         yoErrorVelocity.setIncludingFrame(angularFeedbackTermToPack, linearFeedbackTermToPack);
         yoFilteredErrorVelocity.update();
         linearFeedbackTermToPack.set(yoFilteredErrorVelocity.getLinearPart());
         angularFeedbackTermToPack.set(yoFilteredErrorVelocity.getAngularPart());
      }
      else
      {
         yoErrorVelocity.setIncludingFrame(angularFeedbackTermToPack, linearFeedbackTermToPack);
      }
      yoErrorVelocity.changeFrame(trajectoryFrame);

      if (linearGainsFrame != null)
         linearFeedbackTermToPack.changeFrame(linearGainsFrame);
      else
         linearFeedbackTermToPack.changeFrame(controlFrame);

      if (angularGainsFrame != null)
         angularFeedbackTermToPack.changeFrame(angularGainsFrame);
      else
         angularFeedbackTermToPack.changeFrame(controlFrame);

      positionGains.getDerivativeGainMatrix(tempGainMatrix);
      tempGainMatrix.transform(linearFeedbackTermToPack);

      orientationGains.getDerivativeGainMatrix(tempGainMatrix);
      tempGainMatrix.transform(angularFeedbackTermToPack);

      linearFeedbackTermToPack.changeFrame(controlFrame);
      angularFeedbackTermToPack.changeFrame(controlFrame);
   }

   /**
    * Computes the feedback term resulting from the integrated error in position:<br>
    * x<sub>FB</sub><sup>linear</sup> = ki<sup>linear</sup> * &int;<sup>t</sup> (x<sub>desired</sub> -
    * x<sub>current</sub>)<br>
    * x<sub>FB</sub><sup>angular</sup> = ki<sup>angular</sup> * &int;<sup>t</sup>
    * &theta;<sub>error</sub>
    * <p>
    * The current error in position and orientation of the {@code controlFrame} is obtained from
    * {@link #yoErrorVector} and {@link #yoErrorOrientation} respectively.
    * </p>
    * <p>
    * This method also updates {@link #yoErrorPositionIntegrated},
    * {@link #yoErrorOrientationCumulated}, and {@link #yoErrorRotationVectorIntegrated}.
    * </p>
    *
    * @param linearFeedbackTermToPack  the value of the feedback term x<sub>FB</sub><sup>linear</sup>.
    *                                  Modified.
    * @param angularFeedbackTermToPack the value of the feedback term x<sub>FB</sub><sup>angular</sup>.
    *                                  Modified.
    */
   private void computeIntegralTerm(FrameVector3D linearFeedbackTermToPack, FrameVector3D angularFeedbackTermToPack)
   {
      if (!computeIntegralTerm)
      {
         linearFeedbackTermToPack.setToZero(controlFrame);
         angularFeedbackTermToPack.setToZero(controlFrame);
         return;
      }

      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();

      double maximumLinearIntegralError = positionGains.getMaximumIntegralError();

      if (maximumLinearIntegralError < 1.0e-5)
      {
         linearFeedbackTermToPack.setToZero(controlFrame);
         yoErrorPositionIntegrated.setToZero(trajectoryFrame);
      }
      else
      {
         // If the trajectory frame changed reset the integration.
         if (yoErrorPositionIntegrated.getReferenceFrame() != trajectoryFrame)
         {
            yoErrorPositionIntegrated.setToZero(trajectoryFrame);
         }

         linearFeedbackTermToPack.setIncludingFrame(yoErrorVector.getLinearPart());
         linearFeedbackTermToPack.scale(dt);
         linearFeedbackTermToPack.add(yoErrorPositionIntegrated);
         linearFeedbackTermToPack.changeFrame(controlFrame);
         selectionMatrix.applyLinearSelection(linearFeedbackTermToPack);
         linearFeedbackTermToPack.clipToMaxLength(maximumLinearIntegralError);
         yoErrorPositionIntegrated.setIncludingFrame(linearFeedbackTermToPack);
         yoErrorPositionIntegrated.changeFrame(trajectoryFrame);

         if (linearGainsFrame != null)
            linearFeedbackTermToPack.changeFrame(linearGainsFrame);
         else
            linearFeedbackTermToPack.changeFrame(controlFrame);

         positionGains.getIntegralGainMatrix(tempGainMatrix);
         tempGainMatrix.transform(linearFeedbackTermToPack);

         linearFeedbackTermToPack.changeFrame(controlFrame);
      }

      double maximumAngularIntegralError = orientationGains.getMaximumIntegralError();

      if (maximumAngularIntegralError < 1.0e-5)
      {
         angularFeedbackTermToPack.setToZero(controlFrame);
         yoErrorOrientationCumulated.setToZero(trajectoryFrame);
         yoErrorRotationVectorIntegrated.setToZero(trajectoryFrame);
      }
      else
      {
         // If the trajectory frame changed reset the integration.
         if (yoErrorOrientationCumulated.getReferenceFrame() != trajectoryFrame)
         {
            yoErrorOrientationCumulated.setToZero(trajectoryFrame);
            yoErrorRotationVectorIntegrated.setToZero(trajectoryFrame);
         }

         errorOrientationCumulated.setIncludingFrame(yoErrorOrientationCumulated);
         errorOrientationCumulated.multiply(yoErrorOrientation);
         yoErrorOrientationCumulated.set(errorOrientationCumulated);
         errorOrientationCumulated.normalizeAndLimitToPi();

         errorOrientationCumulated.getRotationVector(angularFeedbackTermToPack);
         angularFeedbackTermToPack.scale(dt);
         angularFeedbackTermToPack.changeFrame(controlFrame);
         selectionMatrix.applyAngularSelection(angularFeedbackTermToPack);
         angularFeedbackTermToPack.clipToMaxLength(maximumAngularIntegralError);
         yoErrorRotationVectorIntegrated.setIncludingFrame(angularFeedbackTermToPack);
         yoErrorRotationVectorIntegrated.changeFrame(trajectoryFrame);

         if (angularGainsFrame != null)
            angularFeedbackTermToPack.changeFrame(angularGainsFrame);
         else
            angularFeedbackTermToPack.changeFrame(controlFrame);

         orientationGains.getIntegralGainMatrix(tempGainMatrix);
         tempGainMatrix.transform(angularFeedbackTermToPack);

         angularFeedbackTermToPack.changeFrame(controlFrame);
      }
   }

   /**
    * Computes and adds the bias acceleration resulting from the combination of the current linear and
    * angular velocity of the control frame.
    * <p>
    * This is needed when going from a linear acceleration expressed in an inertial frame to a moving
    * frame attached to the end-effector.
    * </p>
    * <p>
    * Intuitively, the Coriolis acceleration only appears when measuring the acceleration from a moving
    * frame, here a frame attache to the end-effector.
    * </p>
    *
    * @param linearAccelerationToModify the linear acceleration vector to which the bias is to be
    *                                   subtracted. Its frame is changed to {@code controlFrame}.
    *                                   Modified.
    */
   private void addCoriolisAcceleration(FrameVector3D linearAccelerationToModify)
   {
      controlFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
      currentAngularVelocity.setIncludingFrame(currentTwist.getAngularPart());
      currentLinearVelocity.setIncludingFrame(currentTwist.getLinearPart());

      biasLinearAcceleration.setToZero(controlFrame);
      biasLinearAcceleration.cross(currentLinearVelocity, currentAngularVelocity);
      linearAccelerationToModify.changeFrame(controlFrame);
      linearAccelerationToModify.add(biasLinearAcceleration);
   }

   /**
    * Computes and subtracts the bias acceleration resulting from the combination of the current linear
    * and angular velocity of the control frame.
    * <p>
    * This is needed when going from a linear acceleration expressed in a moving frame attached to the
    * end-effector to an inertial frame.
    * </p>
    * <p>
    * Intuitively, the Coriolis acceleration only appears when measuring the acceleration from a moving
    * frame, here a frame attache to the end-effector.
    * </p>
    *
    * @param linearAccelerationToModify the linear acceleration vector to which the bias is to be
    *                                   added. Its frame is changed to {@code worldFrame}. Modified.
    */
   private void subtractCoriolisAcceleration(FrameVector3D linearAccelerationToModify)
   {
      ReferenceFrame originalFrame = linearAccelerationToModify.getReferenceFrame();
      controlFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
      currentAngularVelocity.setIncludingFrame(currentTwist.getAngularPart());
      currentLinearVelocity.setIncludingFrame(currentTwist.getLinearPart());

      biasLinearAcceleration.setToZero(controlFrame);
      biasLinearAcceleration.cross(currentLinearVelocity, currentAngularVelocity);
      linearAccelerationToModify.changeFrame(controlFrame);
      linearAccelerationToModify.sub(biasLinearAcceleration);
      linearAccelerationToModify.changeFrame(originalFrame);
   }

   @Override
   public boolean isEnabled()
   {
      return isEnabled.getBooleanValue();
   }

   @Override
   public SpatialAccelerationCommand getInverseDynamicsOutput()
   {
      if (!isEnabled())
         throw new FeedbackControllerException("This controller is disabled.");
      return inverseDynamicsOutput;
   }

   @Override
   public SpatialVelocityCommand getInverseKinematicsOutput()
   {
      if (!isEnabled())
         throw new FeedbackControllerException("This controller is disabled.");
      return inverseKinematicsOutput;
   }

   @Override
   public VirtualModelControlCommand<?> getVirtualModelControlOutput()
   {
      if (!isEnabled())
         throw new FeedbackControllerException("This controller is disabled.");
      return (isRootBody) ? virtualModelControlRootOutput : virtualModelControlOutput;
   }

   public int getControllerIndex()
   {
      return controllerIndex;
   }

   @Override
   public String toString()
   {
      return getClass().getSimpleName() + ": endEffector = " + endEffector;
   }
}
