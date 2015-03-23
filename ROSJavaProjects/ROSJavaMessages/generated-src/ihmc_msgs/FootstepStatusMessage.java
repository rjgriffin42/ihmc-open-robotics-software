package ihmc_msgs;

public interface FootstepStatusMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/FootstepStatusMessage";
  static final java.lang.String _DEFINITION = "## FootstepStatusMessage\n# This message gives the status of the current footstep from the controller.\n\n# Options for status\n# uint8 STARTED = 0\n# uint8 COMPLETED = 1\nuint8 status\n\n# footstepIndex starts at 0 and monotonically increases with each completed footstep in a given\n# FootstepDataListMessage.\nint32 footstep_index\n\nint32 robot_side\n\ngeometry_msgs/Vector3 actual_foot_position_in_world\n\ngeometry_msgs/Quaternion actual_foot_orientation_in_world\n\nbool is_done_walking\n\n\n";
  byte getStatus();
  void setStatus(byte value);
  int getFootstepIndex();
  void setFootstepIndex(int value);
  int getRobotSide();
  void setRobotSide(int value);
  geometry_msgs.Vector3 getActualFootPositionInWorld();
  void setActualFootPositionInWorld(geometry_msgs.Vector3 value);
  geometry_msgs.Quaternion getActualFootOrientationInWorld();
  void setActualFootOrientationInWorld(geometry_msgs.Quaternion value);
  boolean getIsDoneWalking();
  void setIsDoneWalking(boolean value);
}
