package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl;

import us.ihmc.commonWalkingControlModules.controllerCore.WholeBodyControllerCore;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreOutputReadOnly;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.HighLevelBehavior;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.WalkingHighLevelHumanoidController;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states.WalkingStateEnum;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commons.PrintTools;
import us.ihmc.communication.controllerAPI.CommandInputManager;
import us.ihmc.communication.controllerAPI.StatusMessageOutputManager;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.FrameVector2D;
import us.ihmc.humanoidRobotics.bipedSupportPolygons.ContactablePlaneBody;
import us.ihmc.humanoidRobotics.communication.controllerAPI.command.HighLevelControllerStateCommand;
import us.ihmc.humanoidRobotics.communication.packets.HighLevelStateChangeStatusMessage;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;
import us.ihmc.humanoidRobotics.model.CenterOfPressureDataHolder;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.robotics.controllers.ControllerFailureListener;
import us.ihmc.robotics.robotController.RobotController;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.*;
import us.ihmc.robotics.time.ExecutionTimer;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.yoVariables.variable.YoEnum;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class HighLevelHumanoidControllerManager implements RobotController
{
   private final String name = getClass().getSimpleName();
   private final YoVariableRegistry registry = new YoVariableRegistry(name);

   private final GenericStateMachine<HighLevelController, HighLevelBehavior> stateMachine;
   private final HighLevelController initialBehavior;
   private final HighLevelHumanoidControllerToolbox controllerToolbox;

   private final YoEnum<HighLevelController> requestedHighLevelState = new YoEnum<HighLevelController>("requestedHighLevelState", registry,
                                                                                                       HighLevelController.class, true);

   private final YoBoolean isListeningToHighLevelStateMessage = new YoBoolean("isListeningToHighLevelStateMessage", registry);

   private final CenterOfPressureDataHolder centerOfPressureDataHolderForEstimator;
   private final CommandInputManager commandInputManager;
   private final StatusMessageOutputManager statusMessageOutputManager;
   private final ControllerCoreOutputReadOnly controllerCoreOutput;
   private final WholeBodyControllerCore controllerCore;

   private final AtomicReference<HighLevelController> fallbackControllerForFailureReference = new AtomicReference<>();

   private final HighLevelStateChangeStatusMessage highLevelStateChangeStatusMessage = new HighLevelStateChangeStatusMessage();

   private final ExecutionTimer highLevelControllerTimer = new ExecutionTimer("activeHighLevelControllerTimer", 1.0, registry);
   private final ExecutionTimer controllerCoreTimer = new ExecutionTimer("controllerCoreTimer", 1.0, registry);

   public HighLevelHumanoidControllerManager(CommandInputManager commandInputManager, StatusMessageOutputManager statusMessageOutputManager,
                                             WholeBodyControllerCore controllerCore, HighLevelController initialBehavior, ArrayList<HighLevelBehavior> highLevelBehaviors,
                                             HighLevelHumanoidControllerToolbox controllerToolbox, CenterOfPressureDataHolder centerOfPressureDataHolderForEstimator,
                                             ControllerCoreOutputReadOnly controllerCoreOutput)
   {
      this.commandInputManager = commandInputManager;
      this.statusMessageOutputManager = statusMessageOutputManager;
      YoDouble yoTime = controllerToolbox.getYoTime();
      this.controllerCoreOutput = controllerCoreOutput;
      this.controllerCore = controllerCore;

      this.stateMachine = setUpStateMachine(highLevelBehaviors, yoTime, registry);
      requestedHighLevelState.set(initialBehavior);

      isListeningToHighLevelStateMessage.set(true);

      for (int i = 0; i < highLevelBehaviors.size(); i++)
      {
         this.registry.addChild(highLevelBehaviors.get(i).getYoVariableRegistry());
      }
      this.initialBehavior = initialBehavior;
      this.controllerToolbox = controllerToolbox;
      this.centerOfPressureDataHolderForEstimator = centerOfPressureDataHolderForEstimator;
      this.registry.addChild(controllerToolbox.getYoVariableRegistry());

      controllerToolbox.attachControllerFailureListener(new ControllerFailureListener()
      {
         @Override
         public void controllerFailed(FrameVector2D fallingDirection)
         {
            HighLevelController fallbackController = fallbackControllerForFailureReference.get();
            if (fallbackController != null)
               requestedHighLevelState.set(fallbackController);
         }
      });
   }

   public void setFallbackControllerForFailure(HighLevelController fallbackController)
   {
      fallbackControllerForFailureReference.set(fallbackController);
   }

   private GenericStateMachine<HighLevelController, HighLevelBehavior> setUpStateMachine(ArrayList<HighLevelBehavior> highLevelBehaviors, YoDouble yoTime,
                                                                                         YoVariableRegistry registry)
   {
      GenericStateMachine<HighLevelController, HighLevelBehavior> highLevelStateMachine = new GenericStateMachine<>("highLevelController", "switchTimeName",
                                                                                                                    HighLevelController.class, yoTime, registry);

      // Enable transition between every existing state of the state machine
      for (int i = 0; i < highLevelBehaviors.size(); i++)
      {
         HighLevelBehavior highLevelStateA = highLevelBehaviors.get(i);
         highLevelStateA.setControllerCoreOutput(controllerCoreOutput);

         for (int j = 0; j < highLevelBehaviors.size(); j++)
         {
            HighLevelBehavior highLevelStateB = highLevelBehaviors.get(j);

            StateMachineTools.addRequestedStateTransition(requestedHighLevelState, false, highLevelStateA, highLevelStateB);
            StateMachineTools.addRequestedStateTransition(requestedHighLevelState, false, highLevelStateB, highLevelStateA);
         }
      }

      for (int i = 0; i < highLevelBehaviors.size(); i++)
      {
         highLevelStateMachine.addState(highLevelBehaviors.get(i));
      }

      highLevelStateMachine.attachStateChangedListener(new StateChangedListener<HighLevelController>()
      {
         @Override
         public void stateChanged(State<HighLevelController> oldState, State<HighLevelController> newState, double time)
         {
            highLevelStateChangeStatusMessage.setStateChange(oldState.getStateEnum(), newState.getStateEnum());
            statusMessageOutputManager.reportStatusMessage(highLevelStateChangeStatusMessage);
         }
      });

      return highLevelStateMachine;
   }

   public void addYoVariableRegistry(YoVariableRegistry registryToAdd)
   {
      this.registry.addChild(registryToAdd);
   }

   public void requestHighLevelState(HighLevelController requestedHighLevelController)
   {
      this.requestedHighLevelState.set(requestedHighLevelController);
   }

   public void setListenToHighLevelStatePackets(boolean isListening)
   {
      isListeningToHighLevelStateMessage.set(isListening);
   }

   public void initialize()
   {
      controllerCore.initialize();
      controllerToolbox.initialize();
      stateMachine.setCurrentState(initialBehavior);
   }

   public void doControl()
   {
      if (isListeningToHighLevelStateMessage.getBooleanValue())
      {
         if (commandInputManager.isNewCommandAvailable(HighLevelControllerStateCommand.class))
         {
            requestedHighLevelState.set(commandInputManager.pollNewestCommand(HighLevelControllerStateCommand.class).getHighLevelController());
         }
      }

      stateMachine.checkTransitionConditions();
      highLevelControllerTimer.startMeasurement();
      stateMachine.doAction();
      highLevelControllerTimer.stopMeasurement();
      ControllerCoreCommand controllerCoreCommandList = stateMachine.getCurrentState().getControllerCoreCommand();
      controllerCoreTimer.startMeasurement();
      controllerCore.submitControllerCoreCommand(controllerCoreCommandList);
      controllerCore.compute();
      controllerCoreTimer.stopMeasurement();
      reportDesiredCenterOfPressureForEstimator();
   }

   private final SideDependentList<FramePoint2D> desiredFootCoPs = new SideDependentList<FramePoint2D>(new FramePoint2D(), new FramePoint2D());

   private void reportDesiredCenterOfPressureForEstimator()
   {
      SideDependentList<? extends ContactablePlaneBody> contactableFeet = controllerToolbox.getContactableFeet();
      FullHumanoidRobotModel fullHumanoidRobotModel = controllerToolbox.getFullRobotModel();
      for (RobotSide robotSide : RobotSide.values)
      {
         controllerToolbox.getDesiredCenterOfPressure(contactableFeet.get(robotSide), desiredFootCoPs.get(robotSide));
         centerOfPressureDataHolderForEstimator.setCenterOfPressure(desiredFootCoPs.get(robotSide), fullHumanoidRobotModel.getFoot(robotSide));
      }
   }

   public void addHighLevelBehavior(HighLevelBehavior highLevelBehavior, boolean transitionRequested)
   {
      highLevelBehavior.setControllerCoreOutput(controllerCoreOutput);

      // Enable transition between every existing state of the state machine
      for (HighLevelController stateEnum : HighLevelController.values)
      {
         FinishableState<HighLevelController> otherHighLevelState = stateMachine.getState(stateEnum);
         if (otherHighLevelState == null)
            continue;

         StateMachineTools.addRequestedStateTransition(requestedHighLevelState, false, otherHighLevelState, highLevelBehavior);
         StateMachineTools.addRequestedStateTransition(requestedHighLevelState, false, highLevelBehavior, otherHighLevelState);
      }

      this.stateMachine.addState(highLevelBehavior);
      this.registry.addChild(highLevelBehavior.getYoVariableRegistry());

      if (transitionRequested)
         requestedHighLevelState.set(highLevelBehavior.getStateEnum());
   }

   public HighLevelController getCurrentHighLevelState()
   {
      return stateMachine.getCurrentStateEnum();
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return this.getClass().getSimpleName();
   }

   public String getDescription()
   {
      return getName();
   }

   /**
    * Warmup the walking behavior by running all states for a number of iterations.
    * 
    * Also warms up the controller core
    * 
    * @param iterations number of times to run a single state
    * @param walkingBehavior 
    */
   public void warmup(int iterations, WalkingHighLevelHumanoidController walkingBehavior)
   {
      PrintTools.info(this, "Starting JIT warmup routine");
      ArrayList<WalkingStateEnum> states = new ArrayList<>();
      controllerCore.initialize();
      walkingBehavior.doTransitionIntoAction();
      
      walkingBehavior.getOrderedWalkingStatesForWarmup(states);
      for(WalkingStateEnum walkingState : states)
      {
         PrintTools.info(this, "Warming up " + walkingState);
         for(int i = 0; i < iterations; i++)
         {
            walkingBehavior.warmupStateIteration(walkingState);
            ControllerCoreCommand controllerCoreCommandList = walkingBehavior.getControllerCoreCommand();
            controllerCore.submitControllerCoreCommand(controllerCoreCommandList);
            controllerCore.compute();
         }
      }

      walkingBehavior.doTransitionOutOfAction();
      walkingBehavior.getControllerCoreCommand().clear();
      PrintTools.info(this, "Finished JIT warmup routine");
   }

}
