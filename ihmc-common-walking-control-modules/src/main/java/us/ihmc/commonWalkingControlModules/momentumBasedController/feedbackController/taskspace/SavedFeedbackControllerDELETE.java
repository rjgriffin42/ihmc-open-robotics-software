//package us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.taskspace;
//
//import org.ejml.data.DMatrixRMaj;
//import org.ejml.dense.row.CommonOps_DDRM;
//import us.ihmc.commonWalkingControlModules.controlModules.YoSE3OffsetFrame;
//import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerException;
//import us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox;
//import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
//import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
//import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.MomentumRateCommand;
//import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseDynamics.SpatialAccelerationCommand;
//import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.SpatialVelocityCommand;
//import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommand;
//import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualWrenchCommand;
//import us.ihmc.commonWalkingControlModules.controllerCore.data.FBPose3D;
//import us.ihmc.commonWalkingControlModules.controllerCore.data.FBQuaternion3D;
//import us.ihmc.commonWalkingControlModules.controllerCore.data.FBRateLimitedVector6D;
//import us.ihmc.commonWalkingControlModules.controllerCore.data.FBVector3D;
//import us.ihmc.commonWalkingControlModules.controllerCore.data.FBVector6D;
//import us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.FeedbackControllerInterface;
//import us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.FeedbackControllerSettings;
//import us.ihmc.commonWalkingControlModules.momentumBasedController.feedbackController.FeedbackControllerSettings.FilterVector3D;
//import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.JointIndexHandler;
//import us.ihmc.euclid.matrix.Matrix3D;
//import us.ihmc.euclid.referenceFrame.FramePoint3D;
//import us.ihmc.euclid.referenceFrame.FramePose3D;
//import us.ihmc.euclid.referenceFrame.FrameQuaternion;
//import us.ihmc.euclid.referenceFrame.FrameVector3D;
//import us.ihmc.euclid.referenceFrame.ReferenceFrame;
//import us.ihmc.mecano.algorithms.CompositeRigidBodyMassMatrixCalculator;
//import us.ihmc.mecano.algorithms.GeometricJacobianCalculator;
//import us.ihmc.mecano.algorithms.interfaces.RigidBodyAccelerationProvider;
//import us.ihmc.mecano.algorithms.interfaces.RigidBodyTwistProvider;
//import us.ihmc.mecano.frames.MovingReferenceFrame;
//import us.ihmc.mecano.multiBodySystem.interfaces.JointBasics;
//import us.ihmc.mecano.multiBodySystem.interfaces.RigidBodyBasics;
//import us.ihmc.mecano.spatial.SpatialAcceleration;
//import us.ihmc.mecano.spatial.Twist;
//import us.ihmc.mecano.tools.MultiBodySystemTools;
//import us.ihmc.robotics.MatrixMissingTools;
//import us.ihmc.robotics.controllers.pidGains.GainCoupling;
//import us.ihmc.robotics.controllers.pidGains.YoPID3DGains;
//import us.ihmc.robotics.controllers.pidGains.YoPIDSE3Gains;
//import us.ihmc.robotics.controllers.pidGains.implementations.DefaultYoPIDSE3Gains;
//import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
//import us.ihmc.yoVariables.registry.YoRegistry;
//import us.ihmc.yoVariables.variable.YoBoolean;
//import us.ihmc.yoVariables.variable.YoDouble;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static us.ihmc.commonWalkingControlModules.controllerCore.FeedbackControllerToolbox.appendIndex;
//import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData3D.POSITION;
//import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData3D.ROTATION_VECTOR;
//import static us.ihmc.commonWalkingControlModules.controllerCore.data.SpaceData6D.*;
//import static us.ihmc.commonWalkingControlModules.controllerCore.data.Type.*;
//
//public class SavedFeedbackControllerDELETE implements FeedbackControllerInterface
//{
//   protected static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
//
//   protected final YoBoolean isEnabled;
//
//   protected final FBPose3D yoDesiredPose;
//   protected final FBPose3D yoCurrentPose;
//
//   protected final FBVector6D yoErrorVector;
//   protected final FBQuaternion3D yoErrorOrientation;
//
//   protected final FBVector3D yoErrorPositionIntegrated;
//   protected final FBQuaternion3D yoErrorOrientationCumulated;
//   protected final FBVector3D yoErrorRotationVectorIntegrated;
//
//   protected final FBVector6D yoDesiredVelocity;
//   protected final FBVector6D yoCurrentVelocity;
//   protected final FBVector6D yoErrorVelocity;
//   protected final FilterVector3D angularVelocityErrorFilter;
//   protected final FilterVector3D linearVelocityErrorFilter;
//   protected final FBVector6D yoFeedForwardVelocity;
//   protected final FBVector6D yoFeedbackVelocity;
//   protected final FBRateLimitedVector6D rateLimitedFeedbackVelocity;
//   protected final FBVector6D yoAchievedVelocity;
//
//   protected final FBVector6D yoDesiredAcceleration;
//   protected final FBVector6D yoFeedForwardAcceleration;
//   protected final FBVector6D yoProportionalFeedbackAcceleration;
//   protected final FBVector6D yoDerivativeFeedbackAcceleration;
//   protected final FBVector6D yoFeedbackAcceleration;
//   protected final FBRateLimitedVector6D rateLimitedFeedbackAcceleration;
//   protected final FBVector6D yoAchievedAcceleration;
//
//   protected final FBVector3D yoDesiredRotationVector;
//   protected final FBVector3D yoCurrentRotationVector;
//
//   protected final FramePoint3D controlFramePosition = new FramePoint3D();
//   protected final FrameQuaternion controlFrameOrientation = new FrameQuaternion();
//   protected final FramePose3D desiredPose = new FramePose3D();
//
//   protected final FrameQuaternion errorOrientationCumulated = new FrameQuaternion();
//
//   protected final FrameVector3D desiredLinearVelocity = new FrameVector3D();
//   protected final FrameVector3D desiredAngularVelocity = new FrameVector3D();
//   protected final FrameVector3D currentLinearVelocity = new FrameVector3D();
//   protected final FrameVector3D currentAngularVelocity = new FrameVector3D();
//   protected final FrameVector3D feedForwardLinearVelocity = new FrameVector3D();
//   protected final FrameVector3D feedForwardAngularVelocity = new FrameVector3D();
//   protected final FrameVector3D achievedAngularVelocity = new FrameVector3D();
//   protected final FrameVector3D achievedLinearVelocity = new FrameVector3D();
//
//   protected final FrameVector3D desiredLinearAcceleration = new FrameVector3D();
//   protected final FrameVector3D desiredAngularAcceleration = new FrameVector3D();
//   protected final FrameVector3D feedForwardLinearAction = new FrameVector3D();
//   protected final FrameVector3D feedForwardAngularAction = new FrameVector3D();
//   protected final FrameVector3D biasLinearAcceleration = new FrameVector3D();
//   protected final FrameVector3D achievedAngularAcceleration = new FrameVector3D();
//   protected final FrameVector3D achievedLinearAcceleration = new FrameVector3D();
//
//   protected final FrameVector3D desiredLinearForce = new FrameVector3D();
//   protected final FrameVector3D desiredAngularTorque = new FrameVector3D();
//
//   protected final Twist currentTwist = new Twist();
//   protected final Twist endEffectorAchievedTwist = new Twist();
//   protected final SpatialAcceleration endEffectorAchievedAcceleration = new SpatialAcceleration();
//
//   protected final SpatialAccelerationCommand inverseDynamicsOutput = new SpatialAccelerationCommand();
//   protected final SpatialVelocityCommand inverseKinematicsOutput = new SpatialVelocityCommand();
//   protected final VirtualWrenchCommand virtualModelControlOutput = new VirtualWrenchCommand();
//   protected final MomentumRateCommand virtualModelControlRootOutput = new MomentumRateCommand();
//   protected final SelectionMatrix6D selectionMatrix = new SelectionMatrix6D();
//
//   protected final YoPIDSE3Gains gains;
//   protected final YoPID3DGains positionGains;
//   protected final YoPID3DGains orientationGains;
//   protected final Matrix3D tempGainMatrix = new Matrix3D();
//   private final GeometricJacobianCalculator jacobianCalculator = new GeometricJacobianCalculator();
//   private final DMatrixRMaj inverseInertiaMatrix = new DMatrixRMaj(0, 0);
//
//   protected final RigidBodyTwistProvider rigidBodyTwistProvider;
//   protected final RigidBodyAccelerationProvider rigidBodyAccelerationProvider;
//   private final CompositeRigidBodyMassMatrixCalculator massMatrixCalculator;
//
//   protected final RigidBodyBasics rootBody;
//   protected RigidBodyBasics base;
//   protected ReferenceFrame controlBaseFrame;
//   protected ReferenceFrame angularGainsFrame;
//   protected ReferenceFrame linearGainsFrame;
//
//   protected final RigidBodyBasics endEffector;
//   protected final YoSE3OffsetFrame controlFrame;
//   JointIndexHandler jointIndexHandler;
//   private int[] jointIndices;
//
//   protected final double dt;
//   protected final boolean isRootBody;
//
//   protected final int controllerIndex;
//   protected int currentCommandId;
//
//   public SavedFeedbackControllerDELETE(RigidBodyBasics endEffector,
//                                             WholeBodyControlCoreToolbox ccToolbox,
//                                             FeedbackControllerToolbox fbToolbox,
//                                             YoRegistry parentRegistry)
//   {
//      this(endEffector, 0, ccToolbox, fbToolbox, parentRegistry);
//   }
//
//   public SavedFeedbackControllerDELETE(RigidBodyBasics endEffector,
//                                             int controllerIndex,
//                                             WholeBodyControlCoreToolbox ccToolbox,
//                                             FeedbackControllerToolbox fbToolbox,
//                                             YoRegistry parentRegistry)
//   {
//      this.endEffector = endEffector;
//      this.controllerIndex = controllerIndex;
//      FeedbackControllerSettings settings = ccToolbox.getFeedbackControllerSettings();
//
//      if (ccToolbox.getRootJoint() != null)
//      {
//         this.rootBody = ccToolbox.getRootJoint().getSuccessor();
//         isRootBody = this.endEffector.getName().equals(rootBody.getName());
//      }
//      else
//      {
//         isRootBody = false;
//         rootBody = null;
//      }
//      jointIndexHandler = ccToolbox.getJointIndexHandler();
//
//      rigidBodyTwistProvider = ccToolbox.getRigidBodyTwistCalculator();
//      rigidBodyAccelerationProvider = ccToolbox.getRigidBodyAccelerationProvider();
//      massMatrixCalculator = ccToolbox.getMassMatrixCalculator();
//
//      String endEffectorName = endEffector.getName();
//      dt = ccToolbox.getControlDT();
//      gains = fbToolbox.getOrCreateSE3PIDGains(endEffector, controllerIndex, false, true);
//      positionGains = gains.getPositionGains();
//      orientationGains = gains.getOrientationGains();
//      YoDouble maximumLinearRate = positionGains.getYoMaximumFeedbackRate();
//      YoDouble maximumAngularRate = orientationGains.getYoMaximumFeedbackRate();
//
//      controlFrame = fbToolbox.getOrCreateSpatialFeedbackControlFrame(endEffector, controllerIndex, true);
//
//      isEnabled = new YoBoolean(appendIndex(endEffectorName, controllerIndex) + "isSpatialFBControllerEnabled", fbToolbox.getRegistry());
//      isEnabled.set(false);
//
//      yoDesiredPose = fbToolbox.getOrCreatePoseData(endEffector, controllerIndex, DESIRED, isEnabled, true);
//      yoCurrentPose = fbToolbox.getOrCreatePoseData(endEffector, controllerIndex, CURRENT, isEnabled, true);
//      yoErrorVector = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, ERROR, POSE, isEnabled, false);
//      yoErrorOrientation = fbToolbox.getOrCreateOrientationData(endEffector, controllerIndex, ERROR, isEnabled, false);
//
//      yoErrorPositionIntegrated = null;
//      yoErrorOrientationCumulated = null;
//      yoErrorRotationVectorIntegrated = null;
//
//      yoDesiredRotationVector = fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, DESIRED, ROTATION_VECTOR, isEnabled, true);
//      yoCurrentRotationVector = fbToolbox.getOrCreateVectorData3D(endEffector, controllerIndex, CURRENT, ROTATION_VECTOR, isEnabled, true);
//
//      yoDesiredVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, DESIRED, VELOCITY, isEnabled, true);
//
//      yoCurrentVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, CURRENT, VELOCITY, isEnabled, true);
//      yoErrorVelocity = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, ERROR, VELOCITY, isEnabled, false);
//      angularVelocityErrorFilter = fbToolbox.getOrCreateAngularVelocityErrorFilter(endEffector, controllerIndex, dt);
//      linearVelocityErrorFilter = fbToolbox.getOrCreateLinearVelocityErrorFilter(endEffector, controllerIndex, dt);
//
//      yoDesiredAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, DESIRED, ACCELERATION, isEnabled, true);
//      yoFeedForwardAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDFORWARD, ACCELERATION, isEnabled, true);
//      yoProportionalFeedbackAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, P_FEEDBACK, ACCELERATION, isEnabled, false);
//      yoDerivativeFeedbackAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, D_FEEDBACK, ACCELERATION, isEnabled, false);
//
//      yoFeedbackAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, FEEDBACK, ACCELERATION, isEnabled, false);
//      rateLimitedFeedbackAcceleration = fbToolbox.getOrCreateRateLimitedVectorData6D(endEffector,
//                                                                                     controllerIndex,
//                                                                                     FEEDBACK,
//                                                                                     ACCELERATION,
//                                                                                     dt,
//                                                                                     maximumAngularRate,
//                                                                                     maximumLinearRate,
//                                                                                     isEnabled,
//                                                                                     false);
//      yoAchievedAcceleration = fbToolbox.getOrCreateVectorData6D(endEffector, controllerIndex, ACHIEVED, ACCELERATION, isEnabled, true);
//
//
//      yoFeedbackVelocity = null;
//      yoFeedForwardVelocity = null;
//      rateLimitedFeedbackVelocity = null;
//      yoAchievedVelocity = null;
//   }
//
//   public void submitFeedbackControlCommand(ImpedanceSpatialFeedbackControlCommand command)
//   {
//      if (command.getEndEffector() != endEffector)
//         throw new FeedbackControllerException("Wrong end effector - received: " + command.getEndEffector() + ", expected: " + endEffector);
//
//      currentCommandId = command.getCommandId();
//      base = command.getBase();
//      controlBaseFrame = command.getControlBaseFrame();
//      inverseDynamicsOutput.set(command.getSpatialAccelerationCommand());
//
//      JointBasics[] jointPath = MultiBodySystemTools.createJointPath(base, endEffector);
//      List<Integer> allJointIndices = new ArrayList<>();
//
//      for (JointBasics joint : jointPath)
//      {
//         int[] indices = jointIndexHandler.getJointIndices(joint);
//         for (int index : indices)
//         {
//            allJointIndices.add(index);
//         }
//      }
//
//      jointIndices = allJointIndices.stream().mapToInt(i -> i).toArray();
//
//      gains.set(command.getGains());
//      command.getSpatialAccelerationCommand().getSelectionMatrix(selectionMatrix);
//      angularGainsFrame = command.getAngularGainsFrame();
//      linearGainsFrame = command.getLinearGainsFrame();
//
//      command.getControlFramePoseIncludingFrame(controlFramePosition, controlFrameOrientation);
//      controlFrame.setOffsetToParent(controlFramePosition, controlFrameOrientation);
//
//      yoDesiredPose.setIncludingFrame(command.getReferencePosition(), command.getReferenceOrientation());
//      yoDesiredPose.getOrientation().getRotationVector(yoDesiredRotationVector);
//      yoDesiredPose.setCommandId(currentCommandId);
//      yoDesiredVelocity.setIncludingFrame(command.getReferenceAngularVelocity(), command.getReferenceLinearVelocity());
//      yoDesiredVelocity.checkReferenceFrameMatch(yoDesiredPose);
//      yoDesiredVelocity.setCommandId(currentCommandId);
//      if (yoFeedForwardVelocity != null)
//      {
//         yoFeedForwardVelocity.setIncludingFrame(command.getReferenceAngularVelocity(), command.getReferenceLinearVelocity());
//         yoFeedForwardVelocity.checkReferenceFrameMatch(yoDesiredPose);
//         yoFeedForwardVelocity.setCommandId(currentCommandId);
//      }
//      if (yoFeedForwardAcceleration != null)
//      {
//         yoFeedForwardAcceleration.setIncludingFrame(command.getReferenceAngularAcceleration(), command.getReferenceLinearAcceleration());
//         yoFeedForwardAcceleration.checkReferenceFrameMatch(yoDesiredPose);
//         yoFeedForwardAcceleration.setCommandId(currentCommandId);
//      }
//   }
//
//   @Override
//   public void setEnabled(boolean isEnabled)
//   {
//      this.isEnabled.set(isEnabled);
//   }
//
//   @Override
//   public void initialize()
//   {
//      /*
//       * TODO This seems like a potential bug: the WholeBodyFeedbackController calls initialize on all
//       * inactive controller, if this controller is inactive but the PointFeedbackController or
//       * OrientationFeedbackController (sharing some of the data) is active, this action can affect the
//       * other controller.
//       */
//      if (rateLimitedFeedbackAcceleration != null)
//      {
//         rateLimitedFeedbackAcceleration.setToZero(worldFrame);
//         rateLimitedFeedbackAcceleration.reset();
//      }
//      if (rateLimitedFeedbackVelocity != null)
//      {
//         rateLimitedFeedbackVelocity.setToZero(worldFrame);
//         rateLimitedFeedbackVelocity.reset();
//      }
//      if (angularVelocityErrorFilter != null)
//      {
//         angularVelocityErrorFilter.reset();
//      }
//      if (yoErrorPositionIntegrated != null)
//      {
//         yoErrorPositionIntegrated.setToZero(worldFrame);
//      }
//      if (yoErrorOrientationCumulated != null)
//      {
//         yoErrorOrientationCumulated.setToZero(worldFrame);
//      }
//      if (yoErrorRotationVectorIntegrated != null)
//      {
//         yoErrorRotationVectorIntegrated.setToZero(worldFrame);
//      }
//   }
//
//   protected final FrameVector3D linearProportionalFeedback = new FrameVector3D();
//   protected final FrameVector3D linearDerivativeFeedback = new FrameVector3D();
//
//   protected final FrameVector3D angularProportionalFeedback = new FrameVector3D();
//   protected final FrameVector3D angularDerivativeFeedback = new FrameVector3D();
//
//   private final DMatrixRMaj jacobianMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj massInverseMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj subMassInverseMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj inverseInertiaTempMatrix = new DMatrixRMaj(0, 0);
//   private final Matrix3D inverseInertiaMatrix3D = new Matrix3D();
//
//   @Override
//   public void computeInverseDynamics()
//   {
//      if (!isEnabled())
//         return;
//
//      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();
//
//
//      jacobianCalculator.clear();
//      jacobianCalculator.setKinematicChain(base, endEffector);
//      jacobianCalculator.setJacobianFrame(controlFrame);
//      jacobianCalculator.reset();
//
//      //      Jacobian is an M x N matrix, M is called the task size and
//      //    * N is the overall number of degrees of freedom (DoFs) to be controlled.
//      jacobianMatrix.set(jacobianCalculator.getJacobianMatrix());
//      jacobianMatrix.reshape(jacobianMatrix.getNumRows(), jacobianMatrix.getNumCols());
//
//      massInverseMatrix.set(massMatrixCalculator.getMassMatrix());
//      massInverseMatrix.reshape(massInverseMatrix.getNumRows(), massInverseMatrix.getNumCols());
//      CommonOps_DDRM.invert(massInverseMatrix);
//      subMassInverseMatrix.set(new DMatrixRMaj(jointIndices.length, jointIndices.length));
//      CommonOps_DDRM.extract(massInverseMatrix, jointIndices[0], jointIndices[jointIndices.length - 1] + 1, jointIndices[0], jointIndices[jointIndices.length - 1] + 1, subMassInverseMatrix, 0, 0);
//
//      inverseInertiaTempMatrix.reshape(jointIndices.length, jointIndices.length);
//      CommonOps_DDRM.mult(jacobianMatrix, subMassInverseMatrix, inverseInertiaTempMatrix);
//      CommonOps_DDRM.multTransB(inverseInertiaTempMatrix, jacobianMatrix, inverseInertiaMatrix);
//      inverseInertiaTempMatrix.reshape(3,3);
//
//      computeProportionalTerm(linearProportionalFeedback, angularProportionalFeedback);
//      computeDerivativeTerm(linearDerivativeFeedback, angularDerivativeFeedback);
//
//      // Linear so extract the 3x3 matrix from the 6x6 matrix (Lower right 3x3 matrix)
//      CommonOps_DDRM.extract(inverseInertiaMatrix, 3, 6, 3, 6, inverseInertiaTempMatrix, 0, 0);
//      inverseInertiaMatrix3D.set(inverseInertiaTempMatrix);
//      inverseInertiaMatrix3D.transform(linearProportionalFeedback);
//      inverseInertiaMatrix3D.transform(linearDerivativeFeedback);
//
//      CommonOps_DDRM.extract(inverseInertiaMatrix, 0, 3, 0, 3, inverseInertiaTempMatrix, 0, 0);
//      inverseInertiaMatrix3D.set(inverseInertiaTempMatrix);
//      inverseInertiaMatrix3D.transform(angularProportionalFeedback);
//      inverseInertiaMatrix3D.transform(angularDerivativeFeedback);
//
//      feedForwardLinearAction.setIncludingFrame(yoFeedForwardAcceleration.getLinearPart());
//      feedForwardAngularAction.setIncludingFrame(yoFeedForwardAcceleration.getAngularPart());
//      feedForwardLinearAction.changeFrame(controlFrame);
//      feedForwardAngularAction.changeFrame(controlFrame);
//
//      desiredLinearAcceleration.setIncludingFrame(linearProportionalFeedback);
//      desiredLinearAcceleration.add(linearDerivativeFeedback);
//      desiredLinearAcceleration.clipToMaxNorm(positionGains.getMaximumFeedback());
//
//      desiredAngularAcceleration.setIncludingFrame(angularProportionalFeedback);
//      desiredAngularAcceleration.add(angularDerivativeFeedback);
//      desiredAngularAcceleration.clipToMaxNorm(orientationGains.getMaximumFeedback());
//
//      yoProportionalFeedbackAcceleration.setIncludingFrame(angularProportionalFeedback, linearProportionalFeedback);
//      yoProportionalFeedbackAcceleration.setCommandId(currentCommandId);
//      yoDerivativeFeedbackAcceleration.setIncludingFrame(angularDerivativeFeedback, linearDerivativeFeedback);
//      yoDerivativeFeedbackAcceleration.setCommandId(currentCommandId);
//
//      yoFeedbackAcceleration.setIncludingFrame(desiredAngularAcceleration, desiredLinearAcceleration);
//      yoFeedbackAcceleration.changeFrame(trajectoryFrame);
//      yoFeedbackAcceleration.setCommandId(currentCommandId);
//      rateLimitedFeedbackAcceleration.changeFrame(trajectoryFrame);
//      rateLimitedFeedbackAcceleration.update();
//      rateLimitedFeedbackAcceleration.setCommandId(currentCommandId);
//      desiredLinearAcceleration.setIncludingFrame(rateLimitedFeedbackAcceleration.getLinearPart());
//      desiredAngularAcceleration.setIncludingFrame(rateLimitedFeedbackAcceleration.getAngularPart());
//
//      desiredLinearAcceleration.changeFrame(controlFrame);
//      desiredLinearAcceleration.add(feedForwardLinearAction);
//
//      desiredAngularAcceleration.changeFrame(controlFrame);
//      desiredAngularAcceleration.add(feedForwardAngularAction);
//
//      proccessInverseDynamicsDesiredAcceleration(controlFrame, desiredAngularAcceleration, desiredLinearAcceleration);
//
//      yoDesiredAcceleration.setIncludingFrame(desiredAngularAcceleration, desiredLinearAcceleration);
//      yoDesiredAcceleration.changeFrame(trajectoryFrame);
//      yoDesiredAcceleration.setCommandId(currentCommandId);
//
//      addCoriolisAcceleration(desiredLinearAcceleration);
//
//      inverseDynamicsOutput.setSpatialAcceleration(controlFrame, desiredAngularAcceleration, desiredLinearAcceleration);
//   }
//
//   @Override
//   public void computeInverseKinematics()
//   {
//      assert false : "computeInverseKinematics is not implemented";
//   }
//
//   @Override
//   public void computeVirtualModelControl()
//   {
//      assert false : "computeVirtualModelControl is not implemented";
//   }
//
//   @Override
//   public void computeAchievedAcceleration()
//   {
//      endEffectorAchievedAcceleration.setIncludingFrame(rigidBodyAccelerationProvider.getRelativeAcceleration(base, endEffector));
//      endEffectorAchievedAcceleration.changeFrame(controlFrame);
//      achievedAngularAcceleration.setIncludingFrame(endEffectorAchievedAcceleration.getAngularPart());
//      achievedLinearAcceleration.setIncludingFrame(endEffectorAchievedAcceleration.getLinearPart());
//      subtractCoriolisAcceleration(achievedLinearAcceleration);
//
//      yoAchievedAcceleration.setReferenceFrame(yoDesiredPose.getReferenceFrame());
//      yoAchievedAcceleration.getAngularPart().setMatchingFrame(achievedAngularAcceleration);
//      yoAchievedAcceleration.getLinearPart().setMatchingFrame(achievedLinearAcceleration);
//      yoAchievedAcceleration.setCommandId(currentCommandId);
//   }
//
//   @Override
//   public void computeAchievedVelocity()
//   {
//      if (yoAchievedVelocity == null)
//         return;
//
//      endEffectorAchievedTwist.setIncludingFrame(rigidBodyTwistProvider.getRelativeTwist(base, endEffector));
//      endEffectorAchievedTwist.changeFrame(controlFrame);
//      achievedAngularVelocity.setIncludingFrame(endEffectorAchievedTwist.getAngularPart());
//      achievedLinearVelocity.setIncludingFrame(endEffectorAchievedTwist.getLinearPart());
//
//      yoAchievedVelocity.setReferenceFrame(yoDesiredPose.getReferenceFrame());
//      yoAchievedVelocity.getAngularPart().setMatchingFrame(achievedAngularVelocity);
//      yoAchievedVelocity.getLinearPart().setMatchingFrame(achievedLinearVelocity);
//      yoAchievedVelocity.setCommandId(currentCommandId);
//   }
//
//   /**
//    * Computes the feedback term resulting from the error in position and orienation:<br>
//    * x<sub>FB</sub><sup>linear</sup> = kp<sup>linear</sup> * (x<sub>desired</sub> -
//    * x<sub>current</sub>)<br>
//    * x<sub>FB</sub><sup>angular</sup> = kp<sup>angular</sup> * &theta;<sub>error</sub><br>
//    * where &theta;<sub>error</sub> is a rotation vector representing the current error in orientation.
//    * <p>
//    * The desired pose of the {@code controlFrame} is obtained from {@link #yoDesiredPose}.
//    * </p>
//    * <p>
//    * This method also updates {@link #yoCurrentPose}, {@link #yoErrorVector}, and
//    * {@link #yoErrorOrientation}.
//    * </p>
//    *
//    * @param linearFeedbackTermToPack  the value of the feedback term x<sub>FB</sub><sup>linear</sup>.
//    *                                  Modified.
//    * @param angularFeedbackTermToPack the value of the feedback term x<sub>FB</sub><sup>angular</sup>.
//    *                                  Modified.
//    */
//   protected void computeProportionalTerm(FrameVector3D linearFeedbackTermToPack, FrameVector3D angularFeedbackTermToPack)
//   {
//      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();
//
//      yoCurrentPose.setToZero(controlFrame);
//      yoCurrentPose.changeFrame(trajectoryFrame);
//      yoCurrentPose.setCommandId(currentCommandId);
//      yoCurrentPose.getOrientation().getRotationVector(yoCurrentRotationVector);
//      yoCurrentRotationVector.setCommandId(currentCommandId);
//
//      desiredPose.setIncludingFrame(yoDesiredPose);
//      desiredPose.changeFrame(controlFrame);
//
//      desiredPose.getOrientation().normalizeAndLimitToPi();
//      linearFeedbackTermToPack.setIncludingFrame(desiredPose.getPosition());
//      desiredPose.getRotationVector(angularFeedbackTermToPack);
//
//      selectionMatrix.applyLinearSelection(linearFeedbackTermToPack);
//      selectionMatrix.applyAngularSelection(angularFeedbackTermToPack);
//
//      linearFeedbackTermToPack.clipToMaxNorm(positionGains.getMaximumProportionalError());
//      angularFeedbackTermToPack.clipToMaxNorm(orientationGains.getMaximumProportionalError());
//
//      yoErrorVector.setIncludingFrame(angularFeedbackTermToPack, linearFeedbackTermToPack);
//      yoErrorVector.changeFrame(controlFrame);
//      yoErrorVector.setCommandId(currentCommandId);
//      yoErrorOrientation.setRotationVectorIncludingFrame(yoErrorVector.getAngularPart());
//      yoErrorOrientation.setCommandId(currentCommandId);
//
//      if (linearGainsFrame != null)
//         linearFeedbackTermToPack.changeFrame(linearGainsFrame);
//      else
//         linearFeedbackTermToPack.changeFrame(controlFrame);
//
//      if (angularGainsFrame != null)
//         angularFeedbackTermToPack.changeFrame(angularGainsFrame);
//      else
//         angularFeedbackTermToPack.changeFrame(controlFrame);
//
//      positionGains.getProportionalGainMatrix(tempGainMatrix);
//      tempGainMatrix.transform(linearFeedbackTermToPack);
//
//      orientationGains.getProportionalGainMatrix(tempGainMatrix);
//      tempGainMatrix.transform(angularFeedbackTermToPack);
//
//      linearFeedbackTermToPack.changeFrame(controlFrame);
//      angularFeedbackTermToPack.changeFrame(controlFrame);
//   }
//
//   private final DMatrixRMaj tempLinearMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj tempAngularMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj tempMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj sqrtInertiaMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj sqrtProportionalGainMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj tempDiagDerivativeGainMatrix = new DMatrixRMaj(0, 0);
//   private final DMatrixRMaj tempDerivativeGainMatrix = new DMatrixRMaj(0, 0);
//   private final Matrix3D tempMatrix3D = new Matrix3D();
//
//   /**
//    * Computes the feedback term resulting from the error in linear velocity:<br>
//    * x<sub>FB</sub><sup>linear</sup> = kd<sup>linear</sup> * (xDot<sub>desired</sub> -
//    * xDot<sub>current</sub>)<br>
//    * x<sub>FB</sub><sup>angular</sup> = kd<sup>angular</sup> * (&omega;<sub>desired</sub> -
//    * &omega;<sub>current</sub>)
//    * <p>
//    * The desired velocity of the {@code controlFrame} relative to the {@code base} is obtained from
//    * {@link #yoDesiredVelocity}.
//    * </p>
//    * <p>
//    * This method also updates {@link #yoCurrentVelocity} and {@link #yoErrorVelocity}.
//    * </p>
//    *
//    * @param linearFeedbackTermToPack  the value of the feedback term x<sub>FB</sub><sup>linear</sup>.
//    *                                  Modified.
//    * @param angularFeedbackTermToPack the value of the feedback term x<sub>FB</sub><sup>angular</sup>.
//    *                                  Modified.
//    */
//   protected void computeDerivativeTerm(FrameVector3D linearFeedbackTermToPack, FrameVector3D angularFeedbackTermToPack)
//   {
//      ReferenceFrame trajectoryFrame = yoDesiredPose.getReferenceFrame();
//
//      controlFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
//      yoCurrentVelocity.setIncludingFrame(currentTwist.getAngularPart(), currentTwist.getLinearPart());
//      yoCurrentVelocity.changeFrame(trajectoryFrame);
//      yoCurrentVelocity.setCommandId(currentCommandId);
//
//      linearFeedbackTermToPack.setToZero(trajectoryFrame);
//      angularFeedbackTermToPack.setToZero(trajectoryFrame);
//      linearFeedbackTermToPack.sub(yoDesiredVelocity.getLinearPart(), yoCurrentVelocity.getLinearPart());
//      angularFeedbackTermToPack.sub(yoDesiredVelocity.getAngularPart(), yoCurrentVelocity.getAngularPart());
//      linearFeedbackTermToPack.changeFrame(controlFrame);
//      angularFeedbackTermToPack.changeFrame(controlFrame);
//      selectionMatrix.applyLinearSelection(linearFeedbackTermToPack);
//      selectionMatrix.applyAngularSelection(angularFeedbackTermToPack);
//
//      linearFeedbackTermToPack.clipToMaxNorm(positionGains.getMaximumDerivativeError());
//      angularFeedbackTermToPack.clipToMaxNorm(orientationGains.getMaximumDerivativeError());
//
//      linearFeedbackTermToPack.changeFrame(trajectoryFrame);
//      angularFeedbackTermToPack.changeFrame(trajectoryFrame);
//      yoErrorVelocity.setIncludingFrame(angularFeedbackTermToPack, linearFeedbackTermToPack);
//
//      if (linearVelocityErrorFilter != null)
//         linearVelocityErrorFilter.apply(linearFeedbackTermToPack, linearFeedbackTermToPack);
//
//      if (angularVelocityErrorFilter != null)
//         angularVelocityErrorFilter.apply(angularFeedbackTermToPack, angularFeedbackTermToPack);
//
//      yoErrorVelocity.setCommandId(currentCommandId);
//
//      linearFeedbackTermToPack.changeFrame(linearGainsFrame != null ? linearGainsFrame : controlFrame);
//      angularFeedbackTermToPack.changeFrame(angularGainsFrame != null ? angularGainsFrame : controlFrame);
//
//      positionGains.getDerivativeGainMatrix(tempGainMatrix);
//      orientationGains.getDerivativeGainMatrix(tempMatrix3D);
//      if (tempGainMatrix.containsNaN() || tempMatrix3D.containsNaN())
//      {
//         positionGains.getFullProportionalGainMatrix(tempLinearMatrix, 3);
//         orientationGains.getFullProportionalGainMatrix(tempAngularMatrix, 0);
//         CommonOps_DDRM.mult(tempLinearMatrix, tempLinearMatrix, tempMatrix);
//
//         sqrtProportionalGainMatrix.reshape(6,6);
//         sqrtInertiaMatrix.reshape(6,6);
//
//         MatrixMissingTools.sqrt(tempMatrix, sqrtProportionalGainMatrix);
//         tempMatrix.set(inverseInertiaMatrix);
//         CommonOps_DDRM.invert(tempMatrix);
//         MatrixMissingTools.sqrt(tempMatrix, sqrtInertiaMatrix);
//
//         CommonOps_DDRM.mult(sqrtInertiaMatrix, sqrtProportionalGainMatrix, tempDerivativeGainMatrix);
//         CommonOps_DDRM.multAdd(sqrtProportionalGainMatrix, sqrtInertiaMatrix, tempDerivativeGainMatrix);
//
//         tempDiagDerivativeGainMatrix.reshape(tempDerivativeGainMatrix.getNumRows(), tempDerivativeGainMatrix.getNumCols());
//         MatrixMissingTools.diagonal(tempDerivativeGainMatrix, tempDiagDerivativeGainMatrix);
//
//         tempMatrix.reshape(3, 3);
//         CommonOps_DDRM.extract(tempDiagDerivativeGainMatrix, 3, 6, 3, 6, tempMatrix, 0, 0);
//         tempMatrix3D.set(tempMatrix);
//         tempMatrix3D.transform(linearFeedbackTermToPack);
//         CommonOps_DDRM.extract(tempDiagDerivativeGainMatrix, 0, 3,0, 3, tempMatrix, 0, 0);
//         tempMatrix3D.set(tempMatrix);
//         tempMatrix3D.transform(angularFeedbackTermToPack);
//      }
//      else
//      {
//         tempGainMatrix.transform(linearFeedbackTermToPack);
//         tempMatrix3D.transform(angularFeedbackTermToPack);
//      }
//
//      linearFeedbackTermToPack.changeFrame(controlFrame);
//      angularFeedbackTermToPack.changeFrame(controlFrame);
//   }
//
//   /**
//    * Computes and adds the bias acceleration resulting from the combination of the current linear and
//    * angular velocity of the control frame.
//    * <p>
//    * This is needed when going from a linear acceleration expressed in an inertial frame to a moving
//    * frame attached to the end-effector.
//    * </p>
//    * <p>
//    * Intuitively, the Coriolis acceleration only appears when measuring the acceleration from a moving
//    * frame, here a frame attache to the end-effector.
//    * </p>
//    *
//    * @param linearAccelerationToModify the linear acceleration vector to which the bias is to be
//    *                                   subtracted. Its frame is changed to {@code controlFrame}.
//    *                                   Modified.
//    */
//   protected void addCoriolisAcceleration(FrameVector3D linearAccelerationToModify)
//   {
//      controlFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
//      currentAngularVelocity.setIncludingFrame(currentTwist.getAngularPart());
//      currentLinearVelocity.setIncludingFrame(currentTwist.getLinearPart());
//
//      biasLinearAcceleration.setToZero(controlFrame);
//      biasLinearAcceleration.cross(currentLinearVelocity, currentAngularVelocity);
//      linearAccelerationToModify.changeFrame(controlFrame);
//      linearAccelerationToModify.add(biasLinearAcceleration);
//   }
//
//   /**
//    * Computes and subtracts the bias acceleration resulting from the combination of the current linear
//    * and angular velocity of the control frame.
//    * <p>
//    * This is needed when going from a linear acceleration expressed in a moving frame attached to the
//    * end-effector to an inertial frame.
//    * </p>
//    * <p>
//    * Intuitively, the Coriolis acceleration only appears when measuring the acceleration from a moving
//    * frame, here a frame attache to the end-effector.
//    * </p>
//    *
//    * @param linearAccelerationToModify the linear acceleration vector to which the bias is to be
//    *                                   added. Its frame is changed to {@code worldFrame}. Modified.
//    */
//   protected void subtractCoriolisAcceleration(FrameVector3D linearAccelerationToModify)
//   {
//      ReferenceFrame originalFrame = linearAccelerationToModify.getReferenceFrame();
//      controlFrame.getTwistRelativeToOther(controlBaseFrame, currentTwist);
//      currentAngularVelocity.setIncludingFrame(currentTwist.getAngularPart());
//      currentLinearVelocity.setIncludingFrame(currentTwist.getLinearPart());
//
//      biasLinearAcceleration.setToZero(controlFrame);
//      biasLinearAcceleration.cross(currentLinearVelocity, currentAngularVelocity);
//      linearAccelerationToModify.changeFrame(controlFrame);
//      linearAccelerationToModify.sub(biasLinearAcceleration);
//      linearAccelerationToModify.changeFrame(originalFrame);
//   }
//
//   /**
//    * This function can add post-processing to the desired accelerations from the inverse dynamics
//    * calculator. This is done before the coriolis acceleration is calculated. The default
//    * implementation is a no-op.
//    *
//    * @param controlFrame
//    * @param desiredAngularAcceleration
//    * @param desiredLinearAcceleration
//    */
//   protected void proccessInverseDynamicsDesiredAcceleration(MovingReferenceFrame controlFrame,
//                                                             FrameVector3D desiredAngularAcceleration,
//                                                             FrameVector3D desiredLinearAcceleration)
//   {
//   }
//
//   @Override
//   public boolean isEnabled()
//   {
//      return isEnabled.getBooleanValue();
//   }
//
//   @Override
//   public SpatialAccelerationCommand getInverseDynamicsOutput()
//   {
//      if (!isEnabled())
//         throw new FeedbackControllerException("This controller is disabled.");
//      return inverseDynamicsOutput;
//   }
//
//   @Override
//   public SpatialVelocityCommand getInverseKinematicsOutput()
//   {
//      if (!isEnabled())
//         throw new FeedbackControllerException("This controller is disabled.");
//      return inverseKinematicsOutput;
//   }
//
//   @Override
//   public VirtualModelControlCommand<?> getVirtualModelControlOutput()
//   {
//      if (!isEnabled())
//         throw new FeedbackControllerException("This controller is disabled.");
//      return (isRootBody) ? virtualModelControlRootOutput : virtualModelControlOutput;
//   }
//
//   public int getControllerIndex()
//   {
//      return controllerIndex;
//   }
//
//   @Override
//   public String toString()
//   {
//      return getClass().getSimpleName() + ": endEffector = " + endEffector;
//   }
//}
