package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates.jumpingController.states;

import java.util.List;

import us.ihmc.commonWalkingControlModules.controlModules.flight.ContactState;
import us.ihmc.commonWalkingControlModules.controlModules.flight.JumpMessageHandler;
import us.ihmc.commonWalkingControlModules.controlModules.flight.WholeBodyMotionPlanner;
import us.ihmc.commonWalkingControlModules.momentumBasedController.HighLevelHumanoidControllerToolbox;
import us.ihmc.commons.PrintTools;
import us.ihmc.euclid.referenceFrame.FramePoint3D;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.screwTheory.Wrench;
import us.ihmc.robotics.sensors.FootSwitchInterface;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.FinishableState;

public abstract class AbstractJumpState extends FinishableState<JumpStateEnum>
{
   private final WholeBodyMotionPlanner motionPlanner;
   private final JumpMessageHandler messageHandler;
   private final HighLevelHumanoidControllerToolbox controllerToolbox;
   protected FramePoint3D initialPositionInState = new FramePoint3D();
   protected FrameVector3D initialVelocityInState = new FrameVector3D();
   protected FrameVector3D initialGroundReactionForceInState = new FrameVector3D();
   
   private final Wrench tempWrench = new Wrench();

   public AbstractJumpState(JumpStateEnum stateEnum, WholeBodyMotionPlanner motionPlanner, JumpMessageHandler messageHandler,
                            HighLevelHumanoidControllerToolbox controllerToolbox)
   {
      super(stateEnum);
      this.motionPlanner = motionPlanner;
      this.messageHandler = messageHandler;
      this.controllerToolbox = controllerToolbox;
   }
   
   public abstract void doStateSpecificTransitionIntoAction();

   @Override
   public void doTransitionIntoAction()
   {
      PrintTools.debug("Transitioning to " + getStateEnum());
      motionPlanner.reset();
      controllerToolbox.getCenterOfMassPosition(initialPositionInState);
      controllerToolbox.getCenterOfMassVelocity(initialVelocityInState);
      SideDependentList<FootSwitchInterface> footSwitches = controllerToolbox.getFootSwitches();
      ReferenceFrame plannerFrame = motionPlanner.getPlanningFrame();
      initialGroundReactionForceInState.setToZero(plannerFrame);
      for(RobotSide robotSide : RobotSide.values)
      {
         FootSwitchInterface footSwitch = footSwitches.get(robotSide);
         footSwitch.computeAndPackFootWrench(tempWrench);
         tempWrench.changeFrame(plannerFrame);
         initialGroundReactionForceInState.add(tempWrench.getLinearPart());
      }
      messageHandler.createJumpSequenceForTesting(initialPositionInState, getStateEnum());
      List<ContactState> contactStateList = messageHandler.getContactStateList();
      motionPlanner.setInitialState(initialPositionInState, initialVelocityInState, initialGroundReactionForceInState);
      if(getStateEnum() == JumpStateEnum.STANDING)
         motionPlanner.setFinalState(initialPositionInState, initialVelocityInState, initialGroundReactionForceInState);
      motionPlanner.processContactStatesAndGenerateMotionNodesForPlanning(contactStateList);
      motionPlanner.computeMotionPlan();
      doStateSpecificTransitionIntoAction();
   }
}
