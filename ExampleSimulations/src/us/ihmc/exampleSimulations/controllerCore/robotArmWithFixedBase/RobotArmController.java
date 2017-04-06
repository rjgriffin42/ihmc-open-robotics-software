package us.ihmc.exampleSimulations.controllerCore.robotArmWithFixedBase;

import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.commonWalkingControlModules.configurations.JointPrivilegedConfigurationParameters;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCore;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand.PrivilegedConfigurationOption;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolderReadOnly;
import us.ihmc.commonWalkingControlModules.momentumBasedController.GeometricJacobianHolder;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.ControllerCoreOptimizationSettings;
import us.ihmc.commonWalkingControlModules.trajectories.StraightLinePoseTrajectoryGenerator;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicCoordinateSystem;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.controllers.SE3PIDGainsInterface;
import us.ihmc.robotics.controllers.YoSymmetricSE3PIDGains;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.dataStructures.variable.BooleanYoVariable;
import us.ihmc.robotics.dataStructures.variable.DoubleYoVariable;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.linearAlgebra.MatrixTools;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotController.RobotController;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.TwistCalculator;
import us.ihmc.sensorProcessing.sensorProcessors.RobotJointLimitWatcher;

public class RobotArmController implements RobotController
{
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final RobotArm robotArm;
   private final DoubleYoVariable yoTime;
   private final CenterOfMassReferenceFrame centerOfMassFrame;
   private final TwistCalculator twistCalculator;
   private final GeometricJacobianHolder geometricJacobianHolder;

   public enum FeedbackControlType
   {
      SPATIAL, LINEAR_ANGULAR_SEPARATE
   };

   private final EnumYoVariable<FeedbackControlType> feedbackControlToUse = new EnumYoVariable<>("feedbackControlToUse", registry, FeedbackControlType.class,
                                                                                                 false);

   private final PointFeedbackControlCommand handPointCommand = new PointFeedbackControlCommand();
   private final OrientationFeedbackControlCommand handOrientationCommand = new OrientationFeedbackControlCommand();
   private final SpatialFeedbackControlCommand handSpatialCommand = new SpatialFeedbackControlCommand();
   private final ControllerCoreCommand controllerCoreCommand = new ControllerCoreCommand(WholeBodyControllerCoreMode.INVERSE_DYNAMICS);

   private final WholeBodyControllerCore controllerCore;

   private final DoubleYoVariable handWeight = new DoubleYoVariable("handWeight", registry);
   private final YoSymmetricSE3PIDGains handGains = new YoSymmetricSE3PIDGains("hand", registry);
   private final YoFramePoint handTargetPosition = new YoFramePoint("handTarget", worldFrame, registry);

   private final YoFrameOrientation handTargetOrientation = new YoFrameOrientation("handTarget", worldFrame, registry);
   private final BooleanYoVariable goToTarget = new BooleanYoVariable("goToTarget", registry);
   private final DoubleYoVariable trajectoryDuration = new DoubleYoVariable("handTrajectoryDuration", registry);
   private final DoubleYoVariable trajectoryStartTime = new DoubleYoVariable("handTrajectoryStartTime", registry);

   private final StraightLinePoseTrajectoryGenerator trajectory;

   private final BooleanYoVariable controlLinearX = new BooleanYoVariable("controlLinearX", registry);
   private final BooleanYoVariable controlLinearY = new BooleanYoVariable("controlLinearY", registry);
   private final BooleanYoVariable controlLinearZ = new BooleanYoVariable("controlLinearZ", registry);
   private final BooleanYoVariable controlAngularX = new BooleanYoVariable("controlAngularX", registry);
   private final BooleanYoVariable controlAngularY = new BooleanYoVariable("controlAngularY", registry);
   private final BooleanYoVariable controlAngularZ = new BooleanYoVariable("controlAngularZ", registry);

   private final PrivilegedConfigurationCommand privilegedConfigurationCommand = new PrivilegedConfigurationCommand();
   private final RobotJointLimitWatcher robotJointLimitWatcher;

