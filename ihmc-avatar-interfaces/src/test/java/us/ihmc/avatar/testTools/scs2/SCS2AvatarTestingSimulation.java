package us.ihmc.avatar.testTools.scs2;

import static us.ihmc.robotics.Assert.assertTrue;
import static us.ihmc.robotics.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.mutable.MutableInt;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.scs2.SCS2AvatarSimulation;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelHumanoidControllerFactory;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.communication.net.ObjectConsumer;
import us.ihmc.euclid.geometry.interfaces.BoundingBox3DReadOnly;
import us.ihmc.euclid.orientation.interfaces.Orientation3DReadOnly;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tools.RotationMatrixTools;
import us.ihmc.euclid.tuple2D.Point2D;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DReadOnly;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphic;
import us.ihmc.graphicsDescription.yoGraphics.YoGraphicsListRegistry;
import us.ihmc.humanoidBehaviors.behaviors.scripts.engine.ScriptBasedControllerCommandGenerator;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.multiBodySystem.interfaces.FloatingJointBasics;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.controllers.ControllerFailureListener;
import us.ihmc.ros2.ROS2Node;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.scs2.definition.robot.RobotDefinition;
import us.ihmc.scs2.definition.state.interfaces.SixDoFJointStateBasics;
import us.ihmc.scs2.definition.visual.VisualDefinition;
import us.ihmc.scs2.definition.yoGraphic.YoGraphicDefinition;
import us.ihmc.scs2.session.SessionMode;
import us.ihmc.scs2.session.tools.SCS1GraphicConversionTools;
import us.ihmc.scs2.sessionVisualizer.jfx.SessionVisualizerControls;
import us.ihmc.scs2.sessionVisualizer.jfx.SessionVisualizerIOTools;
import us.ihmc.scs2.sessionVisualizer.jfx.tools.JavaFXMissingTools;
import us.ihmc.scs2.simulation.SimulationSession;
import us.ihmc.scs2.simulation.SimulationSessionControls;
import us.ihmc.scs2.simulation.robot.Robot;
import us.ihmc.scs2.simulation.robot.RobotInterface;
import us.ihmc.sensorProcessing.frames.CommonHumanoidReferenceFrames;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools.VideoAndDataExporter;
import us.ihmc.simulationconstructionset.util.RobotController;
import us.ihmc.simulationconstructionset.util.simulationTesting.SimulationTestingParameters;
import us.ihmc.yoVariables.listener.YoVariableChangedListener;
import us.ihmc.yoVariables.registry.YoNamespace;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.registry.YoVariableHolder;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoVariable;

public class SCS2AvatarTestingSimulation implements YoVariableHolder
{
   private static final double CAMERA_PITCH_FROM_ROBOT = Math.toRadians(-15.0);
   private static final double CAMERA_YAW_FROM_ROBOT = Math.toRadians(15.0);
   private static final double CAMERA_DISTANCE_FROM_ROBOT = 6.0;

   private final SCS2AvatarSimulation avatarSimulation;

   private final ControllerFailureListener exceptionOnFailureListener = fallingDirection ->
   {
      throw new ControllerFailureRuntimeException("Controller has failed!");
   };

   private ROS2Node ros2Node;
   @SuppressWarnings("rawtypes")
   private Map<Class<?>, IHMCROS2Publisher> defaultControllerPublishers;

   private final AtomicReference<Throwable> lastThrowable = new AtomicReference<>();

   private final AtomicBoolean isVisualizerGoingDown = new AtomicBoolean(false);

   private boolean createVideo = false;
   private boolean keepSCSUp = false;

