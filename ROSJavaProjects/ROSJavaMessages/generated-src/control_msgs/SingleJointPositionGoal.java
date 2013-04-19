package control_msgs;

public interface SingleJointPositionGoal extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "control_msgs/SingleJointPositionGoal";
  static final java.lang.String _DEFINITION = "# ====== DO NOT MODIFY! AUTOGENERATED FROM AN ACTION DEFINITION ======\nfloat64 position\nduration min_duration\nfloat64 max_velocity\n";
  double getPosition();
  void setPosition(double value);
  org.ros.message.Duration getMinDuration();
  void setMinDuration(org.ros.message.Duration value);
  double getMaxVelocity();
  void setMaxVelocity(double value);
}