   public RobotArmController(RobotArm robotArm, double controlDT, WholeBodyControllerCoreMode controlMode, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.robotArm = robotArm;
      controllerCoreCommand.setControllerCoreMode(controlMode);

      yoTime = robotArm.getYoTime();
      double gravityZ = robotArm.getGravity();
      RigidBody hand = robotArm.getHand();
      RigidBody elevator = robotArm.getElevator();
      InverseDynamicsJoint[] controlledJoints = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMassFrame", worldFrame, elevator);
      twistCalculator = new TwistCalculator(worldFrame, elevator);
      geometricJacobianHolder = new GeometricJacobianHolder();

      ControllerCoreOptimizationSettings optimizationSettings = new RobotArmControllerCoreOptimizationSettings();

      WholeBodyControlCoreToolbox controlCoreToolbox = new WholeBodyControlCoreToolbox(controlDT, gravityZ, null, controlledJoints, centerOfMassFrame,
                                                                                       twistCalculator, geometricJacobianHolder, optimizationSettings,
                                                                                       yoGraphicsListRegistry, registry);

      controlCoreToolbox.setJointPrivilegedConfigurationParameters(new JointPrivilegedConfigurationParameters());

      controlCoreToolbox.setupForInverseDynamicsSolver(new ArrayList<>());
      controlCoreToolbox.setupForInverseKinematicsSolver();

      FeedbackControlCommandList allPossibleCommands = new FeedbackControlCommandList();

      handPointCommand.set(elevator, hand);
      handOrientationCommand.set(elevator, hand);
      handSpatialCommand.set(elevator, hand);
      allPossibleCommands.addCommand(handPointCommand);
      allPossibleCommands.addCommand(handOrientationCommand);
      allPossibleCommands.addCommand(handSpatialCommand);

      controllerCore = new WholeBodyControllerCore(controlCoreToolbox, allPossibleCommands, registry);

      yoGraphicsListRegistry.registerYoGraphic("desireds", new YoGraphicCoordinateSystem("targetFrame", handTargetPosition, handTargetOrientation, 0.15,
                                                                                         YoAppearance.Red()));

      privilegedConfigurationCommand.setPrivilegedConfigurationOption(PrivilegedConfigurationOption.AT_ZERO);
      privilegedConfigurationCommand.addJoint(robotArm.getElbowPitch(), Math.PI / 3.0);

      trajectory = new StraightLinePoseTrajectoryGenerator("handTrajectory", false, worldFrame, registry, true, yoGraphicsListRegistry);

      robotJointLimitWatcher = new RobotJointLimitWatcher(ScrewTools.filterJoints(controlledJoints, OneDoFJoint.class));
      registry.addChild(robotJointLimitWatcher.getYoVariableRegistry());

      initialize();
   }

   @Override
   public void initialize()
   {
      robotArm.updateIDRobot();

      handWeight.set(1.0);
      handGains.setProportionalGain(100.0);
      handGains.setDampingRatio(1.0);
      handGains.createDerivativeGainUpdater(true);

      FramePoint initialPosition = new FramePoint(robotArm.getHandControlFrame());
      initialPosition.changeFrame(worldFrame);
      FrameOrientation initialOrientation = new FrameOrientation(robotArm.getHandControlFrame());
      initialOrientation.changeFrame(worldFrame);

      handTargetPosition.setAndMatchFrame(initialPosition);
      handTargetOrientation.setAndMatchFrame(initialOrientation);

      trajectoryDuration.set(0.5);
      trajectory.setInitialPose(initialPosition, initialOrientation);
      trajectory.setFinalPose(initialPosition, initialOrientation);
      trajectory.setTrajectoryTime(trajectoryDuration.getDoubleValue());

      controlLinearX.set(true);
      controlLinearY.set(true);
      controlLinearZ.set(true);
      controlAngularX.set(true);
      controlAngularY.set(true);
      controlAngularZ.set(true);
      trajectory.showVisualization();
   }

   private final FramePoint position = new FramePoint();
   private final FrameVector linearVelocity = new FrameVector();
   private final FrameVector linearAcceleration = new FrameVector();
   private final FrameOrientation orientation = new FrameOrientation();
   private final FrameVector angularVelocity = new FrameVector();
   private final FrameVector angularAcceleration = new FrameVector();

   @Override
   public void doControl()
   {
      robotArm.updateIDRobot();
      centerOfMassFrame.update();
      twistCalculator.compute();
      geometricJacobianHolder.compute();

      updateTrajectory();
      updateFeedbackCommands();

      controllerCoreCommand.clear();
      if (feedbackControlToUse.getEnumValue() == FeedbackControlType.SPATIAL)
         controllerCoreCommand.addFeedbackControlCommand(handSpatialCommand);
      else
      {
         controllerCoreCommand.addFeedbackControlCommand(handPointCommand);
         controllerCoreCommand.addFeedbackControlCommand(handOrientationCommand);
      }
      controllerCoreCommand.addInverseDynamicsCommand(privilegedConfigurationCommand);
      controllerCore.submitControllerCoreCommand(controllerCoreCommand);
      controllerCore.compute();

      LowLevelOneDoFJointDesiredDataHolderReadOnly lowLevelOneDoFJointDesiredDataHolder = controllerCore.getControllerCoreOutput().getLowLevelOneDoFJointDesiredDataHolder();

      if (controllerCoreCommand.getControllerCoreMode() == WholeBodyControllerCoreMode.INVERSE_KINEMATICS)
         robotArm.updateSCSRobotJointConfiguration(lowLevelOneDoFJointDesiredDataHolder);
      else
         robotArm.updateSCSRobotJointTaus(lowLevelOneDoFJointDesiredDataHolder);

      robotJointLimitWatcher.doControl();
   }

