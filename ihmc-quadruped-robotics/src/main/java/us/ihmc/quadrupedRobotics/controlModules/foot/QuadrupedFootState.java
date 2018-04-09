package us.ihmc.quadrupedRobotics.controlModules.foot;

import us.ihmc.commonWalkingControlModules.controllerCore.command.feedbackController.FeedbackControlCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.virtualModelControl.VirtualModelControlCommand;
import us.ihmc.euclid.referenceFrame.FrameVector3D;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;
import us.ihmc.quadrupedRobotics.controller.toolbox.QuadrupedStepTransitionCallback;
import us.ihmc.quadrupedRobotics.controller.toolbox.QuadrupedWaypointCallback;
import us.ihmc.robotics.stateMachine.extra.EventState;

public abstract class QuadrupedFootState implements EventState
{
   protected final FrameVector3D soleForceCommand = new FrameVector3D();

   protected QuadrupedStepTransitionCallback stepTransitionCallback = null;
   protected QuadrupedWaypointCallback waypointCallback = null;

   public abstract VirtualModelControlCommand<?> getVirtualModelControlCommand();
   public abstract FeedbackControlCommand<?> getFeedbackControlCommand();
   public abstract FeedbackControlCommand<?> createFeedbackControlTemplate();

   public FrameVector3DReadOnly getSoleForceCommand()
   {
      return soleForceCommand;
   }

   public void registerStepTransitionCallback(QuadrupedStepTransitionCallback stepTransitionCallback)
   {
      this.stepTransitionCallback = stepTransitionCallback;
   }

   public void registerWaypointCallback(QuadrupedWaypointCallback waypointCallback)
   {
      this.waypointCallback = waypointCallback;
   }
}
