package us.ihmc.valkyrie.hands.zimmer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import controller_msgs.msg.dds.HandJointAnglePacket;
import controller_msgs.msg.dds.OneDoFJointTrajectoryMessage;
import ihmc_common_msgs.msg.dds.TrajectoryPoint1DMessage;
import us.ihmc.avatar.handControl.packetsAndConsumers.HandJointAngleCommunicator;
import us.ihmc.avatar.handControl.packetsAndConsumers.HandSensorData;
import us.ihmc.communication.IHMCRealtimeROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.idl.IDLSequence.Byte;
import us.ihmc.idl.IDLSequence.Object;
import us.ihmc.log.LogTools;
import us.ihmc.mecano.multiBodySystem.interfaces.OneDoFJointBasics;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.math.trajectories.generators.MultipleWaypointsTrajectoryGenerator;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.ros2.ROS2Topic;
import us.ihmc.ros2.RealtimeROS2Node;
import us.ihmc.sensorProcessing.outputData.JointDesiredControlMode;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputBasics;
import us.ihmc.sensorProcessing.outputData.JointDesiredOutputListBasics;
import us.ihmc.valkyrie.hands.ValkyrieHandController;
import us.ihmc.valkyrie.hands.zimmer.ZimmerGripperModel.ZimmerJointName;
import us.ihmc.yoVariables.providers.DoubleProvider;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import valkyrie_msgs.msg.dds.ZimmerTrajectoryMessage;

public class SimulatedZimmerController implements ValkyrieHandController
{
   private final YoRegistry registry;

   private final DoubleProvider yoTime;
   private final AtomicReference<ZimmerTrajectoryMessage> trajectoryMessageReference = new AtomicReference<>();
   private final List<JointHandler> jointHandlers = new ArrayList<>();

   private final JointStatePublisher jointStatePublisher;

   public SimulatedZimmerController(RobotSide robotSide,
                                    FullHumanoidRobotModel fullRobotModel,
                                    JointDesiredOutputListBasics jointDesiredOutputList,
                                    DoubleProvider yoTime,
                                    RealtimeROS2Node realtimeROS2Node,
                                    ROS2Topic<?> outputTopic,
                                    ROS2Topic<?> inputTopic)
   {
      this.yoTime = yoTime;

      registry = new YoRegistry(robotSide.getCamelCaseName() + getClass().getSimpleName());
      // TODO Consider moving up
      if (realtimeROS2Node != null)
         jointStatePublisher = new JointStatePublisher(robotSide, fullRobotModel, realtimeROS2Node, outputTopic);
      else
         jointStatePublisher = null;

      ROS2Tools.createCallbackSubscriptionTypeNamed(realtimeROS2Node, ZimmerTrajectoryMessage.class, inputTopic, s ->
      {
         trajectoryMessageReference.set(s.takeNextData());
      });

      for (ZimmerJointName jointName : ZimmerJointName.values)
      {
         OneDoFJointBasics joint = fullRobotModel.getOneDoFJointByName(jointName.getJointName(robotSide));
         JointDesiredOutputBasics jointDesiredOutput = jointDesiredOutputList.getJointDesiredOutput(joint);
         jointHandlers.add(new JointHandler(robotSide, jointName, joint, jointDesiredOutput, registry));
      }
   }

   @Override
   public void initialize()
   {
      for (int i = 0; i < jointHandlers.size(); i++)
      {
         jointHandlers.get(i).initialize();
      }
   }

   @Override
   public void doControl()
   {
      ZimmerTrajectoryMessage newMessage = trajectoryMessageReference.getAndSet(null);
      if (newMessage != null)
         handleTrajectoryMessage(newMessage);

      jointStatePublisher.update();

      for (int i = 0; i < jointHandlers.size(); i++)
      {
         jointHandlers.get(i).doControl(yoTime.getValue());
      }
   }

   private void handleTrajectoryMessage(ZimmerTrajectoryMessage message)
   {
      Byte jointNames = message.getJointNames();
      Object<OneDoFJointTrajectoryMessage> jointTrajectoryMessages = message.getJointspaceTrajectory().getJointTrajectoryMessages();

      if (jointNames.size() != jointTrajectoryMessages.size())
         LogTools.error("Size of jointNames ({}) and jointTrajectoryMessages ({}) is inconsistent", jointNames.size(), jointTrajectoryMessages.size());

      for (int i = 0; i < jointNames.size(); i++)
      {
         JointHandler jointHandler = jointHandlers.get(jointNames.get(i));
         jointHandler.handleTrajectoryMessage(yoTime.getValue(), jointTrajectoryMessages.get(i));
      }
   }

