package us.ihmc.quadrupedUI;

import controller_msgs.msg.dds.RobotConfigurationData;
import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.Node;
import us.ihmc.euclid.transform.RigidBodyTransform;
import us.ihmc.graphicsDescription.appearance.YoAppearance;
import us.ihmc.graphicsDescription.structure.Graphics3DNode;
import us.ihmc.javaFXToolkit.messager.JavaFXMessager;
import us.ihmc.javaFXToolkit.node.JavaFXGraphics3DNode;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.messager.Messager;
import us.ihmc.quadrupedBasics.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.robotModels.FullQuadrupedRobotModelFactory;
import us.ihmc.robotics.robotDescription.RobotDescription;
import us.ihmc.robotics.sensors.ForceSensorDefinition;
import us.ihmc.robotics.sensors.IMUDefinition;
import us.ihmc.simulationConstructionSetTools.grahics.GraphicsIDRobot;
import us.ihmc.simulationconstructionset.graphics.GraphicsRobot;

import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;

public class JavaFXQuadrupedVisualizer
{
   private GraphicsRobot graphicsRobot;
   private JavaFXGraphics3DNode robotRootNode;

   private final FullQuadrupedRobotModel fullRobotModel;
   private final QuadrupedReferenceFrames referenceFrames;

   private final OneDoFJointBasics[] allJoints;
   private final int jointNameHash;

   private final AtomicReference<RobotConfigurationData> robotConfigurationDataReference = new AtomicReference<>();

   private boolean isRobotLoaded = false;
   private final Group rootNode = new Group();

   private final AnimationTimer animationTimer;

   public JavaFXQuadrupedVisualizer(FullQuadrupedRobotModelFactory fullRobotModelFactory)
   {
      this(null, fullRobotModelFactory);
   }

   public JavaFXQuadrupedVisualizer(Messager messager, FullQuadrupedRobotModelFactory fullRobotModelFactory)
   {
      fullRobotModel = fullRobotModelFactory.createFullRobotModel();
      allJoints = fullRobotModel.getOneDoFJoints();
      referenceFrames = new QuadrupedReferenceFrames(fullRobotModel);

      jointNameHash = calculateJointNameHash(allJoints, fullRobotModel.getForceSensorDefinitions(), fullRobotModel.getIMUDefinitions());

      new Thread(() -> loadRobotModelAndGraphics(fullRobotModelFactory), "RobotVisualizerLoading").start();

      animationTimer = new AnimationTimer()
      {
         @Override
         public void handle(long now)
         {
            if (!isRobotLoaded)
               return;
            else if (rootNode.getChildren().isEmpty())
               rootNode.getChildren().add(robotRootNode);

            RobotConfigurationData robotConfigurationData = robotConfigurationDataReference.getAndSet(null);
            if (robotConfigurationData == null)
               return;

            if (robotConfigurationData.getJointNameHash() != jointNameHash)
               throw new RuntimeException("Joint names do not match for RobotConfigurationData");

            RigidBodyTransform newRootJointPose = new RigidBodyTransform(robotConfigurationData.getRootOrientation(),
                                                                         robotConfigurationData.getRootTranslation());
            fullRobotModel.getRootJoint().setJointConfiguration(newRootJointPose);

            float[] newJointConfiguration = robotConfigurationData.getJointAngles().toArray();
            for (int i = 0; i < allJoints.length; i++)
               allJoints[i].setQ(newJointConfiguration[i]);

            fullRobotModel.getElevator().updateFramesRecursively();
            graphicsRobot.update();
            robotRootNode.update();
            referenceFrames.updateFrames();

            if (messager != null)
            {
               messager.submitMessage(QuadrupedUIMessagerAPI.RobotModelTopic, fullRobotModel);
               messager.submitMessage(QuadrupedUIMessagerAPI.ReferenceFramesTopic, referenceFrames);
            }
         }
      };
   }

   private static int calculateJointNameHash(OneDoFJointBasics[] joints, ForceSensorDefinition[] forceSensorDefinitions, IMUDefinition[] imuDefinitions)
   {
      CRC32 crc = new CRC32();
      for (OneDoFJointBasics joint : joints)
      {
         crc.update(joint.getName().getBytes());
      }

      for (ForceSensorDefinition forceSensorDefinition : forceSensorDefinitions)
      {
         crc.update(forceSensorDefinition.getSensorName().getBytes());
      }

      for (IMUDefinition imuDefinition : imuDefinitions)
      {
         crc.update(imuDefinition.getName().getBytes());
      }

      return (int) crc.getValue();
   }

   private void loadRobotModelAndGraphics(FullQuadrupedRobotModelFactory fullRobotModelFactory)
   {
      RobotDescription robotDescription = fullRobotModelFactory.getRobotDescription();
      graphicsRobot = new GraphicsIDRobot(robotDescription.getName(), fullRobotModel.getElevator(), robotDescription);
      robotRootNode = new JavaFXGraphics3DNode(graphicsRobot.getRootNode());
      robotRootNode.setMouseTransparent(true);
      addNodesRecursively(graphicsRobot.getRootNode(), robotRootNode);
      robotRootNode.update();

      isRobotLoaded = true;
   }

   public void start()
   {
      animationTimer.start();
   }

   public void stop()
   {
      animationTimer.stop();
   }

   private void addNodesRecursively(Graphics3DNode graphics3dNode, JavaFXGraphics3DNode parentNode)
   {
      JavaFXGraphics3DNode node = new JavaFXGraphics3DNode(graphics3dNode, YoAppearance.Green());
      parentNode.addChild(node);
      graphics3dNode.getChildrenNodes().forEach(child -> addNodesRecursively(child, node));
   }

   public void submitNewConfiguration(RobotConfigurationData robotConfigurationData)
   {
      if (robotConfigurationData.getJointNameHash() != jointNameHash)
         throw new RuntimeException("Joint names do not match for RobotConfigurationData");

      robotConfigurationDataReference.set(robotConfigurationData);
   }

   public FullQuadrupedRobotModel getFullRobotModel()
   {
      return fullRobotModel;
   }

   public Node getRootNode()
   {
      return rootNode;
   }
}
