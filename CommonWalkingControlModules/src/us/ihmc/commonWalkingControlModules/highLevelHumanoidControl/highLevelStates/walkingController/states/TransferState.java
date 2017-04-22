package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states;

import us.ihmc.commonWalkingControlModules.controlModules.PelvisOrientationManager;
import us.ihmc.commonWalkingControlModules.controlModules.WalkingFailureDetectionControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FeetManager;
import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.commonWalkingControlModules.desiredFootStep.WalkingMessageHandler;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelControlManagerFactory;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.BalanceManager;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.CenterOfMassHeightManager;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.humanoidRobotics.footstep.Footstep;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FrameConvexPolygon2d;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FramePoint2d;
import us.ihmc.robotics.robotSide.RobotSide;

public abstract class TransferState extends WalkingState
{
   protected final RobotSide transferToSide;

   protected final WalkingMessageHandler walkingMessageHandler;
   protected final HighLevelHumanoidControllerToolbox controllerToolbox;
   protected final WalkingFailureDetectionControlModule failureDetectionControlModule;

   protected final CenterOfMassHeightManager comHeightManager;
   protected final BalanceManager balanceManager;
   protected final PelvisOrientationManager pelvisOrientationManager;
   protected final FeetManager feetManager;

   private final FramePoint2d desiredICPLocal = new FramePoint2d();
   private final FramePoint2d capturePoint2d = new FramePoint2d();
   private final FramePoint2d desiredCMP = new FramePoint2d();

   private final FramePoint2d filteredDesiredCoP = new FramePoint2d();
   private final FramePoint nextExitCMP = new FramePoint();

   public TransferState(RobotSide transferToSide, WalkingStateEnum transferStateEnum, WalkingMessageHandler walkingMessageHandler,
         HighLevelHumanoidControllerToolbox controllerToolbox, HighLevelControlManagerFactory managerFactory,
         WalkingFailureDetectionControlModule failureDetectionControlModule, YoVariableRegistry parentRegistry)
   {
      super(transferStateEnum, parentRegistry);
      this.transferToSide = transferToSide;
      this.walkingMessageHandler = walkingMessageHandler;
      this.failureDetectionControlModule = failureDetectionControlModule;
      this.controllerToolbox = controllerToolbox;

      comHeightManager = managerFactory.getOrCreateCenterOfMassHeightManager();
      balanceManager = managerFactory.getOrCreateBalanceManager();
      pelvisOrientationManager = managerFactory.getOrCreatePelvisOrientationManager();
      feetManager = managerFactory.getOrCreateFeetManager();
   }

   public RobotSide getTransferToSide()
   {
      return transferToSide;
   }

   @Override
   public void doAction()
   {
      feetManager.updateContactStatesInDoubleSupport(transferToSide);

      switchToToeOffIfPossible();

      // Always do this so that when a foot slips or is loaded in the air, the height gets adjusted.
      comHeightManager.setSupportLeg(transferToSide);
   }

   @Override
   public boolean isDone()
   {
      if (!balanceManager.isICPPlanDone())
         return false;
      balanceManager.getCapturePoint(capturePoint2d);
      FrameConvexPolygon2d supportPolygonInWorld = controllerToolbox.getBipedSupportPolygons().getSupportPolygonInWorld();
      boolean isICPInsideSupportPolygon = supportPolygonInWorld.isPointInside(capturePoint2d);

      if (!isICPInsideSupportPolygon)
         return true;
      else
         return balanceManager.isTransitionToSingleSupportSafe(transferToSide);
   }

   public boolean isStopWalkingSafe()
   {
      return balanceManager.isTransitionToStandingSafe();
   }

   public void switchToToeOffIfPossible()
   {
      RobotSide trailingLeg = transferToSide.getOppositeSide();

      boolean shouldComputeToeLineContact = feetManager.shouldComputeToeLineContact();
      boolean shouldComputeToePointContact = feetManager.shouldComputeToePointContact();

      // the only case left for determining the contact state of the trailing foot
      // // FIXME: 4/22/17 have this update properly
      if (feetManager.getCurrentConstraintType(trailingLeg) != ConstraintType.TOES || shouldComputeToeLineContact || shouldComputeToePointContact)
      {
         balanceManager.getDesiredCMP(desiredCMP);
         balanceManager.getDesiredICP(desiredICPLocal);
         balanceManager.getCapturePoint(capturePoint2d);
         balanceManager.getNextExitCMP(nextExitCMP);

         feetManager.updateToeOffDoubleSupport(trailingLeg, nextExitCMP, desiredCMP, desiredICPLocal, capturePoint2d);

         controllerToolbox.getFilteredDesiredCenterOfPressure(controllerToolbox.getContactableFeet().get(trailingLeg), filteredDesiredCoP);

         if (feetManager.willDoPointToeOff() && shouldComputeToePointContact)
         {
            feetManager.computeToeOffContactPoint(trailingLeg, nextExitCMP, filteredDesiredCoP);
            feetManager.useToeOffPointContact(trailingLeg);
            feetManager.requestToeOff(trailingLeg);
            controllerToolbox.updateBipedSupportPolygons(); // need to always update biped support polygons after a change to the contact states
         }
         else if (feetManager.willDoLineToeOff() && shouldComputeToeLineContact)
         {
            feetManager.computeToeOffContactLine(trailingLeg, nextExitCMP, filteredDesiredCoP);
            feetManager.useToeOffLineContact(trailingLeg);
            feetManager.requestToeOff(trailingLeg);
            controllerToolbox.updateBipedSupportPolygons(); // need to always update biped support polygons after a change to the contact states
         }
      }
   }

   @Override
   public void doTransitionIntoAction()
   {
      balanceManager.clearICPPlan();

      feetManager.initializeContactStatesForDoubleSupport(transferToSide);
      controllerToolbox.updateBipedSupportPolygons(); // need to always update biped support polygons after a change to the contact states

      Footstep nextFootstep = walkingMessageHandler.peek(0);
      failureDetectionControlModule.setNextFootstep(nextFootstep);
      balanceManager.setUpcomingFootstep(nextFootstep);

      balanceManager.resetPushRecovery();

      double transferTime = walkingMessageHandler.getNextTransferTime();
      pelvisOrientationManager.setTrajectoryTime(transferTime);
   }

   public boolean isInitialTransfer()
   {
      return getPreviousState().getStateEnum() == WalkingStateEnum.STANDING;
   }

   @Override
   public void doTransitionOutOfAction()
   {
      feetManager.reset();
   }
}