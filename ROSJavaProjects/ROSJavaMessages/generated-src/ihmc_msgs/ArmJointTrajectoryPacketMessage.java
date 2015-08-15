package ihmc_msgs;

public interface ArmJointTrajectoryPacketMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/ArmJointTrajectoryPacketMessage";
  static final java.lang.String _DEFINITION = "## ArmJointTrajectoryPacketMessage\r\n# Packet for executing an arm joint trajectory. It works similar to the\r\n# trajectory_msgs/JointTrajectory message.\r\n\r\n# Specifies the side of the robot that will execute the trajectory\r\n# Options for robotSide\r\nuint8 LEFT=0 # refers to the LEFT side of a robot\r\nuint8 RIGHT=1 # refers to the RIGHT side of a robot\r\nuint8 robot_side\r\n\r\n# List of points in the trajectory\r\nJointTrajectoryPointMessage[] trajectory_points\r\n\r\n\r\n";
  static final byte LEFT = 0;
  static final byte RIGHT = 1;
  byte getRobotSide();
  void setRobotSide(byte value);
  java.util.List<ihmc_msgs.JointTrajectoryPointMessage> getTrajectoryPoints();
  void setTrajectoryPoints(java.util.List<ihmc_msgs.JointTrajectoryPointMessage> value);
}
