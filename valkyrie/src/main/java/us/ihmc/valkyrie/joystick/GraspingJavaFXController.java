package us.ihmc.valkyrie.joystick;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.KinematicsPlanningToolboxOutputStatus;
import controller_msgs.msg.dds.KinematicsPlanningToolboxRigidBodyMessage;
import controller_msgs.msg.dds.ToolboxStateMessage;
import controller_msgs.msg.dds.WholeBodyTrajectoryMessage;
import gnu.trove.list.array.TDoubleArrayList;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Affine;
import us.ihmc.avatar.handControl.HandFingerTrajectoryMessagePublisher;
import us.ihmc.avatar.joystickBasedJavaFXController.ButtonState;
import us.ihmc.avatar.joystickBasedJavaFXController.XBoxOneJavaFXController;
import us.ihmc.avatar.networkProcessor.kinematicsPlanningToolboxModule.KinematicsPlanningToolboxModule;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.ControllerAPIDefinition;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.communication.packets.ToolboxState;
import us.ihmc.euclid.matrix.RotationMatrix;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Tuple3DBasics;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidRobotics.communication.packets.WholeBodyTrajectoryToolboxOutputConverter;
import us.ihmc.humanoidRobotics.frames.HumanoidReferenceFrames;
import us.ihmc.javaFXToolkit.JavaFXTools;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.shapes.JavaFXCoordinateSystem;
import us.ihmc.javaFXVisualizers.JavaFXRobotVisualizer;
import us.ihmc.log.LogTools;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotModels.FullHumanoidRobotModelFactory;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.Ros2Node;

/**
 * What to do this controller.
 * 1. Controlling fingers.
 * 2. Handling end effector key frames with XBOX controller.
 * 3. Communicating with KinematicsPlanningToolbox.
 * 4. Visualizing preview of motion.
 * 5. Publishing WholeBodyTrajectoryMessage.
 */
public class GraspingJavaFXController
{
   private final JavaFXMessager messager;
   private final FullHumanoidRobotModel fullRobotModel;

   private final WholeBodyTrajectoryToolboxOutputConverter outputConverter;

   private final SideDependentList<AtomicReference<Boolean>> sendFingerMessages = new SideDependentList<>();
   private final SideDependentList<AtomicReference<Double>> desiredThumbRolls = new SideDependentList<>();
   private final SideDependentList<AtomicReference<Double>> desiredThumbPitchs = new SideDependentList<>();
   private final SideDependentList<AtomicReference<Double>> desiredThumbPitch2s = new SideDependentList<>();
   private final SideDependentList<AtomicReference<Double>> desiredIndexes = new SideDependentList<>();
   private final SideDependentList<AtomicReference<Double>> desiredMiddles = new SideDependentList<>();
   private final SideDependentList<AtomicReference<Double>> desiredPinkys = new SideDependentList<>();

   private final AnimationTimer animationTimer;

   private final Group rootNode = new Group();

   private final static double timeDurationForFinger = 2.0;
   private final static double ratioJoyStickToPosition = 0.01;
   private final static double ratioJoyStickToRotation = 0.02;
   private final static double lengthOfControlFrame = 0.3;
   private final static double lengthOfkeyFrameReferenceFrame = 0.2;

   private final HandFingerTrajectoryMessagePublisher handFingerTrajectoryMessagePublisher;
   private final IHMCROS2Publisher<WholeBodyTrajectoryMessage> wholeBodyTrajectoryPublisher;
   private final IHMCROS2Publisher<ToolboxStateMessage> toolboxStatePublisher;
   private final IHMCROS2Publisher<KinematicsPlanningToolboxRigidBodyMessage> toolboxMessagePublisher;

   private final AtomicReference<List<Node>> objectsToVisualizeReference = new AtomicReference<>(new ArrayList<>());
   private final static Vector3D defaultPositionToCreateObject = new Vector3D(0.6, 0.3, 1.0);
   private final Point3D controlPosition = new Point3D(defaultPositionToCreateObject);
   private final RotationMatrix controlOrientation = new RotationMatrix();
   //   private final FramePoint3D controlFramePosition;
   //   private final FrameQuaternion controlFrameOrientation;
   private final ReferenceFrame pelvisZUpFrame;

   private boolean selectingSide = false;
   private RobotSide selectedSide = null;