   public void updateFeedbackCommands()
   {
      FramePose controlFramePose = new FramePose(robotArm.getHandControlFrame());
      controlFramePose.changeFrame(robotArm.getHand().getBodyFixedFrame());

      trajectory.getAngularData(orientation, angularVelocity, angularAcceleration);
      trajectory.getLinearData(position, linearVelocity, linearAcceleration);

      handPointCommand.setBodyFixedPointToControl(controlFramePose.getFramePointCopy());
      handPointCommand.setWeightForSolver(handWeight.getDoubleValue());
      handPointCommand.setGains(handGains);
      handPointCommand.setSelectionMatrix(computeLinearSelectionMatrix());
      handPointCommand.set(position, linearVelocity, linearAcceleration);

      handOrientationCommand.setWeightForSolver(handWeight.getDoubleValue());
      handOrientationCommand.setGains(handGains);
      handOrientationCommand.setSelectionMatrix(computeAngularSelectionMatrix());
      handOrientationCommand.set(orientation, angularVelocity, angularAcceleration);

      handSpatialCommand.setControlFrameFixedInEndEffector(controlFramePose);
      handSpatialCommand.setWeightForSolver(handWeight.getDoubleValue());
      handSpatialCommand.setGains((SE3PIDGainsInterface) handGains);
      handSpatialCommand.setSelectionMatrix(computeSpatialSelectionMatrix());
      handSpatialCommand.set(position, linearVelocity, linearAcceleration);
      handSpatialCommand.set(orientation, angularVelocity, angularAcceleration);
   }

   public void updateTrajectory()
   {
      if (goToTarget.getBooleanValue())
      {
         FramePoint initialPosition = new FramePoint(robotArm.getHandControlFrame());
         initialPosition.changeFrame(worldFrame);
         FrameOrientation initialOrientation = new FrameOrientation(robotArm.getHandControlFrame());
         initialOrientation.changeFrame(worldFrame);
         trajectory.setInitialPose(initialPosition, initialOrientation);
         FramePoint finalPosition = new FramePoint();
         FrameOrientation finalOrientation = new FrameOrientation();
         handTargetPosition.getFrameTupleIncludingFrame(finalPosition);
         handTargetOrientation.getFrameOrientationIncludingFrame(finalOrientation);
         trajectory.setFinalPose(finalPosition, finalOrientation);
         trajectory.setTrajectoryTime(trajectoryDuration.getDoubleValue());
         trajectory.initialize();
         trajectoryStartTime.set(yoTime.getDoubleValue());
         goToTarget.set(false);
      }

      trajectory.compute(yoTime.getDoubleValue() - trajectoryStartTime.getDoubleValue());
   }

   private DenseMatrix64F computeLinearSelectionMatrix()
   {
      DenseMatrix64F selectionMatrix = CommonOps.identity(6);
      if (!controlLinearZ.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 5);
      if (!controlLinearY.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 4);
      if (!controlLinearX.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 3);

      MatrixTools.removeRow(selectionMatrix, 2);
      MatrixTools.removeRow(selectionMatrix, 1);
      MatrixTools.removeRow(selectionMatrix, 0);

      return selectionMatrix;
   }

   private DenseMatrix64F computeAngularSelectionMatrix()
   {
      DenseMatrix64F selectionMatrix = CommonOps.identity(6);
      MatrixTools.removeRow(selectionMatrix, 5);
      MatrixTools.removeRow(selectionMatrix, 4);
      MatrixTools.removeRow(selectionMatrix, 3);

      if (!controlAngularZ.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 2);
      if (!controlAngularY.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 1);
      if (!controlAngularX.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 0);

      return selectionMatrix;
   }

   private DenseMatrix64F computeSpatialSelectionMatrix()
   {
      DenseMatrix64F selectionMatrix = CommonOps.identity(6);
      if (!controlLinearZ.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 5);
      if (!controlLinearY.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 4);
      if (!controlLinearX.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 3);

      if (!controlAngularZ.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 2);
      if (!controlAngularY.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 1);
      if (!controlAngularX.getBooleanValue())
         MatrixTools.removeRow(selectionMatrix, 0);

      return selectionMatrix;
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return name;
   }

   @Override
   public String getDescription()
   {
      return name;
   }

   public YoFramePoint getHandTargetPosition()
   {
      return handTargetPosition;
   }

   public YoFrameOrientation getHandTargetOrientation()
   {
      return handTargetOrientation;
   }

   public BooleanYoVariable getGoToTarget()
   {
      return goToTarget;
   }
}