   /**
    * Constructors for setting up a custom simulation environment for which the default factory isn't
    * suited.
    * 
    * @param simulationSession      the simulation session to wrap.
    * @param robotModel             the robot model for enabling convenience methods. Can be
    *                               {@code null}.
    * @param fullRobotModel         the robot to be associated as the controller robot for enabling
    *                               convenience methods. Can be {@code null}.
    * @param yoGraphicsListRegistry graphics to be displayed in the GUI. Can be {@code null}.
    */
   public SCS2AvatarTestingSimulation(SimulationSession simulationSession,
                                      DRCRobotModel robotModel,
                                      FullHumanoidRobotModel fullRobotModel,
                                      YoGraphicsListRegistry yoGraphicsListRegistry,
                                      SimulationTestingParameters parameters)
   {
      this(new SCS2AvatarSimulation());
      avatarSimulation.setSimulationSession(simulationSession);
      avatarSimulation.setRobot(simulationSession.getPhysicsEngine().getRobots().get(0));
      if (robotModel != null)
         avatarSimulation.setRobotModel(robotModel);
      if (fullRobotModel != null)
         avatarSimulation.setFullHumanoidRobotModel(fullRobotModel);
      if (yoGraphicsListRegistry != null)
         simulationSession.getYoGraphicDefinitions().addAll(SCS1GraphicConversionTools.toYoGraphicDefinitions(yoGraphicsListRegistry));

      if (parameters != null)
      {
         avatarSimulation.setShowGUI(parameters.getCreateGUI());
         simulationSession.initializeBufferSize(parameters.getDataBufferSize());
         setCreateVideo(parameters.getCreateSCSVideos());
         setKeepSCSUp(parameters.getKeepSCSUp());
      }
   }

   /**
    * Constructor used by the factory {@link SCS2AvatarTestingSimulationFactory}.
    * 
    * @param avatarSimulation the simulation setup.
    */
   public SCS2AvatarTestingSimulation(SCS2AvatarSimulation avatarSimulation)
   {
      this.avatarSimulation = avatarSimulation;
   }

   public void setCreateVideo(boolean createVideo)
   {
      this.createVideo = createVideo;
   }

   public void setKeepSCSUp(boolean keepSCSUp)
   {
      this.keepSCSUp = keepSCSUp;
   }

   public void start()
   {
      start(true);
   }

   public void start(boolean cameraTracksPelvis)
   {
      getSimulationSessionControls().addSimulationThrowableListener(lastThrowable::set);
      enableExceptionControllerFailure();

      // Necessary to be able to restart the GUI during a series of tests.
      avatarSimulation.setSystemExitOnDestroy(false);
      avatarSimulation.setJavaFXThreadImplicitExit(false);

      avatarSimulation.start();

      if (getSessionVisualizerControls() != null)
      {
         getSessionVisualizerControls().waitUntilFullyUp();
         getSessionVisualizerControls().addVisualizerShutdownListener(() -> isVisualizerGoingDown.set(true));

         SixDoFJointStateBasics initialRootJointState = (SixDoFJointStateBasics) getRobotDefinition().getRootJointDefinitions().get(0).getInitialJointState();
         if (initialRootJointState != null)
            initializeCamera(initialRootJointState.getOrientation(), initialRootJointState.getPosition());
         if (cameraTracksPelvis)
            requestCameraRigidBodyTracking(getRobotModel().getSimpleRobotName(), getRobot().getFloatingRootJoint().getSuccessor().getName());
      }

      // We park the simulation thread assuming that the calling test will need to run the simulation in their own thread to keep things synchronous.
      getSimulationSession().stopSessionThread();
   }

   public void enableExceptionControllerFailure()
   {
      if (getHighLevelHumanoidControllerFactory() != null)
         getHighLevelHumanoidControllerFactory().attachControllerFailureListener(exceptionOnFailureListener);
   }

   public void disableExceptionControllerFailure()
   {
      if (getHighLevelHumanoidControllerFactory() != null)
         getHighLevelHumanoidControllerFactory().detachControllerFailureListener(exceptionOnFailureListener);
   }

   private void initializeCamera(Orientation3DReadOnly robotOrientation, Tuple3DReadOnly robotPosition)
   {
      Point3D focusPosition = new Point3D(robotPosition);
      Point3D cameraPosition = new Point3D(10, 0, 0);
      RotationMatrixTools.applyPitchRotation(CAMERA_PITCH_FROM_ROBOT, cameraPosition, cameraPosition);
      RotationMatrixTools.applyYawRotation(CAMERA_YAW_FROM_ROBOT, cameraPosition, cameraPosition);
      RotationMatrixTools.applyYawRotation(robotOrientation.getYaw(), cameraPosition, cameraPosition);
      cameraPosition.scale(CAMERA_DISTANCE_FROM_ROBOT / cameraPosition.distanceFromOrigin());
      cameraPosition.add(focusPosition);

      setCamera(focusPosition, cameraPosition);
   }

