package us.ihmc.aware.controller.position;

import us.ihmc.aware.animation.QuadrupedAnimationController;
import us.ihmc.aware.providers.QuadrupedControllerInputProvider;
import us.ihmc.aware.controller.QuadrupedController;
import us.ihmc.aware.controller.QuadrupedControllerManager;
import us.ihmc.aware.providers.QuadrupedRuntimeEnvironment;
import us.ihmc.aware.state.FiniteStateMachine;
import us.ihmc.aware.state.FiniteStateMachineBuilder;
import us.ihmc.aware.state.FiniteStateMachineYoVariableTrigger;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedRobotParameters;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.sensorProcessing.model.RobotMotionStatusHolder;
import us.ihmc.simulationconstructionset.robotController.RobotController;

/**
 * A {@link RobotController} for switching between other robot controllers according to an internal finite state
 * machine.
 * <p/>
 * Users can manually fire events on the {@code userTrigger} YoVariable.
 */
public class QuadrupedPositionControllerManager implements QuadrupedControllerManager
{
   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());
   private final RobotMotionStatusHolder motionStatusHolder = new RobotMotionStatusHolder();

   private final FiniteStateMachine<QuadrupedPositionControllerState, QuadrupedPositionControllerEvent> stateMachine;
   private final FiniteStateMachineYoVariableTrigger<QuadrupedPositionControllerEvent> userEventTrigger;

   public QuadrupedPositionControllerManager(QuadrupedRuntimeEnvironment runtimeEnvironment,
         QuadrupedRobotParameters parameters)
   {
      // Initialize input providers.
      QuadrupedControllerInputProvider inputProvider = new QuadrupedControllerInputProvider(runtimeEnvironment.getGlobalDataProducer(), registry);

      QuadrupedController jointInitializationController = new QuadrupedPositionJointInitializationController(
            runtimeEnvironment);
      QuadrupedController doNothingController = new QuadrupedPositionDoNothingController(runtimeEnvironment);
      QuadrupedController standPrepController = new QuadrupedPositionStandPrepController(runtimeEnvironment, parameters);
      QuadrupedController standReadyController = new QuadrupedPositionStandReadyController(runtimeEnvironment);
      QuadrupedController crawlController = new QuadrupedPositionBasedCrawlControllerAdapter(runtimeEnvironment, parameters, inputProvider);
      QuadrupedController animationController = new QuadrupedAnimationController("***REMOVED***", "***REMOVED***", parameters, runtimeEnvironment);

      FiniteStateMachineBuilder<QuadrupedPositionControllerState, QuadrupedPositionControllerEvent> builder = new FiniteStateMachineBuilder<>(
            QuadrupedPositionControllerState.class, "positionControllerState", registry);

      builder.addState(QuadrupedPositionControllerState.JOINT_INITIALIZATION, jointInitializationController);
      builder.addState(QuadrupedPositionControllerState.DO_NOTHING, doNothingController);
      builder.addState(QuadrupedPositionControllerState.STAND_PREP, standPrepController);
      builder.addState(QuadrupedPositionControllerState.STAND_READY, standReadyController);
      builder.addState(QuadrupedPositionControllerState.CRAWL, crawlController);
      builder.addState(QuadrupedPositionControllerState.ANIMATION, animationController);

      // TODO: Define more state transitions.
      builder.addTransition(QuadrupedPositionControllerEvent.JOINTS_INITIALIZED,
            QuadrupedPositionControllerState.JOINT_INITIALIZATION, QuadrupedPositionControllerState.DO_NOTHING);
      builder.addTransition(QuadrupedPositionControllerEvent.STARTING_POSE_REACHED,
            QuadrupedPositionControllerState.STAND_PREP, QuadrupedPositionControllerState.STAND_READY);

      // Manually triggered events to transition to main controllers.
      builder.addTransition(QuadrupedPositionControllerEvent.REQUEST_STAND_PREP,
            QuadrupedPositionControllerState.DO_NOTHING, QuadrupedPositionControllerState.STAND_PREP);
      builder.addTransition(QuadrupedPositionControllerEvent.REQUEST_CRAWL,
            QuadrupedPositionControllerState.STAND_READY, QuadrupedPositionControllerState.CRAWL);
      builder.addTransition(QuadrupedPositionControllerEvent.REQUEST_ANIMATION,
            QuadrupedPositionControllerState.STAND_READY, QuadrupedPositionControllerState.ANIMATION);

      // Transitions from controllers back to stand prep.
      builder.addTransition(QuadrupedPositionControllerEvent.REQUEST_STAND_PREP, QuadrupedPositionControllerState.CRAWL,
            QuadrupedPositionControllerState.STAND_PREP);
      builder.addTransition(QuadrupedPositionControllerEvent.REQUEST_STAND_PREP, QuadrupedPositionControllerState.ANIMATION,
            QuadrupedPositionControllerState.STAND_PREP);

      this.stateMachine = builder.build(QuadrupedPositionControllerState.JOINT_INITIALIZATION);
      this.userEventTrigger = new FiniteStateMachineYoVariableTrigger<>(stateMachine, "userTrigger", registry,
            QuadrupedPositionControllerEvent.class);
   }

   @Override
   public void initialize()
   {

   }

   @Override
   public void doControl()
   {
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
}
