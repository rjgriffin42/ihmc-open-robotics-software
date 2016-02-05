package pr2_controllers_msgs;

public interface SingleJointPositionAction extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "pr2_controllers_msgs/SingleJointPositionAction";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nSingleJointPositionActionGoal action_goal\nSingleJointPositionActionResult action_result\nSingleJointPositionActionFeedback action_feedback\n";
  pr2_controllers_msgs.SingleJointPositionActionGoal getActionGoal();
  void setActionGoal(pr2_controllers_msgs.SingleJointPositionActionGoal value);
  pr2_controllers_msgs.SingleJointPositionActionResult getActionResult();
  void setActionResult(pr2_controllers_msgs.SingleJointPositionActionResult value);
  pr2_controllers_msgs.SingleJointPositionActionFeedback getActionFeedback();
  void setActionFeedback(pr2_controllers_msgs.SingleJointPositionActionFeedback value);
}