   // Simulation controls:
   /**
    * Adds a terminal condition that will be used in the subsequent simulations to determine when to
    * stop the simulation.
    * <p>
    * The condition can be removed with {@link #removeSimulationTerminalCondition(BooleanSupplier)}.
    * </p>
    * 
    * @param terminalCondition the new condition used to terminate future simulation.
    */
   public void addSimulationTerminalCondition(BooleanSupplier terminalCondition)
   {
      getSimulationSessionControls().addExternalTerminalCondition(terminalCondition);
   }

   /**
    * Removes a terminal simulation condition that was previously registered.
    * 
    * @param terminalCondition the condition to remove.
    */
   public void removeSimulationTerminalCondition(BooleanSupplier terminalCondition)
   {
      getSimulationSessionControls().removeExternalTerminalCondition(terminalCondition);
   }

   /**
    * Simulate a single tick.
    * <p>
    * The method returns once the simulation is done.
    * </p>
    * <p>
    * If an exception is thrown during the simulation, it can be retrieved via
    * {@link #getLastThrownException()}.
    * </p>
    * 
    * @return {@code true} if the simulation was successful, {@code false} if the simulation failed or
    *         the controller threw an exception.
    */
   public boolean simulateOneTickNow()
   {
      return simulateNow(1);
   }

   /**
    * Simulate for the duration of 1 record period (typically equal to 1 controller period).
    * <p>
    * The method returns once the simulation is done.
    * </p>
    * <p>
    * If an exception is thrown during the simulation, it can be retrieved via
    * {@link #getLastThrownException()}.
    * </p>
    * 
    * @return {@code true} if the simulation was successful, {@code false} if the simulation failed or
    *         the controller threw an exception.
    */
   public boolean simulateOneBufferRecordPeriodNow()
   {
      return simulateNow(getSimulationSession().getBufferRecordTickPeriod());
   }

   /**
    * Simulate for the given duration.
    * <p>
    * The method returns once the simulation is done.
    * </p>
    * <p>
    * If an exception is thrown during the simulation, it can be retrieved via
    * {@link #getLastThrownException()}.
    * </p>
    * 
    * @param duration desired simulation duration in seconds.
    * @return {@code true} if the simulation was successful, {@code false} if the simulation failed or
    *         the controller threw an exception.
    */
   public boolean simulateNow(double duration)
   {
      checkSimulationSessionAlive();
      lastThrowable.set(null);
      return getSimulationSessionControls().simulateNow(duration);
   }

   /**
    * Simulate for the given number of ticks.
    * <p>
    * The method returns once the simulation is done.
    * </p>
    * <p>
    * If an exception is thrown during the simulation, it can be retrieved via
    * {@link #getLastThrownException()}.
    * </p>
    * 
    * @param numberOfSimulationTicks desired number of simulation ticks.
    * @return {@code true} if the simulation was successful, {@code false} if the simulation failed or
    *         the controller threw an exception.
    */
   public boolean simulateNow(long numberOfSimulationTicks)
   {
      checkSimulationSessionAlive();
      lastThrowable.set(null);
      return getSimulationSessionControls().simulateNow(numberOfSimulationTicks);
   }

   /**
    * Simulate indefinitely.
    * <p>
    * WARNING: This will block the calling thread indefinitely. The caller needs to add an additional
    * terminal condition beforehand to eventually stop the simulation.
    * </p>
    * <p>
    * The method returns once the simulation is done.
    * </p>
    * <p>
    * If an exception is thrown during the simulation, it can be retrieved via
    * {@link #getLastThrownException()}.
    * </p>
    * 
    * @param duration desired simulation duration in seconds.
    * @return {@code true} if the simulation was successful, {@code false} if the simulation failed or
    *         the controller threw an exception.
    * @see #addSimulationTerminalCondition(BooleanSupplier)
    */
   public boolean simulateNow()
   {
      checkSimulationSessionAlive();
      lastThrowable.set(null);
      return getSimulationSessionControls().simulateNow();
   }

   /**
    * Gets the throwable (if any) that was thrown during the last simulation.
    * 
    * @return the exception thrown during the last simulation, or {@code null} if none was thrown.
    */
   public Throwable getLastThrownException()
   {
      return lastThrowable.get();
   }

   public void resetRobot(boolean simulateAfterReset)
   {
      checkSimulationSessionAlive();
      avatarSimulation.resetRobot(simulateAfterReset);
   }

