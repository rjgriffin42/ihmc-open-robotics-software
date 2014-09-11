package us.ihmc.commonWalkingControlModules.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controlModules.foot.FootControlModule.ConstraintType;
import us.ihmc.yoUtilities.dataStructure.variable.BooleanYoVariable;
import us.ihmc.yoUtilities.dataStructure.variable.EnumYoVariable;
import us.ihmc.yoUtilities.stateMachines.StateTransitionAction;


public class FootStateTransitionAction implements StateTransitionAction
{
   EnumYoVariable<ConstraintType> requestedState;
   private final BooleanYoVariable doSingularityEscape;
   private final BooleanYoVariable waitSingularityEscapeBeforeTransitionToNextState;

   public FootStateTransitionAction(EnumYoVariable<ConstraintType> requestedState, BooleanYoVariable doSingularityEscape,
         BooleanYoVariable waitSingularityEscapeBeforeTransitionToNextState)
   {
      this.requestedState = requestedState;
      this.doSingularityEscape = doSingularityEscape;
      this.waitSingularityEscapeBeforeTransitionToNextState = waitSingularityEscapeBeforeTransitionToNextState;
   }

   public void doTransitionAction()
   {
      requestedState.set(null);
      doSingularityEscape.set(false);
      waitSingularityEscapeBeforeTransitionToNextState.set(false);
   }
}
