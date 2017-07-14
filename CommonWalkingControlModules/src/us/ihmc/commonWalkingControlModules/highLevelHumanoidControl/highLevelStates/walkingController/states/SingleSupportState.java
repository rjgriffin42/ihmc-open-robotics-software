package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.walkingController.states;

import us.ihmc.commonWalkingControlModules.desiredFootStep.WalkingMessageHandler;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.factories.HighLevelControlManagerFactory;
import us.ihmc.commonWalkingControlModules.instantaneousCapturePoint.BalanceManager;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.robotModels.FullHumanoidRobotModel;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.sensors.FootSwitchInterface;

public abstract class SingleSupportState extends WalkingState
{
   protected final RobotSide swingSide;
   protected final RobotSide supportSide;
   
   private final YoBoolean hasMinimumTimePassed = new YoBoolean("hasMinimumTimePassed", registry);
   protected final YoDouble minimumSwingFraction = new YoDouble("minimumSwingFraction", registry);

   protected final WalkingMessageHandler walkingMessageHandler;
   protected final SideDependentList<FootSwitchInterface> footSwitches;
   protected final FullHumanoidRobotModel fullRobotModel;

   protected final BalanceManager balanceManager;

   public SingleSupportState(RobotSide supportSide, WalkingStateEnum singleSupportStateEnum, WalkingMessageHandler walkingMessageHandler,
         HighLevelHumanoidControllerToolbox controllerToolbox, HighLevelControlManagerFactory managerFactory, YoVariableRegistry parentRegistry)
   {
      super(singleSupportStateEnum, parentRegistry);

      this.supportSide = supportSide;
      swingSide = supportSide.getOppositeSide();

      minimumSwingFraction.set(0.5);

      this.walkingMessageHandler = walkingMessageHandler;
      footSwitches = controllerToolbox.getFootSwitches();
      fullRobotModel = controllerToolbox.getFullRobotModel();

      balanceManager = managerFactory.getOrCreateBalanceManager();
   }

   public RobotSide getSwingSide()
   {
      return swingSide;
   }

   public RobotSide getSupportSide()
   {
      return supportSide;
   }

   @Override
   public void doAction()
   {
   }

   @Override
   public boolean isDone()
   {
      hasMinimumTimePassed.set(hasMinimumTimePassed());

      if (!hasMinimumTimePassed.getBooleanValue())
         return false;

      return hasMinimumTimePassed.getBooleanValue() && footSwitches.get(swingSide).hasFootHitGround();
   }

   protected abstract boolean hasMinimumTimePassed();

   @Override
   public void doTransitionIntoAction()
   {
      balanceManager.clearICPPlan();
      footSwitches.get(swingSide).reset();
      integrateAnkleAccelerationsOnSwingLeg(swingSide);
   }

   @Override
   public void doTransitionOutOfAction()
   {
      balanceManager.resetPushRecovery();
      resetLoadedLegIntegrators(swingSide);
   }

   private void integrateAnkleAccelerationsOnSwingLeg(RobotSide swingSide)
   {
      fullRobotModel.getLegJoint(swingSide, LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(true);
      fullRobotModel.getLegJoint(swingSide, LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(true);
      fullRobotModel.getLegJoint(swingSide.getOppositeSide(), LegJointName.ANKLE_PITCH).setIntegrateDesiredAccelerations(false);
      fullRobotModel.getLegJoint(swingSide.getOppositeSide(), LegJointName.ANKLE_ROLL).setIntegrateDesiredAccelerations(false);
   }

   private void resetLoadedLegIntegrators(RobotSide robotSide)
   {
      for (LegJointName jointName : fullRobotModel.getRobotSpecificJointNames().getLegJointNames())
         fullRobotModel.getLegJoint(robotSide, jointName).resetDesiredAccelerationIntegrator();
   }
}