   private final DoubleProperty velocityXProperty = new SimpleDoubleProperty(this, "velocityXProperty", 0.0);
   private final DoubleProperty velocityYProperty = new SimpleDoubleProperty(this, "velocityYProperty", 0.0);
   private final DoubleProperty velocityZProperty = new SimpleDoubleProperty(this, "velocityZProperty", 0.0);
   private final DoubleProperty velocityRollProperty = new SimpleDoubleProperty(this, "velocityRollProperty", 0.0);
   private final DoubleProperty velocityPitchProperty = new SimpleDoubleProperty(this, "velocityPitchProperty", 0.0);
   private final DoubleProperty velocityYawProperty = new SimpleDoubleProperty(this, "velocityYawProperty", 0.0);

   private int indexOfSelectedObject = 0;
   private final List<RigidBodyTransform> keyFramePoses = new ArrayList<>();

   private final AtomicReference<KinematicsPlanningToolboxOutputStatus> toolboxOutputPacket = new AtomicReference<>(null);
   private final ValkyrieJavaFXMotionPreviewVisualizer motionPreviewVisualizer;

   public GraspingJavaFXController(String robotName, JavaFXMessager messager, Ros2Node ros2Node, FullHumanoidRobotModelFactory fullRobotModelFactory,
                                   JavaFXRobotVisualizer javaFXRobotVisualizer, HandFingerTrajectoryMessagePublisher handFingerTrajectoryMessagePublisher)
   {
      fullRobotModel = javaFXRobotVisualizer.getFullRobotModel();
      outputConverter = new WholeBodyTrajectoryToolboxOutputConverter(fullRobotModelFactory);

      this.messager = messager;
      motionPreviewVisualizer = new ValkyrieJavaFXMotionPreviewVisualizer(fullRobotModelFactory);

      HumanoidReferenceFrames referenceFrames = new HumanoidReferenceFrames(fullRobotModel);
      pelvisZUpFrame = referenceFrames.getPelvisZUpFrame();
      //      controlFramePosition = new FramePoint3D(pelvisZUpFrame, defaultPositionToCreateObject);
      //      controlFrameOrientation = new FrameQuaternion(pelvisZUpFrame, new Quaternion());

      sendFingerMessages.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightSendMessage, false));
      sendFingerMessages.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftSendMessage, false));

      desiredThumbRolls.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightThumbRoll, 0.0));
      desiredThumbPitchs.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightThumb, 0.0));
      desiredThumbPitch2s.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightThumb2, 0.0));
      desiredIndexes.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightIndex, 0.0));
      desiredMiddles.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightMiddle, 0.0));
      desiredPinkys.put(RobotSide.RIGHT, messager.createInput(GraspingJavaFXTopics.RightPinky, 0.0));

      desiredThumbRolls.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftThumbRoll, 0.0));
      desiredThumbPitchs.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftThumb, 0.0));
      desiredThumbPitch2s.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftThumb2, 0.0));
      desiredIndexes.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftIndex, 0.0));
      desiredMiddles.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftMiddle, 0.0));
      desiredPinkys.put(RobotSide.LEFT, messager.createInput(GraspingJavaFXTopics.LeftPinky, 0.0));

      messager.registerTopicListener(XBoxOneJavaFXController.ButtonSelectState, state -> clearObjects(state));
      messager.registerTopicListener(XBoxOneJavaFXController.ButtonAState, state -> createObject(state));
      messager.registerTopicListener(XBoxOneJavaFXController.ButtonBState, state -> switchSelectedObject(state));

      messager.registerJavaFXSyncedTopicListener(XBoxOneJavaFXController.LeftStickYAxis, this::appendingXAxis);
      messager.registerJavaFXSyncedTopicListener(XBoxOneJavaFXController.LeftStickXAxis, this::appendingYAxis);
      messager.registerTopicListener(XBoxOneJavaFXController.ButtonLeftBumperState, state -> appendingZAxisPositive(state));
      messager.registerJavaFXSyncedTopicListener(XBoxOneJavaFXController.LeftTriggerAxis, this::appendingZAxisNegative);

      messager.registerJavaFXSyncedTopicListener(XBoxOneJavaFXController.RightStickYAxis, this::appendingPitch);
      messager.registerJavaFXSyncedTopicListener(XBoxOneJavaFXController.RightStickXAxis, this::appendingRoll);
      messager.registerTopicListener(XBoxOneJavaFXController.ButtonRightBumperState, state -> appendingYawPositive(state));
      messager.registerJavaFXSyncedTopicListener(XBoxOneJavaFXController.RightTriggerAxis, this::appendingYawNegative);

      messager.registerTopicListener(XBoxOneJavaFXController.ButtonXState, state -> submitReachingManifoldsToToolbox(state));
      //messager.registerTopicListener(XBoxOneJavaFXController.ButtonYState, state -> confirmReachingMotion(state));

      ROS2Tools.MessageTopicNameGenerator toolboxRequestTopicNameGenerator = KinematicsPlanningToolboxModule.getSubscriberTopicNameGenerator(robotName);
      ROS2Tools.MessageTopicNameGenerator toolboxResponseTopicNameGenerator = KinematicsPlanningToolboxModule.getPublisherTopicNameGenerator(robotName);

      MessageTopicNameGenerator subscriberTopicNameGenerator = ControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName);

      wholeBodyTrajectoryPublisher = ROS2Tools.createPublisher(ros2Node, WholeBodyTrajectoryMessage.class, subscriberTopicNameGenerator);
      toolboxStatePublisher = ROS2Tools.createPublisher(ros2Node, ToolboxStateMessage.class, toolboxRequestTopicNameGenerator);
      toolboxMessagePublisher = ROS2Tools.createPublisher(ros2Node, KinematicsPlanningToolboxRigidBodyMessage.class, toolboxRequestTopicNameGenerator);
      this.handFingerTrajectoryMessagePublisher = handFingerTrajectoryMessagePublisher;

      ROS2Tools.createCallbackSubscription(ros2Node, KinematicsPlanningToolboxOutputStatus.class, toolboxResponseTopicNameGenerator,
                                           s -> consumeToolboxOutputStatus(s.takeNextData()));

      animationTimer = new AnimationTimer()
      {
         @Override
         public void handle(long arg0)
         {
            updateSelectedObject();
            updateVisualizationObjects();
            submitDesiredFingerConfigurationMessage();
         }
      };
   }

   private void consumeToolboxOutputStatus(KinematicsPlanningToolboxOutputStatus packet)
   {
      LogTools.info("packet arrived");
      toolboxOutputPacket.set(packet);
      LogTools.info("key frame robot configurations " + toolboxOutputPacket.get().getRobotConfigurations());
   }

   private void submitDesiredFingerConfigurationMessage()
   {
      for (RobotSide robotSide : RobotSide.values)
      {
         if (sendFingerMessages.get(robotSide).get())
         {
            sendFingerMessages.get(robotSide).set(false);

            TDoubleArrayList desiredPositions = new TDoubleArrayList();
            TDoubleArrayList trajectoryTimes = new TDoubleArrayList();

            desiredPositions.add(desiredThumbRolls.get(robotSide).get());
            desiredPositions.add(desiredThumbPitchs.get(robotSide).get());
            desiredPositions.add(desiredThumbPitch2s.get(robotSide).get());
            desiredPositions.add(desiredIndexes.get(robotSide).get());
            desiredPositions.add(desiredMiddles.get(robotSide).get());
            desiredPositions.add(desiredPinkys.get(robotSide).get());

            trajectoryTimes.add(timeDurationForFinger);
            trajectoryTimes.add(timeDurationForFinger);
            trajectoryTimes.add(timeDurationForFinger);
            trajectoryTimes.add(timeDurationForFinger);
            trajectoryTimes.add(timeDurationForFinger);
            trajectoryTimes.add(timeDurationForFinger);

            handFingerTrajectoryMessagePublisher.sendFingerTrajectoryMessage(robotSide, desiredPositions, trajectoryTimes);
         }
      }
   }

   private void submitReachingManifoldsToToolbox(ButtonState state)
   {
      if (state == ButtonState.PRESSED)
         selectingSide = true;
      else
         selectingSide = false;

      if (state == ButtonState.RELEASED)
      {
         if (selectedSide != null)
         {
            if (keyFramePoses.size() > 0)
            {
               motionPreviewVisualizer.enable(false);
               toolboxStatePublisher.publish(MessageTools.createToolboxStateMessage(ToolboxState.WAKE_UP));

               LogTools.info("KinematicsPlanningToolboxMessage is created as...");
               System.out.println("selectedSide " + selectedSide);
               System.out.println("keyFramePoses.size() " + keyFramePoses.size());

               for (int i = 0; i < keyFramePoses.size(); i++)
               {
                  System.out.println("keyFramePoses " + i);
                  System.out.println(keyFramePoses.get(i));
               }

               selectedSide = null;
            }
            else
               System.out.println("there is no key frame created");
         }
         else
            System.out.println("robot side is not selected");
      }
   }

   private void updateSelectedObject()
   {
      pelvisZUpFrame.update();

      controlPosition.add(velocityXProperty.getValue(), velocityYProperty.getValue(), velocityZProperty.getValue());
      controlOrientation.prependRollRotation(velocityRollProperty.getValue());
      controlOrientation.prependPitchRotation(velocityPitchProperty.getValue());
      controlOrientation.prependYawRotation(velocityYawProperty.getValue());
   }

   private void appendingRoll(double alpha)
   {
      velocityRollProperty.set(-alpha * ratioJoyStickToRotation);
   }

   private void appendingPitch(double alpha)
   {
      velocityPitchProperty.set(alpha * ratioJoyStickToRotation);
   }

   private void appendingYawNegative(double alpha)
   {
      velocityYawProperty.set(alpha * ratioJoyStickToRotation);
   }

   private void appendingYawPositive(ButtonState state)
   {
      if (toolboxSelectRobotSide(state, RobotSide.RIGHT))
         ;
      else
      {
         if (state == ButtonState.PRESSED)
            velocityYawProperty.set(ratioJoyStickToRotation);
         else
            velocityYawProperty.set(0.0);
      }
   }

   private void appendingXAxis(double alpha)
   {
      velocityXProperty.set(alpha * ratioJoyStickToPosition);
   }

   private void appendingYAxis(double alpha)
   {
      velocityYProperty.set(alpha * ratioJoyStickToPosition);
   }

   private void appendingZAxisNegative(double alpha)
   {
      velocityZProperty.set(alpha * ratioJoyStickToPosition);
   }

   private void appendingZAxisPositive(ButtonState state)
   {
      if (toolboxSelectRobotSide(state, RobotSide.LEFT))
         ;
      else
      {
         if (state == ButtonState.PRESSED)
            velocityZProperty.set(ratioJoyStickToPosition);
         else
            velocityZProperty.set(0.0);
      }
   }

   private boolean toolboxSelectRobotSide(ButtonState state, RobotSide robotSide)
   {
      if (selectingSide)
      {
         if (state == ButtonState.PRESSED)
            selectedSide = robotSide;

         return true;
      }
      else
         return false;
   }

   private void createObject(ButtonState state)
   {
      if (state == ButtonState.PRESSED)
      {
         RigidBodyTransform rigidBodyTransformToCreateKeyFrame = new RigidBodyTransform();

         rigidBodyTransformToCreateKeyFrame.setTranslation(controlPosition);
         rigidBodyTransformToCreateKeyFrame.setRotation(controlOrientation);

         keyFramePoses.add(rigidBodyTransformToCreateKeyFrame);

         indexOfSelectedObject = keyFramePoses.size() - 1;
      }
   }

   private void switchSelectedObject(ButtonState state)
   {
      int numberOfObjects = keyFramePoses.size();

      if (numberOfObjects < 1)
         return;

      if (state == ButtonState.RELEASED)
         return;

      indexOfSelectedObject++;
      if (indexOfSelectedObject == numberOfObjects)
         indexOfSelectedObject = 0;

      keyFramePoses.get(indexOfSelectedObject).getTranslation(controlPosition);
      keyFramePoses.get(indexOfSelectedObject).getRotation(controlOrientation);
   }

   private void clearObjects(ButtonState state)
   {
      if (state == ButtonState.PRESSED)
      {
         motionPreviewVisualizer.enable(false);
         keyFramePoses.clear();
         indexOfSelectedObject = 0;
      }
   }

   private void updateVisualizationObjects()
   {
      List<Node> objectsToPutReference = new ArrayList<Node>();
      for (int i = 0; i < keyFramePoses.size(); i++)
      {
         RigidBodyTransform objectToVisualize = keyFramePoses.get(i);
         double lengthOfFrame = lengthOfkeyFrameReferenceFrame;
         if (i == indexOfSelectedObject)
         {
            lengthOfFrame = lengthOfControlFrame;
            objectToVisualize.setTranslation(controlPosition);
            objectToVisualize.setRotation(controlOrientation);
         }

         Tuple3DBasics translation = new Point3D(objectToVisualize.getTranslationVector());
         Quaternion orientation = new Quaternion(objectToVisualize.getRotationMatrix());

         JavaFXCoordinateSystem controlCoordinateSystem = new JavaFXCoordinateSystem(lengthOfFrame);
         Affine controlTransform = JavaFXTools.createAffineFromQuaternionAndTuple(new Quaternion(orientation), translation);
         controlCoordinateSystem.getTransforms().add(controlTransform);
         objectsToPutReference.add(controlCoordinateSystem);
      }

      objectsToVisualizeReference.set(objectsToPutReference);

      List<Node> objectsToVisualize = objectsToVisualizeReference.getAndSet(null);
      ObservableList<Node> children = rootNode.getChildren();

      if (objectsToVisualize != null)
      {
         children.clear();
         children.addAll(objectsToVisualize);
      }
   }

   public void start()
   {
      animationTimer.start();
   }

   public void stop()
   {
      animationTimer.stop();
   }

   public Node getRootNode()
   {
      return rootNode;
   }
}