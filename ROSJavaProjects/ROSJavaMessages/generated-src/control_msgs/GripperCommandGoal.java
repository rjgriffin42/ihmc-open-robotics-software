package control_msgs;

public interface GripperCommandGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "control_msgs/GripperCommandGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nGripperCommand command\n";
  control_msgs.GripperCommand getCommand();
  void setCommand(control_msgs.GripperCommand value);
}
