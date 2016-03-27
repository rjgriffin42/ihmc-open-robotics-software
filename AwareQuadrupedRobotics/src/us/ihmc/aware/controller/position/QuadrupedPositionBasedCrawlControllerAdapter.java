package us.ihmc.aware.controller.position;

import us.ihmc.aware.communication.QuadrupedControllerInputProvider;
import us.ihmc.aware.parameters.QuadrupedRuntimeEnvironment;
import us.ihmc.aware.params.ParameterMapRepository;
import us.ihmc.quadrupedRobotics.controller.QuadrupedPositionBasedCrawlController;
import us.ihmc.quadrupedRobotics.parameters.QuadrupedRobotParameters;

public class QuadrupedPositionBasedCrawlControllerAdapter implements QuadrupedPositionController
{
//   private static final String PARAM_INITIAL_COM_HEIGHT = "initialCoMHeight";
//   private static final String PARAM_DEFAULT_SWING_HEIGHT = "defaultSwingHeight";
//   private static final String PARAM_DEFAULT_SWING_DURATION = "defaultSwingDuration";
//   private static final String PARAM_DEFAULT_SUB_CIRCLE_RADIUS = "defaultSubCircleRadius";
//   private static final String PARAM_MAX_YAW_RATE = "maxYawRate";
//   private static final String PARAM_DEFAULT_COM_CLOSE_TO_FINAL_DESIRED_TRANSITION_RADIUS = "defaultCoMCloseToFinalDesiredTransitionRadius";

//   private final ParameterMap params;
   private final QuadrupedPositionBasedCrawlController controller;

   public QuadrupedPositionBasedCrawlControllerAdapter(QuadrupedRuntimeEnvironment environment,
         QuadrupedRobotParameters parameters, QuadrupedControllerInputProvider inputProvider, ParameterMapRepository paramMapRepository)
   {
//      this.params = paramMapRepository.get(QuadrupedPositionBasedCrawlControllerAdapter.class);

//      params.setDefault(PARAM_INITIAL_COM_HEIGHT, 0.55);
//      params.setDefault(PARAM_DEFAULT_SWING_HEIGHT, 0.1);
//      params.setDefault(PARAM_DEFAULT_SWING_DURATION, 0.4);
//      params.setDefault(PARAM_DEFAULT_SUB_CIRCLE_RADIUS, 0.06);
//      params.setDefault(PARAM_MAX_YAW_RATE, 0.2);
//      params.setDefault(PARAM_DEFAULT_COM_CLOSE_TO_FINAL_DESIRED_TRANSITION_RADIUS, 0.10);

      // TODO: null foot switches
      this.controller = new QuadrupedPositionBasedCrawlController(environment.getControlDT(), parameters,
            environment.getFullRobotModel(), inputProvider, environment.getFootSwitches(), environment.getLegIkCalculator(),
            environment.getGlobalDataProducer(), environment.getRobotTimestamp(), environment.getParentRegistry(),
            environment.getGraphicsListRegistry(), environment.getGraphicsListRegistryForDetachedOverhead());
   }

   @Override
   public void onEntry()
   {
      controller.doTransitionIntoAction();
   }

   @Override
   public QuadrupedPositionControllerEvent process()
   {
      controller.doAction();

      // TODO: How do we fire events from the adapted controller?
      return null;
   }

   @Override
   public void onExit()
   {
      controller.doTransitionOutOfAction();
   }
}
