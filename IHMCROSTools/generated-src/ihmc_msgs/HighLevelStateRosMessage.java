package ihmc_msgs;

public interface HighLevelStateRosMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/HighLevelStateRosMessage";
  static final java.lang.String _DEFINITION = "## HighLevelStateRosMessage\n# This message is used to switch the control scheme between force and position control. WARNING: When\n# in position control, the IHMC balance algorithms will be disabled and it is up to the user to ensure\n# stability.\n\n# The enum value of the current high level state of the robot.\nuint8 high_level_state\n\n# A unique id for the current message. This can be a timestamp or sequence number. Only the unique id\n# in the top level message is used, the unique id in nested messages is ignored. Use\n# /output/last_received_message for feedback about when the last message was received. A message with\n# a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller.\nint64 unique_id\n\n\n# This message utilizes \"enums\". Enum value information for this message follows.\n\n# \"high_level_state\" enum values:\nuint8 WALKING=0 # whole body force control employing IHMC walking, balance, and manipulation algorithms\nuint8 DO_NOTHING_BEHAVIOR=1 # do nothing behavior. the robot will start in this behavior, and report this behavior when falling and ramping down the controller. This behavior is intended for feedback only. Requesting this behavior is not supported and can cause the robot to shut down.\nuint8 DIAGNOSTICS=2 # The robot is peforming an automated diagnostic routine\n\n";
  static final byte WALKING = 0;
  static final byte DO_NOTHING_BEHAVIOR = 1;
  static final byte DIAGNOSTICS = 2;
  byte getHighLevelState();
  void setHighLevelState(byte value);
  long getUniqueId();
  void setUniqueId(long value);
}
