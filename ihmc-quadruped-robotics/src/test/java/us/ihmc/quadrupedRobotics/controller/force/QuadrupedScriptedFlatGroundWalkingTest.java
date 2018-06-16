package us.ihmc.quadrupedRobotics.controller.force;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;

import controller_msgs.msg.dds.QuadrupedControllerStateChangeMessage;
import controller_msgs.msg.dds.QuadrupedRequestedControllerStateMessage;
import controller_msgs.msg.dds.QuadrupedSteppingStateChangeMessage;
import controller_msgs.msg.dds.QuadrupedTimedStepListMessage;
import controller_msgs.msg.dds.QuadrupedTimedStepMessage;
import us.ihmc.communication.IHMCROS2Publisher;
import us.ihmc.communication.ROS2Tools;
import us.ihmc.communication.ROS2Tools.MessageTopicNameGenerator;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.quadrupedRobotics.QuadrupedForceTestYoVariables;
import us.ihmc.quadrupedRobotics.QuadrupedMultiRobotTestInterface;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.QuadrupedTestGoals;
import us.ihmc.quadrupedRobotics.communication.QuadrupedControllerAPIDefinition;
import us.ihmc.quadrupedRobotics.communication.QuadrupedMessageTools;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControlMode;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerEnum;
import us.ihmc.quadrupedRobotics.controller.QuadrupedControllerRequestedEvent;
import us.ihmc.quadrupedRobotics.controller.QuadrupedSteppingStateEnum;
import us.ihmc.quadrupedRobotics.input.managers.QuadrupedTeleopManager;
import us.ihmc.quadrupedRobotics.simulation.QuadrupedGroundContactModelType;
import us.ihmc.ros2.Ros2Node;
import us.ihmc.simulationConstructionSetTools.util.simulationrunner.GoalOrientedTestConductor;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.tools.MemoryTools;

public abstract class QuadrupedScriptedFlatGroundWalkingTest implements QuadrupedMultiRobotTestInterface
{
   private GoalOrientedTestConductor conductor;
   private QuadrupedForceTestYoVariables variables;
   private QuadrupedTeleopManager stepTeleopManager;

   @Before
   public void setup()
   {
      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " before test.");
   }

   @After
   public void tearDown()
   {
      conductor.concludeTesting();
      conductor = null;
      variables = null;
      stepTeleopManager = null;

      MemoryTools.printCurrentMemoryUsageAndReturnUsedMemoryInMB(getClass().getSimpleName() + " after test.");
   }

   public void testScriptedFlatGroundWalking() throws IOException, BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      QuadrupedTestFactory quadrupedTestFactory = createQuadrupedTestFactory();
      quadrupedTestFactory.setControlMode(QuadrupedControlMode.FORCE);
      quadrupedTestFactory.setGroundContactModelType(QuadrupedGroundContactModelType.FLAT);
      quadrupedTestFactory.setUseNetworking(true);
      conductor = quadrupedTestFactory.createTestConductor();
      variables = new QuadrupedForceTestYoVariables(conductor.getScs());
      stepTeleopManager = quadrupedTestFactory.getStepTeleopManager();

      Ros2Node ros2Node = ROS2Tools.createRos2Node(PubSubImplementation.INTRAPROCESS, "scripted_flat_ground_walking");
      String robotName = quadrupedTestFactory.getRobotName();
      MessageTopicNameGenerator controllerSubGenerator = QuadrupedControllerAPIDefinition.getSubscriberTopicNameGenerator(robotName);
      MessageTopicNameGenerator controllerPubGenerator = QuadrupedControllerAPIDefinition.getPublisherTopicNameGenerator(robotName);

      AtomicReference<QuadrupedControllerEnum> controllerState = new AtomicReference<>();
      AtomicReference<QuadrupedSteppingStateEnum> steppingState = new AtomicReference<>();
      ROS2Tools.createCallbackSubscription(ros2Node, QuadrupedControllerStateChangeMessage.class, controllerPubGenerator,
                                           s -> controllerState.set(QuadrupedControllerEnum.fromByte(s.takeNextData().getEndQuadrupedControllerEnum())));

      ROS2Tools.createCallbackSubscription(ros2Node, QuadrupedSteppingStateChangeMessage.class, controllerPubGenerator,
                                           s -> steppingState.set(QuadrupedSteppingStateEnum.fromByte(s.takeNextData().getEndQuadrupedSteppingStateEnum())));

      QuadrupedRequestedControllerStateMessage controllerMessage = new QuadrupedRequestedControllerStateMessage();
      controllerMessage.setQuadrupedControllerRequestedEvent(QuadrupedControllerRequestedEvent.REQUEST_STEPPING.toByte());
      IHMCROS2Publisher<QuadrupedRequestedControllerStateMessage> controllerStatePublisher = ROS2Tools.createPublisher(ros2Node, QuadrupedRequestedControllerStateMessage.class, controllerSubGenerator);
      controllerStatePublisher.publish(controllerMessage);
      conductor.addTerminalGoal(QuadrupedTestGoals.timeInFuture(variables, 1.0));
      conductor.simulate();

      List<QuadrupedTimedStepMessage> steps = getSteps();
      QuadrupedTimedStepListMessage message = QuadrupedMessageTools.createQuadrupedTimedStepListMessage(steps, false);
      IHMCROS2Publisher<QuadrupedTimedStepListMessage> timedStepPublisher = ROS2Tools.createPublisher(ros2Node, QuadrupedTimedStepListMessage.class, controllerSubGenerator);
      timedStepPublisher.publish(message);

      boolean isStanding = true;
      while (isStanding)
      {
         conductor.addTerminalGoal(QuadrupedTestGoals.timeInFuture(variables, 1.0));
         conductor.simulate();
         isStanding = steppingState.get() == QuadrupedSteppingStateEnum.STAND;
      }

      boolean isStepping = true;
      while (isStepping)
      {
         conductor.addTerminalGoal(QuadrupedTestGoals.timeInFuture(variables, 1.0));
         conductor.simulate();
         isStepping = steppingState.get() == QuadrupedSteppingStateEnum.STEP;
      }

      // check robot is still upright and walked forward
      Point3D expectedFinalPlanarPosition = getFinalPlanarPosition();
      assertEquals(variables.getRobotBodyX().getDoubleValue(), expectedFinalPlanarPosition.getX(), 0.1);
      assertEquals(variables.getRobotBodyY().getDoubleValue(), expectedFinalPlanarPosition.getY(), 0.1);
      assertEquals(variables.getRobotBodyYaw().getDoubleValue(), expectedFinalPlanarPosition.getZ(), 0.1);
      assertTrue(variables.getRobotBodyZ().getDoubleValue() > 0.0);
      conductor.concludeTesting();
   }

   /**
    * Steps to execute, not expressed in absolute time
    */
   public abstract List<QuadrupedTimedStepMessage> getSteps();

   /**
    * Expected final planar position, given as x, y, yaw
    */
   public abstract Point3D getFinalPlanarPosition();
}
