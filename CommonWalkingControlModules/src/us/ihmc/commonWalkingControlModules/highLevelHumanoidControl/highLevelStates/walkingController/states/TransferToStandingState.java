package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states;

import us.ihmc.commonWalkingControlModules.controlModules.WalkingFailureDetectionControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.pelvis.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.WalkingMessageHandler;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelControlManagerFactory;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.BalanceManager;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.CenterOfMassHeightManager;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.robotSide.RobotSide;

public class TransferToStandingState extends WalkingState
{
   private final YoDouble maxICPErrorToSwitchToStanding = new YoDouble("maxICPErrorToSwitchToStanding", registry);

   private final YoBoolean doFootExplorationInTransferToStanding = new YoBoolean("doFootExplorationInTransferToStanding", registry);

   private final WalkingMessageHandler walkingMessageHandler;
   private final HighLevelHumanoidControllerToolbox controllerToolbox;
   private final WalkingFailureDetectionControlModule failureDetectionControlModule;

   private final CenterOfMassHeightManager comHeightManager;
   private final BalanceManager balanceManager;
   private final PelvisOrientationManager pelvisOrientationManager;
   private final FeetManager feetManager;

   public TransferToStandingState(WalkingMessageHandler walkingMessageHandler, HighLevelHumanoidControllerToolbox controllerToolbox,
         HighLevelControlManagerFactory managerFactory, WalkingFailureDetectionControlModule failureDetectionControlModule, YoVariableRegistry parentRegistry)
   {
      super(WalkingStateEnum.TO_STANDING, parentRegistry);
      maxICPErrorToSwitchToStanding.set(0.025);

      this.walkingMessageHandler = walkingMessageHandler;
      this.controllerToolbox = controllerToolbox;
      this.failureDetectionControlModule = failureDetectionControlModule;

      comHeightManager = managerFactory.getOrCreateCenterOfMassHeightManager();
      balanceManager = managerFactory.getOrCreateBalanceManager();
      pelvisOrientationManager = managerFactory.getOrCreatePelvisOrientationManager();
      feetManager = managerFactory.getOrCreateFeetManager();

      doFootExplorationInTransferToStanding.set(false);
   }

   @Override
   public void doAction()
   {
      // Always do this so that when a foot slips or is loaded in the air, the height gets adjusted.
      comHeightManager.setSupportLeg(RobotSide.LEFT);
   }

   @Override
   public boolean isDone()
   {
      if (!balanceManager.isICPPlanDone())
         return false;

      return balanceManager.getICPErrorMagnitude() < maxICPErrorToSwitchToStanding.getDoubleValue();
   }

   @Override
   public void doTransitionIntoAction()
   {
      balanceManager.clearICPPlan();
      balanceManager.resetPushRecovery();

      feetManager.initializeContactStatesForDoubleSupport(null);

      WalkingState previousState = (WalkingState) getPreviousState();
      RobotSide previousSupportSide = previousState.getSupportSide();

      if (doFootExplorationInTransferToStanding.getBooleanValue())
      {
         if (previousSupportSide != null)
         {
            feetManager.initializeFootExploration(previousSupportSide.getOppositeSide());
         }
      }

      controllerToolbox.updateBipedSupportPolygons(); // need to always update biped support polygons after a change to the contact states

      failureDetectionControlModule.setNextFootstep(null);

      TransferToAndNextFootstepsData transferToAndNextFootstepsDataForDoubleSupport = walkingMessageHandler
            .createTransferToAndNextFootstepDataForDoubleSupport(RobotSide.LEFT);
      double extraToeOffHeight = 0.0;
      comHeightManager.initialize(transferToAndNextFootstepsDataForDoubleSupport, extraToeOffHeight);

      // Just standing in double support, do nothing
      double finalTransferTime = walkingMessageHandler.getFinalTransferTime();
      pelvisOrientationManager.centerInMidFeetZUpFrame(finalTransferTime);
      balanceManager.setICPPlanTransferFromSide(previousSupportSide);
      balanceManager.initializeICPPlanForStanding(finalTransferTime);
   }

   @Override
   public void doTransitionOutOfAction()
   {
   }
}