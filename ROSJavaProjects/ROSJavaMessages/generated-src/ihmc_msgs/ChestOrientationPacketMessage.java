package ihmc_msgs;

public interface ChestOrientationPacketMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/ChestOrientationPacketMessage";
  static final java.lang.String _DEFINITION = "## ChestOrientationPacketMessage\r\n# This message sets the orientation of the robot\'s chest in world coordinates.\r\n\r\ngeometry_msgs/Quaternion orientation\r\n\r\n# trajectoryTime specifies how fast or how slow to move to the desired pose\r\nfloat64 trajectoryTime\r\n\r\n# toHomePosition can be used to move the chest back to its default starting position\r\nbool toHomeOrientation\r\n\r\nint8 destination\r\n\r\n\r\n";
  geometry_msgs.Quaternion getOrientation();
  void setOrientation(geometry_msgs.Quaternion value);
  double getTrajectoryTime();
  void setTrajectoryTime(double value);
  boolean getToHomeOrientation();
  void setToHomeOrientation(boolean value);
  byte getDestination();
  void setDestination(byte value);
}
