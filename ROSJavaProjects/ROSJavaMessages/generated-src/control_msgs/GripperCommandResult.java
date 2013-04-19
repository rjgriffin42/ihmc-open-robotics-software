package control_msgs;

public interface GripperCommandResult extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "control_msgs/GripperCommandResult";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nfloat64 position  # The current gripper gap size (in meters)\nfloat64 effort    # The current effort exerted (in Newtons)\nbool stalled      # True iff the gripper is exerting max effort and not moving\nbool reached_goal # True iff the gripper position has reached the commanded setpoint\n";
  double getPosition();
  void setPosition(double value);
  double getEffort();
  void setEffort(double value);
  boolean getStalled();
  void setStalled(boolean value);
  boolean getReachedGoal();
  void setReachedGoal(boolean value);
}
