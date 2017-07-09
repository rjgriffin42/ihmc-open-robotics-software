package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states;

import us.ihmc.commonWalkingControlModules.controlModules.WalkingFailureDetectionControlModule;
import us.ihmc.commonWalkingControlModules.desiredFootStep.TransferToAndNextFootstepsData;
import us.ihmc.commonWalkingControlModules.desiredFootStep.WalkingMessageHandler;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelControlManagerFactory;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.humanoidRobotics.footstep.FootstepTiming;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.robotSide.RobotSide;

public class TransferToFlamingoStanceState extends TransferState
{
   private final FootstepTiming footstepTiming = new FootstepTiming();

   public TransferToFlamingoStanceState(RobotSide transferToSide, WalkingMessageHandler walkingMessageHandler,
         HighLevelHumanoidControllerToolbox controllerToolbox, HighLevelControlManagerFactory managerFactory,
         WalkingFailureDetectionControlModule failureDetectionControlModule, YoVariableRegistry parentRegistry)
   {
      super(transferToSide, WalkingStateEnum.getFlamingoTransferState(transferToSide), walkingMessageHandler, controllerToolbox, managerFactory,
            failureDetectionControlModule, parentRegistry);
   }

   @Override
   public void doTransitionIntoAction()
   {
      super.doTransitionIntoAction();

      if (!comHeightManager.hasBeenInitializedWithNextStep())
      {
         TransferToAndNextFootstepsData transferToAndNextFootstepsDataForDoubleSupport = walkingMessageHandler.createTransferToAndNextFootstepDataForDoubleSupport(transferToSide);
         double extraToeOffHeight = 0.0;
         if (feetManager.canDoDoubleSupportToeOff(null, transferToSide))
            extraToeOffHeight = feetManager.getToeOffManager().getExtraCoMMaxHeightWithToes();
         comHeightManager.initialize(transferToAndNextFootstepsDataForDoubleSupport, extraToeOffHeight);
      }

      // Transferring to execute a foot pose, hold current desired in upcoming support foot in case it slips
      pelvisOrientationManager.setToHoldCurrentDesiredInSupportFoot(transferToSide);

      double swingTime = Double.POSITIVE_INFINITY;
      double transferTime = walkingMessageHandler.getDefaultTransferTime();
      double finalTransferTime = walkingMessageHandler.getFinalTransferTime();
      footstepTiming.setTimings(Double.POSITIVE_INFINITY, walkingMessageHandler.getDefaultTransferTime());
      balanceManager.addFootstepToPlan(walkingMessageHandler.getFootstepAtCurrentLocation(transferToSide.getOppositeSide()), footstepTiming);
      balanceManager.setICPPlanTransferToSide(transferToSide);
      balanceManager.initializeICPPlanForTransfer(swingTime, transferTime, finalTransferTime);
   }
}