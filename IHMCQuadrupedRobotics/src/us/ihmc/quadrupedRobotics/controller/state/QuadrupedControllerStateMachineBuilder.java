package us.ihmc.quadrupedRobotics.controller.state;

import java.util.ArrayList;
import java.util.List;

import us.ihmc.communication.streamingData.GlobalDataProducer;
import us.ihmc.quadrupedRobotics.controller.QuadrupedCenterOfMassVerificationController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedDoNothingController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedJointInitializer;
import us.ihmc.quadrupedRobotics.controller.QuadrupedLegJointSliderBoardController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedPositionBasedCrawlController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedStandPrepController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedStandReadyController;
import us.ihmc.quadrupedRobotics.controller.QuadrupedTrotWalkController;
import us.ihmc.aware.mechanics.inverseKinematics.QuadrupedLegInverseKinematicsCalculator;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedCommonControllerParameters;
import us.ihmc.aware.model.QuadrupedRobotParameters;
import us.ihmc.robotics.dataStructures.variable.EnumYoVariable;
import us.ihmc.robotics.stateMachines.GenericStateMachine;
import us.ihmc.robotics.stateMachines.StateTransition;
import us.ihmc.robotics.stateMachines.StateTransitionCondition;

public class QuadrupedControllerStateMachineBuilder
{
   private final QuadrupedCommonControllerParameters commonControllerParameters;
   private final QuadrupedRobotParameters robotParameters;

   private final EnumYoVariable<QuadrupedControllerState> requestedState;

   private final List<QuadrupedController> controllers = new ArrayList<>();

   public QuadrupedControllerStateMachineBuilder(QuadrupedCommonControllerParameters commonControllerParameters,
         QuadrupedRobotParameters robotParameters, EnumYoVariable<QuadrupedControllerState> requestedState)
   {
      this.commonControllerParameters = commonControllerParameters;
      this.robotParameters = robotParameters;
      this.requestedState = requestedState;
   }

   public void addDoNothingController()
   {
      controllers.add(new QuadrupedDoNothingController(commonControllerParameters.getFullRobotModel(), commonControllerParameters.getParentRegistry()));
   }

   public void addStandPrepController()
   {
      controllers.add(new QuadrupedStandPrepController(robotParameters, commonControllerParameters.getFullRobotModel(),
            commonControllerParameters.getControlDt(), commonControllerParameters.getParentRegistry()));
   }

   public void addStandReadyController()
   {
      controllers.add(new QuadrupedStandReadyController());
   }
   
   public void addTrotWalkController()
   {
      controllers.add(new QuadrupedTrotWalkController(robotParameters, commonControllerParameters.getFullRobotModel(), commonControllerParameters.getFootSwicthes(),
            commonControllerParameters.getControlDt(), commonControllerParameters.getRobotTimestamp(), commonControllerParameters.getParentRegistry(),
            commonControllerParameters.getGraphicsListRegistry()));
   }

   public void addPositionBasedCrawlController(QuadrupedLegInverseKinematicsCalculator legIkCalc,
         GlobalDataProducer dataProducer)
   {
      controllers
            .add(new QuadrupedPositionBasedCrawlController(commonControllerParameters.getControlDt(), robotParameters,
                  commonControllerParameters.getFullRobotModel(), null, commonControllerParameters.getFootSwicthes(),
                  legIkCalc, dataProducer, commonControllerParameters.getRobotTimestamp(),
                  commonControllerParameters.getParentRegistry(), commonControllerParameters.getGraphicsListRegistry(),
                  commonControllerParameters.getGraphicsListRegistryForDetachedOverhead()));
   }
   
   public void addQuadrupedCenterOfMassVerificationController(QuadrupedLegInverseKinematicsCalculator legIkCalc)
   {
      controllers
      .add(new QuadrupedCenterOfMassVerificationController(commonControllerParameters.getControlDt(), robotParameters,
            commonControllerParameters.getFullRobotModel(), legIkCalc, commonControllerParameters.getRobotTimestamp(),
            commonControllerParameters.getParentRegistry(), commonControllerParameters.getGraphicsListRegistry()));
   }

   public void addSliderBoardController()
   {
      controllers.add(new QuadrupedLegJointSliderBoardController(commonControllerParameters.getFullRobotModel(),
            commonControllerParameters.getParentRegistry()));
   }

   public void addTransition(QuadrupedControllerState from, StateTransition<QuadrupedControllerState> transition)
   {
      QuadrupedController fromController = controllerForEnum(from);

      fromController.addStateTransition(transition);
   }

   public void addPermissibleTransition(QuadrupedControllerState from, QuadrupedControllerState to)
   {
      StateTransitionCondition condition = new PermissiveRequestedStateTransitionCondition<>(requestedState, to);
      StateTransition<QuadrupedControllerState> transition = new StateTransition<>(to, condition);

      addTransition(from, transition);
   }

   public void addJointsInitializedCondition(QuadrupedControllerState from, QuadrupedControllerState to)
   {
      QuadrupedJointInitializer controller = (QuadrupedJointInitializer) controllerForEnum(from);

      ArrayList<StateTransitionCondition> conditions = new ArrayList<>();
      conditions.add(new QuadrupedJointsInitializedTransitionCondition(controller));
      conditions.add(new PermissiveRequestedStateTransitionCondition<>(requestedState, to));

      addTransition(from, new StateTransition<>(to, conditions));
   }

   public void addStandingExitCondition(QuadrupedControllerState from, QuadrupedControllerState to)
   {
      StateTransitionCondition condition = new QuadrupedControllerStandingTransitionCondition(controllerForEnum(from));

      addTransition(from, new StateTransition<>(to, condition));
   }

   public GenericStateMachine<QuadrupedControllerState, QuadrupedController> build()
   {
      GenericStateMachine<QuadrupedControllerState, QuadrupedController> machine = new GenericStateMachine<>(
            "quadrupedControllerStateMachine", "quadrupedControllerSwitchTime", QuadrupedControllerState.class,
            commonControllerParameters.getRobotTimestamp(), commonControllerParameters.getParentRegistry());

      for (int i = 0; i < controllers.size(); i++)
      {
         QuadrupedController controller = controllers.get(i);
         machine.addState(controller);
      }

      return machine;
   }

   /**
    * @param state the state enum for which to search.
    * @return the controller registered for the given state enum.
    */
   private QuadrupedController controllerForEnum(QuadrupedControllerState state)
   {
      for (int i = 0; i < controllers.size(); i++)
      {
         QuadrupedController controller = controllers.get(i);

         if (controller.getStateEnum() == state)
         {
            return controller;
         }
      }

      throw new RuntimeException("Controller not registered: " + state);
   }
}
