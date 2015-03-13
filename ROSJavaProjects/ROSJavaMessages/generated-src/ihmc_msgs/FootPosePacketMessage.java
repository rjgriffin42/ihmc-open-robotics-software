package ihmc_msgs;

public interface FootPosePacketMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/FootPosePacketMessage";
  static final java.lang.String _DEFINITION = "## FootPosePacketMessage\n# This message commands the controller to move the desired foot to a given location\n# and orientation in world coordinates. This message is not intended to specify footsteps\n# as the controller will use a straight-line trajectory when moving the foot from its\n# current to desired location/orientation\n\n#Options for robotSide\n# uint8 L = 0\n# uint8 R = 1\nuint8 robotSide\n\ngeometry_msgs/Vector3 position\n\ngeometry_msgs/Quaternion orientation\n\n# trajectoryTime specifies how fast or how slow to move to the desired pose\nfloat64 trajectoryTime\n\nint8 destination\n\n\n";
  byte getRobotSide();
  void setRobotSide(byte value);
  geometry_msgs.Vector3 getPosition();
  void setPosition(geometry_msgs.Vector3 value);
  geometry_msgs.Quaternion getOrientation();
  void setOrientation(geometry_msgs.Quaternion value);
  double getTrajectoryTime();
  void setTrajectoryTime(double value);
  byte getDestination();
  void setDestination(byte value);
}
