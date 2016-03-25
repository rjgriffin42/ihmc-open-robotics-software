package us.ihmc.aware.controller.force;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.aware.communication.QuadrupedControllerInputProvider;
import us.ihmc.aware.controller.QuadrupedController;
import us.ihmc.aware.controller.QuadrupedControllerManager;
import us.ihmc.aware.controller.force.taskSpaceController.QuadrupedTaskSpaceController;
import us.ihmc.aware.controller.force.taskSpaceController.QuadrupedTaskSpaceEstimator;
import us.ihmc.aware.packets.QuadrupedForceControllerEventPacket;
import us.ihmc.aware.parameters.QuadrupedRuntimeEnvironment;
import us.ihmc.aware.params.ParameterMapRepository;
import us.ihmc.aware.state.StateMachine;
import us.ihmc.aware.state.StateMachineBuilder;
import us.ihmc.aware.state.StateMachineYoVariableTrigger;
import us.ihmc.communication.net.PacketConsumer;
import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedRobotParameters;
import us.ihmc.quadrupedRobotics.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.simulationconstructionset.robotController.RobotController;

/**
 * A {@link RobotController} for switching between other robot controllers according to an internal finite state machine.
 * <p/>
 * Users can manually fire events on the {@code userTrigger} YoVariable.
 */