   @Override
   public YoRegistry getYoRegistry()
   {
      return registry;
   }

   @Override
   public void cleanup()
   {
      if (jointStatePublisher != null)
         jointStatePublisher.cleanup();
   }

   private static class JointHandler
   {
      private final OneDoFJointBasics joint;
      private final JointDesiredOutputBasics jointDesiredOutput;
      private final MultipleWaypointsTrajectoryGenerator trajectory;

      private final YoDouble trajectoryStartTime;

      public JointHandler(RobotSide robotSide,
                          ZimmerJointName jointName,
                          OneDoFJointBasics joint,
                          JointDesiredOutputBasics jointDesiredOutput,
                          YoRegistry registry)
      {
         this.joint = joint;
         this.jointDesiredOutput = jointDesiredOutput;

         trajectory = new MultipleWaypointsTrajectoryGenerator(jointName.getCamelCaseJointName(robotSide), 5, registry);
         trajectoryStartTime = new YoDouble(jointName.getCamelCaseJointName(robotSide) + "TrajectoryStartTime", registry);
      }

      public void handleTrajectoryMessage(double time, OneDoFJointTrajectoryMessage trajectoryMessage)
      {
         double q_0 = trajectory.getValue();
         double qd_0 = trajectory.getVelocity();
         trajectory.clear();

         Object<TrajectoryPoint1DMessage> trajectoryPoints = trajectoryMessage.getTrajectoryPoints();
         TrajectoryPoint1DMessage firstTrajectoryPoint = trajectoryPoints.getFirst();
         if (firstTrajectoryPoint.getTime() < 1.0e-2)
         {
            trajectory.appendWaypoint(0, firstTrajectoryPoint.getPosition(), firstTrajectoryPoint.getVelocity());

            for (int i = 1; i < trajectoryPoints.size(); i++)
            {
               TrajectoryPoint1DMessage trajectoryPoint = trajectoryPoints.get(i);
               trajectory.appendWaypoint(trajectoryPoint.getTime(), trajectoryPoint.getPosition(), trajectoryPoint.getVelocity());
            }
         }
         else
         {
            trajectory.appendWaypoint(0, q_0, qd_0);

            for (int i = 0; i < trajectoryPoints.size(); i++)
            {
               TrajectoryPoint1DMessage trajectoryPoint = trajectoryPoints.get(i);
               trajectory.appendWaypoint(trajectoryPoint.getTime(), trajectoryPoint.getPosition(), trajectoryPoint.getVelocity());
            }
         }

         trajectory.initialize();
      }

      public void initialize()
      {
         trajectory.clear();
         trajectory.appendWaypoint(0, joint.getQ(), 0);
         trajectory.initialize();
         trajectoryStartTime.set(0);
      }

      public void doControl(double time)
      {
         trajectory.compute(Math.max(0, time - trajectoryStartTime.getValue()));
         jointDesiredOutput.setControlMode(JointDesiredControlMode.POSITION);
         jointDesiredOutput.setDesiredPosition(trajectory.getValue());
         jointDesiredOutput.setDesiredVelocity(trajectory.getValue());
      }
   }

   private static class JointStatePublisher
   {
      private final HandJointAngleCommunicator jointAngleProducer;
      private final OneDoFJointBasics[] joints;
      private final double[] jointAngles;
      private final HandSensorData handSensorData = new HandSensorData()
      {
         @Override
         public boolean isConnected()
         {
            return true;
         }

         @Override
         public boolean isCalibrated()
         {
            return true;
         }

         @Override
         public double[] getFingerJointAngles(RobotSide robotSide)
         {
            return jointAngles;
         }
      };

      public JointStatePublisher(RobotSide robotSide, FullHumanoidRobotModel fullRobotModel, RealtimeROS2Node realtimeROS2Node, ROS2Topic<?> outputTopic)
      {
         joints = fullRobotModel.getHand(robotSide).subtreeJointList(OneDoFJointBasics.class).toArray(OneDoFJointBasics[]::new);
         jointAngles = new double[joints.length];

         IHMCRealtimeROS2Publisher<HandJointAnglePacket> publisher = ROS2Tools.createPublisherTypeNamed(realtimeROS2Node,
                                                                                                        HandJointAnglePacket.class,
                                                                                                        outputTopic);
         jointAngleProducer = new HandJointAngleCommunicator(robotSide, publisher);
      }

      public void update()
      {
         for (int i = 0; i < joints.length; i++)
         {
            jointAngles[i] = joints[i].getQ();
         }
         jointAngleProducer.updateHandAngles(handSensorData);
         jointAngleProducer.write();
      }

      public void cleanup()
      {
         jointAngleProducer.cleanup();
      }
   }
}
