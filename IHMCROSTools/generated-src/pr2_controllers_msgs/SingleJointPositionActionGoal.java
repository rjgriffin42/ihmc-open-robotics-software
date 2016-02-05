package pr2_controllers_msgs;

public interface SingleJointPositionActionGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "pr2_controllers_msgs/SingleJointPositionActionGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nHeader header\nactionlib_msgs/GoalID goal_id\nSingleJointPositionGoal goal\n";
  std_msgs.Header getHeader();
  void setHeader(std_msgs.Header value);
  actionlib_msgs.GoalID getGoalId();
  void setGoalId(actionlib_msgs.GoalID value);
  pr2_controllers_msgs.SingleJointPositionGoal getGoal();
  void setGoal(pr2_controllers_msgs.SingleJointPositionGoal value);
}