public class QuadrupedForceControllerManager implements QuadrupedControllerManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final RobotMotionStatusHolder motionStatusHolder = new RobotMotionStatusHolder();
   private final QuadrupedControllerInputProvider inputProvider;

   private final StateMachine<QuadrupedForceControllerState, QuadrupedForceControllerEvent> stateMachine;
   private final StateMachineYoVariableTrigger<QuadrupedForceControllerEvent> userEventTrigger;

   private final AtomicReference<QuadrupedForceControllerEvent> requestedEvent = new AtomicReference<>();

   public QuadrupedForceControllerManager(QuadrupedRuntimeEnvironment runtimeEnvironment, QuadrupedRobotParameters parameters) throws IOException
   {
      // Initialize parameter map repository.
      ParameterMapRepository paramMapRepository = new ParameterMapRepository(registry);

      // Initialize input providers.
      inputProvider = new QuadrupedControllerInputProvider(runtimeEnvironment.getGlobalDataProducer(), paramMapRepository, registry);

      GlobalDataProducer globalDataProducer = runtimeEnvironment.getGlobalDataProducer();
      globalDataProducer.attachListener(QuadrupedForceControllerEventPacket.class, new PacketConsumer<QuadrupedForceControllerEventPacket>()
      {
         @Override
         public void receivedPacket(QuadrupedForceControllerEventPacket packet)
         {
            requestedEvent.set(packet.get());
         }
      });

      this.stateMachine = buildStateMachine(runtimeEnvironment, parameters, paramMapRepository, inputProvider);
      this.userEventTrigger = new StateMachineYoVariableTrigger<>(stateMachine, "userTrigger", registry, QuadrupedForceControllerEvent.class);
   }

   @Override
   public void initialize()
   {

   }

   @Override
   public void doControl()
   {
      QuadrupedForceControllerEvent reqEvent = requestedEvent.getAndSet(null);
      if (reqEvent != null)
      {
         stateMachine.trigger(reqEvent);
      }

      stateMachine.process();
   }

   @Override
   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   @Override
   public String getName()
   {
      return getClass().getSimpleName();
   }

   @Override
   public String getDescription()
   {
      return "A proxy controller for switching between multiple subcontrollers";
   }

   public RobotMotionStatusHolder getMotionStatusHolder()
   {
      return motionStatusHolder;
   }

   private StateMachine<QuadrupedForceControllerState, QuadrupedForceControllerEvent> buildStateMachine(QuadrupedRuntimeEnvironment runtimeEnvironment,
         QuadrupedRobotParameters parameters, ParameterMapRepository paramMapRepository, QuadrupedControllerInputProvider inputProvider)
   {
      // Initialize controller components.
      QuadrupedReferenceFrames referenceFrames = new QuadrupedReferenceFrames(runtimeEnvironment.getFullRobotModel(), parameters.getJointMap(), parameters.getPhysicalProperties());
      QuadrupedTaskSpaceEstimator taskSpaceEstimator = new QuadrupedTaskSpaceEstimator(runtimeEnvironment.getFullRobotModel(), referenceFrames, parameters.getJointMap(), registry, runtimeEnvironment.getGraphicsListRegistry());
      QuadrupedTaskSpaceController taskSpaceController = new QuadrupedTaskSpaceController(runtimeEnvironment.getFullRobotModel(), referenceFrames, parameters.getJointMap(), parameters.getQuadrupedJointLimits(), runtimeEnvironment.getControlDT(),
            registry, runtimeEnvironment.getGraphicsListRegistry());

      // Initialize controllers.
      QuadrupedForceController jointInitializationController = new QuadrupedForceJointInitializationController(runtimeEnvironment, parameters);
      QuadrupedVirtualModelBasedStandPrepController standPrepController = new QuadrupedVirtualModelBasedStandPrepController(runtimeEnvironment, parameters,
            paramMapRepository);
      QuadrupedController standController = new QuadrupedVirtualModelBasedStandController(runtimeEnvironment, parameters, paramMapRepository, inputProvider, referenceFrames, taskSpaceEstimator, taskSpaceController);
      QuadrupedController stepController = new QuadrupedVirtualModelBasedStepController(runtimeEnvironment, parameters, paramMapRepository, inputProvider);
      QuadrupedForceController trotController = new QuadrupedVirtualModelBasedTrotController(runtimeEnvironment, parameters, paramMapRepository, inputProvider, referenceFrames, taskSpaceEstimator, taskSpaceController);

      StateMachineBuilder<QuadrupedForceControllerState, QuadrupedForceControllerEvent> builder = new StateMachineBuilder<>(QuadrupedForceControllerState.class,
            "forceControllerState", registry);

      builder.addState(QuadrupedForceControllerState.JOINT_INITIALIZATION, jointInitializationController);
      builder.addState(QuadrupedForceControllerState.STAND_PREP, standPrepController);
      builder.addState(QuadrupedForceControllerState.STAND, standController);
      builder.addState(QuadrupedForceControllerState.STEP, stepController);
      builder.addState(QuadrupedForceControllerState.TROT, trotController);

      // Add automatic transitions that lead into the stand state.
      builder.addTransition(QuadrupedForceControllerEvent.JOINTS_INITIALIZED, QuadrupedForceControllerState.JOINT_INITIALIZATION,
            QuadrupedForceControllerState.STAND_PREP);
      builder.addTransition(QuadrupedForceControllerEvent.STARTING_POSE_REACHED, QuadrupedForceControllerState.STAND_PREP, QuadrupedForceControllerState.STAND);

      // Manually triggered events to transition to main controllers.
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_STAND, QuadrupedForceControllerState.STAND_PREP, QuadrupedForceControllerState.STAND);
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_STAND, QuadrupedForceControllerState.STEP, QuadrupedForceControllerState.STAND);
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_STAND, QuadrupedForceControllerState.TROT, QuadrupedForceControllerState.STAND);
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_STEP, QuadrupedForceControllerState.STAND_PREP, QuadrupedForceControllerState.STEP);
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_STEP, QuadrupedForceControllerState.STAND, QuadrupedForceControllerState.STEP);
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_TROT, QuadrupedForceControllerState.STAND_PREP, QuadrupedForceControllerState.TROT);
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_TROT, QuadrupedForceControllerState.STAND, QuadrupedForceControllerState.TROT);

      // Transitions from controllers back to stand prep.
      builder.addTransition(QuadrupedForceControllerEvent.REQUEST_STAND_PREP, QuadrupedForceControllerState.STAND, QuadrupedForceControllerState.STAND_PREP);

      return builder.build(QuadrupedForceControllerState.JOINT_INITIALIZATION);
   }
}
