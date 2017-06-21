package us.ihmc.exampleSimulations.controllerCore.robotArmWithFixedBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import us.ihmc.commonWalkingControlModules.configurations.JointPrivilegedConfigurationParameters;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControlCoreToolbox;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCore;
import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCoreMode;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreOutput;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommandList;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.OrientationFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.PointFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.SpatialFeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.inverseKinematics.PrivilegedConfigurationCommand.PrivilegedConfigurationOption;
import us.ihmc.commonWalkingControlModules.controllerCore.command.lowLevel.LowLevelOneDoFJointDesiredDataHolderReadOnly;
import us.ihmc.commonWalkingControlModules.momentumBasedController.optimization.ControllerCoreOptimizationSettings;
import us.ihmc.commonWalkingControlModules.trajectories.StraightLinePoseTrajectoryGenerator;
import us.ihmc.exampleSimulations.controllerCore.ControllerCoreModeChangedListener;
import us.ihmc.exampleSimulations.controllerCore.RobotArmControllerCoreOptimizationSettings;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicCoordinateSystem;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.robotics.controllers.OrientationPIDGainsInterface;
import us.ihmc.robotics.controllers.PositionPIDGainsInterface;
import us.ihmc.robotics.controllers.YoSymmetricSE3PIDGains;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.BooleanYoVariable;
import us.ihmc.yoVariables.variable.DoubleYoVariable;
import us.ihmc.yoVariables.variable.EnumYoVariable;
import us.ihmc.robotics.geometry.FrameOrientation;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePose;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.math.frames.YoFrameOrientation;
import us.ihmc.robotics.math.frames.YoFramePoint;
import us.ihmc.robotics.referenceFrames.CenterOfMassReferenceFrame;
import us.ihmc.robotics.referenceFrames.ReferenceFrame;
import us.ihmc.robotics.robotController.RobotController;
import us.ihmc.robotics.screwTheory.InverseDynamicsJoint;
import us.ihmc.robotics.screwTheory.OneDoFJoint;
import us.ihmc.robotics.screwTheory.RigidBody;
import us.ihmc.robotics.screwTheory.ScrewTools;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.robotics.screwTheory.SelectionMatrix6D;
import us.ihmc.sensorProcessing.sensorProcessors.RobotJointLimitWatcher;

public class FixedBaseRobotArmController implements RobotController
{
   private static final boolean USE_PRIVILEGED_CONFIGURATION = false;
   private static final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();

   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final FixedBaseRobotArm robotArm;
   private final DoubleYoVariable yoTime;
   private final CenterOfMassReferenceFrame centerOfMassFrame;

   public enum FeedbackControlType
   {
      SPATIAL, LINEAR_ANGULAR_SEPARATE
   };

   private final EnumYoVariable<WholeBodyControllerCoreMode> controllerCoreMode = new EnumYoVariable<>("controllerCoreMode", registry,
                                                                                                       WholeBodyControllerCoreMode.class);
   private final AtomicBoolean controllerCoreModeHasChanged = new AtomicBoolean(false);
   private final List<ControllerCoreModeChangedListener> controllerModeListeners = new ArrayList<>();
   private final EnumYoVariable<FeedbackControlType> feedbackControlToUse = new EnumYoVariable<>("feedbackControlToUse", registry, FeedbackControlType.class,
                                                                                                 false);

   private final PointFeedbackControlCommand handPointCommand = new PointFeedbackControlCommand();
   private final OrientationFeedbackControlCommand handOrientationCommand = new OrientationFeedbackControlCommand();
   private final SpatialFeedbackControlCommand handSpatialCommand = new SpatialFeedbackControlCommand();
   private final ControllerCoreCommand controllerCoreCommand = new ControllerCoreCommand(WholeBodyControllerCoreMode.INVERSE_DYNAMICS);

   private final WholeBodyControllerCore controllerCore;

   private final DoubleYoVariable handWeight = new DoubleYoVariable("handWeight", registry);
   private final YoSymmetricSE3PIDGains handPositionGains = new YoSymmetricSE3PIDGains("handPosition", registry);
   private final YoSymmetricSE3PIDGains handOrientationGains = new YoSymmetricSE3PIDGains("handOrientation", registry);
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

   private final BooleanYoVariable setRandomConfiguration = new BooleanYoVariable("setRandomConfiguration", registry);

