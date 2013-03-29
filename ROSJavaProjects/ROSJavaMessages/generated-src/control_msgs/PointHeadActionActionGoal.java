package control_msgs;

public interface PointHeadActionActionGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "control_msgs/PointHeadActionActionGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nHeader header\nactionlib_msgs/GoalID goal_id\nPointHeadActionGoal goal\n";
  std_msgs.Header getHeader();
  void setHeader(std_msgs.Header value);
  actionlib_msgs.GoalID getGoalId();
  void setGoalId(actionlib_msgs.GoalID value);
  control_msgs.PointHeadActionGoal getGoal();
  void setGoal(control_msgs.PointHeadActionGoal value);
}