   private void checkSimulationSessionAlive()
   {
      if (getSimulationSession().isSessionShutdown())
         throw new IllegalStateException("Simulation has been shutdown");
   }

   public void assertRobotsRootJointIsInBoundingBox(BoundingBox3DReadOnly boundingBox)
   {
      checkSimulationSessionAlive();
      RobotInterface robot = getSimulationSession().getPhysicsEngine().getRobots().get(0);
      FloatingJointBasics rootJoint = (FloatingJointBasics) robot.getRootBody().getChildrenJoints().get(0);
      boolean inside = boundingBox.isInsideInclusive(rootJoint.getJointPose().getPosition());
      if (!inside)
      {
         fail("Joint was at " + rootJoint.getJointPose().getPosition() + ". Expecting it to be inside boundingBox " + boundingBox);
      }
   }

   // Buffer controls:
   public void cropBuffer()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().cropBuffer();
   }

   public void setInPoint()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().setBufferInPointIndexToCurrent();
   }

   public void setOutPoint()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().setBufferOutPointIndexToCurrent();
   }

   public void gotoInPoint()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().setBufferCurrentIndexToInPoint();
   }

   public void gotoOutPoint()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().setBufferCurrentIndexToOutPoint();
   }

   public void stepBufferIndexForward()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().stepBufferIndexForward();
   }

   public void stepBufferIndexBackward()
   {
      checkSimulationSessionAlive();
      getSimulationSessionControls().stepBufferIndexBackward();
   }

   // GUI controls:
   public void setCameraZoom(double distanceFromFocus)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().setCameraZoom(distanceFromFocus);
   }

   /**
    * Sets the new focus point the camera is looking at.
    * <p>
    * The camera is rotated during this operation, its position remains unchanged.
    * </p>
    * 
    * @param focus the new focus position.
    */
   public void setCameraFocusPosition(Point3DReadOnly focus)
   {
      checkSimulationSessionAlive();
      setCameraFocusPosition(focus.getX(), focus.getY(), focus.getZ());
   }

   /**
    * Sets the new focus point the camera is looking at.
    * <p>
    * The camera is rotated during this operation, its position remains unchanged.
    * </p>
    *
    * @param x the x-coordinate of the new focus location.
    * @param y the y-coordinate of the new focus location.
    * @param z the z-coordinate of the new focus location.
    */
   public void setCameraFocusPosition(double x, double y, double z)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().setCameraFocusPosition(x, y, z);
   }

   /**
    * Sets the new camera position.
    * <p>
    * The camera is rotated during this operation such that the focus point remains unchanged.
    * </p>
    * 
    * @param position the new camera position.
    */
   public void setCameraPosition(Point3DReadOnly position)
   {
      setCameraPosition(position.getX(), position.getY(), position.getZ());
   }

   /**
    * Sets the new camera position.
    * <p>
    * The camera is rotated during this operation such that the focus point remains unchanged.
    * </p>
    * 
    * @param x the x-coordinate of the new camera location.
    * @param y the y-coordinate of the new camera location.
    * @param z the z-coordinate of the new camera location.
    */
   public void setCameraPosition(double x, double y, double z)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().setCameraPosition(x, y, z);
   }

   /**
    * Sets the camera configuration.
    * 
    * @param cameraFocus    the new focus position (where the camera is looking at).
    * @param cameraPosition the new camerate position.
    */
   public void setCamera(Point3DReadOnly cameraFocus, Point3DReadOnly cameraPosition)
   {
      setCameraFocusPosition(cameraFocus);
      setCameraPosition(cameraPosition);
   }

   public void requestCameraRigidBodyTracking(String robotName, String rigidBodyName)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().requestCameraRigidBodyTracking(robotName, rigidBodyName);
   }

   public void addStaticVisuals(Collection<? extends VisualDefinition> visualDefinitions)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().addStaticVisuals(visualDefinitions);
   }

   public void addYoGraphicDefinition(YoGraphicDefinition yoGraphicDefinition)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().addYoGraphic(yoGraphicDefinition);
   }

   public void addYoGraphicDefinition(String namespace, YoGraphicDefinition yoGraphicDefinition)
   {
      checkSimulationSessionAlive();
      if (getSessionVisualizerControls() != null)
         getSessionVisualizerControls().addYoGraphic(namespace, yoGraphicDefinition);
   }

   public void addYoGraphicsListRegistry(YoGraphicsListRegistry yoGraphicsListRegistry)
   {
      checkSimulationSessionAlive();
      SCS1GraphicConversionTools.toYoGraphicDefinitions(yoGraphicsListRegistry).forEach(this::addYoGraphicDefinition);
   }

   public void addYoGraphic(YoGraphic yoGraphic)
   {
      checkSimulationSessionAlive();
      addYoGraphicDefinition(SCS1GraphicConversionTools.toYoGraphicDefinition(yoGraphic));
   }

   // Misc.
   public void finishTest()
   {
      finishTest(keepSCSUp);
   }

   public void finishTest(boolean waitUntilGUIIsDone)
   {
      if (waitUntilGUIIsDone && getSessionVisualizerControls() != null && !avatarSimulation.hasBeenDestroyed())
      {
         getSimulationSession().setSessionMode(SessionMode.PAUSE);
         getSimulationSession().startSessionThread();

         JavaFXMissingTools.runAndWait(getClass(), () ->
         {
            if (!isVisualizerGoingDown.get())
            {
               Window primaryWindow = getSessionVisualizerControls().getPrimaryWindow();
               primaryWindow.requestFocus();
               Alert alert = new Alert(AlertType.INFORMATION, "Test complete!", ButtonType.OK);
               SessionVisualizerIOTools.addSCSIconToDialog(alert);
               alert.initOwner(primaryWindow);
               JavaFXMissingTools.centerDialogInOwner(alert);
               // TODO Seems that on Ubuntu the changes done to the window position/size are not processed properly until the window is showing.
               // This may be related to the bug reported when using GTK3: https://github.com/javafxports/openjdk-jfx/pull/446, might be fixed in later version.
               alert.setOnShown(e -> JavaFXMissingTools.runLater(getClass(), () -> JavaFXMissingTools.centerDialogInOwner(alert)));
               alert.showAndWait();
            }
         });
         getSessionVisualizerControls().waitUntilDown();
      }
      else
      {
         destroy();
      }
   }

   public void destroy()
   {
      if (ros2Node != null)
      {
         ros2Node.destroy();
         ros2Node = null;
      }

      avatarSimulation.destroy();
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public void publishToController(Object message)
   {
      IHMCROS2Publisher ihmcros2Publisher = defaultControllerPublishers.get(message.getClass());
      ihmcros2Publisher.publish(message);
   }

   public <T> IHMCROS2Publisher<T> createPublisherForController(Class<T> messageType)
   {
      return createPublisher(messageType, ROS2Tools.getControllerInputTopic(getRobotModel().getSimpleRobotName()));
   }

   public <T> IHMCROS2Publisher<T> createPublisher(Class<T> messageType, ROS2Topic<?> generator)
   {
      return ROS2Tools.createPublisherTypeNamed(ros2Node, messageType, generator);
   }

   public <T> IHMCROS2Publisher<T> createPublisher(Class<T> messageType, String topicName)
   {
      return ROS2Tools.createPublisher(ros2Node, messageType, topicName);
   }

   private ConcurrentLinkedQueue<Command<?, ?>> controllerCommands;

   public ConcurrentLinkedQueue<Command<?, ?>> getQueuedControllerCommands()
   {
      if (controllerCommands == null)
      {
         controllerCommands = new ConcurrentLinkedQueue<>();
         getHighLevelHumanoidControllerFactory().createQueuedControllerCommandGenerator(controllerCommands);
      }

      return controllerCommands;
   }

   private ScriptBasedControllerCommandGenerator scriptBasedControllerCommandGenerator;

   public ScriptBasedControllerCommandGenerator getScriptBasedControllerCommandGenerator()
   {
      if (scriptBasedControllerCommandGenerator == null)
      {
         scriptBasedControllerCommandGenerator = new ScriptBasedControllerCommandGenerator(getQueuedControllerCommands(), getControllerFullRobotModel());
      }
      return scriptBasedControllerCommandGenerator;
   }

   public void loadScriptFile(InputStream scriptInputStream, ReferenceFrame referenceFrame)
   {
      getScriptBasedControllerCommandGenerator().loadScriptFile(scriptInputStream, referenceFrame);
   }

   public ROS2Node getROS2Node()
   {
      return ros2Node;
   }

   public void setROS2Node(ROS2Node ros2Node)
   {
      this.ros2Node = ros2Node;
   }

   @SuppressWarnings("rawtypes")
   public void setDefaultControllerPublishers(Map<Class<?>, IHMCROS2Publisher> defaultControllerPublishers)
   {
      this.defaultControllerPublishers = defaultControllerPublishers;
   }

   public <T> void createSubscriberFromController(Class<T> messageType, ObjectConsumer<T> consumer)
   {
      createSubscriber(messageType, ROS2Tools.getControllerOutputTopic(getRobotModel().getSimpleRobotName()), consumer);
   }

   public <T> void createSubscriber(Class<T> messageType, ROS2Topic<?> generator, ObjectConsumer<T> consumer)
   {
      ROS2Tools.createCallbackSubscriptionTypeNamed(ros2Node, messageType, generator, s -> consumer.consumeObject(s.takeNextData()));
   }

   public <T> void createSubscriber(Class<T> messageType, String topicName, ObjectConsumer<T> consumer)
   {
      ROS2Tools.createCallbackSubscription(ros2Node, messageType, topicName, s -> consumer.consumeObject(s.takeNextData()));
   }

   public YoRegistry getEstimatorRegistry()
   {
      return avatarSimulation.getEstimatorThread().getYoRegistry();
   }

   public YoRegistry getControllerRegistry()
   {
      return avatarSimulation.getControllerThread().getYoVariableRegistry();
   }

   public FullHumanoidRobotModel getControllerFullRobotModel()
   {
      return avatarSimulation.getControllerFullRobotModel();
   }

   public HighLevelHumanoidControllerFactory getHighLevelHumanoidControllerFactory()
   {
      return avatarSimulation.getHighLevelHumanoidControllerFactory();
   }

   public HighLevelHumanoidControllerToolbox getHighLevelHumanoidControllerToolbox()
   {
      return getHighLevelHumanoidControllerFactory().getHighLevelHumanoidControllerToolbox();
   }

   public CommonHumanoidReferenceFrames getControllerReferenceFrames()
   {
      return getHighLevelHumanoidControllerToolbox().getReferenceFrames();
   }

   /**
    * For unit testing only
    */
   public void addRobotControllerOnControllerThread(RobotController controller)
   {
      avatarSimulation.addRobotControllerOnControllerThread(controller);
   }

   public void addDesiredICPContinuityAssertion(double maxICPPlanError)
   {
      final YoDouble desiredICPX = (YoDouble) findVariable("desiredICPX");
      final YoDouble desiredICPY = (YoDouble) findVariable("desiredICPY");

      final Point2D previousDesiredICP = new Point2D();
      final Point2D desiredICP = new Point2D();

      final int ticksToInitialize = 100;
      final MutableInt xTicks = new MutableInt(0);
      final MutableInt yTicks = new MutableInt(0);

      desiredICPX.addListener(new YoVariableChangedListener()
      {
         @Override
         public void changed(YoVariable v)
         {
            if (getSimulationSession() == null | getSimulationSession().getActiveMode() != SessionMode.RUNNING)
               return; // Do not perform this check if the sim is not running, so the user can scrub the data when sim is done.

            desiredICP.setX(desiredICPX.getDoubleValue());
            if (xTicks.getValue() > ticksToInitialize && yTicks.getValue() > ticksToInitialize)
            {
               assertTrue("ICP plan desired jumped from " + previousDesiredICP + " to " + desiredICP + " in one control DT.",
                          previousDesiredICP.distance(desiredICP) < maxICPPlanError);
            }
            previousDesiredICP.set(desiredICP);

            xTicks.setValue(xTicks.getValue() + 1);
         }
      });

      desiredICPY.addListener(new YoVariableChangedListener()
      {
         @Override
         public void changed(YoVariable v)
         {
            if (getSimulationSession() == null | getSimulationSession().getActiveMode() != SessionMode.RUNNING)
               return; // Do not perform this check if the sim is not running, so the user can scrub the data when sim is done.

            desiredICP.setY(desiredICPY.getDoubleValue());
            if (xTicks.getValue() > ticksToInitialize && yTicks.getValue() > ticksToInitialize)
            {
               assertTrue("ICP plan desired jumped from " + previousDesiredICP + " to " + desiredICP + " in one control DT.",
                          previousDesiredICP.distance(desiredICP) < maxICPPlanError);
            }
            previousDesiredICP.set(desiredICP);

            yTicks.setValue(yTicks.getValue() + 1);
         }
      });
   }

   public SCS2AvatarSimulation getAvatarSimulation()
   {
      return avatarSimulation;
   }

   public DRCRobotModel getRobotModel()
   {
      return avatarSimulation.getRobotModel();
   }

   public String getRobotName()
   {
      return getRobotModel().getSimpleRobotName();
   }

   public double getSimulationDT()
   {
      return getSimulationSession().getSessionDTSeconds();
   }

   public Robot getRobot()
   {
      return avatarSimulation.getRobot();
   }

   public RobotDefinition getRobotDefinition()
   {
      return avatarSimulation.getRobotDefinition();
   }

   public SimulationSession getSimulationSession()
   {
      return avatarSimulation.getSimulationSession();
   }

   public SimulationSessionControls getSimulationSessionControls()
   {
      return getSimulationSession().getSimulationSessionControls();
   }

   public SessionVisualizerControls getSessionVisualizerControls()
   {
      return avatarSimulation.getSessionVisualizerControls();
   }

   public double getSimulationTime()
   {
      return avatarSimulation.getSimulationSession().getTime().getValue();
   }

   public double getControllerTime()
   {
      return avatarSimulation.getHighLevelHumanoidControllerFactory().getHighLevelHumanoidControllerToolbox().getYoTime().getValue();
   }

   public double getTimePerRecordTick()
   {
      return getSimulationSession().getBufferRecordTimePeriod();
   }

   public void createBambooVideo(String simplifiedRobotModelName, int callStackHeight)
   {
      if (createVideo)
      {
         BambooTools.createVideoWithDateTimeClassMethodAndShareOnSharedDriveIfAvailable(simplifiedRobotModelName,
                                                                                        createBambooToolsVideoAndDataExporter(),
                                                                                        callStackHeight,
                                                                                        avatarSimulation.getShowGUI());
      }
      else
      {
         LogTools.info("Skipping video generation.");
      }
   }

   public void createBambooVideo(String videoName)
   {
      if (createVideo)
      {
         BambooTools.createVideoWithDateTimeAndStoreInDefaultDirectory(createBambooToolsVideoAndDataExporter(), videoName, avatarSimulation.getShowGUI());
      }
      else
      {
         LogTools.info("Skipping video generation.");
      }
   }

   private VideoAndDataExporter createBambooToolsVideoAndDataExporter()
   {
      return new VideoAndDataExporter()
      {
         @Override
         public void writeData(File dataFile)
         {
            // TODO Implement me
         }

         @Override
         public void gotoOutPointNow()
         {
            getSimulationSessionControls().setBufferCurrentIndexToOutPoint();
         }

         @Override
         public File createVideo(String string)
         {
            File videoFile = new File(string);
            exportVideo(videoFile);
            return videoFile;
         }
      };
   }

   public void exportVideo(File videoFile)
   {
      getSimulationSession().startSessionThread();
      getSessionVisualizerControls().exportVideo(videoFile);
   }

   public void addRegistry(YoRegistry registry)
   {
      getRootRegistry().addChild(registry);
   }

   public YoRegistry getRootRegistry()
   {
      return getSimulationSession().getRootRegistry();
   }

   @Override
   public YoVariable findVariable(String namespace, String name)
   {
      return getRootRegistry().findVariable(namespace, name);
   }

   @Override
   public List<YoVariable> findVariables(String namespaceEnding, String name)
   {
      return getRootRegistry().findVariables(namespaceEnding, name);
   }

   @Override
   public List<YoVariable> findVariables(YoNamespace namespace)
   {
      return getRootRegistry().findVariables(namespace);
   }

   @Override
   public boolean hasUniqueVariable(String namespaceEnding, String name)
   {
      return getRootRegistry().hasUniqueVariable(namespaceEnding, name);
   }

   @Override
   public List<YoVariable> getVariables()
   {
      return getRootRegistry().getVariables();
   }

   public static class ControllerFailureRuntimeException extends RuntimeException
   {
      private static final long serialVersionUID = 2279107051689445347L;

      public ControllerFailureRuntimeException(String message, Throwable cause)
      {
         super(message, cause);
      }

      public ControllerFailureRuntimeException(String message)
      {
         super(message);
      }
   }
}