   public FixedBaseRobotArmController(FixedBaseRobotArm robotArm, double controlDT, YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      this.robotArm = robotArm;

      controllerCoreMode.set(WholeBodyControllerCoreMode.INVERSE_DYNAMICS);
      controllerCoreMode.addVariableChangedListener(v -> controllerCoreModeHasChanged.set(true));

      yoTime = robotArm.getYoTime();
      double gravityZ = robotArm.getGravity();
      RigidBody hand = robotArm.getHand();
      RigidBody elevator = robotArm.getElevator();
      InverseDynamicsJoint[] controlledJoints = ScrewTools.computeSupportAndSubtreeJoints(elevator);
      centerOfMassFrame = new CenterOfMassReferenceFrame("centerOfMassFrame", worldFrame, elevator);

      ControllerCoreOptimizationSettings optimizationSettings = new RobotArmControllerCoreOptimizationSettings();

      WholeBodyControlCoreToolbox controlCoreToolbox = new WholeBodyControlCoreToolbox(controlDT, gravityZ, null, controlledJoints, centerOfMassFrame,
                                                                                       optimizationSettings, yoGraphicsListRegistry, registry);

      if (USE_PRIVILEGED_CONFIGURATION)
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

   public void registerControllerCoreModeChangedListener(ControllerCoreModeChangedListener listener)
   {
      controllerModeListeners.add(listener);
   }

   @Override
   public void initialize()
   {
      robotArm.updateIDRobot();

      handWeight.set(1.0);

      handPositionGains.setProportionalGain(100.0);
      handPositionGains.setDampingRatio(1.0);
      handPositionGains.createDerivativeGainUpdater(true);

      handOrientationGains.setProportionalGain(100.0);
      handOrientationGains.setDampingRatio(1.0);
      handOrientationGains.createDerivativeGainUpdater(true);

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
      robotArm.updateControlFrameAcceleration();
      robotArm.updateIDRobot();
      centerOfMassFrame.update();

      updateTrajectory();
      updateFeedbackCommands();

      controllerCoreCommand.clear();
      if (feedbackControlToUse.getEnumValue() == FeedbackControlType.SPATIAL)
      {
         controllerCoreCommand.addFeedbackControlCommand(handSpatialCommand);
      }
      else
      {
         controllerCoreCommand.addFeedbackControlCommand(handPointCommand);
         controllerCoreCommand.addFeedbackControlCommand(handOrientationCommand);
      }
      if (USE_PRIVILEGED_CONFIGURATION)
         controllerCoreCommand.addInverseDynamicsCommand(privilegedConfigurationCommand);
      controllerCore.submitControllerCoreCommand(controllerCoreCommand);
      controllerCore.compute();

      ControllerCoreOutput controllerCoreOutput = controllerCore.getControllerCoreOutput();
      LowLevelOneDoFJointDesiredDataHolderReadOnly lowLevelOneDoFJointDesiredDataHolder = controllerCoreOutput.getLowLevelOneDoFJointDesiredDataHolder();

      if (controllerCoreMode.getEnumValue() == WholeBodyControllerCoreMode.OFF
            || controllerCoreMode.getEnumValue() == WholeBodyControllerCoreMode.VIRTUAL_MODEL)
         controllerCoreMode.set(WholeBodyControllerCoreMode.INVERSE_DYNAMICS);

      if (controllerCoreModeHasChanged.getAndSet(false))
         controllerModeListeners.forEach(listener -> listener.controllerCoreModeHasChanged(controllerCoreMode.getEnumValue()));

      controllerCoreCommand.setControllerCoreMode(controllerCoreMode.getEnumValue());

      if (controllerCoreMode.getEnumValue() == WholeBodyControllerCoreMode.INVERSE_DYNAMICS)
         robotArm.updateSCSRobotJointTaus(lowLevelOneDoFJointDesiredDataHolder);
      else
         robotArm.updateSCSRobotJointConfiguration(lowLevelOneDoFJointDesiredDataHolder);

      if (setRandomConfiguration.getBooleanValue())
      {
         robotArm.setRandomConfiguration();
         setRandomConfiguration.set(false);
      }

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
      handPointCommand.setGains(handPositionGains);
      handPointCommand.setSelectionMatrix(computeLinearSelectionMatrix());
      handPointCommand.set(position, linearVelocity, linearAcceleration);

      handOrientationCommand.setWeightForSolver(handWeight.getDoubleValue());
      handOrientationCommand.setGains(handOrientationGains);
      handOrientationCommand.setSelectionMatrix(computeAngularSelectionMatrix());
      handOrientationCommand.set(orientation, angularVelocity, angularAcceleration);

      handSpatialCommand.setControlFrameFixedInEndEffector(controlFramePose);
      handSpatialCommand.setWeightForSolver(handWeight.getDoubleValue());
      handSpatialCommand.setGains((PositionPIDGainsInterface) handPositionGains);
      handSpatialCommand.setGains((OrientationPIDGainsInterface) handOrientationGains);
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

   private SelectionMatrix3D computeLinearSelectionMatrix()
   {
      SelectionMatrix3D selectionMatrix = new SelectionMatrix3D();

      selectionMatrix.selectXAxis(controlLinearX.getBooleanValue());
      selectionMatrix.selectYAxis(controlLinearY.getBooleanValue());
      selectionMatrix.selectZAxis(controlLinearZ.getBooleanValue());

      return selectionMatrix;
   }

   private SelectionMatrix3D computeAngularSelectionMatrix()
   {
      SelectionMatrix3D selectionMatrix = new SelectionMatrix3D();

      selectionMatrix.selectXAxis(controlAngularX.getBooleanValue());
      selectionMatrix.selectYAxis(controlAngularY.getBooleanValue());
      selectionMatrix.selectZAxis(controlAngularZ.getBooleanValue());

      return selectionMatrix;
   }

   private SelectionMatrix6D computeSpatialSelectionMatrix()
   {
      SelectionMatrix6D selectionMatrix = new SelectionMatrix6D();

      selectionMatrix.selectAngularX(controlAngularX.getBooleanValue());
      selectionMatrix.selectAngularY(controlAngularY.getBooleanValue());
      selectionMatrix.selectAngularZ(controlAngularZ.getBooleanValue());

      selectionMatrix.selectLinearX(controlLinearX.getBooleanValue());
      selectionMatrix.selectLinearY(controlLinearY.getBooleanValue());
      selectionMatrix.selectLinearZ(controlLinearZ.getBooleanValue());

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
