package pr2_controllers_msgs;

public interface Pr2GripperCommandActionResult extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "pr2_controllers_msgs/Pr2GripperCommandActionResult";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\n\nHeader header\nactionlib_msgs/GoalStatus status\nPr2GripperCommandResult result\n";
  std_msgs.Header getHeader();
  void setHeader(std_msgs.Header value);
  actionlib_msgs.GoalStatus getStatus();
  void setStatus(actionlib_msgs.GoalStatus value);
  pr2_controllers_msgs.Pr2GripperCommandResult getResult();
  void setResult(pr2_controllers_msgs.Pr2GripperCommandResult value);
}
